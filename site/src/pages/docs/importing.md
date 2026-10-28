---
layout: ../../layouts/Docs.astro
title: Importing your records
description: Turn existing notes into a checked list of journeys without giving fairenough access to an AI service.
---

## The copy-and-paste workflow

Use an AI chat tool, also called an LLM, to put your notes into JSON: a text format the app can check and import. The example below shows that format.

1. Open **Import journeys** and choose **Copy formatting prompt**.
2. Paste the prompt into any LLM, followed by your records.
3. Answer its questions about personal abbreviations, missing years and unclear dates.
4. Copy the final JSON into fairenough, or open a JSON file.
5. Review the rows, dates and stations, then confirm the selected journeys.

The prompt includes the app's station catalogue. Personal abbreviations are resolved in that external conversation. fairenough validates official names and codes; it does not guess your shorthand.

## Version 1 format

```json
{
  "format": "fairenough-trips",
  "version": 1,
  "trips": [
    {
      "date": "2026-10-01",
      "origin": "Toa Payoh",
      "destination": "Raffles Place",
      "note": "Optional note",
      "sourceText": "Optional original text"
    }
  ]
}
```

Only `date`, `origin` and `destination` are required for each journey. Dates must include the year. Endpoints may be full official names or official codes, for example `NS19`. Ambiguous names need an unambiguous code. Extra properties and unsupported versions are rejected. The import does not accept AI-generated fares.

Keep individual imports below 2 MB and 10,000 journeys. A surrounding JSON Markdown code fence is accepted.

## Errors and duplicates

Invalid rows remain visible with an explanation. Edit the JSON or return to the LLM to correct them. You can explicitly select only the valid rows.

An identical complete batch cannot be imported twice. Rows matching existing dates and routes start unselected for review. Repeated journeys within the new file are preserved because you may legitimately take the same route more than once.

Nothing is saved before confirmation. The selected rows are saved together. **More → Recent imports → Undo** removes only journeys from that batch, with a warning about any later edits.
