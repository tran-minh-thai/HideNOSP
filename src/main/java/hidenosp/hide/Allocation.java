package hidenosp.hide;

import java.util.List;

/**
 * Step 6 of section 9.2, the allocation of remark P7: choose how many masks each sequence takes.
 *
 * <p>For HIDE-DB with one sensitive pattern of distinct items, P1 says the total is fixed at
 * sum(F_S) - sigma + 1 and every split into per-sequence counts k_S reaches it, so the only freedom
 * left is where the masks go. Minimising the surrogate weight over that freedom is
 *
 * <pre>min sum over S of c_S(k_S) subject to sum k_S = K and 0 &lt;= k_S &lt;= F_S.</pre>
 *
 * <p>A greedy walk over marginal costs would be correct if every c_S were convex, which the probe
 * of P7 disproved, so this is a dynamic program over the curves, O(N K F_max).
 *
 * <p>HIDE-SEQ does not use this class: there k_S = max(0, F_S - r + 1) is forced per sequence.
 */
public final class Allocation {

    /** The chosen counts and the total surrogate weight they cost. */
    public record Result(int[] counts, long weight) {
    }

    private Allocation() {
    }

    /**
     * @param curves one cost curve per sequence, in database order
     * @param total K, the total number of masks to distribute
     */
    public static Result solve(List<CostCurve> curves, int total) {
        // Memory: two rows of values and one short per (sequence with support, reachable sum).
        // Full (n + 1) x (K + 1) tables of long and int needed 9 to 12 GB on the largest MSNBC
        // patterns, beyond the heap of the protocol.
        int n = curves.size();
        long infinite = Long.MAX_VALUE / 4;
        long[] best = new long[total + 1];
        java.util.Arrays.fill(best, infinite);
        best[0] = 0;
        short[][] choice = new short[n][];
        int reachable = 0;
        for (int s = 0; s < n; s++) {
            CostCurve curve = curves.get(s);
            int limit = curve.supNO();
            if (limit == 0) {
                continue;
            }
            requireShort(limit);
            int top = Math.min(total, reachable + limit);
            long[] next = new long[total + 1];
            java.util.Arrays.fill(next, infinite);
            choice[s] = new short[top + 1];
            for (int used = 0; used <= reachable; used++) {
                if (best[used] == infinite) {
                    continue;
                }
                for (int k = 0; k <= limit && used + k <= total; k++) {
                    long value = best[used] + curve.cost(k);
                    if (value < next[used + k]) {
                        next[used + k] = value;
                        choice[s][used + k] = (short) k;
                    }
                }
            }
            best = next;
            reachable = top;
        }
        if (best[total] == infinite) {
            throw new IllegalArgumentException("cannot place " + total + " masks within the curves");
        }
        int[] counts = new int[n];
        int used = total;
        for (int s = n - 1; s >= 0; s--) {
            if (choice[s] == null) {
                continue;
            }
            counts[s] = choice[s][used];
            used -= counts[s];
        }
        return new Result(counts, best[total]);
    }

    private static void requireShort(int supNO) {
        if (supNO > Short.MAX_VALUE) {
            throw new IllegalArgumentException("sup_NO of one sequence is " + supNO
                    + ", above the " + Short.MAX_VALUE + " the allocation table can record");
        }
    }

    /**
     * The allocation for exact curves of any mu(P) (theorem P8): choose a remaining support t_S per
     * sequence so that sum over S of t_S &le; sigma - 1, minimising the number of masks and then the
     * weight. A mask may lower sup_NO by more than one when mu(P) &ge; 2, so the program runs on the
     * reduction F_S - t_S capped at the need K; for mu(P) = 1 it returns what {@link #solve} does.
     *
     * @param masks masks[s][t], the least masks that bring sequence s down to t, for t = 0 .. F_s
     * @param weight weight[s][t], the least weight among those
     * @param needed K = sum F_S - sigma + 1, at least 1
     * @return the remaining support t_S chosen for every sequence
     */
    public static int[] solveExact(int[][] masks, long[][] weight, int needed) {
        return solveExact(masks, weight, needed, false);
    }

    /**
     * As {@link #solveExact(int[][], long[][], int)}; with {@code weightFirst} the total weight is
     * minimised first and the masks break ties, the order of FrontierDp.Objective.WEIGHT_THEN_MASKS.
     */
    public static int[] solveExact(int[][] masks, long[][] weight, int needed, boolean weightFirst) {
        return solveExact(masks, weight, needed, weightFirst ? new FrontierDp.Order(1, 0)
                : new FrontierDp.Order(0, 1));
    }

