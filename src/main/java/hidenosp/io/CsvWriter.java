package hidenosp.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A small comma separated writer: header comments first, then one header row, then rows. */
public final class CsvWriter implements AutoCloseable {

    private final BufferedWriter writer;
    private final int columns;

    public CsvWriter(Path path, String[] header, String[] comments) {
        try {
            Files.createDirectories(path.getParent());
            this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
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

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot close the file", e);
        }
    }
}
