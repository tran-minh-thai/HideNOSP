// SPDX-License-Identifier: MIT
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
import hidenosp.core.SupportNO;
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
import hidenosp.util.Deadline;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * SC3: the main comparison on HIDE-DB.
 *
 * <p>One row per instance and per method, where an instance is a triple of dataset, sigma and
 * sensitive set, which is the unit of repetition. The columns are Lost,
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
                "methods", "patternKind", "pareto", "timeLimit", "closure"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        runComparison(id(), sink, options, HideProblem.Mode.HIDE_DB);
    }

    /**
     * The comparison under either problem. HIDE-DB is SC3. HIDE-SEQ is SC4, with the per-sequence limit
     * {@code --r}: the sensitive sets are drawn exactly as in SC3 (the same F_sigma(D), strata and seed),
     * sigma only defines F_sigma(D) for Lost and SupDist, and an instance in which no sequence reaches r
     * for any sensitive pattern needs no mask at all, so it is written with that status and no method row.
     */
    static void runComparison(String id, ResultSink sink, Cli options, HideProblem.Mode mode) {
        runComparison(id, sink, options, mode, false);
    }

    /**
     * The comparison, and with {@code robust} also the support of every sensitive pattern on each masked
     * database under wider gap constraints (preregistration section 23, lot F), written to the file
     * {@code <id>SUP_<stamp>.csv}: one row per sensitive set, method, pattern and constraint. The comparison rows
     * are the same as without it.
     */
    static void runComparison(String id, ResultSink sink, Cli options, HideProblem.Mode mode, boolean robust) {
        GapConstraint gap = new GapConstraint(options.getInt("gmin", 0), options.getInt("gmax", 3));
        GapConstraint hideGap = hideGapOf(options.get("hideGmax", ""), gap);
        if (hideGap != gap && mode != HideProblem.Mode.HIDE_DB) {
            throw new IllegalArgumentException("--hideGmax is defined for HIDE-DB only");
        }
        int sigma = options.getInt("sigma", 20);
        boolean perSequence = mode == HideProblem.Mode.HIDE_SEQ;
        int threshold = perSequence ? options.getInt("r", 0) : sigma;
        if (perSequence && threshold < 1) {
            throw new IllegalArgumentException("HIDE-SEQ needs the per-sequence limit --r, at least 1");
        }
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
        // "none" runs no method: with --pareto=true the run writes only the Pareto front, so the
        // front can be computed in a batch of its own without a second copy of the method rows.
        List<String> methodNames = methodList.trim().equalsIgnoreCase("none") ? List.of()
                : List.of(methodList.split(","));
        if (methodNames.isEmpty() && !options.getBoolean("pareto", false)) {
            throw new IllegalArgumentException("--methods=none only makes sense with --pareto=true");
        }
        // Proposition P10: also write the exact convex Pareto front of CUT-HIDE between |R| and the
        // surrogate weight, one row per vertex, into SC3PARETO_<stamp>.csv
        // (preregistration section 11, point 6: descriptive). One sensitive pattern and g- = 0 only.
        boolean pareto = options.getBoolean("pareto", false);
        // Seconds of wall clock for one method on one sensitive set; 0 means no limit. A method
        // over the limit gets status OT, one out of memory status OOM, and the analysis drops the
        // pair and counts it (preregistration section 5).
        double timeLimit = options.getDouble("timeLimit", 0);
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
                "frequent_before", "nonsensitive_before", "frequent_after",
                "lost_subseq", "lost_rel_subseq", "sup_dist_subseq", "nonsensitive_before_subseq", "status"};

        // Which frequent patterns count as hidden with the sensitive ones (FrequentPatterns.Closure). The
        // line is written even at the default, so a file without it is one written before the rule existed,
        // under the subsequence rule.
        FrequentPatterns.Closure closureRule = FrequentPatterns.Closure.parse(options.get("closure", "necessary"));
        String[] provenance = perSequence
                ? new String[] {"options " + options.all(), sampling.describe(), "closure " + closureRule.label(),
                    "mode HIDE_SEQ r=" + threshold}
                : hideGap != gap
                ? new String[] {"options " + options.all(), sampling.describe(), "closure " + closureRule.label(),
                    "hide gap " + hideGap}
                : new String[] {"options " + options.all(), sampling.describe(), "closure " + closureRule.label()};
        if (pareto && perSequence) {
            throw new IllegalArgumentException("--pareto is defined for HIDE-DB only");
        }
        if (pareto && (sensitiveSize != 1 || gap.gmin() != 0)) {
            throw new IllegalArgumentException("--pareto needs --sensitiveSize=1 and g- = 0, got "
                    + sensitiveSize + " and " + gap);
        }
        String[] frontHeader = {"dataset", "shape", "gap", "sigma", "sensitive_set", "stratum",
                "vertex", "vertices", "mask_count", "weight", "min_masks", "mask_ratio", "hf",
                "lost", "lost_rel", "ghost", "sup_dist", "frequent_before", "nonsensitive_before",
                "frequent_after", "lost_subseq", "lost_rel_subseq", "sup_dist_subseq", "nonsensitive_before_subseq"};
        String[] supportHeader = {"dataset", "gap", "sigma", "sensitive_set", "method_key", "pattern",
                "constraint", "sup_no_before", "sup_no_after"};
        try (CsvWriter writer = sink.open(ResultSink.Kind.INVARIANT, id, header, provenance);
             CsvWriter front = pareto
                     ? sink.open(ResultSink.Kind.INVARIANT, id + "PARETO", frontHeader, provenance)
                     : null;
             CsvWriter support = robust
                     ? sink.open(ResultSink.Kind.INVARIANT, id + "SUP", supportHeader, provenance)
                     : null) {
            for (int index = 0; index < draws.size(); index++) {
                List<Pattern> sensitive = draws.get(index).patterns();
                String stratum = "p" + draws.get(index).percentile();
                HideProblem problem = new HideProblem(database, sensitive, gap, mode, threshold, epsilon,
                        maxPatternLength, sigma).withClosure(closureRule);
                // SC3WIDE: the methods hide under a wider constraint, the side effects are counted under the gap of
                // the run, as in SC3. Without --hideGmax both are the gap of the run and nothing changes.
                HideProblem hiding = hideGap == gap ? problem
                        : new HideProblem(database, sensitive, hideGap, mode, threshold, epsilon, maxPatternLength,
                                sigma).withClosure(closureRule);
                long minMasks = boundFromP1(database, sensitive, hideGap, mode, threshold);
                if (perSequence && !reachesLimit(database, sensitive, gap, threshold)) {
                    writer.writeRow(describe(options), shape(database), gap.toString(), Integer.toString(sigma),
                            sensitive.toString(), stratum, "", "", "",
                            "", "", "", "", "", "", "", Long.toString(minMasks),
                            "", "", "", "", "", "", "", "", "", "", "", "", "no sequence reaches r");
                    System.out.printf("set %d  no sequence reaches r = %d: no mask is needed%n", index, threshold);
                    continue;
                }
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
                                Integer.toString(row.frequentAfter()), Integer.toString(row.lostSubsequence()),
                                format(row.lostRelativeSubsequence()), format(row.supDistSubsequence()),
                                Integer.toString(row.nonSensitiveBeforeSubsequence()));
                    }
                    System.out.printf("set %d  Pareto front: %d vertices, |R| from %d to %d%n",
                            index, vertices.size(), vertices.get(0).masks(),
                            vertices.get(vertices.size() - 1).masks());
                }
                for (String name : methodNames) {
                    // A deterministic method runs once; a randomized method runs --repeats
                    // times with consecutive seeds, one row per run. Taking the median belongs
                    // to the analysis and is not done here: with one row per run the table stays
                    // complete enough to be checked again.
                    int runs = create(name.trim(), hiding, frequent, sigma, epsilon, methodSeed,
                            context)
                            .isRandomized() ? repeats : 1;
                    for (int k = 0; k < runs; k++) {
                        long thisSeed = methodSeed + k;
                        HidingMethod method =
                                create(name.trim(), hiding, frequent, sigma, epsilon, thisSeed,
                                        context);
                        String status = "";
                        Sanitization sanitization = null;
                        // The time of the method is printed only, never written to the file: this is
                        // an invariant result file, and the console serves to plan the next batch.
                        long started = System.nanoTime();
                        try {
                            sanitization = Deadline.within(timeLimit, () -> method.run(hiding));
                        } catch (Deadline.Exceeded e) {
                            status = "OT";
                        } catch (OutOfMemoryError e) {
                            status = "OOM";
                        }
                        if (sanitization == null) {
                            // No sanitization to measure: every result column stays empty, and the
                            // status says why. An empty status means the method finished.
                            writer.writeRow(describe(options), shape(database),
                                    gap.toString(), Integer.toString(sigma),
                                    sensitive.toString(), stratum,
                                    name.trim().toLowerCase(Locale.ROOT), method.name(),
                                    method.isRandomized() ? Long.toString(thisSeed) : "",
                                    "", "", "", "", "", "", "", Long.toString(minMasks),
                                    "", "", "", "", "", "", "", "", "", "", "", "", status);
                            System.out.printf("set %d  %-34s %s after %.1f s (limit %s s)%n", index,
                                    method.name(), status, (System.nanoTime() - started) / 1e9,
                                    timeLimit);
                            continue;
                        }
                        double seconds = (System.nanoTime() - started) / 1e9;
                        Metrics.Row row = context.evaluateMasking(problem, sanitization);
                        if (hiding != problem && Metrics.hidingFailure(hiding, sanitization.sanitized()) > 0) {
                            status = "not hidden at the hiding gap";
                        }
                        // method_key is the name given on the command line, and it is stable;
                        // method is the name the method gives itself, which may carry its
                        // parameters. SBSH-rmax calls itself SBSH-r2, -r3 or -r4 by the length of
                        // the longest sensitive pattern, so the analysis must key on method_key, or
                        // the locked Holm family never matches a name in the file.
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
                                Integer.toString(row.frequentAfter()),
                                Integer.toString(row.lostSubsequence()), format(row.lostRelativeSubsequence()),
                                format(row.supDistSubsequence()), Integer.toString(row.nonSensitiveBeforeSubsequence()),
                                status);
                        if (support != null) {
                            writeSupports(support, describe(options), gap, sigma, sensitive,
                                    name.trim().toLowerCase(Locale.ROOT), database, sanitization.sanitized());
                        }
                        System.out.printf("set %d  %-34s |R| %4d  lost %4d (%.4f)  supDist %.4f  %.1f s%s%n",
                                index, method.name(), row.maskCount(), row.lost(),
                                row.lostRelative(), row.supDist(), seconds,
                                method.isRandomized() ? "  seed " + thisSeed : "");
                    }
                }
            }
            writer.complete();
            if (front != null) {
                front.complete();
            }
            if (support != null) {
                support.complete();
            }
        }
    }

    /**
     * |R| star for a single sensitive pattern: the corollary of P1 for distinct items, theorem P8
     * with its allocation for repeated items when g- = 0; 0 when neither applies.
     */
    private static long boundFromP1(SequenceDatabase database, List<Pattern> sensitive, GapConstraint gap,
                                    HideProblem.Mode mode, int threshold) {
        if (sensitive.size() != 1) {
            return 0;
        }
        DataProfile profile = DataProfile.of(database, sensitive.get(0), gap, mode, threshold);
        return profile.isExact() ? profile.minMasks() : 0;
    }

    /** True when some sequence has sup_NO of some sensitive pattern at r or above. */
    private static boolean reachesLimit(SequenceDatabase database, List<Pattern> sensitive, GapConstraint gap,
                                        int r) {
        for (Pattern p : sensitive) {
            for (int s = 0; s < database.size(); s++) {
                if (SupportNO.of(p, database.get(s), gap) >= r) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The constraint the methods hide under (preregistration section 23, lever 1): the gap of the run when the option is
     * empty, [g-, infinity] for "inf", else [g-, value] with value at least g+ of the run.
     */
    public static GapConstraint hideGapOf(String option, GapConstraint gap) {
        if (option == null || option.isEmpty()) {
            return gap;
        }
        if (option.equals("inf")) {
            return GapConstraint.unbounded(gap.gmin());
        }
        int value = Integer.parseInt(option);
        if (value < gap.gmax()) {
            throw new IllegalArgumentException("--hideGmax=" + value + " is narrower than the gap " + gap + " of the run");
        }
        return value == gap.gmax() ? gap : GapConstraint.of(gap.gmin(), value);
    }

    /**
     * The constraints of lot F for the gap [g-, g+] of the run: [g-, g+] itself, [g-, g+ + 1], [g-, 2 g+] and
     * [g-, infinity], with a constraint that repeats an earlier one left out (at [0,1], g+ + 1 = 2 g+ = 2).
     */
    public static List<GapConstraint> widerConstraints(GapConstraint gap) {
        // GapConstraint has no equals: a repeated constraint is recognised by its g+.
        List<GapConstraint> out = new ArrayList<>();
        List<Integer> seen = new ArrayList<>();
        out.add(gap);
        seen.add(gap.gmax());
        for (int gmax : new int[] {gap.gmax() + 1, 2 * gap.gmax()}) {
            if (gmax > gap.gmax() && !seen.contains(gmax)) {
                out.add(GapConstraint.of(gap.gmin(), gmax));
                seen.add(gmax);
            }
        }
        out.add(GapConstraint.unbounded(gap.gmin()));
        return out;
    }

    private static void writeSupports(CsvWriter out, String dataset, GapConstraint gap, int sigma,
                                      List<Pattern> sensitive, String methodKey, SequenceDatabase before,
                                      SequenceDatabase after) {
        for (Pattern p : sensitive) {
            for (GapConstraint c : widerConstraints(gap)) {
                out.writeRow(dataset, gap.toString(), Integer.toString(sigma), sensitive.toString(), methodKey,
                        p.toString(), c.toString(), Long.toString(total(p, before, c)),
                        Long.toString(total(p, after, c)));
            }
        }
    }

    private static long total(Pattern p, SequenceDatabase database, GapConstraint c) {
        long sum = 0;
        for (int f : SupportNO.perSequence(p, database, c)) {
            sum += f;
        }
        return sum;
    }

    /**
     * CUT-HIDE with one constant changed, for the sensitivity analysis of preregistration section 23: the key
     * {@code cut-hide-rw-ls@<constant><value>} is cut-hide-rw-ls with that constant set to the value and every other
     * constant at its default. The constants: {@code krw} the number of adaptive iterations (default 6), {@code tau}
     * the divisor of tau_Q (default 4), {@code passes} and {@code recv} the two limits of the local search (defaults
     * 50 and 30). With several sensitive patterns the key is cut-hide, as for cut-hide-rw-ls.
     */
    static HidingMethod createVariant(String name, HideProblem problem, Map<Pattern, Long> frequent,
                                      int sigma, double epsilon, long seed, Metrics.Context context) {
        String key = name.toLowerCase(Locale.ROOT);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("cut-hide-rw-ls@(krw|tau|passes|recv)(\\d+(?:\\.\\d+)?)").matcher(key);
        if (!m.matches()) {
            throw new IllegalArgumentException("unknown method variant " + name
                    + "; the form is cut-hide-rw-ls@<krw|tau|passes|recv><value>");
        }
        if (problem.sensitive().size() != 1) {
            return create("cut-hide", problem, frequent, sigma, epsilon, seed, context);
        }
        String constant = m.group(1);
        double value = Double.parseDouble(m.group(2));
        int iterations = AdaptiveCutHide.DEFAULT_ITERATIONS;
        int passes = MaskLocalSearch.DEFAULT_PASSES;
        int receivers = MaskLocalSearch.DEFAULT_RECEIVERS;
        double divisor = AdaptiveCutHide.DEFAULT_TAU_DIVISOR;
        switch (constant) {
            case "krw": iterations = whole(value, name); break;
            case "passes": passes = whole(value, name); break;
            case "recv": receivers = whole(value, name); break;
            default: divisor = value; break;
        }
        Set<Pattern> closure = problem.closureOf(frequent.keySet());
        List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
        double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
        Map<Pattern, Long> watched = new LinkedHashMap<>();
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            if (!closure.contains(e.getKey())) {
                watched.put(e.getKey(), e.getValue());
            }
        }
        Metrics.Context.Session session = context.session(problem);
        return new AdaptiveCutHide(band, factor, watched, sigma, iterations, session::drops)
                .withTauDivisor(divisor)
                .withLocalSearch(passes, receivers);
    }

    private static int whole(double value, String name) {
        if (value != Math.rint(value) || value < 1) {
            throw new IllegalArgumentException("the value of " + name + " must be a whole number of at least 1");
        }
        return (int) value;
    }

    /**
     * The method a key names, configured exactly as SC3 runs it. Shared with SC3LOST and SC6, so the
     * small instances and the timing see the same methods as the main comparison.
     */
    static HidingMethod create(String name, HideProblem problem, Map<Pattern, Long> frequent,
                                int sigma, double epsilon, long seed, Metrics.Context context) {
        if (name.indexOf('@') >= 0) {
            return createVariant(name, problem, frequent, sigma, epsilon, seed, context);
        }
        switch (name.toLowerCase(Locale.ROOT)) {
            case "cut-hide": {
                Set<Pattern> closure =
                        problem.closureOf(frequent.keySet());
                List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
                double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
                return CutHide.forBand(CutHideConfig.standard(), band, factor);
            }
            case "cut-hide-refine": {
                // A probe of proposition P15, not in the H2 set: with several patterns, the greedy of
                // step 7 followed by the exact solution of each one-pattern component (when smaller)
                // and the reverse delete. One pattern is cut-hide unchanged.
                if (problem.sensitive().size() == 1) {
                    return create("cut-hide", problem, frequent, sigma, epsilon, seed, context);
                }
                Set<Pattern> closure =
                        problem.closureOf(frequent.keySet());
                List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
                double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
                return CutHide.forBand(CutHideConfig.standard().withRefineSeveral(), band, factor);
            }
            case "cut-hide-sweep": {
                // CUT-HIDE without theorem P8: the lambda-sweep curves, and for a repeated-item
                // pattern the masks of a cut (P3). The ablation that measures what P8 adds.
                Set<Pattern> closure =
                        problem.closureOf(frequent.keySet());
                List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
                double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
                return CutHide.forBand(CutHideConfig.standard().withCurves(CutHideConfig.Curves.LAMBDA_SWEEP),
                        band, factor);
            }
            case "cut-hide-weight": {
                // Theorem P8 with the surrogate weight first and the masks second: the other end
                // of the trade-off between |R| and Lost on repeated-item patterns.
                Set<Pattern> closure =
                        problem.closureOf(frequent.keySet());
                List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
                double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
                return CutHide.forBand(CutHideConfig.standard()
                        .withCurves(CutHideConfig.Curves.FRONTIER_DP_WEIGHT_FIRST), band, factor);
            }
            case "cut-hide-rw": {
                // CUT-HIDE with the adaptive side-effect weight: iteration 0 is cut-hide, later
                // iterations weigh positions by the exact drops of the previous solution. Same
                // |R|*, never more lost patterns than cut-hide. The drops come from the evaluator
                // that writes the lost column, so method and metric count them the same way.
                // The weight needs one sensitive pattern; with several, CUT-HIDE is the greedy of
                // step 7, as for cut-hide-rw-ls, and the method column of the row says so. Without
                // this every SC3 cell with several patterns stopped at this key.
                if (problem.sensitive().size() != 1) {
                    return create("cut-hide", problem, frequent, sigma, epsilon, seed, context);
                }
                Set<Pattern> closure =
                        problem.closureOf(frequent.keySet());
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
            case "cut-hide-count": {
                // CUT-HIDE with the score of the HIDE-DB greedy used before preregistration section 23.9 (the count of
                // vital patterns), to reproduce the earlier rows; not in any H2 set. One pattern is cut-hide unchanged.
                Set<Pattern> closure = problem.closureOf(frequent.keySet());
                List<Pattern> band = FrequentPatterns.nearThreshold(frequent, sigma, epsilon, closure);
                double[] factor = FrequentPatterns.weightFactors(band, frequent, sigma);
                return CutHide.forBand(CutHideConfig.standard().withCountScoreDatabase(), band, factor);
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
            case "hha-emb":
                // The occurrence-count rule of Abul, Bonchi and Giannotti (TKDE 2010): sanitise until the
                // matching set of the sequence holds at most r - 1 embeddings. HIDE-SEQ only.
                return Hha.perSequenceEmbeddings();
            case "hha-stop":
                // Preregistration section 19.1: HHA's order and position rule, stopping as soon as
                // every sensitive support is below sigma.
                return Hha.stopAtThreshold();
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
    private static Map<Pattern, Long> nonSensitiveFrequent(HideProblem problem, Map<Pattern, Long> frequent) {
        Set<Pattern> closure = problem.closureOf(frequent.keySet());
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

    private static SequenceDatabase load(Cli options, long seed) {
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
    private static String describe(Cli options) {
        String dataset = options.get("dataset", "");
        return dataset.isEmpty() ? "synthetic" : dataset;
    }

    private static String shape(SequenceDatabase database) {
        return "N=" + database.size() + ",len=" + database.totalLength();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

}
