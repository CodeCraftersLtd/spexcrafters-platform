package com.spexcrafters.identity.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The {@code Locale} enum of the OpenAPI contract. Java identifiers cannot contain a
 * hyphen ({@code zh-Hans}), so the wire code is carried explicitly and (de)serialised
 * via {@link JsonValue}/{@link JsonCreator}. An unsupported code fails deserialisation,
 * which the global handler surfaces as a 422 validation problem.
 */
public enum UserLocale {

    EN("en"),
    ZH_HANS("zh-Hans"),
    FR("fr"),
    DE("de");

    private final String code;

    UserLocale(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static UserLocale fromCode(String code) {
        for (UserLocale locale : values()) {
            if (locale.code.equals(code)) {
                return locale;
            }
        }
        throw new IllegalArgumentException("Unsupported locale: " + code);
    }
}
