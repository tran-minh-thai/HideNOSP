package hidenosp.exp;

import hidenosp.baseline.Dbsh;
import hidenosp.baseline.GaHybrid;
import hidenosp.baseline.Hha;
import hidenosp.baseline.HidingMethod;
import hidenosp.baseline.IlpExact;
import hidenosp.baseline.OccurrenceCountHeuristic;
import hidenosp.baseline.RandomMasking;
import hidenosp.baseline.Sbsh;
import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.io.CsvWriter;
import hidenosp.io.DatasetReader;
import hidenosp.io.ResultSink;
import hidenosp.metrics.DataProfile;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.metrics.Metrics;
import hidenosp.util.Cli;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * SC3 of section 11.1: the main comparison on HIDE-DB.
 *
 * <p>One row per instance and per method, where an instance is a triple of dataset, sigma and
 * sensitive set, which is the unit of repetition fixed by section 11.6. The columns are Lost,
 * Ghost, SupDist, |R| and HF, plus the ratio |R| over |R| star that answers RQ3 and the share of
 * cost pairs certified optimal by the lambda sweep. None of them depends on the machine, so the
 * rows go to {@code results-invariant/}; the timing of the same runs belongs to SC6.
 *
 * <p>Methods are selected by name, and asking for one that is not implemented yet is an error
 * rather than a silent omission, so a comparison table can never quietly lose a baseline.
 */
public final class Sc3MainComparison implements Scenario {

    @Override
    public String id() {
        return "SC3";
    }

    @Override
    public String title() {
        return "main comparison on HIDE-DB: Lost, Ghost, SupDist, |R| and HF against the baselines";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "maxPatternLength",
                "epsilon", "sensitiveSets", "sensitiveSize", "seed", "methods"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        GapConstraint gap = new GapConstraint(options.getInt("gmin", 0), options.getInt("gmax", 3));
        int sigma = options.getInt("sigma", 20);
        int maxPatternLength = options.getInt("maxPatternLength", 3);
        double epsilon = options.getDouble("epsilon", 0.5);
        int sensitiveSets = options.getInt("sensitiveSets", 5);
        int sensitiveSize = options.getInt("sensitiveSize", 1);
        long seed = options.getLong("seed", 2026);
        String methodList = options.get("methods", "cut-hide,cut-hide-uniform,occurrence,HH,RR");

        SequenceDatabase database = load(options, seed);
        Map<Pattern, Long> frequent = FrequentPatterns.mine(database, gap, maxPatternLength, sigma);
        if (frequent.isEmpty()) {
            throw new IllegalStateException("F_sigma(D) is empty at sigma = " + sigma
                    + "; lower sigma or widen the gap before running SC3");
        }
        List<List<Pattern>> sensitiveSetList =
                sampleSensitiveSets(frequent, sensitiveSets, sensitiveSize, seed);

        String[] header = {"dataset", "gap", "sigma", "sensitive_set", "method", "hf", "lost",
                "ghost", "dist_r", "sup_dist", "mask_count", "min_masks", "mask_ratio",
                "certified_pairs", "total_pairs", "frequent_before", "frequent_after"};

        try (CsvWriter writer = sink.open(ResultSink.Kind.INVARIANT, id(), header)) {
            for (int index = 0; index < sensitiveSetList.size(); index++) {
                List<Pattern> sensitive = sensitiveSetList.get(index);
                HideProblem problem = new HideProblem(database, sensitive, gap,
                        HideProblem.Mode.HIDE_DB, sigma, epsilon, maxPatternLength);
                long minMasks = boundFromP1(database, sensitive, gap, sigma);
                for (String name : methodList.split(",")) {
                    HidingMethod method = create(name.trim(), problem, frequent, sigma, epsilon, seed);
                    Sanitization sanitization = method.run(problem);
                    Metrics.Row row = Metrics.evaluate(problem, sanitization);
                    writer.writeRow(describe(database), gap.toString(), Integer.toString(sigma),
                            sensitive.toString(), method.name(),
                            format(row.hidingFailure()), Integer.toString(row.lost()),
                            Integer.toString(row.ghost()), format(row.distR()),
                            format(row.supDist()), Integer.toString(row.maskCount()),
                            Long.toString(minMasks),
                            minMasks == 0 ? "" : format((double) row.maskCount() / minMasks),
                            Long.toString(sanitization.certifiedPairs()),
                            Long.toString(sanitization.totalPairs()),
                            Integer.toString(row.frequentBefore()),
                            Integer.toString(row.frequentAfter()));
                    System.out.printf("set %d  %-34s |R| %4d  lost %4d  ghost %3d  supDist %.4f%n",
                            index, method.name(), row.maskCount(), row.lost(), row.ghost(),
                            row.supDist());
                }
            }
        }
    }

