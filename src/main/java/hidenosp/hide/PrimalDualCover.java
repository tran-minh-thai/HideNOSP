// SPDX-License-Identifier: MIT
package hidenosp.hide;

import hidenosp.baseline.HidingMethod;
import hidenosp.core.EventSequence;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Full hiding with a proved approximation factor of l_max, for HIDE-DB with sigma = 1 and for
 * HIDE-SEQ with r = 1. Those two are the same condition: no occurrence of any sensitive pattern may
 * survive anywhere.
 *
 * <h2>The formulation</h2>
 *
 * <p>One binary variable per position; one constraint per occurrence:
 *
 * <pre>min sum c_i x_i   subject to   sum over i in L of x_i &gt;= 1 for every occurrence L</pre>
 *
 * <p>Every constraint lies inside a single sequence, so the program decomposes and each sequence is
 * solved on its own; the optimum of the whole is the sum of the per-sequence optima.
 *
 * <h2>Why the factor is l_max, and why it is not delta</h2>
 *
 * <p>A covering program is approximable within the largest number of variables appearing in one
 * constraint. Here a constraint is one occurrence of a pattern of length m, so it holds exactly
 * m &lt;= l_max variables, and the factor is l_max.
 *
 * <p>It is worth being explicit that this is <em>not</em> the quantity delta of [R20]. delta(i)
 * counts the occurrences through position i, which is the degree of a <em>column</em>; the factor
 * comes from the size of a <em>row</em>. delta is unbounded while m is bounded by l_max, so
 * mistaking one for the other would claim a guarantee that does not exist.
 *
 * <h2>Why primal-dual rather than solving the program and rounding</h2>
 *
 * <p>The design describes this contribution as linear-programming rounding. The
 * classical primal-dual method reaches the same factor, and three things make it the better fit:
 *
 * <ul>
 *   <li>it needs no solver, which keeps the promise in {@code config/protocol.yaml} that the build
 *       has no external dependency and works offline;</li>
 *   <li>the number of occurrences grows exponentially — {@link hidenosp.index.PathCounts} has to
 *       saturate for that reason — so the constraints could never be written out. Primal-dual only
 *       ever needs <em>one</em> violated constraint at a time, which is a shortest path on the
 *       Nettree;</li>
 *   <li>it produces a feasible dual, so every instance comes with its own lower bound. The paper
 *       can then report the ratio actually achieved instead of only the worst case, in the same
 *       spirit as the certified bracket of the lambda sweep.</li>
 * </ul>
 *
 * <p>That wording should be adjusted to match, since primal-dual is a different
 * technique from rounding even though the analysis goes through the same linear program.
 *
 * <h2>The argument</h2>
 *
 * <p>Raise the dual of a violated constraint until some position becomes tight, take every tight
 * position, repeat. The duals never break feasibility, so their sum is at most the optimum of the
 * relaxation and hence at most the integral optimum. Every chosen position is tight, so
 *
 * <pre>cost(X) = sum over i in X of sum over L containing i of y_L
 *         = sum over L of y_L * |X intersect L|
 *         &lt;= sum over L of y_L * m  &lt;= l_max * OPT.</pre>
 */
public final class PrimalDualCover implements HidingMethod {

    /** What one instance cost and what the dual certifies it could not have cost less than. */
    public record Bound(long cost, long lowerBound, int maxConstraintSize) {

        /** The factor actually achieved here, not the worst case. */
        public double achievedRatio() {
            return lowerBound == 0 ? 1 : (double) cost / lowerBound;
        }
    }

    private Bound lastBound;

    /**
     * Minimises the number of masks, which is the first objective of HIDE-DB.
     *
     * <p>A weighted variant would be a small change — the argument never looks at the costs beyond
     * comparing slacks, so any non-negative cost keeps the factor — but it needs B_eps wired in the
     * way {@link CutHide#withNearThreshold} does it. Until that is done, offering the option would
     * be offering a switch that silently does nothing.
     */
    public PrimalDualCover() {
    }

    @Override
    public String name() {
        return "primal-dual-lmax";
    }

    /** The bound from the last run; null before the first. */
    public Bound lastBound() {
        return lastBound;
    }

    @Override
    public Sanitization run(HideProblem problem) {
        requireFullHiding(problem);
        Sanitization result = new Sanitization(problem.database()).method(name());
        long cost = 0;
        long lower = 0;
        int widest = 0;
        for (int s = 0; s < problem.database().size(); s++) {
            Bound bound = coverSequence(problem, s, result);
            cost += bound.cost();
            lower += bound.lowerBound();
            widest = Math.max(widest, bound.maxConstraintSize());
        }
        lastBound = new Bound(cost, lower, widest);
        result.certifiedLowerBound(lower);
        return result;
    }

    /**
     * Refuses anything but full hiding, because the factor does not survive a partial target.
     *
     * <p>With sigma &gt; 1 the requirement is no longer "kill every occurrence" but "let no family
     * of sigma pairwise non-overlapping occurrences survive". Such a constraint holds up to
     * sigma * m variables, so the same argument yields sigma * m, not l_max.
     */
    private void requireFullHiding(HideProblem problem) {
        if (problem.threshold() == 1) {
            return;
        }
        throw new IllegalArgumentException("the l_max factor is proved for full hiding only, that is "
                + "sigma = 1 under HIDE-DB or r = 1 under HIDE-SEQ, but this problem has threshold "
                + problem.threshold() + ". With a partial target a constraint spans up to "
                + "threshold * m variables and the same argument gives that factor instead. Use the "
                + "greedy of CutHide, or the exact solver on a small instance.");
    }

