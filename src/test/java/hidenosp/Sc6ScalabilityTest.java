package hidenosp;

import hidenosp.exp.Sc3MainComparison;
import hidenosp.exp.Sc6Scalability;
import hidenosp.exp.Scenario;
import hidenosp.io.ResultSink;
import hidenosp.io.RunId;
import hidenosp.util.Cli;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reduced SC6 (preregistration section 19.3): the timed code is the code of SC3, which the |R| and
 * Lost of every timed run must show, row by row; F_sigma(D) is timed once per cell on its own row
 * (section 20.1) and its pattern count is that of SC3's mining; and a development run may not write times.
 */
class Sc6ScalabilityTest {

    private static final String METHODS =
            "cut-hide,cut-hide-rw,cut-hide-rw-ls,hha,dbsh,sbsh-r1,sbsh-rmax,occurrence,hha-stop";
    private static final Set<String> READS = Set.of("cut-hide", "cut-hide-rw", "cut-hide-rw-ls", "sbsh-r1", "sbsh-rmax");

    @Test
    @DisplayName("Timed runs give the SC3 rows of the same set and method; index rows follow each set")
    void timedRunsAreTheSc3Runs() throws IOException {
        Path data = Files.createTempFile("sc6", ".txt");
        Random random = new Random(5);
        StringBuilder text = new StringBuilder();
        for (int s = 0; s < 50; s++) {
            int length = 10 + random.nextInt(30);
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < length; i++) {
                line.append(i == 0 ? "" : " ").append(random.nextInt(6));
            }
            text.append(line).append('\n');
        }
        Files.writeString(data, text, StandardCharsets.UTF_8);
        List<String> common = List.of("--dataset=" + data, "--format=PLAIN", "--sigma=25", "--gmin=0", "--gmax=2",
                "--maxPatternLength=3", "--setsPerStratum=2", "--seed=2026", "--patternKind=distinct",
                "--sensitiveSize=1", "--methods=" + METHODS);
        List<Map<String, String>> timed = run(new Sc6Scalability(), common, "TESTBOX", "results");
        List<Map<String, String>> sc3 = run(new Sc3MainComparison(), common, "dev", "results-probe");

        Map<String, Map<String, String>> sc3ByKey = new HashMap<>();
        for (Map<String, String> r : sc3) {
            sc3ByKey.put(r.get("sensitive_set") + "|" + r.get("method_key"), r);
        }
        int compared = 0;
        int indexRows = 0;
        int miningRows = 0;
        List<String> sets = new ArrayList<>();
        for (Map<String, String> r : timed) {
            assertEquals("", r.get("status"), r.toString());
            String key = r.get("method_key");
            if (key.startsWith("index-")) {
                indexRows++;
                continue;
            }
            if (key.equals("mine-frequent")) {
                miningRows++;
                assertEquals(0, compared + indexRows, "the mining row comes before every method row");
                int patterns = hidenosp.metrics.FrequentPatterns.mine(
                        hidenosp.io.DatasetReader.read(data, hidenosp.io.DatasetReader.Format.PLAIN).database(),
                        hidenosp.core.GapConstraint.of(0, 2), 3, 25).size();
                assertTrue(r.get("method").endsWith(": " + patterns + " patterns"), "pattern count of F_sigma(D): " + r);
                assertTrue(patterns > 0, "nothing mined");
                assertTrue(Integer.parseInt(r.get("repetitions")) >= 3, "repetitions of row 8: " + r);
                assertTrue(Double.parseDouble(r.get("wall_ms_mean")) > 0, "a mining time was measured: " + r);
                assertEquals("", r.get("mask_count") + r.get("lost"), "no |R| or Lost on the mining row: " + r);
                continue;
            }
            if (!sets.contains(r.get("sensitive_set"))) {
                sets.add(r.get("sensitive_set"));
            }
            assertEquals(Boolean.toString(READS.contains(key)), r.get("reads_frequent"), "clock of " + key);
            assertTrue(Integer.parseInt(r.get("repetitions")) >= 3, "repetitions of row 8: " + r);
            assertEquals("1", r.get("warmups"), "warm-up of row 9");
            assertTrue(Double.parseDouble(r.get("wall_ms_mean")) > 0, "a time was measured: " + r);
            Map<String, String> same = sc3ByKey.get(r.get("sensitive_set") + "|" + key);
            assertTrue(same != null, "an SC3 row for " + r);
            assertEquals(same.get("mask_count"), r.get("mask_count"), "|R| as in SC3: " + r);
            assertEquals(same.get("lost"), r.get("lost"), "Lost as in SC3: " + r);
            compared++;
        }
        System.out.printf("SC6: %d timed runs compared with SC3 over %d sets, %d index rows%n", compared, sets.size(),
                indexRows);
        assertEquals(9 * sets.size(), compared, "nine methods per kept set");
        assertEquals(1, miningRows, "F_sigma(D) timed once per cell");
        List<String> firstOfStratum = new ArrayList<>();
        List<String> strata = new ArrayList<>();
        for (Map<String, String> r : sc3) {
            if (!strata.contains(r.get("stratum"))) {
                strata.add(r.get("stratum"));
                firstOfStratum.add(r.get("sensitive_set"));
            }
        }
        assertEquals(firstOfStratum, sets, "the first set drawn in each stratum, in the order of SC3");
        assertTrue(sets.size() >= 3, "too few sets kept: " + sets.size());
        assertEquals(3 * sets.size(), indexRows, "three index rows per kept set");
    }

    @Test
    @DisplayName("A run stamped dev may not write times")
    void devRunRefused() {
        assertThrows(IllegalArgumentException.class, () -> run(new Sc6Scalability(),
                List.of("--dataset=none.txt"), "dev", "results"));
    }

    private static List<Map<String, String>> run(Scenario scenario, List<String> args, String machine, String tree)
            throws IOException {
        Path root = Files.createTempDirectory("sc6");
        List<String> allowed = new ArrayList<>(List.of(scenario.options()));
        allowed.add("probe");
        if (machine.equals("dev") && scenario instanceof Sc6Scalability) {
            Path data = Files.createTempFile("sc6dev", ".txt");
            Files.writeString(data, "0 1 2 0 1 2 0 1\n0 1 0 1 0 1\n", StandardCharsets.UTF_8);
            List<String> small = List.of("--dataset=" + data, "--format=PLAIN", "--sigma=2", "--gmin=0", "--gmax=1",
                    "--maxPatternLength=2", "--setsPerStratum=1", "--methods=hha");
            scenario.run(new ResultSink(root, RunId.create(2026, machine), false),
                    new Cli(small.toArray(new String[0]), allowed.toArray(new String[0])));
            return List.of();
        }
        scenario.run(new ResultSink(root, RunId.create(2026, machine), machine.equals("dev")),
                new Cli(args.toArray(new String[0]), allowed.toArray(new String[0])));
        List<Path> csv;
        try (Stream<Path> s = Files.list(root.resolve(tree))) {
            csv = s.filter(p -> p.getFileName().toString().endsWith(".csv")).toList();
        }
        assertEquals(1, csv.size(), "one result file in " + tree);
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(csv.get(0))) {
            if (!line.startsWith("#")) {
                lines.add(line);
            }
        }
        List<String> header = split(lines.get(0));
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
}
