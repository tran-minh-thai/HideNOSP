"""Scratch differential fuzzing of the old and the new analysis on synthetic SC2/SC3 files.
Usage: fuzz.py <cases> <workers>. Every case is generated from its own seed, run through the old
and the new script (drive.py) and compared by compare.py. Prints a summary and the phrases of the
new reports that were reached, so that it is visible which branches the cases exercised."""
import os, random, subprocess, sys
from concurrent.futures import ThreadPoolExecutor

SP = "<checks>"
PY = "<python>"
D = SP + "/work/diff"
ROOT = {"old": os.environ.get("OLD_ROOT", SP + "/ubase"), "new": os.environ.get("NEW_ROOT", SP + "/clone")}
sys.path.insert(0, D)
import compare  # noqa: E402

SC3_COLUMNS = ["dataset", "shape", "gap", "sigma", "sensitive_set", "stratum", "method_key", "method",
               "method_seed", "hf", "lost", "lost_rel", "ghost", "dist_r", "sup_dist", "mask_count",
               "min_masks", "mask_ratio", "certified_pairs", "total_pairs", "lower_bound",
               "achieved_ratio", "frequent_before", "nonsensitive_before", "frequent_after"]
OURS = os.environ.get("OURS", "cut-hide-rw-ls")
METHODS = [OURS, "hha", "dbsh", "sbsh-r1", "sbsh-rmax", "occurrence", "cut-hide-uniform", "rr",
           "cut-hide", "cut-hide-rw"]
PHRASES = ["NOTE: in", "H0 rejected", "not rejected", "Only one baseline", "NO CONCLUSION ON H2",
           "   equivalent", "different beyond the margin", "   no conclusion", "interval not computable",
           "not enough pairs", "above 1", "near 1", "WARNING: a randomized method", "SKIPPED (missing",
           "NO USABLE FILE LEFT", "NO '%s'" % OURS, "Absent from the files", "No tier (b) baseline",
           "DIFFERENT from the locked table", "all as locked", "is locked but has no SC2 pool file",
           "is not in the locked table", "recomputed", "Every method gives r near 1", "rows in no group",
           "No SC2 file", "are reported only", "has status", "still has HF", "... and", "files skipped",
           "tested as H2-R", "does not exist yet", "NO SC2 file of the", "PROBE: no group filter",
           "-> intermediate", "/ beta between", "/ beta effective", "-> degenerate", "-> main"]

def q(s):
    return '"%s"' % s if ("," in s or '"' in s) else s

def gen(seed, case_dir):
    rnd = random.Random(seed)
    os.makedirs(case_dir + "/sc3"); os.makedirs(case_dir + "/sc2")
    datasets = rnd.sample(["d1", "d2", "d3", "d4", "protein_sdb1", "protein_sdb4"], rnd.randint(1, 4))
    gaps = rnd.sample(["[0,1]", "[0,3]"], rnd.randint(1, 2))
    sigmas = rnd.sample([100, 500, 2000], rnd.randint(1, 2))
    methods = [OURS] * (rnd.random() > 0.08) + rnd.sample(METHODS[1:], rnd.randint(0, 7))
    configs = [(d, g, s) for d in datasets for g in gaps for s in sigmas]
    with_status = rnd.random() < 0.4
    n_files = rnd.randint(1, 3)
    for f in range(n_files):
        cols = list(SC3_COLUMNS) + (["status"] if with_status else [])
        old_version = rnd.random() < 0.12
        if old_version:
            cols = [c for c in cols if c not in ("lost_rel", "nonsensitive_before")]
        lines = ["# run SC3 case %d file %d" % (seed, f), ",".join(cols)]
        for (d, g, s) in rnd.sample(configs, rnd.randint(1, len(configs))):
            for k in range(rnd.randint(1, 6)):
                if rnd.random() < 0.7:
                    a = rnd.randint(1, 9); b = rnd.randint(10, 19)
                    sset = "[[%d, %d]]" % (a, b) if rnd.random() < 0.8 else "[[%d, %d], [%d, %d]]" % (a, b, b, a + 20)
                else:
                    a = rnd.randint(1, 9)
                    sset = "[[%d, %d]]" % (a, a) if rnd.random() < 0.6 else "[[%d, %d, %d]]" % (a, a + 1, a)
                base_masks = rnd.randint(3, 30)
                for m in methods:
                    seeds = [""]
                    if m == "rr":
                        seeds = [str(2026 + i) for i in range(10 if rnd.random() < 0.7 else rnd.randint(1, 9))]
                    for sd in seeds:
                        masks = base_masks if (m == OURS or rnd.random() < 0.6) else base_masks + rnd.choice([-2, -1, 1, 2, 5])
                        nonsens = rnd.randint(20, 80)
                        lost = rnd.choice([0, 0, 1, 2, 3, 5, 8]) if m != OURS else rnd.choice([0, 0, 1, 2])
                        hf = "0.000000" if rnd.random() > 0.05 else "0.500000"
                        min_masks = base_masks if rnd.random() < 0.8 else 0
                        ratio = ("%.6f" % (masks / min_masks)) if min_masks else ""
                        row = {"dataset": "datasets/%s.txt" % d, "shape": "N=10,len=100", "gap": g,
                               "sigma": str(s), "sensitive_set": sset, "stratum": "p50", "method_key": m,
                               "method": {"rr": "RR-random", "hha": "HHA-pathcounts"}.get(m, m.upper()),
                               "method_seed": sd, "hf": hf, "lost": str(lost),
                               "lost_rel": "%.6f" % (lost / nonsens) if rnd.random() > 0.03 else "", "ghost": "0", "dist_r": "0.010000",
                               "sup_dist": "%.6f" % rnd.choice([0.0, 0.001, 0.004, 0.02, 0.05]),
                               "mask_count": str(masks), "min_masks": str(min_masks), "mask_ratio": ratio,
                               "certified_pairs": "1", "total_pairs": "1", "lower_bound": "0",
                               "achieved_ratio": "", "frequent_before": "100",
                               "nonsensitive_before": str(nonsens), "frequent_after": "99",
                               "status": rnd.choice(["", "", "", "", "", "", "OT", "OOM"])}
                        lines.append(",".join(q(row[c]) for c in cols))
        open(case_dir + "/sc3/SC3_%d_%d.csv" % (seed, f), "w", encoding="utf-8").write("\n".join(lines) + "\n")
    # SC2 files: pool and repeated profiles, with medians landing on or around the thresholds
    for profile in ("pool", "repeated"):
        if rnd.random() < 0.15:
            continue
        lines = ["# run SC2 case %d" % seed, "# profile %s" % profile,
                 "dataset,gap,sigma,pattern,sup_NO,sup_seq,sup_NO_no_gap,min_masks,mask_share,rho_rep,beta,min_masks_exact"]
        for (d, g, s) in configs:
            if rnd.random() < 0.15:
                continue
            for k in range(rnd.randint(1, 5)):
                pattern = "[%d, %d]" % (k, k + 50) if profile == "pool" else "[%d, %d]" % (k, k)
                rho = rnd.choice([0.0, 0.03, 0.05, 0.1, 0.2, 0.25, 0.6])
                beta = rnd.choice([0.0, 0.05, 0.1, 0.2, 0.4])
                lines.append('datasets/%s.txt,"%s",%d,"%s",1,1,1,1,0,%s,%s,true' % (d, g, s, pattern, rho, beta))
        open(case_dir + "/sc2/SC2_%s_%d.csv" % (profile, seed), "w", encoding="utf-8").write("\n".join(lines) + "\n")
    mode = rnd.choice(["none", "match", "match", "tamper", "missing", "extra", "skip"])
    return mode

