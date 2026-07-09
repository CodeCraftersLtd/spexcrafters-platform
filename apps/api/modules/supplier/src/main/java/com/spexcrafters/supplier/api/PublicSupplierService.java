package com.spexcrafters.supplier.api;

import com.spexcrafters.sharedkernel.i18n.SupportedLocale;
import com.spexcrafters.supplier.domain.OperationalStatus;
import com.spexcrafters.supplier.domain.Supplier;
import com.spexcrafters.supplier.domain.SupplierNotFoundException;
import com.spexcrafters.supplier.domain.SupplierProfile;
import com.spexcrafters.supplier.domain.SupplierProfileTranslation;
import com.spexcrafters.supplier.domain.TranslationStatus;
import com.spexcrafters.supplier.infrastructure.SupplierProfileRepository;
import com.spexcrafters.supplier.infrastructure.SupplierProfileTranslationRepository;
import com.spexcrafters.supplier.infrastructure.SupplierRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The public, unauthenticated supplier profile foundation. Only ACTIVE suppliers are exposed
 * (others 404). Content resolves by the ADR-020 fallback chain and the response labels the
 * language state so the public UI can show fallback/stale indicators. Class-E fields are
 * rendered as-is and never machine-translated.
 */
@Service
public class PublicSupplierService {

    private final SupplierRepository suppliers;
    private final SupplierProfileRepository profiles;
    private final SupplierProfileTranslationRepository translations;

    public PublicSupplierService(SupplierRepository suppliers, SupplierProfileRepository profiles,
            SupplierProfileTranslationRepository translations) {
        this.suppliers = suppliers;
        this.profiles = profiles;
        this.translations = translations;
    }

    @Transactional(readOnly = true)
    public PublicSupplierProfileDto profileFoundation(UUID supplierId, String requestedLocaleRaw) {
        Supplier supplier = suppliers.findById(supplierId)
                .filter(s -> s.getOperationalStatus() == OperationalStatus.ACTIVE)
                .orElseThrow(SupplierNotFoundException::new);
        SupplierProfile profile = profiles.findBySupplierId(supplierId)
                .orElseThrow(SupplierNotFoundException::new);
        String requested = SupportedLocale.normalizeOrFallback(requestedLocaleRaw);

        Resolution resolution = resolve(profile, supplier.getOriginalLocale(), requested);
        SupplierProfileTranslation display = resolution.translation();
        String tradingName = display != null && display.getTradingName() != null
                ? display.getTradingName() : profile.getTradingName();
        String companyDescription = display == null ? null : display.getCompanyDescription();
        boolean stale = display != null && display.isStale(profile.getSourceVersion());

        return new PublicSupplierProfileDto(
                supplierId,
                requested,
                resolution.displayLocale(),
                resolution.fallbackApplied(),
                stale,
                profile.getLegalName(),
                tradingName,
                companyDescription,
                profile.getCountryOfRegistration());
    }

    private Resolution resolve(SupplierProfile profile, String originalLocale, String requested) {
        // 1. requested-locale APPROVED (or original) translation.
        Optional<SupplierProfileTranslation> requestedRow =
                translations.findByProfileIdAndLocale(profile.getId(), requested);
        if (requestedRow.isPresent() && isDisplayable(requestedRow.get())) {
            return new Resolution(requestedRow.get(), requested, false);
        }
        // 2. supplier original language.
        if (!requested.equals(originalLocale)) {
            Optional<SupplierProfileTranslation> originalRow =
                    translations.findByProfileIdAndLocale(profile.getId(), originalLocale);
            if (originalRow.isPresent()) {
                return new Resolution(originalRow.get(), originalLocale, true);
            }
        }
        // 3. en APPROVED.
        if (!requested.equals(SupportedLocale.FALLBACK.code())
                && !originalLocale.equals(SupportedLocale.FALLBACK.code())) {
            Optional<SupplierProfileTranslation> enRow =
                    translations.findByProfileIdAndLocale(profile.getId(), SupportedLocale.FALLBACK.code());
            if (enRow.isPresent() && isDisplayable(enRow.get())) {
                return new Resolution(enRow.get(), SupportedLocale.FALLBACK.code(), true);
            }
        }
        // 4. untranslated.
        return new Resolution(null, requested, true);
    }

    private static boolean isDisplayable(SupplierProfileTranslation t) {
        return t.isOriginal() || t.getTranslationStatus() == TranslationStatus.APPROVED;
    }

    private record Resolution(SupplierProfileTranslation translation, String displayLocale,
            boolean fallbackApplied) {
    }
}
