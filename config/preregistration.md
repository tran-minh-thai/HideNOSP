# Locked before running SC3

Rule 2.4.7: the practical-effect threshold and the tests must be locked **before** SC3 runs.
This file is that lock.

**Status: LOCKED, 2026-09-12.** The commit that recorded this file is the evidence of the date. From
here on, changing any number below must be a separate commit stating the reason, and a change made
after seeing SC3 results must be declared in the paper.

The data behind the values are the SC1 and SC2 files of `results-invariant/`, run independently of SC3.

**Provenance.** The values below were drafted by the proposer and approved by the author. The
proposer **had seen** the direction of one exploratory SC3 run on substitute synthetic data
(`results-probe/`), so every threshold is anchored to the scale of the data (the size of `F_σ`, the
gaps in the distribution of ρ_rep), not to the distance observed between methods. The three numbers
that could be most affected if this introduced a bias are δ_L and the two equivalence margins of §3;
they are marked separately.

---

## 1. Scope

**The main scope of the paper is μ(P) = 1**, that is, sensitive patterns without repeated items. In
that scope, P1, P2, P6(b) and the exact solution all hold.

Patterns with repeated items (μ(P) ≥ 2) are reported in a separate section, using bound P3 and the
exact solver on small instances, and the hardness is stated as an open question.

The limitations section must state the following numbers, measured at ℓ_max = 4, medians:

| Dataset | ρ_rep μ(P) = 1 | ρ_rep μ(P) ≥ 2 | Difference |
|---|---|---|---|
| dna_mt745584 | 0.845 | 0.878 | 0.033 |
| msnbc | 0.218 | 0.364 | 0.146 |
| fifa | 0.008 | 0.952 | 0.944 |

Reading the table: on **all three** datasets the repeated-item class has the higher repetition rate.
What justifies keeping the scope at μ(P) = 1 is **not** that the class without repeated items repeats
more, but that on the strongest main-group dataset (dna) the two classes are **nearly equal**: 0.845
against 0.878, a difference of 0.033. So there, narrowing the scope to μ(P) = 1 loses almost nothing.
On MSNBC the cost is larger (difference 0.146), and on FIFA the whole repetition rate sits in the
repeated-item class (0.952 against 0.008) — but FIFA is the degenerate control, so this does not change
the conclusion.

> **Correction.** The first version of this file said "on the genome dataset MT745584 it is the other
> way round (0.806 against 0.773)", i.e. the class without repeated items repeats more. That number was
> measured at ℓ_max = 3 and as a **mean**; measured again at the locked ℓ_max = 4 and as a **median**, the
> direction reverses. The conclusion to keep μ(P) = 1 is unchanged, but the justification changes from
> "higher" to "nearly equal", and the limitations section must follow the table above, not the old sentence.

Node weights when μ(P) ≥ 2: every node of a position gets the weight of that position. This is an
**implementation choice**, not a consequence of any proposition, because P1 and P2 do not apply there.

---

## 2. Hypotheses

### H2

Over the instances (dataset, σ, sensitive set), the median of the difference
`Lost_rel(baseline) − Lost_rel(CUT-HIDE)` exceeds the practical threshold δ_L.

**Quantity.** `Lost_rel = Lost / |F_σ(D) \ 𝒮⁺|`; the denominator is the number of non-sensitive
frequent patterns before sanitisation, i.e. the SC3 column `frequent_before` minus the size of the
closure 𝒮⁺.

| Item | Locked value |
|---|---|
| **δ_L** | **0.02** (two percentage points) |
| Unit of replication | one instance = the triple (dataset, σ, sensitive set) |
| Test | Wilcoxon signed-rank, one-sided |
| Multiple-testing correction | Holm over the **4** tier (b) baselines |
| Effect size | rank-biserial = (W⁺ − W⁻)/(W⁺ + W⁻) |
| Significance level | α = 0.05 |

**Why a relative quantity.** The measured |F_σ| spans more than an order of magnitude, from 51 (dna
σ=2500) to 672 (msnbc σ=1000). A single absolute δ_L would be strict on small datasets and lax on
large ones, mixing two scales in one Wilcoxon test. The median absolute difference is reported
**alongside** in the same table.

**Why 0.02.** One percentage point on dna (|F| ≈ 51) is less than half a pattern, so any difference,
even of one pattern, would exceed the threshold and δ_L would have no effect. 0.02 corresponds to about
one pattern on dna and 6.5 patterns on msnbc σ=2000.

Σ|S| and |R|\* are **not** used as the denominator: Σ|S| is the denominator of DistR, |R|\* is the bound
on the number of masks.

### H3

The ratio `r = |R| / |R|*` of the tier (b) baselines is above 1 on the main group, and near 1 on the
degenerate group.

| Item | Locked value |
|---|---|
| "above 1" | r ≥ **1.10** |
| "near 1" | r ≤ **1.05** |
| region 1.05 < r < 1.10 | no conclusion |

**Anticipating a negative result.** In the exploratory run every method gave r = 1.000, i.e. no
baseline used extra masks. If that repeats on real data, H3 **cannot be rejected**, and it is a
negative result to be reported under rule 3.4.9, not an error.

### Baseline family for Holm

The family consists of **four** tier (b) baselines: HHA, DBSH, SBSH-r1, SBSH-rmax.

Tier (a) — occurrence-counting heuristics, HH, HR, RH, RR — is reported **descriptively**, not tested:
these exist to show that the problem does not solve itself (§11.3), not to test hypotheses.

If SBSH cannot be checked against Algorithms 2 and 3 of [R3], SBSH is dropped from the table, the family
has **two** members, and this line must be changed **before** running.

---

## 3. Data groups

The thresholds apply to a **configuration**, the triple (dataset, σ, [g⁻,g⁺]) — **not** to a dataset.
Reason: on msnbc σ=2000, changing the gap from [0,1] to [0,10] drops β from 0.432 to 0.043; β is a
property of the configuration, not of the dataset.

Summary statistic within a configuration: the **median** ρ_rep (and median β) over the sampled
sensitive patterns. Not the mean, because the distribution is skewed: msnbc [0,3] σ=2000 spans
0.106–0.575.

| Group | Condition |
|---|---|
| Main group | ρ_rep ≥ **0.20** |
| Intermediate group | **0.05 < ρ_rep < 0.20** |
| Degenerate control | ρ_rep ≤ **0.05** |
| Gap constraint effective | β ≥ **0.20** |
| Degenerate-constraint control | β ≤ **0.05** |

**The intermediate group is not used to test H2 or H3.** It only illustrates that ρ_rep and β are two
independent quantities: bible and leviathan have ρ_rep around 0.15 but β up to 0.50–0.61.

**Why 0.05 and 0.20.** The median ρ_rep of 25 configurations, measured at ℓ_max = 4 and sorted in
increasing order, shows two clear **sparse gaps**:

- from 0.010 (fifa) it jumps to 0.120 (bible) — a gap of width 0.110, with the threshold **0.05** inside;
- from 0.181 (msnbc σ=1000) it jumps to 0.265 (msnbc σ=2000) — a gap of width 0.084, with the threshold
  **0.20** inside.

The thresholds sit in sparse regions, so the grouping is insensitive to shifts of a few percent.

> **Note on the remeasurement.** These two thresholds were locked from measurements at ℓ_max = 3.
> After the protocol locked ℓ_max = 4, all of SC2 was rerun and both thresholds **still lie in the
> sparse gaps**, so they were not changed. This was a remeasurement to check, not a reselection after
> seeing results.

> **Superseded by section 10 (2026-09-25).** The summary statistic and the grouping table below are
> medians over the **sampled SC2 patterns**, including patterns with repeated items. From 2026-09-25
> the groups are computed over **patterns without repeated items** (section 10); the table below is kept
> as history and is no longer the locked table.

**Grouping derived from the measured data:**

Applied to **configurations** (25 configurations, ℓ_max = 4, medians):

| Group | Configurations | Members |
|---|---|---|
| Main | 12 | dna in all 6 configurations; msnbc at σ ∈ {2000, 5000} |
| Intermediate | 7 | **msnbc σ=1000** (all three gaps), bible, leviathan |
| Degenerate | 6 | sign, bms1, fifa |

**An important point that arises from applying the thresholds per configuration:** msnbc at σ = 1000
has a median ρ_rep of 0.172–0.181, i.e. **below** 0.20, so those three configurations belong to the
intermediate group, not to the main group. MSNBC is a main-group dataset only from σ = 2000 upward. If
the thresholds were applied per "dataset" instead of per "configuration", this detail would vanish —
which is exactly why the configuration is locked as the unit.

Moreover, every configuration with gap [0,10] has β ≤ 0.085 (dna σ=4000 has β = 0.044), i.e. it belongs
to the **degenerate-constraint control** even though ρ_rep is very high. These configurations show that
the gap constraint loses its effect when widened; they are not used as evidence for the contribution
about the constraint.

This grouping is locked here and is **not changed** after seeing SC3 results (rule 2.3.10). A dataset
added after the lock date must be grouped with exactly the two thresholds above, before SC3 runs on it.

---

## 4. "No difference" conclusions

To conclude that two methods do not differ, use the confidence interval of the difference together with
a pre-locked equivalence margin, not "p > 0.05".

| Quantity | Equivalence margin |
|---|---|
| Lost_rel | ± **0.02** (exactly δ_L) |
| SupDist | ± **0.01** |

The equivalence margin is set equal to the effect threshold so as not to create a wide region where a
result is neither different nor equivalent.

**The least grounded number in this file is ±0.01 for SupDist.** It is a convention, not yet anchored to
a measured spread. The valid way to replace it with a grounded number is to run **one** single method
on many sensitive sets and measure the spread of SupDist across sets — this does not reveal the direction
of any comparison between methods. A replacement must be a separate commit, before SC3.

---

## 5. Computation conventions

### Wilcoxon

