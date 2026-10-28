"""Rebuild offline assets from downloaded, attributed Singapore open datasets.

pip install pyshp pyproj shapely xlrd
python scripts/prepare_map.py --source-dir .tooling/sources
No network calls; the source manifest records exactly which files were used.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import zipfile

import shapefile
import xlrd
from pyproj import CRS, Transformer
from shapely.geometry import shape

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--source-dir', type=Path, required=True)
args = parser.parse_args()
source = args.source_dir

with zipfile.ZipFile(source / 'stations.zip') as archive:
    def part(ext):
        return io.BytesIO(archive.read(next(n for n in archive.namelist() if n.endswith(ext))))
    projection = archive.read(next(n for n in archive.namelist() if n.endswith('.prj'))).decode()
    transform = Transformer.from_crs(CRS.from_wkt(projection), 'EPSG:4326', always_xy=True)
    reader = shapefile.Reader(shp=part('.shp'), shx=part('.shx'), dbf=part('.dbf'))
    positions = {}
    for record in reader.iterShapeRecords():
        code = record.record.as_dict()['STN_NAM'].strip().upper()
        point = shape(record.shape.__geo_interface__).representative_point()
        lon, lat = transform.transform(point.x, point.y)
        for individual in re.findall(r'[A-Z]+\d*', code):
            positions[individual] = (lon, lat)

with zipfile.ZipFile(source / 'station-codes.zip') as archive:
    workbook = xlrd.open_workbook(file_contents=archive.read(next(n for n in archive.namelist() if n.endswith('.xls'))))
    sheet = workbook.sheet_by_index(0)
    by_name = {}
    for i in range(1, sheet.nrows):
        code, name = str(sheet.cell_value(i, 0)).strip(), str(sheet.cell_value(i, 1)).strip()
        if not code or code == 'BP14':  # Ten Mile Junction closed in 2019.
            continue
        by_name.setdefault(name, []).append(code)

# The station-name XLS predates CCL6; supplement only documented official codes.
# https://www.lta.gov.sg/content/ltagov/en/newsroom/2026/5/news-releases/circle-line-stage-6-to-open-for-public-preview-on-4-july-2026.html
by_name.setdefault('Keppel', []).append('CC30')
by_name.setdefault('Cantonment', []).append('CC31')
by_name.setdefault('Prince Edward Road', []).append('CC32')
# Current code aliases from the official SimplyGo catalogue; retain historic CE codes.
by_name['Marina Bay'].append('CC33')
by_name['Bayfront'].append('CC34')

stations = []
missing = []
registry_path = ROOT / 'data/station-ids.json'
registry = json.loads(registry_path.read_text(encoding='utf-8'))
for name, codes in sorted(by_name.items()):
    codes = sorted(set(codes), key=lambda v: (re.sub(r'\d+', '', v), int(re.search(r'\d+', v).group()) if re.search(r'\d+', v) else 0))
    known = [positions[c] for c in codes if c in positions]
    location = known[0] if known else None
    if not known:
        missing.append({'name': name, 'codes': codes})
    # New line codes must not change the IDs used in existing journeys/backups.
    if name not in registry:
        raise ValueError(f'Add a permanent ID for {name} to data/station-ids.json before rebuilding')
    stations.append({'id': registry[name], 'name': name, 'codes': codes, 'latitude': round(location[1], 6) if location else None, 'longitude': round(location[0], 6) if location else None})

geo = json.loads((source / 'outline.geojson').read_text(encoding='utf-8-sig'))
rings = []
for feature in geo['features']:
    if 'Coastal_Outlines' not in feature['properties'].get('FOLDERPATH', ''):
        continue
    polygon = shape(feature['geometry']).simplify(.00015, preserve_topology=True)
    for p in ([polygon] if polygon.geom_type == 'Polygon' else polygon.geoms):
        if p.area > 0.000001:
            rings.append([[round(x, 6), round(y, 6)] for x, y, *_ in p.exterior.coords])
assert len(stations) > 150, 'Unexpectedly small station catalogue'
assert rings, 'No coastal outlines found'
assert len({s['id'] for s in stations}) == len(stations), 'Duplicate canonical IDs'
assets = ROOT / 'app/src/main/assets'
assets.mkdir(parents=True, exist_ok=True)
(assets / 'stations.json').write_text(json.dumps(stations, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
(assets / 'coastline.json').write_text(json.dumps(rings, separators=(',', ':')) + '\n', encoding='utf-8')
sources = {
    'retrieved': '2026-10-28',
    'licence': 'https://datamall.lta.gov.sg/content/datamall/en/SingaporeOpenDataLicence.html',
    'files': [
        {'file': filename, 'source': url, 'sha256': hashlib.sha256((source / filename).read_bytes()).hexdigest()}
        for filename, url in [
            ('stations.zip', 'https://datamall.lta.gov.sg/content/dam/datamall/datasets/Geospatial/TrainStation_Sep2026.zip'),
            ('station-codes.zip', 'https://datamall.lta.gov.sg/content/dam/datamall/datasets/Geospatial/Train%20Station%20Codes%20and%20Chinese%20Names.zip'),
            ('outline.geojson', 'https://data.gov.sg/datasets/d_29f066d67df3eae91df8a42f443863c8/view'),
        ]
    ],
    'stationCount': len(stations), 'unmappedStations': missing,
    'catalogueSupplement': ['https://www.lta.gov.sg/content/ltagov/en/newsroom/2026/5/news-releases/circle-line-stage-6-to-open-for-public-preview-on-4-july-2026.html', 'https://travelguide.simplygo.com.sg/MRT'],
    'transformation': 'Station footprint representative points converted from the supplied SVY21 projection to WGS84. Station codes grouped by exact official English name. Closed BP14 excluded. Coastal polygons simplified to 0.00015 degrees, exterior rings only, tiny polygons below 0.000001 square degrees excluded. No fare distances inferred.'
}
(ROOT / 'data').mkdir(exist_ok=True)
(ROOT / 'data/sources.json').write_text(json.dumps(sources, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'stations': len(stations), 'coastalRings': len(rings), 'unmapped': missing}, indent=2))
