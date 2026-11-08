// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;
import hidenosp.io.CsvWriter;
import hidenosp.io.DatasetReader;
import hidenosp.io.ResultSink;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.metrics.Metrics;
import hidenosp.util.Cli;
import hidenosp.util.Deadline;
import hidenosp.util.Timing;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * SC6, reduced to running time (preregistration section 19.3).
 *
 * <p>For one SC3 cell, the sets of SC3 are drawn again and the first set drawn in each stratum is kept.
 * Each method is built by the factory of SC3 and timed by {@link Timing#measure}: one warm-up run, one
 * run that sets the number of repetitions (both discarded), then the repetitions of protocol row 8.
 * F_sigma(D) is a preprocessing of the database shared by every hiding request on it
 * (preregistration section 20.1, which amends 19.3): it is timed once per cell on its own row {@code mine-frequent}, mined
 * once outside the clocks and handed to every method, and each timed call builds a fresh evaluator on it,
 * so no cache carries over between repetitions. The column {@code reads_frequent} says which methods need
 * it (the three CUT-HIDE keys and the two SBSH keys); the tables print their time beside the mining time,
 * never added into one number. Reading the file, drawing the set and evaluating the output are outside
 * every clock.
 *
 * <p>The output of the last timed run is evaluated once, outside the clock, and its |R| and Lost are
 * written beside the times: they must equal the SC3 row of the same set and method, which ties the
 * timed code to the code of the comparison tables.
 *
 * <p>Index cost: for each kept set, the time to build the index of every sequence carrying a sensitive
 * pattern, and the time to apply the masks of the {@code cut-hide} solution one by one with the
 * incremental update against rebuilding the index after each mask.
 *
 * <p>Memory is not measured (protocol row 6). The rows depend on the machine and go to results/, which
 * the sink allows only from a declared measuring machine.
 */
public final class Sc6Scalability implements Scenario {

    /**
     * The keys whose construction reads F_sigma(D). Mining it is not part of their clock: it is done once
     * per cell outside every clock and timed on its own row (preregistration section 20.1).
     */
    static final Set<String> READS_FREQUENT = Set.of("cut-hide", "cut-hide-rw", "cut-hide-rw-ls", "cut-hide-refine",
            "cut-hide-sweep", "cut-hide-weight", "sbsh", "sbsh-r1", "sbsh-rmax", "ilp-lost");

    static final String DEFAULT_METHODS =
            "cut-hide,cut-hide-rw,cut-hide-rw-ls,hha,dbsh,sbsh-r1,sbsh-rmax,occurrence,hha-stop";

    @Override
    public String id() {
        return "SC6";
    }

    @Override
    public String title() {
        return "running time of every method and cost of the index, one set per stratum";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "maxPatternLength", "epsilon",
                "setsPerStratum", "sensitiveSize", "seed", "patternKind", "methods", "timeLimit", "warmups"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        String dataset = options.get("dataset", "");
        if (dataset.isEmpty()) {
            throw new IllegalArgumentException("--dataset is required");
        }
        GapConstraint gap = new GapConstraint(options.getInt("gmin", 0), options.getInt("gmax", 3));
        int sigma = options.getInt("sigma", 20);
        int maxPatternLength = options.getInt("maxPatternLength", 4);
        double epsilon = options.getDouble("epsilon", 0.5);
        int perStratum = options.getInt("setsPerStratum", 4);
        int sensitiveSize = options.getInt("sensitiveSize", 1);
        long seed = options.getLong("seed", 2026);
        String patternKind = options.get("patternKind", "distinct");
        if (!patternKind.equals("distinct") && !patternKind.equals("repeated")) {
            throw new IllegalArgumentException("--patternKind is distinct or repeated, got " + patternKind);
        }
        String[] methods = options.get("methods", DEFAULT_METHODS).split(",");
        double timeLimit = options.getDouble("timeLimit", 1800);
        int warmups = options.getInt("warmups", 1);
        DatasetReader.Format format = DatasetReader.Format
                .valueOf(options.get("format", "PLAIN").toUpperCase(Locale.ROOT));
        SequenceDatabase database = DatasetReader.read(Paths.get(dataset), format).database();
        Metrics.Context full = Metrics.Context.of(database, gap, maxPatternLength, sigma);
        SensitiveSampling.Result sampling = SensitiveSampling.draw(full.frequent(), sensitiveSize, perStratum,
                seed, patternKind.equals("repeated"));
        System.out.println(sampling.describe());
        Map<Integer, SensitiveSampling.Draw> firstOfStratum = new LinkedHashMap<>();
        for (SensitiveSampling.Draw draw : sampling.draws()) {
            firstOfStratum.putIfAbsent(draw.percentile(), draw);
        }

        String[] header = {"dataset", "gap", "sigma", "pattern_kind", "sensitive_size", "sensitive_set", "stratum",
            "method_key", "method", "reads_frequent", "mask_count", "lost", "wall_ms_mean", "wall_ms_sd",
            "cpu_ms_mean", "repetitions", "warmups", "note", "status"};
        String[] provenance = {"options " + options.all(), sampling.describe(),
            "sets: the first set drawn in each stratum (" + firstOfStratum.size() + " of "
                    + sampling.draws().size() + ")"};
        Map<Pattern, Long> frequent = full.frequent();
        try (CsvWriter writer = sink.open(ResultSink.Kind.TIMED, id(), header, provenance)) {
            String[] cell = {dataset, gap.toString(), Integer.toString(sigma), patternKind,
                Integer.toString(sensitiveSize), "", ""};
            Timing.Measurement mining = Timing.measure(
                    () -> FrequentPatterns.mine(database, gap, maxPatternLength, sigma).size(), warmups);
            writer.writeRow(row(cell, "mine-frequent", "F_sigma(D) with l_max = " + maxPatternLength + ": "
                    + frequent.size() + " patterns", false, "", "", mining, ""));
            System.out.printf("mining F_sigma(D): %d patterns, %10.1f ms +- %.1f (%d repetitions)%n", frequent.size(),
                    mining.wallMillisMean(), mining.wallMillisStdDev(), mining.repetitions());
            for (SensitiveSampling.Draw draw : firstOfStratum.values()) {
                List<Pattern> sensitive = draw.patterns();
                HideProblem problem = new HideProblem(database, sensitive, gap, HideProblem.Mode.HIDE_DB, sigma,
                        epsilon, maxPatternLength);
                String[] base = {dataset, gap.toString(), Integer.toString(sigma), patternKind,
                    Integer.toString(sensitiveSize), sensitive.toString(), "p" + draw.percentile()};
                Sanitization cutHide = null;
                for (String raw : methods) {
                    String key = raw.trim().toLowerCase(Locale.ROOT);
                    boolean reads = READS_FREQUENT.contains(key);
                    AtomicReference<Sanitization> last = new AtomicReference<>();
                    Supplier<Sanitization> work = () -> Deadline.within(timeLimit, () -> {
                        Metrics.Context context = Metrics.Context.withFrequent(database, gap, maxPatternLength, sigma,
                                frequent);
                        Sanitization s = Sc3MainComparison.create(key, problem, frequent, sigma, epsilon, seed,
                                context).run(problem);
                        last.set(s);
                        return s;
                    });
                    Timing.Measurement m;
                    try {
                        m = Timing.measure(work, warmups);
                    } catch (Deadline.Exceeded e) {
                        writer.writeRow(row(base, key, "", reads, "", "", null, "OT"));
                        continue;
                    } catch (OutOfMemoryError e) {
                        writer.writeRow(row(base, key, "", reads, "", "", null, "OOM"));
                        continue;
                    } catch (RuntimeException e) {
                        writer.writeRow(row(base, key, "", reads, "", "", null,
                                "method failed: " + String.valueOf(e.getMessage()).replace(',', ';')));
                        continue;
                    }
                    Sanitization s = last.get();
                    Metrics.Row measured = full.evaluateMasking(problem, s);
                    if (key.equals("cut-hide")) {
                        cutHide = s;
                    }
                    writer.writeRow(row(base, key, s.method(), reads, Integer.toString(measured.maskCount()),
                            Integer.toString(measured.lost()), m, measured.hidingFailure() > 0 ? "does not hide" : ""));
                    System.out.printf("%s %-16s %10.1f ms +- %.1f (%d repetitions)%n", base[6], key,
                            m.wallMillisMean(), m.wallMillisStdDev(), m.repetitions());
                }
                if (cutHide != null) {
                    indexCost(writer, base, database, sensitive, gap, cutHide, warmups);
                }
            }
            writer.complete();
        }
    }

    /** Build time of the index over every sequence, and the masks of a solution applied incrementally against a rebuild per mask. */
    private static void indexCost(CsvWriter writer, String[] base, SequenceDatabase database, List<Pattern> sensitive,
                                  GapConstraint gap, Sanitization solution, int warmups) {
        Map<Integer, Set<Integer>> masks = solution.masked();
        Timing.Measurement build = Timing.measure(() -> {
            long built = 0;
            for (int s = 0; s < database.size(); s++) {
                built += index(database.get(s), sensitive, gap).patternCount();
            }
            return built;
        }, warmups);
        writer.writeRow(row(base, "index-build", "NettreeCutIndex", false, "", "", build, ""));
        Timing.Measurement incremental = Timing.measure(() -> {
            long left = 0;
            for (Map.Entry<Integer, Set<Integer>> e : masks.entrySet()) {
                NettreeCutIndex index = index(database.get(e.getKey()), sensitive, gap);
                for (int position : new TreeSet<>(e.getValue())) {
                    index.maskPosition(position);
                }
                left += index.supNO(0);
            }
            return left;
        }, warmups);
        writer.writeRow(row(base, "index-incremental", "maskPosition after each mask", false,
                Integer.toString(solution.maskCount()), "", incremental, ""));
        Timing.Measurement rebuild = Timing.measure(() -> {
            long left = 0;
            for (Map.Entry<Integer, Set<Integer>> e : masks.entrySet()) {
                EventSequence sequence = database.get(e.getKey());
                int[] events = sequence.events().clone();
                NettreeCutIndex index = null;
                for (int position : new TreeSet<>(e.getValue())) {
                    events[position] = EventSequence.MASK;
                    index = index(new EventSequence(sequence.id(), events), sensitive, gap);
                }
                left += index == null ? 0 : index.supNO(0);
            }
            return left;
        }, warmups);
        writer.writeRow(row(base, "index-rebuild", "rebuild after each mask", false,
                Integer.toString(solution.maskCount()), "", rebuild, ""));
    }

    private static NettreeCutIndex index(EventSequence sequence, List<Pattern> sensitive, GapConstraint gap) {
        NettreeCutIndex index = new NettreeCutIndex(sequence, gap);
        for (Pattern p : sensitive) {
            index.add(p, true);
        }
        return index;
    }

    private static String[] row(String[] base, String key, String method, boolean reads, String masks, String lost,
                                Timing.Measurement m, String status) {
        String[] out = Arrays.copyOf(base, base.length + 12);
        int k = base.length;
        out[k] = key;
        out[k + 1] = method;
        out[k + 2] = Boolean.toString(reads);
        out[k + 3] = masks;
        out[k + 4] = lost;
        out[k + 5] = m == null ? "" : String.format(Locale.ROOT, "%.3f", m.wallMillisMean());
        out[k + 6] = m == null ? "" : String.format(Locale.ROOT, "%.3f", m.wallMillisStdDev());
        out[k + 7] = m == null || m.cpuMillisMean() < 0 ? "" : String.format(Locale.ROOT, "%.3f", m.cpuMillisMean());
        out[k + 8] = m == null ? "" : Integer.toString(m.repetitions());
        out[k + 9] = m == null ? "" : Integer.toString(m.warmups());
        out[k + 10] = m == null ? "" : m.note();
        out[k + 11] = status;
        return out;
    }
}
