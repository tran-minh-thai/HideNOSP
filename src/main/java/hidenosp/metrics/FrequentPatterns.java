// SPDX-License-Identifier: MIT
package hidenosp.metrics;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * F_sigma(D), the patterns of length 1 to l_max whose non-overlapping support reaches sigma
 * (design doc section 2). Lost, Ghost and SupDist are all counted over this set, so every scenario
 * that reports a side effect builds it twice, once on D and once on D-prime.
 *
 * <p>The enumeration is level-wise and prunes on the prefix, which is sound: if L and L-prime are
 * occurrences of P extended by one item that differ at every level, their prefixes differ at every
 * level too, so sup_NO(P extended, S) &le; sup_NO(P, S) in every sequence and hence over D. Every
 * prefix of a frequent pattern is therefore frequent, and a sequence that does not carry the prefix
 * cannot carry the extension. {@link #enumerateWithoutPruning} keeps the naive version for the
 * implementation check of SC1, which compares the two on small instances.
 *
 * <p>Two further consequences of the same inequality make the real datasets tractable, and neither
 * changes the set produced:
 *
 * <ul>
 *   <li>each frequent pattern carries its <em>projection</em>, the sequences where its support is
 *       positive; an extension is evaluated only there, never over the whole database;</li>
 *   <li>a length-one pattern needs no max flow at all, because its occurrences are single positions
 *       and non-overlapping means distinct, so sup_NO is the number of matching positions.</li>
 * </ul>
 *
 * <p>This class is a counting device, not a contribution: the mining algorithm of [R1] is cited for
 * the definition. SC1 checks the pruned mining against plain enumeration; a comparison against
 * NETGAP is not implemented (see the TODO in Sc1ImplementationCheck).
 */
public final class FrequentPatterns {

    private FrequentPatterns() {
    }

    /** A frequent pattern together with the sequences where its support is positive. */
    private static final class Projection {
        final Pattern pattern;
        final int[] sequences;
        final long support;

        Projection(Pattern pattern, int[] sequences, long support) {
            this.pattern = pattern;
            this.sequences = sequences;
            this.support = support;
        }
    }

    /**
     * F_sigma(D) with its supports, in ascending length then discovery order.
     *
     * @param maxLength l_max
     * @param minSupport sigma, as an absolute number of non-overlapping occurrences
     */
    public static Map<Pattern, Long> mine(SequenceDatabase database, GapConstraint gap,
                                          int maxLength, long minSupport) {
        Map<Pattern, Long> frequent = new LinkedHashMap<>();
        int[] alphabet = database.alphabet();
        List<Projection> level = firstLevel(database, alphabet, minSupport, frequent);
        for (int length = 2; length <= maxLength && !level.isEmpty(); length++) {
            List<Projection> next = new ArrayList<>();
            for (Projection prefix : level) {
                for (int item : alphabet) {
                    Projection extended = extend(database, gap, prefix, item, minSupport);
                    if (extended != null) {
                        frequent.put(extended.pattern, extended.support);
                        next.add(extended);
                    }
                }
            }
            level = next;
        }
        return frequent;
    }

    /** Length-one patterns, counted directly: for m = 1 an occurrence is one position. */
    private static List<Projection> firstLevel(SequenceDatabase database, int[] alphabet,
                                               long minSupport, Map<Pattern, Long> frequent) {
        Map<Integer, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < alphabet.length; i++) {
            index.put(alphabet[i], i);
        }
        long[] total = new long[alphabet.length];
        List<List<Integer>> carriers = new ArrayList<>(alphabet.length);
        for (int i = 0; i < alphabet.length; i++) {
            carriers.add(new ArrayList<>());
        }
        for (int s = 0; s < database.size(); s++) {
            EventSequence sequence = database.get(s);
            int[] count = new int[alphabet.length];
            for (int i = 0; i < sequence.length(); i++) {
                Integer slot = index.get(sequence.at(i));
                if (slot != null) {
                    count[slot]++;
                }
            }
            for (int a = 0; a < alphabet.length; a++) {
                if (count[a] > 0) {
                    total[a] += count[a];
                    carriers.get(a).add(s);
                }
            }
        }
        List<Projection> level = new ArrayList<>();
        for (int a = 0; a < alphabet.length; a++) {
            if (total[a] >= minSupport) {
                Pattern pattern = new Pattern(alphabet[a]);
                frequent.put(pattern, total[a]);
                level.add(new Projection(pattern, toArray(carriers.get(a)), total[a]));
            }
        }
        return level;
    }

    /** Extends one projection by one item, evaluating only inside the projection. */
    private static Projection extend(SequenceDatabase database, GapConstraint gap,
                                     Projection prefix, int item, long minSupport) {
        Pattern candidate = prefix.pattern.extend(item);
        long total = 0;
        List<Integer> carriers = new ArrayList<>();
        for (int s : prefix.sequences) {
            int support = SupportNO.of(candidate, database.get(s), gap);
            if (support > 0) {
                total += support;
                carriers.add(s);
            }
        }
        return total >= minSupport
                ? new Projection(candidate, toArray(carriers), total)
                : null;
    }

    private static int[] toArray(List<Integer> values) {
        int[] out = new int[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    /** The same set by brute force over every word of length up to l_max; SC1 compares the two. */
    public static Map<Pattern, Long> enumerateWithoutPruning(SequenceDatabase database, GapConstraint gap,
                                                             int maxLength, long minSupport) {
        Map<Pattern, Long> frequent = new LinkedHashMap<>();
        int[] alphabet = database.alphabet();
        List<int[]> words = new ArrayList<>();
        words.add(new int[0]);
        for (int length = 1; length <= maxLength; length++) {
            List<int[]> next = new ArrayList<>();
            for (int[] word : words) {
                for (int item : alphabet) {
                    int[] extended = Arrays.copyOf(word, word.length + 1);
                    extended[word.length] = item;
                    next.add(extended);
                    Pattern p = new Pattern(extended);
                    long support = SupportNO.of(p, database, gap);
                    if (support >= minSupport) {
                        frequent.put(p, support);
                    }
                }
            }
            words = next;
        }
        return frequent;
    }

    /**
     * B_eps of step 3: the non-sensitive patterns just above the threshold, that is those with
     * sigma &le; sup_NO(Q, D) &lt; (1 + eps) sigma. Their weight factor is
     * 1 / (sup_NO(Q, D) - sigma + 1), so a pattern sitting exactly on the threshold weighs most.
     */
    public static List<Pattern> nearThreshold(Map<Pattern, Long> frequent, long sigma, double epsilon,
                                              java.util.Set<Pattern> excluded) {
        List<Pattern> out = new ArrayList<>();
        double upper = (1 + epsilon) * sigma;
        for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
            if (!excluded.contains(e.getKey()) && e.getValue() >= sigma && e.getValue() < upper) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    /** The weight factors 1 / (sup_NO(Q, D) - sigma + 1) of the patterns of {@code band}. */
    public static double[] weightFactors(List<Pattern> band, Map<Pattern, Long> frequent, long sigma) {
        double[] factor = new double[band.size()];
        for (int i = 0; i < band.size(); i++) {
            factor[i] = 1.0 / (frequent.get(band.get(i)) - sigma + 1);
        }
        return factor;
    }

    /**
     * S-plus: the sensitive patterns together with every frequent pattern that contains one of them
     * as a subsequence. Hiding P necessarily hides its super-patterns, so they are excluded from
     * Lost and SupDist.
     *
     * <p>The design doc writes S-plus without pinning the containment order down; this reads it as
     * subsequence containment on the item sequence, which is the convention of [R3]. The choice is
     * declared in the protocol table so a reader can reproduce the counts.
     */
    public static java.util.Set<Pattern> closure(java.util.Collection<Pattern> sensitive,
                                                 java.util.Collection<Pattern> frequent) {
        java.util.Set<Pattern> out = new java.util.LinkedHashSet<>(sensitive);
        for (Pattern q : frequent) {
            for (Pattern p : sensitive) {
                if (isSubsequence(p, q)) {
                    out.add(q);
                    break;
                }
            }
        }
        return out;
    }

    /** True when the items of {@code small} appear inside {@code large} in order. */
    public static boolean isSubsequence(Pattern small, Pattern large) {
        int j = 0;
        for (int i = 0; i < large.length() && j < small.length(); i++) {
            if (large.at(i) == small.at(j)) {
                j++;
            }
        }
        return j == small.length();
    }
}
