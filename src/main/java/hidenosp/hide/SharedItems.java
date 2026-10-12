package hidenosp.hide;

import hidenosp.core.Pattern;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The components of the graph that links two sensitive patterns when they share an item. Masking a
 * position keeps its index and removes only the nodes of patterns containing its item, so hiding
 * several patterns splits over these components (proposition P14 of the design).
 */
public final class SharedItems {

    private SharedItems() {
    }

    /** component[k] is the smallest index of a pattern in the component of pattern k. */
    public static int[] components(List<Pattern> patterns) {
        int n = patterns.size();
        int[] parent = new int[n];
        for (int k = 0; k < n; k++) {
            parent[k] = k;
        }
        Map<Integer, Integer> firstWithItem = new HashMap<>();
        for (int k = 0; k < n; k++) {
            for (int item : patterns.get(k).items()) {
                Integer other = firstWithItem.putIfAbsent(item, k);
                if (other != null) {
                    union(parent, k, other);
                }
            }
        }
        int[] component = new int[n];
        for (int k = 0; k < n; k++) {
            component[k] = find(parent, k);
        }
        return component;
    }

    private static int find(int[] parent, int k) {
        while (parent[k] != k) {
            parent[k] = parent[parent[k]];
            k = parent[k];
        }
        return k;
    }

    private static void union(int[] parent, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra != rb) {
            parent[Math.max(ra, rb)] = Math.min(ra, rb);
        }
    }
}
