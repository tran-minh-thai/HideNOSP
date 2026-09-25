package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.metrics.FrequentPatterns;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SBSH against the worked examples of Gkoulalas-Divanis and Loukides, KDD 2011, run on the problem
 * of the paper itself (support = number of sequences, no maximum gap).
 *
 * <p>Figure 1(a) is the database, Figure 1(b) the sensitive sequences, psi = 3, and Section 5.2.3
 * runs SBSH with r = 3. Figure 1(f) prints the output, and Figure 4(b) lists its distance (6) and
 * its side effects (&lt;a,c&gt;, &lt;a,e&gt;, &lt;d,g&gt;). Example 3 with Figure 2(e) fixes
 * Replace on its own. These are the only published outputs of the procedure, so the class is
 * pinned to all of them.
 */
class SbshPaperExampleTest {

    private static final String SYMBOLS = "abcdefgh";
    private static final GapConstraint NO_MAXIMUM_GAP = GapConstraint.unbounded(0);

    private static int[] events(String letters) {
        int[] out = new int[letters.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = SYMBOLS.indexOf(letters.charAt(i));
        }
        return out;
    }

    private static Pattern pattern(String letters) {
        return new Pattern(events(letters));
    }

    private static SequenceDatabase figure1() {
        String[] rows = {"abccde", "abce", "aceb", "cab", "dfghh", "dhb", "dfhhh", "cdfg", "fdhfg"};
        List<EventSequence> sequences = new ArrayList<>();
        for (int t = 0; t < rows.length; t++) {
            sequences.add(new EventSequence(t, events(rows[t])));
        }
        return new SequenceDatabase(sequences);
    }

    private static final List<Pattern> SENSITIVE =
            List.of(pattern("ace"), pattern("dfg"), pattern("dh"), pattern("fh"));

    private static long sequenceSupport(Pattern p, SequenceDatabase database) {
        long count = 0;
        for (int s = 0; s < database.size(); s++) {
            if (SupportNO.of(p, database.get(s), NO_MAXIMUM_GAP) > 0) {
                count++;
            }
        }
        return count;
    }

    /** Every pattern up to length 3 supported by at least psi sequences, by enumeration. */
    private static Map<Pattern, Long> frequentBySequenceCount(SequenceDatabase database, long psi) {
        Map<Pattern, Long> out = new LinkedHashMap<>();
        List<Pattern> level = new ArrayList<>();
        for (int a = 0; a < SYMBOLS.length(); a++) {
            level.add(new Pattern(a));
        }
        for (int length = 1; length <= 3; length++) {
            List<Pattern> next = new ArrayList<>();
            for (Pattern p : level) {
                long support = sequenceSupport(p, database);
                if (support >= psi) {
                    out.put(p, support);
                    for (int a = 0; a < SYMBOLS.length(); a++) {
                        next.add(p.extend(a));
                    }
                }
            }
            level = next;
        }
        return out;
    }

    private static Map<Pattern, Long> nonSensitive(Map<Pattern, Long> frequent) {
        Set<Pattern> closure = FrequentPatterns.closure(SENSITIVE, frequent.keySet());
        Map<Pattern, Long> out = new LinkedHashMap<>();
        frequent.forEach((p, v) -> {
            if (!closure.contains(p)) {
                out.put(p, v);
            }
        });
        return out;
    }

    private static String letters(Pattern p) {
        StringBuilder b = new StringBuilder("<");
        for (int j = 0; j < p.length(); j++) {
            b.append(j == 0 ? "" : ",").append(SYMBOLS.charAt(p.at(j)));
        }
        return b.append(">").toString();
    }

