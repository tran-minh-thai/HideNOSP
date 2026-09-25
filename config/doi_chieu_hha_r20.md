# Đối chiếu HHA với [R20] và [R2] — 25/09/2026

**Nguồn đã đọc toàn văn phần thuật toán.**

- [R20] O. Abul, M. Atzori, F. Bonchi, F. Giannotti, "Hiding sequences", ICDE Workshops 2007,
  mục 4 và Thuật toán 1. Bản trong thư viện: `ThuVienBaiBao/14_PPDM_AnGiauLuatNhayCam/[2007] Hiding
  Sequences.pdf`, 10 trang.
- [R2] O. Abul, F. Bonchi, F. Giannotti, "Hiding sequential and spatiotemporal patterns", IEEE TKDE
  2010, mục 2.2 (Thuật toán 1, giữ nguyên của [R20]), mục 2.3 (ràng buộc, đếm số lần xuất hiện,
  nhiều ngưỡng — Thuật toán 2). Bản trong thư viện: `[2010] Hiding Sequential and Spatiotemporal
  Patterns.pdf`, 15 trang.
- [R3] Gkoulalas-Divanis và Loukides, KDD 2011: gọi thuật toán này là HHA, in đầu ra của nó ở
  Hình 1(d) và khoảng cách, tác dụng phụ ở Hình 4(b).

## 1. Thuật toán, theo nguyên văn

- Bài toán: "∀S_i ∈ S_h, sup_D′(S_i) ≤ ψ" — độ hỗ trợ là số chuỗi, ψ là ngưỡng tiết lộ.
- Heuristic cục bộ: "choose the marking position that is involved in most matches. This
  operation is iterated until M^T_Sh = ∅." δ(T[i]) là số lần khớp đi qua vị trí i, trên tập khớp
  của **mọi** mẫu nhạy cảm (mọi lần nhúng, kể cả chồng lấp).
- Heuristic toàn cục: "sort the sequences in D in ascending order of matching set size, and remove
  all matchings in top |D| − ψ input sequences" — tức giữ nguyên ψ chuỗi có tập khớp **lớn nhất**,
  làm sạch **hết** mọi chuỗi còn lại. Lý do tác giả nêu: "if the matching set is small, then we can
  sanitize the respective sequence with less distortion."
- [R2] giữ nguyên Thuật toán 1. Thuật toán 2 của [R2] là cho **nhiều ngưỡng tiết lộ khác nhau**
  (chọn chuỗi có nhiều lần khớp nhất, xen kẽ với làm sạch) — không phải trường hợp một ngưỡng của
  bài này.

## 2. Bản cài trước ngày này lệch ở đâu

| Điểm | Bản trước | Bài gốc |
|---|---|---|
| Thứ tự toàn cục | Tập khớp **giảm dần**, làm sạch chuỗi lớn trước | **Tăng dần**, giữ ψ chuỗi lớn nhất |
| Làm sạch một chuỗi | Dừng khi đủ mức giảm sup_NO của cả CSDL | Làm tới khi chuỗi **hết mọi lần khớp** |
| δ | Chỉ tính các mẫu còn cần giảm | Mọi mẫu nhạy cảm |

Thứ tự giảm dần có lẽ lấy từ Thuật toán 2 của [R2] (nhiều ngưỡng), không phải thuật toán HHA.

## 3. Chỗ bài để ngỏ và cách đã chọn

- Hoà δ: vị trí **trái nhất**. Hoà cỡ tập khớp: chỉ số chuỗi nhỏ đứng trước, nên chỉ số lớn được
  giữ. Với hai quy tắc này, bản cài tái tạo đúng Hình 1(d) của [R3].
- δ tính bằng che thay cho xoá (xem G2 của bản thiết kế: phép xoá của Định lý 2 sai khi có khoảng
  cách tối đa); `Hha.Delta.ORIGINAL_DP` giữ cách đếm lại của Định lý 2 để SC6 so thời gian.

## 4. Phép chuyển sang sup_NO

ψ của bài = σ − 1. `SupportCount.SEQUENCE_COUNT` giữ đúng ψ chuỗi lớn nhất. Với
`SupportCount.NON_OVERLAPPING`, một chuỗi được giữ tốn sup_NO lớn nhất của nó trên các mẫu nhạy
cảm (thay cho 1), và phần giữ là dãy dài nhất các chuỗi lớn nhất có tổng chi phí ≤ σ − 1. Với một
mẫu nhạy cảm đó là đúng ngân sách của bài tính theo đơn vị sup_NO; với độ hỗ trợ theo số chuỗi nó
trùng hẳn quy tắc của bài. Chuỗi được chọn vẫn bị làm sạch **hết**, như bài, nên HHA có thể dùng
nhiều lần che hơn mức cần để tụt dưới σ — đó là hành vi của HHA.

HIDE-SEQ: mọi chuỗi có sup_NO ≥ r bị làm sạch tới khi mọi mẫu < r. Đây là mở rộng "occurrence
counts" của [R2] mục 2.3 với sup_NO thay cho cỡ tập khớp (xem mục 6).

## 5. Bằng chứng

`src/test/java/hidenosp/baseline/HhaPaperExampleTest.java`:

- Ví dụ 2 của [R20]/[R2]: S = abc, T = aabccbae cho δ(T[1]) = 2, δ(T[2]) = 2, δ(T[3]) = 4, và một
  lần đánh dấu ở T[3] làm sạch chuỗi — đúng cả hai cách tính δ.
- Hình 1(d) của [R3] (ψ = 3 theo quy ước "< ψ" của [R3], tức ngưỡng tiết lộ 2 của [R20]): đúng các
  vị trí bị che T1: 0; T2: 0; T3: 0; T6: 0; T8: 1; T9: 0, 1; khoảng cách 7; đúng sáu tác dụng phụ
  ⟨a⟩, ⟨a,b⟩, ⟨a,c⟩, ⟨a,e⟩, ⟨d,f⟩, ⟨d,g⟩ của Hình 4(b).
- Một ca tự dựng cho phép chuyển sang sup_NO (giữ chuỗi lớn nhất trong ngân sách σ − 1).

Tiêm 6 lỗi (thứ tự giảm dần như bản trước, hoà δ lệch phải, chi phí giữ cộng qua các mẫu, sắp theo
sup_NO thay cho cỡ tập khớp, δ chỉ từ một mẫu, cách đếm lại của Định lý 2 không che vị trí): bắt
được 6/6.

## 6. Phát hiện ngoài phạm vi đối chiếu — đụng tới tính mới của HIDE-SEQ

[R2] mục 2.3, nguyên văn: "Extending our framework to deal with occurrence counts is
straightforward. Given an upper bound ϱ of the occurrence count, the iteration in Sanitize(T, Ph)
can be terminated whenever |M^T_Ph| ≤ ϱ." Tức [R2] đã nêu, trong một đoạn, một **giới hạn số lần
xuất hiện theo từng chuỗi** — đếm bằng cỡ tập khớp (mọi lần nhúng, chồng lấp, tăng theo hàm mũ),
không phải số lần xuất hiện không chồng lấp, không phân tích, không thực nghiệm. Câu "HIDE-SEQ với
r > 1 không phát biểu được bằng sup_seq" ở G1 của bản thiết kế vẫn đúng, nhưng bài phải trích đoạn
này của [R2] và nói rõ khác biệt: đơn vị đếm (không chồng lấp so với mọi lần nhúng), và các kết quả
chính xác của bài. Đã ghi cảnh báo tại G1 của bản thiết kế.
