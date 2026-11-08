# SPDX-License-Identifier: MIT
"""Run every experiment of the paper on one measuring machine, cell by cell, and resume after a stop.

    python tools/campaign.py plan  --machine=NAME [--generation=G] [--batch=B]
    python tools/campaign.py lock  --machine=NAME [--generation=G]
    python tools/campaign.py run   --machine=NAME [--generation=G] --batch=B [--branch=campaign-NAME]
                                   [--report-ps1=PATH --status-project=HideNOSP] [--retry-failed]
    python tools/campaign.py --self-test

A campaign is the ordered list of batches below. Each batch is a list of cells, one call of
tools/run.py each, with the options that name the cell. The cells come from the locked tables of
config/ through tools/sc3_commands.py, never from a hand-written list.

    sc1             SC1 --trials=1500 --seed=2026
    sc2             SC2 on every row of both group tables (pool and repeated profiles)
    sc3-s1 .. s20   SC3 method batch of the main group, |S| = 1, 5, 10, 20 (preregistration 14)
    sc3-degenerate  SC3 |S| = 1 on the degenerate group (preregistration 16)
    sc3-refine      SC3 cut-hide-refine, |S| = 5, 10, 20 (preregistration 17, secondary)
    sc3lb           SC3LB lower bounds on the cells of sc3-s1 .. s20
    sc3opt          SC3OPT exact optimum of instances of 8 and 16 sequences, |S| = 5, 10, 20
    sc3-pareto      SC3 --methods=none --pareto=true, |S| = 1 (heap -Xmx32g)

Generation 2 (preregistration section 19), run with --generation=2:

    sc3lost         SC3LOST, exact Lost optimum on windows of real sequences, cells of sc3-s1 (19.2)
    sc3-stop        SC3 --methods=occurrence,hha-stop on the cells of sc3-s1 .. s20 (19.1)
    sc3cert         SC3CERT, certified lower bound on the optimum Lost, cells of sc3-s1 (20.2)
    sc6             SC6 running time, cells of sc3-s1 and sc3-s10, one set per stratum (19.3, 20.1)

Generation 3 (preregistration section 21), run with --generation=3: the invariant batches again with the
corrected S-plus (option closure=necessary, FrequentPatterns.Closure), because the methods read S-plus as
well as the measures. Each g3- batch is its batch of generation 1 or 2, cell for cell, with that option
added, so a file of the earlier rule (no closure option) never counts as one of its cells. A cell is matched on the
"# closure" header line of the file (no line: the earlier rule), and a cell of generation 1 or 2 of these scenarios
runs with --closure=subsequence, so that it reproduces the files of the earlier rule:

    g3-sc3-s1 .. g3-sc3-s20, g3-sc3-degenerate, g3-sc3-refine, g3-sc3-stop, g3-sc3lost, g3-sc3cert, g3-sc3opt

A cell is done when results-invariant/ holds a complete file of its scenario whose run stamp names
THIS machine and whose options line names the cell (SC1 writes no options line: scenario, machine and
seed). Files of other machines never count, so a new machine reruns everything. A cell whose run
failed is recorded in results-invariant/campaign_<machine>/cells.tsv and is not retried unless
--retry-failed is given; a failure is a result too, and retrying it on every resume would spend the
machine on the same failure again.

After each cell the new result files, the cell's console log (with the repository root and the home
folder replaced by <repository> and <home>) and the ledger are committed, and the commit is pushed to
the campaign branch. A failed push only warns; the commit leaves with the next push. With
--report-ps1, one status line per cell goes to the run-status channel through its report.ps1.

lock writes results-invariant/campaign_<machine>/ENV_LOCK.json: the machine, the JDK (runtime version
and vendor) and the tree hashes of src/ and pom.xml. run refuses when any of them differs from the
lock, so one campaign never mixes two JDKs or two versions of the measured code.

A later generation runs batches that need code written after the first lock. It has its own folder
results-invariant/campaign_<machine>_g<G>/ (lock, ledger, logs), and its lock is written only when the
machine, the host name and the JDK (runtime version and vendor) equal those of the generation before:
only the measured code may differ. Every batch belongs to exactly one generation, and run refuses a
batch under another generation, so old cells are never rerun with new code by mistake.

Exit status: 0 when the batch ran (failed cells are reported, not fatal), 1 on a refused lock or a
bad argument, 2 when the self-test fails.
"""
import csv
import datetime
import io
import json
import os
import platform
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent
sys.path.insert(0, str(HERE))
import run as run_tool  # noqa: E402
import sc3_commands as sc3  # noqa: E402

HEAP = "-Xmx8g"
PARETO_HEAP = "-Xmx32g"          # preregistration section 18; the method batches keep -Xmx8g
OPT_SIZES = "8,16"
# Generation 3: the batches whose methods or measures read S-plus, rerun with the corrected rule.
CLOSURE_BATCHES = ("sc3-s1", "sc3-s5", "sc3-s10", "sc3-s20", "sc3-degenerate", "sc3-refine", "sc3-stop",
                   "sc3lost", "sc3cert", "sc3opt")
