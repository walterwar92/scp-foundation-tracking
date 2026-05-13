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
    echo "[start] Step 1/5: downloading Java dependencies..."
    bash ./setup.sh
else
    echo "[start] Step 1/5: Java deps already present — skipping setup.sh"
fi

# ----- Step 2: DB install + start -----
echo "[start] Step 2/5: portable DB install + start..."
bash ./db-setup.sh

# ----- Step 3: choose DBMS -----
echo "[start] Step 3/5: choose database"
echo
echo "=== Choose database ==="
echo "  1) PostgreSQL  (default)"
echo "  2) Firebird"
read -p "Selection [1]: " DB_CHOICE
DB_CHOICE=${DB_CHOICE:-1}
if [ "$DB_CHOICE" = "2" ]; then
    FDB_PATH="$ROOT/db-runtime/firebird/databases/scp_foundation.fdb"
    cat > config.properties <<EOF
db.dialect=firebird
db.url=jdbc:firebirdsql://localhost:3051/$FDB_PATH?charSet=UTF8
db.user=SYSDBA
db.password=masterkey
EOF
    echo "[start] Selected: Firebird"
else
    cat > config.properties <<EOF
db.dialect=postgres
db.url=jdbc:postgresql://localhost:5433/scp_foundation
db.user=scp_admin
db.password=scp-foundation-db
EOF
    echo "[start] Selected: PostgreSQL"
fi

# ----- Step 4: compile -----
echo "[start] Step 4/5: compiling Java..."
bash ./build.sh

# ----- Step 5: run -----
echo "[start] Step 5/5: launching application..."
exec bash ./run.sh
