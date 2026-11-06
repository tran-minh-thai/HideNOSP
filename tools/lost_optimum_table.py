"""Summarise SC3LOST: how far each method is from the exact Lost optimum on small instances.

    python tools/lost_optimum_table.py [--machine=NAME] [--dir=DIR] [--out FILE]

Reads the SC3LOST_*.csv files of one machine in DIR (default: results-invariant/ of the repository) and
prints, per scope and method, the quantities of preregistration section 19.2, on the instances solved
exactly, with protein_sdb1 reported apart (section 11, point 5):

  - instances, and the skipped instances by reason (the denominators);
  - the share of instances with |R| = |R|*; among them the share with Lost = OPT_Lost, the sum and the
    median of Lost - OPT_Lost, and the median position of Lost in the range from OPT_Lost to the greatest
    Lost of a minimum hiding set (0 = optimum, 1 = worst; instances where the range is empty count 0);
  - for |R| > |R|*: the count, and how often the lexicographic optimum is no worse in both |R| and Lost;
  - per CUT-HIDE key, the instances where B_eps was empty and CUT-HIDE ran with uniform weights (the
    selector of CutHide.forBand, preregistration section 19.2, correction of 2026-11-06), and how many
    of them reached Lost = OPT_Lost. The method column of such a row says so.

Implementation checks that stop the run with exit status 1: a method row with a status (does not hide,
method failed, check failed); |R| < |R|*; a CUT-HIDE key with |R| != |R|*; Lost < OPT_Lost at |R|*; the
greatest Lost below the least. The report prints how many rows each check covered.
"""
import csv
import glob
import io
import statistics
import sys
from collections import Counter, defaultdict
from pathlib import Path

from lower_bound_table import one_machine

REPO = Path(__file__).resolve().parent.parent
CUT_HIDE = ("cut-hide", "cut-hide-rw", "cut-hide-rw-ls")
METHODS = ("cut-hide-rw-ls", "cut-hide-rw", "cut-hide", "hha", "dbsh", "sbsh-r1", "sbsh-rmax", "occurrence",
           "hha-stop")
REPORT_ONLY = ("protein_sdb1",)


def rows_of(path):
    with io.open(path, encoding="utf-8", newline="") as handle:
        return list(csv.DictReader([line for line in handle if not line.startswith("#")]))


def dataset_name(path_text):
    return Path(path_text.replace("\\", "/")).stem


