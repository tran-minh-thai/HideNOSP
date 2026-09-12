package hidenosp.baseline;

import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

/**
 * SBSH of Gkoulalas-Divanis and Loukides 2011 [R3], tier (b) of section 11.3.
 *
 * <p>SBSH colours each position black, red or grey according to whether deleting it would destroy a
 * non-sensitive pattern sitting exactly at the threshold psi, then replaces a red position by a set
 * of black positions that cut every path through it, searching within a radius of r arcs. The
 * authors state that the radius is there to save computation.
 *
 * <p>For one distinct-item pattern, that replacement is the weighted vertex cut of P6(b) with the
 * red position carrying a large capacity, and one max flow solves it exactly with no radius limit.
 * SC7 therefore runs SBSH at its smallest and largest radius, so the comparison separates the
 * colouring idea from the radius restriction.
 *
 * <p>TODO(code): implement Annotate, Replace, C-PA and C-SA as published, with the radius as a
 * declared parameter, and record how often the radius binds.
 */
public final class Sbsh implements HidingMethod {

    private final int radius;

    public Sbsh(int radius) {
        this.radius = radius;
    }

    @Override
    public String name() {
        return "SBSH-r" + radius;
    }

    @Override
    public Sanitization run(HideProblem problem) {
        throw new UnsupportedOperationException("SBSH is not implemented yet; see section 11.3");
    }
}
