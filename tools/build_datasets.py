# SPDX-License-Identifier: MIT
"""Build the SPMF files that the Java code reads, from the raw inputs under datasets/raw/.

Each raw input becomes single-item event sequences by a fixed rule, written to
datasets/<name>.txt in the SPMF format of io.DatasetReader: item codes separated by -1, -2 closing
a sequence, and one "@ITEM=code=label" line per item so that the file states what each code means.

The rules assign a role to every field of the raw data. They are design decisions, and the paper
has to state them:

  Event logs (bpi2012, bpi2013, sepsis). One trace is one sequence: one case, i.e. one loan
  application, one incident or one patient. Its events are sorted by time:timestamp; events that
  share a timestamp keep the order of the log file. The item is the activity:
    bpi2012  concept:name of the events whose lifecycle:transition is COMPLETE
    bpi2013  concept:name + "/" + lifecycle:transition (status and sub-status)
    sepsis   concept:name (every event of this log is "complete")
  Item codes go to the sorted item labels: 0, 1, 2, ...

  HAUP-Hider databases (haup_sdb1 .. haup_sdb14). One line is one sequence, "-1" closes an itemset.
  When every item token is a non-negative integer the tokens keep their value as item code, so the
  authors' utility files still apply; otherwise the sorted distinct tokens get codes 0, 1, 2, ...

  Protein datasets of NTP-Miner (protein_sdb1, protein_sdb4). One line is one protein sequence and
  one residue letter is one item; blanks carry no residue and are skipped. Any other character is an
  error, not a residue. The sorted distinct letters get codes 0, 1, 2, ... The six published files
  are nested (SDB3 in SDB2 in SDB1, SDB6 in SDB5 in SDB4), so only SDB1 and SDB4 are built, and those
  two share most of their sequences too. Every protein build prints its exact-match overlap with the
  six raw files, because the overlap decides how many datasets the protein data count as.

For every event log the share of events whose place among equal timestamps was decided by the file
order is printed, because it has to be declared next to the dataset.

Provenance. The output is recorded in datasets/provenance.tsv like a download, with the build rule
in place of the URL. The rules are deterministic, so an output that differs from its record is an
error (exit code 1): the input or this code has changed, and the numbers of the paper no longer
describe this file. The raw input itself is checked against its own record first.

Run: python tools/build_datasets.py [name ...]
With no argument it builds every dataset listed in BUILDS.
"""
import datetime
import gzip
import hashlib
import io
import os
import sys
import time
import xml.etree.ElementTree as ElementTree

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import fetch_datasets  # noqa: E402  (same directory; provides the provenance table)

DATASETS = fetch_datasets.DATASETS

# name -> (raw catalogue name, kind, rule, role)
BUILDS = {
    "bpi2012": ("bpi2012_raw", "xes", "activity of COMPLETE events",
                "event log: one loan application per sequence (BPI Challenge 2012)"),
    "bpi2013": ("bpi2013_raw", "xes", "status/sub-status of every event",
                "event log: one IT incident per sequence (BPI Challenge 2013 incidents)"),
    "sepsis": ("sepsis_raw", "xes", "activity of every event",
               "event log: one hospital patient per sequence (Sepsis Cases)"),
}
for _k in fetch_datasets.HAUP_SIZES:
    BUILDS["haup_sdb%d" % _k] = ("haup_sdb%d_raw" % _k, "tokens", "one line per sequence",
                                 "database SDB%d of HAUP-Hider, the closest hiding method" % _k)
for _k in fetch_datasets.NTP_BUILT:
    BUILDS["protein_sdb%d" % _k] = ("ntp_sdb%d_raw" % _k, "letters",
                                    "one line per protein, one residue letter per item",
                                    "protein dataset SDB%d of NTP-Miner (NOSEP line of work)" % _k)


def local_name(tag):
    """The tag without its XML namespace."""
    return tag.split("}", 1)[-1]


