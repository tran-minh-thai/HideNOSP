package hidenosp;

import hidenosp.core.BruteForce;
import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.hide.FrontierDp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exact program against the definition: least masks and least weight found by trying every
 * subset of positions, on small random instances that include repeated items, and the worked
 * examples of P3 and P6(c) of the design.
 */
class FrontierDpTest {

    private static EventSequence seq(String letters) {
        int[] events = new int[letters.length()];
        for (int i = 0; i < events.length; i++) {
            events[i] = letters.charAt(i) - 'a';
        }
        return new EventSequence(0, events);
    }

    private static Pattern pat(String letters) {
        int[] items = new int[letters.length()];
        for (int i = 0; i < items.length; i++) {
            items[i] = letters.charAt(i) - 'a';
        }
        return new Pattern(items);
    }

    /** sup_NO after masking, by the flow for speed or by exhaustive packing for the smallest. */
    private static int support(Pattern p, EventSequence s, GapConstraint gap, boolean exhaustive) {
        return exhaustive ? BruteForce.supNO(p, s, gap) : SupportNO.of(p, s, gap);
    }

    /** {least size, least weight at that size} over every subset reaching sup_NO <= t. */
    private static long[] bySubsets(Pattern p, EventSequence s, GapConstraint gap, int t, long[] w,
                                    boolean exhaustive) {
        int n = s.length();
        long bestSize = Long.MAX_VALUE;
        long bestWeight = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << n); mask++) {
            int size = Integer.bitCount(mask);
            if (size > bestSize) {
                continue;
            }
            List<Integer> positions = new ArrayList<>();
            long weight = 0;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    positions.add(i);
                    weight += w[i];
                }
            }
            if (size == bestSize && weight >= bestWeight) {
                continue;
            }
            if (support(p, s.mask(positions), gap, exhaustive) <= t) {
                if (size < bestSize || weight < bestWeight) {
                    bestSize = size;
                    bestWeight = weight;
                }
            }
        }
        return new long[] {bestSize, bestWeight};
    }

    @Test
    @DisplayName("least masks and least weight equal the subset search, repeated items included")
    void matchesSubsetSearch() {
        Random random = new Random(2026);
        int instances = 0;
        int targets = 0;
        int belowCut = 0;       // mu >= 2 and the optimum is below F - t: P3's lower side
        int weightDecides = 0;  // the least weight differs from the heaviest optimal-size set
        int exhaustiveTargets = 0;
        int[] gaps = {0, 1, 2, 3, GapConstraint.UNBOUNDED_MAX};
        for (int trial = 0; trial < 700; trial++) {
            int alphabet = 1 + random.nextInt(3);
            int n = random.nextInt(11);
            int m = 1 + random.nextInt(4);
            int[] events = new int[n];
            for (int i = 0; i < n; i++) {
                events[i] = random.nextInt(alphabet);
            }
            int[] items = new int[m];
            for (int j = 0; j < m; j++) {
                items[j] = random.nextInt(alphabet);
            }
            EventSequence s = new EventSequence(trial, events);
            Pattern p = new Pattern(items);
            GapConstraint gap = new GapConstraint(0, gaps[random.nextInt(gaps.length)]);
            long[] w = new long[n];
            for (int i = 0; i < n; i++) {
                w[i] = random.nextInt(10);
            }
            boolean exhaustive = n <= 7;
            FrontierDp.Curve curve = FrontierDp.solve(p, s, gap, w);
            assertEquals(support(p, s, gap, exhaustive), curve.supNO(), "F of " + p + " in " + s);
            instances++;
            for (int t = 0; t <= curve.supNO(); t++) {
                long[] expected = bySubsets(p, s, gap, t, w, exhaustive);
                String where = p + " in " + s + " gap " + gap + " t " + t;
                assertEquals(expected[0], curve.masks()[t], "masks, " + where);
                assertEquals(expected[1], curve.weight()[t], "weight, " + where);
                int[] r = curve.positions()[t];
                assertEquals(curve.masks()[t], r.length, "size of the returned set, " + where);
                long sum = 0;
                List<Integer> list = new ArrayList<>();
                for (int i : r) {
                    sum += w[i];
                    list.add(i);
                }
                assertEquals(curve.weight()[t], sum, "weight of the returned set, " + where);
                assertTrue(support(p, s.mask(list), gap, exhaustive) <= t,
                        "the returned set reaches t, " + where);
                if (p.mu() >= 2 && curve.masks()[t] < curve.supNO() - t) {
                    belowCut++;
                }
                if (curve.masks()[t] > 0 && heaviest(p, s, gap, t, w, curve.masks()[t])
                        > curve.weight()[t]) {
                    weightDecides++;
                }
                targets++;
                if (exhaustive) {
                    exhaustiveTargets++;
                }
            }
        }
        System.out.printf("FrontierDp against the subset search: %d instances, %d targets (%d by "
                + "exhaustive packing), %d below F - t with repeated items, %d where the weight "
                + "decides%n", instances, targets, exhaustiveTargets, belowCut, weightDecides);
        // The check must have met the cases it exists for, not only easy ones.
        assertEquals(700, instances);
        assertTrue(targets >= 700, "targets checked: " + targets);
        assertTrue(belowCut >= 20, "cases with masks below F - t: " + belowCut);
        assertTrue(weightDecides >= 50, "cases where the weight decides: " + weightDecides);
        assertTrue(exhaustiveTargets >= 200, "targets checked by exhaustive packing: "
                + exhaustiveTargets);
    }

    /** Heaviest set of the given size that reaches t, to know whether the weight had to decide. */
    private static long heaviest(Pattern p, EventSequence s, GapConstraint gap, int t, long[] w,
                                 int size) {
        long best = Long.MIN_VALUE;
        int n = s.length();
        for (int mask = 0; mask < (1 << n); mask++) {
            if (Integer.bitCount(mask) != size) {
                continue;
            }
            List<Integer> positions = new ArrayList<>();
            long weight = 0;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    positions.add(i);
                    weight += w[i];
                }
            }
            if (weight > best && SupportNO.of(p, s.mask(positions), gap) <= t) {
                best = weight;
            }
        }
        return best;
    }

    @Test
    @DisplayName("the examples of P3: both bounds reached")
    void p3Examples() {
        // P = aa, S = abdacaa, [0,2]: F = 3 and two masks bring it to 0 (lower bound side).
        FrontierDp.Curve c1 = FrontierDp.solve(pat("aa"), seq("abdacaa"), GapConstraint.of(0, 2), null);
        assertEquals(3, c1.supNO());
        assertEquals(2, c1.masks()[0]);
        // P = aa, S = baaacca, [0,2], t = 1: one mask (position 4) suffices.
        FrontierDp.Curve c2 = FrontierDp.solve(pat("aa"), seq("baaacca"), GapConstraint.of(0, 2), null);
        assertEquals(3, c2.supNO());
        assertEquals(1, c2.masks()[1]);
        assertArrayEquals(new int[] {3}, c2.positions()[1]);
        // P = aa, S = aabbbaa, [0,0], t = 0: the upper bound F - t = 2 is needed.
        FrontierDp.Curve c3 = FrontierDp.solve(pat("aa"), seq("aabbbaa"), GapConstraint.of(0, 0), null);
        assertEquals(2, c3.supNO());
        assertEquals(2, c3.masks()[0]);
    }

    @Test
    @DisplayName("the weighted counterexamples of P6(c) are solved exactly")
    void p6Counterexamples() {
        // P = ca, S = babbacbcaacb, [0,2]; weights c6 = 3, c8 = 50, a9 = 100, a10 = 1 (1-based).
        long[] w1 = new long[12];
        w1[5] = 3;
        w1[7] = 50;
        w1[8] = 100;
        w1[9] = 1;
        FrontierDp.Curve c1 = FrontierDp.solve(pat("ca"), seq("babbacbcaacb"), GapConstraint.of(0, 2), w1);
        assertEquals(2, c1.supNO());
        assertEquals(1, c1.masks()[1]);
        assertEquals(1, c1.weight()[1]);        // H1 of P6(c) gives 3
        assertEquals(2, c1.masks()[0]);
        assertEquals(53, c1.weight()[0]);       // H2 of P6(c) gives 101
        // P = ab, S = baabbab, [0,2], t = 1; weights a2 = 4, a3 = 37, a6 = 27, b4 = 5, b5 = 6, b7 = 18.
        long[] w2 = {0, 4, 37, 5, 6, 27, 18};
        FrontierDp.Curve c2 = FrontierDp.solve(pat("ab"), seq("baabbab"), GapConstraint.of(0, 2), w2);
        assertEquals(3, c2.supNO());
        assertEquals(2, c2.masks()[1]);
        assertEquals(11, c2.weight()[1]);       // H2 of P6(c) gives 22
        assertArrayEquals(new int[] {3, 4}, c2.positions()[1]);
    }

    @Test
    @DisplayName("mu(P) = 1 gives F - t for every t, as P1 says")
    void distinctItemsFollowP1() {
        Random random = new Random(7);
        int checked = 0;
        for (int trial = 0; trial < 200; trial++) {
            int n = 5 + random.nextInt(40);
            int[] events = new int[n];
            for (int i = 0; i < n; i++) {
                events[i] = random.nextInt(4);
            }
            Pattern p = new Pattern(0, 1, 2);
            GapConstraint gap = GapConstraint.of(0, random.nextInt(4));
            FrontierDp.Curve curve = FrontierDp.solve(p, new EventSequence(trial, events), gap, null);
            for (int t = 0; t <= curve.supNO(); t++) {
                assertEquals(curve.supNO() - t, curve.masks()[t]);
                checked++;
            }
        }
        assertTrue(checked >= 300, "targets checked: " + checked);
    }

    @Test
    @DisplayName("an optimum that deletes fewer than t nodes is found")
    void optimumBelowTheBudget() {
        // Rare cases where no optimal solution can spend the whole budget t of deleted nodes, so
        // the answer for t must also look at smaller budgets. Among 60000 random instances, a
        // program that looked at budget t only gave another answer at 5 targets and none at all
        // on 23 instances; these three are among the latter.
        Object[][] cases = {
            {pat("aaab"), seq("abababababbb"), GapConstraint.of(0, 1), 1},
            {pat("bbba"), seq("abbbababbaaaa"), GapConstraint.of(0, 1), 2},
            {pat("bbba"), seq("bbabababaaa"), GapConstraint.of(0, 2), 1},
        };
        for (Object[] c : cases) {
            Pattern p = (Pattern) c[0];
            EventSequence s = (EventSequence) c[1];
            GapConstraint gap = (GapConstraint) c[2];
            int t = (Integer) c[3];
            FrontierDp.Curve curve = FrontierDp.solve(p, s, gap, null);
            long[] expected = bySubsets(p, s, gap, t, new long[s.length()], false);
            assertEquals(expected[0], curve.masks()[t], p + " in " + s + " t " + t);
        }
    }

    @Test
    @DisplayName("g- > 0 is refused")
    void refusesPositiveMinimumGap() {
        assertThrows(IllegalArgumentException.class,
                () -> FrontierDp.solve(pat("aa"), seq("aaaa"), GapConstraint.of(1, 2), null));
    }
}
