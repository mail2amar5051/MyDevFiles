package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DirectMappingConfig {

    private int headerRow;
    private int startRow;
    private String dataSourceKey;
    private String rowKey;
    private List<DirectColumnMapping> columns;

    public int getHeaderRow() { return headerRow; }
    public void setHeaderRow(int h) { this.headerRow = h; }
    public int getStartRow() { return startRow; }
    public void setStartRow(int s) { this.startRow = s; }
    public String getDataSourceKey() { return dataSourceKey; }
    public void setDataSourceKey(String d) { this.dataSourceKey = d; }
    public String getRowKey() { return rowKey; }
    public void setRowKey(String r) { this.rowKey = r; }
    public List<DirectColumnMapping> getColumns() { return columns; }
    public void setColumns(List<DirectColumnMapping> c) { this.columns = c; }
}