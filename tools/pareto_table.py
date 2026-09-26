"""Summarise the exact Pareto fronts of SC3 --pareto (preregistration section 11, point 6).

Every SC3PARETO_<stamp>.csv is paired with the SC3_<stamp>.csv of the same run. For every
configuration it prints, over the sensitive sets: the number of vertices of the front, the range of
|R| / |R|* along it, Lost_rel at its first vertex (the standard CUT-HIDE) and its least Lost_rel, and,
for every other method in the SC3 file, the share of sets in which some vertex of the front
dominates the method in (|R|, Lost_rel): no worse in both, better in one. It is descriptive: the
tests of the preregistration live in tools/thong_ke.py.

It also checks that the first vertex is the cut-hide row of the same set (same |R| and Lost): the
two are computed by the same exact program, so a difference is an error, not a result.

Run: python tools/pareto_table.py <directory or SC3PARETO file> [...]
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


def pair_files(paths):
    """(front file, SC3 file) pairs; a front file without its SC3 file is an error."""
    fronts = []
    for path in paths:
        if os.path.isdir(path):
            fronts.extend(os.path.join(path, n) for n in sorted(os.listdir(path))
                          if n.startswith("SC3PARETO_") and n.endswith(".csv"))
        elif os.path.isfile(path):
            fronts.append(path)
        else:
            raise SystemExit("no such file or directory: " + path)
    pairs = []
    for f in fronts:
        main = os.path.join(os.path.dirname(f), os.path.basename(f).replace("SC3PARETO_", "SC3_", 1))
        if not os.path.isfile(main):
            raise SystemExit("%s has no SC3 file of the same run (%s)" % (f, main))
        pairs.append((f, main))
    return pairs


def dominates(a_masks, a_lost, b_masks, b_lost):
    return a_masks <= b_masks and a_lost <= b_lost and (a_masks < b_masks or a_lost < b_lost)


def main(argv):
    pairs = pair_files(argv)
    if not pairs:
        print("no SC3PARETO file", file=sys.stderr)
        return 1
    problems = []
    print("| dataset | gap | sigma | sets | median vertices | max vertices | median max |R|/|R|* "
          "| median Lost_rel, first vertex | median least Lost_rel | methods dominated by the front "
          "(share of sets) | files |")
    print("|---|---|---:|---:|---:|---:|---:|---:|---:|---|---|")
    for front_path, main_path in pairs:
        _, front_rows = read(front_path)
        _, main_rows = read(main_path)
        by_set = {}
        for r in front_rows:
            by_set.setdefault(r["sensitive_set"], []).append(r)
        methods = {}
        for r in main_rows:
            methods.setdefault(r["method_key"], {}).setdefault(r["sensitive_set"], []).append(r)
        vertices, max_ratio, first_lost, least_lost = [], [], [], []
        dominated = {m: 0 for m in methods if m != "cut-hide"}
        for sensitive_set, rows in by_set.items():
            rows.sort(key=lambda r: int(r["vertex"]))
            if len(rows) != int(rows[0]["vertices"]):
                problems.append("%s %s: %d rows for %s vertices"
                                % (front_path, sensitive_set, len(rows), rows[0]["vertices"]))
            vertices.append(len(rows))
            ratios = [float(r["mask_ratio"]) for r in rows if r["mask_ratio"]]
            if ratios:
                max_ratio.append(max(ratios))
            first_lost.append(float(rows[0]["lost_rel"]))
            least_lost.append(min(float(r["lost_rel"]) for r in rows))
            standard = methods.get("cut-hide", {}).get(sensitive_set)
            if standard is None:
                problems.append("%s %s: no cut-hide row in %s" % (front_path, sensitive_set, main_path))
            elif (int(standard[0]["mask_count"]) != int(rows[0]["mask_count"])
                  or int(standard[0]["lost"]) != int(rows[0]["lost"])):
                problems.append("%s %s: first vertex (|R| %s, lost %s) is not the cut-hide row "
                                "(|R| %s, lost %s)" % (front_path, sensitive_set,
                                                      rows[0]["mask_count"], rows[0]["lost"],
                                                      standard[0]["mask_count"], standard[0]["lost"]))
            for m in dominated:
                other = methods[m].get(sensitive_set)
                if not other:
                    continue
                # A randomised method has one row per seed: every row must be dominated.
                if all(any(dominates(int(v["mask_count"]), float(v["lost_rel"]),
                                     int(o["mask_count"]), float(o["lost_rel"])) for v in rows)
                       for o in other):
                    dominated[m] += 1
        sets = len(by_set)
        first = front_rows[0]
        dataset = os.path.splitext(os.path.basename(first["dataset"].replace("\\", "/")))[0]
        shares = ", ".join("%s %d/%d" % (m, dominated[m], sets) for m in sorted(dominated))
        print("| %s | %s | %s | %d | %.1f | %d | %s | %.4f | %.4f | %s | %s |" % (
            dataset, first["gap"], first["sigma"], sets, statistics.median(vertices), max(vertices),
            ("%.3f" % statistics.median(max_ratio)) if max_ratio else "-",
            statistics.median(first_lost), statistics.median(least_lost), shares,
            os.path.basename(front_path)))
    print()
    if problems:
        for p in problems:
            print("PROBLEM " + p)
        print("%d problem(s)" % len(problems))
        return 1
    print("%d configurations; the first vertex is the cut-hide row in every set" % len(pairs))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
