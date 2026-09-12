# Những thứ còn phải quyết định

Sinh tự động từ một đợt rà 10 nguồn của dự án: `config/preregistration.md`, `config/protocol.yaml`,
`RUNBOOK.md`, bản thiết kế nghiên cứu ở bốn góc (cổng và rủi ro, lý thuyết, thiết kế thực nghiệm,
khoảng trống), kết quả giai đoạn 1, mã nguồn (`TODO(code)` và `UnsupportedOperationException`),
và bản thảo. Mỗi điểm đã được thẩm định lại bằng một lượt rà độc lập: mở tệp được dẫn, kiểm trích dẫn,
và grep cả tài liệu lẫn mã nguồn xem chỗ khác đã quyết chưa. Một lượt phê bình rà cuối để tìm
loại quyết định mà không ai chạm tới.

89 điểm qua thẩm định, cộng 12 điểm do phê bình bổ sung, gộp lại thành 29 chủ đề dưới đây.

**Cách đọc.** Nhóm A chặn SC3: chưa chốt thì mọi lần chạy SC3 phải kèm `--probe` và số không
dùng được trong bài. Nhóm B chặn lúc nộp chứ không chặn chạy.

---

## A. Chặn SC3 — phải chốt trước khi chạy so sánh chính

### A1. Ngưỡng hiệu ứng thực tiễn δ_L cho H2

Nguồn nêu: `design-experiment`, `design-gate`, `paper`, `phase1-findings`, `preregistration`, `runbook`

**Giá trị và đơn vị của ngưỡng thực tiễn δ_L**

Điền δ_L vào preregistration.md dòng 22 và quyết đơn vị. Phải chọn giữa: (i) hằng số tuyệt đối tính bằng số mẫu không nhạy cảm bị mất — đơn giản nhưng vô nghĩa ở đầu nhỏ và bất khả thi ở đầu lớn, vì quy mô thực thể trải năm bậc (min_masks từ 1 tới 87.107 trên bảng SC2); (ii) tỉ lệ theo |F_σ(D)| trước khi sửa, tức δ_L là phần trăm số mẫu phổ biến bị mất; (iii) tỉ lệ theo chính Lost của baseline, tức hiệu tương đối. Lưu ý ràng buộc: đơn vị lặp của H2 là (bộ, σ, tập nhạy cảm) nên δ_L phải là ngưỡng áp trên trung vị của hiệu qua các thực thể cỡ khác nhau — nếu chọn (i) thì phải nói rõ vì sao trộn được các thực thể đó vào một phép Wilcoxon. SC3 chạy sau khi điền; cột frequent_before/frequent_after của SC3 cho đúng mẫu số nếu chọn (ii). Người viết quyết.

**Ngưỡng hiệu ứng thực tiễn δ_L**

Dien BA con so vao preregistration.md, khong phai mot: (a) delta_L o dong 22, don vi = so mau khong nhay cam bi mat (so nguyen tuyet doi hay ti le tren |F_sigma(D) \ S+|? phai noi ro don vi truoc khi chon so); (b) bien tuong duong cho Lost o dong 40; (c) bien tuong duong cho SupDist o dong 41 (don vi khac han -- SupDist la ti so khong thu nguyen). Can cu kha di de chon delta_L: hoac lay tu bien do cua Lost tren chay do tim trong results-probe/SC3_*.csv, hoac neo vao M2 cua [R20]/[R3] (ti le mau pho bien bi mat) de so sanh duoc. AI QUYET: nguoi viet. GAP: truoc SC3 -- quy tac 2.4.7 cam chay SC3 lay ket qua khi chua chot; H2 khong kiem dinh duoc va moi ket luan 'khong khac biet' deu khong phat bieu duoc.

**Chốt ngưỡng hiệu ứng thực tiễn δ_L cho H2**

Chọn δ_L theo một trong hai dạng, và ghi căn cứ: (a) số tuyệt đối — δ_L = k mẫu không nhạy cảm bị mất (k nguyên, ví dụ 1, 2 hay 5), hoặc (b) tỉ lệ trên |F_σ(D) \ 𝒮⁺| tức cột frequent_before của SC3 (ví dụ 1% hay 2%), rồi quy về số nguyên cho từng thực thể. Lưu ý dạng (b) làm δ_L đổi theo thực thể nên Wilcoxon phải chạy trên hiệu đã trừ δ_L riêng từng thực thể. Không dùng Σ|S| hay |R|* làm mẫu số — Σ|S| là mẫu số của DistR, |R|* là cận số lần che. Căn cứ nên lấy từ quy mô thực đo: probe SC3 (results-probe/SC3_20260912-102131) có frequent_before = 84 và Lost của baseline trong khoảng 1–5, nên δ_L = 1 gần như chắc chắn quá lỏng còn δ_L = 10 sẽ làm H2 không thể bác bỏ.

**Câu kết quả thực nghiệm trong tóm tắt**

Câu kết quả duy nhất trong tóm tắt lấy đại lượng nào và trên kịch bản nào: (i) hiệu Lost giữa CUT-HIDE và baseline mạnh nhất ở cùng |R| và HF=0, đơn vị là số mẫu không nhạy cảm bị mất, lấy từ SC3 trên nhóm chính (dna_mt745584, msnbc); (ii) tỉ số |R|/|R|* của baseline tầng (b) khi đổi mục tiêu sang sup_NO, đơn vị là tỉ số không thứ nguyên, từ SC3; hay (iii) side-effects (%) và distance (%) so với HHA/DBSH/SBSH trên bài toán cũ, từ SC8. Lưu ý (i) chỉ viết được sau khi δ_L trong preregistration.md được điền và SC3 chạy không cờ --probe; (iii) hiện bị chặn vì SC8 chưa cài và trucks chưa có nguồn.

**Biên tương đương cho Lost và SupDist**

Điền hai biên tương đương ở preregistration.md §2 và quyết dạng của chúng. Lost: chọn biên tuyệt đối tính bằng số mẫu phổ biến bị mất, hay biên tỉ lệ theo |F_σ(D)|. SupDist: SupDist đã là đại lượng chuẩn hoá hay chưa cần kiểm trong Metrics trước khi quyết — nếu nó là tổng chênh lệch sup_NO thô thì biên tuyệt đối vô dụng vì sup_NO trải từ 100 (sign) tới 89.106 (msnbc), phải dùng biên tỉ lệ theo sup_NO của chính cấu hình. Hai biên này phải nhất quán với δ_L ở điểm [6]: không được δ_L theo tỉ lệ còn biên tương đương theo tuyệt đối. Chốt trước khi nhìn bất kỳ số SC3 nào. Người viết quyết.

**Chốt δ_L, ngưỡng hiệu ứng thực tiễn của H2**

Chọn δ_L là số nguyên tuyệt đối bao nhiêu mẫu không nhạy cảm bị mất (ví dụ δ_L = 1, 2 hay 5), HAY chuyển sang dạng tương đối (tỉ lệ trên |F_σ(D) \ 𝒮⁺|, vì |F_σ| của dna, msnbc, bms1 chênh nhau hàng chục lần nên một ngưỡng tuyệt đối không so được giữa các bộ)? Căn cứ sẵn có để neo giá trị: số dò tìm ở §7 dòng 45 (Lost trung bình 6,02 của HHA so với 1,95 của lát cắt có trọng số trên 60 CSDL tổng hợp, tức hiệu ~4). Người viết quyết, ghi vào preregistration.md dòng 22 kèm một câu căn cứ, rồi commit trước lần chạy SC3 đầu tiên không kèm --probe.

**Biên tương đương cho Lost**

Chốt biên tương đương cho Lost: ± bao nhiêu mẫu không nhạy cảm bị mất (cùng đơn vị đếm với Lost ở doc §2 dòng 61), và nói rõ quan hệ với δ_L của H2: đặt bằng δ_L (một con số duy nhất, dễ biện minh, nhưng khi đó 'hơn δ_L' và 'không khác quá δ_L' dùng chung một ranh giới nên mọi cặp sẽ rơi đúng một trong hai kết luận), hay đặt độc lập và nhỏ hơn δ_L (chặt hơn, nhưng phải nêu căn cứ riêng). Cần trước khi chạy SC3 lấy số vào bài, vì §2 cấm thay bằng 'p > 0,05' nên không có biên này thì mọi cặp so sánh hoà ở SC3 treo không kết luận được.

**Nội dung tệp preregistration.md**

Điền bốn ô null còn lại của config/preregistration.md rồi commit (git log làm bằng chứng thời điểm): δ_L với đơn vị là số mẫu không nhạy cảm bị mất trên một thực thể (bộ dữ liệu, σ, tập nhạy cảm), biên tương đương cho Lost, biên tương đương cho SupDist, và hai ngưỡng phân nhóm ρ_rep của H3 (trùng điểm [8]). Bộ phép kiểm không phải quyết, đã chốt ở §10. Người viết quyết; đây là chặn cứng của SC3.

> Chặn: Không chạy được SC3 để lấy số vào bài: giả thuyết H2 phát biểu là "trung vị hiệu lớn hơn δ_L", không có δ_L thì Wilcoxon một phía không có giả thuyết vô hiệu để kiểm. Theo chính tệp này, mọi lần chạy SC3 buộc phải kèm cờ --probe và kết quả rơi vào results-probe/, không dùng được trong bài.

### A2. Ngưỡng ρ_rep và β để phân nhóm: giá trị, đơn vị áp, và vùng ở giữa

Nguồn nêu: `design-experiment`, `design-gap`, `design-gate`, `design-theory`, `paper`, `phase1-findings`, `phebinh`, `preregistration`, `protocol`, `runbook`  (có điểm do lượt phê bình bổ sung)

**Ngưỡng phân nhóm ρ_rep và β cho đối chứng suy biến**

Chon hai so thuc trong [0,1] de dien vao preregistration.md dong 28-29: (a) nguong rho_rep tach nhom chinh; (b) nguong beta cho doi chung rang buoc. Pho rho_rep trung binh da do: dna 0,786 | msnbc 0,323 | leviathan 0,154 | bible 0,153 | bike 0,052 | fifa 0,008 | sign 0,000 | bms1 0,000. Moi lat cat trong khoang (0,052; 0,153) giu dung {dna, msnbc} lam nhom chinh; lat cat trong (0,008; 0,052) keo them leviathan/bible/bike vao. Phai chon mot con so, khong duoc de khoang trong giua nguong chinh va nguong suy bien (dong 28 dang co hai o rieng: neu dat khac nhau thi cac bo roi vao khe giua se khong co vai). AI QUYET: nguoi viet -- day la cam ket phuong phap luan, KHONG duoc suy nguoc tu mong muon bo nao vao nhom nao (quy tac 2.3.10). GAP: truoc SC3 -- moi lan chay SC3 hien phai kem --probe, bang H3 va muc 'chung minh dong gop' [2.2.4] treo lai cho den khi tep nay duoc dien va commit.

**Số phận SC8 khi không có bộ TRUCKS**

Quyết hình hài SC8 khi không có TRUCKS. Ba phương án: (i) chạy trên hai bộ MSN(=msnbc) và BMS-WebView-1 với đúng ngưỡng ψ của [R3], khai thẳng trong bài rằng TRUCKS không có nguồn công khai; (ii) thay TRUCKS bằng một bộ khác và khai là thay — bike đã bị chính khao_sat §4 xếp "không dùng" vì ρ_rep 0,052 và chuỗi trung bình 7,3, nên nếu chọn hướng này phải tìm bộ khác; (iii) bỏ SC8. Phương án (iii) đắt vì §12a đã đặt SC8 làm chân dự phòng cho toàn bài ("bài có thể đứng ngay cả khi bỏ phần bài toán mới: chỉ mục cắt Nettree và CUT-HIDE cải thiện được bài toán cũ"). Việc chặn trước cả ba: phải mở lại toàn văn [R3] và [R20] để chép đúng tên bản dữ liệu và giá trị ψ — tra cứu công khai đã thử và không dựng lại được. Và SC8 vẫn chưa cài (Sc8OriginalProblem.java là TODO), nên quyết định này chưa gấp bằng nhóm ngưỡng, nhưng phải xong trước khi viết phần thực nghiệm.

**H3 trên nhóm suy biến là kiểm định hay đồng nhất thức**

Quyết cách phát biểu nửa sau của H3 ("tỉ số |R|/|R|* gần 1 trên nhóm suy biến"). Hai lựa chọn: (i) giữ nguyên như một giả thuyết được kiểm định — phải chấp nhận rằng trên sign và bms1 tỉ số bằng 1 do định nghĩa (sup_NO ≡ sup_seq ở đúng 40/40 dòng), nên phản biện chỉ ra được là vòng quanh; (ii) tách làm hai: nửa đầu (tỉ số > 1 trên nhóm ρ_rep cao) giữ là giả thuyết, nửa sau hạ thành PHÉP KIỂM CÀI ĐẶT đặt cạnh SC1, báo cáo một lần và không đưa vào bảng so sánh theo quy tắc 2.4.2. Kèm theo phải quyết ngay: nửa sau có được đếm là một phép kiểm trong hiệu chỉnh Holm hay không — nếu đếm mà nó là đồng nhất thức thì số phép kiểm bị thổi lên và p hiệu chỉnh sai. Lưu ý không gộp fifa (ρ_rep 0,005–0,013) và bible/leviathan (0,15) vào lập luận đồng nhất thức: chỉ sign và bms1 mới đúng bằng 0. Người viết quyết, trước khi chạy SC3 vì nó đổi số phép kiểm.

**Lưới σ và độ rộng khoảng cách đưa vào bài**

Dien protocol.yaml dong_1_theo_bo cho tung bo thuc su dung: sigma (ca tuyet doi lan ti le theo tong sup_NO, theo ghi_chu dong 22-24), l_max (mac dinh ma = 3, xac nhan hay doi), [g-, g+], va |S| (mac dinh ma [1,5,10,20], 20 tap moi o). Hai cau hoi con phai tra loi truoc do: (a) don vi lap cua H2 la mot bo ba (bo, sigma, tap nhay cam) -- vay bao nhieu cau hinh moi bo vao bang chinh? msnbc dang co 5, dna 3, cac bo khac 1-2, chenh lech nay lam lech trong so Wilcoxon; (b) khoang cach: SC2 cho thay rho_rep va beta keo nguoc nhau ([0,1] cho beta cao, [0,10] cho rho_rep cao, [0,3] can bang) -- chot mot khoang cach chung cho moi bo hay bao cao ca luoi? AI QUYET: nguoi viet, dua tren bang SC2 da co (khong can chay them de quyet, chi can chay them de lam day o). GAP: truoc SC3, vi phan nhom o diem [0] chot theo cau hinh chu khong theo bo.

