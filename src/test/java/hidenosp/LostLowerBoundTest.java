// SPDX-License-Identifier: MIT
package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Nettree;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.hide.LostLowerBound;
import hidenosp.hide.Sanitization;
import hidenosp.metrics.FrequentPatterns;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The certified bound of P12 (preregistration section 20.2) against its definition: every pattern the bound
 * declares lost in every minimum hiding set must be lost in every minimum hiding set, which is checked by
 * enumerating all of them on small random instances.
 */
class LostLowerBoundTest {

    @Test
    @DisplayName("Every pattern certified as always lost is lost in every minimum hiding set")
    void soundAgainstEnumeration() {
        Random random = new Random(20261106);
        int instances = 0;
        int certified = 0;
        int alwaysLost = 0;
        int minimumSets = 0;
        for (int trial = 0; instances < 300 && trial < 20000; trial++) {
            // Half of the instances carry a planted gadget x y y z: the two occurrences of P = (x, y) share
            // the x, so every minimum hiding set masks x there, and (x, z) can be lost in all of them. Pure
            // random instances almost never hold a pattern lost in every minimum set, which would leave the
            // soundness check with nothing to check.
            boolean planted = trial % 2 == 0;
            int alphabet = planted ? 6 : 2 + random.nextInt(2);
            List<EventSequence> sequences = new ArrayList<>();
            int n = 5 + random.nextInt(4);
            for (int s = 0; s < n; s++) {
                List<Integer> events = new ArrayList<>();
                int filler = (planted ? 2 : 5) + random.nextInt(planted ? 4 : 8);
                for (int i = 0; i < filler; i++) {
                    events.add(planted ? 3 + random.nextInt(alphabet - 3) : random.nextInt(alphabet));
                }
                if (planted && random.nextInt(10) < 8) {
                    events.addAll(random.nextInt(events.size() + 1), List.of(0, 1, 1, 2));
                }
                sequences.add(new EventSequence(s, events.stream().mapToInt(Integer::intValue).toArray()));
            }
            SequenceDatabase db = new SequenceDatabase(sequences);
            GapConstraint gap = planted ? GapConstraint.of(0, 2) : random.nextBoolean()
                    ? GapConstraint.of(0, random.nextInt(3)) : GapConstraint.unbounded(0);
            int[] items = new int[2 + random.nextInt(2)];
            for (int i = 0; i < items.length; i++) {
                items[i] = random.nextInt(alphabet);
            }
            Pattern p = planted ? new Pattern(0, 1) : new Pattern(items);
            long support = SupportNO.of(p, db, gap);
            if (support < 3) {
                continue;
            }
            int sigma = (int) (support - 1 - random.nextInt((int) Math.min(3, support - 2)));
            if (sigma < 2) {
                continue;
            }
            Map<Pattern, Long> frequent = FrequentPatterns.mine(db, gap, 3, sigma);
            Set<Pattern> closure = FrequentPatterns.closure(List.of(p), frequent.keySet(), gap, FrequentPatterns.Closure.NECESSARY);
            Map<Pattern, Long> watched = new LinkedHashMap<>();
            frequent.forEach((q, v) -> {
                if (!closure.contains(q)) {
                    watched.put(q, v);
                }
            });
            if (watched.isEmpty()) {
                continue;
            }
            HideProblem problem = new HideProblem(db, List.of(p), gap, HideProblem.Mode.HIDE_DB, sigma, 0.5, 3);
            Sanitization reference = new CutHide(CutHideConfig.uniformWeights()).run(problem);
            List<int[]> candidates = new ArrayList<>();
            for (int s = 0; s < db.size(); s++) {
                Nettree nettree = Nettree.build(p, db.get(s), gap);
                TreeSet<Integer> positions = new TreeSet<>();
                for (int v = 0; v < nettree.nodeCount(); v++) {
                    positions.add(nettree.position(v));
                }
                for (int position : positions) {
                    candidates.add(new int[] {s, position});
                }
            }
            int k = reference.maskCount();
            if (candidates.size() > 22 || k > 6) {
                continue;
            }
            List<Map<Integer, List<Integer>>> minimum = new ArrayList<>();
            enumerate(candidates, 0, k, new ArrayList<>(), problem, minimum);
            if (minimum.isEmpty()) {
                continue;
            }
            List<Map<Integer, List<Integer>>> smaller = new ArrayList<>();
            enumerate(candidates, 0, k - 1, new ArrayList<>(), problem, smaller);
            assertTrue(smaller.isEmpty(), "the reference is not a minimum hiding set, trial " + trial);
            Set<Pattern> always = new java.util.HashSet<>(watched.keySet());
            for (Map<Integer, List<Integer>> masks : minimum) {
                SequenceDatabase after = db.mask(masks);
                always.removeIf(q -> SupportNO.of(q, after, gap) >= sigma);
            }
            LostLowerBound.Result bound = LostLowerBound.of(problem, watched, sigma, reference);
            for (Pattern q : bound.doomed()) {
                assertTrue(always.contains(q), "certified " + q + " survives some minimum hiding set; P = " + p
                        + ", sigma " + sigma + ", gap " + gap + ", trial " + trial);
            }
            instances++;
            certified += bound.bound();
            alwaysLost += always.size();
            minimumSets += minimum.size();
        }
        System.out.printf("P12 bound: %d instances, %d minimum hiding sets enumerated, %d patterns lost in every "
                + "minimum set, %d of them certified by the bound%n", instances, minimumSets, alwaysLost, certified);
        assertTrue(instances >= 250, "too few instances: " + instances);
        assertTrue(certified >= 30, "the bound certified too few patterns for the check to mean much: " + certified);
    }

    private static void enumerate(List<int[]> candidates, int from, int left, List<int[]> chosen, HideProblem problem,
                                  List<Map<Integer, List<Integer>>> out) {
        if (left == 0) {
            Map<Integer, List<Integer>> masks = new HashMap<>();
            for (int[] c : chosen) {
                masks.computeIfAbsent(c[0], x -> new ArrayList<>()).add(c[1]);
            }
            if (SupportNO.of(problem.sensitive().get(0), problem.database().mask(masks), problem.gap())
                    < problem.threshold()) {
                out.add(masks);
            }
            return;
        }
        for (int i = from; i <= candidates.size() - left; i++) {
            chosen.add(candidates.get(i));
            enumerate(candidates, i + 1, left - 1, chosen, problem, out);
            chosen.remove(chosen.size() - 1);
        }
    }
}
