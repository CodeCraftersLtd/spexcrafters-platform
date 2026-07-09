package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One attribute slot on a {@link SpecificationTemplate} (domain-model §9). Carries the
 * per-context {@code required} flag, an optional {@code conditional} jsonb rule
 * ({@code {"requiredWhen":{"attribute":"<CODE>","equals":"<VALUE_CODE>"}}}), a default value and
 * sort order.
 */
@Entity
@Table(name = "specification_template_attribute", schema = "taxonomy")
public class SpecificationTemplateAttribute extends AuditedEntity {

    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "attribute_id", nullable = false)
    private UUID attributeId;

    @Column(name = "required", nullable = false)
    private boolean required;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditional")
    private String conditional;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected SpecificationTemplateAttribute() {
        // JPA
    }

    public SpecificationTemplateAttribute(UUID id, UUID templateId, UUID attributeId, boolean required,
            String conditional, String defaultValue, int sortOrder) {
        this.id = id;
        this.templateId = templateId;
        this.attributeId = attributeId;
        this.required = required;
        this.conditional = conditional;
        this.defaultValue = defaultValue;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public UUID getAttributeId() {
        return attributeId;
    }

    public boolean isRequired() {
        return required;
    }

    public String getConditional() {
        return conditional;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
