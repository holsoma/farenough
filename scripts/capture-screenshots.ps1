$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'local-env.ps1')
$projectRoot = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
Push-Location $projectRoot
try {
    & '.\gradlew.bat' :app:assembleDebug :app:assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw 'Build failed.' }
    & $adb install -r 'app/build/outputs/apk/debug/app-debug.apk'
    & $adb install -r 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    & $adb shell am instrument -w -r -e class 'io.github.holsoma.fairenough.AppJourneyTest#seededAppNavigationAndScreenshots' io.github.holsoma.fairenough.debug.test/androidx.test.runner.AndroidJUnitRunner
    & $adb pull '/sdcard/Android/data/io.github.holsoma.fairenough.debug/files/screenshots/.' 'site/public/screenshots/'
    if ($LASTEXITCODE -ne 0) { throw 'Screenshot export failed.' }
} finally { Pop-Location }