CLOSURE_OPTION = ("closure", "necessary")
# The scenarios whose methods or measures read S-plus. A cell of theirs without a closure option is a cell of the
# earlier rule: it runs with --closure=subsequence and is done only by a file whose "# closure" header line is
# subsequence or absent (files written before the rule existed carry no such line).
CLOSURE_SCENARIOS = ("SC3", "SC3LOST", "SC3CERT", "SC3OPT", "SC6")
EARLIER_RULE = "subsequence"
HEADER_RULE = "# closure "
BATCHES = ("sc1", "sc2", "sc3-s1", "sc3-s5", "sc3-s10", "sc3-s20", "sc3-degenerate", "sc3-refine",
           "sc3lb", "sc3opt", "sc3-pareto", "sc3-stop", "sc3lost", "sc3cert", "sc6")
# The generation a batch belongs to (preregistration section 19 opened the second).
GENERATION = {b: "1" for b in BATCHES}
GENERATION.update({"sc3-stop": "2", "sc3lost": "2", "sc3cert": "2", "sc6": "2"})
BATCHES = BATCHES + tuple("g3-" + b for b in CLOSURE_BATCHES)
GENERATION.update({"g3-" + b: "3" for b in CLOSURE_BATCHES})
# What a later generation must share with the one before: only the measured code may differ.
CARRIED_KEYS = ("machine", "hostname", "java_runtime_version", "java_vendor")
LOCKED_KEYS = ("machine", "hostname", "java_runtime_version", "java_vendor", "source_tree", "pom")
LEDGER_COLUMNS = ("time_utc", "batch", "cell", "exit", "outcome", "result_files", "log", "path_replacements")
SC2_TABLES = (("config/configuration_groups.tsv", "pool"),
              ("config/configuration_groups_repeated.tsv", "repeated"))
# The run stamp of RunId, as tools/check_results.py reads it: time _ machine _ revision _ s<seed>.
STAMP = re.compile(r"^# run (\d{8}-\d{6}(?:-\d{3})?)_([^_]+)_(.+)_s(-?\d+)$")
CELL_KEYS = ("dataset", "format", "sigma", "gmin", "gmax", "maxPatternLength", "setsPerStratum",
             "sensitiveSize", "patternKind")


# ---------------------------------------------------------------- the plan

def sc2_cells(root):
    cells = []
    for name, profile in SC2_TABLES:
        kind = "distinct" if profile == "pool" else "repeated"
        for group in sc3.GROUPS:
            for c in sc3.read_table(root / name, kind, group):
                cells.append({"dataset": c["dataset"], "format": "SPMF", "sigma": c["sigma"], "gmin": c["gmin"],
                              "gmax": c["gmax"], "maxPatternLength": "4", "profile": profile})
    return cells


def cell(scenario, options, heap=HEAP, extra=()):
    """One run: scenario, ordered options (name -> value), JVM heap."""
    return {"scenario": scenario, "options": list(options.items()) + list(extra), "heap": heap}


def batch_cells(root, batch):
    if batch.startswith("g3-") and batch[3:] in CLOSURE_BATCHES:
        return [dict(c, options=c["options"] + [CLOSURE_OPTION]) for c in batch_cells(root, batch[3:])]
    if batch == "sc1":
        return [cell("SC1", {"trials": "1500", "seed": "2026"})]
    if batch == "sc2":
        return [cell("SC2", c) for c in sc2_cells(root)]
    match = re.fullmatch(r"sc3-s(\d+)", batch)
    if match:
        return [cell("SC3", {k: c[k] for k in sc3.IDENTITY}) for c in sc3.planned(root, int(match.group(1)))]
    if batch == "sc3-degenerate":
        return [cell("SC3", {k: c[k] for k in sc3.IDENTITY}) for c in sc3.planned(root, 1, group="degenerate")]
    if batch == "sc3-refine":
        return [cell("SC3", {k: c[k] for k in sc3.IDENTITY}) for size in (5, 10, 20)
                for c in sc3.planned(root, size, refine=True)]
    if batch == "sc3lb":
        return [cell("SC3LB", {k: c[k] for k in CELL_KEYS}) for size in (1, 5, 10, 20)
                for c in sc3.planned(root, size)]
    if batch == "sc3opt":
        return [cell("SC3OPT", {k: c[k] for k in CELL_KEYS}, extra=[("sequencesPerInstance", OPT_SIZES)])
                for size in (5, 10, 20) for c in sc3.planned(root, size)]
    if batch == "sc3-stop":
        return [cell("SC3", {k: c[k] for k in sc3.IDENTITY}) for size in (1, 5, 10, 20)
                for c in sc3.planned(root, size, stop=True)]
    if batch == "sc3lost":
        return [cell("SC3LOST", {k: c[k] for k in CELL_KEYS if k != "sensitiveSize"}) for c in sc3.planned(root, 1)]
    if batch == "sc3cert":
        return [cell("SC3CERT", {k: c[k] for k in CELL_KEYS if k != "sensitiveSize"}) for c in sc3.planned(root, 1)]
    if batch == "sc6":
        return [cell("SC6", {k: c[k] for k in CELL_KEYS}, extra=[("timeLimit", "1800"), ("warmups", "1")])
                for size in (1, 10) for c in sc3.planned(root, size)]
    if batch == "sc3-pareto":
        return [cell("SC3", {k: c[k] for k in sc3.IDENTITY}, heap=PARETO_HEAP) for c in sc3.planned(root, 1, front=True)]
    raise ValueError("unknown batch %s; batches: %s" % (batch, ", ".join(BATCHES)))


