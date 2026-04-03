package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StaticMapping {

    private String column;
    private String dbColumn;

    public String getColumn() { return column; }
    public void setColumn(String column) { this.column = column; }

    public String getDbColumn() { return dbColumn; }
    public void setDbColumn(String dbColumn) { this.dbColumn = dbColumn; }
}
