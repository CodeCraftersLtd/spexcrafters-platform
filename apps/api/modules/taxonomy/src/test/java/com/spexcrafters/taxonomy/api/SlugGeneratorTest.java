package com.spexcrafters.taxonomy.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure tests of ASCII SEO slug normalization and collision de-duplication. */
class SlugGeneratorTest {

    @Test
    void normalizesToAsciiLowercaseHyphen() {
        assertThat(SlugGenerator.slugify("Prescription Lens")).isEqualTo("prescription-lens");
        assertThat(SlugGenerator.slugify("  Blue  Light  Filter!! ")).isEqualTo("blue-light-filter");
        assertThat(SlugGenerator.slugify("Café Crème")).isEqualTo("cafe-creme");
        assertThat(SlugGenerator.slugify("TR-90 / Nylon")).isEqualTo("tr-90-nylon");
    }

    @Test
    void capsLengthAtOneSixty() {
        String slug = SlugGenerator.slugify("a".repeat(300));
        assertThat(slug).hasSize(SlugGenerator.MAX_LENGTH);
    }

    @Test
    void deDupesWithNumericSuffixOnCollision() {
        Set<String> taken = new HashSet<>(Set.of("frame", "frame-2"));
        assertThat(SlugGenerator.uniqueSlug("Frame", taken::contains)).isEqualTo("frame-3");
        assertThat(SlugGenerator.uniqueSlug("Titanium", taken::contains)).isEqualTo("titanium");
    }

    @Test
    void fallsBackToItemForPunctuationOnlyInput() {
        assertThat(SlugGenerator.uniqueSlug("的的的", s -> false)).isEqualTo("item");
    }
}
