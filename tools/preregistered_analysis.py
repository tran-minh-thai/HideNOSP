"""Statistical analysis of H2 and H3, doing exactly what config/preregistration.md locked.

This script does NOT choose the test, the threshold or the pairing. All of that was locked before
SC3 ran; here it is only read back and carried out. The constants taken from the preregistration
sit together in the class LOCKED below, each noting the section it comes from, so that they can be
checked against it by eye.

It does the following, in order:

1. Read the SC3 files and merge the reruns of a randomized method by their MEDIAN (section 5).
2. Group the configurations from the SC2 files of the pool profile (preregistration section 10:
   medians over the distinct-item candidates) with the locked thresholds, and compare every
   configuration with the locked table config/configuration_groups.tsv. A difference stops the
   analysis. H2 is tested on the main group only; the other groups are reported descriptively.
3. Exclude instances under section 5, counted by reason. Above 10% excluded, NO conclusion on H2.
4. Pair under section 5: only instances where the two methods use the SAME |R| enter Wilcoxon.
5. One-sided Pratt-style Wilcoxon signed-rank test on Lost_rel, with the Holm correction over the
   family of tier (b) baselines, and the rank-biserial effect size.
6. H3: the ratio r = |R|/|R|* on the main group and on the degenerate group.
7. Equivalence (section 4): the Hodges-Lehmann estimate with its confidence interval, against the
   locked margins.

Section 11 (2026-09-26): every SC3 row is put in one of two scopes by the items of its sensitive
patterns, distinct (the main scope, H2) or repeated (H2-R). Each scope is grouped from its own SC2
profile (pool, repeated) and checked against its own locked table; H2-R repeats the steps above on
the main group of the repeated scope and is never pooled with H2. Datasets in LOCKED.REPORT_ONLY
(protein_sdb1) are reported but never enter a Wilcoxon test.

Usage:

    python tools/preregistered_analysis.py
    python tools/preregistered_analysis.py --sc3 results-invariant --sc2 results-invariant
    python tools/preregistered_analysis.py --sc3 results-probe --skip-groups
    python tools/preregistered_analysis.py --sc2 results-invariant --write-groups

The last form locks both group tables; it is run once, from the official SC2 runs.

Exit code 1 when something makes the result unusable (too many instances excluded, groups that
differ from the locked table, a missing column), or when H2 or H2-R is not the locked test: the
method under test absent, no tier (b) baseline, or a Holm family smaller than the locked four.
Exit code 0 does not mean that H2 is supported; it only means that the analysis ran, and the
conclusion is in the tables printed.
"""
import argparse
import csv
import io
import os
import sys
from collections import defaultdict

import numpy as np
from scipy import stats


class LOCKED:
    """Constants locked in config/preregistration.md. Changing one here changes the locked plan."""

    DELTA_L = 0.02              # section 2, the practical-effect threshold of H2 on Lost_rel
    ALPHA = 0.05               # section 2
    # section 2, the Holm family of four. These are method_key values, i.e. the names given on the
    # SC3 command line, not the names the methods give themselves: SBSH-rmax calls itself "SBSH-r3"
    # when l_max = 3.
    BASELINE_FAMILY = ("hha", "dbsh", "sbsh-r1", "sbsh-rmax")
    # preregistration section 12: the method under test of H2 and H2-R; config/protocol.yaml
    # (method_H2R) must name the same key, and preregistered_analysis_test.py checks that it does.
    OUR_METHOD = "cut-hide-rw-ls"                            # our side of the difference

    RHO_MAIN = 0.20            # section 3
    RHO_DEGENERATE = 0.05      # section 3
    BETA_EFFECTIVE = 0.20      # section 3
    BETA_DEGENERATE = 0.05     # section 3

    R_ABOVE_1 = 1.10           # section 2, H3
    R_NEAR_1 = 1.05            # section 2, H3

    EQUIVALENCE_MARGIN = {"lost_rel": 0.02, "sup_dist": 0.01}  # section 4

    MAX_EXCLUDED_SHARE = 0.10  # section 5
    SEEDS = tuple(range(2026, 2036))                         # section 5, ten seeds

    # Section 10: the group of every configuration, locked in this file once the official SC2
    # runs of the pool profile exist. The script recomputes the groups and compares them.
    GROUP_TABLE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                               "config", "configuration_groups.tsv")
    # Section 11: the repeated-item scope has its own locked table, from SC2 --profile=repeated.
    GROUP_TABLE_REPEATED = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                                        "config", "configuration_groups_repeated.tsv")
    # Section 11, point 5: reported only, never in a Wilcoxon test (shares 64-75% of its
    # sequences with protein_sdb4, which is tested).
    REPORT_ONLY = ("protein_sdb1",)


# Columns this analysis cannot do without. Old SC3 files have no lost_rel and nonsensitive_before.
REQUIRED_COLUMNS = ("lost_rel", "nonsensitive_before", "mask_count", "hf", "method_key",
                    "sensitive_set")


# ---------------------------------------------------------------- reading files

