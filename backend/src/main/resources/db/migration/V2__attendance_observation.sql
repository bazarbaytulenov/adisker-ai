-- V2__attendance_observation.sql

-- ============================================================
-- ATTENDANCE
-- ============================================================
CREATE TABLE attendance_months (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    group_id        UUID NOT NULL REFERENCES groups(id),
    year            INT NOT NULL,
    month           INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    working_days    INT,
    is_closed       BOOLEAN NOT NULL DEFAULT FALSE,
    closed_by       UUID REFERENCES users(id),
    closed_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    UNIQUE(group_id, year, month)
);
CREATE INDEX idx_attendance_months_group ON attendance_months(group_id);
CREATE INDEX idx_attendance_months_branch ON attendance_months(branch_id);

CREATE TABLE attendance_marks (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    attendance_month_id UUID NOT NULL REFERENCES attendance_months(id),
    child_id            UUID NOT NULL REFERENCES children(id),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    day                 INT NOT NULL CHECK (day BETWEEN 1 AND 31),
    mark                VARCHAR(5), -- '1' present / 'б' sick / 'о' vacation / NULL absent
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by          UUID,
    UNIQUE(attendance_month_id, child_id, day)
);
CREATE INDEX idx_attendance_marks_month ON attendance_marks(attendance_month_id);
CREATE INDEX idx_attendance_marks_child ON attendance_marks(child_id);

-- ============================================================
-- CHARGES & PAYMENTS
-- ============================================================
CREATE TABLE charges (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    branch_id           UUID NOT NULL REFERENCES branches(id),
    child_id            UUID NOT NULL REFERENCES children(id),
    attendance_month_id UUID REFERENCES attendance_months(id),
    year                INT NOT NULL,
    month               INT NOT NULL,
    days_attended       INT NOT NULL DEFAULT 0,
    days_absent         INT NOT NULL DEFAULT 0,
    daily_rate          NUMERIC(10,2),
    amount_charged      NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount            NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_reason     TEXT,
    balance_before      NUMERIC(12,2) NOT NULL DEFAULT 0,
    unique_charge_id    VARCHAR(100) UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID
);
CREATE INDEX idx_charges_child_id ON charges(child_id);
CREATE INDEX idx_charges_organization_id ON charges(organization_id);

CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    child_id            UUID NOT NULL REFERENCES children(id),
    charge_id           UUID REFERENCES charges(id),
    amount              NUMERIC(12,2) NOT NULL,
    payment_method      VARCHAR(50), -- kaspi_qr / kaspi_link / cash / transfer
    payment_date        DATE,
    kaspi_txn_id        VARCHAR(255),
    status              VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending / confirmed / cancelled
    confirmed_by        UUID REFERENCES users(id),
    confirmed_at        TIMESTAMPTZ,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID
);
CREATE INDEX idx_payments_child_id ON payments(child_id);
CREATE INDEX idx_payments_charge_id ON payments(charge_id);

-- ============================================================
-- OBSERVATIONS
-- ============================================================
CREATE TABLE observation_indicators (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    age_group       VARCHAR(50) NOT NULL, -- e.g. '3-4', '4-5', '5-6', '6-7'
    domain          VARCHAR(255) NOT NULL, -- educational domain / subject
    criterion       VARCHAR(512) NOT NULL,
    indicator       TEXT NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_obs_indicators_org ON observation_indicators(organization_id);
CREATE INDEX idx_obs_indicators_age ON observation_indicators(age_group);

CREATE TABLE observations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    group_id        UUID NOT NULL REFERENCES groups(id),
    child_id        UUID NOT NULL REFERENCES children(id),
    period          VARCHAR(20) NOT NULL, -- start / mid / final
    academic_year   VARCHAR(9) NOT NULL,
    filled_by       UUID REFERENCES users(id),
    is_complete     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    UNIQUE(child_id, period, academic_year)
);
CREATE INDEX idx_observations_child ON observations(child_id);
CREATE INDEX idx_observations_group ON observations(group_id);

CREATE TABLE observation_results (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    observation_id  UUID NOT NULL REFERENCES observations(id) ON DELETE CASCADE,
    indicator_id    UUID NOT NULL REFERENCES observation_indicators(id),
    level           VARCHAR(5), -- В (high) / С (mid) / Н (low)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(observation_id, indicator_id)
);
CREATE INDEX idx_obs_results_observation ON observation_results(observation_id);

-- ============================================================
-- GAMES CATALOG (for IKR)
-- ============================================================
CREATE TABLE games_catalog (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    age_group       VARCHAR(50) NOT NULL,
    period          VARCHAR(20) NOT NULL, -- start / mid / final
    domain          VARCHAR(255),
    game_name       TEXT NOT NULL,
    objectives      TEXT,
    procedure       TEXT,
    language        VARCHAR(2) NOT NULL DEFAULT 'ru',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID
);
CREATE INDEX idx_games_catalog_org ON games_catalog(organization_id);
CREATE INDEX idx_games_catalog_age ON games_catalog(age_group, period);

-- ============================================================
-- INDIVIDUAL DEVELOPMENT CARDS (IKR)
-- ============================================================
CREATE TABLE individual_cards (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    child_id        UUID NOT NULL REFERENCES children(id),
    observation_id  UUID NOT NULL REFERENCES observations(id),
    game_id         UUID REFERENCES games_catalog(id),
    game_name       TEXT,
    game_objectives TEXT,
    game_procedure  TEXT,
    custom_notes    TEXT,
    language        VARCHAR(2) NOT NULL DEFAULT 'ru',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    UNIQUE(child_id, observation_id)
);
CREATE INDEX idx_individual_cards_child ON individual_cards(child_id);

-- ============================================================
-- METHODIST SUMMARIES
-- ============================================================
CREATE TABLE methodist_summaries (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    group_id        UUID REFERENCES groups(id),
    period          VARCHAR(20) NOT NULL,
    academic_year   VARCHAR(9) NOT NULL,
    domain          VARCHAR(255) NOT NULL,
    age_group       VARCHAR(50),
    total_children  INT NOT NULL DEFAULT 0,
    high_count      INT NOT NULL DEFAULT 0,
    mid_count       INT NOT NULL DEFAULT 0,
    low_count       INT NOT NULL DEFAULT 0,
    high_pct        NUMERIC(5,2),
    mid_pct         NUMERIC(5,2),
    low_pct         NUMERIC(5,2),
    calculated_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_summaries_branch ON methodist_summaries(branch_id);
CREATE INDEX idx_summaries_group ON methodist_summaries(group_id);
