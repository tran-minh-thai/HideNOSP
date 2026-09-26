"""Sanity checks (not proofs) for the Nettree cut propositions, version 2.
Checks: L0 (sup_NO = max node-disjoint paths = min node cut), P1, P2 (optimal sets are
subsets of minimum cuts), L5 (vital node <=> node in some minimum cut), greedy vital removal,
weighted t > 0 (two candidate algorithms against the optimum), convexity of the per-sequence
cost curve, P3 tightness. Exhaustive search on small random instances.
Run: python3 nettree_checks.py"""
import itertools, math, random
from collections import defaultdict

INF = float('inf')

def occs(P, S, gmin, gmax):
    m, res = len(P), []
    def rec(j, prev, cur):
        if j == m:
            res.append(tuple(cur)); return
        rng = range(len(S)) if j == 0 else range(prev + 1 + gmin, min(len(S), prev + 2 + gmax))
        for p in rng:
            if S[p] == P[j]:
                rec(j + 1, p, cur + [p])
    rec(0, -1, [])
    return res

def sup_no_brute(P, S, gmin, gmax):
    O, best = occs(P, S, gmin, gmax), 0
    nonov = lambda a, b: all(x != y for x, y in zip(a, b))
    def bt(i, chosen):
        nonlocal best
        if len(chosen) + len(O) - i <= best: return
        if i == len(O):
            best = max(best, len(chosen)); return
        if all(nonov(O[i], c) for c in chosen): bt(i + 1, chosen + [O[i]])
        bt(i + 1, chosen)
    bt(0, [])
    return best

def nettree(P, S, gmin, gmax):
    """nodes (j, i); arcs (j,i)->(j+1,i') with gmin <= i'-i-1 <= gmax"""
    m = len(P)
    nodes = [(j, i) for j in range(m) for i in range(len(S)) if S[i] == P[j]]
    arcs = [((j, i), (j + 1, k)) for (j, i) in nodes if j + 1 < m
            for k in range(i + 1 + gmin, min(len(S), i + 2 + gmax)) if S[k] == P[j + 1]]
    return nodes, arcs

def maxflow(P, S, gmin, gmax, removed=frozenset(), cap=None):
    """max number of node-disjoint level-1 -> level-m paths (cap=None) or min-weight cut value."""
    nodes, arcs = nettree(P, S, gmin, gmax)
    m = len(P)
    g = defaultdict(lambda: defaultdict(float))
    for v in nodes:
        if v in removed: continue
        c = 1 if cap is None else cap[v]
        g[('in', v)][('out', v)] += c
        if v[0] == 0: g['s'][('in', v)] += INF
        if v[0] == m - 1: g[('out', v)]['t'] += INF
    for u, v in arcs:
        if u in removed or v in removed: continue
        g[('out', u)][('in', v)] += INF
    flow = 0
    while True:  # BFS augmenting paths (Edmonds-Karp)
        par, q = {'s': None}, ['s']
        while q and 't' not in par:
            nq = []
            for x in q:
                for y, c in g[x].items():
                    if c > 0 and y not in par:
                        par[y] = x; nq.append(y)
            q = nq
        if 't' not in par: break
        path, y = [], 't'
        while par[y] is not None:
            path.append((par[y], y)); y = par[y]
        b = min(g[x][y] for x, y in path)
        for x, y in path:
            g[x][y] -= b; g[y][x] += b
        flow += b
    reach, st = {'s'}, ['s']
    while st:
        x = st.pop()
        for y, c in g[x].items():
            if c > 0 and y not in reach:
                reach.add(y); st.append(y)
    cut = [v for v in nodes if v not in removed and ('in', v) in reach and ('out', v) not in reach]
    return (int(flow) if cap is None else flow), cut

def h_eval(P, S, gmin, gmax, nodes, w, t, lam, F):
    """h(lam) = min over min-cardinality cuts of sum(min(w, lam)) - t*lam, its cut, and its slope."""
    M = 4 * sum(w.values()) + 1
    val, C = maxflow(P, S, gmin, gmax, cap={v: M + min(w[v], lam) for v in nodes})
    return val - M * F - t * lam, C, sum(1 for v in C if w[v] > lam) - t

