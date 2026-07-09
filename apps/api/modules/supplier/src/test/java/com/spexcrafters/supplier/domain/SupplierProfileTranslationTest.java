package com.spexcrafters.supplier.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupplierProfileTranslationTest {

    private SupplierProfileTranslation nonOriginal(int sourceVersion) {
        return new SupplierProfileTranslation(UUID.randomUUID(), UUID.randomUUID(), "fr", "en",
                sourceVersion, TranslationSource.HUMAN, false, UUID.randomUUID());
    }

    @Test
    void originalRowIsApprovedAndNeverStale() {
        SupplierProfileTranslation original = new SupplierProfileTranslation(UUID.randomUUID(),
                UUID.randomUUID(), "en", "en", 1, TranslationSource.HUMAN, true, UUID.randomUUID());
        assertThat(original.isOriginal()).isTrue();
        assertThat(original.getTranslationStatus()).isEqualTo(TranslationStatus.APPROVED);
        assertThat(original.isStale(5)).isFalse();
    }

    @Test
    void translationGoesStaleWhenSourceVersionAdvances() {
        SupplierProfileTranslation translation = nonOriginal(1);
        assertThat(translation.isStale(1)).isFalse();
        assertThat(translation.isStale(2)).isTrue();
    }

    @Test
    void approvingSetsApprovedStatusAndReviewer() {
        SupplierProfileTranslation translation = nonOriginal(1);
        UUID reviewer = UUID.randomUUID();
        translation.approve(reviewer, Instant.now());
        assertThat(translation.getTranslationStatus()).isEqualTo(TranslationStatus.APPROVED);
        assertThat(translation.getApprovedBy()).isEqualTo(reviewer);
    }

    @Test
    void editingNonOriginalResetsApprovalAndMachineSourceIsLabelled() {
        SupplierProfileTranslation translation = nonOriginal(2);
        translation.approve(UUID.randomUUID(), Instant.now());
        translation.applyContent("Nom", "desc", null, null, null, null, null, null, 2,
                TranslationSource.MACHINE, UUID.randomUUID());
        assertThat(translation.getTranslationStatus()).isEqualTo(TranslationStatus.MACHINE_TRANSLATED);
        assertThat(translation.getApprovedAt()).isNull();
    }

    @Test
    void editingOriginalPreservesApprovedStatus() {
        SupplierProfileTranslation original = new SupplierProfileTranslation(UUID.randomUUID(),
                UUID.randomUUID(), "en", "en", 1, TranslationSource.HUMAN, true, UUID.randomUUID());
        original.applyContent("Trading", "About us", null, null, null, null, null, null, 2,
                TranslationSource.HUMAN, UUID.randomUUID());
        assertThat(original.getTranslationStatus()).isEqualTo(TranslationStatus.APPROVED);
        assertThat(original.getCompanyDescription()).isEqualTo("About us");
    }
}
