package com.spexcrafters.taxonomy.web;

import com.spexcrafters.taxonomy.api.CategoryService.TemplateAttributeInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/** Body of {@code putSpecificationTemplate}: the full replacement set of template attributes. */
public record PutSpecificationTemplateRequest(
        @NotBlank @Size(max = 64) String code,
        @NotNull List<TemplateAttribute> attributes) {

    /** One template slot in the replacement set. */
    public record TemplateAttribute(
            @NotBlank String attributeCode,
            boolean required,
            Map<String, Object> conditional,
            String defaultValue,
            Integer sortOrder) {
    }

    public List<TemplateAttributeInput> toInputs() {
        return attributes.stream()
                .map(a -> new TemplateAttributeInput(a.attributeCode(), a.required(), a.conditional(),
                        a.defaultValue(), a.sortOrder()))
                .toList();
    }
}
