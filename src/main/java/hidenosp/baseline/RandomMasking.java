// SPDX-License-Identifier: MIT
package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Tier (a) of the baselines, the two-way control of [R20]: the sequence may be picked by heuristic
 * (H) or at random (R), and so may the position, giving HH, HR, RH and RR. It shows that the
 * problem does not solve itself, and it separates the contribution of the sequence choice from the
 * contribution of the position choice.
 *
 * <p>The heuristic sequence order is by descending sup_NO summed over the sensitive patterns, and
 * the heuristic position rule draws uniformly among the vital positions of L5, so that RH and HR
 * each change exactly one factor against HH. The random position rule draws uniformly among the
 * positions that carry a Nettree node of a pattern still above its threshold: drawing from the
 * whole sequence instead would waste most masks on positions that cannot matter, which would make
 * the control weak for the wrong reason.
 *
 * <p>With several sensitive patterns the rules are unchanged; only the candidate set becomes the
 * union over the patterns that still need a drop. The experiments sweep |S| over {1, 5, 10, 20}, so
 * a control that only handled one pattern would leave three quarters of that sweep without a
 * tier (a) row.
 *
 * <p>The seed is a declared parameter of the run, written into row 3 of the protocol table, and
 * the preregistration fixes ten seeds with the median taken over the ten runs.
 */
public final class RandomMasking implements HidingMethod {

    public enum SequenceChoice { HEURISTIC, RANDOM }

    public enum PositionChoice { HEURISTIC, RANDOM }

    private final SequenceChoice sequenceChoice;
    private final PositionChoice positionChoice;
    private final long seed;

    public RandomMasking(SequenceChoice sequenceChoice, PositionChoice positionChoice, long seed) {
        this.sequenceChoice = sequenceChoice;
        this.positionChoice = positionChoice;
        this.seed = seed;
    }

    @Override
    public boolean isRandomized() {
        return true;
    }

    @Override
    public String name() {
        return (sequenceChoice == SequenceChoice.HEURISTIC ? "H" : "R")
                + (positionChoice == PositionChoice.HEURISTIC ? "H" : "R") + "-random";
    }

    @Override
    public Sanitization run(HideProblem problem) {
        Random random = new Random(seed);
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

        for (int s : order(problem, support, random)) {
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
            maskSequence(problem, s, here, needed, random, result);
        }
        return result;
    }

    /** Sequences that carry a sensitive pattern, ordered by the H or R rule. */
    private int[] order(HideProblem problem, int[][] support, Random random) {
        int sequenceCount = problem.database().size();
        long[] weight = new long[sequenceCount];
        List<Integer> order = new ArrayList<>();
        for (int s = 0; s < sequenceCount; s++) {
            long sum = 0;
            for (int[] row : support) {
                sum += row[s];
            }
            if (sum > 0) {
                weight[s] = sum;
                order.add(s);
            }
        }
        if (sequenceChoice == SequenceChoice.HEURISTIC) {
            order.sort((a, b) -> Long.compare(weight[b], weight[a]));
        } else {
            Collections.shuffle(order, random);
        }
        int[] out = new int[order.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = order.get(i);
        }
        return out;
    }

    /** Masks inside one sequence until its share of the required drop is reached. */
    private void maskSequence(HideProblem problem, int sequenceIndex, long[] here, long[] needed,
                              Random random, Sanitization result) {
        EventSequence sequence = problem.database().get(sequenceIndex);
        NettreeCutIndex index = new NettreeCutIndex(sequence, problem.gap());
        for (Pattern p : problem.sensitive()) {
            index.add(p, true);
        }
        while (!allSatisfied(here)) {
            List<Integer> candidates = candidates(index, sequence, here);
            if (candidates.isEmpty()) {
                return;
            }
            int position = candidates.get(random.nextInt(candidates.size()));
            int[] drop = index.maskPosition(position);
            result.mask(sequenceIndex, position);
            for (int p = 0; p < here.length; p++) {
                here[p] = Math.max(0, here[p] - drop[p]);
                needed[p] = Math.max(0, needed[p] - drop[p]);
            }
        }
    }

    /**
     * The positions this variant may draw from, over the patterns that still need a drop: the vital
     * ones for the heuristic rule, and every position carrying a node for the random rule.
     */
    private List<Integer> candidates(NettreeCutIndex index, EventSequence sequence, long[] here) {
        java.util.TreeSet<Integer> out = new java.util.TreeSet<>();
        for (int p = 0; p < here.length; p++) {
            if (here[p] == 0) {
                continue;
            }
            if (positionChoice == PositionChoice.HEURISTIC) {
                for (int position : index.vitalPositions(p)) {
                    out.add(position);
                }
            } else {
                for (int i = 0; i < sequence.length(); i++) {
                    if (index.isMasked(i)) {
                        continue;
                    }
                    for (int v : index.nettree(p).nodesAtPosition(i)) {
                        if (!index.flow(p).isRemoved(v)) {
                            out.add(i);
                            break;
                        }
                    }
                }
            }
        }
        return new ArrayList<>(out);
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
