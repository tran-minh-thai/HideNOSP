"""Phan tich thong ke cho H2 va H3, dung dung nhung gi config/preregistration.md da chot.

Script nay KHONG chon phep kiem, khong chon nguong, khong chon cach ghep cap. Moi thu do da
chot tu truoc khi chay SC3; o day chi doc lai va thi hanh. Hang so lay tu preregistration nam
gon trong lop CHOT ben duoi, moi hang ghi ro no den tu muc nao, de doi chieu bang mat duoc.

Lam nhung viec sau, theo thu tu:

1. Doc tep SC3, gop cac lan chay lai cua phuong phap ngau nhien bang TRUNG VI (muc 5).
2. Group the configurations from the SC2 files of the pool profile (preregistration section 10:
   medians over the distinct-item candidates) with the locked thresholds, and compare every
   configuration with the locked table config/nhom_cau_hinh.tsv. A difference stops the analysis.
   H2 is tested on the main group only; the other groups are reported descriptively.
3. Loai tru thuc the theo muc 5, dem theo tung ly do. Ti le loai vuot 10% thi KHONG ket luan H2.
4. Ghep cap theo muc 5: chi thuc the ma hai phuong phap dung CUNG |R| moi vao Wilcoxon.
5. Wilcoxon dau hang mot phia kieu Pratt tren Lost_rel, co hieu chinh Holm tren ho baseline
   tang (b), kem co hieu ung rank-biserial.
6. H3: ti so r = |R|/|R|* tren nhom chinh va nhom suy bien.
7. Tuong duong (muc 4): uoc luong Hodges-Lehmann kem khoang tin cay, doi chieu voi bien da chot.

Section 11 (2026-09-26): every SC3 row is put in one of two scopes by the items of its sensitive
patterns, distinct (the main scope, H2) or repeated (H2-R). Each scope is grouped from its own SC2
profile (pool, repeated) and checked against its own locked table; H2-R repeats the steps above on
the main group of the repeated scope and is never pooled with H2. Datasets in CHOT.CHI_BAO_CAO
(protein_sdb1) are reported but never enter a Wilcoxon test.

Cach dung:

    python tools/thong_ke.py
    python tools/thong_ke.py --sc3 results-invariant --sc2 results-invariant
    python tools/thong_ke.py --sc3 results-probe --bo-qua-xep-nhom
    python tools/thong_ke.py --sc2 results-invariant --write-groups   (lock both tables, once)

Ma thoat 1 khi co dieu khien ket qua khong dung duoc (ti le loai qua cao, xep nhom lech,
thieu cot). Ma thoat 0 khong co nghia la H2 duoc ung ho; no chi co nghia la phan tich chay
duoc va ket luan nam trong bang in ra.
"""
import argparse
import csv
import io
import os
import sys
from collections import defaultdict

import numpy as np
from scipy import stats


class CHOT:
    """Hang so da chot o config/preregistration.md. Sua o day la sua ban chot, khong duoc."""

    DELTA_L = 0.02              # muc 2, nguong hieu ung thuc tien cua H2 tren Lost_rel
    ALPHA = 0.05               # muc 2
    # muc 2, ho Holm bon phan tu. Day la method_key, tuc ten goi tren dong lenh cua SC3, chu
    # khong phai ten phuong phap tu dat: SBSH-rmax tu goi minh la "SBSH-r3" khi l_max = 3.
    HO_BASELINE = ("hha", "dbsh", "sbsh-r1", "sbsh-rmax")
    # preregistration section 12: the method under test of H2 and H2-R; config/protocol.yaml
    # (phuong_phap_H2R) must name the same key, and thong_ke_test.py checks that it does.
    PHUONG_PHAP_CUA_TA = "cut-hide-rw-ls"                    # ve duoc cua hieu so

    RHO_CHINH = 0.20           # muc 3
    RHO_SUY_BIEN = 0.05        # muc 3
    BETA_CO_HIEU_LUC = 0.20    # muc 3
    BETA_SUY_BIEN = 0.05       # muc 3

    R_VUOT_1 = 1.10            # muc 2, H3
    R_GAN_1 = 1.05             # muc 2, H3

    BIEN_TUONG_DUONG = {"lost_rel": 0.02, "sup_dist": 0.01}  # muc 4

    TI_LE_LOAI_TOI_DA = 0.10   # muc 5
    HAT_GIONG = tuple(range(2026, 2036))                     # muc 5, muoi hat giong

    # Section 10: the group of every configuration, locked in this file once the official SC2
    # runs of the pool profile exist. The script recomputes the groups and compares them.
    BANG_NHOM = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                             "config", "nhom_cau_hinh.tsv")
    # Section 11: the repeated-item scope has its own locked table, from SC2 --profile=repeated.
    BANG_NHOM_LAP_MUC = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                                     "config", "nhom_cau_hinh_lap_muc.tsv")
    # Section 11, point 5: reported only, never in a Wilcoxon test (shares 64-75% of its
    # sequences with protein_sdb4, which is tested).
    CHI_BAO_CAO = ("protein_sdb1",)


# Cot ma phan tich nay khong the thieu. Ban SC3 cu khong co lost_rel va nonsensitive_before.
COT_BAT_BUOC = ("lost_rel", "nonsensitive_before", "mask_count", "hf", "method_key",
                "sensitive_set")


