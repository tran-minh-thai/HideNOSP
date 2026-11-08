// SPDX-License-Identifier: MIT
package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stop-at-threshold variant of HHA (preregistration section 19.1) against its definition.
 *
 * <p>The reference below is written from the definition only: the matching set and every delta are
 * counted with the dynamic program of Theorem 2 of the papers ({@link Hha#countMatches}), not with
 * the forward and backward counts of the index that the class uses, so the two share no counting
 * code. They must give the same masks on every instance.
 */
class HhaStopTest {

    private static final int SEED = 2026;

    @Test
    @DisplayName("Stops at the threshold where HHA sanitises a chosen sequence completely")
    void stopsWhereHhaGoesOn() {
        // S1 = ab ab ab (three non-overlapping occurrences), S2 = ab; sigma = 2, so the support 4 has
        // to fall by 3. HHA keeps nothing within the budget sigma - 1 = 1 (S1 alone costs 3) and
        // sanitises both completely: 1 + 3 masks. The variant takes S2 first (fewer matchings), then
        // stops in S1 after two masks: 3 masks, the least possible since one mask breaks at most one
        // occurrence of a pattern with distinct items.
        SequenceDatabase database = new SequenceDatabase(List.of(
                new EventSequence(0, PaperFigure1.events("ababab")),
                new EventSequence(1, PaperFigure1.events("ab"))));
        Pattern ab = PaperFigure1.pattern("ab");
        HideProblem problem = new HideProblem(database, List.of(ab), GapConstraint.of(0, 0),
                HideProblem.Mode.HIDE_DB, 2, 0.5, 2);
        assertEquals(4, new Hha().run(problem).maskCount(), "HHA");
        Sanitization stop = Hha.stopAtThreshold().run(problem);
        assertEquals(3, stop.maskCount(), "HHA-stop");
        assertEquals(1, stop.maskedIn(1).size(), "the sequence with fewer matchings goes first");
        assertEquals(1, SupportNO.of(ab, stop.sanitized(), GapConstraint.of(0, 0)), "support below sigma");
    }

    @Test
    @DisplayName("Equals an independent implementation of the definition on random instances")
    void matchesTheDefinition() {
        Random random = new Random(SEED);
        int compared = 0;
        int fewerThanHha = 0;
        int moreThanHha = 0;
        for (int trial = 0; trial < 400; trial++) {
            HideProblem problem = randomProblem(random, HideProblem.Mode.HIDE_DB);
            if (problem == null) {
                continue;
            }
            Sanitization result = Hha.stopAtThreshold().run(problem);
            Map<Integer, Set<Integer>> expected = reference(problem);
            assertEquals(expected, normalise(result.masked()), "trial " + trial + ": " + problem);
            for (Pattern p : problem.sensitive()) {
                assertTrue(SupportNO.of(p, result.sanitized(), problem.gap()) < problem.threshold(),
                        "trial " + trial + " hides " + p);
            }
            int hha = new Hha().run(problem).maskCount();
            fewerThanHha += result.maskCount() < hha ? 1 : 0;
            moreThanHha += result.maskCount() > hha ? 1 : 0;
            compared++;
        }
        System.out.printf("HHA-stop against its definition: %d instances compared; "
                + "fewer masks than HHA on %d, more on %d%n", compared, fewerThanHha, moreThanHha);
        assertTrue(compared >= 300, "too few instances could be compared: " + compared);
        // Without this the comparison could pass on instances where the variant and HHA coincide.
        assertTrue(fewerThanHha > 0, "the variant never differs from HHA on these instances");
    }

    @Test
    @DisplayName("Under HIDE-SEQ it is HHA")
    void equalsHhaUnderPerSequenceLimit() {
        Random random = new Random(SEED + 1);
        int compared = 0;
        for (int trial = 0; trial < 300; trial++) {
            HideProblem problem = randomProblem(random, HideProblem.Mode.HIDE_SEQ);
            if (problem == null) {
                continue;
            }
            assertEquals(new Hha().run(problem).masked(), Hha.stopAtThreshold().run(problem).masked(),
                    "trial " + trial);
            compared++;
        }
        assertTrue(compared >= 150, "too few instances could be compared: " + compared);
    }

    /** The definition of preregistration section 19.1, with every count taken by the dynamic program of the papers. */
    private static Map<Integer, Set<Integer>> reference(HideProblem problem) {
        SequenceDatabase database = problem.database();
        List<Pattern> sensitive = problem.sensitive();
        GapConstraint gap = problem.gap();
        List<Integer> carrying = new ArrayList<>();
        long[] matching = new long[database.size()];
        for (int s = 0; s < database.size(); s++) {
            boolean carries = false;
            for (Pattern p : sensitive) {
                carries |= SupportNO.of(p, database.get(s), gap) > 0;
                matching[s] += Hha.countMatches(p, database.get(s), gap, new boolean[database.get(s).length()]);
            }
            if (carries) {
                carrying.add(s);
            }
        }
        carrying.sort(Comparator.<Integer>comparingLong(s -> matching[s]).thenComparing(Comparator.naturalOrder()));
        long[] needed = new long[sensitive.size()];
        for (int p = 0; p < sensitive.size(); p++) {
            needed[p] = Math.max(0, SupportNO.of(sensitive.get(p), database, gap) - problem.threshold() + 1);
        }
        Map<Integer, Set<Integer>> out = new TreeMap<>();
        for (int s : carrying) {
            if (allZero(needed)) {
                break;
            }
            EventSequence sequence = database.get(s);
            boolean[] masked = new boolean[sequence.length()];
            while (true) {
                boolean[] active = new boolean[sensitive.size()];
                boolean any = false;
                for (int p = 0; p < sensitive.size(); p++) {
                    active[p] = needed[p] > 0 && SupportNO.of(sensitive.get(p), masked(sequence, masked), gap) > 0;
                    any |= active[p];
                }
                if (!any) {
                    break;
                }
                long best = 0;
                int target = -1;
                for (int i = 0; i < sequence.length(); i++) {
                    if (masked[i]) {
                        continue;
                    }
                    long through = 0;
                    for (int p = 0; p < sensitive.size(); p++) {
                        if (!active[p]) {
                            continue;
                        }
                        long before = Hha.countMatches(sensitive.get(p), sequence, gap, masked);
                        masked[i] = true;
                        long after = Hha.countMatches(sensitive.get(p), sequence, gap, masked);
                        masked[i] = false;
                        through += before - after;
                    }
                    if (through > best) {
                        best = through;
                        target = i;
                    }
                }
                long[] before = new long[sensitive.size()];
                for (int p = 0; p < sensitive.size(); p++) {
                    before[p] = SupportNO.of(sensitive.get(p), masked(sequence, masked), gap);
                }
                masked[target] = true;
                out.computeIfAbsent(s, k -> new TreeSet<>()).add(target);
                for (int p = 0; p < sensitive.size(); p++) {
                    needed[p] -= before[p] - SupportNO.of(sensitive.get(p), masked(sequence, masked), gap);
                }
            }
        }
        return out;
    }

    private static EventSequence masked(EventSequence sequence, boolean[] masked) {
        int[] events = new int[sequence.length()];
        for (int i = 0; i < events.length; i++) {
            events[i] = masked[i] ? EventSequence.MASK : sequence.at(i);
        }
        return new EventSequence(sequence.id(), events);
    }

    private static boolean allZero(long[] needed) {
        for (long n : needed) {
            if (n > 0) {
                return false;
            }
        }
        return true;
    }

    private static Map<Integer, Set<Integer>> normalise(Map<Integer, Set<Integer>> masked) {
        Map<Integer, Set<Integer>> out = new TreeMap<>();
        masked.forEach((s, positions) -> {
            if (!positions.isEmpty()) {
                out.put(s, new TreeSet<>(positions));
            }
        });
        return out;
    }

    /** A small random database with one to three sensitive patterns that need a drop, or null. */
    private static HideProblem randomProblem(Random random, HideProblem.Mode mode) {
        int alphabet = 3 + random.nextInt(2);
        List<EventSequence> sequences = new ArrayList<>();
        int n = 4 + random.nextInt(6);
        for (int s = 0; s < n; s++) {
            int[] events = new int[3 + random.nextInt(10)];
            for (int i = 0; i < events.length; i++) {
                events[i] = random.nextInt(alphabet);
            }
            sequences.add(new EventSequence(s, events));
        }
        SequenceDatabase database = new SequenceDatabase(sequences);
        GapConstraint[] gaps = {GapConstraint.of(0, 0), GapConstraint.of(0, 1), GapConstraint.of(0, 2),
                GapConstraint.unbounded(0)};
        GapConstraint gap = gaps[random.nextInt(gaps.length)];
        List<Pattern> sensitive = new ArrayList<>();
        int count = 1 + random.nextInt(3);
        for (int k = 0; k < count; k++) {
            int[] items = new int[2 + random.nextInt(2)];
            for (int i = 0; i < items.length; i++) {
                items[i] = random.nextInt(alphabet);
            }
            Pattern p = new Pattern(items);
            if (!sensitive.contains(p)) {
                sensitive.add(p);
            }
        }
        long least = Long.MAX_VALUE;
        long most = 0;
        for (Pattern p : sensitive) {
            long support = mode == HideProblem.Mode.HIDE_DB ? SupportNO.of(p, database, gap) : maxPerSequence(p, database, gap);
            least = Math.min(least, support);
            most = Math.max(most, support);
        }
        if (most < 2) {
            return null;
        }
        int threshold = (int) Math.max(1, Math.min(most, Math.max(2, least / 2 + 1)));
        if (mode == HideProblem.Mode.HIDE_DB && threshold < 2) {
            return null;
        }
        return new HideProblem(database, sensitive, gap, mode, threshold, 0.5, 3);
    }

    private static long maxPerSequence(Pattern p, SequenceDatabase database, GapConstraint gap) {
        long most = 0;
        for (int s = 0; s < database.size(); s++) {
            most = Math.max(most, SupportNO.of(p, database.get(s), gap));
        }
        return most;
    }
}
