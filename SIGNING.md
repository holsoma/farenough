# APK signing identity

Release application ID: `io.github.holsoma.fairenough`

SHA-256 certificate fingerprint:

```text
02f8d35a489f32dddb4a3fdb97b79e8196fddbb67fe713e84d66a4d6853d9065
```

Verify an APK using Android SDK Build Tools:

```sh
apksigner verify --verbose --print-certs fairenough-v0.1.0.apk
```

Compare the certificate digest with the value above. The file checksum in each release checks file integrity separately.

The local release key is excluded from Git. Its local password copy is protected for the current Windows user with DPAPI. A separate local encrypted-key copy is retained. For recovery on a different machine, use `scripts/export-signing-backup.ps1 -Destination <existing-offline-folder>` to create a portable encrypted backup with a recovery password you choose and keep separately.
