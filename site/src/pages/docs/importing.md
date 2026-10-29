---
layout: ../../layouts/Docs.astro
title: Importing your records
description: Turn existing notes into a checked list of journeys without giving farenough access to an AI service.
---

## The copy-and-paste workflow

Use an AI chat tool, also called an LLM, to put your notes into JSON: a text format the app can check and import. The example below shows that format.

1. Open **Import journeys** and choose **Copy formatting prompt**.
2. Paste the prompt into any LLM, followed by your records.
3. Answer its questions about personal abbreviations, missing years and unclear dates.
4. Copy the final JSON into farenough, or open a JSON file.
5. Review the rows, dates and stations, then confirm the selected journeys.

The prompt includes the app's station catalogue. Personal abbreviations are resolved in that external conversation. farenough validates official names and codes; it does not guess your shorthand.

New files use `farenough-trips`, version 1. Earlier files labelled `fairenough-trips` are also accepted, with the same duplicate checks.

## One note, from start to confirmation

Suppose your note says `12 OCT TP -> RP`. This is an invented example.

The chat tool must ask which year you mean and whether `TP` is Toa Payoh, Tanjong Pagar or another station. You answer **2026, Toa Payoh to Raffles Place**. It can then produce the record below. It must not invent a fare.

```json
{
  "format": "farenough-trips",
  "version": 1,
  "trips": [
    {
      "date": "2026-10-12",
      "origin": "Toa Payoh",
      "destination": "Raffles Place",
      "sourceText": "12 OCT TP -> RP"
    }
  ]
}
```

The app's review shows **12 Oct 2026, Toa Payoh → Raffles Place**. The app calculates the adult fare automatically for this covered date. Dates outside the bundled period show **Fare needed** unless a checked fare applies. You check the row and confirm the import before it enters your records.

## Version 1 format

```json
{
  "format": "farenough-trips",
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
