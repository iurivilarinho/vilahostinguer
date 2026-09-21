# Report Patterns

## Typical pieces

- Export endpoint in a controller
- Orchestration service that loads and flattens source data
- Generic workbook builder utility
- Column annotation or equivalent mapping metadata
- Attachment header helper
- Dedicated row model for the spreadsheet

## Conditional styles already supported

- `status`
- `priority`
- `type`
- `deadlineStatus`

These keys usually match field names in the report row class or the exported column mapping.

## Existing visual rules

- Header with bold white text and green background
- Centered content
- Wrap text enabled
- Auto-filter on the first row
- Auto-size columns after filling data

## Typical extension path

1. Add or update a report row model.
2. Extend the report service to populate rows.
3. Add a resolver if a new styled column is needed.
4. Reuse the same controller/header pattern for download.
