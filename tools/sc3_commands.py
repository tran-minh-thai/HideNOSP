"""Print the SC3 commands that are still missing, from the two locked group tables.

Why a generator and not a hand-written list. The cells of SC3 are the main-group rows of
config/configuration_groups.tsv (pool scope, sensitive patterns without repeated items) and of
config/configuration_groups_repeated.tsv (repeated-item scope), at one size of the sensitive set.
A cell is done when results-invariant/ holds a complete SC3 file whose options line names that
cell. Both lists change as results come in, so the commands are derived from them each time instead
of being copied by hand.

Usage:

    python tools/sc3_commands.py                 commands for |S| = 1 still missing
    python tools/sc3_commands.py --size=5        the same for |S| = 5
    python tools/sc3_commands.py --pareto        the Pareto-front batch (|S| = 1, no method)
    python tools/sc3_commands.py --self-test     checks of the matching rules, with planted faults

What is fixed here (preregistration section 14): the seven methods of the H2 set, the time limit
of 1800 s per method and sensitive set, -Xmx8g, l_max = 4, 4 sets per stratum, and the machine label
of the measuring machine. The Pareto front (|S| = 1, descriptive) is a batch of its own: a dry run
of every cell found that it takes most of the time of a cell and is not under the time limit, so
the method batches run without it (--pareto=false) and the front batch runs --methods=none. The commands are ordered by the
size of the candidate pool, smallest first, so the first command is short and shows whether the
result file is written before a long one starts.

Two-way check. A cell with a file is not printed; a complete SC3 file that matches no planned cell
is listed apart, because it is either a cell run with other options or a run outside this plan.
A .partial file never counts as done.

Exit status: 0 when the commands are printed, 1 when a table or a file cannot be read, 2 when the
self-test fails.
"""
import csv
import io
import os
import re
import shutil
import sys
import tempfile
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

METHODS = "cut-hide-rw-ls,cut-hide-rw,cut-hide,hha,dbsh,sbsh-r1,sbsh-rmax"
TIME_LIMIT = "1800"
HEAP = "-Xmx8g"
MACHINE = "DESKTOP-L8A6EPO"
SIZES = (1, 5, 10, 20)

TABLES = (
    ("config/configuration_groups.tsv", "distinct"),
    ("config/configuration_groups_repeated.tsv", "repeated"),
)

# The options that name a cell. A file with the same values is the same cell; any other option
# (machine, seed default, root) does not change what the cell is.
IDENTITY = ("dataset", "format", "sigma", "gmin", "gmax", "maxPatternLength", "setsPerStratum",
            "sensitiveSize", "patternKind", "pareto", "timeLimit", "methods")


def read_table(path, kind):
    """Main-group rows of one locked table, as cell dictionaries."""
    cells = []
    with io.open(path, encoding="utf-8", newline="") as handle:
        lines = [line for line in handle if not line.startswith("#")]
    for row in csv.DictReader(lines, delimiter="\t"):
        row = {k.strip(): (v or "").strip() for k, v in row.items()}
        if row["group"] != "main":
            continue
        match = re.fullmatch(r"\[(\d+),(\d+)\]", row["gap"])
        if not match:
            raise ValueError("%s: cannot read the gap %r" % (Path(path).as_posix(), row["gap"]))
        cells.append({
            "dataset": "datasets/%s.txt" % row["dataset"],
            "format": "SPMF",
            "sigma": row["sigma"],
            "gmin": match.group(1),
            "gmax": match.group(2),
            "pool": int(row["pool"]),
            "patternKind": kind,
        })
    return cells


