package com.report.engine.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "report_config")
public class ReportConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "template_id")
    private Long templateId;

    @Lob
    @Column(name = "sheet_rules", columnDefinition = "TEXT")
    private String sheetRules;    // The JSON config

    // --- Getters and Setters ---

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getSheetRules() {
        return sheetRules;
    }

    public void setSheetRules(String sheetRules) {
        this.sheetRules = sheetRules;
    }
}
