# SPDX-License-Identifier: MIT
"""Analyses of preregistration section 23 that read result files: lots D and E now, lots A, B and F once their files
exist.

    python tools/reopened_analysis.py --machine=ADMIN-PC [--out FILE]
    python tools/reopened_analysis.py --self-test

Why one more tool. The tests of sections 2-22 are read by tools/preregistered_analysis.py, whose loading lives in its
main(); changing it could change a number of the paper. This tool loads the same files with the same helpers of that
tool, in the same order (closure rule, refinement and Pareto files left out, stop-at-threshold files apart, instances
that need no mask set aside, reruns merged, gaps kept), and takes the groups from the locked tables. Before it reports
anything new it rebuilds every row of the tables h2, h3, h5, h5_ratio, h6 and h6_ratio of
results-invariant/paper_numbers.json from its own loading and stops, exit code 1, on the first difference: a loading
that drifted from the official one cannot pass that check.

Lot D (preregistration section 23.4): for each baseline, d = Lost_rel(baseline) - Lost_rel(CUT-HIDE) on every main-group instance
whatever the mask counts; one-sided Wilcoxon-Pratt of H1: median d > -delta_L; Holm over the four baselines, per scope;
the cross table of masks (fewer, same, more) by lost patterns (fewer, same, more); the same test per size, descriptive.

Lot E (preregistration section 23.5): stratified bootstrap intervals (instances resampled within each dataset, 10,000 resamples, seed
2026, 95% percentile) for the medians of H3, H4a, H5a and the H3 part of H6; per-dataset medians of H2, H2-R and H3;
the rank-biserial on the raw differences for every row of the table h2; the ratio of H4a by size; the number of
tests and of equivalence verdicts per Holm family.

Exit status: 0 when every check passed, 1 when a check failed or a file is missing, 2 when the self-test fails.
"""
import argparse
import io
import json
import os
import re
import sys
from collections import defaultdict

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import preregistered_analysis as pa  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESULTS = os.path.join(ROOT, "results-invariant")
NUMBERS = os.path.join(RESULTS, "paper_numbers.json")
OURS = pa.LOCKED.OUR_METHOD
BASELINES = tuple(pa.LOCKED.BASELINE_FAMILY)
RIVALS = tuple(pa.LOCKED.STOP_FAMILY)
DELTA = pa.LOCKED.DELTA_L
BOOTSTRAPS = 10000
SEED = 2026
TABLES = {  # (scenario, gaps) -> (distinct table, repeated table)
    ("SC3", "zero"): (pa.LOCKED.GROUP_TABLE, pa.LOCKED.GROUP_TABLE_REPEATED),
    ("SC4", "zero"): (pa.LOCKED.GROUP_TABLE, pa.LOCKED.GROUP_TABLE_REPEATED),
    ("SC3", "positive"): (pa.LOCKED.GAP_GROUP_TABLE, pa.LOCKED.GAP_GROUP_TABLE_REPEATED),
    ("SC3REP", "zero"): (pa.LOCKED.REP_GROUP_TABLE, pa.LOCKED.REP_GROUP_TABLE_REPEATED),
}


class Failed(Exception):
    """A check of this tool failed: the report would not be about the files of the paper."""


# ---------------------------------------------------------------- loading

def load(scenario, machine, gaps="zero", directory=RESULTS, closure="necessary"):
    """(rows, stop rows) of one scenario and machine, merged and filtered as tools/preregistered_analysis.py does."""
    files = [f for f in pa.collect_files(directory, scenario + "_") if pa.machine_of(f) == machine]
    files, _ = pa.keep_closure(files, closure)
    if not files:
        raise Failed("no %s file of machine %s under closure rule %s in %s" % (scenario, machine, closure, directory))
    raw, stop = [], []
    for path in files:
        comments, rows = pa.read_csv(path)
        options = next((c for c in comments if c.startswith("# options ")), "")
        if re.search(r"[{ ]methods=%s[,}]" % re.escape(pa.SECONDARY_METHOD), options):
            continue
        if re.search(r"[{ ]methods=none[,}]", options):
            continue
        missing = [c for c in pa.REQUIRED_COLUMNS if not rows or c not in rows[0]]
        if missing:
            raise Failed("%s lacks the columns %s" % (os.path.basename(path), ", ".join(missing)))
        if re.search(r"[{ ]methods=%s[,}]" % re.escape(pa.LOCKED.STOP_METHODS), options):
            stop.extend(rows)
        else:
            raw.extend(rows)
    quiet = []
    raw, _ = pa.set_aside_no_mask(raw)
    rows = pa.merge_reruns(raw, quiet.append)
    rows, _ = pa.keep_gaps(rows, gaps)
    stop_rows = pa.merge_reruns(stop, quiet.append) if stop else []
    stop_rows, _ = pa.keep_gaps(stop_rows, gaps)
    return rows, stop_rows


def groups_of(scenario, gaps, scope):
    distinct, repeated = TABLES[(scenario, gaps)]
    table = pa.read_group_table(distinct if scope == "distinct" else repeated)
    if table is None:
        raise Failed("the locked group table of %s %s %s is missing" % (scenario, gaps, scope))
    return table


def in_group(rows, groups, group="main", keep_report_only=False):
    return [r for r in rows if (keep_report_only or r["dataset"] not in pa.LOCKED.REPORT_ONLY)
            and (groups.get((r["dataset"], r["gap"], r["sigma"])) or {}).get("group") == group]


def by_scope(rows, scope):
    return [r for r in rows if pa.scope_of_set(r["sensitive_set"]) == scope]


def of_size(rows, size):
    return [r for r in rows if pa.pattern_count(r["sensitive_set"]) == size]


# ---------------------------------------------------------------- the reproduction check

