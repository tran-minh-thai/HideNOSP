"""Every number the manuscript quotes, read back from the result files, written as data.

    python tools/paper_numbers.py [--machine=ADMIN-PC] [--small-machine=dev] [--out-dir=DIR]
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
"""
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
    return [p for p in sorted(glob.glob(str(tree / ("%s_*.csv" % scenario)))) if machine_of(p) == machine]


def dataset_name(path_text):
    return Path(path_text).stem


# ---------------------------------------------------------------- preregistered analysis

def parse_family(part, family, prefix, nums, scope):
    """Per-method blocks, Holm table, pairs table and equivalence lines of one H2-like family."""
    rows = []
    for name in family:
        block = one(r"^-- %s\n((?:   .*\n)+)" % re.escape(name), part, "%s block of %s" % (prefix, name))
        same = int(one(r"into Wilcoxon \(same \|R\|\)\s+(\d+) instances", block, prefix + name))
        more = int(one(r"baseline uses more masks\s+(\d+) instances", block, prefix + name))
        ours = int(one(r"CUT-HIDE uses more masks\s+(\d+) instances", block, prefix + name))
        median = float(one(r"median difference of Lost_rel\s+([+-][\d.]+)", block, prefix + name))
        rb = float(one(r"rank-biserial\s+([+-]?[\d.]+)", block, prefix + name))
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
    i_h4 = text.index("Section 19.1, distinct-item scope")
    i_h4r = text.index("Section 19.1, repeated-item scope")
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
    text = run_tool(["cert_table.py", "--machine=%s" % machine, "--small-machine=%s" % small_machine], "cert_table")
    src = "cert_table.py --machine=%s --small-machine=%s" % (machine, small_machine)
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

def sc3_rows(nums, machine):
    total, status = 0, Counter()
    ours, refine = {}, {}
    for path in files(INV, "SC3", machine):
        opts = options_of(path)
        kind = opts.get("patternKind", "")
        for r in rows_of(path):
            total += 1
            status[r.get("status", "")] += 1
            if r.get("method_key") and float(r.get("hf") or 0) != 0:
                raise Missing("SC3 row with HF > 0 (%s, %s): the text says every method hides on every row"
                              % (Path(path).name, r["method_key"]))
            key = (dataset_name(r["dataset"]), r["gap"], r["sigma"], kind, r["sensitive_set"])
            if r["method_key"] == OURS and opts.get("methods", "") not in ("none",):
                ours.setdefault(key, set()).add(int(r["lost"]))
            elif r["method_key"] == REFINE:
                refine.setdefault(key, set()).add(int(r["lost"]))
    src = "SC3 files of %s (every batch)" % machine
    nums.put("sc3.n_rows", total, src)
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
    for path in files(TIMED, "SC6", machine):
        for r in rows_of(path):
            reps[int(r["repetitions"])] += 1
            if r["warmups"] != "1":
                raise Missing("SC6 row with %s warm-up runs" % r["warmups"])
    nums.put("sc6.repetitions.min", min(reps), "SC6 files of %s" % machine)
    nums.put("sc6.repetitions.max", max(reps), "SC6 files of %s" % machine)


def platform(nums, machine):
    lock = json.loads((INV / ("campaign_%s_g2" % machine) / "ENV_LOCK.json").read_text(encoding="utf-8"))
    src = "results-invariant/campaign_%s_g2/ENV_LOCK.json" % machine
    for k in ("cpu", "logical_processors", "ram_bytes", "os", "java_vendor", "java_runtime_version", "hostname"):
        nums.put("platform.%s" % k, lock[k], src)
    heaps = Counter()
    for tree, scenario in ((TIMED, "SC6"), (INV, "SC3")):
        for path in files(tree, scenario, machine):
            with open(path, encoding="utf-8") as f:
                for line in f:
                    if line.startswith("# heap_max_bytes "):
                        heaps[(scenario, int(line.split()[2]))] += 1
                        break
    sc6 = {h for (s, h) in heaps if s == "SC6"}
    if len(sc6) != 1:
        raise Missing("SC6 files run with %d heap sizes: %s" % (len(sc6), heaps))
    nums.put("platform.heap_bytes.sc6", sc6.pop(), "heap_max_bytes of the SC6 files")
    nums.put("platform.heap_bytes.sc3", sorted({h for (s, h) in heaps if s == "SC3"}), "heap_max_bytes of the SC3 files")


# ---------------------------------------------------------------- main

TIMING_PREFIXES = ("sc6.", "platform.")


def build(machine, small_machine):
    nums = Numbers()
    for step in (lambda: sc1(nums, machine), lambda: sc2(nums), lambda: datasets_table(nums),
                 lambda: sc3_rows(nums, machine), lambda: analysis(nums, machine), lambda: lower_bounds(nums, machine),
                 lambda: reduced_optimum(nums, machine), lambda: pareto(nums, machine),
                 lambda: lost_optimum(nums, small_machine), lambda: certified(nums, machine, small_machine),
                 lambda: timing(nums, machine), lambda: platform(nums, machine)):
        step()
    # The same quantity read from two tools must agree.
    a, b = nums.values["sc3.n_excluded"]["value"], sum(v["value"][0] for k, v in nums.values.items()
                                                        if k.startswith("excluded."))
    if b != 0 and a == 0:
        raise Missing("the analysis excluded %d instances while no SC3 row carries a status" % b)
    return nums


def git(*args):
    p = subprocess.run(["git"] + list(args), cwd=ROOT, capture_output=True, text=True)
    return p.stdout.strip()


def write(nums, out_dir, machine, small_machine, argv):
    meta = {"command": "python tools/paper_numbers.py " + " ".join(argv), "commit": git("rev-parse", "HEAD"),
            "tree": "clean" if not git("status", "--porcelain") else "MODIFIED (%d files)" % len(
                git("status", "--porcelain").splitlines()),
            "tool_sha256": sha12(__file__), "python": sys.version.split()[0], "machine": machine,
            "small_machine": small_machine}
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
    small = opts.get("small-machine", "dev")
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
    print("self-test: %d failures%s" % (len(failures), (": " + "; ".join(failures)) if failures else ""))
    print("checks: same key twice, absent pattern, repeated pattern, a good block, pairs/test mismatch, "
          "missing Holm line, missing equivalence line")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
