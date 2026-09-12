"""Gop ket qua ra soat "nhung thu con phai quyet dinh" thanh mot tai lieu theo chu de.

Dau vao la ket qua JSON cua mot dot ra soat da chay (dot ra soat liet-ke-quyet-dinh), dau ra la
config/quyet_dinh_can_chot.md. Giu lai de lan ra soat sau gop duoc theo dung cach, va de thay
ro cach gop: moi diem chi vao DUNG MOT chu de, khong diem nao bi bo.

Chay: python tools/gop_quyet_dinh.py   (sua SRC cho tro toi ket qua moi)
"""
import io, json, re, unicodedata

SRC = r"review_result.json"
OUT = r"G:/My Drive/Researching/NCS_LTMN/HideNOSP Project/code/config/quyet_dinh_can_chot.md"

d = json.loads(io.open(SRC, encoding="utf-8", errors="replace").read())["result"]
items = [dict(x, origin="quet") for x in d["confirmed"]] + \
        [dict(x, origin="phebinh") for x in d["addedByCritic"]]


GREEK = {"ρ": "rho", "β": "beta", "ε": "epsilon", "σ": "sigma",
         "μ": "mu", "ℓ": "l", "δ": "delta", "ψ": "psi"}


def fold(s):
    s = s or ""
    for g, r in GREEK.items():
        s = s.replace(g, r)
    s = s.replace("đ", "d").replace("Đ", "D")
    s = unicodedata.normalize("NFD", s)
    return "".join(c for c in s if unicodedata.category(c) != "Mn").lower()


