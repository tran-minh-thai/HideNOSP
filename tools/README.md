# tools/

Công cụ phụ trợ, không phải một phần của thuật toán.

| Tệp | Việc |
|---|---|
| `run.py` | **Chạy kịch bản có gắn phiên bản mã.** Dùng cái này cho mọi lần chạy lấy số vào bài |
| `fetch_datasets.py` | Tải bộ dữ liệu công khai, tính SHA-256, ghi `datasets/provenance.tsv` |
| `labels.tsv` | Bảng nhãn tập trung (quy tắc 3.3.12). Mọi nhãn trong bảng và hình sinh từ đây |
| `check_results.py` | Kiểm lại luật thư mục kết quả §11.8 trên tệp có thật trên đĩa |
| `gop_quyet_dinh.py` | Gộp kết quả rà soát thành `config/quyet_dinh_can_chot.md` |

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

## Còn thiếu

- Script sinh bảng và hình cho bản thảo từ tệp CSV, đọc nhãn từ `labels.tsv`.
- Script bọc ngoài ghi bộ nhớ thường trú của tiến trình cho SC6 (§11.7 dòng 6);
  trên Windows lấy từ `Get-Process`, trên Linux từ `/proc/self/status`.
