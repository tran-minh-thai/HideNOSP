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

---

## 6. Cập nhật 25/09/2026 — đã sửa theo phương án 1 (sửa cho đúng bài)

Người viết chọn phương án 1. `baseline/Sbsh.java` viết lại theo Thuật toán 5, cùng Thuật toán 1
(Cost, dùng cho Candidates-SBSH và cho thứ tự của Exclude-SBSH), Thuật toán 2 (Annotate) và
C-PA, C-SA, Replace của mục 4.3. Bảng ở mục 5 trên đây mô tả bản cài **trước** ngày này; cả năm
điểm nay đã theo bài:

| # | Điểm | Bản cài từ 25/09 |
|---|---|---|
| 1 | Nhãn xám | Theo Thuật toán 2 (xám = có mẫu không nhạy cảm nằm đúng ngưỡng nhưng xoá vị trí này không làm nó tụt); nhánh xám của Thuật toán 5 (tô màu lại rồi mới xoá) đã có |
| 2 | Bán kính đếm theo cung | Giữ nguyên, đúng |
| 2b | Miền r | `sbsh-rmax` lấy độ dài mẫu nhạy cảm dài nhất của tập, không lấy ℓ_max |
| 3 | C-PA / C-SA / Replace | Mỗi phía tăng k từ 1, dừng ở k đầu tiên tìm được; Replace lấy tập nhỏ hơn |
| 4 | Exclude-SBSH | Đã có (xem điểm mới dưới đây) |

**Phát hiện mới khi đọc lại toàn văn: bài tự mâu thuẫn về Exclude-SBSH.** Mục 5.2.2 viết
nguyên văn: "a variant of Algorithm 3 that removes the transaction that supports the largest
number of sensitive sequences from D_S, breaking ties arbitrarily if many transactions have the
same sanitization cost". Mục 5.2.3 giải thích lựa chọn trong ví dụ bằng mẫu **không** nhạy cảm:
"Although both T1 and T2 have a sanitization cost of 1 [...] T2 supports fewer nonsensitive
frequent sequences. [...] SBSH selects this transaction instead of T1." Tính tay trên Hình 1:

- đọc theo số mẫu **nhạy cảm**, T7 (hỗ trợ 2 mẫu nhạy cảm) được làm sạch trước T8, nên T7 mất
  d và f thành `* * h h h`, trái với Hình 1(f) in `d f * * *`;
- đọc theo số mẫu **không nhạy cảm** (giảm dần), rồi Cost (giảm dần, như Thuật toán 3), rồi chỉ
  số chuỗi, thì ra đúng Hình 1(f) và đúng tác dụng phụ ở Hình 4(b).

Bản cài theo cách đọc thứ hai. Thứ tự cuối cùng (chỉ số chuỗi) bài để "tuỳ ý"; chọn giảm dần thì
tái tạo đúng Hình 1(f), chọn tăng dần thì T2 và T3 đổi vai — cả hai đều đúng với bài.

**Những chỗ bài để ngỏ và cách đã chọn** (ghi cả trong Javadoc của lớp):

1. "P[i] = ∅" đọc là hết mảng P; vị trí không còn nút nào trên đường đầy đủ thì bỏ qua.
2. Thuật toán 5 chỉ xử lý vị trí xám **vẫn** xám khi tô lại. Tô lại ra đen hoặc đỏ thì cập nhật
   nhãn và để vòng sau xử lý theo nhánh đó (nếu không, vòng lặp đứng yên mãi).
3. C-PA/C-SA ở khoảng cách k lấy **cả tầng** cách k cung, chỉ gồm nút còn trên đường đầy đủ, và
   nhận khi mọi vị trí trong tầng mang nhãn đen. Hoà giữa C-PA và C-SA thì lấy C-PA.
4. Hoà trong phủ tập tham lam: ký hiệu xuất hiện trước trong chuỗi — tái tạo đúng Ví dụ 1.

**Bằng chứng** (`src/test/java/hidenosp/baseline/SbshPaperExampleTest.java`), chạy ở chế độ
`SEQUENCE_COUNT` tức bài toán của chính bài báo, không có khoảng cách tối đa:

- Hình 1(f): đúng các vị trí bị che {T2: 0; T6: 0; T7: 2, 3, 4; T8: 1}, khoảng cách 6 (Hình 4(b));
- Hình 4(b): đúng ba tác dụng phụ ⟨a,c⟩, ⟨a,e⟩, ⟨d,g⟩;
- Ví dụ 3 / Hình 2(e): Replace(3, G, 1) = {4, 5, 6}, Replace(3, G, 2) = {0, 1};
- hai ca tự dựng (không có trong bài) cho nhánh xám và cho "tính độ hỗ trợ như thể các vị trí đã
  đánh dấu đã bị xoá" dưới sup_NO.

Tiêm lỗi: 10 lỗi (khoá sắp xếp của Exclude-SBSH, thứ tự hoà, bỏ điều kiện đen của Replace, lấy
tập lớn hơn, Annotate đọc độ hỗ trợ ban đầu, bỏ tô màu lại, đếm theo số chuỗi ở chế độ sup_NO,
không bao giờ thay vị trí đỏ, tô màu lại bỏ qua các vị trí đã đánh dấu, C-SA tìm sai hướng) —
bắt được 10/10. Ví dụ của bài không có vị trí xám nào nên hai lỗi về nhánh xám chỉ bị bắt sau
khi thêm hai ca tự dựng.

**Chuyển sang sup_NO** (bản SC3 dùng): chỉ thay hàm độ hỗ trợ ở ba chỗ bài đọc độ hỗ trợ — ngân
sách giữ lại của Exclude-SBSH (tổng sup_NO của các chuỗi được giữ ≤ σ − 1 cho mỗi mẫu nhạy cảm),
phép so ngưỡng của Annotate, và điều kiện lặp. Phép sửa là che thay cho xoá, như mọi phương pháp.

**Họ Holm giữ nguyên bốn phần tử** (HHA, DBSH, SBSH-r1, SBSH-rmax) như `preregistration.md` §2;
không phải sửa bản chốt.

**Còn lại, không thuộc SBSH nhưng lộ ra khi làm việc này:** `Dbsh` tính Cost bằng tổng, theo từng
mẫu, số vị trí trên đường của ký hiệu rẻ nhất — **không** phải Thuật toán 1 (phủ tập tham lam,
trọng số là số lần xuất hiện của ký hiệu trong chuỗi). Hai đường cơ sở của cùng một bài đang
dùng hai hàm Cost khác nhau; phải đối chiếu DBSH với Thuật toán 3–4 trước khi chạy SC3 lấy số.
