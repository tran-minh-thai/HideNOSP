"""Scratch structural comparison of the old and the new protocol.yaml under a key map: every
non-string value must be identical, every string is either identical or listed as translated,
and no key may be left unmapped on either side."""
import sys, yaml
KEYS = {
 "dong_1_nguong_va_pham_vi": "row_1_thresholds_and_scope", "chung_cho_moi_bo": "common_to_every_dataset",
 "do_dai_mau_nhay_cam": "sensitive_pattern_length", "sensitive_sets_moi_o": "sensitive_sets_per_cell",
 "phan_vi_lay_mau": "sampling_percentiles", "cach_lay_mau": "sampling_method",
 "khoang_cach_chung": "common_gaps", "theo_bo": "per_dataset", "khoang_cach": "gaps", "ghi_chu": "note",
 "tong_hop": None,  # two meanings: resolved by the parent below
 "pham_vi_lap_muc": "repeated_item_scope", "bang_nhom": "group_table", "phuong_phap_H2R": "method_H2R",
 "chi_bao_cao": "report_only", "mat_pareto": "pareto_front", "quy_tac_chon_sigma": "sigma_rule",
 "dong_2_nguong_epsilon_cua_B_eps": "row_2_epsilon_of_B_eps", "gia_tri": "value",
 "kiem_bat_buoc": "required_check", "can_cu": "basis", "dong_3_hat_giong": "row_3_seeds",
 "lay_mau_tap_nhay_cam": "sensitive_set_sampling", "sinh_du_lieu_tong_hop": "synthetic_data_generation",
 "phuong_phap_ngau_nhien": "randomized_methods",
 "cach_tong_hop_phuong_phap_ngau_nhien": "randomized_methods_aggregation",
 "giai_thuat_di_truyen": "genetic_algorithm",
 "dong_4_tham_so_sinh_du_lieu_tong_hop": "row_4_synthetic_data_parameters",
 "phan_phoi_muc": "item_distribution", "kich_thuoc_bang_chu_cai": "alphabet_size",
 "so_chuoi": "sequence_count", "do_dai_chuoi": "sequence_length", "rho_rep_muc_tieu": "target_rho_rep",
 "beta_muc_tieu": "target_beta", "dong_5_moi_truong_chay": "row_5_run_environment", "co_xms": "xms_flag",
 "co_xmx": "xmx_flag", "he_dieu_hanh": "operating_system", "ten_may_do": "machine_name",
 "dong_6_do_bo_nho": "row_6_memory_measurement", "cach_do": "method", "chu_ky_lay_mau_ms": "sampling_period_ms",
 "bo_nho_thuong_tru": "resident_memory", "dong_7_gioi_han_thoi_gian": "row_7_time_limit",
 "gioi_han_moi_lan_chay_giay": "limit_per_run_seconds", "ma_qua_thoi_gian": "timeout_code",
 "ma_het_bo_nho": "out_of_memory_code", "xu_ly_khi_qua_han": "when_exceeded",
 "nguong_bo_toi_da": "max_dropped_share", "dong_8_so_lan_lap_va_tong_hop": "row_8_repetitions_and_aggregation",
 "duoi_1_giay": "under_1_second", "tu_1_den_10_giay": "from_1_to_10_seconds",
 "tu_10_den_120_giay": "from_10_to_120_seconds", "tren_120_giay": "over_120_seconds",
 "dong_9_quy_trinh_lam_nong": "row_9_warmup_procedure", "so_lan_lam_nong": "warmup_runs",
 "quyet_dinh_cai_dat": "implementation_decisions", "pham_vi_mu_P": "mu_P_scope", "thang_trong_so": "weight_scale",
 "quan_he_S_cong": "S_plus_relation", "tia_theo_tien_to_khi_khai_pha": "prefix_pruning_when_mining",
 "ngu_nghia_mask": "mask_semantics", "tuyen_tinh_hoa_tap_muc": "itemset_linearisation",
 "trong_so_nut_khi_mu_lon_hon_1": "node_weight_when_mu_above_1", "thuc_the_nho": "small_instances",
 "so_vi_tri_ung_vien_toi_da": "max_candidate_positions", "so_lan_che_toi_da": "max_masks",
 "moc_toi_uu_cua_SC3": "SC3_optimum_reference", "nhieu_mau_duoi_nguong_CSDL": "several_patterns_under_database_threshold",
 "phien_ban_ma": "code_version",
}
counts = {"identical": 0, "translated": 0}
translated, problems = [], []

def new_key(k, parent):
    if k == "tong_hop":
        return "synthetic" if parent == "theo_bo" else "aggregation"
    return KEYS.get(k, k)

def walk(o, n, path, parent):
    if isinstance(o, dict):
        if not isinstance(n, dict):
            problems.append("%s: dict vs %r" % (path, type(n))); return
        mapped = {new_key(k, parent): k for k in o}
        if set(mapped) != set(n):
            problems.append("%s: keys only old %s, only new %s" % (path, sorted(set(mapped) - set(n)), sorted(set(n) - set(mapped))))
        for nk, ok in mapped.items():
            if nk in n:
                walk(o[ok], n[nk], path + "/" + nk, ok)
    elif isinstance(o, list):
        if not isinstance(n, list) or len(o) != len(n):
            problems.append("%s: list %r vs %r" % (path, o, n)); return
        for i, (a, b) in enumerate(zip(o, n)):
            walk(a, b, "%s[%d]" % (path, i), parent)
    elif isinstance(o, str):
        if not isinstance(n, str):
            problems.append("%s: str vs %r" % (path, n)); return
        if o.strip() == n.strip():
            counts["identical"] += 1
        else:
            counts["translated"] += 1; translated.append(path)
    else:
        if o != n or type(o) != type(n):
            problems.append("%s: %r vs %r" % (path, o, n))
        else:
            counts["identical"] += 1

old = yaml.safe_load(open(sys.argv[1], encoding="utf-8"))
new = yaml.safe_load(open(sys.argv[2], encoding="utf-8"))
walk(old, new, "", None)
unused = [k for k in KEYS if k != "tong_hop" and ("/" + KEYS[k]) not in "".join("/" + p for p in [])]
print("scalars identical (numbers, null, booleans, unchanged strings): %d; strings translated: %d"
      % (counts["identical"], counts["translated"]))
for p in translated:
    print("   translated", p)
print("structural problems:", len(problems))
for p in problems:
    print("  ", p)
sys.exit(1 if problems else 0)
