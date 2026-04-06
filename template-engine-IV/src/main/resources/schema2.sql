-- ============================================================
-- DYNAMIC TEMPLATE ENGINE - Oracle Database Schema
-- ============================================================

-- ============================================================
-- 1. REPORT_TEMPLATE
--    Stores the abstract .xlsx template as a BLOB.
--    One row per template. Multiple sheets live inside the BLOB.
-- ============================================================
CREATE TABLE report_template (
    template_id     NUMBER GENERATED ALWAYS AS IDENTITY,
    template_name   VARCHAR2(255)  NOT NULL,
    template_blob   BLOB           NOT NULL,
    version         NUMBER         DEFAULT 1,
    description     VARCHAR2(1000),
    active          NUMBER(1)      DEFAULT 1,
    created_by      VARCHAR2(255),
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP,
    updated_by      VARCHAR2(255),
    updated_at      TIMESTAMP,
    CONSTRAINT pk_report_template PRIMARY KEY (template_id),
    CONSTRAINT uk_template_name   UNIQUE (template_name, version)
);

COMMENT ON TABLE  report_template IS 'Stores abstract Excel templates with placeholders as BLOB';
COMMENT ON COLUMN report_template.template_name IS 'Human-readable name, used as API lookup key';
COMMENT ON COLUMN report_template.template_blob IS 'The .xlsx file containing placeholder sheets';
COMMENT ON COLUMN report_template.version IS 'Template version for audit trail';
COMMENT ON COLUMN report_template.active IS '1=active, 0=soft deleted';


-- ============================================================
-- 2. TEMPLATE_SHEET_CONFIG
--    One row per sheet in the template.
--    Each row stores:
--      - sheet name (must match the tab in .xlsx)
--      - sheet type (CLONED or DIRECT)
--      - active flag (only active sheets are rendered)
--      - sheet_rules (JSON CLOB with all rules for this sheet)
-- ============================================================
CREATE TABLE template_sheet_config (
    sheet_config_id   NUMBER GENERATED ALWAYS AS IDENTITY,
    template_id       NUMBER         NOT NULL,
    sheet_name        VARCHAR2(255)  NOT NULL,
    output_sheet_name VARCHAR2(255),
    sheet_type        VARCHAR2(20)   NOT NULL,
    sheet_rules       CLOB,
    sort_order        NUMBER         DEFAULT 0,
    active            NUMBER(1)      DEFAULT 1,
    description       VARCHAR2(1000),
    created_by        VARCHAR2(255),
    created_at        TIMESTAMP      DEFAULT SYSTIMESTAMP,
    updated_by        VARCHAR2(255),
    updated_at        TIMESTAMP,
    CONSTRAINT pk_template_sheet_config PRIMARY KEY (sheet_config_id),
    CONSTRAINT fk_tsc_template FOREIGN KEY (template_id)
        REFERENCES report_template (template_id),
    CONSTRAINT uk_tsc_sheet UNIQUE (template_id, sheet_name),
    CONSTRAINT ck_tsc_type CHECK (sheet_type IN ('CLONED', 'DIRECT'))
);

CREATE INDEX idx_tsc_template ON template_sheet_config (template_id);

COMMENT ON TABLE  template_sheet_config IS 'Per-sheet configuration for each template';
COMMENT ON COLUMN template_sheet_config.sheet_name IS 'Must match the tab name in the .xlsx template';
COMMENT ON COLUMN template_sheet_config.output_sheet_name IS 'Renamed tab name in the generated report';
COMMENT ON COLUMN template_sheet_config.sheet_type IS 'CLONED = column block cloning, DIRECT = absolute column mapping';
COMMENT ON COLUMN template_sheet_config.sheet_rules IS 'JSON CLOB: staticColumns, cloneBlock/directMappings, dataRows, conditionalStyles, preserve';
COMMENT ON COLUMN template_sheet_config.sort_order IS 'Processing order — sheets rendered in this order';
COMMENT ON COLUMN template_sheet_config.active IS '1=render this sheet, 0=skip and remove from output';


