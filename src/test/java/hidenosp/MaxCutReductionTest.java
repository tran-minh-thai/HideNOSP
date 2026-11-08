// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.metrics.FrequentPatterns;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The second objective, fewest lost patterns among the minimum-mask hiding sets, is NP-hard even
 * for one sensitive pattern of two distinct items: a reduction from Max-Cut.
 *
 * <p>For a graph G = (V, E) and a threshold sigma: items a, b, one item e per edge and, when
 * sigma &gt; 1, |E| + 1 private items f per extra sequence. The sequence of vertex v is a b followed
 * by the items of the edges at v. Padding: sigma copies of "a" and of "b"; sigma - 1 copies of
 * "a e" and "b e" for every edge item, so that (a, e) and (b, e) have support sigma + 1; and
 * sigma - 1 extra sequences "a b f f ... f", each private item with sigma - 1 copies of "a f" and
 * "b f". P = (a, b), g- = 0, g+ unbounded (or at least the largest degree when sigma = 1).
 *
 * <p>P has support |V| + sigma - 1, so a minimum-mask hiding set masks a or b in exactly |V| of its
 * occurrences. Masking a in vertex v kills (a, e) there for every edge e at v; (a, e) of edge uv is
 * lost exactly when both u and v mask a. Killing an extra sequence loses its |E| + 1 private
 * patterns, more than any vertex-only choice loses. So the least Lost is C + |E| - maxcut(G), where
 * C (the patterns that occur in one vertex sequence only, with a twin on the other item) does not
 * depend on the choice and is 0 when sigma &gt; 1 or l_max = 2.
 *
 * <p>The test checks this from the definitions: F_sigma by enumerating every word, every set of
 * |V| positions among the occurrences of P, supports on the masked database. It also checks that
 * two wrong constructions are caught.
 */
class MaxCutReductionTest {

    private static final int A = 0;
    private static final int B = 1;
    static final Pattern P = new Pattern(A, B);

    /** A wrong construction the test must catch. */
    enum Fault { NONE, NO_PADDING, EDGE_ITEM_IN_A_THIRD_SEQUENCE }

    private static EventSequence sequence(int id, List<Integer> events) {
        return new EventSequence(id, events.stream().mapToInt(Integer::intValue).toArray());
    }

    /** The vertex sequences come first, then the extra sequences carrying P, then the padding. */
    static SequenceDatabase build(int vertices, List<int[]> edges, int sigma, Fault fault) {
        List<EventSequence> list = new ArrayList<>();
        for (int v = 0; v < vertices; v++) {
            List<Integer> events = new ArrayList<>(List.of(A, B));
            for (int e = 0; e < edges.size(); e++) {
                int[] edge = edges.get(e);
                boolean third = fault == Fault.EDGE_ITEM_IN_A_THIRD_SEQUENCE && e == 0
                        && v == firstVertexOutside(edge, vertices);
                if (edge[0] == v || edge[1] == v || third) {
                    events.add(2 + e);
                }
            }
            list.add(sequence(list.size(), events));
        }
        int nextItem = 2 + edges.size();
        List<Integer> privateItems = new ArrayList<>();
        for (int j = 0; j < sigma - 1; j++) {
            List<Integer> events = new ArrayList<>(List.of(A, B));
            for (int k = 0; k <= edges.size(); k++) {
                privateItems.add(nextItem);
                events.add(nextItem++);
            }
            list.add(sequence(list.size(), events));
        }
        if (fault == Fault.NO_PADDING) {
            return new SequenceDatabase(list);
        }
        for (int c = 0; c < sigma; c++) {
            list.add(sequence(list.size(), List.of(A)));
            list.add(sequence(list.size(), List.of(B)));
        }
        List<Integer> padded = new ArrayList<>(privateItems);
        for (int e = 0; e < edges.size(); e++) {
            padded.add(2 + e);
        }
        for (int item : padded) {
            for (int c = 0; c < sigma - 1; c++) {
                list.add(sequence(list.size(), List.of(A, item)));
                list.add(sequence(list.size(), List.of(B, item)));
            }
        }
        return new SequenceDatabase(list);
    }

    private static int firstVertexOutside(int[] edge, int vertices) {
        for (int v = 0; v < vertices; v++) {
            if (v != edge[0] && v != edge[1]) {
                return v;
            }
        }
        return -1;
    }

    static int maxCut(int vertices, List<int[]> edges) {
        int best = 0;
        for (int side = 0; side < (1 << vertices); side++) {
            int cut = 0;
            for (int[] e : edges) {
                if (((side >> e[0]) & 1) != ((side >> e[1]) & 1)) {
                    cut++;
                }
            }
            best = Math.max(best, cut);
        }
        return best;
    }

    /**
     * The outcome of checking one instance. {@code constant}: Lost minus the monochromatic edges is
     * the same for every choice that masks every vertex sequence. {@code hidingSets}: the number
     * of minimum-mask hiding sets met, {@code leastLost} the least Lost over all of them.
     */
    private record Check(boolean constant, int hidingSets, int constantPart, int leastLost,
                         int leastLostVertexOnly, int leastLostWithExtra) {
    }

