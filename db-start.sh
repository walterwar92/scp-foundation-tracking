#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

DB="db-runtime"
PG_DIR="$DB/postgres"
PG_DATA="$PG_DIR/data"
PG_PORT=5433
FB_DIR="$DB/firebird"
FB_PORT=3051

if [ ! -x "$PG_DIR/bin/postgres" ]; then
    echo "ERROR: Postgres не установлен. Запустите ./db-setup.sh"
    exit 1
fi

if "$PG_DIR/bin/pg_ctl" status -D "$PG_DATA" >/dev/null 2>&1; then
    echo "[db-start] PostgreSQL уже запущен"
else
    echo "[db-start] PostgreSQL на порту $PG_PORT..."
    "$PG_DIR/bin/pg_ctl" start -D "$PG_DATA" -l "$PG_DIR/log.txt" -w -o "-p $PG_PORT -h localhost"
fi

if [ -x "$FB_DIR/bin/firebird" ]; then
    PID_FILE="$DB/pids/firebird.pid"
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo "[db-start] Firebird уже запущен"
    else
        echo "[db-start] Firebird на порту $FB_PORT..."
        mkdir -p "$DB/pids"
        FIREBIRD="$ROOT/$FB_DIR" LD_LIBRARY_PATH="$ROOT/$FB_DIR/lib:${LD_LIBRARY_PATH:-}" \
            nohup "$ROOT/$FB_DIR/bin/firebird" -m > "$FB_DIR/firebird.log" 2>&1 &
        echo $! > "$PID_FILE"
        sleep 2
    fi
else
    echo "[db-start] Firebird не установлен (только Postgres будет работать)"
fi

echo "[db-start] Готово"
