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
import hidenosp.hide.AdaptiveCutHide;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.hide.MaskLocalSearch;
import hidenosp.hide.PrimalDualCover;
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
                "epsilon", "setsPerStratum", "sensitiveSize", "seed", "methodSeed", "repeats",
                "methods", "patternKind", "pareto"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        GapConstraint gap = new GapConstraint(options.getInt("gmin", 0), options.getInt("gmax", 3));
        int sigma = options.getInt("sigma", 20);
        int maxPatternLength = options.getInt("maxPatternLength", 3);
        double epsilon = options.getDouble("epsilon", 0.5);
        // Five strata of sup_NO (percentiles 10 to 90), this many distinct sets from each: the
        // protocol fixes 4, i.e. 20 sets per cell when the pool allows it.
        int perStratum = options.getInt("setsPerStratum", 4);
        int sensitiveSize = options.getInt("sensitiveSize", 1);
        long seed = options.getLong("seed", 2026);
        // The seed of the methods is kept apart from the seed that draws the sensitive sets. If the
        // two shared one number, changing the seed would also change the sensitive sets drawn, so
        // the ten reruns could no longer be paired with one another and the rule "median over ten
        // seeds" of the preregistration could not be carried out.
        long methodSeed = options.getLong("methodSeed", seed);
        int repeats = options.getInt("repeats", 1);
        String methodList = options.get("methods", "cut-hide,cut-hide-uniform,occurrence,HH,RR");
        // Proposition P10: also write the exact convex Pareto front of CUT-HIDE between |R| and the
        // surrogate weight, one row per vertex, into SC3PARETO_<stamp>.csv (preregistration
        // section 11, point 6: descriptive). One sensitive pattern and g- = 0 only.
        boolean pareto = options.getBoolean("pareto", false);
        // distinct (the main scope, mu(P) = 1) or repeated (the repeated-item scope of theorem P8).
        String patternKind = options.get("patternKind", "distinct");
        if (!patternKind.equals("distinct") && !patternKind.equals("repeated")) {
            throw new IllegalArgumentException("--patternKind is distinct or repeated, got "
                    + patternKind);
        }

        SequenceDatabase database = load(options, seed);
        // F_sigma(D) is mined ONCE for the whole configuration, not once per evaluation: see the
        // Javadoc of Metrics.Context.
        Metrics.Context context = Metrics.Context.of(database, gap, maxPatternLength, sigma);
        Map<Pattern, Long> frequent = context.frequent();
        if (frequent.isEmpty()) {
            throw new IllegalStateException("F_sigma(D) is empty at sigma = " + sigma
                    + "; lower sigma or widen the gap before running SC3");
        }
        SensitiveSampling.Result sampling = SensitiveSampling.draw(frequent, sensitiveSize,
                perStratum, seed, patternKind.equals("repeated"));
        System.out.println(sampling.describe());
        if (sampling.draws().isEmpty()) {
            throw new IllegalStateException("no sensitive set of size " + sensitiveSize
                    + " can be drawn in this configuration, so the cell does not exist: "
                    + sampling.describe());
        }
        if (sampling.draws().size() < sampling.requested()) {
            System.out.println("WARNING: the pool gives " + sampling.draws().size() + " distinct sets, "
                    + "not " + sampling.requested() + "; the cell runs with these and the result file "
                    + "header says so. No set is repeated to make up the number.");
        }
        List<SensitiveSampling.Draw> draws = sampling.draws();

        String[] header = {"dataset", "shape", "gap", "sigma", "sensitive_set", "stratum", "method_key",
                "method", "method_seed",
                "hf", "lost", "lost_rel", "ghost", "dist_r", "sup_dist", "mask_count", "min_masks",
                "mask_ratio", "certified_pairs", "total_pairs", "lower_bound", "achieved_ratio",
                "frequent_before", "nonsensitive_before", "frequent_after"};

        String[] provenance = {"options " + options.all(), sampling.describe()};
        if (pareto && (sensitiveSize != 1 || gap.gmin() != 0)) {
            throw new IllegalArgumentException("--pareto needs --sensitiveSize=1 and g- = 0, got "
                    + sensitiveSize + " and " + gap);
        }
        String[] frontHeader = {"dataset", "shape", "gap", "sigma", "sensitive_set", "stratum",
                "vertex", "vertices", "mask_count", "weight", "min_masks", "mask_ratio", "hf",
                "lost", "lost_rel", "ghost", "sup_dist", "frequent_before", "nonsensitive_before",
                "frequent_after"};
        try (CsvWriter writer = sink.open(ResultSink.Kind.INVARIANT, id(), header, provenance);
             CsvWriter front = pareto
                     ? sink.open(ResultSink.Kind.INVARIANT, id() + "PARETO", frontHeader, provenance)
                     : null) {
            for (int index = 0; index < draws.size(); index++) {
                List<Pattern> sensitive = draws.get(index).patterns();
                String stratum = "p" + draws.get(index).percentile();
                HideProblem problem = new HideProblem(database, sensitive, gap,
                        HideProblem.Mode.HIDE_DB, sigma, epsilon, maxPatternLength);
                long minMasks = boundFromP1(database, sensitive, gap, sigma);
                if (front != null) {
                    CutHide cutHide = (CutHide) create("cut-hide", problem, frequent, sigma, epsilon,
                            methodSeed, context);
                    List<CutHide.ParetoVertex> vertices = cutHide.paretoFront(problem);
                    // Adjacent vertices differ in few sequences: one session remembers the rest.
                    Metrics.Context.Session session = context.session(problem);
                    for (int k = 0; k < vertices.size(); k++) {
                        CutHide.ParetoVertex v = vertices.get(k);
                        Metrics.Row row = session.evaluate(v.sanitization());
                        front.writeRow(describe(options), shape(database), gap.toString(),
                                Integer.toString(sigma), sensitive.toString(), stratum,
                                Integer.toString(k), Integer.toString(vertices.size()),
                                Integer.toString(row.maskCount()), Long.toString(v.weight()),
                                Long.toString(minMasks),
                                minMasks == 0 ? "" : format((double) row.maskCount() / minMasks),
                                format(row.hidingFailure()), Integer.toString(row.lost()),
                                format(row.lostRelative()), Integer.toString(row.ghost()),
                                format(row.supDist()), Integer.toString(row.frequentBefore()),
                                Integer.toString(row.nonSensitiveBefore()),
                                Integer.toString(row.frequentAfter()));
                    }
                    System.out.printf("set %d  Pareto front: %d vertices, |R| from %d to %d%n",
                            index, vertices.size(), vertices.get(0).masks(),
                            vertices.get(vertices.size() - 1).masks());
                }
                for (String name : methodList.split(",")) {
                    // A deterministic method runs once; a randomized method runs --repeats
                    // times with consecutive seeds, one row per run. Taking the median belongs
                    // to the analysis and is not done here: with one row per run the table stays
                    // complete enough to be checked again.
                    int runs = create(name.trim(), problem, frequent, sigma, epsilon, methodSeed,
                            context)
                            .isRandomized() ? repeats : 1;
                    for (int k = 0; k < runs; k++) {
                        long thisSeed = methodSeed + k;
                        HidingMethod method =
                                create(name.trim(), problem, frequent, sigma, epsilon, thisSeed,
                                        context);
                        Sanitization sanitization = method.run(problem);
                        Metrics.Row row = context.evaluateMasking(problem, sanitization);
                        // method_key is the name given on the command line, and it is stable;
                        // method is the name the method gives itself, which may carry its
                        // parameters. SBSH-rmax calls itself SBSH-r3 when l_max = 3, so the
                        // analysis must key on method_key, or the locked Holm family never
                        // matches a name in the file.
                        writer.writeRow(describe(options), shape(database),
                                gap.toString(), Integer.toString(sigma),
                                sensitive.toString(), stratum,
                                name.trim().toLowerCase(Locale.ROOT), method.name(),
                                method.isRandomized() ? Long.toString(thisSeed) : "",
                                format(row.hidingFailure()), Integer.toString(row.lost()),
                                format(row.lostRelative()),
                                Integer.toString(row.ghost()), format(row.distR()),
                                format(row.supDist()), Integer.toString(row.maskCount()),
                                Long.toString(minMasks),
                                minMasks == 0 ? "" : format((double) row.maskCount() / minMasks),
                                Long.toString(sanitization.certifiedPairs()),
                                Long.toString(sanitization.totalPairs()),
                                Long.toString(sanitization.certifiedLowerBound()),
                                sanitization.certifiedLowerBound() == 0 ? ""
                                        : format((double) row.maskCount()
                                                / sanitization.certifiedLowerBound()),
                                Integer.toString(row.frequentBefore()),
                                Integer.toString(row.nonSensitiveBefore()),
                                Integer.toString(row.frequentAfter()));
                        System.out.printf("set %d  %-34s |R| %4d  lost %4d (%.4f)  supDist %.4f%s%n",
                                index, method.name(), row.maskCount(), row.lost(),
                                row.lostRelative(), row.supDist(),
                                method.isRandomized() ? "  seed " + thisSeed : "");
                    }
                }
            }
            writer.complete();
            if (front != null) {
                front.complete();
            }
        }
    }

    /**
     * |R| star for a single sensitive pattern: the corollary of P1 for distinct items, theorem P8
     * with its allocation for repeated items when g- = 0; 0 when neither applies.
     */
    private long boundFromP1(SequenceDatabase database, List<Pattern> sensitive, GapConstraint gap,
                             int sigma) {
        if (sensitive.size() != 1) {
            return 0;
        }
        DataProfile profile =
                DataProfile.of(database, sensitive.get(0), gap, HideProblem.Mode.HIDE_DB, sigma);
        return profile.isExact() ? profile.minMasks() : 0;
    }

    private HidingMethod create(String name, HideProblem problem, Map<Pattern, Long> frequent,
                                int sigma, double epsilon, long seed, Metrics.Context context) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "cut-hide": {
                Set<Pattern> closure =
                        FrequentPatterns.closure(problem.sensitive(), frequent.keySet());
                List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
                double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
                return new CutHide(CutHideConfig.standard()).withNearThreshold(band, factor);
            }
            case "cut-hide-sweep": {
                // CUT-HIDE without theorem P8: the lambda-sweep curves, and for a repeated-item
                // pattern the masks of a cut (P3). The ablation that measures what P8 adds.
                Set<Pattern> closure =
                        FrequentPatterns.closure(problem.sensitive(), frequent.keySet());
                List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
                double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
                return new CutHide(CutHideConfig.standard().withCurves(CutHideConfig.Curves.LAMBDA_SWEEP))
                        .withNearThreshold(band, factor);
            }
            case "cut-hide-weight": {
                // Theorem P8 with the surrogate weight first and the masks second: the other end
                // of the trade-off between |R| and Lost on repeated-item patterns.
                Set<Pattern> closure =
                        FrequentPatterns.closure(problem.sensitive(), frequent.keySet());
                List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
                double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
                return new CutHide(CutHideConfig.standard()
                        .withCurves(CutHideConfig.Curves.FRONTIER_DP_WEIGHT_FIRST))
                        .withNearThreshold(band, factor);
            }
            case "cut-hide-rw": {
                // CUT-HIDE with the adaptive side-effect weight: iteration 0 is cut-hide, later
                // iterations weigh positions by the exact drops of the previous solution. Same
                // |R|*, never more lost patterns than cut-hide. The drops come from the evaluator
                // that writes the lost column, so method and metric count them the same way.
                Set<Pattern> closure =
                        FrequentPatterns.closure(problem.sensitive(), frequent.keySet());
                List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
                double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
                Map<Pattern, Long> watched = new LinkedHashMap<>();
                for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
                    if (!closure.contains(e.getKey())) {
                        watched.put(e.getKey(), e.getValue());
                    }
                }
                Metrics.Context.Session session = context.session(problem);
                return new AdaptiveCutHide(band, factor, watched, sigma,
                        AdaptiveCutHide.DEFAULT_ITERATIONS, session::drops);
            }
            case "cut-hide-rw-ls": {
                // The method under test of H2 and H2-R since preregistration section 12: the
                // adaptive weight, then the local search with the saving condition of lemma L9.
                // Both need one sensitive pattern; with several, CUT-HIDE is the greedy of step 7,
                // unchanged, and the method column of the row says so.
                if (problem.sensitive().size() != 1) {
                    return create("cut-hide", problem, frequent, sigma, epsilon, seed, context);
                }
                AdaptiveCutHide adaptive = (AdaptiveCutHide) create("cut-hide-rw", problem, frequent,
                        sigma, epsilon, seed, context);
                return adaptive.withLocalSearch(MaskLocalSearch.DEFAULT_PASSES,
                        MaskLocalSearch.DEFAULT_RECEIVERS);
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
                return new Hha();
            case "dbsh":
                return new Dbsh();
            case "sbsh":
            case "sbsh-r1":
                return new Sbsh(1).withNonSensitiveFrequent(nonSensitiveFrequent(problem, frequent));
            case "sbsh-rmax":
                // The paper takes r in [1, length of the longest sensitive pattern], not l_max.
                return new Sbsh(longestSensitive(problem))
                        .withNonSensitiveFrequent(nonSensitiveFrequent(problem, frequent));
            case "primal-dual":
                // Runs only when sigma = 1; with any other sigma it throws an error saying why,
                // instead of silently falling back to another variant.
                return new PrimalDualCover();
            case "ilp":
                return new IlpExact();
            case "ilp-lost":
                return IlpExact.withSideEffects(problem, frequent);
            case "ga":
                return new GaHybrid(true, seed);
            default:
                throw new IllegalArgumentException("unknown method " + name);
        }
    }

    /**
     * F_sigma(D) without the sensitive patterns and their super-patterns, with sup_NO(Q, D). SBSH
     * needs all of them, not only those at the threshold: its Annotate reads the current support,
     * and a pattern above sigma at the start can sit exactly at sigma once earlier sequences have
     * been sanitised.
     */
    private Map<Pattern, Long> nonSensitiveFrequent(HideProblem problem, Map<Pattern, Long> frequent) {
        Set<Pattern> closure = FrequentPatterns.closure(problem.sensitive(), frequent.keySet());
        Map<Pattern, Long> out = new LinkedHashMap<>();
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            if (!closure.contains(e.getKey())) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    private static int longestSensitive(HideProblem problem) {
        int longest = 1;
        for (Pattern p : problem.sensitive()) {
            longest = Math.max(longest, p.length());
        }
        return longest;
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

    /**
     * The same label SC2 writes, so the grouping of section 3 of the preregistration can be joined
     * onto these rows. SC2 writes the dataset path; describing the database by its shape instead
     * would leave the two tables with no key in common and no way to say which group a row is in.
     */
    private String describe(Cli options) {
        String dataset = options.get("dataset", "");
        return dataset.isEmpty() ? "synthetic" : dataset;
    }

    private String shape(SequenceDatabase database) {
        return "N=" + database.size() + ",len=" + database.totalLength();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

}
