package hidenosp.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The machine lines of the result header: the processor model comes from HIDENOSP_CPU and says
 * "not reported" with the reason when it is absent; the JVM fills in logical processors and
 * physical memory, both positive integers on a working JVM.
 */
class RunIdMachineLinesTest {

    @Test
    @DisplayName("processor model from the environment, logical processors and memory from the JVM")
    void machineLines() {
        String[] given = RunId.machineLines(Map.of("HIDENOSP_CPU", "  Test CPU   Model "));
        assertEquals("# cpu Test CPU   Model", given[0]);
        assertTrue(Integer.parseInt(given[1].substring("# logical_processors ".length())) > 0, given[1]);
        assertTrue(given[2].startsWith("# ram_bytes "), given[2]);
        assertTrue(Long.parseLong(given[2].substring("# ram_bytes ".length())) > 0, given[2]);

        String[] absent = RunId.machineLines(Map.of());
        assertTrue(absent[0].startsWith("# cpu not reported: "), absent[0]);
        String[] blank = RunId.machineLines(Map.of("HIDENOSP_CPU", "   "));
        assertTrue(blank[0].startsWith("# cpu not reported: "), blank[0]);

        String[] header = RunId.create(7, "dev").headerLines();
        assertEquals(8, header.length, "five run lines and three machine lines");
        assertTrue(header[5].startsWith("# cpu ") && header[6].startsWith("# logical_processors ")
                && header[7].startsWith("# ram_bytes "), String.join(" | ", header));
    }
}
