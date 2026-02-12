package com.example.aisearch.service.synonym;

public record SynonymReloadResult(
        boolean updated,
        boolean reloaded,
        String mode,
        String synonymsSet,
        String index,
        int ruleCount,
        String message
) {
}
