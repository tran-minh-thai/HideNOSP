# SPDX-License-Identifier: MIT
"""Join the SC3LB bounds to the official SC3 rows and report how far each method is from the bound.

    python tools/lower_bound_table.py [--machine=NAME] [--bound-machine=NAME] [--dir=DIR] [--out FILE]

Reads the SC3LB_*.csv files and the SC3_*.csv files of one machine in DIR (default: results-invariant/
of the repository), joins them on (dataset, gap, sigma, scope, |S|, sensitive set), and prints per
scope and size of the sensitive set. The bound used is the conflict bound (patterns whose occurrence
positions never meet), which is at least the component bound (patterns with no shared item); both
are reported.

The machine is the one named in the run stamp of each file header. --machine picks the SC3 files and
--bound-machine the SC3LB files; each may be left out only when the files of its kind come from one
machine, so that two machines never enter one table. SC3 files are taken from the method batches and
the refinement batch of the main group (the cells of tools/sc3_commands.py). The Pareto-front batch
(--methods=none) and the degenerate-group batch are left out and counted: SC3LB is run on the main
group only. An SC3 file that matches no planned cell stops the run.

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

import closure_rule  # noqa: E402
import sc3_commands

REPO = Path(__file__).resolve().parent.parent
OURS = "cut-hide-rw-ls"
REFINED = "cut-hide-refine"   # preregistration section 17: secondary, its own batch of SC3 files
METHODS = ("cut-hide-rw-ls", REFINED, "cut-hide-rw", "cut-hide", "hha", "dbsh", "sbsh-r1", "sbsh-rmax",
           "occurrence", "hha-stop")   # the last two: preregistration section 19.1, their own batch


def rows_of(path):
    with io.open(path, encoding="utf-8", newline="") as handle:
        lines = [line for line in handle if not line.startswith("#")]
    return list(csv.DictReader(lines))


def dataset_name(path_text):
    return Path(path_text.replace("\\", "/")).stem


def machine_of(path):
    """The machine field of the run stamp in the header of a result file, or "?" without a stamp."""
    with io.open(path, encoding="utf-8-sig", newline="") as handle:
        for line in handle:
            if not line.startswith("#"):
                break
            match = re.match(r"^# run \d{8}-\d{6}(?:-\d{3})?_([^_]+)_", line)
            if match:
                return match.group(1)
    return "?"


def one_machine(paths, wanted, flag, what):
    """The files of one machine: the machine named by flag, or the only machine present.
    Returns (files, machine, None), or (None, None, the reason for refusing)."""
    by_machine = defaultdict(list)
    for p in paths:
        by_machine[machine_of(p)].append(p)
    found = ", ".join("%s (%d files)" % (m, len(v)) for m, v in sorted(by_machine.items())) or "none"
    if wanted:
        if wanted not in by_machine:
            return None, None, "no %s file of machine %s; machines found: %s" % (what, wanted, found)
        return by_machine[wanted], wanted, None
    if len(by_machine) > 1:
        return None, None, "the %s files come from %d machines: %s. Name one with %s=NAME." % (
            what, len(by_machine), found, flag)
    if not by_machine:
        return [], "none", None
    machine, files = next(iter(by_machine.items()))
    return files, machine, None


def header_options(path):
    """The options of a result file, read with the parser of tools/sc3_commands.py (a list value
    such as methods=a,b,c stays whole)."""
    with io.open(path, encoding="utf-8", newline="") as handle:
        for line in handle:
            if not line.startswith("#"):
                break
            if line.startswith("# options "):
                return sc3_commands.parse_options(line.rstrip("\r\n"))
    raise ValueError("%s has no options line" % Path(path).as_posix())


def planned_cells():
    """The planned SC3 cells by batch, from the locked group tables."""
    return {
        "methods": [c for s in sc3_commands.SIZES for c in sc3_commands.planned(REPO, s)],
        "refine": [c for s in sc3_commands.SIZES if s > 1 for c in sc3_commands.planned(REPO, s, refine=True)],
        "stop": [c for s in sc3_commands.SIZES for c in sc3_commands.planned(REPO, s, stop=True)],
        "degenerate": sc3_commands.planned(REPO, 1, group="degenerate"),
    }


def main(argv):
    out_path = None
    directory = REPO / "results-invariant"
    machine = bound_machine = ""
    rule = "necessary"
    i = 0
    while i < len(argv):
        a = argv[i]
        if a == "--out" and i + 1 < len(argv):
            out_path = argv[i + 1]
            i += 2
            continue
        if a.startswith("--machine="):
            machine = a.split("=", 1)[1]
        elif a.startswith("--bound-machine="):
            bound_machine = a.split("=", 1)[1]
        elif a.startswith("--dir="):
            directory = Path(a.split("=", 1)[1])
        elif a.startswith("--closure=") and a.split("=", 1)[1] in closure_rule.RULES:
            rule = a.split("=", 1)[1]
        else:
            print("usage: python tools/lower_bound_table.py [--machine=NAME] [--bound-machine=NAME] "
                  "[--dir=DIR] [--closure=necessary|subsequence] [--out FILE]", file=sys.stderr)
            return 1
        i += 1
    if not directory.is_dir():
        print("no directory %s" % directory.as_posix(), file=sys.stderr)
        return 1
    bound_files, bound_machine, refusal = one_machine(
        sorted(glob.glob(str(directory / "SC3LB_*.csv"))), bound_machine, "--bound-machine", "SC3LB")
    if refusal:
        print(refusal, file=sys.stderr)
        return 1
    sc3_files, machine, refusal = one_machine(
        sorted(glob.glob(str(directory / "SC3_*.csv"))), machine, "--machine", "SC3")
    if refusal:
        print(refusal, file=sys.stderr)
        return 1
    # SC3LB reads no S-plus; the SC3 rows it is compared with do.
    sc3_files, found = closure_rule.keep(sc3_files, rule)
    if not sc3_files:
        print("no SC3 file under closure rule %s; rules found: %s" % (rule, found), file=sys.stderr)
        return 1
    bounds = {}
    for path in bound_files:
        for r in rows_of(path):
            if "conflict_bound" not in r:
                print("%s has no conflict_bound column (an older SC3LB)" % Path(path).name, file=sys.stderr)
                return 1
            key = (dataset_name(r["dataset"]), r["gap"], r["sigma"], r["pattern_kind"], r["sensitive_size"], r["sensitive_set"])
            if key in bounds:
                print("duplicate bound row for %s" % (key,), file=sys.stderr)
                return 1
            bounds[key] = r
    problems = []
    cells = planned_cells()
    taken = defaultdict(int)
    results = defaultdict(dict)
    for path in sc3_files:
        o = header_options(path)
        if o.get("methods") == "none":
            taken["front"] += 1
            continue
        # The gaps added after the main comparison are not in this table's group tables, so a file of
        # theirs matches no planned cell. Leave it out and count it, instead of reading it as a problem.
        if sc3_commands.positive_minimum_gap("[%s,%s]" % (o["gmin"], o["gmax"])):
            taken["positive-gap"] += 1
            continue
        batch = next((b for b, cs in cells.items() if any(sc3_commands.matches(c, o) for c in cs)), None)
        if batch is None:
            problems.append("SC3 file matches no planned cell: %s" % Path(path).name)
            continue
        taken[batch] += 1
        if batch == "degenerate":
            continue
        for r in rows_of(path):
            key = (dataset_name(r["dataset"]), r["gap"], r["sigma"], o["patternKind"], o["sensitiveSize"], r["sensitive_set"])
            results[key][r["method_key"]] = r   # two |S| = 1 cells have two identical files

    lines = [closure_rule.describe("SC3", rule, sc3_files, found)]

    def write(text=""):
        lines.append(text)

    write("SC3 files of %s: %d method-batch, %d refinement-batch%s; left out: %d degenerate-group (SC3LB covers "
          "the main group only), %d Pareto-front, %d with a positive minimum gap (this table covers g- = 0). "
          "SC3LB files of %s: %d."
          % (machine, taken["methods"], taken["refine"],
             ", %d stop-at-threshold-batch" % taken["stop"] if taken["stop"] else "",
             taken["degenerate"], taken["front"], taken["positive-gap"], bound_machine, len(bound_files)))
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