# ---------------------------------------------------------------- doc tep

def doc_csv(duong_dan):
    """Tra ve (danh sach dong chu thich, danh sach dict). Bo qua dong bat dau bang #."""
    chu_thich, hang = [], []
    with io.open(duong_dan, encoding="utf-8-sig", newline="") as f:
        dong = []
        for line in f:
            if line.startswith("#"):
                chu_thich.append(line.rstrip("\n"))
                continue
            dong.append(line)
        for r in csv.DictReader(dong):
            hang.append(r)
    return chu_thich, hang


def gom_tep(thu_muc, tien_to):
    if os.path.isfile(thu_muc):
        return [thu_muc]
    ra = []
    for ten in sorted(os.listdir(thu_muc)):
        if ten.startswith(tien_to) and ten.endswith(".csv"):
            ra.append(os.path.join(thu_muc, ten))
    return ra


def ten_bo(nhan):
    """Chuan hoa nhan bo du lieu: SC2 ghi duong dan, SC3 ghi duong dan hoac 'synthetic'."""
    nhan = nhan.replace("\\", "/").strip()
    ten = os.path.basename(nhan)
    return os.path.splitext(ten)[0] if "." in ten else ten


def so(x, mac_dinh=float("nan")):
    try:
        return float(x)
    except (TypeError, ValueError):
        return mac_dinh


# ---------------------------------------------------------------- xep nhom

def pham_vi_cua_tap(chuoi):
    """The scope of a sensitive set as SC3 prints it, "[[4, 1, 4, 1]]": "distinct" when every
    pattern has distinct items, "repeated" when every pattern repeats one (section 11, point 4).
    A set mixing both kinds, or an unreadable one, is an error: the sampling never mixes them."""
    import re
    mau = re.findall(r"\[([^\[\]]*)\]", chuoi)
    if not mau:
        raise ValueError("sensitive set %r has no pattern" % chuoi)
    loai = set()
    for m in mau:
        muc = [x.strip() for x in m.split(",") if x.strip()]
        if not muc:
            raise ValueError("sensitive set %r has an empty pattern" % chuoi)
        loai.add("distinct" if len(set(muc)) == len(muc) else "repeated")
    if len(loai) != 1:
        raise ValueError("sensitive set %r mixes distinct-item and repeated-item patterns" % chuoi)
    return loai.pop()


def muc_cua_mau(chuoi):
    """The items of a pattern as SC2 prints it, "[3, 5, 3]" -> ["3", "5", "3"]."""
    return [x.strip() for x in chuoi.strip().strip("[]").split(",") if x.strip()]


def xep_nhom(tep_sc2, profile="pool"):
    """Group of every configuration (dataset, gap, sigma) under preregistration section 10.

    Only SC2 files written with --profile=pool count: their rows are every distinct-item candidate
    of the configuration, the patterns the main comparison hides. Files of the sampled profile are
    ignored, and a pool file holding a repeated-item pattern is an error. With profile="repeated"
    the same is done on the files of --profile=repeated, whose rows must all repeat an item. The
    check reads the items of the pattern itself: min_masks_exact no longer tells the two kinds apart
    once theorem P8 makes repeated-item bounds exact.
    """
    if profile not in ("pool", "repeated"):
        raise ValueError("profile is pool or repeated, got %r" % profile)
    theo_mau = defaultdict(dict)   # configuration -> pattern -> (rho_rep, beta)
    for duong_dan in tep_sc2:
        chu_thich, hang = doc_csv(duong_dan)
        if "# profile %s" % profile not in chu_thich:
            continue
        for r in hang:
            muc = muc_cua_mau(r.get("pattern", ""))
            khong_lap = len(set(muc)) == len(muc)
            if not muc or khong_lap != (profile == "pool"):
                raise ValueError("%s: a %s profile holds %s patterns only, found %s"
                                 % (duong_dan, profile,
                                    "distinct-item" if profile == "pool" else "repeated-item",
                                    r.get("pattern")))
            khoa = (ten_bo(r["dataset"]), r["gap"], int(r["sigma"]))
            gia_tri = (so(r["rho_rep"]), so(r["beta"]))
            cu = theo_mau[khoa].get(r["pattern"])
            if cu is not None and cu != gia_tri:
                raise ValueError("%s: pattern %s of %s was profiled twice with different values"
                                 % (duong_dan, r["pattern"], khoa))
            # A second pool file of the same configuration (a rerun) must not count twice.
            theo_mau[khoa][r["pattern"]] = gia_tri
    rho = {k: [v[0] for v in m.values()] for k, m in theo_mau.items()}
    beta = {k: [v[1] for v in m.values()] for k, m in theo_mau.items()}
    ra = {}
    for khoa, gia_tri in rho.items():
        m = float(np.median(gia_tri))
        b = float(np.median(beta[khoa]))
        if m >= CHOT.RHO_CHINH:
            nhom = "chinh"
        elif m <= CHOT.RHO_SUY_BIEN:
            nhom = "suy bien"
        else:
            nhom = "trung gian"
        if b >= CHOT.BETA_CO_HIEU_LUC:
            nhom_beta = "co hieu luc"
        elif b <= CHOT.BETA_SUY_BIEN:
            nhom_beta = "suy bien"
        else:
            nhom_beta = "trung gian"
        ra[khoa] = {"nhom": nhom, "rho_rep": m, "beta": b, "nhom_beta": nhom_beta,
                    "pool": len(gia_tri)}
    return ra


