package hidenosp.exp;

import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.io.CsvWriter;
import hidenosp.io.DatasetReader;
import hidenosp.io.ResultSink;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.util.Cli;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The sigma rule of config/protocol.yaml, applied by a program instead of read off by hand.
 *
 * <p>For every gap and every candidate sigma it counts the frequent patterns |F_sigma| by length,
 * and the pool of distinct-item candidates of length 2 to 4 that the main comparison draws its
 * sensitive sets from ({@link SensitiveSampling#pool}). A configuration meets the rule when
 * |F_sigma| lies within [minF, maxF] and F_sigma is a proper subset of the pattern space up to
 * l_max: a threshold that keeps every pattern leaves nothing that hiding could lose or create.
 *
 * <p>It prepares the data and is not one of the scenarios of the paper; its identifier is SIGMA so
 * that its files cannot be mistaken for those of SC1 to SC8. The counts do not depend on the
 * machine, so the file is an invariant one.
 */
public final class SigmaScan implements Scenario {

    /** The bounds of the rule in config/protocol.yaml. */
    public static final int DEFAULT_MIN_F = 50;
    public static final int DEFAULT_MAX_F = 700;

    /** One configuration of the scan; {@code byLength[k]} counts the patterns of length k + 1. */
    public record Row(String gap, int sigma, int frequent, int[] byLength, int pool, long space,
                      boolean meetsRule) {
    }

    @Override
    public String id() {
        return "SIGMA";
    }

    @Override
    public String title() {
        return "sigma rule of the protocol: |F_sigma| and the distinct-item pool per gap and sigma";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "maxPatternLength",
                "minF", "maxF"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        String datasetOption = options.get("dataset", "");
        if (datasetOption.isEmpty()) {
            throw new IllegalArgumentException("--dataset is required");
        }
        int gmin = options.getInt("gmin", 0);
        int[] gmaxes = parseList(options.get("gmax", "1,3"), "gmax");
        int[] sigmas = parseList(options.get("sigma", ""), "sigma");
        int maxPatternLength = options.getInt("maxPatternLength", 4);
        int minF = options.getInt("minF", DEFAULT_MIN_F);
        int maxF = options.getInt("maxF", DEFAULT_MAX_F);
        DatasetReader.Format format = DatasetReader.Format
                .valueOf(options.get("format", "SPMF").toUpperCase(Locale.ROOT));
        DatasetReader.Loaded loaded = DatasetReader.read(Paths.get(datasetOption), format);
        SequenceDatabase database = loaded.database();

        String[] header = {"dataset", "gap", "sigma", "sigma_share", "f_sigma", "f_by_length",
                "pool", "pattern_space", "meets_rule"};
        String[] provenance = {"rule |F_sigma| within [" + minF + ", " + maxF + "] and a proper "
                + "subset of the pattern space up to l_max = " + maxPatternLength,
                "options " + options.all()};
        Map<Integer, Boolean> atEveryGap = new TreeMap<>();
        try (CsvWriter writer = sink.open(ResultSink.Kind.INVARIANT, id(), header, provenance)) {
            System.out.println("dataset " + loaded.source() + ", |D| = " + database.size()
                    + ", total length " + database.totalLength()
                    + ", alphabet " + database.alphabet().length);
            for (int gmax : gmaxes) {
                GapConstraint gap = new GapConstraint(gmin, gmax);
                for (int sigma : sigmas) {
                    Row row = scan(database, gap, sigma, maxPatternLength, minF, maxF);
                    String share = String.format(Locale.ROOT, "%.6f",
                            sigma / (double) database.totalLength());
                    writer.writeRow(loaded.source(), row.gap(), Integer.toString(sigma), share,
                            Integer.toString(row.frequent()), byLength(row.byLength()),
                            Integer.toString(row.pool()), Long.toString(row.space()),
                            Boolean.toString(row.meetsRule()));
                    System.out.printf("gap %-6s sigma %6d (%s of events): |F_sigma| %5d, by length "
                                    + "%s, pool %4d, %s%n", row.gap(), sigma, share, row.frequent(),
                            byLength(row.byLength()), row.pool(),
                            row.meetsRule() ? "meets the rule" : "outside the rule");
                    atEveryGap.merge(sigma, row.meetsRule(), Boolean::logicalAnd);
                }
            }
            writer.complete();
        }
        List<Integer> kept = new ArrayList<>();
        atEveryGap.forEach((sigma, meets) -> {
            if (meets) {
                kept.add(sigma);
            }
        });
        System.out.println("sigma meeting the rule at every gap scanned: "
                + (kept.isEmpty() ? "none" : kept));
    }

    /** Counts of one configuration. */
    public static Row scan(SequenceDatabase database, GapConstraint gap, int sigma,
                           int maxPatternLength, int minF, int maxF) {
        Map<Pattern, Long> frequent = FrequentPatterns.mine(database, gap, maxPatternLength, sigma);
        int[] byLength = new int[maxPatternLength];
        for (Pattern pattern : frequent.keySet()) {
            byLength[pattern.length() - 1]++;
        }
        int pool = SensitiveSampling.pool(frequent).size();
        long space = patternSpace(database.alphabet().length, maxPatternLength);
        return new Row(gap.toString(), sigma, frequent.size(), byLength, pool, space,
                meetsRule(frequent.size(), space, minF, maxF));
    }

    /** The rule: |F_sigma| within [minF, maxF] and fewer patterns than the whole space. */
    public static boolean meetsRule(long frequent, long space, int minF, int maxF) {
        return frequent >= minF && frequent <= maxF && frequent < space;
    }

    /** Number of words of length 1 to maxLength; Long.MAX_VALUE when that overflows a long. */
    public static long patternSpace(int alphabet, int maxLength) {
        long total = 0;
        long words = 1;
        try {
            for (int length = 1; length <= maxLength; length++) {
                words = Math.multiplyExact(words, alphabet);
                total = Math.addExact(total, words);
            }
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
        return total;
    }

    private static String byLength(int[] counts) {
        StringBuilder out = new StringBuilder();
        for (int k = 0; k < counts.length; k++) {
            out.append(k == 0 ? "" : "/").append(counts[k]);
        }
        return out.toString();
    }

    private static int[] parseList(String text, String name) {
        if (text.isBlank()) {
            throw new IllegalArgumentException("--" + name + " needs a comma separated list, "
                    + "for instance --" + name + "=600,800");
        }
        String[] parts = text.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Integer.parseInt(parts[i].trim());
        }
        return out;
    }
}
