# A25 — Đối chiếu SBSH với [R3]

**Nguồn.** A. Gkoulalas-Divanis, G. Loukides, "Revisiting sequential pattern hiding to enhance
utility", *KDD 2011*, tr. 1316–1324, doi 10.1145/2020408.2020605. Bản trong thư viện:
`ThuVienBaiBao/14_PPDM_AnGiauLuatNhayCam/[2011] Revisiting Sequential Pattern Hiding to Enhance
Utility.pdf`, 9 trang. Đã đọc Thuật toán 1 (Cost), 2 (Annotate), 3 (Exclude), 4 (DBSH),
5 (SBSH) và §4.3.

**Trạng thái.** Phần **sự kiện** của A25 đã xong: ba câu hỏi đều có câu trả lời dứt khoát dưới
đây. Phần **quyết định** — giữ cách cài hiện tại và khai là "biến thể của chúng tôi", hay thay
bằng đúng thủ tục của tác giả — vẫn thuộc về người viết.

---

## 1. Nhãn xám bao phủ vị trí nào?

**Cả hai phương án trong câu hỏi đều sai.** Câu hỏi giả định nhãn màu nói về cấu trúc lát cắt
(vị trí có mang nút nhạy cảm hay không, có nằm trên lát cắt hay không). Thuật toán 2 không nói
về chuyện đó chút nào: nó tô màu theo **tác dụng phụ lên mẫu KHÔNG nhạy cảm**.

Nguyên văn Thuật toán 2, `Annotate(p, D, TN, σ)`:

```
1. TN_p  ← các mẫu không nhạy cảm phổ biến trong TN có chứa sự kiện thứ p của T
2. nếu (không mẫu n_j ∈ TN_p nào có sup(n_j, D) = σ)
3.     trả về ĐEN
4. ngược lại
5.     với mỗi (n_j ∈ TN_p sao cho sup(n_j, D) = σ)
6.         nếu (sup(n_j, D) < σ sau khi xoá sự kiện thứ p của T)
7.             trả về ĐỎ
8. trả về XÁM
```

Vậy:

| Màu | Nghĩa |
|---|---|
| **Đen** | Không mẫu không nhạy cảm nào chứa vị trí này đang nằm **đúng** ở ngưỡng σ. Xoá vô hại |
| **Đỏ** | Có mẫu nằm đúng ở ngưỡng, **và** xoá vị trí này đẩy ít nhất một mẫu xuống dưới σ |
| **Xám** | Có mẫu nằm đúng ở ngưỡng, **nhưng** phép kiểm cục bộ không thấy mẫu nào tụt xuống |

Xám là **chưa chắc**, không phải là "không liên quan". Lý do có nhãn xám: phép kiểm ở bước 6 xét
từng vị trí một, trong khi các lần xoá thì cộng dồn. Vì thế SBSH (Thuật toán 5, bước 32) **chạy
lại `Annotate` trên chính vị trí đó ngay lúc sắp xoá**, và chỉ xoá nếu nó **vẫn còn** xám.

Phạm vi tô màu: bước 4 của Thuật toán 5 dùng một biến thể `Annotate-T` tô **mọi vị trí ứng với
một sự kiện nằm trong một mẫu nhạy cảm** s ∈ TS. Tức miền tô màu đúng bằng các vị trí mang sự
kiện nhạy cảm; ba màu chia nhau miền đó.

### Lệch với `Sbsh.java`

`annotate()` hiện tại tô: ĐEN cho vị trí thiết yếu của mẫu nhạy cảm, ĐỎ cho vị trí thiết yếu của
mẫu ở ngưỡng, và XÁM là **giá trị mặc định của mọi vị trí còn lại**. Ba chỗ lệch:

1. **Xám của ta nằm NGOÀI miền tô màu của họ.** Ta hiểu xám là "không mang nút nhạy cảm"; họ
   hiểu xám là "có mang, nhưng chưa chắc về tác dụng phụ". Đây đúng là điều mà TODO nghi ngờ.
