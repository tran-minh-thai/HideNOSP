package hidenosp.baseline;

import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

/**
 * DBSH of Gkoulalas-Divanis and Loukides 2011 [R3], tier (b) of section 11.3.
 *
 * <p>DBSH works at the level of the database: the function Cost scores each event by the ratio of
 * its number of occurrences in the sequence to the number of sensitive patterns that contain it,
 * picks the smallest ratio and deletes every occurrence of that event; Exclude keeps the sequences
 * whose sanitization would be most harmful outside the plan. The guarantee of O(ln |TS|) is against
 * the set cover over events, not against the least number of positions, which is the point the
 * paper makes when it places the exact cut next to it (gap G4).
 *
 * <p>The probe {@code probe/dbsh.py} found that over 400 gap-free databases the total number of
 * deleted events matched the optimum every time, while inside single sequences Cost deleted more
 * than necessary in 161 of 10643 cases. SC8 repeats that comparison in Java on MSNBC,
 * BMS-WebView-1 and TRUCKS with the thresholds psi of [R3].
 *
 * <p>TODO(code): implement Cost, Candidates, Exclude and Update-paths as published, on the shared
 * Nettree structures, with a flag for the gap-free setting of the original paper and for the gap
 * setting of this paper.
 */
public final class Dbsh implements HidingMethod {

    @Override
    public String name() {
        return "DBSH";
    }

    @Override
    public Sanitization run(HideProblem problem) {
        throw new UnsupportedOperationException("DBSH is not implemented yet; see section 11.3");
    }
}
