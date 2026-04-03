package com.report.engine.processor;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles merged region operations during column block cloning.
 *
 * When a block of columns (e.g. C:Y) is cloned to a new position,
 * any merged regions within that block must be duplicated and shifted
 * to the new column positions.
 */
public class MergedRegionHandler {

    /**
     * Finds all merged regions that fall within the given column range.
     *
     * @param sheet       the sheet to scan
     * @param startCol    start column index (inclusive)
     * @param endCol      end column index (inclusive)
     * @return list of merged regions within the range
     */
    public List<CellRangeAddress> getMergedRegionsInRange(Sheet sheet, int startCol, int endCol) {
        List<CellRangeAddress> regions = new ArrayList<>();

        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);

            if (region.getFirstColumn() >= startCol && region.getLastColumn() <= endCol) {
                regions.add(region);
            }
        }
        return regions;
    }

    /**
     * Clones merged regions from the template block to the target position.
     *
     * @param sheet        the sheet
     * @param sourceStart  start column of the template block
     * @param sourceEnd    end column of the template block
     * @param targetStart  start column of the cloned block
     */
    public void cloneMergedRegions(Sheet sheet, int sourceStart, int sourceEnd, int targetStart) {
        int offset = targetStart - sourceStart;

        List<CellRangeAddress> sourceRegions = getMergedRegionsInRange(sheet, sourceStart, sourceEnd);

        for (CellRangeAddress region : sourceRegions) {
            CellRangeAddress clonedRegion = new CellRangeAddress(
                    region.getFirstRow(),
                    region.getLastRow(),
                    region.getFirstColumn() + offset,
                    region.getLastColumn() + offset
            );
            sheet.addMergedRegion(clonedRegion);
        }
    }

    /**
     * Removes all merged regions within the given column range.
     * Useful for cleanup before re-cloning.
     *
     * Iterates in reverse to avoid index shifting issues.
     *
     * @param sheet     the sheet
     * @param startCol  start column index (inclusive)
     * @param endCol    end column index (inclusive)
     */
    public void removeMergedRegionsInRange(Sheet sheet, int startCol, int endCol) {
        List<Integer> toRemove = new ArrayList<>();

        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);

            if (region.getFirstColumn() >= startCol && region.getLastColumn() <= endCol) {
                toRemove.add(i);
            }
        }

        // Remove in reverse order to preserve indices
        for (int i = toRemove.size() - 1; i >= 0; i--) {
            sheet.removeMergedRegion(toRemove.get(i));
        }
    }
}
