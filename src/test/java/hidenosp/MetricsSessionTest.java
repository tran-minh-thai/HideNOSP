// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A session that remembers masked sequences gives the rows of a fresh evaluation. */
class MetricsSessionTest {

    @Test
    @DisplayName("one session over many sanitizations equals fresh evaluations")
    void sessionEqualsFreshEvaluation() {
        Random random = new Random(55);
        int compared = 0;
        int revisited = 0;
        for (int trial = 0; trial < 40; trial++) {
            List<EventSequence> list = new ArrayList<>();
            for (int s = 0; s < 6; s++) {
                int[] events = new int[8 + random.nextInt(8)];
                for (int i = 0; i < events.length; i++) {
                    events[i] = random.nextInt(3);
                }
                list.add(new EventSequence(s, events));
            }
            SequenceDatabase db = new SequenceDatabase(list);
            GapConstraint gap = GapConstraint.of(0, 2);
            int sigma = 4;
            Metrics.Context context = Metrics.Context.of(db, gap, 3, sigma);
            if (context.frequent().isEmpty()) {
                continue;
            }
            Pattern p = new ArrayList<>(context.frequent().keySet()).get(0);
            HideProblem problem = new HideProblem(db, List.of(p), gap, HideProblem.Mode.HIDE_DB, sigma, 0.5, 3);
            Metrics.Context.Session session = context.session(problem);
            // A walk like a Pareto front: each step changes the masks of one sequence, and some
            // steps return to a masking met before.
            List<List<Integer>> masks = new ArrayList<>();
            for (int s = 0; s < 6; s++) {
                masks.add(new ArrayList<>());
            }
            for (int step = 0; step < 25; step++) {
                int s = random.nextInt(6);
                int length = list.get(s).length();
                masks.set(s, new ArrayList<>());
                for (int i = 0; i < length; i++) {
                    if (random.nextInt(4) == 0) {
                        masks.get(s).add(i);
                    }
                }
                if (step % 5 == 4) {
                    masks.set(s, new ArrayList<>());   // back to an unmasked sequence
                    revisited++;
                }
                Sanitization sanitization = new Sanitization(db);
                for (int k = 0; k < 6; k++) {
                    for (int i : masks.get(k)) {
                        sanitization.mask(k, i);
                    }
                }
                Metrics.Row fromSession = session.evaluate(sanitization);
                Metrics.Row fresh = Metrics.Context.of(db, gap, 3, sigma).evaluateMasking(problem, sanitization);
                assertEquals(fresh, fromSession, "step " + step + " of trial " + trial);
                compared++;
            }
        }
        assertTrue(compared >= 500, "rows compared: " + compared);
        assertTrue(revisited >= 50, "maskings met again: " + revisited);
    }
}
