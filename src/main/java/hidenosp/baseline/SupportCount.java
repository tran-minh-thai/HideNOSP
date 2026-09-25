package hidenosp.baseline;

/**
 * Which support the sequence-hiding algorithms of Gkoulalas-Divanis and Loukides (KDD 2011) count
 * wherever they read a support at the level of the database.
 */
public enum SupportCount {

    /** Non-overlapping occurrences summed over the sequences: the transfer to sup_NO. */
    NON_OVERLAPPING,

    /** Number of sequences containing the pattern: the problem of the paper itself. */
    SEQUENCE_COUNT;

    /** Units a sequence contributes, given sup_NO of the pattern in that sequence. */
    public long unit(int supNO) {
        return this == SEQUENCE_COUNT ? (supNO > 0 ? 1 : 0) : supNO;
    }
}
