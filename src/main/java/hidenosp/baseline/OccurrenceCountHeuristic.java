package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.flow.NettreeFlow;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.PathCounts;

/**
 * Tier (a) of section 11.3: mask the position lying on the most occurrences, repeat. This is the
 * local rule of [R20] transferred to sup_NO, and the object of RQ3 and of gap G3.
 *
 * <p>The rule is not optimal for sup_NO. On S = a b a a b a b a a with P = ab and gap [0,2] the
 * position 4 lies on two occurrences, as many as any other, yet masking it leaves the three
 * pairwise non-overlapping occurrences untouched; the heuristic then needs two masks where one is
 * enough. Lemma L5 explains why: the position carries no vital node.
 *
 * <p>Ties are broken at the leftmost position, which is the variant used for the counts of section
 * 8.2; the probe script also measured the best case over all tie-breaks, which needs exhaustive
 * search and stays out of the Java code.
 */
public final class OccurrenceCountHeuristic implements HidingMethod {

    @Override
    public String name() {
        return "occurrence-count-heuristic";
    }

    @Override
    public Sanitization run(HideProblem problem) {
        if (problem.sensitive().size() != 1) {
            throw new UnsupportedOperationException(
                    "the local rule is stated for one sensitive pattern; use Hha for several");
        }
        Pattern pattern = problem.sensitive().get(0);
        Sanitization result = new Sanitization(problem.database()).method(name());
        int[] budget = budgetPerSequence(problem, pattern);
        for (int s = 0; s < problem.database().size(); s++) {
            maskSequence(problem, pattern, s, budget[s], result);
        }
        return result;
    }

    /** How much support each sequence has to give up, the same split CUT-HIDE is measured against. */
    private int[] budgetPerSequence(HideProblem problem, Pattern pattern) {
        int n = problem.database().size();
        int[] budget = new int[n];
        int[] support = new int[n];
        for (int s = 0; s < n; s++) {
            support[s] = hidenosp.core.SupportNO.of(pattern, problem.database().get(s), problem.gap());
        }
        if (!problem.isDatabaseMode()) {
            for (int s = 0; s < n; s++) {
                budget[s] = Math.max(0, support[s] - problem.threshold() + 1);
            }
            return budget;
        }
        long total = 0;
        for (int f : support) {
            total += f;
        }
        long needed = Math.max(0, total - problem.threshold() + 1);
        // Left to right over the sequences, as the sequence ordering of [R20] does by match-set size.
        for (int s = 0; s < n && needed > 0; s++) {
            int take = (int) Math.min(needed, support[s]);
            budget[s] = take;
            needed -= take;
        }
        return budget;
    }

    private void maskSequence(HideProblem problem, Pattern pattern, int index, int drop,
                              Sanitization result) {
        if (drop <= 0) {
            return;
        }
        EventSequence sequence = problem.database().get(index);
        Nettree nettree = Nettree.build(pattern, sequence, problem.gap());
        NettreeFlow flow = new NettreeFlow(nettree);
        int target = flow.supNO() - drop;
        while (flow.supNO() > target) {
            PathCounts counts = PathCounts.of(nettree, flow.removedFlags());
            int best = -1;
            long bestCount = 0;
            for (int i = 0; i < sequence.length(); i++) {
                long through = counts.throughPosition(i);
                if (through > bestCount) {
                    bestCount = through;
                    best = i;
                }
            }
            if (best < 0) {
                return;
            }
            flow.maskPosition(best);
            result.mask(index, best);
        }
    }
}
