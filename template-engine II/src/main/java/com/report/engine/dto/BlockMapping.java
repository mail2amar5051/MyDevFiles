package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockMapping {

    private int offset;
    private String dbColumn;

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }

    public String getDbColumn() { return dbColumn; }
    public void setDbColumn(String dbColumn) { this.dbColumn = dbColumn; }
}