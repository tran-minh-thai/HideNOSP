# results/

**Machine-dependent** measurements: wall-clock time and CPU time.

Only the measuring machine declared in `config/protocol.yaml` (row 5) writes here. Runs pass
`--machine=<machine name>`; `hidenosp.io.ResultSink` refuses a run with the default label `dev`.
A file here has at least one timing column; a file without one belongs in `results-invariant/`.

Repetitions follow row 8 of `config/protocol.yaml` (under 1 second: 15; 1 to 10 seconds: 10; 10 to 120
seconds: 5; over 120 seconds: 3), after one discarded warm-up run. `hidenosp.util.Timing` implements
the table and records the number of discarded runs.

`paper_numbers_timing.json` holds every running time and the platform description quoted in the paper,
written by `tools/paper_numbers.py`.