def reproduce(machine, numbers, write, directory=RESULTS):
    """Rebuild the rows of the tables h2, h3, h5, h5_ratio, h6, h6_ratio; raise Failed on any difference."""
    tables = numbers["tables"]
    compared = 0
    cases = (("h2", "SC3", "zero", None), ("h5", "SC4", "zero", 1), ("h6", "SC3", "positive", 1))
    ratio_cases = (("h3", "SC3", "zero"), ("h5_ratio", "SC4", "zero"), ("h6_ratio", "SC3", "positive"))
    loaded = {}
    for _, scenario, gaps, _ in cases:
        if (scenario, gaps) not in loaded:
            loaded[(scenario, gaps)] = load(scenario, machine, gaps, directory)
    for name, scenario, gaps, size in cases:
        rows, stop = loaded[(scenario, gaps)]
        for want in tables[name]["rows"]:
            scope, method = want["scope"], want["method"]
            group_rows = in_group(by_scope(rows + (stop if method in RIVALS else []), scope),
                                  groups_of(scenario, gaps, scope))
            if size is not None:
                group_rows = of_size(group_rows, size)
            bins = pa.pair_up(group_rows, method, "lost_rel")
            got = {"instances": len(bins["tested"]) + len(bins["baseline more"]) + len(bins["ours more"]),
                   "same": len(bins["tested"]), "base_more": len(bins["baseline more"]),
                   "ours_more": len(bins["ours more"])}
            d = [e[1] for e in bins["tested"]]
            if d:
                got["median_diff"] = round(float(np.median(d)), 4)
                raw = name != "h2"
                got["rank_biserial"] = round(pa.effect_size(d, raw=raw), 3)
            for key, value in got.items():
                if key in want and want[key] != value and not (isinstance(value, float) and abs(want[key] - value) < 1e-9):
                    raise Failed("table %s %s %s %s: %s is %r in paper_numbers.json, %r here"
                                 % (name, want.get("test", ""), scope, method, key, want[key], value))
            compared += 1
    for name, scenario, gaps in ratio_cases:
        rows, _ = loaded[(scenario, gaps)]
        for want in tables[name]["rows"]:
            scope = want["scope"]
            groups = groups_of(scenario, gaps, scope)
            v = [r["mask_ratio"] for r in of_size(by_scope(rows, scope), 1) if r["method_key"] == want["method"]
                 and r["dataset"] not in pa.LOCKED.REPORT_ONLY and not np.isnan(r["mask_ratio"])
                 and (groups.get((r["dataset"], r["gap"], r["sigma"])) or {}).get("group") == want["group"]]
            got = {"n": len(v), "median": round(float(np.median(v)), 4) if v else None,
                   "max": round(float(max(v)), 4) if v else None}
            for key, value in got.items():
                if want.get(key) != value:
                    raise Failed("table %s %s %s %s: %s is %r in paper_numbers.json, %r here"
                                 % (name, scope, want["group"], want["method"], key, want.get(key), value))
            compared += 1
    write("Reproduction check: %d rows of the tables h2, h5, h6, h3, h5_ratio, h6_ratio rebuilt, 0 differences."
          % compared)
    return loaded


# ---------------------------------------------------------------- statistics

def bootstrap_median(values, strata, resamples=BOOTSTRAPS, seed=SEED):
    """95% percentile interval of the median, resampling within each stratum (preregistration section 23.5)."""
    values = np.asarray(values, dtype=float)
    strata = np.asarray(strata)
    rng = np.random.default_rng(seed)
    parts = []
    for s in sorted(set(strata.tolist())):
        v = values[strata == s]
        parts.append(v[rng.integers(0, len(v), size=(resamples, len(v)))])
    medians = np.median(np.concatenate(parts, axis=1), axis=1)
    return float(np.percentile(medians, 2.5)), float(np.percentile(medians, 97.5))


def smallest_p(n, zeros):
    """The smallest p the one-sided Wilcoxon-Pratt test can return on n differences with this many zeros."""
    if n == 0 or zeros == n:
        return 1.0
    return pa.wilcoxon_one_sided([0.0] * zeros + [1.0 + i for i in range(n - zeros)])[1]


# ---------------------------------------------------------------- lot D

def lot_d(rows, groups, scope, write, label="SC3"):
    main = in_group(by_scope(rows, scope), groups)
    write("")
    write("-- lot D, %s, %s-item scope: d = Lost_rel(baseline) - Lost_rel(CUT-HIDE) on every main-group instance; "
          "H1: median d > -%.2f" % (label, scope, DELTA))
    p_by, out = {}, {}
    excluded_total = pairs_total = 0
    for b in BASELINES:
        bins = pa.pair_up(main, b, "lost_rel")
        entries = bins["tested"] + bins["baseline more"] + bins["ours more"]
        excluded_total += len(bins["excluded"])
        pairs_total += len(entries) + len(bins["excluded"])
        if not entries:
            write("   %-10s no instance" % b)
            continue
        d = [e[1] for e in entries]
        stat, p, effect, n, zeros = pa.wilcoxon_one_sided(d, -DELTA)
        cross = defaultdict(int)
        for _, _, ours, theirs in entries:
            masks = "fewer" if ours["mask_count"] < theirs["mask_count"] else (
                "same" if ours["mask_count"] == theirs["mask_count"] else "more")
            lost = "fewer" if ours["lost"] < theirs["lost"] else ("same" if ours["lost"] == theirs["lost"] else "more")
            cross["%s/%s" % (masks, lost)] += 1
        p_by[b] = p
        out[b] = {"n": n, "zeros": zeros, "median": float(np.median(d)), "p": p,
                  "rb_shifted": effect, "rb_raw": pa.rank_biserial(d), "cross": dict(cross),
                  "excluded": len(bins["excluded"]),
                  "by_size": {}}
        for size in (1, 5, 10, 20):
            ds = [e[1] for e in entries if pa.pattern_count(e[0][3]) == size]
            if ds:
                out[b]["by_size"][size] = (len(ds), float(np.median(ds)), pa.wilcoxon_one_sided(ds, -DELTA)[1])
    holm = pa.holm(p_by) if p_by else {}
    share = excluded_total / pairs_total if pairs_total else 0.0
    for b, v in out.items():
        v["p_holm"] = holm[b]
        v["verdict"] = ("no conclusion (exclusions)" if share > pa.LOCKED.MAX_EXCLUDED_SHARE
                        else "H0 rejected" if holm[b] < pa.LOCKED.ALPHA else "not rejected")
        c = v["cross"]
        write("   %-10s n %4d  median d %+.4f  p %.4g  Holm %.4g  %-13s  rb(d+delta) %+.3f  rb(raw) %+.3f  excluded %d"
              % (b, v["n"], v["median"], v["p"], v["p_holm"], v["verdict"], v["rb_shifted"], v["rb_raw"], v["excluded"]))
        write("   %-10s masks x lost (CUT-HIDE against %s): " % ("", b) + ", ".join(
            "%s/%s %d" % (m, l, c.get("%s/%s" % (m, l), 0)) for m in ("fewer", "same", "more")
            for l in ("fewer", "same", "more")))
        write("   %-10s by size (descriptive): " % "" + ", ".join(
            "|S|=%d n %d median %+.4f p %.4g" % (s, *t) for s, t in sorted(v["by_size"].items())))
    write("   excluded %d of %d pairs (%.1f%%; the threshold of preregistration section 5 is %.0f%%)"
          % (excluded_total, pairs_total, 100 * share, 100 * pa.LOCKED.MAX_EXCLUDED_SHARE))
    return out


