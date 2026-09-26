"""Compare two javac -g:none builds class by class (scratch tool, not part of the repository).

1. sha256 of every .class file: identical, different, only-before, only-after.
2. For every class that differs: disassemble both with javap -c -p -constants, blank out
   constant-pool indices and the text of string constants, and diff. An empty diff means the two
   classes execute the same instructions and differ only in the text of their string constants.
3. Print the string constants (Utf8 entries of the constant pool) that exist on one side only.
"""
import hashlib, re, subprocess, sys
from pathlib import Path

JAVAP = "<jdk>/bin/javap"

def classes(root):
    root = Path(root)
    return {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted(root.rglob("*.class"))}

def javap(path, *flags):
    return subprocess.run([JAVAP, *flags, str(path)], capture_output=True, text=True, check=True).stdout

def normalized(path):
    out = []
    for line in javap(path, "-c", "-p", "-constants").splitlines():
        if line.startswith("Compiled from") or line.startswith("Classfile"):
            continue
        line = re.sub(r"#\d+", "#_", line)                      # constant-pool indices
        line = re.sub(r"(// String ).*", r"\1<text>", line)     # ldc of a string constant
        line = re.sub(r'(ldc(_w)? +#_ +// String ).*', r"\1<text>", line)
        out.append(line)
    return out

def utf8(path):
    return {m.group(1) for m in re.finditer(r"= Utf8 (.*)", javap(path, "-v", "-p"))}

def main(before, after):
    b, a = classes(before), classes(after)
    same = sorted(k for k in b if k in a and b[k] == a[k])
    diff = sorted(k for k in b if k in a and b[k] != a[k])
    gone = sorted(k for k in b if k not in a)
    new = sorted(k for k in a if k not in b)
    print(f"class files: before {len(b)}, after {len(a)}; identical {len(same)}, different {len(diff)}, "
          f"only before {len(gone)}, only after {len(new)}")
    for k in gone: print("  ONLY BEFORE", k)
    for k in new: print("  ONLY AFTER ", k)
    structural = 0
    for k in diff:
        nb, na = normalized(Path(before) / k), normalized(Path(after) / k)
        ub, ua = utf8(Path(before) / k), utf8(Path(after) / k)
        same_code = nb == na
        structural += not same_code
        print(f"  DIFFERENT {k}: instructions with string texts blanked are "
              f"{'IDENTICAL' if same_code else 'DIFFERENT'} ({len(nb)} lines); "
              f"string constants only before {len(ub - ua)}, only after {len(ua - ub)}")
        if not same_code:
            import difflib
            for line in list(difflib.unified_diff(nb, na, lineterm="", n=0))[:40]:
                print("      " + line)
        for s in sorted(ub - ua): print("      - " + s)
        for s in sorted(ua - ub): print("      + " + s)
    print(f"classes whose instructions differ beyond string texts: {structural}")
    return 1 if structural or gone or new else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2]))
