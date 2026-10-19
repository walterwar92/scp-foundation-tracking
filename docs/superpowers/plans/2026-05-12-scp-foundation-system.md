# SCP Foundation System — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Реализовать Java-приложение учёта аномальных объектов Фонда SCP с GUI на JavaFX, работающее с двумя СУБД (PostgreSQL и Firebird) через чистый JDBC, с авторизацией и системой ролей.

**Architecture:** Трёхслойная (UI → Service → DAO → Infrastructure). Без фреймворков. SQL-диалект скрыт за enum `SqlDialect`. Авторизация на SHA-256+salt, контроль доступа в коде приложения. Сборка через скрипты `.sh`/`.bat` с автозагрузкой JAR-зависимостей.

**Tech Stack:** Java 17+, JavaFX 21, PostgreSQL 14+, Firebird 3.0+, JDBC (postgresql-42.7.3, jaybird-5.0.4.java17), без Maven/Gradle.

**Verification model:** Пользователь отказался от автотестов. Вместо TDD-цикла каждая задача завершается:
1. Компиляцией кода без ошибок.
2. Ручным smoke-тестом (где применимо — запуск приложения и проверка функциональности).
3. Коммитом.

---

## Working directory

Все пути относительны от `C:\Users\Роман\Desktop\BD project\` (Windows-хост проекта). В командах используется PowerShell-синтаксис; для bash-эквивалентов см. соответствующие задачи скриптов.

## Phase 0: Bootstrap (Tasks 1-2)

### Task 1: Инициализация структуры проекта и git

**Files:**
- Create: `.gitignore`
- Создаются директории: `src/ru/scp/foundation/`, `resources/{fxml,css,images}/`, `sql/{postgres,firebird}/`, `docs/`, `lib/`

- [ ] **Step 1: Создать дерево директорий**

```powershell
New-Item -ItemType Directory -Force -Path src\ru\scp\foundation\config, src\ru\scp\foundation\db, src\ru\scp\foundation\model, src\ru\scp\foundation\dao, src\ru\scp\foundation\auth, src\ru\scp\foundation\access, src\ru\scp\foundation\ui\controllers, src\ru\scp\foundation\ui\animation, src\ru\scp\foundation\ui\util, resources\fxml, resources\css, resources\images, sql\postgres, sql\firebird, docs, lib
```

- [ ] **Step 2: Создать `.gitignore`**

Создать файл `.gitignore` со следующим содержимым:

```
# Build output
out/
*.class

# Dependencies
lib/

# Local config (содержит реальный пароль БД)
config.properties

# IDE
.idea/
*.iml
.vscode/
.project
.classpath
.settings/

# OS
.DS_Store
Thumbs.db

# Logs
*.log
```

- [ ] **Step 3: Инициализировать git**

```powershell
git init
git add .gitignore
git commit -m "chore: initial commit with gitignore"
```

Expected: `git status` показывает clean working tree, ветка `master` или `main`.

---

### Task 2: config.example.properties

**Files:**
- Create: `config.example.properties`

- [ ] **Step 1: Создать шаблон конфига**

```properties
# Активный диалект: postgres | firebird
db.dialect=postgres

# PostgreSQL пример
db.url=jdbc:postgresql://localhost:5432/scp_foundation
db.user=scp_admin
db.password=changeme

# Firebird пример (раскомментируйте и закомментируйте Postgres):
# db.dialect=firebird
# db.url=jdbc:firebirdsql://localhost:3050/C:/firebird/scp_foundation.fdb
# db.user=SYSDBA
# db.password=masterkey
```

- [ ] **Step 2: Коммит**

```powershell
git add config.example.properties
git commit -m "feat: add config template for two DBMS"
```

---

## Phase 1: SQL для PostgreSQL (Tasks 3-5)

### Task 3: PostgreSQL — схема

**Files:**
- Create: `sql/postgres/01_schema.sql`

- [ ] **Step 1: Написать DDL для 9 таблиц**

Содержимое `sql/postgres/01_schema.sql`:

```sql
-- SCP Foundation Database — PostgreSQL schema
-- Создаёт 9 таблиц без CHECK/FK (они в 02_constraints.sql)

