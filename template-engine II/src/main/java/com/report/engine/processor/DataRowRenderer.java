package com.report.engine.processor;

import com.report.engine.dto.BlockMapping;
import com.report.engine.dto.CloneBlockConfig;
import com.report.engine.dto.DataRowConfig;
import org.apache.poi.ss.usermodel.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataRowRenderer {

    private final Set<Integer> frozenOffsets;

    /**
     * @param frozenOffsets offsets auto-detected from template that contain formulas.
     *                      DataRowRenderer will never write into these.
     */
    public DataRowRenderer(Set<Integer> frozenOffsets) {
        this.frozenOffsets = frozenOffsets;
    }

    public void render(Sheet sheet,
                       DataRowConfig dataRowConfig,
                       CloneBlockConfig cloneBlock,
                       List<String> scenarios,
                       List<Map<String, Object>> dataRows) {

        int startRow = dataRowConfig.getStartRow() - 1;
        int blockWidth = cloneBlock.getBlockWidth();
        int blockStart = cloneBlock.getStartColIndex();
        String scenarioKey = dataRowConfig.getScenarioKey();
        String rowKey = dataRowConfig.getRowKey();

        Map<String, Integer> scenarioIndex = new LinkedHashMap<>();
        for (int i = 0; i < scenarios.size(); i++) {
            scenarioIndex.put(scenarios.get(i), i);
        }

        Map<String, Integer> rowIndex = new LinkedHashMap<>();
        int rowCounter = 0;
        for (Map<String, Object> row : dataRows) {
            String lineId = String.valueOf(row.get(rowKey));
            if (!rowIndex.containsKey(lineId)) {
                rowIndex.put(lineId, rowCounter++);
            }
        }

        for (Map<String, Object> data : dataRows) {
            String scenarioName = String.valueOf(data.get(scenarioKey));
            String lineId = String.valueOf(data.get(rowKey));

            Integer cloneIdx = scenarioIndex.get(scenarioName);
            Integer rowOffset = rowIndex.get(lineId);
            if (cloneIdx == null || rowOffset == null) continue;

            int thisBlockStart = blockStart + (cloneIdx * blockWidth);
            int sheetRow = startRow + rowOffset;
            Row row = getOrCreateRow(sheet, sheetRow);

            for (BlockMapping mapping : dataRowConfig.getBlockMappings()) {

                // AUTO-SKIP: if this offset is a formula column, don't touch it
                if (frozenOffsets.contains(mapping.getOffset())) {
                    continue;
                }

                int colIdx = thisBlockStart + mapping.getOffset();
                Object value = data.get(mapping.getDbColumn());
                setCellValue(row, colIdx, value);
            }
        }
    }

    public int getDataRowCount(DataRowConfig config,
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