BANG_COT = ("dataset", "gap", "sigma", "pool", "rho_rep_median", "beta_median", "group",
            "beta_group")


def ghi_bang_nhom(nhom, duong_dan):
    """Write the group table: done once, from the official SC2 runs, then committed (locked)."""
    with io.open(duong_dan, "w", encoding="utf-8", newline="\n") as f:
        profile = "repeated" if duong_dan == CHOT.BANG_NHOM_LAP_MUC else "pool"
        f.write("# Groups of preregistration section %s, from SC2 --profile=%s. Locked by commit.\n"
                % ("11" if profile == "repeated" else "10", profile))
        f.write("\t".join(BANG_COT) + "\n")
        for khoa in sorted(nhom):
            v = nhom[khoa]
            f.write("\t".join([khoa[0], khoa[1], str(khoa[2]), str(v["pool"]),
                               "%.6f" % v["rho_rep"], "%.6f" % v["beta"], v["nhom"],
                               v["nhom_beta"]]) + "\n")


def doc_bang_nhom(duong_dan):
    """The locked table as {(dataset, gap, sigma): row}, or None when it does not exist yet."""
    if not os.path.exists(duong_dan):
        return None
    ra = {}
    with io.open(duong_dan, encoding="utf-8") as f:
        for line in f:
            if line.startswith("#") or line.startswith("dataset\t") or not line.strip():
                continue
            o = line.rstrip("\n").split("\t")
            ra[(o[0], o[1], int(o[2]))] = dict(zip(BANG_COT, o))
    return ra


def kiem_xep_nhom(nhom, ghi, bang=None, profile="pool"):
    """Compare every recomputed group with the locked table. Any difference stops the analysis."""
    duong_bang = CHOT.BANG_NHOM if profile == "pool" else CHOT.BANG_NHOM_LAP_MUC
    bang = doc_bang_nhom(duong_bang) if bang is None else bang
    ghi("")
    ghi("Groups of the configurations (section %s: medians over the %s candidates;"
        " rho_rep >= %.2f main, <= %.2f degenerate)"
        % ("10" if profile == "pool" else "11",
           "distinct-item" if profile == "pool" else "repeated-item",
           CHOT.RHO_CHINH, CHOT.RHO_SUY_BIEN))
    if not nhom:
        ghi("  NO SC2 file of the %s profile: nothing to group. Run SC2 with --profile=%s."
            % (profile, profile))
        return False
    for khoa in sorted(nhom):
        v = nhom[khoa]
        ghi("  %-16s %-7s sigma %-6d pool %4d  rho_rep %.3f  beta %.3f  -> %s / beta %s"
            % (khoa[0], khoa[1], khoa[2], v["pool"], v["rho_rep"], v["beta"], v["nhom"],
               v["nhom_beta"]))
    if bang is None:
        ghi("")
        ghi("  The locked table %s does not exist yet. Write it once from the official SC2 runs"
            % duong_bang)
        ghi("  (--write-groups) and commit it before any SC3 result is read.")
        return False
    lech = []
    for khoa in sorted(set(nhom) | set(bang)):
        if khoa not in nhom:
            lech.append("%s is locked but has no SC2 pool file here" % (khoa,))
        elif khoa not in bang:
            lech.append("%s has an SC2 pool file but is not in the locked table" % (khoa,))
        elif (nhom[khoa]["nhom"] != bang[khoa]["group"]
              or nhom[khoa]["nhom_beta"] != bang[khoa]["beta_group"]):
            lech.append("%s: recomputed %s / %s, locked %s / %s"
                        % (khoa, nhom[khoa]["nhom"], nhom[khoa]["nhom_beta"], bang[khoa]["group"],
                           bang[khoa]["beta_group"]))
    if lech:
        ghi("")
        ghi("  DIFFERENT from the locked table:")
        for d in lech:
            ghi("    " + d)
        return False
    ghi("  %d configurations, all as locked." % len(nhom))
    return True


# ---------------------------------------------------------------- gop lan chay lai

def gop_lap_lai(hang, ghi):
    """Gop cac lan chay lai cua phuong phap ngau nhien bang TRUNG VI (muc 5).

    Don vi gop la (bo, shape, gap, sigma, tap nhay cam, phuong phap). Phuong phap tat dinh chi
    co mot hang nen trung vi cua mot so la chinh no.
    """
    nhom = defaultdict(list)
    for r in hang:
        khoa = (ten_bo(r["dataset"]), r.get("shape", ""), r["gap"], int(r["sigma"]),
                r["sensitive_set"], r["method_key"])
        nhom[khoa].append(r)

    cot_so = ("hf", "lost", "lost_rel", "ghost", "dist_r", "sup_dist", "mask_count",
              "min_masks", "mask_ratio", "lower_bound", "achieved_ratio",
              "frequent_before", "nonsensitive_before", "frequent_after")
    ra = []
    thieu_hat = []
    for khoa, nhieu in sorted(nhom.items()):
        goc = nhieu[0]
        hat = sorted({r.get("method_seed", "") for r in nhieu} - {""})
        if hat and len(hat) != len(CHOT.HAT_GIONG):
            thieu_hat.append((khoa[5], len(hat)))
        moi = {
            "dataset": khoa[0], "shape": khoa[1], "gap": khoa[2], "sigma": khoa[3],
            "sensitive_set": khoa[4], "method_key": khoa[5],
            "method": goc.get("method", khoa[5]),
            "so_lan": len(nhieu), "so_hat": len(hat),
            "status": goc.get("status", ""),
        }
        for c in cot_so:
            gia_tri = [so(r.get(c, "")) for r in nhieu]
            gia_tri = [v for v in gia_tri if not np.isnan(v)]
            moi[c] = float(np.median(gia_tri)) if gia_tri else float("nan")
        ra.append(moi)

    if thieu_hat:
        ghi("")
        ghi("  CANH BAO: phuong phap ngau nhien phai chay %d hat giong (%d-%d) theo muc 5, nhung:"
            % (len(CHOT.HAT_GIONG), CHOT.HAT_GIONG[0], CHOT.HAT_GIONG[-1]))
        for ten, n in sorted(set(thieu_hat)):
            ghi("    %-26s chi co %d hat" % (ten, n))
        ghi("  Trung vi tren it hat hon la mot dai luong khac, khong dung cho bang vao bai.")
    return ra