    private static Check check(int vertices, List<int[]> edges, int sigma, GapConstraint gap,
                               int maxLength, Fault fault) {
        SequenceDatabase db = build(vertices, edges, sigma, fault);
        Map<Pattern, Long> frequent = FrequentPatterns.enumerateWithoutPruning(db, gap, maxLength, sigma);
        Set<Pattern> closure = FrequentPatterns.closure(List.of(P), frequent.keySet(), gap, FrequentPatterns.Closure.NECESSARY);
        // Candidate masks: a and b of every sequence that carries P. Masking anything else leaves
        // P untouched, and every occurrence needs its own mask, so a minimum hiding set is a set of
        // |V| candidates.
        List<int[]> candidates = new ArrayList<>();
        int carriers = 0;
        for (int s = 0; s < db.size(); s++) {
            if (SupportNO.of(P, db.get(s), gap) > 0) {
                candidates.add(new int[] {s, 0});
                candidates.add(new int[] {s, 1});
                carriers++;
            }
        }
        assertEquals(vertices + sigma - 1, carriers, "sequences carrying P");
        Integer constantPart = null;
        boolean constant = true;
        int leastLost = Integer.MAX_VALUE;
        int leastVertexOnly = Integer.MAX_VALUE;
        int leastWithExtra = Integer.MAX_VALUE;
        int hidingSets = 0;
        for (List<Integer> chosen : subsets(candidates.size(), vertices)) {
            Map<Integer, List<Integer>> masks = new HashMap<>();
            for (int k : chosen) {
                masks.computeIfAbsent(candidates.get(k)[0], x -> new ArrayList<>()).add(candidates.get(k)[1]);
            }
            SequenceDatabase after = db.mask(masks);
            if (SupportNO.of(P, after, gap) >= sigma) {
                continue;
            }
            hidingSets++;
            int lost = 0;
            for (Map.Entry<Pattern, Long> e : frequent.entrySet()) {
                if (!closure.contains(e.getKey()) && SupportNO.of(e.getKey(), after, gap) < sigma) {
                    lost++;
                }
            }
            leastLost = Math.min(leastLost, lost);
            boolean vertexOnly = true;
            for (int s : masks.keySet()) {
                vertexOnly &= s < vertices;
            }
            if (!vertexOnly) {
                leastWithExtra = Math.min(leastWithExtra, lost);
                continue;
            }
            leastVertexOnly = Math.min(leastVertexOnly, lost);
            int monochromatic = 0;
            for (int[] e : edges) {
                if (masks.get(e[0]).get(0).equals(masks.get(e[1]).get(0))) {
                    monochromatic++;
                }
            }
            if (constantPart == null) {
                constantPart = lost - monochromatic;
            } else if (constantPart != lost - monochromatic) {
                constant = false;
            }
        }
        return new Check(constant, hidingSets, constantPart == null ? -1 : constantPart, leastLost,
                leastVertexOnly, leastWithExtra);
    }

    /** Every subset of {0..n-1} with k elements, as sorted lists. */
    private static List<List<Integer>> subsets(int n, int k) {
        List<List<Integer>> out = new ArrayList<>();
        collect(n, k, 0, new ArrayList<>(), out);
        return out;
    }

    private static void collect(int n, int k, int from, List<Integer> current, List<List<Integer>> out) {
        if (current.size() == k) {
            out.add(new ArrayList<>(current));
            return;
        }
        for (int i = from; i <= n - (k - current.size()); i++) {
            current.add(i);
            collect(n, k, i + 1, current, out);
            current.remove(current.size() - 1);
        }
    }

    static List<int[]> randomGraph(Random random, int vertices, int maxDegree) {
        List<int[]> edges = new ArrayList<>();
        int[] degree = new int[vertices];
        for (int u = 0; u < vertices; u++) {
            for (int v = u + 1; v < vertices; v++) {
                if (random.nextInt(3) != 0 && degree[u] < maxDegree && degree[v] < maxDegree) {
                    edges.add(new int[] {u, v});
                    degree[u]++;
                    degree[v]++;
                }
            }
        }
        return edges;
    }

