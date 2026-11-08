# SPDX-License-Identifier: MIT
"""Self-test of check_english.py: every kind of violation it claims to catch is injected into a
small throw-away repository, one at a time, and must be reported -- in the injected file only --
while a set of English files that look Vietnamese to a naive list must stay clean.

Every sample with a diacritic, and the section sign, is written as an escape: this file is checked
by check_english.py like any other code file, with only its word rule switched off.

Run: python tools/check_english_test.py
Exit code 1 when a test fails.
"""
import os
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
CHECKER = os.path.join(HERE, "check_english.py")

# English files that must pass: syllables that are also English words, stand-alone or in free
# text, English splits, a cited theorem, a documentation path in its own language, URLs, digests.
CLEAN = {
    "src/main/java/demo/Clean.java": (
        "package demo;\n\n"
        "/** The cat sat on the mat; a bucket of neutrons; Theorem 2 of Abul et al. (TKDE 2010).\n"
        " */\n"
        "public final class Clean {\n"
        "    // to_do: gap_min and chi2 are fine, and so is tan(x) or sinh(x)\n"
        "    private static final String URL = \"https://data.4tu.nl/file/533f66a4-8911\";\n"
        "    int soMany = 3;  // so + many\n"
        "}\n"),
    "tools/clean_tool.py": (
        "\"\"\"Print the cut of a Nettree (doc/THIET_KE_NGHIEN_CUU_AN_MAU_LAP.md; Section 4 of that paper).\n"
        "\"\"\"\n"
        "DIGEST = \"dbcbeedd1bc947829fee398dc346e87545fc52bf0ff314e924669e0f0d9b4f3e\"\n"
        "def main():\n"
        "    print(\"hat and cat, dot and tap: English words, one at a time\")\n"),
    "config/settings.yaml": "row_1_thresholds:\n  sigma: [100, 200]   # preregistration section 9\n",
    "config/table.tsv": "key\tlabel\ncut-hide\tCUT-HIDE\n",
    ".gitignore": "target/\n",
    "README.md": "# Notes\nSee config/quyet_dinh_can_chot.md and \u00a711.8 of the design; "
                  "the library folder ThuVienBaiBao/14_PPDM_AnGiauLuatNhayCam keeps its name.\n",
    "results-probe/probe_log.txt": "chay xong, khong co loi\n",
    "target/ignored.py": "# khong duoc theo doi: git bo qua tep nay\n",
}
N_CLEAN_CODE = 5   # Clean.java, clean_tool.py, settings.yaml, table.tsv, .gitignore


def make_repo(extra=None, track_extra=True):
    root = tempfile.mkdtemp(prefix="check_english_")
    subprocess.run(["git", "init", "-q", root], check=True)
    files = dict(CLEAN)
    if track_extra and extra:
        files.update(extra)
    for path, content in files.items():
        write(root, path, content)
    subprocess.run(["git", "-C", root, "add", "-A"], check=True)
    if extra and not track_extra:
        for path, content in extra.items():
            write(root, path, content)
    return root


