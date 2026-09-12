package hidenosp.exp;

import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC6 of section 11.1: scalability in |D|, sequence length, the size of the sensitive set and the
 * gap width, plus the cost of the index itself.
 *
 * <p>This is the only scenario whose numbers depend on the machine, so it is the only one that may
 * write into {@code results/}, and only from the declared measuring machine;
 * {@link ResultSink} refuses otherwise. Wall clock, processor time and peak memory follow the
 * repetition table of section 11.6 through {@link hidenosp.util.Timing}.
 *
 * <p>Three comparisons belong here and each needs its opponent implemented honestly, at the same
 * level of optimisation and in the same language (rule 2.3.14):
 *
 * <ul>
 *   <li>index build time, peak memory and time per mask, incremental against a rebuild;</li>
 *   <li>the quantity delta by forward and backward path counts against the per-position dynamic
 *       program of theorem 2 of [R20];</li>
 *   <li>the same against Update-paths of [R3].</li>
 * </ul>
 *
 * <p>TODO(code): implement, and report the phase breakdown asked for by rule 2.5.3, so a large
 * B_eps shows up as its own cost rather than inflating the total.
 */
public final class Sc6Scalability implements Scenario {

    @Override
    public String id() {
        return "SC6";
    }

    @Override
    public String title() {
        return "scalability and index cost: time, peak memory, Nettree size";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "machine", "warmups",
                "seed"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        throw new UnsupportedOperationException("SC6 is not implemented yet");
    }
}
