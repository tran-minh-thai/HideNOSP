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

Given a directory, the newest file in it with the same scenario prefix and the same first three
columns in its first data row (dataset, gap, sigma for SC2) is compared, other than the committed
file itself. "Newest" is read from the run stamp in the file name, not from the modification time,
which a fresh clone sets to the checkout time for every file. The chosen file is printed.

Exit code 0 when every data row is identical, 1 when any row differs or nothing could be compared,
2 on a usage error.
"""
import csv
import os
import sys


def data_rows(path):
    """Return (rows, saw_crlf, saw_backslash) for the non-header lines of a result file."""
    with open(path, "rb") as f:
        raw = f.read().decode("utf-8")
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


def main():
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
