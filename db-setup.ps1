# SCP Foundation — портативная установка PostgreSQL + Firebird для Windows
# Скачивает бинарники, инициализирует БД, применяет schema/constraints/seed.
# Серверы на нестандартных портах 5433 (Postgres) и 3051 (Firebird).

$ErrorActionPreference = 'Stop'

# ============== Параметры ==============
$Root      = (Get-Location).Path
$DbDir     = Join-Path $Root 'db-runtime'
$PgDir     = Join-Path $DbDir 'postgres'
$PgData    = Join-Path $PgDir 'data'
$PgPort    = 5433
$PgPass    = 'scp-foundation-db'
$PgUser    = 'scp_admin'
$PgDbName  = 'scp_foundation'
$FbDir     = Join-Path $DbDir 'firebird'
$FbPort    = 3051
$FbPass    = 'masterkey'
$FbDbFile  = Join-Path $FbDir 'databases\scp_foundation.fdb'

$PgJarUrl  = 'https://repo.maven.apache.org/maven2/io/zonky/test/postgres/embedded-postgres-binaries-windows-amd64/16.4.0/embedded-postgres-binaries-windows-amd64-16.4.0.jar'
$FbZipUrl  = 'https://github.com/FirebirdSQL/firebird/releases/download/v5.0.1/Firebird-5.0.1.1469-0-windows-x64.zip'

Write-Host "[db-setup] === SCP Foundation portable DB setup ===" -ForegroundColor Cyan

# ============== Проверки ==============
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "Java не найдена. Сначала setup.bat"
}
if (-not (Test-Path 'lib')) {
    Write-Error "Папка lib/ не существует. Сначала setup.bat"
}

New-Item -ItemType Directory -Force -Path $DbDir | Out-Null

# Подгружаем тип ZipFile один раз для всего скрипта
Add-Type -AssemblyName System.IO.Compression.FileSystem

# ============== 0. Java-инструменты для распаковки .txz ==============
# Windows tar.exe не умеет xz без внешнего liblzma. Используем чистую Java:
# commons-compress + xz-java читают .txz напрямую.
$ToolsDir = Join-Path $DbDir 'tools'
New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
$CommonsCompressJar = Join-Path $ToolsDir 'commons-compress-1.27.1.jar'
$CommonsIoJar       = Join-Path $ToolsDir 'commons-io-2.16.1.jar'
$XzJar              = Join-Path $ToolsDir 'xz-1.10.jar'

if (-not (Test-Path $CommonsCompressJar)) {
    Write-Host "[db-setup] Загрузка commons-compress (для распаковки .txz)..."
    curl.exe -fSL -o $CommonsCompressJar 'https://repo.maven.apache.org/maven2/org/apache/commons/commons-compress/1.27.1/commons-compress-1.27.1.jar'
    if ($LASTEXITCODE -ne 0) { Write-Error 'Скачивание commons-compress не удалось' }
}
if (-not (Test-Path $CommonsIoJar)) {
    Write-Host "[db-setup] Загрузка commons-io (зависимость commons-compress)..."
    curl.exe -fSL -o $CommonsIoJar 'https://repo.maven.apache.org/maven2/commons-io/commons-io/2.16.1/commons-io-2.16.1.jar'
    if ($LASTEXITCODE -ne 0) { Write-Error 'Скачивание commons-io не удалось' }
}
if (-not (Test-Path $XzJar)) {
    Write-Host "[db-setup] Загрузка xz-java..."
    curl.exe -fSL -o $XzJar 'https://repo.maven.apache.org/maven2/org/tukaani/xz/1.10/xz-1.10.jar'
    if ($LASTEXITCODE -ne 0) { Write-Error 'Скачивание xz-java не удалось' }
}

$ToolsCp = "$CommonsCompressJar;$CommonsIoJar;$XzJar"

# Компилируем TxzExtractor если ещё не собран
$TxzClass = 'out\ru\scp\foundation\util\TxzExtractor.class'
if (-not (Test-Path $TxzClass)) {
    Write-Host "[db-setup] Компилирую TxzExtractor..."
    New-Item -ItemType Directory -Force -Path 'out' | Out-Null
    & javac -encoding UTF-8 -cp $ToolsCp -d out 'src\ru\scp\foundation\util\TxzExtractor.java'
    if ($LASTEXITCODE -ne 0) { Write-Error 'Компиляция TxzExtractor не удалась' }
}

