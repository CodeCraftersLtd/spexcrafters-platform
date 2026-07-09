-- V4: supplier onboarding, verification, evidence and platform-staff contexts (Phase 7).
-- Conventions per docs/database/domain-model.md §I.1 (see V1/V2): uuid PKs (UUIDv7 from the
-- application), audit columns on business tables, snake_case, timestamptz, schema per bounded
-- context. Cross-module table access is forbidden in code (ArchUnit); cross-schema FKs are a
-- database-level integrity net only. Taxonomy labels are NOT stored — the UI translates codes.

-- ============================================================================ reference
CREATE SCHEMA IF NOT EXISTS reference;

-- Supported locales mirror docs/architecture/supported-locales.md and the backend
-- SupportedLocale enum; translation.locale columns FK here for integrity.
CREATE TABLE reference.supported_locale (
    code       varchar(16) PRIMARY KEY,
    dir        varchar(3)  NOT NULL,
    fallback   varchar(16) REFERENCES reference.supported_locale (code),
    active     boolean     NOT NULL DEFAULT true,
    sort_order int         NOT NULL DEFAULT 0,
    CONSTRAINT ck_supported_locale_dir CHECK (dir IN ('ltr', 'rtl'))
);

INSERT INTO reference.supported_locale (code, dir, fallback, sort_order) VALUES
    ('en', 'ltr', NULL, 1),
    ('zh-CN', 'ltr', 'en', 2),
    ('ko', 'ltr', 'en', 3),
    ('ja', 'ltr', 'en', 4),
    ('th', 'ltr', 'en', 5),
    ('vi', 'ltr', 'en', 6),
    ('fr', 'ltr', 'en', 7),
    ('es', 'ltr', 'en', 8),
    ('id', 'ltr', 'en', 9),
    ('ms', 'ltr', 'en', 10),
    ('hi', 'ltr', 'en', 11),
    ('ru', 'ltr', 'en', 12),
    ('bn', 'ltr', 'en', 13),
    ('de', 'ltr', 'en', 14),
    ('ur', 'rtl', 'en', 15),
    ('fil', 'ltr', 'en', 16),
    ('fa', 'rtl', 'en', 17),
    ('pt', 'ltr', 'en', 18),
    ('ar', 'rtl', 'en', 19),
    ('tr', 'ltr', 'en', 20);

-- Stable, language-independent taxonomy codes (localization class C).
CREATE TABLE reference.supplier_type (
    code       varchar(64) PRIMARY KEY,
    active     boolean     NOT NULL DEFAULT true,
    sort_order int         NOT NULL DEFAULT 0
);
INSERT INTO reference.supplier_type (code, sort_order) VALUES
    ('LENS_MANUFACTURER', 1),
    ('FRAME_MANUFACTURER', 2),
    ('CONTACT_LENS_MANUFACTURER', 3),
    ('EQUIPMENT_MANUFACTURER', 4),
    ('COMPONENT_SUPPLIER', 5),
    ('OEM_MANUFACTURER', 6),
    ('ODM_MANUFACTURER', 7),
    ('PRIVATE_LABEL_MANUFACTURER', 8),
    ('DISTRIBUTOR', 9),
    ('TRADING_COMPANY', 10);

CREATE TABLE reference.supplier_capability (
    code       varchar(64) PRIMARY KEY,
    active     boolean     NOT NULL DEFAULT true,
    sort_order int         NOT NULL DEFAULT 0
);
INSERT INTO reference.supplier_capability (code, sort_order) VALUES
    ('OEM', 1),
    ('ODM', 2),
    ('PRIVATE_LABEL', 3),
    ('LENS_COATING', 4),
    ('LENS_EDGING', 5),
    ('FRAME_ASSEMBLY', 6),
    ('INJECTION_MOLDING', 7),
    ('QUALITY_CONTROL_LAB', 8),
    ('EXPORT_LOGISTICS', 9);

CREATE TABLE reference.verification_scope (
    code       varchar(64) PRIMARY KEY,
    active     boolean     NOT NULL DEFAULT true,
    sort_order int         NOT NULL DEFAULT 0
);
-- Minimum first scope set (supplier-domain-model §4 of the brief); extensible by INSERT.
INSERT INTO reference.verification_scope (code, sort_order) VALUES
    ('LEGAL_ENTITY', 1),
    ('BUSINESS_REGISTRATION', 2),
    ('MANUFACTURER_STATUS', 3),
    ('OPTICAL_INDUSTRY_ACTIVITY', 4),
    ('FACTORY_EXISTENCE', 5);

