"""Pin the statistics of preregistered_analysis.py to values computed by hand.

Why. A mistake here does not crash the program; it makes a number in the paper wrong, and nobody
sees it. Every test below carries an answer worked out by hand in its comments, so it can be checked
without trusting scipy or the script itself.

Run: python tools/preregistered_analysis_test.py
Exit code 1 when a test fails.
"""
import os
import re
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import preregistered_analysis as pa

# The method under test of H2 and H2-R (preregistration section 12); fixtures use it by name.
OURS = pa.LOCKED.OUR_METHOD


def close(a, b, tolerance=1e-9, note=""):
    if not (abs(a - b) <= tolerance):
        raise AssertionError("expected %r, got %r  %s" % (b, a, note))


def test_pratt_ranks():
    """d = [0, 0, 1, -2, 3].

    |d| = [0, 0, 1, 2, 3]; average ranks = [1.5, 1.5, 3, 4, 5]. Zero differences ARE ranked (that
    is Pratt) but enter neither side: W+ = 3 + 5 = 8, W- = 4.
    """
    w_plus, w_minus = pa.pratt_ranks([0, 0, 1, -2, 3])
    close(w_plus, 8.0, note="W+")
    close(w_minus, 4.0, note="W-")

    # Dropping the zero differences before ranking (the original Wilcoxon) gives ranks [1, 2, 3]
    # and W+ = 1 + 3 = 4, W- = 2. Quite different. This test keeps anyone from quietly switching.
    w_original_plus, w_original_minus = pa.pratt_ranks([1, -2, 3])
    close(w_original_plus, 4.0)
    close(w_original_minus, 2.0)


def test_rank_biserial():
    """(W+ - W-)/(W+ + W-) = (8 - 4)/12 = 1/3."""
    close(pa.rank_biserial([0, 0, 1, -2, 3]), 1.0 / 3.0)
    # All differences positive gives +1; all negative gives -1.
    close(pa.rank_biserial([1, 2, 3]), 1.0)
    close(pa.rank_biserial([-1, -2, -3]), -1.0)
    # Symmetric gives 0: |d| = [1, 1] has ranks [1.5, 1.5], W+ = W- = 1.5.
    close(pa.rank_biserial([1, -1]), 0.0)
    # All zeros is undefined: it must be nan, not 0.
    assert np.isnan(pa.rank_biserial([0, 0, 0]))


def test_wilcoxon_shifted():
    """The test must run on d - delta_L, not on d.

    d = [0.02] * 6: every difference EQUALS the threshold, so there is no evidence at all for
    "greater than the threshold".
    """
    _, p, effect, n, zeros = pa.wilcoxon_one_sided([0.02] * 6, 0.02)
    close(p, 1.0, note="every difference equal to the shift must give p = 1")
    close(effect, 0.0)
    close(n, 6)
    close(zeros, 6)

    # The same data with shift 0 makes every difference positive: p must be the smallest possible
    # with n = 6, i.e. 1/2^6 = 0.015625.
    _, p0, effect0, _, _ = pa.wilcoxon_one_sided([0.02] * 6, 0.0)
    close(p0, 1.0 / 64.0, tolerance=1e-12)
    close(effect0, 1.0)


def test_wilcoxon_is_pratt_not_wilcox():
    """Pin the Pratt convention by ENUMERATING the null distribution BY HAND, not by trusting scipy.

    d = [0, 0, 0, 0, 1, -2, 3, 4, 5, 6, 7, -8]. Ranking |d|: the four zeros tie at the average rank
    2.5, and the eight non-zero observations take ranks 5 to 12 in turn. Pratt keeps those ranks.
    Under the null hypothesis every non-zero observation takes the sign + or - independently with
    probability 1/2, so W+ ranges over 2^8 = 256 equally likely outcomes. Counting by hand gives 29
    outcomes with W+ >= 50, i.e. p = 29/256 = 0.11328125.

    This test matters more in practice than in theory: in scipy 1.18, method="exact" gives 0.211914
    under Pratt, which is WRONG -- it uses the standard signed-rank distribution over all 12
    observations, ignoring that the ranks of the zeros may not take a sign. Only method="auto" is
    right. Whoever "tidies up" by switching to method="exact" skews every p value in the paper
    without any visible sign.
    """
    import itertools
    from scipy import stats as _st

    d = [0, 0, 0, 0, 1, -2, 3, 4, 5, 6, 7, -8]
    ranks = _st.rankdata(np.abs(d))
    nonzero = ranks[np.array(d) != 0]
    positive = np.array([x for x in d if x != 0]) > 0
    observed = float(nonzero[positive].sum())
    close(observed, 50.0, note="observed W+")

    count = sum(1 for signs in itertools.product([0, 1], repeat=len(nonzero))
                if sum(k for k, s in zip(nonzero, signs) if s) >= observed)
    p_by_hand = count / 2 ** len(nonzero)
    close(p_by_hand, 29 / 256.0, note="enumerated by hand")

    _, p, _, n, zeros = pa.wilcoxon_one_sided(d, 0.0)
    close(n, 12)
    close(zeros, 4)
    close(p, p_by_hand, tolerance=1e-12, note="must match the enumeration by hand")

    p_wrong = _st.wilcoxon(d, zero_method="pratt", alternative="greater", method="exact").pvalue
    assert abs(p_wrong - p_by_hand) > 1e-3, \
        "scipy method='exact' used to be wrong under Pratt; if it is right now, update the comment"


