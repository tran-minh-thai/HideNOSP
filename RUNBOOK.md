# RUNBOOK — HideNOSP code repository

> **Language.** This file is the English translation of the Vietnamese original as of commit `ea39da4`
> (2026-11-02), which stays in the git history. Section numbers, values, dates and decisions are
> unchanged, file names are those after the renaming of 2026-11-02; decimal commas are written as
> decimal points.

This repository implements the **Nettree cut index** and the **CUT-HIDE** algorithm of
`doc/THIET_KE_NGHIEN_CUU_AN_MAU_LAP.md`, together with the baselines and the scenarios SC1–SC8.
Every section name of the form §n below points to that design file.

**The design file lies OUTSIDE the git repository.** The repository consists only of `code/` (§11.8);
`doc/` and `paper/` are in the same project directory `HideNOSP Project/`, one level above this
repository. A cloned copy will not have them.

Language: source code, comments and documents, this file included, in English (the manuscript is submitted in
English, and the existing Python probe scripts are also commented in English).

---

## 1. Requirements and how to compile

- JDK 25 (the LTS release, Eclipse Temurin). Every machine uses the same JDK 25; `tools/run.py` refuses
  to run for numbers that go into the paper on any other Java version. Changed from JDK 17 on 2026-09-25.
  The machine-invariant numbers of phase 1 (JDK 17, Windows) were rerun with JDK 26 on macOS at the same
  commit `3e1811f` (before the history rewrite it was `2f63b56`, see `COMMIT_MAP.tsv`): 4 SC2
  configurations, 58 lines, identical after normalising line endings and path separators.
- Maven 3.9 or later. The `~/.m2` repository already contains junit 5.10.2, so it compiles in offline mode.

```bash
mvn -o -q compile
```

```bash
mvn -o test
```

`mvn test` runs `WorkedExamplesTest`, which contains the four hand-worked examples of the paper (§8.1,
§8.2, G3). If one of the four examples fails, then the **paper** is broken, not only the code: those
numbers are printed in the manuscript.

---

## 2. Running a scenario

```bash
java -cp target/classes hidenosp.exp.Runner
```

the command above prints the list of scenarios and the options of each one.

**For numbers that go into the paper, call through `tools/run.py`, not `Runner` directly:**

```bash
python tools/run.py SC1 --trials=1500 --seed=2026
```

This wrapper script recompiles, checks that the working tree is clean, then sets `HIDENOSP_REVISION` to
the commit sha before calling `Runner`. If `Runner` is called directly, the result file carries the stamp
`unversioned` and cannot be traced back to the commit that produced it, which violates rule 2.6.8 and what
is locked in `config/protocol.yaml`. Call it directly only for informal trial runs.

Example of a direct call for a quick look:

```bash
java -cp target/classes hidenosp.exp.Runner SC1 --trials=1200 --seed=7
```

```bash
java -cp target/classes hidenosp.exp.Runner SC2 --sigma=20 --gmax=2 --patterns=10
```

```bash
java -cp target/classes hidenosp.exp.Runner SC3 --sigma=15 --gmax=2 --setsPerStratum=1 --probe
```

Options shared by every scenario:

| Option | Default | Meaning |
|---|---|---|
| `--seed` | 2026 | Seed; goes into row 3 of the protocol table and into the run identifier |
| `--machine` | `dev` | Name of the declared measuring machine. A run tagged `dev` **may not** record timing measurements |
| `--root` | `.` | Directory that contains the three result directories |
| `--probe` | off | Sends the invariant file to `results-probe/` instead of `results-invariant/` |

`tools/run.py` alone accepts three further options of its own, which are not forwarded to `Runner`:

| Option | Default | Meaning |
|---|---|---|
| `--jvm=<flag>` | `-Xmx4g` | Flag for the JVM; can be repeated |
| `--allow-dirty` | off | Allows running while the working tree still has uncommitted changes |
| `--no-build` | off | Skips the recompile step |

If the working tree still has uncommitted changes, the script **refuses to run**, unless that run has
`--probe` (exploratory numbers, allowed) or you declare `--allow-dirty` explicitly. Both cases stamp the
suffix `-dirty`, so that a result file never wrongly carries a clean sha.

