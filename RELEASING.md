# Releasing fairenough

## Version and signing identity

Update `version.properties`: public `versionName` and a strictly increasing integer `versionCode`. Update `CHANGELOG.md`. Keep the same application ID and signing key for all normal releases. Losing the key prevents compatible future APK updates.

Release signing reads these environment variables:

- `FAIRENOUGH_KEYSTORE`: absolute path to the keystore.
- `FAIRENOUGH_STORE_PASSWORD`.
- `FAIRENOUGH_KEY_ALIAS`: defaults to `fairenough`.
- `FAIRENOUGH_KEY_PASSWORD`.

Never commit keys or passwords. Keep an encrypted offline backup of the signing identity. The repository's first-release signing certificate fingerprint must be recorded once the signed candidate has been verified.

## Candidate checks

1. Run core tests, Android lint, device tests and the production site build.
2. Install the preceding signed candidate, create records, then install the new signed APK over it. Verify passes, trips, fares, corrections, preferences and backup export.
3. Capture the actual app with demonstration records. Review light/dark, large text and all primary screens.
4. Check APK signatures using `apksigner verify --verbose --print-certs`.
5. Produce `SHA256SUMS.txt` and a release note listing Android requirements, fare-data coverage and format versions.

## GitHub

Repository secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD` and `RELEASE_KEY_PASSWORD` feed the release workflow. A `v<versionName>` tag runs checks, signs one APK and creates a **draft** release. Review the candidate before making it public.

The Pages workflow builds static metadata from a published release. Until one exists, the download page says the first public release is being prepared. Do not claim a working public APK link before publication.

## Verify a download

```sh
sha256sum -c SHA256SUMS.txt
apksigner verify --print-certs fairenough-v0.1.0.apk
```

On Windows use `Get-FileHash -Algorithm SHA256` and compare the output to `SHA256SUMS.txt`. A checksum verifies file integrity. Compare the signing certificate separately with the published signing identity.
