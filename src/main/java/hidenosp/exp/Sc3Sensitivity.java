// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.hide.HideProblem;
import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC3SENS: the comparison of SC3, run for preregistration section 23, lots A (uniform weights) and B (one constant
 * of CUT-HIDE changed at a time, method keys cut-hide-rw-ls@...). Same computation as SC3; its own file prefix
 * keeps these rows out of every table of SC3.
 */
public final class Sc3Sensitivity implements Scenario {

    @Override
    public String id() {
        return "SC3SENS";
    }

    @Override
    public String title() {
        return "the comparison of SC3 for the stage ablation and the sensitivity of the constants of CUT-HIDE";
    }

    @Override
    public String[] options() {
        return new Sc3MainComparison().options();
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        Sc3MainComparison.runComparison(id(), sink, options, HideProblem.Mode.HIDE_DB, false);
    }
}
