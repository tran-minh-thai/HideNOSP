package hidenosp.exp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.hide.HideProblem;
import hidenosp.io.CsvWriter;
import hidenosp.io.DatasetReader;
import hidenosp.io.ResultSink;
import hidenosp.metrics.DataProfile;
import hidenosp.metrics.FrequentPatterns;
import hidenosp.util.Cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * SC2 of section 11.1: the data regime of each dataset and each sigma, printed before any
 * comparison is run.
 *
 * <p>For every candidate sensitive pattern it reports rho_rep, beta, the least number of masks
 * |R| star from P1 and its share of the database, exactly the four quantities section 8.4 requires
 * in advance. Their purpose is to classify a configuration before the results arrive: a
 * configuration whose rho_rep is near zero has sup_NO collapse to the sequence support and belongs
 * to the degenerate control group, and the same holds for beta and the gap constraint. Deciding
 * that afterwards, once the comparison is known, would be choosing the evidence to fit the claim.
 *
 * <p>With no dataset given the scenario runs on a small built-in synthetic database, so the
 * pipeline can be exercised before the public datasets are in place.
 */
public final class Sc2DataRegime implements Scenario {

    @Override
    public String id() {
        return "SC2";
    }

    @Override
    public String title() {
        return "data regime: rho_rep, beta and the bound of P1 per dataset and per sigma";
    }

    @Override
    public String[] options() {
        return new String[] {"dataset", "format", "gmin", "gmax", "sigma", "maxPatternLength",
                "patterns", "seed"};
    }

    @Override
    public void run(ResultSink sink, Cli options) {
        String datasetOption = options.get("dataset", "");
        GapConstraint gap = new GapConstraint(options.getInt("gmin", 0),
                options.getInt("gmax", 3));
        int sigma = options.getInt("sigma", 5);
        int maxPatternLength = options.getInt("maxPatternLength", 3);
        int patternCount = options.getInt("patterns", 10);
        long seed = options.getLong("seed", 2026);

        SequenceDatabase database;
        String source;
        double linearisedShare = 0;
        if (datasetOption.isEmpty()) {
            database = builtInSynthetic(seed);
            source = "built-in synthetic";
        } else {
            Path path = Paths.get(datasetOption);
            DatasetReader.Format format = DatasetReader.Format
                    .valueOf(options.get("format", "PLAIN").toUpperCase(java.util.Locale.ROOT));
            DatasetReader.Loaded loaded = DatasetReader.read(path, format);
            database = loaded.database();
            source = loaded.source();
            linearisedShare = loaded.linearisedShare();
        }

        Map<Pattern, Long> frequent =
                FrequentPatterns.mine(database, gap, maxPatternLength, sigma);
        List<Pattern> candidates = sampleByQuantile(frequent, patternCount, seed);

        String[] header = new String[DataProfile.header().length + 3];
        header[0] = "dataset";
        header[1] = "gap";
        header[2] = "sigma";
        System.arraycopy(DataProfile.header(), 0, header, 3, DataProfile.header().length);

        try (CsvWriter writer = sink.open(ResultSink.Kind.INVARIANT, id(), header)) {
            System.out.println("dataset " + source + ", |D| = " + database.size()
                    + ", total length " + database.totalLength()
                    + ", alphabet " + database.alphabet().length
                    + ", linearised share " + linearisedShare);
            System.out.println("|F_sigma(D)| = " + frequent.size() + " at sigma = " + sigma
                    + ", gap " + gap + ", l_max = " + maxPatternLength);
            for (Pattern pattern : candidates) {
                DataProfile profile =
                        DataProfile.of(database, pattern, gap, HideProblem.Mode.HIDE_DB, sigma);
                String[] row = new String[header.length];
                row[0] = source;
                row[1] = gap.toString();
                row[2] = Integer.toString(sigma);
                System.arraycopy(profile.row(), 0, row, 3, profile.row().length);
                writer.writeRow(row);
                System.out.printf("%-18s sup_NO %5d  sup_seq %5d  rho_rep %.3f  beta %.3f  "
                                + "min masks %5d (%.4f of D)%n",
                        pattern, profile.supNO(), profile.supSeq(), profile.repetitionRatio(),
                        profile.constraintEffect(), profile.minMasks(), profile.maskShare());
            }
        }
    }

    /**
     * Sensitive-set candidates drawn across the quantiles of sup_NO, as section 11.5 prescribes,
     * rather than from the top of the list only.
     */
    private List<Pattern> sampleByQuantile(Map<Pattern, Long> frequent, int count, long seed) {
        List<Map.Entry<Pattern, Long>> entries = new ArrayList<>(frequent.entrySet());
        entries.removeIf(e -> e.getKey().length() < 2);
        entries.sort(Map.Entry.comparingByValue());
        List<Pattern> out = new ArrayList<>();
        if (entries.isEmpty()) {
            return out;
        }
        Random random = new Random(seed);
        int buckets = Math.min(count, entries.size());
        for (int b = 0; b < buckets; b++) {
            int lo = (int) ((long) b * entries.size() / buckets);
            int hi = (int) ((long) (b + 1) * entries.size() / buckets);
            int pick = lo + random.nextInt(Math.max(1, hi - lo));
            out.add(entries.get(Math.min(pick, entries.size() - 1)).getKey());
        }
        return out;
    }

    /** A small database with visible repetition, so the pipeline runs before the datasets arrive. */
    private SequenceDatabase builtInSynthetic(long seed) {
        Random random = new Random(seed);
        List<EventSequence> sequences = new ArrayList<>();
        for (int s = 0; s < 30; s++) {
            int[] events = new int[40];
            for (int i = 0; i < events.length; i++) {
                events[i] = random.nextInt(4);
            }
            for (int copy = 0; copy < 4; copy++) {
                int start = random.nextInt(events.length - 4);
                events[start] = 0;
                events[start + 2] = 1;
            }
            sequences.add(new EventSequence(s, events));
        }
        return new SequenceDatabase(sequences);
    }
}
