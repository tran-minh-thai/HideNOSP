package hidenosp.hide;

/**
 * The design variants of CUT-HIDE compared at SC7. Each field changes exactly one factor, as
 * required by rule 2.3.7; the scenario prints the whole record into the run header so a result file
 * always names the variant that produced it.
 */
public record CutHideConfig(Weighting weighting, boolean refineLambda, AllocationRule allocation,
                            boolean verifyPostcondition) {

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

    /** The configuration the paper reports as CUT-HIDE. */
    public static CutHideConfig standard() {
        return new CutHideConfig(Weighting.VITAL_NODE, true, AllocationRule.DYNAMIC_PROGRAM, true);
    }

    /** The variant without side-effect weights, the first row of SC7. */
    public static CutHideConfig uniformWeights() {
        return new CutHideConfig(Weighting.UNIFORM, true, AllocationRule.DYNAMIC_PROGRAM, true);
    }

    @Override
    public String toString() {
        return "CutHide[" + weighting + ", refineLambda=" + refineLambda + ", " + allocation + "]";
    }
}
