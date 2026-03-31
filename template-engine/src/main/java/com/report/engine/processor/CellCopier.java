package com.report.engine.processor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.HashMap;
import java.util.Map;

/**
 * Copies cell content, style, and type from one cell to another.
 *
 * Uses a style cache to avoid hitting POI's 64,000 cell style limit.
 * Styles are reused across cells rather than creating new ones each time.
 */
public class CellCopier {

    private final Map<Integer, CellStyle> styleCache = new HashMap<>();
    private final Workbook workbook;

    public CellCopier(Workbook workbook) {
        this.workbook = workbook;
    }

    /**
     * Copies everything from source cell to target cell:
     * value, formula, style, type, comment.
     */
    public void copyCell(Cell source, Cell target) {
        if (source == null) {
            return;
        }

        // Copy style (using cache)
        copyStyle(source, target);

        // Copy value based on cell type
        switch (source.getCellType()) {
            case STRING:
                target.setCellValue(source.getStringCellValue());
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(source)) {
                    target.setCellValue(source.getDateCellValue());
                } else {
                    target.setCellValue(source.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                target.setCellValue(source.getBooleanCellValue());
                break;
            case FORMULA:
                target.setCellFormula(source.getCellFormula());
                break;
            case BLANK:
                target.setBlank();
                break;
            case ERROR:
                target.setCellErrorValue(source.getErrorCellValue());
                break;
            default:
                break;
        }

        // Copy comment if present
        if (source.getCellComment() != null) {
            target.setCellComment(source.getCellComment());
        }

        // Copy hyperlink if present
        if (source.getHyperlink() != null) {
            target.setHyperlink(source.getHyperlink());
        }
    }

    /**
     * Copies cell style using a cache to avoid creating duplicate styles.
     */
    private void copyStyle(Cell source, Cell target) {
        int sourceHash = source.getCellStyle().hashCode();

        CellStyle cachedStyle = styleCache.get(sourceHash);
        if (cachedStyle == null) {
            cachedStyle = workbook.createCellStyle();
            cachedStyle.cloneStyleFrom(source.getCellStyle());
            styleCache.put(sourceHash, cachedStyle);
        }

        target.setCellStyle(cachedStyle);
    }

    /**
     * Returns the number of cached styles (for diagnostics).
     */
    public int getCacheSize() {
        return styleCache.size();
    }
}
