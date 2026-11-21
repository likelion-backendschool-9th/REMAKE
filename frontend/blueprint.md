# REMAKE Frontend Blueprint

Frontend design document for a service that manages fridge ingredients and reduces food waste with AI-tailored recipes and expiry-date alerts.
It is built as a mobile-first PWA and packaged as an Android app with PWABuilder (TWA).

Korean translation for developers: [`docs/developer/frontend-blueprint.md`](../docs/developer/frontend-blueprint.md)

> The screen list and routes are a draft. When planning and design are finalized, update this document first, then change the code.

## 1. Responsibilities

- **Frontend (Next.js)**: screens and user interaction only. No business logic or data storage.
- **Backend (Spring Boot)**: authentication, data, OCR and AI processing, push delivery.
- The frontend calls only the Spring REST API. Do not reimplement backend features with Next Route Handlers or Server Actions.
  Exception: the service-worker serving route (`app/serwist/[path]`).
- **`/api/` belongs to the backend.** In production the frontend and backend share one domain, and the proxy sends every path starting with `/api/` to Spring unchanged. Everything else goes to Next.js (see [`docs/agent/deployment.md`](../docs/agent/deployment.md)).
  - Do not create any route (page, Route Handler, etc.) whose URL starts with `/api/`. Requests to them never reach Next in production. Judge by the URL, not the folder: route groups do not appear in the URL, so `app/(group)/api/...` is also served at `/api/...`.
  - Frontend-only endpoints such as a future health check (`/healthz`) go outside `/api/`.
- **The frontend shows; the backend decides.** Calculations, validation, permissions, and limits (recipe generation quota, expiry status, food-cost totals, ...) come from the backend. The frontend displays the values it receives and validates only input format (required, length).
- **Server Components by default.** Keep pages and layouts as Server Components. Put `"use client"` only on the smallest leaf component that needs interaction or server data (TanStack Query, §5).

## 2. Tech stack

| Area            | Technology                             | Why                                                                           |
| --------------- | -------------------------------------- | ----------------------------------------------------------------------------- |
| Framework       | Next.js (App Router) + TypeScript      | Screens split per route; frontend handles screens only                        |
| Package manager | pnpm                                   | Fast installs; lockfile keeps dependency versions identical for all 7 members |
| Styling         | CSS Modules + CSS variables            | Component-scoped styles without extra libraries; shared design tokens         |
| Server data     | TanStack Query                         | Caching and refetching server data                                            |
| Client state    | Zustand                                | Only the minimum, such as login info                                          |
| URL state       | nuqs                                   | Filters, tabs, sort, and selected month live in the URL                       |
| API integration | openapi-typescript + openapi-fetch     | Types generated from Spring OpenAPI; type-safe calls                          |
| Forms           | React Hook Form + Zod                  | Validation for onboarding, custom recipe input, ingredient edits              |
| Dates           | dayjs                                  | Date display; meal-plan calendar                                              |
| Icons           | @solar-icons/react (Solar)             | Tree-shakable React components; work offline                                  |
| Font            | Pretendard (`next/font/local`)         | Korean UI font; no layout shift while loading                                 |
| PWA & push      | Serwist + Web Push                     | Service worker, offline screen, expiry alerts                                 |
| Store release   | PWABuilder (TWA)                       | Generates the Android app package                                             |
| Web deploy      | `output: "standalone"` + nginx         | Node server running behind nginx                                              |
| Code quality    | ESLint + Prettier, Husky + lint-staged | Unified code style for 7 people; automatic checks before commit               |
| API mocking     | MSW                                    | Build screens against dummy responses before the backend is ready             |

## 3. Folder structure

