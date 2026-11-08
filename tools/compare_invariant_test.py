# SPDX-License-Identifier: MIT
"""Checks of tools/compare_invariant.py: each check builds result files with one known property and
requires the tool to report it.

    python tools/compare_invariant_test.py

Pair mode: identical files, one changed digit, CRLF and backslash normalisation, a changed header.
Campaign mode (--machine): identical cells, a changed value, a status row, a missing counterpart,
nothing comparable, a changed header, an extra row, a counterpart from the same host, the newest
counterpart winning, the host header against the name label, and reading the new files from a git
ref of a temporary repository. Exit code 1 when any check fails.
"""
import contextlib
import io
import os
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import compare_invariant as ci  # noqa: E402

failures = []
checks = 0

HEADER = "dataset,gap,sigma,sensitive_set,method_key,mask_count,lost,status"
ROWS = ['datasets/a.txt,"[0,1]",10,"[[1, 2]]",cut-hide,7,0,',
        'datasets/a.txt,"[0,1]",10,"[[1, 2]]",hha,9,1,']


def check(name, ok, detail=""):
    global checks
    checks += 1
    print("%-66s %s  %s" % (name, "ok" if ok else "FAILED", detail))
    if not ok:
        failures.append(name)


def text(host, options, rows, header=HEADER, eol="\n"):
    lines = ["# run x", "# host %s" % host, "# options {machine=%s, %s}" % (host, options), header] + rows
    return eol.join(lines) + eol


def run_campaign(machine, new, old):
    out = io.StringIO()
    with contextlib.redirect_stdout(out):
        code = ci.campaign(machine, new, old)
    return code, out.getvalue()


def name(stamp, host):
    return "SC3_%s_%s_abc_s2026.csv" % (stamp, host)


OPT = "dataset=datasets/a.txt, sigma=10"
OLD = {name("20261001-000000-000", "OLD"): text("OLD", OPT, ROWS)}

# --- campaign mode ---------------------------------------------------------------------------
code, out = run_campaign("NEW", {name("20261101-000000-000", "NEW"): text("NEW", OPT, ROWS)}, OLD)
check("identical cell passes", code == 0 and "rows compared 2" in out, out.splitlines()[-1])

changed = [ROWS[0].replace(",7,", ",8,"), ROWS[1]]
code, out = run_campaign("NEW", {name("20261101-000000-000", "NEW"): text("NEW", OPT, changed)}, OLD)
check("changed value fails", code == 1 and "rows differing 1" in out, out.splitlines()[-1])

old_ot = {name("20261001-000000-000", "OLD"): text("OLD", OPT, [ROWS[0].replace(",7,0,", ",,,OT"), ROWS[1]])}
code, out = run_campaign("NEW", {name("20261101-000000-000", "NEW"): text("NEW", OPT, ROWS)}, old_ot)
check("status row is listed, not compared", code == 0 and "old 'OT', new ''" in out
      and "rows compared 1" in out, out.splitlines()[-1])

new = {name("20261101-000000-000", "NEW"): text("NEW", OPT, ROWS),
       name("20261101-000000-001", "NEW"): text("NEW", "dataset=datasets/b.txt, sigma=10", ROWS)}
code, out = run_campaign("NEW", new, OLD)
check("missing counterpart is reported, not failed", code == 0 and "NOT CHECKED" in out, out.splitlines()[-1])

code, out = run_campaign("NEW", {name("20261101-000000-001", "NEW"): text("NEW", "dataset=datasets/b.txt", ROWS)}, OLD)
check("nothing compared fails", code == 1 and "no row of NEW was compared" in out, out.splitlines()[-1])

code, out = run_campaign("NEW", {name("20261101-000000-000", "NEW"): text("NEW", OPT, ROWS, header=HEADER + ",x")}, OLD)
check("changed header fails", code == 1 and "header differs" in out, out.splitlines()[-1])

code, out = run_campaign("NEW", {name("20261101-000000-000", "NEW"): text("NEW", OPT, ROWS + [ROWS[1]])}, OLD)
check("extra row fails", code == 1 and "data rows 3 vs 2" in out, out.splitlines()[-1])

same_host = {name("20261001-000000-000", "NEW"): text("NEW", OPT, ROWS)}
code, out = run_campaign("NEW", {name("20261101-000000-000", "NEW"): text("NEW", OPT, ROWS)}, same_host)
check("a file of the same host is not a counterpart", code == 1 and "NOT CHECKED" in out, out.splitlines()[-1])

