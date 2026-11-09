// SPDX-License-Identifier: MIT
package hidenosp.hide;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.util.Deadline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact least masking of one pattern in one sequence for any gap [g-, g+] with a finite g+, the
 * dynamic program of the corollary for g- &gt; 0.
 *
 * <p>The program is the one of {@link FrontierDp} with another state. With g- = 0 the latest
 * reachable node of a level decides whether a later node of the next level is reachable; with
 * g- &gt; 0 the parent interval of (j, i) is [i - g+ - 1, i - g- - 1], which does not contain i - 1,
 * so one position per level no longer suffices. The state is, for every level j &lt; m, the set of
 * positions among the g+ + 1 latest ones that carry a reachable node of level j which is not in
 * the cut: a bit mask of g+ + 1 bits per level, (m - 1)(g+ + 1) bits in all. A node (j + 1, i) is
 * reachable exactly when it is not removed and the mask of level j has a bit at a distance
 * i - 1 - i' in [g-, g+].
 *
 * <p>The choices at a position and the pruning are those of {@link FrontierDp}: mask the position,
 * or keep it and put a subset of its reachable nodes into the cut C, every reachable leaf included;
 * per state and budget |C| only the entry least in the order is kept. The program is also correct
 * for g- = 0, which the tests use to compare it with {@link FrontierDp}.
 */
public final class WindowDp {

    /** The largest number of state bits, so that one state fits in a long. */
    static final int MAX_BITS = 62;

    private WindowDp() {
    }

    /**
     * True when the program applies to the pattern under the gap: g+ finite and
     * (m - 1)(g+ + 1) state bits at most {@link #MAX_BITS}.
     */
    public static boolean applies(GapConstraint gap, Pattern pattern) {
        if (gap.isUnbounded()) {
            return false;
        }
        long bits = (long) Math.max(0, pattern.length() - 1) * (gap.gmax() + 1L);
        return bits <= MAX_BITS;
    }

    private record Entry(int masks, long weight, Chain chain) {
        boolean betterThan(Entry other, FrontierDp.Order order) {
            return other == null || order.before(masks, weight, other.masks, other.weight);
        }
    }

    private record Chain(int position, Chain previous) {
    }

