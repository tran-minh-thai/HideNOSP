# RUNBOOK — kho mã HideNOSP

Kho này cài đặt **chỉ mục cắt Nettree** và thuật toán **CUT-HIDE** của
`doc/THIET_KE_NGHIEN_CUU_AN_MAU_LAP.md`, cùng các baseline và các kịch bản SC1–SC8.
Mọi tên mục dạng §n dưới đây trỏ tới tệp thiết kế đó.

**Tệp thiết kế nằm NGOÀI kho git.** Kho chỉ gồm `code/` (§11.8); `doc/` và `paper/` ở cùng
thư mục dự án `HideNOSP Project/`, một cấp trên kho này. Bản sao đã clone sẽ không có chúng.

Ngôn ngữ: mã nguồn và chú thích bằng tiếng Anh (bản thảo nộp bằng tiếng Anh, và các script dò
tìm Python sẵn có cũng chú thích tiếng Anh); tài liệu vận hành như tệp này bằng tiếng Việt.

---

## 1. Yêu cầu và cách dịch

- JDK 25 (bản LTS, Eclipse Temurin). Mọi máy dùng cùng JDK 25; `tools/run.py` từ chối chạy lấy
  số vào bài trên phiên bản Java khác. Đổi từ JDK 17 ngày 25/09/2026. Số bất biến của giai đoạn 1
  (JDK 17, Windows) đã được chạy lại bằng JDK 26 trên macOS ở cùng commit `2f63b56`: 4 cấu hình
  SC2, 58 dòng, trùng khít sau khi chuẩn hoá ký tự xuống dòng và dấu phân cách đường dẫn.
- Maven 3.9 trở lên. Kho `~/.m2` đã có sẵn junit 5.10.2 nên dịch được ở chế độ offline.

```bash
mvn -o -q compile
```

```bash
mvn -o test
```

`mvn test` chạy `WorkedExamplesTest`, gồm bốn ví dụ chạy tay của bài (§8.1, §8.2, G3). Nếu một
trong bốn ví dụ hỏng thì **bài** hỏng chứ không chỉ mã: các con số đó được in trong bản thảo.

---

## 2. Chạy một kịch bản

```bash
java -cp target/classes hidenosp.exp.Runner
```

lệnh trên in danh sách kịch bản và tuỳ chọn của từng cái.

**Lấy số vào bài thì gọi qua `tools/run.py`, không gọi thẳng `Runner`:**

```bash
python tools/run.py SC1 --trials=1500 --seed=2026
```

Script bọc này dịch lại, kiểm cây làm việc sạch, rồi đặt `HIDENOSP_REVISION` bằng sha commit
trước khi gọi `Runner`. Gọi thẳng `Runner` thì tệp kết quả mang dấu `unversioned` và không truy
ngược được về commit nào sinh ra nó, tức vi phạm quy tắc 2.6.8 và điều đã chốt ở
`config/protocol.yaml`. Gọi thẳng chỉ dùng khi chạy thử cho vui.

Ví dụ gọi thẳng để xem nhanh:

```bash
java -cp target/classes hidenosp.exp.Runner SC1 --trials=1200 --seed=7
```

```bash
java -cp target/classes hidenosp.exp.Runner SC2 --sigma=20 --gmax=2 --patterns=10
```

```bash
java -cp target/classes hidenosp.exp.Runner SC3 --sigma=15 --gmax=2 --sensitiveSets=5 --probe
```

Tuỳ chọn dùng chung cho mọi kịch bản:

| Tuỳ chọn | Mặc định | Ý nghĩa |
|---|---|---|
| `--seed` | 2026 | Hạt giống, vào dòng 3 bảng giao thức và vào dấu định danh lần chạy |
| `--machine` | `dev` | Tên máy đo đã khai. Lần chạy gắn `dev` **không được** ghi số đo thời gian |
| `--root` | `.` | Thư mục chứa ba thư mục kết quả |
| `--probe` | tắt | Đẩy tệp bất biến sang `results-probe/` thay vì `results-invariant/` |

Riêng `tools/run.py` nhận thêm ba tuỳ chọn của chính nó, không chuyển tiếp cho `Runner`:

