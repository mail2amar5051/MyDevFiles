package com.report.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.engine.dto.SheetConfig;
import com.report.engine.dto.TemplateConfig;
import com.report.engine.config.TemplateProcessingException;
import com.report.engine.processor.*;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private void processSheet1(XSSFWorkbook workbook, SheetConfig sheetConfig,
                              List<Map<String, Object>> scenarios,
                              Map<String, String> commonPlaceholders,
                              Long reportId) {

        String sheetName = sheetConfig.getSourceSheetName();

        // Step 1: Find sheet
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new TemplateProcessingException(sheetName, "INIT",
                    "Sheet '" + sheetName + "' not found in template");
        }

        // Step 2: Rename sheet
        if (sheetConfig.getOutputSheetName() != null
                && !sheetConfig.getOutputSheetName().equals(sheetName)) {
            int idx = workbook.getSheetIndex(sheet);
            workbook.setSheetName(idx, sheetConfig.getOutputSheetName());
        }

        // Step 3: Clone column blocks
        if (sheetConfig.getCloneBlock() != null
                && scenarios != null && !scenarios.isEmpty()) {
            ColumnBlockCloner cloner = new ColumnBlockCloner(workbook);
            cloner.cloneColumns(sheet, sheetConfig, scenarios);
        }

        // Step 4: Replace common placeholders
        placeholderReplacer.replaceInSheet(sheet, commonPlaceholders);

        // Step 5: Render static columns (A, B)
        int dataRowCount = 0;
        if (sheetConfig.getStaticColumns() != null) {
//            List<Map<String, Object>> lineItems = dataFetchService.fetchList(
//                    sheetConfig.getStaticColumns().getDataSourceKey(), reportId);
            List<Map<String, Object>> lineItems = SampleDataBuilder.buildLineItems();


            StaticColumnRenderer staticRenderer = new StaticColumnRenderer();
            staticRenderer.render(sheet, sheetConfig.getStaticColumns(), lineItems);
            dataRowCount = lineItems.size();
        }

        // Step 6: Render data values into scenario blocks
        if (sheetConfig.getDataRows() != null
                && sheetConfig.getCloneBlock() != null) {

//            List<Map<String, Object>> dataRows = dataFetchService.fetchList(
//                    sheetConfig.getDataRows().getDataSourceKey(), reportId);
            List<Map<String, Object>> dataRows = SampleDataBuilder.buildScenarioData();

            String scenarioField = sheetConfig.getCloneBlock().getScenarioNameField();
            List<String> scenarioNames = scenarios.stream()
                    .map(s -> String.valueOf(s.get(scenarioField)))
                    .collect(Collectors.toList());

            Set<Integer> frozenOffsets = Collections.emptySet();
            if (sheetConfig.getCloneBlock() != null) {
                frozenOffsets = TemplateBlockScanner.detectAllFormulaOffsets(
                        sheet,
                        sheetConfig.getCloneBlock().getStartColIndex(),
                        sheetConfig.getCloneBlock().getEndColIndex());
            }

            DataRowRenderer dataRenderer = new DataRowRenderer(frozenOffsets);
            dataRenderer.render(sheet, sheetConfig.getDataRows(),
                    sheetConfig.getCloneBlock(), scenarioNames, dataRows);

            if (dataRowCount == 0) {
                dataRowCount = dataRenderer.getDataRowCount(
                        sheetConfig.getDataRows(), dataRows);
            }
        }

        // Step 7: Render/verify formula columns
        if (sheetConfig.getDataRows() != null
                && sheetConfig.getDataRows().getFormulaColumns() != null
                && !sheetConfig.getDataRows().getFormulaColumns().isEmpty()
                && dataRowCount > 0) {

            int startRow = sheetConfig.getDataRows().getStartRow() - 1;
            int endRow = startRow + dataRowCount - 1;

            FormulaRenderer formulaRenderer = new FormulaRenderer();
            formulaRenderer.render(sheet,
                    sheetConfig.getDataRows().getFormulaColumns(),
                    sheetConfig.getCloneBlock(),
                    scenarios.size(),
                    startRow,
                    endRow);
        }

        // Step 8: Recalculate formulas (LAST)
        try {
            XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
        } catch (Exception e) {
            sheet.setForceFormulaRecalculation(true);
            log.warn("Flagged sheet for recalc on open: {}", e.getMessage());
        }
    }

    private void processSheet(XSSFWorkbook workbook, SheetConfig sheetConfig,
                              List<Map<String, Object>> scenarios,
                              Map<String, String> commonPlaceholders,
                              Long reportId) {

        String sheetName = sheetConfig.getSourceSheetName();

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new TemplateProcessingException(sheetName, "INIT",
                    "Sheet '" + sheetName + "' not found in template");
        }

        // ── SCAN TEMPLATE BEFORE CLONING ──
        // Detect which offsets in the block contain formulas

        Set<Integer> frozenOffsets = Collections.emptySet();
        if (sheetConfig.getCloneBlock() != null) {
            frozenOffsets = TemplateBlockScanner.detectAllFormulaOffsets(
                    sheet,
                    sheetConfig.getCloneBlock().getStartColIndex(),
                    sheetConfig.getCloneBlock().getEndColIndex());
            log.info("Auto-detected frozen offsets: {}", frozenOffsets);
        }

        // Step 1: Rename sheet
        if (sheetConfig.getOutputSheetName() != null
                && !sheetConfig.getOutputSheetName().equals(sheetName)) {
            int idx = workbook.getSheetIndex(sheet);
            workbook.setSheetName(idx, sheetConfig.getOutputSheetName());
        }

        // Step 2: Clone column blocks
        if (sheetConfig.getCloneBlock() != null
                && scenarios != null && !scenarios.isEmpty()) {
            ColumnBlockCloner cloner = new ColumnBlockCloner(workbook);
            cloner.cloneColumns(sheet, sheetConfig, scenarios);
        }

        // Step 3: Replace common placeholders
        placeholderReplacer.replaceInSheet(sheet, commonPlaceholders);

        // Step 4: Render static columns (A, B)
        int dataRowCount = 0;
        if (sheetConfig.getStaticColumns() != null) {
//            List<Map<String, Object>> lineItems = dataFetchService.fetchList(
//                    sheetConfig.getStaticColumns().getDataSourceKey(), reportId);
            List<Map<String, Object>> lineItems = SampleDataBuilder.buildLineItems();

            StaticColumnRenderer staticRenderer = new StaticColumnRenderer();
            staticRenderer.render(sheet, sheetConfig.getStaticColumns(), lineItems);
            dataRowCount = lineItems.size();
        }

        // Step 5: Render data — pass frozenOffsets so formulas are never overwritten
        if (sheetConfig.getDataRows() != null
                && sheetConfig.getCloneBlock() != null) {

//            List<Map<String, Object>> dataRows = dataFetchService.fetchList(
//                    sheetConfig.getDataRows().getDataSourceKey(), reportId);

            List<Map<String, Object>> dataRows = SampleDataBuilder.buildScenarioData();
            String scenarioField = sheetConfig.getCloneBlock().getScenarioNameField();
            List<String> scenarioNames = scenarios.stream()
                    .map(s -> String.valueOf(s.get(scenarioField)))
                    .collect(Collectors.toList());

            DataRowRenderer dataRenderer = new DataRowRenderer(frozenOffsets);
            dataRenderer.render(sheet, sheetConfig.getDataRows(),
                    sheetConfig.getCloneBlock(), scenarioNames, dataRows);

            if (dataRowCount == 0) {
                dataRowCount = dataRenderer.getDataRowCount(
                        sheetConfig.getDataRows(), dataRows);
            }
        }

        // Step 6: Render/verify formula columns
        if (sheetConfig.getDataRows() != null
                && sheetConfig.getDataRows().getFormulaColumns() != null
                && !sheetConfig.getDataRows().getFormulaColumns().isEmpty()
                && dataRowCount > 0) {

            int startRow = sheetConfig.getDataRows().getStartRow() - 1;
            int endRow = startRow + dataRowCount - 1;

            FormulaRenderer formulaRenderer = new FormulaRenderer();
            formulaRenderer.render(sheet,
                    sheetConfig.getDataRows().getFormulaColumns(),
                    sheetConfig.getCloneBlock(),
                    scenarios.size(),
                    startRow,
                    endRow);
        }

        // Step 7: Recalculate formulas (LAST)
        try {
            XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
        } catch (Exception e) {
            sheet.setForceFormulaRecalculation(true);
            log.warn("Flagged sheet for recalc on open: {}", e.getMessage());
        }
    }

    // java
    public byte[] generateReport(Long reportId) throws IOException {
        String configResourcePath = "/sample-config1.json";
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
                        processSheet(workbook, sheetConfig, scenarios, commonPlaceholders, reportId);
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
