#!/usr/bin/env bash
# SCP Foundation — портативная установка PostgreSQL + Firebird для Linux/macOS
# Скачивает бинарники, инициализирует БД, применяет schema/constraints/seed.
# Серверы на нестандартных портах 5433 (Postgres) и 3051 (Firebird).

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

DB="db-runtime"
PG_DIR="$DB/postgres"
PG_DATA="$PG_DIR/data"
PG_PORT=5433
PG_PASS="scp-foundation-db"
PG_USER="scp_admin"
PG_DB_NAME="scp_foundation"

FB_DIR="$DB/firebird"
FB_PORT=3051
FB_PASS="masterkey"

OS=$(uname -s)
case "$OS" in
    Linux*)  PG_OS=linux-amd64; FB_OS=linux-x64;   FB_EXT=tar.gz ;;
    Darwin*) PG_OS=darwin-amd64; FB_OS=macos-arm64; FB_EXT=pkg ;;
    *) echo "ERROR: неподдерживаемая ОС $OS"; exit 1 ;;
esac

# На macOS Firebird распространяется .pkg инсталлятором, портативной версии нет —
# поэтому для macOS этот скрипт обрабатывает только Postgres. Для Firebird на mac
# рекомендую использовать Docker или установить через Homebrew (brew install firebird).
PG_VERSION="16.4.0"
FB_VERSION="5.0.1.1469-0"

PG_JAR_URL="https://repo.maven.apache.org/maven2/io/zonky/test/postgres/embedded-postgres-binaries-$PG_OS/$PG_VERSION/embedded-postgres-binaries-$PG_OS-$PG_VERSION.jar"
FB_TAR_URL="https://github.com/FirebirdSQL/firebird/releases/download/v5.0.1/Firebird-$FB_VERSION-$FB_OS.tar.gz"

echo "[db-setup] === SCP Foundation portable DB setup ==="
echo "[db-setup] Platform: $OS"

# ============== Проверки ==============
command -v java  >/dev/null 2>&1 || { echo "ERROR: java не найдена. Сначала ./setup.sh"; exit 1; }
command -v curl  >/dev/null 2>&1 || { echo "ERROR: curl не найден"; exit 1; }
command -v tar   >/dev/null 2>&1 || { echo "ERROR: tar не найден"; exit 1; }
command -v unzip >/dev/null 2>&1 || { echo "ERROR: unzip не найден"; exit 1; }
command -v xz    >/dev/null 2>&1 || { echo "ERROR: xz не найден. Установите xz-utils (apt install xz-utils / brew install xz)"; exit 1; }

[ -d lib ] || { echo "ERROR: lib/ не существует. Сначала ./setup.sh"; exit 1; }

mkdir -p "$DB" "$DB/pids"

# ============== 1. Скачиваем portable PostgreSQL ==============
if [ -x "$PG_DIR/bin/postgres" ]; then
    echo "[db-setup] PostgreSQL уже распакован"
