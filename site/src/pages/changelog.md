---
layout: ../layouts/Docs.astro
title: Changelog
description: Changes to the app, data formats and fare coverage.
---

## 0.1.0 preview

The first version introduces undergraduate train-pass comparisons, manual journey entry, external-LLM-assisted importing, saved route fares and trip-specific fare corrections.

It includes targets based on your average or a selected route, daily and cumulative charts, directional route rankings, and an offline Singapore map. Full JSON backup/restore and CSV export are included.

### Fare coverage

This version uses user-entered, LTA-checked route fares. No complete automatic fare matrix is included. Unpriced journeys remain visible and monetary results identify incomplete coverage.

### Compatibility

- Android 8.0 or later.
- Import format: `fairenough-trips`, version 1.
- Full backup format: `fairenough-backup`, version 1.
- Application records remain local, with no automatic cloud backup.

Published APKs and final release notes appear on [GitHub Releases](https://github.com/holsoma/fairenough/releases).
