package com.report.engine.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataRowConfig {

    private int startRow;
    private String dataSourceKey;
    private String rowKey;
    private String scenarioKey;
    private List<BlockMapping> blockMappings;
    private List<FormulaColumnConfig> formulaColumns;
    private int headerRow;

    public int getHeaderRow() { return headerRow; }
    public void setHeaderRow(int headerRow) { this.headerRow = headerRow; }

    public int getStartRow() { return startRow; }
    public void setStartRow(int startRow) { this.startRow = startRow; }

    public String getDataSourceKey() { return dataSourceKey; }
    public void setDataSourceKey(String k) { this.dataSourceKey = k; }

    public String getRowKey() { return rowKey; }
    public void setRowKey(String rowKey) { this.rowKey = rowKey; }

    public String getScenarioKey() { return scenarioKey; }
    public void setScenarioKey(String scenarioKey) { this.scenarioKey = scenarioKey; }

    public List<BlockMapping> getBlockMappings() { return blockMappings; }
    public void setBlockMappings(List<BlockMapping> b) { this.blockMappings = b; }

    public List<FormulaColumnConfig> getFormulaColumns() { return formulaColumns; }
    public void setFormulaColumns(List<FormulaColumnConfig> f) { this.formulaColumns = f; }


}
