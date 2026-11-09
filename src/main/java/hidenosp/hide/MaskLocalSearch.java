// SPDX-License-Identifier: MIT
package hidenosp.hide;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.util.Deadline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Local search over the minimum-mask hiding sets of one sensitive pattern, with the exact number of
 * lost patterns as its objective, for every gap where an exact program applies ({@link ExactCurve}).
 * Under HIDE-SEQ only one-sequence moves exist, since the remaining support of each sequence is
 * bounded by r - 1 on its own.
 *
 * <p>A move changes the masks of one or two sequences and keeps the solution a minimum-mask hiding
 * set: a <em>one-sequence move</em> replaces the masks X_s of sequence s by another set of the same
 * size that leaves sup_NO(P, s) no higher; an <em>allocation move</em> lets a masked sequence s1
 * keep one more occurrence of P and a sequence s2 lose one more, with the total number of masks
 * unchanged, so the support of P over the database is unchanged too. Candidate sets come from the
 * exact program of theorem P8 on that sequence alone: for a one-sequence move, the set least
 * overlapping X_s and, for every mask of X_s, the best set avoiding that mask; for an allocation
 * move, s1 keeps as many of its masks as it can and s2 keeps its masks and adds the positions that
 * are vital for the fewest watched patterns, s2 taken among the {@code receivers} sequences whose
 * next unit of support is cheapest in that sense. A pass tries every masked sequence that damages a
 * lost pattern (in increasing index), applying the best improving one-sequence move of each at once;
 * when a pass changes nothing, the best improving allocation move is applied and passes resume.
 *
 * <p>Saving condition (lemma L9 of the design): a lost pattern Q needs its drop D_Q to fall by its
 * excess e_Q = D_Q - m_Q + 1, and a move changing the sequences T lowers D_Q by at most the sum of
 * their current drops d_Q^s. Lost falls only if some lost pattern is saved, so a move is tried only
 * when some lost Q has e_Q at most that sum, and the search stops at once when no lost Q could be
 * saved even by the two sequences that hurt it most. The condition is checked against the state at
 * the moment a move is evaluated, so it removes only moves that cannot lower Lost: the search makes
 * exactly the moves it would make without the condition, and skips the rest.
 *
 * <p>On the Max-Cut instances of the NP-hardness reduction a one-sequence move is a vertex flip, so
 * every local optimum leaves at most half of the edges monochromatic.
 */
public final class MaskLocalSearch {

    /** The outcome of one search. */
    public record Result(Map<Integer, Set<Integer>> masks, int lost, int moves, int passes,
                         int evaluated, boolean nothingSaveable) {
    }

    /** Passes allowed; a pass that changes nothing ends the search earlier. */
    public static final int DEFAULT_PASSES = 50;
    /** Receiving sequences tried in an allocation move, the cheapest first. */
    public static final int DEFAULT_RECEIVERS = 30;

    private final HideProblem problem;
    private final Pattern p;
    private final Map<Pattern, Long> watched;
    private final long sigma;
    private final Map<Integer, Map<Pattern, int[]>> vital;
    private final AdaptiveCutHide.DropOracle oracle;
    private final int maxPasses;
    private final int receivers;
    private boolean savingCondition = true;

    private final Map<Integer, Set<Integer>> x = new TreeMap<>();
    private final Map<Integer, Map<Pattern, Integer>> local = new HashMap<>();
    private final Map<Pattern, Long> total = new HashMap<>();
    private final Map<Integer, FrontierDp.Curve> receiving = new HashMap<>();
    private int moves;
    private int evaluated;

    /**
     * @param watched the patterns Lost counts, with sup_NO(Q, D)
     * @param vital for every sequence carrying P, the vital positions of every watched pattern that
     *     occurs in it (the weight of the receiving side of an allocation move)
     */
    public MaskLocalSearch(HideProblem problem, Map<Pattern, Long> watched, long sigma,
                           Map<Integer, Map<Pattern, int[]>> vital, AdaptiveCutHide.DropOracle oracle,
                           int maxPasses, int receivers) {
        if (problem.sensitive().size() != 1
                || !ExactCurve.applies(problem.gap(), problem.sensitive().get(0))) {
            throw new IllegalArgumentException("the local search needs one sensitive pattern and a gap "
                    + "where the exact program applies; got " + problem.sensitive().size()
                    + " pattern(s), mode " + problem.mode() + ", gap " + problem.gap());
        }
        this.problem = problem;
        this.p = problem.sensitive().get(0);
        this.watched = watched;
        this.sigma = sigma;
        this.vital = vital;
        this.oracle = oracle;
        this.maxPasses = maxPasses;
        this.receivers = receivers;
    }

