# Implementation record

The agreed scope is a native Android app, signed APK, GitHub release assets, an Astro documentation site, screenshots, local import/export, fare comparisons, interactive charts and an offline Singapore connection map. Code licence: Apache 2.0.

## Locked product decisions

- Undergraduate train pass, default S$48, compared with regular adult fares.
- Separate recorded journeys, MRT and LRT, inclusive pass dates.
- No accounts, telemetry, automatic cloud backups or in-app AI.
- The external LLM clarifies personal station abbreviations. The app only validates official names/codes.
- Opposite directions remain separate in rankings.
- The offline map shows endpoints and connections, never inferred train paths.
- If complete reusable automatic fare data is not verified, the first APK uses saved LTA-checked route fares. Missing fares are never zero.
- A saved regular route fare and a correction to a single trip are different records.

## Acceptance and delivery checklist

- [x] Domain calculations and validation, including sample counts 45 / 23 dates / September 6 / October 39.
- [x] Passes and trip editing, favourites, saved route fares and corrections.
- [x] Prompt, validated import review, duplicate decisions, transactions and batch undo.
- [x] Overview, Target, interactive statistics and directional rankings.
- [x] Offline map, official source attribution, accessible alternatives.
- [x] Versioned backup, pre-restore export and CSV.
- [x] Signed APK install/upgrade check and Android UI verification.
- [ ] Website, full documentation, screenshots and two visual QA passes.
- [ ] Repository and draft release workflow, checksums and signing identity.
- [ ] Public release and Pages deployment after reviewing the concrete result.

Implementation progress and verification are recorded here. A checked build alone does not prove these requirements.

## Verified on 28 October 2026

- Eighteen core tests pass. Public fixtures retain the acceptance totals using synthetic dates and station names.
- Four Android instrumentation tests pass on an API 36 emulator, covering pass creation, trip entry, JSON import, transactional import undo, persistence and screenshot capture.
- Android lint: no errors. Dependency update suggestions remain warnings.
- Release APK is signed with the certificate in `SIGNING.md`; debug and release application IDs are separate.
- Eight screenshots use invented records and example fares. Both light and dark themes were captured.
- Website production build: 13 pages, no Astro diagnostic errors or warnings. The first independent visual review identified code contrast, link targets, mobile guide navigation and release wording for correction.
- Station IDs are recorded in `data/station-ids.json` so adding a line code cannot silently change an existing journey's station identity.
- Signed API 36 update check: installed version code 1, created a pass and corrected journey through the UI, restored a synthetic fixture containing saved fares, imports and preferences, then installed version code 2 without uninstalling. Exported data matches every field of the fixture. The pre-restore export matches the original records exactly.

Physical handset testing and testing on Android 8 are not yet complete. The minimum supported version is a build configuration, not a claim that every supported Android version has been tested.
