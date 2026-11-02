# Proposed concrete values for group A (the decisions that block SC3)

> **Language.** This file is the English translation of the Vietnamese original as of commit `ea39da4`
> (2026-11-02), which stays in the git history. Values, dates, file names and decisions are unchanged;
> decimal commas are written as decimal points.

This is a **proposal for the author to approve**, not yet the locked version. Locking means writing the numbers into
`preregistration.md` and `protocol.yaml` and then committing — the git history serves as evidence of the time.

Each item has four parts: **Proposal**, **Grounds** (measured numbers, not intuition), **The cost to
pay**, and **How to refute** — what would have to happen for this number to be wrong.

---

## A warning to read first

Rule 2.4.7 exists to prevent locking a threshold **after** having seen the direction of the comparison result.
The proposer must declare: while building the framework, the proposer **ran and saw** one exploratory SC3 run on substitute
synthetic data, in which CUT-HIDE lost fewer patterns than the baselines.

Therefore every number below is anchored to the **scale of the data** (the size of `F_σ`, the gaps in
the distribution of ρ_rep), **not** to the distance observed between methods. If the author wants the pre-locked
file to be absolutely clean methodologically, the author chooses δ_L and the equivalence margin again; those two
numbers are the only place where having seen the direction could introduce a bias.

The **valid** way to calibrate, if an empirical ground for δ_L is wanted: run **one** single method
on many sensitive sets and measure the spread of Lost across the sets. This does not reveal the direction of
the comparison, and δ_L should be at least as large as that spread.

---

## A1. Practical-effect threshold δ_L of H2

**Proposal.** Change the quantity of H2 to **relative Lost**:

> Lost_rel = Lost / |F_σ(D) \ 𝒮⁺|, where the denominator is the number of non-sensitive frequent patterns before sanitisation.

and set **δ_L = 0.02** (two percentage points). Report the median **absolute** difference alongside in the
same table, so that the reader sees both scales.

**Grounds.** The measured |F_σ| spans more than an order of magnitude: 51 (dna σ=2500) to 672 (msnbc σ=1000). A
single absolute δ_L would be strict on dna (2 patterns = 4% of 51) and lax on msnbc (2 patterns = 0.3% of
672); mixing those two scales in one Wilcoxon test mixes two different things. Normalising by
|F_σ \ 𝒮⁺| makes the units of replication homogeneous. This denominator is **already available** in the SC3 column
`frequent_before`, with no change to the measuring code.

Why 0.02 and not smaller: 1 percentage point on dna (|F| ≈ 51) is less than half a pattern, so any
difference, even of only one pattern, exceeds the threshold and δ_L loses all its effect. 0.02 corresponds to
about 1 pattern on dna and 6.5 patterns on msnbc σ=2000.

**Do NOT use** Σ|S| or |R|\* as the denominator: Σ|S| is the denominator of DistR, |R|\* is the bound on the
number of masks. Taking them as the denominator of Lost is a definitional error.

**Cost.** On a dataset with a small |F_σ|, a difference of one pattern already comes close to the threshold,
so H2 is more easily supported there. This must be stated clearly in the limitations section.

**How to refute.** If the spread of Lost across sensitive sets (measured with one single method)
is larger than 2 percentage points, then δ_L = 0.02 lies within the noise and must be raised.

---

## A2. Thresholds of ρ_rep and β, the unit they apply to, and the band in between

**Proposal.**

| Item | Value |
|---|---|
| Unit the thresholds apply to | **configuration** = the triple (dataset, σ, [g⁻,g⁺]) |
| Summary statistic | **median** ρ_rep over the sampled sensitive patterns of that configuration |
| Main group | ρ_rep ≥ **0.20** |
| Degenerate control | ρ_rep ≤ **0.05** |
| Band in between | 0.05 < ρ_rep < 0.20 → **intermediate group**, not used to test H2/H3 |
| Constraint effective | β ≥ **0.20** |
| Degenerate-constraint control | β ≤ **0.05** |

**Grounds.**

*The unit is the configuration, not the dataset.* On msnbc σ=2000, changing the gap from [0,1] to [0,10]
drops β from 0.432 to 0.043. β is a property of the **configuration**, not of the dataset; assigning roles
to a "dataset" would assign them wrongly. By the same reasoning, ρ_rep on msnbc goes from 0.190 (σ=1000) up to
0.395 (σ=5000).

