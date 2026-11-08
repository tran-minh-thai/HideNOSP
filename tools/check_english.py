# SPDX-License-Identifier: MIT
"""Fail when a code file of the repository is not written in English.

Every file git lists -- tracked, or untracked and not ignored -- falls in exactly one class:

  code           checked: the extensions in CODE_SUFFIXES and the names in CODE_NAMES
  documentation  *.md: the diacritic rule only (below), so that a document may still cite section
                 numbers and name files whose names are Vietnamese
  run output     the output trees of tools/run.py (OUTPUT_DIRS): not checked, they record runs

A file of no class is an error, so that a new kind of file cannot pass unseen.

The text of every code file, and its path, are checked against four rules.

  diacritic  a Vietnamese letter with a diacritic: a Latin vowel carrying one of the Vietnamese
             marks, or d with stroke. The text is NFC-normalised first, so that a letter typed as a
             base letter plus a combining mark is found as well. The same letters in a French or
             Portuguese word are reported too: code is written in English.
  word       a Vietnamese word written without diacritics. Text is cut into identifiers (runs of
             letters, digits, underscores and hyphens) and every identifier into lower-case tokens
             at each non-letter and each camelCase hump. A token of WORDS_ANYWHERE is a violation
             wherever it stands. A token of WORDS_IN_NAMES, a syllable that is also an English
             word, is one only inside an identifier that holds a second token of WORDS_IN_NAMES or
             of SHORT_SYLLABLES, as in so_lan or thong_tin. A lower-case run of six letters or more
             that splits completely into two or more listed syllables of three letters or more, one
             of them from WORDS_ANYWHERE, is a violation too (a name written without separators),
             unless it is one of ENGLISH_SPLITS. A path to a documentation file (*.md) is not cut
             into words: a document may be named in its own language. A run of the letters a to
             f that touches a digit belongs to a digest (sha256, a commit, a UUID) and gives no
             token, or ...9dac7... would read as a word.
  numbering  a section sign followed by a digit, or the Vietnamese name of a theorem, lemma,
             proposition, corollary or definition followed by a number, with or without
             diacritics: the numbering of the paper and of the design documents stays out of code.
  reference  the number of a section or a rule of a document that is not in the repository:
             "rule" followed by a dotted number, "design doc section", or "section" followed by
             a number on a line that does not say which document it cites. A line that names the
             preregistration, or says "its section" or "of that paper" for a cited paper, is
             clean. In PREREGISTRATION_READERS, whose module text declares that every section
             number is one of config/preregistration.md, only the first two forms are checked.

Not detected, by design: a free-text phrase in which every word is also an English word or a
two-letter syllable; the English words Theorem, Lemma and so on followed by a number, which in this
code always cite a published paper; an abbreviation such as "tk" standing for a Vietnamese name; a
word spelt only with the letters a to f right next to a digit, as in cac2.

The word lists were measured against every file of the repository, and a word was
kept only when it matched nothing in the English text there; --measure counts them again.

This file and tools/check_english_test.py hold the word lists and the test samples themselves, so
the word rule does not apply to them; the other two rules do, which is why every non-ASCII
character in them is written as an escape. The report names both files on every run.

Run:
    python tools/check_english.py              the summary and every violation
    python tools/check_english.py --list       also every file with its class
    python tools/check_english.py --measure    how often each listed word occurs, and where
    python tools/check_english.py --root DIR   check the repository at DIR instead

Exit code 0 when every code file is clean; 1 on a violation, on a file of no class, or when there
is no code file to check; 2 when git cannot list the files.
"""
import argparse
import hashlib
import os
import re
import subprocess
import sys
import unicodedata
from collections import Counter, defaultdict
from functools import lru_cache

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from run import OUTPUT_DIRS  # noqa: E402  (the output trees, defined once, beside the runs)

CODE_SUFFIXES = (".java", ".py", ".sh", ".bash", ".ps1", ".psm1", ".bat", ".cmd", ".yaml", ".yml",
                 ".tsv", ".csv", ".txt", ".xml", ".json", ".toml", ".cfg", ".ini", ".properties")
CODE_NAMES = (".gitignore", ".gitattributes")
DOCUMENTATION_SUFFIXES = (".md",)
DOCUMENTATION_NAMES = ("license",)       # compared with the lower-cased file name
SELF = ("tools/check_english.py", "tools/check_english_test.py")
PREREGISTRATION_READERS = ("tools/preregistered_analysis.py", "tools/preregistered_analysis_test.py")

