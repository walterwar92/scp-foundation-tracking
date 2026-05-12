@echo off
setlocal enabledelayedexpansion

set "DB=db-runtime"
set "PG_DIR=%DB%\postgres"
set "PG_DATA=%PG_DIR%\data"

if exist "%PG_DIR%\bin\pg_ctl.exe" (
    "%PG_DIR%\bin\pg_ctl.exe" status -D "%PG_DATA%" >nul 2>&1
    if errorlevel 1 (
        echo [db-stop] PostgreSQL не запущен
    ) else (
        echo [db-stop] Остановка PostgreSQL...
        "%PG_DIR%\bin\pg_ctl.exe" stop -D "%PG_DATA%" -m fast >nul
    )
)

tasklist /FI "IMAGENAME eq firebird.exe" 2>nul | find /I "firebird.exe" >nul
if errorlevel 1 (
    echo [db-stop] Firebird не запущен
) else (
    echo [db-stop] Остановка Firebird...
    taskkill /F /IM firebird.exe >nul 2>&1
)

echo [db-stop] Готово
