@echo off
setlocal enabledelayedexpansion

if not exist lib (
    echo ERROR: run setup.bat first
    exit /b 1
)

echo [build] Cleaning out\
if exist out rmdir /s /q out
mkdir out

set "CP=lib\*"

echo [build] Compiling...
rem PowerShell writes a UTF-8 no-BOM argfile with quoted RELATIVE paths.
rem Reason: javac on Java 17+ reads argfiles as UTF-8 but cmd's `dir` writes OEM,
rem and javac splits on whitespace, so absolute paths with spaces in them break.
powershell -NoProfile -Command "$files = (Get-ChildItem -Recurse -Path src -Filter *.java | Resolve-Path -Relative | ForEach-Object { '\"' + ($_ -replace '\\','/') + '\"' }) -join [Environment]::NewLine; [System.IO.File]::WriteAllText((Join-Path (Get-Location) 'sources.txt'), $files, (New-Object System.Text.UTF8Encoding $false))"
javac -encoding UTF-8 -d out -cp "%CP%" @sources.txt
if errorlevel 1 (
    del sources.txt
    echo ERROR: compile failed
    exit /b 1
)
del sources.txt

echo [build] Copying resources...
xcopy /s /e /y /q resources\* out\ >nul

echo [build] Done. Run: run.bat
