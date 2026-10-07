package hidenosp;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;
import hidenosp.hide.CutHide;
import hidenosp.hide.CutHideConfig;
import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The several-pattern greedy of CUT-HIDE under HIDE-DB, with repeated-item patterns among the
 * sensitive ones. Masking one position can lower such a pattern by more than one, and the greedy
 * once counted one, so its ledger stayed above the true support and it stopped with "no position
 * lowers a sensitive support any further" (the official SC3 of |S| = 5 on repeated-item cells).
 * On random small databases this checks, from the definition of sup_NO on the sanitized database,
 * that the run ends and every pattern is below the threshold, and that the instances do contain
 * repeated-item patterns, without which the check would be vacuous.
 */
class CutHideSeveralPatternsTest {

    @Test
    @DisplayName("several patterns with repeated items: the greedy ends and hides every pattern")
    void severalPatternsHidden() {
        Random random = new Random(20261007L);
        int[] gaps = {1, 2, 3};
        int instances = 0;
        int withRepeated = 0;
        for (int trial = 0; trial < 3000; trial++) {
            int alphabet = 2 + random.nextInt(2);
            int sequences = 2 + random.nextInt(4);
            List<EventSequence> list = new ArrayList<>();
            for (int s = 0; s < sequences; s++) {
                int n = 3 + random.nextInt(8);
                int[] events = new int[n];
                for (int i = 0; i < n; i++) {
                    events[i] = random.nextInt(alphabet);
                }
                list.add(new EventSequence(s, events));
            }
            SequenceDatabase db = new SequenceDatabase(list);
            GapConstraint gap = GapConstraint.of(0, gaps[random.nextInt(gaps.length)]);
            int patternCount = 2 + random.nextInt(2);
            List<Pattern> sensitive = new ArrayList<>();
            long smallest = Long.MAX_VALUE;
            boolean repeated = false;
            for (int k = 0; k < patternCount; k++) {
                int m = 2 + random.nextInt(2);
                int[] items = new int[m];
                for (int j = 0; j < m; j++) {
                    items[j] = random.nextInt(alphabet);
                }
                Pattern p = new Pattern(items);
                if (sensitive.contains(p)) {
                    continue;
                }
                sensitive.add(p);
                repeated |= !p.hasDistinctItems();
                smallest = Math.min(smallest, SupportNO.of(p, db, gap));
            }
            if (sensitive.size() < 2 || smallest == 0) {
                continue;
            }
            int threshold = 1 + random.nextInt((int) smallest);
            HideProblem problem = new HideProblem(db, sensitive, gap, HideProblem.Mode.HIDE_DB,
                    threshold, 0.5, 4);
            Sanitization result = new CutHide(CutHideConfig.uniformWeights()).run(problem);
            instances++;
            if (repeated) {
                withRepeated++;
            }
            SequenceDatabase after = result.sanitized();
            for (Pattern p : sensitive) {
                long support = SupportNO.of(p, after, gap);
                assertTrue(support < threshold, "pattern " + p + " still has sup_NO " + support
                        + " >= " + threshold + " in " + list + " gap " + gap + ", sensitive "
                        + sensitive);
            }
        }
        System.out.printf("Several patterns: %d instances, %d with a repeated-item pattern%n",
                instances, withRepeated);
        assertTrue(withRepeated >= 500, "only " + withRepeated + " instances with a repeated-item"
                + " pattern; the check would not reach the case it is for");
    }
}
