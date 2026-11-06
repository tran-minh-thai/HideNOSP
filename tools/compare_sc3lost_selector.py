"""Compare the SC3LOST rows of two runs, cell by cell, across the empty-band selector of CUT-HIDE.

    python tools/compare_sc3lost_selector.py OLD_DIR OLD_MACHINE NEW_DIR NEW_MACHINE

OLD is a run made before CutHide.forBand existed, NEW a run made with it (preregistration section 19.2,
correction of 2026-11-06). Cells are matched on the options line of the file header, without the machine.
Expected, and checked exactly with no tolerance:
  - every row equal, column by column, except the rows where OLD has a CUT-HIDE key refused with
    "B_eps is empty";
  - on those rows NEW has no status, |R| = |R|*, Lost = OPT_Lost, a method column naming the uniform
    weighting, and every column other than method, mask_count, lost and status equal to OLD.
Prints the denominators (cells, rows, identical rows, refused rows, refused rows that now meet the
expectation) and exits 1 on any difference or when no row was compared.
"""
import csv, glob, io, re, sys
from collections import Counter

def load(directory, machine):
    out = {}
    for p in sorted(glob.glob(directory + "/SC3LOST_*_%s_*.csv" % machine)):
        text = io.open(p, encoding="utf-8", newline="").read().splitlines()
        opts = [l for l in text if l.startswith("# options ")][0]
        opts = re.sub(r"machine=[^,}]*, ", "", opts)
        rows = list(csv.DictReader([l for l in text if not l.startswith("#")]))
        assert opts not in out, "two files for one cell: " + p
        out[opts] = rows
    return out

def main(old_dir, old_m, new_dir, new_m):
    old, new = load(old_dir, old_m), load(new_dir, new_m)
    problems, n = [], Counter()
    if set(old) != set(new):
        problems.append("cells differ: %d only old, %d only new" % (len(set(old) - set(new)), len(set(new) - set(old))))
    for cell in sorted(set(old) & set(new)):
        a, b = old[cell], new[cell]
        n["cells"] += 1
        if len(a) != len(b):
            problems.append("%s: %d rows old, %d new" % (cell, len(a), len(b)))
            continue
        for ra, rb in zip(a, b):
            key = [ra[k] for k in ("dataset", "gap", "sigma", "sensitive_set", "drop", "method_key")]
            if key != [rb[k] for k in ("dataset", "gap", "sigma", "sensitive_set", "drop", "method_key")]:
                problems.append("%s: row order differs at %s" % (cell, key)); break
            n["rows"] += 1
            if ra["status"].startswith("method failed: B_eps is empty"):
                n["refused rows"] += 1
                same = {k: ra[k] for k in ra if k not in ("method", "mask_count", "lost", "status")} == \
                       {k: rb[k] for k in rb if k not in ("method", "mask_count", "lost", "status")}
                ok = (same and not rb["status"] and rb["mask_count"] == rb["min_masks"] and rb["lost"] == rb["opt_lost"]
                      and ("UNIFORM" in rb["method"] or "B_eps empty" in rb["method"]))
                n["refused rows now |R|* and Lost = OPT, named uniform"] += ok
                if not ok:
                    problems.append("%s %s: refused row now %s" % (cell, key, dict(rb)))
            elif ra != rb:
                diff = [k for k in ra if ra[k] != rb[k]]
                problems.append("%s %s: columns differ %s" % (cell, key, diff))
            else:
                n["identical rows"] += 1
    for k, v in n.items():
        print("%-50s %d" % (k, v))
    print("PROBLEMS %d" % len(problems))
    for p in problems[:15]:
        print("  " + p[:300])
    return 1 if problems or not n["rows"] else 0

if __name__ == "__main__":
    sys.exit(main(*sys.argv[1:5]))
