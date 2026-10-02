# Chốt trước khi chạy SC3

Quy tắc 2.4.7: ngưỡng hiệu ứng thực tiễn và phép kiểm phải chốt **trước** khi chạy SC3.
Tệp này là bản chốt đó.

**Trạng thái: ĐÃ CHỐT, ngày 12/09/2026.** Commit ghi tệp này là bằng chứng thời điểm. Từ đây,
sửa bất kỳ con số nào dưới đây đều phải là một commit riêng, ghi rõ lý do sửa, và nếu sửa sau
khi đã thấy kết quả SC3 thì phải khai điều đó trong bài.

Căn cứ chọn từng giá trị: `config/de_xuat_gia_tri_nhom_A.md`. Số liệu làm căn cứ:
`results-invariant/giai_doan_1_tom_tat.md` và `results-invariant/khao_sat_bo_du_lieu.md`, đều
từ SC1 và SC2, chạy độc lập với SC3.

**Khai xuất xứ.** Các giá trị dưới đây do người đề xuất soạn và người viết duyệt. Người đề xuất
**đã thấy** chiều của một lần chạy SC3 dò tìm trên dữ liệu tổng hợp thay thế (`results-probe/`), nên mọi
ngưỡng được neo vào thang của dữ liệu (cỡ `F_σ`, khoảng trống trong phân bố ρ_rep) chứ không neo
vào khoảng cách đã quan sát giữa các phương pháp. Ba con số có thể chịu ảnh hưởng nhiều nhất nếu
việc đó gây thiên lệch là δ_L và hai biên tương đương ở §3; chúng được đánh dấu riêng.

---

## 1. Phạm vi

**Phạm vi chính của bài là μ(P) = 1**, tức mẫu nhạy cảm không lặp mục. Trong phạm vi đó, P1, P2,
P6(b) và lời giải chính xác đều đứng vững.

Mẫu lặp mục (μ(P) ≥ 2) báo cáo ở một mục riêng, dùng cận P3 và bộ giải chính xác trên thực thể
nhỏ, và khai rõ độ khó là câu hỏi mở.

Phần hạn chế phải ghi đúng con số sau, đo ở ℓ_max = 4 và lấy trung vị:

| Bộ | ρ_rep μ(P) = 1 | ρ_rep μ(P) ≥ 2 | Chênh |
|---|---|---|---|
| dna_mt745584 | 0,845 | 0,878 | 0,033 |
| msnbc | 0,218 | 0,364 | 0,146 |
| fifa | 0,008 | 0,952 | 0,944 |

Đọc bảng: trên **cả ba** bộ, lớp lặp mục có tỉ lệ lặp cao hơn. Điều biện minh cho việc giữ phạm
vi ở μ(P) = 1 **không** phải là lớp không lặp mục lặp nhiều hơn, mà là trên bộ nhóm chính mạnh
nhất (dna) hai lớp **gần bằng nhau**: 0,845 so với 0,878, chênh 0,033. Nghĩa là ở đó, thu phạm vi
về μ(P) = 1 gần như không mất gì. Trên MSNBC thì cái giá lớn hơn (chênh 0,146), và trên FIFA thì
toàn bộ tỉ lệ lặp nằm ở lớp lặp mục (0,952 so với 0,008) — nhưng FIFA là đối chứng suy biến nên
điều đó không đổi kết luận.

> **Đính chính.** Bản đầu của tệp này ghi "trên bộ gen MT745584 thì ngược lại (0,806 so với
> 0,773)", tức lớp không lặp mục lặp nhiều hơn. Con số đó đo ở ℓ_max = 3 và lấy **trung bình**;
> đo lại ở ℓ_max = 4 đã chốt và lấy **trung vị** thì chiều đảo lại. Kết luận giữ μ(P) = 1 không
> đổi, nhưng lý do biện minh đổi từ "cao hơn" thành "gần bằng nhau", và phần hạn chế phải viết
> theo bảng trên chứ không theo câu cũ.

Trọng số nút khi μ(P) ≥ 2: gán trọng số vị trí cho mọi nút của vị trí đó. Đây là **lựa chọn cài
đặt**, không phải hệ quả của mệnh đề nào, vì P1 và P2 không áp dụng ở đó.

---

## 2. Giả thuyết

### H2

Trên các thực thể (bộ dữ liệu, σ, tập nhạy cảm), trung vị của hiệu
`Lost_rel(baseline) − Lost_rel(CUT-HIDE)` lớn hơn ngưỡng thực tiễn δ_L.

**Đại lượng.** `Lost_rel = Lost / |F_σ(D) \ 𝒮⁺|`, mẫu số là số mẫu phổ biến không nhạy cảm
trước khi sửa, tức cột `frequent_before` của SC3 trừ đi cỡ bao đóng 𝒮⁺.

| Mục | Giá trị chốt |
|---|---|
| **δ_L** | **0,02** (hai điểm phần trăm) |
| Đơn vị lặp | một thực thể = bộ ba (bộ dữ liệu, σ, tập nhạy cảm) |
| Phép kiểm | Wilcoxon dấu hạng, một phía |
| Hiệu chỉnh đa phép kiểm | Holm trên **4** baseline tầng (b) |
| Cỡ hiệu ứng | rank-biserial = (W⁺ − W⁻)/(W⁺ + W⁻) |
| Mức ý nghĩa | α = 0,05 |

