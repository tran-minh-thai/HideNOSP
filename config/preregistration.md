# Chốt trước khi chạy SC3

Quy tắc 2.4.7: ngưỡng hiệu ứng thực tiễn và phép kiểm phải chốt **trước** khi chạy SC3.
Tệp này là bản chốt đó. Trạng thái hiện tại: **CHƯA CHỐT** — các ô còn `null` phải điền và
tệp phải được ghi vào git (`git log` làm bằng chứng thời điểm) trước lần chạy SC3 đầu tiên
có ý định đưa số vào bài.

Chừng nào tệp này chưa chốt, mọi lần chạy SC3 phải kèm cờ `--probe` để kết quả rơi vào
`results-probe/`.

---

## 1. Giả thuyết

**H2.** Trên các thực thể (bộ dữ liệu, σ, tập nhạy cảm), trung vị của hiệu
`Lost(baseline) − Lost(CUT-HIDE)` lớn hơn ngưỡng thực tiễn δ_L.

- Đơn vị lặp: một thực thể = một bộ ba (bộ dữ liệu, σ, tập nhạy cảm). Ghi ngay trong chú thích bảng.
- Phép kiểm: Wilcoxon dấu hạng, một phía.
- Hiệu chỉnh đa phép kiểm: Holm, theo số baseline được so.
- Cỡ hiệu ứng báo cáo: rank-biserial.
- **δ_L = _null_** ← phải điền. Đơn vị: số mẫu không nhạy cảm bị mất. Nêu căn cứ chọn giá trị.
- Mức ý nghĩa α = 0,05.

**H3.** Tỉ số `|R| / |R|*` của baseline tầng (b) lớn hơn 1 trên nhóm dữ liệu có ρ_rep cao, và
gần 1 trên nhóm suy biến.

- Ngưỡng phân nhóm: ρ_rep ≥ _null_ là nhóm chính; ρ_rep ≤ _null_ là nhóm đối chứng suy biến.
- Ngưỡng β cho nhóm đối chứng ràng buộc: β ≤ _null_.
- Hai ngưỡng trên phải chốt **trước** khi nhìn kết quả SC3, và phải dựa trên bảng SC2, vốn
  chạy độc lập với SC3.

---

## 2. Kết luận "không khác biệt"

Khi muốn kết luận hai phương pháp không khác nhau, dùng khoảng tin cậy của hiệu số cùng biên
tương đương chốt trước, không dùng "p > 0,05".

- Biên tương đương cho Lost: ± _null_.
- Biên tương đương cho SupDist: ± _null_.

---

## 3. Những gì KHÔNG cần kiểm định

- **HF.** Bằng 0 theo thiết kế với CUT-HIDE (bước 8 kiểm lại sau mỗi lần chạy). Nói một lần,
  không đưa vào bảng so sánh (quy tắc 2.4.2).
- **Ghost của phép che.** Bằng 0 theo P5(a). Đây là hệ quả của định lý, không đo như một phát
  hiện (quy tắc 1.2.2). Chỉ nhánh xoá ở SC5 mới báo cáo Ghost.
- **\|R\| của các phương pháp chính xác.** Bằng \|R\|\* theo P1. Chênh lệch ở đây là lỗi cài đặt,
  không phải kết quả.

---

## 4. Thứ tự chạy đã định

1. SC1 phải xanh (0 ca lệch) trước mọi kịch bản khác.
2. SC2 chạy trước SC3 và quyết định bộ nào vào nhóm chính, bộ nào vào nhóm đối chứng suy biến.
   Phân nhóm chốt tại đây, không sửa sau khi thấy kết quả SC3 (quy tắc 2.3.10).
3. SC3, SC4, SC5, SC7, SC8: số bất biến theo máy.
4. SC6 chạy sau cùng, trên máy đo đã khai, với `--machine=<tên máy>`.

---

## 5. Xử lý kết quả âm

Nếu trọng số nút thiết yếu không cải thiện Lost (rủi ro đã lường ở §12), báo cáo kết quả âm
đủ ba điều kiện của quy tắc 3.4.9. Đóng góp lý thuyết (bài toán, chỉ mục, P1–P7) không phụ
thuộc vào chiều của kết quả này.
