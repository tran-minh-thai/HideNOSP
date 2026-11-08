// SPDX-License-Identifier: MIT
package hidenosp.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A small parser for arguments of the shape {@code --key=value}, so the runner needs no dependency.
 * Unknown keys are an error rather than a shrug: a misspelled threshold that silently falls back to
 * a default would produce a result file whose header does not match what actually ran.
 */
public final class Cli {

    private final Map<String, String> values = new LinkedHashMap<>();

    public Cli(String[] args, String... allowed) {
        for (String arg : args) {
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("expected --key=value, got " + arg);
            }
            int split = arg.indexOf('=');
            String key = split < 0 ? arg.substring(2) : arg.substring(2, split);
            String value = split < 0 ? "true" : arg.substring(split + 1);
            boolean known = false;
            for (String candidate : allowed) {
                known |= candidate.equals(key);
            }
            if (!known) {
                throw new IllegalArgumentException("unknown option --" + key
                        + "; allowed: " + String.join(", ", allowed));
            }
            values.put(key, value);
        }
    }

    public String get(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public int getInt(String key, int fallback) {
        String value = values.get(key);
        return value == null ? fallback : Integer.parseInt(value);
    }

    public long getLong(String key, long fallback) {
        String value = values.get(key);
        return value == null ? fallback : Long.parseLong(value);
    }

    public double getDouble(String key, double fallback) {
        String value = values.get(key);
        return value == null ? fallback : Double.parseDouble(value);
    }

    public boolean getBoolean(String key, boolean fallback) {
        String value = values.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public Map<String, String> all() {
        return values;
    }
}
