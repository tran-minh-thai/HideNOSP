// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.exp.Sc3LowerBound;
import hidenosp.hide.HideProblem;
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
 * The bound of several sensitive patterns against the optimum found by enumeration, on random small
 * databases under HIDE-DB. The enumeration is independent of the code under test: it masks every
 * set of positions in order of size and checks sup_NO of every pattern against the threshold. Three
 * statements are checked on every instance: the bound never exceeds the optimum; it equals it when
 * every component of the shared-item graph holds one pattern; and the optimum is the sum of the
 * optima of the components.
 */
class LowerBoundTest {

    /** The least number of masks that brings every pattern below the threshold, by enumeration. */
    private static int optimum(List<EventSequence> sequences, List<Pattern> patterns, GapConstraint gap,
                               int threshold) {
        List<int[]> positions = new ArrayList<>();
        for (int s = 0; s < sequences.size(); s++) {
            for (int i = 0; i < sequences.get(s).length(); i++) {
                positions.add(new int[] {s, i});
            }
        }
        int n = positions.size();
        for (int size = 0; size <= n; size++) {
            if (anyHides(sequences, patterns, gap, threshold, positions, new int[size], 0, 0)) {
                return size;
            }
        }
        throw new AssertionError("masking every position must hide every pattern");
    }

    private static boolean anyHides(List<EventSequence> sequences, List<Pattern> patterns,
                                    GapConstraint gap, int threshold, List<int[]> positions,
                                    int[] chosen, int depth, int start) {
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
    @DisplayName("bound <= optimum; equal with one pattern per component; optimum splits over components")
    void boundAgainstEnumeration() {
        Random random = new Random(20261012L);
        int instances = 0;
        int withSharedItems = 0;
        int boundBelowOptimum = 0;
        int allSingletons = 0;
        int conflictTighter = 0;
        int conflictBelowOptimum = 0;
        for (int trial = 0; trial < 3000 && instances < 300; trial++) {
            int alphabet = 3 + random.nextInt(2);
            int sequences = 1 + random.nextInt(3);
            List<EventSequence> list = new ArrayList<>();
            int total = 0;
            for (int s = 0; s < sequences; s++) {
                int len = 2 + random.nextInt(5);
                int[] events = new int[len];
                for (int i = 0; i < len; i++) {
                    events[i] = random.nextInt(alphabet);
                }
                total += len;
                list.add(new EventSequence(s, events));
            }
            if (total > 11) {
                continue;
            }
            SequenceDatabase db = new SequenceDatabase(list);
            GapConstraint gap = GapConstraint.of(0, 1 + random.nextInt(2));
            List<Pattern> sensitive = new ArrayList<>();
            int count = 2 + random.nextInt(2);
            long smallest = Long.MAX_VALUE;
            for (int k = 0; k < count; k++) {
                int[] items = new int[2];
                items[0] = random.nextInt(alphabet);
                items[1] = random.nextInt(alphabet);
                Pattern p = new Pattern(items);
                if (sensitive.contains(p)) {
                    continue;
                }
                sensitive.add(p);
                smallest = Math.min(smallest, SupportNO.of(p, db, gap));
            }
            if (sensitive.size() < 2 || smallest == 0) {
                continue;
            }
            int threshold = 1 + random.nextInt((int) smallest);
            Sc3LowerBound.Bound b = Sc3LowerBound.bound(db, sensitive, gap, HideProblem.Mode.HIDE_DB, threshold);
            assertTrue(b.optimaExact(), "single-pattern optima exact with g- = 0");
            int opt = optimum(list, sensitive, gap, threshold);
            String where = sensitive + " in " + list + " gap " + gap + " threshold " + threshold;
            instances++;
            assertTrue(b.lowerBound() <= opt, "bound " + b.lowerBound() + " above optimum " + opt + ", " + where);
            // The bound of a component is its largest single-pattern optimum, so the bound is at least
            // the optimum of every pattern alone (read here from the data profile directly).
            for (Pattern p : sensitive) {
                long single = hidenosp.metrics.DataProfile.of(db, p, gap, HideProblem.Mode.HIDE_DB, threshold).minMasks();
                assertTrue(b.lowerBound() >= single, "bound " + b.lowerBound() + " below the optimum " + single
                        + " of " + p + " alone, " + where);
            }
            assertTrue(b.conflictBound() <= opt, "conflict bound " + b.conflictBound() + " above optimum " + opt
                    + ", " + where);
            assertTrue(b.conflictBound() >= b.lowerBound(), "conflict bound below the component bound, " + where);
            if (b.conflictBound() > b.lowerBound()) {
                conflictTighter++;
            }
            if (b.conflictBound() < opt) {
                conflictBelowOptimum++;
            }
            if (b.isOptimum()) {
                allSingletons++;
                assertEquals(opt, b.lowerBound(), "one pattern per component, " + where);
            } else {
                withSharedItems++;
            }
            if (b.lowerBound() < opt) {
                boundBelowOptimum++;
            }
            // The optimum is the sum of the optima of the components.
            Map<Integer, List<Pattern>> byComponent = new HashMap<>();
            for (int k = 0; k < sensitive.size(); k++) {
                byComponent.computeIfAbsent(b.component()[k], x -> new ArrayList<>()).add(sensitive.get(k));
            }
            int sum = 0;
            for (List<Pattern> part : byComponent.values()) {
                sum += optimum(list, part, gap, threshold);
            }
            assertEquals(opt, sum, "optimum splits over components, " + where);
        }
        System.out.printf("Lower bound: %d instances, %d with one pattern per component, %d with shared items,"
                + " bound strictly below the optimum on %d; conflict bound above the component bound on %d,"
                + " still below the optimum on %d%n", instances, allSingletons, withSharedItems,
                boundBelowOptimum, conflictTighter, conflictBelowOptimum);
        assertTrue(conflictTighter >= 1, "the conflict bound must be larger somewhere, or it adds nothing measurable");
        assertTrue(allSingletons >= 30 && withSharedItems >= 30, "both kinds of instance must occur");
        assertTrue(boundBelowOptimum >= 1, "some instance must show the bound below the optimum,"
                + " or the check cannot tell the bound from the optimum");
    }
}
