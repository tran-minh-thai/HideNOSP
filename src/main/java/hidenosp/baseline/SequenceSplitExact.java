// SPDX-License-Identifier: MIT
package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * The least number of masks for several sensitive patterns under HIDE-DB, exactly, on instances
 * whose sequences are short. It reaches larger instances than {@link IlpExact} because it uses the
 * one structure HIDE-DB gives for free: sup_NO(P, D) is the sum over sequences of sup_NO(P, S), and a
 * mask changes only the sequence it is in.
 *
 * <p>Let need(P) = sup_NO(P, D) - threshold + 1, the amount the support of P has to fall. For each
 * sequence S the method masks every subset of the candidate positions of S (positions that carry a
 * node of some sensitive Nettree; a mask anywhere else leaves every occurrence in place) and records,
 * for each vector of drops capped at need, the fewest masks reaching it. A dynamic program over the
 * sequences then keeps, for each vector of remaining need, the fewest masks so far. Its value at the
 * zero vector is the optimum: any solution splits into its parts per sequence, each part is one of
 * the enumerated subsets, and capping a drop at need loses nothing because need is all that counts.
 *
 * <p>It refuses, instead of running for hours, when a sequence has more than
 * {@link #MAX_CANDIDATES_PER_SEQUENCE} candidates (2^c subsets are masked) or the vectors of
 * remaining need exceed {@link #MAX_STATES}. The solution it returns is checked from the definition
 * before it is returned.
 */
public final class SequenceSplitExact implements HidingMethod {

    public static final int MAX_CANDIDATES_PER_SEQUENCE = 18;
    public static final int MAX_STATES = 1_000_000;

    @Override
    public String name() {
        return "exact-split";
    }

    /** Positions of S that carry a node of some sensitive Nettree. */
    public static int[] candidates(List<Pattern> sensitive, EventSequence sequence,
                                   hidenosp.core.GapConstraint gap) {
        TreeSet<Integer> positions = new TreeSet<>();
        for (Pattern p : sensitive) {
            Nettree nettree = Nettree.build(p, sequence, gap);
            for (int v = 0; v < nettree.nodeCount(); v++) {
                positions.add(nettree.position(v));
            }
        }
        return positions.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The product of need(P) + 1 over the patterns: the number of states of the program. */
    public static long states(HideProblem problem) {
        long product = 1;
        for (long n : needs(problem)) {
            product *= n + 1;
            if (product > Integer.MAX_VALUE) {
                return product;
            }
        }
        return product;
    }

    static long[] needs(HideProblem problem) {
        List<Pattern> sensitive = problem.sensitive();
        long[] need = new long[sensitive.size()];
        for (int k = 0; k < need.length; k++) {
            need[k] = Math.max(0, SupportNO.of(sensitive.get(k), problem.database(), problem.gap())
                    - problem.threshold() + 1);
        }
        return need;
    }

    @Override
    public Sanitization run(HideProblem problem) {
        if (!problem.isDatabaseMode()) {
            throw new IllegalArgumentException("exact-split needs HIDE-DB: under HIDE-SEQ the sequences"
                    + " do not add up");
        }
        List<Pattern> sensitive = problem.sensitive();
        SequenceDatabase database = problem.database();
        int n = sensitive.size();
        long[] needLong = needs(problem);
        long states = states(problem);
        if (states > MAX_STATES) {
            throw new IllegalArgumentException("exact-split takes at most " + MAX_STATES
                    + " vectors of remaining need, this instance has " + states + "; lower the needs");
        }
        int[] need = new int[n];
        int[] radix = new int[n];
        int size = 1;
        for (int k = 0; k < n; k++) {
            need[k] = (int) needLong[k];
            radix[k] = size;
            size *= need[k] + 1;
        }
        int start = 0;
        for (int k = 0; k < n; k++) {
            start += need[k] * radix[k];
        }

        // The options of every sequence: capped drop vector -> fewest masks and the subset.
        List<int[]> seqCandidates = new ArrayList<>();
        List<int[][]> seqDrops = new ArrayList<>();
        List<int[]> seqCost = new ArrayList<>();
        List<long[]> seqSubset = new ArrayList<>();
        for (int s = 0; s < database.size(); s++) {
            EventSequence sequence = database.get(s);
            int[] cand = candidates(sensitive, sequence, problem.gap());
            if (cand.length > MAX_CANDIDATES_PER_SEQUENCE) {
                throw new IllegalArgumentException("exact-split takes at most "
                        + MAX_CANDIDATES_PER_SEQUENCE + " candidate positions per sequence, sequence " + s
                        + " has " + cand.length);
            }
            int[] base = new int[n];
            for (int k = 0; k < n; k++) {
                base[k] = SupportNO.of(sensitive.get(k), sequence, problem.gap());
            }
            Map<String, long[]> best = new HashMap<>();
            for (long subset = 1; subset < (1L << cand.length); subset++) {
                List<Integer> masked = new ArrayList<>();
                for (int i = 0; i < cand.length; i++) {
                    if ((subset >>> i & 1L) != 0) {
                        masked.add(cand[i]);
                    }
                }
                EventSequence after = sequence.mask(masked);
                int[] drop = new int[n];
                boolean any = false;
                for (int k = 0; k < n; k++) {
                    if (base[k] == 0 || need[k] == 0) {
                        continue;
                    }
                    drop[k] = Math.min(need[k], base[k] - SupportNO.of(sensitive.get(k), after, problem.gap()));
                    any |= drop[k] > 0;
                }
                if (!any) {
                    continue;
                }
                String key = Arrays.toString(drop);
                long[] known = best.get(key);
                int cost = Long.bitCount(subset);
                if (known == null || cost < known[0]) {
                    best.put(key, new long[] {cost, subset});
                }
            }
            int[][] drops = new int[best.size()][];
            int[] cost = new int[best.size()];
            long[] subsets = new long[best.size()];
            int i = 0;
            for (Map.Entry<String, long[]> e : best.entrySet()) {
                drops[i] = parse(e.getKey(), n);
                cost[i] = (int) e.getValue()[0];
                subsets[i] = e.getValue()[1];
                i++;
            }
            seqCandidates.add(cand);
            seqDrops.add(drops);
            seqCost.add(cost);
            seqSubset.add(subsets);
        }

        // The program over sequences, with the choice of every layer kept for the solution.
        final int infinity = Integer.MAX_VALUE;
        int[] dp = new int[size];
        Arrays.fill(dp, infinity);
        dp[start] = 0;
        int layers = database.size();
        int[][] fromState = new int[layers][];
        int[][] option = new int[layers][];
        int[] remaining = new int[n];
        for (int s = 0; s < layers; s++) {
            int[][] drops = seqDrops.get(s);
            int[] cost = seqCost.get(s);
            int[] next = dp.clone();
            int[] from = new int[size];
            int[] opt = new int[size];
            Arrays.fill(opt, -1);
            for (int state = 0; state < size; state++) {
                from[state] = state;
            }
            for (int state = 0; state < size; state++) {
                if (dp[state] == infinity) {
                    continue;
                }
                int rest = state;
                for (int k = n - 1; k >= 0; k--) {
                    remaining[k] = rest / radix[k];
                    rest %= radix[k];
                }
                for (int o = 0; o < drops.length; o++) {
                    int target = 0;
                    for (int k = 0; k < n; k++) {
                        target += Math.max(0, remaining[k] - drops[o][k]) * radix[k];
                    }
                    int value = dp[state] + cost[o];
                    if (value < next[target]) {
                        next[target] = value;
                        from[target] = state;
                        opt[target] = o;
                    }
                }
            }
            fromState[s] = from;
            option[s] = opt;
            dp = next;
        }
        if (dp[0] == infinity) {
            throw new IllegalStateException("no hiding set exists, which masking every position contradicts");
        }

        Sanitization result = new Sanitization(database).method(name());
        int state = 0;
        for (int s = layers - 1; s >= 0; s--) {
            int o = option[s][state];
            if (o >= 0) {
                long subset = seqSubset.get(s)[o];
                int[] cand = seqCandidates.get(s);
                for (int i = 0; i < cand.length; i++) {
                    if ((subset >>> i & 1L) != 0) {
                        result.mask(s, cand[i]);
                    }
                }
            }
            state = fromState[s][state];
        }
        if (state != start || result.maskCount() != dp[0]) {
            throw new IllegalStateException("the solution read back does not match the program: "
                    + result.maskCount() + " masks against " + dp[0]);
        }
        SequenceDatabase after = result.sanitized();
        for (Pattern p : sensitive) {
            if (SupportNO.of(p, after, problem.gap()) >= problem.threshold()) {
                throw new IllegalStateException("the solution read back leaves " + p + " at or above the threshold");
            }
        }
        return result;
    }

    private static int[] parse(String text, int n) {
        String[] parts = text.substring(1, text.length() - 1).split(", ");
        int[] out = new int[n];
        for (int k = 0; k < n; k++) {
            out[k] = Integer.parseInt(parts[k]);
        }
        return out;
    }
}
