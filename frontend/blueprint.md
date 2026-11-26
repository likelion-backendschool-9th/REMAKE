# REMAKE Frontend Blueprint

Frontend design document for a service that manages fridge ingredients and reduces food waste with AI-tailored recipes and expiry-date alerts.
It is built as a mobile-first PWA and packaged as an Android app with PWABuilder (TWA).

Korean translation for developers: [`docs/developer/frontend-blueprint.md`](../docs/developer/frontend-blueprint.md)

> The routes in §4 follow the WBS v2 frontend URL table (2026-11-22, Figma-based). When planning or design changes, update this document first, then change the code.

## 1. Responsibilities

- **Frontend (Next.js)**: screens and user interaction only. No business logic or data storage.
- **Backend (Spring Boot)**: authentication, data, OCR and AI processing, in-app notifications.
- The frontend calls only the Spring REST API. Do not reimplement backend features with Next Route Handlers or Server Actions.
  Exception: the service-worker serving route (`app/serwist/[path]`).
- **`/api/` belongs to the backend.** In production the frontend and backend share one domain, and the proxy sends every path starting with `/api/` to Spring unchanged. Everything else goes to Next.js (see [`docs/agent/deployment.md`](../docs/agent/deployment.md)).
  - Do not create any route (page, Route Handler, etc.) whose URL starts with `/api/`. Requests to them never reach Next in production. Judge by the URL, not the folder: route groups do not appear in the URL, so `app/(group)/api/...` is also served at `/api/...`.
  - Frontend-only endpoints such as a future health check (`/healthz`) go outside `/api/`.
- **The frontend shows; the backend decides.** Calculations, validation, permissions, and limits (recipe generation quota, expiry status, food-cost totals, ...) come from the backend. The frontend displays the values it receives and validates only input format (required, length).
- **Server Components by default.** Keep pages and layouts as Server Components. Put `"use client"` only on the smallest leaf component that needs interaction or server data (TanStack Query, §5).

## 2. Tech stack

| Area            | Technology                                                                          | Why                                                                                                                                                                                      |
| --------------- | ----------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Framework       | Next.js (App Router) + TypeScript                                                   | Screens split per route; frontend handles screens only                                                                                                                                   |
| Package manager | pnpm                                                                                | Fast installs; lockfile keeps dependency versions identical for all 7 members                                                                                                            |
| Styling         | CSS Modules + CSS variables                                                         | Component-scoped styles without extra libraries; shared design tokens                                                                                                                    |
| Server data     | TanStack Query                                                                      | Caching and refetching server data                                                                                                                                                       |
| Client state    | Zustand                                                                             | Only the minimum, such as login info                                                                                                                                                     |
| URL state       | nuqs                                                                                | Filters, tabs, sort, and selected month live in the URL                                                                                                                                  |
| API integration | openapi-typescript + openapi-fetch                                                  | Types generated from Spring OpenAPI; type-safe calls                                                                                                                                     |
| Forms           | React Hook Form + Zod                                                               | Validation for onboarding, custom recipe input, ingredient edits                                                                                                                         |
| Dates           | dayjs                                                                               | Date display; meal-plan calendar                                                                                                                                                         |
| Icons           | @solar-icons/react (Solar)                                                          | Tree-shakable React components; work offline                                                                                                                                             |
| Font            | Pretendard (`pretendard` package, dynamic subset) + GmarketSans (`next/font/local`) | Pretendard: Korean UI font, downloads only the character groups on screen; the groups almost every screen uses are preloaded. GmarketSans: logo text only, subset to the logo characters |
| Scrollbar       | OverlayScrollbars                                                                   | Page scrollbar drawn over the content: takes no width (the centered layout does not move when a sheet locks scrolling) and shows only while scrolling                                    |
| PWA             | Serwist                                                                             | Service worker, offline screen. Notifications are in-app only (no device push)                                                                                                           |
| Store release   | PWABuilder (TWA)                                                                    | Generates the Android app package                                                                                                                                                        |
| Web deploy      | `output: "standalone"` + nginx                                                      | Node server running behind nginx                                                                                                                                                         |
| Code quality    | ESLint + Prettier, Husky + lint-staged                                              | Unified code style for 7 people; automatic checks before commit                                                                                                                          |
| API mocking     | MSW                                                                                 | Build screens against dummy responses before the backend is ready                                                                                                                        |

