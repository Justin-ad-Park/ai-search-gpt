package com.example.aisearch.service.indexing.orchestration;

import com.example.aisearch.service.indexing.bootstrap.ingest.ProductIndexingService;
import com.example.aisearch.service.indexing.domain.AliasSwitcher;
import com.example.aisearch.service.indexing.domain.IndexCleanupService;
import com.example.aisearch.service.indexing.domain.IndexCreator;
import org.springframework.stereotype.Service;

@Service
public class IndexRolloutService {

    private final IndexCreator indexCreator;
    private final AliasSwitcher aliasSwitcher;
    private final IndexCleanupService indexCleanupService;
    private final ProductIndexingService productIndexingService;

    public IndexRolloutService(
            IndexCreator indexCreator,
            AliasSwitcher aliasSwitcher,
            IndexCleanupService indexCleanupService,
            ProductIndexingService productIndexingService
    ) {
        this.indexCreator = indexCreator;
        this.aliasSwitcher = aliasSwitcher;
        this.indexCleanupService = indexCleanupService;
        this.productIndexingService = productIndexingService;
    }

    public IndexRolloutResult rollOutFromSourceData() {
        String oldIndex = aliasSwitcher.findCurrentAliasedIndex();
        String newIndex = indexCreator.createVersionedIndex();

        long indexedCount = productIndexingService.reindexData(newIndex);

        aliasSwitcher.swapReadAlias(oldIndex, newIndex);
        indexCleanupService.deleteIndexIfExists(oldIndex);

        return new IndexRolloutResult(oldIndex, newIndex, indexedCount);
    }
}
