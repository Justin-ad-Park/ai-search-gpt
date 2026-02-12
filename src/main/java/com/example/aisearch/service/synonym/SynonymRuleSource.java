package com.example.aisearch.service.synonym;

import java.util.List;

public interface SynonymRuleSource {
    List<String> loadRules(SynonymReloadMode mode);
}