| Item | Locked convention |
|---|---|
| Difference exactly 0 | **Pratt**: kept when ranking, not dropped |
| Tied ranks | average rank |
| Effect size | rank-biserial |

Lost is a small integer, so there will be many pairs with zero difference. The original Wilcoxon
discards them, losing the information "the two methods are equal" and inflating the effect size.

### Pairing for H2

Each method runs **until HF = 0**, with |R| of its own choosing. Two reporting modes are kept apart:

1. Instances where every method uses the **same** |R|: these go into the Wilcoxon test for H2.
2. Instances where a baseline uses **more**: reported separately as pairs (|R|, Lost), **not** tested.
   The conclusion there is stronger and needs no statistics.

Comparing Lost at two different |R| compares two different things; forcing a baseline to stop at |R|\*
leaves it not yet done hiding, so its Lost cannot be compared either.

### Excluding instances

| Situation | Handling |
|---|---|
| A method leaves HF > 0 | drop the pair from Wilcoxon, count and report separately |
| Time limit exceeded | drop the pair, record code OT, report the count |
| Out of memory | drop the pair, record code OOM, report the count |
| Share of excluded instances > **10%** | **no conclusion** on H2, and say why |

Assigning a penalty value to a case with HF > 0 would be fabricating data, so it is not done.

### Randomised methods

HH, HR, RH, RR and the genetic algorithm: run **10 times** with seeds **2026 to 2035**, and take the
**median** Lost and |R| as the value of that method on that instance.

This is a machine-invariant quantity, so the repetition table of §11.6 does not apply — that table is for
timing. The observed variation of RR is large (|R| = 31 and 151 on two sensitive sets of the same
configuration), so one run is not enough.

---

## 6. What does NOT need testing

- **HF.** Zero by design with CUT-HIDE (step 8 re-checks after every run). Stated once, not put in the
  comparison table (rule 2.4.2).
- **Ghost of masking.** Zero by P5(a). This is a consequence of a theorem, not measured as a finding
  (rule 1.2.2). Only the deletion branch of SC5 reports Ghost.
- **\|R\| of the exact methods.** Equal to \|R\|\* by P1. A difference here is an implementation error,
  not a result.

---

## 7. Fixed run order

1. SC1 must be green (0 mismatches) before any other scenario. **Done**, 1,500 instances, 0 mismatches.
2. SC2 runs before SC3 and decides the grouping. **Done**, 16 configurations; grouping in §3.
3. SC3, SC4, SC5, SC7, SC8: machine-invariant numbers.
4. SC6 runs last, on the declared measuring machine, with `--machine=<machine name>`.

Before the first SC3 run whose numbers go into the paper, three technical tasks recorded in
`config/protocol.yaml` must be done: attach `HIDENOSP_REVISION` to the run identifier, raise ℓ_max to 4,
and check that B_ε is non-empty in every configuration.

---

## 8. Handling a negative result

If the vital-node weights do not improve Lost (the risk anticipated in §12), report the negative result
meeting the three conditions of rule 3.4.9. The theoretical contribution (the problem, the index, P1–P7)
does not depend on the direction of this result.

Likewise for H3: see the anticipation in §2.

---

## 9. Addendum of 2026-09-25 — how sensitive sets are sampled (before any SC3 run for the paper)

**Reason for the addendum.** `protocol.yaml` row 1 locks five percentiles of sup_NO, 4 sets per
percentile, but the code before this date did **not** do that: it shuffled the candidate pool for each
set independently, so sets could repeat. Measured on real data
(`results-probe/sbsh_feasibility_and_sampling_20260925.md`): on the genome dataset, 20 sets per cell gave
only 1 to 14 distinct sets depending on the configuration, because the candidate pool had only 4 to 23
patterns. Counting repeated sets as separate units would inflate the sample size of the Wilcoxon test.

**Timing.** No SC3 run for the paper has taken place. The SC3 runs so far were all exploratory: on
synthetic data before the lock date, and three feasibility probes on 2026-09-25 (DBSH and SBSH on the
genome dataset and MSNBC), of which only the HF column was read.

**Locked rule**, now implemented in `hidenosp.exp.SensitiveSampling` (test: `SensitiveSamplingTest`):

1. The candidate pool is the patterns of F_σ of length 2 to 4 without repeated items (scope μ(P) = 1 of §1).
2. The pool is sorted by increasing sup_NO (ties by pattern order) and cut into five equal strata by
   rank, centred at p10, p30, p50, p70, p90.
3. Each stratum gives at most **4 distinct sets** of size |𝒮|, drawn from that stratum alone with seed
   2026 (one generator per stratum, derived from the seed). A stratum with fewer patterns than |𝒮| gives
   no set; a stratum with fewer than 4 combinations gives all of them.
4. **A set is never repeated to reach 20.** A cell (configuration, |𝒮|) that yields fewer than 20 sets
   still runs with the sets it has; that number is written at the top of the result file and must appear
   in the paper's table. A cell that yields no set does not exist and does not run; the paper states why
   (the size of the candidate pool).
5. The unit of replication of H2 is still (dataset, σ, gap, sensitive set), and now every unit is
   distinct. A set that could not be drawn is not an "excluded instance" of §5 and does not count toward
   the 10% threshold.

The command-line option changes accordingly: `--sensitiveSets` is removed, replaced by
`--setsPerStratum` (default 4).

**Unchanged:** δ_L, the four-member Holm family, the grouping thresholds, the equivalence margins.

---

## 10. Addendum of 2026-09-25 — data groups computed on patterns without repeated items (before any SC3 run for the paper)

**Decision of the author**, after measuring three new process event logs: the median ρ_rep and β of a
configuration are taken over **patterns without repeated items**, not over every pattern SC2 draws.

**Reason.** SC3 hides only patterns without repeated items (sections 1 and 9), so the groups must
describe exactly that kind of pattern. The median over all patterns mixes in patterns with repeated
items, and on some datasets the two kinds differ sharply: BPI 2013 at [0,1] σ=1000 has a median of 0.254
over all patterns but 0.021 over patterns without repeated items
(`results-probe/SC2_20260925-122652-529`). The old rule put that configuration into the main group
because of patterns that SC3 never hides. The new rule is less favourable to the paper, so it is not a
choice made after looking in order to gain.

**Locked rule**, implemented in `tools/thong_ke.py` and SC2 (`--profile=pool`):

1. For each configuration (dataset, σ, [g⁻,g⁺]), the median ρ_rep and median β are taken over the
   **whole** candidate pool of section 9 — every pattern of F_σ of length 2 to 4 without repeated items —
   not over a subsample. SC2 run with `--profile=pool` writes exactly that pool; an SC2 file of a
   subsample is not used for grouping.
2. The thresholds stay as in section 3: main group ρ_rep ≥ 0.20; degenerate ≤ 0.05; β ≥ 0.20 effective,
   ≤ 0.05 degenerate.
3. The group table of each configuration is generated **once** from the official SC2 runs on the
   measuring machine (`python tools/thong_ke.py --sc2 results-invariant --write-groups`) into
   `config/nhom_cau_hinh.tsv`, and committing that file is the lock. The analysis compares each
   configuration with the table; a mismatch stops it. The table replaces the numbers 12/7/6 of section 3.
4. **H2 is tested on the main group.** Section 3 already says "the intermediate group is not used to test
   H2 or H3", and the design (§8.4) keeps the degenerate group outside the group that demonstrates the
   contribution; the analysis code before this date pooled every configuration into H2, contrary to both.
   Now only main-group rows enter Wilcoxon; the other groups are reported descriptively. This carries out
   what was already locked; it is not a new decision.

**Timing.** No SC3 run for the paper has taken place; no official SC2 file in whole-pool mode exists yet.

**Unchanged:** δ_L, the four-member Holm family, the thresholds, the equivalence margins, the sampling of
section 9.

---

## 11. Addendum of 2026-09-26 — the scope of patterns with repeated items (before any SC3 run for the paper in this scope)

**Decision of the author** (2026-09-26): approve as a whole the proposal drafted on 2026-09-25 — open the
scope to patterns with repeated items; the method of the H2-R test is `cut-hide`; the protein file SDB4
enters the test, SDB1 is only reported; and the Pareto front of CUT-HIDE is reported as a descriptive
analysis (point 6 below).

**Provenance.** Before this section was written, its proposer **had seen** the exploratory runs on the
development machine: `results-probe/repeated_item_exactness_probe_20260925.txt` (the exact solution
against the cut solution on patterns with repeated items) and
`results-probe/repeated_item_scope_probe_20260925.md` (data groups over the pool of patterns with
repeated items; SC3 with repeated items on five configurations, seven methods). Direction seen: every
CUT-HIDE variant loses fewer patterns than the three baselines; `cut-hide` loses more patterns than the
λ-sweep variant in four of five configurations. This section therefore sets **no** directional hypothesis
between the variants of CUT-HIDE, and the method of H2-R is chosen by the objective of the problem
(\|R\| first), not by the numbers seen. **No** Pareto-front number existed when this section was written.

1. **Scope.** Sensitive patterns with repeated items (μ(P) ≥ 2), of length 2 to 4, in F_σ, with g⁻ = 0
   (every configuration of the protocol). There, Theorem P8 of the design gives the exact minimum number
   of masks, so \|R\|* and the ratio \|R\|/\|R\|* mean the same as in the main scope. The main scope
   (μ(P) = 1, section 1) is unchanged.
2. **Data groups.** As in section 10, over the pool of patterns with repeated items (SC2
   `--profile=repeated`), with the same thresholds as section 3. A separate group table
   `config/nhom_cau_hinh_lap_muc.tsv`, generated once from the official SC2 run on the measuring machine;
   committing it is the lock.
3. **Sampling sensitive sets.** As in section 9, over the pool of patterns with repeated items (SC3
   `--patternKind=repeated`).
