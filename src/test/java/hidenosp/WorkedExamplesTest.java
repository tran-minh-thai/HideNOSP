// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.core.flow.NettreeFlow;
import hidenosp.core.SequenceDatabase;
import hidenosp.hide.AdaptiveCutHide;
import hidenosp.hide.CostCurve;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.FrontierDp;
import hidenosp.hide.HideProblem;
import hidenosp.hide.MaskLocalSearch;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The worked examples of the design doc, pinned as tests. They are the examples the paper prints,
 * so a change that breaks one of them breaks the paper, not only the code.
 *
 * <p>Positions are written 1-based in the design doc and 0-based in the code; each test states the
 * conversion where it matters.
 */
class WorkedExamplesTest {

    private static final int A = 0;
    private static final int B = 1;
    private static final int C = 2;

    private static EventSequence sequence(int... events) {
        return new EventSequence(0, events);
    }

    @Test
    @DisplayName("G3 running example: S = abaababaa, P = ab, gap [0,2]")
    void runningExample() {
        EventSequence s = sequence(A, B, A, A, B, A, B, A, A);
        Pattern p = new Pattern(A, B);
        GapConstraint gap = new GapConstraint(0, 2);

        assertEquals(3, SupportNO.of(p, s, gap), "sup_NO of the running example");

        Nettree nettree = Nettree.build(p, s, gap);
        NettreeFlow flow = new NettreeFlow(nettree);
        // The design doc lists positions 1, 2, 5 and 7 as the ones whose masking lowers sup_NO.
        assertArrayEquals(new int[] {0, 1, 4, 6}, flow.vitalPositions(),
                "vital positions, 0-based");

        // Position 4 of the doc, that is index 3, lies on two occurrences yet is not vital.
        assertEquals(3, SupportNO.of(p, s.mask(3), gap), "masking position 4 does not lower sup_NO");
        assertEquals(2, SupportNO.of(p, s.mask(0), gap), "masking position 1 lowers sup_NO");
    }

    @Test
    @DisplayName("Corollary P1c: S = ababcbc, P = abc, gap [0,3] needs two masks, not three")
    void heuristicOverspends() {
        EventSequence s = sequence(A, B, A, B, C, B, C);
        Pattern p = new Pattern(A, B, C);
        GapConstraint gap = new GapConstraint(0, 3);

        assertEquals(2, SupportNO.of(p, s, gap), "the optimum number of masks is sup_NO");

        // The local rule of [R20] masks position 4 of the doc, index 3, because it lies on four
        // matches; two more masks are then needed, three in total.
        EventSequence afterHeuristicFirstMask = s.mask(3);
        assertEquals(2, SupportNO.of(p, afterHeuristicFirstMask, gap),
                "the position on the most matches is not vital here");

        // Masking the two occurrences of c, positions 5 and 7 of the doc, suffices.
        EventSequence exact = s.mask(4).mask(6);
        assertEquals(0, SupportNO.of(p, exact, gap), "two masks are enough");
    }

    @Test
    @DisplayName("P6(c): S = babbacbcaacb, P = ca, gap [0,2], the weighted cuts")
    void weightedCuts() {
        // b a b b a c b c a a c b, 1-based; c sits at 6, 8, 11 and a at 2, 5, 9, 10.
        EventSequence s = sequence(B, A, B, B, A, C, B, C, A, A, C, B);
        Pattern p = new Pattern(C, A);
        GapConstraint gap = new GapConstraint(0, 2);

        assertEquals(2, SupportNO.of(p, s, gap), "F of the counterexample");

        double[] weight = new double[s.length()];
        weight[5] = 3;    // c at position 6
        weight[7] = 50;   // c at position 8
        weight[8] = 100;  // a at position 9
        weight[9] = 1;    // a at position 10

        CostCurve curve = CostCurve.build(Nettree.build(p, s, gap), weight, true);
        assertEquals(2, curve.supNO());
        assertEquals(53 * CostCurve.SCALE, curve.cost(2),
                "k = F is exact by P6(b): the cut {c6, c8} weighs 53");
        assertEquals(CostCurve.SCALE, curve.cost(1),
                "the optimum at k = 1 is 1, masking a10; H1 would pay 3");
        assertArrayEquals(new int[] {9}, curve.positions(1), "a10 is index 9");
        assertTrue(curve.isCertified(1) && curve.isCertified(2),
                "both costs are certified optimal on this instance");
    }

