package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;

import java.util.List;

/**
 * Tier (a) of section 11.3: mask the position lying on the most occurrences, repeat. This is the
 * local rule of [R20] transferred to sup_NO, stripped of everything else, and it is the object of
 * RQ3 and of gap G3.
 *
 * <p>What separates this from {@link Hha} is deliberate and is the whole point of having both:
 * this class takes the sequences in their natural order, while HHA orders them by the size of
 * their match set. Reporting the two side by side separates the contribution of the position rule
 * from the contribution of the sequence ordering.
 *
 * <p>The rule is not optimal for sup_NO. On S = a b a a b a b a a with P = ab and gap [0,2] the
 * position 4 lies on two occurrences, as many as any other, yet masking it leaves the three
 * pairwise non-overlapping occurrences untouched; the heuristic then needs two masks where one is
 * enough. Lemma L5 says why: that position carries no vital node.
 *
 * <p>Ties are broken at the leftmost position, which is the variant behind the counts of section
 * 8.2; the probe script also measured the best case over all tie-breaks, which needs exhaustive
 * search and stays out of the Java code.
 *
 * <p>With several sensitive patterns the rule is unchanged: the count through a position is summed
 * over the patterns that still need a drop. Section 11.5 sweeps |S| over {1, 5, 10, 20}, so the
 * control has to handle all four.
 */
public final class OccurrenceCountHeuristic implements HidingMethod {

    @Override
    public String name() {
        return "occurrence-count-heuristic";
    }

    @Override
    public Sanitization run(HideProblem problem) {
        Sanitization result = new Sanitization(problem.database()).method(name());
        List<Pattern> sensitive = problem.sensitive();
        int sequenceCount = problem.database().size();

        int[][] support = new int[sensitive.size()][sequenceCount];
        long[] total = new long[sensitive.size()];
        for (int p = 0; p < sensitive.size(); p++) {
            for (int s = 0; s < sequenceCount; s++) {
                support[p][s] = SupportNO.of(sensitive.get(p), problem.database().get(s), problem.gap());
                total[p] += support[p][s];
            }
        }

        long[] needed = new long[sensitive.size()];
        if (problem.isDatabaseMode()) {
            for (int p = 0; p < sensitive.size(); p++) {
                needed[p] = Math.max(0, total[p] - problem.threshold() + 1);
            }
        }

        // Natural order, left to right: this control deliberately does not choose its sequences.
        for (int s = 0; s < sequenceCount; s++) {
            if (problem.isDatabaseMode() && allSatisfied(needed)) {
                break;
            }
            long[] here = new long[sensitive.size()];
            for (int p = 0; p < sensitive.size(); p++) {
                here[p] = problem.isDatabaseMode()
                        ? Math.min(needed[p], support[p][s])
                        : Math.max(0, support[p][s] - problem.threshold() + 1);
            }
            if (allSatisfied(here)) {
                continue;
            }
            maskSequence(problem, s, here, needed, result);
        }
        return result;
    }

    private void maskSequence(HideProblem problem, int sequenceIndex, long[] here, long[] needed,
                              Sanitization result) {
        EventSequence sequence = problem.database().get(sequenceIndex);
        NettreeCutIndex index = new NettreeCutIndex(sequence, problem.gap());
        for (Pattern p : problem.sensitive()) {
            index.add(p, true);
        }
        while (!allSatisfied(here)) {
            int target = -1;
            long best = 0;
            for (int i = 0; i < sequence.length(); i++) {
                if (index.isMasked(i)) {
                    continue;
                }
                long through = 0;
                for (int p = 0; p < here.length; p++) {
                    if (here[p] > 0) {
                        through += index.occurrencesThrough(p, i);
                    }
                }
                if (through > best) {
                    best = through;
                    target = i;
                }
            }
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

    private static boolean allSatisfied(long[] remaining) {
        for (long value : remaining) {
            if (value > 0) {
                return false;
            }
        }
        return true;
    }
}
