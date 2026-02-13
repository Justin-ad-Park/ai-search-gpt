package com.example.aisearch.service.indexing.bootstrap;

import org.springframework.stereotype.Service;

@Service
public class IndexRolloutService {

    private final IndexManagementService indexManagementService;
    private final ProductIndexingService productIndexingService;

    public IndexRolloutService(
            IndexManagementService indexManagementService,
            ProductIndexingService productIndexingService
    ) {
        this.indexManagementService = indexManagementService;
        this.productIndexingService = productIndexingService;
    }

    public IndexRolloutResult rollOutFromSourceData() {
        String oldIndex = indexManagementService.findCurrentAliasedIndex();
        String newIndex = indexManagementService.createVersionedIndex();

        long indexedCount = productIndexingService.reindexData(newIndex);

        indexManagementService.swapReadAlias(oldIndex, newIndex);
        indexManagementService.deleteIndexIfExists(oldIndex);

        return new IndexRolloutResult(oldIndex, newIndex, indexedCount);
    }
}
