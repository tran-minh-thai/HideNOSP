# What is still to be decided

> **Language.** This file is the English translation of the Vietnamese original as of commit `ea39da4`
> (2026-11-02), which stays in the git history. Values, dates and decisions are unchanged, file names
> are those after the renaming of 2026-11-02; decimal commas are written as decimal points. This file
> was named `config/quyet_dinh_can_chot.md` until 2026-11-02.

Merged automatically from a review of 10 sources of the project: `config/preregistration.md`, `config/protocol.yaml`,
`RUNBOOK.md`, the research design from four angles (gates and risks, theory, experimental design,
gaps), the results of phase 1, the source code (`TODO(code)` and `UnsupportedOperationException`),
and the manuscript. Every item was checked again by an independent review: open the file cited, check the quotation,
and grep both the documents and the source code for whether it has already been decided elsewhere. A final critical pass
looked for kinds of decision that nobody had touched.

89 items passed the check, plus 12 items added by the critical pass, merged into the 29 themes below.

**How to read it.** Group A blocks SC3: until they are locked, every SC3 run must carry `--probe` and its numbers
cannot be used in the paper. Group B blocks submission, not the runs.

---

## A. Blocks SC3 — to be locked before the main comparison runs

### A1. Practical-effect threshold δ_L of H2

Raised by: `design-experiment`, `design-gate`, `paper`, `phase1-findings`, `preregistration`, `runbook`

**Value and unit of the practical threshold δ_L**

Fill δ_L into preregistration.md line 22 and decide the unit. One must choose between: (i) an absolute constant counted in lost non-sensitive patterns — simple but meaningless at the small end and infeasible at the large end, because the scale of the instances spans five orders of magnitude (min_masks from 1 to 87,107 in the SC2 table); (ii) a share of |F_σ(D)| before sanitisation, i.e. δ_L is the percentage of frequent patterns lost; (iii) a share of the baseline's own Lost, i.e. a relative difference. Note the constraint: the unit of replication of H2 is (dataset, σ, sensitive set), so δ_L must be a threshold applied to the median of the difference across instances of different sizes — if (i) is chosen, it must be stated clearly why those instances can be mixed in one Wilcoxon test. SC3 runs after it is filled in; the frequent_before/frequent_after columns of SC3 give the right denominator if (ii) is chosen. The author decides.

**Practical-effect threshold δ_L**

Fill THREE numbers into preregistration.md, not one: (a) delta_L at line 22, unit = number of lost non-sensitive patterns (absolute integer or share of |F_sigma(D) \ S+|? the unit must be stated before choosing the number); (b) the equivalence margin for Lost at line 40; (c) the equivalence margin for SupDist at line 41 (an entirely different unit -- SupDist is a dimensionless ratio). Possible grounds for choosing delta_L: either take it from the range of Lost in the exploratory runs in results-probe/SC3_*.csv, or anchor it to M2 of [R20]/[R3] (share of frequent patterns lost) so that it is comparable. WHO DECIDES: the author. URGENT: before SC3 -- rule 2.4.7 forbids running SC3 to take results while it is not locked; H2 cannot be tested and no 'no difference' conclusion can be stated.

**Lock the practical-effect threshold δ_L of H2**

Choose δ_L in one of two forms, and record the grounds: (a) an absolute number — δ_L = k lost non-sensitive patterns (k an integer, e.g. 1, 2 or 5), or (b) a share of |F_σ(D) \ 𝒮⁺|, i.e. the frequent_before column of SC3 (e.g. 1% or 2%), then converted to an integer for each instance. Note that form (b) makes δ_L vary with the instance, so Wilcoxon must run on the differences after subtracting each instance's own δ_L. Do not use Σ|S| or |R|* as the denominator — Σ|S| is the denominator of DistR, |R|* is the bound on the number of masks. The grounds should come from the measured scale: the SC3 probe (results-probe/SC3_20260912-102131) has frequent_before = 84 and the baseline's Lost in the range 1–5, so δ_L = 1 is almost certainly too lax while δ_L = 10 would make H2 impossible to reject.

**The sentence on the experimental results in the abstract**

Which quantity, and on which scenario, the only result sentence in the abstract uses: (i) the Lost difference between CUT-HIDE and the strongest baseline at the same |R| and HF=0, unit = number of lost non-sensitive patterns, taken from SC3 on the main group (dna_mt745584, msnbc); (ii) the ratio |R|/|R|* of the tier (b) baselines when the objective is switched to sup_NO, unit = dimensionless ratio, from SC3; or (iii) side-effects (%) and distance (%) against HHA/DBSH/SBSH on the old problem, from SC8. Note that (i) can only be written after δ_L in preregistration.md is filled in and SC3 runs without the --probe flag; (iii) is currently blocked because SC8 is not implemented and trucks has no source.

**Equivalence margins for Lost and SupDist**

Fill in the two equivalence margins in preregistration.md §2 and decide their form. Lost: choose an absolute margin counted in lost frequent patterns, or a margin proportional to |F_σ(D)|. SupDist: whether SupDist is already a normalised quantity must be checked in Metrics before deciding — if it is a raw sum of sup_NO differences, an absolute margin is useless because sup_NO spans from 100 (sign) to 89,106 (msnbc), and a margin proportional to the configuration's own sup_NO must be used. These two margins must be consistent with δ_L in item [6]: δ_L may not be proportional while the equivalence margin is absolute. Lock before looking at any SC3 number. The author decides.

**Lock δ_L, the practical-effect threshold of H2**

Choose δ_L as an absolute integer, how many lost non-sensitive patterns (e.g. δ_L = 1, 2 or 5), OR switch to a relative form (a share of |F_σ(D) \ 𝒮⁺|, because |F_σ| of dna, msnbc, bms1 differ by tens of times, so an absolute threshold is not comparable across datasets)? Grounds already available to anchor the value: the exploratory numbers in §7 line 45 (mean Lost 6.02 for HHA against 1.95 for the weighted cut on 60 synthetic databases, i.e. a difference of ~4). The author decides, records it in preregistration.md line 22 with one sentence of grounds, then commits before the first SC3 run without --probe.

**Equivalence margin for Lost**

Lock the equivalence margin for Lost: ± how many lost non-sensitive patterns (the same counting unit as Lost in doc §2 line 61), and state its relation to δ_L of H2: set it equal to δ_L (a single number, easy to justify, but then 'more than δ_L' and 'not differing by more than δ_L' share one boundary, so every pair falls into exactly one of the two conclusions), or set it independently and smaller than δ_L (stricter, but its own grounds must be stated). Needed before running SC3 to take numbers into the paper, because §2 forbids replacing it with 'p > 0.05', so without this margin every comparison pair that comes out even in SC3 hangs without a conclusion.

**The content of the preregistration.md file**

Fill in the four remaining null cells of config/preregistration.md, then commit (the git log as evidence of the time): δ_L with the unit = number of lost non-sensitive patterns per instance (dataset, σ, sensitive set), the equivalence margin for Lost, the equivalence margin for SupDist, and the two grouping thresholds ρ_rep of H3 (same as item [8]). The set of tests is not to be decided, it is already locked in §10. The author decides; this is a hard block on SC3.

> Blocks: SC3 cannot be run to take numbers into the paper: hypothesis H2 states "the median difference exceeds δ_L", and without δ_L the one-sided Wilcoxon test has no null hypothesis to test. According to this very file, every SC3 run must carry the --probe flag and its results fall into results-probe/, unusable in the paper.

### A2. Thresholds of ρ_rep and β for the groups: values, the unit they apply to, and the band in between

Raised by: `design-experiment`, `design-gap`, `design-gate`, `design-theory`, `paper`, `phase1-findings`, `phebinh`, `preregistration`, `protocol`, `runbook`  (includes items added by the critical pass)

**Grouping thresholds ρ_rep and β for the degenerate control**

Choose two real numbers in [0,1] to fill into preregistration.md lines 28-29: (a) the rho_rep threshold separating the main group; (b) the beta threshold for the constraint control. The measured spectrum of mean rho_rep: dna 0.786 | msnbc 0.323 | leviathan 0.154 | bible 0.153 | bike 0.052 | fifa 0.008 | sign 0.000 | bms1 0.000. Every cut in the interval (0.052; 0.153) keeps exactly {dna, msnbc} as the main group; a cut in (0.008; 0.052) pulls in leviathan/bible/bike as well. One number must be chosen; no empty band may be left between the main threshold and the degenerate threshold (line 28 currently has two separate cells: if they are set differently, the datasets that fall into the slot between them have no role). WHO DECIDES: the author -- this is a methodological commitment, it may NOT be inferred backwards from the wish that a given dataset go into a given group (rule 2.3.10). URGENT: before SC3 -- every SC3 run must currently carry --probe, and the H3 table and the item 'demonstrating the contribution' [2.2.4] hang until this file is filled in and committed.

**What becomes of SC8 without the TRUCKS dataset**

Decide the shape of SC8 without TRUCKS. Three options: (i) run on the two datasets MSN(=msnbc) and BMS-WebView-1 with exactly the ψ thresholds of [R3], stating plainly in the paper that TRUCKS has no public source; (ii) replace TRUCKS with another dataset and declare the replacement — bike has already been classed "not used" by khao_sat §4 itself because of ρ_rep 0.052 and a mean sequence length of 7.3, so if this direction is chosen another dataset must be found; (iii) drop SC8. Option (iii) is expensive because §12a has made SC8 the fallback leg of the whole paper ("the paper can stand even if the part on the new problem is dropped: the Nettree cut index and CUT-HIDE can improve the old problem"). What blocks all three first: the full texts of [R3] and [R20] must be reopened to copy the exact name of the data version and the value of ψ — a public lookup has been tried and could not reconstruct them. And SC8 is still not implemented (Sc8OriginalProblem.java is a TODO), so this decision is less urgent than the group of thresholds, but it must be done before writing the experimental section.

**H3 on the degenerate group: a test or an identity**

Decide how to state the second half of H3 ("ratio |R|/|R|* near 1 on the degenerate group"). Two options: (i) keep it as a hypothesis that is tested — one must accept that on sign and bms1 the ratio equals 1 by definition (sup_NO ≡ sup_seq on exactly 40/40 rows), so a reviewer can point out that it is circular; (ii) split it in two: the first half (ratio > 1 on the high-ρ_rep group) stays a hypothesis, the second half is demoted to an IMPLEMENTATION CHECK placed beside SC1, reported once and not put in the comparison table, under rule 2.4.2. Along with this, decide immediately whether the second half counts as a test in the Holm correction — if it counts while being an identity, the number of tests is inflated and the adjusted p is wrong. Note: do not lump fifa (ρ_rep 0.005–0.013) and bible/leviathan (0.15) into the identity argument: only sign and bms1 are exactly 0. The author decides, before running SC3, because it changes the number of tests.

**The grid of σ and gap widths that goes into the paper**

