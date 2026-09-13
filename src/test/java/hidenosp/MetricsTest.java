package hidenosp;

import hidenosp.baseline.Dbsh;
import hidenosp.baseline.Hha;
import hidenosp.baseline.HidingMethod;
import hidenosp.baseline.OccurrenceCountHeuristic;
import hidenosp.baseline.RandomMasking;
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

/**
 * The fast path of {@link Metrics.Context} must agree with the slow one, pattern for pattern.
 *
 * <p>This is the test that makes the optimisation safe to keep. Caching F_sigma(D) and visiting
 * only the masked sequences turned one evaluation on MSNBC from 75 seconds into something a sweep
 * can afford, but an optimisation that quietly changes a reported number is worse than a slow one.
 * So the incremental Lost and SupDist are checked here against the definition, computed by mining
 * F_sigma again on the sanitized database.
 */
class MetricsTest {

    private static final int A = 0;
    private static final int B = 1;
    private static final int C = 2;

    private static SequenceDatabase database(long seed) {
        Random random = new Random(seed);
        List<EventSequence> sequences = new ArrayList<>();
        for (int s = 0; s < 12; s++) {
            int[] events = new int[14];
            for (int i = 0; i < events.length; i++) {
                events[i] = random.nextInt(3);
            }
            sequences.add(new EventSequence(s, events));
        }
        return new SequenceDatabase(sequences);
    }

    /** Lost and SupDist straight from the definition of section 2, with no incremental shortcut. */
    private static int[] byDefinition(HideProblem problem, Sanitization sanitization) {
        SequenceDatabase after = sanitization.sanitized();
        long sigma = problem.frequentThreshold();
        Map<Pattern, Long> before = FrequentPatterns.mine(problem.database(), problem.gap(),
                problem.maxPatternLength(), sigma);
        Set<Pattern> closure = FrequentPatterns.closure(problem.sensitive(), before.keySet());
        int lost = 0;
        long supportBefore = 0;
        long supportAfter = 0;
        for (Map.Entry<Pattern, Long> e : before.entrySet()) {
            if (closure.contains(e.getKey())) {
                continue;
            }
            long now = SupportNO.of(e.getKey(), after, problem.gap());
            if (now < sigma) {
                lost++;
            }
            supportBefore += e.getValue();
            supportAfter += now;
        }
        return new int[] {lost, (int) supportBefore, (int) supportAfter};
    }

    @Test
    @DisplayName("Lost va SupDist gia tang trung khop voi dinh nghia, tren moi phuong phap")
    void incrementalAgreesWithTheDefinition() {
        for (long seed : new long[] {3, 11, 29}) {
            SequenceDatabase database = database(seed);
            for (int sigma : new int[] {8, 14}) {
                HideProblem problem = new HideProblem(database,
                        List.of(new Pattern(A, B), new Pattern(B, C)), new GapConstraint(0, 2),
                        HideProblem.Mode.HIDE_DB, sigma, 0.5, 3);
                Metrics.Context context = Metrics.Context.of(problem);
                for (HidingMethod method : List.of(new OccurrenceCountHeuristic(), new Hha(),
                        new Dbsh(), new RandomMasking(RandomMasking.SequenceChoice.RANDOM,
                                RandomMasking.PositionChoice.RANDOM, seed))) {
                    Sanitization sanitization = method.run(problem);
                    Metrics.Row fast = context.evaluateMasking(problem, sanitization);
                    int[] slow = byDefinition(problem, sanitization);
                    String where = method.name() + " seed " + seed + " sigma " + sigma;
                    assertEquals(slow[0], fast.lost(), "Lost khac dinh nghia: " + where);
                    double expected = slow[1] == 0 ? 0 : (double) (slow[1] - slow[2]) / slow[1];
                    assertEquals(expected, fast.supDist(), 1e-9, "SupDist khac dinh nghia: " + where);
                    assertEquals(0, fast.ghost(), "che khong bao gio sinh mau ma (P5a): " + where);
                }
            }
        }
    }

    @Test
    @DisplayName("HF gia tang trung khop voi dinh nghia, ke ca khi mau nhay cam co tan suat")
    void incrementalHidingFailureAgreesWithTheDefinition() {
        for (long seed : new long[] {3, 11, 29}) {
            SequenceDatabase database = database(seed);
            // sigma = 1 la ca dang so: moi mau nhay cam deu nam trong F_sigma(D). Loi cu bo qua
            // muc giam cua dung nhung mau do, nen HF luon ra 1 ke ca khi da an sach.
            for (int sigma : new int[] {1, 8, 14}) {
                HideProblem problem = new HideProblem(database,
                        List.of(new Pattern(A, B), new Pattern(B, C)), new GapConstraint(0, 2),
                        HideProblem.Mode.HIDE_DB, sigma, 0.5, 3);
                Metrics.Context context = Metrics.Context.of(problem);
                for (HidingMethod method : List.of(new OccurrenceCountHeuristic(), new Hha(),
                        new Dbsh(), new RandomMasking(RandomMasking.SequenceChoice.RANDOM,
                                RandomMasking.PositionChoice.RANDOM, seed))) {
                    Sanitization sanitization = method.run(problem);
                    Metrics.Row fast = context.evaluateMasking(problem, sanitization);
                    double slow = Metrics.hidingFailure(problem, sanitization.sanitized());
                    assertEquals(slow, fast.hidingFailure(), 1e-9,
                            "HF khac dinh nghia: " + method.name() + " seed " + seed
                                    + " sigma " + sigma);
                }
            }
        }
    }

    @Test
    @DisplayName("Context tu choi khi sigma cua no khac sigma dem tac dung phu cua bai toan")
    void contextRefusesAMismatchedSigma() {
        SequenceDatabase database = database(3);
        HideProblem eight = new HideProblem(database, List.of(new Pattern(A, B)),
                new GapConstraint(0, 2), HideProblem.Mode.HIDE_DB, 8, 0.5, 3);
        HideProblem fourteen = new HideProblem(database, List.of(new Pattern(A, B)),
                new GapConstraint(0, 2), HideProblem.Mode.HIDE_DB, 14, 0.5, 3);
        Metrics.Context context = Metrics.Context.of(eight);
        Sanitization sanitization = new Hha().run(fourteen);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> context.evaluateMasking(fourteen, sanitization));
    }
}
