package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A global brand registry entry (domain-model §8). Future supplier/product claims reference
 * brands by id/code, never free-text. {@code canonicalName} is the language-neutral Latin
 * canonical; localized renderings live in {@link BrandTranslation}. New brands start
 * {@code PENDING} until approved.
 */
@Entity
@Table(name = "brand", schema = "taxonomy")
public class Brand extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "brand_type", nullable = false, length = 32)
    private BrandType brandType;

    @Column(name = "canonical_name", nullable = false, length = 200)
    private String canonicalName;

    @Column(name = "owner_company", length = 300)
    private String ownerCompany;

    @Column(name = "manufacturer", length = 300)
    private String manufacturer;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "website", length = 300)
    private String website;

    @Column(name = "logo_storage_key", length = 300)
    private String logoStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 32)
    private BrandApprovalStatus approvalStatus;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "source_version", nullable = false)
    private int sourceVersion;

    protected Brand() {
        // JPA
    }

    public Brand(UUID id, String code, BrandType brandType, String canonicalName, String ownerCompany,
            String manufacturer, String countryCode, String website) {
        this.id = id;
        this.code = code;
        this.brandType = brandType;
        this.canonicalName = canonicalName;
        this.ownerCompany = ownerCompany;
        this.manufacturer = manufacturer;
        this.countryCode = countryCode;
        this.website = website;
        this.approvalStatus = BrandApprovalStatus.PENDING;
        this.active = true;
        this.sourceVersion = 1;
    }

    public int bumpSourceVersion() {
        return ++sourceVersion;
    }

    public void setApprovalStatus(BrandApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public BrandType getBrandType() {
        return brandType;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getOwnerCompany() {
        return ownerCompany;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getWebsite() {
        return website;
    }

    public String getLogoStorageKey() {
        return logoStorageKey;
    }

    public BrandApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public boolean isActive() {
        return active;
    }

    public int getSourceVersion() {
        return sourceVersion;
    }
}