## 3. Folder structure

```
frontend/
├─ app/                  # routes. Each route folder colocates the code only it uses (see §3.1)
│  ├─ layout.tsx         # root layout, globals.css
│  ├─ fonts/             # GmarketSans files (woff2) + fonts.ts (next/font/local) + pretendard.css, pretendard-preload.ts (generated) + pretendard-fallback.css
│  ├─ page.tsx           # splash (/)
│  ├─ public/            # /public/... routes (before login, §4)
│  ├─ user/              # /user/... routes (after login, §4). Example: user/fridge/ingredients/ — see §3.1
│  ├─ admin/             # /admin/... routes (operator, ADMIN only, §4 "Admin screens")
│  ├─ sw.ts              # service worker (caching)
│  ├─ serwist/[path]/    # service-worker serving route
│  ├─ ~offline/          # offline fallback screen
│  ├─ dev/               # dev-only state previews of screen containers (§3.1 rule 7, 404 in production)
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
app/user/fridge/ingredients/
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
   └─ edit/
      └─ page.tsx
```

1. **Private folders.** Folders starting with `_` are Next.js private folders and are never routed. Put all non-route code of a route in `_components/`, `_hooks/`, `_lib/`. Never put `page.tsx` or `route.ts` inside them.
2. **Isolate by route.** A route may import colocated code from its own folder and from ancestor route folders (e.g. `app/user/fridge/ingredients/[id]/edit` may use `app/user/fridge/ingredients/_components`). Never import from a sibling or unrelated route.
3. **Isolate by component.** Each component has its own folder with its `.tsx`, `.module.css`, and any hook or util only it uses. Do not import another component's CSS module or internal files.
4. **Promote when shared.** When two unrelated routes need the same code, move it up — UI component → `components/<Name>/`, React hook → `hooks/`, query keys and query hooks → `lib/queries/<domain>.ts`, other functions → `lib/`. Shared code must not know about a specific page.
5. **Import paths.** Import colocated code with relative paths (`./_components/...`, `../_lib/...`). Import shared code with `@/` (`@/components/Button`). Never import from `@/app/...` (enforced by ESLint).
6. **Screen container.** Each screen has one top-level component named `<Screen>Container` (e.g. `LoginContainer`) in its route's `_components/`. `page.tsx` only exports `metadata` and renders that container. Do not name it `...Screen`. A state that is hard to reach by hand (a stored value, an open drawer) is a container prop with a default, so the dev preview (rule 7) can render it; the real route uses the defaults.
7. **Dev preview (`app/dev/`).** `/dev/p<N>` lists the states of one Figma area and `/dev/p<N>/<state>` renders one state by passing props to the screen containers. Every page under `app/dev/` calls `notFound()` unless `env.isDev`, so production returns 404. This folder is the only exception to rule 2: it may import route containers (and their `_lib` types and sample values) with relative paths. Never import `app/dev/` from anywhere else. Container defaults are what the real route shows before the API is wired: empty values, or a loading state for server data. Sample values (Figma copies) are passed only from `app/dev/`, never used as container defaults, because `dev` is deployed. In production `generateStaticParams` returns one placeholder param (an empty array is a build error with Cache Components), so no preview paths are built.

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

## 4. Screens / routes

Source: WBS v2 §3 "Frontend URL" (2026-11-22). Format `/{role}/{feature}/{resource}`. Roles: `public` (before login), `user` (after login), and `admin` (operator, ADMIN role only; added 11/26, see "Admin screens" below). The only exception is the splash at `/`.

- Sheets, modals, drawers, loading, and failure states have no URL of their own; they are states inside their screen.
- Not logged in: every `/user/...` route sends the user to `/public/auth/login`. Logged in: `/` and the login screen go to `/user/fridge/ingredients` (the index).
- Onboarding comes right after the first login and is required: there is no skip button. Household size is required to continue; allergies may be 0.

