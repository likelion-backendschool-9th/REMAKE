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
│  └─ utils.ts                  # pure functions (e.g. expiry D-day calculation)
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
Spring API ──(OpenAPI spec)──> pnpm gen:api ──> lib/api/schema.d.ts
                                                     │
Screen (page.tsx) ─> _components ─> _lib/queries.ts or @/lib/queries (useQuery) ─> lib/api/client.ts ─> Spring API
                                                     │
                              during development NEXT_PUBLIC_API_MOCKING=true ─> MSW dummy responses
```

- **Server data** is managed entirely with TanStack Query. Do not copy server data into Zustand.
- **Long-running jobs** (OCR, AI meal-plan generation): the backend returns a job ID and the frontend polls its status with `refetchInterval`. Stop polling when the job completes.
- **queryKey**s live next to their query hooks: the route's `_lib/queries.ts`, or `lib/queries/<domain>.ts` once shared. Example: `fridgeKeys.all`, `fridgeKeys.detail(id)`

## 6. Styling rules

- No Tailwind or CSS-in-JS libraries. Use only **CSS Modules (`*.module.css`) + CSS variables in `globals.css`**.
- Import icons from the per-style paths of `@solar-icons/react`. Example: `import { HomeIcon } from "@solar-icons/react/linear"`. Default size, color, and stroke width come from the `--solar-*` variables in `globals.css`.
- Use the tokens in `globals.css` (`--color-*`, `--space-*`, `--font-size-*`, `--radius-*`) for color, spacing, font size, and radius. Do not introduce new hard-coded values.
- Mobile first. The default max layout width is `--layout-max-width` (480px) and the side gutter is `--layout-gutter`.
- Put a style file next to its component with the same name. Example: `IngredientCard.tsx` + `IngredientCard.module.css`

## 7. PWA · push · TWA

- The service worker runs only with `pnpm build && pnpm start`. It is off in development mode and while MSW is in use (avoids a scope conflict).
- Push: the frontend creates a subscription with `NEXT_PUBLIC_VAPID_PUBLIC_KEY` and stores it on the backend; the backend sends notifications. Receiving and click handling live in `app/sw.ts`.
- Needed before the TWA release: 192/512 PNG icons, `public/.well-known/assetlinks.json` (app signing SHA-256), an HTTPS domain.

## 8. Development flow

1. If a backend API does not exist yet, write dummy responses in `mocks/handlers.ts` first and build the screen against them.
2. When the backend API is ready, regenerate types with `pnpm gen:api` and align the MSW handlers with the real spec.
3. Before a PR, `pnpm lint && pnpm typecheck && pnpm build` must pass.

## 9. Open decisions

- [ ] Authentication method (cookie session / JWT) and token refresh flow
- [ ] Shared component list and final design tokens (based on the design mockups)
- [ ] Frontend CI (GitHub Actions: lint · typecheck · build)
- [ ] Deployment environment (nginx + Node container setup)
