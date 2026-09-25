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
        int n = curves.size();
        long infinite = Long.MAX_VALUE / 4;
        long[][] best = new long[n + 1][total + 1];
        int[][] choice = new int[n + 1][total + 1];
        for (long[] row : best) {
            java.util.Arrays.fill(row, infinite);
        }
        best[0][0] = 0;
        for (int s = 0; s < n; s++) {
            CostCurve curve = curves.get(s);
            int limit = curve.supNO();
            for (int used = 0; used <= total; used++) {
                if (best[s][used] == infinite) {
                    continue;
                }
                for (int k = 0; k <= limit && used + k <= total; k++) {
                    long value = best[s][used] + curve.cost(k);
                    if (value < best[s + 1][used + k]) {
                        best[s + 1][used + k] = value;
                        choice[s + 1][used + k] = k;
                    }
                }
            }
        }
        if (best[n][total] == infinite) {
            throw new IllegalArgumentException("cannot place " + total + " masks within the curves");
        }
        int[] counts = new int[n];
        int used = total;
        for (int s = n; s > 0; s--) {
            counts[s - 1] = choice[s][used];
            used -= choice[s][used];
        }
        return new Result(counts, best[n][total]);
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
        int n = masks.length;
        long infinite = Long.MAX_VALUE / 4;
        long[] bestMasks = new long[needed + 1];
        long[] bestWeight = new long[needed + 1];
        java.util.Arrays.fill(bestMasks, infinite);
        java.util.Arrays.fill(bestWeight, infinite);
        bestMasks[0] = 0;
        bestWeight[0] = 0;
        int[][] reductionChosen = new int[n][];
        int[][] cameFrom = new int[n][];
        for (int s = 0; s < n; s++) {
            int f = masks[s].length - 1;
            long[] nextMasks = new long[needed + 1];
            long[] nextWeight = new long[needed + 1];
            java.util.Arrays.fill(nextMasks, infinite);
            java.util.Arrays.fill(nextWeight, infinite);
            reductionChosen[s] = new int[needed + 1];
            cameFrom[s] = new int[needed + 1];
            for (int reached = 0; reached <= needed; reached++) {
                if (bestMasks[reached] == infinite) {
                    continue;
                }
                for (int k = 0; k <= f; k++) {
                    int to = Math.min(needed, reached + k);
                    long m = bestMasks[reached] + masks[s][f - k];
                    long w = bestWeight[reached] + weight[s][f - k];
                    if (m < nextMasks[to] || (m == nextMasks[to] && w < nextWeight[to])) {
                        nextMasks[to] = m;
                        nextWeight[to] = w;
                        reductionChosen[s][to] = k;
                        cameFrom[s][to] = reached;
                    }
                }
            }
            bestMasks = nextMasks;
            bestWeight = nextWeight;
        }
        if (bestMasks[needed] == infinite) {
            throw new IllegalArgumentException("cannot reduce sup_NO by " + needed
                    + " within the curves");
        }
        int[] remaining = new int[n];
        int at = needed;
        for (int s = n - 1; s >= 0; s--) {
            int f = masks[s].length - 1;
            remaining[s] = f - reductionChosen[s][at];
            at = cameFrom[s][at];
        }
        return remaining;
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
