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