def test_wilcoxon_direction():
    """One-sided "greater": data leaning negative gives a large p, leaning positive a small p."""
    _, p_positive, _, _, _ = pa.wilcoxon_one_sided([1, 2, 3, 4, 5], 0.0)
    _, p_negative, _, _, _ = pa.wilcoxon_one_sided([-1, -2, -3, -4, -5], 0.0)
    assert p_positive < 0.05, p_positive
    assert p_negative > 0.95, p_negative


def test_holm():
    """p = [0.01, 0.02, 0.03, 0.04], m = 4.

    Step 1: 4 x 0.01 = 0.04. Step 2: 3 x 0.02 = 0.06. Step 3: 2 x 0.03 = 0.06.
    Step 4: 1 x 0.04 = 0.04, but it must stay monotone, so it is raised to 0.06.
    """
    out = pa.holm({"a": 0.01, "b": 0.02, "c": 0.03, "d": 0.04})
    close(out["a"], 0.04)
    close(out["b"], 0.06)
    close(out["c"], 0.06)
    close(out["d"], 0.06)

    # Capped at 1.0.
    out = pa.holm({"a": 0.5, "b": 0.6})
    close(out["a"], 1.0)
    close(out["b"], 1.0)

    # With a single member Holm changes nothing.
    close(pa.holm({"a": 0.03})["a"], 0.03)


def test_hodges_lehmann():
    """d = [1, 2, 3, 4, 5]: the Walsh averages are symmetric around 3, so the estimate must be 3."""
    hl, low, high = pa.hodges_lehmann([1, 2, 3, 4, 5])
    close(hl, 3.0)
    assert low <= hl <= high, (low, hl, high)

    # Shifting all the data by a constant shifts the estimate by exactly that much.
    hl2, _, _ = pa.hodges_lehmann([x + 10 for x in [1, 2, 3, 4, 5]])
    close(hl2, 13.0)

    # More spread data gives a wider interval.
    _, low1, high1 = pa.hodges_lehmann([1, 1, 1, 1, 1, 1, 1, 1])
    _, low2, high2 = pa.hodges_lehmann([-9, 1, 1, 1, 1, 1, 1, 11])
    assert (high2 - low2) > (high1 - low1), ((low1, high1), (low2, high2))


def test_merge_by_median():
    """Merging reruns must take the MEDIAN, not the mean.

    lost = [1, 1, 10]: median 1, mean 4. With the mean, one stray run drags the whole value along.
    """
    rows = []
    for seed, lost in ((2026, 1), (2027, 1), (2028, 10)):
        rows.append({"dataset": "d", "shape": "s", "gap": "[0,2]", "sigma": "20",
                     "sensitive_set": "[[0, 1]]", "method_key": "rr", "method": "RR-random",
                     "method_seed": str(seed), "hf": "0", "lost": str(lost),
                     "lost_rel": str(lost / 100.0), "ghost": "0", "dist_r": "0",
                     "sup_dist": "0", "mask_count": "5", "min_masks": "5", "mask_ratio": "1",
                     "lower_bound": "0", "achieved_ratio": "", "frequent_before": "100",
                     "nonsensitive_before": "100", "frequent_after": "99"})
    out = pa.merge_reruns(rows, lambda *a: None)
    assert len(out) == 1, out
    close(out[0]["lost"], 1.0, note="median of [1, 1, 10]")
    close(out[0]["runs"], 3)
    close(out[0]["seeds"], 3)

    # A deterministic method: one row, no seed, and the median of one number is that number.
    single = dict(rows[0], method_key="hha", method="HHA-pathcounts", method_seed="", lost="7")
    out = pa.merge_reruns([single], lambda *a: None)
    close(out[0]["lost"], 7.0)
    close(out[0]["seeds"], 0)


def _row(method_key, mask_count, lost_rel, hf=0.0, sensitive_set="[[0, 1]]", status=""):
    return {"dataset": "d", "gap": "[0,2]", "sigma": 20, "sensitive_set": sensitive_set,
            "method_key": method_key, "method": method_key, "status": status,
            "hf": hf, "lost_rel": lost_rel, "lost": lost_rel * 100, "sup_dist": 0.0,
            "mask_count": mask_count, "mask_ratio": 1.0, "seeds": 0, "runs": 1}


