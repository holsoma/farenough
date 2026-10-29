# Data sources

## Offline map and stations

The app includes 184 canonical MRT/LRT stations, with all endpoint locations available. The source files were retrieved on 28 October 2026.

- **LTA DataMall Train Station, September 2026:** station footprints. A representative point inside each footprint is converted from the supplied SVY21 projection to WGS84.
- **LTA Train Station Codes and Chinese Names:** official English names and station codes, grouped by exact English name. BP14, Ten Mile Junction, is excluded because it closed in 2019.
- **LTA Circle Line 6 announcement and the official SimplyGo catalogue:** three new CCL6 names and current CC33/CC34 aliases supplement the older names file. Historic CE1/CE2 codes remain recognised.
- **SLA National Map Polygon:** coastal outlines simplified at 0.00015 degrees for a whole-island view. Only exterior rings are included; very small polygons are omitted.

The original URLs and source checksums are in [sources.json](data/sources.json). Run `scripts/prepare_map.py --source-dir <downloads>` with Python, pyshp, pyproj, shapely and xlrd to reproduce the transformation. The expected source filenames are `stations.zip`, `station-codes.zip` and `outline.geojson`.

The datasets are adapted under the [Singapore Open Data Licence](https://datamall.lta.gov.sg/content/datamall/en/SingaporeOpenDataLicence.html). Attribute LTA and SLA when redistributing these assets. The app code's Apache licence does not replace their licence.

## Fare investigation

[PTC's MRT/LRT fare-band dataset](https://data.gov.sg/datasets/d_2ca29f673b89e0cfddbb7c74516fa4d3/view) is openly available. Station coordinates do not supply the authoritative fare distances needed to apply it.

The original [singapore-gtfs project](https://github.com/Vorld/singapore-gtfs) documents a public SimplyGo fare form and archives 1,691 HTML responses. A response inspected during this implementation contains an adult card fare, for example Yio Chu Kang to Ang Mo Kio at S$1.28. These archives do not establish complete current coverage, fare distances or an independent redistribution grant from SimplyGo. They are kept only in ignored research downloads and are not bundled in the app.

Consequently, version 0.1.0 uses the agreed **saved route fare** fallback. Users check regular adult fares with [LTA](https://www.lta.gov.sg/content/ltagov/en/map/fare-calculator.html) and save their applicable dates. There is no hidden approximate distance engine and no runtime scraping.

Automatic data must record its provenance, effective fare/network dates, full station-pair coverage and official comparison fixtures. Data licences are separate from the code licence. Never substitute geographic distance or number of stations for fare distance.

## Automatic fares in 0.2.0

Version 0.2.0 uses complete journey responses from the LTA calculator's adult MRT/LRT form. Its catalogue maps to all 184 canonical app stations. The matrix contains one entry for each of the 16,836 station pairs, with the returned fare in cents and distance converted from hundredths of a kilometre to metres. Each query starts a separate journey with zero accumulated distance and fare. The app makes no network calls.

Coverage is restricted to **12 July through 25 December 2026**, using results retrieved on **29 October 2026**. CCL6 opened on 12 July and changes shortest rail distances. The [next fare adjustment](https://www.ptc.gov.sg/media-centre/newsroom/fare-review-exercise-2026/) takes effect on 26 December. The app does not apply this network and fare period to older records. See [PTC's rail fare explanation](https://ask.gov.sg/ptc/questions/cmq0md92s00785l017w6x8nog) and [LTA's CCL6 project](https://www.lta.gov.sg/content/ltagov/en/upcoming_projects/rail_expansion/circle_line_6.html).

Adding rounded adjacent-station distances failed comparison against complete calculator journeys, so that approach is not used. The exact whole-journey distance and returned fare are retained independently. Eight calculator fares are four or six cents below the general PTC distance band for their displayed distance. An independent check of 193 reverse journeys, including all eight exceptions, returned identical distances and amounts. The app retains those observed calculator values; the validation report lists them explicitly.

Saved route fares override automatic amounts in their stated direction and date range. Individual corrections override both. Upgrading or restoring fills only missing regular fares, preserving already recorded prices, identifiers, corrections, notes, preferences and import history. Distance totals explicitly report how many journeys have coverage. They describe the distance used for charging, not a GPS trace or a reconstruction of the route taken.

The fare table is distributed inside the signed APK and is excluded from Git and separate release downloads. Raw responses remain local. The calculator results are attributed to LTA; no independent open-data licence for them has been verified. They are not covered by the code's Apache licence or the map assets' Singapore Open Data Licence. See [LTA's website terms](https://www.lta.gov.sg/content/ltagov/en/terms-of-use.html).

For a normal source build, run `python scripts/restore_rail_fares.py`. It downloads the release APK pinned in `data/rail-fares-source.json`, verifies its SHA-256, reads only `assets/rail-fares.json`, verifies that asset and writes it to the ignored app asset path. Use `--apk <path>` for an existing download. An already matching asset needs no download.

For maintainers with the collected SQLite cache, the original transformation is:

```sh
python scripts/prepare_rail_fares.py --cache .tooling/fare-research/matrix.sqlite --output app/src/main/assets/rail-fares.json --report artifacts/rail-fares-report.json --valid-from 2026-07-12 --valid-until 2026-12-25 --retrieved 2026-10-29
```

The exporter refuses incomplete coverage, unknown pairs, duplicate rows, invalid measurements, mismatched raw responses and inconsistent retrieval dates. Its report records the generated asset's SHA-256 checksum.
