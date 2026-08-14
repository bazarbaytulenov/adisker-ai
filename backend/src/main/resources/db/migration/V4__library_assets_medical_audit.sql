-- V4__library_assets_medical_audit.sql

-- ============================================================
-- LIBRARY FUND
-- ============================================================
CREATE TABLE library_fund (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    branch_id           UUID NOT NULL REFERENCES branches(id),
    age_group           VARCHAR(50),
    language            VARCHAR(2) NOT NULL DEFAULT 'ru',
    publication_type    VARCHAR(100),   -- textbook / fiction / methodical / etc.
    title               VARCHAR(512) NOT NULL,
    authors             TEXT,
    publisher           VARCHAR(255),
    publish_year        INT,
    inventory_number    VARCHAR(100),
    quantity            INT NOT NULL DEFAULT 1,
    receipt_date        DATE,
    is_electronic       BOOLEAN NOT NULL DEFAULT FALSE,
    order_number        VARCHAR(100),   -- order 216 ref
    list_item           VARCHAR(255),   -- перечень пункт
    check_date          DATE,
    doc_link            VARCHAR(512),
    condition           VARCHAR(50) NOT NULL DEFAULT 'good', -- good / repair / write_off
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_library_fund_branch ON library_fund(branch_id);

-- ============================================================
-- MATERIAL ASSETS (МТБ)
-- ============================================================
CREATE TABLE material_assets (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES organizations(id),
    branch_id           UUID NOT NULL REFERENCES branches(id),
    room                VARCHAR(255),
    age_group           VARCHAR(50),
    category            VARCHAR(255),
    name                VARCHAR(512) NOT NULL,
    unit                VARCHAR(50),
    norm_qty            INT,
    actual_qty          INT NOT NULL DEFAULT 0,
    working_qty         INT NOT NULL DEFAULT 0,
    repair_qty          INT NOT NULL DEFAULT 0,
    write_off_qty       INT NOT NULL DEFAULT 0,
    shortage            INT GENERATED ALWAYS AS (GREATEST(0, COALESCE(norm_qty,0) - actual_qty)) STORED,
    supply_pct          NUMERIC(5,2),
    inventory_number    VARCHAR(100),
    purchase_date       DATE,
    responsible_person  VARCHAR(255),
    purchase_plan       TEXT,
    purchase_deadline   DATE,
    status              VARCHAR(30) NOT NULL DEFAULT 'sufficient', -- sufficient / insufficient / absent
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_material_assets_branch ON material_assets(branch_id);

-- ============================================================
-- MEDICAL JOURNALS
-- ============================================================
CREATE TABLE medical_journals (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    journal_type    VARCHAR(50) NOT NULL, -- menu / vaccination / height_weight / brakerage / daily_sample / fridge
    journal_date    DATE NOT NULL,
    data            JSONB NOT NULL DEFAULT '{}',
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID
);
CREATE INDEX idx_medical_journals_branch ON medical_journals(branch_id);
CREATE INDEX idx_medical_journals_type ON medical_journals(journal_type);

-- ============================================================
-- MANAGER / DIRECTOR ORDERS
-- ============================================================
CREATE TABLE manager_orders (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    order_type      VARCHAR(50) NOT NULL, -- child_admission / child_transfer / child_discharge / child_graduation / staff_hire / staff_transfer / staff_dismiss
    order_number    VARCHAR(100),
    order_date      DATE NOT NULL,
    subject         TEXT NOT NULL,
    content         TEXT,
    related_child_id UUID REFERENCES children(id),
    related_user_id  UUID REFERENCES users(id),
    language        VARCHAR(2) NOT NULL DEFAULT 'ru',
    status          VARCHAR(20) NOT NULL DEFAULT 'draft',
    signed_by       UUID REFERENCES users(id),
    signed_at       TIMESTAMPTZ,
    file_url        VARCHAR(512),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_manager_orders_branch ON manager_orders(branch_id);
CREATE INDEX idx_manager_orders_type ON manager_orders(order_type);

-- Staff work schedules / tabels
CREATE TABLE staff_attendance (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id       UUID NOT NULL REFERENCES branches(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    year            INT NOT NULL,
    month           INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    marks           JSONB NOT NULL DEFAULT '{}', -- {day: mark}
    worked_days     INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, year, month)
);

-- ============================================================
-- FILES
-- ============================================================
CREATE TABLE files (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    uploaded_by     UUID NOT NULL REFERENCES users(id),
    file_name       VARCHAR(512) NOT NULL,
    original_name   VARCHAR(512) NOT NULL,
    mime_type       VARCHAR(100),
    size_bytes      BIGINT,
    storage_path    VARCHAR(1024) NOT NULL,
    storage_type    VARCHAR(20) NOT NULL DEFAULT 'local', -- local / s3
    entity_type     VARCHAR(100), -- daily_post / child_photo / protocol / etc.
    entity_id       UUID,
    is_public       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_files_entity ON files(entity_type, entity_id);
CREATE INDEX idx_files_org ON files(organization_id);

-- ============================================================
-- AUDIT LOG
-- ============================================================
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID REFERENCES organizations(id),
    branch_id       UUID REFERENCES branches(id),
    user_id         UUID REFERENCES users(id),
    user_email      VARCHAR(255),
    action          VARCHAR(50) NOT NULL, -- LOGIN / LOGOUT / CREATE / UPDATE / DELETE / PRINT / EXPORT / APPROVE / REJECT
    entity_type     VARCHAR(100),
    entity_id       UUID,
    old_values      JSONB,
    new_values      JSONB,
    ip_address      VARCHAR(50),
    user_agent      TEXT,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_org ON audit_logs(organization_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);

-- ============================================================
-- NOMENCLATURE
-- ============================================================
CREATE TABLE nomenclature (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    index_code      VARCHAR(50),
    title           TEXT NOT NULL,
    retention_period VARCHAR(100),
    notes           TEXT,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_nomenclature_org ON nomenclature(organization_id);