-- ============================================================
-- 3. SHEET_QUERY
--    Stores SQL queries that fetch runtime data.
--    Each query has a PURPOSE:
--      PLACEHOLDERS  → returns key-value map (common across sheets)
--      CLONE_DRIVER  → returns N rows = N clones (CLONED sheets only)
--      DATA          → returns all data: line items + values
--
--    sheet_config_id = NULL means the query is COMMON (all sheets)
--    Queries use named parameters like :cycleId, :entityId
--    Parameters are passed via the API at runtime
-- ============================================================
CREATE TABLE sheet_query (
    query_id          NUMBER GENERATED ALWAYS AS IDENTITY,
    template_id       NUMBER         NOT NULL,
    sheet_config_id   NUMBER,
    purpose           VARCHAR2(50)   NOT NULL,
    query_sql         CLOB           NOT NULL,
    query_type        VARCHAR2(20)   NOT NULL,
    description       VARCHAR2(1000),
    active            NUMBER(1)      DEFAULT 1,
    created_by        VARCHAR2(255),
    created_at        TIMESTAMP      DEFAULT SYSTIMESTAMP,
    updated_by        VARCHAR2(255),
    updated_at        TIMESTAMP,
    CONSTRAINT pk_sheet_query PRIMARY KEY (query_id),
    CONSTRAINT fk_sq_template FOREIGN KEY (template_id)
        REFERENCES report_template (template_id),
    CONSTRAINT fk_sq_sheet_config FOREIGN KEY (sheet_config_id)
        REFERENCES template_sheet_config (sheet_config_id),
    CONSTRAINT uk_sq_purpose UNIQUE (template_id, sheet_config_id, purpose),
    CONSTRAINT ck_sq_purpose CHECK (purpose IN ('PLACEHOLDERS', 'CLONE_DRIVER', 'DATA')),
    CONSTRAINT ck_sq_type CHECK (query_type IN ('LIST', 'MAP', 'SCALAR'))
);

CREATE INDEX idx_sq_template ON sheet_query (template_id);
CREATE INDEX idx_sq_sheet_config ON sheet_query (sheet_config_id);

COMMENT ON TABLE  sheet_query IS 'SQL queries that fetch runtime data for report generation';
COMMENT ON COLUMN sheet_query.sheet_config_id IS 'NULL = common query (applies to all sheets), otherwise sheet-specific';
COMMENT ON COLUMN sheet_query.purpose IS 'PLACEHOLDERS = key-value pairs, CLONE_DRIVER = clone count + names, DATA = all row data';
COMMENT ON COLUMN sheet_query.query_sql IS 'Oracle SQL with named params like :cycleId, :entityId';
COMMENT ON COLUMN sheet_query.query_type IS 'LIST = List<Map>, MAP = Map<String,String>, SCALAR = single value';


-- ============================================================
-- 4. REPORT_GENERATION_LOG
--    Tracks async report generation.
--    Created as PENDING when API is called.
--    Updated to PROCESSING → COMPLETED or FAILED.
--    output_blob holds the generated .xlsx for download.
-- ============================================================
CREATE TABLE report_generation_log (
    report_id         NUMBER GENERATED ALWAYS AS IDENTITY,
    template_id       NUMBER         NOT NULL,
    template_name     VARCHAR2(255)  NOT NULL,
    request_params    CLOB,
    status            VARCHAR2(20)   DEFAULT 'PENDING',
    total_sheets      NUMBER,
    processed_sheets  NUMBER         DEFAULT 0,
    output_blob       BLOB,
    output_file_name  VARCHAR2(255),
    error_message     VARCHAR2(4000),
    requested_by      VARCHAR2(255),
    started_at        TIMESTAMP,
    completed_at      TIMESTAMP,
    created_at        TIMESTAMP      DEFAULT SYSTIMESTAMP,
    CONSTRAINT pk_report_generation_log PRIMARY KEY (report_id),
    CONSTRAINT fk_rgl_template FOREIGN KEY (template_id)
        REFERENCES report_template (template_id),
    CONSTRAINT ck_rgl_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_rgl_template ON report_generation_log (template_id);
CREATE INDEX idx_rgl_status ON report_generation_log (status);
CREATE INDEX idx_rgl_created ON report_generation_log (created_at DESC);

COMMENT ON TABLE  report_generation_log IS 'Tracks async report generation requests and stores output';
COMMENT ON COLUMN report_generation_log.request_params IS 'JSON: runtime params passed via API (cycleId, entityId, etc.)';
COMMENT ON COLUMN report_generation_log.status IS 'PENDING → PROCESSING → COMPLETED or FAILED';
COMMENT ON COLUMN report_generation_log.total_sheets IS 'Number of active sheets to process';
COMMENT ON COLUMN report_generation_log.processed_sheets IS 'Incremented as each sheet completes';
COMMENT ON COLUMN report_generation_log.output_blob IS 'Generated .xlsx file stored for download';
COMMENT ON COLUMN report_generation_log.output_file_name IS 'Suggested download filename';


-- ============================================================
-- SAMPLE DATA
-- ============================================================

-- Template
INSERT INTO report_template (template_name, template_blob, version, description, active, created_by)
VALUES ('Projections Schedule -CitiGroup', EMPTY_BLOB(), 1, 'CitiGroup projections with across-cycles and Q vs A sheets', 1, 'system');
-- Note: template_blob must be updated separately with the actual .xlsx file via JDBC


-- Sheet configs
INSERT INTO template_sheet_config (template_id, sheet_name, output_sheet_name, sheet_type, sheet_rules, sort_order, active, created_by)
VALUES (1, 'Across Cycles_Citigroup', 'Across_cycle_scenarios', 'CLONED',
'{
  "staticColumns": {
    "columns": ["A", "B"],
    "startRow": 6,
    "mappings": [
      { "column": "A", "dbColumn": "line_id" },
      { "column": "B", "dbColumn": "form_line_name" }
    ]
  },
  "cloneBlock": {
    "columns": "C:Y",
    "cloneNamePlaceholder": "{{scenarioname}}",
    "cloneNameField": "scenarioName"
  },
  "dataRows": {
    "headerRow": 5,
    "startRow": 6,
    "rowKey": "line_id",
    "scenarioKey": "scenario_name",
    "blockMappings": [
      { "offset": 1,  "dbColumn": "curr_pq1" },
      { "offset": 2,  "dbColumn": "curr_pq2" },
      { "offset": 3,  "dbColumn": "curr_pq3" },
      { "offset": 4,  "dbColumn": "curr_pq4" },
      { "offset": 5,  "dbColumn": "curr_pq5" },
      { "offset": 6,  "dbColumn": "curr_pq6" },
      { "offset": 7,  "dbColumn": "curr_pq7" },
      { "offset": 8,  "dbColumn": "curr_pq8" },
      { "offset": 9,  "dbColumn": "curr_pq9" },
      { "offset": 11, "dbColumn": "prior_pq1" },
      { "offset": 12, "dbColumn": "prior_pq2" },
      { "offset": 13, "dbColumn": "prior_pq3" },
      { "offset": 14, "dbColumn": "prior_pq4" },
      { "offset": 15, "dbColumn": "prior_pq5" },
      { "offset": 16, "dbColumn": "prior_pq6" },
      { "offset": 17, "dbColumn": "prior_pq7" },
      { "offset": 18, "dbColumn": "prior_pq8" },
      { "offset": 19, "dbColumn": "prior_pq9" }
    ]
  },
  "conditionalStyles": [
    {
      "priority": 1,
      "offsets": [21, 22],
      "condition": "LESS_THAN",
      "value": 0,
      "style": { "fillColor": "FF0000", "fontColor": "000000" }
    }
  ],
  "preserve": ["mergedRegions", "formulas", "styles", "columnWidths"]
}',
1, 1, 'system');