| Tuỳ chọn | Mặc định | Ý nghĩa |
|---|---|---|
| `--jvm=<cờ>` | `-Xmx4g` | Cờ cho JVM; lặp lại được |
| `--allow-dirty` | tắt | Cho chạy khi cây làm việc còn sửa đổi chưa commit |
| `--no-build` | tắt | Bỏ qua bước dịch lại |

Cây làm việc còn sửa đổi chưa commit thì script **từ chối chạy**, trừ khi lần chạy đó có `--probe`
(số dò tìm, được phép) hoặc bạn khai rõ `--allow-dirty`. Cả hai trường hợp đều đóng dấu hậu tố
`-dirty`, để một tệp kết quả không bao giờ nhận nhầm một sha sạch.

Khi chưa có bộ dữ liệu thật, SC2 và SC3 chạy trên một cơ sở dữ liệu tổng hợp nhỏ có sẵn trong mã.
**Mọi lần chạy như vậy phải kèm `--probe`**: đó là số đo dò tìm, không phải kết quả (§8.2).

### Script dò tìm Python

`kiemtra_v2.py` và `probe/*.py` in ra màn hình. §8.2 đòi các con số đó nằm ở `results-probe/`,
nên phải hứng đầu ra vào đó thay vì chỉ đọc trên màn hình:

```bash
python kiemtra_v2.py > results-probe/kiemtra_v2_$(date -u +%Y%m%d-%H%M%S).txt 2>&1
```

```bash
cd probe && python oldprob.py > ../results-probe/oldprob_$(date -u +%Y%m%d-%H%M%S).txt 2>&1
```

`probe/*.py` nạp `kiemtra_v2` qua `sys.path.insert(0, '..')`, nên phải chạy từ trong `probe/`.
`probe/sideeffect.py` cần `numpy`. Lô chạy lại gần nhất mất khoảng ba phút cho `kiemtra_v2.py`
và vài giây cho bốn script còn lại; kết quả và bảng đối chiếu với §8.2 nằm ở `results-probe/`.

Không dùng `>>` để ghi thêm vào tệp nằm trên ổ Google Drive: ổ ảo trả kích thước tệp cũ nên lần
ghi thêm đè vào giữa tệp. Ghi ra đĩa cục bộ rồi chép sang, hoặc chỉ dùng `>`.

---

## 3. Chạy thực nghiệm giai đoạn 1

Theo §12b, giai đoạn 1 là **cài đặt, SC1–SC2, chốt `config/preregistration.md`**. Nó phải xong
trước SC3, vì SC2 quyết định bộ dữ liệu nào vào nhóm chính và bộ nào vào nhóm đối chứng suy
biến, mà quyết định đó không được sửa sau khi đã nhìn kết quả so sánh [2.3.10].

### Bước 0. Lấy dữ liệu

```bash
python tools/fetch_datasets.py
```

Tải MSNBC, SIGN, FIFA và BMS-WebView-1 từ kho công khai của SPMF vào `datasets/`, tính SHA-256
và ghi `datasets/provenance.tsv`. Chạy lại là không tải lại nếu tổng kiểm còn khớp. Muốn một bộ
riêng thì truyền tên: `python tools/fetch_datasets.py msnbc`.

### Bước 1. SC1 phải xanh trước đã

```bash
python tools/run.py SC1 --trials=1500 --seed=2026
```

Kết quả chấp nhận được duy nhất là **0 ca lệch**. SC1 đối chiếu luồng cực đại với vét cạn, kiểm
P1, kiểm đặc trưng nút thiết yếu L5, đối chiếu cập nhật gia tăng với dựng lại, và kiểm P5.
Chưa xanh thì mọi con số sau đó vô nghĩa.

### Bước 2. SC2 trên từng bộ và từng σ

```bash
python tools/run.py SC2 --dataset=datasets/msnbc.txt --format=SPMF --sigma=2000 --gmin=0 --gmax=3 --maxPatternLength=4 --patterns=12
```

```bash
python tools/run.py SC2 --dataset=datasets/sign.txt --format=SPMF --sigma=200 --gmin=0 --gmax=3 --maxPatternLength=4 --patterns=10
```

```bash
python tools/run.py SC2 --dataset=datasets/bms1.txt --format=SPMF --sigma=100 --gmin=0 --gmax=3 --maxPatternLength=4 --patterns=10
```

```bash
python tools/run.py SC2 --dataset=datasets/fifa.txt --format=SPMF --sigma=3000 --gmin=0 --gmax=3 --maxPatternLength=4 --patterns=10
```