While no real dataset is available, SC2 and SC3 run on a small synthetic database built into the code.
**Every such run must have `--probe`**: those are exploratory measurements, not results (§8.2).

### Python probe scripts

`nettree_checks.py` and `probe/*.py` print to the screen. §8.2 requires those numbers to be in
`results-probe/`, so the output must be captured there instead of only being read on the screen:

```bash
python nettree_checks.py > results-probe/nettree_checks_$(date -u +%Y%m%d-%H%M%S).txt 2>&1
```

```bash
cd probe && python oldprob.py > ../results-probe/oldprob_$(date -u +%Y%m%d-%H%M%S).txt 2>&1
```

`probe/*.py` load `nettree_checks` through `sys.path.insert(0, '..')`, so they must be run from inside `probe/`.
`probe/sideeffect.py` needs `numpy`. The most recent rerun batch took about three minutes for `nettree_checks.py`
and a few seconds for the four remaining scripts; the results and the comparison table against §8.2 are in `results-probe/`.

Do not use `>>` to append to a file located on the Google Drive disk: the virtual disk reports the old file size,
so the append overwrites the middle of the file. Write to a local disk and then copy over, or use only `>`.

---

## 3. Running the phase 1 experiments

Per §12b, phase 1 is **implementation, SC1–SC2, locking `config/preregistration.md`**. It must be finished
before SC3, because SC2 decides which datasets go into the main group and which into the degenerate control
group, and that decision may not be changed after the comparison results have been seen [2.3.10].

### Step 0. Get the data

```bash
python tools/fetch_datasets.py
```

```bash
python tools/build_datasets.py
```

The second command builds the SPMF files from the raw data under `datasets/raw/` (three process event logs
from 4TU, 14 databases of HAUP-Hider and two protein files of NTP-Miner, `protein_sdb1` and `protein_sdb4`);
the rule that assigns a role to each field is at the top of `tools/build_datasets.py`. The six raw protein
files are nested, so only the two largest are built; each build prints their overlap with all six files.

**Choose σ for a new dataset** with the rule of `config/protocol.yaml` (|F_σ| in [50, 700] at every
gap, and F_σ is a proper subset of the pattern space), applied by program rather than read by
eye:

```bash
python tools/run.py --probe SIGMA --dataset=datasets/protein_sdb4.txt --format=SPMF --sigma=300,400,500,600,700,800,900,1000,1200 --gmin=0 --gmax=1,3 --maxPatternLength=4
```

The last line prints the σ values that satisfy the rule at every gap scanned; the file `SIGMA_*.csv` records each configuration.
`python tools/data_regime_tables.py sigma <file or directory>` prints a markdown table from those files, and
`python tools/data_regime_tables.py groups <file or directory>` prints the group table from the whole-pool SC2 files
(for reporting only; the locked table is still written by `preregistered_analysis.py --write-groups`).

Downloads MSNBC, SIGN, FIFA and BMS-WebView-1 from the public SPMF repository into `datasets/`, computes SHA-256
and writes `datasets/provenance.tsv`. Rerunning does not download again if the checksum still matches. To get one
dataset only, pass its name: `python tools/fetch_datasets.py msnbc`.

### Step 1. SC1 must be green first

```bash
python tools/run.py SC1 --trials=1500 --seed=2026
```

The only acceptable result is **0 mismatches**. SC1 compares maximum flow against exhaustive search, checks
P1, checks the L5 characterisation of vital nodes, compares incremental update against rebuilding, and checks P5.
Until it is green, every number after it is meaningless.

### Step 2. SC2 on each dataset and each σ

```bash
python tools/run.py SC2 --dataset=datasets/msnbc.txt --format=SPMF --sigma=2000 --gmin=0 --gmax=3 --maxPatternLength=4 --patterns=12
```

```bash
python tools/run.py SC2 --dataset=datasets/sign.txt --format=SPMF --sigma=200 --gmin=0 --gmax=3 --maxPatternLength=4 --patterns=10
```

