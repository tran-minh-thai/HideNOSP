package hidenosp.baseline;

import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Algorithm 3 (Exclude) of Gkoulalas-Divanis and Loukides, KDD 2011, and the post-condition shared
 * by DBSH and SBSH.
 *
 * <p>The sequences that support a sensitive pattern are visited in the order the caller fixes (by
 * Cost, descending, for DBSH; Exclude-SBSH puts another key first). A sequence is left intact "only
 * if every sensitive sequence supported by T has been encountered fewer than (psi - 1) times"
 * (Section 5.1). The count M(s) grows with every sequence visited, sanitised or not; that is the
 * reading that reproduces Figure 1(e), where T8 is sanitised although T5 already was.
 *
 * <p>Under the transfer to sup_NO a sequence brings sup_NO(s, T) units instead of one, so it is left
 * intact when M(s) + sup_NO(s, T) &le; sigma - 1 for every supported s, and M(s) grows by
 * sup_NO(s, T). With the sequence count this is exactly the rule of the paper.
 */
final class Exclude {

    private Exclude() {
    }

    /**
     * @param order the visiting order
     * @param supported for each sequence, the indices of the sensitive patterns it supports
     * @param units units[p][s]: what sequence s brings to the support of sensitive pattern p
     * @param threshold psi, or sigma after the transfer
     * @return the sequences to sanitise, in visiting order
     */
    static List<Integer> toSanitise(List<Integer> order, Map<Integer, List<Integer>> supported,
                                    long[][] units, long threshold) {
        long[] encountered = new long[units.length];
        long budget = threshold - 1;
        List<Integer> out = new ArrayList<>();
        for (int s : order) {
            boolean keep = true;
            for (int p : supported.get(s)) {
                keep &= encountered[p] + units[p][s] <= budget;
            }
            if (!keep) {
                out.add(s);
            }
            for (int p : supported.get(s)) {
                encountered[p] += units[p][s];
            }
        }
        return out;
    }

    /** Every sensitive pattern below the threshold after sanitising; a failure is a bug. */
    static void verify(HideProblem problem, Sanitization result, SupportCount support, String name) {
        SequenceDatabase after = result.sanitized();
        for (Pattern p : problem.sensitive()) {
            if (problem.isDatabaseMode()) {
                long total = 0;
                for (int s = 0; s < after.size(); s++) {
                    total += support.unit(SupportNO.of(p, after.get(s), problem.gap()));
                }
                if (total >= problem.threshold()) {
                    throw new IllegalStateException(name + " left the support of " + p + " at "
                            + total + ", threshold " + problem.threshold());
                }
            } else {
                for (int s = 0; s < after.size(); s++) {
                    int here = SupportNO.of(p, after.get(s), problem.gap());
                    if (here >= problem.threshold()) {
                        throw new IllegalStateException(name + " left sup_NO(" + p + ") = " + here
                                + " in sequence " + s + ", limit " + problem.threshold());
                    }
                }
            }
        }
    }
}