def test_pair_up():
    """The three bins must split by |R|, and HF > 0 must exclude the whole pair."""
    rows = [
        # same |R| -> tested
        _row(OURS, 10, 0.10, sensitive_set="A"), _row("hha", 10, 0.20, sensitive_set="A"),
        # the baseline uses more -> reported apart
        _row(OURS, 10, 0.10, sensitive_set="B"), _row("hha", 14, 0.20, sensitive_set="B"),
        # CUT-HIDE uses more -> a bin of its own, never dropped silently
        _row(OURS, 14, 0.10, sensitive_set="C"), _row("hha", 10, 0.20, sensitive_set="C"),
        # the baseline has not finished hiding -> the whole pair is excluded
        _row(OURS, 10, 0.10, sensitive_set="D"),
        _row("hha", 10, 0.20, hf=0.5, sensitive_set="D"),
    ]
    bins = pa.pair_up(rows, "hha", "lost_rel")
    close(len(bins["tested"]), 1)
    close(len(bins["baseline more"]), 1)
    close(len(bins["ours more"]), 1)
    close(len(bins["excluded"]), 1)
    # The difference is taken as baseline minus ours.
    close(bins["tested"][0][1], 0.10)
    assert "HF" in bins["excluded"][0][1], bins["excluded"][0][1]

    # A status code (OT, OOM) excludes the pair too.
    rows = [_row(OURS, 10, 0.10, sensitive_set="E"),
            _row("hha", 10, 0.20, sensitive_set="E", status="OT")]
    bins = pa.pair_up(rows, "hha", "lost_rel")
    close(len(bins["excluded"]), 1)
    assert "OT" in bins["excluded"][0][1], bins["excluded"][0][1]


def test_groups_at_the_thresholds():
    """The thresholds of section 3 are >= 0.20 and <= 0.05: a value on the boundary belongs to the
    outer group, not to the intermediate one."""
    import tempfile
    import io as _io

    def write_sc2(path, triples):
        with _io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write("# run x\n")
            f.write("# profile pool\n")
            f.write("dataset,gap,sigma,pattern,sup_NO,sup_seq,sup_NO_no_gap,min_masks,"
                    "mask_share,rho_rep,beta,min_masks_exact\n")
            for name, rho, beta in triples:
                f.write("datasets/%s.txt,\"[0,3]\",100,\"[1]\",1,1,1,1,0,%s,%s,true\n"
                        % (name, rho, beta))

    directory = tempfile.mkdtemp()
    path = os.path.join(directory, "SC2_test.csv")
    write_sc2(path, [("upper_edge", 0.20, 0.5), ("lower_edge", 0.05, 0.5),
                     ("middle", 0.10, 0.5)])
    groups = pa.group_configurations([path])
    assert groups[("upper_edge", "[0,3]", 100)]["group"] == "main"
    assert groups[("lower_edge", "[0,3]", 100)]["group"] == "degenerate"
    assert groups[("middle", "[0,3]", 100)]["group"] == "intermediate"


def test_groups_take_the_median():
    """Section 3 locks the MEDIAN, not the mean: on a skewed distribution the two differ."""
    import tempfile
    import io as _io

    directory = tempfile.mkdtemp()
    path = os.path.join(directory, "SC2_test.csv")
    with _io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("# run x\n")
        f.write("# profile pool\n")
        f.write("dataset,gap,sigma,pattern,sup_NO,sup_seq,sup_NO_no_gap,min_masks,"
                "mask_share,rho_rep,beta,min_masks_exact\n")
        # rho_rep = [0.01, 0.02, 0.90]: median 0.02 (degenerate), mean 0.31 (intermediate).
        for k, rho in enumerate((0.01, 0.02, 0.90)):
            f.write("datasets/x.txt,\"[0,3]\",100,\"[%d, 9]\",1,1,1,1,0,%s,0.5,true\n" % (k, rho))
    groups = pa.group_configurations([path])
    info = groups[("x", "[0,3]", 100)]
    close(info["rho_rep"], 0.02)
    assert info["group"] == "degenerate", info