4. **H2-R.** As H2 (section 2): on the main group **of the repeated-item scope**, the median difference
   Lost_rel(baseline) − Lost_rel(`cut-hide`) > δ_L — *changed in section 12 (2026-09-26): the method
   under test is `cut-hide-rw-ls`*; the same four-member Holm family (HHA, DBSH, SBSH-r1, SBSH-rmax), the
   same α = 0.05, δ_L = 0.02, one-sided Wilcoxon, rank-biserial. Tested **separately**, rows not pooled
   with H2. The rows of the two scopes are separated by **the items of the sensitive patterns themselves**
   in each SC3 row, not by file name or option.
5. **Protein files.** SDB4 (`protein_sdb4`) enters H2 and H2-R; SDB1 (`protein_sdb1`) is only reported
   descriptively, in no Wilcoxon test, because the two files share 64–75% of their sequences.
6. **Pareto front (descriptive, not tested).** For each one-pattern sensitive set, in both scopes, compute
   **exactly** the vertices of the convex Pareto front between \|R\| and the surrogate weight w(R) of
   CUT-HIDE (Proposition P10 of the design, the same weights as `cut-hide`), and for each vertex: \|R\|, w,
   Lost_rel, SupDist. Report: the number of vertices; the range of \|R\|/\|R\|* and of Lost_rel on the
   front; for each baseline of the Holm family, the share of sets where some vertex dominates it (no worse
   in both \|R\| and Lost_rel, strictly better in at least one). No vertex replaces `cut-hide` in H2 or H2-R.
7. **Extension of section 6.** \|R\| = \|R\|* of `cut-hide` in the repeated-item scope is a consequence of
   Theorem P8: reported as an implementation check, not tested. The ratio
   \|R\|(`cut-hide-sweep`)/\|R\|* — the cost of not having P8 — is reported descriptively per configuration.

**Unchanged:** all of sections 1–10.

---

## 12. Addendum of 2026-09-26 — the method under test of H2 and H2-R (before any SC3 run for the paper)

**Decision of the author** (2026-09-26): option (A) — the method under test of H2 (section 2) and H2-R
(section 11, point 4) changes from `cut-hide` to `cut-hide-rw-ls`, which adds adaptive surrogate weights
and a local search under the rescue condition (Lemma L9 of design v0.9).

**Reason.** Design v0.9 proves (Proposition P11, a reduction from MAX-CUT) that choosing a minimum mask set
that loses the fewest patterns is NP-hard even for one sensitive pattern of two distinct items, so no fixed
weight that adds up over positions can track the number of lost patterns exactly; the cause is the pairwise
interaction of masks. `cut-hide` uses exactly such a fixed weight. `cut-hide-rw-ls` uses the actual drops of
the previous solution to recompute the weights, then changes the mask set one or two sequences at a time,
with the number of lost patterns computed exactly.

**Definition of `cut-hide-rw-ls`** (code: `hidenosp.hide.AdaptiveCutHide` with `withLocalSearch`,
`hidenosp.hide.MaskLocalSearch`; SC3 key `cut-hide-rw-ls`):
1. *One sensitive pattern* (\|𝒮\| = 1; HIDE-DB; g⁻ = 0 in every configuration):
   - round 0 is exactly `cut-hide` (the weights of §9.2 step 4 on B_ε with ε = 0.5; the exact solution of P8);
   - rounds 1 to 5: w(i) = Σ c_Q over the Q ∈ F_σ \ 𝒮⁺ for which i is vital, with c_Q = 1/m_Q + s(1 − s)/τ_Q,
     s = 1/(1 + exp(−(d_Q − m_Q + ½)/τ_Q)), τ_Q = max(1, m_Q/4), m_Q = sup_NO(Q, D) − σ + 1, d_Q the exact
     drop of Q under the solution of the previous round; solve P8 again with the earlier number of masks.
     Keep the solution with the smallest Lost over the six rounds (ties: the earliest round);
   - local search from that solution (Algorithm LS of the design): one-sequence moves and allocation moves,
     at most 50 passes, the 30 cheapest receiving sequences; drop every move that Lemma L9 proves cannot
     lower Lost.
2. *Several sensitive patterns* (\|𝒮\| ∈ {5, 10, 20}): the greedy of §9.2 step 7, **unchanged** — identical
   to `cut-hide`, because the adaptive weights and the local search rest on the exact one-pattern solution
   of P8. The `method` column of the SC3 row states the branch that ran.

**Parameters locked, not tuned after seeing numbers:** six rounds (round 0 included); the formulas of c_Q and
τ_Q as above; 50 passes; 30 receiving sequences; ε = 0.5 as in `config/protocol.yaml`. The code keeps them in
`AdaptiveCutHide.DEFAULT_ITERATIONS`, `MaskLocalSearch.DEFAULT_PASSES`, `MaskLocalSearch.DEFAULT_RECEIVERS`.

**Proven properties, reported as implementation checks (section 6), not tested:** on every one-pattern
instance, \|R\|(`cut-hide-rw-ls`) = \|R\|* and Lost(`cut-hide-rw-ls`) ≤ Lost(`cut-hide`): round 0 is
`cut-hide`, and every later step is accepted only when Lost drops. Hence on **every** instance the difference
Lost_rel(baseline) − Lost_rel(method under test) is no smaller than the difference computed with `cut-hide`.
The change of method is a weak dominance by construction, not a choice between two peer methods based on
numbers.

**Provenance.** Before this section was written, its proposer **had seen** the exploratory runs on the
development machine on exactly the eight configurations × 20 sets of the Pareto-front probe (genome [0,3]
σ=2500 and protein SDB4 [0,3] σ=500 in both scopes, MSNBC [0,1] σ=2000 in both scopes, Sepsis [0,1] σ=300 and
BPI 2013 [0,1] σ=500 in the repeated-item scope), and 480 small instances cut from those datasets:
`results-probe/surrogate_candidates_probe_20260926.txt`, `lost_small_exact_probe_20260926.txt`,
`lost_local_search_probe_20260926.txt`, `pareto_exceptions_rw_probe_20260926.txt`. Direction seen: the new
method loses fewer patterns than `cut-hide` on part of the sets and more on none (as the construction
guarantees). The six rounds, the formula of τ_Q and the 30 receiving sequences were set when each probe was
written, before running it. **Two things were decided after seeing probe numbers**, declared here: (a) adding
allocation moves to the local search, after seeing the version with one-sequence moves only on small
instances; (b) the number of passes, 50 (the small-instance probe used 40, the full-data probe 50). The L9
filter does not change which move is chosen (proved in Lemma L9, checked in `MaskLocalSearchTest`).

**Additional reporting (descriptive, not tested):** `cut-hide` and `cut-hide-rw` run in the same SC3 run as
two ablation variants; a descriptive table of Lost_rel of the three variants and of the number of sets where
each step (adaptive weights, local search) lowers Lost. SC6 times both `cut-hide` and `cut-hide-rw-ls` on the
measuring machine.

**Timing.** No official SC3 run has taken place. Every number seen is exploratory, on the development machine.

**Unchanged:** δ_L, α, one-sided Wilcoxon with Pratt, the four-member Holm family, the sampling (section 9),
the data groups (sections 10 and 11), the equivalence margins, section 11 point 5; section 11 point 6 (the
Pareto front) is still computed with the weights of `cut-hide` and is still a descriptive analysis.

---

## 13. Editorial note of 2026-09-26 — file renames, no rule changed

The source code moved entirely to English, so three names mentioned in sections 10 and 11 change as follows:

| Name in sections 10 and 11 | Name from 2026-09-26 |
|---|---|
| `tools/thong_ke.py` | `tools/preregistered_analysis.py` |
| `config/nhom_cau_hinh.tsv` | `config/configuration_groups.tsv` |
| `config/nhom_cau_hinh_lap_muc.tsv` | `config/configuration_groups_repeated.tsv` |

The command that generates the group table in section 10 point 3 is now
`python tools/preregistered_analysis.py --sc2 results-invariant --write-groups`. The group labels written in
the two tables are also in English: for ρ_rep, `main`, `intermediate`, `degenerate`; for β, `effective`,
`between`, `degenerate`. When this note was written no group table had been generated, so no locked table
had to be rewritten.

**Unchanged:** all of sections 1–12; every threshold, test, pairing and sampling rule.

---

## 14. Addendum of 2026-10-02 — scope, methods, time limit and order of the official SC3 batch (before any SC3 run for the paper)

**Decision of the author** (2026-10-02), chosen among the options the proposer put forward:

1. **Scope of the run: the main group only.** The official SC3 runs on every row `group = main` of the two
   locked tables, `config/configuration_groups.tsv` (main scope, `--patternKind=distinct`) and
   `config/configuration_groups_repeated.tsv` (repeated-item scope, `--patternKind=repeated`).
   Configurations of the intermediate and degenerate groups do **not** run SC3 in this batch. H2 and H2-R are
   tested only on the main group anyway (section 10 point 4, section 11 point 4), so this changes no test; it
   drops the descriptive reporting of the other groups mentioned in section 10 point 4. If those groups run
   later, they remain descriptive only. `protein_sdb1` belongs to the main group, runs, and is still only
   reported (section 11 point 5).

   > **Correction of 2026-10-04, after the method batch with \|𝒮\| = 1 had run.** The sentence "this changes
   > no test" above is **wrong**: H3 (section 2) needs the degenerate group as well ("near 1 on the degenerate
   > group"). Running only the main group, the second half of H3 **cannot be tested**; the analysis now says
   > so. H2 and H2-R are indeed unchanged. Running the degenerate group, or declaring that H3 tests only its
   > first half, is a decision left open to the author.
2. **Methods: the set of H2.** Seven keys: `cut-hide-rw-ls` (the method under test, section 12); `cut-hide-rw`
   and `cut-hide` (the two ablation variants, section 12); `hha`, `dbsh`, `sbsh-r1`, `sbsh-rmax` (the Holm
   family, section 2). No method is randomised, so the ten-seed convention of section 5 does not apply to
   this batch.