```
frontend/
├─ app/                  # routes. Each route folder colocates the code only it uses (see §3.1)
│  ├─ layout.tsx         # root layout, globals.css, home page.tsx
│  ├─ fridge/            # example route — see §3.1
│  ├─ sw.ts              # service worker (caching, push handling)
│  ├─ serwist/[path]/    # service-worker serving route
│  ├─ ~offline/          # offline fallback screen
│  └─ manifest.ts        # PWA manifest
├─ components/           # reusable UI shared by several routes, no domain knowledge (Button, Input, BottomSheet ...)
├─ hooks/                # reusable React hooks shared by several routes, no domain knowledge (useDebounce ...)
├─ lib/
│  ├─ api/               # client.ts (openapi-fetch), schema.d.ts (generated)
│  ├─ queries/           # query keys + query hooks shared by several routes, one file per domain
│  ├─ events/            # (planned) analytics tracker and screen-code constants (see §11)
│  ├─ query-client.ts
│  ├─ dayjs.ts           # dayjs with ko locale and plugins configured
│  └─ env.ts             # the only place that reads environment variables
├─ mocks/                # MSW handlers
├─ providers/            # global providers (Serwist, TanStack Query, MSW)
├─ stores/               # Zustand stores
└─ public/               # static files, mockServiceWorker.js, (planned) .well-known/assetlinks.json, icons/
```

### 3.1 Colocation rules

Code lives next to the page or component that uses it. Only code reused by several routes moves to the shared folders (`components/`, `hooks/`, `lib/`).

```
app/fridge/
├─ page.tsx                     # thin: composes _components, calls _lib/queries
├─ page.module.css
├─ _components/                 # components used only by this route (and its child routes)
│  └─ IngredientCard/
│     ├─ IngredientCard.tsx
│     ├─ IngredientCard.module.css
│     └─ useIngredientCard.ts   # hook used only by this component
├─ _hooks/                      # hooks used only by this route
├─ _lib/
│  ├─ queries.ts                # query keys + useQuery/useMutation for this route
│  ├─ schema.ts                 # Zod schemas
│  └─ utils.ts                  # pure functions (e.g. filter state → query params)
└─ [id]/
   └─ page.tsx
```

1. **Private folders.** Folders starting with `_` are Next.js private folders and are never routed. Put all non-route code of a route in `_components/`, `_hooks/`, `_lib/`. Never put `page.tsx` or `route.ts` inside them.
2. **Isolate by route.** A route may import colocated code from its own folder and from ancestor route folders (e.g. `app/fridge/[id]` may use `app/fridge/_components`). Never import from a sibling or unrelated route.
3. **Isolate by component.** Each component has its own folder with its `.tsx`, `.module.css`, and any hook or util only it uses. Do not import another component's CSS module or internal files.
4. **Promote when shared.** When two unrelated routes need the same code, move it up — UI component → `components/<Name>/`, React hook → `hooks/`, query keys and query hooks → `lib/queries/<domain>.ts`, other functions → `lib/`. Shared code must not know about a specific page.
5. **Import paths.** Import colocated code with relative paths (`./_components/...`, `../_lib/...`). Import shared code with `@/` (`@/components/Button`). Never import from `@/app/...` (enforced by ESLint).

Shared component layout:

```
components/Button/
├─ Button.tsx
├─ Button.module.css
└─ index.ts          # export { Button } from "./Button";
```

### 3.2 Naming

The folder structure above takes precedence: component folders and files are PascalCase, and hook files are named after the hook.

| Target                                             | Rule                                   | Example                             |
| -------------------------------------------------- | -------------------------------------- | ----------------------------------- |
| Component folders and files (and their CSS Module) | PascalCase                             | `IngredientCard/IngredientCard.tsx` |
| Hook files                                         | Hook name                              | `useIngredientCard.ts`              |
| Other files and folders, route folders             | kebab-case                             | `query-client.ts`, `meal-plan/`     |
| Dynamic segments and slots                         | camelCase                              | `[userId]`, `@sidePanel`            |
| Components, pages, types, Contexts, classes, enums | PascalCase                             | `UserProfilePage`, `AuthContext`    |
| Functions and variables                            | camelCase                              | `formatDate`                        |
| Hooks                                              | `use` + camelCase                      | `useAuth`                           |
| Constants                                          | UPPER_SNAKE_CASE                       | `MAX_RECIPE_PER_DAY`                |
| Acronyms                                           | Treat as one word                      | `ApiClient`, `api-client.ts`        |
| Event handlers                                     | `handle` + action; props `on` + action | `handleSubmit`, `onSubmit`          |
| Booleans                                           | `is` / `has` / `can` prefix            | `isOpen`, `hasError`                |

