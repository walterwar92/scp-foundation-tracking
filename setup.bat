@echo off
setlocal enabledelayedexpansion

echo [setup] Проверка Java...
where java >nul 2>nul
if errorlevel 1 (
    echo ERROR: java не найдена. Установите JDK 17+ и добавьте в PATH.
    exit /b 1
)

for /f "tokens=2 delims==." %%v in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.specification.version"') do (
    set "JAVA_MAJOR=%%v"
)
set "JAVA_MAJOR=!JAVA_MAJOR: =!"
if !JAVA_MAJOR! lss 17 (
    echo ERROR: требуется Java 17+, найдена !JAVA_MAJOR!
    exit /b 1
)
echo [setup] Java !JAVA_MAJOR! OK

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
    echo [setup] Создан config.properties — отредактируйте его перед запуском.
)

echo [setup] Готово. Запустите: build.bat ^&^& run.bat
goto :eof

:download
set "URL=%~1"
for %%F in ("%URL%") do set "NAME=%%~nxF"
if exist "lib\%NAME%" (
    echo [setup] Уже есть: %NAME%
) else (
    echo [setup] Загрузка: %NAME%
    curl -fSL -o "lib\%NAME%" "%URL%"
    if errorlevel 1 (
        echo ERROR: не удалось загрузить %URL%
        exit /b 1
    )
)
goto :eof
