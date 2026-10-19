# Part 2: SQL — Seed + Firebird

Продолжение Phase 1. Tasks 5-8.

## Task 5: PostgreSQL — seed-данные

**Files:**
- Create: `sql/postgres/03_seed.sql`

- [ ] **Step 1: Написать INSERT-ы**

Содержимое `sql/postgres/03_seed.sql`. **Тестовый O5: login=`admin`, password=`scp-foundation`**. Соль и хеш сгенерированы заранее, чтобы скрипт был детерминированным:
- salt = `a1b2c3d4e5f6071829304a5b6c7d8e9f`
- password_hash = SHA-256(salt || "scp-foundation") = `b7f5e3a1c8d9602f4e7a1d3b9c8e2f0a6d5c4b3a9f8e7d6c5b4a3f2e1d0c9b8a7`

> ⚠️ Реальный хеш надо посчитать на этапе генерации (см. Task 27 `PasswordHasher`). Пока зашейте placeholder — после реализации `PasswordHasher` запустите утилиту `PasswordHasher.main()` (см. ту задачу), получите реальный хеш и обновите этот файл. Помечено комментарием в скрипте.

```sql
-- Containment sites (5 записей)
INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES
    ('Site-17',  'United States, classified',     50, 4),
    ('Site-19',  'United States, undisclosed',    80, 5),
    ('Area-14',  'Nevada, desert region',         30, 5),
    ('Site-66',  'Eastern Europe',                40, 3),
    ('Bio-Site-66','Bio-research wing, Site-66',  20, 4);

-- SCP objects (5 записей)
INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES
    ('SCP-173', 'The Sculpture',           'Euclid', 'Hostile concrete statue. Cannot move while observed.',           '1993-06-12'),
    ('SCP-682', 'Hard-to-Destroy Reptile', 'Keter',  'Highly adaptive reptilian entity. Multiple termination attempts.','1965-09-04'),
    ('SCP-049', 'Plague Doctor',           'Euclid', 'Humanoid in plague doctor attire. Believes it cures "the pestilence".','1935-03-19'),
    ('SCP-096', 'The Shy Guy',             'Euclid', 'Humanoid that responds violently to facial observation.',         '1989-07-22'),
    ('SCP-914', 'The Clockworks',          'Safe',   'Mechanical device transforming inputs based on setting.',          '1962-11-08');

-- Personnel (6 записей)
INSERT INTO personnel (full_name, position, clearance_level, base_site_id) VALUES
    ('Dr. Alto Clef',          'Senior Researcher',         5, 2),
    ('Dr. Simon Glass',        'Head of Psychology',        4, 2),
    ('Agent Daniel Navarro',   'Field Operative',           3, 1),
    ('Dr. Elena Volkov',       'Containment Specialist',    3, 4),
    ('Captain Marcus Hayes',   'Security Director',         4, 3),
    ('Researcher Yuki Tanaka', 'Junior Researcher',         2, 5);

-- MTF teams (5 записей)
INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES
    ('MTF Alpha-1',     'Red Right Hand — direct O5 enforcement',  2),
    ('MTF Epsilon-11',  'Nine-Tailed Fox — recontainment',         2),
    ('MTF Mu-4',        'Debuggers — informational anomalies',     1),
    ('MTF Nu-7',        'Hammer Down — heavy assault',             3),
    ('MTF Beta-7',      'Maz Hatters — chemical/biohazard',        5);

-- MTF members (6 записей)
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES
    (1, 1, '2020-01-15'),
    (2, 3, '2021-03-20'),
    (2, 5, '2019-08-01'),
    (3, 2, '2022-05-12'),
    (4, 5, '2020-11-30'),
    (5, 4, '2023-02-14');

-- Containment history (6 записей)
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES
    (1, 2, '1993-07-01', NULL),         -- SCP-173 в Site-19, текущее место
    (2, 3, '1965-10-15', '2010-04-22'), -- SCP-682, прежнее
    (2, 2, '2010-04-22', NULL),         -- SCP-682, текущее
    (3, 4, '1935-04-01', NULL),         -- SCP-049 в Site-66
    (4, 2, '1989-08-10', NULL),         -- SCP-096 в Site-19
    (5, 1, '1962-11-15', NULL);         -- SCP-914 в Site-17

-- Incidents (5 записей)
INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES
    ('2023-04-12 03:22:00', 1, 2, 2, 4, 'Containment breach during routine maintenance. 3 personnel casualties before recontainment.'),
    ('2023-07-08 14:15:00', 2, 2, 4, 5, 'Subject attempted breach. Heavy weapons deployed.'),
    ('2024-01-23 09:00:00', 3, 4, NULL, 2, 'Subject expressed agitation during interview. No physical incident.'),
    ('2024-03-15 22:40:00', 4, 2, 2, 3, 'Visual contact established with junior staff. One casualty.'),
    ('2024-09-30 11:00:00', 5, 1, NULL, 1, 'Setting dial malfunction during routine experiment. No anomalous effect.');

-- Procedure revisions (5 записей)
INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES
    (1, 1, '1993-07-15', 'Subject must be kept under direct observation by minimum 2 personnel at all times. Blinking shifts coordinated.', 1),
    (1, 2, '2010-06-22', 'Revised: minimum 3 personnel with overlapping observation windows. Camera redundancy required.', 1),
    (2, 1, '1965-10-20', 'Subject contained in reinforced cell. Standard armaments insufficient.', 2),
    (3, 1, '1935-04-05', 'Subject is cooperative within research contexts. Maintain dialogue protocols.', 2),
    (5, 1, '1962-12-01', 'Device operates without anomalous effect on operator. Refine setting documentation.', 1);

-- Users (5 записей: 1 O5 + 4 researchers с разным clearance)
-- ВАЖНО: hash для admin/scp-foundation. Реальное значение получить из PasswordHasher.main()
-- после Task 27. Сейчас placeholder.
INSERT INTO users (personnel_id, login, password_hash, salt, role) VALUES
    (1, 'admin',    'REPLACE_AFTER_TASK_27_FOR_admin',    'salt_admin_REPLACE_32_chars____', 'O5'),
    (2, 'sglass',   'REPLACE_AFTER_TASK_27_FOR_sglass',   'salt_sglass_REPLACE_32_chars___', 'RESEARCHER'),
    (3, 'dnavarro', 'REPLACE_AFTER_TASK_27_FOR_dnavarro', 'salt_dnavarro_REPLACE_32_chars_', 'RESEARCHER'),
    (4, 'evolkov',  'REPLACE_AFTER_TASK_27_FOR_evolkov',  'salt_evolkov_REPLACE_32_chars__', 'RESEARCHER'),
    (6, 'ytanaka',  'REPLACE_AFTER_TASK_27_FOR_ytanaka',  'salt_ytanaka_REPLACE_32_chars__', 'RESEARCHER');
```

