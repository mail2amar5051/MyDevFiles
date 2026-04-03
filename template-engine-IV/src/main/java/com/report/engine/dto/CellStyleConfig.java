package com.report.engine.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CellStyleConfig {

    private String fillColor;
    private String fontColor;
    private Boolean bold;
    private Boolean italic;

    public String getFillColor() { return fillColor; }
    public void setFillColor(String f) { this.fillColor = f; }
    public String getFontColor() { return fontColor; }
    public void setFontColor(String f) { this.fontColor = f; }
    public Boolean getBold() { return bold; }
    public void setBold(Boolean b) { this.bold = b; }
    public Boolean getItalic() { return italic; }
    public void setItalic(Boolean i) { this.italic = i; }
}