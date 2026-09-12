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

- JDK 17 trở lên (đã thử với Microsoft OpenJDK 17.0.19).
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
| Độ đo HF, Lost, Ghost, DistR, SupDist | `metrics.Metrics` | §2 |
| `F_σ(D)`, `B_ε`, `𝒮⁺` | `metrics.FrequentPatterns` | §9.2 bước 3 |
| ρ_rep, β, \|R\|\* | `metrics.DataProfile` | §8.4 |
| Baseline tầng (a) | `baseline.RandomMasking` (HH/HR/RH/RR), `baseline.OccurrenceCountHeuristic` | §11.3; chạy được với \|𝒮\| bất kỳ |
| Kiểm B_ε rỗng báo lỗi | `hide.CutHide.requireNonEmptyBand` | Chốt ở `protocol.yaml` dòng 2 |
| Baseline tầng (b): HHA | `baseline.Hha` | [R20]; hai cách tính δ cho SC6 |
| Baseline tầng (b): DBSH | `baseline.Dbsh` | [R3]; Cost theo ký hiệu, Exclude, Update-paths |
| Baseline tầng (b): SBSH | `baseline.Sbsh` | [R3]; **cài lại từ mô tả, phải đối chiếu [R3] trước khi dùng số** |
| Baseline tầng (c): bộ giải chính xác | `baseline.IlpExact` | Nhánh-cận, thực thể nhỏ; tuỳ chọn cực tiểu mẫu mất |
| Tải bộ dữ liệu kèm tổng kiểm | `tools/fetch_datasets.py` | §11.2 |
| SC1, SC2, SC3 | `exp.Sc1…`, `exp.Sc2…`, `exp.Sc3…` | Chạy được |

### Chưa cài (khung có sẵn, đặc tả nằm trong Javadoc của từng lớp)

| Việc | Nơi | Chặn kịch bản nào |
|---|---|---|
| Đối chiếu SBSH với Thuật toán 2 và 3 của [R3] | `baseline.Sbsh` | SC3, SC7, SC8 |
| Bản gốc của HHA, DBSH, SBSH trên bài toán cũ (độ hỗ trợ theo số chuỗi) | `baseline.Hha`, `Dbsh` (cờ `originalProblem`), `Sbsh` | SC8 |
| Quan hệ nới lỏng tuyến tính và làm tròn có bảo đảm ℓ_max | `baseline.IlpExact` | SC3, danh sách đóng góp |
| Giải thuật di truyền, bản thuần và bản lai khởi tạo từ lát cắt | `baseline.GaHybrid` | SC3 (tầng c) |
| CUT-HIDE nhiều mẫu với ngưỡng CSDL | `hide.CutHide.runMultiPattern` | SC3 với \|𝒮\| > 1 |
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
6. **Trọng số nút khi μ(P) ≥ 2.** Một vị trí mang nhiều nút; mã gán trọng số vị trí cho mọi nút
   của vị trí đó. P1 và P2 vốn không áp dụng trong trường hợp này, nên đây là lựa chọn cài đặt
   chứ không phải hệ quả của mệnh đề nào.

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
