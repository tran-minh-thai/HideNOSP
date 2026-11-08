// SPDX-License-Identifier: MIT
package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.hide.HideProblem;
import hidenosp.metrics.FrequentPatterns;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exact method with the second objective must refuse an instance whose minimum hiding sets it
 * cannot all enumerate, instead of returning the best of the ones it met under the name of the
 * optimum. Before this check it stopped at {@link IlpExact#MAX_OPTIMAL_SETS} without a word.
 */
class IlpExactLimitTest {

    /** n copies of the sequence 0 1 2 and P = (0, 1, 2): every one of the 3^n choices is a minimum set. */
    private static HideProblem copies(int n, GapConstraint gap) {
        List<EventSequence> list = new ArrayList<>();
        for (int s = 0; s < n; s++) {
            list.add(new EventSequence(s, new int[] {0, 1, 2}));
        }
        return new HideProblem(new SequenceDatabase(list), List.of(new Pattern(0, 1, 2)), gap,
                HideProblem.Mode.HIDE_DB, 1, 0.5, 3);
    }

    @Test
    @DisplayName("refuses when the minimum sets outnumber the enumeration limit")
    void refusesUncertifiedOptimum() {
        GapConstraint gap = GapConstraint.of(0, 2);
        // 3^10 = 59049 minimum sets, above the limit; 30 candidates and 10 masks, inside the others.
        HideProblem big = copies(10, gap);
        Map<Pattern, Long> frequent = FrequentPatterns.mine(big.database(), gap, 3, 1);
        assertTrue(Math.pow(3, 10) > IlpExact.MAX_OPTIMAL_SETS);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> IlpExact.withSideEffects(big, frequent).run(big));
        assertTrue(e.getMessage().contains("cannot be certified"), e.getMessage());
        // Without the second objective any minimum set is exact for |R|, so no refusal.
        assertEquals(10, new IlpExact().run(big).maskCount());

        // 3^8 = 6561 minimum sets, below the limit: the optimum is certified and returned.
        HideProblem small = copies(8, gap);
        Map<Pattern, Long> frequentSmall = FrequentPatterns.mine(small.database(), gap, 3, 1);
        assertTrue(Math.pow(3, 8) < IlpExact.MAX_OPTIMAL_SETS);
        assertEquals(8, IlpExact.withSideEffects(small, frequentSmall).run(small).maskCount());
    }
}
