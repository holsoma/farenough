# Changelog

## 0.2.0

- Show adult MRT/LRT fares and rail fare distances immediately after station selection.
- Bundle results for 16,836 pairs among 184 stations, covering 12 July to 25 December 2026.
- Price eligible imports and previously unpriced journeys without replacing existing prices or corrections.
- Show per-journey distance and period totals in Insights, with explicit distance coverage.
- Allow saved route fares to override automatic amounts and restore automatic pricing when an override is removed.
- Keep fare previews visible after station selection by dismissing the keyboard.

The fare table is inside the APK, excluded from Git and separate release downloads. Rail fare distance can differ from the route taken. Eight repeated calculator results differ from the general distance bands; the app retains the calculator values. Automatic transfer grouping, time discounts and bus fares remain outside scope.

Compatibility: Android 8.0+, unchanged application ID and signing key, version code 5. Import and backup formats remain version 1. Install over the existing app without uninstalling.

## 0.1.0

- Record MRT/LRT journeys, undergraduate pass periods and favourite routes.
- Compare recorded fares with pass cost, keeping missing fares explicit.
- Save checked regular route fares and correct individual journeys.
- Import version 1 normalised journey JSON with review, duplicate checks and batch undo.
- View trip targets, daily and cumulative charts, directional rankings and an offline Singapore map.
- Export full backups or CSV; validate restores before replacing records.
- Provide light, dark and system appearance, with no accounts or telemetry.

Fare coverage: user-entered LTA-checked route fares. No bundled automatic matrix, transfer grouping or automatic time discounts.

Compatibility: Android 8.0+, `farenough-trips` version 1, `farenough-backup` version 1.
