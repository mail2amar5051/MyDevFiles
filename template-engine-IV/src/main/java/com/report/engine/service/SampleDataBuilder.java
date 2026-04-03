package com.report.engine.service;

import java.util.*;

/**
 * Constructs sample data for testing without DB.
 * Replace with actual DB calls in production.
 */
public class SampleDataBuilder {

    /**
     * Query 1: GET_SCENARIOS
     * Returns: List of scenarios — each becomes one cloned block
     */
    public static List<Map<String, Object>> buildScenarios() {
        List<Map<String, Object>> scenarios = new ArrayList<>();

        scenarios.add(Map.of("scenarioName", "Base Case"));
        scenarios.add(Map.of("scenarioName", "Optimistic"));
        scenarios.add(Map.of("scenarioName", "Pessimistic"));

        return scenarios;
    }

    /**
     * Query 2: GET_PLACEHOLDERS
     * Returns: Common placeholders applied across all clones
     */
    public static Map<String, String> buildPlaceholders() {
        Map<String, String> placeholders = new LinkedHashMap<>();

        placeholders.put("{{reportTitle}}", "Projections Schedule -CitiGroup");
        placeholders.put("{{currentcycle}}", "FY2026");
        placeholders.put("{{priorcycle}}", "FY2025");
        placeholders.put("{{currentcycle vs priorcycle}}", "FY2026 vs FY2025");
        placeholders.put("{{currentcycle vs priorcycle%}}", "FY2026 vs FY2025 %");

        return placeholders;
    }

    /**
     * Query 3: GET_LINE_ITEMS
     * Returns: Unique line items — written once in columns A, B
     */
    public static List<Map<String, Object>> buildLineItems() {
        List<Map<String, Object>> items = new ArrayList<>();

        items.add(Map.of("line_id", 1, "form_line_name", "Revenue"));
        items.add(Map.of("line_id", 2, "form_line_name", "COGS"));
        items.add(Map.of("line_id", 3, "form_line_name", "Gross Profit"));
        items.add(Map.of("line_id", 4, "form_line_name", "Operating Expenses"));
        items.add(Map.of("line_id", 5, "form_line_name", "EBITDA"));
        items.add(Map.of("line_id", 6, "form_line_name", "Depreciation"));
        items.add(Map.of("line_id", 7, "form_line_name", "Interest Expense"));
        items.add(Map.of("line_id", 8, "form_line_name", "Tax"));
        items.add(Map.of("line_id", 9, "form_line_name", "Net Income"));

        return items;
    }

