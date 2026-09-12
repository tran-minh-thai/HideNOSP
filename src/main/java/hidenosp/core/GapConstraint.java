package hidenosp.core;

/**
 * The gap constraint [g-, g+] of design doc section 2, applied to every pattern, sensitive or not.
 *
 * <p>An occurrence L = &lt;l_1, ..., l_m&gt; must satisfy g- &le; l_{j+1} - l_j - 1 &le; g+ for every
 * j. {@link #UNBOUNDED_MAX} encodes g+ = infinity; P5(b) says deletion is monotone exactly in that
 * case, so several scenarios branch on {@link #isUnbounded()}.
 */
public final class GapConstraint {

    /** The code for g+ = infinity. Kept well below Integer.MAX_VALUE so i + 1 + gmax never overflows. */
    public static final int UNBOUNDED_MAX = Integer.MAX_VALUE / 4;

    private final int gmin;
    private final int gmax;

    public GapConstraint(int gmin, int gmax) {
        if (gmin < 0 || gmax < gmin) {
            throw new IllegalArgumentException("require 0 <= gmin <= gmax, got [" + gmin + "," + gmax + "]");
        }
        this.gmin = gmin;
        this.gmax = gmax;
    }

    public static GapConstraint of(int gmin, int gmax) {
        return new GapConstraint(gmin, gmax);
    }

    /** [gmin, infinity). */
    public static GapConstraint unbounded(int gmin) {
        return new GapConstraint(gmin, UNBOUNDED_MAX);
    }

    public int gmin() {
        return gmin;
    }

    public int gmax() {
        return gmax;
    }

    public boolean isUnbounded() {
        return gmax >= UNBOUNDED_MAX;
    }

    /** First position that may carry the next node after position {@code i}. */
    public int firstNext(int i) {
        return i + 1 + gmin;
    }

    /** Last position that may carry the next node after position {@code i}, clamped to {@code n - 1}. */
    public int lastNext(int i, int n) {
        long hi = (long) i + 1 + gmax;
        return (int) Math.min(hi, n - 1);
    }

    /** w = min(n, g+ - g- + 1), the gap width used in every complexity statement (section 8.1). */
    public int width(int n) {
        long w = (long) gmax - gmin + 1;
        return (int) Math.min(w, n);
    }

    @Override
    public String toString() {
        return "[" + gmin + "," + (isUnbounded() ? "inf" : String.valueOf(gmax)) + "]";
    }
}