# ---------------------------------------------------------------- lot E

def ratio_rows(rows, groups, scope, group, method, size=1):
    return [r for r in of_size(by_scope(rows, scope), size) if r["method_key"] == method
            and r["dataset"] not in pa.LOCKED.REPORT_ONLY and not np.isnan(r["mask_ratio"])
            and (groups.get((r["dataset"], r["gap"], r["sigma"])) or {}).get("group") == group]


def lot_e(loaded, numbers, write):
    out = {"intervals": [], "per_dataset": [], "rb_raw": [], "h4a_size": [], "families": []}
    write("")
    write("-- lot E (1): 95%% stratified bootstrap intervals of the medians that give a verdict (%d resamples, seed %d)"
          % (BOOTSTRAPS, SEED))
    for name, scenario, gaps, methods in (("H3", "SC3", "zero", BASELINES), ("H5a", "SC4", "zero", BASELINES + pa.SEQUENCE_ONLY),
                                          ("H6", "SC3", "positive", BASELINES)):
        rows, _ = loaded[(scenario, gaps)]
        for scope in ("distinct", "repeated"):
            groups = groups_of(scenario, gaps, scope)
            for group in ("main", "degenerate"):
                for m in methods:
                    rs = ratio_rows(rows, groups, scope, group, m)
                    if not rs:
                        continue
                    v = [r["mask_ratio"] for r in rs]
                    lo, hi = bootstrap_median(v, [r["dataset"] for r in rs])
                    out["intervals"].append({"test": name, "scope": scope, "group": group, "method": m, "n": len(v),
                                             "datasets": len({r["dataset"] for r in rs}), "median": float(np.median(v)),
                                             "lo": lo, "hi": hi})
                    write("   %-4s %-8s %-10s %-10s n %4d (%d datasets)  median %.4f  [%.4f, %.4f]"
                          % (name, scope, group, m, len(v), len({r["dataset"] for r in rs}), float(np.median(v)), lo, hi))
    rows, stop = loaded[("SC3", "zero")]
    for scope in ("distinct", "repeated"):
        groups = groups_of("SC3", "zero", scope)
        main = in_group(by_scope(rows + stop, scope), groups)
        for c in RIVALS:
            bins = pa.pair_up(main, c, "lost_rel")
            entries = [e for e in bins["tested"] + bins["baseline more"] + bins["ours more"] if e[2]["mask_count"] > 0]
            v = [e[3]["mask_count"] / e[2]["mask_count"] for e in entries]
            lo, hi = bootstrap_median(v, [e[0][0] for e in entries])
            out["intervals"].append({"test": "H4a", "scope": scope, "group": "main", "method": c, "n": len(v),
                                     "datasets": len({e[0][0] for e in entries}), "median": float(np.median(v)),
                                     "lo": lo, "hi": hi})
            write("   H4a  %-8s %-10s %-10s n %4d (%d datasets)  median %.4f  [%.4f, %.4f]"
                  % (scope, "main", c, len(v), len({e[0][0] for e in entries}), float(np.median(v)), lo, hi))
            for size in (1, 5, 10, 20):
                vs = [e[3]["mask_count"] / e[2]["mask_count"] for e in entries if pa.pattern_count(e[0][3]) == size]
                if vs:
                    out["h4a_size"].append({"scope": scope, "method": c, "size": size, "n": len(vs),
                                            "median": float(np.median(vs))})
    write("")
    write("-- lot E (4): the ratio of H4a by size of the sensitive set")
    for r in out["h4a_size"]:
        write("   %-8s %-10s |S| = %2d  n %4d  median %.4f" % (r["scope"], r["method"], r["size"], r["n"], r["median"]))

    write("")
    write("-- lot E (2): per dataset, median Lost_rel difference of H2 / H2-R (equal-count instances) and median ratio of H3")
    for scope, test in (("distinct", "H2"), ("repeated", "H2-R")):
        groups = groups_of("SC3", "zero", scope)
        main = in_group(by_scope(rows, scope), groups)
        for b in BASELINES:
            per = defaultdict(list)
            for key, d, _, _ in pa.pair_up(main, b, "lost_rel")["tested"]:
                per[key[0]].append(d)
            ratios = defaultdict(list)
            for r in ratio_rows(rows, groups, scope, "main", b):
                ratios[r["dataset"]].append(r["mask_ratio"])
            for ds in sorted(set(per) | set(ratios)):
                rec = {"test": test, "scope": scope, "method": b, "dataset": ds,
                       "n_diff": len(per.get(ds, [])), "median_diff": float(np.median(per[ds])) if per.get(ds) else None,
                       "n_ratio": len(ratios.get(ds, [])),
                       "median_ratio": float(np.median(ratios[ds])) if ratios.get(ds) else None}
                out["per_dataset"].append(rec)
                write("   %-5s %-10s %-14s diff n %3d median %s   ratio n %3d median %s"
                      % (test, b, ds, rec["n_diff"], "%+.4f" % rec["median_diff"] if rec["n_diff"] else "   -   ",
                         rec["n_ratio"], "%.4f" % rec["median_ratio"] if rec["n_ratio"] else "-"))

    write("")
    write("-- lot E (3): rank-biserial on the raw differences, next to the one on d - delta_L, rows of table h2")
    for want in numbers["tables"]["h2"]["rows"]:
        scope, m = want["scope"], want["method"]
        main = in_group(by_scope(rows + (stop if m in RIVALS else []), scope), groups_of("SC3", "zero", scope))
        d = [e[1] for e in pa.pair_up(main, m, "lost_rel")["tested"]]
        raw = round(pa.rank_biserial(d), 3) if d else None
        out["rb_raw"].append({"test": want["test"], "scope": scope, "method": m, "rb_shifted": want["rank_biserial"],
                              "rb_raw": raw})
        write("   %-6s %-10s r_rb(d - delta_L) %+.3f   r_rb(raw) %s" % (want["test"], m, want["rank_biserial"],
                                                                         "%+.3f" % raw if raw is not None else "-"))

    write("")
    write("-- lot E (5): tests and equivalence verdicts of the paper, per Holm family (tables of paper_numbers.json)")
    for name in ("h2", "h5", "h5s5", "h6", "h6sev", "sensitivity"):
        fam = defaultdict(list)
        for r in numbers["tables"][name]["rows"]:
            fam[(r.get("test", name), r.get("scope", ""))].append(r)
        for (test, scope), rs in sorted(fam.items()):
            eq = sum(1 for r in rs for k in ("eq_lost", "eq_supdist") if r.get(k))
            out["families"].append({"table": name, "test": test, "scope": scope, "tests": len(rs), "equivalence": eq})
            write("   table %-11s %-6s %-9s tests %d  equivalence verdicts %d" % (name, test, scope, len(rs), eq))
    write("   total: %d tests in %d Holm families, %d equivalence verdicts"
          % (sum(f["tests"] for f in out["families"]), len(out["families"]), sum(f["equivalence"] for f in out["families"])))
    return out