**Vì sao đại lượng tương đối.** |F_σ| đo được trải hơn một bậc, từ 51 (dna σ=2500) tới 672
(msnbc σ=1000). Một δ_L tuyệt đối duy nhất sẽ nghiêm với bộ nhỏ và lỏng với bộ lớn, tức trộn hai
thang vào cùng một phép Wilcoxon. Báo cáo **kèm** trung vị hiệu tuyệt đối trong cùng bảng.

**Vì sao 0,02.** Một điểm phần trăm trên dna (|F| ≈ 51) chưa tới nửa mẫu, nên mọi chênh lệch dù
một mẫu cũng vượt ngưỡng và δ_L mất tác dụng. 0,02 tương ứng khoảng một mẫu trên dna và 6,5 mẫu
trên msnbc σ=2000.

**Không** dùng Σ|S| hay |R|\* làm mẫu số: Σ|S| là mẫu số của DistR, |R|\* là cận số lần che.

### H3

Tỉ số `r = |R| / |R|*` của baseline tầng (b) lớn hơn 1 trên nhóm chính, và gần 1 trên nhóm suy
biến.

| Mục | Giá trị chốt |
|---|---|
| "vượt 1" | r ≥ **1,10** |
| "gần 1" | r ≤ **1,05** |
| vùng 1,05 < r < 1,10 | không kết luận |

**Lường trước một kết quả âm.** Trên lần chạy dò, mọi phương pháp đều cho r = 1,000, tức không
baseline nào dùng thừa lần che. Nếu điều đó lặp lại trên dữ liệu thật thì H3 **không bác bỏ
được**, và đó là kết quả âm phải báo cáo theo quy tắc 3.4.9, không phải lỗi.

### Họ baseline cho Holm

Họ gồm **bốn** baseline tầng (b): HHA, DBSH, SBSH-r1, SBSH-rmax.

Tầng (a) — heuristic đếm lần xuất hiện, HH, HR, RH, RR — báo cáo **mô tả**, không kiểm định:
chúng tồn tại để chứng minh bài toán không tự giải (§11.3), không để kiểm giả thuyết.

Nếu SBSH không đối chiếu được với Thuật toán 2 và 3 của [R3] thì bỏ SBSH khỏi bảng, họ còn **hai**,
và phải sửa dòng này **trước** khi chạy.

---

## 3. Phân nhóm dữ liệu

Ngưỡng áp trên **cấu hình**, tức bộ ba (bộ dữ liệu, σ, [g⁻,g⁺]) — **không** phải trên bộ dữ liệu.
Căn cứ: trên msnbc σ=2000, đổi khoảng cách từ [0,1] sang [0,10] làm β tụt từ 0,432 xuống 0,043;
β là tính chất của cấu hình chứ không của bộ.

Thống kê gộp trong một cấu hình: **trung vị** ρ_rep (và trung vị β) trên các mẫu nhạy cảm đã lấy.
Không dùng trung bình, vì phân bố lệch: msnbc [0,3] σ=2000 trải 0,106–0,575.

| Nhóm | Điều kiện |
|---|---|
| Nhóm chính | ρ_rep ≥ **0,20** |
| Nhóm trung gian | **0,05 < ρ_rep < 0,20** |
| Đối chứng suy biến | ρ_rep ≤ **0,05** |
| Ràng buộc khoảng cách có hiệu lực | β ≥ **0,20** |
| Đối chứng ràng buộc suy biến | β ≤ **0,05** |

**Nhóm trung gian không dùng để kiểm H2 hay H3.** Nó chỉ để minh hoạ rằng ρ_rep và β là hai đại
lượng độc lập: bible và leviathan có ρ_rep quanh 0,15 nhưng β tới 0,50–0,61.

**Vì sao 0,05 và 0,20.** Trung vị ρ_rep của 25 cấu hình, đo ở ℓ_max = 4, sắp tăng dần cho thấy
hai **khe thưa** rõ rệt:

- từ 0,010 (fifa) nhảy lên 0,120 (bible) — khe rộng 0,110, ngưỡng **0,05** nằm giữa;
- từ 0,181 (msnbc σ=1000) nhảy lên 0,265 (msnbc σ=2000) — khe rộng 0,084, ngưỡng **0,20** nằm giữa.

Ngưỡng đặt ở chỗ thưa nên phân nhóm không nhạy với việc xê dịch vài phần trăm.

> **Ghi chú về việc đo lại.** Hai ngưỡng này chốt dựa trên số đo ở ℓ_max = 3. Sau khi protocol
> chốt ℓ_max = 4, toàn bộ SC2 đã chạy lại và hai ngưỡng **vẫn nằm trong khe thưa**, nên chúng
> không bị sửa. Đây là đo lại để kiểm, không phải chọn lại sau khi thấy kết quả.

> **Thay bởi mục 10 (25/09/2026).** Thống kê gộp và bảng phân nhóm dưới đây tính theo trung vị
> trên **mẫu SC2 đã lấy**, gồm cả mẫu lặp mục. Từ ngày 25/09 nhóm được tính trên **mẫu không lặp
> mục** (mục 10); bảng dưới giữ lại làm lịch sử, không còn là bảng chốt.

**Phân nhóm suy ra từ dữ liệu đã đo:**

Áp trên **cấu hình** (25 cấu hình, ℓ_max = 4, trung vị):

| Nhóm | Số cấu hình | Gồm |
|---|---|---|
| Chính | 12 | dna ở cả 6 cấu hình; msnbc ở σ ∈ {2000, 5000} |
| Trung gian | 7 | **msnbc σ=1000** (cả ba khoảng cách), bible, leviathan |
| Suy biến | 6 | sign, bms1, fifa |

