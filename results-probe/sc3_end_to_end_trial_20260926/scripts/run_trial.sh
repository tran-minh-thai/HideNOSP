#!/bin/bash
# End-to-end SC3 trial: probe runs through tools/run.py, then the statistics on the new files only.
W="${REPO:?set REPO to the repository checkout}"
SP="${TRIAL:?set TRIAL to a folder for the trial logs}"
: "${JAVA_HOME:?set JAVA_HOME}"
export PATH=$JAVA_HOME/bin:$PATH
export PYTHONDONTWRITEBYTECODE=1
PY="${PYTHON:-python3}"
cd $W || exit 1
OUT=$SP/logs; mkdir -p $OUT; rm -f $OUT/*
ls results-probe/SC3_*.csv 2>/dev/null | sort > $OUT/sc3_before.txt
METHODS=cut-hide-rw-ls,cut-hide-rw,cut-hide,hha,dbsh,sbsh-r1,sbsh-rmax
echo "trial started $(date '+%Y-%m-%d %H:%M:%S') at $(git rev-parse --short HEAD), tree $(git status --porcelain -- src tools config | wc -l | tr -d ' ') changed files" > $OUT/trial.txt
one() {  # name, extra wrapper flags, then scenario arguments
  local name=$1; shift; local wrap=$1; shift
  local t0=$(date +%s)
  $PY tools/run.py --probe --jvm=-Xmx6g $wrap SC3 "$@" > $OUT/$name.log 2>&1
  local rc=$?
  echo "$name exit=$rc $(( $(date +%s) - t0 ))s args: $*" >> $OUT/trial.txt
}
# The classes are compiled beforehand with javac --release 25 (Maven cannot build offline here); every run uses --no-build.
one sepsis_repeated "--no-build" --dataset=datasets/sepsis.txt --format=SPMF --sigma=300 --gmin=0 --gmax=1 --maxPatternLength=4 --setsPerStratum=4 --sensitiveSize=1 --patternKind=repeated --methods=$METHODS
# Parallel runs started one second apart: RunId stamps the start time to the millisecond, and
# four JVMs started together by xargs got the same stamp in an earlier attempt.
while read -r name rest; do
  one "$name" "--no-build" $rest &
  sleep 1
done <<SPECS
dna_distinct --dataset=datasets/dna_mt745584.txt --format=SPMF --sigma=2500 --gmin=0 --gmax=3 --maxPatternLength=4 --setsPerStratum=4 --sensitiveSize=1 --patternKind=distinct --methods=$METHODS
protein_distinct --dataset=datasets/protein_sdb4.txt --format=SPMF --sigma=500 --gmin=0 --gmax=3 --maxPatternLength=4 --setsPerStratum=4 --sensitiveSize=1 --patternKind=distinct --methods=$METHODS
bpi2013_repeated --dataset=datasets/bpi2013.txt --format=SPMF --sigma=500 --gmin=0 --gmax=1 --maxPatternLength=4 --setsPerStratum=4 --sensitiveSize=1 --patternKind=repeated --methods=$METHODS
protein_five_patterns --dataset=datasets/protein_sdb4.txt --format=SPMF --sigma=500 --gmin=0 --gmax=3 --maxPatternLength=4 --setsPerStratum=1 --sensitiveSize=5 --patternKind=distinct --methods=$METHODS
SPECS
wait
echo "trial finished $(date '+%Y-%m-%d %H:%M:%S')" >> $OUT/trial.txt
ls results-probe/SC3_*.csv 2>/dev/null | sort > $OUT/sc3_after.txt
comm -13 $OUT/sc3_before.txt $OUT/sc3_after.txt > $OUT/sc3_new.txt
