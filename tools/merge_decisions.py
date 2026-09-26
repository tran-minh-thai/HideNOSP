"""Merge the results of a review of "what is still to be decided" into one document by theme.

Input: a JSON file whose "result" holds the review items, "confirmed" (items that passed an
independent check) and "addedByCritic" (items added by a final critical pass); each item has
title, question, sharpened, sourceKey and blocks. Output: config/quyet_dinh_can_chot.md next to
this tool, or the path given second. Kept so that a later review is merged the same way, and so
that the merge can be read: every item goes to exactly ONE theme, and no item is dropped.

The themes and their keywords are written in English, and so is the document this tool writes; a
review whose items are written in another language matches no keyword and ends up, whole, in the
section of unsorted items.

Run: python tools/merge_decisions.py <review result JSON> [<output file>]
"""
import io, json, os, re, sys, unicodedata

if len(sys.argv) not in (2, 3):
    sys.exit("usage: python tools/merge_decisions.py <review result JSON> [<output file>]")
SRC = sys.argv[1]
# The default output is found from this file, not from the directory the command is typed in.
OUT = sys.argv[2] if len(sys.argv) == 3 else os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "config", "quyet_dinh_can_chot.md")

d = json.loads(io.open(SRC, encoding="utf-8", errors="replace").read())["result"]
items = [dict(x, origin="review") for x in d["confirmed"]] + \
        [dict(x, origin="critic") for x in d["addedByCritic"]]


GREEK = {"ρ": "rho", "β": "beta", "ε": "epsilon", "σ": "sigma",
         "μ": "mu", "ℓ": "l", "δ": "delta", "ψ": "psi"}


def fold(s):
    s = s or ""
    for g, r in GREEK.items():
        s = s.replace(g, r)
    s = s.replace(chr(0x111), "d").replace(chr(0x110), "D")   # d with stroke, which NFD keeps
    s = unicodedata.normalize("NFD", s)
    return "".join(c for c in s if unicodedata.category(c) != "Mn").lower()