def lambda_sweep(P, S, gmin, gmax, nodes, w, t, F, refine=True):
    """Returns (LB, list of cuts). h is concave piecewise linear; scan the weight values, then
    (refine=True) locate the exact maximum by intersecting supporting lines."""
    pts = {}
    for lam in sorted(set(w.values()) | {0}):
        pts[lam] = h_eval(P, S, gmin, gmax, nodes, w, t, lam, F)
    cuts = [c for _, c, _ in pts.values()]
    lb = max(v for v, _, _ in pts.values())
    if refine:
        lams = sorted(pts)
        for a, b in zip(lams, lams[1:]):
            (va, _, sa), (vb, _, sb) = pts[a], pts[b]
            lo, hi = (a, va, sa), (b, vb, sb)
            for _ in range(60):
                if lo[2] <= 0 or hi[2] >= 0 or lo[2] == hi[2]: break
                x = (hi[1] - lo[1] + lo[2] * lo[0] - hi[2] * hi[0]) / (lo[2] - hi[2])
                if not (lo[0] < x < hi[0]): break
                vx, cx, sx = h_eval(P, S, gmin, gmax, nodes, w, t, x, F)
                cuts.append(cx); lb = max(lb, vx)
                if abs(vx - (lo[1] + lo[2] * (x - lo[0]))) < 1e-9: break
                if sx > 0: lo = (x, vx, sx)
                elif sx < 0: hi = (x, vx, sx)
                else: break
    return lb, cuts

def is_cut(P, S, gmin, gmax, C):
    return maxflow(P, S, gmin, gmax, frozenset(C))[0] == 0

def rand_instance(rng, distinct=True):
    S = [rng.choice('abcd') for _ in range(rng.randint(3, 10))]
    m = rng.randint(1, 3)
    P = rng.sample('abcd', m) if distinct else [rng.choice('ab') for _ in range(m)]
    gmin = rng.randint(0, 1); gmax = gmin + rng.randint(0, 3)
    return P, S, gmin, gmax

