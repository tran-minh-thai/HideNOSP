package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.exp.SigmaScan;
import hidenosp.metrics.FrequentPatterns;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sigma rule helper: its counts against the unpruned enumeration and against the definition of
 * a sensitive-set candidate, and the boundaries of the rule itself.
 */
class SigmaScanTest {

    /** Small enough for the unpruned enumeration; the last sequence makes (0, 0) frequent. */
    private static SequenceDatabase database() {
        return new SequenceDatabase(List.of(
                new EventSequence(0, new int[] {0, 1, 2, 0, 1, 2, 0, 1}),
                new EventSequence(1, new int[] {1, 0, 2, 1, 0, 2}),
                new EventSequence(2, new int[] {0, 0, 1, 1, 2, 2}),
                new EventSequence(3, new int[] {0, 0, 2, 0, 0})));
    }

    @Test
    @DisplayName("counts equal the unpruned enumeration and the definition of a candidate")
    void countsMatchEnumeration() {
        SequenceDatabase db = database();
        int checked = 0;
        for (int gmax : new int[] {1, 3}) {
            GapConstraint gap = new GapConstraint(0, gmax);
            for (int sigma = 1; sigma <= 6; sigma++) {
                Map<Pattern, Long> all = FrequentPatterns.enumerateWithoutPruning(db, gap, 4, sigma);
                int[] byLength = new int[4];
                int candidates = 0;
                boolean repeatedFrequent = false;
                for (Pattern p : all.keySet()) {
                    byLength[p.length() - 1]++;
                    Set<Integer> items = new HashSet<>();
                    for (int item : p.items()) {
                        items.add(item);
                    }
                    boolean distinct = items.size() == p.length();
                    if (p.length() >= 2 && p.length() <= 4 && distinct) {
                        candidates++;
                    }
                    repeatedFrequent |= p.length() >= 2 && !distinct;
                }
                SigmaScan.Row row = SigmaScan.scan(db, gap, sigma, 4, 1, 1000);
                assertEquals(all.size(), row.frequent(), "|F| at " + gap + " sigma " + sigma);
                for (int k = 0; k < 4; k++) {
                    assertEquals(byLength[k], row.byLength()[k], "length " + (k + 1) + " at " + gap
                            + " sigma " + sigma);
                }
                assertEquals(candidates, row.pool(), "pool at " + gap + " sigma " + sigma);
                if (sigma == 2 && gmax == 1) {
                    // The case that tells a pool with repeated items from the right one.
                    assertTrue(repeatedFrequent, "(0, 0) must be frequent here");
                    assertTrue(row.pool() < row.frequent() - byLength[0]);
                }
                checked++;
            }
        }
        assertEquals(12, checked);
    }

    @Test
    @DisplayName("the rule keeps [minF, maxF] inclusive and refuses the whole pattern space")
    void ruleBoundaries() {
        assertFalse(SigmaScan.meetsRule(49, 1000, 50, 700));
        assertTrue(SigmaScan.meetsRule(50, 1000, 50, 700));
        assertTrue(SigmaScan.meetsRule(700, 1000, 50, 700));
        assertFalse(SigmaScan.meetsRule(701, 1000, 50, 700));
        assertFalse(SigmaScan.meetsRule(84, 84, 1, 100));
        assertTrue(SigmaScan.meetsRule(83, 84, 1, 100));
    }

    @Test
    @DisplayName("the pattern space counts every word of length 1 to l_max")
    void patternSpace() {
        assertEquals(84, SigmaScan.patternSpace(4, 3));          // the DNA note of protocol.yaml
        assertEquals(168_420, SigmaScan.patternSpace(20, 4));    // protein, l_max = 4
        assertEquals(3, SigmaScan.patternSpace(3, 1));
        assertEquals(Long.MAX_VALUE, SigmaScan.patternSpace(100_000, 4));
    }
}