3. **Time limit: 1,800 seconds** of wall-clock time for **one method on one sensitive set**
   (`--timeLimit=1800`), counted from calling the method until it returns its solution; evaluating the
   solution and the Pareto front (section 11 point 6) are not counted. Over the limit: the row carries
   `status = OT` and every result column is left empty; out of memory: `status = OOM`. A method that
   finishes leaves `status` empty. Handling follows the table "Excluding instances" of section 5, unchanged:
   drop the pair, count, report; above 10%, no conclusion. The memory cap of every SC3 command is `-Xmx8g`.
   Implementation: cooperative checks (`hidenosp.util.Deadline`) when building a Nettree, when masking a
   position, in each maximum-flow phase, at each position of the exact dynamic program and in each pass of the
   local search; test `Sc3TimeLimitTest`.
4. **Order: \|𝒮\| = 1 first**, in both scopes, together with the Pareto front (section 11 point 6); then
   \|𝒮\| = 5, 10, 20. The timing of the \|𝒮\| = 1 batch is used to schedule the later batches. No parameter
   locked in this section or in section 12 is changed after seeing the results of the \|𝒮\| = 1 batch.

   > **Correction of the same day (2026-10-02), before any official SC3 run — decision of the author.** The
   > Pareto front **is split into a batch of its own**, not run together with the methods. Measured reason: a
   > trial run of all 54 main-group cells of \|𝒮\| = 1 on the development machine (one set per stratum;
   > `results-probe/sc3_dry_run_all_cells_probe_20261002.txt`) showed that building the Pareto front takes
   > 88–95% of the time of a cell — MSNBC [0,3] σ=5000 main scope: 2,005 seconds for five sets, of which the
   > seven methods took only 110 seconds; one front has up to 1,145 vertices — and that part is not under the
   > time limit of point 3. The method batch runs `--pareto=false`; the Pareto-front batch runs
   > `--methods=none --pareto=true` on the same cells with the same sampling (section 9), hence the same
   > sensitive sets. The content of section 11 point 6 is unchanged; only when it runs changes. Generators:
   > `tools/sc3_commands.py` (method batch) and `tools/sc3_commands.py --pareto` (Pareto-front batch). Order:
   > the method batch with \|𝒮\| = 1 first, then the Pareto-front batch or \|𝒮\| = 5, 10, 20 depending on
   > machine time.

The command of each cell is generated by `tools/sc3_commands.py` from the two group tables, not copied by hand;
that tool skips cells that already have a result file and lists SC3 files that match no cell.

**Consequence for machine invariance.** Rows without `OT` or `OOM` are still machine-invariant quantities.
**The set of `OT` rows depends on the machine**, and the set of `OOM` rows depends on the memory cap. Therefore
the official SC3 run is made on the measuring machine declared in row 5 of `config/protocol.yaml`, with
`--machine` of that machine, and the counts of `OT`/`OOM` cases are reported with the machine name and the
memory cap.

**Provenance.** When this section was written, the proposer had seen, besides the probes listed in section 12,
two probes on the development machine: `results-probe/sc3_time_limit_stop_probe_20261002.txt` (a limit of 2
seconds on MSNBC [0,10]: every method stopped less than 0.05 seconds after the deadline) and
`results-probe/sc3_heavy_cells_timing_probe_20261002.txt` (seven main-group configurations chosen as heavy
cases: the three largest pools of patterns with repeated items, the two largest pools of patterns without
repeated items, genome [0,10] σ=2500 with repeated items and MSNBC [0,10] σ=5000 without repeated items; five
sets per configuration, a limit of 600 seconds: no row `OT`; the longest 42.7 seconds for one method on one
set, by `cut-hide-rw-ls` on MSNBC [0,10] σ=1000 in the repeated-item scope). Both probes printed the Lost of
the methods on the sets they ran; the direction seen matches section 12. The value of 1,800 seconds was set
**before** the second probe and not changed after it. No official SC3 run has taken place.

**Unchanged:** all of sections 1–13.

---

## 15. Addendum of 2026-10-07 — fix of an implementation error of step 7 with patterns with repeated items (before any official SC3 result with \|𝒮\| > 1)

**Incident.** The method batch with \|𝒮\| = 5 on the measuring machine (code `8f8c601`, the same code as
`5627876`) stopped on 10 cells with the message "no position lowers a sensitive support any further, yet the
database threshold is still exceeded". Rerun with exactly that code on the development machine with key
`cut-hide`: exactly 10 of 34 cells stopped, all 10 in the repeated-item scope.

**Cause.** The greedy of §9.2 step 7 (several patterns, HIDE-DB) recorded the drop of each mask as 1 for every
pattern for which that position is vital. That is right when μ(P) = 1. With patterns with repeated items, a
position carries several Nettree nodes, so masking it can lower sup_NO by more than 1. The need ledger was
therefore higher than the actual support: the algorithm kept masking patterns that were already hidden, and
stopped when nothing was left to lower. In the repeated-item cells where it did not stop, it **masked more
than needed without saying so**.

**Fix (commit `dcb0f17`, speed-up in `a8b1bad`).** The need ledger subtracts the **actual drop** of each mask,
read from the maximum flow of the index that step 7 already builds for that sequence; it no longer subtracts
the "vital" mark of the candidate. The position-selection rule of step 7 (the ratio of the total drop to
1 + w(i), ordered by the vital mark) is **unchanged**. With an exact ledger, step 7 always stops with HF = 0
after at most Σ need rounds: a pattern that still needs lowering has sup_NO > 0 in some sequence, and by L0
(Menger on the Nettree) every node of a minimum vertex cut there is vital, so that sequence always has a
candidate.

**Measured consequences** (every number is exploratory on the development machine, key `cut-hide`, the same
commands as the official batch):
- \|𝒮\| = 1: unchanged; the one-pattern branch (the exact solution of P8) does not use step 7. The official
  results of section 14 stand.
- \|𝒮\| = 5, main scope: 14 cells, 257 rows, **0** rows different from the old code.
- \|𝒮\| = 5, repeated-item scope: 34/34 cells run (old code 24/34); on the 10 cells the old code ran, 177 rows
  change |R| or Lost, the fix uses fewer masks in all 177, and Lost does not increase in 176.
- \|𝒮\| = 5, 10, 20: 34/34, 27/27, 21/21 cells run; the two ways of obtaining the actual drop (recomputing
  sup_NO, and reading it from the flow of the index) give the same 1,579/1,579 rows. Record:
  `results-probe/sc3_several_patterns_ledger_probe_20261007.txt`.
- Test `CutHideSeveralPatternsTest`: 2,289 random instances, 2,107 with patterns with repeated items, every
  pattern hidden by definition; the old code fails this test with exactly the message of the measuring machine.

**Consequence for the batch.** Every SC3 file with \|𝒮\| = 5 produced by code `8f8c601` is discarded (none had
been committed) and rerun with the fixed code. No official result with \|𝒮\| > 1 existed before this section.

**Provenance.** The proposer had seen the official \|𝒮\| = 1 results (section 14) and the reruns above (key
`cut-hide` only, no baseline), and no official result with \|𝒮\| > 1. The change fixes a ledger error; it does
not choose between options.

**Unchanged:** all of sections 1–14.

---

## 16. Addendum of 2026-10-12 — running the degenerate group for the second half of H3 (before any SC3 run on this group)

**Decision of the author** (2026-10-12): run SC3 \|𝒮\| = 1, method batch, on **every row `group = degenerate`**
of the two locked tables — 16 configurations in the main scope and 1 in the repeated-item scope. The same seven
methods, the same 1,800-second limit, the same `-Xmx8g`, the same sampling (section 9), the same measuring
machine. Command: `tools/sc3_commands.py --group=degenerate`.

**Test.** H3 (section 2) as locked: the second half, "r near 1 on the degenerate group" (r ≤ 1.05), for the four
tier (b) baselines, in the main scope. The repeated-item cell is only descriptive (section 11 point 7). Rows of
the degenerate group do **not** enter H2, H2-R, the table of pairs (|R|, Lost) or the equivalence test (section
10 point 4); the analysis filters by group in all four places (the filter in the equivalence part was added on
2026-10-12, before any degenerate row existed, with the test `test_equivalence_on_main_group_only`). The
intermediate group still does not run: no hypothesis uses it.

**Provenance.** The proposer had seen the first half of H3 on the main group (the four baselines "near 1" in the
main scope, the correction note of section 14) and the trial run on the development machine below; no official
number of the degenerate group had been seen. The run completes the test locked in section 2; it is not chosen
by numbers.

**Trial run** (exploratory on the development machine,
`results-probe/sc3_degenerate_dry_run_probe_20261012.txt`): 17/17 cells run, 0 errors, 0 rows OT; the longest
method took 49.9 seconds on one set; the total time of the cells was 8350 seconds.

**Unchanged:** all of sections 1–15.

---

## 17. Addendum of 2026-10-12 — secondary analysis: the refinement of solutions for several patterns (AFTER the official results with \|𝒮\| > 1 had been seen)

**Decision of the author** (2026-10-12): the refinement of Proposition P15 (design §8.1) is measured on the
measuring machine as a **secondary, descriptive analysis**. The method under test of H2 and H2-R is
**unchanged**: still `cut-hide-rw-ls` as in section 12, which for \|𝒮\| > 1 is the greedy of §9.2 step 7.

**Why the method under test does not change.** This section is written **after** the official H2 and H2-R
results with \|𝒮\| = 1, 5, 10, 20 had been seen (`results-invariant/sc3_all_sizes_analysis_20261012.txt`): the
main scope does not reject the null hypothesis against any baseline; the repeated-item scope rejects it against
`hha` (Holm p 0.0058), and does not reject it against `dbsh` (Holm p 0.083) or the two SBSH variants. The
refinement lowers \|R\|, so it changes the very set of instances that enter Wilcoxon (section 5: only pairs with
the same \|R\|), and changes Lost on many sets. Bringing it into H2 now would change the outcome after seeing
the numbers. The H2 and H2-R results already reported stand, and are the only test results.

