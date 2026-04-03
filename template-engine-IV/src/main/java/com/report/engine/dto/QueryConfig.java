package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryConfig {

    private QueryDef scenarios;
    private QueryDef placeholders;

    public QueryDef getScenarios() {
        return scenarios;
    }

    public void setScenarios(QueryDef scenarios) {
        this.scenarios = scenarios;
    }

    public QueryDef getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(QueryDef placeholders) {
        this.placeholders = placeholders;
    }
}
