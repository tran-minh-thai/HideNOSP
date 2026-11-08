// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.AdaptiveCutHide;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The selector for an empty B_eps ({@link CutHide#forBand}): with a band known to be computed and
 * empty, CUT-HIDE runs the uniform-weight configuration under that name instead of refusing; with a
 * non-empty band it is the weighted configuration, unchanged; and a band nobody supplied is still
 * refused by {@link CutHide#run}.
 */
class EmptyBandSelectorTest {

    private static final int A = 7;
    private static final int B = 8;
    private static final GapConstraint GAP = new GapConstraint(0, 2);
    private static final int MAX_LENGTH = 3;

    private static SequenceDatabase database(long seed) {
        Random random = new Random(seed);
        List<EventSequence> sequences = new ArrayList<>();
        for (int s = 0; s < 6; s++) {
            int[] events = new int[9];
            for (int i = 0; i < events.length; i++) {
                events[i] = random.nextInt(3);
            }
            events[1] = A;
            events[3] = B;
            sequences.add(new EventSequence(s, events));
        }
        return new SequenceDatabase(sequences);
    }

    private static HideProblem problem(SequenceDatabase database, int sigma) {
        return new HideProblem(database, List.of(new Pattern(A, B)), GAP, HideProblem.Mode.HIDE_DB, sigma, 0.5,
                MAX_LENGTH);
    }

    private static void assertHidden(HideProblem problem, Sanitization result) {
        SequenceDatabase sanitized = problem.database().mask(result.masked());
        long support = SupportNO.of(problem.sensitive().get(0), sanitized, GAP);
        assertTrue(support < problem.threshold(), "sup_NO after masking = " + support);
    }

    @Test
    @DisplayName("an empty computed band selects the uniform-weight configuration, under that name")
    void emptyBandRunsUniform() {
        HideProblem problem = problem(database(3), 4);
        Sanitization selected = CutHide.forBand(CutHideConfig.standard(), List.of(), new double[0]).run(problem);
        Sanitization uniform = new CutHide(CutHideConfig.uniformWeights()).run(problem);
        assertTrue(selected.method().contains("UNIFORM"), selected.method());
        assertFalse(selected.method().contains("VITAL_NODE"), selected.method());
        assertEquals(uniform.masked(), selected.masked(), "the selected run is the uniform-weight run");
        assertHidden(problem, selected);
    }

    @Test
    @DisplayName("all-zero vital-node weights give the uniform-weight solution, which is why the selector is exact")
    void zeroWeightsAreUniform() {
        // The weighted code path with every factor zero: positionWeights then sums nothing, which is
        // the situation of an empty band; the band patterns are still indexed, as they would be.
        int compared = 0;
        for (long seed = 1; seed <= 20; seed++) {
            SequenceDatabase db = database(seed);
            HideProblem problem = problem(db, 4);
            Map<Pattern, Long> frequent = Metrics.Context.of(db, GAP, MAX_LENGTH, 4).frequent();
            Set<Pattern> closure = problem.closureOf(frequent.keySet());
            List<Pattern> band = FrequentPatterns.nearThreshold(frequent, 4, 0.5, closure);
            if (band.isEmpty()) {
                continue;
            }
            Sanitization weighted = new CutHide(CutHideConfig.standard())
                    .withNearThreshold(band, new double[band.size()]).run(problem);
            Sanitization uniform = new CutHide(CutHideConfig.uniformWeights()).run(problem);
            assertEquals(uniform.masked(), weighted.masked(), "seed " + seed);
            compared++;
        }
        assertTrue(compared >= 5, "instances compared: " + compared);
    }

    @Test
    @DisplayName("a non-empty band keeps the weighted configuration exactly as before")
    void nonEmptyBandUnchanged() {
        int compared = 0;
        for (long seed = 1; seed <= 20; seed++) {
            SequenceDatabase db = database(seed);
            HideProblem problem = problem(db, 4);
            Map<Pattern, Long> frequent = Metrics.Context.of(db, GAP, MAX_LENGTH, 4).frequent();
            Set<Pattern> closure = problem.closureOf(frequent.keySet());
            List<Pattern> band = FrequentPatterns.nearThreshold(frequent, 4, 0.5, closure);
            if (band.isEmpty()) {
                continue;
            }
            double[] factor = FrequentPatterns.weightFactors(band, frequent, 4);
            Sanitization selected = CutHide.forBand(CutHideConfig.standard(), band, factor).run(problem);
            Sanitization before = new CutHide(CutHideConfig.standard()).withNearThreshold(band, factor).run(problem);
            assertTrue(selected.method().contains("VITAL_NODE"), selected.method());
            assertEquals(before.method(), selected.method());
            assertEquals(before.masked(), selected.masked(), "seed " + seed);
            compared++;
        }
        assertTrue(compared >= 5, "instances with a non-empty band compared: " + compared);
    }

    @Test
    @DisplayName("a band nobody supplied is still refused")
    void missingBandStillRefused() {
        HideProblem problem = problem(database(3), 4);
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> new CutHide(CutHideConfig.standard()).run(problem));
        assertTrue(thrown.getMessage().contains("B_eps is empty"), thrown.getMessage());
    }

    @Test
    @DisplayName("the adaptive weight runs on an empty band, says so, and starts from the uniform-weight solution")
    void adaptiveOnEmptyBand() {
        SequenceDatabase db = database(5);
        HideProblem problem = problem(db, 4);
        Metrics.Context context = Metrics.Context.of(db, GAP, MAX_LENGTH, 4);
        Set<Pattern> closure = problem.closureOf(context.frequent().keySet());
        Map<Pattern, Long> watched = new LinkedHashMap<>();
        for (Map.Entry<Pattern, Long> e : context.frequent().entrySet()) {
            if (!closure.contains(e.getKey())) {
                watched.put(e.getKey(), e.getValue());
            }
        }
        Metrics.Context.Session session = context.session(problem);
        AdaptiveCutHide adaptive = new AdaptiveCutHide(List.of(), new double[0], watched, 4,
                AdaptiveCutHide.DEFAULT_ITERATIONS, session::drops).withLocalSearch(5, 3);
        Sanitization result = adaptive.run(problem);
        Sanitization uniform = new CutHide(CutHideConfig.uniformWeights()).run(problem);
        assertTrue(result.method().contains("B_eps empty, iteration 0 with uniform weights"), result.method());
        assertEquals(uniform.maskCount(), result.maskCount(), "same |R| as iteration 0");
        assertEquals(session.evaluate(uniform).lost(), adaptive.lostPerIteration()[0], "iteration 0 is uniform");
        assertTrue(session.evaluate(result).lost() <= adaptive.lostPerIteration()[0]);
        assertHidden(problem, result);
    }
}
