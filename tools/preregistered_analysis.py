# SPDX-License-Identifier: MIT
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

Preregistration section 11: every SC3 row is put in one of two scopes by the items of its sensitive
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
import re
import sys
from collections import defaultdict

import numpy as np
from scipy import stats

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import closure_rule  # noqa: E402
import sc3_commands  # noqa: E402


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
    # section 19.1: the competitors that stop at the threshold, H4a and H4b; Holm over these two
    STOP_FAMILY = ("occurrence", "hha-stop")
    STOP_METHODS = "occurrence,hha-stop"                     # the methods option of their batch

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
    # Preregistration section 22.3: the gaps with g- > 0 have tables of their own, written from their SC2 files.
    GAP_GROUP_TABLE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                                   "config", "configuration_groups_gap.tsv")
    GAP_GROUP_TABLE_REPEATED = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                                            "config", "configuration_groups_gap_repeated.tsv")
    # Section 11, point 5: reported only, never in a Wilcoxon test (shares 64-75% of its
    # sequences with protein_sdb4, which is tested).
    REPORT_ONLY = ("protein_sdb1",)


# Columns this analysis cannot do without. Old SC3 files have no lost_rel and nonsensitive_before.
# Preregistration section 17: the method of the secondary refinement batch, kept out of every test.
SECONDARY_METHOD = "cut-hide-refine"

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


# The closure rule of S-plus of a result file (tools/closure_rule.py): every analysis reads the files of one rule.
CLOSURE_RULES = closure_rule.RULES
EARLIER_RULE = closure_rule.EARLIER_RULE
closure_of = closure_rule.closure_of
keep_closure = closure_rule.keep
describe_closure = closure_rule.describe


def machine_of(path):
    """The machine field of the run stamp in the header of a result file, or "?" without a stamp."""
    with io.open(path, encoding="utf-8-sig", newline="") as f:
        for line in f:
            if not line.startswith("#"):
                break
            match = re.match(r"^# run \d{8}-\d{6}(?:-\d{3})?_([^_]+)_", line)
            if match:
                return match.group(1)
    return "?"


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


def positive_minimum_gap(gap):
    """True for a gap "[a,b]" with a > 0 (preregistration section 22.3). One definition, in
    sc3_commands, because every table tool of the main comparison has to leave those files out."""
    return sc3_commands.positive_minimum_gap(gap)


# Preregistration section 22.2: an SC4 instance in which no sequence reaches r needs no mask. Its single row carries
# this status and no method; it is counted apart and never enters a test, not even as a tie.
NO_MASK_STATUS = "no sequence reaches r"
# Section 22.2: the occurrence-count rule of Abul, Bonchi and Giannotti is judged in H5a and descriptive otherwise.
SEQUENCE_ONLY = ("hha-emb",)
SIZE_TESTS = {"all": lambda n: True, "1": lambda n: n == 1, "5": lambda n: n == 5, "several": lambda n: n >= 2}


def pattern_count(sensitive_set):
    """The number of patterns in a sensitive set written "[[...], [...], ...]"."""
    return sensitive_set.count("[") - 1


def keep_sizes(rows, sizes):
    """(the rows whose sensitive set has a size that sizes admits, the count of the others)."""
    test = SIZE_TESTS[sizes]
    kept = [r for r in rows if test(pattern_count(r["sensitive_set"]))]
    return kept, len(rows) - len(kept)


def keep_gaps(rows, gaps):
    """(the rows of the gaps asked for: g- = 0 for "zero", g- > 0 for "positive", the count of the others)."""
    positive = gaps == "positive"
    kept = [r for r in rows if positive_minimum_gap(r["gap"]) == positive]
    return kept, len(rows) - len(kept)


def set_aside_no_mask(rows):
    """(the rows that may enter the analysis, the rows of the instances that need no mask)."""
    apart = [r for r in rows if r.get("status", "") == NO_MASK_STATUS]
    return [r for r in rows if r.get("status", "") != NO_MASK_STATUS], apart


