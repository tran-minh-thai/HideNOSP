# Dataset survey: inheritance from related work, and additions by measured properties

> **Language.** This file is the English translation of the Vietnamese original as of commit `5ffe329`
> (2026-11-02), which stays in the git history. Values, dates, file names and decisions are unchanged;
> decimal commas are written as decimal points.

Date: 2026-09-12. Every ρ_rep and β number below is **measured by the project's own SC2**, not copied
from other people's descriptions (§11.2). Source files: `SC2_20260912-*.csv` in this folder.

This work is done in two steps, in this order: (1) reuse the datasets that related work used, so that
the paper's numbers can be placed next to published numbers; (2) add datasets chosen by the properties
that the paper's contribution needs.

---

## 1. Which datasets related work used

| Work | Dataset | Confidence level |
|---|---|---|
| [R1] NOSEP, Wu et al. 2018 — the very paper that defines sup_NO | **biological DNA sequences**, **time series** data, the **Gazelle** dataset | Verified by public lookup; Gazelle is BMS-WebView |
| [R20], [R2] the Abul line | **trucks** (truck trajectories discretised on a 10×10 grid, 273 sequences) | Recorded in the research design, at the full-text reading level |
| [R3] Gkoulalas-Divanis and Loukides 2011 | **MSN**, **BMS-WebView-1**, **TRUCKS** | Recorded in the research design (§11.1 row SC8), at the full-text reading level |
| Common benchmarks of sequential pattern mining | MSNBC, BMS-WebView, FIFA, SIGN, Bible, Leviathan, Kosarak | Public catalogue of SPMF |

**Stating the limits.** A web lookup verified the list of [R1]. The lists of [R20] and [R3] are taken
from the research design, where the author read the full text of those two papers personally; a public
lookup could **not** reconstruct them, so they must not be recorded as "independently verified". Before
writing the paper, the two original papers must be reopened to copy the exact version name and the ψ
threshold.

**Two datasets not yet obtained.** No public source for `trucks` has been found yet. SPMF's `BIKE`
(sequences of bike stations over time) is the closest public version in the **form** of the data, but it
is **not** the same dataset, and the table below shows that it also fails on properties. The time series
that [R1] used have not been obtained; if needed, discretise a dataset from the UCR archive.

---

## 2. Table of data regimes, measured by SC2

The last column splits by μ(P): the mean ρ_rep of patterns **without repeated items** (μ = 1, the class
to which P1 and P2 apply) against **repeated-item** patterns (μ ≥ 2, the class with only the P3 bounds).

> **Remeasured on 2026-09-12 at ℓ_max = 4.** The set of numbers below was measured at ℓ_max = 3 and as a
> **mean**. After `protocol.yaml` locked ℓ_max = 4, all of SC2 was rerun on 25 configurations and taken
> as the **median** under the locked convention. The replacement table is immediately below; the old
> table is kept for comparison. The grouping conclusion is **unchanged**; the details that change are
> stated in what follows.

### Replacement table: ℓ_max = 4, median, 25 configurations

| Dataset | Configurations | Patterns | Median ρ_rep (min–max) | Median β (min–max) | ρ_rep μ=1 / μ≥2 |
|---|---|---|---|---|---|
| **dna_mt745584** | 6 | 84 | **0.873** (0.803–0.940) | 0.223 (0.016–0.681) | 0.845 / 0.878 |
| **msnbc** | 9 | 180 | **0.324** (0.021–0.684) | 0.210 (0.005–0.736) | 0.218 / 0.364 |
| leviathan | 2 | 38 | 0.130 (0.068–0.964) | **0.574** (0.018–0.825) | 0.112 / 0.360 |
| bible | 2 | 40 | 0.127 (0.001–0.408) | **0.549** (0.001–0.828) | 0.117 / 0.238 |
| fifa | 2 | 28 | 0.008 (0.003–0.960) | 0.316 (0.055–0.467) | 0.008 / **0.952** |
| sign | 2 | 23 | 0.000 | 0.172 (0.000–0.480) | 0.000 / — |
| bms1 | 2 | 40 | 0.000 | 0.053 (0.000–0.582) | 0.000 / — |

Three things change compared with the ℓ_max = 3 set of numbers:

1. **The grouping does not flip.** The two locked thresholds (0.20 and 0.05) still sit in sparse gaps of
   the distribution: the gap 0.010 → 0.120 of width 0.110, and the gap 0.181 → 0.265 of width 0.084.
2. **msnbc σ = 1000 drops into the intermediate group** (ρ_rep 0.172–0.181 < 0.20). MSNBC is a
   main-group dataset only from σ = 2000 upwards. This detail shows up only because the threshold is
   applied per **configuration**.
3. **The direction of the μ(P) comparison reverses.** At ℓ_max = 3 with the mean, dna gives μ=1 (0.806)
   higher than μ≥2 (0.773). At ℓ_max = 4 with the median it is the reverse: 0.845 against 0.878. The
   conclusion to keep the scope μ(P) = 1 is **unchanged**, but the reason changes from "the class without
   repeated items repeats more" to "the two classes are nearly equal on the strongest dataset, a
   difference of 0.033". See the correction in `../config/preregistration.md` §1.
   One more new observation: on FIFA, repeated-item patterns reach a median ρ_rep of **0.952** against
   0.008 for patterns without repeated items — the whole repetition rate of that dataset sits in the
   repeated-item class.

