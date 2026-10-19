#!/usr/bin/env bash
set -euo pipefail

if [ ! -d out ] || [ ! -f out/ru/scp/foundation/Main.class ]; then
    echo "ERROR: сначала ./build.sh"
    exit 1
fi

if [ ! -f config.properties ]; then
    echo "ERROR: нет config.properties. Скопируйте config.example.properties."
    exit 1
fi

java --module-path lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp "out:lib/*" \
     ru.scp.foundation.Main
