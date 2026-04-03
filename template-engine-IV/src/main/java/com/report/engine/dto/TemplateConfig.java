package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateConfig {

    private Long templateId;
    private String templateName;
    private QueryConfig queries;
    private List<SheetConfig> sheets;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }


    public QueryConfig getQueries() {
        return queries;
    }

    public void setQueries(QueryConfig queries) {
        this.queries = queries;
    }

    public List<SheetConfig> getSheets() {
        return sheets;
    }

    public void setSheets(List<SheetConfig> sheets) {
        this.sheets = sheets;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
}