    @Test
    @DisplayName("Figure 1(f): SBSH with psi = 3 and r = 3 masks exactly the published events")
    void figure1Output() {
        SequenceDatabase database = figure1();
        Map<Pattern, Long> frequent = frequentBySequenceCount(database, 3);
        HideProblem problem = new HideProblem(database, SENSITIVE, NO_MAXIMUM_GAP,
                HideProblem.Mode.HIDE_DB, 3, 0.5, 3);
        Sanitization result = new Sbsh(3, SupportCount.SEQUENCE_COUNT)
                .withNonSensitiveFrequent(nonSensitive(frequent))
                .run(problem);

        // Figure 1(f), transactions numbered from 1 in the paper and from 0 here:
        // T2 = *bce, T6 = *hb, T7 = df***, T8 = c*fg, every other transaction untouched.
        Map<Integer, Set<Integer>> expected = new TreeMap<>();
        expected.put(1, Set.of(0));
        expected.put(5, Set.of(0));
        expected.put(6, Set.of(2, 3, 4));
        expected.put(7, Set.of(1));
        Map<Integer, Set<Integer>> actual = new TreeMap<>();
        result.masked().forEach((s, positions) -> {
            if (!positions.isEmpty()) {
                actual.put(s, new TreeSet<>(positions));
            }
        });
        Map<Integer, Set<Integer>> expectedSorted = new TreeMap<>();
        expected.forEach((s, positions) -> expectedSorted.put(s, new TreeSet<>(positions)));
        assertEquals(expectedSorted, actual, "masked positions per transaction");
        assertEquals(6, result.maskCount(), "Figure 4(b): distance of SBSH");
    }

    @Test
    @DisplayName("Figure 4(b): the side effects of SBSH are <a,c>, <a,e> and <d,g>")
    void figure4SideEffects() {
        SequenceDatabase database = figure1();
        Map<Pattern, Long> frequent = frequentBySequenceCount(database, 3);
        Map<Pattern, Long> nonSensitive = nonSensitive(frequent);
        HideProblem problem = new HideProblem(database, SENSITIVE, NO_MAXIMUM_GAP,
                HideProblem.Mode.HIDE_DB, 3, 0.5, 3);
        SequenceDatabase after = new Sbsh(3, SupportCount.SEQUENCE_COUNT)
                .withNonSensitiveFrequent(nonSensitive)
                .run(problem)
                .sanitized();
        Set<String> lost = new TreeSet<>();
        for (Pattern q : nonSensitive.keySet()) {
            if (sequenceSupport(q, after) < 3) {
                lost.add(letters(q));
            }
        }
        assertEquals(new TreeSet<>(List.of("<a,c>", "<a,e>", "<d,g>")), lost);
        for (Pattern p : SENSITIVE) {
            assertEquals(true, sequenceSupport(p, after) < 3, letters(p) + " is still frequent");
        }
    }

    /**
     * Steps 31 to 37 (a gray position is re-annotated before it is removed). The paper's example
     * has no gray position, so this case is constructed, not published.
     *
     * <p>D = {T = xxyyz, U = xzyw, V = yw, W = y}, sensitive &lt;x,y&gt;, psi = 2. U supports more
     * non-sensitive frequent patterns than T, so T is sanitised. Both x of T are gray: &lt;x&gt; and
     * &lt;x,z&gt; sit at psi, but each x alone can be removed without losing them. After the first
     * x is removed, re-annotating the second finds it red, and Replace moves the deletion to the
     * two y (black): T becomes *x**z and no non-sensitive pattern is lost. Removing the second x
     * without re-annotating it would lose &lt;x&gt; and &lt;x,z&gt;.
     */
    @Test
    @DisplayName("a gray position that turns red on re-annotation is replaced, not removed")
    void grayPositionIsReannotated() {
        String symbols = "xyzw";
        java.util.function.Function<String, int[]> ev = w -> w.chars().map(symbols::indexOf).toArray();
        SequenceDatabase database = new SequenceDatabase(List.of(
                new EventSequence(0, ev.apply("xxyyz")), new EventSequence(1, ev.apply("xzyw")),
                new EventSequence(2, ev.apply("yw")), new EventSequence(3, ev.apply("y"))));
        Pattern s = new Pattern(ev.apply("xy"));
        Map<Pattern, Long> nonSensitive = new LinkedHashMap<>();
        for (String q : List.of("x", "y", "z", "w", "xz", "yw")) {
            Pattern p = new Pattern(ev.apply(q));
            nonSensitive.put(p, sequenceSupport(p, database));
        }
        assertEquals(List.of(2L, 4L, 2L, 2L, 2L, 2L), new ArrayList<>(nonSensitive.values()),
                "supports of the constructed case");
        HideProblem problem = new HideProblem(database, List.of(s), NO_MAXIMUM_GAP,
                HideProblem.Mode.HIDE_DB, 2, 0.5, 2);
        Sanitization result = new Sbsh(1, SupportCount.SEQUENCE_COUNT)
                .withNonSensitiveFrequent(nonSensitive)
                .run(problem);
        assertEquals(new TreeSet<>(Set.of(0, 2, 3)), new TreeSet<>(result.maskedIn(0)));
        assertEquals(3, result.maskCount());
        SequenceDatabase after = result.sanitized();
        for (Pattern q : nonSensitive.keySet()) {
            assertEquals(true, sequenceSupport(q, after) >= 2, "lost " + q);
        }
    }

