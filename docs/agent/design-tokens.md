# REMAKE Design Tokens

> v3.1.0 · 2026-11-23 · Copied from the Figma REMAKE file, page `디자인 시스템` > frame `0. 기초` ([node 5761-2324](https://www.figma.com/design/etTm84sh4aA60z6BmOwM3i/REMAKE?node-id=5761-2324), "Remake 디자인 시스템 v1", 2026-11-22): 40 color variables, 11 spacing and 6 radius variables, 10 text styles, 5 shadow styles.
>
> Korean version for developers: [`docs/developer/design-tokens.md`](../developer/design-tokens.md)

## Principle: Figma design comes first

- Tokens are only the variables and styles defined in the Figma design system (`0. 기초`), copied with their names unchanged. CSS variable names equal the code names Figma Dev Mode shows (e.g. `var(--color-text-strong)`).
- When building a screen, read the Figma frame (Dev Mode). If a value has a variable or style, use the token with the same name. If it has none, use the frame value as is.
- If a token and Figma differ, follow Figma and update the token.
- Keep exceptions to what is functionally required ([Functional exceptions](#functional-exceptions)).

## Files and regeneration

| File                            | Role                                             |
| ------------------------------- | ------------------------------------------------ |
| `design/tokens/build_tokens.py` | Source. The token values live in this script.    |
| `design/tokens/tokens.json`     | Generated (data).                                |
| `design/tokens/tokens.css`      | Generated (CSS variables).                       |
| `design/tokens/index.html`      | Generated (preview page). Open it in a browser.  |
| `frontend/app/tokens.css`       | Copy used by the app. Imported by `globals.css`. |

To change a value, edit only `build_tokens.py`. Never edit generated files by hand.

```bash
python3 design/tokens/build_tokens.py
cp design/tokens/tokens.css frontend/app/tokens.css
```

## Naming

| Figma                                    | CSS variable               | Example                                         |
| ---------------------------------------- | -------------------------- | ----------------------------------------------- |
| Color variable `<group>/<name>`          | `--color-<group>-<name>`   | `text/strong` → `var(--color-text-strong)`      |
| Size variable `space/<px>`               | `--space-<px>`             | `space/16` → `var(--space-16)` (1rem)           |
| Size variable `radius/<step>`            | `--radius-<step>`          | `radius/md` → `var(--radius-md)` (0.625rem)     |
| Text style `Remake/<name>`               | `--font-<name>`            | `font: var(--font-content)`                     |
| Effect style `Remake/shadow-<name>`      | `--shadow-<name>`          | `box-shadow: var(--shadow-card)`                |

## Colors (`Remake Color`)

### Text (`text/`)

| CSS variable                     | Value     | Figma              |
| -------------------------------- | --------- | ------------------ |
| `var(--color-text-strong)`       | `#111827` | `text/strong`      |
| `var(--color-text-title)`        | `#374151` | `text/title`       |
| `var(--color-text-body)`         | `#4B5563` | `text/body`        |
| `var(--color-text-sub)`          | `#9CA3AF` | `text/sub`         |
| `var(--color-text-assist)`       | `#98A3B4` | `text/assist`      |
| `var(--color-text-placeholder)`  | `#ABB7CA` | `text/placeholder` |
| `var(--color-text-disabled)`     | `#B0B8C1` | `text/disabled`    |
| `var(--color-text-on-color)`     | `#F8F9F6` | `text/on-color`    |
| `var(--color-text-primary)`      | `#3182F6` | `text/primary`     |
| `var(--color-text-danger)`       | `#DC2626` | `text/danger`      |
| `var(--color-text-warning)`      | `#FF7119` | `text/warning`     |
| `var(--color-text-safe)`         | `#179C00` | `text/safe`        |

### Backgrounds (`bg/`)

| CSS variable                          | Value                    | Figma                   |
| ------------------------------------- | ------------------------ | ----------------------- |
| `var(--color-bg-surface)`             | `#FFFFFF`                | `bg/surface`            |
| `var(--color-bg-subtle)`              | `#F2F4F7`                | `bg/subtle`             |
| `var(--color-bg-section)`             | `#F3F4F6`                | `bg/section`            |
| `var(--color-bg-input)`               | `#F2F4F6`                | `bg/input`              |
| `var(--color-bg-primary-subtle)`      | `#EBF3FF`                | `bg/primary-subtle`     |
| `var(--color-bg-primary-tint)`        | `#EFF5FF`                | `bg/primary-tint`       |
| `var(--color-bg-skeleton)`            | `#E9ECEF`                | `bg/skeleton`           |
| `var(--color-bg-skeleton-highlight)`  | `#F1F5F9`                | `bg/skeleton-highlight` |
| `var(--color-bg-primary)`             | `#3182F6`                | `bg/primary`            |
| `var(--color-bg-danger)`              | `#DC2626`                | `bg/danger`             |
| `var(--color-bg-badge-danger)`        | `#DC262626` (#DC2626 15%) | `bg/badge-danger`      |
| `var(--color-bg-badge-warning)`       | `#FF71191F` (#FF7119 12%) | `bg/badge-warning`     |
| `var(--color-bg-badge-safe)`          | `#179C001F` (#179C00 12%) | `bg/badge-safe`        |
| `var(--color-bg-dim)`                 | `#191F2899` (#191F28 60%) | `bg/dim`               |
| `var(--color-bg-toast)`               | `#111827EB` (#111827 92%) | `bg/toast` (1)         |

(1) `bg/toast` is a Figma variable used by the toast (default, success, error) but has no Dev Mode code name and is not shown in `0. 기초`. Its CSS name follows the same rule.

### Borders (`border/`)

| CSS variable                   | Value     | Figma            |
| ------------------------------ | --------- | ---------------- |
| `var(--color-border-default)`  | `#E2E8F0` | `border/default` |
| `var(--color-border-subtle)`   | `#F5F5F5` | `border/subtle`  |
| `var(--color-border-control)`  | `#CFCFCF` | `border/control` |
| `var(--color-border-chip)`     | `#EAEAEA` | `border/chip`    |

### Icons (`icon/`)

| CSS variable                   | Value     | Figma           |
| ------------------------------ | --------- | --------------- |
| `var(--color-icon-default)`    | `#374151` | `icon/default`  |
| `var(--color-icon-sub)`        | `#9CA3AF` | `icon/sub`      |
| `var(--color-icon-disabled)`   | `#CFCFCF` | `icon/disabled` |
| `var(--color-icon-favorite)`   | `#FDD43E` | `icon/favorite` |

### Brand and social (`brand/`, `social/`)

| CSS variable                        | Value     | Figma                 |
| ----------------------------------- | --------- | --------------------- |
| `var(--color-brand-primary)`        | `#3182F6` | `brand/primary`       |
| `var(--color-brand-complementary)`  | `#F56214` | `brand/complementary` |
| `var(--color-social-kakao)`         | `#FEE500` | `social/kakao`        |
| `var(--color-social-naver)`         | `#03C75A` | `social/naver`        |
| `var(--color-social-google)`        | `#F2F2F2` | `social/google`       |

## Spacing and radius (`Remake Size`)

Values are in rem (px ÷ 16).

| CSS variable       | px  |     | CSS variable          | px  |
| ------------------ | --- | --- | --------------------- | --- |
| `var(--space-2)`   | 2   |     | `var(--radius-xs)`    | 4   |
| `var(--space-4)`   | 4   |     | `var(--radius-sm)`    | 8   |
| `var(--space-6)`   | 6   |     | `var(--radius-md)`    | 10  |
| `var(--space-8)`   | 8   |     | `var(--radius-lg)`    | 14  |
| `var(--space-10)`  | 10  |     | `var(--radius-xl)`    | 20  |
| `var(--space-12)`  | 12  |     | `var(--radius-full)`  | 100 |
| `var(--space-14)`  | 14  |     |                       |     |
| `var(--space-16)`  | 16  |     |                       |     |
| `var(--space-20)`  | 20  |     |                       |     |
| `var(--space-24)`  | 24  |     |                       |     |
| `var(--space-30)`  | 30  |     |                       |     |

## Text styles (`Remake/*`)

Line height 140%, letter spacing 0 for every style. Font: Pretendard. Mobile only (app width 360–480px).

| Usage                              | Figma style              | Size | Weight        |
| ---------------------------------- | ------------------------ | ---- | ------------- |
| `font: var(--font-display)`        | `Remake/display`         | 22px | Bold 700      |
| `font: var(--font-header)`         | `Remake/header`          | 18px | Bold 700      |
| `font: var(--font-subheader)`      | `Remake/subheader`       | 16px | Bold 700      |
| `font: var(--font-title)`          | `Remake/title`           | 16px | Medium 500    |
| `font: var(--font-content)`        | `Remake/content`         | 14px | Medium 500    |
| `font: var(--font-content-regular)`| `Remake/content-regular` | 14px | Regular 400   |
| `font: var(--font-content-strong)` | `Remake/content-strong`  | 14px | Bold 700      |
| `font: var(--font-assist)`         | `Remake/assist`          | 12px | Medium 500    |
| `font: var(--font-assist-strong)`  | `Remake/assist-strong`   | 12px | Bold 700      |
| `font: var(--font-error)`          | `Remake/error`           | 12px | Regular 400   |

## Shadows (`Remake/shadow-*`)

| Usage                                | Figma                  | Value (color, offset, blur)  |
| ------------------------------------ | ---------------------- | ---------------------------- |
| `box-shadow: var(--shadow-control)`  | `Remake/shadow-control`| `#0000001F`, (0, 0), 4       |
| `box-shadow: var(--shadow-card)`     | `Remake/shadow-card`   | `#00000014`, (0, 1), 4       |
| `box-shadow: var(--shadow-sheet)`    | `Remake/shadow-sheet`  | `#0000001A`, (0, -4), 10     |
| `box-shadow: var(--shadow-modal)`    | `Remake/shadow-modal`  | `#00000029`, (0, 4), 16      |
| `box-shadow: var(--shadow-header)`   | `Remake/shadow-header` | `#3182F61A`, (0, 4), 20      |

## Icons

The design system has 24 icons (`아이콘/*`, 24px, color bound to `icon/default`; instances change the color). In code use Solar icons (`@solar-icons/react`) as the frontend rules require: pick the closest Solar icon and set its color with the `--color-icon-*` tokens.

## Ingredient category dot colors

Values used on the Figma spending-pattern screen. They are not design-system variables, but they are tokens because the color is chosen from data (the category code). The variable name is the category code in lowercase kebab-case (e.g. `DAIRY_EGG` → `--category-dairy-egg`).

| CSS variable                | Value     | Category (code)                         |
| --------------------------- | --------- | --------------------------------------- |
| `var(--category-vegetable)` | `#2E9E5D` | Vegetables (`VEGETABLE`)                |
| `var(--category-fruit)`     | `#CE3B78` | Fruit (`FRUIT`)                         |
| `var(--category-meat)`      | `#C24747` | Meat (`MEAT`)                           |
| `var(--category-seafood)`   | `#358AD4` | Seafood (`SEAFOOD`)                     |
| `var(--category-dairy-egg)` | `#A58812` | Eggs and dairy (`DAIRY_EGG`)            |
| `var(--category-grain)`     | `#B98046` | Grains, noodles, bread (`GRAIN`)        |
| `var(--category-bean-tofu)` | `#779933` | Tofu and beans (`BEAN_TOFU`)            |
| `var(--category-processed)` | `#9947C2` | Processed and frozen food (`PROCESSED`) |
| `var(--category-side-dish)` | `#DA5D2F` | Kimchi and side dishes (`SIDE_DISH`)    |
| `var(--category-seasoning)` | `#2E9E8B` | Seasoning (`SEASONING`)                 |
| `var(--category-beverage)`  | `#4641C8` | Beverages (`BEVERAGE`)                  |
| `var(--category-etc)`       | `#7B838E` | Other (`ETC`)                           |

## Functional exceptions

| Variable             | Value               | Reason                                                                                                |
| -------------------- | ------------------- | ----------------------------------------------------------------------------------------------------- |
| `--font-family-base` | Pretendard + others | Font family (Figma's Pretendard plus fallback fonts)             |
| `--font-size-input`  | `1rem`              | Input text is 16px. Figma uses 14 and 12px, but iOS auto-zooms the page when input text is under 16px |
| `--z-sticky`         | `100`               | Sticky top header and tabs                                                                            |
| `--z-fixed`          | `200`               | Fixed bottom button area                                                                              |
| `--z-dropdown`       | `300`               | Kebab menu                                                                                            |
| `--z-overlay`        | `400`               | Dim background                                                                                        |
| `--z-sheet`          | `500`               | Bottom sheet                                                                                          |
| `--z-modal`          | `600`               | Modal                                                                                                 |
| `--z-toast`          | `700`               | Toast                                                                                                 |

## Changes from v2

v2 copied the old local variables (`text/header`, `bg/white`, `stroke-1`, paint styles `primay`/`complementary`, `* - mobile` text styles). v3 replaces all of them with the design-system names. Old → new for the ones the app used: `--text-header` → `--color-text-strong`, `--text-content` → `--color-text-body`, `--text-assist` → `--color-text-assist`, `--text-white` → `--color-text-on-color`, `--text-error` → `--color-text-danger`, `--bg-white` → `--color-bg-surface`, `--stroke-1` → `--color-border-default`, `--style-primay` → `--color-brand-primary`, `--font-header-mobile` → `--font-header` (now Bold), `--font-content-mobile` → `--font-content`.
