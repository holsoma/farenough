$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'assert-signing-identity.ps1')
$identityFile = [IO.Path]::GetTempFileName()
$expected = 'a' * 64
function Expect-IdentityRejection([scriptblock]$Action) {
    try { & $Action } catch {
        if ($_.Exception.Message -notlike '*certificate does not match*') { throw }
        return
    }
    throw 'An invalid signing identity was accepted.'
}
try {
    Set-Content -LiteralPath $identityFile -Value $expected
    Assert-SigningIdentity -Verification @("Signer #1 certificate SHA-256 digest: $expected") -IdentityFile $identityFile
    Expect-IdentityRejection { Assert-SigningIdentity -Verification @("Signer #1 certificate SHA-256 digest: $('b' * 64)") -IdentityFile $identityFile }
    Expect-IdentityRejection { Assert-SigningIdentity -Verification @('No certificate digest') -IdentityFile $identityFile }
    Expect-IdentityRejection { Assert-SigningIdentity -Verification @("Signer #1 certificate SHA-256 digest: $expected", "Signer #2 certificate SHA-256 digest: $('b' * 64)") -IdentityFile $identityFile }
    Set-Content -LiteralPath $identityFile -Value @($expected, $expected)
    Expect-IdentityRejection { Assert-SigningIdentity -Verification @("Signer #1 certificate SHA-256 digest: $expected") -IdentityFile $identityFile }
    Write-Output 'PASS: recorded identity accepted; wrong, missing, multiple and ambiguous identities rejected.'
} finally { Remove-Item -LiteralPath $identityFile }