def group_configurations(sc2_files, profile="pool", positive_gap=False):
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
            # The configurations of g- = 0 and of g- > 0 have separate locked tables; one call groups one kind.
            if positive_minimum_gap(r["gap"]) != positive_gap:
                continue
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
        profile = "repeated" if path in (LOCKED.GROUP_TABLE_REPEATED, LOCKED.GAP_GROUP_TABLE_REPEATED) else "pool"
        if path in (LOCKED.GAP_GROUP_TABLE, LOCKED.GAP_GROUP_TABLE_REPEATED):
            f.write("# Groups of preregistration section 22.3 (gaps with g- > 0), from SC2 --profile=%s. "
                    "Locked by commit.\n" % profile)
        else:
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
        if not seeds and len(many) > 1:
            # A deterministic method has one row per instance. Two rows that differ come from two files of the same
            # cell written under different code or rules; their median would be a number nobody measured.
            differing = [c for c in ("lost", "lost_rel", "sup_dist", "mask_count", "hf", "nonsensitive_before", "status")
                         if len({r.get(c, "") for r in many}) > 1]
            if differing:
                raise ValueError("%d rows of the deterministic method %s on %s %s sigma %s set %s differ in %s: two files "
                                 "of one cell under different code or closure rules; read one rule (--closure)"
                                 % (len(many), key[5], key[0], key[2], key[3], key[4], ", ".join(differing)))
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

def run_h2(rows, write, groups=None, hypothesis="H2", locked=None, label="tier (b) baselines"):
    locked = LOCKED.BASELINE_FAMILY if locked is None else locked
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
        write("  %s is tested on the %d rows of the main group only." % (hypothesis, len(kept)))
        rows = kept
    else:
        write("")
        write("  PROBE: no group filter (--skip-groups); every row enters %s." % hypothesis)

    if not present(rows, LOCKED.OUR_METHOD):
        keys = sorted({r["method_key"] for r in rows})
        write("")
        write("  NO '%s' in the files. %s compares two sides, and without this side there is"
              % (LOCKED.OUR_METHOD, hypothesis))
        write("  nothing to test. The files hold: %s" % ", ".join(keys))
        write("  Note: 'cut-hide-uniform', 'cut-hide' and 'cut-hide-rw' are CONTROL variants and")
        write("  cannot stand in for '%s' in %s (preregistration, section 12)." % (LOCKED.OUR_METHOD, hypothesis))
        write("  %s NOT TESTED: the exit code is 1." % hypothesis)
        return False

    family = [b for b in locked if present(rows, b)]
    absent = [b for b in locked if b not in family]
    if absent:
        write("")
        write("  Absent from the files: %s" % ", ".join(absent))
        write("  The Holm family has %d members left. Section 2 requires this to be amended in the"
              % len(family))
        write("  preregistration BEFORE running, not after seeing that a file lacks them.")
    if not family:
        write("")
        write("  No %s in the files. %s cannot be tested." % (label[:-1] if label.endswith("s") else label,
                                                              hypothesis))
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
            write("NO CONCLUSION ON %s: the share excluded exceeds the locked threshold." % hypothesis)
            return False

    if len(results) >= 2:
        adjusted = holm(results)
        write("")
        write("Holm over the family of %d %s, alpha = %.2f"
              % (len(results), label, LOCKED.ALPHA))
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
              % (len(family), len(locked)))
        write("correction and the verdicts differ from those of section 2. The exit code is 1.")
        return False
    return True


def present(rows, key):
    return any(r["method_key"] == key for r in rows)


# ---------------------------------------------------------------- pairs (|R|, Lost), section 5

def run_dominance(rows, groups, write, locked=None, label="tier (b) baseline", hypothesis="H2"):
    """The report that section 5 asks for beside H2, DESCRIPTIVE: no test.

    Section 5 sends the instances where the baseline uses more masks to a report "as pairs (|R|,
    Lost)", because comparing Lost at two values of |R| compares two different things. For each
    tier (b) baseline this counts, on the same rows as H2 (main group, report-only datasets left
    out), how often CUT-HIDE loses fewer, as many or more patterns, in each regime of |R|, and on
    how many instances it is no worse on both |R| and Lost.
    """
    write("")
    write("=" * 78)
    locked = LOCKED.BASELINE_FAMILY if locked is None else locked
    write("Pairs (|R|, Lost) of section 5, DESCRIPTIVE: %s against each %s"
          % (LOCKED.OUR_METHOD, label))
    write("=" * 78)
    rows = [r for r in rows if r["dataset"] not in LOCKED.REPORT_ONLY]
    if groups is not None:
        rows = [r for r in rows
                if (groups.get((r["dataset"], r["gap"], r["sigma"])) or {}).get("group") == "main"]
        write("  rows: main group, without %s (the rows of %s): %d"
              % (", ".join(LOCKED.REPORT_ONLY), hypothesis, len(rows)))
    else:
        write("  PROBE: no group filter (--skip-groups): %d rows" % len(rows))
    if not present(rows, LOCKED.OUR_METHOD):
        write("  no '%s' row: nothing to pair" % LOCKED.OUR_METHOD)
        return
    write("  Lost compared as numbers of patterns; '<' means %s loses fewer." % LOCKED.OUR_METHOD)
    write("  %-10s %9s | %-20s | %-20s | %9s %13s"
          % ("baseline", "instances", "same |R|: < = >", "baseline more: < = >", "ours more",
             "no worse both"))
    for name in locked:
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


