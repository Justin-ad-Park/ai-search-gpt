package com.example.aisearch.service.search.strategy;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.model.search.SearchPageResult;
import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.service.embedding.model.EmbeddingService;
import com.example.aisearch.service.search.categoryboost.policy.CategoryBoostBetaTuner;
import com.example.aisearch.service.search.categoryboost.policy.CategoryBoostingDecider;
import com.example.aisearch.service.search.categoryboost.policy.CategoryBoostingResult;
import com.example.aisearch.service.search.query.SearchFilterQueryBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 검색 요청을 벡터 기반 하이브리드 검색 또는 필터 전용 검색으로 분기해 실행하는 전략.
 */
@Component
public class KnnSearchStrategy implements SearchStrategy {

    private static final String SCRIPT_COMMON = """
            double vectorScore = (cosineSimilarity(params.query_vector, 'product_vector') + 1.0) / 2.0;
            double lexicalScore = Math.min(_score, 5.0) / 5.0;
            double base = 0.9 * vectorScore + 0.1 * lexicalScore;
            if (base < params.min_score_threshold) return 0.0;
            double categoryBoost = 0.0;
            """;

    private static final String CATEGORY_BOOST_BLOCK = """
            if (doc['categoryId'].size() != 0) {
              String categoryKey = String.valueOf(doc['categoryId'].value);
              def rawBoost = params.category_boost_by_id.get(categoryKey);
              if (rawBoost != null) {
                categoryBoost = ((Number) rawBoost).doubleValue();
              }
            }
            """;

    private static final String SCRIPT_RETURN_BLOCK = """
            double finalScore = base * (1.0 + params.beta * categoryBoost);
            return finalScore;
            """;

    private static final String BASE_SCRIPT = SCRIPT_COMMON + SCRIPT_RETURN_BLOCK;
    private static final String CATEGORY_BOOST_SCRIPT = SCRIPT_COMMON + CATEGORY_BOOST_BLOCK + SCRIPT_RETURN_BLOCK;

    private final ElasticsearchClient client;
    private final AiSearchProperties properties;
    private final EmbeddingService embeddingService;
    private final SearchFilterQueryBuilder filterQueryBuilder;
    private final CategoryBoostBetaTuner categoryBoostBetaTuner;
    private final CategoryBoostingDecider categoryBoostingDecider;

    public KnnSearchStrategy(
            ElasticsearchClient client,
            AiSearchProperties properties,
            EmbeddingService embeddingService,
            SearchFilterQueryBuilder filterQueryBuilder,
            CategoryBoostBetaTuner categoryBoostBetaTuner,
            CategoryBoostingDecider categoryBoostingDecider
    ) {
        this.client = client;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.filterQueryBuilder = filterQueryBuilder;
        this.categoryBoostBetaTuner = categoryBoostBetaTuner;
        this.categoryBoostingDecider = categoryBoostingDecider;
    }

    @Override
    public SearchPageResult search(SearchRequest searchRequest, Pageable pageable) {
        try {
            // 검색어가 있으면 임베딩 + script_score 기반 하이브리드 검색을 수행한다.
            if (searchRequest.hasQuery()) {
                return vectorScoreSearch(searchRequest, pageable);
            }
            // 검색어가 없으면 필터/정렬만 적용한 일반 검색을 수행한다.
            return filterOnlySearch(searchRequest, pageable);
        } catch (IOException e) {
            throw new IllegalStateException("벡터 검색 실패", e);
        }
    }

