function Assert-SigningIdentity {
    param(
        [Parameter(Mandatory)][string[]]$Verification,
        [Parameter(Mandatory)][string]$IdentityFile
    )
    $expected = @(Get-Content -LiteralPath $IdentityFile | Where-Object { $_ -cmatch '^[0-9a-f]{64}$' })
    $actual = @($Verification | Where-Object { $_ -match '^Signer #[0-9]+ certificate SHA-256 digest: ' } | ForEach-Object { ($_ -split ': ', 2)[1].Trim().ToLowerInvariant() })
    if ($expected.Count -ne 1 -or $actual.Count -ne 1 -or $actual[0] -cne $expected[0]) {
        throw 'The APK certificate does not match SIGNING.md. Restore the recorded signing key before releasing.'
    }
}