Fill in protocol.yaml row_1 per_dataset for each dataset actually used: sigma (both absolute and as a share of the total sup_NO, per the note on lines 22-24), l_max (code default = 3, confirm or change), [g-, g+], and |S| (code default [1,5,10,20], 20 sets per cell). Two questions must still be answered first: (a) the unit of replication of H2 is a triple (dataset, sigma, sensitive set) -- so how many configurations per dataset go into the main table? msnbc currently has 5, dna 3, the other datasets 1-2, and this imbalance skews the weights of the Wilcoxon test; (b) gap: SC2 shows that rho_rep and beta pull in opposite directions ([0,1] gives high beta, [0,10] gives high rho_rep, [0,3] is balanced) -- lock one common gap for every dataset, or report the whole grid? WHO DECIDES: the author, based on the SC2 table already available (no further runs are needed to decide, only to fill the cells). URGENT: before SC3, because the grouping in item [0] is locked per configuration, not per dataset.

**Candidate new experimental datasets**

Three separate tasks, do not merge them. (1) Fill the two grouping thresholds into preregistration.md before running SC3: ρ_rep ≥ ? is the main group and ρ_rep ≤ ? is the degenerate control (measured range: dna 0.786; msnbc 0.323; leviathan 0.154; bible 0.153; bike 0.052; fifa 0.008; sign and bms1 exactly 0.000 — so the threshold must fall into the slot 0.008–0.153 or 0.154–0.323 and may not be inferred backwards from the wish that a given dataset go into a given group), plus the β threshold for the constraint control group. (2) Lock the four synthetic-data generation parameters in protocol.yaml row 4: |Σ|, number of sequences, sequence length, and the target ρ_rep/β range — remembering to report only MEASURED values. (3) For trucks: accept running SC8 on two datasets (MSN, BMS-WebView-1) and state clearly that it could not be obtained, or replace it with bike, or ask the author of [R20] for the data? Note that bike has already been excluded by the measurement table itself (ρ_rep 0.052, mean length 7.3).

**The ρ_rep threshold for a dataset to enter the group demonstrating the contribution**

Set two numbers for config/preregistration.md before looking at any SC3 result: the threshold ρ_rep_high for a configuration to enter the group demonstrating the contribution, and the threshold ρ_rep_low to enter the degenerate control group (unit: dimensionless ratio in [0,1], taken as the mean ρ_rep of the sensitive patterns sampled in SC2). The measurements already exist, so the choice is finite and clear: an upper threshold in the interval (0.154; 0.323) separates {dna, msnbc} from {leviathan, bible}; a lower threshold around 0.05 puts {fifa, sign, bms1, bike} into the degenerate group; the band in between decides whether leviathan and bible form an intermediate group or are excluded. Together with the corresponding β threshold. The author decides (it may not be inferred backwards from the wish that a given dataset go into a given group — dataset_survey.md section 4 itself recalls this); TRUCKS is no longer in the list because its source could not be obtained.

**ρ_rep thresholds separating the main group and the degenerate group**

Fill two numbers into preregistration.md line 28: ρ_rep ≥ a goes into the main group, ρ_rep ≤ b into the degenerate control group (a, b are dimensionless real numbers in [0,1]). The measured range is now continuous rather than split into clusters: bms1 and sign 0.000; fifa 0.008; bible 0.153 and leviathan 0.154; msnbc 0.323; dna_mt745584 0.786. One must choose between three kinds of cut: (i) a = b (no grey zone, every configuration is assigned a group — then where the cut lies decides which group leviathan/bible go into); (ii) a > b with a grey zone, and it must be stated whether configurations falling into the grey zone are excluded from both groups or go into a third, "intermediate" group as dataset_survey.md §4 proposes; (iii) thresholds by percentiles of the SC2 table itself instead of absolute numbers. With grounds, and the grounds may not be "so that dataset X goes into group Y". The author decides.

**ρ_rep and β thresholds for assigning the degenerate group**

Fill three numbers into preregistration.md section 1, then commit (the git log as evidence of the time) before the first SC3 run: (a) the minimum ρ_rep threshold to enter the main group; (b) the maximum ρ_rep threshold to be placed in the degenerate control group; (c) the maximum β threshold for the constraint control group. Unit: dimensionless ratio in [0,1], ρ_rep = 1 − sup_seq/sup_NO and β = 1 − sup_NO(with gap)/sup_NO([0,∞)). The measured SC2 table already offers a natural slot in ρ_rep between 0.154 (leviathan) and 0.323 (msnbc), and β of bms1 is 0.094 — but under rule 2.3.10 the thresholds may not be inferred backwards from the wish that a given dataset go into a given group, so the grounds must be written first. The author decides. URGENT: blocks H3, blocks the grouping SC2 → SC3, and as long as it is not locked every SC3 run must carry the --probe flag.

**β threshold for the constraint control group**

Fill a number into preregistration.md line 29: β ≤ b_0 (a dimensionless real number) means the configuration is a control with an ineffective constraint, and state which configuration plays that role. Measured β range: bms1 mean 0.094 (many patterns equal to 0), msnbc [0,10] σ=2000 mean 0.043, sign mean 0.209, fifa mean 0.289, dna mean 0.467, bible mean 0.500, leviathan mean 0.612. Along with this, the priority rule must be locked for a configuration that meets both the main-group threshold by ρ_rep and the control threshold by β — msnbc [0,10] σ=2000 is exactly that case (ρ_rep 0.415 high, β 0.043 low): does it go into the main group, into the constraint control, into both, or is it excluded. Choose between two kinds of control: a natural control (bms1, which already has both low β and ρ_rep = 0) or a control built by widening the gap on a main-group dataset itself. The author decides.

**Grouping thresholds ρ_rep and β for H3**

Fill three numbers into preregistration.md lines 28–29, choosing within the empty bands that the SC2 table has already exposed: the measured ρ_rep is 0.786 (dna), 0.323 (msnbc), 0.154/0.153 (leviathan/bible), 0.052 (bike), 0.008 (fifa), 0.000 (sign, bms1). So one must choose the main-group threshold (natural candidate: ρ_rep ≥ 0.30 keeps both dna and msnbc; or ≥ 0.20 to pull in more?) and the degenerate threshold (≤ 0.05 or ≤ 0.01?), and at the same time decide whether the intermediate group leviathan/bible belongs to one side or stands alone. For β ≤ _null_ of the 'constraint control group' it must also be stated whether low β or high β is the control (measured β: bms1 0.094 lowest, bible 0.500 and leviathan 0.612 highest) — at present the statement 'β ≤ null' contradicts the fact that low β means the constraint LOSES its effect.

**Repeated-item patterns: close the open question or narrow the scope**

Confirm and put in writing the scope of the sensitive patterns, choosing one: (A) lock μ(P) = 1 as the main scope — exactly as Sc3MainComparison.sampleSensitiveSets currently filters — then declare this filter in §11.5 and in row 1 of protocol.yaml, and write in the limitations section exactly the numbers ρ_rep 0.218 against 0.434 for MSNBC and 0.806 against 0.773 for DNA; or (B) open the scope to μ(P) ≥ 2, in which case one must say which bound of P3 CUT-HIDE targets as the masking objective (⌈(F−t)/μ⌉ or F−t), declare the error by the factor μ(P), drop the hasDistinctItems filter and use IlpExact to measure the distance to the optimum on small instances. The author decides. URGENT: before SC3, because it defines the sampling population 𝒮 and how to answer the part of RQ1 'how does the hardness change when patterns repeat items'.

**The unit the threshold applies to: per dataset, per configuration or per pattern**

Correct preregistration.md line 59 to match §8.4 of the research design, which has already locked the grouping unit as the CONFIGURATION, not the dataset. What is truly still open is the exact definition of the axes of a configuration: §8.4 writes (D, 𝒮, σ) but SC2 sweeps (dataset, σ, [g⁻,g⁺]), and the gap width changes the mean ρ_rep by up to 30% relative on msnbc σ=2000 itself. Choose one of the two: configuration = (dataset, σ, [g⁻,g⁺]) and the ρ_rep of a configuration = the mean over the sampled patterns; or configuration = (dataset, σ, [g⁻,g⁺], sensitive set), i.e. grouping at the level of the unit of replication of H2. Hard constraint: the grouping unit of H3 must coincide with, or nest exactly into, the unit of replication of the Wilcoxon test of H2, which §11.6 has locked as (dataset, σ, sensitive set). The author decides.

**The FIFA evidence [17, 17, 17] is in results-probe**

Decide the fate of one sentence in phase1_summary.md lines 85–86. Two options: (i) rerun the FIFA configuration [0,3] σ=3000 without --probe, with a sampling range wide enough for the pattern [17, 17, 17] to land in results-invariant/, then keep the sentence; (ii) delete the sentence. Option (ii) is now much cheaper because the update block of the same day in §3 has replaced the pillar evidence with the DNA dataset (ρ_rep of patterns without repeated items 0.806 against 0.773 for repeated-item patterns), so §3 no longer needs the FIFA number. Note that this pattern is a repeated-item pattern (min_masks_exact=false), i.e. outside the scope μ(P)=1 locked in the code, so even if rerun it could not enter SC3. Does not block SC3; must be handled before any paragraph of §3 is copied into the manuscript. The author decides.

**β threshold for the constraint control group**

Lock a real number in [0,1]: β ≤ ? means the instance is a constraint control (the gap constraint loses its effect), together with the unit of measurement — the mean β over the sensitive patterns of the configuration, or the minimum value. The measured numbers limit the choice: bms1 β 0.094 (suggests a double degeneracy), msnbc at [0,10] drops to 0.043 while at [0,1] it is 0.432, bible 0.500, leviathan 0.612. Note the consequence: because β and ρ_rep pull in opposite directions with the gap width (phase1_summary.md:62-63), a strict β threshold will automatically exclude the wide-gap configurations, which have high ρ_rep — so the β threshold must be chosen at the same time as locking which [g⁻,g⁺] configuration is used for the main group, and the reason for choosing that configuration must be recorded.

**Lock σ and the gap width for each dataset**

Fill in row 1 of protocol.yaml (sigma, l_max, gap, sensitive_size) for each dataset that will run SC3–SC5, SC7, SC8. For msnbc one must choose σ ∈ {1000, 2000, 5000} — different choices change the mean ρ_rep from 0.190 to 0.395, i.e. they change the persuasiveness entirely — and [g⁻,g⁺] ∈ {[0,1], [0,3], [0,10]}. For dna_mt745584 only 3 configurations are measured so far (σ 1000 and 2500, gap [0,1] and [0,3]); decide whether to run more SC2 first and lock afterwards, or to lock now following the recommendation [0,1] of khao_sat §3(b). Constraint: every baseline must be compared at the same configuration, and σ must be declared both in absolute terms and as a share of Σ sup_NO (§11.5). The author decides, after more σ-sweep numbers are available for dna/leviathan/bible.

**ρ_rep threshold for a configuration to enter the main group**

