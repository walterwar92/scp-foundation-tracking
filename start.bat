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
    echo [start] Step 1/5: downloading Java dependencies...
    call "%~dp0setup.bat"
    if errorlevel 1 (
        echo ERROR: setup.bat failed
        exit /b 1
    )
) else (
    echo [start] Step 1/5: Java deps already present, skipping setup.bat
)

rem ----- Step 2: DB install + start -----
echo [start] Step 2/5: portable DB install + start...
call "%~dp0db-setup.bat"
if errorlevel 1 (
    echo ERROR: db-setup.bat failed
    exit /b 1
)

rem ----- Step 3: choose DBMS -----
echo [start] Step 3/5: choose database
echo.
echo === Choose database ===
echo   1 PostgreSQL  ^(default^)
echo   2 Firebird
set "DB_CHOICE="
set /p DB_CHOICE="Selection [1]: "
if "%DB_CHOICE%"=="" set "DB_CHOICE=1"
if "%DB_CHOICE%"=="2" (
    set "FDB_PATH=%CD%\db-runtime\firebird\databases\scp_foundation.fdb"
    setlocal enabledelayedexpansion
    set "FDB_FWD=!FDB_PATH:\=/!"
    > config.properties (
        echo db.dialect=firebird
        echo db.url=jdbc:firebirdsql://localhost:3051/!FDB_FWD!?charSet=UTF8
        echo db.user=SYSDBA
        echo db.password=masterkey
    )
    endlocal
    echo [start] Selected: Firebird
) else (
    > config.properties (
        echo db.dialect=postgres
        echo db.url=jdbc:postgresql://localhost:5433/scp_foundation
        echo db.user=scp_admin
        echo db.password=scp-foundation-db
    )
    echo [start] Selected: PostgreSQL
)

rem ----- Step 4: compile -----
echo [start] Step 4/5: compiling Java...
call "%~dp0build.bat"
if errorlevel 1 (
    echo ERROR: build.bat failed
    exit /b 1
)

rem ----- Step 5: run -----
echo [start] Step 5/5: launching application...
call "%~dp0run.bat"
