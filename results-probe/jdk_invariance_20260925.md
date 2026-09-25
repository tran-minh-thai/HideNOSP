# Machine-invariant output across JDK and operating system (probe, 2026-09-25)

Kind: **probe** (development machine). It checks a property of the output, not a performance
number, so the conclusion does not depend on the machine; it is kept in results-probe/ because it
was run on the development machine.

## Question

The protocol moves every machine from JDK 17 to JDK 25 (Temurin, LTS). Do the machine-invariant
numbers already committed in results-invariant/ (phase 1, JDK 17.0.19 on Windows, host PC-THAI)
stay valid, or must they be regenerated?

## What was run

Code: commit `2f63b56` (the commit stamped into the phase-1 SC2 files), compiled with
`javac --release 17`, run on OpenJDK 26.0.1, macOS, arm64 (the header says `host unknown-host`
because RunId reads the host name from environment variables that macOS does not export).
Datasets: the committed ones, sha256 as in datasets/provenance.tsv (8 of 8 match).

    HIDENOSP_REVISION=2f63b567ccf1 java -cp <classes> hidenosp.exp.Runner SC2 --dataset=datasets/sign.txt      --format=SPMF --sigma=200  --gmin=0 --gmax=1 --maxPatternLength=4 --patterns=10
    HIDENOSP_REVISION=2f63b567ccf1 java -cp <classes> hidenosp.exp.Runner SC2 --dataset=datasets/bms1.txt      --format=SPMF --sigma=100  --gmin=0 --gmax=1 --maxPatternLength=4 --patterns=20
    HIDENOSP_REVISION=2f63b567ccf1 java -cp <classes> hidenosp.exp.Runner SC2 --dataset=datasets/dna_mt745584.txt --format=SPMF --sigma=2500 --gmin=0 --gmax=1 --maxPatternLength=4 --patterns=12
    HIDENOSP_REVISION=2f63b567ccf1 java -cp <classes> hidenosp.exp.Runner SC2 --dataset=datasets/leviathan.txt --format=SPMF --sigma=500  --gmin=0 --gmax=1 --maxPatternLength=4 --patterns=18

The `--patterns` values are **inferred**, not recorded: the phase-1 files do not store their
command line. With the default (10) three of the four configurations came out with a different
row count; with 20, 12 and 18 (the row counts of the committed files) all four match. That is
consistent with those being the values used, but it is a reconstruction.

## Result

Compared with `tools/compare_invariant.py` against the committed files
`SC2_20260912-142737` (sign), `-142741` (bms1), `-142324` (dna), `-142749` (leviathan):

| configuration | data rows compared | differing |
|---|---:|---:|
| sign [0,1] sigma=200 | 8 | 0 |
| bms1 [0,1] sigma=100 | 20 | 0 |
| dna_mt745584 [0,1] sigma=2500 | 12 | 0 |
| leviathan [0,1] sigma=500 | 18 | 0 |
| **total** | **58** | **0** |

Two byte-level differences were normalised, and only these: CRLF line endings in the committed
files, and the Windows path separator in the dataset column (`datasets\sign.txt`). Both are
properties of the operating system that wrote the file, not of the computation.

A second check ran the current code (HEAD `f8e91d3` plus the uncommitted JDK 25 changes, bytecode
target 25, class file major version 69) on SC2 sign with the same arguments: 8 of 8 rows identical
to the committed file. That file is not stored here because it carries the revision of a scratch
commit that does not exist in this repository. The measuring machine repeats this check on JDK 25
(see run.txt).

The comparator was fault-injected before being trusted: one changed digit, one missing row, zero
data rows and one renamed column were each reported as a failure (4 of 4).

## Consequence

The 25 phase-1 SC2 files and the SC1 file stay valid under the JDK change; the SC2 code path
(Sc2DataRegime, DataProfile, core, FrequentPatterns, DatasetReader) has no commit after `2f63b56`.
Not covered: JDK 25 itself (not installed on the development machine; 17 and 26 bracket it), and
the SC3 code path, which has no committed paper numbers yet.