Quét thêm σ và độ rộng khoảng cách để thấy ρ_rep và β đổi theo cấu hình chứ không phải theo bộ:
đổi `--sigma` và `--gmax`. Thời gian tham khảo trên máy phát triển: SIGN vài giây, BMS1 vài giây,
MSNBC khoảng 13 giây, FIFA khoảng hai phút (|Σ| = 2990 làm ứng viên mức 2 đông).

**Không** kèm `--probe` ở bước này: đây là kết quả thật, bất biến theo máy, thuộc về
`results-invariant/`. Cờ `--probe` chỉ dành cho chạy thử trên dữ liệu tổng hợp có sẵn trong mã.

### Bước 3. Đọc bảng SC2 rồi mới chốt preregistration

Với mỗi bộ, xem hai cột:

- **ρ_rep gần 0** nghĩa là sup_NO suy biến thành độ hỗ trợ theo chuỗi. Bộ đó là **đối chứng suy
  biến**, không được đứng trong nhóm chứng minh đóng góp [2.2.4].
- **β gần 0** nghĩa là ràng buộc khoảng cách không có hiệu lực trên cấu hình đó.

Điền ngưỡng phân nhóm và δ_L vào `config/preregistration.md`, commit tệp đó (lịch sử git là bằng
chứng thời điểm), rồi mới sang SC3.

**Đã làm, ngày 12/09/2026.** `config/preregistration.md` và `config/protocol.yaml` đã chốt mọi
tham số chặn SC3; căn cứ từng con số ở `config/de_xuat_gia_tri_nhom_A.md`. Ba việc kỹ thuật phải
xong trước lần chạy SC3 lấy số vào bài: gắn `HIDENOSP_REVISION`, nâng `--maxPatternLength` lên 4,
và kiểm B_ε không rỗng.

### Bước 4. Kiểm luật thư mục kết quả

```bash
python tools/check_results.py
```

---

## 4. Ba thư mục kết quả và luật của chúng (§11.8)

| Thư mục | Chứa gì | Ai được ghi |
|---|---|---|
| `results-invariant/` | Lost, Ghost, \|R\|, HF, kích thước Nettree | Máy nào cũng được; **cấm** cột thời gian hay bộ nhớ |
| `results/` | Thời gian treo tường, thời gian CPU, bộ nhớ đỉnh | Chỉ máy đo đã khai (`--machine=<tên>`), và phải có cột thời gian |
| `results-probe/` | Dò tìm trên máy phát triển, dữ liệu thay thế | Máy nào cũng được; cấm cột thời gian |

`hidenosp.io.ResultSink` cưỡng chế ba luật này và ném lỗi ngay khi mở tệp, không đợi tới lúc
đọc bảng. Lý do đặt kiểm tra máy móc ở đây: sai sót kiểu "số thời gian đo trên laptop rồi in
vào bảng như thể đo trên máy đã khai" là loại sai im lặng và đắt nhất.

Mỗi tệp mang dấu định danh lần chạy (§2.6.8) trong tên và trong phần chú thích đầu tệp:
thời điểm UTC, máy, phiên bản mã (biến môi trường `HIDENOSP_REVISION`), hạt giống, phiên bản JDK,
cỡ heap tối đa.

---

## 5. Trạng thái cài đặt

### Đã cài và đã kiểm

