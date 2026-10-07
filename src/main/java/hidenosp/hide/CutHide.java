package hidenosp.hide;

import hidenosp.baseline.HidingMethod;
import hidenosp.core.EventSequence;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.index.NettreeCutIndex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * CUT-HIDE, the algorithm of section 9 of the design doc, run on the Nettree cut index.
 *
 * <p>The eight steps of section 9.2 map onto this class as follows.
 *
 * <ol>
 *   <li>Build NT(P, S) for every sensitive pattern and every sequence that carries it:
 *       {@link NettreeCutIndex#add}.</li>
 *   <li>Max flow with unit node capacities, giving F_S: {@link hidenosp.core.flow.NettreeFlow}.</li>
 *   <li>Vital positions of the near-threshold patterns B_eps, by L5.</li>
 *   <li>Position weights w(i), from {@link NettreeCutIndex#positionWeights(double[])}.</li>
 *   <li>Per-sequence cost curves c_S(k): {@link CostCurve}.</li>
 *   <li>Allocation across sequences for HIDE-DB: {@link Allocation}; fixed counts for HIDE-SEQ.</li>
 *   <li>Several sensitive patterns: the greedy ratio rule, with an exact integer program left to
 *       {@link hidenosp.baseline.IlpExact} for the small instances.</li>
 *   <li>Post-condition check: recompute sup_NO of every sensitive pattern on D-prime, so that HF is
 *       zero by construction.</li>
 * </ol>
 *
 * <p>With one sensitive pattern and g- = 0, steps 5 and 6 are exact for any mu(P): the curves come
 * from theorem P8 ({@link FrontierDp}) and the allocation minimises the masks and then the weight.
 * With g- &gt; 0 they are exact for distinct items only (P1, P2, P6(b)), the lambda sweep brackets
 * the weight, and a repeated-item pattern gets the cut of P3. With several patterns the problem is
 * NP-hard (L4a) and this class runs the greedy of step 7.
 */
public final class CutHide implements HidingMethod {

    private final CutHideConfig config;
    private List<Pattern> nearThreshold = Collections.emptyList();
    private double[] nearThresholdFactor = new double[0];
    private double[][] explicitWeights;

    public CutHide(CutHideConfig config) {
        this.config = config;
    }

    @Override
    public String name() {
        return "CUT-HIDE " + config;
    }

    /**
     * Supplies B_eps and its weight factors for step 4. The caller computes
     * factor[Q] = 1 / (sup_NO(Q, D) - sigma + 1) from F_sigma(D), so the index stays free of any
     * database-level bookkeeping. Without this call every position weighs the same, which is the
     * uniform variant of SC7.
     */
    public CutHide withNearThreshold(List<Pattern> patterns, double[] factor) {
        if (patterns.size() != factor.length) {
            throw new IllegalArgumentException("one factor per near-threshold pattern is required");
        }
        this.nearThreshold = List.copyOf(patterns);
        this.nearThresholdFactor = factor.clone();
        return this;
    }

    /**
     * Sets the position weight of every position of every sequence directly, in place of the
     * weights of step 4: for tests against an exhaustive search, and for studies of the weights.
     */
    public CutHide withExplicitWeights(double[][] weights) {
        this.explicitWeights = weights;
        return this;
    }

    /** Runs CUT-HIDE and returns R together with the sanitized database. */
    @Override
    public Sanitization run(HideProblem problem) {
        requireNonEmptyBand(problem);
        Sanitization result = new Sanitization(problem.database()).method("CUT-HIDE " + config);
        if (problem.sensitive().size() == 1) {
            runSinglePattern(problem, problem.sensitive().get(0), result);
        } else {
            runMultiPattern(problem, result);
        }
        if (config.verifyPostcondition()) {
            verify(problem, result);
        }
        return result;
    }

    /**
     * Refuses to run the weighted variant with an empty B_eps.
     *
     * <p>Without this, an empty band would make every position weigh zero and CUT-HIDE would
     * quietly become the uniform-weight variant of SC7 — that is, the method under test would
     * silently turn into the method it is being compared against, and nothing in the output would
     * say so. The band really can come out empty: at epsilon = 0.10 it does on the genome at
     * sigma = 1000, which is why {@code config/protocol.yaml} locks epsilon = 0.5 together with
     * this check.
     */
    private void requireNonEmptyBand(HideProblem problem) {
        if (config.weighting() != CutHideConfig.Weighting.VITAL_NODE || !nearThreshold.isEmpty()) {
            return;
        }
        throw new IllegalStateException("B_eps is empty for this configuration ("
                + problem + ", epsilon = " + problem.epsilon() + "), so the vital-node weights "
                + "would all be zero and this run would silently be the uniform-weight variant. "
                + "Raise epsilon, lower sigma, or select the uniform variant on purpose.");
    }

    /**
     * One sensitive pattern. With mu(P) = 1 the number of masks is forced by P1, so the work is
     * only to choose where they go: the cost curves of P6 per sequence, then the allocation of P7.
     * With g- = 0 the exact curves of P8 replace both, for any mu(P).
     */
    private void runSinglePattern(HideProblem problem, Pattern pattern, Sanitization result) {
        if (config.curves() != CutHideConfig.Curves.LAMBDA_SWEEP && FrontierDp.applies(problem.gap())) {
            runSinglePatternExact(problem, pattern, result);
            return;
        }
        int n = problem.database().size();
        int[] supports = SupportNO.perSequence(pattern, problem.database(), problem.gap());

        List<CostCurve> curves = new ArrayList<>(n);
        for (int s = 0; s < n; s++) {
            EventSequence sequence = problem.database().get(s);
            Nettree nettree = Nettree.build(pattern, sequence, problem.gap());
            double[] weights = positionWeights(problem, sequence, s);
            curves.add(CostCurve.build(nettree, weights, config.refineLambda()));
        }

        int[] counts = plan(problem, supports, curves);
        for (int s = 0; s < n; s++) {
            if (counts[s] == 0) {
                continue;
            }
            CostCurve curve = curves.get(s);
            result.recordCertification(curve.isCertified(counts[s]));
            for (int position : curve.positions(counts[s])) {
                result.mask(s, position);
            }
        }
    }

    /**
     * One sensitive pattern with g- = 0: the exact curve of theorem P8 in every sequence, then the
     * remaining support per sequence (forced r - 1 for HIDE-SEQ, allocated for HIDE-DB), then the
     * positions the curve stores for that support.
     */
    private void runSinglePatternExact(HideProblem problem, Pattern pattern, Sanitization result) {
        if (config.allocation() != CutHideConfig.AllocationRule.DYNAMIC_PROGRAM) {
            throw new IllegalArgumentException("the exact curves of P8 are allocated by a dynamic "
                    + "program only; " + config.allocation() + " is defined for the lambda-sweep "
                    + "curves, so select Curves.LAMBDA_SWEEP for that variant");
        }
        FrontierDp.Order order = config.curves() == CutHideConfig.Curves.FRONTIER_DP_WEIGHT_FIRST
                ? new FrontierDp.Order(1, 0) : new FrontierDp.Order(0, 1);
        Solved solved = solveOrdered(problem, pattern, scaledWeights(problem), order);
        for (int s = 0; s < solved.positions().length; s++) {
            if (solved.positions()[s].length == 0) {
                continue;
            }
            result.recordCertification(true);
            for (int position : solved.positions()[s]) {
                result.mask(s, position);
            }
        }
    }

    /** A solution of one sensitive pattern: its masks, its weight, the positions per sequence. */
    private record Solved(int masks, long weight, int[][] positions) {
    }

    /** One vertex of the convex Pareto front of proposition P10, with its sanitization. */
    public record ParetoVertex(int masks, long weight, Sanitization sanitization) {
    }

    /**
     * The vertices of the convex Pareto front between |R| and the surrogate weight w(R), exactly
     * (proposition P10), for one sensitive pattern with g- = 0, in the weights of this
     * configuration. Sorted by masks: the first vertex is the solution with the fewest masks and
     * then the least weight (the standard CUT-HIDE), the last the solution with the least weight
     * and then the fewest masks. Weights are in the scaled unit of {@link CostCurve#SCALE}.
     */
    public List<ParetoVertex> paretoFront(HideProblem problem) {
        if (problem.sensitive().size() != 1 || !FrontierDp.applies(problem.gap())) {
            throw new IllegalArgumentException("the exact Pareto front needs one sensitive pattern "
                    + "and g- = 0; got " + problem.sensitive().size() + " patterns, gap "
                    + problem.gap());
        }
        requireNonEmptyBand(problem);
        Pattern pattern = problem.sensitive().get(0);
        long[][] scaled = scaledWeights(problem);
        FrontierDp.Curve[] fewest = curves(problem, pattern, scaled, new FrontierDp.Order(0, 1), null);
        FrontierDp.Curve[] lightest = curves(problem, pattern, scaled, new FrontierDp.Order(1, 0), null);
        // A sequence whose two extreme curves agree at every target has, at every target, one
        // point that is best in masks and in weight at once: it is optimal for every order, so
        // its curve is reused for every order below instead of being solved again.
        FrontierDp.Curve[] fixed = new FrontierDp.Curve[fewest.length];
        for (int s = 0; s < fewest.length; s++) {
            if (Arrays.equals(fewest[s].masks(), lightest[s].masks())
                    && Arrays.equals(fewest[s].weight(), lightest[s].weight())) {
                fixed[s] = fewest[s];
            }
        }
        Solved left = allocate(problem, fewest, new FrontierDp.Order(0, 1));
        Solved right = allocate(problem, lightest, new FrontierDp.Order(1, 0));
        List<Solved> vertices = new ArrayList<>();
        vertices.add(left);
        if (right.masks() != left.masks() || right.weight() != left.weight()) {
            between(problem, pattern, scaled, fixed, left, right, vertices);
            vertices.add(right);
        }
        List<ParetoVertex> out = new ArrayList<>(vertices.size());
        for (Solved v : vertices) {
            Sanitization sanitization = new Sanitization(problem.database())
                    .method("CUT-HIDE Pareto vertex " + config);
            for (int s = 0; s < v.positions().length; s++) {
                for (int position : v.positions()[s]) {
                    sanitization.mask(s, position);
                }
            }
            if (sanitization.maskCount() != v.masks()) {
                throw new IllegalStateException("a Pareto vertex claims " + v.masks()
                        + " masks but holds " + sanitization.maskCount());
            }
            if (config.verifyPostcondition()) {
                verify(problem, sanitization);
            }
            out.add(new ParetoVertex(v.masks(), v.weight(), sanitization));
        }
        return out;
    }

    /**
     * Proposition P10(b): the vertices strictly between two adjacent known vertices, found along
     * the normal of the segment that joins them, added in the order of increasing masks.
     */
    private void between(HideProblem problem, Pattern pattern, long[][] scaled,
                         FrontierDp.Curve[] fixed, Solved y1, Solved y2, List<Solved> out) {
        long a = y2.masks() - y1.masks();
        long b = y1.weight() - y2.weight();
        if (a <= 0 || b <= 0) {
            throw new IllegalStateException("adjacent vertices must trade masks for weight: ("
                    + y1.masks() + ", " + y1.weight() + ") and (" + y2.masks() + ", " + y2.weight()
                    + ")");
        }
        FrontierDp.Order normal = new FrontierDp.Order(a, b);
        Solved y = allocate(problem, curves(problem, pattern, scaled, normal, fixed), normal);
        if (normal.scalar(y.masks(), y.weight()) >= normal.scalar(y1.masks(), y1.weight())) {
            return;
        }
        between(problem, pattern, scaled, fixed, y1, y, out);
        out.add(y);
        between(problem, pattern, scaled, fixed, y, y2, out);
    }

    /** The position weights of every sequence, in the scaled unit of {@link CostCurve#SCALE}. */
    private long[][] scaledWeights(HideProblem problem) {
        int n = problem.database().size();
        long[][] scaled = new long[n][];
        for (int s = 0; s < n; s++) {
            EventSequence sequence = problem.database().get(s);
            double[] weights = positionWeights(problem, sequence, s);
            scaled[s] = new long[weights.length];
            for (int i = 0; i < weights.length; i++) {
                scaled[s][i] = Math.round(weights[i] * CostCurve.SCALE);
            }
        }
        return scaled;
    }

    /**
     * One sensitive pattern with g- = 0 under a lexicographic order of (masks, weight): the exact
     * curve of theorem P8 in every sequence, then the remaining support per sequence (forced
     * r - 1 for HIDE-SEQ, allocated for HIDE-DB), then the positions the curves store for it.
     */
    private Solved solveOrdered(HideProblem problem, Pattern pattern, long[][] scaled,
                                FrontierDp.Order order) {
        return allocate(problem, curves(problem, pattern, scaled, order, null), order);
    }

    /** The curve of every sequence under the order; a non-null {@code fixed[s]} is reused. */
    private FrontierDp.Curve[] curves(HideProblem problem, Pattern pattern, long[][] scaled,
                                      FrontierDp.Order order, FrontierDp.Curve[] fixed) {
        int n = problem.database().size();
        FrontierDp.Curve[] curves = new FrontierDp.Curve[n];
        for (int s = 0; s < n; s++) {
            curves[s] = fixed != null && fixed[s] != null ? fixed[s]
                    : FrontierDp.solve(pattern, problem.database().get(s), problem.gap(),
                    scaled[s], order);
        }
        return curves;
    }

    /** The remaining support per sequence under the order, and the positions the curves store. */
    private Solved allocate(HideProblem problem, FrontierDp.Curve[] curves, FrontierDp.Order order) {
        int n = curves.length;
        long total = 0;
        for (FrontierDp.Curve c : curves) {
            total += c.supNO();
        }
        int[] remaining = new int[n];
        for (int s = 0; s < n; s++) {
            remaining[s] = curves[s].supNO();
        }
        if (!problem.isDatabaseMode()) {
            for (int s = 0; s < n; s++) {
                remaining[s] = Math.min(curves[s].supNO(), problem.threshold() - 1);
            }
        } else {
            long needed = total - problem.threshold() + 1;
            if (needed > 0) {
                List<Integer> carrying = new ArrayList<>();
                for (int s = 0; s < n; s++) {
                    if (curves[s].supNO() > 0) {
                        carrying.add(s);
                    }
                }
                int[][] masks = new int[carrying.size()][];
                long[][] weight = new long[carrying.size()][];
                for (int c = 0; c < carrying.size(); c++) {
                    masks[c] = curves[carrying.get(c)].masks();
                    weight[c] = curves[carrying.get(c)].weight();
                }
                int[] chosen = Allocation.solveExact(masks, weight, (int) needed, order);
                for (int c = 0; c < carrying.size(); c++) {
                    remaining[carrying.get(c)] = chosen[c];
                }
            }
        }
        int totalMasks = 0;
        long totalWeight = 0;
        int[][] positions = new int[n][];
        for (int s = 0; s < n; s++) {
            if (remaining[s] >= curves[s].supNO()) {
                positions[s] = new int[0];
                continue;
            }
            positions[s] = curves[s].positions()[remaining[s]];
            totalMasks += curves[s].masks()[remaining[s]];
            totalWeight += curves[s].weight()[remaining[s]];
        }
        return new Solved(totalMasks, totalWeight, positions);
    }

    /** The masks per sequence: the allocation of P7 for HIDE-DB, the forced counts for HIDE-SEQ. */
    private int[] plan(HideProblem problem, int[] supports, List<CostCurve> curves) {
        int n = supports.length;
        int[] counts = new int[n];
        if (!problem.isDatabaseMode()) {
            for (int s = 0; s < n; s++) {
                counts[s] = Math.max(0, supports[s] - problem.threshold() + 1);
            }
            return counts;
        }
        long total = 0;
        for (int f : supports) {
            total += f;
        }
        long needed = total - problem.threshold() + 1;
        if (needed <= 0) {
            return counts;
        }
        Allocation.Result allocation = config.allocation() == CutHideConfig.AllocationRule.DYNAMIC_PROGRAM
                ? Allocation.solve(curves, (int) needed)
                : Allocation.greedy(curves, (int) needed);
        return allocation.counts();
    }

    /**
     * Several sensitive patterns, step 7: repeatedly mask the position with the largest ratio of
     * total drop in sensitive support to 1 + w(i), recomputing after each mask on the index. The
     * problem is NP-hard by L4a, so this is a heuristic and SC3 measures its distance to the exact
     * integer program on the small instances.
     */
    private void runMultiPattern(HideProblem problem, Sanitization result) {
        if (problem.isDatabaseMode()) {
            runMultiPatternDatabase(problem, result);
        } else {
            runMultiPatternPerSequence(problem, result);
        }
    }

    /** The best position of one sequence together with what masking it would buy. */
    private record Candidate(int sequence, int position, double weight, int[] drop) {

        double ratio(long[] needed) {
            int gain = 0;
            for (int p = 0; p < drop.length; p++) {
                if (needed[p] > 0) {
                    gain += drop[p];
                }
            }
            return gain / (1 + weight);
        }
    }

    /**
     * HIDE-DB with several sensitive patterns: step 7 of section 9.2, run over the whole database.
     *
     * <p>The database threshold couples the sequences. A mask placed anywhere counts against the
     * same global gap, so the choice cannot be made one sequence at a time; doing that over-masks,
     * because each sequence would drive its own support down without knowing what the others
     * already removed. Each round therefore takes the position with the largest ratio of total drop
     * in sensitive support to 1 + w(i) across all of D, then recomputes.
     *
     * <p>Only the sequence just masked changes, so only its candidate is rebuilt and the rest are
     * reused. They do have to be dropped when a pattern becomes satisfied, because the best position
     * inside a sequence depends on which patterns still count — though not on how much they still
     * need, so that happens at most once per sensitive pattern.
     *
     * <p>The weights w(i) are computed once per sequence, on the unmasked sequence. Step 4 describes
     * them that way, and the design doc already declares the weight an approximation because the
     * effect of several masks does not add up.
     *
     * <p>This is a heuristic: the problem is NP-hard by L4a. SC3 measures its distance to the exact
     * solver on the small instances.
     */
    private void runMultiPatternDatabase(HideProblem problem, Sanitization result) {
        int patternCount = problem.sensitive().size();
        int sequenceCount = problem.database().size();

        long[] needed = new long[patternCount];
        for (int p = 0; p < patternCount; p++) {
            long total = SupportNO.of(problem.sensitive().get(p), problem.database(), problem.gap());
            needed[p] = Math.max(0, total - problem.threshold() + 1);
        }
        if (allSatisfied(needed)) {
            return;
        }

        List<Integer> carrying = new ArrayList<>();
        for (int s = 0; s < sequenceCount; s++) {
            for (Pattern p : problem.sensitive()) {
                if (SupportNO.of(p, problem.database().get(s), problem.gap()) > 0) {
                    carrying.add(s);
                    break;
                }
            }
        }

        Map<Integer, Set<Integer>> masked = new HashMap<>();
        Map<Integer, double[]> weights = new HashMap<>();
        Candidate[] best = new Candidate[sequenceCount];
        for (int s : carrying) {
            best[s] = bestInSequence(problem, s, masked, weights, needed);
        }
        boolean[] satisfied = satisfiedFlags(needed);

        while (!allSatisfied(needed)) {
            Candidate pick = null;
            double pickRatio = 0;
            for (int s : carrying) {
                Candidate c = best[s];
                if (c == null) {
                    continue;
                }
                double ratio = c.ratio(needed);
                if (ratio > pickRatio) {
                    pickRatio = ratio;
                    pick = c;
                }
            }
            if (pick == null) {
                // Cannot happen while the ledger is exact: a pattern that still counts has a
                // positive support in some sequence, and by L0 (Menger on the Nettree) any node of
                // a least vertex cut there is vital, so that sequence offers a position.
                throw new IllegalStateException("no position lowers a sensitive support any "
                        + "further, yet the database threshold is still exceeded: still needed "
                        + Arrays.toString(needed) + ", threshold " + problem.threshold());
            }
            // The drop is measured, not taken from the candidate. A vital position lowers its
            // pattern by exactly one only when mu(P) = 1: with repeated items one position carries
            // several nodes, and masking it can lower a support by more than one. Counting one
            // there left the ledger above the true support, so the greedy went on masking patterns
            // already hidden and then stopped with nothing left to lower (the official SC3 of
            // |S| = 5 stopped this way on repeated-item cells).
            int[] drop = measuredDrop(problem, pick.sequence(),
                    masked.getOrDefault(pick.sequence(), Set.of()), pick.position(), needed);
            masked.computeIfAbsent(pick.sequence(), k -> new TreeSet<>()).add(pick.position());
            result.mask(pick.sequence(), pick.position());
            for (int p = 0; p < patternCount; p++) {
                needed[p] = Math.max(0, needed[p] - drop[p]);
            }
            best[pick.sequence()] = bestInSequence(problem, pick.sequence(), masked, weights, needed);

            boolean[] now = satisfiedFlags(needed);
            if (!Arrays.equals(satisfied, now)) {
                // The set of patterns that still count changed, so every cached choice may be stale.
                satisfied = now;
                for (int s : carrying) {
                    if (s != pick.sequence()) {
                        best[s] = bestInSequence(problem, s, masked, weights, needed);
                    }
                }
            }
        }
    }

    /**
     * The drop in sup_NO of every pattern that still counts, in one sequence, when one more position
     * is masked on top of those already masked there; 0 for the patterns that no longer count.
     */
    private static int[] measuredDrop(HideProblem problem, int sequenceIndex, Set<Integer> already,
                                      int position, long[] needed) {
        EventSequence original = problem.database().get(sequenceIndex);
        EventSequence before = already.isEmpty() ? original : original.mask(already);
        EventSequence after = before.mask(position);
        int[] drop = new int[needed.length];
        for (int p = 0; p < needed.length; p++) {
            if (needed[p] > 0) {
                Pattern pattern = problem.sensitive().get(p);
                drop[p] = SupportNO.of(pattern, before, problem.gap())
                        - SupportNO.of(pattern, after, problem.gap());
            }
        }
        return drop;
    }

    /** Rebuilds one sequence's index over its current mask and returns its best position. */
    private Candidate bestInSequence(HideProblem problem, int sequenceIndex,
                                     Map<Integer, Set<Integer>> masked,
                                     Map<Integer, double[]> weights, long[] needed) {
        EventSequence original = problem.database().get(sequenceIndex);
        Set<Integer> here = masked.getOrDefault(sequenceIndex, Set.of());
        EventSequence sequence = here.isEmpty() ? original : original.mask(here);
        NettreeCutIndex index = new NettreeCutIndex(sequence, problem.gap());
        for (Pattern p : problem.sensitive()) {
            index.add(p, true);
        }
        double[] w = weights.computeIfAbsent(sequenceIndex,
                k -> positionWeights(problem, original, k));

        int patternCount = problem.sensitive().size();
        int[][] dropAt = new int[original.length()][];
        for (int p = 0; p < patternCount; p++) {
            if (needed[p] == 0) {
                continue;
            }
            for (int position : index.vitalPositions(p)) {
                if (dropAt[position] == null) {
                    dropAt[position] = new int[patternCount];
                }
                dropAt[position][p] = 1;
            }
        }

        Candidate best = null;
        double bestRatio = 0;
        for (int i = 0; i < original.length(); i++) {
            if (dropAt[i] == null || here.contains(i)) {
                continue;
            }
            Candidate candidate = new Candidate(sequenceIndex, i, w[i], dropAt[i]);
            double ratio = candidate.ratio(needed);
            if (ratio > bestRatio) {
                bestRatio = ratio;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean[] satisfiedFlags(long[] needed) {
        boolean[] flags = new boolean[needed.length];
        for (int p = 0; p < needed.length; p++) {
            flags[p] = needed[p] == 0;
        }
        return flags;
    }

    private static boolean allSatisfied(long[] needed) {
        for (long value : needed) {
            if (value > 0) {
                return false;
            }
        }
        return true;
    }

    /** HIDE-SEQ with several patterns: the sequences are independent, so one loop each. */
    private void runMultiPatternPerSequence(HideProblem problem, Sanitization result) {
        for (int s = 0; s < problem.database().size(); s++) {
            EventSequence sequence = problem.database().get(s);
            NettreeCutIndex index = buildIndex(problem, sequence);
            while (needsMask(index, problem)) {
                double[] weights = indexWeights(index, problem);
                double[] drop = sensitiveDropPerPosition(index, sequence.length());
                int target = -1;
                double bestRatio = 0;
                for (int i = 0; i < sequence.length(); i++) {
                    if (index.isMasked(i) || drop[i] <= 0) {
                        continue;
                    }
                    double ratio = drop[i] / (1 + weights[i]);
                    if (ratio > bestRatio) {
                        bestRatio = ratio;
                        target = i;
                    }
                }
                if (target < 0) {
                    throw new IllegalStateException("no position lowers a sensitive support in "
                            + "sequence " + s + ", yet the limit is still exceeded");
                }
                index.maskPosition(target);
                result.mask(s, target);
            }
        }
    }

    /** True while some sensitive pattern still exceeds the per-sequence limit r of HIDE-SEQ. */
    private boolean needsMask(NettreeCutIndex index, HideProblem problem) {
        for (int id = 0; id < index.patternCount(); id++) {
            if (index.isSensitive(id) && index.supNO(id) >= problem.threshold()) {
                return true;
            }
        }
        return false;
    }

    /**
     * How much total sensitive support each position would remove, by L5: one pass over the vital
     * sets of the sensitive patterns, not one flow per candidate position.
     */
    private double[] sensitiveDropPerPosition(NettreeCutIndex index, int length) {
        double[] drop = new double[length];
        for (int id = 0; id < index.patternCount(); id++) {
            if (!index.isSensitive(id)) {
                continue;
            }
            for (int position : index.vitalPositions(id)) {
                drop[position] += 1;
            }
        }
        return drop;
    }

    private NettreeCutIndex buildIndex(HideProblem problem, EventSequence sequence) {
        NettreeCutIndex index = new NettreeCutIndex(sequence, problem.gap());
        for (Pattern p : problem.sensitive()) {
            index.add(p, true);
        }
        for (Pattern q : nearThreshold) {
            index.add(q, false);
        }
        return index;
    }

    private double[] indexWeights(NettreeCutIndex index, HideProblem problem) {
        if (config.weighting() == CutHideConfig.Weighting.UNIFORM) {
            return new double[index.sequence().length()];
        }
        double[] factor = new double[index.patternCount()];
        int offset = problem.sensitive().size();
        for (int k = 0; k < nearThresholdFactor.length; k++) {
            factor[offset + k] = nearThresholdFactor[k];
        }
        return index.positionWeights(factor);
    }

    /** Position weights for one sequence, built from a throw-away index over B_eps only. */
    private double[] positionWeights(HideProblem problem, EventSequence sequence, int sequenceIndex) {
        if (explicitWeights != null) {
            return explicitWeights[sequenceIndex];
        }
        if (config.weighting() == CutHideConfig.Weighting.UNIFORM) {
            return new double[sequence.length()];
        }
        NettreeCutIndex index = new NettreeCutIndex(sequence, problem.gap());
        double[] factor = new double[nearThreshold.size()];
        for (int k = 0; k < nearThreshold.size(); k++) {
            index.add(nearThreshold.get(k), false);
            factor[k] = nearThresholdFactor[k];
        }
        return index.positionWeights(factor);
    }

    /**
     * Step 8: recompute sup_NO of every sensitive pattern on D-prime. HF is zero by design, so a
     * failure here is a bug, not a result; the scenarios report it as such.
     */
    private void verify(HideProblem problem, Sanitization result) {
        Map<Integer, java.util.Set<Integer>> masked = result.masked();
        var sanitized = problem.database().mask(masked);
        for (Pattern p : problem.sensitive()) {
            if (problem.isDatabaseMode()) {
                long support = SupportNO.of(p, sanitized, problem.gap());
                if (support >= problem.threshold()) {
                    throw new IllegalStateException("post-condition failed for " + p
                            + ": sup_NO(P, D-prime) = " + support + " >= sigma = " + problem.threshold());
                }
            } else {
                for (int s = 0; s < sanitized.size(); s++) {
                    int support = SupportNO.of(p, sanitized.get(s), problem.gap());
                    if (support >= problem.threshold()) {
                        throw new IllegalStateException("post-condition failed for " + p
                                + " in sequence " + s + ": sup_NO = " + support
                                + " >= r = " + problem.threshold());
                    }
                }
            }
        }
    }
}
