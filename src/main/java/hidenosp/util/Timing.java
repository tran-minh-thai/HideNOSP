// SPDX-License-Identifier: MIT
package hidenosp.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Repeated timing under the rules of section 11.6 of the design doc.
 *
 * <p>The number of repetitions follows the duration of a single run: at least fifteen below one
 * second, at least ten from one to ten seconds, five up to two minutes and three above, the last
 * with a reason recorded. Wall clock and processor time are both taken, as rule 2.3.5 asks, and the
 * warm-up runs are discarded, which row 9 of the protocol table has to describe.
 *
 * <p>Every value produced here is machine dependent, so it may only be written into
 * {@code results/} and only from the declared measuring machine; {@link hidenosp.io.ResultSink}
 * enforces that.
 */
public final class Timing {

    /** Milliseconds of wall clock and of processor time, plus the repetition count. */
    public record Measurement(double wallMillisMean, double wallMillisStdDev,
                              double cpuMillisMean, int repetitions, int warmups, String note) {
    }

    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();

    private Timing() {
    }

    /** Repetitions for a run of the given duration, per the table of section 11.6. */
    public static int repetitionsFor(double singleRunMillis) {
        if (singleRunMillis < 1_000) {
            return 15;
        }
        if (singleRunMillis < 10_000) {
            return 10;
        }
        if (singleRunMillis < 120_000) {
            return 5;
        }
        return 3;
    }

    /**
     * Warms up, then times the action the number of times the table prescribes.
     *
     * @param warmups discarded runs; row 9 of the protocol table must state this number
     */
    public static Measurement measure(Supplier<?> action, int warmups) {
        for (int i = 0; i < warmups; i++) {
            consume(action.get());
        }
        long probeStart = System.nanoTime();
        consume(action.get());
        double probeMillis = (System.nanoTime() - probeStart) / 1e6;
        int repetitions = repetitionsFor(probeMillis);

        double[] wall = new double[repetitions];
        double cpuTotal = 0;
        boolean cpuAvailable = THREADS.isCurrentThreadCpuTimeSupported();
        for (int i = 0; i < repetitions; i++) {
            long cpuStart = cpuAvailable ? THREADS.getCurrentThreadCpuTime() : 0;
            long start = System.nanoTime();
            consume(action.get());
            wall[i] = (System.nanoTime() - start) / 1e6;
            if (cpuAvailable) {
                cpuTotal += (THREADS.getCurrentThreadCpuTime() - cpuStart) / 1e6;
            }
        }
        double mean = Arrays.stream(wall).average().orElse(0);
        double variance = Arrays.stream(wall).map(x -> (x - mean) * (x - mean)).sum()
                / Math.max(1, repetitions - 1);
        String note = repetitions == 3 ? "three repetitions: a single run exceeds two minutes" : "";
        return new Measurement(mean, Math.sqrt(variance), cpuAvailable ? cpuTotal / repetitions : -1,
                repetitions, warmups, note);
    }

    /** Keeps the just-in-time compiler from removing the call whose result nobody reads. */
    private static void consume(Object value) {
        if (value != null && value.hashCode() == Integer.MIN_VALUE) {
            throw new IllegalStateException("unreachable");
        }
    }
}
