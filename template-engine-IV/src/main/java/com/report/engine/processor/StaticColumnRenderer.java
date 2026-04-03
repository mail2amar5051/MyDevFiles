package com.report.engine.processor;


import com.report.engine.dto.CloneBlockConfig;
import com.report.engine.dto.StaticColumnConfig;
import com.report.engine.dto.StaticMapping;
import org.apache.poi.ss.usermodel.*;

import java.util.List;
import java.util.Map;

public class StaticColumnRenderer {

    public void render(Sheet sheet, StaticColumnConfig config,
                       List<Map<String, Object>> lineItems) {

        int startRow = config.getStartRow() - 1;

        for (int rowIdx = 0; rowIdx < lineItems.size(); rowIdx++) {
            Map<String, Object> data = lineItems.get(rowIdx);
            int sheetRow = startRow + rowIdx;
            Row row = getOrCreateRow(sheet, sheetRow);

            for (StaticMapping mapping : config.getMappings()) {
                int colIdx = CloneBlockConfig
                        .columnLetterToIndex(mapping.getColumn());
                Object value = data.get(mapping.getDbColumn());
                setCellValue(row, colIdx, value, sheet, sheetRow);
            }
        }
    }

    private void setCellValue(Row row, int colIdx, Object value,
                              Sheet sheet, int sheetRow) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }

        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private Row getOrCreateRow(Sheet sheet, int rowIdx) {
        Row row = sheet.getRow(rowIdx);
        return row != null ? row : sheet.createRow(rowIdx);
    }
}
