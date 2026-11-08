// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S-plus, the frequent patterns that count as hidden together with the sensitive ones. Under the
 * NECESSARY rule they are the patterns that every solution hides; the earlier SUBSEQUENCE rule also
 * took patterns that hiding does not hide when g+ is finite.
 */
class ClosureRuleTest {

    private static final int A = 0;
    private static final int B = 1;
    private static final int C = 2;

    private static SequenceDatabase database(int[]... sequences) {
        List<EventSequence> out = new ArrayList<>();
        for (int[] s : sequences) {
            out.add(new EventSequence(out.size(), s));
        }
        return new SequenceDatabase(out);
    }

    @Test
    @DisplayName("Block containment: ab is a block of cab and abc, not of acb or ba")
    void blockContainment() {
        Pattern ab = new Pattern(A, B);
        assertTrue(FrequentPatterns.containsBlock(ab, new Pattern(C, A, B)));
        assertTrue(FrequentPatterns.containsBlock(ab, new Pattern(A, B, C)));
        assertTrue(FrequentPatterns.containsBlock(ab, ab));
        assertFalse(FrequentPatterns.containsBlock(ab, new Pattern(A, C, B)));
        assertFalse(FrequentPatterns.containsBlock(ab, new Pattern(B, A)));

        List<Pattern> frequent = List.of(ab, new Pattern(C, A, B), new Pattern(A, B, C), new Pattern(A, C, B),
                new Pattern(B, A));
        GapConstraint bounded = GapConstraint.of(0, 1);
        Set<Pattern> necessary = FrequentPatterns.closure(List.of(ab), frequent, bounded,
                FrequentPatterns.Closure.NECESSARY);
        Set<Pattern> subsequence = FrequentPatterns.closure(List.of(ab), frequent, bounded,
                FrequentPatterns.Closure.SUBSEQUENCE);
        assertEquals(Set.of(ab, new Pattern(C, A, B), new Pattern(A, B, C)), necessary);
        assertEquals(Set.of(ab, new Pattern(C, A, B), new Pattern(A, B, C), new Pattern(A, C, B)), subsequence);
        // With g+ unbounded every subsequence super-pattern is hidden with P, so the two rules agree.
        assertEquals(subsequence, FrequentPatterns.closure(List.of(ab), frequent, GapConstraint.unbounded(0),
                FrequentPatterns.Closure.NECESSARY));
    }

    /**
     * The counterexample of the review: D = {ab, accb}, sigma = 1, gap [0,1]. By hand, F_1(D) at
     * l_max = 3 is {a, b, c, ab, ac, cc, cb, acc, acb, ccb}: ab occurs only in ab, because in accb
     * its items are three positions apart. Masking the a of ab hides ab, and acb keeps its
     * occurrence (0, 1, 3) in accb.
     */
    @Test
    @DisplayName("Counterexample D = {ab, accb}: acb survives the hiding of ab, so only the new rule counts it")
    void counterexample() {
        SequenceDatabase db = database(new int[] {A, B}, new int[] {A, C, C, B});
        GapConstraint gap = GapConstraint.of(0, 1);
        Pattern ab = new Pattern(A, B);
        Pattern acb = new Pattern(A, C, B);
        Metrics.Context context = Metrics.Context.of(db, gap, 3, 1);
        assertEquals(10, context.frequent().size(), "F_1(D) of the hand count");

        Sanitization masked = new Sanitization(db);
        masked.mask(0, 0);
        assertEquals(0, SupportNO.of(ab, masked.sanitized(), gap), "ab is hidden");
        assertEquals(1, SupportNO.of(acb, masked.sanitized(), gap), "acb is still frequent");

        HideProblem necessary = new HideProblem(db, List.of(ab), gap, HideProblem.Mode.HIDE_DB, 1, 0.5, 3);
        assertEquals(FrequentPatterns.Closure.NECESSARY, necessary.closure(), "the default is the corrected rule");
        assertFalse(necessary.closureOf(context.frequent().keySet()).contains(acb));
        assertTrue(necessary.subsequenceClosureOf(context.frequent().keySet()).contains(acb));

        Metrics.Row row = context.evaluateMasking(necessary, masked);
        assertEquals(0.0, row.hidingFailure());
        assertEquals(9, row.nonSensitiveBefore(), "F minus {ab}");
        assertEquals(8, row.nonSensitiveBeforeSubsequence(), "F minus {ab, acb}");
        assertEquals(0, row.lost());
        assertEquals(0, row.lostSubsequence());
        assertEquals(9, row.frequentAfter(), "ab lost, acb kept");

        HideProblem old = necessary.withClosure(FrequentPatterns.Closure.SUBSEQUENCE);
        Metrics.Row oldRow = context.evaluateMasking(old, masked);
        assertEquals(8, oldRow.nonSensitiveBefore(), "the earlier rule, as written in the result files");
        assertEquals(oldRow.nonSensitiveBefore(), oldRow.nonSensitiveBeforeSubsequence());
        assertEquals(oldRow.lost(), oldRow.lostSubsequence());
        assertEquals(oldRow.supDist(), oldRow.supDistSubsequence(), 0.0);
    }