**Definition of `cut-hide-refine`** (SC3 key; code `CutHideConfig.withRefineSeveral()`, commit `22db94c`): for
\|𝒮\| > 1, the step-7 greedy of `cut-hide`, then REFINE of P15: (i) replace the part of the solution on each
one-pattern component of the shared-item graph with the exact one-pattern solution (P1 when μ(P) = 1, P8 when
g⁻ = 0) when that solution uses strictly fewer masks; (ii) reverse-delete each mask whose removal leaves every
pattern hidden. For \|𝒮\| = 1 this key is `cut-hide`, so this batch does not run \|𝒮\| = 1.

**Proven properties, reported as implementation checks (section 6), not tested:** on every set,
\|R\|(`cut-hide-refine`) ≤ \|R\|(`cut-hide`) and the mask set is inclusion-minimal; when step (i) replaces
nothing, Lost does not increase. Step (i) **may** increase Lost; the number of such sets is reported.

**Run.** Every main-group cell with \|𝒮\| = 5, 10, 20 (34 + 27 + 21 cells), **one** method
`--methods=cut-hide-refine`, every other parameter as in section 14 (1,800 seconds, `-Xmx8g`, the sampling of
section 9, the measuring machine of `config/protocol.yaml`, the locked JDK). The baselines are not rerun: their
rows in the official batch are machine-invariant quantities (section 14) and are paired by sensitive set. The
commands are generated by `tools/sc3_commands.py --refine --size=5|10|20`; that tool treats a file as belonging
to this batch when `methods=cut-hide-refine`, and does not confuse it with the method batch (the tool's own
self-test, with fault injection).

**Analysis, descriptive only.** `tools/preregistered_analysis.py` **does not read** the files of this batch (it
lists their names and writes "SECONDARY (section 17)"; test `test_refinement_batch_is_not_read`; the H2/H2-R/H3
report on the official data is byte-identical before and after this rule was added). Reported: (a) \|R\|
against the conflict bound LB_U (Proposition P14(e), `tools/lower_bound_table.py`), the median and the number of
sets that reach the bound; (b) the number of sets where `hha` uses fewer masks; (c) the change of Lost against
`cut-hide-rw-ls`: the number of sets that increase, that decrease, the median; (d) the number of `OT`/`OOM`
cases. No sentence of the paper presents the numbers of this batch as a test of H2.

**Reported alongside, descriptive: the optimum on small instances.** `SC3OPT` (commit `66a6fee`) builds, for each
SC3 sensitive set, instances of 8 and 16 real sequences (or contiguous windows of real sequences), solves them to
optimality with `SequenceSplitExact`, and compares the bound, the optimum, the greedy and the refinement. Results
in `results-invariant/sc3_reduced_optimum_20261012.txt` (machine-invariant, run on the development machine). The
small instances have their own thresholds and contain only short sequences or contiguous windows, so they only
describe how the distance to the bound splits; they do not replace the numbers on the full data.

**Provenance.** When writing this section, the proposer had seen: the official H2, H2-R, H3 results at every
size; the lower-bound table (`results-invariant/sc3_lower_bounds_20261012.txt`); the exploratory
`cut-hide-refine` run on every cell with \|𝒮\| > 1 on the development machine
(`results-probe/sc3_refine_several_probe_20261012.txt`: 1,579/1,579 sets, median \|R\|/LB_U lower by
0.000–0.006 in the main scope and by 0.06–0.09 in the repeated-item scope; the number of sets where `hha` uses
fewer masks went from 57 to 3); and the `SC3OPT` results above. The definition of REFINE was written and tested
(`RefineSeveralTest`) before the exploratory run; none of its parameters was tuned after seeing numbers.

**Unchanged:** all of sections 1–16.

---

## 18. Addendum of 2026-11-01 — change of measuring machine, full rerun, and the memory cap of the Pareto-front batch

**Decision of the author** (2026-11-01):

1. **The measuring machine of the project changes to a Dell XPS with 64 GB (Windows 11).** Every number in the
   paper comes from this machine. The machine name, processor, memory and JDK are written by that machine itself
   into `results-invariant/campaign_<machine name>/ENV_LOCK.json` at the first step of the run, and at the top of
   every result file; row 5 of `config/protocol.yaml` is filled from that file. The JDK stays exactly that of the
   old machine: Eclipse Temurin 25.0.4.1+1.
2. **Full rerun on the new machine**, in this order: SC1; SC2 on the 81 configurations of the two group tables;
   the SC3 method batch of the main group with \|𝒮\| = 1, 5, 10, 20 (section 14); the degenerate group (section
   16); the refinement batch (section 17, secondary analysis); SC3LB; SC3OPT (instances of 8 and 16 sequences);
   the Pareto-front batch. The list of cells is generated by `tools/campaign.py` from the locked tables, with the
   generators of section 14.
3. **The Pareto-front batch runs with `-Xmx32g`.** Every other batch keeps `-Xmx8g` as in section 14 point 3. The
   Pareto-front batch is a descriptive analysis (section 11 point 6), run separately with `--methods=none`, so its
   cap changes no method row. Measured reason: an exploratory cell ran out of memory at 6 GB. No official
   Pareto-front number exists yet.

**Machine invariance is the reproduction check.** Section 14 stated that rows without `OT` or `OOM` are
machine-invariant quantities. Therefore every count column of such a row on the new machine must **equal** the
corresponding row of DESKTOP-L8A6EPO; a difference is an error and must be explained before the numbers of the new
machine are used. The set of `OT` rows may differ (it depends on the machine) and is reported with the machine
name. SC2 on the new machine reproduces the group tables: the tables regenerated from SC2 of the new machine must
equal the two locked tables; a difference is reported, and the locked tables are **not** changed.

**One machine per analysis.** `tools/preregistered_analysis.py` refuses when the SC3 files come from more than one
machine and no `--machine` is given (test `test_one_machine_per_analysis`, with fault injection), and reads only
the files of the named machine. The files of DESKTOP-L8A6EPO stay in the repository as a record and as the
reproduction check; they do not enter the paper's tables.

**Provenance.** When writing this section, the proposer had seen every official result of DESKTOP-L8A6EPO (H2,
H2-R, the first half of H3, the table of pairs), the lower-bound table, the SC3OPT results and the exploratory
`cut-hide-refine` run; no official number of the degenerate group or of the Pareto front had been seen. Changing
the machine and rerunning is a decision about the environment, not a choice by numbers: every parameter of
sections 1–17 is unchanged.

**Unchanged:** all of sections 1–17.

---

## 19. Addendum of 2026-11-06 — competitors that stop at the threshold, the exact Lost optimum on small instances, and running time (AFTER every official result of sections 14–18 had been seen)

**Decision of the author** (2026-11-06): three additions, each run on the measuring machine of section 18 with the
locked environment (the JDK of row 5, `-Xmx8g`, 1,800 seconds per method and set, the sampling of section 9).

**Why.** (a) The tier (b) baselines sanitize every sequence they select until **no matching is left** (HHA,
Algorithm 1 of [R20]; DBSH and SBSH of [R3] likewise work sequence by sequence), while HIDE-DB only asks the
non-overlapping support to fall below σ. The regime "the baseline uses more masks", which holds on most instances of
H2 and H2-R, may therefore come from the stricter problem these baselines solve rather than from how well they solve
it. Competitors that stop as soon as the threshold is met answer that question. (b) H2 compares Lost only at equal
\|R\|, so it cannot say how far any method is from the Lost optimum; on small instances the optimum can be computed
exactly. (c) No running time has been measured.

**What does not change.** H2, H2-R and H3, their tests and their reported results stand as the only results of
sections 2–18. No sentence of the paper presents H4 (below) as a correction or a rescue of H2.

### 19.1 H4 — competitors that stop at the threshold

**Methods** (SC3 keys).

- `occurrence`: tier (a) of section 2 — mask the position lying on the most occurrences of the sensitive patterns
  that still need a drop, take the sequences in their natural order, and stop when every sensitive support is below
  σ (`hidenosp.baseline.OccurrenceCountHeuristic`, unchanged).
- `hha-stop`: HHA with one change, the stopping rule. The sequences carrying a sensitive pattern are taken in the
  order of HHA (ascending size of the matching set, ties to the lower index), and in each sequence the position on
  the most matchings of the patterns that still need a drop is masked (the local rule of HHA, ties to the leftmost),
  until every sensitive pattern has a database support below σ; then the method stops. Under HIDE-SEQ HHA already
  stops at r, so `hha-stop` equals `hha` there. The name describes the behaviour; it is not a published variant.

**Cells.** The 136 main-group cells of section 14 (\|𝒮\| = 1, 5, 10, 20; both scopes), one SC3 batch
`--methods=occurrence,hha-stop`, every other parameter as in section 14. The rows of `cut-hide-rw-ls` are those of
the official method batch, paired by sensitive set (machine-invariant rows, as in section 17).

**H4a (masks).** For each competitor c and each scope, r_c = \|R\|(c) / \|R\|(`cut-hide-rw-ls`) per instance, over
the main-group instances without `protein_sdb1`; verdict on the median with the thresholds of H3: ≥ 1.10 "more
masks", ≤ 1.05 "the same number of masks", in between no conclusion. Reported with it: the number of instances where
c uses fewer masks than `cut-hide-rw-ls`.

**H4b (Lost at equal masks).** The procedure of H2 (sections 2, 4 and 5: one-sided Wilcoxon signed-rank with Pratt's
treatment of zeros on the difference of Lost_rel, δ_L = 0.02, α = 0.05, rank-biserial, the exclusion rules, the
equivalence margins, the table of pairs (\|R\|, Lost)), with the family {`occurrence`, `hha-stop`} and Holm over
these two, separately in the main scope (H4b) and in the repeated-item scope (H4b-R).

### 19.2 SC3LOST — the exact Lost optimum on small instances (descriptive)

