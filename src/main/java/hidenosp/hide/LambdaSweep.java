package hidenosp.hide;

import hidenosp.core.Nettree;
import hidenosp.core.flow.NettreeFlow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * The lambda sweep of P6(d): a certified bracket on c(k), the least weight of k masks that bring
 * sup_NO down to t = F - k, when 0 &lt; t &lt; F.
 *
 * <p>For every lambda &ge; 0 and every minimum-cardinality cut C, the sum of the k smallest weights
 * of C is at least sum over C of min(w, lambda) minus t*lambda. Taking the least such cut gives
 *
 * <pre>h(lambda) = min over C of (sum over C of min(w, lambda)) - t * lambda,</pre>
 *
 * a valid lower bound for every lambda, computable by one weighted minimum cut with the clipped
 * weights min(w, lambda) (P6(b)). The function h is concave and piecewise linear, but its breaks
 * need not sit at weight values, so scanning the weight values alone gives a correct but possibly
 * loose bound; the refinement below intersects supporting lines inside every interval where the
 * supergradient changes sign.
 *
 * <p>The upper bound is the best sum of the k smallest true weights over the cuts met during the
 * sweep. When ceiling(LB) = UB the solution is certified optimal, which for integer weights means
 * UB - LB &lt; 1. The certified share is a quantity the paper reports (section 9.2, step 5).
 *
 * <p>Weights arrive already scaled to integers by {@link CostCurve}; lambda therefore moves over
 * integer values in the same unit. Evaluating h at a rounded lambda keeps the bound valid, since
 * every lambda &ge; 0 gives one.
 */
public final class LambdaSweep {

    /** Outcome of a sweep: the bracket, the best masking set found, and whether it is certified. */
    public record Result(long lowerBound, long upperBound, int[] positions, boolean certified) {
    }

    private final Nettree nettree;
    private final long[] weight;
    private final boolean[] removed;
    private final int k;
    private final int t;

    /**
     * @param weight scaled integer weight per Nettree node
     * @param removed nodes already masked, or null
     * @param k number of masks to place, with 0 &lt; k &lt; F
     * @param t the target support F - k
     */
    public LambdaSweep(Nettree nettree, long[] weight, boolean[] removed, int k, int t) {
        this.nettree = nettree;
        this.weight = weight;
        this.removed = removed;
        this.k = k;
        this.t = t;
    }

    /** Runs the sweep over the distinct weight values, then refines inside each interval. */
    public Result run(boolean refine) {
        TreeSet<Long> lambdas = new TreeSet<>();
        lambdas.add(0L);
        for (int v = 0; v < weight.length; v++) {
            if (removed == null || !removed[v]) {
                lambdas.add(weight[v]);
            }
        }
        List<NettreeFlow.WeightedCut> cuts = new ArrayList<>();
        List<long[]> points = new ArrayList<>();
        long lower = Long.MIN_VALUE;
        for (long lambda : lambdas) {
            long[] point = evaluate(lambda, cuts);
            points.add(point);
            lower = Math.max(lower, point[1]);
        }
        if (refine) {
            for (int idx = 0; idx + 1 < points.size(); idx++) {
                lower = Math.max(lower, refineInterval(points.get(idx), points.get(idx + 1), cuts));
            }
        }
        long upper = Long.MAX_VALUE;
        int[] best = null;
        for (NettreeFlow.WeightedCut cut : cuts) {
            long[] w = new long[cut.nodes().length];
            for (int i = 0; i < w.length; i++) {
                w[i] = weight[cut.nodes()[i]];
            }
            Arrays.sort(w);
            long sum = 0;
            for (int i = 0; i < Math.min(k, w.length); i++) {
                sum += w[i];
            }
            if (w.length >= k && sum < upper) {
                upper = sum;
                best = lightestPositions(cut);
            }
        }
        if (best == null) {
            throw new IllegalStateException("no cut of size at least k found");
        }
        boolean certified = upper - lower < 1;
        return new Result(lower, upper, best, certified);
    }

    /** Returns {lambda, h(lambda), supergradient} and records the cut that attained it. */
    private long[] evaluate(long lambda, List<NettreeFlow.WeightedCut> cuts) {
        long[] clipped = new long[weight.length];
        for (int v = 0; v < weight.length; v++) {
            clipped[v] = Math.min(weight[v], lambda);
        }
        NettreeFlow.WeightedCut cut = NettreeFlow.minWeightMinCut(nettree, clipped, removed);
        cuts.add(cut);
        long above = 0;
        for (int v : cut.nodes()) {
            if (weight[v] > lambda) {
                above++;
            }
        }
        return new long[] {lambda, cut.weight() - (long) t * lambda, above - t};
    }

    /**
     * Inside one interval, walks to the crossing of the two supporting lines. The iteration stops
     * as soon as a value sits on the line it came from, which is the maximum of h there.
     */
    private long refineInterval(long[] lo, long[] hi, List<NettreeFlow.WeightedCut> cuts) {
        long best = Math.max(lo[1], hi[1]);
        for (int step = 0; step < 60; step++) {
            if (lo[2] <= 0 || hi[2] >= 0 || lo[2] == hi[2]) {
                return best;
            }
            double x = (hi[1] - lo[1] + (double) lo[2] * lo[0] - (double) hi[2] * hi[0])
                    / (lo[2] - hi[2]);
            long lambda = Math.round(x);
            if (lambda <= lo[0] || lambda >= hi[0]) {
                return best;
            }
            long[] point = evaluate(lambda, cuts);
            best = Math.max(best, point[1]);
            if (point[2] > 0) {
                lo = point;
            } else if (point[2] < 0) {
                hi = point;
            } else {
                return best;
            }
        }
        return best;
    }

    /** The positions of the k lightest nodes of a cut, the masking set behind the upper bound. */
    private int[] lightestPositions(NettreeFlow.WeightedCut cut) {
        Integer[] order = new Integer[cut.nodes().length];
        for (int i = 0; i < order.length; i++) {
            order[i] = cut.nodes()[i];
        }
        Arrays.sort(order, (a, b) -> Long.compare(weight[a], weight[b]));
        int[] positions = new int[k];
        for (int i = 0; i < k; i++) {
            positions[i] = nettree.position(order[i]);
        }
        return positions;
    }
}
