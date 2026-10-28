param([Parameter(Mandatory=$true)][string]$Destination)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'local-env.ps1')
$projectRoot = Split-Path -Parent $PSScriptRoot
$source = Join-Path $projectRoot '.secrets\fairenough.p12'
$passwordFile = Join-Path $projectRoot '.secrets\store-password.dpapi'
if (-not (Test-Path -LiteralPath $source)) { throw 'No release key exists yet.' }
$targetDirectory = (Resolve-Path -LiteralPath $Destination).Path
$target = Join-Path $targetDirectory 'farenough-signing-backup.p12'
if (Test-Path -LiteralPath $target) { throw 'The destination already contains a signing backup. Choose a different folder.' }
$recovery = Read-Host 'Choose a recovery password and store it in your password manager' -AsSecureString
$original = Get-Content -LiteralPath $passwordFile | ConvertTo-SecureString
$env:FARENOUGH_ORIGINAL = [System.Net.NetworkCredential]::new('', $original).Password
$env:FARENOUGH_RECOVERY = [System.Net.NetworkCredential]::new('', $recovery).Password
try {
    if ($env:FARENOUGH_RECOVERY.Length -lt 12) { throw 'Use a recovery password of at least 12 characters.' }
    & (Join-Path $env:JAVA_HOME 'bin\keytool.exe') -importkeystore -srckeystore $source -srcstoretype PKCS12 -srcstorepass:env FARENOUGH_ORIGINAL -destkeystore $target -deststoretype PKCS12 -deststorepass:env FARENOUGH_RECOVERY -destkeypass:env FARENOUGH_RECOVERY -srcalias fairenough -destalias fairenough -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'Signing backup export failed.' }
    Write-Output "Encrypted portable signing backup created: $target"
} finally { Remove-Item Env:FARENOUGH_ORIGINAL, Env:FARENOUGH_RECOVERY -ErrorAction SilentlyContinue }