def read_csv(path):
    """Return (the comment lines, the rows as dicts). A line starting with # is a comment."""
    comments, rows = [], []
    with io.open(path, encoding="utf-8-sig", newline="") as f:
        lines = []
        for line in f:
            if line.startswith("#"):
                comments.append(line.rstrip("\n"))
                continue
            lines.append(line)
        for r in csv.DictReader(lines):
            rows.append(r)
    return comments, rows


def collect_files(directory, prefix):
    if os.path.isfile(directory):
        return [directory]
    out = []
    for name in sorted(os.listdir(directory)):
        if name.startswith(prefix) and name.endswith(".csv"):
            out.append(os.path.join(directory, name))
    return out


def dataset_name(label):
    """Normalise a dataset label: SC2 writes a path, SC3 a path or 'synthetic'."""
    label = label.replace("\\", "/").strip()
    name = os.path.basename(label)
    return os.path.splitext(name)[0] if "." in name else name


def number(x, default=float("nan")):
    try:
        return float(x)
    except (TypeError, ValueError):
        return default


# ---------------------------------------------------------------- grouping

def scope_of_set(text):
    """The scope of a sensitive set as SC3 prints it, "[[4, 1, 4, 1]]": "distinct" when every
    pattern has distinct items, "repeated" when every pattern repeats one (section 11, point 4).
    A set mixing both kinds, or an unreadable one, is an error: the sampling never mixes them."""
    import re
    patterns = re.findall(r"\[([^\[\]]*)\]", text)
    if not patterns:
        raise ValueError("sensitive set %r has no pattern" % text)
    kinds = set()
    for p in patterns:
        items = [x.strip() for x in p.split(",") if x.strip()]
        if not items:
            raise ValueError("sensitive set %r has an empty pattern" % text)
        kinds.add("distinct" if len(set(items)) == len(items) else "repeated")
    if len(kinds) != 1:
        raise ValueError("sensitive set %r mixes distinct-item and repeated-item patterns" % text)
    return kinds.pop()


def items_of_pattern(text):
    """The items of a pattern as SC2 prints it, "[3, 5, 3]" -> ["3", "5", "3"]."""
    return [x.strip() for x in text.strip().strip("[]").split(",") if x.strip()]


def group_configurations(sc2_files, profile="pool"):
    """Group of every configuration (dataset, gap, sigma) under preregistration section 10.

    Only SC2 files written with --profile=pool count: their rows are every distinct-item candidate
    of the configuration, the patterns the main comparison hides. Files of the sampled profile are
    ignored, and a pool file holding a repeated-item pattern is an error. With profile="repeated"
    the same is done on the files of --profile=repeated, whose rows must all repeat an item. The
    check reads the items of the pattern itself: min_masks_exact no longer tells the two kinds apart
    once theorem P8 makes repeated-item bounds exact.
    """
    if profile not in ("pool", "repeated"):
        raise ValueError("profile is pool or repeated, got %r" % profile)
    by_pattern = defaultdict(dict)   # configuration -> pattern -> (rho_rep, beta)
    for path in sc2_files:
        comments, rows = read_csv(path)
        if "# profile %s" % profile not in comments:
            continue
        for r in rows:
            items = items_of_pattern(r.get("pattern", ""))
            distinct = len(set(items)) == len(items)
            if not items or distinct != (profile == "pool"):
                raise ValueError("%s: a %s profile holds %s patterns only, found %s"
                                 % (path, profile,
                                    "distinct-item" if profile == "pool" else "repeated-item",
                                    r.get("pattern")))
            key = (dataset_name(r["dataset"]), r["gap"], int(r["sigma"]))
            values = (number(r["rho_rep"]), number(r["beta"]))
            previous = by_pattern[key].get(r["pattern"])
            if previous is not None and previous != values:
                raise ValueError("%s: pattern %s of %s was profiled twice with different values"
                                 % (path, r["pattern"], key))
            # A second pool file of the same configuration (a rerun) must not count twice.
            by_pattern[key][r["pattern"]] = values
    rho = {k: [v[0] for v in m.values()] for k, m in by_pattern.items()}
    beta = {k: [v[1] for v in m.values()] for k, m in by_pattern.items()}
    out = {}
    for key, values in rho.items():
        m = float(np.median(values))
        b = float(np.median(beta[key]))
        if m >= LOCKED.RHO_MAIN:
            group = "main"
        elif m <= LOCKED.RHO_DEGENERATE:
            group = "degenerate"
        else:
            group = "intermediate"
        if b >= LOCKED.BETA_EFFECTIVE:
            beta_group = "effective"
        elif b <= LOCKED.BETA_DEGENERATE:
            beta_group = "degenerate"
        else:
            beta_group = "between"
        out[key] = {"group": group, "rho_rep": m, "beta": b, "beta_group": beta_group,
                    "pool": len(values)}
    return out


TABLE_COLUMNS = ("dataset", "gap", "sigma", "pool", "rho_rep_median", "beta_median", "group",
                 "beta_group")


