// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.hide.HideProblem;
import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC4R: the comparison of SC4 (HIDE-SEQ) at another per-sequence limit r (preregistration section 23, lot G: r = 3).
 * Same computation as SC4; its own file prefix keeps the rows of r = 2 and r = 3 apart.
 */
public final class Sc4OtherLimit implements Scenario {

    @Override
    public String id() {
        return "SC4R";
    }

    @Override
    public String title() {
        return "the comparison under HIDE-SEQ at a per-sequence limit other than that of SC4";
    }

    @Override
    public String[] options() {
        return new Sc4PerSequenceLimit().options();
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        Sc3MainComparison.runComparison(id(), sink, options, HideProblem.Mode.HIDE_SEQ, false);
    }
}