2. **Nhánh xám của Thuật toán 5 hoàn toàn vắng mặt ở ta.** Vòng lặp của ta chỉ chọn trong
   `index.vitalPositions(p)`, tức chỉ vị trí thiết yếu, nên không vị trí xám nào được chọn bao
   giờ. Bước 32–37 của họ (chạy lại Annotate rồi xoá) không có tương ứng.
3. **Đỏ thì gần đúng.** Ta lấy "vị trí thiết yếu của một mẫu ở ngưỡng". Dưới `sup_NO`, thiết yếu
   nghĩa là che nó làm độ hỗ trợ giảm ít nhất 1, tức từ σ xuống σ−1 < σ — đúng điều kiện bước 6.
   Chỗ này khớp.

---

## 2. Bán kính r đếm theo cung hay theo vị trí?

**Theo CUNG của đồ thị khớp.** Nguyên văn §4.3:

> "we restrict ourselves to considering positions that are reachable from p by following at most
> r edges in G_{S,T}, where (radius) r ∈ [1, max_{j=1..|S|} |s_j|] is a parameter that controls
> the part of G_{S,T} to be searched."

Ví dụ 3 của bài xác nhận: với vị trí 3, r = 1 cho {4, 5, 6}; r = 2 cho {0, 1} "vì các nút mang
nhãn đó **đi được tới từ nút nhãn 3 bằng cách theo hai cung**".

Cung của đồ thị khớp nối hai tầng liên tiếp, nên r cung = r tầng. **Cách đọc hiện tại của
`Sbsh.java` ("in Nettree levels away from the red node") là ĐÚNG.** Đây là điểm duy nhất trong
ba điểm mà cài đặt của ta khớp với bài.

### Nhưng miền của r thì lệch

Bài chốt r ∈ [1, **độ dài mẫu nhạy cảm dài nhất**]. `Sc3MainComparison` gọi
`new Sbsh(problem.maxPatternLength())` cho `sbsh-rmax`, tức lấy **ℓ_max**. Hai cái chỉ trùng nhau
khi tập nhạy cảm có chứa một mẫu dài đúng ℓ_max. Với `--sensitiveSize=1` và mẫu lấy theo phân vị
thì phần lớn thực thể **không** thoả. Phải sửa thành `max` độ dài trên tập nhạy cảm, nếu không
"r lớn nhất" của SC7 không phải cái mà bài định nghĩa.

---

## 3. C-PA và C-SA khác Replace thường ở chỗ nào?

`Replace` **không phải** một thủ tục khác: nó là **cực tiểu của hai phép tìm có hướng**.

| Hàm | Làm gì |
|---|---|
| `C-PA(p, G, r)` | Tìm tập vị trí (i) toàn **đen**, (ii) chặn **mọi** đường đi đầy đủ qua nút p, (iii) nằm ở **tầng TRƯỚC** tầng của p, (iv) tới được từ p trong ≤ r cung. Bắt đầu từ 1 cung, mỗi vòng tăng một, **dừng ngay lần đầu tìm được một tập** hoặc khi vượt r |
| `C-SA(p, G, r)` | Y hệt, nhưng ở **tầng SAU** |
| `Replace(p, G, r)` | Gọi cả hai, trả về **tập NHỎ HƠN** trong hai tập |

### Lệch với `Sbsh.java`

`replace()` của ta quét **mọi** tầng trong bán kính (`|level − redLevel| ≤ radius`) ở **cả hai
phía trong cùng một vòng**, rồi lấy tập nhỏ nhất toàn cục. Khác ở chỗ:

- Họ **dừng ở lần đầu tìm thấy**, tính theo khoảng cách tăng dần, **riêng từng phía**. Ta **tối
  ưu trong toàn bán kính**. Nên tập của ta nhỏ hơn hoặc bằng tập của họ, và ta không tái hiện
  được hành vi "gần thì lấy luôn dù to" của họ.
- Vì mỗi phía dừng độc lập, `Replace` của họ so hai tập **gần nhất mỗi phía**, không phải hai
  tập nhỏ nhất mỗi phía.

