/**
 * @spexcrafters/design-tokens — GENERATED FILE, do not edit by hand.
 * Source of truth: src/*.tokens.json + src/themes/*.json
 * Regenerate with: pnpm --filter @spexcrafters/design-tokens build
 *
 * Raw values for non-CSS consumers (charts, <canvas>, OG images, email).
 * Note: fonts.* and borders.* are CSS value strings and may contain var()
 * references — only meaningful where CSS custom properties resolve.
 */

export const colors = {
  "paper": {
    "0": "#FDFDFB",
    "1": "#F7F7F4",
    "2": "#F1F1EC"
  },
  "ink": {
    "50": "#F0F1F3",
    "100": "#E4E7EB",
    "200": "#CDD2D9",
    "300": "#A6AEB8",
    "400": "#7A8492",
    "500": "#5C6672",
    "600": "#444D58",
    "700": "#2E353E",
    "800": "#1F242B",
    "900": "#14171C",
    "950": "#0C0E12"
  },
  "accent": {
    "50": "#EDF1FD",
    "100": "#D8E1FA",
    "200": "#B3C4F4",
    "300": "#86A0EC",
    "400": "#5A7BE0",
    "500": "#3A5CCC",
    "600": "#2947B8",
    "700": "#1F3691",
    "800": "#17296E",
    "900": "#101D4E"
  },
  "success": {
    "100": "#D9F0E2",
    "300": "#6FCF97",
    "600": "#22894F",
    "700": "#1B6E45",
    "800": "#14522F",
    "900": "#0E3D23"
  },
  "warning": {
    "100": "#FBEFD3",
    "300": "#F0BE4C",
    "600": "#B37800",
    "700": "#9E6900",
    "800": "#8A5A00",
    "900": "#6B4600"
  },
  "danger": {
    "100": "#FADEDE",
    "300": "#F08A8A",
    "600": "#BA2E2E",
    "700": "#9E2626",
    "800": "#7D1D1D",
    "900": "#641717"
  },
  "info": {
    "100": "#D9EDF8",
    "300": "#67B7E1",
    "600": "#0E71A6",
    "700": "#0B5E8A",
    "800": "#084566",
    "900": "#06344D"
  }
} as const;

export const gradients = {
  "spectral": "linear-gradient(90deg, #6E56CF 0%, #3E63DD 22%, #00A2C7 42%, #30A46C 62%, #FFC53D 82%, #E5484D 100%)"
} as const;

export const fonts = {
  "display": "\"Archivo\", var(--sc-font-fallback-sans)",
  "ui": "\"Instrument Sans\", var(--sc-font-fallback-sans)",
  "mono": "\"IBM Plex Mono\", ui-monospace, \"SFMono-Regular\", Menlo, Consolas, monospace",
  "fallback-sans": "-apple-system, \"Segoe UI\", system-ui, Arial, sans-serif",
  "weight": {
    "regular": 400,
    "medium": 500,
    "semibold": 600,
    "bold": 700
  }
} as const;

export const text = {
  "2xs": "0.6875rem",
  "xs": "0.75rem",
  "sm": "0.875rem",
  "md": "clamp(0.9375rem, 0.9rem + 0.2vw, 1rem)",
  "lg": "clamp(1.0625rem, 1rem + 0.35vw, 1.1875rem)",
  "xl": "clamp(1.25rem, 1.15rem + 0.5vw, 1.5rem)",
  "2xl": "clamp(1.5rem, 1.3rem + 1vw, 2rem)",
  "3xl": "clamp(1.875rem, 1.5rem + 1.8vw, 2.75rem)",
  "4xl": "clamp(2.375rem, 1.8rem + 2.8vw, 3.75rem)",
  "display": "clamp(2.75rem, 2rem + 4vw, 5.25rem)"
} as const;

export const leading = {
  "tight": 1.15,
  "snug": 1.3,
  "body": 1.55,
  "ui": 1.4
} as const;

export const tracking = {
  "display": "-0.015em",
  "normal": 0,
  "caps": "0.06em",
  "mono": 0
} as const;

export const space = {
  "0": "0",
  "1": "4px",
  "2": "8px",
  "3": "12px",
  "4": "16px",
  "5": "20px",
  "6": "24px",
  "8": "32px",
  "10": "40px",
  "12": "48px",
  "16": "64px",
  "20": "80px",
  "24": "96px",
  "32": "128px"
} as const;

export const radius = {
  "none": "0",
  "xs": "2px",
  "sm": "4px",
  "md": "6px",
  "lg": "8px",
  "full": "9999px"
} as const;