def main(argv):
    out_path = None
    directory = REPO / "results-invariant"
    machine = ""
    i = 0
    while i < len(argv):
        a = argv[i]
        if a == "--out" and i + 1 < len(argv):
            out_path = argv[i + 1]
            i += 2
            continue
        if a.startswith("--machine="):
            machine = a.split("=", 1)[1]
        elif a.startswith("--dir="):
            directory = Path(a.split("=", 1)[1])
        else:
            print("usage: python tools/lost_optimum_table.py [--machine=NAME] [--dir=DIR] [--out FILE]", file=sys.stderr)
            return 1
        i += 1
    files, machine, refusal = one_machine(sorted(glob.glob(str(directory / "SC3LOST_*.csv"))), machine,
                                          "--machine", "SC3LOST")
    if refusal:
        print(refusal, file=sys.stderr)
        return 1
    if not files:
        print("no SC3LOST file in %s" % directory.as_posix(), file=sys.stderr)
        return 1
    problems = []
    checked = Counter()
    skipped = defaultdict(Counter)
    groups = defaultdict(lambda: defaultdict(list))   # (part, scope) -> method -> rows
    instances = defaultdict(set)
    empty_band = defaultdict(Counter)                  # (part, scope) -> key -> rows, and key + " opt"
    for path in files:
        for r in rows_of(path):
            name = dataset_name(r["dataset"])
            part = "reported only" if name in REPORT_ONLY else "tested datasets"
            scope = r["pattern_kind"]
            if not r["method_key"]:
                reason = r["status"].split(":")[0] if r["status"] else "(no status)"
                skipped[(part, scope)][reason] += 1
                continue
            key = (name, r["gap"], r["sigma"], r["sensitive_set"], r["drop"])
            instances[(part, scope)].add(key)
            if r["status"]:
                problems.append("%s %s: %s" % (key, r["method_key"], r["status"]))
                continue
            masks, least = int(r["mask_count"]), int(r["min_masks"])
            lost, opt, worst = int(r["lost"]), int(r["opt_lost"]), int(r["worst_lost"])
            checked["rows"] += 1
            if masks < least:
                problems.append("%s %s: |R| %d below |R|* %d" % (key, r["method_key"], masks, least))
            if r["method_key"] in CUT_HIDE:
                checked["cut-hide rows"] += 1
                if masks != least:
                    problems.append("%s %s: a CUT-HIDE key off |R|*" % (key, r["method_key"]))
            if masks == least:
                checked["rows at |R|*"] += 1
                if lost < opt:
                    problems.append("%s %s: Lost %d below the optimum %d" % (key, r["method_key"], lost, opt))
            if worst < opt:
                problems.append("%s: greatest Lost %d below the least %d" % (key, worst, opt))
            groups[(part, scope)][r["method_key"]].append((masks, least, lost, opt, worst))
            if r["method_key"] in CUT_HIDE and ("UNIFORM" in r["method"] or "B_eps empty" in r["method"]):
                empty_band[(part, scope)][r["method_key"]] += 1
                empty_band[(part, scope)][r["method_key"] + " opt"] += int(masks == least and lost == opt)

    lines = ["SC3LOST files of %s: %d" % (machine, len(files)),
             "checks covered: %d method rows, %d of them at |R|*, %d CUT-HIDE rows"
             % (checked["rows"], checked["rows at |R|*"], checked["cut-hide rows"])]
    for part, scope in sorted(set(groups) | set(skipped)):
        lines.append("")
        lines.append("== %s, %s scope: %d instances solved; skipped %s"
                     % (part, scope, len(instances[(part, scope)]), dict(skipped[(part, scope)]) or "none"))
        lines.append("   %-15s %5s %9s %9s %8s %8s %9s %9s %12s"
                     % ("method", "n", "|R|=|R|*", "Lost=OPT", "sum exc", "med exc", "med pos", "|R|>|R|*",
                        "opt no worse"))
        for m in METHODS:
            rs = groups[(part, scope)].get(m)
            if not rs:
                continue
            at = [x for x in rs if x[0] == x[1]]
            above = [x for x in rs if x[0] > x[1]]
            excess = [x[2] - x[3] for x in at]
            position = [(x[2] - x[3]) / (x[4] - x[3]) if x[4] > x[3] else 0.0 for x in at]
            optimal = sum(1 for e in excess if e == 0)
            no_worse = sum(1 for x in above if x[3] <= x[2])
            lines.append("   %-15s %5d %4d/%-4d %4d/%-4d %8d %8s %9s %9d %6d/%-5d"
                         % (m, len(rs), len(at), len(rs), optimal, len(at), sum(excess),
                            ("%.1f" % statistics.median(excess)) if excess else "-",
                            ("%.3f" % statistics.median(position)) if position else "-",
                            len(above), no_worse, len(above)))
        e = empty_band[(part, scope)]
        lines.append("   empty B_eps, CUT-HIDE with uniform weights: %s"
                     % ("; ".join("%s %d (Lost = OPT %d)" % (k, e[k], e[k + " opt"]) for k in CUT_HIDE if e[k])
                        or "none"))
    lines.append("")
    if problems:
        lines.append("PROBLEMS (%d):" % len(problems))
        lines += ["  " + p for p in problems[:20]]
    else:
        lines.append("No problem: every method hides, no |R| below |R|*, every CUT-HIDE key at |R|*, no Lost below the "
                     "optimum at |R|*.")
    text = "\n".join(lines) + "\n"
    if out_path:
        with io.open(out_path, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(text)
        print("wrote %s" % Path(out_path).as_posix())
    else:
        sys.stdout.write(text)
    return 1 if problems or not checked["rows"] else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