# ---------------------------------------------------------------- lots A, B, F (generation 7 files)

def load_plain(scenario, machine, directory=RESULTS, closure="necessary"):
    """(file name, options, comment lines, rows as read) of every file of one scenario and machine: the checks of lots
    B and F compare rows column by column, before any merging."""
    files = [f for f in pa.collect_files(directory, scenario + "_") if pa.machine_of(f) == machine]
    files, _ = pa.keep_closure(files, closure)
    out = []
    for path in files:
        comments, rows = pa.read_csv(path)
        line = next((c for c in comments if c.startswith("# options ")), "")
        out.append((os.path.basename(path), pa.sc3_commands.parse_options(line) if line else {}, comments, rows))
    return out


def sc3_plain(machine, directory=RESULTS):
    """The SC3 rows as read, by (dataset, gap, sigma, sensitive set, method key)."""
    out = {}
    for _, _, _, rows in load_plain("SC3", machine, directory):
        for r in rows:
            out[(pa.dataset_name(r["dataset"]), r["gap"], r["sigma"], r["sensitive_set"], r["method_key"])] = r
    return out


def same_as_sc3(rows, sc3, key_of=lambda r: r["method_key"], skip=()):
    """(rows compared, rows that differ from SC3 on some column) for rows whose SC3 counterpart must be identical."""
    compared, differ = 0, []
    for r in rows:
        k = (pa.dataset_name(r["dataset"]), r["gap"], r["sigma"], r["sensitive_set"], key_of(r))
        if k not in sc3:
            differ.append((k, "no SC3 row"))
            continue
        cols = [c for c in r if c not in skip and r[c] != sc3[k].get(c)]
        compared += 1
        if cols:
            differ.append((k, cols))
    return compared, differ


def lot_a_instances(machine, directory=RESULTS):
    """{scope: (complete, excluded, missing)} of lot A, or None before its files exist. complete maps an instance
    (dataset, gap, sigma, sensitive set) to its four rows; the later descriptive lots (B extended, SC3WIDE) report on
    these same instances, as preregistration sections 23.10 and 23.11 say."""
    files = [f for f in load_plain("SC3SENS", machine, directory) if f[1].get("methods") == "cut-hide-uniform"]
    if not files:
        return None
    quiet = []
    uniform = pa.merge_reruns([r for f in files for r in f[3]], quiet.append)
    rows, _ = load("SC3", machine, "zero", directory)
    rows = [r for r in of_size(rows, 1) if r["method_key"] in ("cut-hide", "cut-hide-rw", OURS)] + uniform
    out = {}
    for scope in ("distinct", "repeated"):
        main = in_group(by_scope(rows, scope), groups_of("SC3", "zero", scope))
        by_instance = defaultdict(dict)
        for r in main:
            by_instance[(r["dataset"], r["gap"], r["sigma"], r["sensitive_set"])][r["method_key"]] = r
        complete, excluded = {}, 0
        for k, m in by_instance.items():
            if len(m) != 4:
                continue
            if any(x["status"] or (not np.isnan(x["hf"]) and x["hf"] > 0) for x in m.values()):
                excluded += 1
                continue
            off = [name for name, x in m.items() if x["mask_count"] != x["min_masks"]]
            if off:
                raise Failed("lot A: %s not at |R|* on %s; an implementation error stops the analysis" % (off, k))
            complete[k] = m
        missing = sum(1 for m in by_instance.values() if len(m) != 4)
        out[scope] = (complete, excluded, missing)
    return out


def raw_instance(r):
    """The instance key of a row as read, in the form of lot_a_instances."""
    return (pa.dataset_name(r["dataset"]), r["gap"], int(r["sigma"]), r["sensitive_set"])


