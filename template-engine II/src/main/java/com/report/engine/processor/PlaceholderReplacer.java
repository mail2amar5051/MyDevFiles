package com.report.engine.processor;

import org.apache.poi.ss.usermodel.*;

import java.util.Map;

/**
 * Replaces placeholder text in cells with actual values.
 *
 * Scans cells for placeholder patterns like {{scenarioname}}, {{currentcycle}}, etc.
 * Supports replacement in:
 *   - String cells
 *   - Formula cells (replaces placeholder text within the formula string)
 *
 * Can operate on:
 *   - An entire sheet (for global/common placeholders)
 *   - A specific column range (for per-clone placeholders)
 */
public class PlaceholderReplacer {

    /**
     * Replaces all placeholders across the entire sheet.
     * Used for common/global placeholders.
     *
     * @param sheet        the sheet to process
     * @param placeholders map of placeholder → replacement value
     */
    public void replaceInSheet(Sheet sheet, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return;
        }

        for (Row row : sheet) {
            for (Cell cell : row) {
                replaceInCell(cell, placeholders);
            }
        }
    }

    /**
     * Replaces placeholders only within a specific column range.
     * Used for per-clone scenario-specific placeholders.
     *
     * @param sheet        the sheet
     * @param startCol     start column index (inclusive)
     * @param endCol       end column index (inclusive)
     * @param placeholders map of placeholder → replacement value
     */
    public void replaceInColumnRange(Sheet sheet, int startCol, int endCol,
                                     Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return;
        }

        for (Row row : sheet) {
            for (int colIdx = startCol; colIdx <= endCol; colIdx++) {
                Cell cell = row.getCell(colIdx);
                if (cell != null) {
                    replaceInCell(cell, placeholders);
                }
            }
        }
    }

    /**
     * Replaces placeholder text in a single cell.
     * Handles both STRING and FORMULA cell types.
     */
    private void replaceInCell(Cell cell, Map<String, String> placeholders) {
        if (cell == null) {
            return;
        }

        switch (cell.getCellType()) {
            case STRING:
                String value = cell.getStringCellValue();
                if (value != null && !value.isEmpty()) {
                    String replaced = applyReplacements(value, placeholders);
                    if (!replaced.equals(value)) {
                        // Try to set as numeric if the replacement is a number
                        if (isNumeric(replaced)) {
                            cell.setCellValue(Double.parseDouble(replaced));
                        } else {
                            cell.setCellValue(replaced);
                        }
                    }
                }
                break;

            case FORMULA:
                String formula = cell.getCellFormula();
                if (formula != null && !formula.isEmpty()) {
                    String replacedFormula = applyReplacements(formula, placeholders);
                    if (!replacedFormula.equals(formula)) {
                        cell.setCellFormula(replacedFormula);
                    }
                }
                break;

            default:
                // NUMERIC, BOOLEAN, BLANK, ERROR — no placeholders possible
                break;
        }
    }

    /**
     * Applies all placeholder replacements to a string.
     */
    private String applyReplacements(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Checks if a string represents a numeric value.
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
