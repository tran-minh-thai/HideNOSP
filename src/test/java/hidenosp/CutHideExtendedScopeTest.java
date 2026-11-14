// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.BruteForce;
import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.AdaptiveCutHide;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.hide.MaskLocalSearch;
import hidenosp.hide.Sanitization;
import hidenosp.metrics.DataProfile;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three stages of CUT-HIDE outside the scope they were first written for: HIDE-SEQ (fixed
 * allocation r - 1, one-sequence moves only) and HIDE-DB with g- &gt; 0 (the program of the
 * corollary). On small random databases every stage hides, masks exactly |R|* found by trying every
 * subset, never loses more patterns than the stage before, and never fewer than the optimum found by
 * trying every minimum hiding set.
 */
class CutHideExtendedScopeTest {

    private record Stages(Sanitization first, Sanitization second, Sanitization third) {
    }

    private static Stages stages(HideProblem problem, Metrics.Context context, int sigma, double epsilon) {
        Map<Pattern, Long> frequent = context.frequent();
        Set<Pattern> closure = problem.closureOf(frequent.keySet());
        List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
        double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
        Map<Pattern, Long> watched = new LinkedHashMap<>();
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            if (!closure.contains(e.getKey())) {
                watched.put(e.getKey(), e.getValue());
            }
        }
        Metrics.Context.Session session = context.session(problem);
        Sanitization first = CutHide.forBand(CutHideConfig.standard(), band, factor).run(problem);
        Sanitization second = new AdaptiveCutHide(band, factor, watched, sigma,
                AdaptiveCutHide.DEFAULT_ITERATIONS, session::drops).run(problem);
        Sanitization third = new AdaptiveCutHide(band, factor, watched, sigma,
                AdaptiveCutHide.DEFAULT_ITERATIONS, session::drops)
                .withLocalSearch(MaskLocalSearch.DEFAULT_PASSES, MaskLocalSearch.DEFAULT_RECEIVERS).run(problem);
        return new Stages(first, second, third);
    }

    private static SequenceDatabase randomDatabase(Random random, int sequences, int length, int alphabet,
                                                   Pattern plant, GapConstraint gap) {
        List<EventSequence> out = new ArrayList<>();
        for (int s = 0; s < sequences; s++) {
            int[] events = new int[length];
            for (int i = 0; i < length; i++) {
                events[i] = random.nextInt(alphabet);
            }
            // Plant the pattern once, at the smallest allowed gap, so that most sequences carry it.
            int at = random.nextInt(Math.max(1, length - plant.length() * (gap.gmin() + 1)));
            for (int j = 0; j < plant.length() && at < length; j++) {
                events[at] = plant.at(j);
                at += gap.gmin() + 1;
            }
            out.add(new EventSequence(s, events));
        }
        return new SequenceDatabase(out);
    }

    private static boolean hides(HideProblem problem, Sanitization r) {
        SequenceDatabase after = problem.database().mask(r.masked());
        Pattern p = problem.sensitive().get(0);
        if (problem.isDatabaseMode()) {
            return SupportNO.of(p, after, problem.gap()) < problem.threshold();
        }
        for (int s = 0; s < after.size(); s++) {
            if (SupportNO.of(p, after.get(s), problem.gap()) >= problem.threshold()) {
                return false;
            }
        }
        return true;
    }

    /** Positions of the database that carry an item of P, as (sequence, position) pairs. */
    private static List<int[]> carrying(SequenceDatabase db, Pattern p) {
        List<int[]> out = new ArrayList<>();
        for (int s = 0; s < db.size(); s++) {
            for (int i = 0; i < db.get(s).length(); i++) {
                if (p.contains(db.get(s).at(i))) {
                    out.add(new int[] {s, i});
                }
            }
        }
        return out;
    }

    /**
     * {|R|*, least Lost at |R|*} by trying every subset of the carrying positions. A position that
     * carries no item of P lowers no support, so no minimum solution masks it.
     */
    private static int[] bySubsets(HideProblem problem, Metrics.Context.Session session) {
        List<int[]> cells = carrying(problem.database(), problem.sensitive().get(0));
        int k = cells.size();
        int bestSize = Integer.MAX_VALUE;
        int bestLost = Integer.MAX_VALUE;
        for (int size = 0; size <= k && size <= bestSize; size++) {
            for (long mask = 0; mask < (1L << k); mask++) {
                if (Long.bitCount(mask) != size) {
                    continue;
                }
                Sanitization r = new Sanitization(problem.database());
                for (int c = 0; c < k; c++) {
                    if ((mask & (1L << c)) != 0) {
                        r.mask(cells.get(c)[0], cells.get(c)[1]);
                    }
                }
                if (!hides(problem, r)) {
                    continue;
                }
                bestSize = size;
                bestLost = Math.min(bestLost, session.evaluate(r).lost());
            }
        }
        return new int[] {bestSize, bestLost};
    }

    private record Tally(int instances, int optimal, int improvedByLaterStages) {
    }

    private static Tally run(Random random, HideProblem.Mode mode, int rounds) {
        int instances = 0;
        int optimal = 0;
        int improved = 0;
        for (int round = 0; round < rounds; round++) {
            int gmin = mode == HideProblem.Mode.HIDE_SEQ ? random.nextInt(2) : 1;
            GapConstraint gap = new GapConstraint(gmin, gmin + 1 + random.nextInt(2));
            boolean repeated = random.nextInt(3) == 0;
            Pattern p = repeated ? new Pattern(0, 0) : new Pattern(0, 1);
            int sequences = 2 + random.nextInt(2);
            SequenceDatabase db = randomDatabase(random, sequences, 6 + random.nextInt(2), 3, p, gap);
            int carried = carrying(db, p).size();
            if (carried > 16) {
                continue;
            }
            int sigma = 2;
            int threshold = mode == HideProblem.Mode.HIDE_SEQ ? 1 + random.nextInt(2) : sigma;
            Metrics.Context context = Metrics.Context.of(db, gap, 3, sigma);
            if (!context.frequent().containsKey(p)) {
                continue;
            }
            HideProblem problem = new HideProblem(db, List.of(p), gap, mode, threshold, 0.5, 3, sigma);
            Metrics.Context.Session session = context.session(problem);
            int[] best = bySubsets(problem, session);
            if (best[0] == 0) {
                continue;
            }
            instances++;
            Stages st = stages(problem, context, sigma, 0.5);
            int lost1 = session.evaluate(st.first()).lost();
            int lost2 = session.evaluate(st.second()).lost();
            int lost3 = session.evaluate(st.third()).lost();
            for (Sanitization r : List.of(st.first(), st.second(), st.third())) {
                assertTrue(hides(problem, r), "a stage leaves the pattern visible: " + problem);
                assertEquals(best[0], r.maskCount(), "|R| of a stage against |R|* by subsets: " + problem);
            }
            // The exact |R|* of the data profile (Theorem 1, 2(b) or the corollary) agrees too.
            DataProfile profile = DataProfile.of(db, p, gap, mode, threshold);
            assertTrue(profile.isExact(), "the profile says |R|* is not exact for " + p + " " + gap);
            assertEquals(best[0], profile.minMasks(), "|R|* of the data profile: " + problem);
            assertTrue(lost2 <= lost1 && lost3 <= lost2, "Lost rose across the stages: " + lost1 + ", "
                    + lost2 + ", " + lost3);
            assertTrue(lost3 >= best[1], "below the optimum found by subsets: " + lost3 + " < " + best[1]);
            if (lost3 == best[1]) {
                optimal++;
            }
            if (lost3 < lost1) {
                improved++;
            }
        }
        return new Tally(instances, optimal, improved);
    }

    @Test
    @DisplayName("HIDE-SEQ, g- in {0, 1}, r in {1, 2}: the three stages against every subset")
    void perSequenceLimit() {
        Tally t = run(new Random(20261109L), HideProblem.Mode.HIDE_SEQ, 260);
        System.out.printf("HIDE-SEQ: %d instances, Lost optimal on %d, improved by stages (ii)-(iii) on %d%n",
                t.instances(), t.optimal(), t.improvedByLaterStages());
        assertTrue(t.instances() >= 60, "too few instances need a mask: " + t.instances());
    }

    @Test
    @DisplayName("HIDE-DB with g- = 1: the three stages against every subset")
    void positiveMinimumGap() {
        Tally t = run(new Random(17L), HideProblem.Mode.HIDE_DB, 260);
        System.out.printf("HIDE-DB g- = 1: %d instances, Lost optimal on %d, improved by stages (ii)-(iii) on %d%n",
                t.instances(), t.optimal(), t.improvedByLaterStages());
        assertTrue(t.instances() >= 60, "too few instances need a mask: " + t.instances());
    }

    @Test
    @DisplayName("HIDE-SEQ with several patterns: the greedy hides every pattern in every sequence")
    void perSequenceSeveralPatterns() {
        Random random = new Random(5L);
        int checked = 0;
        for (int round = 0; round < 120; round++) {
            GapConstraint gap = new GapConstraint(random.nextInt(2), 2 + random.nextInt(2));
            SequenceDatabase db = randomDatabase(random, 4, 14, 3, new Pattern(0, 1), gap);
            List<Pattern> sensitive = List.of(new Pattern(0, 1), new Pattern(1, 2), new Pattern(2, 2));
            int r = 1 + random.nextInt(3);
            HideProblem problem = new HideProblem(db, sensitive, gap, HideProblem.Mode.HIDE_SEQ, r, 0.5, 3, 2);
            Sanitization result = new CutHide(CutHideConfig.uniformWeights()).run(problem);
            SequenceDatabase after = db.mask(result.masked());
            boolean needed = false;
            for (Pattern p : sensitive) {
                for (int s = 0; s < db.size(); s++) {
                    needed |= SupportNO.of(p, db.get(s), gap) >= r;
                    assertTrue(SupportNO.of(p, after.get(s), gap) < r, "visible after the greedy");
                }
                // Checked again by exhaustive packing, apart from the index the greedy reads.
                assertTrue(BruteForce.supNO(p, after.get(0), gap) < r);
            }
            if (needed) {
                checked++;
            }
        }
        assertTrue(checked >= 60, "too few instances need a mask: " + checked);
    }

    @Test
    @DisplayName("HIDE-SEQ with several patterns: a pattern already below r does not attract a mask")
    void perSequenceGreedyIgnoresSatisfiedPatterns() {
        // c d a b a b with r = 2: <c,d> occurs once, already below r; <a,b> occurs twice and needs one mask.
        // With uniform weights every vital position of either pattern removes one unit, and the greedy
        // keeps the first best position, so counting the satisfied pattern would mask c first, uselessly,
        // and then a: two masks. One mask on <a,b> is enough.
        int a = 0, b = 1, c = 2, d = 3;
        SequenceDatabase db = new SequenceDatabase(List.of(new EventSequence(0, new int[] {c, d, a, b, a, b})));
        GapConstraint gap = new GapConstraint(0, 3);
        List<Pattern> sensitive = List.of(new Pattern(a, b), new Pattern(c, d));
        HideProblem problem = new HideProblem(db, sensitive, gap, HideProblem.Mode.HIDE_SEQ, 2, 0.5, 3, 2);
        assertEquals(2, SupportNO.of(new Pattern(a, b), db.get(0), gap));
        assertEquals(1, SupportNO.of(new Pattern(c, d), db.get(0), gap));
        Sanitization result = new CutHide(CutHideConfig.uniformWeights()).run(problem);
        assertEquals(1, result.maskCount(), "masked " + result.masked());
        assertTrue(SupportNO.of(new Pattern(a, b), db.mask(result.masked()).get(0), gap) < 2);
    }

    @Test
    @DisplayName("HIDE-SEQ with several patterns: no more masks than the summed need of every sequence")
    void perSequenceGreedyStaysWithinTheSummedNeed() {
        // Each mask lands on a position vital for a pattern still at or above r, which lowers its support by
        // at least one, so the greedy places at most sum over S and P of max(0, sup_NO(P, S) - r + 1) masks.
        // A mask spent on a pattern already below r lowers no need and can push the count above this sum.
        Random random = new Random(20261114L);
        int compared = 0;
        int withSatisfied = 0;
        for (int round = 0; round < 400; round++) {
            GapConstraint gap = new GapConstraint(random.nextInt(2), 1 + random.nextInt(3));
            SequenceDatabase db = randomDatabase(random, 3, 12, 4, new Pattern(0, 1), gap);
            List<Pattern> sensitive = List.of(new Pattern(0, 1), new Pattern(2, 3), new Pattern(1, 2));
            int r = 1 + random.nextInt(3);
            HideProblem problem = new HideProblem(db, sensitive, gap, HideProblem.Mode.HIDE_SEQ, r, 0.5, 3, 2);
            long need = 0;
            boolean satisfiedOne = false;
            for (int s = 0; s < db.size(); s++) {
                boolean needy = false;
                boolean satisfied = false;
                for (Pattern p : sensitive) {
                    int f = SupportNO.of(p, db.get(s), gap);
                    need += Math.max(0, f - r + 1);
                    needy |= f >= r;
                    satisfied |= f > 0 && f < r;
                }
                satisfiedOne |= needy && satisfied;
            }
            if (need == 0) {
                continue;
            }
            Sanitization result = new CutHide(CutHideConfig.uniformWeights()).run(problem);
            assertTrue(result.maskCount() <= need, result.maskCount() + " masks, summed need " + need);
            compared++;
            if (satisfiedOne) {
                withSatisfied++;
            }
        }
        // The bound only bites where a sequence holds a needy pattern beside one already below r.
        assertTrue(compared >= 200 && withSatisfied >= 100, compared + " compared, " + withSatisfied
                + " with a satisfied pattern beside a needy one");
    }
}
