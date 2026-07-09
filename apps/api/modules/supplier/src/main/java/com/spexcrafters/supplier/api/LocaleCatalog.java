package com.spexcrafters.supplier.api;

import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Serves the supported-locale registry for {@code GET /locales}. Backed by the deterministic
 * {@link SupportedLocale} (the {@code reference.supported_locale} table mirrors it for FK
 * integrity). Direction and fallback come from the registry; labels are a UI concern.
 */
@Service
public class LocaleCatalog {

    public List<LocaleDto> list() {
        return java.util.Arrays.stream(SupportedLocale.values())
                .map(locale -> new LocaleDto(
                        locale.code(),
                        locale.isRightToLeft() ? "rtl" : "ltr",
                        locale == SupportedLocale.FALLBACK ? null : SupportedLocale.FALLBACK.code()))
                .toList();
    }
}
