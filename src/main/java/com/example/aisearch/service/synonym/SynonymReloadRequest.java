package com.example.aisearch.service.synonym;

public record SynonymReloadRequest(
        SynonymReloadMode mode,
        String synonymsSet
) {
    public static SynonymReloadRequest defaultRequest() {
        return new SynonymReloadRequest(SynonymReloadMode.PRODUCTION, null);
    }
}
