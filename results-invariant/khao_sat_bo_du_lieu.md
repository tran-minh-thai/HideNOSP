# Khảo sát bộ dữ liệu: kế thừa của công trình liên quan, và bổ sung theo đặc trưng đo được

Ngày: 2026-09-12. Mọi con số ρ_rep và β dưới đây **đo bằng SC2 của chính dự án**, không chép từ
mô tả của người khác (§11.2). Tệp gốc: `SC2_20260912-*.csv` trong thư mục này.

Việc này làm theo hai bước, đúng thứ tự: (1) dùng lại bộ mà công trình liên quan đã dùng, để số
của bài đặt cạnh số đã công bố được; (2) bổ sung bộ chọn theo đặc trưng mà đóng góp của bài cần.

---

## 1. Công trình liên quan đã dùng bộ nào

| Công trình | Bộ dữ liệu | Mức tin cậy |
|---|---|---|
| [R1] NOSEP, Wu và cộng sự 2018 — chính bài định nghĩa sup_NO | **chuỗi DNA sinh học**, dữ liệu **chuỗi thời gian**, bộ **Gazelle** | Đã xác minh qua tra cứu công khai; Gazelle chính là BMS-WebView |
| [R20], [R2] dòng Abul | **trucks** (quỹ đạo xe tải rời rạc hoá lưới 10×10, 273 chuỗi) | Bản thiết kế ghi, ở mức đọc toàn văn |
| [R3] Gkoulalas-Divanis và Loukides 2011 | **MSN**, **BMS-WebView-1**, **TRUCKS** | Bản thiết kế ghi (§11.1 dòng SC8), ở mức đọc toàn văn |
| Chuẩn chung của khai phá mẫu tuần tự | MSNBC, BMS-WebView, FIFA, SIGN, Bible, Leviathan, Kosarak | Danh mục công khai của SPMF |

**Nói rõ giới hạn.** Tra cứu web xác minh được danh sách của [R1]. Danh sách của [R20] và [R3]
lấy từ bản thiết kế, nơi người viết đã tự đọc toàn văn hai bài đó; tra cứu công khai **không**
dựng lại được, nên không được ghi là "đã xác minh độc lập". Trước khi viết bài phải mở lại hai
bài gốc để chép đúng tên bản và ngưỡng ψ.

**Hai bộ chưa lấy được.** `trucks` chưa tìm được nguồn công khai. `BIKE` của SPMF (chuỗi trạm xe
đạp theo thời gian) là bản công khai gần nhất về **dạng** dữ liệu, nhưng **không phải** cùng bộ,
và bảng dưới cho thấy nó cũng không đạt về đặc trưng. Chuỗi thời gian mà [R1] dùng chưa lấy;
nếu cần thì rời rạc hoá một bộ của kho UCR.

---

## 2. Bảng chế độ dữ liệu, đo bằng SC2

Cột cuối tách theo μ(P): trung bình ρ_rep của mẫu **không lặp mục** (μ = 1, lớp mà P1 và P2 áp
dụng) so với mẫu **lặp mục** (μ ≥ 2, lớp chỉ có cận P3).

| Bộ | Cấu hình | Mẫu | ρ_rep nhỏ/tb/lớn | β nhỏ/tb/lớn | ρ_rep μ=1 / μ≥2 |
|---|---|---|---|---|---|
| **dna_mt745584** | 3 | 30 | **0,602 / 0,786 / 0,924** | 0,225 / 0,467 / 0,722 | **0,806 / 0,773** |
| **msnbc** | 5 | 60 | 0,064 / 0,323 / 0,750 | 0,010 / 0,238 / 0,618 | 0,218 / 0,434 |
| leviathan | 1 | 10 | 0,061 / 0,154 / 0,274 | 0,427 / 0,612 / **0,801** | 0,120 / 0,233 |
| bible | 1 | 10 | 0,023 / 0,153 / 0,248 | 0,194 / 0,500 / **0,850** | 0,153 / 0,152 |
| bike | 1 | 10 | 0,008 / 0,052 / 0,231 | 0,086 / 0,375 / 0,512 | 0,032 / 0,231 |
| fifa | 1 | 10 | 0,005 / 0,008 / 0,013 | 0,185 / 0,289 / 0,467 | 0,008 / — |
| sign | 2 | 20 | 0,000 / 0,000 / 0,000 | 0,000 / 0,209 / 0,534 | 0,000 / — |
| bms1 | 2 | 20 | 0,000 / 0,000 / 0,000 | 0,000 / 0,094 / 0,522 | 0,000 / — |

