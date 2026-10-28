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

Before adding a bundled matrix, require source provenance, reuse rights, effective fare/network dates, full station-pair coverage and official comparison fixtures. Never substitute geographic distance or number of stations for fare distance.