INSERT INTO template_sheet_config (template_id, sheet_name, output_sheet_name, sheet_type, sheet_rules, sort_order, active, created_by)
VALUES (1, 'Q vs A_Citigroup', 'Q vs A_Citigroup', 'DIRECT',
'{
  "staticColumns": {
    "columns": ["A", "B", "C"],
    "startRow": 5,
    "mappings": [
      { "column": "A", "dbColumn": "line_id" },
      { "column": "B", "dbColumn": "form_line_name" },
      { "column": "C", "dbColumn": "baseline_14q" }
    ]
  },
  "directMappings": {
    "headerRow": 4,
    "startRow": 5,
    "rowKey": "line_id",
    "columns": [
      { "column": "E",  "dbColumn": "base_pq1" },
      { "column": "F",  "dbColumn": "base_pq2" },
      { "column": "G",  "dbColumn": "base_pq3" },
      { "column": "H",  "dbColumn": "base_pq4" },
      { "column": "I",  "dbColumn": "base_pq5" },
      { "column": "J",  "dbColumn": "base_pq6" },
      { "column": "K",  "dbColumn": "base_pq7" },
      { "column": "L",  "dbColumn": "base_pq8" },
      { "column": "M",  "dbColumn": "base_pq9" },
      { "column": "O",  "dbColumn": "stress_pq1" },
      { "column": "P",  "dbColumn": "stress_pq2" },
      { "column": "Q",  "dbColumn": "stress_pq3" },
      { "column": "R",  "dbColumn": "stress_pq4" },
      { "column": "S",  "dbColumn": "stress_pq5" },
      { "column": "T",  "dbColumn": "stress_pq6" },
      { "column": "U",  "dbColumn": "stress_pq7" },
      { "column": "V",  "dbColumn": "stress_pq8" },
      { "column": "W",  "dbColumn": "stress_pq9" }
    ]
  },
  "conditionalStyles": [
    {
      "priority": 1,
      "offsets": [24, 25, 27, 28],
      "condition": "LESS_THAN",
      "value": 0,
      "style": { "fillColor": "FF0000", "fontColor": "000000" }
    }
  ],
  "preserve": ["mergedRegions", "formulas", "styles", "columnWidths"]
}',
2, 1, 'system');