def write(root, path, content):
    full = os.path.join(root, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    if isinstance(content, bytes):
        with open(full, "wb") as handle:
            handle.write(content)
    else:
        with open(full, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(content)


def run(root, *options):
    out = subprocess.run([sys.executable, CHECKER, "--root", root] + list(options),
                         capture_output=True, text=True,
                         env=dict(os.environ, PYTHONDONTWRITEBYTECODE="1"))
    return out.returncode, out.stdout + out.stderr


def violations(output):
    """{path: [kind, ...]} from the report."""
    found = {}
    for line in output.splitlines():
        parts = line.strip().split(":", 2)
        if line.startswith("  ") and len(parts) == 3 and parts[1].isdigit():
            found.setdefault(parts[0], []).append(parts[2].strip().split(":", 1)[0])
    return found


def expect_caught(path, content, kind, track=True):
    """Inject one file, and require exit 1 with violations of that kind in that file only."""
    root = make_repo({path: content}, track_extra=track)
    try:
        assert os.path.exists(os.path.join(root, path)), "the injection did not land: " + path
        code, output = run(root)
        found = violations(output)
        assert code == 1, "exit %d, expected 1\n%s" % (code, output)
        assert set(found) == {path}, \
            "violations in %s, expected only %s\n%s" % (sorted(found), path, output)
        assert kind in found[path], \
            "no %s violation in %s: %s\n%s" % (kind, path, found[path], output)
    finally:
        shutil.rmtree(root)


def test_clean_repository_passes_and_counts_its_files():
    root = make_repo()
    try:
        code, output = run(root)
        assert code == 0, output
        assert "Checked %d code files" % N_CLEAN_CODE in output, output
        assert "Checked 1 documentation files" in output and "1 run-output files" in output, output
    finally:
        shutil.rmtree(root)


def test_diacritic_in_a_java_comment():
    expect_caught("src/main/java/demo/A.java",
                  "class A { // m\u1eabu nh\u1ea1y c\u1ea3m\n}\n", "diacritic")


def test_diacritic_in_a_printed_string():
    expect_caught("tools/a.py", "print(\"\u0111\u00e3 ghi\")\n", "diacritic")


def test_decomposed_diacritic_is_found_after_normalisation():
    expect_caught("config/a.yaml", "note: \"cha\u0306m\"\n", "diacritic")


def test_upper_case_letter_and_d_with_stroke():
    expect_caught("config/b.yaml", "note: \"\u0110\u1ea0I\"\n", "diacritic")


def test_unaccented_word_in_a_comment():
    expect_caught("tools/b.py", "# khong duoc chay khi chua co du lieu\nx = 1\n", "word")


def test_unaccented_word_in_an_identifier():
    expect_caught("tools/c.py", "def doc_bang_nhom(path):\n    return path\n", "word")


def test_short_syllable_with_a_syllable_that_is_also_english():
    expect_caught("tools/d.py", "so_lan = 3\n", "word")


def test_two_syllables_that_are_also_english():
    expect_caught("tools/e.py", "thong_tin = {}\n", "word")


def test_camel_case_identifier():
    expect_caught("src/main/java/demo/B.java", "class B { int soLan = 3; }\n", "word")


def test_kebab_case_option_in_a_shell_script():
    expect_caught("run_all.sh", "python tools/x.py --bo-qua-xep-nhom\n", "word")


def test_printed_string_in_powershell():
    expect_caught("run_all.ps1", "Write-Host \"Tep da ghi\"\n", "word")


def test_batch_file():
    expect_caught("run_all.bat", "echo Chay xong\n", "word")


def test_cmd_file():
    expect_caught("run_all.cmd", "rem Kiem tra truoc khi chay\n", "word")


def test_tsv_header():
    expect_caught("config/labels.tsv", "khoa\tnhan_vi\tnhan_en\n", "word")


def test_file_name_with_separators():
    expect_caught("tools/thong_ke.py", "print(\"statistics\")\n", "word")


def test_file_name_without_separators():
    expect_caught("kiemtra_v2.py", "print(\"checks\")\n", "word")


def test_section_sign_followed_by_a_number():
    expect_caught("src/main/java/demo/C.java", "class C { // see \u00a711.5 \n}\n", "numbering")


def test_theorem_number_with_diacritics():
    expect_caught("run_theorem.bat", "echo \u0110\u1ecbnh l\u00fd %d\n" % 17, "numbering")


def test_theorem_number_without_diacritics():
    expect_caught("config/c.yaml", "note: \"Dinh ly %d\"\n" % 17, "numbering")


def test_corollary_with_a_design_document_label():
    expect_caught("src/main/java/demo/D.java", "class D { // He qua P%dc\n}\n" % 1, "numbering")


def test_rule_number_of_an_internal_document():
    expect_caught("src/main/java/demo/E.java", "class E { // as rule 2.3.4 asks\n}\n", "reference")


def test_design_document_section():
    expect_caught("tools/h.py", "# the gap constraint (design doc section 2)\n", "reference")


def test_bare_section_number():
    expect_caught("src/main/java/demo/F.java", "/** SC1 of section 11.1: the check. */\nclass F {}\n", "reference")


def test_bare_section_number_in_a_printed_string():
    expect_caught("tools/i.py", "print(\"see section 8.4\")\n", "reference")


def test_rule_number_is_caught_in_a_preregistration_reader():
    expect_caught("tools/preregistered_analysis.py", "# reported under rule 3.4.9\n", "reference")


def test_cited_sections_are_clean():
    root = make_repo({
        "src/main/java/demo/G.java": "/** (preregistration section 19.1); its Section 4.3; Section 5.1 of that paper. */\nclass G {}\n",
        "tools/preregistered_analysis.py": "ALPHA = 0.05  # section 2\n",
        "tools/check_english_test.py": "SAMPLE = 'design doc section 11.5, rule 2.3.4'\n",
    })
    try:
        code, output = run(root)
        assert code == 0, output
    finally:
        shutil.rmtree(root)


def test_digest_with_listed_letters_is_not_a_word():
    root = make_repo({"config/digests.tsv": (
        "name\tsha256\tuuid\n"
        "x\t0f9dac7e3cac1b7cacdac9e2bac3de\t533f66a4-dac1-4ac7-8612-1235d65d1f37\n")})
    try:
        code, output = run(root)
        assert code == 0, output
    finally:
        shutil.rmtree(root)


def test_word_of_hexadecimal_letters_without_a_digit_is_still_a_word():
    expect_caught("tools/g.py", "# dac biet: cac mau\n", "word")


def test_file_that_is_not_utf8():
    expect_caught("tools/f.py", "# ch\u00e1y\n".encode("cp1258"), "encoding")


def test_untracked_file_is_checked():
    expect_caught("tools/untracked.py", "# chua commit: khong duoc quen\n", "word", track=False)


def test_file_of_no_class_is_an_error():
    root = make_repo({"notes.docx": b"PK\x03\x04binary"})
    try:
        code, output = run(root)
        assert code == 1 and "NO CLASS for 1 files" in output and "notes.docx" in output, output
    finally:
        shutil.rmtree(root)


def test_an_empty_repository_does_not_pass():
    root = tempfile.mkdtemp(prefix="check_english_")
    try:
        subprocess.run(["git", "init", "-q", root], check=True)
        write(root, "README.md", "# nothing\n")
        subprocess.run(["git", "-C", root, "add", "-A"], check=True)
        code, output = run(root)
        assert code == 1 and "NOTHING CHECKED" in output, output
    finally:
        shutil.rmtree(root)


def test_a_directory_outside_git_is_exit_code_2():
    root = tempfile.mkdtemp(prefix="check_english_")
    try:
        write(root, "a.py", "x = 1\n")
        code, output = run(root)
        assert code == 2 and "git cannot list the files" in output, output
    finally:
        shutil.rmtree(root)


def test_the_word_rule_is_off_only_for_the_two_files_that_hold_the_lists():
    root = make_repo({"tools/check_english_test.py": "WORDS = ('khong', 'duoc')\n"})
    try:
        code, output = run(root)
        assert code == 0, output
        assert "Word rule not applied to: tools/check_english_test.py" in output, output
    finally:
        shutil.rmtree(root)
    expect_caught("tools/check_english_test.py", "# m\u1eabu\n", "diacritic")
    expect_caught("tools/check_english.py", "# see \u00a73.1\n", "numbering")


def test_diacritic_in_a_documentation_file():
    expect_caught("docs/notes.md", "# Notes\nT\u00e0i li\u1ec7u.\n", "diacritic")


def test_diacritic_in_the_name_of_a_documentation_file():
    expect_caught("docs/ghi_ch\u00fa.md", "# Notes\n", "diacritic")


def test_measure_counts_a_word_in_the_documentation():
    root = make_repo({"docs.md": "khong khong khong\n"})
    try:
        code, output = run(root, "--measure")
        assert code == 0, output
        row = next(line for line in output.splitlines() if line.startswith("anywhere\tkhong\t"))
        # code 0; documentation 3 (docs.md); run output 1 (results-probe/probe_log.txt of CLEAN)
        assert row.split("\t")[2:5] == ["0", "3", "1"], row
    finally:
        shutil.rmtree(root)


TESTS = [v for k, v in sorted(globals().items()) if k.startswith("test_")]


def main():
    failed = 0
    for test in TESTS:
        try:
            test()
            print("  OK   %s" % test.__name__)
        except Exception as error:            # noqa: BLE001 -- report all, do not stop at the first
            failed += 1
            print("  FAIL %s: %s" % (test.__name__, error))
    print("")
    print("%d tests, %d failed" % (len(TESTS), failed))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
