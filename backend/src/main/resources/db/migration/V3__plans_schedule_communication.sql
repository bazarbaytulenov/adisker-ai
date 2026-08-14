-- V3__plans_schedule_communication.sql

-- ============================================================
-- ANNUAL PLANS
-- ============================================================
CREATE TABLE annual_plans (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    academic_year   VARCHAR(9) NOT NULL,
    language        VARCHAR(2) NOT NULL DEFAULT 'ru',
    title           VARCHAR(512),
    status          VARCHAR(20) NOT NULL DEFAULT 'draft', -- draft / approved
    approved_by     UUID REFERENCES users(id),
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    UNIQUE(branch_id, academic_year, language)
);

CREATE TABLE annual_plan_sections (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    annual_plan_id  UUID NOT NULL REFERENCES annual_plans(id) ON DELETE CASCADE,
    title           VARCHAR(512) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0
);

CREATE TABLE annual_plan_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id      UUID NOT NULL REFERENCES annual_plan_sections(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    title           TEXT NOT NULL,
    event_form      VARCHAR(255),
    participants    TEXT,
    deadline        DATE,
    responsible     TEXT,
    notes           TEXT,
    month           INT,
    is_propagated   BOOLEAN NOT NULL DEFAULT FALSE -- propagated to monthly plans
);
CREATE INDEX idx_annual_events_section ON annual_plan_events(section_id);

-- ============================================================
-- MONTHLY PLANS
-- ============================================================
CREATE TABLE monthly_plans (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    annual_plan_id  UUID REFERENCES annual_plans(id),
    year            INT NOT NULL,
    month           INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    language        VARCHAR(2) NOT NULL DEFAULT 'ru',
    status          VARCHAR(20) NOT NULL DEFAULT 'draft',
    approved_by     UUID REFERENCES users(id),
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    UNIQUE(branch_id, year, month, language)
);

CREATE TABLE monthly_plan_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    monthly_plan_id UUID NOT NULL REFERENCES monthly_plans(id) ON DELETE CASCADE,
    annual_event_id UUID REFERENCES annual_plan_events(id),
    title           TEXT NOT NULL,
    event_form      VARCHAR(255),
    participants    TEXT,
    event_date      DATE,
    responsible     TEXT,
    notes           TEXT
);

-- ============================================================
-- PROTOCOLS
-- ============================================================
CREATE TABLE protocols (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    protocol_type   VARCHAR(50) NOT NULL, -- pedagogical / methodical / parents / guardian / ethics
    number          VARCHAR(50),
    protocol_date   DATE NOT NULL,
    language        VARCHAR(2) NOT NULL DEFAULT 'ru',
    chairman        TEXT,
    secretary       TEXT,
    attendees       TEXT,
    agenda          TEXT,
    heard           JSONB, -- [{topic, speaker, content}]
    speakers        JSONB,
    decisions       JSONB, -- [{decision, responsible, deadline}]
    status          VARCHAR(20) NOT NULL DEFAULT 'draft',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID
);
CREATE INDEX idx_protocols_branch ON protocols(branch_id);
CREATE INDEX idx_protocols_type ON protocols(protocol_type);

-- ============================================================
-- PROSPECTIVE PLANS (ПЕРСПЕКТИВНЫЙ ПЛАН)
-- ============================================================
CREATE TABLE prospective_plans (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    group_id        UUID NOT NULL REFERENCES groups(id),
    academic_year   VARCHAR(9) NOT NULL,
    month           INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    week            INT NOT NULL CHECK (week BETWEEN 1 AND 5),
    theme           VARCHAR(512),
    language        VARCHAR(2) NOT NULL DEFAULT 'ru',
    overall_status  VARCHAR(20) NOT NULL DEFAULT 'draft',
    fill_pct        NUMERIC(5,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    UNIQUE(group_id, academic_year, month, week, language)
);
CREATE INDEX idx_prospective_plans_group ON prospective_plans(group_id);
CREATE INDEX idx_prospective_plans_branch ON prospective_plans(branch_id);

-- Each educational domain/subject area of the plan
CREATE TABLE prospective_plan_sections (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id             UUID NOT NULL REFERENCES prospective_plans(id) ON DELETE CASCADE,
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    domain              VARCHAR(100) NOT NULL, -- e.g. kaz_language / music / physical / educator
    domain_name_ru      VARCHAR(255),
    domain_name_kk      VARCHAR(255),
    owner_role          VARCHAR(100), -- role_code of owner
    owner_user_id       UUID REFERENCES users(id),
    content             TEXT,
    objectives          TEXT,
    materials           TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'draft', -- draft/review/approved/returned
    submitted_at        TIMESTAMPTZ,
    approved_at         TIMESTAMPTZ,
    approved_by         UUID REFERENCES users(id),
    returned_at         TIMESTAMPTZ,
    return_comment      TEXT,
    version             INT NOT NULL DEFAULT 1,
    sort_order          INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by          UUID
);
CREATE INDEX idx_pp_sections_plan ON prospective_plan_sections(plan_id);
CREATE INDEX idx_pp_sections_domain ON prospective_plan_sections(domain);

-- Role-level permissions per section
CREATE TABLE prospective_plan_permissions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id      UUID NOT NULL REFERENCES prospective_plan_sections(id) ON DELETE CASCADE,
    role_code       VARCHAR(100) NOT NULL REFERENCES roles(code),
    can_view        BOOLEAN NOT NULL DEFAULT TRUE,
    can_edit        BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(section_id, role_code)
);