Lock a real number in [0,1]: ρ_rep ≥ ? means the instance (dataset, σ, gap) goes into the main group. At the same time lock the unit of measurement: take the mean ρ_rep of the sensitive patterns in that configuration, or require EVERY sensitive pattern to meet the threshold (i.e. take the minimum). The measured SC2 data constrain the choice: a threshold > 0.323 excludes msnbc and leaves only dna (0.786) in the main group; a threshold ≤ 0.15 pulls in leviathan/bible as well. If the minimum is chosen, msnbc will hardly pass because its low percentile is below 0.218. It must be filled in and committed before the SC3 run that takes numbers into the paper; choosing only after looking at the SC3 results violates rule 2.3.10.

**ρ_rep threshold of the degenerate control group**

Lock two things at once: (a) ρ_rep ≤ ? is the degenerate control (measured data: sign and bms1 exactly 0.000; fifa 0.008; bike 0.052 — any threshold in the range 0.01–0.05 collects exactly the first three datasets); (b) how to handle instances falling into the band between the two thresholds — choose one: exclude them entirely from the H3 table, or form a third, 'intermediate' group as dataset_survey.md:81 proposes and state clearly that this group is not used to test H3 but only illustrates that ρ_rep and β are independent of each other. If (b) is chosen, §1 of preregistration.md must be corrected to have three groups, not two.

**The role of each dataset after measuring rho_rep**

Fill in and commit into config/preregistration.md, section H3, three numbers: the mean ρ_rep threshold to enter the main group, the ρ_rep threshold to enter the degenerate control group, and the β threshold for the effective-constraint group; the SC2 table separates very clearly (0.000 / 0.008 / 0.052 against 0.154 / 0.323 / 0.786), so a cut around 0.10 can be inferred from the data. Along with this: lock the role of all eight measured datasets and state definitively that TRUCKS is excluded from SC8 because it has no public source, or name an alternative source. Inferred from the SC2 data already available; must be locked and committed BEFORE running SC3.

**The open question P3 when patterns repeat items (mu >= 2)**

Decide one line for config/preregistration.md and for the code: whether the sensitive sets sampled in SC3/SC4 are filtered down to μ(P)=1 or not. If filtered, the filter must be added to Sc3MainComparison.sampleSensitiveSets and declared in the limitations section with exactly the measured numbers (MSNBC: mean ρ_rep 0.218 for μ=1 against 0.434 for μ≥2; DNA: 0.806 against 0.773). If not filtered, the branch of bound P3 and the exact ILP must be finished in order to measure the distance to the optimum for the class μ≥2. The author decides. Urgent before SC3, because it changes the unit of replication and changes the whole result table.

**Lock the state of the research gaps G1 and G5**

After the full text of [R11] is available, or after activating the reduced reading level (§12a item 3), decide three things in §3: G1 moves from 'strong, provisional' to 'strong, official' or is lowered to 'weak'; G5 stays 'provisional' or is closed according to exactly the scope of the review in §4; and restate G1 by mechanism (without an absolute negative sentence, per constraint (b) of §12a item 3), with the measured ρ_rep numbers. The original documents must be read. Deadline: before writing the research-gap claim of the manuscript.

**l_max and gap [g-, g+] for each dataset (row 1)**

Two separate questions. (a) ℓ_max: keep 3 (and change §11.5 'length 2-4' to 2-3 to match), or raise it to 4 and accept the increased cost of mining F_σ on msnbc/fifa? (b) For each main-group dataset, lock the [g⁻,g⁺] used for SC3: [0,1] (high β ~0.43 but lower ρ_rep) or [0,3] (balanced), following exactly the measurements in phase1_summary.md. The author locks it before SC3; changing it afterwards means rerunning all of SC3.

**Target rho_rep and beta when generating data**

Lock the target grid for generation: how many ρ_rep levels (e.g. {0; 0.3; 0.8}) times how many β levels (e.g. {0; 0.3; 0.6}), how many generated copies per cell, and the rule for tuning plantedPerSequence/spacing to approach the target. The author locks it before SC6; the paper still prints only measured values, the targets sit only in row 4 of the protocol table.

**The discretised time-series datasets of the NOSEP line**

Decide once and for all: (a) take a discretised time-series dataset to cover the third kind of data that [R1] uses — then lock the archive and the specific dataset (UCR: which dataset), the discretisation method (SAX or equal-width intervals), the alphabet size and the window size, then run SC2 to measure ρ_rep and β before assigning a role; or (b) state clearly in the paper that the data scope covers only event sequences already in discrete form (clicks, genes, text, transactions) and that [R1]'s additional use of time series is a difference in evaluation scope, not a shortcoming. Note that if (a) is chosen, the discretisation parameters become a new row of the protocol table, because ρ_rep depends directly on the alphabet size.

> Blocks: Two of the three kinds of data of [R1] are already present (DNA, and BMS1, which is Gazelle), the third is not. A reviewer from the NOSEP line will immediately ask why it is missing. Choosing (a) late means rerunning both SC2 and SC3 for the new dataset after the grouping has been locked, and rule 2.3.10 forbids changing the grouping after looking at SC3 results.

### A4. Equivalence margins for Lost and SupDist

Raised by: `design-experiment`, `preregistration`

**The test and the quantitative criterion "near 1" for H3**

Declare all four things for H3 as was done for H2: (a) the test for the part 'greater than 1 on the main group' — one-sided Wilcoxon signed-rank on the ratio |R|/|R|* with null hypothesis median = 1, or a test on the log ratio; (b) the level α (0.05 to align with H2?) and whether H3 joins the same Holm family as H2 or forms a separate family; (c) the effect size reported (rank-biserial to align?); (d) the quantitative equivalence margin for 'near 1' on the degenerate group — a ratio within [1, 1+?] is considered near 1, e.g. ±0.05 or ±0.10. At the same time declare the scope: because Sc3MainComparison.boundFromP1 computes |R|* only for a sensitive set of one pattern without repeated items (in every other case the mask_ratio column is empty), H3 is tested only on that class of instances — this must be said explicitly in the statement of H3, not left implicit.

**Equivalence margin for SupDist**

Lock a real number: the equivalence margin for SupDist is ± how much, computed on the scale already defined in doc §2 line 65 — a dimensionless ratio in [0,1] (Metrics.Row.supDist), NOT an absolute sup_NO difference and NOT normalised by σ; do not reopen the question of the scale. The SC3 probe numbers show the order of magnitude to aim at: the sup_dist of the methods lies around 0.1345–0.1481, i.e. differences between methods of about 0.01. A margin of ± 0.05 would declare everything equivalent; a margin of ± 0.001 would never conclude equivalence.

**The test for H3 and the meaning of "near 1"**

What tests H3: (a) a one-sample test on the ratio |R|/|R|* of the main group with H₀: median ≤ 1 (one-sided Wilcoxon signed-rank, the same unit of replication (dataset, σ, sensitive set) as H2), plus (b) an EQUIVALENCE test by two one-sided tests (TOST) on the degenerate group with an equivalence margin to be locked (e.g. |R|/|R|* ∈ [1; 1.05] to be called 'near 1')? Or only report a confidence interval without a test? To be locked: the test, the unit of replication, the margin defining 'near 1', and whether H3 goes into the Holm correction together with H2 or separately. Record it in preregistration.md §1 under H3.

> Blocks: H3 cannot be tested. The H2 section declares all four things (one-sided Wilcoxon test, Holm correction, rank-biserial effect size, α = 0.05) while the H3 section declares only the grouping thresholds, not the test. §2 moreover locks equivalence margins only for Lost and SupDist, with no margin for the ratio |R|/|R|*, so the part "near 1 on the degenerate group" currently has no criterion for concluding true or false.

### A6. The family of baselines in the Holm correction

Raised by: `design-experiment`, `phebinh`, `preregistration`  (includes items added by the critical pass)

**The family of baselines in the Holm correction**

List by name the baselines in the comparison family of H2 (chosen from the 14 names that Sc3MainComparison.create accepts: occurrence, HH, HR, RH, RR in tier (a); hha, dbsh, sbsh-r1, sbsh-rmax in tier (b); ilp, ilp-lost, ga in tier (c)), and from that write down directly the number m used for Holm. Also lock: is the family counted SEPARATELY for each metric (Lost one family, SupDist one family, |R| one family) or are all metrics POOLED into one family — pooling triples m and makes H2 much harder to reject. State clearly which baselines are not yet fully implemented but are still in the family, so that m does not shrink when it comes time to run. Must be locked before SC3: the current CLI default (5 methods, no tier (b) at all) cannot stand in for the lock, and deciding after seeing the results is selective reporting.

**H1 missing; the labels H1, H2 carry two meanings**

Two separate decisions: (1) whether to add hypotheses for RQ4 (the number of ghost patterns of deletion when g⁺ is finite, SC5) and RQ5 (the cost of HIDE-SEQ, SC4), or to state clearly in the paper that RQ1/RQ4/RQ5 are answered by propositions and description, not by tests — if they are added, the test and the threshold must be locked in the preregistration at the same time as H2; (2) choose a naming scheme so that the two heuristics of P6(c) do not share labels with the hypotheses — e.g. rename the heuristics A1/A2 or HEU-1/HEU-2 (correcting doc lines 274, 212, 305, 403 and Sc7DesignVariants.java:11,23, WorkedExamplesTest.java:96), or renumber the hypotheses. Must be done before locking the preregistration, because Holm 'by the number of baselines' and the number of hypotheses depend on decision (1).

**Baseline [R11] locked after the full text is available**

Set a deadline for the three steps of obtaining the full text in §12a (CESTI/university library; a letter to Meng Zhang; ResearchGate). If it has not been obtained by the deadline: activate the branch already written in §12a item 3 — [R11] does NOT enter tier (b), doc §11.3 line 429 is removed from the baseline table and [R11] remains only in the related-work section. This must be done before locking the preregistration, because Holm 'corrected by the number of baselines' (doc line 382) requires the baseline list to be closed before SC3 runs. If the full text is available, decide in addition: how to switch it to sup_NO and whether it can be applied for r > 1.

**The statistics tool and where the test results are kept**

Lock who computes Wilcoxon, Holm, rank-biserial and the confidence intervals: (a) add a Python script using scipy (the repository already has numpy and scipy in probe/, but no statistics file yet and no declared versions), (b) write it in R with an renv file, or (c) implement it in Java to keep the promise of 'no dependence on external libraries'. Together with two things that must be decided at the same time: in which of the three result directories the output file of the tests (p, p after Holm, effect size, confidence interval, number of pairs) lives and which run identifier it carries — currently ResultSink only opens files for scenario SCn, with no path for derived files; and in which row of the protocol table the tool version is declared (row 5 has only the JDK).

> Blocks: No p-value of the paper is currently produced by any tool, and there is no valid place to store it. Test results computed by hand on a machine and then copied into the paper are the kind of number that cannot be traced back, contrary to rule 2.6.8, which the whole repository enforces for the CSV files.

### A7. Pairing for H2: the same |R| budget, or each method choosing its own

Raised by: `phebinh`  (includes items added by the critical pass)

**Compare Lost at the same |R| budget, or let each method choose its own**

