package com.spexcrafters.supplier.web;

import com.spexcrafters.supplier.api.LocaleCatalog;
import com.spexcrafters.supplier.api.LocaleDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public supported-locale registry. */
@RestController
@RequestMapping("/api/v1/locales")
public class LocaleController {

    private final LocaleCatalog localeCatalog;

    public LocaleController(LocaleCatalog localeCatalog) {
        this.localeCatalog = localeCatalog;
    }

    /** operationId: listLocales */
    @GetMapping
    public List<LocaleDto> listLocales() {
        return localeCatalog.list();
    }
}