| Thành phần | Lớp | Ghi chú |
|---|---|---|
| Nettree, bảng vị trí dùng chung | `core.Nettree` | §2, §9.1 |
| Luồng cực đại sức chứa nút (Dinic) | `core.flow.Dinic`, `core.flow.NettreeFlow` | L0 |
| Lát cắt nhỏ nhất | `NettreeFlow.minCut` | P1, P2 |
| Nút thiết yếu: một luồng + một lần SCC | `NettreeFlow.vitalNodes`, `core.flow.ResidualScc` | L5 |
| Cập nhật gia tăng sau mỗi lần che | `NettreeFlow.maskPosition` | §9.1; SC1 đối chiếu với dựng lại |
| Lát cắt nhỏ nhất có trọng số | `NettreeFlow.minWeightMinCut` | P6(b), chính xác |
| Quét λ có chặn dưới và chứng nhận | `hide.LambdaSweep` | P6(d) |
| Đường cong chi phí `c_S(k)` | `hide.CostCurve` | §9.2 bước 5 |
| Phân bổ giữa các chuỗi | `hide.Allocation` | P7, quy hoạch động và cả bản tham lam để so ở SC7 |
| Đếm tiến và lùi | `index.PathCounts` | §9.1; thay quy hoạch động theo vị trí của [R20] |
| Chỉ mục gộp nhiều mẫu | `index.NettreeCutIndex` | §9.1 |
| CUT-HIDE, một mẫu | `hide.CutHide` | Chính xác khi μ(P)=1; bước 1–6, 8 |
| CUT-HIDE, nhiều mẫu, HIDE-SEQ | `hide.CutHide` | Tham lam theo tỉ số, bước 7 |
| CUT-HIDE, nhiều mẫu, HIDE-DB | `hide.CutHide.runMultiPatternDatabase` | Tham lam theo tỉ số trên toàn CSDL; **không** có bảo đảm hệ số khi σ > 1 |
| Nguyên–đối ngẫu, hệ số ℓ_max và chặn dưới cho từng thực thể | `hide.PrimalDualCover` | Chỉ khi σ = 1 (xem mục 7 khoản 7 và 9) |
| Độ đo HF, Lost, Ghost, DistR, SupDist | `metrics.Metrics` | §2 |
| `F_σ(D)`, `B_ε`, `𝒮⁺` | `metrics.FrequentPatterns` | §9.2 bước 3 |
| ρ_rep, β, \|R\|\* | `metrics.DataProfile` | §8.4 |
| Baseline tầng (a) | `baseline.RandomMasking` (HH/HR/RH/RR), `baseline.OccurrenceCountHeuristic` | §11.3; chạy được với \|𝒮\| bất kỳ |
| Kiểm B_ε rỗng báo lỗi | `hide.CutHide.requireNonEmptyBand` | Chốt ở `protocol.yaml` dòng 2 |
| Baseline tầng (b): HHA | `baseline.Hha` | [R20]; hai cách tính δ cho SC6 |
| Baseline tầng (b): DBSH | `baseline.Dbsh` | [R3]; Cost theo ký hiệu, Exclude, Update-paths |
| Baseline tầng (b): SBSH | `baseline.Sbsh` | [R3], Thuật toán 5 kèm 1–3 và mục 4.3; tái tạo đúng Hình 1(f), Hình 4(b) và Ví dụ 3 của bài (`SbshPaperExampleTest`). Chỗ bài để ngỏ: `config/doi_chieu_sbsh_r3.md` mục 6 |
| Baseline tầng (c): bộ giải chính xác | `baseline.IlpExact` | Nhánh-cận, thực thể nhỏ; tuỳ chọn cực tiểu mẫu mất |
| Tải bộ dữ liệu kèm tổng kiểm | `tools/fetch_datasets.py` | §11.2 |
| SC1, SC2, SC3 | `exp.Sc1…`, `exp.Sc2…`, `exp.Sc3…` | Chạy được |

### Chưa cài (khung có sẵn, đặc tả nằm trong Javadoc của từng lớp)

| Việc | Nơi | Chặn kịch bản nào |
|---|---|---|
| Đối chiếu DBSH và HHA với bài gốc: DBSH còn dùng hàm Cost khác Thuật toán 1 của [R3] (tổng số vị trí trên đường của ký hiệu rẻ nhất theo từng mẫu, thay cho phủ tập tham lam) | `baseline.Dbsh`, `baseline.Hha` | SC3, SC8 |
| Bản gốc của HHA, DBSH trên bài toán cũ (độ hỗ trợ theo số chuỗi); SBSH đã có (`Sbsh.Support.SEQUENCE_COUNT`) | `baseline.Hha`, `Dbsh` (cờ `originalProblem`) | SC8 |
| Giải thuật di truyền, bản thuần và bản lai khởi tạo từ lát cắt | `baseline.GaHybrid` | SC3 (tầng c) |
| SC4, SC5, SC6, SC7, SC8 | `exp.Sc4…` đến `exp.Sc8…` | chính nó |
| Đối chiếu với NETGAP của [R1] | `exp.Sc1ImplementationCheck` | SC1 (phần còn thiếu) |
| Đo bộ nhớ thường trú của tiến trình | `util.MemoryProbe` | SC6 |

