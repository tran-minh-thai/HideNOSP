"""Print the data-regime tables of a set of result files as Markdown, so that a summary document
embeds generated tables instead of numbers copied by hand.

Two tables:

  sigma   one row per (dataset, gap, sigma) of SIGMA files (hidenosp.exp.SigmaScan): |F_sigma|, its
          split by pattern length, the distinct-item pool, and whether the sigma rule holds.
  groups  one row per (dataset, gap, sigma) of SC2 files written with --profile=pool: the medians of
          rho_rep and beta over the pool and the groups of preregistration section 10, computed by
          thong_ke.xep_nhom, the same function that writes the locked table. This table is a
          report only; it never writes config/nhom_cau_hinh.tsv. With --profile=repeated it reads
          the files of SC2 --profile=repeated instead, the repeated-item pool.

Every row names the file it comes from, so a number in a document can be traced to its artifact.

Run: python tools/data_regime_tables.py sigma|groups [--profile=repeated] <file or directory> [...]
A directory stands for its SIGMA_*.csv (sigma) or SC2_*.csv (groups) files.
"""
import csv
import io
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)


def expand(paths, prefix):
    """The files named, with every directory replaced by its <prefix>*.csv files."""
    out = []
    for path in paths:
        if os.path.isdir(path):
            out.extend(os.path.join(path, name) for name in sorted(os.listdir(path))
                       if name.startswith(prefix) and name.endswith(".csv"))
        elif os.path.isfile(path):
            out.append(path)
        else:
            raise SystemExit("no such file or directory: " + path)
    return out


def data_rows(path):
    """Header comments and rows of one result file."""
    comments, lines = [], []
    with io.open(path, encoding="utf-8") as handle:
        for line in handle:
            (comments if line.startswith("#") else lines).append(line)
    return comments, list(csv.DictReader(lines))


def dataset_name(label):
    name = os.path.basename(label.replace("\\", "/").strip())
    return os.path.splitext(name)[0]


# The group labels of thong_ke.xep_nhom, in English; an unknown label stops the report.
GROUP = {"chinh": "main", "trung gian": "intermediate", "suy bien": "degenerate"}
BETA_GROUP = {"co hieu luc": "effective", "trung gian": "between", "suy bien": "degenerate"}


def label(table, value):
    if value not in table:
        raise SystemExit("unknown group label %r from thong_ke.xep_nhom" % value)
    return table[value]


def sigma_table(files):
    print("| dataset | gap | sigma | \\|F_sigma\\| | by length 1/2/3/4 | pool | rule | file |")
    print("|---|---|---:|---:|---|---:|---|---|")
    count = 0
    for path in files:
        _, rows = data_rows(path)
        if not rows or "meets_rule" not in rows[0]:
            raise SystemExit("%s is not a SIGMA file" % path)
        for r in rows:
            print("| %s | %s | %s | %s | %s | %s | %s | %s |" % (
                dataset_name(r["dataset"]), r["gap"], r["sigma"], r["f_sigma"], r["f_by_length"],
                r["pool"], "meets" if r["meets_rule"] == "true" else "**outside**",
                os.path.basename(path)))
            count += 1
    print()
    print("%d rows from %d files" % (count, len(files)))
    return count


def groups_table(files, profile="pool"):
    import preregistered_analysis  # needs numpy; imported here so that the sigma table runs without it

    pool_files = [p for p in files if "# profile %s\n" % profile in data_rows(p)[0]]
    source = {}
    for path in pool_files:
        _, rows = data_rows(path)
        for r in rows:
            key = (dataset_name(r["dataset"]), r["gap"], int(r["sigma"]))
            source.setdefault(key, set()).add(os.path.basename(path))
    groups = preregistered_analysis.xep_nhom(pool_files, profile=profile)
    print("| dataset | gap | sigma | pool | median rho_rep | median beta | group | beta group "
          "| file |")
    print("|---|---|---:|---:|---:|---:|---|---|---|")
    for key in sorted(groups):
        v = groups[key]
        print("| %s | %s | %d | %d | %.3f | %.3f | %s | %s | %s |" % (
            key[0], key[1], key[2], v["pool"], v["rho_rep"], v["beta"], label(GROUP, v["nhom"]),
            label(BETA_GROUP, v["nhom_beta"]),
            ", ".join(sorted(source.get(key, ())))))
    print()
    print("%d configurations from %d %s files (%d files given)"
          % (len(groups), len(pool_files), profile, len(files)))
    return len(groups)


def main(argv):
    if len(argv) < 2 or argv[0] not in ("sigma", "groups"):
        print(__doc__)
        return 2
    profile = "pool"
    rest = []
    for a in argv[1:]:
        if a.startswith("--profile="):
            profile = a.split("=", 1)[1]
            if profile not in ("pool", "repeated"):
                raise SystemExit("--profile is pool or repeated, got " + profile)
        else:
            rest.append(a)
    if argv[0] == "sigma":
        n = sigma_table(expand(rest, "SIGMA_"))
    else:
        n = groups_table(expand(rest, "SC2_"), profile)
    if n == 0:
        print("no row: nothing to report", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
