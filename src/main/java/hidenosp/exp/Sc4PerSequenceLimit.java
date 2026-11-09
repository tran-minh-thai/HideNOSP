// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.hide.HideProblem;
import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC4: the comparison of SC3 under HIDE-SEQ, the per-sequence limit r on how often one individual may
 * repeat the sensitive behaviour.
 *
 * <p>Everything except the problem is the one of SC3 ({@link Sc3MainComparison#runComparison}): the
 * sensitive sets are drawn from the same F_sigma(D) with the same strata and seed, the methods are built by
 * the same factory, and the same evaluator counts Lost and SupDist against F_sigma(D), so sigma only
 * defines F_sigma(D) here. The hiding constraint is sup_NO(P, S) &lt; r in every sequence, so the methods
 * have no sequence to choose: every sequence at or above r is sanitised. The least number of masks is the
 * fixed sum over the sequences of OPT_S(r - 1) (Theorem 1 for distinct items, Theorem 2(b) or its
 * corollary otherwise), written as {@code min_masks} for one sensitive pattern.
 *
 * <p>The result file carries the line {@code mode HIDE_SEQ r=<r>} in its header. An instance in which no
 * sequence reaches r needs no mask; it is written with the status {@code no sequence reaches r} and counted
 * apart, never as a tie.
 */
public final class Sc4PerSequenceLimit implements Scenario {

    @Override
    public String id() {
        return "SC4";
    }

    @Override
    public String title() {
        return "the main comparison under HIDE-SEQ: Lost, SupDist and |R| at a per-sequence limit r";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "r", "maxPatternLength",
                "epsilon", "setsPerStratum", "sensitiveSize", "seed", "methodSeed", "repeats",
                "methods", "patternKind", "timeLimit", "closure"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        Sc3MainComparison.runComparison(id(), sink, options, HideProblem.Mode.HIDE_SEQ);
    }
}
