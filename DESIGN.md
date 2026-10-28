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
