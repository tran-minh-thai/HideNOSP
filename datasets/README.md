# datasets/

> **Language.** This file is the English translation of the Vietnamese original as of commit `ea39da4`
> (2026-11-02), which stays in the git history. Values, dates, file names and decisions are unchanged;
> decimal commas are written as decimal points.

Raw data does **not** go into git (see `.gitignore`). This directory keeps the provenance, how the
data is obtained and how it is converted, so that someone else can rebuild exactly the files the
paper used.

Every characteristic of a dataset (ρ_rep, β, \|R\|\*, alphabet size, sequence length) is **measured
by SC2**, not copied from someone else's description (§11.2).

## Dataset table (§11.2)

| File | Source | Role (measured by SC2, see `../results-invariant/khao_sat_bo_du_lieu.md`) | Status |
|---|---|---|---|
| `dna_mt745584.txt` | SPMF `MT745584_SPMF.txt` | **Main group**: COVID-19 genome, ρ_rep 0.786; the data type of [R1] | downloaded |
| `msnbc.txt` | SPMF `MSNBC.txt` | **Main group**: ρ_rep 0.323; [R3] uses the MSN version | downloaded |
| `leviathan.txt` | SPMF `LEVIATHAN.txt` | Intermediate: ρ_rep 0.154 but β 0.612 | downloaded |
| `bible.txt` | SPMF `BIBLE.txt` | Intermediate: ρ_rep 0.153 but β 0.500 | downloaded |
| `fifa.txt` | SPMF `FIFA.txt` | Degenerate control: ρ_rep 0.008 | downloaded |
| `sign.txt` | SPMF `SIGN.txt` | Degenerate control: ρ_rep 0.000 | downloaded |
| `bms1.txt` | SPMF `BMS1_spmf.txt` | Double degenerate control: ρ_rep 0.000, β 0.094; used by [R3] | downloaded |
| `bike.txt` | SPMF `BIKE.txt` | Not used: ρ_rep 0.052; the public dataset closest to trucks in form, **not** the same dataset | downloaded |
| `kosarak.txt` | SPMF `kosarak_sequences.txt`, 60 MB | Scaling in SC6 | not downloaded |
| `trucks.txt` | Data of [R20] and [R3] | Needed for SC8 | no public source found yet |
| discretised time series | UCR archive or equivalent | [R1] uses this kind | not obtained yet |

Excluded after looking at the format: **E-Shop** from SPMF (24,026 sequences, mean length 62) is
**multi-element itemsets**, outside the paper's data model of single-item event sequences (§8.3).

The four downloaded files were obtained with `python tools/fetch_datasets.py`, which records the
provenance in `provenance.tsv` (this file **does** go into git: it holds only the URL, download
date, SHA-256 checksum and size).

## Measured sizes

| Dataset | N | Total length | Mean | Longest | \|Σ\| |
|---|---|---|---|---|---|
| dna_mt745584 | 498 | 29,860 | 60.0 | 60 | **4** |
| msnbc | 31,790 | 423,776 | 13.3 | 100 | 17 |
| bike | 21,078 | 153,383 | 7.3 | 53 | 67 |
| sign | 730 | 37,958 | 52.0 | 94 | 267 |
| bms1 | 59,601 | 149,638 | 2.5 | 267 | 497 |
| fifa | 20,450 | 741,092 | 36.2 | 100 | 2,990 |
| leviathan | 5,834 | 197,251 | 33.8 | 100 | 9,025 |
| bible | 36,369 | 787,066 | 21.6 | 100 | 13,905 |

All eight files are single-element itemsets, so the linearised share is 0: they really are
single-item event sequences, not transaction data forced into sequences.

`tools/fetch_datasets.py` writes `provenance.tsv` automatically. For a dataset downloaded by hand,
add one row following exactly the columns of that file: name, file, format, URL, download date,
number of bytes, SHA-256, role.

## The two formats that `io.DatasetReader` reads

**`PLAIN`** — one sequence per line, item codes separated by whitespace:

```
1 2 1 1 3 2
4 4 1 2
```

**`SPMF`** — the sequence format of the SPMF library: items separated by whitespace, `-1` closes an
itemset, `-2` closes a sequence:

```
1 -1 2 3 -1 1 -1 -2
```

Empty lines and lines starting with `#` or `@` are skipped.

## Multi-element itemsets

The paper's data model is sequences of **single-item events** (§8.3). An itemset with more than one
element lies outside that model. `DatasetReader` linearises it by sorting by increasing item code —
a deterministic rule — and returns `linearisedShare`, the share of positions that come from such
itemsets. Rule 2.2.10 requires this share to be declared next to the dataset; SC2 prints it.

If that share is large, the dataset is not really a single-item event sequence and its exclusion
from the group that demonstrates the contribution must be considered, not merely noted.

## Synthetic data

`io.SyntheticGenerator` generates data with controlled ρ_rep and β, with a seed per file
(rules 2.6.7, 4.2.4). Report only the ρ_rep and β **measured** with `SyntheticGenerator.measure`,
not the target values as if they had been reached.

## Addition of 2026-09-25 — process event logs and the databases of HAUP-Hider

Obtained with `python tools/fetch_datasets.py` (raw files into `datasets/raw/`, MD5 checked as
published by the source), built with `python tools/build_datasets.py` (the role-assignment rules
are at the top of that file). Every number below is **measured** on the built files; the grouping
column is an SC2 **probe** on the development machine (`../results-probe/SC2_20260925-122*`), and
the official grouping waits for SC2 on the measuring machine.

