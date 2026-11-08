# SPDX-License-Identifier: MIT
"""Split the distance between CUT-HIDE and the lower bound into a part of the bound and a part of the method.

    python tools/reduced_optimum_table.py [DIR ...] [--machine=NAME] [--out FILE]

Reads the SC3OPT_*.csv files of one machine under the given directories (default: results-invariant/
of the repository); --machine names the machine of the run stamp, and may be left out only when the
files come from one machine. It prints, per scope, size of the sensitive set and requested number of
sequences per instance:

  - how many instances were solved, and why the others were skipped (the denominators);
  - medians of greedy / bound, refined / bound, optimum / bound, refined / optimum, where the bound is
    the conflict bound and "refined" is the greedy of step 7 followed by the refinement;
  - how often the optimum equals the bound and how often the refined method equals the optimum;
  - the share of the distance due to the bound: sum log(optimum / bound) over sum log(refined / bound),
    over the instances where refined > bound (a share near 1 means the bound is what is loose).

Exits with status 1 when a run left a .partial file or an instance breaks bound <= optimum <= refined <= greedy.
"""
import csv
import glob
import io
import math
import statistics
import sys
from collections import Counter, defaultdict
from pathlib import Path

import closure_rule  # noqa: E402
from lower_bound_table import one_machine

REPO = Path(__file__).resolve().parent.parent


def rows_of(path):
    with io.open(path, encoding="utf-8", newline="") as handle:
        return list(csv.DictReader([line for line in handle if not line.startswith("#")]))


def median(values):
    return statistics.median(values) if values else float("nan")


def main(argv):
    out_path = None
    machine = ""
    directories = []
    rule = "necessary"
    i = 0
    while i < len(argv):
        if argv[i] == "--out" and i + 1 < len(argv):
            out_path = argv[i + 1]
            i += 2
        elif argv[i].startswith("--machine="):
            machine = argv[i].split("=", 1)[1]
            i += 1
        elif argv[i].startswith("--closure=") and argv[i].split("=", 1)[1] in closure_rule.RULES:
            rule = argv[i].split("=", 1)[1]
            i += 1
        else:
            directories.append(Path(argv[i]))
            i += 1
    if not directories:
        directories = [REPO / "results-invariant"]
    files = []
    partial = []
    for d in directories:
        files += sorted(glob.glob(str(d / "**" / "SC3OPT_*.csv"), recursive=True))
        partial += sorted(glob.glob(str(d / "**" / "SC3OPT_*.csv.partial"), recursive=True))
    files, machine, refusal = one_machine(files, machine, "--machine", "SC3OPT")
    files, found = closure_rule.keep(files, rule)
    if refusal:
        print(refusal, file=sys.stderr)
        return 1
    # A run that stopped early leaves its rows in a .partial file (CsvWriter); count it, never read it.
    problems = ["incomplete run: %s" % Path(p).name for p in partial]
    groups = defaultdict(list)
    skipped = defaultdict(Counter)
    for path in files:
        for r in rows_of(path):
            g = (r["pattern_kind"], int(r["sensitive_size"]), int(r["requested_sequences"]))
            if r["status"]:
                skipped[g][r["status"]] += 1
                continue
            b, o, f, gr = (int(r[k]) for k in ("conflict_bound", "optimum", "refined", "greedy"))
            if not b <= o <= f <= gr:
                problems.append("order broken on %s %s: %d %d %d %d" % (r["dataset"], r["sensitive_set"], b, o, f, gr))
            groups[g].append(r)

    lines = [closure_rule.describe("SC3OPT", rule, files, found), "SC3OPT files: %d, machine %s" % (len(files), machine)]
    for g in sorted(set(groups) | set(skipped)):
        rows = groups[g]
        lines.append("")
        lines.append("== %s scope, |S| = %d, instances of %d sequences: %d solved, %d skipped %s"
                     % (g[0], g[1], g[2], len(rows), sum(skipped[g].values()), dict(skipped[g])))
        if not rows:
            continue
        val = [(int(r["conflict_bound"]), int(r["optimum"]), int(r["refined"]), int(r["greedy"])) for r in rows]
        pos = [v for v in val if v[0] > 0]
        lines.append("   weights: %s; instances with a window: %d; binding patterns median %s; optimum median %s"
                     % (dict(Counter(r["weights"] for r in rows)), sum(int(r["windows"]) > 0 for r in rows),
                        median([int(r["binding_patterns"]) for r in rows]), median([v[1] for v in val])))
        lines.append("   median ratio to the bound: greedy %.4f, refined %.4f, optimum %.4f (over %d with bound > 0)"
                     % (median([v[3] / v[0] for v in pos]), median([v[2] / v[0] for v in pos]),
                        median([v[1] / v[0] for v in pos]), len(pos)))
        lines.append("   median ratio to the optimum: greedy %.4f, refined %.4f"
                     % (median([v[3] / v[1] for v in val]), median([v[2] / v[1] for v in val])))
        lines.append("   optimum = bound: %d/%d; refined = optimum: %d/%d; greedy = optimum: %d/%d"
                     % (sum(v[1] == v[0] for v in val), len(val), sum(v[2] == v[1] for v in val), len(val),
                        sum(v[3] == v[1] for v in val), len(val)))
        apart = [v for v in pos if v[2] > v[0]]
        if apart:
            share = sum(math.log(v[1] / v[0]) for v in apart) / sum(math.log(v[2] / v[0]) for v in apart)
            lines.append("   share of log(refined / bound) due to the bound, over %d instances with refined > bound: %.4f"
                         % (len(apart), share))
    lines.append("")
    if problems:
        lines.append("PROBLEMS (%d):" % len(problems))
        lines += ["  " + p for p in problems[:20]]
    else:
        lines.append("No problem: every file complete, bound <= optimum <= refined <= greedy on every instance.")
    text = "\n".join(lines) + "\n"
    if out_path:
        with io.open(out_path, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(text)
        print("wrote %s" % Path(out_path).as_posix())
    else:
        sys.stdout.write(text)
    return 1 if problems or not files else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
