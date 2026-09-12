# Đề xuất giá trị cụ thể cho nhóm A (các quyết định chặn SC3)

Đây là **đề xuất để bạn duyệt**, chưa phải bản chốt. Chốt là việc ghi con số vào
`preregistration.md` và `protocol.yaml` rồi commit — lịch sử git làm bằng chứng thời điểm.

Mỗi mục có bốn phần: **Đề xuất**, **Căn cứ** (số đo được, không phải cảm tính), **Cái giá phải
trả**, và **Cách bác bỏ** — điều gì xảy ra thì con số này sai.

---

## Một cảnh báo phải đọc trước

Quy tắc 2.4.7 tồn tại để ngăn việc chốt ngưỡng **sau khi** đã thấy chiều của kết quả so sánh.
Tôi phải khai: trong quá trình dựng khung, tôi **đã chạy và đã thấy** một lần SC3 dò tìm trên dữ
liệu tổng hợp thay thế, trong đó CUT-HIDE mất ít mẫu hơn các baseline.

Vì vậy mọi con số dưới đây được neo vào **thang của dữ liệu** (cỡ của `F_σ`, khoảng trống trong
phân bố ρ_rep), **không** neo vào khoảng cách đã quan sát giữa các phương pháp. Nếu bạn muốn tệp
chốt trước sạch tuyệt đối về mặt phương pháp luận, hãy tự chọn lại δ_L và biên tương đương; hai
con số đó là chỗ duy nhất mà việc đã thấy chiều có thể gây thiên lệch.

Cách hiệu chỉnh **hợp lệ** nếu muốn căn cứ thực nghiệm cho δ_L: chạy **một** phương pháp duy
nhất trên nhiều tập nhạy cảm và đo độ tản của Lost giữa các tập. Việc đó không lộ chiều so sánh,
và δ_L nên ít nhất bằng độ tản đó.

---

## A1. Ngưỡng hiệu ứng thực tiễn δ_L cho H2

**Đề xuất.** Đổi đại lượng của H2 sang **Lost tương đối**:

> Lost_rel = Lost / |F_σ(D) \ 𝒮⁺|, với mẫu số là số mẫu phổ biến không nhạy cảm trước khi sửa.

và đặt **δ_L = 0,02** (hai điểm phần trăm). Báo cáo kèm trung vị hiệu **tuyệt đối** trong cùng
bảng, để người đọc thấy cả hai thang.

**Căn cứ.** |F_σ| đo được trải hơn một bậc: 51 (dna σ=2500) tới 672 (msnbc σ=1000). Một δ_L
tuyệt đối duy nhất sẽ nghiêm với dna (2 mẫu = 4% của 51) và lỏng với msnbc (2 mẫu = 0,3% của
672); trộn hai thang đó vào một phép Wilcoxon là trộn hai thứ khác nhau. Chuẩn hoá theo
|F_σ \ 𝒮⁺| làm đơn vị lặp đồng nhất. Mẫu số này **đã có sẵn** ở cột `frequent_before` của SC3,
không phải sửa mã đo.

Vì sao 0,02 chứ không nhỏ hơn: 1 điểm phần trăm trên dna (|F| ≈ 51) chưa tới nửa mẫu, nên mọi
chênh lệch dù chỉ một mẫu cũng vượt ngưỡng và δ_L mất hết tác dụng. 0,02 tương ứng khoảng 1 mẫu
trên dna và 6,5 mẫu trên msnbc σ=2000.

**KHÔNG dùng** Σ|S| hay |R|\* làm mẫu số: Σ|S| là mẫu số của DistR, |R|\* là cận số lần che.
Lấy chúng làm mẫu số cho Lost là lỗi định nghĩa.

**Cái giá.** Trên bộ có |F_σ| nhỏ, một mẫu chênh lệch đã gần chạm ngưỡng, nên H2 dễ được ủng hộ
ở đó hơn. Phải nói rõ điều này trong phần hạn chế.

