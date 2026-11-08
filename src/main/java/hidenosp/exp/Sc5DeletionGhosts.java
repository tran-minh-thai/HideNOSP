// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC5: how many ghost patterns deletion creates, swept over g+ in {0, 1, 2, 4,
 * infinity}.
 *
 * <p>This scenario measures one side only. By P5(a) masking never raises sup_NO, so its Ghost count
 * is identically zero and a table of zeros would be a restatement of the lemma, not a measurement;
 * it is not reported as a finding. What is worth measuring is the deletion arm: P5(b)
 * says deletion stops being monotone exactly when g+ is finite, and the size of the effect on real
 * data is not predicted by the lemma.
 *
 * <p>The same phenomenon appears next door: [R27] observed that three classical utility-mining
 * sanitizers produce new itemsets once a periodicity constraint is present. That is the empirical
 * counterpart of P5(b), and the paper cites it rather than claiming the observation as new.
 *
 * <p>TODO(code): implement. For every g+ in the sweep, run the same masking solution, delete the
 * same positions instead of masking them, and count the patterns of F_sigma(D-prime) that were not
 * in F_sigma(D).
 */
public final class Sc5DeletionGhosts implements Scenario {

    @Override
    public String id() {
        return "SC5";
    }

    @Override
    public String title() {
        return "deletion against masking: ghost patterns as the maximum gap varies";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "sigma", "maxPatternLength", "seed"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        throw new UnsupportedOperationException("SC5 is not implemented yet");
    }
}