Cả tám bộ đều là tập mục một phần tử, tỉ lệ tuyến tính hoá bằng 0, nên đều nằm trong mô hình dữ
liệu của bài (§8.3). Một bộ đã bị loại ngay ở bước xem định dạng: **E-Shop** của SPMF (24.026
chuỗi, độ dài trung bình 62) trông rất hợp nhưng là **tập mục nhiều phần tử**, nằm ngoài mô hình.

---

## 3. Ba điều bảng này thay đổi

**(a) Nhóm chính không còn chỉ một bộ.** Trước đợt này chỉ MSNBC đạt. Nay có thêm bộ gen
MT745584, và nó **mạnh hơn MSNBC ở mọi phân vị**: ρ_rep nhỏ nhất của DNA (0,602) còn cao hơn
trung bình của MSNBC (0,323).

**(b) DNA là cấu hình duy nhất cho ρ_rep và β cao cùng lúc.** Trên MSNBC hai đại lượng kéo ngược
nhau theo độ rộng khoảng cách: nới [0,1] lên [0,10] đưa ρ_rep trung bình từ 0,321 lên 0,415 nhưng
β tụt từ 0,432 xuống 0,043. Trên DNA ở khoảng cách [0,1], ρ_rep vẫn 0,602–0,836 trong khi β đạt
0,420–0,722. Đó là chế độ dữ liệu mà cả hai giả thiết của bài cùng có hiệu lực.

**(c) Căng thẳng về μ(P) của giai đoạn 1 được gỡ.** Trên MSNBC, tỉ lệ lặp tập trung ở mẫu lặp
mục (0,434 so với 0,218), tức đúng lớp mà P1 và P2 **không** áp dụng — bài mạnh nhất ở nơi lý
thuyết yếu nhất. Trên DNA điều đó **đảo lại**: mẫu không lặp mục có ρ_rep trung bình 0,806, còn
cao hơn mẫu lặp mục 0,773. Nghĩa là giữ phạm vi ở μ(P) = 1, nơi P1, P2 và lời giải chính xác đều
đứng vững, **không còn phải trả giá bằng việc bỏ chế độ dữ liệu lặp cao**. Ba phương án nêu ở mục
3 của `giai_doan_1_tom_tat.md` nay có một phương án trội hẳn: giữ μ(P) = 1 làm phạm vi chính, và
đưa MSNBC ra làm bằng chứng rằng trên dữ liệu khác thì lớp lặp mục mới là lớp lặp nhiều, kèm
đúng con số.

---

## 4. Vai đề nghị cho từng bộ

| Vai | Bộ | Căn cứ |
|---|---|---|
| Nhóm chính | **dna_mt745584**, **msnbc** | ρ_rep trung bình 0,786 và 0,323; cả hai kế thừa: DNA là dạng dữ liệu của [R1], MSNBC là bản MSN của [R3] |
| Nhóm trung gian | leviathan, bible | ρ_rep 0,15 nhưng β 0,50–0,61: ràng buộc khoảng cách có hiệu lực mạnh trong khi sup_NO gần suy biến; cho thấy hai đại lượng độc lập nhau |
| Đối chứng suy biến | sign, bms1 | ρ_rep đúng bằng 0; bms1 còn suy biến cả β, và là bộ [R3] đã dùng |
| Đối chứng suy biến | fifa | ρ_rep 0,008 |
| Không dùng | bike | ρ_rep 0,052, chuỗi trung bình chỉ 7,3; giữ lại chỉ để nói rằng đã thử bản công khai gần nhất với trucks |
| Không dùng | e_shop | tập mục nhiều phần tử, ngoài mô hình dữ liệu |

Đây là **đề nghị**, không phải quyết định: ngưỡng phân nhóm ρ_rep và β vẫn phải chốt trong
`config/preregistration.md` theo nguyên tắc, trước khi chạy SC3, và không được suy ngược từ mong
muốn cho bộ nào vào nhóm nào.

---

## 5. Việc còn lại

| Việc | Ghi chú |
|---|---|
| Mở lại [R20] và [R3] chép đúng tên bản dữ liệu và ngưỡng ψ | Cần cho SC8; tra cứu công khai không thay được |
| Tìm nguồn `trucks`, hoặc khai rõ là không lấy được | SC8 dự kiến chạy trên MSN, BMS-WebView-1 và TRUCKS |
| Cân nhắc thêm một bộ chuỗi thời gian rời rạc hoá | [R1] dùng loại này; kho UCR là nguồn thường gặp |
| Quét thêm σ và khoảng cách cho dna, leviathan, bible | Hiện mỗi bộ mới mới có 1–3 cấu hình, so với 5 của msnbc |