def main(seed=2026, trials=1200):
    rng = random.Random(seed)
    cnt = defaultdict(int)
    h1_bad, h2_bad, nonconvex = [], [], []
    for _ in range(trials):
        P, S, gmin, gmax = rand_instance(rng, distinct=True)
        nodes, _ = nettree(P, S, gmin, gmax)
        F, _ = maxflow(P, S, gmin, gmax); F = int(F)
        # L0: max flow equals brute-force sup_NO; min cut size equals F
        cnt['L0'] += 1
        cnt['L0_bad'] += F != sup_no_brute(P, S, gmin, gmax)
        mincuts = [set(C) for C in itertools.combinations(nodes, F) if is_cut(P, S, gmin, gmax, C)] if F else [set()]
        cnt['L0_bad'] += (F > 0 and not mincuts)
        if F == 0: continue
        # L5: vital <=> in some minimum cut
        in_some = set().union(*mincuts)
        for v in nodes:
            vital = maxflow(P, S, gmin, gmax, frozenset([v]))[0] < F
            cnt['L5'] += 1; cnt['L5_bad'] += vital != (v in in_some)
        w = {v: rng.randint(1, 6) for v in nodes}
        cost = [0]
        for t in range(F - 1, -1, -1):
            k = F - t
            # P1 and P2 by enumeration over node subsets of size k
            feas = [set(R) for R in itertools.combinations(nodes, k)
                    if maxflow(P, S, gmin, gmax, frozenset(R))[0] <= t]
            cnt['P1'] += 1; cnt['P1_bad'] += not feas
            smaller = any(maxflow(P, S, gmin, gmax, frozenset(R))[0] <= t
                          for R in itertools.combinations(nodes, k - 1)) if k > 1 else (F <= t)
            cnt['P1_bad'] += smaller
            cnt['P2'] += len(feas)
            cnt['P2_bad'] += sum(not any(R <= C for C in mincuts) for R in feas)
            # greedy removal of any vital node reaches t in exactly k steps
            rem = set()
            while maxflow(P, S, gmin, gmax, frozenset(rem))[0] > t:
                cur = maxflow(P, S, gmin, gmax, frozenset(rem))[0]
                v = next(v for v in nodes if v not in rem
                         and maxflow(P, S, gmin, gmax, frozenset(rem | {v}))[0] < cur)
                rem.add(v)
            cnt['GV'] += 1; cnt['GV_bad'] += len(rem) != k
            # weighted: optimum over feasible sets of size k
            opt = min(sum(w[v] for v in R) for R in feas)
            cost.append(opt)
            # H1: min-weight minimum cut (capacity M + w), keep the k lightest nodes
            M = sum(w.values()) + 1
            _, C = maxflow(P, S, gmin, gmax, cap={v: M + w[v] for v in nodes})
            h1 = sum(sorted(w[v] for v in C)[:k])
            # H2: repeatedly remove the lightest vital node
            rem = set()
            while maxflow(P, S, gmin, gmax, frozenset(rem))[0] > t:
                cur = maxflow(P, S, gmin, gmax, frozenset(rem))[0]
                v = min((v for v in nodes if v not in rem
                         and maxflow(P, S, gmin, gmax, frozenset(rem | {v}))[0] < cur), key=lambda v: w[v])
                rem.add(v)
            h2 = sum(w[v] for v in rem)
            cnt['W'] += 1
            if h1 > opt: h1_bad.append((''.join(P), ''.join(S), gmin, gmax, t, opt, h1))
            if h2 > opt: h2_bad.append((''.join(P), ''.join(S), gmin, gmax, t, opt, h2))
        d = [cost[i + 1] - cost[i] for i in range(len(cost) - 1)]
        cnt['CV'] += 1
        if any(d[i + 1] < d[i] for i in range(len(d) - 1)): nonconvex.append((''.join(P), ''.join(S), gmin, gmax, cost))
    print(f"L0: {cnt['L0']} instances, {cnt['L0_bad']} mismatches (max flow vs brute-force sup_NO, min cut size)")
    print(f"L5: {cnt['L5']} nodes, {cnt['L5_bad']} mismatches (vital vs member of some minimum cut)")
    print(f"P1: {cnt['P1']} (instance, t) pairs, {cnt['P1_bad']} violations")
    print(f"P2: {cnt['P2']} optimal-size feasible sets, {cnt['P2_bad']} not inside a minimum cut")
    print(f"Greedy vital removal: {cnt['GV']} runs, {cnt['GV_bad']} used more than F - t masks")
    print(f"Weighted t>0: {cnt['W']} runs; H1 (min-weight min cut, lightest F-t) suboptimal in {len(h1_bad)}; "
          f"H2 (lightest vital node, repeated) suboptimal in {len(h2_bad)}")
    for x in h1_bad[:3]: print("   H1 counterexample P,S,gmin,gmax,t,opt,h1 =", x)
    for x in h2_bad[:3]: print("   H2 counterexample P,S,gmin,gmax,t,opt,h2 =", x)
    print(f"Cost curve c(k): {cnt['CV']} instances, {len(nonconvex)} non-convex")
    for x in nonconvex[:3]: print("   non-convex P,S,gmin,gmax,c =", x)

def l0_extended(seed=17, trials=1500):
    rng = random.Random(seed); bad = 0
    for _ in range(trials):
        S = [rng.choice('ab') for _ in range(rng.randint(1, 9))]
        P = [rng.choice('ab') for _ in range(rng.randint(1, 3))]
        gmin = rng.randint(0, 1); gmax = rng.choice([gmin, gmin + 1, gmin + 3, 10 ** 6])
        bad += maxflow(P, S, gmin, gmax)[0] != sup_no_brute(P, S, gmin, gmax)
    print(f"L0 extended (repeated items, m = 1, gmax = inf): {trials} instances, {bad} mismatches")

def p3_tightness(seed=7, trials=1500):
    rng = random.Random(seed); tight = total = 0; ex = None
    for _ in range(trials):
        P, S, gmin, gmax = rand_instance(rng, distinct=False)
        if len(set(P)) == len(P): continue
        mu = max(P.count(x) for x in set(P)); F = sup_no_brute(P, S, gmin, gmax)
        for t in range(F):
            k = next(k for k in range(len(S) + 1) for R in itertools.combinations(range(len(S)), k)
                     if sup_no_brute(P, ['#' if i in R else c for i, c in enumerate(S)], gmin, gmax) <= t)
            total += 1
            if k == math.ceil((F - t) / mu) and k < F - t:
                tight += 1; ex = ex or (''.join(P), ''.join(S), gmin, gmax, F, t, k)
    print(f"P3: {total} repeated-item (instance, t) pairs; lower bound attained with k < F - t in {tight}; example {ex}")

