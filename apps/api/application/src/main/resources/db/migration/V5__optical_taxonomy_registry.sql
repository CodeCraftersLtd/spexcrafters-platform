-- V5: Optical Taxonomy & Specification Registry (Phase 8). Master registry of optical
-- vocabulary -- categories, enumerations, attributes, units, countries, certifications,
-- brands, and specification templates -- that every future optical module (products,
-- supplier claims, search, RFQ, SEO, analytics, AI) references by stable identifier.
-- Binding spec: docs/architecture/optical-taxonomy-domain-model.md. Companion decisions:
-- ADR-024 (module & schema), ADR-025 (attributes/templates/validation), ADR-026
-- (localization & identifier policy), ADR-027 (SEO slugs & aliases). Reuses the ADR-020
-- translation-row shape and reference.supported_locale (V4).
-- Conventions per V1-V4: uuid PKs (UUIDv7 from the application), timestamptz, snake_case,
-- string enums re-checked by DB CHECK, the audit tail (created_at/updated_at/created_by/
-- updated_by/version) on every business table, constraint naming ck_<table>_<field> /
-- uq_<name> / ix_<table>_<cols>. V1-V4 are immutable; this migration adds the `taxonomy`
-- schema only. gen_random_uuid() is a PostgreSQL-core builtin since PG13 (no pgcrypto
-- extension required -- V1 enables only `citext`); used here for inline seed rows only,
-- the application generates UUIDv7 for all runtime writes.
--
-- Table order below follows the domain-model doc's own numbering (par 2 category, par 3
-- enumeration, par 4 attribute, par 5 units, par 6 country, par 7 certification, par 8
-- brand, par 9 templates) where possible, but units/country/certification/enumeration are
-- created BEFORE category/attribute/templates because taxonomy.attribute has FKs to
-- taxonomy.unit_of_measure and taxonomy.enumeration, taxonomy.certification/brand FK to
-- taxonomy.country, and taxonomy.specification_template FKs to taxonomy.category -- DDL
-- must create a referenced table before the referencing one. This is a structural
-- reordering only; every table/column/constraint matches the doc's per-table spec.

CREATE SCHEMA IF NOT EXISTS taxonomy;

-- ============================================================================ taxonomy.unit_of_measure (par 5)
-- De-duplicated unit registry (one row per symbol) with conversion metadata to a family
-- base unit. COUNT-family units (piece/pair/carton/box/set) are not interconvertible.

CREATE TABLE taxonomy.unit_of_measure (
    code           varchar(32)  PRIMARY KEY,
    family         varchar(32)  NOT NULL,
    base_unit_code varchar(32)  REFERENCES taxonomy.unit_of_measure (code),
    factor_to_base numeric,
    offset_to_base numeric      NOT NULL DEFAULT 0,
    display_format varchar(64),
    active         boolean      NOT NULL DEFAULT true,
    sort_order     int          NOT NULL DEFAULT 0,
    created_at     timestamptz  NOT NULL,
    updated_at     timestamptz  NOT NULL,
    created_by     uuid,
    updated_by     uuid,
    version        int          NOT NULL DEFAULT 0,
    CONSTRAINT ck_unit_of_measure_family
        CHECK (family IN ('LENGTH', 'MASS', 'POWER_DIOPTER', 'ANGLE', 'COUNT'))
);

INSERT INTO taxonomy.unit_of_measure
    (code, family, base_unit_code, factor_to_base, offset_to_base, display_format, active, sort_order, created_at, updated_at, version)
VALUES
    ('mm',      'LENGTH',        'mm',      1,      0, '{value} mm',  true, 1,  now(), now(), 0),
    ('cm',      'LENGTH',        'mm',      10,     0, '{value} cm',  true, 2,  now(), now(), 0),
    ('m',       'LENGTH',        'mm',      1000,   0, '{value} m',   true, 3,  now(), now(), 0),
    ('inch',    'LENGTH',        'mm',      25.4,   0, '{value} in',  true, 4,  now(), now(), 0),
    ('micron',  'LENGTH',        'mm',      0.001,  0, '{value} um',  true, 5,  now(), now(), 0),
    ('gram',    'MASS',          'gram',    1,      0, '{value} g',   true, 6,  now(), now(), 0),
    ('kg',      'MASS',          'gram',    1000,   0, '{value} kg',  true, 7,  now(), now(), 0),
    ('diopter', 'POWER_DIOPTER', 'diopter', 1,      0, '{value} D',   true, 8,  now(), now(), 0),
    ('degree',  'ANGLE',         'degree',  1,      0, '{value} deg', true, 9,  now(), now(), 0),
    ('piece',   'COUNT',         NULL,      NULL,   0, '{value} pc',  true, 10, now(), now(), 0),
    ('pair',    'COUNT',         NULL,      NULL,   0, '{value} pr',  true, 11, now(), now(), 0),
    ('carton',  'COUNT',         NULL,      NULL,   0, '{value} ctn', true, 12, now(), now(), 0),
    ('box',     'COUNT',         NULL,      NULL,   0, '{value} bx',  true, 13, now(), now(), 0),
    ('set',     'COUNT',         NULL,      NULL,   0, '{value} set', true, 14, now(), now(), 0);

