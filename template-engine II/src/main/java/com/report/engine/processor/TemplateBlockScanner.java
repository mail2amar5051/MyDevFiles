package com.report.engine.processor;

import org.apache.poi.ss.usermodel.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Scans the template block BEFORE cloning to detect:
 *   - Which column offsets contain formulas
 *   - Which column offsets contain merged regions
 *   - Which column offsets are empty (no data expected)
 *
 * These offsets are then "frozen" — DataRowRenderer will
 * never write data into them.
 *
 * This makes the engine fully dynamic. No hardcoded skipOffsets needed.
 */
public class TemplateBlockScanner {

    /**
     * Scans all rows within the template block and returns
     * the set of column offsets that contain at least one formula cell.
     *
     * @param sheet      the sheet (before cloning)
     * @param blockStart start column of template block (e.g. 2 for C)
     * @param blockEnd   end column of template block (e.g. 24 for Y)
     * @param dataStartRow first data row (0-based)
     * @return set of offsets (relative to blockStart) that have formulas
     */
    public static Set<Integer> detectFormulaOffsets(Sheet sheet,
                                                    int blockStart,
                                                    int blockEnd,
                                                    int dataStartRow) {
        Set<Integer> formulaOffsets = new LinkedHashSet<>();
        int lastRow = sheet.getLastRowNum();

        // Scan data rows in the template block
        for (int rowIdx = dataStartRow; rowIdx <= lastRow; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;

            for (int colIdx = blockStart; colIdx <= blockEnd; colIdx++) {
                Cell cell = row.getCell(colIdx);
                if (cell != null && cell.getCellType() == CellType.FORMULA) {
                    int offset = colIdx - blockStart;
                    formulaOffsets.add(offset);
                }
            }
        }

        return formulaOffsets;
    }

    /**
     * Scans ALL rows (including headers) to detect formula offsets.
     * Use this when data rows haven't been filled yet but the template
     * has formula placeholders in header/summary rows too.
     */
    public static Set<Integer> detectAllFormulaOffsets(Sheet sheet,
                                                       int blockStart,
                                                       int blockEnd) {
        Set<Integer> formulaOffsets = new LinkedHashSet<>();
        int lastRow = sheet.getLastRowNum();

        for (int rowIdx = 0; rowIdx <= lastRow; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;

            for (int colIdx = blockStart; colIdx <= blockEnd; colIdx++) {
                Cell cell = row.getCell(colIdx);
                if (cell != null && cell.getCellType() == CellType.FORMULA) {
                    formulaOffsets.add(colIdx - blockStart);
                }
            }
        }

        return formulaOffsets;
    }
}
