package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;
import hidenosp.index.PathCounts;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * SBSH, the side-effect based sequence hiding algorithm of A. Gkoulalas-Divanis and G. Loukides,
 * "Revisiting sequential pattern hiding to enhance utility", KDD 2011 (Algorithm 5, with
 * Algorithms 1 to 3 and the procedures of its Section 4.3).
 *
 * <p>The class follows the published procedure step by step:
 *
 * <ol>
 *   <li><b>Exclude-SBSH</b> (step 1): order the sequences that support a sensitive pattern and keep
 *       intact as many as the threshold allows; the rest are sanitised, in that order.</li>
 *   <li><b>Candidates-SBSH</b> (step 3): the greedy weighted set cover over symbols of Algorithm 1,
 *       flattened into one array P of positions, with the covered subsets S_j kept in H.</li>
 *   <li><b>Annotate-T</b> (step 4): colour every position carrying a symbol of a supported sensitive
 *       pattern by Algorithm 2 &mdash; black when no non-sensitive frequent pattern containing its
 *       symbol sits exactly at the threshold, red when removing it drops one of those below the
 *       threshold, gray otherwise.</li>
 *   <li><b>Main loop</b> (steps 7 to 37): black positions are removed; a red position is replaced by
 *       the union over the subsets S_j of Replace, which is the smaller of C-PA and C-SA; a gray
 *       position is re-annotated with the marks made so far and removed if it is still gray.</li>
 * </ol>
 *
 * <p><b>Where the paper leaves a choice open</b>, and what this class does:
 *
 * <ul>
 *   <li><em>Exclude-SBSH.</em> The text describes it as removing from D_S the transaction that
 *       supports the largest number of sensitive sequences, while the worked example of its
 *       Section 5.2.3 justifies the choices by the number of non-sensitive frequent sequences each
 *       transaction supports. Reproduced by hand, only the second reading yields the published
 *       output of its Figure 1(f): with the first one the third transaction of the example is
 *       sanitised before the eighth and loses its first two events instead of its last three. This
 *       class therefore orders by the number of supported non-sensitive frequent patterns
 *       (descending), then by the Cost of Algorithm 1 (descending, as in Algorithm 3), then by
 *       sequence index (descending, standing for "arbitrary"; ascending would swap the second and
 *       third transaction of the example, which the paper leaves open). D_S is processed in the
 *       same order. The test {@code SbshPaperExampleTest} pins the whole example.</li>
 *   <li><em>Invalid positions.</em> "P[i] = null" is read as the end of P, and a position whose
 *       nodes lie on no complete path any more (Update-paths has removed them) is skipped.</li>
 *   <li><em>Re-annotation.</em> Algorithm 5 only handles a gray position that is gray again. When the
 *       re-annotation returns black or red, the annotation is updated and the position is handled
 *       by that branch on the next pass.</li>
 *   <li><em>C-PA and C-SA.</em> At distance k the candidate set is the layer of nodes k edges before
 *       (after) the node of P[i], restricted to nodes still on a complete path; it is accepted when
 *       every position in it is annotated black. The first k that works is returned. Replace keeps
 *       the smaller of the two sets and C-PA on a tie.</li>
 *   <li><em>Ties in the greedy cover</em> are broken by the first occurrence of the symbol in the
 *       sequence, which reproduces the choice stated in the paper's Example 1.</li>
 * </ul>
 *
 * <p><b>Two transfers</b> have to be declared where this is used as a baseline. First, the paper
 * deletes events and this class masks them, so that every method applies the same modification.
 * Without a maximum gap the two give the same supports. Second, {@link SupportCount#NON_OVERLAPPING}
 * replaces the support of the paper (the number of sequences containing a pattern) by the number
 * of non-overlapping occurrences, everywhere the procedure reads a support: the keep budget of
 * Exclude-SBSH, the threshold test of Annotate and the loop condition. Nothing else changes, and
 * {@link SupportCount#SEQUENCE_COUNT} runs the procedure of the paper unchanged.
 */
public final class Sbsh implements HidingMethod {

    static final int BLACK = 0;
    static final int RED = 1;
    static final int GRAY = 2;
    static final int NOT_ANNOTATED = -1;

    private final int radius;
    private final SupportCount support;
    private Map<Pattern, Long> nonSensitiveFrequent;

    /** The transferred version on sup_NO, the one the main comparison runs. */
    public Sbsh(int radius) {
        this(radius, SupportCount.NON_OVERLAPPING);
    }

    /**
     * @param radius r of Replace, counted in edges of the matching graph, i.e. Nettree levels; the
     *     paper takes r in [1, length of the longest sensitive pattern]
     */
    public Sbsh(int radius, SupportCount support) {
        if (radius < 1) {
            throw new IllegalArgumentException("the radius of Replace starts at 1, got " + radius);
        }
        this.radius = radius;
        this.support = support;
    }

    /**
     * The non-sensitive frequent patterns, F_sigma(D) without the sensitive patterns and their
     * super-patterns, each with its support in the original database counted as the {@link SupportCount} of this instance
     * says. Annotate and Exclude-SBSH read them; the map may be empty, but it must be given.
     */
    public Sbsh withNonSensitiveFrequent(Map<Pattern, Long> supports) {
        this.nonSensitiveFrequent = new LinkedHashMap<>(supports);
        return this;
    }

    @Override
    public String name() {
        return (support == SupportCount.SEQUENCE_COUNT ? "SBSH-original-r" : "SBSH-r") + radius;
    }

    public int radius() {
        return radius;
    }

    @Override
    public Sanitization run(HideProblem problem) {
        if (nonSensitiveFrequent == null) {
            throw new IllegalStateException("SBSH needs the non-sensitive frequent patterns and their "
                    + "supports (withNonSensitiveFrequent); without them Annotate colours every "
                    + "position black and the run is not SBSH");
        }
        if (support == SupportCount.SEQUENCE_COUNT && !problem.isDatabaseMode()) {
            throw new IllegalArgumentException("the sequence-count support of the paper is defined "
                    + "for a database threshold only");
        }
        return new Run(problem).execute();
    }

    /** Units a sequence contributes to a support, given sup_NO of the pattern in that sequence. */
    private long unit(int supNO) {
        return support.unit(supNO);
    }

    /** State of one call to {@link #run}. */
    private final class Run {

        private final HideProblem problem;
        private final SequenceDatabase database;
        private final GapConstraint gap;
        private final List<Pattern> sensitive;
        private final List<Pattern> nonSensitive;
        /** Current database support of every sensitive pattern. */
        private final long[] sensitiveSupport;
        /** Current database support of every non-sensitive frequent pattern. */
        private final long[] nonSensitiveSupport;
        private final Sanitization result;
        /** For every carrying sequence: {non-sensitive pattern, sup_NO in that sequence} pairs. */
        private final Map<Integer, List<int[]>> nonSensitiveHereOf = new HashMap<>();

        Run(HideProblem problem) {
            this.problem = problem;
            this.database = problem.database();
            this.gap = problem.gap();
            this.sensitive = problem.sensitive();
            this.nonSensitive = new ArrayList<>(nonSensitiveFrequent.keySet());
            this.nonSensitiveSupport = new long[nonSensitive.size()];
            for (int q = 0; q < nonSensitive.size(); q++) {
                nonSensitiveSupport[q] = nonSensitiveFrequent.get(nonSensitive.get(q));
            }
            this.sensitiveSupport = new long[sensitive.size()];
            this.result = new Sanitization(database).method(name());
        }

        Sanitization execute() {
            int n = database.size();
            int[][] units = new int[sensitive.size()][n];
            for (int p = 0; p < sensitive.size(); p++) {
                for (int s = 0; s < n; s++) {
                    units[p][s] = (int) unit(SupportNO.of(sensitive.get(p), database.get(s), gap));
                    sensitiveSupport[p] += units[p][s];
                }
            }

            List<Integer> carrying = new ArrayList<>();
            Map<Integer, List<Integer>> supportedSensitive = new HashMap<>();
            for (int s = 0; s < n; s++) {
                List<Integer> ts = new ArrayList<>();
                for (int p = 0; p < sensitive.size(); p++) {
                    if (units[p][s] > 0) {
                        ts.add(p);
                    }
                }
                if (!ts.isEmpty()) {
                    carrying.add(s);
                    supportedSensitive.put(s, ts);
                }
            }

            for (int s : excludeSbsh(carrying, supportedSensitive, units)) {
                sanitise(s, supportedSensitive.get(s));
            }
            verify();
            return result;
        }

        /**
         * Exclude-SBSH: order, keep what the budget allows, and return the sequences to sanitise in
         * that order. Under a per-sequence limit every sequence at or above the limit has to be
         * sanitised, so only the order applies.
         */
        private List<Integer> excludeSbsh(List<Integer> carrying, Map<Integer, List<Integer>> ts,
                                          int[][] units) {
            Map<Integer, long[]> key = new HashMap<>();
            for (int s : carrying) {
                EventSequence sequence = database.get(s);
                List<int[]> here = new ArrayList<>();
                for (int q = 0; q < nonSensitive.size(); q++) {
                    int value = SupportNO.of(nonSensitive.get(q), sequence, gap);
                    if (value > 0) {
                        here.add(new int[] {q, value});
                    }
                }
                nonSensitiveHereOf.put(s, here);
                key.put(s, new long[] {here.size(), cover(sequence, ts.get(s)).cost()});
            }
            List<Integer> order = new ArrayList<>(carrying);
            order.sort(Comparator.<Integer>comparingLong(s -> -key.get(s)[0])
                    .thenComparingLong(s -> -key.get(s)[1])
                    .thenComparing(Comparator.<Integer>reverseOrder()));

            List<Integer> toSanitise = new ArrayList<>();
            if (!problem.isDatabaseMode()) {
                for (int s : order) {
                    for (int p : ts.get(s)) {
                        if (units[p][s] >= problem.threshold()) {
                            toSanitise.add(s);
                            break;
                        }
                    }
                }
                return toSanitise;
            }
            long[][] longUnits = new long[units.length][];
            for (int p = 0; p < units.length; p++) {
                longUnits[p] = java.util.Arrays.stream(units[p]).asLongStream().toArray();
            }
            return Exclude.toSanitise(order, ts, longUnits, problem.threshold());
        }

        private SymbolCover.Cover cover(EventSequence sequence, List<Integer> supported) {
            return SymbolCover.of(sequence, supported, sensitive);
        }

        /** Steps 3 to 39 of Algorithm 5 for one sequence. */
        private void sanitise(int s, List<Integer> supported) {
            Sequence t = new Sequence(s, supported);
            SymbolCover.Cover cover = cover(t.original, supported);
            int[] colour = t.annotateAll();
            List<Integer> positions = cover.flatPositions();

            int i = 0;
            while (i < positions.size() && t.maxUpdatedSupport() >= problem.threshold()) {
                int position = positions.get(i);
                if (!t.isValid(position)) {
                    i++;
                    continue;
                }
                if (colour[position] == BLACK) {
                    t.mask(position);
                    i++;
                } else if (colour[position] == RED) {
                    Set<Integer> alternatives = new LinkedHashSet<>();
                    for (Set<Integer> subset : cover.subsets()) {
                        if (t.hasSingleNodePath(subset, position)) {
                            break;
                        }
                        alternatives.addAll(t.replace(position, subset, colour, radius));
                    }
                    if (alternatives.isEmpty()) {
                        t.mask(position);
                        i++;
                    } else {
                        for (int alternative : alternatives) {
                            t.mask(alternative);
                        }
                    }
                } else {
                    int again = t.annotate(position);
                    if (again == GRAY) {
                        t.mask(position);
                        i++;
                    } else {
                        colour[position] = again;
                    }
                }
            }
            t.commit();
        }

        /** Post-condition: every sensitive pattern is below the threshold. A failure is a bug. */
        private void verify() {
            Exclude.verify(problem, result, support, name());
        }

        /** One sequence while it is being sanitised: its index, its marks and the supports they move. */
        private final class Sequence {

            final int index;
            final EventSequence original;
            final NettreeCutIndex nettrees;
            /** Index ids of the supported sensitive patterns, in the order of {@code supported}. */
            final int[] sensitiveId;
            final int[] sensitivePattern;
            /** Non-sensitive frequent patterns this sequence supports, and their index ids. */
            final List<Integer> nonSensitiveHere = new ArrayList<>();
            final Map<Integer, Integer> nonSensitiveId = new HashMap<>();
            final Map<Integer, Integer> nonSensitiveUnitAtStart = new HashMap<>();
            final int[] sensitiveUnitAtStart;
            final Set<Integer> marked = new TreeSet<>();
            private int version;
            private final Map<Integer, int[]> vitalCache = new HashMap<>();
            private final Map<Integer, Integer> vitalVersion = new HashMap<>();
            private final Map<Integer, PathCounts> countsCache = new HashMap<>();
            private final Map<Integer, Integer> countsVersion = new HashMap<>();

            Sequence(int index, List<Integer> supported) {
                this.index = index;
                this.original = database.get(index);
                this.nettrees = new NettreeCutIndex(original, gap);
                this.sensitivePattern = supported.stream().mapToInt(Integer::intValue).toArray();
                this.sensitiveId = new int[sensitivePattern.length];
                this.sensitiveUnitAtStart = new int[sensitivePattern.length];
                for (int k = 0; k < sensitivePattern.length; k++) {
                    sensitiveId[k] = nettrees.add(sensitive.get(sensitivePattern[k]), true);
                    sensitiveUnitAtStart[k] = (int) unit(nettrees.supNO(sensitiveId[k]));
                }
                for (int[] pair : nonSensitiveHereOf.get(index)) {
                    int q = pair[0];
                    int here = pair[1];
                    nonSensitiveHere.add(q);
                    int u = (int) unit(here);
                    nonSensitiveUnitAtStart.put(q, u);
                    // Only a pattern whose support can sit exactly at the threshold while this
                    // sequence is marked can turn a position red, so only those enter the index.
                    long now = nonSensitiveSupport[q];
                    long threshold = problem.frequentThreshold();
                    if (now >= threshold && now - u <= threshold) {
                        nonSensitiveId.put(q, nettrees.add(nonSensitive.get(q), false));
                    }
                }
            }

            /** Annotate-T: every position carrying a symbol of a supported sensitive pattern. */
            int[] annotateAll() {
                Set<Integer> symbols = new java.util.HashSet<>();
                for (int p : sensitivePattern) {
                    for (int item : sensitive.get(p).items()) {
                        symbols.add(item);
                    }
                }
                int[] colour = new int[original.length()];
                Arrays.fill(colour, NOT_ANNOTATED);
                for (int i = 0; i < original.length(); i++) {
                    if (symbols.contains(original.at(i))) {
                        colour[i] = annotate(i);
                    }
                }
                return colour;
            }

            /** Algorithm 2, with the supports computed as if the marked positions were removed. */
            int annotate(int position) {
                int symbol = original.at(position);
                boolean atThreshold = false;
                for (Map.Entry<Integer, Integer> e : nonSensitiveId.entrySet()) {
                    Pattern q = nonSensitive.get(e.getKey());
                    if (!contains(q, symbol)) {
                        continue;
                    }
                    int id = e.getValue();
                    long updated = nonSensitiveSupport[e.getKey()]
                            - (nonSensitiveUnitAtStart.get(e.getKey()) - unit(nettrees.supNO(id)));
                    if (updated != problem.frequentThreshold()) {
                        continue;
                    }
                    atThreshold = true;
                    if (lowers(id, position)) {
                        return RED;
                    }
                }
                return atThreshold ? GRAY : BLACK;
            }

            /** True when masking the position lowers the support units of index pattern {@code id}. */
            private boolean lowers(int id, int position) {
                int now = nettrees.supNO(id);
                if (now == 0 || marked.contains(position)) {
                    return false;
                }
                int after;
                if (nettrees.pattern(id).hasDistinctItems()) {
                    // A position carries at most one node, and it lowers sup_NO exactly when that
                    // node lies on every maximum family of disjoint paths.
                    after = Arrays.binarySearch(vital(id), position) >= 0 ? now - 1 : now;
                } else {
                    Set<Integer> trial = new TreeSet<>(marked);
                    trial.add(position);
                    after = SupportNO.of(nettrees.pattern(id), original.mask(trial), gap);
                }
                return unit(after) < unit(now);
            }

            private int[] vital(int id) {
                Integer seen = vitalVersion.get(id);
                if (seen == null || seen != version) {
                    vitalCache.put(id, nettrees.vitalPositions(id));
                    vitalVersion.put(id, version);
                }
                return vitalCache.get(id);
            }

            /** Max-updated-support: the largest current support over the supported sensitive patterns. */
            long maxUpdatedSupport() {
                long max = 0;
                for (int k = 0; k < sensitiveId.length; k++) {
                    long now = unit(nettrees.supNO(sensitiveId[k]));
                    long value = problem.isDatabaseMode()
                            ? sensitiveSupport[sensitivePattern[k]] - (sensitiveUnitAtStart[k] - now)
                            : nettrees.supNO(sensitiveId[k]);
                    max = Math.max(max, value);
                }
                return max;
            }

            /** A position still carries a node on a complete path of some supported sensitive pattern. */
            boolean isValid(int position) {
                if (marked.contains(position)) {
                    return false;
                }
                for (int id : sensitiveId) {
                    if (nettrees.occurrencesThrough(id, position) > 0) {
                        return true;
                    }
                }
                return false;
            }

            /** Update-paths: mark the position and remove it from every Nettree of the index. */
            void mask(int position) {
                if (marked.add(position)) {
                    nettrees.maskPosition(position);
                    result.mask(index, position);
                    version++;
                }
            }

            /** True when a sensitive pattern of the subset has a one-node complete path at the position. */
            boolean hasSingleNodePath(Set<Integer> subset, int position) {
                for (int k : subset) {
                    int id = sensitiveId[k];
                    if (nettrees.pattern(id).length() == 1
                            && nettrees.occurrencesThrough(id, position) > 0) {
                        return true;
                    }
                }
                return false;
            }

            /** Replace: the smaller of C-PA and C-SA; an empty set means neither found one. */
            List<Integer> replace(int position, Set<Integer> subset, int[] colour, int r) {
                List<Integer> before = directed(position, subset, colour, r, true);
                List<Integer> after = directed(position, subset, colour, r, false);
                if (before.isEmpty()) {
                    return after;
                }
                if (after.isEmpty()) {
                    return before;
                }
                return before.size() <= after.size() ? before : after;
            }

            /**
             * C-PA ({@code preceding} true) or C-SA: for k = 1, ..., r, the positions of the nodes k
             * edges before (after) every node of the position on a complete path of a pattern of the
             * subset. The first k at which that layer is non-empty and entirely black is returned.
             */
            private List<Integer> directed(int position, Set<Integer> subset, int[] colour, int r,
                                           boolean preceding) {
                for (int k = 1; k <= r; k++) {
                    Set<Integer> layer = new TreeSet<>();
                    boolean usable = true;
                    boolean anyNode = false;
                    for (int s : subset) {
                        int id = sensitiveId[s];
                        Nettree nettree = nettrees.nettree(id);
                        for (int v : nettree.nodesAtPosition(position)) {
                            if (!alive(id, v)) {
                                continue;
                            }
                            anyNode = true;
                            int target = nettree.level(v) + (preceding ? -k : k);
                            if (target < 0 || target >= nettree.pattern().length()) {
                                usable = false;
                                continue;
                            }
                            for (int u : reachable(id, v, k, preceding)) {
                                int p = nettree.position(u);
                                if (colour[p] != BLACK) {
                                    usable = false;
                                }
                                layer.add(p);
                            }
                        }
                    }
                    if (!anyNode) {
                        return List.of();
                    }
                    if (usable && !layer.isEmpty()) {
                        return new ArrayList<>(layer);
                    }
                }
                return List.of();
            }

            /** The nodes exactly k edges before (after) v, through nodes still on a complete path. */
            private Set<Integer> reachable(int id, int v, int k, boolean preceding) {
                Nettree nettree = nettrees.nettree(id);
                Set<Integer> frontier = new TreeSet<>(List.of(v));
                for (int step = 0; step < k; step++) {
                    Set<Integer> next = new TreeSet<>();
                    ArrayDeque<Integer> queue = new ArrayDeque<>(frontier);
                    while (!queue.isEmpty()) {
                        int x = queue.poll();
                        for (int y : preceding ? nettree.in(x) : nettree.out(x)) {
                            if (alive(id, y)) {
                                next.add(y);
                            }
                        }
                    }
                    frontier = next;
                }
                return frontier;
            }

            /** A node is in the matching graph while it lies on a complete path (Update-paths). */
            private boolean alive(int id, int v) {
                if (nettrees.flow(id).isRemoved(v)) {
                    return false;
                }
                Integer seen = countsVersion.get(id);
                if (seen == null || seen != version) {
                    countsCache.put(id, PathCounts.of(nettrees.nettree(id), nettrees.flow(id).removedFlags()));
                    countsVersion.put(id, version);
                }
                return countsCache.get(id).through(v) > 0;
            }

            /** Step 39 and the bookkeeping after it: the marks become deletions of this sequence. */
            void commit() {
                for (int k = 0; k < sensitiveId.length; k++) {
                    long now = unit(nettrees.supNO(sensitiveId[k]));
                    sensitiveSupport[sensitivePattern[k]] -= sensitiveUnitAtStart[k] - now;
                }
                if (marked.isEmpty()) {
                    return;
                }
                EventSequence after = original.mask(marked);
                Set<Integer> maskedSymbols = new java.util.HashSet<>();
                for (int position : marked) {
                    maskedSymbols.add(original.at(position));
                }
                for (int q : nonSensitiveHere) {
                    Pattern pattern = nonSensitive.get(q);
                    boolean touched = false;
                    for (int item : pattern.items()) {
                        touched |= maskedSymbols.contains(item);
                    }
                    if (!touched) {
                        continue;
                    }
                    Integer id = nonSensitiveId.get(q);
                    int nowNO = id != null ? nettrees.supNO(id) : SupportNO.of(pattern, after, gap);
                    nonSensitiveSupport[q] -= nonSensitiveUnitAtStart.get(q) - unit(nowNO);
                }
            }
        }
    }

    /**
     * Replace on one sequence and one sensitive pattern under a given annotation, without the rest
     * of the procedure. It exists so that the paper's Example 3 can be checked on its own.
     */
    static List<Integer> replaceOn(int radius, EventSequence sequence, GapConstraint gap,
                                   Pattern pattern, int position, int[] colour) {
        Sbsh sbsh = new Sbsh(radius, SupportCount.SEQUENCE_COUNT).withNonSensitiveFrequent(Map.of());
        HideProblem problem = new HideProblem(new SequenceDatabase(List.of(sequence)),
                List.of(pattern), gap, HideProblem.Mode.HIDE_DB, 1, 0.5, pattern.length());
        Run run = sbsh.new Run(problem);
        run.nonSensitiveHereOf.put(0, List.of());
        Run.Sequence t = run.new Sequence(0, List.of(0));
        return t.replace(position, Set.of(0), colour, radius);
    }

    private static boolean contains(Pattern pattern, int item) {
        for (int x : pattern.items()) {
            if (x == item) {
                return true;
            }
        }
        return false;
    }
}
