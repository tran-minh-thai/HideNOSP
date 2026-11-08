// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.Pattern;
import hidenosp.exp.SensitiveSampling;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sensitive sets of the main comparison: five strata of sup_NO, distinct sets only, and an
 * honest shortfall when the pool is small. The small pools below are the sizes measured on the DNA
 * dataset (4 and 9 candidates).
 */
class SensitiveSamplingTest {

    /**
     * {@code count} distinct-item candidates whose supports are a permutation of 1..count that does
     * not follow the insertion order (so a sampler that forgot to sort would be caught), plus three
     * patterns that must never be candidates.
     */
    private static Map<Pattern, Long> frequent(int count) {
        int step = 37;
        while (gcd(step, count) != 1) {
            step++;
        }
        Map<Pattern, Long> out = new LinkedHashMap<>();
        int made = 0;
        outer:
        for (int a = 0; a < 40; a++) {
            for (int b = 0; b < 40; b++) {
                if (a != b) {
                    out.put(new Pattern(a, b), (long) ((long) made * step % count) + 1);
                    if (++made == count) {
                        break outer;
                    }
                }
            }
        }
        out.put(new Pattern(1), 1000L);                   // too short
        out.put(new Pattern(2, 2), 1000L);                // repeated item
        out.put(new Pattern(1, 2, 3, 4, 5), 1000L);       // too long
        return out;
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private static Set<Pattern> excluded() {
        return Set.of(new Pattern(1), new Pattern(2, 2), new Pattern(1, 2, 3, 4, 5));
    }

    @Test
    @DisplayName("20 distinct sets from a large pool, four per stratum, strata ordered by sup_NO")
    void distinctSetsInOrderedStrata() {
        Map<Pattern, Long> frequent = frequent(50);
        SensitiveSampling.Result result = SensitiveSampling.draw(frequent, 1, 4, 2026);
        assertEquals(50, result.poolSize());
        assertEquals(20, result.draws().size());
        Set<List<Pattern>> seen = new HashSet<>();
        Map<Integer, List<Long>> supportsByStratum = new LinkedHashMap<>();
        for (SensitiveSampling.Draw d : result.draws()) {
            assertTrue(seen.add(d.patterns()), "repeated set " + d.patterns());
            for (Pattern p : d.patterns()) {
                assertFalse(excluded().contains(p), "excluded candidate drawn: " + p);
                supportsByStratum.computeIfAbsent(d.percentile(), k -> new ArrayList<>())
                        .add(frequent.get(p));
            }
        }
        long previousMax = Long.MIN_VALUE;
        for (int percentile : SensitiveSampling.PERCENTILES) {
            List<Long> supports = supportsByStratum.get(percentile);
            assertEquals(4, supports.size(), "sets drawn at p" + percentile);
            long min = supports.stream().mapToLong(Long::longValue).min().getAsLong();
            long max = supports.stream().mapToLong(Long::longValue).max().getAsLong();
            assertTrue(min > previousMax, "stratum p" + percentile + " overlaps the one below");
            previousMax = max;
        }
    }

    @Test
    @DisplayName("nine sets out of ten candidates per stratum are still nine distinct sets")
    void noRepeatWhenAlmostEveryCandidateIsTaken() {
        SensitiveSampling.Result result = SensitiveSampling.draw(frequent(50), 1, 9, 7);
        Set<List<Pattern>> seen = new HashSet<>();
        for (SensitiveSampling.Draw d : result.draws()) {
            assertTrue(seen.add(d.patterns()), "repeated set " + d.patterns());
        }
        assertEquals(45, result.draws().size());
    }

    @Test
    @DisplayName("a small pool gives fewer sets, never a repeated one (the DNA pools of 9 and 4)")
    void smallPoolShortfallIsReported() {
        SensitiveSampling.Result nine = SensitiveSampling.draw(frequent(9), 1, 4, 2026);
        assertEquals(9, nine.draws().size(), "every candidate once, not 20 draws");
        assertEquals(20, nine.requested());
        assertEquals(9, new HashSet<>(nine.draws()).size());

        SensitiveSampling.Result four = SensitiveSampling.draw(frequent(4), 1, 4, 2026);
        assertEquals(4, four.draws().size());

        SensitiveSampling.Result tooLarge = SensitiveSampling.draw(frequent(9), 5, 4, 2026);
        assertTrue(tooLarge.draws().isEmpty(), "no stratum holds five candidates");
    }

    @Test
    @DisplayName("when a stratum has fewer possible sets than asked, it gives all of them")
    void allCombinationsOfAFewCandidates() {
        // 15 candidates: strata of 3, and C(3, 2) = 3 sets of two, fewer than the 4 asked for.
        SensitiveSampling.Result result = SensitiveSampling.draw(frequent(15), 2, 4, 2026);
        assertEquals(15, result.draws().size());
        assertEquals(15, new HashSet<>(result.draws()).size());
    }

    @Test
    @DisplayName("the same seed gives the same sets, another seed other sets")
    void seeded() {
        Map<Pattern, Long> frequent = frequent(200);
        List<SensitiveSampling.Draw> a = SensitiveSampling.draw(frequent, 5, 4, 2026).draws();
        List<SensitiveSampling.Draw> b = SensitiveSampling.draw(frequent, 5, 4, 2026).draws();
        List<SensitiveSampling.Draw> c = SensitiveSampling.draw(frequent, 5, 4, 2027).draws();
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    @DisplayName("the repeated-item pool holds repeated items of length 2 to 4, ordered by sup_NO")
    void repeatedPool() {
        Map<Pattern, Long> frequent = new LinkedHashMap<>();
        frequent.put(new Pattern(3, 3), 7L);
        frequent.put(new Pattern(1, 2, 1), 2L);
        frequent.put(new Pattern(4, 4, 4, 4), 5L);
        frequent.put(new Pattern(5, 6), 9L);                 // distinct: not in this pool
        frequent.put(new Pattern(7), 9L);                    // too short
        frequent.put(new Pattern(8, 8, 8, 8, 8), 9L);        // too long
        List<Pattern> pool = SensitiveSampling.repeatedPool(frequent);
        assertEquals(List.of(new Pattern(1, 2, 1), new Pattern(4, 4, 4, 4), new Pattern(3, 3)), pool);
        SensitiveSampling.Result drawn = SensitiveSampling.draw(frequent, 1, 4, 2026, true);
        assertTrue(drawn.repeatedItems());
        assertEquals(3, drawn.poolSize());
        for (SensitiveSampling.Draw d : drawn.draws()) {
            assertFalse(d.patterns().get(0).hasDistinctItems(), "a repeated-item set was expected");
        }
        assertTrue(drawn.describe().contains("repeated-item pool 3"), drawn.describe());
        SensitiveSampling.Result distinct = SensitiveSampling.draw(frequent, 1, 4, 2026);
        assertEquals(1, distinct.poolSize());
        assertEquals(new Pattern(5, 6), distinct.draws().get(0).patterns().get(0));
    }
}
