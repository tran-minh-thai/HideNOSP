# SPDX-License-Identifier: MIT
"""Diagnostic: does the side-effect weight explain the HIDE-SEQ instances where HHA uses fewer masks than CUT-HIDE?

Under HIDE-SEQ with five sensitive patterns, HHA uses fewer masks than CUT-HIDE on some instances (r = 2 and r = 3).
CUT-HIDE with several patterns masks, in each sequence, the position with the largest ratio drop / (1 + w(i)); w is the
side-effect weight of the near-threshold patterns. With w = 0 (method key cut-hide-uniform) the rule ranks positions by
the drop alone. This tool counts, on exactly the instances where HHA uses fewer masks, how many the w = 0 greedy solves
with no more masks than HHA.

Three commands:

    python tools/hha_mask_diagnostic.py select
        Reads the SC4 (r = 2) and SC4R (r = 3) files of machine ADMIN-PC with five sensitive patterns, keeps the main
        group as the analysis does, lists the instances where HHA uses fewer masks than CUT-HIDE per scope, and prints
        one run command per cell (dataset, gap, sigma, pattern kind, r) that holds such an instance.
    python tools/hha_mask_diagnostic.py report <directory>
        Reads the files of the diagnostic runs in <directory> (methods cut-hide-uniform,cut-hide-rw-ls,hha), checks that
        the rerun of HHA and CUT-HIDE reproduces the stored mask counts on every selected instance, and writes
        summary.tsv and instances.tsv into <directory>.
    python tools/hha_mask_diagnostic.py self-test
"""
import csv
import os
import re
import sys
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import preregistered_analysis as pa  # noqa: E402
import reopened_analysis as ra  # noqa: E402

MACHINE = "ADMIN-PC"
SCENARIOS = (("SC4", 2), ("SC4R", 3))
OURS = "cut-hide-rw-ls"
BASE = "hha"
UNIFORM = "cut-hide-uniform"
METHODS = ",".join((UNIFORM, OURS, BASE))


def key(r):
    return (pa.dataset_name(r["dataset"]), r["gap"], int(r["sigma"]), r["sensitive_set"])


def selected(scenario):
    """{scope: (complete instances, instances where HHA uses fewer masks)} for five sensitive patterns."""
    rows, _ = ra.load(scenario, MACHINE, "zero")
    out = {}
    for scope in ("distinct", "repeated"):
        part = ra.in_group(ra.by_scope(ra.of_size(rows, 5), scope), ra.groups_of("SC4", "zero", scope))
        by = defaultdict(dict)
        for r in part:
            by[key(r)][r["method_key"]] = r
        complete = {k: m for k, m in by.items()
                    if OURS in m and BASE in m and not m[OURS]["status"] and not m[BASE]["status"]}
        fewer = {k: m for k, m in complete.items() if int(m[BASE]["mask_count"]) < int(m[OURS]["mask_count"])}
        out[scope] = (complete, fewer)
    return out


def cells(scenario, fewer_keys):
    """The option sets of the stored files that hold at least one selected instance."""
    files = [f for f in pa.collect_files(ra.RESULTS, scenario + "_") if pa.machine_of(f) == MACHINE]
    out = {}
    for path in files:
        comments, rows = pa.read_csv(path)
        options = next((c for c in comments if c.startswith("# options ")), "")
        if "sensitiveSize=5," not in options:
            continue
        if any(key(r) in fewer_keys for r in rows):
            o = dict(kv.split("=", 1) for kv in re.findall(r"(\w+=[^,}]+)", options[len("# options "):]))
            out[(o["dataset"], o["sigma"], o["gmin"], o["gmax"], o["patternKind"], o["r"])] = o
    return out


def command(o):
    return ("python tools/run.py --jvm=-Xmx8g %s --dataset=%s --format=%s --sigma=%s --gmin=%s --gmax=%s "
            "--maxPatternLength=%s --setsPerStratum=%s --sensitiveSize=5 --patternKind=%s --timeLimit=%s "
            "--methods=%s --r=%s --closure=%s"
            % ("SC4R" if o["r"] == "3" else "SC4", o["dataset"], o["format"], o["sigma"], o["gmin"], o["gmax"],
               o["maxPatternLength"], o["setsPerStratum"], o["patternKind"], o["timeLimit"], METHODS, o["r"],
               o["closure"]))


def do_select():
    for scenario, r in SCENARIOS:
        sel = selected(scenario)
        fewer_keys = set()
        for scope in ("distinct", "repeated"):
            complete, fewer = sel[scope]
            fewer_keys |= set(fewer)
            print("%s (r = %d), %s-item scope: HHA uses fewer masks on %d of %d instances"
                  % (scenario, r, scope, len(fewer), len(complete)))
        cs = cells(scenario, fewer_keys)
        print("   cells holding them: %d" % len(cs))
        for o in sorted(cs.values(), key=lambda o: (o["dataset"], int(o["sigma"]), o["gmax"], o["patternKind"])):
            print("   " + command(o))
    return 0


def read_dir(directory):
    rows = []
    for name in sorted(os.listdir(directory)):
        if re.match(r"SC4R?_.*\.csv$", name):
            comments, rs = pa.read_csv(os.path.join(directory, name))
            options = next((c for c in comments if c.startswith("# options ")), "")
            r = int(re.search(r"[ ,{]r=(\d+)", options).group(1))
            rows += [dict(x, r=r) for x in rs]
    return rows


