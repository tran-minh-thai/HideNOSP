// SPDX-License-Identifier: MIT
package hidenosp.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Exhaustive counterparts of the flow computations, for the implementation check of SC1 only.
 *
 * <p>These methods enumerate occurrences and search over subsets, so they are exponential and must
 * never appear in a timed scenario. They mirror {@code occs} and {@code sup_no_brute} of the probe
 * script {@code nettree_checks.py}, which keeps the Java results comparable with the numbers
 * already recorded in section 8.2 of the design doc.
 */
public final class BruteForce {

    private BruteForce() {
    }

    /** Every occurrence of P in S under the gap constraint, as arrays of 0-based positions. */
    public static List<int[]> occurrences(Pattern pattern, EventSequence sequence, GapConstraint gap) {
        List<int[]> out = new ArrayList<>();
        int[] current = new int[pattern.length()];
        extend(pattern, sequence, gap, 0, -1, current, out);
        return out;
    }

    private static void extend(Pattern pattern, EventSequence sequence, GapConstraint gap,
                               int level, int previous, int[] current, List<int[]> out) {
        if (level == pattern.length()) {
            out.add(current.clone());
            return;
        }
        int n = sequence.length();
        int lo = level == 0 ? 0 : gap.firstNext(previous);
        int hi = level == 0 ? n - 1 : gap.lastNext(previous, n);
        for (int i = lo; i <= hi; i++) {
            if (sequence.at(i) != EventSequence.MASK && sequence.at(i) == pattern.at(level)) {
                current[level] = i;
                extend(pattern, sequence, gap, level + 1, i, current, out);
            }
        }
    }

    /**
     * sup_NO(P, S) by exhaustive search over sets of pairwise non-overlapping occurrences. Two
     * occurrences overlap when they share a position at the same level (design doc section 2).
     */
    public static int supNO(Pattern pattern, EventSequence sequence, GapConstraint gap) {
        List<int[]> all = occurrences(pattern, sequence, gap);
        return search(all, 0, new ArrayList<>(), 0);
    }

    private static int search(List<int[]> all, int index, List<int[]> chosen, int best) {
        if (chosen.size() + all.size() - index <= best) {
            return best;
        }
        if (index == all.size()) {
            return Math.max(best, chosen.size());
        }
        int[] candidate = all.get(index);
        boolean free = true;
        for (int[] c : chosen) {
            if (overlaps(candidate, c)) {
                free = false;
                break;
            }
        }
        if (free) {
            chosen.add(candidate);
            best = search(all, index + 1, chosen, best);
            chosen.remove(chosen.size() - 1);
        }
        return search(all, index + 1, chosen, best);
    }

    /** True when the two occurrences share a position at the same level. */
    public static boolean overlaps(int[] a, int[] b) {
        for (int j = 0; j < a.length; j++) {
            if (a[j] == b[j]) {
                return true;
            }
        }
        return false;
    }

    /**
     * The smallest number of masked positions that brings sup_NO down to {@code target}, by
     * exhaustive search over position subsets. P1 predicts F - target when mu(P) = 1; SC1 compares
     * the two.
     */
    public static int minMasks(Pattern pattern, EventSequence sequence, GapConstraint gap, int target) {
        int n = sequence.length();
        for (int k = 0; k <= n; k++) {
            if (existsMaskOfSize(pattern, sequence, gap, target, k, 0, new int[k], 0)) {
                return k;
            }
        }
        return n;
    }

    private static boolean existsMaskOfSize(Pattern pattern, EventSequence sequence, GapConstraint gap,
                                            int target, int k, int start, int[] chosen, int filled) {
        if (filled == k) {
            EventSequence masked = sequence;
            for (int idx = 0; idx < k; idx++) {
                masked = masked.mask(chosen[idx]);
            }
            return supNO(pattern, masked, gap) <= target;
        }
        for (int i = start; i < sequence.length(); i++) {
            chosen[filled] = i;
            if (existsMaskOfSize(pattern, sequence, gap, target, k, i + 1, chosen, filled + 1)) {
                return true;
            }
        }
        return false;
    }
}
