// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.io.CsvWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A result file takes its final name only when the run says it is complete. */
class CsvWriterTest {

    @Test
    @DisplayName("complete: the final name, no partial file")
    void completeRun() throws IOException {
        Path dir = Files.createTempDirectory("csv");
        Path file = dir.resolve("SC9_x.csv");
        try (CsvWriter w = new CsvWriter(file, new String[] {"a", "b"}, new String[] {"# run x"})) {
            w.writeRow("1", "2");
            w.complete();
        }
        assertTrue(Files.exists(file));
        assertFalse(Files.exists(dir.resolve("SC9_x.csv" + CsvWriter.PARTIAL)));
        assertEquals("# run x\na,b\n1,2\n", Files.readString(file).replace("\r\n", "\n"));
    }

    @Test
    @DisplayName("a run that stops early leaves only the partial file")
    void stoppedRun() throws IOException {
        Path dir = Files.createTempDirectory("csv");
        Path file = dir.resolve("SC9_y.csv");
        assertThrows(IllegalStateException.class, () -> {
            try (CsvWriter w = new CsvWriter(file, new String[] {"a"}, new String[0])) {
                w.writeRow("1");
                throw new IllegalStateException("stops here, as an out-of-memory error would");
            }
        });
        assertFalse(Files.exists(file), "no file may carry the final name");
        assertTrue(Files.exists(dir.resolve("SC9_y.csv" + CsvWriter.PARTIAL)));
    }
}
