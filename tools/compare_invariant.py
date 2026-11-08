# SPDX-License-Identifier: MIT
"""Compare the data rows of two machine-invariant result files, exactly.

Used to check that a new machine or a new JDK reproduces a committed file: same code, same data,
same arguments must give the same rows. The comparison is exact, with no tolerance, because a
difference of 1e-12 already means that the computation took another path.

Two differences are normalised and nothing else, and both are reported when they occur:

- line endings (CRLF from Windows, LF elsewhere);
- the path separator inside the dataset column (datasets\\x.txt on Windows, datasets/x.txt
  elsewhere).

Header lines starting with '#' (run stamp, host, JDK, heap) are skipped: they describe the run,
not its result.

Usage:

    python tools/compare_invariant.py <committed file> <new file>
    python tools/compare_invariant.py <committed file> <directory>
    python tools/compare_invariant.py --machine=<host> [--ref=<git ref>] [--dir=<directory>]

Given a directory, the newest file in it with the same scenario prefix and the same first three
columns in its first data row (dataset, gap, sigma for SC2) is compared, other than the committed
file itself. "Newest" is read from the run stamp in the file name, not from the modification time,
which a fresh clone sets to the checkout time for every file. The chosen file is printed.

Exit code 0 when every data row is identical, 1 when any row differs or nothing could be compared,
2 on a usage error.

Campaign mode (--machine) checks every result file of one machine at once. The new files are the
files of results-invariant/ (or --dir) whose run was made on <host>, read from the working tree or,
with --ref, from that git ref without checking it out (a campaign branch that is not merged yet).
Each is paired with the committed file of the same scenario and the same options (the "# options"
header without machine=), made on another host; when several qualify, the newest run stamp wins.
The host of a file is its "# host" header, not the machine label in its name: a label can be set
by hand, the header is written by the run itself.

Rows where either file has a non-empty status column (OT, OOM) are not compared: the time limit
binds differently on another machine. They are listed, with both statuses. The same two
normalisations as above apply. Per scenario it prints the files, the hosts of the counterparts,
the rows compared and differing, and the files with no counterpart (NOT CHECKED).

Exit code 1 when a header, a row count or a compared row differs, or when no row was compared at
all; files with no counterpart are reported but do not fail the check.
"""
import collections
import csv
import io
import os
import re
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent


def data_rows(path):
    """Return (rows, saw_crlf, saw_backslash) for the non-header lines of a result file."""
    with open(path, "rb") as f:
        return rows_of_text(f.read().decode("utf-8"))


def rows_of_text(raw):
    """data_rows on the text of a file."""
    saw_crlf = "\r\n" in raw
    rows = []
    saw_backslash = False
    for line in raw.replace("\r\n", "\n").split("\n"):
        if not line or line.startswith("#"):
            continue
        if "\\" in line:
            saw_backslash = True
            line = line.replace("\\", "/")
        rows.append(line)
    return rows, saw_crlf, saw_backslash


def configuration(rows):
    """The first three cells of the first data row, or None."""
    return tuple(next(csv.reader([rows[1]]))[:3]) if len(rows) > 1 else None


def newest_matching(reference, directory):
    """The newest file in directory with the reference's scenario and configuration, or None."""
    prefix = os.path.basename(reference).split("_", 1)[0] + "_"
    wanted = configuration(data_rows(reference)[0])
    candidates = []
    for entry in os.listdir(directory):
        path = os.path.join(directory, entry)
        if (not entry.startswith(prefix) or not entry.endswith(".csv")
                or os.path.abspath(path) == os.path.abspath(reference)):
            continue
        if configuration(data_rows(path)[0]) == wanted:
            candidates.append(entry)
    if not candidates:
        print("NOT CHECKED: no %s file in %s has the configuration %s"
              % (prefix.rstrip("_"), directory, ",".join(wanted or ())))
        return None
    chosen = os.path.join(directory, sorted(candidates)[-1])
    print("comparing with the newest of %d matching file(s): %s" % (len(candidates), chosen))
    return chosen


def header_of(raw):
    """The '# key value' lines of a result file, first occurrence of each key."""
    head = {}
    for line in raw.replace("\r\n", "\n").split("\n"):
        if line.startswith("# "):
            k, _, v = line[2:].partition(" ")
            head.setdefault(k, v.strip())
    return head


def cell_key(name, head):
    """Scenario and options without machine=: the identity of a cell across machines."""
    return name.split("_", 1)[0], re.sub(r"machine=[^,}]*,?\s*", "", head.get("options", ""))


def git_files(ref, directory, repo=REPO):
    """{file name: text} of the result files under directory at a git ref."""
    rel = directory.resolve().relative_to(Path(repo).resolve()).as_posix()
    names = subprocess.run(["git", "-C", str(repo), "ls-tree", "-r", "--name-only", ref, "--", rel],
                           capture_output=True, text=True, check=True).stdout.split()
    out = {}
    for name in names:
        if Path(name).parent.as_posix() == rel and name.endswith(".csv"):
            blob = subprocess.run(["git", "-C", str(repo), "show", "%s:%s" % (ref, name)],
                                  capture_output=True, check=True).stdout
            out[Path(name).name] = blob.decode("utf-8")
    return out


