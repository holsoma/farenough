"""Restore the ignored fare asset from a checksum-pinned, published APK."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def restore(manifest, apk, destination):
    if sha256(apk) != manifest['apkSha256']:
        raise ValueError('APK checksum differs from data/rail-fares-source.json.')
    with zipfile.ZipFile(io.BytesIO(apk)) as archive:
        info = archive.getinfo(manifest['assetPath'])
        if info.file_size > 5_000_000:
            raise ValueError('Unexpectedly large fare asset.')
        asset = archive.read(info)
    if sha256(asset) != manifest['assetSha256']:
        raise ValueError('Fare asset checksum differs from data/rail-fares-source.json.')
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix('.tmp')
    temporary.write_bytes(asset)
    temporary.replace(destination)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, help='Use an already downloaded APK instead of fetching the pinned release.')
    args = parser.parse_args()
    manifest = json.loads((ROOT / 'data/rail-fares-source.json').read_text(encoding='utf-8'))
    destination = ROOT / 'app/src/main/assets/rail-fares.json'
    if not args.apk and destination.exists() and sha256(destination.read_bytes()) == manifest['assetSha256']:
        print(f"Fare asset already verified: {manifest['datasetId']}")
        return
    if args.apk:
        apk = args.apk.read_bytes()
    else:
        request = urllib.request.Request(manifest['apkUrl'], headers={'User-Agent': 'farenough-build'})
        with urllib.request.urlopen(request, timeout=60) as response:
            apk = response.read(20_000_001)
        if len(apk) > 20_000_000:
            raise ValueError('Unexpectedly large APK download.')
    restore(manifest, apk, destination)
    print(f"Restored verified fare asset: {manifest['datasetId']}")


if __name__ == '__main__':
    main()