# (ma, ten chu de, do gap, tu khoa nhan dien)
THEMES = [
    ("A1", "Ngưỡng hiệu ứng thực tiễn δ_L cho H2", "SC3", ["delta_l", "d_l", "hieu ung thuc tien"]),
    ("A2", "Ngưỡng ρ_rep và β để phân nhóm: giá trị, đơn vị áp, và vùng ở giữa", "SC3", ["rho_rep"]),
    ("A3", "Ngưỡng beta cho nhóm đối chứng ràng buộc", "SC3", ["nguong beta", "beta cho nhom"]),
    ("A4", "Biên tương đương cho Lost và SupDist", "SC3", ["bien tuong duong"]),
    ("A5", "Phép kiểm cho H3 và nghĩa định lượng của \"gần 1\"", "SC3", ["h3 va nghia", "gan 1", "h3 tren nhom suy bien"]),
    ("A6", "Họ baseline đưa vào hiệu chỉnh Holm", "SC3", ["holm"]),
    ("A7", "Ghép cặp H2: cùng ngân sách |R| hay để mỗi phương pháp tự chọn", "SC3", ["ngan sach |r|", "ghep cap"]),
    ("A8", "Quy ước tính Wilcoxon: hiệu bằng 0, hạng buộc, cỡ hiệu ứng", "SC3", ["wilcoxon"]),
    ("A9", "Quy tắc loại trừ thực thể hỏng (HF>0, quá giờ, hết bộ nhớ)", "SC3", ["thuc the hong"]),
    ("A10", "Số lần chạy lại của các phương pháp ngẫu nhiên", "SC3", ["phuong phap ngau nhien", "chay lai cua cac"]),
    ("A11", "Lưới σ, ℓ_max, khoảng cách và |𝒮| cho từng bộ", "SC3", ["sigma cho tung bo", "l_max va khoang cach", "luoi sigma", "gia tri sigma", "chot sigma va do rong", "ell_max"]),
    ("A12", "Ngưỡng ε của B_ε cho từng bộ", "SC3", ["epsilon"]),
    ("A13", "Các phân vị sup_NO để lấy mẫu tập nhạy cảm", "SC3", ["phan vi sup_no"]),
    ("A14", "Phạm vi μ(P): giữ hẹp ở μ=1 hay mở sang μ≥2", "SC3", ["mu(p)", "lap muc", "trong so nut khi"]),
    ("A15", "Nhiều mẫu nhạy cảm dưới ngưỡng CSDL: LP làm tròn hay GA lai", "SC3", ["lp hay ga", "lp lam tron", "nhieu mau nhay cam o nguong", "ho metaheuristic"]),
    ("A16", "\"Thực thể nhỏ\" là gì, và SC3 lấy biến thể ILP nào làm mốc", "SC3", ["thuc the nho", "bien the ilp"]),
    ("A17", "Gắn phiên bản mã vào tệp kết quả (đang là unversioned)", "SC3", ["unversioned", "phien ban ma vao"]),
    ("A18", "Bộ chuỗi thời gian rời rạc hoá của dòng NOSEP", "SC3", ["chuoi thoi gian"]),
    ("A19", "Vai chính thức của từng bộ dữ liệu sau khi đo", "SC3", ["vai cua tung bo", "bo du lieu thuc nghiem moi", "vao nhom chung minh"]),
    ("A20", "Trần cho tỉ lệ |R|*/Σ|S| và xử lý mẫu cực đoan", "SC3", ["tran cho ti le"]),
    ("A21", "Cách in |R|* và β khi tập nhạy cảm có nhiều mẫu", "SC3", ["cach in |r|"]),
    ("A22", "Bước 3 với mẫu lặp mục: tập ứng viên chưa định nghĩa", "SC3", ["buoc 3 voi mau"]),
    ("A23", "Nội dung tệp preregistration và việc chuyển go/no-go sang chính thức", "SC3", ["preregistration.md", "go/no-go"]),
    ("A24", "Baseline tầng (a) khi tập nhạy cảm có nhiều hơn một mẫu", "SC3", ["tang (a) khi tap"]),
    ("A25", "Đối chiếu SBSH với Thuật toán 2 và 3 của [R3]", "SC3", ["sbsh"]),

    ("B1", "Máy đo, cờ JVM, số lần làm nóng, giới hạn thời gian", "nop", ["may do", "xms", "lam nong", "gioi han thoi gian", "moi truong chay"]),
    ("B2", "Đo bộ nhớ đỉnh: heap hay RSS của tiến trình", "nop", ["heap hay rss", "bo nho dinh"]),
    ("B3", "σ báo cáo theo tỉ lệ hay tuyệt đối", "nop", ["ti le hay giu tuyet doi", "ti le hay tuyet doi"]),
    ("B4", "Tham số và quy mô sinh dữ liệu tổng hợp", "nop", ["du lieu tong hop"]),
    ("B5", "Cài NETGAP tham chiếu cho phép kiểm SC1", "nop", ["netgap"]),
    ("B6", "Nguồn bộ TRUCKS, và số phận SC8 nếu không có", "nop", ["trucks", "sc8 khi khong"]),
    ("B7", "Ngưỡng ψ và đơn vị báo cáo của các bài gốc cho SC8", "nop", ["nguong psi"]),
    ("B8", "[R11] Zhao 2025: còn chặn Cổng 1 không, có đếm theo sup_NO không", "nop", ["[r11]", "zhao 2025"]),
    ("B9", "Trạng thái các khoảng trống G1, G2, G5 và phạm vi rà", "nop", ["khoang trong g", "g1 va g5", "g2 khong thuoc", "pham vi ra"]),
    ("B10", "Mức đọc bắt buộc cho [R22] trước khi tuyên bố G5", "nop", ["[r22]"]),
    ("B11", "Cổng 1: có tuyên bố đã qua khi mức đọc chưa đạt không", "nop", ["cong 1 da qua"]),
    ("B12", "Người thứ hai đọc kiểm chứng minh", "nop", ["nguoi thu hai"]),
    ("B13", "Nơi nộp, mẫu định dạng, chỗ đặt mã giả và chứng minh dài", "nop", ["noi nop", "ma gia"]),
    ("B14", "Câu kết quả thực nghiệm trong tóm tắt", "nop", ["cau ket qua thuc nghiem"]),
    ("B15", "Giấy phép mã nguồn của kho khi công bố", "nop", ["giay phep ma nguon"]),
    ("B16", "Điều khoản dùng lại dữ liệu và tuyên bố nguồn", "nop", ["dung lai du lieu"]),
    ("B17", "Đường ống sinh bảng và hình từ CSV vào bản thảo", "nop", ["sinh bang va hinh"]),
    ("B18", "Công cụ tính thống kê và nơi lưu kết quả kiểm định", "nop", ["cong cu tinh thong ke"]),
    ("B19", "SC8 thiếu trong lịch, và thứ tự bỏ bớt khi thiếu thời gian", "nop", ["sc8 khong co trong lich", "moc thoi gian tuyet doi"]),
    ("B20", "Trần quy mô cài đặt và cách khai khi đếm bão hoà", "nop", ["tran quy mo", "luong cuc dai va so"]),
    ("B21", "Ngưỡng chấp nhận cho tỉ lệ chứng nhận UB = LB", "nop", ["ub = lb", "chung nhan ub"]),
    ("B22", "SC7 hàng 4: thế nào là một \"lát cắt bất kỳ\"", "nop", ["lat cat bat ky"]),
    ("B23", "σ nào để đếm tác dụng phụ của SC4", "nop", ["tac dung phu cua sc4"]),
    ("B24", "Điều kiện gắn với đóng góp 1 và 2, phương án khi vỡ", "nop", ["dong gop 1 va 2"]),
    ("B25", "Phương án nếu nhóm [R6], [R7] đang làm cùng bài toán", "nop", ["[r6]"]),
    ("B26", "Bằng chứng FIFA [17,17,17] đang nằm ở results-probe", "nop", ["[17, 17, 17]", "fifa [17"]),
]

