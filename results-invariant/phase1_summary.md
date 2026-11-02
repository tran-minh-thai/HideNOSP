# Phase 1: SC1 and SC2 on real data

> **Language.** This file is the English translation of the Vietnamese original as of commit `5ffe329`
> (2026-11-02), which stays in the git history. Values, dates and decisions are unchanged, file names
> are those after the renaming of 2026-11-02; decimal commas are written as decimal points. This file
> was named `results-invariant/giai_doan_1_tom_tat.md` until 2026-11-02.

> **Rerun on 2026-09-12 at ℓ_max = 4**, after `protocol.yaml` locked that value, and entirely
> through `tools/run.py`, so every file carries the version stamp `2f63b567ccf1`. The replacement ρ_rep
> and β table is in `dataset_survey.md`. The ℓ_max = 3 set of numbers below is kept for comparison; it
> **no longer matches the protocol** because §11.4 row 5 requires every method to use the same ℓ_max.

Run date: 2026-09-12. Machine: development machine (every number here is machine-invariant, so that
has no effect). Code: see the identifier stamp at the head of each CSV file in this folder.

Phase 1 under §12b is **implementation, SC1–SC2, locking `config/preregistration.md`**. The first two
steps are done; the third still awaits the author's decision, and §4 below states which decision.

---

## 1. SC1: implementation check

`Runner SC1 --trials=1500 --seed=2026`, file `SC1_20260912-*.csv`.

| Check | Instances | Mismatches |
|---|---|---|
| L0: maximum flow = exhaustive sup_NO | 1,500 | 0 |
| L0: minimum cut size = sup_NO | 1,500 | 0 |
| P1: minimum number of masks = F − t | 1,069 | 0 |
| L5: vital node ⇔ masking decreases sup_NO | 4,079 | 0 |
| Incremental update = rebuild | 1,391 | 0 |
| P5(a): masking never increases sup_NO | 7,394 | 0 |
| P5(b): deletion is monotone when g⁺ = ∞ | 1,719 | 0 |
| Mining `F_σ` with pruning = exhaustive | 40 | 0 |

Accompanying observation (not a check): deletion with a finite g⁺ **increases** sup_NO in 54 of 5,675
cases, exactly as P5(b) predicts. This is the empirical ground for choosing masking as the main edit operation.

**Conclusion: SC1 green.** Every later scenario is allowed to run.

---

## 2. SC2: data regime

Four public datasets, downloaded with `tools/fetch_datasets.py`, checksums recorded in `datasets/provenance.tsv`.
Ten configurations (dataset, σ, gap), each configuration sampling sensitive patterns by percentiles of sup_NO.

| Dataset | σ | Gap | ρ_rep min/mean/max | β min/mean/max | max \|R\|\*/Σ\|S\| |
|---|---|---|---|---|---|
| bms1 | 100 | [0,3] | 0.000 / 0.000 / 0.000 | 0.000 / 0.117 / 0.522 | 0.0024 |
| bms1 | 300 | [0,3] | 0.000 / 0.000 / 0.000 | 0.000 / 0.071 / 0.366 | 0.0032 |
| fifa | 3000 | [0,3] | 0.005 / 0.008 / 0.013 | 0.185 / 0.289 / 0.467 | 0.0025 |
| sign | 100 | [0,3] | 0.000 / 0.000 / 0.000 | 0.010 / 0.256 / 0.534 | 0.0080 |
| sign | 200 | [0,3] | 0.000 / 0.000 / 0.000 | 0.000 / 0.163 / 0.339 | 0.0054 |
| msnbc | 1000 | [0,3] | 0.064 / 0.190 / 0.470 | 0.111 / 0.311 / 0.445 | 0.0231 |
| msnbc | 2000 | [0,1] | 0.085 / 0.321 / 0.524 | 0.146 / 0.432 / 0.618 | 0.0536 |
| msnbc | 2000 | [0,3] | 0.106 / 0.293 / 0.575 | 0.047 / 0.232 / 0.319 | 0.0374 |
| msnbc | 2000 | [0,10] | 0.158 / 0.415 / 0.750 | 0.010 / 0.043 / 0.077 | 0.2056 |
| msnbc | 5000 | [0,3] | 0.195 / 0.395 / 0.750 | 0.080 / 0.173 / 0.271 | 0.1836 |

### Reading the table

**Only MSNBC qualifies for the main group.** SIGN and BMS1 have ρ_rep exactly 0 in every sampled
pattern: on those two datasets sup_NO degenerates completely into sequence-based support, so they say
nothing about the paper's contribution. FIFA has ρ_rep of about 0.008, i.e. it is also degenerate on
patterns without repeated items. BMS1 is degenerate in β as well (mean 0.117, many patterns equal to
0), exactly the double-control role that §11.2 planned for it.

