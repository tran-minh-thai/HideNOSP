// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.GapConstraint;
import hidenosp.exp.Sc3MainComparison;
import hidenosp.exp.Sc3Robustness;
import hidenosp.exp.Sc3Sensitivity;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scenarios of preregistration section 23: SC3SENS with a constant of CUT-HIDE set at its default gives the rows
 * of SC3, a malformed variant key is refused, SC3ROB writes the rows of SC3 and one support row per set, method,
 * pattern and constraint, and the constraints of lot F are the ones listed there.
 */
class ReopenedScenariosTest {

    private static final List<String> COMMON = List.of("--format=PLAIN", "--sigma=20", "--gmin=0", "--gmax=2",
            "--maxPatternLength=3", "--setsPerStratum=2", "--seed=2026", "--patternKind=distinct", "--sensitiveSize=1");

    @Test
    @DisplayName("Wider constraints: [0,1] gives [0,2] and [0,inf]; [0,3] gives [0,4], [0,6], [0,inf]; [0,10] 11, 20, inf")
    void widerConstraints() {
        assertEquals("[[0,1], [0,2], [0,inf]]", Sc3MainComparison.widerConstraints(GapConstraint.of(0, 1)).toString());
        assertEquals("[[0,3], [0,4], [0,6], [0,inf]]",
                Sc3MainComparison.widerConstraints(GapConstraint.of(0, 3)).toString());
        assertEquals("[[0,10], [0,11], [0,20], [0,inf]]",
                Sc3MainComparison.widerConstraints(GapConstraint.of(0, 10)).toString());
        assertEquals("[[1,2], [1,3], [1,4], [1,inf]]",
                Sc3MainComparison.widerConstraints(GapConstraint.of(1, 2)).toString());
    }

