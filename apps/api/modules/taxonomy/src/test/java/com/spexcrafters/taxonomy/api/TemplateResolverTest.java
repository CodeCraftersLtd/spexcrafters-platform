package com.spexcrafters.taxonomy.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.spexcrafters.taxonomy.api.TemplateResolver.CategoryLayer;
import com.spexcrafters.taxonomy.api.TemplateResolver.ResolvedSlot;
import com.spexcrafters.taxonomy.api.TemplateResolver.SlotDef;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure tests of effective-template inheritance resolution (nearest-ancestor wins). */
class TemplateResolverTest {

    @Test
    void ownAttributesComeFirstAndAreNotInherited() {
        List<ResolvedSlot> slots = TemplateResolver.merge(List.of(
                new CategoryLayer("PRESCRIPTION_LENS", true, List.of(
                        new SlotDef("LENS_DESIGN", true, null, null, 1))),
                new CategoryLayer("LENS", false, List.of(
                        new SlotDef("LENS_MATERIAL", true, null, null, 1)))));

        assertThat(slots).extracting(ResolvedSlot::attributeCode)
                .containsExactly("LENS_DESIGN", "LENS_MATERIAL");
        assertThat(slots.get(0).inherited()).isFalse();
        assertThat(slots.get(0).sourceCategoryCode()).isEqualTo("PRESCRIPTION_LENS");
        assertThat(slots.get(1).inherited()).isTrue();
        assertThat(slots.get(1).sourceCategoryCode()).isEqualTo("LENS");
    }

    @Test
    void nearestAncestorWinsOnAttributeCollision() {
        List<ResolvedSlot> slots = TemplateResolver.merge(List.of(
                new CategoryLayer("CHILD", true, List.of(
                        new SlotDef("LENS_INDEX", false, null, "1.5", 1))),
                new CategoryLayer("PARENT", false, List.of(
                        new SlotDef("LENS_INDEX", true, null, "1.6", 1)))));

        assertThat(slots).hasSize(1);
        ResolvedSlot winner = slots.get(0);
        assertThat(winner.inherited()).isFalse();
        assertThat(winner.required()).isFalse();
        assertThat(winner.defaultValue()).isEqualTo("1.5");
        assertThat(winner.sourceCategoryCode()).isEqualTo("CHILD");
    }

    @Test
    void emptyChainYieldsNoSlots() {
        assertThat(TemplateResolver.merge(List.of(
                new CategoryLayer("LEAF", true, List.of())))).isEmpty();
    }
}
