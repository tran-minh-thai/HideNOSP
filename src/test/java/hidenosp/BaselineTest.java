package hidenosp;

import hidenosp.baseline.Dbsh;
import hidenosp.baseline.Hha;
import hidenosp.baseline.HidingMethod;
import hidenosp.baseline.IlpExact;
import hidenosp.baseline.OccurrenceCountHeuristic;
import hidenosp.baseline.RandomMasking;
import hidenosp.baseline.Sbsh;
import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.metrics.DataProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every hiding method has to satisfy, whatever else it does.
 *
 * <p>The post-condition is the same for all of them, so it belongs here and not inside each class:
 * after sanitising, no sensitive pattern may still meet the threshold. HF is zero by design, which
 * is why the design doc says it is stated once and never discussed again; a method that fails it is
 * broken, not merely worse.
 *
 * <p>The exact method is pinned against proposition P1: for a single pattern with distinct items
 * the least number of masks is forced, so an exact search has to find exactly that many.
 */
class BaselineTest {

    private static final int A = 0;
    private static final int B = 1;
    private static final int C = 2;

    /** A small database with visible repetition, small enough for the exact search. */
    private static SequenceDatabase smallDatabase() {
        Random random = new Random(11);
        List<EventSequence> sequences = new ArrayList<>();
        for (int s = 0; s < 5; s++) {
            int[] events = new int[8];
            for (int i = 0; i < events.length; i++) {
                events[i] = random.nextInt(3);
            }
            events[1] = A;
            events[3] = B;
            sequences.add(new EventSequence(s, events));
        }
        return new SequenceDatabase(sequences);
    }

    private static HideProblem problem(SequenceDatabase database, int sigma) {
        return new HideProblem(database, List.of(new Pattern(A, B)), new GapConstraint(0, 2),
                HideProblem.Mode.HIDE_DB, sigma, 0.5, 3);
    }

    /**
     * CUT-HIDE o bien the CO TRONG SO, kem B_eps that su lay tu F_sigma. Phai qua day chu khong
     * goi thang CutHideConfig.standard(): khong cung cap B_eps thi lop do bao loi, dung theo thiet
     * ke, vi mot lan chay nhu vay se la bien the trong so deu duoi ten bien the co trong so.
     */
    private static CutHide weighted(HideProblem problem) {
        long sigma = problem.frequentThreshold();
        var frequent = hidenosp.metrics.FrequentPatterns.mine(
                problem.database(), problem.gap(), problem.maxPatternLength(), sigma);
        var closure = hidenosp.metrics.FrequentPatterns.closure(problem.sensitive(), frequent.keySet());
        for (double eps : new double[] {0.5, 1.0, 2.0, 5.0}) {
            var band = hidenosp.metrics.FrequentPatterns.nearThreshold(frequent, sigma, eps, closure);
            if (!band.isEmpty()) {
                double[] factor =
                        hidenosp.metrics.FrequentPatterns.weightFactors(band, frequent, sigma);
                return new CutHide(CutHideConfig.standard()).withNearThreshold(band, factor);
            }
        }
        throw new IllegalStateException("khong dung duoc B_eps cho thuc the thu nghiem nay");
    }

    private static List<HidingMethod> methods() {
        return List.of(
                new CutHide(CutHideConfig.uniformWeights()),
                new OccurrenceCountHeuristic(),
                new Hha(Hha.Delta.PATH_COUNTS),
                new Hha(Hha.Delta.ORIGINAL_DP),
                new Dbsh(),
                new Sbsh(1),
                new RandomMasking(RandomMasking.SequenceChoice.HEURISTIC,
                        RandomMasking.PositionChoice.HEURISTIC, 7),
                new RandomMasking(RandomMasking.SequenceChoice.RANDOM,
                        RandomMasking.PositionChoice.RANDOM, 7));
    }

