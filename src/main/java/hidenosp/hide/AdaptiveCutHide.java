package hidenosp.hide;

import hidenosp.baseline.HidingMethod;
import hidenosp.core.EventSequence;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.index.NettreeCutIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CUT-HIDE with an adaptive side-effect weight: the position weight of step 4 is recomputed from
 * the exact drop in support that the previous solution causes, and the solution that loses the
 * fewest patterns is kept.
 *
 * <p>Iteration 0 is the standard CUT-HIDE: vital positions of the near-threshold band B_eps with
 * factor 1 / m_Q. Every later iteration solves the exact program of theorem P8 again, masks first,
 * so every solution masks |R|* positions; only the weight changes. For every non-sensitive frequent
 * pattern Q, with m_Q = sup_NO(Q, D) - sigma + 1 the drop that loses Q and d_Q its exact drop under
 * the previous solution,
 * <pre>
 *   c_Q = 1 / m_Q + s (1 - s) / tau,   s = 1 / (1 + exp(-(d_Q - m_Q + 1/2) / tau)),   tau = max(1, m_Q / 4)
 * </pre>
 * and w(i) = sum of c_Q over the patterns Q for which position i is vital. The second term is the
 * derivative of a smoothed step at the current drop: patterns near their margin weigh most, patterns
 * kept safely or lost whatever happens weigh least. The band is not used after iteration 0, so a
 * pattern far above sigma still gets weight once its real drop comes close to m_Q.
 *
 * <p>Guarantees: every solution has |R| = |R|*, and the result never loses more patterns than
 * iteration 0, the standard CUT-HIDE. No fixed additive weight can do better in general: finding
 * the fewest lost patterns among the minimum-mask solutions is NP-hard even for one sensitive
 * pattern of two distinct items (a reduction from Max-Cut, checked in {@code MaxCutReductionTest}),
 * because a pattern can be lost only by two masks together.
 *
 * <p>Limit, measured: the re-weighting changes every sequence at once. On the Max-Cut instances all
 * vertices see the same signal, so the solution flips between "mask a everywhere" and "mask b
 * everywhere" and never improves on CUT-HIDE there ({@code AdaptiveCutHideTest}); breaking that
 * symmetry needs moves that change one sequence at a time. {@link #withLocalSearch} adds them as a
 * second stage ({@link MaskLocalSearch}), started from the best re-weighted solution: the
 * re-weighting moves many sequences together, which the deep losses of large supports need, and the
 * local search moves one or two sequences at a time, which pairs of masks and small supports need.
 *
 * <p>The drops come from the caller's evaluator, the same code that counts Lost in the result files,
 * so the method and the metric cannot disagree about a drop. Only one sensitive pattern, HIDE-DB and
 * g- = 0 (where theorem P8 is exact) are accepted; anything else is refused, not approximated.
 */
public final class AdaptiveCutHide implements HidingMethod {

    /** The exact drop in sup_NO over the whole database, per pattern, that a sanitization causes. */
    public interface DropOracle {
        Map<Pattern, Long> drops(Sanitization sanitization);
    }

    /** The number of solutions tried by default, iteration 0 included. */
    public static final int DEFAULT_ITERATIONS = 6;

    private final List<Pattern> band;
    private final double[] bandFactor;
    private final Map<Pattern, Long> watched;
    private final long sigma;
    private final int iterations;
    private final DropOracle oracle;
    private int[] lostPerIteration = new int[0];
    private int chosen = -1;
    private int searchPasses = -1;
    private int searchReceivers = -1;
    private MaskLocalSearch.Result searchResult;

    /**
     * @param band B_eps and its factors 1 / m_Q, exactly as given to the standard CUT-HIDE
     * @param watched every frequent pattern outside the closure of the sensitive pattern, with its
     *     support sup_NO(Q, D): the patterns Lost counts. Its iteration order fixes the order in
     *     which the weights are summed, so it must be deterministic (the order of F_sigma(D)) for
     *     the floating-point sums, and hence the solutions, to be reproducible
     * @param sigma the frequency threshold of Lost
     * @param iterations solutions tried, iteration 0 included; at least 1
     * @param oracle the exact drops of a sanitization, from the evaluator of the scenario
     */
    public AdaptiveCutHide(List<Pattern> band, double[] bandFactor, Map<Pattern, Long> watched,
                           long sigma, int iterations, DropOracle oracle) {
        if (band.size() != bandFactor.length) {
            throw new IllegalArgumentException("one factor per band pattern is required");
        }
        if (iterations < 1) {
            throw new IllegalArgumentException("at least one iteration is required, got " + iterations);
        }
        this.band = List.copyOf(band);
        this.bandFactor = bandFactor.clone();
        this.watched = new LinkedHashMap<>(watched);
        this.sigma = sigma;
        this.iterations = iterations;
        this.oracle = oracle;
    }

    /**
     * Adds the second stage: the local search of {@link MaskLocalSearch} from the best re-weighted
     * solution, with the saving condition of lemma L9. It never loses more patterns than its start.
     */
    public AdaptiveCutHide withLocalSearch(int maxPasses, int receivers) {
        if (maxPasses < 1 || receivers < 1) {
            throw new IllegalArgumentException("passes and receivers must be at least 1, got "
                    + maxPasses + " and " + receivers);
        }
        this.searchPasses = maxPasses;
        this.searchReceivers = receivers;
        return this;
    }

    @Override
    public String name() {
        return "CUT-HIDE adaptive weight (" + iterations + " iterations)"
                + (searchPasses > 0 ? " + local search (" + searchPasses + " passes, "
                + searchReceivers + " receivers)" : "");
    }

    /** The local search of the last run, or null when it is off. */
    public MaskLocalSearch.Result searchResult() {
        return searchResult;
    }

    /** Lost of every iteration of the last run, iteration 0 first. */
    public int[] lostPerIteration() {
        return lostPerIteration.clone();
    }

    /** The iteration whose solution the last run returned (the earliest among ties). */
    public int chosenIteration() {
        return chosen;
    }

    @Override
    public Sanitization run(HideProblem problem) {
        if (problem.sensitive().size() != 1 || !problem.isDatabaseMode()
                || !FrontierDp.applies(problem.gap())) {
            throw new IllegalArgumentException("the adaptive weight needs one sensitive pattern, "
                    + "HIDE-DB and g- = 0, where theorem P8 is exact; got " + problem.sensitive().size()
                    + " pattern(s), mode " + problem.mode() + ", gap " + problem.gap());
        }
        Pattern p = problem.sensitive().get(0);
        int n = problem.database().size();
        lostPerIteration = new int[iterations];
        searchResult = null;

        Sanitization best = new CutHide(CutHideConfig.standard())
                .withNearThreshold(band, bandFactor).run(problem);
        Map<Pattern, Long> drop = oracle.drops(best);
        int bestLost = lost(drop);
        lostPerIteration[0] = bestLost;
        chosen = 0;
        if (iterations == 1 && searchPasses < 0) {
            return best.method(name());
        }

        // Vital positions of every watched pattern in every sequence that carries P: masks go
        // nowhere else, so no other position needs a weight.
        List<Map<Pattern, int[]>> vital = new ArrayList<>(n);
        Map<Integer, Map<Pattern, int[]>> vitalBySequence = new LinkedHashMap<>();
        for (int s = 0; s < n; s++) {
            EventSequence sequence = problem.database().get(s);
            if (SupportNO.of(p, sequence, problem.gap()) == 0) {
                vital.add(null);
                continue;
            }
            NettreeCutIndex index = new NettreeCutIndex(sequence, problem.gap());
            Map<Pattern, int[]> here = new LinkedHashMap<>();
            for (Pattern q : watched.keySet()) {
                int id = index.add(q, false);
                if (index.supNO(id) > 0) {
                    here.put(q, index.vitalPositions(id));
                }
            }
            vital.add(here);
            vitalBySequence.put(s, here);
        }

        for (int k = 1; k < iterations; k++) {
            Map<Pattern, Double> factor = factors(drop);
            double[][] weights = new double[n][];
            for (int s = 0; s < n; s++) {
                weights[s] = new double[problem.database().get(s).length()];
                if (vital.get(s) == null) {
                    continue;
                }
                for (Map.Entry<Pattern, int[]> e : vital.get(s).entrySet()) {
                    double c = factor.get(e.getKey());
                    for (int position : e.getValue()) {
                        weights[s][position] += c;
                    }
                }
            }
            Sanitization candidate = new CutHide(CutHideConfig.uniformWeights())
                    .withExplicitWeights(weights).run(problem);
            if (candidate.maskCount() != best.maskCount()) {
                throw new IllegalStateException("iteration " + k + " masked " + candidate.maskCount()
                        + " positions, iteration 0 masked " + best.maskCount()
                        + "; both solve the same program masks first, so this is a bug");
            }
            drop = oracle.drops(candidate);
            int lost = lost(drop);
            lostPerIteration[k] = lost;
            if (lost < bestLost) {
                bestLost = lost;
                best = candidate;
                chosen = k;
            }
        }
        if (searchPasses < 0) {
            return best.method(name());
        }

        searchResult = new MaskLocalSearch(problem, watched, sigma, vitalBySequence, oracle,
                searchPasses, searchReceivers).run(best);
        Sanitization searched = new Sanitization(problem.database());
        for (Map.Entry<Integer, Set<Integer>> e : searchResult.masks().entrySet()) {
            for (int position : e.getValue()) {
                searched.mask(e.getKey(), position);
            }
        }
        // Step 8 again: the moves keep the support of P below sigma by construction; check it.
        long left = SupportNO.of(p, problem.database().mask(searchResult.masks()), problem.gap());
        if (left >= problem.threshold()) {
            throw new IllegalStateException("the local search left sup_NO(P) = " + left
                    + ", not below " + problem.threshold() + "; this is a bug");
        }
        // The search counts Lost incrementally; the evaluator must agree, or the search is wrong.
        int checked = lost(oracle.drops(searched));
        if (checked != searchResult.lost() || searched.maskCount() != best.maskCount()
                || searchResult.lost() > bestLost) {
            throw new IllegalStateException("local search inconsistent: it counted "
                    + searchResult.lost() + " lost, the evaluator " + checked + ", start " + bestLost
                    + "; |R| " + searched.maskCount() + " against " + best.maskCount());
        }
        return searched.method(name());
    }

    /** The patterns of {@code watched} whose support falls below sigma under these drops. */
    private int lost(Map<Pattern, Long> drop) {
        int lost = 0;
        for (Map.Entry<Pattern, Long> e : watched.entrySet()) {
            if (e.getValue() - drop.getOrDefault(e.getKey(), 0L) < sigma) {
                lost++;
            }
        }
        return lost;
    }

    /** c_Q for every watched pattern, from its exact drop under the previous solution. */
    private Map<Pattern, Double> factors(Map<Pattern, Long> drop) {
        Map<Pattern, Double> factor = new HashMap<>();
        for (Map.Entry<Pattern, Long> e : watched.entrySet()) {
            double margin = e.getValue() - sigma + 1;
            double tau = Math.max(1.0, 0.25 * margin);
            double s = 1.0 / (1.0 + Math.exp(-(drop.getOrDefault(e.getKey(), 0L) - margin + 0.5) / tau));
            factor.put(e.getKey(), 1.0 / margin + s * (1 - s) / tau);
        }
        return factor;
    }
}
