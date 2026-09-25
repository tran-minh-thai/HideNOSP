# SBSH on real data, and the pool of sensitive patterns (probe, 2026-09-25)

Kind: **probe** (development machine, macOS arm64, OpenJDK 26.0.1, bytecode target 25). Part 1
reports feasibility only; its run times are development-machine numbers and never go into the
paper. Part 2 measures a property of the data, which does not depend on the machine.

## 1. Feasibility of the rewritten SBSH (commit 55e06cd)

Run from a clean clone of commit `55e06cd`, datasets as in datasets/provenance.tsv:

    python tools/run.py --no-build --probe --jvm=-Xmx6g SC3 --dataset=datasets/dna_mt745584.txt --format=SPMF --sigma=2500 --gmin=0 --gmax=3 --maxPatternLength=4 --epsilon=0.5 --sensitiveSets=2 --sensitiveSize=1 --seed=2026 --methods=dbsh,sbsh-r1,sbsh-rmax
    (same with --sensitiveSize=5)
    python tools/run.py --no-build --probe --jvm=-Xmx6g SC3 --dataset=datasets/msnbc.txt --format=SPMF --sigma=2000 --gmin=0 --gmax=3 --maxPatternLength=4 --epsilon=0.5 --sensitiveSets=1 --sensitiveSize=5 --seed=2026 --methods=dbsh,sbsh-r1,sbsh-rmax

Files: `SC3_20260925-115839-657`, `-115841-919` (DNA, |S| = 1 and 5), `-115859-500` (MSNBC, |S| = 5).
Every row has HF = 0 for DBSH, SBSH-r1 and SBSH-rmax (the class also checks this itself and
throws otherwise). Wall time of each whole run: about 2 s on DNA, 19 s on MSNBC. The Lost columns
were not read for this note.

## 2. How many distinct sensitive sets a cell can have

SC3 draws each sensitive set by shuffling the pool of candidates (frequent, distinct items, length
2 to 4) and taking the first |S|, independently for every set, so two sets can coincide. The
replay below uses that exact procedure with seed 2026 and 20 sets per cell (the protocol's value).

| configuration | F_sigma | pool | distinct sets of 20, for size 1 / 5 / 10 / 20 |
|---|---:|---:|---|
| dna [0,1] sigma=2500 | 16 | 9 | 8 / 19 / 1 / 1 |
| dna [0,3] sigma=2500 | 67 | 23 | 14 / 20 / 20 / 20 |
| dna [0,3] sigma=4000 | 14 | 4 | 4 / 1 / 1 / 1 |
| msnbc [0,3] sigma=2000 | 556 | 178 | 19 / 20 / 20 / 20 |
| msnbc [0,1] sigma=5000 | 107 | 28 | 14 / 20 / 20 / 20 |

Two consequences. First, cells whose pool is smaller than the requested size give the same set
20 times (for instance dna [0,3] sigma=4000 has 4 candidates, so every set of size 5, 10 or 20 is
the whole pool); counted as 20 units of repetition, they would inflate the Wilcoxon sample.
Second, the procedure does not implement the locked protocol at all: config/protocol.yaml row 1
fixes five sup_NO percentiles (p10, p30, p50, p70, p90) with four sets each.

The pool is small on DNA because its alphabet has four symbols: with distinct items there are at
most 4*3 + 4*3*2 + 4*3*2*1 = 60 patterns of length 2 to 4, and only the frequent ones qualify.

Replay program (compiled against target/classes of commit 55e06cd):

    java -cp target/classes:. DupCheck datasets/<file> <gmax> <sigma>

```java
import hidenosp.core.*; import hidenosp.io.*; import hidenosp.metrics.*; import java.util.*; import java.nio.file.*;
public class DupCheck {
  public static void main(String[] a) throws Exception {
    SequenceDatabase db = DatasetReader.read(Paths.get(a[0]), DatasetReader.Format.SPMF).database();
    GapConstraint gap = new GapConstraint(0, Integer.parseInt(a[1])); int sigma = Integer.parseInt(a[2]);
    Map<Pattern, Long> frequent = FrequentPatterns.mine(db, gap, 4, sigma);
    List<Pattern> pool = new ArrayList<>();
    for (Map.Entry<Pattern, Long> e : frequent.entrySet()) if (e.getKey().length() >= 2 && e.getKey().hasDistinctItems()) pool.add(e.getKey());
    for (int size : new int[]{1, 5, 10, 20}) {
      Random random = new Random(2026); List<List<Pattern>> out = new ArrayList<>();
      for (int i = 0; i < 20; i++) { List<Pattern> s = new ArrayList<>(pool); Collections.shuffle(s, random); out.add(new ArrayList<>(s.subList(0, Math.min(size, s.size())))); }
      Set<Set<Pattern>> distinct = new HashSet<>(); for (List<Pattern> s : out) distinct.add(new HashSet<>(s));
      System.out.printf("%s gap[0,%s] sigma=%d |F|=%d pool=%d size=%d: 20 sets drawn, %d distinct%n", Paths.get(a[0]).getFileName(), a[1], sigma, frequent.size(), pool.size(), size, distinct.size());
    }
  }
}
```