def weighted_stress(seeds=(1, 2, 3, 4, 5), per_seed=4000):
    """Weighted t > 0 on larger instances: H1, H2 and the lambda sweep (H3) against the optimum.
    H3: for each distinct weight lam, take a minimum-cardinality cut minimising sum(min(w, lam));
    UB = best sum of the F - t lightest true weights over these cuts;
    LB = max over lam of (sum(min(w, lam)) over that cut - t*lam), a valid lower bound."""
    runs = h1b = h2b = h3b = h3b0 = cert = cert0 = 0; ex = {}
    for seed in seeds:
        for law in (0, 1):
            rng = random.Random(1000 * seed + law)
            for _ in range(per_seed):
                n = rng.randint(6, 14); S = [rng.choice('abc') for _ in range(n)]
                P = rng.sample('abc', rng.randint(2, 3))
                gmin = rng.randint(0, 1); gmax = gmin + rng.randint(0, 4)
                nodes, _ = nettree(P, S, gmin, gmax)
                if len(nodes) > 15: continue
                F = maxflow(P, S, gmin, gmax)[0]
                if F < 2: continue
                w = {v: (rng.choice([1, 2, 3, 50, 100]) if law == 0 else rng.randint(1, 20)) for v in nodes}
                M = 4 * sum(w.values()) + 1
                _, C1 = maxflow(P, S, gmin, gmax, cap={v: M + w[v] for v in nodes})
                for k in range(1, F):
                    t = F - k
                    opt = min(sum(w[v] for v in R) for R in itertools.combinations(nodes, k)
                              if maxflow(P, S, gmin, gmax, frozenset(R))[0] <= t)
                    h1 = sum(sorted(w[v] for v in C1)[:k])
                    rem = set()
                    while True:
                        cur = maxflow(P, S, gmin, gmax, frozenset(rem))[0]
                        if cur <= t: break
                        rem.add(min((v for v in nodes if v not in rem and
                                     maxflow(P, S, gmin, gmax, frozenset(rem | {v}))[0] < cur), key=lambda v: w[v]))
                    h2 = sum(w[v] for v in rem)
                    lb0, cuts0 = lambda_sweep(P, S, gmin, gmax, nodes, w, t, F, refine=False)
                    lb, cuts = lambda_sweep(P, S, gmin, gmax, nodes, w, t, F, refine=True)
                    ub = min(sum(sorted(w[v] for v in C)[:k]) for C in cuts)
                    ub0 = min(sum(sorted(w[v] for v in C)[:k]) for C in cuts0)
                    assert lb <= opt + 1e-9 and lb0 <= lb + 1e-9
                    runs += 1; h1b += h1 > opt; h2b += h2 > opt
                    h3b += ub > opt; h3b0 += ub0 > opt
                    cert += ub - lb < 1 - 1e-9          # integer weights: LB rounded up equals UB
                    cert0 += ub - lb0 < 1 - 1e-9
                    inst = (''.join(P), ''.join(S), gmin, gmax, t, opt, {(j + 1, i + 1): w[(j, i)] for (j, i) in nodes})
                    if ub > opt and 'H3' not in ex: ex['H3'] = inst + (ub,)
                    if t > 0 and h2 > opt and 'H2' not in ex: ex['H2'] = inst + (h2,)
    print(f"Weighted stress: {runs} runs (0 < t < F); suboptimal: H1 {h1b}, H2 {h2b}, "
          f"lambda sweep {h3b0} scanning weight values only, {h3b} with exact maximisation of h")
    print(f"   certified optimal (ceil(LB) = UB): {cert} with exact maximisation of h, {cert0} scanning weight values only")
    print("   lambda-sweep counterexample (P,S,gmin,gmax,t,opt,weights[(level,pos)],UB):", ex.get('H3'))
    print("   H2 counterexample with t > 0 (P,S,gmin,gmax,t,opt,weights,H2):", ex.get('H2'))