def lot_a(machine, write, directory=RESULTS):
    write("")
    write("=" * 78)
    write("Lot A (preregistration section 23.1): uniform weights, CH-FW, CH-AW and CUT-HIDE at |R|*, |S| = 1")
    write("=" * 78)
    instances = lot_a_instances(machine, directory)
    if instances is None:
        write("   no SC3SENS file of methods cut-hide-uniform for machine %s yet" % machine)
        return None
    pairs = (("cut-hide-uniform", "cut-hide"), ("cut-hide", "cut-hide-rw"), ("cut-hide-rw", OURS))
    out = {}
    for scope in ("distinct", "repeated"):
        complete, excluded, missing = instances[scope]
        write("")
        write("-- %s-item scope: %d instances with the four rows (%d excluded: status or HF > 0; %d without the four rows)"
              % (scope, len(complete), excluded, missing))
        if not complete:
            write("   no instance to test")
            out[scope] = {"instances": 0, "excluded": excluded, "pairs": {}, "sup_dist": {}}
            continue
        p_by, res = {}, {}
        for first, second in pairs:
            d = [m[first]["lost_rel"] - m[second]["lost_rel"] for m in complete.values()]
            stat, p, effect, n, zeros = pa.wilcoxon_one_sided(d, 0.0)
            p_by[(first, second)] = p
            res[(first, second)] = {"n": n, "pos": sum(1 for x in d if x > 0), "zero": sum(1 for x in d if x == 0),
                                    "neg": sum(1 for x in d if x < 0), "median": float(np.median(d)) if d else None,
                                    "p": p, "rb_raw": pa.rank_biserial(d) if d else None}
        holm = pa.holm(p_by)
        for k, v in res.items():
            v["p_holm"] = holm[k]
            write("   %-16s - %-14s n %4d  >0 %4d  =0 %4d  <0 %4d  median %+.4f  p %.4g  Holm %.4g  %s  r_rb(raw) %+.3f"
                  % (k[0], k[1], v["n"], v["pos"], v["zero"], v["neg"], v["median"], v["p"], v["p_holm"],
                     "H0 rejected" if v["p_holm"] < pa.LOCKED.ALPHA else "not rejected", v["rb_raw"]))
        sup = {name: float(np.median([m[name]["sup_dist"] for m in complete.values()]))
               for name in ("cut-hide-uniform", "cut-hide", "cut-hide-rw", OURS)}
        write("   median SupDist: " + ", ".join("%s %.4f" % kv for kv in sup.items()))
        out[scope] = {"instances": len(complete), "excluded": excluded, "pairs": {"%s - %s" % k: v for k, v in res.items()},
                      "sup_dist": sup}
    return out


def lot_b(machine, write, directory=RESULTS):
    write("")
    write("=" * 78)
    write("Lot B (preregistration section 23.2): one constant of CUT-HIDE at a time, descriptive")
    write("=" * 78)
    files = [f for f in load_plain("SC3SENS", machine, directory) if f[1].get("methods") != "cut-hide-uniform"]
    if not files:
        write("   no SC3SENS file of lot B for machine %s yet" % machine)
        return None
    sc3 = sc3_plain(machine, directory)
    control = [dict(r, _eps=f[1].get("epsilon", "0.5")) for f in files for r in f[3]
               if r["method_key"] == OURS and float(f[1].get("epsilon", "0.5")) == 0.5]
    compared, differ = same_as_sc3([{k: v for k, v in r.items() if k != "_eps"} for r in control], sc3)
    write("   control: %d rows of cut-hide-rw-ls at the defaults compared with SC3, %d differ" % (compared, len(differ)))
    if differ or not compared:
        raise Failed("lot B: the control rows differ from SC3 (%d of %d): %s" % (len(differ), compared, differ[:3]))
    variants = []
    for name, options, _, rows in files:
        eps = options.get("epsilon", "0.5")
        for r in rows:
            label = r["method_key"] if float(eps) == 0.5 else "%s@eps%s" % (r["method_key"], eps)
            if label == OURS:
                continue
            variants.append((label, r))
    groups = {s: groups_of("SC3", "zero", s) for s in ("distinct", "repeated")}
    lot_a_sets = lot_a_instances(machine, directory) or {}
    out = []
    for label in sorted({v[0] for v in variants}):
        for scope in ("distinct", "repeated"):
            rs = [r for l, r in variants if l == label and pa.scope_of_set(r["sensitive_set"]) == scope
                  and pa.dataset_name(r["dataset"]) not in pa.LOCKED.REPORT_ONLY
                  and (groups[scope].get((pa.dataset_name(r["dataset"]), r["gap"], int(r["sigma"]))) or {}).get("group") == "main"]
            if not rs:
                continue
            lost_rel = [float(r["lost_rel"]) for r in rs if r["lost_rel"] != ""]
            more = fewer = masks = 0
            total = total_default = 0
            for r in rs:
                base = sc3[(pa.dataset_name(r["dataset"]), r["gap"], r["sigma"], r["sensitive_set"], OURS)]
                if r["lost"] != "" and base["lost"] != "":
                    more += int(r["lost"]) > int(base["lost"])
                    fewer += int(r["lost"]) < int(base["lost"])
                    total += int(r["lost"])
                    total_default += int(base["lost"])
                masks += r["mask_count"] != base["mask_count"]
            # Preregistration section 23.11: a divisor of tau changes no mask count; one difference stops the analysis.
            if label.startswith(OURS + "@tau") and masks:
                raise Failed("lot B: %s changes |R| on %d %s-item instances; preregistration section 23.11 stops the analysis"
                             % (label, masks, scope))
            # The instances must be those of lot A (sections 23.2 and 23.11): say how many are not.
            keys = {raw_instance(r) for r in rs}
            lot_a_keys = set(lot_a_sets[scope][0]) if scope in lot_a_sets else set()
            outside, absent = len(keys - lot_a_keys), len(lot_a_keys - keys)
            empty = sum(1 for r in rs if "B_eps empty" in r["method"])
            q = np.percentile(lost_rel, [25, 50, 75]) if lost_rel else [float("nan")] * 3
            rec = {"variant": label, "scope": scope, "n": len(rs), "q1": float(q[0]), "median": float(q[1]),
                   "q3": float(q[2]), "lost_more": more, "lost_fewer": fewer, "masks_differ": masks, "empty_band": empty,
                   "lost_total": total, "lost_total_default": total_default, "outside_lot_a": outside,
                   "lot_a_absent": absent}
            out.append(rec)
            write("   %-28s %-8s n %4d  Lost_rel median %.4f [%.4f, %.4f]  Lost more %3d fewer %3d  |R| differs %3d"
                  "  total Lost %5d (default %5d)%s%s"
                  % (label, scope, len(rs), rec["median"], rec["q1"], rec["q3"], more, fewer, masks, total, total_default,
                     "  empty band %d" % empty if "eps0.25" in label else "",
                     "  NOT the lot A instances: %d outside, %d absent" % (outside, absent) if outside or absent else ""))
    tau_lines(out, write)
    return out


