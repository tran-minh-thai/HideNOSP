# tools/

Supporting tools, not part of the algorithm.

| File | Purpose |
|---|---|
| `run.py` | **Runs a scenario stamped with the code version.** Use it for every run whose numbers go into the paper |
| `fetch_datasets.py` | Downloads the public datasets, computes SHA-256, writes `datasets/provenance.tsv` |
| `check_results.py` | Re-checks the results-directory rule of design section 11.8 on the files actually on disk |
| `merge_decisions.py` | Merges review results into `config/quyet_dinh_can_chot.md` (before 2026-09-26: `gop_quyet_dinh.py`) |
| `preregistered_analysis.py` | **Analysis of H2 and H3** exactly as `config/preregistration.md` locks it (before 2026-09-26: `thong_ke.py`) |
| `preregistered_analysis_test.py` | Pins the statistics to hand-computed values. Run it before trusting any table |
| `check_english.py` | Checks that every code file is written in English only, and that no documentation file holds a Vietnamese letter; exit code 1 on a violation |
| `check_english_test.py` | Injects each kind of violation into a temporary repository and requires `check_english.py` to catch it |
| `compare_invariant.py` | Compare machine-invariant result files exactly: one pair, or (`--machine=<host> [--ref=<branch>]`) every file of one machine against the committed file of the same cell made on another host |
| `compare_invariant_test.py` | Builds files with one known property each and requires `compare_invariant.py` to report it (18 checks) |
| `sc3_commands.py` | **Generates the missing SC3 commands** from the two locked group tables (main group), skipping cells that already have a result file; `--self-test` injects faults into the cell-matching rule |

`check_results.py` checks the same rule that `hidenosp.io.ResultSink` enforces when it opens a file,
but on disk: a file moved or edited by hand, or produced by an old version of the code, is still
caught.

```bash
python tools/check_results.py
```

It exits with code 1 when there is a problem, so it can be used as a pre-commit hook.

## Why every run goes through `run.py`

`RunId` reads the environment variable `HIDENOSP_REVISION` and stamps it into the result file name.
Without it every file carries the stamp `unversioned` and cannot be traced back to the commit that
produced it. Rule 2.6.8 requires a run identifier, and `config/protocol.yaml` locks this variable as
**mandatory** for numbers that go into the paper.

The script does three things in order: it recompiles (so that the `.class` files certainly match the
source whose sha is being stamped), checks that the working tree is clean (a sha identifies the code
that ran only when no change is pending), and only then sets the variable and calls `Runner`.

## `preregistered_analysis.py`

```bash
python tools/preregistered_analysis.py --sc3 results-invariant --sc2 results-invariant --out results-invariant/h2_h3_analysis.txt
```

The script does **not** choose the test, the thresholds or the pairing. All of these were locked in
`config/preregistration.md` before SC3 ran; here they are only read back and carried out. The
constants sit together in the class `LOCKED`, each noting the section of the preregistration it
comes from, so that they can be checked by eye.

It checks three things itself and **stops with exit code 1** when one fails: an SC3 file lacks
columns (it was produced by an old version of the code), the configuration groups recomputed from
SC2 differ from the table locked in section 3, or the share of excluded instances exceeds 10%.

Next to H2 it prints the table of pairs (|R|, Lost) that section 5 of the preregistration specifies
for the instances where a baseline uses more masks — descriptive, not tested. H3 groups by
`method_key`, not by the display name.

Exit code 0 does **not** mean that H2 is supported. It only means that the analysis ran.

Run `python tools/preregistered_analysis_test.py` first. Its tests pin Pratt, rank-biserial, Holm
and Hodges–Lehmann to hand-computed values, including an enumeration of all 2⁸ outcomes that
catches the error of scipy's `method="exact"` under Pratt (see section 7 of `../RUNBOOK.md`).

## `check_english.py`

```bash
python tools/check_english.py
python tools/check_english_test.py
```

Every code file (comments, printed strings, names, file names) must be written in English. The tool
reads the file list from git and puts each file into exactly one class (code, documentation `*.md`,
run output); a file that fits no class is an error. Documentation outside the result trees is checked
for Vietnamese letters only, so that it may still cite section numbers and name files whose names are
Vietnamese. Three rules: Vietnamese letters with diacritics,
Vietnamese words without diacritics (a list measured over the whole repository; `--measure` counts
again), and section or theorem numbers of the paper. Known limits are listed at the top of the file.

## Still missing

- A script that builds the tables and figures of the manuscript from the CSV files. The label table
  belongs to the manuscript side: `labels.tsv` left the code repository on 2026-09-26 (last version
  in the git history, commit `5e5a5de`).
- A wrapper that records the resident memory of the process for SC6 (design section 11.7, line 6);
  on Windows from `Get-Process`, on Linux from `/proc/self/status`.