**Lưu ý quan trọng nảy ra từ việc áp ngưỡng trên cấu hình:** msnbc ở σ = 1000 có trung vị ρ_rep
0,172–0,181, tức **dưới** 0,20, nên ba cấu hình đó thuộc nhóm trung gian chứ không phải nhóm chính.
MSNBC chỉ là bộ nhóm chính từ σ = 2000 trở lên. Nếu áp ngưỡng trên "bộ" thay vì trên "cấu hình"
thì chi tiết này biến mất — đó chính là lý do đơn vị áp ngưỡng được chốt là cấu hình.

Ngoài ra, mọi cấu hình khoảng cách [0,10] đều có β ≤ 0,085 (dna σ=4000 thì β = 0,044), tức thuộc
**đối chứng ràng buộc suy biến** dù ρ_rep rất cao. Chúng dùng để cho thấy ràng buộc khoảng cách
mất hiệu lực khi nới rộng, không dùng làm bằng chứng cho đóng góp về ràng buộc.

Phân nhóm này chốt tại đây và **không sửa** sau khi thấy kết quả SC3 (quy tắc 2.3.10). Bộ dữ liệu
mới thêm sau ngày chốt phải được xếp nhóm bằng đúng hai ngưỡng trên, trước khi chạy SC3 trên nó.

---

## 4. Kết luận "không khác biệt"

Khi muốn kết luận hai phương pháp không khác nhau, dùng khoảng tin cậy của hiệu số cùng biên
tương đương chốt trước, không dùng "p > 0,05".

| Đại lượng | Biên tương đương |
|---|---|
| Lost_rel | ± **0,02** (bằng đúng δ_L) |
| SupDist | ± **0,01** |

Biên tương đương đặt bằng ngưỡng hiệu ứng để không sinh ra một vùng rộng mà kết quả vừa không
khác biệt vừa không tương đương.

**Con số yếu căn cứ nhất trong tệp này là ±0,01 cho SupDist.** Nó là một quy ước, chưa neo vào
độ tản đo được. Nếu muốn thay bằng con số có căn cứ, cách hợp lệ là chạy **một** phương pháp duy
nhất trên nhiều tập nhạy cảm và đo độ tản của SupDist giữa các tập — việc đó không lộ chiều so
sánh giữa các phương pháp. Thay thì phải là một commit riêng, trước SC3.

---

## 5. Quy ước tính toán

### Wilcoxon

| Mục | Quy ước chốt |
|---|---|
| Hiệu đúng bằng 0 | **Pratt**: giữ lại khi xếp hạng, không loại bỏ |
| Hạng buộc | hạng trung bình |
| Cỡ hiệu ứng | rank-biserial |

Lost là số nguyên nhỏ nên số cặp có hiệu bằng 0 sẽ nhiều. Wilcoxon gốc vứt chúng đi, làm mất
thông tin "hai phương pháp ngang nhau" và thổi phồng cỡ hiệu ứng.

### Ghép cặp cho H2

Mỗi phương pháp chạy **tới khi HF = 0**, với |R| tự chọn. Hai chế độ báo cáo tách rời:

1. Thực thể mà mọi phương pháp dùng **cùng** |R|: đưa vào Wilcoxon cho H2.
2. Thực thể mà baseline dùng **nhiều hơn**: báo cáo riêng dạng cặp (|R|, Lost), **không** kiểm
   định. Kết luận ở đó mạnh hơn và không cần thống kê.

So Lost ở hai mức |R| khác nhau là so hai thứ khác nhau; ép baseline dừng ở |R|\* thì nó chưa ẩn
xong nên Lost cũng không so được.

### Loại trừ thực thể

| Tình huống | Xử lý |
|---|---|
| Một phương pháp còn HF > 0 | loại cặp khỏi Wilcoxon, đếm và báo cáo riêng |
| Quá giới hạn thời gian | loại cặp, ghi mã OT, báo cáo số ca |
| Hết bộ nhớ | loại cặp, ghi mã OOM, báo cáo số ca |
| Tỉ lệ thực thể bị loại > **10%** | **không kết luận** H2, và nói rõ vì sao |

Gán một giá trị phạt cho ca HF > 0 là bịa dữ liệu, nên không làm.

### Phương pháp ngẫu nhiên

HH, HR, RH, RR và giải thuật di truyền: chạy **10 lần** với hạt giống **2026 tới 2035**, lấy
**trung vị** Lost và |R| làm giá trị của phương pháp đó trên thực thể đó.

Đây là đại lượng bất biến theo máy nên bảng số lần lặp của §11.6 không áp dụng — bảng đó dành cho
đo thời gian. Biến thiên quan sát được ở RR là lớn (|R| = 31 và 151 trên hai tập nhạy cảm của
cùng một cấu hình), nên một lần chạy là không đủ.

---

## 6. Những gì KHÔNG cần kiểm định

- **HF.** Bằng 0 theo thiết kế với CUT-HIDE (bước 8 kiểm lại sau mỗi lần chạy). Nói một lần,
  không đưa vào bảng so sánh (quy tắc 2.4.2).
- **Ghost của phép che.** Bằng 0 theo P5(a). Đây là hệ quả của định lý, không đo như một phát
  hiện (quy tắc 1.2.2). Chỉ nhánh xoá ở SC5 mới báo cáo Ghost.
- **\|R\| của các phương pháp chính xác.** Bằng \|R\|\* theo P1. Chênh lệch ở đây là lỗi cài đặt,
  không phải kết quả.

---

## 7. Thứ tự chạy đã định