## 4. Screens / routes (draft)

| Route            | Screen                   | Main features                                                  |
| ---------------- | ------------------------ | -------------------------------------------------------------- |
| `/`              | Home                     | Ingredients near expiry, recommended recipe summary            |
| `/login`         | Login                    | Social login                                                   |
| `/onboarding`    | Onboarding               | Household size, then allergies (each step can be skipped)      |
| `/fridge`        | My fridge                | Ingredient list, sort by storage location / expiry date        |
| `/fridge/[id]`   | Ingredient detail & edit | Edit name, category, storage, expiry, price, quantity          |
| `/fridge/add`    | Add ingredient           | Choose manual entry or receipt photo                           |
| `/receipt`       | Receipt OCR              | Scan → loading (one request) → review and edit items → confirm |
| `/recipes`       | Recipe recommendations   | AI recommendations based on owned ingredients                  |
| `/recipes/[id]`  | Recipe detail            | Ingredients, cooking steps, missing ingredients                |
| `/recipes/new`   | Custom recipe            | Generate a recipe from selected options (no free text)         |
| `/meal-plan`     | Meal plan                | Monthly calendar, AI meal-plan generation                      |
| `/notifications` | Notifications            | Expiry alert list, push permission settings                    |
| `/mypage`        | My page                  | Profile, settings                                              |

## 5. Data flow

```
Spring API ──(OpenAPI spec)──> pnpm gen:api ──> lib/api/schema.d.ts
                                                     │
Screen (page.tsx) ─> _components ─> _lib/queries.ts or @/lib/queries (useQuery) ─> lib/api/client.ts ─> Spring API
                                                     │
                              during development NEXT_PUBLIC_API_MOCKING=true ─> MSW dummy responses
```

- **API address**: production images are built with an empty `NEXT_PUBLIC_API_BASE_URL`, so `lib/api/client.ts` calls the same domain with relative paths (`/api/...`) and the proxy forwards them to Spring. Login cookies work without CORS. In local development it defaults to `http://localhost:8080` (a different origin, so the backend needs CORS).
  - Relative paths only work in the browser. Call the API from Client Components (TanStack Query). If a Server Component must call the backend, ask first: it needs a separate server-side address set at runtime.
- **Server data** is managed entirely with TanStack Query. Do not copy server data into Zustand.
- **OCR and AI generation** are one synchronous request each; the response carries the result. Show a loading screen while waiting (time limits: OCR 30 s, AI 60 s). There is no job ID, polling, or streaming.
- **queryKey**s live next to their query hooks: the route's `_lib/queries.ts`, or `lib/queries/<domain>.ts` once shared. Example: `fridgeKeys.all`, `fridgeKeys.detail(id)`
- **API client**: every call goes through `lib/api/client.ts`. Send cookies (`credentials: "include"`). Set timeouts with `AbortSignal.timeout`. During development use generous values: default 30 s, OCR 60 s, AI generation 120 s. The frontend timeout must stay longer than the backend limit (above), so the screen receives the server's `OCR_TIMEOUT` / `AI_TIMEOUT` response instead of cutting the request first. Final values are decided near the end of development in #122.
- **Retries**: retry GET once. Never retry POST, PUT, PATCH, or DELETE, except APIs that take an `Idempotency-Key`. TanStack Query retries 3 times by default, so set queries to 1 and mutations to 0.
- **Caching**: data that rarely changes (categories, mart list, allergy list, recipes of the week) gets a long `staleTime` (minutes to 1 hour). Data that changes often (fridge ingredients, cart) gets a short `staleTime` and `invalidateQueries` after every change.
- **Errors**: keep the mapping from backend error `code` to user-facing text in one table, `lib/error-messages.ts` (§10 Errors). On 401, go to the login screen.
- **Zustand** holds only truly global state (logged-in user summary, UI settings) and state whose props would otherwise pass down more than 3 levels.
- **URL state**: filters, tabs, sort, and selected month go in the URL with nuqs. Bottom sheets, modals, and drawers are local state, never URL state (back button: §7.1).
- **Environment variables**: only values the browser may see get the `NEXT_PUBLIC_` prefix. Secrets stay on the server.

