# tools/

Công cụ phụ trợ, không phải một phần của thuật toán.

| Tệp | Việc |
|---|---|
| `run.py` | **Chạy kịch bản có gắn phiên bản mã.** Dùng cái này cho mọi lần chạy lấy số vào bài |
| `fetch_datasets.py` | Tải bộ dữ liệu công khai, tính SHA-256, ghi `datasets/provenance.tsv` |
| `check_results.py` | Kiểm lại luật thư mục kết quả §11.8 trên tệp có thật trên đĩa |
| `merge_decisions.py` | Gộp kết quả rà soát thành `config/quyet_dinh_can_chot.md` (trước 26/09/2026: `gop_quyet_dinh.py`) |
| `preregistered_analysis.py` | **Phân tích H2 và H3** đúng theo `config/preregistration.md` (trước 26/09/2026: `thong_ke.py`) |
| `preregistered_analysis_test.py` | Ghim phần thống kê vào giá trị tính tay. Chạy trước khi tin vào bảng nào |
| `check_english.py` | Kiểm mọi tệp mã chỉ viết tiếng Anh; mã thoát 1 khi có vi phạm |
| `check_english_test.py` | Tiêm từng loại vi phạm vào một kho tạm và đòi `check_english.py` bắt đúng |
| `sc3_commands.py` | **Sinh lệnh SC3 còn thiếu** từ hai bảng nhóm đã chốt (nhóm chính), bỏ ô đã có tệp kết quả; `--self-test` tiêm lỗi vào quy tắc khớp ô |

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

## `preregistered_analysis.py`

```bash
python tools/preregistered_analysis.py --sc3 results-invariant --sc2 results-invariant --out results-invariant/h2_h3_analysis.txt
```

Script **không** chọn phép kiểm, ngưỡng hay cách ghép cặp. Mọi thứ đó chốt ở
`config/preregistration.md` trước khi chạy SC3; ở đây chỉ đọc lại và thi hành. Hằng số nằm gọn
trong lớp `LOCKED`, mỗi hằng ghi rõ nó đến từ mục nào của bản chốt, để đối chiếu bằng mắt được.

Nó tự kiểm ba thứ và **dừng với mã thoát 1** khi không ổn: tệp SC3 thiếu cột (do bản mã cũ sinh
ra), xếp nhóm cấu hình tính lại từ SC2 lệch so với bảng đã chốt ở mục 3, và tỉ lệ thực thể bị
loại vượt 10%.

Mã thoát 0 **không** có nghĩa là H2 được ủng hộ. Nó chỉ có nghĩa là phân tích chạy được.

Chạy `python tools/preregistered_analysis_test.py` trước. Các phép thử ở đó ghim Pratt, rank-biserial,
Holm và Hodges–Lehmann vào giá trị tính tay — trong đó có một phép liệt kê toàn bộ 2⁸ khả năng
để bắt lỗi `method="exact"` của scipy dưới Pratt (xem mục 7 của `../RUNBOOK.md`).

## `check_english.py`

```bash
python tools/check_english.py
python tools/check_english_test.py
```

Mọi tệp mã (chú thích, chuỗi in ra, tên, tên tệp) phải viết tiếng Anh. Công cụ đọc danh sách tệp
của git, xếp mỗi tệp vào đúng một loại (mã, tài liệu `*.md`, đầu ra các lượt chạy); tệp không
thuộc loại nào là lỗi. Ba luật: chữ tiếng Việt có dấu, từ tiếng Việt không dấu (danh sách đo trên
toàn kho, `--measure` đếm lại), số hiệu mục hay định lý của bài. Giới hạn đã biết ghi ở đầu tệp.

## Còn thiếu

- Script sinh bảng và hình cho bản thảo từ tệp CSV. Bảng nhãn thuộc phía bản thảo: `labels.tsv` đã
  rời kho mã ngày 26/09/2026 (bản cuối trong lịch sử git, commit `5e5a5de`).
- Script bọc ngoài ghi bộ nhớ thường trú của tiến trình cho SC6 (§11.7 dòng 6);
  trên Windows lấy từ `Get-Process`, trên Linux từ `/proc/self/status`.
