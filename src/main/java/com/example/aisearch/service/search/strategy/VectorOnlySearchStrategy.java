package com.example.aisearch.service.search.strategy;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.search.ProductSearchRequest;
import com.example.aisearch.model.search.SearchSortOption;
import com.example.aisearch.service.embedding.QueryEmbeddingService;
import com.example.aisearch.service.search.categoryboost.policy.CategoryBoostingResult;
import com.example.aisearch.service.search.query.HybridBaseQueryBuilder;
import com.example.aisearch.service.search.query.SearchFilterQueryBuilder;
import com.example.aisearch.service.search.strategy.mapper.DefaultSearchResponseMapper;
import com.example.aisearch.service.search.strategy.request.ElasticsearchSearchRequestBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * BM25 가중치를 섞지 않고 벡터 유사도만으로 검색하는 전략.
 *
 * <p>검색어가 있을 때:
 * - filter 또는 match_all 을 base query 로 사용
 * - cosineSimilarity(product_vector, query_vector) 만으로 점수를 계산
 *
 * <p>검색어가 없을 때:
 * - 기존과 동일하게 필터/정렬 기반 일반 검색을 수행한다.
 */
@Component
@ConditionalOnProperty(prefix = "ai-search.search", name = "mode", havingValue = "vector-only")
public class VectorOnlySearchStrategy extends SearchStrategy {

    public VectorOnlySearchStrategy(
            ElasticsearchClient client,
            QueryEmbeddingService queryEmbeddingService,
            SearchFilterQueryBuilder filterQueryBuilder,
            HybridBaseQueryBuilder hybridBaseQueryBuilder,
            ElasticsearchSearchRequestBuilder searchRequestBuilder,
            DefaultSearchResponseMapper searchResponseMapper,
            AiSearchProperties properties
    ) {
        super(
                client,
                queryEmbeddingService,
                filterQueryBuilder,
                hybridBaseQueryBuilder,
                searchRequestBuilder,
                searchResponseMapper,
                properties
        );
    }

    @Override
    protected Query buildBaseQuery(ProductSearchRequest request) {
        return filterQueryBuilder.buildRootQuery(request);
    }

    @Override
    protected CategoryBoostingResult buildDecision(ProductSearchRequest request) {
        return CategoryBoostingResult.withoutBoost(SearchSortOption.RELEVANCE_DESC);
    }
}
