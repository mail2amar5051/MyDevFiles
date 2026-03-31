package com.report.engine.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_template")
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "name")
    private String name;

    @Lob
    @Column(name = "template_blob")
    private byte[] templateBlob;

    @Column(name = "version")
    private Integer version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // --- Getters and Setters ---

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getTemplateBlob() {
        return templateBlob;
    }

    public void setTemplateBlob(byte[] templateBlob) {
        this.templateBlob = templateBlob;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