    /**
     * Query 4: GET_SCENARIO_DATA
     * Returns: One row per line item per scenario
     *          Offsets 0-8 = curr_pq1..pq9, offsets 10-18 = prior_pq1..pq9
     *          Offsets 9, 19, 20, 21 are FORMULA columns — not in this data
     */
    public static List<Map<String, Object>> buildScenarioData() {
        List<Map<String, Object>> data = new ArrayList<>();

        // ── Base Case ──
        data.add(row("Base Case", 1,
                50000, 52000, 53000, 51000, 55000, 54000, 56000, 57000, 58000,
                45000, 46000, 47000, 45500, 48000, 47000, 49000, 50000, 51000));

        data.add(row("Base Case", 2,
                30000, 31000, 32000, 30500, 33000, 32000, 34000, 35000, 35500,
                27000, 28000, 28500, 27500, 29000, 28500, 30000, 30500, 31000));

        data.add(row("Base Case", 3,
                20000, 21000, 21000, 20500, 22000, 22000, 22000, 22000, 22500,
                18000, 18000, 18500, 18000, 19000, 18500, 19000, 19500, 20000));

        data.add(row("Base Case", 4,
                10000, 10500, 10200, 10800, 11000, 10700, 11200, 11500, 11000,
                9500, 9800, 9600, 10000, 10200, 9900, 10500, 10800, 10300));

        data.add(row("Base Case", 5,
                10000, 10500, 10800, 9700, 11000, 11300, 10800, 10500, 11500,
                8500, 8200, 8900, 8000, 8800, 8600, 8500, 8700, 9700));

        data.add(row("Base Case", 6,
                2000, 2000, 2100, 2100, 2200, 2200, 2300, 2300, 2400,
                1800, 1800, 1900, 1900, 2000, 2000, 2100, 2100, 2200));

        data.add(row("Base Case", 7,
                1500, 1500, 1600, 1600, 1700, 1700, 1800, 1800, 1900,
                1400, 1400, 1500, 1500, 1600, 1600, 1700, 1700, 1800));

        data.add(row("Base Case", 8,
                1950, 2100, 2130, 1800, 2130, 2220, 2010, 1920, 2160,
                1590, 1500, 1650, 1380, 1560, 1500, 1410, 1470, 1710));

        data.add(row("Base Case", 9,
                4550, 4900, 4970, 4200, 4970, 5180, 4690, 4480, 5040,
                3710, 3500, 3850, 3220, 3640, 3500, 3290, 3430, 3990));

        // ── Optimistic ──
        data.add(row("Optimistic", 1,
                60000, 63000, 65000, 62000, 67000, 66000, 68000, 70000, 72000,
                45000, 46000, 47000, 45500, 48000, 47000, 49000, 50000, 51000));

        data.add(row("Optimistic", 2,
                33000, 34000, 35000, 33500, 36000, 35000, 37000, 38000, 39000,
                27000, 28000, 28500, 27500, 29000, 28500, 30000, 30500, 31000));

        data.add(row("Optimistic", 3,
                27000, 29000, 30000, 28500, 31000, 31000, 31000, 32000, 33000,
                18000, 18000, 18500, 18000, 19000, 18500, 19000, 19500, 20000));

        data.add(row("Optimistic", 4,
                11000, 11500, 11200, 11800, 12000, 11700, 12200, 12500, 12000,
                9500, 9800, 9600, 10000, 10200, 9900, 10500, 10800, 10300));

        data.add(row("Optimistic", 5,
                16000, 17500, 18800, 16700, 19000, 19300, 18800, 19500, 21000,
                8500, 8200, 8900, 8000, 8800, 8600, 8500, 8700, 9700));

        data.add(row("Optimistic", 6,
                2000, 2000, 2100, 2100, 2200, 2200, 2300, 2300, 2400,
                1800, 1800, 1900, 1900, 2000, 2000, 2100, 2100, 2200));

        data.add(row("Optimistic", 7,
                1500, 1500, 1600, 1600, 1700, 1700, 1800, 1800, 1900,
                1400, 1400, 1500, 1500, 1600, 1600, 1700, 1700, 1800));

        data.add(row("Optimistic", 8,
                3750, 4200, 4530, 3900, 4530, 4620, 4410, 4620, 5010,
                1590, 1500, 1650, 1380, 1560, 1500, 1410, 1470, 1710));

        data.add(row("Optimistic", 9,
                8750, 9800, 10570, 9100, 10570, 10780, 10290, 10780, 11690,
                3710, 3500, 3850, 3220, 3640, 3500, 3290, 3430, 3990));

        // ── Pessimistic ──
        data.add(row("Pessimistic", 1,
                42000, 41000, 40000, 39000, 43000, 42000, 44000, 43000, 44000,
                45000, 46000, 47000, 45500, 48000, 47000, 49000, 50000, 51000));

        data.add(row("Pessimistic", 2,
                28000, 28500, 28000, 27500, 30000, 29000, 30500, 30000, 30500,
                27000, 28000, 28500, 27500, 29000, 28500, 30000, 30500, 31000));

        data.add(row("Pessimistic", 3,
                14000, 12500, 12000, 11500, 13000, 13000, 13500, 13000, 13500,
                18000, 18000, 18500, 18000, 19000, 18500, 19000, 19500, 20000));

        data.add(row("Pessimistic", 4,
                9000, 9500, 9200, 9800, 10000, 9700, 10200, 10500, 10000,
                9500, 9800, 9600, 10000, 10200, 9900, 10500, 10800, 10300));

        data.add(row("Pessimistic", 5,
                5000, 3000, 2800, 1700, 3000, 3300, 3300, 2500, 3500,
                8500, 8200, 8900, 8000, 8800, 8600, 8500, 8700, 9700));

        data.add(row("Pessimistic", 6,
                2000, 2000, 2100, 2100, 2200, 2200, 2300, 2300, 2400,
                1800, 1800, 1900, 1900, 2000, 2000, 2100, 2100, 2200));

        data.add(row("Pessimistic", 7,
                1500, 1500, 1600, 1600, 1700, 1700, 1800, 1800, 1900,
                1400, 1400, 1500, 1500, 1600, 1600, 1700, 1700, 1800));

        data.add(row("Pessimistic", 8,
                450, -150, -270, -600, -270, -180, -240, -480, -240,
                1590, 1500, 1650, 1380, 1560, 1500, 1410, 1470, 1710));

        data.add(row("Pessimistic", 9,
                1050, -350, -630, -1400, -630, -420, -560, -1120, -560,
                3710, 3500, 3850, 3220, 3640, 3500, 3290, 3430, 3990));

        return data;
    }

    /**
     * Helper: builds one row of scenario data.
     */
    private static Map<String, Object> row(String scenario, int lineId,
                                           Number cp1, Number cp2, Number cp3,
                                           Number cp4, Number cp5, Number cp6,
                                           Number cp7, Number cp8, Number cp9,
                                           Number pp1, Number pp2, Number pp3,
                                           Number pp4, Number pp5, Number pp6,
                                           Number pp7, Number pp8, Number pp9) {

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("scenario_name", scenario);
        r.put("line_id", lineId);

        r.put("curr_pq1", cp1); r.put("curr_pq2", cp2); r.put("curr_pq3", cp3);
        r.put("curr_pq4", cp4); r.put("curr_pq5", cp5); r.put("curr_pq6", cp6);
        r.put("curr_pq7", cp7); r.put("curr_pq8", cp8); r.put("curr_pq9", cp9);

        r.put("prior_pq1", pp1); r.put("prior_pq2", pp2); r.put("prior_pq3", pp3);
        r.put("prior_pq4", pp4); r.put("prior_pq5", pp5); r.put("prior_pq6", pp6);
        r.put("prior_pq7", pp7); r.put("prior_pq8", pp8); r.put("prior_pq9", pp9);

        return r;
    }


