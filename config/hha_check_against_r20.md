# Checking HHA against [R20] and [R2] — 2026-09-25

> **Language.** This file is the English translation of the Vietnamese original as of commit `ea39da4`
> (2026-11-02), which stays in the git history. Values, dates and decisions are unchanged, file names
> are those after the renaming of 2026-11-02; decimal commas are written as decimal points. This file
> was named `config/doi_chieu_hha_r20.md` until 2026-11-02.

**Sources whose algorithm sections were read in full.**

- [R20] O. Abul, M. Atzori, F. Bonchi, F. Giannotti, "Hiding sequences", ICDE Workshops 2007,
  section 4 and Algorithm 1. Copy in the library: `ThuVienBaiBao/14_PPDM_AnGiauLuatNhayCam/[2007] Hiding
  Sequences.pdf`, 10 pages.
- [R2] O. Abul, F. Bonchi, F. Giannotti, "Hiding sequential and spatiotemporal patterns", IEEE TKDE
  2010, section 2.2 (Algorithm 1, unchanged from [R20]), section 2.3 (constraints, occurrence counting,
  multiple thresholds — Algorithm 2). Copy in the library: `[2010] Hiding Sequential and Spatiotemporal
  Patterns.pdf`, 15 pages.
- [R3] Gkoulalas-Divanis and Loukides, KDD 2011: call this algorithm HHA, print its output in
  Figure 1(d), and its distance and side effects in Figure 4(b).

## 1. The algorithm, verbatim

- The problem: "∀S_i ∈ S_h, sup_D′(S_i) ≤ ψ" — support is the number of sequences, ψ is the disclosure threshold.
- Local heuristic: "choose the marking position that is involved in most matches. This
  operation is iterated until M^T_Sh = ∅." δ(T[i]) is the number of matches passing through position i, over
  the matching set of **all** sensitive patterns (every embedding, overlapping ones included).
- Global heuristic: "sort the sequences in D in ascending order of matching set size, and remove
  all matchings in top |D| − ψ input sequences" — i.e. keep unchanged the ψ sequences with the **largest**
  matching sets, and sanitise **completely** every remaining sequence. The reason the authors give: "if the
  matching set is small, then we can sanitize the respective sequence with less distortion."
- [R2] keeps Algorithm 1 unchanged. Algorithm 2 of [R2] is for **multiple different disclosure thresholds**
  (choose the sequence with the most matches, interleaved with sanitisation) — not the single-threshold case
  of this paper.

## 2. Where the implementation before this date deviated

| Point | Previous version | Original paper |
|---|---|---|
| Global order | Matching set **descending**, sanitise large sequences first | **Ascending**, keep the ψ largest sequences |
| Sanitising one sequence | Stop once the sup_NO of the whole database has dropped enough | Continue until the sequence has **no match left** |
| δ | Counts only the patterns that still need lowering | All sensitive patterns |

The descending order was probably taken from Algorithm 2 of [R2] (multiple thresholds), not from the HHA algorithm.

## 3. What the paper leaves open, and the choices made

- Ties in δ: the **leftmost** position. Ties in matching-set size: the smaller sequence index comes first,
  so the larger index is kept. With these two rules, the implementation reproduces Figure 1(d) of [R3] exactly.
- δ is computed by masking instead of deletion (see G2 of the research design: the deletion of Theorem 2 is
  wrong when there is a maximum gap); `Hha.Delta.ORIGINAL_DP` keeps the recounting of Theorem 2 so that SC6
  can compare times.

## 4. The transfer to sup_NO

The ψ of the paper = σ − 1. `SupportCount.SEQUENCE_COUNT` keeps exactly the ψ largest sequences. With
`SupportCount.NON_OVERLAPPING`, a kept sequence costs its largest sup_NO over the sensitive
patterns (instead of 1), and the kept part is the longest run of the largest sequences whose total cost is
≤ σ − 1. With one sensitive pattern this is exactly the paper's budget expressed in units of sup_NO; with support
counted as a number of sequences it coincides exactly with the paper's rule. The selected sequences are still
sanitised **completely**, as in the paper, so HHA may use more masks than needed to drop below σ — that is the
behaviour of HHA.

HIDE-SEQ: every sequence with sup_NO ≥ r is sanitised until every pattern is < r. This is the "occurrence
counts" extension of [R2] section 2.3, with sup_NO instead of the matching-set size (see section 6).

## 5. Evidence

`src/test/java/hidenosp/baseline/HhaPaperExampleTest.java`:

- Example 2 of [R20]/[R2]: S = abc, T = aabccbae gives δ(T[1]) = 2, δ(T[2]) = 2, δ(T[3]) = 4, and one
  marking at T[3] sanitises the sequence — correct under both ways of computing δ.
- Figure 1(d) of [R3] (ψ = 3 under the "< ψ" convention of [R3], i.e. disclosure threshold 2 of [R20]): exactly
  the masked positions T1: 0; T2: 0; T3: 0; T6: 0; T8: 1; T9: 0, 1; distance 7; exactly the six side effects
  ⟨a⟩, ⟨a,b⟩, ⟨a,c⟩, ⟨a,e⟩, ⟨d,f⟩, ⟨d,g⟩ of Figure 4(b).
- A self-built case for the transfer to sup_NO (keep the largest sequences within the budget σ − 1).

6 faults injected (descending order as in the previous version, δ ties broken to the right, keeping cost summed
over the patterns, sorting by sup_NO instead of matching-set size, δ from one pattern only, the recounting of
Theorem 2 without masking the position): caught 6/6.

## 6. A finding outside the scope of the check — it touches the novelty of HIDE-SEQ

[R2] section 2.3, verbatim: "Extending our framework to deal with occurrence counts is
straightforward. Given an upper bound ϱ of the occurrence count, the iteration in Sanitize(T, Ph)
can be terminated whenever |M^T_Ph| ≤ ϱ." That is, [R2] already stated, in one paragraph, a **per-sequence
bound on the number of occurrences** — counted as the matching-set size (every embedding, overlapping, growing
exponentially), not as the number of non-overlapping occurrences, with no analysis and no experiments. The sentence
"HIDE-SEQ with r > 1 cannot be stated with sup_seq" in G1 of the research design still holds, but the paper must
quote this passage of [R2] and state the difference clearly: the counting unit (non-overlapping against every
embedding), and the exact results of the paper. A warning has been recorded at G1 of the research design.
