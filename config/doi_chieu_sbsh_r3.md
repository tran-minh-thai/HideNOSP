# A25 — Checking SBSH against [R3]

> **Language.** This file is the English translation of the Vietnamese original as of commit `ea39da4`
> (2026-11-02), which stays in the git history. Values, dates, file names and decisions are unchanged;
> decimal commas are written as decimal points.

**Source.** A. Gkoulalas-Divanis, G. Loukides, "Revisiting sequential pattern hiding to enhance
utility", *KDD 2011*, pp. 1316–1324, doi 10.1145/2020408.2020605. Copy in the library:
`ThuVienBaiBao/14_PPDM_AnGiauLuatNhayCam/[2011] Revisiting Sequential Pattern Hiding to Enhance
Utility.pdf`, 9 pages. Read: Algorithms 1 (Cost), 2 (Annotate), 3 (Exclude), 4 (DBSH),
5 (SBSH) and §4.3.

**Status.** The **facts** part of A25 is done: the three questions all have a definite answer
below. The **decision** part — keep the current implementation and declare it "our variant", or
replace it with exactly the authors' procedure — still belongs to the author.

---

## 1. Which positions does the gray label cover?

**Both options in the question are wrong.** The question assumes that the colour label is about
the structure of the cut (whether a position carries a sensitive node, whether it lies on the
cut). Algorithm 2 says nothing at all about that: it colours by **side effect on NONsensitive
patterns**.

Algorithm 2, restated in this project's notation (σ for the paper's ψ, patterns for sequences), `Annotate(p, D, TN, σ)`:

```
1. TN_p  ← the nonsensitive frequent patterns in TN that contain the p-th event of T
2. if (no pattern n_j ∈ TN_p has sup(n_j, D) = σ)
3.     return BLACK
4. else
5.     for each (n_j ∈ TN_p such that sup(n_j, D) = σ)
6.         if (sup(n_j, D) < σ after deleting the p-th event of T)
7.             return RED
8. return GRAY
```

So:

| Colour | Meaning |
|---|---|
| **Black** | No nonsensitive pattern containing this position sits **exactly** at the threshold σ. Deleting it is harmless |
| **Red** | Some pattern sits exactly at the threshold, **and** deleting this position pushes at least one pattern below σ |
| **Gray** | Some pattern sits exactly at the threshold, **but** the local check sees no pattern dropping |

Gray means **not yet certain**, not "irrelevant". The reason for a gray label: the check in step 6
considers one position at a time, while the deletions accumulate. That is why SBSH (Algorithm 5,
step 32) **runs `Annotate` again on that very position just before deleting it**, and deletes only
if it is **still** gray.

Scope of the colouring: step 4 of Algorithm 5 uses a variant `Annotate-T` that colours **every
position corresponding to an event that lies in a sensitive pattern** s ∈ TS. That is, the
colouring domain is exactly the positions carrying a sensitive event; the three colours share that
domain.

### Deviation in `Sbsh.java`

The current `annotate()` colours: BLACK for a vital position of a sensitive pattern, RED for a
vital position of a pattern at the threshold, and GRAY is **the default value of every other
position**. Three deviations:

1. **Our gray lies OUTSIDE their colouring domain.** We read gray as "does not carry a sensitive
   node"; they read gray as "does carry one, but the side effect is not yet certain". This is
   exactly what the TODO suspected.
2. **The gray branch of Algorithm 5 is entirely absent in ours.** Our loop chooses only from
   `index.vitalPositions(p)`, that is, only vital positions, so no gray position is ever chosen.
   Their steps 32–37 (run Annotate again, then delete) have no counterpart.
3. **Red is nearly right.** We take "a vital position of a pattern at the threshold". Under
   `sup_NO`, vital means that masking it lowers the support by at least 1, that is from σ to
   σ−1 < σ — exactly the condition of step 6. This part matches.

---

## 2. Is the radius r counted in edges or in positions?

**In EDGES of the matching graph.** §4.3 verbatim:

> "we restrict ourselves to considering positions that are reachable from p by following at most
> r edges in G_{S,T}, where (radius) r ∈ [1, max_{j=1..|S|} |s_j|] is a parameter that controls
> the part of G_{S,T} to be searched."

Example 3 of the paper confirms it: for position 3, r = 1 gives {4, 5, 6}; r = 2 gives {0, 1}
"because the nodes carrying those labels **are reachable from the node labelled 3 by following two
edges**".

An edge of the matching graph joins two consecutive levels, so r edges = r levels. **The current
reading of `Sbsh.java` ("in Nettree levels away from the red node") is RIGHT.** This is the only
one of the three points where our implementation matches the paper.

