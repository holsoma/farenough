param([int]$VersionCode = 0, [string]$OutputName = '')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'local-env.ps1')
. (Join-Path $PSScriptRoot 'assert-signing-identity.ps1')
$projectRoot = Split-Path -Parent $PSScriptRoot
$secretDirectory = Join-Path $projectRoot '.secrets'
$keyPath = Join-Path $secretDirectory 'fairenough.p12'
$passwordPath = Join-Path $secretDirectory 'store-password.dpapi'
if (-not (Test-Path -LiteralPath $keyPath) -or -not (Test-Path -LiteralPath $passwordPath)) {
    throw 'The recorded signing key or password file is missing. Restore it from backup; do not create a replacement.'
}
$secure = Get-Content -LiteralPath $passwordPath | ConvertTo-SecureString
$env:FARENOUGH_STORE_PASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password
$env:FARENOUGH_KEY_PASSWORD = $env:FARENOUGH_STORE_PASSWORD
$env:FARENOUGH_KEYSTORE = $keyPath
try {
    $arguments = @(':app:assembleRelease', '--no-daemon')
    if ($VersionCode -gt 0) { $arguments += "-PreleaseCode=$VersionCode" }
    Push-Location $projectRoot
    try { & '.\gradlew.bat' @arguments; if ($LASTEXITCODE -ne 0) { throw 'Release build failed.' } } finally { Pop-Location }
    $version = ((Get-Content (Join-Path $projectRoot 'version.properties') | Where-Object { $_ -like 'versionName=*' }) -split '=',2)[1]
    if (-not $OutputName) { $OutputName = "farenough-v$version.apk" }
    if ($OutputName -notmatch '^[A-Za-z0-9._-]+\.apk$') { throw 'OutputName must be a filename ending in .apk.' }
    $artifacts = Join-Path $projectRoot 'artifacts'
    New-Item -ItemType Directory -Force -Path $artifacts | Out-Null
    $apk = Join-Path $artifacts $OutputName
    $builtApk = Join-Path $projectRoot 'app\build\outputs\apk\release\app-release.apk'
    $verification = & (Join-Path $env:ANDROID_HOME 'build-tools\36.0.0\apksigner.bat') verify --verbose --print-certs $builtApk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    Assert-SigningIdentity -Verification $verification -IdentityFile (Join-Path $projectRoot 'SIGNING.md')
    Copy-Item -LiteralPath $builtApk -Destination $apk
    $verification | Set-Content -LiteralPath (Join-Path $artifacts 'signing-certificate.txt')
    $hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
    $checksumName = if ($OutputName -eq "farenough-v$version.apk") { 'SHA256SUMS.txt' } else { "$OutputName.sha256" }
    [IO.File]::WriteAllText((Join-Path $artifacts $checksumName), "$hash  $OutputName`n", [Text.UTF8Encoding]::new($false))
    # Separate local encrypted-key copy. The DPAPI password remains tied to this Windows user.
    $backup = Join-Path $secretDirectory 'offline-backup'
    New-Item -ItemType Directory -Force -Path $backup | Out-Null
    Copy-Item -LiteralPath $keyPath -Destination (Join-Path $backup 'fairenough.p12')
    Copy-Item -LiteralPath $passwordPath -Destination (Join-Path $backup 'store-password.dpapi')
    Write-Output "Verified signed APK: $apk"
    $verification | Where-Object { $_ -like '*SHA-256 digest*' }
} finally {
    Remove-Item Env:FARENOUGH_STORE_PASSWORD, Env:FARENOUGH_KEY_PASSWORD, Env:FARENOUGH_KEYSTORE -ErrorAction SilentlyContinue
}