**Bộ dữ liệu thực nghiệm mới là ứng viên**

Ba việc rời nhau, đừng gộp. (1) Điền hai ngưỡng phân nhóm vào preregistration.md trước khi chạy SC3: ρ_rep ≥ ? là nhóm chính và ρ_rep ≤ ? là đối chứng suy biến (dải đo được: dna 0,786; msnbc 0,323; leviathan 0,154; bible 0,153; bike 0,052; fifa 0,008; sign và bms1 đúng 0,000 — nên ngưỡng phải rơi vào khe 0,008–0,153 hoặc 0,154–0,323 và không được suy ngược từ mong muốn bộ nào vào nhóm nào), cộng ngưỡng β cho nhóm đối chứng ràng buộc. (2) Chốt bốn tham số sinh dữ liệu tổng hợp trong protocol.yaml dòng 4: |Σ|, số chuỗi, độ dài chuỗi, và dải ρ_rep/β mục tiêu — nhớ chỉ báo cáo giá trị ĐO ĐƯỢC. (3) Với trucks: chấp nhận chạy SC8 trên hai bộ (MSN, BMS-WebView-1) và khai rõ không lấy được, hay thay bằng bike, hay xin dữ liệu từ tác giả [R20]? Lưu ý bike đã bị chính bảng đo loại (ρ_rep 0,052, độ dài trung bình 7,3).

**Ngưỡng ρ_rep để một bộ dữ liệu vào nhóm chứng minh đóng góp**

Đặt hai con số cho config/preregistration.md trước khi nhìn bất kỳ kết quả SC3 nào: ngưỡng ρ_rep_cao để một cấu hình vào nhóm chứng minh đóng góp, và ngưỡng ρ_rep_thap để vào nhóm đối chứng suy biến (đơn vị: tỉ lệ không thứ nguyên trong [0,1], lấy theo trung bình ρ_rep của các mẫu nhạy cảm đã lấy ở SC2). Số đo đã có nên lựa chọn là hữu hạn và rõ: một ngưỡng trên trong khoảng (0,154; 0,323) tách {dna, msnbc} khỏi {leviathan, bible}; ngưỡng dưới quanh 0,05 gộp {fifa, sign, bms1, bike} vào nhóm suy biến; khoảng giữa quyết định leviathan và bible là nhóm trung gian hay bị loại. Kèm ngưỡng β tương ứng. Người viết quyết (không được suy ngược từ mong muốn bộ nào vào nhóm nào — chính khao_sat_bo_du_lieu.md mục 4 nhắc điều này); TRUCKS không còn trong danh sách vì không lấy được nguồn.

**Ngưỡng ρ_rep phân nhóm chính và nhóm suy biến**

Điền hai con số vào preregistration.md dòng 28: ρ_rep ≥ a vào nhóm chính, ρ_rep ≤ b vào nhóm đối chứng suy biến (a, b là số thực không thứ nguyên trong [0,1]). Dải đo được nay liên tục chứ không tách cụm: bms1 và sign 0,000; fifa 0,008; bible 0,153 và leviathan 0,154; msnbc 0,323; dna_mt745584 0,786. Phải chọn giữa ba kiểu cắt: (i) a = b (không vùng xám, mọi cấu hình đều bị gán một nhóm — khi đó cắt ở đâu quyết định leviathan/bible vào nhóm nào); (ii) a > b có vùng xám, và phải nói rõ cấu hình rơi vào vùng xám thì bị loại khỏi cả hai nhóm hay vào nhóm thứ ba "trung gian" như khao_sat_bo_du_lieu.md §4 đề nghị; (iii) ngưỡng theo phân vị của chính bảng SC2 thay vì số tuyệt đối. Kèm căn cứ, và căn cứ không được là "để bộ X vào nhóm Y". Người viết quyết.

**Ngưỡng ρ_rep và β để xếp nhóm suy biến**

Điền ba con số vào preregistration.md mục 1 rồi commit (git log làm bằng chứng thời điểm) trước lần chạy SC3 đầu tiên: (a) ngưỡng ρ_rep tối thiểu để vào nhóm chính; (b) ngưỡng ρ_rep tối đa để xếp vào nhóm đối chứng suy biến; (c) ngưỡng β tối đa cho nhóm đối chứng ràng buộc. Đơn vị: tỉ lệ không thứ nguyên trong [0,1], ρ_rep = 1 − sup_seq/sup_NO và β = 1 − sup_NO(có khoảng cách)/sup_NO([0,∞)). Bảng SC2 đo được cho sẵn một khe tự nhiên ở ρ_rep giữa 0,154 (leviathan) và 0,323 (msnbc), và β của bms1 là 0,094 — nhưng theo quy tắc 2.3.10 không được suy ngược ngưỡng từ mong muốn bộ nào vào nhóm nào, nên phải viết căn cứ trước. Người viết quyết. GẤP: chặn H3, chặn việc phân nhóm SC2 → SC3, và chừng nào chưa chốt thì mọi lần chạy SC3 phải kèm cờ --probe.

**Ngưỡng β cho nhóm đối chứng ràng buộc**

Điền số vào preregistration.md dòng 29: β ≤ b_0 (số thực không thứ nguyên) thì cấu hình là đối chứng ràng buộc vô hiệu, và nêu cấu hình nào đóng vai đó. Dải β đo được: bms1 tb 0,094 (nhiều mẫu bằng 0), msnbc [0,10] σ=2000 tb 0,043, sign tb 0,209, fifa tb 0,289, dna tb 0,467, bible tb 0,500, leviathan tb 0,612. Kèm theo phải chốt luật ưu tiên khi một cấu hình vừa đạt ngưỡng nhóm chính theo ρ_rep vừa đạt ngưỡng đối chứng theo β — msnbc [0,10] σ=2000 là đúng ca đó (ρ_rep 0,415 cao, β 0,043 thấp): nó vào nhóm chính, vào đối chứng ràng buộc, vào cả hai, hay bị loại. Chọn giữa hai kiểu đối chứng: đối chứng tự nhiên (bms1, có sẵn β thấp lẫn ρ_rep = 0) hay đối chứng dựng bằng cách nới khoảng cách trên chính bộ nhóm chính. Người viết quyết.

**Ngưỡng phân nhóm ρ_rep và β cho H3**

Điền ba con số vào preregistration.md dòng 28–29, chọn trong khoảng trống mà bảng SC2 đã để lộ sẵn: ρ_rep đo được là 0,786 (dna), 0,323 (msnbc), 0,154/0,153 (leviathan/bible), 0,052 (bike), 0,008 (fifa), 0,000 (sign, bms1). Vậy phải chọn ngưỡng nhóm chính (ứng viên tự nhiên: ρ_rep ≥ 0,30 giữ cả dna và msnbc; hay ≥ 0,20 để kéo thêm?) và ngưỡng suy biến (≤ 0,05 hay ≤ 0,01?), đồng thời quyết nhóm trung gian leviathan/bible thuộc bên nào hay đứng riêng. Với β ≤ _null_ của 'nhóm đối chứng ràng buộc' còn phải nói rõ β thấp hay β cao mới là đối chứng (β đo được: bms1 0,094 thấp nhất, bible 0,500 và leviathan 0,612 cao nhất) — hiện phát biểu 'β ≤ null' mâu thuẫn với việc β thấp nghĩa là ràng buộc MẤT hiệu lực.

**Mẫu lặp mục: đóng câu hỏi mở hay thu hẹp phạm vi**

Xác nhận và ghi thành văn phạm vi mẫu nhạy cảm, chọn một: (A) chốt μ(P) = 1 làm phạm vi chính — đúng như Sc3MainComparison.sampleSensitiveSets đang lọc — rồi khai bộ lọc này ở §11.5 và protocol.yaml dòng 1, và viết ở phần hạn chế đúng con số ρ_rep 0,218 so với 0,434 của MSNBC cùng 0,806 so với 0,773 của DNA; hoặc (B) mở phạm vi sang μ(P) ≥ 2, khi đó phải nói CUT-HIDE nhắm cận nào của P3 (⌈(F−t)/μ⌉ hay F−t) làm mục tiêu che, khai sai số theo hệ số μ(P), bỏ bộ lọc hasDistinctItems và dùng IlpExact đo khoảng cách tới tối ưu trên thực thể nhỏ. Người viết quyết. GẤP: trước SC3, vì nó định nghĩa quần thể lấy mẫu 𝒮 và cách trả lời vế 'độ khó thay đổi thế nào khi mẫu lặp mục' của RQ1.

**Đơn vị áp ngưỡng: theo bộ, theo cấu hình hay theo mẫu**

Sửa preregistration.md dòng 59 cho khớp §8.4 của bản thiết kế, vốn đã chốt đơn vị phân nhóm là CẤU HÌNH chứ không phải bộ. Phần còn thực sự mở là định nghĩa chính xác trục của cấu hình: §8.4 ghi (D, 𝒮, σ) nhưng SC2 quét (bộ, σ, [g⁻,g⁺]) và độ rộng khoảng cách đổi ρ_rep trung bình tới 30% tương đối trên chính msnbc σ=2000. Chọn một trong hai: cấu hình = (bộ, σ, [g⁻,g⁺]) và ρ_rep của cấu hình = trung bình trên các mẫu đã lấy; hay cấu hình = (bộ, σ, [g⁻,g⁺], tập nhạy cảm) tức phân nhóm ở mức đơn vị lặp của H2. Ràng buộc cứng: đơn vị phân nhóm của H3 phải trùng hoặc lồng khớp với đơn vị lặp của Wilcoxon ở H2, mà §11.6 đã chốt là (bộ dữ liệu, σ, tập nhạy cảm). Người viết quyết.

**Bằng chứng FIFA [17, 17, 17] nằm ở results-probe**

Quyết số phận một câu ở giai_doan_1_tom_tat.md dòng 85–86. Hai lựa chọn: (i) chạy lại cấu hình FIFA [0,3] σ=3000 không kèm --probe với dải lấy mẫu đủ rộng để mẫu [17, 17, 17] rơi vào results-invariant/, rồi giữ câu; (ii) xoá câu. Phương án (ii) nay rẻ hơn hẳn vì khối cập nhật cùng ngày ở §3 đã thay bằng chứng trụ cột bằng bộ DNA (ρ_rep mẫu không lặp mục 0,806 so với mẫu lặp mục 0,773), nên §3 không còn cần con số FIFA. Chú ý mẫu đó là mẫu lặp mục (min_masks_exact=false), tức nằm ngoài phạm vi μ(P)=1 đã chốt trong mã, nên kể cả chạy lại nó cũng không vào được SC3. Không chặn SC3; phải xử trước khi bất kỳ đoạn nào của §3 được chép vào bản thảo. Người viết quyết.

**Ngưỡng β cho nhóm đối chứng ràng buộc**

Chốt một số thực trong [0,1]: β ≤ ? thì thực thể là đối chứng ràng buộc (ràng buộc khoảng cách mất hiệu lực), kèm đơn vị đo — trung bình β trên các mẫu nhạy cảm của cấu hình hay giá trị nhỏ nhất. Số đã đo giới hạn lựa chọn: bms1 β 0,094 (đề nghị suy biến kép), msnbc ở [0,10] tụt còn 0,043 trong khi ở [0,1] là 0,432, bible 0,500, leviathan 0,612. Lưu ý hệ quả: vì β và ρ_rep kéo ngược nhau theo độ rộng khoảng cách (giai_doan_1_tom_tat.md:62-63), một ngưỡng β chặt sẽ tự động loại các cấu hình khoảng cách rộng vốn có ρ_rep cao — nên phải chọn ngưỡng β đồng thời với việc chốt cấu hình [g⁻,g⁺] nào dùng cho nhóm chính, và ghi lý do vì sao chọn cấu hình đó.

**Chốt σ và độ rộng khoảng cách cho từng bộ**

Điền dòng 1 của protocol.yaml (sigma, l_max, gap, sensitive_size) cho từng bộ sẽ chạy SC3–SC5, SC7, SC8. Với msnbc phải chọn σ ∈ {1000, 2000, 5000} — chọn khác nhau đổi ρ_rep trung bình từ 0,190 lên 0,395, tức đổi hẳn độ thuyết phục — và [g⁻,g⁺] ∈ {[0,1], [0,3], [0,10]}. Với dna_mt745584 hiện chỉ có 3 cấu hình đo được (σ 1000 và 2500, gap [0,1] và [0,3]); phải quyết chạy thêm SC2 trước rồi mới chốt, hay chốt ngay theo khuyến nghị [0,1] của khao_sat §3(b). Ràng buộc: mọi baseline phải so ở cùng cấu hình, và σ phải khai cả tuyệt đối lẫn tỉ lệ theo Σ sup_NO (§11.5). Người viết quyết, sau khi có thêm số quét σ cho dna/leviathan/bible.

**Ngưỡng ρ_rep để một cấu hình vào nhóm chính**

Chốt một số thực trong [0,1]: ρ_rep ≥ ? thì thực thể (bộ, σ, khoảng cách) vào nhóm chính. Đồng thời chốt đơn vị đo: lấy trung bình ρ_rep của các mẫu nhạy cảm trong cấu hình đó, hay đòi TỪNG mẫu nhạy cảm đều đạt ngưỡng (tức lấy giá trị nhỏ nhất). Số liệu SC2 đã đo ràng buộc lựa chọn: ngưỡng > 0,323 loại msnbc và chỉ còn dna (0,786) trong nhóm chính; ngưỡng ≤ 0,15 kéo cả leviathan/bible vào. Nếu chọn theo giá trị nhỏ nhất thì msnbc khó qua vì phân vị thấp của nó dưới 0,218. Phải điền và commit trước lần chạy SC3 lấy số vào bài; nhìn kết quả SC3 rồi mới chọn là vi phạm quy tắc 2.3.10.

**Ngưỡng ρ_rep của nhóm đối chứng suy biến**

