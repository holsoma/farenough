---
layout: ../../layouts/Docs.astro
title: Building and contributing
description: Run the Android tests, build the app and preview this website.
---

## Android

Install JDK 21 and the Android SDK with API 36 and Build Tools 36.0.0. Set `JAVA_HOME` and `ANDROID_HOME`, or configure the SDK in Android Studio. The repository includes the Gradle wrapper.

```sh
./gradlew :core:test :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

On Windows, use `gradlew.bat`. The debug app has a separate application ID, so it can coexist with a signed release.

The plain Kotlin core handles calculations, import validation and backup formats. The Android app provides Room storage, Compose screens and explicit file operations. The data-preparation script records the sources and transformations used for offline map assets.

## Website

Use Node.js 24 or Node.js 22.23.4 and later.

```sh
cd site
npm ci
npm run dev
npm run build
```

The site is built for the `/farenough/` GitHub Pages path. Test that path locally as well as direct documentation URLs.

## Report an issue

Describe the behaviour you expected, what happened, the app version and Android version. Use invented records when demonstrating an import problem. Do not include private backups in public issues.

For fare-data contributions, include the original source, effective dates, redistribution terms and comparisons with an official reference. A graph built from approximate geographic distances is not an acceptable automatic fare source.

Code is Apache 2.0. Map data and font licences remain separate and are documented in the repository.
