# datasets/

Dữ liệu thô **không** vào git (xem `.gitignore`). Thư mục này giữ nguồn gốc, cách lấy và cách
chuyển đổi, để một người khác dựng lại đúng tệp mà bài đã dùng.

Mọi đặc trưng của bộ dữ liệu (ρ_rep, β, \|R\|\*, kích thước bảng chữ cái, độ dài chuỗi) **đo bằng
SC2**, không chép từ mô tả của người khác (§11.2).

## Bảng bộ dữ liệu (§11.2)

| Tệp | Nguồn | Vai (đo bằng SC2, xem `../results-invariant/khao_sat_bo_du_lieu.md`) | Trạng thái |
|---|---|---|---|
| `dna_mt745584.txt` | SPMF `MT745584_SPMF.txt` | **Nhóm chính**: bộ gen COVID-19, ρ_rep 0,786; dạng dữ liệu của [R1] | đã tải |
| `msnbc.txt` | SPMF `MSNBC.txt` | **Nhóm chính**: ρ_rep 0,323; [R3] dùng bản MSN | đã tải |
| `leviathan.txt` | SPMF `LEVIATHAN.txt` | Trung gian: ρ_rep 0,154 nhưng β 0,612 | đã tải |
| `bible.txt` | SPMF `BIBLE.txt` | Trung gian: ρ_rep 0,153 nhưng β 0,500 | đã tải |
| `fifa.txt` | SPMF `FIFA.txt` | Đối chứng suy biến: ρ_rep 0,008 | đã tải |
| `sign.txt` | SPMF `SIGN.txt` | Đối chứng suy biến: ρ_rep 0,000 | đã tải |
| `bms1.txt` | SPMF `BMS1_spmf.txt` | Đối chứng suy biến kép: ρ_rep 0,000, β 0,094; [R3] dùng | đã tải |
| `bike.txt` | SPMF `BIKE.txt` | Không dùng: ρ_rep 0,052; bản công khai gần nhất với trucks về dạng, **không phải** cùng bộ | đã tải |
| `kosarak.txt` | SPMF `kosarak_sequences.txt`, 60 MB | Mở rộng ở SC6 | chưa tải |
| `trucks.txt` | Dữ liệu của [R20] và [R3] | Cần cho SC8 | chưa tìm được nguồn công khai |
| chuỗi thời gian rời rạc hoá | kho UCR hoặc tương đương | [R1] dùng loại này | chưa lấy |

Đã loại sau khi xem định dạng: **E-Shop** của SPMF (24.026 chuỗi, dài trung bình 62) là **tập mục
nhiều phần tử**, nằm ngoài mô hình dữ liệu chuỗi sự kiện đơn mục của bài (§8.3).

Bốn tệp đã tải lấy bằng `python tools/fetch_datasets.py`, ghi nguồn gốc vào `provenance.tsv`
(tệp này **có** vào git: nó chỉ chứa URL, ngày tải, tổng kiểm SHA-256 và kích thước).

## Kích thước đo được

| Bộ | N | Tổng độ dài | Trung bình | Dài nhất | \|Σ\| |
|---|---|---|---|---|---|
| dna_mt745584 | 498 | 29.860 | 60,0 | 60 | **4** |
| msnbc | 31.790 | 423.776 | 13,3 | 100 | 17 |
| bike | 21.078 | 153.383 | 7,3 | 53 | 67 |
| sign | 730 | 37.958 | 52,0 | 94 | 267 |
| bms1 | 59.601 | 149.638 | 2,5 | 267 | 497 |
| fifa | 20.450 | 741.092 | 36,2 | 100 | 2.990 |
| leviathan | 5.834 | 197.251 | 33,8 | 100 | 9.025 |
| bible | 36.369 | 787.066 | 21,6 | 100 | 13.905 |

Cả tám tệp đều là tập mục một phần tử, nên tỉ lệ tuyến tính hoá bằng 0: chúng đúng là chuỗi sự
kiện đơn mục, không phải dữ liệu giao dịch bị ép về chuỗi.

`tools/fetch_datasets.py` ghi `provenance.tsv` tự động. Bộ nào tải tay thì thêm một dòng theo
đúng cột của tệp đó: tên, tệp, định dạng, URL, ngày tải, số byte, SHA-256, vai.

## Hai định dạng mà `io.DatasetReader` đọc

**`PLAIN`** — mỗi dòng một chuỗi, các mã mục cách nhau bằng khoảng trắng:

```
1 2 1 1 3 2
4 4 1 2
```

