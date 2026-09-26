"""Scratch equivalence test of tools/merge_decisions.py (English) against the upstream
tools/gop_quyet_dinh.py (Vietnamese keywords and document).

A parallel review is built: for every theme and every keyword position i, one item whose text holds
keyword i, written with the old keyword for the old tool and the new keyword for the new tool; plus
items that match nothing, a repeated question, items without sourceKey, and items of the critical
pass. Every item carries an id "[item N]" in its title in both languages. Both tools run, and for
each tool the output document is parsed into: theme code -> ids, unsorted ids, the sources line and
the blocks line of every theme; the printed counts are compared too. The theme data are read from
the two scripts themselves (not copied), so the test follows the files it tests.
"""
import ast
import json
import os
import re
import subprocess
import sys
import tempfile

OLD_TOOL, NEW_TOOL = sys.argv[1], sys.argv[2]


def themes_of(path):
    tree = ast.parse(open(path, encoding="utf-8").read())
    for node in tree.body:
        if isinstance(node, ast.Assign) and getattr(node.targets[0], "id", "") == "THEMES":
            return ast.literal_eval(node.value)
    raise SystemExit("no THEMES in " + path)


old_themes, new_themes = themes_of(OLD_TOOL), themes_of(NEW_TOOL)
assert [t[0] for t in old_themes] == [t[0] for t in new_themes], "theme codes differ"
assert [len(t[3]) for t in old_themes] == [len(t[3]) for t in new_themes], "keyword counts differ"
URGENCY = {"SC3": "SC3", "nop": "submission"}
assert [URGENCY[t[2]] for t in old_themes] == [t[2] for t in new_themes], "urgency differs"


def review(themes, filler_none, lang):
    confirmed, critic = [], []
    n = 0
    for code, _, _, keys in themes:
        for i, key in enumerate(keys):
            n += 1
            item = {"title": "[item %d] %s" % (n, key), "question": key,
                    "sharpened": "x" * (n % 7), "sourceKey": "src%d" % (n % 5),
                    "blocks": "block %d" % (n % 3) if n % 2 else ""}
            if n % 11 == 0:
                del item["sourceKey"]          # falls back to the origin marker
            (critic if n % 13 == 0 else confirmed).append(item)
    for a in range(0, len(themes) - 7, 7):      # items holding keywords of two themes: the
        first, later = themes[a], themes[a + 5]  # earlier theme must win (first match)
        n += 1
        text = "%s and %s" % (later[3][0], first[3][-1])
        confirmed.append({"title": "[item %d] %s" % (n, text), "question": text,
                          "sharpened": "", "sourceKey": "pair"})
    for k in range(4):                          # items that match no theme at all
        n += 1
        confirmed.append({"title": "[item %d] %s" % (n, filler_none), "question": filler_none,
                          "sharpened": "", "sourceKey": "none"})
    dup = dict(confirmed[0]); dup["title"] = dup["title"].replace("[item 1]", "[item 999]")
    confirmed.append(dup)                        # a repeated question is printed once
    return {"result": {"confirmed": confirmed, "addedByCritic": critic}}


def run(tool, data):
    d = tempfile.mkdtemp()
    src, out = os.path.join(d, "review.json"), os.path.join(d, "out.md")
    json.dump(data, open(src, "w", encoding="utf-8"), ensure_ascii=False)
    p = subprocess.run([sys.executable, tool, src, out], capture_output=True, text=True)
    assert p.returncode == 0, p.stderr
    return open(out, encoding="utf-8").read(), p.stdout


def parse(doc):
    sections, current, unsorted, sources, blocks = {}, None, [], {}, {}
    in_c = False
    for line in doc.splitlines():
        m = re.match(r"### (\w+)\. ", line)
        if m:
            current = m.group(1); sections[current] = []; in_c = False; continue
        if line.startswith("## C."):
            in_c = True; current = None; continue
        if line.startswith("## "):
            current = None; in_c = False; continue
        ids = re.findall(r"\[item (\d+)\]", line)
        if in_c and ids:
            unsorted += ids
        elif current and ids:
            sections[current] += ids
        elif current and (line.startswith("Raised by:") or line.startswith("Ngu")):
            sources[current] = (re.findall(r"`([^`]*)`", line), "(" in line)
        elif current and line.startswith("> "):
            blocks[current] = line.split(":", 1)[1].strip()
    return sections, unsorted, sources, blocks


old_doc, old_out = run(OLD_TOOL, review(old_themes, "khong lien quan gi", "vi"))
new_doc, new_out = run(NEW_TOOL, review(new_themes, "nothing related here", "en"))
o, n = parse(old_doc), parse(new_doc)
origin = {"quet": "review", "phebinh": "critic"}
o_sources = {k: ([origin.get(s, s) for s in v[0]], v[1]) for k, v in o[2].items()}
checks = {
    "themes and their item ids": o[0] == n[0],
    "unsorted item ids": o[1] == n[1],
    "sources and critic note per theme": o_sources == n[2],
    "blocks line per theme": o[3] == n[3],
    "printed counts": re.findall(r"\d+", old_out.split("\n", 1)[1]) == re.findall(r"\d+", new_out.split("\n", 1)[1]),
}
items = sum(len(v) for v in o[0].values())
print("themes %d, keyword items sorted %d (old) / %d (new), unsorted %d / %d"
      % (len(old_themes), items, sum(len(v) for v in n[0].values()), len(o[1]), len(n[1])))
for name, ok in checks.items():
    print("  %-36s %s" % (name, "SAME" if ok else "DIFFERENT"))
if not all(checks.values()):
    for code in sorted(set(o[0]) | set(n[0])):
        if o[0].get(code) != n[0].get(code):
            print("   ", code, "old", o[0].get(code), "new", n[0].get(code))
    sys.exit(1)
