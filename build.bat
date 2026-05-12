@echo off
setlocal enabledelayedexpansion

if not exist lib (
    echo ERROR: запустите setup.bat сначала
    exit /b 1
)

echo [build] Очистка out\
if exist out rmdir /s /q out
mkdir out

set "CP=lib\*"

echo [build] Компиляция...
dir /s /b src\*.java > sources.txt
javac -d out -cp "%CP%" @sources.txt
del sources.txt

echo [build] Копирование ресурсов...
xcopy /s /e /y /q resources\* out\ >nul

echo [build] Готово. Запустите: run.bat
