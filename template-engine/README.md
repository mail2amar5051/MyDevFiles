# Dynamic Excel Template Engine

A generic Spring Boot service that generates Excel reports by cloning column blocks from a template stored as a BLOB in the database.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    REST API Layer                        │
│  GET /api/reports/{reportId}/generate → .xlsx download   │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│               TemplateEngineService                      │
│  Orchestrates: fetch template → fetch data → process     │
└────────────────────────┬────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
   DataFetchService  Processors    PlaceholderReplacer
   (DB queries)      (cloning)     (find/replace)
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
   ColumnBlockCloner  MergedRegion  FormulaShifter
   CellCopier         Handler
```

## How It Works

1. **Template** (.xlsx with placeholders like `{{scenarioname}}`) is stored as BLOB
2. **Rules JSON** defines what to clone, what to preserve, which queries to run
3. **Two DB queries** return runtime data:
   - **Scenarios list**: determines how many times to clone the block
   - **Placeholders map**: common values to replace across all clones
4. Engine clones the column block N times, replaces placeholders, returns .xlsx

## Database Tables

| Table | Purpose |
|-------|---------|
| `report_template` | Stores .xlsx template as BLOB |
| `report_config` | Stores the rules JSON per report |
| `query_registry` | Maps queryKey → actual SQL (decouples JSON from SQL) |
| `report_scenarios` | Runtime: list of scenarios per report |
| `report_placeholders` | Runtime: placeholder values per report |

## JSON Config Format

```json
{
  "templateId": 101,
  "reportId": 5001,
  "queries": {
    "scenarios": { "queryKey": "GET_SCENARIOS", "returns": "LIST" },
    "placeholders": { "queryKey": "GET_PLACEHOLDERS", "returns": "MAP" }
  },
  "sheets": [
    {
      "sourceSheetName": "Sheet1",
      "outputSheetName": "Across_cycle_scenarios",
      "staticColumns": ["A", "B"],
      "cloneBlock": {
        "columns": "C:Y",
        "repeatForEach": "scenarios",
        "scenarioNamePlaceholder": "{{scenarioname}}",
        "scenarioNameField": "scenarioName"
      },
      "commonPlaceholders": "placeholders",
      "preserve": ["mergedRegions", "formulas", "styles", "columnWidths"]
    }
  ]
}
```

## Processing Order (Critical)

```
1. Open template BLOB → XSSFWorkbook
2. Rename sheet → outputSheetName
3. Clone column blocks RIGHT-TO-LEFT (avoids index shifting)
   For each clone:
     a. Copy cells (values, styles, types)
     b. Copy column widths
     c. Clone merged regions with offset
     d. Shift formula column references
4. Replace per-clone placeholder (scenario name) in each block
5. Replace common placeholders across entire sheet
6. Force formula recalculation
7. Write → byte[] → HTTP download
```

## Project Structure

```
src/main/java/com/report/engine/
├── TemplateEngineApplication.java       # Spring Boot entry point
├── config/
│   ├── GlobalExceptionHandler.java      # REST error handling
│   └── TemplateProcessingException.java # Custom exception
├── controller/
│   └── ReportController.java            # GET /api/reports/{id}/generate
├── dto/
│   ├── TemplateConfig.java              # Root JSON model
│   ├── QueryConfig.java                 # Query definitions wrapper
│   ├── QueryDef.java                    # Single query definition
│   ├── SheetConfig.java                 # Per-sheet rules
│   └── CloneBlockConfig.java            # Column range + helpers
├── entity/
│   ├── ReportTemplate.java              # JPA: template BLOB
│   └── ReportConfig.java                # JPA: rules JSON
├── repository/
│   ├── ReportTemplateRepository.java
│   └── ReportConfigRepository.java
├── service/
│   ├── TemplateEngineService.java       # Main orchestrator
│   └── DataFetchService.java            # Runs DB queries by key
└── processor/
    ├── ColumnBlockCloner.java           # Clones column blocks
    ├── CellCopier.java                  # Cell copy with style cache
    ├── MergedRegionHandler.java         # Merged region duplication
    ├── FormulaShifter.java              # Formula column shifting
    └── PlaceholderReplacer.java         # {{placeholder}} replacement

src/main/resources/
├── application.yml                      # DB + server config
├── schema.sql                           # Oracle DDL + sample data
└── sample-config.json                   # Example rules JSON
```

## Setup

1. Create Oracle tables using `schema.sql`
2. Upload your .xlsx template into `report_template.template_blob`
3. Insert your rules JSON into `report_config.sheet_rules`
4. Register your SQL queries in `query_registry`
5. Insert runtime data (scenarios, placeholders)
6. Configure `application.yml` with your Oracle connection
7. `mvn spring-boot:run`
8. `GET http://localhost:8080/api/reports/5001/generate` → downloads .xlsx

## Key Design Decisions

- **No hardcoded values**: JSON defines structure, DB provides all values
- **Query registry**: SQL queries are stored in DB, referenced by key in JSON
- **Style caching**: CellCopier reuses styles to avoid POI's 64K limit
- **Right-to-left cloning**: Prevents column index shifting during insertion
- **Formula-aware**: Shifts column references within cloned formulas
- **Merged region-safe**: Duplicates and offsets all merged regions per clone