### Old table, ℓ_max = 3, mean (kept for comparison)

| Dataset | Configurations | Patterns | ρ_rep min/mean/max | β min/mean/max | ρ_rep μ=1 / μ≥2 |
|---|---|---|---|---|---|
| **dna_mt745584** | 3 | 30 | **0.602 / 0.786 / 0.924** | 0.225 / 0.467 / 0.722 | **0.806 / 0.773** |
| **msnbc** | 5 | 60 | 0.064 / 0.323 / 0.750 | 0.010 / 0.238 / 0.618 | 0.218 / 0.434 |
| leviathan | 1 | 10 | 0.061 / 0.154 / 0.274 | 0.427 / 0.612 / **0.801** | 0.120 / 0.233 |
| bible | 1 | 10 | 0.023 / 0.153 / 0.248 | 0.194 / 0.500 / **0.850** | 0.153 / 0.152 |
| bike | 1 | 10 | 0.008 / 0.052 / 0.231 | 0.086 / 0.375 / 0.512 | 0.032 / 0.231 |
| fifa | 1 | 10 | 0.005 / 0.008 / 0.013 | 0.185 / 0.289 / 0.467 | 0.008 / — |
| sign | 2 | 20 | 0.000 / 0.000 / 0.000 | 0.000 / 0.209 / 0.534 | 0.000 / — |
| bms1 | 2 | 20 | 0.000 / 0.000 / 0.000 | 0.000 / 0.094 / 0.522 | 0.000 / — |

All eight datasets are single-element itemsets with a linearised share of 0, so they all lie within the
paper's data model (§8.3). One dataset was excluded right at the format-inspection step: SPMF's
**E-Shop** (24,026 sequences, mean length 62) looks very suitable but consists of **multi-element
itemsets**, outside the model.

---

## 3. Three things this table changes

**(a) The main group is no longer a single dataset.** Before this round only MSNBC qualified. Now the
genome dataset MT745584 is added, and it is **stronger than MSNBC at every percentile**: the minimum
ρ_rep of DNA (0.602) is even higher than the mean of MSNBC (0.323).

**(b) DNA is the only configuration giving high ρ_rep and high β at the same time.** On MSNBC the two
quantities pull in opposite directions with the gap width: widening [0,1] to [0,10] takes the mean ρ_rep
from 0.321 to 0.415 but drops β from 0.432 to 0.043. On DNA at gap [0,1], ρ_rep is still 0.602–0.836
while β reaches 0.420–0.722. That is the data regime in which both of the paper's assumptions are in
effect at the same time.

**(c) The μ(P) tension of phase 1 is removed.** On MSNBC, the repetition rate is concentrated in
repeated-item patterns (0.434 against 0.218), i.e. exactly the class to which P1 and P2 do **not** apply —
the paper is strongest where the theory is weakest. On DNA this is **reversed**: patterns without
repeated items have a mean ρ_rep of 0.806, even higher than the 0.773 of repeated-item patterns. That
means keeping the scope at μ(P) = 1, where P1, P2 and the exact solution all hold, **no longer has to be
paid for by giving up the high-repetition data regime**. The three options raised in section
3 of `giai_doan_1_tom_tat.md` now have one clearly dominant option: keep μ(P) = 1 as the main scope, and
bring MSNBC in as evidence that on other data it is the repeated-item class that repeats more, with
exactly the numbers.

---

## 4. Proposed role for each dataset

| Role | Dataset | Grounds |
|---|---|---|
| Main group | **dna_mt745584**, **msnbc** | Mean ρ_rep 0.786 and 0.323; both inherited: DNA is the data type of [R1], MSNBC is the MSN version of [R3] |
| Intermediate group | leviathan, bible | ρ_rep 0.15 but β 0.50–0.61: the gap constraint is strongly effective while sup_NO is nearly degenerate; shows that the two quantities are independent of each other |
| Degenerate control | sign, bms1 | ρ_rep exactly 0; bms1 is degenerate in β as well, and is the dataset [R3] used |
| Degenerate control | fifa | ρ_rep 0.008 |
| Not used | bike | ρ_rep 0.052, mean sequence length only 7.3; kept only to say that the closest public version to trucks was tried |
| Not used | e_shop | multi-element itemsets, outside the data model |

This is a **proposal**, not a decision: the ρ_rep and β grouping thresholds must still be locked in
`config/preregistration.md` by principle, before running SC3, and must not be inferred backwards from
the wish that a given dataset go into a given group.

---

## 5. Remaining work

| Task | Note |
|---|---|
| Reopen [R20] and [R3] to copy the exact name of the data version and the ψ threshold | Needed for SC8; a public lookup cannot replace this |
| Find a source for `trucks`, or state clearly that it could not be obtained | SC8 is planned to run on MSN, BMS-WebView-1 and TRUCKS |
| Consider adding one discretised time-series dataset | [R1] uses this type; the UCR archive is the usual source |
| Scan more σ values and gaps for dna, leviathan, bible | Each new dataset currently has only 1–3 configurations, against 5 for msnbc |
