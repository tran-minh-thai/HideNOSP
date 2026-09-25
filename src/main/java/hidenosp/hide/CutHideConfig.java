package hidenosp.hide;

/**
 * The design variants of CUT-HIDE compared at SC7. Each field changes exactly one factor, as
 * required by rule 2.3.7; the scenario prints the whole record into the run header so a result file
 * always names the variant that produced it.
 */
public record CutHideConfig(Weighting weighting, boolean refineLambda, AllocationRule allocation,
                            boolean verifyPostcondition, Curves curves) {

    public enum Weighting {
        /** Every position weighs the same; the cut then minimises only the number of masks. */
        UNIFORM,
        /** w(i) of step 4, summed over the near-threshold patterns for which i is vital. */
        VITAL_NODE
    }

    public enum AllocationRule {
        /** The dynamic program of P7, correct even when the cost curves are not convex. */
        DYNAMIC_PROGRAM,
        /** The greedy walk over marginal costs, correct only for convex curves. */
        MARGINAL_GREEDY
    }

    /** How the per-sequence curves of one sensitive pattern are computed. */
    public enum Curves {
        /**
         * Theorem P8 when g- = 0: exact least masks and least weight for every target, for any
         * mu(P). With g- &gt; 0 it falls back to the lambda sweep.
         */
        FRONTIER_DP,
        /**
         * Theorem P8 with the order of the two costs reversed: least surrogate weight, then fewest
         * masks. It departs from the lexicographic objective of section 2 on purpose, to measure
         * the other end of the trade-off; with g- &gt; 0 it falls back to the lambda sweep.
         */
        FRONTIER_DP_WEIGHT_FIRST,
        /**
         * The weighted cut and the lambda sweep of P6 only. For a repeated-item pattern the masks
         * are those of a cut, the mu(P)-approximation of P3.
         */
        LAMBDA_SWEEP
    }

    /** The configuration the paper reports as CUT-HIDE. */
    public static CutHideConfig standard() {
        return new CutHideConfig(Weighting.VITAL_NODE, true, AllocationRule.DYNAMIC_PROGRAM, true,
                Curves.FRONTIER_DP);
    }

    /** The variant without side-effect weights, the first row of SC7. */
    public static CutHideConfig uniformWeights() {
        return new CutHideConfig(Weighting.UNIFORM, true, AllocationRule.DYNAMIC_PROGRAM, true,
                Curves.FRONTIER_DP);
    }

    /** This configuration with the curves computed the other way, for SC7 and the ablations. */
    public CutHideConfig withCurves(Curves other) {
        return new CutHideConfig(weighting, refineLambda, allocation, verifyPostcondition, other);
    }

    @Override
    public String toString() {
        return "CutHide[" + weighting + ", refineLambda=" + refineLambda + ", " + allocation + ", "
                + curves + "]";
    }
}