CREATE TABLE taxonomy.unit_translation (
    id                  uuid         PRIMARY KEY,
    unit_code           varchar(32)  NOT NULL REFERENCES taxonomy.unit_of_measure (code),
    locale              varchar(16)  NOT NULL REFERENCES reference.supported_locale (code),
    display_name        varchar(120),
    translation_status  varchar(32)  NOT NULL,
    source_locale       varchar(16)  NOT NULL,
    source_version      int          NOT NULL,
    translation_source  varchar(16)  NOT NULL,
    translator_user_id  uuid,
    reviewer_user_id    uuid,
    approved_at         timestamptz,
    approved_by         uuid,
    is_original         boolean      NOT NULL DEFAULT false,
    created_at          timestamptz  NOT NULL,
    updated_at          timestamptz  NOT NULL,
    created_by          uuid,
    updated_by          uuid,
    version             int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_unit_translation UNIQUE (unit_code, locale),
    CONSTRAINT ck_unit_translation_status CHECK (translation_status IN
        ('MISSING', 'DRAFT', 'MACHINE_TRANSLATED', 'HUMAN_REVIEWED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_unit_translation_source CHECK (translation_source IN ('HUMAN', 'MACHINE', 'IMPORT'))
);
CREATE INDEX ix_unit_translation_unit ON taxonomy.unit_translation (unit_code);
CREATE INDEX ix_unit_translation_locale ON taxonomy.unit_translation (locale);

INSERT INTO taxonomy.unit_translation
    (id, unit_code, locale, display_name, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), 'mm',      'en', 'millimetre', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'cm',      'en', 'centimetre', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'm',       'en', 'metre',      'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'inch',    'en', 'inch',       'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'micron',  'en', 'micron',     'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'gram',    'en', 'gram',       'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'kg',      'en', 'kilogram',   'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'diopter', 'en', 'diopter',    'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'degree',  'en', 'degree',     'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'piece',   'en', 'piece',      'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'pair',    'en', 'pair',       'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'carton',  'en', 'carton',     'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'box',     'en', 'box',        'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'set',     'en', 'set',        'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0);

-- ============================================================================ taxonomy.country (par 6)
-- ISO 3166-1 country registry (alpha-2 PK). Future customs/logistics metadata attaches
-- here without redesign.

CREATE TABLE taxonomy.country (
    code         varchar(2)   PRIMARY KEY,
    alpha3       varchar(3)   NOT NULL,
    numeric_code varchar(3)   NOT NULL,
    region       varchar(64),
    subregion    varchar(64),
    continent    varchar(32),
    active       boolean      NOT NULL DEFAULT true,
    sort_order   int          NOT NULL DEFAULT 0,
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL,
    created_by   uuid,
    updated_by   uuid,
    version      int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_country_alpha3 UNIQUE (alpha3)
);

CREATE TABLE taxonomy.country_translation (
    id                  uuid         PRIMARY KEY,
    country_code        varchar(2)   NOT NULL REFERENCES taxonomy.country (code),
    locale              varchar(16)  NOT NULL REFERENCES reference.supported_locale (code),
    name                varchar(160),
    translation_status  varchar(32)  NOT NULL,
    source_locale       varchar(16)  NOT NULL,
    source_version      int          NOT NULL,
    translation_source  varchar(16)  NOT NULL,
    translator_user_id  uuid,
    reviewer_user_id    uuid,
    approved_at         timestamptz,
    approved_by         uuid,
    is_original         boolean      NOT NULL DEFAULT false,
    created_at          timestamptz  NOT NULL,
    updated_at          timestamptz  NOT NULL,
    created_by          uuid,
    updated_by          uuid,
    version             int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_country_translation UNIQUE (country_code, locale),
    CONSTRAINT ck_country_translation_status CHECK (translation_status IN
        ('MISSING', 'DRAFT', 'MACHINE_TRANSLATED', 'HUMAN_REVIEWED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_country_translation_source CHECK (translation_source IN ('HUMAN', 'MACHINE', 'IMPORT'))
);
CREATE INDEX ix_country_translation_country ON taxonomy.country_translation (country_code);
CREATE INDEX ix_country_translation_locale ON taxonomy.country_translation (locale);

-- Full ISO 3166-1 seed (alpha-2/alpha-3/numeric/region/subregion/continent), sourced from
-- the ISO 3166 country list with UN M49 regional codes. `region` mirrors the top-level UN
-- M49 region (Africa/Americas/Asia/Europe/Oceania); `subregion` the UN M49 subregion;
-- `continent` uses the 7-continent scheme (splits Americas into North America/South
-- America; Antarctica is its own continent). Taiwan (TW) and Antarctica (AQ) have no
-- official UN region code and are given best-fit continent values. Names are the ISO 3166
-- English short names (diacritics ASCII-transliterated for portability). 249 countries.
INSERT INTO taxonomy.country (code, alpha3, numeric_code, region, subregion, continent, active, sort_order, created_at, updated_at, version) VALUES
    ('AF', 'AFG', '004', 'Asia', 'Southern Asia', 'Asia', true, 1, now(), now(), 0),
    ('AX', 'ALA', '248', 'Europe', 'Northern Europe', 'Europe', true, 2, now(), now(), 0),
    ('AL', 'ALB', '008', 'Europe', 'Southern Europe', 'Europe', true, 3, now(), now(), 0),
    ('DZ', 'DZA', '012', 'Africa', 'Northern Africa', 'Africa', true, 4, now(), now(), 0),
    ('AS', 'ASM', '016', 'Oceania', 'Polynesia', 'Oceania', true, 5, now(), now(), 0),
    ('AD', 'AND', '020', 'Europe', 'Southern Europe', 'Europe', true, 6, now(), now(), 0),
    ('AO', 'AGO', '024', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 7, now(), now(), 0),
    ('AI', 'AIA', '660', 'Americas', 'Latin America and the Caribbean', 'North America', true, 8, now(), now(), 0),
    ('AQ', 'ATA', '010', NULL, NULL, 'Antarctica', true, 9, now(), now(), 0),
    ('AG', 'ATG', '028', 'Americas', 'Latin America and the Caribbean', 'North America', true, 10, now(), now(), 0),
    ('AR', 'ARG', '032', 'Americas', 'Latin America and the Caribbean', 'South America', true, 11, now(), now(), 0),
    ('AM', 'ARM', '051', 'Asia', 'Western Asia', 'Asia', true, 12, now(), now(), 0),
    ('AW', 'ABW', '533', 'Americas', 'Latin America and the Caribbean', 'North America', true, 13, now(), now(), 0),
    ('AU', 'AUS', '036', 'Oceania', 'Australia and New Zealand', 'Oceania', true, 14, now(), now(), 0),
    ('AT', 'AUT', '040', 'Europe', 'Western Europe', 'Europe', true, 15, now(), now(), 0),
    ('AZ', 'AZE', '031', 'Asia', 'Western Asia', 'Asia', true, 16, now(), now(), 0),
    ('BS', 'BHS', '044', 'Americas', 'Latin America and the Caribbean', 'North America', true, 17, now(), now(), 0),
    ('BH', 'BHR', '048', 'Asia', 'Western Asia', 'Asia', true, 18, now(), now(), 0),
    ('BD', 'BGD', '050', 'Asia', 'Southern Asia', 'Asia', true, 19, now(), now(), 0),
    ('BB', 'BRB', '052', 'Americas', 'Latin America and the Caribbean', 'North America', true, 20, now(), now(), 0),
    ('BY', 'BLR', '112', 'Europe', 'Eastern Europe', 'Europe', true, 21, now(), now(), 0),
    ('BE', 'BEL', '056', 'Europe', 'Western Europe', 'Europe', true, 22, now(), now(), 0),
    ('BZ', 'BLZ', '084', 'Americas', 'Latin America and the Caribbean', 'North America', true, 23, now(), now(), 0),
    ('BJ', 'BEN', '204', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 24, now(), now(), 0),
    ('BM', 'BMU', '060', 'Americas', 'Northern America', 'North America', true, 25, now(), now(), 0),
    ('BT', 'BTN', '064', 'Asia', 'Southern Asia', 'Asia', true, 26, now(), now(), 0),
    ('BO', 'BOL', '068', 'Americas', 'Latin America and the Caribbean', 'South America', true, 27, now(), now(), 0),
    ('BQ', 'BES', '535', 'Americas', 'Latin America and the Caribbean', 'North America', true, 28, now(), now(), 0),
    ('BA', 'BIH', '070', 'Europe', 'Southern Europe', 'Europe', true, 29, now(), now(), 0),
    ('BW', 'BWA', '072', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 30, now(), now(), 0),
    ('BV', 'BVT', '074', 'Americas', 'Latin America and the Caribbean', 'South America', true, 31, now(), now(), 0),
    ('BR', 'BRA', '076', 'Americas', 'Latin America and the Caribbean', 'South America', true, 32, now(), now(), 0),
    ('IO', 'IOT', '086', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 33, now(), now(), 0),
    ('BN', 'BRN', '096', 'Asia', 'South-eastern Asia', 'Asia', true, 34, now(), now(), 0),
    ('BG', 'BGR', '100', 'Europe', 'Eastern Europe', 'Europe', true, 35, now(), now(), 0),
    ('BF', 'BFA', '854', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 36, now(), now(), 0),
    ('BI', 'BDI', '108', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 37, now(), now(), 0),
    ('CV', 'CPV', '132', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 38, now(), now(), 0),
    ('KH', 'KHM', '116', 'Asia', 'South-eastern Asia', 'Asia', true, 39, now(), now(), 0),
    ('CM', 'CMR', '120', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 40, now(), now(), 0),
    ('CA', 'CAN', '124', 'Americas', 'Northern America', 'North America', true, 41, now(), now(), 0),
    ('KY', 'CYM', '136', 'Americas', 'Latin America and the Caribbean', 'North America', true, 42, now(), now(), 0),
    ('CF', 'CAF', '140', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 43, now(), now(), 0),
    ('TD', 'TCD', '148', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 44, now(), now(), 0),
    ('CL', 'CHL', '152', 'Americas', 'Latin America and the Caribbean', 'South America', true, 45, now(), now(), 0),
    ('CN', 'CHN', '156', 'Asia', 'Eastern Asia', 'Asia', true, 46, now(), now(), 0),
    ('CX', 'CXR', '162', 'Oceania', 'Australia and New Zealand', 'Oceania', true, 47, now(), now(), 0),
    ('CC', 'CCK', '166', 'Oceania', 'Australia and New Zealand', 'Oceania', true, 48, now(), now(), 0),
    ('CO', 'COL', '170', 'Americas', 'Latin America and the Caribbean', 'South America', true, 49, now(), now(), 0),
    ('KM', 'COM', '174', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 50, now(), now(), 0),
    ('CG', 'COG', '178', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 51, now(), now(), 0),
    ('CD', 'COD', '180', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 52, now(), now(), 0),
    ('CK', 'COK', '184', 'Oceania', 'Polynesia', 'Oceania', true, 53, now(), now(), 0),
    ('CR', 'CRI', '188', 'Americas', 'Latin America and the Caribbean', 'North America', true, 54, now(), now(), 0),
    ('CI', 'CIV', '384', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 55, now(), now(), 0),
    ('HR', 'HRV', '191', 'Europe', 'Southern Europe', 'Europe', true, 56, now(), now(), 0),
    ('CU', 'CUB', '192', 'Americas', 'Latin America and the Caribbean', 'North America', true, 57, now(), now(), 0),
    ('CW', 'CUW', '531', 'Americas', 'Latin America and the Caribbean', 'North America', true, 58, now(), now(), 0),
    ('CY', 'CYP', '196', 'Asia', 'Western Asia', 'Asia', true, 59, now(), now(), 0),
    ('CZ', 'CZE', '203', 'Europe', 'Eastern Europe', 'Europe', true, 60, now(), now(), 0),
    ('DK', 'DNK', '208', 'Europe', 'Northern Europe', 'Europe', true, 61, now(), now(), 0),
    ('DJ', 'DJI', '262', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 62, now(), now(), 0),
    ('DM', 'DMA', '212', 'Americas', 'Latin America and the Caribbean', 'North America', true, 63, now(), now(), 0),
    ('DO', 'DOM', '214', 'Americas', 'Latin America and the Caribbean', 'North America', true, 64, now(), now(), 0),
    ('EC', 'ECU', '218', 'Americas', 'Latin America and the Caribbean', 'South America', true, 65, now(), now(), 0),
    ('EG', 'EGY', '818', 'Africa', 'Northern Africa', 'Africa', true, 66, now(), now(), 0),
    ('SV', 'SLV', '222', 'Americas', 'Latin America and the Caribbean', 'North America', true, 67, now(), now(), 0),
    ('GQ', 'GNQ', '226', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 68, now(), now(), 0),
    ('ER', 'ERI', '232', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 69, now(), now(), 0),
    ('EE', 'EST', '233', 'Europe', 'Northern Europe', 'Europe', true, 70, now(), now(), 0),
    ('SZ', 'SWZ', '748', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 71, now(), now(), 0),
    ('ET', 'ETH', '231', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 72, now(), now(), 0),
    ('FK', 'FLK', '238', 'Americas', 'Latin America and the Caribbean', 'South America', true, 73, now(), now(), 0),
    ('FO', 'FRO', '234', 'Europe', 'Northern Europe', 'Europe', true, 74, now(), now(), 0),
    ('FJ', 'FJI', '242', 'Oceania', 'Melanesia', 'Oceania', true, 75, now(), now(), 0),
    ('FI', 'FIN', '246', 'Europe', 'Northern Europe', 'Europe', true, 76, now(), now(), 0),
    ('FR', 'FRA', '250', 'Europe', 'Western Europe', 'Europe', true, 77, now(), now(), 0),
    ('GF', 'GUF', '254', 'Americas', 'Latin America and the Caribbean', 'South America', true, 78, now(), now(), 0),
    ('PF', 'PYF', '258', 'Oceania', 'Polynesia', 'Oceania', true, 79, now(), now(), 0),
    ('TF', 'ATF', '260', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 80, now(), now(), 0),
    ('GA', 'GAB', '266', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 81, now(), now(), 0),
    ('GM', 'GMB', '270', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 82, now(), now(), 0),
    ('GE', 'GEO', '268', 'Asia', 'Western Asia', 'Asia', true, 83, now(), now(), 0),
    ('DE', 'DEU', '276', 'Europe', 'Western Europe', 'Europe', true, 84, now(), now(), 0),
    ('GH', 'GHA', '288', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 85, now(), now(), 0),
    ('GI', 'GIB', '292', 'Europe', 'Southern Europe', 'Europe', true, 86, now(), now(), 0),
    ('GR', 'GRC', '300', 'Europe', 'Southern Europe', 'Europe', true, 87, now(), now(), 0),
    ('GL', 'GRL', '304', 'Americas', 'Northern America', 'North America', true, 88, now(), now(), 0),
    ('GD', 'GRD', '308', 'Americas', 'Latin America and the Caribbean', 'North America', true, 89, now(), now(), 0),
    ('GP', 'GLP', '312', 'Americas', 'Latin America and the Caribbean', 'North America', true, 90, now(), now(), 0),
    ('GU', 'GUM', '316', 'Oceania', 'Micronesia', 'Oceania', true, 91, now(), now(), 0),
    ('GT', 'GTM', '320', 'Americas', 'Latin America and the Caribbean', 'North America', true, 92, now(), now(), 0),
    ('GG', 'GGY', '831', 'Europe', 'Northern Europe', 'Europe', true, 93, now(), now(), 0),
    ('GN', 'GIN', '324', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 94, now(), now(), 0),
    ('GW', 'GNB', '624', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 95, now(), now(), 0),
    ('GY', 'GUY', '328', 'Americas', 'Latin America and the Caribbean', 'South America', true, 96, now(), now(), 0),
    ('HT', 'HTI', '332', 'Americas', 'Latin America and the Caribbean', 'North America', true, 97, now(), now(), 0),
    ('HM', 'HMD', '334', 'Oceania', 'Australia and New Zealand', 'Oceania', true, 98, now(), now(), 0),
    ('VA', 'VAT', '336', 'Europe', 'Southern Europe', 'Europe', true, 99, now(), now(), 0),
    ('HN', 'HND', '340', 'Americas', 'Latin America and the Caribbean', 'North America', true, 100, now(), now(), 0),
    ('HK', 'HKG', '344', 'Asia', 'Eastern Asia', 'Asia', true, 101, now(), now(), 0),
    ('HU', 'HUN', '348', 'Europe', 'Eastern Europe', 'Europe', true, 102, now(), now(), 0),
    ('IS', 'ISL', '352', 'Europe', 'Northern Europe', 'Europe', true, 103, now(), now(), 0),
    ('IN', 'IND', '356', 'Asia', 'Southern Asia', 'Asia', true, 104, now(), now(), 0),
    ('ID', 'IDN', '360', 'Asia', 'South-eastern Asia', 'Asia', true, 105, now(), now(), 0),
    ('IR', 'IRN', '364', 'Asia', 'Southern Asia', 'Asia', true, 106, now(), now(), 0),
    ('IQ', 'IRQ', '368', 'Asia', 'Western Asia', 'Asia', true, 107, now(), now(), 0),
    ('IE', 'IRL', '372', 'Europe', 'Northern Europe', 'Europe', true, 108, now(), now(), 0),
    ('IM', 'IMN', '833', 'Europe', 'Northern Europe', 'Europe', true, 109, now(), now(), 0),
    ('IL', 'ISR', '376', 'Asia', 'Western Asia', 'Asia', true, 110, now(), now(), 0),
    ('IT', 'ITA', '380', 'Europe', 'Southern Europe', 'Europe', true, 111, now(), now(), 0),
    ('JM', 'JAM', '388', 'Americas', 'Latin America and the Caribbean', 'North America', true, 112, now(), now(), 0),
    ('JP', 'JPN', '392', 'Asia', 'Eastern Asia', 'Asia', true, 113, now(), now(), 0),
    ('JE', 'JEY', '832', 'Europe', 'Northern Europe', 'Europe', true, 114, now(), now(), 0),
    ('JO', 'JOR', '400', 'Asia', 'Western Asia', 'Asia', true, 115, now(), now(), 0),
    ('KZ', 'KAZ', '398', 'Asia', 'Central Asia', 'Asia', true, 116, now(), now(), 0),
    ('KE', 'KEN', '404', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 117, now(), now(), 0),
    ('KI', 'KIR', '296', 'Oceania', 'Micronesia', 'Oceania', true, 118, now(), now(), 0),
    ('KP', 'PRK', '408', 'Asia', 'Eastern Asia', 'Asia', true, 119, now(), now(), 0),
    ('KR', 'KOR', '410', 'Asia', 'Eastern Asia', 'Asia', true, 120, now(), now(), 0),
    ('KW', 'KWT', '414', 'Asia', 'Western Asia', 'Asia', true, 121, now(), now(), 0),
    ('KG', 'KGZ', '417', 'Asia', 'Central Asia', 'Asia', true, 122, now(), now(), 0),
    ('LA', 'LAO', '418', 'Asia', 'South-eastern Asia', 'Asia', true, 123, now(), now(), 0),
    ('LV', 'LVA', '428', 'Europe', 'Northern Europe', 'Europe', true, 124, now(), now(), 0),
    ('LB', 'LBN', '422', 'Asia', 'Western Asia', 'Asia', true, 125, now(), now(), 0),
    ('LS', 'LSO', '426', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 126, now(), now(), 0),
    ('LR', 'LBR', '430', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 127, now(), now(), 0),
    ('LY', 'LBY', '434', 'Africa', 'Northern Africa', 'Africa', true, 128, now(), now(), 0),
    ('LI', 'LIE', '438', 'Europe', 'Western Europe', 'Europe', true, 129, now(), now(), 0),
    ('LT', 'LTU', '440', 'Europe', 'Northern Europe', 'Europe', true, 130, now(), now(), 0),
    ('LU', 'LUX', '442', 'Europe', 'Western Europe', 'Europe', true, 131, now(), now(), 0),
    ('MO', 'MAC', '446', 'Asia', 'Eastern Asia', 'Asia', true, 132, now(), now(), 0),
    ('MG', 'MDG', '450', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 133, now(), now(), 0),
    ('MW', 'MWI', '454', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 134, now(), now(), 0),
    ('MY', 'MYS', '458', 'Asia', 'South-eastern Asia', 'Asia', true, 135, now(), now(), 0),
    ('MV', 'MDV', '462', 'Asia', 'Southern Asia', 'Asia', true, 136, now(), now(), 0),
    ('ML', 'MLI', '466', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 137, now(), now(), 0),
    ('MT', 'MLT', '470', 'Europe', 'Southern Europe', 'Europe', true, 138, now(), now(), 0),
    ('MH', 'MHL', '584', 'Oceania', 'Micronesia', 'Oceania', true, 139, now(), now(), 0),
    ('MQ', 'MTQ', '474', 'Americas', 'Latin America and the Caribbean', 'North America', true, 140, now(), now(), 0),
    ('MR', 'MRT', '478', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 141, now(), now(), 0),
    ('MU', 'MUS', '480', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 142, now(), now(), 0),
    ('YT', 'MYT', '175', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 143, now(), now(), 0),
    ('MX', 'MEX', '484', 'Americas', 'Latin America and the Caribbean', 'North America', true, 144, now(), now(), 0),
    ('FM', 'FSM', '583', 'Oceania', 'Micronesia', 'Oceania', true, 145, now(), now(), 0),
    ('MD', 'MDA', '498', 'Europe', 'Eastern Europe', 'Europe', true, 146, now(), now(), 0),
    ('MC', 'MCO', '492', 'Europe', 'Western Europe', 'Europe', true, 147, now(), now(), 0),
    ('MN', 'MNG', '496', 'Asia', 'Eastern Asia', 'Asia', true, 148, now(), now(), 0),
    ('ME', 'MNE', '499', 'Europe', 'Southern Europe', 'Europe', true, 149, now(), now(), 0),
    ('MS', 'MSR', '500', 'Americas', 'Latin America and the Caribbean', 'North America', true, 150, now(), now(), 0),
    ('MA', 'MAR', '504', 'Africa', 'Northern Africa', 'Africa', true, 151, now(), now(), 0),
    ('MZ', 'MOZ', '508', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 152, now(), now(), 0),
    ('MM', 'MMR', '104', 'Asia', 'South-eastern Asia', 'Asia', true, 153, now(), now(), 0),
    ('NA', 'NAM', '516', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 154, now(), now(), 0),
    ('NR', 'NRU', '520', 'Oceania', 'Micronesia', 'Oceania', true, 155, now(), now(), 0),
    ('NP', 'NPL', '524', 'Asia', 'Southern Asia', 'Asia', true, 156, now(), now(), 0),
    ('NL', 'NLD', '528', 'Europe', 'Western Europe', 'Europe', true, 157, now(), now(), 0),
    ('NC', 'NCL', '540', 'Oceania', 'Melanesia', 'Oceania', true, 158, now(), now(), 0),
    ('NZ', 'NZL', '554', 'Oceania', 'Australia and New Zealand', 'Oceania', true, 159, now(), now(), 0),
    ('NI', 'NIC', '558', 'Americas', 'Latin America and the Caribbean', 'North America', true, 160, now(), now(), 0),
    ('NE', 'NER', '562', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 161, now(), now(), 0),
    ('NG', 'NGA', '566', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 162, now(), now(), 0),
    ('NU', 'NIU', '570', 'Oceania', 'Polynesia', 'Oceania', true, 163, now(), now(), 0),
    ('NF', 'NFK', '574', 'Oceania', 'Australia and New Zealand', 'Oceania', true, 164, now(), now(), 0),
    ('MK', 'MKD', '807', 'Europe', 'Southern Europe', 'Europe', true, 165, now(), now(), 0),
    ('MP', 'MNP', '580', 'Oceania', 'Micronesia', 'Oceania', true, 166, now(), now(), 0),
    ('NO', 'NOR', '578', 'Europe', 'Northern Europe', 'Europe', true, 167, now(), now(), 0),
    ('OM', 'OMN', '512', 'Asia', 'Western Asia', 'Asia', true, 168, now(), now(), 0),
    ('PK', 'PAK', '586', 'Asia', 'Southern Asia', 'Asia', true, 169, now(), now(), 0),
    ('PW', 'PLW', '585', 'Oceania', 'Micronesia', 'Oceania', true, 170, now(), now(), 0),
    ('PS', 'PSE', '275', 'Asia', 'Western Asia', 'Asia', true, 171, now(), now(), 0),
    ('PA', 'PAN', '591', 'Americas', 'Latin America and the Caribbean', 'North America', true, 172, now(), now(), 0),
    ('PG', 'PNG', '598', 'Oceania', 'Melanesia', 'Oceania', true, 173, now(), now(), 0),
    ('PY', 'PRY', '600', 'Americas', 'Latin America and the Caribbean', 'South America', true, 174, now(), now(), 0),
    ('PE', 'PER', '604', 'Americas', 'Latin America and the Caribbean', 'South America', true, 175, now(), now(), 0),
    ('PH', 'PHL', '608', 'Asia', 'South-eastern Asia', 'Asia', true, 176, now(), now(), 0),
    ('PN', 'PCN', '612', 'Oceania', 'Polynesia', 'Oceania', true, 177, now(), now(), 0),
    ('PL', 'POL', '616', 'Europe', 'Eastern Europe', 'Europe', true, 178, now(), now(), 0),
    ('PT', 'PRT', '620', 'Europe', 'Southern Europe', 'Europe', true, 179, now(), now(), 0),
    ('PR', 'PRI', '630', 'Americas', 'Latin America and the Caribbean', 'North America', true, 180, now(), now(), 0),
    ('QA', 'QAT', '634', 'Asia', 'Western Asia', 'Asia', true, 181, now(), now(), 0),
    ('RE', 'REU', '638', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 182, now(), now(), 0),
    ('RO', 'ROU', '642', 'Europe', 'Eastern Europe', 'Europe', true, 183, now(), now(), 0),
    ('RU', 'RUS', '643', 'Europe', 'Eastern Europe', 'Europe', true, 184, now(), now(), 0),
    ('RW', 'RWA', '646', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 185, now(), now(), 0),
    ('BL', 'BLM', '652', 'Americas', 'Latin America and the Caribbean', 'North America', true, 186, now(), now(), 0),
    ('SH', 'SHN', '654', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 187, now(), now(), 0),
    ('KN', 'KNA', '659', 'Americas', 'Latin America and the Caribbean', 'North America', true, 188, now(), now(), 0),
    ('LC', 'LCA', '662', 'Americas', 'Latin America and the Caribbean', 'North America', true, 189, now(), now(), 0),
    ('MF', 'MAF', '663', 'Americas', 'Latin America and the Caribbean', 'North America', true, 190, now(), now(), 0),
    ('PM', 'SPM', '666', 'Americas', 'Northern America', 'North America', true, 191, now(), now(), 0),
    ('VC', 'VCT', '670', 'Americas', 'Latin America and the Caribbean', 'North America', true, 192, now(), now(), 0),
    ('WS', 'WSM', '882', 'Oceania', 'Polynesia', 'Oceania', true, 193, now(), now(), 0),
    ('SM', 'SMR', '674', 'Europe', 'Southern Europe', 'Europe', true, 194, now(), now(), 0),
    ('ST', 'STP', '678', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 195, now(), now(), 0),
    ('SA', 'SAU', '682', 'Asia', 'Western Asia', 'Asia', true, 196, now(), now(), 0),
    ('SN', 'SEN', '686', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 197, now(), now(), 0),
    ('RS', 'SRB', '688', 'Europe', 'Southern Europe', 'Europe', true, 198, now(), now(), 0),
    ('SC', 'SYC', '690', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 199, now(), now(), 0),
    ('SL', 'SLE', '694', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 200, now(), now(), 0),
    ('SG', 'SGP', '702', 'Asia', 'South-eastern Asia', 'Asia', true, 201, now(), now(), 0),
    ('SX', 'SXM', '534', 'Americas', 'Latin America and the Caribbean', 'North America', true, 202, now(), now(), 0),
    ('SK', 'SVK', '703', 'Europe', 'Eastern Europe', 'Europe', true, 203, now(), now(), 0),
    ('SI', 'SVN', '705', 'Europe', 'Southern Europe', 'Europe', true, 204, now(), now(), 0),
    ('SB', 'SLB', '090', 'Oceania', 'Melanesia', 'Oceania', true, 205, now(), now(), 0),
    ('SO', 'SOM', '706', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 206, now(), now(), 0),
    ('ZA', 'ZAF', '710', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 207, now(), now(), 0),
    ('GS', 'SGS', '239', 'Americas', 'Latin America and the Caribbean', 'South America', true, 208, now(), now(), 0),
    ('SS', 'SSD', '728', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 209, now(), now(), 0),
    ('ES', 'ESP', '724', 'Europe', 'Southern Europe', 'Europe', true, 210, now(), now(), 0),
    ('LK', 'LKA', '144', 'Asia', 'Southern Asia', 'Asia', true, 211, now(), now(), 0),
    ('SD', 'SDN', '729', 'Africa', 'Northern Africa', 'Africa', true, 212, now(), now(), 0),
    ('SR', 'SUR', '740', 'Americas', 'Latin America and the Caribbean', 'South America', true, 213, now(), now(), 0),
    ('SJ', 'SJM', '744', 'Europe', 'Northern Europe', 'Europe', true, 214, now(), now(), 0),
    ('SE', 'SWE', '752', 'Europe', 'Northern Europe', 'Europe', true, 215, now(), now(), 0),
    ('CH', 'CHE', '756', 'Europe', 'Western Europe', 'Europe', true, 216, now(), now(), 0),
    ('SY', 'SYR', '760', 'Asia', 'Western Asia', 'Asia', true, 217, now(), now(), 0),
    ('TW', 'TWN', '158', 'Asia', 'Eastern Asia', 'Asia', true, 218, now(), now(), 0),
    ('TJ', 'TJK', '762', 'Asia', 'Central Asia', 'Asia', true, 219, now(), now(), 0),
    ('TZ', 'TZA', '834', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 220, now(), now(), 0),
    ('TH', 'THA', '764', 'Asia', 'South-eastern Asia', 'Asia', true, 221, now(), now(), 0),
    ('TL', 'TLS', '626', 'Asia', 'South-eastern Asia', 'Asia', true, 222, now(), now(), 0),
    ('TG', 'TGO', '768', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 223, now(), now(), 0),
    ('TK', 'TKL', '772', 'Oceania', 'Polynesia', 'Oceania', true, 224, now(), now(), 0),
    ('TO', 'TON', '776', 'Oceania', 'Polynesia', 'Oceania', true, 225, now(), now(), 0),
    ('TT', 'TTO', '780', 'Americas', 'Latin America and the Caribbean', 'North America', true, 226, now(), now(), 0),
    ('TN', 'TUN', '788', 'Africa', 'Northern Africa', 'Africa', true, 227, now(), now(), 0),
    ('TR', 'TUR', '792', 'Asia', 'Western Asia', 'Asia', true, 228, now(), now(), 0),
    ('TM', 'TKM', '795', 'Asia', 'Central Asia', 'Asia', true, 229, now(), now(), 0),
    ('TC', 'TCA', '796', 'Americas', 'Latin America and the Caribbean', 'North America', true, 230, now(), now(), 0),
    ('TV', 'TUV', '798', 'Oceania', 'Polynesia', 'Oceania', true, 231, now(), now(), 0),
    ('UG', 'UGA', '800', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 232, now(), now(), 0),
    ('UA', 'UKR', '804', 'Europe', 'Eastern Europe', 'Europe', true, 233, now(), now(), 0),
    ('AE', 'ARE', '784', 'Asia', 'Western Asia', 'Asia', true, 234, now(), now(), 0),
    ('GB', 'GBR', '826', 'Europe', 'Northern Europe', 'Europe', true, 235, now(), now(), 0),
    ('US', 'USA', '840', 'Americas', 'Northern America', 'North America', true, 236, now(), now(), 0),
    ('UM', 'UMI', '581', 'Oceania', 'Micronesia', 'Oceania', true, 237, now(), now(), 0),
    ('UY', 'URY', '858', 'Americas', 'Latin America and the Caribbean', 'South America', true, 238, now(), now(), 0),
    ('UZ', 'UZB', '860', 'Asia', 'Central Asia', 'Asia', true, 239, now(), now(), 0),
    ('VU', 'VUT', '548', 'Oceania', 'Melanesia', 'Oceania', true, 240, now(), now(), 0),
    ('VE', 'VEN', '862', 'Americas', 'Latin America and the Caribbean', 'South America', true, 241, now(), now(), 0),
    ('VN', 'VNM', '704', 'Asia', 'South-eastern Asia', 'Asia', true, 242, now(), now(), 0),
    ('VG', 'VGB', '092', 'Americas', 'Latin America and the Caribbean', 'North America', true, 243, now(), now(), 0),
    ('VI', 'VIR', '850', 'Americas', 'Latin America and the Caribbean', 'North America', true, 244, now(), now(), 0),
    ('WF', 'WLF', '876', 'Oceania', 'Polynesia', 'Oceania', true, 245, now(), now(), 0),
    ('EH', 'ESH', '732', 'Africa', 'Northern Africa', 'Africa', true, 246, now(), now(), 0),
    ('YE', 'YEM', '887', 'Asia', 'Western Asia', 'Asia', true, 247, now(), now(), 0),
    ('ZM', 'ZMB', '894', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 248, now(), now(), 0),
    ('ZW', 'ZWE', '716', 'Africa', 'Sub-Saharan Africa', 'Africa', true, 249, now(), now(), 0);

-- 249 en country_translation rows
INSERT INTO taxonomy.country_translation (id, country_code, locale, name, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version) VALUES
    (gen_random_uuid(), 'AF', 'en', 'Afghanistan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AX', 'en', 'Aland Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AL', 'en', 'Albania', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'DZ', 'en', 'Algeria', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AS', 'en', 'American Samoa', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AD', 'en', 'Andorra', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AO', 'en', 'Angola', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AI', 'en', 'Anguilla', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AQ', 'en', 'Antarctica', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AG', 'en', 'Antigua and Barbuda', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AR', 'en', 'Argentina', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AM', 'en', 'Armenia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AW', 'en', 'Aruba', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AU', 'en', 'Australia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AT', 'en', 'Austria', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AZ', 'en', 'Azerbaijan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BS', 'en', 'Bahamas', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BH', 'en', 'Bahrain', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BD', 'en', 'Bangladesh', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BB', 'en', 'Barbados', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BY', 'en', 'Belarus', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BE', 'en', 'Belgium', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BZ', 'en', 'Belize', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BJ', 'en', 'Benin', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BM', 'en', 'Bermuda', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BT', 'en', 'Bhutan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BO', 'en', 'Bolivia, Plurinational State of', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BQ', 'en', 'Bonaire, Sint Eustatius and Saba', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BA', 'en', 'Bosnia and Herzegovina', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BW', 'en', 'Botswana', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BV', 'en', 'Bouvet Island', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BR', 'en', 'Brazil', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'IO', 'en', 'British Indian Ocean Territory', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BN', 'en', 'Brunei Darussalam', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BG', 'en', 'Bulgaria', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BF', 'en', 'Burkina Faso', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BI', 'en', 'Burundi', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CV', 'en', 'Cabo Verde', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'KH', 'en', 'Cambodia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CM', 'en', 'Cameroon', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CA', 'en', 'Canada', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'KY', 'en', 'Cayman Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CF', 'en', 'Central African Republic', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TD', 'en', 'Chad', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CL', 'en', 'Chile', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CN', 'en', 'China', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CX', 'en', 'Christmas Island', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CC', 'en', 'Cocos (Keeling) Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CO', 'en', 'Colombia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'KM', 'en', 'Comoros', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CG', 'en', 'Congo', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CD', 'en', 'Congo, Democratic Republic of the', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CK', 'en', 'Cook Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CR', 'en', 'Costa Rica', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CI', 'en', 'Cote d''Ivoire', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'HR', 'en', 'Croatia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CU', 'en', 'Cuba', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CW', 'en', 'Curacao', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CY', 'en', 'Cyprus', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CZ', 'en', 'Czechia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'DK', 'en', 'Denmark', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'DJ', 'en', 'Djibouti', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'DM', 'en', 'Dominica', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'DO', 'en', 'Dominican Republic', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'EC', 'en', 'Ecuador', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'EG', 'en', 'Egypt', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SV', 'en', 'El Salvador', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GQ', 'en', 'Equatorial Guinea', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'ER', 'en', 'Eritrea', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'EE', 'en', 'Estonia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SZ', 'en', 'Eswatini', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'ET', 'en', 'Ethiopia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'FK', 'en', 'Falkland Islands (Malvinas)', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'FO', 'en', 'Faroe Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'FJ', 'en', 'Fiji', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'FI', 'en', 'Finland', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'FR', 'en', 'France', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GF', 'en', 'French Guiana', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PF', 'en', 'French Polynesia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TF', 'en', 'French Southern Territories', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GA', 'en', 'Gabon', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GM', 'en', 'Gambia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GE', 'en', 'Georgia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'DE', 'en', 'Germany', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GH', 'en', 'Ghana', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GI', 'en', 'Gibraltar', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GR', 'en', 'Greece', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GL', 'en', 'Greenland', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GD', 'en', 'Grenada', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GP', 'en', 'Guadeloupe', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GU', 'en', 'Guam', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GT', 'en', 'Guatemala', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GG', 'en', 'Guernsey', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GN', 'en', 'Guinea', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GW', 'en', 'Guinea-Bissau', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GY', 'en', 'Guyana', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'HT', 'en', 'Haiti', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'HM', 'en', 'Heard Island and McDonald Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'VA', 'en', 'Holy See', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'HN', 'en', 'Honduras', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'HK', 'en', 'Hong Kong', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'HU', 'en', 'Hungary', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'IS', 'en', 'Iceland', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'IN', 'en', 'India', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'ID', 'en', 'Indonesia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'IR', 'en', 'Iran, Islamic Republic of', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'IQ', 'en', 'Iraq', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'IE', 'en', 'Ireland', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'IM', 'en', 'Isle of Man', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'IL', 'en', 'Israel', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'IT', 'en', 'Italy', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'JM', 'en', 'Jamaica', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'JP', 'en', 'Japan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'JE', 'en', 'Jersey', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'JO', 'en', 'Jordan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'KZ', 'en', 'Kazakhstan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'KE', 'en', 'Kenya', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'KI', 'en', 'Kiribati', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'KP', 'en', 'Korea, Democratic People''s Republic of', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'KR', 'en', 'Korea, Republic of', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'KW', 'en', 'Kuwait', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'KG', 'en', 'Kyrgyzstan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'LA', 'en', 'Lao People''s Democratic Republic', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'LV', 'en', 'Latvia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'LB', 'en', 'Lebanon', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'LS', 'en', 'Lesotho', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'LR', 'en', 'Liberia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'LY', 'en', 'Libya', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'LI', 'en', 'Liechtenstein', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'LT', 'en', 'Lithuania', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'LU', 'en', 'Luxembourg', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MO', 'en', 'Macao', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MG', 'en', 'Madagascar', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MW', 'en', 'Malawi', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MY', 'en', 'Malaysia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MV', 'en', 'Maldives', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'ML', 'en', 'Mali', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MT', 'en', 'Malta', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MH', 'en', 'Marshall Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MQ', 'en', 'Martinique', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MR', 'en', 'Mauritania', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MU', 'en', 'Mauritius', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'YT', 'en', 'Mayotte', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MX', 'en', 'Mexico', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'FM', 'en', 'Micronesia, Federated States of', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MD', 'en', 'Moldova, Republic of', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MC', 'en', 'Monaco', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MN', 'en', 'Mongolia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'ME', 'en', 'Montenegro', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MS', 'en', 'Montserrat', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MA', 'en', 'Morocco', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MZ', 'en', 'Mozambique', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MM', 'en', 'Myanmar', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NA', 'en', 'Namibia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NR', 'en', 'Nauru', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NP', 'en', 'Nepal', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NL', 'en', 'Netherlands, Kingdom of the', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NC', 'en', 'New Caledonia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NZ', 'en', 'New Zealand', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NI', 'en', 'Nicaragua', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NE', 'en', 'Niger', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NG', 'en', 'Nigeria', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NU', 'en', 'Niue', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NF', 'en', 'Norfolk Island', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MK', 'en', 'North Macedonia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MP', 'en', 'Northern Mariana Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'NO', 'en', 'Norway', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'OM', 'en', 'Oman', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PK', 'en', 'Pakistan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PW', 'en', 'Palau', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PS', 'en', 'Palestine, State of', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PA', 'en', 'Panama', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PG', 'en', 'Papua New Guinea', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PY', 'en', 'Paraguay', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PE', 'en', 'Peru', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PH', 'en', 'Philippines', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PN', 'en', 'Pitcairn', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PL', 'en', 'Poland', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PT', 'en', 'Portugal', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PR', 'en', 'Puerto Rico', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'QA', 'en', 'Qatar', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'RE', 'en', 'Reunion', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'RO', 'en', 'Romania', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'RU', 'en', 'Russian Federation', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'RW', 'en', 'Rwanda', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'BL', 'en', 'Saint Barthelemy', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SH', 'en', 'Saint Helena, Ascension and Tristan da Cunha', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'KN', 'en', 'Saint Kitts and Nevis', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'LC', 'en', 'Saint Lucia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'MF', 'en', 'Saint Martin (French part)', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'PM', 'en', 'Saint Pierre and Miquelon', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'VC', 'en', 'Saint Vincent and the Grenadines', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'WS', 'en', 'Samoa', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SM', 'en', 'San Marino', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'ST', 'en', 'Sao Tome and Principe', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SA', 'en', 'Saudi Arabia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SN', 'en', 'Senegal', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'RS', 'en', 'Serbia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SC', 'en', 'Seychelles', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SL', 'en', 'Sierra Leone', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SG', 'en', 'Singapore', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SX', 'en', 'Sint Maarten (Dutch part)', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SK', 'en', 'Slovakia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SI', 'en', 'Slovenia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SB', 'en', 'Solomon Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SO', 'en', 'Somalia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'ZA', 'en', 'South Africa', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GS', 'en', 'South Georgia and the South Sandwich Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SS', 'en', 'South Sudan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'ES', 'en', 'Spain', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'LK', 'en', 'Sri Lanka', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SD', 'en', 'Sudan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SR', 'en', 'Suriname', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SJ', 'en', 'Svalbard and Jan Mayen', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SE', 'en', 'Sweden', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'CH', 'en', 'Switzerland', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'SY', 'en', 'Syrian Arab Republic', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TW', 'en', 'Taiwan, Province of China', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TJ', 'en', 'Tajikistan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TZ', 'en', 'Tanzania, United Republic of', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TH', 'en', 'Thailand', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TL', 'en', 'Timor-Leste', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TG', 'en', 'Togo', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TK', 'en', 'Tokelau', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TO', 'en', 'Tonga', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TT', 'en', 'Trinidad and Tobago', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TN', 'en', 'Tunisia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TR', 'en', 'Turkiye', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TM', 'en', 'Turkmenistan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TC', 'en', 'Turks and Caicos Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'TV', 'en', 'Tuvalu', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'UG', 'en', 'Uganda', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'UA', 'en', 'Ukraine', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'AE', 'en', 'United Arab Emirates', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'GB', 'en', 'United Kingdom of Great Britain and Northern Ireland', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'US', 'en', 'United States of America', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'UM', 'en', 'United States Minor Outlying Islands', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'UY', 'en', 'Uruguay', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'UZ', 'en', 'Uzbekistan', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'VU', 'en', 'Vanuatu', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'VE', 'en', 'Venezuela, Bolivarian Republic of', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'VN', 'en', 'Viet Nam', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'VG', 'en', 'Virgin Islands (British)', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'VI', 'en', 'Virgin Islands (U.S.)', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'WF', 'en', 'Wallis and Futuna', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'EH', 'en', 'Western Sahara', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'YE', 'en', 'Yemen', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'ZM', 'en', 'Zambia', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0),
    (gen_random_uuid(), 'ZW', 'en', 'Zimbabwe', 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0);

-- ============================================================================ taxonomy.certification (par 7)

CREATE TABLE taxonomy.certification (
    id              uuid         PRIMARY KEY,
    code            varchar(64)  NOT NULL,
    category        varchar(32),
    country_scope   varchar(2)   REFERENCES taxonomy.country (code),
    industry_scope  varchar(64),
    validity_months int,
    deprecated      boolean      NOT NULL DEFAULT false,
    active          boolean      NOT NULL DEFAULT true,
    sort_order      int          NOT NULL DEFAULT 0,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL,
    created_by      uuid,
    updated_by      uuid,
    version         int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_certification_code UNIQUE (code),
    CONSTRAINT ck_certification_category CHECK (category IS NULL OR category IN
        ('QUALITY', 'SAFETY', 'ENVIRONMENTAL', 'MEDICAL', 'REGULATORY'))
);

-- country_scope NULL = global. CE and REACH are EU-wide (no single ISO country code for
-- "EU"), noted in their description rather than modeled as country_scope.
INSERT INTO taxonomy.certification
    (id, code, category, country_scope, industry_scope, validity_months, deprecated, active, sort_order, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), 'ISO_9001',       'QUALITY',       NULL, NULL,       36,   false, true, 1, now(), now(), 0),
    (gen_random_uuid(), 'ISO_13485',      'MEDICAL',       NULL, NULL,       36,   false, true, 2, now(), now(), 0),
    (gen_random_uuid(), 'ISO_14001',      'ENVIRONMENTAL', NULL, NULL,       36,   false, true, 3, now(), now(), 0),
    (gen_random_uuid(), 'CE',             'SAFETY',        NULL, 'OPTICAL',  NULL, false, true, 4, now(), now(), 0),
    (gen_random_uuid(), 'FDA',            'REGULATORY',    'US', NULL,       NULL, false, true, 5, now(), now(), 0),
    (gen_random_uuid(), 'ROHS',           'ENVIRONMENTAL', NULL, NULL,       NULL, false, true, 6, now(), now(), 0),
    (gen_random_uuid(), 'REACH',          'ENVIRONMENTAL', NULL, NULL,       NULL, false, true, 7, now(), now(), 0),
    (gen_random_uuid(), 'MEDICAL_DEVICE', 'MEDICAL',       NULL, NULL,       NULL, false, true, 8, now(), now(), 0);

CREATE TABLE taxonomy.certification_translation (
    id                  uuid         PRIMARY KEY,
    certification_id    uuid         NOT NULL REFERENCES taxonomy.certification (id),
    locale              varchar(16)  NOT NULL REFERENCES reference.supported_locale (code),
    name                varchar(300),
    description         varchar(4000),
    translation_status  varchar(32)  NOT NULL,
    source_locale       varchar(16)  NOT NULL,
    source_version      int          NOT NULL,
    translation_source  varchar(16)  NOT NULL,
    translator_user_id  uuid,
    reviewer_user_id    uuid,
    approved_at         timestamptz,
    approved_by         uuid,
    is_original         boolean      NOT NULL DEFAULT false,
    created_at          timestamptz  NOT NULL,
    updated_at          timestamptz  NOT NULL,
    created_by          uuid,
    updated_by          uuid,
    version             int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_certification_translation UNIQUE (certification_id, locale),
    CONSTRAINT ck_certification_translation_status CHECK (translation_status IN
        ('MISSING', 'DRAFT', 'MACHINE_TRANSLATED', 'HUMAN_REVIEWED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_certification_translation_source CHECK (translation_source IN ('HUMAN', 'MACHINE', 'IMPORT'))
);
CREATE INDEX ix_certification_translation_certification ON taxonomy.certification_translation (certification_id);
CREATE INDEX ix_certification_translation_locale ON taxonomy.certification_translation (locale);

INSERT INTO taxonomy.certification_translation
    (id, certification_id, locale, name, description, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), c.id, 'en', v.name, v.description, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.certification c
JOIN (VALUES
    ('ISO_9001',       'ISO 9001'::varchar,                  'Quality management systems certification.'::varchar),
    ('ISO_13485',      'ISO 13485',                           'Medical devices quality management systems certification.'),
    ('ISO_14001',      'ISO 14001',                           'Environmental management systems certification.'),
    ('CE',              'CE Marking',                         'Conformite Europeenne marking indicating compliance with EU health, safety, and environmental protection standards.'),
    ('FDA',             'FDA Registration',                   'United States Food and Drug Administration registration or clearance.'),
    ('ROHS',            'RoHS',                                'Restriction of Hazardous Substances compliance.'),
    ('REACH',           'REACH',                               'EU Registration, Evaluation, Authorisation and Restriction of Chemicals compliance.'),
    ('MEDICAL_DEVICE',  'Medical Device Certification',       'General medical device regulatory certification.')
) AS v(code, name, description) ON v.code = c.code;

-- ============================================================================ taxonomy.enumeration (par 3)
-- Shared, versionable enumerations referenced by ENUMERATION/SINGLE_SELECT/MULTI_SELECT
-- attributes.

CREATE TABLE taxonomy.enumeration (
    id               uuid         PRIMARY KEY,
    code             varchar(64)  NOT NULL,
    description_key  varchar(120),
    active           boolean      NOT NULL DEFAULT true,
    created_at       timestamptz  NOT NULL,
    updated_at       timestamptz  NOT NULL,
    created_by       uuid,
    updated_by       uuid,
    version          int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_enumeration_code UNIQUE (code)
);

INSERT INTO taxonomy.enumeration (id, code, description_key, active, created_at, updated_at, version) VALUES
    (gen_random_uuid(), 'FRAME_SHAPE',        NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'FRAME_MATERIAL',     NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'LENS_MATERIAL',      NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'LENS_DESIGN',        NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'LENS_COATING',       NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'LENS_INDEX',         NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'PHOTOCHROMIC_COLOR', NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'POLARIZATION',       NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'GENDER',             NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'AGE_GROUP',          NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'MACHINE_VOLTAGE',    NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'POWER_SUPPLY',       NULL, true, now(), now(), 0),
    (gen_random_uuid(), 'PACKAGING_TYPE',     NULL, true, now(), now(), 0);

CREATE TABLE taxonomy.enumeration_value (
    id              uuid         PRIMARY KEY,
    enumeration_id  uuid         NOT NULL REFERENCES taxonomy.enumeration (id),
    code            varchar(64)  NOT NULL,
    sort_order      int          NOT NULL DEFAULT 0,
    deprecated      boolean      NOT NULL DEFAULT false,
    active          boolean      NOT NULL DEFAULT true,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL,
    created_by      uuid,
    updated_by      uuid,
    version         int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_enumeration_value UNIQUE (enumeration_id, code)
);
CREATE INDEX ix_enumeration_value_enum ON taxonomy.enumeration_value (enumeration_id);

CREATE TABLE taxonomy.enumeration_value_translation (
    id                  uuid         PRIMARY KEY,
    enumeration_value_id uuid        NOT NULL REFERENCES taxonomy.enumeration_value (id),
    locale              varchar(16)  NOT NULL REFERENCES reference.supported_locale (code),
    label               varchar(300),
    description          varchar(2000),
    translation_status  varchar(32)  NOT NULL,
    source_locale       varchar(16)  NOT NULL,
    source_version      int          NOT NULL,
    translation_source  varchar(16)  NOT NULL,
    translator_user_id  uuid,
    reviewer_user_id    uuid,
    approved_at         timestamptz,
    approved_by         uuid,
    is_original         boolean      NOT NULL DEFAULT false,
    created_at          timestamptz  NOT NULL,
    updated_at          timestamptz  NOT NULL,
    created_by          uuid,
    updated_by          uuid,
    version             int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_enumeration_value_translation UNIQUE (enumeration_value_id, locale),
    CONSTRAINT ck_enumeration_value_translation_status CHECK (translation_status IN
        ('MISSING', 'DRAFT', 'MACHINE_TRANSLATED', 'HUMAN_REVIEWED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_enumeration_value_translation_source CHECK (translation_source IN ('HUMAN', 'MACHINE', 'IMPORT'))
);
CREATE INDEX ix_enumeration_value_translation_value ON taxonomy.enumeration_value_translation (enumeration_value_id);
CREATE INDEX ix_enumeration_value_translation_locale ON taxonomy.enumeration_value_translation (locale);

CREATE TABLE taxonomy.enumeration_value_alias (
    id                    uuid         PRIMARY KEY,
    enumeration_value_id  uuid         NOT NULL REFERENCES taxonomy.enumeration_value (id),
    alias                 varchar(120) NOT NULL,
    created_at            timestamptz  NOT NULL,
    updated_at            timestamptz  NOT NULL,
    created_by            uuid,
    updated_by            uuid,
    version               int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_enumeration_value_alias UNIQUE (enumeration_value_id, alias)
);

-- FRAME_SHAPE
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('ROUND',1),('SQUARE',2),('RECTANGLE',3),('OVAL',4),('AVIATOR',5),('CAT_EYE',6),('WAYFARER',7),('GEOMETRIC',8)) AS v(code, sort_order) ON true
WHERE e.code = 'FRAME_SHAPE';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('ROUND','Round'::varchar),('SQUARE','Square'),('RECTANGLE','Rectangle'),('OVAL','Oval'),('AVIATOR','Aviator'),('CAT_EYE','Cat Eye'),('WAYFARER','Wayfarer'),('GEOMETRIC','Geometric')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'FRAME_SHAPE';

-- FRAME_MATERIAL
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('METAL',1),('TITANIUM',2),('ACETATE',3),('TR90',4),('STAINLESS_STEEL',5),('ALUMINUM',6),('WOOD',7),('COMBINATION',8)) AS v(code, sort_order) ON true
WHERE e.code = 'FRAME_MATERIAL';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('METAL','Metal'::varchar),('TITANIUM','Titanium'),('ACETATE','Acetate'),('TR90','TR-90'),('STAINLESS_STEEL','Stainless Steel'),('ALUMINUM','Aluminum'),('WOOD','Wood'),('COMBINATION','Combination')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'FRAME_MATERIAL';

-- LENS_MATERIAL
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('CR39',1),('POLYCARBONATE',2),('TRIVEX',3),('HIGH_INDEX_167',4),('HIGH_INDEX_174',5),('GLASS',6)) AS v(code, sort_order) ON true
WHERE e.code = 'LENS_MATERIAL';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('CR39','CR-39'::varchar),('POLYCARBONATE','Polycarbonate'),('TRIVEX','Trivex'),('HIGH_INDEX_167','High-Index 1.67'),('HIGH_INDEX_174','High-Index 1.74'),('GLASS','Glass')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'LENS_MATERIAL';

-- LENS_DESIGN
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('SINGLE_VISION',1),('BIFOCAL',2),('PROGRESSIVE',3),('OFFICE',4)) AS v(code, sort_order) ON true
WHERE e.code = 'LENS_DESIGN';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('SINGLE_VISION','Single Vision'::varchar),('BIFOCAL','Bifocal'),('PROGRESSIVE','Progressive'),('OFFICE','Office')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'LENS_DESIGN';

-- LENS_COATING
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('ANTI_REFLECTIVE',1),('HARD_COAT',2),('BLUE_LIGHT_FILTER',3),('PHOTOCHROMIC',4),('POLARIZED',5),('MIRROR',6),('HYDROPHOBIC',7)) AS v(code, sort_order) ON true
WHERE e.code = 'LENS_COATING';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('ANTI_REFLECTIVE','Anti-Reflective'::varchar),('HARD_COAT','Hard Coat'),('BLUE_LIGHT_FILTER','Blue Light Filter'),('PHOTOCHROMIC','Photochromic'),('POLARIZED','Polarized'),('MIRROR','Mirror'),('HYDROPHOBIC','Hydrophobic')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'LENS_COATING';

-- LENS_INDEX
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('INDEX_150',1),('INDEX_156',2),('INDEX_160',3),('INDEX_167',4),('INDEX_174',5)) AS v(code, sort_order) ON true
WHERE e.code = 'LENS_INDEX';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('INDEX_150','1.50 Index'::varchar),('INDEX_156','1.56 Index'),('INDEX_160','1.60 Index'),('INDEX_167','1.67 Index'),('INDEX_174','1.74 Index')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'LENS_INDEX';

-- PHOTOCHROMIC_COLOR
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('GREY',1),('BROWN',2),('GREEN',3)) AS v(code, sort_order) ON true
WHERE e.code = 'PHOTOCHROMIC_COLOR';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('GREY','Grey'::varchar),('BROWN','Brown'),('GREEN','Green')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'PHOTOCHROMIC_COLOR';