def read_xes(path, rule):
    """Return (sequences as label lists, number of events kept, events ordered by the file)."""
    sequences = []
    kept = 0
    tie_ordered = 0
    with gzip.open(path, "rb") as handle:
        for _, element in ElementTree.iterparse(handle, events=("end",)):
            if local_name(element.tag) != "trace":
                continue
            events = []
            for order, child in enumerate(element):
                if local_name(child.tag) != "event":
                    continue
                attributes = {a.get("key"): a.get("value") for a in child}
                label = event_label(attributes, rule)
                if label is None:
                    continue
                stamp = attributes.get("time:timestamp")
                if stamp is None:
                    raise ValueError("an event without time:timestamp in " + path)
                moment = datetime.datetime.fromisoformat(stamp)
                if moment.tzinfo is None:
                    moment = moment.replace(tzinfo=datetime.timezone.utc)
                events.append((moment.timestamp(), order, label))
            element.clear()
            events.sort(key=lambda e: (e[0], e[1]))
            for i in range(1, len(events)):
                if events[i][0] == events[i - 1][0]:
                    tie_ordered += 1
            kept += len(events)
            sequences.append([e[2] for e in events])
    return sequences, kept, tie_ordered


def event_label(attributes, rule):
    """The item of one event under the rule, or None when the rule drops the event."""
    name = attributes.get("concept:name")
    if name is None:
        raise ValueError("an event without concept:name")
    transition = attributes.get("lifecycle:transition")
    if rule == "activity of COMPLETE events":
        return name if transition == "COMPLETE" else None
    if rule == "status/sub-status of every event":
        if transition is None:
            raise ValueError("an event without lifecycle:transition")
        return name + "/" + transition
    if rule == "activity of every event":
        return name
    raise ValueError("unknown rule " + rule)


def read_tokens(path):
    """Return the sequences of a -1 separated token file, one list of itemsets per line."""
    sequences = []
    with io.open(path, encoding="utf-8") as handle:
        for line in handle:
            tokens = line.split()
            if not tokens:
                continue
            itemsets, current = [], []
            for token in tokens:
                if token == "-2":
                    break
                if token == "-1":
                    if current:
                        itemsets.append(current)
                    current = []
                else:
                    current.append(token)
            if current:
                itemsets.append(current)
            if itemsets:
                sequences.append(itemsets)
    return sequences


def read_letters(path):
    """Return the sequences of a letter file, one sequence per line, one letter per item."""
    sequences = []
    with io.open(path, encoding="ascii") as handle:
        for number, line in enumerate(handle, 1):
            residues = "".join(line.split())
            if not residues:
                continue
            if not residues.isalpha():
                bad = sorted({c for c in residues if not c.isalpha()})
                raise ValueError("%s line %d holds characters that are not residue letters: %r"
                                 % (path, number, bad))
            sequences.append([[letter] for letter in residues])
    return sequences


def print_overlap(name, itemset_sequences, rows):
    """Print how many sequences this protein dataset shares with each raw NTP-Miner file."""
    own = {"".join(itemset[0] for itemset in seq) for seq in itemset_sequences}
    for k in sorted(fetch_datasets.NTP_SIZES):
        raw_name = "ntp_sdb%d_raw" % k
        path = os.path.join(DATASETS, fetch_datasets.CATALOGUE[raw_name][1])
        if raw_name not in rows or not os.path.exists(path) \
                or fetch_datasets.sha256_of(path) != rows[raw_name][6]:
            print("  %-10s overlap with SDB%d not measured: the raw file is missing or differs from "
                  "its record" % (name, k))
            continue
        other = {"".join(itemset[0] for itemset in seq) for seq in read_letters(path)}
        shared = len(own & other)
        print("  %-10s shares %d sequences with SDB%d: %.2f of its %d, %.2f of the %d of SDB%d"
              % (name, shared, k, shared / len(own), len(own), shared / len(other), len(other), k))


def codes_for(labels):
    """Item codes: integer labels keep their value when all are integers, else sorted order."""
    distinct = sorted(set(labels))
    if all(label.isdigit() for label in distinct):
        return {label: int(label) for label in distinct}, True
    return {label: code for code, label in enumerate(distinct)}, False


