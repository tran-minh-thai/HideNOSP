# Superseded: batch g5-sc4-s5 (SC4, HIDE-SEQ, five sensitive patterns, r = 2)

These 34 result files were measured on ADMIN-PC in generation 5. They are kept for the record and read by no
analysis tool: every reader takes the result files at the top level of `results-invariant/` only.

**Why they are superseded.** Under HIDE-SEQ with several sensitive patterns, the per-sequence greedy of CUT-HIDE
scored a candidate position by the number of patterns, still at or above r, for which the position is vital. A
position of a repeated item can carry several nodes of one pattern, so one mask there can lower that support by
more than one, and this count could not see it. The greedy now scores a position by the exact drop it causes,
summed over the patterns still at or above r and capped per pattern at what it still needs
(`CutHide.exactDropPerPosition`, preregistration section 22.8).

**What the change touches.** Only the per-sequence loop for several patterns, reached under HIDE-SEQ with two or more
sensitive patterns; in this campaign that is this batch alone. The loop over the whole database under HIDE-DB and
every one-pattern path are unchanged.

**Where the new files are.** The same 34 cells run again as batch `g6-sc4-s5`; their files sit at the top level of
`results-invariant/`, and the ledger is `campaign_ADMIN-PC_g6/cells.tsv`. The ledger rows of these files stay in
`campaign_ADMIN-PC_g5/cells.tsv`.
