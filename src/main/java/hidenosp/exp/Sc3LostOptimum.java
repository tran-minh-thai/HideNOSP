// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.baseline.HidingMethod;
import hidenosp.baseline.IlpExact;
import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * How far each method sits from the exact Lost optimum, on small instances cut from real data
 * (preregistration section 19.2). On the full databases the second objective is NP-hard even for one
 * sensitive pattern, so the optimum is out of reach; here every minimum hiding set is enumerated.
 *
 * <p>Each sensitive set of SC3 with |S| = 1 is drawn again (same pool, strata and seed, so the rows
 * join SC3 rows on the set). The instance keeps the pattern P and the gap and takes ten windows of
 * {@code window} consecutive events: {@code carrying} windows that contain an occurrence of P, from the
 * sequences of the dataset in a seeded random order (the window starts at a seeded random offset among
 * those whose window contains one), and {@code without} windows from sequences in which P does not
 * occur. A sequence shorter than the window is taken whole. For each requested drop r, the threshold of
 * the instance is sup_NO(P, I) - r + 1, so P has to fall by r; below 2 the instance is skipped.
 *
 * <p>The optimum is {@link IlpExact#solveExactly}: every hiding set of the least size |R|*, the least and
 * the greatest Lost among them. An instance the exact class refuses (too many candidates, masks or
 * minimum sets) is written with its reason in {@code status}. Every method is built by the factory of
 * SC3, so it is configured as in the main comparison, and evaluated by the same evaluator; the Lost of
 * the exact set is evaluated by that evaluator too and must equal the exact class's own count, or the
 * row says so.
 */
public final class Sc3LostOptimum implements Scenario {

    static final String DEFAULT_METHODS =
            "cut-hide,cut-hide-rw,cut-hide-rw-ls,hha,dbsh,sbsh-r1,sbsh-rmax,occurrence,hha-stop";

    @Override
    public String id() {
        return "SC3LOST";
    }

    @Override
    public String title() {
        return "exact Lost optimum on small instances cut from real data, against every method";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "maxPatternLength", "epsilon",
                "setsPerStratum", "seed", "patternKind", "window", "carrying", "without", "drops",
                "instanceSeed", "methods", "closure"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        // Which frequent patterns count as hidden with the sensitive ones (FrequentPatterns.Closure); written
        // into the header even at the default, so a file without the line was written under the subsequence rule.
        FrequentPatterns.Closure closureRule = FrequentPatterns.Closure.parse(options.get("closure", "necessary"));
        String dataset = options.get("dataset", "");
        if (dataset.isEmpty()) {
            throw new IllegalArgumentException("--dataset is required: the instances are cut from real sequences");
        }
        GapConstraint gap = new GapConstraint(options.getInt("gmin", 0), options.getInt("gmax", 3));
        int sigma = options.getInt("sigma", 20);
        int maxPatternLength = options.getInt("maxPatternLength", 4);
        double epsilon = options.getDouble("epsilon", 0.5);
        int perStratum = options.getInt("setsPerStratum", 4);
        long seed = options.getLong("seed", 2026);
        String patternKind = options.get("patternKind", "distinct");
        if (!patternKind.equals("distinct") && !patternKind.equals("repeated")) {
            throw new IllegalArgumentException("--patternKind is distinct or repeated, got " + patternKind);
        }
        int window = options.getInt("window", 15);
        int carrying = options.getInt("carrying", 5);
        int without = options.getInt("without", 5);
        int[] drops = Arrays.stream(options.get("drops", "2,3,4").split(","))
                .map(String::trim).mapToInt(Integer::parseInt).toArray();
        long instanceSeed = options.getLong("instanceSeed", 20261106L);
        String[] methods = options.get("methods", DEFAULT_METHODS).split(",");
        DatasetReader.Format format = DatasetReader.Format
                .valueOf(options.get("format", "PLAIN").toUpperCase(Locale.ROOT));
        SequenceDatabase database = DatasetReader.read(Paths.get(dataset), format).database();
        Metrics.Context full = Metrics.Context.of(database, gap, maxPatternLength, sigma);
        SensitiveSampling.Result sampling = SensitiveSampling.draw(full.frequent(), 1, perStratum, seed,
                patternKind.equals("repeated"));
        System.out.println(sampling.describe());

        String[] header = {"dataset", "gap", "sigma", "pattern_kind", "sensitive_set", "stratum", "drop",
            "instance_sigma", "sequences", "carrying_windows", "frequent_patterns", "min_masks", "optimal_sets",
            "opt_lost", "worst_lost", "method_key", "method", "mask_count", "lost", "status"};
        String[] provenance = {"options " + options.all(), sampling.describe(), "closure " + closureRule.label()};
        int instances = 0;
        int solved = 0;
        try (CsvWriter writer = sink.open(ResultSink.Kind.INVARIANT, id(), header, provenance)) {
            List<SensitiveSampling.Draw> draws = sampling.draws();
            for (int d = 0; d < draws.size(); d++) {
                Pattern pattern = draws.get(d).patterns().get(0);
                Random random = new Random(instanceSeed + 1_000_003L * d);
                List<EventSequence> windows = new ArrayList<>();
                int found = cut(database, pattern, gap, window, carrying, without, random, windows);
                SequenceDatabase instance = new SequenceDatabase(windows);
                long support = SupportNO.of(pattern, instance, gap);
                for (int r : drops) {
                    instances++;
                    String[] base = {dataset, gap.toString(), Integer.toString(sigma), patternKind,
                        List.of(pattern).toString(), "p" + draws.get(d).percentile(), Integer.toString(r),
                        "", Integer.toString(windows.size()), Integer.toString(found)};
                    if (found < carrying) {
                        writer.writeRow(row(base, "", "", "", "", "", "", "", "", "",
                                "fewer than " + carrying + " windows with an occurrence"));
                        continue;
                    }
                    long instanceSigma = support - r + 1;
                    base[7] = Long.toString(instanceSigma);
                    if (instanceSigma < 2) {
                        writer.writeRow(row(base, "", "", "", "", "", "", "", "", "",
                                "threshold of the instance below 2"));
                        continue;
                    }
                    if (solve(writer, base, instance, pattern, gap, (int) instanceSigma, epsilon,
                            maxPatternLength, seed, methods, closureRule)) {
                        solved++;
                    }
                }
            }
            writer.complete();
        }
        System.out.printf("%d instances, %d solved exactly%n", instances, solved);
    }

    /**
     * Cuts the windows of one instance into {@code out}; returns how many contain an occurrence of P.
     * The carrying windows come first, then the windows from sequences without P.
     */
    public static int cut(SequenceDatabase database, Pattern pattern, GapConstraint gap, int window, int carrying,
                   int without, Random random, List<EventSequence> out) {
        List<Integer> order = new ArrayList<>();
        for (int s = 0; s < database.size(); s++) {
            order.add(s);
        }
        Collections.shuffle(order, random);
        List<int[]> with = new ArrayList<>();
        List<int[]> other = new ArrayList<>();
        for (int s : order) {
            if (with.size() == carrying && other.size() == without) {
                break;
            }
            EventSequence sequence = database.get(s);
            boolean occurs = SupportNO.of(pattern, sequence, gap) > 0;
            if (occurs && with.size() < carrying) {
                int[] events = sequence.events();
                if (events.length <= window) {
                    with.add(events);
                    continue;
                }
                List<Integer> starts = new ArrayList<>();
                for (int o = 0; o + window <= events.length; o++) {
                    int[] part = Arrays.copyOfRange(events, o, o + window);
                    if (SupportNO.of(pattern, new EventSequence(0, part), gap) > 0) {
                        starts.add(o);
                    }
                }
                if (!starts.isEmpty()) {
                    int o = starts.get(random.nextInt(starts.size()));
                    with.add(Arrays.copyOfRange(events, o, o + window));
                }
            } else if (!occurs && other.size() < without) {
                int[] events = sequence.events();
                int o = events.length <= window ? 0 : random.nextInt(events.length - window + 1);
                other.add(Arrays.copyOfRange(events, o, Math.min(events.length, o + window)));
            }
        }
        for (int[] events : with) {
            out.add(new EventSequence(out.size(), events));
        }
        for (int[] events : other) {
            out.add(new EventSequence(out.size(), events));
        }
        return with.size();
    }

    /** Solves one instance exactly and runs every method on it; returns whether the exact part succeeded. */
    private static boolean solve(CsvWriter writer, String[] base, SequenceDatabase instance, Pattern pattern,
                                 GapConstraint gap, int instanceSigma, double epsilon, int maxPatternLength,
                                 long seed, String[] methods, FrequentPatterns.Closure closureRule) {
        HideProblem problem = new HideProblem(instance, List.of(pattern), gap, HideProblem.Mode.HIDE_DB,
                instanceSigma, epsilon, maxPatternLength).withClosure(closureRule);
        Metrics.Context context = Metrics.Context.of(instance, gap, maxPatternLength, instanceSigma);
        Map<Pattern, Long> frequent = context.frequent();
        String watched = Integer.toString(frequent.size());
        IlpExact.Exact exact;
        try {
            exact = IlpExact.withSideEffects(problem, frequent).solveExactly(problem);
        } catch (IllegalArgumentException | IllegalStateException e) {
            writer.writeRow(row(base, watched, "", "", "", "", "", "", "", "", "exact method refused: "
                    + e.getMessage().replace(',', ';')));
            return false;
        }
        String[] exactPart = {watched, Integer.toString(exact.minMasks()), Integer.toString(exact.optimalSets()),
            Integer.toString(exact.bestLost()), Integer.toString(exact.worstLost())};
        int evaluated = context.evaluateMasking(problem, exact.best()).lost();
        if (evaluated != exact.bestLost()) {
            writer.writeRow(row(base, exactPart, "exact", "exact-minR-minLost",
                    Integer.toString(exact.best().maskCount()), Integer.toString(evaluated),
                    "check failed: the evaluator counts " + evaluated + " lost patterns for the exact set, the"
                            + " exact class " + exact.bestLost()));
            return false;
        }
        for (String raw : methods) {
            String name = raw.trim().toLowerCase(Locale.ROOT);
            HidingMethod method = Sc3MainComparison.create(name, problem, frequent, instanceSigma, epsilon,
                    seed, context);
            Sanitization sanitization;
            try {
                sanitization = method.run(problem);
            } catch (RuntimeException e) {
                writer.writeRow(row(base, exactPart, name, method.name(), "", "",
                        "method failed: " + String.valueOf(e.getMessage()).replace(',', ';')));
                continue;
            }
            Metrics.Row measured = context.evaluateMasking(problem, sanitization);
            String status = measured.hidingFailure() > 0 ? "does not hide" : "";
            writer.writeRow(row(base, exactPart, name, method.name(), Integer.toString(measured.maskCount()),
                    Integer.toString(measured.lost()), status));
        }
        return true;
    }

    private static String[] row(String[] base, String[] exactPart, String key, String method, String masks,
                                String lost, String status) {
        return row(base, exactPart[0], exactPart[1], exactPart[2], exactPart[3], exactPart[4], key, method,
                masks, lost, status);
    }

    private static String[] row(String[] base, String watched, String minMasks, String optimalSets,
                                String optLost, String worstLost, String key, String method, String masks,
                                String lost, String status) {
        String[] out = Arrays.copyOf(base, base.length + 10);
        out[base.length] = watched;
        out[base.length + 1] = minMasks;
        out[base.length + 2] = optimalSets;
        out[base.length + 3] = optLost;
        out[base.length + 4] = worstLost;
        out[base.length + 5] = key;
        out[base.length + 6] = method;
        out[base.length + 7] = masks;
        out[base.length + 8] = lost;
        out[base.length + 9] = status;
        return out;
    }
}
