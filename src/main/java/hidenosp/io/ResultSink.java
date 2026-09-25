package hidenosp.io;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Where a result file is allowed to go, following the project layout of section 11.8.
 *
 * <ul>
 *   <li>{@link Kind#INVARIANT} writes to {@code results-invariant/}: Lost, Ghost, |R|, HF and
 *       Nettree sizes. These do not depend on the machine, so any machine may produce them, and a
 *       timing column in such a file would be a category error.</li>
 *   <li>{@link Kind#TIMED} writes to {@code results/}: wall clock, processor time and peak memory.
 *       Only the declared measuring machine may produce them, so the sink refuses to open one when
 *       the run is stamped "dev".</li>
 *   <li>{@link Kind#PROBE} writes to {@code results-probe/}: exploratory measurements on a
 *       development machine. They never enter the paper as results, and they carry no timing column
 *       either, which is what keeps them distinguishable from the files of {@code results/}.</li>
 * </ul>
 *
 * <p>The checks below are deliberately mechanical. The rule they encode is the one place where a
 * mistake is silent and expensive: a timing number measured on a laptop and then printed in a table
 * as if it came from the declared machine.
 */
public final class ResultSink {

    public enum Kind {
        INVARIANT("results-invariant"),
        TIMED("results"),
        PROBE("results-probe");

        private final String directory;

        Kind(String directory) {
            this.directory = directory;
        }

        public String directory() {
            return directory;
        }
    }

    /** Column names that mark a measurement as machine dependent. */
    private static final String[] TIMING_WORDS = {"time", "ms", "sec", "second", "memory", "rss",
            "heap", "throughput", "speedup"};

    private final Path root;
    private final RunId runId;
    private final boolean probeMode;

    /**
     * @param probeMode when true, an invariant file is redirected to {@code results-probe/}. An
     *     exploratory run on a development machine, or one on generated data that stands in for a
     *     dataset, is a probe: section 8.2 keeps such numbers out of the paper, and the only way to
     *     keep that promise is to keep them out of the results directories in the first place
     */
    public ResultSink(Path root, RunId runId, boolean probeMode) {
        this.root = root;
        this.runId = runId;
        this.probeMode = probeMode;
    }

    public ResultSink(Path root, RunId runId) {
        this(root, runId, false);
    }

    public ResultSink(RunId runId) {
        this(Paths.get("."), runId, false);
    }

    /**
     * Opens a result file of the given kind. The file name carries the scenario and the run stamp,
     * so two runs never overwrite each other.
     *
     * @throws IllegalArgumentException when the columns do not match the kind, or when a timed file
     *     is opened from a run that is not stamped with a declared measuring machine
     * @throws IllegalStateException when a file with that name already exists; overwriting it would
     *     lose a result silently
     */
    public CsvWriter open(Kind kind, String scenario, String[] header) {
        return open(kind, scenario, header, new String[0]);
    }

    /**
     * As {@link #open(Kind, String, String[])}, with further header lines that describe how the rows
     * were produced, for instance the options of the run. Each line gets a leading "# " when it has
     * none, so no reader mistakes it for data.
     */
    public CsvWriter open(Kind kind, String scenario, String[] header, String[] extraComments) {
        if (probeMode && kind == Kind.INVARIANT) {
            kind = Kind.PROBE;
        }
        boolean timing = hasTimingColumn(header);
        if (kind != Kind.TIMED && timing) {
            throw new IllegalArgumentException("a " + kind + " file may not carry a timing or "
                    + "memory column; move it to results/ or drop the column");
        }
        if (kind == Kind.TIMED) {
            if (!timing) {
                throw new IllegalArgumentException("a timed file without a timing column belongs in "
                        + "results-invariant/");
            }
            if (runId.machineLabel().equalsIgnoreCase("dev")) {
                throw new IllegalArgumentException("run " + runId.stamp() + " is stamped dev, so it "
                        + "may not write timing results; declare the measuring machine first");
            }
        }
        Path path = root.resolve(kind.directory())
                .resolve(scenario + "_" + runId.stamp() + ".csv");
        if (java.nio.file.Files.exists(path)) {
            throw new IllegalStateException("a result file already exists at " + path
                    + "; refusing to overwrite it. Two runs sharing one stamp would lose a result "
                    + "silently, which is how a sweep can drop a configuration without any error.");
        }
        String[] comments = runId.headerLines();
        String[] withKind = new String[comments.length + 2 + extraComments.length];
        System.arraycopy(comments, 0, withKind, 0, comments.length);
        withKind[comments.length] = "# kind " + kind;
        withKind[comments.length + 1] = "# scenario " + scenario;
        for (int i = 0; i < extraComments.length; i++) {
            String line = extraComments[i].replace('\n', ' ');
            withKind[comments.length + 2 + i] = line.startsWith("#") ? line : "# " + line;
        }
        return new CsvWriter(path, header, withKind);
    }

    private static boolean hasTimingColumn(String[] header) {
        for (String column : header) {
            String lower = column.toLowerCase(Locale.ROOT);
            for (String word : TIMING_WORDS) {
                if (lower.equals(word) || lower.contains("_" + word) || lower.startsWith(word + "_")) {
                    return true;
                }
            }
        }
        return false;
    }

    public RunId runId() {
        return runId;
    }
}