1. SC1 phải xanh (0 ca lệch) trước mọi kịch bản khác. **Đã xong**, 1.500 thực thể, 0 ca lệch.
2. SC2 chạy trước SC3 và quyết định phân nhóm. **Đã xong**, 16 cấu hình; phân nhóm ở §3.
3. SC3, SC4, SC5, SC7, SC8: số bất biến theo máy.
4. SC6 chạy sau cùng, trên máy đo đã khai, với `--machine=<tên máy>`.

Trước lần chạy SC3 đầu tiên lấy số vào bài, phải xong ba việc kỹ thuật ghi ở
`config/protocol.yaml`: gắn `HIDENOSP_REVISION` vào dấu định danh lần chạy, nâng ℓ_max lên 4, và
kiểm rằng B_ε không rỗng ở mọi cấu hình.

---

## 8. Xử lý kết quả âm

Nếu trọng số nút thiết yếu không cải thiện Lost (rủi ro đã lường ở §12), báo cáo kết quả âm
đủ ba điều kiện của quy tắc 3.4.9. Đóng góp lý thuyết (bài toán, chỉ mục, P1–P7) không phụ
thuộc vào chiều của kết quả này.

Tương tự với H3: xem mục lường trước ở §2.

---

## 9. Bổ sung ngày 25/09/2026 — cách lấy mẫu tập nhạy cảm (trước mọi lần chạy SC3 lấy số)

**Lý do bổ sung.** `protocol.yaml` dòng 1 chốt năm phân vị sup_NO, mỗi phân vị 4 tập, nhưng mã
trước ngày này **không** thực hiện điều đó: nó trộn ngẫu nhiên kho ứng viên cho từng tập, độc lập
nhau, nên có thể lặp tập. Đo trên dữ liệu thật (`results-probe/sbsh_feasibility_and_sampling_20260925.md`):
trên bộ gen, 20 tập mỗi ô chỉ cho 1 đến 14 tập khác nhau tuỳ cấu hình, vì kho ứng viên chỉ có 4
đến 23 mẫu. Đếm tập lặp như những đơn vị riêng sẽ thổi phồng cỡ mẫu của phép Wilcoxon.

**Thời điểm.** Chưa có lần chạy SC3 lấy số nào. Các lượt SC3 đã chạy đều là thăm dò: trên dữ liệu
tổng hợp trước ngày chốt, và ba lượt thăm dò khả thi ngày 25/09 (DBSH và SBSH trên bộ gen và
MSNBC), trong đó chỉ cột HF được đọc.

**Quy tắc chốt**, nay đã cài ở `hidenosp.exp.SensitiveSampling` (test: `SensitiveSamplingTest`):

1. Kho ứng viên là các mẫu của F_σ dài từ 2 tới 4 và không lặp mục (phạm vi μ(P) = 1 ở §1).
2. Kho được sắp theo sup_NO tăng dần (hoà thì theo thứ tự mẫu) và cắt thành năm tầng bằng nhau
   theo hạng, tâm ở p10, p30, p50, p70, p90.
3. Mỗi tầng cho tối đa **4 tập khác nhau** cỡ |𝒮|, rút từ riêng tầng đó bằng hạt giống 2026 (mỗi
   tầng một bộ sinh suy ra từ hạt giống). Tầng có ít mẫu hơn |𝒮| không cho tập nào; tầng có ít tổ
   hợp hơn 4 thì cho mọi tổ hợp.
4. **Không bao giờ lặp một tập để đủ 20.** Ô (cấu hình, |𝒮|) nào rút được ít hơn 20 tập vẫn chạy
   với số tập có được; số đó ghi ở đầu tệp kết quả và phải có trong bảng của bài. Ô không rút được
   tập nào thì không tồn tại và không chạy; bài ghi rõ vì sao (cỡ kho ứng viên).
5. Đơn vị lặp của H2 vẫn là (bộ dữ liệu, σ, khoảng cách, tập nhạy cảm), nay mọi đơn vị là khác
   nhau. Tập rút thiếu không phải "thực thể bị loại" của §5, nên không tính vào ngưỡng 10%.

Tuỳ chọn dòng lệnh đổi theo: `--sensitiveSets` bỏ, thay bằng `--setsPerStratum` (mặc định 4).

**Không đổi:** δ_L, họ Holm bốn phần tử, ngưỡng phân nhóm, các biên tương đương.

---

## 10. Bổ sung ngày 25/09/2026 — nhóm dữ liệu tính trên mẫu không lặp mục (trước mọi lần chạy SC3 lấy số)

**Quyết định của người viết**, sau khi đo ba nhật ký tiến trình mới: trung vị ρ_rep và β của một
cấu hình được lấy trên **mẫu không lặp mục**, không phải trên mọi mẫu SC2 lấy ra.

**Lý do.** SC3 chỉ ẩn mẫu không lặp mục (mục 1 và mục 9), nên nhóm phải mô tả đúng loại mẫu đó.
Trung vị trên mọi mẫu trộn cả mẫu lặp mục, và có bộ mà hai loại khác hẳn nhau: BPI 2013 ở [0,1]
σ=1000 có trung vị 0,254 trên mọi mẫu nhưng 0,021 trên mẫu không lặp mục
(`results-probe/SC2_20260925-122652-529`). Cách cũ xếp cấu hình đó vào nhóm chính vì những mẫu mà
SC3 không bao giờ ẩn. Cách mới bất lợi hơn cho bài, nên không phải chọn sau khi nhìn để có lợi.

**Quy tắc chốt**, cài ở `tools/thong_ke.py` và SC2 (`--profile=pool`):