Chốt hai thứ cùng lúc: (a) ρ_rep ≤ ? là đối chứng suy biến (số liệu đã đo: sign và bms1 đúng bằng 0,000; fifa 0,008; bike 0,052 — mọi ngưỡng trong khoảng 0,01–0,05 đều gom đúng ba bộ đầu); (b) thực thể rơi vào khoảng giữa hai ngưỡng thì xử lý ra sao — chọn một: loại hẳn khỏi bảng H3, hay lập nhóm thứ ba 'trung gian' như khao_sat_bo_du_lieu.md:81 đề nghị và khai rõ nhóm này không dùng để kiểm H3 mà chỉ minh hoạ ρ_rep và β độc lập nhau. Nếu chọn (b) thì phải sửa §1 của preregistration.md để có ba nhóm chứ không phải hai.

**Vai của từng bộ dữ liệu sau khi đo rho_rep**

Điền và commit vào config/preregistration.md mục H3 ba số: ngưỡng ρ_rep trung bình để vào nhóm chính, ngưỡng ρ_rep để vào nhóm đối chứng suy biến, và ngưỡng β cho nhóm ràng buộc-có-hiệu-lực; bảng SC2 tách rất rõ (0,000 / 0,008 / 0,052 so với 0,154 / 0,323 / 0,786) nên một lát cắt quanh 0,10 là suy được từ dữ liệu. Kèm theo: chốt vai của cả tám bộ đã đo và ghi dứt khoát rằng TRUCKS bị loại khỏi SC8 vì không có nguồn công khai, hoặc nêu nguồn thay thế. Suy ra từ dữ liệu SC2 đã có; phải chốt và commit TRƯỚC khi chạy SC3.

**Câu hỏi mở P3 khi mẫu lặp mục (mu >= 2)**

Quyết một dòng cho config/preregistration.md và cho mã: tập nhạy cảm lấy mẫu ở SC3/SC4 có lọc về μ(P)=1 hay không. Nếu lọc, phải thêm bộ lọc vào Sc3MainComparison.sampleSensitiveSets và khai trong phần hạn chế kèm đúng số đã đo (MSNBC: ρ_rep trung bình 0,218 cho μ=1 so với 0,434 cho μ≥2; DNA: 0,806 so với 0,773). Nếu không lọc thì phải cài xong nhánh cận P3 và ILP chính xác để đo khoảng cách tới tối ưu cho lớp μ≥2. Người viết quyết. Gấp trước SC3 vì nó đổi đơn vị lặp và đổi toàn bộ bảng kết quả.

**Chốt trạng thái của khoảng trống G1 và G5**

Sau khi có toàn văn [R11], hoặc sau khi kích hoạt mức đọc rút gọn (§12a mục 3), quyết ba việc trong §3: G1 chuyển 'mạnh, tạm' sang 'mạnh, chính thức' hay hạ xuống 'yếu'; G5 giữ 'tạm' hay đóng lại theo đúng phạm vi rà của §4; và phát biểu lại G1 theo cơ chế (không dùng câu phủ định tuyệt đối, theo ràng buộc (b) của §12a mục 3), kèm số ρ_rep đã đo. Cần đọc tài liệu gốc. Hạn: trước khi viết phần tuyên bố khoảng trống của bản thảo.

**l_max va khoang cach [g-, g+] tung bo (dong 1)**

Hai câu hỏi tách nhau. (a) ℓ_max: giữ 3 (và sửa §11.5 'độ dài 2-4' thành 2-3 cho khớp), hay nâng lên 4 và chấp nhận chi phí khai phá F_σ tăng trên msnbc/fifa? (b) Với mỗi bộ nhóm chính, chốt [g⁻,g⁺] dùng cho SC3: [0,1] (β cao ~0,43 nhưng ρ_rep thấp hơn) hay [0,3] (cân bằng), theo đúng số đo ở giai_doan_1_tom_tat.md. Người viết chốt trước SC3; đổi sau là phải chạy lại toàn bộ SC3.

**Muc tieu rho_rep va beta khi sinh du lieu**

Chốt lưới mục tiêu khi sinh: mấy mức ρ_rep (ví dụ {0; 0,3; 0,8}) nhân mấy mức β (ví dụ {0; 0,3; 0,6}), mỗi ô bao nhiêu bản sinh, và quy tắc dò plantedPerSequence/spacing để tiệm cận mục tiêu. Người viết chốt trước SC6; bài vẫn chỉ in giá trị đo được, mục tiêu chỉ nằm ở dòng 4 bảng giao thức.

**Bộ chuỗi thời gian rời rạc hoá của dòng NOSEP**

Quyết dứt điểm: (a) lấy một bộ chuỗi thời gian rời rạc hoá để phủ nốt loại dữ liệu thứ ba mà [R1] dùng — khi đó phải chốt kho và bộ cụ thể (UCR: bộ nào), phương pháp rời rạc hoá (SAX hay chia khoảng đều), số ký hiệu của bảng chữ cái và cỡ cửa sổ, rồi chạy SC2 để đo ρ_rep và β trước khi gán vai; hay (b) khai rõ trong bài rằng phạm vi dữ liệu chỉ gồm chuỗi sự kiện có sẵn dạng rời rạc (nhấp chuột, gen, văn bản, giao dịch) và [R1] dùng thêm chuỗi thời gian là khác biệt về phạm vi đánh giá, không phải thiếu sót. Lưu ý nếu chọn (a) thì tham số rời rạc hoá trở thành một dòng mới của bảng giao thức, vì ρ_rep phụ thuộc trực tiếp vào số ký hiệu.

> Chặn: Hai trong ba loại dữ liệu của [R1] đã có (DNA, và BMS1 chính là Gazelle), loại thứ ba chưa. Phản biện dòng NOSEP hỏi ngay vì sao thiếu. Chọn (a) muộn thì phải chạy lại cả SC2 lẫn SC3 cho bộ mới sau khi đã chốt phân nhóm, mà quy tắc 2.3.10 cấm sửa phân nhóm sau khi nhìn kết quả SC3.

### A4. Biên tương đương cho Lost và SupDist

Nguồn nêu: `design-experiment`, `preregistration`

**Phép kiểm và tiêu chí định lượng "gần 1" của H3**

Khai đủ bốn thứ cho H3 như đã khai cho H2: (a) phép kiểm cho vế 'lớn hơn 1 trên nhóm chính' — Wilcoxon dấu hạng một phía trên tỉ số |R|/|R|* với giả thuyết vô hiệu trung vị = 1, hay kiểm trên log tỉ số; (b) mức α (0,05 cho đồng bộ với H2?) và có gộp H3 vào cùng họ Holm với H2 hay tách họ riêng; (c) cỡ hiệu ứng báo cáo (rank-biserial cho đồng bộ?); (d) biên tương đương định lượng 'gần 1' trên nhóm suy biến — tỉ số nằm trong [1, 1+?] thì coi là gần 1, ví dụ ±0,05 hay ±0,10. Đồng thời khai phạm vi: vì Sc3MainComparison.boundFromP1 chỉ tính |R|* cho tập nhạy cảm một mẫu không lặp mục (mọi trường hợp khác cột mask_ratio trống), H3 chỉ kiểm trên lớp thực thể đó — phải nói rõ trong phát biểu H3 chứ không để ngầm.

**Biên tương đương cho SupDist**

Chốt một số thực: biên tương đương cho SupDist là ± bao nhiêu, tính trên thang đã định sẵn của doc §2 dòng 65 — tỉ lệ không thứ nguyên trong [0,1] (Metrics.Row.supDist), KHÔNG phải chênh lệch sup_NO tuyệt đối và KHÔNG chuẩn hoá theo σ; đừng mở lại câu hỏi thang đo. Số đo probe SC3 cho thấy bậc độ lớn cần nhắm: sup_dist của các phương pháp nằm quanh 0,1345–0,1481, tức chênh lệch giữa các phương pháp cỡ 0,01. Biên ± 0,05 sẽ tuyên bố mọi thứ là tương đương; biên ± 0,001 sẽ không bao giờ kết luận được tương đương.

**Phép kiểm cho H3 và nghĩa của "gần 1"**

H3 kiểm bằng gì: (a) kiểm một mẫu trên tỉ số |R|/|R|* của nhóm chính với H₀: trung vị ≤ 1 (Wilcoxon dấu hạng một phía, cùng đơn vị lặp (bộ, σ, tập nhạy cảm) như H2), cộng (b) một kiểm TƯƠNG ĐƯƠNG hai một phía (TOST) trên nhóm suy biến với biên tương đương phải chốt (ví dụ |R|/|R|* ∈ [1; 1,05] mới gọi là 'gần 1')? Hay chỉ báo cáo khoảng tin cậy không kiểm? Phải chốt: phép kiểm, đơn vị lặp, biên định nghĩa 'gần 1', và có đưa H3 vào hiệu chỉnh Holm chung với H2 hay tách. Ghi vào preregistration.md §1 mục H3.

> Chặn: Không kiểm định được H3. Mục H2 khai đủ bốn thứ (phép kiểm Wilcoxon một phía, hiệu chỉnh Holm, cỡ hiệu ứng rank-biserial, α = 0,05) còn mục H3 chỉ khai ngưỡng phân nhóm, không khai phép kiểm. §2 lại chỉ chốt biên tương đương cho Lost và SupDist, không có biên cho tỉ số |R|/|R|*, nên vế "gần 1 trên nhóm suy biến" hiện không có tiêu chí nào để kết luận đúng hay sai.

### A6. Họ baseline đưa vào hiệu chỉnh Holm

Nguồn nêu: `design-experiment`, `phebinh`, `preregistration`  (có điểm do lượt phê bình bổ sung)

**Họ baseline đưa vào hiệu chỉnh Holm**

Liệt kê đích danh tên các baseline trong họ so sánh của H2 (chọn từ 14 tên mà Sc3MainComparison.create nhận: occurrence, HH, HR, RH, RR ở tầng (a); hha, dbsh, sbsh-r1, sbsh-rmax ở tầng (b); ilp, ilp-lost, ga ở tầng (c)), và từ đó ghi thẳng số m dùng cho Holm. Chốt thêm: họ tính RIÊNG cho từng chỉ số (Lost một họ, SupDist một họ, |R| một họ) hay GỘP chung mọi chỉ số vào một họ — gộp làm m tăng gấp ba và làm H2 khó bác bỏ hơn nhiều. Ghi rõ baseline nào chưa cài xong nhưng vẫn nằm trong họ, để m không co lại khi đến lúc chạy. Phải chốt trước SC3: mặc định CLI hiện tại (5 phương pháp, không có tầng (b) nào) không thay được bản chốt, và để quyết sau khi nhìn kết quả là chọn lọc báo cáo.

**Thiếu H1; nhãn H1, H2 mang hai nghĩa**

Hai quyết định tách rời: (1) có thêm giả thuyết cho RQ4 (số mẫu ma của xoá khi g⁺ hữu hạn, SC5) và RQ5 (chi phí của HIDE-SEQ, SC4) hay khai rõ trong bài rằng RQ1/RQ4/RQ5 trả lời bằng mệnh đề và mô tả, không bằng kiểm định — nếu thêm thì phải chốt phép kiểm và ngưỡng trong preregistration cùng lúc với H2; (2) chọn sơ đồ đặt tên để hai heuristic của P6(c) không trùng nhãn giả thuyết — ví dụ đổi heuristic thành A1/A2 hoặc HEU-1/HEU-2 (sửa doc dòng 274, 212, 305, 403 và Sc7DesignVariants.java:11,23, WorkedExamplesTest.java:96), hoặc đánh số lại giả thuyết. Phải làm trước khi chốt preregistration vì Holm 'theo số baseline' và số giả thuyết phụ thuộc quyết định (1).

**Baseline [R11] chốt sau khi có toàn văn**

Đặt một hạn chót cho ba bước lấy toàn văn ở §12a (CESTI/thư viện trường; thư cho Meng Zhang; ResearchGate). Hết hạn mà chưa có: kích hoạt sẵn nhánh đã viết ở §12a mục 3 — [R11] KHÔNG vào tầng (b), doc §11.3 dòng 429 bị xoá khỏi bảng baseline và [R11] chỉ còn ở phần công trình liên quan. Việc này phải xong trước khi chốt preregistration, vì Holm 'hiệu chỉnh theo số baseline' (doc dòng 382) đòi danh sách baseline đóng trước khi chạy SC3. Nếu có toàn văn thì quyết thêm: chuyển sang sup_NO bằng cách nào và có áp được cho r > 1 hay không.

**Công cụ tính thống kê và nơi lưu kết quả kiểm định**

Chốt ai tính Wilcoxon, Holm, rank-biserial và khoảng tin cậy: (a) thêm một script Python dùng scipy (kho hiện đã có numpy và scipy trong probe/, nhưng chưa có tệp thống kê nào và chưa khai phiên bản), (b) viết bằng R kèm tệp renv, hay (c) tự cài trong Java để giữ lời hứa 'không phụ thuộc thư viện ngoài'. Kèm hai thứ phải quyết cùng lúc: tệp đầu ra của phép kiểm (p, p sau Holm, cỡ hiệu ứng, khoảng tin cậy, số cặp) nằm ở thư mục nào trong ba thư mục kết quả và mang dấu định danh lần chạy nào — hiện ResultSink chỉ mở tệp cho kịch bản SCn, không có đường cho tệp dẫn xuất; và phiên bản công cụ khai ở dòng nào của bảng giao thức (dòng 5 chỉ có JDK).

> Chặn: Mọi con số p của bài hiện chưa có công cụ nào sinh ra, và không có chỗ hợp lệ để lưu. Kết quả kiểm định tính bằng tay trên máy rồi chép vào bài là loại số không truy lại được, trái với quy tắc 2.6.8 mà cả kho đang cưỡng chế cho tệp CSV.

### A7. Ghép cặp H2: cùng ngân sách |R| hay để mỗi phương pháp tự chọn

Nguồn nêu: `phebinh`  (có điểm do lượt phê bình bổ sung)

**So Lost ở cùng ngân sách |R| hay để mỗi phương pháp tự chọn**

