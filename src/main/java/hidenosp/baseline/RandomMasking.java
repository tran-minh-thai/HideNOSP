package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.core.flow.NettreeFlow;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tier (a) of section 11.3, the two-way control of [R20]: the sequence may be picked by heuristic
 * (H) or at random (R), and so may the position, giving HH, HR, RH and RR. It shows that the
 * problem does not solve itself, and it separates the contribution of the sequence choice from the
 * contribution of the position choice.
 *
 * <p>The heuristic sequence order is by descending sup_NO and the heuristic position rule is the
 * vital-node rule of L5, so that RH and HR each change exactly one factor against HH.
 *
 * <p>The seed is a declared parameter of the run, written into row 3 of the protocol table.
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
    public String name() {
        return (sequenceChoice == SequenceChoice.HEURISTIC ? "H" : "R")
                + (positionChoice == PositionChoice.HEURISTIC ? "H" : "R") + "-random";
    }

    @Override
    public Sanitization run(HideProblem problem) {
        if (problem.sensitive().size() != 1) {
            throw new UnsupportedOperationException("the two-way control is stated for one pattern");
        }
        Pattern pattern = problem.sensitive().get(0);
        Random random = new Random(seed);
        Sanitization result = new Sanitization(problem.database()).method(name());
        int n = problem.database().size();
        int[] support = SupportNO.perSequence(pattern, problem.database(), problem.gap());

        List<Integer> order = new ArrayList<>();
        for (int s = 0; s < n; s++) {
            if (support[s] > 0) {
                order.add(s);
            }
        }
        if (sequenceChoice == SequenceChoice.HEURISTIC) {
            order.sort((a, b) -> Integer.compare(support[b], support[a]));
        } else {
            java.util.Collections.shuffle(order, random);
        }

        long remaining;
        if (problem.isDatabaseMode()) {
            long total = 0;
            for (int f : support) {
                total += f;
            }
            remaining = Math.max(0, total - problem.threshold() + 1);
        } else {
            remaining = Long.MAX_VALUE;
        }

        for (int s : order) {
            if (remaining <= 0) {
                break;
            }
            int target = problem.isDatabaseMode()
                    ? (int) Math.max(0, support[s] - remaining)
                    : problem.threshold() - 1;
            remaining -= maskSequence(problem, pattern, s, target, random, result);
        }
        return result;
    }

    /** Masks one sequence down to {@code target} and returns the support actually removed. */
    private int maskSequence(HideProblem problem, Pattern pattern, int index, int target,
                             Random random, Sanitization result) {
        EventSequence sequence = problem.database().get(index);
        Nettree nettree = Nettree.build(pattern, sequence, problem.gap());
        NettreeFlow flow = new NettreeFlow(nettree);
        int before = flow.supNO();
        while (flow.supNO() > target) {
            int position;
            if (positionChoice == PositionChoice.HEURISTIC) {
                int[] vital = flow.vitalPositions();
                if (vital.length == 0) {
                    break;
                }
                position = vital[random.nextInt(vital.length)];
            } else {
                List<Integer> candidates = new ArrayList<>();
                for (int i = 0; i < sequence.length(); i++) {
                    for (int v : nettree.nodesAtPosition(i)) {
                        if (!flow.isRemoved(v)) {
                            candidates.add(i);
                            break;
                        }
                    }
                }
                if (candidates.isEmpty()) {
                    break;
                }
                position = candidates.get(random.nextInt(candidates.size()));
            }
            flow.maskPosition(position);
            result.mask(index, position);
        }
        return before - flow.supNO();
    }
}
