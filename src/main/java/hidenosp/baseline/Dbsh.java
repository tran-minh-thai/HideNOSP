package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import hidenosp.index.NettreeCutIndex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DBSH, the distance based sequence hiding algorithm of A. Gkoulalas-Divanis and G. Loukides,
 * "Revisiting sequential pattern hiding to enhance utility", KDD 2011 (Algorithm 4, with Algorithm 1
 * for Cost and Candidates and Algorithm 3 for Exclude).
 *
 * <ol>
 *   <li><b>Exclude</b> (step 1): the sequences that support a sensitive pattern are visited by Cost,
 *       descending; one is left intact while each pattern it supports has been encountered fewer
 *       than psi - 1 times, and the others are sanitised (see {@link Exclude}).</li>
 *   <li><b>Candidates</b> (step 3): the greedy cover of Algorithm 1 gives, for every chosen symbol j,
 *       the subset S_j of supported patterns that contain it and the positions of the symbol,
 *       ordered by the size of S_j, descending.</li>
 *   <li><b>Sanitising</b> (steps 4 to 13): for each j in turn, the positions of the symbol are
 *       removed one after another while the sequence still supports a pattern of S_j.</li>
 * </ol>
 *
 * <p><b>Where the paper leaves a choice open</b>, and what this class does:
 *
 * <ul>
 *   <li><em>Ties in Exclude</em>, "arbitrary" in the paper, go to the higher sequence index. With it
 *       the class reproduces Figure 1(e) exactly (see {@code DbshPaperExampleTest}).</li>
 *   <li><em>Positions that are not on a complete path.</em> Candidates works on the matching graph,
 *       which keeps only nodes on complete paths (Definition 4.1), and Update-paths removes the
 *       nodes that fall off them. A position of the chosen symbol that carries no such node is
 *       therefore skipped. Removing it would lower no sensitive support and could only add side
 *       effects, so this is also the reading most favourable to DBSH.</li>
 *   <li><em>Order inside P[j]</em>: increasing position.</li>
 * </ul>
 *
 * <p><b>Transfer.</b> The paper deletes and this class masks, as every method here does. With
 * {@link SupportCount#NON_OVERLAPPING} the support read by Exclude is sup_NO, so a sequence brings
 * sup_NO(s, T) units to the budget sigma - 1. The loop of Algorithm 4 asks whether the sequence
 * supports S_j, which is containment under either support, so a sequence chosen for sanitising is
 * sanitised completely, as in the paper, even when fewer masks would bring the database below
 * sigma: that is how DBSH behaves, and it is kept. Under a per-sequence limit r (HIDE-SEQ),
 * "supports" reads sup_NO(s, T) &ge; r and every sequence at or above r is sanitised.
 */
public final class Dbsh implements HidingMethod {

    private final SupportCount support;

    /** The transferred version on sup_NO, the one the main comparison runs. */
    public Dbsh() {
        this(SupportCount.NON_OVERLAPPING);
    }

    public Dbsh(SupportCount support) {
        this.support = support;
    }

    @Override
    public String name() {
        return support == SupportCount.SEQUENCE_COUNT ? "DBSH-original" : "DBSH";
    }

    @Override
    public Sanitization run(HideProblem problem) {
        if (support == SupportCount.SEQUENCE_COUNT && !problem.isDatabaseMode()) {
            throw new IllegalArgumentException("the sequence-count support of the paper is defined "
                    + "for a database threshold only");
        }
        SequenceDatabase database = problem.database();
        List<Pattern> sensitive = problem.sensitive();
        GapConstraint gap = problem.gap();
        int n = database.size();
        int limit = problem.isDatabaseMode() ? 1 : problem.threshold();

        long[][] units = new long[sensitive.size()][n];
        Map<Integer, List<Integer>> supported = new HashMap<>();
        List<Integer> carrying = new ArrayList<>();
        for (int s = 0; s < n; s++) {
            List<Integer> here = new ArrayList<>();
            for (int p = 0; p < sensitive.size(); p++) {
                int supNO = SupportNO.of(sensitive.get(p), database.get(s), gap);
                units[p][s] = support.unit(supNO);
                if (supNO >= limit) {
                    here.add(p);
                }
            }
            if (!here.isEmpty()) {
                supported.put(s, here);
                carrying.add(s);
            }
        }

        Map<Integer, Long> cost = new HashMap<>();
        for (int s : carrying) {
            cost.put(s, SymbolCover.of(database.get(s), supported.get(s), sensitive).cost());
        }
        List<Integer> order = new ArrayList<>(carrying);
        order.sort(Comparator.<Integer>comparingLong(s -> -cost.get(s))
                .thenComparing(Comparator.<Integer>reverseOrder()));

        List<Integer> toSanitise = problem.isDatabaseMode()
                ? Exclude.toSanitise(order, supported, units, problem.threshold())
                : order;

        Sanitization result = new Sanitization(database).method(name());
        for (int s : toSanitise) {
            sanitise(problem, s, supported.get(s), limit, result);
        }
        Exclude.verify(problem, result, support, name());
        return result;
    }

    /** Steps 3 to 13 of Algorithm 4 for one sequence. */
    private void sanitise(HideProblem problem, int s, List<Integer> supported, int limit,
                          Sanitization result) {
        EventSequence sequence = problem.database().get(s);
        NettreeCutIndex index = new NettreeCutIndex(sequence, problem.gap());
        int[] id = new int[supported.size()];
        for (int k = 0; k < id.length; k++) {
            id[k] = index.add(problem.sensitive().get(supported.get(k)), true);
        }
        SymbolCover.Cover cover = SymbolCover.of(sequence, supported, problem.sensitive());
        for (int j = 0; j < cover.subsets().size(); j++) {
            Set<Integer> subset = cover.subsets().get(j);
            List<Integer> positions = cover.positions().get(j);
            for (int i = 0; i < positions.size() && supportsAny(index, id, subset, limit); i++) {
                int position = positions.get(i);
                if (!onCompletePath(index, id, subset, position)) {
                    continue;
                }
                index.maskPosition(position);
                result.mask(s, position);
            }
        }
    }

    private static boolean supportsAny(NettreeCutIndex index, int[] id, Set<Integer> subset, int limit) {
        for (int k : subset) {
            if (index.supNO(id[k]) >= limit) {
                return true;
            }
        }
        return false;
    }

    private static boolean onCompletePath(NettreeCutIndex index, int[] id, Set<Integer> subset,
                                          int position) {
        if (index.isMasked(position)) {
            return false;
        }
        for (int k : subset) {
            if (index.occurrencesThrough(id[k], position) > 0) {
                return true;
            }
        }
        return false;
    }
}