def write_group_table(groups, path):
    """Write the group table: done once, from the official SC2 runs, then committed (locked)."""
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        profile = "repeated" if path == LOCKED.GROUP_TABLE_REPEATED else "pool"
        f.write("# Groups of preregistration section %s, from SC2 --profile=%s. Locked by commit.\n"
                % ("11" if profile == "repeated" else "10", profile))
        f.write("\t".join(TABLE_COLUMNS) + "\n")
        for key in sorted(groups):
            v = groups[key]
            f.write("\t".join([key[0], key[1], str(key[2]), str(v["pool"]),
                               "%.6f" % v["rho_rep"], "%.6f" % v["beta"], v["group"],
                               v["beta_group"]]) + "\n")


def read_group_table(path):
    """The locked table as {(dataset, gap, sigma): row}, or None when it does not exist yet."""
    if not os.path.exists(path):
        return None
    out = {}
    with io.open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("#") or line.startswith("dataset\t") or not line.strip():
                continue
            o = line.rstrip("\n").split("\t")
            out[(o[0], o[1], int(o[2]))] = dict(zip(TABLE_COLUMNS, o))
    return out


def check_groups(groups, write, table=None, profile="pool"):
    """Compare every recomputed group with the locked table. Any difference stops the analysis."""
    table_path = LOCKED.GROUP_TABLE if profile == "pool" else LOCKED.GROUP_TABLE_REPEATED
    table = read_group_table(table_path) if table is None else table
    write("")
    write("Groups of the configurations (section %s: medians over the %s candidates;"
          " rho_rep >= %.2f main, <= %.2f degenerate)"
          % ("10" if profile == "pool" else "11",
             "distinct-item" if profile == "pool" else "repeated-item",
             LOCKED.RHO_MAIN, LOCKED.RHO_DEGENERATE))
    if not groups:
        write("  NO SC2 file of the %s profile: nothing to group. Run SC2 with --profile=%s."
              % (profile, profile))
        return False
    for key in sorted(groups):
        v = groups[key]
        write("  %-16s %-7s sigma %-6d pool %4d  rho_rep %.3f  beta %.3f  -> %s / beta %s"
              % (key[0], key[1], key[2], v["pool"], v["rho_rep"], v["beta"], v["group"],
                 v["beta_group"]))
    if table is None:
        write("")
        write("  The locked table %s does not exist yet. Write it once from the official SC2 runs"
              % table_path)
        write("  (--write-groups) and commit it before any SC3 result is read.")
        return False
    differences = []
    for key in sorted(set(groups) | set(table)):
        if key not in groups:
            differences.append("%s is locked but has no SC2 pool file here" % (key,))
        elif key not in table:
            differences.append("%s has an SC2 pool file but is not in the locked table" % (key,))
        elif (groups[key]["group"] != table[key]["group"]
              or groups[key]["beta_group"] != table[key]["beta_group"]):
            differences.append("%s: recomputed %s / %s, locked %s / %s"
                               % (key, groups[key]["group"], groups[key]["beta_group"],
                                  table[key]["group"], table[key]["beta_group"]))
    if differences:
        write("")
        write("  DIFFERENT from the locked table:")
        for d in differences:
            write("    " + d)
        return False
    write("  %d configurations, all as locked." % len(groups))
    return True


# ---------------------------------------------------------------- merging reruns

def merge_reruns(rows, write):
    """Merge the reruns of a randomized method by their MEDIAN (section 5).

    The unit merged is (dataset, shape, gap, sigma, sensitive set, method). A deterministic method
    has a single row, and the median of one number is that number.
    """
    runs = defaultdict(list)
    for r in rows:
        key = (dataset_name(r["dataset"]), r.get("shape", ""), r["gap"], int(r["sigma"]),
               r["sensitive_set"], r["method_key"])
        runs[key].append(r)

    numeric_columns = ("hf", "lost", "lost_rel", "ghost", "dist_r", "sup_dist", "mask_count",
                       "min_masks", "mask_ratio", "lower_bound", "achieved_ratio",
                       "frequent_before", "nonsensitive_before", "frequent_after")
    out = []
    missing_seeds = []
    for key, many in sorted(runs.items()):
        first = many[0]
        seeds = sorted({r.get("method_seed", "") for r in many} - {""})
        if seeds and len(seeds) != len(LOCKED.SEEDS):
            missing_seeds.append((key[5], len(seeds)))
        merged = {
            "dataset": key[0], "shape": key[1], "gap": key[2], "sigma": key[3],
            "sensitive_set": key[4], "method_key": key[5],
            "method": first.get("method", key[5]),
            "runs": len(many), "seeds": len(seeds),
            "status": first.get("status", ""),
        }
        for c in numeric_columns:
            values = [number(r.get(c, "")) for r in many]
            values = [v for v in values if not np.isnan(v)]
            merged[c] = float(np.median(values)) if values else float("nan")
        out.append(merged)

    if missing_seeds:
        write("")
        write("  WARNING: a randomized method must run with %d seeds (%d-%d) under section 5, but:"
              % (len(LOCKED.SEEDS), LOCKED.SEEDS[0], LOCKED.SEEDS[-1]))
        for name, n in sorted(set(missing_seeds)):
            write("    %-26s has only %d seeds" % (name, n))
        write("  A median over fewer seeds is another quantity, not for a table in the paper.")
    return out


# ---------------------------------------------------------------- Pratt-style Wilcoxon

