// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.hide.HideProblem;
import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

import java.util.ArrayList;
import java.util.List;

/**
 * SC3WIDE: the comparison of SC3 with every method hiding under a wider gap constraint, --hideGmax (a number or inf),
 * while Lost, SupDist and HF are counted under the gap of the run, as in SC3 (preregistration section 23, lever 1).
 * The non-overlapping support does not decrease when g+ grows, so a set hidden under the wider constraint is hidden
 * under every narrower one. min_masks is |R|* under the hiding constraint. A row whose method leaves a sensitive pattern
 * at or above the threshold under the hiding constraint carries the status "not hidden at the hiding gap".
 */
public final class Sc3Wide implements Scenario {

    @Override
    public String id() {
        return "SC3WIDE";
    }

    @Override
    public String title() {
        return "the comparison of SC3 with every method hiding under a wider gap constraint";
    }

    @Override
    public String[] options() {
        List<String> names = new ArrayList<>(List.of(new Sc3MainComparison().options()));
        names.add("hideGmax");
        return names.toArray(new String[0]);
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        if (options.get("hideGmax", "").isEmpty()) {
            throw new IllegalArgumentException("SC3WIDE needs --hideGmax (a number at least g+, or inf)");
        }
        Sc3MainComparison.runComparison(id(), sink, options, HideProblem.Mode.HIDE_DB, false);
    }
}
