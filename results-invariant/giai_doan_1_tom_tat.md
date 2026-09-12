# Giai đoạn 1: SC1 và SC2 trên dữ liệu thật

> **Đã chạy lại ngày 12/09/2026 ở ℓ_max = 4**, sau khi `protocol.yaml` chốt giá trị đó, và toàn bộ
> qua `tools/run.py` nên mỗi tệp mang dấu phiên bản `2f63b567ccf1`. Bảng ρ_rep và β thay thế nằm ở
> `khao_sat_bo_du_lieu.md`. Bộ số ℓ_max = 3 dưới đây giữ lại để đối chiếu; nó **không còn khớp
> giao thức** vì §11.4 dòng 5 đòi mọi phương pháp dùng cùng ℓ_max.

Ngày chạy: 2026-09-12. Máy: máy phát triển (mọi số ở đây bất biến theo máy, nên điều đó không
ảnh hưởng). Mã: xem dấu định danh ở đầu từng tệp CSV trong thư mục này.

Giai đoạn 1 theo §12b là **cài đặt, SC1–SC2, chốt `config/preregistration.md`**. Hai bước đầu đã
xong; bước ba còn chờ quyết định của người viết, và §4 dưới đây nêu rõ quyết định nào.

---

## 1. SC1: kiểm cài đặt

`Runner SC1 --trials=1500 --seed=2026`, tệp `SC1_20260912-*.csv`.

| Phép kiểm | Số thực thể | Ca lệch |
|---|---|---|
| L0: luồng cực đại = sup_NO vét cạn | 1.500 | 0 |
| L0: cỡ lát cắt nhỏ nhất = sup_NO | 1.500 | 0 |
| P1: số lần che tối thiểu = F − t | 1.069 | 0 |
| L5: nút thiết yếu ⇔ che làm giảm sup_NO | 4.079 | 0 |
| Cập nhật gia tăng = dựng lại | 1.391 | 0 |
| P5(a): che không bao giờ làm tăng sup_NO | 7.394 | 0 |
| P5(b): xoá đơn điệu khi g⁺ = ∞ | 1.719 | 0 |
| Khai phá `F_σ` có tỉa = vét cạn | 40 | 0 |

Quan sát kèm theo (không phải phép kiểm): xoá với g⁺ hữu hạn làm **tăng** sup_NO ở 54 trên 5.675
lần, đúng như P5(b) dự đoán. Đây là căn cứ thực nghiệm cho việc chọn phép che làm phép sửa chính.

**Kết luận: SC1 xanh.** Mọi kịch bản sau được phép chạy.

---

## 2. SC2: chế độ dữ liệu

Bốn bộ công khai, tải bằng `tools/fetch_datasets.py`, tổng kiểm ghi ở `datasets/provenance.tsv`.
Mười cấu hình (bộ, σ, khoảng cách), mỗi cấu hình lấy mẫu mẫu nhạy cảm theo phân vị của sup_NO.

| Bộ | σ | Khoảng cách | ρ_rep nhỏ/tb/lớn | β nhỏ/tb/lớn | \|R\|\*/Σ\|S\| lớn nhất |
|---|---|---|---|---|---|
| bms1 | 100 | [0,3] | 0,000 / 0,000 / 0,000 | 0,000 / 0,117 / 0,522 | 0,0024 |
| bms1 | 300 | [0,3] | 0,000 / 0,000 / 0,000 | 0,000 / 0,071 / 0,366 | 0,0032 |
| fifa | 3000 | [0,3] | 0,005 / 0,008 / 0,013 | 0,185 / 0,289 / 0,467 | 0,0025 |
| sign | 100 | [0,3] | 0,000 / 0,000 / 0,000 | 0,010 / 0,256 / 0,534 | 0,0080 |
| sign | 200 | [0,3] | 0,000 / 0,000 / 0,000 | 0,000 / 0,163 / 0,339 | 0,0054 |
| msnbc | 1000 | [0,3] | 0,064 / 0,190 / 0,470 | 0,111 / 0,311 / 0,445 | 0,0231 |
| msnbc | 2000 | [0,1] | 0,085 / 0,321 / 0,524 | 0,146 / 0,432 / 0,618 | 0,0536 |
| msnbc | 2000 | [0,3] | 0,106 / 0,293 / 0,575 | 0,047 / 0,232 / 0,319 | 0,0374 |
| msnbc | 2000 | [0,10] | 0,158 / 0,415 / 0,750 | 0,010 / 0,043 / 0,077 | 0,2056 |
| msnbc | 5000 | [0,3] | 0,195 / 0,395 / 0,750 | 0,080 / 0,173 / 0,271 | 0,1836 |

### Đọc bảng

**Chỉ MSNBC đủ tư cách vào nhóm chính.** SIGN và BMS1 có ρ_rep đúng bằng 0 ở mọi mẫu đã lấy:
trên hai bộ đó sup_NO suy biến hoàn toàn thành độ hỗ trợ theo chuỗi, nên chúng không nói được gì
về đóng góp của bài. FIFA có ρ_rep khoảng 0,008, tức cũng suy biến trên các mẫu không lặp mục.
BMS1 còn suy biến cả ở β (trung bình 0,117, nhiều mẫu bằng 0), đúng vai đối chứng kép mà §11.2
dự kiến cho nó.

Đây chính là rủi ro §12 đã lường: "Dữ liệu thật có ρ_rep thấp, làm sup_NO suy biến". Nó đã xảy ra,
và xảy ra với ba trên bốn bộ.

