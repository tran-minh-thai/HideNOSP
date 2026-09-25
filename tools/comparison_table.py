"""Print a descriptive Markdown table of SC3 files: one row per configuration and method.

For every (dataset, gap, sigma, kind of sensitive pattern) and every method key it gives the number
of sensitive sets, how many of them the method failed to hide (HF > 0), and the medians of |R|,
|R| / |R|*, Lost_rel and SupDist over the sets. It is descriptive only: the tests of the
preregistration live in tools/thong_ke.py. Every row names the file it comes from.

Run: python tools/comparison_table.py <SC3 file or directory> [...]
"""
import csv
import io
import os
import statistics
import sys


def read(path):
    comments, lines = [], []
    with io.open(path, encoding="utf-8") as handle:
        for line in handle:
            (comments if line.startswith("#") else lines).append(line)
    return comments, list(csv.DictReader(lines))


def kind(comments):
    """The kind of sensitive pattern the file was drawn from, read from its header."""
    joined = "".join(comments)
    if "patternKind=repeated" in joined:
        return "repeated"
    return "distinct"


def median(values):
    return statistics.median(values) if values else float("nan")


def main(argv):
    files = []
    for path in argv:
        if os.path.isdir(path):
            files.extend(os.path.join(path, n) for n in sorted(os.listdir(path))
                         if n.startswith("SC3_") and n.endswith(".csv"))
        elif os.path.isfile(path):
            files.append(path)
        else:
            raise SystemExit("no such file or directory: " + path)
    print("| dataset | gap | sigma | kind | method | sets | HF > 0 | median \\|R\\| "
          "| median \\|R\\|/\\|R\\|* | median Lost_rel | median SupDist | file |")
    print("|---|---|---:|---|---|---:|---:|---:|---:|---:|---:|---|")
    rows_out = 0
    for path in files:
        comments, rows = read(path)
        if not rows:
            continue
        if "method_key" not in rows[0]:
            raise SystemExit("%s has no method_key column: not an SC3 file of this version" % path)
        by_method = {}
        for r in rows:
            by_method.setdefault(r["method_key"], []).append(r)
        first = rows[0]
        dataset = os.path.splitext(os.path.basename(first["dataset"].replace("\\", "/")))[0]
        for method, rs in by_method.items():
            ratios = [float(r["mask_ratio"]) for r in rs if r["mask_ratio"]]
            print("| %s | %s | %s | %s | %s | %d | %d | %.1f | %s | %.4f | %.4f | %s |" % (
                dataset, first["gap"], first["sigma"], kind(comments), method, len(rs),
                sum(1 for r in rs if float(r["hf"]) > 0),
                median([int(r["mask_count"]) for r in rs]),
                ("%.3f" % median(ratios)) if ratios else "-",
                median([float(r["lost_rel"]) for r in rs]),
                median([float(r["sup_dist"]) for r in rs]),
                os.path.basename(path)))
            rows_out += 1
    print()
    print("%d rows from %d files" % (rows_out, len(files)))
    return 0 if rows_out else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
