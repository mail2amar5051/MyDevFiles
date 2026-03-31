package com.report.engine.processor;

import com.report.engine.dto.CloneBlockConfig;
import com.report.engine.dto.SheetConfig;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Clones a block of columns (e.g. C:Y) for each scenario.
 *
 * Processing order:
 *   1. Clone columns right-to-left (to avoid shifting issues)
 *   2. Copy cell values, styles, types
 *   3. Clone merged regions
 *   4. Shift formulas
 *   5. Copy column widths
 *   6. Replace per-clone placeholders (scenario name)
 *
 * Static columns (A, B) are never touched.
 */
public class ColumnBlockCloner {

    private static final Logger log = LoggerFactory.getLogger(ColumnBlockCloner.class);
    private final MergedRegionHandler mergedRegionHandler;
    private final PlaceholderReplacer placeholderReplacer;
    private final CellCopier cellCopier;

    public ColumnBlockCloner(Workbook workbook) {
        this.mergedRegionHandler = new MergedRegionHandler();
        this.placeholderReplacer = new PlaceholderReplacer();
        this.cellCopier = new CellCopier(workbook);
    }

    /**
     * Clones the template column block for each scenario.
     *
     * @param sheet       the sheet to process
     * @param config      sheet-level config (contains cloneBlock and preserve flags)
     * @param scenarios   list of scenario maps from DB (each has scenarioName, etc.)
     */
    public void cloneColumns(Sheet sheet, SheetConfig config, List<Map<String, Object>> scenarios) {
        if (config.getCloneBlock() == null || CollectionUtils.isEmpty(scenarios)) {
            return;
        }
        log.info("Cloning column block for {} scenarios", scenarios.size());
        CloneBlockConfig cloneBlock = config.getCloneBlock();

        int templateStart = cloneBlock.getStartColIndex();
        int templateEnd = cloneBlock.getEndColIndex();
        int blockWidth = cloneBlock.getBlockWidth();
        int cloneCount = scenarios.size();

        // Scenario 0 uses the original template block.
        // Scenarios 1..N-1 are cloned to the right.
        // We clone right-to-left to avoid index shifting problems.

        for (int i = cloneCount - 1; i >= 1; i--) {
            int targetStart = templateStart + (i * blockWidth);

            // Step 1: Copy all cells from template block to target position
            copyCellBlock(sheet, templateStart, templateEnd, targetStart);

            // Step 2: Copy column widths
            if (config.shouldPreserve("columnWidths")) {
                copyColumnWidths(sheet, templateStart, templateEnd, targetStart);
            }

            // Step 3: Clone merged regions
            if (config.shouldPreserve("mergedRegions")) {
                mergedRegionHandler.cloneMergedRegions(sheet, templateStart, templateEnd, targetStart);
            }

            // Step 4: Shift formula references in the cloned block
            if (config.shouldPreserve("formulas")) {
                shiftFormulasInBlock(sheet, targetStart, targetStart + blockWidth - 1,
                        i * blockWidth, templateStart, templateEnd);
            }
        }

        // Step 5: Replace per-clone placeholders (scenario name) in each block
        String scenarioPlaceholder = cloneBlock.getScenarioNamePlaceholder();
        String scenarioField = cloneBlock.getScenarioNameField();

        for (int i = 0; i < cloneCount; i++) {
            int blockStart = templateStart + (i * blockWidth);
            int blockEnd = blockStart + blockWidth - 1;

            String scenarioName = String.valueOf(scenarios.get(i).get(scenarioField));

            placeholderReplacer.replaceInColumnRange(sheet, blockStart, blockEnd,
                    Map.of(scenarioPlaceholder, scenarioName));
        }
    }

    /**
     * Copies all cells from the source column range to the target position.
     * Iterates over every row in the sheet.
     */
    private void copyCellBlock(Sheet sheet, int srcStart, int srcEnd, int targetStart) {
        int lastRowNum = sheet.getLastRowNum();

        for (int rowIdx = 0; rowIdx <= lastRowNum; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) {
                continue;
            }

            for (int colOffset = 0; colOffset <= (srcEnd - srcStart); colOffset++) {
                Cell sourceCell = row.getCell(srcStart + colOffset);
                Cell targetCell = row.createCell(targetStart + colOffset);

                if (sourceCell != null) {
                    cellCopier.copyCell(sourceCell, targetCell);
                }
            }
        }
    }

    /**
     * Copies column widths from the source range to the target range.
     */
    private void copyColumnWidths(Sheet sheet, int srcStart, int srcEnd, int targetStart) {
        for (int colOffset = 0; colOffset <= (srcEnd - srcStart); colOffset++) {
            int srcWidth = sheet.getColumnWidth(srcStart + colOffset);
            sheet.setColumnWidth(targetStart + colOffset, srcWidth);

            // Also copy hidden state
            boolean hidden = sheet.isColumnHidden(srcStart + colOffset);
            sheet.setColumnHidden(targetStart + colOffset, hidden);
        }
    }

    /**
     * Shifts formula references within the cloned block.
     * Only shifts references that originally pointed within the template range.
     */
    private void shiftFormulasInBlock(Sheet sheet, int blockStart, int blockEnd,
                                      int shiftBy, int sourceStart, int sourceEnd) {
        int lastRowNum = sheet.getLastRowNum();

        for (int rowIdx = 0; rowIdx <= lastRowNum; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) {
                continue;
            }

            for (int colIdx = blockStart; colIdx <= blockEnd; colIdx++) {
                Cell cell = row.getCell(colIdx);
                if (cell != null && cell.getCellType() == CellType.FORMULA) {
                    String original = cell.getCellFormula();
                    String shifted = FormulaShifter.shiftFormula(original, shiftBy,
                            sourceStart, sourceEnd);
                    cell.setCellFormula(shifted);
                }
            }
        }
    }
}