**Cách bác bỏ.** Nếu độ tản của Lost giữa các tập nhạy cảm (đo bằng một phương pháp duy nhất)
lớn hơn 2 điểm phần trăm, thì δ_L = 0,02 nằm trong nhiễu và phải nâng lên.

---

## A2. Ngưỡng ρ_rep và β, đơn vị áp, và vùng ở giữa

**Đề xuất.**

| Mục | Giá trị |
|---|---|
| Đơn vị áp ngưỡng | **cấu hình** = bộ ba (bộ dữ liệu, σ, [g⁻,g⁺]) |
| Thống kê gộp | **trung vị** ρ_rep trên các mẫu nhạy cảm đã lấy của cấu hình đó |
| Nhóm chính | ρ_rep ≥ **0,20** |
| Đối chứng suy biến | ρ_rep ≤ **0,05** |
| Vùng giữa | 0,05 < ρ_rep < 0,20 → **nhóm trung gian**, không dùng kiểm H2/H3 |
| Ràng buộc có hiệu lực | β ≥ **0,20** |
| Đối chứng ràng buộc suy biến | β ≤ **0,05** |

**Căn cứ.**

*Đơn vị là cấu hình, không phải bộ.* Trên msnbc σ=2000, đổi khoảng cách từ [0,1] sang [0,10]
làm β tụt từ 0,432 xuống 0,043. β là tính chất của **cấu hình**, không của bộ dữ liệu; gán vai
cho "bộ" sẽ gán sai. Cùng lẽ đó, ρ_rep trên msnbc đi từ 0,190 (σ=1000) lên 0,395 (σ=5000).

*Vì sao 0,05 và 0,20.* Trung bình ρ_rep đo được, sắp tăng dần: 0,000 (sign, bms1), 0,008 (fifa),
0,052 (bike), 0,153 (bible), 0,154 (leviathan), 0,323 (msnbc), 0,786 (dna). Có **hai khe thưa**
trong phân bố: giữa 0,052 và 0,153, và giữa 0,154 và 0,323. Hai ngưỡng đề xuất rơi vào hai khe
đó, nên kết luận phân nhóm **không nhạy** với việc xê dịch ngưỡng vài phần trăm. Chọn ngưỡng ở
chỗ thưa của phân bố là chọn theo nguyên tắc; nó khác hẳn với chọn con số vừa đủ để bộ mình thích
lọt vào nhóm mình muốn.

*Vì sao trung vị chứ không trung bình.* Phân bố lệch: msnbc [0,3] σ=2000 trải 0,106–0,575. Trung
vị tính được ngay từ CSV của SC2 (mỗi dòng một mẫu), không phải sửa mã.

**Hệ quả với dữ liệu hiện có.** Nhóm chính: **dna, msnbc**. Trung gian: **bible, leviathan**.
Suy biến: **sign, bms1, fifa, bike**.

**Cái giá.** Nhóm trung gian không dùng để kiểm giả thuyết, nên công sức chạy bible và leviathan
chỉ đổi lấy một hình minh hoạ rằng ρ_rep và β độc lập nhau. Đó vẫn là điều đáng in.

**Cách bác bỏ.** Nếu một bộ mới rơi đúng vào 0,05–0,20 và quan trọng với bài, thì ranh giới đang
cắt qua chỗ đông chứ không phải chỗ thưa, và phải chọn lại.

---

## A4. Biên tương đương cho Lost và SupDist

**Đề xuất.** Lost: **±0,02** trên thang tương đối, tức bằng đúng δ_L. SupDist: **±0,01**.

**Căn cứ.** Biên tương đương nên bằng ngưỡng hiệu ứng; nếu đặt nhỏ hơn sẽ sinh một vùng rộng mà
kết quả "vừa không khác biệt vừa không tương đương", tức không kết luận được gì. SupDist vốn là
tỉ số không thứ nguyên trong [0,1] nên ±0,01 là một điểm phần trăm độ hỗ trợ.

