// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.core.BruteForce;
import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.hide.FrontierDp;
import hidenosp.hide.WindowDp;
import hidenosp.io.CsvWriter;
import hidenosp.io.ResultSink;
import hidenosp.util.Cli;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * SC1GAP: the implementation check of the dynamic program of the corollary for g- &gt; 0
 * ({@link WindowDp}), a supplement to SC1.
 *
 * <p>On seeded random instances (sequence length 6 to {@code maxLength}, pattern length 2 to 4, g- in
 * {1, 2}, g+ from g- to 4, alphabets of 2 or 3 items, so many patterns repeat items), the least number of
 * masks OPT(t) of the program is compared, for every t from 0 to sup_NO, with the exhaustive search of
 * {@link BruteForce#minMasks}, which tries every subset of positions and counts sup_NO by exhaustive packing
 * of occurrences, so it shares no code with the program or with the flow. One row per (g-, g+, repeated
 * items or not): instances, targets compared, targets that need a mask, mismatches.
 */
public final class Sc1WindowCheck implements Scenario {

    @Override
    public String id() {
        return "SC1GAP";
    }

    @Override
    public String title() {
        return "implementation check of the exact program for g- > 0 against exhaustive search";
    }

    @Override
    public String[] options() {
        return new String[] {"trials", "maxLength"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        int trials = options.getInt("trials", 3000);
        int maxLength = options.getInt("maxLength", 11);
        long seed = options.getLong("seed", 2026);
        Random random = new Random(seed);
        Map<String, long[]> tally = new TreeMap<>();
        for (int trial = 0; trial < trials; trial++) {
            int n = 6 + random.nextInt(Math.max(1, maxLength - 5));
            int m = 2 + random.nextInt(3);
            int gmin = 1 + random.nextInt(2);
            int gmax = gmin + random.nextInt(5 - gmin);
            int alphabet = 2 + random.nextInt(2);
            int[] events = new int[n];
            for (int i = 0; i < n; i++) {
                events[i] = random.nextInt(alphabet);
            }
            int[] items = new int[m];
            for (int j = 0; j < m; j++) {
                items[j] = random.nextInt(alphabet);
            }
            Pattern pattern = new Pattern(items);
            EventSequence sequence = new EventSequence(0, events);
            GapConstraint gap = new GapConstraint(gmin, gmax);
            FrontierDp.Curve curve = WindowDp.solve(pattern, sequence, gap, null, new FrontierDp.Order(0, 1));
            int brute = BruteForce.supNO(pattern, sequence, gap);
            String key = gap + (pattern.hasDistinctItems() ? " distinct" : " repeated");
            long[] t = tally.computeIfAbsent(key, k -> new long[4]);
            if (brute != curve.supNO()) {
                t[3]++;
            }
            if (brute == 0) {
                continue;
            }
            t[0]++;
            for (int target = 0; target <= brute; target++) {
                int least = BruteForce.minMasks(pattern, sequence, gap, target);
                t[1]++;
                if (least > 0) {
                    t[2]++;
                }
                if (target > curve.supNO() || least != curve.masks()[target]) {
                    t[3]++;
                }
            }
        }
        String[] header = {"gap", "items", "instances", "targets", "targets_needing_a_mask", "mismatches"};
        long mismatches = 0;
        try (CsvWriter writer = sink.open(ResultSink.Kind.INVARIANT, id(), header,
                new String[] {"options " + options.all()})) {
            for (Map.Entry<String, long[]> e : tally.entrySet()) {
                String[] parts = e.getKey().split(" ");
                long[] t = e.getValue();
                writer.writeRow(parts[0], parts[1], Long.toString(t[0]), Long.toString(t[1]),
                        Long.toString(t[2]), Long.toString(t[3]));
                System.out.printf("%-8s %-9s instances %5d  targets %6d  need a mask %6d  mismatches %d%n",
                        parts[0], parts[1], t[0], t[1], t[2], t[3]);
                mismatches += t[3];
            }
            writer.complete();
        }
        if (mismatches != 0) {
            throw new IllegalStateException(mismatches + " mismatches between the program and exhaustive search");
        }
    }
}
