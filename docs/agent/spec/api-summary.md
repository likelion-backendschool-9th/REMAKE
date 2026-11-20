# API summary

Source: API definition v3.1 (Korean, kept in Notion / Google Sheets; not in the repo). Base `/api/v1/{public|user|admin}`. All `user` and `admin` calls need a logged-in session. The frontend calls the same domain with relative `/api/...` paths and an HttpOnly cookie; the final token flow is an open decision (`frontend/blueprint.md` §9), so do not add `Authorization: Bearer` handling on your own. Error codes are in `errors.md`. "Events" lists the events the endpoint publishes (see `events.md`). Owner = feature-area owner.

## Common

- JSON UTF-8; success returns the resource directly; 201 create, 204 delete, 202 event ingest.
- Paging `?page=0&size=20` -> `{ content, page, size, totalElements, hasNext }`.
- Dates `YYYY-MM-DD`; timestamps ISO 8601 `+09:00`; money = integer KRW.
- `Idempotency-Key` header on create/deduct/confirm POSTs (24 h replay).
- Timeouts: 10 s default, OCR 30 s, AI 60 s. Headers `X-Client-Platform`, `X-App-Version` on every call.
- "Events" lists what happens during the call. `cart_item_added` is emitted once by the cart's add UseCase, not by the calling API (see `events.md`).

## 1. Start and social login (owner: 이준호)

| Method | Path | Notes |
|---|---|---|
| POST | `/public/auth/oauth/{provider}` | provider `kakao`/`naver`/`google`. Body `{ code, redirectUri }` -> `{ accessToken, onboardingCompleted }`, refresh token in HttpOnly cookie. Events: login_succeeded, login_failed, signup_completed |
| POST | `/public/auth/refresh` | Reissue access token from refresh cookie |
| POST | `/user/auth/logout` | Revoke refresh token |
| GET/PUT | `/user/profile` | `provider, householdSize, onboardingCompleted, lastMealPurpose, excludedCount`; PUT `{ householdSize, onboardingCompleted }` (skip allowed). Event: onboarding_completed |
| GET/PATCH | `/user/settings` | `{ recipeSupplementEnabled, notificationEnabled, provider }` |
| GET/PUT | `/user/excluded-ingredients` | PUT replaces all: `{ catalogIds: [] }` |
| POST | `/user/account/deletion-codes` | Send 6-digit email code (resend = same call). Event: account_deletion_requested |
| POST | `/user/account/deletion-codes/verify` | `{ code }`; correct code deletes account (204). Event: account_deleted |

## 2. Fridge (owner: 김재연)

| Method | Path | Notes |
|---|---|---|
| GET | `/user/ingredients/summary` | `{ total, danger, soon, ok }` |
| GET | `/user/ingredients` | Filters `storageType`, `categoryCode`, `q`; default sort by expiry soonest; item has `dDay`, `status` (DANGER/SOON/OK), `isExpirationEstimated` |
| GET | `/user/ingredients/{id}` | Detail |
| POST | `/user/ingredients` | Required `name, categoryCode, storage, expirationDate, amount, unit`; optional `price`. Event: ingredient_added |
| PATCH | `/user/ingredients/{id}` | Amount, expiry, storage, price. Editing expiry clears the "estimated" flag |
| POST | `/user/ingredients/{id}/consume` | `USED`/`DISCARDED` + amount; at 0 -> FINISHED. Events: ingredient_used, ingredient_discarded |
| DELETE | `/user/ingredients/{id}` | Blocked with `INGREDIENT_HAS_LOGS` if history exists |
| GET | `/user/ingredient-catalog` | Autocomplete / exclusion search `?q=&commonExclusion=true` |
| GET | `/user/ingredient-categories` | 12 categories with defaults |

## 3. Receipts and spending (owner: 이동준)

| Method | Path | Notes |
|---|---|---|
| POST | `/user/receipts/scan` | Multipart `image` (10 MB; jpg/png/heic). **Analysis only, nothing stored.** Returns `items[]` (food only: name, categoryCode, amount, unit, price, storage, expirationDate, isExpirationEstimated), `purchasedOn` and `storeName` (nullable when unreadable), `storeCode` (15 codes, `ETC` fallback), `duplicateSuspected`. No scan limit. Events: receipt_scan_succeeded, receipt_scan_failed |
| POST | `/user/receipts` | Confirm + save (multipart `image` + `payload` JSON with required `purchasedOn`, nonblank `storeName`, and `items[].register`). One transaction; only items with `register: true` become fridge ingredients. Response `{ receiptId, registeredCount }`. Purchase date older than 6 months -> INVALID_INPUT. Events: ingredient_added, receipt_saved |
| GET | `/user/receipts` | `?month=YYYY-MM` (within last 6 months). Item `{ id, storeName, storeCode, purchasedOn, foodItemCount, foodAmount }` |
| GET | `/user/receipts/{id}` | Items, total, signed image URL |
| DELETE | `/user/receipts/{id}` | Removes image/items; fridge ingredients stay; amounts leave spending stats |
| GET | `/user/expenses/monthly` | `?month=` -> `{ month, totalAmount, receiptCount, comparison }`; comparison = last-6-month average `{ baselineMonths, baselineAverage, diffAmount, diffRate }` or null |
| GET | `/user/expenses/monthly/analysis` | Category share + discarded amount (sum of `ingredient_logs.cost_amount`; the Figma screen for it will be added) |
| GET | `/user/expenses/patterns` | `?month=` -> `{ months[], month, totalAmount, comparison, categoryShares[], dailyTotals[] }` (food items of confirmed receipts, by purchase date) |
| GET | `/user/expenses/daily-items` | `?date=` -> items bought that day |