    private SearchPageResult vectorScoreSearch(SearchRequest request, Pageable pageable) throws IOException {
        int size = pageable.getPageSize();
        int from = (int) pageable.getOffset();
        CategoryBoostingResult decision = categoryBoostingDecider.decide(request);

        // 사용자 질의를 임베딩 벡터로 변환해 cosineSimilarity 계산에 사용한다.
        float[] embedding = embeddingService.embed(request.query());
        List<Float> queryVector = toFloatList(embedding);
        Query baseQuery = buildHybridBaseQuery(request, filterQueryBuilder.buildFilterQuery(request));

        SearchResponse<Map> response
                = client.search(s -> s  /* SearchRequest.Builder */
                        .index(getReadAlias())
                        .query(
                        /* Query.Builder */
                        q -> q.scriptScore(
                                /* ScriptScoreQuery.Builder */
                                ss -> ss
                                        .query(baseQuery)
                                        .script(
                                                /* Script.Builder */
                                                sc -> sc.inline(
                                                        /* InlineScript.Builder */
                                                        i ->
                                                        {
                                                            i.lang("painless")
                                                                    .source(selectScriptSource(decision))
                                                                    .params("query_vector", JsonData.of(queryVector))
                                                                    .params("min_score_threshold", JsonData.of(properties.minScoreThreshold()))
                                                                    .params("beta", JsonData.of(categoryBoostBetaTuner.getBeta()));
                                                            // boost 적용 케이스에서만 category boost 파라미터를 전달한다.
                                                            if (decision.applyCategoryBoost()) {
                                                                i.params("category_boost_by_id", JsonData.of(decision.categoryBoostById()));
                                                            }
                                                            return i;
                                                        }
                                                )
                                        )
                                )
                        )
                        .sort(decision.sortOptions())
                        .trackScores(true)
                        .from(from)
                        .size(size)
                        .minScore(properties.minScoreThreshold())
                ,
                Map.class
        );
        List<SearchHitResult> results = toResults(response);
        return SearchPageResult.of(pageable, extractTotalHits(response), results);
    }

    private String selectScriptSource(CategoryBoostingResult decision) {
        // 카테고리 부스팅 필요 여부에 따라 script_score 소스를 선택한다.
        return decision.applyCategoryBoost() ? CATEGORY_BOOST_SCRIPT : BASE_SCRIPT;
    }

    private SearchPageResult filterOnlySearch(
            SearchRequest request,
            Pageable pageable
    ) throws IOException {
        int size = pageable.getPageSize();
        int from = (int) pageable.getOffset();
        Query rootQuery = filterQueryBuilder.buildRootQuery(request);
        SearchResponse<Map> response = client.search(s -> s
                        .index(getReadAlias())
                        .query(rootQuery)
                        .sort(request.sortOption().toSortOptions())
                        .trackScores(true)
                        .from(from)
                        .size(size),
                Map.class
        );
        List<SearchHitResult> results = toResults(response);
        return SearchPageResult.of(pageable, extractTotalHits(response), results);
    }

    private Query buildHybridBaseQuery(SearchRequest request, java.util.Optional<Query> filterQuery) {
        // 텍스트 연관도(_score)를 script_score에서 lexicalScore로 함께 반영한다.
        Query lexicalQuery = Query.of(q -> q.multiMatch(mm -> mm
                .query(request.query())
                .fields("product_name^2", "description")
        ));

        return Query.of(q -> q.bool(b -> {
            // 필터는 점수 계산과 무관하게 후보군을 제한한다.
            filterQuery.ifPresent(b::filter);
            // 텍스트 쿼리는 should로 넣어도 minimum_should_match=0 이라 필터만으로도 검색 가능하다.
            b.should(lexicalQuery);
            b.minimumShouldMatch("0");
            return b;
        }));
    }

    private List<SearchHitResult> toResults(SearchResponse<Map> response) {
        return response.hits().hits().stream()
                .map(hit -> new SearchHitResult(hit.id(), hit.score(), stripVector(hit.source())))
                .toList();
    }

    private Map<String, Object> stripVector(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        // 응답 payload 축소를 위해 대용량 벡터 필드를 제거한다.
        Map<String, Object> filtered = new java.util.HashMap<>(source);
        filtered.remove("product_vector");
        return filtered;
    }

    private long extractTotalHits(SearchResponse<Map> response) {
        if (response.hits() == null || response.hits().total() == null) {
            return 0L;
        }
        return response.hits().total().value();
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float value : array) {
            list.add(value);
        }
        return list;
    }

    private String getReadAlias() {
        String readAlias = properties.readAlias();
        if (readAlias == null || readAlias.isBlank()) {
            throw new IllegalStateException("ai-search.read-alias 값이 비어 있습니다.");
        }
        return readAlias;
    }
}
