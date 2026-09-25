"""Ghim phan thong ke cua thong_ke.py vao nhung gia tri tinh duoc bang tay.

Vi sao can. Sai sot o day khong lam chuong trinh do; no lam mot con so trong bai sai ma khong
ai thay. Moi phep thu duoi day co mot dap so tinh tay ghi kem trong chu thich, de doi chieu
duoc ma khong phai tin vao scipy hay vao chinh script.

Chay: python tools/thong_ke_test.py
Ma thoat 1 khi co phep thu hong.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import thong_ke as tk


def gan(a, b, sai_so=1e-9, ghi_chu=""):
    if not (abs(a - b) <= sai_so):
        raise AssertionError("cho %r, duoc %r  %s" % (b, a, ghi_chu))


def thu_hang_pratt():
    """d = [0, 0, 1, -2, 3].

    |d| = [0, 0, 1, 2, 3]; hang trung binh = [1.5, 1.5, 3, 4, 5]. Hieu bang 0 VAN duoc xep hang
    (do la Pratt) nhung khong vao ve nao: W+ = 3 + 5 = 8, W- = 4.
    """
    w_duong, w_am = tk.hang_pratt([0, 0, 1, -2, 3])
    gan(w_duong, 8.0, ghi_chu="W+")
    gan(w_am, 4.0, ghi_chu="W-")

    # Neu vut hieu bang 0 di truoc khi xep hang (Wilcoxon goc) thi hang la [1, 2, 3] va
    # W+ = 1 + 3 = 4, W- = 2. Khac han. Phep thu nay giu cho khong ai lang le doi quy uoc.
    w_goc_duong, w_goc_am = tk.hang_pratt([1, -2, 3])
    gan(w_goc_duong, 4.0)
    gan(w_goc_am, 2.0)


def thu_rank_biserial():
    """(W+ - W-)/(W+ + W-) = (8 - 4)/12 = 1/3."""
    gan(tk.rank_biserial([0, 0, 1, -2, 3]), 1.0 / 3.0)
    # Moi hieu duong thi bang +1; moi hieu am thi bang -1.
    gan(tk.rank_biserial([1, 2, 3]), 1.0)
    gan(tk.rank_biserial([-1, -2, -3]), -1.0)
    # Doi xung thi bang 0: |d| = [1, 1] cho hang [1.5, 1.5], W+ = W- = 1.5.
    gan(tk.rank_biserial([1, -1]), 0.0)
    # Toan so 0 thi khong dinh nghia duoc, phai la nan chu khong phai 0.
    assert np.isnan(tk.rank_biserial([0, 0, 0]))


def thu_wilcoxon_lech_dich():
    """Phep kiem phai chay tren d - delta_L chu khong phai tren d.

    d = [0.02] * 6: moi hieu bang DUNG nguong, nen khong co bang chung nao cho "lon hon nguong".
    """
    _, p, co, n, so_khong = tk.wilcoxon_mot_phia([0.02] * 6, 0.02)
    gan(p, 1.0, ghi_chu="moi hieu bang dung dich thi p phai bang 1")
    gan(co, 0.0)
    gan(n, 6)
    gan(so_khong, 6)

    # Doi voi cung du lieu do nhung dich 0 thi moi hieu deu duong: p phai nho nhat co the voi
    # n = 6, tuc 1/2^6 = 0.015625.
    _, p0, co0, _, _ = tk.wilcoxon_mot_phia([0.02] * 6, 0.0)
    gan(p0, 1.0 / 64.0, sai_so=1e-12)
    gan(co0, 1.0)


def thu_wilcoxon_dung_pratt_chu_khong_phai_wilcox():
    """Ghim quy uoc Pratt bang cach LIET KE TAY phan bo vo hieu, khong tin vao scipy.

    d = [0, 0, 0, 0, 1, -2, 3, 4, 5, 6, 7, -8]. Xep hang |d|: bon so 0 buoc nhau o hang trung
    binh 2,5, tam quan sat khac 0 lan luot mang hang 5 den 12. Pratt giu nguyen nhung hang do.
    Duoi gia thuyet vo hieu moi quan sat khac 0 nhan dau +/- doc lap voi xac suat 1/2, nen
    W+ chay tren 2^8 = 256 kha nang dong xac suat. Dem tay duoc 29 kha nang co W+ >= 50, tuc
    p = 29/256 = 0,11328125.

    Phep thu nay quan trong hon ve mat thuc hanh so voi ve mat ly thuyet: trong scipy 1.18,
    method="exact" cho 0,211914 duoi Pratt, tuc SAI -- no dung phan bo dau hang chuan tren ca
    12 quan sat, khong ke rang hang cua cac so 0 khong duoc phep nhan dau. Chi method="auto"
    cho dung. Ai "don dep" ma doi sang method="exact" se lam moi gia tri p trong bai sai lech
    ma khong co dau hieu gi.
    """
    import itertools
    from scipy import stats as _st

    d = [0, 0, 0, 0, 1, -2, 3, 4, 5, 6, 7, -8]
    hang = _st.rankdata(np.abs(d))
    khac_khong = hang[np.array(d) != 0]
    dau_that = np.array([x for x in d if x != 0]) > 0
    quan_sat = float(khac_khong[dau_that].sum())
    gan(quan_sat, 50.0, ghi_chu="W+ quan sat")

    dem = sum(1 for dau in itertools.product([0, 1], repeat=len(khac_khong))
              if sum(k for k, s in zip(khac_khong, dau) if s) >= quan_sat)
    p_tay = dem / 2 ** len(khac_khong)
    gan(p_tay, 29 / 256.0, ghi_chu="liet ke tay")

    _, p, _, n, so_khong = tk.wilcoxon_mot_phia(d, 0.0)
    gan(n, 12)
    gan(so_khong, 4)
    gan(p, p_tay, sai_so=1e-12, ghi_chu="phai khop liet ke tay")

    p_sai = _st.wilcoxon(d, zero_method="pratt", alternative="greater", method="exact").pvalue
    assert abs(p_sai - p_tay) > 1e-3,         "scipy method='exact' truoc day sai duoi Pratt; neu no da dung thi cap nhat chu thich"


def thu_wilcoxon_huong():
    """Mot phia "lon hon": du lieu nghieng am phai cho p lon, nghieng duong phai cho p nho."""
    _, p_duong, _, _, _ = tk.wilcoxon_mot_phia([1, 2, 3, 4, 5], 0.0)
    _, p_am, _, _, _ = tk.wilcoxon_mot_phia([-1, -2, -3, -4, -5], 0.0)
    assert p_duong < 0.05, p_duong
    assert p_am > 0.95, p_am


def thu_holm():
    """p = [0.01, 0.02, 0.03, 0.04], m = 4.

    Buoc 1: 4 x 0.01 = 0.04. Buoc 2: 3 x 0.02 = 0.06. Buoc 3: 2 x 0.03 = 0.06.
    Buoc 4: 1 x 0.04 = 0.04, nhung phai don dieu nen nang len 0.06.
    """
    ra = tk.holm({"a": 0.01, "b": 0.02, "c": 0.03, "d": 0.04})
    gan(ra["a"], 0.04)
    gan(ra["b"], 0.06)
    gan(ra["c"], 0.06)
    gan(ra["d"], 0.06)

    # Chan tren 1.0.
    ra = tk.holm({"a": 0.5, "b": 0.6})
    gan(ra["a"], 1.0)
    gan(ra["b"], 1.0)

    # Mot phan tu thi Holm khong doi gi.
    gan(tk.holm({"a": 0.03})["a"], 0.03)


def thu_hodges_lehmann():
    """d = [1, 2, 3, 4, 5]: trung binh Walsh doi xung quanh 3 nen uoc luong phai bang 3."""
    hl, thap, cao = tk.hodges_lehmann([1, 2, 3, 4, 5])
    gan(hl, 3.0)
    assert thap <= hl <= cao, (thap, hl, cao)

    # Doi toan bo du lieu them mot hang so thi uoc luong doi dung bang nhieu do.
    hl2, _, _ = tk.hodges_lehmann([x + 10 for x in [1, 2, 3, 4, 5]])
    gan(hl2, 13.0)

    # Du lieu tan hon thi khoang rong hon.
    _, t1, c1 = tk.hodges_lehmann([1, 1, 1, 1, 1, 1, 1, 1])
    _, t2, c2 = tk.hodges_lehmann([-9, 1, 1, 1, 1, 1, 1, 11])
    assert (c2 - t2) > (c1 - t1), ((t1, c1), (t2, c2))


def thu_gop_trung_vi():
    """Gop lan chay lai phai lay TRUNG VI, khong phai trung binh.

    lost = [1, 1, 10]: trung vi 1, trung binh 4. Lay trung binh thi mot lan chay lech keo ca
    gia tri di.
    """
    hang = []
    for hat, lost in ((2026, 1), (2027, 1), (2028, 10)):
        hang.append({"dataset": "d", "shape": "s", "gap": "[0,2]", "sigma": "20",
                     "sensitive_set": "[[0, 1]]", "method_key": "rr", "method": "RR-random",
                     "method_seed": str(hat), "hf": "0", "lost": str(lost),
                     "lost_rel": str(lost / 100.0), "ghost": "0", "dist_r": "0",
                     "sup_dist": "0", "mask_count": "5", "min_masks": "5", "mask_ratio": "1",
                     "lower_bound": "0", "achieved_ratio": "", "frequent_before": "100",
                     "nonsensitive_before": "100", "frequent_after": "99"})
    ra = tk.gop_lap_lai(hang, lambda *a: None)
    assert len(ra) == 1, ra
    gan(ra[0]["lost"], 1.0, ghi_chu="trung vi cua [1, 1, 10]")
    gan(ra[0]["so_lan"], 3)
    gan(ra[0]["so_hat"], 3)

    # Phuong phap tat dinh: mot hang, khong hat giong, trung vi cua mot so la chinh no.
    mot = dict(hang[0], method_key="hha", method="HHA-pathcounts", method_seed="", lost="7")
    ra = tk.gop_lap_lai([mot], lambda *a: None)
    gan(ra[0]["lost"], 7.0)
    gan(ra[0]["so_hat"], 0)


def _hang(method_key, mask_count, lost_rel, hf=0.0, tap="[[0, 1]]", status=""):
    return {"dataset": "d", "gap": "[0,2]", "sigma": 20, "sensitive_set": tap,
            "method_key": method_key, "method": method_key, "status": status,
            "hf": hf, "lost_rel": lost_rel, "lost": lost_rel * 100, "sup_dist": 0.0,
            "mask_count": mask_count, "mask_ratio": 1.0, "so_hat": 0, "so_lan": 1}


def thu_ghep_cap():
    """Ba thung phai tach dung theo |R|, va HF > 0 phai loai ca cap."""
    hang = [
        # cung |R| -> vao kiem dinh
        _hang("cut-hide", 10, 0.10, tap="A"), _hang("hha", 10, 0.20, tap="A"),
        # baseline dung nhieu hon -> bao cao rieng
        _hang("cut-hide", 10, 0.10, tap="B"), _hang("hha", 14, 0.20, tap="B"),
        # CUT-HIDE dung nhieu hon -> thung rieng, khong duoc im lang bo di
        _hang("cut-hide", 14, 0.10, tap="C"), _hang("hha", 10, 0.20, tap="C"),
        # baseline chua an xong -> loai ca cap
        _hang("cut-hide", 10, 0.10, tap="D"), _hang("hha", 10, 0.20, hf=0.5, tap="D"),
    ]
    thung = tk.ghep_cap(hang, "hha", "lost_rel")
    gan(len(thung["kiem"]), 1)
    gan(len(thung["baseline hon"]), 1)
    gan(len(thung["ta hon"]), 1)
    gan(len(thung["loai"]), 1)
    # Hieu so lay theo chieu baseline tru chung ta.
    gan(thung["kiem"][0][1], 0.10)
    assert "HF" in thung["loai"][0][1], thung["loai"][0][1]

    # Ma trang thai (OT, OOM) cung loai cap.
    hang = [_hang("cut-hide", 10, 0.10, tap="E"),
            _hang("hha", 10, 0.20, tap="E", status="OT")]
    thung = tk.ghep_cap(hang, "hha", "lost_rel")
    gan(len(thung["loai"]), 1)
    assert "OT" in thung["loai"][0][1], thung["loai"][0][1]


def thu_xep_nhom_dung_nguong():
    """Nguong muc 3 la >= 0,20 va <= 0,05, tuc bien thuoc ve nhom ngoai, khong phai trung gian."""
    import tempfile
    import io as _io

    def viet(duong_dan, bo_ba):
        with _io.open(duong_dan, "w", encoding="utf-8", newline="\n") as f:
            f.write("# run x\n")
            f.write("# profile pool\n")
            f.write("dataset,gap,sigma,pattern,sup_NO,sup_seq,sup_NO_no_gap,min_masks,"
                    "mask_share,rho_rep,beta,min_masks_exact\n")
            for ten, rho, beta in bo_ba:
                f.write("datasets/%s.txt,\"[0,3]\",100,\"[1]\",1,1,1,1,0,%s,%s,true\n"
                        % (ten, rho, beta))

    thu_muc = tempfile.mkdtemp()
    duong_dan = os.path.join(thu_muc, "SC2_test.csv")
    viet(duong_dan, [("bien_tren", 0.20, 0.5), ("bien_duoi", 0.05, 0.5),
                     ("giua", 0.10, 0.5)])
    nhom = tk.xep_nhom([duong_dan])
    assert nhom[("bien_tren", "[0,3]", 100)]["nhom"] == "chinh"
    assert nhom[("bien_duoi", "[0,3]", 100)]["nhom"] == "suy bien"
    assert nhom[("giua", "[0,3]", 100)]["nhom"] == "trung gian"


def thu_xep_nhom_lay_trung_vi():
    """Muc 3 chot TRUNG VI, khong phai trung binh: phan bo lech nen hai cai khac nhau."""
    import tempfile
    import io as _io

    thu_muc = tempfile.mkdtemp()
    duong_dan = os.path.join(thu_muc, "SC2_test.csv")
    with _io.open(duong_dan, "w", encoding="utf-8", newline="\n") as f:
        f.write("# run x\n")
        f.write("# profile pool\n")
        f.write("dataset,gap,sigma,pattern,sup_NO,sup_seq,sup_NO_no_gap,min_masks,"
                "mask_share,rho_rep,beta,min_masks_exact\n")
        # rho_rep = [0.01, 0.02, 0.90]: trung vi 0.02 (suy bien), trung binh 0.31 (trung gian).
        for k, rho in enumerate((0.01, 0.02, 0.90)):
            f.write("datasets/x.txt,\"[0,3]\",100,\"[%d, 9]\",1,1,1,1,0,%s,0.5,true\n" % (k, rho))
    nhom = tk.xep_nhom([duong_dan])
    thong_tin = nhom[("x", "[0,3]", 100)]
    gan(thong_tin["rho_rep"], 0.02)
    assert thong_tin["nhom"] == "suy bien", thong_tin


def thu_hang_so_khop_ban_chot():
    """Hang so trong lop CHOT phai khop voi config/preregistration.md.

    Doc thang tep chot va tim tung con so. Ai sua mot ben ma quen ben kia thi phep thu nay hong.
    """
    import io as _io
    goc = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    duong_dan = os.path.join(goc, "config", "preregistration.md")
    if not os.path.exists(duong_dan):
        print("  bo qua: khong thay config/preregistration.md")
        return
    van_ban = _io.open(duong_dan, encoding="utf-8").read()
    for ten, chuoi in (("DELTA_L", "0,02"), ("RHO_CHINH", "0,20"), ("RHO_SUY_BIEN", "0,05"),
                       ("R_VUOT_1", "1,10"), ("R_GAN_1", "1,05")):
        assert chuoi in van_ban, "khong thay %s (%s) trong ban chot" % (ten, chuoi)
    assert "**10%**" in van_ban or "10%" in van_ban
    assert "2026 tới 2035" in van_ban or "2026 toi 2035" in van_ban
    for ten in ("HHA", "DBSH", "SBSH-r1", "SBSH-rmax"):
        assert ten in van_ban, "ho Holm thieu %s trong ban chot" % ten


def _viet_sc2(duong_dan, hang, profile):
    """An SC2 file: rows of (dataset, rho_rep, beta, distinct items), profile line if given."""
    import io as _io
    with _io.open(duong_dan, "w", encoding="utf-8", newline="\n") as f:
        f.write("# run x\n")
        if profile:
            f.write("# profile %s\n" % profile)
        f.write("dataset,gap,sigma,pattern,sup_NO,sup_seq,sup_NO_no_gap,min_masks,"
                "mask_share,rho_rep,beta,min_masks_exact\n")
        for k, (ten, rho, beta, rieng) in enumerate(hang):
            f.write("datasets/%s.txt,\"[0,3]\",100,\"[%d, 9]\",1,1,1,1,0,%s,%s,%s\n"
                    % (ten, k, rho, beta, "true" if rieng else "false"))


def thu_xep_nhom_chi_doc_toan_kho():
    """Section 10: only SC2 files of the pool profile group; a sampled file is ignored."""
    import tempfile
    thu_muc = tempfile.mkdtemp()
    mau_con = os.path.join(thu_muc, "SC2_sample.csv")
    toan_kho = os.path.join(thu_muc, "SC2_pool.csv")
    _viet_sc2(mau_con, [("x", 0.90, 0.5, False), ("x", 0.90, 0.5, True)], None)
    _viet_sc2(toan_kho, [("x", 0.01, 0.5, True), ("x", 0.02, 0.5, True), ("x", 0.03, 0.5, True)],
              "pool")
    nhom = tk.xep_nhom([mau_con, toan_kho])
    thong_tin = nhom[("x", "[0,3]", 100)]
    gan(thong_tin["rho_rep"], 0.02)
    gan(thong_tin["pool"], 3)
    assert thong_tin["nhom"] == "suy bien", thong_tin
    assert tk.xep_nhom([mau_con]) == {}, "a sampled file alone must give no group"


def thu_xep_nhom_tu_choi_mau_lap_muc():
    """A pool file with a repeated-item pattern is not a pool file: stop, do not average it in."""
    import tempfile
    duong_dan = os.path.join(tempfile.mkdtemp(), "SC2_pool.csv")
    _viet_sc2(duong_dan, [("x", 0.30, 0.5, True), ("x", 0.90, 0.5, False)], "pool")
    try:
        tk.xep_nhom([duong_dan])
    except ValueError:
        return
    raise AssertionError("a repeated-item row in a pool file was accepted")


def thu_bang_nhom_khop_va_lech():
    """The locked table is compared configuration by configuration, both ways."""
    import tempfile
    thu_muc = tempfile.mkdtemp()
    sc2 = os.path.join(thu_muc, "SC2_pool.csv")
    _viet_sc2(sc2, [("x", 0.30, 0.5, True), ("y", 0.01, 0.01, True)], "pool")
    nhom = tk.xep_nhom([sc2])
    bang_tep = os.path.join(thu_muc, "nhom.tsv")
    tk.ghi_bang_nhom(nhom, bang_tep)
    bang = tk.doc_bang_nhom(bang_tep)
    assert tk.kiem_xep_nhom(nhom, lambda s="": None, bang), "an unchanged table must match"
    bang[("x", "[0,3]", 100)]["group"] = "trung gian"
    assert not tk.kiem_xep_nhom(nhom, lambda s="": None, bang), "a changed group must fail"
    del bang[("x", "[0,3]", 100)]
    assert not tk.kiem_xep_nhom(nhom, lambda s="": None, bang), "a missing row must fail"
    assert tk.doc_bang_nhom(os.path.join(thu_muc, "none.tsv")) is None


def thu_h2_chi_tren_nhom_chinh():
    """Section 3 and the design: H2 is tested on the main group, the rest is not pooled in."""
    hang = []
    for ten in ("a", "b"):
        for pp, lost in (("cut-hide", 0.10), ("hha", 0.20)):
            r = _hang(pp, 10, lost, tap="T")
            r["dataset"] = ten
            hang.append(r)
    nhom = {("a", "[0,2]", 20): {"nhom": "chinh"}, ("b", "[0,2]", 20): {"nhom": "suy bien"}}
    ra = []
    tk.chay_h2(hang, lambda s="": ra.append(s), nhom)
    assert any("tested on the 2 rows of the main group only" in s for s in ra), ra[:8]



def thu_xep_nhom_khong_dem_trung_lan_chay_lai():
    """Two pool files of one configuration (a rerun) count each pattern once."""
    import tempfile
    thu_muc = tempfile.mkdtemp()
    hang = [("x", 0.30, 0.5, True)]
    for ten in ("SC2_a.csv", "SC2_b.csv"):
        _viet_sc2(os.path.join(thu_muc, ten), hang, "pool")
    nhom = tk.xep_nhom([os.path.join(thu_muc, "SC2_a.csv"), os.path.join(thu_muc, "SC2_b.csv")])
    gan(nhom[("x", "[0,3]", 100)]["pool"], 1)
    _viet_sc2(os.path.join(thu_muc, "SC2_c.csv"), [("x", 0.40, 0.5, True)], "pool")
    try:
        tk.xep_nhom([os.path.join(thu_muc, "SC2_a.csv"), os.path.join(thu_muc, "SC2_c.csv")])
    except ValueError:
        return
    raise AssertionError("one pattern with two different profiles was accepted")

PHEP_THU = [v for k, v in sorted(globals().items()) if k.startswith("thu_")]


def main():
    hong = 0
    for ham in PHEP_THU:
        ten = ham.__name__
        try:
            ham()
            print("  OK   %s" % ten)
        except Exception as loi:            # noqa: BLE001 -- bao cao het, khong dung o loi dau
            hong += 1
            print("  HONG %s: %s" % (ten, loi))
    print("")
    print("%d phep thu, %d hong" % (len(PHEP_THU), hong))
    return 1 if hong else 0


if __name__ == "__main__":
    sys.exit(main())
