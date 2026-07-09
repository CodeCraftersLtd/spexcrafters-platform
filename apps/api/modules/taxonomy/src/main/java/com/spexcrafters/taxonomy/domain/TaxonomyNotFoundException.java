package com.spexcrafters.taxonomy.domain;

import com.spexcrafters.sharedkernel.problem.ApiProblemException;
import org.springframework.http.HttpStatus;

/**
 * 404 — the requested taxonomy registry entry does not exist (or, for admin lookups by id, is
 * concealed from a non-staff caller per platform policy — but authorization is checked first).
 */
public class TaxonomyNotFoundException extends ApiProblemException {

    public TaxonomyNotFoundException() {
        super(HttpStatus.NOT_FOUND, TaxonomyProblemTypes.NOT_FOUND, "Not found",
                "The requested resource does not exist.");
    }
}
