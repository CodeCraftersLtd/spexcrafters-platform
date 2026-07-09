package com.spexcrafters.supplier.web;

import com.spexcrafters.supplier.api.PublicSupplierProfileDto;
import com.spexcrafters.supplier.api.PublicSupplierService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, unauthenticated supplier profile foundation with locale fallback. */
@RestController
@RequestMapping("/api/v1/public/suppliers")
public class PublicSupplierController {

    private final PublicSupplierService publicSupplierService;

    public PublicSupplierController(PublicSupplierService publicSupplierService) {
        this.publicSupplierService = publicSupplierService;
    }

    /** operationId: getPublicSupplierProfileFoundation */
    @GetMapping("/{supplierId}/profile-foundation")
    public PublicSupplierProfileDto getPublicSupplierProfileFoundation(@PathVariable UUID supplierId,
            @RequestParam(required = false) String locale) {
        return publicSupplierService.profileFoundation(supplierId, locale);
    }
}