# (code, theme, what it blocks, keywords that recognise it)
THEMES = [
    ("A1", "Practical-effect threshold δ_L of H2", "SC3", ["delta_l", "d_l", "practical effect"]),
    ("A2", "Thresholds of ρ_rep and β for the groups: values, the unit they apply to, and the band in between", "SC3", ["rho_rep"]),
    ("A3", "The β threshold of the constraint control group", "SC3", ["beta threshold", "beta for the group"]),
    ("A4", "Equivalence margins for Lost and SupDist", "SC3", ["equivalence margin"]),
    ("A5", "The test for H3 and the quantitative meaning of \"near 1\"", "SC3", ["h3 and the meaning", "near 1", "h3 on the degenerate group"]),
    ("A6", "The family of baselines in the Holm correction", "SC3", ["holm"]),
    ("A7", "Pairing for H2: the same |R| budget, or each method choosing its own", "SC3", ["|r| budget", "pairing"]),
    ("A8", "Wilcoxon conventions: zero differences, ties, effect size", "SC3", ["wilcoxon"]),
    ("A9", "The exclusion rule for failed instances (HF>0, timeout, out of memory)", "SC3", ["failed instance"]),
    ("A10", "The number of reruns of the randomized methods", "SC3", ["randomized method", "reruns of the"]),
    ("A11", "The grid of σ, ℓ_max, gaps and |S| for each dataset", "SC3", ["sigma for each dataset", "l_max and gap", "sigma grid", "sigma value", "lock sigma and width", "ell_max"]),
    ("A12", "The ε threshold of B_ε for each dataset", "SC3", ["epsilon"]),
    ("A13", "The sup_NO percentiles that draw the sensitive sets", "SC3", ["sup_no percentile"]),
    ("A14", "The scope of μ(P): keep it narrow at μ=1, or open it to μ≥2", "SC3", ["mu(p)", "repeated item", "node weight when"]),
    ("A15", "Several sensitive patterns under a database threshold: LP rounding or hybrid GA", "SC3", ["lp or ga", "lp rounding", "several sensitive patterns at the threshold", "metaheuristic family"]),
    ("A16", "What a \"small instance\" is, and which ILP variant SC3 takes as its reference", "SC3", ["small instance", "ilp variant"]),
    ("A17", "Stamping the code version into the result files (now unversioned)", "SC3", ["unversioned", "code version into"]),
    ("A18", "The discretised time-series datasets of the NOSEP line", "SC3", ["time series"]),
    ("A19", "The official role of each dataset once measured", "SC3", ["role of each dataset", "new experimental dataset", "into the evidence group"]),
    ("A20", "A ceiling on the ratio |R|*/Σ|S|, and the handling of extreme patterns", "SC3", ["ceiling on the ratio"]),
    ("A21", "How to print |R|* and β when a sensitive set holds several patterns", "SC3", ["how to print |r|"]),
    ("A22", "Step 3 with repeated-item patterns: the candidate set is not defined", "SC3", ["step 3 with pattern"]),
    ("A23", "The content of the preregistration file, and making the go/no-go official", "SC3", ["preregistration.md", "go/no-go"]),
    ("A24", "Tier (a) baselines when a sensitive set holds more than one pattern", "SC3", ["tier (a) when a set"]),
    ("A25", "Checking SBSH against Algorithms 2 and 3 of [R3]", "SC3", ["sbsh"]),

    ("B1", "Measuring machine, JVM flags, number of warm-up runs, time limit", "submission", ["measuring machine", "xms", "warm-up", "time limit", "run environment"]),
    ("B2", "Peak memory: the heap or the RSS of the process", "submission", ["heap or rss", "peak memory"]),
    ("B3", "σ reported as a share or as an absolute number", "submission", ["share or keep it absolute", "share or absolute"]),
    ("B4", "Parameters and scale of the synthetic data", "submission", ["synthetic data"]),
    ("B5", "A reference NETGAP implementation for the SC1 check", "submission", ["netgap"]),
    ("B6", "The source of TRUCKS, and what becomes of SC8 without it", "submission", ["trucks", "sc8 without"]),
    ("B7", "The ψ threshold and the reporting unit of the original papers for SC8", "submission", ["psi threshold"]),
    ("B8", "[R11] Zhao 2025: does it still block Gate 1, does it count by sup_NO", "submission", ["[r11]", "zhao 2025"]),
    ("B9", "The state of the research gaps G1, G2, G5 and the scope of the review", "submission", ["research gap g", "g1 and g5", "g2 is not", "scope of the review"]),
    ("B10", "The reading level required for [R22] before G5 is claimed", "submission", ["[r22]"]),
    ("B11", "Gate 1: may it be declared passed while the reading level falls short", "submission", ["gate 1 passed"]),
    ("B12", "A second reader to check the proofs", "submission", ["second reader"]),
    ("B13", "Venue, format template, and where the pseudocode and the long proofs go", "submission", ["venue", "pseudocode"]),
    ("B14", "The sentence on the experimental results in the abstract", "submission", ["result sentence"]),
    ("B15", "The source-code licence of the repository at publication", "submission", ["source-code licence"]),
    ("B16", "Terms for reusing the data, and statements of the sources", "submission", ["data reuse"]),
    ("B17", "The pipeline from the CSV files to the tables and figures of the manuscript", "submission", ["tables and figures"]),
    ("B18", "The statistics tool, and where the test results are kept", "submission", ["statistics tool"]),
    ("B19", "SC8 missing from the schedule, and what to drop first when time runs short", "submission", ["sc8 is not in the schedule", "absolute deadline"]),
    ("B20", "The size limits of the implementation, and how to declare a saturated count", "submission", ["size limit", "maximum flow and the number"]),
    ("B21", "The acceptance threshold for the certified share UB = LB", "submission", ["ub = lb", "certified ub"]),
    ("B22", "SC7 row 4: what \"any cut\" means", "submission", ["any cut"]),
    ("B23", "Which σ counts the side effects of SC4", "submission", ["side effects of sc4"]),
    ("B24", "The conditions attached to contributions 1 and 2, and the fallback if they fail", "submission", ["contributions 1 and 2"]),
    ("B25", "The plan if the group of [R6], [R7] works on the same problem", "submission", ["[r6]"]),
    ("B26", "The FIFA evidence [17,17,17] is in results-probe", "submission", ["[17, 17, 17]", "fifa [17"]),
]