Lock the pairing of H2: (a) each method runs until HF = 0 with a SELF-CHOSEN number of masks, then the raw Lost difference is compared — then |R| must be reported alongside, and it must be admitted that a method that masks more naturally loses more patterns, i.e. the Lost difference mixes both causes; (b) force every method to the SAME |R| (take the |R| of CUT-HIDE, or the |R|* of P1 when available) before comparing Lost — then a masking-budget parameter must be added to the HidingMethod interface, and it must be defined what happens when a baseline does not hide everything within that budget; or (c) compare not points but the TRADE-OFF CURVE of Lost against |R|, in which case the budget grid must be locked, together with how to summarise a curve into one number to feed into Wilcoxon. The SC3 table in §11.1 promises a 'trade-off curve' but the code runs each method exactly once, with no budget knob.

**Handling of failed instances in the paired test**

Declare in advance the exclusion rule for the H2/H3 analysis, in three separate cases: (1) baseline rows with HF > 0 (not everything hidden) — drop the whole pair from Wilcoxon, or keep it and assign Lost a predetermined penalty value, or report it separately as a failure rate; (2) rows carrying the code OT or OOM — the same question, and it must be stated clearly, because dropping the slowest cells is a bias in favour of the slow method; (3) rows with an empty mask_ratio column (|S| > 1 or a repeated-item pattern, where |R|* is not defined) — on which subset H3 runs. Together with a reporting rule: the table must print the number of pairs entering the test and the number of pairs excluded for each reason.

> Blocks: H2 currently states the Lost difference without controlling |R|, so a win may only reflect that CUT-HIDE masks fewer times, not that it chooses positions more cleverly; a reviewer can break it immediately. Without locking it, the caption of the SC3 table cannot be written and the 'trade-off curve' that §11.1 itself promises cannot be built.

### A8. Wilcoxon conventions: zero differences, ties, effect size

Raised by: `phebinh`  (includes items added by the critical pass)

**Number of reruns of the randomized methods**

Lock the number of reruns and how to aggregate them for the RANDOMIZED methods when measuring the MACHINE-INVARIANT metrics (Lost, Ghost, SupDist, |R|), i.e. HH, HR, RH, RR and GA: each instance runs 1 time with seed 2026 as the code currently does, or runs k times with k different seeds (k = ?) and takes the mean, the median, or the best run. If several runs are taken, it must be decided that the unit of replication of Wilcoxon remains (dataset, σ, sensitive set) with the aggregated value, and each run may not be counted as an independent observation. Row 8 of the protocol table currently specifies the number of repetitions only for TIME MEASUREMENT and does not cover this case.

**Wilcoxon conventions: zero differences, ties, effect size**

Declare three computational conventions that the preregistration file leaves blank: (a) instances whose difference is EXACTLY 0 (very likely, because Lost is a small integer; the SC3 probe gives Lost in the range 1–5) are handled by classical Wilcoxon (dropped, reducing n) or by Pratt (kept and ranked) — the two give different p; (b) compute the EXACT p (permutation distribution, feasible when n is small) or a normal approximation with continuity correction and tie correction; (c) which version of the rank-biserial formula (r = (W+ − W−)/(W+ + W−) or 1 − 2W−/(n(n+1)/2)), and computed on n before or after dropping the differences equal to 0.

> Blocks: The trivial tier (a) controls and GA are randomized, so a single run gives a lucky number; the conclusion 'CUT-HIDE beats RR' can flip when the seed changes. Without locking it beforehand, one cannot justify why exactly seed 2026 was taken, and running more after seeing the results is selective reporting.

### A11. The grid of σ, ℓ_max, gaps and |S| for each dataset

Raised by: `code-todo`, `design-experiment`, `paper`, `protocol`

**Which sigma counts the side effects of SC4**

Lock one sentence for SC4: is the sigma that defines F_sigma(D) for counting Lost/Ghost/SupDist kept FIXED for all four levels r in {1,2,3,5} (and equal to exactly the sigma of the corresponding HIDE-DB row of that dataset, taken from protocol.yaml row_1), or does it change with each r (for example sigma = r x |D| for "the same nominal privacy level")? Choosing fixed makes the Lost column comparable horizontally across the r rows and with the HIDE-DB row - exactly the purpose of SC4 - but it must be declared that when r is small, the number of masks rises sharply while the denominator F_sigma(D) does not change. Choosing per r changes the denominator in every row, the Lost column loses its meaning as a horizontal comparison and SC4 can no longer answer RQ5. The mechanism already exists (the frequentThreshold field of HideProblem and the seven-parameter constructor); only one policy sentence + the sigma value for each dataset are missing. The author decides; needed before implementing and running SC4.

**The values of ell_max and epsilon are not yet set**

Only a narrow choice remains: keep ℓ_max = 3 and ε = 0.5 for EVERY dataset (declared once in the protocol table, exactly as in the ten SC2 configurations already run), or set separate values per dataset? If kept uniform, fill the same pair into all eight per_dataset rows of protocol.yaml row 1 and row 2 and consider it done. If set separately, a measured basis must be given and SC2 rerun for each new pair, because when ℓ_max changes F_σ(D) changes and when ε changes B_ε changes, carrying along all the weights w(i), and no Lost/SupDist measure is comparable between the two runs. σ alone must be declared per dataset, because the ranges already run differ (bms1 100 and 300; sign 100 and 200; fifa 3000; msnbc 1000, 2000, 5000).

**The values of σ, ℓ_max, gap and |𝒮| for each dataset**

Fill row 1 of protocol.yaml for each dataset that will run SC3 (at least dna and msnbc as the main group; sign, bms1, fifa as degenerate controls): which σ range each dataset runs at (how many values to choose, anchored at the σ already measured in SC2 — dna 1000, msnbc 1000/2000/5000, fifa 3000, sign 100/200, bms1 100/300 — or a denser sweep?), whether ℓ_max stays 3 for every dataset or differs, whether [g⁻,g⁺] stays [0,3] for every dataset or [0,1] is also used for datasets that need a high β (giai_doan_1 §2 recommends [0,3] as the main configuration, [0,1] if a high β is wanted), and whether |𝒮| runs the full {1,5,10,20} or a reduced set. Along with this, change the default of Sc3MainComparison so that sensitiveSets = 20 to match §11.5, or state clearly in the paper that fewer are used.

**Sigma for each dataset (row 1)**

For each dataset ACTUALLY present in the repository (dna_mt745584, msnbc, fifa, sign, bms1, leviathan, bible), lock a sequence of at least 3 absolute σ values (unit: number of non-overlapping occurrences) as the horizontal axis for SC3-SC8, recorded with the corresponding ratio σ/Σ_P sup_NO; at the same time decide whether to drop or keep the three rows kosarak, trucks, synthetic in the protocol table. The author locks it, based on the SC2 table already in results-invariant, and must lock it BEFORE the SC3 run whose numbers go into the paper (preregistration rule 2.3.10).

> Blocks: Blocks SC4. If sigma changes between rows, the Lost column is not comparable horizontally with the HIDE-DB row at "the same nominal privacy level" - and that is precisely the purpose of SC4.

### A12. The ε threshold of B_ε for each dataset

Raised by: `design-gap`, `design-gate`, `design-theory`, `protocol`

**Step 3 with repeated-item patterns: the candidate set is not defined**

Lock the definition of the candidate set and the check of step 3 when μ(Q) ≥ 2, choosing one: (i) keep the cheap rule the code uses now — 'a position carrying at least one vital node by L5' — and state clearly in the paper that this is a lower APPROXIMATION of the set of vital positions, at a cost of one maximum flow for each Q ∈ B_ε; (ii) do exactly §9.2: run one maximum flow on NT − V_i for each candidate position (limited to positions that carry nodes), at a cost of |positions carrying nodes| × the cost of one flow for each Q, and change NettreeFlow/NettreeCutIndex to match; (iii) filter B_ε down to μ(Q) = 1 in FrequentPatterns.nearThreshold, in which case L5 gives the exact set with one maximum flow and the complexity sentence becomes simpler. The unit of the cost to be stated: the number of maximum-flow runs over |B_ε|. The author decides (choosing between the cheap approximation and the full check). URGENT: before SC3, because this decision changes w(i) in step 4, hence changes every Lost number of SC3, and also changes the index time numbers in SC6.

**The ε threshold of B_ε has no value for each dataset yet**

Lock ε (a dimensionless real number, defining B_ε = {Q : σ ≤ sup_NO(Q,D) < (1+ε)σ}) and fill row 2 of protocol.yaml: keep ONE common value ε = 0.5 for all eight datasets (exactly the code default) with a one-sentence basis, or set ε separately per dataset? If set separately, which criterion decides: an upper bound on the size |B_ε| (number of patterns) to keep the cost of step 3 within budget, or the coverage of near-threshold patterns (the share of F_σ inside the band)? The author decides, but a sweep of |B_ε| over ε ∈ {0.1, 0.25, 0.5, 1.0} must first be run on each dataset to have numbers. URGENT: before SC3, because w(i) in step 4 and every Lost number of SC3 depend on ε, and §11.5 requires printing a |B_ε| warning before running a large set.

**The ε threshold that defines the 'near-threshold patterns' B_ε**

Choose between two forms of locking row 2 of the protocol table: (a) keep a single ε value for every dataset — confirm that ε = 0.5 is the current default in the code (Sc3MainComparison.java line 71) and delete the seven per_dataset cells in protocol.yaml, or (b) set ε separately for each dataset (msnbc, dna_mt745584, leviathan, bible, fifa, sign, bms1) according to the measured |B_ε|. Unit: a dimensionless real number, with as its basis the size |B_ε| and the cost of step 3 observed in SC2/SC6. The author decides, and records it in config/protocol.yaml before running the SC3 whose numbers go into the paper.

**The value of the epsilon threshold of B_epsilon**

Decide exactly one thing: keep ε = 0.5 uniform for every dataset (the default value running in Sc3MainComparison and already recorded in protocol.yaml) or set ε separately for each dataset; then fill row 2 of config/protocol.yaml for all eight datasets. The cutting rule for |B_ε| does not need to be decided again (§9.2 has locked it: limited by ε, and turning off step 3 does not change HF). The author decides; needed before SC3 so that every number put into the paper has its ε declared with it.

**Epsilon threshold of B_eps per dataset (row 2)**

Choose between two options: (a) declare ε = 0.5 uniform for every dataset, with a one-sentence basis resting on the measured |B_ε|; or (b) set ε per dataset when |B_ε| blows up (dna and msnbc are the two datasets at risk), and if sweeping, sweep how many levels, for example {0.1, 0.25, 0.5}. The author locks it BEFORE SC3, because when ε changes the weights w(i) change, carrying Lost and SupDist with them, and all of SC3 must be rerun.

> Blocks: The size and memory of the index cannot be estimated (the core claim of contribution 2); step 3 and step 4 of CUT-HIDE cannot run; SC6 cannot measure peak memory; the file code/config/preregistration.md cannot be locked yet.