Chốt cách ghép cặp của H2: (a) mỗi phương pháp chạy tới khi HF = 0 với số lần che TỰ CHỌN, rồi so hiệu Lost thô — khi đó phải báo cáo kèm |R| và thừa nhận rằng một phương pháp che nhiều hơn thì đương nhiên mất nhiều mẫu hơn, tức hiệu Lost lẫn cả hai nguyên nhân; (b) ép mọi phương pháp về CÙNG |R| (lấy |R| của CUT-HIDE, hoặc |R|* của P1 khi có) rồi mới so Lost — khi đó phải thêm tham số ngân sách che vào giao diện HidingMethod và định nghĩa việc gì xảy ra khi baseline không ẩn hết ở ngân sách đó; hay (c) không so điểm mà so ĐƯỜNG đánh đổi Lost theo |R|, khi đó phải chốt lưới ngân sách và cách tóm tắt đường thành một số để đưa vào Wilcoxon. Bảng SC3 ở §11.1 hứa 'đường đánh đổi' nhưng mã chỉ chạy mỗi phương pháp đúng một lần, không có nút vặn ngân sách.

**Xử lý thực thể hỏng trong phép kiểm ghép cặp**

Khai trước quy tắc loại trừ cho phân tích H2/H3, ba ca rời nhau: (1) hàng baseline có HF > 0 (không ẩn hết) — bỏ cả cặp khỏi Wilcoxon, hay giữ và gán Lost một giá trị phạt đã định, hay báo cáo riêng thành tỉ lệ thất bại; (2) hàng mang mã OT hoặc OOM — cùng câu hỏi, và phải nói rõ vì bỏ ô chậm nhất là thiên vị có lợi cho phương pháp chậm; (3) hàng có cột mask_ratio trống (|S| > 1 hoặc mẫu lặp mục, nơi |R|* không định nghĩa) — H3 chạy trên tập con nào. Kèm quy tắc báo cáo: bảng phải in số cặp đưa vào kiểm và số cặp bị loại theo từng lý do.

> Chặn: H2 hiện phát biểu trên hiệu Lost mà không kiểm soát |R|, nên một kết quả thắng có thể chỉ phản ánh việc CUT-HIDE che ít lần hơn chứ không phải chọn vị trí khéo hơn; phản biện bẻ được ngay. Không chốt thì không viết được chú thích bảng SC3 và không dựng được 'đường đánh đổi' mà chính §11.1 hứa.

### A8. Quy ước tính Wilcoxon: hiệu bằng 0, hạng buộc, cỡ hiệu ứng

Nguồn nêu: `phebinh`  (có điểm do lượt phê bình bổ sung)

**Số lần chạy lại của các phương pháp ngẫu nhiên**

Chốt số lần chạy lại và cách tổng hợp cho các phương pháp NGẪU NHIÊN khi đo các chỉ số BẤT BIẾN theo máy (Lost, Ghost, SupDist, |R|), tức HH, HR, RH, RR và GA: mỗi thực thể chạy 1 lần với hạt giống 2026 như mã đang làm, hay chạy k lần với k hạt giống khác nhau (k = ?) rồi lấy trung bình, trung vị, hay lần chạy tốt nhất. Nếu lấy nhiều lần thì phải quyết đơn vị lặp của Wilcoxon vẫn là (bộ, σ, tập nhạy cảm) với giá trị đã tổng hợp, chứ không được đếm mỗi lần chạy là một quan sát độc lập. Dòng 8 của bảng giao thức hiện chỉ quy định số lần lặp cho ĐO THỜI GIAN, không phủ ca này.

**Quy ước tính Wilcoxon: hiệu bằng 0, hạng buộc, cỡ hiệu ứng**

Khai ba quy ước tính toán mà tệp chốt trước đang bỏ trống: (a) các thực thể có hiệu ĐÚNG BẰNG 0 (rất dễ xảy ra vì Lost là số nguyên nhỏ, probe SC3 cho Lost trong khoảng 1–5) xử lý theo Wilcoxon cổ điển (loại bỏ, giảm n) hay theo Pratt (giữ và cho hạng) — hai cách cho p khác nhau; (b) tính p CHÍNH XÁC (phân phối hoán vị, khả thi khi n nhỏ) hay xấp xỉ chuẩn có hiệu chỉnh liên tục và hiệu chỉnh hạng buộc; (c) công thức rank-biserial dùng bản nào (r = (W+ − W−)/(W+ + W−) hay 1 − 2W−/(n(n+1)/2)) và tính trên n trước hay sau khi bỏ các hiệu bằng 0.

> Chặn: Đối chứng tầm thường tầng (a) và GA là ngẫu nhiên, nên một lần chạy duy nhất cho một con số may rủi; kết luận 'CUT-HIDE hơn RR' có thể lật khi đổi hạt giống. Không chốt trước thì không biện minh được vì sao lấy đúng hạt giống 2026, và chạy thêm sau khi thấy kết quả là chọn lọc báo cáo.

### A11. Lưới σ, ℓ_max, khoảng cách và |𝒮| cho từng bộ

Nguồn nêu: `code-todo`, `design-experiment`, `paper`, `protocol`

**Sigma nao de dem tac dung phu cua SC4**

Chot mot cau cho SC4: sigma dinh nghia F_sigma(D) de dem Lost/Ghost/SupDist duoc giu CO DINH cho ca bon muc r in {1,2,3,5} (va bang dung sigma cua dong HIDE-DB tuong ung cua bo du lieu do, lay tu protocol.yaml dong_1), hay doi theo tung r (vi du sigma = r x |D| de "cung muc rieng tu danh nghia")? Chon co dinh thi cot Lost so sanh duoc theo chieu ngang giua cac dong r va voi dong HIDE-DB - dung muc dich cua SC4 - nhung phai khai rang khi r nho, so lan che tang manh trong khi mau so F_sigma(D) khong doi. Chon theo tung r thi moi dong doi mau so, cot Lost mat y nghia so sanh ngang va SC4 khong con tra loi duoc RQ5. Co che da san (truong frequentThreshold cua HideProblem va constructor bay tham so); chi thieu mot cau chinh sach + gia tri sigma cho tung bo. Nguoi viet quyet; can truoc khi cai va chay SC4.

**Giá trị ell_max và epsilon chưa ấn định**

Chỉ còn một lựa chọn hẹp: giữ ℓ_max = 3 và ε = 0,5 cho MỌI bộ (khai một lần trong bảng giao thức, đúng như mười cấu hình SC2 đã chạy), hay đặt giá trị riêng theo từng bộ? Nếu giữ đồng nhất thì điền cùng một cặp vào cả tám dòng theo_bo của protocol.yaml dòng 1 và dòng 2 rồi coi như xong. Nếu đặt riêng thì phải nêu căn cứ đo được và chạy lại SC2 cho mỗi cặp mới, vì ℓ_max đổi thì F_σ(D) đổi và ε đổi thì B_ε đổi, kéo theo toàn bộ trọng số w(i) và mọi độ đo Lost/SupDist không so được giữa hai lần chạy. Riêng σ thì bắt buộc phải khai theo từng bộ vì dải đã chạy khác nhau (bms1 100 và 300; sign 100 và 200; fifa 3000; msnbc 1000, 2000, 5000).

**Giá trị σ, ℓ_max, khoảng cách, |𝒮| cho từng bộ**

Điền dòng 1 của protocol.yaml cho từng bộ sẽ chạy SC3 (tối thiểu dna và msnbc là nhóm chính; sign, bms1, fifa là đối chứng suy biến): mỗi bộ chạy ở dải σ nào (chọn mấy giá trị, neo vào các σ đã đo ở SC2 — dna 1000, msnbc 1000/2000/5000, fifa 3000, sign 100/200, bms1 100/300 — hay quét dày hơn?), ℓ_max giữ 3 cho mọi bộ hay khác, [g⁻,g⁺] giữ [0,3] cho mọi bộ hay dùng thêm [0,1] cho bộ cần β cao (giai_doan_1 §2 khuyến nghị [0,3] làm cấu hình chính, [0,1] nếu muốn β cao), và |𝒮| chạy đủ {1,5,10,20} hay rút gọn. Kèm theo, sửa mặc định Sc3MainComparison để sensitiveSets = 20 cho khớp §11.5 hoặc khai rõ trong bài là dùng ít hơn.

**Sigma cho tung bo du lieu (dong 1)**

Với từng bộ THỰC SỰ có trong kho (dna_mt745584, msnbc, fifa, sign, bms1, leviathan, bible), chốt dãy ít nhất 3 giá trị σ tuyệt đối (đơn vị: số lần xuất hiện không chồng lấp) làm trục hoành cho SC3-SC8, ghi kèm tỉ lệ σ/Σ_P sup_NO tương ứng; đồng thời quyết bỏ hay giữ ba hàng kosarak, trucks, tong_hop trong bảng giao thức. Người viết chốt, căn cứ bảng SC2 đã có ở results-invariant, và phải chốt TRƯỚC lần chạy SC3 lấy số vào bài (preregistration quy tắc 2.3.10).

> Chặn: Chan SC4. Neu doi sigma giua cac dong thi cot Lost khong so sanh duoc theo chieu ngang voi dong HIDE-DB o "cung muc rieng tu danh nghia" - ma do chinh la muc dich cua SC4.

### A12. Ngưỡng ε của B_ε cho từng bộ

Nguồn nêu: `design-gap`, `design-gate`, `design-theory`, `protocol`

**Bước 3 với mẫu lặp mục: tập ứng viên chưa định nghĩa**

Chốt định nghĩa tập ứng viên và phép kiểm của bước 3 khi μ(Q) ≥ 2, chọn một: (i) giữ quy tắc rẻ mà mã đang dùng — 'vị trí mang ít nhất một nút thiết yếu theo L5' — và khai rõ trong bài rằng đây là XẤP XỈ dưới của tập vị trí thiết yếu, chi phí một luồng cực đại cho mỗi Q ∈ B_ε; (ii) làm đúng §9.2: chạy một luồng cực đại trên NT − V_i cho từng vị trí ứng viên (giới hạn ở vị trí mang nút), chi phí |vị trí mang nút| × chi phí luồng cho mỗi Q, và sửa NettreeFlow/NettreeCutIndex cho khớp; (iii) lọc B_ε xuống μ(Q) = 1 trong FrequentPatterns.nearThreshold, khi đó L5 cho tập chính xác bằng một luồng cực đại và câu độ phức tạp gọn lại. Đơn vị của chi phí cần phát biểu: số lần chạy luồng cực đại trên |B_ε|. Người viết quyết (chọn giữa xấp xỉ rẻ và phép kiểm đầy đủ). GẤP: trước SC3, vì quyết định này đổi w(i) ở bước 4, do đó đổi mọi số Lost của SC3, và cũng đổi số thời gian chỉ mục ở SC6.

**Ngưỡng ε của B_ε chưa có giá trị cho từng bộ**

Chốt ε (số thực không thứ nguyên, định nghĩa B_ε = {Q : σ ≤ sup_NO(Q,D) < (1+ε)σ}) và điền dòng 2 protocol.yaml: giữ MỘT giá trị chung ε = 0,5 cho cả tám bộ (đúng mặc định mã) kèm một câu căn cứ, hay đặt ε riêng từng bộ? Nếu đặt riêng thì tiêu chí nào quyết: chặn trên kích thước |B_ε| (số mẫu) để giữ chi phí bước 3 trong ngân sách, hay độ phủ mẫu gần biên (tỉ lệ F_σ nằm trong dải)? Người viết quyết, nhưng phải chạy trước một lượt quét |B_ε| theo ε ∈ {0,1; 0,25; 0,5; 1,0} trên từng bộ để có số. GẤP: trước SC3, vì w(i) ở bước 4 và mọi số Lost của SC3 phụ thuộc ε, và §11.5 đòi in cảnh báo |B_ε| trước khi chạy tập lớn.

**Ngưỡng ε xác định 'mẫu gần biên' B_ε**

Chọn giữa hai hình thức chốt cho dòng 2 bảng giao thức: (a) giữ một giá trị ε duy nhất cho mọi bộ — xác nhận ε = 0,5 đang là mặc định trong mã (Sc3MainComparison.java dòng 71) và xoá bảy ô theo_bo trong protocol.yaml, hay (b) đặt ε riêng cho từng bộ (msnbc, dna_mt745584, leviathan, bible, fifa, sign, bms1) theo |B_ε| đo được. Đơn vị: số thực không thứ nguyên, kèm căn cứ là kích thước |B_ε| và chi phí bước 3 quan sát ở SC2/SC6. Người viết quyết, ghi vào config/protocol.yaml trước khi chạy SC3 lấy số vào bài.

**Giá trị ngưỡng epsilon của B_epsilon**

Quyết đúng một điều: giữ ε = 0,5 đồng nhất cho mọi bộ (giá trị mặc định đang chạy trong Sc3MainComparison và đã ghi ở protocol.yaml) hay đặt ε riêng cho từng bộ; rồi điền dòng 2 của config/protocol.yaml cho cả tám bộ. Không phải quyết lại quy tắc cắt |B_ε| (§9.2 đã chốt: giới hạn theo ε, và tắt bước 3 không đổi HF). Người viết quyết; cần trước SC3 để mọi số đưa vào bài đều có ε khai kèm.

**Nguong epsilon cua B_eps tung bo (dong 2)**

Chọn giữa hai phương án: (a) khai ε = 0,5 đồng nhất cho mọi bộ, kèm một câu căn cứ dựa trên |B_ε| đo được; hay (b) đặt ε riêng theo bộ khi |B_ε| nổ ra (dna và msnbc là hai bộ có nguy cơ), và nếu quét thì quét mấy mức, ví dụ {0,1; 0,25; 0,5}. Người viết chốt TRƯỚC SC3, vì ε đổi thì trọng số w(i) đổi, kéo theo Lost và SupDist đổi, phải chạy lại toàn bộ SC3.

> Chặn: Không ước lượng được kích thước và bộ nhớ của chỉ mục (tuyên bố cốt lõi của đóng góp 2); không chạy được bước 3 và bước 4 của CUT-HIDE; SC6 không đo được bộ nhớ đỉnh; tệp code/config/preregistration.md chưa chốt được.

### A13. Các phân vị sup_NO để lấy mẫu tập nhạy cảm

Nguồn nêu: `design-experiment`

**Các phân vị sup_NO để lấy mẫu tập nhạy cảm**