# ---------------------------------------------------------------- Wilcoxon kieu Pratt

def hang_pratt(d):
    """Hang cua |d| theo kieu Pratt: hieu bang 0 VAN duoc xep hang, chi khong vao W+ hay W-.

    Wilcoxon goc vut cac hieu bang 0 di. Lost la so nguyen nho nen so cap hieu bang 0 rat nhieu;
    vut chung di lam mat thong tin "hai phuong phap ngang nhau" va thoi phong co hieu ung. muc 5
    chot Pratt vi ly do do.
    """
    d = np.asarray(d, dtype=float)
    r = stats.rankdata(np.abs(d))
    w_duong = float(r[d > 0].sum())
    w_am = float(r[d < 0].sum())
    return w_duong, w_am


def rank_biserial(d):
    """(W+ - W-)/(W+ + W-), dung cong thuc muc 2 chu khong phai bien the 2W+/(n(n+1)) - 1."""
    w_duong, w_am = hang_pratt(d)
    tong = w_duong + w_am
    return float("nan") if tong == 0 else (w_duong - w_am) / tong


def wilcoxon_mot_phia(d, dich=0.0):
    """Wilcoxon dau hang mot phia kieu Pratt, gia thuyet doi: trung vi cua d lon hon dich.

    Tra ve (thong ke, p, co hieu ung, n, so hieu bang 0 sau khi tru dich).
    """
    x = np.asarray(d, dtype=float) - dich
    n = len(x)
    if n == 0:
        return float("nan"), float("nan"), float("nan"), 0, 0
    so_khong = int((x == 0).sum())
    if so_khong == n:
        # Moi hieu bang dung dich: khong co bang chung theo huong nao ca.
        return 0.0, 1.0, 0.0, n, so_khong
    # method PHAI la "auto". Trong scipy 1.18, method="exact" duoi Pratt tra ve gia tri SAI:
    # no dung phan bo dau hang chuan tren ca n quan sat, khong ke rang hang cua cac hieu bang 0
    # khong duoc phep nhan dau. Tren mot vi du 12 quan sat co 4 so 0, liet ke tay cho
    # p = 29/256 = 0,113281; "auto" cho dung so do, "exact" cho 0,211914 va "approx" cho
    # 0,099370. thong_ke_test.thu_wilcoxon_dung_pratt_chu_khong_phai_wilcox ghim cho no.
    ket_qua = stats.wilcoxon(x, zero_method="pratt", alternative="greater", method="auto")
    return float(ket_qua.statistic), float(ket_qua.pvalue), rank_biserial(x), n, so_khong


def holm(p_theo_ten):
    """Holm-Bonferroni. Tra ve dict ten -> p da hieu chinh, giu tinh don dieu."""
    muc = sorted(p_theo_ten.items(), key=lambda kv: kv[1])
    m = len(muc)
    ra = {}
    truoc = 0.0
    for i, (ten, p) in enumerate(muc):
        dieu_chinh = min(1.0, (m - i) * p)
        truoc = max(truoc, dieu_chinh)
        ra[ten] = truoc
    return ra


def hodges_lehmann(d, alpha=0.05):
    """Uoc luong HL cua trung vi hieu, kem khoang tin cay dua tren Wilcoxon (muc 4).

    Uoc luong la trung vi cua cac trung binh Walsh (d_i + d_j)/2, i <= j. Bien khoang lay theo
    xap xi chuan cua thong ke dau hang; voi n nho thi khoang rong ra chu khong sai huong.
    """
    x = np.asarray(d, dtype=float)
    n = len(x)
    if n < 2:
        return float("nan"), float("nan"), float("nan")
    walsh = np.array([(x[i] + x[j]) / 2 for i in range(n) for j in range(i, n)])
    walsh.sort()
    uoc_luong = float(np.median(walsh))
    z = stats.norm.ppf(1 - alpha / 2)
    trung_binh = n * (n + 1) / 4
    do_lech = np.sqrt(n * (n + 1) * (2 * n + 1) / 24)
    k = int(np.floor(trung_binh - z * do_lech))
    if k < 0:
        k = 0
    if k >= len(walsh):
        return uoc_luong, float("nan"), float("nan")
    return uoc_luong, float(walsh[k]), float(walsh[len(walsh) - 1 - k])