*Why 0.05 and 0.20.* The measured mean ρ_rep, sorted in increasing order: 0.000 (sign, bms1), 0.008 (fifa),
0.052 (bike), 0.153 (bible), 0.154 (leviathan), 0.323 (msnbc), 0.786 (dna). There are **two sparse gaps**
in the distribution: between 0.052 and 0.153, and between 0.154 and 0.323. The two proposed thresholds fall into
those two gaps, so the grouping conclusion is **insensitive** to shifting the thresholds by a few percent. Choosing
thresholds in the sparse parts of the distribution is choosing by principle; it is entirely different from choosing a
number just large enough for a favoured dataset to land in the group one wants.

*Why the median and not the mean.* The distribution is skewed: msnbc [0,3] σ=2000 spans 0.106–0.575. The
median can be computed directly from the SC2 CSV (one row per pattern), with no change to the code.

**Consequence for the current data.** Main group: **dna, msnbc**. Intermediate: **bible, leviathan**.
Degenerate: **sign, bms1, fifa, bike**.

**Cost.** The intermediate group is not used to test hypotheses, so the effort of running bible and leviathan
buys only an illustration that ρ_rep and β are independent of each other. That is still worth printing.

**How to refute.** If a new dataset falls right into 0.05–0.20 and matters to the paper, then the boundary is
cutting through a dense region rather than a sparse one, and must be chosen again.

---

## A4. Equivalence margins for Lost and SupDist

**Proposal.** Lost: **±0.02** on the relative scale, i.e. exactly equal to δ_L. SupDist: **±0.01**.

**Grounds.** The equivalence margin should equal the effect threshold; setting it smaller would create a wide
region where a result is "neither different nor equivalent", i.e. nothing can be concluded. SupDist is by nature
a dimensionless ratio in [0,1], so ±0.01 is one percentage point of support.

**To be honest:** ±0.01 for SupDist is the **least grounded** number in this whole document. It should be
reset after measuring the spread of SupDist with one single method (see the valid way to calibrate
at the top of the file).

---

## A5. The quantitative meaning of "near 1" in H3

**Proposal.** For the ratio r = |R| / |R|\*: "above 1" means **r ≥ 1.10**; "near 1" means
**r ≤ 1.05**. The region 1.05 < r < 1.10 gives no conclusion.

**Grounds.** H3 needs a buffer between its two parts; otherwise every r > 1, however small, becomes "above".
5% and 10% are two conventional marks, and the buffer keeps the two parts mutually exclusive.

**Warning.** On the probe run, every exact **and** heuristic method gave r = 1.000 —
i.e. no baseline used extra masks. If that repeats on real data, **H3 cannot be
rejected**, and that is a negative result to be reported, not an error. The wording for this
situation should be prepared in advance.

---

## A6. The family of baselines in the Holm correction

**Proposal.** The family = the **four** tier (b) baselines: HHA, DBSH, SBSH-r1, SBSH-rmax. Holm correction
over 4 comparisons. Tier (a) (occurrence, HH, HR, RH, RR) is reported **descriptively**, not tested.

**Grounds.** Tier (a) exists to show that the problem does not solve itself (§11.3), not to test
hypotheses; putting them into the family only makes Holm stricter without adding content. A family of 4 keeps the
statistical power for exactly the comparisons the paper relies on.

**How to refute.** If SBSH cannot be checked against [R3] (see A25), the family drops to 2, and this must be
declared again before running.

---

## A7. Pairing for H2: the same |R| budget, or each method choosing its own

**Proposal.** Run each method **until HF = 0** with |R| of its own choosing, then keep two reporting modes apart:

1. Instances where every method uses the **same** |R|: put into the Wilcoxon test for H2.
2. Instances where a baseline uses **more**: reported separately as pairs (|R|, Lost), not tested.
   The conclusion there is stronger and needs no statistics: the baseline both masks more and loses more.

**Grounds.** Comparing Lost at two different levels of |R| compares two different things. Forcing a baseline to
stop exactly at |R|\* leaves it not yet done hiding (HF > 0), and then Lost cannot be compared either. Separating
the two modes is the only way to keep each comparison meaningful.

---

## A8. Wilcoxon conventions

**Proposal.**

| Item | Value |
|---|---|
| Difference exactly 0 | use **Pratt** (kept when ranking), not the original Wilcoxon (dropped) |
| Tied ranks | average rank |
| Effect size | rank-biserial = (W⁺ − W⁻)/(W⁺ + W⁻) |
| Significance level | α = 0.05, one-sided |

