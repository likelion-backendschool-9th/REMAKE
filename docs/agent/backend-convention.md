# Backend Code Convention

> Korean version for developers: [`docs/developer/backend-convention.md`](../developer/backend-convention.md)

## Code style

- Write tests by the [backend testing doc](backend-testing.md) (test types, the integration test base class, the test clock).
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
│           └── ai / ocr / oauth / filestorage / mail  External service calls (only where needed)
│
└── global                      Shared by all features
    ├── error                   ErrorCode, BusinessException, GlobalExceptionHandler
    ├── config                  Swagger, JPA, CORS, Clock, JSON time format, shared external service config
    ├── security                JWT filter, Security config
    └── common                  Shared classes such as BaseCreatedEntity, BaseTimeEntity, the duplicate-request filter
```

Feature folders and tables (ERD v3):

| Feature folder | Tables |
|---|---|
| `user` | users, user_excluded_ingredients, account_deletion_requests, subscriptions, subscription_payments |
| `auth` | refresh_tokens |
| `ingredient` | ingredient_categories, ingredient_catalog, ingredients, ingredient_logs |
| `receipt` | receipts, receipt_items |
| `recipe` | recipes, recipe_ingredients, recipe_steps, saved_recipes |
| `mealplan` | meal_plans, meal_plan_slots |
| `cart` | cart_items |
| `share` | shared_recipes, shared_recipe_reports, shared_recipe_weekly_ranks |
| `quota` | usage_logs, generation_quota_grants |
| `notification` | notifications |
| `event` | events |
| `admin` | On hold: admin screens are not in Figma, so there are no admin tables. When designed, admin actions call each feature's UseCase |

Rules:

- Dependencies point one way: `adapter → application → domain`. Do not use Spring or JPA annotations in `domain`.
- Controllers call only `UseCase` interfaces.
- Services call only their own feature's `Port` interfaces and other features' `UseCase` interfaces.
- Only JPA entities extend the shared base classes. Domain objects do not.
  - Tables whose values are updated (users, ingredients, meal_plan_slots, cart_items, shared_recipes) extend `BaseTimeEntity` (`created_at` + `updated_at`). Confirmed receipts are never edited, so `receipts` and `receipt_items` extend `BaseCreatedEntity`.
  - All other tables extend `BaseCreatedEntity` (`created_at`).
  - Join tables, reference data, daily aggregates, and `events` extend neither.

### Using another feature's data

- When you need another feature's data, call that feature's `UseCase` (port/in). Do not use another feature's `Port` (port/out), adapters, or repositories.
- Reference another feature's data by ID only, such as `Long ingredientId`, in both domain objects and JPA entities. Do not create JPA associations (`@ManyToOne`, etc.) to another feature's entities. Keep the DB foreign keys.
- Another feature's domain enums (fixed value lists such as `IngredientUnit` and `StorageLocation` in `ingredient/domain`) may be imported directly. The `UseCase` rule applies to reading and writing data, not to value types. Keep each enum in the feature that owns the values; do not move it to `global`.
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
| Social unlink (Kakao unlink / Naver delete) | `adapter/out/oauth` | `user/adapter/out/oauth` |
| Email delivery (Kakao/Google account deletion code) | `adapter/out/mail` | `user/adapter/out/mail` |
| Payments (Toss billing key and auto-charge) | `adapter/out/payment` | `user/adapter/out/payment` |

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
  - Backend CI checks these two rules against the latest dev (see the [migration check](ci.md#migration-check)). When a migration lands on dev, open PRs that add migrations get this check re-run automatically, so a PR whose number is now taken turns red. If unsure, re-run Backend CI right before merging.
  - During a deploy, the old and new app versions share the DB for a while. Split changes that break the old version, such as renaming or dropping a column, into steps (see the [deployment doc](deployment.md)).
- **Types**: quantities `numeric(10,3)` ↔ `BigDecimal`, money `bigint` ↔ `Long` (won), timestamps `timestamp` ↔ `LocalDateTime` (Korea time), JSON `jsonb`.
- **Fixed value lists** (statuses, kinds, units) are Java enums stored with `@Enumerated(EnumType.STRING)`. Never store the ordinal (`ORDINAL`).
- **Read the current time only from the injected `Clock`.** Do not use `now()` without a `Clock` such as `LocalDateTime.now()` or `LocalDate.now(zone)`, a column `DEFAULT now()`, or `now()` inside a query. The base classes set created and updated times through JPA auditing. `ClockUsageTest` fails the build when production code or migration SQL breaks this rule.

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
- Write resources as plural kebab-case (`saved-recipes`, `cart-items`). Put actions as a verb after the resource (`/recipes/generate`, `/receipts/scan`). Do not use `me` in paths (`/user/profile`, not `/users/me`). The full endpoint list is [`spec/api-summary.md`](spec/api-summary.md).
- In production the proxy sends only requests starting with `/api/` to the backend and everything else to the frontend. Breaking this rule works locally but returns 404 in production.
- Do not add the prefix with `server.servlet.context-path` or a global path setting (`addPathPrefix`). `context-path` also moves the health check (`/actuator/health/readiness`) and breaks deployment; a global prefix turns a controller that also writes `/api` into `/api/api/...`.
- Use full paths in Security `requestMatchers` too.
  - `/api/v1/public/**`, `/actuator/health/**`: allow without authentication. Blocking the health check breaks deployment.
  - `/api/v1/admin/**`: ADMIN only. 403 without the role.
  - All other `/api/v1/**`: login required.
- In local development the frontend (`http://localhost:3000`) calls the backend (`http://localhost:8080`) directly, so allow it in CORS (`global/config/CorsConfig`, allowed origins in `remake.cors.allowed-origins`). Auth cookies are sent only with `allowCredentials(true)` and exact origins: list each origin (e.g. `http://localhost:3000`); never use `*`. Production uses one domain and needs no CORS.
- If you need a path outside `/api/` (WebSocket, file download, etc.), tell the deployment owner before building it. The proxy needs a new rule.

## Responses and errors

On success, return the Response DTO as-is without a wrapper. Never create success wrapper classes such as `ApiResponse` or `CommonResponse` (`{ data, meta }`, etc.). Distinguish success and failure by HTTP status code: 201 for creation, 204 for deletion without a body, 202 for accepted-now-processed-later (event collection), 200 for other successes.

Paginate growing lists in this shape (`?page=0&size=20`, `PageResponse.from(page, XxxResponse::from)`). Do not use cursor pagination. Return fixed-size reference data (ingredient categories, etc.) as a plain array without paging.

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "hasNext": false }
```

On failure, every API returns this shape. Include `fieldErrors` (`{ field, message }` items) only for input validation failures (`INVALID_INPUT`). Include `details` only when the screen needs extra values, such as the remaining count and reset time for `GENERATION_LIMIT_EXCEEDED` (`"details": { "remaining": 0, "resetsAt": "2026-11-21T00:00:00+09:00" }`). When `fieldErrors` has no value, return it as `null` (do not omit the field); when `details` has no value, omit the field (`@JsonInclude(NON_NULL)`). Pass details with `new BusinessException(ErrorCode.XXX, Map.of(...))` or `new BusinessException(ErrorCode.XXX, message, Map.of(...))`; `GlobalExceptionHandler` copies them into the response. The error code list is [`spec/errors.md`](spec/errors.md).

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
  - Use `LocalDate` and `LocalDateTime` in DTOs as-is. `global/config/JacksonConfig` adds `+09:00`, so do not convert to `OffsetDateTime` or strings in DTOs. Fractions of a second are dropped.
  - A timestamp received in a request with another offset (`Z`, etc.) is converted to Korea time; one without an offset is treated as Korea time.
- Use camelCase field names. Return enum values as their names (UPPER_SNAKE_CASE, `FRIDGE`, `SHARED_COPY`). Use the enum type itself in DTOs.
- When a value is missing, return the field as `null`; do not omit it.
- Never return domain objects or JPA entities directly; convert them to Response DTOs.
- The frontend generates its API client from the OpenAPI document (`pnpm gen:api`). Declare controller return types as concrete DTOs; never return `Object` or `Map`. The document is at `/v3/api-docs` on the local backend and the UI at `/swagger-ui.html`. APIs that need login declare cookie authentication in Swagger with `@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)` (OpenAPI `apiKey`, `in: cookie`, name `ACCESS_TOKEN`), matching #42.
- Services only throw `BusinessException(ErrorCode.XXX)`. `GlobalExceptionHandler` in `global/error` builds every error response in one place. Do not build error responses with try-catch in controllers or services.
- Keep all error codes in the single `global/error/ErrorCode`. Use the same names as the API and error definition documents.
- 401 (login required) and 403 (forbidden) are rejected in the Security filter chain and never reach `GlobalExceptionHandler`. Return the same shape from `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403) in `global/security`.
- There is no `Idempotency-Key` header and no duplicate-request key table. The frontend disables buttons such as register and deduct until the request ends.
- Authentication is decided in [#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42) (option A): the access (30 min) and refresh (14 d) JWTs are both HttpOnly cookies (`Secure; SameSite=Lax`). Read the token from the cookie, not the `Authorization` header. Login and refresh responses do not include `accessToken`.

### Example API

Build new APIs by following the ingredient category list (`GET /api/v1/user/ingredient-categories`). It has a full set, from migration to tests.

| Location | Files |
|---|---|
| Migration | `db/migration/V1__create_ingredient_categories.sql` |
| domain | `IngredientCategory`, `IngredientCategoryCode`, `StorageLocation`, `IngredientUnit` |
| port/in | `GetIngredientCategoriesUseCase` |
| port/out | `LoadIngredientCategoryPort` |
| service | `IngredientCategoryService` (throws `BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND)` when missing) |
| adapter/in/web | `IngredientCategoryController`, `IngredientCategoryResponse` |
| adapter/out/persistence | `IngredientCategoryJpaEntity`, `IngredientCategoryJpaRepository`, `IngredientCategoryPersistenceAdapter` |
| Tests | `src/test/.../ingredient/IngredientCategoryApiTest` |

## Data access and request handling

- Scope every query to the token owner. Another user's resource returns 404 (`*_NOT_FOUND`), not 403, so its existence is not leaked.
- Generate a `traceId` per request, put it in the MDC log context, and return it in error responses. Never return stack traces, SQL, or provider error text.
- Time limits: normal requests 10 s, OCR 30 s, AI recipe generation 60 s (meal-plan generation replies 202 and runs as a server job; its limit is (open, #122)). Convert upstream timeouts to `OCR_TIMEOUT` / `AI_TIMEOUT` and provider errors to `OCR_UPSTREAM_ERROR` / `AI_UPSTREAM_ERROR`.
- Business rules (limits, units, expiry states, receipt retention) are in [`spec/domain-rules.md`](spec/domain-rules.md). Table-level rules (optimistic lock, deletion, retention) are in the [ERD](erd.md).

## AI generation

- Each feature calls the LLM through its own `port/out` interfaces, implemented in that feature's `adapter/out/ai`; do not share another feature's AI port. The input is IDs and enums from the request plus DB data (fridge ingredients, excluded allergen codes). **Never put text the user typed into a prompt.**
- Ask for structured JSON and validate it: schema, unit codes from the 14-unit list, non-empty steps. Invalid output → `AI_INVALID_RESPONSE`; do not store it and do not count it toward the quota.
- With the "recipe supplement" setting ON, recipes may use ingredients that are not in the fridge. With it OFF, use only fridge ingredients; fewer than 2 non-seasoning ingredients (expired ones excluded) → `NO_INGREDIENTS` without calling the AI. The number 2 is the recipe area owner's decision (the policy only says "too few"); it is `GenerationIngredients.MIN_MAIN_INGREDIENTS`. Excluded (allergy) ingredients sent to the AI = the user's settings (`GetUserGenerationPreferencesUseCase`, current values, always, for quick, custom, and regenerate) plus the chips picked in custom. Request and domain types are `AllergenCode` (`user/domain`, ERD Appendix B); an unknown code is `400 INVALID_INPUT`.
- Meal plans: every slot gets a full recipe at generation (no lazy slot detail). Do not allocate the same ingredient amount to several meals (based on each slot's recipe ingredients; no per-slot planned-ingredient table). Generate only as many days as the ingredients allow (max 5 for BASIC, 7 for PREMIUM), then add the shortages to the cart.
- Only successful generations count toward the quota. Limits are in [`spec/domain-rules.md`](spec/domain-rules.md). The quota feature (`quota`) stores no remaining count: it counts `usage_logs` rows since today (recipes) or the 1st of the month (meal plans). Charge with `ConsumeRecipeQuotaUseCase` (recipes) or `ConsumeMealPlanQuotaUseCase` (meal plans) in the same transaction that saves the result; it takes a per-user advisory lock so concurrent requests cannot exceed the limit. `remake.quota.enforced=false` turns off only the limit check (logs are still written). It is set in `application-local.yml` alone (`QUOTA_ENFORCED`, default `false` there) so developers can test repeatedly with their own Gemini key; production and tests use the default `true`. Every developer uses their own free key in `backend/.env`.
- The LLM is Google Gemini (free tier). `GenerateRecipeAiPort` (`recipe/application/port/out`) is implemented by `GeminiRecipeAdapter` (`recipe/adapter/out/ai`). Meal plans have their own port, `GenerateMealPlanAiPort` (`mealplan/application/port/out`), because one call fills many slots and another feature's `Port` must not be used; it is implemented by `GeminiMealPlanAdapter` (`mealplan/adapter/out/ai`), which uses its own `RestClient` with a 90-second read timeout (one call returns every slot, and the job runs in the background). Both adapters call the stateless `generateContent` API with `responseMimeType: application/json` and a `responseSchema`, and put the rules in the system instruction and the data (option codes, fridge ingredients) in the user message as JSON. Fridge ingredient names come from the DB but users typed them, so the system instruction says to treat them as data, not instructions.
- Settings: `remake.gemini.api-key` (`GEMINI_API_KEY`), `remake.gemini.model` (`GEMINI_MODEL`, default `gemini-3.5-flash-lite`). Locally the key goes in `backend/.env`; in production it goes in the server's `.env` and `compose.prod.yml` passes it to the backend. The production key belongs to a production-only Google Cloud project; do not use it locally. Receipt OCR uses the same key, model, and base URL with its own 30 s limit. An empty key returns `AI_UPSTREAM_ERROR` for generation and `OCR_UPSTREAM_ERROR` for receipt OCR. Never put the key in a request body or logs.
- One adapter call has a 25 s limit and does not retry. The service retries once (also after `AI_INVALID_RESPONSE`), so two calls stay within the 60 s API limit. Meal plans: one adapter call has a 90 s limit (`GeminiMealPlanClientConfig.READ_TIMEOUT`, to be tuned after measurement); `MealPlanGenerationJobService` validates the slots with `GeneratedMealPlanValidator` and retries once after 1.5 s on the same three AI errors. A provider overload (Gemini 503, `MealPlanAiBusyException`, still `AI_UPSTREAM_ERROR`) is retried up to 9 more times (10 calls in total) after 3 s, 5 s, 7 s, 9 s, and then 10 s each, 74 s of waiting in all (`remake.meal-plan.ai-busy-retry-delays`), because the job runs in the background and 503s were frequent in the 11-24 measurement. Rejected calls also count as requests in AI Studio usage, so the retries are spaced out instead of sent back to back, to spare the free per-minute limit shared with recipe generation. This differs from the "retries once" policy and is a proposal for the PO. If any requested slot is missing, duplicated, or breaks the recipe limits, the whole response is `AI_INVALID_RESPONSE` (dropping one slot would cut the plan from that day). Error mapping: timeout → `AI_TIMEOUT`; HTTP error (including 429 and 503), connection failure, safety block, or missing key → `AI_UPSTREAM_ERROR`; non-JSON, no candidate, or `MAX_TOKENS` → `AI_INVALID_RESPONSE`. Log only the provider's status code, never its error text.
- `GeneratedRecipeValidator` turns the AI output into a `Recipe`: servings come from the request, a line with a unit outside the 14 codes is dropped (no lines left → `AI_INVALID_RESPONSE`), an unknown fridge ID becomes "not in the fridge", `TO_TASTE` has no amount, and amounts are rounded to 3 decimals. With supplement OFF, any non-fridge ingredient → `AI_INVALID_RESPONSE`, including basic seasonings (salt, cooking oil) and `PINCH`/`TO_TASTE` lines: seasonings are items users register like any other (#158). The only exception is water (`GeneratedRecipeValidator.WATER`, exact name "물"; recipe area owner's decision): it is not a fridge item, so it is allowed without the fridge and never counts as a shortage (event `shortage_count`, cart). `PINCH` and `TO_TASTE` only mean "not deducted when cooking".
- Free-tier limits are per Google Cloud project: 15 requests per minute, 500 per day (resets at midnight Pacific time). One recipe is about 800 tokens and 5–10 s. Meal-plan generation must not call the model once per slot; one plan in one call (or a few) keeps it under 15 RPM.
- List rows that show a recipe (saved list 33, meal plan 40·42, shared 49·50) build `ingredientSummary` and `extraIngredientCount` with `IngredientSummary.of` (`recipe/domain`): the first 3 ingredient names in recipe order joined with `" · "`, plus the count of the rest (the screen shows "+N", hidden when 0). Water, `PINCH`, and `TO_TASTE` lines count like any other (recipe area owner's decision, Figma p4.1 and p4.1.4). Other features read recipe rows through `GetRecipeListItemsUseCase` (title, summary, favorite by recipe ID list). The sharing area reads summaries of other users' public recipes in one call through `GetRecipeIngredientSummariesUseCase` (recipe ID list → `IngredientSummary` per ID). It does not check visibility: the caller passes only the original recipe IDs of VISIBLE shared posts.
- Saved list `AVAILABLE` ("바로 가능"): every ingredient except water has a non-expired fridge ingredient with the same name. Amounts are not compared until unit conversion (#63). Compare by name because a saved shared recipe points at another user's fridge IDs.
- Unit conversion lives only in `UnitConversion` (`recipe/domain`, #63): weight↔weight, volume↔volume (TBSP 15 ml, TSP 5 ml, CUP 200 ml), count units only with the same unit, `PINCH`/`TO_TASTE` never; results are rounded to 3 decimals. Meal-plan allocation (`StockUnits`) and cart → fridge (`CartFridgeQuantity`) use it too. Do not add another conversion table.
- Cook preview (31) pairs each recipe line with my non-expired fridge ingredient of the same name, the one expiring first. It skips water, `PINCH`, `TO_TASTE`, and lines without an amount. The amount is converted to the fridge unit and capped at the remaining amount; when the kinds differ (1 EA ↔ 300 g, a receipt `PACK` stock ↔ g, #145) it is `null` and the user decides. Cook (32) deducts through `ConsumeIngredientUseCase` (via `COOK`, which emits E25), all or nothing, and marks today's meal slot through `MarkMealSlotCookedUseCase`.

## Scheduled jobs (Korea time)

| Job | When | What |
| --- | --- | --- |
| Expiry notifications | Daily 09:00 | `EXPIRY_SOON` (3 days or less, one per ingredient) and `EXPIRED` (one per day) (in-app only, no device push). Skip users with notifications off. Emit `ingredient_expired` and `notification_sent` |
| Weekly ranking and reward | Monday 00:00 | Fix last week's TOP 100 in `shared_recipe_weekly_ranks` and give the authors of the top 3 recipes +10/+6/+3 recipe generations (valid one month), in one transaction. Rules in the [ERD](erd.md) table 22. Emit `weekly_reward_granted` |
| Receipt cleanup | Daily early morning | Delete receipts older than 6 months by purchase date, with images and items |
| Subscription auto-payment | Daily early morning | Charge ACTIVE subscriptions whose next billing date is today with the billingKey and extend one month; end CANCELED ones (`users.plan` BASIC). Failure handling is open (ask the user) |
| Event retention | Daily early morning | Delete raw events older than 180 days. No daily aggregation table |

- Daily and monthly quota resets need no job: remaining counts are computed from `usage_logs`. Other cleanups are listed in the ERD implementation rules.

## Event publishing (`EventTracker`)

The event contract is [`spec/events.md`](spec/events.md). Each API's events are in the "Events" column of [`spec/api-summary.md`](spec/api-summary.md).

- The `event` feature owns one tracker. Other features call it only through the `event` feature's UseCase (proposed: `eventTracker.track(EventName.RECEIPT_SAVED, Screen.RECEIPT_RESULT, props)`; the event owner decides the final signature).
- Emit at the end of the API, on success, failure, and rejection, exactly as the spec says. Record one user action once, as either a CLIENT or a SERVER event.
- Write asynchronously after the transaction commits. Tracking never blocks or fails the request; swallow and log its errors.
- Keep event names and properties in an enum or constants class that matches the event sheet. Do not type event names as string literals.
- The tracker filters personal data: it drops keys not in the event's allowed property list and rejects more than 25 keys or nested objects. Properties are flat strings, numbers, and booleans.
- It sets the common fields: `user_id` (internal ID), `session_id`, `source` (CLIENT/SERVER), `screen`, `platform`, `app_version`, `is_seed`, `occurred_at`.
- `POST /api/v1/user/events` accepts up to 50 client events, drops unknown names, and returns 202 `{ accepted, rejected }`. More than 50 → `EVENT_BATCH_TOO_LARGE`.

## Required tests

How to write tests: [backend testing](backend-testing.md).

- Unit tests for business rules: quota counting (success only, concurrent requests per member), unit conversion (same kind only; TBSP 15 ml, TSP 5 ml, CUP 200 ml; PINCH and TO_TASTE never deducted), expiry status, meal-plan ingredient allocation and shortage, ranking and `rankChange`, duplicate receipts, receipt retention.
- Mock the OCR and LLM adapters and cover the timeout and invalid-response cases.
- Admin endpoints (on hold until admin screens exist): test 403 for non-admins.
