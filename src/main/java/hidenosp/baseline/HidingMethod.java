// SPDX-License-Identifier: MIT
package hidenosp.baseline;

import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

/**
 * One hiding method, so that every tier of the baselines is driven by the same scenario code. The
 * methods share one set of data structures with flags rather than one program per method:
 * implementations therefore build on the Nettree and on sup_NO, never on their own index.
 */
public interface HidingMethod {

    /** The name printed in result files; it must identify the variant, not only the family. */
    String name();

    /** Produces R and, through it, D-prime. Implementations must not modify the problem. */
    Sanitization run(HideProblem problem);

    /**
     * True when the result depends on the seed, so one run is not the method's value on an
     * instance. The preregistration answers those with the median over ten seeds, 2026 to 2035;
     * the scenario needs to know which methods to repeat, and guessing from the name would break
     * the first time a method is renamed.
     */
    default boolean isRandomized() {
        return false;
    }
}