Chốt đúng ba con số rồi sửa Sc3MainComparison.sampleSensitiveSets cho khớp: (1) dùng mấy phân vị của sup_NO và là phân vị nào — ba mức thấp/giữa/cao (25/50/75) hay rổ đều theo số lượng như Sc2DataRegime đang làm; (2) mỗi phân vị lấy bao nhiêu tập nhạy cảm để tổng đạt 20 tập mỗi ô như §11.5 đòi (ví dụ 3 phân vị × 7 tập, hay 4 × 5); (3) ràng độ dài mẫu đúng 2–4 (hiện mã chỉ ràng ≥ 2, và với ℓ_max = 3 thì thực tế chỉ ra 2–3 — nên quyết luôn ℓ_max = 4 hay sửa §11.5 thành 2–3). Phải chốt trước SC3 vì đơn vị lặp của H2 chính là (bộ, σ, tập nhạy cảm); ghi vào protocol.yaml dòng 1 và preregistration.

> Chặn: Hai lần chạy SC3 có thể lấy hai tập nhạy cảm khác nhau mà vẫn đúng mô tả trong bài; đơn vị lặp của H2 chính là (bộ, σ, tập nhạy cảm).

### A14. Phạm vi μ(P): giữ hẹp ở μ=1 hay mở sang μ≥2

Nguồn nêu: `design-theory`, `phase1-findings`, `runbook`

**Trọng số nút khi một mẫu có μ(P) ≥ 2**

Chot PHAM VI mu(P) cho bang chinh, chon mot trong ba phuong an da liet ke o giai_doan_1_tom_tat.md muc 3: (1) giu pham vi o mu(P) = 1, khai trong phan han che rang do la lop mau lap it hon tren MSNBC (0,218 so voi 0,434) nhung lap NHIEU hon tren DNA (0,806 so voi 0,773); (2) mo sang mu(P) >= 2, dung can P3 va IlpExact tren thuc the nho de do khoang cach toi toi uu, chap nhan khong co ket qua chinh xac cho lop do; (3) da lam roi -- bo sung DNA. Bang chung moi tu khao_sat_bo_du_lieu.md muc 3(c) lam phuong an 1 troi han, nen day la xac nhan chu khong phai can nhac lai tu dau. Kem theo, neu chon (2) thi phai them mot dong vao protocol.yaml quyet_dinh_cai_dat ghi cach gan trong so khi mu(P) >= 2 (hien chi nam trong RUNBOOK, chua vao bang giao thuc). AI QUYET: nguoi viet. GAP: truoc SC3, vi pham vi mu(P) quyet dinh mau nao duoc lay vao tap nhay cam.

**Trần cho tỉ lệ |R|*/Σ|S| và xử lý mẫu cực đoan**

Quyết có đặt trần cho |R|*/Σ|S| hay không, và nếu có thì bằng bao nhiêu. Bối cảnh đã hẹp lại: hai mẫu 0,206 và 0,184 mà điểm này lo là mẫu lặp mục nên đã bị mã loại khỏi SC3; đỉnh thật sự trong lớp μ(P)=1 là 0,102 (dna [3,4], σ=1000, [0,3]). Ba lựa chọn: (i) không đặt trần, chỉ in tỉ lệ theo đúng §8.4 và báo cáo mẫu nặng nhất riêng — hợp lệ vì bản thiết kế không đòi trần; (ii) đặt trần tuyệt đối trên mask_share (ví dụ loại mọi thực thể có |R|*/Σ|S| vượt một ngưỡng) và ghi luật loại trừ vào preregistration TRƯỚC khi chạy SC3; (iii) không loại nhưng đổi cách tổng hợp sang trung vị thay vì trung bình để một thực thể nặng không chi phối. Nếu chọn (ii) phải chốt trước SC3 để không bị coi là chọn dữ liệu sau khi thấy kết quả. Người viết quyết.

**Cách in |R|* và β khi tập nhạy cảm có nhiều mẫu**

Chốt quy tắc in |R|* và tỉ số |R|/|R|* cho ô |𝒮| ∈ {5,10,20}, chọn một trong ba: (a) khai rõ trong preregistration.md rằng RQ3 và H3 chỉ định nghĩa cho |𝒮| = 1 với μ = 1, và bảng SC3 để trống ô đó đúng như mã đang làm; (b) dùng cận dưới gộp = MAX qua các mẫu của |R|*_P (hợp lệ vì mỗi lần che hạ sup_NO của mỗi mẫu nhiều nhất 1 theo L1; lưu ý TỔNG thì KHÔNG phải cận dưới hợp lệ vì một lần che có thể phục vụ nhiều mẫu), rồi sửa boundFromP1; (c) in theo TỪNG mẫu, một dòng mỗi mẫu, và định nghĩa lại đơn vị lặp của H3 cho khớp. Đơn vị: số lần che. Người viết quyết vì đây là định nghĩa đại lượng của giả thuyết, không suy ra được từ dữ liệu. GẤP: trước SC3, vì rơi vào quy tắc 2.4.7 chốt trước khi chạy.

> Chặn: Chưa biết được phép tuyên bố CUT-HIDE chính xác tới đâu trên dữ liệu thật (§5 ghi "Chính xác khi μ(P)=1"); chưa viết được câu khai trong bản thảo cho trường hợp μ(P) ≥ 2, vốn là trường hợp phổ biến ở MSNBC/FIFA.

### A15. Nhiều mẫu nhạy cảm dưới ngưỡng CSDL: LP làm tròn hay GA lai

Nguồn nêu: `code-todo`, `design-gap`, `design-theory`, `paper`

**CUT-HIDE nhieu mau duoi nguong CSDL: LP hay GA lai**

Cho nhanh CUT-HIDE nhieu mau + nguong CSDL (CutHide.runMultiPattern khi isDatabaseMode), phai chot hai thu roi rac: (1) dinh nghia duong cong chi phi chung c_S(k) khi cac mau nhay cam chia nhau mot vi tri - k dem theo don vi nao (tong muc giam sup_NO cong don tren moi mau nhay cam trong chuoi S, hay vector muc giam tung mau), vi P7 chi dinh nghia duoc khi chi phi la ham mot bien; (2) thuat toan: (a) lam tron nghiem noi long tuyen tinh, cho he so xap xi l_max nhung can them bo giai LP vao build Java von dang tu chua (IlpExact co y nhanh-can khong dung solver ngoai), hay (b) GA lai khoi tao tu cac lat cat, khong co bao dam nhung dung lai ha tang da co. Don vi danh gia: ti so |R|/|R|* va thoi gian chay. Nguoi viet quyet, co the dua vao so do san: probe/oldprob.py cho khe nguyen OPT/LP trung binh 1,001, tuc phuong an (a) gan nhu luon toi uu tren thuc the nho. Phai chot TRUOC SC3 vi protocol.yaml dong_1 dat sensitive_size = [1, 5, 10, 20]: ba trong bon cau hinh roi vao nhanh nay va hien nem UnsupportedOperationException.

**Nhiều mẫu nhạy cảm ở ngưỡng CSDL: chọn cơ chế nào**

Chọn ĐÚNG MỘT cơ chế phân bổ số lần che giữa các chuỗi khi |𝒮| > 1 dưới ngưỡng CSDL, rồi viết đặc tả thủ tục con và sửa §7 cho khớp: (A) mở rộng tham lam theo tỉ số của §9.2 bước 7 ra TOÀN cơ sở dữ liệu (bỏ vòng lặp theo từng chuỗi) và chỉ giữ ILP làm thước đo khoảng cách tới tối ưu trên thực thể nhỏ — rẻ nhất, nhưng phải bỏ lời hứa 'bảo đảm hệ số l_max' ở §7 dòng 224; (B) nới lỏng tuyến tính + làm tròn, phải phát biểu và chứng minh hệ số l_max, cài trong baseline/IlpExact.java; (C) GA lai khởi tạo từ lát cắt, phải khai cỡ quần thể, số thế hệ, tỉ lệ lai/đột biến và hạt giống (dòng 3 protocol.yaml đang null), cài trong baseline/GaHybrid.java; (D) tổng quát hoá quy hoạch động P7 sang đường cong chi phí gộp nhiều mẫu. Người viết quyết; nếu chọn (B) hoặc (C) thì phải có thêm một chứng minh hoặc một bộ tham số, nếu chọn (A) thì phải sửa danh sách đóng góp. GẤP: chặn nhánh |𝒮| ∈ {5,10,20} của SC3, cột Metaheuristic ở bảng §11.4 và baseline tầng (c).

**Ho metaheuristic va tham so cho GA thuan va GA lai**

Chi con hai o phai chot, khong phai toan bo thiet ke GA. (1) Doi chung tang (c): lay EMO cua [R12] (toi uu dien tu, cung nhom tac gia, phai doc lai de cai dung) hay GA thong thuong khoi tao ngau nhien - hai lua chon nay tra loi cung mot cau hoi "phuong phap khac ho dat duoc gi", nhung EMO gay rui ro trung dong gop va dong 30 con doi doi chieu lai voi cach de cuong NCS dung NSGA-II, trong khi dong 29 da cam Pareto/NSGA-II. (2) Bon con so + hat giong: kich thuoc quan the, so the he, ti le lai ghep, ti le dot bien, va gia tri cho protocol.yaml dong_3 giai_thuat_di_truyen (hien null). Don vi: so ca the, so the he, xac suat trong [0,1], so nguyen seed. Nguoi viet quyet; phai chot truoc khi chay SC3 voi methods co "ga", vi ham thich nghi HF/Lost/|R| va so lan danh gia quyet dinh ca thoi gian chay lan ket luan "phan dong gop cua viec khoi tao tu lat cat".

**Trường hợp nhiều mẫu: làm tròn LP hay GA lai**

Chọn MỘT trong ba cho nhánh nhiều mẫu nhạy cảm dưới ngưỡng cơ sở dữ liệu, và chọn trước khi chạy SC3 với |𝒮|>1: (a) làm tròn nới lỏng tuyến tính — bài được tuyên bố tỉ số xấp xỉ ℓ_max (với ℓ_max=3 là hệ số 3), phải cài quan hệ nới lỏng trong IlpExact và chứng minh bảo đảm; (b) GA lai khởi tạo từ lát cắt — không có tỉ số, bài phải trình bày nó như lớp tìm kiếm cho phần NP-khó kèm so sánh bắt buộc với GA thuần để tách phần đóng góp của khởi tạo, và phải khai quần thể, số thế hệ, tỉ lệ lai ghép, đột biến vào dòng 3 bảng giao thức; (c) tham lam theo tỉ số như §9.2 bước 7 đã đặc tả và mã đã cài cho HIDE-SEQ, mở rộng sang phân bổ liên chuỗi — rẻ nhất nhưng hạ câu "chỉ dùng lớp tìm kiếm xấp xỉ" ở tóm tắt xuống mức heuristic thuần. Chọn xong phải sửa đồng thời baibao.tex:801-803, doc §7 mục 3 và doc §9.2 bước 7 cho khớp nhau.

**Lời giải cho trường hợp nhiều mẫu: LP làm tròn hay GA lai**

Chọn một lời giải cho nhánh nhiều mẫu dưới ngưỡng CSDL (chỗ CutHide.java dòng 143 đang ném lỗi): (a) làm tròn nới lỏng tuyến tính — nếu chọn thì phải viết thêm một mệnh đề vào §8.1 chứng minh hệ số xấp xỉ ℓ_max, nếu không thì phải bỏ cụm 'có bảo đảm hệ số ℓ_max' khỏi đóng góp 3; (b) GA lai khởi tạo từ lát cắt — phải cài GaHybrid và khai tham số quần thể, thế hệ, lai, đột biến vào dòng 3 bảng giao thức; (c) mở rộng tham lam theo tỉ số của bước 7 sang ngưỡng CSDL bằng cách gộp đường cong chi phí, chấp nhận không có bảo đảm. Đơn vị: một lựa chọn + (nếu (a)) một mệnh đề có chứng minh. Người viết quyết, trước khi chạy SC3 với |𝒮| > 1 và trước khi chốt câu đóng góp 3.

> Chặn: Không cài được bước 7 của CUT-HIDE; không chạy được nhánh nhiều mẫu của SC3; baseline tầng (c) ở §11.3 giả định tồn tại 'GA lai' để đo phần đóng góp của việc khởi tạo; câu khẳng định 'có bảo đảm hệ số ℓ_max' trong danh sách đóng góp hiện chưa có chứng minh trong §8.1.

### A16. "Thực thể nhỏ" là gì, và SC3 lấy biến thể ILP nào làm mốc

Nguồn nêu: `code-todo`, `design-experiment`

**SC3 dung bien the ILP nao lam moc toi uu**

Chi con mot o: khi |S| > 1 (noi P1 khong cho |R|*), cot doi chieu toi uu cua bang SC3 lay tu "ilp" (exact-minR, chi cuc tieu |R|) hay "ilp-lost" (exact-minR-minLost, them muc tieu Lost)? §2 da phat bieu muc tieu tu dien nen ve dinh nghia phai la exact-minR-minLost; nhung chon no thi phai chap nhan gia: IlpExact liet ke moi nghiem co nho nhat, chan boi MAX_OPTIMAL_SETS = 20.000 va thuc the bi gioi han MAX_CANDIDATES = 64, MAX_MASKS = 12, nen phai khai ro co thuc the toi da chay duoc va cach xu ly khi vuot han (bo dong hay danh dau OT). Neu chon exact-minR cho re thi bang chi tra loi duoc "can bao nhieu lan che", khong tra loi duoc "tiet kiem duoc bao nhieu Lost", va cau RQ2 ("o cung |R|, CUT-HIDE co mat it mau hon khong") mat moc tren. Cung phai chot luon: them "ilp" hay "ilp-lost" vao methodList mac dinh cua Sc3 dong 75 (hien ca hai deu ngoai mac dinh), de bang khong am tham thieu cot. Nguoi viet quyet; truoc SC3.

**Định nghĩa "thực thể nhỏ" cho bộ giải chính xác**

Quyết cách DỰNG thực thể nhỏ, vì hai chặn có sẵn trong mã (≤ 64 vị trí ứng viên trên toàn CSDL, ≤ 12 lần che) khiến không thực thể thật nào lọt qua: lấy mẫu con bao nhiêu chuỗi từ mỗi bộ (|D| = ?), cắt độ dài chuỗi tới bao nhiêu, |𝒮| bằng mấy, σ nào, và lấy bao nhiêu thực thể như vậy cho mỗi bộ để tỉ lệ khoảng cách tới tối ưu có ý nghĩa (ví dụ 30 thực thể/bộ, dùng chung đơn vị lặp (bộ, σ, tập nhạy cảm) của §11.6)? Đồng thời quyết dùng bản 'exact-minR' hay 'exact-minR-minLost' (IlpExact.withSideEffectObjective) — Javadoc của lớp đòi kịch bản phải khai rõ đã dùng bản nào. Ghi vào protocol.yaml và preregistration trước SC3, vì khoảng cách tới tối ưu là bằng chứng của RQ2.