No update endpoint for confirmed receipts. No "analysis finished" notification.

## Usage quotas (owners: 김종수 recipe part, 한윤수 meal-plan part)

Not related to receipts. The recipe screens and the meal-plan setup screen ("이번 달 남은 N회") read it.

| Method | Path | Notes |
|---|---|---|
| GET | `/user/usage-quotas` | Remaining counts for recipes and meal plans, computed from `usage_logs` and `generation_quota_grants` |

Response:
```json
{ "recipe": { "limit": 2, "used": 1, "remaining": 1, "bonus": 0, "bonusExpiresAt": null, "resetsAt": "2026-11-20T00:00:00+09:00" },
  "mealPlan": { "limit": 1, "used": 0, "remaining": 1, "maxDays": 5, "resetsAt": "2026-12-01T00:00:00+09:00" } }
```

## 4. AI recipes (owner: 김종수)

| Method | Path | Notes |
|---|---|---|
| POST | `/user/recipes/generate` | `{ mode: "QUICK"|"CUSTOM", servings, options? }`; custom options `{ ingredientIds, tastes, maxMinutes, tools, methods, cuisineTypes, excludeCatalogIds }` all selections. One response (no streaming). Charged only on success. Events: quota_exceeded, recipe_generated, recipe_generate_failed |
| GET | `/user/recipes/{id}` | Ingredients (`have`: HAVE/SHORT/NONE), servings, cookMinutes, tips, steps, `aiGenerated: true`, `isFavorite`, `allergenWarning` |
| GET | `/user/recipes/{id}/cook-preview` | Deduction candidates; same-kind units auto-converted, otherwise `needsCheck: true`; pinch/to-taste excluded |
| POST | `/user/recipes/{id}/cook` | `{ items: [{ ingredientId, amount, unit }], servings, feedback? }` -> marks USED + cook log. Events: ingredient_used, cook_completed |
| GET | `/user/saved-recipes` | Newest saved first; `q`, `favorite=true` |
| PUT/DELETE | `/user/saved-recipes/{recipeId}` | Save/unsave; PUT returns `{ cartAddedCount }`. Events: recipe_saved, cart_item_added |
| PUT/DELETE | `/user/saved-recipes/{recipeId}/favorite` | Star on/off -> `{ isFavorite }` |

## 5. Meal plan (owner: 한윤수)

| Method | Path | Notes |
|---|---|---|
| POST | `/user/meal-plans/generate` | `{ startDate, purpose, mealTypes: ["LUNCH","DINNER"], servings, overwrite? }`; purpose `BALANCED`(default)/`DIET`/`HIGH_PROTEIN`/`SIMPLE`/`SAVING`. Generates only as many days as ingredients allow. Returns `{ mealPlanId, generatedDays, shortage[], cartAddedCount }`. Events: quota_exceeded, meal_plan_generated, meal_plan_generate_failed, cart_item_added |
| GET | `/user/meal-plans/calendar` | `?month=YYYY-MM`, past months allowed |
| GET | `/user/meal-plans/days/{date}` | Slots `{ slotId, mealType, menuName, recipeId?, ingredientChips, source }` |
| POST | `/user/meal-plan-slots` | Add meal `{ date, mealType, recipeId }` (saved recipe into an empty slot). Event: meal_added |
| POST | `/user/meal-plan-slots/{slotId}/detail` | Lazily generate the recipe for a name-only slot; does not use the daily recipe quota (proposal). Events: meal_slot_detail_generated/failed |

No delete, no slot regeneration/replacement.

## 6. Notifications and cart (owners: 이상호 notifications, 최유리 cart)