def test_constants_match_the_preregistration():
    """The constants of the class LOCKED must match config/preregistration.md.

    Read the locked file itself and look for every number. Whoever changes one side and forgets the
    other breaks this test.
    """
    import io as _io
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    path = os.path.join(root, "config", "preregistration.md")
    if not os.path.exists(path):
        print("  skipped: config/preregistration.md not found")
        return
    text = _io.open(path, encoding="utf-8").read()
    for name, needle in (("DELTA_L", "0,02"), ("RHO_MAIN", "0,20"), ("RHO_DEGENERATE", "0,05"),
                         ("R_ABOVE_1", "1,10"), ("R_NEAR_1", "1,05")):
        assert needle in text, "%s (%s) not found in the locked file" % (name, needle)
    assert "**10%**" in text or "10%" in text
    # The locked file is not written in English: the word joining the two seeds is matched by a
    # pattern, so that the needle does not depend on the language of the file.
    assert re.search(r"\b2026\s+\S+\s+2035\b", text), "no seeds 2026 to 2035 in the locked file"
    for name in ("HHA", "DBSH", "SBSH-r1", "SBSH-rmax"):
        assert name in text, "the Holm family lacks %s in the locked file" % name
    # Section 12: the method under test must be the one the preregistration names, and the one
    # config/protocol.yaml names for H2-R; one fact, two files, checked here.
    section_12 = text.split("## 12.", 1)
    assert len(section_12) == 2, "the preregistration has no section 12"
    assert "`%s`" % pa.LOCKED.OUR_METHOD in section_12[1], \
        "section 12 does not name %s" % pa.LOCKED.OUR_METHOD
    protocol = _io.open(os.path.join(root, "config", "protocol.yaml"), encoding="utf-8").read()
    declared = re.findall(r'^\s*method_H2R:\s*"([^"]+)"', protocol, re.M)
    assert declared == [pa.LOCKED.OUR_METHOD], \
        "config/protocol.yaml names %s for H2-R, tools/preregistered_analysis.py %s" \
        % (declared, pa.LOCKED.OUR_METHOD)


def _write_sc2(path, rows, profile):
    """An SC2 file: rows of (dataset, rho_rep, beta, distinct items), profile line if given."""
    import io as _io
    with _io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("# run x\n")
        if profile:
            f.write("# profile %s\n" % profile)
        f.write("dataset,gap,sigma,pattern,sup_NO,sup_seq,sup_NO_no_gap,min_masks,"
                "mask_share,rho_rep,beta,min_masks_exact\n")
        for k, (name, rho, beta, distinct) in enumerate(rows):
            # A distinct-item pattern [k, 9], or the repeated-item pattern [k, k].
            pattern = "[%d, 9]" % k if distinct else "[%d, %d]" % (k, k)
            f.write("datasets/%s.txt,\"[0,3]\",100,\"%s\",1,1,1,1,0,%s,%s,true\n"
                    % (name, pattern, rho, beta))


def test_groups_read_only_pool_files():
    """Section 10: only SC2 files of the pool profile group; a sampled file is ignored."""
    import tempfile
    directory = tempfile.mkdtemp()
    sampled = os.path.join(directory, "SC2_sample.csv")
    pool = os.path.join(directory, "SC2_pool.csv")
    _write_sc2(sampled, [("x", 0.90, 0.5, False), ("x", 0.90, 0.5, True)], None)
    _write_sc2(pool, [("x", 0.01, 0.5, True), ("x", 0.02, 0.5, True), ("x", 0.03, 0.5, True)],
               "pool")
    groups = pa.group_configurations([sampled, pool])
    info = groups[("x", "[0,3]", 100)]
    close(info["rho_rep"], 0.02)
    close(info["pool"], 3)
    assert info["group"] == "degenerate", info
    assert pa.group_configurations([sampled]) == {}, "a sampled file alone must give no group"


def test_groups_refuse_repeated_items_in_pool():
    """A pool file with a repeated-item pattern is not a pool file: stop, do not average it in."""
    import tempfile
    path = os.path.join(tempfile.mkdtemp(), "SC2_pool.csv")
    _write_sc2(path, [("x", 0.30, 0.5, True), ("x", 0.90, 0.5, False)], "pool")
    try:
        pa.group_configurations([path])
    except ValueError:
        return
    raise AssertionError("a repeated-item row in a pool file was accepted")


def test_groups_of_the_repeated_pool():
    """profile="repeated" groups the repeated-item pool and refuses a distinct-item row there."""
    import tempfile
    directory = tempfile.mkdtemp()
    repeated = os.path.join(directory, "SC2_repeated.csv")
    _write_sc2(repeated, [("x", 0.30, 0.5, False), ("x", 0.40, 0.5, False),
                          ("x", 0.10, 0.5, False)], "repeated")
    groups = pa.group_configurations([repeated], profile="repeated")
    close(groups[("x", "[0,3]", 100)]["rho_rep"], 0.30)
    assert groups[("x", "[0,3]", 100)]["group"] == "main"
    assert pa.group_configurations([repeated]) == {}, "a repeated file must stay out of the pool"
    bad = os.path.join(directory, "SC2_repeated_bad.csv")
    _write_sc2(bad, [("x", 0.30, 0.5, False), ("x", 0.90, 0.5, True)], "repeated")
    try:
        pa.group_configurations([bad], profile="repeated")
    except ValueError:
        return
    raise AssertionError("a distinct-item row in a repeated file was accepted")