DROP TABLE IF EXISTS procedure_revisions CASCADE;
DROP TABLE IF EXISTS incidents CASCADE;
DROP TABLE IF EXISTS mtf_members CASCADE;
DROP TABLE IF EXISTS mtf_teams CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS personnel CASCADE;
DROP TABLE IF EXISTS containment_history CASCADE;
DROP TABLE IF EXISTS scp_objects CASCADE;
DROP TABLE IF EXISTS containment_sites CASCADE;

CREATE TABLE scp_objects (
    id              BIGSERIAL PRIMARY KEY,
    item_number     VARCHAR(20)  NOT NULL UNIQUE,
    code_name       VARCHAR(200) NOT NULL,
    object_class    VARCHAR(10)  NOT NULL,
    description     TEXT,
    discovered_at   DATE
);

CREATE TABLE containment_sites (
    id              BIGSERIAL PRIMARY KEY,
    site_code       VARCHAR(20)  NOT NULL UNIQUE,
    location        VARCHAR(200),
    capacity        INTEGER,
    security_level  INTEGER      NOT NULL
);

CREATE TABLE containment_history (
    id              BIGSERIAL PRIMARY KEY,
    scp_id          BIGINT       NOT NULL,
    site_id         BIGINT       NOT NULL,
    moved_in        DATE         NOT NULL,
    moved_out       DATE
);

CREATE TABLE personnel (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(200) NOT NULL,
    position        VARCHAR(100) NOT NULL,
    clearance_level INTEGER      NOT NULL,
    base_site_id    BIGINT       NOT NULL
);

CREATE TABLE mtf_teams (
    id              BIGSERIAL PRIMARY KEY,
    callsign        VARCHAR(50)  NOT NULL UNIQUE,
    specialization  VARCHAR(200),
    base_site_id    BIGINT       NOT NULL
);

CREATE TABLE mtf_members (
    mtf_id          BIGINT       NOT NULL,
    personnel_id    BIGINT       NOT NULL,
    joined_at       DATE         NOT NULL,
    PRIMARY KEY (mtf_id, personnel_id)
);

CREATE TABLE incidents (
    id              BIGSERIAL PRIMARY KEY,
    occurred_at     TIMESTAMP    NOT NULL,
    scp_id          BIGINT       NOT NULL,
    site_id         BIGINT       NOT NULL,
    mtf_id          BIGINT,
    severity        INTEGER      NOT NULL,
    description     TEXT
);

