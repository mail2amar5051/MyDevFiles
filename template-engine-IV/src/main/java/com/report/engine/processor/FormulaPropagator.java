package com.report.engine.processor;

import com.report.engine.dto.CloneBlockConfig;
import org.apache.poi.ss.usermodel.*;

import java.util.Set;

/**
 * After data rendering, the template's formula row (first data row)
 * has formulas only in row 6. Rows 7+ are empty at frozen offsets.
 *
 * This propagator copies formulas from the first data row DOWN
 * to all other data rows, with row references auto-adjusted by POI.
 */
public class FormulaPropagator {

    /**
     * @param sheet          the sheet (data already rendered)
     * @param frozenOffsets   offsets that contain formulas (auto-detected)
     * @param cloneBlock     block config
     * @param scenarioCount  number of cloned blocks
     * @param firstDataRow   first data row (0-based) — has the template formulas
     * @param lastDataRow    last data row (0-based, inclusive)
     */
    public static void propagate(Sheet sheet,
                                 Set<Integer> frozenOffsets,
                                 CloneBlockConfig cloneBlock,
                                 int scenarioCount,
                                 int firstDataRow,
                                 int lastDataRow) {

        if (frozenOffsets == null || frozenOffsets.isEmpty()) return;
        if (firstDataRow >= lastDataRow) return; // only 1 row, nothing to propagate

        int blockStart = cloneBlock.getStartColIndex();
        int blockWidth = cloneBlock.getBlockWidth();

        for (int scenarioIdx = 0; scenarioIdx < scenarioCount; scenarioIdx++) {
            int thisBlockStart = blockStart + (scenarioIdx * blockWidth);

            for (int offset : frozenOffsets) {
                int colIdx = thisBlockStart + offset;

                // Get formula from first data row
                Row sourceRow = sheet.getRow(firstDataRow);
                if (sourceRow == null) continue;

                Cell sourceCell = sourceRow.getCell(colIdx);
                if (sourceCell == null) continue;
                if (sourceCell.getCellType() != CellType.FORMULA) continue;

                String formula = sourceCell.getCellFormula();

                // Copy formula to all subsequent rows, adjusting row references
                for (int rowIdx = firstDataRow + 1; rowIdx <= lastDataRow; rowIdx++) {
                    Row targetRow = sheet.getRow(rowIdx);
                    if (targetRow == null) {
                        targetRow = sheet.createRow(rowIdx);
                    }

                    Cell targetCell = targetRow.getCell(colIdx);
                    if (targetCell == null) {
                        targetCell = targetRow.createCell(colIdx);
                    }

                    // Shift row references in formula
                    int rowShift = rowIdx - firstDataRow;
                    String shiftedFormula = shiftFormulaRowsPublic(formula, rowShift);
                    targetCell.setCellFormula(shiftedFormula);

                    // Copy style from source
                    if (sourceCell.getCellStyle() != null) {
                        targetCell.setCellStyle(sourceCell.getCellStyle());
                    }
                }
            }
        }
    }

    /**
     * Shifts all row references in a formula by the given amount.
     *
     * "SUM(D6:L6)" with shift 1 → "SUM(D7:L7)"
     * "IF(W6=0,0,(M6-W6)/W6)" with shift 3 → "IF(W9=0,0,(M9-W9)/W9)"
     *
     * Respects absolute rows ($6 stays as $6).
     */
    public static String shiftFormulaRowsPublic(String formula, int shift) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < formula.length()) {
            char c = formula.charAt(i);

            // Check for $ (absolute row marker)
            if (c == '$' && i + 1 < formula.length()
                    && Character.isDigit(formula.charAt(i + 1))) {
                // Absolute row — don't shift, copy as-is
                result.append(c);
                i++;
                while (i < formula.length() && Character.isDigit(formula.charAt(i))) {
                    result.append(formula.charAt(i));
                    i++;
                }
                continue;
            }

            // Check if this is a letter followed by digits (cell reference)
            if (Character.isLetter(c)) {
                // Collect all letters
                int letterStart = i;
                while (i < formula.length() && Character.isLetter(formula.charAt(i))) {
                    i++;
                }
                String letters = formula.substring(letterStart, i);

                // Check if followed by digits (row number)
                if (i < formula.length() && Character.isDigit(formula.charAt(i))) {
                    int digitStart = i;
                    while (i < formula.length() && Character.isDigit(formula.charAt(i))) {
                        i++;
                    }
                    String digits = formula.substring(digitStart, i);

                    // Check if letters look like a column ref (A-XFD)
                    // and not a function name like SUM, IF, etc.
                    if (isColumnReference(letters)) {
                        int rowNum = Integer.parseInt(digits) + shift;
                        result.append(letters).append(rowNum);
                    } else {
                        // It's a function name followed by a number (rare)
                        result.append(letters).append(digits);
                    }
                } else {
                    result.append(letters);
                }
                continue;
            }

            result.append(c);
            i++;
        }

        return result.toString();
    }

    /**
     * Checks if a string looks like an Excel column reference (A-XFD)
     * rather than a function name (SUM, IF, AVERAGE, etc.).
     *
     * Column refs are 1-3 uppercase letters, max "XFD".
     */
    private static boolean isColumnReference(String s) {
        if (s.length() > 3) return false;

        // Common function names to exclude
        String upper = s.toUpperCase();
        switch (upper) {
            case "SUM":
            case "IF":
            case "AND":
            case "OR":
            case "NOT":
            case "MAX":
            case "MIN":
            case "ABS":
            case "MOD":
            case "LOG":
            case "LN":
            case "PI":
            case "ROW":
            case "NOW":
            case "DAY":
            case "NA":
            case "MID":
            case "LEN":
            case "ASC":
                return false;
            default:
                // All chars must be A-Z
                for (char c : upper.toCharArray()) {
                    if (c < 'A' || c > 'Z') return false;
                }
                return true;
        }
    }
}