-- Version history per section
CREATE TABLE prospective_plan_versions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id      UUID NOT NULL REFERENCES prospective_plan_sections(id),
    version         INT NOT NULL,
    content         TEXT,
    objectives      TEXT,
    materials       TEXT,
    changed_by      UUID NOT NULL REFERENCES users(id),
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    change_comment  TEXT
);
CREATE INDEX idx_pp_versions_section ON prospective_plan_versions(section_id);

-- Comments from methodist
CREATE TABLE prospective_plan_comments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id      UUID NOT NULL REFERENCES prospective_plan_sections(id),
    author_id       UUID NOT NULL REFERENCES users(id),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    comment_text    TEXT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_pp_comments_section ON prospective_plan_comments(section_id);

-- Concurrency locks
CREATE TABLE prospective_plan_locks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id      UUID NOT NULL UNIQUE REFERENCES prospective_plan_sections(id),
    locked_by       UUID NOT NULL REFERENCES users(id),
    locked_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL
);

-- ============================================================
-- CYCLOGRAM
-- ============================================================
CREATE TABLE cyclograms (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    group_id        UUID NOT NULL REFERENCES groups(id),
    academic_year   VARCHAR(9) NOT NULL,
    month           INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    week            INT NOT NULL CHECK (week BETWEEN 1 AND 5),
    language        VARCHAR(2) NOT NULL DEFAULT 'ru',
    content         JSONB,    -- structured day-by-day content
    plan_section_id UUID REFERENCES prospective_plan_sections(id),
    generated_by_ai BOOLEAN NOT NULL DEFAULT FALSE,
    status          VARCHAR(20) NOT NULL DEFAULT 'draft',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID
);
CREATE INDEX idx_cyclogram_group ON cyclograms(group_id);

-- ============================================================
-- ROUTINES (РЕЖИМ ДНЯ)
-- ============================================================
CREATE TABLE routines (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    group_id        UUID NOT NULL REFERENCES groups(id),
    academic_year   VARCHAR(9) NOT NULL,
    language        VARCHAR(2) NOT NULL DEFAULT 'ru',
    approval_info   TEXT,
    group_name      VARCHAR(255),
    items           JSONB NOT NULL DEFAULT '[]', -- [{time: "07:00", activity: "..."}]
    is_published    BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID,
    UNIQUE(group_id, academic_year, language)
);
CREATE INDEX idx_routines_group ON routines(group_id);

-- ============================================================
-- SCHEDULES (РАСПИСАНИЕ ОД)
-- ============================================================
CREATE TABLE schedules (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    group_id        UUID NOT NULL REFERENCES groups(id),
    academic_year   VARCHAR(9) NOT NULL,
    language        VARCHAR(2) NOT NULL DEFAULT 'ru',
    approval_info   TEXT,
    is_published    BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID,
    UNIQUE(group_id, academic_year, language)
);
CREATE INDEX idx_schedules_group ON schedules(group_id);

CREATE TABLE schedule_entries (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    schedule_id         UUID NOT NULL REFERENCES schedules(id) ON DELETE CASCADE,
    day_of_week         INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 5), -- 1=Mon..5=Fri
    start_time          TIME NOT NULL,
    end_time            TIME,
    subject             VARCHAR(255) NOT NULL,
    educator_id         UUID REFERENCES users(id),
    educator_role       VARCHAR(100),
    notes               TEXT
);
CREATE INDEX idx_schedule_entries_schedule ON schedule_entries(schedule_id);

-- ============================================================
-- DAILY POSTS (ИНФОРМАЦИЯ ЗА ДЕНЬ)
-- ============================================================
CREATE TABLE daily_posts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    group_id        UUID NOT NULL REFERENCES groups(id),
    post_date       DATE NOT NULL,
    theme           VARCHAR(512),
    description     TEXT,
    home_tasks      TEXT,
    is_published    BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    UNIQUE(group_id, post_date)
);
CREATE INDEX idx_daily_posts_group ON daily_posts(group_id);
CREATE INDEX idx_daily_posts_date ON daily_posts(post_date);

CREATE TABLE daily_post_photos (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    daily_post_id   UUID NOT NULL REFERENCES daily_posts(id) ON DELETE CASCADE,
    file_url        VARCHAR(512) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- CHAT
-- ============================================================
CREATE TABLE chat_threads (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    child_id        UUID NOT NULL REFERENCES children(id),
    educator_id     UUID NOT NULL REFERENCES users(id),
    parent_user_id  UUID REFERENCES users(id),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(child_id, educator_id)
);
CREATE INDEX idx_chat_threads_child ON chat_threads(child_id);
CREATE INDEX idx_chat_threads_educator ON chat_threads(educator_id);

CREATE TABLE chat_messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    thread_id       UUID NOT NULL REFERENCES chat_threads(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL REFERENCES users(id),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    content         TEXT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_chat_messages_thread ON chat_messages(thread_id);
CREATE INDEX idx_chat_messages_created ON chat_messages(created_at DESC);