def pratt_ranks(d):
    """Ranks of |d| the Pratt way: zero differences ARE ranked, they only stay out of W+ and W-.

    The original Wilcoxon test drops the zero differences. Lost is a small integer, so zero
    differences are very common; dropping them loses the information "the two methods are level"
    and inflates the effect size. Section 5 locks Pratt for that reason.
    """
    d = np.asarray(d, dtype=float)
    r = stats.rankdata(np.abs(d))
    w_plus = float(r[d > 0].sum())
    w_minus = float(r[d < 0].sum())
    return w_plus, w_minus


def rank_biserial(d):
    """(W+ - W-)/(W+ + W-), the formula of section 2, not the variant 2W+/(n(n+1)) - 1."""
    w_plus, w_minus = pratt_ranks(d)
    total = w_plus + w_minus
    return float("nan") if total == 0 else (w_plus - w_minus) / total


def wilcoxon_one_sided(d, shift=0.0):
    """One-sided Pratt-style Wilcoxon signed-rank test; alternative: the median of d exceeds shift.

    Returns (statistic, p, effect size, n, number of zero differences after subtracting shift).
    """
    x = np.asarray(d, dtype=float) - shift
    n = len(x)
    if n == 0:
        return float("nan"), float("nan"), float("nan"), 0, 0
    zeros = int((x == 0).sum())
    if zeros == n:
        # Every difference equals the shift exactly: no evidence in either direction.
        return 0.0, 1.0, 0.0, n, zeros
    # method MUST be "auto". In scipy 1.18, method="exact" under Pratt returns a WRONG value: it
    # uses the standard signed-rank distribution over all n observations, ignoring that the ranks
    # of the zero differences may not take a sign. On an example of 12 observations with 4 zeros,
    # enumerating by hand gives p = 29/256 = 0.113281; "auto" gives exactly that, "exact" gives
    # 0.211914 and "approx" 0.099370. preregistered_analysis_test.test_wilcoxon_is_pratt_not_wilcox
    # pins it.
    result = stats.wilcoxon(x, zero_method="pratt", alternative="greater", method="auto")
    return float(result.statistic), float(result.pvalue), rank_biserial(x), n, zeros


def holm(p_by_name):
    """Holm-Bonferroni. Returns a dict name -> adjusted p, kept monotone."""
    items = sorted(p_by_name.items(), key=lambda kv: kv[1])
    m = len(items)
    out = {}
    previous = 0.0
    for i, (name, p) in enumerate(items):
        adjusted = min(1.0, (m - i) * p)
        previous = max(previous, adjusted)
        out[name] = previous
    return out


def hodges_lehmann(d, alpha=0.05):
    """HL estimate of the median difference, with a Wilcoxon-based confidence interval (section 4).

    The estimate is the median of the Walsh averages (d_i + d_j)/2, i <= j. The ends of the interval
    come from the normal approximation of the signed-rank statistic; with a small n the interval
    gets wider, it does not point the wrong way.
    """
    x = np.asarray(d, dtype=float)
    n = len(x)
    if n < 2:
        return float("nan"), float("nan"), float("nan")
    walsh = np.array([(x[i] + x[j]) / 2 for i in range(n) for j in range(i, n)])
    walsh.sort()
    estimate = float(np.median(walsh))
    z = stats.norm.ppf(1 - alpha / 2)
    mean = n * (n + 1) / 4
    deviation = np.sqrt(n * (n + 1) * (2 * n + 1) / 24)
    k = int(np.floor(mean - z * deviation))
    if k < 0:
        k = 0
    if k >= len(walsh):
        return estimate, float("nan"), float("nan")
    return estimate, float(walsh[k]), float(walsh[len(walsh) - 1 - k])


# ---------------------------------------------------------------- pairing and exclusion

def pair_up(rows, baseline, quantity):
    """Pair CUT-HIDE with one baseline on every instance, under the rule of section 5.

    Returns four bins:
      "tested"         same |R|, goes into Wilcoxon
      "baseline more"  the baseline uses MORE masks: reported apart, not tested
      "ours more"      CUT-HIDE uses more masks -- section 5 does NOT cover this case, see run_h2
      "excluded"       excluded, with the reason
    """
    by_instance = defaultdict(dict)
    for r in rows:
        key = (r["dataset"], r["gap"], r["sigma"], r["sensitive_set"])
        by_instance[key][r["method_key"]] = r

    bins = {"tested": [], "baseline more": [], "ours more": [], "excluded": []}
    for key, by_method in sorted(by_instance.items()):
        ours = by_method.get(LOCKED.OUR_METHOD)
        theirs = by_method.get(baseline)
        if ours is None or theirs is None:
            continue
        reason = exclusion_reason(ours, theirs)
        if reason:
            bins["excluded"].append((key, reason))
            continue
        difference = theirs[quantity] - ours[quantity]
        entry = (key, difference, ours, theirs)
        if ours["mask_count"] == theirs["mask_count"]:
            bins["tested"].append(entry)
        elif theirs["mask_count"] > ours["mask_count"]:
            bins["baseline more"].append(entry)
        else:
            bins["ours more"].append(entry)
    return bins