> Chặn: Ti so |R|/|R|* va cau tra loi cho RQ3 phu thuoc vao moc nao duoc chon; neu khong chot thi bang SC3 khong co cot so sanh voi loi giai chinh xac, tuc mat tang (c) trong bang so sanh chinh.

### A17. Gắn phiên bản mã vào tệp kết quả (đang là unversioned)

Nguồn nêu: `phebinh`  (có điểm do lượt phê bình bổ sung)

**Gắn phiên bản mã vào tệp kết quả (đang là unversioned)**

Chốt cách gắn phiên bản mã vào mỗi lần chạy lấy số vào bài, và số phận số liệu đã có: (a) đặt HIDENOSP_REVISION = git rev-parse HEAD trong một script bọc ngoài (hoặc cho RunId tự gọi git) và bổ sung điều kiện cây làm việc sạch, ghi thêm hậu tố -dirty khi bẩn; (b) chấp nhận nhãn unversioned và bỏ cột phiên bản mã khỏi yêu cầu 2.6.8. Kèm theo: 16 tệp SC2 của giai đoạn 1 đang mang nhãn dev_unversioned — giữ nguyên và khai trong bài rằng chúng gắn với commit nào bằng ngày giờ, hay chạy lại một lượt SC2 sau khi có cơ chế đóng dấu và sau khi protocol.yaml dòng 1 được chốt.

> Chặn: Bảng đặc trưng dữ liệu của bài (ρ_rep, β) lấy từ các tệp SC2 hiện có; không có phiên bản mã thì không ai, kể cả người viết, dựng lại được đúng số khi mã đã đổi bốn commit trong một ngày. Chính tệp chốt trước đòi 'git log làm bằng chứng thời điểm', nhưng kết quả lại không trỏ ngược về commit nào.

### A23. Nội dung tệp preregistration và việc chuyển go/no-go sang chính thức

Nguồn nêu: `code-todo`, `design-gap`, `design-gate`, `design-theory`, `runbook`

**Cai dat tham chieu NETGAP cho phep kiem SC1**

Chon mot trong ba, va he qua khac han nhau: (a) lay cai dat NETGAP cua [R1] tu ma nguon cua tac gia hoac mot thu vien cong khai - phai them mot o "nguon va phien ban thu vien NETGAP" vao protocol.yaml (hien dong_5 khong co o do, nen TODO dang tro toi mot o khong ton tai) va chap nhan phu thuoc ngoai trong build Maven von dang tu chua; (b) tu cai lai NETGAP trong kho nay - khi do phai ha cach phat bieu trong bai tu "doi chieu voi cai dat goc" xuong "doi chieu voi mot cai dat doc lap cua thuat toan NETGAP theo mo ta trong [R1]", vi cung mot nguoi cai ca hai thi sai sot ve HIEU noi dinh nghia se trung nhau va phep kiem khong bat duoc; (c) bo nua phep kiem nay va sua dong 396 cua thiet ke, dua hoan toan vao doi chieu voi vet can (BruteForce) von da chay. Nguoi viet quyet. GAP: phai chot truoc SC3 vi preregistration.md muc 4 dat "SC1 phai xanh (0 ca lech) truoc moi kich ban khac", va moi so lieu SC3 deu dua tren sup_NO.

**Bộ nhớ đỉnh của chỉ mục: đo heap hay RSS**

Chốt đại lượng 'bộ nhớ đỉnh' của SC6 và điền dong_6_do_bo_nho: (i) chỉ báo cáo heap đỉnh lấy mẫu trong tiến trình (MemoryProbe, chu kỳ 50 ms) và SỬA §11.7 dòng 6 để bỏ đòi hỏi RSS, ghi rõ trong chú thích bảng là heap chứ không phải RSS; hoặc (ii) giữ RSS tối đa của tiến trình, khi đó phải chốt ba thứ: ai viết script bọc ngoài (Get-Process trên Windows, /proc/self/status trên Linux), chu kỳ lấy mẫu bao nhiêu ms, và in một cột hay hai cột (heap và RSS). Đơn vị: byte (hoặc MiB) đỉnh trên một lần chạy. Người viết quyết. Mức gấp: trước khi nộp — SC6 chạy sau cùng theo preregistration.md mục 4 và exp/Sc6Scalability còn chưa cài — nhưng phải xong trước lần chạy SC6 lấy số, vì đây là cột so chỉ mục cắt Nettree với HHA, DBSH, SBSH.

**Máy đo thời gian đã khai là máy nào**

Chon MOT may vat ly lam may do da khai va dien du sau o cua protocol.yaml dong_5 (jdk -- da co san '17.0.19 OpenJDK 64-Bit Server VM' o dau moi tep ket qua; co_xms; co_xmx -- cac lan chay hien dung -Xmx4g, heap_max_bytes 6442450944; he_dieu_hanh; cpu; ram; ten_may_do), cong hai o lien quan dang null: dong_7 gioi_han_moi_lan_chay_giay va dong_9 so_lan_lam_nong. Kem theo phai quyet: PC-THAI (may phat trien hien tai) co duoc khai luon lam may do khong, hay dung mot may khac -- neu dung may khac thi moi so thoi gian phai chay lai tren do. AI QUYET: nguoi viet. GAP: truoc SC6, khong gap bang [0]/[1] vi SC6 chay sau cung theo preregistration.md muc 4 va ban than SC6 hien con nem UnsupportedOperationException.

**Ngưỡng chấp nhận cho tỉ lệ chứng nhận UB = LB của quét λ**

Quyết cách phát biểu đóng góp 3 sao cho không va vào P6(e): chọn giữa (a) bỏ cụm khẳng định chung 'có chứng nhận' và thay bằng phát biểu định lượng có điều kiện kiểu 'quét λ trả về cặp cận LB/UB, và chứng nhận tối ưu ở phần lớn cặp (S,k) đo được — dò tìm cho 14.862/14.867', hay (b) giữ cụm 'có chứng nhận' nhưng kèm một ngưỡng diễn giải chốt trước trong config/preregistration.md (đơn vị: tỉ lệ cặp (S,k) có ⌈LB⌉ = UB, ví dụ ≥ 0,95) cùng câu nói trước sẽ làm gì nếu SC7 rơi xuống dưới ngưỡng. Người viết quyết; nếu chọn (b) thì phải điền vào preregistration.md trước khi chạy SC7 lấy số vào bài.

**Chọn người thứ hai đọc kiểm chứng minh**

Chọn đích danh một người đọc kiểm (trong nhóm cộng tác [R6]/[R12] hay ngoài nhóm), giao đúng danh sách chứng minh của §8.1 gồm L0, L1, P1, P2, Hệ quả P1c, L5, P3, L4a, L4b, P6(a)–(e), P7, P5; ấn định đọc TRƯỚC khi đóng băng §5–§7; và chốt nơi ghi kết quả đọc kiểm (đề nghị một tệp riêng ngoài kho, ví dụ doc/doc_kiem_chung_minh.md, không đưa vào bản thảo). Người viết quyết. Cần trước khi nộp, vì §13 mục 1.2.1 không tick được nếu thiếu.

**Chuyển go/no-go từ kết luận tạm sang chính thức**

Chốt hai thứ và ghi thẳng vào §12a: (i) tiêu chí go chính thức — giữ nguyên tiêu chí v0.1 (còn HIDE-SEQ + P1–P2 + CUT-HIDE là của mình) hay thêm điều kiện 'SC8 phải thắng HHA/DBSH/SBSH ít nhất về Lost'; (ii) ngày hết hạn chờ toàn văn [R11], sau ngày đó tự động kích hoạt mức đọc rút gọn (§12a mục 3) và ghi 'go' mà không chờ nữa. Người viết quyết. Gấp trước SC3 vì §13 mục 1.1.7 (chốt danh sách đóng góp) treo vào nó.

> Chặn: SC1 hien mat mot nua phep kiem ma thiet ke nghien cuu (dong 396) doi "NETGAP, luong cuc dai va vet can cho cung sup_NO; so ca lech = 0". Khong co nua nay thi khong co bang chung cai dat sup_NO khop voi dinh nghia goc, ma moi so lieu SC3 deu dua tren sup_NO.

### A24. Baseline tầng (a) khi tập nhạy cảm có nhiều hơn một mẫu

Nguồn nêu: `code-todo`

**Baseline tang (a) khi tap nhay cam co nhieu hon mot mau**

Phai chot hinh dang cua bang SC3 khi |S| > 1, chon mot trong ba: (a) mo rong RandomMasking sang nhieu mau - re nhat ve khai niem (chon chuoi theo tong sup_NO tren moi mau nhay cam, chon vi tri ngau nhien tren mot lan xuat hien nhay cam bat ky), nhung do la mot thuat toan MOI do ta dat ra, khong con la "thiet ke hai chieu cua [R20]" va phai khai nhu vay; (b) chap nhan doi chung tang (a) chi ton tai o |S| = 1, tuc bang SC3 tach lam hai phan co bo phuong phap khac nhau, va phai noi ro trong chu thich bang rang cot HH/HR/RH/RR va occurrence-count trong o |S| > 1 khong phai bi bo quen; (c) thay hoan toan tang (a) bang HHA khi |S| > 1 - nhung khi do mat doi chung tam thuong, ma no la thu chung minh "bai toan khong tu giai". Nguoi viet quyet. GAP: phai chot truoc SC3 o cac co tap nhay cam lon, vi Sc3 co y khong am tham bo baseline (dong 45-46) nen cau hinh |S| in {5, 10, 20} cua protocol.yaml dong_1 se nem UnsupportedOperationException giua chung thay vi in bang.

> Chặn: SC3 co chu y la khong bao gio am tham bo mot baseline, nen cau hinh |S| > 1 se nem ngoai le thay vi in bang. Phai chot truoc khi chay SC3 o cac co tap nhay cam lon.

### A25. Đối chiếu SBSH với Thuật toán 2 và 3 của [R3]

Nguồn nêu: `code-todo`, `design-theory`

**Doi chieu SBSH voi Thuat toan 2 va 3 cua [R3]**

Ba cau hoi su kien phai lay tu Thuat toan 2 va 3 cua [R3], khong suy ra duoc tu tep nao trong kho: (1) nhan xam bao phu vi tri nao - chi vi tri khong mang nut nhay cam nao, hay ca vi tri mang nut nhung khong nam tren lat cat; (2) ban kinh r cua Replace dem theo CUNG cua Nettree hay theo VI TRI trong chuoi (lech nhau lam bien the "r nho nhat" va "r lon nhat" cua SC7 khong con dung nghia, vi Sbsh hien dien giai r la so CAP Nettree: Javadoc constructor ghi "in Nettree levels away from the red node"); (3) C-PA va C-SA khac Replace thuong o cho nao. Sau khi doc xong moi den mot quyet dinh thuc su: giu cach cai hien tai (cat moi duong qua nut do bang cach lay tron mot cap Nettree, chon cap re nhat trong ban kinh) va khai ro trong bai la "bien the cua chung toi", hay thay bang dung thu tuc cua tac gia. Nguoi doc tai lieu goc quyet phan su kien, nguoi viet quyet phan giu hay thay. Phai xong truoc SC3 vi SBSH nam trong bang so sanh chinh, va truoc SC7 (quet ban kinh) va SC8.

**Thuật toán luồng cực đại và số λ cho ca có trọng số**

Chốt câu độ phức tạp của bước 5 cho khớp mã, chọn một: (i) giữ Dinic và công bố chặn tổng quát của Dinic O(|V|²·|A|) cho mỗi lần tính lát cắt có trọng số, bỏ trích [R31] ở chỗ này; hoặc (ii) giữ tuyên bố O(|V|·|A|) [R31] thì phải thay Dinic bằng một cài đặt đạt chặn đó cho ca có trọng số. Đồng thời viết chặn số λ đã có sẵn trong mã: nhiều nhất (số trọng số phân biệt ≤ |V| + 1) + 60 lần đánh giá cho mỗi khoảng tinh chỉnh, tức O(|V|) lần luồng cực đại. Người viết quyết (đây là quyết định về điều được công bố, không suy ra từ dữ liệu). Mức gấp: trước khi nộp, không chặn SC3; nhưng phải xong trước khi diễn giải số thời gian SC6 so với HHA/DBSH/SBSH.

> Chặn: Moi so lieu SBSH trong SC3, SC7 (quet ban kinh nho nhat va lon nhat) va SC8 deu khong dung duoc cho ban thao cho toi khi doi chieu xong - chinh Javadoc dat dieu kien do.

---

## B. Chặn lúc nộp — không chặn chạy

### B1. Máy đo, cờ JVM, số lần làm nóng, giới hạn thời gian

Nguồn nêu: `code-todo`, `design-gate`, `preregistration`, `protocol`

**Khai máy đo dùng cho SC6**

Chỉ hai ô thật sự cần người quyết: (a) ten_may_do — đặt một nhãn cố định cho máy sẽ gánh TOÀN BỘ số đo thời gian và bộ nhớ (RUNBOOK dùng PC-THAI làm máy phát triển; nếu chính nó là máy đo thì đặt nhãn riêng khác 'dev', ví dụ 'pc-thai-do', vì ResultSink.java:93 chặn đúng chữ 'dev'); (b) co_xms và co_xmx — chốt một cặp cố định dùng cho mọi lần chạy SC6 (các lần chạy hiện có không nhất quán: heap_max_bytes 6.442.450.944 ở tệp SC2 nhưng 2.051.014.656 ở tệp SC3 probe, tức đã chạy với -Xmx khác nhau). Bốn ô còn lại không cần quyết: jdk và cỡ heap chép từ header tệp kết quả, he_dieu_hanh/cpu/ram là ghi chép sự kiện về máy đã chọn. Không gấp: SC6 chạy sau cùng theo §4 và hiện chưa cài (Sc6Scalability.run ném UnsupportedOperationException), nên việc này không chặn SC3.

**So lan lam nong truoc khi do thoi gian**

