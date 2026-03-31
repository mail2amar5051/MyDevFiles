package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloneBlockConfig {

    private boolean isCloneRequired = false; // true if cloning is needed, false to skip cloning
    private String columns;                   // e.g. "C:Y"
    private String repeatForEach;             // references query key e.g. "scenarios"
    private String scenarioNamePlaceholder;   // e.g. "{{scenarioname}}"
    private String scenarioNameField;         // e.g. "scenarioName" - the field in each scenario map

    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public String getRepeatForEach() {
        return repeatForEach;
    }

    public void setRepeatForEach(String repeatForEach) {
        this.repeatForEach = repeatForEach;
    }

    public String getScenarioNamePlaceholder() {
        return scenarioNamePlaceholder;
    }

    public void setScenarioNamePlaceholder(String scenarioNamePlaceholder) {
        this.scenarioNamePlaceholder = scenarioNamePlaceholder;
    }

    public String getScenarioNameField() {
        return scenarioNameField;
    }

    public void setScenarioNameField(String scenarioNameField) {
        this.scenarioNameField = scenarioNameField;
    }

    // --- Helpers ---

    /**
     * Parses "C:Y" → returns start column index (e.g. 2 for C)
     */
    public int getStartColIndex() {
        String startCol = columns.split(":")[0].trim();
        return columnLetterToIndex(startCol);
    }

    /**
     * Parses "C:Y" → returns end column index (e.g. 24 for Y)
     */
    public int getEndColIndex() {
        String endCol = columns.split(":")[1].trim();
        return columnLetterToIndex(endCol);
    }

    /**
     * Returns the width of the block (number of columns).
     */
    public int getBlockWidth() {
        return getEndColIndex() - getStartColIndex() + 1;
    }

    /**
     * Converts column letter(s) to 0-based index.
     * A=0, B=1, ... Z=25, AA=26, AB=27, etc.
     */
    public static int columnLetterToIndex(String letters) {
        int index = 0;
        for (char c : letters.toUpperCase().toCharArray()) {
            index = index * 26 + (c - 'A' + 1);
        }
        return index - 1;
    }

    /**
     * Converts 0-based index to column letter(s).
     * 0=A, 1=B, ... 25=Z, 26=AA, 27=AB, etc.
     */
    public static String indexToColumnLetter(int index) {
        StringBuilder sb = new StringBuilder();
        index++;
        while (index > 0) {
            index--;
            sb.insert(0, (char) ('A' + index % 26));
            index /= 26;
        }
        return sb.toString();
    }

    public boolean isCloneRequired() {
        return isCloneRequired;
    }

    public void setCloneRequired(boolean cloneRequired) {
        isCloneRequired = cloneRequired;
    }
}
