# Releasing farenough

## Research behind the release structure

Reviewed on 28 October 2026:

- [Mihon's release history](https://github.com/mihonapp/mihon/releases) uses named version releases, readable change categories and an explicit recommendation for the APK to choose. Its [repository](https://github.com/mihonapp/mihon) links to a separate FAQ, changelog and contribution guide. farenough follows that separation between installation, usage documentation and development, with one APK to avoid a choice of builds.
- [AntennaPod's releases](https://github.com/AntennaPod/AntennaPod/releases) describe concrete behaviour changes, including fixes to playback and statistics. farenough release notes should explain changes to journeys, fare calculations, imports and backups in similarly specific terms.
- [Fossify Gallery's releases](https://github.com/FossifyOrg/Gallery/releases) group changes and link to the full comparison between versions. farenough keeps a versioned changelog and attaches installation files to releases.

These are organisational references. farenough's code and visual assets were created for this project. Its additional release requirements are a stable signing identity, a file checksum, explicit fare-data coverage and an update test that preserves existing records.

## Version and signing identity

Update `version.properties`: public `versionName` and a strictly increasing integer `versionCode`. Update `CHANGELOG.md`. Keep the same application ID and signing key for all normal releases. Losing the key prevents compatible future APK updates.

Release signing reads these environment variables:

- `FARENOUGH_KEYSTORE`: absolute path to the keystore.
- `FARENOUGH_STORE_PASSWORD`.
- `FARENOUGH_KEY_ALIAS`: defaults to `fairenough`, the original signing key alias.
- `FARENOUGH_KEY_PASSWORD`.

Never commit keys or passwords. Keep an encrypted offline backup of the signing identity. `SIGNING.md` records the release certificate. The local build script requires the existing key and verifies that certificate before replacing an APK in `artifacts/`. A missing key must be restored from backup.

## Candidate checks

1. Restore the ignored fare table with `python scripts/restore_rail_fares.py`, then run core tests, Android lint, device tests and the production site build. The helper verifies the APK and asset against `data/rail-fares-source.json`.
2. Install the preceding signed candidate, create records, then install the new signed APK over it. Verify passes, trips, fares, corrections, preferences and backup export.
3. Capture the actual app with demonstration records. Review light/dark, large text and all primary screens.
4. Check APK signatures using `apksigner verify --verbose --print-certs`.
5. Produce `SHA256SUMS.txt` and a release note listing Android requirements, fare-data coverage and format versions.

## GitHub

Repository secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD` and `RELEASE_KEY_PASSWORD` feed the release workflow. A `v<versionName>` tag runs checks, signs one APK and creates or refreshes a **draft** release. Review the candidate after the workflow finishes before making it public. The staging script leaves an already-published release unchanged and never publishes one itself.

Before any upload, `scripts/stage-release.sh` verifies the APK checksum and recorded signing identity. It lists releases to recognise an unpublished draft without a Git tag, and stops on API errors rather than treating them as a missing release. To inspect an existing candidate without modifying GitHub, set `GITHUB_REPOSITORY` and `GITHUB_REF_NAME`, then run `bash scripts/stage-release.sh artifacts --dry-run`.

Release guard tests run with `node --test scripts/stage-release.test.mjs` and `pwsh -NoProfile -File scripts/test-signing-identity.ps1`. They use synthetic packages and a replacement GitHub command; they do not upload files. On Windows, set `FARENOUGH_TEST_BASH` to Git Bash's `bash.exe` if it is not the default Bash.

The Pages workflow builds static metadata from a published release. It deploys from `main`, using the repository's existing environment permission. After publishing a release, dispatch the site build:

```sh
gh workflow run pages.yml --repo holsoma/farenough --ref main
```

Changes to website files on `main` also deploy automatically. Release tags do not deploy Pages directly. Until a public release exists, the download page says the first release is being prepared.

## Initial release of a new fare table

Keep the table and raw observations out of Git and separate downloads. Prepare and test the signed APK locally, then pin its URL and the APK/asset checksums in `data/rail-fares-source.json`. Push the source commit to a release branch, upload the already verified APK, checksum and certificate to a draft targeting that commit, verify the uploaded bytes, then publish the release. Fast-forward `main` to that commit so CI can restore the table from the now-public APK. This avoids a circular dependency on an unpublished data source. Subsequent releases can use the usual tag workflow while the pinned source APK is available.

Never replace a published APK to change its embedded data. Publish a new version and pin the new checksums.

## Verify a download

```sh
sha256sum -c SHA256SUMS.txt
apksigner verify --print-certs farenough-v0.1.0.apk
```

On Windows use `Get-FileHash -Algorithm SHA256` and compare the output to `SHA256SUMS.txt`. A checksum verifies file integrity. Compare the signing certificate separately with the published signing identity.
