// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.exp.Sc3MainComparison;
import hidenosp.io.ResultSink;
import hidenosp.io.RunId;
import hidenosp.util.Cli;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The method under test of H2 and H2-R (preregistration section 12) as SC3 runs it: with one
 * sensitive pattern, cut-hide-rw-ls masks as many positions as cut-hide and cut-hide-rw and loses no
 * more patterns than either; with several, it and cut-hide-rw are the greedy of cut-hide and say so
 * in their rows (an end-to-end SC3 trial found cut-hide-rw stopping the whole run there).
 */
class MethodUnderTestScenarioTest {

    /** SC3 rows by sensitive set, then by method key: each row as a map from column to value. */
    private static Map<String, Map<String, Map<String, String>>> run(String... args) throws IOException {
        Path root = Files.createTempDirectory("sc3");
        Sc3MainComparison scenario = new Sc3MainComparison();
        List<String> allowed = new ArrayList<>(List.of(scenario.options()));
        allowed.add("probe");
        scenario.run(new ResultSink(root, RunId.create(2026, "dev"), true), new Cli(args, allowed.toArray(new String[0])));
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
        Map<String, Map<String, Map<String, String>>> out = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            List<String> cells = split(line);
            assertEquals(header.size(), cells.size(), "cells of " + line);
            Map<String, String> row = new HashMap<>();
            for (int k = 0; k < header.size(); k++) {
                row.put(header.get(k), cells.get(k));
            }
            out.computeIfAbsent(row.get("sensitive_set"), k -> new HashMap<>()).put(row.get("method_key"), row);
        }
        return out;
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
    @DisplayName("one pattern: same |R| as cut-hide, never more lost; several: the greedy of cut-hide")
    void methodUnderTest() throws IOException {
        Map<String, Map<String, Map<String, String>>> single = run("--sigma=30", "--gmin=0", "--gmax=3",
                "--maxPatternLength=3", "--setsPerStratum=1", "--methods=cut-hide,cut-hide-rw,cut-hide-rw-ls");
        assertTrue(!single.isEmpty(), "no sensitive set drawn");
        for (Map.Entry<String, Map<String, Map<String, String>>> e : single.entrySet()) {
            Map<String, String> base = e.getValue().get("cut-hide");
            Map<String, String> rw = e.getValue().get("cut-hide-rw");
            Map<String, String> tested = e.getValue().get("cut-hide-rw-ls");
            assertEquals(base.get("mask_count"), tested.get("mask_count"), "|R| of set " + e.getKey());
            assertEquals(base.get("mask_count"), rw.get("mask_count"), "|R| of set " + e.getKey());
            assertTrue(Integer.parseInt(tested.get("lost")) <= Integer.parseInt(rw.get("lost")), e.getKey());
            assertTrue(Integer.parseInt(rw.get("lost")) <= Integer.parseInt(base.get("lost")), e.getKey());
            assertEquals("0", tested.get("hf").replaceAll("\\.0+$", ""), "HF of set " + e.getKey());
            assertTrue(tested.get("method").contains("adaptive weight") && tested.get("method").contains("local search"),
                    tested.get("method"));
        }
        Map<String, Map<String, Map<String, String>>> several = run("--sigma=30", "--gmin=0", "--gmax=3",
                "--maxPatternLength=3", "--setsPerStratum=1", "--sensitiveSize=2",
                "--methods=cut-hide,cut-hide-rw,cut-hide-rw-ls");
        assertTrue(!several.isEmpty(), "no sensitive set of two patterns drawn");
        for (Map.Entry<String, Map<String, Map<String, String>>> e : several.entrySet()) {
            Map<String, String> base = e.getValue().get("cut-hide");
            for (String key : List.of("cut-hide-rw", "cut-hide-rw-ls")) {
                Map<String, String> row = e.getValue().get(key);
                assertEquals(base.get("method"), row.get("method"), key + " with several patterns runs the greedy of cut-hide");
                assertEquals(base.get("mask_count"), row.get("mask_count"), key + " " + e.getKey());
                assertEquals(base.get("lost"), row.get("lost"), key + " " + e.getKey());
            }
        }
        System.out.printf("Method under test through SC3: %d one-pattern sets, %d two-pattern sets%n",
                single.size(), several.size());
    }
}
