# Localization Content Classification (Phase 7)

**Status:** Binding (§5 of the Phase-7 brief). Determines where each kind of content lives and how it is translated.

| Class | What | Storage | Translation mechanism |
|---|---|---|---|
| **A · Platform UI** | nav, buttons, form labels, validation, empty/error states, status labels, a11y labels, dashboard text | version-controlled `apps/web/messages/{locale}/{namespace}.json` (ICU) | UI resources; `en` canonical source; human-reviewed before launch; MT dev-only, tracked |
| **B · Platform editorial** | homepage, about, verification explainers, guides, insights, events, SEO landing, future CMS | database (future `content` module) with localized content records | ADR-020 translation lifecycle (not built in Phase 7 beyond the model) |
| **C · Structured taxonomy** | supplier types, capabilities, verification scopes, evidence types, countries, units, future optical attributes | **stable language-independent codes** in DB + seed | display **labels translated in UI resources** (namespace `taxonomy`); domain logic never depends on labels |
| **D · Supplier-entered** | trading name (localized), company/factory/production/OEM/ODM/private-label/facility/QC/export descriptions, marketing | `supplier_profile` + `supplier_profile_translation` (ADR-020) | original language preserved; separate translation rows with lifecycle + stale detection |
| **E · Non-translatable** | legal company name, registration number, certificate/model numbers, SKU/GTIN, technical measurements, currency/country/ISO codes, personal names, prescriptions | plain columns; single value | **never auto-translated**; rendered as-is, bidi-isolated in RTL |

## Rules
- Class C codes are `UPPER_SNAKE`/dotted stable identifiers (e.g. `LENS_MANUFACTURER`, `OEM`, `FACTORY_EXISTENCE`, `BUSINESS_REGISTRATION_DOCUMENT`). Adding a code adds a UI label key; **no schema change**, no code depends on the label text.
- Class D: the entity carries `original_locale` + `source_version`; the original-language content is authoritative; translations are additive and reviewable; changing source bumps `source_version` → dependent translations go stale.
- Class E fields are excluded from any translation table and from MT (ADR-022). An **officially registered** translated legal name is the only exception and is stored as an explicit separate field, not a translation.
- Reviewer and public surfaces must indicate fallback/stale/MT state whenever the displayed language differs from what the user requested (§10).
