"""Checks of the end-to-end SC3 trial, read from the result files only.

Usage: python e2e_checks.py <file listing the new SC3 files> <directory of the earlier attempt's SC3 files>

For every file: complete (no .partial next to it), one row per (set, method) for the seven keys,
HF = 0 for the three CUT-HIDE keys. One sensitive pattern: the three CUT-HIDE keys mask the same
number of positions, equal to min_masks (|R|*), and Lost(cut-hide-rw-ls) <= Lost(cut-hide-rw) <=
Lost(cut-hide). Several patterns: both adaptive keys give the row of cut-hide (same method name,
|R| and Lost). Files of the same configuration in the earlier attempt must have the same data rows.
Exit 1 on any failed check; every check prints how many cases it compared.
"""
import csv
import io
import os
import sys

KEYS = ("cut-hide-rw-ls", "cut-hide-rw", "cut-hide", "hha", "dbsh", "sbsh-r1", "sbsh-rmax")
CUT = ("cut-hide-rw-ls", "cut-hide-rw", "cut-hide")


def read(path):
    comments, lines = [], []
    with io.open(path, encoding="utf-8") as handle:
        for line in handle:
            (comments if line.startswith("#") else lines).append(line)
    return comments, list(csv.DictReader(lines))


def options(comments):
    for c in comments:
        if c.startswith("# options"):
            return c
    return ""


def config_key(comments):
    o = options(comments)
    parts = {}
    for token in o.replace("# options", "").replace("{", " ").replace("}", " ").replace(",", " ").split():
        if "=" in token:
            k, v = token.split("=", 1)
            parts[k.strip()] = v.strip()
    return tuple(parts.get(k, "") for k in ("dataset", "sigma", "gmax", "patternKind", "sensitiveSize", "setsPerStratum"))


def main(listing, earlier_dir):
    files = [l.strip() for l in open(listing) if l.strip()]
    failures = []
    counters = {}

    def count(name, ok):
        c = counters.setdefault(name, [0, 0])
        c[0] += 1
        if not ok:
            c[1] += 1

    earlier = {}
    for name in sorted(os.listdir(earlier_dir)):
        if name.startswith("SC3_") and name.endswith(".csv"):
            comments, rows = read(os.path.join(earlier_dir, name))
            earlier[config_key(comments)] = (name, rows)

    for path in files:
        comments, rows = read(path)
        key = config_key(comments)
        several = key[4] not in ("", "1")
        partial = os.path.exists(path + ".partial")
        count("file complete (no .partial)", not partial)
        by_set = {}
        for r in rows:
            by_set.setdefault(r["sensitive_set"], {}).setdefault(r["method_key"], []).append(r)
        for s, methods in by_set.items():
            count("every key present once per set", all(len(methods.get(k, [])) == 1 for k in KEYS))
            one = {k: methods[k][0] for k in KEYS if k in methods}
            for k in CUT:
                count("HF = 0 for the CUT-HIDE keys", float(one[k]["hf"]) == 0.0)
            if several:
                for k in ("cut-hide-rw", "cut-hide-rw-ls"):
                    same = all(one[k][c] == one["cut-hide"][c] for c in ("method", "mask_count", "lost"))
                    count("several patterns: adaptive keys give the cut-hide row", same)
                    if not same:
                        failures.append("%s set %s: %s differs from cut-hide" % (path, s, k))
            else:
                masks = {k: int(one[k]["mask_count"]) for k in CUT}
                equal = len(set(masks.values())) == 1 and masks["cut-hide"] == int(one["cut-hide"]["min_masks"])
                count("one pattern: |R| equal across CUT-HIDE keys and equal to |R|*", equal)
                lost = {k: int(one[k]["lost"]) for k in CUT}
                order = lost["cut-hide-rw-ls"] <= lost["cut-hide-rw"] <= lost["cut-hide"]
                count("one pattern: Lost rw-ls <= rw <= cut-hide", order)
                if not (equal and order):
                    failures.append("%s set %s: masks %s lost %s" % (path, s, masks, lost))
        if key in earlier:
            name, old_rows = earlier[key]
            same_rows = [dict(r) for r in old_rows] == [dict(r) for r in rows]
            count("data rows identical to the earlier attempt (same configuration)", same_rows)
            if not same_rows:
                failures.append("%s: data rows differ from %s" % (path, name))
        print("%s: %s, %d sets, %d rows" % (os.path.basename(path), " ".join(key), len(by_set), len(rows)))
        totals = {k: sum(int(m[k][0]["lost"]) for m in by_set.values() if k in m) for k in KEYS}
        fewer = sum(1 for m in by_set.values() if int(m["cut-hide-rw-ls"][0]["lost"]) < int(m["cut-hide"][0]["lost"]))
        print("   Lost summed over sets: " + ", ".join("%s %d" % (k, totals[k]) for k in KEYS)
              + "; sets where cut-hide-rw-ls loses fewer than cut-hide: %d" % fewer)
    print()
    for name, (n, bad) in counters.items():
        print("%-72s %d checked, %d failed" % (name, n, bad))
    for f in failures:
        print("FAILED " + f)
    return 1 if failures or any(bad for _, bad in counters.values()) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2]))
