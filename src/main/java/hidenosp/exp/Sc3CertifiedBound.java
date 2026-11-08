// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.LostLowerBound;
import hidenosp.hide.Sanitization;
import hidenosp.io.CsvWriter;
import hidenosp.io.DatasetReader;
import hidenosp.io.ResultSink;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.metrics.Metrics;
import hidenosp.util.Cli;
import hidenosp.util.Deadline;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The certified lower bound on the optimum Lost of proposition P12 (preregistration section 20.2), in two
 * places.
 *
 * <p>On the full data: the |S| = 1 sets of SC3 are drawn again, the method under test
 * ({@code cut-hide-rw-ls}, built by the SC3 factory) is rerun, and its Lost is written beside the bound B
 * of {@link LostLowerBound} with that solution as reference. Lost = B certifies the solution optimal. The
 * Lost must equal the official SC3 row of the set, which ties the rerun to the comparison tables.
 *
 * <p>On the small instances of SC3LOST: the instances are rebuilt with the same seeds and the same
 * construction ({@link Sc3LostOptimum#cut}), and B is written per set and drop, to be joined with the
 * SC3LOST rows, where the exact optimum is. B must not exceed it.
 *
 * <p>{@code --parts} (default {@code full,small}) names the parts to run; {@code --parts=small} rebuilds the
 * small instances alone, with the same sets and the same instances as a run of both parts, since the sets are
 * drawn from the full data in either case and the instance seed does not depend on the full-data row.
 */
public final class Sc3CertifiedBound implements Scenario {

    static final String METHOD = "cut-hide-rw-ls";

    @Override
    public String id() {
        return "SC3CERT";
    }

    @Override
    public String title() {
        return "certified lower bound on the optimum Lost, on the full data and on the small instances of SC3LOST";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "maxPatternLength", "epsilon",
                "setsPerStratum", "seed", "patternKind", "window", "carrying", "without", "drops", "instanceSeed",
                "timeLimit", "parts", "closure"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        // Which frequent patterns count as hidden with the sensitive ones (FrequentPatterns.Closure); written
        // into the header even at the default, so a file without the line was written under the subsequence rule.
        FrequentPatterns.Closure closureRule = FrequentPatterns.Closure.parse(options.get("closure", "necessary"));
        String dataset = options.get("dataset", "");
        if (dataset.isEmpty()) {
            throw new IllegalArgumentException("--dataset is required");
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
        double timeLimit = options.getDouble("timeLimit", 1800);
        Set<String> parts = new LinkedHashSet<>();
        for (String part : options.get("parts", "full,small").split(",")) {
            String trimmed = part.trim();
            if (!trimmed.equals("full") && !trimmed.equals("small")) {
                throw new IllegalArgumentException("--parts lists full and/or small, got " + trimmed);
            }
            parts.add(trimmed);
        }
        DatasetReader.Format format = DatasetReader.Format
                .valueOf(options.get("format", "PLAIN").toUpperCase(Locale.ROOT));
        SequenceDatabase database = DatasetReader.read(Paths.get(dataset), format).database();
        Metrics.Context full = Metrics.Context.of(database, gap, maxPatternLength, sigma);
        SensitiveSampling.Result sampling = SensitiveSampling.draw(full.frequent(), 1, perStratum, seed,
                patternKind.equals("repeated"));
        System.out.println(sampling.describe());

        String[] header = {"dataset", "gap", "sigma", "pattern_kind", "sensitive_set", "stratum", "instance", "drop",
            "instance_sigma", "mask_count", "lost", "bound", "examined", "certified", "status"};
        String[] provenance = {"options " + options.all(), sampling.describe(), "reference method " + METHOD,
            "closure " + closureRule.label()};
        try (CsvWriter writer = sink.open(ResultSink.Kind.INVARIANT, id(), header, provenance)) {
            List<SensitiveSampling.Draw> draws = sampling.draws();
            for (int d = 0; d < draws.size(); d++) {
                Pattern pattern = draws.get(d).patterns().get(0);
                String[] base = {dataset, gap.toString(), Integer.toString(sigma), patternKind, List.of(pattern).toString(),
                    "p" + draws.get(d).percentile()};
                HideProblem problem = new HideProblem(database, List.of(pattern), gap, HideProblem.Mode.HIDE_DB, sigma,
                        epsilon, maxPatternLength).withClosure(closureRule);
                if (parts.contains("full")) {
                    writer.writeRow(certify(base, "full", "", sigma, problem, full, epsilon, seed, timeLimit, d));
                }
                if (!parts.contains("small")) {
                    continue;
                }

                Random random = new Random(instanceSeed + 1_000_003L * d);
                List<EventSequence> windows = new ArrayList<>();
                int found = Sc3LostOptimum.cut(database, pattern, gap, window, carrying, without, random, windows);
                SequenceDatabase instance = new SequenceDatabase(windows);
                long support = SupportNO.of(pattern, instance, gap);
                for (int r : drops) {
                    long instanceSigma = support - r + 1;
                    if (found < carrying || instanceSigma < 2) {
                        writer.writeRow(row(base, "small", Integer.toString(r), "", "", "", "", "", "",
                                found < carrying ? "fewer than " + carrying + " windows with an occurrence"
                                        : "threshold of the instance below 2"));
                        continue;
                    }
                    HideProblem small = new HideProblem(instance, List.of(pattern), gap, HideProblem.Mode.HIDE_DB,
                            (int) instanceSigma, epsilon, maxPatternLength).withClosure(closureRule);
                    Metrics.Context context = Metrics.Context.of(instance, gap, maxPatternLength, instanceSigma);
                    writer.writeRow(certify(base, "small", Integer.toString(r), (int) instanceSigma, small, context,
                            epsilon, seed, timeLimit, d));
                }
            }
            writer.complete();
        }
    }

    /** Runs the reference method and the bound on one problem; one row, and one progress line on the console. */
    private static String[] certify(String[] base, String kind, String drop, int sigma, HideProblem problem,
                                    Metrics.Context context, double epsilon, long seed, double timeLimit, int set) {
        String[] out = certifyRow(base, kind, drop, sigma, problem, context, epsilon, seed, timeLimit);
        int k = base.length;
        System.out.printf("set %d  %-5s %-2s |R| %5s  lost %5s  bound %5s  examined %5s  %s%n", set, kind, drop,
                out[k + 3], out[k + 4], out[k + 5], out[k + 6], out[k + 8].isEmpty() ? "" : out[k + 8]);
        return out;
    }

    private static String[] certifyRow(String[] base, String kind, String drop, int sigma, HideProblem problem,
                                       Metrics.Context context, double epsilon, long seed, double timeLimit) {
        Map<Pattern, Long> frequent = context.frequent();
        Set<Pattern> closure = problem.closureOf(frequent.keySet());
        Map<Pattern, Long> watched = new LinkedHashMap<>();
        frequent.forEach((q, v) -> {
            if (!closure.contains(q)) {
                watched.put(q, v);
            }
        });
        Sanitization reference;
        LostLowerBound.Result bound;
        try {
            long start = System.nanoTime();
            Sanitization solved = Deadline.within(timeLimit, () -> Sc3MainComparison.create(METHOD, problem, frequent,
                    sigma, epsilon, seed, context).run(problem));
            reference = solved;
            long middle = System.nanoTime();
            bound = Deadline.within(timeLimit, () -> LostLowerBound.of(problem, watched, sigma, solved));
            System.out.printf("        %s %s: reference %.1f s, bound %.1f s%n", kind, drop,
                    (middle - start) / 1e9, (System.nanoTime() - middle) / 1e9);
        } catch (Deadline.Exceeded e) {
            return row(base, kind, drop, Integer.toString(sigma), "", "", "", "", "", "OT");
        } catch (OutOfMemoryError e) {
            return row(base, kind, drop, Integer.toString(sigma), "", "", "", "", "", "OOM");
        } catch (RuntimeException e) {
            return row(base, kind, drop, Integer.toString(sigma), "", "", "", "", "",
                    "method failed: " + String.valueOf(e.getMessage()).replace(',', ';'));
        }
        int lost = context.evaluateMasking(problem, reference).lost();
        return row(base, kind, drop, Integer.toString(sigma), Integer.toString(reference.maskCount()),
                Integer.toString(lost), Integer.toString(bound.bound()), Integer.toString(bound.examined()),
                Boolean.toString(lost == bound.bound()), lost < bound.bound() ? "check failed: bound above Lost" : "");
    }

    private static String[] row(String[] base, String kind, String drop, String sigma, String masks, String lost,
                                String bound, String examined, String certified, String status) {
        String[] out = Arrays.copyOf(base, base.length + 9);
        int k = base.length;
        out[k] = kind;
        out[k + 1] = drop;
        out[k + 2] = sigma;
        out[k + 3] = masks;
        out[k + 4] = lost;
        out[k + 5] = bound;
        out[k + 6] = examined;
        out[k + 7] = certified;
        out[k + 8] = status;
        return out;
    }
}
