# farenough design

An Android concession-pass tracker for a Singapore undergraduate. Its first job is to make the recorded fare comparison legible, then make recording the next journey quick.

## Concept exploration

Scores are originality / fit / feasibility / portability (lower portability is better).

| Concept | Anchor and signature | Scores |
| --- | --- | --- |
| Enough is a stop | The pass has a fixed threshold. A marker advances along a line towards the stop where the cost is covered. | 4 / 5 / 5 / 1 |
| Tap through | A fare gate opens when an example pass is covered. | 3 / 5 / 4 / 2 |
| A month of journeys | A date grid builds the month's recorded connections. | 3 / 4 / 5 / 3 |

The visual system uses **Enough is a stop**: transit lines, stop markers and exact figures. The original slider has been removed at the user's request. The landing page now explains the calculation directly, using the same demonstration records as the app screenshot.

Idea: enough journeys cover the pass price.
Metaphor: a transit line with a financial destination.
Signature: the app screenshot's recorded fares, pass price and estimated savings are explained in a compact comparison.
Emotion: clarity in the first three seconds.
Motion verb: advance, then stop.
Screenshot: the question, a real Android screen and its clearly labelled fare comparison.

## System

- Ground #F6F8FA; ink #18242B; muted #52616B; line #D4DCE1; accent #006C67; marker #E8B339.
- Web: locally hosted Archivo, display 600/700, body 400. Android: system sans, tabular amounts. Do not use headings as decoration.
- Desktop: 12 columns, 24px gutters, maximum width 1240px. Mobile: 4 columns, 20px margins. Android: 20dp horizontal padding, 48dp targets.
- One 16px radius for prominent surfaces; smaller controls use Material 3 defaults. Flat surfaces, no glass effects or gradient blobs.
- Motion: power3.out, 180ms controls and 400ms main transition. One GSAP ticker. No loader or WebGL. Landing page can use Lenis on desktop; documentation keeps native scrolling.
- Still at rest is intentional. The comparison uses ordinary text and requires no interaction or JavaScript. Reduced motion retains all content and disables page transition motion.

## Website composition

Hero, asymmetric pair:
```
question + explanation     real app screen
download / guide          short screenshot caption
```
Calculation, compact two-column section:
```
explanation + guide link       recorded fares    S$57.20
                               pass price        S$48.00
                               estimated savings  S$9.20
```
Usage, index rows:
```
Record a journey       description / actual screenshot
Bring your records    description / actual screenshot
See your month        description / actual screenshot
```
Documentation, quiet statement with a guide index. Footer: wordmark, source, privacy, release version.

The genericness check removed equal feature cards, decorative labels and automatic count-up animations. The app uses normal Android navigation. Its interactive statistics belong in the app; the website explains the comparison without a separate fare simulator.

## Verification

Capture real app screens with synthetic records. Check phone and desktop, light/dark app themes, large text, keyboard access, reduced motion and screen readers. Run two production website review passes and an independent screenshot critique. No final score until evidence exists.

The website uses a static comparison of recorded fares and pass price. The trip slider, fare selector, event listeners, resize observer and their styles have been removed. The import explanation follows a stop sequence from an invented note through clarification to review. Actual screenshots show the journey list and map; full screenshots remain linked.

The original release's independent estimate was 7.6/10. This predates the comparison change. See `IMPLEMENTATION.md` for the tested scope and published release. These are review estimates, not awards or claims about an Awwwards submission.


## Android history review, October 2026

The current work applies the awwwards skill to the existing native Kotlin and Jetpack Compose app. Android Material 3 remains the stack because its controls support system text sizing, touch, focus, TalkBack and platform motion preferences. Website-only requirements such as SEO, GSAP, loaders and browser hover do not apply to an APK.

Brief: make a personal Singapore travel history easy to inspect and compare, while keeping the pass question primary. Use real app screens with clearly synthetic demonstration data for public screenshots. The owner's supplied history is used only in ignored local test files. No invented statistics or pass savings appear in calendar comparisons.

Three directions, scored originality / fit / feasibility / portability (lower portability is better):

- Threshold and history: extend the existing pass destination with a month selector that updates the fare, journey and route views together. Anchor: the app compares a fixed pass price with dated journeys. Signature: selecting a month changes the comparison and its labelled bar. Feeling: immediate understanding. Screenshot: two named months, a fare difference and their journey counts. Scores 4 / 5 / 5 / 1.
- Journey receipt: set every screen as a compact dated receipt. Anchor: individual fares and corrections. Signature: expand each receipt into its stations. Feeling: precision. Screenshot: one long fare breakdown. Scores 3 / 4 / 5 / 3.
- Station departures: style all data as a departure board. Anchor: Singapore rail travel. Signature: rows change like service information. Feeling: familiarity. Screenshot: a grid of routes and amounts. Scores 3 / 3 / 4 / 3.

Choose threshold and history because it extends the established purpose without making routine input theatrical. Stillness at rest is deliberate. Native press feedback, selected controls and immediate data changes provide feedback. No automatic number-counting or decorative movement.

System: retain the teal accent, pale blue-grey surfaces, amber selection and system sans. Reduce page titles to 24sp to preserve useful space on 360dp screens. Use a 20dp outer margin, 8/12dp control gaps and 20dp section gaps. Keep touch targets at least 48dp and allow horizontal control rows to scroll at large text sizes. Set every surface tone explicitly so dialogs and menus do not introduce unrelated purple defaults. Large amounts use medium weight; table rows align their values.

Composition:

```
Overview: question -> dated pass result -> coverage percentage -> trip count -> next action
Charts: heading -> view tabs -> period selector -> compact totals -> labelled chart -> daily detail
Compare: heading -> view tabs -> two named months -> fare total and difference -> metric rows -> month bars
Map: heading -> view tabs -> period -> map -> totals -> ranked routes
Import: short instructions -> copy action -> formatted records -> review -> explicit confirmation
```

The review removed a blocking copy-success dialog, unexplained import terminology, excessive chart-summary height and an ambiguous filled progress bar. Success feedback uses a snackbar. Error and destructive-action confirmations retain dialogs. Monthly fare changes are withheld if either month has unpriced journeys. Average fares identify priced coverage. Calendar fare totals are not labelled as pass savings; pass comparisons retain actual validity dates.

Baseline independent screenshot review: Design 6.7, Usability 6.0, Creativity 6.5, Content 7.2, weighted 6.48. This is an adapted estimate, not an official Awwwards score. Baseline captures exposed a copy confirmation covering later screens. Final evidence must include unobscured screens, the month comparison, light and dark themes, large text, chart selection and an existing-record upgrade.
