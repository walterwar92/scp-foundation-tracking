#!/usr/bin/env bash
set -euo pipefail

if [ ! -d lib ]; then
    echo "ERROR: запустите ./setup.sh сначала"
    exit 1
fi

echo "[build] Очистка out/"
rm -rf out
mkdir -p out

CP="lib/*"

echo "[build] Компиляция..."
find src -name "*.java" > sources.txt
javac -d out -cp "$CP" @sources.txt
rm sources.txt

echo "[build] Копирование ресурсов..."
cp -r resources/* out/

echo "[build] ✓ Готово. Запустите: ./run.sh"