TAU_DIVISORS = (("2", OURS + "@tau2"), ("4", OURS), ("8", OURS + "@tau8"), ("16", OURS + "@tau16"),
                ("64", OURS + "@tau64"), ("tau=1", OURS + "@tau1000000000"))


def tau_lines(records, write):
    """Preregistration section 23.11: the grid of the divisor d of tau, d = 4 being the default (its row is that of SC3), and the two
    expectations written before the run, each stated as held or contradicted."""
    by = {(r["variant"], r["scope"]): r for r in records}
    for scope in ("distinct", "repeated"):
        present = [(d, key) for d, key in TAU_DIVISORS if key == OURS or (key, scope) in by]
        if len(present) < len(TAU_DIVISORS):
            write("   tau grid %s: %d of %d divisors present; the grid is not reported" % (scope, len(present), len(TAU_DIVISORS)))
            continue
        default = {r["lost_total_default"] for k, r in by.items() if k[1] == scope and k[0].startswith(OURS + "@tau")}
        if len(default) != 1:
            raise Failed("lot B: the tau variants of the %s scope cover different instances (default totals %s)"
                         % (scope, sorted(default)))
        total = {d: (by[(key, scope)]["lost_total"] if key != OURS else default.copy().pop()) for d, key in TAU_DIVISORS}
        write("   tau grid %-8s total Lost: %s" % (scope, ", ".join("d=%s %d" % (d, total[d]) if d != "tau=1"
                                                                   else "tau=1 %d" % total[d] for d, _ in TAU_DIVISORS)))
        write("      expectation 1 (d=16 <= d=4): %s; expectation 2 (tau=1 >= d=8): %s"
              % ("held" if total["16"] <= total["4"] else "CONTRADICTED",
                 "held" if total["tau=1"] >= total["8"] else "CONTRADICTED"))


def lot_f(machine, write, directory=RESULTS):
    write("")
    write("=" * 78)
    write("Lot F (preregistration section 23.6): supports under wider gap constraints, descriptive")
    write("=" * 78)
    files = load_plain("SC3ROB", machine, directory)
    sup_files = load_plain("SC3ROBSUP", machine, directory)
    if not files:
        write("   no SC3ROB file for machine %s yet" % machine)
        return None
    sc3 = sc3_plain(machine, directory)
    compared, differ = same_as_sc3([r for f in files for r in f[3]], sc3)
    write("   check: %d SC3ROB rows compared with SC3, %d differ" % (compared, len(differ)))
    if differ or not compared:
        raise Failed("lot F: the SC3ROB rows differ from SC3 (%d of %d): %s" % (len(differ), compared, differ[:3]))
    stamps = {f[0].split("_", 1)[1] for f in files}
    sup_stamps = {f[0].split("_", 1)[1] for f in sup_files}
    if stamps != sup_stamps:
        raise Failed("lot F: %d SC3ROB files without their SC3ROBSUP file" % len(stamps - sup_stamps))
    groups = {s: groups_of("SC3", "zero", s) for s in ("distinct", "repeated")}
    count = defaultdict(lambda: [0, 0])
    for _, options, _, rows in sup_files:
        sigma = int(options["sigma"])
        for r in rows:
            scope = pa.scope_of_set(r["sensitive_set"])
            ds = pa.dataset_name(r["dataset"])
            if ds in pa.LOCKED.REPORT_ONLY or (groups[scope].get((ds, r["gap"], int(r["sigma"]))) or {}).get("group") != "main":
                continue
            c = count[(r["method_key"], scope, r["gap"], r["constraint"])]
            c[0] += 1
            c[1] += int(r["sup_no_after"]) >= sigma
    out = []
    for (m, scope, gap, constraint), (n, back) in sorted(count.items()):
        out.append({"method": m, "scope": scope, "gap": gap, "constraint": constraint, "patterns": n, "back": back,
                    "share": back / n})
        write("   %-10s %-8s gap %-6s constraint %-8s patterns %5d  at or above sigma %5d (%.3f)"
              % (m, scope, gap, constraint, n, back, back / n))
    return out


def wide_constraint(options):
    """'2x' or 'inf' for the hiding constraint of an SC3WIDE file, from its options; anything else stops."""
    g, h = options.get("gmax"), options.get("hideGmax")
    if h == "inf":
        return "inf"
    if g is not None and h is not None and h.isdigit() and int(h) == 2 * int(g):
        return "2x"
    raise Failed("SC3WIDE: hiding constraint hideGmax=%s with gmax=%s is neither 2g+ nor infinity" % (h, g))


def cross_pair(ours, other):
    """'masks/lost' of CUT-HIDE against one baseline on one instance: fewer, same or more for CUT-HIDE."""
    def cmp(a, b):
        return "fewer" if a < b else "same" if a == b else "more"
    return "%s/%s" % (cmp(int(ours["mask_count"]), int(other["mask_count"])), cmp(int(ours["lost"]), int(other["lost"])))


