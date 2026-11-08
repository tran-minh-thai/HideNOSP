// SPDX-License-Identifier: MIT
package hidenosp.hide;

import hidenosp.core.SequenceDatabase;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The output of a hiding method: the set R of masked positions, the sanitized database D-prime, and
 * the bookkeeping every scenario reports next to it.
 *
 * <p>R is stored as a map from sequence index to masked positions, because both objectives of
 * section 2 and the per-sequence constraint of HIDE-SEQ are read per sequence.
 */
public final class Sanitization {

    private final SequenceDatabase original;
    private final Map<Integer, Set<Integer>> masked = new TreeMap<>();
    private String method = "unknown";
    private long certifiedPairs;
    private long totalPairs;
    private long certifiedLowerBound;
    private int replacedComponents;
    private int removedMasks;

    public Sanitization(SequenceDatabase original) {
        this.original = original;
    }

    public Sanitization method(String name) {
        this.method = name;
        return this;
    }

    public String method() {
        return method;
    }

    /** Records one masked position; repeated calls on the same position are idempotent. */
    public void mask(int sequenceIndex, int position) {
        masked.computeIfAbsent(sequenceIndex, k -> new TreeSet<>()).add(position);
    }

    /** Removes one masked position; a sequence left with no mask is dropped from the map. */
    public void unmask(int sequenceIndex, int position) {
        Set<Integer> set = masked.get(sequenceIndex);
        if (set != null) {
            set.remove(position);
            if (set.isEmpty()) {
                masked.remove(sequenceIndex);
            }
        }
    }

    /**
     * Records what the several-pattern refinement of CUT-HIDE changed: components of one pattern
     * replaced by the exact single-pattern solution, and masks removed by the reverse delete.
     */
    public void recordRefinement(int replacedComponents, int removedMasks) {
        this.replacedComponents += replacedComponents;
        this.removedMasks += removedMasks;
    }

    public int replacedComponents() {
        return replacedComponents;
    }

    public int removedMasks() {
        return removedMasks;
    }

    public Set<Integer> maskedIn(int sequenceIndex) {
        return masked.getOrDefault(sequenceIndex, new TreeSet<>());
    }

    public Map<Integer, Set<Integer>> masked() {
        return masked;
    }

    /**
     * A lower bound on the optimum this instance could not have beaten, or 0 when the method proves
     * none. {@link PrimalDualCover} fills it from its feasible dual; the greedy methods cannot.
     */
    public Sanitization certifiedLowerBound(long bound) {
        this.certifiedLowerBound = bound;
        return this;
    }

    public long certifiedLowerBound() {
        return certifiedLowerBound;
    }

    /** |R|, the first objective. */
    public int maskCount() {
        int total = 0;
        for (Set<Integer> s : masked.values()) {
            total += s.size();
        }
        return total;
    }

    public SequenceDatabase original() {
        return original;
    }

    /** D-prime = che(D, R). */
    public SequenceDatabase sanitized() {
        return original.mask(masked);
    }

    /**
     * Counts the pairs (S, k) whose cost was certified optimal by the lambda sweep of P6(d), that
     * is those with ceiling(LB) = UB. The design doc asks for this ratio to be reported as a
     * quantity of the paper, not hidden inside the algorithm.
     */
    public void recordCertification(boolean certified) {
        totalPairs++;
        if (certified) {
            certifiedPairs++;
        }
    }

    public long certifiedPairs() {
        return certifiedPairs;
    }

    public long totalPairs() {
        return totalPairs;
    }

    @Override
    public String toString() {
        return "Sanitization[" + method + ", |R|=" + maskCount()
                + ", certified=" + certifiedPairs + "/" + totalPairs + "]";
    }
}
