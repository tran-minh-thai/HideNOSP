// SPDX-License-Identifier: MIT
package hidenosp.core;

import hidenosp.core.flow.NettreeFlow;

/**
 * sup_NO, the non-overlapping support of [R1]: the largest number of pairwise non-overlapping
 * occurrences of P in S, and its sum over the database.
 *
 * <p>Every value here comes from a max flow on the Nettree (lemma L0). The exhaustive counterpart
 * used to check this implementation lives in {@link BruteForce} and is only called by SC1.
 */
public final class SupportNO {

    private SupportNO() {
    }

    /**
     * sup_NO(P, S).
     *
     * <p>Two shortcuts stand in front of the max flow, and neither changes the value. A pattern of
     * length one has single positions as its occurrences, and non-overlapping then means distinct,
     * so its support is the number of matching positions. And if some level of the Nettree would
     * carry no node, no occurrence exists at all. Both matter on the real datasets, where mining
     * F_sigma evaluates a great many candidates that fail immediately.
     */
    public static int of(Pattern pattern, EventSequence sequence, GapConstraint gap) {
        if (pattern.length() == 1) {
            int item = pattern.at(0);
            int count = 0;
            for (int i = 0; i < sequence.length(); i++) {
                if (sequence.at(i) != EventSequence.MASK && sequence.at(i) == item) {
                    count++;
                }
            }
            return count;
        }
        for (int j = 0; j < pattern.length(); j++) {
            boolean present = false;
            for (int i = 0; i < sequence.length() && !present; i++) {
                present = sequence.at(i) != EventSequence.MASK && sequence.at(i) == pattern.at(j);
            }
            if (!present) {
                return 0;
            }
        }
        Nettree nettree = Nettree.build(pattern, sequence, gap);
        return nettree.nodeCount() == 0 ? 0 : new NettreeFlow(nettree).supNO();
    }

    /** sup_NO(P, D) = sum over S of sup_NO(P, S). */
    public static long of(Pattern pattern, SequenceDatabase database, GapConstraint gap) {
        long total = 0;
        for (EventSequence s : database.sequences()) {
            total += of(pattern, s, gap);
        }
        return total;
    }

    /** Per-sequence values F_S = sup_NO(P, S), in database order. */
    public static int[] perSequence(Pattern pattern, SequenceDatabase database, GapConstraint gap) {
        int[] out = new int[database.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = of(pattern, database.get(i), gap);
        }
        return out;
    }

    /** sup_seq(P, D): the number of sequences that contain P, the support used by [R20] and [R3]. */
    public static int sequenceSupport(Pattern pattern, SequenceDatabase database, GapConstraint gap) {
        int c = 0;
        for (EventSequence s : database.sequences()) {
            if (of(pattern, s, gap) > 0) {
                c++;
            }
        }
        return c;
    }

    /**
     * The largest sup_NO reachable without the gap constraint, used by the effective-constraint
     * ratio beta.
     */
    public static long withoutGap(Pattern pattern, SequenceDatabase database) {
        return of(pattern, database, GapConstraint.unbounded(0));
    }
}