-- Queries: COMMON (sheet_config_id IS NULL)
INSERT INTO sheet_query (template_id, sheet_config_id, purpose, query_sql, query_type, description, active, created_by)
VALUES (1, NULL, 'PLACEHOLDERS',
'SELECT param_key AS placeholder_key, param_value AS placeholder_value
FROM report_parameters
WHERE cycle_id = :cycleId AND entity_id = :entityId',
'MAP', 'Fetches placeholder key-value pairs common to all sheets', 1, 'system');


-- Queries: Across Cycles sheet (sheet_config_id = 1)
INSERT INTO sheet_query (template_id, sheet_config_id, purpose, query_sql, query_type, description, active, created_by)
VALUES (1, 1, 'CLONE_DRIVER',
'SELECT scenario_name AS "scenarioName"
FROM scenario_master
WHERE cycle_id = :cycleId AND entity_id = :entityId
ORDER BY sort_order',
'LIST', 'Returns scenario names — row count drives clone copy count', 1, 'system');

INSERT INTO sheet_query (template_id, sheet_config_id, purpose, query_sql, query_type, description, active, created_by)
VALUES (1, 1, 'DATA',
'SELECT d.line_id,
       l.form_line_name,
       d.scenario_name,
       d.curr_pq1, d.curr_pq2, d.curr_pq3, d.curr_pq4, d.curr_pq5,
       d.curr_pq6, d.curr_pq7, d.curr_pq8, d.curr_pq9,
       d.prior_pq1, d.prior_pq2, d.prior_pq3, d.prior_pq4, d.prior_pq5,
       d.prior_pq6, d.prior_pq7, d.prior_pq8, d.prior_pq9
FROM projection_data d
JOIN line_items l ON d.line_id = l.line_id
WHERE d.cycle_id = :cycleId AND d.entity_id = :entityId
ORDER BY d.scenario_name, d.line_id',
'LIST', 'Returns all data rows — line items + period values per scenario', 1, 'system');


-- Queries: Q vs A sheet (sheet_config_id = 2)
INSERT INTO sheet_query (template_id, sheet_config_id, purpose, query_sql, query_type, description, active, created_by)
VALUES (1, 2, 'DATA',
'SELECT d.line_id,
       l.form_line_name,
       d.baseline_14q,
       d.base_pq1, d.base_pq2, d.base_pq3, d.base_pq4, d.base_pq5,
       d.base_pq6, d.base_pq7, d.base_pq8, d.base_pq9,
       d.stress_pq1, d.stress_pq2, d.stress_pq3, d.stress_pq4, d.stress_pq5,
       d.stress_pq6, d.stress_pq7, d.stress_pq8, d.stress_pq9
FROM qva_data d
JOIN line_items l ON d.line_id = l.line_id
WHERE d.cycle_id = :cycleId AND d.entity_id = :entityId
ORDER BY d.line_id',
'LIST', 'Returns Q vs A data — line items + base and stress period values', 1, 'system');


COMMIT;


-- ============================================================
-- USEFUL VIEWS
-- ============================================================

-- View: all active sheets with their queries for a template
CREATE OR REPLACE VIEW vw_template_sheet_queries AS
SELECT
    rt.template_id,
    rt.template_name,
    tsc.sheet_config_id,
    tsc.sheet_name,
    tsc.sheet_type,
    tsc.sort_order,
    tsc.active AS sheet_active,
    sq.query_id,
    sq.purpose,
    sq.query_type,
    sq.query_sql,
    sq.active AS query_active
FROM report_template rt
JOIN template_sheet_config tsc ON rt.template_id = tsc.template_id
LEFT JOIN sheet_query sq ON tsc.sheet_config_id = sq.sheet_config_id
WHERE rt.active = 1
ORDER BY rt.template_id, tsc.sort_order, sq.purpose;


-- View: common queries (not tied to any sheet)
CREATE OR REPLACE VIEW vw_common_queries AS
SELECT
    rt.template_id,
    rt.template_name,
    sq.query_id,
    sq.purpose,
    sq.query_type,
    sq.query_sql,
    sq.active
FROM report_template rt
JOIN sheet_query sq ON rt.template_id = sq.template_id
WHERE sq.sheet_config_id IS NULL
  AND rt.active = 1
  AND sq.active = 1;


-- View: report generation status
CREATE OR REPLACE VIEW vw_report_status AS
SELECT
    rgl.report_id,
    rgl.template_name,
    rgl.status,
    rgl.total_sheets,
    rgl.processed_sheets,
    rgl.error_message,
    rgl.requested_by,
    rgl.started_at,
    rgl.completed_at,
    ROUND((CAST(rgl.completed_at AS DATE) - CAST(rgl.started_at AS DATE)) * 86400, 2) AS duration_seconds
FROM report_generation_log rgl
ORDER BY rgl.created_at DESC;