    @Test
    @DisplayName("P7: the cost curve of S = babcaabbababc, P = ab, gap [1,5] is not convex")
    void nonConvexCostCurve() {
        // b a b c a a b b a b a b c, 1-based.
        EventSequence s = sequence(B, A, B, C, A, A, B, B, A, B, A, B, C);
        Pattern p = new Pattern(A, B);
        GapConstraint gap = new GapConstraint(1, 5);

        double[] weight = new double[s.length()];
        weight[1] = 100;  // a at 2
        weight[4] = 3;    // a at 5
        weight[5] = 50;   // a at 6
        weight[8] = 1;    // a at 9
        weight[10] = 2;   // a at 11
        weight[0] = 2;    // b at 1
        weight[2] = 1;    // b at 3
        weight[6] = 3;    // b at 7
        weight[7] = 3;    // b at 8
        weight[9] = 3;    // b at 10
        weight[11] = 3;   // b at 12

        CostCurve curve = CostCurve.build(Nettree.build(p, s, gap), weight, true);
        assertEquals(4, curve.supNO(), "F of the non-convexity example");
        long[] expected = {0, 1, 4, 9, 12};
        for (int k = 0; k <= 4; k++) {
            assertEquals(expected[k] * CostCurve.SCALE, curve.cost(k),
                    "c(" + k + ") of the design doc");
        }
        assertFalse(curve.isConvex(),
                "c = (0, 1, 4, 9, 12) has increments 1, 3, 5, 3, so it is not convex");
        // The one pair the sweep cannot certify here: LB = 8 against UB = 9 at k = 3. The share of
        // certified pairs is a quantity the paper reports, so the gap is pinned rather than hidden.
        assertFalse(curve.isCertified(3), "k = 3 is the uncertified pair of this instance");
        assertEquals(8 * CostCurve.SCALE, curve.lowerBound(3), "the lower bound at k = 3");
    }

    // ---------------------------------------------------------------- the example database

    private static final int D = 3;

    /** The five-sequence example database: abaababaa, aaaab, acccbab, baabbab, cdcda. */
    private static SequenceDatabase exampleDatabase() {
        int[][] rows = {{A, B, A, A, B, A, B, A, A}, {A, A, A, A, B}, {A, C, C, C, B, A, B},
                        {B, A, A, B, B, A, B}, {C, D, C, D, A}};
        List<EventSequence> sequences = new ArrayList<>();
        for (int s = 0; s < rows.length; s++) {
            sequences.add(new EventSequence(s, rows[s]));
        }
        return new SequenceDatabase(sequences);
    }