    /**
     * With {@code false}, every move of the neighbourhood is evaluated even when lemma L9 says it
     * cannot lower Lost. The moves made are the same either way; only the work differs. The switch
     * exists to measure that claim and the work the condition saves.
     */
    public MaskLocalSearch withSavingCondition(boolean on) {
        this.savingCondition = on;
        return this;
    }

    /** Runs the search from {@code start}, which must be a minimum-mask hiding set. */
    public Result run(Sanitization start) {
        for (Map.Entry<Integer, Set<Integer>> e : start.masked().entrySet()) {
            if (!e.getValue().isEmpty()) {
                Set<Integer> set = new TreeSet<>(e.getValue());
                x.put(e.getKey(), set);
                Map<Pattern, Integer> d = dropIn(e.getKey(), set);
                local.put(e.getKey(), d);
                for (Map.Entry<Pattern, Integer> q : d.entrySet()) {
                    total.merge(q.getKey(), (long) q.getValue(), Long::sum);
                }
            }
        }
        int passes = 0;
        boolean nothingSaveable = false;
        while (passes < maxPasses) {
            Deadline.check();
            if (savingCondition && !anySaveable()) {
                nothingSaveable = true;
                break;
            }
            passes++;
            boolean improved = false;
            for (int s : damaging()) {
                improved |= oneSequenceMove(s);
            }
            // HIDE-SEQ fixes the remaining support of every sequence at r - 1 at most, so a move
            // between two sequences does not exist there: one-sequence moves only.
            if (!improved && problem.isDatabaseMode()) {
                improved = allocationMove();
            }
            if (!improved) {
                break;
            }
        }
        Map<Integer, Set<Integer>> masks = new TreeMap<>();
        for (Map.Entry<Integer, Set<Integer>> e : x.entrySet()) {
            masks.put(e.getKey(), new TreeSet<>(e.getValue()));
        }
        return new Result(masks, lost(), moves, passes, evaluated, nothingSaveable);
    }

    /** The drops of the watched patterns in sequence s when positions m are masked there. */
    private Map<Pattern, Integer> dropIn(int s, Set<Integer> m) {
        Sanitization only = new Sanitization(problem.database());
        for (int i : m) {
            only.mask(s, i);
        }
        Map<Pattern, Integer> d = new HashMap<>();
        for (Map.Entry<Pattern, Long> e : oracle.drops(only).entrySet()) {
            if (watched.containsKey(e.getKey()) && e.getValue() > 0) {
                d.put(e.getKey(), Math.toIntExact(e.getValue()));
            }
        }
        return d;
    }

    private boolean isLost(Pattern q, long drop) {
        return watched.get(q) - drop < sigma;
    }

    /** e_Q = D_Q - m_Q + 1 for a lost pattern: how much D_Q must fall to save it. */
    private long excess(Pattern q) {
        return total.getOrDefault(q, 0L) - (watched.get(q) - sigma + 1) + 1;
    }

    private int lost() {
        int lost = 0;
        for (Map.Entry<Pattern, Long> e : watched.entrySet()) {
            if (isLost(e.getKey(), total.getOrDefault(e.getKey(), 0L))) {
                lost++;
            }
        }
        return lost;
    }

    /** Masked sequences that lower the support of a pattern now lost, in increasing index. */
    private List<Integer> damaging() {
        List<Integer> out = new ArrayList<>();
        for (int s : x.keySet()) {
            for (Pattern q : local.get(s).keySet()) {
                if (isLost(q, total.getOrDefault(q, 0L))) {
                    out.add(s);
                    break;
                }
            }
        }
        return out;
    }

