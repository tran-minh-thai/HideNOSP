// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CUT-HIDE with one sensitive pattern and g- = 0 returns the least number of masks, repeated items
 * included, for HIDE-DB and HIDE-SEQ: compared with a search over every set of positions of the
 * whole database.
 */
class CutHideExactTest {

    private static boolean hidden(Pattern p, SequenceDatabase db, GapConstraint gap,
                                  HideProblem.Mode mode, int threshold) {
        if (mode == HideProblem.Mode.HIDE_DB) {
            return SupportNO.of(p, db, gap) < threshold;
        }
        for (EventSequence s : db.sequences()) {
            if (SupportNO.of(p, s, gap) >= threshold) {
                return false;
            }
        }
        return true;
    }

    /** The least number of masked positions over the whole database, by trying every subset. */
    private static int leastMasks(Pattern p, SequenceDatabase db, GapConstraint gap,
                                  HideProblem.Mode mode, int threshold) {
        List<int[]> slots = new ArrayList<>();
        for (int s = 0; s < db.size(); s++) {
            for (int i = 0; i < db.get(s).length(); i++) {
                slots.add(new int[] {s, i});
            }
        }
        int best = slots.size();
        for (int mask = 0; mask < (1 << slots.size()); mask++) {
            int size = Integer.bitCount(mask);
            if (size >= best) {
                continue;
            }
            Map<Integer, List<Integer>> chosen = new HashMap<>();
            for (int k = 0; k < slots.size(); k++) {
                if ((mask & (1 << k)) != 0) {
                    chosen.computeIfAbsent(slots.get(k)[0], x -> new ArrayList<>()).add(slots.get(k)[1]);
                }
            }
            if (hidden(p, db.mask(chosen), gap, mode, threshold)) {
                best = size;
            }
        }
        return best;
    }

    @Test
    @DisplayName("one pattern, g- = 0: CUT-HIDE masks as few positions as any set can")
    void leastMasksForOnePattern() {
        Random random = new Random(2027);
        int instances = 0;
        int repeatedBelowCut = 0;
        int[] gaps = {0, 1, 2, GapConstraint.UNBOUNDED_MAX};
        for (int trial = 0; instances < 400; trial++) {
            int alphabet = 1 + random.nextInt(3);
            int sequences = 1 + random.nextInt(3);
            List<EventSequence> list = new ArrayList<>();
            int positions = 0;
            for (int s = 0; s < sequences; s++) {
                int n = random.nextInt(6);
                int[] events = new int[n];
                for (int i = 0; i < n; i++) {
                    events[i] = random.nextInt(alphabet);
                }
                positions += n;
                list.add(new EventSequence(s, events));
            }
            if (positions > 13) {
                continue;
            }
            int m = 1 + random.nextInt(3);
            int[] items = new int[m];
            for (int j = 0; j < m; j++) {
                items[j] = random.nextInt(alphabet);
            }
            Pattern p = new Pattern(items);
            SequenceDatabase db = new SequenceDatabase(list);
            GapConstraint gap = GapConstraint.of(0, gaps[random.nextInt(gaps.length)]);
            long support = SupportNO.of(p, db, gap);
            if (support == 0) {
                continue;
            }
            HideProblem.Mode mode = random.nextBoolean() ? HideProblem.Mode.HIDE_DB
                    : HideProblem.Mode.HIDE_SEQ;
            int threshold;
            if (mode == HideProblem.Mode.HIDE_DB) {
                threshold = 1 + random.nextInt((int) support);
            } else {
                int largest = 0;
                for (EventSequence s : list) {
                    largest = Math.max(largest, SupportNO.of(p, s, gap));
                }
                threshold = 1 + random.nextInt(largest);
            }
            HideProblem problem = new HideProblem(db, List.of(p), gap, mode, threshold, 0.5, 4);
            Sanitization result = new CutHide(CutHideConfig.uniformWeights()).run(problem);
            String where = p + " in " + list + " gap " + gap + " " + mode + " threshold " + threshold;
            assertTrue(hidden(p, result.sanitized(), gap, mode, threshold), "hidden, " + where);
            int least = leastMasks(p, db, gap, mode, threshold);
            assertEquals(least, result.maskCount(), "least masks, " + where);
            // The bound SC2 prints is the same optimum, and it says it is exact.
            hidenosp.metrics.DataProfile profile =
                    hidenosp.metrics.DataProfile.of(db, p, gap, mode, threshold);
            assertEquals(least, profile.minMasks(), "|R| star of the data profile, " + where);
            assertTrue(profile.isExact(), "exact when g- = 0, " + where);
            long cut = 0;
            if (mode == HideProblem.Mode.HIDE_DB) {
                cut = Math.max(0, support - threshold + 1);
            } else {
                for (EventSequence s : list) {
                    cut += Math.max(0, SupportNO.of(p, s, gap) - threshold + 1);
                }
            }
            if (p.mu() >= 2 && least < cut) {
                repeatedBelowCut++;
            }
            if (p.mu() == 1) {
                assertEquals(cut, result.maskCount(), "P1 and its corollary, " + where);
            }
            instances++;
        }
        System.out.printf("CUT-HIDE against every subset: %d instances, %d with repeated items "
                + "needing fewer masks than the cut%n", instances, repeatedBelowCut);
        assertTrue(repeatedBelowCut >= 15, "instances below the cut: " + repeatedBelowCut);
    }

