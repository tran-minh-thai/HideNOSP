package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HHA against its published examples.
 *
 * <p>Example 2 of Abul et al. (ICDE Workshops 2007, repeated in TKDE 2010): S = abc, T = aabccbae,
 * delta(T[1]) = 2, delta(T[2]) = 2, delta(T[3]) = 4, and marking T[3] removes every matching.
 *
 * <p>Figure 1(d) of Gkoulalas-Divanis and Loukides (KDD 2011) prints the output of HHA on their
 * running example with psi = 3 in their convention (support below psi), i.e. the disclosure
 * threshold 2 of Abul et al.; Figure 4(b) gives its distance (7) and side effects. That example is
 * what fixes the direction of the global heuristic (ascending, the largest sequences kept) and the
 * leftmost tie-break of the local one.
 */
class HhaPaperExampleTest {

    @Test
    @DisplayName("Example 2: delta = 2, 2, 4 on aabccbae, and one mark at T[3] sanitises it")
    void example2() {
        EventSequence t = new EventSequence(0, PaperFigure1.events("aabccbae"));
        Pattern s = PaperFigure1.pattern("abc");
        NettreeCutIndex index = new NettreeCutIndex(t, PaperFigure1.NO_MAXIMUM_GAP);
        index.add(s, true);
        assertEquals(2, index.occurrencesThrough(0, 0), "delta(T[1])");
        assertEquals(2, index.occurrencesThrough(0, 1), "delta(T[2])");
        assertEquals(4, index.occurrencesThrough(0, 2), "delta(T[3])");
        HideProblem problem = new HideProblem(new SequenceDatabase(List.of(t)), List.of(s),
                PaperFigure1.NO_MAXIMUM_GAP, HideProblem.Mode.HIDE_DB, 1, 0.5, 3);
        for (Hha.Delta delta : Hha.Delta.values()) {
            Sanitization result = new Hha(delta, SupportCount.SEQUENCE_COUNT).run(problem);
            assertEquals(Set.of(2), result.maskedIn(0), "the mark of Example 2, " + delta);
        }
    }

    private static final HideProblem FIGURE1 = new HideProblem(PaperFigure1.database(),
            PaperFigure1.SENSITIVE, PaperFigure1.NO_MAXIMUM_GAP, HideProblem.Mode.HIDE_DB,
            PaperFigure1.PSI, 0.5, 3);

    @Test
    @DisplayName("Figure 1(d) of the KDD 2011 paper: HHA masks exactly the printed events")
    void figure1Output() {
        Sanitization result = new Hha(Hha.Delta.PATH_COUNTS, SupportCount.SEQUENCE_COUNT).run(FIGURE1);
        // Figure 1(d), transactions numbered from 1 in the paper and from 0 here: T1 = *bccde,
        // T2 = *bce, T3 = *ceb, T6 = *hb, T8 = c*fg, T9 = **hfg; T4, T5 and T7 untouched.
        Map<Integer, Set<Integer>> expected = new TreeMap<>();
        expected.put(0, new TreeSet<>(Set.of(0)));
        expected.put(1, new TreeSet<>(Set.of(0)));
        expected.put(2, new TreeSet<>(Set.of(0)));
        expected.put(5, new TreeSet<>(Set.of(0)));
        expected.put(7, new TreeSet<>(Set.of(1)));
        expected.put(8, new TreeSet<>(Set.of(0, 1)));
        assertEquals(expected, PaperFigure1.masks(result), "masked positions per transaction");
        assertEquals(7, result.maskCount(), "Figure 4(b): distance of HHA");
    }

    @Test
    @DisplayName("Figure 4(b): the side effects of HHA are <a>, <a,b>, <a,c>, <a,e>, <d,f>, <d,g>")
    void figure4SideEffects() {
        SequenceDatabase database = PaperFigure1.database();
        Map<Pattern, Long> nonSensitive =
                PaperFigure1.nonSensitive(PaperFigure1.frequent(database, PaperFigure1.PSI));
        SequenceDatabase after =
                new Hha(Hha.Delta.ORIGINAL_DP, SupportCount.SEQUENCE_COUNT).run(FIGURE1).sanitized();
        assertEquals(new TreeSet<>(List.of("<a>", "<a,b>", "<a,c>", "<a,e>", "<d,f>", "<d,g>")),
                PaperFigure1.lost(nonSensitive, after));
    }

    /**
     * Under sup_NO a kept sequence costs its largest sup_NO over the sensitive patterns. Constructed:
     * one pattern ab, sequences with 3, 2 and 1 non-overlapping occurrences and matching sets of
     * 6, 3 and 1 (ababab has six embeddings), sigma = 4. The budget 3 keeps ababab alone (cost 3);
     * the other two are sanitised although dropping one occurrence would already have been enough.
     */
    @Test
    @DisplayName("the sup_NO transfer keeps the largest sequences within sigma - 1")
    void transferKeepsLargestWithinBudget() {
        java.util.function.Function<String, int[]> ev =
                w -> w.chars().map(c -> "ab".indexOf(c)).toArray();
        SequenceDatabase database = new SequenceDatabase(List.of(
                new EventSequence(0, ev.apply("ab")), new EventSequence(1, ev.apply("abab")),
                new EventSequence(2, ev.apply("ababab"))));
        HideProblem problem = new HideProblem(database, List.of(new Pattern(ev.apply("ab"))),
                GapConstraint.unbounded(0), HideProblem.Mode.HIDE_DB, 4, 0.5, 2);
        Sanitization result = new Hha().run(problem);
        assertEquals(Set.of(), result.maskedIn(2), "the largest sequence is kept");
        assertEquals(1, result.maskedIn(0).size());
        assertEquals(2, result.maskedIn(1).size());
    }
}
