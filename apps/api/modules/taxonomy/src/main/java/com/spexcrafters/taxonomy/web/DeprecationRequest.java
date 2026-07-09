package com.spexcrafters.taxonomy.web;

import jakarta.validation.constraints.NotNull;

/** Body of {@code setAttributeDeprecation}. */
public record DeprecationRequest(@NotNull Boolean deprecated) {
}