**Thành thật:** ±0,01 cho SupDist là con số **yếu căn cứ nhất** trong toàn bộ tài liệu này. Nên
đặt lại sau khi đo độ tản của SupDist bằng một phương pháp duy nhất (xem cách hiệu chỉnh hợp lệ
ở đầu tệp).

---

## A5. Nghĩa định lượng của "gần 1" trong H3

**Đề xuất.** Với tỉ số r = |R| / |R|\*: "vượt 1" nghĩa là **r ≥ 1,10**; "gần 1" nghĩa là
**r ≤ 1,05**. Vùng 1,05 < r < 1,10 không kết luận.

**Căn cứ.** H3 cần một vùng đệm giữa hai vế, nếu không thì mọi r > 1 dù nhỏ đều thành "vượt".
5% và 10% là hai mốc quy ước, và vùng đệm giữ cho hai vế loại trừ nhau.

**Cảnh báo.** Trên lần chạy dò, mọi phương pháp chính xác **và** heuristic đều cho r = 1,000 —
tức không baseline nào dùng thừa lần che. Nếu điều đó lặp lại trên dữ liệu thật thì **H3 không
bác bỏ được**, và đó là một kết quả âm phải báo cáo chứ không phải một lỗi. Nên chuẩn bị trước
câu chữ cho tình huống này.

---

## A6. Họ baseline đưa vào hiệu chỉnh Holm

**Đề xuất.** Họ = **bốn** baseline tầng (b): HHA, DBSH, SBSH-r1, SBSH-rmax. Hiệu chỉnh Holm
trên 4 phép so. Tầng (a) (occurrence, HH, HR, RH, RR) báo cáo **mô tả**, không kiểm định.

**Căn cứ.** Tầng (a) tồn tại để chứng minh bài toán không tự giải (§11.3), không phải để kiểm
giả thuyết; đưa chúng vào họ chỉ làm Holm khắt khe hơn mà không thêm nội dung. Họ 4 giữ được lực
kiểm định cho đúng những so sánh mà bài dựa vào.

**Cách bác bỏ.** Nếu SBSH không đối chiếu được với [R3] (xem A25) thì họ còn 2, phải khai lại
trước khi chạy.

---

## A7. Ghép cặp H2: cùng ngân sách |R| hay tự chọn

**Đề xuất.** Chạy mỗi phương pháp **tới khi HF = 0** với |R| tự chọn, rồi tách hai chế độ báo cáo:

1. Thực thể mà mọi phương pháp dùng **cùng** |R|: đưa vào Wilcoxon cho H2.
2. Thực thể mà baseline dùng **nhiều hơn**: báo cáo riêng dạng cặp (|R|, Lost), không kiểm định.
   Kết luận ở đó mạnh hơn và không cần thống kê: baseline vừa che nhiều hơn vừa mất nhiều hơn.

**Căn cứ.** So Lost ở hai mức |R| khác nhau là so hai thứ khác nhau. Ép baseline dừng đúng ở
|R|\* thì nó chưa ẩn xong (HF > 0), và khi đó Lost cũng không so được. Tách hai chế độ là cách
duy nhất giữ cho mỗi so sánh có nghĩa.

---

## A8. Quy ước tính Wilcoxon

**Đề xuất.**

| Mục | Giá trị |
|---|---|
| Hiệu đúng bằng 0 | dùng **Pratt** (giữ lại khi xếp hạng), không dùng Wilcoxon gốc (loại bỏ) |
| Hạng buộc | hạng trung bình |
| Cỡ hiệu ứng | rank-biserial = (W⁺ − W⁻)/(W⁺ + W⁻) |
| Mức ý nghĩa | α = 0,05, một phía |

**Căn cứ.** Lost là số nguyên nhỏ nên số cặp có hiệu bằng 0 sẽ nhiều. Wilcoxon gốc vứt chúng đi,
làm mất thông tin "hai phương pháp ngang nhau" và thổi phồng cỡ hiệu ứng. Pratt giữ lại.

---

## A9. Quy tắc loại trừ thực thể hỏng

**Đề xuất.**

