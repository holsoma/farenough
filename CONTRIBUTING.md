# Contributing

Use UK English, clear labels and explicit assumptions. Keep private travel records out of the repository and public issues.

## Checks

Run `./gradlew :core:test :app:lintDebug :app:assembleDebug` and, with an Android emulator or device, `./gradlew :app:connectedDebugAndroidTest`. Run `npm ci` and `npm run build` in `site/` for website changes.

The core tests cover monetary boundaries, pass dates, missing values, import validation, duplicate handling, backups and directional statistics. Add tests when changing those behaviours. Android instrumentation tests cover persistence, import transactions and the main navigation, and capture the screenshots used in documentation.

Database changes need an explicit Room migration and a migration test. Never use destructive migration fallback. Backup and import format changes require a version change or demonstrated backward compatibility.

## Fare data

Attach primary sources, effective dates and redistribution terms. Validate against official results. Approximate geographic distances are not acceptable fare inputs. See `DATA_SOURCES.md`.

## Pull requests

Explain the concrete problem, resulting behaviour and relevant checks. Keep the change focused. Include before/after screenshots for visible changes, using invented data.
