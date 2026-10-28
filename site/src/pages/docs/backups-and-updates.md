---
layout: ../../layouts/Docs.astro
title: Backups and updates
description: Keep a portable copy of your records and update the APK without losing them.
---

## Full backup

Choose **More → Create full backup**. Select a destination through Android's file picker. The versioned JSON file contains passes, journeys, saved route fares, assessments, import batches, appearance and favourite routes.

The file is unencrypted. You control where it goes. Automatic cloud backup is disabled for the app's private records.

## Restore

Choose **Restore a backup**, select a file and read its summary. Restoration replaces existing records. Before replacement, the app asks you to export the current records. If that export fails or is cancelled, replacement does not begin.

The incoming file is validated before any change. The database replacement is atomic: all of it succeeds or none of it does. Unknown backup versions and invalid references are rejected.

## CSV is different

CSV export is useful for spreadsheets and independent analysis. It is not a full backup and cannot restore pass settings or import history. Use the JSON backup when moving phones.

## Update the APK

Download the new release from the same [GitHub repository](https://github.com/holsoma/fairenough/releases) and install it over the existing app. Keep the application installed: uninstalling deletes local data. Android checks the signing identity during an update.

Directly installed APKs do not update themselves. Release notes state Android requirements, data-format changes and fare coverage. Keep a backup before installing an update.
