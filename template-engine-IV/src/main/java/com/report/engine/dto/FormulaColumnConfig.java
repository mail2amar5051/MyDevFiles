package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FormulaColumnConfig {

    private int offset;
    private String description;
    private String formula;
    private String source;

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }

    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }

    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
