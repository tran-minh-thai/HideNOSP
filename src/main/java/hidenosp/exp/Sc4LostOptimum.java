// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.hide.HideProblem;
import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC4LOST: the exact Lost optimum of SC3LOST under HIDE-SEQ. The windows, the strata, the seeds and the
 * instance threshold sigma_I = sup_NO(P, I) - drop + 1, which defines F_sigma_I(I), are those of SC3LOST;
 * the hiding constraint is sup_NO(P, S) &lt; r in every window, for every r of {@code --rs}. Every minimum
 * hiding set is enumerated ({@link hidenosp.baseline.IlpExact#solveExactly}) and every method is run and
 * evaluated as in SC3LOST. See {@link Sc3LostOptimum#runLost}.
 */
public final class Sc4LostOptimum implements Scenario {

    @Override
    public String id() {
        return "SC4LOST";
    }

    @Override
    public String title() {
        return "exact Lost optimum under HIDE-SEQ on small instances cut from real data";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "maxPatternLength", "epsilon",
                "setsPerStratum", "seed", "patternKind", "window", "carrying", "without", "drops", "rs",
                "instanceSeed", "methods", "closure"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        Sc3LostOptimum.runLost(id(), sink, options, HideProblem.Mode.HIDE_SEQ);
    }
}