    /** Some lost pattern whose excess the two sequences hurting it most could undo (L9). */
    private boolean anySaveable() {
        Map<Pattern, long[]> top = new HashMap<>();
        for (Map<Pattern, Integer> d : local.values()) {
            for (Map.Entry<Pattern, Integer> e : d.entrySet()) {
                long[] t = top.computeIfAbsent(e.getKey(), k -> new long[2]);
                if (e.getValue() > t[0]) {
                    t[1] = t[0];
                    t[0] = e.getValue();
                } else if (e.getValue() > t[1]) {
                    t[1] = e.getValue();
                }
            }
        }
        for (Map.Entry<Pattern, long[]> e : top.entrySet()) {
            Pattern q = e.getKey();
            if (isLost(q, total.getOrDefault(q, 0L)) && excess(q) <= e.getValue()[0] + e.getValue()[1]) {
                return true;
            }
        }
        return false;
    }

    /** L9 for the sequences {@code a} and {@code b} (b may be -1): could changing them save a lost pattern? */
    private boolean couldSave(int a, int b) {
        Map<Pattern, Integer> da = local.getOrDefault(a, Map.of());
        Map<Pattern, Integer> db = b < 0 ? Map.of() : local.getOrDefault(b, Map.of());
        Set<Pattern> keys = new HashSet<>(da.keySet());
        keys.addAll(db.keySet());
        for (Pattern q : keys) {
            if (isLost(q, total.getOrDefault(q, 0L))
                    && excess(q) <= da.getOrDefault(q, 0) + db.getOrDefault(q, 0)) {
                return true;
            }
        }
        return false;
    }

    /** Change in Lost if the drops of s1 (and s2) are replaced by new1 (and new2). */
    private int delta(int s1, Map<Pattern, Integer> new1, int s2, Map<Pattern, Integer> new2) {
        Map<Pattern, Integer> old1 = local.getOrDefault(s1, Map.of());
        Map<Pattern, Integer> old2 = s2 < 0 ? Map.of() : local.getOrDefault(s2, Map.of());
        Set<Pattern> touched = new HashSet<>(old1.keySet());
        touched.addAll(old2.keySet());
        touched.addAll(new1.keySet());
        touched.addAll(new2.keySet());
        int delta = 0;
        for (Pattern q : touched) {
            long before = total.getOrDefault(q, 0L);
            long after = before - old1.getOrDefault(q, 0) - old2.getOrDefault(q, 0)
                    + new1.getOrDefault(q, 0) + new2.getOrDefault(q, 0);
            delta += (isLost(q, after) ? 1 : 0) - (isLost(q, before) ? 1 : 0);
        }
        return delta;
    }

    private void apply(int s, Set<Integer> set, Map<Pattern, Integer> drop) {
        for (Map.Entry<Pattern, Integer> q : local.getOrDefault(s, Map.of()).entrySet()) {
            total.merge(q.getKey(), (long) -q.getValue(), Long::sum);
        }
        for (Map.Entry<Pattern, Integer> q : drop.entrySet()) {
            total.merge(q.getKey(), (long) q.getValue(), Long::sum);
        }
        if (set.isEmpty()) {
            x.remove(s);
            local.remove(s);
        } else {
            x.put(s, set);
            local.put(s, drop);
        }
        receiving.remove(s);
    }

    private int remaining(int s) {
        EventSequence sequence = problem.database().get(s);
        return SupportNO.of(p, x.containsKey(s) ? sequence.mask(x.get(s)) : sequence, gap());
    }

    private GapConstraint gap() {
        return problem.gap();
    }

    private static Set<Integer> toSet(int[] positions) {
        Set<Integer> out = new TreeSet<>();
        for (int i : positions) {
            out.add(i);
        }
        return out;
    }

    /** Tries the candidates of sequence s and applies the best one if it lowers Lost. */
    private boolean oneSequenceMove(int s) {
        if (savingCondition && !couldSave(s, -1)) {
            return false;  // L9: no candidate for s can save a lost pattern now
        }
        EventSequence sequence = problem.database().get(s);
        Set<Integer> current = x.get(s);
        int target = remaining(s);
        List<long[]> weightings = new ArrayList<>();
        long[] avoidAll = new long[sequence.length()];
        for (int i : current) {
            avoidAll[i] = CostCurve.SCALE;
        }
        weightings.add(avoidAll);
        for (int i : current) {
            long[] avoidOne = new long[sequence.length()];
            avoidOne[i] = CostCurve.SCALE;
            weightings.add(avoidOne);
        }
        Set<Set<Integer>> seen = new HashSet<>();
        seen.add(current);
        int bestDelta = 0;
        Set<Integer> bestSet = null;
        Map<Pattern, Integer> bestDrop = null;
        for (long[] w : weightings) {
            FrontierDp.Curve curve = ExactCurve.solve(p, sequence, gap(), w);
            if (curve.masks()[target] != current.size()) {
                continue;
            }
            Set<Integer> candidate = toSet(curve.positions()[target]);
            if (!seen.add(candidate)) {
                continue;
            }
            evaluated++;
            Map<Pattern, Integer> drop = dropIn(s, candidate);
            int delta = delta(s, drop, -1, Map.of());
            if (delta < bestDelta) {
                bestDelta = delta;
                bestSet = candidate;
                bestDrop = drop;
            }
        }
        if (bestSet == null) {
            return false;
        }
        apply(s, bestSet, bestDrop);
        moves++;
        return true;
    }