def lot_wide(machine, write, directory=RESULTS):
    """Preregistration section 23.10: every method hides under [g-, 2g+] and under [g-, inf], |S| = 1, descriptive."""
    write("")
    write("=" * 78)
    write("SC3WIDE (preregistration section 23.10): hiding under a wider gap constraint, descriptive")
    write("=" * 78)
    files = load_plain("SC3WIDE", machine, directory)
    if not files:
        write("   no SC3WIDE file for machine %s yet" % machine)
        return None
    instances = lot_a_instances(machine, directory)
    if instances is None:
        raise Failed("SC3WIDE: the lot A files are missing; the instances of lot A cannot be formed")
    sc3 = sc3_plain(machine, directory)
    rows = defaultdict(dict)  # (constraint, instance) -> method -> row
    for name, options, _, rs in files:
        constraint = wide_constraint(options)
        for r in rs:
            k = (constraint, raw_instance(r))
            if r["method_key"] in rows[k]:
                raise Failed("SC3WIDE: two rows of %s on %s (%s)" % (r["method_key"], k, name))
            rows[k][r["method_key"]] = r
    methods = (OURS,) + BASELINES
    # The two checks of preregistration section 23.10, over every row read (not only the lot A instances).
    checked = off = hf = 0
    for k, m in rows.items():
        for key, r in m.items():
            checked += 1
            hf += r["hf"] == "" or float(r["hf"]) > 0
            if key == OURS:
                off += r["mask_count"] != r["min_masks"]
    write("   checks: %d rows; CUT-HIDE off |R|* under the hiding constraint on %d; HF > 0 or missing on %d"
          % (checked, off, hf))
    if not checked or off or hf:
        raise Failed("SC3WIDE: %d rows, %d CUT-HIDE rows off |R|*, %d rows with HF > 0; preregistration section 23.10 stops the analysis"
                     % (checked, off, hf))
    out = []
    for scope in ("distinct", "repeated"):
        keys = set(instances[scope][0])
        for constraint in ("2x", "inf"):
            here = {k[1]: m for k, m in rows.items() if k[0] == constraint and k[1] in keys}
            not_hidden = sum(1 for m in here.values() for r in m.values() if r["status"])
            incomplete = sum(1 for m in here.values() if set(m) != set(methods))
            write("")
            write("-- %s-item scope, hiding under %s: %d of %d lot A instances have rows (%d without every method; "
                  "%d rows with a status)" % (scope, "[g-, 2g+]" if constraint == "2x" else "[g-, inf]",
                                              len(here), len(keys), incomplete, not_hidden))
            for method in methods:
                ratio, change = [], []
                for inst, m in here.items():
                    if method not in m or m[method]["status"]:
                        continue
                    base = sc3.get(inst[:1] + (inst[1], str(inst[2]), inst[3], method))
                    if base is None:
                        raise Failed("SC3WIDE: no SC3 row of %s on %s" % (method, inst))
                    ratio.append(int(m[method]["mask_count"]) / int(base["mask_count"]))
                    change.append(float(m[method]["lost_rel"]) - float(base["lost_rel"]))
                q = np.percentile(ratio, [25, 50, 75]) if ratio else [float("nan")] * 3
                rec = {"scope": scope, "constraint": constraint, "method": method, "n": len(ratio),
                       "ratio_q1": float(q[0]), "ratio_median": float(q[1]), "ratio_q3": float(q[2]),
                       "lost_rel_change_median": float(np.median(change)) if change else None,
                       "lost_rel_median": float(np.median([float(here[i][method]["lost_rel"]) for i in here
                                                           if method in here[i] and not here[i][method]["status"]]))
                       if ratio else None}
                if method != OURS:
                    cross = defaultdict(int)
                    for m in here.values():
                        if OURS in m and method in m and not m[OURS]["status"] and not m[method]["status"]:
                            cross[cross_pair(m[OURS], m[method])] += 1
                    rec["cross"] = dict(cross)
                out.append(rec)
                write("   %-15s n %4d  |R| / |R| in SC3: median %.3f [%.3f, %.3f]  Lost_rel median %.4f, change %+.4f%s"
                      % (method, rec["n"], rec["ratio_median"], rec["ratio_q1"], rec["ratio_q3"],
                         rec["lost_rel_median"], rec["lost_rel_change_median"],
                         "" if method == OURS else "  CUT-HIDE masks/lost vs it: " + ", ".join(
                             "%s %d" % kv for kv in sorted(rec["cross"].items()))))
    return out


# ---------------------------------------------------------------- main

