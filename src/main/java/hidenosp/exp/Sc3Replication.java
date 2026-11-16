// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.hide.HideProblem;
import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC3REP: the comparison of SC3 on the datasets added after the main comparison (preregistration section 23,
 * lot C), analysed apart from SC3. Same computation as SC3, own file prefix.
 */
public final class Sc3Replication implements Scenario {

    @Override
    public String id() {
        return "SC3REP";
    }

    @Override
    public String title() {
        return "the comparison of SC3 on the replication datasets";
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
