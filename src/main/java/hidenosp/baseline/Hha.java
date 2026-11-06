package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * HHA, the sequence hiding algorithm of O. Abul, M. Atzori, F. Bonchi and F. Giannotti, "Hiding
 * sequences", ICDE Workshops 2007 (Algorithm 1), kept unchanged as Algorithm 1 of O. Abul,
 * F. Bonchi and F. Giannotti, "Hiding sequential and spatiotemporal patterns", IEEE TKDE 2010. The
 * name HHA is the one Gkoulalas-Divanis and Loukides (KDD 2011) use for it.
 *
 * <ol>
 *   <li><b>Global heuristic</b> (lines 2 to 5): "sort the sequences in D in ascending order of
 *       matching set size, and remove all matchings in top |D| - psi input sequences". The psi
 *       sequences with the largest matching sets are left intact; every other sequence is
 *       sanitised. The matching set counts every embedding of every sensitive pattern, overlapping
 *       or not, under the gap constraint.</li>
 *   <li><b>Local heuristic</b> (Sanitize): "choose the marking position that is involved in most
 *       matches", iterated until the sequence has no matching left. delta(T[i]) is the number of
 *       matchings of all sensitive patterns that use position i.</li>
 * </ol>
 *
 * <p><b>Where the papers leave a choice open</b>, and what this class does:
 *
 * <ul>
 *   <li><em>Ties of delta</em> go to the leftmost position, and <em>ties of the matching set
 *       size</em> to the lower sequence index, so the higher index is kept. With these the class
 *       reproduces the HHA output of Figure 1(d) of the KDD 2011 paper (see
 *       {@code HhaPaperExampleTest}).</li>
 * </ul>
 *
 * <p><b>Transfer.</b> The papers mark with a symbol outside the alphabet, which is what masking is.
 * The threshold of the papers keeps sup &le; psi; here the database threshold keeps the support
 * below sigma, so psi = sigma - 1. {@link SupportCount#SEQUENCE_COUNT} keeps the psi largest
 * sequences, as the papers do. With {@link SupportCount#NON_OVERLAPPING} a kept sequence costs
 * its largest sup_NO over the sensitive patterns instead of 1, and the kept part is the longest run
 * of largest sequences whose cost stays within sigma - 1; with one pattern that is the budget of
 * the paper in sup_NO units, and with the sequence count it is exactly the rule of the paper. A
 * sequence chosen for sanitising loses every matching, as in the paper, even when fewer masks would
 * bring the database below sigma. Under a per-sequence limit r (HIDE-SEQ) every sequence at or
 * above r is sanitised until each pattern is below r, which is the "occurrence count" extension of
 * the TKDE paper with sup_NO in place of the size of the matching set.
 *
 * <p>{@link Delta} selects how delta is computed; both give the same values. {@code ORIGINAL_DP}
 * recounts the matchings with the position masked, one dynamic program per position, as the proof
 * of Theorem 2 does (with masking rather than deletion, which the gap constraint requires).
 * {@code PATH_COUNTS} reads every delta at once from the forward and backward counts of the index.
 * SC6 times the two against each other.
 *
 * <p><b>The stop-at-threshold variant</b> ({@link #stopAtThreshold()}, SC3 key {@code hha-stop},
 * preregistration section 19.1) changes one thing, the stopping rule under a database threshold: the
 * sequences carrying a sensitive pattern are taken in the order above (ascending matching set, ties
 * to the lower index), and in each the position on the most matchings of the patterns that still
 * need a drop is masked, until every sensitive pattern is below sigma; then it stops. It never keeps
 * a sequence intact by the budget rule and never sanitises past the threshold. Under HIDE-SEQ HHA
 * already stops at r, so the variant equals HHA there. The name describes the behaviour; it is not a
 * published algorithm.
 */
public final class Hha implements HidingMethod {

    /** How delta(T[i]) is computed. The values agree; the cost does not. */
    public enum Delta {
        /** One forward and one backward pass over the Nettree give every delta at once. */
        PATH_COUNTS,
        /** Theorem 2: one count of the matchings per candidate position. */
        ORIGINAL_DP
    }

    private final Delta delta;
    private final SupportCount support;
    private final boolean stopAtThreshold;

    /** The configuration used in the comparison tables. */
    public Hha() {
        this(Delta.PATH_COUNTS, SupportCount.NON_OVERLAPPING);
    }

    public Hha(Delta delta) {
        this(delta, SupportCount.NON_OVERLAPPING);
    }

    public Hha(Delta delta, SupportCount support) {
        this(delta, support, false);
    }

    private Hha(Delta delta, SupportCount support, boolean stopAtThreshold) {
        this.delta = delta;
        this.support = support;
        this.stopAtThreshold = stopAtThreshold;
    }

    /** HHA's order and position rule, stopping as soon as every sensitive support is below sigma. */
    public static Hha stopAtThreshold() {
        return new Hha(Delta.PATH_COUNTS, SupportCount.NON_OVERLAPPING, true);
    }

    @Override
    public String name() {
        return "HHA" + (stopAtThreshold ? "-stop" : "")
                + (support == SupportCount.SEQUENCE_COUNT ? "-original" : "")
                + (delta == Delta.ORIGINAL_DP ? "-dp" : "");
    }

    @Override
    public Sanitization run(HideProblem problem) {
        if (support == SupportCount.SEQUENCE_COUNT && !problem.isDatabaseMode()) {
            throw new IllegalArgumentException("the sequence-count support of the paper is defined "
                    + "for a database threshold only");
        }
        SequenceDatabase database = problem.database();
        List<Pattern> sensitive = problem.sensitive();
        int n = database.size();
        int limit = problem.isDatabaseMode() ? 1 : problem.threshold();

        long[] cost = new long[n];
        long[] matchingSet = new long[n];
        List<Integer> carrying = new ArrayList<>();
        for (int s = 0; s < n; s++) {
            boolean carries = false;
            for (Pattern p : sensitive) {
                int supNO = SupportNO.of(p, database.get(s), problem.gap());
                cost[s] = Math.max(cost[s], support.unit(supNO));
                carries |= supNO >= limit;
            }
            if (carries) {
                carrying.add(s);
                NettreeCutIndex index = index(problem, s);
                long total = 0;
                for (int id = 0; id < index.patternCount(); id++) {
                    total = Math.min(hidenosp.index.PathCounts.SATURATED,
                            total + index.totalOccurrences(id));
                }
                matchingSet[s] = total;
            }
        }

        List<Integer> toSanitise = new ArrayList<>(carrying);
        if (problem.isDatabaseMode() && stopAtThreshold) {
            toSanitise.sort(Comparator.<Integer>comparingLong(s -> matchingSet[s])
                    .thenComparing(Comparator.<Integer>naturalOrder()));
            Sanitization result = new Sanitization(database).method(name());
            long[] needed = new long[sensitive.size()];
            for (int p = 0; p < sensitive.size(); p++) {
                needed[p] = Math.max(0, SupportNO.of(sensitive.get(p), database, problem.gap())
                        - problem.threshold() + 1);
            }
            for (int s : toSanitise) {
                if (!anyNeeded(needed)) {
                    break;
                }
                sanitiseUntilThreshold(problem, s, needed, result);
            }
            Exclude.verify(problem, result, support, name());
            return result;
        }
        if (problem.isDatabaseMode()) {
            toSanitise.sort(Comparator.<Integer>comparingLong(s -> matchingSet[s])
                    .thenComparing(Comparator.<Integer>naturalOrder()));
            long budget = problem.threshold() - 1L;
            long kept = 0;
            int keepFrom = toSanitise.size();
            while (keepFrom > 0 && kept + cost[toSanitise.get(keepFrom - 1)] <= budget) {
                keepFrom--;
                kept += cost[toSanitise.get(keepFrom)];
            }
            toSanitise = new ArrayList<>(toSanitise.subList(0, keepFrom));
        }

        Sanitization result = new Sanitization(database).method(name());
        for (int s : toSanitise) {
            sanitise(problem, s, limit, result);
        }
        Exclude.verify(problem, result, support, name());
        return result;
    }

    /** Sanitize(T, Sh): mask the position on the most matchings until none (or none at r) is left. */
    private void sanitise(HideProblem problem, int s, int limit, Sanitization result) {
        EventSequence sequence = problem.database().get(s);
        NettreeCutIndex index = index(problem, s);
        while (anyAtLimit(index, limit)) {
            int target = bestPosition(problem, sequence, index);
            if (target < 0) {
                throw new IllegalStateException("a sequence still supports a sensitive pattern but "
                        + "no position lies on a matching");
            }
            index.maskPosition(target);
            result.mask(s, target);
        }
    }

    /**
     * The local rule of HHA restricted to the patterns that still need a drop, stopping in the middle
     * of the sequence as soon as none does. {@code needed[p]} falls by the drop of sup_NO of pattern p.
     */
    private void sanitiseUntilThreshold(HideProblem problem, int s, long[] needed, Sanitization result) {
        EventSequence sequence = problem.database().get(s);
        NettreeCutIndex index = index(problem, s);
        while (true) {
            boolean[] active = new boolean[index.patternCount()];
            boolean any = false;
            for (int p = 0; p < index.patternCount(); p++) {
                active[p] = needed[p] > 0 && index.supNO(p) > 0;
                any |= active[p];
            }
            if (!any) {
                return;
            }
            int target = bestPosition(problem, sequence, index, active);
            if (target < 0) {
                throw new IllegalStateException("a pattern still needs a drop in this sequence but "
                        + "no position lies on one of its matchings");
            }
            long[] before = new long[index.patternCount()];
            for (int p = 0; p < index.patternCount(); p++) {
                before[p] = index.supNO(p);
            }
            index.maskPosition(target);
            result.mask(s, target);
            for (int p = 0; p < index.patternCount(); p++) {
                needed[p] -= before[p] - index.supNO(p);
            }
        }
    }

    private static boolean anyNeeded(long[] needed) {
        for (long n : needed) {
            if (n > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyAtLimit(NettreeCutIndex index, int limit) {
        for (int id = 0; id < index.patternCount(); id++) {
            if (index.supNO(id) >= limit) {
                return true;
            }
        }
        return false;
    }

    /** The position on the most matchings of all sensitive patterns; ties go to the leftmost. */
    private int bestPosition(HideProblem problem, EventSequence sequence, NettreeCutIndex index) {
        boolean[] all = new boolean[index.patternCount()];
        Arrays.fill(all, true);
        return bestPosition(problem, sequence, index, all);
    }

    /** The same, counting the matchings of the active patterns only. */
    private int bestPosition(HideProblem problem, EventSequence sequence, NettreeCutIndex index,
                             boolean[] active) {
        long best = 0;
        int target = -1;
        for (int i = 0; i < sequence.length(); i++) {
            if (index.isMasked(i)) {
                continue;
            }
            long through = 0;
            for (int p = 0; p < index.patternCount(); p++) {
                if (!active[p]) {
                    continue;
                }
                long here = delta == Delta.PATH_COUNTS
                        ? index.occurrencesThrough(p, i)
                        : originalDelta(problem, sequence, index, p, i);
                through = Math.min(hidenosp.index.PathCounts.SATURATED, through + here);
            }
            if (through > best) {
                best = through;
                target = i;
            }
        }
        return target;
    }

    /**
     * delta(i) the way Theorem 2 computes it: count the matchings of the sequence, count them again
     * with position i masked, and subtract. Kept so SC6 can time the original against the forward
     * and backward counts of the index.
     */
    private long originalDelta(HideProblem problem, EventSequence sequence, NettreeCutIndex index,
                               int patternId, int position) {
        Pattern pattern = index.pattern(patternId);
        boolean[] masked = new boolean[sequence.length()];
        for (int i = 0; i < sequence.length(); i++) {
            masked[i] = index.isMasked(i);
        }
        long before = countMatches(pattern, sequence, problem.gap(), masked);
        masked[position] = true;
        long after = countMatches(pattern, sequence, problem.gap(), masked);
        return before - after;
    }

    /**
     * The number of matchings of P in S, by the dynamic program of Lemmas 2 to 5 of the papers:
     * q[k][j] counts the matchings of the prefix of length k+1 that end at position j. Saturates at
     * the ceiling of {@link hidenosp.index.PathCounts}, since the count grows exponentially.
     */
    public static long countMatches(Pattern pattern, EventSequence sequence, GapConstraint gap,
                                    boolean[] masked) {
        int n = sequence.length();
        int m = pattern.length();
        long ceiling = hidenosp.index.PathCounts.SATURATED;
        long[][] q = new long[m][n];
        for (int j = 0; j < n; j++) {
            if (matches(sequence, masked, j, pattern.at(0))) {
                q[0][j] = 1;
            }
        }
        for (int k = 1; k < m; k++) {
            for (int j = 0; j < n; j++) {
                if (!matches(sequence, masked, j, pattern.at(k))) {
                    continue;
                }
                long sum = 0;
                int lo = (int) Math.max(0L, (long) j - 1 - gap.gmax());
                int hi = j - 1 - gap.gmin();
                for (int l = lo; l <= hi; l++) {
                    sum = Math.min(ceiling, sum + q[k - 1][l]);
                }
                q[k][j] = sum;
            }
        }
        long total = 0;
        for (int j = 0; j < n; j++) {
            total = Math.min(ceiling, total + q[m - 1][j]);
        }
        return total;
    }

    private static boolean matches(EventSequence sequence, boolean[] masked, int position, int item) {
        return !masked[position] && sequence.at(position) != EventSequence.MASK
                && sequence.at(position) == item;
    }

    private NettreeCutIndex index(HideProblem problem, int sequenceIndex) {
        NettreeCutIndex index = new NettreeCutIndex(problem.database().get(sequenceIndex), problem.gap());
        for (Pattern p : problem.sensitive()) {
            index.add(p, true);
        }
        return index;
    }
}