# ---------------------------------------------------------------- ghep cap va loai tru

def ghep_cap(hang, ten_baseline, dai_luong):
    """Ghep CUT-HIDE voi mot baseline tren tung thuc the, theo dung quy tac muc 5.

    Tra ve bon thung:
      "kiem"       cung |R|, dua vao Wilcoxon
      "baseline hon"  baseline dung NHIEU lan che hon, bao cao rieng, khong kiem dinh
      "ta hon"     CUT-HIDE dung nhieu hon -- muc 5 KHONG noi toi ca nay, xem ghi chu ben duoi
      "loai"       bi loai, kem ly do
    """
    theo_thuc_the = defaultdict(dict)
    for r in hang:
        khoa = (r["dataset"], r["gap"], r["sigma"], r["sensitive_set"])
        theo_thuc_the[khoa][r["method_key"]] = r

    thung = {"kiem": [], "baseline hon": [], "ta hon": [], "loai": []}
    for khoa, theo_pp in sorted(theo_thuc_the.items()):
        ta = theo_pp.get(CHOT.PHUONG_PHAP_CUA_TA)
        doi = theo_pp.get(ten_baseline)
        if ta is None or doi is None:
            continue
        ly_do = ly_do_loai(ta, doi)
        if ly_do:
            thung["loai"].append((khoa, ly_do))
            continue
        hieu = doi[dai_luong] - ta[dai_luong]
        muc = (khoa, hieu, ta, doi)
        if ta["mask_count"] == doi["mask_count"]:
            thung["kiem"].append(muc)
        elif doi["mask_count"] > ta["mask_count"]:
            thung["baseline hon"].append(muc)
        else:
            thung["ta hon"].append(muc)
    return thung


def ly_do_loai(ta, doi):
    for nhan, r in (("chung ta", ta), ("baseline", doi)):
        if r["status"]:
            return "%s co ma %s" % (nhan, r["status"])
        if not np.isnan(r["hf"]) and r["hf"] > 0:
            return "%s con HF = %.3f > 0" % (nhan, r["hf"])
    return None


# ---------------------------------------------------------------- H2