# The marks that turn a Latin vowel into a Vietnamese letter: grave, acute, tilde, hook above, dot
# below, circumflex, breve and horn.
VIETNAMESE_MARKS = frozenset("\u0300\u0301\u0303\u0309\u0323\u0302\u0306\u031b")
D_WITH_STROKE = frozenset("\u0111\u0110")
SECTION_SIGN = "\u00a7"

NUMBERED = re.compile(r"(?i)(?<![a-z])(dinh ly|bo de|menh de|he qua|dinh nghia)\s*[A-Z]?\d")
SECTION = re.compile(SECTION_SIGN + r"\s*\d")
IDENTIFIER = re.compile(r"[A-Za-z0-9_-]+")
TOKEN = re.compile(r"[A-Z]+(?![a-z])|[A-Z]?[a-z]+")
LOWER_RUN = re.compile(r"[a-z]{6,}")
DOC_PATH = re.compile(r"[A-Za-z0-9_./-]*[A-Za-z0-9_-]+\.md\b")
RULE_NUMBER = re.compile(r"(?<![A-Za-z])[Rr]ule \d+\.\d+(?:\.\d+)*")
DESIGN_SECTION = re.compile(r"(?i)\bdesign(?: doc(?:ument)?)? section\b")
SECTION_NUMBER = re.compile(r"(?<![A-Za-z])[Ss]ection \d+(?:\.\d+)*")
CITED_DOCUMENT = re.compile(r"(?i)preregistration|\bits section\b|\bof that paper\b")
HEX_LETTERS = frozenset("abcdefABCDEF")
ENGLISH_FUNCTION_WORDS = frozenset(
    "and of is are was were for with from this which when what where has have been would should "
    "could must into than over only every its does these those".split())

# Vietnamese syllables that are no English word: a violation wherever they stand.
WORDS_ANYWHERE = frozenset("""
    anh bai bao benh bia biet bieu boc boi buc buoc cac cach cai cang canh cao cau chan chenh
    chep chiem chieu chinh chiu choi chon chong chot chu chua chuan chuc chung chuoi chuong
    chuot chuyen cong cua cuc cung cuoi cuong cuu dac dai danh dat dau dem deu dia dien diep
    dieu dinh doan dua duc duoc duoi duong dut duy duyet ganh ghep ghi gia giac giai giam gian
    giao giau giay giet gioi giong giu giua goc gom gop gui hai hang hanh hiem hien hieu hinh
    hoa hoac hoach hoan hoanh hoat hoc hom hua huong huu kem keo khac khai kham khang khao khap
    khau kheo khi khiem khien khit kho khoa khoan khoang khoat khoi khong khop khuc khuyen
    khuyet kich kiem kien kieu kinh lanh lenh liet loai loat loc loi lom lon lua luan luat luoc
    luoi luon luong luot luu mach mai manh menh mieng minh moc muc muoi muon mut nang nao nem
    nen neu ngam ngan ngat ngau ngay nghe nghi nghia nghiem nghien ngo ngoac ngoai ngon ngu
    nguoc nguoi nguon nguong nha nhac nham nhanh nhap nhat nhau nhay nhe nhi nhien nhieu nhin
    nho nhom nhu nhung nhuong noi nong nop nua nuoc ong pha phac phai pham phan phang phap phat
    phay phe phep phia phien phieu phinh phoi phong phuc phuong quoc quy quyen quyet rac rai
    ranh rieng rui ruoi sach sanh sau sieu soan soat som sua suat suc suot suy tac tep thach
    tham thang thanh thao thap thay thi thich thien thiet thieu thoa thoat thoi thu thua thuat
    thuc thuong thuyet tia tich tiem tieng tiep tiet tieu tinh toan toc trai trang tranh tre
    tren trich trien triet trinh tro troi trong tru truc trum trung truoc truong truyen tuc
    tuong tuy tuyen tuyet ung uoc vao vay vec ven viec vien viet voi von vong vua vung vuong
    vuot vut xac xam xap xau xay xem xep xet xong xuat xung xuoi xuong xuyen yeu
""".split())

