// SPDX-License-Identifier: MIT
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
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The local search over the minimum-mask hiding sets: it keeps |R| and the hiding, never ends above
 * its start, makes exactly the same moves with and without the saving condition of lemma L9, and on
 * the Max-Cut instances leaves at most half of the edges monochromatic.
 */
class MaskLocalSearchTest {

    /** Everything a search needs for one problem, built the way SC3 builds it. */
    private record Setup(HideProblem problem, Map<Pattern, Long> watched, List<Pattern> band,
                         double[] factor, Metrics.Context.Session session,
                         Map<Integer, Map<Pattern, int[]>> vital) {
    }

    private static Setup setup(HideProblem problem, Metrics.Context context, int sigma) {
        Map<Pattern, Long> frequent = context.frequent();
        Pattern p = problem.sensitive().get(0);
        Set<Pattern> closure = problem.closureOf(frequent.keySet());
        List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, 0.5, closure);
        double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
        Map<Pattern, Long> watched = new LinkedHashMap<>();
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            if (!closure.contains(e.getKey())) {
                watched.put(e.getKey(), e.getValue());
            }
        }
        Map<Integer, Map<Pattern, int[]>> vital = new LinkedHashMap<>();
        for (int s = 0; s < problem.database().size(); s++) {
            EventSequence sequence = problem.database().get(s);
            if (SupportNO.of(p, sequence, problem.gap()) == 0) {
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
            vital.put(s, here);
        }
        return new Setup(problem, watched, band, factor, context.session(problem), vital);
    }

    private static Sanitization toSanitization(SequenceDatabase db, Map<Integer, Set<Integer>> masks) {
        Sanitization s = new Sanitization(db);
        for (Map.Entry<Integer, Set<Integer>> e : masks.entrySet()) {
            for (int i : e.getValue()) {
                s.mask(e.getKey(), i);
            }
        }
        return s;
    }

    @Test
    @DisplayName("keeps |R| and the hiding, never ends above its start, same moves without L9")
    void searchOnRandomDatabases() {
        Random random = new Random(31337);
        int instances = 0;
        int improved = 0;
        int savedWork = 0;
        int repeatedItems = 0;
        for (int trial = 0; instances < 150 && trial < 40_000; trial++) {
            int alphabet = 2 + random.nextInt(3);
            List<EventSequence> list = new ArrayList<>();
            int sequences = 4 + random.nextInt(6);
            for (int s = 0; s < sequences; s++) {
                int[] events = new int[5 + random.nextInt(9)];
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
            int sigma = (int) Math.max(2, support - 1 - random.nextInt(4));
            HideProblem problem = new HideProblem(db, List.of(p), gap, HideProblem.Mode.HIDE_DB, sigma, 0.5, 3);
            Metrics.Context context = Metrics.Context.of(db, gap, 3, sigma);
            Setup su = setup(problem, context, sigma);
            if (su.band().isEmpty()) {
                continue;
            }
            Sanitization start = new CutHide(CutHideConfig.standard()).withNearThreshold(su.band(), su.factor()).run(problem);
            int startLost = su.session().evaluate(start).lost();
            MaskLocalSearch.Result with = new MaskLocalSearch(problem, su.watched(), sigma, su.vital(),
                    su.session()::drops, MaskLocalSearch.DEFAULT_PASSES, MaskLocalSearch.DEFAULT_RECEIVERS).run(start);
            MaskLocalSearch.Result without = new MaskLocalSearch(problem, su.watched(), sigma, su.vital(),
                    su.session()::drops, MaskLocalSearch.DEFAULT_PASSES, MaskLocalSearch.DEFAULT_RECEIVERS)
                    .withSavingCondition(false).run(start);
            String where = p + " in " + list + " gap " + gap + " sigma " + sigma;
            Sanitization after = toSanitization(db, with.masks());
            assertEquals(start.maskCount(), after.maskCount(), "|R| changed, " + where);
            assertTrue(SupportNO.of(p, db.mask(with.masks()), gap) < sigma, "P no longer hidden, " + where);
            assertEquals(su.session().evaluate(after).lost(), with.lost(), "incremental Lost, " + where);
            assertTrue(with.lost() <= startLost, "ended above its start, " + where);
            // Lemma L9 removes only moves that cannot lower Lost: the same moves, the same result.
            assertEquals(without.masks(), with.masks(), "the saving condition changed the search, " + where);
            assertEquals(without.moves(), with.moves(), where);
            assertTrue(with.evaluated() <= without.evaluated(), where);
            if (with.evaluated() < without.evaluated()) {
                savedWork++;
            }
            if (with.lost() < startLost) {
                improved++;
            }
            if (p.mu() >= 2) {
                repeatedItems++;
            }
            instances++;
        }
        System.out.printf("Local search on random databases: %d instances (%d repeated-item patterns), fewer lost "
                + "patterns than CUT-HIDE in %d, fewer evaluations with lemma L9 in %d%n",
                instances, repeatedItems, improved, savedWork);
        assertEquals(150, instances, "instances reached");
        assertTrue(improved >= 5, "the search never improved, so the check is empty: " + improved);
        assertTrue(savedWork >= 5, "the saving condition never skipped a move, so its check is empty: " + savedWork);
        assertTrue(repeatedItems >= 20, "repeated-item patterns: " + repeatedItems);
    }

    @Test
    @DisplayName("on the Max-Cut instances at most half of the edges stay monochromatic")
    void halfCutOnMaxCutInstances() {
        Random random = new Random(2024);
        int instances = 0;
        int optimal = 0;
        int optimalBefore = 0;
        for (int trial = 0; instances < 40 && trial < 10_000; trial++) {
            int vertices = 3 + random.nextInt(5);
            List<int[]> edges = MaxCutReductionTest.randomGraph(random, vertices, vertices);
            if (edges.size() < 2) {
                continue;
            }
            SequenceDatabase db = MaxCutReductionTest.build(vertices, edges, 1, MaxCutReductionTest.Fault.NONE);
            GapConstraint gap = GapConstraint.of(0, GapConstraint.UNBOUNDED_MAX);
            HideProblem problem = new HideProblem(db, List.of(MaxCutReductionTest.P), gap,
                    HideProblem.Mode.HIDE_DB, 1, 0.5, 3);
            Metrics.Context context = Metrics.Context.of(db, gap, 3, 1);
            Setup su = setup(problem, context, 1);
            if (su.band().isEmpty()) {
                continue;
            }
            // C = Lost - monochromatic edges is the same for every choice; the optimum by trying all.
            int best = Integer.MAX_VALUE;
            int constant = 0;
            for (int choice = 0; choice < (1 << vertices); choice++) {
                Sanitization s = new Sanitization(db);
                int mono = 0;
                for (int v = 0; v < vertices; v++) {
                    s.mask(v, (choice >> v) & 1);
                }
                for (int[] e : edges) {
                    if (((choice >> e[0]) & 1) == ((choice >> e[1]) & 1)) {
                        mono++;
                    }
                }
                int lost = su.session().evaluate(s).lost();
                best = Math.min(best, lost);
                constant = lost - mono;
            }
            AdaptiveCutHide method = new AdaptiveCutHide(su.band(), su.factor(), su.watched(), 1,
                    AdaptiveCutHide.DEFAULT_ITERATIONS, su.session()::drops)
                    .withLocalSearch(MaskLocalSearch.DEFAULT_PASSES, MaskLocalSearch.DEFAULT_RECEIVERS);
            Sanitization result = method.run(problem);
            int lost = su.session().evaluate(result).lost();
            int before = method.lostPerIteration()[0];
            assertTrue(2 * (lost - constant) <= edges.size(),
                    "more than half of the edges monochromatic: " + (lost - constant) + " of " + edges.size());
            assertTrue(lost >= best, "below the optimum: the evaluation is wrong");
            if (lost == best) {
                optimal++;
            }
            if (before == best) {
                optimalBefore++;
            }
            instances++;
        }
        System.out.printf("Local search on Max-Cut instances: %d graphs, optimum reached by CUT-HIDE in %d, "
                + "after the local search in %d%n", instances, optimalBefore, optimal);
        assertEquals(40, instances, "instances reached");
        assertTrue(optimalBefore < optimal, "the local search should reach the optimum where CUT-HIDE does not");
    }

    @Test
    @DisplayName("refuses what theorem P8 does not solve exactly")
    void refusesUnsupportedProblems() {
        SequenceDatabase db = new SequenceDatabase(List.of(new EventSequence(0, new int[] {0, 1, 0, 1, 2})));
        GapConstraint gap = GapConstraint.of(0, 2);
        AdaptiveCutHide.DropOracle none = s -> Map.of();
        assertThrows(IllegalArgumentException.class, () -> new MaskLocalSearch(new HideProblem(db,
                List.of(new Pattern(0, 1), new Pattern(1, 2)), gap, HideProblem.Mode.HIDE_DB, 1, 0.5, 3),
                Map.of(), 1, Map.of(), none, 5, 5));
        assertThrows(IllegalArgumentException.class, () -> new MaskLocalSearch(new HideProblem(db,
                List.of(new Pattern(0, 1)), GapConstraint.of(1, 2), HideProblem.Mode.HIDE_DB, 1, 0.5, 3),
                Map.of(), 1, Map.of(), none, 5, 5));
        assertThrows(IllegalArgumentException.class, () -> new MaskLocalSearch(new HideProblem(db,
                List.of(new Pattern(0, 1)), gap, HideProblem.Mode.HIDE_SEQ, 1, 0.5, 3),
                Map.of(), 1, Map.of(), none, 5, 5));
    }
}