    /**
     * Same database, masking the a of both sequences: ab is hidden and a, ac, acc, acb fall below sigma
     * = 1. By hand, the corrected watched set has 9 patterns with supports a2 b2 c2 ac1 cc1 cb1 acc1
     * acb1 ccb1 (sum 12) that fall to a0 b2 c2 ac0 cc1 cb1 acc0 acb0 ccb1 (sum 7); the earlier rule
     * leaves acb out (sum 11 to 7). Lost is 4 against 3, SupDist 5/12 against 4/11, and the patterns
     * frequent after are b, c, cc, cb, ccb.
     */
    @Test
    @DisplayName("Counterexample, both a masked: Lost 4 and SupDist 5/12, against 3 and 4/11 under the earlier rule")
    void sideColumnsDiffer() {
        SequenceDatabase db = database(new int[] {A, B}, new int[] {A, C, C, B});
        GapConstraint gap = GapConstraint.of(0, 1);
        Metrics.Context context = Metrics.Context.of(db, gap, 3, 1);
        Sanitization masked = new Sanitization(db);
        masked.mask(0, 0);
        masked.mask(1, 0);
        HideProblem problem = new HideProblem(db, List.of(new Pattern(A, B)), gap, HideProblem.Mode.HIDE_DB, 1, 0.5, 3);
        Metrics.Row row = context.evaluateMasking(problem, masked);
        assertEquals(4, row.lost());
        assertEquals(3, row.lostSubsequence());
        assertEquals(5.0 / 12, row.supDist(), 1e-12);
        assertEquals(4.0 / 11, row.supDistSubsequence(), 1e-12);
        assertEquals(4.0 / 9, row.lostRelative(), 1e-12);
        assertEquals(3.0 / 8, row.lostRelativeSubsequence(), 1e-12);
        assertEquals(5, row.frequentAfter());
    }

    /**
     * D = {abc}, sigma = 1, gap [0,1], P = ab. By hand F_1(D) = {a, b, c, ab, ac, bc, abc} (ac at
     * positions 0 and 2, one position apart), and the corrected S-plus holds ab and abc. Masking the a
     * loses a and ac among the watched patterns and leaves b, c and bc frequent: three patterns, so
     * frequent_after must count the loss of abc, whose drop no method asks for.
     */
    @Test
    @DisplayName("frequent_after counts the loss of a super-pattern of S-plus: D = {abc}, mask a, 3 patterns left")
    void frequentAfterCountsClosureLosses() {
        SequenceDatabase db = database(new int[] {A, B, C});
        GapConstraint gap = GapConstraint.of(0, 1);
        Metrics.Context context = Metrics.Context.of(db, gap, 3, 1);
        assertEquals(7, context.frequent().size());
        Sanitization masked = new Sanitization(db);
        masked.mask(0, 0);
        HideProblem problem = new HideProblem(db, List.of(new Pattern(A, B)), gap, HideProblem.Mode.HIDE_DB, 1, 0.5, 3);
        Metrics.Row row = context.evaluateMasking(problem, masked);
        assertEquals(2, row.lost(), "a and ac are the watched patterns lost");
        assertEquals(3, row.frequentAfter());
    }