def label(c):
    o = dict(c["options"])
    if c["scenario"] == "SC1":
        return "SC1 trials=%s" % o["trials"]
    parts = [c["scenario"], Path(o["dataset"]).stem, "[%s,%s]" % (o["gmin"], o["gmax"]), "sigma=%s" % o["sigma"]]
    for key, name in (("profile", ""), ("patternKind", ""), ("sensitiveSize", "S="), ("methods", ""),
                      ("closure", "closure=")):
        if key in o:
            value = o[key]
            if key == "methods":
                value = {"none": "pareto", sc3.REFINE: "refine", sc3.STOP: "stop"}.get(value, "methods")
            parts.append(name + value)
    return " ".join(parts)


# ---------------------------------------------------------------- what is done

def result_files(root, machine):
    """Complete files of results-invariant/ and results/ stamped with this machine: (path, scenario, options)."""
    out = []
    for tree in ("results-invariant", "results"):
        directory = root / tree
        if not directory.is_dir():
            continue
        for path in sorted(directory.iterdir()):
            if not (path.is_file() and path.name.endswith(".csv")):
                continue
            stamp, scenario, options, rule = None, None, None, EARLIER_RULE
            with io.open(path, encoding="utf-8", newline="") as handle:
                for line in handle:
                    line = line.rstrip("\r\n")
                    if not line.startswith("#"):
                        break
                    if line.startswith("# run "):
                        stamp = line[len("# run "):]
                    elif line.startswith("# scenario "):
                        scenario = line[len("# scenario "):].strip()
                    elif line.startswith("# options ") and options is None:
                        options = sc3.parse_options(line)
                    elif line.startswith(HEADER_RULE):
                        rule = line[len(HEADER_RULE):].strip()
            match = STAMP.match("# run " + stamp) if stamp else None
            if options is not None:
                options = dict(options, **{HEADER_RULE: rule})
            if match and match.group(2) == machine:
                out.append((path, scenario, options, match.group(4)))
    return out


def wanted_rule(c):
    """The closure rule a cell asks for: its closure option, or the earlier rule; None when the scenario has none."""
    if c["scenario"] not in CLOSURE_SCENARIOS:
        return None
    return dict(c["options"]).get("closure", EARLIER_RULE)


def is_done(c, files):
    wanted = dict(c["options"])
    rule = wanted_rule(c)
    for path, scenario, options, seed in files:
        if scenario != c["scenario"]:
            continue
        if options is None:
            # SC1 writes no options line: scenario, machine and seed name the run.
            if c["scenario"] == "SC1" and seed == wanted.get("seed"):
                return path
            continue
        if rule is not None and options.get(HEADER_RULE, EARLIER_RULE) != rule:
            continue
        same = True
        for key, value in wanted.items():
            given = options.get(key)
            if key == "dataset" and given is not None:
                given = given.replace("\\", "/")
            if given != value:
                same = False
                break
        if same:
            return path
    return None


def campaign_dir(root, machine, generation="1"):
    """results-invariant/campaign_<machine> for the first generation, campaign_<machine>_g<G> after."""
    return root / "results-invariant" / ("campaign_" + machine + ("" if generation == "1" else "_g" + generation))


def ledger_path(root, machine, generation="1"):
    return campaign_dir(root, machine, generation) / "cells.tsv"


def read_ledger(root, machine, generation="1"):
    path = ledger_path(root, machine, generation)
    if not path.is_file():
        return []
    with io.open(path, encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle, delimiter="\t"))


def failed_cells(root, machine, generation="1"):
    rows = read_ledger(root, machine, generation)
    last = {}
    for r in rows:
        last[(r["batch"], r["cell"])] = r["outcome"]
    return {k for k, v in last.items() if v == "failed"}


def status(root, machine, batch):
    files = result_files(root, machine)
    failed = failed_cells(root, machine, GENERATION[batch])
    cells = batch_cells(root, batch)
    done = [c for c in cells if is_done(c, files)]
    gave_up = [c for c in cells if not is_done(c, files) and (batch, label(c)) in failed]
    return cells, done, gave_up


# ---------------------------------------------------------------- environment lock

def java_properties(java):
    out = subprocess.run([java, "-XshowSettings:properties", "-version"], capture_output=True, text=True)
    text = (out.stderr or "") + (out.stdout or "")
    props = dict(re.findall(r"^\s*([\w.]+) = (.*)$", text, re.M))
    return {k: v.strip() for k, v in props.items()}


def hostname():
    return os.environ.get("COMPUTERNAME") or platform.node().split(".")[0]


def ram_bytes():
    try:
        if os.name == "nt":
            import ctypes

            class Status(ctypes.Structure):
                _fields_ = [("length", ctypes.c_ulong), ("load", ctypes.c_ulong), ("total", ctypes.c_ulonglong),
                            ("avail", ctypes.c_ulonglong), ("page_total", ctypes.c_ulonglong),
                            ("page_avail", ctypes.c_ulonglong), ("virtual_total", ctypes.c_ulonglong),
                            ("virtual_avail", ctypes.c_ulonglong), ("extended", ctypes.c_ulonglong)]
            s = Status()
            s.length = ctypes.sizeof(Status)
            ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(s))
            return str(s.total)
        if platform.system() == "Darwin":
            return subprocess.run(["sysctl", "-n", "hw.memsize"], capture_output=True, text=True).stdout.strip()
        with open("/proc/meminfo", encoding="utf-8") as f:
            return str(int(f.readline().split()[1]) * 1024)
    except (OSError, ValueError, AttributeError) as e:
        return "not reported: %s" % e


