# Part 3: Build/Run/Setup Scripts

Phase 2. Tasks 9-12.

## Task 9: setup.sh (Linux/macOS)

**Files:**
- Create: `setup.sh`

- [ ] **Step 1: Написать скрипт**

```bash
#!/usr/bin/env bash
set -euo pipefail

# === SCP Foundation — setup ===
# Скачивает JDBC и JavaFX JAR-ы в lib/, копирует config.example.properties.

echo "[setup] Проверка Java..."
if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: java не найдена. Установите JDK 17+ и убедитесь, что 'java' в PATH."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F[\".] 'NR==1 {print $2}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "ERROR: требуется Java 17+, найдена $JAVA_VERSION"
    exit 1
fi
echo "[setup] Java $JAVA_VERSION OK"

# Определение ОС для JavaFX classifier
OS=$(uname -s)
case "$OS" in
    Linux*)   FX_OS="linux" ;;
    Darwin*)  FX_OS="mac" ;;
    *)        echo "ERROR: неподдерживаемая ОС $OS"; exit 1 ;;
esac
echo "[setup] Platform: $FX_OS"

mkdir -p lib

MAVEN="https://repo.maven.apache.org/maven2"

declare -a JARS=(
    "$MAVEN/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar"
    "$MAVEN/org/firebirdsql/jdbc/jaybird/5.0.4.java17/jaybird-5.0.4.java17.jar"
    "$MAVEN/org/openjfx/javafx-base/21.0.2/javafx-base-21.0.2-$FX_OS.jar"
    "$MAVEN/org/openjfx/javafx-graphics/21.0.2/javafx-graphics-21.0.2-$FX_OS.jar"
    "$MAVEN/org/openjfx/javafx-controls/21.0.2/javafx-controls-21.0.2-$FX_OS.jar"
    "$MAVEN/org/openjfx/javafx-fxml/21.0.2/javafx-fxml-21.0.2-$FX_OS.jar"
)

for url in "${JARS[@]}"; do
    file="lib/$(basename "$url")"
    if [ -f "$file" ]; then
        echo "[setup] Уже есть: $(basename "$url")"
    else
        echo "[setup] Загрузка: $(basename "$url")"
        curl -fSL -o "$file" "$url"
    fi
done

if [ ! -f config.properties ]; then
    cp config.example.properties config.properties
    echo "[setup] Создан config.properties — отредактируйте его перед запуском."
fi

echo "[setup] ✓ Готово. Запустите: ./build.sh && ./run.sh"
```

- [ ] **Step 2: Сделать исполняемым и проверить**

```bash
chmod +x setup.sh
./setup.sh
ls -la lib/
```

Expected: 6 JAR файлов в `lib/`, `config.properties` создан.

- [ ] **Step 3: Коммит**

```powershell
git add setup.sh
git commit -m "feat(build): setup.sh with JDBC and JavaFX auto-download"
```

---

## Task 10: setup.bat (Windows)

**Files:**
- Create: `setup.bat`

- [ ] **Step 1: Написать скрипт**

```batch
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
call :download "%MAVEN%/org/firebirdsql/jdbc/jaybird/5.0.4.java17/jaybird-5.0.4.java17.jar"
call :download "%MAVEN%/org/openjfx/javafx-base/21.0.2/javafx-base-21.0.2-%FX_OS%.jar"
call :download "%MAVEN%/org/openjfx/javafx-graphics/21.0.2/javafx-graphics-21.0.2-%FX_OS%.jar"
call :download "%MAVEN%/org/openjfx/javafx-controls/21.0.2/javafx-controls-21.0.2-%FX_OS%.jar"
call :download "%MAVEN%/org/openjfx/javafx-fxml/21.0.2/javafx-fxml-21.0.2-%FX_OS%.jar"

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
```

- [ ] **Step 2: Проверить запуском**

```powershell
.\setup.bat
Get-ChildItem lib
```

Expected: 6 JAR файлов, `config.properties` создан.

- [ ] **Step 3: Коммит**

```powershell
git add setup.bat
git commit -m "feat(build): setup.bat for Windows"
```

---

## Task 11: build.sh + build.bat

**Files:**
- Create: `build.sh`, `build.bat`

- [ ] **Step 1: build.sh**

```bash
#!/usr/bin/env bash
set -euo pipefail

if [ ! -d lib ]; then
    echo "ERROR: запустите ./setup.sh сначала"
    exit 1
fi

echo "[build] Очистка out/"
rm -rf out
mkdir -p out

CP="lib/*"

echo "[build] Компиляция..."
find src -name "*.java" > sources.txt
javac -d out -cp "$CP" @sources.txt
rm sources.txt

echo "[build] Копирование ресурсов..."
cp -r resources/* out/

echo "[build] ✓ Готово. Запустите: ./run.sh"
```

- [ ] **Step 2: build.bat**

```batch
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
```

- [ ] **Step 3: Проверить (после того как появится `Main.java`)**

Скрипт пока не запустится — нет исходников. Это нормально, в дальнейших задачах его проверим.

- [ ] **Step 4: Коммит**

```powershell
git add build.sh build.bat
git commit -m "feat(build): build scripts for both OS"
```

---

## Task 12: run.sh + run.bat

**Files:**
- Create: `run.sh`, `run.bat`

- [ ] **Step 1: run.sh**

```bash
#!/usr/bin/env bash
set -euo pipefail

if [ ! -d out ] || [ ! -f out/ru/scp/foundation/Main.class ]; then
    echo "ERROR: сначала ./build.sh"
    exit 1
fi

if [ ! -f config.properties ]; then
    echo "ERROR: нет config.properties. Скопируйте config.example.properties."
    exit 1
fi

java --module-path lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp "out:lib/*" \
     ru.scp.foundation.Main
```

- [ ] **Step 2: run.bat**

```batch
@echo off
if not exist out\ru\scp\foundation\Main.class (
    echo ERROR: сначала build.bat
    exit /b 1
)
if not exist config.properties (
    echo ERROR: нет config.properties. Скопируйте config.example.properties.
    exit /b 1
)

java --module-path lib ^
     --add-modules javafx.controls,javafx.fxml ^
     -cp "out;lib\*" ^
     ru.scp.foundation.Main
```

- [ ] **Step 3: chmod (Linux/macOS)**

```bash
chmod +x run.sh build.sh setup.sh
```

- [ ] **Step 4: Коммит**

```powershell
git add run.sh run.bat
git commit -m "feat(build): run scripts with JavaFX module path"
```