1. Với mỗi cấu hình (bộ dữ liệu, σ, [g⁻,g⁺]), trung vị ρ_rep và trung vị β lấy trên **toàn bộ**
   kho ứng viên của mục 9 — mọi mẫu của F_σ dài 2 tới 4, không lặp mục — chứ không trên một mẫu
   con. SC2 chạy với `--profile=pool` ghi ra đúng kho đó; tệp SC2 lấy mẫu con không dùng để xếp nhóm.
2. Ngưỡng giữ nguyên như mục 3: nhóm chính ρ_rep ≥ 0,20; suy biến ≤ 0,05; β ≥ 0,20 có hiệu lực,
   ≤ 0,05 suy biến.
3. Bảng nhóm của từng cấu hình được sinh **một lần** từ các lượt SC2 chính thức trên máy đo
   (`python tools/thong_ke.py --sc2 results-invariant --write-groups`) thành
   `config/nhom_cau_hinh.tsv`, và commit tệp đó là chốt. Phân tích so từng cấu hình với bảng; lệch
   là dừng. Bảng thay cho các con số 12/7/6 ở mục 3.
4. **H2 kiểm trên nhóm chính.** Mục 3 đã ghi "nhóm trung gian không dùng để kiểm H2 hay H3", và bản
   thiết kế (§8.4) để nhóm suy biến ngoài nhóm chứng minh đóng góp; mã phân tích trước ngày này gộp
   mọi cấu hình vào H2, trái với cả hai. Nay chỉ hàng của nhóm chính vào Wilcoxon; các nhóm khác báo
   cáo mô tả. Đây là thi hành điều đã chốt, không phải quyết định mới.

**Thời điểm.** Chưa có lần chạy SC3 lấy số nào; chưa có tệp SC2 chính thức nào ở chế độ toàn kho.

**Không đổi:** δ_L, họ Holm bốn phần tử, các ngưỡng, các biên tương đương, cách lấy mẫu của mục 9.

---

## 11. Bổ sung ngày 26/09/2026 — phạm vi mẫu lặp mục (trước mọi lần chạy SC3 lấy số trên phạm vi này)

**Quyết định của người viết** (26/09/2026): duyệt nguyên đề xuất soạn ngày 25/09 — mở phạm vi sang mẫu lặp
mục; phương pháp của phép kiểm H2-R là `cut-hide`; tệp protein SDB4 vào phép kiểm, SDB1 chỉ báo cáo; và báo
cáo mặt Pareto của CUT-HIDE như một phân tích mô tả (điểm 6 dưới đây).

**Khai xuất xứ.** Trước khi mục này được viết, người đề xuất mục này **đã thấy** các lượt thăm dò trên máy phát triển:
`results-probe/repeated_item_exactness_probe_20260925.txt` (lời giải chính xác so với lời giải lát cắt trên
mẫu lặp mục) và `results-probe/repeated_item_scope_probe_20260925.md` (nhóm dữ liệu trên kho mẫu lặp mục;
SC3 lặp mục trên năm cấu hình, bảy phương pháp). Chiều đã thấy: mọi biến thể CUT-HIDE mất ít mẫu hơn ba đường
cơ sở; `cut-hide` mất nhiều mẫu hơn biến thể quét λ ở bốn trên năm cấu hình. Vì vậy mục này **không** đặt giả
thuyết có hướng nào giữa các biến thể của CUT-HIDE, và phương pháp của H2-R được chọn theo mục tiêu của bài toán
(\|R\| trước), không theo số đã thấy. **Chưa có** số mặt Pareto nào khi mục này được viết.

1. **Phạm vi.** Mẫu nhạy cảm lặp mục (μ(P) ≥ 2), dài 2 tới 4, thuộc F_σ, với g⁻ = 0 (mọi cấu hình của giao
   thức). Ở đó Định lý P8 của bản thiết kế cho số lần che tối thiểu chính xác, nên \|R\|* và tỉ số \|R\|/\|R\|*
   có nghĩa như ở phạm vi chính. Phạm vi chính (μ(P) = 1, mục 1) không đổi.
2. **Nhóm dữ liệu.** Như mục 10, trên kho mẫu lặp mục (SC2 `--profile=repeated`), cùng ngưỡng của mục 3. Bảng
   nhóm riêng `config/nhom_cau_hinh_lap_muc.tsv`, sinh một lần từ lượt SC2 chính thức trên máy đo, commit là chốt.
3. **Lấy mẫu tập nhạy cảm.** Như mục 9, trên kho mẫu lặp mục (SC3 `--patternKind=repeated`).
4. **H2-R.** Như H2 (mục 2): trên nhóm chính **của phạm vi lặp mục**, trung vị hiệu Lost_rel(đường cơ sở) −
   Lost_rel(`cut-hide`) > δ_L — *sửa ở mục 12 (26/09/2026): phương pháp đang kiểm là `cut-hide-rw-ls`*; cùng họ Holm bốn phần tử (HHA, DBSH, SBSH-r1, SBSH-rmax), cùng α = 0,05, δ_L =
   0,02, Wilcoxon một phía, rank-biserial. Kiểm **riêng**, không gộp hàng với H2. Hàng của hai phạm vi được
   tách theo **chính các mục của mẫu nhạy cảm** trong từng hàng SC3, không theo tên tệp hay tuỳ chọn.
5. **Tệp protein.** SDB4 (`protein_sdb4`) vào H2 và H2-R; SDB1 (`protein_sdb1`) chỉ báo cáo mô tả, không vào
   phép Wilcoxon nào, vì hai tệp chung 64–75% chuỗi.