### But the domain of r deviates

The paper locks r ∈ [1, **the length of the longest sensitive pattern**]. `Sc3MainComparison` calls
`new Sbsh(problem.maxPatternLength())` for `sbsh-rmax`, that is, it takes **ℓ_max**. The two
coincide only when the sensitive set contains a pattern of length exactly ℓ_max. With
`--sensitiveSize=1` and patterns drawn by percentile, most instances do **not** satisfy that. It
must be changed to the `max` length over the sensitive set, otherwise the "largest r" of SC7 is not
the one the paper defines.

---

## 3. How do C-PA and C-SA differ from an ordinary Replace?

`Replace` is **not** a separate procedure: it is **the minimum of two directed searches**.

| Function | What it does |
|---|---|
| `C-PA(p, G, r)` | Finds a set of positions that (i) are all **black**, (ii) block **every** complete path through node p, (iii) lie at a **level BEFORE** the level of p, (iv) are reachable from p in ≤ r edges. Starts from 1 edge, increases by one each round, **stops at the first time a set is found** or when r is exceeded |
| `C-SA(p, G, r)` | Exactly the same, but at a **level AFTER** |
| `Replace(p, G, r)` | Calls both, returns the **SMALLER set** of the two |

### Deviation in `Sbsh.java`

Our `replace()` scans **every** level within the radius (`|level − redLevel| ≤ radius`) on **both
sides in the same round**, then takes the globally smallest set. The differences:

- They **stop at the first find**, by increasing distance, **separately for each side**. We
  **optimise over the whole radius**. So our set is smaller than or equal to theirs, and we cannot
  reproduce their behaviour of "near, so take it at once even if large".
- Because each side stops independently, their `Replace` compares the two **nearest sets, one per
  side**, not the two smallest sets, one per side.

Consequence for SC7: the "smallest r" and "largest r" variants measure something different in ours
than in the paper. In ours, increasing r only widens the optimisation space, so the set only gets
smaller; in theirs, increasing r can change the chosen set altogether.

---

## 4. One more deviation, outside the three questions: `Exclude-SBSH` is absent

Algorithm 5 step 1 calls `Exclude-SBSH`, a variant of Algorithm 3 that **removes from D_S the
sequence containing the LARGEST NUMBER of sensitive patterns**, breaking ties arbitrarily when
several sequences have the same cost. The paper says plainly that this choice "can considerably
reduce the number of side effects, as our experiments confirm".

`Sbsh.run()` has no such step: it walks the sequences in plain index order. `Dbsh` **does** have
Exclude (sort by cost, then sanitize the cheap ones first, equivalent to the sort-descending-then-
remove approach of Algorithm 3).

Because the quantity that H2 tests is **Lost**, which is exactly the side effect, the missing
`Exclude-SBSH` is likely the **heaviest** of the four deviations — it weakens SBSH exactly on the
side where SBSH was made to be strong.

---

## 5. Summary and what must be decided

| # | Point | Current implementation |
|---|---|---|
| 1 | Gray label | **Wrong**, and the gray branch of Algorithm 5 is absent |
| 2 | Radius counted in edges | **Right** |
| 2b | Domain of r = length of the longest sensitive pattern | **Wrong**, takes ℓ_max |
| 3 | C-PA / C-SA / Replace | **Different**: we optimise over the whole radius, they stop at the first find, separately for each side |
| 4 | `Exclude-SBSH` | **Absent** |

Exactly 1 out of 5. The background part of `preregistration.md` §2 says: if SBSH cannot be
checked, it is dropped from the table and the Holm family keeps two. The check is done, but the
result is that **the current implementation is not the SBSH of [R3]**.

**Three options, for the author to choose:**

1. **Fix it to match the paper.** The most work, but it keeps the four-member Holm family and keeps
   the sentence "precisely their competitor" in §12a. Four tasks: rewrite `annotate` following
   Algorithm 2, add the gray branch, split `replace` into stop-at-first-find C-PA and C-SA, add
   `Exclude-SBSH`. Also fix the domain of r.
2. **Keep it as is and declare it a variant.** Cheap, but then the two SBSH rows in the table are
   no longer the baseline of [R3] but two variants of ours, so they **must be withdrawn from the
   Holm family** — the family keeps two (HHA, DBSH), and `preregistration.md` §2 must be changed
   **before** running SC3.
3. **Fix the cheap part, declare the rest.** Add `Exclude-SBSH` and fix the domain of r (two small
   tasks, hitting the heaviest deviation), declare the gray label and Replace as variants. It still
   has to be decided whether that can still be called SBSH.