def summarise(selection, new_rows):
    """selection: {(r, scope): {instance: {method: stored row}}}; new_rows: rows of the diagnostic runs with key r.
    Returns (summary rows, instance rows, problems)."""
    new = defaultdict(dict)
    for x in new_rows:
        new[(x["r"], key(x))][x["method_key"]] = x
    summary, inst, problems = [], [], []
    for (r, scope), fewer in sorted(selection.items()):
        n = no_more = equal = fewer_u = more_u = same_as_ours = 0
        for k, m in sorted(fewer.items()):
            got = new.get((r, k))
            if not got or any(x not in got for x in (UNIFORM, OURS, BASE)):
                problems.append("r = %d %s: no diagnostic rows for %s" % (r, scope, k))
                continue
            if any(got[x]["status"] for x in (UNIFORM, OURS, BASE)):
                problems.append("r = %d %s: a diagnostic row has a status on %s" % (r, scope, k))
                continue
            for x in (OURS, BASE):
                if int(got[x]["mask_count"]) != int(m[x]["mask_count"]):
                    problems.append("r = %d %s: rerun of %s gives %s masks, stored %s, on %s"
                                    % (r, scope, x, got[x]["mask_count"], m[x]["mask_count"], k))
            u, h, o = (int(got[x]["mask_count"]) for x in (UNIFORM, BASE, OURS))
            n += 1
            no_more += u <= h
            equal += u == h
            fewer_u += u < h
            more_u += u > h
            same_as_ours += u == o
            inst.append({"r": r, "scope": scope, "dataset": k[0], "gap": k[1], "sigma": k[2], "sensitive_set": k[3],
                         "masks_hha": h, "masks_cut_hide": o, "masks_w0": u,
                         "lost_hha": got[BASE]["lost"], "lost_cut_hide": got[OURS]["lost"], "lost_w0": got[UNIFORM]["lost"]})
        summary.append({"r": r, "scope": scope, "selected": len(fewer), "compared": n, "w0_no_more_than_hha": no_more,
                        "w0_fewer_than_hha": fewer_u, "w0_equal_hha": equal, "w0_more_than_hha": more_u,
                        "w0_equal_cut_hide": same_as_ours})
    return summary, inst, problems


def write_tsv(path, rows):
    with open(path, "w", newline="", encoding="utf-8") as h:
        w = csv.DictWriter(h, fieldnames=list(rows[0]), delimiter="\t", lineterminator="\n")
        w.writeheader()
        w.writerows(rows)


def do_report(directory):
    selection = {}
    for scenario, r in SCENARIOS:
        for scope, (complete, fewer) in selected(scenario).items():
            selection[(r, scope)] = fewer
    summary, inst, problems = summarise(selection, read_dir(directory))
    for s in summary:
        print("r = %(r)d, %(scope)s-item: selected %(selected)d, compared %(compared)d; w = 0 uses no more masks than "
              "HHA on %(w0_no_more_than_hha)d (fewer %(w0_fewer_than_hha)d, equal %(w0_equal_hha)d), more on "
              "%(w0_more_than_hha)d; w = 0 equals CUT-HIDE on %(w0_equal_cut_hide)d" % s)
    for p in problems:
        print("PROBLEM: " + p)
    if problems or not inst:
        return 1
    write_tsv(os.path.join(directory, "summary.tsv"), summary)
    write_tsv(os.path.join(directory, "instances.tsv"), inst)
    print("wrote summary.tsv and instances.tsv in %s" % directory)
    return 0


def self_test():
    """Planted faults on synthetic rows: each must change the count it targets, and only that count."""
    def row(method, masks, status=""):
        return {"dataset": "datasets/x.txt", "gap": "[0,1]", "sigma": "10", "sensitive_set": "[[1, 1]]",
                "method_key": method, "mask_count": str(masks), "status": status, "lost": "0"}
    stored = {(2, "repeated"): {("x", "[0,1]", 10, "[[1, 1]]"): {OURS: row(OURS, 5), BASE: row(BASE, 4)}}}
    good = [dict(row(UNIFORM, 4), r=2), dict(row(OURS, 5), r=2), dict(row(BASE, 4), r=2)]
    ok = []
    s, _, p = summarise(stored, good)
    ok.append(("w = 0 equal to HHA counts as no more", s[0]["w0_no_more_than_hha"] == 1 and s[0]["w0_equal_hha"] == 1 and not p))
    s, _, p = summarise(stored, [dict(good[0], mask_count="6")] + good[1:])
    ok.append(("w = 0 above HHA counts as more", s[0]["w0_more_than_hha"] == 1 and s[0]["w0_no_more_than_hha"] == 0))
    s, _, p = summarise(stored, good[:2] + [dict(good[2], mask_count="3")])
    ok.append(("rerun of HHA that differs from the stored row is a problem", len(p) == 1))
    s, _, p = summarise(stored, good[:2])
    ok.append(("missing diagnostic row is a problem, not a pass", len(p) == 1 and s[0]["compared"] == 0))
    s, _, p = summarise(stored, [dict(good[0], status="OT")] + good[1:])
    ok.append(("a status on a diagnostic row is a problem", len(p) == 1 and s[0]["compared"] == 0))
    s, _, p = summarise(stored, [dict(x, r=3) for x in good])
    ok.append(("rows of the other r do not match", len(p) == 1))
    for name, v in ok:
        print("%-4s %s" % ("OK" if v else "FAIL", name))
    print("self-test: %d/%d" % (sum(v for _, v in ok), len(ok)))
    return 0 if all(v for _, v in ok) else 1


def main(argv):
    if argv[:1] == ["select"]:
        return do_select()
    if argv[:1] == ["report"] and len(argv) == 2:
        return do_report(argv[1])
    if argv[:1] == ["self-test"]:
        return self_test()
    print(__doc__)
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