    @Test
    @DisplayName("least Lost over the minimum-mask sets equals a constant plus |E| - maxcut")
    void lostEncodesMaxCut() {
        Random random = new Random(20260926);
        int instances = 0;
        int hidingSets = 0;
        int notBipartite = 0;
        int extraChecked = 0;
        int[] perSigma = new int[4];
        for (int trial = 0; instances < 60; trial++) {
            int sigma = 1 + trial % 3;
            boolean boundedGap = sigma == 1 && trial % 2 == 1;
            int vertices = 2 + random.nextInt(sigma == 1 ? 5 : 4);
            List<int[]> edges = randomGraph(random, vertices, boundedGap ? 3 : vertices);
            if (edges.isEmpty() || edges.size() > (sigma == 1 ? 8 : 6)) {
                continue;
            }
            // With sigma = 1 there are no extra sequences, and g+ = 3 reaches the third edge item
            // after b from a, so graphs of degree 3 fit.
            GapConstraint gap = boundedGap ? GapConstraint.of(0, 3)
                    : GapConstraint.of(0, GapConstraint.UNBOUNDED_MAX);
            int maxLength = sigma > 1 ? 2 + (trial / 3) % 2 : 2 + edges.size() <= 6 ? 4 : 3;
            String where = vertices + " vertices, edges " + describe(edges) + ", sigma " + sigma
                    + ", gap " + gap + ", l_max " + maxLength;

            // P occurs once in each sequence that carries it, on positions 0 and 1 only.
            SequenceDatabase db = build(vertices, edges, sigma, Fault.NONE);
            for (int s = 0; s < vertices + sigma - 1; s++) {
                EventSequence seq = db.get(s);
                assertEquals(1, SupportNO.of(P, seq, gap), where);
                for (int i = 0; i < seq.length(); i++) {
                    assertEquals(i <= 1 ? 0 : 1, SupportNO.of(P, seq.mask(i), gap), where + ", mask " + i);
                }
            }
            // So |R|* = |V|, and the exact CUT-HIDE agrees.
            HideProblem problem = new HideProblem(db, List.of(P), gap, HideProblem.Mode.HIDE_DB, sigma, 0.5, maxLength);
            assertEquals(vertices, new CutHide(CutHideConfig.uniformWeights()).run(problem).maskCount(), where);

            Check c = check(vertices, edges, sigma, gap, maxLength, Fault.NONE);
            int cut = maxCut(vertices, edges);
            assertTrue(c.constant(), "Lost minus the monochromatic edges varies, " + where);
            assertEquals(c.constantPart() + edges.size() - cut, c.leastLostVertexOnly(), where);
            assertEquals(c.leastLostVertexOnly(), c.leastLost(), "an extra sequence was worth killing, " + where);
            if (sigma > 1 || maxLength == 2) {
                assertEquals(0, c.constantPart(), where);
            }
            if (sigma > 1) {
                // The strict reduction behind the hardness of approximation (design P16): a set that
                // touches an extra sequence loses more than every edge, so it is worse than any colouring.
                assertTrue(c.leastLostWithExtra() >= edges.size() + 1,
                        "a set touching an extra sequence loses " + c.leastLostWithExtra() + ", " + where);
                extraChecked++;
            }
            int binomial = 1;
            for (int k = 0; k < vertices; k++) {
                binomial = binomial * (vertices + sigma - 1 - k) / (k + 1);
            }
            assertEquals(binomial << vertices, c.hidingSets(), "minimum-mask hiding sets, " + where);
            if (cut < edges.size()) {
                notBipartite++;
            }
            hidingSets += c.hidingSets();
            perSigma[sigma]++;
            instances++;
        }
        System.out.printf("Max-Cut reduction: %d graphs (sigma 1: %d, 2: %d, 3: %d), %d minimum-mask "
                + "hiding sets checked, %d graphs not bipartite, %d graphs where a set touching an extra "
                + "sequence was checked to lose more than every edge%n", instances, perSigma[1], perSigma[2],
                perSigma[3], hidingSets, notBipartite, extraChecked);
        assertTrue(extraChecked >= 30, "too few graphs with extra sequences: " + extraChecked);
        assertTrue(notBipartite >= 15, "graphs where the least Lost is not the constant: " + notBipartite);
    }

    @Test
    @DisplayName("the check catches a construction without padding or with a shared edge item")
    void wrongConstructionsAreCaught() {
        Random random = new Random(7);
        int[] tried = new int[3];
        int[] caught = new int[3];
        for (int trial = 0; tried[1] < 15; trial++) {
            int vertices = 3 + random.nextInt(3);
            List<int[]> edges = randomGraph(random, vertices, vertices);
            if (edges.isEmpty() || edges.size() > 7) {
                continue;
            }
            GapConstraint gap = GapConstraint.of(0, GapConstraint.UNBOUNDED_MAX);
            for (Fault fault : Fault.values()) {
                Check c = check(vertices, edges, 1, gap, 3, fault);
                tried[fault.ordinal()]++;
                if (!c.constant()) {
                    caught[fault.ordinal()]++;
                }
            }
        }
        System.out.printf("Wrong constructions caught: no padding %d/%d, shared edge item %d/%d, "
                + "correct construction flagged %d/%d%n", caught[1], tried[1], caught[2], tried[2],
                caught[0], tried[0]);
        assertEquals(0, caught[0], "the correct construction must never be flagged");
        assertEquals(tried[1], caught[1], "without padding, masking every a loses (a)");
        assertTrue(caught[2] >= tried[2] / 2, "an edge item in three sequences breaks the pairs");
    }

    private static String describe(List<int[]> edges) {
        StringBuilder b = new StringBuilder();
        for (int[] e : edges) {
            b.append(e[0]).append('-').append(e[1]).append(' ');
        }
        return b.toString().trim();
    }
}
