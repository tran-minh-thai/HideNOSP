package hidenosp.core;

import java.util.Arrays;

/**
 * A pattern P = p_1 ... p_m over Sigma (design doc section 2).
 *
 * <p>{@link #mu()} is mu(P), the largest number of repetitions of one item inside P. The exact
 * results P1, P2 and the vital-node corollary of L5 hold for mu(P) = 1 only; for mu(P) &ge; 2 only
 * the bounds of P3 are available, and a position may carry several Nettree nodes.
 */
public final class Pattern implements Comparable<Pattern> {

    private final int[] items;
    private final int mu;

    public Pattern(int... items) {
        if (items.length == 0) {
            throw new IllegalArgumentException("empty pattern");
        }
        this.items = items.clone();
        this.mu = computeMu(this.items);
    }

    private static int computeMu(int[] items) {
        int mu = 1;
        for (int i = 0; i < items.length; i++) {
            int c = 0;
            for (int item : items) {
                if (item == items[i]) {
                    c++;
                }
            }
            mu = Math.max(mu, c);
        }
        return mu;
    }

    public int length() {
        return items.length;
    }

    public int at(int j) {
        return items[j];
    }

    public int[] items() {
        return items;
    }

    /** mu(P); the pattern has distinct items when this is 1. */
    public int mu() {
        return mu;
    }

    public boolean hasDistinctItems() {
        return mu == 1;
    }

    /** P extended by one item on the right, used by the level-wise enumeration of F_sigma(D). */
    public Pattern extend(int item) {
        int[] out = Arrays.copyOf(items, items.length + 1);
        out[items.length] = item;
        return new Pattern(out);
    }

    /** The prefix of length {@code len}. */
    public Pattern prefix(int len) {
        return new Pattern(Arrays.copyOf(items, len));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Pattern && Arrays.equals(items, ((Pattern) o).items);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items);
    }

    @Override
    public int compareTo(Pattern o) {
        return Arrays.compare(items, o.items);
    }

    @Override
    public String toString() {
        return Arrays.toString(items);
    }
}
