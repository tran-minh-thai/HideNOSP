# Exact Pareto fronts of CUT-HIDE against the baselines (probe, 2026-09-26)

Kind: **probe** (development machine, macOS arm64, OpenJDK 26.0.1, bytecode target 25), `tools/run.py --no-build
--probe` from a clean clone of commit `e1ed80e`. Preregistration section 11, point 6: descriptive, no test.
Theory: proposition P10 of the design (the vertices of the convex front between |R| and the surrogate weight
w(R), exactly, for one sensitive pattern with g- = 0). Each configuration: 20 sensitive sets of one pattern
(five sup_NO strata, four sets each); every method runs on the same sets. Log: `sc3_pareto_probe_log_20260926.txt`.

A method is **dominated** in a set when some vertex of the front has no more masks and no larger Lost_rel,
and is strictly better in one of the two; it **ties** when it lands exactly on a vertex. Ties happen where the
front is a single optimum that the method also reaches. Every table below is printed by a tool from the files
named in it.

An earlier run of the first configuration at `3a55912` (before two speed-ups that do not change results) is
not kept: its files were deleted in the clone before they were copied out. The `e1ed80e` run reproduces its
summary line exactly (222 median vertices, 868 at most, 2.817, 0.0227, 0.0152, the same dominance counts).

## 1. Fronts and dominance

Command: `python tools/pareto_table.py results-probe` (rows 1-5: repeated-item sets; rows 6-8: distinct-item
sets, the main scope).

| dataset | gap | sigma | sets | median vertices | max vertices | median max |R|/|R|* | median Lost_rel, first vertex | median least Lost_rel | methods dominated by the front (share of sets) | files |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|---|
| dna_mt745584 | [0,3] | 2500 | 20 | 222.0 | 868 | 2.817 | 0.0227 | 0.0152 | cut-hide-sweep 19/20, cut-hide-weight 20/20, dbsh 20/20, hha 20/20, sbsh-r1 20/20, sbsh-rmax 20/20 | SC3PARETO_20260926-004316-283_dev_e1ed80e2aa3f_s2026.csv |
| sepsis | [0,1] | 300 | 20 | 37.5 | 115 | 1.587 | 0.0285 | 0.0053 | cut-hide-sweep 16/20, cut-hide-weight 20/20, dbsh 20/20, hha 20/20, sbsh-r1 20/20, sbsh-rmax 20/20 | SC3PARETO_20260926-005222-764_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | 20 | 21.5 | 67 | 1.276 | 0.0021 | 0.0000 | cut-hide-sweep 20/20, cut-hide-weight 18/20 (+2 tied), dbsh 20/20, hha 20/20, sbsh-r1 20/20, sbsh-rmax 20/20 | SC3PARETO_20260926-005237-531_dev_e1ed80e2aa3f_s2026.csv |
| bpi2013 | [0,1] | 500 | 20 | 6.5 | 217 | 1.136 | 0.0052 | 0.0026 | cut-hide-sweep 12/20 (+5 tied), cut-hide-weight 18/20 (+2 tied), dbsh 18/20 (+2 tied), hha 20/20, sbsh-r1 18/20 (+2 tied), sbsh-rmax 18/20 (+2 tied) | SC3PARETO_20260926-005455-160_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | 20 | 83.0 | 657 | 1.715 | 0.0065 | 0.0000 | cut-hide-sweep 19/20, cut-hide-weight 19/20 (+1 tied), dbsh 20/20, hha 20/20, sbsh-r1 20/20, sbsh-rmax 20/20 | SC3PARETO_20260926-005649-463_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | 20 | 85.5 | 735 | 1.530 | 0.0000 | 0.0000 | cut-hide-sweep 0/20 (+20 tied), cut-hide-weight 18/20 (+2 tied), dbsh 20/20, hha 19/20, sbsh-r1 20/20, sbsh-rmax 20/20 | SC3PARETO_20260926-011337-531_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | 20 | 1.5 | 144 | 1.009 | 0.0000 | 0.0000 | cut-hide-sweep 0/20 (+19 tied), cut-hide-weight 10/20 (+10 tied), dbsh 18/20 (+2 tied), hha 16/20 (+4 tied), sbsh-r1 19/20 (+1 tied), sbsh-rmax 19/20 (+1 tied) | SC3PARETO_20260926-011720-945_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | 20 | 1.0 | 1 | 1.000 | 0.0000 | 0.0000 | cut-hide-sweep 1/20 (+18 tied), cut-hide-weight 0/20 (+20 tied), dbsh 14/20 (+6 tied), hha 13/20 (+7 tied), sbsh-r1 10/20 (+10 tied), sbsh-rmax 10/20 (+10 tied) | SC3PARETO_20260926-011957-740_dev_e1ed80e2aa3f_s2026.csv |