**Instances.** For each sensitive set of SC3 with \|𝒮\| = 1 in a main-group cell of either scope (the same pool,
strata and seed as section 9, so rows join SC3 rows on the set; `protein_sdb1` is reported only), an instance
keeps the pattern P and the gap and takes ten windows of W = 15 consecutive events of real sequences, from the
sequences of the dataset in a seeded random order: five windows that contain an occurrence of P (the window starts
at a seeded random offset among the offsets whose window contains one) and five windows from sequences in which P
does not occur. A sequence shorter than W is taken whole. For each requested drop r ∈ {2, 3, 4}, the threshold of
the instance is σ_I = sup_NO(P, I) − r + 1; when σ_I < 2 the instance is skipped and counted. F_{σ_I}(I) is mined
with l_max = 4.

**Optimum.** `exact-minR-minLost` (`hidenosp.baseline.IlpExact` with the side-effect objective): every hiding set of
the least size \|R\|* is enumerated and the least Lost among them, OPT_Lost, is taken, together with the greatest
Lost among them. An instance with more than 64 candidate positions, more than 12 masks or at least 20,000 minimum
hiding sets is refused by the class and counted by reason.

**Methods.** `cut-hide`, `cut-hide-rw`, `cut-hide-rw-ls`, `hha`, `dbsh`, `sbsh-r1`, `sbsh-rmax`, `occurrence`,
`hha-stop`.

**Reported, per scope and method.** The number of instances (and the skipped ones by reason); the share with
\|R\| = \|R\|*; among those, the share with Lost = OPT_Lost, the sum and the median of Lost − OPT_Lost, and the
position of Lost in the range from OPT_Lost to the greatest Lost of a minimum hiding set; for a method with
\|R\| > \|R\|*, the count and whether the lexicographic optimum is no worse in both \|R\| and Lost.
**Implementation checks** (stop the analysis when broken): every method hides; \|R\| ≥ \|R\|* for every method;
\|R\| = \|R\|* for the three CUT-HIDE keys; OPT_Lost ≤ Lost for every method with \|R\| = \|R\|*.

> **Correction of 2026-11-06, AFTER the 54 SC3LOST cells of ADMIN-PC (generation 2) had been seen.** The check
> "every method hides" stopped the analysis on 32 instances (96 rows), all of the repeated scope of the tested
> datasets, in 11 cells: the three CUT-HIDE keys refused to run because B_eps was empty (no frequent pattern
> outside the closure of P within epsilon of the instance threshold). The refusal is the guard of protocol row 2,
> written so that a band nobody supplied cannot turn CUT-HIDE silently into its uniform-weight variant. On all 32
> instances every minimum hiding set has Lost = 0 (OPT_Lost = greatest Lost = 0, measured). The competitors ran
> there; they reach \|R\|* on 7 to 21 of the 32.
>
> **Decision of the author: a selector, not an exclusion.** When the band has been computed and is empty, CUT-HIDE
> runs the uniform-weight configuration (`CutHide.forBand`; the adaptive keys start their iteration 0 from it).
> This is exact, not a substitute: with an empty band every vital-node weight is zero
> (`NettreeCutIndex.positionWeights` sums the factors of band patterns only), so the weighted configuration would
> compute the same masks; a test compares the two on 20 seeded instances. The method column of such a row names
> the uniform weighting (`CutHide[UNIFORM, ...]`, or "B_eps empty, iteration 0 with uniform weights" for the
> adaptive keys), so the selection is never silent, and `tools/lost_optimum_table.py` counts these rows per key.
> A band nobody supplied is still refused. Prediction written before the rerun: on the 32 instances every CUT-HIDE
> key reaches \|R\|* with Lost = OPT_Lost = 0, and every other row of the 54 cells is unchanged. The 54 cells are
> rerun with the selector; the counts are machine-invariant, and the rerun is compared row by row with ADMIN-PC.
>
> **Same correction for the small instances of SC3CERT (section 20.2), written before that rerun.** The 53 SC3CERT
> files of ADMIN-PC available when this was written hold 3,332 rows: 833 full-data rows, none refused, and 2,499
> small-instance rows, 29 of them refused because B_eps was empty (10 files). The full-data rows stay as measured.
> The small part of the 54 cells is rerun with the selector and `--parts=small`, which writes the small rows alone
> with the same sets and instances. Prediction: every refused row now carries \|R\|, Lost and B with no status, its
> instance is one of the 32 instances above, Lost = B = OPT_Lost = 0 there (so the row is certified), and every
> other small row equals the ADMIN-PC row column by column. The table takes the full-data rows from ADMIN-PC and
> the small rows from the rerun, joined with the SC3LOST rerun.

### 19.3 SC6, reduced — running time

**Cells.** The main-group cells of section 14 with \|𝒮\| ∈ {1, 10}, both scopes; in each cell the first set drawn
in each stratum (at most five sets). The subset is fixed by this rule, not by any measured time.

**Methods.** The nine of 19.2.

**What is inside the clock** (one row per method, as the protocol requires):

| method | timed | not timed |
|---|---|---|
| `cut-hide`, `cut-hide-rw`, `cut-hide-rw-ls` | mining F_σ(D) with l_max = 4, the near-threshold band and the weights, the evaluator the adaptive weight reads, the method | reading the file, drawing the set, evaluating the output |
| `hha`, `hha-stop`, `dbsh`, `sbsh-r1`, `sbsh-rmax`, `occurrence` | everything the method computes from the database | the same |

**Measurement.** `hidenosp.util.Timing.measure`: one warm-up run (protocol row 9, locked here to 1), one run that
sets the number of repetitions (discarded), then the repetitions of protocol row 8; wall-clock time by
`System.nanoTime` (mean and standard deviation) is the reported quantity; the processor time of the thread is
recorded beside it and read only where a run lasts well above the 15.625 ms step of the Windows thread clock. A
single run above 1,800 seconds is `OT`.

**Index cost.** For each instance: the time to build `NettreeCutIndex` for the sensitive pattern, and the time to
apply the masks of the `cut-hide` solution one by one with the incremental update against rebuilding the index after
each mask, under the same repetition rule.

**Memory is not reported** (protocol row 6 stays unlocked): heap sampling every 50 ms cannot catch the peak of a run
shorter than a second, and the resident set needs a wrapper that does not exist.

The rows are machine-dependent and go to `results/`, from the measuring machine only.

### 19.4 Provenance

When writing this section, the proposer had seen every official result of sections 14–18 on ADMIN-PC (H2, H2-R,
both halves of H3, the table of pairs, the lower bounds, SC3OPT, the refinement, the Pareto fronts) and the
exploratory small-instance runs of 2026-09-26 (`results-probe/lost_small_exact_probe_20260926.txt`, CUT-HIDE stages
against an exhaustive optimum on 480 windows; the harness of those runs is not in the repository and cannot be
traced back, and the construction of 19.2 is written anew without claiming to reproduce it). `occurrence` had run
only in exploratory counts of the design document; `hha-stop` had never run; no running time had been measured.

**Unchanged:** all of sections 1–18.

---

## 20. Addendum of 2026-11-06 — amendment of 19.3 before any time was measured, and the certified Lost bound

**Decision of the author** (2026-11-06): (a) amend the clock of 19.3; (b) implement and measure the certified lower
bound on the optimum Lost of proposition P12 of the design (the paper states it as a contribution). Both run in a
third generation of the campaign (same machine and JDK as generations 1 and 2; only the measured code differs).

> **Correction (2026-11-06, before any lock of generation 2 existed).** Both run in the **second** generation, with
> the batches of section 19, not in a third one: when this section was written no batch of generation 2 had run and
> its lock did not exist, so one lock covers the code of sections 19 and 20 and the measuring machine needs one
> paste. The campaign tool places `sc3lost`, `sc3-stop`, `sc3cert` and `sc6` in generation 2. Nothing else of this
> section changes.

### 20.1 Amendment of 19.3: mining F_σ(D) is timed once per cell, on a row of its own

**Why.** 19.3 put the mining of F_σ(D) inside the clock of every method that reads it, on every timed call. A
feasibility run on the development machine (no time recorded, only where the time goes: three samples of the
stack, all inside `FrequentPatterns.mine`) showed that this repeats the most expensive step at least 125 times per
cell (five methods, five sets, at least five calls each), against once per cell in SC3; on the heaviest cells this
is hundreds of machine hours. F_σ(D) depends on the database, the gap, σ and l_max only, not on the sensitive set:
it is a preprocessing of the database that every hiding request on it shares.

**New rule.** Per cell, one row `mine-frequent` times the mining of F_σ(D) (same `Timing.measure`, one warm-up,
protocol row 8). Every method row then times the method only: F_σ(D) is mined once outside the clocks and handed to
every method (to the five that read it, and to the four that ignore it); each timed call builds a fresh evaluator on
it, so no cache carries over between repetitions. The tables print the method time and, beside the five methods that
read F_σ(D) (the three CUT-HIDE keys and the two SBSH keys), the mining time of the cell; the two are never added into
one number presented as measured. Everything else of 19.3 is unchanged.

### 20.2 The certified lower bound on the optimum Lost (P12)

**Definition** (design P12, one sensitive pattern P, HIDE-DB, g⁻ = 0). For a reference minimum hiding set R₀ and each
non-sensitive frequent pattern Q outside the closure that is lost under R₀: in every sequence S carrying P, take one
minimum cut C_S of the Nettree of Q in S (the source side of the residual graph of `NettreeFlow`), weigh each position
by the number of nodes of C_S on it, and compute LB_Q, the least total weight of a minimum hiding set, with the exact
program of theorem P8 (`CutHide` with these explicit weights). With m_Q = sup_NO(Q, D) − σ + 1, Q is lost in every
minimum hiding set when LB_Q ≥ m_Q. The bound is B = the number of such Q. A pattern not lost under R₀ cannot be lost
in every minimum set, so B does not depend on the choice of R₀ beyond the patterns it examines; B ≤ OPT_Lost holds for
every R₀. R₀ is the solution of `cut-hide-rw-ls`.