# Vietnamese syllables that are English words too: counted only inside one identifier.
WORDS_IN_NAMES = frozenset("""
    bac bam ban bang bat bay ben bien binh bon bot bung cam cat cay chac cham chang chap chat
    chay chet chi chin cho chut con cot cum cuon dam dan dang dao dap den dich diem don dong dot
    dung gan gay gen gio goi gon ham han hao hat hau hep het hoi hon hong hop hung ich kep ket
    kha khat kin kip lai lan lap lat lay lech lich lien lieu lop lot luc lui man mang mat mau
    mien moi mot nam nap nay nguyen nhan nut phi pho phu phut qua quan quat rang rap rat ret roi
    rong rut sai san sang sao sap sat sinh song sot sung tach tai tam tan tang tao tap tat tay
    thai theo tho thong thuan thung thuoc tien tim tin toa toi tom ton tong tra tram tran tri
    tron tua tuan tung tut vai van vang vat vet yen
""".split())

# Two-letter syllables: counted only as the second token of such an identifier.
SHORT_SYLLABLES = frozenset("""
    an ao ap ay ba bi bo ca co cu da de di do du ep ga gi ha ho ii ke ky la le lo ly ma mo mu ne
    ra re ro se so su ta te ti tu uu va ve vi vo vu xa xe xi xu
""".split())

# English words (/usr/share/dict/words) that split into listed syllables: never a
# concatenation.
ENGLISH_SPLITS = frozenset("""
    batlon bonduc botong bucket bucketman cachet caiman caucho chandam chulan deuton diadem
    diatom goitrogen gombay guidon hangman hansom hocket homrai khilat landau lochan locket
    longan maidan maiden mandua mucket mucluc mutton neutron phalangian phantom quatre racket
    suclat tacket taihoa tandem tantrum theophobia tholoi transom trebuchet trevet trochantin
    trochi trogon trophi truvat tucket
""".split())


@lru_cache(maxsize=None)
def is_vietnamese_letter(ch):
    if ch in D_WITH_STROKE:
        return True
    if ch.isascii():
        return False
    parts = unicodedata.normalize("NFD", ch)
    return (len(parts) > 1 and parts[0] in "aeiouyAEIOUY"
            and all(mark in VIETNAMESE_MARKS for mark in parts[1:]))


def without_marks(text):
    text = text.replace("\u0111", "d").replace("\u0110", "D")
    return "".join(c for c in unicodedata.normalize("NFD", text) if unicodedata.category(c) != "Mn")


def is_digest_part(identifier, match):
    """True for a run of hexadecimal letters that touches a digit: part of a digest (sha256, a
    commit, a UUID), where ...9dac7... would otherwise read as the word dac."""
    if not set(match.group(0)) <= HEX_LETTERS:
        return False
    before = identifier[match.start() - 1] if match.start() else ""
    after = identifier[match.end()] if match.end() < len(identifier) else ""
    return before.isdigit() or after.isdigit()


def tokens_of(identifier):
    return [m.group(0).lower() for m in TOKEN.finditer(identifier)
            if not is_digest_part(identifier, m)]


@lru_cache(maxsize=None)
def is_concatenation(run):
    """True when a lower-case run splits completely into two or more listed syllables of three
    letters or more, at least one of them from WORDS_ANYWHERE."""
    if run in ENGLISH_SPLITS or run in WORDS_ANYWHERE or run in WORDS_IN_NAMES:
        return False

    @lru_cache(maxsize=None)
    def splits(start, anywhere, parts):
        if start == len(run):
            return anywhere and parts >= 2
        for end in range(start + 3, min(len(run), start + 7) + 1):
            part = run[start:end]
            if part in WORDS_ANYWHERE or part in WORDS_IN_NAMES:
                if splits(end, anywhere or part in WORDS_ANYWHERE, parts + 1):
                    return True
        return False

    return splits(0, False, 0)


def diacritic_violations(line):
    letters = sorted({c for c in line if is_vietnamese_letter(c)})
    return [("diacritic", "".join(letters))] if letters else []


def numbering_violations(line):
    found = [("numbering", m.group(0)) for m in SECTION.finditer(line)]
    found += [("numbering", m.group(0)) for m in NUMBERED.finditer(without_marks(line))]
    return found


def reference_violations(line, bare_sections=True):
    """("reference", text) for every section or rule number of a document outside the repository."""
    found = [("reference", m.group(0)) for m in RULE_NUMBER.finditer(line)]
    found += [("reference", m.group(0)) for m in DESIGN_SECTION.finditer(line)]
    if bare_sections and not CITED_DOCUMENT.search(line):
        found += [("reference", m.group(0)) for m in SECTION_NUMBER.finditer(line)]
    return found