    /** Primal-dual over one sequence; the constraints never cross sequences. */
    private Bound coverSequence(HideProblem problem, int sequenceIndex, Sanitization result) {
        EventSequence original = problem.database().get(sequenceIndex);

        // Unit costs, so the slacks stay integral and the duals are exact.
        long[] slack = new long[original.length()];
        Arrays.fill(slack, 1L);

        TreeSet<Integer> chosen = new TreeSet<>();
        List<Integer> order = new ArrayList<>();
        long dual = 0;
        int widest = 0;

        while (true) {
            int[] violated = cheapestUncovered(problem, original, chosen, slack);
            if (violated == null) {
                break;
            }
            widest = Math.max(widest, violated.length);
            long raise = Long.MAX_VALUE;
            for (int i : violated) {
                raise = Math.min(raise, slack[i]);
            }
            dual += raise;
            for (int i : violated) {
                slack[i] -= raise;
                if (slack[i] == 0 && chosen.add(i)) {
                    order.add(i);
                }
            }
        }

        reverseDelete(problem, original, chosen, order, slack);

        for (int i : chosen) {
            result.mask(sequenceIndex, i);
        }
        return new Bound(chosen.size(), dual, widest);
    }

    /**
     * Drops every chosen position the rest of the set already covers for, latest choice first.
     *
     * <p>Without this the method pays the worst case on every instance rather than in the worst
     * case. With unit costs the cheapest violated constraint is always one no position of which is
     * tight yet, so all m of its positions reach zero slack in the same round: each round buys one
     * unit of dual and spends m, and the ratio comes out at exactly m every time. Measured on three
     * random databases before this step was added it was 2.000, 2.000, 2.000.
     *
     * <p>Removing positions can only lower the cost and never touches the dual, so the certificate
     * and the factor both survive: cost after &le; cost before &le; l_max * dual &le; l_max * OPT.
     * Latest first is the usual order — the last choices are the ones the earlier ones most often
     * made redundant.
     */
    private void reverseDelete(HideProblem problem, EventSequence original,
                               TreeSet<Integer> chosen, List<Integer> order, long[] slack) {
        for (int k = order.size() - 1; k >= 0; k--) {
            int position = order.get(k);
            chosen.remove(position);
            if (cheapestUncovered(problem, original, chosen, slack) != null) {
                chosen.add(position);
            }
        }
    }

    /**
     * One violated constraint, chosen as the occurrence of least total slack, or null when none is
     * left. Least slack is the most violated constraint; any violated one would keep the guarantee,
     * but the cheapest one wastes less.
     *
     * <p>This is a shortest path on the Nettree with the slack as node weight, which is a sweep over
     * the levels. The occurrences themselves are never enumerated: there can be exponentially many.
     */
    private int[] cheapestUncovered(HideProblem problem, EventSequence original,
                                    TreeSet<Integer> chosen, long[] slack) {
        EventSequence sequence = chosen.isEmpty() ? original : original.mask(chosen);
        int[] best = null;
        long bestCost = Long.MAX_VALUE;
        for (Pattern pattern : problem.sensitive()) {
            Nettree nettree = Nettree.build(pattern, sequence, problem.gap());
            if (nettree.nodeCount() == 0) {
                continue;
            }
            int[] path = shortestPath(nettree, slack);
            if (path == null) {
                continue;
            }
            long total = 0;
            for (int position : path) {
                total += slack[position];
            }
            if (total < bestCost) {
                bestCost = total;
                best = path;
            }
        }
        return best;
    }

    /** The level-1 to level-m path of least total slack, as positions, or null when none exists. */
    private int[] shortestPath(Nettree nettree, long[] slack) {
        int n = nettree.nodeCount();
        long[] distance = new long[n];
        int[] previous = new int[n];
        Arrays.fill(distance, Long.MAX_VALUE);
        Arrays.fill(previous, -1);
        for (int v = 0; v < n; v++) {
            if (nettree.isSource(v)) {
                distance[v] = slack[nettree.position(v)];
            }
            if (distance[v] == Long.MAX_VALUE) {
                continue;
            }
            for (int u : nettree.out(v)) {
                long through = distance[v] + slack[nettree.position(u)];
                if (through < distance[u]) {
                    distance[u] = through;
                    previous[u] = v;
                }
            }
        }
        int end = -1;
        long best = Long.MAX_VALUE;
        for (int v = 0; v < n; v++) {
            if (nettree.isSink(v) && distance[v] < best) {
                best = distance[v];
                end = v;
            }
        }
        if (end < 0) {
            return null;
        }
        List<Integer> positions = new ArrayList<>();
        for (int v = end; v >= 0; v = previous[v]) {
            positions.add(nettree.position(v));
        }
        int[] out = new int[positions.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = positions.get(positions.size() - 1 - i);
        }
        return out;
    }

    /** The optimum of full hiding for one distinct-item pattern, by corollary P1c: its sup_NO. */
    public static long optimumForOneDistinctPattern(HideProblem problem) {
        if (problem.sensitive().size() != 1 || !problem.sensitive().get(0).hasDistinctItems()) {
            throw new IllegalArgumentException("P1c covers one distinct-item pattern only");
        }
        return SupportNO.of(problem.sensitive().get(0), problem.database(), problem.gap());
    }
}
