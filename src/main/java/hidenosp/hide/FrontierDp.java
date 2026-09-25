package hidenosp.hide;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact least masking of one pattern in one sequence, for any number of repeated items, when the
 * gap constraint has g- = 0.
 *
 * <p>For every target t it returns the least |R| with sup_NO(P, mask(S, R)) &le; t, and among the
 * sets of that size the least total position weight, with one such set. When mu(P) = 1 the count
 * is F - t by P1, but the weight for 0 &lt; t &lt; F is the open case of P6(e); when mu(P) &ge; 2
 * even the count was only bracketed, by P3.
 *
 * <p>Two facts turn the problem into a dynamic program over the positions of S.
 * <ol>
 *   <li>Menger on the masked Nettree (lemma L0 holds for every mu): sup_NO &le; t exactly when a
 *       set C of at most t nodes, together with the nodes of the masked positions, meets every
 *       occurrence path. The program chooses masks and at most t deleted nodes, and only has to
 *       leave no complete path: a question of reachability, not of flow.</li>
 *   <li>With g- = 0 a node (j, i) is reachable from level 1 exactly when the latest reachable node
 *       of level j - 1 before position i lies at most g+ + 1 positions back; an earlier one never
 *       serves where the latest does not. So the future depends on the past only through, per
 *       level, the distance back to its latest reachable node, capped at g+ + 1.</li>
 * </ol>
 * The state is that vector of m - 1 distances: at most (g+ + 2)^(m-1) states, times F + 1 budgets
 * of deleted nodes, times at most 1 + 2^mu(P) choices per position that carries a pattern item.
 * With g- &gt; 0 the second fact fails, a later node can be too close to serve, and
 * {@link #solve} refuses.
 */
public final class FrontierDp {

    /**
     * The exact curve of one sequence. For t = 0 .. supNO: {@code masks[t]} is the least number of
     * masked positions that bring sup_NO down to at most t, {@code weight[t]} the least total weight
     * among such sets, {@code positions[t]} one set achieving both, sorted. {@code states} is the
     * largest number of distinct states held at one position, for the complexity report.
     */
    public record Curve(int supNO, int[] masks, long[] weight, int[][] positions, int states) {
    }

    /** True when the gap constraint allows the program, that is g- = 0. */
    public static boolean applies(GapConstraint gap) {
        return gap.gmin() == 0;
    }

    /** Distance of a level with no reachable node; larger than any g + 1. */
    private static final int FAR = Integer.MAX_VALUE / 4;

    private FrontierDp() {
    }

    /** One partial solution: its cost and the positions it masked, shared as a persistent list. */
    private record Entry(int masks, long weight, Chain chain) {
        boolean betterThan(Entry other) {
            return other == null || masks < other.masks
                    || (masks == other.masks && weight < other.weight);
        }
    }

    private record Chain(int position, Chain previous) {
    }

    /**
     * Solves every target t = 0 .. sup_NO(P, S).
     *
     * @param positionWeight weight of masking each position of S, or null for all zero
     * @throws IllegalArgumentException when g- &gt; 0
     */
    public static Curve solve(Pattern pattern, EventSequence sequence, GapConstraint gap,
                              long[] positionWeight) {
        if (!applies(gap)) {
            throw new IllegalArgumentException("FrontierDp needs g- = 0, got " + gap
                    + "; with g- > 0 the latest reachable node no longer dominates");
        }
        int supNO = SupportNO.of(pattern, sequence, gap);
        int m = pattern.length();
        int n = sequence.length();
        boolean unbounded = gap.isUnbounded() || gap.gmax() >= n;
        int g = unbounded ? 0 : gap.gmax();
        // Stored value per level after a position: 0..g = distance back to the latest reachable
        // node, NONE = no node that can still serve. Unbounded: 0 = one exists, NONE = none.
        final int none = unbounded ? 1 : g + 1;
        final long base = none + 1L;
        int budgets = supNO + 1;

        Map<Long, Entry[]> current = new HashMap<>();
        int[] start = new int[Math.max(0, m - 1)];
        Arrays.fill(start, none);
        Entry[] first = new Entry[budgets];
        first[0] = new Entry(0, 0L, null);
        current.put(encode(start, base), first);
        int largest = 1;

        int last = -1;
        int[] a = new int[Math.max(0, m - 1)];
        int[] d = new int[Math.max(0, m - 1)];
        int[] next = new int[Math.max(0, m - 1)];
        for (int i = 0; i < n; i++) {
            int item = sequence.at(i);
            if (item == EventSequence.MASK) {
                continue;
            }
            List<Integer> levels = new ArrayList<>();
            for (int j = 0; j < m; j++) {
                if (pattern.at(j) == item) {
                    levels.add(j);
                }
            }
            if (levels.isEmpty()) {
                continue;
            }
            int delta = last < 0 ? 1 : i - last;
            long w = positionWeight == null ? 0L : positionWeight[i];
            Map<Long, Entry[]> after = new HashMap<>();
            for (Map.Entry<Long, Entry[]> state : current.entrySet()) {
                decode(state.getKey(), base, a);
                // Distance at position i back to the latest reachable node of each level, FAR
                // when there is none. It may equal g + 1, the NONE code of the stored values,
                // so it needs its own sentinel.
                for (int j = 0; j < m - 1; j++) {
                    d[j] = a[j] == none ? FAR : (unbounded ? 0 : a[j] + delta);
                }
                // Levels at position i whose node is reachable from level 1 before any choice.
                int capable = 0;
                int[] capableLevel = new int[levels.size()];
                for (int level : levels) {
                    boolean reach = level == 0 || d[level - 1] <= g + 1;
                    if (reach) {
                        capableLevel[capable++] = level;
                    }
                }
                Entry[] entries = state.getValue();
                for (int c = 0; c < budgets; c++) {
                    Entry e = entries[c];
                    if (e == null) {
                        continue;
                    }
                    // Mask position i: none of its nodes is reachable.
                    carry(d, next, m, g, none, -1, 0, capableLevel);
                    relax(after, encode(next, base), budgets, c,
                            new Entry(e.masks() + 1, e.weight() + w, new Chain(i, e.chain())));
                    // Keep position i and delete a subset of its reachable nodes (budget).
                    for (int subset = 0; subset < (1 << capable); subset++) {
                        int deleted = Integer.bitCount(subset);
                        if (c + deleted >= budgets) {
                            continue;
                        }
                        int kept = (~subset) & ((1 << capable) - 1);
                        boolean complete = false;
                        for (int k = 0; k < capable; k++) {
                            if ((kept & (1 << k)) != 0 && capableLevel[k] == m - 1) {
                                complete = true;
                            }
                        }
                        if (complete) {
                            continue;
                        }
                        carry(d, next, m, g, none, kept, capable, capableLevel);
                        relax(after, encode(next, base), budgets, c + deleted, e);
                    }
                }
            }
            prune(after);
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
                    if (entries[c] != null && entries[c].betterThan(best)) {
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
        if (masks[supNO] != 0) {
            throw new IllegalStateException("sup_NO = " + supNO + " from the flow, but the program "
                    + "needs " + masks[supNO] + " masks to reach it; Menger says 0");
        }
        return new Curve(supNO, masks, weight, positions, largest);
    }

    /** The stored values after position i: 0 for the kept reachable levels, else aged or NONE. */
    private static void carry(int[] d, int[] next, int m, int g, int none, int kept, int capable,
                              int[] capableLevel) {
        for (int j = 0; j < m - 1; j++) {
            // A node at distance g + 1 serves position i but no later one.
            next[j] = d[j] > g ? none : d[j];
        }
        if (kept < 0) {
            return;
        }
        for (int k = 0; k < capable; k++) {
            if ((kept & (1 << k)) != 0) {
                next[capableLevel[k]] = 0;
            }
        }
    }

    private static void relax(Map<Long, Entry[]> map, long key, int budgets, int c, Entry candidate) {
        Entry[] entries = map.computeIfAbsent(key, k -> new Entry[budgets]);
        if (candidate.betterThan(entries[c])) {
            entries[c] = candidate;
        }
    }

    /** Drops an entry that uses more budget than another of its state without costing less. */
    private static void prune(Map<Long, Entry[]> map) {
        for (Entry[] entries : map.values()) {
            Entry best = null;
            for (int c = 0; c < entries.length; c++) {
                if (entries[c] == null) {
                    continue;
                }
                if (best != null && !entries[c].betterThan(best)) {
                    entries[c] = null;
                } else {
                    best = entries[c];
                }
            }
        }
    }

    private static long encode(int[] values, long base) {
        long key = 0;
        for (int j = values.length - 1; j >= 0; j--) {
            key = key * base + values[j];
        }
        return key;
    }

    private static void decode(long key, long base, int[] out) {
        for (int j = 0; j < out.length; j++) {
            out[j] = (int) (key % base);
            key /= base;
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