Whatever is chosen, it must be done **before** running SC3 for the numbers in the paper, and if
`preregistration.md` is changed, that is a separate commit stating the reason, exactly as the
opening paragraph of that file says.

---

## 6. Update 2026-09-25 — fixed following option 1 (fix it to match the paper)

The author chose option 1. `baseline/Sbsh.java` was rewritten following Algorithm 5, together with
Algorithm 1 (Cost, used for Candidates-SBSH and for the order of Exclude-SBSH), Algorithm 2
(Annotate) and C-PA, C-SA, Replace of section 4.3. The table in section 5 above describes the
implementation **before** this date; all five points now follow the paper:

| # | Point | Implementation from 2026-09-25 |
|---|---|---|
| 1 | Gray label | Following Algorithm 2 (gray = some nonsensitive pattern sits exactly at the threshold but deleting this position does not make it drop); the gray branch of Algorithm 5 (colour again, then delete) is in place |
| 2 | Radius counted in edges | Unchanged, right |
| 2b | Domain of r | `sbsh-rmax` takes the length of the longest sensitive pattern of the set, not ℓ_max |
| 3 | C-PA / C-SA / Replace | Each side increases k from 1, stops at the first k that finds a set; Replace takes the smaller set |
| 4 | Exclude-SBSH | In place (see the new finding below) |

**New finding when rereading the full text: the paper contradicts itself on Exclude-SBSH.** Section
5.2.2 says verbatim: "a variant of Algorithm 3 that removes the transaction that supports the largest
number of sensitive sequences from D_S, breaking ties arbitrarily if many transactions have the
same sanitization cost". Section 5.2.3 explains the choice in the example by **non**sensitive
patterns: "Although both T1 and T2 have a sanitization cost of 1 [...] T2 supports fewer nonsensitive
frequent sequences. [...] SBSH selects this transaction instead of T1." Computed by hand on Fig. 1:

- reading by the number of **sensitive** patterns, T7 (supporting 2 sensitive patterns) is sanitized
  before T8, so T7 loses d and f and becomes `* * h h h`, contrary to Fig. 1(f), which prints `d f * * *`;
- reading by the number of **nonsensitive** patterns (descending), then Cost (descending, as in
  Algorithm 3), then sequence index, gives exactly Fig. 1(f) and exactly the side effects of Fig. 4(b).

The implementation follows the second reading. The last order (sequence index) the paper leaves
"arbitrary"; choosing descending reproduces Fig. 1(f) exactly, choosing ascending makes T2 and T3
swap roles — both are consistent with the paper.

**What the paper leaves open, and the choices made** (also recorded in the Javadoc of the class):

1. "P[i] = ∅" is read as the end of array P; a position with no node left on a complete path is skipped.
2. Algorithm 5 only handles a gray position that is **still** gray when coloured again. If colouring
   again gives black or red, the label is updated and the next round handles it by that branch
   (otherwise the loop stands still forever).
3. C-PA/C-SA at distance k take the **whole level** k edges away, containing only nodes still on a
   complete path, and accept when every position in the level is labelled black. A tie between C-PA
   and C-SA takes C-PA.
4. Ties in the greedy set cover: the symbol that appears earlier in the sequence — reproduces Example 1
   exactly.

**Evidence** (`src/test/java/hidenosp/baseline/SbshPaperExampleTest.java`), run in
`SEQUENCE_COUNT` mode, that is the problem of the paper itself, with no maximum gap:

- Fig. 1(f): exactly the masked positions {T2: 0; T6: 0; T7: 2, 3, 4; T8: 1}, distance 6 (Fig. 4(b));
- Fig. 4(b): exactly the three side effects ⟨a,c⟩, ⟨a,e⟩, ⟨d,g⟩;
- Example 3 / Fig. 2(e): Replace(3, G, 1) = {4, 5, 6}, Replace(3, G, 2) = {0, 1};
- two hand-built cases (not in the paper) for the gray branch and for "compute the support as if the
  marked positions had already been deleted" under sup_NO.

Fault injection: 10 faults (the sort key of Exclude-SBSH, the tie order, dropping the black condition
of Replace, taking the larger set, Annotate reading the initial support, dropping the colouring again,
counting by number of sequences in sup_NO mode, never replacing a red position, colouring again
skipping the marked positions, C-SA searching in the wrong direction) — 10/10 caught. The paper's
example has no gray position, so the two faults on the gray branch were caught only after the two
hand-built cases were added.

