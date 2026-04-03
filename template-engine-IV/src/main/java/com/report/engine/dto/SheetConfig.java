package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SheetConfig {

    private String sourceSheetName;
    private String outputSheetName;
    private StaticColumnConfig staticColumns;
    private CloneBlockConfig cloneBlock;
    private DataRowConfig dataRows;
    private String commonPlaceholders;
    private List<String> preserve;
    private List<ConditionalStyleConfig> conditionalStyles;
    private DirectMappingConfig directMappings;

    public DirectMappingConfig getDirectMappings() { return directMappings; }
    public void setDirectMappings(DirectMappingConfig d) { this.directMappings = d; }

    public List<ConditionalStyleConfig> getConditionalStyles() { return conditionalStyles; }
    public void setConditionalStyles(List<ConditionalStyleConfig> c) { this.conditionalStyles = c; }

    public String getSourceSheetName() { return sourceSheetName; }
    public void setSourceSheetName(String s) { this.sourceSheetName = s; }

    public String getOutputSheetName() { return outputSheetName; }
    public void setOutputSheetName(String s) { this.outputSheetName = s; }

    public StaticColumnConfig getStaticColumns() { return staticColumns; }
    public void setStaticColumns(StaticColumnConfig s) { this.staticColumns = s; }

    public CloneBlockConfig getCloneBlock() { return cloneBlock; }
    public void setCloneBlock(CloneBlockConfig c) { this.cloneBlock = c; }

    public DataRowConfig getDataRows() { return dataRows; }
    public void setDataRows(DataRowConfig d) { this.dataRows = d; }

    public String getCommonPlaceholders() { return commonPlaceholders; }
    public void setCommonPlaceholders(String c) { this.commonPlaceholders = c; }

    public List<String> getPreserve() { return preserve; }
    public void setPreserve(List<String> p) { this.preserve = p; }

    public boolean shouldPreserve(String feature) {
        return preserve != null && preserve.contains(feature);
    }
}