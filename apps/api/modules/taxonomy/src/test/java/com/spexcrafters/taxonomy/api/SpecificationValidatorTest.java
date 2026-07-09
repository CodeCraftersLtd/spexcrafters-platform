package com.spexcrafters.taxonomy.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.spexcrafters.taxonomy.api.SpecificationValidator.References;
import com.spexcrafters.taxonomy.domain.Attribute;
import com.spexcrafters.taxonomy.domain.AttributeDataType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Exhaustive pure tests of the per-value validation core and the conditional-required rule. */
class SpecificationValidatorTest {

    private static final UUID ENUM_ID = UUID.randomUUID();

    private static final References REFS = new References() {
        @Override
        public boolean enumerationMember(UUID enumerationId, String valueCode) {
            return Set.of("ROUND", "SQUARE").contains(valueCode);
        }

        @Override
        public boolean countryActive(String code) {
            return code.equals("US");
        }

        @Override
        public boolean languageValid(String code) {
            return code.equals("en") || code.equals("zh-CN");
        }

        @Override
        public boolean brandResolvable(String code) {
            return code.equals("ESSILOR");
        }

        @Override
        public boolean certificationResolvable(String code) {
            return code.equals("CE");
        }
    };

    private static Attribute attr(String code, AttributeDataType type) {
        return new Attribute(UUID.randomUUID(), code, type, 0);
    }

    private static List<String> codes(Attribute attr, String raw) {
        return SpecificationValidator.validateValue(attr, raw, REFS).stream()
                .map(SpecificationViolation::code).toList();
    }

    @Test
    void integerParsingAndRange() {
        Attribute a = attr("POWER_WATT", AttributeDataType.INTEGER);
        a.setBounds(BigDecimal.ZERO, new BigDecimal("100"), null, null, null);
        assertThat(codes(a, "50")).isEmpty();
        assertThat(codes(a, "12.5")).containsExactly("not-a-number");
        assertThat(codes(a, "abc")).containsExactly("not-a-number");
        assertThat(codes(a, "-1")).containsExactly("out-of-range");
        assertThat(codes(a, "200")).containsExactly("out-of-range");
    }

    @Test
    void measurementAndDecimalRange() {
        Attribute eye = attr("EYE_SIZE", AttributeDataType.MEASUREMENT);
        eye.setBounds(new BigDecimal("30"), new BigDecimal("70"), null, null, null);
        assertThat(codes(eye, "48")).isEmpty();
        assertThat(codes(eye, "80")).containsExactly("out-of-range");

        Attribute curve = attr("BASE_CURVE", AttributeDataType.DECIMAL);
        curve.setBounds(BigDecimal.ZERO, new BigDecimal("10"), null, null, null);
        assertThat(codes(curve, "5.5")).isEmpty();
        assertThat(codes(curve, "nope")).containsExactly("not-a-number");
    }

    @Test
    void stringLengthAndPattern() {
        Attribute a = attr("SKU", AttributeDataType.STRING);
        a.setBounds(null, null, 2, 3, "[A-Z]+");
        assertThat(codes(a, "AB")).isEmpty();
        assertThat(codes(a, "A")).containsExactly("too-short");
        assertThat(codes(a, "ABCD")).containsExactly("too-long");
        assertThat(codes(a, "ab")).containsExactly("pattern");
    }

    @Test
    void booleanDateColor() {
        assertThat(codes(attr("B", AttributeDataType.BOOLEAN), "true")).isEmpty();
        assertThat(codes(attr("B", AttributeDataType.BOOLEAN), "yes")).containsExactly("not-a-boolean");
        assertThat(codes(attr("D", AttributeDataType.DATE), "2020-01-01")).isEmpty();
        assertThat(codes(attr("D", AttributeDataType.DATE), "2020-13-40")).containsExactly("not-a-date");
        assertThat(codes(attr("C", AttributeDataType.COLOR), "#fff")).isEmpty();
        assertThat(codes(attr("C", AttributeDataType.COLOR), "#aabbcc")).isEmpty();
        assertThat(codes(attr("C", AttributeDataType.COLOR), "red")).containsExactly("not-a-color");
    }

    @Test
    void rangeParsing() {
        Attribute a = attr("SPAN", AttributeDataType.RANGE);
        a.setBounds(new BigDecimal("30"), new BigDecimal("70"), null, null, null);
        assertThat(codes(a, "40-50")).isEmpty();
        assertThat(codes(a, "40..50")).isEmpty();
        assertThat(codes(a, "abc")).containsExactly("not-a-range");
        assertThat(codes(a, "50-40")).containsExactly("not-a-range");
        assertThat(codes(a, "10-50")).containsExactly("out-of-range");
    }

    @Test
    void enumerationMembership() {
        Attribute single = attr("FRAME_SHAPE", AttributeDataType.ENUMERATION);
        single.setEnumerationId(ENUM_ID);
        assertThat(codes(single, "ROUND")).isEmpty();
        assertThat(codes(single, "TRIANGLE")).containsExactly("not-a-member");

        Attribute multi = attr("TAGS", AttributeDataType.MULTI_SELECT);
        multi.setEnumerationId(ENUM_ID);
        assertThat(codes(multi, "ROUND,SQUARE")).isEmpty();
        assertThat(codes(multi, "ROUND,NOPE")).containsExactly("not-a-member");
    }

    @Test
    void referenceExistence() {
        assertThat(codes(attr("ORIGIN", AttributeDataType.COUNTRY), "US")).isEmpty();
        assertThat(codes(attr("ORIGIN", AttributeDataType.COUNTRY), "ZZ")).containsExactly("unknown-reference");
        assertThat(codes(attr("LANG", AttributeDataType.LANGUAGE), "en")).isEmpty();
        assertThat(codes(attr("LANG", AttributeDataType.LANGUAGE), "xx")).containsExactly("unknown-reference");
        assertThat(codes(attr("MAKER", AttributeDataType.BRAND), "ESSILOR")).isEmpty();
        assertThat(codes(attr("MAKER", AttributeDataType.BRAND), "NOPE")).containsExactly("unknown-reference");
        assertThat(codes(attr("CERT", AttributeDataType.CERTIFICATION), "CE")).isEmpty();
        assertThat(codes(attr("CERT", AttributeDataType.CERTIFICATION), "NOPE"))
                .containsExactly("unknown-reference");
    }

    @Test
    void conditionalRequiredWhenEquals() {
        Map<String, Object> rule = Map.of("requiredWhen", Map.of("attribute", "LENS_DESIGN", "equals",
                "PHOTOCHROMIC"));
        assertThat(SpecificationValidator.requiredByConditional(rule,
                Map.of("LENS_DESIGN", "PHOTOCHROMIC"))).isTrue();
        assertThat(SpecificationValidator.requiredByConditional(rule,
                Map.of("LENS_DESIGN", "CLEAR"))).isFalse();
        assertThat(SpecificationValidator.requiredByConditional(rule, Map.of())).isFalse();
    }

    @Test
    void conditionalRequiredWhenIn() {
        Map<String, Object> rule = Map.of("requiredWhen", Map.of("attribute", "TINT", "in",
                List.of("BROWN", "GREY")));
        assertThat(SpecificationValidator.requiredByConditional(rule, Map.of("TINT", "GREY"))).isTrue();
        assertThat(SpecificationValidator.requiredByConditional(rule, Map.of("TINT", "CLEAR"))).isFalse();
    }
}