distinct-item sets: 240 pairs of a Holm-family baseline and a set: 198 dominated, 41 tied, 1 not dominated
repeated-item sets: 400 pairs of a Holm-family baseline and a set: 394 dominated, 6 tied, 0 not dominated

Pairs neither dominated nor tied (every method):
- repeated dna_mt745584 [0,3] sigma 2500, cut-hide-sweep, set [[4, 1, 1, 4]] (p30): |R| 224, Lost_rel 0.0152; front |R| 112..387, best Lost_rel within |R| <= 224: 0.0303
- repeated sepsis [0,1] sigma 300, cut-hide-sweep, set [[2, 9, 2, 2]] (p30): |R| 124, Lost_rel 0.0106; front |R| 51..97, best Lost_rel within |R| <= 124: 0.0213
- repeated sepsis [0,1] sigma 300, cut-hide-sweep, set [[9, 2, 9, 9]] (p50): |R| 133, Lost_rel 0.0106; front |R| 58..150, best Lost_rel within |R| <= 133: 0.0213
- repeated sepsis [0,1] sigma 300, cut-hide-sweep, set [[9, 2, 2, 9]] (p50): |R| 226, Lost_rel 0.0106; front |R| 136..213, best Lost_rel within |R| <= 226: 0.0213
- repeated sepsis [0,1] sigma 300, cut-hide-sweep, set [[9, 2, 9, 2]] (p70): |R| 344, Lost_rel 0.0638; front |R| 195..308, best Lost_rel within |R| <= 344: 0.0745
- repeated bpi2013 [0,1] sigma 500, cut-hide-sweep, set [[1, 11, 1, 0]] (p70): |R| 1647, Lost_rel 0.0206; front |R| 1647..1994, best Lost_rel within |R| <= 1647: 0.0258
- repeated bpi2013 [0,1] sigma 500, cut-hide-sweep, set [[1, 1, 1, 10]] (p90): |R| 2769, Lost_rel 0.0052; front |R| 2744..2751, best Lost_rel within |R| <= 2769: 0.0258
- repeated bpi2013 [0,1] sigma 500, cut-hide-sweep, set [[1, 11, 11]] (p90): |R| 3246, Lost_rel 0.0161; front |R| 2394..3086, best Lost_rel within |R| <= 3246: 0.0430
- repeated msnbc [0,1] sigma 2000, cut-hide-sweep, set [[1, 4, 1]] (p90): |R| 5927, Lost_rel 0.0066; front |R| 4914..5579, best Lost_rel within |R| <= 5927: 0.0099
- distinct dna_mt745584 [0,3] sigma 2500, hha, set [[1, 4]] (p90): |R| 2651, Lost_rel 0.3778; front |R| 2633..3820, best Lost_rel within |R| <= 2651: 0.4000
- distinct protein_sdb4 [0,3] sigma 500, cut-hide-sweep, set [[7, 2]] (p90): |R| 979, Lost_rel 0.0000; front |R| 979..979, best Lost_rel within |R| <= 979: 0.0021
- distinct msnbc [0,1] sigma 2000, cut-hide-sweep, set [[1, 4]] (p90): |R| 11326, Lost_rel 0.0035; front |R| 11326..11326, best Lost_rel within |R| <= 11326: 0.0069

8 configurations; the first vertex is the cut-hide row in every set

Reading (every number is in the lines the tool printed above):

- the totals per scope count the pairs (baseline of the Holm family, sensitive set). In the repeated-item
  scope no baseline is better than the front in any set; the ties are sets whose front is a single optimum the
  baseline also reaches. In the main scope one pair is not dominated, listed above: HHA on the top-stratum
  genome set, where every method loses a large share of the patterns.
- the other exceptions are the lambda-sweep variant of CUT-HIDE. In some sets it masks more positions than
  the last vertex of the front and loses fewer patterns; in two sets of the main scope it masks exactly as
  many positions as the single vertex and loses fewer. Among masking sets of equal size, the lightest in the
  surrogate weight is not always the one that loses the fewest patterns: the weight tracks Lost only
  approximately. This is the lever for the next step, not a defect of the front, which is exact for the
  weight it is given.
- MSNBC in the main scope has one vertex per front: the fewest-masks solution is also the lightest.

## 2. The methods on the same sets (medians)

Command: `python tools/comparison_table.py results-probe/SC3_*_e1ed80e2aa3f_*.csv`