6. **Mặt Pareto (mô tả, không kiểm định).** Với mỗi tập nhạy cảm một mẫu, ở cả hai phạm vi, tính **chính xác**
   các đỉnh của mặt Pareto lồi giữa \|R\| và trọng số đại diện w(R) của CUT-HIDE (Mệnh đề P10 của bản thiết kế,
   cùng trọng số như `cut-hide`), và với mỗi đỉnh: \|R\|, w, Lost_rel, SupDist. Báo cáo: số đỉnh; khoảng
   \|R\|/\|R\|* và Lost_rel trên mặt; với mỗi đường cơ sở của họ Holm, tỉ lệ tập mà có một đỉnh trội hơn nó
   (không kém ở cả \|R\| và Lost_rel, hơn hẳn ở ít nhất một). Không đỉnh nào thay `cut-hide` trong H2 hay H2-R.
7. **Mở rộng mục 6.** \|R\| = \|R\|* của `cut-hide` trên phạm vi lặp mục là hệ quả của Định lý P8: báo cáo như
   phép kiểm cài đặt, không kiểm định. Tỉ số \|R\|(`cut-hide-sweep`)/\|R\|* — cái giá của việc không có P8 — báo
   cáo mô tả theo từng cấu hình.

**Không đổi:** mọi mục 1–10.

---

## 12. Bổ sung ngày 26/09/2026 — phương pháp đang kiểm của H2 và H2-R (trước mọi lần chạy SC3 lấy số)

**Quyết định của người viết** (26/09/2026): chọn phương án (A) — phương pháp đang kiểm của H2 (mục 2) và H2-R
(mục 11, điểm 4) đổi từ `cut-hide` sang `cut-hide-rw-ls`, gồm trọng số đại diện thích nghi và tìm kiếm cục bộ
theo điều kiện cứu được (Bổ đề L9 của bản thiết kế v0.9).

**Lý do.** Bản thiết kế v0.9 chứng minh (Mệnh đề P11, quy dẫn từ MAX-CUT) rằng chọn tập che tối thiểu mất ít mẫu
nhất là NP-khó ngay cả với một mẫu nhạy cảm hai mục khác nhau, nên không trọng số cố định nào cộng theo vị trí có
thể bám đúng số mẫu mất; nguyên nhân là tác động theo cặp của các lần che. `cut-hide` dùng đúng một trọng số cố định
như thế. `cut-hide-rw-ls` dùng mức giảm thật của lời giải trước để tính lại trọng số, rồi đổi tập che từng một hoặc
hai chuỗi với số mẫu mất tính chính xác.

**Định nghĩa `cut-hide-rw-ls`** (mã: `hidenosp.hide.AdaptiveCutHide` với `withLocalSearch`,
`hidenosp.hide.MaskLocalSearch`; khoá SC3 `cut-hide-rw-ls`):
1. *Một mẫu nhạy cảm* (\|𝒮\| = 1; HIDE-DB; g⁻ = 0 ở mọi cấu hình):
   - vòng 0 đúng là `cut-hide` (trọng số §9.2 bước 4 trên B_ε với ε = 0,5; lời giải chính xác của P8);
   - vòng 1 tới 5: w(i) = Σ c_Q trên các Q ∈ F_σ \ 𝒮⁺ mà i thiết yếu với Q, với c_Q = 1/m_Q + s(1 − s)/τ_Q,
     s = 1/(1 + exp(−(d_Q − m_Q + ½)/τ_Q)), τ_Q = max(1, m_Q/4), m_Q = sup_NO(Q, D) − σ + 1, d_Q là mức giảm
     chính xác của Q dưới lời giải vòng trước; giải lại P8 với số lần che trước. Giữ lời giải có Lost nhỏ nhất
     trong sáu vòng (hoà thì vòng sớm nhất);
   - tìm kiếm cục bộ từ lời giải ấy (Thuật toán LS của bản thiết kế): nước một chuỗi và nước phân bổ, tối đa
     50 lượt, 30 chuỗi nhận rẻ nhất; bỏ mọi nước mà Bổ đề L9 chứng minh không thể giảm Lost.
2. *Nhiều mẫu nhạy cảm* (\|𝒮\| ∈ {5, 10, 20}): tham lam của §9.2 bước 7, **không đổi** — trùng `cut-hide`, vì
   trọng số thích nghi và tìm kiếm cục bộ dựa trên lời giải chính xác của P8 cho một mẫu. Cột `method` của hàng SC3
   ghi rõ nhánh đã chạy.

**Tham số chốt, không chỉnh sau khi thấy số:** sáu vòng (kể cả vòng 0); công thức c_Q và τ_Q như trên; 50 lượt;
30 chuỗi nhận; ε = 0,5 như `config/protocol.yaml`. Mã giữ chúng ở `AdaptiveCutHide.DEFAULT_ITERATIONS`,
`MaskLocalSearch.DEFAULT_PASSES`, `MaskLocalSearch.DEFAULT_RECEIVERS`.

**Tính chất có chứng minh, báo cáo như phép kiểm cài đặt (mục 6), không kiểm định:** trên mọi thực thể một mẫu,
\|R\|(`cut-hide-rw-ls`) = \|R\|* và Lost(`cut-hide-rw-ls`) ≤ Lost(`cut-hide`): vòng 0 là `cut-hide`, và mỗi bước
sau chỉ được nhận khi Lost giảm. Vì vậy trên **từng** thực thể, hiệu Lost_rel(đường cơ sở) − Lost_rel(phương pháp
đang kiểm) không nhỏ hơn hiệu tính với `cut-hide`. Việc đổi phương pháp là trội yếu theo cấu trúc, không phải một
lựa chọn giữa hai phương pháp ngang hàng dựa trên số.