# ============== 1. Скачиваем portable PostgreSQL ==============
if (Test-Path (Join-Path $PgDir 'bin\postgres.exe')) {
    Write-Host "[db-setup] PostgreSQL уже распакован"
} else {
    $jarPath = Join-Path $DbDir 'pg.jar'
    if (-not (Test-Path $jarPath)) {
        Write-Host "[db-setup] Загрузка PostgreSQL (zonky, ~22MB)..."
        curl.exe -fSL -o $jarPath $PgJarUrl
        if ($LASTEXITCODE -ne 0) { Write-Error "Скачивание PostgreSQL не удалось" }
    }
    $jarExtract = Join-Path $DbDir 'pg-jar'
    if (Test-Path $jarExtract) { Remove-Item -Recurse -Force $jarExtract }
    Write-Host "[db-setup] Распаковка JAR..."
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($jarPath, $jarExtract)

    New-Item -ItemType Directory -Force -Path $PgDir | Out-Null
    $txz = Get-ChildItem -Path $jarExtract -Filter '*.txz' | Select-Object -First 1
    Write-Host "[db-setup] Распаковка $($txz.Name) через TxzExtractor (Java)..."
    & java -cp "out;$ToolsCp" ru.scp.foundation.util.TxzExtractor $txz.FullName $PgDir
    if ($LASTEXITCODE -ne 0) { Write-Error "Распаковка TXZ не удалась" }

    Remove-Item -Recurse -Force $jarExtract
    Remove-Item -Force $jarPath
}

# ============== 2. initdb ==============
if (Test-Path (Join-Path $PgData 'PG_VERSION')) {
    Write-Host "[db-setup] Postgres кластер уже инициализирован"
} else {
    Write-Host "[db-setup] initdb..."
    $pwFile = Join-Path $DbDir 'pg-pwfile.txt'
    Set-Content -Path $pwFile -Value $PgPass -Encoding ASCII -NoNewline
    $initdb = Join-Path $PgDir 'bin\initdb.exe'
    # На Windows с русской системной локалью postgres внедряет cp1251-байты
    # в bootstrap SQL — UTF8-БД падает. Форсим English/C для initdb-процесса.
    $savedLcAll = $env:LC_ALL
    $savedLang  = $env:LANG
    $savedLcMsg = $env:LC_MESSAGES
    $env:LC_ALL = 'C'
    $env:LANG = 'C'
    $env:LC_MESSAGES = 'C'
    try {
        # Кластер инициализируется в SQL_ASCII — обход бага Postgres-on-Windows,
        # когда имя OS-пользователя в cp1251 ("Роман") вставляется в UTF-8 системные
        # таблицы и падает на post-bootstrap. Наша рабочая БД (создаётся ниже) явно
        # с UTF8 через template0.
        & $initdb -D $PgData -U postgres "--pwfile=$pwFile" -E SQL_ASCII --no-locale --auth=md5
        if ($LASTEXITCODE -ne 0) {
            Write-Error "initdb не удался"
        }
    } finally {
        $env:LC_ALL = $savedLcAll
        $env:LANG = $savedLang
        $env:LC_MESSAGES = $savedLcMsg
        Remove-Item -Force $pwFile -ErrorAction SilentlyContinue
    }

    # Прописываем порт в postgresql.conf
    Add-Content -Path (Join-Path $PgData 'postgresql.conf') -Value @"

port = $PgPort
listen_addresses = 'localhost'
lc_messages = 'C'
"@
}

# ============== 3. Старт Postgres ==============
$pgCtl = Join-Path $PgDir 'bin\pg_ctl.exe'
& $pgCtl status -D $PgData *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[db-setup] Старт PostgreSQL на порту $PgPort..."
    & $pgCtl start -D $PgData -l (Join-Path $PgDir 'log.txt') -w -o "-p $PgPort -h localhost"
    if ($LASTEXITCODE -ne 0) { Write-Error "Не удалось запустить Postgres. Лог: $PgDir\log.txt" }
} else {
    Write-Host "[db-setup] PostgreSQL уже запущен"
}

# ============== 4. SqlRunner ==============
$SqlRunnerClass = 'out\ru\scp\foundation\util\SqlRunner.class'
if (-not (Test-Path $SqlRunnerClass)) {
    Write-Host "[db-setup] Компилирую SqlRunner..."
    New-Item -ItemType Directory -Force -Path 'out' | Out-Null
    & javac -encoding UTF-8 -d out 'src\ru\scp\foundation\util\SqlRunner.java'
    if ($LASTEXITCODE -ne 0) { Write-Error "Компиляция SqlRunner не удалась" }
}

