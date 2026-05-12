@echo off
setlocal enabledelayedexpansion

echo [setup] Checking Java...
where java >nul 2>nul
if errorlevel 1 (
    echo ERROR: java not found. Install JDK 17+ and add it to PATH.
    exit /b 1
)

rem Best-effort major version check (works for Java 17+ which is what we require)
for /f "tokens=2 delims= " %%v in ('java --version 2^>nul') do (
    set "JAVA_VER=%%v"
    goto :got_version
)
:got_version
if not defined JAVA_VER (
    echo [setup] WARN: could not detect Java version, proceeding anyway
) else (
    for /f "tokens=1 delims=." %%m in ("!JAVA_VER!") do set "JAVA_MAJOR=%%m"
    if defined JAVA_MAJOR (
        if !JAVA_MAJOR! lss 17 (
            echo ERROR: Java !JAVA_VER! detected, need 17+
            exit /b 1
        )
        echo [setup] Java !JAVA_VER! OK
    )
)

set "FX_OS=win"
echo [setup] Platform: %FX_OS%

if not exist lib mkdir lib

set "MAVEN=https://repo.maven.apache.org/maven2"

call :download "%MAVEN%/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar"
if errorlevel 1 exit /b 1
call :download "%MAVEN%/org/firebirdsql/jdbc/jaybird/5.0.4.java11/jaybird-5.0.4.java11.jar"
if errorlevel 1 exit /b 1
call :download "%MAVEN%/org/openjfx/javafx-base/21.0.2/javafx-base-21.0.2-%FX_OS%.jar"
if errorlevel 1 exit /b 1
call :download "%MAVEN%/org/openjfx/javafx-graphics/21.0.2/javafx-graphics-21.0.2-%FX_OS%.jar"
if errorlevel 1 exit /b 1
call :download "%MAVEN%/org/openjfx/javafx-controls/21.0.2/javafx-controls-21.0.2-%FX_OS%.jar"
if errorlevel 1 exit /b 1
call :download "%MAVEN%/org/openjfx/javafx-fxml/21.0.2/javafx-fxml-21.0.2-%FX_OS%.jar"
if errorlevel 1 exit /b 1

if not exist config.properties (
    copy config.example.properties config.properties >nul
    echo [setup] Created config.properties - edit it before running.
)

echo [setup] Done. Run: build.bat ^&^& run.bat
goto :eof

:download
set "URL=%~1"
for %%F in ("%URL%") do set "NAME=%%~nxF"
if exist "lib\%NAME%" (
    echo [setup] Already present: %NAME%
) else (
    echo [setup] Downloading: %NAME%
    curl -fSL -o "lib\%NAME%" "%URL%"
    if errorlevel 1 (
        echo ERROR: failed to download %URL%
        exit /b 1
    )
)
goto :eof
