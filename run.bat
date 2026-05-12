@echo off
if not exist out\ru\scp\foundation\Main.class (
    echo ERROR: run build.bat first
    exit /b 1
)
if not exist config.properties (
    echo ERROR: config.properties missing. Copy config.example.properties.
    exit /b 1
)

java --module-path lib ^
     --add-modules javafx.controls,javafx.fxml ^
     -cp "out;lib\*" ^
     ru.scp.foundation.Main
