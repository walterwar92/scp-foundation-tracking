#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

DB="db-runtime"
PG_DIR="$DB/postgres"
PG_DATA="$PG_DIR/data"

if [ -x "$PG_DIR/bin/pg_ctl" ]; then
    if "$PG_DIR/bin/pg_ctl" status -D "$PG_DATA" >/dev/null 2>&1; then
        echo "[db-stop] Остановка PostgreSQL..."
        "$PG_DIR/bin/pg_ctl" stop -D "$PG_DATA" -m fast >/dev/null
    else
        echo "[db-stop] PostgreSQL не запущен"
    fi
fi

PID_FILE="$DB/pids/firebird.pid"
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo "[db-stop] Остановка Firebird (pid=$PID)..."
        kill "$PID" 2>/dev/null || true
        sleep 1
        kill -9 "$PID" 2>/dev/null || true
    fi
    rm -f "$PID_FILE"
else
    pkill -f "$ROOT/db-runtime/firebird/bin/firebird" 2>/dev/null || true
fi

echo "[db-stop] Готово"