**Grounds.** Lost is a small integer, so there will be many pairs with a difference of 0. The original Wilcoxon
throws them away, losing the information "the two methods are equal" and inflating the effect size. Pratt keeps them.

---

## A9. The exclusion rule for failed instances

**Proposal.**

| Situation | Handling |
|---|---|
| A method still has HF > 0 | **drop the pair** from Wilcoxon, count and report separately |
| Time limit exceeded | drop the pair, record code OT, report the number of cases |
| Out of memory | drop the pair, record code OOM, report the number of cases |
| Share of excluded instances > 10% | **no conclusion** on H2, and say why |

**Grounds.** Lost can be compared only when both methods have finished hiding; assigning a penalty value to a
case with HF > 0 is fabricating data. The 10% threshold is there to prevent exclusion from silently turning the
sample into a selected sample.

---

## A10. The number of reruns of the randomized methods

**Proposal.** HH, HR, RH, RR and GA: **10 runs** with 10 pre-declared seeds (2026 to 2035), taking the
**median** Lost and |R| as the value of that method on that instance.

**Grounds.** This is a **machine-invariant** quantity, so the repetition table of §11.6 does not apply
— that table is for timing. The observed variation of RR is large (|R| = 31 and 151 on two different
sensitive sets of the same configuration), so one run is not enough.

---

## A11. The grid of σ, ℓ_max, gaps and |𝒮|

**Proposal.**

| Parameter | Value |
|---|---|
| ℓ_max | **4** for every dataset |
| Gap | [0,1] and [0,3] for every dataset; plus [0,10] for dna and msnbc |
| σ dna | **2500, 4000** |
| σ msnbc | **1000, 2000, 5000** |
| σ leviathan / bible | 500 / 2000 |
| σ sign / bms1 / fifa | 200 / 100 / 3000 |
| \|𝒮\| | {1, 5, 10, 20}, 20 sets per cell (as in §11.5) |

**Grounds.**

*ℓ_max = 4* is required to match §11.5, which says the sensitive sets draw patterns **of length 2–4**. With
ℓ_max = 3, patterns of length 4 are not in F_σ and so cannot be drawn — this is an **existing
inconsistency** between §11.5 and every SC2 run done so far. The measured cost:

| Dataset | σ | \|F\| at ℓ_max=3 | seconds | \|F\| at ℓ_max=4 | seconds |
|---|---|---|---|---|---|
| dna | 2500 | 51 | 0.7 | 67 | 1.2 |
| msnbc | 2000 | 327 | 8.7 | 556 | 20.1 |
| leviathan | 500 | 106 | 53.6 | 108 | 60.6 |
| bms1 | 100 | 652 | 6.7 | 662 | 7.4 |
| sign | 200 | 95 | 2.8 | 95 | 2.7 |
| fifa | 3000 | 78 | 123.0 | 79 | 123.6 |

Cheap on **all six datasets**, with no exception. On sign no pattern of length 4 is frequent (95 → 95),
fifa adds only one pattern and costs 0.6 seconds more, leviathan adds two patterns. Reason: the mining cost is
concentrated at **level 2**, where the number of candidates is |L₁| × |Σ|; by level 4 the projection has already
narrowed the set of sequences to scan, so the extension is almost free. The most expensive case is fifa with
|Σ| = 2990, and that cost is already paid at ℓ_max = 3.

In other words, the argument "ℓ_max = 3 for cheapness" does not stand: it is not significantly cheaper, yet it
makes the length-4 patterns of §11.5 impossible to draw.

*σ for dna must be ≥ 2500.* With |Σ| = 4 and ℓ_max = 3, the pattern space has only 4 + 16 + 64 = 84 patterns,
and at σ = 1000, |F_σ| = **84**, i.e. **every** pattern is frequent. When F_σ is the whole space,
Ghost cannot differ from 0 and Lost loses its comparative meaning. At σ = 2500, |F_σ| = 51 out of 84, a proper
subset. This is a constraint that only shows up when measured, and cannot be derived from the design.

*General rule for choosing σ:* keep |F_σ| within [50, 700] **and** F_σ a proper subset of the
pattern space. Every σ proposed above satisfies this.

---

## A12. The ε threshold of B_ε