| Method | Path | Notes |
|---|---|---|
| GET | `/user/notifications/summary` | Top banner `{ latest, unreadCount }` |
| GET | `/user/notifications` | Last 30 days, types `EXPIRY_SOON`/`EXPIRED`, with `target` |
| PATCH | `/user/notifications/{id}/read` | Mark read |
| POST/DELETE | `/user/push-subscriptions[/{id}]` | Web push subscription |
| GET | `/user/cart-items` | `{ groups: [{ recipeId, groupName, items: [{ id, name, amount, unit }] }] }` |
| PATCH | `/user/cart-items/{id}` | Change amount |
| POST | `/user/cart-items/delete` | `{ ids: [] }` |

Cart items cannot be added manually; they are added by meal-plan generation and by saving a recipe (own or shared).

## 7. Sharing (owner: 최유리)

Fully anonymous: no author info in any response.

| Method | Path | Notes |
|---|---|---|
| GET | `/user/shared-recipes/weekly-top` | TOP 3 `{ rank, id, title, ingredientChips, photoUrl }` (no rank change) |
| GET | `/user/shared-recipes/ranking` | TOP 100; `?q=`, `?sort=popular|latest`; popular item has `rankChange` (last rank - this rank, null if none/same), latest item has `sharedAt` and no rank |
| POST | `/user/shared-recipes` | Multipart `recipeId`, `title` (required), `photos[]` (>= 1). Only Remake-generated own recipes. Event: shared_recipe_published |
| GET | `/user/shared-recipes/{id}` | Detail incl. `aiGenerated`, `saveCount`, `saved`; hidden/deleted -> 404 |
| DELETE | `/user/shared-recipes/{id}` | Author takes it down (screen undecided) |
| PUT/DELETE | `/user/shared-recipes/{id}/save` | Save makes a copy in own saved list; returns `{ saveCount, cartAddedCount }`. Events: recipe_saved, cart_item_added, shared_recipe_saved |
| POST | `/user/shared-recipes/{id}/reports` | `{ reason: PHOTO|OFFENSIVE|DANGEROUS|OTHER, detail? }`; not auto-hidden. Event: shared_recipe_reported |

## Events (common)

`POST /user/events` — `{ events: [{ name, screen, properties, occurredAt, sessionId }] }`, max 50; unknown names dropped; returns 202 `{ accepted, rejected }`.

## Admin (all under `/admin`, ADMIN only; every action writes `admin_audit_logs`)

Decision [#58](https://github.com/likelion-backendschool-9th/REMAKE/issues/58): WBS 8.2 analytics APIs (dashboard, metrics, funnels, event log/catalog) remain P0. The ADMIN authorization check and non-admin blocking required by these APIs are P0 prerequisites from WBS 8.1, not an upgrade of all admin features. Implement and verify this gate first: ADMIN allowed, authenticated non-admin 403 `FORBIDDEN`, unauthenticated requests rejected through server security filters. Other admin features remain P1. Access control is tracked in [#84](https://github.com/likelion-backendschool-9th/REMAKE/issues/84), analytics in [#93](https://github.com/likelion-backendschool-9th/REMAKE/issues/93).

WBS 11.8 P0 event verification covers only first-demo features. Sharing/cart/reward events follow their P1 feature schedules; see [the event priority rules](events.md#demo-implementation-and-verification-priority-decision-58). Event names and payloads are unchanged.

| Method | Path | Purpose |
|---|---|---|
| GET | `/admin/dashboard/summary` | North-star metric, KPI cards, alerts (`?from&to&includeSeed`) |
| GET | `/admin/metrics` | KPI time series `?kpi&from&to&interval` |
| GET | `/admin/funnels/{funnelCode}` | Funnel conversion |
| GET | `/admin/events`, `/admin/events/catalog` | Raw event log (internal user id only), event catalog with 24 h volume |
| GET | `/admin/usage/daily` | AI/OCR calls, success rate, latency, top users, limit-hit ratio |
| GET/PATCH | `/admin/reports`, `/admin/reports/{id}` | Report queue; result `HIDDEN`/`DELETED`/`DISMISSED` |
| GET/PATCH | `/admin/shared-recipes[/{id}]` | Shared recipe status `VISIBLE`/`HIDDEN`/`DELETED` |
| GET | `/admin/users` | Search (no email shown) |
| PATCH | `/admin/users/{id}/generation-quota` | `{ delta, reason }` reason required |
| PATCH | `/admin/users/{id}/status` | `ACTIVE`/`SUSPENDED` + reason |
| GET | `/admin/audit-logs`, `/admin/weekly-rewards` | Action history, weekly TOP 3 and reward history |

Admin screens are a proposal (no Figma yet): A1 dashboard, A2 funnel, A3 feature monitoring, A4 event log (P0); A5 reports, A6 users, A7 usage/cost, A9 ranking/rewards (P1); A8 anomaly alerts (P2, via `alerts` in dashboard summary).