**Where it is measured** (scenario SC3CERT, one run per main-group cell with |𝒮| = 1, both scopes; 54 cells):
- on the full data, for every SC3 set: Lost of `cut-hide-rw-ls` (which must equal the official SC3 row), B, the number
  of patterns examined, and whether Lost = B (the solution is then **certified optimal**);
- on the small instances of 19.2 (rebuilt with the same seeds, joined with the SC3LOST rows on set and drop): B against
  OPT_Lost.

**Reported, descriptive.** Per scope: the share of full-data sets certified optimal, the median of Lost − B; on the
small instances the share with B = OPT_Lost and the median of OPT_Lost − B. **Implementation checks** (stop the
analysis): B ≤ OPT_Lost on every small instance; B ≤ Lost on every set; the Lost of the rerun equals the SC3 row.

**Provenance.** When writing this section, the proposer had seen every official result of sections 14–18, the
exploratory runs of 19.2 (implementation checks and skip reasons only) and the exploratory runs of P12 of 2026-09-26
(`results-probe/lost_lower_bound_probe_20260926.txt`: on 160 full-data sets the bound certified few sets with Lost > 0;
on small instances it counted about half of the patterns lost in every minimum set). No official number of the bound
or of any running time exists.

**Unchanged:** all of sections 1–19 apart from the clock of 19.3, amended in 20.1.

## 21. Addendum of 2026-11-08 — the closure S⁺ corrected, and the invariant batches rerun (added after the main comparison)

**Decision of the author** (2026-11-08): correct the definition of 𝒮⁺, apply it to the methods as well as to the
measures, and rerun every invariant batch that reads 𝒮⁺, on the measuring machine of generations 1 and 2, with the
same JDK and the same heap (`-Xmx8g`). This section was written after every result of sections 14–20 had been seen.

**What was wrong.** Sections 2 and 14–20 count Lost and SupDist on F_σ(D) \ 𝒮⁺, with 𝒮⁺ the sensitive patterns and
every frequent pattern that contains one of them **as a subsequence** (protocol row `S_plus_relation` before
2026-11-08). The stated reason, that hiding P hides its super-patterns, holds only for a super-pattern that contains P
as a block of consecutive items when g⁺ is finite. Counterexample: D = {ab, accb}, σ = 1, gap [0,1]; masking the a of
ab hides ab, and acb keeps its occurrence (0, 1, 3) in accb. Every gap of the paper is finite. The methods read 𝒮⁺ too:
the near-threshold band B_ε of CUT-HIDE and its weights, the watched patterns of the adaptive weight and the local
search, the side-effect counts of SBSH, the side-effect objective of the exact method and the certified bound.

**Corrected rule** (`FrequentPatterns.Closure.NECESSARY`, carried by `HideProblem`). Under HIDE-DB with a hiding
threshold no larger than the σ of F_σ(D), 𝒮⁺ is the sensitive patterns and every frequent pattern that contains one of
them as a block (as a subsequence when g⁺ is unbounded): such a Q has sup_NO(Q) ≤ sup_NO(P) < σ under every hiding
set. Under HIDE-SEQ, 𝒮⁺ is the sensitive patterns alone. The test `ClosureRuleTest` checks the counterexample by hand
and the statement itself on 400 random databases (every pattern of the corrected 𝒮⁺ hidden by random hiding sets;
some patterns of the earlier 𝒮⁺ left frequent), and catches each of five injected faults.

**Generation 3 of the campaign** (`tools/campaign.py`, batches `g3-*`): the cells of `sc3-s1` … `sc3-s20`,
`sc3-degenerate`, `sc3-refine`, `sc3-stop`, `sc3lost`, `sc3cert` and `sc3opt`, cell for cell, with the option
`closure=necessary`. Every result file writes the line `# closure <rule>`; a file without it was written under the
subsequence rule. Each SC3 row also carries `lost_subseq`, `lost_rel_subseq`, `sup_dist_subseq` and
`nonsensitive_before_subseq`: the measures of the same solution under the earlier rule.

**Also corrected:** the column `frequent_after` (frequent patterns left after the sanitization) counted the frequent
super-patterns of 𝒮⁺ as kept; it now counts their real losses. No table reads it.

**Not rerun:**
- SC1, SC2 and SC3LB do not read 𝒮⁺. Their files stand.
- The Pareto-front batch is no longer reported.
- SC6 (running time). Its methods read 𝒮⁺ as an input, so its rows keep the earlier rule. Their check against SC3
  (equal |R| and Lost) is made against the SC3 files of the earlier rule. The paper states that the timing was
  measured with the earlier 𝒮⁺.
  **Amended 2026-11-10 (22.6):** SC6 is measured again under the corrected rule, as a batch of generation 4.

**Checks before the lock of generation 3** (development machine, results not reported):
- the current code with `closure=subsequence` reproduces every common column of 14 stored files: five SC3 method
  cells (455 rows), one refinement cell (20), two stop-at-threshold cells (36), two SC3LOST files (162), two SC3CERT
  files, and two SC3OPT files (38);
- the three SC3CERT rows that differ are the small instances that generation 2 refused with an empty B_ε, now solved
  by the selector of commit 2f09296 (section 19.2 correction);
- with `closure=necessary`, every instance whose 𝒮⁺ is the same under both rules (175 of 175 rows of two cells)
  reproduces its stored row;
- the side-column denominators equal the stored denominators, and the new denominators equal the stored ones plus
  the measured number of patterns that leave 𝒮⁺.

**Provenance.** Before writing this section the proposer had measured, from the stored files and without masks,
the following.
- How many patterns leave 𝒮⁺ per instance.
- Bounds on the effect for H2 and H2-R (not for H3 or H4). No rejection of H2 or H2-R can change. Two
  equivalence verdicts of H2-R can change (DBSH, SBSH-r1).
- That the instances where CUT-HIDE and a baseline of the Holm family use the same |R| all have |𝒮| = 1.

These bounds assume the earlier solutions. The methods now read the corrected 𝒮⁺, so solutions can change, and no
number of generation 3 exists.

**Unchanged:** every hypothesis, threshold, test, margin, exclusion rule and group table of sections 1–20. The
analysis reads the files of generation 3 for every invariant number; files of the two rules are never mixed in one
table.

---

## 22. Added after the main comparison (2026-11-08) — HIDE-SEQ and g⁻ > 0 (written 2026-11-09, before any of these runs)

**Decision of the author** (2026-11-08): widen the experiments instead of narrowing the claims, by running HIDE-SEQ
and gaps with g⁻ > 0. Everything below was fixed after every result of sections 14–21 had been seen (see
Provenance), and before any run of this section. The paper states it as "fixed after the main comparison". No result
of SC1 to SC3 is rerun or changed by this section.

**Scope fixed by the author** (2026-11-09), chosen among three options with their estimated machine time: HIDE-SEQ at
r = 2 only, the small HIDE-SEQ instances at r = 1 only, and g⁻ > 0 in full (22.2, 22.3). This is the last experimental
batch of the paper: nothing is added after it, whatever its results. r = 1 and r = 3 of the comparison and r = 2 and
r = 3 of the small instances do not run (22.4).

### 22.1 What the code adds

- **The exact program for g⁻ > 0** (`hidenosp.hide.WindowDp`, the dynamic program of the corollary of Theorem 2): the
  state is, per level j < m, the set of positions among the g⁺ + 1 latest that carry a reachable node of level j
  outside the cut, (m − 1)(g⁺ + 1) bits in all, at most 62; g⁺ must be finite. `hidenosp.hide.ExactCurve` calls the
  program of Theorem 2 when g⁻ = 0, exactly as before, and this one when g⁻ > 0. CUT-HIDE (stages i–iii), the data
  profile (|R|* of a repeated-item pattern) and the local search go through it.
- **HIDE-SEQ in the three stages of CUT-HIDE.** Stage (i): the fixed allocation t_S = r − 1. Stage (ii): the six
  reweighting rounds of (13) with that allocation. Stage (iii): one-sequence moves only (an allocation move does not
  exist under a per-sequence limit). Several patterns: the greedy rule (15) in every sequence, until every sensitive
  pattern is below r in every sequence.
