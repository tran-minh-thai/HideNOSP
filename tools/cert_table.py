"""Summarise SC3CERT (preregistration section 20.2): the certified lower bound B on the optimum Lost.

    python tools/cert_table.py [--machine=NAME] [--dir=DIR] [--out FILE]

Reads the SC3CERT_*.csv files of one machine in DIR (default: results-invariant/ of the repository), with the
SC3_*.csv and SC3LOST_*.csv files of the same machine, and prints per scope, with protein_sdb1 reported apart
(section 11, point 5):

  - full data: the number of sets, the share certified optimal (Lost = B), the share with B > 0, and the
    median of Lost - B; the denominators of rows with a status (OT, OOM, method failed) are printed;
  - small instances: the number of instances joined with an SC3LOST row solved exactly, the share with
    B = OPT_Lost, and the median of OPT_Lost - B, then the same on the instances with OPT_Lost > 0 (the only
    ones where B <= OPT_Lost can fail); skipped instances by reason, including those SC3LOST did not solve.

Implementation checks that stop the run with exit status 1, each with the number of rows it covered:
  - B <= Lost on every row with a bound;
  - a full-data row has an SC3 row of cut-hide-rw-ls for the same set, with the same |R| and Lost;
  - a small row with a bound has an SC3LOST row of cut-hide-rw-ls for the same set and drop, with the same
    instance threshold and Lost, and B <= OPT_Lost;
  - a row whose status starts with "check failed".
"""
import csv
import glob
import io
import statistics
import sys
from collections import Counter, defaultdict
from pathlib import Path

from lower_bound_table import machine_of, one_machine

REPO = Path(__file__).resolve().parent.parent
METHOD = "cut-hide-rw-ls"
REPORT_ONLY = ("protein_sdb1",)


def rows_of(path):
    with io.open(path, encoding="utf-8", newline="") as handle:
        return list(csv.DictReader([line for line in handle if not line.startswith("#")]))


def dataset_name(path_text):
    return Path(path_text.replace("\\", "/")).stem


def same_machine(directory, pattern, machine):
    return [p for p in sorted(glob.glob(str(directory / pattern))) if machine_of(p) == machine]


def share(k, n):
    return "%d/%d" % (k, n)