function Invoke-Sql {
    param(
        [string]$driver,
        [string]$url,
        [string]$user,
        [string]$pass,
        [string]$mode,
        [string]$arg,
        [bool]$ignoreErrors = $false
    )
    $cpSep = ';'
    $javaArgs = @('-cp', "out${cpSep}lib\*", 'ru.scp.foundation.util.SqlRunner', $driver, $url, $user, $pass, $mode, $arg)
    if ($ignoreErrors) { $javaArgs += '--ignore-errors' }
    & java @javaArgs
}

# ============== 5. CREATE ROLE + CREATE DATABASE ==============
$PgSeededMarker = Join-Path $DbDir '.postgres-seeded'
if (Test-Path $PgSeededMarker) {
    Write-Host "[db-setup] PostgreSQL уже инициализирован — пропускаю SQL"
} else {
    Write-Host "[db-setup] Создание роли и БД..."
    Invoke-Sql 'org.postgresql.Driver' "jdbc:postgresql://localhost:$PgPort/postgres" `
        'postgres' $PgPass '-c' "CREATE ROLE $PgUser LOGIN PASSWORD '$PgPass' CREATEDB" $true
    Invoke-Sql 'org.postgresql.Driver' "jdbc:postgresql://localhost:$PgPort/postgres" `
        'postgres' $PgPass '-c' "CREATE DATABASE $PgDbName OWNER $PgUser ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C' TEMPLATE template0" $true

    # ============== 6. Применение SQL ==============
    Write-Host "[db-setup] Применение schema/constraints/seed (Postgres)..."
    foreach ($f in @('01_schema.sql', '02_constraints.sql', '03_seed.sql')) {
        Write-Host "  sql\postgres\$f"
        Invoke-Sql 'org.postgresql.Driver' "jdbc:postgresql://localhost:$PgPort/$PgDbName" `
            $PgUser $PgPass '-f' "sql\postgres\$f" $false
        if ($LASTEXITCODE -ne 0) { Write-Error "Postgres SQL не применился: $f" }
    }
    Set-Content -Path $PgSeededMarker -Value (Get-Date -Format 'o') -Encoding ASCII
}

# ============== 7. Скачиваем portable Firebird ==============
if (Test-Path (Join-Path $FbDir 'firebird.exe')) {
    Write-Host "[db-setup] Firebird уже распакован"
} else {
    $fbZip = Join-Path $DbDir 'fb.zip'
    if (-not (Test-Path $fbZip)) {
        Write-Host "[db-setup] Загрузка Firebird (~15MB)..."
        curl.exe -fSL -o $fbZip $FbZipUrl
        if ($LASTEXITCODE -ne 0) { Write-Error "Скачивание Firebird не удалось" }
    }
    Write-Host "[db-setup] Распаковка Firebird..."
    New-Item -ItemType Directory -Force -Path $FbDir | Out-Null
    [System.IO.Compression.ZipFile]::ExtractToDirectory($fbZip, $FbDir)
    Remove-Item -Force $fbZip

    # Правим конфиг — нестандартный порт + отключаем WireCrypt для совместимости
    $fbConf = Join-Path $FbDir 'firebird.conf'
    if (Test-Path $fbConf) {
        $confText = Get-Content $fbConf -Raw
        $confText = $confText -replace '(?m)^#?RemoteServicePort\s*=.*$', "RemoteServicePort = $FbPort"
        $confText = $confText -replace '(?m)^#?WireCrypt\s*=.*$', 'WireCrypt = Enabled'
        $confText = $confText -replace '(?m)^#?AuthServer\s*=.*$', 'AuthServer = Srp, Legacy_Auth'
        $confText = $confText -replace '(?m)^#?UserManager\s*=.*$', 'UserManager = Srp, Legacy_UserManager'
        Set-Content -Path $fbConf -Value $confText -NoNewline
    }
}

New-Item -ItemType Directory -Force -Path (Join-Path $FbDir 'databases') | Out-Null

# ============== 7b. databases.conf: ASCII-алиас 'scp' для нашей БД ==============
# JDBC URL с кириллицей в пути падает на Jaybird (SQLSTATE 08001).
# Используем 8.3 короткий путь Windows для databases-каталога — он гарантированно
# ASCII (e.g. "C:\Users\РОМАН~1\Desktop\BD~1\..."), Firebird парсит без проблем.
$fbAbsPathForAlias = (Resolve-Path -LiteralPath $FbDir).Path
$fbDbDir = Join-Path $fbAbsPathForAlias 'databases'
New-Item -ItemType Directory -Force -Path $fbDbDir | Out-Null
try {
    $fso = New-Object -ComObject Scripting.FileSystemObject
    $fbDbDirShort = $fso.GetFolder($fbDbDir).ShortPath
} catch {
    $fbDbDirShort = $fbDbDir
}
$fbDbAbsPathForAlias = Join-Path $fbDbDirShort 'scp_foundation.fdb'
Write-Host "[db-setup] FDB-путь (short): $fbDbAbsPathForAlias"

$dbConf = Join-Path $FbDir 'databases.conf'
$aliasLine = "scp = $fbDbAbsPathForAlias"
$aliasNeedsRewrite = $true
if (Test-Path $dbConf) {
    $dbConfText = Get-Content $dbConf -Raw -ErrorAction SilentlyContinue
    # Если строка алиаса уже та самая — не трогаем
    if ($dbConfText -match [regex]::Escape($aliasLine)) {
        $aliasNeedsRewrite = $false
    } else {
        # Удаляем любую старую строку 'scp = ...' (могла быть с длинным путём)
        $dbConfText = ($dbConfText -split "`r?`n" | Where-Object {
            $_ -notmatch '^\s*scp\s*=' -and $_ -notmatch '^\s*#\s*SCP Foundation portable alias'
        }) -join "`n"
        # Пишем без BOM в ASCII (путь после short-conversion это позволяет)
        [System.IO.File]::WriteAllText($dbConf, $dbConfText.TrimEnd() + "`n`n# SCP Foundation portable alias`n$aliasLine`n", [System.Text.Encoding]::ASCII)
        Write-Host "[db-setup] Алиас 'scp' обновлён в databases.conf"
    }
} else {
    [System.IO.File]::WriteAllText($dbConf, "# SCP Foundation portable alias`n$aliasLine`n", [System.Text.Encoding]::ASCII)
    Write-Host "[db-setup] databases.conf создан с алиасом 'scp'"
}

