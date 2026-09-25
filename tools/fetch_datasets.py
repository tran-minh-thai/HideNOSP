"""Download the public datasets of section 11.2 into datasets/ and record their provenance.

Raw data never enters git (see .gitignore); what enters git is datasets/provenance.tsv, which
records for every file its URL, the day it was fetched, its SHA-256 and its size. That record is
what lets someone else obtain the same bytes, and it is what a reviewer needs when a table says
"MSNBC" without saying which copy of MSNBC.

The script is idempotent: a file already present whose SHA-256 matches the record is left alone.
A dataset that already has a record is only verified against it; a download that differs is
reported as an error (exit code 1) and the record is never overwritten.

Run: python tools/fetch_datasets.py [name ...]
With no argument it fetches every dataset listed in CATALOGUE.
"""
import hashlib
import io
import os
import sys
import time
import urllib.request

BASE = "https://www.philippe-fournier-viger.com/spmf/publicdatasets"

# name -> (url, local file, format for io.DatasetReader, expected bytes or None, role in section 11.2)
CATALOGUE = {
    "msnbc": (BASE + "/MSNBC.txt", "msnbc.txt", "SPMF", 2314085,
              "nhom chinh: nhap chuot theo nhom trang, lap nhieu; [R3] dung ban MSN"),
    "sign":  (BASE + "/SIGN.txt", "sign.txt", "SPMF", 236727,
              "nhom chinh neu rho_rep du cao: chuoi su kien don muc"),
    "fifa":  (BASE + "/FIFA.txt", "fifa.txt", "SPMF", 4939888,
              "nhom chinh neu rho_rep du cao: chuoi su kien don muc"),
    "bms1":  (BASE + "/BMS1_spmf.txt", "bms1.txt", "SPMF", 1525544,
              "doi chung suy bien: chuoi ngan, it lap; [R3] dung BMS-WebView-1"),
    # Bo sung dot 2: chon theo dac trung ma dong gop cua bai can (|Sigma| nho, chuoi dai, lap trong chuoi).
    "dna":   (BASE + "/MT745584_SPMF.txt", "dna_mt745584.txt", "SPMF", 151789,
              "ung vien nhom chinh: bo gen COVID-19 MT745584, |Sigma|=4; dang du lieu cua dong NOSEP [R1]"),
    "bike":  (BASE + "/BIKE.txt", "bike.txt", "SPMF", 1290298,
              "ung vien: chuoi tram xe dap theo thoi gian, |Sigma|=67; ban cong khai gan nhat voi trucks cua [R20]"),
    "leviathan": (BASE + "/LEVIATHAN.txt", "leviathan.txt", "SPMF", 1339118,
              "ung vien: van ban Leviathan theo chuoi tu, dai trung binh 33.8"),
    "bible": (BASE + "/BIBLE.txt", "bible.txt", "SPMF", 5572333,
              "ung vien: van ban Kinh thanh theo chuoi tu, dai trung binh 21.6"),
}

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DATASETS = os.path.join(ROOT, "datasets")
PROVENANCE = os.path.join(DATASETS, "provenance.tsv")
HEADER = ["name", "file", "format", "url", "fetched", "bytes", "sha256", "role"]


def sha256_of(path):
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for block in iter(lambda: handle.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def read_provenance():
    rows = {}
    if not os.path.exists(PROVENANCE):
        return rows
    with io.open(PROVENANCE, encoding="utf-8") as handle:
        for line in handle:
            if line.startswith("#") or not line.strip():
                continue
            cells = line.rstrip("\n").split("\t")
            if cells[0] == "name":
                continue
            rows[cells[0]] = cells
    return rows


def write_provenance(rows):
    with io.open(PROVENANCE, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("# Nguon goc bo du lieu, sinh boi tools/fetch_datasets.py.\n")
        handle.write("# Du lieu tho khong vao git; tep nay thi co, vi no la thu cho phep lay lai dung bytes do.\n")
        handle.write("\t".join(HEADER) + "\n")
        for name in sorted(rows):
            handle.write("\t".join(rows[name]) + "\n")


def fetch(name, rows):
    """Fetch one dataset and check it against its provenance record.

    Returns "present", "fetched", "recorded" (a dataset with no record yet, now recorded) or
    "mismatch". A recorded dataset is VERIFIED, never re-recorded: the record is what the paper's
    numbers were computed on, so a download that differs from it is an error, not an update.
    Rewriting the record here would also change provenance.tsv on every fresh machine (the fetch
    date alone differs), and tools/run.py would then refuse every run because the tree is dirty.
    """
    url, filename, fmt, expected, role = CATALOGUE[name]
    target = os.path.join(DATASETS, filename)
    recorded = rows[name][6] if name in rows else None
    if recorded is not None and os.path.exists(target) and sha256_of(target) == recorded:
        print("  %-9s present, sha256 matches the record" % name)
        return "present"
    print("  %-9s downloading %s" % (name, url))
    request = urllib.request.Request(url, headers={"User-Agent": "hidenosp-fetch/1.0"})
    with urllib.request.urlopen(request, timeout=120) as response:
        data = response.read()
    digest = hashlib.sha256(data).hexdigest()
    if recorded is not None and digest != recorded:
        rejected = target + ".mismatch"
        with open(rejected, "wb") as handle:
            handle.write(data)
        print("  ERROR %s: the download does not match datasets/provenance.tsv" % name)
        print("        recorded sha256 %s (%s bytes)" % (recorded, rows[name][5]))
        print("        received sha256 %s (%d bytes)" % (digest, len(data)))
        print("        kept aside as %s; %s was not touched" % (rejected, target))
        return "mismatch"
    with open(target, "wb") as handle:
        handle.write(data)
    if recorded is not None:
        print("            %d bytes, sha256 matches the record" % len(data))
        return "fetched"
    if expected is not None and len(data) != expected:
        print("  WARNING %s: %d bytes received, %d expected by CATALOGUE" % (name, len(data), expected))
    rows[name] = [name, filename, fmt, url, time.strftime("%Y-%m-%d"), str(len(data)), digest, role]
    print("            %d bytes, sha256 %s..., NEW record" % (len(data), digest[:16]))
    return "recorded"


def main():
    names = sys.argv[1:] or list(CATALOGUE)
    unknown = [n for n in names if n not in CATALOGUE]
    if unknown:
        print("unknown dataset: " + ", ".join(unknown))
        print("known: " + ", ".join(CATALOGUE))
        return 2
    if not os.path.isdir(DATASETS):
        os.makedirs(DATASETS)
    rows = read_provenance()
    print("datasets directory: %s" % DATASETS)
    outcome = {name: fetch(name, rows) for name in names}
    if "recorded" in outcome.values():
        write_provenance(rows)
        print("wrote %s (new records: %s)" % (PROVENANCE, ", ".join(
            n for n, o in outcome.items() if o == "recorded")))
    counts = {o: sum(1 for v in outcome.values() if v == o)
              for o in ("present", "fetched", "recorded", "mismatch")}
    print("checked %d dataset(s): %d present, %d fetched, %d newly recorded, %d MISMATCH"
          % (len(names), counts["present"], counts["fetched"], counts["recorded"], counts["mismatch"]))
    if counts["mismatch"]:
        print("STOP: do not run anything on these datasets until the mismatch is resolved.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
