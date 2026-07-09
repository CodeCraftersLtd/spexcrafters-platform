package com.spexcrafters.taxonomy.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code createEnumeration}. */
public record CreateEnumerationRequest(@NotBlank @Size(max = 64) String code) {
}
