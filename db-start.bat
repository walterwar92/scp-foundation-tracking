@echo off
setlocal enabledelayedexpansion

set "ROOT=%CD%"
set "DB=db-runtime"
set "PG_DIR=%DB%\postgres"
set "PG_DATA=%PG_DIR%\data"
set "PG_PORT=5433"
set "FB_DIR=%DB%\firebird"
set "FB_PORT=3051"

if not exist "%PG_DIR%\bin\postgres.exe" (
    echo ERROR: Postgres не установлен. Запустите db-setup.bat
    exit /b 1
)

"%PG_DIR%\bin\pg_ctl.exe" status -D "%PG_DATA%" >nul 2>&1
if errorlevel 1 (
    echo [db-start] PostgreSQL на порту %PG_PORT%...
    "%PG_DIR%\bin\pg_ctl.exe" start -D "%PG_DATA%" -l "%PG_DIR%\log.txt" -w -o "-p %PG_PORT% -h localhost" || exit /b 1
) else (
    echo [db-start] PostgreSQL уже запущен
)

if not exist "%FB_DIR%\firebird.exe" (
    echo ERROR: Firebird не установлен. Запустите db-setup.bat
    exit /b 1
)

tasklist /FI "IMAGENAME eq firebird.exe" 2>nul | find /I "firebird.exe" >nul
if errorlevel 1 (
    echo [db-start] Firebird на порту %FB_PORT%...
    cd /d "%FB_DIR%"
    start "FirebirdSCP" /B firebird.exe -m
    cd /d "%ROOT%"
) else (
    echo [db-start] Firebird уже запущен
)

echo [db-start] Готово