| dataset | gap | sigma | kind | method | sets | HF > 0 | median \|R\| | median \|R\|/\|R\|* | median Lost_rel | median SupDist | file |
|---|---|---:|---|---|---:|---:|---:|---:|---:|---:|---|
| dna_mt745584 | [0,3] | 2500 | repeated | cut-hide | 20 | 0 | 222.0 | 1.000 | 0.0227 | 0.0183 | SC3_20260926-004316-283_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | cut-hide-weight | 20 | 0 | 614.0 | 2.817 | 0.0303 | 0.0276 | SC3_20260926-004316-283_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | cut-hide-sweep | 20 | 0 | 434.0 | 1.960 | 0.0152 | 0.0154 | SC3_20260926-004316-283_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | hha | 20 | 0 | 393.5 | 1.681 | 0.1364 | 0.0261 | SC3_20260926-004316-283_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | dbsh | 20 | 0 | 515.0 | 2.164 | 0.1591 | 0.0311 | SC3_20260926-004316-283_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | sbsh-r1 | 20 | 0 | 516.0 | 2.263 | 0.1439 | 0.0297 | SC3_20260926-004316-283_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | repeated | sbsh-rmax | 20 | 0 | 516.0 | 2.263 | 0.1439 | 0.0297 | SC3_20260926-004316-283_dev_e1ed80e2aa3f_s2026.csv |
| sepsis | [0,1] | 300 | repeated | cut-hide | 20 | 0 | 104.5 | 1.000 | 0.0285 | 0.0266 | SC3_20260926-005222-764_dev_e1ed80e2aa3f_s2026.csv |
| sepsis | [0,1] | 300 | repeated | cut-hide-weight | 20 | 0 | 181.5 | 1.587 | 0.0053 | 0.0285 | SC3_20260926-005222-764_dev_e1ed80e2aa3f_s2026.csv |
| sepsis | [0,1] | 300 | repeated | cut-hide-sweep | 20 | 0 | 195.5 | 1.739 | 0.0106 | 0.0317 | SC3_20260926-005222-764_dev_e1ed80e2aa3f_s2026.csv |
| sepsis | [0,1] | 300 | repeated | hha | 20 | 0 | 183.0 | 1.581 | 0.0578 | 0.0422 | SC3_20260926-005222-764_dev_e1ed80e2aa3f_s2026.csv |
| sepsis | [0,1] | 300 | repeated | dbsh | 20 | 0 | 230.5 | 1.933 | 0.0863 | 0.0510 | SC3_20260926-005222-764_dev_e1ed80e2aa3f_s2026.csv |
| sepsis | [0,1] | 300 | repeated | sbsh-r1 | 20 | 0 | 229.5 | 2.024 | 0.0764 | 0.0491 | SC3_20260926-005222-764_dev_e1ed80e2aa3f_s2026.csv |
| sepsis | [0,1] | 300 | repeated | sbsh-rmax | 20 | 0 | 229.5 | 2.024 | 0.0764 | 0.0491 | SC3_20260926-005222-764_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | cut-hide | 20 | 0 | 82.0 | 1.000 | 0.0021 | 0.0012 | SC3_20260926-005237-531_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | cut-hide-weight | 20 | 0 | 118.5 | 1.276 | 0.0000 | 0.0014 | SC3_20260926-005237-531_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | cut-hide-sweep | 20 | 0 | 105.5 | 1.253 | 0.0000 | 0.0014 | SC3_20260926-005237-531_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | hha | 20 | 0 | 106.0 | 1.233 | 0.0085 | 0.0024 | SC3_20260926-005237-531_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | dbsh | 20 | 0 | 110.0 | 1.320 | 0.0085 | 0.0025 | SC3_20260926-005237-531_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | sbsh-r1 | 20 | 0 | 112.5 | 1.345 | 0.0053 | 0.0024 | SC3_20260926-005237-531_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | repeated | sbsh-rmax | 20 | 0 | 112.0 | 1.345 | 0.0064 | 0.0024 | SC3_20260926-005237-531_dev_e1ed80e2aa3f_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | cut-hide | 20 | 0 | 614.0 | 1.000 | 0.0052 | 0.0359 | SC3_20260926-005455-160_dev_e1ed80e2aa3f_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | cut-hide-weight | 20 | 0 | 625.0 | 1.136 | 0.0052 | 0.0364 | SC3_20260926-005455-160_dev_e1ed80e2aa3f_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | cut-hide-sweep | 20 | 0 | 623.5 | 1.048 | 0.0052 | 0.0372 | SC3_20260926-005455-160_dev_e1ed80e2aa3f_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | hha | 20 | 0 | 624.5 | 1.036 | 0.0232 | 0.0401 | SC3_20260926-005455-160_dev_e1ed80e2aa3f_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | dbsh | 20 | 0 | 625.0 | 1.056 | 0.0541 | 0.0339 | SC3_20260926-005455-160_dev_e1ed80e2aa3f_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | sbsh-r1 | 20 | 0 | 626.0 | 1.058 | 0.0361 | 0.0314 | SC3_20260926-005455-160_dev_e1ed80e2aa3f_s2026.csv |
| bpi2013 | [0,1] | 500 | repeated | sbsh-rmax | 20 | 0 | 637.0 | 1.075 | 0.0258 | 0.0319 | SC3_20260926-005455-160_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | cut-hide | 20 | 0 | 869.0 | 1.000 | 0.0065 | 0.0121 | SC3_20260926-005649-463_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | cut-hide-weight | 20 | 0 | 1574.5 | 1.715 | 0.0000 | 0.0114 | SC3_20260926-005649-463_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | cut-hide-sweep | 20 | 0 | 1743.5 | 1.970 | 0.0000 | 0.0120 | SC3_20260926-005649-463_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | hha | 20 | 0 | 1649.0 | 1.816 | 0.0114 | 0.0156 | SC3_20260926-005649-463_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | dbsh | 20 | 0 | 1781.5 | 1.999 | 0.0162 | 0.0126 | SC3_20260926-005649-463_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | sbsh-r1 | 20 | 0 | 1775.5 | 1.999 | 0.0146 | 0.0140 | SC3_20260926-005649-463_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | repeated | sbsh-rmax | 20 | 0 | 1775.5 | 1.999 | 0.0130 | 0.0140 | SC3_20260926-005649-463_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | distinct | cut-hide | 20 | 0 | 210.5 | 1.000 | 0.0000 | 0.0060 | SC3_20260926-011337-531_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | distinct | cut-hide-weight | 20 | 0 | 381.5 | 1.530 | 0.0303 | 0.0180 | SC3_20260926-011337-531_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | distinct | cut-hide-sweep | 20 | 0 | 210.5 | 1.000 | 0.0000 | 0.0060 | SC3_20260926-011337-531_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | distinct | hha | 20 | 0 | 214.0 | 1.011 | 0.0758 | 0.0201 | SC3_20260926-011337-531_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | distinct | dbsh | 20 | 0 | 240.0 | 1.106 | 0.0758 | 0.0126 | SC3_20260926-011337-531_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | distinct | sbsh-r1 | 20 | 0 | 262.5 | 1.172 | 0.0606 | 0.0186 | SC3_20260926-011337-531_dev_e1ed80e2aa3f_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | distinct | sbsh-rmax | 20 | 0 | 256.0 | 1.191 | 0.0682 | 0.0149 | SC3_20260926-011337-531_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | distinct | cut-hide | 20 | 0 | 308.5 | 1.000 | 0.0000 | 0.0038 | SC3_20260926-011720-945_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | distinct | cut-hide-weight | 20 | 0 | 317.0 | 1.009 | 0.0000 | 0.0038 | SC3_20260926-011720-945_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | distinct | cut-hide-sweep | 20 | 0 | 308.5 | 1.000 | 0.0000 | 0.0038 | SC3_20260926-011720-945_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | distinct | hha | 20 | 0 | 308.5 | 1.000 | 0.0075 | 0.0048 | SC3_20260926-011720-945_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | distinct | dbsh | 20 | 0 | 319.0 | 1.045 | 0.0085 | 0.0041 | SC3_20260926-011720-945_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | distinct | sbsh-r1 | 20 | 0 | 319.0 | 1.047 | 0.0032 | 0.0040 | SC3_20260926-011720-945_dev_e1ed80e2aa3f_s2026.csv |
| protein_sdb4 | [0,3] | 500 | distinct | sbsh-rmax | 20 | 0 | 319.0 | 1.047 | 0.0032 | 0.0040 | SC3_20260926-011720-945_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | distinct | cut-hide | 20 | 0 | 1120.5 | 1.000 | 0.0000 | 0.0031 | SC3_20260926-011957-740_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | distinct | cut-hide-weight | 20 | 0 | 1120.5 | 1.000 | 0.0000 | 0.0031 | SC3_20260926-011957-740_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | distinct | cut-hide-sweep | 20 | 0 | 1120.5 | 1.000 | 0.0000 | 0.0031 | SC3_20260926-011957-740_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | distinct | hha | 20 | 0 | 1120.5 | 1.000 | 0.0033 | 0.0027 | SC3_20260926-011957-740_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | distinct | dbsh | 20 | 0 | 1120.5 | 1.000 | 0.0049 | 0.0024 | SC3_20260926-011957-740_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | distinct | sbsh-r1 | 20 | 0 | 1120.5 | 1.000 | 0.0016 | 0.0021 | SC3_20260926-011957-740_dev_e1ed80e2aa3f_s2026.csv |
| msnbc | [0,1] | 2000 | distinct | sbsh-rmax | 20 | 0 | 1120.5 | 1.000 | 0.0016 | 0.0021 | SC3_20260926-011957-740_dev_e1ed80e2aa3f_s2026.csv |

56 rows from 8 files
