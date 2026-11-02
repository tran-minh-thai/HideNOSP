# HideNOSP

Hiding sensitive sequential patterns under **non-overlapping support** (`sup_NO`) with **gap constraints**.

This repository implements the **Nettree cut index** and the **CUT-HIDE** algorithm, together with the
baselines and the experimental scenarios SC1–SC8 of the research design.

- Detailed operation: [`RUNBOOK.md`](RUNBOOK.md)
- Protocol table, 9 rows: [`config/protocol.yaml`](config/protocol.yaml)
- Thresholds locked before the main comparison is run: [`config/preregistration.md`](config/preregistration.md)

The research design (`doc/`), the manuscript (`paper/`) and the run procedure for the measuring
machine (`run.txt`) are **outside** this repository, in the project directory one level up.

## Trying it out

```bash
mvn -o test
```

```bash
java -cp target/classes hidenosp.exp.Runner SC1 --trials=1200
```

SC1 is the implementation check: on each random instance it compares the maximum flow with
exhaustive search, checks the minimum number of masks, checks the characterisation of vital nodes,
and compares the incremental update with a rebuild. The only acceptable result is 0 mismatches.

```bash
java -cp target/classes hidenosp.exp.Runner
```

prints the list of scenarios together with the options of each.

## Requirements

JDK 25 (the LTS release, Eclipse Temurin), Maven 3.9 or later. No external library dependency other than JUnit 5 (tests only).

## Layout

```
pom.xml                 Maven, Java 25
src/main/java/hidenosp/
  core/                 event sequences, patterns, gap constraints, Nettree, sup_NO
  core/flow/            Dinic with node capacities, minimum cut, vital nodes, incremental masking
  index/                Nettree cut index, forward and backward counting
  hide/                 CUT-HIDE, cost curve, lambda sweep, allocation across sequences
  baseline/             random controls HH/HR/RH/RR, occurrence-counting heuristic, HHA, DBSH, SBSH, ILP, GA
  metrics/              HF, Lost, Ghost, DistR, SupDist, F_sigma mining, data regime
  io/                   dataset reading, synthetic data generation, result writing, run identifier stamp
  exp/                  SC1–SC8 and the runner
src/test/java/          the hand-worked examples of the research design, turned into tests
config/                 protocol table, preregistration file
datasets/               dataset provenance; raw data does not go into git
tools/                  versioned runs, data fetching, preregistered analysis, check scripts
nettree_checks.py, probe/  Python probe scripts
results-invariant/      machine-independent measurements
results/                time and memory measurements, only from the declared measuring machine
results-probe/          probe measurements, not used in the paper
COMMIT_MAP.tsv          old → new commit ids of the history rewrite of 2026-09-26
```

The three results directories have their own rules, and `hidenosp.io.ResultSink` enforces them as
soon as a file is opened: an invariant file may not carry a time or memory column, and only the
declared measuring machine may write time figures. `tools/check_results.py` re-checks those rules on
the files actually on disk.

The history of the repository was rewritten on 2026-09-26 to correct the provenance statements in two
documents in `config/`, one script in `tools/` and two commit messages; code, data and results are
unchanged. Result files record the commit id at run time: for a commit that has been rewritten, look
up the new id in [`COMMIT_MAP.tsv`](COMMIT_MAP.tsv).

## Status

The single source of truth for the implementation status is section 5 of [`RUNBOOK.md`](RUNBOOK.md);
this file does not copy it, so that the two places do not drift apart. Quick search for the parts not
yet implemented: `grep -rn "TODO(code)" src/`.

> **Language.** This file is the English translation of the Vietnamese original as of commit `ea39da4`
> (2026-11-02), which stays in the git history. Values, dates, file names and decisions are unchanged;
> decimal commas are written as decimal points.
