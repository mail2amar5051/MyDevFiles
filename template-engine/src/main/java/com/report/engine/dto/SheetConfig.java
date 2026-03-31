package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SheetConfig {

    private String sourceSheetName;
    private String outputSheetName;
    private List<String> staticColumns;   // e.g. ["A", "B"]
    private CloneBlockConfig cloneBlock;
    private String commonPlaceholders;    // references queries.placeholders
    private List<String> preserve;        // ["mergedRegions", "formulas", "styles", "columnWidths"]

    public String getSourceSheetName() {
        return sourceSheetName;
    }

    public void setSourceSheetName(String sourceSheetName) {
        this.sourceSheetName = sourceSheetName;
    }

    public String getOutputSheetName() {
        return outputSheetName;
    }

    public void setOutputSheetName(String outputSheetName) {
        this.outputSheetName = outputSheetName;
    }

    public List<String> getStaticColumns() {
        return staticColumns;
    }

    public void setStaticColumns(List<String> staticColumns) {
        this.staticColumns = staticColumns;
    }

    public CloneBlockConfig getCloneBlock() {
        return cloneBlock;
    }

    public void setCloneBlock(CloneBlockConfig cloneBlock) {
        this.cloneBlock = cloneBlock;
    }

    public String getCommonPlaceholders() {
        return commonPlaceholders;
    }

    public void setCommonPlaceholders(String commonPlaceholders) {
        this.commonPlaceholders = commonPlaceholders;
    }

    public List<String> getPreserve() {
        return preserve;
    }

    public void setPreserve(List<String> preserve) {
        this.preserve = preserve;
    }

    // Helper methods

    public boolean shouldPreserve(String feature) {
        return preserve != null && preserve.contains(feature);
    }

}
