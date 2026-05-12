#!/usr/bin/env bash
set -euo pipefail

# === SCP Foundation — setup ===
# Скачивает JDBC и JavaFX JAR-ы в lib/, копирует config.example.properties.

echo "[setup] Проверка Java..."
if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: java не найдена. Установите JDK 17+ и убедитесь, что 'java' в PATH."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F[\".] 'NR==1 {print $2}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "ERROR: требуется Java 17+, найдена $JAVA_VERSION"
    exit 1
fi
echo "[setup] Java $JAVA_VERSION OK"

# Определение ОС для JavaFX classifier
OS=$(uname -s)
case "$OS" in
    Linux*)   FX_OS="linux" ;;
    Darwin*)  FX_OS="mac" ;;
    *)        echo "ERROR: неподдерживаемая ОС $OS"; exit 1 ;;
esac
echo "[setup] Platform: $FX_OS"

mkdir -p lib

MAVEN="https://repo.maven.apache.org/maven2"

declare -a JARS=(
    "$MAVEN/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar"
    "$MAVEN/org/firebirdsql/jdbc/jaybird/5.0.4.java11/jaybird-5.0.4.java11.jar"
    "$MAVEN/org/openjfx/javafx-base/21.0.2/javafx-base-21.0.2-$FX_OS.jar"
    "$MAVEN/org/openjfx/javafx-graphics/21.0.2/javafx-graphics-21.0.2-$FX_OS.jar"
    "$MAVEN/org/openjfx/javafx-controls/21.0.2/javafx-controls-21.0.2-$FX_OS.jar"
    "$MAVEN/org/openjfx/javafx-fxml/21.0.2/javafx-fxml-21.0.2-$FX_OS.jar"
)

for url in "${JARS[@]}"; do
    file="lib/$(basename "$url")"
    if [ -f "$file" ]; then
        echo "[setup] Уже есть: $(basename "$url")"
    else
        echo "[setup] Загрузка: $(basename "$url")"
        curl -fSL -o "$file" "$url"
    fi
done

if [ ! -f config.properties ]; then
    cp config.example.properties config.properties
    echo "[setup] Создан config.properties — отредактируйте его перед запуском."
fi

echo "[setup] ✓ Готово. Запустите: ./build.sh && ./run.sh"