def exclusion_reason(ours, theirs):
    for label, r in (("ours", ours), ("baseline", theirs)):
        if r["status"]:
            return "%s has status %s" % (label, r["status"])
        if not np.isnan(r["hf"]) and r["hf"] > 0:
            return "%s still has HF = %.3f > 0" % (label, r["hf"])
    return None


# ---------------------------------------------------------------- H2

def run_h2(rows, write, groups=None, hypothesis="H2"):
    write("")
    write("=" * 78)
    write("%s  median of Lost_rel(baseline) - Lost_rel(CUT-HIDE) greater than delta_L = %.2f"
          % (hypothesis, LOCKED.DELTA_L))
    write("=" * 78)

    reported = [r for r in rows if r["dataset"] in LOCKED.REPORT_ONLY]
    if reported:
        write("")
        write("  %d rows of %s are reported only and do not enter the test (section 11, point 5)."
              % (len(reported), ", ".join(sorted({r["dataset"] for r in reported}))))
        rows = [r for r in rows if r["dataset"] not in LOCKED.REPORT_ONLY]

    if groups is not None:
        # Preregistration section 3 keeps the intermediate group out of H2, and the design keeps
        # the degenerate group out of the evidence for the contribution: H2 is the main group.
        by_group = defaultdict(int)
        kept = []
        for r in rows:
            info = groups.get((r["dataset"], r["gap"], r["sigma"]))
            group = info["group"] if info else "ungrouped"
            by_group[group] += 1
            if group == "main":
                kept.append(r)
        write("")
        write("  rows by group: "
              + ", ".join("%s %d" % (k, v) for k, v in sorted(by_group.items())))
        write("  H2 is tested on the %d rows of the main group only." % len(kept))
        rows = kept
    else:
        write("")
        write("  PROBE: no group filter (--skip-groups); every row enters H2.")

    if not present(rows, LOCKED.OUR_METHOD):
        keys = sorted({r["method_key"] for r in rows})
        write("")
        write("  NO '%s' in the files. H2 compares two sides, and without this side there is"
              % LOCKED.OUR_METHOD)
        write("  nothing to test. The files hold: %s" % ", ".join(keys))
        write("  Note: 'cut-hide-uniform', 'cut-hide' and 'cut-hide-rw' are CONTROL variants and")
        write("  cannot stand in for '%s' in H2 (preregistration, section 12)." % LOCKED.OUR_METHOD)
        write("  %s NOT TESTED: the exit code is 1." % hypothesis)
        return False

    family = [b for b in LOCKED.BASELINE_FAMILY if present(rows, b)]
    absent = [b for b in LOCKED.BASELINE_FAMILY if b not in family]
    if absent:
        write("")
        write("  Absent from the files: %s" % ", ".join(absent))
        write("  The Holm family has %d members left. Section 2 requires this to be amended in the"
              % len(family))
        write("  preregistration BEFORE running, not after seeing that a file lacks them.")
    if not family:
        write("")
        write("  No tier (b) baseline in the files. H2 cannot be tested.")
        write("  %s NOT TESTED: the exit code is 1." % hypothesis)
        return False

    results = {}
    total_pairs = 0
    total_excluded = 0
    for name in family:
        bins = pair_up(rows, name, "lost_rel")
        n_used = len(bins["tested"]) + len(bins["baseline more"]) + len(bins["ours more"])
        total_pairs += n_used + len(bins["excluded"])
        total_excluded += len(bins["excluded"])

        write("")
        write("-- %s" % name)
        write("   into Wilcoxon (same |R|)             %4d instances" % len(bins["tested"]))
        write("   baseline uses more masks             %4d instances  (reported apart, not tested)"
              % len(bins["baseline more"]))
        write("   CUT-HIDE uses more masks             %4d instances  (a case section 5 omits)"
              % len(bins["ours more"]))
        write("   excluded                             %4d instances" % len(bins["excluded"]))
        for key, reason in bins["excluded"][:5]:
            write("       %s  %s" % (key[3][:40], reason))
        if len(bins["excluded"]) > 5:
            write("       ... and %d more" % (len(bins["excluded"]) - 5))

        if bins["ours more"]:
            write("   NOTE: in %d instances CUT-HIDE uses more masks than the baseline. Section 5"
                  % len(bins["ours more"]))
            write("   defines two regimes (same |R|, and the baseline using more), not this case.")
            write("   Dropping it silently would favour us, so it is counted apart for a decision.")

        d = [m[1] for m in bins["tested"]]
        if len(d) < 2:
            write("   not enough pairs for a test")
            continue
        statistic, p, effect, n, zeros = wilcoxon_one_sided(d, LOCKED.DELTA_L)
        median = float(np.median(d))
        absolute = float(np.median([m[3]["lost"] - m[2]["lost"] for m in bins["tested"]]))
        results[name] = p
        write("   median difference of Lost_rel        %+.4f   (absolute: %+.1f patterns)"
              % (median, absolute))
        write("   Wilcoxon-Pratt one-sided, H1 > %.2f  W = %.1f, p = %.4g, n = %d"
              " (%d differences of 0)" % (LOCKED.DELTA_L, statistic, p, n, zeros))
        write("   rank-biserial                        %+.3f" % effect)

    if total_pairs:
        share = total_excluded / total_pairs
        write("")
        write("Share of instances excluded: %d/%d = %.1f%%  (the threshold of section 5 is %.0f%%)"
              % (total_excluded, total_pairs, 100 * share, 100 * LOCKED.MAX_EXCLUDED_SHARE))
        if share > LOCKED.MAX_EXCLUDED_SHARE:
            write("NO CONCLUSION ON H2: the share excluded exceeds the locked threshold.")
            return False

    if len(results) >= 2:
        adjusted = holm(results)
        write("")
        write("Holm over the family of %d tier (b) baselines, alpha = %.2f"
              % (len(results), LOCKED.ALPHA))
        write("  %-12s %12s %12s   %s" % ("baseline", "raw p", "Holm p", "verdict"))
        for name in sorted(adjusted, key=lambda k: results[k]):
            rejected = adjusted[name] < LOCKED.ALPHA
            write("  %-12s %12.4g %12.4g   %s"
                  % (name, results[name], adjusted[name],
                     "H0 rejected" if rejected else "not rejected"))
    elif len(results) == 1:
        write("")
        write("Only one baseline could be tested; no Holm correction is needed.")
    if absent:
        write("")
        write("NOT THE LOCKED TEST: the Holm family above has %d of its %d locked members, so the"
              % (len(family), len(LOCKED.BASELINE_FAMILY)))
        write("correction and the verdicts differ from those of section 2. The exit code is 1.")
        return False
    return True


