@echo off
setlocal enabledelayedexpansion

rem ============================================================
rem SCP Foundation - one-click startup
rem
rem Step 1: download Java deps (lib/) if missing
rem Step 2: download + init portable Postgres/Firebird (db-runtime/)
rem         and start them on ports 5433/3051
rem Step 3: compile Java sources to out/
rem Step 4: launch the JavaFX app
rem
rem Every step is idempotent: re-running this script after the first
rem successful run is fast (skips downloads, skips DB init/seed).
rem ============================================================

echo === SCP Foundation: full startup ===

rem ----- Step 1: Java deps -----
if not exist "lib\postgresql-42.7.3.jar" (
    echo [start] Step 1/4: downloading Java dependencies...
    call "%~dp0setup.bat"
    if errorlevel 1 (
        echo ERROR: setup.bat failed
        exit /b 1
    )
) else (
    echo [start] Step 1/4: Java deps already present, skipping setup.bat
)

rem ----- Step 2: DB install + start -----
echo [start] Step 2/4: portable DB install + start...
call "%~dp0db-setup.bat"
if errorlevel 1 (
    echo ERROR: db-setup.bat failed
    exit /b 1
)

rem ----- Step 3: compile -----
echo [start] Step 3/4: compiling Java...
call "%~dp0build.bat"
if errorlevel 1 (
    echo ERROR: build.bat failed
    exit /b 1
)

rem ----- Step 4: run -----
echo [start] Step 4/4: launching application...
call "%~dp0run.bat"