### A13. The sup_NO percentiles that draw the sensitive sets

Raised by: `design-experiment`

**The sup_NO percentiles for sampling sensitive sets**

Lock exactly three numbers, then change Sc3MainComparison.sampleSensitiveSets to match: (1) how many percentiles of sup_NO to use and which ones — three levels low/middle/high (25/50/75) or equal-count buckets as Sc2DataRegime does now; (2) how many sensitive sets to take per percentile so that the total reaches 20 sets per cell as §11.5 requires (for example 3 percentiles × 7 sets, or 4 × 5); (3) constrain the pattern length to exactly 2–4 (the code currently only constrains ≥ 2, and with ℓ_max = 3 it actually yields only 2–3 — so decide at the same time on ℓ_max = 4 or change §11.5 to 2–3). Must be locked before SC3 because the unit of replication of H2 is exactly (dataset, σ, sensitive set); record it in protocol.yaml row 1 and in the preregistration.

> Blocks: Two SC3 runs could draw two different sensitive sets and still both match the description in the paper; the unit of replication of H2 is exactly (dataset, σ, sensitive set).

### A14. The scope of μ(P): keep it narrow at μ=1, or open it to μ≥2

Raised by: `design-theory`, `phase1-findings`, `runbook`

**Node weight when a pattern has μ(P) ≥ 2**

Lock the SCOPE of mu(P) for the main table, choosing one of the three options listed in phase1_summary.md section 3: (1) keep the scope at mu(P) = 1, declaring in the limitations section that it is the class of patterns that repeat less on MSNBC (0.218 against 0.434) but repeat MORE on DNA (0.806 against 0.773); (2) open to mu(P) >= 2, using bound P3 and IlpExact on small instances to measure the gap to the optimum, accepting that there is no exact result for that class; (3) already done -- add DNA. New evidence from dataset_survey.md section 3(c) makes option 1 clearly better, so this is a confirmation and not a reconsideration from scratch. Along with this, if (2) is chosen, a row must be added to protocol.yaml implementation_decisions recording how weights are assigned when mu(P) >= 2 (currently only in RUNBOOK, not yet in the protocol table). WHO DECIDES: the author. URGENT: before SC3, because the scope of mu(P) decides which patterns are drawn into the sensitive set.

**A ceiling for the ratio |R|*/Σ|S| and the handling of extreme patterns**

Decide whether to set a ceiling on |R|*/Σ|S|, and if so how high. The context has narrowed: the two patterns 0.206 and 0.184 that this point worried about are repeated-item patterns and so have been excluded from SC3 by the code; the real peak in the μ(P)=1 class is 0.102 (dna [3,4], σ=1000, [0,3]). Three choices: (i) set no ceiling, only print the ratio exactly as in §8.4 and report the heaviest pattern separately — valid because the research design does not require a ceiling; (ii) set an absolute ceiling on mask_share (for example exclude every instance whose |R|*/Σ|S| exceeds a threshold) and write the exclusion rule into the preregistration BEFORE running SC3; (iii) exclude nothing but change the aggregation to the median instead of the mean, so that one heavy instance does not dominate. If (ii) is chosen it must be locked before SC3, so as not to be regarded as selecting data after seeing the results. The author decides.

**How to print |R|* and β when a sensitive set holds several patterns**

Lock the rule for printing |R|* and the ratio |R|/|R|* for the cells |𝒮| ∈ {5,10,20}, choosing one of three: (a) state clearly in preregistration.md that RQ3 and H3 are defined only for |𝒮| = 1 with μ = 1, and the SC3 table leaves that cell empty, exactly as the code does now; (b) use the pooled lower bound = MAX over the patterns of |R|*_P (valid because each mask lowers the sup_NO of each pattern by at most 1 by L1; note that the SUM is NOT a valid lower bound, because one mask can serve several patterns), then change boundFromP1; (c) print PER pattern, one row per pattern, and redefine the unit of replication of H3 to match. Unit: number of masks. The author decides, because this is the definition of the quantity of a hypothesis and cannot be derived from the data. URGENT: before SC3, because it falls under rule 2.4.7, locked before running.

> Blocks: It is not yet known how far CUT-HIDE may be claimed to be exact on real data (§5 says "Exact when μ(P)=1"); the statement in the manuscript for the case μ(P) ≥ 2, which is the common case in MSNBC/FIFA, cannot be written yet.

### A15. Several sensitive patterns under a database threshold: LP rounding or hybrid GA

Raised by: `code-todo`, `design-gap`, `design-theory`, `paper`

**CUT-HIDE with several patterns under the database threshold: LP or hybrid GA**

For the CUT-HIDE branch with several patterns + database threshold (CutHide.runMultiPattern when isDatabaseMode), two discrete things must be locked: (1) the definition of the joint cost curve c_S(k) when the sensitive patterns share a position - in what unit k counts (the total drop of sup_NO summed over every sensitive pattern in the sequence S, or a vector of per-pattern drops), because P7 is defined only when the cost is a function of one variable; (2) the algorithm: (a) rounding the linear relaxation, which gives the approximation factor l_max but needs an LP solver added to the Java build, which is currently self-contained (IlpExact is deliberately branch-and-bound and uses no external solver), or (b) a hybrid GA initialised from the cuts, with no guarantee but reusing the existing infrastructure. Unit of evaluation: the ratio |R|/|R|* and the running time. The author decides, and can rely on numbers already available: probe/oldprob.py gives a mean OPT/LP integrality gap of 1.001, i.e. option (a) is nearly always optimal on small instances. Must be locked BEFORE SC3 because protocol.yaml row_1 sets sensitive_size = [1, 5, 10, 20]: three of the four configurations fall into this branch and currently throw UnsupportedOperationException.

**Several sensitive patterns at the database threshold: which mechanism to choose**

Choose EXACTLY ONE mechanism for allocating the number of masks between sequences when |𝒮| > 1 under the database threshold, then write the specification of the subprocedure and change §7 to match: (A) extend the ratio greedy of §9.2 step 7 to the WHOLE database (drop the per-sequence loop) and keep ILP only as the gauge of the gap to the optimum on small instances — cheapest, but the promise 'guaranteed factor l_max' in §7 line 224 must be dropped; (B) linear relaxation + rounding, which must state and prove the factor l_max, implemented in baseline/IlpExact.java; (C) hybrid GA initialised from cuts, which must declare the population size, the number of generations, the crossover/mutation rates and the seed (row 3 of protocol.yaml is null), implemented in baseline/GaHybrid.java; (D) generalise the dynamic programming of P7 to a cost curve pooling several patterns. The author decides; if (B) or (C) is chosen, an extra proof or a parameter set is needed, if (A) is chosen the list of contributions must be changed. URGENT: blocks the |𝒮| ∈ {5,10,20} branch of SC3, the Metaheuristic column of the §11.4 table and the tier (c) baseline.

**Metaheuristic family and parameters for plain GA and hybrid GA**

Only two cells remain to be locked, not the whole GA design. (1) The tier (c) control: take EMO of [R12] (electromagnetism-like optimisation, same group of authors, must be read again to implement correctly) or an ordinary GA with random initialisation - these two choices answer the same question "what does a method of another family achieve", but EMO carries the risk of overlapping the contribution, and row 30 still awaits a cross-check against the way the PhD proposal uses NSGA-II, while row 29 has already forbidden Pareto/NSGA-II. (2) Four numbers + seed: population size, number of generations, crossover rate, mutation rate, and the value for protocol.yaml row_3 genetic_algorithm (currently null). Units: number of individuals, number of generations, probability in [0,1], integer seed. The author decides; must be locked before running SC3 with methods containing "ga", because the fitness function HF/Lost/|R| and the number of evaluations decide both the running time and the conclusion "the share of the contribution due to initialising from cuts".

**The several-pattern case: LP rounding or hybrid GA**

Choose ONE of three for the branch with several sensitive patterns under the database threshold, and choose before running SC3 with |𝒮|>1: (a) rounding the linear relaxation — the paper may claim the approximation ratio ℓ_max (with ℓ_max=3 that is a factor 3), the relaxation must be implemented in IlpExact and the guarantee proved; (b) hybrid GA initialised from cuts — no ratio, the paper must present it as a search layer for the NP-hard part, with a mandatory comparison against plain GA to separate the contribution of the initialisation, and must declare population, number of generations, crossover and mutation rates in row 3 of the protocol table; (c) ratio greedy as §9.2 step 7 has specified and the code has implemented for HIDE-SEQ, extended to allocation across sequences — cheapest, but it lowers the sentence "only uses an approximate search layer" in the abstract to the level of a pure heuristic. Once chosen, baibao.tex:801-803, doc §7 item 3 and doc §9.2 step 7 must all be changed together to agree.

**The solution for the several-pattern case: LP rounding or hybrid GA**

Choose one solution for the several-pattern branch under the database threshold (where CutHide.java line 143 currently throws an error): (a) rounding the linear relaxation — if chosen, a proposition must be added to §8.1 proving the approximation factor ℓ_max, otherwise the phrase 'with a guaranteed factor ℓ_max' must be removed from contribution 3; (b) hybrid GA initialised from cuts — GaHybrid must be implemented and the parameters population, generations, crossover, mutation declared in row 3 of the protocol table; (c) extend the ratio greedy of step 7 to the database threshold by pooling the cost curves, accepting that there is no guarantee. Unit: one choice + (if (a)) one proposition with a proof. The author decides, before running SC3 with |𝒮| > 1 and before locking the sentence of contribution 3.

> Blocks: Step 7 of CUT-HIDE cannot be implemented; the several-pattern branch of SC3 cannot run; the tier (c) baseline in §11.3 assumes that a 'hybrid GA' exists to measure the contribution of the initialisation; the claim 'with a guaranteed factor ℓ_max' in the list of contributions has as yet no proof in §8.1.

### A16. What a "small instance" is, and which ILP variant SC3 takes as its reference

Raised by: `code-todo`, `design-experiment`

**Which ILP variant SC3 uses as the optimum reference**

Only one cell remains: when |S| > 1 (where P1 gives no |R|*), is the optimum reference column of the SC3 table taken from "ilp" (exact-minR, minimises |R| only) or from "ilp-lost" (exact-minR-minLost, adds the Lost objective)? §2 has stated a lexicographic objective, so by definition it must be exact-minR-minLost; but choosing it means accepting the price: IlpExact enumerates every minimum-size solution, bounded by MAX_OPTIMAL_SETS = 20,000, and instances are limited by MAX_CANDIDATES = 64, MAX_MASKS = 12, so the maximum instance size that can run must be stated clearly, along with how exceeding the limit is handled (drop the row or mark OT). If the cheaper exact-minR is chosen, the table can only answer "how many masks are needed", not "how much Lost is saved", and the RQ2 question ("at the same |R|, does CUT-HIDE lose fewer patterns") loses its upper reference. Also lock at the same time: add "ilp" or "ilp-lost" to the default methodList of Sc3 line 75 (currently both are outside the default), so that the table does not silently miss a column. The author decides; before SC3.

