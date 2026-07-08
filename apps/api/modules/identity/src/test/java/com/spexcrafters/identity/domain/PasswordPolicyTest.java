package com.spexcrafters.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import java.util.List;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private static final String EMAIL = "jane.doe@example.com";

    @Test
    void acceptsACompliantPassword() {
        assertThat(PasswordPolicy.validate("correct-horse-battery-7", EMAIL)).isEmpty();
    }

    @Test
    void rejectsPasswordsShorterThanTwelveCharacters() {
        List<ProblemFieldError> errors = PasswordPolicy.validate("short7a", EMAIL);

        assertThat(errors).extracting(ProblemFieldError::code).containsExactly("password.too_short");
        assertThat(errors).allSatisfy(error -> assertThat(error.field()).isEqualTo("password"));
    }

    @Test
    void rejectsPasswordsLongerThanTheMaximum() {
        String tooLong = "a1".repeat(65); // 130 characters

        List<ProblemFieldError> errors = PasswordPolicy.validate(tooLong, EMAIL);

        assertThat(errors).extracting(ProblemFieldError::code).containsExactly("password.too_long");
    }

    @Test
    void requiresAtLeastOneLetter() {
        List<ProblemFieldError> errors = PasswordPolicy.validate("123456789012", EMAIL);

        assertThat(errors).extracting(ProblemFieldError::code).contains("password.needs_letter");
    }

    @Test
    void requiresAtLeastOneDigit() {
        List<ProblemFieldError> errors = PasswordPolicy.validate("onlylettershere", EMAIL);

        assertThat(errors).extracting(ProblemFieldError::code).contains("password.needs_digit");
    }

    @Test
    void rejectsPasswordsContainingTheEmailLocalPart() {
        List<ProblemFieldError> errors = PasswordPolicy.validate("xxJane.Doe123zz", EMAIL);

        assertThat(errors).extracting(ProblemFieldError::code).contains("password.contains_email");
    }

    @Test
    void collectsMultipleViolationsAtOnce() {
        // 12+ characters, no digit, and contains the email local part.
        List<ProblemFieldError> errors = PasswordPolicy.validate("xxjane.doexxyy", EMAIL);

        assertThat(errors).extracting(ProblemFieldError::code)
                .containsExactlyInAnyOrder("password.needs_digit", "password.contains_email");
    }

    @Test
    void rejectsNullPasswordAsTooShort() {
        List<ProblemFieldError> errors = PasswordPolicy.validate(null, EMAIL);

        assertThat(errors).extracting(ProblemFieldError::code).containsExactly("password.too_short");
    }
}
