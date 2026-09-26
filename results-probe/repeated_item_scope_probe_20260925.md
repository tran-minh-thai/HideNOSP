# Repeated-item scope: data regime and main comparison (probe, 2026-09-25)

Kind: **probe** (development machine, macOS arm64, OpenJDK 26.0.1, bytecode target 25), runs through
`tools/run.py --no-build --probe` from clean clones. Counts and groups are properties of the data; the
official tables need the runs of the measuring machine. Every table below is printed by a tool from the files
named in its last column. Theory behind it: theorem P8 of the design (exact least masks for any mu(P) when
g- = 0), implemented in `hide.FrontierDp`.

Commits: SC2 files of the first batch at `f5a65b8`; the nine MSNBC SC2 files at `2960d00` (the first batch
ran out of memory there and left truncated files, which are not in the repository; the allocation was
rewritten in `9538122`). Five configurations were rerun at `2960d00` and matched the `f5a65b8` files row for
row (630 rows), so the two commits give the same SC2 output. SC3 files at `2960d00`; an earlier SC3 run at
`f5a65b8` with six of the seven methods matched them on all 600 common rows and is not kept.
Logs: `sc2_repeated_probe_log_20260925.txt`, `sc2_repeated_msnbc_rerun_log_20260925.txt`,
`sc3_repeated_probe_log_20260925.txt`. Exact against cut on sampled patterns:
`repeated_item_exactness_probe_20260925.txt`.

SIGN (sigma 200) and BMS1 (sigma 100) have no repeated-item pattern of length 2 to 4 in F_sigma at [0,1] or
[0,3]: the four runs stop with that message, as they should. The genome at sigma 4000, [0,1], has no
distinct-item candidate but two repeated-item ones; it was added on 2026-09-26 (run at `e1ed80e`), so the
repeated scope has 39 configurations.

## 1. Groups over the repeated-item pool (SC2 --profile=repeated)

Same thresholds as preregistration section 3, medians over every frequent repeated-item pattern of length 2
to 4.

Command: `python tools/data_regime_tables.py groups --profile=repeated results-probe`

