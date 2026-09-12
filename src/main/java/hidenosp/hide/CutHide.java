package hidenosp.hide;

import hidenosp.baseline.HidingMethod;
import hidenosp.core.EventSequence;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.index.NettreeCutIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
 * <p>Steps 5 and 6 are exact only for one sensitive pattern with distinct items (P1, P2, P6(b));
 * with several patterns the problem is NP-hard (L4a) and this class runs the greedy of step 7.
 */
public final class CutHide implements HidingMethod {

    private final CutHideConfig config;
    private List<Pattern> nearThreshold = Collections.emptyList();
    private double[] nearThresholdFactor = new double[0];

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

    /** Runs CUT-HIDE and returns R together with the sanitized database. */
    @Override
    public Sanitization run(HideProblem problem) {
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
     * One sensitive pattern. With mu(P) = 1 the number of masks is forced by P1, so the work is
     * only to choose where they go: the cost curves of P6 per sequence, then the allocation of P7.
     */
    private void runSinglePattern(HideProblem problem, Pattern pattern, Sanitization result) {
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
            // TODO(code): the database threshold couples the sequences, so a per-sequence loop
            // over-masks. Generalise the allocation of P7 to several patterns: build the joint cost
            // curves, then either the linear-programming rounding with its l_max guarantee or the
            // hybrid genetic algorithm seeded from the cuts (section 7 of the design doc).
            throw new UnsupportedOperationException(
                    "CUT-HIDE with several sensitive patterns under a database threshold is not "
                            + "implemented yet; see section 9.2 step 7");
        }
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
        if (config.weighting() == CutHideConfig.Weighting.UNIFORM || nearThreshold.isEmpty()) {
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