def word_violations(line):
    """("word", identifier) for every identifier of the line that is Vietnamese."""
    found = []
    for identifier in IDENTIFIER.findall(DOC_PATH.sub(" ", line)):
        tokens = tokens_of(identifier)
        in_names = sum(1 for t in tokens if t in WORDS_IN_NAMES)
        short = sum(1 for t in tokens if t in SHORT_SYLLABLES)
        if (any(t in WORDS_ANYWHERE for t in tokens)
                or (in_names and in_names + short >= 2)
                or any(is_concatenation(m.group(0)) for m in LOWER_RUN.finditer(identifier)
                       if not is_digest_part(identifier, m))):
            found.append(("word", identifier))
    return found


def classify(path):
    if path.startswith(OUTPUT_DIRS):
        return "run output"
    name = path.rsplit("/", 1)[-1].lower()
    if name.endswith(DOCUMENTATION_SUFFIXES) or name in DOCUMENTATION_NAMES:
        return "documentation"
    if name in CODE_NAMES or name.endswith(CODE_SUFFIXES):
        return "code"
    return None


def listed_files(root):
    """Every path git lists under root, tracked or untracked but not ignored; None if git fails."""
    try:
        out = subprocess.run(
            ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
            cwd=root, capture_output=True, check=True)
    except (OSError, subprocess.CalledProcessError) as error:
        detail = getattr(error, "stderr", b"") or b""
        print("ERROR: git cannot list the files of %s: %s %s"
              % (root, error, detail.decode("utf-8", "replace").strip()), file=sys.stderr)
        return None
    return sorted({p for p in out.stdout.decode("utf-8").split("\0") if p})


def read_text(root, path):
    """(text, None), or (None, reason) when the file is missing or not UTF-8."""
    try:
        with open(os.path.join(root, path), "rb") as handle:
            data = handle.read()
    except FileNotFoundError:
        return None, "missing"
    try:
        return unicodedata.normalize("NFC", data.decode("utf-8")), None
    except UnicodeDecodeError as error:
        return None, "not UTF-8 (byte %d)" % error.start


def check_file(root, path, rules=None):
    """(violations, number of lines or None when the file is missing)."""
    if rules is None:
        word_rule = path not in SELF
        rules = [diacritic_violations, numbering_violations] + ([word_violations] if word_rule else [])
        if path in PREREGISTRATION_READERS:
            rules.append(lambda line: reference_violations(line, bare_sections=False))
        elif path not in SELF:
            rules.append(reference_violations)
    violations = [(0, kind, what, "(the path)") for rule in rules for kind, what in rule(path)]
    text, problem = read_text(root, path)
    if problem == "missing":
        return violations, None
    if problem:
        return violations + [(0, "encoding", problem, "")], 0
    lines = text.splitlines()
    for number, line in enumerate(lines, 1):
        for rule in rules:
            for kind, what in rule(line):
                violations.append((number, kind, what, line.strip()[:120]))
    return violations, len(lines)


def check(root, show_list):
    files = listed_files(root)
    if files is None:
        return 2
    classes = defaultdict(list)
    for path in files:
        classes[classify(path)].append(path)
    if show_list:
        for path in files:
            print("%-14s %s" % (classify(path) or "NO CLASS", path))
        print()

    violations, missing, lines, doc_lines = {}, [], 0, 0
    for path in classes["code"]:
        found, count = check_file(root, path)
        if count is None:
            missing.append(path)
        else:
            lines += count
        if found:
            violations[path] = found
    docs_missing = []
    for path in classes["documentation"]:
        found, count = check_file(root, path, [diacritic_violations])
        if count is None:
            docs_missing.append(path)
        else:
            doc_lines += count
        if found:
            violations[path] = found
    missing += docs_missing

    checked = len(classes["code"]) - (len(missing) - len(docs_missing))
    # The directory name only: a machine path does not belong in a report that may be kept.
    print("Checked %d code files (%d lines) in repository %s"
          % (checked, lines, os.path.basename(os.path.abspath(root))))
    print("Checked %d documentation files (*.md, %d lines) for diacritics only"
          % (len(classes["documentation"]) - len(docs_missing), doc_lines))
    print("Not checked: %d run-output files (%s)"
          % (len(classes["run output"]), ", ".join(OUTPUT_DIRS)))
    exempt = [p for p in SELF if p in classes["code"]]
    if exempt:
        print("Word rule not applied to: %s (they hold the word lists and the test samples)"
              % ", ".join(exempt))
        print("Reference rule not applied to: %s (they hold the test samples)" % ", ".join(exempt))
    readers = [p for p in PREREGISTRATION_READERS if p in classes["code"]]
    if readers:
        print("Bare section numbers read as preregistration sections in: %s" % ", ".join(readers))
    if missing:
        print("Listed by git but missing from the working tree, not checked: %s"
              % ", ".join(missing))
    status = 0
    if classes[None]:
        print("NO CLASS for %d files; add the extension to CODE_SUFFIXES or DOCUMENTATION_SUFFIXES:"
              % len(classes[None]))
        for path in classes[None]:
            print("  " + path)
        status = 1
    if not checked:
        print("NOTHING CHECKED: no code file was found, so a clean result would mean nothing.")
        status = 1
    total = sum(len(v) for v in violations.values())
    print("Violations: %d in %d files" % (total, len(violations)))
    for path in sorted(violations):
        for number, kind, what, line in violations[path]:
            print("  %s:%d: %s: %s | %s" % (path, number, kind, what, line))
    return 1 if total else status


