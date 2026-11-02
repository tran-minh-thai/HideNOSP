# Comparison of §8.2 with run batch 20260912-095144

> **Language.** This file is the English translation of the Vietnamese original as of commit `5ffe329`
> (2026-11-02), which stays in the git history. Values, dates and decisions are unchanged, file names
> are those after the renaming of 2026-11-02; decimal commas are written as decimal points. This file
> was named `results-probe/doi_chieu_82_20260912-095144.md` until 2026-11-02.

This batch reruns all probe measurements of §8.2 of the research design, so that each number in that
table has a source file alongside it. These are **probe measurements**, not put into the paper as
experimental results.

## Environment

| Item | Value |
|---|---|
| Python | 3.14.6 |
| numpy | 2.5.0 (used only by `sideeffect.py`) |
| Machine | development machine, not the declared measuring machine |
| How it was run | `kiemtra_v2.py` at the repository root; the four other scripts run from inside `probe/`; sequentially, `timing.py` run last while the machine was idle |
| Exit code | all five scripts: 0 |

Written to a local disk and then copied here. Do not write directly with `>>` onto the Google Drive
disk: the virtual disk reports the old file size, so the append overwrites the middle of the file;
this happened once in the previous batch.

## Comparison table

| Result of §8.2 | Number in §8.2 | File of this batch | Rerun number | Match |
|---|---|---|---|---|
| L0 | 1,200 instances, match on every instance | `kiemtra_v2` | 1200 instances, 0 mismatches | yes |
| L0 extended (repeated items, m=1, g⁺=∞) | 1,500 instances | `kiemtra_v2` | 1500 instances, 0 mismatches | yes |
| L5 | 1,683 nodes | `kiemtra_v2` | 1683 nodes, 0 mismatches | yes |
| P1 | 912 pairs, 0 violations | `kiemtra_v2` | 912 pairs, 0 violations | yes |
| P2 | 2,220 valid sets | `kiemtra_v2` | 2220 sets, 0 outside the minimum cut | yes |
| Corollary of L5 (greedy by vital nodes) | 912 runs | `kiemtra_v2` | 912 runs, 0 using more than F − t | yes |
| Corollary of P1c, every tie-breaking | 21,016 sequences, excess in 18 | `kiemtra_v2` | 21016 sequences, 18 | yes |
| Corollary of P1c, smallest-position tie-breaking | 54 of 10,366 | `kiemtra_v2` | 10366 sequences, 54 | yes |
| P3 | 152 repeated-item pairs, lower bound reached in 17 | `kiemtra_v2` | 152 pairs, 17 | yes |
| P6(c) H1 | not optimal 436 times | `kiemtra_v2` | H1 436 | yes |
| P6(c) H2 | not optimal 120 times | `kiemtra_v2` | H2 120 | yes |
| P6(d) λ sweep over weight values only | not optimal 2 times | `kiemtra_v2` | 2 | yes |
| P6(d) λ sweep with exact maximum search | not optimal 0 times | `kiemtra_v2` | 0 | yes |
| P6(d) certificate ⌈LB⌉ = UB | 14,862 / 14,864 of 14,867 | `kiemtra_v2` | 14862 / 14864 of 14867 | yes |
| P7 | 5,931 curves F ≥ 3, 3 not convex | `kiemtra_v2` | 5931, 3; example c = (0, 1, 4, 9, 12) | yes |
| Old problem, one pattern, global | 293 databases, HHA excess in 24, mean 1.0%, maximum 33.3% | `oldprob` | 293, 24, 1.0%, 33.3% | yes |
| Old problem, multiple patterns | 1,214 sequences; MM optimal 97.6%, maximum ratio 1.67; LP 99.9%, maximum 1.50; integrality gap 1.001 | `oldprob` | identical | yes |
| Old problem, DBSH | 400 databases, equal to the optimum in all 400; Cost excess in 161 of 10,643 | `dbsh` | 400, 0; 161 / 10643 | yes |
| Old problem, side effects | HHA loses 6.02; exact cut 5.70; weighted λ=1 1.95; masking 7.9 / 7.8 / 8.4 | `sideeffect` | identical | yes |
| P5 | v0.1: 1,500 deletions, finite g⁺ 16 cases of increase | — | **cannot be rerun** | see below |
| Index, time, n = 400 | 1.91 s / 0.023 s / 0.005 s | `timing` | 1.63 s / 0.021 s / 0.005 s | machine-dependent |

## Two points that need to be stated clearly

**P5.** The numbers of §8.2 were produced by `kiemtra_menh_de.py` (v0.1), and that script is no
longer in the repository, so they cannot be rerun. `kiemtra_v2.py` has no P5 check. That role is now
held by SC1 on the Java side: `Runner SC1 --trials=1500 --seed=2026` gives 7,394 maskings, none of
which increases sup_NO (in agreement with P5(a)), 1,719 deletions with g⁺ = ∞, none of which increases
it, and 5,675 deletions with finite g⁺, of which 54 increase it (in agreement with P5(b)). The conclusion
does not change from v0.1; only the sample size and the generator differ. File:
`../results-invariant/SC1_20260912-094959_dev_unversioned_s2026.csv`.

**λ sweep over weight values only.** The number 2 of §8.2 was originally not printed anywhere in
`kiemtra_v2.py`: `weighted_stress` only computes the upper bound from the cuts of the version with
maximum search. This batch adds the count of the upper bound of the version that sweeps only the
weight values, and it gives exactly 2. The addition is purely additive: every other number is
identical to the previous batch byte for byte.

**Time.** `timing.py` prints seconds, that is, machine-dependent numbers. It lives in
`results-probe/` and not in `results/`, and is never printed in the paper. The new ratio is
1.63 / 0.021 ≈ 78 times, still consistent with the sentence "about 80 times faster at n = 400" in §7
of the research design. To get timing numbers for the paper, run SC6 on the declared measuring machine.
