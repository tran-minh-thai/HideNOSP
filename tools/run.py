"""Run one scenario with the code revision stamped into the run identifier.

Why this wrapper exists. RunId reads the environment variable HIDENOSP_REVISION and stamps it into
the result file name; without it every file says "unversioned" and cannot be traced back to the
commit that produced it. config/protocol.yaml makes the variable MANDATORY for every run whose
numbers go into the paper.

The wrapper does five things, in this order:

1. Checks the Java version. The protocol fixes JDK 25 on every machine. Runs for the paper refuse
   any other major version; probe runs (--probe) only warn.
2. Rebuilds (mvn -o -q compile). Skipping this could stamp the CURRENT source revision onto a run
   of stale .class files, so the stamp would lie.
3. Checks that the working tree is clean. A commit sha identifies the code that ran only when no
   uncommitted change is pending. A dirty tree is refused unless the run is a probe (--probe) or
   the user says --allow-dirty; both cases get the suffix -dirty.
   The three output directories results, results-invariant and results-probe do NOT count: they
   hold the output of the runs themselves, so a batch of runs must not block itself.
4. On Windows, asks the system not to sleep until the run ends.
5. Reads the processor model (the JVM cannot) and passes it in HIDENOSP_CPU; the result header
   records it next to the logical processors and physical memory that the JVM reports itself.
6. Sets HIDENOSP_REVISION and calls hidenosp.exp.Runner with the remaining arguments.

Usage:

    python tools/run.py SC1 --trials=1500 --seed=2026
    python tools/run.py --jvm=-Xmx6g SC2 --dataset=datasets/dna_mt745584.txt --format=SPMF --sigma=2500 --gmin=0 --gmax=3 --maxPatternLength=4
    python tools/run.py --probe SC3 --sigma=2000 --maxPatternLength=4

Options of THIS script (not forwarded to Runner):

    --jvm=<flag>      JVM flag, default -Xmx4g; repeat to pass several flags
    --allow-dirty     run even though the working tree has uncommitted changes
    --no-build        skip the rebuild (only when the .class files are known to be current)
"""
import os
import platform
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CLASSES = os.path.join(ROOT, "target", "classes")
MAIN = "hidenosp.exp.Runner"

# config/protocol.yaml, row 5. The bytecode target in pom.xml is the same number.
REQUIRED_JAVA_MAJOR = 25


def git(*args):
    """Run a git command in the repository; return stripped stdout, or None on failure."""
    try:
        out = subprocess.run(["git"] + list(args), cwd=ROOT, capture_output=True, text=True)
    except FileNotFoundError:
        return None
    if out.returncode != 0:
        return None
    return out.stdout.strip()


# These three directories hold the OUTPUT of the runs, not their input. A freshly written result
# file does not change the code that ran, so it must not count as a dirty tree -- otherwise the
# second run of a batch would be blocked by the first.
# Everything else DOES count, including config/ and datasets/provenance.tsv, because they are input.
OUTPUT_DIRS = ("results/", "results-invariant/", "results-probe/")


def is_output(status_line):
    """True if a `git status --porcelain` line refers to a file inside an output directory."""
    path = status_line[3:].strip().strip('"')
    if " -> " in path:
        path = path.split(" -> ", 1)[1].strip().strip('"')
    return path.startswith(OUTPUT_DIRS)