```bash
python tools/run.py SC2 --dataset=datasets/bms1.txt --format=SPMF --sigma=100 --gmin=0 --gmax=3 --maxPatternLength=4 --patterns=10
```

```bash
python tools/run.py SC2 --dataset=datasets/fifa.txt --format=SPMF --sigma=3000 --gmin=0 --gmax=3 --maxPatternLength=4 --patterns=10
```

Scan further σ values and gap widths to see that ρ_rep and β change with the configuration, not with the dataset:
change `--sigma` and `--gmax`. Reference times on the development machine: SIGN a few seconds, BMS1 a few seconds,
MSNBC about 13 seconds, FIFA about two minutes (|Σ| = 2990 makes the level-2 candidates numerous).

Do **not** add `--probe` at this step: this is a real result, machine-invariant, and belongs in
`results-invariant/`. The `--probe` flag is only for trial runs on the synthetic data built into the code.

### Step 3. Read the SC2 table before locking the preregistration

For each dataset, look at two columns:

- **ρ_rep near 0** means that sup_NO degenerates into sequence-based support. That dataset is a **degenerate
  control**, and may not stand in the group that demonstrates the contribution [2.2.4].
- **β near 0** means that the gap constraint has no effect on that configuration.

Fill the grouping thresholds and δ_L into `config/preregistration.md`, commit that file (the git history is the
evidence of the date), and only then move on to SC3.

**From 2026-09-25 (preregistration section 10):** groups are computed over **all patterns without repeated items** of length 2–4 of
F_σ, i.e. the pool from which SC3 draws the sensitive sets. Run SC2 with `--profile=pool` for every configuration of
`protocol.yaml`; an SC2 file of a subsample is no longer used for grouping. After the official SC2 run on the measuring
machine, generate the group table once, then review it and commit it (the commit is the lock):

```bash
python tools/preregistered_analysis.py --sc2 results-invariant --write-groups
```

`tools/preregistered_analysis.py` (before 2026-09-26 named `tools/thong_ke.py`) compares each configuration
with `config/configuration_groups.tsv`, and tests H2 only on the main group.

**Done, on 2026-09-12.** `config/preregistration.md` and `config/protocol.yaml` have locked every
parameter that blocks SC3; the rationale for each number is in `config/group_a_proposed_values.md`. Three technical tasks must be
done before the SC3 run whose numbers go into the paper: attach `HIDENOSP_REVISION`, raise `--maxPatternLength` to 4,
and check that B_ε is non-empty.

### Step 4. Check the results-directory rule

```bash
python tools/check_results.py
```

---

## 4. The three result directories and their rules (§11.8)

| Directory | Contents | Who may write |
|---|---|---|
| `results-invariant/` | Lost, Ghost, \|R\|, HF, Nettree size | Any machine; timing or memory columns **forbidden** |
| `results/` | Wall-clock time, CPU time, peak memory | Only the declared measuring machine (`--machine=<name>`), and timing columns are required |
| `results-probe/` | Probes on the development machine, substitute data | Any machine; timing columns forbidden |

`hidenosp.io.ResultSink` enforces these three rules and throws an error as soon as the file is opened, without
waiting until the table is read. The reason for putting a mechanical check here: an error of the kind "timing
numbers measured on a laptop and then printed in the table as if measured on the declared machine" is the most
silent and the most costly kind of error.

Each file carries the run identifier (§2.6.8) in its name and in the comment header of the file:
UTC time, machine, code version (environment variable `HIDENOSP_REVISION`), seed, JDK version,
maximum heap size.

**`.partial` files.** Results are written to `<name>.csv.partial` and renamed to the final name only when the scenario runs
to the end (`CsvWriter.complete()`). A run interrupted midway — out of memory, stopped, machine restarted for an
update — leaves a `.partial` file: no reading tool accepts it, and `tools/check_results.py` reports an error. Delete that file
and rerun exactly the same command; do not commit. Before this mechanism existed, the SC2 runs that ran out of memory on MSNBC
left files carrying the final name but missing the last rows (884/885, 1,448/1,450 rows) — exactly the highest-support patterns.

---

## 5. Implementation status