def render(itemset_sequences, code, labelled):
    """SPMF text: optional label lines, then one line per sequence."""
    out = io.StringIO()
    if labelled:
        out.write("@CONVERTED_FROM_TEXT\n")
        for label in sorted(code, key=code.get):
            out.write("@ITEM=%d=%s\n" % (code[label], label))
    for itemsets in itemset_sequences:
        parts = []
        for itemset in itemsets:
            parts.append(" ".join(str(code[token]) for token in itemset) + " -1")
        out.write(" ".join(parts) + " -2\n")
    return out.getvalue().encode("utf-8")


def build(name, rows):
    raw_name, kind, rule, role = BUILDS[name]
    url, raw_file = fetch_datasets.CATALOGUE[raw_name][0], fetch_datasets.CATALOGUE[raw_name][1]
    raw_path = os.path.join(DATASETS, raw_file)
    if raw_name not in rows:
        print("  ERROR %s: %s has no provenance record; run tools/fetch_datasets.py %s first"
              % (name, raw_name, raw_name))
        return "error"
    if not os.path.exists(raw_path) or fetch_datasets.sha256_of(raw_path) != rows[raw_name][6]:
        print("  ERROR %s: %s is missing or differs from its record; run tools/fetch_datasets.py %s"
              % (name, raw_path, raw_name))
        return "error"

    if kind == "xes":
        label_sequences, kept, tie_ordered = read_xes(raw_path, rule)
        itemset_sequences = [[[label] for label in seq] for seq in label_sequences if seq]
        dropped = len(label_sequences) - len(itemset_sequences)
        tie_note = "%d of %d events (%.4f) ordered by the file among equal timestamps" % (
            tie_ordered, kept, tie_ordered / kept if kept else 0.0)
    elif kind == "letters":
        itemset_sequences = read_letters(raw_path)
        dropped = 0
        tie_note = "no timestamps"
        print_overlap(name, itemset_sequences, rows)
    else:
        itemset_sequences = read_tokens(raw_path)
        dropped = 0
        tie_note = "no timestamps"
    all_labels = [token for seq in itemset_sequences for itemset in seq for token in itemset]
    code, numeric = codes_for(all_labels)
    data = render(itemset_sequences, code, labelled=not numeric)
    multi = sum(1 for seq in itemset_sequences for itemset in seq if len(itemset) > 1)
    lengths = [sum(len(itemset) for itemset in seq) for seq in itemset_sequences]
    print("  %-10s %d sequences, %d events, |items| = %d, mean length %.1f, max %d, "
          "%d multi-item itemsets, %d empty sequences dropped; %s"
          % (name, len(lengths), sum(lengths), len(code), sum(lengths) / max(1, len(lengths)),
             max(lengths) if lengths else 0, multi, dropped, tie_note))

    target = os.path.join(DATASETS, name + ".txt")
    digest = hashlib.sha256(data).hexdigest()
    if name in rows:
        if digest != rows[name][6]:
            with open(target + ".mismatch", "wb") as handle:
                handle.write(data)
            print("  ERROR %s: the build does not match datasets/provenance.tsv (recorded %s, "
                  "built %s); kept aside as %s.mismatch" % (name, rows[name][6], digest, target))
            return "mismatch"
        with open(target, "wb") as handle:
            handle.write(data)
        print("             sha256 matches the record")
        return "built"
    with open(target, "wb") as handle:
        handle.write(data)
    source = "built by tools/build_datasets.py from %s (%s); %s" % (raw_name, rule, url)
    rows[name] = [name, name + ".txt", "SPMF", source, time.strftime("%Y-%m-%d"), str(len(data)),
                  digest, role]
    print("             sha256 %s..., NEW record" % digest[:16])
    return "recorded"


def main():
    names = sys.argv[1:] or list(BUILDS)
    unknown = [n for n in names if n not in BUILDS]
    if unknown:
        print("unknown dataset: " + ", ".join(unknown))
        print("known: " + ", ".join(BUILDS))
        return 2
    rows = fetch_datasets.read_provenance()
    outcome = {name: build(name, rows) for name in names}
    if "recorded" in outcome.values():
        fetch_datasets.write_provenance(rows)
        print("wrote %s" % fetch_datasets.PROVENANCE)
    bad = [n for n, o in outcome.items() if o in ("error", "mismatch")]
    print("built %d dataset(s), %d failed%s" % (len(names), len(bad),
                                                 (": " + ", ".join(bad)) if bad else ""))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
