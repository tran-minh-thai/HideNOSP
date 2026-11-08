// SPDX-License-Identifier: MIT
package hidenosp.core.flow;

import hidenosp.core.Nettree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The flow state of one Nettree: the node-split network, a max flow on it, and the derived
 * quantities that the Nettree cut index stores.
 *
 * <p>Every Nettree node v is split into in(v) = 2v and out(v) = 2v+1 joined by an arc of capacity
 * one; the source feeds every level-1 node and every level-m node feeds the sink, both with
 * infinite capacity, as do the Nettree arcs. By lemma L0 the value of a max flow is sup_NO(P, S),
 * a minimum cut is a smallest set of nodes whose removal kills every occurrence, and by L5 a node
 * is vital exactly when its node arc is saturated and its two endpoints fall in different strongly
 * connected components of the residual graph.
 *
 * <p>{@link #maskPosition(int)} is the incremental update: it cancels the flow path through each
 * node carried by that position and then re-augments, instead of rebuilding the network. When the
 * masked node was vital the flow drops by exactly one and no augmenting path exists, which is the
 * cost model of the Nettree cut index, measured in SC6.
 */
public final class NettreeFlow {

    private final Nettree nettree;
    private final Dinic graph;
    private final int source;
    private final int sink;
    private final int[] nodeArc;
    private final boolean[] removed;
    private long flowValue;

    public NettreeFlow(Nettree nettree) {
        this(nettree, null);
    }

    private NettreeFlow(Nettree nettree, long[] capacities) {
        this.nettree = nettree;
        int n = nettree.nodeCount();
        this.source = 2 * n;
        this.sink = 2 * n + 1;
        this.nodeArc = new int[n];
        this.removed = new boolean[n];
        this.graph = new Dinic(2 * n + 2, n + nettree.arcCount() + 2 * n);
        for (int v = 0; v < n; v++) {
            long c = capacities == null ? 1 : capacities[v];
            nodeArc[v] = graph.addArc(2 * v, 2 * v + 1, c);
            removed[v] = c == 0;
            if (nettree.isSource(v)) {
                graph.addArc(source, 2 * v, Dinic.INF);
            }
            if (nettree.isSink(v)) {
                graph.addArc(2 * v + 1, sink, Dinic.INF);
            }
        }
        for (int v = 0; v < n; v++) {
            for (int u : nettree.out(v)) {
                graph.addArc(2 * v + 1, 2 * u, Dinic.INF);
            }
        }
        this.flowValue = graph.maxFlow(source, sink);
    }

    public Nettree nettree() {
        return nettree;
    }

    /** sup_NO(P, S) for the current state of the mask (lemma L0). */
    public int supNO() {
        return (int) flowValue;
    }

    public boolean isRemoved(int v) {
        return removed[v];
    }

    /**
     * One minimum cut, as a flag per Nettree node: the source side of the residual graph. By P1 its
     * size is sup_NO, and by P2 every optimal masking set of size F - t sits inside such a cut.
     */
    public boolean[] minCut() {
        boolean[] reach = graph.residualReachable(source);
        boolean[] cut = new boolean[nettree.nodeCount()];
        for (int v = 0; v < cut.length; v++) {
            cut[v] = !removed[v] && reach[2 * v] && !reach[2 * v + 1];
        }
        return cut;
    }

    /**
     * Every vital node, by lemma L5: one max flow plus one strongly connected component pass. A
     * node is vital when deleting it lowers sup_NO by one.
     */
    public boolean[] vitalNodes() {
        int[] comp = ResidualScc.compute(graph);
        boolean[] vital = new boolean[nettree.nodeCount()];
        for (int v = 0; v < vital.length; v++) {
            vital[v] = !removed[v]
                    && graph.residual(nodeArc[v]) == 0
                    && comp[2 * v] != comp[2 * v + 1];
        }
        return vital;
    }

    /**
     * Positions carrying at least one vital node, in increasing order. For a pattern with distinct
     * items this is exactly the set of positions whose masking lowers sup_NO, so masking any F - t
     * of them in turn reaches the target t (corollary of L5). For a repeated-item pattern a
     * position may carry several nodes and that corollary no longer applies, so callers must branch
     * on mu(P).
     */
    public int[] vitalPositions() {
        boolean[] vital = vitalNodes();
        // Nodes are numbered by level first, so the same position can appear twice for a
        // repeated-item pattern and the node order is not the position order; sort and de-duplicate.
        java.util.TreeSet<Integer> out = new java.util.TreeSet<>();
        for (int v = 0; v < vital.length; v++) {
            if (vital[v]) {
                out.add(nettree.position(v));
            }
        }
        int[] a = new int[out.size()];
        int k = 0;
        for (int position : out) {
            a[k++] = position;
        }
        return a;
    }

    /**
     * Masks position i: removes every node that position carries (remark O1) and repairs the flow
     * incrementally. Returns the drop in sup_NO, which is 0 or 1 when mu(P) = 1 and may be larger
     * when one position carries several nodes.
     */
    public int maskPosition(int position) {
        long before = flowValue;
        for (int v : nettree.nodesAtPosition(position)) {
            if (removed[v]) {
                continue;
            }
            if (graph.flow(nodeArc[v]) > 0) {
                cancelFlowThrough(v);
                flowValue--;
            }
            graph.setCapacity(nodeArc[v], 0);
            removed[v] = true;
        }
        flowValue += graph.maxFlow(source, sink);
        return (int) (before - flowValue);
    }

    /** Cancels the single unit of flow routed through node v, arc by arc, back to s and on to t. */
    private void cancelFlowThrough(int v) {
        int x = 2 * v;
        while (x != source) {
            int arc = incomingFlowArc(x);
            if (arc < 0) {
                throw new IllegalStateException("flow path broken before in(" + v + ")");
            }
            graph.push(arc ^ 1, 1);
            x = graph.target(arc ^ 1);
        }
        graph.push(nodeArc[v] ^ 1, 1);
        x = 2 * v + 1;
        while (x != sink) {
            int arc = outgoingFlowArc(x);
            if (arc < 0) {
                throw new IllegalStateException("flow path broken after out(" + v + ")");
            }
            graph.push(arc ^ 1, 1);
            x = graph.target(arc);
        }
    }

    /** A forward arc that carries flow into x, or -1. */
    private int incomingFlowArc(int x) {
        for (int e = graph.headArc(x); e != -1; e = graph.nextArc(e)) {
            int rev = e ^ 1;
            if (graph.flow(rev) > 0) {
                return rev;
            }
        }
        return -1;
    }

    /** A forward arc that carries flow out of x, or -1. */
    private int outgoingFlowArc(int x) {
        for (int e = graph.headArc(x); e != -1; e = graph.nextArc(e)) {
            if (graph.flow(e) > 0) {
                return e;
            }
        }
        return -1;
    }

    /** A minimum cut, its cardinality and its weight, as returned by the weighted cut of P6(b). */
    public record WeightedCut(int[] nodes, int cardinality, long weight) {
    }

    /**
     * P6(b): among all minimum-cardinality cuts, one of least total weight, exactly. Node
     * capacities are M + w(v) with M larger than the total weight, so the value of a cut C is
     * M|C| + w(C) and the least value picks the smallest cardinality first and the smallest weight
     * second.
     *
     * <p>The lambda sweep of P6(d) calls this with the clipped weights min(w, lambda).
     */
    public static WeightedCut minWeightMinCut(Nettree nettree, long[] weight, boolean[] removed) {
        int n = nettree.nodeCount();
        long total = 0;
        for (int v = 0; v < n; v++) {
            if (weight[v] < 0) {
                throw new IllegalArgumentException("negative weight at node " + v);
            }
            total += weight[v];
        }
        long big = 4 * total + 1;
        long[] caps = new long[n];
        for (int v = 0; v < n; v++) {
            caps[v] = removed != null && removed[v] ? 0 : big + weight[v];
        }
        NettreeFlow flow = new NettreeFlow(nettree, caps);
        boolean[] reach = flow.graph.residualReachable(flow.source);
        List<Integer> cut = new ArrayList<>();
        long cutWeight = 0;
        for (int v = 0; v < n; v++) {
            if (!flow.removed[v] && reach[2 * v] && !reach[2 * v + 1]) {
                cut.add(v);
                cutWeight += weight[v];
            }
        }
        int[] nodes = new int[cut.size()];
        for (int k = 0; k < nodes.length; k++) {
            nodes[k] = cut.get(k);
        }
        long cardinality = flow.flowValue / big;
        if (cardinality != nodes.length) {
            throw new IllegalStateException("weighted cut inconsistent: value " + flow.flowValue
                    + ", M " + big + ", cut size " + nodes.length);
        }
        return new WeightedCut(nodes, nodes.length, cutWeight);
    }

    /** Copy of the removal flags, for callers that re-run a weighted cut on the same state. */
    public boolean[] removedFlags() {
        return Arrays.copyOf(removed, removed.length);
    }

    @Override
    public String toString() {
        return "NettreeFlow[supNO=" + flowValue + ", removed=" + countRemoved()
                + "/" + nettree.nodeCount() + "]";
    }

    private int countRemoved() {
        int c = 0;
        for (boolean b : removed) {
            if (b) {
                c++;
            }
        }
        return c;
    }
}
