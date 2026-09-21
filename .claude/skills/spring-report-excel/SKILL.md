---
name: spring-report-excel
description: Generate or modify Excel report exports in Java Spring backends using Apache POI and a reusable report engine. Use when Codex needs a Swagger-documented download endpoint, `byte[]` Excel response, annotated report rows, conditional cell styles, header generation, attachment headers, or hierarchical export flows flattened into spreadsheet rows.
---

# Spring Report Excel

Follow a reusable Excel export pattern for Spring services and controllers.

## Read first

- Any existing report controller or export endpoint
- The service that assembles export rows
- The shared Excel generator utility, if one exists
- The annotation or mapping strategy used for column labels
- A report row class or response already used by another export
- The helper used to set attachment headers
- `references/report-patterns.md`

## Workflow

1. Filter source data in the service layer.
2. Flatten domain objects into a dedicated report row class.
3. Annotate row fields with the project's column-label annotation or equivalent mapping metadata.
4. Register `CellStyleResolver` instances by field name when conditional styling is needed.
5. Return `byte[]` from the service and attach Excel headers in the controller.

## Report row rules

- Create a dedicated row model instead of exporting entities directly.
- Convert dates to display strings inside the report row object when the current report style expects formatted text.
- Derive presentation-only fields such as deadline status in the row object.
- Keep column labels in an annotation or explicit mapping, depending on the target codebase.

## Generator rules

- Reuse the project's shared Excel generator instead of duplicating Apache POI setup.
- Pass a `Map<String, CellStyleResolver>` keyed by the field name from the report row class.
- Let the generator build headers from reflection and annotations.

## Controller rules

- Return `ResponseEntity<byte[]>`.
- Use the project's existing helper for Excel attachment headers when available.
- Keep filtering parameters aligned with the list endpoint when the export mirrors an existing search.
- Document export endpoints and parameters with Swagger/OpenAPI.

## Constraints

- Keep report assembly in `service/`.
- Keep POI boilerplate and sheet styling in `utils/`.
- Add a new style resolver only when a column needs conditional coloring or formatting.
