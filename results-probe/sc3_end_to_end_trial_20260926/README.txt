End-to-end SC3 trial of the method under test (preregistration section 12), 2026-09-26
======================================================================================

PROBE on the development machine (macOS arm64, JDK 26.0.1 instead of the locked JDK 25, which
tools/run.py allows for probes only). The counts are properties of these runs; the seconds are
wall-clock with four runs in parallel and are not measurements.

Purpose. Run SC3 through tools/run.py exactly as an official run would, with the method under test
cut-hide-rw-ls, its two ablations (cut-hide-rw, cut-hide) and the four baselines of the Holm family
(hha, dbsh, sbsh-r1, sbsh-rmax), then run tools/preregistered_analysis.py on the real rows. Until
now the analysis had only met cut-hide-rw-ls in generated rows.

Cells (all l_max = 4, g- = 0, 4 sets per stratum unless noted):
  sepsis        sigma 300   [0,1]  repeated-item patterns
  dna_mt745584  sigma 2500  [0,3]  distinct-item patterns
  protein_sdb4  sigma 500   [0,3]  distinct-item patterns
  bpi2013       sigma 500   [0,1]  repeated-item patterns
  protein_sdb4  sigma 500   [0,3]  five distinct-item patterns per set, 1 set per stratum
The exact commands are in scripts/run_trial.sh and in each outputs/attempt_*/trial.txt.

Build. Maven cannot build offline on this machine (outputs/attempt_1_maven_offline/: the compiler
plugin needs artifacts never downloaded here), so the classes were compiled with javac using the
settings of pom.xml (release 25, UTF-8) from the clean tree, and every run used --no-build.
outputs/build.txt records the compiler and a digest of all class files.

Attempts, in order (every one is kept):
  1  Maven offline build failed; the other runs found no classes. Nothing written.
  2  The separate working copy has no data (raw data is never in git); every run stopped at
     "cannot read datasets/...". The four data files were then copied from the checkout that has
     them and checked against the SHA-256 and sizes of datasets/provenance.tsv (outputs/dataset_checks.txt).
     This attempt also showed that four JVMs started together by xargs got the same run stamp,
     to the millisecond; later attempts start parallel runs one second apart (see "Open").
  3  At 6c418ae: the four one-pattern cells completed; the five-pattern cell stopped at the key
     cut-hide-rw ("the adaptive weight needs one sensitive pattern ... got 5"), leaving a .partial
     file with one row (kept in outputs/attempt_3_at_6c418ae/, removed from results-probe/). The
     runbook asks every official SC3 run to list cut-hide-rw, so every cell with 5, 10 or 20
     patterns would have stopped. Fixed in 97ba7c9 (the key runs the greedy of cut-hide with
     several patterns, as cut-hide-rw-ls already did).
  4  At 97ba7c9: all five cells completed. These are the files the checks and the statistics use.

Checks of the result files (scripts/e2e_checks.py; its three injected faults are caught, see
outputs/checker_injections/). Output of attempt 4, verbatim:
  SC3_20260926-105412-166_dev_97ba7c977f33_s2026.csv: datasets/sepsis.txt 300 1 repeated 1 4, 20 sets, 140 rows
     Lost summed over sets: cut-hide-rw-ls 67, cut-hide-rw 67, cut-hide 70, hha 116, dbsh 143, sbsh-r1 135, sbsh-rmax 134; sets where cut-hide-rw-ls loses fewer than cut-hide: 3
  SC3_20260926-105422-904_dev_97ba7c977f33_s2026.csv: datasets/dna_mt745584.txt 2500 3 distinct 1 4, 20 sets, 140 rows
     Lost summed over sets: cut-hide-rw-ls 66, cut-hide-rw 67, cut-hide 85, hha 185, dbsh 141, sbsh-r1 134, sbsh-rmax 132; sets where cut-hide-rw-ls loses fewer than cut-hide: 9
  SC3_20260926-105423-923_dev_97ba7c977f33_s2026.csv: datasets/protein_sdb4.txt 500 3 distinct 1 4, 20 sets, 140 rows
     Lost summed over sets: cut-hide-rw-ls 26, cut-hide-rw 27, cut-hide 38, hha 172, dbsh 183, sbsh-r1 133, sbsh-rmax 133; sets where cut-hide-rw-ls loses fewer than cut-hide: 3
  SC3_20260926-105424-932_dev_97ba7c977f33_s2026.csv: datasets/bpi2013.txt 500 1 repeated 1 4, 20 sets, 140 rows
     Lost summed over sets: cut-hide-rw-ls 65, cut-hide-rw 65, cut-hide 88, hha 191, dbsh 301, sbsh-r1 252, sbsh-rmax 242; sets where cut-hide-rw-ls loses fewer than cut-hide: 10
  SC3_20260926-105425-957_dev_97ba7c977f33_s2026.csv: datasets/protein_sdb4.txt 500 3 distinct 5 1, 5 sets, 35 rows
     Lost summed over sets: cut-hide-rw-ls 70, cut-hide-rw 70, cut-hide 70, hha 264, dbsh 197, sbsh-r1 155, sbsh-rmax 154; sets where cut-hide-rw-ls loses fewer than cut-hide: 0
  
  file complete (no .partial)                                              5 checked, 0 failed
  every key present once per set                                           85 checked, 0 failed
  HF = 0 for the CUT-HIDE keys                                             255 checked, 0 failed
  one pattern: |R| equal across CUT-HIDE keys and equal to |R|*            80 checked, 0 failed
  one pattern: Lost rw-ls <= rw <= cut-hide                                80 checked, 0 failed
  data rows identical to the earlier attempt (same configuration)          4 checked, 0 failed
  several patterns: adaptive keys give the cut-hide row                    10 checked, 0 failed

The last-but-one line compares the data rows of attempt 4 with those of attempt 3 for the four
one-pattern cells: the fix of 97ba7c9 touches only the several-pattern branch, and the rows are
the same.

Statistics (tools/preregistered_analysis.py --sc3 <the five files of attempt 4> --skip-groups):
outputs/stats_report.txt. Negative control: the same files with every cut-hide-rw-ls row removed:
outputs/stats_report_missing.txt. Before 9ba4406 the negative control exited with 0 although the
report said the method under test was absent; the tool now exits with 1 when H2 or H2-R is not the
locked test. Exit codes with the committed tool:
  trial files: exit 0
  negative control (every cut-hide-rw-ls row removed): exit 1
  tool: tools/preregistered_analysis.py at 9ba4406 (sha256 277e49fbb3e0)

Observation (probe, five cells, no group filter; not a result). Under the pairing of section 5
of the preregistration, most (instance, baseline) pairs fall in "baseline uses more masks", which
is reported apart and not tested, so the Wilcoxon tests of this trial run on few pairs; see the
lines "into Wilcoxon (same |R|)" and "baseline uses more masks" of outputs/stats_report.txt. The
pairing was locked before SC3; this trial does not change it.

Open.
  - Run stamps have millisecond resolution; runs started in the same millisecond get the same
    stamp. ResultSink refuses to overwrite, so such a run fails loudly instead of losing data.
    run.txt starts no run in parallel.
  - The dataset files of a new checkout must be fetched or copied and checked against
    datasets/provenance.tsv before any run.