def present(rows, key):
    return any(r["method_key"] == key for r in rows)


# ---------------------------------------------------------------- pairs (|R|, Lost), section 5

def run_dominance(rows, groups, write):
    """The report that section 5 asks for beside H2, DESCRIPTIVE: no test.

    Section 5 sends the instances where the baseline uses more masks to a report "as pairs (|R|,
    Lost)", because comparing Lost at two values of |R| compares two different things. For each
    tier (b) baseline this counts, on the same rows as H2 (main group, report-only datasets left
    out), how often CUT-HIDE loses fewer, as many or more patterns, in each regime of |R|, and on
    how many instances it is no worse on both |R| and Lost.
    """
    write("")
    write("=" * 78)
    write("Pairs (|R|, Lost) of section 5, DESCRIPTIVE: %s against each tier (b) baseline"
          % LOCKED.OUR_METHOD)
    write("=" * 78)
    rows = [r for r in rows if r["dataset"] not in LOCKED.REPORT_ONLY]
    if groups is not None:
        rows = [r for r in rows
                if (groups.get((r["dataset"], r["gap"], r["sigma"])) or {}).get("group") == "main"]
        write("  rows: main group, without %s (the rows of H2): %d"
              % (", ".join(LOCKED.REPORT_ONLY), len(rows)))
    else:
        write("  PROBE: no group filter (--skip-groups): %d rows" % len(rows))
    if not present(rows, LOCKED.OUR_METHOD):
        write("  no '%s' row: nothing to pair" % LOCKED.OUR_METHOD)
        return
    write("  Lost compared as numbers of patterns; '<' means %s loses fewer." % LOCKED.OUR_METHOD)
    write("  %-10s %9s | %-20s | %-20s | %9s %13s"
          % ("baseline", "instances", "same |R|: < = >", "baseline more: < = >", "ours more",
             "no worse both"))
    for name in LOCKED.BASELINE_FAMILY:
        if not present(rows, name):
            write("  %-10s absent from the files" % name)
            continue
        bins = pair_up(rows, name, "lost_rel")

        def split(entries):
            less = sum(1 for e in entries if e[2]["lost"] < e[3]["lost"])
            same = sum(1 for e in entries if e[2]["lost"] == e[3]["lost"])
            return less, same, len(entries) - less - same

        same_r, more_r = split(bins["tested"]), split(bins["baseline more"])
        total = len(bins["tested"]) + len(bins["baseline more"]) + len(bins["ours more"])
        no_worse = same_r[0] + same_r[1] + more_r[0] + more_r[1]
        write("  %-10s %9d | %6d %6d %6d | %6d %6d %6d | %9d %6d/%d"
              % (name, total, same_r[0], same_r[1], same_r[2], more_r[0], more_r[1], more_r[2],
                 len(bins["ours more"]), no_worse, total))
        if bins["excluded"]:
            write("  %-10s %d excluded instances not counted (status or HF > 0)"
                  % ("", len(bins["excluded"])))


# ---------------------------------------------------------------- H3