def revision(allow_dirty, is_probe):
    """Return the revision string to stamp, or exit if the tree is dirty and that is not allowed."""
    sha = git("rev-parse", "--short=12", "HEAD")
    if sha is None:
        print("ERROR: cannot read the git revision. Running outside a git repository?",
              file=sys.stderr)
        print("       To run without a revision on purpose, set HIDENOSP_REVISION by hand.",
              file=sys.stderr)
        sys.exit(2)

    status = git("status", "--porcelain")
    changed = [line for line in (status or "").splitlines()
               if line.strip() and not is_output(line)]
    if not changed:
        return sha
    if not (allow_dirty or is_probe):
        print("ERROR: the working tree has %d uncommitted change(s)." % len(changed),
              file=sys.stderr)
        print("       A commit sha identifies the code that ran only when nothing is pending.",
              file=sys.stderr)
        for line in changed[:10]:
            print("         " + line, file=sys.stderr)
        if len(changed) > 10:
            print("         ... and %d more" % (len(changed) - 10), file=sys.stderr)
        print(file=sys.stderr)
        print("       Either commit the changes; or add --probe for a probe run; or add",
              file=sys.stderr)
        print("       --allow-dirty if you really mean it.", file=sys.stderr)
        sys.exit(3)

    print("WARNING: the working tree has %d uncommitted change(s); the stamp is %s-dirty."
          % (len(changed), sha), flush=True)
    return sha + "-dirty"


def java_executable():
    """The java launcher of JAVA_HOME when it is set, otherwise `java` on PATH.

    Maven compiles with the JDK of JAVA_HOME, so taking the launcher from the same place keeps the
    compile and the run on one JDK even when an older Java is still first on PATH.
    """
    home = os.environ.get("JAVA_HOME")
    if not home:
        return "java"
    exe = os.path.join(home, "bin", "java.exe" if os.name == "nt" else "java")
    if not os.path.isfile(exe):
        print("ERROR: JAVA_HOME is set to %s, but %s does not exist." % (home, exe),
              file=sys.stderr)
        sys.exit(5)
    return exe


def java_version(java):
    """Return (major version, first line of `java -version`) for the given launcher."""
    try:
        out = subprocess.run([java, "-version"], capture_output=True, text=True)
    except FileNotFoundError:
        print("ERROR: no Java launcher found (tried %s). Install JDK %d."
              % (java, REQUIRED_JAVA_MAJOR), file=sys.stderr)
        sys.exit(5)
    text = (out.stderr or "") + (out.stdout or "")
    first = text.strip().splitlines()[0] if text.strip() else "(no output)"
    match = re.search(r'version "(\d+)(?:\.(\d+))?', text)
    if not match:
        return None, first
    major = int(match.group(1))
    if major == 1 and match.group(2):  # the old "1.8" style
        major = int(match.group(2))
    return major, first


def check_java(java, is_probe):
    """Refuse a run for the paper on any Java major version other than the one fixed by the protocol."""
    major, first = java_version(java)
    if major == REQUIRED_JAVA_MAJOR:
        print("java: %s  [%s]" % (first, java), flush=True)
        return
    message = ("Java major version %s at %s (\"%s\"), but config/protocol.yaml fixes JDK %d."
               % (major if major is not None else "unknown", java, first, REQUIRED_JAVA_MAJOR))
    if is_probe:
        print("WARNING: " + message + " Allowed because this is a probe run.", flush=True)
        return
    print("ERROR: " + message, file=sys.stderr)
    print("       Install Temurin %d and point JAVA_HOME at it, or add --probe for a probe run."
          % REQUIRED_JAVA_MAJOR, file=sys.stderr)
    sys.exit(5)


def keep_awake():
    """On Windows, ask the system not to sleep while this process waits for the run.

    The request (ES_CONTINUOUS | ES_SYSTEM_REQUIRED) belongs to the calling thread and ends when the
    thread does, i.e. when the run finishes or is interrupted. It does not stop Windows Update from
    restarting the machine; that has to be paused by hand before a long batch.
    """
    if os.name != "nt":
        return
    import ctypes
    es_continuous = 0x80000000
    es_system_required = 0x00000001
    previous = ctypes.windll.kernel32.SetThreadExecutionState(es_continuous | es_system_required)
    if previous == 0:
        print("WARNING: SetThreadExecutionState failed; the machine may sleep during the run.",
              flush=True)
    else:
        print("keep-awake: on until this run ends", flush=True)


