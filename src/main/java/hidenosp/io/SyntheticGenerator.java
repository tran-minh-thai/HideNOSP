package hidenosp.io;

import hidenosp.core.EventSequence;
import hidenosp.core.GapConstraint;
import hidenosp.core.Pattern;
import hidenosp.core.SequenceDatabase;
import hidenosp.core.SupportNO;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Controlled synthetic data for the regime checks of section 11.2 and for the scaling of SC6.
 *
 * <p>Two regimes matter and both are measured, never assumed. A database is useful for the
 * contribution when the repetition ratio rho_rep is high, that is when the pattern repeats inside
 * single sequences so sup_NO says more than the sequence support; and when beta is high, that is
 * when the gap constraint actually removes occurrences. A database with either ratio near zero is
 * kept as a declared degenerate control (rule 2.2.4).
 *
 * <p>{@link #generate} plants a target number of occurrences of one pattern per sequence and fills
 * the rest with noise; {@link #measure} then reports what was actually achieved, because planting k
 * occurrences does not guarantee sup_NO = k once the gap constraint and the noise are taken into
 * account. Only the measured value is reported, following rule 2.2.3.
 *
 * <p>The seed is written into row 3 of the protocol table, and every file records it in its run
 * stamp.
 */
public final class SyntheticGenerator {

    private final long seed;
    private final int alphabetSize;

    public SyntheticGenerator(long seed, int alphabetSize) {
        this.seed = seed;
        this.alphabetSize = alphabetSize;
    }

    /**
     * @param sequenceCount N
     * @param length length of every sequence
     * @param pattern the pattern to plant
     * @param plantedPerSequence how many occurrences to attempt per sequence
     * @param spacing gap left between the planted items, in positions
     */
    public SequenceDatabase generate(int sequenceCount, int length, Pattern pattern,
                                     int plantedPerSequence, int spacing) {
        Random random = new Random(seed);
        List<EventSequence> sequences = new ArrayList<>(sequenceCount);
        for (int s = 0; s < sequenceCount; s++) {
            int[] events = new int[length];
            for (int i = 0; i < length; i++) {
                events[i] = random.nextInt(alphabetSize);
            }
            int cursor = 0;
            for (int copy = 0; copy < plantedPerSequence; copy++) {
                for (int j = 0; j < pattern.length() && cursor < length; j++) {
                    events[cursor] = pattern.at(j);
                    cursor += 1 + spacing;
                }
            }
            sequences.add(new EventSequence(s, events));
        }
        return new SequenceDatabase(sequences);
    }

    /** The two regime ratios actually achieved, to be printed before the run (section 8.4). */
    public static double[] measure(SequenceDatabase database, Pattern pattern, GapConstraint gap) {
        long supNO = SupportNO.of(pattern, database, gap);
        int supSeq = SupportNO.sequenceSupport(pattern, database, gap);
        long withoutGap = SupportNO.withoutGap(pattern, database);
        double repetition = supNO == 0 ? 0 : 1.0 - (double) supSeq / supNO;
        double effect = withoutGap == 0 ? 0 : 1.0 - (double) supNO / withoutGap;
        return new double[] {repetition, effect};
    }

    public long seed() {
        return seed;
    }
}
