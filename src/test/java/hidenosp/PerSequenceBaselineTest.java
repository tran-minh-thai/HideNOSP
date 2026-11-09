// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.baseline.Hha;
import hidenosp.baseline.IlpExact;
import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;
import hidenosp.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The HIDE-SEQ parts of the baselines: the exact enumeration with its per-sequence pruning against a
 * search over every subset, and the occurrence-count rule of Abul, Bonchi and Giannotti (TKDE 2010).
 */
class PerSequenceBaselineTest {

    private static SequenceDatabase randomDatabase(Random random, int sequences, int length, int alphabet) {
        List<EventSequence> out = new ArrayList<>();
        for (int s = 0; s < sequences; s++) {
            int[] events = new int[length];
            for (int i = 0; i < length; i++) {
                events[i] = random.nextInt(alphabet);
            }
            out.add(new EventSequence(s, events));
        }
        return new SequenceDatabase(out);
    }

    @Test
    @DisplayName("HIDE-SEQ exact enumeration: |R|*, the least and the greatest Lost equal every subset")
    void exactEnumerationAgainstSubsets() {
        Random random = new Random(41L);
        int compared = 0;
        for (int round = 0; round < 150; round++) {
            GapConstraint gap = new GapConstraint(random.nextInt(2), 1 + random.nextInt(3));
            SequenceDatabase db = randomDatabase(random, 3, 6, 3);
            Pattern p = random.nextBoolean() ? new Pattern(0, 1) : new Pattern(0, 0);
            int r = 1 + random.nextInt(2);
            HideProblem problem = new HideProblem(db, List.of(p), gap, HideProblem.Mode.HIDE_SEQ, r, 0.5, 3, 2);
            Metrics.Context context = Metrics.Context.of(db, gap, 3, 2);
            Map<Pattern, Long> frequent = context.frequent();
            Metrics.Context.Session session = context.session(problem);
            // Every subset of the carrying positions, smallest size first.
            List<int[]> cells = new ArrayList<>();
            for (int s = 0; s < db.size(); s++) {
                for (int i = 0; i < db.get(s).length(); i++) {
                    if (p.contains(db.get(s).at(i))) {
                        cells.add(new int[] {s, i});
                    }
                }
            }
            int bestSize = -1;
            int best = Integer.MAX_VALUE;
            int worst = Integer.MIN_VALUE;
            int count = 0;
            for (int size = 0; size <= cells.size() && bestSize < 0; size++) {
                for (int mask = 0; mask < (1 << cells.size()); mask++) {
                    if (Integer.bitCount(mask) != size) {
                        continue;
                    }
                    Sanitization x = new Sanitization(db);
                    for (int c = 0; c < cells.size(); c++) {
                        if ((mask & (1 << c)) != 0) {
                            x.mask(cells.get(c)[0], cells.get(c)[1]);
                        }
                    }
                    if (Metrics.hidingFailure(problem, db.mask(x.masked())) > 0) {
                        continue;
                    }
                    bestSize = size;
                    int lost = session.evaluate(x).lost();
                    best = Math.min(best, lost);
                    worst = Math.max(worst, lost);
                    count++;
                }
            }
            if (bestSize <= 0) {
                continue;
            }
            IlpExact.Exact exact = IlpExact.withSideEffects(problem, frequent).solveExactly(problem);
            assertEquals(bestSize, exact.minMasks(), "|R|*");
            assertEquals(count, exact.optimalSets(), "number of minimum hiding sets");
            assertEquals(best, exact.bestLost(), "least Lost");
            assertEquals(worst, exact.worstLost(), "greatest Lost");
            compared++;
        }
        assertTrue(compared >= 60, "too few instances need a mask: " + compared);
    }

    @Test
    @DisplayName("HHA-emb: every sequence ends with at most r - 1 matchings, and one under r is never touched")
    void occurrenceCountRule() {
        Random random = new Random(9L);
        int sanitised = 0;
        int untouchedBelowR = 0;
        int strictlyMoreThanSupNo = 0;
        for (int round = 0; round < 200; round++) {
            GapConstraint gap = new GapConstraint(random.nextInt(2), 1 + random.nextInt(3));
            SequenceDatabase db = randomDatabase(random, 5, 12, 3);
            List<Pattern> sensitive = random.nextBoolean() ? List.of(new Pattern(0, 1))
                    : List.of(new Pattern(0, 1), new Pattern(1, 2));
            int r = 1 + random.nextInt(3);
            HideProblem problem = new HideProblem(db, sensitive, gap, HideProblem.Mode.HIDE_SEQ, r, 0.5, 3, 2);
            Sanitization emb = Hha.perSequenceEmbeddings().run(problem);
            Sanitization nonOverlapping = new Hha().run(problem);
            SequenceDatabase after = db.mask(emb.masked());
            for (int s = 0; s < db.size(); s++) {
                long before = matchings(db.get(s), sensitive, gap);
                long now = matchings(after.get(s), sensitive, gap);
                assertTrue(now <= r - 1, "more than r - 1 matchings left in sequence " + s);
                Set<Integer> masks = emb.maskedIn(s);
                if (before <= r - 1) {
                    assertTrue(masks.isEmpty(), "a sequence with at most r - 1 matchings was masked");
                    untouchedBelowR++;
                } else {
                    sanitised++;
                }
                for (Pattern p : sensitive) {
                    assertTrue(SupportNO.of(p, after.get(s), gap) < r);
                }
            }
            if (emb.maskCount() > nonOverlapping.maskCount()) {
                strictlyMoreThanSupNo++;
            }
            assertTrue(emb.maskCount() >= nonOverlapping.maskCount() || sensitive.size() > 1,
                    "with one pattern the embedding rule stops later, never earlier, than the sup_NO rule");
        }
        assertTrue(sanitised >= 100 && untouchedBelowR >= 100, sanitised + " / " + untouchedBelowR);
        // The two stopping rules differ on real instances: the embedding count is the larger one.
        assertTrue(strictlyMoreThanSupNo >= 20, "the embedding rule never cost more: " + strictlyMoreThanSupNo);
        assertThrows(IllegalArgumentException.class, () -> Hha.perSequenceEmbeddings().run(
                new HideProblem(randomDatabase(random, 2, 6, 2), List.of(new Pattern(0, 1)),
                        new GapConstraint(0, 2), HideProblem.Mode.HIDE_DB, 2, 0.5, 3)));
    }

    /** |M_T^{P^h}|: every embedding of every sensitive pattern, by the counting program of the papers. */
    private static long matchings(EventSequence sequence, List<Pattern> sensitive, GapConstraint gap) {
        long total = 0;
        for (Pattern p : sensitive) {
            total += Hha.countMatches(p, sequence, gap, new boolean[sequence.length()]);
        }
        return total;
    }

    @Test
    @DisplayName("the index counts the same matchings as the counting program of the papers")
    void indexCountsAgree() {
        Random random = new Random(13L);
        for (int round = 0; round < 200; round++) {
            GapConstraint gap = new GapConstraint(random.nextInt(2), 1 + random.nextInt(3));
            EventSequence s = randomDatabase(random, 1, 12, 3).get(0);
            Pattern p = new Pattern(random.nextInt(3), random.nextInt(3), random.nextInt(3));
            NettreeCutIndex index = new NettreeCutIndex(s, gap);
            int id = index.add(p, true);
            assertEquals(Hha.countMatches(p, s, gap, new boolean[s.length()]), index.totalOccurrences(id));
        }
    }
}
