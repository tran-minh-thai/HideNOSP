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

lệnh trên in danh sách kịch bản và tuỳ chọn của từng cái. Ví dụ:

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
`probe/sideeffect.py` cần `numpy`. Các script này vét cạn trên tập con nên chạy lâu; chúng là dò tìm,
không nằm trong đường chạy của bài.

---

## 3. Ba thư mục kết quả và luật của chúng (§11.8)

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

## 4. Trạng thái cài đặt

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
| Baseline tầng (a) | `baseline.RandomMasking` (HH/HR/RH/RR), `baseline.OccurrenceCountHeuristic` | §11.3 |
| SC1, SC2, SC3 | `exp.Sc1…`, `exp.Sc2…`, `exp.Sc3…` | Chạy được |

### Chưa cài (khung có sẵn, đặc tả nằm trong Javadoc của từng lớp)

| Việc | Nơi | Chặn kịch bản nào |
|---|---|---|
| HHA của [R20], cả bản quy hoạch động gốc | `baseline.Hha` | SC3 (tầng b), SC6, SC8 |
| DBSH của [R3] | `baseline.Dbsh` | SC3, SC8 |
| SBSH của [R3], tham số bán kính | `baseline.Sbsh` | SC3, SC7, SC8 |
| Quy hoạch nguyên chính xác và quan hệ nới lỏng tuyến tính | `baseline.IlpExact` | SC3 (tầng c) |
| Giải thuật di truyền, bản thuần và bản lai khởi tạo từ lát cắt | `baseline.GaHybrid` | SC3 (tầng c) |
| CUT-HIDE nhiều mẫu với ngưỡng CSDL | `hide.CutHide.runMultiPattern` | SC3 với \|𝒮\| > 1 |
| SC4, SC5, SC6, SC7, SC8 | `exp.Sc4…` đến `exp.Sc8…` | chính nó |
| Đối chiếu với NETGAP của [R1] | `exp.Sc1ImplementationCheck` | SC1 (phần còn thiếu) |
| Đo bộ nhớ thường trú của tiến trình | `util.MemoryProbe` | SC6 |

Tìm nhanh: `grep -rn "TODO(code)" src/`.

---

## 5. Bảng giao thức 9 dòng

`config/protocol.yaml` là bản điền. Quy tắc 2.6.2 đòi điền **ngay ở bản nháp đầu**, không đợi
tới lúc nộp. Những ô mã sinh ra được (phiên bản JDK, cỡ heap, hạt giống) đã được in sẵn vào
đầu mỗi tệp kết quả, chép từ đó sang.

`config/preregistration.md` chốt ngưỡng hiệu ứng δ_L và phép kiểm **trước** khi chạy SC3
(quy tắc 2.4.7). Chưa chốt thì chưa chạy SC3 để lấy kết quả.

---

## 6. Những quyết định cài đặt phải khai trong bài

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

## 7. Bản đồ từ §9.2 sang mã

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

## 8. Giới hạn kỹ thuật đã biết

- `Dinic.dfs` đệ quy. Với chuỗi rất dài và khoảng cách rộng, ngăn xếp có thể tràn; cần chuyển
  sang vòng lặp trước khi chạy SC6 ở quy mô lớn.
- `PathCounts` bão hoà ở `Long.MAX_VALUE / 4`. Giá trị bão hoà vẫn dùng làm khoá so sánh được
  (đó là tất cả những gì heuristic của [R20] cần) nhưng **không được in ra như số đếm chính xác**;
  `PathCounts.isExact()` cho biết đã bão hoà hay chưa.
- `Metrics.evaluate` khai phá `F_σ` hai lần cho mỗi thực thể. Với bộ dữ liệu lớn phải thêm bộ nhớ
  đệm cho `F_σ(D)`, vốn không đổi trong suốt một lần quét tập nhạy cảm.
