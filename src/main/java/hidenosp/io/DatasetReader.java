package hidenosp.io;

import hidenosp.core.EventSequence;
import hidenosp.core.SequenceDatabase;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the public datasets of section 11.2 into single-item event sequences.
 *
 * <p>Two formats cover every candidate. {@link Format#PLAIN} is one sequence per line with the item
 * codes separated by blanks, which is how MSNBC and the discretised trucks trajectories arrive.
 * {@link Format#SPMF} is the sequence format of the SPMF library: blank separated items, -1 closing
 * an itemset and -2 closing the sequence.
 *
 * <p>An SPMF itemset holding more than one item is outside the data model of this paper, which is a
 * sequence of single events (section 8.3). Such an itemset is linearised in ascending item order,
 * a deterministic rule, and the share of positions that required it is returned in
 * {@link Loaded#linearisedShare()}. Rule 2.2.10 asks for that share to be declared next to the
 * dataset, so a scenario must print it and not silently drop it.
 */
public final class DatasetReader {

    public enum Format { PLAIN, SPMF }

    /** A dataset with the provenance a result table has to quote. */
    public record Loaded(SequenceDatabase database, Format format, String source,
                         double linearisedShare, int itemsetsLinearised) {
    }

    private DatasetReader() {
    }

    public static Loaded read(Path path, Format format) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path, e);
        }
        List<EventSequence> sequences = new ArrayList<>();
        long positions = 0;
        long linearised = 0;
        int itemsets = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("@")) {
                continue;
            }
            int[] events;
            if (format == Format.PLAIN) {
                events = parsePlain(trimmed);
            } else {
                int[] counted = new int[2];
                events = parseSpmf(trimmed, counted);
                linearised += counted[0];
                itemsets += counted[1];
            }
            if (events.length == 0) {
                continue;
            }
            for (int item : events) {
                if (item < 0) {
                    throw new IllegalArgumentException("negative item code " + item + " in " + path
                            + "; the code -1 is reserved for the mask, so item codes must be at "
                            + "least 0. Re-encode the dataset before loading it.");
                }
            }
            positions += events.length;
            sequences.add(new EventSequence(sequences.size(), events));
        }
        double share = positions == 0 ? 0 : (double) linearised / positions;
        return new Loaded(new SequenceDatabase(sequences), format, path.toString(), share, itemsets);
    }

    private static int[] parsePlain(String line) {
        String[] parts = line.split("\\s+");
        int[] events = new int[parts.length];
        int k = 0;
        for (String part : parts) {
            if (!part.isEmpty()) {
                events[k++] = Integer.parseInt(part);
            }
        }
        return java.util.Arrays.copyOf(events, k);
    }

    /**
     * Parses one SPMF line. {@code counted[0]} receives the number of positions that came out of a
     * multi-item itemset and {@code counted[1]} the number of such itemsets.
     */
    private static int[] parseSpmf(String line, int[] counted) {
        String[] parts = line.split("\\s+");
        List<Integer> events = new ArrayList<>();
        List<Integer> itemset = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            int value = Integer.parseInt(part);
            if (value == -2) {
                break;
            }
            if (value == -1) {
                flush(itemset, events, counted);
                continue;
            }
            itemset.add(value);
        }
        flush(itemset, events, counted);
        int[] out = new int[events.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = events.get(i);
        }
        return out;
    }

    private static void flush(List<Integer> itemset, List<Integer> events, int[] counted) {
        if (itemset.isEmpty()) {
            return;
        }
        if (itemset.size() > 1) {
            counted[0] += itemset.size();
            counted[1]++;
            java.util.Collections.sort(itemset);
        }
        events.addAll(itemset);
        itemset.clear();
    }
}
