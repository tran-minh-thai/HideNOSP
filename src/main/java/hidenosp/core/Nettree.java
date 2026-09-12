package hidenosp.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * NT(P, S) of design doc section 2: a node (j, i) for every pair with e_i = p_j, and an arc
 * (j, i) to (j+1, i2) whenever g- &le; i2 - i - 1 &le; g+. Every occurrence of P in S is a path
 * from level 1 to level m and conversely (lemma L0).
 *
 * <p>Nodes are numbered 0..nodeCount-1 in (level, position) order. {@link #nodesAtPosition(int)}
 * is the shared position table of section 9.1: masking position i deletes every node in that list,
 * which is what makes masking a node-deletion operation on the merged index (remark O1).
 *
 * <p>Sizes: at most mu(P) * n nodes and (node count) * w arcs, with w = min(n, g+ - g- + 1).
 */
public final class Nettree {

    private final Pattern pattern;
    private final EventSequence sequence;
    private final GapConstraint gap;

    private final int[] level;
    private final int[] position;
    /** Node ids grouped by position, in increasing level order. */
    private final int[][] byPosition;
    /** Out-arcs: {@code out[v]} lists the node ids at level(v)+1 reachable from v. */
    private final int[][] out;
    /** In-arcs, the reverse of {@code out}; used by backward path counts and by flow repair. */
    private final int[][] in;

    private Nettree(Pattern pattern, EventSequence sequence, GapConstraint gap,
                    int[] level, int[] position, int[][] byPosition, int[][] out, int[][] in) {
        this.pattern = pattern;
        this.sequence = sequence;
        this.gap = gap;
        this.level = level;
        this.position = position;
        this.byPosition = byPosition;
        this.out = out;
        this.in = in;
    }

    /** Builds NT(P, S) under the gap constraint. Positions holding the mask carry no node. */
    public static Nettree build(Pattern pattern, EventSequence sequence, GapConstraint gap) {
        int m = pattern.length();
        int n = sequence.length();
        List<int[]> nodes = new ArrayList<>();
        int[][] idAt = new int[m][n];
        for (int[] row : idAt) {
            Arrays.fill(row, -1);
        }
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                if (sequence.at(i) != EventSequence.MASK && sequence.at(i) == pattern.at(j)) {
                    idAt[j][i] = nodes.size();
                    nodes.add(new int[] {j, i});
                }
            }
        }
        int count = nodes.size();
        int[] level = new int[count];
        int[] position = new int[count];
        for (int v = 0; v < count; v++) {
            level[v] = nodes.get(v)[0];
            position[v] = nodes.get(v)[1];
        }

        List<List<Integer>> outLists = new ArrayList<>(count);
        List<List<Integer>> inLists = new ArrayList<>(count);
        for (int v = 0; v < count; v++) {
            outLists.add(new ArrayList<>());
            inLists.add(new ArrayList<>());
        }
        for (int v = 0; v < count; v++) {
            int j = level[v];
            if (j + 1 >= m) {
                continue;
            }
            int lo = gap.firstNext(position[v]);
            int hi = gap.lastNext(position[v], n);
            for (int i2 = lo; i2 <= hi; i2++) {
                int u = idAt[j + 1][i2];
                if (u >= 0) {
                    outLists.get(v).add(u);
                    inLists.get(u).add(v);
                }
            }
        }

        List<List<Integer>> perPos = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            perPos.add(new ArrayList<>());
        }
        for (int v = 0; v < count; v++) {
            perPos.get(position[v]).add(v);
        }
        return new Nettree(pattern, sequence, gap, level, position,
                toArray(perPos), toArray(outLists), toArray(inLists));
    }

    private static int[][] toArray(List<List<Integer>> lists) {
        int[][] out = new int[lists.size()][];
        for (int i = 0; i < lists.size(); i++) {
            List<Integer> l = lists.get(i);
            int[] a = new int[l.size()];
            for (int k = 0; k < a.length; k++) {
                a[k] = l.get(k);
            }
            out[i] = a;
        }
        return out;
    }

    public Pattern pattern() {
        return pattern;
    }

    public EventSequence sequence() {
        return sequence;
    }

    public GapConstraint gap() {
        return gap;
    }

    public int nodeCount() {
        return level.length;
    }

    public int arcCount() {
        int c = 0;
        for (int[] a : out) {
            c += a.length;
        }
        return c;
    }

    /** 0-based level j of node v. */
    public int level(int v) {
        return level[v];
    }

    /** 0-based position i of node v. */
    public int position(int v) {
        return position[v];
    }

    public int[] out(int v) {
        return out[v];
    }

    public int[] in(int v) {
        return in[v];
    }

    /** The shared position table: every node carried by position i. */
    public int[] nodesAtPosition(int i) {
        return byPosition[i];
    }

    public boolean isSource(int v) {
        return level[v] == 0;
    }

    public boolean isSink(int v) {
        return level[v] == pattern.length() - 1;
    }
}