**Khai xuất xứ.** Trước khi mục này được viết, người đề xuất mục này **đã thấy** các lượt thăm dò trên máy phát triển ở đúng tám
cấu hình × 20 tập của thăm dò mặt Pareto (bộ gen [0,3] σ=2500 và protein SDB4 [0,3] σ=500 ở cả hai phạm vi, MSNBC
[0,1] σ=2000 ở cả hai phạm vi, Sepsis [0,1] σ=300 và BPI 2013 [0,1] σ=500 ở phạm vi lặp mục), và 480 thực thể nhỏ cắt
từ các bộ ấy: `results-probe/surrogate_candidates_probe_20260926.txt`, `lost_small_exact_probe_20260926.txt`,
`lost_local_search_probe_20260926.txt`, `pareto_exceptions_rw_probe_20260926.txt`. Chiều đã thấy: phương pháp mới
mất ít mẫu hơn `cut-hide` ở một phần các tập và không nhiều hơn ở tập nào (đúng theo cấu trúc). Sáu vòng, công
thức τ_Q và 30 chuỗi nhận được đặt khi viết từng bộ thăm dò, trước khi chạy nó. **Hai điều được quyết sau khi thấy
số thăm dò**, khai ở đây: (a) thêm nước phân bổ vào tìm kiếm cục bộ, sau khi thấy bản chỉ có nước một chuỗi trên
thực thể nhỏ; (b) số lượt 50 (thăm dò thực thể nhỏ dùng 40, thăm dò dữ liệu đầy đủ dùng 50). Bộ lọc L9 không thay
đổi nước nào được chọn (chứng minh ở Bổ đề L9, kiểm ở `MaskLocalSearchTest`).

**Báo cáo thêm (mô tả, không kiểm định):** `cut-hide` và `cut-hide-rw` chạy trong cùng lượt SC3 như hai biến thể
loại trừ; bảng mô tả Lost_rel của ba biến thể và số tập mà mỗi bước (trọng số thích nghi, tìm kiếm cục bộ) làm
Lost giảm. SC6 đo thời gian của cả `cut-hide` và `cut-hide-rw-ls` trên máy đo.

**Thời điểm.** Chưa có lượt SC3 chính thức nào. Mọi số đã thấy là thăm dò trên máy phát triển.

**Không đổi:** δ_L, α, Wilcoxon một phía với Pratt, họ Holm bốn phần tử, cách lấy mẫu (mục 9), nhóm dữ liệu (mục 10
và 11), các biên tương đương, mục 11 điểm 5; mục 11 điểm 6 (mặt Pareto) vẫn tính với trọng số của `cut-hide` và
vẫn là phân tích mô tả.

---

## 13. Ghi chú biên tập ngày 26/09/2026 — đổi tên tệp, không đổi quy tắc nào

Mã nguồn chuyển hẳn sang tiếng Anh, nên ba tên mà mục 10 và mục 11 nhắc tới đổi như sau:

| Tên ở mục 10 và 11 | Tên từ ngày 26/09/2026 |
|---|---|
| `tools/thong_ke.py` | `tools/preregistered_analysis.py` |
| `config/nhom_cau_hinh.tsv` | `config/configuration_groups.tsv` |
| `config/nhom_cau_hinh_lap_muc.tsv` | `config/configuration_groups_repeated.tsv` |

Lệnh sinh bảng nhóm ở mục 10 điểm 3 nay là
`python tools/preregistered_analysis.py --sc2 results-invariant --write-groups`. Nhãn nhóm ghi trong
hai bảng cũng sang tiếng Anh: với ρ_rep, `main` (chính), `intermediate` (trung gian), `degenerate`
(suy biến); với β, `effective` (có hiệu lực), `between` (trung gian), `degenerate` (suy biến). Khi
ghi chú này được viết, chưa bảng nhóm nào được sinh, nên không bảng chốt nào phải viết lại.

**Không đổi:** mọi mục 1–12; mọi ngưỡng, phép kiểm, cách ghép cặp và cách lấy mẫu.

---

## 14. Bổ sung ngày 02/10/2026 — phạm vi, phương pháp, giới hạn thời gian và thứ tự của đợt SC3 chính thức (trước mọi lần chạy SC3 lấy số)

**Quyết định của người viết** (02/10/2026), chọn trong các phương án người đề xuất đưa ra:

1. **Phạm vi chạy: chỉ nhóm chính.** SC3 chính thức chạy trên mọi dòng `group = main` của hai bảng đã chốt,
   `config/configuration_groups.tsv` (phạm vi chính, `--patternKind=distinct`) và
   `config/configuration_groups_repeated.tsv` (phạm vi lặp mục, `--patternKind=repeated`). Cấu hình nhóm trung gian
   và suy biến **không** chạy SC3 trong đợt này. H2 và H2-R vốn chỉ kiểm trên nhóm chính (mục 10 điểm 4, mục 11
   điểm 4), nên việc này không đổi phép kiểm nào; nó bỏ phần báo cáo mô tả của các nhóm khác mà mục 10 điểm 4 nhắc
   tới. Nếu sau này chạy các nhóm ấy thì chúng vẫn chỉ là mô tả. `protein_sdb1` thuộc nhóm chính, chạy, và vẫn chỉ
   báo cáo (mục 11 điểm 5).
