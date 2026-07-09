package com.spexcrafters.supplier.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Non-translatable supplier profile fields (localization class E) plus the original-language
 * trading name (class D authoritative value). Localized trading names and the marketing
 * descriptions live in {@link SupplierProfileTranslation} rows (ADR-020).
 *
 * <p>{@code sourceVersion} is monotonic: it is bumped whenever a translatable source field
 * changes, which marks dependent translations stale (ADR-020 stale detection). Legal and
 * registration fields (class E) are never machine-translated and are rendered as-is.
 */
@Entity
@Table(name = "supplier_profile", schema = "supplier")
public class SupplierProfile extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "supplier_id", nullable = false, unique = true)
    private UUID supplierId;

    @Column(name = "legal_name", nullable = false, length = 300)
    private String legalName;

    @Column(name = "registered_legal_name_translated", length = 300)
    private String registeredLegalNameTranslated;

    @Column(name = "trading_name", length = 300)
    private String tradingName;

    @Column(name = "registration_number", length = 120)
    private String registrationNumber;

    @Column(name = "country_of_registration", length = 2)
    private String countryOfRegistration;

    @Column(name = "registration_authority", length = 300)
    private String registrationAuthority;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "company_type_code", length = 64)
    private String companyTypeCode;

    @Column(name = "year_established")
    private Integer yearEstablished;

    @Column(name = "employee_range", length = 32)
    private String employeeRange;

    @Column(name = "website", length = 300)
    private String website;

    @Column(name = "business_email", length = 254)
    private String businessEmail;

    @Column(name = "business_phone", length = 40)
    private String businessPhone;

    @Column(name = "source_version", nullable = false)
    private int sourceVersion;

    protected SupplierProfile() {
        // JPA
    }

    public SupplierProfile(UUID id, UUID supplierId, String legalName) {
        if (id == null || supplierId == null) {
            throw new IllegalArgumentException("id and supplierId are required");
        }
        if (legalName == null || legalName.isBlank()) {
            throw new IllegalArgumentException("legalName is required");
        }
        this.id = id;
        this.supplierId = supplierId;
        this.legalName = legalName.trim();
        this.sourceVersion = 1;
    }

    /**
     * Applies a draft update. Non-translatable fields are set directly; a change to the
     * translatable {@code tradingName} bumps {@code sourceVersion}, marking dependent
     * translations stale. {@code null} arguments leave the corresponding field unchanged;
     * blanking a field uses an explicit sentinel handled by the service.
     */
    public boolean applyTradingName(String newTradingName) {
        boolean changed = !java.util.Objects.equals(normalize(newTradingName), tradingName);
        if (changed) {
            this.tradingName = normalize(newTradingName);
            this.sourceVersion++;
        }
        return changed;
    }

    /**
     * Bumps the monotonic source version, marking dependent (non-original) translations stale.
     * Called when the original-language content (e.g. descriptions held on the original
     * translation row) changes. Returns the new version.
     */
    public int bumpSourceVersion() {
        return ++this.sourceVersion;
    }

    public void setLegalName(String legalName) {
        if (legalName != null && !legalName.isBlank()) {
            this.legalName = legalName.trim();
        }
    }

    public void setRegisteredLegalNameTranslated(String value) {
        this.registeredLegalNameTranslated = normalize(value);
    }

    public void setRegistrationNumber(String value) {
        this.registrationNumber = normalize(value);
    }

    public void setCountryOfRegistration(String value) {
        this.countryOfRegistration = value == null ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public void setRegistrationAuthority(String value) {
        this.registrationAuthority = normalize(value);
    }

    public void setRegistrationDate(LocalDate value) {
        this.registrationDate = value;
    }

    public void setCompanyTypeCode(String value) {
        this.companyTypeCode = normalize(value);
    }

    public void setYearEstablished(Integer value) {
        this.yearEstablished = value;
    }

    public void setEmployeeRange(String value) {
        this.employeeRange = normalize(value);
    }

    public void setWebsite(String value) {
        this.website = normalize(value);
    }

    public void setBusinessEmail(String value) {
        this.businessEmail = normalize(value);
    }

    public void setBusinessPhone(String value) {
        this.businessPhone = normalize(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getRegisteredLegalNameTranslated() {
        return registeredLegalNameTranslated;
    }

    public String getTradingName() {
        return tradingName;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getCountryOfRegistration() {
        return countryOfRegistration;
    }

    public String getRegistrationAuthority() {
        return registrationAuthority;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public String getCompanyTypeCode() {
        return companyTypeCode;
    }

    public Integer getYearEstablished() {
        return yearEstablished;
    }

    public String getEmployeeRange() {
        return employeeRange;
    }

    public String getWebsite() {
        return website;
    }

    public String getBusinessEmail() {
        return businessEmail;
    }

    public String getBusinessPhone() {
        return businessPhone;
    }

    public int getSourceVersion() {
        return sourceVersion;
    }
}
