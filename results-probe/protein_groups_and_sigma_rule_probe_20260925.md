# Protein data and the sigma rule (probe, 2026-09-25)

Kind: **probe** (development machine, macOS arm64, OpenJDK 26.0.1, bytecode target 25), run with
`tools/run.py --no-build --probe` from a clean clone of commit `a548e53`, after
`tools/fetch_datasets.py` (37 files match their records) and `tools/build_datasets.py` (19 builds
match their records). The counts and groups below are properties of the data and do not depend on
the machine; the official group table is still written from the SC2 runs of the measuring machine
(run.txt section 5a). Every table is printed by `tools/data_regime_tables.py` from the files named
in its last column; the run logs are `sigma_rule_scan_log_20260925.txt` and
`sc2_pool_protein_log_20260925.txt`.

## 1. Groups of the protein configurations (SC2 --profile=pool)

Sigma for the protein files was fixed before any SC2 run on them, by one rule: the smallest and the
largest sigma of the scanned grid (300 to 1200) that meet the sigma rule at both gaps (table 2).

Command: `python tools/data_regime_tables.py groups results-probe/SC2_*_a548e53690c5_*.csv`

| dataset | gap | sigma | pool | median rho_rep | median beta | group | beta group | file |
|---|---|---:|---:|---:|---:|---|---|---|
| protein_sdb1 | [0,1] | 400 | 181 | 0.495 | 0.829 | main | effective | SC2_20260925-141108-164_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,1] | 800 | 31 | 0.597 | 0.796 | main | effective | SC2_20260925-141139-452_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,3] | 400 | 411 | 0.536 | 0.739 | main | effective | SC2_20260925-141116-141_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,3] | 800 | 157 | 0.645 | 0.691 | main | effective | SC2_20260925-141141-985_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,1] | 500 | 177 | 0.490 | 0.831 | main | effective | SC2_20260925-141022-488_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,1] | 900 | 38 | 0.583 | 0.802 | main | effective | SC2_20260925-141054-116_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,3] | 500 | 371 | 0.552 | 0.731 | main | effective | SC2_20260925-141031-346_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,3] | 900 | 178 | 0.640 | 0.700 | main | effective | SC2_20260925-141057-353_dev_a548e53690c5_s2026.csv |

8 configurations from 8 pool files (8 files given)

## 2. The sigma rule on every dataset (SIGMA)

Rule of config/protocol.yaml: |F_sigma| within [50, 700] and F_sigma a proper subset of the pattern
space up to l_max = 4. The protein rows scan a grid; the other rows check the sigma values that the
protocol already fixes, at the gaps it fixes.

Command: `python tools/data_regime_tables.py sigma results-probe/SIGMA_*_a548e53690c5_*.csv`

