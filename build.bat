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
rem PowerShell записывает список исходников в UTF-8 без BOM, с относительными
rem путями в кавычках — argfile с абсолютными путями ломает javac, если в пути
rem есть пробел или не-ASCII символы (например, кириллица в %USERPROFILE%).
powershell -NoProfile -Command "$files = (Get-ChildItem -Recurse -Path src -Filter *.java | Resolve-Path -Relative | ForEach-Object { '\"' + ($_ -replace '\\','/') + '\"' }) -join [Environment]::NewLine; [System.IO.File]::WriteAllText((Join-Path (Get-Location) 'sources.txt'), $files, (New-Object System.Text.UTF8Encoding $false))"
javac -encoding UTF-8 -d out -cp "%CP%" @sources.txt
if errorlevel 1 (
    del sources.txt
    echo ERROR: компиляция упала
    exit /b 1
)
del sources.txt

echo [build] Копирование ресурсов...
xcopy /s /e /y /q resources\* out\ >nul

echo [build] Готово. Запустите: run.bat
