"""Old problem, side effects. Support = number of sequences with gap [0,g]. One sensitive pattern,
full removal from (sup - psi) sequences by marking. Compare Abul HH, flow (min marks), weighted flow."""
import sys, random, itertools, time
sys.path.insert(0, '..')
from kiemtra_v2 import occs, maxflow, nettree
import numpy as np

def contains(Q, T, g):
    # greedy leftmost is not valid with max gap; use reachable-set DP
    cur = {i for i, c in enumerate(T) if c == Q[0]}
    for q in Q[1:]:
        cur = {k for i in cur for k in range(i + 1, min(len(T), i + 2 + g)) if T[k] == q}
        if not cur: return False
    return bool(cur)

def freq(D, g, sigma, alph, L=3):
    F = {}
    for l in range(1, L + 1):
        for Q in itertools.product(alph, repeat=l):
            s = sum(contains(Q, T, g) for T in D)
            if s >= sigma: F[Q] = s
    return F

def is_super(Q, P):  # P subsequence of Q (then Q must be lost too)
    it = iter(Q); return all(c in it for c in P)

def most_matches(P, T, g):
    T = list(T); k = 0
    while True:
        O = occs(P, T, 0, g)
        if not O: return T, k
        cnt = {}
        for o in O:
            for p in o: cnt[p] = cnt.get(p, 0) + 1
        b = max(cnt.values()); p = min(q for q in cnt if cnt[q] == b); T[p] = '#'; k += 1

def weighted_cut(P, T, g, w, lam):
    nodes, _ = nettree(P, T, 0, g)
    val, C = maxflow(P, T, 0, g, cap={v: 1 + lam * w.get(v[1], 0.0) for v in nodes})
    T2 = list(T)
    for (_, i) in C: T2[i] = '#'
    return T2, len(C), val

def harm_weights(T, F, sigma, g, P, eps=1.0):
    """w(i) = sum over near-border non-sensitive Q present in T that masking i alone removes from T."""
    w = {}
    for Q, s in F.items():
        if s >= (1 + eps) * sigma or is_super(Q, P) or not contains(Q, T, g): continue
        for i in range(len(T)):
            if T[i] in Q:
                T2 = T[:i] + ['#'] + T[i + 1:]
                if not contains(Q, T2, g): w[i] = w.get(i, 0) + 1.0 / (s - sigma + 1)
    return w

def run(seed, N=50, alph='abcdef'):
    rng = random.Random(seed)
    g = rng.choice([1, 2, 3])
    D = [[rng.choice(alph) for _ in range(rng.randint(6, 14))] for _ in range(N)]
    sigma = int(0.2 * N)
    F = freq(D, g, sigma, alph)
    cand = [Q for Q in F if len(Q) in (2, 3) and len(set(Q)) == len(Q)]
    if not cand: return None
    P = rng.choice(cand); sup = [i for i, T in enumerate(D) if contains(P, T, g)]
    psi = sigma - 1; k = len(sup) - psi
    base = {Q: s for Q, s in F.items() if not is_super(Q, P)}
    res = {}
    def evaluate(Dn, marks):
        Fn = freq(Dn, g, sigma, alph)
        lost = sum(1 for Q in base if Q not in Fn)
        sd = np.mean([(base[Q] - Fn.get(Q, 0)) / base[Q] for Q in base])
        assert not contains(P, max(Dn, key=len), g) or sum(contains(P, T, g) for T in Dn) <= psi
        return marks, lost, sd
    # Abul HH
    order = sorted(sup, key=lambda i: len(occs(P, D[i], 0, g)))[:k]
    Dn = [list(T) for T in D]; marks = 0
    for i in order: Dn[i], m = most_matches(P, D[i], g); marks += m
    res['HH'] = evaluate(Dn, marks)
    # random sequences + random positions on matches (RR)
    Dn = [list(T) for T in D]; marks = 0
    for i in rng.sample(sup, k):
        T = list(D[i])
        while True:
            O = occs(P, T, 0, g)
            if not O: break
            p = rng.choice(rng.choice(O)); T[p] = '#'; marks += 1
        Dn[i] = T
    res['RR'] = evaluate(Dn, marks)
    # flow, min marks: k sequences with smallest sup_NO, min cut
    costs = sorted((maxflow(P, D[i], 0, g)[0], i) for i in sup)[:k]
    Dn = [list(T) for T in D]; marks = 0
    for _, i in costs:
        Dn[i], m, _ = weighted_cut(P, D[i], g, {}, 0.0); marks += m
    res['FLOW'] = evaluate(Dn, marks)
    # weighted flow: cost = weighted cut value, pick k cheapest
    for lam in (1.0, 5.0):
        cand_cuts = []
        for i in sup:
            w = harm_weights(D[i], F, sigma, g, P)
            T2, m, val = weighted_cut(P, D[i], g, w, lam); cand_cuts.append((val, i, T2, m))
        Dn = [list(T) for T in D]; marks = 0
        for val, i, T2, m in sorted(cand_cuts)[:k]: Dn[i] = T2; marks += m
        res[f'WFLOW{lam:g}'] = evaluate(Dn, marks)
    return res, len(base)

if __name__ == '__main__':
    agg = {}; n = 0
    for seed in range(60):
        r = run(seed)
        if r is None: continue
        r, nb = r; n += 1
        for kname, v in r.items(): agg.setdefault(kname, []).append(v)
    print(f"{n} databases (N=50, |alphabet|=6, gap [0,g], g in 1..3, sigma = 20%, one sensitive pattern)")
    hh = np.array([v[1] for v in agg['HH']])
    for kname, v in agg.items():
        v = np.array(v, dtype=float)
        lost = v[:, 1]
        print(f"  {kname:8s} marks {v[:,0].mean():6.1f}  lost {lost.mean():6.2f}  supDist {100*v[:,2].mean():5.2f}%  "
              f"lost < HH in {np.sum(lost < hh)}, = in {np.sum(lost == hh)}, > in {np.sum(lost > hh)}")
