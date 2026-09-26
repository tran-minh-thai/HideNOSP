package hidenosp.metrics;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The side-effect measures of design doc section 2, shared by every method and every scenario
 * (rule 2.1.7).
 *
 * <ul>
 *   <li>HF: the share of sensitive patterns that still meet the threshold after sanitizing. Zero by
 *       construction for CUT-HIDE, which is why the design doc says it is stated once and not
 *       discussed again.</li>
 *   <li>Lost: the patterns of F_sigma(D) outside S-plus that leave F_sigma(D-prime).</li>
 *   <li>Ghost: the patterns of F_sigma(D-prime) that were not in F_sigma(D). Identically zero for
 *       masking by P5(a), so only the deletion arm of SC5 computes it.</li>
 *   <li>DistR: |R| divided by the total length of D.</li>
 *   <li>SupDist: the relative loss of support summed over F_sigma(D) minus S-plus.</li>
 * </ul>
 *
 * <p>The names Lost and Ghost are used deliberately instead of MC and AC, which the survey [R28]
 * defines over utility; the mapping is noted in the paper, not encoded here.
 *
 * <h2>Why this class is built around a {@link Context}</h2>
 *
 * <p>A sweep evaluates the same configuration (dataset, sigma, gap) under many methods and many
 * sensitive sets. Mining F_sigma(D) once per evaluation made that unaffordable: one instance of one
 * method on MSNBC took 75 seconds, of which about 40 were two minings, and the locked grid is some
 * 96000 method-runs. A {@code Context} mines F_sigma(D) once and every evaluation reuses it.
 *
 * <p>The second mining, of F_sigma(D-prime), is gone entirely for masking. It existed only to
 * count Ghost, and P5(a) proves Ghost is identically zero there; the preregistration says so and
 * says not to report it. Paying a mining per run to recompute a number known to be zero is the
 * kind of cost that hides in a design until someone multiplies it out.
 *
 * <p>What remains is exact, not approximate. Masking a position can only lower support, and only
 * in the sequence it touches, so
 *
 * <pre>sup_NO(Q, D') = sup_NO(Q, D) - sum over masked sequences s of (sup_NO(Q, S_s) - sup_NO(Q, S'_s))</pre>
 *
 * and only the masked sequences are visited. The per-sequence supports of D are cached, because
 * different methods keep masking the same busy sequences.
 */
public final class Metrics {

    /** One row of results. Every field is invariant across machines. */
    public record Row(double hidingFailure, int lost, int ghost, double distR, double supDist,
                      int maskCount, int frequentBefore, int frequentAfter,
                      int nonSensitiveBefore) {

        /**
         * Lost as a share of what could have been lost, which is the quantity H2 is stated on.
         *
         * <p>The denominator is |F_sigma(D) \ S+|, the frequent patterns that are neither
         * sensitive nor in their closure, because those are the only ones Lost ever counts. It is
         * not {@link #frequentBefore()}: that includes the closure, so dividing by it would shrink
         * every ratio by a different amount on every instance.
         */
        public double lostRelative() {
            return nonSensitiveBefore == 0 ? 0 : (double) lost / nonSensitiveBefore;
        }
    }

    private Metrics() {
    }

    /**
     * F_sigma(D) for one configuration, mined once and reused by every evaluation on it.
     *
     * <p>Build one per (dataset, sigma, gap, l_max) and keep it for the whole sweep over sensitive
     * sets and methods.
     */
    public static final class Context {

        private final SequenceDatabase database;
        private final GapConstraint gap;
        private final long sigma;
        private final int maxLength;
        private final Map<Pattern, Long> frequent;
        private final Map<Pattern, Map<Integer, Integer>> perSequence = new HashMap<>();

        private Context(SequenceDatabase database, GapConstraint gap, long sigma, int maxLength,
                        Map<Pattern, Long> frequent) {
            this.database = database;
            this.gap = gap;
            this.sigma = sigma;
            this.maxLength = maxLength;
            this.frequent = frequent;
        }

        /** Mines F_sigma(D). Do this once per configuration, never per evaluation. */
        public static Context of(SequenceDatabase database, GapConstraint gap, int maxLength,
                                 long sigma) {
            return new Context(database, gap, sigma, maxLength,
                    FrequentPatterns.mine(database, gap, maxLength, sigma));
        }

        /** Builds the context a problem implies. */
        public static Context of(HideProblem problem) {
            return of(problem.database(), problem.gap(), problem.maxPatternLength(),
                    problem.frequentThreshold());
        }

        public Map<Pattern, Long> frequent() {
            return frequent;
        }

        public long sigma() {
            return sigma;
        }

