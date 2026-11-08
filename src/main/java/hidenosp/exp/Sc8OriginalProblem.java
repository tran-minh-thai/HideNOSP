// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * SC8: the original problem of [R20] and [R3], where support counts sequences
 * rather than non-overlapping occurrences.
 *
 * <p>Running there matters because it is the only place where the numbers of this paper can be put
 * next to published numbers: MSN, BMS-WebView-1 and TRUCKS with the thresholds psi of [R3], and the
 * side effects, distance and time reported the way those papers report them. It is also where
 * corollary P1c applies most directly: for one distinct-item pattern the least number of marks that
 * makes a sequence stop supporting the pattern is exactly sup_NO of that pattern in that sequence,
 * computable by one max flow, while [R20] proved hardness only for several patterns and used a
 * heuristic throughout.
 *
 * <p>The probe scripts already carry the shape of this comparison in Python:
 * {@code probe/oldprob.py} for HHA against the exact cut, {@code probe/dbsh.py} for DBSH, and
 * {@code probe/sideeffect.py} for the loss of non-sensitive patterns. Their numbers are probes and
 * do not enter the paper; this scenario is what replaces them.
 *
 * <p>TODO(code): implement once HHA, DBSH and SBSH are in place, on the real datasets, with the
 * thresholds of the original papers and their own reporting units.
 */
public final class Sc8OriginalProblem implements Scenario {

    @Override
    public String id() {
        return "SC8";
    }

    @Override
    public String title() {
        return "the original sequence-support problem: CUT-HIDE against HHA, DBSH and SBSH";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "psi", "gmin", "gmax", "seed"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        throw new UnsupportedOperationException("SC8 is not implemented yet");
    }
}
