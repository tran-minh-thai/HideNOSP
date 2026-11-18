// SPDX-License-Identifier: MIT
package hidenosp.hide;

/**
 * The design variants of CUT-HIDE compared at SC7. Each field changes exactly one factor;
 * the scenario prints the whole record into the run header so a result file
 * always names the variant that produced it.
 */
public record CutHideConfig(Weighting weighting, boolean refineLambda, AllocationRule allocation,
                            boolean verifyPostcondition, Curves curves, boolean refineSeveral,
                            boolean exactDropDatabase) {

    /** The configurations written before the exact-drop score of HIDE-DB existed: that score off. */
    public CutHideConfig(Weighting weighting, boolean refineLambda, AllocationRule allocation,
                         boolean verifyPostcondition, Curves curves, boolean refineSeveral) {
        this(weighting, refineLambda, allocation, verifyPostcondition, curves, refineSeveral, false);
    }

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
         * masks. It departs from the lexicographic objective of HIDE-DB on purpose, to measure
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
        // The exact-drop score of the HIDE-DB greedy is part of CUT-HIDE since preregistration section 23.9, as the
        // greedy of HIDE-SEQ has been since preregistration section 22.8; withCountScoreDatabase() gives the earlier rule.
        return new CutHideConfig(Weighting.VITAL_NODE, true, AllocationRule.DYNAMIC_PROGRAM, true,
                Curves.FRONTIER_DP, false, true);
    }

    /** The variant without side-effect weights, the first row of SC7. */
    public static CutHideConfig uniformWeights() {
        return new CutHideConfig(Weighting.UNIFORM, true, AllocationRule.DYNAMIC_PROGRAM, true,
                Curves.FRONTIER_DP, false, true);
    }

    /** This configuration with the other weighting, everything else unchanged. */
    public CutHideConfig withWeighting(Weighting other) {
        return new CutHideConfig(other, refineLambda, allocation, verifyPostcondition, curves, refineSeveral,
                exactDropDatabase);
    }

    /** This configuration with the curves computed the other way, for SC7 and the ablations. */
    public CutHideConfig withCurves(Curves other) {
        return new CutHideConfig(weighting, refineLambda, allocation, verifyPostcondition, other,
                refineSeveral, exactDropDatabase);
    }

    /**
     * This configuration with the several-pattern refinement of proposition P15: after the greedy
     * of step 7, each component of the shared-item graph holding one pattern is replaced by the
     * exact single-pattern solution when that uses fewer masks, then masks are removed in reverse
     * order of choice while every pattern stays hidden. One sensitive pattern is unaffected.
     */
    public CutHideConfig withRefineSeveral() {
        return new CutHideConfig(weighting, refineLambda, allocation, verifyPostcondition, curves, true,
                exactDropDatabase);
    }

    /**
     * This configuration with the step-7 greedy of HIDE-DB scoring a position by the exact drop it causes in each
     * sensitive pattern still needed, capped at what that pattern still needs, as the greedy of HIDE-SEQ does since
     * preregistration section 22.8. Off, a position is scored by the number of those patterns it is vital for.
     */
    public CutHideConfig withExactDropDatabase() {
        return new CutHideConfig(weighting, refineLambda, allocation, verifyPostcondition, curves, refineSeveral,
                true);
    }

    /**
     * This configuration with the score of the HIDE-DB greedy used before preregistration section 23.9: the number of
     * sensitive patterns still needed for which the position is vital. It reproduces the rows of the earlier files.
     */
    public CutHideConfig withCountScoreDatabase() {
        return new CutHideConfig(weighting, refineLambda, allocation, verifyPostcondition, curves, refineSeveral,
                false);
    }

    @Override
    public String toString() {
        // The refinement is named only when on, so the configurations that existed before it keep the
        // display name already written in the result files.
        return "CutHide[" + weighting + ", refineLambda=" + refineLambda + ", " + allocation + ", "
                + curves + (refineSeveral ? ", refineSeveral" : "") + (exactDropDatabase ? ", exactDropDatabase" : "")
                + "]";
    }
}