def run(machine, write, directory=RESULTS, numbers_path=NUMBERS):
    with io.open(numbers_path, encoding="utf-8") as f:
        numbers = json.load(f)
    loaded = reproduce(machine, numbers, write, directory)
    rows, _ = loaded[("SC3", "zero")]
    result = {"machine": machine, "lot_d": {}, "lot_e": None}
    write("")
    write("=" * 78)
    write("Lot D (preregistration section 23.4), fixed after the main comparison on these files")
    write("=" * 78)
    for scope in ("distinct", "repeated"):
        result["lot_d"][scope] = lot_d(rows, groups_of("SC3", "zero", scope), scope, write)
    write("")
    write("=" * 78)
    write("Lot E (preregistration section 23.5)")
    write("=" * 78)
    result["lot_e"] = lot_e(loaded, numbers, write)
    result["lot_a"] = lot_a(machine, write, directory)
    result["lot_b"] = lot_b(machine, write, directory)
    result["lot_f"] = lot_f(machine, write, directory)
    result["lot_wide"] = lot_wide(machine, write, directory)
    return result


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--machine", default="ADMIN-PC")
    parser.add_argument("--out", default="")
    parser.add_argument("--json", default="")
    parser.add_argument("--numbers", default=NUMBERS,
                        help="the tables to rebuild (default results-invariant/paper_numbers.json); tools/paper_numbers.py "
                             "passes the tables it has just built, so the check never compares with a stale export")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)
    if args.self_test:
        return self_test()
    lines = []
    try:
        result = run(args.machine, lines.append, numbers_path=args.numbers)
    except Failed as e:
        print("\n".join(lines))
        print("FAILED: %s" % e, file=sys.stderr)
        return 1
    text = "\n".join(lines) + "\n"
    sys.stdout.write(text)
    if args.out:
        with io.open(args.out, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
    if args.json:
        with io.open(args.json, "w", encoding="utf-8", newline="\n") as f:
            json.dump(result, f, indent=1, sort_keys=True, default=str)
    return 0


def self_test():
    """Planted faults: each must make its check fail; a clean run must pass."""
    import copy
    import tempfile
    failures = []
    counted = []

    def check(name, ok):
        counted.append(name)
        print("%-78s %s" % (name, "ok" if ok else "FAILED"))
        if not ok:
            failures.append(name)

    with io.open(NUMBERS, encoding="utf-8") as f:
        numbers = json.load(f)
    lines = []
    try:
        reproduce("ADMIN-PC", numbers, lines.append)
        clean = True
    except Failed as e:
        clean = False
        print("  clean run failed: %s" % e)
    check("the reproduction check passes on the files of the paper", clean)

    def planted(edit):
        bad = copy.deepcopy(numbers)
        edit(bad["tables"])
        try:
            reproduce("ADMIN-PC", bad, [].append)
            return False
        except Failed:
            return True

    check("a changed count of equal-mask instances in h2 is caught",
          planted(lambda t: t["h2"]["rows"][0].__setitem__("same", t["h2"]["rows"][0]["same"] + 1)))
    check("a changed median difference in h5 is caught",
          planted(lambda t: t["h5"]["rows"][0].__setitem__("median_diff", t["h5"]["rows"][0]["median_diff"] + 0.0001)))
    check("a changed rank-biserial in h6 is caught",
          planted(lambda t: t["h6"]["rows"][1].__setitem__("rank_biserial", -t["h6"]["rows"][1]["rank_biserial"] - 0.5)))
    check("a changed median ratio in h3 is caught",
          planted(lambda t: t["h3"]["rows"][3].__setitem__("median", t["h3"]["rows"][3]["median"] + 0.0001)))
    check("a changed n in h6_ratio is caught",
          planted(lambda t: t["h6_ratio"]["rows"][2].__setitem__("n", t["h6_ratio"]["rows"][2]["n"] + 1)))
    # The loading itself: a loader that keeps protein_sdb1 in the tests must fail the check.
    saved = pa.LOCKED.REPORT_ONLY
    pa.LOCKED.REPORT_ONLY = ()
    try:
        reproduce("ADMIN-PC", numbers, [].append)
        caught = False
    except Failed:
        caught = True
    finally:
        pa.LOCKED.REPORT_ONLY = saved
    check("a loading that keeps the report-only dataset is caught", caught)
    # The bootstrap: a constant sample has a point interval; the interval of a skewed sample holds its median;
    # resampling within strata keeps each stratum's share (a stratum of a single value cannot move the median out).
    lo, hi = bootstrap_median([1.0] * 30, ["a"] * 30)
    check("the interval of a constant sample is that constant", lo == hi == 1.0)
    v = list(np.linspace(1, 2, 41))
    lo, hi = bootstrap_median(v, ["a"] * 20 + ["b"] * 21)
    check("the interval holds the median of the sample", lo <= float(np.median(v)) <= hi and lo < hi)
    check("the same seed gives the same interval", bootstrap_median(v, ["a"] * 41) == bootstrap_median(v, ["a"] * 41))
    # The smallest attainable p (rule of preregistration section 23.3): 2^-n without zeros for small n.
    check("the smallest p of 7 differences without zeros is 2^-7", abs(smallest_p(7, 0) - 2 ** -7) < 1e-12)
    check("a zero makes the smallest p larger", smallest_p(7, 1) > smallest_p(7, 0))
    # Preregistration sections 23.10 and 23.11: one planted row must stop each new lot, and the clean files must pass.
    real_load_plain = load_plain

    def stops(fn, scenario, edit):
        def patched(sc, machine, directory=RESULTS, closure="necessary"):
            got = real_load_plain(sc, machine, directory, closure)
            if sc == scenario:
                got = copy.deepcopy(got)
                edit(got)
            return got
        globals()["load_plain"] = patched
        try:
            fn("ADMIN-PC", [].append)
            return False
        except Failed:
            return True
        finally:
            globals()["load_plain"] = real_load_plain

    def first_row(files, method, options=None):
        return next(r for f in files if all(f[1].get(k) == v for k, v in (options or {}).items())
                    for r in f[3] if r["method_key"] == method)

    try:
        lot_wide("ADMIN-PC", [].append)
        lot_b("ADMIN-PC", [].append)
        clean_new = True
    except Failed as e:
        clean_new = False
        print("  clean run of lot B or SC3WIDE failed: %s" % e)
    check("lot B and SC3WIDE pass on the files of the paper", clean_new)
    check("a CUT-HIDE row off |R|* in SC3WIDE is caught",
          stops(lot_wide, "SC3WIDE", lambda f: first_row(f, OURS).__setitem__(
              "mask_count", str(int(first_row(f, OURS)["mask_count"]) + 1))))
    check("an SC3WIDE row with HF > 0 is caught",
          stops(lot_wide, "SC3WIDE", lambda f: first_row(f, "hha").__setitem__("hf", "0.010000")))
    check("a duplicated SC3WIDE row is caught",
          stops(lot_wide, "SC3WIDE", lambda f: f[0][3].append(dict(f[0][3][0]))))
    check("an SC3WIDE file whose hiding gap is neither 2g+ nor infinity is caught",
          stops(lot_wide, "SC3WIDE", lambda f: f[0][1].__setitem__("hideGmax", "5")))
    check("a tau divisor that changes |R| is caught",
          stops(lot_b, "SC3SENS", lambda f: first_row(f, OURS + "@tau16").__setitem__(
              "mask_count", str(int(first_row(f, OURS + "@tau16")["mask_count"]) + 1))))
    check("the same change on a non-tau variant is reported, not stopped",
          not stops(lot_b, "SC3SENS", lambda f: first_row(f, OURS + "@krw3").__setitem__(
              "mask_count", str(int(first_row(f, OURS + "@krw3")["mask_count"]) + 1))))
    print("%d checks, %d failed" % (len(counted), len(failures)))
    return 2 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