    /**
     * "We compute supports as if the marked nodes had been removed" (Section 5.2.2), under the
     * transfer to sup_NO, where one sequence can carry several units of a support. Constructed.
     *
     * <p>D = {T = xxxyyyzzzw, U = xw}, sensitive &lt;x,y&gt;, sigma = 2, non-sensitive &lt;x,z&gt;
     * (sup_NO 3) and &lt;x,w&gt; (sup_NO 2, at the threshold). All three x of T start gray. Once the
     * first x is removed, &lt;x,z&gt; drops to 2 = sigma, so re-annotating the second x must find
     * it red and replace it by the three y. Reading the supports without this sequence's own marks
     * keeps &lt;x,z&gt; at 3, finds the second x gray, removes it, and loses &lt;x,z&gt;.
     */
    @Test
    @DisplayName("re-annotation counts the marks already made in the same sequence (sup_NO)")
    void reannotationSeesTheMarksOfTheSequence() {
        String symbols = "xyzw";
        java.util.function.Function<String, int[]> ev = w -> w.chars().map(symbols::indexOf).toArray();
        SequenceDatabase database = new SequenceDatabase(List.of(
                new EventSequence(0, ev.apply("xxxyyyzzzw")), new EventSequence(1, ev.apply("xw"))));
        Pattern s = new Pattern(ev.apply("xy"));
        Pattern xz = new Pattern(ev.apply("xz"));
        Pattern xw = new Pattern(ev.apply("xw"));
        Map<Pattern, Long> nonSensitive = new LinkedHashMap<>();
        nonSensitive.put(xz, SupportNO.of(xz, database, NO_MAXIMUM_GAP));
        nonSensitive.put(xw, SupportNO.of(xw, database, NO_MAXIMUM_GAP));
        assertEquals(List.of(3L, 2L), new ArrayList<>(nonSensitive.values()),
                "supports of the constructed case");
        HideProblem problem = new HideProblem(database, List.of(s), NO_MAXIMUM_GAP,
                HideProblem.Mode.HIDE_DB, 2, 0.5, 2);
        Sanitization result = new Sbsh(1).withNonSensitiveFrequent(nonSensitive).run(problem);
        assertEquals(new TreeSet<>(Set.of(0, 3, 4, 5)), new TreeSet<>(result.maskedIn(0)));
        assertEquals(2L, SupportNO.of(xz, result.sanitized(), NO_MAXIMUM_GAP), "<x,z> kept at sigma");
    }

    @Test
    @DisplayName("Example 3: Replace(3, G, 1) = {4,5,6} and Replace(3, G, 2) = {0,1}")
    void example3Replace() {
        // Figure 2(e): T = <c,c,d,f,h,h,h>, s = <c,d,f,h>; positions 2 and 3 are red, the rest black.
        EventSequence t = new EventSequence(0, events("ccdfhhh"));
        Pattern s = pattern("cdfh");
        int[] colour = {Sbsh.BLACK, Sbsh.BLACK, Sbsh.RED, Sbsh.RED, Sbsh.BLACK, Sbsh.BLACK, Sbsh.BLACK};
        assertEquals(List.of(4, 5, 6), Sbsh.replaceOn(1, t, NO_MAXIMUM_GAP, s, 3, colour),
                "r = 1: C-PA finds nothing (position 2 is red), C-SA gives {4,5,6}");
        assertEquals(List.of(0, 1), Sbsh.replaceOn(2, t, NO_MAXIMUM_GAP, s, 3, colour),
                "r = 2: C-PA gives {0,1}, smaller than C-SA's {4,5,6}");
    }
}
