package hidenosp;

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
import hidenosp.index.NettreeCutIndex;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The three-sequence example of the stages of CUT-HIDE (ccabc, bbaccbc, bacbacb; sensitive ab,
 * sigma 3, gap [0, 2], l_max 2, epsilon 0.5), recomputed from the repository's own classes.
 *
 * <p>Stage (i), the fixed weight, masks position 4 of the first sequence and 6 of the second and
 * loses cb, whose two occurrences both fall under those masks. Stage (ii) recomputes the weight
 * from the exact drops of that solution, moves the first mask to position 3, and loses nothing, so
 * the local search finds nothing to save. The coefficients c_Q come from the private method of
 * {@link AdaptiveCutHide} itself, called by reflection, so the formula is not copied here.
 *
 * <p>{@link #main} prints every number and exits with 1 when a check fails, so the probe record in
 * results-probe can be regenerated from this file alone.
 */
class ThreeSequenceExampleTest {

    private static final String[] SEQUENCES = {"ccabc", "bbaccbc", "bacbacb"};
    private static final int SIGMA = 3;
    private static final int MAX_LENGTH = 2;
    private static final double EPSILON = 0.5;

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

    private static Map<Integer, List<Integer>> masks(Sanitization s) {
        Map<Integer, List<Integer>> out = new TreeMap<>();
        for (Map.Entry<Integer, Set<Integer>> e : s.masked().entrySet()) {
            List<Integer> positions = new ArrayList<>();
            for (int p : e.getValue()) {
                positions.add(p + 1);
            }
            Collections.sort(positions);
            if (!positions.isEmpty()) {
                out.put(e.getKey() + 1, positions);
            }
        }
        return out;
    }

    /** Every number of the example, as lines; the checks read the same values. */
    record Report(List<String> lines, Map<String, Object> values) {
    }

    static Report compute() throws ReflectiveOperationException {
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
                HideProblem.Mode.HIDE_DB, SIGMA, EPSILON, MAX_LENGTH);
        Metrics.Context context = Metrics.Context.of(database, gap, MAX_LENGTH, SIGMA);
        Map<Pattern, Long> frequent = context.frequent();
        Set<Pattern> closure = FrequentPatterns.closure(problem.sensitive(), frequent.keySet());
        List<Pattern> band = FrequentPatterns.nearThreshold(frequent, SIGMA, EPSILON, closure);
        double[] factor = FrequentPatterns.weightFactors(band, frequent, SIGMA);
        Map<Pattern, Long> watched = new LinkedHashMap<>();
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            if (!closure.contains(e.getKey())) {
                watched.put(e.getKey(), e.getValue());
            }
        }
        List<String> lines = new ArrayList<>();
        Map<String, Object> values = new LinkedHashMap<>();

        Map<String, Long> support = new TreeMap<>();
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            support.put(letters(e.getKey()), e.getValue());
        }
        values.put("support", support);
        lines.add("frequent patterns with sup_NO: " + support);
        Map<String, Double> bandFactor = new TreeMap<>();
        for (int k = 0; k < band.size(); k++) {
            bandFactor.put(letters(band.get(k)), factor[k]);
        }
        values.put("band", bandFactor);
        lines.add("near-threshold band with 1/m_Q: " + bandFactor);

        for (int s = 0; s < SEQUENCES.length; s++) {
            NettreeCutIndex index = new NettreeCutIndex(database.get(s), gap);
            for (Pattern q : band) {
                index.add(q, false);
            }
            double[] w = index.positionWeights(factor);
            Map<Integer, Double> nonZero = new TreeMap<>();
            for (int i = 0; i < w.length; i++) {
                if (w[i] != 0) {
                    nonZero.put(i + 1, w[i]);
                }
            }
            values.put("weight0 T" + (s + 1), nonZero);
            lines.add("fixed weight, T" + (s + 1) + " (position=weight): " + nonZero);
        }

        Sanitization first = new CutHide(CutHideConfig.standard()).withNearThreshold(band, factor).run(problem);
        Map<Pattern, Long> firstDrop = context.session(problem).drops(first);
        values.put("masks (i)", masks(first));
        values.put("lost (i)", lostPatterns(firstDrop, watched));
        lines.add("stage (i): masks " + masks(first) + ", lost " + lostPatterns(firstDrop, watched)
                + ", supports after " + after(firstDrop, watched));

        AdaptiveCutHide adaptive = new AdaptiveCutHide(band, factor, watched, SIGMA,
                AdaptiveCutHide.DEFAULT_ITERATIONS, context.session(problem)::drops);
        Method factors = AdaptiveCutHide.class.getDeclaredMethod("factors", Map.class);
        factors.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Pattern, Double> coefficient = (Map<Pattern, Double>) factors.invoke(adaptive, firstDrop);
        Map<String, Double> named = new TreeMap<>();
        for (Map.Entry<Pattern, Double> e : coefficient.entrySet()) {
            named.put(letters(e.getKey()), e.getValue());
        }
        values.put("coefficient", named);
        lines.add("adaptive coefficients c_Q after stage (i): " + format(named));
        for (int s = 0; s < SEQUENCES.length; s++) {
            NettreeCutIndex index = new NettreeCutIndex(database.get(s), gap);
            double[] w = new double[database.get(s).length()];
            for (Pattern q : watched.keySet()) {
                int id = index.add(q, false);
                if (index.supNO(id) > 0) {
                    for (int position : index.vitalPositions(id)) {
                        w[position] += coefficient.get(q);
                    }
                }
            }
            Map<Integer, Double> all = new TreeMap<>();
            for (int i = 0; i < w.length; i++) {
                if (w[i] != 0) {
                    all.put(i + 1, w[i]);
                }
            }
            values.put("weight1 T" + (s + 1), all);
            lines.add("adaptive weight, T" + (s + 1) + ": " + formatPositions(all));
        }

        Sanitization second = adaptive.run(problem);
        Map<Pattern, Long> secondDrop = context.session(problem).drops(second);
        values.put("masks (ii)", masks(second));
        values.put("lost (ii)", lostPatterns(secondDrop, watched));
        values.put("chosen", adaptive.chosenIteration());
        lines.add("stage (ii): masks " + masks(second) + ", lost " + lostPatterns(secondDrop, watched)
                + ", supports after " + after(secondDrop, watched) + ", chosen iteration "
                + adaptive.chosenIteration());

        AdaptiveCutHide full = new AdaptiveCutHide(band, factor, watched, SIGMA,
                AdaptiveCutHide.DEFAULT_ITERATIONS, context.session(problem)::drops)
                .withLocalSearch(MaskLocalSearch.DEFAULT_PASSES, MaskLocalSearch.DEFAULT_RECEIVERS);
        Sanitization third = full.run(problem);
        values.put("masks (iii)", masks(third));
        values.put("nothing saveable", full.searchResult().nothingSaveable());
        values.put("moves", full.searchResult().moves());
        lines.add("full method: masks " + masks(third) + ", local search " + full.searchResult());
        return new Report(lines, values);
    }

    private static Set<String> lostPatterns(Map<Pattern, Long> drop, Map<Pattern, Long> watched) {
        Set<String> lost = new TreeSet<>();
        for (Map.Entry<Pattern, Long> w : watched.entrySet()) {
            if (w.getValue() - drop.getOrDefault(w.getKey(), 0L) < SIGMA) {
                lost.add(letters(w.getKey()));
            }
        }
        return lost;
    }

    private static Map<String, Long> after(Map<Pattern, Long> drop, Map<Pattern, Long> watched) {
        Map<String, Long> out = new TreeMap<>();
        for (Map.Entry<Pattern, Long> w : watched.entrySet()) {
            long d = drop.getOrDefault(w.getKey(), 0L);
            if (d > 0) {
                out.put(letters(w.getKey()), w.getValue() - d);
            }
        }
        return out;
    }

    private static String format(Map<String, Double> m) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, Double> e : m.entrySet()) {
            b.append(e.getKey()).append(String.format(Locale.ROOT, "=%.4f ", e.getValue()));
        }
        return b.toString().trim();
    }

    private static String formatPositions(Map<Integer, Double> m) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<Integer, Double> e : m.entrySet()) {
            b.append(e.getKey()).append(String.format(Locale.ROOT, "=%.4f ", e.getValue()));
        }
        return b.toString().trim();
    }

    private static double round2(Object value) {
        return Math.round((Double) value * 100) / 100.0;
    }

    /** The numbers the example states, compared at the precision it states them. */
    @SuppressWarnings("unchecked")
    static List<String> check(Report r) {
        List<String> failures = new ArrayList<>();
        Map<String, Object> v = r.values();
        Map<String, Long> support = (Map<String, Long>) v.get("support");
        expect(failures, "sup_NO of ab", 4L, support.get("ab"));
        expect(failures, "sup_NO of cb", 4L, support.get("cb"));
        expect(failures, "sup_NO of a", 4L, support.get("a"));
        expect(failures, "sup_NO of ac", 4L, support.get("ac"));
        Map<Integer, Double> w1 = (Map<Integer, Double>) v.get("weight0 T1");
        Map<Integer, Double> w2 = (Map<Integer, Double>) v.get("weight0 T2");
        expect(failures, "fixed weight of position 4 of T1", 0.5, w1.get(4));
        expect(failures, "fixed weight of position 3 of T1", 1.0, w1.get(3));
        expect(failures, "fixed weight of position 6 of T2", 0.5, w2.get(6));
        expect(failures, "position 3 of T2 heavier than position 6", true, w2.get(3) > w2.get(6));
        expect(failures, "stage (i) masks", Map.of(1, List.of(4), 2, List.of(6)), v.get("masks (i)"));
        expect(failures, "stage (i) loses", Set.of("cb"), v.get("lost (i)"));
        Map<String, Double> c = (Map<String, Double>) v.get("coefficient");
        expect(failures, "c_cb", 0.74, round2(c.get("cb")));
        expect(failures, "c_a", 0.65, round2(c.get("a")));
        expect(failures, "c_ac", 0.65, round2(c.get("ac")));
        expect(failures, "c_b", 0.28, round2(c.get("b")));
        expect(failures, "c_bc", 0.40, round2(c.get("bc")));
        Map<Integer, Double> a1 = (Map<Integer, Double>) v.get("weight1 T1");
        expect(failures, "adaptive weight of position 4 of T1", 1.42, round2(a1.get(4)));
        expect(failures, "adaptive weight of position 3 of T1", 1.30, round2(a1.get(3)));
        expect(failures, "stage (ii) masks", Map.of(1, List.of(3), 2, List.of(6)), v.get("masks (ii)"));
        expect(failures, "stage (ii) loses", Set.of(), v.get("lost (ii)"));
        expect(failures, "full method masks", Map.of(1, List.of(3), 2, List.of(6)), v.get("masks (iii)"));
        expect(failures, "local search finds nothing saveable", true, v.get("nothing saveable"));
        return failures;
    }

    private static int checked;

    private static void expect(List<String> failures, String what, Object want, Object got) {
        checked++;
        if (!want.equals(got)) {
            failures.add(what + ": want " + want + ", got " + got);
        }
    }

    @Test
    @DisplayName("fixed weight loses cb; the adaptive weight moves one mask and loses nothing")
    void example() throws ReflectiveOperationException {
        assertEquals(List.of(), check(compute()));
    }

    /** Prints every number and every failed check; exits with 1 on any failure. */
    public static void main(String[] args) throws ReflectiveOperationException {
        Report r = compute();
        r.lines().forEach(System.out::println);
        List<String> failures = check(r);
        failures.forEach(f -> System.out.println("FAILED " + f));
        System.out.println("checked " + checked + " stated numbers and outcomes, failed " + failures.size());
        System.exit(failures.isEmpty() ? 0 : 1);
    }
}
