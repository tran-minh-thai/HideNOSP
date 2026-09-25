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

# Raw inputs, stored under datasets/raw/. The Java code never reads them; tools/build_datasets.py
# turns each into an SPMF file and records that file in provenance.tsv as well.
FOURTU = "https://data.4tu.nl/file"
CATALOGUE.update({
    "bpi2012_raw": (FOURTU + "/533f66a4-8911-4ac7-8612-1235d65d1f37/3276db7f-8bee-4f2b-88ee-92dbffb5a893",
                    "raw/BPI_Challenge_2012.xes.gz", "XES_GZ", 3342406,
                    "raw event log BPI Challenge 2012, 4TU.ResearchData, "
                    "doi 10.4121/uuid:3926db30-f712-4394-aebc-75976070e91f; input of bpi2012"),
    "bpi2013_raw": (FOURTU + "/0fc5c579-e544-4fab-9143-fab1f5192432/aa51ffbb-25fd-4b5a-b0b8-9aba659b7e8c",
                    "raw/BPI_Challenge_2013_incidents.xes.gz", "XES_GZ", 1322247,
                    "raw event log BPI Challenge 2013 incidents, 4TU.ResearchData, "
                    "doi 10.4121/uuid:500573e6-accc-4b0c-9576-aa5468b10cee; input of bpi2013"),
    "sepsis_raw": (FOURTU + "/33632f3c-5c48-40cf-8d8f-2db57f5a6ce7/643dccf2-985a-459e-835c-a82bce1c0339",
                   "raw/Sepsis_Cases_Event_Log.xes.gz", "XES_GZ", 202508,
                   "raw event log Sepsis Cases, 4TU.ResearchData, "
                   "doi 10.4121/uuid:915d2bfb-7e84-49ad-a286-dc35f063a460; input of sepsis"),
})

# The checksums the publisher lists for the files above. A download that does not match them is
# refused before anything is written, whether or not provenance.tsv already has a record.
PUBLISHED_MD5 = {
    "bpi2012_raw": "74c7ba9aba85bfcb181a22c9d565e5b5",
    "bpi2013_raw": "d4809bd55e3e1c15b017ab4e58228297",
    "sepsis_raw": "b5671166ac71eb20680d3c74616c43d2",
}

# The 14 databases of HAUP-Hider (Wu, Zhou, Li, Liu, Wei, Wu), the closest hiding method on
# non-overlapping occurrences, from the authors' repository pinned to the commit that added them.
# The repository declares no licence: the files are fetched for evaluation, never redistributed.
HAUP = ("https://raw.githubusercontent.com/wuc567/Pattern-Mining/"
        "37c82b7be9dd7debdf32497666e79af6df79d002/HAUP-Hider/Databases")
HAUP_SIZES = {1: 50067, 2: 310553, 3: 23444, 4: 812992, 5: 1004723, 6: 235896, 7: 102005,
              8: 151714, 9: 204011, 10: 254770, 11: 305831, 12: 357219, 13: 408568, 14: 459324}
HAUP_UTILITY_SIZES = {1: 23, 2: 1615, 3: 74, 4: 51617, 5: 123143, 6: 1520}
for _k, _size in HAUP_SIZES.items():
    CATALOGUE["haup_sdb%d_raw" % _k] = (
        HAUP + "/SDB%d.txt" % _k, "raw/haup/SDB%d.txt" % _k, "TOKENS", _size,
        "raw database SDB%d of HAUP-Hider, github wuc567/Pattern-Mining at 37c82b7; input of "
        "haup_sdb%d" % (_k, _k))
for _k, _size in HAUP_UTILITY_SIZES.items():
    CATALOGUE["haup_sdb%d_utility_raw" % _k] = (
        HAUP + "/SDB%d_utility.txt" % _k, "raw/haup/SDB%d_utility.txt" % _k, "UTILITY", _size,
        "item utilities of SDB%d of HAUP-Hider; only needed to run HAUP-Hider itself" % _k)

# The protein datasets of the non-overlapping mining line (Wu et al.), as published with NTP-Miner
# in the same repository, pinned to the commit of that folder. One protein per line, one lower-case
# letter per residue. The six files are nested (exact sequence match, printed again by every build of
# tools/build_datasets.py): every sequence of SDB2 is in SDB1, of SDB3 in SDB2, of SDB5 in SDB4 and of
# SDB6 in SDB5. Only the two largest, SDB1 and SDB4, are built; the other four are kept because they
# are the published files that show the nesting.
NTP = ("https://raw.githubusercontent.com/wuc567/Pattern-Mining/"
       "0b3d4e51565abc1aff70c1c39f47785ad5c0d480/NTP-Miner/dataset")
NTP_SIZES = {1: 92887, 2: 63659, 3: 32839, 4: 110603, 5: 74223, 6: 37725}
NTP_BUILT = (1, 4)
NTP_CONTAINED_IN = {2: 1, 3: 2, 5: 4, 6: 5}
for _k, _size in NTP_SIZES.items():
    CATALOGUE["ntp_sdb%d_raw" % _k] = (
        NTP + "/SDB%d.txt" % _k, "raw/ntp/SDB%d.txt" % _k, "LETTERS", _size,
        "raw protein dataset SDB%d of NTP-Miner, github wuc567/Pattern-Mining at 0b3d4e5; " % _k
        + ("input of protein_sdb%d" % _k if _k in NTP_BUILT else
           "every sequence is also in SDB%d, so it is not built" % NTP_CONTAINED_IN[_k]))

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
    published = PUBLISHED_MD5.get(name)
    if published is not None and hashlib.md5(data).hexdigest() != published:
        print("  ERROR %s: md5 %s differs from the %s published by the source; nothing written"
              % (name, hashlib.md5(data).hexdigest(), published))
        return "mismatch"
    digest = hashlib.sha256(data).hexdigest()
    os.makedirs(os.path.dirname(target), exist_ok=True)
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
