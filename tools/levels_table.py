# SPDX-License-Identifier: MIT
"""Levels of the side effects at the solution each method returns, DESCRIPTIVE: no test.

    python tools/levels_table.py --machine=ADMIN-PC [--out FILE]
    python tools/levels_table.py --self-test

H2 compares Lost_rel only where two methods use the same number of masks. This table reports what
the data holder gets from each method at the method's own solution: the distribution of Lost_rel
and the medians of the distortion rate (dist_r = |R| / sum |S|) and of SupDist. It reads the same
rows as the pairs tables of tools/preregistered_analysis.py (main group of each scope, report-only
datasets left out, an instance being a dataset, gap, sigma and sensitive set), and keeps only the
instances where CUT-HIDE, the four same-family baselines and the two stopping rivals all have a
valid row, so every box of a block is drawn over the same instances.

Blocks: scope (distinct, repeated) x number of sensitive patterns (1, and 5 to 20 pooled).
Box statistics: quartiles by linear interpolation (numpy default), whiskers at the most extreme
value within 1.5 IQR of the box (Tukey); the share of values beyond the whiskers is printed.

Exit code 1 when no SC3 file of the machine exists, when a block is empty, or when an instance
has a method row with a status or HF > 0.
"""
import argparse
import io
import os
import re
import sys
from collections import defaultdict

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import preregistered_analysis as pa  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
METHODS = (pa.LOCKED.OUR_METHOD,) + pa.LOCKED.BASELINE_FAMILY + pa.LOCKED.STOP_FAMILY
SIZE_GROUPS = (("1", lambda n: n == 1), ("5-20", lambda n: n >= 2))


def set_size(text):
    return len(re.findall(r"\[([^\[\]]*)\]", text))


def box(values):
    v = np.asarray(sorted(values), dtype=float)
    q1, med, q3 = (float(x) for x in np.percentile(v, [25, 50, 75]))
    iqr = q3 - q1
    inside = v[(v >= q1 - 1.5 * iqr) & (v <= q3 + 1.5 * iqr)]
    lo, hi = float(inside.min()), float(inside.max())
    beyond = int(np.sum((v < lo) | (v > hi)))
    return {"lo": lo, "q1": q1, "median": med, "q3": q3, "hi": hi, "max": float(v.max()), "beyond": beyond}


def load(directory, machine, write):
    files = [p for p in pa.collect_files(directory, "SC3_") if pa.machine_of(p) == machine]
    if not files:
        raise SystemExit("no SC3 file of machine %s in %s" % (machine, directory))
    raw, used = [], 0
    for path in files:
        comments, rows = pa.read_csv(path)
        options = next((c for c in comments if c.startswith("# options ")), "")
        if re.search(r"[{ ]methods=(%s|none)[,}]" % re.escape(pa.SECONDARY_METHOD), options):
            continue
        raw.extend(rows)
        used += 1
    write("SC3 files of %s read: %d (refinement and Pareto-front batches left out), %d rows"
          % (machine, used, len(raw)))
    return pa.merge_reruns(raw, lambda s: None)


def blocks(rows, sc2_files, write):
    out = {}
    failed = False
    for scope, profile in (("distinct", "pool"), ("repeated", "repeated")):
        groups = pa.group_configurations(sc2_files, profile=profile)
        by_instance = defaultdict(dict)
        for r in rows:
            if r["dataset"] in pa.LOCKED.REPORT_ONLY or pa.scope_of_set(r["sensitive_set"]) != scope:
                continue
            if (groups.get((r["dataset"], r["gap"], r["sigma"])) or {}).get("group") != "main":
                continue
            by_instance[(r["dataset"], r["gap"], r["sigma"], r["sensitive_set"])][r["method_key"]] = r
        complete = {k: v for k, v in by_instance.items() if all(m in v for m in METHODS)}
        write("")
        write("%s scope: %d instances in the main group, %d with every method of %s"
              % (scope, len(by_instance), len(complete), ", ".join(METHODS)))
        for key, by_method in complete.items():
            for m in METHODS:
                reason = pa.exclusion_reason(by_method[m], by_method[m])
                if reason:
                    write("  INVALID ROW: %s %s: %s" % (m, key, reason))
                    failed = True
        for label, keep in SIZE_GROUPS:
            inst = [v for k, v in complete.items() if keep(set_size(k[3]))]
            if not inst:
                write("  EMPTY BLOCK: %s |S| = %s" % (scope, label))
                failed = True
                continue
            out[(scope, label)] = {m: {"lost_rel": box([i[m]["lost_rel"] for i in inst]),
                                       "dist_r": float(np.median([i[m]["dist_r"] for i in inst])),
                                       "sup_dist": float(np.median([i[m]["sup_dist"] for i in inst])),
                                       "n": len(inst)} for m in METHODS}
    return out, failed


