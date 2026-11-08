# HideNOSP

Hiding sensitive sequential patterns when disclosure is measured by **non-overlapping support**
(`sup_NO`) under **gap constraints**. The repository contains the Nettree cut index, the CUT-HIDE
algorithm, the reimplemented baselines (HHA, DBSH, SBSH, and two rivals that stop at the threshold),
every experiment of the paper, and the result files from which every reported number is computed.

## Requirements

- JDK 25 (the results were produced with Eclipse Temurin 25.0.4.1+1) and Maven 3.9 or later. JUnit 5 is
  the only dependency, for the tests.
- Python 3.10 or later for the tools in `tools/`. The analysis tools need the packages of
  `requirements.txt` (numpy, scipy); fetching data and running experiments need only the standard library.

```bash
python3 -m venv .venv
. .venv/bin/activate          # on Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

## Quick start

```bash
mvn -q test
```

runs the unit tests, including the worked examples of the paper (`src/test/java/hidenosp/WorkedExamplesTest.java`).

```bash
python3 tools/fetch_datasets.py
python3 tools/build_datasets.py
```

downloads the datasets and checks every file against the SHA-256 recorded in `datasets/provenance.tsv`
(see `datasets/README.md` for sources and licences).

```bash
python3 tools/run.py --probe SC1 --trials=200 --seed=2026
```

runs a small implementation check (maximum flow against exhaustive search, minimum number of masks,
vital nodes, incremental index update) and writes its file to `results-probe/`. Every check must report
0 mismatches. `java -cp target/classes hidenosp.exp.Runner` lists every scenario with its options.

## Reproducing the numbers of the paper

From the result files in this repository:

```bash
python3 tools/paper_numbers.py --machine=ADMIN-PC
```

re-runs every table tool on the stored result files and writes `results-invariant/paper_numbers.json`
(counts, ratios, test results) and `results/paper_numbers_timing.json` (running times, platform). Each
value records the tool and the output line it comes from. The tool exits with code 1 when a line cannot
be read or when a number contradicts a statement of the paper.

From scratch, on your own machine (running times will differ; every other number is machine-invariant):

```bash
python3 tools/campaign.py plan --machine=NAME
python3 tools/campaign.py lock --machine=NAME
python3 tools/campaign.py run  --machine=NAME --batch=sc1
```

then the batches `sc2`, `sc3-s1`, `sc3-s5`, `sc3-s10`, `sc3-s20`, `sc3-degenerate`, `sc3-refine`, `sc3lb`,
`sc3opt`, `sc3-pareto`, and, with `--generation=2` (after `lock --generation=2`), `sc3lost`, `sc3-stop`,
`sc3cert` and `sc6`. With `--generation=3` (after `lock --generation=3`), the `g3-` batches rerun the invariant
batches that read S-plus with its corrected definition (`config/preregistration.md`, section 21); the numbers of
the paper come from them. A batch resumes where it stopped. `python3 tools/campaign.py plan` prints the cells
of each batch; `tools/compare_invariant.py --machine=NAME` compares your invariant files with the
committed ones cell by cell.

### Paper to repository

| In the paper | Scenario | Table tool |
|---|---|---|
| SC1, implementation check | `SC1` | read by `paper_numbers.py` |
| SC2, data regime and groups | `SC2` | `preregistered_analysis.py --write-groups` (locked tables in `config/`) |
| Tests of H2, H2-R, H4b, H4b-R; pairwise comparison; ratio to the minimum number of masks (H3, H4a) | `SC3` | `preregistered_analysis.py` |
| Lost_rel at the solution of each method | `SC3` | `levels_table.py` |
| Ratio to the lower bound for several sensitive patterns | `SC3LB` | `lower_bound_table.py` |
| Distance to the exact optimum on small instances | `SC3OPT` | `reduced_optimum_table.py` |
| Exact optimum of Lost on small single-pattern instances | `SC3LOST` | `lost_optimum_table.py` |
| Certified lower bound on the optimum Lost | `SC3CERT` | `cert_table.py` |
| SC4, running time | `SC6` | `timing_table.py` |
| Sensitivity analysis with the dataset as the unit | `SC3` | `dataset_sensitivity.py` |

Scenarios `SC4`, `SC5`, `SC7`, `SC8` and `SIGMA` of the runner are implemented but not reported in the
paper (`SIGMA` applies the sigma rule of `config/protocol.yaml`).

## Default parameters

| Parameter | Value | Where |
|---|---|---|
| longest pattern counted, `l_max` | 4 | `config/protocol.yaml` |
| length of sensitive patterns | 2 to 4 | idem |
| number of sensitive patterns per set | 1, 5, 10, 20 | idem |
| sensitive sets per configuration | at most 20: five strata by `sup_NO`, up to 4 sets each | idem |
| sigma and gap constraints per dataset | listed per dataset | idem |
| band of near-threshold patterns, `epsilon` | 0.5 | idem |
| rounds of adaptive weights | 6 | `AdaptiveCutHide.DEFAULT_ITERATIONS` |
| local search: passes, receiving sequences | 50, 30 | `MaskLocalSearch` |
| weight scale | 1,000,000 | `CostCurve.SCALE` |
| seed | 2026 | `config/protocol.yaml` |
| time limit per method and set | 1800 s | idem |
| JVM heap | 8 GB (32 GB for the Pareto-front batch) | idem |
| small instances (SC3LOST, SC3CERT) | windows of 15 events, 5 sequences with the pattern and 5 without | `Sc3LostOptimum` |
| small instances (SC3OPT) | 8 and 16 sequences | `tools/campaign.py` |
| tests | alpha 0.05, practical threshold 0.02 on Lost_rel, equivalence margins 0.02 (Lost_rel) and 0.01 (SupDist), at most 10% excluded instances | `config/preregistration.md` |
| timing | one warm-up run, then 15, 10, 5 or 3 repetitions by duration | `config/protocol.yaml` rows 8 and 9 |

## Result files

| Directory | Content |
|---|---|
| `results-invariant/` | machine-invariant measurements (Lost, Ghost, \|R\|, HF, SupDist, distortion rate); no timing column allowed |
| `results/` | running times, written only by the declared measuring machine |
| `results-probe/` | exploratory runs (`--probe`); no reported number comes from here |

Every file name has the form `<scenario>_<UTC time>_<machine>_<commit>_s<seed>.csv`, and its header
repeats the run identifier, the options, the JDK and the machine. `hidenosp.io.ResultSink` enforces the
rules of the three directories when a file is opened; `tools/check_results.py` re-checks the files on
disk. Result files written before 2026-09-26 record commit ids that `COMMIT_MAP.tsv` maps to the current
history.

## Layout

```
pom.xml                   Maven build, Java 25
src/main/java/hidenosp/
  core/                   event sequences, patterns, gap constraints, Nettree, sup_NO
  core/flow/              maximum flow with node capacities, minimum cut, vital nodes, incremental masking
  index/                  Nettree cut index
  hide/                   CUT-HIDE: exact curves, allocation, adaptive weights, local search, several patterns
  baseline/               HHA, DBSH, SBSH, stopping rivals, exact solvers, random controls
  metrics/                F_sigma mining, Lost, Ghost, SupDist, HF, data regime
  io/                     dataset reading, result writing, run identifier
  exp/                    the scenarios and the runner
src/test/java/            unit tests and worked examples
config/                   protocol table, preregistration, locked group tables
datasets/                 provenance of the data (data files are downloaded, not stored)
tools/                    run wrapper, campaign, data fetching, table tools, checks (see tools/README.md)
nettree_checks.py, probe/ independent Python checks of the Nettree computations
results*/                 result files (see above)
```

## Licence

MIT, see `LICENSE`. The datasets keep the licences of their sources (`datasets/README.md`).
