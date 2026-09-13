package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SBSH of Gkoulalas-Divanis and Loukides 2011 [R3], tier (b) of section 11.3.
 *
 * <p>SBSH decides inside a sequence which positions are safe to remove:
 *
 * <ul>
 *   <li><b>Annotate.</b> Colour each position by what removing it would cost. A position is
 *       <em>red</em> when removing it would push a non-sensitive pattern sitting exactly at the
 *       threshold below it; <em>black</em> when it lies on a sensitive occurrence and is not red;
 *       <em>grey</em> when it carries no sensitive node at all.</li>
 *   <li><b>Replace.</b> When the position the rule would take is red, replace it by a set of black
 *       positions that cut every path through it, searched within a radius of r arcs. The authors
 *       state the radius exists to save computation.</li>
 * </ul>
 *
 * <p>For one pattern with distinct items that replacement is a special case of the weighted vertex
 * cut of P6(b), with the red position carrying a large capacity, and one max flow solves it exactly
 * with no radius at all. That is the comparison gap G4 sets up, so SC7 runs SBSH at its smallest
 * and its largest radius and records how often the radius binds.
 *
 * <p><b>This is NOT the SBSH of [R3].</b> The paper has now been read against this class; the
 * comparison is written up in {@code config/doi_chieu_sbsh_r3.md}. Four of the five points checked
 * come out different, so no number from this class may be presented as a result for SBSH until
 * either the class is corrected or the manuscript declares it a variant of ours:
 *
 * <ul>
 *   <li><b>The grey label is wrong.</b> Algorithm 2 colours by the side effect on non-sensitive
 *       patterns, not by the cut structure: grey means "carries a pattern sitting exactly at the
 *       threshold, yet the local check sees none of them fall below it", which is uncertainty
 *       rather than irrelevance. Here grey is merely the default for a position carrying no
 *       sensitive node, which puts it outside their colouring altogether, and the grey branch of
 *       Algorithm 5 (re-annotate at the moment of deletion, delete only if still grey) is
 *       missing.</li>
 *   <li><b>The radius is right.</b> Section 4.3 counts r in edges of the matching graph, which is
 *       Nettree arcs, hence levels. Their Example 3 confirms it.</li>
 *   <li><b>The range of r is wrong.</b> The paper fixes r in [1, length of the longest sensitive
 *       sequence]; {@code Sc3MainComparison} passes l_max, which coincides only when the sensitive
 *       set happens to hold a pattern of that length.</li>
 *   <li><b>Replace differs.</b> Theirs is min(C-PA, C-SA), where each side walks outward from one
 *       edge and stops at the FIRST covering set it finds. This one optimises over the whole
 *       radius on both sides at once, so it returns a set at least as small but never reproduces
 *       their "take the near one even if it is large" behaviour -- which is what the radius sweep
 *       of SC7 is supposed to be measuring.</li>
 *   <li><b>Exclude-SBSH is missing.</b> Algorithm 5 step 1 drops the sequence carrying the most
 *       sensitive patterns first; the paper says that choice alone reduces side effects
 *       appreciably. This class walks the sequences in index order. Since H2 is tested on Lost,
 *       which is exactly the side-effect count, this is likely the heaviest of the four.</li>
 * </ul>
 *
 * <p>As with DBSH, [R3] deletes and this implementation masks, because section 11.4 requires the
 * same modification for every method.
 */
public final class Sbsh implements HidingMethod {

    private static final int GREY = 0;
    private static final int BLACK = 1;
    private static final int RED = 2;

    private final int radius;
    private List<Pattern> thresholdPatterns = Collections.emptyList();

    /**
     * @param radius how far the replacement may look, in Nettree levels away from the red node
     */
    public Sbsh(int radius) {
        this.radius = radius;
    }

    /**
     * The non-sensitive patterns whose support sits exactly at the threshold. Annotate needs them:
     * without them every position is black and the colouring does nothing. The caller takes them
     * from F_sigma(D), as it does for B_eps of CUT-HIDE.
     */
    public Sbsh withThresholdPatterns(List<Pattern> patterns) {
        this.thresholdPatterns = List.copyOf(patterns);
        return this;
    }

