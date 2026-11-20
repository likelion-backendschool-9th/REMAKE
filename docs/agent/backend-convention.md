# Backend Code Convention

> Korean version for developers: [`docs/developer/backend-convention.md`](../developer/backend-convention.md)

## Code style

- Use camelCase in Java code and JSON, snake_case in the DB.

## Folder structure

Use hexagonal architecture. Group code by feature, and give every feature folder the same shape.

```
com.remake
├── user / auth / ingredient / receipt / recipe / mealplan / cart / share / quota / notification / event / admin
│   ├── domain                  Core objects and rules (no Spring, no JPA)
│   ├── application
│   │   ├── port
│   │   │   ├── in              UseCase interfaces (inbound)
│   │   │   └── out             Port interfaces (outbound)
│   │   └── service             UseCase implementations; throws exceptions
│   └── adapter
│       ├── in
│       │   ├── web             Controllers, Request/Response DTOs
│       │   └── scheduler       Scheduled jobs (only where needed)
│       └── out
│           ├── persistence     JPA entities, repositories, Port implementations
│           └── ai / ocr / oauth / filestorage / push / mail  External service calls (only where needed)
│
└── global                      Shared by all features
    ├── error                   ErrorCode, BusinessException, GlobalExceptionHandler
    ├── config                  Swagger, JPA, CORS, Clock, shared external service config
    ├── security                JWT filter, Security config
    └── common                  Shared classes such as BaseCreatedEntity, BaseTimeEntity, the duplicate-request filter
```

Feature folders and tables (ERD v3):

| Feature folder | Tables |
|---|---|
| `user` | users, user_excluded_ingredients, account_deletion_requests |
| `auth` | refresh_tokens |
| `ingredient` | ingredient_categories, ingredient_catalog, ingredients, ingredient_logs |
| `receipt` | receipts, receipt_items |
| `recipe` | recipes, recipe_ingredients, recipe_steps, recipe_catalog_items, saved_recipes, recipe_cook_logs |
| `mealplan` | meal_plans, meal_plan_slots, meal_plan_slot_ingredients |
| `cart` | cart_items |
| `share` | shared_recipes, shared_recipe_photos, shared_recipe_reports, shared_recipe_weekly_ranks |
| `quota` | usage_logs, generation_quota_grants |
| `notification` | notifications, push_subscriptions |
| `event` | events, event_daily_stats |
| `admin` | admin_audit_logs. Admin screen actions (handling reports, adjusting quota, suspending accounts) call each feature's UseCase |
| `global/common` | idempotency_keys |

Rules:

- Dependencies point one way: `adapter → application → domain`. Do not use Spring or JPA annotations in `domain`.
- Controllers call only `UseCase` interfaces.
- Services call only their own feature's `Port` interfaces and other features' `UseCase` interfaces.
- Only JPA entities extend the shared base classes. Domain objects do not.
  - Tables whose values are updated (users, ingredients, receipts, receipt_items, meal_plan_slots, cart_items, shared_recipes) extend `BaseTimeEntity` (`created_at` + `updated_at`).
  - All other tables extend `BaseCreatedEntity` (`created_at`).
  - Join tables, reference data, daily aggregates, and `events` extend neither.

### Using another feature's data

- When you need another feature's data, call that feature's `UseCase` (port/in). Do not use another feature's `Port` (port/out), adapters, or repositories.
- Reference another feature's data by ID only, such as `Long ingredientId`, in both domain objects and JPA entities. Do not create JPA associations (`@ManyToOne`, etc.) to another feature's entities. Keep the DB foreign keys.
- Within one feature, compose objects and use JPA associations freely. Example: `Recipe` and `RecipeStep`.

### Folders created only where needed

When a feature needs a connection other than `adapter/in/web` and `adapter/out/persistence`, the feature owner creates the folder when needed, using the names below.