def measure(root):
    """How often every listed word occurs as a token, per class of file; words that occur in an
    English line of the documentation or of the run output are shown with that line."""
    files = listed_files(root)
    if files is None:
        return 2
    lists = [("anywhere", WORDS_ANYWHERE), ("in names", WORDS_IN_NAMES), ("short", SHORT_SYLLABLES)]
    counts = defaultdict(Counter)
    english = Counter()
    example = {}
    for path in files:
        kind = classify(path)
        if kind is None or path in SELF:
            continue
        text, problem = read_text(root, path)
        if problem:
            continue
        for line in text.splitlines():
            line = DOC_PATH.sub(" ", line)
            tokens = [t for identifier in IDENTIFIER.findall(line) for t in tokens_of(identifier)]
            plain = set(re.findall(r"(?<![A-Za-z])[a-z]+(?![A-Za-z])", line))
            is_english = (kind != "code" and not any(is_vietnamese_letter(c) for c in line)
                          and plain & ENGLISH_FUNCTION_WORDS
                          and sum(1 for t in set(tokens) if t in WORDS_ANYWHERE) < 2)
            for t in tokens:
                counts[t][kind] += 1
                if is_english:
                    english[t] += 1
                    example.setdefault(t, "%s: %s" % (path, line.strip()[:100]))
    revision = subprocess.run(["git", "rev-parse", "--short=12", "HEAD"], cwd=root,
                              capture_output=True, text=True).stdout.strip() or "unknown"
    dirty = subprocess.run(["git", "status", "--porcelain"], cwd=root, capture_output=True,
                           text=True).stdout.strip()
    with open(os.path.abspath(__file__), "rb") as handle:
        digest = hashlib.sha256(handle.read()).hexdigest()
    print("# check_english.py --measure; repository %s at %s (%s); tool sha256 %s"
          % (os.path.basename(os.path.abspath(root)), revision,
             "tree MODIFIED" if dirty else "tree clean", digest))
    print("# code, documentation, output: token counts per class of file. english: counts in lines")
    print("# of documentation or output with no diacritic, an English function word, and fewer")
    print("# than two WORDS_ANYWHERE tokens. A WORDS_ANYWHERE word needs 0 in code and in english.")
    print("list\tword\tcode\tdocumentation\toutput\tenglish\texample")
    review = []
    for name, words in lists:
        for word in sorted(words):
            c = counts[word]
            print("%s\t%s\t%d\t%d\t%d\t%d\t%s"
                  % (name, word, c["code"], c["documentation"], c["run output"], english[word],
                     example.get(word, "")))
            if name == "anywhere" and (c["code"] or english[word]):
                review.append(word)
    print("# WORDS_ANYWHERE words with a count in code or in an English line: %d%s"
          % (len(review), (": " + " ".join(review)) if review else ""))
    return 1 if review else 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--root", default=os.path.dirname(HERE),
                        help="the repository to check (default: the one holding this tool)")
    parser.add_argument("--list", action="store_true", help="also list every file with its class")
    parser.add_argument("--measure", action="store_true",
                        help="count every listed word per class of file instead of checking")
    args = parser.parse_args(argv)
    if args.measure:
        return measure(args.root)
    return check(args.root, args.list)


if __name__ == "__main__":
    sys.exit(main())