| dataset | gap | sigma | pool | median rho_rep | median beta | group | beta group | file |
|---|---|---:|---:|---:|---:|---|---|---|
| bible | [0,1] | 2000 | 4 | 0.233 | 0.622 | main | effective | SC2_20260925-150709-129_dev_f5a65b82073d_s2026.csv |
| bible | [0,3] | 2000 | 19 | 0.251 | 0.668 | main | effective | SC2_20260925-150858-539_dev_f5a65b82073d_s2026.csv |
| bpi2012 | [0,1] | 2000 | 24 | 0.109 | 0.212 | intermediate | effective | SC2_20260925-151341-398_dev_f5a65b82073d_s2026.csv |
| bpi2012 | [0,1] | 4000 | 11 | 0.767 | 0.149 | main | between | SC2_20260925-151401-921_dev_f5a65b82073d_s2026.csv |
| bpi2012 | [0,3] | 2000 | 69 | 0.000 | 0.179 | degenerate | between | SC2_20260925-151347-810_dev_f5a65b82073d_s2026.csv |
| bpi2012 | [0,3] | 4000 | 14 | 0.742 | 0.141 | main | between | SC2_20260925-151406-181_dev_f5a65b82073d_s2026.csv |
| bpi2013 | [0,1] | 500 | 112 | 0.233 | 0.387 | main | effective | SC2_20260925-151309-963_dev_f5a65b82073d_s2026.csv |
| bpi2013 | [0,1] | 1000 | 64 | 0.354 | 0.311 | main | effective | SC2_20260925-151327-061_dev_f5a65b82073d_s2026.csv |
| bpi2013 | [0,3] | 500 | 204 | 0.283 | 0.171 | main | between | SC2_20260925-151315-882_dev_f5a65b82073d_s2026.csv |
| bpi2013 | [0,3] | 1000 | 110 | 0.328 | 0.098 | main | between | SC2_20260925-151332-079_dev_f5a65b82073d_s2026.csv |
| dna_mt745584 | [0,10] | 2500 | 260 | 0.859 | 0.064 | main | between | SC2_20260925-145336-674_dev_f5a65b82073d_s2026.csv |
| dna_mt745584 | [0,10] | 4000 | 54 | 0.895 | 0.046 | main | degenerate | SC2_20260925-145454-052_dev_f5a65b82073d_s2026.csv |
| dna_mt745584 | [0,1] | 2500 | 3 | 0.892 | 0.450 | main | effective | SC2_20260925-145332-998_dev_f5a65b82073d_s2026.csv |
| dna_mt745584 | [0,1] | 4000 | 2 | 0.897 | 0.444 | main | effective | SC2_20260926-004410-497_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | 40 | 0.829 | 0.417 | main | effective | SC2_20260925-145333-544_dev_f5a65b82073d_s2026.csv |
| dna_mt745584 | [0,3] | 4000 | 6 | 0.904 | 0.364 | main | effective | SC2_20260925-145453-160_dev_f5a65b82073d_s2026.csv |
| fifa | [0,1] | 3000 | 3 | 0.955 | 0.071 | main | between | SC2_20260925-151141-898_dev_f5a65b82073d_s2026.csv |
| fifa | [0,3] | 3000 | 3 | 0.949 | 0.055 | main | between | SC2_20260925-151219-203_dev_f5a65b82073d_s2026.csv |
| leviathan | [0,1] | 500 | 4 | 0.600 | 0.113 | main | between | SC2_20260925-150623-225_dev_f5a65b82073d_s2026.csv |
| leviathan | [0,3] | 500 | 13 | 0.250 | 0.769 | main | effective | SC2_20260925-150642-803_dev_f5a65b82073d_s2026.csv |
| msnbc | [0,10] | 1000 | 1450 | 0.214 | 0.071 | main | between | SC2_20260925-151049-006_dev_2960d00946cc_s2026.csv |
| msnbc | [0,10] | 2000 | 591 | 0.283 | 0.050 | main | degenerate | SC2_20260925-151500-471_dev_2960d00946cc_s2026.csv |
| msnbc | [0,10] | 5000 | 155 | 0.445 | 0.027 | main | degenerate | SC2_20260925-151742-207_dev_2960d00946cc_s2026.csv |
| msnbc | [0,1] | 1000 | 422 | 0.341 | 0.496 | main | effective | SC2_20260925-150906-453_dev_2960d00946cc_s2026.csv |
| msnbc | [0,1] | 2000 | 195 | 0.418 | 0.419 | main | effective | SC2_20260925-151345-673_dev_2960d00946cc_s2026.csv |
| msnbc | [0,1] | 5000 | 64 | 0.435 | 0.342 | main | effective | SC2_20260925-151700-695_dev_2960d00946cc_s2026.csv |
| msnbc | [0,3] | 1000 | 885 | 0.216 | 0.303 | main | effective | SC2_20260925-150939-359_dev_2960d00946cc_s2026.csv |
| msnbc | [0,3] | 2000 | 362 | 0.349 | 0.229 | main | effective | SC2_20260925-151410-746_dev_2960d00946cc_s2026.csv |
| msnbc | [0,3] | 5000 | 121 | 0.441 | 0.150 | main | between | SC2_20260925-151714-503_dev_2960d00946cc_s2026.csv |
| protein_sdb1 | [0,1] | 400 | 11 | 0.553 | 0.863 | main | effective | SC2_20260925-151500-824_dev_f5a65b82073d_s2026.csv |
| protein_sdb1 | [0,1] | 800 | 4 | 0.671 | 0.843 | main | effective | SC2_20260925-151530-281_dev_f5a65b82073d_s2026.csv |
| protein_sdb1 | [0,3] | 400 | 104 | 0.478 | 0.878 | main | effective | SC2_20260925-151507-765_dev_f5a65b82073d_s2026.csv |
| protein_sdb1 | [0,3] | 800 | 15 | 0.698 | 0.755 | main | effective | SC2_20260925-151532-769_dev_f5a65b82073d_s2026.csv |
| protein_sdb4 | [0,1] | 500 | 11 | 0.569 | 0.863 | main | effective | SC2_20260925-151418-035_dev_f5a65b82073d_s2026.csv |
| protein_sdb4 | [0,1] | 900 | 5 | 0.616 | 0.853 | main | effective | SC2_20260925-151447-529_dev_f5a65b82073d_s2026.csv |
| protein_sdb4 | [0,3] | 500 | 78 | 0.469 | 0.878 | main | effective | SC2_20260925-151425-880_dev_f5a65b82073d_s2026.csv |
| protein_sdb4 | [0,3] | 900 | 13 | 0.709 | 0.751 | main | effective | SC2_20260925-151450-644_dev_f5a65b82073d_s2026.csv |
| sepsis | [0,1] | 300 | 25 | 0.535 | 0.597 | main | effective | SC2_20260925-151414-160_dev_f5a65b82073d_s2026.csv |
| sepsis | [0,3] | 300 | 140 | 0.480 | 0.226 | main | effective | SC2_20260925-151415-037_dev_f5a65b82073d_s2026.csv |

