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
- [x] Website, full documentation, screenshots and two visual QA passes.
- [x] Repository and draft release workflow, checksums and signing identity.
- [ ] Public release and Pages deployment after reviewing the concrete result.

Implementation progress and verification are recorded here. A checked build alone does not prove these requirements.

## Verified on 28 October 2026

- Nineteen core tests pass, including compatibility with earlier import and backup format names. Public fixtures retain the acceptance totals using synthetic dates and station names.
- Three Android instrumentation tests pass on an API 36 emulator, covering pass creation, trip entry, JSON import, transactional import undo, persistence, light/dark system-bar contrast and screenshot capture. The former diagnostic fingerprint method is no longer counted as a test.
- Android lint: no errors. Dependency update suggestions remain warnings.
- Release APK is signed with the certificate in `SIGNING.md`; debug and release application IDs are separate.
- Eight screenshots use invented records and example fares. Both light and dark themes were captured.
- Website production build: 13 pages, no Astro diagnostic errors or warnings. The first independent visual review identified code contrast, link targets, mobile guide navigation and release wording for correction.
- Station IDs are recorded in `data/station-ids.json` so adding a line code cannot silently change an existing journey's station identity.
- Signed API 36 update check: installed version code 1, created a pass and corrected journey through the UI, restored a synthetic fixture containing saved fares, imports and preferences, then installed version code 2 without uninstalling. Exported data matches every field of the fixture. The pre-restore export matches the original records exactly.
- The renamed candidate uses version code 4. Installing it over version code 3 preserved every field in the exported synthetic backup. The display name and new export filenames use farenough; the Android installation identity remains unchanged. A dark status-bar contrast correction is included. A 150% text-size check retained readable content and navigation.
- The final Android, device and website jobs pass in [GitHub run 40647246801](https://github.com/holsoma/farenough/actions/runs/40647246801). The device job frees unused toolchains on its temporary runner before creating the API 36 emulator. Tests wait for visible UI completion after asynchronous saves.
- The final APK was downloaded from the private draft and matched its local SHA-256 digest. The checksum file uses LF line endings and passes `sha256sum -c`.
- Website review covered three full screenshot passes and a targeted follow-up. Final evidence has no audit failures, console errors, serious accessibility findings, horizontal overflow or undersized touch targets. Phone LCP was below 0.8 seconds and CLS at most 0.001 in the measured captures. The audit's origin-root discovery warnings are explained by the GitHub Pages project path; the project sitemap, robots file, social image and real 404 response were verified separately.
- The final independent design estimate is 7.6/10: Design 7.5, Usability 8, Creativity 7, Content 8; six developer categories score 8. The awwwards target of 8 in every category has not been met. Further visual work would extend the line-and-stop structure through the feature section and reduce repetition in the import guide.

## Publication in progress

The user approved public publication and renamed the app and repository to farenough. The repository is now `holsoma/farenough`, and the site uses `/farenough/`. GitHub Actions signing secrets are configured. The signed version code 4 APK, nineteen core tests, three Android UI tests and thirteen-page site build pass locally. Release guard tests cover nine staging cases and rejected signing identities. The public release, workflow signing stage and Pages deployment remain to be verified.

Physical handset testing and testing on Android 8 are not yet complete. The minimum supported version is a build configuration, not a claim that every supported Android version has been tested.
