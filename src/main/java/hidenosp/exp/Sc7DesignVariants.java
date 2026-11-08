// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC7 of section 11.1: the design variants of CUT-HIDE, each changing exactly one factor.
 *
 * <ul>
 *   <li>uniform weights against the vital-node weights of step 4;</li>
 *   <li>the two natural heuristics H1 and H2 of P6(c) against the lambda sweep, and the sweep over
 *       weight values against the sweep with exact maximisation;</li>
 *   <li>the dynamic program of P7 against the greedy walk over marginal costs;</li>
 *   <li>a weighted minimum cut against an arbitrary minimum cut.</li>
 * </ul>
 *
 * <p>These variants change the output, so the comparison is at equal masking budget and is called a
 * comparison of design variants, not an ablation that keeps the result set fixed (rule 2.3.8). The
 * scenario also reports the share of cost pairs certified optimal, which is a quantity of the
 * paper in its own right.
 *
 * <p>TODO(code): implement using {@link hidenosp.hide.CutHideConfig} for the first, third and
 * fourth rows, and add H1 and H2 as explicit alternatives to the sweep for the second.
 */
public final class Sc7DesignVariants implements Scenario {

    @Override
    public String id() {
        return "SC7";
    }

    @Override
    public String title() {
        return "design variants at equal budget: weights, lambda sweep, allocation, cut choice";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "epsilon", "seed"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        throw new UnsupportedOperationException("SC7 is not implemented yet");
    }
}
