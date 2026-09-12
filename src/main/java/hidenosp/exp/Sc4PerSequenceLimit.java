package hidenosp.exp;

import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC4 of section 11.1: HIDE-SEQ at r in {1, 2, 3, 5}, the per-sequence limit on how often one
 * individual may repeat the sensitive behaviour.
 *
 * <p>It answers RQ5: at the same nominal privacy level, how many masks and how much pattern loss
 * does the per-sequence limit cost compared with the database threshold. By the corollary of P1 the
 * number of masks is fixed at sum over S of max(0, F_S - r + 1), so the comparison is about where
 * they fall, not how many there are, and the table must print both numbers side by side.
 *
 * <p>Note that sigma and r are different quantities, so the side effects of this scenario are
 * counted against a sigma that has to be stated in the table caption; {@link
 * hidenosp.hide.HideProblem} carries it as a separate field for that reason.
 *
 * <p>TODO(code): implement on the same instance unit as SC3, sweeping r and reporting |R|, Lost and
 * SupDist next to the HIDE-DB row at the same nominal level.
 */
public final class Sc4PerSequenceLimit implements Scenario {

    @Override
    public String id() {
        return "SC4";
    }

    @Override
    public String title() {
        return "HIDE-SEQ at r in {1, 2, 3, 5}: masks and pattern loss against the database threshold";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "r", "seed"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        throw new UnsupportedOperationException("SC4 is not implemented yet");
    }
}
