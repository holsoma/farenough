"""Export a complete, locally collected LTA station-pair cache for the Android app.

This transforms factual observations; it does not grant redistribution rights.
The source cache is deliberately separate from the app and is never fetched by it.
"""
import argparse
import hashlib
import itertools
import json
import sqlite3
from datetime import date
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cache', type=Path, required=True)
    parser.add_argument('--stations', type=Path, default=Path('app/src/main/assets/stations.json'))
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--report', type=Path, required=True)
    parser.add_argument('--valid-from', required=True)
    parser.add_argument('--valid-until', required=True)
    parser.add_argument('--retrieved', required=True)
    args = parser.parse_args()
    start, end, retrieved = (date.fromisoformat(value) for value in (args.valid_from, args.valid_until, args.retrieved))
    if not start <= retrieved <= end:
        raise ValueError('The retrieval date must be within the stated coverage period.')
    stations = json.loads(args.stations.read_text(encoding='utf-8'))
    ids = sorted(station['id'] for station in stations)
    if len(ids) != len(set(ids)):
        raise ValueError('Duplicate canonical station IDs.')
    expected = set(itertools.combinations(ids, 2))
    with sqlite3.connect(args.cache.resolve().as_uri() + '?mode=ro', uri=True) as db:
        rows = db.execute('SELECT origin,destination,distance,fare,raw,retrieved FROM quotes ORDER BY origin,destination').fetchall()
    if {(row[0], row[1]) for row in rows} != expected or len(rows) != len(expected):
        raise ValueError(f'Incomplete or invalid matrix: {len(rows)} rows, {len(expected)} required.')
    pairs = []
    for origin, destination, distance, fare, raw, collected in rows:
        response = json.loads(raw)
        if type(distance) is not int or type(fare) is not int or not (0 < distance < 10000 and 0 < fare < 1000):
            raise ValueError(f'Invalid observation: {origin}, {destination}')
        if int(response['distance']) != distance or int(response['fare']) != fare or collected != args.retrieved:
            raise ValueError(f'Source response or collection date mismatch: {origin}, {destination}')
        pairs.append(dict(origin=origin, destination=destination, distanceMetres=distance * 10, cents=fare))
    data = dict(schemaVersion=1, id=f'lta-{retrieved}-adult', validFrom=str(start), validUntil=str(end),
                retrieved=str(retrieved), source='LTA calculator, adult fare', stationIds=ids, pairs=pairs)
    encoded = (json.dumps(data, ensure_ascii=False, separators=(',', ':')) + '\n').encode('utf-8')
    report = dict(datasetId=data['id'], stationCount=len(ids), pairCount=len(pairs),
                  sha256=hashlib.sha256(encoded).hexdigest(), bytes=len(encoded),
                  minimumMetres=min(pair['distanceMetres'] for pair in pairs),
                  maximumMetres=max(pair['distanceMetres'] for pair in pairs),
                  minimumCents=min(pair['cents'] for pair in pairs), maximumCents=max(pair['cents'] for pair in pairs),
                  source='https://www.lta.gov.sg/content/ltagov/en/map/fare-calculator.html',
                  fareType='Adult, independent rail journey',
                  validFrom=str(start), validUntil=str(end), retrieved=str(retrieved),
                  reuseStatus='Calculator results have no verified open redistribution licence.')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix('.tmp')
    temporary.write_bytes(encoded)
    temporary.replace(args.output)
    args.report.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
