package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StaticColumnConfig {

    private List<String> columns;
    private String dataSourceKey;
    private int startRow;
    private List<StaticMapping> mappings;

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }

    public String getDataSourceKey() { return dataSourceKey; }
    public void setDataSourceKey(String k) { this.dataSourceKey = k; }

    public int getStartRow() { return startRow; }
    public void setStartRow(int startRow) { this.startRow = startRow; }

    public List<StaticMapping> getMappings() { return mappings; }
    public void setMappings(List<StaticMapping> m) { this.mappings = m; }
}