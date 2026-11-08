// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.baseline.IlpExact;
import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.hide.AdaptiveCutHide;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.hide.MaskLocalSearch;
import hidenosp.hide.Sanitization;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The five-sequence sample database of the running example (sensitive pattern ab, sigma 6, gap
 * [0, 2], epsilon 0.5) through every stage of CUT-HIDE and through the exact solver of |R| then Lost.
 *
 * <p>With patterns up to length 2, the fixed weight and the adaptive weight both lose ba, while the
 * local search reaches the exact solver's lossless set of three masks. From length 3 on, aaa is
 * frequent too, and even the exact optimum loses one pattern: the example holds only with l_max = 2.
 *
 * <p>{@link #main} prints the table and exits with 1 when a check fails, so the probe record in
 * results-probe can be regenerated from this file alone.
 */
class RunningExampleTest {

    private static final String[] SEQUENCES = {"abaababaa", "aaaab", "acccbab", "baabbab", "cdcda"};
    private static final int SIGMA = 6;
    private static final double EPSILON = 0.5;
    private static final String[] KEYS = {"cut-hide", "cut-hide-rw", "cut-hide-rw-ls", "ilp-lost"};

    /** One method's outcome: masks per sequence (1-based positions) and the patterns lost. */
    record Outcome(int maskCount, double hidingFailure, int lost, Map<Integer, List<Integer>> masks,
                   Set<String> lostPatterns) {
    }

    /** Frequent patterns with their support, and the outcome of each key, for one l_max. */
    record Run(Map<String, Long> frequent, Map<String, Outcome> outcomes) {
    }

    private static Pattern pattern(String letters) {
        int[] items = new int[letters.length()];
        for (int i = 0; i < items.length; i++) {
            items[i] = letters.charAt(i) - 'a';
        }
        return new Pattern(items);
    }

    private static String letters(Pattern p) {
        StringBuilder b = new StringBuilder();
        for (int item : p.items()) {
            b.append((char) ('a' + item));
        }
        return b.toString();
    }

    static Run run(int maxLength) {
        List<EventSequence> sequences = new ArrayList<>();
        for (int s = 0; s < SEQUENCES.length; s++) {
            int[] events = new int[SEQUENCES[s].length()];
            for (int i = 0; i < events.length; i++) {
                events[i] = SEQUENCES[s].charAt(i) - 'a';
            }
            sequences.add(new EventSequence(s, events));
        }
        SequenceDatabase database = new SequenceDatabase(sequences);
        GapConstraint gap = GapConstraint.of(0, 2);
        HideProblem problem = new HideProblem(database, List.of(pattern("ab")), gap,
                HideProblem.Mode.HIDE_DB, SIGMA, EPSILON, maxLength);
        Metrics.Context context = Metrics.Context.of(database, gap, maxLength, SIGMA);
        Map<Pattern, Long> frequent = context.frequent();
        Set<Pattern> closure = problem.closureOf(frequent.keySet());
        List<Pattern> band = FrequentPatterns.nearThreshold(frequent, SIGMA, EPSILON, closure);
        double[] factor = FrequentPatterns.weightFactors(band, frequent, SIGMA);
        Map<Pattern, Long> watched = new LinkedHashMap<>();
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            if (!closure.contains(e.getKey())) {
                watched.put(e.getKey(), e.getValue());
            }
        }

        Map<String, Sanitization> solutions = new LinkedHashMap<>();
        solutions.put("cut-hide", new CutHide(CutHideConfig.standard()).withNearThreshold(band, factor).run(problem));
        solutions.put("cut-hide-rw", new AdaptiveCutHide(band, factor, watched, SIGMA,
                AdaptiveCutHide.DEFAULT_ITERATIONS, context.session(problem)::drops).run(problem));
        solutions.put("cut-hide-rw-ls", new AdaptiveCutHide(band, factor, watched, SIGMA,
                AdaptiveCutHide.DEFAULT_ITERATIONS, context.session(problem)::drops)
                .withLocalSearch(MaskLocalSearch.DEFAULT_PASSES, MaskLocalSearch.DEFAULT_RECEIVERS).run(problem));
        solutions.put("ilp-lost", IlpExact.withSideEffects(problem, frequent).run(problem));

        Map<String, Outcome> outcomes = new LinkedHashMap<>();
        for (Map.Entry<String, Sanitization> e : solutions.entrySet()) {
            Sanitization s = e.getValue();
            Metrics.Row row = context.session(problem).evaluate(s);
            Map<Pattern, Long> drop = context.session(problem).drops(s);
            Map<Integer, List<Integer>> masks = new TreeMap<>();
            for (Map.Entry<Integer, Set<Integer>> m : s.masked().entrySet()) {
                List<Integer> positions = new ArrayList<>();
                for (int p : m.getValue()) {
                    positions.add(p + 1);
                }
                Collections.sort(positions);
                if (!positions.isEmpty()) {
                    masks.put(m.getKey() + 1, positions);
                }
            }
            Set<String> lost = new TreeSet<>();
            for (Map.Entry<Pattern, Long> w : watched.entrySet()) {
                if (w.getValue() - drop.getOrDefault(w.getKey(), 0L) < SIGMA) {
                    lost.add(letters(w.getKey()));
                }
            }
            outcomes.put(e.getKey(), new Outcome(s.maskCount(), row.hidingFailure(), row.lost(), masks, lost));
        }
        Map<String, Long> named = new TreeMap<>();
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            named.put(letters(e.getKey()), e.getValue());
        }
        return new Run(named, outcomes);
    }

    /** Every check, as messages; empty means all hold. Prints nothing. */
    static List<String> check(int maxLength, Run run) {
        List<String> failures = new ArrayList<>();
        Map<String, Outcome> o = run.outcomes();
        for (String key : KEYS) {
            if (o.get(key).maskCount() != 3 || o.get(key).hidingFailure() != 0.0) {
                failures.add(key + ": want |R| = 3 and HF = 0, got " + o.get(key));
            }
            if (o.get(key).lost() != o.get(key).lostPatterns().size()) {
                failures.add(key + ": Lost column " + o.get(key).lost() + " differs from the lost patterns counted here "
                        + o.get(key).lostPatterns());
            }
        }
        if (maxLength == 2) {
            Map<String, Long> want = new TreeMap<>(Map.of("a", 16L, "b", 10L, "aa", 10L, "ab", 8L, "ba", 6L, "bb", 6L));
            if (!run.frequent().equals(want)) {
                failures.add("F_sigma with l_max = 2: want " + want + ", got " + run.frequent());
            }
            for (String key : new String[] {"cut-hide", "cut-hide-rw"}) {
                if (!o.get(key).lostPatterns().equals(Set.of("ba"))) {
                    failures.add(key + " with l_max = 2 should lose exactly ba, lost " + o.get(key).lostPatterns());
                }
            }
            if (!o.get("cut-hide-rw-ls").lostPatterns().isEmpty() || o.get("ilp-lost").lost() != 0) {
                failures.add("with l_max = 2 the full method and the exact solver should lose nothing: "
                        + o.get("cut-hide-rw-ls") + " / " + o.get("ilp-lost"));
            }
            if (!o.get("cut-hide-rw-ls").masks().equals(o.get("ilp-lost").masks())) {
                failures.add("with l_max = 2 the full method should give the exact solver's masks: "
                        + o.get("cut-hide-rw-ls").masks() + " vs " + o.get("ilp-lost").masks());
            }
        } else {
            if (!Long.valueOf(7).equals(run.frequent().get("aaa"))) {
                failures.add("with l_max = " + maxLength + ", aaa should be frequent with support 7: " + run.frequent());
            }
            if (o.get("ilp-lost").lost() != 1 || o.get("cut-hide-rw-ls").lost() != 1) {
                failures.add("with l_max = " + maxLength + ", the exact optimum loses 1 and the full method reaches it: "
                        + o.get("cut-hide-rw-ls") + " / " + o.get("ilp-lost"));
            }
        }
        return failures;
    }

    @Test
    @DisplayName("l_max = 2: only the local search reaches the exact lossless set of three masks")
    void lengthTwo() {
        assertEquals(List.of(), check(2, run(2)));
    }

    @Test
    @DisplayName("l_max = 3 and 4: aaa is frequent and even the exact optimum loses one pattern")
    void lengthThreeAndFour() {
        assertEquals(List.of(), check(3, run(3)));
        assertEquals(List.of(), check(4, run(4)));
    }

    /** Prints the table for l_max = 2, 3, 4 and every failed check; exits with 1 on any failure. */
    public static void main(String[] args) {
        int failed = 0;
        int checked = 0;
        for (int maxLength = 2; maxLength <= 4; maxLength++) {
            Run run = run(maxLength);
            System.out.println("l_max = " + maxLength + "; frequent: " + run.frequent());
            for (Map.Entry<String, Outcome> e : run.outcomes().entrySet()) {
                Outcome o = e.getValue();
                System.out.printf("  %-15s |R| = %d  HF = %.1f  Lost = %d  masks (sequence=[positions]) %s  lost %s%n",
                        e.getKey(), o.maskCount(), o.hidingFailure(), o.lost(), o.masks(), o.lostPatterns());
            }
            List<String> failures = check(maxLength, run);
            checked++;
            failed += failures.isEmpty() ? 0 : 1;
            for (String f : failures) {
                System.out.println("  FAILED " + f);
            }
        }
        System.out.println("checked " + checked + " values of l_max, failed " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