def run_h3(rows, groups, write, descriptive=False):
    write("")
    write("=" * 78)
    if descriptive:
        write("r = |R|/|R|* per method and group, DESCRIPTIVE (section 11, point 7): no verdict")
    else:
        write("H3  r = |R|/|R|* above 1 on the main group (r >= %.2f), near 1 on the degenerate"
              " group (r <= %.2f)" % (LOCKED.R_ABOVE_1, LOCKED.R_NEAR_1))
    write("=" * 78)
    reported = [r for r in rows if r["dataset"] in LOCKED.REPORT_ONLY]
    if reported:
        write("")
        write("  %d rows of %s are reported only and left out here (section 11, point 5)."
              % (len(reported), ", ".join(sorted({r["dataset"] for r in reported}))))
        rows = [r for r in rows if r["dataset"] not in LOCKED.REPORT_ONLY]

    by_group = defaultdict(lambda: defaultdict(list))
    ungrouped = 0
    without_r = 0
    for r in rows:
        if np.isnan(r["mask_ratio"]):
            without_r += 1
            continue
        key = (r["dataset"], r["gap"], r["sigma"])
        info = groups.get(key)
        if info is None:
            ungrouped += 1
            continue
        # Keyed on method_key, the stable name given on the command line. The display name carries
        # parameters: SBSH-rmax calls itself SBSH-r2, -r3 or -r4 by the length of the pattern, so
        # keying on it split one baseline into three rows (found on the official SC3 of 2026-10-04).
        by_group[info["group"]][r["method_key"]].append(r["mask_ratio"])

    write("")
    write("  rows without |R|* (several patterns, or repeated items with g- > 0): %d" % without_r)
    if ungrouped:
        write("  rows in no group (no matching configuration in SC2): %d" % ungrouped)
    if not descriptive:
        write("  Section 2 requires saying so: H3 is tested only on instances of one distinct-item"
              " pattern.")

    for group in ("main", "degenerate"):
        write("")
        write("-- group %s" % group)
        if not by_group[group]:
            write("   no rows")
            continue
        write("   %-34s %5s %8s %8s   %s" % ("method", "n", "median", "max", "verdict"))
        for name in sorted(by_group[group]):
            v = by_group[group][name]
            med = float(np.median(v))
            if descriptive:
                write("   %-34s %5d %8.4f %8.4f" % (name[:34], len(v), med, max(v)))
                continue
            if name not in LOCKED.BASELINE_FAMILY:
                # Section 2 states H3 for the tier (b) baselines; other methods are shown only.
                verdict = "(not a tier (b) baseline: shown, not judged)"
            elif group == "main":
                verdict = ("above 1" if med >= LOCKED.R_ABOVE_1
                           else "near 1" if med <= LOCKED.R_NEAR_1 else "no conclusion")
            else:
                verdict = ("near 1" if med <= LOCKED.R_NEAR_1
                           else "above 1" if med >= LOCKED.R_ABOVE_1 else "no conclusion")
            write("   %-34s %5d %8.4f %8.4f   %s" % (name[:34], len(v), med, max(v), verdict))

    family_main = [v for k, v in by_group["main"].items() if k in LOCKED.BASELINE_FAMILY]
    if not descriptive and not by_group["degenerate"]:
        write("")
        write("  The second half of H3 (r near 1 on the degenerate group) is NOT TESTED: the SC3")
        write("  files hold no row of the degenerate group.")
    if not descriptive and family_main and all(
            float(np.median(v)) <= LOCKED.R_NEAR_1 for v in family_main):
        write("")
        write("  Every tier (b) baseline gives r near 1 on the main group: H3 is NOT rejected. Section 2")
        write("  anticipated this case -- it is a negative result to be reported under rule 3.4.9,")
        write("  not an implementation error.")


# ---------------------------------------------------------------- equivalence, section 4

def run_equivalence(rows, write):
    write("")
    write("=" * 78)
    write("Equivalence (section 4): Hodges-Lehmann with a %.0f%% confidence interval, against the"
          " locked margins" % (100 * (1 - LOCKED.ALPHA)))
    write("=" * 78)
    rows = [r for r in rows if r["dataset"] not in LOCKED.REPORT_ONLY]
    family = [b for b in LOCKED.BASELINE_FAMILY if present(rows, b)]
    for quantity, margin in LOCKED.EQUIVALENCE_MARGIN.items():
        write("")
        write("-- %s, equivalence margin +/- %.2f" % (quantity, margin))
        if not family:
            write("   no tier (b) baseline")
            continue
        write("   %-12s %5s %10s %22s   %s" % ("baseline", "n", "HL", "CI", "verdict"))
        for name in family:
            bins = pair_up(rows, name, quantity)
            d = [m[1] for m in bins["tested"]]
            if len(d) < 2:
                write("   %-12s %5d %10s %22s   not enough pairs" % (name, len(d), "-", "-"))
                continue
            hl, low, high = hodges_lehmann(d, LOCKED.ALPHA)
            if np.isnan(low) or np.isnan(high):
                verdict = "interval not computable"
            elif low > -margin and high < margin:
                verdict = "equivalent"
            elif low >= margin or high <= -margin:
                verdict = "different beyond the margin"
            else:
                verdict = "no conclusion"
            write("   %-12s %5d %10.4f [%+8.4f, %+8.4f]   %s"
                  % (name, len(d), hl, low, high, verdict))


# ---------------------------------------------------------------- running