- **`hha-emb`**, the occurrence-count rule of Abul, Bonchi and Giannotti (TKDE 2010, p. 1713, "Maximum occurrence
  counts"): "the iteration in Sanitize(T, P^h) can be terminated whenever |M_T^{P^h}| ≤ ρ", with ρ = r − 1. Every
  sequence whose matching set (every embedding of every sensitive pattern) holds at least r embeddings is sanitized
  by the local rule of HHA. With several patterns the bound applies to the matching set of all of them together, as
  the paper states it.
- **Scenarios** (repository identifiers; the paper numbers its experiments separately): `SC4` (the comparison of SC3
  under HIDE-SEQ, option `--r`), `SC4LOST` (the exact Lost optimum of SC3LOST under HIDE-SEQ, option `--rs`), `SC1GAP`
  (implementation check of `WindowDp`). The SC3 scenario itself runs g⁻ > 0 through its existing options `gmin` and
  `gmax`.
- **Checks.** `WindowDpTest` (2,179 targets on 832 random instances with g⁻ ∈ {1, 2}, 504 with repeated items, 0
  mismatches against every subset of positions, weight as well as size; agreement with the program of Theorem 2 when
  g⁻ = 0); `CutHideExtendedScopeTest` (the three stages against every subset, HIDE-SEQ and HIDE-DB with g⁻ = 1:
  hiding, |R| = |R|*, Lost not rising across the stages, never below the optimum); `PerSequenceBaselineTest` (the
  exact enumeration under HIDE-SEQ against every subset, the occurrence-count rule). Seven injected faults (three in
  the program, two in the HIDE-SEQ stages, two in the pruning of the exact enumeration) were each caught. With g⁻ = 0, four SC3 cells and one SC3LOST cell (206 rows) are identical, row for row and header for
  header, between the code before and after this section; a control run under the earlier closure rule differs.

### 22.2 HIDE-SEQ (`SC4`, `SC4LOST`)

**Cells.** The cells of `sc3-s1` and `sc3-degenerate` (|𝒮| = 1) and of `sc3-s5` (|𝒮| = 5): the same dataset, σ, gap,
scope, sampling (section 9: five strata, 4 sets per stratum, seed 2026), l_max = 4, closure rule `necessary`, at
r = 2. σ only defines F_σ(D) for Lost and SupDist. The intermediate group does not run, as in section 14.
An instance in which no sequence reaches r for any sensitive pattern needs no mask; it is written with the status
`no sequence reaches r` and counted apart, never as a tie.

**Methods.** `cut-hide-rw-ls` (the method under test), `cut-hide-rw` and `cut-hide` (its stages), the Holm family
`hha`, `dbsh`, `sbsh-r1`, `sbsh-rmax` under HIDE-SEQ (each reads sup_NO per sequence and masks), `occurrence`
(descriptive), `hha-emb` (H5a only, descriptive otherwise). Time limit 1,800 s per method and set, `-Xmx8g`, the
exclusion rules of section 5.

**H5a (masks).** For each baseline and for `hha-emb`, the median over the main-group instances with |𝒮| = 1 (without
`protein_sdb1`) of |R| / |R|*_SEQ, with |R|*_SEQ = Σ_S OPT_S(r − 1) exact (Theorem 1 for distinct items, Theorem 2(b)
or its corollary otherwise); verdict on the median with the thresholds of H3 (≥ 1.10 more masks, ≤ 1.05 the same, in
between no conclusion), no test, as H4a.

**H5b (Lost at equal masks).** The procedure of H2 (sections 2, 4, 5: one-sided Wilcoxon signed-rank with Pratt's
treatment of zeros on the difference of Lost_rel, δ_L = 0.02, α = 0.05, Holm over the four baselines, rank-biserial on
the raw differences, the equivalence margins ±0.02 for Lost_rel and ±0.01 for SupDist, the table of pairs (|R|,
Lost)), on the main-group instances at equal |R|, **separately for |𝒮| = 1 and |𝒮| = 5**: two families of four tests.
The result is written
"the null of H5b is rejected" or "is not rejected", per baseline and family. Sensitivity analysis: the dataset as the
unit, as for H2 (median difference over the equal-count instances of each dataset, one-sided sign test over the
datasets).

**Small instances (`SC4LOST`).** The cells of `sc3-s1`; the windows, strata, seeds and the instance threshold
σ_I = sup_NO(P, I) − drop + 1 (drop ∈ {2, 3, 4}) of SC3LOST, so F_σI(I) is the one of SC3LOST; the hiding constraint
sup_NO(P, S) < r in every window, r = 1. Every minimum hiding set is enumerated (the exact class, with the
per-sequence bounds of 22.1); reported: instances solved, instances where each method reaches |R|*_SEQ, reaches the
optimum Lost, and the total excess. Descriptive.

### 22.3 g⁻ > 0 (`SC2`, `SC3`, `SC1GAP`)

**Gaps.** [1,2] and [1,3]. [2,4] runs only if the author adds it after the estimate of running time; it is not run by
default.

**Groups.** SC2 `--profile=pool` and `--profile=repeated` on the 12 datasets and every σ of row 1 of the protocol table,
at each new gap; the groups follow section 3 and section 10 with the same thresholds (ρ_rep ≥ 0.20 main, ≤ 0.05
degenerate, β ≥ 0.20 effective). The two tables are written by the analysis tool from the SC2 files and locked by
commit before any SC3 run at g⁻ > 0.

> **Locked 2026-11-09, commit `0c83332`** (`tools/preregistered_analysis.py --write-gap-groups`, from 71 SC2 files of the
> development machine; SC2 measures properties of the data only, as the tables of section 10 were). Main scope: 37
> configurations, 14 in the main group, 13 degenerate. Repeated-item scope: 34 configurations, 31 main, 1 degenerate.
> fifa [1,2] σ = 3000 has no distinct-item candidate of length 2–4 in F_σ: the configuration has nothing to hide and
> does not exist in the main scope. The tables of g⁻ = 0 are unchanged and still validate (42 and 39 configurations).
> Cells: 47, 23, 17 and 11 at |𝒮| = 1, 5, 10, 20 and 13 degenerate-group cells. The batch `g4-sc2gap` reruns these SC2
> cells on the measuring machine, and the analysis compares its groups with the locked tables.

**Cells and methods.** The main-group rows of the new tables at |𝒮| ∈ {1, 5, 10, 20} in both scopes, and the
degenerate-group rows at |𝒮| = 1, exactly as sections 14 and 16: the seven methods of section 14, 1,800 s, `-Xmx8g`,
closure rule `necessary`.

**H6.** H2, H2-R and H3 repeated at g⁻ > 0 with the same thresholds, tests and margins, reported separately for
|𝒮| = 1 and for several patterns. For distinct items, |R|* comes from Theorem 1 (the flow), independent of the program
CUT-HIDE runs; every row of CUT-HIDE must mask exactly |R|*, and the count of rows compared is reported.

**SC1GAP.** 3,000 random instances (sequence length 6–11, m = 2–4, g⁻ ∈ {1, 2}, g⁺ ≤ 4, alphabets of 2 or 3 items):
OPT(t) of the program against exhaustive search over subsets with exhaustive packing, every t; the number of
instances, targets and mismatches is reported per gap and per kind of pattern.

### 22.4 Where this differs from the request of 2026-11-08

1. `hha-emb` with several patterns bounds the matching set of all sensitive patterns together, as the paper states,
   not each pattern.
2. Only r = 2 runs for the comparison and only r = 1 for the small instances. Measured reason (probe on the
   development machine, three configurations, |𝒮| = 1, not reported): at r = 2 the methods stand as at g⁻ = 0, so
   r = 1 and r = 3 would repeat the picture at three times the machine time (35–46 hours for r ∈ {1, 2, 3} against
   12–15 hours for r = 2); on the windows of 15 events of the small instances most instances need no mask at r ≥ 2
   (protein_sdb4: 45 of 60 at r = 2, 54 of 60 at r = 3), so only r = 1 gives instances to solve.
3. The HIDE-SEQ cells are those of SC3 (main and degenerate groups at |𝒮| = 1, main at |𝒮| = 5), so every instance is
   paired with its SC3 instance; the intermediate group does not run.
4. Running time of HIDE-SEQ and of g⁻ > 0 is not measured by these batches: SC4 and SC3 write machine-invariant rows
   only. A timed run needs SC6 extended to these problems, which is not done; the author decides whether to add it.

### 22.5 Provenance

Before writing this section the proposer had seen every result of sections 14–21 and, from generation 3, the SC3LOST,
SC3OPT and SC3CERT files and 24 of the 34 cells of `g3-sc3-s5`. Of the new problems the proposer had seen only: the
tests of 22.1, one SC4 cell (sepsis [0,1] σ = 300, r = 2, one set per stratum) and one SC1GAP run on the development
machine, and the running-time probe used for the estimate. None of these is reported.

The hypotheses H5a, H5b and H6 were written before the Lost and |R| of the probe were read. The scope above was fixed
after they were read: on 264 pairs (instance × baseline) under HIDE-SEQ and 168 pairs at [1,3], no baseline used no
more masks and lost no more patterns than `cut-hide-rw-ls` with one of the two strictly smaller (corrected 2026-11-09:
an earlier version of this sentence gave 166 pairs, a hand count from a table that left out three of the baselines); under HIDE-SEQ the median |R| / |R|* of HHA and of the
occurrence heuristic was 1.000–1.024, so H5a may not hold for them. Neither the hypotheses nor their thresholds were
changed after reading these numbers.

### 22.6 Running time under the corrected 𝒮⁺ (decision of the author, 2026-11-10, before any of these runs)

**Decision.** The reduced SC6 of 19.3, as amended by 20.1, is measured again under the corrected rule of section 21,
as the batch `g4-sc6` of this generation. This replaces the last item of "Not rerun" in section 21. Generation 4 stays
the last experimental batch; this batch is part of it.

**Why.** The comparison tables now come from the files of the corrected rule. The timed methods read 𝒮⁺ (19.3), and
Lost is counted on F_σ(D) \ 𝒮⁺, so a timed run of the earlier rule is no longer the run of the table row beside it:
of the 3,528 timed rows of the earlier rule, joined with the SC3 rows of the corrected rule on the same machine,
2,764 carry the same |R| and Lost and 764 do not (89 of the 392 timed sets, 43 of the 81 cells). With the batch
measured again, every table of the paper follows one rule and the check of 19.3 (equal |R| and Lost in the timed run
and in the SC3 row) is made against the files the tables read.

**What runs.** The batch `sc6`, cell for cell (81 cells), with the option `closure=necessary` and nothing else
changed: the same cells, the same sets (the first set drawn in each stratum), the nine methods, one warm-up run, the
same time limit, the same clock (19.3, 20.1), the same measuring machine, JDK and heap (`-Xmx8g`) as generations 1 to
3. The whole batch runs again, also the methods that do not read 𝒮⁺, so that the timing table comes from one batch.

**What is reported.** Exactly what 19.3 and 20.1 report, read from the files of the corrected rule. The timing files
of the earlier rule stay in `results/`; no table of the paper reads them. No hypothesis is attached to running time,
as in 19.3. Running time of HIDE-SEQ and of g⁻ > 0 is still not measured (22.4, item 4).

**Provenance.** Written after every running time of the earlier rule had been seen. Before the lock of this batch one
cell (sepsis [0,1] σ = 300, repeated items, |𝒮| = 1) was run on the development machine under the corrected rule,
written outside the repository, to check the join only: its 45 timed rows carry the |R| and Lost of the SC3 rows of
the corrected rule (45 of 45), and 9 of the 45 differ from the SC3 rows of the earlier rule. Its times are not
reported.

**Unchanged:** all of sections 1–21.