| Tình huống | Xử lý |
|---|---|
| Một phương pháp còn HF > 0 | **loại cặp** khỏi Wilcoxon, đếm và báo cáo riêng |
| Quá giới hạn thời gian | loại cặp, ghi mã OT, báo cáo số ca |
| Hết bộ nhớ | loại cặp, ghi mã OOM, báo cáo số ca |
| Tỉ lệ thực thể bị loại > 10% | **không kết luận** H2, và nói rõ vì sao |

**Căn cứ.** Lost chỉ so được khi cả hai phương pháp đã ẩn xong; gán một giá trị phạt cho ca
HF > 0 là bịa dữ liệu. Ngưỡng 10% để tránh việc loại trừ âm thầm biến mẫu thành mẫu chọn lọc.

---

## A10. Số lần chạy lại của các phương pháp ngẫu nhiên

**Đề xuất.** HH, HR, RH, RR và GA: **10 lần** với 10 hạt giống khai trước (2026 tới 2035), lấy
**trung vị** Lost và |R| làm giá trị của phương pháp đó trên thực thể đó.

**Căn cứ.** Đây là đại lượng **bất biến theo máy**, nên bảng số lần lặp của §11.6 không áp dụng
— bảng đó dành cho đo thời gian. Biến thiên quan sát được ở RR là lớn (|R| = 31 và 151 trên hai
tập nhạy cảm khác nhau của cùng một cấu hình), nên một lần chạy là không đủ.

---

## A11. Lưới σ, ℓ_max, khoảng cách và |𝒮|

**Đề xuất.**

| Tham số | Giá trị |
|---|---|
| ℓ_max | **4** cho mọi bộ |
| Khoảng cách | [0,1] và [0,3] cho mọi bộ; thêm [0,10] cho dna và msnbc |
| σ dna | **2500, 4000** |
| σ msnbc | **1000, 2000, 5000** |
| σ leviathan / bible | 500 / 2000 |
| σ sign / bms1 / fifa | 200 / 100 / 3000 |
| \|𝒮\| | {1, 5, 10, 20}, mỗi ô 20 tập (đúng §11.5) |

**Căn cứ.**

*ℓ_max = 4* là bắt buộc để khớp §11.5, vốn nói tập nhạy cảm lấy mẫu **độ dài 2–4**. Với ℓ_max = 3
thì mẫu độ dài 4 không nằm trong F_σ nên không lấy mẫu được — đây là một chỗ **không nhất quán
đang có** giữa §11.5 và mọi lần chạy SC2 đã làm. Chi phí đo được:

| Bộ | σ | \|F\| ở ℓ_max=3 | giây | \|F\| ở ℓ_max=4 | giây |
|---|---|---|---|---|---|
| dna | 2500 | 51 | 0,7 | 67 | 1,2 |
| msnbc | 2000 | 327 | 8,7 | 556 | 20,1 |
| leviathan | 500 | 106 | 53,6 | 108 | 60,6 |
| bms1 | 100 | 652 | 6,7 | 662 | 7,4 |
| sign | 200 | 95 | 2,8 | 95 | 2,7 |
| fifa | 3000 | 78 | 123,0 | 79 | 123,6 |

Rẻ ở **cả sáu bộ**, không có ngoại lệ. Trên sign không có mẫu độ dài 4 nào phổ biến (95 → 95),
fifa chỉ thêm một mẫu và tốn thêm 0,6 giây, leviathan thêm hai mẫu. Lý do: chi phí khai phá dồn
ở **mức 2**, nơi số ứng viên là |L₁| × |Σ|; tới mức 4 thì phép chiếu đã thu hẹp tập chuỗi phải
quét nên mở rộng gần như miễn phí. Chỗ đắt nhất là fifa với |Σ| = 2990, và cái đắt đó đã phải trả
ngay ở ℓ_max = 3.

Nói cách khác, lý lẽ "ℓ_max = 3 cho rẻ" không đứng: nó không rẻ hơn đáng kể, mà lại làm mẫu độ
dài 4 của §11.5 không lấy mẫu được.