This is exactly the risk §12 anticipated: "Real data has low ρ_rep, making sup_NO degenerate". It has
happened, and it happened with three of the four datasets.

**β and ρ_rep pull in opposite directions with the gap width.** On MSNBC at σ = 2000: widening [0,1] to
[0,10] raises the mean ρ_rep from 0.321 to 0.415 but drops β from 0.432 to 0.043. The wider the gap,
the more a pattern can repeat, but the more the constraint loses its effect. The configuration that
gives both quantities at a usable level is **[0,3]**, and [0,1] if a high β is wanted.

**The higher σ, the higher ρ_rep.** MSNBC [0,3]: σ = 1000 gives a mean ρ_rep of 0.190; σ = 5000 gives
0.395. The more frequent a pattern, the more it repeats within each sequence.

---

## 3. A tension to resolve before writing the paper

Splitting the sampled patterns on MSNBC by μ(P):

| Dataset | Patterns without repeated items (μ = 1) | | Repeated-item patterns (μ ≥ 2) | |
|---|---|---|---|---|
| | n | mean ρ_rep | n | mean ρ_rep |
| msnbc | 31 | 0.218 | 29 | **0.434** |
| fifa | 10 | 0.008 | 0 | — |
| sign | 20 | 0.000 | 0 | — |
| bms1 | 20 | 0.000 | 0 | — |

On MSNBC, repeated-item patterns have **twice** the repetition rate of patterns without repeated items
(0.434 against 0.218; maximum 0.750 against 0.555). In addition, the only pattern with a very high
ρ_rep found on FIFA is `[17, 17, 17]`, with sup_NO 4,999 against sup_seq 256, i.e. ρ_rep = 0.949 — also
a repeated-item pattern.

In other words: **the data regime in which sup_NO really differs from sup_seq is concentrated in exactly
the class of patterns where the paper's theory is weakest.** P1 and P2 hold only when μ(P) = 1; for
μ(P) ≥ 2 there are only the two bounds of P3, and §8.1 records the hardness of that case as an open
question. The paper must not evade this point. Three ways to handle it, to be chosen before running
SC3:

1. Keep the scope at μ(P) = 1, stating clearly in the limitations section that this is the class of
   patterns with the lower repetition rate, with exactly the numbers 0.218 against 0.434 above.
2. Open the scope to μ(P) ≥ 2, using the bound P3 and the exact solver on small instances to measure
   the distance to the optimum, accepting that there is no exact result for that class.
3. Add a dataset with a high repetition rate **also for patterns without repeated items**, namely the
   DNA sequences that §11.2 already planned and that the NOSEP line [R1] uses. This is the only way to
   make the main group wider than one dataset.

> **Update the same day: option 3 has been done, and it also removes this tension.** The genome dataset
> MT745584 (498 sequences × 60, |Σ| = 4) gives a mean ρ_rep of 0.786, and more importantly, ρ_rep of the
> patterns **without repeated items** there is 0.806 — **higher** than the repeated-item patterns
> (0.773), i.e. exactly the reverse of MSNBC. Keeping the scope at μ(P) = 1 no longer has to be paid for
> by giving up the high-repetition data regime, so option 1 becomes the dominant choice. Numbers and the
> proposed role for each dataset: `dataset_survey.md`.

---

## 4. Remaining work of phase 1

| Task | Status |
|---|---|
| SC1 green | done |
| SC2 on four public datasets | done |
| Obtain the DNA dataset and run SC2 on it | **done**, see `dataset_survey.md`; the result reverses the conclusion of section 3 |
| Obtain the trucks dataset of [R20] | no public source found yet |
| Lock the ρ_rep and β grouping thresholds in `config/preregistration.md` | **done** 2026-09-12: ≥0.20 main group, ≤0.05 degenerate, applied per configuration |
| Lock δ_L and the equivalence margins | **done** 2026-09-12: δ_L = 0.02 on relative Lost; margins ±0.02 and ±0.01 |
| Decide the μ(P) scope per section 3 | **done** 2026-09-12: keep μ(P) = 1 as the main scope |

`config/preregistration.md` was locked and committed on 2026-09-12, so SC3 is allowed to run to produce
numbers for the paper — after the three technical tasks recorded in §7 of that file are done.

---

## 5. Source files

In this folder: `SC1_20260912-*.csv` and the files `SC2_20260912-*.csv` (ten configurations of the first round, plus six configurations of the round that added datasets). Each file carries the run
identifier stamp in its name and in the comment header of the file. No file has a time or memory
column, as the rule of `results-invariant/` requires.