## 6. Styling rules

- No Tailwind or CSS-in-JS libraries. Use only **CSS Modules (`*.module.css`) + CSS variables in `globals.css`**.
- Import icons from the per-style paths of `@solar-icons/react`. Example: `import { HomeIcon } from "@solar-icons/react/linear"`. Default size, color, and stroke width come from the `--solar-*` variables in `globals.css`.
- Design tokens follow [`docs/agent/design-tokens.md`](../docs/agent/design-tokens.md): Figma variables and styles copied with their Figma names. `design/tokens/build_tokens.py` generates them; the result is copied to `app/tokens.css`, which `globals.css` imports. Never edit `app/tokens.css` by hand. If a Figma value has a variable or style, use the token with the same name. If it has none (spacing, radius, shadow, glass, ...), use the Figma frame value as is.
- Mobile first. Design for a 360px width. The default max layout width is `--layout-max-width` (480px) and the side gutter is `--layout-gutter`. Wider screens show the 480px container centered.
- Light theme only; no dark mode.
- Units: rem (1rem = 16px; never change the root font size). Convert Figma px by dividing by 16 (10px → 0.625rem). Use px only for lines such as borders.
- Font: load Pretendard with `next/font/local`.
- Full-screen height uses `100dvh`. Bottom-fixed elements add `env(safe-area-inset-bottom)`.
- Mostly flat. Apply glass effects only to elements that have them in Figma.
- Put a style file next to its component with the same name. Example: `IngredientCard.tsx` + `IngredientCard.module.css`

### 6.1 Semantic HTML

- Choose elements by meaning and purpose, not appearance. Use `a` with `href` for navigation and `button` for actions such as saving; do not substitute clickable `div` elements. Example: a link opens details, a button saves changes.
- Use `ul` or `ol` with `li` for lists, and `table` with header cells for tabular data. Use neutral containers for layout; do not turn every region into a `section`. When you use a `section`, give it a heading and connect them with `aria-labelledby`.
- Use native form controls with associated `label` elements. Do not use placeholders as the only labels. When a submit fails, move focus to the first invalid field. Give informative images meaningful `alt` text and decorative images empty `alt` text.