def disk_files(directory):
    """{file name: text} of the result files directly under directory."""
    return {p.name: io.open(p, encoding="utf-8", newline="").read()
            for p in sorted(directory.glob("SC*_*.csv"))}


def campaign(machine, new_files, old_files):
    """Compare every file of machine in new_files with its counterpart in old_files. Returns the exit code."""
    old = {}
    for name in sorted(old_files):
        head = header_of(old_files[name])
        if head.get("host") != machine:
            old.setdefault(cell_key(name, head), []).append((name, head.get("host", "?")))
    stats = collections.defaultdict(collections.Counter)
    hosts = collections.defaultdict(collections.Counter)
    problems, status_rows, unchecked = [], [], []
    for name in sorted(new_files):
        raw = new_files[name]
        head = header_of(raw)
        if head.get("host") != machine:
            continue
        sc = name.split("_", 1)[0]
        stats[sc]["files"] += 1
        pairs = old.get(cell_key(name, head))
        if not pairs:
            stats[sc]["NOT CHECKED (no counterpart)"] += 1
            unchecked.append(name)
            continue
        old_name, old_host = pairs[-1]
        hosts[sc][old_host] += 1
        a = list(csv.reader(rows_of_text(old_files[old_name])[0]))
        b = list(csv.reader(rows_of_text(raw)[0]))
        if not a or not b or a[0] != b[0]:
            problems.append("header differs: %s vs %s" % (name, old_name))
            continue
        if len(a) != len(b):
            problems.append("data rows %d vs %d: %s vs %s" % (len(b) - 1, len(a) - 1, name, old_name))
        st = a[0].index("status") if "status" in a[0] else None
        for i, (x, y) in enumerate(zip(a[1:], b[1:]), 1):
            if st is not None and (x[st] or y[st]):
                stats[sc]["rows not compared (status)"] += 1
                status_rows.append("%s row %d: old %r, new %r" % (name, i, x[st], y[st]))
                continue
            stats[sc]["rows compared"] += 1
            if x != y:
                stats[sc]["rows differing"] += 1
                problems.append("row %d differs: %s vs %s" % (i, name, old_name))
    for sc in sorted(stats):
        print("%s: %s; counterparts made on %s" % (sc, ", ".join("%s %d" % kv for kv in sorted(stats[sc].items())),
                                                   dict(hosts[sc]) or "none"))
    for line in status_rows:
        print("  not compared, %s" % line)
    for name in unchecked:
        print("  NOT CHECKED, no committed file with the same options: %s" % name)
    compared = sum(c["rows compared"] for c in stats.values())
    if not compared:
        problems.append("no row of %s was compared" % machine)
    for line in problems[:20]:
        print("  " + line)
    print("%s: %d rows compared, %d problem(s)" % ("DIFFERENT" if problems else "IDENTICAL", compared, len(problems)))
    return 1 if problems else 0


def campaign_main(args):
    opts = dict(a[2:].split("=", 1) for a in args if a.startswith("--") and "=" in a)
    if len(opts) != len(args) or "machine" not in opts or set(opts) - {"machine", "ref", "dir"}:
        print(__doc__)
        return 2
    directory = Path(opts.get("dir", REPO / "results-invariant")).resolve()
    new_files = git_files(opts["ref"], directory) if "ref" in opts else disk_files(directory)
    print("new files: %s at %s" % (directory.as_posix(), opts.get("ref", "the working tree")))
    return campaign(opts["machine"], new_files, disk_files(directory))


def main():
    if any(a.startswith("--machine") for a in sys.argv[1:]):
        return campaign_main(sys.argv[1:])
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    left, right = sys.argv[1], sys.argv[2]
    if os.path.isdir(right):
        right = newest_matching(left, right)
        if right is None:
            return 1
    a, a_crlf, a_bs = data_rows(left)
    b, b_crlf, b_bs = data_rows(right)
    for name, crlf, bs in ((left, a_crlf, a_bs), (right, b_crlf, b_bs)):
        notes = [n for n, on in (("CRLF line endings", crlf), ("backslash paths", bs)) if on]
        if notes:
            print("normalised in %s: %s" % (name, ", ".join(notes)))
    if not a or not b:
        print("NOT CHECKED: %d rows in %s, %d rows in %s" % (len(a), left, len(b), right))
        return 1
    if a[0] != b[0]:
        print("DIFFERENT: the column headers differ")
        print("  " + a[0])
        print("  " + b[0])
        return 1
    compared = min(len(a), len(b)) - 1
    differing = [i for i in range(1, min(len(a), len(b))) if a[i] != b[i]]
    print("data rows: %d vs %d; compared %d; differing %d"
          % (len(a) - 1, len(b) - 1, compared, len(differing)))
    for i in differing[:5]:
        print("  row %d:\n    %s\n    %s" % (i, a[i], b[i]))
    if len(a) != len(b) or differing:
        print("DIFFERENT")
        return 1
    print("IDENTICAL")
    return 0


if __name__ == "__main__":
    sys.exit(main())
