package hidenosp.util;

import java.util.function.Supplier;

/**
 * A wall-clock limit for one piece of work on the current thread, checked cooperatively.
 *
 * <p>The preregistration drops a pair from the main test when a method exceeds the time limit
 * (code OT) and counts it apart. Java can no longer stop a thread from outside, and a method left
 * running on another thread would keep a core and its memory busy for the rest of the batch. So
 * the limit is checked from inside: {@link #check()} sits in the primitives every hiding method
 * goes through (building a Nettree, masking one position of a sequence, each phase of a max flow, each
 * position of the exact program, each pass of the local search) and throws {@link Exceeded} once the limit has passed. Outside
 * {@link #within} the check does nothing, so measuring the result after the method is never cut.
 */
public final class Deadline {

    /** Thrown by {@link #check()} once the limit of the current {@link #within} call has passed. */
    public static final class Exceeded extends RuntimeException {
        public Exceeded(double seconds) {
            super("time limit of " + seconds + " s exceeded", null, false, false);
        }
    }

    /** The end of the current limit in System.nanoTime() and the limit as given, in seconds. */
    private record Limit(long end, double seconds) {
    }

    private static final ThreadLocal<Limit> LIMIT = new ThreadLocal<>();

    private Deadline() {
    }

    /**
     * Runs {@code work} with a limit of {@code seconds} of wall-clock time on this thread. A limit
     * of 0 or less means no limit. A positive limit below one nanosecond is over as soon as it
     * starts, so the first check throws: the tests use that to prove a method reaches a check at
     * all. Calls do not nest: the work must not start another limit.
     */
    public static <T> T within(double seconds, Supplier<T> work) {
        if (!(seconds > 0)) {
            return work.get();
        }
        if (LIMIT.get() != null) {
            throw new IllegalStateException("a time limit is already running on this thread");
        }
        long nanos = (long) Math.floor(seconds * 1e9);
        LIMIT.set(new Limit(System.nanoTime() + nanos, seconds));
        try {
            return work.get();
        } finally {
            LIMIT.remove();
        }
    }

    /** Throws {@link Exceeded} when the limit of the current {@link #within} call has passed. */
    public static void check() {
        Limit limit = LIMIT.get();
        if (limit != null && System.nanoTime() - limit.end() >= 0) {
            throw new Exceeded(limit.seconds());
        }
    }
}
