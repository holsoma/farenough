# Android UI audit

Reviewed 30 October 2026. Release: [v0.3.0](https://github.com/holsoma/farenough/releases/tag/v0.3.0).

The review applied the awwwards skill's design, usability, creativity and content rubric to the existing native Android app. The purpose remains concrete: show whether recorded journey fares cover a pass, then make the records behind that result easy to inspect. Kotlin, Jetpack Compose and Material 3 were retained. Website requirements such as SEO and hover effects were not scored as Android requirements.

## Changes shipped

- Compare two recorded months by fares, journeys, travel days and average fare. Select a month in the history chart, then open its daily charts and map.
- Browse recorded calendar months directly, including months outside a saved pass period.
- Keep the cumulative chart above its detailed totals and explain the pass-price threshold before the chart.
- Label the busiest map stations and show a selected connection's stations and journey count above the map.
- Wrap view controls and stack comparison values at larger text sizes.
- Keep Save journey visible beneath the scrolling entry form, with Swap beside the two station fields.
- Label the Pass action, state the pass coverage percentage, and change the overview action after the pass is covered.
- Use brief success messages instead of a blocking copy confirmation. Explain the import process in ordinary language.
- Show unpriced records as **Not priced** and withhold incomplete monetary comparisons.

## Review estimates

Each pass used a separate critic who saw screenshots and the review rubric, without app code or earlier scores. The weights are Design 40%, Usability 30%, Creativity 20% and Content 10%.

| Pass | Design | Usability | Creativity | Content | Weighted |
| --- | ---: | ---: | ---: | ---: | ---: |
| Baseline | 6.7 | 6.0 | 6.5 | 7.2 | 6.48 |
| First signed build | 7.0 | 6.5 | 6.5 | 7.5 | 6.80 |
| Published v0.3.0 | 7.2 | 6.8 | 6.7 | 7.4 | 7.00 |

These are independent review estimates, not official Awwwards ratings. The final estimate remains below the skill's 8/10 target.

## Final evidence

All 14 final screenshots were inspected. They came from the actual signed release on Android API 36, at 720 × 1600 pixels and density 320. Comparison captures also cover text scale 1.3. All records in public screenshots are invented and their example fares are labelled. No personal travel history is included.

| Area | Evidence |
| --- | --- |
| Pass result | [Overview](site/public/screenshots/overview.png), [dark overview](site/public/screenshots/overview-dark.png), [trip target](site/public/screenshots/target.png) |
| Insights | [Chart](site/public/screenshots/charts.png), [map](site/public/screenshots/map.png), [selected connection](site/public/screenshots/map-selected.png) |
| Months | [Comparison](site/public/screenshots/month-comparison.png), [history](site/public/screenshots/month-history.png), [dark comparison](site/public/screenshots/comparison-dark.png) |
| Larger text | [Comparison controls](site/public/screenshots/comparison-large.png), [stacked values](site/public/screenshots/comparison-large-details.png) |
| Recording | [Journey list](site/public/screenshots/trips.png), [import](site/public/screenshots/import.png), [entry form](site/public/screenshots/add-trip.png) |

[Screenshot workflow run](https://github.com/holsoma/farenough/actions/runs/40925988359). The workflow checks the published APK checksum and signing certificate before capture. APK SHA-256: `ff29ed73f609c328212df90b0a3b691111ef5e6197ccf38ab54de970a8e22a1e`.

[Release checks](https://github.com/holsoma/farenough/actions/runs/40888845283) passed, including 46 core tests, 11 Android device tests, Android lint and the website build. Automated tests verify behaviour; they do not establish visual quality. The older `automatic-fare.png` and `automatic-distance.png` documentation images are retained from v0.2.0 and were excluded from this final review.

## Remaining improvements

1. Fade unselected map connections and outline the selected connection so overlapping routes are easier to distinguish.
2. Add intermediate date and currency labels to the cumulative chart. The app already shows the selected date and cumulative amount, but the final captures do not include that state.
3. Simplify the month-history progress bars. Their default track and endpoint marks do not explain additional data.
4. Show an accepted import example near the input and make the review step easier to discover. The external AI formatting step remains part of the requested import workflow.
5. Reduce the gaps between Insights controls to leave more space for data.

The screenshots establish visible layout at one phone size. They do not establish TalkBack order, keyboard behaviour, animation quality, frame rate or performance on a range of devices. System animations were disabled during capture. No measured accessibility or performance score is claimed.
