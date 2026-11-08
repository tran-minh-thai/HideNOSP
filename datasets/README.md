# datasets/

Data files are not stored in git. `provenance.tsv` records, for every file, the address it comes from,
its size and its SHA-256, so the exact bytes used in the paper can be fetched and checked.

```bash
python3 tools/fetch_datasets.py        # download and verify every file of provenance.tsv
python3 tools/build_datasets.py        # convert the event logs and the protein files to SPMF
```

Both scripts stop with exit code 1 when a file differs from its recorded checksum.

## The twelve datasets of the paper

| File | Source | Conversion | Licence (as stated at the source) |
|---|---|---|---|
| `bible.txt` | SPMF public datasets, `BIBLE.txt` | none | no licence stated ("All rights reserved" on the SPMF site) |
| `bms1.txt` | SPMF, `BMS1_spmf.txt` (KDD Cup 2000, BMS-WebView-1) | none | as above |
| `fifa.txt` | SPMF, `FIFA.txt` (World Cup 98 web log) | none | as above |
| `leviathan.txt` | SPMF, `LEVIATHAN.txt` | none | as above |
| `sign.txt` | SPMF, `SIGN.txt` (ASL sign language, Boston University) | none | as above |
| `msnbc.txt` | SPMF, `MSNBC.txt` (UCI Machine Learning Repository, MSNBC.com Anonymous Web Data, doi 10.24432/C5390X) | none | CC BY 4.0 (UCI) |
| `dna_mt745584.txt` | SPMF, `MT745584_SPMF.txt` (SARS-CoV-2 genome MT745584, NCBI GenBank) | none | see NCBI GenBank policy |
| `bpi2012.txt` | 4TU.ResearchData, BPI Challenge 2012, doi 10.4121/uuid:3926db30-f712-4394-aebc-75976070e91f | one loan application per sequence; item = activity of the events with lifecycle COMPLETE | 4TU General Terms of Use (non-commercial; cite the creator) |
| `bpi2013.txt` | 4TU.ResearchData, BPI Challenge 2013 incidents, doi 10.4121/uuid:500573e6-accc-4b0c-9576-aa5468b10cee | one incident per sequence; item = status/sub-status of every event | as above |
| `sepsis.txt` | 4TU.ResearchData, Sepsis Cases event log, doi 10.4121/uuid:915d2bfb-7e84-49ad-a286-dc35f063a460 | one patient per sequence; item = activity of every event | as above |
| `protein_sdb4.txt` | GitHub `wuc567/Pattern-Mining`, `NTP-Miner/dataset/SDB4.txt` at commit `0b3d4e5` | one protein per sequence; one residue letter per item | no licence stated |
| `protein_sdb1.txt` | as above, `SDB1.txt` | as above | as above |

Conversion rules (`tools/build_datasets.py`): events of a trace are ordered by timestamp, ties keep the
order of the log file, and item codes are assigned in sorted order of the item labels. The published
protein files are nested (SDB3 in SDB2 in SDB1, SDB6 in SDB5 in SDB4), so only SDB1 and SDB4 are built;
`protein_sdb1` shares most sequences with `protein_sdb4` and is reported but never enters a test.

The input format is SPMF: integer item codes separated by `-1`, `-2` ending a sequence. An itemset with
more than one item is linearised in ascending item order (the model of the paper is a sequence of single
events); `hidenosp.io.DatasetReader` records the share of positions this affects.

`provenance.tsv` also lists files that the paper does not use (`bike`, the HAUP-Hider databases
`haup_sdb1` to `haup_sdb14`); they are fetched by the same command.

The sigma values and gap constraints of each dataset are in `config/protocol.yaml`; the group of every
configuration is locked in `config/configuration_groups.tsv` and `config/configuration_groups_repeated.tsv`.
