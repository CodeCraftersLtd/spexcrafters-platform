package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A master specification-field definition (domain-model §4). Products (future) reference
 * attributes by id; they never define specs inline. Cross-field integrity (unit only with
 * MEASUREMENT/RANGE; enumeration only with ENUMERATION/SINGLE_SELECT/MULTI_SELECT) is enforced
 * in the service on write, not in SQL.
 */
@Entity
@Table(name = "attribute", schema = "taxonomy")
public class Attribute extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 32)
    private AttributeDataType dataType;

    @Column(name = "unit_code", length = 32)
    private String unitCode;

    @Column(name = "enumeration_id")
    private UUID enumerationId;

    @Column(name = "min_value")
    private BigDecimal minValue;

    @Column(name = "max_value")
    private BigDecimal maxValue;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "regex_pattern", length = 500)
    private String regexPattern;

    @Column(name = "searchable", nullable = false)
    private boolean searchable;

    @Column(name = "filterable", nullable = false)
    private boolean filterable;

    @Column(name = "sortable", nullable = false)
    private boolean sortable;

    @Column(name = "comparable", nullable = false)
    private boolean comparable;

    @Column(name = "facetable", nullable = false)
    private boolean facetable;

    @Column(name = "seo", nullable = false)
    private boolean seo;

    @Column(name = "visible", nullable = false)
    private boolean visible;

    @Column(name = "deprecated", nullable = false)
    private boolean deprecated;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_metadata")
    private String aiMetadata;

    @Column(name = "source_version", nullable = false)
    private int sourceVersion;

    protected Attribute() {
        // JPA
    }

    public Attribute(UUID id, String code, AttributeDataType dataType, int sortOrder) {
        this.id = id;
        this.code = code;
        this.dataType = dataType;
        this.visible = true;
        this.sortOrder = sortOrder;
        this.sourceVersion = 1;
    }

    public int bumpSourceVersion() {
        return ++sourceVersion;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public void setEnumerationId(UUID enumerationId) {
        this.enumerationId = enumerationId;
    }

    public void setBounds(BigDecimal minValue, BigDecimal maxValue, Integer minLength, Integer maxLength,
            String regexPattern) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.regexPattern = regexPattern;
    }

    public void setCapabilityFlags(boolean searchable, boolean filterable, boolean sortable, boolean comparable,
            boolean facetable, boolean seo, boolean visible) {
        this.searchable = searchable;
        this.filterable = filterable;
        this.sortable = sortable;
        this.comparable = comparable;
        this.facetable = facetable;
        this.seo = seo;
        this.visible = visible;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public AttributeDataType getDataType() {
        return dataType;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public UUID getEnumerationId() {
        return enumerationId;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public boolean isSortable() {
        return sortable;
    }

    public boolean isComparable() {
        return comparable;
    }

    public boolean isFacetable() {
        return facetable;
    }

    public boolean isSeo() {
        return seo;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getAiMetadata() {
        return aiMetadata;
    }

    public int getSourceVersion() {
        return sourceVersion;
    }
}