    @Test
    @DisplayName("the exact allocation breaks ties by weight and uses a mask that lowers by two")
    void exactAllocation() {
        // Two sequences with F = 1, one needed: same masks, so the lighter one is chosen.
        int[] tie = hidenosp.hide.Allocation.solveExact(new int[][] {{1, 0}, {1, 0}},
                new long[][] {{5, 0}, {2, 0}}, 1);
        assertEquals(1, tie[0]);
        assertEquals(0, tie[1]);
        // The same with the lighter sequence first, so the tie is met in the other order.
        int[] tieSwapped = hidenosp.hide.Allocation.solveExact(new int[][] {{1, 0}, {1, 0}},
                new long[][] {{2, 0}, {5, 0}}, 1);
        assertEquals(0, tieSwapped[0]);
        assertEquals(1, tieSwapped[1]);
        // A: one mask takes it from 2 to 0 (repeated items); B: one mask from 1 to 0. Need 2:
        // one mask in A beats one in each.
        int[] over = hidenosp.hide.Allocation.solveExact(new int[][] {{1, 1, 0}, {1, 0}},
                new long[][] {{0, 0, 0}, {0, 0}}, 2);
        assertEquals(0, over[0]);
        assertEquals(1, over[1]);
        // Need 1 only: the cheaper single reduction, B at weight 1 against A at weight 3.
        int[] need1 = hidenosp.hide.Allocation.solveExact(new int[][] {{1, 1, 0}, {1, 0}},
                new long[][] {{3, 3, 0}, {1, 0}}, 1);
        assertEquals(2, need1[0]);
        assertEquals(0, need1[1]);
    }

    @Test
    @DisplayName("the lambda-sweep variant stays feasible on repeated items")
    void lambdaSweepVariantIsFeasible() {
        Random random = new Random(99);
        int checked = 0;
        for (int trial = 0; checked < 150; trial++) {
            int n = 3 + random.nextInt(8);
            int[] events = new int[n];
            for (int i = 0; i < n; i++) {
                events[i] = random.nextInt(2);
            }
            Pattern p = new Pattern(0, 1, 0);
            SequenceDatabase db = new SequenceDatabase(List.of(new EventSequence(0, events)));
            GapConstraint gap = GapConstraint.of(0, 2);
            long support = SupportNO.of(p, db, gap);
            if (support == 0) {
                continue;
            }
            HideProblem problem = new HideProblem(db, List.of(p), gap, HideProblem.Mode.HIDE_DB, 1, 0.5, 4);
            CutHideConfig sweep = CutHideConfig.uniformWeights().withCurves(CutHideConfig.Curves.LAMBDA_SWEEP);
            Sanitization result = new CutHide(sweep).run(problem);
            assertTrue(SupportNO.of(p, result.sanitized(), gap) < 1);
            int exact = new CutHide(CutHideConfig.uniformWeights()).run(problem).maskCount();
            assertTrue(exact <= result.maskCount(), "the exact curves never mask more");
            checked++;
        }
    }
}
