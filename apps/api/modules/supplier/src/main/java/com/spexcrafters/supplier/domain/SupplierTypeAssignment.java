package com.spexcrafters.supplier.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A supplier's declared business type, referenced by a stable taxonomy code (class C). Labels
 * are translated in UI resources; domain logic never depends on them. Unique per
 * {@code (supplier_id, type_code)}.
 */
@Entity
@Table(name = "supplier_type_assignment", schema = "supplier")
public class SupplierTypeAssignment extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "type_code", nullable = false, length = 64)
    private String typeCode;

    protected SupplierTypeAssignment() {
        // JPA
    }

    public SupplierTypeAssignment(UUID id, UUID supplierId, String typeCode) {
        this.id = id;
        this.supplierId = supplierId;
        this.typeCode = typeCode;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public String getTypeCode() {
        return typeCode;
    }
}