### Implemented and checked

| Component | Class | Note |
|---|---|---|
| Nettree, shared position table | `core.Nettree` | §2, §9.1 |
| Maximum flow with node capacities (Dinic) | `core.flow.Dinic`, `core.flow.NettreeFlow` | L0 |
| Minimum cut | `NettreeFlow.minCut` | P1, P2 |
| Vital nodes: one flow + one SCC pass | `NettreeFlow.vitalNodes`, `core.flow.ResidualScc` | L5 |
| Incremental update after each mask | `NettreeFlow.maskPosition` | §9.1; SC1 compares it against rebuilding |
| Minimum-weight minimum cut | `NettreeFlow.minWeightMinCut` | P6(b), exact |
| λ sweep with lower bound and certificate | `hide.LambdaSweep` | P6(d) |
| Cost curve `c_S(k)` | `hide.CostCurve` | §9.2 step 5 |
| Allocation across sequences | `hide.Allocation` | P7, dynamic programming and also the greedy version for comparison in SC7 |
| Forward and backward counting | `index.PathCounts` | §9.1; replaces the position-based dynamic programming of [R20] |
| Combined index over several patterns | `index.NettreeCutIndex` | §9.1 |
| CUT-HIDE, one pattern | `hide.CutHide` | Exact when μ(P)=1; steps 1–6, 8 |
| CUT-HIDE, several patterns, HIDE-SEQ | `hide.CutHide` | Ratio greedy, step 7 |
| CUT-HIDE, several patterns, HIDE-DB | `hide.CutHide.runMultiPatternDatabase` | Ratio greedy over the whole database; **no** factor guarantee when σ > 1 |
| Primal–dual, factor ℓ_max and a lower bound for each instance | `hide.PrimalDualCover` | Only when σ = 1 (see section 7 items 7 and 9) |
| Metrics HF, Lost, Ghost, DistR, SupDist | `metrics.Metrics` | §2 |
| `F_σ(D)`, `B_ε`, `𝒮⁺` | `metrics.FrequentPatterns` | §9.2 step 3 |
| ρ_rep, β, \|R\|\* | `metrics.DataProfile` | §8.4 |
| Tier (a) baselines | `baseline.RandomMasking` (HH/HR/RH/RR), `baseline.OccurrenceCountHeuristic` | §11.3; runs with any \|𝒮\| |
| Empty-B_ε check reports an error | `hide.CutHide.requireNonEmptyBand` | Locked in `protocol.yaml` row 2 |
| Tier (b) baseline: HHA | `baseline.Hha` | [R20], [R2] Algorithm 1; reproduces exactly Example 2 of [R20] and Figures 1(d), 4(b) of [R3] (`HhaPaperExampleTest`); two ways of computing δ for SC6. Cross-check: `config/hha_check_against_r20.md` |
| Tier (b) baseline: DBSH | `baseline.Dbsh` | [R3], Algorithm 4 together with 1 and 3; reproduces exactly Figure 1(e), Figure 4(b), Example 1 (`DbshPaperExampleTest`). Shares Cost (`SymbolCover`) and Exclude (`Exclude`) with SBSH |
| Tier (b) baseline: SBSH | `baseline.Sbsh` | [R3], Algorithm 5 together with 1–3 and section 4.3; reproduces exactly Figure 1(f), Figure 4(b) and Example 3 of the paper (`SbshPaperExampleTest`). Points the paper leaves open: `config/sbsh_check_against_r3.md` section 6 |
| Tier (c) baseline: exact solver | `baseline.IlpExact` | Branch and bound, small instances; option to minimise lost patterns |
| Downloading datasets with checksums | `tools/fetch_datasets.py` | §11.2; only checks, does not overwrite the record; checks the MD5 published by the source |
| Building SPMF files from raw data (XES logs, HAUP-Hider databases, protein) | `tools/build_datasets.py` | Role-assignment rule written in the file; the build must match the record |
| σ selection rule, applied by program | `exp.SigmaScan` (scenario code `SIGMA`) | Not a scenario of the paper; machine-invariant file |
| Exact one-pattern solution, every μ, when g⁻ = 0 | `hide.FrontierDp` | Theorem P8 of the design: dynamic programming over the latest reachable node; two objective orders (`Objective`); checked against search over all subsets (`FrontierDpTest`) |
| Exact allocation across sequences | `hide.Allocation.solveExact`, `leastMasks` | Lexicographic in (number of masks, weight) or the reverse; O(K) memory for the values, one `short` per (sequence with support, total) for backtracking |
| CUT-HIDE one pattern, every μ | `hide.CutHide` with `CutHideConfig.Curves` | `FRONTIER_DP` (default), `FRONTIER_DP_WEIGHT_FIRST`, `LAMBDA_SWEEP` (control); SC3 method keys: `cut-hide`, `cut-hide-weight`, `cut-hide-sweep` |
| Method under test of H2 and H2-R (the preregistration, section 12) | `hide.AdaptiveCutHide` + `hide.MaskLocalSearch` | SC3 key `cut-hide-rw-ls`: adaptive weights over six rounds (round 0 is `cut-hide`), then local search per Lemma L9 (50 passes, 30 receiving sequences). Several sensitive patterns: the greedy of `cut-hide`. Key `cut-hide-rw`: adaptive weights only (ablation variant). Every official SC3 run must have `cut-hide-rw-ls`, `cut-hide-rw` and `cut-hide` in `--methods` |
| Pool of patterns with repeated items | `exp.SensitiveSampling.repeatedPool`; SC2 `--profile=repeated`; SC3 `--patternKind=repeated` | Exact \|R\|* for patterns with repeated items when g⁻ = 0 (`metrics.DataProfile`) |
| Exact convex Pareto front between \|R\| and weight | `hide.CutHide.paretoFront`, `FrontierDp.Order`; SC3 `--pareto=true` writes `SC3PARETO_<stamp>.csv` | Proposition P10 of the design; `tools/pareto_table.py` joins it with the SC3 file of the same run, counts the share of sets where the front dominates each method, checks that the first vertex is the `cut-hide` row |
| Evaluating many solutions of the same problem | `metrics.Metrics.Context.session` | Remembers the support of each (sequence, set of masked positions) already seen; rows identical to `evaluateMasking` (`MetricsSessionTest`) |
| Markdown tables from artifacts (σ rule, groups) | `tools/data_regime_tables.py` | So that summary documents embed generated tables, not tables copied by hand |
| Check that code is written only in English: letters with diacritics, words without diacritics, section and theorem numbers, file names as well | `tools/check_english.py` | Exit code 1 when there is a violation; self-check by fault injection: `tools/check_english_test.py`; word list remeasured with `--measure` |
| Sampling of sensitive sets by five percentile strata, without repetition | `exp.SensitiveSampling` | `preregistration.md` section 9 |
| SC1, SC2, SC3 | `exp.Sc1…`, `exp.Sc2…`, `exp.Sc3…` | Runnable |

