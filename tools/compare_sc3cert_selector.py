"""Compare the small-instance rows of SC3CERT across the empty-band selector of CUT-HIDE.

    python tools/compare_sc3cert_selector.py OLD_DIR OLD_MACHINE NEW_DIR NEW_MACHINE

OLD is a run of both parts made before CutHide.forBand existed; NEW a run made with it and --parts=small
(preregistration section 19.2, correction of 2026-11-06 for SC3CERT). OLD_DIR must also hold the SC3LOST files
of OLD_MACHINE, made before the selector: they name the instances where CUT-HIDE refused because B_eps was
empty. Cells are matched on the options line of the file header, without the machine and without parts.
Expected, and checked exactly with no tolerance:
  - NEW holds no full-data row, and as many small rows as OLD, in the same order;
  - every small row equal, column by column, except the rows OLD refused with "B_eps is empty";
  - on those rows NEW has no status, Lost = B = 0, certified = true, a mask count, the instance is one
    where SC3LOST refused CUT-HIDE for the same reason, and every other column equals OLD.
Prints the denominators and exits 1 on any difference, or when no row or no refused row was compared.
"""
import csv
import glob
import io
import re
import sys
from collections import Counter
from pathlib import Path

REFUSAL = "method failed: B_eps is empty"
CHANGED = ("mask_count", "lost", "bound", "examined", "certified", "status")


def read(path):
    text = io.open(path, encoding="utf-8", newline="").read().splitlines()
    options = [line for line in text if line.startswith("# options ")][0]
    options = re.sub(r"machine=[^,}]*, ", "", options)
    options = re.sub(r", parts=[^,}]*", "", options)
    return options, list(csv.DictReader([line for line in text if not line.startswith("#")]))


def load(directory, scenario, machine):
    out = {}
    for path in sorted(glob.glob(str(Path(directory) / ("%s_*_%s_*.csv" % (scenario, machine))))):
        options, rows = read(path)
        if options in out:
            raise SystemExit("two files for one cell: " + Path(path).as_posix())
        out[options] = rows
    return out


def instance(r):
    return (Path(r["dataset"].replace("\\", "/")).stem, r["gap"], r["sigma"], r["sensitive_set"], r["drop"])


def main(old_dir, old_machine, new_dir, new_machine):
    old = load(old_dir, "SC3CERT", old_machine)
    new = load(new_dir, "SC3CERT", new_machine)
    refused_lost = {instance(r) for rows in load(old_dir, "SC3LOST", old_machine).values() for r in rows
                    if r["status"].startswith(REFUSAL)}
    problems, n = [], Counter()
    n["SC3LOST instances refused before the selector"] = len(refused_lost)
    only_old, only_new = set(old) - set(new), set(new) - set(old)
    if only_old or only_new:
        problems.append("cells differ: %d only old, %d only new" % (len(only_old), len(only_new)))
    for cell in sorted(set(old) & set(new)):
        n["cells"] += 1
        a = [r for r in old[cell] if r["instance"] == "small"]
        b = new[cell]
        if any(r["instance"] != "small" for r in b):
            problems.append("%s: the new file holds a full-data row" % cell)
            continue
        if len(a) != len(b):
            problems.append("%s: %d small rows old, %d new" % (cell, len(a), len(b)))
            continue
        for ra, rb in zip(a, b):
            key = instance(ra)
            if key != instance(rb):
                problems.append("%s: row order differs at %s" % (cell, key))
                break
            n["small rows"] += 1
            if ra["status"].startswith(REFUSAL):
                n["refused rows"] += 1
                same = {k: ra[k] for k in ra if k not in CHANGED} == {k: rb[k] for k in rb if k not in CHANGED}
                ok = (same and not rb["status"] and rb["mask_count"] != "" and rb["lost"] == "0"
                      and rb["bound"] == "0" and rb["certified"] == "true" and key in refused_lost)
                n["refused rows now Lost = B = 0, certified, on a refused SC3LOST instance"] += ok
                if not ok:
                    problems.append("%s %s: refused row now %s (instance refused in SC3LOST: %s)"
                                    % (cell, key, dict(rb), key in refused_lost))
            elif ra != rb:
                problems.append("%s %s: columns differ %s" % (cell, key, [k for k in ra if ra[k] != rb[k]]))
            else:
                n["identical rows"] += 1
    for k, v in n.items():
        print("%-75s %d" % (k, v))
    print("PROBLEMS %d" % len(problems))
    for p in problems[:15]:
        print("  " + p[:300])
    return 1 if problems or not n["small rows"] or not n["refused rows"] else 0


if __name__ == "__main__":
    if len(sys.argv) != 5:
        print(__doc__.strip().splitlines()[2].strip(), file=sys.stderr)
        sys.exit(2)
    sys.exit(main(*sys.argv[1:5]))
