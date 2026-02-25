package com.example.aisearch.service.search.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.aisearch.model.search.SearchRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 하이브리드 검색에서 사용할 베이스 bool 쿼리를 생성한다.
 */
@Component
public class HybridBaseQueryBuilder {

    public Query build(SearchRequest request, Optional<Query> filterQuery) {
        Query lexicalQuery = Query.of(q -> q.multiMatch(mm -> mm
                .query(request.query())
                .fields("product_name^2", "description")
        ));

        return Query.of(q -> q.bool(b -> {
            filterQuery.ifPresent(b::filter);
            b.should(lexicalQuery);
            b.minimumShouldMatch("0");
            return b;
        }));
    }
}