### Not implemented (skeleton present, specification in the Javadoc of each class)

| Task | Where | Blocks which scenario |
|---|---|---|
| Genetic algorithm, plain version and hybrid version initialised from the cut | `baseline.GaHybrid` | SC3 (tier c) |
| SC4, SC5, SC6, SC7, SC8 | `exp.Sc4…` to `exp.Sc8…` | itself |
| Cross-check against NETGAP of [R1] | `exp.Sc1ImplementationCheck` | SC1 (the missing part) |
| Measuring the resident memory of the process | `util.MemoryProbe` | SC6 |

Quick search: `grep -rn "TODO(code)" src/`.

---

## 6. The 9-row protocol table

`config/protocol.yaml` is the filled-in version. Rule 2.6.2 requires filling it in **already in the first draft**, without waiting
until submission. The cells that the code can generate (JDK version, heap size, seed) are already printed at the
top of every result file; copy them from there.

`config/preregistration.md` locks the effect threshold δ_L and the test **before** SC3 runs
(rule 2.4.7). **Locked on 2026-09-12**; changing any number from here on must be a separate
commit stating the reason.

`config/open_decisions.md` lists **every** point the project still has to decide, grouped by
theme, separating the group that blocks SC3 from the group that blocks submission. Produced from a review of ten sources, then each
point was checked again; `tools/merge_decisions.py` merges the results into that document (before 2026-09-26 named
`tools/gop_quyet_dinh.py`; from that date the themes, the keywords and the document it generates are written in English).

