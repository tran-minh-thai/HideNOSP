#!/bin/bash
# Compile main and test sources of a checkout with javac directly (Maven cannot resolve its
# compiler plugin offline on this machine). Usage: build.sh <checkout> <outdir> <debug flag>
set -euo pipefail
SRC="$1"; OUT="$2"; DBG="$3"
JDK=<jdk>
M2=$HOME/.m2/repository
CP_JUNIT="$M2/org/junit/jupiter/junit-jupiter-api/5.10.2/junit-jupiter-api-5.10.2.jar:$M2/org/junit/jupiter/junit-jupiter-params/5.10.2/junit-jupiter-params-5.10.2.jar:$M2/org/junit/platform/junit-platform-commons/1.10.2/junit-platform-commons-1.10.2.jar:$M2/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar:$M2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"
rm -rf "$OUT"; mkdir -p "$OUT/main" "$OUT/test"
find "$SRC/src/main/java" -name '*.java' | LC_ALL=C sort > "$OUT/main.list"
find "$SRC/src/test/java" -name '*.java' | LC_ALL=C sort > "$OUT/test.list"
"$JDK/bin/javac" --release 25 -encoding UTF-8 $DBG -nowarn -d "$OUT/main" @"$OUT/main.list"
"$JDK/bin/javac" --release 25 -encoding UTF-8 $DBG -nowarn -d "$OUT/test" -cp "$OUT/main:$CP_JUNIT" @"$OUT/test.list"
echo "compiled $(wc -l < "$OUT/main.list") main and $(wc -l < "$OUT/test.list") test sources with $DBG into $OUT"