def test_group_table_match_and_difference():
    """The locked table is compared configuration by configuration, both ways."""
    import tempfile
    directory = tempfile.mkdtemp()
    sc2 = os.path.join(directory, "SC2_pool.csv")
    _write_sc2(sc2, [("x", 0.30, 0.5, True), ("y", 0.01, 0.01, True)], "pool")
    groups = pa.group_configurations([sc2])
    table_file = os.path.join(directory, "groups.tsv")
    pa.write_group_table(groups, table_file)
    table = pa.read_group_table(table_file)
    assert pa.check_groups(groups, lambda s="": None, table), "an unchanged table must match"
    table[("x", "[0,3]", 100)]["group"] = "intermediate"
    assert not pa.check_groups(groups, lambda s="": None, table), "a changed group must fail"
    del table[("x", "[0,3]", 100)]
    assert not pa.check_groups(groups, lambda s="": None, table), "a missing row must fail"
    assert pa.read_group_table(os.path.join(directory, "none.tsv")) is None


def test_h2_on_main_group_only():
    """Section 3 and the design: H2 is tested on the main group, the rest is not pooled in."""
    rows = []
    for name in ("a", "b"):
        for method, lost in ((OURS, 0.10), ("hha", 0.20)):
            r = _row(method, 10, lost, sensitive_set="T")
            r["dataset"] = name
            rows.append(r)
    groups = {("a", "[0,2]", 20): {"group": "main"}, ("b", "[0,2]", 20): {"group": "degenerate"}}
    out = []
    pa.run_h2(rows, lambda s="": out.append(s), groups)
    assert any("tested on the 2 rows of the main group only" in s for s in out), out[:8]



def test_scope_of_set():
    """Section 11, point 4: the scope comes from the items of the patterns themselves."""
    assert pa.scope_of_set("[[4, 1, 4, 1]]") == "repeated"
    assert pa.scope_of_set("[[1, 5, 8]]") == "distinct"
    assert pa.scope_of_set("[[1, 2], [3, 4]]") == "distinct"
    assert pa.scope_of_set("[[2, 2], [3, 1, 3]]") == "repeated"
    for bad in ("[[1, 2], [3, 3]]", "[]", "not a set"):
        try:
            pa.scope_of_set(bad)
        except ValueError:
            continue
        raise AssertionError("accepted %r" % bad)


def test_h2_leaves_out_protein_sdb1():
    """Section 11, point 5: protein_sdb1 is reported, never tested."""
    rows = []
    for name in ("protein_sdb4", "protein_sdb1"):
        for method, lost in ((OURS, 0.10), ("hha", 0.20)):
            r = _row(method, 10, lost, sensitive_set="T")
            r["dataset"] = name
            rows.append(r)
    out = []
    pa.run_h2(rows, lambda s="": out.append(s), None)
    assert any("2 rows of protein_sdb1 are reported only" in s for s in out), out[:8]
    assert any("into Wilcoxon (same |R|)                1 instances" in s for s in out), out


def _write_sc3(path, rows, options=None):
    """An SC3 file with the columns the analysis requires; rows = (set, method, lost_rel)."""
    import io as _io
    columns = ("dataset,shape,gap,sigma,sensitive_set,stratum,method_key,method,method_seed,hf,"
               "lost,lost_rel,ghost,dist_r,sup_dist,mask_count,min_masks,mask_ratio,"
               "certified_pairs,total_pairs,lower_bound,achieved_ratio,frequent_before,"
               "nonsensitive_before,frequent_after")
    with _io.open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("# run x\n" + ("# options {%s}\n" % options if options else "") + columns + "\n")
        for sensitive_set, method, lost in rows:
            f.write('datasets/x.txt,"N=1,len=1","[0,3]",100,"%s",p10,%s,%s,,0,1,%s,0,0.1,0.1,'
                    '10,10,1.0,1,1,10,1.0,50,40,49\n' % (sensitive_set, method, method, lost))


