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
- [x] Public release and GitHub Pages deployment.

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
- The release's Android, device, website and release guard jobs pass in [GitHub run 40656876286](https://github.com/holsoma/farenough/actions/runs/40656876286). The device job frees unused toolchains on its temporary runner before creating the API 36 emulator. Tests wait for visible UI completion after asynchronous saves.
- The final APK was downloaded from the private draft and matched its local SHA-256 digest. The checksum file uses LF line endings and passes `sha256sum -c`.
- Website review covered three full screenshot passes and a targeted follow-up. Final evidence has no audit failures, console errors, serious accessibility findings, horizontal overflow or undersized touch targets. Phone LCP was below 0.8 seconds and CLS at most 0.001 in the measured captures. The audit's origin-root discovery warnings are explained by the GitHub Pages project path; the project sitemap, robots file, social image and real 404 response were verified separately.
- The final independent design estimate is 7.6/10: Design 7.5, Usability 8, Creativity 7, Content 8; six developer categories score 8. The awwwards target of 8 in every category has not been met. Further visual work would extend the line-and-stop structure through the feature section and reduce repetition in the import guide.

## Published release

The [repository](https://github.com/holsoma/farenough) is public. [Release v0.1.0](https://github.com/holsoma/farenough/releases/tag/v0.1.0) contains one signed APK, its checksum and certificate. [The tagged release workflow](https://github.com/holsoma/farenough/actions/runs/40656876286) passed every check and built the signed APK. An anonymous download matched SHA-256 `fc890f94c67e682fc91fb27f908591b747ca4fe52548d8f723c154349409f4b8`; that exact APK installed successfully over the existing installation. Release guard tests cover nine staging cases and rejected signing identities.

The [website and guide](https://holsoma.github.io/farenough/) are live. [Pages deployment 40658501798](https://github.com/holsoma/farenough/actions/runs/40658501798) succeeded from `main`, using the existing environment permission. All twelve regular pages, fourteen public assets, the APK download link and the custom 404 passed live verification. Future releases deploy the site from `main` as documented in `RELEASING.md`.

Physical handset testing and testing on Android 8 are not yet complete. The minimum supported version is a build configuration, not a claim that every supported Android version has been tested.

## Automatic-fare candidate, 29 October 2026

Version 0.2.0, code 5, adds offline adult fare and rail fare distance lookup for every pair among the 184 supported stations. All 16,836 pairs were collected from the LTA calculator. A separate set of 193 reverse-direction requests matched every stored distance and fare, including eight prices which differ from the general distance-band table. The dataset is limited to 12 July through 25 December 2026. See [DATA_SOURCES.md](DATA_SOURCES.md) for provenance and the unresolved redistribution terms.

- All 28 core tests pass, including full dataset coverage, official quote fixtures, date boundaries, import pricing, distance totals and preservation of existing prices and corrections.
- All six Android instrumentation tests pass on API 36. They cover live station/date changes, automatic import prices, distance totals, saved-fare removal, unchanged edits to stored fares, navigation, backup round trips and import undo.
- Android lint reports no errors. The date field no longer retains focus behind station selection and reopens its keyboard over the fare preview.
- The signed APK contains the exact validated asset, whose SHA-256 is `b552b7437884bcae048f084be74d702cb435e5566eec3cf65113f7c94c1489c4`.
- Installed the new signed APK over published v0.1.0, code 4, without uninstalling. Before/after exports match every field except the three expected automatic pricing fields on one previously unpriced journey. Its regular fare became S$1.49. The saved S$2.00 route fare, S$1.50 individual correction, pass, preferences, favourite route and import history remained unchanged.

Local deliverable: `artifacts/farenough-v0.2.0-local.apk`, SHA-256 `451fc31304bcfbca0e365e31eb05c3fc4487205411ee0814545a30506098e81e`. It uses the original certificate recorded in `SIGNING.md`. The adjacent checksum, `rail-fares-report.json`, `automatic-fares-upgrade-check.json` and `automatic-fares-screenshots/` contain the delivery evidence. This candidate was initially kept local. The repository owner subsequently authorised publishing this exact signed APK with the fare table embedded, while keeping the table out of Git and separate release downloads. Public source builds restore the asset from that checksum-pinned APK.

## Published v0.2.0

[Release v0.2.0](https://github.com/holsoma/farenough/releases/tag/v0.2.0) publishes that exact tested APK as `farenough-v0.2.0.apk`, alongside only `SHA256SUMS.txt` and `signing-certificate.txt`. The uploaded draft and an anonymous public download matched the recorded checksum and signing identity. The fare table and raw research remain excluded from Git; the table is bundled inside the APK. Its source tag points to `cf449d8e23186f11fdabd439f5e7db449e139299`.

The [main checks](https://github.com/holsoma/farenough/actions/runs/40838347320) and [tagged release workflow](https://github.com/holsoma/farenough/actions/runs/40838306451) passed Android, emulator, website, release-guard and packaging jobs. The release guard retained the already published APK. Four additional build-helper tests verify checksum failures, missing assets and extraction of only the intended file. Source builds successfully restored the verified table from the public APK.

[Pages deployment](https://github.com/holsoma/farenough/actions/runs/40838347321) succeeded from `main`. Anonymous verification passed for twelve live pages, sixteen assets, the v0.2.0 APK link, automatic-fare documentation, new screenshots and the custom 404 response. The temporary release branch was removed after its commit was preserved on `main` and the version tag.


Version 0.3.0, code 8, adds selective earlier coverage for 16,106 station pairs from 1 March 2025 through 11 July 2026. A conservative whole-pair distance check excludes any route potentially affected by CCL6, unopened stations and calculator band anomalies. Derived quotes have their own source and version. Existing unpriced records are assessed on load without reimporting, while existing prices and corrections are retained. Entirely unpriced totals display Not priced. The method and limits are in DATA_SOURCES.md.
