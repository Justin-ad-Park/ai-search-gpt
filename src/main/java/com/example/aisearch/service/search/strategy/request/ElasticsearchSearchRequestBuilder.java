package com.example.aisearch.service.search.strategy.request;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.search.SearchSortOption;
import com.example.aisearch.service.search.categoryboost.policy.CategoryBoostBetaTuner;
import com.example.aisearch.service.search.categoryboost.policy.CategoryBoostingResult;
import com.example.aisearch.service.search.strategy.script.PainlessHybridScoreScriptFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Elasticsearch SearchRequest 조립을 전담한다.
 */
@Component
public class ElasticsearchSearchRequestBuilder {

    private final AiSearchProperties properties;
    private final CategoryBoostBetaTuner categoryBoostBetaTuner;
    private final PainlessHybridScoreScriptFactory scriptFactory;

    public ElasticsearchSearchRequestBuilder(
            AiSearchProperties properties,
            CategoryBoostBetaTuner categoryBoostBetaTuner,
            PainlessHybridScoreScriptFactory scriptFactory
    ) {
        this.properties = properties;
        this.categoryBoostBetaTuner = categoryBoostBetaTuner;
        this.scriptFactory = scriptFactory;
    }

    public SearchRequest buildHybridRequest(
            String readAlias,
            Query baseQuery,
            CategoryBoostingResult decision,
            List<Float> queryVector,
            int from,
            int size
    ) {
        return SearchRequest.of(s -> s
                .index(readAlias)
                .query(q -> q.scriptScore(ss -> ss
                        .query(baseQuery)
                        .script(sc -> sc.inline(i -> {
                            i.lang("painless")
                                    .source(scriptFactory.selectScriptSource(decision))
                                    .params("query_vector", JsonData.of(queryVector))
                                    .params("min_score_threshold", JsonData.of(properties.minScoreThreshold()))
                                    .params("beta", JsonData.of(categoryBoostBetaTuner.getBeta()));
                            if (decision.applyCategoryBoost()) {
                                i.params("category_boost_by_id", JsonData.of(decision.categoryBoostById()));
                            }
                            return i;
                        }))
                ))
                .sort(decision.sortOptions())
                .trackScores(true)
                .from(from)
                .size(size)
                .minScore(properties.minScoreThreshold())
        );
    }

    public SearchRequest buildFilterOnlyRequest(
            String readAlias,
            Query rootQuery,
            SearchSortOption sortOption,
            int from,
            int size
    ) {
        return SearchRequest.of(s -> s
                .index(readAlias)
                .query(rootQuery)
                .sort(sortOption.toSortOptions())
                .trackScores(true)
                .from(from)
                .size(size)
        );
    }
}