**`SPMF`** — định dạng chuỗi của thư viện SPMF: mục cách nhau bằng khoảng trắng, `-1` đóng một
tập mục, `-2` đóng một chuỗi:

```
1 -1 2 3 -1 1 -1 -2
```

Dòng rỗng và dòng bắt đầu bằng `#` hoặc `@` bị bỏ qua.

## Tập mục nhiều phần tử

Mô hình dữ liệu của bài là chuỗi **sự kiện đơn mục** (§8.3). Một tập mục có nhiều hơn một phần
tử nằm ngoài mô hình đó. `DatasetReader` tuyến tính hoá bằng cách sắp theo mã mục tăng dần —
một quy tắc tất định — và trả về `linearisedShare`, tỉ lệ vị trí phát sinh từ những tập mục như
vậy. Quy tắc 2.2.10 đòi khai tỉ lệ này cạnh bộ dữ liệu; SC2 in nó ra.

Nếu tỉ lệ đó lớn, bộ dữ liệu không thực sự là chuỗi sự kiện đơn mục và phải cân nhắc loại khỏi
nhóm chứng minh đóng góp, chứ không chỉ ghi chú.

## Dữ liệu tổng hợp

`io.SyntheticGenerator` sinh dữ liệu có kiểm soát ρ_rep và β, hạt giống theo từng tệp
(quy tắc 2.6.7, 4.2.4). Chỉ báo cáo ρ_rep và β **đo được** bằng `SyntheticGenerator.measure`,
không báo cáo giá trị mục tiêu như thể đã đạt.

## Bổ sung ngày 25/09/2026 — nhật ký tiến trình và CSDL của HAUP-Hider

Lấy bằng `python tools/fetch_datasets.py` (tệp thô vào `datasets/raw/`, kiểm MD5 do nguồn công bố),
dựng bằng `python tools/build_datasets.py` (quy tắc gán vai ở đầu tệp đó). Mọi số dưới đây **đo**
trên tệp đã dựng; cột phân nhóm là **thăm dò** SC2 trên máy phát triển
(`../results-probe/SC2_20260925-122*`), phân nhóm chính thức chờ SC2 trên máy đo.

**Bảng gán vai của nhật ký tiến trình** (một quyết định thiết kế, phải khai trong bài): một ca
(hồ sơ vay, sự cố, bệnh nhân) là một chuỗi; một sự kiện là một vị trí; mục là hoạt động
(BPI 2012: tên hoạt động của sự kiện COMPLETE; BPI 2013: trạng thái/trạng thái con; Sepsis: tên
hoạt động); thứ tự theo dấu thời gian, sự kiện trùng dấu thời gian giữ thứ tự trong tệp.

| Bộ | Nguồn | N | Sự kiện | \|Σ\| | Trùng dấu thời gian | ρ_rep trung vị (mọi mẫu / chỉ μ=1) | β | Phân nhóm (thăm dò) |
|---|---|---|---|---|---|---|---|---|
| `bpi2013` | 4TU, BPI Challenge 2013 incidents | 7.554 | 65.533 | 13 | 0,0% | [0,1] σ=1000: 0,254 / 0,021; [0,3] σ=500: 0,236 / 0,135; hai cấu hình còn lại 0,13 | 0,11–0,45 | Theo chữ của quy tắc đã chốt: **nhóm chính** ở 2/4 cấu hình. Chỉ tính mẫu không lặp mục: trung gian hoặc suy biến |
| `bpi2012` | 4TU, BPI Challenge 2012 | 13.087 | 164.506 | 23 | 8,5% | 0,000 / 0,000 ở cả 4 cấu hình (mẫu lặp mục: 0,00–0,75; 3/4 cấu hình từ 0,48) | 0,06–0,21 | Suy biến cho phạm vi μ(P) = 1 |
| `sepsis` | 4TU, Sepsis Cases | 1.050 | 15.214 | 16 | **29,2%** | 0,048 / 0,000 và 0,000 / 0,000 (mẫu lặp mục: 0,28–0,51) | 0,20–0,53 | Suy biến; tỉ lệ trùng dấu thời gian lớn, nên xét loại khỏi nhóm chứng minh đóng góp |

**CSDL của HAUP-Hider** (`haup_sdb1` … `haup_sdb14`, kho `wuc567/Pattern-Mining` tại commit
`37c82b7`, kho không khai giấy phép; chỉ tải về để đánh giá, không phân phối lại). Phép đo dấu vết
tập mục bị dàn phẳng (tỉ lệ cặp kề tăng dần và độ dài đoạn tăng):

