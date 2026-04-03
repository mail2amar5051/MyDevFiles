package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionalStyleConfig {

    private int priority;
    private List<Integer> offsets;
    private String description;
    private String condition;
    private double value;
    private Double valueTo;
    private CellStyleConfig style;

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public List<Integer> getOffsets() { return offsets; }
    public void setOffsets(List<Integer> offsets) { this.offsets = offsets; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public Double getValueTo() { return valueTo; }
    public void setValueTo(Double valueTo) { this.valueTo = valueTo; }
    public CellStyleConfig getStyle() { return style; }
    public void setStyle(CellStyleConfig style) { this.style = style; }
}