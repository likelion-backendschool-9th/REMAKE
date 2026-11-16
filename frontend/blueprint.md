# REMAKE Frontend Blueprint

Frontend design document for a service that manages fridge ingredients and reduces food waste with AI-tailored recipes and expiry-date alerts.
It is built as a mobile-first PWA and packaged as an Android app with PWABuilder (TWA).

Korean translation for developers: [`docs/developer/frontend-blueprint.md`](../docs/developer/frontend-blueprint.md)

> The screen list and routes are a draft. When planning and design are finalized, update this document first, then change the code.

## 1. Responsibilities

- **Frontend (Next.js)**: screens and user interaction only. No business logic or data storage.
- **Backend (Spring Boot)**: authentication, data, OCR and AI processing, push delivery.
- The frontend calls only the Spring REST API. Do not reimplement backend features with Next Route Handlers or Server Actions.
  Exception: the service-worker serving route (`src/app/serwist/[path]`).

## 2. Tech stack

| Area            | Technology                             | Why                                                                           |
| --------------- | -------------------------------------- | ----------------------------------------------------------------------------- |
| Framework       | Next.js (App Router) + TypeScript      | Screens split per route; frontend handles screens only                        |
| Package manager | pnpm                                   | Fast installs; lockfile keeps dependency versions identical for all 7 members |
| Styling         | CSS Modules + CSS variables            | Component-scoped styles without extra libraries; shared design tokens         |
| Server data     | TanStack Query                         | Caching; polling the progress of OCR and meal-plan jobs                       |
| Client state    | Zustand                                | Only the minimum, such as login info                                          |
| API integration | openapi-typescript + openapi-fetch     | Types generated from Spring OpenAPI; type-safe calls                          |
| Forms           | React Hook Form + Zod                  | Validation for onboarding, custom recipe input, ingredient edits              |
| Dates           | dayjs                                  | Expiry-date calculation; weekly meal-plan calendar                            |
| Icons           | @solar-icons/react (Solar)             | Tree-shakable React components; work offline                                  |
| PWA & push      | Serwist + Web Push                     | Service worker, offline screen, expiry and repurchase alerts                  |
| Store release   | PWABuilder (TWA)                       | Generates the Android app package                                             |
| Web deploy      | `output: "standalone"` + nginx         | Node server running behind nginx                                              |
| Code quality    | ESLint + Prettier, Husky + lint-staged | Unified code style for 7 people; automatic checks before commit               |
| API mocking     | MSW                                    | Build screens against dummy responses before the backend is ready             |

## 3. Folder structure

```
frontend/
├─ public/                  # static files, mockServiceWorker.js, (planned) .well-known/assetlinks.json, icons/
└─ src/
   ├─ app/                  # routes (screens). Keep page.tsx thin; compose components from features
   │  ├─ sw.ts              # service worker (caching, push handling)
   │  ├─ serwist/[path]/    # service-worker serving route
   │  ├─ ~offline/          # offline fallback screen
   │  └─ manifest.ts        # PWA manifest
   ├─ components/           # shared UI used by several features (Button, Input, BottomSheet ...)
   ├─ features/             # code grouped by feature (see rules below)
   ├─ hooks/                # shared hooks not tied to a feature
   ├─ lib/
   │  ├─ api/               # client.ts (openapi-fetch), schema.d.ts (generated)
   │  ├─ query-client.ts
   │  ├─ dayjs.ts           # dayjs with ko locale and plugins configured
   │  └─ env.ts             # the only place that reads environment variables
   ├─ mocks/                # MSW handlers
   ├─ providers/            # global providers (Serwist, TanStack Query, MSW)
   └─ stores/               # Zustand stores
```

### Feature folder rules

One feature = one folder. Do not import another feature's internal files; use only what it exposes through `index.ts`.

```
features/fridge/
├─ api.ts            # API call functions + queryKey + useQuery/useMutation hooks
├─ schema.ts         # Zod schemas (form validation)
├─ components/       # components for this feature only (*.tsx + *.module.css)
├─ utils.ts          # pure functions for this feature only (e.g. expiry D-day calculation)
└─ index.ts          # public API of the feature
```

