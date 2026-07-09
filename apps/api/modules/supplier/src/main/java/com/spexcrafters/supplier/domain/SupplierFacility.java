package com.spexcrafters.supplier.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A supplier facility (factory/office/warehouse), typed by a stable code (class C). Address
 * disclosure is governed by {@link AddressPrivacy}. Localized name/description live in
 * {@link SupplierFacilityTranslation}; {@code sourceVersion} drives their stale detection.
 */
@Entity
@Table(name = "supplier_facility", schema = "supplier")
public class SupplierFacility extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "facility_type_code", nullable = false, length = 64)
    private String facilityTypeCode;

    @Column(name = "country", length = 2, nullable = false)
    private String country;

    @Column(name = "region", length = 200)
    private String region;

    @Column(name = "city", length = 200)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_privacy", nullable = false, length = 32)
    private AddressPrivacy addressPrivacy;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership", nullable = false, length = 32)
    private FacilityOwnership ownership;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "source_version", nullable = false)
    private int sourceVersion;

    protected SupplierFacility() {
        // JPA
    }

    public SupplierFacility(UUID id, UUID supplierId, String facilityTypeCode, String country,
            AddressPrivacy addressPrivacy, FacilityOwnership ownership, boolean isPublic) {
        this.id = id;
        this.supplierId = supplierId;
        this.facilityTypeCode = facilityTypeCode;
        this.country = country == null ? null : country.trim().toUpperCase(java.util.Locale.ROOT);
        this.addressPrivacy = addressPrivacy;
        this.ownership = ownership;
        this.isPublic = isPublic;
        this.sourceVersion = 1;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public String getFacilityTypeCode() {
        return facilityTypeCode;
    }

    public String getCountry() {
        return country;
    }

    public String getRegion() {
        return region;
    }

    public String getCity() {
        return city;
    }

    public AddressPrivacy getAddressPrivacy() {
        return addressPrivacy;
    }

    public FacilityOwnership getOwnership() {
        return ownership;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public int getSourceVersion() {
        return sourceVersion;
    }
}