def test_main_splits_two_scopes():
    """main() runs H2 on the distinct-item rows and H2-R on the repeated-item rows, apart."""
    import tempfile
    directory = tempfile.mkdtemp()
    _write_sc3(os.path.join(directory, "SC3_a.csv"),
               [("[[1, 2]]", OURS, 0.10), ("[[1, 2]]", "hha", 0.30),
                ("[[3, 3]]", OURS, 0.05), ("[[3, 3]]", "hha", 0.25),
                ("[[4, 5, 4]]", OURS, 0.05), ("[[4, 5, 4]]", "hha", 0.35)])
    report = os.path.join(directory, "report.txt")
    pa.main(["--sc3", directory, "--sc2", directory, "--skip-groups", "--out", report])
    import io as _io
    text = _io.open(report, encoding="utf-8").read()
    assert "distinct items 2, repeated items 4" in text, text[:600]
    assert "distinct-item sensitive patterns, 2 rows, tested as H2" in text
    assert "repeated-item sensitive patterns, 4 rows, tested as H2-R" in text
    assert "H2-R  median" in text
    # Without the bypass each scope reads its own SC2 profile and names its own section.
    _write_sc2(os.path.join(directory, "SC2_pool.csv"), [("x", 0.30, 0.5, True)], "pool")
    # A different group per profile, so reading the wrong profile shows.
    _write_sc2(os.path.join(directory, "SC2_rep.csv"), [("x", 0.01, 0.5, False)], "repeated")
    saved = (pa.LOCKED.GROUP_TABLE, pa.LOCKED.GROUP_TABLE_REPEATED)
    pa.LOCKED.GROUP_TABLE = os.path.join(directory, "missing_1.tsv")
    pa.LOCKED.GROUP_TABLE_REPEATED = os.path.join(directory, "missing_2.tsv")
    try:
        code = pa.main(["--sc3", directory, "--sc2", directory, "--out", report])
    finally:
        pa.LOCKED.GROUP_TABLE, pa.LOCKED.GROUP_TABLE_REPEATED = saved
    text = _io.open(report, encoding="utf-8").read()
    assert code == 1, "no locked table yet: the analysis must not pass"
    assert "section 10: medians over the distinct-item candidates" in text
    assert "section 11: medians over the repeated-item candidates" in text
    assert "missing_2.tsv does not exist yet" in text
    before, _, after = text.partition("section 11: medians over the repeated-item candidates")
    assert "-> main" in before.split("section 10: medians")[1], "the distinct scope reads pool"
    assert "-> degenerate" in after.split("\n\n")[0], "the repeated scope reads repeated"


def test_refinement_batch_is_not_read():
    """Section 17: a file of the refinement batch (methods=cut-hide-refine) is listed and left out.
    By hand: the main file has 2 distinct-item and 4 repeated-item rows; a refinement file with 3 more
    rows must leave the counts at 2 and 4, and name the file as secondary. A file whose methods list
    only mentions the key inside a longer list (the H2 set never does, but a wider set might) is read."""
    import tempfile
    import io as _io
    directory = tempfile.mkdtemp()
    _write_sc3(os.path.join(directory, "SC3_a.csv"),
               [("[[1, 2]]", OURS, 0.10), ("[[1, 2]]", "hha", 0.30),
                ("[[3, 3]]", OURS, 0.05), ("[[3, 3]]", "hha", 0.25),
                ("[[4, 5, 4]]", OURS, 0.05), ("[[4, 5, 4]]", "hha", 0.35)])
    _write_sc3(os.path.join(directory, "SC3_b.csv"),
               [("[[1, 2]]", "cut-hide-refine", 0.00), ("[[3, 3]]", "cut-hide-refine", 0.00),
                ("[[4, 5, 4]]", "cut-hide-refine", 0.00)],
               options="dataset=datasets/x.txt, sensitiveSize=5, methods=cut-hide-refine")
    report = os.path.join(directory, "report.txt")
    pa.main(["--sc3", directory, "--sc2", directory, "--skip-groups", "--out", report])
    text = _io.open(report, encoding="utf-8").read()
    assert "distinct items 2, repeated items 4" in text, text[:800]
    assert "SC3_b.csv" in text and "SECONDARY (section 17)" in text, text[:800]
    assert "1 files of the refinement batch" in text
    _write_sc3(os.path.join(directory, "SC3_c.csv"), [("[[6, 7]]", "cut-hide-refine-x", 0.00)],
               options="dataset=datasets/x.txt, methods=cut-hide-refine-x")
    pa.main(["--sc3", directory, "--sc2", directory, "--skip-groups", "--out", report])
    text = _io.open(report, encoding="utf-8").read()
    assert "distinct items 3, repeated items 4" in text, "another key is not the refinement batch"


