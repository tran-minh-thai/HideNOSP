package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;

import java.util.ArrayList;
import java.util.List;

/**
 * DBSH of Gkoulalas-Divanis and Loukides 2011 [R3], tier (b) of section 11.3.
 *
 * <p>DBSH decides at the level of the database which sequences to sanitise, and inside a chosen
 * sequence it works on one event symbol rather than on positions:
 *
 * <ul>
 *   <li><b>Cost.</b> The cost of a sequence is the smallest number of on-path positions carrying
 *       one symbol of the pattern: {@code min over c in items(P) of |{i on a complete path :
 *       e_i = c}|}. On-path means the position lies on at least one complete occurrence, which is
 *       exactly a Nettree node with a positive forward and a positive backward count.</li>
 *   <li><b>Exclude.</b> The sequences that would be most expensive to sanitise are kept, and the
 *       cheap ones are sanitised. Under the original threshold psi that means keeping the psi - 1
 *       most expensive supporting sequences.</li>
 *   <li><b>Candidates and Update-paths.</b> Inside a sequence, fix the symbol e attaining the cost,
 *       then repeatedly remove the leftmost on-path position carrying e, recomputing the paths after
 *       each removal.</li>
 * </ul>
 *
 * <p>The guarantee of O(ln |TS|) that the authors prove is against the set cover over symbols, not
 * against the least number of positions. That is the point gap G4 makes, and the probe
 * {@code probe/dbsh.py} quantified it: over 400 gap-free databases the total number of deletions
 * matched the optimum every time, while inside single sequences the symbol rule removed more than
 * necessary in 161 of 10643 cases.
 *
 * <p>Two transfers have to be declared where this is used as a baseline. First, [R3] deletes and
 * this implementation masks, because section 11.4 requires the same modification and the same
 * semantics of the masked symbol for every method. Second, {@link #Dbsh(boolean)} with
 * {@code originalProblem} false keeps the symbol rule but moves the target to sup_NO, which the
 * authors never tuned for.
 */
public final class Dbsh implements HidingMethod {

    private final boolean originalProblem;

    /**
     * @param originalProblem true to stop as soon as a sequence no longer contains the pattern,
     *     which is the setting of [R3]; false to drive sup_NO down to the threshold of the problem
     */
    public Dbsh(boolean originalProblem) {
        this.originalProblem = originalProblem;
    }

    /** The transferred version, the one SC3 compares against. */
    public Dbsh() {
        this(false);
    }

    @Override
    public String name() {
        return originalProblem ? "DBSH-original" : "DBSH";
    }

    @Override
    public Sanitization run(HideProblem problem) {
        Sanitization result = new Sanitization(problem.database()).method(name());
        int sequenceCount = problem.database().size();
        List<Pattern> sensitive = problem.sensitive();

        int[][] support = new int[sensitive.size()][sequenceCount];
        long[] total = new long[sensitive.size()];
        for (int p = 0; p < sensitive.size(); p++) {
            for (int s = 0; s < sequenceCount; s++) {
                support[p][s] = SupportNO.of(sensitive.get(p), problem.database().get(s), problem.gap());
                total[p] += support[p][s];
            }
        }

        if (!problem.isDatabaseMode()) {
            for (int s = 0; s < sequenceCount; s++) {
                if (carries(support, s)) {
                    sanitiseSequence(problem, s, perSequenceTarget(problem, support, s), result);
                }
            }
            return result;
        }

        // Exclude: sanitise the cheapest sequences first, keeping the expensive ones intact.
        long[] needed = new long[sensitive.size()];
        for (int p = 0; p < sensitive.size(); p++) {
            needed[p] = Math.max(0, total[p] - problem.threshold() + 1);
        }
        for (int s : orderByAscendingCost(problem, support)) {
            if (allSatisfied(needed)) {
                break;
            }
            long[] here = new long[sensitive.size()];
            for (int p = 0; p < sensitive.size(); p++) {
                here[p] = Math.min(needed[p], support[p][s]);
            }
            long[] removed = sanitiseSequence(problem, s, here, result);
            for (int p = 0; p < sensitive.size(); p++) {
                needed[p] = Math.max(0, needed[p] - removed[p]);
            }
        }
        return result;
    }

    private long[] perSequenceTarget(HideProblem problem, int[][] support, int sequenceIndex) {
        long[] here = new long[support.length];
        for (int p = 0; p < support.length; p++) {
            here[p] = Math.max(0, support[p][sequenceIndex] - problem.threshold() + 1);
        }
        return here;
    }

