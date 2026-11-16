// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.hide.HideProblem;
import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC3ROB: the comparison of SC3 run again, and the support of every sensitive pattern on each masked database
 * under wider gap constraints, in SC3ROBSUP_&lt;stamp&gt;.csv (preregistration section 23, lot F). The comparison
 * rows must equal those of SC3 for the same cell: the methods are deterministic.
 */
public final class Sc3Robustness implements Scenario {

    @Override
    public String id() {
        return "SC3ROB";
    }

    @Override
    public String title() {
        return "the comparison of SC3 and the support of each sensitive pattern under wider gap constraints";
    }

    @Override
    public String[] options() {
        return new Sc3MainComparison().options();
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        Sc3MainComparison.runComparison(id(), sink, options, HideProblem.Mode.HIDE_DB, true);
    }
}
