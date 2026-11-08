// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

/**
 * One scenario. The numbering of the files follows the numbering of the scenarios, so a table in
 * the paper names the file that produced it.
 */
public interface Scenario {

    /** SC1 to SC8, or SIGMA for the helper that applies the sigma rule. */
    String id();

    /** One line: the purpose of the scenario. */
    String title();

    /** Option names this scenario accepts, for the argument parser. */
    String[] options();

    /** Runs the scenario and writes its result files through the sink. */
    void run(ResultSink sink, Cli options);
}