    /**
     * Solves every target t = 0 .. sup_NO(P, S) under the order (a * weight + b * masks, masks,
     * weight) of {@code order}.
     *
     * @param positionWeight weight of masking each position of S, or null for all zero
     * @throws IllegalArgumentException when the program does not apply, see {@link #applies}
     */
    public static FrontierDp.Curve solve(Pattern pattern, EventSequence sequence, GapConstraint gap,
                                         long[] positionWeight, FrontierDp.Order order) {
        if (!applies(gap, pattern)) {
            throw new IllegalArgumentException("WindowDp needs a finite g+ and (m - 1)(g+ + 1) <= "
                    + MAX_BITS + " state bits; got pattern length " + pattern.length() + ", gap " + gap);
        }
        int supNO = SupportNO.of(pattern, sequence, gap);
        int m = pattern.length();
        int n = sequence.length();
        int gmin = gap.gmin();
        int gmax = gap.gmax();
        int width = gmax + 1;
        long windowMask = width >= 64 ? -1L : (1L << width) - 1;
        // Bits b with g- <= b <= g+: the distances i - 1 - i' of the parents of a node at i.
        long parentMask = windowMask & ~((1L << gmin) - 1);
        int levels = Math.max(0, m - 1);
        int budgets = supNO + 1;

        Map<Long, Entry[]> current = new HashMap<>();
        Entry[] first = new Entry[budgets];
        first[0] = new Entry(0, 0L, null);
        current.put(0L, first);
        int largest = 1;

        int last = -1;
        long[] window = new long[levels];
        long[] next = new long[levels];
        for (int i = 0; i < n; i++) {
            Deadline.check();
            int item = sequence.at(i);
            if (item == EventSequence.MASK) {
                continue;
            }
            List<Integer> carried = new ArrayList<>();
            for (int j = 0; j < m; j++) {
                if (pattern.at(j) == item) {
                    carried.add(j);
                }
            }
            if (carried.isEmpty()) {
                continue;
            }
            // In a state after position `last`, bit c of a level stands for position last - c.
            int delta = last < 0 ? 0 : i - last;
            long w = positionWeight == null ? 0L : positionWeight[i];
            Map<Long, Entry[]> after = new HashMap<>();
            for (Map.Entry<Long, Entry[]> state : current.entrySet()) {
                decode(state.getKey(), width, windowMask, window);
                // Bit b of `seen` stands for position i - 1 - b, the distance tested by the gap.
                int capable = 0;
                int[] capableLevel = new int[carried.size()];
                for (int level : carried) {
                    boolean reach;
                    if (level == 0) {
                        reach = true;
                    } else {
                        long seen = last < 0 ? 0L : shift(window[level - 1], delta - 1);
                        reach = (seen & parentMask) != 0;
                    }
                    if (reach) {
                        capableLevel[capable++] = level;
                    }
                }
                // The windows after position i, before the decision at i: every bit moves by delta.
                for (int j = 0; j < levels; j++) {
                    next[j] = last < 0 ? 0L : shift(window[j], delta) & windowMask;
                }
                long masked = encode(next, width);
                Entry[] entries = state.getValue();
                for (int c = 0; c < budgets; c++) {
                    Entry e = entries[c];
                    if (e == null) {
                        continue;
                    }
                    relax(after, masked, budgets, c,
                            new Entry(e.masks() + 1, e.weight() + w, new Chain(i, e.chain())), order);
                    for (int subset = 0; subset < (1 << capable); subset++) {
                        int deleted = Integer.bitCount(subset);
                        if (c + deleted >= budgets) {
                            continue;
                        }
                        int kept = (~subset) & ((1 << capable) - 1);
                        boolean complete = false;
                        long key = masked;
                        for (int k = 0; k < capable; k++) {
                            if ((kept & (1 << k)) == 0) {
                                continue;
                            }
                            if (capableLevel[k] == m - 1) {
                                complete = true;
                                break;
                            }
                            key |= 1L << (capableLevel[k] * width);
                        }
                        if (complete) {
                            continue;
                        }
                        relax(after, key, budgets, c + deleted, e, order);
                    }
                }
            }
            prune(after, order);
            current = after;
            largest = Math.max(largest, current.size());
            last = i;
        }

        int[] masks = new int[budgets];
        long[] weight = new long[budgets];
        int[][] positions = new int[budgets][];
        for (int t = 0; t < budgets; t++) {
            Entry best = null;
            for (Entry[] entries : current.values()) {
                for (int c = 0; c <= t; c++) {
                    if (entries[c] != null && entries[c].betterThan(best, order)) {
                        best = entries[c];
                    }
                }
            }
            if (best == null) {
                throw new IllegalStateException("no feasible masking for t = " + t
                        + "; masking every position must always be feasible");
            }
            masks[t] = best.masks();
            weight[t] = best.weight();
            positions[t] = toArray(best.chain());
        }
        if (masks[supNO] != 0 || weight[supNO] != 0) {
            throw new IllegalStateException("sup_NO = " + supNO + " from the flow, but the program "
                    + "needs " + masks[supNO] + " masks to reach it; Menger says 0");
        }
        return new FrontierDp.Curve(supNO, masks, weight, positions, largest);
    }

    /** {@code value} shifted left by {@code by} bits, 0 once every bit has left a long. */
    private static long shift(long value, int by) {
        if (by <= 0) {
            return by == 0 ? value : value >>> -by;
        }
        return by >= 64 ? 0L : value << by;
    }

    private static long encode(long[] window, int width) {
        long key = 0;
        for (int j = 0; j < window.length; j++) {
            key |= window[j] << (j * width);
        }
        return key;
    }

    private static void decode(long key, int width, long windowMask, long[] out) {
        for (int j = 0; j < out.length; j++) {
            out[j] = (key >>> (j * width)) & windowMask;
        }
    }

    private static void relax(Map<Long, Entry[]> map, long key, int budgets, int c, Entry candidate,
                              FrontierDp.Order order) {
        Entry[] entries = map.computeIfAbsent(key, k -> new Entry[budgets]);
        if (candidate.betterThan(entries[c], order)) {
            entries[c] = candidate;
        }
    }

    private static void prune(Map<Long, Entry[]> map, FrontierDp.Order order) {
        for (Entry[] entries : map.values()) {
            Entry best = null;
            for (int c = 0; c < entries.length; c++) {
                if (entries[c] == null) {
                    continue;
                }
                if (best != null && !entries[c].betterThan(best, order)) {
                    entries[c] = null;
                } else {
                    best = entries[c];
                }
            }
        }
    }

    private static int[] toArray(Chain chain) {
        List<Integer> out = new ArrayList<>();
        for (Chain c = chain; c != null; c = c.previous()) {
            out.add(c.position());
        }
        int[] array = new int[out.size()];
        for (int k = 0; k < array.length; k++) {
            array[k] = out.get(k);
        }
        Arrays.sort(array);
        return array;
    }
}
