package hidenosp.exp;

import hidenosp.io.ResultSink;
import hidenosp.io.RunId;
import hidenosp.util.Cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The entry point: {@code java -cp target/classes hidenosp.exp.Runner SC1 --trials=200}.
 *
 * <p>Two options are read here rather than by the scenarios. {@code --machine} names the measuring
 * machine that row 5 to 7 of the protocol table declares; it defaults to {@code dev}, and a run
 * stamped {@code dev} may not write timing results. {@code --root} says where the three result
 * directories live, so a scenario can be run from anywhere.
 */
public final class Runner {

    private static final String[] SHARED_OPTIONS = {"machine", "root", "seed", "probe"};

    private Runner() {
    }

    public static void main(String[] args) {
        Map<String, Scenario> scenarios = registry();
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("list")) {
            printUsage(scenarios);
            return;
        }
        Scenario scenario = scenarios.get(args[0].toUpperCase(java.util.Locale.ROOT));
        if (scenario == null) {
            System.err.println("unknown scenario " + args[0]);
            printUsage(scenarios);
            System.exit(2);
            return;
        }
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);

        List<String> allowed = new ArrayList<>(List.of(scenario.options()));
        allowed.addAll(List.of(SHARED_OPTIONS));
        Cli options = new Cli(rest, allowed.toArray(new String[0]));

        long seed = options.getLong("seed", 2026);
        String machine = options.get("machine", "dev");
        Path root = Paths.get(options.get("root", "."));
        RunId runId = RunId.create(seed, machine);
        boolean probe = options.getBoolean("probe", false);
        ResultSink sink = new ResultSink(root, runId, probe);

        System.out.println(scenario.id() + " " + scenario.title());
        for (String line : runId.headerLines()) {
            System.out.println(line);
        }
        long start = System.nanoTime();
        scenario.run(sink, options);
        System.out.printf("done in %.1f s%n", (System.nanoTime() - start) / 1e9);
    }

    /**
     * Every scenario of the table of section 11.1, keyed by its identifier, and SIGMA, the helper
     * that applies the sigma rule of the protocol before any scenario runs on a dataset.
     */
    public static Map<String, Scenario> registry() {
        Map<String, Scenario> map = new LinkedHashMap<>();
        for (Scenario scenario : List.of(
                new Sc1ImplementationCheck(),
                new Sc2DataRegime(),
                new Sc3MainComparison(),
                new Sc3LowerBound(),
                new Sc4PerSequenceLimit(),
                new Sc5DeletionGhosts(),
                new Sc6Scalability(),
                new Sc7DesignVariants(),
                new Sc8OriginalProblem(),
                new SigmaScan())) {
            map.put(scenario.id(), scenario);
        }
        return map;
    }

    private static void printUsage(Map<String, Scenario> scenarios) {
        System.out.println("usage: java -cp target/classes hidenosp.exp.Runner <SCn> [--key=value]");
        System.out.println();
        for (Scenario scenario : scenarios.values()) {
            System.out.printf("  %-5s %s%n", scenario.id(), scenario.title());
            System.out.printf("        options: %s%n", String.join(", ", scenario.options()));
        }
        System.out.println();
        System.out.println("  shared options: " + String.join(", ", SHARED_OPTIONS));
        System.out.println("  --machine names the declared measuring machine; the default dev");
        System.out.println("  may not write timing results into results/.");
        System.out.println("  --probe sends the invariant file to results-probe/ instead, for an");
        System.out.println("  exploratory run or a run on generated stand-in data.");
    }
}