    /** |R| star for a single sensitive pattern, from the corollary of P1; 0 when it does not apply. */
    private long boundFromP1(SequenceDatabase database, List<Pattern> sensitive, GapConstraint gap,
                             int sigma) {
        if (sensitive.size() != 1 || !sensitive.get(0).hasDistinctItems()) {
            return 0;
        }
        return DataProfile.of(database, sensitive.get(0), gap, HideProblem.Mode.HIDE_DB, sigma)
                .minMasks();
    }

    private HidingMethod create(String name, HideProblem problem, Map<Pattern, Long> frequent,
                                int sigma, double epsilon, long seed) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "cut-hide": {
                Set<Pattern> closure =
                        FrequentPatterns.closure(problem.sensitive(), frequent.keySet());
                List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
                double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
                return new CutHide(CutHideConfig.standard()).withNearThreshold(band, factor);
            }
            case "cut-hide-uniform":
                return new CutHide(CutHideConfig.uniformWeights());
            case "occurrence":
                return new OccurrenceCountHeuristic();
            case "hh":
                return new RandomMasking(RandomMasking.SequenceChoice.HEURISTIC,
                        RandomMasking.PositionChoice.HEURISTIC, seed);
            case "hr":
                return new RandomMasking(RandomMasking.SequenceChoice.HEURISTIC,
                        RandomMasking.PositionChoice.RANDOM, seed);
            case "rh":
                return new RandomMasking(RandomMasking.SequenceChoice.RANDOM,
                        RandomMasking.PositionChoice.HEURISTIC, seed);
            case "rr":
                return new RandomMasking(RandomMasking.SequenceChoice.RANDOM,
                        RandomMasking.PositionChoice.RANDOM, seed);
            case "hha":
                return new Hha(false);
            case "dbsh":
                return new Dbsh();
            case "sbsh":
                return new Sbsh(1);
            case "ilp":
                return new IlpExact();
            case "ga":
                return new GaHybrid(true, seed);
            default:
                throw new IllegalArgumentException("unknown method " + name);
        }
    }

    private List<List<Pattern>> sampleSensitiveSets(Map<Pattern, Long> frequent, int count, int size,
                                                    long seed) {
        List<Pattern> pool = new ArrayList<>();
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            if (e.getKey().length() >= 2 && e.getKey().hasDistinctItems()) {
                pool.add(e.getKey());
            }
        }
        if (pool.isEmpty()) {
            throw new IllegalStateException("no frequent pattern of length at least two");
        }
        Random random = new Random(seed);
        List<List<Pattern>> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            List<Pattern> set = new ArrayList<>();
            List<Pattern> shuffled = new ArrayList<>(pool);
            java.util.Collections.shuffle(shuffled, random);
            for (int k = 0; k < Math.min(size, shuffled.size()); k++) {
                set.add(shuffled.get(k));
            }
            out.add(set);
        }
        return out;
    }

    private SequenceDatabase load(Cli options, long seed) {
        String dataset = options.get("dataset", "");
        if (dataset.isEmpty()) {
            Random random = new Random(seed);
            List<EventSequence> sequences = new ArrayList<>();
            for (int s = 0; s < 20; s++) {
                int[] events = new int[30];
                for (int i = 0; i < events.length; i++) {
                    events[i] = random.nextInt(4);
                }
                for (int copy = 0; copy < 3; copy++) {
                    int start = random.nextInt(events.length - 4);
                    events[start] = 0;
                    events[start + 2] = 1;
                }
                sequences.add(new EventSequence(s, events));
            }
            return new SequenceDatabase(sequences);
        }
        DatasetReader.Format format = DatasetReader.Format
                .valueOf(options.get("format", "PLAIN").toUpperCase(Locale.ROOT));
        return DatasetReader.read(Paths.get(dataset), format).database();
    }

    private String describe(SequenceDatabase database) {
        return "N=" + database.size() + ",len=" + database.totalLength();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

}