**Proposal.** **ε = 0.5** for every dataset. Add a run-time check: if B_ε is empty, **report
an error** rather than silently continuing.

**Grounds.** Measured |B_ε| by ε:

| Configuration | \|F_σ\| | ε=0.05 | ε=0.10 | ε=0.25 | ε=0.50 | ε=1.00 |
|---|---|---|---|---|---|---|
| dna [0,3] σ=1000 | 84 | 0 | **0** | 1 | 6 | 20 |
| dna [0,3] σ=2500 | 51 | 7 | 19 | 24 | 36 | 42 |
| dna [0,1] σ=1000 | 59 | 1 | 5 | 8 | 26 | 40 |
| msnbc [0,3] σ=1000 | 672 | 34 | 53 | 123 | 217 | 345 |
| msnbc [0,3] σ=2000 | 327 | 8 | 22 | 61 | 112 | 174 |
| msnbc [0,3] σ=5000 | 119 | 4 | 10 | 24 | 40 | 64 |
| leviathan [0,3] σ=500 | 106 | 6 | 11 | 26 | 42 | 66 |
| bms1 [0,3] σ=100 | 652 | 34 | 81 | 159 | 248 | 349 |

ε = 0.10 gives an **empty** B_ε on dna [0,3] σ=1000. Then step 4 assigns weight 0 to every position and
CUT-HIDE silently falls back to the uniform-weight variant — i.e. the variant being compared against in SC7 is
the very variant being run, without anyone knowing. This is the most dangerous kind of silent error.

ε = 1.00 pulls 24–54% of F_σ into the band; then the weight 1/(sup − σ + 1) is almost uniform and the phrase
"patterns near the border" loses its meaning.

ε = 0.50 gives |B_ε| from 6 to 248, i.e. 7–38% of F_σ, and is **non-empty in every configuration measured**.

**Cost.** A large B_ε makes step 3 more expensive; §12 already anticipated this risk and §11.1 requires SC6 to
report the cost broken down by phase. With ε = 0.5, the largest |B_ε| measured is 248 (bms1, a control dataset).

---

## A13. The sup_NO percentiles that draw the sensitive sets

**Proposal.** Five percentiles of sup_NO over the candidates: **p10, p30, p50, p70, p90**; **4 sets**
per percentile → exactly 20 sets per cell, as §11.5 requires. The candidates are the patterns of length 2–4 in F_σ.

**Grounds.** §11.5 already locked 20 sets per cell; five evenly spaced percentiles are the simplest split that gives
exactly 20 and covers both ends of the distribution. The measurements show that the sup_NO distribution is very
skewed (msnbc σ=2000: p10 = 2313, p50 = 3800, p90 = 13368, maximum 115180), so sampling only at the upper end
would give a wrong picture.

---

## A14. The scope of μ(P)

**Proposal.** Keep **μ(P) = 1** as the main scope of the paper. Patterns with repeated items are reported in a
separate section, using bound P3 and the exact solver on small instances, and the hardness is stated as an open question.

**Grounds.** On dna, the ρ_rep of patterns **without repeated items** is 0.806, **higher than** that of patterns
with repeated items, 0.773. This means that keeping the scope narrow no longer costs giving up the high-repetition
data regime — which is exactly why the round of additional data was done. In the scope μ = 1, P1, P2, P6(b) and
the exact solution all hold.

**In addition.** Node weights when μ ≥ 2 keep the current implementation (assign the weight of a position to every
node of that position) and are **declared as an implementation choice**, since P1 and P2 do not apply there in the
first place.

**Cost.** It must be stated in the limitations section that on msnbc it is the repeated-item class that repeats more
(0.434 against 0.218), with the exact numbers, so as not to be read as evasion.

---

## A15. Several sensitive patterns under a database threshold

**Proposal.** **Linear programming rounding** as the main option. The hybrid GA is the fallback, used only
if LP cannot be implemented in time. If neither is ready in time, **narrow the scope of the paper to |𝒮| = 1** and
state it clearly, instead of bringing in GA with its parameters not yet locked.

**Grounds.** LP rounding has an **ℓ_max factor guarantee**, already in the list of contributions in §7. GA has
no guarantee and needs at least four parameters locked in advance (population size, number of generations,
crossover rate, mutation rate), i.e. it inflates the decision surface exactly when it needs narrowing.

---

## A16. "Small instance" and the ILP variant taken as the reference