def git_out(root, *args):
    out = subprocess.run(["git", "-C", str(root)] + list(args), capture_output=True, text=True)
    return out.stdout.strip() if out.returncode == 0 else None


def environment(root, machine):
    props = java_properties(run_tool.java_executable())
    mvn = "mvn.cmd" if os.name == "nt" else "mvn"
    try:
        maven = subprocess.run([mvn, "-v"], capture_output=True, text=True).stdout.strip().splitlines()[0]
    except (FileNotFoundError, IndexError):
        maven = "not found"
    return {
        "machine": machine, "hostname": hostname(), "os": platform.platform(), "architecture": platform.machine(),
        "cpu": run_tool.cpu_model(), "logical_processors": str(os.cpu_count()), "ram_bytes": ram_bytes(),
        "java_runtime_version": props.get("java.runtime.version", "unknown"),
        "java_vendor": props.get("java.vendor", "unknown"), "java_vm": props.get("java.vm.name", "unknown"),
        "maven": maven, "python": platform.python_version(),
        "source_tree": git_out(root, "rev-parse", "HEAD:src") or "unknown",
        "pom": git_out(root, "rev-parse", "HEAD:pom.xml") or "unknown",
        "commit": git_out(root, "rev-parse", "HEAD") or "unknown",
    }


def lock_path(root, machine, generation="1"):
    return campaign_dir(root, machine, generation) / "ENV_LOCK.json"


def lock_differences(lock, now):
    return ["%s: locked %r, now %r" % (k, lock.get(k), now.get(k)) for k in LOCKED_KEYS if lock.get(k) != now.get(k)]