    /** Masks per sequence, both 1-based, as the paper writes them. */
    private static Map<Integer, Set<Integer>> oneBased(Map<Integer, Set<Integer>> masked) {
        Map<Integer, Set<Integer>> out = new TreeMap<>();
        for (Map.Entry<Integer, Set<Integer>> e : masked.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }
            Set<Integer> positions = new TreeSet<>();
            for (int i : e.getValue()) {
                positions.add(i + 1);
            }
            out.put(e.getKey() + 1, positions);
        }
        return out;
    }

    private static Map<Pattern, Long> watched(Map<Pattern, Long> frequent, Set<Pattern> closure) {
        Map<Pattern, Long> out = new LinkedHashMap<>();
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            if (!closure.contains(e.getKey())) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    private static int lost(Map<Pattern, Long> drop, Map<Pattern, Long> watched, long sigma) {
        int lost = 0;
        for (Map.Entry<Pattern, Long> e : watched.entrySet()) {
            if (e.getValue() - drop.getOrDefault(e.getKey(), 0L) < sigma) {
                lost++;
            }
        }
        return lost;
    }

    @Test
    @DisplayName("ExactCurve: P = aa on aaaab, gap [0,2]: OPT(t) = 3, 2, 1, 0; {2,3,4} is one optimal set for t = 0")
    void exactCurveOnRepeatedItems() {
        EventSequence s = sequence(A, A, A, A, B);
        FrontierDp.Curve curve = FrontierDp.solve(new Pattern(A, A), s, new GapConstraint(0, 2), null);
        assertEquals(3, curve.supNO(), "sup_NO(aa, aaaab)");
        assertArrayEquals(new int[] {3, 2, 1, 0}, curve.masks(), "OPT(t) for t = 0 .. 3");
        // The branch that keeps position 1 masks positions 2, 3 and 4 (indices 1, 2, 3): optimal.
        assertEquals(0, SupportNO.of(new Pattern(A, A), s.mask(1).mask(2).mask(3), new GapConstraint(0, 2)),
                "masking positions 2, 3 and 4 leaves no occurrence of aa");
        // With all weights 0 the tie goes to the earliest positions: the program returns {1, 2, 3}.
        int[] atZero = curve.positions()[0].clone();
        java.util.Arrays.sort(atZero);
        assertArrayEquals(new int[] {0, 1, 2}, atZero, "the masking set the program returns for t = 0");
    }

    @Test
    @DisplayName("LocalSearch on the example database: P = ab, gap [0,2], sigma 6, l_max 3, eps 0.5")
    void localSearchOnExampleDatabase() {
        SequenceDatabase database = exampleDatabase();
        GapConstraint gap = new GapConstraint(0, 2);
        long sigma = 6;
        int maxLength = 3;
        Pattern ab = new Pattern(A, B);
        HideProblem problem = new HideProblem(database, List.of(ab), gap, HideProblem.Mode.HIDE_DB,
                (int) sigma, 0.5, maxLength);
        Metrics.Context context = Metrics.Context.of(database, gap, maxLength, sigma);
        Map<Pattern, Long> frequent = context.frequent();
        Set<Pattern> closure = problem.closureOf(frequent.keySet());
        List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, 0.5, closure);
        double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
        Map<Pattern, Long> watched = watched(frequent, closure);

        assertEquals(8L, frequent.get(ab), "sup_NO(ab, D)");
        Pattern ba = new Pattern(B, A);
        assertEquals(6L, frequent.get(ba), "sup_NO(ba, D), so m_ba = 1");

        // Stages (i) and (ii) mask position 5 of S2 and positions 2 and 3 of S4, 1-based.
        Map<Integer, Set<Integer>> stated = Map.of(2, Set.of(5), 4, Set.of(2, 3));
        Sanitization first = CutHide.forBand(CutHideConfig.standard(), band, factor).run(problem);
        assertEquals(3, first.maskCount(), "three masks are needed");
        assertEquals(stated, oneBased(first.masked()), "stage (i) masks");

        AdaptiveCutHide second = new AdaptiveCutHide(band, factor, watched, sigma,
                AdaptiveCutHide.DEFAULT_ITERATIONS, context.session(problem)::drops);
        assertEquals(stated, oneBased(second.run(problem).masked()), "stage (ii) masks");

        Map<Pattern, Long> drop = context.session(problem).drops(first);
        assertEquals(1, lost(drop, watched, sigma), "the stage solution loses one pattern");
        assertTrue(watched.get(ba) - drop.getOrDefault(ba, 0L) < sigma, "the lost pattern is ba");

        AdaptiveCutHide full = new AdaptiveCutHide(band, factor, watched, sigma,
                AdaptiveCutHide.DEFAULT_ITERATIONS, context.session(problem)::drops)
                .withLocalSearch(MaskLocalSearch.DEFAULT_PASSES, MaskLocalSearch.DEFAULT_RECEIVERS);
        Sanitization third = full.run(problem);
        MaskLocalSearch.Result search = full.searchResult();
        assertFalse(search.nothingSaveable(), "the stopping test does not end the search at once");
        assertEquals(0, search.moves(), "no move lowers Lost");
        assertEquals(1, search.passes(), "one pass");
        assertEquals(stated, oneBased(third.masked()), "the local search returns the same solution");

        // Optimality: every set of three masks that hides ab loses at least one pattern.
        List<int[]> positions = new ArrayList<>();
        for (int s = 0; s < database.size(); s++) {
            if (SupportNO.of(ab, database.get(s), gap) == 0) {
                continue;
            }
            for (int i = 0; i < database.get(s).length(); i++) {
                positions.add(new int[] {s, i});
            }
        }
        int hiding = 0;
        int best = Integer.MAX_VALUE;
        int atBest = 0;
        for (int x = 0; x < positions.size(); x++) {
            for (int y = x + 1; y < positions.size(); y++) {
                for (int z = y + 1; z < positions.size(); z++) {
                    Sanitization candidate = new Sanitization(database);
                    for (int[] q : new int[][] {positions.get(x), positions.get(y), positions.get(z)}) {
                        candidate.mask(q[0], q[1]);
                    }
                    Map<Pattern, Long> d = context.session(problem).drops(candidate);
                    if (8L - d.getOrDefault(ab, 0L) >= sigma) {
                        continue;
                    }
                    hiding++;
                    int l = lost(d, watched, sigma);
                    if (l < best) {
                        best = l;
                        atBest = 0;
                    }
                    if (l == best) {
                        atBest++;
                    }
                }
            }
        }
        assertTrue(hiding > 0, "some set of three masks hides ab (checked " + hiding + " sets)");
        assertEquals(213, hiding, "minimum hiding sets enumerated");
        assertEquals(1, best, "the least Lost over the minimum hiding sets");
        assertEquals(35, atBest, "minimum hiding sets that lose exactly one pattern");
    }

    @Test
    @DisplayName("GreedyMulti on the example database: S = {ab, ba}, gap [0,2], sigma 6, l_max 2")
    void greedyMultiOnExampleDatabase() {
        SequenceDatabase database = exampleDatabase();
        GapConstraint gap = new GapConstraint(0, 2);
        long sigma = 6;
        int maxLength = 2;
        Pattern ab = new Pattern(A, B);
        Pattern ba = new Pattern(B, A);
        HideProblem problem = new HideProblem(database, List.of(ab, ba), gap, HideProblem.Mode.HIDE_DB,
                (int) sigma, 0.5, maxLength);
        assertEquals(8, SupportNO.of(ab, database, gap), "sup_NO(ab, D)");
        assertEquals(6, SupportNO.of(ba, database, gap), "sup_NO(ba, D)");

        // Position 6 of S3, index 5, is vital for both patterns.
        NettreeCutIndex index = new NettreeCutIndex(database.get(2), gap);
        int idAb = index.add(ab, true);
        int idBa = index.add(ba, true);
        assertTrue(java.util.Arrays.stream(index.vitalPositions(idAb)).anyMatch(i -> i == 5),
                "position 6 of S3 is vital for ab");
        assertTrue(java.util.Arrays.stream(index.vitalPositions(idBa)).anyMatch(i -> i == 5),
                "position 6 of S3 is vital for ba");

        Metrics.Context context = Metrics.Context.of(database, gap, maxLength, sigma);
        Map<Pattern, Long> frequent = context.frequent();
        Set<Pattern> closure = problem.closureOf(frequent.keySet());
        List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, 0.5, closure);
        double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);

        Map<Integer, Set<Integer>> stated = Map.of(1, Set.of(1), 2, Set.of(5), 3, Set.of(6));
        Sanitization greedy = CutHide.forBand(CutHideConfig.standard(), band, factor).run(problem);
        assertEquals(stated, oneBased(greedy.masked()), "the greedy masks");
        // The bound: |R|* of ab alone is sup_NO - sigma + 1 = 3, and three masks reach it.
        assertEquals(3, greedy.maskCount(), "the greedy solution reaches the bound of 3");
        Sanitization refined = CutHide.forBand(CutHideConfig.standard().withRefineSeveral(), band, factor)
                .run(problem);
        assertEquals(stated, oneBased(refined.masked()), "the refinement drops no mask");
    }
}
