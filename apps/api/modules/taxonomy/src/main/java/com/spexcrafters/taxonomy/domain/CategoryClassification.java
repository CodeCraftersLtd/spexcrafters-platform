package com.spexcrafters.taxonomy.domain;

/** High-level product family of a category (domain-model §2). Mirrors the DB CHECK. */
public enum CategoryClassification {
    LENS,
    FRAME,
    SUNGLASSES,
    CONTACT_LENS,
    MACHINERY,
    LAB_EQUIPMENT,
    ACCESSORY,
    PACKAGING,
    COMPONENT,
    OTHER
}
