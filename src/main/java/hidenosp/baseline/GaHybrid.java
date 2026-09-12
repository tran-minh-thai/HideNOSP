package hidenosp.baseline;

import hidenosp.hide.HideProblem;
import hidenosp.hide.Sanitization;

/**
 * Tier (c) of section 11.3: a metaheuristic in the style of [R12], and the hybrid that seeds it
 * from the cuts.
 *
 * <p>Two variants are needed and they answer different questions. The plain genetic algorithm,
 * initialised at random and scoring a candidate by HF, Lost and |R|, says what a method from
 * another family achieves. The hybrid, initialised from minimum cuts and evaluating candidates on
 * the Nettree cut index, says how much of the result comes from the initialisation and from the
 * index rather than from the search; that difference is the reason the hybrid appears in the
 * contribution list at all.
 *
 * <p>TODO(code): implement both, with population size, generations, crossover and mutation rates as
 * declared parameters, and the seed in row 3 of the protocol table.
 */
public final class GaHybrid implements HidingMethod {

    private final boolean seededFromCuts;
    private final long seed;

    public GaHybrid(boolean seededFromCuts, long seed) {
        this.seededFromCuts = seededFromCuts;
        this.seed = seed;
    }

    @Override
    public String name() {
        return seededFromCuts ? "GA-hybrid" : "GA-plain";
    }

    @Override
    public Sanitization run(HideProblem problem) {
        throw new UnsupportedOperationException("the genetic algorithm is not implemented yet");
    }
}
