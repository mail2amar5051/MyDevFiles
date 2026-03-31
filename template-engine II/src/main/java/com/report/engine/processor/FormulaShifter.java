package com.report.engine.processor;

import com.report.engine.dto.CloneBlockConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shifts column references in Excel formulas by a given offset.
 *
 * When a column block is cloned, formulas like "SUM(C6:C20)" in the original
 * become "SUM(Z6:Z20)" in the clone (shifted by blockWidth columns).
 *
 * Handles:
 *   - Simple references:  C5, AA10
 *   - Range references:   C5:K5
 *   - Mixed references:   $C5 (absolute column stays), C$5 (absolute row stays)
 *   - Cross-references:   References outside the block range are NOT shifted
 */
public class FormulaShifter {

    // Matches cell references like C5, AA10, $C5, C$5, $C$5
    private static final Pattern CELL_REF_PATTERN =
            Pattern.compile("(\\$?)([A-Z]{1,3})(\\$?)(\\d+)");

    /**
     * Shifts all column references in a formula that fall within the source block range.
     *
     * @param formula      the original formula string
     * @param shiftBy      number of columns to shift
     * @param sourceStart  start column index of the template block
     * @param sourceEnd    end column index of the template block
     * @return shifted formula
     */
    public static String shiftFormula(String formula, int shiftBy, int sourceStart, int sourceEnd) {
        if (formula == null || formula.isEmpty()) {
            return formula;
        }

        Matcher matcher = CELL_REF_PATTERN.matcher(formula);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String colAbsoluteMarker = matcher.group(1);  // "$" or ""
            String colLetters = matcher.group(2);          // "C", "AA", etc.
            String rowAbsoluteMarker = matcher.group(3);   // "$" or ""
            String rowNumber = matcher.group(4);            // "5", "10", etc.

            int colIndex = CloneBlockConfig.columnLetterToIndex(colLetters);

            // Only shift if:
            // 1. Column is NOT absolute ($C stays as $C)
            // 2. Column falls within the source block range
            if (colAbsoluteMarker.isEmpty() && colIndex >= sourceStart && colIndex <= sourceEnd) {
                int newColIndex = colIndex + shiftBy;
                String newColLetters = CloneBlockConfig.indexToColumnLetter(newColIndex);
                matcher.appendReplacement(result,
                        Matcher.quoteReplacement(colAbsoluteMarker + newColLetters + rowAbsoluteMarker + rowNumber));
            } else {
                // Leave unchanged
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