Tìm nhanh: `grep -rn "TODO(code)" src/`.

---

## 6. Bảng giao thức 9 dòng

`config/protocol.yaml` là bản điền. Quy tắc 2.6.2 đòi điền **ngay ở bản nháp đầu**, không đợi
tới lúc nộp. Những ô mã sinh ra được (phiên bản JDK, cỡ heap, hạt giống) đã được in sẵn vào
đầu mỗi tệp kết quả, chép từ đó sang.

`config/preregistration.md` chốt ngưỡng hiệu ứng δ_L và phép kiểm **trước** khi chạy SC3
(quy tắc 2.4.7). **Đã chốt ngày 12/09/2026**; sửa bất kỳ con số nào từ đây phải là một commit
riêng có ghi lý do.

`config/quyet_dinh_can_chot.md` liệt kê **mọi** điểm còn phải quyết định của dự án, gộp theo chủ
đề, tách nhóm chặn SC3 và nhóm chặn lúc nộp. Sinh từ một đợt rà mười nguồn rồi thẩm định lại từng
điểm; `tools/gop_quyet_dinh.py` gộp kết quả thành tài liệu đó.

`config/de_xuat_gia_tri_nhom_A.md` đề xuất **giá trị cụ thể** cho từng điểm chặn SC3, mỗi giá trị
kèm căn cứ đo được, cái giá phải trả, và cách bác bỏ. Đây là bản để duyệt, chưa phải bản chốt:
chốt là ghi số vào hai tệp trên rồi commit.

---

## 7. Những quyết định cài đặt phải khai trong bài

Đây là các chỗ mã phải chọn một cách làm mà tệp thiết kế chưa chốt. Mỗi dòng cần một câu trong
bản thảo hoặc trong bảng giao thức.

1. **Thang trọng số.** `w(i)` là số thực, còn luồng cực đại cần sức chứa nguyên. `CostCurve.SCALE`
   = 10⁶ là đơn vị dấu phẩy tĩnh. Hai lần chạy khác thang thì không so được với nhau.
2. **𝒮⁺.** Thiết kế viết "𝒮 cùng mọi mẫu cha" mà không chốt quan hệ cha. Mã đọc là quan hệ
   **chuỗi con** trên dãy mục (`FrequentPatterns.isSubsequence`), theo quy ước của [R3].
3. **Tỉa theo tiền tố khi khai phá `F_σ`.** Hợp lệ vì `sup_NO(P·a, D) ≤ sup_NO(P, D)`: tiền tố của
   một họ lần xuất hiện đôi một không chồng lấp vẫn đôi một không chồng lấp. SC1 đối chiếu bản
   tỉa với bản vét cạn trên thực thể nhỏ.
4. **Ngữ nghĩa ⊥.** `EventSequence.MASK = -1`, không khớp mục nào, đúng như §2.
5. **Tuyến tính hoá tập mục.** Tệp SPMF có tập mục nhiều phần tử nằm ngoài mô hình dữ liệu của
   bài (§8.3). `DatasetReader` sắp theo mã mục tăng dần và trả về tỉ lệ vị trí bị ảnh hưởng;
   quy tắc 2.2.10 đòi khai tỉ lệ này cạnh bộ dữ liệu.
8. **Va chạm ký hiệu: δ đang mang hai nghĩa.** Quy tắc 3.2.4 đòi mỗi ký hiệu định nghĩa đúng một
   lần, nhưng hiện δ dùng cho hai đại lượng không liên quan:

   - **δ(i)** của [R20]: số lần khớp đi qua vị trí i, tức `|M_T| − |M_T che i|`, đếm **mọi** lần
     nhúng kể cả chồng lấp. Ở mã là `PathCounts.throughPosition`; `Hha.Delta` chọn cách tính.
   - **δ_L**: ngưỡng hiệu ứng thực tiễn của H2, chốt ở `preregistration.md` = 0,02.

   Nên giữ δ(i) cho [R20] vì phải giữ nguyên ký hiệu khi trích của họ, và đổi δ_L sang chữ khác
   trong bản thảo.

   Kèm theo, một chỗ dễ đọc nhầm cần một câu trong bài: **δ(i) không phải mức giảm sup_NO.** Ví dụ
   chạy tay của G3 cho thấy vị trí có δ lớn nhất lại không làm giảm sup_NO chút nào. δ đếm mọi lần
   khớp; cái bài cần là nút thiết yếu của L5. Khác nhau ở chỗ đó chính là RQ3.

   Và trong làm tròn quy hoạch tuyến tính, hệ số xấp xỉ là **cỡ ràng buộc lớn nhất** (m ≤ ℓ_max),
   **không phải** bậc của biến (chính là δ). Lấy nhầm δ làm hệ số sẽ cho một bảo đảm sai, vì δ
   không bị chặn còn m thì có.