def strata(pool):
    """Sizes of the five strata of hidenosp.exp.SensitiveSampling for a pool of this size."""
    return [(k + 1) * pool // 5 - k * pool // 5 for k in range(5)]


def absent(root, size):
    """Main-group cells with no set of this size: no stratum holds that many patterns, so SC3
    would stop with "the cell does not exist". The pool column of the locked tables equals the pool
    SC3 draws from (checked on the 54 official files of |S| = 1, 2026-10-04)."""
    out = []
    for name, kind in TABLES:
        for cell in read_table(root / name, kind):
            if max(strata(cell["pool"])) < size:
                out.append(cell)
    out.sort(key=lambda c: (c["pool"], c["patternKind"], c["dataset"], c["gmax"], int(c["sigma"])))
    return out


def planned(root, size, front=False):
    if front and size != 1:
        raise ValueError("the Pareto front exists for |S| = 1 only")
    cells = []
    for name, kind in TABLES:
        for cell in read_table(root / name, kind):
            if max(strata(cell["pool"])) < size:
                continue
            cell.update({
                "maxPatternLength": "4",
                "setsPerStratum": "4",
                "sensitiveSize": str(size),
                "pareto": "true" if front else "false",
                "timeLimit": TIME_LIMIT,
                "methods": "none" if front else METHODS,
            })
            cells.append(cell)
    cells.sort(key=lambda c: (c["pool"], c["patternKind"], c["dataset"], c["gmax"], int(c["sigma"])))
    return cells


def parse_options(line):
    """The options line of a result header: '# options {a=1, b=x,y, c=z}'."""
    body = line.strip()[len("# options "):].strip()
    if not (body.startswith("{") and body.endswith("}")):
        raise ValueError("options line without braces: %r" % line.strip())
    body = body[1:-1]
    out = {}
    for part in re.split(r", (?=[A-Za-z]+=)", body):
        if "=" in part:
            key, value = part.split("=", 1)
            out[key.strip()] = value.strip()
    return out


def existing(root):
    """Complete SC3 files of results-invariant/: (file name, options)."""
    directory = root / "results-invariant"
    files = []
    if not directory.is_dir():
        return files
    for path in sorted(directory.iterdir()):
        if not (path.name.startswith("SC3_") and path.name.endswith(".csv")):
            continue
        scenario, options = None, None
        with io.open(path, encoding="utf-8", newline="") as handle:
            for line in handle:
                line = line.rstrip("\r\n")
                if not line.startswith("#"):
                    break
                if line.startswith("# scenario "):
                    scenario = line[len("# scenario "):].strip()
                elif line.startswith("# options ") and options is None:
                    options = parse_options(line)
        if scenario != "SC3" or options is None:
            continue
        files.append((path.name, options))
    return files


def matches(cell, options):
    for key in IDENTITY:
        given = options.get(key)
        if key == "dataset" and given is not None:
            given = given.replace("\\", "/")
        if key == "pareto" and given is None:
            given = "false"
        if given != cell[key]:
            return False
    return True


def command(cell):
    args = ["python", "tools/run.py", "--jvm=" + HEAP, "SC3", "--machine=" + MACHINE]
    args += ["--%s=%s" % (key, cell[key]) for key in IDENTITY]
    # Quoted when it holds a comma: PowerShell may read an unquoted a,b as an array. Double quotes
    # pass the argument whole in PowerShell 5.1 and 7, cmd and bash alike.
    return " ".join('"%s"' % a if "," in a else a for a in args)


def plan(root, size, front=False):
    cells = planned(root, size, front)
    files = existing(root)
    todo, done = [], []
    used = set()
    for cell in cells:
        hit = [name for name, options in files if matches(cell, options)]
        if hit:
            done.append((cell, hit))
            used.update(hit)
        else:
            todo.append(cell)
    # A file of another size, or of the other batch, belongs to another plan, not to this one.
    stray = [name for name, options in files
             if name not in used and options.get("sensitiveSize") == str(size)
             and (options.get("methods") == "none") == front]
    return cells, todo, done, stray


def describe(cell):
    return "%s %s [%s,%s] sigma=%s pool=%d" % (cell["patternKind"], Path(cell["dataset"]).stem,
                                               cell["gmin"], cell["gmax"], cell["sigma"], cell["pool"])


def main(argv):
    size, front = 1, False
    for arg in argv:
        if arg == "--self-test":
            return self_test()
        if arg == "--pareto":
            front = True
            continue
        match = re.fullmatch(r"--size=(\d+)", arg)
        if match and int(match.group(1)) in SIZES:
            size = int(match.group(1))
        else:
            print("unknown argument %s; use --size=1|5|10|20, --pareto or --self-test" % arg, file=sys.stderr)
            return 1
    try:
        cells, todo, done, stray = plan(REPO, size, front)
    except (OSError, ValueError, KeyError) as error:
        print("cannot build the plan: %s" % error, file=sys.stderr)
        return 1
    kinds = {k: sum(1 for c in cells if c["patternKind"] == k) for _, k in TABLES}
    print("# SC3 %s |S|=%d, main group: %d cells (%d pool scope, %d repeated-item scope), %d done, %d to run"
          % ("Pareto front" if front else "methods", size, len(cells), kinds["distinct"], kinds["repeated"], len(done), len(todo)))
    print("# repository root: %s" % REPO.name)
    for cell in absent(REPO, size):
        print("# no set of size %d: %s, largest stratum %d" % (size, describe(cell), max(strata(cell["pool"]))))
    for cell, names in done:
        print("# done: %s  <- %s" % (describe(cell), ", ".join(names)))
    for name in stray:
        print("# WARNING: %s is an SC3 file of |S|=%d that matches no planned cell" % (name, size))
    for cell in todo:
        print("# %s" % describe(cell))
        print(command(cell))
    return 0


# ---------------------------------------------------------------- self-test

def self_test():
    """Builds a small repository in a temporary folder and checks each rule against it."""
    checks, failures = 0, []

    def check(name, ok, detail=""):
        nonlocal checks
        checks += 1
        print("%-66s %s  %s" % (name, "ok" if ok else "FAILED", detail))
        if not ok:
            failures.append(name)

    root = Path(tempfile.mkdtemp(prefix="sc3plan"))
    try:
        (root / "config").mkdir()
        (root / "results-invariant").mkdir()
        header = "dataset\tgap\tsigma\tpool\trho_rep_median\tbeta_median\tgroup\tbeta_group\n"
        (root / "config/configuration_groups.tsv").write_text(
            "# test\n" + header
            + "alpha\t[0,1]\t10\t5\t0.5\t0.5\tmain\teffective\n"
            + "alpha\t[0,3]\t10\t50\t0.1\t0.5\tintermediate\teffective\n"
            + "beta\t[0,10]\t20\t2\t0.9\t0.0\tmain\tdegenerate\n", encoding="utf-8")
        (root / "config/configuration_groups_repeated.tsv").write_text(
            header + "alpha\t[0,1]\t12\t7\t0.5\t0.5\tmain\teffective\r\n"
            + "gamma\t[0,3]\t30\t3\t0.01\t0.3\tdegenerate\teffective\r\n", encoding="utf-8")

        cells, todo, done, stray = plan(root, 1)
        check("only main rows are planned (2 pool + 1 repeated)", len(cells) == 3, str(len(cells)))
        check("CRLF table is read", any(c["patternKind"] == "repeated" for c in cells))
        check("smallest pool first", [c["pool"] for c in cells] == [2, 5, 7],
              str([c["pool"] for c in cells]))
        check("nothing done in an empty results directory", len(todo) == 3 and not done)
        first = command(cells[0])
        check("command carries limit, methods, heap, machine and Pareto",
              all(s in first for s in ("--timeLimit=1800", "--methods=" + METHODS, "--jvm=-Xmx8g",
                                       "--machine=" + MACHINE, "--pareto=false", "--patternKind=distinct",
                                       "--dataset=datasets/beta.txt", "--gmax=10")), first)
        check("an argument with a comma is quoted", '"--methods=' + METHODS + '"' in first, first)
        check("strata as in SensitiveSampling", strata(7) == [1, 1, 2, 1, 2] and strata(25) == [5] * 5,
              str((strata(7), strata(25))))
        five = planned(root, 5)
        check("|S|=5 keeps a pool of 5 or more per stratum, drops pools 2 and 7",
              [c["pool"] for c in five] == [] and [c["pool"] for c in absent(root, 5)] == [2, 5, 7],
              str(([c["pool"] for c in five], [c["pool"] for c in absent(root, 5)])))
        check("|S|=2 keeps pool 7 and drops pools 2 and 5",
              [c["pool"] for c in planned(root, 2)] == [7] and [c["pool"] for c in absent(root, 2)] == [2, 5],
              str([c["pool"] for c in planned(root, 2)]))
        front = command(planned(root, 1, True)[0])
        check("the front batch runs no method", "--methods=none" in front and "--pareto=true" in front, front)
        try:
            planned(root, 5, True)
            check("no front batch for |S|=5", False)
        except ValueError:
            check("no front batch for |S|=5", True)

        def write(name, cell, change=None, drop=None, scenario="SC3", crlf=False):
            options = {key: cell[key] for key in IDENTITY}
            options["machine"] = MACHINE
            if change:
                options.update(change)
            if drop:
                options.pop(drop)
            line = "# options {" + ", ".join("%s=%s" % kv for kv in options.items()) + "}"
            end = "\r\n" if crlf else "\n"
            text = end.join(["# run x", "# scenario " + scenario, line, "dataset,status", "a,"]) + end
            (root / "results-invariant" / name).write_bytes(text.encode("utf-8"))

        write("SC3_a.csv", cells[0], crlf=True)
        cells, todo, done, stray = plan(root, 1)
        check("a matching CRLF file marks its cell done", len(done) == 1 and len(todo) == 2,
              "%d done" % len(done))

        planted = [
            ("a .partial file does not count", "SC3_b.csv.partial", {}, None, "SC3", False),
            ("another time limit does not count", "SC3_c.csv", {"timeLimit": "600"}, None, "SC3", True),
            ("another method set does not count", "SC3_d.csv", {"methods": "cut-hide"}, None, "SC3", True),
            ("another scope does not count", "SC3_e.csv", {"patternKind": "repeated"}, None, "SC3", True),
            ("a file without timeLimit does not count", "SC3_f.csv", {}, "timeLimit", "SC3", True),
            ("an SC2 file does not count", "SC3_g.csv", {}, None, "SC2", False),
        ]
        for name, file_name, change, drop, scenario, is_stray in planted:
            write(file_name, cells[1], change=change, drop=drop, scenario=scenario)
            _, todo2, done2, stray2 = plan(root, 1)
            still = any(describe(c) == describe(cells[1]) for c in todo2)
            check(name, still and len(done2) == 1, "%d done" % len(done2))
            if is_stray:
                check(name + ": listed as unmatched", file_name in stray2, str(stray2))
            (root / "results-invariant" / file_name).unlink()

        write("SC3_i.csv", cells[2])
        _, todo4, done4, _ = plan(root, 1)
        check("a repeated-scope file marks only the repeated cell",
              len(done4) == 2 and any(describe(c) == describe(cells[2]) for c, _ in done4)
              and any(describe(c) == describe(cells[1]) for c in todo4), "%d done" % len(done4))
        (root / "results-invariant" / "SC3_i.csv").unlink()

        fronts = planned(root, 1, True)
        write("SC3_j.csv", fronts[0])
        _, _, done5, stray5 = plan(root, 1)
        _, todo6, done6, _ = plan(root, 1, True)
        check("a front file is not a method cell, nor stray there", len(done5) == 1 and not stray5,
              "%d done, stray %s" % (len(done5), stray5))
        check("a front file marks its front cell done", len(done6) == 1 and len(todo6) == 2,
              "%d done" % len(done6))
        (root / "results-invariant" / "SC3_j.csv").unlink()

        write("SC3_h.csv", cells[1], change={"dataset": "datasets\\" + Path(cells[1]["dataset"]).name})
        _, todo3, done3, _ = plan(root, 1)
        check("a Windows path in the options matches", len(done3) == 2, "%d done" % len(done3))
    finally:
        shutil.rmtree(root, ignore_errors=True)
    print("%d checks, %d failed" % (checks, len(failures)))
    return 2 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