    @Test
    @DisplayName("SC3SENS: every constant at its default gives the rows of cut-hide-rw-ls; a changed constant is named")
    void defaultsGiveTheRowsOfSc3() throws IOException {
        Path data = gadget();
        List<String> args = args(data, "--methods=cut-hide-rw-ls,cut-hide-rw-ls@krw6,cut-hide-rw-ls@tau4,"
                + "cut-hide-rw-ls@passes50,cut-hide-rw-ls@recv30,cut-hide-rw-ls@tau8,cut-hide-rw-ls@tau2,cut-hide-rw-ls@krw1");
        List<Map<String, String>> rows = run(new Sc3Sensitivity(), args).get("SC3SENS");
        Map<String, Map<String, Map<String, String>>> byKey = new HashMap<>();
        for (Map<String, String> r : rows) {
            byKey.computeIfAbsent(r.get("method_key"), k -> new LinkedHashMap<>()).put(r.get("sensitive_set"), strip(r));
        }
        Map<String, Map<String, String>> plain = byKey.get("cut-hide-rw-ls");
        assertTrue(plain.size() >= 4, "sets compared: " + plain.size());
        for (String key : List.of("cut-hide-rw-ls@krw6", "cut-hide-rw-ls@tau4", "cut-hide-rw-ls@passes50",
                "cut-hide-rw-ls@recv30")) {
            assertEquals(plain, byKey.get(key), key);
        }
        // The constant must act, not only be named: some set changes under tau at half or twice its default.
        int changed = 0;
        for (String key : List.of("cut-hide-rw-ls@tau2", "cut-hide-rw-ls@tau8")) {
            for (Map.Entry<String, Map<String, String>> e : byKey.get(key).entrySet()) {
                Map<String, String> mine = new LinkedHashMap<>(e.getValue());
                Map<String, String> base = new LinkedHashMap<>(plain.get(e.getKey()));
                mine.remove("method");
                base.remove("method");
                changed += mine.equals(base) ? 0 : 1;
            }
        }
        assertTrue(changed > 0, "no set changes when tau is halved or doubled");
        String named = byKey.get("cut-hide-rw-ls@tau8").values().iterator().next().get("method");
        assertTrue(named.contains("m / 8.0"), named);
        assertNotEquals(plain.values().iterator().next().get("method"),
                byKey.get("cut-hide-rw-ls@krw1").values().iterator().next().get("method"));
        for (String bad : List.of("cut-hide-rw-ls@krw0", "cut-hide-rw-ls@foo3", "cut-hide-rw-ls@krw2.5", "cut-hide@krw3")) {
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> run(new Sc3Sensitivity(), args(data, "--methods=" + bad)), bad);
            assertTrue(thrown.getMessage().contains(bad), thrown.getMessage());
        }
    }

    @Test
    @DisplayName("SC3ROB: the rows of SC3, and one support row per set, method, pattern and wider constraint")
    void robustness() throws IOException {
        Path data = gadget();
        List<String> args = args(data, "--methods=cut-hide-rw-ls,hha,dbsh");
        Map<String, List<Map<String, String>>> rob = run(new Sc3Robustness(), args);
        List<Map<String, String>> sc3 = run(new Sc3MainComparison(), args).get("SC3");
        assertEquals(sc3, rob.get("SC3ROB"), "the comparison rows of SC3ROB are those of SC3");
        List<Map<String, String>> support = rob.get("SC3ROBSUP");
        int constraints = Sc3MainComparison.widerConstraints(GapConstraint.of(0, 2)).size();
        assertEquals(sc3.size() * constraints, support.size(), "one sensitive pattern per set");
        Map<String, List<Map<String, String>>> groups = support.stream().collect(Collectors.groupingBy(
                r -> r.get("sensitive_set") + "|" + r.get("method_key") + "|" + r.get("pattern"),
                LinkedHashMap::new, Collectors.toList()));
        for (List<Map<String, String>> g : groups.values()) {
            assertEquals("[0,2]", g.get(0).get("constraint"));
            assertTrue(Long.parseLong(g.get(0).get("sup_no_after")) < 20, "hidden at its own gap: " + g.get(0));
            for (int k = 1; k < g.size(); k++) {
                assertTrue(Long.parseLong(g.get(k).get("sup_no_after")) >= Long.parseLong(g.get(k - 1).get("sup_no_after")));
                assertTrue(Long.parseLong(g.get(k).get("sup_no_after")) <= Long.parseLong(g.get(k).get("sup_no_before")));
            }
        }
    }

    private static List<String> args(Path data, String methods) {
        List<String> out = new ArrayList<>(COMMON);
        out.add("--dataset=" + data);
        out.add(methods);
        return out;
    }

    private static Map<String, String> strip(Map<String, String> row) {
        Map<String, String> out = new LinkedHashMap<>(row);
        out.remove("method_key");
        return out;
    }

    private static Path gadget() throws IOException {
        Path data = Files.createTempFile("reopened", ".txt");
        Random random = new Random(3);
        StringBuilder text = new StringBuilder();
        for (int s = 0; s < 60; s++) {
            List<Integer> events = new ArrayList<>();
            int filler = random.nextInt(4);
            for (int i = 0; i < filler; i++) {
                events.add(3 + random.nextInt(3));
            }
            int copies = 1 + random.nextInt(3);
            for (int c = 0; c < copies; c++) {
                events.addAll(random.nextInt(events.size() + 1), List.of(0, 1, 1, 2));
            }
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < events.size(); i++) {
                line.append(i == 0 ? "" : " ").append(events.get(i));
            }
            text.append(line).append('\n');
        }
        Files.writeString(data, text, StandardCharsets.UTF_8);
        return data;
    }

    /** Every result file of one run, by its scenario prefix, as rows. */
    private static Map<String, List<Map<String, String>>> run(Scenario scenario, List<String> args) throws IOException {
        Path root = Files.createTempDirectory("reopened");
        List<String> allowed = new ArrayList<>(List.of(scenario.options()));
        allowed.add("probe");
        scenario.run(new ResultSink(root, RunId.create(2026, "dev"), true),
                new Cli(args.toArray(new String[0]), allowed.toArray(new String[0])));
        List<Path> csv;
        try (Stream<Path> s = Files.list(root.resolve("results-probe"))) {
            csv = s.filter(p -> p.getFileName().toString().endsWith(".csv")).toList();
        }
        Map<String, List<Map<String, String>>> out = new HashMap<>();
        for (Path file : csv) {
            List<String> lines = new ArrayList<>();
            for (String line : Files.readAllLines(file)) {
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
            out.put(file.getFileName().toString().split("_", 2)[0], rows);
        }
        return out;
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