assigned = {}
leftovers = []
for it in items:
    hay = fold(it.get("title", "") + " || " + it.get("question", "") + " || " + it.get("sharpened", ""))
    hit = None
    for code, name, urg, keys in THEMES:
        if any(fold(k) in hay for k in keys):
            hit = code
            break
    if hit:
        assigned.setdefault(hit, []).append(it)
    else:
        leftovers.append(it)

lines = []
W = lines.append
W("# What is still to be decided")
W("")
W("Generated from a review of 10 sources of the project: `config/preregistration.md`, `config/protocol.yaml`,")
W("`RUNBOOK.md`, the research design from four angles (gates and risks, theory, experimental design,")
W("gaps), the results of phase 1, the source code (`TODO(code)` and `UnsupportedOperationException`),")
W("and the manuscript. Every item was checked again by an independent pass: open the file cited, check the quotation,")
W("and grep both the documents and the code for a decision already taken elsewhere. A final critical pass looked for")
W("kinds of decision that nobody had touched.")
W("")
W("%d items passed the check, plus %d added by the critical pass, merged into the %d themes below."
  % (len(d["confirmed"]), len(d["addedByCritic"]), len([c for c in THEMES if c[0] in assigned])))
W("")
W("**How to read it.** Group A blocks SC3: until they are locked, every SC3 run must carry `--probe` and its numbers")
W("cannot be used in the paper. Group B blocks submission, not the runs.")
W("")
W("---")
W("")

for urg, head in (("SC3", "## A. Blocks SC3 — to be locked before the main comparison runs"),
                  ("submission", "## B. Blocks submission — does not block the runs")):
    W(head)
    W("")
    for code, name, u, _ in THEMES:
        if u != urg or code not in assigned:
            continue
        group = assigned[code]
        srcs = sorted({g.get("sourceKey", g.get("origin", "")) for g in group})
        W("### %s. %s" % (code, name))
        W("")
        W("Raised by: %s%s" % (", ".join(f"`{s}`" for s in srcs),
                               "  (includes items added by the critical pass)" if any(g["origin"] == "critic" for g in group) else ""))
        W("")
        seen_q = set()
        for g in sorted(group, key=lambda x: -len(x.get("sharpened", "") or "")):
            q = " ".join((g.get("sharpened") or g.get("question") or "").split())
            key = q[:90].lower()
            if not q or key in seen_q:
                continue
            seen_q.add(key)
            W("**%s**" % g.get("title", "").strip())
            W("")
            W(q if len(q) <= 1400 else q[:1400] + " [...]")
            W("")
        blocks = sorted({" ".join(g.get("blocks", "").split()) for g in group if g.get("blocks")}, key=len)
        if blocks:
            W("> Blocks: %s" % (blocks[-1] if len(blocks[-1]) <= 500 else blocks[-1][:500] + " [...]"))
            W("")
    W("---")
    W("")

if leftovers:
    W("## C. Not sorted into any theme")
    W("")
    for it in leftovers:
        W("- **%s** (`%s`) — %s" % (it.get("title", ""), it.get("sourceKey", it.get("origin", "")),
                                    (it.get("sharpened") or it.get("question") or "")[:400]))
    W("")

io.open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")
print("wrote", OUT)
print("themes with content:", len([c for c in THEMES if c[0] in assigned]), "/", len(THEMES))
print("left unsorted:", len(leftovers))
for it in leftovers:
    print("   -", it.get("title", "")[:70])
