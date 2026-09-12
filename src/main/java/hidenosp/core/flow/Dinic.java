package hidenosp.core.flow;

import java.util.Arrays;

/**
 * Dinic max flow on an explicit arc list, with the residual graph left open for inspection.
 *
 * <p>Arcs are stored in pairs: arc {@code e} and its reverse {@code e ^ 1}. The residual capacity
 * of {@code e} is {@code cap[e]}; the flow on a forward arc is {@code cap0[e] - cap[e]}. Two
 * clients need that detail: {@link NettreeFlow} cancels the flow path through a masked node instead
 * of recomputing a max flow, and the vital-node test of L5 walks the residual graph.
 *
 * <p>With unit node capacities the run time is O(|A| sqrt(|V|)) [R30]; the weighted cuts of P6 use
 * capacities M + w and need the general bound.
 */
public final class Dinic {

    public static final long INF = Long.MAX_VALUE / 4;

    private final int nodeCount;
    private int[] head;
    private int[] next;
    private int[] to;
    private long[] cap;
    private long[] cap0;
    private int arcCount;

    private int[] levelOf;
    private int[] iter;

    public Dinic(int nodeCount, int expectedArcs) {
        this.nodeCount = nodeCount;
        this.head = new int[nodeCount];
        Arrays.fill(head, -1);
        int cap2 = Math.max(4, 2 * expectedArcs);
        this.next = new int[cap2];
        this.to = new int[cap2];
        this.cap = new long[cap2];
        this.cap0 = new long[cap2];
        this.levelOf = new int[nodeCount];
        this.iter = new int[nodeCount];
    }

    /** Adds arc u to v with capacity c, plus its zero-capacity reverse. Returns the forward arc id. */
    public int addArc(int u, int v, long c) {
        ensure(arcCount + 2);
        int e = arcCount;
        to[e] = v;
        cap[e] = c;
        cap0[e] = c;
        next[e] = head[u];
        head[u] = e;
        to[e + 1] = u;
        cap[e + 1] = 0;
        cap0[e + 1] = 0;
        next[e + 1] = head[v];
        head[v] = e + 1;
        arcCount += 2;
        return e;
    }

    private void ensure(int needed) {
        if (needed <= to.length) {
            return;
        }
        int n = Math.max(needed, to.length * 2);
        next = Arrays.copyOf(next, n);
        to = Arrays.copyOf(to, n);
        cap = Arrays.copyOf(cap, n);
        cap0 = Arrays.copyOf(cap0, n);
    }

    public int nodeCount() {
        return nodeCount;
    }

    public int arcCount() {
        return arcCount;
    }

    public int headArc(int u) {
        return head[u];
    }

    public int nextArc(int e) {
        return next[e];
    }

    public int target(int e) {
        return to[e];
    }

    public long residual(int e) {
        return cap[e];
    }

    /** Flow currently on arc e; negative on a reverse arc, which is the intended reading. */
    public long flow(int e) {
        return cap0[e] - cap[e];
    }

    public void setCapacity(int e, long c) {
        cap[e] = c;
        cap0[e] = c;
    }

    /** Moves {@code delta} units along arc e, updating the reverse arc. */
    public void push(int e, long delta) {
        cap[e] -= delta;
        cap[e ^ 1] += delta;
    }

    /** Runs Dinic from {@code s} to {@code t} on the current residual graph and returns the added flow. */
    public long maxFlow(int s, int t) {
        long added = 0;
        while (buildLevels(s, t)) {
            System.arraycopy(head, 0, iter, 0, nodeCount);
            long f;
            while ((f = dfs(s, t, INF)) > 0) {
                added += f;
            }
        }
        return added;
    }

    private boolean buildLevels(int s, int t) {
        Arrays.fill(levelOf, -1);
        int[] queue = new int[nodeCount];
        int qh = 0;
        int qt = 0;
        levelOf[s] = 0;
        queue[qt++] = s;
        while (qh < qt) {
            int u = queue[qh++];
            for (int e = head[u]; e != -1; e = next[e]) {
                if (cap[e] > 0 && levelOf[to[e]] < 0) {
                    levelOf[to[e]] = levelOf[u] + 1;
                    queue[qt++] = to[e];
                }
            }
        }
        return levelOf[t] >= 0;
    }

    private long dfs(int u, int t, long limit) {
        if (u == t) {
            return limit;
        }
        for (; iter[u] != -1; iter[u] = next[iter[u]]) {
            int e = iter[u];
            int v = to[e];
            if (cap[e] > 0 && levelOf[v] == levelOf[u] + 1) {
                long d = dfs(v, t, Math.min(limit, cap[e]));
                if (d > 0) {
                    push(e, d);
                    return d;
                }
                levelOf[v] = -1;
            }
        }
        iter[u] = -1;
        return 0;
    }

    /** Nodes reachable from {@code s} in the residual graph; the source side of a minimum cut. */
    public boolean[] residualReachable(int s) {
        boolean[] seen = new boolean[nodeCount];
        int[] stack = new int[nodeCount];
        int sp = 0;
        seen[s] = true;
        stack[sp++] = s;
        while (sp > 0) {
            int u = stack[--sp];
            for (int e = head[u]; e != -1; e = next[e]) {
                if (cap[e] > 0 && !seen[to[e]]) {
                    seen[to[e]] = true;
                    stack[sp++] = to[e];
                }
            }
        }
        return seen;
    }
}
