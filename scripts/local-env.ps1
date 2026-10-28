# Optional helper for the project-local toolchain. Does not modify system settings.
$projectRoot = Split-Path -Parent $PSScriptRoot
$localJdk = Get-ChildItem (Join-Path $projectRoot '.tooling\jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
$localSdk = Join-Path $projectRoot '.tooling\android'
if (Test-Path -LiteralPath $localSdk) {
    $env:ANDROID_HOME = $localSdk
    $env:ANDROID_USER_HOME = Join-Path $projectRoot '.tooling\android-user'
    $env:ANDROID_AVD_HOME = Join-Path $projectRoot '.tooling\avd'
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.tooling\gradle-home'
}
