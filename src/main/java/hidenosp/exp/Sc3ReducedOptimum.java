// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.baseline.SequenceSplitExact;
import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.io.CsvWriter;
import hidenosp.io.DatasetReader;
import hidenosp.io.ResultSink;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.metrics.Metrics;
import hidenosp.util.Cli;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Where the distance between CUT-HIDE and the lower bound comes from, for several sensitive
 * patterns: from the bound, or from the method. On the full databases the optimum is out of reach,
 * so this scenario builds small instances from them and solves those exactly.
 *
 * <p>Each sensitive set of SC3 is drawn again (same pool, same strata, same seed, so the rows join
 * SC3 rows on the set). For each set, an instance keeps the patterns and the gap and takes a few
 * real sequences of the dataset: sequences in a seeded random order, kept when at least one pattern
 * occurs in them, until {@code sequencesPerInstance} are kept (a list of sizes gives one instance per
 * size, from the same seeded order, so a larger instance extends a smaller one). A sequence with more than
 * {@code maxCandidates} candidate positions is replaced by a contiguous window of itself (see
 * {@link #window}); occurrences under a gap constraint are local, so a window keeps the local
 * structure of the data, and the row counts the windows. Its threshold t is drawn between half the smallest support
 * of a pattern present and that support, and raised while the exact program would need more than
 * {@link SequenceSplitExact#MAX_STATES} states. On the instance the row records the component bound,
 * the conflict bound, the optimum ({@link SequenceSplitExact}), the greedy of step 7 and the greedy
 * with the refinement of proposition P15, with the side-effect weights of SC3 when the reduced
 * database has a near-threshold band and the uniform weights otherwise (the row says which).
 *
 * <p>The instance is not the full problem: short sequences or windows only, and a threshold of its own. The
 * report therefore puts the ratio method/bound measured here next to the ratio on the full data,
 * and reads the split only where the two are of the same size.
 */
public final class Sc3ReducedOptimum implements Scenario {

    @Override
    public String id() {
        return "SC3OPT";
    }

    @Override
    public String title() {
        return "optimum on small instances built from the SC3 sensitive sets: bound, optimum, greedy, refined";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "maxPatternLength", "epsilon",
                "setsPerStratum", "sensitiveSize", "seed", "patternKind", "sequencesPerInstance",
                "maxCandidates", "instancesPerSet", "instanceSeed", "closure"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        // Which frequent patterns count as hidden with the sensitive ones (FrequentPatterns.Closure); written
        // into the header even at the default, so a file without the line was written under the subsequence rule.
        FrequentPatterns.Closure closureRule = FrequentPatterns.Closure.parse(options.get("closure", "necessary"));
        String dataset = options.get("dataset", "");
        if (dataset.isEmpty()) {
            throw new IllegalArgumentException("--dataset is required: the instances are built from real sequences");
        }
        GapConstraint gap = new GapConstraint(options.getInt("gmin", 0), options.getInt("gmax", 3));
        int sigma = options.getInt("sigma", 20);
        int maxPatternLength = options.getInt("maxPatternLength", 3);
        double epsilon = options.getDouble("epsilon", 0.5);
        int perStratum = options.getInt("setsPerStratum", 4);
        int sensitiveSize = options.getInt("sensitiveSize", 5);
        long seed = options.getLong("seed", 2026);
        String patternKind = options.get("patternKind", "distinct");
        if (!patternKind.equals("distinct") && !patternKind.equals("repeated")) {
            throw new IllegalArgumentException("--patternKind is distinct or repeated, got " + patternKind);
        }
        // One or more instance sizes, e.g. "8,16": the same sets and the same seeded order of
        // sequences, so F_sigma(D) is mined once and the larger instance extends the smaller one.
        int[] sizes = java.util.Arrays.stream(options.get("sequencesPerInstance", "8").split(","))
                .map(String::trim).mapToInt(Integer::parseInt).toArray();
        int maxCandidates = options.getInt("maxCandidates", 14);
        if (maxCandidates > SequenceSplitExact.MAX_CANDIDATES_PER_SEQUENCE) {
            throw new IllegalArgumentException("--maxCandidates is at most "
                    + SequenceSplitExact.MAX_CANDIDATES_PER_SEQUENCE + ", got " + maxCandidates);
        }
        int instancesPerSet = options.getInt("instancesPerSet", 1);
        long instanceSeed = options.getLong("instanceSeed", 20261013L);
        DatasetReader.Format format = DatasetReader.Format
                .valueOf(options.get("format", "PLAIN").toUpperCase(Locale.ROOT));
        SequenceDatabase database = DatasetReader.read(Paths.get(dataset), format).database();
        Metrics.Context context = Metrics.Context.of(database, gap, maxPatternLength, sigma);
        SensitiveSampling.Result sampling = SensitiveSampling.draw(context.frequent(), sensitiveSize,
                perStratum, seed, patternKind.equals("repeated"));
        System.out.println(sampling.describe());

        String[] header = {"dataset", "gap", "sigma", "pattern_kind", "sensitive_size", "sensitive_set",
            "stratum", "instance", "sequences", "candidates_max", "threshold", "binding_patterns", "needs",
            "components", "component_bound", "conflict_bound", "optimum", "greedy", "refined", "weights",
            "windows", "requested_sequences", "status"};
        String[] provenance = {"options " + options.all(), sampling.describe(), "closure " + closureRule.label()};
        int written = 0;
        int solved = 0;
        long[] clock = new long[1];
        try (CsvWriter writer = sink.open(ResultSink.Kind.INVARIANT, id(), header, provenance)) {
            List<SensitiveSampling.Draw> draws = sampling.draws();
            for (int d = 0; d < draws.size(); d++) {
                List<Pattern> sensitive = draws.get(d).patterns();
                for (int keep : sizes) {
                    for (int r = 0; r < instancesPerSet; r++) {
                        Random random = new Random(instanceSeed + 1_000_003L * d + r);
                        String[] row = new String[header.length];
                        java.util.Arrays.fill(row, "");
                        row[0] = dataset;
                        row[1] = gap.toString();
                        row[2] = Integer.toString(sigma);
                        row[3] = patternKind;
                        row[4] = Integer.toString(sensitiveSize);
                        row[5] = sensitive.toString();
                        row[6] = "p" + draws.get(d).percentile();
                        row[7] = Integer.toString(r);
                        String status = solve(database, sensitive, gap, maxPatternLength, epsilon, keep,
                                maxCandidates, random, row, clock, closureRule);
                        row[21] = Integer.toString(keep);
                        row[22] = status;
                        solved += status.isEmpty() ? 1 : 0;
                        writer.writeRow(row);
                        written++;
                    }
                }
            }
            writer.complete();
        }
        System.out.printf("%d instances written, %d solved; exact method %.1f s in total (console only)%n",
                written, solved, clock[0] / 1e9);
    }

    /**
     * Builds and solves one instance into {@code row}; returns "" or the reason it was skipped. The
     * time of the exact method is added to {@code clock[0]} for the console, never to the file.
     */
    private static String solve(SequenceDatabase database, List<Pattern> sensitive, GapConstraint gap,
                                int maxPatternLength, double epsilon, int keep, int maxCandidates,
                                Random random, String[] row, long[] clock, FrequentPatterns.Closure closureRule) {
        List<Integer> order = new ArrayList<>();
        for (int s = 0; s < database.size(); s++) {
            order.add(s);
        }
        Collections.shuffle(order, random);
        List<EventSequence> kept = new ArrayList<>();
        int widest = 0;
        int windows = 0;
        for (int s : order) {
            if (kept.size() == keep) {
                break;
            }
            EventSequence sequence = database.get(s);
            boolean occurs = false;
            for (Pattern p : sensitive) {
                occurs |= SupportNO.of(p, sequence, gap) > 0;
            }
            if (!occurs) {
                continue;
            }
            int[] candidates = SequenceSplitExact.candidates(sensitive, sequence, gap);
            int[] events = sequence.events();
            if (candidates.length > maxCandidates) {
                events = window(sequence, sensitive, gap, maxCandidates, candidates, random);
                if (events == null) {
                    continue;
                }
                windows++;
            }
            EventSequence unit = new EventSequence(kept.size(), events);
            widest = Math.max(widest, SequenceSplitExact.candidates(sensitive, unit, gap).length);
            kept.add(unit);
        }
        row[8] = Integer.toString(kept.size());
        row[9] = Integer.toString(widest);
        row[20] = Integer.toString(windows);
        if (kept.size() < 2) {
            return "fewer than two sequences or windows with an occurrence";
        }
        SequenceDatabase reduced = new SequenceDatabase(kept);
        long smallest = Long.MAX_VALUE;
        long largest = 0;
        for (Pattern p : sensitive) {
            long support = SupportNO.of(p, reduced, gap);
            if (support > 0) {
                smallest = Math.min(smallest, support);
                largest = Math.max(largest, support);
            }
        }
        long low = Math.max(1, (smallest + 1) / 2);
        int threshold = (int) (low + random.nextInt((int) (smallest - low + 1)));
        HideProblem problem = new HideProblem(reduced, sensitive, gap, HideProblem.Mode.HIDE_DB, threshold,
                epsilon, maxPatternLength).withClosure(closureRule);
        while (SequenceSplitExact.states(problem) > SequenceSplitExact.MAX_STATES && threshold < largest) {
            threshold++;
            problem = new HideProblem(reduced, sensitive, gap, HideProblem.Mode.HIDE_DB, threshold, epsilon,
                    maxPatternLength).withClosure(closureRule);
        }
        row[10] = Integer.toString(threshold);
        StringJoiner needs = new StringJoiner(";");
        int binding = 0;
        for (Pattern p : sensitive) {
            long need = Math.max(0, SupportNO.of(p, reduced, gap) - threshold + 1);
            needs.add(Long.toString(need));
            binding += need > 0 ? 1 : 0;
        }
        row[11] = Integer.toString(binding);
        row[12] = needs.toString();
        if (binding < 2) {
            return "fewer than two patterns at or above the threshold";
        }
        if (SequenceSplitExact.states(problem) > SequenceSplitExact.MAX_STATES) {
            return "too many states for the exact method";
        }
        Sc3LowerBound.Bound bound = Sc3LowerBound.bound(reduced, sensitive, gap, HideProblem.Mode.HIDE_DB,
                threshold);
        if (!bound.optimaExact()) {
            return "a single-pattern optimum is not exact";
        }
        long start = System.nanoTime();
        int optimum = new SequenceSplitExact().run(problem).maskCount();
        clock[0] += System.nanoTime() - start;

        Map<Pattern, Long> frequent = Metrics.Context.of(reduced, gap, maxPatternLength, threshold).frequent();
        Set<Pattern> closure = problem.closureOf(frequent.keySet());
        List<Pattern> band = FrequentPatterns.nearThreshold(frequent, threshold, epsilon, closure);
        String weights = band.isEmpty() ? "uniform" : "side-effect";
        Sanitization greedy = method(band, frequent, threshold, false).run(problem);
        Sanitization refined = method(band, frequent, threshold, true).run(problem);
        for (Sanitization s : List.of(greedy, refined)) {
            for (Pattern p : sensitive) {
                if (SupportNO.of(p, s.sanitized(), gap) >= threshold) {
                    throw new IllegalStateException(s.method() + " leaves " + p + " visible on " + reduced);
                }
            }
        }
        if (bound.conflictBound() > optimum || optimum > refined.maskCount()
                || refined.maskCount() > greedy.maskCount()) {
            throw new IllegalStateException("order bound <= optimum <= refined <= greedy broken: "
                    + bound.conflictBound() + ", " + optimum + ", " + refined.maskCount() + ", "
                    + greedy.maskCount() + " on " + sensitive + " in " + reduced);
        }
        row[13] = Integer.toString(bound.components());
        row[14] = Long.toString(bound.lowerBound());
        row[15] = Long.toString(bound.conflictBound());
        row[16] = Integer.toString(optimum);
        row[17] = Integer.toString(greedy.maskCount());
        row[18] = Integer.toString(refined.maskCount());
        row[19] = weights;
        return "";
    }

    /**
     * A window of a sequence with too many candidate positions: it starts at a candidate position
     * drawn at random and is the longest that still carries at most {@code maxCandidates} candidates
     * (their number only grows as the window grows to the right, since a node keeps its parents).
     * Null when that window holds no occurrence of a sensitive pattern.
     */
    private static int[] window(EventSequence sequence, List<Pattern> sensitive, GapConstraint gap,
                                int maxCandidates, int[] candidates, Random random) {
        int[] events = sequence.events();
        int start = candidates[random.nextInt(candidates.length)];
        int low = start + 1;
        int high = events.length;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            EventSequence piece = new EventSequence(0, java.util.Arrays.copyOfRange(events, start, middle));
            if (SequenceSplitExact.candidates(sensitive, piece, gap).length <= maxCandidates) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        int[] piece = java.util.Arrays.copyOfRange(events, start, low);
        EventSequence unit = new EventSequence(0, piece);
        for (Pattern p : sensitive) {
            if (SupportNO.of(p, unit, gap) > 0) {
                return piece;
            }
        }
        return null;
    }

    private static CutHide method(List<Pattern> band, Map<Pattern, Long> frequent, long threshold, boolean refine) {
        CutHideConfig config = refine ? CutHideConfig.standard().withRefineSeveral() : CutHideConfig.standard();
        return CutHide.forBand(config, band, FrequentPatterns.weightFactors(band, frequent, threshold));
    }
}