def drive(side, case_dir, tables, args):
    return subprocess.run([PY, D + "/drive.py", side, ROOT[side] + "/tools", tables] + args,
                          cwd=case_dir, capture_output=True, text=True,
                          env=dict(os.environ, PYTHONDONTWRITEBYTECODE="1")).stdout

MAP_G = {"chinh": "main", "trung gian": "intermediate", "suy bien": "degenerate"}
MAP_B = {"co hieu luc": "effective", "trung gian": "between", "suy bien": "degenerate"}

def alter(path, mode, side, rnd):
    if not os.path.exists(path):
        return
    lines = open(path, encoding="utf-8").read().splitlines()
    data = [i for i, l in enumerate(lines) if not l.startswith("#") and not l.startswith("dataset\t")]
    if not data:
        return
    i = data[0]
    cells = lines[i].split("\t")
    if mode == "tamper":
        cells[6] = ("trung gian" if side == "old" else "intermediate") if cells[6] in ("chinh", "main") else ("chinh" if side == "old" else "main")
        lines[i] = "\t".join(cells)
    elif mode == "missing":
        del lines[i]
    elif mode == "extra":
        lines.append("\t".join(["zz_extra", "[0,1]", "7", "3", "0.300000", "0.300000",
                                "chinh" if side == "old" else "main", "co hieu luc" if side == "old" else "effective"]))
    open(path, "w", encoding="utf-8").write("\n".join(lines) + "\n")

def one(seed):
    case_dir = "%s/fuzz/case_%04d" % (D, seed)
    mode = gen(seed, case_dir)
    outs = {}
    for side in ("old", "new"):
        tables = case_dir + "/tables_" + side
        os.makedirs(tables)
        if mode in ("match", "tamper", "missing", "extra"):
            outs["write_" + side] = drive(side, case_dir, tables, ["--sc2", "sc2", "--write-groups"])
            for f in ("pool.tsv", "repeated.tsv"):
                alter(os.path.join(tables, f), mode, side, None)
        args = ["--sc3", "sc3", "--sc2", "sc2"] + (["--skip-groups"] if mode == "skip" else [])
        outs[side] = drive(side, case_dir, tables, args)
    results = []
    if "write_old" in outs:
        results.append(("case %04d %s write-groups" % (seed, mode), outs["write_old"], outs["write_new"]))
    results.append(("case %04d %s analysis" % (seed, mode), outs["old"], outs["new"]))
    return results, outs["new"]

def main(cases, workers):
    import io, contextlib
    ok = bad = 0
    reached = {p: 0 for p in PHRASES}
    with ThreadPoolExecutor(max_workers=workers) as pool:
        for results, new_text in pool.map(one, range(1, cases + 1)):
            for name, o, n in results:
                buf = io.StringIO()
                with contextlib.redirect_stdout(buf):
                    same = compare.compare(o, n, name)
                if same:
                    ok += 1
                else:
                    bad += 1; print(buf.getvalue(), end="")
            for p in PHRASES:
                reached[p] += p in new_text
    print("comparisons: %d SAME, %d DIFFERENT" % (ok, bad))
    print("branches reached (number of cases whose new report contains the phrase):")
    for p in PHRASES:
        print("  %5d  %s" % (reached[p], p))
    return 1 if bad else 0

if __name__ == "__main__":
    sys.exit(main(int(sys.argv[1]), int(sys.argv[2])))