**Transfer to sup_NO** (the version SC3 uses): only the support function is replaced, at the three
places where the paper reads support — the retention budget of Exclude-SBSH (total sup_NO of the
kept sequences ≤ σ − 1 for each sensitive pattern), the threshold comparison of Annotate, and the
loop condition. The modification is masking instead of deletion, as for every method.

**The Holm family keeps its four members** (HHA, DBSH, SBSH-r1, SBSH-rmax) as in `preregistration.md`
§2; the locked version does not need to change.

**Remaining, not part of SBSH but exposed by this work:** `Dbsh` computes Cost as the sum, over
patterns, of the number of positions on the paths of the cheapest symbol — **not** Algorithm 1
(greedy set cover, weight = number of occurrences of the symbol in the sequence). Two baselines of
the same paper are using two different Cost functions; DBSH must be checked against Algorithms 3–4
before running SC3 for the numbers.

---

## 7. Update 2026-09-25 (pass 2) — DBSH fixed to match the paper; correction to section 6 on Exclude

**Correction to section 6.** Section 6 records the transfer of Exclude-SBSH as "total sup_NO of the
**kept** sequences ≤ σ − 1". That is a wrong reading of Algorithm 3. Section 5.1 verbatim: "moves
the transaction to D′ without sanitizing it only if every sensitive sequence supported by T has been
**encountered** fewer than (ψ − 1) times" — the counter M(s) increases with **every** transaction
visited, including sanitized ones. Computed by hand on Fig. 1(e): only this reading gives exactly
DBSH sanitizing T8 (whereas once T5 has been sanitized, ⟨d,f,g⟩ is already below the threshold).
Fig. 1(f) of SBSH gives the same result under both readings, so the SBSH test could not catch this
fault; the DBSH test does. The two algorithms now share **one** Exclude implementation
(`baseline/Exclude.java`), so both follow the right reading.

**DBSH** (`baseline/Dbsh.java`) was rewritten following Algorithm 4, with Algorithm 1 (Cost, greedy
set cover over symbols, weight = number of occurrences of the symbol in the sequence) and Algorithm
3 (Exclude). The previous version deviated in three places:

| Point | Previous version | Version from 2026-09-25 |
|---|---|---|
| Cost | Sum over patterns of the number of positions on the paths of the cheapest symbol | Algorithm 1: greedy set cover, weight divided by the number of patterns the symbol covers |
| Exclude | Sanitize the cheap sequences first until the reduction is sufficient | Algorithm 3: visit by descending Cost, keep a sequence when every pattern it supports has been encountered fewer than ψ − 1 times (counting the sanitized sequences too) |
| Sanitizing one sequence | Remove sup_NO units one by one down to exactly the level needed | Algorithm 4: for each subset S_j, remove the positions of the chosen symbol until the sequence no longer supports any pattern of S_j — that is, sanitize the chosen sequence **completely**, as in the paper |

**What the paper leaves open, and the choices made.** A tie in Exclude ("arbitrary") takes the larger
sequence index first — reproduces Fig. 1(e) exactly. A position of the chosen symbol that lies on no
complete path is skipped: Candidates works on the matching graph, which keeps only nodes on complete
paths (Definition 4.1), and this is also the reading most favourable to DBSH. Order within P[j]:
increasing position.

**Consequence of the transfer that must be declared in the paper.** Under sup_NO, DBSH still
sanitizes each chosen sequence completely, so it may use more masks than needed to drop below σ.
That is the behaviour of DBSH according to the paper, not a defect of the transfer; the version
before this date had "adjusted" DBSH for sup_NO in a way the authors never did.

**Evidence** (`src/test/java/hidenosp/baseline/DbshPaperExampleTest.java`): reproduces exactly Fig.
1(e) (T1: 0; T5: 0, 1; T6: 0; T8: 1), distance 5 and the five side effects ⟨a,c⟩, ⟨a,e⟩, ⟨d,f⟩,
⟨d,g⟩, ⟨f,g⟩ of Fig. 4(b), and the Cost of Example 1 (equal to 2). Plus two hand-built cases: a
position outside the complete paths is skipped; Cost divides the weight by the number of patterns
covered. 7 faults injected (Exclude counting only the kept sequences, ascending Cost, ascending ties,
Cost not divided, positions outside the paths not skipped, ties by last occurrence, stopping when one
pattern of S_j is gone): 7/7 caught. The 10 SBSH fault injections rerun after splitting off the
shared part: still 10/10 caught.

**Remaining:** HHA of [R20] has not yet been checked line by line against the original paper in the
same way.
