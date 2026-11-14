# Superseded: batch g4-sc4-s5 (SC4, HIDE-SEQ, five sensitive patterns, r = 2)

These 34 result files were measured on ADMIN-PC in generation 4. They are kept for the record and read by no
analysis tool: every reader takes the result files at the top level of `results-invariant/` only.

**Why they are superseded.** Under HIDE-SEQ with several sensitive patterns, CUT-HIDE scored a candidate position
by the drop it gives to every sensitive pattern, including the patterns already below the per-sequence limit r in
that sequence. The database-level loop counts only the patterns still needed. The fix makes the per-sequence loop
count only the patterns still at or above r (commit `5eb2bf2c`, `CutHide.sensitiveDropPerPosition`).

**What the fix changes.** Only the per-sequence loop for several patterns calls the changed method, so only this
batch runs changed code. Checked on the development machine: with the fix, one-pattern HIDE-SEQ cells and
five-pattern HIDE-DB cells give the same rows as the measured files (360 of 360 and 56 of 56).

**Where the new files are.** The same 34 cells run again as batch `g5-sc4-s5` (preregistration section 22.7);
their files sit at the top level of `results-invariant/`, and the ledger is `campaign_ADMIN-PC_g5/cells.tsv`.
The ledger rows of these files stay in `campaign_ADMIN-PC_g4/cells.tsv`.
