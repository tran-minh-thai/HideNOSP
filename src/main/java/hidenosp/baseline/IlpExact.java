package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.metrics.FrequentPatterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Tier (c) of section 11.3: the exact solution on small instances, used to measure how far CUT-HIDE
 * sits from the optimum once several sensitive patterns make the problem NP-hard (L4a).
 *
 * <p>The model is the integer program of section 2: one binary variable per position, the
 * constraint that sup_NO of every sensitive pattern falls below the threshold, and the
 * lexicographic objective, smallest |R| first and fewest lost patterns second. It is solved by
 * branch and bound here rather than by an external solver, which keeps the build self-contained and
 * offline; the decision and its consequence, that only small instances are reachable, are declared
 * in the protocol table.
 *
 * <p>Two facts do the pruning. Only a position carrying a node of a sensitive pattern can ever
 * help, so the candidate set is small. And by remark O1 with lemma L1, one mask lowers the support
 * of a pattern P by at most mu(P), so a partial solution with {@code left} masks still to place
 * cannot close a gap larger than {@code left * mu(P)}.
 *
 * <p>The second objective is optimised only when {@link #withSideEffectObjective} supplies
 * F_sigma(D): without it the class returns some minimum-size solution, which answers "how many
 * masks are needed" but not "how much could be saved". The scenario has to say which of the two it
 * used.
 */
public final class IlpExact implements HidingMethod {

    /** Refuses instead of running forever; small instances are what this class is for. */
    public static final int MAX_CANDIDATES = 64;
    public static final int MAX_MASKS = 12;
    private static final int MAX_OPTIMAL_SETS = 20_000;

    private Map<Pattern, Long> frequentBefore;
    private Set<Pattern> closure;
    private long sigma;

    /**
     * Turns on the second objective: among the solutions of least size, pick one losing the fewest
     * non-sensitive patterns. Enumerating them all is what makes this expensive.
     */
    public IlpExact withSideEffectObjective(Map<Pattern, Long> frequentBefore, Set<Pattern> closure,
                                            long sigma) {
        this.frequentBefore = frequentBefore;
        this.closure = closure;
        this.sigma = sigma;
        return this;
    }

    @Override
    public String name() {
        return frequentBefore == null ? "exact-minR" : "exact-minR-minLost";
    }

    @Override
    public Sanitization run(HideProblem problem) {
        List<int[]> candidates = candidates(problem);
        if (candidates.size() > MAX_CANDIDATES) {
            throw new IllegalArgumentException("the exact method takes at most " + MAX_CANDIDATES
                    + " candidate positions, this instance has " + candidates.size()
                    + "; shrink the instance or use a heuristic");
        }
        int lower = lowerBound(problem);
        for (int k = lower; k <= Math.min(MAX_MASKS, candidates.size()); k++) {
            List<List<int[]>> found = new ArrayList<>();
            search(problem, candidates, 0, k, new ArrayList<>(), found);
            if (!found.isEmpty()) {
                return build(problem, choose(problem, found));
            }
        }
        throw new IllegalStateException("no solution with at most " + MAX_MASKS
                + " masks; raise the cap or shrink the instance");
    }

    /** Only a position carrying a node of a sensitive pattern can lower a sensitive support. */
    private List<int[]> candidates(HideProblem problem) {
        List<int[]> out = new ArrayList<>();
        for (int s = 0; s < problem.database().size(); s++) {
            EventSequence sequence = problem.database().get(s);
            TreeSet<Integer> positions = new TreeSet<>();
            for (Pattern p : problem.sensitive()) {
                Nettree nettree = Nettree.build(p, sequence, problem.gap());
                for (int v = 0; v < nettree.nodeCount(); v++) {
                    positions.add(nettree.position(v));
                }
            }
            for (int position : positions) {
                out.add(new int[] {s, position});
            }
        }
        return out;
    }

    /** By L1 and O1, one mask closes at most mu(P) of the gap of pattern P. */
    private int lowerBound(HideProblem problem) {
        int bound = 0;
        for (Pattern p : problem.sensitive()) {
            long gap = gapOf(problem, p, problem.database());
            bound = Math.max(bound, (int) Math.ceil((double) gap / p.mu()));
        }
        return bound;
    }

    /** How much the support of P still has to fall in the current database. */
    private long gapOf(HideProblem problem, Pattern pattern, SequenceDatabase database) {
        if (problem.isDatabaseMode()) {
            return Math.max(0, SupportNO.of(pattern, database, problem.gap()) - problem.threshold() + 1);
        }
        long worst = 0;
        for (int s = 0; s < database.size(); s++) {
            worst = Math.max(worst,
                    Math.max(0, SupportNO.of(pattern, database.get(s), problem.gap())
                            - problem.threshold() + 1));
        }
        return worst;
    }

    private void search(HideProblem problem, List<int[]> candidates, int from, int left,
                        List<int[]> chosen, List<List<int[]>> found) {
        if (found.size() >= MAX_OPTIMAL_SETS) {
            return;
        }
        SequenceDatabase current = apply(problem.database(), chosen);
        if (left == 0) {
            if (feasible(problem, current)) {
                found.add(new ArrayList<>(chosen));
            }
            return;
        }
        for (Pattern p : problem.sensitive()) {
            if (gapOf(problem, p, current) > (long) left * p.mu()) {
                return;
            }
        }
        for (int i = from; i <= candidates.size() - left; i++) {
            chosen.add(candidates.get(i));
            search(problem, candidates, i + 1, left - 1, chosen, found);
            chosen.remove(chosen.size() - 1);
            if (found.size() >= MAX_OPTIMAL_SETS) {
                return;
            }
        }
    }

    private boolean feasible(HideProblem problem, SequenceDatabase database) {
        for (Pattern p : problem.sensitive()) {
            if (gapOf(problem, p, database) > 0) {
                return false;
            }
        }
        return true;
    }

    /** The second objective, when F_sigma(D) was supplied; otherwise the first solution found. */
    private List<int[]> choose(HideProblem problem, List<List<int[]>> found) {
        if (frequentBefore == null) {
            return found.get(0);
        }
        List<int[]> best = found.get(0);
        int bestLost = Integer.MAX_VALUE;
        for (List<int[]> candidate : found) {
            SequenceDatabase after = apply(problem.database(), candidate);
            int lost = 0;
            for (Map.Entry<Pattern, Long> e : frequentBefore.entrySet()) {
                if (closure.contains(e.getKey())) {
                    continue;
                }
                if (SupportNO.of(e.getKey(), after, problem.gap()) < sigma) {
                    lost++;
                }
            }
            if (lost < bestLost) {
                bestLost = lost;
                best = candidate;
            }
        }
        return best;
    }

    private SequenceDatabase apply(SequenceDatabase database, List<int[]> masks) {
        Map<Integer, List<Integer>> bySequence = new TreeMap<>();
        for (int[] mask : masks) {
            bySequence.computeIfAbsent(mask[0], k -> new ArrayList<>()).add(mask[1]);
        }
        return database.mask(bySequence);
    }

    private Sanitization build(HideProblem problem, List<int[]> masks) {
        Sanitization result = new Sanitization(problem.database()).method(name());
        for (int[] mask : masks) {
            result.mask(mask[0], mask[1]);
        }
        return result;
    }

    /** Convenience for a scenario that already mined F_sigma(D). */
    public static IlpExact withSideEffects(HideProblem problem, Map<Pattern, Long> frequent) {
        Set<Pattern> closure = FrequentPatterns.closure(problem.sensitive(), frequent.keySet());
        return new IlpExact().withSideEffectObjective(frequent, closure, problem.frequentThreshold());
    }
}
