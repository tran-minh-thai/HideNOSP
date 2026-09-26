"""Scratch comparison of an old and a new report: exit code, line count, every number of every
line in order, and the words that carry meaning (group labels, verdicts, exclusion labels) after
mapping the old words to the new ones. The wording of the rest is not compared: it is the
translation."""
import re, sys

OLD_TO_NEW = [  # old phrase -> new phrase ("" = a phrase to skip); longest first
    ("khac biet vuot bien", "different beyond the margin"), ("khoang khong tinh duoc", "interval not computable"),
    ("bien tuong duong", ""), ("khong du cap", "not enough pairs"), ("khong ket luan", "no conclusion"),
    ("tuong duong", "equivalent"), ("khong bac bo", "not rejected"), ("bac bo H0", "H0 rejected"),
    ("lon hon 1", "above 1"), ("vuot 1", "above 1"), ("gan 1", "near 1"), ("co hieu luc", "effective"),
    ("trung gian", "?"), ("suy bien", "degenerate"), ("chinh", "main"), ("chung ta", "ours"),
]
NEW_WORDS = ["different beyond the margin", "interval not computable", "not enough pairs", "no conclusion",
             "equivalent", "not rejected", "H0 rejected", "above 1", "near 1", "effective", "intermediate",
             "between", "degenerate", "main", "ungrouped", "ours"]
GROUP_WORDS = {"chinh": "main", "trung gian": "intermediate", "suy bien": "degenerate", "khong xep": "ungrouped"}
NUM = re.compile(r"[-+]?(?:\d+\.\d+|\.\d+|\d+)(?:[eE][-+]?\d+)?|nan|inf")

def scan(line, table):
    found = []
    for phrase, new in table:
        for m in re.finditer(r"(?<![A-Za-z_])" + re.escape(phrase) + r"(?![A-Za-z_])", line):
            found.append((m.start(), -len(phrase), phrase, new))
    found.sort()
    out, end = [], -1
    for start, _, phrase, new in found:
        if start < end:
            continue
        end = start + len(phrase)
        if new == "?":     # intermediate for rho, between for beta: decided by what precedes it
            before = line[:start].rstrip()
            new = "between" if before.endswith("beta") or before.endswith("/") else "intermediate"
        if new:
            out.append(new)
    return out

def words_old(line):
    return scan(line, OLD_TO_NEW + [(w, w) for w in NEW_WORDS])

def words_new(line):
    return scan(line, [(w, w) for w in NEW_WORDS])

def group_counts(line, old):
    d = {}
    for part in line.split(":", 1)[1].split(","):
        if not part.strip():
            continue
        label, count = part.strip().rsplit(" ", 1)
        d[GROUP_WORDS.get(label, label) if old else label] = count
    return d

def split(text):
    body, _, rest = text.partition("@@STDERR@@\n")
    err, _, code = rest.partition("@@EXIT@@ ")
    return body.splitlines(), err.splitlines(), code.strip()

def compare(old_text, new_text, name):
    ob, oe, oc = split(old_text); nb, ne, nc = split(new_text)
    problems, checked_numbers, checked_words = [], 0, 0
    if oc != nc and not (oc.startswith("EXCEPTION") and nc.startswith("EXCEPTION")):
        problems.append("exit code %s vs %s" % (oc, nc))
    for kind, o, n in (("stdout", ob, nb), ("stderr", oe, ne)):
        if len(o) != len(n):
            problems.append("%s: %d lines vs %d" % (kind, len(o), len(n)))
            continue
        for i, (a, b) in enumerate(zip(o, n)):
            if a.strip().startswith("rows by group:"):
                ga, gb = group_counts(a, True), group_counts(b, False)
                checked_words += len(ga)
                if ga != gb:
                    problems.append("%s line %d group counts: %r vs %r" % (kind, i + 1, a, b))
                continue
            na, nb_ = NUM.findall(a), NUM.findall(b)
            checked_numbers += len(na)
            if na != nb_:
                problems.append("%s line %d numbers %s vs %s | %r | %r" % (kind, i + 1, na, nb_, a, b))
            wa, wb = words_old(a), words_new(b)
            checked_words += len(wa)
            if wa != wb:
                problems.append("%s line %d words %s vs %s | %r | %r" % (kind, i + 1, wa, wb, a, b))
    status = "SAME" if not problems else "DIFFERENT"
    print("%-46s %s  exit %s/%s  lines %d  numbers %d  words %d"
          % (name, status, oc, nc, len(ob) + len(oe), checked_numbers, checked_words))
    for p in problems[:12]:
        print("     " + p)
    return not problems

if __name__ == "__main__":
    ok = compare(open(sys.argv[1], encoding="utf-8").read(), open(sys.argv[2], encoding="utf-8").read(), sys.argv[3])
    sys.exit(0 if ok else 1)