def chay_h2(hang, ghi, nhom=None, ten_gia_thuyet="H2"):
    ghi("")
    ghi("=" * 78)
    ghi("%s  trung vi cua Lost_rel(baseline) - Lost_rel(CUT-HIDE) lon hon delta_L = %.2f"
        % (ten_gia_thuyet, CHOT.DELTA_L))
    ghi("=" * 78)

    bao_cao = [r for r in hang if r["dataset"] in CHOT.CHI_BAO_CAO]
    if bao_cao:
        ghi("")
        ghi("  %d rows of %s are reported only and do not enter the test (section 11, point 5)."
            % (len(bao_cao), ", ".join(sorted({r["dataset"] for r in bao_cao}))))
        hang = [r for r in hang if r["dataset"] not in CHOT.CHI_BAO_CAO]

    if nhom is not None:
        # Preregistration section 3 keeps the intermediate group out of H2, and the design keeps
        # the degenerate group out of the evidence for the contribution: H2 is the main group.
        theo_nhom = defaultdict(int)
        giu = []
        for r in hang:
            thong_tin = nhom.get((r["dataset"], r["gap"], r["sigma"]))
            ten_nhom = thong_tin["nhom"] if thong_tin else "khong xep"
            theo_nhom[ten_nhom] += 1
            if ten_nhom == "chinh":
                giu.append(r)
        ghi("")
        ghi("  rows by group: " + ", ".join("%s %d" % (k, v) for k, v in sorted(theo_nhom.items())))
        ghi("  H2 is tested on the %d rows of the main group only." % len(giu))
        hang = giu
    else:
        ghi("")
        ghi("  PROBE: no group filter (--bo-qua-xep-nhom); every row enters H2.")

    if not co_mat(hang, CHOT.PHUONG_PHAP_CUA_TA):
        co = sorted({r["method_key"] for r in hang})
        ghi("")
        ghi("  KHONG CO '%s' trong tep. H2 la mot so sanh co ve, khong co ve do thi khong"
            % CHOT.PHUONG_PHAP_CUA_TA)
        ghi("  kiem duoc gi. Trong tep dang co: %s" % ", ".join(co))
        ghi("  Luu y 'cut-hide-uniform', 'cut-hide' va 'cut-hide-rw' la bien the DOI CHUNG, khong")
        ghi("  thay duoc cho '%s' o H2 (ban chot truoc, muc 12)." % CHOT.PHUONG_PHAP_CUA_TA)
        return True

    ho = [b for b in CHOT.HO_BASELINE if co_mat(hang, b)]
    vang = [b for b in CHOT.HO_BASELINE if b not in ho]
    if vang:
        ghi("")
        ghi("  Vang mat khoi tep: %s" % ", ".join(vang))
        ghi("  Ho Holm con %d phan tu. muc 2 doi dieu nay phai duoc sua trong preregistration"
            % len(ho))
        ghi("  TRUOC khi chay, khong phai sau khi thay tep thieu.")
    if not ho:
        ghi("")
        ghi("  Khong co baseline tang (b) nao trong tep. Khong kiem duoc H2.")
        return True

    ket_qua = {}
    tong_cap = 0
    tong_loai = 0
    for ten in ho:
        thung = ghep_cap(hang, ten, "lost_rel")
        n_dung = len(thung["kiem"]) + len(thung["baseline hon"]) + len(thung["ta hon"])
        tong_cap += n_dung + len(thung["loai"])
        tong_loai += len(thung["loai"])

        ghi("")
        ghi("-- %s" % ten)
        ghi("   dua vao Wilcoxon (cung |R|)          %4d thuc the" % len(thung["kiem"]))
        ghi("   baseline dung nhieu lan che hon      %4d thuc the  (bao cao rieng, khong kiem)"
            % len(thung["baseline hon"]))
        ghi("   CUT-HIDE dung nhieu lan che hon      %4d thuc the  (muc 5 khong noi toi ca nay)"
            % len(thung["ta hon"]))
        ghi("   bi loai                              %4d thuc the" % len(thung["loai"]))
        for khoa, ly_do in thung["loai"][:5]:
            ghi("       %s  %s" % (khoa[3][:40], ly_do))
        if len(thung["loai"]) > 5:
            ghi("       ... va %d ca nua" % (len(thung["loai"]) - 5))

        if thung["ta hon"]:
            ghi("   LUU Y: co %d thuc the CUT-HIDE dung nhieu lan che hon baseline. muc 5 chi dinh"
                % len(thung["ta hon"]))
            ghi("   nghia hai che do (cung |R|, va baseline dung nhieu hon), khong dinh nghia ca")
            ghi("   nay. Bo im la thien vi co loi cho ta, nen o day dem rieng va phai xu ly tay.")

        d = [m[1] for m in thung["kiem"]]
        if len(d) < 2:
            ghi("   khong du cap de kiem dinh")
            continue
        thong_ke, p, co, n, so_khong = wilcoxon_mot_phia(d, CHOT.DELTA_L)
        trung_vi = float(np.median(d))
        tuyet_doi = float(np.median([m[3]["lost"] - m[2]["lost"] for m in thung["kiem"]]))
        ket_qua[ten] = p
        ghi("   trung vi hieu Lost_rel               %+.4f   (tuyet doi: %+.1f mau)"
            % (trung_vi, tuyet_doi))
        ghi("   Wilcoxon-Pratt mot phia, H1 > %.2f   W = %.1f, p = %.4g, n = %d (%d hieu bang 0)"
            % (CHOT.DELTA_L, thong_ke, p, n, so_khong))
        ghi("   rank-biserial                        %+.3f" % co)

    if tong_cap:
        ti_le = tong_loai / tong_cap
        ghi("")
        ghi("Ti le thuc the bi loai: %d/%d = %.1f%%  (nguong muc 5 la %.0f%%)"
            % (tong_loai, tong_cap, 100 * ti_le, 100 * CHOT.TI_LE_LOAI_TOI_DA))
        if ti_le > CHOT.TI_LE_LOAI_TOI_DA:
            ghi("KHONG KET LUAN H2: ti le loai vuot nguong da chot.")
            return False

    if len(ket_qua) >= 2:
        dieu_chinh = holm(ket_qua)
        ghi("")
        ghi("Holm tren ho %d baseline tang (b), alpha = %.2f" % (len(ket_qua), CHOT.ALPHA))
        ghi("  %-12s %12s %12s   %s" % ("baseline", "p tho", "p sau Holm", "ket luan"))
        for ten in sorted(dieu_chinh, key=lambda k: ket_qua[k]):
            bac_bo = dieu_chinh[ten] < CHOT.ALPHA
            ghi("  %-12s %12.4g %12.4g   %s"
                % (ten, ket_qua[ten], dieu_chinh[ten],
                   "bac bo H0" if bac_bo else "khong bac bo"))
    elif len(ket_qua) == 1:
        ghi("")
        ghi("Chi mot baseline kiem duoc, khong can Holm.")
    return True


def co_mat(hang, khoa):
    return any(r["method_key"] == khoa for r in hang)


# ---------------------------------------------------------------- H3

