"""Chay mot kich ban co gan phien ban ma vao dau dinh danh lan chay.

Vi sao can script nay. RunId doc bien moi truong HIDENOSP_REVISION va dong dau no vao ten tep
ket qua; khong dat thi moi tep mang dau "unversioned" va khong truy nguoc duoc ve commit nao
sinh ra no. Quy tac 2.6.8 doi dau dinh danh lan chay, va config/protocol.yaml chot rang bien nay
BAT BUOC cho moi lan chay lay so vao bai.

Script lam ba viec, theo dung thu tu:

1. Dich lai (mvn -o -q compile). Neu bo qua buoc nay thi phien ban duoc dong dau co the la ma
   NGUON hien tai trong khi cai dang chay lai la lop .class cu -- tuc dau dinh danh noi doi.
2. Kiem cay lam viec sach. Mot sha commit chi dinh danh dung ma da chay khi khong co sua doi
   chua commit. Cay ban thi tu choi, tru khi lan chay do la do tim (--probe) hoac nguoi dung
   khai ro --allow-dirty; ca hai truong hop deu dong dau hau to -dirty.
3. Dat HIDENOSP_REVISION roi goi hidenosp.exp.Runner voi moi doi so con lai.

Cach dung:

    python tools/run.py SC1 --trials=1500 --seed=2026
    python tools/run.py --jvm=-Xmx6g SC2 --dataset=datasets/dna_mt745584.txt --format=SPMF --sigma=2500 --gmin=0 --gmax=3 --maxPatternLength=4
    python tools/run.py --probe SC3 --sigma=2000 --maxPatternLength=4

Tuy chon cua RIENG script nay (khong chuyen tiep cho Runner):

    --jvm=<co>        co cho JVM, mac dinh -Xmx4g; lap lai duoc de dua nhieu co
    --allow-dirty     cho chay khi cay lam viec con sua doi chua commit
    --no-build        bo qua buoc dich lai (chi dung khi biet chac .class dang moi)
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CLASSES = os.path.join(ROOT, "target", "classes")
MAIN = "hidenosp.exp.Runner"


def git(*args):
    """Chay mot lenh git trong kho, tra ve chuoi da cat khoang trang, hoac None neu loi."""
    try:
        out = subprocess.run(["git"] + list(args), cwd=ROOT, capture_output=True, text=True)
    except FileNotFoundError:
        return None
    if out.returncode != 0:
        return None
    return out.stdout.strip()


def revision(allow_dirty, is_probe):
    """Tra ve chuoi phien ban de dong dau, hoac thoat neu cay ban ma khong duoc phep."""
    sha = git("rev-parse", "--short=12", "HEAD")
    if sha is None:
        print("LOI: khong doc duoc phien ban git. Dang chay ngoai mot kho git?", file=sys.stderr)
        print("     Neu that su muon chay khong co phien ban, dat HIDENOSP_REVISION bang tay.",
              file=sys.stderr)
        sys.exit(2)

    status = git("status", "--porcelain")
    if not status:
        return sha

    changed = [line for line in status.splitlines() if line.strip()]
    if not (allow_dirty or is_probe):
        print("LOI: cay lam viec con %d thay doi chua commit." % len(changed), file=sys.stderr)
        print("     Mot sha chi dinh danh dung ma da chay khi khong con sua doi nao treo.",
              file=sys.stderr)
        for line in changed[:10]:
            print("       " + line, file=sys.stderr)
        if len(changed) > 10:
            print("       ... va %d dong nua" % (len(changed) - 10), file=sys.stderr)
        print(file=sys.stderr)
        print("     Chon mot trong ba: commit cac thay doi; hoac them --probe neu day la lan",
              file=sys.stderr)
        print("     chay do tim; hoac them --allow-dirty neu that su co y.", file=sys.stderr)
        sys.exit(3)

    print("CANH BAO: cay lam viec con %d thay doi chua commit; dong dau la %s-dirty."
          % (len(changed), sha), flush=True)
    return sha + "-dirty"


def build():
    """Dich lai de lop .class chac chan khop voi ma nguon ma sha dang dong dau."""
    mvn = "mvn.cmd" if os.name == "nt" else "mvn"
    try:
        out = subprocess.run([mvn, "-o", "-q", "compile"], cwd=ROOT)
    except FileNotFoundError:
        print("LOI: khong tim thay maven. Them --no-build neu .class chac chan da moi.",
              file=sys.stderr)
        sys.exit(4)
    if out.returncode != 0:
        print("LOI: dich that bai, khong chay tiep.", file=sys.stderr)
        sys.exit(out.returncode)


def check_protocol(args):
    """Nhac cac rang buoc da chot o config/protocol.yaml ma script nay thay duoc."""
    scenario = next((a for a in args if not a.startswith("-")), "")
    if scenario.upper() in ("SC2", "SC3", "SC4", "SC5", "SC7"):
        lmax = next((a.split("=", 1)[1] for a in args if a.startswith("--maxPatternLength=")), None)
        if lmax != "4":
            print("CANH BAO: protocol.yaml chot l_max = 4, lan chay nay dung %s."
                  % (lmax if lmax else "mac dinh cua kich ban"), flush=True)
            print("          So sinh ra se khong so duoc voi cac bang khac cua bai.", flush=True)


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

    # Runner doi ten kich ban o vi tri dau. Script nay nhan tuy chon o bat ky dau nao, nen
    # phai tu dua ten kich ban len truoc, neu khong thi "run.py --probe SC1" se hong.
    scenario_at = next((i for i, a in enumerate(forwarded) if not a.startswith("-")), None)
    if scenario_at is not None and scenario_at > 0:
        forwarded.insert(0, forwarded.pop(scenario_at))

    is_probe = "--probe" in forwarded or "--probe=true" in forwarded

    if not no_build:
        build()
    rev = revision(allow_dirty, is_probe)
    check_protocol(forwarded)

    env = dict(os.environ)
    env["HIDENOSP_REVISION"] = rev
    print("HIDENOSP_REVISION = %s" % rev, flush=True)

    # Xo dem truoc khi giao fd cho java, neu khong thi cac dong tren hien sau ket qua chay.
    sys.stdout.flush()
    sys.stderr.flush()
    command = ["java"] + jvm + ["-cp", CLASSES, MAIN] + forwarded
    return subprocess.run(command, cwd=ROOT, env=env).returncode


if __name__ == "__main__":
    sys.exit(main())
