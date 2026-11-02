# results/

> **Language.** This file is the English translation of the Vietnamese original as of commit `5ffe329`
> (2026-11-02), which stays in the git history. Values, dates, file names and decisions are unchanged;
> decimal commas are written as decimal points.

**Machine-dependent** measurements: wall-clock time, CPU time, peak memory.

Only the measuring machine declared in `config/protocol.yaml` (rows 5 to 7) writes here. Run
with `--machine=<machine name>`; a run with the default label `dev` is refused by
`hidenosp.io.ResultSink`.

A file here must have at least one timing or memory column. If it has none, it belongs in
`results-invariant/`.

Number of repetitions according to the table of §11.6 (under 1 second: ≥ 15 repetitions; 1–10
seconds: ≥ 10; 10–120 seconds: 5; over 120 seconds: 3 with a reason). `hidenosp.util.Timing`
implements exactly this table and also records the number of discarded warm-up runs.