*σ cho dna phải ≥ 2500.* Với |Σ| = 4 và ℓ_max = 3, không gian mẫu chỉ có 4 + 16 + 64 = 84 mẫu,
và ở σ = 1000 thì |F_σ| = **84**, tức **mọi** mẫu đều phổ biến. Khi F_σ là toàn bộ không gian,
Ghost không thể khác 0 và Lost mất ý nghĩa so sánh. Ở σ = 2500 thì |F_σ| = 51 trên 84, là tập con
thực sự. Đây là ràng buộc chỉ lộ ra khi đo, không suy được từ thiết kế.

*Quy tắc chung để chọn σ:* giữ |F_σ| trong khoảng [50, 700] **và** F_σ là tập con thực sự của
không gian mẫu. Mọi σ đề xuất ở trên đều thoả.

---

## A12. Ngưỡng ε của B_ε

**Đề xuất.** **ε = 0,5** cho mọi bộ. Thêm một phép kiểm chạy-thời-gian: nếu B_ε rỗng thì **báo
lỗi**, không chạy tiếp im lặng.

**Căn cứ.** Số đo |B_ε| theo ε:

| Cấu hình | \|F_σ\| | ε=0,05 | ε=0,10 | ε=0,25 | ε=0,50 | ε=1,00 |
|---|---|---|---|---|---|---|
| dna [0,3] σ=1000 | 84 | 0 | **0** | 1 | 6 | 20 |
| dna [0,3] σ=2500 | 51 | 7 | 19 | 24 | 36 | 42 |
| dna [0,1] σ=1000 | 59 | 1 | 5 | 8 | 26 | 40 |
| msnbc [0,3] σ=1000 | 672 | 34 | 53 | 123 | 217 | 345 |
| msnbc [0,3] σ=2000 | 327 | 8 | 22 | 61 | 112 | 174 |
| msnbc [0,3] σ=5000 | 119 | 4 | 10 | 24 | 40 | 64 |
| leviathan [0,3] σ=500 | 106 | 6 | 11 | 26 | 42 | 66 |
| bms1 [0,3] σ=100 | 652 | 34 | 81 | 159 | 248 | 349 |

ε = 0,10 cho B_ε **rỗng** trên dna [0,3] σ=1000. Khi đó bước 4 gán trọng số 0 cho mọi vị trí và
CUT-HIDE âm thầm tụt về biến thể trọng số đều — tức biến thể đang được so ở SC7 lại chính là
biến thể đang chạy, mà không ai biết. Đây là loại lỗi im lặng nguy hiểm nhất.

ε = 1,00 kéo 24–54% của F_σ vào băng; khi đó trọng số 1/(sup − σ + 1) gần như đều và cụm từ "mẫu
gần biên" mất nghĩa.

ε = 0,50 cho |B_ε| từ 6 tới 248, tức 7–38% của F_σ, và **không rỗng ở mọi cấu hình đã đo**.

**Cái giá.** B_ε lớn làm bước 3 đắt hơn; §12 đã lường rủi ro này và §11.1 bắt SC6 báo cáo phân rã
chi phí theo pha. Với ε = 0,5, |B_ε| lớn nhất đo được là 248 (bms1, một bộ đối chứng).

---

## A13. Các phân vị sup_NO để lấy mẫu tập nhạy cảm

**Đề xuất.** Năm phân vị của sup_NO trên các ứng viên: **p10, p30, p50, p70, p90**; mỗi phân vị
**4 tập** → đúng 20 tập mỗi ô như §11.5 đòi. Ứng viên là mẫu độ dài 2–4 trong F_σ.

**Căn cứ.** §11.5 đã chốt 20 tập mỗi ô; năm phân vị đều nhau là cách chia đơn giản nhất cho ra
đúng 20 và phủ cả hai đầu phân bố. Số đo cho thấy phân bố sup_NO rất lệch (msnbc σ=2000: p10 =
2313, p50 = 3800, p90 = 13368, lớn nhất 115180), nên lấy mẫu chỉ ở đầu trên sẽ cho một bức tranh
sai.