# ---------------------------------------------------------------- H4a, section 19.1

def run_h4a(rows, groups, write, hypothesis="H4a"):
    """r_c = |R|(c) / |R|(CUT-HIDE) per instance for each competitor that stops at the threshold, on
    the rows of H2 (main group, report-only datasets left out), judged on the median with the
    thresholds of H3 (section 19.1)."""
    write("")
    write("=" * 78)
    write("%s  |R|(competitor) / |R|(%s): median >= %.2f more masks, <= %.2f the same"
          % (hypothesis, LOCKED.OUR_METHOD, LOCKED.R_ABOVE_1, LOCKED.R_NEAR_1))
    write("=" * 78)
    rows = [r for r in rows if r["dataset"] not in LOCKED.REPORT_ONLY]
    if groups is not None:
        rows = [r for r in rows
                if (groups.get((r["dataset"], r["gap"], r["sigma"])) or {}).get("group") == "main"]
    write("  %-12s %9s %9s %9s %9s   %s" % ("competitor", "instances", "median", "max", "fewer", "verdict"))
    for name in LOCKED.STOP_FAMILY:
        if not present(rows, name):
            write("  %-12s absent from the files" % name)
            continue
        bins = pair_up(rows, name, "lost_rel")
        entries = bins["tested"] + bins["baseline more"] + bins["ours more"]
        ratios = [e[3]["mask_count"] / e[2]["mask_count"] for e in entries if e[2]["mask_count"] > 0]
        if not ratios:
            write("  %-12s no instance to compare" % name)
            continue
        med = float(np.median(ratios))
        verdict = ("more masks" if med >= LOCKED.R_ABOVE_1
                   else "the same number of masks" if med <= LOCKED.R_NEAR_1 else "no conclusion")
        write("  %-12s %9d %9.4f %9.4f %9d   %s" % (name, len(ratios), med, max(ratios), len(bins["ours more"]),
                                                  verdict))
        if bins["excluded"]:
            write("  %-12s %d excluded instances not counted (status or HF > 0)" % ("", len(bins["excluded"])))


# ---------------------------------------------------------------- H3

def run_h3(rows, groups, write, descriptive=False, hypothesis="H3", judged=None, judge_degenerate=True):
    judged = LOCKED.BASELINE_FAMILY if judged is None else judged
    write("")
    write("=" * 78)
    if descriptive:
        write("r = |R|/|R|* per method and group, DESCRIPTIVE (section 11, point 7): no verdict")
    else:
        write("%s  r = |R|/|R|* above 1 on the main group (r >= %.2f), near 1 on the degenerate"
              " group (r <= %.2f)" % (hypothesis, LOCKED.R_ABOVE_1, LOCKED.R_NEAR_1))
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
        # keying on it would split one baseline into three rows.
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
            if name not in judged:
                # Section 2 states H3 for the tier (b) baselines; other methods are shown only.
                verdict = "(not a tier (b) baseline: shown, not judged)"
            elif group != "main" and not judge_degenerate:
                verdict = "(%s judges the main group only: shown, not judged)" % hypothesis
            elif group == "main":
                verdict = ("above 1" if med >= LOCKED.R_ABOVE_1
                           else "near 1" if med <= LOCKED.R_NEAR_1 else "no conclusion")
            else:
                verdict = ("near 1" if med <= LOCKED.R_NEAR_1
                           else "above 1" if med >= LOCKED.R_ABOVE_1 else "no conclusion")
            write("   %-34s %5d %8.4f %8.4f   %s" % (name[:34], len(v), med, max(v), verdict))

    family_main = [v for k, v in by_group["main"].items() if k in judged]
    if not descriptive and judge_degenerate and not by_group["degenerate"]:
        write("")
        write("  The second half of %s (r near 1 on the degenerate group) is NOT TESTED: the SC3" % hypothesis)
        write("  files hold no row of the degenerate group.")
    if not descriptive and family_main and all(
            float(np.median(v)) <= LOCKED.R_NEAR_1 for v in family_main):
        write("")
        write("  Every tier (b) baseline gives r near 1 on the main group: %s is NOT rejected. Section 2" % hypothesis)
        write("  anticipated this case -- it is a negative result to be reported,")
        write("  not an implementation error.")