        /** sup_NO(Q, S_s) on the untouched database, cached: busy sequences are masked repeatedly. */
        private int supportIn(Pattern pattern, int sequenceIndex) {
            return perSequence
                    .computeIfAbsent(pattern, p -> new HashMap<>())
                    .computeIfAbsent(sequenceIndex,
                            s -> SupportNO.of(pattern, database.get(s), gap));
        }

        /**
         * Every measure for one masking sanitization. Ghost is zero by P5(a), not measured.
         *
         * @throws IllegalArgumentException if the threshold of the problem does not match the sigma
         *     this context was mined at, which would silently count Lost against the wrong set
         */
        public Row evaluateMasking(HideProblem problem, Sanitization sanitization) {
            return session(problem).evaluate(sanitization);
        }

        /**
         * An evaluator for many sanitizations of one problem, such as the vertices of a Pareto
         * front: it remembers the supports of every (sequence, masked positions) it has met, and
         * adjacent vertices differ in few sequences. The rows are those of
         * {@link #evaluateMasking}; only the work is shared.
         */
        public Session session(HideProblem problem) {
            if (problem.frequentThreshold() != sigma) {
                throw new IllegalArgumentException("this context was mined at sigma = " + sigma
                        + " but the problem counts side effects at sigma = "
                        + problem.frequentThreshold());
            }
            return new Session(problem);
        }

        /** See {@link #session(HideProblem)}. */
        public final class Session {

            private final HideProblem problem;
            private final Set<Pattern> closure;
            private final List<Pattern> watched = new ArrayList<>();
            private final List<Pattern> all;
            private final Map<Integer, Map<Set<Integer>, Map<Pattern, Integer>>> seen = new HashMap<>();

            private Session(HideProblem problem) {
                this.problem = problem;
                this.closure = FrequentPatterns.closure(problem.sensitive(), frequent.keySet());
                for (Pattern q : frequent.keySet()) {
                    if (!closure.contains(q)) {
                        watched.add(q);
                    }
                }
                // Mau nhay cam khong bao gio nam trong watched, vi bao dong da loai chung ra, nen
                // phai them vao day du co tan suat hay khong. Dieu kien cu la
                // "!frequent.containsKey(p)", tuc mau nhay cam nao co tan suat thi khong ai tinh
                // muc giam cua no, va HF luon ra 1 ke ca khi da an sach.
                this.all = new ArrayList<>(watched);
                for (Pattern p : problem.sensitive()) {
                    if (!all.contains(p)) {
                        all.add(p);
                    }
                }
            }

            /** The drop of every pattern of {@code all} in one masked sequence, remembered. */
            private Map<Pattern, Integer> dropIn(int s, Set<Integer> masked) {
                return seen.computeIfAbsent(s, k -> new HashMap<>()).computeIfAbsent(
                        Set.copyOf(masked), m -> {
                            EventSequence after = database.get(s).mask(m);
                            Map<Pattern, Integer> drop = new HashMap<>();
                            for (Pattern q : all) {
                                int before = supportIn(q, s);
                                if (before == 0) {
                                    continue;
                                }
                                int now = SupportNO.of(q, after, gap);
                                if (now != before) {
                                    drop.put(q, before - now);
                                }
                            }
                            return drop;
                        });
            }

            public Row evaluate(Sanitization sanitization) {
                Map<Pattern, Long> drop = new HashMap<>();
                for (Map.Entry<Integer, Set<Integer>> e : sanitization.masked().entrySet()) {
                    if (e.getValue().isEmpty()) {
                        continue;
                    }
                    for (Map.Entry<Pattern, Integer> d : dropIn(e.getKey(), e.getValue()).entrySet()) {
                        drop.merge(d.getKey(), (long) d.getValue(), Long::sum);
                    }
                }

                int lost = 0;
                long supportBefore = 0;
                long supportAfter = 0;
                for (Pattern q : watched) {
                    long before = frequent.get(q);
                    long now = before - drop.getOrDefault(q, 0L);
                    if (now < sigma) {
                        lost++;
                    }
                    supportBefore += before;
                    supportAfter += now;
                }

                int failed = 0;
                for (Pattern p : problem.sensitive()) {
                    long before = frequent.containsKey(p)
                            ? frequent.get(p)
                            : SupportNO.of(p, database, gap);
                    long now = before - drop.getOrDefault(p, 0L);
                    if (problem.isDatabaseMode()) {
                        if (now >= problem.threshold()) {
                            failed++;
                        }
                    } else if (exceedsPerSequenceLimit(problem, p, sanitization)) {
                        failed++;
                    }
                }
                double hidingFailure = problem.sensitive().isEmpty()
                        ? 0 : (double) failed / problem.sensitive().size();

                double distR = (double) sanitization.maskCount() / database.totalLength();
                double supDist = supportBefore == 0
                        ? 0 : (double) (supportBefore - supportAfter) / supportBefore;
                int frequentAfter = frequent.size() - lost - countClosureLost(closure, drop);
                return new Row(hidingFailure, lost, 0, distR, supDist, sanitization.maskCount(),
                        frequent.size(), frequentAfter, watched.size());
            }
        }