---

## A14. Phạm vi μ(P)

**Đề xuất.** Giữ **μ(P) = 1** làm phạm vi chính của bài. Mẫu lặp mục báo cáo ở một mục riêng,
dùng cận P3 và bộ giải chính xác trên thực thể nhỏ, và khai rõ độ khó là câu hỏi mở.

**Căn cứ.** Trên dna, ρ_rep của mẫu **không lặp mục** là 0,806, **cao hơn** mẫu lặp mục 0,773.
Nghĩa là giữ phạm vi hẹp không còn phải trả giá bằng việc bỏ chế độ dữ liệu lặp cao — đó chính là
lý do đợt bổ sung dữ liệu được làm. Trong phạm vi μ = 1, P1, P2, P6(b) và lời giải chính xác đều
đứng vững.

**Kèm theo.** Trọng số nút khi μ ≥ 2 giữ nguyên cách cài hiện tại (gán trọng số vị trí cho mọi
nút của vị trí đó) và **khai là lựa chọn cài đặt**, vì P1 và P2 vốn không áp dụng ở đó.

**Cái giá.** Phải khai trong phần hạn chế rằng trên msnbc thì lớp lặp mục mới là lớp lặp nhiều
(0,434 so với 0,218), kèm đúng con số, để không bị đọc là né tránh.

---

## A15. Nhiều mẫu nhạy cảm dưới ngưỡng cơ sở dữ liệu

**Đề xuất.** **Làm tròn quy hoạch tuyến tính** làm phương án chính. GA lai là dự phòng, chỉ dùng
nếu LP không cài kịp. Nếu cả hai không kịp thì **thu phạm vi bài về |𝒮| = 1** và khai rõ, thay vì
đưa GA vào mà tham số chưa chốt.

**Căn cứ.** LP làm tròn có **bảo đảm hệ số ℓ_max**, đã nằm trong danh sách đóng góp §7. GA không
có bảo đảm nào và cần chốt trước ít nhất bốn tham số (cỡ quần thể, số thế hệ, tỉ lệ lai ghép, tỉ
lệ đột biến), tức làm phình thêm bề mặt quyết định đúng lúc đang cần thu hẹp nó.

---

## A16. "Thực thể nhỏ" và biến thể ILP làm mốc

**Đề xuất.** "Thực thể nhỏ" = thực thể có **≤ 64 vị trí ứng viên** và cần **≤ 12 lần che** —
đúng hai hằng số `MAX_CANDIDATES` và `MAX_MASKS` đã cài trong `IlpExact`. Ghi hai con số này vào
bảng giao thức. SC3 lấy **`exact-minR-minLost`** làm mốc tối ưu.

**Căn cứ.** Định nghĩa "nhỏ" bằng đúng giới hạn mà mã cưỡng chế thì không có khoảng cách giữa
tuyên bố và cài đặt. Chọn biến thể `minLost` vì mục tiêu thứ hai của §2 là số mẫu mất;
`exact-minR` chỉ trả lời "cần bao nhiêu lần che", mà câu đó P1 đã trả lời bằng công thức.

---

## A17. Gắn phiên bản mã vào tệp kết quả

**Đề xuất.** Thêm một script bọc đặt `HIDENOSP_REVISION = git rev-parse HEAD` rồi mới gọi
`Runner`. **Chạy lại** mọi lần chạy lấy số vào bài. Số hiện có (đóng dấu `unversioned`) giữ
nguyên làm số dò tìm.

**Căn cứ.** Mọi tệp kết quả hiện tại mang dấu `unversioned`, nên không truy được về commit nào
sinh ra chúng. Quy tắc 2.6.8 đòi dấu định danh lần chạy; dấu đó đang thiếu đúng thành phần quan
trọng nhất.

---

## A23. Gộp vào một lần chốt

