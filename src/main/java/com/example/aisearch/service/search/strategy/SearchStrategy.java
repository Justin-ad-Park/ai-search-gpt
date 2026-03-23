package com.example.aisearch.service.search.strategy;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.aisearch.config.AiSearchProperties;
import com.example.aisearch.model.search.ProductSearchRequest;
import com.example.aisearch.model.search.SearchPageResult;
import com.example.aisearch.service.embedding.QueryEmbeddingUnavailableException;
import com.example.aisearch.service.embedding.QueryEmbeddingService;
import com.example.aisearch.service.search.categoryboost.policy.CategoryBoostingResult;
import com.example.aisearch.service.search.query.HybridBaseQueryBuilder;
import com.example.aisearch.service.search.query.SearchFilterQueryBuilder;
import com.example.aisearch.service.search.strategy.mapper.DefaultSearchResponseMapper;
import com.example.aisearch.service.search.strategy.request.ElasticsearchSearchRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.Map;

public abstract class SearchStrategy {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ElasticsearchClient client;
    protected final QueryEmbeddingService queryEmbeddingService;
    protected final SearchFilterQueryBuilder filterQueryBuilder;
    protected final HybridBaseQueryBuilder hybridBaseQueryBuilder;
    protected final ElasticsearchSearchRequestBuilder searchRequestBuilder;
    protected final DefaultSearchResponseMapper searchResponseMapper;
    protected final AiSearchProperties properties;

    protected SearchStrategy(
            ElasticsearchClient client,
            QueryEmbeddingService queryEmbeddingService,
            SearchFilterQueryBuilder filterQueryBuilder,
            HybridBaseQueryBuilder hybridBaseQueryBuilder,
            ElasticsearchSearchRequestBuilder searchRequestBuilder,
            DefaultSearchResponseMapper searchResponseMapper,
            AiSearchProperties properties
    ) {
        this.client = client;
        this.queryEmbeddingService = queryEmbeddingService;
        this.filterQueryBuilder = filterQueryBuilder;
        this.hybridBaseQueryBuilder = hybridBaseQueryBuilder;
        this.searchRequestBuilder = searchRequestBuilder;
        this.searchResponseMapper = searchResponseMapper;
        this.properties = properties;
    }

    protected SearchPageResult searchWithQuery(
            ProductSearchRequest request,
            Pageable pageable
    ) throws IOException {
        CategoryBoostingResult decision = buildDecision(request);
        try {
            Query baseQuery = buildBaseQuery(request);
            co.elastic.clients.elasticsearch.core.SearchRequest esSearchRequest = searchRequestBuilder.buildHybridRequest(
                    getReadAlias(),
                    baseQuery,
                    decision,
                    queryEmbeddingService.toQueryEmbedding(request.query()),
                    (int) pageable.getOffset(),
                    pageable.getPageSize()
            );
            return execute(esSearchRequest, pageable);
        } catch (QueryEmbeddingUnavailableException e) {
            log.warn("Query embedding unavailable. Falling back to lexical search. query={}", request.query(), e);
            return lexicalFallbackSearch(request, pageable, decision);
        }
    }

    protected abstract Query buildBaseQuery(ProductSearchRequest request);

    protected abstract CategoryBoostingResult buildDecision(ProductSearchRequest request);

    public SearchPageResult search(ProductSearchRequest searchRequest, Pageable pageable) {
        try {
            if (searchRequest.hasQuery()) {
                return searchWithQuery(searchRequest, pageable);
            }
            return filterOnlySearch(searchRequest, pageable);
        } catch (IOException e) {
            throw new IllegalStateException("검색 요청 실패", e);
        }
    }

    protected SearchPageResult filterOnlySearch(
            ProductSearchRequest request,
            Pageable pageable
    ) throws IOException {
        co.elastic.clients.elasticsearch.core.SearchRequest esSearchRequest = searchRequestBuilder.buildFilterOnlyRequest(
                getReadAlias(),
                filterQueryBuilder.buildRootQuery(request),
                request.sortOption(),
                (int) pageable.getOffset(),
                pageable.getPageSize()
        );
        return execute(esSearchRequest, pageable);
    }

    protected SearchPageResult lexicalFallbackSearch(
            ProductSearchRequest request,
            Pageable pageable,
            CategoryBoostingResult decision
    ) throws IOException {
        Query lexicalFallbackQuery = hybridBaseQueryBuilder.buildLexicalFallback(
                request,
                filterQueryBuilder.buildFilterQuery(request)
        );
        co.elastic.clients.elasticsearch.core.SearchRequest esSearchRequest = searchRequestBuilder.buildFilterOnlyRequest(
                getReadAlias(),
                lexicalFallbackQuery,
                decision.searchSortOption(),
                (int) pageable.getOffset(),
                pageable.getPageSize()
        );
        return execute(esSearchRequest, pageable);
    }

    protected SearchPageResult execute(
            co.elastic.clients.elasticsearch.core.SearchRequest esSearchRequest,
            Pageable pageable
    ) throws IOException {
        SearchResponse<Map> response = client.search(esSearchRequest, Map.class);
        return searchResponseMapper.toPageResult(response, pageable);
    }

    protected String getReadAlias() {
        String readAlias = properties.readAlias();
        if (readAlias == null || readAlias.isBlank()) {
            throw new IllegalStateException("ai-search.read-alias 값이 비어 있습니다.");
        }
        return readAlias;
    }
}