Hệ quả cho SC7: biến thể "r nhỏ nhất" và "r lớn nhất" ở ta đo một thứ khác với ở bài. Ở ta, tăng
r chỉ mở rộng không gian tối ưu nên tập chỉ nhỏ đi; ở họ, tăng r có thể đổi hẳn tập được chọn.

---

## 4. Một lệch nữa, không nằm trong ba câu hỏi: `Exclude-SBSH` vắng mặt

Thuật toán 5 bước 1 gọi `Exclude-SBSH`, một biến thể của Thuật toán 3 **loại khỏi D_S chuỗi
đang chứa NHIỀU mẫu nhạy cảm NHẤT**, phá hoà bằng cách chọn tuỳ ý khi nhiều chuỗi cùng chi phí.
Bài nói thẳng lựa chọn này "có thể giảm đáng kể số tác dụng phụ, như thực nghiệm của chúng tôi
xác nhận".

`Sbsh.run()` không có bước này: nó duyệt chuỗi theo đúng thứ tự chỉ số. `Dbsh` thì **có** Exclude
(sắp theo chi phí rồi làm sạch cái rẻ trước, tương đương với cách sắp giảm dần rồi loại của
Thuật toán 3).

Vì đại lượng mà H2 kiểm là **Lost**, tức chính là tác dụng phụ, thiếu `Exclude-SBSH` nhiều khả
năng là chỗ lệch **nặng nhất** trong bốn chỗ — nó làm SBSH yếu đi đúng ở mặt mà SBSH sinh ra để
mạnh.

---

## 5. Tóm tắt và việc phải quyết

| # | Điểm | Cài đặt hiện tại |
|---|---|---|
| 1 | Nhãn xám | **Sai**, và nhánh xám của Thuật toán 5 vắng mặt |
| 2 | Bán kính đếm theo cung | **Đúng** |
| 2b | Miền r = độ dài mẫu nhạy cảm dài nhất | **Sai**, đang lấy ℓ_max |
| 3 | C-PA / C-SA / Replace | **Khác**: ta tối ưu toàn bán kính, họ dừng ở lần đầu, riêng từng phía |
| 4 | `Exclude-SBSH` | **Vắng mặt** |

Đúng 1 trên 5. Phần lý lịch của `preregistration.md` §2 viết: nếu SBSH không đối chiếu được thì
bỏ khỏi bảng, họ Holm còn hai. Đối chiếu thì đã xong, nhưng kết quả là **cài đặt hiện tại không
phải SBSH của [R3]**.

**Ba phương án, người viết chọn:**

1. **Sửa cho đúng bài.** Tốn công nhất, nhưng giữ được họ Holm bốn phần tử và giữ được câu "đúng
   đối thủ của họ" ở §12a. Bốn việc: viết lại `annotate` theo Thuật toán 2, thêm nhánh xám, tách
   `replace` thành C-PA và C-SA dừng-ở-lần-đầu, thêm `Exclude-SBSH`. Sửa cả miền r.
2. **Giữ nguyên và khai là biến thể.** Rẻ, nhưng khi ấy hai dòng SBSH trong bảng không còn là
   đường cơ sở của [R3] mà là hai biến thể của ta, nên **phải rút khỏi họ Holm** — họ còn hai
   (HHA, DBSH), và `preregistration.md` §2 phải sửa **trước** khi chạy SC3.
3. **Sửa phần rẻ, khai phần còn lại.** Thêm `Exclude-SBSH` và sửa miền r (hai việc nhỏ, đánh
   trúng chỗ lệch nặng nhất), khai rõ nhãn xám và Replace là biến thể. Vẫn phải quyết xem như
   thế có còn gọi là SBSH được không.

Dù chọn gì, phải làm **trước** khi chạy SC3 lấy số vào bài, và nếu sửa `preregistration.md` thì
đó là một commit riêng ghi rõ lý do, theo đúng đoạn đầu của tệp đó.