-- POLARIZATION
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('POLARIZED',1),('NON_POLARIZED',2)) AS v(code, sort_order) ON true
WHERE e.code = 'POLARIZATION';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('POLARIZED','Polarized'::varchar),('NON_POLARIZED','Non-Polarized')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'POLARIZATION';

-- GENDER
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('MEN',1),('WOMEN',2),('UNISEX',3),('KIDS',4)) AS v(code, sort_order) ON true
WHERE e.code = 'GENDER';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('MEN','Men'::varchar),('WOMEN','Women'),('UNISEX','Unisex'),('KIDS','Kids')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'GENDER';

-- AGE_GROUP
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('ADULT',1),('TEEN',2),('CHILD',3),('INFANT',4)) AS v(code, sort_order) ON true
WHERE e.code = 'AGE_GROUP';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('ADULT','Adult'::varchar),('TEEN','Teen'),('CHILD','Child'),('INFANT','Infant')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'AGE_GROUP';

-- MACHINE_VOLTAGE
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('V110',1),('V220',2),('V380',3),('DUAL',4)) AS v(code, sort_order) ON true
WHERE e.code = 'MACHINE_VOLTAGE';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('V110','110V'::varchar),('V220','220V'),('V380','380V'),('DUAL','Dual Voltage')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'MACHINE_VOLTAGE';

