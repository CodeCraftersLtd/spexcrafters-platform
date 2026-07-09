# Supported Locales — Registry (Phase 7)

**Status:** Binding. The single source of truth for locale codes across the whole platform (frontend routing, backend `SupportedLocale`, DB `locale` columns, message-resource directories, CI validation). Codes are **BCP 47**. Never use `cn/kr/jp/in`, "Iranian", or "Tagalog" as codes.

## The 20 launch locales

| # | Language | Code (BCP 47) | Script | Dir | Fallback |
|---|---|---|---|---|---|
| 1 | English (canonical source) | `en` | Latin | ltr | — |
| 2 | Simplified Chinese | `zh-CN` | Han (Simplified) | ltr | en |
| 3 | Korean | `ko` | Hangul | ltr | en |
| 4 | Japanese | `ja` | Japanese | ltr | en |
| 5 | Thai | `th` | Thai | ltr | en |
| 6 | Vietnamese | `vi` | Latin (extended) | ltr | en |
| 7 | French | `fr` | Latin | ltr | en |
| 8 | Spanish | `es` | Latin | ltr | en |
| 9 | Indonesian | `id` | Latin | ltr | en |
| 10 | Malay | `ms` | Latin | ltr | en |
| 11 | Hindi | `hi` | Devanagari | ltr | en |
| 12 | Russian | `ru` | Cyrillic | ltr | en |
| 13 | Bengali | `bn` | Bengali | ltr | en |
| 14 | German | `de` | Latin | ltr | en |
| 15 | Urdu | `ur` | Arabic (Nastaliq) | **rtl** | en |
| 16 | Filipino | `fil` | Latin | ltr | en |
| 17 | Persian | `fa` | Arabic | **rtl** | en |
| 18 | Portuguese | `pt` | Latin | ltr | en |
| 19 | Arabic | `ar` | Arabic | **rtl** | en |
| 20 | Turkish | `tr` | Latin | ltr | en |

- **Canonical fallback:** `en` (deterministic; every UI key and content fallback resolves to `en` last).
- **RTL set:** `ar`, `fa`, `ur` (first-class RTL). Everything else `ltr`.
- **Turkish note:** locale-aware case only (dotless-ı); never `toLowerCase()` locale strings with the Turkish locale — locale codes are normalized with `Locale.ROOT`.

## Script → font group (typography strategy, see ADR-021)

| Group | Locales | Primary font (self-hosted, subset) | Fallback |
|---|---|---|---|
| Latin | en, vi, fr, es, id, ms, de, fil, pt, tr | Instrument Sans / Archivo (existing MERIDIAN) | system-ui |
| CJK-SC | zh-CN | Noto Sans SC | system CJK |
| Korean | ko | Noto Sans KR | system |
| Japanese | ja | Noto Sans JP | system |
| Thai | th | Noto Sans Thai | system |
| Devanagari | hi | Noto Sans Devanagari | system |
| Cyrillic | ru | Instrument Sans (Cyrillic subset) → Noto Sans | system |
| Bengali | bn | Noto Sans Bengali | system |
| Arabic-script | ar, fa, ur | Noto Sans Arabic (ur: Noto Nastaliq Urdu display) | system |

Fonts are **loaded per active locale's script group only** (never one global payload); Latin is always present (mixed technical content). Line-height overrides per group documented in ADR-021.

## Future-proofing

Adding `zh-TW`, `it`, `nl`, `pl`, … = one row here + one message directory + one `supported_locale` seed row. **No schema redesign** (translations are row-per-locale, never column-per-language).

## Test tiers (CI, §23)

- **Tier 1 (full E2E):** `en`, `zh-CN`, `ar`, `de` (fallback / CJK / RTL / expansion).
- **Tier 2 (component+routing):** the other 16.
- **Tier 3 (structural CI, all 20):** registration, message-resource existence, namespace + key parity, ICU syntax, placeholder/plural parity, RTL declaration, routing, fallback, unsupported-locale rejection.

## Normalization rules

- Accept only these 20 codes (case-insensitive match → canonical case as above). Unknown/`x-default`/legacy → resolve to `en` (never 500). `zh`, `zh-Hans` → `zh-CN` (documented alias, redirect at the edge). The Phase-1..6 code used `zh-Hans`/no-`es` etc.; Phase 7 **migrates** message dirs to this registry (see i18n workstream).
