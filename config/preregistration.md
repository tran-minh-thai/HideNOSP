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