-- POWER_SUPPLY
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('SINGLE_PHASE',1),('THREE_PHASE',2)) AS v(code, sort_order) ON true
WHERE e.code = 'POWER_SUPPLY';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('SINGLE_PHASE','Single Phase'::varchar),('THREE_PHASE','Three Phase')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'POWER_SUPPLY';

-- PACKAGING_TYPE
INSERT INTO taxonomy.enumeration_value (id, enumeration_id, code, sort_order, deprecated, active, created_at, updated_at, version)
SELECT gen_random_uuid(), e.id, v.code, v.sort_order, false, true, now(), now(), 0
FROM taxonomy.enumeration e
JOIN (VALUES ('BOX',1),('BLISTER',2),('POUCH',3),('BULK',4),('CARTON',5)) AS v(code, sort_order) ON true
WHERE e.code = 'PACKAGING_TYPE';

INSERT INTO taxonomy.enumeration_value_translation (id, enumeration_value_id, locale, label, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), ev.id, 'en', v.label, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.enumeration_value ev JOIN taxonomy.enumeration e ON e.id = ev.enumeration_id
JOIN (VALUES ('BOX','Box'::varchar),('BLISTER','Blister Pack'),('POUCH','Pouch'),('BULK','Bulk'),('CARTON','Carton')) AS v(code, label) ON v.code = ev.code
WHERE e.code = 'PACKAGING_TYPE';

