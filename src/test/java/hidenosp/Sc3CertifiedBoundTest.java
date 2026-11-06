package hidenosp;

import hidenosp.exp.Sc3CertifiedBound;
import hidenosp.exp.Sc3LostOptimum;
import hidenosp.exp.Sc3MainComparison;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SC3CERT (preregistration section 20.2) on a small database with a planted gadget, so that some patterns are
 * lost in every minimum hiding set: the full-data Lost equals the SC3 row, the small instances are those of
 * SC3LOST (joined on set and drop), and the bound never exceeds the exact optimum nor the Lost.
 */
class Sc3CertifiedBoundTest {

    @Test
    @DisplayName("Joins SC3 and SC3LOST; bound <= optimum <= Lost on every joined row")
    void checks() throws IOException {
        Path data = gadget();
        List<String> common = List.of("--dataset=" + data, "--format=PLAIN", "--sigma=20", "--gmin=0", "--gmax=2",
                "--maxPatternLength=3", "--setsPerStratum=2", "--seed=2026", "--patternKind=distinct");
        checks(data, common);
    }

    @Test
    @DisplayName("--parts=small writes the small rows of a run of both parts, unchanged, and no full-data row")
    void smallPartAlone() throws IOException {
        Path data = gadget();
        List<String> args = List.of("--dataset=" + data, "--format=PLAIN", "--sigma=20", "--gmin=0", "--gmax=2",
                "--maxPatternLength=3", "--setsPerStratum=2", "--seed=2026", "--patternKind=distinct",
                "--drops=2,3", "--window=12");
        List<Map<String, String>> both = run(new Sc3CertifiedBound(), args);
        List<String> smallArgs = new ArrayList<>(args);
        smallArgs.add("--parts=small");
        List<Map<String, String>> alone = run(new Sc3CertifiedBound(), smallArgs);
        List<Map<String, String>> expected = both.stream().filter(r -> r.get("instance").equals("small")).toList();
        assertTrue(both.stream().anyMatch(r -> r.get("instance").equals("full")), "the default run has full rows");
        assertTrue(expected.stream().filter(r -> r.get("status").isEmpty()).count() >= 5,
                "small rows with a bound compared: " + expected.size());
        assertEquals(expected, alone, "the small rows, column by column");
        List<String> badArgs = new ArrayList<>(args);
        badArgs.add("--parts=small,tiny");
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> run(new Sc3CertifiedBound(), badArgs));
        assertTrue(thrown.getMessage().contains("tiny"), thrown.getMessage());
    }

    private static Path gadget() throws IOException {
        Path data = Files.createTempFile("sc3cert", ".txt");
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

    private static void checks(Path data, List<String> common) throws IOException {
        List<String> small = List.of("--drops=2,3", "--window=12");
        List<String> certArgs = new ArrayList<>(common);
        certArgs.addAll(small);
        List<Map<String, String>> cert = run(new Sc3CertifiedBound(), certArgs);
        List<String> lostArgs = new ArrayList<>(common);
        lostArgs.addAll(small);
        lostArgs.add("--methods=cut-hide-rw-ls");
        List<Map<String, String>> lost = run(new Sc3LostOptimum(), lostArgs);
        List<String> sc3Args = new ArrayList<>(common);
        sc3Args.addAll(List.of("--sensitiveSize=1", "--methods=cut-hide-rw-ls"));
        List<Map<String, String>> sc3 = run(new Sc3MainComparison(), sc3Args);

        Map<String, Map<String, String>> sc3BySet = new HashMap<>();
        sc3.forEach(r -> sc3BySet.put(r.get("sensitive_set"), r));
        Map<String, Map<String, String>> lostByInstance = new HashMap<>();
        lost.forEach(r -> lostByInstance.put(r.get("sensitive_set") + "|" + r.get("drop"), r));
        int fullRows = 0;
        int smallJoined = 0;
        int positive = 0;
        for (Map<String, String> r : cert) {
            if (!r.get("status").isEmpty()) {
                assertTrue(!r.get("status").startsWith("check failed"), r.toString());
                Map<String, String> exact = lostByInstance.get(r.get("sensitive_set") + "|" + r.get("drop"));
                if (r.get("instance").equals("small") && exact != null && r.get("status").startsWith("method failed")) {
                    assertTrue(exact.get("status").startsWith("method failed"),
                            "the method fails here but not in SC3LOST: " + r);
                }
                continue;
            }
            int lostHere = Integer.parseInt(r.get("lost"));
            int bound = Integer.parseInt(r.get("bound"));
            assertTrue(bound <= lostHere, "bound above Lost: " + r);
            positive += bound > 0 ? 1 : 0;
            if (r.get("instance").equals("full")) {
                Map<String, String> same = sc3BySet.get(r.get("sensitive_set"));
                assertTrue(same != null, "an SC3 row for " + r);
                assertEquals(same.get("lost"), r.get("lost"), "Lost as in SC3: " + r);
                assertEquals(same.get("mask_count"), r.get("mask_count"), "|R| as in SC3: " + r);
                fullRows++;
            } else {
                Map<String, String> exact = lostByInstance.get(r.get("sensitive_set") + "|" + r.get("drop"));
                assertTrue(exact != null, "an SC3LOST row for " + r);
                if (exact.get("opt_lost").isEmpty()) {
                    continue;
                }
                assertEquals(exact.get("instance_sigma"), r.get("instance_sigma"), "the same instance: " + r);
                assertEquals(exact.get("lost"), r.get("lost"), "the same reference Lost as SC3LOST: " + r);
                assertTrue(bound <= Integer.parseInt(exact.get("opt_lost")), "bound above the exact optimum: " + r);
                smallJoined++;
            }
        }
        System.out.printf("SC3CERT checks: %d full-data rows equal to SC3, %d small instances joined with SC3LOST, "
                + "%d rows with a positive bound%n", fullRows, smallJoined, positive);
        assertTrue(fullRows >= 5 && smallJoined >= 5, "too few rows compared");
        assertTrue(positive > 0, "the bound is 0 everywhere: the comparison with the optimum is vacuous");
    }

    private static List<Map<String, String>> run(Scenario scenario, List<String> args) throws IOException {
        Path root = Files.createTempDirectory("sc3cert");
        List<String> allowed = new ArrayList<>(List.of(scenario.options()));
        allowed.add("probe");
        scenario.run(new ResultSink(root, RunId.create(2026, "dev"), true),
                new Cli(args.toArray(new String[0]), allowed.toArray(new String[0])));
        List<Path> csv;
        try (Stream<Path> s = Files.list(root.resolve("results-probe"))) {
            csv = s.filter(p -> p.getFileName().toString().endsWith(".csv")).toList();
        }
        assertEquals(1, csv.size(), "one result file");
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
