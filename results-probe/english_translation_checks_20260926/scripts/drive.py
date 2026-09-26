"""Scratch driver: run the old (thong_ke) or new (preregistered_analysis) analysis on given
arguments, with the locked-table paths redirected to a scratch directory. Prints the report and the
exit code. Usage: drive.py old|new <tools dir> <table dir> <arg> ..."""
import contextlib, io, os, sys
which, tools, tables = sys.argv[1], sys.argv[2], sys.argv[3]
args = sys.argv[4:]
sys.path.insert(0, tools)
if which == "old":
    import thong_ke as m
    m.CHOT.BANG_NHOM = os.path.join(tables, "pool.tsv")
    m.CHOT.BANG_NHOM_LAP_MUC = os.path.join(tables, "repeated.tsv")
    args = ["--bo-qua-xep-nhom" if a == "--skip-groups" else "--ra" if a == "--out" else a for a in args]
else:
    import preregistered_analysis as m
    m.LOCKED.GROUP_TABLE = os.path.join(tables, "pool.tsv")
    m.LOCKED.GROUP_TABLE_REPEATED = os.path.join(tables, "repeated.tsv")
out, err = io.StringIO(), io.StringIO()
with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
    try:
        code = m.main(args)
    except SystemExit as e:
        code = "SystemExit(%s)" % e.code
    except Exception as e:  # an exception is an outcome to compare, not a crash of the driver
        code = "EXCEPTION %s: %s" % (type(e).__name__, e)
sys.stdout.write(out.getvalue())
sys.stdout.write("@@STDERR@@\n" + err.getvalue())
sys.stdout.write("@@EXIT@@ %s\n" % code)
