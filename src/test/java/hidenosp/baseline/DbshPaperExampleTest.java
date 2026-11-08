// SPDX-License-Identifier: MIT
package hidenosp.baseline;

import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DBSH against the published outputs of Gkoulalas-Divanis and Loukides, KDD 2011: Figure 1(e)
 * prints the sanitised database, Figure 4(b) its distance (5) and side effects.
 *
 * <p>This example is what fixes the reading of Exclude: T8 is sanitised although sanitising T5
 * alone would already bring &lt;d,f,g&gt; below psi. That happens only when the count M(s) of
 * Algorithm 3 grows with every transaction visited, the kept and the sanitised alike.
 */
class DbshPaperExampleTest {

    private static final HideProblem PROBLEM = new HideProblem(PaperFigure1.database(),
            PaperFigure1.SENSITIVE, PaperFigure1.NO_MAXIMUM_GAP, HideProblem.Mode.HIDE_DB,
            PaperFigure1.PSI, 0.5, 3);

    @Test
    @DisplayName("Figure 1(e): DBSH with psi = 3 masks exactly the published events")
    void figure1Output() {
        Sanitization result = new Dbsh(SupportCount.SEQUENCE_COUNT).run(PROBLEM);
        // Figure 1(e), transactions numbered from 1 in the paper and from 0 here:
        // T1 = *bccde, T5 = **ghh, T6 = *hb, T8 = c*fg, every other transaction untouched.
        Map<Integer, Set<Integer>> expected = new TreeMap<>();
        expected.put(0, new TreeSet<>(Set.of(0)));
        expected.put(4, new TreeSet<>(Set.of(0, 1)));
        expected.put(5, new TreeSet<>(Set.of(0)));
        expected.put(7, new TreeSet<>(Set.of(1)));
        assertEquals(expected, PaperFigure1.masks(result), "masked positions per transaction");
        assertEquals(5, result.maskCount(), "Figure 4(b): distance of DBSH");
    }

    /**
     * A position of the chosen symbol that lies on no complete path is skipped, not removed.
     * Constructed: T = acbcab with no gap allowed, sensitive &lt;a,b&gt;; its only occurrence is
     * (4, 5). Cost ties a and b (two occurrences each) and takes a, which occurs first; the a at
     * position 0 is followed by c, so removing it would change nothing. DBSH masks position 4 only.
     */
    @Test
    @DisplayName("positions of the chosen symbol off every complete path are skipped")
    void offPathPositionsAreSkipped() {
        java.util.function.Function<String, int[]> ev =
                w -> w.chars().map(c -> "abc".indexOf(c)).toArray();
        SequenceDatabase database = new SequenceDatabase(
                List.of(new hidenosp.core.EventSequence(0, ev.apply("acbcab"))));
        HideProblem problem = new HideProblem(database, List.of(new Pattern(ev.apply("ab"))),
                new hidenosp.core.GapConstraint(0, 0), HideProblem.Mode.HIDE_SEQ, 1, 0.5, 2, 1);
        Sanitization result = new Dbsh().run(problem);
        assertEquals(Set.of(4), result.maskedIn(0));
    }

    @Test
    @DisplayName("Example 1: Cost on T7 with s3 and s4 is 2, choosing d then f")
    void example1Cost() {
        SequenceDatabase database = PaperFigure1.database();
        // s3 = <d,h> and s4 = <f,h> are entries 2 and 3 of the sensitive list.
        SymbolCover.Cover cover = SymbolCover.of(database.get(6), List.of(2, 3), PaperFigure1.SENSITIVE);
        assertEquals(2, cover.cost());
        assertEquals(List.of(List.of(0), List.of(1)), cover.positions(), "d at 0, then f at 1");
    }

    /**
     * Algorithm 1 divides the weight of a symbol by the number of patterns it covers. Constructed:
     * T = eabec with &lt;e,a&gt;, &lt;e,b&gt;, &lt;e,c&gt;; e occurs twice but covers all three
     * patterns (ratio 2/3), a, b and c once each (ratio 1), so Cost takes e alone and returns 2.
     */
    @Test
    @DisplayName("Cost weighs a symbol by occurrences per covered pattern")
    void costDividesByCoveredPatterns() {
        java.util.function.Function<String, int[]> ev =
                w -> w.chars().map(c -> "eabc".indexOf(c)).toArray();
        List<Pattern> sensitive = List.of(new Pattern(ev.apply("ea")), new Pattern(ev.apply("eb")),
                new Pattern(ev.apply("ec")));
        SymbolCover.Cover cover = SymbolCover.of(
                new hidenosp.core.EventSequence(0, ev.apply("eabec")), List.of(0, 1, 2), sensitive);
        assertEquals(2, cover.cost());
        assertEquals(List.of(Set.of(0, 1, 2)), cover.subsets());
    }

    @Test
    @DisplayName("Figure 4(b): the side effects of DBSH are <a,c>, <a,e>, <d,f>, <d,g>, <f,g>")
    void figure4SideEffects() {
        SequenceDatabase database = PaperFigure1.database();
        Map<Pattern, Long> nonSensitive =
                PaperFigure1.nonSensitive(PaperFigure1.frequent(database, PaperFigure1.PSI));
        SequenceDatabase after = new Dbsh(SupportCount.SEQUENCE_COUNT).run(PROBLEM).sanitized();
        assertEquals(new TreeSet<>(List.of("<a,c>", "<a,e>", "<d,f>", "<d,g>", "<f,g>")),
                PaperFigure1.lost(nonSensitive, after));
    }
}