-- ============================================================================ taxonomy.category (par 2)
-- Unlimited-depth adjacency tree with a materialized ancestor `path` for subtree reads.

CREATE TABLE taxonomy.category (
    id              uuid         PRIMARY KEY,
    parent_id       uuid         REFERENCES taxonomy.category (id),
    code            varchar(64)  NOT NULL,
    depth           int           NOT NULL,
    path            varchar(1000) NOT NULL,
    classification  varchar(32)   NOT NULL,
    active          boolean      NOT NULL DEFAULT true,
    sort_order      int          NOT NULL DEFAULT 0,
    source_version  int          NOT NULL DEFAULT 1,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL,
    created_by      uuid,
    updated_by      uuid,
    version         int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_category_code UNIQUE (code),
    CONSTRAINT ck_category_classification CHECK (classification IN
        ('LENS', 'FRAME', 'SUNGLASSES', 'CONTACT_LENS', 'MACHINERY', 'LAB_EQUIPMENT',
         'ACCESSORY', 'PACKAGING', 'COMPONENT', 'OTHER')),
    CONSTRAINT ck_category_no_self_parent CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE INDEX ix_category_parent ON taxonomy.category (parent_id);
CREATE INDEX ix_category_path ON taxonomy.category (path);

-- Seed tree (see docs/architecture/optical-taxonomy-domain-model.md par 12). OPTICAL is
-- the eyewear umbrella root (LENS/FRAME/SUNGLASSES/CONTACT_LENS branches below it);
-- MACHINERY/LAB_EQUIPMENT/ACCESSORY/PACKAGING are separate top-level roots.

-- depth 0: roots
INSERT INTO taxonomy.category (id, parent_id, code, depth, path, classification, active, sort_order, created_at, updated_at, version) VALUES
    (gen_random_uuid(), NULL, 'OPTICAL',       0, '/OPTICAL/',       'OTHER',         true, 1, now(), now(), 0),
    (gen_random_uuid(), NULL, 'MACHINERY',     0, '/MACHINERY/',     'MACHINERY',     true, 2, now(), now(), 0),
    (gen_random_uuid(), NULL, 'LAB_EQUIPMENT', 0, '/LAB_EQUIPMENT/', 'LAB_EQUIPMENT', true, 3, now(), now(), 0),
    (gen_random_uuid(), NULL, 'ACCESSORY',     0, '/ACCESSORY/',     'ACCESSORY',     true, 4, now(), now(), 0),
    (gen_random_uuid(), NULL, 'PACKAGING',     0, '/PACKAGING/',     'PACKAGING',     true, 5, now(), now(), 0);

-- depth 1: children of OPTICAL and MACHINERY
INSERT INTO taxonomy.category (id, parent_id, code, depth, path, classification, active, sort_order, created_at, updated_at, version)
SELECT gen_random_uuid(), p.id, v.code, 1, p.path || v.code || '/', v.classification, true, v.sort_order, now(), now(), 0
FROM taxonomy.category p
JOIN (VALUES
    ('OPTICAL',   'LENS',         'LENS',         1),
    ('OPTICAL',   'FRAME',        'FRAME',        2),
    ('OPTICAL',   'SUNGLASSES',   'SUNGLASSES',   3),
    ('OPTICAL',   'CONTACT_LENS', 'CONTACT_LENS', 4),
    ('MACHINERY', 'EDGER',        'MACHINERY',    1),
    ('MACHINERY', 'TRACER',       'MACHINERY',    2),
    ('MACHINERY', 'BLOCKER',      'MACHINERY',    3),
    ('MACHINERY', 'DRILLER',      'MACHINERY',    4),
    ('MACHINERY', 'INSPECTION',   'MACHINERY',    5)
) AS v(parent_code, code, classification, sort_order) ON v.parent_code = p.code;

-- depth 2: children of LENS and FRAME
INSERT INTO taxonomy.category (id, parent_id, code, depth, path, classification, active, sort_order, created_at, updated_at, version)
SELECT gen_random_uuid(), p.id, v.code, 2, p.path || v.code || '/', v.classification, true, v.sort_order, now(), now(), 0
FROM taxonomy.category p
JOIN (VALUES
    ('LENS',  'PRESCRIPTION_LENS', 'LENS',  1),
    ('FRAME', 'METAL',             'FRAME', 1),
    ('FRAME', 'ACETATE',           'FRAME', 2),
    ('FRAME', 'TR90',              'FRAME', 3)
) AS v(parent_code, code, classification, sort_order) ON v.parent_code = p.code;

-- depth 3: children of PRESCRIPTION_LENS and METAL
INSERT INTO taxonomy.category (id, parent_id, code, depth, path, classification, active, sort_order, created_at, updated_at, version)
SELECT gen_random_uuid(), p.id, v.code, 3, p.path || v.code || '/', v.classification, true, v.sort_order, now(), now(), 0
FROM taxonomy.category p
JOIN (VALUES
    ('PRESCRIPTION_LENS', 'SINGLE_VISION', 'LENS',  1),
    ('PRESCRIPTION_LENS', 'PROGRESSIVE',   'LENS',  2),
    ('PRESCRIPTION_LENS', 'PHOTOCHROMIC',  'LENS',  3),
    ('PRESCRIPTION_LENS', 'BLUE_FILTER',   'LENS',  4),
    ('METAL',             'TITANIUM',      'FRAME', 1)
) AS v(parent_code, code, classification, sort_order) ON v.parent_code = p.code;

CREATE TABLE taxonomy.category_translation (
    id                  uuid         PRIMARY KEY,
    category_id         uuid         NOT NULL REFERENCES taxonomy.category (id),
    locale              varchar(16)  NOT NULL REFERENCES reference.supported_locale (code),
    name                varchar(300),
    description         varchar(4000),
    translation_status  varchar(32)  NOT NULL,
    source_locale       varchar(16)  NOT NULL,
    source_version      int          NOT NULL,
    translation_source  varchar(16)  NOT NULL,
    translator_user_id  uuid,
    reviewer_user_id    uuid,
    approved_at         timestamptz,
    approved_by         uuid,
    is_original         boolean      NOT NULL DEFAULT false,
    created_at          timestamptz  NOT NULL,
    updated_at          timestamptz  NOT NULL,
    created_by          uuid,
    updated_by          uuid,
    version             int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_category_translation UNIQUE (category_id, locale),
    CONSTRAINT ck_category_translation_status CHECK (translation_status IN
        ('MISSING', 'DRAFT', 'MACHINE_TRANSLATED', 'HUMAN_REVIEWED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_category_translation_source CHECK (translation_source IN ('HUMAN', 'MACHINE', 'IMPORT'))
);
CREATE INDEX ix_category_translation_category ON taxonomy.category_translation (category_id);
CREATE INDEX ix_category_translation_locale ON taxonomy.category_translation (locale);

INSERT INTO taxonomy.category_translation (id, category_id, locale, name, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), c.id, 'en', v.name, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.category c
JOIN (VALUES
    ('OPTICAL',           'Optical'::varchar),
    ('LENS',               'Lens'),
    ('PRESCRIPTION_LENS',  'Prescription Lens'),
    ('SINGLE_VISION',      'Single Vision'),
    ('PROGRESSIVE',        'Progressive'),
    ('PHOTOCHROMIC',       'Photochromic'),
    ('BLUE_FILTER',        'Blue Light Filter'),
    ('FRAME',              'Frame'),
    ('METAL',              'Metal'),
    ('TITANIUM',           'Titanium'),
    ('ACETATE',            'Acetate'),
    ('TR90',               'TR90'),
    ('SUNGLASSES',         'Sunglasses'),
    ('CONTACT_LENS',       'Contact Lens'),
    ('MACHINERY',          'Machinery'),
    ('EDGER',              'Edger'),
    ('TRACER',             'Tracer'),
    ('BLOCKER',            'Blocker'),
    ('DRILLER',            'Driller'),
    ('INSPECTION',         'Inspection Equipment'),
    ('LAB_EQUIPMENT',      'Lab Equipment'),
    ('ACCESSORY',          'Accessory'),
    ('PACKAGING',          'Packaging')
) AS v(code, name) ON v.code = c.code;

CREATE TABLE taxonomy.category_alias (
    id           uuid         PRIMARY KEY,
    category_id  uuid         NOT NULL REFERENCES taxonomy.category (id),
    alias_code   varchar(64)  NOT NULL,
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL,
    created_by   uuid,
    updated_by   uuid,
    version      int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_category_alias UNIQUE (alias_code)
);

CREATE TABLE taxonomy.category_slug (
    id           uuid         PRIMARY KEY,
    category_id  uuid         NOT NULL REFERENCES taxonomy.category (id),
    locale       varchar(16)  NOT NULL REFERENCES reference.supported_locale (code),
    slug         varchar(160) NOT NULL,
    is_primary   boolean      NOT NULL DEFAULT true,
    active       boolean      NOT NULL DEFAULT true,
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL,
    created_by   uuid,
    updated_by   uuid,
    version      int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_category_slug_locale UNIQUE (locale, slug)
);
CREATE INDEX ix_category_slug_category ON taxonomy.category_slug (category_id);

INSERT INTO taxonomy.category_slug (id, category_id, locale, slug, is_primary, active, created_at, updated_at, version)
SELECT gen_random_uuid(), c.id, 'en', v.slug, true, true, now(), now(), 0
FROM taxonomy.category c
JOIN (VALUES
    ('OPTICAL',           'optical'::varchar),
    ('LENS',               'lens'),
    ('PRESCRIPTION_LENS',  'prescription-lens'),
    ('SINGLE_VISION',      'single-vision'),
    ('PROGRESSIVE',        'progressive'),
    ('PHOTOCHROMIC',       'photochromic'),
    ('BLUE_FILTER',        'blue-light-filter'),
    ('FRAME',              'frame'),
    ('METAL',              'metal'),
    ('TITANIUM',           'titanium'),
    ('ACETATE',            'acetate'),
    ('TR90',               'tr90'),
    ('SUNGLASSES',         'sunglasses'),
    ('CONTACT_LENS',       'contact-lens'),
    ('MACHINERY',          'machinery'),
    ('EDGER',              'edger'),
    ('TRACER',             'tracer'),
    ('BLOCKER',            'blocker'),
    ('DRILLER',            'driller'),
    ('INSPECTION',         'inspection-equipment'),
    ('LAB_EQUIPMENT',      'lab-equipment'),
    ('ACCESSORY',          'accessory'),
    ('PACKAGING',          'packaging')
) AS v(code, slug) ON v.code = c.code;

-- ============================================================================ taxonomy.attribute (par 4)
-- Master specification-field registry. Data types are a closed, engine-coded primitive
-- set (ADR-025); cross-field integrity (unit only with MEASUREMENT/RANGE, enumeration_id
-- only with ENUMERATION/SINGLE_SELECT/MULTI_SELECT) is validated in the service, not SQL.

CREATE TABLE taxonomy.attribute (
    id              uuid          PRIMARY KEY,
    code            varchar(64)   NOT NULL,
    data_type       varchar(32)   NOT NULL,
    unit_code       varchar(32)   REFERENCES taxonomy.unit_of_measure (code),
    enumeration_id  uuid          REFERENCES taxonomy.enumeration (id),
    min_value       numeric,
    max_value       numeric,
    min_length      int,
    max_length      int,
    regex_pattern   varchar(500),
    searchable      boolean       NOT NULL DEFAULT false,
    filterable      boolean       NOT NULL DEFAULT false,
    sortable        boolean       NOT NULL DEFAULT false,
    comparable      boolean       NOT NULL DEFAULT false,
    facetable       boolean       NOT NULL DEFAULT false,
    seo             boolean       NOT NULL DEFAULT false,
    visible         boolean       NOT NULL DEFAULT true,
    deprecated      boolean       NOT NULL DEFAULT false,
    sort_order      int           NOT NULL DEFAULT 0,
    ai_metadata     jsonb,
    source_version  int           NOT NULL DEFAULT 1,
    created_at      timestamptz   NOT NULL,
    updated_at      timestamptz   NOT NULL,
    created_by      uuid,
    updated_by      uuid,
    version         int           NOT NULL DEFAULT 0,
    CONSTRAINT uq_attribute_code UNIQUE (code),
    CONSTRAINT ck_attribute_data_type CHECK (data_type IN (
        'STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'ENUMERATION', 'MEASUREMENT',
        'RANGE', 'JSON', 'REFERENCE', 'MULTI_SELECT', 'SINGLE_SELECT', 'FILE_REFERENCE',
        'COLOR', 'COUNTRY', 'LANGUAGE', 'BRAND', 'CERTIFICATION'))
);
CREATE INDEX ix_attribute_enumeration ON taxonomy.attribute (enumeration_id);
CREATE INDEX ix_attribute_unit ON taxonomy.attribute (unit_code);

-- Frame attributes (sort_order 1-9): MEASUREMENT/COLOR dimension attrs are filterable,
-- comparable, and searchable; ENUMERATION/COLOR attrs are also filterable and facetable.
INSERT INTO taxonomy.attribute
    (id, code, data_type, unit_code, enumeration_id, min_value, max_value, searchable, filterable, sortable, comparable, facetable, seo, visible, deprecated, sort_order, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), 'EYE_SIZE',       'MEASUREMENT', 'mm', NULL, 30,  70,  true, true, true, true, false, false, true, false, 1, now(), now(), 0),
    (gen_random_uuid(), 'BRIDGE_WIDTH',   'MEASUREMENT', 'mm', NULL, 10,  30,  true, true, true, true, false, false, true, false, 2, now(), now(), 0),
    (gen_random_uuid(), 'TEMPLE_LENGTH',  'MEASUREMENT', 'mm', NULL, 100, 160, true, true, true, true, false, false, true, false, 3, now(), now(), 0),
    (gen_random_uuid(), 'FRAME_WIDTH',    'MEASUREMENT', 'mm', NULL, 100, 160, true, true, true, true, false, false, true, false, 4, now(), now(), 0),
    (gen_random_uuid(), 'LENS_HEIGHT',    'MEASUREMENT', 'mm', NULL, 20,  70,  true, true, true, true, false, false, true, false, 5, now(), now(), 0),
    (gen_random_uuid(), 'FRAME_MATERIAL', 'ENUMERATION', NULL, (SELECT id FROM taxonomy.enumeration WHERE code = 'FRAME_MATERIAL'), NULL, NULL, true, true, false, false, true, false, true, false, 6, now(), now(), 0),
    (gen_random_uuid(), 'FRAME_SHAPE',    'ENUMERATION', NULL, (SELECT id FROM taxonomy.enumeration WHERE code = 'FRAME_SHAPE'),    NULL, NULL, true, true, false, false, true, false, true, false, 7, now(), now(), 0),
    (gen_random_uuid(), 'GENDER',         'ENUMERATION', NULL, (SELECT id FROM taxonomy.enumeration WHERE code = 'GENDER'),         NULL, NULL, true, true, false, false, true, false, true, false, 8, now(), now(), 0),
    (gen_random_uuid(), 'FRAME_COLOR',    'COLOR',       NULL, NULL,                                                                NULL, NULL, true, true, false, false, true, false, true, false, 9, now(), now(), 0);

-- Lens attributes (sort_order 10-15)
INSERT INTO taxonomy.attribute
    (id, code, data_type, unit_code, enumeration_id, min_value, max_value, searchable, filterable, sortable, comparable, facetable, seo, visible, deprecated, sort_order, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), 'LENS_MATERIAL', 'ENUMERATION', NULL, (SELECT id FROM taxonomy.enumeration WHERE code = 'LENS_MATERIAL'), NULL, NULL, true, true, false, false, true, false, true, false, 10, now(), now(), 0),
    (gen_random_uuid(), 'LENS_INDEX',    'ENUMERATION', NULL, (SELECT id FROM taxonomy.enumeration WHERE code = 'LENS_INDEX'),    NULL, NULL, true, true, false, false, true, false, true, false, 11, now(), now(), 0),
    (gen_random_uuid(), 'LENS_DESIGN',   'ENUMERATION', NULL, (SELECT id FROM taxonomy.enumeration WHERE code = 'LENS_DESIGN'),   NULL, NULL, true, true, false, false, true, false, true, false, 12, now(), now(), 0),
    (gen_random_uuid(), 'LENS_COATING',  'ENUMERATION', NULL, (SELECT id FROM taxonomy.enumeration WHERE code = 'LENS_COATING'),  NULL, NULL, true, true, false, false, true, false, true, false, 13, now(), now(), 0),
    (gen_random_uuid(), 'LENS_DIAMETER', 'MEASUREMENT', 'mm', NULL, 50, 85, true, true, true, true, false, false, true, false, 14, now(), now(), 0),
    (gen_random_uuid(), 'BASE_CURVE',    'DECIMAL',     NULL, NULL, 0,  10, true, true, true, true, false, false, true, false, 15, now(), now(), 0);

-- Machinery attributes (sort_order 16-18). POWER_WATT is INTEGER (not MEASUREMENT) to
-- avoid introducing a new POWER_ELECTRICAL unit family for a single attribute.
INSERT INTO taxonomy.attribute
    (id, code, data_type, unit_code, enumeration_id, min_value, max_value, searchable, filterable, sortable, comparable, facetable, seo, visible, deprecated, sort_order, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), 'VOLTAGE',    'ENUMERATION', NULL, (SELECT id FROM taxonomy.enumeration WHERE code = 'MACHINE_VOLTAGE'), NULL, NULL, true, true, false, false, true, false, true, false, 16, now(), now(), 0),
    (gen_random_uuid(), 'POWER_WATT', 'INTEGER',     NULL, NULL, 0, NULL, true, true, true, true, false, false, true, false, 17, now(), now(), 0),
    (gen_random_uuid(), 'NET_WEIGHT', 'MEASUREMENT', 'kg', NULL, 0, 2000, true, true, true, true, false, false, true, false, 18, now(), now(), 0);

