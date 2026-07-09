package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Certification registry entry (domain-model §7). Stable id {@code code}; PK is a UUIDv7. */
@Entity
@Table(name = "certification", schema = "taxonomy")
public class Certification extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32)
    private CertificationCategory category;

    @Column(name = "country_scope", length = 2)
    private String countryScope;

    @Column(name = "industry_scope", length = 64)
    private String industryScope;

    @Column(name = "validity_months")
    private Integer validityMonths;

    @Column(name = "deprecated", nullable = false)
    private boolean deprecated;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Certification() {
        // JPA
    }

    public Certification(UUID id, String code, CertificationCategory category, String countryScope,
            String industryScope, Integer validityMonths, int sortOrder) {
        this.id = id;
        this.code = code;
        this.category = category;
        this.countryScope = countryScope;
        this.industryScope = industryScope;
        this.validityMonths = validityMonths;
        this.deprecated = false;
        this.active = true;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public CertificationCategory getCategory() {
        return category;
    }

    public String getCountryScope() {
        return countryScope;
    }

    public String getIndustryScope() {
        return industryScope;
    }

    public Integer getValidityMonths() {
        return validityMonths;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