7. **Phạm vi của bảo đảm hệ số ℓ_max.** §7 của bản thiết kế liệt kê trong đóng góp: "với nhiều
   mẫu (NP-khó) dùng làm tròn quy hoạch tuyến tính **có bảo đảm hệ số ℓ_max**". Bảo đảm đó chỉ
   đứng khi **ẩn hết**, tức σ = 1 với HIDE-DB hoặc r = 1 với HIDE-SEQ. Lý do:

   - Khi σ = 1, điều kiện là "không lần xuất hiện nào sống sót". Mỗi lần xuất hiện L cho một ràng
     buộc phủ `Σ_{i ∈ L} x_i ≥ 1` với đúng m ≤ ℓ_max biến. Làm tròn mọi `x_i ≥ 1/ℓ_max` lên 1 cho
     lời giải khả thi với chi phí ≤ ℓ_max lần nghiệm quy hoạch tuyến tính, tức ≤ ℓ_max lần tối ưu.
     Đây là làm tròn phủ chuẩn với cỡ ràng buộc bị chặn.
   - Khi σ > 1, điều kiện là "không **họ σ lần xuất hiện đôi một không chồng lấp** nào sống sót".
     Ràng buộc tương ứng có tới σ·m biến, nên cùng phép làm tròn chỉ cho hệ số **σ·m**, không phải
     ℓ_max. Số ràng buộc cũng bùng nổ nên còn phải sinh ràng buộc trễ.

   Vậy câu trong §7 cần một mệnh đề giới hạn phạm vi trước khi vào bài. Cái đang cài cho σ > 1 là
   **tham lam theo tỉ số** của bước 7 §9.2, không có bảo đảm hệ số, và SC3 đo khoảng cách của nó
   tới bộ giải chính xác trên thực thể nhỏ.

6. **Trọng số nút khi μ(P) ≥ 2.** Một vị trí mang nhiều nút; mã gán trọng số vị trí cho mọi nút
   của vị trí đó. P1 và P2 vốn không áp dụng trong trường hợp này, nên đây là lựa chọn cài đặt
   chứ không phải hệ quả của mệnh đề nào.