`config/group_a_proposed_values.md` proposes **concrete values** for each point that blocks SC3, each value
with a measured rationale, the price to pay, and how to refute it. This is a version for approval, not yet the locked version:
locking means writing the numbers into the two files above and committing.

---

## 7. Implementation decisions that must be declared in the paper

These are the places where the code had to choose one way of doing things that the design file has not yet locked. Each line
needs one sentence in the manuscript or in the protocol table.

1. **Weight scale.** `w(i)` is a real number, whereas maximum flow needs integer capacities. `CostCurve.SCALE`
   = 10⁶ is the fixed-point unit. Two runs with different scales cannot be compared with each other.
2. **𝒮⁺.** The design writes "𝒮 together with all parent patterns" without locking the parent relation. The code reads it as the
   **subsequence** relation on item sequences (`FrequentPatterns.isSubsequence`), following the convention of [R3].
3. **Prefix pruning when mining `F_σ`.** Valid because `sup_NO(P·a, D) ≤ sup_NO(P, D)`: the prefixes of
   a family of pairwise non-overlapping occurrences are still pairwise non-overlapping. SC1 compares the pruned
   version against the exhaustive version on small instances.
4. **Semantics of ⊥.** `EventSequence.MASK = -1`, which matches no item, exactly as in §2.
5. **Linearising itemsets.** SPMF files with multi-element itemsets lie outside the data model of the
   paper (§8.3). `DatasetReader` sorts by increasing item code and returns the share of affected positions;
   rule 2.2.10 requires stating this share next to the dataset.
8. **Symbol clash: δ currently carries two meanings.** Rule 3.2.4 requires each symbol to be defined exactly
   once, but δ is currently used for two unrelated quantities:

   - **δ(i)** of [R20]: the number of matches that pass through position i, i.e. `|M_T| − |M_T masked at i|`, counting **every**
     embedding including overlapping ones. In the code it is `PathCounts.throughPosition`; `Hha.Delta` chooses the computation.
   - **δ_L**: the practical-effect threshold of H2, locked in `preregistration.md` = 0.02.

   It is better to keep δ(i) for [R20], because their notation must be kept when quoting them, and to change δ_L to another letter
   in the manuscript.

   Along with this, a point that is easy to misread needs one sentence in the paper: **δ(i) is not the drop in sup_NO.** The
   hand-worked example of G3 shows that the position with the largest δ does not lower sup_NO at all. δ counts every
   match; what the paper needs is the vital nodes of L5. That difference is exactly RQ3.

   And in linear-programming rounding, the approximation factor is the **largest constraint size** (m ≤ ℓ_max),
   **not** the degree of a variable (which is δ). Taking δ as the factor by mistake would give a false guarantee, because δ
   is unbounded whereas m is bounded.

7. **Scope of the ℓ_max factor guarantee.** §7 of the design lists among the contributions: "for several
   patterns (NP-hard) use linear-programming rounding **with an ℓ_max factor guarantee**". That guarantee only
   holds when **hiding completely**, i.e. σ = 1 with HIDE-DB or r = 1 with HIDE-SEQ. The reason:

   - When σ = 1, the condition is "no occurrence survives". Each occurrence L gives a cover
     constraint `Σ_{i ∈ L} x_i ≥ 1` with exactly m ≤ ℓ_max variables. Rounding every `x_i ≥ 1/ℓ_max` up to 1 gives
     a feasible solution with cost ≤ ℓ_max times the linear-programming solution, i.e. ≤ ℓ_max times the optimum.
     This is the standard covering rounding with bounded constraint size.
   - When σ > 1, the condition is "no **family of σ pairwise non-overlapping occurrences** survives".
     The corresponding constraint has up to σ·m variables, so the same rounding only gives the factor **σ·m**, not
     ℓ_max. The number of constraints also explodes, so lazy constraint generation would also be needed.

   So the sentence in §7 needs a clause limiting its scope before it goes into the paper. What is implemented for σ > 1 is
   the **ratio greedy** of step 7 of §9.2, with no factor guarantee, and SC3 measures its distance
   to the exact solver on small instances.

