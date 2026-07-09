package com.spexcrafters.taxonomy.domain;

/**
 * The closed, engine-coded primitive data-type vocabulary (ADR-025 §2). Mirrors the DB
 * {@code ck_attribute_data_type} CHECK. Adding a primitive is a deliberate platform change
 * (enum value + CHECK alter + engine code), never routine data entry.
 */
public enum AttributeDataType {
    STRING,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    DATE,
    ENUMERATION,
    MEASUREMENT,
    RANGE,
    JSON,
    REFERENCE,
    MULTI_SELECT,
    SINGLE_SELECT,
    FILE_REFERENCE,
    COLOR,
    COUNTRY,
    LANGUAGE,
    BRAND,
    CERTIFICATION
}
