---
layout: ../layouts/Docs.astro
title: Changelog
description: Changes to the app, data formats and fare coverage.
---

## 0.2.0

Station selection now shows the regular adult fare and rail fare distance automatically. The APK includes 16,836 station pairs among 184 stations for travel dates from 12 July to 25 December 2026.

Eligible imports and previously unpriced journeys receive fares automatically. Existing recorded prices and individual corrections are preserved. Insights adds distance totals with coverage counts. Saved route fares remain available as overrides and can be removed to restore automatic pricing.

The update uses the existing signing key and keeps import and backup formats at version 1. Install it over the existing app without uninstalling.

[Read the fare coverage and limits](../docs/fare-calculations/).

## 0.1.0

The first version introduces undergraduate train-pass comparisons, manual journey entry, external-LLM-assisted importing, saved route fares and trip-specific fare corrections.

It includes targets based on your average or a selected route, daily and cumulative charts, directional route rankings, and an offline Singapore map. Full JSON backup/restore and CSV export are included.

### Fare coverage

This version uses user-entered, LTA-checked route fares. No complete automatic fare matrix is included. Unpriced journeys remain visible and monetary results identify incomplete coverage.

### Compatibility

- Android 8.0 or later.
- Import format: `farenough-trips`, version 1.
- Full backup format: `farenough-backup`, version 1.
- Application records remain local, with no automatic cloud backup.

Published APKs and final release notes appear on [GitHub Releases](https://github.com/holsoma/farenough/releases).