6. **Node weights when μ(P) ≥ 2.** A position carries several nodes; the code assigns the position weight to every node
   of that position. P1 and P2 do not apply in this case anyway, so this is an implementation choice
   and not a consequence of any proposition.

9. **Primal–dual instead of linear-programming rounding.** §7 calls this contribution "linear-programming
   rounding". What is implemented (`PrimalDualCover`) is the **primal–dual** method, which reaches
   the same factor ℓ_max through the same linear program but is a **different technique**, so the sentence in
   the paper must be corrected accordingly. Three reasons:

   - it needs no solver, keeping exactly the promise "no external dependency, built offline" in
     `config/protocol.yaml`;
   - the number of occurrences grows exponentially (which is exactly why `PathCounts` has to saturate), so the constraints can
     never all be written out. Primal–dual needs only **one** violated constraint per iteration,
     found by a shortest path on the Nettree with the slacks as node weights;
   - it produces a feasible dual, so **each instance carries its own lower bound**. The paper can report
     the ratio actually achieved and not only the worst case, in the same spirit as the certified bound pair
     of the λ sweep.

   **The reverse-deletion step is mandatory, not decoration.** With unit costs, the cheapest constraint always
   is a constraint none of whose positions is tight yet, so all m of its positions become tight in the same iteration: each iteration buys one
   unit of dual and spends m. Measured on three random databases, the ratio came out at exactly 2.000 — equal to the
   worst case — on **all three**. Adding the reverse-deletion step (remove redundant positions, the last chosen removed first) brings
   all three down to **1.000**, i.e. the cost equals the lower bound exactly, so the solution is **optimal**, and not
   merely within the factor. Deleting positions only lowers the cost and does not touch the dual, so the guarantee stays
   intact, and the resulting set is **minimal**: removing any position exposes an occurrence again
   (`PrimalDualCoverTest.theMaskSetIsMinimal` pins this).

   **The lower bound belongs to the instance, not to the method alone.** A feasible dual lower-bounds
   the linear-programming solution, and that solution lower-bounds the integer optimum, i.e. |R|* of **the instance
   itself**. So the `lower_bound` column is a lower bound for **every** method run on that row, not only
   primal–dual. This is the real gain: the exact solver only runs on small instances
   (64 candidates, 12 masks), whereas this lower bound runs on instances of any size, so SC3 can report
   the distance to the optimum of each baseline on the whole data. Valid only when σ = 1.

   **Trial measurement: it does not win on |R|.** On the synthetic database of SC3, with one sensitive pattern every
   method except RR reaches exactly the optimum — because P1c gives |R|* = sup_NO and every greedy reaches it, so
   σ = 1 with one pattern **distinguishes nothing**. With three sensitive patterns, five sets:

   | set | lower bound | primal–dual | ratio | best of the competitors | ratio |
   |----:|----------:|----------------:|------:|---------------------:|------:|
   | 0 | 103 | 109 | 1.058 | 107 | 1.039 |
   | 1 |  56 |  63 | 1.125 |  61 | 1.089 |
   | 2 | 110 | 112 | 1.018 | 113 | 1.027 |
   | 3 | 128 | 131 | 1.023 | 134 | 1.047 |
   | 4 |  81 |  93 | 1.148 |  87 | 1.074 |

   It wins 2 out of 5. So its contribution is **not** a smaller |R|, but (a) a provable
   guarantee, and (b) the certificate that allows saying "every method on this row is no more than 1.02
   to 1.15 times the optimum" — much tighter than the ℓ_max worst case. The paper must present it exactly that way; do not
   set it up as the best method and then let the table contradict it.

   Consequence for SC3: report **both** the provable factor (ℓ_max, worst case) **and** the
   measured ratio, and say clearly that the two differ.

