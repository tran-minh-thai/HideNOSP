// SPDX-License-Identifier: MIT
package hidenosp.exp;

import hidenosp.core.Pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The sensitive sets of the main comparison, drawn as the protocol fixes them.
 *
 * <p>The candidates are the frequent patterns of length 2 to 4 with distinct items. Sorted by
 * sup_NO (ties by the pattern order), they are cut into five strata of equal rank width, centred on
 * the percentiles 10, 30, 50, 70 and 90. Every stratum contributes up to a fixed number of
 * <em>distinct</em> sets of the requested size, drawn with a seed from that stratum only, so a set
 * has one support level and the five levels cover both ends of a skewed distribution.
 *
 * <p>Two things this class never does, because each would inflate the unit of repetition of the
 * statistical tests: it never returns the same set twice, and it never pads a stratum that cannot
 * supply enough sets. A stratum with fewer patterns than the set size supplies nothing; a stratum
 * with fewer possible sets than requested supplies all of them. The shortfall is reported in the
 * result, and the caller has to print it.
 */
public final class SensitiveSampling {

    /** The percentiles the five strata are centred on. */
    public static final int[] PERCENTILES = {10, 30, 50, 70, 90};

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 4;

    /** One sensitive set and the stratum it was drawn from. */
    public record Draw(List<Pattern> patterns, int percentile) {
    }

    /** The drawn sets together with what the pool allowed. */
    public record Result(List<Draw> draws, int poolSize, int[] stratumSize, int[] drawn,
                         int requestedPerStratum, int setSize, boolean repeatedItems) {

        public int requested() {
            return requestedPerStratum * PERCENTILES.length;
        }

        /** A line for the console and for the header of the result file. */
        public String describe() {
            StringBuilder b = new StringBuilder();
            b.append("sensitive sets: size ").append(setSize)
                    .append(repeatedItems ? ", repeated-item pool " : ", pool ").append(poolSize)
                    .append(" candidates, ").append(draws.size()).append(" of ").append(requested())
                    .append(" requested sets drawn; per stratum (percentile: candidates -> sets)");
            for (int k = 0; k < PERCENTILES.length; k++) {
                b.append(k == 0 ? " " : ", ").append('p').append(PERCENTILES[k]).append(": ")
                        .append(stratumSize[k]).append(" -> ").append(drawn[k]);
            }
            return b.toString();
        }
    }

    private SensitiveSampling() {
    }

    /**
     * The repeated-item counterpart of {@link #pool}: the frequent patterns of length 2 to 4 in
     * which some item occurs twice or more, ordered the same way. Theorem P8 makes their least
     * masks exact when g- = 0, which is what lets them be profiled and hidden like the pool.
     */
    public static List<Pattern> repeatedPool(Map<Pattern, Long> frequent) {
        List<Pattern> pool = new ArrayList<>();
        for (Pattern p : frequent.keySet()) {
            if (p.length() >= MIN_LENGTH && p.length() <= MAX_LENGTH && !p.hasDistinctItems()) {
                pool.add(p);
            }
        }
        pool.sort((a, b) -> {
            int bySupport = Long.compare(frequent.get(a), frequent.get(b));
            return bySupport != 0 ? bySupport : a.compareTo(b);
        });
        return pool;
    }

    /** The candidates, sorted by sup_NO and then by pattern: the order the strata are cut from. */
    public static List<Pattern> pool(Map<Pattern, Long> frequent) {
        List<Pattern> pool = new ArrayList<>();
        for (Pattern p : frequent.keySet()) {
            if (p.length() >= MIN_LENGTH && p.length() <= MAX_LENGTH && p.hasDistinctItems()) {
                pool.add(p);
            }
        }
        pool.sort((a, b) -> {
            int bySupport = Long.compare(frequent.get(a), frequent.get(b));
            return bySupport != 0 ? bySupport : a.compareTo(b);
        });
        return pool;
    }

    /**
     * @param size number of patterns in a set
     * @param perStratum number of distinct sets asked from every stratum
     * @param seed the sampling seed of the protocol; each stratum derives its own generator from it
     */
    public static Result draw(Map<Pattern, Long> frequent, int size, int perStratum, long seed) {
        return draw(frequent, size, perStratum, seed, false);
    }

    /**
     * As {@link #draw(Map, int, int, long)}, from the repeated-item pool when {@code repeatedItems}
     * is true: the same five strata and the same rule of distinct sets.
     */
    public static Result draw(Map<Pattern, Long> frequent, int size, int perStratum, long seed,
                              boolean repeatedItems) {
        if (size < 1 || perStratum < 1) {
            throw new IllegalArgumentException("set size and sets per stratum must be positive");
        }
        List<Pattern> pool = repeatedItems ? repeatedPool(frequent) : pool(frequent);
        int n = pool.size();
        int strata = PERCENTILES.length;
        int[] stratumSize = new int[strata];
        int[] drawn = new int[strata];
        List<Draw> draws = new ArrayList<>();
        for (int k = 0; k < strata; k++) {
            int from = (int) ((long) k * n / strata);
            int to = (int) ((long) (k + 1) * n / strata);
            List<Pattern> stratum = pool.subList(from, to);
            stratumSize[k] = stratum.size();
            List<List<Pattern>> sets = setsOf(stratum, size, perStratum, new Random(seed * 1_000_003L + k));
            drawn[k] = sets.size();
            for (List<Pattern> set : sets) {
                draws.add(new Draw(set, PERCENTILES[k]));
            }
        }
        return new Result(draws, n, stratumSize, drawn, perStratum, size, repeatedItems);
    }

    /** Up to {@code wanted} distinct sets of {@code size} members of the stratum. */
    private static List<List<Pattern>> setsOf(List<Pattern> stratum, int size, int wanted,
                                              Random random) {
        int n = stratum.size();
        if (n < size) {
            return List.of();
        }
        if (combinations(n, size, wanted) <= wanted) {
            List<List<Pattern>> all = new ArrayList<>();
            enumerate(stratum, size, 0, new ArrayList<>(), all);
            return all;
        }
        Set<List<Integer>> seen = new LinkedHashSet<>();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            indices.add(i);
        }
        while (seen.size() < wanted) {
            Collections.shuffle(indices, random);
            List<Integer> chosen = new ArrayList<>(indices.subList(0, size));
            Collections.sort(chosen);
            seen.add(chosen);
        }
        List<List<Pattern>> out = new ArrayList<>();
        for (List<Integer> chosen : seen) {
            List<Pattern> set = new ArrayList<>();
            for (int i : chosen) {
                set.add(stratum.get(i));
            }
            out.add(set);
        }
        return out;
    }

    /** C(n, k), or {@code cap + 1} as soon as it exceeds {@code cap}. */
    static long combinations(int n, int k, long cap) {
        long c = 1;
        for (int i = 1; i <= k; i++) {
            c = c * (n - k + i) / i;
            if (c > cap) {
                return cap + 1;
            }
        }
        return c;
    }

    private static void enumerate(List<Pattern> stratum, int size, int start, List<Pattern> prefix,
                                  List<List<Pattern>> out) {
        if (prefix.size() == size) {
            out.add(new ArrayList<>(prefix));
            return;
        }
        for (int i = start; i < stratum.size(); i++) {
            prefix.add(stratum.get(i));
            enumerate(stratum, size, i + 1, prefix, out);
            prefix.remove(prefix.size() - 1);
        }
    }
}