        private int countClosureLost(Set<Pattern> closure, Map<Pattern, Long> drop) {
            int lost = 0;
            for (Pattern q : closure) {
                Long before = frequent.get(q);
                if (before != null && before - drop.getOrDefault(q, 0L) < sigma) {
                    lost++;
                }
            }
            return lost;
        }

        private boolean exceedsPerSequenceLimit(HideProblem problem, Pattern pattern,
                                                Sanitization sanitization) {
            for (int s = 0; s < database.size(); s++) {
                Set<Integer> masked = sanitization.maskedIn(s);
                EventSequence sequence = masked.isEmpty()
                        ? database.get(s) : database.get(s).mask(masked);
                if (SupportNO.of(pattern, sequence, gap) >= problem.threshold()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * The deletion arm of SC5, where Ghost is the point: deletion can create occurrences once
         * the maximum gap is finite (P5(b)), so F_sigma has to be mined again on the modified
         * database. Only SC5 pays this.
         */
        public Row evaluateDeletion(HideProblem problem, Sanitization sanitization) {
            SequenceDatabase after = problem.database().delete(sanitization.masked());
            Map<Pattern, Long> frequentAfter =
                    FrequentPatterns.mine(after, gap, maxLength, sigma);
            Set<Pattern> closure = FrequentPatterns.closure(problem.sensitive(), frequent.keySet());
            int lost = 0;
            long supportBefore = 0;
            long supportAfter = 0;
            int watched = 0;
            for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
                if (closure.contains(e.getKey())) {
                    continue;
                }
                watched++;
                long now = frequentAfter.getOrDefault(e.getKey(),
                        SupportNO.of(e.getKey(), after, gap));
                if (now < sigma) {
                    lost++;
                }
                supportBefore += e.getValue();
                supportAfter += now;
            }
            int ghost = 0;
            for (Pattern q : frequentAfter.keySet()) {
                if (!frequent.containsKey(q)) {
                    ghost++;
                }
            }
            double hidingFailure = hidingFailure(problem, after);
            double distR = (double) sanitization.maskCount() / database.totalLength();
            double supDist = supportBefore == 0
                    ? 0 : (double) (supportBefore - supportAfter) / supportBefore;
            return new Row(hidingFailure, lost, ghost, distR, supDist, sanitization.maskCount(),
                    frequent.size(), frequentAfter.size(), watched);
        }
    }

    /**
     * Convenience for a single evaluation: builds a context, uses it once, throws it away. A sweep
     * must not call this, because it re-mines F_sigma(D) every time; build one
     * {@link Context} per configuration instead.
     */
    public static Row evaluate(HideProblem problem, Sanitization sanitization) {
        return Context.of(problem).evaluateMasking(problem, sanitization);
    }

    /** The share of sensitive patterns that still meet the threshold in D-prime. */
    public static double hidingFailure(HideProblem problem, SequenceDatabase after) {
        List<Pattern> sensitive = problem.sensitive();
        if (sensitive.isEmpty()) {
            return 0;
        }
        int failed = 0;
        for (Pattern p : sensitive) {
            if (problem.isDatabaseMode()) {
                if (SupportNO.of(p, after, problem.gap()) >= problem.threshold()) {
                    failed++;
                }
            } else {
                for (int s = 0; s < after.size(); s++) {
                    if (SupportNO.of(p, after.get(s), problem.gap()) >= problem.threshold()) {
                        failed++;
                        break;
                    }
                }
            }
        }
        return (double) failed / sensitive.size();
    }

    /**
     * The patterns lost, listed rather than counted. SC7 needs the list to compare two variants on
     * the same budget, not only their totals.
     */
    public static Set<Pattern> lostPatterns(HideProblem problem, SequenceDatabase after) {
        GapConstraint gap = problem.gap();
        long sigma = problem.frequentThreshold();
        Map<Pattern, Long> frequentBefore =
                FrequentPatterns.mine(problem.database(), gap, problem.maxPatternLength(), sigma);
        Set<Pattern> closure = FrequentPatterns.closure(problem.sensitive(), frequentBefore.keySet());
        Set<Pattern> lost = new LinkedHashSet<>();
        for (Pattern q : frequentBefore.keySet()) {
            if (!closure.contains(q) && SupportNO.of(q, after, gap) < sigma) {
                lost.add(q);
            }
        }
        return lost;
    }
}