    @Override
    public String name() {
        return "SBSH-r" + radius;
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

        long[] needed = new long[sensitive.size()];
        if (problem.isDatabaseMode()) {
            for (int p = 0; p < sensitive.size(); p++) {
                needed[p] = Math.max(0, total[p] - problem.threshold() + 1);
            }
        }

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
            long[] removed = sanitiseSequence(problem, s, here, result);
            for (int p = 0; p < sensitive.size(); p++) {
                needed[p] = Math.max(0, needed[p] - removed[p]);
            }
        }
        return result;
    }

    private long[] sanitiseSequence(HideProblem problem, int sequenceIndex, long[] target,
                                    Sanitization result) {
        EventSequence sequence = problem.database().get(sequenceIndex);
        NettreeCutIndex index = new NettreeCutIndex(sequence, problem.gap());
        int sensitiveCount = problem.sensitive().size();
        for (Pattern p : problem.sensitive()) {
            index.add(p, true);
        }
        for (Pattern q : thresholdPatterns) {
            index.add(q, false);
        }

        long[] removed = new long[target.length];
        long[] remaining = target.clone();
        while (!allSatisfied(remaining)) {
            int[] colour = annotate(index, sensitiveCount, sequence.length());
            int candidate = -1;
            for (int p = 0; p < sensitiveCount && candidate < 0; p++) {
                if (remaining[p] == 0) {
                    continue;
                }
                candidate = pick(index, p, colour, BLACK);
                if (candidate < 0) {
                    int red = pick(index, p, colour, RED);
                    if (red >= 0) {
                        List<Integer> replacement = replace(index, p, red, colour);
                        if (!replacement.isEmpty()) {
                            for (int position : replacement) {
                                applyMask(index, position, sequenceIndex, remaining, removed, result);
                            }
                            candidate = Integer.MIN_VALUE;
                        } else {
                            candidate = red;
                        }
                    }
                }
            }
            if (candidate == Integer.MIN_VALUE) {
                continue;
            }
            if (candidate < 0) {
                break;
            }
            applyMask(index, candidate, sequenceIndex, remaining, removed, result);
        }
        return removed;
    }

    private void applyMask(NettreeCutIndex index, int position, int sequenceIndex,
                           long[] remaining, long[] removed, Sanitization result) {
        int[] drop = index.maskPosition(position);
        result.mask(sequenceIndex, position);
        for (int p = 0; p < remaining.length; p++) {
            remaining[p] = Math.max(0, remaining[p] - drop[p]);
            removed[p] += drop[p];
        }
    }

    /** Annotate: one colour per position of the sequence. */
    private int[] annotate(NettreeCutIndex index, int sensitiveCount, int length) {
        int[] colour = new int[length];
        for (int p = 0; p < sensitiveCount; p++) {
            for (int position : index.vitalPositions(p)) {
                colour[position] = BLACK;
            }
        }
        for (int q = sensitiveCount; q < index.patternCount(); q++) {
            for (int position : index.vitalPositions(q)) {
                colour[position] = RED;
            }
        }
        return colour;
    }

    /** The first position of the wanted colour whose masking lowers the support of pattern p. */
    private int pick(NettreeCutIndex index, int patternId, int[] colour, int wanted) {
        for (int position : index.vitalPositions(patternId)) {
            if (!index.isMasked(position) && colour[position] == wanted) {
                return position;
            }
        }
        return -1;
    }

    /**
     * Replace: a set of black positions that cuts every complete path through the red position.
     * Every complete path through a node at level j also passes through some node of each other
     * level, so taking every position that carries a level-k node on a path through the red node
     * cuts them all. The level k is chosen within the radius and at the lowest cost in positions;
     * a level holding a position that is not black is rejected, which is the point of the colouring.
     *
     * @return the positions to mask instead of the red one, empty when the radius holds nothing usable
     */
    private List<Integer> replace(NettreeCutIndex index, int patternId, int redPosition, int[] colour) {
        Nettree nettree = index.nettree(patternId);
        int redLevel = -1;
        for (int v : nettree.nodesAtPosition(redPosition)) {
            redLevel = nettree.level(v);
        }
        if (redLevel < 0) {
            return List.of();
        }
        List<Integer> best = null;
        for (int level = 0; level < nettree.pattern().length(); level++) {
            if (level == redLevel || Math.abs(level - redLevel) > radius) {
                continue;
            }
            List<Integer> positions = new ArrayList<>();
            boolean usable = true;
            for (int v = 0; v < nettree.nodeCount() && usable; v++) {
                if (nettree.level(v) != level || index.flow(patternId).isRemoved(v)) {
                    continue;
                }
                if (index.occurrencesThrough(patternId, nettree.position(v)) == 0) {
                    continue;
                }
                int position = nettree.position(v);
                if (colour[position] != BLACK) {
                    usable = false;
                } else if (!positions.contains(position)) {
                    positions.add(position);
                }
            }
            if (usable && !positions.isEmpty() && (best == null || positions.size() < best.size())) {
                best = positions;
            }
        }
        return best == null ? List.of() : best;
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
