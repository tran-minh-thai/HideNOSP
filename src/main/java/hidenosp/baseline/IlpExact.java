package hidenosp.baseline;

import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

/**
 * Tier (c) of section 11.3: the exact integer program, used on small instances to measure how far
 * CUT-HIDE sits from the optimum when several sensitive patterns make the problem NP-hard (L4a).
 *
 * <p>Model. One binary variable x_i per position of the database, with x_i = 1 when the position is
 * masked. For each sensitive pattern P and each sequence S, sup_NO must drop to the target, which
 * by lemma L0 means every family of node-disjoint paths of NT(P, S) larger than the target has to
 * be broken; the separation problem is one max flow, so the constraints are generated lazily rather
 * than written out. The objective is the number of masks first and the surrogate weight second, in
 * the lexicographic order of section 2.
 *
 * <p>The linear relaxation of the same model is what the rounding with the l_max guarantee of the
 * contribution list uses, so this class also has to expose the fractional optimum.
 *
 * <p>TODO(code): pick the solver and declare it in the protocol table. An external solver adds a
 * dependency and a version to declare; a small branch and bound over the cut structure of P2 keeps
 * the build self-contained. Decide before SC3.
 */
public final class IlpExact implements HidingMethod {

    @Override
    public String name() {
        return "ILP-exact";
    }

    @Override
    public Sanitization run(HideProblem problem) {
        throw new UnsupportedOperationException("the exact program is not implemented yet");
    }
}
