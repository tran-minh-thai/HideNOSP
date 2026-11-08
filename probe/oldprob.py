# SPDX-License-Identifier: MIT
"""Old problem (Abul 2007 / GD&L 2011): support = number of sequences, full removal of the
sensitive pattern(s) from selected sequences by marking. Probe: Abul's heuristics vs flow-based."""
import sys, random, itertools
sys.path.insert(0, '..')
from nettree_checks import occs, maxflow
import numpy as np
from scipy.optimize import milp, LinearConstraint, Bounds, linprog

def most_matches(Ps, T, gmin, gmax):
    T = list(T); marks = []
    while True:
        O = [o for P in Ps for o in occs(P, T, gmin, gmax)]
        if not O: return marks
        cnt = {}
        for o in O:
            for p in set(o): cnt[p] = cnt.get(p, 0) + 1
        b = max(cnt.values()); p = min(q for q in cnt if cnt[q] == b)
        T[p] = '#'; marks.append(p)

def exact_hit(Ps, T, gmin, gmax, relax=False):
    paths = [set(o) for P in Ps for o in occs(P, T, gmin, gmax)]
    if not paths: return 0, []
    pos = sorted(set().union(*paths)); idx = {p: i for i, p in enumerate(pos)}
    A = np.zeros((len(paths), len(pos)))
    for r, pth in enumerate(paths):
        for p in pth: A[r, idx[p]] = 1
    c = np.ones(len(pos))
    if relax:
        res = linprog(c, A_ub=-A, b_ub=-np.ones(len(paths)), bounds=(0, 1), method='highs')
        return res.fun, dict(zip(pos, res.x)), paths
    res = milp(c, constraints=LinearConstraint(A, lb=1), integrality=np.ones(len(pos)), bounds=Bounds(0, 1))
    return round(res.fun), [p for p, v in zip(pos, res.x) if v > 0.5]

def lp_round(Ps, T, gmin, gmax):
    val, x, paths = exact_hit(Ps, T, gmin, gmax, relax=True)
    m = max(len(P) for P in Ps)
    R = {p for p, v in x.items() if v >= 1.0 / m - 1e-9}
    # clean-up: drop redundant positions greedily (keeps feasibility)
    for p in sorted(R, key=lambda q: x[q]):
        if all(pth & (R - {p}) for pth in paths): R.discard(p)
    return len(R), val

def exp_global(seed=1, trials=300):
    rng = random.Random(seed); worse = 0; tot = 0; exc = []
    for _ in range(trials):
        P = rng.sample('abcde', rng.randint(2, 3)); gmax = rng.choice([1, 2, 3, 100])
        D = [[rng.choice('abcde') for _ in range(rng.randint(5, 14))] for _ in range(40)]
        sup = [T for T in D if occs(P, T, 0, gmax)]
        if len(sup) < 4: continue
        psi = len(sup) // 2; k = len(sup) - psi
        # Abul HH: sort by number of matches (all embeddings), local most-matches
        hh = sorted(sup, key=lambda T: len(occs(P, T, 0, gmax)))[:k]
        m_hh = sum(len(most_matches([P], T, 0, gmax)) for T in hh)
        # flow: exact cost = sup_NO(P,T) (Corollary P1c), take k cheapest, cut each exactly
        m_fl = sum(sorted(maxflow(P, T, 0, gmax)[0] for T in sup)[:k])
        tot += 1; worse += m_hh > m_fl; exc.append((m_hh - m_fl) / m_fl)
    print(f"[global, 1 pattern] {tot} databases (40 seq.): Abul HH total marks > optimum in {worse}; "
          f"mean excess {100*np.mean(exc):.1f}%, max {100*max(exc):.1f}%")

def proxy_example():
    P = list('ab'); T1 = list('aaaaab'); T2 = list('abxab')
    for T in (T1, T2):
        print(f"   T = {''.join(T)}: matches {len(occs(P, T, 0, 100))}, exact cost sup_NO = {maxflow(P, T, 0, 100)[0]}")

def exp_local_multi(seed=2, trials=1500):
    rng = random.Random(seed); stats = {'MM': [], 'LP': []}; lpgap = []
    for _ in range(trials):
        T = [rng.choice('abcd') for _ in range(rng.randint(6, 14))]
        Ps = [rng.sample('abcd', rng.randint(2, 3)) for _ in range(rng.randint(2, 4))]
        gmax = rng.choice([1, 2, 3, 100])
        if not any(occs(P, T, 0, gmax) for P in Ps): continue
        opt, _ = exact_hit(Ps, T, 0, gmax)
        mm = len(most_matches(Ps, T, 0, gmax)); lr, lpv = lp_round(Ps, T, 0, gmax)
        stats['MM'].append(mm / opt); stats['LP'].append(lr / opt); lpgap.append(opt / lpv)
    for k, v in stats.items():
        v = np.array(v)
        print(f"[local, several patterns] {k}: ratio to optimum mean {v.mean():.3f}, max {v.max():.2f}, optimal in {100*np.mean(v < 1 + 1e-9):.1f}% of {len(v)}")
    print(f"   integrality gap OPT/LP: mean {np.mean(lpgap):.3f}, max {max(lpgap):.2f}")

if __name__ == '__main__':
    exp_global(); proxy_example(); exp_local_multi()