**Đề xuất.** Ghi toàn bộ A1–A17 vào `preregistration.md` và `protocol.yaml` trong **một commit
duy nhất**, trước lần chạy SC3 đầu tiên không kèm `--probe`. Cùng commit đó chuyển kết luận
go/no-go của §12a từ "tạm" sang chính thức.

---

## A24. Baseline tầng (a) khi tập nhạy cảm có nhiều hơn một mẫu

**Đề xuất.** Mở rộng `RandomMasking`: chọn vị trí ngẫu nhiên trong **hợp** các Nettree của mọi
mẫu nhạy cảm, dừng khi mọi mẫu đạt ngưỡng. Giữ nguyên bốn tổ hợp HH/HR/RH/RR.

**Căn cứ.** Hiện `RandomMasking` ném lỗi khi |𝒮| > 1, nên ô |𝒮| ∈ {5, 10, 20} của §11.5 không có
đối chứng tầng (a). Cách mở rộng trên là cách đơn giản nhất còn giữ đúng tinh thần "đối chứng
tầm thường".

---

## A25. Đối chiếu SBSH với [R3]

**Đề xuất.** Mở [R3], đối chiếu Thuật toán 2 và 3. Cho tới khi đối chiếu xong, **không đưa số
SBSH vào bài**. Nếu không đối chiếu được thì bỏ SBSH khỏi bảng và khai lý do, đồng thời sửa họ
Holm ở A6 từ 4 xuống 2.

**Căn cứ.** Bản cài hiện tại dựng lại từ mô tả trong bản thiết kế, không từ thuật toán gốc. Ba
chỗ cần kiểm: nhãn xám bao gồm những gì, bán kính tính theo cung hay theo vị trí, và C-PA với
C-SA khác gì Replace thường. In một con số mang tên SBSH mà chưa đối chiếu là gán cho [R3] một
kết quả có thể không phải của họ.

---

## Bảng tóm tắt để chép vào preregistration

| Mã | Tham số | Giá trị đề xuất |
|---|---|---|
| A1 | δ_L (trên Lost tương đối) | 0,02 |
| A2 | đơn vị áp ngưỡng | cấu hình (bộ, σ, khoảng cách) |
| A2 | thống kê gộp | trung vị |
| A2 | ρ_rep nhóm chính / suy biến | ≥ 0,20 / ≤ 0,05 |
| A2 | β có hiệu lực / suy biến | ≥ 0,20 / ≤ 0,05 |
| A4 | biên tương đương Lost / SupDist | ±0,02 / ±0,01 |
| A5 | H3: vượt 1 / gần 1 | r ≥ 1,10 / r ≤ 1,05 |
| A6 | họ Holm | 4 baseline tầng (b) |
| A7 | ghép cặp H2 | chạy tới HF = 0; kiểm định chỉ trên thực thể cùng \|R\| |
| A8 | hiệu bằng 0 / hạng buộc / cỡ hiệu ứng | Pratt / hạng trung bình / rank-biserial |
| A9 | loại trừ | HF>0, OT, OOM loại cặp; >10% thì không kết luận |
| A10 | lần chạy lại phương pháp ngẫu nhiên | 10, hạt giống 2026–2035, lấy trung vị |
| A11 | ℓ_max | 4 |
| A11 | khoảng cách | [0,1], [0,3]; thêm [0,10] cho dna và msnbc |
| A11 | σ dna / msnbc | 2500, 4000 / 1000, 2000, 5000 |
| A12 | ε của B_ε | 0,5, kèm kiểm B_ε không rỗng |
| A13 | phân vị lấy mẫu | p10, p30, p50, p70, p90; 4 tập mỗi phân vị |
| A14 | phạm vi μ(P) | 1 |
| A15 | nhiều mẫu, ngưỡng CSDL | LP làm tròn; GA dự phòng |
| A16 | thực thể nhỏ / mốc ILP | ≤64 ứng viên và ≤12 lần che / exact-minR-minLost |
| A17 | phiên bản mã | HIDENOSP_REVISION từ git, chạy lại số vào bài |