Dien mot so vao protocol.yaml dong_9 so_lan_lam_nong TRUOC khi chay SC6, va chot luon quy tac: mot con so duy nhat cho moi kich ban va moi kich co (vi du 3, du de JIT bien dich cac vong nong cua Dinic va PathCounts), hay so lan lam nong thay doi theo do dai mot lan chay (vi du bo qua cac lan chay trong 2 giay dau, tuong tu bang 11.6 da phan tang theo thoi gian)? Neu chon phuong an hai thi Timing.measure phai doi chu ky: hien no nhan warmups la mot so nguyen co dinh va chi do thoi gian mot lan tham do SAU khi lam nong xong, nen chua bieu dien duoc quy tac theo thoi gian. Nguoi viet quyet; gia tri phai ghi vao protocol.yaml va commit truoc lan chay SC6 dau tien, vi chon sau khi thay so do vi pham quy tac chot truoc.

**Chọn và khai báo máy đo cho SC3 đến SC7**

Chốt và điền dòng 5, 6, 7, 9 của config/protocol.yaml: tên máy đo cùng CPU, RAM, hệ điều hành; phiên bản JDK và cặp cờ -Xms/-Xmx cụ thể; giới hạn giây cho mỗi lần chạy (sinh mã OT); số lần làm nóng. Kèm một quyết định riêng: bảng bộ nhớ in 'heap đã dùng' (MemoryProbe hiện có, chu kỳ 50 ms) hay 'RSS đỉnh' như §11.7 dòng 6 đòi — nếu chọn RSS thì phải viết script bọc ngoài trước. Người viết quyết; cần trước SC6, không chặn SC3 đến SC5, SC7, SC8.

**May do chuan va moi truong chay (dong 5)**

Chỉ đích danh MỘT máy làm máy đo (có thể chính PC-THAI nếu chấp nhận cấu hình đó, hoặc một máy khác), đặt tên truyền vào --machine, rồi chép JDK/hệ điều hành/CPU/RAM vào dòng 5. Người viết quyết trước khi chạy SC6; SC3-SC5, SC7, SC8 vẫn chạy được trên máy bất kỳ vì số của chúng bất biến theo máy, nên lịch Tuần 8-11 không thật sự bị chặn.

**Co JVM -Xms va -Xmx**

Chốt một cặp cờ cố định dùng cho MỌI thuật toán và mọi baseline ở SC6 và SC8, ví dụ -Xms = -Xmx = 8g (đặt Xms bằng Xmx để loại nhiễu do heap nở dần), kèm căn cứ theo RAM của máy đo và theo thực thể lớn nhất định chạy. Người viết chốt cùng lúc với điểm máy đo, trước khi chạy SC6; đổi cờ sau là phải đo lại toàn bộ bảng thời gian và bộ nhớ.

**So lan lam nong truoc khi do (dong 9)**

Chốt số lần chạy làm nóng bị bỏ (ví dụ 3 hoặc 5) bằng cách đo chứ không bằng cảm tính: chạy thử một thực thể đại diện trên máy đo, xem tới lần lặp thứ mấy thời gian ổn định dưới một ngưỡng biến thiên đã khai (ví dụ dao động < 5%), lấy con số đó ghi vào dòng 9 TRƯỚC lần chạy SC6 đầu tiên; mọi số thời gian đo trước khi chốt phải bỏ.

**Gioi han thoi gian moi lan chay (dong 7)**

Chốt MỘT ngưỡng cắt duy nhất tính bằng giây cho mỗi lần chạy (ví dụ 1800 s), áp chung cho CUT-HIDE, mọi baseline và bộ giải chính xác; kèm cách cắt (ngắt luồng từ trong hay giết tiến trình từ script bọc ngoài) và cách ghi ô OT vào bảng. Người viết chốt trước SC3, vì ô ILP của SC3 chưa có bất kỳ cận nào.

> Chặn: SC6 không ghi được số thời gian và bộ nhớ vào bài: nhãn mặc định `dev` bị `hidenosp.io.ResultSink` từ chối, và ô tương ứng trong config/protocol.yaml (`ten_may_do`, cùng `jdk`, `co_xms`, `co_xmx`, `he_dieu_hanh`, `cpu`, `ram`) vẫn là null.

### B3. σ báo cáo theo tỉ lệ hay tuyệt đối

Nguồn nêu: `design-experiment`, `protocol`

**σ giữ tỉ lệ hay giữ tuyệt đối**

Khi SC6 đổi |D| (và độ dài chuỗi), σ giữ nguyên số tuyệt đối hay giữ nguyên tỉ lệ σ/Σ_P sup_NO? Lưu ý chi phí không đối xứng: giữ tuyệt đối là phương án KHÔNG phải viết thêm mã (FrequentPatterns.mine đã nhận σ tuyệt đối), còn giữ tỉ lệ đòi thêm một bước quy đổi và một cảnh báo (doc dòng 451 đã hứa 'Bộ chạy in cảnh báo quy đổi σ'). Người viết quyết, ghi vào protocol.yaml dong_1.ghi_chu (thay câu 'SC6 quyết định' bằng quyết định thật) trước khi cài exp/Sc6Scalability, và bài phải in σ ở cả hai dạng cho từng bộ dù chọn quy ước nào.

**Bao cao sigma theo ti le hay tuyet doi**

Ở SC6, khi |D| và độ dài chuỗi thay đổi, giữ cố định σ tuyệt đối (dễ đặt cạnh ngưỡng ψ của [R3]) hay giữ cố định tỉ lệ σ/Σ_P sup_NO (giữ mật độ mẫu không đổi khi dữ liệu to ra)? Chọn đúng một, khai một câu trong bài, và mọi bảng SC2-SC8 in dạng còn lại trong ngoặc. Người viết chốt trước khi chạy SC6.

> Chặn: Khong thong nhat duoc truc sigma giua cac bang SC2-SC8; neu doi cach khai sau khi da chay thi phai ve lai toan bo duong danh doi.

### B4. Tham số và quy mô sinh dữ liệu tổng hợp

Nguồn nêu: `protocol`

**Quy mo du lieu tong hop (dong 4)**

Chốt lưới sinh cho bộ tổng hợp: kích thước bảng chữ cái (ví dụ 4 như DNA hay 17 như MSNBC), số chuỗi N và độ dài chuỗi ở cấu hình cơ sở, cộng dải quét cho SC6 (ví dụ N ∈ {10^3, 10^4, 10^5} × độ dài ∈ {50, 100, 200}). Đơn vị: số nguyên. Người viết chốt trước khi chạy SC6; không chặn SC3.

> Chặn: Khong sinh duoc bo tong_hop, nen dong 1 va dong 2 cho tong_hop khong dien duoc; SC6 (mo rong theo |D| va do dai chuoi) khong co dai quet.

### B6. Nguồn bộ TRUCKS, và số phận SC8 nếu không có

Nguồn nêu: `code-todo`, `design-experiment`

**Nguong psi va don vi bao cao cua cac bai goc cho SC8**

Hai thu phai lay tu chinh [R3] roi chep vao protocol.yaml (hien khong co dong nao chua duoc chung): (1) gia tri psi cho tung bo trong ba bo MSN, BMS-WebView-1, TRUCKS - va phai khai psi la nguong TUYET DOI (so giao dich/chuoi) hay TI LE tren |D|, vi [R3] dem theo so chuoi chu khong theo sup_NO, dung sai don vi thi bang so sanh vo nghia; (2) cong thuc chinh xac cua hai don vi ho bao cao - "side-effects (%)" lay mau so la gi (|F_psi(D)| ban dau, hay |F_psi(D) \ S+|) va "distance (%)" do tren cai gi (so su kien bi xoa chia tong so su kien, hay khoang cach chuoi), de con ghi cong thuc doi chieu canh Lost/DistR cua §2. Nguoi doc tai lieu goc [R3] quyet; khong the suy ra tu du lieu hay tu ma. GAP: chan SC8, kich ban duy nhat cho phep dat so cua bai canh so da cong bo (thiet ke dong 402); lay psi sai thi bang mat gia tri ma khong bao loi.

**Vai và nguồn của bộ TRUCKS cho SC8**

Chọn một trong ba: (a) tiếp tục xin trucks (tác giả [R20]/[R3] hoặc kho quỹ đạo gốc) và nếu được thì SC8 giữ nguyên ba bộ; (b) bỏ TRUCKS, chạy SC8 trên hai bộ MSNBC và BMS1 và viết một câu khai rõ trong bài rằng bộ thứ ba không lấy được nên số của [R20]/[R2] trên TRUCKS không đặt cạnh được; (c) thay bằng một bộ quỹ đạo rời rạc hoá công khai khác (không phải BIKE, vì BIKE đã bị loại theo đặc trưng đo được). Người viết quyết; nếu chọn (b) thì phải sửa cả doc §11.1 dòng 402 (mô tả SC8 đang hứa ba bộ) và §11.2 dòng 417.

> Chặn: Chan SC8 - day la kich ban duy nhat cho phep dat so lieu cua bai nay canh so lieu da cong bo (thiet ke nghien cuu dong 402). Khong co psi thi khong chay duoc, va lay psi sai thi bang so sanh mat gia tri.

### B8. [R11] Zhao 2025: còn chặn Cổng 1 không, có đếm theo sup_NO không

Nguồn nêu: `design-gap`, `design-gate`

**Phạm vi rà và cách phát biểu khoảng trống G1, G5**

Chỉ còn một nửa mở: có mở rộng đợt rà cho dòng episode (Laxman [R19] và họ hàng) hay chấp nhận phạm vi hiện tại? Cụ thể là chọn giữa (a) nâng [R19] từ SDL lên MUC và rà thêm ít nhất hai nguồn có cấu trúc (ACM DL, DBLP theo tác giả 2007–2026) rồi ghi nhật ký truy vấn vào §4, hay (b) giữ nguyên một lượt tìm kiếm từ khoá và viết G5 kèm đúng một câu khai phạm vi cùng ngày rà. Nửa còn lại của câu hỏi gốc — viết phủ định tuyệt đối hay theo cơ chế kèm phạm vi — coi như đã quyết theo cách thứ hai (§4 dòng 117, tiêu đề G1 dòng 75, cột 'Mức đọc' của §4), chỉ cần áp thêm cho nhánh có toàn văn [R11]. Người viết quyết.

**Điều kiện gắn với đóng góp 1 và 2, và phương án khi vỡ**

Chọn MỘT điều kiện và MỘT phương án, rồi sửa cả ba chỗ cho khớp. (1) Điều kiện làm mất đóng góp 1: một vế ('[R11] đếm theo sup_NO', §7) hay hội hai vế ('đếm sup_NO VÀ giảm một phần', §12a dòng 539)? (2) Khi điều kiện vỡ thì chọn phương án nào trong ba phương án đang cùng tồn tại: (a) chuyển sang HIDE-SEQ đơn thuần, (b) bỏ hướng, (c) giữ bài với hai lớp còn lại (chỉ mục cắt Nettree + CUT-HIDE) và đánh giá chủ yếu trên bài toán cũ ở SC8 như §12a dòng 537 gợi ý? Người viết quyết; kết quả phải viết lại vào §7 dòng 207–208, §12 dòng 507 và §12a dòng 539.

**Zhao 2025 [R11] có đếm theo sup_NO không**

Thuật toán thứ ba của [R11] (Zhao, Chen, Zhang, CCPE 2025, doi 10.1002/cpe.70187) đo 'mất mẫu phổ biến' bằng cách đếm nào: (i) độ hỗ trợ không chồng lấp kiểu NOSEP [R1], (ii) đếm mọi lần khớp có chồng lấp, hay (iii) chỉ có mặt/không có mặt? Chỉ cần một câu trả lời ba lựa chọn này để điền bốn ô (?) ở cột [R11] của bảng delta §6 và chuyển G1 từ 'tạm' sang chính thức. Phải lấy từ toàn văn bài gốc (mua, mượn qua CESTI/thư viện trường, hoặc xin bản tác giả) — không suy ra được từ bất kỳ tệp nào trong dự án.

**Có tuyên bố Cổng 1 đã qua khi mức đọc chưa đạt không**

Quyết một lần: có kích hoạt phương án mức đọc rút gọn (§12a mục 3) để ghi 1.1.12 là 'ĐẠT có ràng buộc' và sửa dòng 7 + dòng 571 cho nhất quán với dòng 546 hay không. Nếu có, đồng thời lập danh sách chín mục §13 còn lại (1.1.1, 1.1.8, 1.1.9, 1.1.10, 1.1.13, 1.1.14, 1.2.1, 1.2.3, 1.2.7) với người làm và hạn cho từng mục, vì chúng mới là phần còn lại của Cổng 1. Người viết quyết. Cần trước khi viết tuyên bố khoảng trống vào bản thảo, không chặn SC3.

**[R11] còn là điểm chặn Cổng 1 hay không**

Đặt một hạn cứng (ngày cụ thể, tính từ hôm nay) để chuyển sang bước 3 của §12a: sau hạn đó, tick 1.1.12 với mức đọc 'tóm tắt + trang 1' cho [R11] kèm ba ràng buộc (a)(b)(c), và sửa câu ở §0 dòng 7 cùng ô §13 dòng 571 từ 'CHƯA ĐẠT / chặn duy nhất' thành 'đạt có điều kiện, theo §12a bước 3'. Đơn vị quyết định: một ngày tháng + hai câu sửa trong tệp thiết kế. Người viết quyết; không tệp nào trong dự án chốt hộ được.

**Mốc thời gian tuyệt đối của lịch 15 tuần**

Đặt ba mốc tuyệt đối và đánh số lại lịch cho khớp thực tế: (i) ngày T0 bắt đầu đếm 15 tuần; (ii) ngày hết hạn chờ toàn văn [R11], tức mốc tự động kích hoạt §12a mục 3 (trùng phần (ii) của điểm [1]); (iii) tên một hội nghị cụ thể cùng hạn nộp của nó (trùng phần còn lại của điểm [3]). Kèm việc sắp lại §12b vì tuần 4–7 đã xong trước tuần 1–3. Người viết quyết.

> Chặn: G1 và G5 còn xếp loại 'tạm'; bốn ô '(?)' ở cột [R11] của bảng delta §6 (cách đếm, cấu trúc, mẫu ma, độ khó); câu delta so với [R11]; đóng góp 1 và 2 ở §7 còn ghi điều kiện; mục 1.1.12 của Cổng 1 ghi CHƯA ĐẠT; baseline tầng (b) từ [R11] ở §11.3 ghi 'chốt sau khi có toàn văn'.

