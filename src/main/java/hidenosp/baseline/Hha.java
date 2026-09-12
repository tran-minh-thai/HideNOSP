package hidenosp.baseline;

import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

/**
 * HHA of Abul, Atzori, Bonchi and Giannotti 2007 [R20], tier (b) of section 11.3, re-implemented
 * faithfully rather than approximated.
 *
 * <p>Two parts have to be kept apart, because the paper measures them separately.
 *
 * <ol>
 *   <li><b>The original problem</b> (SC8): support is the number of sequences containing the
 *       pattern, the modification is the delayed deletion mark, and sequences are ordered by the
 *       size of their match set. Run in that setting, HHA is compared against the exact minimum cut
 *       of corollary P1c, which says the least number of marks for one distinct-item pattern is
 *       exactly sup_NO of that pattern in that sequence.</li>
 *   <li><b>The new problem</b> (SC3): the same local rule, mask the position on the most matches,
 *       but the target is sup_NO. That transfer must be described explicitly in the paper; the
 *       authors never tuned their rule for a non-overlapping count. The position rule alone lives
 *       in {@link OccurrenceCountHeuristic}; this class adds the sequence ordering and the
 *       marking discipline.</li>
 * </ol>
 *
 * <p>The quantity delta of theorem 2 of [R20] is a per-position dynamic program in O(n squared m).
 * The index computes the same numbers with forward and backward path counts in one pass
 * ({@link hidenosp.index.PathCounts}); the probe measured roughly eighty times faster at n = 400.
 * The comparison belongs to SC6, so this class must keep a flag selecting the original dynamic
 * program, otherwise the timing comparison would be against a straw man.
 *
 * <p>TODO(code): implement. Order sequences by match-set size; inside a sequence apply the local
 * rule until the sequence no longer supports the pattern; expose {@code useOriginalDynamicProgram}
 * so SC6 can time both ways of computing delta.
 */
public final class Hha implements HidingMethod {

    private final boolean useOriginalDynamicProgram;

    public Hha(boolean useOriginalDynamicProgram) {
        this.useOriginalDynamicProgram = useOriginalDynamicProgram;
    }

    @Override
    public String name() {
        return "HHA" + (useOriginalDynamicProgram ? "-dp" : "-pathcounts");
    }

    @Override
    public Sanitization run(HideProblem problem) {
        throw new UnsupportedOperationException("HHA is not implemented yet; see section 11.3");
    }
}