    @Test
    @DisplayName("HIDE-SEQ: a per-sequence limit hides no super-pattern over the database, S-plus is S")
    void perSequenceLimit() {
        SequenceDatabase db = database(new int[] {A, B, C}, new int[] {A, B, C});
        GapConstraint gap = GapConstraint.of(0, 1);
        Pattern ab = new Pattern(A, B);
        Map<Pattern, Long> frequent = FrequentPatterns.mine(db, gap, 3, 1);
        HideProblem seq = new HideProblem(db, List.of(ab), gap, HideProblem.Mode.HIDE_SEQ, 1, 0.5, 3, 1);
        assertEquals(Set.of(ab), seq.closureOf(frequent.keySet()));
        HideProblem dbMode = new HideProblem(db, List.of(ab), gap, HideProblem.Mode.HIDE_DB, 1, 0.5, 3);
        assertTrue(dbMode.closureOf(frequent.keySet()).contains(new Pattern(A, B, C)));
    }

    /**
     * The statement the rule rests on, checked on random databases: whatever masks hide P below sigma,
     * every frequent Q of the NECESSARY S-plus falls below sigma too. The same runs count the patterns
     * of the SUBSEQUENCE S-plus that stay frequent, which shows the earlier rule is wrong here.
     */
    @Test
    @DisplayName("Random databases: every pattern of the corrected S-plus is hidden by any hiding set")
    void necessaryPatternsAreHidden() {
        Random random = new Random(2026);
        int checked = 0;
        int survivorsOfOldRule = 0;
        int instances = 0;
        for (int trial = 0; trial < 400; trial++) {
            int gmax = random.nextInt(3);
            GapConstraint gap = GapConstraint.of(0, gmax);
            int[][] rows = new int[3 + random.nextInt(5)][];
            for (int s = 0; s < rows.length; s++) {
                rows[s] = new int[4 + random.nextInt(6)];
                for (int i = 0; i < rows[s].length; i++) {
                    rows[s][i] = random.nextInt(3);
                }
            }
            SequenceDatabase db = database(rows);
            Pattern p = new Pattern(random.nextInt(3), random.nextInt(3));
            long support = SupportNO.of(p, db, gap);
            if (support < 2) {
                continue;
            }
            int sigma = 1 + random.nextInt((int) support);
            Map<Pattern, Long> frequent = FrequentPatterns.mine(db, gap, 4, sigma);
            HideProblem problem = new HideProblem(db, List.of(p), gap, HideProblem.Mode.HIDE_DB, sigma, 0.5, 4);
            Set<Pattern> necessary = problem.closureOf(frequent.keySet());
            Set<Pattern> subsequence = problem.subsequenceClosureOf(frequent.keySet());
            // A random hiding set: mask random positions holding an item of P until P is hidden.
            Sanitization masks = new Sanitization(db);
            while (SupportNO.of(p, masks.sanitized(), gap) >= sigma) {
                int s = random.nextInt(rows.length);
                int i = random.nextInt(rows[s].length);
                if (p.contains(rows[s][i])) {
                    masks.mask(s, i);
                }
            }
            instances++;
            SequenceDatabase after = masks.sanitized();
            for (Pattern q : frequent.keySet()) {
                if (q.equals(p)) {
                    continue;
                }
                long now = SupportNO.of(q, after, gap);
                if (necessary.contains(q)) {
                    checked++;
                    assertTrue(now < sigma, q + " is in the corrected S-plus but stays frequent, " + p + " sigma "
                            + sigma + " gap " + gap);
                } else if (subsequence.contains(q) && now >= sigma) {
                    survivorsOfOldRule++;
                }
            }
        }
        assertTrue(instances >= 200, "instances: " + instances);
        assertTrue(checked >= 200, "super-patterns of the corrected S-plus checked: " + checked);
        assertTrue(survivorsOfOldRule > 0, "the earlier rule should have patterns that survive the hiding");
        System.out.printf("closure rule: %d instances, %d super-patterns of the corrected S-plus all hidden, "
                + "%d patterns of the earlier S-plus left frequent%n", instances, checked, survivorsOfOldRule);
    }
}