    /**
     * As {@link #solveExact(int[][], long[][], int)} under the lexicographic order of
     * {@code order}, (a * weight + b * masks, masks, weight): the allocation step of proposition
     * P10(a). The curves must have been computed under the same order.
     */
    public static int[] solveExact(int[][] masks, long[][] weight, int needed,
                                   FrontierDp.Order order) {
        // The reductions add up to exactly K. Lowering a sequence by more than it has to never
        // helps, because every curve is non-increasing in t (FrontierDpTest checks it), so the
        // exact sum loses nothing and the choice alone identifies the predecessor.
        int n = masks.length;
        long infinite = Long.MAX_VALUE / 4;
        long[] bestMasks = new long[needed + 1];
        long[] bestWeight = new long[needed + 1];
        java.util.Arrays.fill(bestMasks, infinite);
        java.util.Arrays.fill(bestWeight, infinite);
        bestMasks[0] = 0;
        bestWeight[0] = 0;
        short[][] choice = new short[n][];
        int reachable = 0;
        for (int s = 0; s < n; s++) {
            int f = masks[s].length - 1;
            requireShort(f);
            int top = Math.min(needed, reachable + f);
            long[] nextMasks = new long[needed + 1];
            long[] nextWeight = new long[needed + 1];
            java.util.Arrays.fill(nextMasks, infinite);
            java.util.Arrays.fill(nextWeight, infinite);
            choice[s] = new short[top + 1];
            for (int reached = 0; reached <= reachable; reached++) {
                if (bestMasks[reached] == infinite) {
                    continue;
                }
                for (int k = 0; k <= f && reached + k <= needed; k++) {
                    int to = reached + k;
                    long m = bestMasks[reached] + masks[s][f - k];
                    long w = bestWeight[reached] + weight[s][f - k];
                    boolean better = nextMasks[to] == infinite
                            || order.before(m, w, nextMasks[to], nextWeight[to]);
                    if (better) {
                        nextMasks[to] = m;
                        nextWeight[to] = w;
                        choice[s][to] = (short) k;
                    }
                }
            }
            bestMasks = nextMasks;
            bestWeight = nextWeight;
            reachable = top;
        }
        if (bestMasks[needed] == infinite) {
            throw new IllegalArgumentException("cannot reduce sup_NO by " + needed
                    + " within the curves");
        }
        int[] remaining = new int[n];
        int at = needed;
        for (int s = n - 1; s >= 0; s--) {
            int f = masks[s].length - 1;
            int k = choice[s][at];
            remaining[s] = f - k;
            at -= k;
        }
        return remaining;
    }

    /**
     * The least total of masks of {@link #solveExact} without the sets, in O(K) memory: what SC2
     * needs for |R| star.
     */
    public static long leastMasks(int[][] masks, int needed) {
        long infinite = Long.MAX_VALUE / 4;
        long[] best = new long[needed + 1];
        java.util.Arrays.fill(best, infinite);
        best[0] = 0;
        int reachable = 0;
        for (int[] curve : masks) {
            int f = curve.length - 1;
            int top = Math.min(needed, reachable + f);
            long[] next = new long[needed + 1];
            java.util.Arrays.fill(next, infinite);
            for (int reached = 0; reached <= reachable; reached++) {
                if (best[reached] == infinite) {
                    continue;
                }
                for (int k = 0; k <= f && reached + k <= needed; k++) {
                    long m = best[reached] + curve[f - k];
                    if (m < next[reached + k]) {
                        next[reached + k] = m;
                    }
                }
            }
            best = next;
            reachable = top;
        }
        if (best[needed] == infinite) {
            throw new IllegalArgumentException("cannot reduce sup_NO by " + needed
                    + " within the curves");
        }
        return best[needed];
    }

    /**
     * The greedy alternative compared against the dynamic program at SC7: repeatedly give one more
     * mask to the sequence with the smallest marginal cost. Correct only for convex curves.
     */
    public static Result greedy(List<CostCurve> curves, int total) {
        int n = curves.size();
        int[] counts = new int[n];
        long weight = 0;
        for (int step = 0; step < total; step++) {
            int bestIndex = -1;
            long bestDelta = Long.MAX_VALUE;
            for (int s = 0; s < n; s++) {
                CostCurve curve = curves.get(s);
                if (counts[s] >= curve.supNO()) {
                    continue;
                }
                long delta = curve.cost(counts[s] + 1) - curve.cost(counts[s]);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    bestIndex = s;
                }
            }
            if (bestIndex < 0) {
                throw new IllegalArgumentException("cannot place " + total + " masks within the curves");
            }
            counts[bestIndex]++;
            weight += bestDelta;
        }
        return new Result(counts, weight);
    }
}