    // Add these methods to SampleDataBuilder.java

    /**
     * Q vs A sheet: Line items with 14Q baseline value
     * Used for staticColumns A, B, C
     */
    public static List<Map<String, Object>> buildQvsALineItems() {
        List<Map<String, Object>> items = new ArrayList<>();

        items.add(Map.of("line_id", 1, "form_line_name", "Revenue",             "baseline_14q", 480000));
        items.add(Map.of("line_id", 2, "form_line_name", "COGS",                "baseline_14q", 290000));
        items.add(Map.of("line_id", 3, "form_line_name", "Gross Profit",        "baseline_14q", 190000));
        items.add(Map.of("line_id", 4, "form_line_name", "Operating Expenses",  "baseline_14q", 95000));
        items.add(Map.of("line_id", 5, "form_line_name", "EBITDA",              "baseline_14q", 95000));
        items.add(Map.of("line_id", 6, "form_line_name", "Depreciation",        "baseline_14q", 19000));
        items.add(Map.of("line_id", 7, "form_line_name", "Interest Expense",    "baseline_14q", 14500));
        items.add(Map.of("line_id", 8, "form_line_name", "Tax",                 "baseline_14q", 18450));
        items.add(Map.of("line_id", 9, "form_line_name", "Net Income",          "baseline_14q", 43050));

        return items;
    }

    /**
     * Q vs A sheet: BHC BASE data (PQ1-PQ9)
     * One row per line item
     */
    public static List<Map<String, Object>> buildQvsAData() {
        List<Map<String, Object>> data = new ArrayList<>();

        // Each row: line_id + base PQ1-9 + stress PQ1-9
        data.add(qvaRow(1,
                500000, 510000, 520000, 515000, 530000, 525000, 540000, 535000, 550000,
                450000, 440000, 430000, 425000, 420000, 415000, 410000, 405000, 400000));

        data.add(qvaRow(2,
                300000, 305000, 310000, 308000, 315000, 312000, 320000, 318000, 325000,
                280000, 278000, 275000, 273000, 270000, 268000, 265000, 263000, 260000));

        data.add(qvaRow(3,
                200000, 205000, 210000, 207000, 215000, 213000, 220000, 217000, 225000,
                170000, 162000, 155000, 152000, 150000, 147000, 145000, 142000, 140000));

        data.add(qvaRow(4,
                98000, 99000, 100000, 99500, 101000, 100500, 102000, 101500, 103000,
                92000, 91000, 90000, 89500, 89000, 88500, 88000, 87500, 87000));

        data.add(qvaRow(5,
                102000, 106000, 110000, 107500, 114000, 112500, 118000, 115500, 122000,
                78000, 71000, 65000, 62500, 61000, 58500, 57000, 54500, 53000));

        data.add(qvaRow(6,
                20000, 20500, 21000, 21000, 21500, 21500, 22000, 22000, 22500,
                19000, 19000, 18500, 18500, 18000, 18000, 17500, 17500, 17000));

        data.add(qvaRow(7,
                15000, 15200, 15500, 15300, 15800, 15600, 16000, 15800, 16200,
                14000, 13800, 13500, 13300, 13000, 12800, 12500, 12300, 12000));

        data.add(qvaRow(8,
                20100, 21090, 22050, 21360, 23040, 22800, 24000, 23310, 25020,
                13500, 11460, 9900, 9270, 9000, 8340, 8100, 7410, 7200));

        data.add(qvaRow(9,
                46900, 49210, 51450, 49840, 53760, 52200, 56000, 54390, 58380,
                31500, 26740, 23100, 21630, 21000, 19460, 18900, 17290, 16800));

        return data;
    }

    private static Map<String, Object> qvaRow(int lineId,
                                              Number bp1, Number bp2, Number bp3,
                                              Number bp4, Number bp5, Number bp6,
                                              Number bp7, Number bp8, Number bp9,
                                              Number sp1, Number sp2, Number sp3,
                                              Number sp4, Number sp5, Number sp6,
                                              Number sp7, Number sp8, Number sp9) {

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("line_id", lineId);

        // BHC BASE PQ1-9
        r.put("base_pq1", bp1); r.put("base_pq2", bp2); r.put("base_pq3", bp3);
        r.put("base_pq4", bp4); r.put("base_pq5", bp5); r.put("base_pq6", bp6);
        r.put("base_pq7", bp7); r.put("base_pq8", bp8); r.put("base_pq9", bp9);

        // BHC STRESS PQ1-9
        r.put("stress_pq1", sp1); r.put("stress_pq2", sp2); r.put("stress_pq3", sp3);
        r.put("stress_pq4", sp4); r.put("stress_pq5", sp5); r.put("stress_pq6", sp6);
        r.put("stress_pq7", sp7); r.put("stress_pq8", sp8); r.put("stress_pq9", sp9);

        return r;
    }
}