two = {name("20261001-000000-000", "OLD"): text("OLD", OPT, changed),
       name("20261002-000000-000", "OLD"): text("OLD", OPT, ROWS)}
code, out = run_campaign("NEW", {name("20261101-000000-000", "NEW"): text("NEW", OPT, ROWS)}, two)
check("newest counterpart wins (newest equal)", code == 0, out.splitlines()[-1])
two_rev = {name("20261001-000000-000", "OLD"): text("OLD", OPT, ROWS),
           name("20261002-000000-000", "OLD"): text("OLD", OPT, changed)}
code, out = run_campaign("NEW", {name("20261101-000000-000", "NEW"): text("NEW", OPT, ROWS)}, two_rev)
check("newest counterpart wins (newest differs)", code == 1, out.splitlines()[-1])

labelled = {name("20261101-000000-000", "NEW"): text("OTHER", OPT, changed),
            name("20261101-000000-001", "dev"): text("NEW", OPT, ROWS)}
code, out = run_campaign("NEW", labelled, OLD)
check("host header decides, not the name label", code == 0 and "files 1" in out, out.splitlines()[-1])

windows = [r.replace("datasets/", "datasets\\") for r in ROWS]
code, out = run_campaign("NEW", {name("20261101-000000-000", "NEW"): text("NEW", OPT, windows, eol="\r\n")}, OLD)
check("CRLF and backslash paths are normalised", code == 0, out.splitlines()[-1])

# --- git ref ---------------------------------------------------------------------------------
with tempfile.TemporaryDirectory() as tmp:
    repo = Path(tmp)
    res = repo / "results-invariant"
    res.mkdir()
    git = lambda *a: subprocess.run(["git", "-C", str(repo)] + list(a), check=True, capture_output=True)
    git("init", "-q")
    git("config", "user.name", "test")
    git("config", "user.email", "test@example.org")
    for n, t in OLD.items():
        (res / n).write_text(t, encoding="utf-8", newline="")
    git("add", "-A")
    git("commit", "-qm", "old")
    git("checkout", "-qb", "campaign")
    (res / name("20261101-000000-000", "NEW")).write_text(text("NEW", OPT, ROWS), encoding="utf-8", newline="")
    git("add", "-A")
    git("commit", "-qm", "new")
    git("checkout", "-q", "-")
    on_branch = ci.git_files("campaign", res, repo)
    check("git ref: the branch file is read without a checkout",
          name("20261101-000000-000", "NEW") in on_branch
          and not (res / name("20261101-000000-000", "NEW")).exists(), "%d files" % len(on_branch))
    code, out = run_campaign("NEW", on_branch, ci.disk_files(res))
    check("git ref: campaign over the branch passes", code == 0 and "rows compared 2" in out, out.splitlines()[-1])

# --- pair mode -------------------------------------------------------------------------------
with tempfile.TemporaryDirectory() as tmp:
    a, b = Path(tmp) / "SC3_a.csv", Path(tmp) / "SC3_b.csv"

    def pair(left, right):
        a.write_text(left, encoding="utf-8", newline="")
        b.write_text(right, encoding="utf-8", newline="")
        sys.argv = ["compare_invariant.py", str(a), str(b)]
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            code = ci.main()
        return code, out.getvalue()

    code, out = pair(text("OLD", OPT, ROWS), text("NEW", OPT, ROWS))
    check("pair: identical", code == 0 and "IDENTICAL" in out, out.splitlines()[-1])
    code, out = pair(text("OLD", OPT, ROWS), text("NEW", OPT, changed))
    check("pair: one changed digit", code == 1 and "differing 1" in out, out.splitlines()[-1])
    code, out = pair(text("OLD", OPT, ROWS), text("NEW", OPT, windows, eol="\r\n"))
    check("pair: CRLF and backslash normalised", code == 0 and "normalised" in out, out.splitlines()[-1])
    code, out = pair(text("OLD", OPT, ROWS), text("NEW", OPT, ROWS, header=HEADER + ",x"))
    check("pair: changed header", code == 1 and "headers differ" in out, out.splitlines()[-1])

print("%d checks, %d failed" % (checks, len(failures)))
sys.exit(1 if failures else 0)
