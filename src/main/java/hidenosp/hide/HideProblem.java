// SPDX-License-Identifier: MIT
package hidenosp.hide;

import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.metrics.FrequentPatterns;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * An instance of HIDE-DB or HIDE-SEQ.
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
    private final FrequentPatterns.Closure closure;

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
        this(database, sensitive, gap, mode, threshold, epsilon, maxPatternLength, frequentThreshold,
                FrequentPatterns.Closure.NECESSARY);
    }

    /**
     * @param closure which frequent patterns count as hidden together with the sensitive ones and
     *     are therefore left out of Lost and SupDist; see {@link #closureOf}
     */
    public HideProblem(SequenceDatabase database, List<Pattern> sensitive, GapConstraint gap,
                       Mode mode, int threshold, double epsilon, int maxPatternLength,
                       long frequentThreshold, FrequentPatterns.Closure closure) {
        this.closure = closure;
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

    public FrequentPatterns.Closure closure() {
        return closure;
    }

    /** This problem with another closure rule; every other field is kept. */
    public HideProblem withClosure(FrequentPatterns.Closure other) {
        return new HideProblem(database, sensitive, gap, mode, threshold, epsilon, maxPatternLength,
                frequentThreshold, other);
    }

    /** This problem with other sensitive patterns; every other field, the closure rule included, is kept. */
    public HideProblem withSensitive(List<Pattern> other) {
        return new HideProblem(database, other, gap, mode, threshold, epsilon, maxPatternLength,
                frequentThreshold, closure);
    }

    /**
     * S-plus of this problem among {@code frequent}: the sensitive patterns, and the frequent patterns
     * that hiding them hides as well, which Lost and SupDist leave out and the methods do not try to
     * protect.
     *
     * <p>Under {@link FrequentPatterns.Closure#NECESSARY} the super-patterns enter only when every
     * solution hides them: under HIDE-DB with a hiding threshold no larger than the sigma of
     * F_sigma(D), the patterns that contain a sensitive pattern as a block (as a subsequence when g+
     * is unbounded). Under HIDE-SEQ the limit holds in each sequence, which does not bring the
     * support of a super-pattern over the database below sigma, so S-plus is the sensitive patterns
     * alone. {@link FrequentPatterns.Closure#SUBSEQUENCE} is the earlier rule, kept to reproduce
     * the result files written with it.
     */
    public Set<Pattern> closureOf(Collection<Pattern> frequent) {
        if (closure == FrequentPatterns.Closure.NECESSARY
                && !(isDatabaseMode() && threshold <= frequentThreshold)) {
            return new java.util.LinkedHashSet<>(sensitive);
        }
        return FrequentPatterns.closure(sensitive, frequent, gap, closure);
    }

    /** S-plus under the earlier rule, for the side columns that report measures as they were counted before. */
    public Set<Pattern> subsequenceClosureOf(Collection<Pattern> frequent) {
        return FrequentPatterns.closure(sensitive, frequent, gap, FrequentPatterns.Closure.SUBSEQUENCE);
    }

    @Override
    public String toString() {
        return mode + "[|D|=" + database.size() + ", |S|=" + sensitive.size()
                + ", threshold=" + threshold + ", gap=" + gap + ", eps=" + epsilon
                + ", closure=" + closure.label() + "]";
    }
}
