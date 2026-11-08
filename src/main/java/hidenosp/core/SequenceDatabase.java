// SPDX-License-Identifier: MIT
package hidenosp.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * D = {S_1, ..., S_N}, a database of single-item event sequences (design doc section 2).
 *
 * <p>{@code labels} keeps the original symbol of each item code so tables and figures can be
 * printed with the names of the dataset; the central label table of rule 3.3.12 reads from here.
 */
public final class SequenceDatabase {

    private final List<EventSequence> sequences;
    private final Map<Integer, String> labels;

    public SequenceDatabase(List<EventSequence> sequences, Map<Integer, String> labels) {
        this.sequences = List.copyOf(sequences);
        this.labels = Map.copyOf(labels);
    }

    public SequenceDatabase(List<EventSequence> sequences) {
        this(sequences, Collections.emptyMap());
    }

    public int size() {
        return sequences.size();
    }

    public EventSequence get(int index) {
        return sequences.get(index);
    }

    public List<EventSequence> sequences() {
        return sequences;
    }

    public Map<Integer, String> labels() {
        return labels;
    }

    /** Sigma, as the sorted set of item codes actually present (masked positions excluded). */
    public int[] alphabet() {
        TreeMap<Integer, Integer> seen = new TreeMap<>();
        for (EventSequence s : sequences) {
            for (int i = 0; i < s.length(); i++) {
                if (s.at(i) != EventSequence.MASK) {
                    seen.put(s.at(i), 1);
                }
            }
        }
        int[] out = new int[seen.size()];
        int k = 0;
        for (int item : seen.keySet()) {
            out[k++] = item;
        }
        return out;
    }

    /** sum over S of |S|, the denominator of DistR. */
    public long totalLength() {
        long t = 0;
        for (EventSequence s : sequences) {
            t += s.length();
        }
        return t;
    }

    /**
     * che(D, R), where {@code positions} maps a sequence index to the positions masked in it.
     * Sequences absent from the map are shared, not copied.
     */
    public SequenceDatabase mask(Map<Integer, ? extends Collection<Integer>> positions) {
        List<EventSequence> out = new ArrayList<>(sequences.size());
        for (int i = 0; i < sequences.size(); i++) {
            Collection<Integer> r = positions.get(i);
            out.add(r == null || r.isEmpty() ? sequences.get(i) : sequences.get(i).mask(r));
        }
        return new SequenceDatabase(out, labels);
    }

    /** xoa(D, R); only the deletion arm of SC5 uses it (P5(b)). */
    public SequenceDatabase delete(Map<Integer, ? extends Collection<Integer>> positions) {
        List<EventSequence> out = new ArrayList<>(sequences.size());
        for (int i = 0; i < sequences.size(); i++) {
            Collection<Integer> r = positions.get(i);
            out.add(r == null || r.isEmpty() ? sequences.get(i) : sequences.get(i).delete(r));
        }
        return new SequenceDatabase(out, labels);
    }

    @Override
    public String toString() {
        return "D[N=" + sequences.size() + ", totalLength=" + totalLength() + "]";
    }
}