Follow the [W3C page structure guide](https://www.w3.org/WAI/tutorials/page-structure/) when expressing content relationships with appropriate elements.

### 6.2 Landmarks and keyboard accessibility

- Use `header` for the page-wide header, `nav` for major navigation, `main` for primary content, and `footer` for a page-wide footer when present. Keep one active `main`; do not duplicate it between layouts and pages. Distinguish article-level `header` and `footer` elements from page-wide landmarks. Give multiple navigation regions distinct names.
- Provide a skip-to-main-content link. Keep DOM, reading, and focus order aligned. Avoid positive `tabindex` values and preserve visible focus indicators.
- Make every action keyboard-operable. Example: navigate with Tab and Shift+Tab and activate native buttons with Enter or Space. Move focus into an opened modal, contain focus there, provide a close action and Escape handling, and return focus to its trigger on close. Modals, bottom sheets, and drawers use `role="dialog"`, `aria-modal="true"`, and `aria-labelledby`, and make the content behind them `inert`.
- Convey states and errors with text as well as color (e.g. category color plus its name). Keep content and controls available at increased zoom and narrow viewport widths.
- Touch targets are at least 44×44px. Text contrast is at least 4.5:1.
- Under `prefers-reduced-motion: reduce`, reduce animations.
- Checks: the jsx-a11y rules (included through `eslint-config-next`) must pass in `pnpm lint`. When a screen is finished, check it with axe or the Lighthouse accessibility audit.

Refer to the [W3C page structure guide](https://www.w3.org/WAI/tutorials/page-structure/) for landmarks and the [W3C keyboard accessibility guide](https://www.w3.org/WAI/WCAG22/Understanding/keyboard.html) for keyboard interaction.

### 6.3 WAI-ARIA usage

Prefer native HTML semantics and behavior. Add ARIA only for names, states, or change announcements that HTML alone cannot convey. Do not duplicate native roles or declare states that differ from the UI. ARIA attributes do not implement keyboard behavior. Follow the [W3C ARIA principles](https://www.w3.org/WAI/ARIA/apg/practices/read-me-first/).

| Attribute                                                   | Rule and brief example                                                                                                                                                                                                                                               |
| ----------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-label`                                                | Name icon-only buttons without visible names, such as “Close.” Prefer visible text or a `label` when available and include that wording in the accessible name.                                                                                                      |
| `aria-expanded`                                             | Reflect the actual open/closed state on custom disclosure buttons. Use `aria-controls` to reference the target ID when needed. Example: `false` when collapsed, `true` when expanded.                                                                                |
| `aria-live`                                                 | Announce save results or loading completion without moving focus (toasts, OCR results, generation complete). Example: update a pre-existing `polite` region to “Saved.” Reserve `assertive` or `role="alert"` for urgent messages and avoid duplicate announcements. |
| `aria-describedby` and `aria-invalid`                       | Associate help/error text with inputs and reflect validation errors. Example: preserve the input value while explaining that field's error.                                                                                                                          |
| `aria-hidden`                                               | Put `aria-hidden="true"` on decorative icons. Icon-only buttons still need `aria-label`.                                                                                                                                                                             |
| `role="tablist"`, `"tab"`, `"tabpanel"` and `aria-selected` | Tabs. Mark the selected tab with `aria-selected="true"`.                                                                                                                                                                                                             |
| `aria-pressed`                                              | Toggle buttons. Example: `true` while a favorite toggle is on.                                                                                                                                                                                                       |
| `aria-busy`                                                 | Set `true` on a region while its content loads.                                                                                                                                                                                                                      |

Follow the [W3C Disclosure pattern](https://www.w3.org/WAI/ARIA/apg/patterns/disclosure/) for expanded states and the [W3C status messages guide](https://www.w3.org/WAI/WCAG22/Understanding/status-messages.html) for dynamic announcements.

### 6.4 Basic SEO

- Provide a unique, concise `title` identifying each screen and a `meta description` summarizing its actual content. Example: `Screen name | REMAKE`. Exclude personal information from both.
- As a project convention, use one `h1` for the page's primary heading and organize subordinate content logically with `h2` through `h6`. Do not choose or skip heading levels for visual sizing.
- Write link text that describes its destination and image alternatives that convey meaning. Avoid repeating irrelevant keywords in titles and descriptions.
- Screens that need login (fridge, receipt, meal plan, ...) have nothing to show in search, so they set `robots: { index: false }`. Only public screens such as login and intro are indexed. REMAKE cannot be used without login, so SEO matters only for those public screens. Protect private data with authentication and authorization; search exclusion settings are not access controls.
- The root `layout.tsx` defines `metadata` (title template, description, openGraph, twitter, icons, manifest) and `viewport` (themeColor, `width=device-width`, `viewportFit: "cover"`). Each page sets its own `metadata` (title, description).
- Add `app/sitemap.ts`, `app/robots.ts`, and a default `app/opengraph-image`.
- Data-based metadata (e.g. `generateMetadata` with the OG title and image of a shared recipe) needs a server-side backend call. Ask first (§5).

Refer to the [Google SEO Starter Guide](https://developers.google.com/search/docs/fundamentals/seo-starter-guide) for titles and descriptions and the [W3C page structure guide](https://www.w3.org/WAI/tutorials/page-structure/) for heading hierarchy.

## 7. PWA · push · TWA

- The service worker runs only with `pnpm build && pnpm start`. It is off in development mode and while MSW is in use (avoids a scope conflict).
- The service worker (`/serwist/sw.js`) must be prerendered at build time (`●` in the `pnpm build` output). The standalone production server has no esbuild or `app/sw.ts`, so a request-time `ƒ` route returns 500.
  - Because `cacheComponents` is on, route segment configs (`dynamic`, `revalidate`, `dynamicParams`) are ignored or rejected. Do not re-export them from `createSerwistRoute`.
  - `app/serwist/[path]/route.ts` bundles the worker inside a `"use cache"` function, so the route stays prerendered no matter how long esbuild takes. Apply the same pattern to any `GET` Route Handler that must be static.
  - That function calls `cacheLife("max")`. `sw.js` only changes per build; with the default profile (revalidate 15 minutes) the production server would retry bundling in the background, fail without esbuild, and log an error on every retry. The old `sw.js` keeps being served either way.
  - As a result `sw.js` is sent with `Cache-Control: s-maxage=2592000` (30 days). Browsers ignore `s-maxage`, but a shared cache (CDN or proxy cache) in front of the app would keep an old service worker after a deploy. Exclude `/serwist/` from any shared cache.
  - After changing the service worker or this route, check that `pnpm build` lists `/serwist/sw.js` as `●` and that `.next/server/app/serwist/sw.js.body` exists.
- Push: the frontend creates a subscription with `NEXT_PUBLIC_VAPID_PUBLIC_KEY` and stores it on the backend; the backend sends notifications. Receiving and click handling live in `app/sw.ts`.
- Needed before the TWA release: 192/512 PNG icons, `public/.well-known/assetlinks.json` (app signing SHA-256), an HTTPS domain.

### 7.1 Back button closes sheets

- Bottom sheets, modals, and drawers never add URL history (their open state is local). The back action (Android back button, iOS swipe back) closes only the open sheet, without navigating.
- Handle this in one shared hook, `useBackToClose(isOpen, onClose)` in `hooks/`. Components never handle `popstate` themselves.
- Chromium browsers (including the TWA): receive the close request with `CloseWatcher` and set the state to closed.
- Browsers without `CloseWatcher` (Safari, Firefox): on open, push one temporary entry with `history.pushState` without changing the URL, and close on `popstate`. When the sheet is closed by a close button, remove that entry with `history.back()`.
- When sheets are stacked, close only the topmost one.

## 8. Development flow

1. If a backend API does not exist yet, write dummy responses in `mocks/handlers.ts` first and build the screen against them.
2. When the backend API is ready, regenerate types with `pnpm gen:api` and align the MSW handlers with the real spec.
3. Before a PR, `pnpm lint && pnpm typecheck && pnpm build` must pass.

TypeScript runs in `strict` mode, and `any` is not allowed (enforced by ESLint).

## 9. Open decisions

- [x] Authentication method and token refresh flow — **decided in [#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42) option A**: access (30 min) and refresh (14 d) are both JWT in HttpOnly cookies (`Secure; SameSite=Lax`). Login/refresh responses do not include `accessToken`. Call `/api/...` with `credentials: 'include'`; do not use `Authorization: Bearer` or store tokens in JS-readable storage. Locally (different origin) the backend must allow CORS with credentials.
- [ ] Shared component list (based on the design mockups)
- [ ] Frontend CI (GitHub Actions: lint · typecheck · build)
- [ ] Deployment environment (nginx + Node container setup)

## 10. UI rules from the product policy

Source: [`docs/agent/spec/domain-rules.md`](../docs/agent/spec/domain-rules.md). Screens follow Figma.

- **No free-text input for AI.** Recipe customization (ingredients, taste, time, tools, method, cuisine, excluded allergens) and meal-plan settings (start date, purpose, meals, servings) are selections only. Send IDs and enums.
- **Expiry state**: show the `status` and `dDay` the server returns (DANGER = expired or D-DAY, SOON = D-1 to D-3, OK = later). Do not recompute them with the device clock.
- **Labels**: show "AI가 생성한 레시피" on AI recipe details and shared recipe details. Show "참고용" wording on meal plans.
- **Receipt flow**: scan → loading screen (synchronous wait) → result and confirm screen → save. Nothing is saved until the user confirms. Leaving the result screen discards the OCR result and sends `receipt_result_abandoned`. A confirmed receipt cannot be edited, only deleted; fix ingredient data later in the fridge.
- **Quantity input**: stepper steps are g and ml 50, kg and L 0.5, EA/PACK/BUNCH/PIECE/SHEET 1. Typed input accepts numbers and decimals only, no fractions. The fridge uses 9 units (G, KG, ML, L, EA, PACK, BUNCH, PIECE, SHEET); recipes use all 14, and recipe amounts may be shown as 1/2, 1/3, or 1/4.
- **Sharing**: a title and at least one cooking photo are required, with a confirm modal first. Never show author information.
- **Meal plan**: monthly calendar; tapping a slot opens the recipe detail. Adding a meal puts a saved recipe into an empty slot. There is no delete and no slot regeneration. Show "남은 N회" from `GET /api/v1/user/usage-quotas`.
- **Cart**: grouped by recipe. Items are added automatically (meal-plan generation, saving a recipe); users can only change amounts and delete selected items. No checkout or external links.
- **Notifications**: only `EXPIRY_SOON` and `EXPIRED`. There is no "receipt analysis finished" notification.
- **Premium**: the subscription screen in settings is UI only.
- **Errors**: choose the message and behavior by `code` ([`docs/agent/spec/errors.md`](../docs/agent/spec/errors.md)), never by `message`. Unknown codes fall back to the `INTERNAL_ERROR` screen. Show `traceId` only in a "contact" detail, never as the main message.
- **Display formats**: the API sends dates as `2026-11-19`, timestamps as ISO 8601 with `+09:00`, and money as an integer in won. Display dates and money only through the shared functions in `lib/format.ts` (built on `@/lib/dayjs`), in the formats the policy document defines. User-facing copy is Korean.

## 11. Event tracking

Contract: [`docs/agent/spec/events.md`](../docs/agent/spec/events.md). Backend side: `docs/agent/backend-convention.md` §Event publishing.

- Use one shared tracker (planned at `lib/events/`). Components never call `POST /api/v1/user/events` directly.
- API: `track(eventName, properties?)`. The tracker adds `screen`, `sessionId` (a new session after 30 minutes idle), and `occurredAt`; batches up to 50 events; and flushes on an interval, on `visibilitychange`/`pagehide` (with `navigator.sendBeacon`), and on logout.
- Send only events whose source is CLIENT in the event sheet. The backend emits SERVER events; never record the same action from both sides.
- Swallow tracking failures silently: no toast, at most a small bounded retry.
- No personal data or free text in properties. Send codes, counts, and booleans (`category_code`, `item_count`). Properties are flat, at most 25 keys.
- Screen codes (`fridge_list`, `receipt_result`, `recipe_detail`, `meal_plan_calendar`, ...) come from the event sheet's screen-code tab. Keep them as constants in `lib/events/screens.ts`.
- Send `screen_viewed` from one shared layout hook, not by hand on each page.

## 12. Performance

- Images: use `next/image` (Next installs sharp). Always set `sizes`; use `priority` only for the large first-screen image. Register external image hosts in `images.remotePatterns`. Downscale user photos (receipts, cooking photos) before upload.
- Load heavy components (charts, bottom-sheet contents) with `next/dynamic`.
- Every route has `loading.tsx` (skeleton) and `error.tsx`; there is one global `not-found.tsx`. Split large regions with `Suspense` so they stream.
- Long lists (e.g. ranking TOP 100): infinite scroll, and consider virtualization.
- Bundle size: `pnpm next analyze` (Turbopack Bundle Analyzer, Next 16.1+). `@next/bundle-analyzer` works only with webpack builds.
