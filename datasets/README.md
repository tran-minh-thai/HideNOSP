# datasets/

Dữ liệu thô **không** vào git (xem `.gitignore`). Thư mục này giữ nguồn gốc, cách lấy và cách
chuyển đổi, để một người khác dựng lại đúng tệp mà bài đã dùng.

Mọi đặc trưng của bộ dữ liệu (ρ_rep, β, \|R\|\*, kích thước bảng chữ cái, độ dài chuỗi) **đo bằng
SC2**, không chép từ mô tả của người khác (§11.2).

## Bảng bộ dữ liệu (§11.2)

| Tệp | Nguồn | Định dạng | Vai dự kiến (§11.2) | Trạng thái |
|---|---|---|---|---|
| `msnbc.txt` | SPMF `publicdatasets/MSNBC.txt` | `SPMF` | Nhóm chính; [R3] đã dùng bản MSN | đã tải |
| `sign.txt` | SPMF `publicdatasets/SIGN.txt` | `SPMF` | Chờ SC2 quyết | đã tải |
| `fifa.txt` | SPMF `publicdatasets/FIFA.txt` | `SPMF` | Chờ SC2 quyết | đã tải |
| `bms1.txt` | SPMF `publicdatasets/BMS1_spmf.txt` | `SPMF` | Đối chứng suy biến; [R3] đã dùng BMS-WebView-1 | đã tải |
| `kosarak.txt` | SPMF `publicdatasets/kosarak_sequences.txt`, 60 MB | `SPMF` | Mở rộng ở SC6 | chưa tải |
| `dna.txt` | công khai, kiểm lại nguồn trong [R1] | `PLAIN` | Bảng chữ cái nhỏ, lặp dày, dạng dữ liệu của dòng NOSEP | chưa tải |
| `trucks.txt` | Dữ liệu của [R20], quỹ đạo rời rạc hoá lưới 10×10, 273 chuỗi | `PLAIN` | Đặt số cạnh [R20] | chưa tải, chưa tìm được nguồn công khai |

Bốn tệp đã tải lấy bằng `python tools/fetch_datasets.py`, ghi nguồn gốc vào `provenance.tsv`
(tệp này **có** vào git: nó chỉ chứa URL, ngày tải, tổng kiểm SHA-256 và kích thước).

## Kích thước đo được

| Bộ | N | Tổng độ dài | Trung bình | Dài nhất | \|Σ\| |
|---|---|---|---|---|---|
| msnbc | 31.790 | 423.776 | 13,3 | 100 | 17 |
| sign | 730 | 37.958 | 52,0 | 94 | 267 |
| fifa | 20.450 | 741.092 | 36,2 | 100 | 2.990 |
| bms1 | 59.601 | 149.638 | 2,5 | 267 | 497 |

Cả bốn tệp đều là tập mục một phần tử, nên tỉ lệ tuyến tính hoá bằng 0: chúng đúng là chuỗi sự
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
