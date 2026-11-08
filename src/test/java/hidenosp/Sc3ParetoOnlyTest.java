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
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Pareto front in a batch of its own (--methods=none --pareto=true): the front is the same as
 * when it runs next to the methods, the SC3 file holds no row, and "none" without the front is
 * refused instead of writing an empty file.
 */
class Sc3ParetoOnlyTest {

    private static final String[] CELL = {"--sigma=30", "--gmin=0", "--gmax=3", "--maxPatternLength=3",
        "--setsPerStratum=1", "--pareto=true"};

    /** Data lines (no comment line) of the file whose name starts with the prefix. */
    private static List<String> rows(Path root, String prefix) throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.list(root.resolve("results-probe"))) {
            files = s.filter(p -> p.getFileName().toString().startsWith(prefix)
                    && p.getFileName().toString().endsWith(".csv")).toList();
        }
        assertEquals(1, files.size(), prefix + " files: " + files);
        List<String> out = new ArrayList<>();
        for (String line : Files.readAllLines(files.get(0))) {
            if (!line.startsWith("#")) {
                out.add(line);
            }
        }
        return out;
    }

    private static Path run(String methods) throws IOException {
        Path root = Files.createTempDirectory("sc3pareto");
        Sc3MainComparison scenario = new Sc3MainComparison();
        List<String> args = new ArrayList<>(List.of(CELL));
        args.add("--methods=" + methods);
        List<String> allowed = new ArrayList<>(List.of(scenario.options()));
        allowed.add("probe");
        scenario.run(new ResultSink(root, RunId.create(2026, "dev"), true),
                new Cli(args.toArray(new String[0]), allowed.toArray(new String[0])));
        return root;
    }

    @Test
    @DisplayName("the front alone equals the front next to the methods; no method row")
    void frontAlone() throws IOException {
        Path alone = run("none");
        Path together = run("cut-hide,hha");
        List<String> frontAlone = rows(alone, "SC3PARETO_");
        List<String> frontTogether = rows(together, "SC3PARETO_");
        assertTrue(frontAlone.size() > 1, "the front has rows");
        assertEquals(frontTogether, frontAlone, "same front with or without the methods");
        assertEquals(1, rows(alone, "SC3_").size(), "header only in the SC3 file of --methods=none");
        assertTrue(rows(together, "SC3_").size() > 1, "method rows when methods are given");
        System.out.printf("Pareto front alone: %d rows, same as next to the methods%n", frontAlone.size() - 1);
    }

    @Test
    @DisplayName("--methods=none without the front is refused")
    void noneWithoutFront() {
        Sc3MainComparison scenario = new Sc3MainComparison();
        List<String> allowed = new ArrayList<>(List.of(scenario.options()));
        allowed.add("probe");
        String[] args = {"--sigma=30", "--gmin=0", "--gmax=3", "--maxPatternLength=3", "--setsPerStratum=1",
            "--methods=none"};
        assertThrows(IllegalArgumentException.class, () -> scenario.run(
                new ResultSink(Files.createTempDirectory("sc3none"), RunId.create(2026, "dev"), true),
                new Cli(args, allowed.toArray(new String[0]))));
    }
}
