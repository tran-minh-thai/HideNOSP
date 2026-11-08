// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SC3LOST (preregistration section 19.2) on a small synthetic database: the rows join SC3 on the
 * sensitive set, and every row obeys the implementation checks of the section. The checks are counted
 * so that a pass over no comparable row cannot look like a pass.
 */
class Sc3LostOptimumTest {

    private static final List<String> CUT_HIDE = List.of("cut-hide", "cut-hide-rw", "cut-hide-rw-ls");

    @Test
    @DisplayName("Rows join SC3 on the set and obey the implementation checks of preregistration section 19.2")
    void checks() throws IOException {
        Path data = Files.createTempFile("sc3lost", ".txt");
        Random random = new Random(7);
        StringBuilder text = new StringBuilder();
        for (int s = 0; s < 60; s++) {
            int length = 10 + random.nextInt(25);
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < length; i++) {
                line.append(i == 0 ? "" : " ").append(random.nextInt(8));
            }
            text.append(line).append('\n');
        }
        Files.writeString(data, text, StandardCharsets.UTF_8);
        List<String> common = List.of("--dataset=" + data, "--format=PLAIN", "--sigma=15", "--gmin=0",
                "--gmax=2", "--maxPatternLength=3", "--setsPerStratum=2", "--seed=2026", "--patternKind=distinct");

        List<String> lostArgs = new ArrayList<>(common);
        lostArgs.addAll(List.of("--drops=2,3", "--window=12"));
        List<Map<String, String>> rows = run(new Sc3LostOptimum(), lostArgs);
        List<String> sc3Args = new ArrayList<>(common);
        sc3Args.addAll(List.of("--sensitiveSize=1", "--methods=cut-hide"));
        List<Map<String, String>> sc3 = run(new Sc3MainComparison(), sc3Args);

        Set<String> lostSets = new LinkedHashSet<>();
        rows.forEach(r -> lostSets.add(r.get("sensitive_set")));
        Set<String> sc3Sets = new LinkedHashSet<>();
        sc3.forEach(r -> sc3Sets.add(r.get("sensitive_set")));
        assertEquals(sc3Sets, lostSets, "SC3LOST draws the sets of SC3");

        int solved = 0;
        int methodRows = 0;
        int atMinimum = 0;
        int spread = 0;
        Set<String> instances = new LinkedHashSet<>();
        for (Map<String, String> r : rows) {
            assertFalse(r.get("status").startsWith("check failed"), r.toString());
            if (r.get("method_key").isEmpty()) {
                continue;
            }
            methodRows++;
            String instance = r.get("sensitive_set") + "/" + r.get("drop");
            if (instances.add(instance)) {
                solved++;
                spread += Integer.parseInt(r.get("worst_lost")) > Integer.parseInt(r.get("opt_lost")) ? 1 : 0;
            }
            assertEquals("", r.get("status"), "every method finishes and hides: " + r);
            int masks = Integer.parseInt(r.get("mask_count"));
            int min = Integer.parseInt(r.get("min_masks"));
            int lost = Integer.parseInt(r.get("lost"));
            int opt = Integer.parseInt(r.get("opt_lost"));
            assertTrue(masks >= min, "|R| >= |R|*: " + r);
            assertTrue(Integer.parseInt(r.get("worst_lost")) >= opt, "worst >= optimum: " + r);
            if (CUT_HIDE.contains(r.get("method_key"))) {
                assertEquals(min, masks, "CUT-HIDE keys reach |R|*: " + r);
            }
            if (masks == min) {
                atMinimum++;
                assertTrue(lost >= opt, "no method below the optimum at |R|*: " + r);
            }
        }
        System.out.printf("SC3LOST checks: %d instances solved, %d method rows, %d at |R|*, %d instances where "
                + "the minimum sets differ in Lost%n", solved, methodRows, atMinimum, spread);
        assertTrue(solved >= 3, "too few instances solved: " + solved);
        assertEquals(9 * solved, methodRows, "nine methods per solved instance");
        assertTrue(spread > 0, "no instance where the minimum hiding sets differ in Lost: the checks are vacuous");
    }

    @Test
    @DisplayName("Windows: contiguous slices; the carrying ones contain P, the others come from sequences without P")
    void windows() {
        Random random = new Random(11);
        int checkedWith = 0;
        int checkedWithout = 0;
        for (int trial = 0; trial < 50; trial++) {
            List<EventSequence> sequences = new ArrayList<>();
            for (int s = 0; s < 30; s++) {
                int[] events = new int[5 + random.nextInt(30)];
                for (int i = 0; i < events.length; i++) {
                    events[i] = random.nextInt(6);
                }
                sequences.add(new EventSequence(s, events));
            }
            SequenceDatabase database = new SequenceDatabase(sequences);
            Pattern pattern = new Pattern(random.nextInt(6), random.nextInt(6), random.nextInt(6));
            GapConstraint gap = GapConstraint.of(0, random.nextInt(3));
            List<EventSequence> out = new ArrayList<>();
            int with = Sc3LostOptimum.cut(database, pattern, gap, 12, 5, 5, new Random(trial), out);
            assertTrue(with <= 5 && out.size() - with <= 5, "at most five of each kind");
            for (int w = 0; w < out.size(); w++) {
                int[] events = out.get(w).events();
                assertTrue(events.length <= 12, "window longer than 12");
                assertTrue(isSliceOfSome(events, database), "window " + w + " is not a slice of a sequence");
                if (w < with) {
                    assertTrue(SupportNO.of(pattern, out.get(w), gap) > 0, "carrying window " + w + " lacks P");
                    checkedWith++;
                } else {
                    assertTrue(isSliceOfSequenceWithout(events, database, pattern, gap),
                            "window " + w + " should come from a sequence without P");
                    checkedWithout++;
                }
            }
        }
        System.out.printf("SC3LOST windows: %d carrying and %d other windows checked%n", checkedWith, checkedWithout);
        assertTrue(checkedWith >= 100 && checkedWithout >= 100, "too few windows checked");
    }

    private static boolean isSliceOfSome(int[] events, SequenceDatabase database) {
        for (int s = 0; s < database.size(); s++) {
            if (isSlice(events, database.get(s).events())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSliceOfSequenceWithout(int[] events, SequenceDatabase database, Pattern pattern,
                                                    GapConstraint gap) {
        for (int s = 0; s < database.size(); s++) {
            if (SupportNO.of(pattern, database.get(s), gap) == 0 && isSlice(events, database.get(s).events())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSlice(int[] part, int[] whole) {
        for (int o = 0; o + part.length <= whole.length; o++) {
            if (java.util.Arrays.equals(part, java.util.Arrays.copyOfRange(whole, o, o + part.length))) {
                return true;
            }
        }
        return false;
    }

    private static List<Map<String, String>> run(Scenario scenario, List<String> args) throws IOException {
        Path root = Files.createTempDirectory("sc3lost");
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