| Connection | Location | Example |
|---|---|---|
| Jobs that run automatically on a schedule | `adapter/in/scheduler` | Creating expiry alerts, receipt cleanup, weekly recipe rewards |
| AI (recipe and meal plan generation, category estimation) | `adapter/out/ai` | `recipe/adapter/out/ai` |
| OCR (reading receipt text) | `adapter/out/ocr` | `receipt/adapter/out/ocr` |
| Social login | `adapter/out/oauth` | `auth/adapter/out/oauth` |
| File storage (receipt photos, cooking photos) | `adapter/out/filestorage` | `receipt/adapter/out/filestorage` |
| Web push delivery | `adapter/out/push` | `notification/adapter/out/push` |
| Email delivery (account deletion code) | `adapter/out/mail` | `user/adapter/out/mail` |

- Schedulers call only `UseCase` interfaces, like controllers. Running a job twice must give the same result; block concurrent duplicates with a DB unique key.
- Services never call external services directly; they call `port/out` interfaces only. External call code implements those interfaces in the adapter folders above.
- Keep external service config shared by several features (API keys, HTTP clients) in one place in `global/config`.
- If you need a connection not in the table, choose a name and add it to this table first.
- The file storage folder is `filestorage`, not `storage`. `storage` means an ingredient's storage location (fridge, freezer, pantry), so keep the two apart.

### Class name examples (ingredient feature)

| Location | Name |
|---|---|
| domain | `Ingredient` |
| port/in | `CreateIngredientUseCase`, `GetIngredientsUseCase` |
| port/out | `LoadIngredientPort`, `SaveIngredientPort` |
| service | `IngredientService` |
| adapter/in/web | `IngredientController`, `IngredientCreateRequest`, `IngredientResponse` |
| adapter/out/persistence | `IngredientJpaEntity`, `IngredientJpaRepository`, `IngredientPersistenceAdapter` |

## DB and entities

Table structure follows the [ERD and table definition](erd.md).

- **Create and change tables only with Flyway.** When you add or change an entity, add `backend/src/main/resources/db/migration/V<number>__<description>.sql` in the same PR. JPA runs `validate` (check only) both locally and in production, so the app does not start if the SQL is missing.
  - Use the number after dev's last migration. If another PR merges first with the same number, raise your file's number before merging.
  - Never edit a migration file already merged into dev. Add a new numbered file instead.
  - During a deploy, the old and new app versions share the DB for a while. Split changes that break the old version, such as renaming or dropping a column, into steps (see the [deployment doc](deployment.md)).
- **Types**: quantities `numeric(10,3)` ↔ `BigDecimal`, money `bigint` ↔ `Long` (won), timestamps `timestamp` ↔ `LocalDateTime` (Korea time), JSON `jsonb`.
- **Fixed value lists** (statuses, kinds, units) are Java enums stored with `@Enumerated(EnumType.STRING)`. Never store the ordinal (`ORDINAL`).
- **Read the current time only from the injected `Clock`.** Do not use argument-less `now()` such as `LocalDateTime.now()`, a column `DEFAULT now()`, or `now()` inside a query. The base classes set created and updated times through JPA auditing.

## Terms

Use the same word in code, DB, and API.

| UI term (Korean) | Code word |
|---|---|
| 회원 (member) | `user` |
| 재료 (ingredient) | `ingredient` |
| 재료 사전 (ingredient dictionary) | `ingredientCatalog` |
| 제외 재료(알레르기) (excluded ingredient, allergy) | `excludedIngredient` |
| 소비기한 (expiration date) | `expirationDate` |
| 보관 위치 (storage location) | `storage` |
| 수량 / 단위 (quantity / unit) | `amount` / `unit` |
| 영수증 (receipt) | `receipt` |
| 레시피 (recipe) | `recipe` |
| 요리했어요(요리 기록) (cooked, cook log) | `cook` / `cookLog` |
| 식단표 / 식단 칸 (meal plan / meal plan slot) | `mealPlan` / `mealPlanSlot` |
| 장바구니 (cart) | `cart` |
| 공유 레시피 (shared recipe) | `sharedRecipe` |
| 생성 횟수 (generation quota) | `quota` |
| 알림 (notification) | `notification` |