assigned = {}
leftovers = []
for it in items:
    hay = fold(it.get("title", "") + " || " + it.get("question", "") + " || " + it.get("sharpened", ""))
    hit = None
    for code, name, urg, keys in THEMES:
        if any(fold(k) in hay for k in keys):
            hit = code
            break
    if hit:
        assigned.setdefault(hit, []).append(it)
    else:
        leftovers.append(it)

lines = []
W = lines.append
W("# Những thứ còn phải quyết định")
W("")
W("Sinh tự động từ một đợt rà 10 nguồn của dự án: `config/preregistration.md`, `config/protocol.yaml`,")
W("`RUNBOOK.md`, bản thiết kế nghiên cứu ở bốn góc (cổng và rủi ro, lý thuyết, thiết kế thực nghiệm,")
W("khoảng trống), kết quả giai đoạn 1, mã nguồn (`TODO(code)` và `UnsupportedOperationException`),")
W("và bản thảo. Mỗi điểm đã được thẩm định lại bằng một lượt rà độc lập: mở tệp được dẫn, kiểm trích dẫn,")
W("và grep cả tài liệu lẫn mã nguồn xem chỗ khác đã quyết chưa. Một lượt phê bình rà cuối để tìm")
W("loại quyết định mà không ai chạm tới.")
W("")
W("%d điểm qua thẩm định, cộng %d điểm do phê bình bổ sung, gộp lại thành %d chủ đề dưới đây."
  % (len(d["confirmed"]), len(d["addedByCritic"]), len([c for c in THEMES if c[0] in assigned])))
W("")
W("**Cách đọc.** Nhóm A chặn SC3: chưa chốt thì mọi lần chạy SC3 phải kèm `--probe` và số không")
W("dùng được trong bài. Nhóm B chặn lúc nộp chứ không chặn chạy.")
W("")
W("---")
W("")

for urg, head in (("SC3", "## A. Chặn SC3 — phải chốt trước khi chạy so sánh chính"),
                  ("nop", "## B. Chặn lúc nộp — không chặn chạy")):
    W(head)
    W("")
    for code, name, u, _ in THEMES:
        if u != urg or code not in assigned:
            continue
        group = assigned[code]
        srcs = sorted({g.get("sourceKey", g.get("origin", "")) for g in group})
        W("### %s. %s" % (code, name))
        W("")
        W("Nguồn nêu: %s%s" % (", ".join(f"`{s}`" for s in srcs),
                               "  (có điểm do lượt phê bình bổ sung)" if any(g["origin"] == "phebinh" for g in group) else ""))
        W("")
        seen_q = set()
        for g in sorted(group, key=lambda x: -len(x.get("sharpened", "") or "")):
            q = " ".join((g.get("sharpened") or g.get("question") or "").split())
            key = q[:90].lower()
            if not q or key in seen_q:
                continue
            seen_q.add(key)
            W("**%s**" % g.get("title", "").strip())
            W("")
            W(q if len(q) <= 1400 else q[:1400] + " [...]")
            W("")
        blocks = sorted({" ".join(g.get("blocks", "").split()) for g in group if g.get("blocks")}, key=len)
        if blocks:
            W("> Chặn: %s" % (blocks[-1] if len(blocks[-1]) <= 500 else blocks[-1][:500] + " [...]"))
            W("")
    W("---")
    W("")

if leftovers:
    W("## C. Chưa xếp được vào chủ đề nào")
    W("")
    for it in leftovers:
        W("- **%s** (`%s`) — %s" % (it.get("title", ""), it.get("sourceKey", it.get("origin", "")),
                                    (it.get("sharpened") or it.get("question") or "")[:400]))
    W("")

io.open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")
print("da ghi", OUT)
print("chu de co noi dung:", len([c for c in THEMES if c[0] in assigned]), "/", len(THEMES))
print("con lai chua xep:", len(leftovers))
for it in leftovers:
    print("   -", it.get("title", "")[:70])
