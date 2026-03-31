package com.report.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.engine.dto.SheetConfig;
import com.report.engine.dto.TemplateConfig;
import com.report.engine.config.TemplateProcessingException;
import com.report.engine.processor.ColumnBlockCloner;
import com.report.engine.processor.PlaceholderReplacer;
//import com.report.engine.repository.ReportConfigRepository;
//import com.report.engine.repository.ReportTemplateRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the entire report generation flow:
 *
 *   1. Fetch template BLOB from DB
 *   2. Fetch rules JSON from DB → parse into TemplateConfig
 *   3. Fetch runtime data (scenarios list + placeholders map) from DB
 *   4. Open template as XSSFWorkbook
 *   5. For each sheet config:
 *        a. Rename sheet if outputSheetName is specified
 *        b. Clone column blocks for each scenario
 *        c. Replace common placeholders across the sheet
 *        d. Force formula recalculation
 *   6. Write workbook to byte[] and return
 */
@Service
public class TemplateEngineService {

    private static final Logger log = LoggerFactory.getLogger(TemplateEngineService.class);

//    @Autowired
//    private ReportTemplateRepository templateRepository;
//
//    @Autowired
//    private ReportConfigRepository configRepository;

    @Autowired
    private DataFetchService dataFetchService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlaceholderReplacer placeholderReplacer = new PlaceholderReplacer();

    /**
     * Main entry point: generates a report and returns the Excel file as bytes.
     *
     * @param reportId  the report configuration ID
     * @return byte array of the generated .xlsx file
     */
    /*
    public byte[] generateReport1(Long reportId) throws IOException {

        // ── Step 1: Fetch report config ──
        ReportConfig reportConfig = configRepository.findById(reportId)
                .orElseThrow(() -> new IOException("Report config not found: " + reportId));

        // ── Step 2: Parse rules JSON ──
        TemplateConfig config = objectMapper.readValue(reportConfig.getSheetRules(), TemplateConfig.class);
        log.info("Parsed template config for reportId={}, templateId={}", reportId, config.getTemplateId());

        // ── Step 3: Fetch template BLOB ──
        ReportTemplate template = templateRepository.findById(config.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Template not found: " + config.getTemplateId()));

        // ── Step 4: Fetch runtime data from DB ──
        /*
        List<Map<String, Object>> scenarios = dataFetchService.fetchList(
                config.getQueries().getScenarios().getQueryKey(), reportId);

        Map<String, String> commonPlaceholders = dataFetchService.fetchPlaceholderMap(
                config.getQueries().getPlaceholders().getQueryKey(), reportId);

        // For testing, create sample data instead of fetching from DB
        List<Map<String, Object>> scenarios = List.of(
                Map.of("scenarioName", "Base Case"),
                Map.of("scenarioName", "Optimistic"),
                Map.of("scenarioName", "Pessimistic"));

        Map<String, String> commonPlaceholders = Map.of(
                "{{reportTitle}}", "Projections Schedule -CG",
                "{{currentcycle}}", "FY2026",
                "{{priorcycle}}", "FY2025"
        );

        log.info("Fetched {} scenarios and {} common placeholders", scenarios.size(), commonPlaceholders.size());

        // ── Step 5: Open workbook from BLOB ──
        XSSFWorkbook workbook;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(template.getTemplateBlob())) {
            workbook = new XSSFWorkbook(bis);
        }

        // ── Step 6: Process each sheet ──
        for (SheetConfig sheetConfig : config.getSheets()) {
            processSheet(workbook, sheetConfig, scenarios, commonPlaceholders);
        }

        // ── Step 7: Write to byte array ──
        byte[] output;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            output = bos.toByteArray();
        } finally {
            workbook.close();
        }

        log.info("Report generated successfully for reportId={}, size={} bytes", reportId, output.length);
        return output;
    }
*/
    /**
     * Processes a single sheet:
     *   1. Find or rename the sheet
     *   2. Clone column blocks (if configured)
     *   3. Replace common placeholders
     *   4. Recalculate formulas
     */
    private void processSheet(XSSFWorkbook workbook, SheetConfig sheetConfig,
                              List<Map<String, Object>> scenarios,
                              Map<String, String> commonPlaceholders) {

        log.info("Processing sheet config: source='{}', output='{}', cloneBlock={}'",
                sheetConfig.getSourceSheetName(), sheetConfig.getOutputSheetName(), sheetConfig.getCloneBlock());
        // Find the sheet by source name
        String sourceName = sheetConfig.getSourceSheetName();
        Sheet sheet = workbook.getSheet(sourceName);
        if (sheet == null) {
            throw new TemplateProcessingException(sourceName, "INIT",
                    "Sheet '" + sourceName + "' not found in template");
        }

        // Rename sheet if output name is different
        if (sheetConfig.getOutputSheetName() != null
                && !sheetConfig.getOutputSheetName().equals(sourceName)) {
            int sheetIndex = workbook.getSheetIndex(sheet);
            workbook.setSheetName(sheetIndex, sheetConfig.getOutputSheetName());
            log.info("Renamed sheet '{}' → '{}'", sourceName, sheetConfig.getOutputSheetName());
        }

        // Clone column blocks for each scenario
        if (sheetConfig.getCloneBlock() != null && scenarios != null && !scenarios.isEmpty()) {
            ColumnBlockCloner cloner = new ColumnBlockCloner(workbook);
                cloner.cloneColumns(sheet, sheetConfig, scenarios);
            log.info("Cloned column block {} times for sheet '{}'",
                    scenarios.size(), sheetConfig.getOutputSheetName());
        }

        // Replace common placeholders across the entire sheet
        placeholderReplacer.replaceInSheet(sheet, commonPlaceholders);
        log.info("Replaced {} common placeholders in sheet '{}'",
                commonPlaceholders.size(), sheetConfig.getOutputSheetName());

        // Force formula recalculation
        try {
            XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
        } catch (Exception e) {
            // Some formulas may reference external data or be complex.
            // Set the flag so Excel recalculates on open.
            sheet.setForceFormulaRecalculation(true);
            log.warn("Could not evaluate all formulas in sheet '{}', flagged for recalc on open",
                    sheetConfig.getOutputSheetName());
        }
    }


