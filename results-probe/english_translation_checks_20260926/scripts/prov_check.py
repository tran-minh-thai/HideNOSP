"""Scratch check of fetch_datasets.py against datasets/provenance.tsv in one checkout:
(1) the CATALOGUE role equals the recorded role for every dataset that has both;
(2) write_provenance(read_provenance()) rewrites provenance.tsv byte for byte."""
import os, sys, tempfile
root = sys.argv[1]
sys.path.insert(0, os.path.join(root, "tools"))
import fetch_datasets as f
rows = f.read_provenance()
both = [n for n in f.CATALOGUE if n in rows]
same = [n for n in both if f.CATALOGUE[n][4] == rows[n][7]]
print("datasets in CATALOGUE and provenance.tsv: %d; role identical: %d; differ: %s"
      % (len(both), len(same), [n for n in both if n not in same]))
tmp = os.path.join(tempfile.mkdtemp(), "provenance.tsv")
f.PROVENANCE = tmp
f.write_provenance(rows)
a = open(os.path.join(root, "datasets", "provenance.tsv"), "rb").read()
b = open(tmp, "rb").read()
print("round trip write_provenance(read_provenance()) byte-identical:", a == b, "(%d bytes)" % len(a))
