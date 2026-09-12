package hidenosp.metrics;

import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;

/**
 * The bounds that section 8.4 requires to be printed before a configuration is run, and the data
 * regime measured by SC2.
 *
 * <ul>
 *   <li>{@code minMasks}: |R| star, the least number of masks by P1 and its corollary. Valid for a
 *       single sensitive pattern with distinct items; for a repeated-item pattern P3 only brackets
 *       it, and {@link #isExact()} says so.</li>
 *   <li>{@code repetitionRatio}: rho_rep = 1 - sup_seq / sup_NO. Near zero means sup_NO collapses
 *       to the sequence support, so the configuration belongs to the degenerate control group.</li>
 *   <li>{@code constraintEffect}: beta = 1 - sup_NO under the gap constraint over sup_NO without
 *       it. Near zero means the gap constraint does no work.</li>
 * </ul>
 *
 * <p>Both ratios select the groups of rule 2.2.4: a configuration with rho_rep near zero or beta
 * near zero is kept as a declared control, never as evidence for the contribution.
 */
public record DataProfile(Pattern pattern, long supNO, int supSeq, long supNOWithoutGap,
                          long minMasks, double maskShare, double repetitionRatio,
                          double constraintEffect, boolean isExact) {

    /** Profiles one sensitive pattern against one configuration. */
    public static DataProfile of(SequenceDatabase database, Pattern pattern, GapConstraint gap,
                                 HideProblem.Mode mode, int threshold) {
        int[] perSequence = SupportNO.perSequence(pattern, database, gap);
        long total = 0;
        for (int f : perSequence) {
            total += f;
        }
        int supSeq = SupportNO.sequenceSupport(pattern, database, gap);
        long withoutGap = SupportNO.withoutGap(pattern, database);

        long minMasks;
        if (mode == HideProblem.Mode.HIDE_DB) {
            minMasks = Math.max(0, total - threshold + 1);
        } else {
            minMasks = 0;
            for (int f : perSequence) {
                minMasks += Math.max(0, f - threshold + 1);
            }
        }
        double maskShare = (double) minMasks / database.totalLength();
        double repetition = total == 0 ? 0 : 1.0 - (double) supSeq / total;
        double effect = withoutGap == 0 ? 0 : 1.0 - (double) total / withoutGap;
        return new DataProfile(pattern, total, supSeq, withoutGap, minMasks, maskShare,
                repetition, effect, pattern.hasDistinctItems());
    }

    /** Header for the SC2 table, kept next to the row writer so the two cannot drift apart. */
    public static String[] header() {
        return new String[] {"pattern", "sup_NO", "sup_seq", "sup_NO_no_gap", "min_masks",
                "mask_share", "rho_rep", "beta", "min_masks_exact"};
    }

    /** One row for the SC2 table. */
    public String[] row() {
        return new String[] {pattern.toString(), Long.toString(supNO), Integer.toString(supSeq),
                Long.toString(supNOWithoutGap), Long.toString(minMasks),
                String.format(java.util.Locale.ROOT, "%.6f", maskShare),
                String.format(java.util.Locale.ROOT, "%.6f", repetitionRatio),
                String.format(java.util.Locale.ROOT, "%.6f", constraintEffect),
                Boolean.toString(isExact)};
    }

    /** True when the configuration is a degenerate control: sup_NO collapses to sup_seq. */
    public boolean isDegenerateRepetition(double tolerance) {
        return repetitionRatio <= tolerance;
    }

    /** True when the gap constraint has no effect on this configuration. */
    public boolean isDegenerateConstraint(double tolerance) {
        return constraintEffect <= tolerance;
    }
}