    @Test
    @DisplayName("every method reaches HF = 0 under a database threshold")
    void postConditionHolds() {
        SequenceDatabase database = smallDatabase();
        HideProblem problem = problem(database, 6);
        Pattern sensitive = problem.sensitive().get(0);
        List<HidingMethod> all = new ArrayList<>(methods());
        all.add(weighted(problem));
        for (HidingMethod method : all) {
            Sanitization sanitization = method.run(problem);
            long after = SupportNO.of(sensitive, sanitization.sanitized(), problem.gap());
            assertTrue(after < problem.threshold(),
                    method.name() + " left sup_NO = " + after + ", threshold " + problem.threshold());
        }
    }

    @Test
    @DisplayName("the exact search uses exactly the number of masks P1 predicts")
    void exactMatchesTheBound() {
        SequenceDatabase database = smallDatabase();
        HideProblem problem = problem(database, 6);
        long predicted = DataProfile
                .of(database, problem.sensitive().get(0), problem.gap(), HideProblem.Mode.HIDE_DB, 6)
                .minMasks();
        Sanitization exact = new IlpExact().run(problem);
        assertEquals(predicted, exact.maskCount(),
                "P1 forces the number of masks for a distinct-item pattern");
    }

    @Test
    @DisplayName("CUT-HIDE attains the bound of P1, the heuristics never beat it")
    void cutHideAttainsTheBound() {
        SequenceDatabase database = smallDatabase();
        HideProblem problem = problem(database, 6);
        long predicted = DataProfile
                .of(database, problem.sensitive().get(0), problem.gap(), HideProblem.Mode.HIDE_DB, 6)
                .minMasks();
        List<HidingMethod> all = new ArrayList<>(methods());
        all.add(weighted(problem));
        for (HidingMethod method : all) {
            long used = method.run(problem).maskCount();
            assertTrue(used >= predicted,
                    method.name() + " used " + used + " masks, fewer than the optimum " + predicted);
        }
        assertEquals(predicted, weighted(problem).run(problem).maskCount(),
                "CUT-HIDE is exact for one distinct-item pattern");
    }

    @Test
    @DisplayName("HHA computes the same delta both ways")
    void deltaAgrees() {
        SequenceDatabase database = smallDatabase();
        HideProblem problem = problem(database, 6);
        Sanitization viaCounts = new Hha(Hha.Delta.PATH_COUNTS).run(problem);
        Sanitization viaDp = new Hha(Hha.Delta.ORIGINAL_DP).run(problem);
        assertEquals(viaCounts.masked().toString(), viaDp.masked().toString(),
                "the per-position dynamic program of [R20] and the path counts must agree");
    }

    @Test
    @DisplayName("the per-sequence limit is met in every sequence")
    void perSequenceLimit() {
        SequenceDatabase database = smallDatabase();
        HideProblem problem = new HideProblem(database, List.of(new Pattern(A, B)),
                new GapConstraint(0, 2), HideProblem.Mode.HIDE_SEQ, 1, 0.5, 3, 6);
        for (HidingMethod method : List.of(weighted(problem), new Hha(), new Dbsh())) {
            SequenceDatabase after = method.run(problem).sanitized();
            for (int s = 0; s < after.size(); s++) {
                assertEquals(0, SupportNO.of(problem.sensitive().get(0), after.get(s), problem.gap()),
                        method.name() + " left support in sequence " + s);
            }
        }
    }

    @Test
    @DisplayName("masking never creates a pattern, whatever the method")
    void noGhostPatterns() {
        SequenceDatabase database = smallDatabase();
        HideProblem problem = problem(database, 6);
        Pattern[] watched = {new Pattern(A), new Pattern(B), new Pattern(C),
                new Pattern(B, A), new Pattern(A, C), new Pattern(C, B, A)};
        List<HidingMethod> all = new ArrayList<>(methods());
        all.add(weighted(problem));
        for (HidingMethod method : all) {
            SequenceDatabase after = method.run(problem).sanitized();
            for (Pattern q : watched) {
                assertTrue(SupportNO.of(q, after, problem.gap()) <= SupportNO.of(q, database, problem.gap()),
                        method.name() + " raised the support of " + q + ", contradicting P5(a)");
            }
        }
    }