def cpu_model(system=None):
    """Processor model of this machine, for the header of every result file.

    Standard library only, so it runs on the measuring machine as it is. When the model cannot be
    read the text says so and why; it is never guessed.
    """
    system = system or platform.system()
    try:
        if system == "Windows":
            import winreg
            key = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE,
                                 r"HARDWARE\DESCRIPTION\System\CentralProcessor\0")
            value, _ = winreg.QueryValueEx(key, "ProcessorNameString")
            return " ".join(str(value).split())
        if system == "Darwin":
            out = subprocess.run(["sysctl", "-n", "machdep.cpu.brand_string"],
                                 capture_output=True, text=True)
            if out.returncode == 0 and out.stdout.strip():
                return " ".join(out.stdout.split())
        if system == "Linux":
            with open("/proc/cpuinfo", encoding="utf-8") as f:
                for line in f:
                    if line.lower().startswith("model name"):
                        return " ".join(line.split(":", 1)[1].split())
    except (OSError, ImportError) as e:
        return "not reported: reading the model on %s failed (%s)" % (system, e)
    return "not reported: no processor model found on %s" % system


def build():
    """Rebuild, so the .class files certainly match the source whose sha is being stamped."""
    mvn = "mvn.cmd" if os.name == "nt" else "mvn"
    try:
        out = subprocess.run([mvn, "-o", "-q", "compile"], cwd=ROOT)
    except FileNotFoundError:
        print("ERROR: Maven not found. Add --no-build if the .class files are certainly current.",
              file=sys.stderr)
        sys.exit(4)
    if out.returncode != 0:
        print("ERROR: the build failed; not running.", file=sys.stderr)
        sys.exit(out.returncode)


def check_protocol(args):
    """Warn about the constraints fixed in config/protocol.yaml that this script can see."""
    scenario = next((a for a in args if not a.startswith("-")), "")
    if scenario.upper() in ("SC2", "SC3", "SC4", "SC5", "SC7"):
        lmax = next((a.split("=", 1)[1] for a in args if a.startswith("--maxPatternLength=")), None)
        if lmax != "4":
            print("WARNING: protocol.yaml fixes l_max = 4, this run uses %s."
                  % (lmax if lmax else "the scenario default"), flush=True)
            print("         Its numbers will not be comparable with the other tables.", flush=True)


def main():
    args = sys.argv[1:]
    if not args or args[0] in ("-h", "--help"):
        print(__doc__)
        return 0

    jvm = []
    forwarded = []
    allow_dirty = False
    no_build = False
    for a in args:
        if a.startswith("--jvm="):
            jvm.append(a.split("=", 1)[1])
        elif a == "--allow-dirty":
            allow_dirty = True
        elif a == "--no-build":
            no_build = True
        else:
            forwarded.append(a)

    if not jvm:
        jvm = ["-Xmx4g"]

    # Runner expects the scenario name first. This script accepts options anywhere, so it moves
    # the scenario name to the front; otherwise "run.py --probe SC1" would break.
    scenario_at = next((i for i, a in enumerate(forwarded) if not a.startswith("-")), None)
    if scenario_at is not None and scenario_at > 0:
        forwarded.insert(0, forwarded.pop(scenario_at))

    is_probe = "--probe" in forwarded or "--probe=true" in forwarded

    java = java_executable()
    check_java(java, is_probe)
    if not no_build:
        build()
    rev = revision(allow_dirty, is_probe)
    check_protocol(forwarded)
    keep_awake()

    env = dict(os.environ)
    env["HIDENOSP_REVISION"] = rev
    env["HIDENOSP_CPU"] = cpu_model()
    print("HIDENOSP_REVISION = %s" % rev, flush=True)
    print("HIDENOSP_CPU = %s" % env["HIDENOSP_CPU"], flush=True)

    # Flush before handing the file descriptors to java, otherwise the lines above appear after
    # the output of the run.
    sys.stdout.flush()
    sys.stderr.flush()
    command = [java] + jvm + ["-cp", CLASSES, MAIN] + forwarded
    return subprocess.run(command, cwd=ROOT, env=env).returncode


if __name__ == "__main__":
    sys.exit(main())
