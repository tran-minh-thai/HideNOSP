package hidenosp.baseline;

import hidenosp.core.EventSequence;
import hidenosp.core.Pattern;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Algorithm 1 (Cost) of Gkoulalas-Divanis and Loukides, KDD 2011, and its Candidates variants.
 *
 * <p>Every distinct symbol of a supported sensitive pattern covers the patterns that contain it and
 * weighs its number of occurrences in the sequence. The symbol with the least weight per covered
 * pattern is taken, until every supported pattern is covered; the cost is the total weight. Ties
 * go to the symbol that occurs first in the sequence, which reproduces the choice stated in the
 * paper's Example 1.
 *
 * <p>Candidates returns, for every chosen symbol j, the subset S_j of patterns it covers and the
 * positions of the symbol, ordered by the size of S_j (descending) and then by the order of choice;
 * Candidates-SBSH is the same list flattened into one array.
 */
final class SymbolCover {

    /**
     * @param cost the value of Cost
     * @param subsets S_j, as indices into the list of supported patterns given to {@link #of}
     * @param positions the positions of the j-th chosen symbol, ascending
     */
    record Cover(long cost, List<Set<Integer>> subsets, List<List<Integer>> positions) {

        /** Candidates-SBSH: the positions of every chosen symbol, in the order of the subsets. */
        List<Integer> flatPositions() {
            List<Integer> out = new ArrayList<>();
            for (List<Integer> p : positions) {
                out.addAll(p);
            }
            return out;
        }
    }

    private SymbolCover() {
    }

    /**
     * @param supported the supported sensitive patterns, as indices into {@code sensitive}
     */
    static Cover of(EventSequence sequence, List<Integer> supported, List<Pattern> sensitive) {
        Map<Integer, Set<Integer>> covers = new LinkedHashMap<>();
        Map<Integer, Integer> firstOccurrence = new HashMap<>();
        Map<Integer, Integer> weight = new HashMap<>();
        for (int i = 0; i < sequence.length(); i++) {
            int item = sequence.at(i);
            if (item == EventSequence.MASK) {
                continue;
            }
            weight.merge(item, 1, Integer::sum);
            firstOccurrence.putIfAbsent(item, i);
        }
        for (int k = 0; k < supported.size(); k++) {
            for (int item : sensitive.get(supported.get(k)).items()) {
                covers.computeIfAbsent(item, x -> new TreeSet<>()).add(k);
            }
        }
        Set<Integer> covered = new TreeSet<>();
        List<Integer> chosen = new ArrayList<>();
        long cost = 0;
        while (covered.size() < supported.size()) {
            int best = -1;
            double bestRatio = Double.POSITIVE_INFINITY;
            for (Map.Entry<Integer, Set<Integer>> e : covers.entrySet()) {
                int item = e.getKey();
                if (chosen.contains(item) || covered.containsAll(e.getValue())
                        || !weight.containsKey(item)) {
                    continue;
                }
                double ratio = (double) weight.get(item) / e.getValue().size();
                if (ratio < bestRatio || (ratio == bestRatio && best >= 0
                        && firstOccurrence.get(item) < firstOccurrence.get(best))) {
                    bestRatio = ratio;
                    best = item;
                }
            }
            if (best < 0) {
                throw new IllegalStateException("a supported sensitive pattern has a symbol that "
                        + "does not occur in the sequence");
            }
            chosen.add(best);
            cost += weight.get(best);
            covered.addAll(covers.get(best));
        }
        List<Integer> bySubsetSize = new ArrayList<>(chosen);
        bySubsetSize.sort(Comparator.comparingInt((Integer item) -> -covers.get(item).size()));
        List<Set<Integer>> subsets = new ArrayList<>();
        List<List<Integer>> positions = new ArrayList<>();
        for (int item : bySubsetSize) {
            subsets.add(covers.get(item));
            List<Integer> here = new ArrayList<>();
            for (int i = 0; i < sequence.length(); i++) {
                if (sequence.at(i) == item) {
                    here.add(i);
                }
            }
            positions.add(here);
        }
        return new Cover(cost, subsets, positions);
    }
}
