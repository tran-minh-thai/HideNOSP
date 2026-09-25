package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.hide.Allocation;
import hidenosp.hide.CostCurve;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The allocations against every split, on small instances. */
class AllocationTest {

    @Test
    @DisplayName("the allocation of P7 equals the best split over every count vector")
    void p7AgainstEverySplit() {
        Random random = new Random(31);
        int checked = 0;
        for (int trial = 0; checked < 300; trial++) {
            int n = 1 + random.nextInt(4);
            List<CostCurve> curves = new ArrayList<>();
            int sum = 0;
            for (int s = 0; s < n; s++) {
                int length = random.nextInt(9);
                int[] events = new int[length];
                double[] weights = new double[length];
                for (int i = 0; i < length; i++) {
                    events[i] = random.nextInt(2);
                    weights[i] = random.nextInt(5);
                }
                Nettree nt = Nettree.build(new Pattern(0, 1), new EventSequence(s, events),
                        GapConstraint.of(0, 2));
                CostCurve curve = CostCurve.build(nt, weights, true);
                curves.add(curve);
                sum += curve.supNO();
            }
            if (sum == 0) {
                continue;
            }
            int total = 1 + random.nextInt(sum);
            Allocation.Result result = Allocation.solve(curves, total);
            long best = bestSplit(curves, 0, total);
            assertEquals(best, result.weight(), "optimum");
            long recomputed = 0;
            int placed = 0;
            for (int s = 0; s < n; s++) {
                assertTrue(result.counts()[s] <= curves.get(s).supNO());
                recomputed += curves.get(s).cost(result.counts()[s]);
                placed += result.counts()[s];
            }
            assertEquals(total, placed, "every mask placed");
            assertEquals(result.weight(), recomputed, "the counts realise the weight");
            checked++;
        }
    }

    private static long bestSplit(List<CostCurve> curves, int from, int left) {
        if (from == curves.size()) {
            return left == 0 ? 0 : Long.MAX_VALUE / 4;
        }
        long best = Long.MAX_VALUE / 4;
        CostCurve c = curves.get(from);
        for (int k = 0; k <= c.supNO() && k <= left; k++) {
            long rest = bestSplit(curves, from + 1, left - k);
            best = Math.min(best, c.cost(k) + rest);
        }
        return best;
    }

    @Test
    @DisplayName("the exact allocation equals the best remaining-support vector, both orders")
    void exactAgainstEveryVector() {
        Random random = new Random(37);
        int checked = 0;
        int differentOrders = 0;
        for (int trial = 0; checked < 400; trial++) {
            int n = 1 + random.nextInt(4);
            int[][] masks = new int[n][];
            long[][] weight = new long[n][];
            int sum = 0;
            for (int s = 0; s < n; s++) {
                int f = random.nextInt(5);
                masks[s] = new int[f + 1];
                weight[s] = new long[f + 1];
                // Non-increasing in t, as every exact curve is; masks[f] = weight[f] = 0.
                for (int t = f - 1; t >= 0; t--) {
                    masks[s][t] = masks[s][t + 1] + random.nextInt(2);
                    weight[s][t] = weight[s][t + 1] + random.nextInt(6);
                }
                sum += f;
            }
            if (sum == 0) {
                continue;
            }
            int needed = 1 + random.nextInt(sum);
            for (boolean weightFirst : new boolean[] {false, true}) {
                int[] remaining = Allocation.solveExact(masks, weight, needed, weightFirst);
                long[] got = cost(masks, weight, remaining);
                int reduced = 0;
                for (int s = 0; s < n; s++) {
                    reduced += masks[s].length - 1 - remaining[s];
                }
                assertTrue(reduced >= needed, "the need is met");
                long[] best = bestVector(masks, weight, needed, weightFirst);
                assertEquals(best[0], got[0], "first cost, weightFirst=" + weightFirst);
                assertEquals(best[1], got[1], "second cost, weightFirst=" + weightFirst);
                if (!weightFirst) {
                    assertEquals(got[0], Allocation.leastMasks(masks, needed), "value-only form");
                }
            }
            long[] a = cost(masks, weight, Allocation.solveExact(masks, weight, needed, false));
            long[] b = cost(masks, weight, Allocation.solveExact(masks, weight, needed, true));
            if (a[0] != b[0] || a[1] != b[1]) {
                differentOrders++;
            }
            checked++;
        }
        assertTrue(differentOrders >= 20, "instances where the two orders disagree: " + differentOrders);
    }

    /** {masks, weight} of a remaining-support vector. */
    private static long[] cost(int[][] masks, long[][] weight, int[] remaining) {
        long m = 0;
        long w = 0;
        for (int s = 0; s < masks.length; s++) {
            m += masks[s][remaining[s]];
            w += weight[s][remaining[s]];
        }
        return new long[] {m, w};
    }

    /** Best {first, second} cost in the given order over every vector that meets the need. */
    private static long[] bestVector(int[][] masks, long[][] weight, int needed, boolean weightFirst) {
        int n = masks.length;
        int[] t = new int[n];
        long[] best = {Long.MAX_VALUE, Long.MAX_VALUE};
        while (true) {
            int reduced = 0;
            for (int s = 0; s < n; s++) {
                reduced += masks[s].length - 1 - t[s];
            }
            if (reduced >= needed) {
                long[] c = cost(masks, weight, t);
                long first = weightFirst ? c[1] : c[0];
                long second = weightFirst ? c[0] : c[1];
                if (first < best[0] || (first == best[0] && second < best[1])) {
                    best = new long[] {first, second};
                }
            }
            int s = 0;
            while (s < n && t[s] == masks[s].length - 1) {
                t[s] = 0;
                s++;
            }
            if (s == n) {
                break;
            }
            t[s]++;
        }
        return weightFirst ? new long[] {best[1], best[0]} : best;
    }
}
