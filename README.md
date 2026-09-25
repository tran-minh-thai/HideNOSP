# HideNOSP

Ẩn mẫu tuần tự nhạy cảm theo **độ hỗ trợ không chồng lấp** (`sup_NO`) có **ràng buộc khoảng cách**.

Kho này cài đặt **chỉ mục cắt Nettree** và thuật toán **CUT-HIDE**, cùng các baseline và các
kịch bản thực nghiệm SC1–SC8 của bản thiết kế nghiên cứu.

- Vận hành chi tiết: [`RUNBOOK.md`](RUNBOOK.md)
- Bảng giao thức 9 dòng: [`config/protocol.yaml`](config/protocol.yaml)
- Ngưỡng chốt trước khi chạy so sánh chính: [`config/preregistration.md`](config/preregistration.md)

Bản thiết kế nghiên cứu (`doc/`), bản thảo (`paper/`) và quy trình chạy trên máy đo (`run.txt`)
nằm **ngoài** kho này, ở cùng thư mục dự án một cấp trên.

## Chạy thử

```bash
mvn -o test
```

```bash
java -cp target/classes hidenosp.exp.Runner SC1 --trials=1200
```

SC1 là phép kiểm cài đặt: trên mỗi thực thể ngẫu nhiên nó đối chiếu luồng cực đại với vét cạn,
kiểm số lần che tối thiểu, kiểm đặc trưng nút thiết yếu, và đối chiếu cập nhật gia tăng với
dựng lại. Kết quả chấp nhận được duy nhất là 0 ca lệch.

```bash
java -cp target/classes hidenosp.exp.Runner
```

in danh sách kịch bản cùng tuỳ chọn của từng cái.

## Yêu cầu

JDK 25 (bản LTS, Eclipse Temurin), Maven 3.9 trở lên. Không phụ thuộc thư viện ngoài ngoài JUnit 5 (chỉ cho test).

## Bố cục

```
pom.xml                 Maven, Java 25
src/main/java/hidenosp/
  core/                 chuỗi sự kiện, mẫu, ràng buộc khoảng cách, Nettree, sup_NO
  core/flow/            Dinic sức chứa nút, lát cắt nhỏ nhất, nút thiết yếu, che gia tăng
  index/                chỉ mục cắt Nettree, đếm tiến và lùi
  hide/                 CUT-HIDE, đường cong chi phí, quét lambda, phân bổ giữa các chuỗi
  baseline/             đối chứng ngẫu nhiên HH/HR/RH/RR, heuristic đếm lần xuất hiện, HHA, DBSH, SBSH, ILP, GA
  metrics/              HF, Lost, Ghost, DistR, SupDist, khai phá F_sigma, chế độ dữ liệu
  io/                   đọc bộ dữ liệu, sinh dữ liệu tổng hợp, ghi kết quả, dấu định danh lần chạy
  exp/                  SC1–SC8 và bộ chạy
src/test/java/          các ví dụ chạy tay của bản thiết kế, đóng thành test
config/                 bảng giao thức, tệp chốt trước
datasets/               nguồn gốc bộ dữ liệu; dữ liệu thô không vào git
tools/                  bảng nhãn tập trung, script kiểm luật thư mục kết quả
kiemtra_v2.py, probe/   các script dò tìm bằng Python
results-invariant/      số đo không phụ thuộc máy
results/                số đo thời gian và bộ nhớ, chỉ từ máy đo đã khai
results-probe/          số đo dò tìm, không đưa vào bài
```

Ba thư mục kết quả có luật riêng và `hidenosp.io.ResultSink` cưỡng chế ngay lúc mở tệp: tệp bất
biến không được mang cột thời gian hay bộ nhớ, và chỉ máy đo đã khai mới được ghi số thời gian.
`tools/check_results.py` kiểm lại các luật đó trên tệp có thật trên đĩa.

## Trạng thái

Nguồn đúng duy nhất cho trạng thái cài đặt là mục 5 của [`RUNBOOK.md`](RUNBOOK.md); tệp này không
chép lại để hai nơi không lệch nhau. Tìm nhanh phần chưa cài: `grep -rn "TODO(code)" src/`.
