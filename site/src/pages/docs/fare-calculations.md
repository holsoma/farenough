---
layout: ../../layouts/Docs.astro
title: Understanding the fares
description: What goes into the comparison, what is estimated, and where the data comes from.
---

## The comparison

fairenough adds the regular adult fares for recorded journeys within the selected pass's inclusive dates, using individual corrections where supplied. It subtracts the actual pass price.

The result is an estimate of the cost difference for the records you entered. Automatic transfer discounts, time-based discounts, rewards and bus travel are not calculated in this version.

## A worked example

These are invented amounts to show the calculation, not current fare quotes.

| Recorded journeys | Fare used | Total |
| --- | --- | --- |
| 20 journeys on a regular route | S$2.00 each | S$40.00 |
| 2 journeys on another route | S$2.50 each | S$5.00 |
| 1 journey with a known discount | S$1.20 correction, replacing its S$2.00 saved fare | S$1.20 |

The 23 journeys add up to **S$46.20**. Against a S$48.00 pass, **S$1.80 remains**. One further journey at S$2.00 would bring the total to S$48.20.

If you also record a journey whose fare is unknown, the app reports 23 priced journeys out of 24. The comparison is incomplete until you add that fare. The journey still appears in your trip count and map.

## Saving a route fare

The first release uses fares you check with the [official LTA calculator](https://www.lta.gov.sg/content/ltagov/en/map/fare-calculator.html). Choose the adult card fare and treat the recorded journey independently.

In **More → Saved route fares**, enter its origin, destination, amount and the fare period in which it applies. A saved fare is directional. Save the return direction separately if needed.

Before a change is applied, the app shows the number of journeys that will be reassessed. A corrected fare on a particular trip continues to take precedence.

An unknown fare never becomes zero. Monetary totals and trip targets say how many records are priced. Counts and map connections still include unpriced journeys.

## Why not calculate from the map?

Geographic coordinates do not represent official fare distances. The app does not multiply straight-line distances or count stations to invent a fare.

Open PTC fare bands and LTA station data were verified, but a complete current station-pair fare matrix with confirmed redistribution terms was not. The archived SimplyGo responses investigated during development contain fares, but their code repository's licence does not independently grant rights to the provider's data. Saved route fares are the explicit first-release fallback.

## Effective dates

The default saved-fare period runs from 27 December 2025 through 25 December 2026. Check dates and rates against the official source. PTC has announced a new adjustment from 26 December 2026. Do not apply a rate to a date outside its verified period.

## Sources and attribution

- [SimplyGo undergraduate pass conditions](https://simplygo.com.sg/concession-monthly-travel-cards/students/undergraduate-concession-card/).
- [PTC MRT/LRT fare bands](https://data.gov.sg/datasets/d_2ca29f673b89e0cfddbb7c74516fa4d3/view).
- [PTC 2026 fare review](https://www.ptc.gov.sg/media-centre/newsroom/fare-review-exercise-2026/).
- [LTA DataMall station footprints and names](https://datamall.lta.gov.sg/content/datamall/en/static-data.html), September 2026 footprint snapshot.
- [SLA National Map Polygon](https://data.gov.sg/datasets/d_29f066d67df3eae91df8a42f443863c8/view), coastal outlines, simplified for display.
- [LTA Circle Line 6 announcement](https://www.lta.gov.sg/content/ltagov/en/newsroom/2026/5/news-releases/circle-line-stage-6-to-open-for-public-preview-on-4-july-2026.html), supplementing the older station-name file.

The map datasets are adapted under the [Singapore Open Data Licence](https://datamall.lta.gov.sg/content/datamall/en/SingaporeOpenDataLicence.html). The repository records source URLs, checksums and transformations. fairenough is independent of LTA and SimplyGo.
