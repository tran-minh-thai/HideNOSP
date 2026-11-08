// SPDX-License-Identifier: MIT
package hidenosp.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A small comma separated writer: header comments first, then one header row, then rows.
 *
 * <p>The rows go to {@code <file>.partial}, and the file takes its final name only when the
 * scenario calls {@link #complete()} after its last row. A run that stops early (out of memory, a
 * killed process, a machine that restarts for an update) therefore leaves a {@code .partial} file,
 * which no reader picks up and tools/check_results.py reports, instead of a file that looks like a
 * result and misses its last rows. Measured case: the SC2 runs that ran out of memory on MSNBC left
 * files with 884 of 885 and 1448 of 1450 rows.
 */
public final class CsvWriter implements AutoCloseable {

    /** The suffix of a file whose run has not finished. */
    public static final String PARTIAL = ".partial";

    private final BufferedWriter writer;
    private final int columns;
    private final Path target;
    private final Path partial;
    private boolean complete;

    public CsvWriter(Path path, String[] header, String[] comments) {
        try {
            Files.createDirectories(path.getParent());
            this.target = path;
            this.partial = path.resolveSibling(path.getFileName() + PARTIAL);
            this.writer = Files.newBufferedWriter(partial, StandardCharsets.UTF_8);
            this.columns = header.length;
            for (String comment : comments) {
                writer.write(comment);
                writer.newLine();
            }
            writeRow(header);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot open " + path, e);
        }
    }

    public void writeRow(String... cells) {
        if (cells.length != columns) {
            throw new IllegalArgumentException("expected " + columns + " cells, got " + cells.length);
        }
        try {
            List<String> escaped = new ArrayList<>(cells.length);
            for (String cell : cells) {
                escaped.add(cell.contains(",") || cell.contains("\"")
                        ? "\"" + cell.replace("\"", "\"\"") + "\""
                        : cell);
            }
            writer.write(String.join(",", escaped));
            writer.newLine();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write a row", e);
        }
    }

    /** Declares that every row has been written; {@link #close()} then gives the final name. */
    public void complete() {
        this.complete = true;
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot close the file", e);
        }
        if (!complete) {
            System.err.println("INCOMPLETE: the run stopped before its last row; what was written is "
                    + "kept in " + partial + " and is not a result. Run the command again.");
            return;
        }
        try {
            try {
                Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(partial, target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot give " + partial + " its final name " + target, e);
        }
    }
}
