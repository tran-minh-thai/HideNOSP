# results/

Số đo **phụ thuộc máy**: thời gian treo tường, thời gian CPU, bộ nhớ đỉnh.

Chỉ máy đo đã khai trong `config/protocol.yaml` (dòng 5 đến 7) được ghi vào đây. Chạy bằng
`--machine=<tên máy>`; lần chạy gắn nhãn mặc định `dev` bị `hidenosp.io.ResultSink` từ chối.

Một tệp ở đây phải có ít nhất một cột thời gian hoặc bộ nhớ. Nếu không có thì nó thuộc về
`results-invariant/`.

Số lần lặp theo bảng §11.6 (dưới 1 giây: ≥ 15 lần; 1–10 giây: ≥ 10; 10–120 giây: 5; trên 120
giây: 3 kèm lý do). `hidenosp.util.Timing` cài đúng bảng này và ghi cả số lần làm nóng đã bỏ.
