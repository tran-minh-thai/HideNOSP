"""GD&L 2011 setting (no gap constraint, deletion, sup = #sequences, hide below psi).
One sensitive pattern. DBSH (Exclude by symbol-level Cost + Candidates) vs exact min cut (Corollary P1c)."""
import sys, random
sys.path.insert(0, '..')
from kiemtra_v2 import occs, maxflow
G = 10**6
def supports(P, T): it = iter(T); return all(c in it for c in P)
def dbsh_seq(P, T):
    # Cost/Candidates for one sensitive sequence: symbol with fewest occurrences; delete its
    # occurrences left to right until T no longer supports P (Update-paths)
    # symbol-level Cost picks the event e whose occurrences ON COMPLETE PATHS (graph nodes) are fewest;
    # delete such nodes left to right, updating the graph, until no complete path remains
    T = list(T); d = 0
    onpath = lambda T: {p for o in occs(P, T, 0, G) for p in o}
    nodes = onpath(T)
    e = min(set(P), key=lambda c: sum(1 for p in nodes if T[p] == c))
    while True:
        nodes = onpath(T)
        if not nodes: return d
        cand = sorted(p for p in nodes if T[p] == e)
        T[cand[0]] = '#'; d += 1
rng = random.Random(4); tot = worse = 0; ex_d = []; costgap = 0; n_seq = 0; ex = None
for _ in range(400):
    P = rng.sample('abcdef', rng.randint(2, 3))
    D = [[rng.choice('abcdef') for _ in range(rng.randint(4, 20))] for _ in range(60)]
    S = [T for T in D if supports(P, T)]
    if len(S) < 6: continue
    psi = len(S) // 3   # keep psi-1 supporting transactions
    # DBSH: Exclude keeps the psi-1 transactions with the highest symbol-level Cost
    cost = lambda T: min(sum(1 for p in {q for o in occs(P, T, 0, G) for q in o} if T[p] == c) for c in set(P))
    keep_d = sorted(S, key=cost, reverse=True)[:psi - 1]
    dist_d = sum(dbsh_seq(P, T) for T in S if not any(T is k for k in keep_d))
    # exact: keep the psi-1 transactions with the highest exact cost sup_NO, cut the rest exactly
    ex_costs = sorted((maxflow(P, T, 0, G)[0] for T in S), reverse=True)
    dist_f = sum(ex_costs[psi - 1:])
    tot += 1; worse += dist_d > dist_f; ex_d.append((dist_d - dist_f) / dist_f)
    for T in S:
        n_seq += 1; c, f = dbsh_seq(P, T), maxflow(P, T, 0, G)[0]
        if c > f:
            costgap += 1
            if ex is None or len(T) < len(ex[1]): ex = (''.join(P), ''.join(T), c, f)
import numpy as np
print(f"{tot} databases: DBSH distance > exact in {worse}; mean excess {100*np.mean(ex_d):.1f}%, max {100*max(ex_d):.1f}%")
print(f"per transaction: DBSH deletes more than the optimum in {costgap} of {n_seq}; smallest example {ex}")