Planned feature folders: `auth`, `onboarding`, `fridge`, `receipt` (OCR), `recipe`, `meal-plan`, `notification`, `mypage`

## 4. Screens / routes (draft)

| Route            | Screen                   | Main features                                                      |
| ---------------- | ------------------------ | ------------------------------------------------------------------ |
| `/`              | Home                     | Ingredients near expiry, recommended recipe summary                |
| `/login`         | Login                    | Social login                                                       |
| `/onboarding`    | Onboarding               | Household, eating habits and allergy input (RHF + Zod)             |
| `/fridge`        | My fridge                | Ingredient list, sort by storage location / expiry date            |
| `/fridge/[id]`   | Ingredient detail & edit | Edit quantity and expiry date                                      |
| `/fridge/add`    | Add ingredient           | Choose manual entry or receipt photo                               |
| `/receipt`       | Receipt OCR              | Upload → poll processing status → review and edit recognized items |
| `/recipes`       | Recipe recommendations   | AI recommendations based on owned ingredients                      |
| `/recipes/[id]`  | Recipe detail            | Ingredients, cooking steps, missing ingredients                    |
| `/recipes/new`   | Custom recipe            | Write a recipe manually (RHF + Zod)                                |
| `/meal-plan`     | Meal plan                | Weekly calendar, poll AI meal-plan generation status               |
| `/notifications` | Notifications            | Expiry and repurchase alert list, push permission settings         |
| `/mypage`        | My page                  | Profile, settings                                                  |

## 5. Data flow

```
Spring API ──(OpenAPI spec)──> pnpm gen:api ──> src/lib/api/schema.d.ts
                                                     │
Screen (page.tsx) ─> features/*/components ─> features/*/api.ts (useQuery) ─> lib/api/client.ts ─> Spring API
                                                     │
                              during development NEXT_PUBLIC_API_MOCKING=true ─> MSW dummy responses
```

- **Server data** is managed entirely with TanStack Query. Do not copy server data into Zustand.
- **Long-running jobs** (OCR, AI meal-plan generation): the backend returns a job ID and the frontend polls its status with `refetchInterval`. Stop polling when the job completes.
- **queryKey**s live in each feature's `api.ts`. Example: `fridgeKeys.all`, `fridgeKeys.detail(id)`

## 6. Styling rules

- No Tailwind or CSS-in-JS libraries. Use only **CSS Modules (`*.module.css`) + CSS variables in `globals.css`**.
- Import icons from the per-style paths of `@solar-icons/react`. Example: `import { HomeIcon } from "@solar-icons/react/linear"`. Default size, color, and stroke width come from the `--solar-*` variables in `globals.css`.
- Use the tokens in `globals.css` (`--color-*`, `--space-*`, `--font-size-*`, `--radius-*`) for color, spacing, font size, and radius. Do not introduce new hard-coded values.
- Mobile first. The default max layout width is `--layout-max-width` (480px) and the side gutter is `--layout-gutter`.
- Put a style file next to its component with the same name. Example: `IngredientCard.tsx` + `IngredientCard.module.css`

## 7. PWA · push · TWA

- The service worker runs only with `pnpm build && pnpm start`. It is off in development mode and while MSW is in use (avoids a scope conflict).
- Push: the frontend creates a subscription with `NEXT_PUBLIC_VAPID_PUBLIC_KEY` and stores it on the backend; the backend sends notifications. Receiving and click handling live in `src/app/sw.ts`.
- Needed before the TWA release: 192/512 PNG icons, `public/.well-known/assetlinks.json` (app signing SHA-256), an HTTPS domain.

## 8. Development flow

1. If a backend API does not exist yet, write dummy responses in `src/mocks/handlers.ts` first and build the screen against them.
2. When the backend API is ready, regenerate types with `pnpm gen:api` and align the MSW handlers with the real spec.
3. Before a PR, `pnpm lint && pnpm typecheck && pnpm build` must pass.

## 9. Open decisions

- [ ] Authentication method (cookie session / JWT) and token refresh flow
- [ ] Shared component list and final design tokens (based on the design mockups)
- [ ] Frontend CI (GitHub Actions: lint · typecheck · build)
- [ ] Deployment environment (nginx + Node container setup)
