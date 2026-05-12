#!/usr/bin/env bash
# ============================================================
# SCP Foundation — one-click startup
#
# Step 1: download Java deps (lib/) if missing
# Step 2: download + init portable Postgres/Firebird (db-runtime/)
#         and start them on ports 5433/3051
# Step 3: compile Java sources to out/
# Step 4: launch the JavaFX app
#
# Every step is idempotent: re-running this script after the first
# successful run is fast (skips downloads, skips DB init/seed).
# ============================================================

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

echo "=== SCP Foundation: full startup ==="

# ----- Step 1: Java deps -----
if [ ! -f "lib/postgresql-42.7.3.jar" ]; then
    echo "[start] Step 1/4: downloading Java dependencies..."
    bash ./setup.sh
else
    echo "[start] Step 1/4: Java deps already present — skipping setup.sh"
fi

# ----- Step 2: DB install + start -----
echo "[start] Step 2/4: portable DB install + start..."
bash ./db-setup.sh

# ----- Step 3: compile -----
echo "[start] Step 3/4: compiling Java..."
bash ./build.sh

# ----- Step 4: run -----
echo "[start] Step 4/4: launching application..."
exec bash ./run.sh
