// SPDX-License-Identifier: MIT
package hidenosp.core;

import java.util.Arrays;
import java.util.Collection;

/**
 * A single-item event sequence S = e_1 ... e_n over the alphabet Sigma (design doc section 2).
 *
 * <p>Items are encoded as non-negative {@code int} codes; {@link #MASK} is the sentinel for the
 * masked symbol &perp;, which by definition matches no item of Sigma. Positions are 0-based
 * internally and printed 1-based, matching the design doc.
 *
 * <p>Instances are immutable: {@link #mask} and {@link #delete} return new sequences, so the
 * original database D is always available next to the sanitized D' when metrics are computed.
 */
public final class EventSequence {

    /** The sentinel for &perp;. It is never equal to any item code, so it matches nothing. */
    public static final int MASK = -1;

    private final int id;
    private final int[] events;

    public EventSequence(int id, int[] events) {
        this.id = id;
        this.events = events.clone();
    }

    public int id() {
        return id;
    }

    public int length() {
        return events.length;
    }

    /** Item at 0-based position {@code i}, or {@link #MASK}. */
    public int at(int i) {
        return events[i];
    }

    /** Raw backing array; callers must not modify it. */
    public int[] events() {
        return events;
    }

    /** che(S, R): replace every position of {@code positions} by &perp; (design doc section 2). */
    public EventSequence mask(Collection<Integer> positions) {
        int[] out = events.clone();
        for (int i : positions) {
            out[i] = MASK;
        }
        return new EventSequence(id, out);
    }

    /** che(S, {i}). */
    public EventSequence mask(int position) {
        int[] out = events.clone();
        out[position] = MASK;
        return new EventSequence(id, out);
    }

    /**
     * xoa(S, R): drop the positions of {@code positions} (design doc section 2). Only used by the
     * deletion arm of SC5; CUT-HIDE itself never deletes, because P5(b) shows deletion is
     * non-monotone as soon as g+ is finite.
     */
    public EventSequence delete(Collection<Integer> positions) {
        boolean[] drop = new boolean[events.length];
        for (int i : positions) {
            drop[i] = true;
        }
        int[] out = new int[events.length];
        int k = 0;
        for (int i = 0; i < events.length; i++) {
            if (!drop[i]) {
                out[k++] = events[i];
            }
        }
        return new EventSequence(id, Arrays.copyOf(out, k));
    }

    /** Number of positions currently holding &perp;. */
    public int maskedCount() {
        int c = 0;
        for (int e : events) {
            if (e == MASK) {
                c++;
            }
        }
        return c;
    }

    @Override
    public String toString() {
        return "S" + id + Arrays.toString(events);
    }
}
