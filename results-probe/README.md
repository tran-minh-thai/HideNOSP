# results-probe/

Runs started with `--probe` write their invariant files here instead of `results-invariant/`. These files
are exploratory: no number of the paper is read from this directory. Files here carry no time or memory
column; `tools/check_results.py` enforces that rule.

The exploratory records cited in `config/preregistration.md` are kept in the git history at commit
`abf2f20`; read one with `git show abf2f20:results-probe/<file name>`.
