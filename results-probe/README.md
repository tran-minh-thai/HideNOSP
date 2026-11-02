# results-probe/

> **Language.** This file is the English translation of the Vietnamese original as of commit `5ffe329`
> (2026-11-02), which stays in the git history. Values, dates, file names and decisions are unchanged;
> decimal commas are written as decimal points.

**Probe** measurements on the development machine, and every run on substitute synthetic data while
no real dataset is available yet.

These numbers **do not enter the paper as results** (§8.2). They are used to: search for
counterexamples, see whether a configuration is worth a full run, and check the pipeline before the
real run.

Run with the flag `--probe`; the invariant file is then redirected here instead of
`results-invariant/`. Timing columns and memory columns are forbidden, as in `results-invariant/`.

The Python probe numbers in `../nettree_checks.py` and `../probe/` also belong to this kind. Those
scripts print to the screen, so their output must be captured into this directory; section 2 of
`../RUNBOOK.md` has the ready-made commands.

`probe/timing.py` prints **seconds**, that is, machine-dependent numbers. It still lives here and
not in `results/`, because it is a probe on the development machine: §8.2 of the research design
places it in the probe group. Its numbers are **never** moved to `results/` or printed in the paper;
to get timing numbers for the paper, run SC6 on the declared measuring machine.

Do not use `>>` to append to a file located on the Google Drive disk: this virtual disk reports the
old file size, so the append overwrites the middle of the file. Write to a local disk and then copy
over.