CREATE TABLE procedure_revisions (
    id              BIGSERIAL PRIMARY KEY,
    scp_id          BIGINT       NOT NULL,
    revision_number INTEGER      NOT NULL,
    revision_date   DATE         NOT NULL,
    procedure_text  TEXT         NOT NULL,
    approved_by_id  BIGINT       NOT NULL,
    UNIQUE (scp_id, revision_number)
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    personnel_id    BIGINT       NOT NULL UNIQUE,
    login           VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   CHAR(64)     NOT NULL,
    salt            CHAR(32)     NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 2: Применить и проверить**

```powershell
psql -U scp_admin -d scp_foundation -f sql\postgres\01_schema.sql
```

Expected: `CREATE TABLE` × 9, без ошибок. Проверить через `\dt`.

- [ ] **Step 3: Коммит**

```powershell
git add sql\postgres\01_schema.sql
git commit -m "feat(db): postgres schema with 9 tables"
```

---

### Task 4: PostgreSQL — ограничения

**Files:**
- Create: `sql/postgres/02_constraints.sql`

- [ ] **Step 1: Написать CHECK и FK**

Содержимое `sql/postgres/02_constraints.sql`:

```sql
-- CHECK constraints
ALTER TABLE scp_objects
    ADD CONSTRAINT chk_object_class
    CHECK (object_class IN ('Safe', 'Euclid', 'Keter'));

ALTER TABLE containment_sites
    ADD CONSTRAINT chk_security_level
    CHECK (security_level BETWEEN 1 AND 5);

ALTER TABLE containment_history
    ADD CONSTRAINT chk_history_dates
    CHECK (moved_out IS NULL OR moved_out > moved_in);

ALTER TABLE personnel
    ADD CONSTRAINT chk_clearance_level
    CHECK (clearance_level BETWEEN 0 AND 5);

ALTER TABLE incidents
    ADD CONSTRAINT chk_severity
    CHECK (severity BETWEEN 1 AND 5);

ALTER TABLE users
    ADD CONSTRAINT chk_role
    CHECK (role IN ('O5', 'RESEARCHER'));

-- Foreign keys
ALTER TABLE containment_history
    ADD CONSTRAINT fk_history_scp FOREIGN KEY (scp_id) REFERENCES scp_objects(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_history_site FOREIGN KEY (site_id) REFERENCES containment_sites(id) ON DELETE RESTRICT;

ALTER TABLE personnel
    ADD CONSTRAINT fk_personnel_site FOREIGN KEY (base_site_id) REFERENCES containment_sites(id) ON DELETE RESTRICT;

ALTER TABLE mtf_teams
    ADD CONSTRAINT fk_mtf_site FOREIGN KEY (base_site_id) REFERENCES containment_sites(id) ON DELETE RESTRICT;

ALTER TABLE mtf_members
    ADD CONSTRAINT fk_member_mtf FOREIGN KEY (mtf_id) REFERENCES mtf_teams(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_member_personnel FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE;

ALTER TABLE incidents
    ADD CONSTRAINT fk_incident_scp FOREIGN KEY (scp_id) REFERENCES scp_objects(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_incident_site FOREIGN KEY (site_id) REFERENCES containment_sites(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_incident_mtf FOREIGN KEY (mtf_id) REFERENCES mtf_teams(id) ON DELETE SET NULL;

ALTER TABLE procedure_revisions
    ADD CONSTRAINT fk_proc_scp FOREIGN KEY (scp_id) REFERENCES scp_objects(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_proc_approver FOREIGN KEY (approved_by_id) REFERENCES personnel(id) ON DELETE RESTRICT;

ALTER TABLE users
    ADD CONSTRAINT fk_user_personnel FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE;
```

- [ ] **Step 2: Применить**

```powershell
psql -U scp_admin -d scp_foundation -f sql\postgres\02_constraints.sql
```

Expected: серия `ALTER TABLE` без ошибок.

- [ ] **Step 3: Коммит**

```powershell
git add sql\postgres\02_constraints.sql
git commit -m "feat(db): postgres CHECK and FK constraints"
```

---

---

## Полный индекс задач (46 задач, выполнять в порядке)

Из-за объёма плана дальнейшие фазы вынесены в отдельные файлы — содержат полный исполняемый код:

| Файл | Фаза | Задачи | Содержание |
|---|---|---|---|
| **part2-sql-seed.md** | 1 | 5–8 | Postgres seed, Firebird (schema + constraints + seed) |
| **part3-scripts.md** | 2 | 9–12 | setup/build/run для Linux+Windows с автозагрузкой JAR |
| **part4-infrastructure.md** | 3 | 13–15 | DbConfig, SqlDialect, ConnectionManager |
| **part5a-models.md** | 4 | 16 | 9 POJO records + 2 enums |
| **part5b-dao-core.md** | 5 | 17–21 | BaseDao, ScpObjectDao, SiteDao, PersonnelDao, UserDao |
| **part5c-dao-secondary.md** | 5 | 22–25 | History, MTF, Incident, ProcedureRevision DAO |
| **part6-auth.md** | 6 | 27–29 | PasswordHasher, AuthService, AccessControl |
| **part7-ui-foundation.md** | 7 | 30–36 | Main, CSS, анимации, Login, MainView, smoke-test |
| **part8a-ui-scp-sites.md** | 8 | 37–39 | ScpList, SiteList, PersonnelList |
| **part8b-ui-remaining.md** | 8 | 40–44 | MTF, Incidents, History, Procedures, Users |
| **part9-docs.md** | 9 | 45–47 | README, testing plan, Claude Design промт |

> Примечание: в нумерации есть пропуск номера 26 (объединял задачи) — порядок выполнения сохраняется.

## Критические зависимости между задачами

- **Task 27** (PasswordHasher) обновляет seed-файлы реальными SHA-256 хешами. До этого момента в Task 36 (smoke-тест входа) admin/scp-foundation работать НЕ будет — порядок имеет значение.
- **Task 36** требует наличия Main.java (Task 30) и заглушек контроллеров для экранов Phase 8.
- Каждая фаза должна завершаться компиляцией без ошибок.
