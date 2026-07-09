package com.spexcrafters.taxonomy.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import com.spexcrafters.taxonomy.domain.Attribute;
import com.spexcrafters.taxonomy.domain.BrandApprovalStatus;
import com.spexcrafters.taxonomy.domain.Category;
import com.spexcrafters.taxonomy.infrastructure.AttributeRepository;
import com.spexcrafters.taxonomy.infrastructure.BrandRepository;
import com.spexcrafters.taxonomy.infrastructure.CategoryRepository;
import com.spexcrafters.taxonomy.infrastructure.CertificationRepository;
import com.spexcrafters.taxonomy.infrastructure.CountryRepository;
import com.spexcrafters.taxonomy.infrastructure.EnumerationValueRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The specification validation engine (ADR-025 §4): validates a map of raw attribute values
 * against a category's effective specification template. Enforces required (incl.
 * {@code conditional.requiredWhen}), data-type parse, numeric range, string length, regex,
 * enumeration membership, reference existence, and rejects unknown attributes. Violation
 * {@code code}s are stable slugs mapped to frontend i18n keys. Stores nothing.
 */
@Service
public class SpecificationValidator {

    private static final Pattern HEX_COLOR = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
    private static final Pattern RANGE = Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*(?:\\.\\.|-|to)\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");

    private final TemplateResolver templateResolver;
    private final CategoryRepository categories;
    private final AttributeRepository attributes;
    private final EnumerationValueRepository enumerationValues;
    private final CountryRepository countries;
    private final BrandRepository brands;
    private final CertificationRepository certifications;
    private final ObjectMapper objectMapper;

    public SpecificationValidator(TemplateResolver templateResolver, CategoryRepository categories,
            AttributeRepository attributes, EnumerationValueRepository enumerationValues,
            CountryRepository countries, BrandRepository brands, CertificationRepository certifications,
            ObjectMapper objectMapper) {
        this.templateResolver = templateResolver;
        this.categories = categories;
        this.attributes = attributes;
        this.enumerationValues = enumerationValues;
        this.countries = countries;
        this.brands = brands;
        this.certifications = certifications;
        this.objectMapper = objectMapper;
    }

    /** Resolves references against the live registry. Injected into the pure per-value core. */
    public interface References {
        boolean enumerationMember(UUID enumerationId, String valueCode);

        boolean countryActive(String code);

        boolean languageValid(String code);

        boolean brandResolvable(String code);

        boolean certificationResolvable(String code);
    }

    @Transactional(readOnly = true)
    public SpecificationValidationResult validate(String categoryCode, Map<String, String> values) {
        Category category = categories.findByCode(categoryCode)
                .orElseThrow(() -> ApiProblemException.validation(List.of(new ProblemFieldError(
                        "categoryCode", "unknown-category", "Unknown category code: " + categoryCode))));
        Map<String, String> input = values == null ? Map.of() : values;

        List<TemplateResolver.ResolvedSlot> slots = templateResolver.effectiveSlots(category);
        Map<String, TemplateResolver.ResolvedSlot> slotByCode = new java.util.LinkedHashMap<>();
        for (TemplateResolver.ResolvedSlot slot : slots) {
            slotByCode.put(slot.attributeCode(), slot);
        }
        References refs = liveReferences();
        List<SpecificationViolation> violations = new ArrayList<>();

        for (String key : input.keySet()) {
            if (!slotByCode.containsKey(key)) {
                violations.add(new SpecificationViolation(key, "unknown-attribute",
                        "Attribute is not part of this category's specification template."));
            }
        }

        for (TemplateResolver.ResolvedSlot slot : slots) {
            String raw = input.get(slot.attributeCode());
            boolean blank = raw == null || raw.isBlank();
            boolean required = slot.required() || conditionalRequires(slot.conditionalJson(), input);
            if (blank) {
                if (required) {
                    violations.add(new SpecificationViolation(slot.attributeCode(), "required",
                            "A value is required."));
                }
                continue;
            }
            Attribute attribute = attributes.findByCode(slot.attributeCode()).orElse(null);
            if (attribute != null) {
                violations.addAll(validateValue(attribute, raw.trim(), refs));
            }
        }
        return SpecificationValidationResult.of(violations);
    }