**Definition of "small instance" for the exact solver**

Decide how to BUILD small instances, because the two caps already in the code (≤ 64 candidate positions over the whole database, ≤ 12 masks) let no real instance through: how many sequences to subsample from each dataset (|D| = ?), what length to cut sequences to, what |𝒮|, which σ, and how many such instances to take per dataset so that the ratio of the gap to the optimum is meaningful (for example 30 instances/dataset, sharing the unit of replication (dataset, σ, sensitive set) of §11.6)? At the same time decide whether to use the 'exact-minR' or the 'exact-minR-minLost' version (IlpExact.withSideEffectObjective) — the class Javadoc requires the scenario to state clearly which version was used. Record it in protocol.yaml and the preregistration before SC3, because the gap to the optimum is the evidence for RQ2.

> Blocks: The ratio |R|/|R|* and the answer to RQ3 depend on which reference is chosen; if it is not locked, the SC3 table has no column comparing with the exact solution, i.e. tier (c) is lost from the main comparison table.

### A17. Stamping the code version into the result files (now unversioned)

Raised by: `phebinh`  (includes items added by the critical pass)

**Stamping the code version into the result files (now unversioned)**

Lock how the code version is attached to each run whose numbers go into the paper, and the fate of the numbers already obtained: (a) set HIDENOSP_REVISION = git rev-parse HEAD in a wrapper script (or let RunId call git itself) and add the condition of a clean working tree, adding a -dirty suffix when it is dirty; (b) accept the label unversioned and drop the code-version column from requirement 2.6.8. Along with this: the 16 SC2 files of phase 1 carry the label dev_unversioned — keep them as they are and state in the paper which commit they belong to by date and time, or rerun one pass of SC2 after the stamping mechanism exists and after protocol.yaml row 1 is locked.

> Blocks: The paper's table of data characteristics (ρ_rep, β) is taken from the existing SC2 files; without a code version nobody, the author included, can rebuild exactly the same numbers when the code changed four commits in one day. The preregistration file itself requires 'git log as evidence of the time', yet the results do not point back to any commit.

### A23. The content of the preregistration file, and making the go/no-go official

Raised by: `code-todo`, `design-gap`, `design-gate`, `design-theory`, `runbook`

**Reference NETGAP implementation for the SC1 check**

Choose one of three, and the consequences are quite different: (a) take the NETGAP implementation of [R1] from the authors' source code or from a public library - a cell "source and version of the NETGAP library" must be added to protocol.yaml (row_5 currently has no such cell, so the TODO points at a cell that does not exist) and an external dependency accepted in the Maven build, which is currently self-contained; (b) reimplement NETGAP in this repository - then the wording in the paper must be lowered from "checked against the original implementation" to "checked against an independent implementation of the NETGAP algorithm as described in [R1]", because when one person implements both, errors in UNDERSTANDING the definition will coincide and the check cannot catch them; (c) drop this half of the check and change line 396 of the design, relying entirely on the comparison with brute force (BruteForce), which has already run. The author decides. URGENT: must be locked before SC3 because preregistration.md section 4 sets "SC1 must be green (0 mismatches) before any other scenario", and every SC3 number rests on sup_NO.

**Peak memory of the index: measure the heap or the RSS**

Lock the quantity 'peak memory' of SC6 and fill row_6_memory_measurement: (i) report only the peak heap sampled inside the process (MemoryProbe, period 50 ms) and CHANGE §11.7 row 6 to drop the RSS requirement, stating clearly in the table note that it is the heap and not the RSS; or (ii) keep the maximum RSS of the process, in which case three things must be locked: who writes the wrapper script (Get-Process on Windows, /proc/self/status on Linux), what the sampling period is in ms, and whether to print one column or two (heap and RSS). Unit: peak bytes (or MiB) per run. The author decides. Urgency: before submission — SC6 runs last according to preregistration.md section 4 and exp/Sc6Scalability is not implemented yet — but it must be done before the SC6 run whose numbers are used, because this is the column that compares the Nettree cut index with HHA, DBSH, SBSH.

**Which machine is the declared timing machine**

Choose ONE physical machine as the declared measuring machine and fill all six cells of protocol.yaml row_5 (jdk -- already present as '17.0.19 OpenJDK 64-Bit Server VM' at the head of every result file; xms_flag; xmx_flag -- the current runs use -Xmx4g, heap_max_bytes 6442450944; operating_system; cpu; ram; machine_name), plus two related cells that are null: row_7 limit_per_run_seconds and row_9 warmup_runs. Along with this it must be decided: may PC-THAI (the current development machine) also be declared the measuring machine, or is another machine used -- if another machine is used, every timing number must be rerun there. WHO DECIDES: the author. URGENT: before SC6, not as urgent as [0]/[1], because SC6 runs last according to preregistration.md section 4 and SC6 itself currently still throws UnsupportedOperationException.

**Acceptance threshold for the certified share UB = LB of the λ sweep**

Decide how to state contribution 3 so that it does not collide with P6(e): choose between (a) dropping the general claim 'certified' and replacing it with a conditional quantitative statement such as 'the λ sweep returns a pair of bounds LB/UB, and certifies optimality on most of the measured (S,k) pairs — probe: 14,862/14,867', or (b) keeping the phrase 'certified' but with an interpretation threshold locked in advance in config/preregistration.md (unit: the share of (S,k) pairs with ⌈LB⌉ = UB, for example ≥ 0.95), together with a sentence saying in advance what will be done if SC7 falls below the threshold. The author decides; if (b) is chosen it must be filled into preregistration.md before running the SC7 whose numbers go into the paper.

**Choosing a second reader to check the proofs**

Name one checking reader (within the collaborating group [R6]/[R12] or outside it), hand over exactly the list of proofs of §8.1, consisting of L0, L1, P1, P2, Corollary P1c, L5, P3, L4a, L4b, P6(a)–(e), P7, P5; schedule the reading BEFORE §5–§7 are frozen; and lock where the result of the check is recorded (a separate file outside the repository is suggested, for example doc/doc_kiem_chung_minh.md, not put into the manuscript). The author decides. Needed before submission, because §13 item 1.2.1 cannot be ticked without it.

**Moving go/no-go from a provisional conclusion to an official one**

Lock two things and write them directly into §12a: (i) the official go criterion — keep the v0.1 criterion unchanged (HIDE-SEQ + P1–P2 + CUT-HIDE are still ours) or add the condition 'SC8 must beat HHA/DBSH/SBSH at least on Lost'; (ii) the deadline for waiting for the full text of [R11], after which the reduced reading level (§12a item 3) is triggered automatically and 'go' is recorded without waiting any longer. The author decides. Urgent before SC3, because §13 item 1.1.7 (locking the list of contributions) hangs on it.

> Blocks: SC1 currently lacks half of the check that the research design (line 396) requires: "NETGAP, maximum flow and brute force give the same sup_NO; number of mismatches = 0". Without this half there is no evidence that the sup_NO implementation matches the original definition, and every SC3 number rests on sup_NO.

### A24. Tier (a) baselines when a sensitive set holds more than one pattern

Raised by: `code-todo`

**Tier (a) baseline when the sensitive set has more than one pattern**

The shape of the SC3 table when |S| > 1 must be locked, choosing one of three: (a) extend RandomMasking to several patterns - the cheapest conceptually (choose sequences by the total sup_NO over every sensitive pattern, choose a random position on any sensitive occurrence), but that is a NEW algorithm that we set up, no longer "the two-way design of [R20]", and it must be declared as such; (b) accept that the tier (a) control exists only at |S| = 1, i.e. the SC3 table splits into two parts with different method sets, and the table note must say clearly that the columns HH/HR/RH/RR and occurrence-count in the |S| > 1 cell were not left out by oversight; (c) replace tier (a) entirely by HHA when |S| > 1 - but then the trivial control is lost, and it is the thing that shows "the problem does not solve itself". The author decides. URGENT: must be locked before SC3 at the large sensitive-set sizes, because Sc3 deliberately does not silently drop a baseline (lines 45-46), so the configuration |S| in {5, 10, 20} of protocol.yaml row_1 will throw UnsupportedOperationException midway instead of printing the table.

> Blocks: SC3 deliberately never silently drops a baseline, so the configuration |S| > 1 will throw an exception instead of printing the table. Must be locked before running SC3 at the large sensitive-set sizes.

### A25. Checking SBSH against Algorithms 2 and 3 of [R3]

Raised by: `code-todo`, `design-theory`

**Checking SBSH against Algorithms 2 and 3 of [R3]**

Three factual questions must be taken from Algorithms 2 and 3 of [R3]; they cannot be derived from any file in the repository: (1) which positions the grey label covers - only positions that carry no sensitive node, or also positions that carry nodes but do not lie on the cut; (2) whether the radius r of Replace counts ARCS of the Nettree or POSITIONS in the sequence (a mismatch makes the "smallest r" and "largest r" variants of SC7 lose their correct meaning, because Sbsh currently interprets r as a number of Nettree LEVELS: the constructor Javadoc says "in Nettree levels away from the red node"); (3) where C-PA and C-SA differ from ordinary Replace. Only after reading does a real decision come: keep the current implementation (cut every path through the red node by taking a whole Nettree level, choosing the cheapest level within the radius) and state clearly in the paper that it is "our variant", or replace it with exactly the authors' procedure. The reader of the original source decides the factual part, the author decides the keep-or-replace part. Must be done before SC3 because SBSH is in the main comparison table, and before SC7 (radius sweep) and SC8.

**The maximum-flow algorithm and the number of λ values for the weighted case**

Lock the complexity sentence of step 5 to match the code, choosing one: (i) keep Dinic and publish Dinic's general bound O(|V|²·|A|) for each computation of a weighted cut, dropping the citation of [R31] at this point; or (ii) keep the claim O(|V|·|A|) [R31], in which case Dinic must be replaced by an implementation that reaches that bound for the weighted case. At the same time write down the bound on the number of λ values that is already in the code: at most (number of distinct weights ≤ |V| + 1) + 60 evaluations for each refinement interval, i.e. O(|V|) maximum flows. The author decides (this is a decision about what is published, not something derived from data). Urgency: before submission, does not block SC3; but it must be done before interpreting the SC6 timing numbers against HHA/DBSH/SBSH.

> Blocks: No SBSH number in SC3, SC7 (sweep of the smallest and the largest radius) and SC8 can be used for the manuscript until the check is done - the Javadoc itself sets that condition.

---

## B. Blocks submission — does not block the runs

### B1. Measuring machine, JVM flags, number of warm-up runs, time limit

Raised by: `code-todo`, `design-gate`, `preregistration`, `protocol`

**Declaring the measuring machine used for SC6**

