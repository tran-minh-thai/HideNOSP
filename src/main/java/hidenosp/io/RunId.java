package hidenosp.io;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The run stamp required by rule 2.6.8: every result file carries one, so a table in the paper can
 * be traced back to the run that produced it.
 *
 * <p>The timestamp carries milliseconds. Second resolution was not enough: two fast scenarios run
 * back to back landed in the same second and the second one silently overwrote the first, losing a
 * whole configuration out of a sweep without any error. Milliseconds make that unlikely, and
 * {@link ResultSink} refuses to overwrite an existing file, which makes it impossible.
 *
 * <p>The stamp holds the time in UTC, the host, the code revision and the seed. The revision is
 * read from the environment variable HIDENOSP_REVISION when present, so a continuous run can pass
 * the commit hash in without this class shelling out to git.
 */
public record RunId(String timestamp, String host, String revision, long seed, String machineLabel) {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);

    /**
     * @param machineLabel the declared measuring machine of row 5 to 7 of the protocol table, or
     *     "dev" for a probe run on a development machine
     */
    public static RunId create(long seed, String machineLabel) {
        String time = FORMAT.format(ZonedDateTime.now(ZoneOffset.UTC));
        String host = System.getenv().getOrDefault("COMPUTERNAME",
                System.getenv().getOrDefault("HOSTNAME", "unknown-host"));
        String revision = System.getenv().getOrDefault("HIDENOSP_REVISION", "unversioned");
        return new RunId(time, host, revision, seed, machineLabel);
    }

    /** The string stamped into file names and into the first column of every row. */
    public String stamp() {
        return timestamp + "_" + machineLabel + "_" + revision + "_s" + seed;
    }

    /** The header lines written at the top of a result file, each already prefixed with a hash. */
    public String[] headerLines() {
        return new String[] {
            "# run " + stamp(),
            "# host " + host,
            "# jdk " + System.getProperty("java.version") + " " + System.getProperty("java.vm.name"),
            "# heap_max_bytes " + Runtime.getRuntime().maxMemory(),
            "# seed " + seed,
        };
    }
}
