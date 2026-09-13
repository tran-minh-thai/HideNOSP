# tools/

Công cụ phụ trợ, không phải một phần của thuật toán.

| Tệp | Việc |
|---|---|
| `run.py` | **Chạy kịch bản có gắn phiên bản mã.** Dùng cái này cho mọi lần chạy lấy số vào bài |
| `fetch_datasets.py` | Tải bộ dữ liệu công khai, tính SHA-256, ghi `datasets/provenance.tsv` |
| `labels.tsv` | Bảng nhãn tập trung (quy tắc 3.3.12). Mọi nhãn trong bảng và hình sinh từ đây |
| `check_results.py` | Kiểm lại luật thư mục kết quả §11.8 trên tệp có thật trên đĩa |
| `gop_quyet_dinh.py` | Gộp kết quả rà soát thành `config/quyet_dinh_can_chot.md` |
| `thong_ke.py` | **Phân tích H2 và H3** đúng theo `config/preregistration.md` |
| `thong_ke_test.py` | Ghim phần thống kê vào giá trị tính tay. Chạy trước khi tin vào bảng nào |

`check_results.py` kiểm cùng luật mà `hidenosp.io.ResultSink` cưỡng chế lúc mở tệp, nhưng kiểm
trên đĩa: một tệp bị chuyển tay, sửa tay, hay sinh bởi bản mã cũ vẫn bị bắt.

```bash
python tools/check_results.py
```

Trả về mã thoát 1 khi có vấn đề, nên gắn được vào hook trước khi commit.

## Vì sao phải qua `run.py`

`RunId` đọc biến môi trường `HIDENOSP_REVISION` và đóng dấu nó vào tên tệp kết quả. Không đặt thì
mọi tệp mang dấu `unversioned`, không truy ngược được về commit nào sinh ra nó — quy tắc 2.6.8 đòi
dấu định danh lần chạy, và `config/protocol.yaml` chốt biến này là **bắt buộc** cho số vào bài.

Script làm ba việc theo thứ tự: dịch lại (để lớp `.class` chắc chắn khớp mã nguồn mà sha đang đóng
dấu), kiểm cây làm việc sạch (một sha chỉ định danh đúng mã đã chạy khi không còn sửa đổi treo),
rồi mới đặt biến và gọi `Runner`.

## `thong_ke.py`

```bash
python tools/thong_ke.py --sc3 results-invariant --sc2 results-invariant --ra results-invariant/phan_tich_h2_h3.txt
```

Script **không** chọn phép kiểm, ngưỡng hay cách ghép cặp. Mọi thứ đó chốt ở
`config/preregistration.md` trước khi chạy SC3; ở đây chỉ đọc lại và thi hành. Hằng số nằm gọn
trong lớp `CHOT`, mỗi hằng ghi rõ nó đến từ mục nào của bản chốt, để đối chiếu bằng mắt được.

Nó tự kiểm ba thứ và **dừng với mã thoát 1** khi không ổn: tệp SC3 thiếu cột (do bản mã cũ sinh
ra), xếp nhóm cấu hình tính lại từ SC2 lệch so với bảng đã chốt ở mục 3, và tỉ lệ thực thể bị
loại vượt 10%.

Mã thoát 0 **không** có nghĩa là H2 được ủng hộ. Nó chỉ có nghĩa là phân tích chạy được.

Chạy `python tools/thong_ke_test.py` trước. Mười hai phép thử ở đó ghim Pratt, rank-biserial,
Holm và Hodges–Lehmann vào giá trị tính tay — trong đó có một phép liệt kê toàn bộ 2⁸ khả năng
để bắt lỗi `method="exact"` của scipy dưới Pratt (xem mục 7 của `../RUNBOOK.md`).

## Còn thiếu

- Script sinh bảng và hình cho bản thảo từ tệp CSV, đọc nhãn từ `labels.tsv`.
- Script bọc ngoài ghi bộ nhớ thường trú của tiến trình cho SC6 (§11.7 dòng 6);
  trên Windows lấy từ `Get-Process`, trên Linux từ `/proc/self/status`.