    // java
    public byte[] generateReport(Long reportId) throws IOException {
        String configResourcePath = "/sample-config.json";
        String templateResourcePath = "/templates/Test_AS.xlsx";

        // Load and parse JSON config from classpath
        try (InputStream configStream = getClass().getResourceAsStream(configResourcePath)) {
            if (configStream == null) {
                throw new IOException("Config resource not found: " + configResourcePath);
            }
            TemplateConfig config = objectMapper.readValue(configStream, TemplateConfig.class);
            log.info("Parsed template config from resource {} for reportId={}, templateId={}",
                    configResourcePath, reportId, config.getTemplateId());

            // Load workbook from classpath
            try (InputStream templateStream = getClass().getResourceAsStream(templateResourcePath)) {
                if (templateStream == null) {
                    throw new IOException("Template resource not found: " + templateResourcePath);
                }

                try (XSSFWorkbook workbook = new XSSFWorkbook(templateStream)) {

                    // For testing, use sample runtime data (no DB required)
                    List<Map<String, Object>> scenarios = List.of(
                            Map.of("scenarioName", "Base Case"),
                            Map.of("scenarioName", "Optimistic"),
                            Map.of("scenarioName", "Pessimistic"));

                    Map<String, String> commonPlaceholders = Map.of(
                            "{{reportTitle}}", "Projections Schedule -CG",
                            "{{currentcycle}}", "FY2026",
                            "{{priorcycle}}", "FY2025",
                            "{{currentcycle vs priorcycle}}", "FY2026 vs FY2025",
                            "{{currentcycle vs priorcycle %}}", "FY2026 vs FY2025 %",
                            "{{currentcycle}} BHC BASE", "FY2026 BHC BASE",
                            "{{currentcycle}} BHC STRESS", "FY2026 BHC STRESS"
                    );

                    log.info("Fetched {} scenarios and {} common placeholders", scenarios.size(), commonPlaceholders.size());

                    // Process each sheet defined in the config
                    for (SheetConfig sheetConfig : config.getSheets()) {
                        processSheet(workbook, sheetConfig, scenarios, commonPlaceholders);
                    }

                    // Write workbook to byte array and return
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                        workbook.write(bos);
                        byte[] output = bos.toByteArray();
                        log.info("Report generated successfully for reportId={}, size={} bytes", reportId, output.length);
                        return output;
                    }
                }
            }
        }
    }

}
