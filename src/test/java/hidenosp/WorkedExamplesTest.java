package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.core.flow.NettreeFlow;
import hidenosp.hide.CostCurve;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The worked examples of the design doc, pinned as tests. They are the examples the paper prints,
 * so a change that breaks one of them breaks the paper, not only the code.
 *
 * <p>Positions are written 1-based in the design doc and 0-based in the code; each test states the
 * conversion where it matters.
 */
class WorkedExamplesTest {

    private static final int A = 0;
    private static final int B = 1;
    private static final int C = 2;

    private static EventSequence sequence(int... events) {
        return new EventSequence(0, events);
    }

    @Test
    @DisplayName("G3 running example: S = abaababaa, P = ab, gap [0,2]")
    void runningExample() {
        EventSequence s = sequence(A, B, A, A, B, A, B, A, A);
        Pattern p = new Pattern(A, B);
        GapConstraint gap = new GapConstraint(0, 2);

        assertEquals(3, SupportNO.of(p, s, gap), "sup_NO of the running example");

        Nettree nettree = Nettree.build(p, s, gap);
        NettreeFlow flow = new NettreeFlow(nettree);
        // The design doc lists positions 1, 2, 5 and 7 as the ones whose masking lowers sup_NO.
        assertArrayEquals(new int[] {0, 1, 4, 6}, flow.vitalPositions(),
                "vital positions, 0-based");

        // Position 4 of the doc, that is index 3, lies on two occurrences yet is not vital.
        assertEquals(3, SupportNO.of(p, s.mask(3), gap), "masking position 4 does not lower sup_NO");
        assertEquals(2, SupportNO.of(p, s.mask(0), gap), "masking position 1 lowers sup_NO");
    }

    @Test
    @DisplayName("Corollary P1c: S = ababcbc, P = abc, gap [0,3] needs two masks, not three")
    void heuristicOverspends() {
        EventSequence s = sequence(A, B, A, B, C, B, C);
        Pattern p = new Pattern(A, B, C);
        GapConstraint gap = new GapConstraint(0, 3);

        assertEquals(2, SupportNO.of(p, s, gap), "the optimum number of masks is sup_NO");

        // The local rule of [R20] masks position 4 of the doc, index 3, because it lies on four
        // matches; two more masks are then needed, three in total.
        EventSequence afterHeuristicFirstMask = s.mask(3);
        assertEquals(2, SupportNO.of(p, afterHeuristicFirstMask, gap),
                "the position on the most matches is not vital here");

        // Masking the two occurrences of c, positions 5 and 7 of the doc, suffices.
        EventSequence exact = s.mask(4).mask(6);
        assertEquals(0, SupportNO.of(p, exact, gap), "two masks are enough");
    }

    @Test
    @DisplayName("P6(c): S = babbacbcaacb, P = ca, gap [0,2], the weighted cuts")
    void weightedCuts() {
        // b a b b a c b c a a c b, 1-based; c sits at 6, 8, 11 and a at 2, 5, 9, 10.
        EventSequence s = sequence(B, A, B, B, A, C, B, C, A, A, C, B);
        Pattern p = new Pattern(C, A);
        GapConstraint gap = new GapConstraint(0, 2);

        assertEquals(2, SupportNO.of(p, s, gap), "F of the counterexample");

        double[] weight = new double[s.length()];
        weight[5] = 3;    // c at position 6
        weight[7] = 50;   // c at position 8
        weight[8] = 100;  // a at position 9
        weight[9] = 1;    // a at position 10

        CostCurve curve = CostCurve.build(Nettree.build(p, s, gap), weight, true);
        assertEquals(2, curve.supNO());
        assertEquals(53 * CostCurve.SCALE, curve.cost(2),
                "k = F is exact by P6(b): the cut {c6, c8} weighs 53");
        assertEquals(CostCurve.SCALE, curve.cost(1),
                "the optimum at k = 1 is 1, masking a10; H1 would pay 3");
        assertArrayEquals(new int[] {9}, curve.positions(1), "a10 is index 9");
        assertTrue(curve.isCertified(1) && curve.isCertified(2),
                "both costs are certified optimal on this instance");
    }

    @Test
    @DisplayName("P7: the cost curve of S = babcaabbababc, P = ab, gap [1,5] is not convex")
    void nonConvexCostCurve() {
        // b a b c a a b b a b a b c, 1-based.
        EventSequence s = sequence(B, A, B, C, A, A, B, B, A, B, A, B, C);
        Pattern p = new Pattern(A, B);
        GapConstraint gap = new GapConstraint(1, 5);

        double[] weight = new double[s.length()];
        weight[1] = 100;  // a at 2
        weight[4] = 3;    // a at 5
        weight[5] = 50;   // a at 6
        weight[8] = 1;    // a at 9
        weight[10] = 2;   // a at 11
        weight[0] = 2;    // b at 1
        weight[2] = 1;    // b at 3
        weight[6] = 3;    // b at 7
        weight[7] = 3;    // b at 8
        weight[9] = 3;    // b at 10
        weight[11] = 3;   // b at 12

        CostCurve curve = CostCurve.build(Nettree.build(p, s, gap), weight, true);
        assertEquals(4, curve.supNO(), "F of the non-convexity example");
        long[] expected = {0, 1, 4, 9, 12};
        for (int k = 0; k <= 4; k++) {
            assertEquals(expected[k] * CostCurve.SCALE, curve.cost(k),
                    "c(" + k + ") of the design doc");
        }
        assertFalse(curve.isConvex(),
                "c = (0, 1, 4, 9, 12) has increments 1, 3, 5, 3, so it is not convex");
        // The one pair the sweep cannot certify here: LB = 8 against UB = 9 at k = 3. The share of
        // certified pairs is a quantity the paper reports, so the gap is pinned rather than hidden.
        assertFalse(curve.isCertified(3), "k = 3 is the uncertified pair of this instance");
        assertEquals(8 * CostCurve.SCALE, curve.lowerBound(3), "the lower bound at k = 3");
    }
}