**Proposal.** "Small instance" = an instance with **≤ 64 candidate positions** that needs **≤ 12 masks** —
exactly the two constants `MAX_CANDIDATES` and `MAX_MASKS` implemented in `IlpExact`. Write these two numbers
into the protocol table. SC3 takes **`exact-minR-minLost`** as the optimal reference.

**Grounds.** Defining "small" by exactly the limits the code enforces leaves no gap between the claim
and the implementation. The `minLost` variant is chosen because the second objective of §2 is the number of lost patterns;
`exact-minR` only answers "how many masks are needed", a question P1 already answers with a formula.

---

## A17. Stamping the code version into the result files

**Proposal.** Add a wrapper script that sets `HIDENOSP_REVISION = git rev-parse HEAD` before calling
`Runner`. **Rerun** every run whose numbers go into the paper. The existing numbers (stamped `unversioned`) are
kept as exploratory numbers.

**Grounds.** Every current result file carries the stamp `unversioned`, so it cannot be traced back to the commit
that produced it. Rule 2.6.8 requires a run identifier stamp; that stamp is missing exactly its most
important component.

---

## A23. Bundling everything into one lock

**Proposal.** Write all of A1–A17 into `preregistration.md` and `protocol.yaml` in **one single
commit**, before the first SC3 run without `--probe`. The same commit turns the go/no-go conclusion
of §12a from "provisional" into official.

---

## A24. Tier (a) baselines when a sensitive set holds more than one pattern

**Proposal.** Extend `RandomMasking`: choose a random position in the **union** of the Nettrees of all
sensitive patterns, stopping when every pattern reaches the threshold. Keep the four combinations HH/HR/RH/RR unchanged.

**Grounds.** Currently `RandomMasking` throws an error when |𝒮| > 1, so the cells |𝒮| ∈ {5, 10, 20} of §11.5 have no
tier (a) control. The extension above is the simplest one that still keeps the spirit of a "trivial
control".

---

## A25. Checking SBSH against [R3]

**Proposal.** Open [R3] and check Algorithms 2 and 3. Until the check is done, **put no SBSH
number into the paper**. If the check cannot be done, drop SBSH from the table and state the reason, and at the
same time change the Holm family in A6 from 4 down to 2.

**Grounds.** The current implementation was rebuilt from the description in the research design, not from the
original algorithm. Three points need checking: what the grey label covers, whether the radius is counted in arcs or
in positions, and how C-PA and C-SA differ from plain Replace. Printing a number labelled SBSH before the check
attributes to [R3] a result that may not be theirs.

---

## Summary table to copy into preregistration

| Code | Parameter | Proposed value |
|---|---|---|
| A1 | δ_L (on relative Lost) | 0.02 |
| A2 | unit the thresholds apply to | configuration (dataset, σ, gap) |
| A2 | summary statistic | median |
| A2 | ρ_rep main group / degenerate | ≥ 0.20 / ≤ 0.05 |
| A2 | β effective / degenerate | ≥ 0.20 / ≤ 0.05 |
| A4 | equivalence margin Lost / SupDist | ±0.02 / ±0.01 |
| A5 | H3: above 1 / near 1 | r ≥ 1.10 / r ≤ 1.05 |
| A6 | Holm family | 4 tier (b) baselines |
| A7 | H2 pairing | run until HF = 0; test only on instances with the same \|R\| |
| A8 | difference of 0 / tied ranks / effect size | Pratt / average rank / rank-biserial |
| A9 | exclusion | HF>0, OT, OOM drop the pair; >10% means no conclusion |
| A10 | reruns of randomized methods | 10, seeds 2026–2035, take the median |
| A11 | ℓ_max | 4 |
| A11 | gap | [0,1], [0,3]; plus [0,10] for dna and msnbc |
| A11 | σ dna / msnbc | 2500, 4000 / 1000, 2000, 5000 |
| A12 | ε of B_ε | 0.5, with a check that B_ε is non-empty |
| A13 | sampling percentiles | p10, p30, p50, p70, p90; 4 sets per percentile |
| A14 | scope of μ(P) | 1 |
| A15 | several patterns, database threshold | LP rounding; GA as fallback |
| A16 | small instance / ILP reference | ≤64 candidates and ≤12 masks / exact-minR-minLost |
| A17 | code version | HIDENOSP_REVISION from git, rerun the numbers that go into the paper |
