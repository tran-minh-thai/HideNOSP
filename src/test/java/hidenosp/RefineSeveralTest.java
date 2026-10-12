package hidenosp;

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
import hidenosp.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The several-pattern refinement of CUT-HIDE (proposition P15) against the greedy it refines, on
 * random small databases under HIDE-DB. Every statement is checked from the definitions: each
 * pattern below the threshold by sup_NO on the sanitized database; |R| no larger than the greedy;
 * no single mask removable (minimal under inclusion); Lost no larger than the greedy when only the
 * reverse delete acted; and |R| equal to the bound when every component holds one pattern, where
 * the bound is the optimum.
 */
class RefineSeveralTest {

    private static boolean hidesAll(SequenceDatabase db, List<Pattern> patterns, GapConstraint gap, int threshold,
                                    Map<Integer, Set<Integer>> masks) {
        List<EventSequence> after = new ArrayList<>();
        for (int s = 0; s < db.size(); s++) {
            Set<Integer> here = masks.get(s);
            after.add(here == null || here.isEmpty() ? db.get(s) : db.get(s).mask(here));
        }
        SequenceDatabase d = new SequenceDatabase(after);
        for (Pattern p : patterns) {
            if (SupportNO.of(p, d, gap) >= threshold) {
                return false;
            }
        }
        return true;
    }

    @Test
    @DisplayName("refinement hides every pattern, never uses more masks, is minimal, and is optimal on one-pattern components")
    void refinement() {
        Random random = new Random(20261013L);
        int instances = 0;
        int pruned = 0;
        int replaced = 0;
        int fewer = 0;
        int singletonInstances = 0;
        for (int trial = 0; trial < 4000 && instances < 400; trial++) {
            int alphabet = 3 + random.nextInt(3);
            int count = 2 + random.nextInt(4);
            List<EventSequence> list = new ArrayList<>();
            for (int s = 0; s < count; s++) {
                int len = 4 + random.nextInt(8);
                int[] events = new int[len];
                for (int i = 0; i < len; i++) {
                    events[i] = random.nextInt(alphabet);
                }
                list.add(new EventSequence(s, events));
            }
            SequenceDatabase db = new SequenceDatabase(list);
            GapConstraint gap = GapConstraint.of(0, 1 + random.nextInt(3));
            List<Pattern> sensitive = new ArrayList<>();
            long smallest = Long.MAX_VALUE;
            int patterns = 2 + random.nextInt(3);
            for (int k = 0; k < patterns; k++) {
                int m = 2 + random.nextInt(2);
                int[] items = new int[m];
                for (int j = 0; j < m; j++) {
                    items[j] = random.nextInt(alphabet);
                }
                Pattern p = new Pattern(items);
                if (!sensitive.contains(p)) {
                    sensitive.add(p);
                    smallest = Math.min(smallest, SupportNO.of(p, db, gap));
                }
            }
            if (sensitive.size() < 2 || smallest == 0) {
                continue;
            }
            int threshold = 1 + random.nextInt((int) smallest);
            HideProblem problem = new HideProblem(db, sensitive, gap, HideProblem.Mode.HIDE_DB, threshold, 0.5, 3);
            Sanitization greedy = new CutHide(CutHideConfig.uniformWeights()).run(problem);
            Sanitization refined = new CutHide(CutHideConfig.uniformWeights().withRefineSeveral()).run(problem);
            instances++;
            String where = sensitive + " in " + list + " gap " + gap + " threshold " + threshold;
            assertTrue(hidesAll(db, sensitive, gap, threshold, refined.masked()), "hidden, " + where);
            assertTrue(refined.maskCount() <= greedy.maskCount(), "no more masks than the greedy, " + where);
            // Minimal under inclusion: no single mask can go.
            for (Map.Entry<Integer, Set<Integer>> e : refined.masked().entrySet()) {
                for (int position : e.getValue()) {
                    Map<Integer, Set<Integer>> fewerMasks = new java.util.HashMap<>();
                    for (Map.Entry<Integer, Set<Integer>> f : refined.masked().entrySet()) {
                        fewerMasks.put(f.getKey(), new TreeSet<>(f.getValue()));
                    }
                    fewerMasks.get(e.getKey()).remove(position);
                    assertTrue(!hidesAll(db, sensitive, gap, threshold, fewerMasks),
                            "mask " + e.getKey() + ":" + position + " is removable, " + where);
                }
            }
            if (refined.removedMasks() > 0) {
                pruned++;
            }
            if (refined.replacedComponents() > 0) {
                replaced++;
            }
            if (refined.maskCount() < greedy.maskCount()) {
                fewer++;
            }
            if (refined.replacedComponents() == 0) {
                Metrics.Context context = Metrics.Context.of(db, gap, 3, threshold);
                int lostGreedy = context.evaluateMasking(problem, greedy).lost();
                int lostRefined = context.evaluateMasking(problem, refined).lost();
                assertTrue(lostRefined <= lostGreedy, "the reverse delete raised Lost from " + lostGreedy
                        + " to " + lostRefined + ", " + where);
            }
            Sc3LowerBound.Bound b = Sc3LowerBound.bound(db, sensitive, gap, HideProblem.Mode.HIDE_DB, threshold);
            assertTrue(refined.maskCount() >= b.conflictBound(), "below the bound, " + where);
            if (b.isOptimum()) {
                singletonInstances++;
                assertEquals(b.lowerBound(), refined.maskCount(), "one pattern per component is optimal, " + where);
            }
        }
        System.out.printf("Refinement: %d instances; reverse delete removed masks on %d, a component replaced on %d,"
                + " fewer masks than the greedy on %d; %d with one pattern per component%n",
                instances, pruned, replaced, fewer, singletonInstances);
        assertTrue(pruned >= 20, "the reverse delete must act on enough instances to be checked: " + pruned);
        assertTrue(singletonInstances >= 20, "one-pattern components must occur: " + singletonInstances);
    }
}