**β và ρ_rep kéo ngược nhau theo độ rộng khoảng cách.** Trên MSNBC ở σ = 2000: nới [0,1] lên
[0,10] làm ρ_rep trung bình tăng từ 0,321 lên 0,415 nhưng β tụt từ 0,432 xuống 0,043. Khoảng cách
càng rộng thì mẫu càng lặp được nhiều, nhưng ràng buộc càng mất hiệu lực. Cấu hình cho cả hai đại
lượng ở mức dùng được là **[0,3]**, và [0,1] nếu muốn β cao.

**σ càng cao thì ρ_rep càng cao.** MSNBC [0,3]: σ = 1000 cho ρ_rep trung bình 0,190; σ = 5000 cho
0,395. Mẫu càng phổ biến thì càng lặp trong từng chuỗi.

---

## 3. Một căng thẳng phải xử lý trước khi viết bài

Tách các mẫu đã lấy ở MSNBC theo μ(P):

| Bộ | Mẫu không lặp mục (μ = 1) | | Mẫu lặp mục (μ ≥ 2) | |
|---|---|---|---|---|
| | n | ρ_rep trung bình | n | ρ_rep trung bình |
| msnbc | 31 | 0,218 | 29 | **0,434** |
| fifa | 10 | 0,008 | 0 | — |
| sign | 20 | 0,000 | 0 | — |
| bms1 | 20 | 0,000 | 0 | — |

Trên MSNBC, mẫu lặp mục có tỉ lệ lặp **gấp đôi** mẫu không lặp mục (0,434 so với 0,218; lớn nhất
0,750 so với 0,555). Ngoài ra, mẫu duy nhất có ρ_rep rất cao gặp ở FIFA là `[17, 17, 17]`, với
sup_NO 4.999 so với sup_seq 256, tức ρ_rep = 0,949 — cũng là mẫu lặp mục.

Nói cách khác: **chế độ dữ liệu mà sup_NO thực sự khác sup_seq lại tập trung ở đúng lớp mẫu mà
lý thuyết của bài yếu nhất.** P1 và P2 chỉ đúng khi μ(P) = 1; với μ(P) ≥ 2 chỉ có hai cận của P3,
và §8.1 ghi độ khó của trường hợp đó là câu hỏi mở. Bài không được lảng tránh chỗ này. Ba cách
xử lý, chọn trước khi chạy SC3:

1. Giữ phạm vi ở μ(P) = 1, khai rõ trong phần hạn chế rằng đó là lớp mẫu có tỉ lệ lặp thấp hơn,
   kèm đúng con số 0,218 so với 0,434 ở trên.
2. Mở phạm vi sang μ(P) ≥ 2, dùng cận P3 và bộ giải chính xác trên thực thể nhỏ để đo khoảng cách
   tới tối ưu, chấp nhận không có kết quả chính xác cho lớp đó.
3. Bổ sung bộ dữ liệu có tỉ lệ lặp cao **cho cả mẫu không lặp mục**, tức chuỗi DNA mà §11.2 đã dự
   kiến và dòng NOSEP [R1] dùng. Đây là cách duy nhất làm nhóm chính rộng hơn một bộ.

> **Cập nhật cùng ngày: phương án 3 đã làm, và nó gỡ luôn căng thẳng này.** Bộ gen MT745584
> (498 chuỗi × 60, |Σ| = 4) cho ρ_rep trung bình 0,786, và quan trọng hơn, ρ_rep của mẫu **không
> lặp mục** ở đó là 0,806 — **cao hơn** mẫu lặp mục (0,773), tức ngược hẳn MSNBC. Giữ phạm vi ở
> μ(P) = 1 nay không còn phải trả giá bằng việc bỏ chế độ dữ liệu lặp cao, nên phương án 1 trở
> thành lựa chọn trội. Số liệu và vai đề nghị cho từng bộ: `khao_sat_bo_du_lieu.md`.

---

## 4. Việc còn lại của giai đoạn 1

| Việc | Trạng thái |
|---|---|
| SC1 xanh | xong |
| SC2 trên bốn bộ công khai | xong |
| Lấy bộ DNA và chạy SC2 trên đó | **xong**, xem `khao_sat_bo_du_lieu.md`; kết quả đảo ngược kết luận ở mục 3 |
| Lấy bộ trucks của [R20] | chưa tìm được nguồn công khai |
| Chốt ngưỡng phân nhóm ρ_rep và β trong `config/preregistration.md` | **xong** 12/09/2026: ≥0,20 nhóm chính, ≤0,05 suy biến, áp trên cấu hình |
| Chốt δ_L và biên tương đương | **xong** 12/09/2026: δ_L = 0,02 trên Lost tương đối; biên ±0,02 và ±0,01 |
| Quyết phạm vi μ(P) theo mục 3 | **xong** 12/09/2026: giữ μ(P) = 1 làm phạm vi chính |

`config/preregistration.md` đã chốt và đã commit ngày 12/09/2026, nên SC3 được phép chạy lấy số
vào bài — sau khi xong ba việc kỹ thuật ghi ở §7 của tệp đó.

---

## 5. Tệp gốc

Trong thư mục này: `SC1_20260912-*.csv` và các tệp `SC2_20260912-*.csv` (mười cấu hình của đợt đầu, cộng sáu cấu hình của đợt bổ sung bộ dữ liệu). Mỗi tệp mang dấu định
danh lần chạy ở tên và ở phần chú thích đầu tệp. Không tệp nào có cột thời gian hay bộ nhớ, đúng
luật của `results-invariant/`.
