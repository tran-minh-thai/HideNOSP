package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;

import java.util.ArrayList;
import java.util.List;

/**
 * HHA of Abul, Atzori, Bonchi and Giannotti 2007 [R20], tier (b) of section 11.3.
 *
 * <p>The method has two parts and the paper measures them separately.
 *
 * <ul>
 *   <li><b>The position rule.</b> Mask the position lying on the most matches, repeat. The quantity
 *       the paper calls delta(i) is the number of occurrences through position i, that is
 *       |M_T| - |M_T with i masked|. Ties go to the leftmost position, as in the probe scripts that
 *       produced the numbers of section 8.2.</li>
 *   <li><b>The sequence ordering.</b> Sequences are taken in decreasing size of their match set,
 *       so the sequences with the most occurrences are sanitised first.</li>
 * </ul>
 *
 * <p>{@link Delta} selects how delta is computed, and both ways return the same numbers. {@code
 * ORIGINAL_DP} is the per-position dynamic program of theorem 2 of [R20]: one count over the whole
 * sequence for every candidate position. {@code PATH_COUNTS} is the forward and backward count of
 * the index, which gives every delta in one pass. SC6 times the two against each other, so the
 * original must stay available; timing the index against a straw man would prove nothing.
 *
 * <p>The rule is not optimal for sup_NO, which is the subject of RQ3 and of gap G3: on
 * S = a b a a b a b a a with P = ab and gap [0,2], position 4 lies on two occurrences, as many as
 * any other, yet masking it leaves three pairwise non-overlapping occurrences untouched. Lemma L5
 * says why: that position carries no vital node.
 *
 * <p>The authors never tuned this rule for a non-overlapping count, and the paper has to say so
 * where it reports the comparison.
 */
public final class Hha implements HidingMethod {

    /** How delta(i) is computed. The values agree; the cost does not. */
    public enum Delta {
        /** One forward and one backward pass over the Nettree give every delta at once. */
        PATH_COUNTS,
        /** Theorem 2 of [R20]: one match count per candidate position. */
        ORIGINAL_DP
    }

    private final Delta delta;

    public Hha(Delta delta) {
        this.delta = delta;
    }

    /** The configuration used in the comparison tables. */
    public Hha() {
        this(Delta.PATH_COUNTS);
    }

    @Override
    public String name() {
        return "HHA-" + (delta == Delta.PATH_COUNTS ? "pathcounts" : "dp");
    }

    @Override
    public Sanitization run(HideProblem problem) {
        Sanitization result = new Sanitization(problem.database()).method(name());
        List<Pattern> sensitive = problem.sensitive();
        int patternCount = sensitive.size();
        int sequenceCount = problem.database().size();

        int[][] support = new int[patternCount][sequenceCount];
        long[] total = new long[patternCount];
        for (int p = 0; p < patternCount; p++) {
            for (int s = 0; s < sequenceCount; s++) {
                support[p][s] = SupportNO.of(sensitive.get(p), problem.database().get(s), problem.gap());
                total[p] += support[p][s];
            }
        }

        long[] needed = new long[patternCount];
        if (problem.isDatabaseMode()) {
            for (int p = 0; p < patternCount; p++) {
                needed[p] = Math.max(0, total[p] - problem.threshold() + 1);
            }
        }

        for (int s : orderByMatchSetSize(problem, support)) {
            if (problem.isDatabaseMode() && allSatisfied(needed)) {
                break;
            }
            sanitiseSequence(problem, s, support, needed, result);
        }
        return result;
    }

    /** Sequences in decreasing size of their match set, the ordering rule of [R20]. */
    private int[] orderByMatchSetSize(HideProblem problem, int[][] support) {
        int sequenceCount = problem.database().size();
        long[] size = new long[sequenceCount];
        List<Integer> order = new ArrayList<>();
        for (int s = 0; s < sequenceCount; s++) {
            boolean carries = false;
            for (int[] row : support) {
                carries |= row[s] > 0;
            }
            if (!carries) {
                continue;
            }
            NettreeCutIndex index = index(problem, s);
            long matches = 0;
            for (int id = 0; id < index.patternCount(); id++) {
                matches = Math.min(Long.MAX_VALUE / 4, matches + index.totalOccurrences(id));
            }
            size[s] = matches;
            order.add(s);
        }
        order.sort((a, b) -> Long.compare(size[b], size[a]));
        int[] out = new int[order.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = order.get(i);
        }
        return out;
    }

    /** Masks inside one sequence until its share of the required drop is reached. */
    private void sanitiseSequence(HideProblem problem, int sequenceIndex, int[][] support,
                                  long[] needed, Sanitization result) {
        EventSequence sequence = problem.database().get(sequenceIndex);
        NettreeCutIndex index = index(problem, sequenceIndex);
        long[] here = new long[support.length];
        for (int p = 0; p < support.length; p++) {
            here[p] = problem.isDatabaseMode()
                    ? Math.min(needed[p], support[p][sequenceIndex])
                    : Math.max(0, support[p][sequenceIndex] - problem.threshold() + 1);
        }
        while (!allSatisfied(here)) {
            int target = bestPosition(problem, sequence, index, here);
            if (target < 0) {
                return;
            }
            int[] drop = index.maskPosition(target);
            result.mask(sequenceIndex, target);
            for (int p = 0; p < here.length; p++) {
                here[p] = Math.max(0, here[p] - drop[p]);
                needed[p] = Math.max(0, needed[p] - drop[p]);
            }
        }
    }

    /** The position on the most matches, counted over the patterns that still need a drop. */
    private int bestPosition(HideProblem problem, EventSequence sequence, NettreeCutIndex index,
                             long[] here) {
        long best = 0;
        int target = -1;
        for (int i = 0; i < sequence.length(); i++) {
            if (index.isMasked(i)) {
                continue;
            }
            long through = 0;
            for (int p = 0; p < here.length; p++) {
                if (here[p] > 0) {
                    through += delta == Delta.PATH_COUNTS
                            ? index.occurrencesThrough(p, i)
                            : originalDelta(problem, sequence, index, p, i);
                }
            }
            if (through > best) {
                best = through;
                target = i;
            }
        }
        return target;
    }

    /**
     * delta(i) the way theorem 2 of [R20] computes it: count the matches of the sequence, count
     * them again with position i masked, and subtract. Kept so SC6 can time the original against
     * the forward and backward counts of the index.
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
     * The number of occurrences of P in S, by the dynamic program of lemmas 3 and 4 of [R20]:
     * Q[k][j] counts the matches of the prefix of length k+1 that end at position j. Saturates at
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

    private static boolean allSatisfied(long[] needed) {
        for (long value : needed) {
            if (value > 0) {
                return false;
            }
        }
        return true;
    }
}
