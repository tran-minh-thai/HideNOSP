# SC3 with the stratified sampling, the rewritten DBSH and SBSH (probe, 2026-09-25)

Kind: **probe** (development machine, macOS arm64, OpenJDK 26.0.1, bytecode target 25), run from
a clean clone of commit `e89252a`. Only feasibility is read here: the sampling outcome, HF and
whether the run completes. Lost and |R| were not read.

    python tools/run.py --no-build --probe --jvm=-Xmx8g SC3 --dataset=datasets/<file> --format=SPMF --sigma=<s> --gmin=0 --gmax=<g> --maxPatternLength=4 --epsilon=0.5 --sensitiveSize=<k> --seed=2026 --methods=cut-hide,dbsh,sbsh-r1,sbsh-rmax

| dataset, sigma, gap, set size | sets drawn / requested | per stratum p10..p90 (candidates -> sets) | HF = 0 | file |
|---|---|---|---|---|
| dna_mt745584, 2500, [0,3], 1 | 20 / 20 | 4->4, 5->4, 4->4, 5->4, 5->4 | all rows, all four methods | `SC3_20260925-123939-397` |
| dna_mt745584, 2500, [0,3], 5 | 3 / 20 | 4->0, 5->1, 4->0, 5->1, 5->1 | all rows | `SC3_20260925-123958-037` |
| dna_mt745584, 4000, [0,3], 5 | 0 / 20 | 0, 1, 1, 1, 1 -> 0 each | run refused: the cell does not exist | none |
| msnbc, 2000, [0,3], 5 | 20 / 20 | 35-36 -> 4 each | all rows | `SC3_20260925-124001-736` |
| bpi2013, 1000, [0,1], 1 | 20 / 20 | 6-7 -> 4 each | all rows | `SC3_20260925-124341-061` |

Every file now carries two header lines written by SC3 itself: `# options {...}` with the full
option map of the run, and `# sensitive sets: ...` with the sampling outcome above.