    @Test
    @DisplayName("nhieu mau nhay cam: moi baseline tang (a) va (b) van dat HF = 0")
    void multiplePatternsUnderDatabaseThreshold() {
        SequenceDatabase database = smallDatabase();
        List<Pattern> sensitive = List.of(new Pattern(A, B), new Pattern(B, C));
        HideProblem problem = new HideProblem(database, sensitive, new GapConstraint(0, 2),
                HideProblem.Mode.HIDE_DB, 4, 0.5, 3);
        // CUT-HIDE duoi nguong CSDL voi nhieu mau van chua cai (LP lam tron), nen khong co o day.
        List<HidingMethod> methods = List.of(
                new OccurrenceCountHeuristic(),
                new Hha(),
                new Dbsh(),
                new Sbsh(1),
                new RandomMasking(RandomMasking.SequenceChoice.HEURISTIC,
                        RandomMasking.PositionChoice.HEURISTIC, 7),
                new RandomMasking(RandomMasking.SequenceChoice.HEURISTIC,
                        RandomMasking.PositionChoice.RANDOM, 7),
                new RandomMasking(RandomMasking.SequenceChoice.RANDOM,
                        RandomMasking.PositionChoice.HEURISTIC, 7),
                new RandomMasking(RandomMasking.SequenceChoice.RANDOM,
                        RandomMasking.PositionChoice.RANDOM, 7));
        for (HidingMethod method : methods) {
            SequenceDatabase after = method.run(problem).sanitized();
            for (Pattern p : sensitive) {
                long support = SupportNO.of(p, after, problem.gap());
                assertTrue(support < problem.threshold(),
                        method.name() + " left sup_NO(" + p + ") = " + support);
            }
        }
    }

    @Test
    @DisplayName("nhieu mau nhay cam: gioi han tung chuoi duoc thoa, ke ca CUT-HIDE")
    void multiplePatternsUnderPerSequenceLimit() {
        SequenceDatabase database = smallDatabase();
        List<Pattern> sensitive = List.of(new Pattern(A, B), new Pattern(B, C));
        HideProblem problem = new HideProblem(database, sensitive, new GapConstraint(0, 2),
                HideProblem.Mode.HIDE_SEQ, 1, 0.5, 3, 6);
        for (HidingMethod method : List.of(new CutHide(CutHideConfig.uniformWeights()),
                new OccurrenceCountHeuristic(), new Hha(), new Dbsh(),
                new RandomMasking(RandomMasking.SequenceChoice.RANDOM,
                        RandomMasking.PositionChoice.RANDOM, 7))) {
            SequenceDatabase after = method.run(problem).sanitized();
            for (Pattern p : sensitive) {
                for (int s = 0; s < after.size(); s++) {
                    assertEquals(0, SupportNO.of(p, after.get(s), problem.gap()),
                            method.name() + " left support of " + p + " in sequence " + s);
                }
            }
        }
    }

    @Test
    @DisplayName("B_eps rong thi CUT-HIDE co trong so phai BAO LOI, khong tut ve trong so deu")
    void emptyBandIsAnError() {
        SequenceDatabase database = smallDatabase();
        HideProblem problem = problem(database, 6);
        // cau hinh co trong so nhung khong ai cung cap B_eps
        CutHide weighted = new CutHide(CutHideConfig.standard());
        IllegalStateException thrown =
                org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                        () -> weighted.run(problem));
        assertTrue(thrown.getMessage().contains("B_eps is empty"), thrown.getMessage());
        // bien the trong so deu thi van chay binh thuong
        assertTrue(new CutHide(CutHideConfig.uniformWeights()).run(problem).maskCount() > 0);
    }
}
