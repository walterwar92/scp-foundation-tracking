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
