# SPDX-License-Identifier: MIT
"""The closure rule of S-plus a result file was written under, and the selection of the files of one rule.

The scenarios whose methods or measures read S-plus (SC3, SC3PARETO, SC3LOST, SC3CERT, SC3OPT, SC6) write a header
line "# closure <rule>" from 2026-11-08 (FrequentPatterns.Closure in the Java code, preregistration section 21). A
file of these scenarios without that line was written before, under the subsequence rule. Every analysis reads the
files of ONE rule: rows of two rules describe different quantities, and merged as reruns of one cell they would be
averaged into one row without a word.

    python tools/closure_rule.py --self-test
"""
import io
import os
import sys
import tempfile
from collections import defaultdict

RULES = ("necessary", "subsequence")
EARLIER_RULE = "subsequence"
HEADER = "# closure "


def closure_of(path):
    """The rule named in the header of a result file; the earlier rule when the header names none."""
    with io.open(path, encoding="utf-8-sig", newline="") as f:
        for line in f:
            if not line.startswith("#"):
                break
            if line.startswith(HEADER):
                return line[len(HEADER):].strip()
    return EARLIER_RULE


def keep(paths, rule):
    """(the paths written under rule, a dict rule -> count of every rule found among paths)."""
    if rule not in RULES:
        raise ValueError("the closure rule is one of %s, got %r" % (", ".join(RULES), rule))
    found = defaultdict(int)
    kept = []
    for p in paths:
        r = closure_of(p)
        found[r] += 1
        if r == rule:
            kept.append(p)
    return kept, dict(found)


def describe(what, rule, kept, found):
    """One report line: how many files of each rule were found and how many are read."""
    others = ", ".join("%d of rule %s" % (n, r) for r, n in sorted(found.items()) if r != rule)
    return "%s files under closure rule %s: %d read%s" % (what, rule, len(kept), (" (left out: %s)" % others) if others else "")


def self_test():
    failures = []
    d = tempfile.mkdtemp()
    files = {"old.csv": "# run x\n# options {a=1}\ncol\n1\n", "new.csv": "# run x\n# closure necessary\ncol\n1\n",
             "sub.csv": "# run x\n# closure subsequence\ncol\n1\n", "late.csv": "# run x\ncol\n# closure necessary\n"}
    for name, text in files.items():
        io.open(os.path.join(d, name), "w", encoding="utf-8").write(text)
    path = lambda n: os.path.join(d, n)
    if [closure_of(path(n)) for n in ("old.csv", "new.csv", "sub.csv", "late.csv")] != \
            ["subsequence", "necessary", "subsequence", "subsequence"]:
        failures.append("closure_of: a missing line is the earlier rule, and a line after the header does not count")
    kept, found = keep([path(n) for n in sorted(files)], "necessary")
    if [os.path.basename(p) for p in kept] != ["new.csv"] or found != {"necessary": 1, "subsequence": 3}:
        failures.append("keep necessary: %s %s" % (kept, found))
    kept, found = keep([path(n) for n in sorted(files)], "subsequence")
    if sorted(os.path.basename(p) for p in kept) != ["late.csv", "old.csv", "sub.csv"]:
        failures.append("keep subsequence: %s" % kept)
    if describe("SC3", "necessary", ["x"], {"necessary": 1, "subsequence": 3}) != \
            "SC3 files under closure rule necessary: 1 read (left out: 3 of rule subsequence)":
        failures.append("describe")
    try:
        keep([], "contiguous")
        failures.append("an unknown rule was accepted")
    except ValueError:
        pass
    print("self-test: %d failures%s" % (len(failures), (": " + "; ".join(failures)) if failures else ""))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(self_test() if "--self-test" in sys.argv else 0)
