"""Checks of the processor-model reader of tools/run.py.

Run it on every machine that produces results, the measuring machine above all: the Windows branch
reads the registry and can only be tested there.

    python tools/run_test.py

The first check reads the model on this machine and fails when it cannot, so a header that would say
"not reported" is caught before a long batch. The other checks force each failure path and require
the text to say "not reported" with a reason instead of guessing a value.
"""
import os
import platform
import sys
import types

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import run  # noqa: E402

failures = []
checks = 0


def check(name, ok, detail):
    global checks
    checks += 1
    print("%-62s %s  %s" % (name, "ok" if ok else "FAILED", detail))
    if not ok:
        failures.append(name)


here = run.cpu_model()
check("model read on this machine (%s)" % platform.system(),
      bool(here) and not here.startswith("not reported"), here)

unknown = run.cpu_model("Plan9")
check("unknown system says not reported", unknown.startswith("not reported: "), unknown)

real_run = run.subprocess.run
run.subprocess.run = lambda *a, **k: types.SimpleNamespace(returncode=1, stdout="")
try:
    darwin = run.cpu_model("Darwin")
finally:
    run.subprocess.run = real_run
check("macOS with a failing sysctl says not reported", darwin.startswith("not reported: "), darwin)

real_open = run.open if hasattr(run, "open") else None
run.open = lambda *a, **k: (_ for _ in ()).throw(OSError("no /proc"))
try:
    linux = run.cpu_model("Linux")
finally:
    if real_open is None:
        del run.open
    else:
        run.open = real_open
check("Linux without /proc/cpuinfo says not reported", linux.startswith("not reported: "), linux)

if platform.system() != "Windows":
    windows = run.cpu_model("Windows")
    check("Windows branch off Windows says not reported", windows.startswith("not reported: "), windows)

print("%d checks, %d failed" % (checks, len(failures)))
sys.exit(1 if failures else 0)