CREATE TABLE taxonomy.attribute_translation (
    id                  uuid         PRIMARY KEY,
    attribute_id        uuid         NOT NULL REFERENCES taxonomy.attribute (id),
    locale              varchar(16)  NOT NULL REFERENCES reference.supported_locale (code),
    name                varchar(300),
    description         varchar(4000),
    translation_status  varchar(32)  NOT NULL,
    source_locale       varchar(16)  NOT NULL,
    source_version      int          NOT NULL,
    translation_source  varchar(16)  NOT NULL,
    translator_user_id  uuid,
    reviewer_user_id    uuid,
    approved_at         timestamptz,
    approved_by         uuid,
    is_original         boolean      NOT NULL DEFAULT false,
    created_at          timestamptz  NOT NULL,
    updated_at          timestamptz  NOT NULL,
    created_by          uuid,
    updated_by          uuid,
    version             int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_attribute_translation UNIQUE (attribute_id, locale),
    CONSTRAINT ck_attribute_translation_status CHECK (translation_status IN
        ('MISSING', 'DRAFT', 'MACHINE_TRANSLATED', 'HUMAN_REVIEWED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_attribute_translation_source CHECK (translation_source IN ('HUMAN', 'MACHINE', 'IMPORT'))
);
CREATE INDEX ix_attribute_translation_attribute ON taxonomy.attribute_translation (attribute_id);
CREATE INDEX ix_attribute_translation_locale ON taxonomy.attribute_translation (locale);

INSERT INTO taxonomy.attribute_translation (id, attribute_id, locale, name, translation_status, source_locale, source_version, translation_source, is_original, approved_at, created_at, updated_at, version)
SELECT gen_random_uuid(), a.id, 'en', v.name, 'APPROVED', 'en', 1, 'IMPORT', true, now(), now(), now(), 0
FROM taxonomy.attribute a
JOIN (VALUES
    ('EYE_SIZE',       'Eye Size'::varchar),
    ('BRIDGE_WIDTH',   'Bridge Width'),
    ('TEMPLE_LENGTH',  'Temple Length'),
    ('FRAME_WIDTH',    'Frame Width'),
    ('LENS_HEIGHT',    'Lens Height'),
    ('FRAME_MATERIAL', 'Frame Material'),
    ('FRAME_SHAPE',    'Frame Shape'),
    ('GENDER',         'Gender'),
    ('FRAME_COLOR',    'Frame Color'),
    ('LENS_MATERIAL',  'Lens Material'),
    ('LENS_INDEX',     'Lens Index'),
    ('LENS_DESIGN',    'Lens Design'),
    ('LENS_COATING',   'Lens Coating'),
    ('LENS_DIAMETER',  'Lens Diameter'),
    ('BASE_CURVE',     'Base Curve'),
    ('VOLTAGE',        'Voltage'),
    ('POWER_WATT',     'Power (Watts)'),
    ('NET_WEIGHT',     'Net Weight')
) AS v(code, name) ON v.code = a.code;

-- taxonomy.attribute_allowed_value(+_translation): inline controlled values for
-- attributes not backed by a shared enumeration (rare; prefer enumerations per
-- ADR-025). No seed data -- every ENUMERATION-typed attribute above binds to a shared
-- taxonomy.enumeration instead.
CREATE TABLE taxonomy.attribute_allowed_value (
    id            uuid         PRIMARY KEY,
    attribute_id  uuid         NOT NULL REFERENCES taxonomy.attribute (id),
    value_code    varchar(64)  NOT NULL,
    sort_order    int          NOT NULL DEFAULT 0,
    active        boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL,
    created_by    uuid,
    updated_by    uuid,
    version       int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_attribute_allowed_value UNIQUE (attribute_id, value_code)
);
CREATE INDEX ix_attribute_allowed_value_attribute ON taxonomy.attribute_allowed_value (attribute_id);

CREATE TABLE taxonomy.attribute_allowed_value_translation (
    id                     uuid         PRIMARY KEY,
    attribute_allowed_value_id uuid    NOT NULL REFERENCES taxonomy.attribute_allowed_value (id),
    locale                 varchar(16)  NOT NULL REFERENCES reference.supported_locale (code),
    label                  varchar(300),
    translation_status     varchar(32)  NOT NULL,
    source_locale          varchar(16)  NOT NULL,
    source_version         int          NOT NULL,
    translation_source     varchar(16)  NOT NULL,
    translator_user_id     uuid,
    reviewer_user_id       uuid,
    approved_at            timestamptz,
    approved_by            uuid,
    is_original            boolean      NOT NULL DEFAULT false,
    created_at             timestamptz  NOT NULL,
    updated_at             timestamptz  NOT NULL,
    created_by             uuid,
    updated_by             uuid,
    version                int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_attribute_allowed_value_translation UNIQUE (attribute_allowed_value_id, locale),
    CONSTRAINT ck_attribute_allowed_value_translation_status CHECK (translation_status IN
        ('MISSING', 'DRAFT', 'MACHINE_TRANSLATED', 'HUMAN_REVIEWED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_attribute_allowed_value_translation_source CHECK (translation_source IN ('HUMAN', 'MACHINE', 'IMPORT'))
);
CREATE INDEX ix_attribute_allowed_value_translation_value ON taxonomy.attribute_allowed_value_translation (attribute_allowed_value_id);
CREATE INDEX ix_attribute_allowed_value_translation_locale ON taxonomy.attribute_allowed_value_translation (locale);

-- ============================================================================ taxonomy.brand (par 8)
-- Global brand registry. No seed rows (not listed in domain-model par 12 seed data) --
-- brands are created at runtime by platform staff / supplier claims.

CREATE TABLE taxonomy.brand (
    id                uuid         PRIMARY KEY,
    code              varchar(64)  NOT NULL,
    brand_type        varchar(32)  NOT NULL,
    canonical_name    varchar(200) NOT NULL,
    owner_company     varchar(300),
    manufacturer      varchar(300),
    country_code      varchar(2)   REFERENCES taxonomy.country (code),
    website           varchar(300),
    logo_storage_key  varchar(300),
    approval_status   varchar(32)  NOT NULL DEFAULT 'PENDING',
    active            boolean      NOT NULL DEFAULT true,
    source_version    int          NOT NULL DEFAULT 1,
    created_at        timestamptz  NOT NULL,
    updated_at        timestamptz  NOT NULL,
    created_by        uuid,
    updated_by        uuid,
    version           int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_brand_code UNIQUE (code),
    CONSTRAINT ck_brand_type CHECK (brand_type IN
        ('LENS', 'FRAME', 'SUNGLASSES', 'CONTACT_LENS', 'MACHINE', 'ACCESSORY', 'GENERAL')),
    CONSTRAINT ck_brand_approval_status CHECK (approval_status IN
        ('PENDING', 'APPROVED', 'REJECTED', 'DEPRECATED'))
);
CREATE INDEX ix_brand_country ON taxonomy.brand (country_code);

CREATE TABLE taxonomy.brand_translation (
    id                  uuid         PRIMARY KEY,
    brand_id            uuid         NOT NULL REFERENCES taxonomy.brand (id),
    locale              varchar(16)  NOT NULL REFERENCES reference.supported_locale (code),
    display_name        varchar(200),
    description         varchar(2000),
    translation_status  varchar(32)  NOT NULL,
    source_locale       varchar(16)  NOT NULL,
    source_version      int          NOT NULL,
    translation_source  varchar(16)  NOT NULL,
    translator_user_id  uuid,
    reviewer_user_id    uuid,
    approved_at         timestamptz,
    approved_by         uuid,
    is_original         boolean      NOT NULL DEFAULT false,
    created_at          timestamptz  NOT NULL,
    updated_at          timestamptz  NOT NULL,
    created_by          uuid,
    updated_by          uuid,
    version             int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_brand_translation UNIQUE (brand_id, locale),
    CONSTRAINT ck_brand_translation_status CHECK (translation_status IN
        ('MISSING', 'DRAFT', 'MACHINE_TRANSLATED', 'HUMAN_REVIEWED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_brand_translation_source CHECK (translation_source IN ('HUMAN', 'MACHINE', 'IMPORT'))
);
CREATE INDEX ix_brand_translation_brand ON taxonomy.brand_translation (brand_id);
CREATE INDEX ix_brand_translation_locale ON taxonomy.brand_translation (locale);

CREATE TABLE taxonomy.brand_alias (
    id          uuid         PRIMARY KEY,
    brand_id    uuid         NOT NULL REFERENCES taxonomy.brand (id),
    alias       varchar(200) NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    created_by  uuid,
    updated_by  uuid,
    version     int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_brand_alias UNIQUE (brand_id, alias)
);

-- ============================================================================ taxonomy.specification_template (par 9)
-- One template per category; the effective template (own + inherited ancestor
-- attributes) is resolved by the service, never duplicated in the DB.
--
-- DEVIATION: the domain-model doc lists a `version int NOT NULL DEFAULT 1` column on
-- this table for the *structural* template version (bumped on attribute add/remove),
-- separate in meaning from the standard audit-tail optimistic-lock `version` column
-- every business table also carries. Two same-named columns is impossible in SQL, so the
-- structural-version column is named `template_version` here to avoid colliding with the
-- audit-tail `version`. The audit-tail `version` still exists and still means what it
-- means everywhere else (optimistic locking), unchanged.

CREATE TABLE taxonomy.specification_template (
    id                uuid         PRIMARY KEY,
    category_id       uuid         NOT NULL REFERENCES taxonomy.category (id),
    code              varchar(64)  NOT NULL,
    template_version  int          NOT NULL DEFAULT 1,
    active            boolean      NOT NULL DEFAULT true,
    created_at        timestamptz  NOT NULL,
    updated_at        timestamptz  NOT NULL,
    created_by        uuid,
    updated_by        uuid,
    version           int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_specification_template_category UNIQUE (category_id),
    CONSTRAINT uq_specification_template_code UNIQUE (code)
);

INSERT INTO taxonomy.specification_template (id, category_id, code, template_version, active, created_at, updated_at, version)
SELECT gen_random_uuid(), c.id, v.code, 1, true, now(), now(), 0
FROM taxonomy.category c
JOIN (VALUES
    ('FRAME', 'TEMPLATE_FRAME'::varchar),
    ('LENS',  'TEMPLATE_LENS'),
    ('EDGER', 'TEMPLATE_EDGER')
) AS v(category_code, code) ON v.category_code = c.code;

CREATE TABLE taxonomy.specification_template_attribute (
    id             uuid         PRIMARY KEY,
    template_id    uuid         NOT NULL REFERENCES taxonomy.specification_template (id),
    attribute_id   uuid         NOT NULL REFERENCES taxonomy.attribute (id),
    required       boolean      NOT NULL DEFAULT false,
    conditional    jsonb,
    default_value  varchar(500),
    sort_order     int          NOT NULL DEFAULT 0,
    created_at     timestamptz  NOT NULL,
    updated_at     timestamptz  NOT NULL,
    created_by     uuid,
    updated_by     uuid,
    version        int          NOT NULL DEFAULT 0,
    CONSTRAINT uq_template_attribute UNIQUE (template_id, attribute_id)
);
CREATE INDEX ix_template_attribute_template ON taxonomy.specification_template_attribute (template_id);

-- FRAME template attributes
INSERT INTO taxonomy.specification_template_attribute (id, template_id, attribute_id, required, sort_order, created_at, updated_at, version)
SELECT gen_random_uuid(), t.id, a.id, v.required, v.sort_order, now(), now(), 0
FROM taxonomy.specification_template t
JOIN (VALUES
    ('EYE_SIZE',       true,  1),
    ('BRIDGE_WIDTH',   true,  2),
    ('TEMPLE_LENGTH',  false, 3),
    ('FRAME_WIDTH',    false, 4),
    ('FRAME_MATERIAL', true,  5),
    ('FRAME_SHAPE',    false, 6),
    ('GENDER',         false, 7),
    ('FRAME_COLOR',    false, 8)
) AS v(attribute_code, required, sort_order) ON true
JOIN taxonomy.attribute a ON a.code = v.attribute_code
WHERE t.code = 'TEMPLATE_FRAME';

-- LENS template attributes
INSERT INTO taxonomy.specification_template_attribute (id, template_id, attribute_id, required, sort_order, created_at, updated_at, version)
SELECT gen_random_uuid(), t.id, a.id, v.required, v.sort_order, now(), now(), 0
FROM taxonomy.specification_template t
JOIN (VALUES
    ('LENS_MATERIAL', true,  1),
    ('LENS_INDEX',    true,  2),
    ('LENS_DESIGN',   true,  3),
    ('LENS_COATING',  false, 4),
    ('LENS_DIAMETER', false, 5),
    ('BASE_CURVE',    false, 6)
) AS v(attribute_code, required, sort_order) ON true
JOIN taxonomy.attribute a ON a.code = v.attribute_code
WHERE t.code = 'TEMPLATE_LENS';

-- EDGER template attributes
INSERT INTO taxonomy.specification_template_attribute (id, template_id, attribute_id, required, sort_order, created_at, updated_at, version)
SELECT gen_random_uuid(), t.id, a.id, v.required, v.sort_order, now(), now(), 0
FROM taxonomy.specification_template t
JOIN (VALUES
    ('VOLTAGE',    true,  1),
    ('POWER_WATT', false, 2),
    ('NET_WEIGHT', false, 3)
) AS v(attribute_code, required, sort_order) ON true
JOIN taxonomy.attribute a ON a.code = v.attribute_code
WHERE t.code = 'TEMPLATE_EDGER';