Only two fields really need a person to decide: (a) machine_name — set a fixed label for the machine that will carry ALL the time and memory measurements (RUNBOOK uses PC-THAI as the development machine; if that same machine is the measuring machine, give it a separate label other than 'dev', for example 'pc-thai-do', because ResultSink.java:93 rejects exactly the word 'dev'); (b) xms_flag and xmx_flag — lock one fixed pair used for every SC6 run (the existing runs are inconsistent: heap_max_bytes 6,442,450,944 in the SC2 file but 2,051,014,656 in the SC3 probe file, i.e. they ran with different -Xmx). The remaining four fields need no decision: jdk and the heap size are copied from the header of the result file, operating_system/cpu/ram are factual records about the chosen machine. Not urgent: SC6 runs last according to §4 and is not implemented yet (Sc6Scalability.run throws UnsupportedOperationException), so this does not block SC3.

**Number of warm-up runs before timing**

Fill one number into protocol.yaml row_9 warmup_runs BEFORE running SC6, and lock the rule at the same time: a single number for every scenario and every size (for example 3, enough for the JIT to compile the hot loops of Dinic and PathCounts), or a number of warm-up runs that varies with the length of one run (for example discarding the runs in the first 2 seconds, similar to table 11.6, which is already stratified by time)? If option two is chosen then Timing.measure must change its signature: at present it takes warmups as a fixed integer and only times one probe run AFTER the warm-up is done, so it cannot yet express a time-based rule. The author decides; the value must be written into protocol.yaml and committed before the first SC6 run, because choosing after seeing the measurements breaks the lock-in-advance rule.

**Choosing and declaring the measuring machine for SC3 to SC7**

Lock and fill rows 5, 6, 7, 9 of config/protocol.yaml: the name of the measuring machine together with CPU, RAM, operating system; the JDK version and the specific pair of -Xms/-Xmx flags; the limit in seconds for each run (which produces code OT); the number of warm-up runs. Plus a separate decision: does the memory table print 'heap used' (the existing MemoryProbe, 50 ms period) or 'peak RSS' as §11.7 line 6 demands — if RSS is chosen then a wrapper script must be written first. The author decides; needed before SC6, does not block SC3 to SC5, SC7, SC8.

**Reference measuring machine and run environment (row 5)**

Name exactly ONE machine as the measuring machine (possibly PC-THAI itself if that configuration is acceptable, or another machine), give it a name passed to --machine, then copy JDK/operating system/CPU/RAM into row 5. The author decides before running SC6; SC3-SC5, SC7, SC8 can still run on any machine because their numbers are machine-invariant, so the schedule of weeks 8-11 is not really blocked.

**JVM flags -Xms and -Xmx**

Lock one fixed pair of flags used for EVERY algorithm and every baseline in SC6 and SC8, for example -Xms = -Xmx = 8g (setting Xms equal to Xmx removes the noise of a heap that grows gradually), with a justification based on the RAM of the measuring machine and on the largest instance to be run. The author locks it together with the measuring-machine point, before running SC6; changing the flags later means measuring the whole time and memory tables again.

**Number of warm-up runs before measuring (row 9)**

Lock the number of discarded warm-up runs (for example 3 or 5) by measurement, not by feel: trial-run a representative instance on the measuring machine, see from which iteration the time stabilises below a declared variation threshold (for example fluctuation < 5%), and write that number into row 9 BEFORE the first SC6 run; every time measured before the lock must be discarded.

**Time limit per run (row 7)**

Lock ONE single cut-off threshold in seconds per run (for example 1800 s), applied alike to CUT-HIDE, every baseline and the exact solver; together with the cut-off mechanism (interrupting the thread from inside, or killing the process from an outer wrapper script) and the way the OT cell is recorded in the table. The author locks it before SC3, because the ILP cell of SC3 has no bound at all yet.

> Blocks: SC6 cannot write time and memory numbers into the paper: the default label `dev` is rejected by `hidenosp.io.ResultSink`, and the corresponding fields in config/protocol.yaml (`machine_name`, together with `jdk`, `xms_flag`, `xmx_flag`, `operating_system`, `cpu`, `ram`) are still null.

### B3. σ reported as a share or as an absolute number

Raised by: `design-experiment`, `protocol`

**σ: keep it as a share or keep it absolute**

When SC6 changes |D| (and the sequence length), does σ keep the same absolute number or the same share σ/Σ_P sup_NO? Note the asymmetric cost: keeping it absolute is the option that needs NO extra code (FrequentPatterns.mine already takes an absolute σ), whereas keeping the share needs an extra conversion step and a warning (doc line 451 has already promised 'The runner prints a σ conversion warning'). The author decides and writes it into protocol.yaml row_1 note (replacing the sentence 'SC6 decides' with the real decision) before implementing exp/Sc6Scalability, and the paper must print σ in both forms for each dataset whichever convention is chosen.

**Report sigma as a share or absolute**

In SC6, as |D| and the sequence length change, keep the absolute σ fixed (easy to put next to the ψ threshold of [R3]) or keep the share σ/Σ_P sup_NO fixed (keeping the pattern density constant as the data grow)? Choose exactly one, state it in one sentence in the paper, and every table SC2-SC8 prints the other form in parentheses. The author locks it before running SC6.

> Blocks: The sigma axis cannot be made consistent across tables SC2-SC8; if the convention is changed after the runs, all the trade-off curves must be redrawn.

### B4. Parameters and scale of the synthetic data

Raised by: `protocol`

**Scale of the synthetic data (row 4)**

Lock the generation grid of the synthetic dataset: alphabet size (for example 4 like DNA or 17 like MSNBC), number of sequences N and sequence length in the base configuration, plus the sweep range for SC6 (for example N ∈ {10^3, 10^4, 10^5} × length ∈ {50, 100, 200}). Unit: integer. The author locks it before running SC6; does not block SC3.

> Blocks: The synthetic dataset cannot be generated, so row 1 and row 2 for synthetic cannot be filled; SC6 (scaling with |D| and sequence length) has no sweep range.

### B6. The source of TRUCKS, and what becomes of SC8 without it

Raised by: `code-todo`, `design-experiment`

**The psi threshold and the reporting units of the original papers for SC8**

Two things must be taken from [R3] itself and then copied into protocol.yaml (no row contains them at present): (1) the psi value for each of the three datasets MSN, BMS-WebView-1, TRUCKS - and it must be stated whether psi is an ABSOLUTE threshold (number of transactions/sequences) or a SHARE of |D|, because [R3] counts by number of sequences, not by sup_NO, and with the wrong unit the comparison table is meaningless; (2) the exact formulas of the two units they report - what the denominator of "side-effects (%)" is (the original |F_psi(D)|, or |F_psi(D) \ S+|) and what "distance (%)" is measured on (number of deleted events divided by the total number of events, or a sequence distance), so that a reconciling formula can still be written next to Lost/DistR of §2. The one who reads the original document [R3] decides; it cannot be inferred from the data or from the code. URGENT: blocks SC8, the only scenario that allows the numbers of the paper to be put next to published numbers (design line 402); taking the wrong psi makes the table worthless without reporting any error.

**Role and source of the TRUCKS dataset for SC8**

Choose one of three: (a) keep asking for trucks (from the authors of [R20]/[R3] or from the original trajectory repository) and, if it is obtained, SC8 keeps the three datasets; (b) drop TRUCKS, run SC8 on the two datasets MSNBC and BMS1 and write one sentence in the paper stating clearly that the third dataset could not be obtained, so the numbers of [R20]/[R2] on TRUCKS cannot be placed alongside; (c) replace it with another public discretised trajectory dataset (not BIKE, because BIKE was already excluded on its measured characteristics). The author decides; if (b) is chosen then doc §11.1 line 402 (the description of SC8, which currently promises three datasets) and §11.2 line 417 must also be revised.

> Blocks: Blocks SC8 - this is the only scenario that allows the data of this paper to be put next to published data (research design line 402). Without psi it cannot run, and taking the wrong psi makes the comparison table worthless.

### B8. [R11] Zhao 2025: does it still block Gate 1, does it count by sup_NO

Raised by: `design-gap`, `design-gate`

**Scope of the review and how to state research gaps G1, G5**

Only half remains open: extend the review to the episode line (Laxman [R19] and its relatives) or accept the current scope? Concretely, choose between (a) raising [R19] from SDL to MUC and reviewing at least two more structured sources (ACM DL, DBLP by author 2007–2026), then logging the queries in §4, or (b) keeping the single keyword search and writing G5 with exactly one sentence stating the scope together with the review date. The other half of the original question — write an absolute negation, or a mechanism-based statement with its scope — is considered decided in favour of the second way (§4 line 117, heading G1 line 75, the 'Reading level' column of §4), and only needs to be applied also to the branch with the full text of [R11]. The author decides.

**The conditions attached to contributions 1 and 2, and the fallback when they fail**

Choose ONE condition and ONE option, then revise all three places to match. (1) The condition under which contribution 1 is lost: one clause ('[R11] counts by sup_NO', §7) or the conjunction of two clauses ('counts sup_NO AND reduces partially', §12a line 539)? (2) When the condition fails, which of the three options that currently coexist is chosen: (a) switch to plain HIDE-SEQ, (b) drop the direction, (c) keep the paper with the two remaining layers (Nettree cut index + CUT-HIDE) and evaluate mainly on the old problem in SC8 as §12a line 537 suggests? The author decides; the result must be written back into §7 lines 207–208, §12 line 507 and §12a line 539.

**Does Zhao 2025 [R11] count by sup_NO**

