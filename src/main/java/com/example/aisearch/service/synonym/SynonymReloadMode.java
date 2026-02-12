package com.example.aisearch.service.synonym;

import java.util.Locale;

public enum SynonymReloadMode {
    PRODUCTION,
    REGRESSION;

    public static SynonymReloadMode fromNullable(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return PRODUCTION;
        }
        try {
            return SynonymReloadMode.valueOf(rawMode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid reload mode: " + rawMode);
        }
    }
}
