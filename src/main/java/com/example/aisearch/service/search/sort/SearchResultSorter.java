package com.example.aisearch.service.search.sort;

import com.example.aisearch.model.SearchHitResult;
import com.example.aisearch.model.search.SearchRequest;
import com.example.aisearch.model.search.SearchSortOption;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SearchResultSorter {

    private final Map<SearchSortOption, Comparator<SearchHitResult>> comparatorByOption;

    public SearchResultSorter() {
        this.comparatorByOption = new EnumMap<>(SearchSortOption.class);
        comparatorByOption.put(SearchSortOption.RELEVANCE_DESC, relevanceDescComparator());
        comparatorByOption.put(SearchSortOption.PRICE_ASC, priceAscComparator());
        comparatorByOption.put(SearchSortOption.PRICE_DESC, priceDescComparator());
    }

    public List<SearchHitResult> sort(SearchRequest request, List<SearchHitResult> results) {
        Comparator<SearchHitResult> comparator = comparatorByOption.get(request.sortOption());
        if (comparator == null || request.sortOption() == SearchSortOption.RELEVANCE_DESC) {
            return results;
        }
        return results.stream().sorted(comparator).toList();
    }

    private Comparator<SearchHitResult> relevanceDescComparator() {
        return Comparator.comparing(
                SearchHitResult::score,
                Comparator.nullsLast(Comparator.reverseOrder())
        );
    }

    private Comparator<SearchHitResult> priceAscComparator() {
        return Comparator
                .comparing(
                        this::extractPrice,
                        Comparator.nullsLast(Integer::compareTo)
                )
                .thenComparing(relevanceDescComparator());
    }

    private Comparator<SearchHitResult> priceDescComparator() {
        return Comparator
                .comparing(
                        this::extractPrice,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(relevanceDescComparator());
    }

    private Integer extractPrice(SearchHitResult hit) {
        Object value = hit.source().get("price");
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
