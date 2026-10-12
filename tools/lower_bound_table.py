"""Join the SC3LB bounds to the official SC3 rows and report how far each method is from the bound.

    python tools/lower_bound_table.py [--out FILE]

Reads every SC3LB_*.csv and every SC3_*.csv of the measuring machine in results-invariant/, joins
them on (dataset, gap, sigma, scope, |S|, sensitive set), and prints per scope and size of the
sensitive set. The bound used is the conflict bound (patterns whose occurrence positions never
meet), which is at least the component bound (patterns with no shared item); both are reported.

  - the shape of the sets: components of the shared-item graph, how many sets have one pattern per
    component (there the bound is the optimum), and how often the conflict bound is the larger;
  - for each method key, |R| against the bound: how many sets reach it (proved optimal), and the
    median and largest ratio |R| / bound;
  - the sets where HHA uses fewer masks than the method under test, with both ratios.

Checks that stop the run with exit status 1: a bound row with no SC3 row or the other way round
(the join must be complete, and the report prints its denominators); a set of |S| = 1 whose bound
differs from the min_masks column of SC3 (both are the single-pattern optimum); a method with |R|
below the conflict bound (the bound would be wrong); a conflict bound below the component bound; a
single-pattern optimum that is not exact.
"""
import csv
import glob
import io
import os
import re
import statistics
import sys
from collections import defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
OURS = "cut-hide-rw-ls"
REFINED = "cut-hide-refine"   # preregistration section 17: secondary, its own batch of SC3 files
METHODS = ("cut-hide-rw-ls", REFINED, "cut-hide-rw", "cut-hide", "hha", "dbsh", "sbsh-r1", "sbsh-rmax")


def rows_of(path):
    with io.open(path, encoding="utf-8", newline="") as handle:
        lines = [line for line in handle if not line.startswith("#")]
    return list(csv.DictReader(lines))


def options_of(path):
    with io.open(path, encoding="utf-8", newline="") as handle:
        for line in handle:
            if line.startswith("# options"):
                return dict(re.findall(r"(\w+)=([^,}]+)", line))
    raise ValueError("%s has no options line" % Path(path).as_posix())


def dataset_name(path_text):
    return Path(path_text.replace("\\", "/")).stem


