<div align="center">

<a href="https://holsoma.github.io/farenough/">
  <img src="site/public/farenough.svg" width="88" height="88" alt="farenough logo" />
</a>

# farenough

### Has your train pass paid for itself?

Record your Singapore MRT and LRT journeys. Compare their fares with your concession pass.

[![Release downloads](https://img.shields.io/github/downloads/holsoma/farenough/total?style=flat&label=Release%20downloads&labelColor=18242b&color=006c67)](https://github.com/holsoma/farenough/releases)
[![Build checks](https://img.shields.io/github/actions/workflow/status/holsoma/farenough/checks.yml?branch=main&style=flat&label=Checks&labelColor=18242b)](https://github.com/holsoma/farenough/actions/workflows/checks.yml)
[![Android 8.0 or later](https://img.shields.io/badge/Android-8.0%2B-006c67?style=flat&labelColor=18242b&logo=android&logoColor=white)](https://holsoma.github.io/farenough/docs/getting-started/)
[![Apache 2.0 licence](https://img.shields.io/github/license/holsoma/farenough?style=flat&label=Licence&labelColor=18242b&color=52616b)](LICENSE)

## Download

[![Download APK](https://img.shields.io/badge/Download-APK-006c67?style=for-the-badge&labelColor=004d49&logo=android&logoColor=white)](https://holsoma.github.io/farenough/download/)
[![Latest stable build](https://img.shields.io/github/v/release/holsoma/farenough?style=for-the-badge&label=Stable%20build&labelColor=18242b&color=006c67)](https://github.com/holsoma/farenough/releases/latest)

**Android 8.0+ · Free and open source · No account or tracking**

[Website](https://holsoma.github.io/farenough/) · [Documentation](https://holsoma.github.io/farenough/docs/) · [Changelog](https://holsoma.github.io/farenough/changelog/) · [Report an issue](https://github.com/holsoma/farenough/issues)

</div>

## A look inside

<p align="center">
  <a href="site/public/screenshots/overview.png"><img src="site/public/screenshots/overview.png" width="220" alt="Overview: pass cost, recorded fares and estimated savings" /></a>
  <a href="site/public/screenshots/trips.png"><img src="site/public/screenshots/trips.png" width="220" alt="Journeys: station pairs and fares grouped by date" /></a>
  <a href="site/public/screenshots/map.png"><img src="site/public/screenshots/map.png" width="220" alt="Insights: monthly journey connections on a Singapore map" /></a>
</p>

<p align="center"><sub>Actual app screens with invented records and example fares.</sub></p>

## Features

| | What you can do |
| --- | --- |
| **Pass comparison** | Compare recorded adult fares with the price and dates of your undergraduate train pass. |
| **Quick journey entry** | Choose stations, save favourite routes and correct individual fares. |
| **Trip targets** | See how many more journeys would cover your pass, using an average fare or a selected route. |
| **Charts and maps** | Explore daily totals, your most travelled routes and monthly connections across Singapore. |
| **Import your old notes** | Copy a formatting prompt into an AI chat tool, then review and import its normalised JSON. |
| **Your data, on your device** | Use the app offline. Export full backups or CSV files, and restore your records when needed. |

> [!NOTE]
> **Fare coverage:** fares come from saved, LTA-checked routes with explicit valid dates. There is no bundled automatic fare matrix. Missing fares are never counted as zero. Automatic transfers, time discounts and bus fares are outside this release.

## Documentation

| Guide | Start here to… |
| --- | --- |
| [Getting started](https://holsoma.github.io/farenough/docs/getting-started/) | Install the app and set up your pass. |
| [Recording journeys](https://holsoma.github.io/farenough/docs/recording-trips/) | Add trips, favourite routes and fare corrections. |
| [Importing your records](https://holsoma.github.io/farenough/docs/importing/) | Bring in notes you have kept elsewhere. |
| [Understanding the fares](https://holsoma.github.io/farenough/docs/fare-calculations/) | See what counts towards the comparison. |
| [Targets, charts and maps](https://holsoma.github.io/farenough/docs/targets-and-insights/) | Read the trip targets and explore your travel history. |
| [Backups and updates](https://holsoma.github.io/farenough/docs/backups-and-updates/) | Keep your records when updating or changing devices. |

## Install and update

Download the signed APK attached to a GitHub release and open it on your phone. Android may ask you to allow installation from that browser or file manager. To update, install the new APK over the old app. **Do not uninstall first:** uninstalling removes local records. Direct APK installs do not update automatically.

Each release includes a SHA-256 checksum and signing-certificate details. See [SIGNING.md](SIGNING.md) to verify the app's identity, or [RELEASING.md](RELEASING.md) for the release process. Debug builds use a separate application ID.

## Build

Install JDK 21, Android SDK API 36 and Build Tools 36.0.0. Configure `JAVA_HOME` and `ANDROID_HOME` or the SDK location in Android Studio.

```sh
./gradlew :core:test :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

On Windows use `gradlew.bat`. The optional `scripts/local-env.ps1` uses project-local tools if they exist, without changing system settings.

```sh
cd site
npm ci
npm run build
npm run dev
```

The website requires Node.js 24, or at least 22.23.4, and serves at `/farenough/`.

## Data and privacy

The app has no internet permission. Data lives in its private Room database. Android cloud backup and automatic device transfer are excluded; use explicit backup/restore. Exported files are unencrypted and go to the location you choose. The external LLM workflow only shares text when you paste it into that external service.

Map sources, transformations and checksums are listed in [data/sources.json](data/sources.json) and [DATA_SOURCES.md](DATA_SOURCES.md). The map shows endpoints, not actual rail paths. No geographic distances are used to calculate fares.

## Contribute

Read [CONTRIBUTING.md](CONTRIBUTING.md). Use synthetic examples in public issues, rather than private travel records. Implementation details and verified checks are recorded in [IMPLEMENTATION.md](IMPLEMENTATION.md). Screenshots are captured by [the Android UI test](app/src/androidTest/java/io/github/holsoma/fairenough/AppJourneyTest.kt).

Code: [Apache 2.0](LICENSE). Data and font licences are documented separately in [NOTICE.md](NOTICE.md). This project is not affiliated with LTA, SimplyGo or any transport operator.