## API paths

- Every controller path starts with `/api/v1/{caller}/{resource}`, written directly on the controller. Example: `@RequestMapping("/api/v1/user/ingredients")`
  - `public`: APIs called before login (social login, token refresh)
  - `user`: a logged-in user. Most APIs. They handle only the token owner's data
  - `admin`: an admin (role=ADMIN)
- Write resources as plural kebab-case (`saved-recipes`, `cart-items`). Put actions as a verb after the resource (`/recipes/generate`, `/receipts/{id}/confirm`).
- In production the proxy sends only requests starting with `/api/` to the backend and everything else to the frontend. Breaking this rule works locally but returns 404 in production.
- Do not add the prefix with `server.servlet.context-path` or a global path setting (`addPathPrefix`). `context-path` also moves the health check (`/actuator/health/readiness`) and breaks deployment; a global prefix turns a controller that also writes `/api` into `/api/api/...`.
- Use full paths in Security `requestMatchers` too.
  - `/api/v1/public/**`, `/actuator/health/**`: allow without authentication. Blocking the health check breaks deployment.
  - `/api/v1/admin/**`: ADMIN only. 403 without the role.
  - All other `/api/v1/**`: login required.
- In local development the frontend (`http://localhost:3000`) calls the backend (`http://localhost:8080`) directly, so allow it in CORS. Production uses one domain and needs no CORS.
- If you need a path outside `/api/` (WebSocket, file download, etc.), tell the deployment owner before building it. The proxy needs a new rule.

## Responses and errors

On success, return the Response DTO as-is without a wrapper. Distinguish success and failure by HTTP status code: 201 for creation, 204 for deletion without a body, 202 for accepted-now-processed-later (event collection), 200 for other successes.

Paginate lists in this shape (`?page=0&size=20`):

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "hasNext": false }
```

On failure, every API returns this shape. Include `fieldErrors` (`{ field, message }` items) only for input validation failures (`INVALID_INPUT`). Include `details` only when the screen needs extra values, such as the remaining count and reset time for `GENERATION_LIMIT_EXCEEDED` (`"details": { "remaining": 0, "resetsAt": "2026-11-21T00:00:00+09:00" }`). The error code list is [`spec/errors.md`](spec/errors.md).

```json
{
  "code": "INGREDIENT_NOT_FOUND",
  "message": "재료를 찾을 수 없습니다.",
  "status": 404,
  "traceId": "a1b2c3d4",
  "fieldErrors": [{ "field": "amount", "message": "0보다 커야 합니다." }]
}
```

- Format dates as `2026-12-21` and timestamps as `2026-11-15T09:00:00+09:00`. Money is an integer in won.
- When a value is missing, return the field as `null`; do not omit it.
- Never return domain objects or JPA entities directly; convert them to Response DTOs.
- The frontend generates its API client from the OpenAPI document (`pnpm gen:api`). Declare controller return types as concrete DTOs; never return `Object` or `Map`.
- Services only throw `BusinessException(ErrorCode.XXX)`. `GlobalExceptionHandler` in `global/error` builds every error response in one place. Do not build error responses with try-catch in controllers or services.
- Keep all error codes in the single `global/error/ErrorCode`. Use the same names as the API and error definition documents.
- 401 (login required) and 403 (forbidden) are rejected in the Security filter chain and never reach `GlobalExceptionHandler`. Return the same shape from `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403) in `global/security`.
- Create, deduct, and confirm POSTs (recipe generation, cook, receipt confirm, etc.) accept an `Idempotency-Key` header. When the same key arrives again, do not process it; return the first response (24 hours, `idempotency_keys` table). One shared filter in `global/common` handles this, not each feature.
