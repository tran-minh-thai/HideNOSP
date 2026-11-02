# results-invariant/

> **Language.** This file is the English translation of the Vietnamese original as of commit `5ffe329`
> (2026-11-02), which stays in the git history. Values, dates, file names and decisions are unchanged;
> decimal commas are written as decimal points.

**Machine-invariant** measurements: Lost, Ghost, |R|, HF, SupDist, DistR, Nettree size, share of (S,
k) pairs certified optimal.

Any machine that runs them gives the same result, so no measuring machine needs to be declared. In
return, **timing columns and memory columns are forbidden** here; `hidenosp.io.ResultSink` refuses
to open a file with such columns.

File name: `SCn_<UTC time>_<machine>_<code version>_s<seed>.csv`, and the run identifier stamp is
repeated in the comment header of the file (rule 2.6.8).

Files here are tracked by git: they are the evidence for the tables in the paper.
