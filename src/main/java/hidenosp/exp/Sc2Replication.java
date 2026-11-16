// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC2REP: the data regime of SC2 for the datasets added after the main comparison (preregistration section 23,
 * lot C). Same computation as SC2; its own file prefix keeps these rows out of the locked group tables of SC2.
 */
public final class Sc2Replication implements Scenario {

    @Override
    public String id() {
        return "SC2REP";
    }

    @Override
    public String title() {
        return "data regime of SC2 for the replication datasets";
    }

    @Override
    public String[] options() {
        return new Sc2DataRegime().options();
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        new Sc2DataRegime().runAs(id(), sink, options);
    }
}