    /** The curve of sequence s that keeps its masks and adds positions vital for few patterns. */
    private FrontierDp.Curve receivingCurve(int s) {
        return receiving.computeIfAbsent(s, k -> {
            EventSequence sequence = problem.database().get(k);
            int[] count = new int[sequence.length()];
            for (int[] positions : vital.getOrDefault(k, Map.of()).values()) {
                for (int i : positions) {
                    count[i]++;
                }
            }
            Set<Integer> current = x.getOrDefault(k, Set.of());
            long[] w = new long[sequence.length()];
            for (int i = 0; i < w.length; i++) {
                w[i] = current.contains(i) ? 0 : CostCurve.SCALE * (1 + count[i]);
            }
            return ExactCurve.solve(p, sequence, gap(), w);
        });
    }

    /** Applies the allocation move that lowers Lost most, if any. */
    private boolean allocationMove() {
        Map<Integer, Integer> left = new HashMap<>();
        List<long[]> ranked = new ArrayList<>();
        for (int s : vital.keySet()) {
            int r = remaining(s);
            left.put(s, r);
            if (r == 0) {
                continue;
            }
            FrontierDp.Curve c = receivingCurve(s);
            ranked.add(new long[] {c.weight()[r - 1] - c.weight()[r], s});
        }
        ranked.sort(Comparator.comparingLong((long[] v) -> v[0]).thenComparingLong(v -> v[1]));
        List<Integer> takers = new ArrayList<>();
        for (int k = 0; k < Math.min(receivers, ranked.size()); k++) {
            takers.add((int) ranked.get(k)[1]);
        }
        int bestDelta = 0;
        int best1 = -1;
        int best2 = -1;
        Set<Integer> set1 = null;
        Set<Integer> set2 = null;
        Map<Pattern, Integer> drop1 = null;
        Map<Pattern, Integer> drop2 = null;
        for (int s1 : damaging()) {
            int r1 = left.get(s1);
            EventSequence sequence = problem.database().get(s1);
            Set<Integer> current = x.get(s1);
            long[] keep = new long[sequence.length()];
            for (int i = 0; i < keep.length; i++) {
                keep[i] = current.contains(i) ? 0 : CostCurve.SCALE;
            }
            FrontierDp.Curve c1 = ExactCurve.solve(p, sequence, gap(), keep);
            if (r1 + 1 > c1.supNO()) {
                continue;
            }
            Set<Integer> a1 = toSet(c1.positions()[r1 + 1]);
            Map<Pattern, Integer> e1 = null;
            for (int s2 : takers) {
                if (s2 == s1 || (savingCondition && !couldSave(s1, s2))) {
                    continue;  // L9 for the pair
                }
                int r2 = left.get(s2);
                FrontierDp.Curve c2 = receivingCurve(s2);
                int before = current.size() + x.getOrDefault(s2, Set.of()).size();
                if (c1.masks()[r1 + 1] + c2.masks()[r2 - 1] != before) {
                    continue;
                }
                if (e1 == null) {
                    e1 = a1.isEmpty() ? new HashMap<>() : dropIn(s1, a1);
                }
                evaluated++;
                Set<Integer> a2 = toSet(c2.positions()[r2 - 1]);
                Map<Pattern, Integer> e2 = dropIn(s2, a2);
                int delta = delta(s1, e1, s2, e2);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    best1 = s1;
                    best2 = s2;
                    set1 = a1;
                    set2 = a2;
                    drop1 = e1;
                    drop2 = e2;
                }
            }
        }
        if (best1 < 0) {
            return false;
        }
        apply(best1, set1, drop1);
        apply(best2, set2, drop2);
        moves++;
        return true;
    }
}
