-- V1__init_core_tables.sql
-- Расширения
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ============================================================
-- ORGANIZATIONS
-- ============================================================
CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    legal_name      VARCHAR(512),
    bin             VARCHAR(12),
    address         TEXT,
    phone           VARCHAR(50),
    email           VARCHAR(255),
    logo_url        VARCHAR(512),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ
);

-- ============================================================
-- BRANCHES
-- ============================================================
CREATE TABLE branches (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    name                VARCHAR(255) NOT NULL,
    address             TEXT,
    phone               VARCHAR(50),
    head_name           VARCHAR(255),
    design_capacity     INT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_branches_organization_id ON branches(organization_id);

-- ============================================================
-- ROLES & PERMISSIONS
-- ============================================================
CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(100) NOT NULL UNIQUE,
    name_ru         VARCHAR(255) NOT NULL,
    name_kk         VARCHAR(255) NOT NULL,
    description     TEXT,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO roles (id, code, name_ru, name_kk, is_system) VALUES
    (uuid_generate_v4(), 'SYSTEM_ADMIN',    'Системный администратор', 'Жүйелік әкімші',             TRUE),
    (uuid_generate_v4(), 'FOUNDER',         'Учредитель',              'Құрылтайшы',                  TRUE),
    (uuid_generate_v4(), 'DIRECTOR',        'Руководитель',            'Басшы',                       TRUE),
    (uuid_generate_v4(), 'METHODIST',       'Методист',                'Әдіскер',                     TRUE),
    (uuid_generate_v4(), 'EDUCATOR',        'Воспитатель',             'Тәрбиеші',                    TRUE),
    (uuid_generate_v4(), 'KAZ_TEACHER',     'Педагог казахского языка','Қазақ тілі мұғалімі',         TRUE),
    (uuid_generate_v4(), 'MUSIC_TEACHER',   'Музыкальный руководитель','Музыка жетекшісі',            TRUE),
    (uuid_generate_v4(), 'PE_INSTRUCTOR',   'Инструктор по физкультуре','Дене шынықтыру нұсқаушысы', TRUE),
    (uuid_generate_v4(), 'NURSE',           'Медицинская сестра',      'Медбике',                     TRUE),
    (uuid_generate_v4(), 'JANITOR',         'Завхоз',                  'Шаруашылық меңгерушісі',      TRUE),
    (uuid_generate_v4(), 'ACCOUNTANT',      'Бухгалтер',               'Бухгалтер',                   TRUE),
    (uuid_generate_v4(), 'PARENT',          'Родитель',                'Ата-ана',                      TRUE);

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    email               VARCHAR(255) NOT NULL UNIQUE,
    phone               VARCHAR(50),
    password_hash       VARCHAR(255) NOT NULL,
    first_name          VARCHAR(255) NOT NULL,
    last_name           VARCHAR(255) NOT NULL,
    middle_name         VARCHAR(255),
    role_code           VARCHAR(100) NOT NULL REFERENCES roles(code),
    photo_url           VARCHAR(512),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at       TIMESTAMPTZ,
    failed_login_count  INT NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    preferred_language  VARCHAR(2) NOT NULL DEFAULT 'ru',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_users_organization_id ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_code ON users(role_code);

-- User-Branch assignments (many-to-many for multi-branch access)
CREATE TABLE user_branch_assignments (
    user_id     UUID NOT NULL REFERENCES users(id),
    branch_id   UUID NOT NULL REFERENCES branches(id),
    PRIMARY KEY (user_id, branch_id)
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMPTZ,
    ip_address  VARCHAR(50),
    user_agent  TEXT
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ============================================================
-- GROUPS
-- ============================================================
CREATE TABLE groups (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    branch_id           UUID NOT NULL REFERENCES branches(id),
    name                VARCHAR(255) NOT NULL,
    age_from_months     INT,
    age_to_months       INT,
    language            VARCHAR(2) NOT NULL DEFAULT 'ru', -- ru/kk
    group_type          VARCHAR(50),  -- full_day / short_day / mixed_age / special
    educator_id         UUID REFERENCES users(id),
    educator_phone      VARCHAR(50),
    educator_email      VARCHAR(255),
    educator_info       TEXT,
    educator_photo_url  VARCHAR(512),
    academic_year       VARCHAR(9),   -- e.g. 2025-2026
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_groups_branch_id ON groups(branch_id);
CREATE INDEX idx_groups_organization_id ON groups(organization_id);

-- Specialist-Group assignments
CREATE TABLE specialist_group_assignments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    specialist_id   UUID NOT NULL REFERENCES users(id),
    group_id        UUID NOT NULL REFERENCES groups(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(specialist_id, group_id)
);

-- ============================================================
-- CHILDREN (CONTINGENT)
-- ============================================================
CREATE TABLE children (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    branch_id           UUID NOT NULL REFERENCES branches(id),
    group_id            UUID REFERENCES groups(id),
    last_name           VARCHAR(255) NOT NULL,
    first_name          VARCHAR(255) NOT NULL,
    middle_name         VARCHAR(255),
    birth_date          DATE NOT NULL,
    gender              VARCHAR(10), -- male / female
    iin                 VARCHAR(12),
    photo_url           VARCHAR(512),
    -- Enrollment
    admission_date      DATE,
    admission_order_num VARCHAR(100),
    admission_order_date DATE,
    -- Discharge
    discharge_date      DATE,
    discharge_order_num VARCHAR(100),
    discharge_order_date DATE,
    discharge_reason    TEXT,
    status              VARCHAR(30) NOT NULL DEFAULT 'active', -- active / transferred / discharged / graduated
    notes               TEXT,
    -- Parent info (filled before parent account created)
    parent_name         VARCHAR(255),
    parent_phone        VARCHAR(50),
    parent_email        VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_children_organization_id ON children(organization_id);
CREATE INDEX idx_children_branch_id ON children(branch_id);
CREATE INDEX idx_children_group_id ON children(group_id);
CREATE INDEX idx_children_status ON children(status);

-- Child movement history
CREATE TABLE child_movements (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    child_id            UUID NOT NULL REFERENCES children(id),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    from_branch_id      UUID REFERENCES branches(id),
    to_branch_id        UUID REFERENCES branches(id),
    from_group_id       UUID REFERENCES groups(id),
    to_group_id         UUID REFERENCES groups(id),
    movement_type       VARCHAR(30) NOT NULL, -- admission / transfer / discharge / graduation
    order_number        VARCHAR(100),
    order_date          DATE,
    effective_date      DATE NOT NULL,
    reason              TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID
);
CREATE INDEX idx_child_movements_child_id ON child_movements(child_id);

-- ============================================================
-- PARENT ACCOUNTS & INVITATIONS
-- ============================================================
CREATE TABLE parent_invitations (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    child_id            UUID NOT NULL REFERENCES children(id),
    invite_code         VARCHAR(50) NOT NULL UNIQUE,
    phone               VARCHAR(50),
    created_by          UUID NOT NULL REFERENCES users(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'active', -- active / used / revoked
    used_at             TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_parent_invitations_code ON parent_invitations(invite_code);
CREATE INDEX idx_parent_invitations_child_id ON parent_invitations(child_id);

CREATE TABLE parent_accounts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id),
    child_id            UUID NOT NULL REFERENCES children(id),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    invitation_id       UUID REFERENCES parent_invitations(id),
    consent_given       BOOLEAN NOT NULL DEFAULT FALSE,
    consent_date        TIMESTAMPTZ,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_parent_accounts_user_child ON parent_accounts(user_id, child_id);