| Route                                        | Screen (Figma)                            | Notes                                                                        |
| -------------------------------------------- | ----------------------------------------- | ---------------------------------------------------------------------------- |
| `/`                                          | Splash                                    | Goes to login or the fridge depending on login state                         |
| `/public/auth/login`                         | p1 Login                                  | Kakao, Naver, Google. "최근 로그인" bubble. Terms drawer inside this screen  |
| `/public/auth/callback/[provider]`           | (no screen)                               | Social auth return URL; calls API 1 (login) or API 10 (Naver re-auth)        |
| `/user/onboarding/household`                 | p1.1 Household size                       | 1–50, digits only. "다음" disabled until entered                             |
| `/user/onboarding/allergies`                 | p1.2 Allergies                            | 19 chips, 0 allowed. Saves both onboarding values at once (API 4)            |
| `/user/setting/profile`                      | p1.3 Settings                             | Logout modal and account-deletion modal inside                               |
| `/user/setting/household`                    | p1.3.1 Household size                     |                                                                              |
| `/user/setting/excluded-ingredients`         | p1.3.2 Excluded ingredients               | 0 allowed                                                                    |
| `/user/setting/subscription`                 | p1.3.4 Subscription (before / subscribed) | Toss Payments card registration                                              |
| `/user/setting/subscription/billing-success` | (no screen)                               | Toss return URL; starts the subscription (API 59), then back to subscription |
| `/user/setting/subscription/billing-fail`    | (no screen)                               | Toss failure/cancel return URL; toast, then back to subscription             |
| `/user/setting/account-deletion`             | p1.3.3 Email verification code            | Kakao and Google only; Naver re-authenticates instead                        |
| `/user/fridge/ingredients`                   | p2 Fridge (index)                         | Status cards, location/category sheets, use/discard sheet                    |
| `/user/fridge/ingredients/new`               | p2.2 Add ingredient                       |                                                                              |
| `/user/fridge/ingredients/[id]/edit`         | p2.1 Edit ingredient                      | Value sheets, delete icon                                                    |
| `/user/receipt/receipts`                     | p3 Receipts                               | Month picker, weekly groups                                                  |
| `/user/receipt/receipts/[id]`                | p3.2 Receipt detail                       | Delete modal inside                                                          |
| `/user/receipt/scan`                         | p3.1 Camera / image upload                | Device camera app via file input; no in-app camera (domain-rules 3)          |
| `/user/receipt/scan/result`                  | p3.3 OCR result                           | Skeleton while waiting; OCR failure is a state of this URL                   |
| `/user/receipt/scan/items/new`               | p3.3.2 Add OCR item                       |                                                                              |
| `/user/receipt/scan/items/[index]/edit`      | p3.3.1 Edit OCR item                      | Edits stay in screen state until registration                                |
| `/user/receipt/patterns`                     | p3.4 My spending                          |                                                                              |
| `/user/receipt/patterns/discarded`           | p3.4.1 Discarded this month               | Opened from "N월 버린 식비" on p3.4; `?month=2026-11`                        |
| `/user/recipe/saved-recipes`                 | p4.1 Recipes (main)                       | Weekly TOP 3, saved list, quick-generate modal inside                        |
| `/user/recipe/custom`                        | p4.1.2 Custom recipe                      | Selections only, no free text                                                |
| `/user/recipe/results/[id]`                  | p4.1.3 Generation result                  | Loading, failure, and quota-used-up are states of this URL                   |
| `/user/recipe/recipes/[id]`                  | p4.1.1 Recipe detail                      |                                                                              |
| `/user/recipe/recipes/[id]/cook`             | p4.1.1.2 Cook deduction                   |                                                                              |
| `/user/recipe/shared-recipes`                | p4.1.4 Weekly popular recipes             | Ranking, search                                                              |
| `/user/recipe/shared-recipes/new`            | p4.1.1.1 Share recipe                     |                                                                              |
| `/user/recipe/shared-recipes/[id]`           | p4.1.4.1 Shared recipe detail             | Report sheet inside                                                          |
| `/user/meal-plan/calendar`                   | p5 Meal plan                              | Empty state, month calendar, day sheet, ⋮ sheet (delete / replace)           |
| `/user/meal-plan/meal-plans/new`             | p5 Meal-plan setup                        | Overlap modal and quota-used-up inside. Generation is async (§5)             |
| `/user/notification/notifications`           | p6 Notifications                          |                                                                              |
| `/user/cart/cart-items`                      | p7 Cart                                   | Amount sheet, delete mode                                                    |

