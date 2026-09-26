package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.AdaptiveCutHide;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CUT-HIDE with the adaptive side-effect weight: the same |R|* as CUT-HIDE on every instance and
 * never more lost patterns, and on the Max-Cut instances never fewer than the optimum found by
 * trying every choice.
 *
 * <p>Refuted expectation, kept on record: this test first asserted that on the Max-Cut instances the
 * adaptive weight loses fewer patterns than CUT-HIDE on at least 5 of 40 graphs. It did on 0. Every
 * vertex sees the same signal, so the whole solution flips between "mask a everywhere" and "mask b
 * everywhere", both of which make every edge monochromatic. A re-weighting that changes all
 * sequences at once cannot break that symmetry; moves that change one sequence at a time can.
 */
class AdaptiveCutHideTest {

    /** CUT-HIDE and the adaptive variant on one problem, with Lost counted by the evaluator. */
    private record Pair(int masks, int lost, int adaptiveMasks, int adaptiveLost, int chosen) {
    }

    private static Pair compare(HideProblem problem, Metrics.Context context, int sigma, double epsilon) {
        Map<Pattern, Long> frequent = context.frequent();
        Pattern p = problem.sensitive().get(0);
        Set<Pattern> closure = FrequentPatterns.closure(List.of(p), frequent.keySet());
        List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
        double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
        Map<Pattern, Long> watched = new LinkedHashMap<>();
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            if (!closure.contains(e.getKey())) {
                watched.put(e.getKey(), e.getValue());
            }
        }
        Metrics.Context.Session session = context.session(problem);
        Sanitization standard = new CutHide(CutHideConfig.standard()).withNearThreshold(band, factor).run(problem);
        AdaptiveCutHide adaptive = new AdaptiveCutHide(band, factor, watched, sigma,
                AdaptiveCutHide.DEFAULT_ITERATIONS, session::drops);
        Sanitization result = adaptive.run(problem);
        int[] perIteration = adaptive.lostPerIteration();
        int lost = session.evaluate(standard).lost();
        int adaptiveLost = session.evaluate(result).lost();
        // The bookkeeping of the method must agree with the evaluator on both ends.
        assertEquals(lost, perIteration[0], "iteration 0 is CUT-HIDE");
        assertEquals(adaptiveLost, perIteration[adaptive.chosenIteration()], "Lost of the chosen iteration");
        for (int l : perIteration) {
            assertTrue(l >= adaptiveLost, "the chosen iteration is the best one");
        }
        return new Pair(standard.maskCount(), lost, result.maskCount(), adaptiveLost, adaptive.chosenIteration());
    }

    @Test
    @DisplayName("same |R| as CUT-HIDE and never more lost patterns, on random databases")
    void neverWorseThanCutHide() {
        Random random = new Random(4242);
        int instances = 0;
        int fewer = 0;
        int repeatedItems = 0;
        for (int trial = 0; instances < 120 && trial < 20_000; trial++) {
            int alphabet = 2 + random.nextInt(3);
            int sequences = 4 + random.nextInt(5);
            List<EventSequence> list = new ArrayList<>();
            for (int s = 0; s < sequences; s++) {
                int[] events = new int[5 + random.nextInt(8)];
                for (int i = 0; i < events.length; i++) {
                    events[i] = random.nextInt(alphabet);
                }
                list.add(new EventSequence(s, events));
            }
            SequenceDatabase db = new SequenceDatabase(list);
            GapConstraint gap = GapConstraint.of(0, 1 + random.nextInt(3));
            int[] items = new int[2 + random.nextInt(2)];
            for (int j = 0; j < items.length; j++) {
                items[j] = random.nextInt(alphabet);
            }
            Pattern p = new Pattern(items);
            long support = SupportNO.of(p, db, gap);
            if (support < 3) {
                continue;
            }
            // sigma leaves P frequent, so the hiding needs 1 to 3 masks' worth of drop.
            int sigma = (int) Math.max(2, support - 1 - random.nextInt(3));
            HideProblem problem = new HideProblem(db, List.of(p), gap, HideProblem.Mode.HIDE_DB, sigma, 0.5, 3);
            Metrics.Context context = Metrics.Context.of(db, gap, 3, sigma);
            Set<Pattern> closure = FrequentPatterns.closure(List.of(p), context.frequent().keySet());
            if (FrequentPatterns.nearThreshold(context.frequent(), sigma, 0.5, closure).isEmpty()) {
                continue;  // CUT-HIDE refuses an empty band, and so does iteration 0
            }
            Pair r = compare(problem, context, sigma, 0.5);
            String where = p + " in " + list + " gap " + gap + " sigma " + sigma;
            assertEquals(r.masks(), r.adaptiveMasks(), "|R| differs, " + where);
            assertTrue(r.adaptiveLost() <= r.lost(), "more lost patterns than CUT-HIDE, " + where);
            if (r.adaptiveLost() < r.lost()) {
                fewer++;
            }
            if (p.mu() >= 2) {
                repeatedItems++;
            }
            instances++;
        }
        System.out.printf("Adaptive weight on random databases: %d instances (%d repeated-item patterns), "
                + "fewer lost patterns than CUT-HIDE in %d%n", instances, repeatedItems, fewer);
        assertEquals(120, instances, "instances reached");
        assertTrue(repeatedItems >= 20, "repeated-item patterns: " + repeatedItems);
        assertTrue(fewer >= 1, "the re-weighting never changed the outcome, so the check is empty");
    }

    @Test
    @DisplayName("on the Max-Cut instances: never below the optimum, never above CUT-HIDE")
    void maxCutInstances() {
        Random random = new Random(99);
        int instances = 0;
        int fewer = 0;
        int optimal = 0;
        int optimalBefore = 0;
        for (int trial = 0; instances < 40 && trial < 10_000; trial++) {
            int vertices = 3 + random.nextInt(4);
            List<int[]> edges = MaxCutReductionTest.randomGraph(random, vertices, vertices);
            if (edges.size() < 2) {
                continue;
            }
            SequenceDatabase db = MaxCutReductionTest.build(vertices, edges, 1, MaxCutReductionTest.Fault.NONE);
            GapConstraint gap = GapConstraint.of(0, GapConstraint.UNBOUNDED_MAX);
            int maxLength = 3;
            HideProblem problem = new HideProblem(db, List.of(MaxCutReductionTest.P), gap,
                    HideProblem.Mode.HIDE_DB, 1, 0.5, maxLength);
            Metrics.Context context = Metrics.Context.of(db, gap, maxLength, 1);
            Set<Pattern> closure = FrequentPatterns.closure(List.of(MaxCutReductionTest.P), context.frequent().keySet());
            if (FrequentPatterns.nearThreshold(context.frequent(), 1, 0.5, closure).isEmpty()) {
                continue;
            }
            // The optimum over the minimum-mask sets: one of a, b in every vertex sequence.
            int best = Integer.MAX_VALUE;
            Metrics.Context.Session session = context.session(problem);
            for (int choice = 0; choice < (1 << vertices); choice++) {
                Sanitization s = new Sanitization(db);
                for (int v = 0; v < vertices; v++) {
                    s.mask(v, (choice >> v) & 1);
                }
                best = Math.min(best, session.evaluate(s).lost());
            }
            Pair r = compare(problem, context, 1, 0.5);
            assertEquals(vertices, r.adaptiveMasks());
            assertTrue(r.adaptiveLost() >= best, "below the optimum: the evaluation is wrong");
            assertTrue(r.adaptiveLost() <= r.lost());
            if (r.adaptiveLost() < r.lost()) {
                fewer++;
            }
            if (r.adaptiveLost() == best) {
                optimal++;
            }
            if (r.lost() == best) {
                optimalBefore++;
            }
            instances++;
        }
        System.out.printf("Adaptive weight on Max-Cut instances: %d graphs, optimum reached by CUT-HIDE in %d, "
                + "by the adaptive weight in %d; fewer lost patterns than CUT-HIDE in %d%n",
                instances, optimalBefore, optimal, fewer);
        assertEquals(40, instances, "instances reached");
        assertTrue(optimalBefore < instances, "CUT-HIDE is optimal on every graph, so the check is empty");
    }

    @Test
    @DisplayName("refuses what theorem P8 does not solve exactly")
    void refusesUnsupportedProblems() {
        SequenceDatabase db = new SequenceDatabase(List.of(new EventSequence(0, new int[] {0, 1, 0, 1, 2}),
                new EventSequence(1, new int[] {0, 1, 2, 0, 1})));
        AdaptiveCutHide method = new AdaptiveCutHide(List.of(new Pattern(2)), new double[] {1},
                Map.of(new Pattern(2), 2L), 2, 3, s -> new HashMap<>());
        GapConstraint gap = GapConstraint.of(0, 2);
        assertThrows(IllegalArgumentException.class, () -> method.run(new HideProblem(db,
                List.of(new Pattern(0, 1), new Pattern(1, 2)), gap, HideProblem.Mode.HIDE_DB, 2, 0.5, 3)));
        assertThrows(IllegalArgumentException.class, () -> method.run(new HideProblem(db,
                List.of(new Pattern(0, 1)), GapConstraint.of(1, 2), HideProblem.Mode.HIDE_DB, 2, 0.5, 3)));
        assertThrows(IllegalArgumentException.class, () -> method.run(new HideProblem(db,
                List.of(new Pattern(0, 1)), gap, HideProblem.Mode.HIDE_SEQ, 1, 0.5, 3)));
        assertThrows(IllegalArgumentException.class, () -> new AdaptiveCutHide(List.of(), new double[0],
                Map.of(), 2, 0, s -> new HashMap<>()));
        assertArrayEquals(new int[0], method.lostPerIteration());
    }
}
