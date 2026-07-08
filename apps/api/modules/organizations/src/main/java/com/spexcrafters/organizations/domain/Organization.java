package com.spexcrafters.organizations.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An organization (tenant root of the organizations bounded context). Every
 * organization-owned row carries this aggregate's id; profile updates use the inherited
 * optimistic {@code version}, and owner-affecting membership mutations serialize on this
 * row via a {@code PESSIMISTIC_WRITE} lock (last-owner invariant).
 */
@Entity
@Table(name = "organization", schema = "organizations")
public class Organization extends AuditedEntity {

    static final int NAME_MIN_LENGTH = 2;
    static final int NAME_MAX_LENGTH = 120;

    private static final Pattern COUNTRY_PATTERN = Pattern.compile("[A-Z]{2}");

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private OrganizationType type;

    /** ISO 3166-1 alpha-2, uppercase; nullable (organizations may omit their country). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country", columnDefinition = "char(2)", length = 2)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrganizationStatus status;

    protected Organization() {
        // JPA
    }

    public Organization(UUID id, String name, OrganizationType type, String country) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        this.id = id;
        this.name = validName(name);
        this.country = validCountry(country);
        this.type = type;
        this.status = OrganizationStatus.ACTIVE;
    }

    public void rename(String newName) {
        this.name = validName(newName);
    }

    /** Assigns or clears ({@code null}) the ISO 3166-1 alpha-2 country code. */
    public void assignCountry(String newCountry) {
        this.country = validCountry(newCountry);
    }

    public boolean isActive() {
        return status == OrganizationStatus.ACTIVE;
    }

    private static String validName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            throw new IllegalArgumentException("Organization name is required");
        }
        String trimmed = candidate.trim();
        if (trimmed.length() < NAME_MIN_LENGTH || trimmed.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Organization name must be between "
                    + NAME_MIN_LENGTH + " and " + NAME_MAX_LENGTH + " characters");
        }
        return trimmed;
    }

    private static String validCountry(String candidate) {
        if (candidate == null) {
            return null;
        }
        if (!COUNTRY_PATTERN.matcher(candidate).matches()) {
            throw new IllegalArgumentException("Country must be an uppercase ISO 3166-1 alpha-2 code");
        }
        return candidate;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public OrganizationType getType() {
        return type;
    }

    public String getCountry() {
        return country;
    }

    public OrganizationStatus getStatus() {
        return status;
    }
}
