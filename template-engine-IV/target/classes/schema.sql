-- ============================================================
-- REPORT TEMPLATE ENGINE - Oracle Database Schema
-- ============================================================

-- 1. Stores the abstract Excel template as a BLOB
CREATE TABLE report_template (
    template_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR2(255) NOT NULL,
    template_blob BLOB NOT NULL,
    version       NUMBER DEFAULT 1,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Stores per-report generation config (the rules JSON)
CREATE TABLE report_config (
    report_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_id   NUMBER NOT NULL,
    sheet_rules   CLOB NOT NULL,  -- The JSON config
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_template FOREIGN KEY (template_id)
        REFERENCES report_template (template_id)
);

-- 3. Query registry: maps queryKey → actual SQL
--    This decouples the JSON config from real SQL queries.
CREATE TABLE query_registry (
    query_key     VARCHAR2(100) PRIMARY KEY,
    query_sql     CLOB NOT NULL,
    description   VARCHAR2(500),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- SAMPLE DATA
-- ============================================================

-- Register the query that fetches scenario list
INSERT INTO query_registry (query_key, query_sql, description) VALUES (
    'GET_SCENARIOS',
    'SELECT scenario_name AS "scenarioName" FROM report_scenarios WHERE report_id = ? ORDER BY sort_order',
    'Returns list of scenarios for a given report'
);

-- Register the query that fetches common placeholders
INSERT INTO query_registry (query_key, query_sql, description) VALUES (
    'GET_PLACEHOLDERS',
    'SELECT placeholder_key, placeholder_value FROM report_placeholders WHERE report_id = ?',
    'Returns placeholder key-value pairs for a given report'
);

-- ============================================================
-- SUPPORTING TABLES (runtime data)
-- ============================================================

-- Scenarios per report
CREATE TABLE report_scenarios (
    id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_id       NUMBER NOT NULL,
    scenario_name   VARCHAR2(255) NOT NULL,
    sort_order      NUMBER DEFAULT 0
);

-- Placeholder values per report
CREATE TABLE report_placeholders (
    id                NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_id         NUMBER NOT NULL,
    placeholder_key   VARCHAR2(255) NOT NULL,   -- e.g. {{currentcycle}}
    placeholder_value VARCHAR2(4000)             -- e.g. FY2026
);

-- ============================================================
-- SAMPLE RUNTIME DATA
-- ============================================================

-- 3 scenarios for report_id = 5001
INSERT INTO report_scenarios (report_id, scenario_name, sort_order) VALUES (5001, 'Base Case', 1);
INSERT INTO report_scenarios (report_id, scenario_name, sort_order) VALUES (5001, 'Optimistic', 2);
INSERT INTO report_scenarios (report_id, scenario_name, sort_order) VALUES (5001, 'Pessimistic', 3);

-- Common placeholders for report_id = 5001
INSERT INTO report_placeholders (report_id, placeholder_key, placeholder_value)
VALUES (5001, '{{reportTitle}}', 'Projections Schedule -CitiGroup');

INSERT INTO report_placeholders (report_id, placeholder_key, placeholder_value)
VALUES (5001, '{{currentcycle}}', 'FY2026');

INSERT INTO report_placeholders (report_id, placeholder_key, placeholder_value)
VALUES (5001, '{{priorcycle}}', 'FY2025');

INSERT INTO report_placeholders (report_id, placeholder_key, placeholder_value)
VALUES (5001, '{{currentcycle vs priorcycle}}', 'FY2026 vs FY2025');

INSERT INTO report_placeholders (report_id, placeholder_key, placeholder_value)
VALUES (5001, '{{currentcycle vs priorcycle%}}', 'FY2026 vs FY2025 %');

COMMIT;
