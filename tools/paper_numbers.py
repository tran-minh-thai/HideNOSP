# SPDX-License-Identifier: MIT
"""Every number the manuscript quotes, read back from the result files, written as data.

    python tools/paper_numbers.py [--machine=ADMIN-PC] [--closure=necessary|subsequence] [--small-machine=NAME]
                                  [--out-dir=DIR]
    python tools/paper_numbers.py --self-test

The tool runs the table tools of this repository on the result files (preregistered_analysis,
lower_bound_table, reduced_optimum_table, pareto_table, lost_optimum_table, cert_table, timing_table),
parses their output with fixed patterns, and adds the few quantities no table tool prints: the
row count and status of every SC3 file, the Lost change of the refinement, the certified sets with
a positive bound, the SC1 checks, the locked group tables, the dataset statistics and the platform.

It writes two files, one per result tree (the timing numbers depend on the machine, the counts do
not):
    results-invariant/paper_numbers.json   counts, ratios, test results, table rows
    results/paper_numbers_timing.json      running times and the measuring platform
Each value carries the tool and the output line it was read from. Values are not rounded here.

Exit code 1 when any expected line is missing, when a pattern matches more or fewer times than
expected, when a table tool exits non-zero, or when two sources of the same quantity disagree.
It never falls back to a default value.

--closure names the rule of S-plus of the files read (tools/closure_rule.py; default necessary, the corrected rule of
preregistration section 21). Every table tool that reads S-plus is run with it, and the files read here are the files
of that rule. Under the corrected rule the small instances come from the same machine (--small-machine defaults to
--machine) and the Pareto fronts, run under the earlier rule only and not quoted by the manuscript, are not read.
The running times (SC6) were measured under the earlier rule; timing_table checks them against the SC3 files of the
rule the SC6 files name.
"""
import ast
import csv
import glob
import hashlib
import json
import os
import re
import statistics
import subprocess
import sys
import tempfile
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(Path(__file__).resolve().parent))
import closure_rule  # noqa: E402
RULE = ["necessary"]   # set by main from --closure
# The table tools that read files of a closure rule, and the scenarios whose files carry one.
CLOSURE_TOOLS = ("preregistered_analysis.py", "lower_bound_table.py", "reduced_optimum_table.py", "lost_optimum_table.py",
                 "cert_table.py", "levels_table.py", "dataset_sensitivity.py", "timing_table.py")
# Every scenario whose methods or measures read S-plus, so that files() keeps the files of one rule.
# The running time was measured under both rules, so SC6 belongs here too: without it the repetitions
# and the spread would be read from the files of both rules at once.
CLOSURE_SCENARIOS = ("SC3", "SC3LOST", "SC3CERT", "SC3OPT", "SC3PARETO", "SC6", "SC4", "SC4LOST")
TOOLS = ROOT / "tools"
INV = ROOT / "results-invariant"
TIMED = ROOT / "results"
REPORT_ONLY = ("protein_sdb1",)
BASELINES = ("hha", "dbsh", "sbsh-r1", "sbsh-rmax")
RIVALS = ("occurrence", "hha-stop")
OURS = "cut-hide-rw-ls"
REFINE = "cut-hide-refine"


class Missing(Exception):
    pass


class Numbers:
    """Key -> value, with the place each value was read from."""

    def __init__(self):
        self.values = {}
        self.tables = {}

    def put(self, key, value, source):
        if key in self.values and self.values[key]["value"] != value:
            raise Missing("%s read twice with different values: %r (%s) and %r (%s)" % (
                key, self.values[key]["value"], self.values[key]["source"], value, source))
        self.values[key] = {"value": value, "source": source}

    def table(self, name, columns, rows, source):
        self.tables[name] = {"columns": columns, "rows": rows, "source": source}


def one(pattern, text, what, flags=re.M):
    found = re.findall(pattern, text, flags)
    if len(found) != 1:
        raise Missing("%s: pattern %r matched %d times, expected 1" % (what, pattern, len(found)))
    return found[0]


def run_tool(args, what):
    """Run a table tool with this interpreter; return its output text. A non-zero exit stops here."""
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp) / "out.txt"
        cmd = [sys.executable, str(TOOLS / args[0])] + args[1:]
        if args[0] in CLOSURE_TOOLS:
            cmd.append("--closure=" + RULE[0])
        if "--stdout" in cmd:
            cmd.remove("--stdout")
            p = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True,
                               env=dict(os.environ, PYTHONDONTWRITEBYTECODE="1"))
            text = p.stdout
        else:
            cmd += ["--out", str(out)]
            p = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True,
                               env=dict(os.environ, PYTHONDONTWRITEBYTECODE="1"))
            text = out.read_text(encoding="utf-8") if out.exists() else ""
        if p.returncode != 0:
            raise Missing("%s exited %d: %s" % (what, p.returncode, (p.stderr or p.stdout)[-500:]))
        if not text.strip():
            raise Missing("%s wrote nothing" % what)
        return text