def test_h2_not_tested_fails_the_run():
    """Without the method under test, without any tier (b) baseline, or with a Holm family smaller
    than the locked four, H2 is not the locked test and the exit code must say so. An end-to-end
    SC3 trial (2026-09-26) found the first case exiting with 0."""
    import tempfile
    import io as _io
    family = pa.LOCKED.BASELINE_FAMILY
    cases = {
        "complete": [OURS] + list(family),
        "no method under test": ["cut-hide", "cut-hide-rw"] + list(family),
        "no baseline": [OURS, "cut-hide"],
        "three of the four baselines": [OURS] + list(family[:-1]),
    }
    expected = {"complete": 0, "no method under test": 1, "no baseline": 1,
                "three of the four baselines": 1}
    for case, methods in cases.items():
        directory = tempfile.mkdtemp()
        rows = []
        for k, sensitive_set in enumerate(("[[1, 2]]", "[[1, 3]]", "[[2, 3]]", "[[1, 4]]", "[[2, 4]]")):
            for method in methods:
                lost = 0.05 if method == OURS else 0.10 + 0.01 * k
                rows.append((sensitive_set, method, lost))
        _write_sc3(os.path.join(directory, "SC3_a.csv"), rows)
        report = os.path.join(directory, "report.txt")
        code = pa.main(["--sc3", directory, "--sc2", directory, "--skip-groups", "--out", report])
        text = _io.open(report, encoding="utf-8").read()
        assert code == expected[case], "%s: exit code %d, expected %d" % (case, code, expected[case])
        if case == "no method under test":
            assert "H2 NOT TESTED" in text and "Wilcoxon" not in text, case
        if case == "three of the four baselines":
            assert "NOT THE LOCKED TEST" in text, case


def test_write_both_group_tables():
    """--write-groups writes the pool table and the repeated table (section 11, point 2)."""
    import tempfile
    directory = tempfile.mkdtemp()
    _write_sc2(os.path.join(directory, "SC2_pool.csv"), [("x", 0.30, 0.5, True)], "pool")
    _write_sc2(os.path.join(directory, "SC2_rep.csv"), [("x", 0.01, 0.5, False)], "repeated")
    saved = (pa.LOCKED.GROUP_TABLE, pa.LOCKED.GROUP_TABLE_REPEATED)
    pa.LOCKED.GROUP_TABLE = os.path.join(directory, "configuration_groups.tsv")
    pa.LOCKED.GROUP_TABLE_REPEATED = os.path.join(directory, "configuration_groups_repeated.tsv")
    try:
        assert pa.main(["--sc2", directory, "--write-groups"]) == 0
        main_table = pa.read_group_table(pa.LOCKED.GROUP_TABLE)
        repeated_table = pa.read_group_table(pa.LOCKED.GROUP_TABLE_REPEATED)
        assert main_table[("x", "[0,3]", 100)]["group"] == "main", main_table
        assert repeated_table[("x", "[0,3]", 100)]["group"] == "degenerate", repeated_table
        import io as _io
        assert "section 11" in _io.open(pa.LOCKED.GROUP_TABLE_REPEATED, encoding="utf-8").readline()
    finally:
        pa.LOCKED.GROUP_TABLE, pa.LOCKED.GROUP_TABLE_REPEATED = saved


def test_groups_count_a_rerun_once():
    """Two pool files of one configuration (a rerun) count each pattern once."""
    import tempfile
    directory = tempfile.mkdtemp()
    rows = [("x", 0.30, 0.5, True)]
    for name in ("SC2_a.csv", "SC2_b.csv"):
        _write_sc2(os.path.join(directory, name), rows, "pool")
    groups = pa.group_configurations([os.path.join(directory, "SC2_a.csv"),
                                      os.path.join(directory, "SC2_b.csv")])
    close(groups[("x", "[0,3]", 100)]["pool"], 1)
    _write_sc2(os.path.join(directory, "SC2_c.csv"), [("x", 0.40, 0.5, True)], "pool")
    try:
        pa.group_configurations([os.path.join(directory, "SC2_a.csv"),
                                 os.path.join(directory, "SC2_c.csv")])
    except ValueError:
        return
    raise AssertionError("one pattern with two different profiles was accepted")

def test_h3_keys_on_method_key():
    """SBSH-rmax names itself SBSH-r2 or SBSH-r3 by the length of the pattern; H3 must still see one
    baseline. By hand: r = 1.0 and 1.2 for sbsh-rmax -> one row, n = 2, median 1.1 -> "above 1";
    hha r = 1.0, 1.0 -> "near 1"; cut-hide is shown without a verdict; no degenerate row -> the
    second half of H3 is reported as not tested."""
    rows = []
    for k, (display, ratio) in enumerate((("SBSH-r2", 1.0), ("SBSH-r3", 1.2))):
        r = _row("sbsh-rmax", 10, 0.1, sensitive_set="S%d" % k)
        r["method"], r["mask_ratio"] = display, ratio
        rows.append(r)
        h = _row("hha", 10, 0.1, sensitive_set="S%d" % k)
        rows.append(h)
        c = _row(OURS, 10, 0.1, sensitive_set="S%d" % k)
        c["method"] = "CUT-HIDE adaptive weight"
        rows.append(c)
    groups = {("d", "[0,2]", 20): {"group": "main"}}
    out = []
    pa.run_h3(rows, groups, lambda s="": out.append(s))
    lines = [s for s in out if s.strip().startswith(("sbsh", "SBSH", "hha", OURS))]
    assert not any("SBSH-r" in s for s in lines), lines
    sb = [s for s in lines if s.strip().startswith("sbsh-rmax")]
    assert len(sb) == 1 and " 2 " in sb[0] and "1.1000" in sb[0] and "above 1" in sb[0], sb
    assert any(s.strip().startswith("hha") and "near 1" in s for s in lines), lines
    assert any(s.strip().startswith(OURS) and "not judged" in s for s in lines), lines
    assert any("second half of H3" in s and "NOT TESTED" in s for s in out), out


