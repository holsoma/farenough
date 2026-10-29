"""Capture the published APK with synthetic records on an isolated CI emulator."""
import hashlib
import json
import os
from pathlib import Path
import re
import sqlite3
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "io.github.holsoma.fairenough"
OUT = ROOT / ".shots/release-ui"


def adb(*args, binary=False):
    result = subprocess.run(["adb", "-s", "emulator-5554", *args], check=True, capture_output=True)
    return result.stdout if binary else result.stdout.decode("utf-8", errors="replace").strip()


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/qa-window.xml")
    return list(ET.fromstring(adb("shell", "cat", "/sdcard/qa-window.xml")).iter("node"))


def tap(text):
    matches = [n for n in nodes() if text in (n.get("text"), n.get("content-desc"))]
    if len(matches) != 1:
        raise RuntimeError(f"Expected one target {text!r}, found {len(matches)}")
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", matches[0].get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(0.7)


def swipe(up=True):
    adb("shell", "input", "swipe", "360", "1250" if up else "450", "360", "450" if up else "1250", "300")
    time.sleep(0.4)


def top():
    for _ in range(4):
        swipe(False)


def find(text):
    for _ in range(6):
        if any(text in (n.get("text"), n.get("content-desc")) for n in nodes()):
            return
        swipe()
    raise RuntimeError(f"Could not reveal {text}")


def shot(name):
    time.sleep(0.5)
    OUT.joinpath(name + ".png").write_bytes(adb("exec-out", "screencap", "-p", binary=True))
    print(name, flush=True)


def seed_fixture():
    # Never overwrite data on a local device. This script is only for fresh CI emulators.
    if os.environ.get("GITHUB_ACTIONS") != "true" or adb("shell", "getprop", "ro.kernel.qemu") != "1":
        raise RuntimeError("Requires an isolated GitHub Actions emulator")
    schema = json.loads(ROOT.joinpath("app/schemas/io.github.holsoma.fairenough.data.LocalDatabase/1.json").read_text())["database"]
    stations = json.loads(ROOT.joinpath("app/src/main/assets/stations.json").read_text())
    ids = {code: s["id"] for s in stations for code in [s["id"], *s["codes"]]}
    fixture = ROOT / ".tooling/release-ui.db"
    fixture.parent.mkdir(exist_ok=True)
    with sqlite3.connect(fixture) as db:
        db.execute(schema["entities"][0]["createSql"].replace("${TABLE_NAME}", "records"))
        for sql in schema["setupQueries"]:
            db.execute(sql)
        db.execute("PRAGMA user_version=1")
        rows = [("pass", "demo", {"id": "demo", "start": "2026-10-01", "end": "2026-10-31", "priceCents": 4800}),
                ("preferences", "main", {"appearance": "light", "favourites": []})]
        pairs = [("NS19", "EW14"), ("EW14", "NS19"), ("NS1", "DT32"), ("DT32", "NS1"), ("NS9", "CC33"), ("CC33", "NS9")]
        for month, count in [("2026-08", 22), ("2026-09", 32), ("2026-10", 26)]:
            for i in range(count):
                a, b = pairs[i % len(pairs)]
                key = f"demo-{month}-{i}"
                rows.append(("trip", key, {"id": key, "date": f"{month}-{i//2+1:02}", "origin": ids[a], "destination": ids[b],
                            "fareCents": 220, "fareSource": "Example fare", "note": "Demonstration record"}))
        db.executemany("INSERT OR REPLACE INTO records VALUES(?,?,?)", [(kind, key, json.dumps(value)) for kind, key, value in rows])
    adb("root")
    adb("wait-for-device")
    adb("shell", "am", "start", "-n", f"{PACKAGE}/{PACKAGE}.MainActivity")
    time.sleep(3)
    adb("shell", "am", "force-stop", PACKAGE)
    directory = f"/data/data/{PACKAGE}/databases"
    owner = adb("shell", "stat", "-c", "%u:%g", f"/data/data/{PACKAGE}")
    adb("shell", "mkdir", "-p", directory)
    adb("shell", "rm", "-f", directory+"/fairenough.db-wal", directory+"/fairenough.db-shm")
    adb("push", str(fixture), directory+"/fairenough.db")
    adb("shell", "chown", owner, directory+"/fairenough.db")
    adb("shell", "restorecon", "-RF", directory)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    apk, = ROOT.joinpath(".release/screenshots").glob("*.apk")
    adb("install", "-r", str(apk))
    adb("shell", "wm", "size", "720x1600")
    adb("shell", "wm", "density", "320")
    seed_fixture()
    adb("shell", "am", "start", "-n", f"{PACKAGE}/{PACKAGE}.MainActivity")
    time.sleep(3)
    shot("overview")
    tap("View pass result"); shot("target"); tap("Back")
    tap("Insights"); shot("charts"); tap("Map"); shot("map")
    tap("Routes"); find("Toa Payoh → Raffles Place"); tap("Toa Payoh → Raffles Place"); top(); tap("Map"); shot("map-selected")
    tap("Compare"); shot("month-comparison"); find("Month by month"); shot("month-history"); top()
    tap("Trips"); shot("trips"); tap("Import"); shot("import"); tap("Back")
    tap("Record a journey"); shot("add-trip"); tap("Back")
    tap("More"); find("Dark"); tap("Dark"); tap("Overview"); shot("overview-dark")
    tap("Insights"); top(); tap("Compare"); shot("comparison-dark")
    adb("shell", "settings", "put", "system", "font_scale", "1.3"); time.sleep(2)
    shot("comparison-large"); swipe(); shot("comparison-large-details")
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    OUT.joinpath("audit.txt").write_text(
        "Production APK screenshots on Android API 36, 720x1600, density 320. Synthetic demonstration records only. "
        "Normal text scale 1.0; comparison-large images use 1.3. System animations disabled for captures. "
        "Motion, TalkBack and frame rate are not measured by this script.\nAPK SHA256: " + hashlib.sha256(apk.read_bytes()).hexdigest()+"\n")


if __name__ == "__main__":
    main()