9. **Nguyên–đối ngẫu thay cho làm tròn quy hoạch tuyến tính.** §7 gọi đóng góp này là "làm tròn
   quy hoạch tuyến tính". Cái đã cài (`PrimalDualCover`) là phương pháp **nguyên–đối ngẫu**, đạt
   cùng hệ số ℓ_max qua cùng một quy hoạch tuyến tính nhưng là **kỹ thuật khác**, nên câu trong
   bài phải sửa theo. Ba lý do:

   - không cần bộ giải, giữ đúng lời hứa "không phụ thuộc ngoài, dựng ngoại tuyến" ở
     `config/protocol.yaml`;
   - số lần xuất hiện tăng theo hàm mũ (chính vì thế `PathCounts` phải bão hòa) nên không bao giờ
     viết ra hết ràng buộc được. Nguyên–đối ngẫu mỗi lượt chỉ cần **một** ràng buộc bị vi phạm,
     tìm bằng đường đi ngắn nhất trên Nettree với khe hở làm trọng số nút;
   - nó sinh một đối ngẫu khả thi, nên **mỗi thực thể tự mang chặn dưới của nó**. Bài báo cáo được
     tỉ số đạt thực tế chứ không chỉ trường hợp xấu nhất, cùng tinh thần với cặp chặn có chứng chỉ
     của quét λ.

   **Bước xóa ngược là bắt buộc, không phải tô điểm.** Với chi phí đơn vị, ràng buộc rẻ nhất luôn
   là ràng buộc chưa vị trí nào căng, nên cả m vị trí của nó căng cùng một lượt: mỗi lượt mua một
   đơn vị đối ngẫu và tiêu m. Đo trên ba cơ sở dữ liệu ngẫu nhiên, tỉ số ra đúng 2,000 — bằng
   trường hợp xấu nhất — ở **cả ba**. Thêm bước xóa ngược (bỏ vị trí thừa, chọn sau bỏ trước) thì
   cả ba xuống **1,000**, tức chi phí bằng đúng chặn dưới nên lời giải là **tối ưu**, chứ không
   chỉ trong hệ số. Xóa vị trí chỉ làm giảm chi phí và không đụng tới đối ngẫu nên bảo đảm còn
   nguyên, và tập thu được là **tối tiểu**: bỏ bất kỳ vị trí nào cũng lộ lại một lần xuất hiện
   (`PrimalDualCoverTest.theMaskSetIsMinimal` ghim điều này).

   **Chặn dưới là của thực thể, không phải của riêng phương pháp.** Đối ngẫu khả thi cận dưới
   nghiệm quy hoạch tuyến tính, mà nghiệm đó cận dưới tối ưu nguyên, tức |R|* của **chính thực
   thể**. Vậy cột `lower_bound` chặn dưới cho **mọi** phương pháp chạy trên hàng đó, không riêng
   nguyên–đối ngẫu. Đây là cái được thật sự: bộ giải chính xác chỉ chạy nổi trên thực thể nhỏ
   (64 ứng viên, 12 lần che), còn chặn dưới này chạy trên thực thể cỡ nào cũng được, nên SC3 báo
   cáo được khoảng cách tới tối ưu của từng đường cơ sở trên toàn bộ dữ liệu. Chỉ đúng khi σ = 1.

   **Đo thử: nó không thắng về |R|.** Trên CSDL tổng hợp của SC3, một mẫu nhạy cảm thì mọi phương
   pháp trừ RR đều chạm đúng tối ưu — vì P1c cho |R|* = sup_NO và tham lam nào cũng tới đó, nên
   σ = 1 với một mẫu **không phân biệt được gì**. Với ba mẫu nhạy cảm, năm tập:

   | tập | chặn dưới | nguyên–đối ngẫu | tỉ số | tốt nhất của đối thủ | tỉ số |
   |----:|----------:|----------------:|------:|---------------------:|------:|
   | 0 | 103 | 109 | 1,058 | 107 | 1,039 |
   | 1 |  56 |  63 | 1,125 |  61 | 1,089 |
   | 2 | 110 | 112 | 1,018 | 113 | 1,027 |
   | 3 | 128 | 131 | 1,023 | 134 | 1,047 |
   | 4 |  81 |  93 | 1,148 |  87 | 1,074 |

   Thắng 2 trên 5. Vậy đóng góp của nó **không phải** là |R| nhỏ hơn, mà là (a) bảo đảm chứng minh
   được, và (b) cái chứng chỉ cho phép nói "mọi phương pháp ở hàng này cách tối ưu không quá 1,02
   đến 1,15 lần" — chặt hơn hẳn trường hợp xấu nhất ℓ_max. Bài phải trình bày đúng như thế, đừng
   dựng nó thành phương pháp tốt nhất rồi để bảng phản lại.

   Hệ quả cho SC3: phải báo cáo **cả** hệ số chứng minh được (ℓ_max, trường hợp xấu nhất) **lẫn**
   tỉ số đo được, và nói rõ hai cái đó khác nhau.

10. **Chi phí đơn vị, chưa có biến thể có trọng số.** `PrimalDualCover` cực tiểu |R|, tức mục tiêu
    (1) của §2. Lập luận không hề nhìn vào chi phí ngoài việc so khe hở, nên mọi chi phí không âm
    đều giữ được hệ số; nhưng biến thể có trọng số cần nối B_ε theo đúng cách `CutHide` làm, chưa
    làm thì không nên bày ra một công tắc không có tác dụng gì.
