// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.core.BruteForce;
import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.core.flow.NettreeFlow;
import hidenosp.io.CsvWriter;
import hidenosp.io.ResultSink;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.util.Cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * SC1: the implementation check. Every quantity here is a check, not a finding, and
 * the only acceptable result is zero mismatches.
 *
 * <p>It repeats on small random instances what the probe script {@code nettree_checks.py}
 * established in Python, so a difference between the two implementations shows up here rather
 * than inside a result table:
 *
 * <ul>
 *   <li>L0: the max flow equals the exhaustive sup_NO, and a minimum cut has exactly that size;</li>
 *   <li>P1: for a distinct-item pattern the least number of masks reaching t is F - t;</li>
 *   <li>L5: a node is vital exactly when masking its position lowers sup_NO;</li>
 *   <li>the incremental update after a mask agrees with a rebuild from scratch, which is the
 *       correctness claim behind the Nettree cut index;</li>
 *   <li>P5: masking never raises sup_NO, while deletion can once the maximum gap is finite;</li>
 *   <li>the level-wise mining of F_sigma with prefix pruning agrees with plain enumeration.</li>
 * </ul>
 *
 * <p>TODO(code): add the comparison against NETGAP of [R1], which the design doc lists as the other
 * half of this check. It needs the reference implementation, so it waits until the library version
 * is declared in the protocol table.
 */
public final class Sc1ImplementationCheck implements Scenario {

    @Override
    public String id() {
        return "SC1";
    }

    @Override
    public String title() {
        return "implementation check: flow against exhaustive sup_NO, P1, L5, incremental update";
    }

