package com.report.engine.processor;

import com.report.engine.dto.CellStyleConfig;
import com.report.engine.dto.CloneBlockConfig;
import com.report.engine.dto.ConditionalStyleConfig;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConditionalStyleRenderer {

    private final XSSFWorkbook workbook;
    private final Map<String, CellStyle> styleCache = new HashMap<>();

    public ConditionalStyleRenderer(XSSFWorkbook workbook) {
        this.workbook = workbook;
    }

    public void apply(Sheet sheet,
                      List<ConditionalStyleConfig> rules,
                      CloneBlockConfig cloneBlock,
                      int scenarioCount,
                      int firstDataRow,
                      int lastDataRow) {

        if (rules == null || rules.isEmpty()) return;

        int blockStart = cloneBlock.getStartColIndex();
        int blockWidth = cloneBlock.getBlockWidth();

        // Sort by priority — lowest first, highest last (highest wins)
        List<ConditionalStyleConfig> sorted = rules.stream()
                .sorted(Comparator.comparingInt(ConditionalStyleConfig::getPriority))
                .collect(Collectors.toList());

        for (ConditionalStyleConfig rule : sorted) {
            for (int scenarioIdx = 0; scenarioIdx < scenarioCount; scenarioIdx++) {
                int thisBlockStart = blockStart + (scenarioIdx * blockWidth);

                for (int offset : rule.getOffsets()) {
                    int colIdx = thisBlockStart + offset;

                    for (int rowIdx = firstDataRow; rowIdx <= lastDataRow; rowIdx++) {
                        Row row = sheet.getRow(rowIdx);
                        if (row == null) continue;

                        Cell cell = row.getCell(colIdx);
                        if (cell == null) continue;

                        double cellValue = getCellNumericValue(cell);
                        if (Double.isNaN(cellValue)) continue;

                        if (matchesCondition(cellValue, rule)) {
                            applyStyle(cell, rule.getStyle());
                        }
                    }
                }
            }
        }
    }

    private double getCellNumericValue(Cell cell) {
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case FORMULA:
                    try {
                        return cell.getNumericCellValue();
                    } catch (Exception e) {
                        try {
                            FormulaEvaluator evaluator = workbook.getCreationHelper()
                                    .createFormulaEvaluator();
                            CellValue cv = evaluator.evaluate(cell);
                            if (cv != null && cv.getCellType() == CellType.NUMERIC) {
                                return cv.getNumberValue();
                            }
                        } catch (Exception ex) {
                            return Double.NaN;
                        }
                    }
                    return Double.NaN;
                default:
                    return Double.NaN;
            }
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private boolean matchesCondition(double cellValue, ConditionalStyleConfig rule) {
        switch (rule.getCondition().toUpperCase()) {
            case "LESS_THAN":
                return cellValue < rule.getValue();
            case "LESS_THAN_OR_EQUAL":
                return cellValue <= rule.getValue();
            case "GREATER_THAN":
                return cellValue > rule.getValue();
            case "GREATER_THAN_OR_EQUAL":
                return cellValue >= rule.getValue();
            case "EQUALS":
                return Math.abs(cellValue - rule.getValue()) < 0.0001;
            case "NOT_EQUALS":
                return Math.abs(cellValue - rule.getValue()) >= 0.0001;
            case "BETWEEN":
                return rule.getValueTo() != null
                        && cellValue >= rule.getValue()
                        && cellValue <= rule.getValueTo();
            default:
                return false;
        }
    }

    private void applyStyle(Cell cell, CellStyleConfig styleConfig) {
        CellStyle existingStyle = cell.getCellStyle();
        String cacheKey = existingStyle.hashCode()
                + "_f:" + styleConfig.getFillColor()
                + "_fc:" + styleConfig.getFontColor()
                + "_b:" + styleConfig.getBold()
                + "_i:" + styleConfig.getItalic();

        CellStyle cachedStyle = styleCache.get(cacheKey);

        if (cachedStyle == null) {
            XSSFCellStyle newStyle = workbook.createCellStyle();
            newStyle.cloneStyleFrom(existingStyle);

            if (styleConfig.getFillColor() != null) {
                XSSFColor fillColor = new XSSFColor(
                        hexToBytes(styleConfig.getFillColor()), null);
                newStyle.setFillForegroundColor(fillColor);
                newStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }

            if (styleConfig.getFontColor() != null
                    || styleConfig.getBold() != null
                    || styleConfig.getItalic() != null) {

                XSSFFont existingFont = workbook.getFontAt(
                        existingStyle.getFontIndex());
                XSSFFont newFont = workbook.createFont();

                newFont.setFontName(existingFont.getFontName());
                newFont.setFontHeightInPoints(existingFont.getFontHeightInPoints());
                newFont.setBold(existingFont.getBold());
                newFont.setItalic(existingFont.getItalic());
                newFont.setUnderline(existingFont.getUnderline());

                if (styleConfig.getFontColor() != null) {
                    newFont.setColor(new XSSFColor(
                            hexToBytes(styleConfig.getFontColor()), null));
                }
                if (styleConfig.getBold() != null) {
                    newFont.setBold(styleConfig.getBold());
                }
                if (styleConfig.getItalic() != null) {
                    newFont.setItalic(styleConfig.getItalic());
                }

                newStyle.setFont(newFont);
            }

            styleCache.put(cacheKey, newStyle);
            cachedStyle = newStyle;
        }

        cell.setCellStyle(cachedStyle);
    }

    private byte[] hexToBytes(String hex) {
        hex = hex.replace("#", "");
        return new byte[]{
                (byte) Integer.parseInt(hex.substring(0, 2), 16),
                (byte) Integer.parseInt(hex.substring(2, 4), 16),
                (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    /**
     * Applies conditional styles for non-cloned sheets.
     * Offsets in the JSON are treated as absolute column indices.
     */
    public void applyDirect(Sheet sheet,
                            List<ConditionalStyleConfig> rules,
                            int firstDataRow,
                            int lastDataRow) {

        if (rules == null || rules.isEmpty()) return;

        List<ConditionalStyleConfig> sorted = rules.stream()
                .sorted(Comparator.comparingInt(ConditionalStyleConfig::getPriority))
                .collect(Collectors.toList());

        for (ConditionalStyleConfig rule : sorted) {
            for (int colIdx : rule.getOffsets()) {
                for (int rowIdx = firstDataRow; rowIdx <= lastDataRow; rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) continue;

                    Cell cell = row.getCell(colIdx);
                    if (cell == null) continue;

                    double cellValue = getCellNumericValue(cell);
                    if (Double.isNaN(cellValue)) continue;

                    if (matchesCondition(cellValue, rule)) {
                        applyStyle(cell, rule.getStyle());
                    }
                }
            }
        }
    }
}
