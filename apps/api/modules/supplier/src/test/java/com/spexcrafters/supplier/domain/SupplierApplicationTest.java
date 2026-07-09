package com.spexcrafters.supplier.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupplierApplicationTest {

    private SupplierApplication newApplication() {
        return new SupplierApplication(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void startsInDraft() {
        assertThat(newApplication().getStatus()).isEqualTo(ApplicationStatus.DRAFT);
    }

    @Test
    void fullHappyPathReachesApproved() {
        SupplierApplication application = newApplication();
        application.submit(Instant.now());
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        application.claimForReview(UUID.randomUUID());
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        application.approve(UUID.randomUUID(), Instant.now());
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(application.getStatus().isTerminal()).isTrue();
    }

    @Test
    void changesRequestedResubmitCycle() {
        SupplierApplication application = newApplication();
        application.submit(Instant.now());
        application.claimForReview(UUID.randomUUID());
        application.requestChanges();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CHANGES_REQUESTED);
        assertThat(application.isEditableBySupplier()).isTrue();
        application.resubmit(Instant.now());
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.RESUBMITTED);
        application.claimForReview(UUID.randomUUID());
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void cannotApproveFromDraft() {
        SupplierApplication application = newApplication();
        assertThatThrownBy(() -> application.approve(UUID.randomUUID(), Instant.now()))
                .isInstanceOf(InvalidApplicationTransitionException.class);
    }

    @Test
    void cannotSubmitTwice() {
        SupplierApplication application = newApplication();
        application.submit(Instant.now());
        assertThatThrownBy(() -> application.submit(Instant.now()))
                .isInstanceOf(InvalidApplicationTransitionException.class);
    }

    @Test
    void cannotTransitionOutOfTerminalState() {
        SupplierApplication application = newApplication();
        application.submit(Instant.now());
        application.claimForReview(UUID.randomUUID());
        application.reject(UUID.randomUUID(), Instant.now());
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThatThrownBy(application::withdraw).isInstanceOf(InvalidApplicationTransitionException.class);
    }

    @Test
    void withdrawAllowedFromPreDecisionStates() {
        SupplierApplication application = newApplication();
        application.submit(Instant.now());
        application.withdraw();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(application.getStatus().isTerminal()).isTrue();
    }
}