def median(values):
    return ("%.1f" % statistics.median(values)) if values else "-"


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
            print("usage: python tools/cert_table.py [--machine=NAME] [--dir=DIR] [--out FILE]", file=sys.stderr)
            return 1
        i += 1
    files, machine, refusal = one_machine(sorted(glob.glob(str(directory / "SC3CERT_*.csv"))), machine,
                                          "--machine", "SC3CERT")
    if refusal:
        print(refusal, file=sys.stderr)
        return 1
    if not files:
        print("no SC3CERT file in %s" % directory.as_posix(), file=sys.stderr)
        return 1

    sc3 = {}
    for path in same_machine(directory, "SC3_*.csv", machine):
        for r in rows_of(path):
            if r["method_key"] == METHOD:
                sc3[(dataset_name(r["dataset"]), r["gap"], r["sigma"], r["sensitive_set"])] = r
    lost_rows = {}
    for path in same_machine(directory, "SC3LOST_*.csv", machine):
        for r in rows_of(path):
            if r["method_key"] in (METHOD, ""):
                # A row without a method key is an instance SC3LOST skipped (its status says why).
                lost_rows[(dataset_name(r["dataset"]), r["gap"], r["sigma"], r["sensitive_set"], r["drop"])] = r

    problems = []
    checked = Counter()
    full = defaultdict(list)       # (part, scope) -> [(lost, bound)]
    small = defaultdict(list)      # (part, scope) -> [(opt, bound)]
    status = defaultdict(Counter)  # (part, scope, instance) -> reason -> count
    for path in files:
        for r in rows_of(path):
            name = dataset_name(r["dataset"])
            group = ("reported only" if name in REPORT_ONLY else "tested datasets", r["pattern_kind"])
            where = "%s %s %s %s %s %s" % (name, r["gap"], r["sigma"], r["sensitive_set"], r["instance"], r["drop"])
            if r["status"].startswith("check failed"):
                problems.append("%s: %s" % (where, r["status"]))
                continue
            if r["status"]:
                status[group + (r["instance"],)][r["status"].split(":")[0]] += 1
                continue
            lost, bound = int(r["lost"]), int(r["bound"])
            checked["rows with a bound"] += 1
            if bound > lost:
                problems.append("%s: bound %d above Lost %d" % (where, bound, lost))
            if r["instance"] == "full":
                same = sc3.get((name, r["gap"], r["sigma"], r["sensitive_set"]))
                if same is None:
                    problems.append("%s: no SC3 row of %s for %s" % (where, METHOD, machine))
                else:
                    checked["full rows joined with SC3"] += 1
                    if (same["mask_count"], same["lost"]) != (r["mask_count"], r["lost"]):
                        problems.append("%s: |R|, Lost %s, %s here, %s, %s in SC3" % (
                            where, r["mask_count"], r["lost"], same["mask_count"], same["lost"]))
                full[group].append((lost, bound))
                continue
            exact = lost_rows.get((name, r["gap"], r["sigma"], r["sensitive_set"], r["drop"]))
            if exact is None:
                problems.append("%s: no SC3LOST row of %s for %s" % (where, METHOD, machine))
                continue
            if not exact["method_key"] or not exact["opt_lost"]:
                reason = exact["status"].split(":")[0] if exact["status"] else "no exact optimum"
                status[group + ("small",)]["SC3LOST: " + reason] += 1
                continue
            checked["small rows joined with SC3LOST"] += 1
            if (exact["instance_sigma"], exact["lost"]) != (r["instance_sigma"], r["lost"]):
                problems.append("%s: threshold, Lost %s, %s here, %s, %s in SC3LOST" % (
                    where, r["instance_sigma"], r["lost"], exact["instance_sigma"], exact["lost"]))
            opt = int(exact["opt_lost"])
            if bound > opt:
                problems.append("%s: bound %d above the exact optimum %d" % (where, bound, opt))
            small[group].append((opt, bound))

    lines = ["SC3CERT files of %s: %d" % (machine, len(files)),
             "checks covered: %d rows with a bound, %d full-data rows joined with SC3, %d small rows joined with "
             "SC3LOST" % (checked["rows with a bound"], checked["full rows joined with SC3"],
                          checked["small rows joined with SC3LOST"])]
    for group in sorted(set(full) | set(small) | {k[:2] for k in status}):
        f, s = full[group], small[group]
        lines.append("")
        lines.append("== %s, %s scope" % group)
        lines.append("   full data: %d sets; certified optimal (Lost = B) %s; B > 0 %s; median Lost - B %s; "
                     "not evaluated %s" % (len(f), share(sum(1 for x in f if x[0] == x[1]), len(f)),
                                           share(sum(1 for x in f if x[1] > 0), len(f)),
                                           median([x[0] - x[1] for x in f]),
                                           dict(status[group + ("full",)]) or "none"))
        positive = [x for x in s if x[0] > 0]
        lines.append("   small instances: %d joined; B = OPT_Lost %s; median OPT_Lost - B %s; skipped %s"
                     % (len(s), share(sum(1 for x in s if x[0] == x[1]), len(s)), median([x[0] - x[1] for x in s]),
                        dict(status[group + ("small",)]) or "none"))
        lines.append("   of them with OPT_Lost > 0 (where B <= OPT_Lost can fail): %d; B = OPT_Lost %s; B > 0 %s"
                     % (len(positive), share(sum(1 for x in positive if x[0] == x[1]), len(positive)),
                        share(sum(1 for x in positive if x[1] > 0), len(positive))))
    lines.append("")
    if problems:
        lines.append("PROBLEMS (%d):" % len(problems))
        lines += ["  " + p for p in problems[:20]]
    else:
        lines.append("No problem: B <= Lost on every row, B <= OPT_Lost on every small instance, every rerun equals "
                     "its SC3 or SC3LOST row.")
    text = "\n".join(lines) + "\n"
    if out_path:
        with io.open(out_path, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(text)
        print("wrote %s" % Path(out_path).as_posix())
    else:
        sys.stdout.write(text)
    return 1 if problems or not checked["rows with a bound"] else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
