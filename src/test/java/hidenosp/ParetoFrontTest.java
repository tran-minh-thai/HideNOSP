package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.CostCurve;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proposition P10: the vertices of the convex Pareto front of CUT-HIDE equal the vertices of the
 * lower convex hull of the non-dominated (|R|, w(R)) points, found by trying every set of positions
 * of small databases with random integer weights.
 */
class ParetoFrontTest {

    /** Non-dominated points of all feasible sets, then the vertices of their lower convex hull. */
    private static List<long[]> hullBySubsets(Pattern p, SequenceDatabase db, GapConstraint gap,
                                              HideProblem.Mode mode, int threshold, long[][] w) {
        List<int[]> slots = new ArrayList<>();
        for (int s = 0; s < db.size(); s++) {
            for (int i = 0; i < db.get(s).length(); i++) {
                slots.add(new int[] {s, i});
            }
        }
        // For every number of masks, the least weight of a feasible set of that size.
        TreeMap<Integer, Long> best = new TreeMap<>();
        for (int mask = 0; mask < (1 << slots.size()); mask++) {
            Map<Integer, List<Integer>> chosen = new HashMap<>();
            long weight = 0;
            for (int k = 0; k < slots.size(); k++) {
                if ((mask & (1 << k)) != 0) {
                    int[] slot = slots.get(k);
                    chosen.computeIfAbsent(slot[0], x -> new ArrayList<>()).add(slot[1]);
                    weight += w[slot[0]][slot[1]];
                }
            }
            int size = Integer.bitCount(mask);
            Long known = best.get(size);
            if (known != null && known <= weight) {
                continue;
            }
            SequenceDatabase after = db.mask(chosen);
            boolean hidden;
            if (mode == HideProblem.Mode.HIDE_DB) {
                hidden = SupportNO.of(p, after, gap) < threshold;
            } else {
                hidden = true;
                for (EventSequence s : after.sequences()) {
                    hidden &= SupportNO.of(p, s, gap) < threshold;
                }
            }
            if (hidden) {
                best.put(size, weight);
            }
        }
        // Non-dominated: weight strictly below every smaller size.
        List<long[]> front = new ArrayList<>();
        long lowest = Long.MAX_VALUE;
        for (Map.Entry<Integer, Long> e : best.entrySet()) {
            if (e.getValue() < lowest) {
                front.add(new long[] {e.getKey(), e.getValue()});
                lowest = e.getValue();
            }
        }
        // Lower convex hull by the monotone chain, dropping collinear points.
        List<long[]> hull = new ArrayList<>();
        for (long[] q : front) {
            while (hull.size() >= 2) {
                long[] o = hull.get(hull.size() - 2);
                long[] a = hull.get(hull.size() - 1);
                long cross = (a[0] - o[0]) * (q[1] - o[1]) - (a[1] - o[1]) * (q[0] - o[0]);
                if (cross <= 0) {
                    hull.remove(hull.size() - 1);
                } else {
                    break;
                }
            }
            hull.add(q);
        }
        return hull;
    }