### B9. Trạng thái các khoảng trống G1, G2, G5 và phạm vi rà

Nguồn nêu: `design-gap`

**Xếp loại khoảng trống G2 không thuộc phân loại đã khai**

G2 nhận nhãn nào trong hệ phân loại hai giá trị mà chính §3 dòng 73 khai: 'mạnh' hay 'yếu'? (Gợi ý kiểm: tiêu chí 'yếu' là công trình gần nhất đã chạm tới ý đó và bài này chỉ hình thức hoá — mà [R20] đã cảnh báo mẫu giả cho phép THAY và [R2] đã chứng minh tính đơn điệu cho mô hình quỹ đạo, nên G2 khớp mô tả 'yếu' hơn.) Nếu muốn giữ ba loại thì phải thêm một câu định nghĩa 'trung bình' kèm tiêu chí vào §3 dòng 73 và sửa §13 dòng 562. Đơn vị: một từ + (nếu giữ ba loại) một câu định nghĩa. Người viết quyết.

> Chặn: §13 mục 1.1.2 đang ghi là đã xếp mạnh/yếu xong, nhưng thực tế G2 chưa được xếp; câu tuyên bố khoảng trống G2 trong bản thảo không viết được theo đúng khung phân loại.

### B10. Mức đọc bắt buộc cho [R22] trước khi tuyên bố G5

Nguồn nêu: `design-gap`

**Mức đọc bắt buộc cho [R22] trước khi tuyên bố G5**

Chọn một trong ba: (a) nâng [R22] (Loukides, Gwadera, SDM 2015, doi 10.1137/1.9781611974010.87) lên toàn văn rồi mới tuyên bố G5, và sửa cột 'Mức cần' ở bảng §5 dòng 135 từ MUC thành TV; (b) giữ mức MUC nhưng bỏ chữ 'gần nhất' cho [R22] ở §3 dòng 94, tức phát biểu G5 mà không viện [R22] làm mốc gần nhất; (c) nới luật §5 dòng 123 thành 'gần nhất phải đạt ít nhất MUC' và khai rõ hệ quả. Đơn vị: một lựa chọn + sửa nhất quán §3 dòng 94, §5 dòng 123 và 135, §13 dòng 573. Người viết quyết; nếu chọn (a) thì kèm công việc đọc toàn văn.

> Chặn: Không tuyên bố được G5; ô 'Làm sạch một chuỗi sự kiện [R22]' ở ma trận §4 chỉ đứng ở mức TT; mục 1.1.14 của Cổng 1 còn ghi [R22] trong việc còn lại.

### B13. Nơi nộp, mẫu định dạng, chỗ đặt mã giả và chứng minh dài

Nguồn nêu: `design-gate`, `paper`, `phebinh`  (có điểm do lượt phê bình bổ sung)

**Chỗ đặt mã giả chi tiết và chứng minh dài**

Hai câu hỏi, câu sau độc lập với câu trước. (1) Sáu thủ tục BuildIndex, VitalNodes, WeightedCut, LambdaSweep, Allocate, MultiPattern: in mã giả đầy đủ trong bài, hay chỉ giữ khung Thuật toán 1 cộng một đoạn đặc tả văn xuôi cho mỗi thủ tục (mức tối thiểu mà §9.2 đòi) rồi đẩy mã giả xuống phụ lục? Trả lời được sau khi có giới hạn trang thật từ điểm [10]. (2) Có công bố kho code/ kèm bài không, và phụ lục trực tuyến đặt ở đâu — kho mã công khai, bản lưu có DOI kiểu Zenodo, hay trang cá nhân? Lưu ý dữ liệu thô không vào git, nên bản công bố phải kèm datasets/provenance.tsv và tools/fetch_datasets.py để người khác dựng lại đúng tệp; và nếu điểm [10] cho ra phản biện kín hai chiều thì đường dẫn phải là bản ẩn danh.

**Nơi nộp và mẫu định dạng chưa chọn**

Chọn tên nơi nộp và ngày nộp cụ thể. Ba thứ suy ra ngay sau đó, không thứ nào quyết được trước: (1) giới hạn trang thật của mẫu hai cột, thay cho ước lượng 8–10 trang — hiện nội dung 13 trang một cột ≈ 7,5 trang hai cột nên biết thật mới biết còn bao nhiêu chỗ cho bảng SC1–SC8 và hình; (2) có phản biện kín hai chiều hay không — nếu có thì gỡ tên tác giả, đơn vị, email khỏi trang đầu và ẩn danh cả đường dẫn kho mã ở điểm [11]; (3) hạn hoàn tất bản dịch tiếng Anh, tính lùi từ hạn nộp, hiện chỉ có mốc tương đối "tuần 12-15" ở §12b. Ràng buộc duy nhất đã chốt: khác nơi nộp dự kiến của ba bài trong đề cương NCS.

**Phương án nếu nhóm [R6], [R7] đang làm cùng bài toán**

Hai bước: (i) gửi câu hỏi cho nhóm tác giả [R6], [R7] — việc này chưa làm và là cái đang chặn mục 1.1.10; (ii) chốt sẵn một trong ba phương án nếu họ trả lời là đang có bản thảo cùng bài toán: thu hẹp phạm vi về chỉ mục cắt Nettree + CUT-HIDE trên bài toán cũ (SC8), cộng tác và ghi đồng tác giả, hay giữ nguyên phạm vi nhưng đổi nơi nộp sang vũng phản biện khác. Người viết quyết (thư do chính người viết gửi). Không gấp bằng các mục chặn SC3.

**Điều khoản dùng lại dữ liệu và tuyên bố nguồn dữ liệu**

Quyết hai thứ: (a) kiểm và ghi điều kiện dùng lại của từng bộ vào provenance.tsv (thêm một cột): dữ liệu SPMF là bản sao lưu trữ lại chứ không phải nguồn gốc — MSNBC gốc ở UCI, BMS-WebView-1 gốc từ KDD Cup 2000, MT745584 là bản ghi GenBank, BIBLE và LEVIATHAN là văn bản đã rời rạc hoá; bài phải trích nguồn GỐC hay trích SPMF, và có được phân phối lại tệp kèm phụ lục không (hiện .gitignore chặn dữ liệu thô, nên mặc định là không); (b) bài có mục tuyên bố dữ liệu và đạo đức hay không — đây là bài bảo toàn riêng tư chạy trên vết nhấp chuột người dùng và trên trình tự gen, nhiều nơi nộp nay đòi một câu về nguồn và về việc dữ liệu đã ẩn danh sẵn.

> Chặn: Không tệp nào trong dự án nhắc tới giấy phép hay điều khoản của bộ dữ liệu. Nếu tới lúc nộp mới phát hiện một bộ không cho dùng lại hoặc phải trích nguồn gốc, bảng kết quả đã chạy xong mới phải sửa chú nguồn, và mục tuyên bố dữ liệu phải viết gấp.

### B15. Giấy phép mã nguồn của kho khi công bố

Nguồn nêu: `phebinh`  (có điểm do lượt phê bình bổ sung)

**Giấy phép mã nguồn và cách trích dẫn kho đã công khai**

Quyết bốn thứ về việc công bố mã: (a) giấy phép nào cho kho (MIT, Apache-2.0, BSD-3, hay không giấy phép — không giấy phép nghĩa là người khác KHÔNG được quyền dùng lại, trái với mục đích tái lập); (b) thêm CITATION.cff và một DOI lưu trữ (Zenodo) hay chỉ để đường dẫn GitHub sống, vốn có thể đổi; (c) kho hiện đã công khai dưới tài khoản mang đúng tên tác giả liên hệ và lịch sử commit gắn ngày — quyết xem có đặt riêng một bản ẩn danh cho vòng phản biện kín hay chấp nhận không nộp nơi phản biện kín; (d) có đăng bản tiền công bố (arXiv) hay không, và nếu có thì trước hay sau khi nộp, vì việc đó tương tác với (c).

> Chặn: Không tệp nào trong dự án nhắc tới giấy phép, bản quyền hay tiền công bố. Kho đã ở trên mạng: đến lúc nộp thì quyết định 'ẩn danh đường dẫn' không gỡ lại được việc mã đã gắn tên tác giả, nên phải quyết trước khi chọn nơi nộp chứ không phải sau.

### B17. Đường ống sinh bảng và hình từ CSV vào bản thảo

Nguồn nêu: `phebinh`  (có điểm do lượt phê bình bổ sung)

**Đường ống sinh bảng và hình từ CSV vào bản thảo**

Chốt đường đi của một con số từ tệp CSV tới bảng trong baibao.tex: (a) viết script sinh tệp .tex (bảng booktabs) và hình, đặt ở code/tools/ hay ở paper/tools/ (§11.8 nói paper/tools/ 'chưa có'), viết bằng gì (Python + matplotlib, hay pgfplots đọc thẳng CSV), và bản thảo \input tệp sinh ra hay chép tay; (b) chấp nhận chép tay và khai rõ rủi ro lệch số. Quyết cùng lúc: labels.tsv có hai cột nhãn Việt và Anh, vậy script sinh bảng cho bản thảo tiếng Việt hiện tại lấy cột nào, và đổi cột bằng cờ nào khi dịch sang tiếng Anh.

> Chặn: Chưa có công cụ nào biến kết quả thành bảng trong bài. Chép tay 8 kịch bản nhân nhiều bộ dữ liệu là chỗ sinh lỗi im lặng đúng loại mà ResultSink được dựng ra để chặn, và làm hỏng lời hứa 'một lần đổi tên không để sót chỗ nào' của labels.tsv.

### B19. SC8 thiếu trong lịch, và thứ tự bỏ bớt khi thiếu thời gian

Nguồn nêu: `phebinh`  (có điểm do lượt phê bình bổ sung)

**SC8 không có trong lịch, và thứ tự bỏ bớt khi thiếu thời gian**

Chốt hai thứ về thứ tự công việc: (a) SC8 nằm ở tuần nào — lịch §12b liệt kê SC3–SC7 và bỏ sót SC8, trong khi §12a lại đặt SC8 làm phương án để bài vẫn đứng nếu phần bài toán mới đổ; nếu SC8 là chân dự phòng thì nó phải chạy SỚM chứ không phải cuối, vì nó cũng là kịch bản phụ thuộc nhiều nhất vào việc mở lại toàn văn [R3] để lấy ψ; (b) thứ tự bỏ bớt khi hết thời gian: xếp hạng SC4, SC5, SC6, SC7, SC8 theo mức cần thiết cho bài, và khai trước kịch bản nào bỏ được mà vẫn nộp (ví dụ SC5 và SC7 chuyển sang phụ lục trực tuyến), để lúc cập rập không phải bỏ theo cảm tính.

> Chặn: Lịch hiện thiếu hẳn một kịch bản, mà đúng kịch bản đó là phương án dự phòng cho toàn bài. Không có thứ tự ưu tiên thì tới tuần 12 mới biết phải bỏ cái gì, và lúc đó bỏ cái nào cũng là quyết định sau khi đã thấy kết quả.

### B20. Trần quy mô cài đặt và cách khai khi đếm bão hoà

Nguồn nêu: `phebinh`  (có điểm do lượt phê bình bổ sung)

**Trần quy mô của cài đặt và cách khai khi bão hoà**

Quyết trước khi chạy SC6 lấy số: (a) có viết lại Dinic.dfs thành vòng lặp không, hay chấp nhận và khai một trần quy mô (độ dài chuỗi tối đa và g⁺ tối đa) mà trên đó không chạy — nếu chọn khai trần thì trần đó chính là giới hạn của tuyên bố khả năng mở rộng, phải nêu trong bài chứ không để tràn ngăn xếp thành ô trống; (b) khi PathCounts bão hoà thì bảng SC6 và trọng số w(i) khai thế nào: in kèm cờ isExact và loại thực thể bão hoà khỏi cột kích thước chỉ mục, hay chuyển sang đếm theo log, hay khai rằng w(i) chỉ là khoá xếp hạng chứ không phải số đếm — lưu ý w(i) bão hoà làm lát cắt có trọng số mất tính chính xác mà P6(b) hứa.

> Chặn: SC6 là cột so chỉ mục cắt Nettree với HHA, DBSH, SBSH, tức bằng chứng cho đóng góp thứ hai. Nếu bão hoà hoặc tràn ngăn xếp xảy ra đúng ở các thực thể lớn nhất thì phần thú vị nhất của bảng lại là phần không in được, và tính chính xác của lát cắt có trọng số bị đặt câu hỏi mà bài chưa có câu trả lời chuẩn bị sẵn.

### B22. SC7 hàng 4: thế nào là một "lát cắt bất kỳ"

Nguồn nêu: `code-todo`

**SC7 hang 4: the nao la mot "lat cat bat ky"**

Phai dinh nghia bang ma "lat cat nho nhat bat ky" nghia la gi, va chung minh no KHAC hang 1. Ba lua chon cu the: (a) lay lat cat phia nguon do ResidualScc tra ve khong tinh trong so (lat cat "tu nhien" cua Dinic) - nhung voi Weighting.UNIFORM thi hang 1 cung ra dung lat cat do, nen hang 4 se trung hang 1 va SC7 mat mot hang; (b) chon ngau nhien mot lat cat nho nhat trong so cac lat cat nho nhat, khi do phai khai them hat giong va so lan lap de lay trung binh, va phai noi ro lay ngau nhien tren khong gian nao (moi lat cat nho nhat co xac suat bang nhau la viec kho, thuong chi lam duoc "pha hoa ngau nhien khi duyet"); (c) bo hang 4 va sua dong 403 cua thiet ke con ba hang. Neu chon (a) hoac (b) thi CutHideConfig phai them mot truong thu nam, vi du CutSelection {WEIGHT_MINIMAL, SOURCE_SIDE, RANDOM_TIE_BREAK} - record hien tai khong bieu dien duoc. Nguoi viet quyet; can truoc khi cai SC7, khong chan SC3.

> Chặn: Chan mot trong bon hang cua SC7. TODO tu khang dinh CutHideConfig lo duoc hang thu tu, nhung record khong co truong tuong ung, nen khong ro y dinh la gi.

---