10. **Unit cost, no weighted variant yet.** `PrimalDualCover` minimises |R|, i.e. objective
    (1) of §2. The argument does not look at the cost at all except to compare slacks, so every non-negative cost
    keeps the factor; but the weighted variant needs to connect B_ε exactly the way `CutHide` does, and until that is done
    one should not expose a switch that has no effect at all.
11. **How p of the Pratt-style Wilcoxon is computed.** The lock specifies Pratt and average ranks, but
    does **not** specify whether p is computed with the exact distribution or the normal approximation. This is an implementation choice
    that must be declared, because the three ways give three different numbers.

    In `scipy` 1.18, `method="exact"` under Pratt returns a **wrong** value: it uses the standard signed-rank
    distribution over all n observations, without taking into account that the ranks of differences equal to 0 may not take a
    sign. On an example of 12 observations with 4 zeros, enumerating all 2⁸ outcomes by hand gives
    p = 29/256 = 0.113281; `method="auto"` gives exactly that number, `"exact"` gives 0.211914 and `"approx"`
    gives 0.099370.

    So `tools/preregistered_analysis.py` locks `method="auto"`, and `tools/preregistered_analysis_test.py`
    pins it by
    hand enumeration rather than by trusting scipy. The paper must state "p computed with Pratt's exact conditional
    distribution when the sample size allows".

12. **Three gaps in the lock, found while implementing `preregistered_analysis.py`.** Not patched on our own, only recorded:

    - **|R| of CUT-HIDE larger than that of the baseline.** Section 5 defines two reporting modes: with the same |R|, test;
      if the baseline uses **more**, report separately. The case where CUT-HIDE uses more is **not** defined. Silently
      dropping it would be a bias in our favour, so the script counts that bin separately and prints a warning; the lock
      needs a line for this case.
    - **Codes OT and OOM have no place to be recorded.** Section 5 says to exclude instances carrying code OT or OOM, but SC3
      has no column in which to record them. The script reads the `status` column if it exists; if not, it must be added before
      the real run, otherwise that exclusion rule cannot be enforced.
    - **The method seed separated from the sampling seed.** Section 5 requires 10 seeds
      2026–2035, taking the median. Previously SC3 used **one** seed both for sampling the sensitive set
      and for running the method, so changing the seed also changed the sensitive set, and the ten runs could not
      be paired with each other. Now split into `--seed` and `--methodSeed`, with `--repeats` added.

---

## 8. Map from §9.2 to the code

| Step of §9.2 | Class, method |
|---|---|
| 1. Build NT(P,S) | `Nettree.build`, `NettreeCutIndex.add` |
| 2. Maximum flow F_S | `NettreeFlow` (available once built) |
| 3. Set of vital positions of `B_ε` | `NettreeFlow.vitalPositions`, `FrequentPatterns.nearThreshold` |
| 4. Position weights `w(i)` | `NettreeCutIndex.positionWeights`, `FrequentPatterns.weightFactors` |
| 5. Curve `c_S(k)` | `CostCurve.build`, `LambdaSweep.run` |
| 6. Allocation across sequences | `Allocation.solve` (and `Allocation.greedy` for comparison in SC7) |
| 7. Several sensitive patterns | `CutHide.runMultiPattern` |
| 8. Postcondition check | `CutHide.verify` |

---

## 9. Known technical limits

- `Dinic.dfs` is recursive. With very long sequences and wide gaps, the stack may overflow; it must be converted
  to a loop before running SC6 at large scale.
- `PathCounts` saturates at `Long.MAX_VALUE / 4`. A saturated value can still be used as a comparison key
  (that is all the heuristic of [R20] needs) but **must not be printed as an exact count**;
  `PathCounts.isExact()` tells whether it has saturated.
- A scanning scenario **must** build one `Metrics.Context` per configuration (dataset, σ, gap,
  ℓ_max) and reuse it, instead of calling the static `Metrics.evaluate`. The static function re-mines `F_σ(D)` on every
  call; measured on MSNBC, that is 74.7 seconds per pass against 1.83 seconds with the cache, a 41-fold
  difference. `Metrics.evaluate` is only for a single one-off evaluation or for tests.
