# tools/

Công cụ phụ trợ, không phải một phần của thuật toán.

| Tệp | Việc |
|---|---|
| `labels.tsv` | Bảng nhãn tập trung (quy tắc 3.3.12). Mọi nhãn trong bảng và hình sinh từ đây |
| `check_results.py` | Kiểm lại luật thư mục kết quả §11.8 trên tệp có thật trên đĩa |

`check_results.py` kiểm cùng luật mà `hidenosp.io.ResultSink` cưỡng chế lúc mở tệp, nhưng kiểm
trên đĩa: một tệp bị chuyển tay, sửa tay, hay sinh bởi bản mã cũ vẫn bị bắt.

```bash
python tools/check_results.py
```

Trả về mã thoát 1 khi có vấn đề, nên gắn được vào hook trước khi commit.

## Còn thiếu

- Script sinh bảng và hình cho bản thảo từ tệp CSV, đọc nhãn từ `labels.tsv`.
- Script bọc ngoài ghi bộ nhớ thường trú của tiến trình cho SC6 (§11.7 dòng 6);
  trên Windows lấy từ `Get-Process`, trên Linux từ `/proc/self/status`.
