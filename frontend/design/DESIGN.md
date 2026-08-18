---
name: Structure LXP
colors:
  surface: "#f7fafc"
  surface-dim: "#d7dadc"
  surface-bright: "#f7fafc"
  surface-container-lowest: "#ffffff"
  surface-container-low: "#f1f4f6"
  surface-container: "#ebeef0"
  surface-container-high: "#e5e9eb"
  surface-container-highest: "#e0e3e5"
  on-surface: "#181c1e"
  on-surface-variant: "#44474c"
  inverse-surface: "#2d3133"
  inverse-on-surface: "#eef1f3"
  outline: "#74777d"
  outline-variant: "#c4c6cd"
  surface-tint: "#4f6073"
  primary: "#041627"
  on-primary: "#ffffff"
  primary-container: "#1a2b3c"
  on-primary-container: "#8192a7"
  inverse-primary: "#b7c8de"
  secondary: "#0453cd"
  on-secondary: "#ffffff"
  secondary-container: "#356ee7"
  on-secondary-container: "#fefcff"
  tertiary: "#211200"
  on-tertiary: "#ffffff"
  tertiary-container: "#38260b"
  on-tertiary-container: "#a88c69"
  error: "#ba1a1a"
  on-error: "#ffffff"
  error-container: "#ffdad6"
  on-error-container: "#93000a"
  primary-fixed: "#d2e4fb"
  primary-fixed-dim: "#b7c8de"
  on-primary-fixed: "#0b1d2d"
  on-primary-fixed-variant: "#38485a"
  secondary-fixed: "#dae2ff"
  secondary-fixed-dim: "#b2c5ff"
  on-secondary-fixed: "#001848"
  on-secondary-fixed-variant: "#0040a2"
  tertiary-fixed: "#feddb5"
  tertiary-fixed-dim: "#e1c29b"
  on-tertiary-fixed: "#281802"
  on-tertiary-fixed-variant: "#584326"
  background: "#f7fafc"
  on-background: "#181c1e"
  surface-variant: "#e0e3e5"
  slate-text: "#4A5568"
  success-green: "#22C55E"
  warning-amber: "#F59E0B"
  error-red: "#EF4444"
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: "700"
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: "700"
    lineHeight: 32px
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: "600"
    lineHeight: 32px
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: "600"
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: "400"
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: "400"
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: "400"
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: "600"
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: "500"
    lineHeight: 16px
    letterSpacing: 0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  container-max: 1280px
  gutter: 24px
  margin-desktop: 32px
  margin-mobile: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
---

## Brand & Style

The design system is engineered for the modern enterprise, balancing the gravity of corporate compliance with the agility of high-performance learning. It targets HR directors and professionals who require a dependable, scalable tool for talent development.

The visual style is **Corporate / Modern** with a strong leaning toward **Minimalism**. It prioritizes information density and clarity over decorative elements. By utilizing a restrained color palette and purposeful whitespace, the interface reduces cognitive load, allowing users to focus on educational content while feeling supported by a robust, secure infrastructure.

## Colors

The color strategy uses **Deep Navy (#1A2B3C)** as the primary anchor to instill trust and authority, reserved for persistent structural elements like sidebars and headers. **Trust Blue (#0052CC)** serves as the high-contrast action color, reserved strictly for interactive elements like primary buttons and progress indicators.

**Slate (#F4F7F9)** is the foundational background color, creating a soft, low-strain canvas. Use the named colors for semantic feedback: success for course completion, warning for expiring certifications, and error for system alerts. Background layers should utilize subtle shifts in saturation rather than pure gray to maintain a premium "SaaS" feel.

## Typography

This design system uses **Inter** exclusively to ensure maximum legibility and a systematic, technical appearance. The type hierarchy relies on weight and slight negative letter-spacing for headlines to create a "locked-in" professional look.

Body text uses a generous 1.5 line-height to ensure long-form learning content is easy to digest. Labels are always set in medium or semi-bold weights to differentiate them from body text at a glance. Mobile headlines are scaled down to prevent awkward word breaks in narrow course navigation.

## Layout & Spacing

The layout utilizes a **12-column Fluid Grid** with fixed maximum width for content-heavy pages like course modules.

- **Desktop (1200px+):** 12 columns, 24px gutters, 32px margins.
- **Tablet (768px - 1199px):** 8 columns, 16px gutters, 24px margins.
- **Mobile (Below 768px):** 4 columns, 16px gutters, 16px margins.

We employ a 4px base unit for all internal component spacing (padding/margins). Use "Stack" units for vertical rhythm: `stack-sm` for related elements (input + label), `stack-md` for component spacing, and `stack-lg` for section breaks.

## Elevation & Depth

Visual hierarchy is established through **Tonal Layers** supplemented by **Ambient Shadows**. The background layer (#F4F7F9) acts as Level 0. White surfaces (#FFFFFF) represent Level 1, used for primary cards and content areas.

Elevation levels:

1. **Flat:** 1px border (#E2E8F0), no shadow. Used for secondary inputs and lists.
2. **Raised:** 2px blur, 4% opacity black shadow. Used for interactive cards.
3. **Overlay:** 12px blur, 8% opacity black shadow. Used for dropdowns and tooltips.
4. **Modal:** 24px blur, 12% opacity black shadow. Used for dialogs and critical system alerts.

Avoid using shadows on the primary sidebar; instead, use color contrast (Deep Navy) to separate navigation from the workspace.

## Shapes

The shape language is consistently **Rounded** (0.5rem / 8px). This softens the corporate aesthetic, making the platform feel more approachable for learners while maintaining a structural grid.

- **Standard (8px):** Buttons, Input fields, Chips.
- **Large (16px):** Course cards, Dashboard widgets.
- **Extra Large (24px):** Promotional banners, Hero containers.

Buttons should never be fully pill-shaped; they must maintain a structured corner to align with the professional brand narrative.

## Components

### Buttons

- **Primary:** Trust Blue background, White text. 8px radius.
- **Secondary:** Transparent background, 1px Trust Blue border, Trust Blue text.
- **Ghost:** Transparent background, Deep Navy text. Used for less prominent actions in navigation.

### Input Fields

- White background with a 1px border (#E2E8F0). On focus, the border transitions to Trust Blue with a 2px soft glow. Labels should be `label-md` in Deep Navy.

### Cards

- Level 1 elevation (White surface). Ensure internal padding is consistent with `stack-md` (16px) or `stack-lg` (32px) for dashboard widgets.

### Progress Indicators

- Use Trust Blue for active progress. Background track should be a 10% opacity version of the Deep Navy.

### Chips

- Used for course categories or status tags. Use `label-sm` with a light tint of the semantic color (e.g., light green background with dark green text for "Completed").

### Lists

- Use subtle 1px dividers (#F1F5F9). Include a hover state that slightly shifts the background to a darker Slate tint to indicate interactivity.
