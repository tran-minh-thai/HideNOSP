Checks of the English translation of the code (2026-09-26)

What: the translation commits d161b70 (renames), 3fc0dee (translation and checker) and c350cdf
(checker fix), checked against their parent c516133. These are probes on the development machine:
every check compares outputs that do not depend on the machine (bytes of classes, test verdicts,
report numbers, labels); no timing number is recorded here.

Environment: macOS 27.0 (arm64), JDK 26.0.1 compiling with --release 25 (the target of pom.xml),
JUnit 5.10.2, Python 3.14.6 with numpy 2.5.3, scipy 1.18.1 and PyYAML 6.0.3, git 2.54.0. Maven was
not used: offline it could not resolve its compiler plugin, so scripts/build.sh compiles with javac
and scripts/runtests.sh runs every JUnit test through the platform launcher (scripts/RunAll.java).
In the outputs and scripts, machine paths are replaced by <checks> (the directory of these
scripts), <python>, <jdk>, <clone of c516133>, <clone of the translated branch> and
<temporary file>; checker outputs name those two clones by their directory names, ubase and final.

Files, and what they show:

outputs/java_bytecode_against_c516133.txt   scripts/build.sh with -g:none on both trees, then
    scripts/compare_classes.py: 114 of 118 classes byte-identical; 4 differ only in string
    constants (1, 4, 7 and 20 strings), their instructions identical once string texts are blanked.
outputs/java_bytecode_injections.txt   the comparison is not blind: one string changed in
    Metrics.java changes exactly Metrics$Context (strings only); one operator (< to <=) changes
    exactly Metrics$Context$Session and is reported as an instruction difference (ifge to ifgt).
outputs/junit_c516133.txt, junit_translated.txt   75 tests found, 75 succeeded on both; the same
    test methods; 12 display names differ (the translated ones).
outputs/statistics_tests_c516133.txt, statistics_tests_translated.txt   tools/thong_ke_test.py and
    tools/preregistered_analysis_test.py: 22 tests, 0 failed on both.
outputs/statistics_tests_injections.txt   the cross-check of preregistration section 12 fails when
    config/protocol.yaml names another method, and when section 12 is missing.
outputs/analysis_on_probe_files.txt   scripts/drive.py runs tools/thong_ke.py (c516133) and
    tools/preregistered_analysis.py on the probe files; scripts/compare.py requires the same exit
    code, line count, every number of every line in order, and the same group labels and verdicts
    after mapping the old words to the new ones: 9 of 9 the same. The group tables written by
    --write-groups (42 and 39 rows) are identical once the old labels are mapped.
outputs/analysis_fuzz_1000_cases.txt   scripts/fuzz.py: 1000 generated cases (SC3 and SC2 files,
    locked tables absent, matching, altered, short or extended), 1715 comparisons, all the same;
    every branch listed was reached but one, NO 'cut-hide', which the list still named with the
    old key of the method under test. analysis_fuzz_200_cases_missing_method.txt names it right:
    reached in 131 of 200 cases, 342 comparisons, all the same.
outputs/analysis_fuzz_injections.txt   the comparison is not blind: DELTA_L 0.02 to 0.03, one group
    label, mean instead of median, and a constant exit code 0, each injected alone, give 142, 108,
    36 and 116 differing comparisons of 257.
outputs/decisions_tool_equivalence.txt   scripts/decisions_equivalence.py feeds tools/gop_quyet_dinh.py
    (c516133, Vietnamese keywords) and tools/merge_decisions.py (English keywords) a parallel
    review: 96 items sorted into the same themes, 4 left unsorted by both, the same sources,
    blocks lines and printed counts. Three logic faults are caught; a changed keyword is not,
    because the items are built from each tool's own keywords (see below).
outputs/tables_and_probes.txt   tools/data_regime_tables.py, comparison_table.py and
    pareto_table.py, and the deterministic probes, give byte-identical output on both trees.
outputs/provenance_protocol_git_rules.txt   datasets/provenance.tsv agrees with
    tools/fetch_datasets.py and survives a rewrite byte for byte; config/protocol.yaml keeps 110
    values under the key map (25 strings translated); git ignores and attributes 349 paths the
    same way on both trees.
outputs/checker_on_c516133.txt   tools/check_english.py on c516133: 3284 violations in 24 files,
    the files this translation changed. checker_on_translated_tree.txt: none.
outputs/checker_line_recall_on_c516133.txt   of the 1299 lines the translation changed or removed
    in those 24 files, 1124 are flagged by the checker at c516133; each of the other 175 is
    listed. They are English lines that were only rewrapped or renamed, short Vietnamese names
    that are also English words or two letters long (ra, ten, ho, gan), and a few strings made
    only of such words; the docstring of the checker lists these blind spots.
outputs/checker_self_test.txt   tools/check_english_test.py: 30 tests, 0 failed.
    checker_self_test_on_previous_checker.txt: the same tests against the checker of 3fc0dee fail
    exactly the digest test, the case c350cdf fixes.
outputs/word_list_measurement.tsv   tools/check_english.py --measure at c350cdf: every listed word,
    counted per class of file; no WORDS_ANYWHERE word occurs in code or in an English line. The
    list itself was drawn before from the Vietnamese text (diacritics removed) of the documents of
    this repository and of the design document and manuscript beside it, keeping a word only when
    it matched nothing in the English text of the repository.

Not verified, and why:
- The English keywords of tools/merge_decisions.py: no English review exists, so whether they
  sort a real review as the Vietnamese ones did cannot be tested; only the sorting logic is.
- The generated cases of scripts/fuzz.py are synthetic; the probe files are real but hold no row
  of the method under test (cut-hide-rw-ls), so that branch is covered by the generated cases.
- probe/timing.py prints seconds; it ran but is not compared.

Found on the way, not changed: tools/gop_quyet_dinh.py (and merge_decisions.py, the same logic)
prints the longest blocks text of a theme, and among texts of equal length the one printed follows
Python's hash seed (the last lines of decisions_tool_equivalence.txt: 5 themes vary across seeds 1
to 4). Sorting by (length, text) would make it deterministic.
