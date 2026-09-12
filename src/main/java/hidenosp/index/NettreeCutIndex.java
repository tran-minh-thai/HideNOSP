package hidenosp.index;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.flow.NettreeFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * The Nettree cut index of design doc section 9.1, held for one sequence S.
 *
 * <p>It merges the Nettrees of every sensitive pattern and of every near-threshold pattern of
 * B_eps over a shared position table: {@link #patternsAtPosition(int)} says which Nettrees carry a
 * node at a position, so masking that position is a node deletion in each of them (remark O1).
 * Beside every Nettree it keeps
 *
 * <ul>
 *   <li>a max flow and the strongly connected components of its residual graph, which give
 *       sup_NO, a minimum cut and every vital node at once (L0, L5);</li>
 *   <li>forward and backward path counts, which give the occurrences through every position in one
 *       pass, for the baseline of [R20] and for side-effect weights;</li>
 *   <li>an incremental update after each mask, rather than a rebuild.</li>
 * </ul>
 *
 * <p>The path counts are rebuilt lazily after a mask; the flow state is repaired in place by
 * {@link NettreeFlow#maskPosition(int)}. SC6 reports build time, peak memory and per-mask time
 * against a rebuild, against the per-position dynamic program of [R20] and against Update-paths of
 * [R3].
 */
public final class NettreeCutIndex {

    private final EventSequence sequence;
    private final GapConstraint gap;
    private final List<Pattern> patterns = new ArrayList<>();
    private final List<Boolean> sensitive = new ArrayList<>();
    private final List<Nettree> nettrees = new ArrayList<>();
    private final List<NettreeFlow> flows = new ArrayList<>();
    private final List<PathCounts> counts = new ArrayList<>();
    private final List<List<Integer>> byPosition = new ArrayList<>();
    private final boolean[] masked;

    public NettreeCutIndex(EventSequence sequence, GapConstraint gap) {
        this.sequence = sequence;
        this.gap = gap;
        this.masked = new boolean[sequence.length()];
        for (int i = 0; i < sequence.length(); i++) {
            byPosition.add(new ArrayList<>());
        }
    }

    /**
     * Adds a pattern to the index. {@code isSensitive} separates the members of the sensitive set,
     * whose sup_NO must fall, from the members of B_eps, whose sup_NO drives the position weights
     * of step 4. Patterns with sup_NO = 0 are still accepted; callers skip them for speed.
     *
     * @return the index of the pattern inside this structure
     */
    public int add(Pattern pattern, boolean isSensitive) {
        Nettree nettree = Nettree.build(pattern, sequence, gap);
        int id = patterns.size();
        patterns.add(pattern);
        sensitive.add(isSensitive);
        nettrees.add(nettree);
        flows.add(new NettreeFlow(nettree));
        counts.add(null);
        for (int i = 0; i < sequence.length(); i++) {
            if (nettree.nodesAtPosition(i).length > 0) {
                byPosition.get(i).add(id);
            }
        }
        return id;
    }

    public EventSequence sequence() {
        return sequence;
    }

    public GapConstraint gap() {
        return gap;
    }

    public int patternCount() {
        return patterns.size();
    }

    public Pattern pattern(int id) {
        return patterns.get(id);
    }

    public boolean isSensitive(int id) {
        return sensitive.get(id);
    }

    public Nettree nettree(int id) {
        return nettrees.get(id);
    }

    public NettreeFlow flow(int id) {
        return flows.get(id);
    }

    /** The shared position table: the patterns whose Nettree carries a node at position i. */
    public List<Integer> patternsAtPosition(int i) {
        return byPosition.get(i);
    }

    public boolean isMasked(int i) {
        return masked[i];
    }

    /** sup_NO of pattern {@code id} in the current, partly masked sequence. */
    public int supNO(int id) {
        return flows.get(id).supNO();
    }

    /** The positions whose masking lowers sup_NO of pattern {@code id} by one (L5). */
    public int[] vitalPositions(int id) {
        return flows.get(id).vitalPositions();
    }

    /** Occurrences of pattern {@code id} through position i, from the path counts. */
    public long occurrencesThrough(int id, int position) {
        PathCounts c = counts.get(id);
        if (c == null) {
            c = PathCounts.of(nettrees.get(id), flows.get(id).removedFlags());
            counts.set(id, c);
        }
        return c.throughPosition(position);
    }

    /** The total number of occurrences of pattern {@code id}, the match set size of [R20]. */
    public long totalOccurrences(int id) {
        PathCounts c = counts.get(id);
        if (c == null) {
            c = PathCounts.of(nettrees.get(id), flows.get(id).removedFlags());
            counts.set(id, c);
        }
        return c.totalOccurrences();
    }

    /**
     * Masks position i in every Nettree that carries a node there and returns the drop in sup_NO
     * per pattern, indexed as in this structure. Only the affected path counts are invalidated.
     */
    public int[] maskPosition(int i) {
        int[] drop = new int[patterns.size()];
        if (masked[i]) {
            return drop;
        }
        masked[i] = true;
        for (int id : byPosition.get(i)) {
            drop[id] = flows.get(id).maskPosition(i);
            counts.set(id, null);
        }
        return drop;
    }

    /** Total drop in sup_NO over the sensitive patterns only, the numerator of step 7. */
    public int sensitiveDrop(int[] dropPerPattern) {
        int total = 0;
        for (int id = 0; id < dropPerPattern.length; id++) {
            if (sensitive.get(id)) {
                total += dropPerPattern[id];
            }
        }
        return total;
    }

    /**
     * Step 4 of section 9.2: w(i) = sum over the patterns Q of B_eps for which i is vital of
     * {@code factor[Q]}, where the caller passes factor[Q] = 1 / (sup_NO(Q, D) - sigma + 1). The
     * weight is an approximation, because the effect of several masks does not add up; that is
     * declared in the paper and measured against the exact integer program at SC3.
     *
     * @param factor one entry per pattern of this index; entries of sensitive patterns are ignored
     * @return one weight per position of the sequence
     */
    public double[] positionWeights(double[] factor) {
        double[] w = new double[sequence.length()];
        for (int id = 0; id < patterns.size(); id++) {
            if (sensitive.get(id) || factor[id] == 0) {
                continue;
            }
            for (int i : flows.get(id).vitalPositions()) {
                w[i] += factor[id];
            }
        }
        return w;
    }

    /** Total number of Nettree nodes held, one of the size measures reported by SC6. */
    public long nodeCount() {
        long total = 0;
        for (Nettree nettree : nettrees) {
            total += nettree.nodeCount();
        }
        return total;
    }

    /** Total number of Nettree arcs held, the other size measure reported by SC6. */
    public long arcCount() {
        long total = 0;
        for (Nettree nettree : nettrees) {
            total += nettree.arcCount();
        }
        return total;
    }

    /** The positions masked so far, in increasing order. */
    public List<Integer> maskedPositions() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < masked.length; i++) {
            if (masked[i]) {
                out.add(i);
            }
        }
        return out;
    }
}