def sha12(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()[:12]


def rows_of(path):
    with open(path, encoding="utf-8", newline="") as f:
        lines = [l for l in f if not l.startswith("#")]
    return list(csv.DictReader(lines))


def options_of(path):
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("# options "):
                body = line[len("# options "):].strip().strip("{}")
                return dict(part.split("=", 1) for part in body.split(", "))
            if not line.startswith("#"):
                break
    return {}


def machine_of(path):
    m = re.search(r"_\d{8}-\d{6}(?:-\d{3})?_([^_]+)_[0-9a-f]+_s\d+\.csv$", Path(path).name)
    return m.group(1) if m else None


def files(tree, scenario, machine):
    """The files of this scenario and machine; for a scenario that reads S-plus, only those of the rule in RULE."""
    out = [p for p in sorted(glob.glob(str(tree / ("%s_*.csv" % scenario)))) if machine_of(p) == machine]
    return closure_rule.keep(out, RULE[0])[0] if scenario in CLOSURE_SCENARIOS else out


def dataset_name(path_text):
    return Path(path_text).stem


# ---------------------------------------------------------------- preregistered analysis

def parse_family(part, family, prefix, nums, scope, raw_effect=False):
    """Per-method blocks, Holm table, pairs table and equivalence lines of one H2-like family.

    raw_effect: the family must print its rank-biserial on the raw differences (H5b, H6, preregistration 22);
    otherwise on d - delta_L (H2, H4b). The label is read, so a family printed the other way stops here.
    """
    rows = []
    for name in family:
        block = one(r"^-- %s\n((?:   .*\n)+)" % re.escape(name), part, "%s block of %s" % (prefix, name))
        same = int(one(r"into Wilcoxon \(same \|R\|\)\s+(\d+) instances", block, prefix + name))
        more = int(one(r"baseline uses more masks\s+(\d+) instances", block, prefix + name))
        ours = int(one(r"CUT-HIDE uses more masks\s+(\d+) instances", block, prefix + name))
        median = float(one(r"median difference of Lost_rel\s+([+-][\d.]+)", block, prefix + name))
        rb = float(one(r"rank-biserial %s\s+([+-]?[\d.]+)" % (r"\(raw differences\)" if raw_effect else ""),
                       block, "%s rank-biserial of %s (%s)" % (prefix, name, "raw" if raw_effect else "d - delta_L")))
        holm = one(r"^  %s\s+(\S+)\s+(\S+)\s+(H0 rejected|not rejected)\s*$" % re.escape(name), part,
                   "%s Holm line of %s" % (prefix, name))
        pairs = one(r"^  %s\s+(\d+) \|\s+(\d+)\s+(\d+)\s+(\d+) \|\s+(\d+)\s+(\d+)\s+(\d+) \|\s+(\d+)\s+(\d+)/(\d+)\s*$"
                    % re.escape(name), part, "%s pairs line of %s" % (prefix, name))
        pairs = [int(x) for x in pairs]
        eq = {}
        for metric in ("lost_rel", "sup_dist"):
            sec = one(r"^-- %s, equivalence margin[^\n]*\n((?:   .*\n)+)" % metric, part, prefix + metric)
            est, lo, hi, verdict = one(r"^   %s\s+\d+\s+([+-]?[\d.]+) \[\s*([+-][\d.]+),\s+([+-][\d.]+)\]\s+(.+?)\s*$"
                                       % re.escape(name), sec, "%s %s of %s" % (prefix, metric, name))
            eq[metric] = (float(est), float(lo), float(hi), verdict)
        if pairs[0] != pairs[9] or pairs[1] + pairs[2] + pairs[3] != same or pairs[7] != ours:
            raise Missing("%s %s: pairs table %s disagrees with the test block (same %d, ours more %d)"
                          % (prefix, name, pairs, same, ours))
        rows.append({"scope": scope, "method": name, "instances": pairs[0], "same": same, "base_more": more,
                     "ours_more": ours, "median_diff": median, "p_raw": float(holm[0]), "p_holm": float(holm[1]),
                     "verdict": holm[2], "rank_biserial": rb,
                     "same_lt": pairs[1], "same_eq": pairs[2], "same_gt": pairs[3],
                     "more_lt": pairs[4], "more_eq": pairs[5], "more_gt": pairs[6], "no_worse_both": pairs[8],
                     "hl_lost": eq["lost_rel"][:3], "eq_lost": eq["lost_rel"][3],
                     "hl_supdist": eq["sup_dist"][:3], "eq_supdist": eq["sup_dist"][3]})
    return rows


def parse_h3(part, scope, what):
    out = []
    for group in ("main", "degenerate"):
        sec = one(r"^-- group %s\n   method[^\n]*\n((?:   \S.*\n)+)" % group, part, "%s group %s" % (what, group))
        for line in sec.splitlines():
            f = line.split()
            out.append({"scope": scope, "group": group, "method": f[0], "n": int(f[1]),
                        "median": float(f[2]), "max": float(f[3])})
    return out


def analysis(nums, machine):
    text = run_tool(["preregistered_analysis.py", "--sc3", "results-invariant", "--sc2", "results-invariant",
                     "--machine=%s" % machine], "preregistered_analysis")
    src = "preregistered_analysis.py --machine=%s" % machine
    i_rep = text.index("Scope: repeated-item sensitive patterns")
    i_h4 = text.index("Section 19.1, distinct-item scope")    # preregistration section 19.1
    i_h4r = text.index("Section 19.1, repeated-item scope")  # preregistration section 19.1
    dist, rep, h4, h4r = text[:i_rep], text[i_rep:i_h4], text[i_h4:i_h4r], text[i_h4r:]
    nums.put("sc3.n_rows.analysis", int(one(r"After merging the reruns by their median: (\d+) rows", text, src)), src)
    tested = []
    for part, scope, family, prefix in ((dist, "distinct", BASELINES, "H2"), (rep, "repeated", BASELINES, "H2-R"),
                                        (h4, "distinct", RIVALS, "H4b"), (h4r, "repeated", RIVALS, "H4b-R")):
        excluded = one(r"Share of instances excluded: (\d+)/(\d+)", part, prefix)
        nums.put("excluded.%s" % prefix, [int(excluded[0]), int(excluded[1])], src)
        rows = parse_family(part, family, prefix, nums, scope)
        for r in rows:
            r["test"] = prefix
        tested += rows
    for part, scope, name in ((h4, "distinct", "H4a"), (h4r, "repeated", "H4a-R")):
        sec = one(r"^%s  \|R\|\(competitor\)[^\n]*\n=+\n  competitor[^\n]*\n((?:  \S.*\n)+)" % re.escape(name),
                  part, name)
        for line in sec.splitlines():
            f = line.split()
            row = next(r for r in tested if r["test"].startswith("H4b") and r["scope"] == scope and r["method"] == f[0])
            if int(f[1]) != row["instances"]:
                raise Missing("%s %s: %s instances, the H4b pairs table has %d" % (name, f[0], f[1], row["instances"]))
            row.update({"ratio_median": float(f[2]), "ratio_max": float(f[3]), "rival_fewer": int(f[4]),
                        "ratio_verdict": " ".join(f[5:])})
    h3 = parse_h3(dist[dist.index("\nH3  r = "):], "distinct", "H3") + parse_h3(
        rep[rep.index("r = |R|/|R|* per method and group"):], "repeated", "H3-R")
    # The conclusion states that at equal masking count CUT-HIDE never loses more patterns than any
    # baseline or rival, in either scope: every '>' cell of the pairs tables must be 0.
    worse = [(r["test"], r["method"], r["same_gt"]) for r in tested if r["same_gt"] != 0]
    if worse:
        raise Missing("pairs tables: CUT-HIDE loses more patterns at equal |R| in %s; the text says never" % worse)
    nums.put("pairs.same_gt.total", 0, src)
    nums.table("h2", ["test", "scope", "method"], tested, src)
    nums.table("h3", ["scope", "group", "method"], h3, src)

    def sel(test, method=None):
        return [r for r in tested if r["test"] == test and (method is None or r["method"] == method)]

    for key, test in (("h2", "H2"), ("h2r", "H2-R"), ("h4", "H4b"), ("h4r", "H4b-R")):
        rs = sel(test)
        ns = {r["instances"] for r in rs}
        if len(ns) != 1:
            raise Missing("%s: instance counts differ across methods: %s" % (test, ns))
        nums.put(key + ".n", ns.pop(), src)
        for field, name in (("same", "same"), ("base_more", "base_more"), ("no_worse_both", "nowb")):
            nums.put("%s.%s.min" % (key, name), min(r[field] for r in rs), src)
            nums.put("%s.%s.max" % (key, name), max(r[field] for r in rs), src)
    for b in ("hha", "dbsh"):
        r = sel("H2-R", b)[0]
        nums.put("h2r.%s.p_holm" % b, r["p_holm"], src)
        nums.put("h2r.%s.median" % b, r["median_diff"], src)
        nums.put("h2r.%s.rb" % b, r["rank_biserial"], src)
    # The contributions and the conclusion say the baselines use more masks on MOST instances.
    for test in ("H2", "H2-R"):
        rs = sel(test)
        if any(2 * r["base_more"] <= r["instances"] for r in rs):
            raise Missing("%s: a baseline uses more masks on at most half of the instances; the text says most" % test)
    nums.put("h2r.ours_more.hha", sel("H2-R", "hha")[0]["ours_more"], src)
    others = {r["ours_more"] for r in sel("H2-R") if r["method"] != "hha"}
    if len(others) != 1:
        raise Missing("h2r.ours_more.others: the three other baselines differ: %s" % others)
    nums.put("h2r.ours_more.others", others.pop(), src)
    rs = sel("H4b-R")
    for field, name in (("p_holm", "p_holm"), ("rank_biserial", "rb"), ("median_diff", "median")):
        nums.put("h4r.%s.min" % name, min(r[field] for r in rs), src)
        nums.put("h4r.%s.max" % name, max(r[field] for r in rs), src)
    for scope, test in (("distinct", "H4b"), ("repeated", "H4b-R")):
        rs = sel(test)
        nums.put("h4a.ratio.%s.min" % scope, min(r["ratio_median"] for r in rs), src)
        nums.put("h4a.ratio.%s.max" % scope, max(r["ratio_median"] for r in rs), src)
        if scope == "repeated":
            nums.put("h4a.fewer.repeated.min", min(r["rival_fewer"] for r in rs), src)
            nums.put("h4a.fewer.repeated.max", max(r["rival_fewer"] for r in rs), src)
        elif any(r["rival_fewer"] for r in rs):
            raise Missing("H4a distinct: a rival uses fewer masks on some instance; the text says none")
        for r in sel(test.replace("H4b", "H2")):
            if r["ours_more"] and scope == "distinct":
                raise Missing("H2: CUT-HIDE uses more masks on some instance; the text says none")
    for scope, group, key in (("distinct", "main", "h3.main"), ("repeated", "main", "h3r.main"),
                              ("distinct", "degenerate", "h3.degen")):
        rs = [r for r in h3 if r["scope"] == scope and r["group"] == group and r["method"] in BASELINES]
        ns = {r["n"] for r in rs}
        if len(ns) != 1:
            raise Missing("%s: instance counts differ: %s" % (key, ns))
        nums.put(key + ".n", ns.pop(), src)
        nums.put(key + ".median.min", min(r["median"] for r in rs), src)
        nums.put(key + ".median.max", max(r["median"] for r in rs), src)
    for r in h3:
        if r["method"].startswith("cut-hide") and (r["median"], r["max"]) != (1.0, 1.0):
            raise Missing("H3 table: %s does not reach |R|* everywhere (%s, %s)" % (r["method"], r["scope"], r["group"]))


# ---------------------------------------------------------------- HIDE-SEQ and g- > 0 (preregistration 22.2, 22.3)

def parse_pairs_only(part, family, prefix, scope):
    """A family with no test: the per-method blocks and the pairs table only.

    With several sensitive patterns the methods almost never mask the same number of positions, so the H2
    procedure has nothing to test; the report says "not enough pairs for a test" for each baseline, and the
    evidence is the table of pairs. Each baseline must say so, so a family that does become testable is not read
    here by mistake.
    """
    rows = []
    for name in family:
        block = one(r"^-- %s\n((?:   .*\n)+)" % re.escape(name), part, "%s block of %s" % (prefix, name))
        same = int(one(r"into Wilcoxon \(same \|R\|\)\s+(\d+) instances", block, prefix + name))
        more = int(one(r"baseline uses more masks\s+(\d+) instances", block, prefix + name))
        ours = int(one(r"CUT-HIDE uses more masks\s+(\d+) instances", block, prefix + name))
        if "not enough pairs for a test" not in block:
            raise Missing("%s %s: a test was run on %d pairs; read this family with parse_family" % (prefix, name, same))
        pairs = one(r"^  %s\s+(\d+) \|\s+(\d+)\s+(\d+)\s+(\d+) \|\s+(\d+)\s+(\d+)\s+(\d+) \|\s+(\d+)\s+(\d+)/(\d+)\s*$"
                    % re.escape(name), part, "%s pairs line of %s" % (prefix, name))
        pairs = [int(x) for x in pairs]
        if pairs[0] != pairs[9] or pairs[1] + pairs[2] + pairs[3] != same or pairs[7] != ours \
                or pairs[4] + pairs[5] + pairs[6] != more:
            raise Missing("%s %s: pairs table %s disagrees with the block (same %d, baseline more %d, ours more %d)"
                          % (prefix, name, pairs, same, more, ours))
        rows.append({"scope": scope, "method": name, "instances": pairs[0], "same": same, "base_more": more,
                     "ours_more": ours, "same_lt": pairs[1], "same_eq": pairs[2], "same_gt": pairs[3],
                     "more_lt": pairs[4], "more_eq": pairs[5], "more_gt": pairs[6], "no_worse_both": pairs[8]})
    return rows


def parse_ratio_block(part, scope):
    """The |R|/|R|* block (H3, H5a, the H3 part of H6): per group and method, n, median, max and the verdict."""
    out = []
    for group in ("main", "degenerate"):
        sec = one(r"^-- group %s\n((?:   .*\n)+)" % group, part, "ratio block, group %s" % group)
        if sec.strip() == "no rows":
            continue
        for line in sec.splitlines()[1:]:
            m = re.match(r"^   (\S+)\s+(\d+)\s+([\d.]+)\s+([\d.]+)(?:\s+(.*?))?\s*$", line)
            if not m:
                raise Missing("ratio block: line not understood: %r" % line)
            out.append({"scope": scope, "group": group, "method": m.group(1), "n": int(m.group(2)),
                        "median": float(m.group(3)), "max": float(m.group(4)), "verdict": m.group(5) or ""})
    return out


# The four readings of the new problems, each one call of preregistered_analysis.py.
# (key, scenario, gaps, sizes, a test is expected, the label of the scopes)
NEW_MODES = (("h5", "SC4", "zero", "1", True, ("H5b", "H5b-R")),
             ("h5s5", "SC4", "zero", "5", False, ("H5b", "H5b-R")),
             ("h6", "SC3", "positive", "1", True, ("H6", "H6-R")),
             ("h6sev", "SC3", "positive", "several", False, ("H6", "H6-R")))


def mode_report(machine, key, scenario, gaps, sizes):
    text = run_tool(["preregistered_analysis.py", "--sc3", "results-invariant", "--sc2", "results-invariant",
                     "--machine=%s" % machine, "--scenario=%s" % scenario, "--gaps=%s" % gaps, "--sizes=%s" % sizes],
                    "preregistered_analysis %s" % key)
    return text, "preregistered_analysis.py --machine=%s --scenario=%s --gaps=%s --sizes=%s" % (
        machine, scenario, gaps, sizes)


def analysis_mode(nums, machine, key, scenario, gaps, sizes, tested, labels):
    text, src = mode_report(machine, key, scenario, gaps, sizes)
    parse_mode(nums, text, src, key, scenario, sizes, tested, labels)


def parse_mode(nums, text, src, key, scenario, sizes, tested, labels):
    i_dist = text.index("Scope: distinct-item sensitive patterns")
    i_rep = text.index("Scope: repeated-item sensitive patterns")
    i_check = text.index("The method under test masks exactly |R|* on every one-pattern instance")
    parts = {"distinct": text[i_dist:i_rep], "repeated": text[i_rep:i_check]}
    # The groups: the measuring machine's SC2 files must give the locked tables (twice: pool, repeated).
    locked = [int(x) for x in re.findall(r"^  (\d+) configurations, all as locked\.$", text, re.M)]
    if len(locked) != 2:
        raise Missing("%s: %d group checks passed, expected 2" % (key, len(locked)))
    nums.put("%s.groups.pool" % key, locked[0], src)
    nums.put("%s.groups.repeated" % key, locked[1], src)
    if scenario == "SC4":
        m = re.search(r"^Instances that need no mask \(no sequence reaches r\)[^\n]*: distinct items (\d+), "
                      r"repeated items (\d+)$", text, re.M)
        nums.put("%s.nomask.distinct" % key, int(m.group(1)) if m else 0, src)
        nums.put("%s.nomask.repeated" % key, int(m.group(2)) if m else 0, src)
    rows, ratios = [], []
    for scope, label in zip(("distinct", "repeated"), labels):
        part = parts[scope]
        excluded = one(r"Share of instances excluded: (\d+)/(\d+)", part, "%s %s" % (key, label))
        nums.put("%s.%s.excluded" % (key, scope), [int(excluded[0]), int(excluded[1])], src)
        family = parse_family(part, BASELINES, label, Numbers(), scope, raw_effect=True) if tested else \
            parse_pairs_only(part, BASELINES, label, scope)
        for r in family:
            r["test"] = label
            # On the raw differences the effect is +1 exactly when no equal-mask instance has CUT-HIDE losing more
            # (Lost and Lost_rel share the denominator of an instance, so they have the same sign).
            if tested and r["same"] and (r["rank_biserial"] == 1.0) != (r["same_gt"] == 0 and r["same_lt"] > 0):
                raise Missing("%s %s %s: rank-biserial %+.3f disagrees with the pairs table (< %d, > %d)"
                              % (key, label, r["method"], r["rank_biserial"], r["same_lt"], r["same_gt"]))
        rows += family
        if sizes == "1":
            start = part.index("|R|/|R|*")
            ratios += parse_ratio_block(part[start:], scope)
    # The method under test masks exactly |R|* on every one-pattern instance; with one pattern this is checked.
    for scope in ("distinct", "repeated"):
        m = re.search(r"^  %s scope: (\d+) rows compared, (\d+) mask another count" % scope, text, re.M)
        if sizes == "1":
            if not m:
                raise Missing("%s %s: the |R|* check compared no row" % (key, scope))
            if int(m.group(2)):
                raise Missing("%s %s: %s rows mask another count than |R|*" % (key, scope, m.group(2)))
            nums.put("%s.rstar.%s" % (key, scope), int(m.group(1)), src)
    nums.table(key, ["test", "scope", "method"], rows, src)
    if ratios:
        nums.table(key + "_ratio", ["scope", "group", "method"], ratios, src)
    for scope in ("distinct", "repeated"):
        rs = [r for r in rows if r["scope"] == scope]
        ns = {r["instances"] for r in rs}
        if len(ns) != 1:
            raise Missing("%s %s: instance counts differ across methods: %s" % (key, scope, ns))
        k = "%s.%s" % (key, scope)
        nums.put(k + ".n", ns.pop(), src)
        for field, name in (("same", "same"), ("base_more", "base_more"), ("ours_more", "ours_more"),
                            ("no_worse_both", "nowb"), ("same_gt", "same_gt")):
            nums.put("%s.%s.min" % (k, name), min(r[field] for r in rs), src)
            nums.put("%s.%s.max" % (k, name), max(r[field] for r in rs), src)
        for r in rs:
            for field in ("same", "base_more", "ours_more", "no_worse_both", "same_gt"):
                nums.put("%s.%s.%s" % (k, r["method"], field), r[field], src)
        if tested:
            nums.put(k + ".rejected", sum(r["verdict"] == "H0 rejected" for r in rs), src)
            nums.put(k + ".equivalent_lost", sum(r["eq_lost"] == "equivalent" for r in rs), src)
            nums.put(k + ".equivalent_supdist", sum(r["eq_supdist"] == "equivalent" for r in rs), src)
            nums.put(k + ".p_holm.min", min(r["p_holm"] for r in rs), src)
    for r in ratios:
        if r["group"] == "main" and r["method"] in BASELINES + ("hha-emb",):
            nums.put("%s.ratio.%s.%s" % (key, r["scope"], r["method"]), r["median"], src)
        if r["method"].startswith("cut-hide") and (r["median"], r["max"]) != (1.0, 1.0):
            raise Missing("%s: %s does not reach |R|* everywhere (%s, %s)" % (key, r["method"], r["scope"], r["group"]))
    for scope in ("distinct", "repeated"):
        main = [r for r in ratios if r["scope"] == scope and r["group"] == "main" and r["method"] in BASELINES]
        if main:
            nums.put("%s.ratio.%s.n" % (key, scope), main[0]["n"], src)
            nums.put("%s.ratio.%s.median.min" % (key, scope), min(r["median"] for r in main), src)
            nums.put("%s.ratio.%s.median.max" % (key, scope), max(r["median"] for r in main), src)
            if any(r["verdict"] for r in main):  # a descriptive block carries no verdict
                nums.put("%s.ratio.%s.above" % (key, scope), sum(r["verdict"] == "above 1" for r in main), src)


def seq_lost(nums, machine):
    """SC4LOST: the exact Lost optimum under HIDE-SEQ on small instances (preregistration 22.2), descriptive."""
    text = run_tool(["lost_optimum_table.py", "--machine=%s" % machine, "--scenario=SC4LOST"], "lost_optimum_table SC4LOST")
    parse_seq_lost(nums, text, "lost_optimum_table.py --machine=%s --scenario=SC4LOST" % machine)


def parse_seq_lost(nums, text, src):
    rows = []
    for scope in ("distinct", "repeated"):
        head = one(r"^== tested datasets, %s scope: (\d+) instances solved; skipped (.*)$" % scope, text, src)
        skipped = sum(int(x) for x in re.findall(r"(\d+)", head[1])) if head[1] != "none" else 0
        nums.put("seqlost.n.%s" % scope, int(head[0]), src)
        nums.put("seqlost.skipped.%s" % scope, skipped, src)
        body = one(r"^== tested datasets, %s scope:[^\n]*\n   method[^\n]*\n((?:   \S.*\n)+)" % scope, text, src)
        for line in body.splitlines():
            if line.strip().startswith("empty B_eps"):
                continue
            m = re.match(r"^   (\S+)\s+(\d+)\s+(\d+)/(\d+)\s+(\d+)/(\d+)\s+(\d+)\s+(\S+)\s+(\S+)\s+(\d+)\s+(\d+)/(\d+)", line)
            if not m:
                raise Missing("lost_optimum_table SC4LOST: line not understood: %r" % line)
            g = m.groups()
            rows.append({"scope": scope, "method": g[0], "n": int(g[1]), "at_R": int(g[2]), "opt": int(g[4]),
                         "excess": int(g[6]), "above": int(g[9]), "opt_no_worse": int(g[10])})
    if "No problem:" not in text:
        raise Missing("lost_optimum_table SC4LOST reports a problem")
    nums.table("seqlost", ["scope", "method"], rows, src)
    for scope in ("distinct", "repeated"):
        rs = {r["method"]: r for r in rows if r["scope"] == scope}
        if rs[OURS]["at_R"] != rs[OURS]["n"]:
            raise Missing("SC4LOST %s: CUT-HIDE not at |R|*_SEQ on every instance" % scope)
        nums.put("seqlost.ours.opt.%s" % scope, rs[OURS]["opt"], src)
        nums.put("seqlost.ours.excess.%s" % scope, rs[OURS]["excess"], src)
        nums.put("seqlost.chaw.opt.%s" % scope, rs["cut-hide-rw"]["opt"], src)
        nums.put("seqlost.chfw.opt.%s" % scope, rs["cut-hide"]["opt"], src)
        others = [r for m, r in rs.items() if m in BASELINES + RIVALS]
        nums.put("seqlost.base.opt.min.%s" % scope, min(r["opt"] for r in others), src)
        nums.put("seqlost.base.opt.max.%s" % scope, max(r["opt"] for r in others), src)
        nums.put("seqlost.base.excess.min.%s" % scope, min(r["excess"] for r in others), src)
        nums.put("seqlost.base.above.max.%s" % scope, max(r["above"] for r in others), src)
        if not min(r["excess"] for r in others) > rs[OURS]["excess"]:
            raise Missing("SC4LOST %s: some method loses fewer patterns in excess than CUT-HIDE" % scope)


def sc4_rows(nums, machine):
    """SC4 (HIDE-SEQ): rows, rows of an instance that needs no mask, Ghost and HF, the three CUT-HIDE keys."""
    paths = files(INV, "SC4", machine)
    total, method_rows, nomask = 0, 0, 0
    several = defaultdict(dict)
    for path in paths:
        for r in rows_of(path):
            total += 1
            if not r.get("method_key"):
                if r.get("status") != "no sequence reaches r":
                    raise Missing("SC4 row without a method and with status %r (%s)" % (r.get("status"), Path(path).name))
                nomask += 1
                continue
            method_rows += 1
            if r.get("status"):
                raise Missing("SC4 method row with status %r (%s)" % (r["status"], Path(path).name))
            if float(r.get("ghost") or 0) != 0 or float(r.get("hf") or 0) != 0:
                raise Missing("SC4 row with Ghost or HF above 0 (%s, %s)" % (Path(path).name, r["method_key"]))
            if r["method_key"] in ("cut-hide-rw-ls", "cut-hide-rw", "cut-hide") and r["sensitive_set"].count("[") > 2:
                k = (dataset_name(r["dataset"]), r["gap"], r["sigma"], r["sensitive_set"])
                several[k][r["method_key"]] = (r["mask_count"], r["lost"], r["lost_rel"], r["sup_dist"], r["dist_r"])
    src = "SC4 files of %s" % machine
    complete = {k: v for k, v in several.items() if len(v) == 3}
    if not complete or any(len(set(v.values())) != 1 for v in complete.values()):
        raise Missing("SC4 |S| >= 2: the three CUT-HIDE keys differ on some set, or no set has all three")
    nums.put("sc4.n_rows", total, src)
    nums.put("sc4.method_rows", method_rows, src)
    nums.put("sc4.nomask_rows", nomask, src)
    nums.put("sc4.several.same.n", len(complete), src)


def sc1gap(nums, machine):
    """SC1GAP: the exact program for g- > 0 against exhaustive search (preregistration 22.3)."""
    paths = [p for p in sorted(glob.glob(str(INV / "SC1GAP_*.csv"))) if machine_of(p) == machine]
    if len(paths) != 1:
        raise Missing("SC1GAP files of %s: %d, expected 1" % (machine, len(paths)))
    check_sc1gap(nums, rows_of(paths[0]), Path(paths[0]).name)


def check_sc1gap(nums, rows, src):
    if not rows:
        raise Missing("SC1GAP %s: no row" % src)
    total = Counter()
    for r in rows:
        if int(r["mismatches"]):
            raise Missing("SC1GAP %s %s: %s mismatches" % (r["gap"], r["items"], r["mismatches"]))
        for c in ("instances", "targets", "targets_needing_a_mask"):
            total[c] += int(r[c])
    nums.put("sc1gap.gaps", len({r["gap"] for r in rows}), src)
    nums.put("sc1gap.instances", total["instances"], src)
    nums.put("sc1gap.targets", total["targets"], src)
    nums.put("sc1gap.targets_needing", total["targets_needing_a_mask"], src)
    nums.put("sc1gap.mismatches", 0, src)
    for scope in ("distinct", "repeated"):
        nums.put("sc1gap.instances.%s" % scope, sum(int(r["instances"]) for r in rows if r["items"] == scope), src)


# ---------------------------------------------------------------- lower bounds

def lower_bounds(nums, machine):
    text = run_tool(["lower_bound_table.py", "--machine=%s" % machine, "--bound-machine=%s" % machine], "lower_bound_table")
    src = "lower_bound_table.py --machine=%s" % machine
    rows = []
    fewer = {}
    for scope, size, body in re.findall(r"^== (distinct|repeated) scope, \|S\| = (\d+): \d+ sets\n((?:   .*\n)+)", text, re.M):
        for m in re.finditer(r"^   (\S+)\s+(\d+)\s+(\d+)/(\d+)\s+([\d.]+)\s+([\d.]+)\s*$", body, re.M):
            rows.append({"scope": scope, "size": int(size), "method": m.group(1), "sets": int(m.group(2)),
                         "at_bound": int(m.group(3)), "median": float(m.group(5)), "max": float(m.group(6))})
        for which in (OURS, REFINE):
            f = re.search(r"sets where hha uses fewer masks than %s: (\d+)" % re.escape(which), body)
            if f:
                fewer[(scope, int(size), which)] = int(f.group(1))
    # Every block holds the three CUT-HIDE keys, the four baselines and the two rivals; |S| > 1 adds REFINE.
    for scope in ("distinct", "repeated"):
        for size in (1, 5, 10, 20):
            have = {r["method"] for r in rows if r["scope"] == scope and r["size"] == size}
            want = {OURS, "cut-hide-rw", "cut-hide"} | set(BASELINES) | set(RIVALS) | ({REFINE} if size > 1 else set())
            if have != want:
                raise Missing("lower_bound_table %s |S|=%d: methods %s, expected %s" % (scope, size, sorted(have), sorted(want)))
    nums.table("bound", ["scope", "size", "method"], rows, src)
    for r in rows:
        if r["size"] == 1 and r["method"] == OURS:
            if r["at_bound"] != r["sets"]:
                raise Missing("lower_bound_table: %s not at |R|* on every single-pattern set" % OURS)
            nums.put("exact.%s.n" % r["scope"], r["sets"], src)
    for scope in ("distinct", "repeated"):
        several = [r for r in rows if r["scope"] == scope and r["size"] > 1]
        ours = [r["median"] for r in several if r["method"] == OURS]
        base = [r["median"] for r in several if r["method"] in BASELINES]
        if len(ours) != 3 or len(base) != 12:
            raise Missing("lower_bound_table %s: %d CUT-HIDE and %d baseline medians" % (scope, len(ours), len(base)))
        nums.put("lb.ours.%s.min" % scope, min(ours), src)
        nums.put("lb.ours.%s.max" % scope, max(ours), src)
        nums.put("lb.base.%s.min" % scope, min(base), src)
        nums.put("lb.base.%s.max" % scope, max(base), src)
        for size in (5, 10, 20):
            o = next(r["median"] for r in several if r["size"] == size and r["method"] == OURS)
            if any(r["median"] <= o for r in several if r["size"] == size and r["method"] in BASELINES):
                raise Missing("lower_bound_table %s |S|=%d: a baseline median is not above CUT-HIDE" % (scope, size))
    # The figure of the bound ratios leaves the counts of sets at the bound to the text: CUT-HIDE and
    # CUT-HIDE+REFINE by size, and no baseline or rival on any set.
    for r in rows:
        if r["size"] > 1 and r["method"] in (OURS, REFINE):
            nums.put("lb.at_bound.%s.%d.%s" % (r["scope"], r["size"], "ours" if r["method"] == OURS else "refine"),
                     r["at_bound"], src)
    reached = [(r["scope"], r["size"], r["method"], r["at_bound"]) for r in rows
               if r["size"] > 1 and r["method"] in BASELINES + RIVALS and r["at_bound"]]
    if reached:
        raise Missing("lower_bound_table: a baseline or rival reaches LB_U on some set %s; the text says none" % reached)
    nums.put("lb.at_bound.rivals.total", 0, src)
    ours = [v for (s, z, w), v in fewer.items() if s == "repeated" and w == OURS]
    ref = [v for (s, z, w), v in fewer.items() if s == "repeated" and w == REFINE]
    if len(ours) != 3 or len(ref) != 3:
        raise Missing("lower_bound_table: hha-fewer lines read %d and %d, expected 3 and 3" % (len(ours), len(ref)))
    nums.put("lb.hha_fewer.ours", sum(ours), src)
    nums.put("lb.hha_fewer.refine", sum(ref), src)


# ---------------------------------------------------------------- reduced optimum

def reduced_optimum(nums, machine):
    text = run_tool(["reduced_optimum_table.py", "--machine=%s" % machine], "reduced_optimum_table")
    src = "reduced_optimum_table.py --machine=%s" % machine
    rows = []
    for scope, size, seqs, solved, body in re.findall(
            r"^== (distinct|repeated) scope, \|S\| = (\d+), instances of (\d+) sequences: (\d+) solved[^\n]*\n((?:   .*\n)+)",
            text, re.M):
        solved = int(solved)
        b = one(r"median ratio to the bound: greedy ([\d.]+), refined ([\d.]+), optimum ([\d.]+)", body, src)
        o = one(r"median ratio to the optimum: greedy ([\d.]+), refined ([\d.]+)", body, src)
        share = one(r"due to the bound, over (\d+) instances with refined > bound: ([\d.]+)", body, src)
        rows.append({"scope": scope, "size": int(size), "sequences": int(seqs), "solved": solved,
                     "greedy_bound": float(b[0]), "refined_bound": float(b[1]), "opt_bound": float(b[2]),
                     "greedy_opt": float(o[0]), "refined_opt": float(o[1]), "bound_share": float(share[1])})
    if len(rows) != 12:
        raise Missing("reduced_optimum_table: %d blocks, expected 12" % len(rows))
    # The text states LB_U <= |R|*(S) <= |R|(refined) <= |R|(greedy) on every instance: read every SC3OPT row.
    solved, holds = 0, 0
    for path in files(INV, "SC3OPT", machine):
        for r in rows_of(path):
            if r.get("status") or r.get("optimum", "") == "":
                continue
            solved += 1
            lb, opt, ref, gr = (int(r[k]) for k in ("conflict_bound", "optimum", "refined", "greedy"))
            holds += lb <= opt <= ref <= gr
    if solved != sum(r["solved"] for r in rows):
        raise Missing("SC3OPT rows: %d solved, the table blocks %d" % (solved, sum(r["solved"] for r in rows)))
    if holds != solved:
        raise Missing("SC3OPT: the order LB_U <= OPT <= refined <= greedy fails on %d of %d instances" % (solved - holds, solved))
    nums.put("opt.order.n", solved, "SC3OPT files of %s" % machine)
    nums.table("optimum", ["scope", "size", "sequences"], rows, src)
    for scope in ("distinct", "repeated"):
        rs = [r for r in rows if r["scope"] == scope]
        nums.put("opt.bound_share.%s.min" % scope, min(r["bound_share"] for r in rs), src)
        nums.put("opt.bound_share.%s.max" % scope, max(r["bound_share"] for r in rs), src)
        nums.put("opt.refined_vs_opt.%s.max" % scope, max(r["refined_opt"] for r in rs), src)
        nums.put("opt.greedy_vs_opt.%s.max" % scope, max(r["greedy_opt"] for r in rs), src)
        for size in (5, 10, 20):
            s8 = next(r for r in rs if r["size"] == size and r["sequences"] == 8)
            s16 = next(r for r in rs if r["size"] == size and r["sequences"] == 16)
            if scope == "repeated" and not s16["bound_share"] < s8["bound_share"]:
                raise Missing("reduced_optimum %s |S|=%d: method share not larger on 16 sequences" % (scope, size))


# ---------------------------------------------------------------- Pareto fronts

def pareto(nums, machine):
    text = run_tool(["pareto_table.py", "--machine=%s" % machine, "results-invariant", "--stdout"], "pareto_table")
    src = "pareto_table.py --machine=%s" % machine
    rows = re.findall(r"^\| (\S+) \| (\[\d+,\d+\]) \| (\d+) \| (\d+) \|", text, re.M)
    if not rows:
        raise Missing("pareto_table: no configuration row")
    nums.put("pareto.n_config", len(rows), src)
    nums.put("pareto.n_sets", sum(int(r[3]) for r in rows), src)
    for scope in ("distinct", "repeated"):
        p = one(r"^%s-item sets: (\d+) pairs of a Holm-family baseline and a set: (\d+) dominated, (\d+) tied, (\d+) not dominated"
                % scope, text, src)
        p = [int(x) for x in p]
        if p[1] + p[2] + p[3] != p[0]:
            raise Missing("pareto %s: %s does not add up" % (scope, p))
        for k, v in zip(("pairs", "dominated", "tied", "neither"), p):
            nums.put("pareto.%s.%s" % (scope, k), v, src)


# ---------------------------------------------------------------- exact Lost optimum (SC3LOST)

def lost_optimum(nums, small_machine):
    text = run_tool(["lost_optimum_table.py", "--machine=%s" % small_machine], "lost_optimum_table")
    src = "lost_optimum_table.py --machine=%s" % small_machine
    rows = []
    for scope in ("distinct", "repeated"):
        head = one(r"^== tested datasets, %s scope: (\d+) instances solved; skipped (.*)$" % scope, text, src)
        skipped = re.findall(r"(\d+)", head[1]) if head[1] != "none" else ["0"]
        nums.put("lostopt.n.%s" % scope, int(head[0]), src)
        nums.put("lostopt.skipped.%s" % scope, sum(int(x) for x in skipped), src)
        body = one(r"^== tested datasets, %s scope:[^\n]*\n   method[^\n]*\n((?:   \S.*\n)+)" % scope, text, src)
        for line in body.splitlines():
            if line.strip().startswith("empty B_eps"):
                continue
            m = re.match(r"^   (\S+)\s+(\d+)\s+(\d+)/(\d+)\s+(\d+)/(\d+)\s+(\d+)\s+(\S+)\s+(\S+)\s+(\d+)\s+(\d+)/(\d+)", line)
            if not m:
                raise Missing("lost_optimum_table: line not understood: %r" % line)
            g = m.groups()
            rows.append({"scope": scope, "method": g[0], "n": int(g[1]), "at_R": int(g[2]), "opt": int(g[4]),
                         "excess": int(g[6]), "above": int(g[9]), "opt_no_worse": int(g[10])})
        empty = one(r"^   empty B_eps, CUT-HIDE with uniform weights: (.*)$", body, src)
        if scope == "distinct" and empty.strip() != "none":
            raise Missing("lost_optimum_table: empty band in the distinct scope: %r" % empty)
        if scope == "repeated":
            counts = re.findall(r"cut-hide(?:-rw)?(?:-ls)? (\d+) \(Lost = OPT (\d+)\)", empty)
            if len(counts) != 3 or len({c for c in counts}) != 1 or counts[0][0] != counts[0][1]:
                raise Missing("lost_optimum_table: empty-band line %r" % empty)
            nums.put("lostopt.emptyband.n", int(counts[0][0]), src)
    nums.table("lostopt", ["scope", "method"], rows, src)
    for scope in ("distinct", "repeated"):
        rs = {r["method"]: r for r in rows if r["scope"] == scope}
        if rs[OURS]["at_R"] != rs[OURS]["n"]:
            raise Missing("SC3LOST %s: CUT-HIDE not at |R|* on every instance" % scope)
        nums.put("lostopt.ours.opt.%s" % scope, rs[OURS]["opt"], src)
        if 2 * rs[OURS]["opt"] <= rs[OURS]["n"]:
            raise Missing("SC3LOST %s: CUT-HIDE optimal on at most half of the instances; the text says most" % scope)
        nums.put("lostopt.chaw.opt.%s" % scope, rs["cut-hide-rw"]["opt"], src)
        nums.put("lostopt.chfw.opt.%s" % scope, rs["cut-hide"]["opt"], src)
        if not rs["cut-hide"]["opt"] < rs["cut-hide-rw"]["opt"] < rs[OURS]["opt"]:
            raise Missing("SC3LOST %s: the order CH-FW < CH-AW < CUT-HIDE does not hold" % scope)
        others = [r for m, r in rs.items() if m in BASELINES + RIVALS]
        if len(others) != 6:
            raise Missing("SC3LOST %s: %d other methods, expected 6" % (scope, len(others)))
        # The text names the methods at |R|* on every instance as exceptions, and gives the range of the rest.
        everywhere = sorted(m for m, r in rs.items() if m in BASELINES + RIVALS and r["at_R"] == r["n"])
        expected = ["hha-stop"] if scope == "distinct" else []
        if everywhere != expected:
            raise Missing("SC3LOST %s: methods at |R|* on every instance %s, the text names %s" % (scope, everywhere, expected))
        rest = [r for r in others if r["at_R"] < r["n"]]
        nums.put("lostopt.base.atR.min.%s" % scope, min(r["at_R"] for r in rest), src)
        nums.put("lostopt.base.atR.max.%s" % scope, max(r["at_R"] for r in rest), src)
        nums.put("lostopt.base.opt.min.%s" % scope, min(r["opt"] for r in others), src)
        nums.put("lostopt.base.opt.max.%s" % scope, max(r["opt"] for r in others), src)
        nums.put("lostopt.excess.ours.%s" % scope, rs[OURS]["excess"], src)
        nums.put("lostopt.excess.base.min.%s" % scope, min(r["excess"] for r in others), src)
        if not min(r["excess"] for r in others) > 10 * rs[OURS]["excess"]:
            raise Missing("SC3LOST %s: excess of CUT-HIDE not an order of magnitude below the others" % scope)
        above = [r for r in others if r["above"]]
        if scope == "distinct" and any(r["opt_no_worse"] != r["above"] for r in above):
            raise Missing("SC3LOST distinct: the optimum is worse than some method above |R|*; the text says never")
        if scope == "repeated":
            # Per method, the instances above |R|* where the optimum at |R|* loses MORE patterns.
            worse = [r["above"] - r["opt_no_worse"] for r in above]
            if min(worse) == 0:
                raise Missing("SC3LOST repeated: a method has no exception; the text says every method has some")
            nums.put("lostopt.optworse.repeated.min", min(worse), src)
            nums.put("lostopt.optworse.repeated.max", max(worse), src)


# ---------------------------------------------------------------- certified bound (SC3CERT)

def certified(nums, machine, small_machine):
    # The small instances of one machine need no second machine: cert_table reads them from the same files.
    extra = ["--small-machine=%s" % small_machine] if small_machine != machine else []
    text = run_tool(["cert_table.py", "--machine=%s" % machine] + extra, "cert_table")
    src = "cert_table.py --machine=%s%s" % (machine, (" " + extra[0]) if extra else "")
    for scope in ("distinct", "repeated"):
        body = text.split("== tested datasets, %s scope" % scope)[1].split("\n\n")[0]
        full = one(r"full data: (\d+) sets; certified optimal \(Lost = B\) (\d+)/\d+; B > 0 (\d+)/\d+", body, src)
        pos = one(r"with OPT_Lost > 0 \(where B <= OPT_Lost can fail\): (\d+); B = OPT_Lost (\d+)/\d+", body, src)
        nums.put("cert.full.n.%s" % scope, int(full[0]), src)
        nums.put("cert.full.certified.%s" % scope, int(full[1]), src)
        nums.put("cert.full.bpos.%s" % scope, int(full[2]), src)
        nums.put("cert.small.pos.n.%s" % scope, int(pos[0]), src)
        nums.put("cert.small.pos.hit.%s" % scope, int(pos[1]), src)
    # Not printed by cert_table: certified sets whose bound is positive, on the tested datasets.
    count = Counter()
    for path in files(INV, "SC3CERT", machine):
        for r in rows_of(path):
            if r["instance"] != "full" or r.get("status") or r.get("bound", "") == "":
                continue
            if dataset_name(r["dataset"]) in REPORT_ONLY:
                continue
            lost, bound = int(r["lost"]), int(r["bound"])
            count[(r["pattern_kind"], "n")] += 1
            count[(r["pattern_kind"], "cert")] += lost == bound
            count[(r["pattern_kind"], "cert_pos")] += lost == bound and bound > 0
            count[(r["pattern_kind"], "cert_zero")] += lost == bound and lost == 0
    src2 = "SC3CERT files of %s, full rows of the tested datasets" % machine
    for scope in ("distinct", "repeated"):
        if count[(scope, "n")] != nums.values["cert.full.n.%s" % scope]["value"]:
            raise Missing("SC3CERT %s: %d full rows read here, cert_table reports %d"
                          % (scope, count[(scope, "n")], nums.values["cert.full.n.%s" % scope]["value"]))
        if count[(scope, "cert")] != nums.values["cert.full.certified.%s" % scope]["value"]:
            raise Missing("SC3CERT %s: certified count differs from cert_table" % scope)
        nums.put("cert.full.certified_pos.%s" % scope, count[(scope, "cert_pos")], src2)
    if count[("distinct", "cert_pos")] != 0:
        raise Missing("SC3CERT distinct: a certified set has a positive bound; the text says the bound is zero")


# ---------------------------------------------------------------- SC3 rows, refinement

def positive_gap(gap):
    """g- > 0: the lower end of the gap "[a,b]" is above 0 (the definition of preregistered_analysis.py)."""
    return int(re.match(r"\[\s*(\d+)", gap).group(1)) > 0


def has_single_component(sensitive_set):
    """True when some pattern of a set of two or more shares no item with any other pattern of the set
    (a one-pattern component of the shared-item graph, where the refinement substitutes the single-pattern optimum)."""
    pats = [set(p) for p in ast.literal_eval(sensitive_set)]
    return len(pats) >= 2 and any(all(not (p & q) for j, q in enumerate(pats) if j != i) for i, p in enumerate(pats))


def check_equal_counts(masks):
    """With g- = 0, every instance where a baseline masks as many positions as CUT-HIDE has one sensitive pattern.

    masks: instance key -> method key -> set of mask counts. Every group is read, so the check is stronger than the
    sentence about the main group. Returns the number of equal-count pairs (all single-pattern)."""
    equal, several = 0, []
    for key, ms in masks.items():
        if positive_gap(key[1]) or OURS not in ms:
            continue
        for b in BASELINES:
            if b in ms and ms[b] == ms[OURS]:
                equal += 1
                if len(ast.literal_eval(key[4])) >= 2:
                    several.append((key, b))
    if several:
        raise Missing("%d equal-count pairs with |S| >= 2 (first %s); the text says every equal-count instance has one pattern"
                      % (len(several), several[0]))
    if not equal:
        raise Missing("no equal-count pair at g- = 0: the one-pattern check compared nothing")
    return equal


def check_refine_up(refine, ours, masks):
    """Every set where the refinement raises Lost has fewer masks after it and a one-pattern component.

    refine, ours: instance key -> set of Lost values; masks as in check_equal_counts. Returns the number of sets."""
    up, bad = 0, []
    for key, lost in refine.items():
        if next(iter(lost)) <= next(iter(ours[key])):
            continue
        up += 1
        before, after = masks[key].get(OURS, set()), masks[key].get(REFINE, set())
        if len(before) != 1 or len(after) != 1 or not next(iter(after)) < next(iter(before)) \
                or not has_single_component(key[4]):
            bad.append((key, sorted(before), sorted(after)))
    if bad:
        raise Missing("refinement raises Lost on %d sets without fewer masks or a one-pattern component (first %s)"
                      % (len(bad), bad[0]))
    return up


def sc3_rows(nums, machine):
    total, status = 0, Counter()
    by_gap = Counter()
    ours, refine = {}, {}
    masks = defaultdict(lambda: defaultdict(set))
    ghost_rows, params = 0, defaultdict(set)
    several = defaultdict(dict)
    for path in files(INV, "SC3", machine):
        opts = options_of(path)
        kind = opts.get("patternKind", "")
        for name in ("maxPatternLength", "timeLimit"):
            params[name].add(opts.get(name))
        for r in rows_of(path):
            total += 1
            status[r.get("status", "")] += 1
            g = "gpos" if positive_gap(r["gap"]) else "gzero"
            by_gap[("rows", g)] += 1
            by_gap[("method", g)] += bool(r.get("method_key"))
            if r.get("method_key") in ("cut-hide-rw-ls", "cut-hide-rw", "cut-hide") and r["sensitive_set"].count("[") > 2:
                k = (dataset_name(r["dataset"]), r["gap"], r["sigma"], r["sensitive_set"])
                several[k][r["method_key"]] = (r["mask_count"], r["lost"], r["lost_rel"], r["sup_dist"], r["dist_r"])
            if r.get("method_key"):
                ghost_rows += 1
                if float(r.get("ghost") or 0) != 0:
                    raise Missing("SC3 row with Ghost > 0 (%s, %s): masking cannot create a frequent pattern"
                                  % (Path(path).name, r["method_key"]))
            if r.get("method_key") and float(r.get("hf") or 0) != 0:
                raise Missing("SC3 row with HF > 0 (%s, %s): the text says every method hides on every row"
                              % (Path(path).name, r["method_key"]))
            key = (dataset_name(r["dataset"]), r["gap"], r["sigma"], kind, r["sensitive_set"])
            if r.get("method_key") and opts.get("methods", "") not in ("none",):
                masks[key][r["method_key"]].add(int(r["mask_count"]))
            if r["method_key"] == OURS and opts.get("methods", "") not in ("none",):
                ours.setdefault(key, set()).add(int(r["lost"]))
            elif r["method_key"] == REFINE:
                refine.setdefault(key, set()).add(int(r["lost"]))
    src = "SC3 files of %s (every batch)" % machine
    nums.put("sc3.n_rows", total, src)
    nums.put("sc3.ghost.rows", ghost_rows, src)
    nums.put("sc3.ghost.max", 0, src)
    # The text says CUT-HIDE, CH-AW and CH-FW return the same solution on every set with |S| >= 2 (they run the
    # same greedy). The rows hold measures, not positions: count the sets where all five measures agree.
    complete = {k: v for k, v in several.items() if len(v) == 3}
    differ = [k for k, v in complete.items() if len(set(v.values())) != 1]
    if not complete or differ:
        raise Missing("|S| >= 2: %d sets with the three CUT-HIDE keys, %d with different measures" % (len(complete), len(differ)))
    nums.put("several.same.n", len(complete), src)
    # The positive-gap batch (preregistration 22.3) added rows with g- > 0 to SC3; a sentence may count one part only.
    for g in ("gzero", "gpos"):
        nums.put("sc3.n_rows.%s" % g, by_gap[("rows", g)], src)
        nums.put("sc3.ghost.rows.%s" % g, by_gap[("method", g)], src)
        nums.put("several.same.n.%s" % g, sum(1 for k in complete if positive_gap(k[1]) == (g == "gpos")), src)
    for name, key in (("maxPatternLength", "params.l_max"), ("timeLimit", "params.time_limit_s")):
        if len(params[name]) != 1 or None in params[name]:
            raise Missing("SC3 files of %s run with %s = %s; the text states one value" % (machine, name, sorted(map(str, params[name]))))
        nums.put(key, int(params[name].pop()), src)
    nums.put("sc3.status", dict(status), src)
    nums.put("sc3.n_excluded", sum(v for k, v in status.items() if k), src)
    up = down = same = 0
    changes = []
    for key, lost in refine.items():
        if key not in ours:
            raise Missing("refinement row without a method-batch row: %s" % (key,))
        if len(lost) != 1 or len(ours[key]) != 1:
            raise Missing("several Lost values for one set: %s" % (key,))
        d = next(iter(lost)) - next(iter(ours[key]))
        changes.append(d)
        up += d > 0
        down += d < 0
    if not changes:
        raise Missing("no refinement row joined")
    nums.put("refine.n", len(changes), src)
    nums.put("refine.lost_up", up, src)
    nums.put("refine.lost_down", down, src)
    nums.put("refine.lost_median", statistics.median(changes), src)
    if check_refine_up(refine, ours, masks) != up:
        raise Missing("refinement: two counts of the sets where Lost rises differ")
    nums.put("sc3.equal_count.pairs", check_equal_counts(masks), src)


# ---------------------------------------------------------------- levels at each method's own solution

LEVEL_METHODS = (OURS,) + BASELINES + RIVALS


def parse_levels(text, src):
    rows = []
    for scope, size, n, body in re.findall(r"^== (distinct|repeated) scope, \|S\| = (1|5-20): (\d+) instances\n   method[^\n]*\n((?:   .*\n)+)",
                                           text, re.M):
        for line in body.splitlines():
            f = line.split()
            if len(f) != 13 or f[2] != "|" or f[10] != "|":
                raise Missing("levels_table: line not understood: %r" % line)
            if int(f[1]) != int(n):
                raise Missing("levels_table %s |S|=%s: %s has %s instances, the block %s" % (scope, size, f[0], f[1], n))
            rows.append({"scope": scope, "size": size, "method": f[0], "n": int(f[1]),
                         "lo": float(f[3]), "q1": float(f[4]), "median": float(f[5]), "q3": float(f[6]),
                         "hi": float(f[7]), "max": float(f[8]), "beyond": int(f[9]),
                         "dist_r": float(f[11]), "sup_dist": float(f[12])})
    for scope in ("distinct", "repeated"):
        for size in ("1", "5-20"):
            have = sorted(r["method"] for r in rows if (r["scope"], r["size"]) == (scope, size))
            if have != sorted(LEVEL_METHODS):
                raise Missing("levels_table %s |S|=%s: methods %s, expected %s" % (scope, size, have, sorted(LEVEL_METHODS)))
    return rows


def levels(nums, machine):
    text = run_tool(["levels_table.py", "--machine=%s" % machine], "levels_table")
    src = "levels_table.py --machine=%s" % machine
    rows = parse_levels(text, src)
    nums.table("levels", ["scope", "size", "method"], rows, src)
    for scope in ("distinct", "repeated"):
        nums.put("levels.n.%s" % scope, sum(r["n"] for r in rows if r["scope"] == scope and r["method"] == OURS), src)
        for size in ("1", "5-20"):
            block = {r["method"]: r for r in rows if (r["scope"], r["size"]) == (scope, size)}
            ours = block[OURS]
            others = [block[m] for m in LEVEL_METHODS if m != OURS]
            tag = "levels.%s.%s" % (scope, size.replace("-", "_"))
            # The text states that CUT-HIDE has the lowest median Lost_rel and SupDist in every block,
            # and with several sensitive patterns also the lowest median distortion rate.
            measures = ("median", "sup_dist") + (("dist_r",) if size != "1" else ())
            for field in measures:
                if any(o[field] <= ours[field] for o in others):
                    raise Missing("%s: a rival has a median %s not above CUT-HIDE; the text says lowest" % (tag, field))
            for field, name in (("median", "lost_rel"), ("dist_r", "dist_r"), ("sup_dist", "sup_dist")):
                nums.put("%s.ours.%s" % (tag, name), ours[field], src)
                nums.put("%s.rivals.%s.min" % (tag, name), min(o[field] for o in others), src)
                nums.put("%s.rivals.%s.max" % (tag, name), max(o[field] for o in others), src)


# ---------------------------------------------------------------- dataset-level sensitivity analysis

def parse_sensitivity(text, src):
    rows = []
    for test, scope, body in re.findall(r"^== (H2|H2-R|H4b|H4b-R) \((distinct|repeated) scope\), dataset as the unit[^\n]*\n   method[^\n]*\n((?:   .*\n)+)",
                                        text, re.M):
        for line in body.splitlines():
            f = line.split("|")[0].split()
            if len(f) != 8:
                raise Missing("dataset_sensitivity: line not understood: %r" % line)
            rows.append({"test": test, "scope": scope, "method": f[0], "datasets": int(f[1]), "above": int(f[2]),
                         "positive": int(f[3]), "n": int(f[4]), "p": float(f[5]), "p_holm": float(f[6]),
                         "min_p": float(f[7])})
    have = sorted({r["test"] for r in rows})
    if have != ["H2", "H2-R", "H4b", "H4b-R"]:
        raise Missing("dataset_sensitivity: tests %s" % have)
    return rows


def sensitivity(nums, machine):
    text = run_tool(["dataset_sensitivity.py", "--machine=%s" % machine], "dataset_sensitivity")
    src = "dataset_sensitivity.py --machine=%s" % machine
    rows = parse_sensitivity(text, src)
    nums.table("sensitivity", ["test", "method"], rows, src)
    for test in ("H2", "H2-R", "H4b", "H4b-R"):
        rs = [r for r in rows if r["test"] == test]
        k = "sens.%s" % test.lower().replace("-", "")
        nums.put(k + ".datasets.min", min(r["datasets"] for r in rs), src)
        nums.put(k + ".datasets.max", max(r["datasets"] for r in rs), src)
        nums.put(k + ".p_holm.min", min(r["p_holm"] for r in rs), src)
        nums.put(k + ".p.min", min(r["p"] for r in rs), src)
        nums.put(k + ".positive.min", min(r["positive"] for r in rs), src)
        nums.put(k + ".positive.max", max(r["positive"] for r in rs), src)
        nums.put(k + ".min_attainable_p", min(r["min_p"] for r in rs), src)
    # The text states one dataset count for H2-R and H4b-R together.
    counts = {test: sorted({r["datasets"] for r in rows if r["test"] == test}) for test in ("H2-R", "H4b-R")}
    if counts["H2-R"] != counts["H4b-R"]:
        raise Missing("dataset counts differ between H2-R %s and H4b-R %s; the text states one count" % (counts["H2-R"], counts["H4b-R"]))


# ---------------------------------------------------------------- lots D and E (preregistration section 23)

# The intervals that give a verdict: H3 and the H3 part of H6 judge the distinct-item scope (main and degenerate
# groups; the repeated-item ratios are descriptive), H5a and H4a judge the main group of both scopes.
JUDGED_INTERVALS = {("H3", "distinct"), ("H6", "distinct"), ("H5a", "distinct"), ("H5a", "repeated"), ("H4a", "distinct"),
                    ("H4a", "repeated")}


def judged(r):
    return (r["test"], r["scope"]) in JUDGED_INTERVALS and (r["group"] == "main" or r["test"] in ("H3", "H6"))


def reopened(nums, machine):
    """Lots D and E from tools/reopened_analysis.py, run on the tables built above so that its reproduction check
    compares with this build, not with an earlier export."""
    with tempfile.TemporaryDirectory() as tmp:
        tables = Path(tmp) / "tables.json"
        tables.write_text(json.dumps({"tables": nums.tables}), encoding="utf-8")
        report = Path(tmp) / "report.json"
        run_tool(["reopened_analysis.py", "--machine=%s" % machine, "--numbers", str(tables), "--json", str(report),
                  "--stdout"], "reopened_analysis")
        data = json.loads(report.read_text(encoding="utf-8"))
    src = "reopened_analysis.py --machine=%s" % machine
    # Lot D: the non-inferiority test without the equal-mask filter.
    rows = []
    for scope in ("distinct", "repeated"):
        by_method = data["lot_d"][scope]
        if sorted(by_method) != sorted(("hha", "dbsh", "sbsh-r1", "sbsh-rmax")):
            raise Missing("lot D %s: baselines %s" % (scope, sorted(by_method)))
        for m, v in by_method.items():
            c = v["cross"]
            rows.append({"scope": scope, "method": m, "n": v["n"], "median": v["median"], "p_holm": v["p_holm"],
                         "verdict": v["verdict"], "rb_shifted": v["rb_shifted"], "excluded": v["excluded"],
                         **{"cross_" + k.replace("/", "_"): c.get(k, 0) for k in
                            ("fewer/fewer", "fewer/same", "fewer/more", "same/fewer", "same/same", "same/more",
                             "more/fewer", "more/same", "more/more")}})
        ns = {v["n"] for v in by_method.values()}
        if len(ns) != 1:
            raise Missing("lot D %s: the baselines have different n %s; the text states one" % (scope, sorted(ns)))
        nums.put("noninf.%s.n" % scope, ns.pop(), src)
        rs = [r for r in rows if r["scope"] == scope]
        nums.put("noninf.%s.fewer_more.min" % scope, min(r["cross_fewer_more"] for r in rs), src)
        nums.put("noninf.%s.fewer_more.max" % scope, max(r["cross_fewer_more"] for r in rs), src)
        nums.put("noninf.%s.median.min" % scope, min(r["median"] for r in rs), src)
        nums.put("noninf.%s.median.max" % scope, max(r["median"] for r in rs), src)
        nums.put("noninf.%s.p_holm.max" % scope, max(r["p_holm"] for r in rs), src)
    nums.put("noninf.repeated.hha.more_more",
             next(r["cross_more_more"] for r in rows if r["scope"] == "repeated" and r["method"] == "hha"), src)
    nums.table("noninf", ["scope", "method"], rows, src)
    e = data["lot_e"]
    # Lot E (1): intervals.
    for r in e["intervals"]:
        base = "ci.%s.%s.%s.%s" % (r["test"].lower(), r["scope"], r["group"], r["method"])
        nums.put(base + ".lo", r["lo"], src)
        nums.put(base + ".hi", r["hi"], src)
    nums.table("ci", ["test", "scope", "group", "method"], e["intervals"], src)
    # Lot E (4): H4a by size.
    for r in e["h4a_size"]:
        nums.put("h4a.size.%s.%s.s%d" % (r["scope"], r["method"], r["size"]), r["median"], src)
    nums.table("h4a_size", ["scope", "method", "size"], e["h4a_size"], src)
    # Lot E (2): the datasets with equal-count instances in H2-R, the same for every baseline.
    sets = {}
    for r in e["per_dataset"]:
        if r["test"] == "H2-R" and r["n_diff"] > 0:
            sets.setdefault(r["method"], set()).add(r["dataset"])
    if len({tuple(sorted(s)) for s in sets.values()}) != 1 or len(sets) != 4:
        raise Missing("H2-R: the datasets with equal-count instances differ between baselines: %s; the text names one set"
                      % {k: sorted(v) for k, v in sets.items()})
    names = sorted(next(iter(sets.values())))
    nums.put("h2r.equal.datasets", len(names), src)
    nums.put("names.h2r.equal", ", ".join(names[:-1]) + " and " + names[-1], src)
    nums.table("per_dataset", ["test", "method", "dataset"], e["per_dataset"], src)
    nums.table("rb_raw", ["test", "method"], e["rb_raw"], src)
    # Lot E (5): tests, Holm families and equivalence verdicts of the tables of this build.
    nums.put("tests.total", sum(f["tests"] for f in e["families"]), src)
    nums.put("tests.families", len(e["families"]), src)
    nums.put("tests.equivalence", sum(f["equivalence"] for f in e["families"]), src)


# ---------------------------------------------------------------- SC1, SC2, datasets

def sc1(nums, machine):
    paths = files(INV, "SC1", machine)
    if len(paths) != 1:
        raise Missing("SC1 files of %s: %d, expected 1" % (machine, len(paths)))
    seen, checks, instances = set(), 0, 0
    src = "SC1 file %s" % Path(paths[0]).name
    for r in rows_of(paths[0]):
        if r["check"] in seen:
            continue                                # the file repeats the second half of its rows
        seen.add(r["check"])
        if r["kind"] == "check":
            if int(r["mismatches"]) != 0:
                raise Missing("SC1 check %r has %s mismatches" % (r["check"], r["mismatches"]))
            checks += 1
            instances += int(r["instances"])
        elif r["kind"] == "observation" and "deletion raised" in r["check"]:
            nums.put("sc1.ghost.n", int(r["mismatches"]), src)
            nums.put("sc1.ghost.of", int(r["instances"]), src)
    nums.put("sc1.n_checks", checks, src)
    nums.put("sc1.n_instances", instances, src)


def sc2(nums):
    for scope, name in (("distinct", "configuration_groups.tsv"), ("repeated", "configuration_groups_repeated.tsv")):
        path = ROOT / "config" / name
        rows = rows_tsv(path)
        groups = Counter(r["group"] for r in rows)
        src = "config/%s" % name
        nums.put("sc2.n_config.%s" % scope, len(rows), src)
        for g in ("main", "intermediate", "degenerate"):
            nums.put("sc2.%s.%s" % (g, scope), groups.get(g, 0), src)


def rows_tsv(path):
    with open(path, encoding="utf-8") as f:
        lines = [l for l in f if not l.startswith("#")]
    return list(csv.DictReader(lines, delimiter="\t"))


def datasets_table(nums):
    groups = defaultdict(lambda: {"distinct": Counter(), "repeated": Counter(), "configs": set()})
    for scope, name in (("distinct", "configuration_groups.tsv"), ("repeated", "configuration_groups_repeated.tsv")):
        for r in rows_tsv(ROOT / "config" / name):
            groups[r["dataset"]][scope][r["group"]] += 1
            groups[r["dataset"]]["configs"].add((r["gap"], r["sigma"]))
    rows = []
    for name in sorted(groups):
        path = ROOT / "datasets" / ("%s.txt" % name)
        if not path.exists():
            raise Missing("dataset file %s is missing (fetch it with tools/fetch_datasets.py)" % path)
        seqs, events, items = 0, 0, set()
        with open(path, encoding="utf-8") as f:
            for line in f:
                toks = [t for t in line.split() if t not in ("-1", "-2")]
                if not toks or line.startswith(("#", "@")):
                    continue
                seqs += 1
                events += len(toks)
                items.update(toks)
        g = groups[name]
        rows.append({"dataset": name, "sequences": seqs, "mean_length": events / seqs, "items": len(items),
                     "gaps": sorted({c[0] for c in g["configs"]}), "sigmas": sorted({int(c[1]) for c in g["configs"]}),
                     "distinct": dict(g["distinct"]), "repeated": dict(g["repeated"]),
                     "file_sha256": sha12(path)})
    nums.table("datasets", ["dataset"], rows, "datasets/*.txt and the two locked group tables")


# ---------------------------------------------------------------- timing and platform

def timing(nums, machine):
    text = run_tool(["timing_table.py", "--machine=%s" % machine], "timing_table")
    src = "timing_table.py --machine=%s" % machine
    head = one(r"^SC6 files of \S+: (\d+) \(one mining row checked in (\d+)\); timed rows (\d+), joined with an SC3 row (\d+)",
               text, src)
    head = [int(x) for x in head]
    if head[0] != head[1] or head[2] != head[3]:
        raise Missing("timing_table header %s: files and mining rows, or timed and joined rows, differ" % head)
    nums.put("sc6.n_cells", head[0], src)
    nums.put("sc6.n_timed", head[2], src)
    rows = []
    for scope, size, body in re.findall(r"^== (distinct|repeated) scope, \|S\| = (\d+)\n   method[^\n]*\n((?:   .*\n)+)", text, re.M):
        for line in body.splitlines():
            m = re.match(r"^   (\S+?)(\*?)\s+(\d+)\s+([\d.]+)(?:\s+\(cells, not sets\))?\s*$", line)
            if m:
                rows.append({"scope": scope, "size": int(size), "method": m.group(1), "reads_frequent": m.group(2) == "*",
                             "n": int(m.group(3)), "median_ms": float(m.group(4))})
                continue
            m = re.match(r"^   mine F_sigma\s+(\d+)\s+([\d.]+)\s+\(cells, not sets\)\s*$", line)
            if m:
                rows.append({"scope": scope, "size": int(size), "method": "mine F_sigma", "reads_frequent": False,
                             "n": int(m.group(1)), "median_ms": float(m.group(2))})
                continue
            m = re.match(r"^   index: build median ([\d.]+) ms over \d+ sets; rebuild / incremental median ([\d.]+)", line)
            if m:
                rows.append({"scope": scope, "size": int(size), "method": "index", "build_ms": float(m.group(1)),
                             "rebuild_over_incremental": float(m.group(2))})
                continue
            raise Missing("timing_table: line not understood: %r" % line)
    blocks = {(r["scope"], r["size"]) for r in rows}
    if len(blocks) != 4:
        raise Missing("timing_table: %d blocks, expected 4" % len(blocks))
    nums.table("timing", ["scope", "size", "method"], rows, src)
    for key, pick in (("mine", lambda r: r["method"] == "mine F_sigma"), ("ours", lambda r: r["method"] == OURS),
                      ("holm", lambda r: r["method"] in ("hha", "dbsh") + RIVALS)):
        for size in (1, 10):
            vals = [r["median_ms"] for r in rows if r["size"] == size and pick(r)]
            nums.put("sc6.%s.ms.s%d.min" % (key, size), min(vals), src)
            nums.put("sc6.%s.ms.s%d.max" % (key, size), max(vals), src)
    # The conclusion compares CUT-HIDE with HHA as a ratio of the two medians of each block.
    ratios = []
    for (scope, size) in sorted(blocks):
        b = {r["method"]: r for r in rows if (r["scope"], r["size"]) == (scope, size)}
        ratios.append(b[OURS]["median_ms"] / b["hha"]["median_ms"])
    nums.put("sc6.ratio.ours_hha.min", min(ratios), src)
    nums.put("sc6.ratio.ours_hha.max", max(ratios), src)
    index = [r for r in rows if r["method"] == "index"]
    if len(index) != 4:
        raise Missing("timing_table: %d index lines, expected 4" % len(index))
    nums.put("sc6.index.build_ms.min", min(r["build_ms"] for r in index), src)
    nums.put("sc6.index.build_ms.max", max(r["build_ms"] for r in index), src)
    nums.put("sc6.index.ratio.min", min(r["rebuild_over_incremental"] for r in index), src)
    nums.put("sc6.index.ratio.max", max(r["rebuild_over_incremental"] for r in index), src)
    if min(r["rebuild_over_incremental"] for r in index) <= 1:
        raise Missing("timing_table: the incremental update is not faster than a rebuild in some block")
    for (scope, size) in blocks:
        b = {r["method"]: r for r in rows if (r["scope"], r["size"]) == (scope, size)}
        if not b["mine F_sigma"]["median_ms"] > b[OURS]["median_ms"]:
            raise Missing("timing %s |S|=%d: mining is not longer than CUT-HIDE on one set" % (scope, size))
    reps = Counter()
    cv = []
    for path in files(TIMED, "SC6", machine):
        for r in rows_of(path):
            reps[int(r["repetitions"])] += 1
            if r.get("wall_ms_sd") and float(r["wall_ms_mean"]) > 0:
                cv.append(float(r["wall_ms_sd"]) / float(r["wall_ms_mean"]))
            if r["warmups"] != "1":
                raise Missing("SC6 row with %s warm-up runs" % r["warmups"])
    nums.put("sc6.repetitions.min", min(reps), "SC6 files of %s" % machine)
    nums.put("sc6.repetitions.max", max(reps), "SC6 files of %s" % machine)
    if len(cv) != sum(reps.values()):
        raise Missing("SC6: %d rows carry a standard deviation, %d rows in all" % (len(cv), sum(reps.values())))
    nums.put("sc6.cv.median", statistics.median(cv), "SC6 files of %s: wall_ms_sd / wall_ms_mean" % machine)


MACHINE_KEYS = ("cpu", "logical_processors", "ram_bytes", "os", "java_vendor", "java_runtime_version", "hostname")


def locks(machine):
    """The environment lock of every generation of this machine, oldest first."""
    first = sorted(INV.glob("campaign_%s/ENV_LOCK.json" % machine))
    later = sorted(INV.glob("campaign_%s_g*/ENV_LOCK.json" % machine),
                   key=lambda p: int(re.search(r"_g(\d+)$", p.parent.name).group(1)))
    return first + later


def one_platform(read, machine):
    """The newest of the locks, once every lock agrees on the machine it describes.

    The paper states one measuring environment. Each generation locks its own copy, so a key that
    differs between two generations means the environment moved under the campaign and the sentence
    would describe only part of the numbers.
    """
    if not read:
        raise Missing("no environment lock of %s" % machine)
    for k in MACHINE_KEYS:
        seen = {d.get(k) for _, d in read}
        if len(seen) != 1:
            raise Missing("the %d environment locks of %s disagree on %s: %s; the paper states one platform"
                          % (len(read), machine, k, sorted(map(str, seen))))
    return read[-1]


def platform(nums, machine):
    read = [(p, json.loads(p.read_text(encoding="utf-8"))) for p in locks(machine)]
    path, lock = one_platform(read, machine)
    src = path.relative_to(ROOT).as_posix()
    nums.put("platform.n_locks", len(read), "%s/campaign_%s*/ENV_LOCK.json" % (INV.name, machine))
    nums.put("platform.lock", "%s/%s" % (path.parent.name, path.name), src)
    for k in MACHINE_KEYS:
        nums.put("platform.%s" % k, lock[k], src)
    heaps = Counter()
    for tree, scenario in ((TIMED, "SC6"), (INV, "SC3")):
        for path in files(tree, scenario, machine):
            # The Pareto-front batch (methods=none) runs apart, without a time limit and with its own heap.
            kind = "SC3PARETO" if scenario == "SC3" and options_of(path).get("methods") == "none" else scenario
            with open(path, encoding="utf-8") as f:
                for line in f:
                    if line.startswith("# heap_max_bytes "):
                        heaps[(kind, int(line.split()[2]))] += 1
                        break
    sc6 = {h for (s, h) in heaps if s == "SC6"}
    if len(sc6) != 1:
        raise Missing("SC6 files run with %d heap sizes: %s" % (len(sc6), heaps))
    nums.put("platform.heap_bytes.sc6", sc6.pop(), "heap_max_bytes of the SC6 files")
    # The Pareto fronts exist under the earlier rule only (see build); their heap is read with them.
    kinds = [("SC3", "platform.heap_bytes.sc3")]
    if RULE[0] == "subsequence":
        kinds.append(("SC3PARETO", "platform.heap_bytes.pareto"))
    elif any(s == "SC3PARETO" for (s, h) in heaps):
        raise Missing("Pareto-front files under closure rule %s: %s; the fronts are read under the earlier rule only"
                      % (RULE[0], heaps))
    for kind, key in kinds:
        sizes = {h for (s, h) in heaps if s == kind}
        if len(sizes) != 1:
            raise Missing("%s files run with %d heap sizes: %s; the text states one" % (kind, len(sizes), heaps))
        nums.put(key, sizes.pop(), "heap_max_bytes of the %s files" % kind)


# ---------------------------------------------------------------- main

TIMING_PREFIXES = ("sc6.", "platform.")


def build(machine, small_machine):
    nums = Numbers()
    # Without files of the rule asked for, every later step would fail on a symptom; say the cause first.
    for scenario, who in (("SC3", machine), ("SC3LOST", small_machine), ("SC3CERT", machine), ("SC3OPT", machine),
                          ) + ((("SC4", machine), ("SC4LOST", small_machine)) if RULE[0] == "necessary" else ()):
        if not files(INV, scenario, who):
            every = [q for q in glob.glob(str(INV / ("%s_*.csv" % scenario))) if machine_of(q) == who]
            raise Missing("no %s file of %s under closure rule %s (rules found: %s)%s" % (
                scenario, who, RULE[0], closure_rule.keep(every, RULE[0])[1] or "none",
                "; the files of the corrected rule come from generation 3 of the campaign" if RULE[0] == "necessary" else ""))
    for step in [lambda: sc1(nums, machine), lambda: sc2(nums), lambda: datasets_table(nums),
                 lambda: sc3_rows(nums, machine), lambda: analysis(nums, machine), lambda: levels(nums, machine),
                 lambda: sensitivity(nums, machine),
                 lambda: lower_bounds(nums, machine),
                 lambda: reduced_optimum(nums, machine),
                 # The fronts exist under the earlier rule only, and the manuscript quotes none of their numbers.
                 lambda: pareto(nums, machine) if RULE[0] == "subsequence" else None,
                 lambda: lost_optimum(nums, small_machine), lambda: certified(nums, machine, small_machine),
                 lambda: timing(nums, machine), lambda: platform(nums, machine),
                 # HIDE-SEQ and g- > 0 were run under the corrected rule only (preregistration 22).
                 ] + ([lambda: seq_lost(nums, small_machine), lambda: sc1gap(nums, machine),
                       lambda: sc4_rows(nums, machine)] + [
                     (lambda m=m: analysis_mode(nums, machine, *m)) for m in NEW_MODES] +
                    [lambda: reopened(nums, machine)] if RULE[0] == "necessary" else []):
        step()
    # The same quantity read from two tools must agree.
    for scope, key in (("distinct", "h2.n"), ("repeated", "h2r.n")):
        if nums.values["levels.n.%s" % scope]["value"] != nums.values[key]["value"]:
            raise Missing("levels_table %s: %d instances, the pairs table %d" % (
                scope, nums.values["levels.n.%s" % scope]["value"], nums.values[key]["value"]))
    if RULE[0] == "necessary" and nums.values["sc4.nomask_rows"]["value"] != \
            nums.values["h5.nomask.distinct"]["value"] + nums.values["h5.nomask.repeated"]["value"]:
        raise Missing("SC4: %d no-mask rows read here, the analysis counts %d + %d" % (
            nums.values["sc4.nomask_rows"]["value"], nums.values["h5.nomask.distinct"]["value"],
            nums.values["h5.nomask.repeated"]["value"]))
    if RULE[0] == "necessary":
        sentence_checks(nums)
    a, b = nums.values["sc3.n_excluded"]["value"], sum(v["value"][0] for k, v in nums.values.items()
                                                        if k.startswith("excluded."))
    if b != 0 and a == 0:
        raise Missing("the analysis excluded %d instances while no SC3 row carries a status" % b)
    return nums


# The thresholds of H3 and the margin of H2, as locked in tools/preregistered_analysis.py (read, not copied).
def locked_constants():
    sys.path.insert(0, str(TOOLS))
    import preregistered_analysis as pa
    return pa.LOCKED.R_ABOVE_1, pa.LOCKED.R_NEAR_1, pa.LOCKED.DELTA_L


def sentence_checks(nums):
    """Sentences of the manuscript that state a relation between numbers rather than a number.

    Each check reads values already exported and stops when the relation does not hold, so that a new
    result file cannot silently contradict the wording. Under the corrected closure rule only (HIDE-SEQ and
    g- > 0 exist under it only)."""
    v = lambda k: nums.values[k]["value"]
    tab = lambda name: nums.tables[name]["rows"]
    above, near, delta = locked_constants()
    # At equal masking count CUT-HIDE loses more patterns than a baseline on exactly one instance, against HHA
    # in the distinct-item scope of HIDE-SEQ; no such instance elsewhere (HIDE-DB with g- = 0 and g- > 0, the
    # rivals, several patterns).
    worse = [(name, r["scope"], r["method"], r["same_gt"]) for name in ("h2", "h5", "h6", "h5s5", "h6sev")
             for r in tab(name) if r["same_gt"]]
    if worse != [("h5", "distinct", "hha", 1)] or v("h5.distinct.hha.same_gt") != 1:
        raise Missing("equal masking count, CUT-HIDE losing more: %s; the text names one instance, HHA, HIDE-SEQ, "
                      "distinct-item scope" % worse)
    # HIDE-SEQ, one pattern: no baseline uses fewer masks than CUT-HIDE on any instance.
    if any(r["ours_more"] for r in tab("h5")):
        raise Missing("HIDE-SEQ, one pattern: a baseline uses fewer masks than CUT-HIDE; the text says none")
    # g- > 0, several distinct-item patterns: every baseline uses more masks on every instance.
    if any(r["base_more"] != r["instances"] for r in tab("h6sev") if r["scope"] == "distinct"):
        raise Missing("g- > 0, several distinct-item patterns: a baseline does not use more masks everywhere")
    # HIDE-SEQ, five patterns: no instance has equal counts in either scope (the text gives this as the reason no test
    # applies); in the repeated-item scope HHA is the only baseline that uses fewer masks than CUT-HIDE on some instance;
    # some baseline uses fewer somewhere (the limitation).
    same = max(r["same"] for r in tab("h5s5"))
    if same != 0 or v("h5s5.repeated.same.max") != 0:
        raise Missing("HIDE-SEQ, five patterns: %d equal-count instances; the text says no instance has equal counts" % same)
    fewer = sorted(r["method"] for r in tab("h5s5") if r["scope"] == "repeated" and r["ours_more"])
    if fewer != ["hha"]:
        raise Missing("HIDE-SEQ, five repeated-item patterns: baselines with fewer masks than CUT-HIDE on some instance %s; "
                      "the text names HHA only" % fewer)
    if not any(r["ours_more"] for r in tab("h5s5")):
        raise Missing("HIDE-SEQ, five patterns: no baseline uses fewer masks than CUT-HIDE; the limitation says some does")
    # The abstract names that baseline once and then times CUT-HIDE against it: it must be HHA, the key of the ratio.
    rep5 = {r["method"]: r["ours_more"] for r in tab("h5s5") if r["scope"] == "repeated"}
    if max(rep5, key=rep5.get) != "hha" or sorted(rep5.values())[-2] == rep5["hha"]:
        raise Missing("HIDE-SEQ, five repeated-item patterns: the baseline with most instances of fewer masks is not HHA "
                      "alone (%s); the abstract times CUT-HIDE against HHA as that baseline" % rep5)
    # Ratios |R|/|R|*: one distinct-item pattern is near the minimum under HIDE-DB (at most the near-1 threshold of
    # H3) and below the more-masks threshold under HIDE-SEQ and with g- > 0; in the repeated-item scope of HIDE-SEQ,
    # DBSH and both SBSH variants reach that threshold while HHA stays below it.
    # One distinct-item pattern, both problems: every baseline uses few masks beyond the minimum (all three scenarios
    # below the more-masks threshold of H3).
    few = {k: v(k) for k in ("h3.main.median.max", "h5.ratio.distinct.median.max", "h6.ratio.distinct.median.max")}
    if not max(few.values()) < above:
        raise Missing("one distinct-item pattern: median ratios %s, not all below %.2f; the abstract says few masks beyond "
                      "the minimum" % (few, above))
    if not v("h3.main.median.max") <= near:
        raise Missing("H3, one distinct-item pattern: median ratio %.4f above %.2f; the text says close to the minimum"
                      % (v("h3.main.median.max"), near))
    for key in ("h5.ratio.distinct.median.max", "h6.ratio.distinct.median.max"):
        if not v(key) < above:
            raise Missing("%s = %.4f, not below the threshold %.2f of H3" % (key, v(key), above))
    reach = {m: v("h5.ratio.repeated.%s" % m) >= above for m in BASELINES}
    if reach != {"hha": False, "dbsh": True, "sbsh-r1": True, "sbsh-rmax": True}:
        raise Missing("HIDE-SEQ, repeated-item: methods reaching %.2f: %s; the text names DBSH and both SBSH" % (above, reach))
    # Exact optimum: CUT-HIDE is optimal on more instances than every other method under HIDE-SEQ, and its share of
    # optimal instances is lowest under HIDE-SEQ in the repeated-item scope ("its least favorable setting").
    for scope in ("distinct", "repeated"):
        rs = {r["method"]: r for r in tab("seqlost") if r["scope"] == scope}
        if any(r["opt"] >= rs[OURS]["opt"] for m, r in rs.items() if m != OURS):
            raise Missing("SC4LOST %s: a method is optimal on as many instances as CUT-HIDE; the text says more" % scope)
    # The excess of CUT-HIDE in tab_lostopt is summed over at least as many instances as that of any other method.
    for scope in ("distinct", "repeated"):
        rs = {r["method"]: r for r in tab("lostopt") if r["scope"] == scope}
        if any(r["at_R"] > rs[OURS]["at_R"] for r in rs.values()):
            raise Missing("SC3LOST %s: a method is at |R|* on more instances than CUT-HIDE; the text says at least as many" % scope)
    share = {(t, s): v("%s.ours.opt.%s" % (t, s)) / v("%s.n.%s" % (t, s))
             for t in ("lostopt", "seqlost") for s in ("distinct", "repeated")}
    if min(share, key=share.get) != ("seqlost", "repeated"):
        raise Missing("the lowest share of exact optima of CUT-HIDE is %s, not HIDE-SEQ repeated-item" % (min(share, key=share.get),))
    # Medians of Lost_rel of the other methods with one sensitive pattern are of the order of the margin: at most twice.
    for scope in ("distinct", "repeated"):
        key = "levels.%s.1.rivals.lost_rel.max" % scope
        if not v(key) <= 2 * delta:
            raise Missing("%s = %.4f, above twice the margin %.2f; the text says the same order" % (key, v(key), delta))
    # Tests: across HIDE-DB (g- = 0, g- > 0) and HIDE-SEQ, the null hypothesis is rejected exactly for H2-R against
    # HHA and for H4b-R against both rivals.
    rejected = sorted((r["test"], r["method"]) for name in ("h2", "h5", "h6") for r in tab(name)
                      if r["verdict"] == "H0 rejected")
    if rejected != [("H2-R", "hha"), ("H4b-R", "hha-stop"), ("H4b-R", "occurrence")]:
        raise Missing("rejected null hypotheses %s; the text names H2-R against HHA and H4b-R against both rivals" % rejected)
    # The conclusion names these rejections "against HHA and OCC": the HHA-stop row of H4b-R must coincide with the HHA
    # row of H2-R on every column of the test except the Holm-adjusted p, which is taken within another family.
    h2r = {r["method"]: r for r in tab("h2") if r["scope"] == "repeated"}
    cols = ("same", "median_diff", "p_raw", "rank_biserial", "hl_lost", "eq_lost", "hl_supdist", "eq_supdist",
            "same_lt", "same_eq", "same_gt")
    apart = [c for c in cols if h2r["hha-stop"][c] != h2r["hha"][c]]
    if apart:
        raise Missing("H4b-R HHA-stop and H2-R HHA differ on %s; the conclusion counts them as one result" % apart)
    # Under HIDE-DB the baselines exceed the minimum count once patterns repeat items: every median ratio of the
    # repeated-item scope at or above the more-masks threshold of H3.
    if not v("h3r.main.median.min") >= above:
        raise Missing("H3, repeated-item: median ratio %.4f below %.2f; the conclusion says the baselines exceed the "
                      "minimum" % (v("h3r.main.median.min"), above))
    # ... or several patterns are hidden: at every |S| >= 2 and in both scopes, the median ratio to LB_U of every
    # baseline is above that of CUT-HIDE, whose count is itself at least the minimum.
    for scope in ("distinct", "repeated"):
        for size in sorted({r["size"] for r in tab("bound") if r["size"] > 1}):
            rs = {r["method"]: r["median"] for r in tab("bound") if r["scope"] == scope and r["size"] == size}
            low = [m for m in BASELINES if not rs[m] > rs[OURS]]
            if low:
                raise Missing("several patterns, %s |S|=%d: %s not above CUT-HIDE in median ratio to LB_U; the conclusion "
                              "says the baselines exceed the minimum" % (scope, size, low))
    # Lot E (3): at equal masks CUT-HIDE never loses more in SC3, so the rank-biserial on the raw differences is 1 in
    # every row of table h2; the text says so instead of a column.
    off = [(r["test"], r["method"], r["rb_raw"]) for r in tab("rb_raw") if r["rb_raw"] != 1.0]
    if off or any(r["same_gt"] for r in tab("h2")):
        raise Missing("raw rank-biserial not 1 in %s, or an SC3 instance where CUT-HIDE loses more at equal masks; the "
                      "text says it never does" % off)
    # Lot E (1): exactly four intervals of a verdict cross a threshold of H3 (1.05 or 1.10).
    def zone(x):
        return 2 if x >= above else (0 if x <= near else 1)
    crossing = sorted((r["test"], r["scope"], r["method"]) for r in tab("ci") if judged(r)
                      and len({zone(r["lo"]), zone(r["median"]), zone(r["hi"])}) > 1)
    wanted = [("H4a", "distinct", "occurrence"), ("H5a", "distinct", "hha-emb"), ("H5a", "distinct", "sbsh-r1"),
              ("H5a", "distinct", "sbsh-rmax")]
    if crossing != wanted:
        raise Missing("intervals crossing a threshold: %s; the text names %s" % (crossing, wanted))
    # Lot E (4): with one distinct-item pattern both stopping rivals use exactly the masks of CUT-HIDE (median 1).
    ones = [v("h4a.size.distinct.%s.s1" % c) for c in ("occurrence", "hha-stop")]
    if ones != [1.0, 1.0]:
        raise Missing("H4a, one distinct-item pattern: medians %s; the text says both equal 1" % ones)
    # Lot D: the null hypothesis of non-inferiority is rejected for every baseline in both scopes, and on most instances
    # (more than half of every row) CUT-HIDE uses fewer masks and loses fewer patterns.
    kept = [(r["scope"], r["method"]) for r in tab("noninf") if r["verdict"] != "H0 rejected"]
    if kept or len(tab("noninf")) != 8:
        raise Missing("lot D: not rejected for %s (of %d rows); the text says rejected for every baseline in both scopes"
                      % (kept, len(tab("noninf"))))
    few = [(r["scope"], r["method"], r["cross_fewer_fewer"], r["n"]) for r in tab("noninf")
           if not 2 * r["cross_fewer_fewer"] > r["n"]]
    if few:
        raise Missing("lot D: fewer masks and fewer patterns on at most half of the instances in %s; the text says most" % few)
    # Lot E (2): in the distinct-item scope the median difference of every dataset with equal-count instances is below
    # delta_L, for every baseline.
    high = [(r["method"], r["dataset"], r["median_diff"]) for r in tab("per_dataset")
            if r["scope"] == "distinct" and r["n_diff"] > 0 and not r["median_diff"] < delta]
    if high or not any(r["scope"] == "distinct" and r["n_diff"] > 0 for r in tab("per_dataset")):
        raise Missing("lot E: a distinct-item dataset at or above delta_L: %s; the text says every one is below" % high)
    # Lot E (4): the ratio of OCC in the distinct-item scope grows with the size of the sensitive set.
    occ = [v("h4a.size.distinct.occurrence.s%d" % s) for s in (1, 5, 10, 20)]
    if not all(a < b for a, b in zip(occ, occ[1:])):
        raise Missing("H4a, OCC, distinct-item scope: ratios %s by size do not grow; the text says they do" % occ)
    # Lot E (2): the names of the H2-R datasets are those with equal-count instances against HHA in the per-dataset table.
    named = v("names.h2r.equal").replace(" and ", ", ").split(", ")
    shown = sorted(r["dataset"] for r in tab("per_dataset") if r["scope"] == "repeated" and r["method"] == "hha"
                   and r["n_diff"] > 0)
    if sorted(named) != shown or v("h2r.equal.datasets") != len(shown):
        raise Missing("H2-R datasets named %s, the per-dataset table shows %s" % (named, shown))
    nums.put("sentence_checks.passed", 27, "sentence_checks in paper_numbers.py")


def git(*args):
    p = subprocess.run(["git"] + list(args), cwd=ROOT, capture_output=True, text=True)
    return p.stdout.strip()


def write(nums, out_dir, machine, small_machine, argv):
    meta = {"command": "python tools/paper_numbers.py " + " ".join(argv), "commit": git("rev-parse", "HEAD"),
            "tree": "clean" if not git("status", "--porcelain") else "MODIFIED (%d files)" % len(
                git("status", "--porcelain").splitlines()),
            "tool_sha256": sha12(__file__), "python": sys.version.split()[0], "machine": machine,
            "small_machine": small_machine, "closure": RULE[0]}
    inv = {"_meta": meta, "values": {k: v for k, v in sorted(nums.values.items()) if not k.startswith(TIMING_PREFIXES)},
           "tables": {k: v for k, v in sorted(nums.tables.items()) if k != "timing"}}
    tim = {"_meta": meta, "values": {k: v for k, v in sorted(nums.values.items()) if k.startswith(TIMING_PREFIXES)},
           "tables": {k: v for k, v in nums.tables.items() if k == "timing"}}
    paths = []
    for data, sub, name in ((inv, "results-invariant", "paper_numbers.json"), (tim, "results", "paper_numbers_timing.json")):
        target = (Path(out_dir) / sub) if out_dir else (ROOT / sub)
        target.mkdir(parents=True, exist_ok=True)
        p = target / name
        p.write_text(json.dumps(data, indent=1, ensure_ascii=False, sort_keys=False) + "\n", encoding="utf-8", newline="\n")
        paths.append(p)
    return paths


def main(argv):
    if "--self-test" in argv:
        return self_test()
    opts = dict(a[2:].split("=", 1) for a in argv if a.startswith("--") and "=" in a)
    machine = opts.get("machine", "ADMIN-PC")
    rule = opts.get("closure", "necessary")
    if rule not in closure_rule.RULES:
        print("paper_numbers: --closure is one of %s" % ", ".join(closure_rule.RULES), file=sys.stderr)
        return 1
    RULE[0] = rule
    # Under the earlier rule the small instances were rerun on the development machine (log entries 57-58).
    small = opts.get("small-machine", "dev" if rule == "subsequence" else machine)
    try:
        nums = build(machine, small)
    except Missing as e:
        print("paper_numbers: %s" % e, file=sys.stderr)
        return 1
    for p in write(nums, opts.get("out-dir"), machine, small, argv):
        print("wrote %s" % p)
    print("%d values, %d tables" % (len(nums.values), len(nums.tables)))
    return 0


def self_test():
    """Each check feeds a broken input to one parser and requires it to stop."""
    failures = []

    def expect_stop(name, fn):
        try:
            fn()
        except Missing:
            return
        failures.append(name)

    n = Numbers()
    n.put("a", 1, "x")
    expect_stop("same key, two values", lambda: n.put("a", 2, "y"))
    expect_stop("pattern absent", lambda: one(r"^nothing here$", "text\n", "t"))
    expect_stop("pattern twice", lambda: one(r"^x$", "x\nx\n", "t"))
    good = ("-- hha\n   into Wilcoxon (same |R|)   10 instances\n   baseline uses more masks   5 instances  (r)\n"
            "   CUT-HIDE uses more masks   0 instances  (a)\n   excluded   0 instances\n"
            "   median difference of Lost_rel        +0.0048   (absolute: +1.0 patterns)\n"
            "   Wilcoxon-Pratt one-sided, H1 > 0.02  W = 1.0, p = 1, n = 10 (0 differences of 0)\n"
            "   rank-biserial                        -0.792\n\n"
            "  hha                     1            1   not rejected\n"
            "  hha              15 |      4      6      0 |      5      0      0 |         0     15/15\n"
            "-- lost_rel, equivalence margin +/- 0.02\n   hha            10     0.0073 [ +0.0049,  +0.0095]   equivalent\n\n"
            "-- sup_dist, equivalence margin +/- 0.01\n   hha            10     0.0009 [ +0.0005,  +0.0013]   equivalent\n")
    rows = parse_family(good, ("hha",), "T", Numbers(), "distinct")
    if rows[0]["same"] != 10 or rows[0]["no_worse_both"] != 15:
        failures.append("good family block misread: %s" % rows[0])
    expect_stop("pairs disagree with test block",
                lambda: parse_family(good.replace("      4      6      0 |", "      4      5      0 |"), ("hha",), "T", Numbers(), "d"))
    expect_stop("Holm line missing", lambda: parse_family(good.replace("not rejected", "maybe"), ("hha",), "T", Numbers(), "d"))
    expect_stop("equivalence line missing",
                lambda: parse_family(good.replace("-- sup_dist", "-- other"), ("hha",), "T", Numbers(), "d"))
    head = "   method               n |       lo       q1   median       q3       hi      max beyond |   dist_r sup_dist\n"
    line = "   %-16s %5d | 0.0000 0.0000 %.4f 0.0100 0.0200 0.3000 3 | %.4f %.4f\n"
    lv = ""
    for scope in ("distinct", "repeated"):
        for size in ("1", "5-20"):
            lv += "== %s scope, |S| = %s: 10 instances\n" % (scope, size) + head
            lv += "".join(line % (m, 10, 0.001 if m == OURS else 0.01, 0.01, 0.01 if m == OURS else 0.02)
                          for m in LEVEL_METHODS)
    if len(parse_levels(lv, "t")) != 4 * len(LEVEL_METHODS):
        failures.append("good levels text misread")
    expect_stop("levels: a method missing", lambda: parse_levels(lv.replace("   hha-stop", "   other   ", 1), "t"))
    expect_stop("levels: instance count of a row differs", lambda: parse_levels(lv.replace(" hha                 10 ", " hha                 11 ", 1), "t"))
    # one_platform: every generation's lock must agree, and the newest one fills the sentence.
    same = {k: "v" for k in MACHINE_KEYS}
    pair = [(Path("campaign_X/ENV_LOCK.json"), dict(same)), (Path("campaign_X_g2/ENV_LOCK.json"), dict(same))]
    def stop_message(fn):
        try:
            fn()
        except Missing as e:
            return str(e)
        return ""
    if "no environment lock" not in stop_message(lambda: one_platform([], "X")):
        failures.append("the empty case does not name the missing lock")
    expect_stop("two locks disagree on the JDK",
                lambda: one_platform([pair[0], (pair[1][0], dict(same, java_runtime_version="other"))], "X"))
    expect_stop("two locks disagree on the machine name",
                lambda: one_platform([pair[0], (pair[1][0], dict(same, hostname="OTHER"))], "X"))
    if one_platform(pair, "X")[0].parent.name != "campaign_X_g2":
        failures.append("one_platform does not take the newest lock")
    # The readers of HIDE-SEQ and g- > 0 are tested on the real reports, each with one fault injected.
    # Each injection must change the text exactly once, or the check would test nothing.
    def inject(text, old, new, name):
        if text.count(old) < 1:
            failures.append("%s: the text to change is absent" % name)
            return text
        changed = text.replace(old, new, 1)
        if changed == text:
            failures.append("%s: the injection changed nothing" % name)
        return changed

    machine = "ADMIN-PC"
    mode = {m[0]: m for m in NEW_MODES}
    reports = {}
    try:
        for k in ("h5", "h5s5"):
            reports[k] = mode_report(machine, *mode[k][:4])[0]
    except Missing as e:
        failures.append("the reports of the new modes could not be read: %s" % e)
    if len(reports) == 2:
        def parse(k, text, tested=None):
            m = mode[k]
            parse_mode(Numbers(), text, "t", k, m[1], m[3], m[4] if tested is None else tested, m[5])
        try:
            parse("h5", reports["h5"])
            parse("h5s5", reports["h5s5"])
        except Missing as e:
            failures.append("the unchanged reports are refused: %s" % e)
        h5, s5 = reports["h5"], reports["h5s5"]
        expect_stop("|R|* check reports one mismatch",
                    lambda: parse("h5", inject(h5, "rows compared, 0 mask another", "rows compared, 1 mask another", "rstar")))
        expect_stop("a group check missing",
                    lambda: parse("h5", inject(h5, "configurations, all as locked.", "configurations, one not as locked.", "groups")))
        expect_stop("a pairs line disagrees with its block",
                    lambda: parse("h5", inject(h5, "  hha              232 |     85     31      1 |",
                                               "  hha              232 |     85     30      1 |", "pairs")))
        expect_stop("the method under test above |R|*",
                    lambda: parse("h5", inject(h5, "   cut-hide-rw-ls                       232   1.0000   1.0000",
                                               "   cut-hide-rw-ls                       232   1.0000   1.0100", "ratio")))
        expect_stop("an untestable family: pairs line disagrees with its block",
                    lambda: parse("h5s5", inject(s5, "  hha              177 |      0      0      0 |    154 ",
                                                 "  hha              177 |      0      0      0 |    153 ", "pairs s5")))
        expect_stop("a new family printing the rank-biserial on d - delta_L",
                    lambda: parse("h5", inject(h5, "rank-biserial (raw differences)", "rank-biserial                  ",
                                               "raw label")))
        expect_stop("a rank-biserial that disagrees with the pairs table",
                    lambda: parse("h5", inject(h5, "rank-biserial (raw differences)      +1.000",
                                               "rank-biserial (raw differences)      +0.900", "rb vs pairs")))
        expect_stop("a tested family read as untestable", lambda: parse("h5", h5, tested=False))
        expect_stop("an untestable family read as tested", lambda: parse("h5s5", s5, tested=True))
        expect_stop("an untestable family where one baseline was tested",
                    lambda: parse("h5s5", inject(s5, "   not enough pairs for a test\n", "\n", "untestable")))
    lot = ("== tested datasets, %s scope: 10 instances solved; skipped none\n"
           "   method              n  |R|=|R|*  Lost=OPT  sum exc  med exc   med pos  |R|>|R|* opt no worse\n"
           + "".join("   %-16s 10   10/10     %s/10        %s      0.0     0.000         0      0/0\n"
                     % (m, "9" if m == OURS else "5", "1" if m == OURS else "9")
                     for m in (OURS, "cut-hide-rw", "cut-hide") + BASELINES + RIVALS)
           + "   empty B_eps, CUT-HIDE with uniform weights: none\n\n")
    lot = lot % "distinct" + lot % "repeated" + "No problem: every method hides.\n"
    try:
        parse_seq_lost(Numbers(), lot, "t")
    except Missing as e:
        failures.append("good SC4LOST text refused: %s" % e)
    expect_stop("SC4LOST reports a problem", lambda: parse_seq_lost(Numbers(), inject(lot, "No problem", "Problem", "lot"), "t"))
    expect_stop("SC4LOST: CUT-HIDE below |R|*_SEQ on an instance",
                lambda: parse_seq_lost(Numbers(), inject(lot, "   cut-hide-rw-ls   10   10/10", "   cut-hide-rw-ls   10    9/10",
                                                         "lot at R"), "t"))
    expect_stop("SC4LOST: a baseline with less excess than CUT-HIDE",
                lambda: parse_seq_lost(Numbers(), inject(lot, "   hha              10   10/10     5/10        9",
                                                         "   hha              10   10/10     5/10        0", "lot excess"), "t"))
    gap = [{"gap": "[1,2]", "items": s, "instances": "5", "targets": "9", "targets_needing_a_mask": "4", "mismatches": "0"}
           for s in ("distinct", "repeated")]
    try:
        check_sc1gap(Numbers(), gap, "t")
    except Missing as e:
        failures.append("good SC1GAP rows refused: %s" % e)
    expect_stop("SC1GAP with one mismatch", lambda: check_sc1gap(Numbers(), [gap[0], dict(gap[1], mismatches="1")], "t"))
    expect_stop("SC1GAP with no row", lambda: check_sc1gap(Numbers(), [], "t"))
    # One-pattern components, the equal-count check and the refinement check on small inputs.
    for s, want in (("[[1, 2]]", False), ("[[1, 2], [3, 4]]", True), ("[[1, 2], [2, 3]]", False),
                    ("[[1, 2], [2, 3], [5, 6]]", True), ("[[1, 1, 2], [2, 3]]", False)):
        if has_single_component(s) != want:
            failures.append("has_single_component(%s) is not %s" % (s, want))
    k1, k2 = ("d", "[0,1]", "10", "distinct", "[[1, 2]]"), ("d", "[0,1]", "10", "distinct", "[[1, 2], [3, 4]]")
    good_masks = {k1: {OURS: {3}, "hha": {3}}, k2: {OURS: {5}, "hha": {6}, REFINE: {4}}}
    if check_equal_counts(good_masks) != 1:
        failures.append("equal-count check misreads a good input")
    expect_stop("an equal-count pair with two patterns",
                lambda: check_equal_counts({**good_masks, k2: {OURS: {5}, "hha": {5}}}))
    expect_stop("no equal-count pair at all", lambda: check_equal_counts({k2: good_masks[k2]}))
    if check_refine_up({k2: {7}}, {k2: {6}}, good_masks) != 1:
        failures.append("refinement check misreads a good input")
    expect_stop("refinement raises Lost without fewer masks",
                lambda: check_refine_up({k2: {7}}, {k2: {6}}, {k2: {OURS: {5}, REFINE: {5}}}))
    k3 = ("d", "[0,1]", "10", "distinct", "[[1, 2], [2, 3]]")
    expect_stop("refinement raises Lost on a set without a one-pattern component",
                lambda: check_refine_up({k3: {7}}, {k3: {6}}, {k3: {OURS: {5}, REFINE: {4}}}))
    # The sentence checks on the exported numbers, then with one fault per check.
    exported = INV / "paper_numbers.json"
    if not exported.exists():
        failures.append("sentence checks: %s absent, nothing to check" % exported)
    else:
        import copy
        data = json.loads(exported.read_text(encoding="utf-8"))

        def loaded(edit=None):
            n2 = Numbers()
            n2.values = copy.deepcopy(data["values"])
            n2.values.pop("sentence_checks.passed", None)
            n2.tables = copy.deepcopy(data["tables"])
            if edit:
                edit(n2)
            return n2

        def row(n2, name, **match):
            rs = [r for r in n2.tables[name]["rows"] if all(r[k] == w for k, w in match.items())]
            if len(rs) != 1:
                raise KeyError("%s %s: %d rows" % (name, match, len(rs)))
            return rs[0]

        def setv(key, value):
            def edit(n2):
                n2.values[key]["value"] = value
            return edit

        def setr(name, field, value, **match):
            def edit(n2):
                row(n2, name, **match)[field] = value
            return edit
        try:
            sentence_checks(loaded())
        except Missing as e:
            failures.append("sentence checks refuse the exported numbers: %s" % e)
        faults = [
            ("a second instance where CUT-HIDE loses more", setr("h6", "same_gt", 1, scope="repeated", method="dbsh")),
            ("the one instance against another baseline", lambda n2: (setr("h5", "same_gt", 0, scope="distinct", method="hha")(n2),
                                                                     setr("h5", "same_gt", 1, scope="distinct", method="dbsh")(n2))),
            ("a baseline with fewer masks under HIDE-SEQ, one pattern", setr("h5", "ours_more", 1, scope="repeated", method="hha")),
            ("g- > 0, several distinct-item: a baseline not above everywhere",
             setr("h6sev", "base_more", 267, scope="distinct", method="dbsh")),
            ("HIDE-SEQ five patterns: one equal-count instance", setr("h5s5", "same", 1, scope="distinct", method="hha")),
            ("HIDE-SEQ five patterns: SBSH with fewer masks", setr("h5s5", "ours_more", 1, scope="repeated", method="sbsh-r1")),
            ("HIDE-SEQ five patterns: DBSH with fewer masks", setr("h5s5", "ours_more", 2, scope="repeated", method="dbsh")),
            ("HIDE-SEQ five patterns: no baseline with fewer masks",
             lambda n2: [r.__setitem__("ours_more", 0) for r in n2.tables["h5s5"]["rows"]]),
            ("H3 distinct median above the near-1 threshold", setv("h3.main.median.max", 1.06)),
            ("HIDE-SEQ distinct median at the more-masks threshold", setv("h5.ratio.distinct.median.max", 1.10)),
            ("HIDE-SEQ repeated: HHA reaches the threshold", setv("h5.ratio.repeated.hha", 1.10)),
            ("SC4LOST: CH-AW as often optimal as CUT-HIDE",
             setr("seqlost", "opt", 545, scope="distinct", method="cut-hide-rw")),
            ("the lowest share moves to HIDE-DB", setv("lostopt.ours.opt.repeated", 700)),
            ("a rival's median Lost_rel above twice the margin", setv("levels.repeated.1.rivals.lost_rel.max", 0.041)),
            ("one more rejected null hypothesis", setr("h2", "verdict", "H0 rejected", test="H2-R", method="dbsh")),
            ("H3 distinct median at the more-masks threshold",
             lambda n2: (setv("h3.main.median.max", 1.10)(n2), setv("h3.main.median.min", 1.0)(n2))),
            ("HIDE-SEQ five repeated: DBSH ties HHA on fewer masks",
             setr("h5s5", "ours_more", 190, scope="repeated", method="dbsh")),
            ("SC3LOST: a method at |R|* more often than CUT-HIDE",
             setr("lostopt", "at_R", 630, scope="distinct", method="hha-stop")),
            ("H4b-R HHA-stop apart from H2-R HHA", setr("h2", "median_diff", 0.04, scope="repeated", method="hha-stop")),
            ("H3 repeated median below the more-masks threshold", setv("h3r.main.median.min", 1.09)),
            ("a raw rank-biserial below 1", setr("rb_raw", "rb_raw", 0.99, test="H2-R", method="dbsh")),
            ("a fifth interval crosses a threshold", setr("ci", "hi", 1.051, test="H6", scope="distinct", group="main",
                                                          method="dbsh")),
            ("the interval of OCC no longer crosses 1.10", setr("ci", "lo", 1.101, test="H4a", scope="distinct",
                                                               group="main", method="occurrence")),
            ("H4a, one pattern: OCC not exactly 1", setv("h4a.size.distinct.occurrence.s1", 1.0001)),
            ("lot D: one baseline not rejected", setr("noninf", "verdict", "not rejected", scope="repeated", method="dbsh")),
            ("lot D: fewer masks and fewer patterns on half the instances",
             setr("noninf", "cross_fewer_fewer", 296, scope="distinct", method="hha")),
            ("lot E: a distinct-item dataset at delta_L",
             setr("per_dataset", "median_diff", 0.02, test="H2", method="dbsh", dataset="msnbc")),
            ("H4a, OCC distinct: the ratio at 10 patterns below that at 5",
             setv("h4a.size.distinct.occurrence.s10", 1.2)),
            ("H2-R: a dataset named without equal-count instances", setv("names.h2r.equal", "bible, bpi2012, msnbc and sepsis")),
            ("several patterns: a baseline at the median ratio of CUT-HIDE",
             setr("bound", "median", 1.3003, scope="distinct", size=5, method="sbsh-r1")),
        ]
        for name, edit in faults:
            try:
                n2 = loaded(edit)
            except KeyError as e:
                failures.append("sentence fault %r could not be injected: %s" % (name, e))
                continue
            expect_stop("sentence check: " + name, lambda: sentence_checks(n2))
    print("self-test: %d failures%s" % (len(failures), (": " + "; ".join(failures)) if failures else ""))
    print("checks: same key twice, absent pattern, repeated pattern, a good block, pairs/test mismatch, "
          "missing Holm line, missing equivalence line, levels text, levels method missing, levels count, no lock named as "
          "missing, two locks disagree on the JDK, two locks disagree on the machine name, the newest lock wins, "
          "the two real HIDE-SEQ reports read, |R|* mismatch, group check missing, pairs line vs block (tested and untestable), method under test "
          "above |R|*, rank-biserial on d - delta_L in a new family, rank-biserial vs pairs table, tested read as untestable and back, one untestable baseline tested, good SC4LOST text, SC4LOST "
          "problem, CUT-HIDE below |R|*_SEQ, a baseline with less excess, good SC1GAP rows, SC1GAP mismatch, SC1GAP empty, "
          "one-pattern components, equal-count check (good, two patterns, empty), refinement check (good, masks, component), "
          "sentence checks on the exported numbers and 30 faults")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