def chay_h3(hang, nhom, ghi, mo_ta=False):
    ghi("")
    ghi("=" * 78)
    if mo_ta:
        ghi("r = |R|/|R|* per method and group, DESCRIPTIVE (section 11, point 7): no verdict")
    else:
        ghi("H3  r = |R|/|R|* lon hon 1 tren nhom chinh (r >= %.2f), gan 1 tren nhom suy bien"
            " (r <= %.2f)" % (CHOT.R_VUOT_1, CHOT.R_GAN_1))
    ghi("=" * 78)
    bao_cao = [r for r in hang if r["dataset"] in CHOT.CHI_BAO_CAO]
    if bao_cao:
        ghi("")
        ghi("  %d rows of %s are reported only and left out here (section 11, point 5)."
            % (len(bao_cao), ", ".join(sorted({r["dataset"] for r in bao_cao}))))
        hang = [r for r in hang if r["dataset"] not in CHOT.CHI_BAO_CAO]

    theo_nhom = defaultdict(lambda: defaultdict(list))
    khong_xep = 0
    khong_co_r = 0
    for r in hang:
        if np.isnan(r["mask_ratio"]):
            khong_co_r += 1
            continue
        khoa = (r["dataset"], r["gap"], r["sigma"])
        thong_tin = nhom.get(khoa)
        if thong_tin is None:
            khong_xep += 1
            continue
        theo_nhom[thong_tin["nhom"]][r["method"]].append(r["mask_ratio"])  # ten hien thi

    ghi("")
    ghi("  rows without |R|* (several patterns, or repeated items with g- > 0): %d" % khong_co_r)
    if khong_xep:
        ghi("  hang khong xep duoc nhom (khong co cau hinh tuong ung trong SC2): %d" % khong_xep)
    if not mo_ta:
        ghi("  muc 2 doi khai ro: H3 chi kiem tren lop thuc the mot mau khong lap muc.")

    for ten_nhom in ("chinh", "suy bien"):
        ghi("")
        ghi("-- nhom %s" % ten_nhom)
        if not theo_nhom[ten_nhom]:
            ghi("   khong co hang nao")
            continue
        ghi("   %-34s %5s %8s %8s   %s" % ("phuong phap", "n", "trung vi", "lon nhat", "ket luan"))
        for ten in sorted(theo_nhom[ten_nhom]):
            v = theo_nhom[ten_nhom][ten]
            tv = float(np.median(v))
            if mo_ta:
                ghi("   %-34s %5d %8.4f %8.4f" % (ten[:34], len(v), tv, max(v)))
                continue
            if ten_nhom == "chinh":
                ket = ("vuot 1" if tv >= CHOT.R_VUOT_1
                       else "gan 1" if tv <= CHOT.R_GAN_1 else "khong ket luan")
            else:
                ket = ("gan 1" if tv <= CHOT.R_GAN_1
                       else "vuot 1" if tv >= CHOT.R_VUOT_1 else "khong ket luan")
            ghi("   %-34s %5d %8.4f %8.4f   %s" % (ten[:34], len(v), tv, max(v), ket))

    if not mo_ta and theo_nhom["chinh"] and all(
            float(np.median(v)) <= CHOT.R_GAN_1 for v in theo_nhom["chinh"].values()):
        ghi("")
        ghi("  Moi phuong phap deu cho r gan 1 tren nhom chinh: H3 KHONG bac bo duoc. muc 2 da")
        ghi("  luong truoc ca nay -- do la ket qua am phai bao cao theo quy tac 3.4.9, khong")
        ghi("  phai loi cai dat.")


# ---------------------------------------------------------------- tuong duong muc 4

def chay_tuong_duong(hang, ghi):
    ghi("")
    ghi("=" * 78)
    ghi("Tuong duong (muc 4): Hodges-Lehmann kem khoang tin cay %.0f%%, doi chieu bien da chot"
        % (100 * (1 - CHOT.ALPHA)))
    ghi("=" * 78)
    hang = [r for r in hang if r["dataset"] not in CHOT.CHI_BAO_CAO]
    ho = [b for b in CHOT.HO_BASELINE if co_mat(hang, b)]
    for dai_luong, bien in CHOT.BIEN_TUONG_DUONG.items():
        ghi("")
        ghi("-- %s, bien tuong duong +/- %.2f" % (dai_luong, bien))
        if not ho:
            ghi("   khong co baseline tang (b)")
            continue
        ghi("   %-12s %5s %10s %22s   %s" % ("baseline", "n", "HL", "KTC", "ket luan"))
        for ten in ho:
            thung = ghep_cap(hang, ten, dai_luong)
            d = [m[1] for m in thung["kiem"]]
            if len(d) < 2:
                ghi("   %-12s %5d %10s %22s   khong du cap" % (ten, len(d), "-", "-"))
                continue
            hl, thap, cao = hodges_lehmann(d, CHOT.ALPHA)
            if np.isnan(thap) or np.isnan(cao):
                ket = "khoang khong tinh duoc"
            elif thap > -bien and cao < bien:
                ket = "tuong duong"
            elif thap >= bien or cao <= -bien:
                ket = "khac biet vuot bien"
            else:
                ket = "khong ket luan"
            ghi("   %-12s %5d %10.4f [%+8.4f, %+8.4f]   %s" % (ten, len(d), hl, thap, cao, ket))


# ---------------------------------------------------------------- chay

