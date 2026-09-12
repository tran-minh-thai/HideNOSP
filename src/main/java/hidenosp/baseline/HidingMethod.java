package hidenosp.baseline;

import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

/**
 * One hiding method, so that every tier of the baseline table of section 11.3 is driven by the same
 * scenario code. Rule 2.3.4 asks for one set of data structures with flags, not one program per
 * method: implementations therefore build on the Nettree and on sup_NO, never on their own index.
 */
public interface HidingMethod {

    /** The name printed in result files; it must identify the variant, not only the family. */
    String name();

    /** Produces R and, through it, D-prime. Implementations must not modify the problem. */
    Sanitization run(HideProblem problem);
}
