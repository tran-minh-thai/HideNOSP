package hidenosp.hide;

import hidenosp.core.EventSequence;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.core.flow.NettreeFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The certified lower bound on the optimum Lost of proposition P12 of the design (preregistration
 * section 20.2), for one sensitive pattern P under HIDE-DB with g- = 0, where theorem P8 is exact.
 *
 * <p>For a non-sensitive frequent pattern Q, take in every sequence S carrying P one minimum cut C_S of
 * the Nettree of Q in S and weigh each position by the number of nodes of C_S on it. By lemma L8(a) the
 * drop of sup_NO(Q) under any mask set R is at least the total weight of R, so the least total weight
 * LB_Q of a minimum hiding set, computed exactly by the program of theorem P8, bounds the least drop
 * from below. Q has to fall by m_Q = sup_NO(Q, D) - sigma + 1 to be lost; when LB_Q >= m_Q it is lost in
 * every minimum hiding set. The bound is the number of such Q.
 *
 * <p>Only the patterns lost under a reference minimum hiding set are examined: a pattern that survives
 * one minimum set is not lost in all of them. The bound therefore never exceeds the optimum Lost, for any
 * reference, and a reference whose Lost equals the bound is certified optimal.
 */
public final class LostLowerBound {

    /** The bound, the patterns found lost in every minimum hiding set, and how many were examined. */
    public record Result(int bound, List<Pattern> doomed, int examined) {
    }

    private LostLowerBound() {
    }

    /**
     * @param watched  the non-sensitive frequent patterns outside the closure of P, with their support
     * @param sigma    the threshold below which a pattern is lost
     * @param reference a minimum hiding set of the problem
     */
    public static Result of(HideProblem problem, Map<Pattern, Long> watched, long sigma, Sanitization reference) {
        if (problem.sensitive().size() != 1 || !problem.isDatabaseMode() || !FrontierDp.applies(problem.gap())) {
            throw new IllegalArgumentException("the bound needs one sensitive pattern, HIDE-DB and g- = 0, where "
                    + "theorem P8 is exact");
        }
        Pattern p = problem.sensitive().get(0);
        SequenceDatabase database = problem.database();
        SequenceDatabase after = database.mask(reference.masked());
        boolean[] carries = new boolean[database.size()];
        for (int s = 0; s < database.size(); s++) {
            carries[s] = SupportNO.of(p, database.get(s), problem.gap()) > 0;
        }
        List<Pattern> doomed = new ArrayList<>();
        int examined = 0;
        for (Map.Entry<Pattern, Long> e : watched.entrySet()) {
            Pattern q = e.getKey();
            if (SupportNO.of(q, after, problem.gap()) >= sigma) {
                continue;
            }
            examined++;
            long needed = e.getValue() - sigma + 1;
            double[][] weights = new double[database.size()][];
            for (int s = 0; s < database.size(); s++) {
                EventSequence sequence = database.get(s);
                weights[s] = new double[sequence.length()];
                if (!carries[s]) {
                    continue;
                }
                Nettree nettree = Nettree.build(q, sequence, problem.gap());
                if (nettree.nodeCount() == 0) {
                    continue;
                }
                boolean[] cut = new NettreeFlow(nettree).minCut();
                for (int v = 0; v < cut.length; v++) {
                    if (cut[v]) {
                        weights[s][nettree.position(v)] += 1;
                    }
                }
            }
            Sanitization least = new CutHide(CutHideConfig.uniformWeights()).withExplicitWeights(weights).run(problem);
            if (least.maskCount() != reference.maskCount()) {
                throw new IllegalStateException("the weighted program masked " + least.maskCount() + " positions, the "
                        + "reference " + reference.maskCount() + "; both must be minimum hiding sets");
            }
            long weight = 0;
            for (Map.Entry<Integer, Set<Integer>> m : least.masked().entrySet()) {
                for (int position : m.getValue()) {
                    weight += (long) weights[m.getKey()][position];
                }
            }
            if (weight >= needed) {
                doomed.add(q);
            }
        }
        return new Result(doomed.size(), List.copyOf(doomed), examined);
    }
}