def main(argv):
    out_path = None
    if argv[:1] == ["--out"] and len(argv) == 2:
        out_path = argv[1]
    elif argv:
        print("usage: python tools/lower_bound_table.py [--out FILE]", file=sys.stderr)
        return 1
    directory = REPO / "results-invariant"
    bounds = {}
    for path in sorted(glob.glob(str(directory / "SC3LB_*.csv"))):
        for r in rows_of(path):
            if "conflict_bound" not in r:
                print("%s has no conflict_bound column (an older SC3LB)" % Path(path).name, file=sys.stderr)
                return 1
            key = (dataset_name(r["dataset"]), r["gap"], r["sigma"], r["pattern_kind"], r["sensitive_size"], r["sensitive_set"])
            if key in bounds:
                print("duplicate bound row for %s" % (key,), file=sys.stderr)
                return 1
            bounds[key] = r
    results = defaultdict(dict)
    for path in sorted(glob.glob(str(directory / "SC3_*DESKTOP-L8A6EPO*.csv"))):
        o = options_of(path)
        if o.get("methods") == "none":
            continue
        for r in rows_of(path):
            key = (dataset_name(r["dataset"]), r["gap"], r["sigma"], o["patternKind"], o["sensitiveSize"], r["sensitive_set"])
            results[key][r["method_key"]] = r   # two |S| = 1 cells have two identical files

    problems = []
    lines = []

    def write(text=""):
        lines.append(text)

    missing_results = [k for k in bounds if k not in results]
    missing_bounds = [k for k in results if k not in bounds]
    write("Bound rows: %d; SC3 sets: %d; joined: %d; bound without SC3: %d; SC3 without bound: %d"
          % (len(bounds), len(results), len(set(bounds) & set(results)), len(missing_results), len(missing_bounds)))
    for k in missing_results[:5] + missing_bounds[:5]:
        problems.append("not joined: %s" % (k,))
    for k, b in bounds.items():
        if b["optima_exact"] != "true":
            problems.append("single-pattern optimum not exact: %s" % (k,))
        if int(b["conflict_bound"]) < int(b["lower_bound"]):
            problems.append("conflict bound below the component bound: %s" % (k,))

    by_group = defaultdict(list)
    for k in sorted(set(bounds) & set(results)):
        by_group[(k[3], int(k[4]))].append(k)

    for (scope, size), keys in sorted(by_group.items()):
        write("")
        write("== %s scope, |S| = %d: %d sets" % (scope, size, len(keys)))
        comps = [int(bounds[k]["components"]) for k in keys]
        optimum = sum(1 for k in keys if bounds[k]["bound_is_optimum"] == "true")
        write("   components per set: median %s, min %d, max %d; one pattern per component (bound = optimum): %d/%d"
              % (statistics.median(comps), min(comps), max(comps), optimum, len(keys)))
        larger = sum(1 for k in keys if int(bounds[k]["conflict_bound"]) > int(bounds[k]["lower_bound"]))
        gain = [int(bounds[k]["conflict_bound"]) / int(bounds[k]["lower_bound"]) for k in keys if int(bounds[k]["lower_bound"]) > 0]
        write("   conflict bound above the component bound: %d/%d sets; median ratio conflict/component %.4f"
              % (larger, len(keys), statistics.median(gain) if gain else float("nan")))
        write("   %-16s %9s %12s %12s %10s" % ("method", "sets", "|R| = bound", "median ratio", "max ratio"))
        for m in METHODS:
            ratios = []
            reach = 0
            for k in keys:
                r = results[k].get(m)
                if r is None or r["status"]:
                    continue
                lb = int(bounds[k]["conflict_bound"])
                masks = int(r["mask_count"])
                if masks < lb:
                    problems.append("%s uses %d masks below the bound %d on %s" % (m, masks, lb, k))
                if size == 1 and r["min_masks"] and (int(r["min_masks"]) != lb
                                                     or int(bounds[k]["lower_bound"]) != lb):
                    problems.append("|S| = 1 bound %d differs from min_masks %s on %s" % (lb, r["min_masks"], k))
                if lb == 0:
                    continue
                ratios.append(masks / lb)
                reach += masks == lb
            if ratios:
                write("   %-16s %9d %7d/%-4d %12.4f %10.4f" % (m, len(ratios), reach, len(ratios),
                                                            statistics.median(ratios), max(ratios)))
        worse = [k for k in keys if not results[k][OURS]["status"] and not results[k]["hha"]["status"]
                 and int(results[k]["hha"]["mask_count"]) < int(results[k][OURS]["mask_count"])]
        if worse:
            ro = [int(results[k][OURS]["mask_count"]) / int(bounds[k]["conflict_bound"]) for k in worse]
            rh = [int(results[k]["hha"]["mask_count"]) / int(bounds[k]["conflict_bound"]) for k in worse]
            write("   sets where hha uses fewer masks than %s: %d; ratio to the bound there: %s median %.4f, hha median %.4f"
                  % (OURS, len(worse), OURS, statistics.median(ro), statistics.median(rh)))
        both = [k for k in keys if REFINED in results[k] and not results[k][REFINED]["status"]
                and not results[k]["hha"]["status"]]
        if both:
            fewer = sum(int(results[k]["hha"]["mask_count"]) < int(results[k][REFINED]["mask_count"]) for k in both)
            write("   sets where hha uses fewer masks than %s: %d of %d with both" % (REFINED, fewer, len(both)))

    write("")
    if problems:
        write("PROBLEMS (%d):" % len(problems))
        for p in problems[:20]:
            write("  " + p)
    else:
        write("No problem: the join is complete, every |S| = 1 bound equals min_masks, no method is below the bound.")
    text = "\n".join(lines) + "\n"
    if out_path:
        with io.open(out_path, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(text)
        print("wrote %s" % Path(out_path).as_posix())
    else:
        sys.stdout.write(text)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
