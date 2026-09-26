package hidenosp;

import hidenosp.exp.Sc1ImplementationCheck;
import hidenosp.exp.Sc2DataRegime;
import hidenosp.exp.Sc3MainComparison;
import hidenosp.exp.Scenario;
import hidenosp.exp.SigmaScan;
import hidenosp.io.CsvWriter;
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

/**
 * Every scenario that writes a result file completes it: one run, one file with its final name,
 * no .partial file left behind. Catches a scenario that forgets CsvWriter.complete().
 */
class ScenarioFilesTest {

    private static List<Path> files(Path root, String suffix) throws IOException {
        List<Path> out = new ArrayList<>();
        Path dir = root.resolve("results-probe");
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().endsWith(suffix)).forEach(out::add);
        }
        return out;
    }

    private static void runOnce(Scenario scenario, String... args) throws IOException {
        runExpecting(1, scenario, args);
    }

    private static void runExpecting(int files, Scenario scenario, String... args) throws IOException {
        Path root = Files.createTempDirectory("scenario");
        List<String> allowed = new ArrayList<>(List.of(scenario.options()));
        allowed.add("probe");
        Cli options = new Cli(args, allowed.toArray(new String[0]));
        scenario.run(new ResultSink(root, RunId.create(2026, "dev"), true), options);
        assertEquals(files, files(root, ".csv").size(), scenario.id() + ": complete files");
        assertEquals(0, files(root, CsvWriter.PARTIAL).size(), scenario.id() + ": no partial file");
    }

    @Test
    @DisplayName("SC1, SC2, SC3 and SIGMA leave one complete file and no partial file")
    void everyScenarioCompletesItsFile() throws IOException {
        runOnce(new Sc1ImplementationCheck(), "--trials=20");
        runOnce(new Sc2DataRegime(), "--sigma=5", "--gmin=0", "--gmax=3", "--maxPatternLength=3");
        runOnce(new Sc3MainComparison(), "--sigma=10", "--gmin=0", "--gmax=3", "--maxPatternLength=3",
                "--setsPerStratum=1", "--methods=cut-hide-uniform,HH");
        // --pareto adds the file of the Pareto front: two complete files.
        // (sigma = 30: at 10 the near-threshold band of the built-in data is empty and the
        // standard CUT-HIDE refuses to run, as it should.)
        runExpecting(2, new Sc3MainComparison(), "--sigma=30", "--gmin=0", "--gmax=3",
                "--maxPatternLength=3", "--setsPerStratum=1", "--methods=cut-hide", "--pareto=true");
        Path data = Files.createTempFile("sigma", ".txt");
        Files.writeString(data, "0 -1 1 -1 0 -1 1 -1 -2\n1 -1 0 -1 1 -1 -2\n0 -1 0 -1 1 -1 -2\n");
        runOnce(new SigmaScan(), "--dataset=" + data, "--format=SPMF", "--sigma=1,2", "--gmax=1,3",
                "--maxPatternLength=3", "--minF=1", "--maxF=100");
    }
}
