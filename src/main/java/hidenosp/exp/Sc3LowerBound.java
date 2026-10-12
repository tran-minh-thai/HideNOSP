package hidenosp.exp;

import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.hide.HideProblem;
import hidenosp.io.CsvWriter;
import hidenosp.io.DatasetReader;
import hidenosp.io.ResultSink;
import hidenosp.metrics.DataProfile;
import hidenosp.metrics.Metrics;
import hidenosp.util.Cli;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * The lower bound on the least number of masks for several sensitive patterns, on the sensitive
 * sets of SC3 (same sampling, same seed, so the rows join SC3 rows on the sensitive set).
 *
 * <p>Two patterns are linked when they share an item. Masking a position keeps its index and only
 * removes the nodes of patterns that contain its item, so the problem splits over the connected
 * components of that graph: OPT(S) is the sum of the optima of the components, each at least the
 * largest single-pattern optimum in it. The bound is the sum over components of that largest
 * optimum, and it is the optimum itself when every component holds one pattern. The single-pattern
 * optima come from {@link DataProfile} (exact for distinct items, and for repeated items with
 * g- = 0); a row says when one of them is not exact.
 */
public final class Sc3LowerBound implements Scenario {

    @Override
    public String id() {
        return "SC3LB";
    }

    @Override
    public String title() {
        return "lower bound on |R| for several sensitive patterns, by components of shared items";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "maxPatternLength",
                "setsPerStratum", "sensitiveSize", "seed", "patternKind"};
    }

    /** The bound of one sensitive set, with what it is made of. */
    public record Bound(int components, int singletons, long lowerBound, long[] singleOptima,
                        int[] component, boolean optimaExact) {

        /** True when every component holds one pattern, so the bound is the optimum. */
        public boolean isOptimum() {
            return singletons == components;
        }
    }

    /** Components of the shared-item graph and the bound, for one sensitive set. */
    public static Bound bound(SequenceDatabase database, List<Pattern> sensitive, GapConstraint gap,
                              HideProblem.Mode mode, int sigma) {
        int n = sensitive.size();
        int[] parent = new int[n];
        for (int k = 0; k < n; k++) {
            parent[k] = k;
        }
        Map<Integer, Integer> firstWithItem = new HashMap<>();
        for (int k = 0; k < n; k++) {
            for (int item : sensitive.get(k).items()) {
                Integer other = firstWithItem.putIfAbsent(item, k);
                if (other != null) {
                    union(parent, k, other);
                }
            }
        }
        long[] optimum = new long[n];
        boolean exact = true;
        for (int k = 0; k < n; k++) {
            DataProfile profile = DataProfile.of(database, sensitive.get(k), gap, mode, sigma);
            optimum[k] = profile.minMasks();
            exact &= profile.isExact();
        }
        Map<Integer, Long> largest = new HashMap<>();
        Map<Integer, Integer> size = new HashMap<>();
        int[] component = new int[n];
        for (int k = 0; k < n; k++) {
            int root = find(parent, k);
            component[k] = root;
            largest.merge(root, optimum[k], Math::max);
            size.merge(root, 1, Integer::sum);
        }
        long bound = 0;
        for (long value : largest.values()) {
            bound += value;
        }
        int singletons = 0;
        for (int value : size.values()) {
            if (value == 1) {
                singletons++;
            }
        }
        return new Bound(largest.size(), singletons, bound, optimum, component, exact);
    }

    private static int find(int[] parent, int k) {
        while (parent[k] != k) {
            parent[k] = parent[parent[k]];
            k = parent[k];
        }
        return k;
    }

    private static void union(int[] parent, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra != rb) {
            parent[Math.max(ra, rb)] = Math.min(ra, rb);
        }
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        String dataset = options.get("dataset", "");
        if (dataset.isEmpty()) {
            throw new IllegalArgumentException("--dataset is required: the bound is joined to SC3 rows");
        }
        GapConstraint gap = new GapConstraint(options.getInt("gmin", 0), options.getInt("gmax", 3));
        int sigma = options.getInt("sigma", 20);
        int maxPatternLength = options.getInt("maxPatternLength", 3);
        int perStratum = options.getInt("setsPerStratum", 4);
        int sensitiveSize = options.getInt("sensitiveSize", 1);
        long seed = options.getLong("seed", 2026);
        String patternKind = options.get("patternKind", "distinct");
        if (!patternKind.equals("distinct") && !patternKind.equals("repeated")) {
            throw new IllegalArgumentException("--patternKind is distinct or repeated, got " + patternKind);
        }
        DatasetReader.Format format = DatasetReader.Format
                .valueOf(options.get("format", "PLAIN").toUpperCase(Locale.ROOT));
        SequenceDatabase database = DatasetReader.read(Paths.get(dataset), format).database();
        Metrics.Context context = Metrics.Context.of(database, gap, maxPatternLength, sigma);
        // The same draw as SC3: same pool, same strata, same seed.
        SensitiveSampling.Result sampling = SensitiveSampling.draw(context.frequent(), sensitiveSize,
                perStratum, seed, patternKind.equals("repeated"));
        System.out.println(sampling.describe());

        String[] header = {"dataset", "shape", "gap", "sigma", "pattern_kind", "sensitive_size",
            "sensitive_set", "stratum", "patterns", "components", "singleton_components",
            "lower_bound", "bound_is_optimum", "single_optima", "component_of", "optima_exact"};
        String[] provenance = {"options " + options.all(), sampling.describe()};
        try (CsvWriter writer = sink.open(ResultSink.Kind.INVARIANT, id(), header, provenance)) {
            for (SensitiveSampling.Draw draw : sampling.draws()) {
                List<Pattern> sensitive = draw.patterns();
                Bound b = bound(database, sensitive, gap, HideProblem.Mode.HIDE_DB, sigma);
                writer.writeRow(dataset, "N=" + database.size() + ",len=" + database.totalLength(),
                        gap.toString(), Integer.toString(sigma), patternKind,
                        Integer.toString(sensitiveSize), sensitive.toString(), "p" + draw.percentile(),
                        Integer.toString(sensitive.size()), Integer.toString(b.components()),
                        Integer.toString(b.singletons()), Long.toString(b.lowerBound()),
                        Boolean.toString(b.isOptimum()), join(b.singleOptima()),
                        join(b.component()), Boolean.toString(b.optimaExact()));
            }
            writer.complete();
        }
        System.out.printf("%d sensitive sets written%n", sampling.draws().size());
    }

    private static String join(long[] values) {
        StringJoiner j = new StringJoiner(";");
        for (long v : values) {
            j.add(Long.toString(v));
        }
        return j.toString();
    }

    private static String join(int[] values) {
        StringJoiner j = new StringJoiner(";");
        for (int v : values) {
            j.add(Integer.toString(v));
        }
        return j.toString();
    }
}
