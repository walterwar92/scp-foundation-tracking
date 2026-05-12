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
- PostgreSQL 14+ или Firebird 3.0+ (одна из двух)
- `curl` (на Windows 10+ есть встроенный, на Linux/macOS обычно установлен)
- ~50 МБ свободного места под зависимости

## Установка

### 1. Развёртывание базы данных

#### Вариант A: PostgreSQL

```bash
# Создать пользователя и БД
sudo -u postgres psql
postgres=# CREATE USER scp_admin WITH PASSWORD 'changeme';
postgres=# CREATE DATABASE scp_foundation OWNER scp_admin;
postgres=# \q

# Применить скрипты
psql -U scp_admin -d scp_foundation -f sql/postgres/01_schema.sql
psql -U scp_admin -d scp_foundation -f sql/postgres/02_constraints.sql
psql -U scp_admin -d scp_foundation -f sql/postgres/03_seed.sql
```

#### Вариант B: Firebird

```bash
# Применить скрипты через isql
isql -u SYSDBA -p masterkey -i sql/firebird/01_schema.sql /path/to/scp_foundation.fdb
isql -u SYSDBA -p masterkey -i sql/firebird/02_constraints.sql /path/to/scp_foundation.fdb
isql -u SYSDBA -p masterkey -i sql/firebird/03_seed.sql /path/to/scp_foundation.fdb
```

### 2. Установка зависимостей приложения

```bash
# Linux / macOS
./setup.sh

# Windows
setup.bat
```

Скрипт скачает в `lib/`: PostgreSQL JDBC, Jaybird (Firebird JDBC), JavaFX SDK.
И создаст `config.properties` из шаблона.

### 3. Настройка подключения

Откройте `config.properties` и укажите параметры **вашей** СУБД:

```properties
db.dialect=postgres
db.url=jdbc:postgresql://localhost:5432/scp_foundation
db.user=scp_admin
db.password=changeme
```

Чтобы переключиться на Firebird — поменяйте `db.dialect=firebird` и
закомментируйте Postgres-секцию, раскомментировав Firebird.

### 4. Сборка и запуск

```bash
# Linux / macOS
./build.sh && ./run.sh

# Windows
build.bat && run.bat
```

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
├── src/                       # Java исходники
├── resources/                 # FXML, CSS, изображения
├── sql/postgres/              # SQL для PostgreSQL
├── sql/firebird/              # SQL для Firebird
├── docs/testing-plan.md       # План ручного тестирования
├── docs/claude-design-prompt.md # Промт для Claude Design
├── lib/                       # JAR-зависимости (создаётся setup'ом)
├── out/                       # Скомпилированные классы (создаётся build'ом)
├── config.properties          # Активный конфиг (не в git)
├── config.example.properties  # Шаблон
├── setup.sh / setup.bat
├── build.sh / build.bat
├── run.sh / run.bat
└── .gitignore
```

## Лицензия

Учебный проект. Используется фанатская вселенная SCP Foundation, лицензия CC BY-SA 3.0.
