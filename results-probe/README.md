# results-probe/

Số đo **dò tìm** trên máy phát triển, và mọi lần chạy trên dữ liệu tổng hợp thay thế khi chưa
có bộ dữ liệu thật.

Những con số này **không vào bài như kết quả** (§8.2). Chúng dùng để: dò phản ví dụ, xem một
cấu hình có đáng chạy đầy đủ không, và kiểm tra đường ống trước khi chạy thật.

Chạy bằng cờ `--probe`, khi đó tệp bất biến được chuyển hướng về đây thay vì
`results-invariant/`. Cấm cột thời gian và cột bộ nhớ, giống `results-invariant/`.

Các số dò tìm bằng Python trong `../kiemtra_v2.py` và `../probe/` cũng thuộc loại này. Các
script đó in ra màn hình, nên phải hứng đầu ra vào thư mục này; mục 2 của `../RUNBOOK.md` có
lệnh sẵn.

`probe/timing.py` in ra **giây**, tức số phụ thuộc máy. Nó vẫn nằm ở đây chứ không ở `results/`,
vì đó là một phép dò trên máy phát triển: §8.2 của bản thiết kế xếp nó vào nhóm dò tìm. Số của
nó **không bao giờ** được chuyển sang `results/` hay in vào bài; muốn có số thời gian cho bài
thì chạy SC6 trên máy đo đã khai.

Không dùng `>>` để ghi thêm vào tệp nằm trên ổ Google Drive: ổ ảo này trả kích thước tệp cũ,
nên lần ghi thêm đè lên giữa tệp. Hãy ghi ra đĩa cục bộ rồi chép sang.
