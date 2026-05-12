# Система учёта аномальных объектов Фонда SCP

Учебный проект по курсу «Базы данных». Java-приложение с GUI на JavaFX,
работающее с реляционной БД через чистый JDBC (без ORM/фреймворков).

## Возможности

- Учёт аномальных объектов SCP с классификацией Safe/Euclid/Keter
- Площадки содержания, история перемещения объектов
- Персонал с уровнями допуска L0–L5
- Мобильные оперативные группы (MTF) и их состав
- Журнал инцидентов
- Версионируемые процедуры содержания
- Авторизация (SHA-256 + соль) и контроль доступа по уровню допуска
- **Работа с двумя СУБД** (PostgreSQL и Firebird) — выбор через `config.properties`

## Требования

- Java JDK 17 или новее (OpenJDK / Oracle / Temurin)
- `curl` (на Windows 10+ есть встроенный, на Linux/macOS обычно установлен)
- ~200 МБ свободного места (Java-зависимости + портативные СУБД)
- **PostgreSQL и Firebird ставить вручную НЕ нужно** — скачаются автоматически

## Быстрый старт — одна команда

```powershell
# Windows
start.bat
```
```bash
# Linux / macOS
./start.sh
```

Этот единый скрипт делает всё подряд:
1. Скачивает Java-зависимости в `lib/` (если ещё нет).
2. Скачивает портативный PostgreSQL + Firebird в `db-runtime/`, инициализирует
   их и запускает на портах 5433 и 3051.
3. Применяет схему и тестовые данные.
4. Компилирует Java-исходники.
5. Запускает GUI-приложение.

Все шаги **идемпотентны** — повторный запуск `start` пропускает уже сделанное
(скачивания, init, seed), запускает сервера если они погасли, и просто стартует
приложение. После перезагрузки компьютера: `start.bat` снова поднимет всё.

Чтобы корректно остановить серверы БД когда закончили — `db-stop.bat` /
`./db-stop.sh`.

## Отдельные команды (если хотите контроля)

Вместо `start` можно вызывать пошагово (см. секцию «Установка — подробно»):
- `setup.bat` — только Java-зависимости
- `db-setup.bat` — только БД
- `db-start.bat` / `db-stop.bat` — управление серверами
- `build.bat` — компиляция
- `run.bat` — запуск

## Установка — подробно

### 1. Java-зависимости (`setup.sh` / `setup.bat`)

Скачивает в `lib/`: PostgreSQL JDBC (Postgres-42.7.3), Jaybird 5 (Firebird JDBC),
JavaFX 21 SDK для вашей ОС. Создаёт `config.properties` из шаблона.

### 2. Развёртывание БД — два варианта

#### Вариант A: Автоматическая портативная установка (рекомендуется)

```bash
./db-setup.sh    # Linux / macOS
db-setup.bat     # Windows
```

Скрипт делает всё сам:
1. Скачивает портативный PostgreSQL 16 (zonky binaries, ~25MB) в `db-runtime/postgres/`.
2. Запускает `initdb`, кластер на порту **5433**, локальный.
3. Создаёт роль `scp_admin` и БД `scp_foundation`.
4. Применяет `01_schema.sql` → `02_constraints.sql` → `03_seed.sql`.
5. Скачивает портативный Firebird 5.0 (~15MB) в `db-runtime/firebird/`.
6. Запускает Firebird на порту **3051**.
7. Создаёт `scp_foundation.fdb` и накатывает Firebird SQL.
8. Записывает в `config.properties` правильные URL.

⚠️ На macOS Firebird шагов 5–7 пропускается (нет официальной портативной
сборки) — Postgres ставится нормально, для Firebird см. `brew install firebird`.

Управление серверами:
- `db-start.sh` / `db-start.bat` — поднять оба сервера после перезагрузки
- `db-stop.sh` / `db-stop.bat` — корректно остановить
- Удаление: `rm -rf db-runtime/` — полностью убрать портативную установку

#### Вариант B: Использовать уже установленные PostgreSQL/Firebird

Если у вас на компьютере уже стоит Postgres/Firebird (стандартными
инсталляторами), пропустите `db-setup` и примените SQL вручную:

```bash
# PostgreSQL
sudo -u postgres psql
postgres=# CREATE USER scp_admin WITH PASSWORD 'changeme';
postgres=# CREATE DATABASE scp_foundation OWNER scp_admin;
postgres=# \q
psql -U scp_admin -d scp_foundation -f sql/postgres/01_schema.sql
psql -U scp_admin -d scp_foundation -f sql/postgres/02_constraints.sql
psql -U scp_admin -d scp_foundation -f sql/postgres/03_seed.sql

# Firebird
isql -u SYSDBA -p masterkey -i sql/firebird/01_schema.sql /path/to/scp_foundation.fdb
isql -u SYSDBA -p masterkey -i sql/firebird/02_constraints.sql /path/to/scp_foundation.fdb
isql -u SYSDBA -p masterkey -i sql/firebird/03_seed.sql /path/to/scp_foundation.fdb
```

Затем отредактируйте `config.properties` — укажите ваши URL/login/password.

### 3. Сборка и запуск

```bash
./build.sh && ./run.sh    # Linux / macOS
build.bat && run.bat       # Windows
```

### 4. Переключение между Postgres и Firebird

Редактируете `config.properties`, меняете `db.dialect=postgres` на `firebird`
и активную секцию URL/user/password. Перезапуск приложения — и оно работает
с другой СУБД, никакой пересборки не нужно.

## Тестовые учётные записи

| Login    | Password         | Role       | Clearance |
|----------|------------------|------------|-----------|
| admin    | scp-foundation   | O5         | 5         |
| sglass   | euclid-1989      | RESEARCHER | 4         |
| dnavarro | alpha-bravo      | RESEARCHER | 3         |
| evolkov  | containment-66   | RESEARCHER | 3         |
| ytanaka  | research-2024    | RESEARCHER | 2         |

> Эти пароли — для учебной среды. В реальном продакшне используйте bcrypt/argon2,
> а не SHA-256.

## Роли и видимость

- **O5** видит и редактирует всё.
- **RESEARCHER** видит карточки SCP в зависимости от clearance:
  - L0: ничего;
  - L1–2: Safe;
  - L3: Safe + Euclid;
  - L4–5: всё.
- Инциденты и история перемещений фильтруются аналогично.
- Управление пользователями — только для O5.

## Структура проекта

```
BD project/
├── src/                       # Java исходники (+ util/SqlRunner для скриптов)
├── resources/                 # FXML, CSS, изображения
├── sql/postgres/              # SQL для PostgreSQL
├── sql/firebird/              # SQL для Firebird
├── docs/testing-plan.md       # План ручного тестирования
├── docs/claude-design-prompt.md # Промт для Claude Design
├── lib/                       # JAR-зависимости (создаётся setup'ом)
├── out/                       # Скомпилированные классы (создаётся build'ом)
├── db-runtime/                # Портативные Postgres+Firebird (создаётся db-setup'ом)
├── config.properties          # Активный конфиг (не в git)
├── config.example.properties  # Шаблон
├── setup.sh / setup.bat       # Качает Java-зависимости
├── db-setup.sh / db-setup.bat # Качает и настраивает Postgres+Firebird
├── db-start.sh / db-start.bat # Поднимает серверы (после перезагрузки)
├── db-stop.sh / db-stop.bat   # Останавливает серверы
├── build.sh / build.bat       # Компилирует Java
├── run.sh / run.bat           # Запускает приложение
└── .gitignore
```

## Лицензия

Учебный проект. Используется фанатская вселенная SCP Foundation, лицензия CC BY-SA 3.0.
