import io
import tempfile
import unittest
import zipfile
from pathlib import Path
from restore_rail_fares import restore, sha256


class RestoreRailFaresTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix='farenough-restore-')
        def cleanup():
            root = Path(self.directory.name).resolve()
            self.assertEqual(Path(tempfile.gettempdir()).resolve(), root.parent)
            self.assertTrue(root.name.startswith('farenough-restore-'))
            self.directory.cleanup()
        self.addCleanup(cleanup)
        self.destination = Path(self.directory.name) / 'rail-fares.json'
        self.destination.write_bytes(b'existing asset')
        self.asset = b'{"synthetic":"test data"}'
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, 'w') as archive:
            archive.writestr('assets/rail-fares.json', self.asset)
            archive.writestr('../unexpected.txt', b'never extract other entries')
        self.apk = buffer.getvalue()
        self.manifest = dict(apkSha256=sha256(self.apk), assetPath='assets/rail-fares.json', assetSha256=sha256(self.asset))

    def test_only_the_verified_asset_is_written(self):
        restore(self.manifest, self.apk, self.destination)
        self.assertEqual(self.asset, self.destination.read_bytes())
        self.assertEqual([self.destination], list(Path(self.directory.name).iterdir()))

    def test_changed_apk_preserves_existing_file(self):
        with self.assertRaisesRegex(ValueError, 'APK checksum'):
            restore(self.manifest, self.apk + b'changed', self.destination)
        self.assertEqual(b'existing asset', self.destination.read_bytes())

    def test_wrong_asset_hash_preserves_existing_file(self):
        self.manifest['assetSha256'] = '0' * 64
        with self.assertRaisesRegex(ValueError, 'Fare asset checksum'):
            restore(self.manifest, self.apk, self.destination)
        self.assertEqual(b'existing asset', self.destination.read_bytes())

    def test_missing_asset_preserves_existing_file(self):
        self.manifest['assetPath'] = 'missing.json'
        with self.assertRaises(KeyError):
            restore(self.manifest, self.apk, self.destination)
        self.assertEqual(b'existing asset', self.destination.read_bytes())


if __name__ == '__main__':
    unittest.main()
