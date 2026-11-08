"""Sensitivity analysis of H2, H2-R, H4b and H4b-R with the dataset as the statistical unit.

    python tools/dataset_sensitivity.py --machine=ADMIN-PC [--out FILE]
    python tools/dataset_sensitivity.py --self-test

Fixed after the main comparison (it is not part of the preregistered tests). On the rows of the main
tests (main group of each scope, report-only datasets left out, same pairing rule as the main tests:
only instances where CUT-HIDE and the method use the same number of masks), each dataset contributes
one value: the median, over its equal-count instances, of Lost_rel(method) - Lost_rel(CUT-HIDE). A
one-sided exact sign test then runs over the datasets, against the same practical threshold as H2:
H1 says the median difference exceeds DELTA_L, so a dataset counts as a success when its value is above
DELTA_L, as a failure when below, and is dropped when equal. The report also counts the datasets whose
value is positive. Holm's correction runs over the methods of each family, as in the main tests.

With n datasets the smallest attainable one-sided p-value is 0.5 ** n; the report prints it, because
with few datasets no outcome can reach alpha and the test is then uninformative by construction.

Exit code 1 when no SC3 file of the machine exists or a family has no dataset with an equal-count pair.
"""
import argparse
import io
import os
import statistics
import sys
from collections import defaultdict

from scipy import stats

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import levels_table  # noqa: E402
import preregistered_analysis as pa  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FAMILIES = (("H2", "distinct", pa.LOCKED.BASELINE_FAMILY), ("H2-R", "repeated", pa.LOCKED.BASELINE_FAMILY),
            ("H4b", "distinct", pa.LOCKED.STOP_FAMILY), ("H4b-R", "repeated", pa.LOCKED.STOP_FAMILY))


def sign_test(values, shift):
    above = sum(1 for v in values if v > shift)
    below = sum(1 for v in values if v < shift)
    n = above + below
    p = stats.binomtest(above, n, 0.5, alternative="greater").pvalue if n else 1.0
    return above, below, n, float(p)


def per_dataset(rows, scope, method, groups):
    by_instance = defaultdict(dict)
    for r in rows:
        if r["dataset"] in pa.LOCKED.REPORT_ONLY or pa.scope_of_set(r["sensitive_set"]) != scope:
            continue
        if (groups.get((r["dataset"], r["gap"], r["sigma"])) or {}).get("group") != "main":
            continue
        by_instance[(r["dataset"], r["gap"], r["sigma"], r["sensitive_set"])][r["method_key"]] = r
    diffs = defaultdict(list)
    for key, m in by_instance.items():
        ours, theirs = m.get(pa.LOCKED.OUR_METHOD), m.get(method)
        if ours is None or theirs is None or pa.exclusion_reason(ours, theirs):
            continue
        if ours["mask_count"] == theirs["mask_count"]:
            diffs[key[0]].append(theirs["lost_rel"] - ours["lost_rel"])
    return {d: statistics.median(v) for d, v in sorted(diffs.items())}, {d: len(v) for d, v in diffs.items()}


def run(rows, sc2_files, write):
    failed = False
    for name, scope, family in FAMILIES:
        groups = pa.group_configurations(sc2_files, profile="pool" if scope == "distinct" else "repeated")
        results = {}
        for method in family:
            med, cnt = per_dataset(rows, scope, method, groups)
            results[method] = (med, cnt) + sign_test(list(med.values()), pa.LOCKED.DELTA_L)
        p_holm = pa.holm({m: r[5] for m, r in results.items()})
        write("")
        write("== %s (%s scope), dataset as the unit, shift %.2f" % (name, scope, pa.LOCKED.DELTA_L))
        write("   %-10s %8s %8s %8s %6s %10s %10s %12s | per-dataset median difference (equal-count instances)"
              % ("method", "datasets", "above", "positive", "n", "p", "p_holm", "min p (0.5^n)"))
        for method in family:
            med, cnt, above, below, n, p = results[method]
            if not med:
                write("   %-10s no dataset with an equal-count pair" % method)
                failed = True
                continue
            positive = sum(1 for v in med.values() if v > 0)
            detail = ", ".join("%s %+.4f (%d)" % (d, v, cnt[d]) for d, v in med.items())
            write("   %-10s %8d %8d %8d %6d %10.4g %10.4g %12.4g | %s"
                  % (method, len(med), above, positive, n, p, p_holm[method][0] if isinstance(p_holm[method], tuple) else p_holm[method], 0.5 ** len(med), detail))
    return failed


def self_test():
    failures = []
    a, b, n, p = sign_test([0.03, 0.05, 0.01, 0.02], 0.02)
    if (a, b, n) != (2, 1, 3) or abs(p - 0.5) > 1e-12:
        failures.append("sign test with a tie at the shift: %s" % ((a, b, n, p),))
    a, b, n, p = sign_test([0.1] * 9, 0.02)
    if abs(p - 0.5 ** 9) > 1e-15:
        failures.append("nine successes: p %s, want %s" % (p, 0.5 ** 9))
    if sign_test([], 0.02)[3] != 1.0:
        failures.append("empty list")
    print("self-test: %d failures%s" % (len(failures), (": " + "; ".join(failures)) if failures else ""))
    print("checks: tie dropped, all successes, empty input")
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
    write("Sensitivity analysis, dataset as the unit (fixed after the main comparison)")
    rows = levels_table.load(args.sc3, args.machine, write)
    failed = run(rows, pa.collect_files(args.sc2, "SC2_"), write)
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
