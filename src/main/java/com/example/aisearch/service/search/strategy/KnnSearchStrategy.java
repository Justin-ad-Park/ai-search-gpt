package com.example.aisearch.service.search.strategy;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.search.ProductSearchRequest;
import com.example.aisearch.service.embedding.QueryEmbeddingService;
import com.example.aisearch.service.search.categoryboost.policy.CategoryBoostingDecider;
import com.example.aisearch.service.search.categoryboost.policy.CategoryBoostingResult;
import com.example.aisearch.service.search.query.HybridBaseQueryBuilder;
import com.example.aisearch.service.search.query.SearchFilterQueryBuilder;
import com.example.aisearch.service.search.strategy.mapper.DefaultSearchResponseMapper;
import com.example.aisearch.service.search.strategy.request.ElasticsearchSearchRequestBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 검색 요청을 벡터 기반 하이브리드 검색 또는 필터 전용 검색으로 분기해 실행하는 전략.
 */
@Component
@ConditionalOnProperty(prefix = "ai-search.search", name = "mode", havingValue = "hybrid", matchIfMissing = true)
public class KnnSearchStrategy extends SearchStrategy {

    private final CategoryBoostingDecider categoryBoostingDecider;

    public KnnSearchStrategy(
            ElasticsearchClient client,
            QueryEmbeddingService queryEmbeddingService,
            SearchFilterQueryBuilder filterQueryBuilder,
            HybridBaseQueryBuilder hybridBaseQueryBuilder,
            ElasticsearchSearchRequestBuilder searchRequestBuilder,
            DefaultSearchResponseMapper searchResponseMapper,
            CategoryBoostingDecider categoryBoostingDecider,
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
        this.categoryBoostingDecider = categoryBoostingDecider;
    }

    @Override
    protected Query buildBaseQuery(ProductSearchRequest request) {
        return hybridBaseQueryBuilder.build(request, filterQueryBuilder.buildFilterQuery(request));
    }

    @Override
    protected CategoryBoostingResult buildDecision(ProductSearchRequest request) {
        return categoryBoostingDecider.decide(request);
    }
}
