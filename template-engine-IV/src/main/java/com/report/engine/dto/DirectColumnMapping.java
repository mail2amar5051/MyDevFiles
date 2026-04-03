package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DirectColumnMapping {

    private String column;     // "E", "F", "O", etc.
    private String dbColumn;   // "base_pq1", "stress_pq1", etc.

    public String getColumn() { return column; }
    public void setColumn(String c) { this.column = c; }
    public String getDbColumn() { return dbColumn; }
    public void setDbColumn(String d) { this.dbColumn = d; }
}