- [ ] **Step 2: Применить (placeholder-версия)**

```powershell
psql -U scp_admin -d scp_foundation -f sql\postgres\03_seed.sql
```

Expected: 47 строк вставлено, без ошибок. Если CHECK сработал — проверить класс/уровень.

- [ ] **Step 3: Verify counts**

```powershell
psql -U scp_admin -d scp_foundation -c "SELECT 'scp_objects',COUNT(*) FROM scp_objects UNION ALL SELECT 'sites',COUNT(*) FROM containment_sites UNION ALL SELECT 'personnel',COUNT(*) FROM personnel UNION ALL SELECT 'mtf_teams',COUNT(*) FROM mtf_teams UNION ALL SELECT 'mtf_members',COUNT(*) FROM mtf_members UNION ALL SELECT 'history',COUNT(*) FROM containment_history UNION ALL SELECT 'incidents',COUNT(*) FROM incidents UNION ALL SELECT 'revisions',COUNT(*) FROM procedure_revisions UNION ALL SELECT 'users',COUNT(*) FROM users;"
```

Expected: каждая таблица ≥5 строк.

- [ ] **Step 4: Коммит**

```powershell
git add sql\postgres\03_seed.sql
git commit -m "feat(db): postgres seed data with 5+ rows per table"
```

