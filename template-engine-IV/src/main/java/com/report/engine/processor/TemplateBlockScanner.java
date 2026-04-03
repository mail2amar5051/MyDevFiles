//package com.report.engine.processor;
//
//import org.apache.poi.ss.usermodel.*;
//
//import java.util.LinkedHashSet;
//import java.util.Set;
//
///**
// * Scans the template block BEFORE cloning to detect:
// *   - Which column offsets contain formulas
// *   - Which column offsets contain merged regions
// *   - Which column offsets are empty (no data expected)
// *
// * These offsets are then "frozen" — DataRowRenderer will
// * never write data into them.
// *
// * This makes the engine fully dynamic. No hardcoded skipOffsets needed.
// */
//public class TemplateBlockScanner {
//
//    /**
//     * Scans all rows within the template block and returns
//     * the set of column offsets that contain at least one formula cell.
//     *
//     * @param sheet      the sheet (before cloning)
//     * @param blockStart start column of template block (e.g. 2 for C)
//     * @param blockEnd   end column of template block (e.g. 24 for Y)
//     * @param dataStartRow first data row (0-based)
//     * @return set of offsets (relative to blockStart) that have formulas
//     */
//    public static Set<Integer> detectFormulaOffsets(Sheet sheet,
//                                                    int blockStart,
//                                                    int blockEnd,
//                                                    int dataStartRow) {
//        Set<Integer> formulaOffsets = new LinkedHashSet<>();
//        int lastRow = sheet.getLastRowNum();
//
//        // Scan data rows in the template block
//        for (int rowIdx = dataStartRow; rowIdx <= lastRow; rowIdx++) {
//            Row row = sheet.getRow(rowIdx);
//            if (row == null) continue;
//
//            for (int colIdx = blockStart; colIdx <= blockEnd; colIdx++) {
//                Cell cell = row.getCell(colIdx);
//                if (cell != null && cell.getCellType() == CellType.FORMULA) {
//                    int offset = colIdx - blockStart;
//                    formulaOffsets.add(offset);
//                }
//            }
//        }
//
//        return formulaOffsets;
//    }
//
//    /**
//     * Scans ALL rows (including headers) to detect formula offsets.
//     * Use this when data rows haven't been filled yet but the template
//     * has formula placeholders in header/summary rows too.
//     */
//    public static Set<Integer> detectAllFormulaOffsets(Sheet sheet,
//                                                       int blockStart,
//                                                       int blockEnd) {
//        Set<Integer> formulaOffsets = new LinkedHashSet<>();
//        int lastRow = sheet.getLastRowNum();
//
//        for (int rowIdx = 0; rowIdx <= lastRow; rowIdx++) {
//            Row row = sheet.getRow(rowIdx);
//            if (row == null) continue;
//
//            for (int colIdx = blockStart; colIdx <= blockEnd; colIdx++) {
//                Cell cell = row.getCell(colIdx);
//                if (cell != null && cell.getCellType() == CellType.FORMULA) {
//                    formulaOffsets.add(colIdx - blockStart);
//                }
//            }
//        }
//
//        return formulaOffsets;
//    }
//}

package com.report.engine.processor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Scans the template block BEFORE cloning.
 *
 * Simple rule: only offsets where the HEADER ROW has a
 * non-empty, non-merged-span label are data columns.
 * Everything else is frozen.
 */
public class TemplateBlockScanner {

    /**
     * Returns the set of offsets that are FROZEN (should NOT receive data).
     *
     * An offset is frozen if:
     *   - Header cell is null, blank, or empty string
     *   - Header cell is a "spanned" cell inside a merged region
     *     (not the top-left origin of the merge)
     *   - Any data row in that column contains a formula
     */
    public static Set<Integer> detectFrozenOffsets(Sheet sheet,
                                                   int blockStart,
                                                   int blockEnd,
                                                   int headerRow,
                                                   int dataStartRow) {

        Set<Integer> frozen = new LinkedHashSet<>();
        Set<Integer> mergedSpanCols = getMergedSpanColumns(sheet, headerRow,
                blockStart, blockEnd);

        Row hRow = sheet.getRow(headerRow);

        for (int colIdx = blockStart; colIdx <= blockEnd; colIdx++) {
            int offset = colIdx - blockStart;

            // Check 1: Is this column a merged-span cell in the header?
            if (mergedSpanCols.contains(colIdx)) {
                frozen.add(offset);
                continue;
            }

            // Check 2: Is header cell empty/blank/null?
            boolean hasLabel = false;
            if (hRow != null) {
                Cell cell = hRow.getCell(colIdx);
                if (cell != null) {
                    if (cell.getCellType() == CellType.STRING
                            && !cell.getStringCellValue().trim().isEmpty()) {
                        hasLabel = true;
                    } else if (cell.getCellType() == CellType.NUMERIC) {
                        hasLabel = true;
                    }
                }
            }

            if (!hasLabel) {
                frozen.add(offset);
                continue;
            }

            // Check 3: Does any data row have a formula at this column?
            if (hasFormulaInColumn(sheet, colIdx, dataStartRow)) {
                frozen.add(offset);
            }
        }

        return frozen;
    }

    /**
     * Finds all columns in the given row that are "swallowed"
     * by a merged region — i.e. they are NOT the first column
     * of the merge.
     *
     * Example: If cells C3:L3 are merged, columns D through L
     * are spanned. C is the origin (not spanned).
     */
    private static Set<Integer> getMergedSpanColumns(Sheet sheet,
                                                     int targetRow,
                                                     int blockStart,
                                                     int blockEnd) {
        Set<Integer> spanned = new LinkedHashSet<>();

        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);

            // Only merges that touch our header row
            if (targetRow < region.getFirstRow()
                    || targetRow > region.getLastRow()) {
                continue;
            }

            // Only merges that overlap our block
            if (region.getLastColumn() < blockStart
                    || region.getFirstColumn() > blockEnd) {
                continue;
            }

            // Every column in the merge EXCEPT the first is spanned
            for (int col = region.getFirstColumn() + 1;
                 col <= region.getLastColumn(); col++) {
                if (col >= blockStart && col <= blockEnd) {
                    spanned.add(col);
                }
            }
        }

        return spanned;
    }

    /**
     * Checks if any row from dataStartRow downward has a formula
     * in the given column.
     */
    private static boolean hasFormulaInColumn(Sheet sheet, int colIdx,
                                              int dataStartRow) {
        int lastRow = sheet.getLastRowNum();
        for (int rowIdx = dataStartRow; rowIdx <= lastRow; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;

            Cell cell = row.getCell(colIdx);
            if (cell != null && cell.getCellType() == CellType.FORMULA) {
                return true;
            }
        }
        return false;
    }
}