**Role-assignment table of the process event logs** (a design decision, to be declared in the
paper): a case (loan application, incident, patient) is a sequence; an event is a position; the
item is the activity (BPI 2012: activity name of the COMPLETE events; BPI 2013: status/sub-status;
Sepsis: activity name); order by timestamp, events with the same timestamp keep their order in the
file.

| Dataset | Source | N | Events | \|Σ\| | Tied timestamps | Median ρ_rep (all patterns / μ=1 only) | β | Grouping (probe) |
|---|---|---|---|---|---|---|---|---|
| `bpi2013` | 4TU, BPI Challenge 2013 incidents | 7,554 | 65,533 | 13 | 0.0% | [0,1] σ=1000: 0.254 / 0.021; [0,3] σ=500: 0.236 / 0.135; the other two configurations 0.13 | 0.11–0.45 | By the letter of the locked rule: **main group** in 2/4 configurations. Counting only patterns without repeated items: intermediate or degenerate |
| `bpi2012` | 4TU, BPI Challenge 2012 | 13,087 | 164,506 | 23 | 8.5% | 0.000 / 0.000 in all 4 configurations (repeated-item patterns: 0.00–0.75; 3/4 configurations from 0.48 up) | 0.06–0.21 | Degenerate for the scope μ(P) = 1 |
| `sepsis` | 4TU, Sepsis Cases | 1,050 | 15,214 | 16 | **29.2%** | 0.048 / 0.000 and 0.000 / 0.000 (repeated-item patterns: 0.28–0.51) | 0.20–0.53 | Degenerate; the share of tied timestamps is large, so consider excluding it from the group that demonstrates the contribution |

**Databases of HAUP-Hider** (`haup_sdb1` … `haup_sdb14`, repository `wuc567/Pattern-Mining` at commit
`37c82b7`, the repository declares no license; downloaded only for evaluation, not redistributed).
Measurement of the traces of flattened itemsets (share of ascending adjacent pairs and length of
ascending runs):

| Dataset | Ascending adjacent pairs | Ascending run mean / longest | Reading |
|---|---|---|---|
| `haup_sdb2`, `haup_sdb7`–`haup_sdb14` | 0.90 | **9.00 / 9** | Every ascending run has length exactly 9: itemsets of 9 elements sorted ascending, then concatenated. Matches the E-Shop figures (283 items; SDB2: 766 sequences, 47,322 items) |
| `haup_sdb3`, `haup_sdb4`, `haup_sdb5` | 0.86–0.91 | 6.2–9.3 / 12–58 | Flattened itemsets; SDB4, SDB5 match OnlineRetail-Best, OnlineRetail-All |
| `haup_sdb1` | 0.63 | 2.52 / 6 | Matches Babysale (6 items, 359 sequences; mean 2.49 items per itemset) |
| `haup_sdb6` | 0.64 | 2.66 / 14 | Is SIGN, a dataset already present (`sign.txt`) |
| reference: `msnbc`, `bible`, the logs | 0.42–0.51 | 1.6–1.9 / 5–24 | Real order |

Role: **13/14 datasets lie outside the paper's data model** (the order within an itemset is
artificial, so the gap constraint has no meaning). Used only to compare with HAUP-Hider on its own
data, with that role declared; not used as evidence for the contribution.

**Side finding on `bms1`:** 100% of adjacent pairs are ascending — every sequence has been sorted
by item code, and the real click order is lost. This dataset remains a degenerate control; it must
not be used as evidence for anything that depends on order (gap constraint, SC5).

## Addition of 2026-09-25 (2) — proteins of the non-overlapping mining line

Source: the six files `SDB1` … `SDB6` published with NTP-Miner in the repository
`wuc567/Pattern-Mining`, pinned at commit `0b3d4e5`; downloaded with `python tools/fetch_datasets.py`,
built with `python tools/build_datasets.py` (rule `letters`: one line is one protein, one amino-acid
letter is one item; a character that is not a letter is an error). Every number below is
**measured** on the built files; verbatim output of the builder:
`../results-probe/protein_build_log_20260925.txt`.

**The six files are nested, so they are ONE data source, not six datasets.** Measured by matching
whole sequences: every sequence of SDB2 is in SDB1, of SDB3 in SDB2, of SDB5 in SDB4, of SDB6 in
SDB5; SDB1 and SDB4 share 378 sequences (75% of SDB1, 64% of SDB4). Only the two largest files are
built; the other four keep their raw files because they are the evidence of the nesting.

| Dataset | Raw file | N | Events | \|Σ\| | Mean length / longest | σ (chosen by rule, before any SC2 run) | Grouping (probe) |
|---|---|---|---|---|---|---|---|
| `protein_sdb4` | SDB4 | 590 | 109,424 | 20 | 185.5 / 653 | 500, 900 | **Main group**, gap constraint effective, all 4 configurations |
| `protein_sdb1` | SDB1 | 507 | 91,875 | 20 | 181.2 / 994 | 400, 800 | **Main group**, gap constraint effective, all 4 configurations |

Median ρ_rep, β and candidate pool size of each configuration: table 1 of
`../results-probe/protein_groups_and_sigma_rule_probe_20260925.md` (produced by
`tools/data_regime_tables.py`). The official grouping waits for SC2 on the measuring machine
(`run.txt` item 5a).

**Role:** demonstrating the contribution (main group). Because the two files share most of their
sequences, they **count as one dataset**; which file goes into the H2 test is a decision still
open; once locked, it will be recorded as an addendum section of `../config/preregistration.md`,
before any SC3 run on proteins.