# ---------------------------------------------------------------- equivalence, section 4

def run_equivalence(rows, write, groups=None, locked=None, label="tier (b) baseline", hypothesis="H2"):
    locked = LOCKED.BASELINE_FAMILY if locked is None else locked
    write("")
    write("=" * 78)
    write("Equivalence (section 4): Hodges-Lehmann with a %.0f%% confidence interval, against the"
          " locked margins" % (100 * (1 - LOCKED.ALPHA)))
    write("=" * 78)
    rows = [r for r in rows if r["dataset"] not in LOCKED.REPORT_ONLY]
    # The companion of H2, so on the rows of H2: the main group (section 10, point 4). Without this
    # filter the pairs of the degenerate group, run for H3 only, would enter the intervals.
    if groups is not None:
        rows = [r for r in rows
                if (groups.get((r["dataset"], r["gap"], r["sigma"])) or {}).get("group") == "main"]
        write("  rows: main group, without %s (the rows of %s): %d"
              % (", ".join(LOCKED.REPORT_ONLY), hypothesis, len(rows)))
    else:
        write("  PROBE: no group filter (--skip-groups): %d rows" % len(rows))
    family = [b for b in locked if present(rows, b)]
    for quantity, margin in LOCKED.EQUIVALENCE_MARGIN.items():
        write("")
        write("-- %s, equivalence margin +/- %.2f" % (quantity, margin))
        if not family:
            write("   no %s" % label)
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

