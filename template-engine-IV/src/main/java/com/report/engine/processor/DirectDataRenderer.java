package com.report.engine.processor;

import com.report.engine.dto.CloneBlockConfig;
import com.report.engine.dto.DirectColumnMapping;
import com.report.engine.dto.DirectMappingConfig;
import org.apache.poi.ss.usermodel.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders data directly into absolute column positions.
 * Used for sheets WITHOUT column cloning (e.g. Q vs A).
 *
 * Unlike DataRowRenderer which uses block-relative offsets,
 * this renderer maps column letters directly to DB columns.
 */
public class DirectDataRenderer {

    public void render(Sheet sheet,
                       DirectMappingConfig config,
                       List<Map<String, Object>> dataRows) {

        int startRow = config.getStartRow() - 1; // 0-based
        String rowKey = config.getRowKey();

        // Build row index from data order
        Map<String, Integer> rowIndex = new LinkedHashMap<>();
        int rowCounter = 0;
        for (Map<String, Object> row : dataRows) {
            String lineId = String.valueOf(row.get(rowKey));
            if (!rowIndex.containsKey(lineId)) {
                rowIndex.put(lineId, rowCounter++);
            }
        }

        for (Map<String, Object> data : dataRows) {
            String lineId = String.valueOf(data.get(rowKey));
            Integer rowOffset = rowIndex.get(lineId);
            if (rowOffset == null) continue;

            int sheetRow = startRow + rowOffset;
            Row row = getOrCreateRow(sheet, sheetRow);

            for (DirectColumnMapping mapping : config.getColumns()) {
                int colIdx = CloneBlockConfig.columnLetterToIndex(mapping.getColumn());
                Object value = data.get(mapping.getDbColumn());

                // Skip if cell already has a formula
                Cell existing = row.getCell(colIdx);
                if (existing != null && existing.getCellType() == CellType.FORMULA) {
                    continue;
                }

                setCellValue(row, colIdx, value);
            }
        }
    }

    public int getDataRowCount(DirectMappingConfig config,
                               List<Map<String, Object>> dataRows) {
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (Map<String, Object> row : dataRows) {
            seen.put(String.valueOf(row.get(config.getRowKey())), true);
        }
        return seen.size();
    }

    private void setCellValue(Row row, int colIdx, Object value) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            String str = String.valueOf(value);
            try {
                cell.setCellValue(Double.parseDouble(str));
            } catch (NumberFormatException e) {
                cell.setCellValue(str);
            }
        }
    }

    private Row getOrCreateRow(Sheet sheet, int rowIdx) {
        Row row = sheet.getRow(rowIdx);
        return row != null ? row : sheet.createRow(rowIdx);
    }
}
