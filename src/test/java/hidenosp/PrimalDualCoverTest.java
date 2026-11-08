// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.baseline.IlpExact;
import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.PrimalDualCover;
import hidenosp.hide.Sanitization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three things the l_max method has to satisfy: it hides everything, it never spends more than
 * l_max times what is possible, and the bound it reports is genuinely a lower bound.
 *
 * <p>The last one matters most. A method that returns a "certificate" nobody checks is worse than
 * one that returns none, so the dual is compared against an optimum computed exactly.
 */
class PrimalDualCoverTest {

    private static final int A = 0;
    private static final int B = 1;
    private static final int C = 2;

    private static SequenceDatabase database(long seed, int count, int length, int alphabet) {
        Random random = new Random(seed);
        List<EventSequence> sequences = new ArrayList<>();
        for (int s = 0; s < count; s++) {
            int[] events = new int[length];
            for (int i = 0; i < events.length; i++) {
                events[i] = random.nextInt(alphabet);
            }
            sequences.add(new EventSequence(s, events));
        }
        return new SequenceDatabase(sequences);
    }

    private static HideProblem fullHiding(SequenceDatabase database, List<Pattern> sensitive) {
        return new HideProblem(database, sensitive, new GapConstraint(0, 2),
                HideProblem.Mode.HIDE_DB, 1, 0.5, 4);
    }

    @Test
    @DisplayName("complete hiding: no sensitive pattern keeps any occurrence")
    void hidesEverything() {
        for (long seed : new long[] {5, 17, 41}) {
            SequenceDatabase database = database(seed, 10, 12, 3);
            HideProblem problem = fullHiding(database,
                    List.of(new Pattern(A, B), new Pattern(B, C), new Pattern(A, B, C)));
            Sanitization result = new PrimalDualCover().run(problem);
            assertTrue(result.maskCount() > 0, "empty instance: the test would check nothing");
            SequenceDatabase after = result.sanitized();
            for (Pattern p : problem.sensitive()) {
                assertEquals(0, SupportNO.of(p, after, problem.gap()),
                        "an occurrence of " + p + " is left at seed " + seed);
            }
        }
    }

    @Test
    @DisplayName("the lower bound really is a lower bound, and the ratio stays within l_max")
    void boundIsSoundAndTheFactorHolds() {
        for (long seed : new long[] {5, 17, 41}) {
            SequenceDatabase database = database(seed, 10, 12, 3);
            HideProblem problem = fullHiding(database,
                    List.of(new Pattern(A, B), new Pattern(B, C), new Pattern(A, B, C)));
            PrimalDualCover method = new PrimalDualCover();
            Sanitization result = method.run(problem);
            PrimalDualCover.Bound bound = method.lastBound();

            assertTrue(bound.cost() > 0, "empty instance: the test would check nothing");
            assertTrue(bound.maxConstraintSize() >= 2,
                    "with one variable per constraint the factor would say nothing");
            assertEquals(result.maskCount(), bound.cost(), "the reported cost must equal |R|");
            assertTrue(bound.lowerBound() <= bound.cost(),
                    "lower bound " + bound.lowerBound() + " exceeds the cost " + bound.cost());
            long lmax = problem.maxPatternLength();
            assertTrue(bound.cost() <= lmax * bound.lowerBound(),
                    "cost " + bound.cost() + " exceeds " + lmax + " times the lower bound "
                            + bound.lowerBound());
            assertTrue(bound.maxConstraintSize() <= lmax,
                    "a constraint has " + bound.maxConstraintSize() + " variables, over l_max");
        }
    }

    @Test
    @DisplayName("one distinct-item pattern: the bound stays below the optimum, sup_NO by P1c")
    void boundNeverExceedsTheKnownOptimum() {
        for (long seed : new long[] {5, 17, 41}) {
            SequenceDatabase database = database(seed, 10, 12, 3);
            HideProblem problem = fullHiding(database, List.of(new Pattern(A, B)));
            // For one distinct-item pattern and complete hiding, corollary P1c gives the optimum:
            // exactly sup_NO.
            long optimum = PrimalDualCover.optimumForOneDistinctPattern(problem);
            assertTrue(optimum > 0, "empty instance: the test would check nothing");
            PrimalDualCover method = new PrimalDualCover();
            Sanitization result = method.run(problem);
            PrimalDualCover.Bound bound = method.lastBound();

            assertTrue(bound.lowerBound() <= optimum,
                    "lower bound " + bound.lowerBound() + " exceeds the optimum " + optimum);
            assertTrue(result.maskCount() >= optimum,
                    "used " + result.maskCount() + " masks, fewer than the optimum " + optimum);
            assertTrue(result.maskCount() <= 2 * optimum,
                    "the pattern has length 2, so the factor is 2, but it used "
                            + result.maskCount() + " against " + optimum);
        }
    }

    @Test
    @DisplayName("agrees with the exact solver on a small instance")
    void agreesWithTheExactSolverWithinTheFactor() {
        SequenceDatabase database = database(23, 3, 8, 3);
        HideProblem problem = fullHiding(database, List.of(new Pattern(A, B), new Pattern(B, C)));
        long exact = new IlpExact().run(problem).maskCount();
        assertTrue(exact > 0, "empty instance: the test would check nothing");
        PrimalDualCover method = new PrimalDualCover();
        long approximate = method.run(problem).maskCount();
        PrimalDualCover.Bound bound = method.lastBound();

        assertTrue(bound.lowerBound() <= exact,
                "lower bound " + bound.lowerBound() + " exceeds the exact optimum " + exact);
        assertTrue(approximate >= exact, "cannot use fewer masks than the optimum");
        assertTrue(approximate <= 2L * exact,
                "the patterns have length 2, so the factor is 2: " + approximate + " against "
                        + exact);
    }

    @Test
    @DisplayName("minimal: dropping any position exposes an occurrence again")
    void theMaskSetIsMinimal() {
        for (long seed : new long[] {5, 17, 41}) {
            SequenceDatabase database = database(seed, 10, 12, 3);
            HideProblem problem = fullHiding(database,
                    List.of(new Pattern(A, B), new Pattern(B, C), new Pattern(A, B, C)));
            Sanitization result = new PrimalDualCover().run(problem);
            assertTrue(result.maskCount() > 0, "empty instance: the test would check nothing");

            for (var entry : result.masked().entrySet()) {
                int s = entry.getKey();
                for (int dropped : entry.getValue()) {
                    Set<Integer> fewer = new TreeSet<>(entry.getValue());
                    fewer.remove(dropped);
                    EventSequence weaker = database.get(s).mask(fewer);
                    boolean stillHidden = true;
                    for (Pattern p : problem.sensitive()) {
                        if (SupportNO.of(p, weaker, problem.gap()) > 0) {
                            stillHidden = false;
                            break;
                        }
                    }
                    assertFalse(stillHidden, "position " + dropped + " of sequence " + s
                            + " is redundant: without it everything is still hidden");
                }
            }
        }
    }

    @Test
    @DisplayName("refuses a threshold above 1, where the factor no longer holds")
    void refusesPartialHiding() {
        SequenceDatabase database = database(5, 10, 12, 3);
        HideProblem partial = new HideProblem(database, List.of(new Pattern(A, B)),
                new GapConstraint(0, 2), HideProblem.Mode.HIDE_DB, 6, 0.5, 4);
        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> new PrimalDualCover().run(partial));
        assertTrue(thrown.getMessage().contains("full hiding"), thrown.getMessage());
    }
}
