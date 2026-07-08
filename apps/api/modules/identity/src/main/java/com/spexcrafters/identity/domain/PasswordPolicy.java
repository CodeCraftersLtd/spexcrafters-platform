package com.spexcrafters.identity.domain;

import com.spexcrafters.sharedkernel.problem.ProblemFieldError;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Platform password policy, applied server-side on registration in addition to the
 * declarative length constraint on the request record. Error {@code code}s are stable
 * i18n keys for the frontend.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    /**
     * Validates a candidate password. Returns an empty list when the password is acceptable.
     *
     * @param password the raw candidate password
     * @param email    the (normalised) account email; the local part must not appear in the password
     */
    public static List<ProblemFieldError> validate(String password, String email) {
        List<ProblemFieldError> errors = new ArrayList<>();
        if (password == null || password.length() < MIN_LENGTH) {
            errors.add(error("password.too_short",
                    "Password must be at least " + MIN_LENGTH + " characters long."));
            return errors;
        }
        if (password.length() > MAX_LENGTH) {
            errors.add(error("password.too_long",
                    "Password must be at most " + MAX_LENGTH + " characters long."));
            return errors;
        }
        if (password.chars().noneMatch(Character::isLetter)) {
            errors.add(error("password.needs_letter", "Password must contain at least one letter."));
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            errors.add(error("password.needs_digit", "Password must contain at least one digit."));
        }
        String localPart = emailLocalPart(email);
        if (localPart.length() >= 3
                && password.toLowerCase(Locale.ROOT).contains(localPart.toLowerCase(Locale.ROOT))) {
            errors.add(error("password.contains_email", "Password must not contain your email address."));
        }
        return errors;
    }

    private static String emailLocalPart(String email) {
        if (email == null) {
            return "";
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private static ProblemFieldError error(String code, String message) {
        return new ProblemFieldError("password", code, message);
    }
}