2. **Phương pháp: bộ của H2.** Bảy khoá: `cut-hide-rw-ls` (phương pháp đang kiểm, mục 12); `cut-hide-rw` và
   `cut-hide` (hai biến thể loại trừ, mục 12); `hha`, `dbsh`, `sbsh-r1`, `sbsh-rmax` (họ Holm, mục 2). Không phương
   pháp nào ngẫu nhiên, nên quy ước mười hạt giống của mục 5 không áp dụng ở đợt này.
3. **Giới hạn thời gian: 1.800 giây** đồng hồ thực cho **một phương pháp trên một tập nhạy cảm**
   (`--timeLimit=1800`), tính từ lúc gọi phương pháp tới lúc nó trả lời giải; phần đánh giá lời giải và mặt Pareto
   (mục 11 điểm 6) không tính vào. Vượt giới hạn: hàng mang `status = OT` và để trống mọi cột kết quả; hết bộ nhớ:
   `status = OOM`. Phương pháp chạy xong thì `status` để trống. Xử lý theo bảng "Loại trừ thực thể" của mục 5,
   không đổi: loại cặp, đếm, báo cáo; quá 10% thì không kết luận. Trần bộ nhớ mọi lệnh SC3 là `-Xmx8g`.
   Cài đặt: kiểm hợp tác (`hidenosp.util.Deadline`) khi dựng một Nettree, khi che một vị trí, ở mỗi pha luồng cực
   đại, mỗi vị trí của quy hoạch động chính xác và mỗi lượt tìm kiếm cục bộ; phép kiểm `Sc3TimeLimitTest`.
4. **Thứ tự: \|𝒮\| = 1 trước**, ở cả hai phạm vi, kèm mặt Pareto (mục 11 điểm 6); rồi \|𝒮\| = 5, 10, 20. Thời gian
   của đợt \|𝒮\| = 1 dùng để lên lịch các đợt sau. Mọi tham số chốt ở mục này và mục 12 **không** đổi sau khi thấy
   kết quả của đợt \|𝒮\| = 1.

   > **Sửa cùng ngày (02/10/2026), trước mọi lượt SC3 chính thức — quyết định của người viết.** Mặt Pareto **tách
   > thành một đợt riêng**, không chạy kèm các phương pháp. Lý do đo được: chạy thử cả 54 ô nhóm chính của \|𝒮\| = 1
   > trên máy phát triển (một tập mỗi tầng; `results-probe/sc3_dry_run_all_cells_probe_20261002.txt`) cho thấy dựng mặt Pareto chiếm 88–95% thời gian của một ô — MSNBC [0,3]
   > σ=5000 phạm vi chính: 2.005 giây cho năm tập, trong đó bảy phương pháp chỉ 110 giây; một mặt có tới 1.145 đỉnh — và
   > phần ấy không nằm dưới giới hạn thời gian của điểm 3. Đợt phương pháp chạy `--pareto=false`; đợt mặt Pareto chạy
   > `--methods=none --pareto=true` trên cùng các ô và cùng cách lấy mẫu (mục 9), nên cùng các tập nhạy cảm. Nội dung của
   > mục 11 điểm 6 không đổi; chỉ đổi lúc chạy. Bộ sinh: `tools/sc3_commands.py` (đợt phương pháp) và
   > `tools/sc3_commands.py --pareto` (đợt mặt Pareto). Thứ tự: đợt phương pháp \|𝒮\| = 1 trước, rồi đợt mặt Pareto hoặc
   > \|𝒮\| = 5, 10, 20 tuỳ thời gian máy.

Lệnh của từng ô do `tools/sc3_commands.py` sinh từ hai bảng nhóm, không chép tay; công cụ ấy bỏ ô đã có tệp kết quả
và liệt kê tệp SC3 không khớp ô nào.

**Hệ quả với tính bất biến theo máy.** Hàng không có `OT` hay `OOM` vẫn là đại lượng bất biến theo máy. **Tập hàng
`OT` thì phụ thuộc máy**, và tập hàng `OOM` phụ thuộc trần bộ nhớ. Vì vậy lượt SC3 chính thức chạy trên máy đo đã
khai ở hàng 5 của `config/protocol.yaml`, mang `--machine` của máy ấy, và số ca `OT`/`OOM` được báo cáo kèm tên máy
và trần bộ nhớ.

**Khai xuất xứ.** Khi mục này được viết, người đề xuất đã thấy, ngoài các lượt thăm dò kê ở mục 12, hai lượt thăm
dò trên máy phát triển: `results-probe/sc3_time_limit_stop_probe_20261002.txt` (giới hạn 2 giây trên MSNBC
[0,10]: mọi phương pháp dừng chưa tới 0,05 giây sau hạn) và `results-probe/sc3_heavy_cells_timing_probe_20261002.txt` (bảy cấu hình nhóm chính chọn làm ca nặng: ba kho mẫu lặp mục lớn nhất, hai kho mẫu không lặp mục lớn nhất, bộ gen [0,10] σ=2500 lặp mục và MSNBC [0,10] σ=5000 không lặp mục; năm tập mỗi cấu hình, giới hạn 600 giây: không hàng nào `OT`; lâu nhất 42,7 giây cho một phương pháp trên một tập, của `cut-hide-rw-ls` trên MSNBC [0,10] σ=1000 phạm vi lặp mục). Hai lượt ấy có in Lost của các phương pháp ở các tập đã chạy; chiều đã thấy giống mục 12. Giá trị 1.800 giây được đặt **trước** lượt đo thứ hai và không đổi sau nó. Chưa có lượt SC3 chính thức nào.

**Không đổi:** mọi mục 1–13.
