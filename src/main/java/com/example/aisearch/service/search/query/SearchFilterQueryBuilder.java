package com.example.aisearch.service.search.query;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.example.aisearch.model.search.SearchPrice;
import com.example.aisearch.model.search.SearchRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SearchFilterQueryBuilder {

    public Query buildFilterQuery(SearchRequest request) {
        List<Query> filters = new ArrayList<>();
        addPriceFilter(request, filters);
        addCategoryFilter(request, filters);

        if (filters.isEmpty()) {
            return null;
        }
        return Query.of(q -> q.bool(b -> b.filter(filters)));
    }

    public Query buildRootQuery(SearchRequest request) {
        Query filterQuery = buildFilterQuery(request);
        if (filterQuery == null) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        return filterQuery;
    }

    private void addPriceFilter(SearchRequest request, List<Query> filters) {
        if (!request.hasPriceCondition()) {
            return;
        }
        SearchPrice price = request.searchPrice();
        filters.add(Query.of(q -> q.range(r -> {
            r.field("price");
            if (price.minPrice() != null) {
                r.gte(JsonData.of(price.minPrice()));   //Greater Than or Equal to 크거나 같은
            }
            if (price.maxPrice() != null) {
                r.lte(JsonData.of(price.maxPrice()));   //Less Than or Equal to 작거나 같은
            }
            return r;
        })));
    }

    private void addCategoryFilter(SearchRequest request, List<Query> filters) {
        if (!request.hasCategoryCondition()) {
            return;
        }
        List<FieldValue> values = request.categoryIds().stream()
                .map(FieldValue::of)
                .toList();

        filters.add(Query.of(q -> q.terms(t -> t
                .field("categoryId")
                .terms(tf -> tf.value(values))      //sql로 비유하면 WHERE categoryId IN (1,2,3)

        )));
    }
}
