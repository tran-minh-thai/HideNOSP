# datasets/

Dữ liệu thô **không** vào git (xem `.gitignore`). Thư mục này giữ nguồn gốc, cách lấy và cách
chuyển đổi, để một người khác dựng lại đúng tệp mà bài đã dùng.

Mọi đặc trưng của bộ dữ liệu (ρ_rep, β, \|R\|\*, kích thước bảng chữ cái, độ dài chuỗi) **đo bằng
SC2**, không chép từ mô tả của người khác (§11.2).

## Bảng bộ dữ liệu (§11.2)

| Tên tệp dự kiến | Nguồn | Định dạng | Vai dự kiến | Trạng thái |
|---|---|---|---|---|
| `msnbc.txt` | UCI, msnbc990928 | `PLAIN` | Chứng minh đóng góp; đặt số cạnh [R3] (bản MSN) | chưa tải |
| `fifa.txt` | SPMF | `SPMF` | Chứng minh đóng góp nếu ρ_rep đủ cao | chưa tải |
| `sign.txt` | SPMF | `SPMF` | Chứng minh đóng góp nếu ρ_rep đủ cao | chưa tải |
| `dna.txt` | công khai, kiểm lại nguồn trong [R1] | `PLAIN` | Bảng chữ cái nhỏ, lặp dày, dạng dữ liệu của dòng NOSEP | chưa tải |
| `bms1.txt` | SPMF, BMS-WebView-1 | `SPMF` | Đối chứng suy biến (ρ_rep thấp); [R3] đã dùng | chưa tải |
| `kosarak.txt` | SPMF | `SPMF` | Mở rộng ở SC6 | chưa tải |
| `trucks.txt` | Dữ liệu của [R20], quỹ đạo rời rạc hoá lưới 10×10, 273 chuỗi | `PLAIN` | Đặt số cạnh [R20]; vai chốt sau khi đo ρ_rep ở SC2 | chưa tải |

Với mỗi tệp tải về, ghi thêm một dòng vào `provenance.tsv` (tự tạo, không theo dõi bởi git nếu
chứa đường dẫn máy cá nhân): tên tệp, URL, ngày tải, tổng kiểm SHA-256, số chuỗi, tổng độ dài.

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
