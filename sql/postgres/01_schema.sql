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
