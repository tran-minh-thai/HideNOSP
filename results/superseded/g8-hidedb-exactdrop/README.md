# Superseded: the repeated-item cells of CUT-HIDE under HIDE-DB with several patterns (generation 8)

These files were measured on ADMIN-PC in generations 3, 4 and 7. They are kept for the record and read by no
analysis tool: every reader takes the result files at the top level of `results-invariant/` and `results/` only.

| scenario | files | earlier batches |
|---|---|---|
| SC3 | 131 | g3-sc3-s5, g3-sc3-s10, g3-sc3-s20, g3-sc3-refine, g4-gap-s5, g4-gap-s10, g4-gap-s20 |
| SC3OPT | 52 | g3-sc3opt |
| SC3ROB and SC3ROBSUP | 52 and 52 | g7-rob-s5, g7-rob-s10, g7-rob-s20 (each run writes both files) |
| SC3REP | 2 | g7-rep-s5, g7-rep-s10 |
| SC6 (in `results/superseded/g8-hidedb-exactdrop/`) | 18 | g4-sc6 |

**Why they are superseded.** Under HIDE-DB with several sensitive patterns, the greedy of CUT-HIDE over the whole
database scored a candidate position by the number of patterns, still above the threshold, for which the position is
vital, divided by 1 + w(i). A position of a repeated item can carry several nodes of one pattern, so one mask there can
lower that support by more than one, and this count could not see it. The greedy now scores a position by the exact drop
it causes, summed over the patterns still above the threshold and capped per pattern at what it still needs
(`CutHideConfig.exactDropDatabase`, preregistration section 23.9). The same change was made to the per-sequence greedy
under HIDE-SEQ in section 22.8.

**What the change touches.** Only cells with repeated-item patterns and two or more sensitive patterns under HIDE-DB.
With distinct items every drop is one, so the two scores are equal; the test
`ReopenedScenariosTest` checks that the rows are identical on such cells.

**Where the new files are.** The same 255 cells run again as the batches `g8-*` listed in `tools/campaign.py`
(`G8_SOURCES`); their files sit at the top level, and the ledger is `results-invariant/campaign_ADMIN-PC_g8/cells.tsv`.
The ledger rows of these files stay in the ledgers of their own generations.
