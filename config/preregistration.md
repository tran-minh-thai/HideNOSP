# Locked before running SC3

> **Language.** This file is the English translation of the Vietnamese original as of commit `c16f727`
> (2026-11-01), which stays in the git history. Section numbers, values, dates, file names and decisions are
> unchanged; decimal commas are written as decimal points. Each section keeps the date on which the original
> was committed. The three documents cited just below were renamed on 2026-11-02; they are cited by their new
> names (formerly `config/de_xuat_gia_tri_nhom_A.md`, `results-invariant/giai_doan_1_tom_tat.md` and
> `results-invariant/khao_sat_bo_du_lieu.md`). No value, rule or decision changed.

Rule 2.4.7: the practical-effect threshold and the tests must be locked **before** SC3 runs.
This file is that lock.

**Status: LOCKED, 2026-09-12.** The commit that recorded this file is the evidence of the date. From
here on, changing any number below must be a separate commit stating the reason, and a change made
after seeing SC3 results must be declared in the paper.

Rationale for each value: `config/group_a_proposed_values.md`. Data behind it:
`results-invariant/phase1_summary.md` and `results-invariant/dataset_survey.md`, both
from SC1 and SC2, run independently of SC3.

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