CREATE TABLE reference.evidence_type (
    code       varchar(64) PRIMARY KEY,
    active     boolean     NOT NULL DEFAULT true,
    sort_order int         NOT NULL DEFAULT 0
);
INSERT INTO reference.evidence_type (code, sort_order) VALUES
    ('BUSINESS_REGISTRATION_DOCUMENT', 1),
    ('BUSINESS_LICENSE', 2),
    ('TAX_CERTIFICATE', 3),
    ('FACTORY_PHOTO', 4),
    ('ISO_CERTIFICATE', 5),
    ('QUALITY_CERTIFICATE', 6),
    ('EXPORT_LICENSE', 7),
    ('OTHER', 99);

CREATE TABLE reference.facility_type (
    code       varchar(64) PRIMARY KEY,
    active     boolean     NOT NULL DEFAULT true,
    sort_order int         NOT NULL DEFAULT 0
);
INSERT INTO reference.facility_type (code, sort_order) VALUES
    ('FACTORY', 1),
    ('HEADQUARTERS', 2),
    ('WAREHOUSE', 3),
    ('RESEARCH_CENTER', 4),
    ('SHOWROOM', 5);

-- ====================================================================== platform_access
CREATE SCHEMA IF NOT EXISTS platform_access;

-- Platform-staff grants. Provisioned by a documented bootstrap (operator/test insert), NOT
-- by an email allowlist or a hidden route. Example dev/test insert (replace the user id):
--   INSERT INTO platform_access.platform_staff
--     (id, user_id, platform_role, active, created_at, updated_at, version)
--   VALUES (gen_random_uuid(), '<user-uuid>', 'SENIOR_REVIEWER', true, now(), now(), 0);
CREATE TABLE platform_access.platform_staff (
    id            uuid        PRIMARY KEY,
    user_id       uuid        NOT NULL UNIQUE REFERENCES identity.user_account (id),
    platform_role varchar(32) NOT NULL,
    active        boolean     NOT NULL DEFAULT true,
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL,
    created_by    uuid,
    updated_by    uuid,
    version       int         NOT NULL DEFAULT 0,
    CONSTRAINT ck_platform_staff_role
        CHECK (platform_role IN ('REVIEWER', 'SENIOR_REVIEWER', 'PLATFORM_ADMIN'))
);

-- ============================================================================= supplier
CREATE SCHEMA IF NOT EXISTS supplier;

CREATE TABLE supplier.supplier (
    id                 uuid        PRIMARY KEY,
    organization_id    uuid        NOT NULL REFERENCES organizations.organization (id),
    operational_status varchar(32) NOT NULL DEFAULT 'PENDING',
    original_locale    varchar(16) NOT NULL REFERENCES reference.supported_locale (code),
    created_at         timestamptz NOT NULL,
    updated_at         timestamptz NOT NULL,
    created_by         uuid,
    updated_by         uuid,
    version            int         NOT NULL DEFAULT 0,
    CONSTRAINT ck_supplier_operational_status
        CHECK (operational_status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'DEACTIVATED'))
);
-- One active (non-deactivated) supplier identity per organization; DEACTIVATED rows are
-- retained for audit and free the slot for a fresh application.
CREATE UNIQUE INDEX uq_supplier_active_org
    ON supplier.supplier (organization_id)
    WHERE operational_status <> 'DEACTIVATED';
CREATE INDEX ix_supplier_organization ON supplier.supplier (organization_id);

CREATE TABLE supplier.supplier_application (
    id           uuid        PRIMARY KEY,
    supplier_id  uuid        NOT NULL REFERENCES supplier.supplier (id),
    status       varchar(32) NOT NULL DEFAULT 'DRAFT',
    submitted_at timestamptz,
    decided_at   timestamptz,
    decided_by   uuid,
    claimed_by   uuid,
    created_at   timestamptz NOT NULL,
    updated_at   timestamptz NOT NULL,
    created_by   uuid,
    updated_by   uuid,
    version      int         NOT NULL DEFAULT 0,
    CONSTRAINT ck_application_status CHECK (status IN
        ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'CHANGES_REQUESTED', 'RESUBMITTED',
         'APPROVED', 'REJECTED', 'WITHDRAWN'))
);
CREATE INDEX ix_application_supplier ON supplier.supplier_application (supplier_id);
-- Reviewer queue: filter by status, page by id (UUIDv7 time-ordered cursor).
CREATE INDEX ix_application_queue ON supplier.supplier_application (status, id);

