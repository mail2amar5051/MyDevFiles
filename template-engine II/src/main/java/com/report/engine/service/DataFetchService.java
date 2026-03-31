package com.report.engine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches runtime data from the database using query keys defined in the JSON config.
 *
 * The query keys map to actual SQL queries stored in a query registry (DB table or config).
 * This keeps the JSON config decoupled from actual SQL.
 */
@Service
public class DataFetchService {

    //@Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Fetches a list of maps — used for scenarios.
     * Each map represents one scenario with its fields.
     *
     * Example return:
     * [
     *   { "scenarioName": "Base Case" },
     *   { "scenarioName": "Optimistic" },
     *   { "scenarioName": "Pessimistic" }
     * ]
     */
    public List<Map<String, Object>> fetchList(String queryKey, Long reportId) {
//        String sql = resolveQuery(queryKey);
//        return jdbcTemplate.queryForList(sql, reportId);
        // create  a sample list of maps to return
        List<Map<String, Object>> scenarios = List.of(
                Map.of("scenarioName", "Base Case"),
                Map.of("scenarioName", "Optimistic"),
                Map.of("scenarioName", "Pessimistic")
        );
        return scenarios;
    }

    /**
     * Fetches a single map of key-value pairs — used for common placeholders.
     *
     * Example return:
     * {
     *   "{{reportTitle}}": "Projections Schedule -CitiGroup",
     *   "{{currentcycle}}": "FY2026",
     *   "{{priorcycle}}": "FY2025"
     * }
     */
    public Map<String, String> fetchPlaceholderMap(String queryKey, Long reportId) {
 /*       String sql = resolveQuery(queryKey);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, reportId);

        Map<String, String> placeholders = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String key = (String) row.get("placeholder_key");
            String value = String.valueOf(row.get("placeholder_value"));
            placeholders.put(key, value);
        }
        return placeholders;*/
        // create a sample map to return
        Map<String, String> placeholders = Map.of(
                "{{reportTitle}}", "Projections Schedule -CG",
                "{{currentcycle}}", "FY2026",
                "{{priorcycle}}", "FY2025"
        );
        return placeholders;
    }

    /**
     * Resolves a queryKey to an actual SQL string.
     *
     * Option 1: Store queries in a DB table (query_registry)
     * Option 2: Store in application.yml
     * Option 3: Store in a properties file
     *
     * This example uses a DB table approach.
     */
    private String resolveQuery(String queryKey) {
        String sql = "SELECT query_sql FROM query_registry WHERE query_key = ?";
        return jdbcTemplate.queryForObject(sql, String.class, queryKey);
    }
}