Not placed yet (screen pending): this month's discarded-food amount, "recommend this recipe?".

### Admin screens (operator page, 11/26)

Source: Figma "운영자 페이지" (node 2005:4) and the 11/26 planning WBS. Desktop 1440. Admin frontend features are the side-menu groups `monitoring` and `operation`.

- Modals, toasts, empty states, and errors are states inside their screen. The a3 tab is in the URL so the dashboard anomaly "기능별 지표 보기" can open it directly.
- Every `/admin/...` route uses the common layout: side menu (with the pending report count, API 65), operator account, and "서비스로 돌아가기" (to `/`).
- The frontend checks `role` from `GET /user/profile` (API 5) first; the server returns 403 `FORBIDDEN` to non-ADMIN accounts. Either way, show a0 no permission. a0 loading and load failure are states inside each admin screen.

| Route                                       | Screen (Figma)              | Notes                                                                                           |
| ------------------------------------------- | --------------------------- | ----------------------------------------------------------------------------------------------- |
| `/admin`                                    | (no screen)                 | Goes to `/admin/monitoring/dashboard`                                                           |
| `/admin/monitoring/dashboard`               | a1 Dashboard                | Anomaly and no-data states inside. API 61                                                       |
| `/admin/monitoring/funnels`                 | a2 Funnel                   | API 62                                                                                          |
| `/admin/monitoring/metrics/[tab]`           | a3 Feature metrics (8 tabs) | `tab` = registration, consumption, ai, meal-plan-cart, shared, retention, cost, error. API 63   |
| `/admin/monitoring/events`                  | a6 Event log                | No-search-result state inside. API 64                                                           |
| `/admin/operation/reports`                  | a4 Report management        | Pending, done, and empty states inside. API 65                                                  |
| `/admin/operation/reports/[sharedRecipeId]` | a4.1 Report detail          | Three confirm modals, handled, toast, already handled, 4th-sanction warning inside. APIs 66, 67 |
| `/admin/operation/users`                    | a5 User management          | Detail panel, count-adjust modal, and 6 states inside. APIs 68, 69, 70                          |
| `/admin/operation/weekly-rewards`           | a7 Ranking and rewards      | No-reward and fewer-than-3 states inside. API 71                                                |

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
- **OCR and AI recipe generation** are one synchronous request each; the response carries the result. Show a loading screen while waiting (time limits: OCR 30 s, AI recipe 60 s). There is no job ID, polling, or streaming.
- **Meal-plan generation** is asynchronous: API 38 replies 202, the app goes straight to the fridge (index) screen with the toast "식단을 완성하면 알림을 보내드려요", and the result arrives as an in-app notification. There is no generating screen.
- **queryKey**s live next to their query hooks: the route's `_lib/queries.ts`, or `lib/queries/<domain>.ts` once shared. Example: `fridgeKeys.all`, `fridgeKeys.detail(id)`
- Text the user types that becomes part of a request (search keywords, `?q=`) goes through `useDebounce` (`hooks/useDebounce.ts`, 300 ms by default) before it enters the queryKey, so typing does not send a request per character. An emptied field may skip the wait and show the full list at once.
- **API client**: every call goes through `lib/api/client.ts`. Send cookies (`credentials: "include"`). Set timeouts with `AbortSignal.timeout`. During development use generous values: default 30 s, OCR 60 s, AI recipe generation 120 s. The frontend timeout must stay longer than the backend limit (above), so the screen receives the server's `OCR_TIMEOUT` / `AI_TIMEOUT` response instead of cutting the request first. Final values are decided near the end of development in #122.
- **Retries**: retry GET once. Never retry POST, PUT, PATCH, or DELETE. TanStack Query retries 3 times by default, so set queries to 1 and mutations to 0.
- **Caching**: data that rarely changes (categories, mart list, allergy list, recipes of the week) gets a long `staleTime` (minutes to 1 hour). Data that changes often (fridge ingredients, cart) gets a short `staleTime` and `invalidateQueries` after every change.
- **Errors**: keep the mapping from backend error `code` to user-facing text in one table, `lib/error-messages.ts` (§10 Errors).
- **401 handling** (decide by `code`, not by status; [`errors.md`](../docs/agent/spec/errors.md)):
  - `TOKEN_EXPIRED` (access cookie expired after 30 min): call `POST /api/v1/public/auth/refresh` (API 2) once, then resend the original request once. Show nothing to the user. If several requests expire together, refresh once and let the others wait for it.
  - `REFRESH_TOKEN_INVALID` (the refresh call failed: refresh token expired after 14 days, removed, or forged) and `UNAUTHORIZED` (no or forged access cookie): clear the TanStack Query cache, go to `/public/auth/login`, and return to the original URL after login.
  - Implement this once in `lib/api/client.ts`; screens never handle these codes.