def run_reaches_optimum(rows, write):
    """Every one-pattern row of the method under test must mask exactly |R|*; False when one does not.

    For distinct items |R|* is the closed form of the flow theorem, computed apart from the program the method runs,
    so the agreement is evidence. For repeated items |R|* comes from the exact program, the same family the method
    runs; its own check against exhaustive search is SC1GAP. The count of rows compared is reported, and a scope with
    no row to compare says so instead of passing.
    """
    write("")
    write("=" * 78)
    write("The method under test masks exactly |R|* on every one-pattern instance")
    write("=" * 78)
    ok = True
    for scope, reference in (("distinct", "the closed form of the flow theorem, computed apart from the method"),
                             ("repeated", "the exact program; checked against exhaustive search by SC1GAP")):
        compared = [r for r in rows if r["method_key"] == LOCKED.OUR_METHOD and not r["status"]
                    and pattern_count(r["sensitive_set"]) == 1 and scope_of_set(r["sensitive_set"]) == scope
                    and not np.isnan(r["min_masks"])]
        if not compared:
            write("  %-8s scope: NOT CHECKED, no one-pattern row of %s" % (scope, LOCKED.OUR_METHOD))
            continue
        off = [r for r in compared if r["mask_count"] != r["min_masks"]]
        write("  %-8s scope: %d rows compared, %d mask another count than |R|* (reference: %s)"
              % (scope, len(compared), len(off), reference))
        for r in off[:5]:
            write("    %s %s sigma %s set %s: %d masks, |R|* %d"
                  % (r["dataset"], r["gap"], r["sigma"], r["sensitive_set"], r["mask_count"], r["min_masks"]))
        ok = ok and not off
    if not ok:
        write("  A row above masks another count than |R|*: the exit code is 1.")
    return ok


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
    parser.add_argument("--machine", default="",
                        help="read only the SC3 files whose run stamp names this machine; required "
                             "when the files come from more than one machine")
    parser.add_argument("--closure", default="necessary", choices=CLOSURE_RULES,
                        help="read only the SC3 files written under this closure rule of S-plus (default necessary, "
                             "the corrected rule; subsequence reads the files written before 2026-11-08)")
    parser.add_argument("--scenario", default="SC3", choices=("SC3", "SC4"),
                        help="SC3: the comparison under HIDE-DB (default); SC4: the same comparison under HIDE-SEQ at a "
                             "per-sequence limit r, read as H5a and H5b (preregistration section 22.2)")
    parser.add_argument("--gaps", default="zero", choices=("zero", "positive"),
                        help="zero: the gaps of the main comparison, g- = 0 (default); positive: the gaps with g- > 0 and "
                             "their own group tables, read as H6 (preregistration section 22.3)")
    parser.add_argument("--sizes", default="all", choices=sorted(SIZE_TESTS),
                        help="the sizes of sensitive set kept: all (default), 1, 5, or several (two or more)")
    parser.add_argument("--write-gap-groups", action="store_true",
                        help="write the two group tables of the gaps with g- > 0 (preregistration section 22.3) "
                             "from their SC2 files and stop; done once and committed")
    parser.add_argument("--write-groups", action="store_true",
                        help="write config/configuration_groups.tsv from the SC2 pool files and "
                             "stop; done once, from the official runs, and committed")
    args = parser.parse_args(argv)

    out_lines = []

    def write(s=""):
        out_lines.append(s)

    if args.write_gap_groups:
        files = collect_files(args.sc2, "SC2_")
        for profile, path in (("pool", LOCKED.GAP_GROUP_TABLE), ("repeated", LOCKED.GAP_GROUP_TABLE_REPEATED)):
            groups = group_configurations(files, profile=profile, positive_gap=True)
            if not groups:
                print("no SC2 file of the %s profile with g- > 0 in %s" % (profile, args.sc2), file=sys.stderr)
                return 1
            write_group_table(groups, path)
            print("wrote %s (%d configurations)" % (path, len(groups)))
        return 0

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

    default_mode = (args.scenario, args.gaps, args.sizes) == ("SC3", "zero", "all")
    if args.scenario == "SC4":
        names, h3_name = {"distinct": "H5b", "repeated": "H5b-R"}, "H5a"
    elif args.gaps == "positive":
        names, h3_name = {"distinct": "H6", "repeated": "H6-R"}, "H6 (its H3 part)"
    else:
        names, h3_name = {"distinct": "H2", "repeated": "H2-R"}, "H3"
    sc3_files = collect_files(args.sc3, args.scenario + "_")
    if not sc3_files:
        print("no %s file found in %s" % (args.scenario, args.sc3), file=sys.stderr)
        return 1
    # One machine per analysis. Rows of two machines are the same cells measured twice; merged as
    # reruns they would be averaged into one row without a word (the OT rows depend on the machine).
    by_machine = {}
    for path in sc3_files:
        by_machine.setdefault(machine_of(path), []).append(path)
    if args.machine:
        sc3_files = by_machine.get(args.machine, [])
        if not sc3_files:
            print("no SC3 file of machine %s in %s; machines found: %s" % (
                args.machine, args.sc3, ", ".join("%s (%d)" % (m, len(v)) for m, v in sorted(by_machine.items()))),
                file=sys.stderr)
            return 1
    elif len(by_machine) > 1:
        print("the SC3 files come from %d machines: %s. Name one with --machine." % (
            len(by_machine), ", ".join("%s (%d files)" % (m, len(v)) for m, v in sorted(by_machine.items()))),
            file=sys.stderr)
        return 1

    sc3_files, found = keep_closure(sc3_files, args.closure)
    if not sc3_files:
        print("no SC3 file under closure rule %s%s; rules found: %s" % (
            args.closure, " of machine " + args.machine if args.machine else "",
            ", ".join("%s (%d)" % (r, n) for r, n in sorted(found.items()))), file=sys.stderr)
        return 1

    write("Statistical analysis of %s, %s and %s under config/preregistration.md"
          % (names["distinct"], names["repeated"], h3_name))
    if not default_mode:
        write("Scenario %s, gaps %s (g- %s), sizes of the sensitive set: %s"
              % (args.scenario, args.gaps, "= 0" if args.gaps == "zero" else "> 0", args.sizes))
    write("")
    write(describe_closure("SC3", args.closure, sc3_files, found))
    write("")
    write("SC3 files read:")
    # Check the columns file by file, not on the first row of the whole batch: a directory can mix
    # old and new files, and dropping the whole batch for one old file throws the good ones away.
    raw_rows = []
    skipped = []
    secondary = []
    fronts = []
    stop_files = []
    stop_raw = []
    for path in sc3_files:
        comments, rows = read_csv(path)
        header = next((c for c in comments if c.startswith("# run ")), "# run ?")
        # Section 17: the refinement batch (key cut-hide-refine) is a secondary analysis added after
        # the |S| > 1 results were seen. Its rows never enter H2, H2-R, H3 or the row counts here.
        options = next((c for c in comments if c.startswith("# options ")), "")
        if re.search(r"[{ ]methods=%s[,}]" % re.escape(SECONDARY_METHOD), options):
            secondary.append(os.path.basename(path))
            write("  %-58s %d rows  SECONDARY (section 17), not read here"
                  % (os.path.basename(path), len(rows)))
            continue
        # Section 19.1: the batch of the competitors that stop at the threshold enters H4 only, never
        # the rows or the counts of H2, H2-R and H3.
        if re.search(r"[{ ]methods=%s[,}]" % re.escape(LOCKED.STOP_METHODS), options):
            if [c for c in REQUIRED_COLUMNS if not rows or c not in rows[0]]:
                skipped.append((os.path.basename(path), ["columns"]))
                write("  %-58s %d rows  SKIPPED (missing columns)" % (os.path.basename(path), len(rows)))
                continue
            stop_files.append(os.path.basename(path))
            stop_raw.extend(rows)
            write("  %-58s %d rows  STOP-AT-THRESHOLD BATCH (section 19.1), read for H4 only"
                  % (os.path.basename(path), len(rows)))
            continue
        # The Pareto-front batch runs with --methods=none: its SC3 file holds no method row by design
        # (the front itself is in the SC3PARETO file of the same run, read by tools/pareto_table.py).
        if re.search(r"[{ ]methods=none[,}]", options):
            fronts.append(os.path.basename(path))
            write("  %-58s %d rows  PARETO-FRONT BATCH (no method row), not read here"
                  % (os.path.basename(path), len(rows)))
            continue
        missing =[c for c in REQUIRED_COLUMNS if not rows or c not in rows[0]]
        if missing:
            skipped.append((os.path.basename(path), missing))
            write("  %-58s %d rows  SKIPPED (missing %s)"
                  % (os.path.basename(path), len(rows), ", ".join(missing)))
            continue
        write("  %-58s %d rows  %s" % (os.path.basename(path), len(rows), header[6:]))
        raw_rows.extend(rows)

    if secondary:
        write("")
        write("  %d files of the refinement batch (section 17) are left to the secondary analysis."
              % len(secondary))
    if fronts:
        write("")
        write("  %d files of the Pareto-front batch hold no method row; the fronts are read by "
              "tools/pareto_table.py." % len(fronts))
    if stop_files:
        write("")
        write("  %d files of the stop-at-threshold batch (section 19.1) are read for H4 only."
              % len(stop_files))
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

    raw_rows, no_mask = set_aside_no_mask(raw_rows)
    rows = merge_reruns(raw_rows, write)
    write("")
    write("After merging the reruns by their median: %d rows (from %d)"
          % (len(rows), len(raw_rows)))
    if no_mask:
        counted = defaultdict(int)
        for r in no_mask:
            if SIZE_TESTS[args.sizes](pattern_count(r["sensitive_set"])) and \
                    positive_minimum_gap(r["gap"]) == (args.gaps == "positive"):
                counted[scope_of_set(r["sensitive_set"])] += 1
        write("Instances that need no mask (%s), counted apart and never as a tie (section 22.2): "
              "distinct items %d, repeated items %d" % (NO_MASK_STATUS, counted["distinct"], counted["repeated"]))
    rows, other_gaps = keep_gaps(rows, args.gaps)
    rows, other_sizes = keep_sizes(rows, args.sizes)
    if not default_mode or other_gaps or other_sizes:
        write("Rows left out: %d of the other gaps (g- %s), %d of the other sizes; %d rows kept"
              % (other_gaps, "> 0" if args.gaps == "zero" else "= 0", other_sizes, len(rows)))

    # Section 11: split by the items of the sensitive patterns, never by file name or option.
    by_scope = {"distinct": [], "repeated": []}
    for r in rows:
        by_scope[scope_of_set(r["sensitive_set"])].append(r)
    write("")
    write("Rows by scope (section 11): distinct items %d, repeated items %d"
          % (len(by_scope["distinct"]), len(by_scope["repeated"])))

    sc2_files = collect_files(args.sc2, "SC2_")
    # The groups are checked on the SC2 files of the machine analysed, so the check says that this machine
    # reproduces the locked tables (for g- > 0 that is what the batch g4-sc2gap is for, section 22.3).
    if args.machine and sc2_files:
        own = [f for f in sc2_files if machine_of(f) == args.machine]
        write("")
        if own:
            write("SC2 files of machine %s used for the groups: %d (of %d found)" % (args.machine, len(own), len(sc2_files)))
            sc2_files = own
        else:
            write("No SC2 file of machine %s: the groups come from the SC2 files of %s" % (
                args.machine, ", ".join(sorted({machine_of(f) for f in sc2_files}))))
    all_valid = True
    reaches_ok = True
    for scope, profile, name in (("distinct", "pool", names["distinct"]), ("repeated", "repeated", names["repeated"])):
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
            groups = group_configurations(sc2_files, profile=profile, positive_gap=(args.gaps == "positive"))
            if not args.skip_groups:
                table = None
                if args.gaps == "positive":
                    table = read_group_table(LOCKED.GAP_GROUP_TABLE if profile == "pool"
                                             else LOCKED.GAP_GROUP_TABLE_REPEATED)
                groups_valid = check_groups(groups, write, table=table, profile=profile)
        else:
            write("")
            write("  No SC2 file: no groups, so H3 cannot split the main and degenerate groups.")
            # Without groups H2 has no main group to be tested on; only a probe may go on.
            groups_valid = args.skip_groups

        h2_valid = run_h2(scope_rows, write, None if args.skip_groups else groups, name)
        run_dominance(scope_rows, None if args.skip_groups else groups, write, hypothesis=name)
        # H3 is preregistered for the main scope; section 11, point 7 makes the ratio of the
        # repeated scope descriptive. H6 repeats H3 as it is. H5a states a verdict on the main group for each
        # baseline and for hha-emb, in either scope, since |R|*_SEQ is exact in both (section 22.2).
        if args.scenario == "SC4":
            run_h3(scope_rows, groups, write, hypothesis=h3_name,
                   judged=LOCKED.BASELINE_FAMILY + SEQUENCE_ONLY, judge_degenerate=False)
        else:
            run_h3(scope_rows, groups, write, descriptive=(scope == "repeated"), hypothesis=h3_name)
        run_equivalence(scope_rows, write, None if args.skip_groups else groups, hypothesis=name)
        all_valid = all_valid and h2_valid and groups_valid

    if not default_mode:
        reaches_ok = run_reaches_optimum(rows, write)

    if stop_raw and not default_mode:
        write("")
        write("  The stop-at-threshold batch (section 19.1) belongs to H4 under HIDE-DB at g- = 0; not read here.")
    if stop_raw and default_mode:
        stop_rows = merge_reruns(stop_raw, write)
        for scope, profile, h4b, h4a in (("distinct", "pool", "H4b", "H4a"), ("repeated", "repeated", "H4b-R", "H4a-R")):
            scope_rows = [r for r in by_scope[scope] if r["method_key"] == LOCKED.OUR_METHOD]
            scope_rows += [r for r in stop_rows if scope_of_set(r["sensitive_set"]) == scope]
            groups = {}
            if sc2_files and not args.skip_groups:
                groups = group_configurations(sc2_files, profile=profile)
            use_groups = None if args.skip_groups else groups
            write("")
            write("#" * 78)
            write("Section 19.1, %s-item scope: competitors that stop at the threshold (%s, %s)"
                  % (scope, h4a, h4b))
            write("#" * 78)
            run_h4a(scope_rows, use_groups, write, h4a)
            h4_valid = run_h2(scope_rows, write, use_groups, h4b, LOCKED.STOP_FAMILY,
                              "competitors that stop at the threshold")
            run_dominance(scope_rows, use_groups, write, LOCKED.STOP_FAMILY, "competitor that stops at the threshold")
            run_equivalence(scope_rows, write, use_groups, LOCKED.STOP_FAMILY, "competitor that stops at the threshold")
            all_valid = all_valid and h4_valid

    write("")
    write("=" * 78)
    write("Reminder: exit code 0 does NOT mean that H2 is supported. It only means that the")
    write("analysis ran, and the conclusion is in the tables above.")

    emit(out_lines, args.out)
    return 0 if all_valid and reaches_ok else 1


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