    /**
     * The pure per-value rule core: validates one raw value against one attribute definition,
     * returning zero or more violations (required/unknown-attribute are handled by the caller).
     */
    public static List<SpecificationViolation> validateValue(Attribute attr, String raw, References refs) {
        String code = attr.getCode();
        List<SpecificationViolation> out = new ArrayList<>();
        switch (attr.getDataType()) {
            case STRING, REFERENCE, FILE_REFERENCE -> validateString(attr, raw, out);
            case INTEGER -> parseAndRange(attr, raw, out, true);
            case DECIMAL, MEASUREMENT -> parseAndRange(attr, raw, out, false);
            case RANGE -> validateRange(attr, raw, out);
            case BOOLEAN -> {
                if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
                    out.add(violation(code, "not-a-boolean", "Expected true or false."));
                }
            }
            case DATE -> {
                try {
                    LocalDate.parse(raw);
                } catch (DateTimeParseException ex) {
                    out.add(violation(code, "not-a-date", "Expected an ISO-8601 date (yyyy-MM-dd)."));
                }
            }
            case COLOR -> {
                if (!HEX_COLOR.matcher(raw).matches()) {
                    out.add(violation(code, "not-a-color", "Expected a hex color like #RRGGBB."));
                }
            }
            case ENUMERATION, SINGLE_SELECT -> {
                if (!isMember(attr, raw, refs)) {
                    out.add(violation(code, "not-a-member", "Value is not an active member of the enumeration."));
                }
            }
            case MULTI_SELECT -> {
                for (String part : raw.split(",")) {
                    String token = part.trim();
                    if (!token.isEmpty() && !isMember(attr, token, refs)) {
                        out.add(violation(code, "not-a-member",
                                "Value '" + token + "' is not an active member of the enumeration."));
                    }
                }
            }
            case COUNTRY -> {
                if (!refs.countryActive(raw)) {
                    out.add(violation(code, "unknown-reference", "Unknown or inactive country code."));
                }
            }
            case LANGUAGE -> {
                if (!refs.languageValid(raw)) {
                    out.add(violation(code, "unknown-reference", "Unknown language code."));
                }
            }
            case BRAND -> {
                if (!refs.brandResolvable(raw)) {
                    out.add(violation(code, "unknown-reference", "Unknown or unapproved brand."));
                }
            }
            case CERTIFICATION -> {
                if (!refs.certificationResolvable(raw)) {
                    out.add(violation(code, "unknown-reference", "Unknown certification."));
                }
            }
            case JSON -> { /* opaque; presence already satisfied. */ }
            default -> { /* no additional rule */ }
        }
        return out;
    }

    private static boolean isMember(Attribute attr, String value, References refs) {
        return attr.getEnumerationId() != null && refs.enumerationMember(attr.getEnumerationId(), value);
    }

    private static void validateString(Attribute attr, String raw, List<SpecificationViolation> out) {
        String code = attr.getCode();
        if (attr.getMinLength() != null && raw.length() < attr.getMinLength()) {
            out.add(violation(code, "too-short", "Shorter than the minimum length of " + attr.getMinLength() + "."));
        }
        if (attr.getMaxLength() != null && raw.length() > attr.getMaxLength()) {
            out.add(violation(code, "too-long", "Longer than the maximum length of " + attr.getMaxLength() + "."));
        }
        if (attr.getRegexPattern() != null && !attr.getRegexPattern().isBlank()) {
            try {
                if (!Pattern.compile(attr.getRegexPattern()).matcher(raw).matches()) {
                    out.add(violation(code, "pattern", "Value does not match the required pattern."));
                }
            } catch (PatternSyntaxException ignored) {
                // A bad stored pattern is a configuration error, not a value error; skip.
            }
        }
    }

    private static void parseAndRange(Attribute attr, String raw, List<SpecificationViolation> out, boolean integer) {
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(raw);
        } catch (NumberFormatException ex) {
            out.add(violation(attr.getCode(), "not-a-number", "Expected a numeric value."));
            return;
        }
        if (integer && parsed.stripTrailingZeros().scale() > 0) {
            out.add(violation(attr.getCode(), "not-a-number", "Expected an integer value."));
            return;
        }
        checkRange(attr, parsed, out);
    }

    private static void validateRange(Attribute attr, String raw, List<SpecificationViolation> out) {
        var matcher = RANGE.matcher(raw);
        if (!matcher.matches()) {
            out.add(violation(attr.getCode(), "not-a-range", "Expected a range like '10-20'."));
            return;
        }
        BigDecimal min = new BigDecimal(matcher.group(1));
        BigDecimal max = new BigDecimal(matcher.group(2));
        if (min.compareTo(max) > 0) {
            out.add(violation(attr.getCode(), "not-a-range", "Range minimum exceeds its maximum."));
            return;
        }
        checkRange(attr, min, out);
        checkRange(attr, max, out);
    }

    private static void checkRange(Attribute attr, BigDecimal value, List<SpecificationViolation> out) {
        if (attr.getMinValue() != null && value.compareTo(attr.getMinValue()) < 0) {
            out.add(violation(attr.getCode(), "out-of-range", "Below the minimum of " + attr.getMinValue() + "."));
        }
        if (attr.getMaxValue() != null && value.compareTo(attr.getMaxValue()) > 0) {
            out.add(violation(attr.getCode(), "out-of-range", "Above the maximum of " + attr.getMaxValue() + "."));
        }
    }

    private static SpecificationViolation violation(String attributeCode, String code, String message) {
        return new SpecificationViolation(attributeCode, code, message);
    }

    @SuppressWarnings("unchecked")
    private boolean conditionalRequires(String conditionalJson, Map<String, String> values) {
        if (conditionalJson == null || conditionalJson.isBlank()) {
            return false;
        }
        try {
            return requiredByConditional(objectMapper.readValue(conditionalJson, Map.class), values);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Pure evaluation of a parsed {@code conditional} rule against the supplied values. Supports
     * {@code {"requiredWhen":{"attribute":"<CODE>","equals":"<VALUE>"}}} and an {@code "in"} list.
     */
    public static boolean requiredByConditional(Map<String, Object> rule, Map<String, String> values) {
        if (rule == null || !(rule.get("requiredWhen") instanceof Map<?, ?> when)) {
            return false;
        }
        Object attribute = when.get("attribute");
        if (attribute == null) {
            return false;
        }
        String actual = values.get(attribute.toString());
        Object equals = when.get("equals");
        if (equals != null) {
            return equals.toString().equals(actual);
        }
        Object in = when.get("in");
        return in instanceof List<?> options && actual != null
                && options.stream().map(Object::toString).anyMatch(actual::equals);
    }

    private References liveReferences() {
        return new References() {
            @Override
            public boolean enumerationMember(UUID enumerationId, String valueCode) {
                return enumerationValues.findByEnumerationIdAndCode(enumerationId, valueCode)
                        .filter(v -> v.isActive() && !v.isDeprecated()).isPresent();
            }

            @Override
            public boolean countryActive(String code) {
                return countries.findById(code).filter(c -> c.isActive()).isPresent();
            }

            @Override
            public boolean languageValid(String code) {
                return SupportedLocale.parse(code).isPresent();
            }

            @Override
            public boolean brandResolvable(String code) {
                return brands.findByCode(code)
                        .filter(b -> b.isActive() && b.getApprovalStatus() == BrandApprovalStatus.APPROVED)
                        .isPresent();
            }

            @Override
            public boolean certificationResolvable(String code) {
                return certifications.findByCode(code).filter(c -> c.isActive()).isPresent();
            }
        };
    }
}