    /**
     * Sequences that carry a sensitive pattern, cheapest first. The cost of a sequence is the Cost
     * function of [R3], summed over the sensitive patterns it carries.
     */
    private int[] orderByAscendingCost(HideProblem problem, int[][] support) {
        int sequenceCount = problem.database().size();
        long[] cost = new long[sequenceCount];
        List<Integer> order = new ArrayList<>();
        for (int s = 0; s < sequenceCount; s++) {
            if (!carries(support, s)) {
                continue;
            }
            NettreeCutIndex index = index(problem, s);
            long sum = 0;
            for (int p = 0; p < index.patternCount(); p++) {
                sum += symbolCost(index, p)[1];
            }
            cost[s] = sum;
            order.add(s);
        }
        order.sort((a, b) -> Long.compare(cost[a], cost[b]));
        int[] out = new int[order.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = order.get(i);
        }
        return out;
    }

    /**
     * The Cost function of [R3] for one pattern in one sequence: returns {item, count} for the
     * symbol of the pattern with the fewest on-path positions. Returns {@code {-1, 0}} when the
     * pattern has no complete occurrence left.
     */
    private long[] symbolCost(NettreeCutIndex index, int patternId) {
        Pattern pattern = index.pattern(patternId);
        EventSequence sequence = index.sequence();
        long best = Long.MAX_VALUE;
        int bestItem = -1;
        boolean anyPath = false;
        for (int j = 0; j < pattern.length(); j++) {
            int item = pattern.at(j);
            long count = 0;
            for (int i = 0; i < sequence.length(); i++) {
                if (!index.isMasked(i) && sequence.at(i) == item
                        && index.occurrencesThrough(patternId, i) > 0) {
                    count++;
                }
            }
            anyPath |= count > 0;
            if (count > 0 && count < best) {
                best = count;
                bestItem = item;
            }
        }
        return anyPath ? new long[] {bestItem, best} : new long[] {-1, 0};
    }

    /**
     * Candidates and Update-paths inside one sequence: fix the symbol attaining the cost, then
     * remove its leftmost on-path position, recompute, repeat until the target is met.
     *
     * @return how much support each pattern actually lost here
     */
    private long[] sanitiseSequence(HideProblem problem, int sequenceIndex, long[] target,
                                    Sanitization result) {
        NettreeCutIndex index = index(problem, sequenceIndex);
        EventSequence sequence = problem.database().get(sequenceIndex);
        long[] removed = new long[target.length];
        long[] remaining = target.clone();

        while (!allSatisfied(remaining)) {
            int chosenPattern = -1;
            for (int p = 0; p < remaining.length; p++) {
                if (remaining[p] > 0 && index.supNO(p) > 0) {
                    chosenPattern = p;
                    break;
                }
            }
            if (chosenPattern < 0) {
                break;
            }
            long[] cost = symbolCost(index, chosenPattern);
            if (cost[0] < 0) {
                remaining[chosenPattern] = 0;
                continue;
            }
            int item = (int) cost[0];
            int target0 = -1;
            for (int i = 0; i < sequence.length() && target0 < 0; i++) {
                if (!index.isMasked(i) && sequence.at(i) == item
                        && index.occurrencesThrough(chosenPattern, i) > 0) {
                    target0 = i;
                }
            }
            if (target0 < 0) {
                remaining[chosenPattern] = 0;
                continue;
            }
            int[] drop = index.maskPosition(target0);
            result.mask(sequenceIndex, target0);
            for (int p = 0; p < remaining.length; p++) {
                remaining[p] = Math.max(0, remaining[p] - drop[p]);
                removed[p] += drop[p];
            }
            if (originalProblem) {
                for (int p = 0; p < remaining.length; p++) {
                    if (index.supNO(p) == 0) {
                        remaining[p] = 0;
                    }
                }
            }
        }
        return removed;
    }

    private NettreeCutIndex index(HideProblem problem, int sequenceIndex) {
        NettreeCutIndex index = new NettreeCutIndex(problem.database().get(sequenceIndex), problem.gap());
        for (Pattern p : problem.sensitive()) {
            index.add(p, true);
        }
        return index;
    }

    private static boolean carries(int[][] support, int sequenceIndex) {
        for (int[] row : support) {
            if (row[sequenceIndex] > 0) {
                return true;
            }
        }
        return false;
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