| dataset | gap | sigma | \|F_sigma\| | by length 1/2/3/4 | pool | rule | file |
|---|---|---:|---:|---|---:|---|---|
| protein_sdb4 | [0,1] | 300 | 309 | 20/285/4/0 | 270 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,1] | 400 | 257 | 20/236/1/0 | 224 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,1] | 500 | 208 | 20/188/0/0 | 177 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,1] | 600 | 164 | 20/144/0/0 | 135 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,1] | 700 | 120 | 20/100/0/0 | 92 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,1] | 800 | 88 | 20/68/0/0 | 62 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,1] | 900 | 63 | 20/43/0/0 | 38 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,1] | 1000 | 49 | 20/29/0/0 | 25 | **outside** | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,1] | 1200 | 28 | 20/8/0/0 | 7 | **outside** | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,3] | 300 | 1525 | 20/356/1145/4 | 1226 | **outside** | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,3] | 400 | 742 | 20/324/397/1 | 572 | **outside** | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,3] | 500 | 469 | 20/298/150/1 | 371 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,3] | 600 | 334 | 20/272/42/0 | 271 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,3] | 700 | 275 | 20/245/10/0 | 233 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,3] | 800 | 247 | 20/223/4/0 | 211 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,3] | 900 | 211 | 20/190/1/0 | 178 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,3] | 1000 | 192 | 20/171/1/0 | 159 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb4 | [0,3] | 1200 | 133 | 20/112/1/0 | 104 | meets | SIGMA_20260925-140404-872_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,1] | 300 | 271 | 20/248/3/0 | 235 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,1] | 400 | 212 | 20/192/0/0 | 181 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,1] | 500 | 162 | 20/142/0/0 | 132 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,1] | 600 | 116 | 20/96/0/0 | 89 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,1] | 700 | 77 | 20/57/0/0 | 52 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,1] | 800 | 55 | 20/35/0/0 | 31 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,1] | 900 | 39 | 20/19/0/0 | 15 | **outside** | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,1] | 1000 | 31 | 19/12/0/0 | 9 | **outside** | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,1] | 1200 | 24 | 19/5/0/0 | 4 | **outside** | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,3] | 300 | 977 | 20/341/611/5 | 762 | **outside** | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,3] | 400 | 535 | 20/308/206/1 | 411 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,3] | 500 | 347 | 20/271/56/0 | 277 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,3] | 600 | 283 | 20/237/26/0 | 233 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,3] | 700 | 230 | 20/200/10/0 | 189 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,3] | 800 | 192 | 20/169/3/0 | 157 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,3] | 900 | 156 | 20/135/1/0 | 124 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,3] | 1000 | 132 | 19/112/1/0 | 101 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| protein_sdb1 | [0,3] | 1200 | 80 | 19/61/0/0 | 55 | meets | SIGMA_20260925-140727-283_dev_a548e53690c5_s2026.csv |
| dna_mt745584 | [0,1] | 2500 | 16 | 4/11/1/0 | 9 | **outside** | SIGMA_20260925-140941-165_dev_a548e53690c5_s2026.csv |
| dna_mt745584 | [0,1] | 4000 | 6 | 4/2/0/0 | 0 | **outside** | SIGMA_20260925-140941-165_dev_a548e53690c5_s2026.csv |
| dna_mt745584 | [0,3] | 2500 | 67 | 4/15/32/16 | 23 | meets | SIGMA_20260925-140941-165_dev_a548e53690c5_s2026.csv |
| dna_mt745584 | [0,3] | 4000 | 14 | 4/6/3/1 | 4 | **outside** | SIGMA_20260925-140941-165_dev_a548e53690c5_s2026.csv |
| dna_mt745584 | [0,10] | 2500 | 324 | 4/16/64/240 | 60 | meets | SIGMA_20260925-140941-165_dev_a548e53690c5_s2026.csv |
| dna_mt745584 | [0,10] | 4000 | 77 | 4/14/33/26 | 19 | meets | SIGMA_20260925-140941-165_dev_a548e53690c5_s2026.csv |
| msnbc | [0,1] | 1000 | 664 | 16/156/241/251 | 226 | meets | SIGMA_20260925-140945-106_dev_a548e53690c5_s2026.csv |
| msnbc | [0,1] | 2000 | 309 | 16/85/106/102 | 98 | meets | SIGMA_20260925-140945-106_dev_a548e53690c5_s2026.csv |
| msnbc | [0,1] | 5000 | 107 | 15/36/35/21 | 28 | meets | SIGMA_20260925-140945-106_dev_a548e53690c5_s2026.csv |
| msnbc | [0,3] | 1000 | 1335 | 16/185/471/663 | 434 | **outside** | SIGMA_20260925-140945-106_dev_a548e53690c5_s2026.csv |
| msnbc | [0,3] | 2000 | 556 | 16/122/189/229 | 178 | meets | SIGMA_20260925-140945-106_dev_a548e53690c5_s2026.csv |
| msnbc | [0,3] | 5000 | 174 | 15/46/58/55 | 38 | meets | SIGMA_20260925-140945-106_dev_a548e53690c5_s2026.csv |
| msnbc | [0,10] | 1000 | 2352 | 16/205/866/1265 | 886 | **outside** | SIGMA_20260925-140945-106_dev_a548e53690c5_s2026.csv |
| msnbc | [0,10] | 2000 | 901 | 16/151/329/405 | 294 | **outside** | SIGMA_20260925-140945-106_dev_a548e53690c5_s2026.csv |
| msnbc | [0,10] | 5000 | 233 | 15/56/87/75 | 63 | meets | SIGMA_20260925-140945-106_dev_a548e53690c5_s2026.csv |
| leviathan | [0,1] | 500 | 73 | 55/15/2/1 | 14 | meets | SIGMA_20260925-141130-539_dev_a548e53690c5_s2026.csv |
| leviathan | [0,3] | 500 | 108 | 55/39/12/2 | 40 | meets | SIGMA_20260925-141130-539_dev_a548e53690c5_s2026.csv |
| bible | [0,1] | 2000 | 78 | 57/15/6/0 | 17 | meets | SIGMA_20260925-141209-323_dev_a548e53690c5_s2026.csv |
| bible | [0,3] | 2000 | 113 | 57/34/18/4 | 37 | meets | SIGMA_20260925-141209-323_dev_a548e53690c5_s2026.csv |
| sign | [0,1] | 200 | 88 | 80/8/0/0 | 8 | meets | SIGMA_20260925-141609-944_dev_a548e53690c5_s2026.csv |
| sign | [0,3] | 200 | 95 | 80/14/1/0 | 15 | meets | SIGMA_20260925-141609-944_dev_a548e53690c5_s2026.csv |
| bms1 | [0,1] | 100 | 545 | 286/207/47/5 | 259 | meets | SIGMA_20260925-141613-154_dev_a548e53690c5_s2026.csv |
| bms1 | [0,3] | 100 | 662 | 286/289/77/10 | 376 | meets | SIGMA_20260925-141613-154_dev_a548e53690c5_s2026.csv |
| fifa | [0,1] | 3000 | 54 | 46/6/1/1 | 5 | meets | SIGMA_20260925-141621-479_dev_a548e53690c5_s2026.csv |
| fifa | [0,3] | 3000 | 79 | 46/28/4/1 | 30 | meets | SIGMA_20260925-141621-479_dev_a548e53690c5_s2026.csv |
| bpi2013 | [0,1] | 250 | 314 | 10/36/91/177 | 117 | meets | SIGMA_20260925-141739-347_dev_a548e53690c5_s2026.csv |
| bpi2013 | [0,1] | 500 | 195 | 8/26/61/100 | 75 | meets | SIGMA_20260925-141739-347_dev_a548e53690c5_s2026.csv |
| bpi2013 | [0,1] | 1000 | 106 | 8/18/33/47 | 34 | meets | SIGMA_20260925-141739-347_dev_a548e53690c5_s2026.csv |
| bpi2013 | [0,1] | 2000 | 61 | 7/14/19/21 | 18 | meets | SIGMA_20260925-141739-347_dev_a548e53690c5_s2026.csv |
| bpi2013 | [0,3] | 250 | 529 | 10/41/133/345 | 192 | meets | SIGMA_20260925-141739-347_dev_a548e53690c5_s2026.csv |
| bpi2013 | [0,3] | 500 | 325 | 8/31/89/197 | 113 | meets | SIGMA_20260925-141739-347_dev_a548e53690c5_s2026.csv |
| bpi2013 | [0,3] | 1000 | 191 | 8/25/59/99 | 73 | meets | SIGMA_20260925-141739-347_dev_a548e53690c5_s2026.csv |
| bpi2013 | [0,3] | 2000 | 96 | 7/18/30/41 | 33 | meets | SIGMA_20260925-141739-347_dev_a548e53690c5_s2026.csv |
| bpi2012 | [0,1] | 1000 | 302 | 21/58/88/135 | 220 | meets | SIGMA_20260925-141749-371_dev_a548e53690c5_s2026.csv |
| bpi2012 | [0,1] | 2000 | 161 | 21/37/49/54 | 116 | meets | SIGMA_20260925-141749-371_dev_a548e53690c5_s2026.csv |
| bpi2012 | [0,1] | 4000 | 67 | 14/19/19/15 | 42 | meets | SIGMA_20260925-141749-371_dev_a548e53690c5_s2026.csv |
| bpi2012 | [0,1] | 8000 | 13 | 5/4/2/2 | 1 | **outside** | SIGMA_20260925-141749-371_dev_a548e53690c5_s2026.csv |
| bpi2012 | [0,3] | 1000 | 1001 | 21/93/259/628 | 765 | **outside** | SIGMA_20260925-141749-371_dev_a548e53690c5_s2026.csv |
| bpi2012 | [0,3] | 2000 | 474 | 21/64/131/258 | 384 | meets | SIGMA_20260925-141749-371_dev_a548e53690c5_s2026.csv |
| bpi2012 | [0,3] | 4000 | 138 | 14/30/44/50 | 110 | meets | SIGMA_20260925-141749-371_dev_a548e53690c5_s2026.csv |
| bpi2012 | [0,3] | 8000 | 13 | 5/4/2/2 | 1 | **outside** | SIGMA_20260925-141749-371_dev_a548e53690c5_s2026.csv |
| sepsis | [0,1] | 150 | 281 | 11/44/100/126 | 189 | meets | SIGMA_20260925-141824-885_dev_a548e53690c5_s2026.csv |
| sepsis | [0,1] | 300 | 95 | 10/25/35/25 | 60 | meets | SIGMA_20260925-141824-885_dev_a548e53690c5_s2026.csv |
| sepsis | [0,1] | 600 | 32 | 10/13/7/2 | 12 | **outside** | SIGMA_20260925-141824-885_dev_a548e53690c5_s2026.csv |
| sepsis | [0,3] | 150 | 926 | 11/56/224/635 | 591 | **outside** | SIGMA_20260925-141824-885_dev_a548e53690c5_s2026.csv |
| sepsis | [0,3] | 300 | 455 | 10/45/146/254 | 305 | meets | SIGMA_20260925-141824-885_dev_a548e53690c5_s2026.csv |
| sepsis | [0,3] | 600 | 101 | 10/32/36/23 | 61 | meets | SIGMA_20260925-141824-885_dev_a548e53690c5_s2026.csv |

83 rows from 12 files
