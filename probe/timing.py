# SPDX-License-Identifier: MIT
import sys, random, time
sys.path.insert(0, '..')
from nettree_checks import maxflow
def count_matches(P, T, g):   # Abul Lemma 3/4 style DP: matches of prefix k ending at j, O(n*m*w)
    n, m = len(T), len(P)
    Q = [[0]*n for _ in range(m)]
    for j in range(n):
        if T[j] == P[0]: Q[0][j] = 1
    for k in range(1, m):
        for j in range(n):
            if T[j] == P[k]:
                Q[k][j] = sum(Q[k-1][l] for l in range(max(0, j-1-g), j))
    return sum(Q[m-1])
def abul_local(P, T, g):
    T = list(T); marks = 0; ops = 0
    while True:
        tot = count_matches(P, T, g)
        if tot == 0: return marks
        best, bi = -1, None
        for i in range(len(T)):          # delta(i) = |M_T| - |M_{T with i marked}| (one DP per position)
            if T[i] not in P: continue
            c = T[i]; T[i] = '#'; d = tot - count_matches(P, T, g); T[i] = c
            if d > best: best, bi = d, i
        T[bi] = '#'; marks += 1
def fwd_bwd_local(P, T, g):          # same heuristic, delta for all positions in one forward/backward pass
    T = list(T); marks = 0; n, m = len(T), len(P)
    while True:
        F = [[0]*n for _ in range(m)]; B = [[0]*n for _ in range(m)]
        for j in range(n):
            if T[j] == P[0]: F[0][j] = 1
            if T[j] == P[m-1]: B[m-1][j] = 1
        for k in range(1, m):
            for j in range(n):
                if T[j] == P[k]: F[k][j] = sum(F[k-1][l] for l in range(max(0, j-1-g), j))
        for k in range(m-2, -1, -1):
            for j in range(n):
                if T[j] == P[k]: B[k][j] = sum(B[k+1][l] for l in range(j+1, min(n, j+2+g)))
        tot = sum(F[m-1])
        if tot == 0: return marks
        d = [sum(F[k][j]*B[k][j] for k in range(m)) for j in range(n)]
        bi = max(range(n), key=lambda j: d[j]); T[bi] = '#'; marks += 1
rng = random.Random(1); P = list('abc'); g = 3
print("n   | Abul DP-per-position | forward/backward | max flow (exact) | marks A/FB/flow")
for n in (50, 100, 200, 400):
    T = [rng.choice('abcd') for _ in range(n)]
    t0 = time.time(); a = abul_local(P, T, g); t1 = time.time()
    b = fwd_bwd_local(P, T, g); t2 = time.time()
    f = maxflow(P, T, 0, g)[0]; t3 = time.time()
    print(f"{n:4d}| {t1-t0:9.2f} s | {t2-t1:9.3f} s | {t3-t2:9.3f} s | {a}/{b}/{f}")