> Хеши паролей будут обновлены в Task 27 после реализации `PasswordHasher`.

---

## Task 6: Firebird — схема

**Files:**
- Create: `sql/firebird/01_schema.sql`

- [ ] **Step 1: Написать DDL для Firebird 3.0+**

```sql
-- SCP Foundation Database — Firebird 3.0+ schema
-- Использует GENERATED ALWAYS AS IDENTITY вместо BIGSERIAL

CREATE TABLE scp_objects (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_number     VARCHAR(20)  NOT NULL UNIQUE,
    code_name       VARCHAR(200) NOT NULL,
    object_class    VARCHAR(10)  NOT NULL,
    description     BLOB SUB_TYPE TEXT,
    discovered_at   DATE
);

CREATE TABLE containment_sites (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    site_code       VARCHAR(20)  NOT NULL UNIQUE,
    location        VARCHAR(200),
    capacity        INTEGER,
    security_level  INTEGER      NOT NULL
);

CREATE TABLE containment_history (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scp_id          BIGINT       NOT NULL,
    site_id         BIGINT       NOT NULL,
    moved_in        DATE         NOT NULL,
    moved_out       DATE
);

CREATE TABLE personnel (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name       VARCHAR(200) NOT NULL,
    "POSITION"      VARCHAR(100) NOT NULL,
    clearance_level INTEGER      NOT NULL,
    base_site_id    BIGINT       NOT NULL
);

CREATE TABLE mtf_teams (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
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
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at     TIMESTAMP    NOT NULL,
    scp_id          BIGINT       NOT NULL,
    site_id         BIGINT       NOT NULL,
    mtf_id          BIGINT,
    severity        INTEGER      NOT NULL,
    description     BLOB SUB_TYPE TEXT
);

CREATE TABLE procedure_revisions (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scp_id          BIGINT       NOT NULL,
    revision_number INTEGER      NOT NULL,
    revision_date   DATE         NOT NULL,
    procedure_text  BLOB SUB_TYPE TEXT NOT NULL,
    approved_by_id  BIGINT       NOT NULL,
    UNIQUE (scp_id, revision_number)
);

CREATE TABLE users (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    personnel_id    BIGINT       NOT NULL UNIQUE,
    login           VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   CHAR(64)     NOT NULL,
    salt            CHAR(32)     NOT NULL,
    "ROLE"          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

> **Важно:** `position` и `role` — зарезервированные слова в Firebird, поэтому квотированы. В DAO для Firebird-диалекта тоже квотировать.

- [ ] **Step 2: Применить через isql**

```powershell
isql -u SYSDBA -p masterkey -i sql\firebird\01_schema.sql C:\firebird\scp_foundation.fdb
```

(Путь к БД — реальный путь к .fdb файлу).

- [ ] **Step 3: Коммит**

```powershell
git add sql\firebird\01_schema.sql
git commit -m "feat(db): firebird schema with 9 tables"
```

---

## Task 7: Firebird — ограничения

**Files:**
- Create: `sql/firebird/02_constraints.sql`

- [ ] **Step 1: Написать CHECK и FK**

```sql
-- CHECK constraints
ALTER TABLE scp_objects
    ADD CONSTRAINT chk_object_class CHECK (object_class IN ('Safe', 'Euclid', 'Keter'));

ALTER TABLE containment_sites
    ADD CONSTRAINT chk_security_level CHECK (security_level BETWEEN 1 AND 5);

ALTER TABLE containment_history
    ADD CONSTRAINT chk_history_dates CHECK (moved_out IS NULL OR moved_out > moved_in);

ALTER TABLE personnel
    ADD CONSTRAINT chk_clearance_level CHECK (clearance_level BETWEEN 0 AND 5);

ALTER TABLE incidents
    ADD CONSTRAINT chk_severity CHECK (severity BETWEEN 1 AND 5);

ALTER TABLE users
    ADD CONSTRAINT chk_role CHECK ("ROLE" IN ('O5', 'RESEARCHER'));