def test_dominance_counts():
    """Section 5 pairs. By hand, against hha (lost = lost_rel * 100 in _row):
    A same |R|, ours 10 < 20; B same |R|, 30 > 20; C baseline more masks, 10 = 10;
    D baseline more masks, 30 > 10 (a trade-off); E ours uses more masks; F excluded (HF > 0).
    -> instances 5 (F not counted), same |R| 1 0 1, baseline more 0 1 1, ours more 1,
    no worse on both 2/5."""
    pairs = (("A", 10, 0.10, 10, 0.20), ("B", 10, 0.30, 10, 0.20), ("C", 10, 0.10, 12, 0.10),
             ("D", 10, 0.30, 12, 0.10), ("E", 12, 0.10, 10, 0.20))
    rows = []
    for name, mo, lo, mb, lb in pairs:
        rows += [_row(OURS, mo, lo, sensitive_set=name), _row("hha", mb, lb, sensitive_set=name)]
    rows += [_row(OURS, 10, 0.10, sensitive_set="F"), _row("hha", 10, 0.20, hf=0.5, sensitive_set="F")]
    out = []
    pa.run_dominance(rows, None, lambda s="": out.append(s))
    line = [s for s in out if s.strip().startswith("hha")]
    assert len(line) == 1, out
    numbers = [int(x) for x in re.findall(r"\d+", line[0].replace("/", " "))]
    assert numbers == [5, 1, 0, 1, 0, 1, 1, 1, 2, 5], (numbers, line[0])
    assert any("1 excluded instances not counted" in s for s in out), out
    # The same rows of H2: a degenerate-group row and a report-only row are left out.
    extra = [_row(OURS, 10, 0.10, sensitive_set="G"), _row("hha", 10, 0.20, sensitive_set="G")]
    for r in extra:
        r["dataset"] = "protein_sdb1"
    degenerate = [_row(OURS, 10, 0.10, sensitive_set="H"), _row("hha", 10, 0.20, sensitive_set="H")]
    for r in degenerate:
        r["dataset"] = "e"
    extra += degenerate
    groups = {("d", "[0,2]", 20): {"group": "main"}, ("protein_sdb1", "[0,2]", 20): {"group": "main"},
              ("e", "[0,2]", 20): {"group": "degenerate"}}
    out = []
    pa.run_dominance(rows + extra, groups, lambda s="": out.append(s))
    line = [s for s in out if s.strip().startswith("hha")]
    assert re.findall(r"\d+", line[0])[0] == "5", line


def test_equivalence_on_main_group_only():
    """Section 4 is the companion of H2, so it uses the rows of H2: a degenerate-group pair must not
    enter the interval. By hand: two main pairs with differences 0.10 and 0.10 -> n = 2; adding two
    degenerate pairs must leave n = 2."""
    rows = []
    for name, group_rows in (("a", ("S1", "S2")), ("b", ("S3", "S4"))):
        for s_ in group_rows:
            for method, lost in ((OURS, 0.10), ("hha", 0.20)):
                r = _row(method, 10, lost, sensitive_set=s_)
                r["dataset"] = name
                rows.append(r)
    groups = {("a", "[0,2]", 20): {"group": "main"}, ("b", "[0,2]", 20): {"group": "degenerate"}}
    out = []
    pa.run_equivalence(rows, lambda s="": out.append(s), groups)
    line = [s for s in out if s.strip().startswith("hha")]
    assert line and re.findall(r"\d+", line[0])[0] == "2", (line, out)
    assert any("rows: main group" in s and ": 4" in s for s in out), out


TESTS = [v for k, v in sorted(globals().items()) if k.startswith("test_")]


def main():
    failed = 0
    for test in TESTS:
        name = test.__name__
        try:
            test()
            print("  OK   %s" % name)
        except Exception as error:            # noqa: BLE001 -- report all, do not stop at the first
            failed += 1
            print("  FAIL %s: %s" % (name, error))
    print("")
    print("%d tests, %d failed" % (len(TESTS), failed))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
