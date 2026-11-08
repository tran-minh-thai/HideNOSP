// SPDX-License-Identifier: MIT
package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.Sanitization;
import hidenosp.metrics.FrequentPatterns;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The running example of Gkoulalas-Divanis and Loukides, KDD 2011: the database of Figure 1(a),
 * the sensitive sequences of Figure 1(b), psi = 3, no maximum gap, support = number of sequences.
 */
final class PaperFigure1 {

    static final String SYMBOLS = "abcdefgh";
    static final GapConstraint NO_MAXIMUM_GAP = GapConstraint.unbounded(0);
    static final int PSI = 3;

    private PaperFigure1() {
    }

    static int[] events(String letters) {
        int[] out = new int[letters.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = SYMBOLS.indexOf(letters.charAt(i));
        }
        return out;
    }

    static Pattern pattern(String letters) {
        return new Pattern(events(letters));
    }

    static SequenceDatabase database() {
        String[] rows = {"abccde", "abce", "aceb", "cab", "dfghh", "dhb", "dfhhh", "cdfg", "fdhfg"};
        List<EventSequence> sequences = new ArrayList<>();
        for (int t = 0; t < rows.length; t++) {
            sequences.add(new EventSequence(t, events(rows[t])));
        }
        return new SequenceDatabase(sequences);
    }

    static final List<Pattern> SENSITIVE =
            List.of(pattern("ace"), pattern("dfg"), pattern("dh"), pattern("fh"));

    static long sequenceSupport(Pattern p, SequenceDatabase database) {
        long count = 0;
        for (int s = 0; s < database.size(); s++) {
            if (SupportNO.of(p, database.get(s), NO_MAXIMUM_GAP) > 0) {
                count++;
            }
        }
        return count;
    }

    /** Every pattern up to length 3 supported by at least psi sequences, by enumeration. */
    static Map<Pattern, Long> frequent(SequenceDatabase database, long psi) {
        Map<Pattern, Long> out = new LinkedHashMap<>();
        List<Pattern> level = new ArrayList<>();
        for (int a = 0; a < SYMBOLS.length(); a++) {
            level.add(new Pattern(a));
        }
        for (int length = 1; length <= 3; length++) {
            List<Pattern> next = new ArrayList<>();
            for (Pattern p : level) {
                long support = sequenceSupport(p, database);
                if (support >= psi) {
                    out.put(p, support);
                    for (int a = 0; a < SYMBOLS.length(); a++) {
                        next.add(p.extend(a));
                    }
                }
            }
            level = next;
        }
        return out;
    }

    static Map<Pattern, Long> nonSensitive(Map<Pattern, Long> frequent) {
        Set<Pattern> closure = FrequentPatterns.closure(SENSITIVE, frequent.keySet(),
                hidenosp.core.GapConstraint.unbounded(0), FrequentPatterns.Closure.NECESSARY);
        Map<Pattern, Long> out = new LinkedHashMap<>();
        frequent.forEach((p, v) -> {
            if (!closure.contains(p)) {
                out.put(p, v);
            }
        });
        return out;
    }

    static String letters(Pattern p) {
        StringBuilder b = new StringBuilder("<");
        for (int j = 0; j < p.length(); j++) {
            b.append(j == 0 ? "" : ",").append(SYMBOLS.charAt(p.at(j)));
        }
        return b.append(">").toString();
    }

    /** The masked positions per sequence, sequences without a mask left out. */
    static Map<Integer, Set<Integer>> masks(Sanitization result) {
        Map<Integer, Set<Integer>> out = new TreeMap<>();
        result.masked().forEach((s, positions) -> {
            if (!positions.isEmpty()) {
                out.put(s, new TreeSet<>(positions));
            }
        });
        return out;
    }

    /** The non-sensitive frequent patterns that fall below psi after sanitising. */
    static Set<String> lost(Map<Pattern, Long> nonSensitive, SequenceDatabase after) {
        Set<String> out = new TreeSet<>();
        for (Pattern q : nonSensitive.keySet()) {
            if (sequenceSupport(q, after) < PSI) {
                out.add(letters(q));
            }
        }
        return out;
    }
}
