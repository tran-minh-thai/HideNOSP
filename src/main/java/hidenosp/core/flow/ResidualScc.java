// SPDX-License-Identifier: MIT
package hidenosp.core.flow;

import java.util.Arrays;

/**
 * Strongly connected components of the residual graph of a {@link Dinic} instance, computed with an
 * iterative Tarjan so that long sequences cannot overflow the Java stack.
 *
 * <p>This is the second half of the vital-node test of lemma L5 (computation note): the node arc
 * in(v) to out(v) belongs to some minimum cut exactly when it is saturated and its two endpoints
 * lie in different components [R29]. One max flow plus one run of this class therefore yields every
 * vital node of a Nettree at once, which is what the index of section 9.1 stores.
 */
public final class ResidualScc {

    private ResidualScc() {
    }

    /** Component id per graph node; ids are arbitrary but equal exactly on one component. */
    public static int[] compute(Dinic g) {
        int n = g.nodeCount();
        int[] index = new int[n];
        int[] low = new int[n];
        int[] comp = new int[n];
        boolean[] onStack = new boolean[n];
        Arrays.fill(index, -1);
        Arrays.fill(comp, -1);

        int[] stack = new int[n];
        int sp = 0;
        int[] callNode = new int[n + 1];
        int[] callArc = new int[n + 1];
        int counter = 0;
        int components = 0;

        for (int root = 0; root < n; root++) {
            if (index[root] != -1) {
                continue;
            }
            int depth = 0;
            callNode[0] = root;
            callArc[0] = -2;
            while (depth >= 0) {
                int u = callNode[depth];
                if (callArc[depth] == -2) {
                    index[u] = counter;
                    low[u] = counter;
                    counter++;
                    stack[sp++] = u;
                    onStack[u] = true;
                    callArc[depth] = g.headArc(u);
                } else {
                    int e = callArc[depth];
                    int v = g.target(e);
                    low[u] = Math.min(low[u], low[v]);
                    callArc[depth] = g.nextArc(e);
                }
                boolean descended = false;
                for (int e = callArc[depth]; e != -1; e = g.nextArc(e)) {
                    if (g.residual(e) <= 0) {
                        continue;
                    }
                    int v = g.target(e);
                    if (index[v] == -1) {
                        callArc[depth] = e;
                        depth++;
                        callNode[depth] = v;
                        callArc[depth] = -2;
                        descended = true;
                        break;
                    }
                    if (onStack[v]) {
                        low[u] = Math.min(low[u], index[v]);
                    }
                    callArc[depth] = g.nextArc(e);
                }
                if (descended) {
                    continue;
                }
                if (low[u] == index[u]) {
                    int w;
                    do {
                        w = stack[--sp];
                        onStack[w] = false;
                        comp[w] = components;
                    } while (w != u);
                    components++;
                }
                depth--;
            }
        }
        return comp;
    }
}