else
    if [ ! -f "$DB/pg.jar" ]; then
        echo "[db-setup] Загрузка PostgreSQL (zonky, ~25MB)..."
        curl -fSL -o "$DB/pg.jar" "$PG_JAR_URL"
    fi
    echo "[db-setup] Извлечение TXZ из JAR..."
    rm -rf "$DB/pg-jar"
    mkdir -p "$DB/pg-jar"
    unzip -q "$DB/pg.jar" -d "$DB/pg-jar"
    mkdir -p "$PG_DIR"
    TXZ=$(ls "$DB/pg-jar"/*.txz 2>/dev/null | head -1)
    [ -n "$TXZ" ] || { echo "ERROR: не нашёл TXZ внутри JAR"; exit 1; }
    echo "[db-setup] Распаковка TXZ $(basename "$TXZ")..."
    tar -xf "$TXZ" -C "$PG_DIR"
    rm -rf "$DB/pg-jar" "$DB/pg.jar"
    chmod +x "$PG_DIR"/bin/* 2>/dev/null || true
fi

# ============== 2. initdb ==============
if [ -f "$PG_DATA/PG_VERSION" ]; then
    echo "[db-setup] Postgres кластер уже инициализирован"
else
    echo "[db-setup] initdb..."
    PWFILE="$DB/pg-pwfile.txt"
    printf '%s' "$PG_PASS" > "$PWFILE"
    "$PG_DIR/bin/initdb" -D "$PG_DATA" -U postgres --pwfile="$PWFILE" -E UTF8 --locale=C --auth=md5
    rm -f "$PWFILE"
    cat >> "$PG_DATA/postgresql.conf" <<EOF

port = $PG_PORT
listen_addresses = 'localhost'
EOF
fi

# ============== 3. Старт Postgres ==============
if "$PG_DIR/bin/pg_ctl" status -D "$PG_DATA" >/dev/null 2>&1; then
    echo "[db-setup] PostgreSQL уже запущен"
else
    echo "[db-setup] Старт PostgreSQL на порту $PG_PORT..."
    "$PG_DIR/bin/pg_ctl" start -D "$PG_DATA" -l "$PG_DIR/log.txt" -w -o "-p $PG_PORT -h localhost"
fi

# ============== 4. Компилируем SqlRunner ==============
if [ ! -f out/ru/scp/foundation/util/SqlRunner.class ]; then
    echo "[db-setup] Компилирую SqlRunner..."
    mkdir -p out
    javac -encoding UTF-8 -d out src/ru/scp/foundation/util/SqlRunner.java
fi

invoke_sql() {
    local driver="$1" url="$2" user="$3" pass="$4" mode="$5" arg="$6" ignore_errors="${7:-false}"
    local args=( -cp "out:lib/*" ru.scp.foundation.util.SqlRunner "$driver" "$url" "$user" "$pass" "$mode" "$arg" )
    if [ "$ignore_errors" = "true" ]; then
        args+=( --ignore-errors )
    fi
    java "${args[@]}"
}

# ============== 5. CREATE ROLE + CREATE DATABASE + apply SQL (idempotent) ==============
PG_MARKER="$DB/.postgres-seeded"
if [ -f "$PG_MARKER" ]; then
    echo "[db-setup] PostgreSQL уже инициализирован — пропускаю SQL"
else
    echo "[db-setup] Создание роли и БД..."
    invoke_sql org.postgresql.Driver \
        "jdbc:postgresql://localhost:$PG_PORT/postgres" \
        postgres "$PG_PASS" \
        -c "CREATE ROLE $PG_USER LOGIN PASSWORD '$PG_PASS' CREATEDB" true

    invoke_sql org.postgresql.Driver \
        "jdbc:postgresql://localhost:$PG_PORT/postgres" \
        postgres "$PG_PASS" \
        -c "CREATE DATABASE $PG_DB_NAME OWNER $PG_USER ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C' TEMPLATE template0" true

    echo "[db-setup] Применение schema/constraints/seed (Postgres)..."
    for f in 01_schema.sql 02_constraints.sql 03_seed.sql; do
        echo "  sql/postgres/$f"
        invoke_sql org.postgresql.Driver \
            "jdbc:postgresql://localhost:$PG_PORT/$PG_DB_NAME" \
            "$PG_USER" "$PG_PASS" \
            -f "sql/postgres/$f" false
    done
    date -u +%FT%TZ > "$PG_MARKER"
fi

# ============== 7. Firebird (только Linux; macOS — см. примечание выше) ==============
if [ "$OS" = "Darwin" ]; then
    echo "[db-setup] Firebird пропущен на macOS — установите через 'brew install firebird' и применяйте SQL вручную"
else
    if [ -x "$FB_DIR/bin/firebird" ]; then
        echo "[db-setup] Firebird уже распакован"
    else
        if [ ! -f "$DB/fb.tar.gz" ]; then
            echo "[db-setup] Загрузка Firebird (~15MB)..."
            curl -fSL -o "$DB/fb.tar.gz" "$FB_TAR_URL"
        fi
        echo "[db-setup] Распаковка внешнего tar.gz..."
        rm -rf "$DB/fb-outer"
        mkdir -p "$DB/fb-outer"
        tar -xzf "$DB/fb.tar.gz" -C "$DB/fb-outer"
        BUILDROOT=$(find "$DB/fb-outer" -name "buildroot.tar.gz" | head -1)
        [ -n "$BUILDROOT" ] || { echo "ERROR: buildroot.tar.gz не найден"; exit 1; }
        echo "[db-setup] Распаковка buildroot.tar.gz..."
        rm -rf "$DB/fb-buildroot"
        mkdir -p "$DB/fb-buildroot"
        tar -xzf "$BUILDROOT" -C "$DB/fb-buildroot"
        # внутри: opt/firebird/...
        mkdir -p "$FB_DIR"
        cp -R "$DB/fb-buildroot/opt/firebird/." "$FB_DIR/"
        chmod +x "$FB_DIR"/bin/* 2>/dev/null || true
        rm -rf "$DB/fb-outer" "$DB/fb-buildroot" "$DB/fb.tar.gz"

        # настройка firebird.conf
        FBCONF="$FB_DIR/firebird.conf"
        if [ -f "$FBCONF" ]; then
            sed -i.bak \
                -e "s|^#\\?RemoteServicePort.*|RemoteServicePort = $FB_PORT|" \
                -e "s|^#\\?WireCrypt.*|WireCrypt = Enabled|" \
                -e "s|^#\\?AuthServer.*|AuthServer = Srp, Legacy_Auth|" \
                -e "s|^#\\?UserManager.*|UserManager = Srp, Legacy_UserManager|" \
                "$FBCONF"
            rm -f "$FBCONF.bak"
        fi
    fi

    mkdir -p "$FB_DIR/databases"

    # ASCII-алиас 'scp' в databases.conf — путь может содержать не-ASCII,
    # Jaybird падает на транслитерации Cyrillic в JDBC URL.
    FB_DB_CONF="$FB_DIR/databases.conf"
    FB_ALIAS_TARGET="$ROOT/$FB_DIR/databases/scp_foundation.fdb"
    ALIAS_RELOAD_MARKER="$DB/.firebird-alias-applied"
    ALIAS_JUST_ADDED=0
    if ! grep -q "^scp\s*=" "$FB_DB_CONF" 2>/dev/null; then
        {
            echo ""
            echo "# SCP Foundation portable alias"
            echo "scp = $FB_ALIAS_TARGET"
        } >> "$FB_DB_CONF"
        echo "[db-setup] Алиас 'scp' добавлен в databases.conf"
        ALIAS_JUST_ADDED=1
    fi
    # Если алиас только что добавили или маркер потерян — перезагружаем сервер
    if [ "$ALIAS_JUST_ADDED" = "1" ] || [ ! -f "$ALIAS_RELOAD_MARKER" ]; then
        if [ -f "$DB/pids/firebird.pid" ] && kill -0 "$(cat "$DB/pids/firebird.pid")" 2>/dev/null; then
            echo "[db-setup] Перезагрузка Firebird для применения алиаса..."
            kill "$(cat "$DB/pids/firebird.pid")" 2>/dev/null || true
            sleep 2
            rm -f "$DB/pids/firebird.pid"
        fi
        date -u +%FT%TZ > "$ALIAS_RELOAD_MARKER"
    fi

    # Bootstrap SYSDBA в security database (FB5 ставит её пустой).
    # Делаем ДО старта сервера: isql в embedded-режиме работает с security5.fdb
    # напрямую без TCP-аутентификации.
    SYSDBA_MARKER="$DB/.firebird-sysdba"
    PID_FILE="$DB/pids/firebird.pid"
    if [ ! -f "$SYSDBA_MARKER" ]; then
        # Останавливаем сервер если запущен — иначе security5.fdb заблокирован
        if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
            echo "[db-setup] Остановка Firebird для bootstrap SYSDBA..."
            kill "$(cat "$PID_FILE")" 2>/dev/null || true
            sleep 2
        fi
        ISQL="$FB_DIR/bin/isql"
        SECURITY_DB="$ROOT/$FB_DIR/security5.fdb"
        if [ -x "$ISQL" ] && [ -f "$SECURITY_DB" ]; then
            echo "[db-setup] Bootstrap SYSDBA в $SECURITY_DB (embedded)..."
            BOOTSTRAP_SQL=$(mktemp)
            cat > "$BOOTSTRAP_SQL" <<EOF
CREATE OR ALTER USER SYSDBA PASSWORD 'masterkey' USING PLUGIN Srp;
CREATE OR ALTER USER SYSDBA PASSWORD 'masterkey' USING PLUGIN Legacy_UserManager;
COMMIT;
QUIT;
EOF
            export FIREBIRD="$ROOT/$FB_DIR"
            export LD_LIBRARY_PATH="$ROOT/$FB_DIR/lib:${LD_LIBRARY_PATH:-}"
            export ISC_USER=SYSDBA
            export ISC_PASSWORD="$FB_PASS"
            "$ISQL" -bail -i "$BOOTSTRAP_SQL" "$SECURITY_DB" 2>&1 || true
            rm -f "$BOOTSTRAP_SQL"
            date -u +%FT%TZ > "$SYSDBA_MARKER"
            echo "[db-setup] SYSDBA создан в security5.fdb"
            # Сброс seed-маркера и битого FDB
            if [ -f "$DB/.firebird-seeded" ]; then
                rm -f "$DB/.firebird-seeded"
            fi
            if [ -f "$ROOT/$FB_DIR/databases/scp_foundation.fdb" ]; then
                rm -f "$ROOT/$FB_DIR/databases/scp_foundation.fdb"
            fi
        fi
    fi

    # Старт Firebird
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo "[db-setup] Firebird уже запущен (pid=$(cat "$PID_FILE"))"
    else
        echo "[db-setup] Старт Firebird на порту $FB_PORT..."
        FIREBIRD="$ROOT/$FB_DIR" LD_LIBRARY_PATH="$ROOT/$FB_DIR/lib:${LD_LIBRARY_PATH:-}" \
            nohup "$ROOT/$FB_DIR/bin/firebird" -m > "$FB_DIR/firebird.log" 2>&1 &
        echo $! > "$PID_FILE"
        sleep 3
    fi

    FB_DB_ABS="$ROOT/$FB_DIR/databases/scp_foundation.fdb"
    FB_MARKER="$DB/.firebird-seeded"

    if [ -f "$FB_MARKER" ]; then
        echo "[db-setup] Firebird уже инициализирован — пропускаю SQL"
    else
        if [ -f "$FB_DB_ABS" ]; then
            echo "[db-setup] FDB-файл существует, пропускаю создание"
        else
            echo "[db-setup] Создание Firebird БД через алиас 'scp'..."
            # Алиас 'scp' определён в databases.conf — обходим проблему
            # с не-ASCII в connection string Jaybird-а.
            invoke_sql org.firebirdsql.jdbc.FBDriver \
                "jdbc:firebirdsql://localhost:$FB_PORT/scp?charSet=UTF8&createDatabaseIfNotExist=true" \
                SYSDBA "$FB_PASS" \
                -c 'SELECT 1 FROM RDB$DATABASE' true || true
        fi

        echo "[db-setup] Применение schema/constraints/seed (Firebird)..."
        for f in 01_schema.sql 02_constraints.sql 03_seed.sql; do
            echo "  sql/firebird/$f"
            invoke_sql org.firebirdsql.jdbc.FBDriver \
                "jdbc:firebirdsql://localhost:$FB_PORT/scp?charSet=UTF8" \
                SYSDBA "$FB_PASS" \
                -f "sql/firebird/$f" true || true
        done
        date -u +%FT%TZ > "$FB_MARKER"
    fi
fi

# ============== 8. config.properties ==============
echo "[db-setup] Обновление config.properties..."
{
    echo "# Активный диалект: postgres | firebird"
    echo "db.dialect=postgres"
    echo ""
    echo "# PostgreSQL (портативный, db-runtime/postgres)"
    echo "db.url=jdbc:postgresql://localhost:$PG_PORT/$PG_DB_NAME"
    echo "db.user=$PG_USER"
    echo "db.password=$PG_PASS"
    if [ "$OS" != "Darwin" ]; then
        echo ""
        echo "# Firebird (портативный, db-runtime/firebird) - раскомментируйте и закомментируйте Postgres:"
        echo "# db.dialect=firebird"
        echo "# db.url=jdbc:firebirdsql://localhost:$FB_PORT/scp?charSet=UTF8"
        echo "# db.user=SYSDBA"
        echo "# db.password=$FB_PASS"
    fi
} > config.properties

echo ""
echo "[db-setup] === Готово ==="
echo "PostgreSQL:  localhost:$PG_PORT  user=$PG_USER  db=$PG_DB_NAME"
if [ "$OS" != "Darwin" ]; then
    echo "Firebird:    localhost:$FB_PORT  user=SYSDBA   db=$ROOT/$FB_DIR/databases/scp_foundation.fdb"
fi
echo ""
echo "Чтобы остановить серверы:    ./db-stop.sh"
echo "Чтобы запустить заново:      ./db-start.sh"
echo "Чтобы переключить СУБД:      отредактируйте config.properties"
