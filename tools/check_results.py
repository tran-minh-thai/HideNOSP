# SPDX-License-Identifier: MIT
"""Check the result files against the rules of the three result directories.

The Java sink enforces the same rules when a file is opened; this script re-checks them on what is
actually on disk, so a file moved by hand, edited by hand, or produced by an older build cannot sit
in the wrong directory unnoticed.

Rules checked:
  results-invariant/  no timing or memory column; run stamp present
  results-probe/      no timing or memory column; run stamp present
  results/            at least one timing or memory column; machine label is not "dev"
  every directory     no .partial file: CsvWriter writes <file>.csv.partial and renames it only when
                      the scenario finished, so a .partial file is a run that stopped early (out of
                      memory, killed, a restart) and holds a truncated table that must not be read

Run: python tools/check_results.py [root]
Exit status is 1 when anything fails, so it can be wired into a pre-commit hook or a pipeline.
"""
import csv
import io
import os
import re
import sys

TIMING_WORDS = ("time", "ms", "sec", "second", "memory", "rss", "heap", "throughput", "speedup")
# Must match RunId.stamp(): yyyyMMdd-HHmmss[-SSS] _ machine _ revision _ s<seed>.
#
# The milliseconds are optional here, and only here. RunId always writes them now -- they were
# added after two runs landed in the same second and one silently overwrote the other -- but the
# SC1 and SC2 files already in results-invariant/ were produced before that change and are the
# evidence behind the tables. Rejecting them would be rejecting real results for a reason that has
# nothing to do with them. This pattern was left at second resolution when RunId changed, so for a
# while every NEW file was reported as having no stamp at all; hence both shapes are accepted.
STAMP = re.compile(r"^# run (\d{8}-\d{6}(?:-\d{3})?)_([^_]+)_(.+)_s(-?\d+)$")


def is_timing_column(name):
    lower = name.strip().lower()
    return any(lower == w or ("_" + w) in lower or lower.startswith(w + "_") for w in TIMING_WORDS)


def read_file(path):
    """Returns (comment lines, header cells)."""
    comments, header = [], []
    with io.open(path, encoding="utf-8") as handle:
        for line in handle:
            if line.startswith("#"):
                comments.append(line.rstrip("\n"))
                continue
            header = next(csv.reader([line]))
            break
    return comments, header


def check_directory(root, name, expect_timing):
    directory = os.path.join(root, name)
    problems = []
    if not os.path.isdir(directory):
        return ["missing directory: " + name]
    for entry in sorted(os.listdir(directory)):
        if entry.endswith(".partial"):
            problems.append("%s/%s: the run stopped before its last row; delete this file and run "
                            "the same command again" % (name, entry))
            continue
        if not entry.endswith(".csv"):
            continue
        path = os.path.join(directory, entry)
        comments, header = read_file(path)
        stamp = None
        for comment in comments:
            match = STAMP.match(comment)
            if match:
                stamp = match
                break
        if stamp is None:
            problems.append("%s/%s: no run stamp in the header (rule 2.6.8)" % (name, entry))
        timing = [c for c in header if is_timing_column(c)]
        if expect_timing:
            if not timing:
                problems.append("%s/%s: no timing column; it belongs in results-invariant/"
                                % (name, entry))
            if stamp is not None and stamp.group(2).lower() == "dev":
                problems.append("%s/%s: stamped dev, so it may not hold timing results"
                                % (name, entry))
        elif timing:
            problems.append("%s/%s: timing or memory column %s is not allowed here"
                            % (name, entry, timing))
    return problems


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else "."
    problems = []
    problems += check_directory(root, "results-invariant", expect_timing=False)
    problems += check_directory(root, "results-probe", expect_timing=False)
    problems += check_directory(root, "results", expect_timing=True)
    for problem in problems:
        print("FAIL " + problem)
    if problems:
        print("%d problem(s)" % len(problems))
        return 1
    print("results directories are consistent with their rules")
    return 0


if __name__ == "__main__":
    sys.exit(main())
