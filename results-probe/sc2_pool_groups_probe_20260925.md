# Groups under preregistration section 10, from SC2 --profile=pool (probe, 2026-09-25)

Kind: **probe** (development machine, macOS arm64, OpenJDK 26.0.1, bytecode target 25), run from a
clean clone of commit `4f3fa78` with the commands of run.txt section 5a plus `--no-build --probe`.
The groups are a property of the data and do not depend on the machine, but the table is **not**
locked from these files: `config/nhom_cau_hinh.tsv` is written from the official runs on the
measuring machine. Computed with `tools/thong_ke.py` (`xep_nhom`), medians over every frequent
distinct-item pattern of length 2 to 4. Run log: `sc2_pool_probe_log_20260925.txt` (558 s in all).

| dataset | gap | sigma | pool | rho_rep | beta | group | beta |
|---|---|---:|---:|---:|---:|---|---|
| dna_mt745584 | [0,1] | 2500 | 9 | 0.822 | 0.441 | main | effective |
| dna_mt745584 | [0,3] | 2500 | 23 | 0.818 | 0.360 | main | effective |
| dna_mt745584 | [0,10] | 2500 | 60 | 0.848 | 0.052 | main | between |
| dna_mt745584 | [0,1] | 4000 | 0 | — | — | no candidate: the cell does not exist | — |
| dna_mt745584 | [0,3] | 4000 | 4 | 0.890 | 0.238 | main | effective |
| dna_mt745584 | [0,10] | 4000 | 19 | 0.892 | 0.037 | main | degenerate |
| msnbc | [0,1] | 1000 | 226 | 0.141 | 0.503 | intermediate | effective |
| msnbc | [0,3] | 1000 | 434 | 0.127 | 0.315 | intermediate | effective |
| msnbc | [0,10] | 1000 | 886 | 0.142 | 0.074 | intermediate | between |
| msnbc | [0,1] | 2000 | 98 | 0.215 | 0.407 | main | effective |
| msnbc | [0,3] | 2000 | 178 | 0.171 | 0.253 | intermediate | effective |
| msnbc | [0,10] | 2000 | 294 | 0.184 | 0.044 | intermediate | degenerate |
| msnbc | [0,1] | 5000 | 28 | 0.350 | 0.244 | main | effective |
| msnbc | [0,3] | 5000 | 38 | 0.282 | 0.143 | main | between |
| msnbc | [0,10] | 5000 | 63 | 0.279 | 0.021 | main | degenerate |
| leviathan | [0,1] / [0,3] | 500 | 14 / 40 | 0.111 / 0.109 | 0.588 / 0.591 | intermediate | effective |
| bible | [0,1] / [0,3] | 2000 | 17 / 37 | 0.119 / 0.109 | 0.520 / 0.475 | intermediate | effective |
| bpi2013 | four configurations | 500, 1000 | 34–113 | 0.035–0.045 | 0.149–0.443 | degenerate | — |
| bpi2012 | four configurations | 2000, 4000 | 42–384 | 0.000 | 0.006–0.181 | degenerate | — |
| sepsis | [0,1] / [0,3] | 300 | 60 / 305 | 0.000 | 0.440 / 0.137 | degenerate | — |
| fifa | [0,1] / [0,3] | 3000 | 5 / 30 | 0.008 / 0.007 | 0.384 / 0.303 | degenerate | — |
| sign | [0,1] / [0,3] | 200 | 8 / 15 | 0.000 | 0.338 / 0.109 | degenerate | — |
| bms1 | [0,1] / [0,3] | 100 | 259 / 376 | 0.000 | 0.000 | degenerate | degenerate |

Counts: 9 main, 9 intermediate, 16 degenerate, 1 without candidates. The main group is still two
datasets. Main with an effective gap constraint (beta >= 0.20): DNA [0,1] and [0,3] at 2500, DNA
[0,3] at 4000 (pool of 4), MSNBC [0,1] at 2000 and 5000. Compared with the table of preregistration
section 3 (all sampled patterns): MSNBC [0,3] sigma=2000 leaves the main group.

Also here: `SC3_20260925-1312*`, `-131504-569` (commit `a102f64`): the rewritten HHA and CUT-HIDE
on DNA (|S| = 1), MSNBC (|S| = 5) and BPI 2013 (|S| = 1); HF = 0 on every row, 10 s, 125 s, 10 s.