CREATE TABLE supplier.supplier_profile (
    id                                uuid        PRIMARY KEY,
    supplier_id                       uuid        NOT NULL UNIQUE REFERENCES supplier.supplier (id),
    legal_name                        varchar(300) NOT NULL,
    registered_legal_name_translated  varchar(300),
    trading_name                      varchar(300),
    registration_number               varchar(120),
    country_of_registration           varchar(2),
    registration_authority            varchar(300),
    registration_date                 date,
    company_type_code                 varchar(64),
    year_established                  int,
    employee_range                    varchar(32),
    website                           varchar(300),
    business_email                    varchar(254),
    business_phone                    varchar(40),
    source_version                    int         NOT NULL DEFAULT 1,
    created_at                        timestamptz NOT NULL,
    updated_at                        timestamptz NOT NULL,
    created_by                        uuid,
    updated_by                        uuid,
    version                           int         NOT NULL DEFAULT 0,
    CONSTRAINT ck_profile_country
        CHECK (country_of_registration IS NULL OR country_of_registration ~ '^[A-Z]{2}$')
);

CREATE TABLE supplier.supplier_profile_translation (
    id                                uuid        PRIMARY KEY,
    profile_id                        uuid        NOT NULL REFERENCES supplier.supplier_profile (id),
    locale                            varchar(16) NOT NULL REFERENCES reference.supported_locale (code),
    trading_name                      varchar(300),
    company_description               varchar(4000),
    production_capability_description varchar(4000),
    oem_description                   varchar(4000),
    odm_description                   varchar(4000),
    private_label_description         varchar(4000),
    quality_control_description       varchar(4000),
    export_market_description         varchar(4000),
    translation_status                varchar(32) NOT NULL,
    source_locale                     varchar(16) NOT NULL,
    source_version                    int         NOT NULL,
    translation_source                varchar(16) NOT NULL,
    translator_user_id                uuid,
    reviewer_user_id                  uuid,
    approved_at                       timestamptz,
    approved_by                       uuid,
    is_original                       boolean     NOT NULL DEFAULT false,
    created_at                        timestamptz NOT NULL,
    updated_at                        timestamptz NOT NULL,
    created_by                        uuid,
    updated_by                        uuid,
    version                           int         NOT NULL DEFAULT 0,
    CONSTRAINT uq_profile_translation UNIQUE (profile_id, locale),
    CONSTRAINT ck_profile_translation_status CHECK (translation_status IN
        ('MISSING', 'DRAFT', 'MACHINE_TRANSLATED', 'HUMAN_REVIEWED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_profile_translation_source CHECK (translation_source IN ('HUMAN', 'MACHINE', 'IMPORT'))
);
CREATE INDEX ix_profile_translation_profile ON supplier.supplier_profile_translation (profile_id);
CREATE INDEX ix_profile_translation_locale ON supplier.supplier_profile_translation (locale);

CREATE TABLE supplier.supplier_type_assignment (
    id          uuid        PRIMARY KEY,
    supplier_id uuid        NOT NULL REFERENCES supplier.supplier (id),
    type_code   varchar(64) NOT NULL REFERENCES reference.supplier_type (code),
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    created_by  uuid,
    updated_by  uuid,
    version     int         NOT NULL DEFAULT 0,
    CONSTRAINT uq_type_assignment UNIQUE (supplier_id, type_code)
);

CREATE TABLE supplier.supplier_capability_declaration (
    id              uuid        PRIMARY KEY,
    supplier_id     uuid        NOT NULL REFERENCES supplier.supplier (id),
    capability_code varchar(64) NOT NULL REFERENCES reference.supplier_capability (code),
    claim_status    varchar(32) NOT NULL DEFAULT 'CLAIMED',
    created_at      timestamptz NOT NULL,
    updated_at      timestamptz NOT NULL,
    created_by      uuid,
    updated_by      uuid,
    version         int         NOT NULL DEFAULT 0,
    CONSTRAINT uq_capability_declaration UNIQUE (supplier_id, capability_code),
    CONSTRAINT ck_capability_claim_status
        CHECK (claim_status IN ('CLAIMED', 'EVIDENCE_SUBMITTED', 'VERIFIED', 'REJECTED'))
);

CREATE TABLE supplier.supplier_facility (
    id                 uuid        PRIMARY KEY,
    supplier_id        uuid        NOT NULL REFERENCES supplier.supplier (id),
    facility_type_code varchar(64) NOT NULL REFERENCES reference.facility_type (code),
    country            varchar(2)  NOT NULL,
    region             varchar(200),
    city               varchar(200),
    address_privacy    varchar(32) NOT NULL,
    ownership          varchar(32) NOT NULL,
    is_public          boolean     NOT NULL DEFAULT false,
    source_version     int         NOT NULL DEFAULT 1,
    created_at         timestamptz NOT NULL,
    updated_at         timestamptz NOT NULL,
    created_by         uuid,
    updated_by         uuid,
    version            int         NOT NULL DEFAULT 0,
    CONSTRAINT ck_facility_country CHECK (country ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_facility_address_privacy CHECK (address_privacy IN ('PUBLIC_CITY', 'PRIVATE')),
    CONSTRAINT ck_facility_ownership CHECK (ownership IN ('OWNED', 'LEASED', 'PARTNER'))
);
CREATE INDEX ix_facility_supplier ON supplier.supplier_facility (supplier_id);

CREATE TABLE supplier.supplier_facility_translation (
    id                 uuid        PRIMARY KEY,
    facility_id        uuid        NOT NULL REFERENCES supplier.supplier_facility (id),
    locale             varchar(16) NOT NULL REFERENCES reference.supported_locale (code),
    name               varchar(300),
    description        varchar(4000),
    translation_status varchar(32) NOT NULL,
    source_version     int         NOT NULL,
    is_original        boolean     NOT NULL DEFAULT false,
    created_at         timestamptz NOT NULL,
    updated_at         timestamptz NOT NULL,
    created_by         uuid,
    updated_by         uuid,
    version            int         NOT NULL DEFAULT 0,
    CONSTRAINT uq_facility_translation UNIQUE (facility_id, locale),
    CONSTRAINT ck_facility_translation_status CHECK (translation_status IN
        ('MISSING', 'DRAFT', 'MACHINE_TRANSLATED', 'HUMAN_REVIEWED', 'APPROVED', 'REJECTED'))
);
CREATE INDEX ix_facility_translation_facility ON supplier.supplier_facility_translation (facility_id);

CREATE TABLE supplier.review_request (
    id                uuid         PRIMARY KEY,
    application_id    uuid         NOT NULL REFERENCES supplier.supplier_application (id),
    requested_item    varchar(200) NOT NULL,
    reason            varchar(4000) NOT NULL,
    requested_by      uuid         NOT NULL,
    requested_at      timestamptz  NOT NULL,
    status            varchar(32)  NOT NULL DEFAULT 'OPEN',
    supplier_response varchar(4000),
    response_locale   varchar(16),
    resolved_at       timestamptz,
    created_at        timestamptz  NOT NULL,
    updated_at        timestamptz  NOT NULL,
    created_by        uuid,
    updated_by        uuid,
    version           int          NOT NULL DEFAULT 0,
    CONSTRAINT ck_review_request_status CHECK (status IN ('OPEN', 'RESPONDED', 'RESOLVED'))
);
CREATE INDEX ix_review_request_application ON supplier.review_request (application_id);

-- Evidence METADATA only — bytes never enter PostgreSQL (evidence-storage-architecture).
CREATE TABLE supplier.verification_evidence (
    id                  uuid         PRIMARY KEY,
    supplier_id         uuid         NOT NULL REFERENCES supplier.supplier (id),
    organization_id     uuid         NOT NULL REFERENCES organizations.organization (id),
    evidence_type_code  varchar(64)  NOT NULL REFERENCES reference.evidence_type (code),
    storage_key         varchar(300) NOT NULL,
    original_filename   varchar(300) NOT NULL,
    declared_media_type varchar(100) NOT NULL,
    media_type          varchar(100),
    byte_size           bigint,
    sha256              varchar(64),
    uploaded_by         uuid         NOT NULL,
    uploaded_at         timestamptz,
    upload_state        varchar(32)  NOT NULL DEFAULT 'INITIATED',
    scan_status         varchar(32)  NOT NULL DEFAULT 'PENDING_SCAN',
    review_status       varchar(32)  NOT NULL DEFAULT 'UNREVIEWED',
    reviewed_by         uuid,
    reviewed_at         timestamptz,
    document_locale     varchar(16),
    retention_status    varchar(32)  NOT NULL DEFAULT 'NONE',
    created_at          timestamptz  NOT NULL,
    updated_at          timestamptz  NOT NULL,
    created_by          uuid,
    updated_by          uuid,
    version             int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_evidence_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_evidence_upload_state CHECK (upload_state IN ('INITIATED', 'FINALIZED')),
    CONSTRAINT ck_evidence_scan_status
        CHECK (scan_status IN ('PENDING_SCAN', 'SCANNING', 'CLEAN', 'REJECTED', 'QUARANTINED')),
    CONSTRAINT ck_evidence_review_status CHECK (review_status IN ('UNREVIEWED', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT ck_evidence_retention_status CHECK (retention_status IN ('NONE', 'RETAINED'))
);
CREATE INDEX ix_evidence_supplier ON supplier.verification_evidence (supplier_id);
CREATE INDEX ix_evidence_organization ON supplier.verification_evidence (organization_id);

-- ========================================================================= verification
CREATE SCHEMA IF NOT EXISTS verification;

CREATE TABLE verification.verification_case (
    id          uuid        PRIMARY KEY,
    supplier_id uuid        NOT NULL UNIQUE REFERENCES supplier.supplier (id),
    status      varchar(32) NOT NULL DEFAULT 'UNDER_REVIEW',
    opened_at   timestamptz NOT NULL,
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    created_by  uuid,
    updated_by  uuid,
    version     int         NOT NULL DEFAULT 0,
    CONSTRAINT ck_verification_case_status CHECK (status IN
        ('NOT_REQUESTED', 'PENDING', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED',
         'CHANGES_REQUESTED', 'EXPIRED', 'SUSPENDED', 'REVOKED'))
);

CREATE TABLE verification.verification_scope_result (
    id          uuid        PRIMARY KEY,
    case_id     uuid        NOT NULL REFERENCES verification.verification_case (id),
    scope_code  varchar(64) NOT NULL REFERENCES reference.verification_scope (code),
    status      varchar(32) NOT NULL DEFAULT 'PENDING',
    decided_by  uuid,
    decided_at  timestamptz,
    valid_from  timestamptz,
    valid_until timestamptz,
    reason      varchar(4000),
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    created_by  uuid,
    updated_by  uuid,
    version     int         NOT NULL DEFAULT 0,
    CONSTRAINT uq_scope_result UNIQUE (case_id, scope_code),
    CONSTRAINT ck_scope_result_status CHECK (status IN
        ('NOT_REQUESTED', 'PENDING', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED',
         'CHANGES_REQUESTED', 'EXPIRED', 'SUSPENDED', 'REVOKED'))
);
CREATE INDEX ix_scope_result_case ON verification.verification_scope_result (case_id);

-- Evidence linkage for a scope grant (the "grant requires evidence" invariant). The FK into
-- the supplier schema is a database integrity net only.
CREATE TABLE verification.scope_result_evidence (
    id              uuid        PRIMARY KEY,
    scope_result_id uuid        NOT NULL REFERENCES verification.verification_scope_result (id),
    evidence_id     uuid        NOT NULL REFERENCES supplier.verification_evidence (id),
    created_at      timestamptz NOT NULL,
    updated_at      timestamptz NOT NULL,
    created_by      uuid,
    updated_by      uuid,
    version         int         NOT NULL DEFAULT 0,
    CONSTRAINT uq_scope_result_evidence UNIQUE (scope_result_id, evidence_id)
);
CREATE INDEX ix_scope_result_evidence_result ON verification.scope_result_evidence (scope_result_id);