11. **Cách tính p của Wilcoxon kiểu Pratt.** Bản chốt định Pratt và hạng trung bình, nhưng
    **không** định tính p bằng phân bố chính xác hay xấp xỉ chuẩn. Đây là một lựa chọn cài đặt
    phải khai, vì ba cách cho ba số khác nhau.

    Trong `scipy` 1.18, `method="exact"` dưới Pratt trả về giá trị **sai**: nó dùng phân bố dấu
    hạng chuẩn trên cả n quan sát, không kể rằng hạng của các hiệu bằng 0 không được phép nhận
    dấu. Trên một ví dụ 12 quan sát có 4 số 0, liệt kê tay toàn bộ 2⁸ khả năng cho
    p = 29/256 = 0,113281; `method="auto"` cho đúng số đó, `"exact"` cho 0,211914 và `"approx"`
    cho 0,099370.

    Vậy `tools/thong_ke.py` chốt `method="auto"`, và `tools/thong_ke_test.py` ghim nó bằng phép
    liệt kê tay chứ không bằng việc tin vào scipy. Bài phải khai "p tính bằng phân bố chính xác
    có điều kiện của Pratt khi cỡ mẫu cho phép".

12. **Ba khoảng trống trong bản chốt, phát hiện khi cài `thong_ke.py`.** Không tự vá, chỉ ghi ra:

    - **|R| của CUT-HIDE lớn hơn baseline.** Mục 5 định nghĩa hai chế độ báo cáo: cùng |R| thì
      kiểm định, baseline dùng **nhiều hơn** thì báo cáo riêng. Trường hợp CUT-HIDE dùng nhiều
      hơn **không** được định nghĩa. Bỏ im là thiên vị có lợi cho ta, nên script đếm riêng thùng
      đó và in cảnh báo; bản chốt cần một dòng cho ca này.
    - **Mã OT và OOM chưa có chỗ ghi.** Mục 5 bảo loại thực thể mang mã OT hoặc OOM, nhưng SC3
      không có cột nào để ghi. Script đọc cột `status` nếu có; chưa có thì phải thêm trước khi
      chạy thật, không thì quy tắc loại trừ đó không thi hành được.
    - **Hạt giống của phương pháp tách khỏi hạt giống lấy mẫu.** Mục 5 đòi 10 hạt giống
      2026–2035 lấy trung vị. Trước đây SC3 dùng **một** hạt cho cả việc lấy mẫu tập nhạy cảm
      lẫn việc chạy phương pháp, nên đổi hạt là đổi luôn tập nhạy cảm và mười lần chạy không
      ghép cặp được với nhau. Đã tách thành `--seed` và `--methodSeed`, thêm `--repeats`.

---

## 8. Bản đồ từ §9.2 sang mã

| Bước §9.2 | Lớp, phương thức |
|---|---|
| 1. Dựng NT(P,S) | `Nettree.build`, `NettreeCutIndex.add` |
| 2. Luồng cực đại F_S | `NettreeFlow` (dựng xong là có) |
| 3. Tập vị trí thiết yếu của `B_ε` | `NettreeFlow.vitalPositions`, `FrequentPatterns.nearThreshold` |
| 4. Trọng số vị trí `w(i)` | `NettreeCutIndex.positionWeights`, `FrequentPatterns.weightFactors` |
| 5. Đường cong `c_S(k)` | `CostCurve.build`, `LambdaSweep.run` |
| 6. Phân bổ giữa các chuỗi | `Allocation.solve` (và `Allocation.greedy` để so ở SC7) |
| 7. Nhiều mẫu nhạy cảm | `CutHide.runMultiPattern` |
| 8. Kiểm hậu điều kiện | `CutHide.verify` |

---

## 9. Giới hạn kỹ thuật đã biết

- `Dinic.dfs` đệ quy. Với chuỗi rất dài và khoảng cách rộng, ngăn xếp có thể tràn; cần chuyển
  sang vòng lặp trước khi chạy SC6 ở quy mô lớn.
- `PathCounts` bão hoà ở `Long.MAX_VALUE / 4`. Giá trị bão hoà vẫn dùng làm khoá so sánh được
  (đó là tất cả những gì heuristic của [R20] cần) nhưng **không được in ra như số đếm chính xác**;
  `PathCounts.isExact()` cho biết đã bão hoà hay chưa.
- Một kịch bản quét **phải** dựng một `Metrics.Context` cho mỗi cấu hình (bộ, σ, khoảng cách,
  ℓ_max) và dùng lại nó, thay vì gọi `Metrics.evaluate` tĩnh. Hàm tĩnh khai phá lại `F_σ(D)` mỗi
  lần gọi; đo được trên MSNBC là 74,7 giây một lượt so với 1,83 giây khi có bộ đệm, tức chênh 41
  lần. `Metrics.evaluate` chỉ dành cho một lần đánh giá lẻ hoặc cho test.
