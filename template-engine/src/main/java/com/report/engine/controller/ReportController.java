package com.report.engine.controller;

import com.report.engine.service.TemplateEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for report generation.
 *
 * GET /api/reports/{reportId}/generate
 *   → Fetches template + rules + data, generates Excel, returns as download.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private TemplateEngineService templateEngineService;

    /**
     * Generates a report and returns it as a downloadable .xlsx file.
     *
     * @param reportId the report configuration ID
     * @return byte[] of the generated Excel file
     */
    @GetMapping("/{reportId}/generate")
    public ResponseEntity<byte[]> generateReport(@PathVariable Long reportId) {
        try {
            log.info("Report generation requested for reportId={}", reportId);

            byte[] excelBytes = templateEngineService.generateReport(reportId);

            String filename = "report_" + reportId + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            log.info("Returning report file '{}' ({} bytes)", filename, excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed to generate report for reportId={}", reportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
