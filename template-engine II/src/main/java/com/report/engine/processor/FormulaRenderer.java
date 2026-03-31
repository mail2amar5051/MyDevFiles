package com.report.engine.processor;

import com.report.engine.dto.CloneBlockConfig;
import com.report.engine.dto.FormulaColumnConfig;
import org.apache.poi.ss.usermodel.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormulaRenderer {

    private static final Pattern BLOCK_REF = Pattern.compile("\\{block\\+(\\d+)\\}");
    private static final Pattern ROW_REF = Pattern.compile("\\{row\\}");

    public void render(Sheet sheet,
                       List<FormulaColumnConfig> formulaConfigs,
                       CloneBlockConfig cloneBlock,
                       int scenarioCount,
                       int startRow,
                       int endRow) {

        if (formulaConfigs == null || formulaConfigs.isEmpty()) {
            return;
        }

        int blockStart = cloneBlock.getStartColIndex();
        int blockWidth = cloneBlock.getBlockWidth();

        for (int scenarioIdx = 0; scenarioIdx < scenarioCount; scenarioIdx++) {
            int thisBlockStart = blockStart + (scenarioIdx * blockWidth);

            for (FormulaColumnConfig fc : formulaConfigs) {
                int colIdx = thisBlockStart + fc.getOffset();
                String source = fc.getSource() != null
                        ? fc.getSource() : "TEMPLATE_OR_JSON";

                for (int rowIdx = startRow; rowIdx <= endRow; rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) continue;

                    Cell cell = row.getCell(colIdx);
                    int excelRow = rowIdx + 1;

                    switch (source) {
                        case "TEMPLATE_ONLY":
                            break;

                        case "JSON_ONLY":
                            String jsonFormula = resolveFormula(
                                    fc.getFormula(), thisBlockStart, excelRow);
                            if (cell == null) {
                                cell = row.createCell(colIdx);
                                inheritStyle(sheet, cell, rowIdx, colIdx);
                            }
                            cell.setCellFormula(jsonFormula);
                            break;

                        case "TEMPLATE_OR_JSON":
                        default:
                            if (cell != null
                                    && cell.getCellType() == CellType.FORMULA) {
                                // template formula exists — keep it
                            } else {
                                String fallback = resolveFormula(
                                        fc.getFormula(), thisBlockStart, excelRow);
                                if (cell == null) {
                                    cell = row.createCell(colIdx);
                                    inheritStyle(sheet, cell, rowIdx, colIdx);
                                }
                                cell.setCellFormula(fallback);
                            }
                            break;
                    }
                }
            }
        }
    }

    private String resolveFormula(String pattern, int blockStart, int excelRow) {
        Matcher blockMatcher = BLOCK_REF.matcher(pattern);
        StringBuilder sb = new StringBuilder();
        while (blockMatcher.find()) {
            int offset = Integer.parseInt(blockMatcher.group(1));
            int colIndex = blockStart + offset;
            String colLetter = CloneBlockConfig.indexToColumnLetter(colIndex);
            blockMatcher.appendReplacement(sb, Matcher.quoteReplacement(colLetter));
        }
        blockMatcher.appendTail(sb);

        return ROW_REF.matcher(sb.toString())
                .replaceAll(String.valueOf(excelRow));
    }

    private void inheritStyle(Sheet sheet, Cell cell, int rowIdx, int colIdx) {
//        if (rowIdx > 0) {
//            Row above = sheet.getRow(rowIdx - 1);
//            if (above != null) {
//                Cell aboveCell = above.getCell(colIdx);
//                if (aboveCell != null) {
//                    cell.setCellStyle(aboveCell.getCellStyle());
//                }
//            }
//        }
    }
}