def do_lock(root, machine, env=None, generation="1"):
    env = env or environment(root, machine)
    path = lock_path(root, machine, generation)
    if generation != "1" and not path.is_file():
        previous = lock_path(root, machine, str(int(generation) - 1))
        if not previous.is_file():
            print("REFUSED: generation %s needs the lock of generation %d first: %s"
                  % (generation, int(generation) - 1, previous.relative_to(root).as_posix()))
            return 1
        before = json.loads(previous.read_text(encoding="utf-8"))
        moved = ["%s: generation %d has %r, now %r" % (k, int(generation) - 1, before.get(k), env.get(k))
                 for k in CARRIED_KEYS if before.get(k) != env.get(k)]
        if moved:
            print("REFUSED: a new generation may change the measured code only:")
            for d in moved:
                print("  " + d)
            return 1
        env = dict(env, previous_lock=previous.relative_to(root).as_posix(),
                   previous_source_tree=before.get("source_tree", "unknown"))
    if path.is_file():
        lock = json.loads(path.read_text(encoding="utf-8"))
        diff = lock_differences(lock, env)
        if diff:
            print("REFUSED: %s exists with another environment:" % path.relative_to(root).as_posix())
            for d in diff:
                print("  " + d)
            return 1
        print("lock: %s matches this environment" % path.relative_to(root).as_posix())
        return 0
    path.parent.mkdir(parents=True, exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(env, handle, indent=2, sort_keys=True)
        handle.write("\n")
    print("lock: wrote %s" % path.relative_to(root).as_posix())
    for k in sorted(env):
        print("  %-22s %s" % (k, env[k]))
    return 0


# ---------------------------------------------------------------- running one cell

def sanitize(text, root):
    """Replace the repository root and the home folder by placeholders; return (text, count)."""
    count = 0
    targets = []
    for base, mark in ((root, "<repository>"), (Path.home(), "<home>")):
        native = str(base)
        for form in {native, native.replace("\\", "/"), native.replace("/", "\\")}:
            targets.append((form, mark))
    for form, mark in sorted(targets, key=lambda t: -len(t[0])):
        if not form:
            continue
        pattern = re.compile(re.escape(form), re.I if os.name == "nt" else 0)
        text, n = pattern.subn(mark, text)
        count += n
    return text, count


def real_runner(root, machine, c):
    command = [sys.executable, str(root / "tools" / "run.py"), "--jvm=" + c["heap"], c["scenario"],
               "--machine=" + machine] + ["--%s=%s" % kv for kv in c["options"]]
    if wanted_rule(c) == EARLIER_RULE and "closure" not in dict(c["options"]):
        command.append("--closure=" + EARLIER_RULE)
    process = subprocess.Popen(command, cwd=str(root), stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                               text=True, encoding="utf-8", errors="replace")
    lines = []
    for line in process.stdout:
        sys.stdout.write(line)
        sys.stdout.flush()
        lines.append(line)
    return process.wait(), "".join(lines)


def top_level(root):
    out = set()
    for tree in ("results-invariant", "results"):
        directory = root / tree
        if directory.is_dir():
            out |= {p for p in directory.iterdir() if p.is_file()}
    return out


def git_commit_push(root, paths, message, branch):
    rel = [p.relative_to(root).as_posix() for p in paths]
    out = subprocess.run(["git", "-C", str(root), "add", "--"] + rel, capture_output=True, text=True)
    if out.returncode != 0:
        return "git add failed: " + out.stderr.strip()
    staged = subprocess.run(["git", "-C", str(root), "diff", "--cached", "--quiet"])
    if staged.returncode == 0:
        return "nothing to commit"
    out = subprocess.run(["git", "-C", str(root), "commit", "-q", "-m", message], capture_output=True, text=True)
    if out.returncode != 0:
        return "git commit failed: " + (out.stderr or out.stdout).strip()
    if not branch:
        return "committed"
    out = subprocess.run(["git", "-C", str(root), "push", "-q", "origin", "HEAD:refs/heads/" + branch],
                         capture_output=True, text=True)
    if out.returncode != 0:
        return "committed, push failed (leaves with the next push): " + out.stderr.strip().replace("\n", " ")
    return "pushed"


def report(ps1, project, message):
    if not ps1:
        return
    shell = "powershell" if os.name == "nt" else "pwsh"
    text = message.replace("'", "")
    command = ". '%s'; $StatusProject = '%s'; Report '%s'" % (ps1, project, text)
    try:
        subprocess.run([shell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", command], timeout=300)
    except (OSError, subprocess.TimeoutExpired) as e:
        print("WARNING: status line not sent (%s): %s" % (e, text), flush=True)


def append_ledger(root, machine, row, generation="1"):
    path = ledger_path(root, machine, generation)
    path.parent.mkdir(parents=True, exist_ok=True)
    new = not path.is_file()
    with io.open(path, "a", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        if new:
            writer.writerow(LEDGER_COLUMNS)
        writer.writerow([row[k] for k in LEDGER_COLUMNS])


def do_run(root, machine, batch, branch=None, ps1=None, project="HideNOSP", retry_failed=False,
           runner=real_runner, env=None):
    generation = GENERATION[batch]
    path = lock_path(root, machine, generation)
    if not path.is_file():
        print("REFUSED: no environment lock at %s; run 'python tools/campaign.py lock --machine=%s%s' first"
              % (path.relative_to(root).as_posix(), machine,
                 "" if generation == "1" else " --generation=" + generation))
        return 1
    diff = lock_differences(json.loads(path.read_text(encoding="utf-8")), env or environment(root, machine))
    if diff:
        print("REFUSED: this environment differs from %s:" % path.relative_to(root).as_posix())
        for d in diff:
            print("  " + d)
        report(ps1, project, "%s: REFUSED, environment differs from the lock (%s)" % (batch, "; ".join(diff)))
        return 1
    stale = [p for tree in ("results-invariant", "results") if (root / tree).is_dir()
             for p in (root / tree).iterdir() if p.name.endswith(".partial")]
    for p in stale:
        p.unlink()
    if stale:
        report(ps1, project, "%s: removed %d .partial file(s) of a run stopped before" % (batch, len(stale)))
    cells, done, gave_up = status(root, machine, batch)
    todo = [c for c in cells if c not in done and (retry_failed or c not in gave_up)]
    report(ps1, project, "%s: start, %d cells, %d done, %d failed before%s, %d to run"
           % (batch, len(cells), len(done), len(gave_up), " (retried)" if retry_failed else "", len(todo)))
    ran = failed = 0
    for index, c in enumerate(todo, 1):
        name = label(c)
        before = top_level(root)
        start = datetime.datetime.now(datetime.timezone.utc)
        code, console = runner(root, machine, c)
        seconds = (datetime.datetime.now(datetime.timezone.utc) - start).total_seconds()
        after = top_level(root)
        new = sorted(after - before)
        partial = [p for p in new if p.name.endswith(".partial")]
        for p in partial:
            p.unlink()
        new = [p for p in new if not p.name.endswith(".partial")]
        complete = is_done(c, result_files(root, machine)) is not None
        outcome = "done" if code == 0 and complete else "failed"
        ran += 1
        failed += outcome == "failed"
        logdir = campaign_dir(root, machine, generation) / "logs" / batch
        logdir.mkdir(parents=True, exist_ok=True)
        log = logdir / ("%s_%s.log" % (start.strftime("%Y%m%d-%H%M%S"), re.sub(r"[^A-Za-z0-9.=-]+", "_", name)))
        text, replaced = sanitize(console, root)
        header = "# cell %s\n# machine %s\n# exit %d after %.0f s; outcome %s; removed .partial: %d\n" % (
            name, machine, code, seconds, outcome, len(partial))
        log.write_text(header + text, encoding="utf-8", newline="\n")
        append_ledger(root, machine, {"time_utc": start.strftime("%Y-%m-%dT%H:%M:%SZ"), "batch": batch, "cell": name,
                                      "exit": str(code), "outcome": outcome,
                                      "result_files": " ".join(p.name for p in new),
                                      "log": log.relative_to(root).as_posix(), "path_replacements": str(replaced)},
                      generation)
        git = git_commit_push(root, new + [log, ledger_path(root, machine, generation)],
                              "Results %s on %s: %s (%s)" % (batch, machine, name, outcome), branch)
        report(ps1, project, "%s %d/%d %s: exit %d, %s, %.0f s, %s" % (batch, index, len(todo), name, code, outcome,
                                                                       seconds, git))
    cells, done, gave_up = status(root, machine, batch)
    report(ps1, project, "%s: finished, %d of %d cells done, %d failed (ran %d now)"
           % (batch, len(done), len(cells), len(gave_up), ran))
    return 0


def do_plan(root, machine, batches):
    total = 0
    for b in batches:
        cells, done, gave_up = status(root, machine, b)
        todo = len(cells) - len(done) - len(gave_up)
        total += todo
        print("%-15s %4d cells  %4d done  %3d failed  %4d to run  heap %s"
              % (b, len(cells), len(done), len(gave_up), todo, cells[0]["heap"] if cells else "-"))
    print("%d cells to run on %s" % (total, machine))
    return 0


# ---------------------------------------------------------------- command line

def main(argv):
    if argv[:1] == ["--self-test"]:
        return self_test()
    if not argv or argv[0] not in ("plan", "lock", "run"):
        print(__doc__)
        return 1
    action, args = argv[0], dict(a[2:].split("=", 1) if "=" in a else (a[2:], "true") for a in argv[1:])
    machine = args.get("machine")
    if not machine or "_" in machine:
        print("--machine=NAME is required and may not contain '_' (it is a field of the run stamp)")
        return 1
    if action in ("lock", "run") and machine != hostname():
        print("REFUSED: --machine=%s but this machine is %s (COMPUTERNAME / host name). The label must be the"
              " machine that runs." % (machine, hostname()))
        return 1
    generation = args.get("generation", "1")
    if generation not in set(GENERATION.values()):
        print("--generation is one of: %s" % ", ".join(sorted(set(GENERATION.values()))))
        return 1
    own = [b for b in BATCHES if GENERATION[b] == generation]
    if action == "plan":
        return do_plan(REPO, machine, [args["batch"]] if "batch" in args else own)
    if action == "lock":
        return do_lock(REPO, machine, generation=generation)
    batch = args.get("batch")
    if batch not in BATCHES:
        print("--batch is one of: %s" % ", ".join(BATCHES))
        return 1
    if GENERATION[batch] != generation:
        print("REFUSED: batch %s belongs to generation %s; this command names generation %s"
              % (batch, GENERATION[batch], generation))
        return 1
    return do_run(REPO, machine, batch, branch=args.get("branch"), ps1=args.get("report-ps1"),
                  project=args.get("status-project", "HideNOSP"), retry_failed="retry-failed" in args)


# ---------------------------------------------------------------- self-test

def self_test():
    checks, failures = 0, []

    def check(name, ok, detail=""):
        nonlocal checks
        checks += 1
        print("%-70s %s  %s" % (name, "ok" if ok else "FAILED", detail))
        if not ok:
            failures.append(name)

    # 1. The plan of the real repository: the number of cells of each batch.
    expected = {"sc1": 1, "sc2": 81, "sc3-s1": 54, "sc3-s5": 34, "sc3-s10": 27, "sc3-s20": 21, "sc3-degenerate": 17,
                "sc3-refine": 82, "sc3lb": 136, "sc3opt": 82, "sc3-pareto": 54, "sc3-stop": 136, "sc3lost": 54, "sc3cert": 54,
                "sc6": 81}
    expected.update({"g3-" + b: expected[b] for b in CLOSURE_BATCHES})
    for b in BATCHES:
        n = len(batch_cells(REPO, b))
        check("plan of %s has %d cells" % (b, expected[b]), n == expected[b], str(n))
    check("only the Pareto batch runs at -Xmx32g",
          {b for b in BATCHES if any(c["heap"] == PARETO_HEAP for c in batch_cells(REPO, b))} == {"sc3-pareto"})
    labels = [label(c) for b in BATCHES for c in batch_cells(REPO, b)]
    check("cell labels are unique across the campaign", len(labels) == len(set(labels)),
          "%d labels, %d distinct" % (len(labels), len(set(labels))))
    same = all(len(batch_cells(REPO, "g3-" + b)) == len(batch_cells(REPO, b)) and all(
        g["scenario"] == c["scenario"] and g["heap"] == c["heap"] and g["options"] == c["options"] + [CLOSURE_OPTION]
        for g, c in zip(batch_cells(REPO, "g3-" + b), batch_cells(REPO, b))) for b in CLOSURE_BATCHES)
    check("every g3 batch is its base batch, cell for cell, plus closure=necessary", same)
    check("the g3 batches belong to generation 3 and nothing else does",
          {b for b in BATCHES if GENERATION[b] == "3"} == {"g3-" + b for b in CLOSURE_BATCHES})
    base = batch_cells(REPO, "sc3-s1")[0]
    g3 = batch_cells(REPO, "g3-sc3-s1")[0]
    old_file = [(Path("old.csv"), "SC3", dict(base["options"]), "2026")]
    new_file = [(Path("new.csv"), "SC3", dict(g3["options"], **{HEADER_RULE: "necessary"}), "2026")]
    other_rule = [(Path("sub.csv"), "SC3", dict(base["options"], closure="subsequence", **{HEADER_RULE: "subsequence"}),
                   "2026")]
    check("a file of the earlier rule does not count as a g3 cell, and the g3 file does",
          is_done(g3, old_file) is None and is_done(g3, new_file) is not None and is_done(g3, other_rule) is None)
    check("a g3 file does not count as its base cell, and a file of the earlier rule does",
          is_done(base, new_file) is None and is_done(base, old_file) is not None and is_done(base, other_rule) is not None)
    check("a base cell of a closure scenario runs with --closure=subsequence, a g3 cell with its own option",
          wanted_rule(base) == "subsequence" and wanted_rule(g3) == "necessary"
          and wanted_rule(batch_cells(REPO, "sc2")[0]) is None)
    official = [(p, s, o, seed) for p, s, o, seed in result_files(REPO, "DESKTOP-L8A6EPO") if s == "SC3"]
    covered = sum(1 for b in ("sc3-s1", "sc3-s5", "sc3-s10", "sc3-s20")
                  for c in batch_cells(REPO, b) if is_done(c, official))
    check("the official SC3 files of the old machine cover its 136 method cells", covered == 136, str(covered))

    # 2. A small repository with a bare remote: resume, other machines, failures, pushes, the lock.
    tmp = Path(tempfile.mkdtemp(prefix="campaign"))
    try:
        root, remote = tmp / "repo", tmp / "remote.git"
        shutil.copytree(REPO / "config", root / "config")
        (root / "tools").mkdir()
        (root / "results-invariant").mkdir()
        (root / "results").mkdir()
        (root / "src").mkdir()
        (root / "src" / "A.java").write_text("class A {}\n", encoding="utf-8")
        (root / "pom.xml").write_text("<project/>\n", encoding="utf-8")
        subprocess.run(["git", "init", "-q", str(root)], check=True)
        subprocess.run(["git", "init", "-q", "--bare", str(remote)], check=True)
        for k, v in (("user.name", "test"), ("user.email", "test@example.org")):
            subprocess.run(["git", "-C", str(root), "config", k, v], check=True)
        subprocess.run(["git", "-C", str(root), "add", "-A"], check=True)
        subprocess.run(["git", "-C", str(root), "commit", "-q", "-m", "start"], check=True)
        subprocess.run(["git", "-C", str(root), "remote", "add", "origin", str(remote)], check=True)
        env = {"machine": "XPS", "hostname": "XPS", "java_runtime_version": "25.0.4.1+1",
               "java_vendor": "Eclipse Adoptium", "source_tree": git_out(root, "rev-parse", "HEAD:src"),
               "pom": git_out(root, "rev-parse", "HEAD:pom.xml")}
        calls = []
        fail_label = label(batch_cells(root, "sc3-degenerate")[2])

        def fake(root_, machine, c):
            calls.append(label(c))
            o = dict(c["options"])
            stamp = "20261101-000000-%03d_%s_abc_s2026" % (len(calls), machine)
            path = root_ / "results-invariant" / ("%s_%s.csv" % (c["scenario"], stamp))
            if label(c) == fail_label:
                (root_ / "results-invariant" / (path.name + ".partial")).write_text("# run x\n", encoding="utf-8")
                return 1, "java.lang.OutOfMemoryError in %s\n" % root_
            body = "# run %s\n# scenario %s\n" % (stamp, c["scenario"])
            if c["scenario"] != "SC1":
                body += "# options {machine=%s, %s}\n" % (machine, ", ".join("%s=%s" % kv for kv in o.items()))
            path.write_text(body + "dataset,status\nx,\n", encoding="utf-8")
            return 0, "wrote %s\n" % path

        check("run without a lock is refused", do_run(root, "XPS", "sc1", runner=fake, env=env) == 1 and not calls)
        do_lock(root, "XPS", env=env)
        changed = dict(env, java_runtime_version="25.0.5+8")
        check("run with another JDK than the lock is refused",
              do_run(root, "XPS", "sc1", runner=fake, env=changed) == 1 and not calls)
        check("lock with another JDK is refused", do_lock(root, "XPS", env=changed) == 1)
        do_run(root, "XPS", "sc1", branch="campaign-xps", runner=fake, env=env)
        do_run(root, "XPS", "sc3-degenerate", branch="campaign-xps", runner=fake, env=env)
        first = len(calls)
        check("first pass runs 1 + 17 cells", first == 18, str(first))
        do_run(root, "XPS", "sc3-degenerate", branch="campaign-xps", runner=fake, env=env)
        check("second pass runs nothing (16 done, 1 failed and not retried)", len(calls) == first, str(len(calls) - first))
        do_run(root, "XPS", "sc3-degenerate", branch="campaign-xps", runner=fake, env=env, retry_failed=True)
        check("--retry-failed reruns exactly the failed cell", calls[first:] == [fail_label], str(calls[first:]))
        partials = [p.name for p in (root / "results-invariant").iterdir() if p.name.endswith(".partial")]
        check("no .partial file is left", not partials, str(partials))
        ledger = read_ledger(root, "XPS")
        check("ledger records 19 runs, 2 failed", len(ledger) == 19 and sum(r["outcome"] == "failed" for r in ledger) == 2,
              "%d rows" % len(ledger))
        logs = list((root / "results-invariant" / "campaign_XPS" / "logs").rglob("*.log"))
        leaked = [p.name for p in logs if str(root) in p.read_text(encoding="utf-8")]
        check("19 logs, none holding the repository path", len(logs) == 19 and not leaked, "%d logs, %s" % (len(logs), leaked))
        check("the failed cell's log replaced the path", any(r["outcome"] == "failed" and r["path_replacements"] == "1"
                                                            for r in ledger))
        pushed = subprocess.run(["git", "--git-dir", str(remote), "log", "--oneline", "campaign-xps"],
                                capture_output=True, text=True).stdout.strip().splitlines()
        check("every cell is a commit on the campaign branch of the remote", len(pushed) == 19 + 1, str(len(pushed)))
        dirty = subprocess.run(["git", "-C", str(root), "status", "--porcelain"], capture_output=True, text=True).stdout
        check("nothing is left uncommitted", dirty.strip() == "" or all(l.endswith("ENV_LOCK.json") or "campaign_XPS/" in l
                                                                    for l in dirty.splitlines()), dirty.strip())

        # Files of another machine and files of another batch never count as done.
        other = batch_cells(root, "sc3-s1")[0]
        o = dict(other["options"])
        line = "# options {machine=OLD, %s}\n" % ", ".join("%s=%s" % kv for kv in o.items())
        (root / "results-invariant" / "SC3_20261001-000000-000_OLD_abc_s2026.csv").write_text(
            "# run 20261001-000000-000_OLD_abc_s2026\n# scenario SC3\n" + line + "a\n", encoding="utf-8")
        _, done_old, _ = status(root, "XPS", "sc3-s1")
        check("a file of another machine does not mark a cell done", not done_old, str(len(done_old)))
        refine_line = line.replace("methods=" + o["methods"], "methods=" + sc3.REFINE)
        (root / "results-invariant" / "SC3_20261001-000000-001_XPS_abc_s2026.csv").write_text(
            "# run 20261001-000000-001_XPS_abc_s2026\n# scenario SC3\n" + refine_line + "a\n", encoding="utf-8")
        _, done_ref, _ = status(root, "XPS", "sc3-s1")
        check("a refinement file does not mark a method cell done", not done_ref, str(len(done_ref)))
        bad = subprocess.run(["git", "-C", str(root), "remote", "set-url", "origin", str(tmp / "missing.git")])
        before = len(calls)
        do_run(root, "XPS", "sc3-s1", branch="campaign-xps", runner=lambda r, m, c: (fake(r, m, c) if len(calls) - before < 2
                                                                                   else (calls.append("stop"), (9, ""))[1]),
               env=env)
        rows = read_ledger(root, "XPS")
        check("a failed push does not stop the batch", sum(1 for r in rows if r["batch"] == "sc3-s1") == 54 and bad.returncode == 0,
              str(sum(1 for r in rows if r["batch"] == "sc3-s1")))
        check("sanitize replaces the home folder", sanitize("x %s y" % Path.home(), root) == ("x <home> y", 1))

        # 3. A second generation: its own lock, carried from the first, and its own batches only.
        subprocess.run(["git", "-C", str(root), "remote", "set-url", "origin", str(remote)], check=True)
        (root / "src" / "B.java").write_text("class B {}\n", encoding="utf-8")
        new_code = dict(env, source_tree="changed")
        before = len(calls)
        check("a first-generation batch is refused under new code",
              do_run(root, "XPS", "sc1", runner=fake, env=new_code) == 1 and len(calls) == before)
        check("a second-generation batch needs the second lock",
              do_run(root, "XPS", "sc3-stop", runner=fake, env=new_code) == 1 and len(calls) == before)
        check("the second lock refuses another JDK",
              do_lock(root, "XPS", env=dict(new_code, java_runtime_version="25.0.5+8"), generation="2") == 1
              and not lock_path(root, "XPS", "2").is_file())
        check("the second lock refuses another host name",
              do_lock(root, "XPS", env=dict(new_code, hostname="OTHER"), generation="2") == 1)
        check("the third lock needs the second", do_lock(root, "XPS", env=new_code, generation="3") == 1)
        check("the second lock is written when only the code differs",
              do_lock(root, "XPS", env=new_code, generation="2") == 0 and lock_path(root, "XPS", "2").is_file())
        second = json.loads(lock_path(root, "XPS", "2").read_text(encoding="utf-8"))
        check("the second lock names the first", second.get("previous_source_tree") == env["source_tree"]
              and second.get("previous_lock", "").endswith("campaign_XPS/ENV_LOCK.json"), str(second))
        stop_cells = batch_cells(root, "sc3-stop")
        do_run(root, "XPS", "sc3-stop", branch="campaign-xps", runner=lambda r, m, c: (fake(r, m, c)
               if len(calls) - before < 3 else (calls.append("stop"), (9, ""))[1]), env=new_code)
        check("a second-generation batch writes its own ledger",
              len(read_ledger(root, "XPS", "2")) == len(stop_cells) and len(read_ledger(root, "XPS")) == len(rows),
              "%d and %d rows" % (len(read_ledger(root, "XPS", "2")), len(read_ledger(root, "XPS"))))
        check("its logs go to its own folder",
              len(list((root / "results-invariant" / "campaign_XPS_g2" / "logs").rglob("*.log"))) == len(stop_cells))
        _, done_stop, _ = status(root, "XPS", "sc3-stop")
        _, done_s1, _ = status(root, "XPS", "sc3-s1")
        check("a stop file marks its stop cell only", len(done_stop) == 3 and len(done_s1) == 2,
              "%d stop, %d methods" % (len(done_stop), len(done_s1)))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    print("%d checks, %d failed" % (checks, len(failures)))
    return 2 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
