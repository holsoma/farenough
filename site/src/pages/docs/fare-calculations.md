---
layout: ../../layouts/Docs.astro
title: Understanding the fares
description: What goes into the comparison, what is estimated, and where the data comes from.
---

## The comparison

farenough adds the regular adult fares for recorded journeys within the selected pass's inclusive dates, using individual corrections where supplied. It subtracts the actual pass price.

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

## Automatic fares

Version 0.2.0 includes offline adult MRT/LRT calculator results for all **16,836 station pairs among 184 stations**. Selecting stations shows their regular fare and rail fare distance. Imported and previously unpriced journeys receive eligible fares automatically. Already recorded prices and individual corrections are preserved.

Full coverage runs from **12 July through 25 December 2026**, with results checked on 29 October 2026. Line changes inside one paid journey are included. Each recorded entry starts a separate journey, so transfer and time-based discounts need an individual correction.

## Earlier records, including June 2026

From v0.3.0, **16,106 station pairs** also receive calculated adult fares for **1 March 2025 through 11 July 2026**. It selects the fare schedule for the travel date, including the lower rates before 27 December 2025. The app checks that the Circle Line extension could not have shortened that pair's fare distance, then uses the fare schedule effective for the earlier date. These are labelled **Adult fare derived for the pre-July network**, rather than presented as historical calculator observations.

For 2 June 2026, Punggol Coast to Bugis and Bugis to Bukit Panjang each calculate as **S$2.11**, totalling **S$4.22** as separate regular adult journeys. Existing unpriced records update when you open the app. You do not need to reimport them. Recorded prices and corrections stay unchanged.

This covers most pairs across the earlier network, but it is not complete historical coverage. Journeys that might have changed with CCL6, the three stations that had not opened, and eight calculator results that differ from the general fare bands are excluded. Those records remain **Fare needed**. Dates before 1 March 2025 or after 25 December 2026 also need checked fares.

## Saving a route fare

To override an automatic amount or cover another date, check the [official LTA calculator](https://www.lta.gov.sg/content/ltagov/en/map/fare-calculator.html). Choose the adult card fare and treat the recorded journey independently.

In **More → Saved route fares**, enter its origin, destination, amount and the fare period in which it applies. A saved fare is directional. Save the return direction separately if needed.

Before a change is applied, the app shows the number of journeys that will be reassessed. A corrected fare on a particular trip continues to take precedence. **Remove saved fare** restores the automatic amount where available and keeps individual corrections.

An unknown fare never becomes zero. Monetary totals and trip targets say how many records are priced. Counts and map connections still include unpriced journeys.

## Why not calculate from the map?

Geographic coordinates do not represent official fare distances. The app does not multiply straight-line distances or count stations to invent a fare.

The app uses complete journey results from LTA's calculator. Adding rounded adjacent-station distances can change the resulting fare, so that approach is not used. Eight returned calculator prices differ from the general PTC distance band for their displayed distance. Independent reverse-journey checks reproduced those values; the app retains the calculator result. You can override a fare when you have a checked correction.

## Effective dates

Full automatic coverage begins on 12 July 2026, when CCL6 changed rail distances, and ends on 25 December 2026. Selective earlier coverage starts on 1 March 2025, as explained above. The editable default for a manually checked fare runs from 27 December 2025 through 25 December 2026. Check dates and rates against the official source. PTC has announced a new adjustment from 26 December 2026. Do not apply a rate to a date outside its verified period.

## Sources and attribution

- [SimplyGo undergraduate pass conditions](https://simplygo.com.sg/concession-monthly-travel-cards/students/undergraduate-concession-card/).
- [PTC MRT/LRT fare bands](https://data.gov.sg/datasets/d_2ca29f673b89e0cfddbb7c74516fa4d3/view).
- [PTC 2026 fare review](https://www.ptc.gov.sg/media-centre/newsroom/fare-review-exercise-2026/).
- [LTA DataMall station footprints and names](https://datamall.lta.gov.sg/content/datamall/en/static-data.html), September 2026 footprint snapshot.
- [SLA National Map Polygon](https://data.gov.sg/datasets/d_29f066d67df3eae91df8a42f443863c8/view), coastal outlines, simplified for display.
- [LTA Circle Line 6 announcement](https://www.lta.gov.sg/content/ltagov/en/newsroom/2026/5/news-releases/circle-line-stage-6-to-open-for-public-preview-on-4-july-2026.html), supplementing the older station-name file.

The fare table is included inside the APK, with no separate JSON download. Calculator results are attributed to LTA and are separate from the code and map-data licences. See the [source record](https://github.com/holsoma/farenough/blob/main/DATA_SOURCES.md).

The map datasets are adapted under the [Singapore Open Data Licence](https://datamall.lta.gov.sg/content/datamall/en/SingaporeOpenDataLicence.html). The repository records source URLs, checksums and transformations. farenough is independent of LTA and SimplyGo.