def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--sc3", default="results-invariant",
                        help="directory or CSV file of SC3 (default results-invariant)")
    parser.add_argument("--sc2", default="results-invariant",
                        help="directory or CSV file of SC2, used to group the configurations")
    parser.add_argument("--skip-groups", action="store_true",
                        help="do not compare the configurations with the locked table (probe runs)")
    parser.add_argument("--out", default="",
                        help="write the report to this file instead of the screen")
    parser.add_argument("--write-groups", action="store_true",
                        help="write config/configuration_groups.tsv from the SC2 pool files and "
                             "stop; done once, from the official runs, and committed")
    args = parser.parse_args(argv)

    out_lines = []

    def write(s=""):
        out_lines.append(s)

    if args.write_groups:
        files = collect_files(args.sc2, "SC2_")
        groups = group_configurations(files)
        if not groups:
            print("no SC2 file of the pool profile in %s" % args.sc2, file=sys.stderr)
            return 1
        write_group_table(groups, LOCKED.GROUP_TABLE)
        print("wrote %s (%d configurations)" % (LOCKED.GROUP_TABLE, len(groups)))
        # Section 11, point 2: the repeated-item scope, from SC2 --profile=repeated.
        repeated = group_configurations(files, profile="repeated")
        if repeated:
            write_group_table(repeated, LOCKED.GROUP_TABLE_REPEATED)
            print("wrote %s (%d configurations)" % (LOCKED.GROUP_TABLE_REPEATED, len(repeated)))
        else:
            print("no SC2 file of the repeated profile in %s: %s not written"
                  % (args.sc2, LOCKED.GROUP_TABLE_REPEATED), file=sys.stderr)
            return 1
        return 0

    sc3_files = collect_files(args.sc3, "SC3_")
    if not sc3_files:
        print("no SC3 file found in %s" % args.sc3, file=sys.stderr)
        return 1

    write("Statistical analysis of H2 and H3 under config/preregistration.md")
    write("")
    write("SC3 files read:")
    # Check the columns file by file, not on the first row of the whole batch: a directory can mix
    # old and new files, and dropping the whole batch for one old file throws the good ones away.
    raw_rows = []
    skipped = []
    for path in sc3_files:
        comments, rows = read_csv(path)
        header = next((c for c in comments if c.startswith("# run ")), "# run ?")
        missing = [c for c in REQUIRED_COLUMNS if not rows or c not in rows[0]]
        if missing:
            skipped.append((os.path.basename(path), missing))
            write("  %-58s %d rows  SKIPPED (missing %s)"
                  % (os.path.basename(path), len(rows), ", ".join(missing)))
            continue
        write("  %-58s %d rows  %s" % (os.path.basename(path), len(rows), header[6:]))
        raw_rows.extend(rows)

    if skipped:
        write("")
        write("  %d files skipped because an old version of the code wrote them. If they hold"
              % len(skipped))
        write("  numbers for the paper, rerun SC3 with the current code; old probe files may go.")
    if not raw_rows:
        write("")
        write("NO USABLE FILE LEFT.")
        emit(out_lines, args.out)
        return 1

    rows = merge_reruns(raw_rows, write)
    write("")
    write("After merging the reruns by their median: %d rows (from %d)"
          % (len(rows), len(raw_rows)))

    # Section 11: split by the items of the sensitive patterns, never by file name or option.
    by_scope = {"distinct": [], "repeated": []}
    for r in rows:
        by_scope[scope_of_set(r["sensitive_set"])].append(r)
    write("")
    write("Rows by scope (section 11): distinct items %d, repeated items %d"
          % (len(by_scope["distinct"]), len(by_scope["repeated"])))

    sc2_files = collect_files(args.sc2, "SC2_")
    all_valid = True
    for scope, profile, name in (("distinct", "pool", "H2"), ("repeated", "repeated", "H2-R")):
        scope_rows = by_scope[scope]
        if not scope_rows:
            continue
        write("")
        write("#" * 78)
        write("Scope: %s-item sensitive patterns, %d rows, tested as %s"
              % (scope, len(scope_rows), name))
        write("#" * 78)
        groups = {}
        groups_valid = True
        if sc2_files:
            groups = group_configurations(sc2_files, profile=profile)
            if not args.skip_groups:
                groups_valid = check_groups(groups, write, profile=profile)
        else:
            write("")
            write("  No SC2 file: no groups, so H3 cannot split the main and degenerate groups.")
            # Without groups H2 has no main group to be tested on; only a probe may go on.
            groups_valid = args.skip_groups

        h2_valid = run_h2(scope_rows, write, None if args.skip_groups else groups, name)
        run_dominance(scope_rows, None if args.skip_groups else groups, write)
        # H3 is preregistered for the main scope; section 11, point 7 makes the ratio of the
        # repeated scope descriptive.
        run_h3(scope_rows, groups, write, descriptive=(scope == "repeated"))
        run_equivalence(scope_rows, write)
        all_valid = all_valid and h2_valid and groups_valid

    write("")
    write("=" * 78)
    write("Reminder: exit code 0 does NOT mean that H2 is supported. It only means that the")
    write("analysis ran, and the conclusion is in the tables above.")

    emit(out_lines, args.out)
    return 0 if all_valid else 1


def emit(out_lines, path):
    text = "\n".join(out_lines) + "\n"
    if path:
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
        print("wrote %s" % path)
    else:
        sys.stdout.write(text)


if __name__ == "__main__":
    sys.exit(main())
