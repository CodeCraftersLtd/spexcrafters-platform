package com.spexcrafters.taxonomy.web;

import jakarta.validation.constraints.NotNull;

/** Body of {@code setCategoryActivation}. */
public record ActivationRequest(@NotNull Boolean active) {
}
