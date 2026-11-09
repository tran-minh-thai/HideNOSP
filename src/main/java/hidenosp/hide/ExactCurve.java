// SPDX-License-Identifier: MIT
package hidenosp.hide;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;

/**
 * The exact curve of one pattern in one sequence, whichever program applies: {@link FrontierDp}
 * when g- = 0, exactly as before; {@link WindowDp}, the program of the corollary for g- &gt; 0, when
 * g+ is finite and the state is small enough. Every caller that needs the exact least masking goes
 * through this class, so that g- = 0 keeps computing what it computed before WindowDp existed.
 */
public final class ExactCurve {

    private ExactCurve() {
    }

    /** True when an exact program applies to the pattern under the gap. */
    public static boolean applies(GapConstraint gap, Pattern pattern) {
        return FrontierDp.applies(gap) || WindowDp.applies(gap, pattern);
    }

    /** True when an exact program applies to every pattern given. */
    public static boolean appliesToAll(GapConstraint gap, Iterable<Pattern> patterns) {
        for (Pattern p : patterns) {
            if (!applies(gap, p)) {
                return false;
            }
        }
        return true;
    }

    /** The exact curve under {@code order}; see {@link FrontierDp#solve} for its fields. */
    public static FrontierDp.Curve solve(Pattern pattern, EventSequence sequence, GapConstraint gap,
                                         long[] positionWeight, FrontierDp.Order order) {
        if (FrontierDp.applies(gap)) {
            return FrontierDp.solve(pattern, sequence, gap, positionWeight, order);
        }
        return WindowDp.solve(pattern, sequence, gap, positionWeight, order);
    }

    /** The exact curve with fewest masks first, then least weight. */
    public static FrontierDp.Curve solve(Pattern pattern, EventSequence sequence, GapConstraint gap,
                                         long[] positionWeight) {
        return solve(pattern, sequence, gap, positionWeight, new FrontierDp.Order(0, 1));
    }
}
