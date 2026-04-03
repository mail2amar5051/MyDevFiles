package com.report.engine.config;

/**
 * Thrown when template processing fails.
 * Carries context about which sheet/block/step failed.
 */
public class TemplateProcessingException extends RuntimeException {

    private final String sheetName;
    private final String phase;

    public TemplateProcessingException(String message) {
        super(message);
        this.sheetName = null;
        this.phase = null;
    }

    public TemplateProcessingException(String sheetName, String phase, String message) {
        super(String.format("[Sheet: %s, Phase: %s] %s", sheetName, phase, message));
        this.sheetName = sheetName;
        this.phase = phase;
    }

    public TemplateProcessingException(String sheetName, String phase, String message, Throwable cause) {
        super(String.format("[Sheet: %s, Phase: %s] %s", sheetName, phase, message), cause);
        this.sheetName = sheetName;
        this.phase = phase;
    }

    public String getSheetName() {
        return sheetName;
    }

    public String getPhase() {
        return phase;
    }
}
