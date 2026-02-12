package com.example.aisearch.controller.dto;

import com.example.aisearch.service.synonym.SynonymReloadResult;

public record ReloadSynonymsResponseDto(
        boolean updated,
        boolean reloaded,
        String mode,
        String synonymsSet,
        String index,
        int ruleCount,
        String message
) {
    public static ReloadSynonymsResponseDto from(SynonymReloadResult result) {
        return new ReloadSynonymsResponseDto(
                result.updated(),
                result.reloaded(),
                result.mode(),
                result.synonymsSet(),
                result.index(),
                result.ruleCount(),
                result.message()
        );
    }
}
