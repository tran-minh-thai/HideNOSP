package hidenosp;

import hidenosp.exp.Sc3MainComparison;
import hidenosp.io.ResultSink;
import hidenosp.io.RunId;
import hidenosp.util.Cli;
import hidenosp.util.Deadline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The time limit of SC3 (preregistration section 5: a method over the limit is dropped from the
 * pair and counted). Three properties: a limit that is not reached changes no row; a limit that is
 * already over turns every method of the H2 set into an OT row with empty results, which proves each
 * of them reaches a check; and the limit is gone once the method returns or throws.
 */
class Sc3TimeLimitTest {

    private static final String H2_METHODS =
            "--methods=cut-hide-rw-ls,cut-hide-rw,cut-hide,hha,dbsh,sbsh-r1,sbsh-rmax";

    /** The rows of one SC3 run in file order, each a map from column to value. */
    private static List<Map<String, String>> run(String... extra) throws IOException {
        List<String> args = new ArrayList<>(List.of("--sigma=30", "--gmin=0", "--gmax=3",
                "--maxPatternLength=3", "--setsPerStratum=1", H2_METHODS));
        args.addAll(List.of(extra));
        Path root = Files.createTempDirectory("sc3limit");
        Sc3MainComparison scenario = new Sc3MainComparison();
        List<String> allowed = new ArrayList<>(List.of(scenario.options()));
        allowed.add("probe");
        scenario.run(new ResultSink(root, RunId.create(2026, "dev"), true),
                new Cli(args.toArray(new String[0]), allowed.toArray(new String[0])));
        List<Path> csv;
        try (Stream<Path> s = Files.list(root.resolve("results-probe"))) {
            csv = s.filter(p -> p.getFileName().toString().endsWith(".csv")).toList();
        }
        assertEquals(1, csv.size(), "one SC3 file");
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(csv.get(0))) {
            if (!line.startsWith("#")) {
                lines.add(line);
            }
        }
        List<String> header = split(lines.get(0));
        assertEquals("status", header.get(header.size() - 1), "status is the last column");
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            List<String> cells = split(line);
            assertEquals(header.size(), cells.size(), "cells of " + line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int k = 0; k < header.size(); k++) {
                row.put(header.get(k), cells.get(k));
            }
            rows.add(row);
        }
        return rows;
    }

    /** Splits one CSV line of CsvWriter: a cell with a comma or a quote is quoted, quotes doubled. */
    private static List<String> split(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    @Test
    @DisplayName("a limit not reached changes no row; a limit already over gives OT for all seven methods")
    void timeLimit() throws IOException {
        List<Map<String, String>> none = run();
        List<Map<String, String>> generous = run("--timeLimit=3600");
        assertFalse(none.isEmpty(), "no row");
        assertEquals(none, generous, "a limit that is not reached must not change a row");
        for (Map<String, String> row : none) {
            assertEquals("", row.get("status"), "a finished method leaves status empty: " + row);
            assertFalse(row.get("mask_count").isEmpty(), "a finished method has |R|: " + row);
        }

        List<Map<String, String>> over = run("--timeLimit=1e-12");
        assertEquals(none.size(), over.size(), "one row per method and set, finished or not");
        Set<String> results = Set.of("hf", "lost", "lost_rel", "ghost", "dist_r", "sup_dist", "mask_count",
                "mask_ratio", "certified_pairs", "total_pairs", "lower_bound", "achieved_ratio",
                "frequent_before", "nonsensitive_before", "frequent_after");
        Set<String> keys = new java.util.TreeSet<>();
        for (int i = 0; i < over.size(); i++) {
            Map<String, String> row = over.get(i);
            assertEquals("OT", row.get("status"), "every method must reach a check: " + row);
            for (String column : results) {
                assertEquals("", row.get(column), column + " of an OT row: " + row);
            }
            assertEquals(none.get(i).get("method_key"), row.get("method_key"), "same order of rows");
            assertEquals(none.get(i).get("sensitive_set"), row.get("sensitive_set"), "same sets");
            assertEquals(none.get(i).get("min_masks"), row.get("min_masks"), "the bound is not a method result");
            keys.add(row.get("method_key"));
        }
        assertEquals(Set.of("cut-hide-rw-ls", "cut-hide-rw", "cut-hide", "hha", "dbsh", "sbsh-r1", "sbsh-rmax"),
                keys, "the seven methods of the H2 set");
        System.out.printf("SC3 time limit: %d rows unchanged under 3600 s, %d OT rows under 1e-12 s, %d methods%n",
                generous.size(), over.size(), keys.size());
    }

    @Test
    @DisplayName("the check does nothing outside a limit, and the limit ends with the work")
    void limitScope() {
        Deadline.check();
        assertEquals(7, Deadline.within(0, () -> 7), "0 means no limit");
        assertThrows(Deadline.Exceeded.class, () -> Deadline.within(1e-12, () -> {
            Deadline.check();
            return 0;
        }));
        Deadline.check();
        assertThrows(IllegalStateException.class, () -> Deadline.within(60, () -> Deadline.within(60, () -> 0)),
                "limits do not nest");
        Deadline.check();
        assertTrue(Deadline.within(60, () -> {
            Deadline.check();
            return true;
        }), "a limit not reached lets the work finish");
    }
}