# Каждый раз когда мы трогаем databases.conf — перезагружаем сервер
if ($aliasNeedsRewrite) {
    $fbProc = Get-Process firebird -ErrorAction SilentlyContinue
    if ($fbProc) {
        Write-Host "[db-setup] Перезагрузка Firebird для применения алиаса..."
        Stop-Process -Id $fbProc.Id -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
}

# ============== 8a. Bootstrap SYSDBA в security database ==============
# Firebird 5 ставит security5.fdb пустой. Чтобы создать SYSDBA, надо
# работать с файлом security5.fdb напрямую в embedded-режиме (без сервера).
# Поэтому: останавливаем сервер если запущен, бутстрапим через isql,
# затем стартуем сервер.
$fbExe = Join-Path $FbDir 'firebird.exe'
$SysdbaMarker = Join-Path $DbDir '.firebird-sysdba'

if (-not (Test-Path $SysdbaMarker)) {
    # Останавливаем сервер если работает (бэкап-файлы будут заняты)
    $fbProc = Get-Process firebird -ErrorAction SilentlyContinue
    if ($fbProc) {
        Write-Host "[db-setup] Остановка Firebird перед bootstrap..."
        Stop-Process -Id $fbProc.Id -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }

    $isqlExe = Join-Path $FbDir 'isql.exe'
    $securityDb = Join-Path $FbDir 'security5.fdb'
    if ((Test-Path $isqlExe) -and (Test-Path $securityDb)) {
        Write-Host "[db-setup] Bootstrap SYSDBA в $securityDb (embedded)..."
        # CREATE OR ALTER идемпотентно — работает и при первом создании,
        # и если SYSDBA уже был добавлен в прошлый запуск.
        $bootstrapSql = @"
CREATE OR ALTER USER SYSDBA PASSWORD 'masterkey' USING PLUGIN Srp;
CREATE OR ALTER USER SYSDBA PASSWORD 'masterkey' USING PLUGIN Legacy_UserManager;
COMMIT;
QUIT;
"@
        $tempSql = Join-Path $DbDir 'fb-bootstrap.sql'
        Set-Content -Path $tempSql -Value $bootstrapSql -Encoding ASCII

        # Embedded-режим: FIREBIRD env, isql подключается к файлу напрямую без TCP.
        $env:FIREBIRD = (Resolve-Path $FbDir).Path
        $env:ISC_USER = 'SYSDBA'
        $env:ISC_PASSWORD = $FbPass
        $out = & $isqlExe -bail -i $tempSql $securityDb 2>&1
        $bootstrapExit = $LASTEXITCODE
        Remove-Item -Force $tempSql -ErrorAction SilentlyContinue

        # SQLSTATE 23000 (integrity constraint violation) = пользователь уже есть
        # — для нашего сценария это успех.
        $alreadyExists = ($out -join "`n") -match '23000'
        if ($bootstrapExit -eq 0 -or $alreadyExists) {
            Set-Content -Path $SysdbaMarker -Value (Get-Date -Format 'o') -Encoding ASCII
            if ($alreadyExists -and $bootstrapExit -ne 0) {
                Write-Host "[db-setup] SYSDBA уже существовал — продолжаю"
            } else {
                Write-Host "[db-setup] SYSDBA создан в security5.fdb"
            }
            # Сбрасываем seed-маркер и битый FDB от прошлой неудачной попытки
            $FbSeededMarkerReset = Join-Path $DbDir '.firebird-seeded'
            if (Test-Path $FbSeededMarkerReset) {
                Remove-Item -Force $FbSeededMarkerReset
            }
            $fbDbReset = Join-Path $FbDir 'databases\scp_foundation.fdb'
            if (Test-Path $fbDbReset) {
                Remove-Item -Force $fbDbReset
            }
        } else {
            Write-Host "[db-setup] Предупреждение: bootstrap вернул код $bootstrapExit"
            Write-Host $out
        }
    }
}

# ============== 8b. Старт Firebird ==============
$fbProc = Get-Process firebird -ErrorAction SilentlyContinue
if (-not $fbProc) {
    Write-Host "[db-setup] Старт Firebird на порту $FbPort..."
    Start-Process -FilePath $fbExe -ArgumentList '-m' -WorkingDirectory $FbDir -WindowStyle Hidden
    Start-Sleep -Seconds 3
} else {
    Write-Host "[db-setup] Firebird уже запущен"
}

# ============== 9. Создание Firebird БД + применение SQL ==============
$fbAbsPath = (Resolve-Path -LiteralPath $FbDir).Path
$fbDbAbsPath = Join-Path $fbAbsPath 'databases\scp_foundation.fdb'

$FbSeededMarker = Join-Path $DbDir '.firebird-seeded'
if (Test-Path $FbSeededMarker) {
    Write-Host "[db-setup] Firebird уже инициализирован — пропускаю SQL"
} else {
    if (Test-Path $fbDbAbsPath) {
        Write-Host "[db-setup] FDB-файл существует, пропускаю создание"
    } else {
        Write-Host "[db-setup] Создание Firebird БД через алиас 'scp'..."
        # Используем алиас 'scp' (ASCII) вместо абсолютного пути с кириллицей —
        # Jaybird падает на транслитерации Cyrillic в connection string.
        $createUrl = "jdbc:firebirdsql://localhost:$FbPort/scp?charSet=UTF8&createDatabaseIfNotExist=true&isc_dpb_force_write=true"
        Invoke-Sql 'org.firebirdsql.jdbc.FBDriver' $createUrl `
            'SYSDBA' $FbPass '-c' 'SELECT 1 FROM RDB$DATABASE' $true
    }

    Write-Host "[db-setup] Применение schema/constraints/seed (Firebird)..."
    $fbJdbc = "jdbc:firebirdsql://localhost:$FbPort/scp?charSet=UTF8"
    foreach ($f in @('01_schema.sql', '02_constraints.sql', '03_seed.sql')) {
        Write-Host "  sql\firebird\$f"
        Invoke-Sql 'org.firebirdsql.jdbc.FBDriver' $fbJdbc `
            'SYSDBA' $FbPass '-f' "sql\firebird\$f" $true
    }
    Set-Content -Path $FbSeededMarker -Value (Get-Date -Format 'o') -Encoding ASCII
}

# ============== 10. config.properties ==============
Write-Host "[db-setup] Обновление config.properties..."
$config = @"
# Активный диалект: postgres | firebird
db.dialect=postgres

# PostgreSQL (портативный, db-runtime/postgres)
db.url=jdbc:postgresql://localhost:$PgPort/$PgDbName
db.user=$PgUser
db.password=$PgPass

# Firebird (портативный, db-runtime/firebird) - раскомментируйте и закомментируйте Postgres:
# Алиас 'scp' определён в db-runtime/firebird/databases.conf.
# db.dialect=firebird
# db.url=jdbc:firebirdsql://localhost:$FbPort/scp?charSet=UTF8
# db.user=SYSDBA
# db.password=$FbPass
"@
Set-Content -Path 'config.properties' -Value $config -Encoding UTF8

Write-Host ""
Write-Host "[db-setup] === Готово ===" -ForegroundColor Green
Write-Host "PostgreSQL:  localhost:$PgPort   user=$PgUser   db=$PgDbName"
Write-Host "Firebird:    localhost:$FbPort   user=SYSDBA      db=$fbDbAbsPath"
Write-Host ""
Write-Host "Чтобы остановить серверы:  db-stop.bat"
Write-Host "Чтобы запустить заново:    db-start.bat"
Write-Host "Чтобы переключить СУБД:    отредактируйте config.properties"