    @Test
    @DisplayName("the vertices equal the lower convex hull of every feasible set")
    void verticesMatchTheHull() {
        Random random = new Random(1234);
        int instances = 0;
        int withInnerVertex = 0;
        int withRepeatedItems = 0;
        int[] gaps = {0, 1, 2, GapConstraint.UNBOUNDED_MAX};
        // Half the instances are drawn freely; the other half are built to trade masks for weight:
        // two letters with a majority of a (positions that stand at several levels of aa or aba)
        // and weights that are either cheap or dear, so fronts with inner vertices occur.
        int[][] designedPatterns = {{0, 0}, {0, 1, 0}, {0, 0, 1}, {0, 1}};
        long[] designedWeights = {0, 1, 2, 9, 15};
        // A third kind is built for fronts with several vertices: runs aaa or aaaa whose inner
        // positions stand at two levels of aa and are dear, while the ends are cheap, with a
        // different exchange rate in every sequence.
        for (int trial = 0; instances < 450; trial++) {
            int kind = trial % 3;
            boolean designed = kind == 1;
            boolean runs = kind == 2;
            int alphabet = designed || runs ? 2 : 1 + random.nextInt(3);
            int sequences = runs ? 3 : designed ? 2 + random.nextInt(2) : 1 + random.nextInt(3);
            List<EventSequence> list = new ArrayList<>();
            int positions = 0;
            for (int s = 0; s < sequences; s++) {
                int n = runs ? 3 + random.nextInt(2) : designed ? 3 + random.nextInt(3) : random.nextInt(6);
                int[] events = new int[n];
                for (int i = 0; i < n; i++) {
                    events[i] = runs ? 0 : designed ? (random.nextInt(4) == 0 ? 1 : 0) : random.nextInt(alphabet);
                }
                positions += n;
                list.add(new EventSequence(s, events));
            }
            if (positions > 13) {
                continue;
            }
            int[] items;
            if (runs) {
                items = new int[] {0, 0};
            } else if (designed) {
                items = designedPatterns[random.nextInt(designedPatterns.length)];
            } else {
                items = new int[1 + random.nextInt(3)];
                for (int j = 0; j < items.length; j++) {
                    items[j] = random.nextInt(alphabet);
                }
            }
            Pattern p = new Pattern(items);
            SequenceDatabase db = new SequenceDatabase(list);
            GapConstraint gap = GapConstraint.of(0, runs ? random.nextInt(2) : gaps[random.nextInt(gaps.length)]);
            long support = SupportNO.of(p, db, gap);
            if (support == 0) {
                continue;
            }
            HideProblem.Mode mode = random.nextBoolean() ? HideProblem.Mode.HIDE_DB
                    : HideProblem.Mode.HIDE_SEQ;
            int threshold;
            if (runs) {
                mode = HideProblem.Mode.HIDE_DB;
                threshold = 1 + random.nextInt(2);
            } else if (mode == HideProblem.Mode.HIDE_DB) {
                threshold = 1 + random.nextInt((int) support);
            } else {
                int largest = 0;
                for (EventSequence s : list) {
                    largest = Math.max(largest, SupportNO.of(p, s, gap));
                }
                threshold = 1 + random.nextInt(largest);
            }
            double[][] weights = new double[sequences][];
            long[][] scaled = new long[sequences][];
            for (int s = 0; s < sequences; s++) {
                weights[s] = new double[list.get(s).length()];
                scaled[s] = new long[list.get(s).length()];
                for (int i = 0; i < weights[s].length; i++) {
                    boolean inner = i > 0 && i < weights[s].length - 1;
                    weights[s][i] = runs ? (inner ? 9 + random.nextInt(7) : random.nextInt(4))
                            : designed ? designedWeights[random.nextInt(designedWeights.length)]
                            : random.nextInt(8);
                    scaled[s][i] = Math.round(weights[s][i] * CostCurve.SCALE);
                }
            }
            HideProblem problem = new HideProblem(db, List.of(p), gap, mode, threshold, 0.5, 4);
            List<CutHide.ParetoVertex> front = new CutHide(CutHideConfig.uniformWeights())
                    .withExplicitWeights(weights).paretoFront(problem);
            List<long[]> expected = hullBySubsets(p, db, gap, mode, threshold, scaled);
            String where = p + " in " + list + " gap " + gap + " " + mode + " threshold " + threshold;
            assertEquals(expected.size(), front.size(), "number of vertices, " + where);
            for (int k = 0; k < front.size(); k++) {
                assertEquals(expected.get(k)[0], front.get(k).masks(), "masks of vertex " + k + ", " + where);
                assertEquals(expected.get(k)[1], front.get(k).weight(), "weight of vertex " + k + ", " + where);
            }
            if (front.size() >= 3) {
                withInnerVertex++;
            }
            if (p.mu() >= 2 && front.size() >= 2) {
                withRepeatedItems++;
            }
            instances++;
        }
        System.out.printf("Pareto front against every subset: %d instances, %d with an inner vertex, "
                + "%d repeated-item fronts with two vertices or more%n", instances, withInnerVertex,
                withRepeatedItems);
        assertTrue(withInnerVertex >= 10, "fronts with an inner vertex: " + withInnerVertex);
        assertTrue(withRepeatedItems >= 10, "repeated-item fronts: " + withRepeatedItems);
    }

    @Test
    @DisplayName("the first vertex is the standard CUT-HIDE solution")
    void firstVertexIsTheStandardSolution() {
        Random random = new Random(77);
        int checked = 0;
        for (int trial = 0; checked < 100; trial++) {
            int n = 4 + random.nextInt(8);
            int[] events = new int[n];
            double[] w = new double[n];
            for (int i = 0; i < n; i++) {
                events[i] = random.nextInt(2);
                w[i] = random.nextInt(6);
            }
            SequenceDatabase db = new SequenceDatabase(List.of(new EventSequence(0, events)));
            Pattern p = new Pattern(0, 1, 0);
            GapConstraint gap = GapConstraint.of(0, 2);
            if (SupportNO.of(p, db, gap) == 0) {
                continue;
            }
            HideProblem problem = new HideProblem(db, List.of(p), gap, HideProblem.Mode.HIDE_DB, 1, 0.5, 4);
            CutHide cutHide = new CutHide(CutHideConfig.uniformWeights()).withExplicitWeights(new double[][] {w});
            List<CutHide.ParetoVertex> front = cutHide.paretoFront(problem);
            assertEquals(cutHide.run(problem).maskCount(), front.get(0).masks());
            for (int k = 1; k < front.size(); k++) {
                assertTrue(front.get(k).masks() > front.get(k - 1).masks());
                assertTrue(front.get(k).weight() < front.get(k - 1).weight());
            }
            checked++;
        }
    }
}
