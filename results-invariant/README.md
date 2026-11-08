# results-invariant/

**Machine-invariant** measurements: Lost, Ghost, |R|, HF, SupDist, distortion rate, Nettree size. Any
machine that runs them gives the same values, so `hidenosp.io.ResultSink` refuses a file here with a
timing or memory column.

File name: `<scenario>_<UTC time>_<machine>_<code version>_s<seed>.csv`; the header of each file repeats
the run identifier, the options of the run, the JDK and the machine.

- `campaign_<machine>*/`: the cell ledger, environment lock (`ENV_LOCK.json`) and console logs of each
  campaign run by `tools/campaign.py`.
- `paper_numbers.json`: every count, ratio and test result quoted in the paper, each with the tool and
  output line it was read from, written by `tools/paper_numbers.py`.
- `*_analysis_*.txt`, `sc3_*`: reports written by the table tools of `tools/` from the files of this
  directory.

Result files written before 2026-09-26 record commit ids that `COMMIT_MAP.tsv` maps to the current
history.