def convexity(seeds=range(1, 9), per_seed=2500):
    tot = bad = 0; ex = None
    for seed in seeds:
        rng = random.Random(100 + seed)
        for _ in range(per_seed):
            n = rng.randint(6, 15); S = [rng.choice('abc') for _ in range(n)]
            P = rng.sample('abc', rng.randint(1, 3))
            gmin = rng.randint(0, 1); gmax = gmin + rng.randint(0, 4)
            nodes, _ = nettree(P, S, gmin, gmax)
            if len(nodes) > 15: continue
            F = maxflow(P, S, gmin, gmax)[0]
            if F < 3: continue
            w = {v: (rng.choice([1, 2, 3, 50, 100]) if seed % 2 else rng.randint(1, 20)) for v in nodes}
            c = [0] + [min(sum(w[v] for v in R) for R in itertools.combinations(nodes, k)
                           if maxflow(P, S, gmin, gmax, frozenset(R))[0] <= F - k) for k in range(1, F + 1)]
            tot += 1; d = [c[i + 1] - c[i] for i in range(F)]
            if any(d[i + 1] < d[i] for i in range(F - 1)):
                bad += 1; ex = ex or (''.join(P), ''.join(S), gmin, gmax, c, {(j + 1, i + 1): w[(j, i)] for (j, i) in nodes})
    print(f"Cost curve c(k), F >= 3: {tot} instances, {bad} non-convex; example (P,S,gmin,gmax,c,weights[(level,pos)]): {ex}")

def counterexample_h1_h2():
    P, S, g = list('ca'), list('babbacbcaacb'), (0, 2)
    w = {(0, 5): 3, (0, 7): 50, (1, 8): 100, (1, 9): 1, (0, 10): 100, (1, 1): 50, (1, 4): 2}
    print("H1/H2 counterexample: P = ca, S = babbacbcaacb, gap [0,2]; F =", maxflow(P, S, *g)[0])
    nodes, _ = nettree(P, S, *g)
    for k in (1, 2):
        best = min((sum(w[v] for v in R), R) for R in itertools.combinations(nodes, k)
                   if maxflow(P, S, *g, frozenset(R))[0] <= 2 - k)
        print(f"   k = {k}: optimum weight {best[0]} at positions {[i + 1 for _, i in best[1]]}")

def abul_heuristic(seed_first=3, seed_all=5):
    """Local heuristic of Abul et al. 2007 (mark the position on most matches, repeat until no match)
    against the optimum sup_NO (Corollary P1c), one distinct-item pattern, full removal."""
    from functools import lru_cache
    def run(P, S, gmin, gmax):
        S = list(S); marks = 0
        while True:
            O = occs(P, S, gmin, gmax)
            if not O: return marks
            cnt = defaultdict(int)
            for o in O:
                for p in set(o): cnt[p] += 1
            b = max(cnt.values()); S[min(q for q in cnt if cnt[q] == b)] = '#'; marks += 1
    def best_over_ties(P, S, gmin, gmax):
        @lru_cache(None)
        def rec(Sx):
            O = occs(P, list(Sx), gmin, gmax)
            if not O: return 0
            cnt = defaultdict(int)
            for o in O:
                for p in set(o): cnt[p] += 1
            b = max(cnt.values())
            return 1 + min(rec(Sx[:p] + '#' + Sx[p + 1:]) for p in cnt if cnt[p] == b)
        return rec(''.join(S))
    for seed, trials, fn, label in ((seed_first, 20000, run, 'leftmost tie-break'),
                                    (seed_all, 40000, best_over_ties, 'every tie-break')):
        rng = random.Random(seed); tot = worse = 0
        for _ in range(trials):
            S = [rng.choice('abc') for _ in range(rng.randint(3, 12))]
            P = rng.sample('abc', rng.randint(2, 3)); gmax = rng.choice([0, 1, 2, 3, 100])
            F = maxflow(P, S, 0, gmax)[0]
            if F == 0: continue
            tot += 1; worse += fn(P, S, 0, gmax) > F
        print(f"Abul local heuristic vs sup_NO ({label}): {tot} sequences, more marks than optimum in {worse}")
    P, S = list('abc'), list('ababcbc')
    print("   example P = abc, T = ababcbc, gap [0,3]: optimum", maxflow(P, S, 0, 3)[0], "heuristic", run(P, S, 0, 3))

if __name__ == "__main__":
    main(); l0_extended(); p3_tightness(); counterexample_h1_h2(); abul_heuristic(); weighted_stress(); convexity()
