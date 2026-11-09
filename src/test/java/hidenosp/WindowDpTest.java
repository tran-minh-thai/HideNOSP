// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.hide.ExactCurve;
import hidenosp.hide.FrontierDp;
import hidenosp.hide.WindowDp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The program of the corollary for g- &gt; 0 against the definition: least masks and least weight
 * found by trying every subset of positions, on small random instances with repeated items; and
 * against {@link FrontierDp} when g- = 0, where both programs apply.
 */
class WindowDpTest {

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

    /** {least size, least weight at that size} over every subset of positions reaching sup_NO <= t. */
    private static long[] bySubsets(Pattern p, EventSequence s, GapConstraint gap, int t, long[] w) {
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
            if (SupportNO.of(p, s.mask(positions), gap) <= t) {
                bestSize = size;
                bestWeight = weight;
            }
        }
        return new long[] {bestSize, bestWeight};
    }

    private static EventSequence randomSequence(Random random, int n, int alphabet) {
        int[] events = new int[n];
        for (int i = 0; i < n; i++) {
            events[i] = random.nextInt(alphabet);
        }
        return new EventSequence(0, events);
    }

    private static Pattern randomPattern(Random random, int m, int alphabet) {
        int[] items = new int[m];
        for (int j = 0; j < m; j++) {
            items[j] = random.nextInt(alphabet);
        }
        return new Pattern(items);
    }

    private static long[] randomWeights(Random random, int n) {
        long[] w = new long[n];
        for (int i = 0; i < n; i++) {
            w[i] = random.nextInt(6);
        }
        return w;
    }

    /** The positions a curve stores for t really reach sup_NO <= t, with the stated size and weight. */
    private static void checkPositions(Pattern p, EventSequence s, GapConstraint gap, long[] w,
                                       FrontierDp.Curve curve, int t) {
        int[] positions = curve.positions()[t];
        assertEquals(curve.masks()[t], positions.length, "size of the stored set for t = " + t);
        long weight = 0;
        List<Integer> list = new ArrayList<>();
        for (int i : positions) {
            weight += w[i];
            list.add(i);
        }
        assertEquals(curve.weight()[t], weight, "weight of the stored set for t = " + t);
        assertTrue(SupportNO.of(p, s.mask(list), gap) <= t, "the stored set misses the target t = " + t);
    }

    @Test
    @DisplayName("g- > 0: least masks and least weight equal the search over every subset")
    void positiveMinimumGapAgainstSubsets() {
        Random random = new Random(20261109L);
        int instances = 0;
        int targets = 0;
        int repeated = 0;
        int positiveMasks = 0;
        int mismatches = 0;
        for (int round = 0; round < 2500; round++) {
            int n = 6 + random.nextInt(6);
            int m = 2 + random.nextInt(3);
            int gmin = 1 + random.nextInt(2);
            int gmax = gmin + random.nextInt(5 - gmin);
            GapConstraint gap = new GapConstraint(gmin, gmax);
            EventSequence s = randomSequence(random, n, 2 + random.nextInt(2));
            Pattern p = randomPattern(random, m, 2 + random.nextInt(2));
            long[] w = randomWeights(random, n);
            FrontierDp.Curve curve = WindowDp.solve(p, s, gap, w, new FrontierDp.Order(0, 1));
            assertEquals(SupportNO.of(p, s, gap), curve.supNO());
            if (curve.supNO() == 0) {
                continue;
            }
            instances++;
            if (!p.hasDistinctItems()) {
                repeated++;
            }
            for (int t = 0; t <= curve.supNO(); t++) {
                long[] expected = bySubsets(p, s, gap, t, w);
                targets++;
                if (expected[0] > 0) {
                    positiveMasks++;
                }
                if (curve.masks()[t] != expected[0] || curve.weight()[t] != expected[1]) {
                    mismatches++;
                }
                checkPositions(p, s, gap, w, curve, t);
            }
        }
        System.out.printf("WindowDp g- > 0: %d instances (%d with repeated items), %d targets"
                + " (%d need a mask), %d mismatches%n", instances, repeated, targets, positiveMasks, mismatches);
        // The comparison is not vacuous: many instances carry the pattern, many need masks, and
        // repeated-item patterns, where one mask can remove several nodes, are among them.
        assertTrue(instances >= 300, "too few instances carry the pattern: " + instances);
        assertTrue(repeated >= 100, "too few repeated-item instances: " + repeated);
        assertTrue(positiveMasks >= 300, "too few targets need a mask: " + positiveMasks);
        assertEquals(0, mismatches);
    }

    @Test
    @DisplayName("g- > 0, weight first: least weight, then least masks, equal the search over every subset")
    void positiveMinimumGapWeightFirst() {
        Random random = new Random(7L);
        int compared = 0;
        for (int round = 0; round < 300; round++) {
            int n = 6 + random.nextInt(5);
            GapConstraint gap = new GapConstraint(1, 1 + random.nextInt(3));
            EventSequence s = randomSequence(random, n, 2);
            Pattern p = randomPattern(random, 2 + random.nextInt(2), 2);
            long[] w = randomWeights(random, n);
            FrontierDp.Curve curve = WindowDp.solve(p, s, gap, w, new FrontierDp.Order(1, 0));
            for (int t = 0; t <= curve.supNO(); t++) {
                long bestWeight = Long.MAX_VALUE;
                long bestSize = Long.MAX_VALUE;
                for (int mask = 0; mask < (1 << n); mask++) {
                    List<Integer> positions = new ArrayList<>();
                    long weight = 0;
                    for (int i = 0; i < n; i++) {
                        if ((mask & (1 << i)) != 0) {
                            positions.add(i);
                            weight += w[i];
                        }
                    }
                    int size = positions.size();
                    boolean better = weight < bestWeight || (weight == bestWeight && size < bestSize);
                    if (better && SupportNO.of(p, s.mask(positions), gap) <= t) {
                        bestWeight = weight;
                        bestSize = size;
                    }
                }
                assertEquals(bestWeight, curve.weight()[t], "least weight, t = " + t);
                assertEquals(bestSize, curve.masks()[t], "masks of the lightest set, t = " + t);
                checkPositions(p, s, gap, w, curve, t);
                compared++;
            }
        }
        assertTrue(compared >= 300, "too few targets compared: " + compared);
    }

    @Test
    @DisplayName("g- = 0: the window program computes the curves of FrontierDp")
    void zeroMinimumGapAgreesWithFrontierDp() {
        Random random = new Random(11L);
        int compared = 0;
        for (int round = 0; round < 600; round++) {
            int n = 5 + random.nextInt(25);
            int m = 2 + random.nextInt(3);
            GapConstraint gap = new GapConstraint(0, random.nextInt(5));
            EventSequence s = randomSequence(random, n, 2 + random.nextInt(3));
            Pattern p = randomPattern(random, m, 2 + random.nextInt(2));
            long[] w = randomWeights(random, n);
            for (FrontierDp.Order order : List.of(new FrontierDp.Order(0, 1), new FrontierDp.Order(1, 0),
                    new FrontierDp.Order(2, 3))) {
                FrontierDp.Curve a = FrontierDp.solve(p, s, gap, w, order);
                FrontierDp.Curve b = WindowDp.solve(p, s, gap, w, order);
                assertEquals(a.supNO(), b.supNO());
                assertArrayEquals(a.masks(), b.masks(), "masks, " + p + " in " + s + " " + gap);
                assertArrayEquals(a.weight(), b.weight(), "weight, " + p + " in " + s + " " + gap);
                compared++;
            }
        }
        assertTrue(compared >= 1500, "too few comparisons: " + compared);
    }

    @Test
    @DisplayName("the counterexample to the latest-node rule: ab in aab under [1,1]")
    void latestNodeCounterexample() {
        // b at position 3 is reachable through a at position 1, while the latest a, at position 2,
        // is too close. sup_NO = 1 and one mask (on b, or on the first a) brings it to 0.
        Pattern p = pat("ab");
        EventSequence s = seq("aab");
        GapConstraint gap = new GapConstraint(1, 1);
        FrontierDp.Curve curve = WindowDp.solve(p, s, gap, null, new FrontierDp.Order(0, 1));
        assertEquals(1, curve.supNO());
        assertEquals(1, curve.masks()[0]);
        assertEquals(0, curve.masks()[1]);
        assertThrows(IllegalArgumentException.class,
                () -> FrontierDp.solve(p, s, gap, null));
    }

    @Test
    @DisplayName("applies: finite g+ and a state of at most 62 bits")
    void appliesOnlyToSmallFiniteWindows() {
        assertFalse(WindowDp.applies(GapConstraint.unbounded(1), pat("ab")));
        assertTrue(WindowDp.applies(new GapConstraint(1, 3), pat("abcd")));
        // (4 - 1) * (20 + 1) = 63 bits: one too many.
        assertFalse(WindowDp.applies(new GapConstraint(1, 20), pat("abcd")));
        assertTrue(WindowDp.applies(new GapConstraint(1, 19), pat("abcd")));
        assertThrows(IllegalArgumentException.class,
                () -> WindowDp.solve(pat("ab"), seq("ab"), GapConstraint.unbounded(1), null,
                        new FrontierDp.Order(0, 1)));
    }

    @Test
    @DisplayName("ExactCurve: FrontierDp itself when g- = 0, the window program when g- > 0")
    void dispatch() {
        Random random = new Random(3L);
        for (int round = 0; round < 200; round++) {
            int n = 8 + random.nextInt(20);
            EventSequence s = randomSequence(random, n, 3);
            Pattern p = randomPattern(random, 3, 3);
            long[] w = randomWeights(random, n);
            GapConstraint zero = new GapConstraint(0, 2);
            FrontierDp.Curve a = FrontierDp.solve(p, s, zero, w);
            FrontierDp.Curve b = ExactCurve.solve(p, s, zero, w);
            assertArrayEquals(a.masks(), b.masks());
            assertArrayEquals(a.weight(), b.weight());
            for (int t = 0; t <= a.supNO(); t++) {
                // The very same program, so the very same set, not only the same cost.
                assertArrayEquals(a.positions()[t], b.positions()[t]);
            }
            GapConstraint positive = new GapConstraint(1, 3);
            FrontierDp.Curve c = WindowDp.solve(p, s, positive, w, new FrontierDp.Order(0, 1));
            FrontierDp.Curve d = ExactCurve.solve(p, s, positive, w);
            assertArrayEquals(c.masks(), d.masks());
            assertArrayEquals(c.weight(), d.weight());
        }
        assertTrue(ExactCurve.applies(new GapConstraint(0, 3), pat("abc")));
        assertTrue(ExactCurve.applies(GapConstraint.unbounded(0), pat("abc")));
        assertFalse(ExactCurve.applies(GapConstraint.unbounded(1), pat("abc")));
    }
}
