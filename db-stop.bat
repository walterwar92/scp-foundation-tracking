@echo off
setlocal enabledelayedexpansion

set "DB=db-runtime"
set "PG_DIR=%DB%\postgres"
set "PG_DATA=%PG_DIR%\data"

if exist "%PG_DIR%\bin\pg_ctl.exe" (
    "%PG_DIR%\bin\pg_ctl.exe" status -D "%PG_DATA%" >nul 2>&1
    if errorlevel 1 (
        echo [db-stop] PostgreSQL not running
    ) else (
        echo [db-stop] Stopping PostgreSQL...
        "%PG_DIR%\bin\pg_ctl.exe" stop -D "%PG_DATA%" -m fast >nul
    )
)

tasklist /FI "IMAGENAME eq firebird.exe" 2>nul | find /I "firebird.exe" >nul
if errorlevel 1 (
    echo [db-stop] Firebird not running
) else (
    echo [db-stop] Stopping Firebird...
    taskkill /F /IM firebird.exe >nul 2>&1
)

echo [db-stop] Done
