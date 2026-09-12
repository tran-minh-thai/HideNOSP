package hidenosp.core;

import hidenosp.core.flow.NettreeFlow;

/**
 * sup_NO, the non-overlapping support of [R1]: the largest number of pairwise non-overlapping
 * occurrences of P in S, and its sum over the database (design doc section 2).
 *
 * <p>Every value here comes from a max flow on the Nettree (lemma L0). The exhaustive counterpart
 * used to check this implementation lives in {@link BruteForce} and is only called by SC1.
 */
public final class SupportNO {

    private SupportNO() {
    }

    /** sup_NO(P, S). */
    public static int of(Pattern pattern, EventSequence sequence, GapConstraint gap) {
        return new NettreeFlow(Nettree.build(pattern, sequence, gap)).supNO();
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
     * ratio beta of section 8.4.
     */
    public static long withoutGap(Pattern pattern, SequenceDatabase database) {
        return of(pattern, database, GapConstraint.unbounded(0));
    }
}
