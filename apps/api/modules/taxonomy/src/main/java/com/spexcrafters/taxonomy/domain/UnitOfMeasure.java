package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A de-duplicated unit of measure (domain-model §5). The stable id is the symbol {@code code}
 * (natural PK). Conversion metadata targets a family base unit; COUNT-family units are not
 * interconvertible ({@code factorToBase} null).
 */
@Entity
@Table(name = "unit_of_measure", schema = "taxonomy")
public class UnitOfMeasure extends AuditedEntity {

    @Id
    @Column(name = "code", length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "family", nullable = false, length = 32)
    private UnitFamily family;

    @Column(name = "base_unit_code", length = 32)
    private String baseUnitCode;

    @Column(name = "factor_to_base")
    private BigDecimal factorToBase;

    @Column(name = "offset_to_base", nullable = false)
    private BigDecimal offsetToBase;

    @Column(name = "display_format", length = 64)
    private String displayFormat;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected UnitOfMeasure() {
        // JPA
    }

    public UnitOfMeasure(String code, UnitFamily family, String baseUnitCode, BigDecimal factorToBase,
            BigDecimal offsetToBase, String displayFormat, int sortOrder) {
        this.code = code;
        this.family = family;
        this.baseUnitCode = baseUnitCode;
        this.factorToBase = factorToBase;
        this.offsetToBase = offsetToBase == null ? BigDecimal.ZERO : offsetToBase;
        this.displayFormat = displayFormat;
        this.active = true;
        this.sortOrder = sortOrder;
    }

    public String getCode() {
        return code;
    }

    public UnitFamily getFamily() {
        return family;
    }

    public String getBaseUnitCode() {
        return baseUnitCode;
    }

    public BigDecimal getFactorToBase() {
        return factorToBase;
    }

    public BigDecimal getOffsetToBase() {
        return offsetToBase;
    }

    public String getDisplayFormat() {
        return displayFormat;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
