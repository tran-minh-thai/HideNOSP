package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.exp.Sc3LowerBound;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The split exact method against an enumeration that shares none of its structure: the enumeration
 * masks every set of positions of the whole database, in order of size, and checks sup_NO of every
 * pattern on the masked database. It does not split by sequence, does not restrict to Nettree nodes
 * and does not cap drops, so an error in any of the three shows up as a different optimum.
 */
class SequenceSplitExactTest {

    private static int enumerated(List<EventSequence> sequences, List<Pattern> patterns, GapConstraint gap,
                                  int threshold) {
        List<int[]> positions = new ArrayList<>();
        for (int s = 0; s < sequences.size(); s++) {
            for (int i = 0; i < sequences.get(s).length(); i++) {
                positions.add(new int[] {s, i});
            }
        }
        for (int size = 0; size <= positions.size(); size++) {
            if (anyHides(sequences, patterns, gap, threshold, positions, new int[size], 0, 0)) {
                return size;
            }
        }
        throw new AssertionError("masking every position must hide every pattern");
    }

    private static boolean anyHides(List<EventSequence> sequences, List<Pattern> patterns, GapConstraint gap,
                                    int threshold, List<int[]> positions, int[] chosen, int depth, int start) {
        if (depth == chosen.length) {
            Map<Integer, Set<Integer>> masked = new HashMap<>();
            for (int k : chosen) {
                masked.computeIfAbsent(positions.get(k)[0], x -> new TreeSet<>()).add(positions.get(k)[1]);
            }
            List<EventSequence> after = new ArrayList<>();
            for (int s = 0; s < sequences.size(); s++) {
                Set<Integer> here = masked.get(s);
                after.add(here == null ? sequences.get(s) : sequences.get(s).mask(here));
            }
            SequenceDatabase db = new SequenceDatabase(after);
            for (Pattern p : patterns) {
                if (SupportNO.of(p, db, gap) >= threshold) {
                    return false;
                }
            }
            return true;
        }
        for (int k = start; k <= positions.size() - (chosen.length - depth); k++) {
            chosen[depth] = k;
            if (anyHides(sequences, patterns, gap, threshold, positions, chosen, depth + 1, k + 1)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("split exact equals the whole-database enumeration and IlpExact; bound <= optimum <= CUT-HIDE")
    void againstEnumeration() {
        Random random = new Random(20261013L);
        int instances = 0;
        int severalSequences = 0;
        int optimumAboveBound = 0;
        int greedyAboveOptimum = 0;
        int repeatedItems = 0;
        for (int trial = 0; trial < 5000 && instances < 300; trial++) {
            int alphabet = 3 + random.nextInt(2);
            int count = 1 + random.nextInt(3);
            List<EventSequence> list = new ArrayList<>();
            int total = 0;
            for (int s = 0; s < count; s++) {
                int len = 2 + random.nextInt(5);
                int[] events = new int[len];
                for (int i = 0; i < len; i++) {
                    events[i] = random.nextInt(alphabet);
                }
                total += len;
                list.add(new EventSequence(s, events));
            }
            if (total > 12) {
                continue;
            }
            SequenceDatabase db = new SequenceDatabase(list);
            GapConstraint gap = GapConstraint.of(0, 1 + random.nextInt(2));
            List<Pattern> sensitive = new ArrayList<>();
            long smallest = Long.MAX_VALUE;
            int patterns = 2 + random.nextInt(2);
            boolean repeated = false;
            for (int k = 0; k < patterns; k++) {
                int[] items = {random.nextInt(alphabet), random.nextInt(alphabet)};
                Pattern p = new Pattern(items);
                if (!sensitive.contains(p)) {
                    sensitive.add(p);
                    smallest = Math.min(smallest, SupportNO.of(p, db, gap));
                    repeated |= items[0] == items[1];
                }
            }
            if (sensitive.size() < 2 || smallest == 0) {
                continue;
            }
            int threshold = 1 + random.nextInt((int) smallest);
            HideProblem problem = new HideProblem(db, sensitive, gap, HideProblem.Mode.HIDE_DB, threshold, 0.5, 3);
            String where = sensitive + " in " + list + " gap " + gap + " threshold " + threshold;
            Sanitization exact = new SequenceSplitExact().run(problem);
            int opt = enumerated(list, sensitive, gap, threshold);
            assertEquals(opt, exact.maskCount(), "split exact against enumeration, " + where);
            assertEquals(opt, new IlpExact().run(problem).maskCount(), "IlpExact against enumeration, " + where);
            Sc3LowerBound.Bound b = Sc3LowerBound.bound(db, sensitive, gap, HideProblem.Mode.HIDE_DB, threshold);
            assertTrue(b.conflictBound() <= opt, "bound above the optimum, " + where);
            int greedy = new CutHide(CutHideConfig.uniformWeights()).run(problem).maskCount();
            int refined = new CutHide(CutHideConfig.uniformWeights().withRefineSeveral()).run(problem).maskCount();
            assertTrue(refined >= opt && refined <= greedy, "refined outside [optimum, greedy], " + where);
            instances++;
            severalSequences += count > 1 ? 1 : 0;
            optimumAboveBound += opt > b.conflictBound() ? 1 : 0;
            greedyAboveOptimum += greedy > opt ? 1 : 0;
            repeatedItems += repeated ? 1 : 0;
        }
        System.out.printf("Split exact: %d instances, %d with several sequences, %d with a repeated-item pattern;"
                + " optimum above the conflict bound on %d, greedy above the optimum on %d%n",
                instances, severalSequences, repeatedItems, optimumAboveBound, greedyAboveOptimum);
        assertTrue(instances >= 250, "too few instances: " + instances);
        assertTrue(severalSequences >= 50, "the split over sequences must be exercised: " + severalSequences);
        assertTrue(optimumAboveBound >= 1 && greedyAboveOptimum >= 1,
                "the optimum must differ from both the bound and the greedy somewhere, or equality is vacuous");
    }
}