-- Foreign keys (Firebird не поддерживает множественный ADD CONSTRAINT в одном ALTER)
ALTER TABLE containment_history ADD CONSTRAINT fk_history_scp FOREIGN KEY (scp_id) REFERENCES scp_objects(id);
ALTER TABLE containment_history ADD CONSTRAINT fk_history_site FOREIGN KEY (site_id) REFERENCES containment_sites(id);

ALTER TABLE personnel ADD CONSTRAINT fk_personnel_site FOREIGN KEY (base_site_id) REFERENCES containment_sites(id);

ALTER TABLE mtf_teams ADD CONSTRAINT fk_mtf_site FOREIGN KEY (base_site_id) REFERENCES containment_sites(id);

ALTER TABLE mtf_members ADD CONSTRAINT fk_member_mtf FOREIGN KEY (mtf_id) REFERENCES mtf_teams(id) ON DELETE CASCADE;
ALTER TABLE mtf_members ADD CONSTRAINT fk_member_personnel FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE;

ALTER TABLE incidents ADD CONSTRAINT fk_incident_scp FOREIGN KEY (scp_id) REFERENCES scp_objects(id);
ALTER TABLE incidents ADD CONSTRAINT fk_incident_site FOREIGN KEY (site_id) REFERENCES containment_sites(id);
ALTER TABLE incidents ADD CONSTRAINT fk_incident_mtf FOREIGN KEY (mtf_id) REFERENCES mtf_teams(id) ON DELETE SET NULL;

ALTER TABLE procedure_revisions ADD CONSTRAINT fk_proc_scp FOREIGN KEY (scp_id) REFERENCES scp_objects(id);
ALTER TABLE procedure_revisions ADD CONSTRAINT fk_proc_approver FOREIGN KEY (approved_by_id) REFERENCES personnel(id);

ALTER TABLE users ADD CONSTRAINT fk_user_personnel FOREIGN KEY (personnel_id) REFERENCES personnel(id) ON DELETE CASCADE;
```

- [ ] **Step 2: Применить**

```powershell
isql -u SYSDBA -p masterkey -i sql\firebird\02_constraints.sql C:\firebird\scp_foundation.fdb
```

- [ ] **Step 3: Коммит**

```powershell
git add sql\firebird\02_constraints.sql
git commit -m "feat(db): firebird constraints"
```

---

## Task 8: Firebird — seed-данные

**Files:**
- Create: `sql/firebird/03_seed.sql`

- [ ] **Step 1: Написать INSERT-ы**

Идентичны по содержанию PostgreSQL-версии, но:
- Колонки `position` и `role` квотированы: `"POSITION"`, `"ROLE"`.
- Литералы дат остаются как `'1993-07-01'` (Firebird понимает ISO).

```sql
INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES ('Site-17',  'United States, classified',     50, 4);
INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES ('Site-19',  'United States, undisclosed',    80, 5);
INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES ('Area-14',  'Nevada, desert region',         30, 5);
INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES ('Site-66',  'Eastern Europe',                40, 3);
INSERT INTO containment_sites (site_code, location, capacity, security_level) VALUES ('Bio-Site-66','Bio-research wing, Site-66',  20, 4);

INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES ('SCP-173', 'The Sculpture',           'Euclid', 'Hostile concrete statue. Cannot move while observed.',           '1993-06-12');
INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES ('SCP-682', 'Hard-to-Destroy Reptile', 'Keter',  'Highly adaptive reptilian entity.',                              '1965-09-04');
INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES ('SCP-049', 'Plague Doctor',           'Euclid', 'Humanoid in plague doctor attire.',                               '1935-03-19');
INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES ('SCP-096', 'The Shy Guy',             'Euclid', 'Responds violently to facial observation.',                       '1989-07-22');
INSERT INTO scp_objects (item_number, code_name, object_class, description, discovered_at) VALUES ('SCP-914', 'The Clockworks',          'Safe',   'Mechanical device transforming inputs based on setting.',         '1962-11-08');

INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Dr. Alto Clef',          'Senior Researcher',         5, 2);
INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Dr. Simon Glass',        'Head of Psychology',        4, 2);
INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Agent Daniel Navarro',   'Field Operative',           3, 1);
INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Dr. Elena Volkov',       'Containment Specialist',    3, 4);
INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Captain Marcus Hayes',   'Security Director',         4, 3);
INSERT INTO personnel (full_name, "POSITION", clearance_level, base_site_id) VALUES ('Researcher Yuki Tanaka', 'Junior Researcher',         2, 5);

INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES ('MTF Alpha-1',     'Red Right Hand',          2);
INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES ('MTF Epsilon-11',  'Nine-Tailed Fox',         2);
INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES ('MTF Mu-4',        'Debuggers',               1);
INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES ('MTF Nu-7',        'Hammer Down',             3);
INSERT INTO mtf_teams (callsign, specialization, base_site_id) VALUES ('MTF Beta-7',      'Maz Hatters',             5);

INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (1, 1, '2020-01-15');
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (2, 3, '2021-03-20');
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (2, 5, '2019-08-01');
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (3, 2, '2022-05-12');
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (4, 5, '2020-11-30');
INSERT INTO mtf_members (mtf_id, personnel_id, joined_at) VALUES (5, 4, '2023-02-14');

INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (1, 2, '1993-07-01', NULL);
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (2, 3, '1965-10-15', '2010-04-22');
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (2, 2, '2010-04-22', NULL);
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (3, 4, '1935-04-01', NULL);
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (4, 2, '1989-08-10', NULL);
INSERT INTO containment_history (scp_id, site_id, moved_in, moved_out) VALUES (5, 1, '1962-11-15', NULL);

INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES ('2023-04-12 03:22:00', 1, 2, 2, 4, 'Containment breach. 3 casualties.');
INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES ('2023-07-08 14:15:00', 2, 2, 4, 5, 'Subject attempted breach.');
INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES ('2024-01-23 09:00:00', 3, 4, NULL, 2, 'Subject expressed agitation during interview.');
INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES ('2024-03-15 22:40:00', 4, 2, 2, 3, 'Visual contact with junior staff. One casualty.');
INSERT INTO incidents (occurred_at, scp_id, site_id, mtf_id, severity, description) VALUES ('2024-09-30 11:00:00', 5, 1, NULL, 1, 'Setting dial malfunction.');

INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES (1, 1, '1993-07-15', 'Min. 2 personnel observation.', 1);
INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES (1, 2, '2010-06-22', 'Min. 3 personnel + camera redundancy.', 1);
INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES (2, 1, '1965-10-20', 'Reinforced cell required.', 2);
INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES (3, 1, '1935-04-05', 'Dialogue protocols.', 2);
INSERT INTO procedure_revisions (scp_id, revision_number, revision_date, procedure_text, approved_by_id) VALUES (5, 1, '1962-12-01', 'Refine setting documentation.', 1);

-- Хеши обновить после Task 27
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (1, 'admin',    'REPLACE_AFTER_TASK_27_FOR_admin',    'salt_admin_REPLACE_32_chars____', 'O5');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (2, 'sglass',   'REPLACE_AFTER_TASK_27_FOR_sglass',   'salt_sglass_REPLACE_32_chars___', 'RESEARCHER');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (3, 'dnavarro', 'REPLACE_AFTER_TASK_27_FOR_dnavarro', 'salt_dnavarro_REPLACE_32_chars_', 'RESEARCHER');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (4, 'evolkov',  'REPLACE_AFTER_TASK_27_FOR_evolkov',  'salt_evolkov_REPLACE_32_chars__', 'RESEARCHER');
INSERT INTO users (personnel_id, login, password_hash, salt, "ROLE") VALUES (6, 'ytanaka',  'REPLACE_AFTER_TASK_27_FOR_ytanaka',  'salt_ytanaka_REPLACE_32_chars__', 'RESEARCHER');

COMMIT;
```

- [ ] **Step 2: Применить и проверить counts**

```powershell
isql -u SYSDBA -p masterkey -i sql\firebird\03_seed.sql C:\firebird\scp_foundation.fdb
```

- [ ] **Step 3: Коммит**

```powershell
git add sql\firebird\03_seed.sql
git commit -m "feat(db): firebird seed data"
```
