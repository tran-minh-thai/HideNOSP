package hidenosp.hide;

import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;

import java.util.List;

/**
 * An instance of HIDE-DB or HIDE-SEQ (design doc section 2).
 *
 * <p>HIDE-DB asks for a set of positions R with sup_NO(P, che(D, R)) &lt; sigma for every sensitive
 * P; HIDE-SEQ asks for sup_NO(P, S) &lt; r in every single sequence, which bounds how often one
 * individual may repeat the behaviour. The objectives are lexicographic: first the size of R, then
 * the number of non-sensitive patterns lost.
 */
public final class HideProblem {

    public enum Mode {
        /** Database threshold: sup_NO(P, D) below sigma. */
        HIDE_DB,
        /** Per-sequence limit: sup_NO(P, S) below r in every sequence. */
        HIDE_SEQ
    }

    private final SequenceDatabase database;
    private final List<Pattern> sensitive;
    private final GapConstraint gap;
    private final Mode mode;
    private final int threshold;
    private final double epsilon;
    private final int maxPatternLength;
    private final long frequentThreshold;

    /**
     * @param threshold sigma for HIDE-DB, r for HIDE-SEQ
     * @param epsilon width of the near-threshold band B_eps used for the side-effect weights
     * @param maxPatternLength l_max, the largest pattern length of F_sigma(D)
     * @param frequentThreshold the sigma that defines F_sigma(D) for Lost, Ghost and SupDist. It
     *     equals {@code threshold} under HIDE-DB; under HIDE-SEQ the per-sequence limit r is a
     *     different quantity, so SC4 has to state which sigma its side effects are counted against
     */
    public HideProblem(SequenceDatabase database, List<Pattern> sensitive, GapConstraint gap,
                       Mode mode, int threshold, double epsilon, int maxPatternLength,
                       long frequentThreshold) {
        this.database = database;
        this.sensitive = List.copyOf(sensitive);
        this.gap = gap;
        this.mode = mode;
        this.threshold = threshold;
        this.epsilon = epsilon;
        this.maxPatternLength = maxPatternLength;
        this.frequentThreshold = frequentThreshold;
    }

    /** HIDE-DB, where the hiding threshold and the frequency threshold are the same sigma. */
    public HideProblem(SequenceDatabase database, List<Pattern> sensitive, GapConstraint gap,
                       Mode mode, int threshold, double epsilon, int maxPatternLength) {
        this(database, sensitive, gap, mode, threshold, epsilon, maxPatternLength, threshold);
    }

    public SequenceDatabase database() {
        return database;
    }

    public List<Pattern> sensitive() {
        return sensitive;
    }

    public GapConstraint gap() {
        return gap;
    }

    public Mode mode() {
        return mode;
    }

    /** sigma in HIDE-DB, r in HIDE-SEQ. */
    public int threshold() {
        return threshold;
    }

    public double epsilon() {
        return epsilon;
    }

    public int maxPatternLength() {
        return maxPatternLength;
    }

    /** The sigma that defines F_sigma(D) when Lost, Ghost and SupDist are counted. */
    public long frequentThreshold() {
        return frequentThreshold;
    }

    public boolean isDatabaseMode() {
        return mode == Mode.HIDE_DB;
    }

    @Override
    public String toString() {
        return mode + "[|D|=" + database.size() + ", |S|=" + sensitive.size()
                + ", threshold=" + threshold + ", gap=" + gap + ", eps=" + epsilon + "]";
    }
}
