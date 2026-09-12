package hidenosp.hide;

import hidenosp.core.Nettree;
import hidenosp.core.flow.NettreeFlow;

import java.util.Arrays;

/**
 * The per-sequence cost curve c_S(k) of section 9.2, step 5: for k = 0 .. F_S, the least total
 * weight of k masked positions that bring sup_NO down to F_S - k.
 *
 * <ul>
 *   <li>k = 0 costs nothing;</li>
 *   <li>k = F_S is exact, by the weighted minimum cut of P6(b);</li>
 *   <li>0 &lt; k &lt; F_S is bracketed by the lambda sweep of P6(d), which returns an upper bound
 *       with its masking set, a lower bound, and whether the pair is certified optimal.</li>
 * </ul>
 *
 * <p>The curve is not convex in general: the probe of P7 found three non-convex curves in 5931
 * instances, which is why the allocation of {@link Allocation} is a dynamic program and not a
 * greedy walk over marginal costs.
 *
 * <p>Weights arrive as doubles from {@link hidenosp.index.NettreeCutIndex#positionWeights(double[])}
 * and are scaled to integers by {@link #SCALE} so that the max flow stays integral. The scale is a
 * declared parameter of the run: it is written into the protocol table, and two runs with different
 * scales are not comparable.
 */
public final class CostCurve {

    /** Fixed-point unit for the position weights. Declared in the protocol table, row 1. */
    public static final long SCALE = 1_000_000L;

    private final Nettree nettree;
    private final long[] nodeWeight;
    private final int supNO;
    private final long[] cost;
    private final long[] lowerBound;
    private final int[][] positions;
    private final boolean[] certified;

    private CostCurve(Nettree nettree, long[] nodeWeight, int supNO) {
        this.nettree = nettree;
        this.nodeWeight = nodeWeight;
        this.supNO = supNO;
        this.cost = new long[supNO + 1];
        this.lowerBound = new long[supNO + 1];
        this.positions = new int[supNO + 1][];
        this.certified = new boolean[supNO + 1];
        this.positions[0] = new int[0];
        this.certified[0] = true;
    }

    /**
     * Builds the curve for one sequence.
     *
     * @param nettree NT(P, S) of the sensitive pattern
     * @param positionWeight w(i) per position of S, from step 4
     * @param refine whether the lambda sweep looks for the exact maximum of h, rather than only
     *     scanning the weight values; SC7 compares the two
     */
    public static CostCurve build(Nettree nettree, double[] positionWeight, boolean refine) {
        int n = nettree.nodeCount();
        long[] nodeWeight = new long[n];
        for (int v = 0; v < n; v++) {
            nodeWeight[v] = Math.round(positionWeight[nettree.position(v)] * SCALE);
        }
        int supNO = new NettreeFlow(nettree).supNO();
        CostCurve curve = new CostCurve(nettree, nodeWeight, supNO);
        if (supNO == 0) {
            return curve;
        }
        NettreeFlow.WeightedCut full = NettreeFlow.minWeightMinCut(nettree, nodeWeight, null);
        curve.cost[supNO] = full.weight();
        curve.lowerBound[supNO] = full.weight();
        curve.certified[supNO] = true;
        int[] all = new int[full.nodes().length];
        for (int i = 0; i < all.length; i++) {
            all[i] = nettree.position(full.nodes()[i]);
        }
        Arrays.sort(all);
        curve.positions[supNO] = all;
        for (int k = 1; k < supNO; k++) {
            LambdaSweep.Result r = new LambdaSweep(nettree, nodeWeight, null, k, supNO - k).run(refine);
            curve.cost[k] = r.upperBound();
            curve.lowerBound[k] = r.lowerBound();
            curve.positions[k] = r.positions();
            curve.certified[k] = r.certified();
        }
        return curve;
    }

    /** F_S = sup_NO(P, S). */
    public int supNO() {
        return supNO;
    }

    /** c_S(k), in the scaled unit; this is the upper bound when the pair is not certified. */
    public long cost(int k) {
        return cost[k];
    }

    /** The lower bound of the lambda sweep at k, in the scaled unit. */
    public long lowerBound(int k) {
        return lowerBound[k];
    }

    /** The positions to mask to realise c_S(k). */
    public int[] positions(int k) {
        return positions[k];
    }

    /** Whether ceiling(LB) = UB held at k, the certification counted in the results table. */
    public boolean isCertified(int k) {
        return certified[k];
    }

    public Nettree nettree() {
        return nettree;
    }

    public long[] nodeWeight() {
        return nodeWeight;
    }

    /** True when the curve has non-decreasing increments; the probe of P7 found counterexamples. */
    public boolean isConvex() {
        for (int k = 1; k + 1 <= supNO; k++) {
            long before = cost[k] - cost[k - 1];
            long after = cost[k + 1] - cost[k];
            if (after < before) {
                return false;
            }
        }
        return true;
    }
}
