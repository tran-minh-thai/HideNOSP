// SPDX-License-Identifier: MIT
package hidenosp.index;

import hidenosp.core.Nettree;

/**
 * Forward and backward path counts on a Nettree (design doc section 9.1).
 *
 * <p>{@code forward[v]} counts the paths from level 1 to v and {@code backward[v]} the paths from v
 * to level m, so their product is the number of occurrences through v. One pass over the Nettree
 * therefore gives the quantity delta of every position, replacing the per-position dynamic program
 * of theorem 2 of [R20]; the probe {@code probe/timing.py} measured roughly eighty times faster at
 * n = 400, and SC6 repeats the comparison in Java.
 *
 * <p>Counts grow exponentially, so both arrays saturate at {@link #SATURATED}. A saturated entry is
 * still usable as a comparison key, which is all the heuristic of [R20] needs, but it must not be
 * printed as an exact count. {@link #isExact()} reports whether saturation occurred.
 */
public final class PathCounts {

    /** Ceiling for the saturating arithmetic; any count at this value is only a lower bound. */
    public static final long SATURATED = Long.MAX_VALUE / 4;

    private final Nettree nettree;
    private final long[] forward;
    private final long[] backward;
    private final boolean exact;

    private PathCounts(Nettree nettree, long[] forward, long[] backward, boolean exact) {
        this.nettree = nettree;
        this.forward = forward;
        this.backward = backward;
        this.exact = exact;
    }

    /**
     * Computes both directions in one pass each. Nodes are numbered in (level, position) order,
     * which is a topological order of the Nettree, so a single ascending and a single descending
     * sweep suffice. Nodes flagged in {@code removed} are treated as absent (remark O1).
     */
    public static PathCounts of(Nettree nettree, boolean[] removed) {
        int n = nettree.nodeCount();
        long[] forward = new long[n];
        long[] backward = new long[n];
        boolean exact = true;
        for (int v = 0; v < n; v++) {
            if (removed != null && removed[v]) {
                continue;
            }
            if (nettree.isSource(v)) {
                forward[v] = 1;
            }
            for (int u : nettree.in(v)) {
                if (removed == null || !removed[u]) {
                    long sum = forward[v] + forward[u];
                    if (sum >= SATURATED) {
                        sum = SATURATED;
                        exact = false;
                    }
                    forward[v] = sum;
                }
            }
        }
        for (int v = n - 1; v >= 0; v--) {
            if (removed != null && removed[v]) {
                continue;
            }
            if (nettree.isSink(v)) {
                backward[v] = 1;
            }
            for (int u : nettree.out(v)) {
                if (removed == null || !removed[u]) {
                    long sum = backward[v] + backward[u];
                    if (sum >= SATURATED) {
                        sum = SATURATED;
                        exact = false;
                    }
                    backward[v] = sum;
                }
            }
        }
        return new PathCounts(nettree, forward, backward, exact);
    }

    public boolean isExact() {
        return exact;
    }

    public long forward(int v) {
        return forward[v];
    }

    public long backward(int v) {
        return backward[v];
    }

    /** Occurrences of P in S that pass through node v. */
    public long through(int v) {
        long product = forward[v] * backward[v];
        if (forward[v] != 0 && product / forward[v] != backward[v]) {
            return SATURATED;
        }
        return Math.min(product, SATURATED);
    }

    /**
     * Occurrences through position i, summed over the nodes that position carries. This is the key
     * of the local heuristic of [R20], kept here so the baseline and CUT-HIDE read the same index.
     */
    public long throughPosition(int i) {
        long total = 0;
        for (int v : nettree.nodesAtPosition(i)) {
            total = Math.min(total + through(v), SATURATED);
        }
        return total;
    }

    /** Total number of occurrences of P in S, the size of the match set of [R20]. */
    public long totalOccurrences() {
        long total = 0;
        for (int v = 0; v < forward.length; v++) {
            if (nettree.isSink(v)) {
                total = Math.min(total + forward[v], SATURATED);
            }
        }
        return total;
    }
}