| Bộ | Cặp kề tăng | Đoạn tăng trung bình / dài nhất | Đọc |
|---|---|---|---|
| `haup_sdb2`, `haup_sdb7`–`haup_sdb14` | 0,90 | **9,00 / 9** | Mọi đoạn tăng dài đúng 9: tập mục 9 phần tử sắp tăng rồi nối lại. Khớp số liệu E-Shop (283 mục; SDB2: 766 chuỗi, 47.322 mục) |
| `haup_sdb3`, `haup_sdb4`, `haup_sdb5` | 0,86–0,91 | 6,2–9,3 / 12–58 | Tập mục dàn phẳng; SDB4, SDB5 khớp OnlineRetail-Best, OnlineRetail-All |
| `haup_sdb1` | 0,63 | 2,52 / 6 | Khớp Babysale (6 mục, 359 chuỗi; trung bình 2,49 mục mỗi tập) |
| `haup_sdb6` | 0,64 | 2,66 / 14 | Là SIGN, bộ đã có (`sign.txt`) |
| đối chiếu: `msnbc`, `bible`, nhật ký | 0,42–0,51 | 1,6–1,9 / 5–24 | Thứ tự thật |

Vai: **13/14 bộ nằm ngoài mô hình dữ liệu của bài** (thứ tự trong tập mục là giả nên ràng buộc
khoảng cách không có nghĩa). Chỉ dùng để so với HAUP-Hider trên chính dữ liệu của họ, khai rõ vai
đó; không dùng làm bằng chứng cho đóng góp.

**Phát hiện phụ về `bms1`:** 100% cặp kề tăng dần — mỗi chuỗi đã bị sắp theo mã mục, thứ tự nhấp
chuột thật đã mất. Bộ này vẫn là đối chứng suy biến; không được dùng nó làm bằng chứng cho bất cứ
điều gì phụ thuộc thứ tự (ràng buộc khoảng cách, SC5).

## Bổ sung ngày 25/09/2026 (2) — protein của dòng khai phá không chồng lấp

Nguồn: sáu tệp `SDB1` … `SDB6` công bố cùng NTP-Miner trong kho `wuc567/Pattern-Mining`, ghim commit
`0b3d4e5`; tải bằng `python tools/fetch_datasets.py`, dựng bằng `python tools/build_datasets.py`
(quy tắc `letters`: một dòng là một protein, một chữ cái axit amin là một mục; ký tự khác chữ cái là
lỗi). Mọi số dưới đây **đo** trên tệp đã dựng; đầu ra nguyên văn của bộ dựng:
`../results-probe/protein_build_log_20260925.txt`.

**Sáu tệp lồng nhau, nên chúng là MỘT nguồn dữ liệu, không phải sáu bộ.** Đo bằng so khớp nguyên
chuỗi: mọi chuỗi của SDB2 nằm trong SDB1, của SDB3 trong SDB2, của SDB5 trong SDB4, của SDB6 trong
SDB5; SDB1 và SDB4 chung 378 chuỗi (75% của SDB1, 64% của SDB4). Chỉ dựng hai tệp lớn nhất; bốn tệp
còn lại giữ nguyên tệp thô vì chúng là bằng chứng của việc lồng nhau.

| Bộ | Tệp thô | N | Sự kiện | \|Σ\| | Dài trung bình / dài nhất | σ (chọn theo quy tắc, trước mọi lượt SC2) | Phân nhóm (thăm dò) |
|---|---|---|---|---|---|---|---|
| `protein_sdb4` | SDB4 | 590 | 109.424 | 20 | 185,5 / 653 | 500, 900 | **Nhóm chính**, ràng buộc khoảng cách có hiệu lực, cả 4 cấu hình |
| `protein_sdb1` | SDB1 | 507 | 91.875 | 20 | 181,2 / 994 | 400, 800 | **Nhóm chính**, ràng buộc khoảng cách có hiệu lực, cả 4 cấu hình |

Trung vị ρ_rep, β và cỡ kho ứng viên của từng cấu hình: bảng 1 của
`../results-probe/protein_groups_and_sigma_rule_probe_20260925.md` (sinh bằng
`tools/data_regime_tables.py`). Phân nhóm chính thức chờ SC2 trên máy đo (`run.txt` mục 5a).

**Vai:** chứng minh đóng góp (nhóm chính). Vì hai tệp chung phần lớn chuỗi, **đếm là một bộ**;
tệp nào vào phép kiểm H2 là quyết định còn mở; khi chốt sẽ ghi thành một mục bổ sung của
`../config/preregistration.md`, trước mọi lượt SC3 trên protein.