def main(argv=None):
    bo = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    bo.add_argument("--sc3", default="results-invariant",
                    help="thu muc hoac tep CSV cua SC3 (mac dinh results-invariant)")
    bo.add_argument("--sc2", default="results-invariant",
                    help="thu muc hoac tep CSV cua SC2, dung de xep nhom cau hinh")
    bo.add_argument("--bo-qua-xep-nhom", action="store_true",
                    help="khong doi chieu so cau hinh voi bang da chot (dung khi chay do tim)")
    bo.add_argument("--ra", default="", help="ghi bao cao ra tep thay vi in ra man hinh")
    bo.add_argument("--write-groups", action="store_true",
                    help="write config/nhom_cau_hinh.tsv from the SC2 pool files and stop; done "
                         "once, from the official runs, and committed")
    doi_so = bo.parse_args(argv)

    dong_ra = []

    def ghi(s=""):
        dong_ra.append(s)

    if doi_so.write_groups:
        tep = gom_tep(doi_so.sc2, "SC2_")
        nhom = xep_nhom(tep)
        if not nhom:
            print("no SC2 file of the pool profile in %s" % doi_so.sc2, file=sys.stderr)
            return 1
        ghi_bang_nhom(nhom, CHOT.BANG_NHOM)
        print("wrote %s (%d configurations)" % (CHOT.BANG_NHOM, len(nhom)))
        # Section 11, point 2: the repeated-item scope, from SC2 --profile=repeated.
        lap = xep_nhom(tep, profile="repeated")
        if lap:
            ghi_bang_nhom(lap, CHOT.BANG_NHOM_LAP_MUC)
            print("wrote %s (%d configurations)" % (CHOT.BANG_NHOM_LAP_MUC, len(lap)))
        else:
            print("no SC2 file of the repeated profile in %s: %s not written"
                  % (doi_so.sc2, CHOT.BANG_NHOM_LAP_MUC), file=sys.stderr)
            return 1
        return 0

    tep_sc3 = gom_tep(doi_so.sc3, "SC3_")
    if not tep_sc3:
        print("khong tim thay tep SC3 nao o %s" % doi_so.sc3, file=sys.stderr)
        return 1

    ghi("Phan tich thong ke H2 va H3 theo config/preregistration.md")
    ghi("")
    ghi("Tep SC3 doc vao:")
    # Kiem cot theo TUNG tep chu khong tren dong dau tien cua ca lo: mot thu muc co the lan
    # tep cu va tep moi, va bo ca lo vi mot tep cu la vut luon nhung tep dung.
    hang_tho = []
    bo_qua = []
    for duong_dan in tep_sc3:
        chu_thich, hang = doc_csv(duong_dan)
        dau = next((c for c in chu_thich if c.startswith("# run ")), "# run ?")
        thieu = [c for c in COT_BAT_BUOC if not hang or c not in hang[0]]
        if thieu:
            bo_qua.append((os.path.basename(duong_dan), thieu))
            ghi("  %-58s %d hang  BO QUA (thieu %s)"
                % (os.path.basename(duong_dan), len(hang), ", ".join(thieu)))
            continue
        ghi("  %-58s %d hang  %s" % (os.path.basename(duong_dan), len(hang), dau[6:]))
        hang_tho.extend(hang)

    if bo_qua:
        ghi("")
        ghi("  %d tep bi bo qua vi do ban ma cu sinh ra. Neu do la tep lay so vao bai thi phai"
            % len(bo_qua))
        ghi("  chay lai SC3 bang ban hien tai; neu chi la tep do tim cu thi bo qua la dung.")
    if not hang_tho:
        ghi("")
        ghi("KHONG CON TEP NAO DUNG DUOC.")
        xuat(dong_ra, doi_so.ra)
        return 1

    hang = gop_lap_lai(hang_tho, ghi)
    ghi("")
    ghi("Sau khi gop lan chay lai bang trung vi: %d hang (tu %d)" % (len(hang), len(hang_tho)))

    # Section 11: split by the items of the sensitive patterns, never by file name or option.
    theo_pham_vi = {"distinct": [], "repeated": []}
    for r in hang:
        theo_pham_vi[pham_vi_cua_tap(r["sensitive_set"])].append(r)
    ghi("")
    ghi("Rows by scope (section 11): distinct items %d, repeated items %d"
        % (len(theo_pham_vi["distinct"]), len(theo_pham_vi["repeated"])))

    tep_sc2 = gom_tep(doi_so.sc2, "SC2_")
    tat_ca_dung = True
    for pham_vi, profile, ten in (("distinct", "pool", "H2"), ("repeated", "repeated", "H2-R")):
        hang_pv = theo_pham_vi[pham_vi]
        if not hang_pv:
            continue
        ghi("")
        ghi("#" * 78)
        ghi("Scope: %s-item sensitive patterns, %d rows, tested as %s" % (pham_vi, len(hang_pv), ten))
        ghi("#" * 78)
        nhom = {}
        xep_dung = True
        if tep_sc2:
            nhom = xep_nhom(tep_sc2, profile=profile)
            if not doi_so.bo_qua_xep_nhom:
                xep_dung = kiem_xep_nhom(nhom, ghi, profile=profile)
        else:
            ghi("")
            ghi("  Khong co tep SC2: khong xep nhom duoc, H3 se khong chia nhom chinh/suy bien.")
            # Without groups H2 has no main group to be tested on; only a probe may go on.
            xep_dung = doi_so.bo_qua_xep_nhom

        h2_dung = chay_h2(hang_pv, ghi, None if doi_so.bo_qua_xep_nhom else nhom, ten)
        # H3 is preregistered for the main scope; section 11, point 7 makes the ratio of the
        # repeated scope descriptive.
        chay_h3(hang_pv, nhom, ghi, mo_ta=(pham_vi == "repeated"))
        chay_tuong_duong(hang_pv, ghi)
        tat_ca_dung = tat_ca_dung and h2_dung and xep_dung

    ghi("")
    ghi("=" * 78)
    ghi("Nhac lai: ma thoat 0 KHONG co nghia la H2 duoc ung ho. No chi co nghia la phan tich")
    ghi("chay duoc va ket luan nam trong bang tren.")

    xuat(dong_ra, doi_so.ra)
    return 0 if tat_ca_dung else 1


def xuat(dong_ra, duong_dan):
    van_ban = "\n".join(dong_ra) + "\n"
    if duong_dan:
        with io.open(duong_dan, "w", encoding="utf-8", newline="\n") as f:
            f.write(van_ban)
        print("da ghi %s" % duong_dan)
    else:
        sys.stdout.write(van_ban)


if __name__ == "__main__":
    sys.exit(main())