By which count does the third algorithm of [R11] (Zhao, Chen, Zhang, CCPE 2025, doi 10.1002/cpe.70187) measure 'frequent-pattern loss': (i) non-overlapping support of the NOSEP kind [R1], (ii) counting every match, overlapping ones included, or (iii) presence/absence only? A single answer among these three choices is all that is needed to fill the four (?) cells in the [R11] column of the delta table of §6 and to move G1 from 'provisional' to official. It must be taken from the full text of the original paper (buy it, borrow it through CESTI/the university library, or ask for the author's copy) — it cannot be inferred from any file in the project.

**Whether to declare Gate 1 passed while the reading level falls short**

Decide once: whether to activate the reduced-reading-level option (§12a item 3) so as to record 1.1.12 as 'PASSED with constraints' and revise line 7 + line 571 to be consistent with line 546. If yes, also draw up the list of the nine remaining §13 items (1.1.1, 1.1.8, 1.1.9, 1.1.10, 1.1.13, 1.1.14, 1.2.1, 1.2.3, 1.2.7) with an owner and a deadline for each, because they are what actually remains of Gate 1. The author decides. Needed before the gap claim is written into the manuscript, does not block SC3.

**Is [R11] still a blocker of Gate 1**

Set a hard deadline (a specific date, counted from today) for moving to step 3 of §12a: after that deadline, tick 1.1.12 with reading level 'abstract + page 1' for [R11] with the three constraints (a)(b)(c), and revise the sentence in §0 line 7 and the §13 cell at line 571 from 'NOT PASSED / sole blocker' to 'passed conditionally, per §12a step 3'. Unit of the decision: one date + two revised sentences in the design file. The author decides; no file in the project can lock it on the author's behalf.

**Absolute milestones of the 15-week schedule**

Set three absolute milestones and renumber the schedule to match reality: (i) the date T0 from which the 15 weeks are counted; (ii) the date on which waiting for the full text of [R11] expires, i.e. the milestone that automatically activates §12a item 3 (the same as part (ii) of point [1]); (iii) the name of a specific conference together with its submission deadline (the same as the remaining part of point [3]). Plus reordering §12b, because weeks 4–7 were finished before weeks 1–3. The author decides.

> Blocks: G1 and G5 are still classed 'provisional'; the four '(?)' cells in the [R11] column of the delta table of §6 (counting, structure, ghost patterns, hardness); the delta sentence against [R11]; contributions 1 and 2 in §7 still carry a condition; item 1.1.12 of Gate 1 reads NOT PASSED; the tier (b) baseline from [R11] in §11.3 reads 'lock after the full text is available'.

### B9. The state of the research gaps G1, G2, G5 and the scope of the review

Raised by: `design-gap`

**The class of research gap G2 is not in the declared classification**

Which label does G2 receive in the two-valued classification that §3 line 73 itself declares: 'strong' or 'weak'? (A hint for checking: the 'weak' criterion is that the closest work has already touched the idea and this paper only formalises it — and [R20] has already warned about the spurious patterns that REPLACEMENT allows, and [R2] has already proved monotonicity for the trajectory model, so G2 fits the 'weak' description better.) If three classes are to be kept then a sentence defining 'medium', with its criterion, must be added to §3 line 73 and §13 line 562 revised. Unit: one word + (if three classes are kept) one defining sentence. The author decides.

> Blocks: §13 item 1.1.2 currently reads that the strong/weak classification is done, but in fact G2 has not been classified; the sentence claiming gap G2 in the manuscript cannot be written within the proper classification framework.

### B10. The reading level required for [R22] before G5 is claimed

Raised by: `design-gap`

**The reading level required for [R22] before G5 is claimed**

Choose one of three: (a) raise [R22] (Loukides, Gwadera, SDM 2015, doi 10.1137/1.9781611974010.87) to full text before claiming G5, and change the 'Required level' column in the table of §5 line 135 from MUC to TV; (b) keep level MUC but drop the word 'closest' for [R22] in §3 line 94, i.e. state G5 without citing [R22] as the closest reference point; (c) relax the rule of §5 line 123 to 'the closest must reach at least MUC' and state the consequence clearly. Unit: one choice + a consistent revision of §3 line 94, §5 lines 123 and 135, §13 line 573. The author decides; if (a) is chosen, the work of reading the full text comes with it.

> Blocks: G5 cannot be claimed; the cell 'Sanitising an event sequence [R22]' in the matrix of §4 stands only at level TT; item 1.1.14 of Gate 1 still lists [R22] among the remaining work.

### B13. Venue, format template, and where the pseudocode and the long proofs go

Raised by: `design-gate`, `paper`, `phebinh`  (includes items added by the critical pass)

**Where the detailed pseudocode and the long proofs go**

Two questions, the second independent of the first. (1) The six procedures BuildIndex, VitalNodes, WeightedCut, LambdaSweep, Allocate, MultiPattern: print the full pseudocode in the paper, or keep only the skeleton of Algorithm 1 plus a prose specification for each procedure (the minimum that §9.2 requires) and move the pseudocode to an appendix? This can be answered once the real page limit from point [10] is known. (2) Is the code/ repository published with the paper, and where does the online appendix go — a public code repository, a DOI archive such as Zenodo, or a personal page? Note that raw data do not go into git, so the published version must include datasets/provenance.tsv and tools/fetch_datasets.py so that others can rebuild exactly the right files; and if point [10] turns out to mean double-blind review then the link must be the anonymised version.

**Venue and format template not yet chosen**

Choose the name of the venue and a specific submission date. Three things follow immediately, none of which can be decided before: (1) the real page limit of the two-column template, replacing the estimate of 8–10 pages — the content is currently 13 single-column pages ≈ 7.5 two-column pages, so only the real limit shows how much room is left for the tables SC1–SC8 and the figures; (2) whether the review is double-blind — if it is, remove the author names, affiliations and emails from the first page and also anonymise the link to the code repository in point [11]; (3) the deadline for completing the English translation, counted back from the submission deadline; at present there is only the relative milestone "weeks 12-15" in §12b. The only constraint already locked: a venue different from the intended venues of the three papers in the NCS (doctoral) proposal.

**The plan if the group of [R6], [R7] is working on the same problem**

Two steps: (i) send a question to the author group of [R6], [R7] — this has not been done yet and it is what currently blocks item 1.1.10; (ii) lock in advance one of three options in case they answer that they have a manuscript on the same problem: narrow the scope to only the Nettree cut index + CUT-HIDE on the old problem (SC8), collaborate and list them as co-authors, or keep the scope but move to a venue with a different reviewer pool. The author decides (the letter is sent by the author personally). Not as urgent as the items that block SC3.

**Terms for reusing the data, and the statement of data sources**

Decide two things: (a) check and record the reuse conditions of each dataset in provenance.tsv (add a column): the SPMF data are archived copies, not the original sources — MSNBC originally from UCI, BMS-WebView-1 originally from KDD Cup 2000, MT745584 is a GenBank record, BIBLE and LEVIATHAN are discretised texts; must the paper cite the ORIGINAL source or cite SPMF, and may the files be redistributed with the appendix (at present .gitignore blocks raw data, so by default no); (b) whether the paper has a data and ethics statement section — this is a privacy-preserving paper running on users' click traces and on gene sequences, and many venues now require a sentence about the source and about whether the data were already anonymised.

> Blocks: No file in the project mentions a licence or terms of the datasets. If it is only discovered at submission time that a dataset may not be reused or that the original source must be cited, the results tables, already finished, will need their source notes revised, and the data statement section will have to be written in a hurry.

### B15. The source-code licence of the repository at publication

Raised by: `phebinh`  (includes items added by the critical pass)

**Source-code licence and how to cite the public repository**

Decide four things about publishing the code: (a) which licence for the repository (MIT, Apache-2.0, BSD-3, or no licence — no licence means others do NOT have the right to reuse it, contrary to the purpose of reproducibility); (b) add CITATION.cff and an archival DOI (Zenodo), or only leave the live GitHub link, which may change; (c) the repository is already public under an account bearing the exact name of the corresponding author, with a dated commit history — decide whether to set up a separate anonymised copy for the double-blind review round or to accept not submitting to a double-blind venue; (d) whether to post a preprint (arXiv), and if so before or after submission, because this interacts with (c).

> Blocks: No file in the project mentions a licence, copyright or a preprint. The repository is already online: at submission time, a decision to 'anonymise the link' cannot undo the fact that the code is already tied to the author's name, so it must be decided before the venue is chosen, not after.

### B17. The pipeline from the CSV files to the tables and figures of the manuscript

Raised by: `phebinh`  (includes items added by the critical pass)

**The pipeline that generates tables and figures from CSV into the manuscript**

Lock the path of a number from a CSV file to a table in baibao.tex: (a) write a script that generates .tex files (booktabs tables) and figures, placed in code/tools/ or in paper/tools/ (§11.8 says paper/tools/ 'does not exist yet'), written in what (Python + matplotlib, or pgfplots reading the CSV directly), and does the manuscript \input the generated file or copy it by hand; (b) accept copying by hand and state the risk of mismatched numbers clearly. Decide at the same time: labels.tsv has two label columns, Vietnamese and English, so which column does the table script take for the current Vietnamese manuscript, and by which flag is the column switched when translating into English.

> Blocks: There is no tool yet that turns results into tables in the paper. Copying 8 scenarios times many datasets by hand is where silent errors arise, of exactly the kind that ResultSink was built to prevent, and it breaks the promise of labels.tsv that 'one rename leaves no place behind'.

### B19. SC8 missing from the schedule, and what to drop first when time runs short

Raised by: `phebinh`  (includes items added by the critical pass)

**SC8 is not in the schedule, and the order of dropping when time runs short**

Lock two things about the order of work: (a) which week SC8 is in — the schedule of §12b lists SC3–SC7 and omits SC8, while §12a makes SC8 the option that keeps the paper standing if the new-problem part collapses; if SC8 is the fallback leg then it must run EARLY, not last, because it is also the scenario that depends most on reopening the full text of [R3] to get ψ; (b) the order of dropping when time runs out: rank SC4, SC5, SC6, SC7, SC8 by how necessary they are to the paper, and state in advance which scenarios can be dropped while still submitting (for example SC5 and SC7 moved to the online appendix), so that under pressure nothing is dropped by feel.

> Blocks: The schedule lacks a scenario entirely, and that very scenario is the fallback for the whole paper. Without a priority order, only in week 12 will it be known what must be dropped, and at that point whatever is dropped is a decision taken after seeing the results.

### B20. The size limits of the implementation, and how to declare a saturated count

Raised by: `phebinh`  (includes items added by the critical pass)

**The size limit of the implementation and how to declare saturation**

Decide before running SC6 for numbers: (a) whether to rewrite Dinic.dfs as a loop, or to accept and declare a size limit (maximum sequence length and maximum g⁺) above which nothing is run — if declaring a limit is chosen then that limit is exactly the bound of the scalability claim, and it must be stated in the paper rather than letting a stack overflow turn into an empty cell; (b) when PathCounts saturates, how the SC6 table and the weights w(i) are declared: print the isExact flag alongside and exclude saturated instances from the index-size column, or switch to counting on a log scale, or state that w(i) is only a ranking key and not a count — note that a saturated w(i) makes the weighted cut lose the exactness that P6(b) promises.

> Blocks: SC6 is the column that compares the Nettree cut index with HHA, DBSH, SBSH, i.e. the evidence for the second contribution. If saturation or stack overflow happens exactly on the largest instances, then the most interesting part of the table is the part that cannot be printed, and the exactness of the weighted cut is called into question with no answer prepared in the paper.

### B22. SC7 row 4: what "any cut" means

Raised by: `code-todo`

**SC7 row 4: what is "any cut"**

It must be defined in code what "any minimum cut" means, and shown to be DIFFERENT from row 1. Three concrete choices: (a) take the source-side cut returned by ResidualScc without weights (the "natural" cut of Dinic) - but with Weighting.UNIFORM row 1 also yields exactly that cut, so row 4 would coincide with row 1 and SC7 would lose a row; (b) pick a random minimum cut among the minimum cuts, in which case the seed and the number of repetitions for averaging must also be declared, and it must be stated over which space the random choice is made (giving every minimum cut equal probability is hard; usually only "random tie-breaking during traversal" is feasible); (c) drop row 4 and revise line 403 of the design to three rows. If (a) or (b) is chosen then CutHideConfig must get a fifth field, for example CutSelection {WEIGHT_MINIMAL, SOURCE_SIDE, RANDOM_TIE_BREAK} - the current record cannot express it. The author decides; needed before implementing SC7, does not block SC3.

> Blocks: Blocks one of the four rows of SC7. The TODO itself claims that CutHideConfig takes care of row four, but the record has no corresponding field, so the intent is unclear.

---

