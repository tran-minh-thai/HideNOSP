# SPDX-License-Identifier: MIT
"""Summarise the reduced SC6 (preregistration sections 19.3 and 20.1): running time of every method, cost of
the index, and the time of mining F_sigma(D), printed beside the methods and never added to them.

    python tools/timing_table.py [--machine=NAME] [--dir=DIR] [--out FILE]

Reads the SC6_*.csv files of one machine in DIR/results (default: the repository) and prints, per scope
and size of the sensitive set, the median over the timed sets of each method's mean wall-clock time, with
the number of sets; the median over the cells of the time of mining F_sigma(D) (the preprocessing that the
methods reading it share, timed once per cell on the row mine-frequent); and for the index the median build
time and the median ratio of rebuilding after each mask to the incremental update.

Checks that stop the run with exit status 1, each with the number of rows it covered:
  - a row with a status (OT, OOM, method failed, does not hide) is printed and counted, not hidden;
  - each file holds exactly one mine-frequent row, before every method row (section 20.1);
  - |R| and Lost of a timed run equal the SC3 row of the same set and method on the same machine
    (results-invariant/SC3_*.csv): the timed code is the code of the comparison tables.
"""
import csv
import glob
import io
import statistics
import sys
from collections import defaultdict
from pathlib import Path

from lower_bound_table import machine_of, one_machine

REPO = Path(__file__).resolve().parent.parent
READS = ("cut-hide", "cut-hide-rw", "cut-hide-rw-ls", "sbsh-r1", "sbsh-rmax")
METHODS = ("cut-hide-rw-ls", "cut-hide-rw", "cut-hide", "hha", "hha-stop", "dbsh", "sbsh-r1", "sbsh-rmax",
           "occurrence")


def rows_of(path):
    with io.open(path, encoding="utf-8", newline="") as handle:
        return list(csv.DictReader([line for line in handle if not line.startswith("#")]))


def dataset_name(path_text):
    return Path(path_text.replace("\\", "/")).stem


def main(argv):
    out_path = None
    root = REPO
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
            root = Path(a.split("=", 1)[1])
        else:
            print("usage: python tools/timing_table.py [--machine=NAME] [--dir=DIR] [--out FILE]", file=sys.stderr)
            return 1
        i += 1
    files, machine, refusal = one_machine(sorted(glob.glob(str(root / "results" / "SC6_*.csv"))), machine,
                                          "--machine", "SC6")
    if refusal:
        print(refusal, file=sys.stderr)
        return 1
    if not files:
        print("no SC6 file in %s" % (root / "results").as_posix(), file=sys.stderr)
        return 1
    sc3 = {}
    for path in sorted(glob.glob(str(root / "results-invariant" / "SC3_*.csv"))):
        if machine_of(path) != machine:
            continue
        for r in rows_of(path):
            sc3[(dataset_name(r["dataset"]), r["gap"], r["sigma"], r["sensitive_set"], r["method_key"])] = r

    problems = []
    covered = defaultdict(int)
    times = defaultdict(lambda: defaultdict(list))
    index = defaultdict(lambda: defaultdict(list))
    mining = defaultdict(list)
    for path in files:
        rows = rows_of(path)
        mine_rows = [k for k, r in enumerate(rows) if r["method_key"] == "mine-frequent"]
        covered["files checked for one mining row"] += 1
        if len(mine_rows) != 1 or mine_rows[0] != 0:
            problems.append("%s: %d mine-frequent rows at positions %s, expected one, first"
                            % (Path(path).name, len(mine_rows), mine_rows))
        for r in rows:
            group = (r["pattern_kind"], int(r["sensitive_size"]))
            key = r["method_key"]
            where = "%s %s %s %s %s" % (dataset_name(r["dataset"]), r["gap"], r["sigma"], r["sensitive_set"], key)
            if r["status"]:
                problems.append("%s: %s" % (where, r["status"]))
                continue
            if key == "mine-frequent":
                mining[group].append(float(r["wall_ms_mean"]))
                continue
            if key.startswith("index-"):
                index[group][(dataset_name(r["dataset"]), r["gap"], r["sigma"], r["sensitive_set"])].append(
                    (key, float(r["wall_ms_mean"])))
                continue
            covered["timed rows"] += 1
            same = sc3.get((dataset_name(r["dataset"]), r["gap"], r["sigma"], r["sensitive_set"], key))
            if same is None:
                problems.append("%s: no SC3 row of %s for this set" % (where, machine))
            else:
                covered["joined with SC3"] += 1
                if (same["mask_count"], same["lost"]) != (r["mask_count"], r["lost"]):
                    problems.append("%s: |R|, Lost %s, %s here, %s, %s in SC3" % (where, r["mask_count"], r["lost"],
                                                                               same["mask_count"], same["lost"]))
            times[group][key].append(float(r["wall_ms_mean"]))

    lines = ["SC6 files of %s: %d (one mining row checked in %d); timed rows %d, joined with an SC3 row %d"
             % (machine, len(files), covered["files checked for one mining row"], covered["timed rows"],
                covered["joined with SC3"]),
             "Methods marked * read F_sigma(D); its mining time is the separate line, not part of their times."]
    for group in sorted(set(times) | set(index) | set(mining)):
        lines.append("")
        lines.append("== %s scope, |S| = %d" % group)
        lines.append("   %-15s %5s %16s" % ("method", "sets", "median wall ms"))
        for m in METHODS:
            values = times[group].get(m)
            if values:
                lines.append("   %-15s %5d %16.1f" % (m + ("*" if m in READS else ""), len(values),
                                                     statistics.median(values)))
        if mining[group]:
            lines.append("   %-15s %5d %16.1f   (cells, not sets)" % ("mine F_sigma", len(mining[group]),
                                                                       statistics.median(mining[group])))
        build, ratio = [], []
        for parts in index[group].values():
            d = dict(parts)
            if "index-build" in d:
                build.append(d["index-build"])
            if d.get("index-incremental", 0) > 0 and "index-rebuild" in d:
                ratio.append(d["index-rebuild"] / d["index-incremental"])
        if build:
            lines.append("   index: build median %.1f ms over %d sets; rebuild / incremental median %.2f over %d sets"
                         % (statistics.median(build), len(build), statistics.median(ratio) if ratio else float("nan"),
                            len(ratio)))
    lines.append("")
    if problems:
        lines.append("PROBLEMS (%d):" % len(problems))
        lines += ["  " + p for p in problems[:30]]
    else:
        lines.append("No problem: every timed run finished, F_sigma(D) is timed once per cell, and every "
                     "timed run equals its SC3 row.")
    text = "\n".join(lines) + "\n"
    if out_path:
        with io.open(out_path, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(text)
        print("wrote %s" % Path(out_path).as_posix())
    else:
        sys.stdout.write(text)
    return 1 if problems or not covered["timed rows"] else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
