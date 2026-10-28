param([int]$VersionCode = 0, [string]$OutputName = '')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'local-env.ps1')
$projectRoot = Split-Path -Parent $PSScriptRoot
$secretDirectory = Join-Path $projectRoot '.secrets'
$keyPath = Join-Path $secretDirectory 'fairenough.p12'
$passwordPath = Join-Path $secretDirectory 'store-password.dpapi'
New-Item -ItemType Directory -Force -Path $secretDirectory | Out-Null
if (-not (Test-Path -LiteralPath $keyPath)) {
    if (Test-Path -LiteralPath $passwordPath) { throw 'A password already exists without its keystore. Restore the key before continuing.' }
    $random = [byte[]]::new(32)
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($random)
    $secure = ConvertTo-SecureString ([Convert]::ToBase64String($random)) -AsPlainText -Force
    $secure | ConvertFrom-SecureString | Set-Content -LiteralPath $passwordPath
} else {
    if (-not (Test-Path -LiteralPath $passwordPath)) { throw 'The keystore password file is missing. Do not replace the signing identity.' }
    $secure = Get-Content -LiteralPath $passwordPath | ConvertTo-SecureString
}
$env:FAIRENOUGH_STORE_PASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password
$env:FAIRENOUGH_KEY_PASSWORD = $env:FAIRENOUGH_STORE_PASSWORD
$env:FAIRENOUGH_KEYSTORE = $keyPath
try {
    if (-not (Test-Path -LiteralPath $keyPath)) {
        & (Join-Path $env:JAVA_HOME 'bin\keytool.exe') -genkeypair -keystore $keyPath -storetype PKCS12 -storepass:env FAIRENOUGH_STORE_PASSWORD -keypass:env FAIRENOUGH_KEY_PASSWORD -alias fairenough -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=fairenough, OU=Open source'
        if ($LASTEXITCODE -ne 0) { throw 'Signing key creation failed.' }
    }
    $arguments = @(':app:assembleRelease', '--no-daemon')
    if ($VersionCode -gt 0) { $arguments += "-PreleaseCode=$VersionCode" }
    Push-Location $projectRoot
    try { & '.\gradlew.bat' @arguments; if ($LASTEXITCODE -ne 0) { throw 'Release build failed.' } } finally { Pop-Location }
    $version = ((Get-Content (Join-Path $projectRoot 'version.properties') | Where-Object { $_ -like 'versionName=*' }) -split '=',2)[1]
    if (-not $OutputName) { $OutputName = "fairenough-v$version.apk" }
    if ($OutputName -notmatch '^[A-Za-z0-9._-]+\.apk$') { throw 'OutputName must be a filename ending in .apk.' }
    $artifacts = Join-Path $projectRoot 'artifacts'
    New-Item -ItemType Directory -Force -Path $artifacts | Out-Null
    $apk = Join-Path $artifacts $OutputName
    Copy-Item -LiteralPath (Join-Path $projectRoot 'app\build\outputs\apk\release\app-release.apk') -Destination $apk
    $verification = & (Join-Path $env:ANDROID_HOME 'build-tools\36.0.0\apksigner.bat') verify --verbose --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    $verification | Set-Content -LiteralPath (Join-Path $artifacts 'signing-certificate.txt')
    $hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
    $checksumName = if ($OutputName -eq "fairenough-v$version.apk") { 'SHA256SUMS.txt' } else { "$OutputName.sha256" }
    "$hash  $OutputName" | Set-Content -LiteralPath (Join-Path $artifacts $checksumName)
    # Separate local encrypted-key copy. The DPAPI password remains tied to this Windows user.
    $backup = Join-Path $secretDirectory 'offline-backup'
    New-Item -ItemType Directory -Force -Path $backup | Out-Null
    Copy-Item -LiteralPath $keyPath -Destination (Join-Path $backup 'fairenough.p12')
    Copy-Item -LiteralPath $passwordPath -Destination (Join-Path $backup 'store-password.dpapi')
    Write-Output "Verified signed APK: $apk"
    $verification | Where-Object { $_ -like '*SHA-256 digest*' }
} finally {
    Remove-Item Env:FAIRENOUGH_STORE_PASSWORD, Env:FAIRENOUGH_KEY_PASSWORD, Env:FAIRENOUGH_KEYSTORE -ErrorAction SilentlyContinue
}
