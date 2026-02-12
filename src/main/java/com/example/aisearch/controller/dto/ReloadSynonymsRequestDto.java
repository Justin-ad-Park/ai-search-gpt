package com.example.aisearch.controller.dto;

import com.example.aisearch.service.synonym.SynonymReloadMode;
import com.example.aisearch.service.synonym.SynonymReloadRequest;

public record ReloadSynonymsRequestDto(
        String mode,
        String index,
        String synonymsSet
) {
    public SynonymReloadRequest toServiceRequest() {
        SynonymReloadMode parsedMode = SynonymReloadMode.fromNullable(mode);
        return new SynonymReloadRequest(parsedMode, index, synonymsSet);
    }
}