def report(out, write):
    for (scope, label), by_method in out.items():
        n = by_method[METHODS[0]]["n"]
        write("")
        write("== %s scope, |S| = %s: %d instances" % (scope, label, n))
        write("   %-16s %5s | %8s %8s %8s %8s %8s %8s %6s | %8s %8s"
              % ("method", "n", "lo", "q1", "median", "q3", "hi", "max", "beyond", "dist_r", "sup_dist"))
        for m in METHODS:
            b, s = by_method[m]["lost_rel"], by_method[m]
            write("   %-16s %5d | %8.4f %8.4f %8.4f %8.4f %8.4f %8.4f %6d | %8.4f %8.4f"
                  % (m, s["n"], b["lo"], b["q1"], b["median"], b["q3"], b["hi"], b["max"], b["beyond"],
                     s["dist_r"], s["sup_dist"]))


def self_test():
    failures = []
    b = box([0, 0, 0, 1, 2, 3, 100])
    if not (b["median"] == 1 and b["hi"] == 3 and b["beyond"] == 1 and b["max"] == 100):
        failures.append("box of a sample with one outlier: %s" % b)
    if box([5.0])["lo"] != 5.0 or box([5.0])["beyond"] != 0:
        failures.append("box of one value")
    if set_size("[[1, 2], [3, 4, 5]]") != 2 or set_size("[[1, 1]]") != 1:
        failures.append("set_size")
    rows = [{"dataset": "d", "gap": "[0,1]", "sigma": 5, "sensitive_set": "[[1, 2]]", "method_key": m,
             "status": "", "hf": 0.0, "lost_rel": 0.1, "dist_r": 0.01, "sup_dist": 0.02} for m in METHODS]
    rows[1] = dict(rows[1], hf=0.5)
    try:
        import preregistered_analysis as p2
        saved = p2.group_configurations
        p2.group_configurations = lambda files, profile="pool": {("d", "[0,1]", 5): {"group": "main"}}
        out, failed = blocks(rows, [], lambda s: None)
        if not failed:
            failures.append("a row with HF > 0 was not reported")
        out, failed = blocks(rows[:1] + rows[2:], [], lambda s: None)
        if not failed:
            failures.append("an instance without every method left an empty block unreported")
        p2.group_configurations = saved
    except Exception as e:  # pragma: no cover
        failures.append("blocks raised %r" % e)
    print("self-test: %d failures%s" % (len(failures), (": " + "; ".join(failures)) if failures else ""))
    print("checks: box with an outlier, box of one value, set size, invalid row, incomplete instance")
    return 1 if failures else 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--sc3", default=os.path.join(ROOT, "results-invariant"))
    parser.add_argument("--sc2", default=os.path.join(ROOT, "results-invariant"))
    parser.add_argument("--machine", default="ADMIN-PC")
    parser.add_argument("--out", default="")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)
    if args.self_test:
        return self_test()
    lines = []
    write = lines.append
    write("Levels of Lost_rel, distortion rate and SupDist at each method's own solution, DESCRIPTIVE")
    rows = load(args.sc3, args.machine, write)
    out, failed = blocks(rows, pa.collect_files(args.sc2, "SC2_"), write)
    report(out, write)
    text = "\n".join(lines) + "\n"
    if args.out:
        with io.open(args.out, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
        print("wrote %s" % args.out)
    else:
        sys.stdout.write(text)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
