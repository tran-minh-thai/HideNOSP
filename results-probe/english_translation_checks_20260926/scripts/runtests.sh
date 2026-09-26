#!/bin/bash
# Run every JUnit test of a build made by build.sh, from the checkout root. Usage: runtests.sh <checkout> <outdir>
set -uo pipefail
SRC="$1"; OUT="$2"
JDK=<jdk>
M2=$HOME/.m2/repository
CP="$M2/org/junit/jupiter/junit-jupiter-api/5.10.2/junit-jupiter-api-5.10.2.jar:$M2/org/junit/jupiter/junit-jupiter-engine/5.10.2/junit-jupiter-engine-5.10.2.jar:$M2/org/junit/jupiter/junit-jupiter-params/5.10.2/junit-jupiter-params-5.10.2.jar:$M2/org/junit/platform/junit-platform-commons/1.10.2/junit-platform-commons-1.10.2.jar:$M2/org/junit/platform/junit-platform-engine/1.10.2/junit-platform-engine-1.10.2.jar:$M2/org/junit/platform/junit-platform-launcher/1.10.2/junit-platform-launcher-1.10.2.jar:$M2/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar:$M2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"
H=<checks>/harness
mkdir -p "$H/launcher"
"$JDK/bin/javac" -nowarn -d "$H/launcher" -cp "$CP" "$H/RunAll.java"
cd "$SRC" && "$JDK/bin/java" -Xmx6g -cp "$OUT/main:$OUT/test:$H/launcher:$CP" RunAll