39 configurations from 39 repeated files (100 files given)

## 2. Main comparison on repeated-item sensitive sets (SC3 --patternKind=repeated, |S| = 1)

Twenty sets per configuration (five strata of sup_NO, four sets each), seven methods on the same sets.
`cut-hide`: theorem P8, fewest masks then weight (the objective of the problem). `cut-hide-weight`: theorem
P8, weight then masks. `cut-hide-sweep`: the curves before P8 (lambda sweep, and for a repeated-item pattern
the masks of a cut, the mu(P)-approximation of P3).

Command: `python tools/comparison_table.py results-probe/SC3_*_2960d00946cc_*.csv`

| dataset | gap | sigma | kind | method | sets | HF > 0 | median \|R\| | median \|R\|/\|R\|* | median Lost_rel | median SupDist | file |
|---|---|---:|---|---|---:|---:|---:|---:|---:|---:|---|
| dna_mt745584 | [0,3] | 2500 | repeated | cut-hide | 20 | 0 | 222.0 | 1.000 | 0.0227 | 0.0183 | SC3_20260925-150039-986_dev_2960d00946cc_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | cut-hide-weight | 20 | 0 | 614.0 | 2.817 | 0.0303 | 0.0276 | SC3_20260925-150039-986_dev_2960d00946cc_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | cut-hide-sweep | 20 | 0 | 434.0 | 1.960 | 0.0152 | 0.0154 | SC3_20260925-150039-986_dev_2960d00946cc_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | hha | 20 | 0 | 393.5 | 1.681 | 0.1364 | 0.0261 | SC3_20260925-150039-986_dev_2960d00946cc_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | dbsh | 20 | 0 | 515.0 | 2.164 | 0.1591 | 0.0311 | SC3_20260925-150039-986_dev_2960d00946cc_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | sbsh-r1 | 20 | 0 | 516.0 | 2.263 | 0.1439 | 0.0297 | SC3_20260925-150039-986_dev_2960d00946cc_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | sbsh-rmax | 20 | 0 | 516.0 | 2.263 | 0.1439 | 0.0297 | SC3_20260925-150039-986_dev_2960d00946cc_s2026.csv |
| sepsis | [0,1] | 300 | repeated | cut-hide | 20 | 0 | 104.5 | 1.000 | 0.0285 | 0.0266 | SC3_20260925-150114-863_dev_2960d00946cc_s2026.csv |
| sepsis | [0,1] | 300 | repeated | cut-hide-weight | 20 | 0 | 181.5 | 1.587 | 0.0053 | 0.0285 | SC3_20260925-150114-863_dev_2960d00946cc_s2026.csv |
| sepsis | [0,1] | 300 | repeated | cut-hide-sweep | 20 | 0 | 195.5 | 1.739 | 0.0106 | 0.0317 | SC3_20260925-150114-863_dev_2960d00946cc_s2026.csv |
| sepsis | [0,1] | 300 | repeated | hha | 20 | 0 | 183.0 | 1.581 | 0.0578 | 0.0422 | SC3_20260925-150114-863_dev_2960d00946cc_s2026.csv |
| sepsis | [0,1] | 300 | repeated | dbsh | 20 | 0 | 230.5 | 1.933 | 0.0863 | 0.0510 | SC3_20260925-150114-863_dev_2960d00946cc_s2026.csv |
| sepsis | [0,1] | 300 | repeated | sbsh-r1 | 20 | 0 | 229.5 | 2.024 | 0.0764 | 0.0491 | SC3_20260925-150114-863_dev_2960d00946cc_s2026.csv |
| sepsis | [0,1] | 300 | repeated | sbsh-rmax | 20 | 0 | 229.5 | 2.024 | 0.0764 | 0.0491 | SC3_20260925-150114-863_dev_2960d00946cc_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | cut-hide | 20 | 0 | 82.0 | 1.000 | 0.0021 | 0.0012 | SC3_20260925-150123-357_dev_2960d00946cc_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | cut-hide-weight | 20 | 0 | 118.5 | 1.276 | 0.0000 | 0.0014 | SC3_20260925-150123-357_dev_2960d00946cc_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | cut-hide-sweep | 20 | 0 | 105.5 | 1.253 | 0.0000 | 0.0014 | SC3_20260925-150123-357_dev_2960d00946cc_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | hha | 20 | 0 | 106.0 | 1.233 | 0.0085 | 0.0024 | SC3_20260925-150123-357_dev_2960d00946cc_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | dbsh | 20 | 0 | 110.0 | 1.320 | 0.0085 | 0.0025 | SC3_20260925-150123-357_dev_2960d00946cc_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | sbsh-r1 | 20 | 0 | 112.5 | 1.345 | 0.0053 | 0.0024 | SC3_20260925-150123-357_dev_2960d00946cc_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | sbsh-rmax | 20 | 0 | 112.0 | 1.345 | 0.0064 | 0.0024 | SC3_20260925-150123-357_dev_2960d00946cc_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | cut-hide | 20 | 0 | 614.0 | 1.000 | 0.0052 | 0.0359 | SC3_20260925-150322-178_dev_2960d00946cc_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | cut-hide-weight | 20 | 0 | 625.0 | 1.136 | 0.0052 | 0.0364 | SC3_20260925-150322-178_dev_2960d00946cc_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | cut-hide-sweep | 20 | 0 | 623.5 | 1.048 | 0.0052 | 0.0372 | SC3_20260925-150322-178_dev_2960d00946cc_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | hha | 20 | 0 | 624.5 | 1.036 | 0.0232 | 0.0401 | SC3_20260925-150322-178_dev_2960d00946cc_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | dbsh | 20 | 0 | 625.0 | 1.056 | 0.0541 | 0.0339 | SC3_20260925-150322-178_dev_2960d00946cc_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | sbsh-r1 | 20 | 0 | 626.0 | 1.058 | 0.0361 | 0.0314 | SC3_20260925-150322-178_dev_2960d00946cc_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | sbsh-rmax | 20 | 0 | 637.0 | 1.075 | 0.0258 | 0.0319 | SC3_20260925-150322-178_dev_2960d00946cc_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | cut-hide | 20 | 0 | 869.0 | 1.000 | 0.0065 | 0.0121 | SC3_20260925-150416-571_dev_2960d00946cc_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | cut-hide-weight | 20 | 0 | 1574.5 | 1.715 | 0.0000 | 0.0114 | SC3_20260925-150416-571_dev_2960d00946cc_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | cut-hide-sweep | 20 | 0 | 1743.5 | 1.970 | 0.0000 | 0.0120 | SC3_20260925-150416-571_dev_2960d00946cc_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | hha | 20 | 0 | 1649.0 | 1.816 | 0.0114 | 0.0156 | SC3_20260925-150416-571_dev_2960d00946cc_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | dbsh | 20 | 0 | 1781.5 | 1.999 | 0.0162 | 0.0126 | SC3_20260925-150416-571_dev_2960d00946cc_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | sbsh-r1 | 20 | 0 | 1775.5 | 1.999 | 0.0146 | 0.0140 | SC3_20260925-150416-571_dev_2960d00946cc_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | sbsh-rmax | 20 | 0 | 1775.5 | 1.999 | 0.0130 | 0.0140 | SC3_20260925-150416-571_dev_2960d00946cc_s2026.csv |

35 rows from 5 files
