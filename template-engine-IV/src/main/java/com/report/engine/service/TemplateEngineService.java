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

        boolean isClonedSheet = sheetConfig.getCloneBlock() != null;
        boolean isDirectSheet = sheetConfig.getDirectMappings() != null;

        // ── SCAN TEMPLATE BEFORE CLONING (cloned sheets only) ──
        Set<Integer> frozenOffsets = Collections.emptySet();
        if (isClonedSheet && sheetConfig.getDataRows() != null) {
            int headerRow = sheetConfig.getDataRows().getHeaderRow() - 1;
            int dataStartRow = sheetConfig.getDataRows().getStartRow() - 1;

            frozenOffsets = TemplateBlockScanner.detectFrozenOffsets(
                    sheet,
                    sheetConfig.getCloneBlock().getStartColIndex(),
                    sheetConfig.getCloneBlock().getEndColIndex(),
                    headerRow,
                    dataStartRow);
            log.info("Auto-detected frozen offsets: {}", frozenOffsets);
        }

        // ── Step 1: Rename sheet ──
        if (sheetConfig.getOutputSheetName() != null
                && !sheetConfig.getOutputSheetName().equals(sheetName)) {
            int idx = workbook.getSheetIndex(sheet);
            workbook.setSheetName(idx, sheetConfig.getOutputSheetName());
        }

        // ── Step 2: Clone column blocks (cloned sheets only) ──
        if (isClonedSheet && scenarios != null && !scenarios.isEmpty()) {
            ColumnBlockCloner cloner = new ColumnBlockCloner(workbook);
            cloner.cloneColumns(sheet, sheetConfig, scenarios);
        }

        // ── Step 3: Replace common placeholders ──
        placeholderReplacer.replaceInSheet(sheet, commonPlaceholders);

        // ── Step 4: Render static columns ──
        int dataRowCount = 0;
        if (sheetConfig.getStaticColumns() != null) {
            // TODO production: dataFetchService.fetchList(
            //     sheetConfig.getStaticColumns().getDataSourceKey(), reportId);
            List<Map<String, Object>> lineItems;
            if (isClonedSheet) {
                lineItems = SampleDataBuilder.buildLineItems();
            } else {
                lineItems = SampleDataBuilder.buildQvsALineItems();
            }

            StaticColumnRenderer staticRenderer = new StaticColumnRenderer();
            staticRenderer.render(sheet, sheetConfig.getStaticColumns(), lineItems);
            dataRowCount = lineItems.size();
        }

        // ── Step 5a: Render block data (cloned sheets) ──
        if (isClonedSheet && sheetConfig.getDataRows() != null) {
            // TODO production: dataFetchService.fetchList(...)
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
            log.info("Rendered block data: {} rows × {} scenarios",
                    dataRowCount, scenarioNames.size());
        }

        // ── Step 5b: Render direct-mapped data (non-cloned sheets) ──
        if (isDirectSheet) {
            // TODO production: dataFetchService.fetchList(...)
            List<Map<String, Object>> directData = SampleDataBuilder.buildQvsAData();

            DirectDataRenderer directRenderer = new DirectDataRenderer();
            directRenderer.render(sheet, sheetConfig.getDirectMappings(), directData);

            if (dataRowCount == 0) {
                dataRowCount = directRenderer.getDataRowCount(
                        sheetConfig.getDirectMappings(), directData);
            }
            log.info("Rendered {} direct-mapped rows", dataRowCount);
        }

        // ── Step 6: Propagate formulas ──
        if (dataRowCount > 1) {
            if (isClonedSheet && !frozenOffsets.isEmpty()) {
                // Cloned sheet: propagate within each scenario block
                int firstDataRow = sheetConfig.getDataRows().getStartRow() - 1;
                int lastDataRow = firstDataRow + dataRowCount - 1;

                FormulaPropagator.propagate(sheet,
                        frozenOffsets,
                        sheetConfig.getCloneBlock(),
                        scenarios.size(),
                        firstDataRow,
                        lastDataRow);
                log.info("Propagated block formulas, rows {}-{}",
                        firstDataRow + 1, lastDataRow + 1);

            } else if (isDirectSheet) {
                // Direct sheet: find formula cells in first row, propagate down
                int firstDataRow = sheetConfig.getDirectMappings().getStartRow() - 1;
                int lastDataRow = firstDataRow + dataRowCount - 1;

                Row formulaRow = sheet.getRow(firstDataRow);
                if (formulaRow != null) {
                    for (Cell cell : formulaRow) {
                        if (cell.getCellType() == CellType.FORMULA) {
                            String formula = cell.getCellFormula();
                            int colIdx = cell.getColumnIndex();

                            for (int r = firstDataRow + 1; r <= lastDataRow; r++) {
                                Row target = sheet.getRow(r);
                                if (target == null) target = sheet.createRow(r);
                                Cell targetCell = target.getCell(colIdx);
                                if (targetCell == null) targetCell = target.createCell(colIdx);

                                String shifted = FormulaPropagator.shiftFormulaRowsPublic(
                                        formula, r - firstDataRow);
                                targetCell.setCellFormula(shifted);
                            }
                        }
                    }
                }
                log.info("Propagated direct formulas, rows {}-{}",
                        firstDataRow + 1, lastDataRow + 1);
            }
        }

        // ── Step 7: Evaluate formulas (so conditional styles can read values) ──
        try {
            XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
        } catch (Exception e) {
            sheet.setForceFormulaRecalculation(true);
            log.warn("Partial formula evaluation: {}", e.getMessage());
        }

        // ── Step 8: Apply conditional styles ──
        if (sheetConfig.getConditionalStyles() != null
                && !sheetConfig.getConditionalStyles().isEmpty()
                && dataRowCount > 0) {

            int firstDataRow;
            int lastDataRow;

            if (isClonedSheet && sheetConfig.getDataRows() != null) {
                firstDataRow = sheetConfig.getDataRows().getStartRow() - 1;
            } else if (isDirectSheet) {
                firstDataRow = sheetConfig.getDirectMappings().getStartRow() - 1;
            } else {
                return;
            }
            lastDataRow = firstDataRow + dataRowCount - 1;

            ConditionalStyleRenderer styleRenderer =
                    new ConditionalStyleRenderer(workbook);

            if (isClonedSheet) {
                // Block-relative offsets across all scenario clones
                styleRenderer.apply(sheet,
                        sheetConfig.getConditionalStyles(),
                        sheetConfig.getCloneBlock(),
                        scenarios.size(),
                        firstDataRow,
                        lastDataRow);
            } else {
                // Direct sheets: offsets are absolute column indices
                // Wrap in a dummy 1-block call (blockStart=0, width=max)
                styleRenderer.applyDirect(sheet,
                        sheetConfig.getConditionalStyles(),
                        firstDataRow,
                        lastDataRow);
            }

            log.info("Applied {} conditional style rules",
                    sheetConfig.getConditionalStyles().size());
        }
    }

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
