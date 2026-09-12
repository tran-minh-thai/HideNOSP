# results-probe/

Số đo **dò tìm** trên máy phát triển, và mọi lần chạy trên dữ liệu tổng hợp thay thế khi chưa
có bộ dữ liệu thật.

Những con số này **không vào bài như kết quả** (§8.2). Chúng dùng để: dò phản ví dụ, xem một
cấu hình có đáng chạy đầy đủ không, và kiểm tra đường ống trước khi chạy thật.

Chạy bằng cờ `--probe`, khi đó tệp bất biến được chuyển hướng về đây thay vì
`results-invariant/`. Cấm cột thời gian và cột bộ nhớ, giống `results-invariant/`.

Các số dò tìm bằng Python trong `../kiemtra_v2.py` và `../probe/` cũng thuộc loại này. Hai
script đó in ra màn hình, nên phải hứng đầu ra vào thư mục này; mục 2 của `../RUNBOOK.md` có
lệnh sẵn.