    @Override
    public String[] options() {
        return new String[] {"trials", "seed", "maxLength", "maxPatternLength", "alphabet"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        int trials = options.getInt("trials", 400);
        long seed = options.getLong("seed", 2026);
        int maxLength = options.getInt("maxLength", 9);
        int maxPatternLength = options.getInt("maxPatternLength", 4);
        int alphabet = options.getInt("alphabet", 3);

        Random random = new Random(seed);
        Map<String, long[]> counters = new LinkedHashMap<>();
        observations.clear();
        for (int trial = 0; trial < trials; trial++) {
            GapConstraint gap = randomGap(random);
            EventSequence sequence = randomSequence(random, maxLength, alphabet, trial);
            Pattern pattern = randomPattern(random, maxPatternLength, alphabet, trial % 4 == 0);
            checkInstance(sequence, pattern, gap, counters);
        }
        checkMining(random, alphabet, maxPatternLength, counters);

        String[] header = {"kind", "check", "instances", "mismatches"};
        try (CsvWriter writer = sink.open(ResultSink.Kind.INVARIANT, id(), header)) {
            long failures = 0;
            for (Map.Entry<String, long[]> e : counters.entrySet()) {
                boolean isCheck = !observations.contains(e.getKey());
                writer.writeRow(isCheck ? "check" : "observation", e.getKey(),
                        Long.toString(e.getValue()[0]), Long.toString(e.getValue()[1]));
                if (isCheck) {
                    failures += e.getValue()[1];
                }
                System.out.printf("%-11s %-52s %7d instances %7d %s%n",
                        isCheck ? "check" : "observation", e.getKey(),
                        e.getValue()[0], e.getValue()[1], isCheck ? "mismatches" : "cases");
            }
            // Every counter is written: the file is complete even when it records mismatches.
            writer.complete();
            System.out.println(failures == 0
                    ? "SC1 passed: no mismatch"
                    : "SC1 FAILED: " + failures + " mismatches, see the file in results-invariant/");
            if (failures != 0) {
                throw new IllegalStateException("SC1 found " + failures + " mismatches");
            }
        }
    }

    private void checkInstance(EventSequence sequence, Pattern pattern, GapConstraint gap,
                               Map<String, long[]> counters) {
        Nettree nettree = Nettree.build(pattern, sequence, gap);
        NettreeFlow flow = new NettreeFlow(nettree);
        int viaFlow = flow.supNO();
        int viaBrute = BruteForce.supNO(pattern, sequence, gap);
        count(counters, "L0 flow equals brute force", viaFlow != viaBrute);

        boolean[] cut = flow.minCut();
        int cutSize = 0;
        for (boolean b : cut) {
            if (b) {
                cutSize++;
            }
        }
        count(counters, "L0 minimum cut size", cutSize != viaFlow);

        if (pattern.hasDistinctItems() && viaFlow > 0) {
            for (int t = 0; t < viaFlow; t++) {
                int least = BruteForce.minMasks(pattern, sequence, gap, t);
                count(counters, "P1 least masks equals F minus t", least != viaFlow - t);
            }
        }

        boolean[] vital = flow.vitalNodes();
        for (int v = 0; v < nettree.nodeCount(); v++) {
            int position = nettree.position(v);
            int after = SupportNO.of(pattern, sequence.mask(position), gap);
            boolean lowers = after < viaFlow;
            boolean expected = vital[v];
            if (!pattern.hasDistinctItems()) {
                // A position may carry several nodes, so it can lower the support by more than one
                // and the node-level statement of L5 no longer transfers to positions directly.
                continue;
            }
            count(counters, "L5 vital node lowers sup_NO", expected != lowers);
        }

        if (viaFlow > 0) {
            NettreeFlow incremental = new NettreeFlow(Nettree.build(pattern, sequence, gap));
            EventSequence masked = sequence;
            int[] order = flow.vitalPositions();
            for (int position : order) {
                incremental.maskPosition(position);
                masked = masked.mask(position);
                int rebuilt = SupportNO.of(pattern, masked, gap);
                count(counters, "incremental update equals rebuild", incremental.supNO() != rebuilt);
            }
        }

        SequenceDatabase single = new SequenceDatabase(List.of(sequence));
        int supportBefore = SupportNO.of(pattern, sequence, gap);
        for (int position = 0; position < sequence.length(); position++) {
            SequenceDatabase maskedDb = single.mask(Map.of(0, List.of(position)));
            count(counters, "P5a masking never raises sup_NO",
                    SupportNO.of(pattern, maskedDb, gap) > supportBefore);
            SequenceDatabase deletedDb = single.delete(Map.of(0, List.of(position)));
            boolean raised = SupportNO.of(pattern, deletedDb, gap) > supportBefore;
            if (gap.isUnbounded()) {
                count(counters, "P5b deletion is monotone when the gap is unbounded", raised);
            } else {
                // Not a check: with a finite maximum gap, deletion is expected to create
                // occurrences. The count is the observation the deletion arm of SC5 reports.
                observe(counters, "P5b deletion raised sup_NO, finite maximum gap", raised);
            }
        }
    }

    /**
     * The level-wise mining with prefix pruning against plain enumeration, at the same l_max the
     * paper uses. Checking at a shorter length would leave the length actually used unchecked.
     */
    private void checkMining(Random random, int alphabet, int maxLength, Map<String, long[]> counters) {
        for (int trial = 0; trial < 40; trial++) {
            List<EventSequence> sequences = new ArrayList<>();
            for (int s = 0; s < 4; s++) {
                sequences.add(randomSequence(random, 8, alphabet, s));
            }
            SequenceDatabase database = new SequenceDatabase(sequences);
            GapConstraint gap = randomGap(random);
            long sigma = 1 + random.nextInt(3);
            Map<Pattern, Long> pruned = FrequentPatterns.mine(database, gap, maxLength, sigma);
            Map<Pattern, Long> plain =
                    FrequentPatterns.enumerateWithoutPruning(database, gap, maxLength, sigma);
            count(counters, "F_sigma pruning equals enumeration", !pruned.equals(plain));
        }
    }

    /** Keys that record an observation rather than a check; they never make the scenario fail. */
    private final java.util.Set<String> observations = new java.util.LinkedHashSet<>();

    private void observe(Map<String, long[]> counters, String key, boolean happened) {
        observations.add(key);
        long[] entry = counters.computeIfAbsent(key, k -> new long[2]);
        entry[0]++;
        if (happened) {
            entry[1]++;
        }
    }

    private static void count(Map<String, long[]> counters, String key, boolean failed) {
        long[] entry = counters.computeIfAbsent(key, k -> new long[2]);
        entry[0]++;
        if (failed) {
            entry[1]++;
        }
    }

    private static GapConstraint randomGap(Random random) {
        int gmin = random.nextInt(2);
        int choice = random.nextInt(4);
        return choice == 3 ? GapConstraint.unbounded(gmin) : new GapConstraint(gmin, gmin + choice);
    }

    private static EventSequence randomSequence(Random random, int maxLength, int alphabet, int id) {
        int length = 1 + random.nextInt(maxLength);
        int[] events = new int[length];
        for (int i = 0; i < length; i++) {
            events[i] = random.nextInt(alphabet);
        }
        return new EventSequence(id, events);
    }

    private static Pattern randomPattern(Random random, int maxLength, int alphabet, boolean allowRepeats) {
        int length = 1 + random.nextInt(maxLength);
        int[] items = new int[length];
        if (allowRepeats) {
            for (int j = 0; j < length; j++) {
                items[j] = random.nextInt(alphabet);
            }
            return new Pattern(items);
        }
        List<Integer> pool = new ArrayList<>();
        for (int item = 0; item < alphabet; item++) {
            pool.add(item);
        }
        java.util.Collections.shuffle(pool, random);
        length = Math.min(length, pool.size());
        items = new int[length];
        for (int j = 0; j < length; j++) {
            items[j] = pool.get(j);
        }
        return new Pattern(items);
    }
}
