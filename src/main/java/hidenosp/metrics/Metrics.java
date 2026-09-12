package hidenosp.metrics;

import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

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
 *       masking by P5(a), so only the deletion arm of SC5 reports it.</li>
 *   <li>DistR: |R| divided by the total length of D.</li>
 *   <li>SupDist: the relative loss of support summed over F_sigma(D) minus S-plus.</li>
 * </ul>
 *
 * <p>The names Lost and Ghost are used deliberately instead of MC and AC, which the survey [R28]
 * defines over utility; the mapping is noted in the paper, not encoded here.
 */
public final class Metrics {

    /** One row of results. Every field is invariant across machines. */
    public record Row(double hidingFailure, int lost, int ghost, double distR, double supDist,
                      int maskCount, int frequentBefore, int frequentAfter) {
    }

    private Metrics() {
    }

    /**
     * Computes every measure for one sanitization. F_sigma is mined twice, once per database, which
     * dominates the cost; scenarios that sweep many sensitive sets should cache the first one.
     */
    public static Row evaluate(HideProblem problem, Sanitization sanitization) {
        SequenceDatabase before = problem.database();
        SequenceDatabase after = sanitization.sanitized();
        GapConstraint gap = problem.gap();
        int maxLength = problem.maxPatternLength();
        long sigma = problem.frequentThreshold();

        Map<Pattern, Long> frequentBefore = FrequentPatterns.mine(before, gap, maxLength, sigma);
        Map<Pattern, Long> frequentAfter = FrequentPatterns.mine(after, gap, maxLength, sigma);
        Set<Pattern> closure = FrequentPatterns.closure(problem.sensitive(), frequentBefore.keySet());

        double hidingFailure = hidingFailure(problem, after);

        int lost = 0;
        long supportBefore = 0;
        long supportAfter = 0;
        for (Map.Entry<Pattern, Long> e : frequentBefore.entrySet()) {
            if (closure.contains(e.getKey())) {
                continue;
            }
            long now = frequentAfter.getOrDefault(e.getKey(), SupportNO.of(e.getKey(), after, gap));
            if (now < sigma) {
                lost++;
            }
            supportBefore += e.getValue();
            supportAfter += now;
        }

        int ghost = 0;
        for (Pattern q : frequentAfter.keySet()) {
            if (!frequentBefore.containsKey(q)) {
                ghost++;
            }
        }

        double distR = (double) sanitization.maskCount() / before.totalLength();
        double supDist = supportBefore == 0 ? 0 : (double) (supportBefore - supportAfter) / supportBefore;
        return new Row(hidingFailure, lost, ghost, distR, supDist, sanitization.maskCount(),
                frequentBefore.size(), frequentAfter.size());
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
