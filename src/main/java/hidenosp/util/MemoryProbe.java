// SPDX-License-Identifier: MIT
package hidenosp.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Peak memory during a run, for row 6 of the protocol table of section 11.7.
 *
 * <p>The design doc asks for the largest resident set size of the process together with the
 * sampling period. The Java virtual machine does not expose the resident set directly, so two
 * numbers are kept apart and both are declared: the peak used heap sampled from inside, and the
 * peak resident set of the operating system, which a wrapper script records around the run. A table
 * must say which of the two it prints.
 *
 * <p>TODO(code): add the wrapper that records the resident set. On Windows the counter is available
 * from {@code Get-Process}, on Linux from {@code /proc/self/status}. Until then only the heap is
 * reported and the protocol row must say so.
 */
public final class MemoryProbe implements AutoCloseable {

    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();

    private final Thread sampler;
    private final long periodMillis;
    private volatile boolean running = true;
    private volatile long peakHeapBytes;

    /** Starts sampling the used heap every {@code periodMillis}. */
    public MemoryProbe(long periodMillis) {
        this.periodMillis = periodMillis;
        this.sampler = new Thread(this::sample, "memory-probe");
        this.sampler.setDaemon(true);
        this.sampler.start();
    }

    private void sample() {
        while (running) {
            long used = MEMORY.getHeapMemoryUsage().getUsed();
            if (used > peakHeapBytes) {
                peakHeapBytes = used;
            }
            try {
                Thread.sleep(periodMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public long peakHeapBytes() {
        return peakHeapBytes;
    }

    public long periodMillis() {
        return periodMillis;
    }

    @Override
    public void close() {
        running = false;
        sampler.interrupt();
    }
}
