# REMAKE Design Tokens

> v2.0.0 · 2026-11-20 · The 24 local variables, 2 paint styles, and 6 mobile text styles of the Figma REMAKE file, copied with their names unchanged.
>
> Korean version for developers: [`docs/developer/design-tokens.md`](../developer/design-tokens.md)

## Principle: Figma design comes first

- Tokens are only the variables and styles defined in Figma, copied with their names unchanged. Do not merge values or force them onto a 4pt (or any other) scale.
- When building a screen, read the Figma frame (Dev Mode). If a value has a variable, use the token with the same name. If it has none (spacing, radius, shadow, glass, ...), use the frame value as is.
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

## Figma variables → CSS variables

`/` in a Figma name becomes `-`. Typos (`warring`, `skeletn`) are kept as in Figma.

### Text colors (`text/`)

| CSS variable              | Value     | Figma variable     |
| ------------------------- | --------- | ------------------ |
| `var(--text-header)`      | `#111827` | `text/header`      |
| `var(--text-title)`       | `#374151` | `text/title`       |
| `var(--text-content)`     | `#4B5563` | `text/content`     |
| `var(--text-assist)`      | `#98A3B4` | `text/assist`      |
| `var(--text-placeholder)` | `#ABB7CA` | `text/placeholder` |
| `var(--text-disabled)`    | `#B0B8C1` | `text/disabled`    |
| `var(--text-primary)`     | `#3182F6` | `text/primary`     |
| `var(--text-error)`       | `#EF4444` | `text/error`       |
| `var(--text-warring)`     | `#FF7119` | `text/warring`     |
| `var(--text-safe)`        | `#179C00` | `text/safe`        |
| `var(--text-white)`       | `#F8F9F6` | `text/white`       |

### Backgrounds (`bg/`)

| CSS variable                    | Value                    | Figma variable           |
| ------------------------------- | ------------------------ | ------------------------ |
| `var(--bg-white)`               | `#FFFFFF`                | `bg/white`               |
| `var(--bg-1depth)`              | `#F2F4F7`                | `bg/1depth`              |
| `var(--bg-1depth-bg)`           | `#EFF5FF`                | `bg/1depth-bg`           |
| `var(--bg-blue-btn-bg)`         | `#EBF3FF`                | `bg/blue-btn-bg`         |
| `var(--bg-input-bg)`            | `#F2F4F6`                | `bg/input-bg`            |
| `var(--bg-gray-1depth)`         | `#F5F5F5`                | `bg/gray-1depth`         |
| `var(--bg-gray-2depth)`         | `#EAEAEA`                | `bg/gray-2depth`         |
| `var(--bg-gray-3depth)`         | `#CFCFCF`                | `bg/gray-3depth`         |
| `var(--bg-gray-4depth)`         | `#B9B9B9`                | `bg/gray-4depth`         |
| `var(--bg-skeletn-bg)`          | `#E9ECEF`                | `bg/skeletn-bg`          |
| `var(--bg-skeleton-highlights)` | `#F1F5F9`                | `bg/skeleton-highlights` |
| `var(--bg-shadow)`              | `rgba(25, 31, 40, 0.32)` | `bg/shadow`              |

### Lines

| CSS variable      | Value     | Figma variable |
| ----------------- | --------- | -------------- |
| `var(--stroke-1)` | `#E2E8F0` | `stroke-1`     |

## Figma paint styles

| CSS variable                 | Value     | Figma style     |
| ---------------------------- | --------- | --------------- |
| `var(--style-primay)`        | `#3182F6` | `primay`        |
| `var(--style-complementary)` | `#F56214` | `complementary` |

## Figma text styles (mobile)

Line height is auto for every style in Figma, so it stays CSS `normal`. The 6 tablet styles are omitted because the app width (360–480px) never uses them.

| Usage                                  | Figma style            | Size | Weight   |
| -------------------------------------- | ---------------------- | ---- | -------- |
| `font: var(--font-header-mobile)`      | `header - mobile`      | 18px | SemiBold |
| `font: var(--font-title-mobile)`       | `title - mobile`       | 16px | Medium   |
| `font: var(--font-content-mobile)`     | `content - mobile`     | 14px | Medium   |
| `font: var(--font-assist-mobile)`      | `assist - mobile`      | 12px | Medium   |
| `font: var(--font-placeholder-mobile)` | `placeholder - mobile` | 12px | Medium   |
| `font: var(--font-error-mobile)`       | `error - mobile`       | 12px | Regular  |

## Ingredient category dot colors

Values used on the Figma spending-pattern screen. They are tokens because the color is chosen from data (the category code). The variable name is the category code in lowercase kebab-case (e.g. `DAIRY_EGG` → `--category-dairy-egg`).

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
| `--font-family-base` | Pretendard + others | Font family (Figma's Pretendard plus fallback fonts)                                                  |
| `--font-size-input`  | `1rem`              | Input text is 16px. Figma uses 14 and 12px, but iOS auto-zooms the page when input text is under 16px |
| `--z-sticky`         | `100`               | Sticky top header and tabs                                                                            |
| `--z-fixed`          | `200`               | Fixed bottom button area                                                                              |
| `--z-dropdown`       | `300`               | Kebab menu                                                                                            |
| `--z-overlay`        | `400`               | Dim background                                                                                        |
| `--z-sheet`          | `500`               | Bottom sheet                                                                                          |
| `--z-modal`          | `600`               | Modal                                                                                                 |
| `--z-toast`          | `700`               | Toast                                                                                                 |
