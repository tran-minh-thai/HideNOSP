"""Download the public datasets of section 11.2 into datasets/ and record their provenance.

Raw data never enters git (see .gitignore); what enters git is datasets/provenance.tsv, which
records for every file its URL, the day it was fetched, its SHA-256 and its size. That record is
what lets someone else obtain the same bytes, and it is what a reviewer needs when a table says
"MSNBC" without saying which copy of MSNBC.

The script is idempotent: a file already present whose SHA-256 matches the record is left alone.

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
    url, filename, fmt, expected, role = CATALOGUE[name]
    target = os.path.join(DATASETS, filename)
    if os.path.exists(target) and name in rows and sha256_of(target) == rows[name][6]:
        print("  %-8s da co, tong kiem khop, bo qua" % name)
        return
    print("  %-8s tai %s" % (name, url))
    request = urllib.request.Request(url, headers={"User-Agent": "hidenosp-fetch/1.0"})
    with urllib.request.urlopen(request, timeout=120) as response:
        data = response.read()
    if expected is not None and len(data) != expected:
        print("     CANH BAO: nhan %d byte, cho %d. Nguon co the da doi; kiem lai truoc khi dung."
              % (len(data), expected))
    with open(target, "wb") as handle:
        handle.write(data)
    digest = sha256_of(target)
    rows[name] = [name, filename, fmt, url, time.strftime("%Y-%m-%d"), str(len(data)), digest, role]
    print("     %d byte, sha256 %s" % (len(data), digest[:16] + "..."))


def main():
    names = sys.argv[1:] or list(CATALOGUE)
    unknown = [n for n in names if n not in CATALOGUE]
    if unknown:
        print("khong biet bo du lieu: " + ", ".join(unknown))
        print("co the chon: " + ", ".join(CATALOGUE))
        return 2
    if not os.path.isdir(DATASETS):
        os.makedirs(DATASETS)
    rows = read_provenance()
    print("tai ve %s" % DATASETS)
    for name in names:
        fetch(name, rows)
    write_provenance(rows)
    print("da ghi %s" % PROVENANCE)
    return 0


if __name__ == "__main__":
    sys.exit(main())