- **Zustand** holds only truly global state (logged-in user summary, UI settings) and state whose props would otherwise pass down more than 3 levels.
- **URL state**: filters, tabs, sort, and selected month go in the URL with nuqs. Bottom sheets, modals, and drawers are local state, never URL state (back button: §7.1).
- **Environment variables**: only values the browser may see get the `NEXT_PUBLIC_` prefix. Secrets stay on the server.

## 6. Styling rules

- No Tailwind or CSS-in-JS libraries. Use only **CSS Modules (`*.module.css`) + CSS variables in `globals.css`**.
- Import icons from the per-style paths of `@solar-icons/react`. Example: `import { HomeIcon } from "@solar-icons/react/linear"`. Default size, color, and stroke width come from the `--solar-*` variables in `globals.css`.
- Design tokens follow [`docs/agent/design-tokens.md`](../docs/agent/design-tokens.md): the variables and styles of the Figma design system (`0. 기초`) copied with their names; CSS variable names equal the Figma Dev Mode code names (`var(--color-text-strong)`, `var(--space-16)`, `font: var(--font-content)`). `design/tokens/build_tokens.py` generates them; the result is copied to `app/tokens.css`, which `globals.css` imports. Never edit `app/tokens.css` by hand. If a Figma value has a variable or style, use the token with the same name. If it has none, use the Figma frame value as is.
- Mobile first. Design for a 360px width. The default max layout width is `--layout-max-width` (480px) and the side gutter is `--layout-gutter`. Wider screens show the 480px container centered: the root `app/layout.tsx` wraps every screen in one container (`app/layout.module.css`), so screens do not set the max width themselves. The container has the `bg/surface` background; the area outside it is gray (`--layout-outside-bg`, #ececec, on `body`). The container uses `overflow: clip` so shadows and other overflow do not show outside it (not `hidden`, which would create a scroll container and break the `position: sticky` headers). Elements outside that container — `position: fixed` bars and portals to `body` (bottom bar, sheets, modals, drawers) — set `max-width: var(--layout-max-width)` themselves, including the dim behind sheets and modals, so overlays never cover the gray area outside the container.
- Light theme only; no dark mode.
- Units: rem (1rem = 16px; never change the root font size). Convert Figma px by dividing by 16 (10px → 0.625rem). Use px only for lines such as borders.
- Font: use woff2 files only.
  - Pretendard (body text): use the dynamic subset files of the `pretendard` package (each weight is split into character groups with `unicode-range`, so the browser downloads only the groups that appear on screen; the full file is about 770 KB per weight). `app/fonts/pretendard.css` is generated by `pnpm gen:fonts` (`scripts/gen-pretendard-css.mjs`) and imported in `app/layout.tsx`. Never edit it by hand. Weights 400/500/700/800 only (the ones in use; 400 is the `Remake/content-regular` and `Remake/error` text styles, 800 is the store badge). To add a weight, edit `WEIGHTS` in the script and run it again. Font family name `Pretendard`, used by `--font-family-base` in `tokens.css`.
    - Preload: the character groups that almost every screen uses (`PRELOAD` in the script: group 91 for 400/500/700, groups 89 and 90 for 500/700; about 98 KB) are left out of `pretendard.css` and written to `app/fonts/pretendard-preload.ts` (generated) as `next/font/local` calls with `font-family: Pretendard` and the group's `unicode-range`. `app/layout.tsx` imports it, so every route gets `<link rel="preload">` for them and they start downloading before the first layout. Choose groups by measuring which groups the screens actually load; preloading a group most screens do not use wastes bandwidth.
    - Fallback size: `app/fonts/pretendard-fallback.css` defines `Pretendard Fallback Malgun` (Windows) and `Pretendard Fallback Noto` (Android) with `local()` sources and `size-adjust` / `ascent-override` / `descent-override`, split by character range (Hangul, space, Latin and symbols, digits). `--font-pretendard` in `globals.css` puts them right after `Pretendard` in `--font-family-base`, so text keeps nearly the same width while Pretendard downloads and does not rewrap when it swaps. The values are measured width ratios against Pretendard (see the file header). The iOS/macOS font (Apple SD Gothic Neo) is not adjusted.
  - GmarketSans (logo text only): loaded with `next/font/local` in `app/fonts/fonts.ts` and attached to `<html>` in `app/layout.tsx`. Weights 300/500/700, `preload: false`. Use `font-family: var(--font-family-logo)` (defined in `globals.css`). Do not use it for other text. The logo text is only "REMAKE" (headers, login) and "리메이크" (splash), so the files keep only U+0020-007E plus those four Hangul syllables (U+B9AC, U+BA54, U+C774, U+D06C; about 18 KB each instead of about 0.5 MB), and the `@font-face` has that `unicode-range`. The original is SIL OFL 1.1 with the Reserved Font Name "Gmarket Sans Font", so the subset files (`GmarketSans-*.logo.woff2`) use the internal font name "REMAKE Logo" and keep the copyright and license records. To use other characters in the logo font, subset the original again with fontTools (`pyftsubset`).
- Full-screen height uses `100dvh`. Bottom-fixed elements add `env(safe-area-inset-bottom)`.
- Mostly flat. Apply glass effects only to elements that have them in Figma.
- Text that wraps to two or more lines uses a line height of 150% (`line-height: 1.5` after the `font` token), even when the Figma text style says 140%. Single-line text keeps the token line height.
- Use interaction colors (the primary blue `--color-text-primary` / `--color-brand-primary` and link styles) only on buttons and links. Do not use them for plain text, counts, or decoration, so that blue always means "this can be tapped".
- Never use a placeholder in place of a label. Every input has a visible label (or a `<label>` the screen reader reads); a placeholder only shows an example value.
- Text inputs (`TextField`, `SearchField`) show a clear button (✕, `components/InputClearButton`) on the right while they have a value (Figma 5780-4568, 5991-7263). It empties the input with a normal `input` event, so both `useState` values and React Hook Form `register` fields update through `onChange`. It is on by default; pass `clearable={false}` only when the design has no clear button. When the label sits outside the field, pass `clearLabel` (e.g. "레시피 제목 지우기").
- Give every choice group a default option (radio, segmented control, chips, select sheet, wheel). Preselect the most common or safest option instead of starting with nothing selected. Exception: leave a choice empty when a wrong preselection would be saved silently if the user skips it (e.g. ingredient category, which drives the unit lookup, the category image and filter, and event `category_code`). The save button stays disabled until the user picks one.
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

## 7. PWA · TWA

- The service worker runs only with `pnpm build && pnpm start`. It is off in development mode and while MSW is in use (avoids a scope conflict).
- The service worker (`/serwist/sw.js`) must be prerendered at build time (`●` in the `pnpm build` output). The standalone production server has no esbuild or `app/sw.ts`, so a request-time `ƒ` route returns 500.
  - Because `cacheComponents` is on, route segment configs (`dynamic`, `revalidate`, `dynamicParams`) are ignored or rejected. Do not re-export them from `createSerwistRoute`.
  - `app/serwist/[path]/route.ts` bundles the worker inside a `"use cache"` function, so the route stays prerendered no matter how long esbuild takes. Apply the same pattern to any `GET` Route Handler that must be static.
  - That function calls `cacheLife("max")`. `sw.js` only changes per build; with the default profile (revalidate 15 minutes) the production server would retry bundling in the background, fail without esbuild, and log an error on every retry. The old `sw.js` keeps being served either way.
  - As a result `sw.js` is sent with `Cache-Control: s-maxage=2592000` (30 days). Browsers ignore `s-maxage`, but a shared cache (CDN or proxy cache) in front of the app would keep an old service worker after a deploy. Exclude `/serwist/` from any shared cache.
  - After changing the service worker or this route, check that `pnpm build` lists `/serwist/sw.js` as `●` and that `.next/server/app/serwist/sw.js.body` exists.
- No device push. Notifications are in-app only: the top banner on every tab screen (API 43) and the notification screen (API 44). The Web Push handlers in `app/sw.ts` and `NEXT_PUBLIC_VAPID_PUBLIC_KEY` are leftovers to remove.
- Needed before the TWA release: 192/512 PNG icons, `public/.well-known/assetlinks.json` (app signing SHA-256), an HTTPS domain.

### 7.1 Back button closes sheets

- Bottom sheets, modals, and drawers never touch history: no URL change and no `history.pushState` (their open state is local).
- Handle closing in one shared hook, `useBackToClose(isOpen, onClose)` in `hooks/` (used by `useDialogLayer`). Components never handle `popstate` themselves.
- Chromium browsers (including the TWA): receive the Android back button and Esc as a close request with `CloseWatcher`, and close only the open sheet without navigating. `CloseWatcher` does not use history.
- Browsers without `CloseWatcher` (Safari, Firefox): Esc closes the sheet. The back action (iOS swipe back) navigates to the previous screen as usual.
- When a screen with an open sheet is hidden (navigation from inside the sheet, or back), the sheet closes. With `cacheComponents` the previous screen keeps its state, so without this the sheet would still be open when the user comes back.
- While a sheet or modal is open, the screen behind it does not scroll. `useDialogLayer` locks it with `setPageScrollLocked` in `lib/page-scrollbar.ts`, which changes the overflow option of the OverlayScrollbars instance on `body` (created in `AppProviders`). That scrollbar takes no width, so the centered layout does not shift when the lock hides it. Do not lock scrolling by setting `overflow: hidden` on `html` or `body` yourself.
- `onClose` only sets the closed state; it is also called when the screen is hidden. Do not put other actions (such as recording a cancel event) in it; put them in the cancel button's handler. To navigate after confirming, close the sheet first, then navigate.
- When sheets are stacked, close only the topmost one.

## 8. Development flow

1. If a backend API does not exist yet, write dummy responses in `mocks/handlers.ts` first and build the screen against them.
2. When the backend API is ready, regenerate types with `pnpm gen:api` and align the MSW handlers with the real spec.
3. Before a PR, `pnpm lint && pnpm typecheck && pnpm build` must pass.

TypeScript runs in `strict` mode, and `any` is not allowed (enforced by ESLint).

## 9. Open decisions

- [x] Authentication method and token refresh flow — **decided in [#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42) option A**: access (30 min) and refresh (14 d) are both JWT in HttpOnly cookies (`Secure; SameSite=Lax`). Login/refresh responses do not include `accessToken`. Call `/api/...` with `credentials: 'include'`; do not use `Authorization: Bearer` or store tokens in JS-readable storage. Locally (different origin) the backend must allow CORS with credentials.
- [ ] Shared component list (based on the design mockups)
- [x] Frontend CI — `.github/workflows/frontend-ci.yml` runs `pnpm lint` · `pnpm typecheck` · `pnpm build` on PRs to `dev`/`main` ([`docs/agent/ci.md`](../docs/agent/ci.md)).
- [x] Deployment environment — `frontend-blue`/`frontend-green` containers in `compose.prod.yml` behind nginx (`deploy/nginx/`), deployed by `.github/workflows/frontend-cd.yml` ([`docs/agent/deployment.md`](../docs/agent/deployment.md)).

## 10. UI rules from the product policy

Source: [`docs/agent/spec/domain-rules.md`](../docs/agent/spec/domain-rules.md). Screens follow Figma.

- **No free-text input for AI.** Recipe customization (ingredients, taste, time, tools, method, cuisine, excluded allergens) and meal-plan settings (start date, purpose, meals, servings) are selections only. Send IDs and enums.
- **Expiry state**: show the `status` and `dDay` the server returns (DANGER = expired or D-DAY, SOON = D-1 to D-3, OK = later). Do not recompute them with the device clock. Exception: the OCR result before registration (p3.3) has no server value because it is not stored and the user edits the expiry; compute it with the KST date (`getExpiry` in `app/user/receipt/scan/_lib/scan.ts`).
- **Today**: when a screen needs today's date (month picker, blocking future dates, D-N), use `todayInKst()` from `lib/dayjs.ts`, not `dayjs()`. The server counts days in KST, so a device in another time zone would be off by a day near midnight.
- **Labels**: do not add an "AI가 생성한 레시피" label or a "참고용 식단" label (policy v4: neither is in Figma). The AI-results disclaimer lives only in the terms of service, Article 5 (11/24 terms decision).
- **Receipt flow**: scan → loading screen (synchronous wait) → result and confirm screen → save. Nothing is saved until the user confirms. Leaving the result screen discards the OCR result and sends `receipt_result_abandoned`. A confirmed receipt cannot be edited, only deleted; fix ingredient data later in the fridge.
- **Quantity input**: stepper steps are g and ml 50, kg and L 0.5, EA/PACK/BUNCH/PIECE/SHEET 1. Typed input accepts numbers and decimals only, no fractions. The fridge uses 9 units (G, KG, ML, L, EA, PACK, BUNCH, PIECE, SHEET); recipes use all 14, and recipe amounts may be shown as 1/2, 1/3, or 1/4.
- **Sharing**: exactly one photo (required) and a title (required); the share button is enabled only when both are present. No confirm modal. The same recipe can be shared once per week. Never show author information.
- **Meal plan**: monthly calendar; tapping a meal row opens the recipe detail. No manual add to an empty slot ("+" opens the add sheet, which leads to setup). The slot ⋮ sheet offers "식단 삭제" (deletes that slot) and "레시피 교체" (new recipe, uses 1 daily recipe count); today or later only, past days show the `PAST_DATE_NOT_ALLOWED` toast. Show remaining counts from `GET /api/v1/user/usage-quotas`.
- **Cart**: one row per item with its source recipe name (the same ingredient from different recipes stays separate). Items are added automatically only (meal-plan shortages, saving my recipe with supplemented ingredients, saving a shared recipe). Users can change amounts, delete selected items, and move selected items to the fridge. No checkout or external links. API 45 returns a non-null positive amount for each persisted row. Unsaving a recipe or deleting a meal assignment keeps cart rows and edited amounts; deleting selected cart rows does not change recipe saves or meal assignments. The nullable initial-source slot FK is server metadata, not a new response field. Receipt registration deducts only explicitly selected rows and returns the count deleted at zero. All three add paths exclude `amount=null`, PINCH and TO_TASTE ingredients from the cart, while retaining them in recipes; never substitute an arbitrary quantity. This was [confirmed by the cart owner in PR #216](https://github.com/likelion-backendschool-9th/REMAKE/pull/216#issuecomment-7123595032); do not infer a nullable API amount from preview data. The preview may keep its defensive null display.
- **Notifications**: in-app only (no device push). Types `EXPIRY_SOON`, `EXPIRED`, `MEAL_PLAN_COMPLETED`, `MEAL_PLAN_FAILED`. The top banner on every tab screen shows the latest message and the unread count. Tapping an expiry item does nothing; `MEAL_PLAN_COMPLETED` opens the meal plan at the plan's start date; `MEAL_PLAN_FAILED` opens the meal-plan setup.
- **Premium**: real subscription with Toss Payments card auto-billing (`/user/setting/subscription`, API 57–60). Never send or store the Toss secret key or the billing key on the frontend.
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
