package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 409 — the request conflicts with existing state: a duplicate stable {@code code}, a duplicate
 * localized slug, or an optimistic-lock version mismatch.
 */
public class TaxonomyConflictException extends ApiProblemException {

    public TaxonomyConflictException(String detail) {
        super(HttpStatus.CONFLICT, TaxonomyProblemTypes.CONFLICT, "Conflict", detail);
    }
}