export const shadows = {
  "0": "none",
  "1": "0 1px 2px rgb(12 14 18 / 0.06)",
  "2": "0 2px 8px rgb(12 14 18 / 0.10)",
  "3": "0 8px 24px rgb(12 14 18 / 0.14)",
  "4": "0 16px 48px rgb(12 14 18 / 0.18)"
} as const;

export const borders = {
  "hairline": "1px solid var(--sc-color-border-decorative)",
  "strong": "1px solid var(--sc-color-border-strong)",
  "selected": "2px solid var(--sc-color-action-primary)",
  "error": "1px solid var(--sc-color-status-danger)"
} as const;

export const z = {
  "base": 0,
  "raised": 10,
  "sticky": 100,
  "header": 200,
  "dropdown": 1000,
  "overlay": 1300,
  "modal": 1400,
  "popover": 1500,
  "toast": 1600,
  "tooltip": 1700
} as const;

export const grid = {
  "max": "1320px"
} as const;

export const breakpoints = {
  "sm": "480px",
  "md": "768px",
  "lg": "1024px",
  "xl": "1280px",
  "2xl": "1536px"
} as const;

export const motion = {
  "duration": {
    "instant": "80ms",
    "fast": "120ms",
    "base": "160ms",
    "slow": "240ms",
    "story": "400ms"
  },
  "ease": {
    "standard": "cubic-bezier(0.2, 0, 0, 1)",
    "exit": "cubic-bezier(0.3, 0, 1, 1)",
    "instrument": "cubic-bezier(0.5, 0, 0.1, 1)"
  }
} as const;

export const themes = {
  "light": {
    "color": {
      "bg": {
        "page": "#FDFDFB",
        "surface": "#FDFDFB",
        "surface-raised": "#F7F7F4",
        "surface-sunken": "#F1F1EC",
        "hover-wash": "#EDF1FD",
        "active-wash": "#D8E1FA"
      },
      "text": {
        "primary": "#14171C",
        "secondary": "#444D58",
        "muted": "#5C6672",
        "disabled": "#A6AEB8",
        "placeholder": "#7A8492",
        "inverse": "#FDFDFB"
      },
      "action": {
        "primary": "#2947B8",
        "primary-hover": "#1F3691",
        "primary-active": "#17296E",
        "danger": "#BA2E2E",
        "danger-hover": "#9E2626",
        "danger-active": "#7D1D1D",
        "danger-text": "#FDFDFB"
      },
      "link": "#2947B8",
      "border": {
        "decorative": "#CDD2D9",
        "strong": "#7A8492",
        "strong-hover": "#5C6672"
      },
      "focus-ring": "#2947B8",
      "status": {
        "success": "#1B6E45",
        "success-bg": "#D9F0E2",
        "success-on-bg": "#14522F",
        "warning": "#8A5A00",
        "warning-bg": "#FBEFD3",
        "warning-on-bg": "#6B4600",
        "danger": "#BA2E2E",
        "danger-bg": "#FADEDE",
        "danger-on-bg": "#7D1D1D",
        "info": "#0B5E8A",
        "info-bg": "#D9EDF8",
        "info-on-bg": "#084566"
      }
    }
  },
  "dark": {
    "color": {
      "bg": {
        "page": "#0C0E12",
        "surface": "#14171C",
        "surface-raised": "#1F242B",
        "surface-sunken": "#0C0E12",
        "hover-wash": "#1F242B",
        "active-wash": "#2E353E"
      },
      "text": {
        "primary": "#E7E9EC",
        "secondary": "#A6AEB8",
        "muted": "#7A8492",
        "disabled": "#444D58",
        "placeholder": "#7A8492",
        "inverse": "#0C0E12"
      },
      "action": {
        "primary": "#86A0EC",
        "primary-hover": "#B3C4F4",
        "primary-active": "#D8E1FA",
        "danger": "#BA2E2E",
        "danger-hover": "#9E2626",
        "danger-active": "#7D1D1D",
        "danger-text": "#FDFDFB"
      },
      "link": "#86A0EC",
      "border": {
        "decorative": "#2E353E",
        "strong": "#7A8492",
        "strong-hover": "#A6AEB8"
      },
      "focus-ring": "#86A0EC",
      "status": {
        "success": "#6FCF97",
        "success-bg": "#0E3D23",
        "success-on-bg": "#6FCF97",
        "warning": "#F0BE4C",
        "warning-bg": "#6B4600",
        "warning-on-bg": "#F0BE4C",
        "danger": "#F08A8A",
        "danger-bg": "#641717",
        "danger-on-bg": "#F08A8A",
        "info": "#67B7E1",
        "info-bg": "#06344D",
        "info-on-bg": "#67B7E1"
      }
    }
  }
} as const;

export type ThemeName = keyof typeof themes;
