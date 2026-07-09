package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** ISO 3166-1 country (domain-model §6). Stable id is the alpha-2 {@code code} (natural PK). */
@Entity
@Table(name = "country", schema = "taxonomy")
public class Country extends AuditedEntity {

    @Id
    @Column(name = "code", length = 2)
    private String code;

    @Column(name = "alpha3", nullable = false, length = 3)
    private String alpha3;

    @Column(name = "numeric_code", nullable = false, length = 3)
    private String numericCode;

    @Column(name = "region", length = 64)
    private String region;

    @Column(name = "subregion", length = 64)
    private String subregion;

    @Column(name = "continent", length = 32)
    private String continent;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Country() {
        // JPA
    }

    public String getCode() {
        return code;
    }

    public String getAlpha3() {
        return alpha3;
    }

    public String getNumericCode() {
        return numericCode;
    }

    public String getRegion() {
        return region;
    }

    public String getSubregion() {
        return subregion;
    }

    public String getContinent() {
        return continent;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
