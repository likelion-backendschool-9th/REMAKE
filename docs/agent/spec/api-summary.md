# API summary

Source: API definition v4 (Figma-based, Korean, kept in Notion; not in the repo). 60 APIs, numbered 1-60, plus API 61 (discarded food, proposal 11/25, #248) added after v4; other spec files refer to APIs by these numbers. Admin APIs are on hold because admin screens are not in Figma yet. Error codes per API are in `errors.md`; events are in `events.md`.

## Common

| Item | Rule |
| --- | --- |
| Path | `/api/v1/{role}/{resource}[/{id}[/{action}]]`. Role `public` (before login) or `user` (after login). Resources are plural kebab-case; actions are trailing verbs (`generate`, `cook`). No `me` under `user` |
| Format | REST + JSON (UTF-8). Only photo uploads use `multipart/form-data` |
| Auth | Access (30 min) and refresh (14 days) JWTs, both in HttpOnly cookies (`Secure; SameSite=Lax`) ([#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42) option A). No `Authorization` header. Same domain `/api/...` + `credentials: 'include'` |
| Success | Return the resource directly (no wrapper). Create 201, no body 204, event ingest and meal-plan generation acceptance (38) 202 |
| Errors | `{ code, message, status, traceId, fieldErrors?, details? }`. Codes and `details` are in `errors.md` |
| Paging | `?page=0&size=20` -> `{ content, page, size, totalElements, hasNext }`. Fridge, cart, and month lists are not paged |
| Dates and money | Date `2026-11-19`, time ISO 8601 (`+09:00`), money integer KRW |
| Codes | Units, categories, meal purposes, etc. are the ERD Appendix C codes |
| Images | `imageUrl` in responses is a server-made URL (storage is the backend's choice). Ingredient and item images are the category image |
| Client headers | Every request sends `X-Client-Platform` (TWA/PWA/WEB), `X-App-Version`, and `X-Screen` (current screen code). The server writes them to `events.platform`, `app_version`, `screen` |
| Timeouts | 10 s default, OCR 30 s, AI recipe generation 60 s (recipes only; meal-plan generation replies 202 at once and runs as a server job). Failed or timed-out generation is never charged |
| Admin | Not in this version (no admin screens in Figma) |
| Payments (Toss Payments) | Card auto-billing. The frontend only opens the card registration window with the client key. The server issues the billingKey (`POST /v1/billing/authorizations/issue`) and charges (`POST /v1/billing/{billingKey}`) with the secret key. Never send the billingKey or secret key to the frontend. The server makes the `orderId` |
| Numbering | Subscription APIs (57-60) belong to area 1 but are numbered last so other documents keep their API numbers |
| Removed | No `Idempotency-Key` header rule (the duplicate-request key table was removed) |

## 1. Login, onboarding, settings (owner: 이준호)

| No. | Method | Path | Description | Request | Response | Events |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | POST | `/public/auth/oauth/{provider}` | Social login and signup. provider = kakao, naver, google. Sets access and refresh tokens as HttpOnly cookies | `{ code, redirectUri }` | `200 { onboardingCompleted }` | login_succeeded, login_failed, signup_completed |
| 2 | POST | `/public/auth/refresh` | Reissue the access cookie from the refresh cookie | no body (refresh cookie) | `204` | |
| 3 | POST | `/user/auth/logout` | Delete the refresh token row and both cookies | no body | `204` | |
| 4 | PUT | `/user/onboarding` | Save onboarding. Sent once when p1.2 is completed, with both screens' values. Zero allergies allowed | `{ householdSize (1-50), allergenCodes: [] }` | `204` | onboarding_completed |
| 5 | GET | `/user/profile` | All settings screen values | - | `200 { provider, plan, householdSize, excludedIngredientCount, recipeSupplementEnabled, notificationEnabled, lastMealPurpose, onboardingCompleted }` | |
| 6 | PATCH | `/user/profile` | Send only the changed values among household size, recipe supplement, notifications | `{ householdSize? (1-50), recipeSupplementEnabled?, notificationEnabled? }` | `200` (same shape as API 5) | |
| 7 | GET | `/user/excluded-ingredients` | My excluded-ingredient codes | - | `200 { allergenCodes: [] }` | |
| 8 | PUT | `/user/excluded-ingredients` | Replace the whole set. Zero allowed (settings "done" stays enabled) | `{ allergenCodes: [] }` | `200 { allergenCodes }` | |
| 9 | POST | `/user/account/deletion-code` | Send a 6-digit code to the Kakao/Google member's email (valid 5 minutes). Resend uses the same API | no body | `201 { expiresAt }` | |
| 10 | POST | `/user/account/deletion` | Delete the account. Kakao/Google confirm with the email code, Naver with a re-authentication code. Clears cookies on success | Kakao/Google `{ method: "EMAIL_CODE", code }` / Naver `{ method: "REAUTH", oauthCode, redirectUri }` | `204` | account_deletion_requested, account_deleted, subscription_ended |
| 57 | GET | `/user/subscription` | My subscription. NONE -> before-subscribe screen, ACTIVE -> subscribed screen. Before subscribing, the next billing date is today + 1 month | - | `200 { status: NONE\|ACTIVE\|CANCELED, price, nextBillingDate, currentPeriodEnd? }` | |
| 58 | POST | `/user/subscription/billing-auth` | Values to open the Toss card registration window. The frontend calls Toss SDK `requestBillingAuth`; Toss returns to the frontend success URL with `authKey` and `customerKey` | no body | `200 { customerKey, orderName, amount }` | |
| 59 | POST | `/user/subscription` | Start the subscription: issue a billingKey with `authKey`, charge the first month (9,900 KRW), and on success start it (`users.plan` PREMIUM, next billing date = today + 1 month). If the first payment fails, no subscription is created | `{ authKey, customerKey }` | `201` (same shape as API 57) | subscription_started, subscription_payment_failed |
| 60 | POST | `/user/subscription/cancel` | Schedule cancellation. Benefits stay until the day before the next billing date; on that date no charge and the plan becomes BASIC. Already scheduled -> 200 unchanged | no body | `200` (same shape as API 57, status CANCELED) | subscription_canceled |

## 2. Fridge (owner: 김재연)

| No. | Method | Path | Description | Request | Response | Events |
| --- | --- | --- | --- | --- | --- | --- |
| 11 | GET | `/user/ingredients/summary` | Total and per-status counts (independent of filters) | - | `200 { total, danger, soon, ok }` | |
| 12 | GET | `/user/ingredients` | My ingredients, soonest expiry first. **No search** (not in Figma). Not paged | `?status=DANGER\|SOON\|OK &storage=FRIDGE\|FREEZER\|PANTRY &categoryCode=` | `200 { items: [{ id, name, categoryCode, imageUrl, storage, amount, unit, expirationDate, dDay, status }] }` | |
| 13 | GET | `/user/ingredients/{id}` | One ingredient | - | `200 { id, name, categoryCode, imageUrl, storage, amount, unit, expirationDate, dDay, status, price }` | |
| 14 | GET | `/user/ingredient-units` | The backend picks the unit for an ingredient name. The quantity field stays disabled until a name is chosen; then the frontend calls this and enables it. Call again when the name or category changes, including on the OCR item edit screen (p3.3.1) before registration. Uses `ingredient_catalog` with the same rule as saving (15, 16, 21). `categoryCode` is optional: with it the result equals the unit that will be saved (for example "유기농 우유" + DAIRY_EGG -> `PACK`); without it only an exact name match gives the dictionary unit, otherwise `EA`. Send `categoryCode` whenever the screen has one (add p2.2 and OCR edit p3.3.1 require it before saving). An empty `name` or an unknown `categoryCode` is `INVALID_INPUT` | `?name=대파&categoryCode=VEGETABLE` | `200 { unit }` | |
| 15 | POST | `/user/ingredients` | Manual add. Category required (no "all"). The request has no `unit`: the server picks it from the name and category (same rule as API 14 with `categoryCode`, so the preview equals the saved unit) and returns it. Price optional (included in consumption patterns when set) | `{ name, categoryCode, storage, expirationDate, amount, price? }` | `201` (one-ingredient shape) | ingredient_added |
| 16 | PATCH | `/user/ingredients/{id}` | Send only changed values. Changing the name or the category makes the server pick the unit again with the new values and return it (e.g. 대파 1 BUNCH -> 양파 1 EA; the number stays; the result equals API 14 for the same name and category). Sending the same name and category again changes nothing. Also when the ingredient has use/discard logs (logs keep their own name and unit). `amount` fixes an input mistake: it sets both `amount` and `initial_amount`, and is rejected with `INVALID_INPUT` if the ingredient has any use/discard log (#128) | `{ name?, categoryCode?, storage?, expirationDate?, price?, amount? }` | `200` (one-ingredient shape) | |
| 17 | DELETE | `/user/ingredients/{id}` | Delete the ingredient. Use/discard logs stay with the name | - | `204` | |
| 18 | POST | `/user/ingredients/{id}/consume` | Use or discard. Discarding computes the cost when a price exists. Deletes the ingredient at 0 | `{ type: "USED"\|"DISCARDED", amount }` | `200 { remainingAmount, deleted, costAmount? }` | ingredient_used, ingredient_discarded |
| 19 | GET | `/user/ingredient-categories` | 12 categories | - | `200 { items: [{ code, name, imageUrl }] }` | |

`GET /user/ingredient-catalog` (autocomplete) is not part of the API. `ingredient_catalog` is used only inside the server (11/22 decision).

## 3. Receipts and consumption patterns (owner: 이동준)

| No. | Method | Path | Description | Request | Response | Events |
| --- | --- | --- | --- | --- | --- | --- |
| 20 | POST | `/user/receipts/scan` | OCR analysis only, nothing stored. Synchronous. Returns food only. A receipt with no food items, older than 6 months, or already registered (same store (branch name), receipt number, and purchase time; if the number is unreadable compare store, purchase date, and food names; if the time is unreadable compare by purchase date) fails and is shown on the OCR failure screen. No `dDay`/`status`: the result is not stored yet and the user edits the expiry, so the frontend computes them from `expirationDate` with the KST date (11/25) | multipart `image` (jpg, png, heic, 10 MB) | `200 { storeName, storeCode, purchasedOn, purchasedTime, receiptNumber, items: [{ name, categoryCode, storage, amount, unit, price, expirationDate }] }` | receipt_scan_succeeded, receipt_scan_failed |
| 21 | POST | `/user/receipts` | Save receipt, items, and fridge ingredients at once. The server picks units from names. Only selected cart items are deducted (#57); skipping leaves the cart unchanged, and cartRemovedCount counts rows actually deleted at zero. Receipts older than 6 months cannot be registered. Duplicates are blocked again | multipart `image` + `payload { storeName, storeCode, purchasedOn, purchasedTime, receiptNumber, items: [{ name, categoryCode, storage, amount, price, expirationDate, cartDeductions?: [{ cartItemId, amount }] }] }` | `201 { receiptId, ingredientCount, cartRemovedCount }` | ingredient_added, receipt_saved |
| 22 | GET | `/user/receipts` | Receipts of the month. Month total from receipt items only; average = average monthly total of the last 6 months | `?month=2026-11` (within the last 6 months) | `200 { month, totalAmount, averageDiffAmount, weeks: [{ label, startDate, endDate, totalAmount, receipts: [{ id, storeCode, storeName, purchasedOn, itemCount, totalAmount }] }] }` | |
| 23 | GET | `/user/receipts/{id}` | One receipt | - | `200 { id, storeCode, storeName, purchasedOn, imageUrl, items: [{ name, categoryCode, imageUrl, storage, amount, unit, price }], totalAmount }` | |
| 24 | DELETE | `/user/receipts/{id}` | Delete receipt, items, image. It leaves spending and patterns; fridge ingredients stay | - | `204` | |
| 25 | GET | `/user/expenses/patterns` | Consumption of the month: receipt items + ingredients added without a receipt with a price (by registration date). `discardedAmount` = discarded cost of the month (sum of `ingredient_logs.cost_amount`; proposal 11/25, #248) | `?month=2026-11` (within the last 6 months) | `200 { month, totalAmount, averageDiffAmount, discardedAmount, categories: [{ categoryCode, amount, ratio }], dailyTotals: [{ date, amount }] }` | |
| 26 | GET | `/user/expenses/daily-items` | Items of the chosen day | `?date=2026-11-20` | `200 { date, totalAmount, items: [{ name, categoryCode, imageUrl, storage, amount, unit, price }] }` | |
| 61 | GET | `/user/expenses/discarded` | (proposal 11/25, #248) Discarded food of the month for p3.4.1: one row per `DISCARDED` log, newest first. `discardedOn` = discard date (KST). `amount` = `cost_amount`, null without a price; `totalAmount` sums the non-null amounts and equals API 25 `discardedAmount`. Empty month: 200 with an empty list | `?month=2026-11` (within the last 6 months) | `200 { month, totalAmount, items: [{ name, categoryCode, discardedOn, amount }] }` | |

No update endpoint for registered receipts. `GET /user/expenses/monthly` and `/monthly/analysis` were removed (merged into API 22 and 25).

## 4. AI recipes (owner: 김종수)

| No. | Method | Path | Description | Request | Response | Events |
| --- | --- | --- | --- | --- | --- | --- |
| 27 | GET | `/user/usage-quotas` | Remaining counts. Recipes per day (basic 2, premium 4, + rewards); meal plans this month (basic 1, premium 4) | - | `200 { recipe: { limit, used, bonusRemaining, remaining }, mealPlan: { limit, used, remaining, maxDays } }` | |
| 28 | POST | `/user/recipes/generate` | Generate a recipe (one response, no streaming). Charged only on success. Quick uses the household size as servings. Custom options are single choices. With recipe supplement off, only my fridge ingredients are used and too few ingredients fail (not charged; failure screen to be added). With it on, missing ingredients are added | Quick `{ mode: "QUICK" }` / Custom `{ mode: "CUSTOM", servings, ingredientIds: [], taste?, cookTime?, tool?, method?, cuisine?, allergenCodes: [] }` | `201` (recipe detail shape) | quota_exceeded, recipe_generated, recipe_generate_failed |
| 29 | POST | `/user/recipes/{id}/regenerate` | New recipe with the same conditions. Always allowed; uses the daily count | no body | `201` (recipe detail shape) | quota_exceeded, recipe_generated, recipe_generate_failed |
| 30 | GET | `/user/recipes/{id}` | Recipe detail. No "AI가 생성한 레시피" text. `shareable` = not shared this week | - | `200 { id, title, servings, cookMinutes, tips, ingredients: [{ name, amount, unit }], steps: [{ stepNo, content }], saved, favorite, shareable }` | |
| 31 | GET | `/user/recipes/{id}/cook-preview` | My fridge ingredients to deduct and recipe amounts. Each recipe line is paired with my non-expired fridge ingredient of the same name that expires first; water, PINCH, TO_TASTE are skipped. `amount` and `unit` are in the fridge unit, capped at `remainingAmount`. `amount` is null when the kinds differ and cannot be converted (1 EA vs 300 g, a receipt `PACK` stock vs g, #145): the user sets it | `?mealPlanSlotId=` (when opened from a meal slot) | `200 { items: [{ ingredientId, name, imageUrl, amount, unit, remainingAmount }] }` | |
| 32 | POST | `/user/recipes/{id}/cook` | Deduct checked ingredients as USED. Opened from today's meal slot -> mark that slot cooked. After deduction the app goes to the fridge (p2) and shows the bottom sheet "이 레시피 어땠나요?" (추천해요 / 추천하지 않아요 → 보내기); the answer is collected only as event E50 through API 56 (no API or table) | `{ items: [{ ingredientId, amount }], mealPlanSlotId? }` | `200 { deductedCount }` | ingredient_used, cook_completed |
| 33 | GET | `/user/saved-recipes` | Saved list. AVAILABLE = can be made with the current fridge | `?filter=ALL\|FAVORITE\|AVAILABLE &sort=SAVED_DESC\|SAVED_ASC &page=0&size=20` | `200 { totalCount, content: [{ recipeId, title, ingredientSummary, extraIngredientCount, favorite }], page, size, totalElements, hasNext }` | |
| 34 | PUT | `/user/saved-recipes/{recipeId}` | Save my recipe (idempotent). Missing ingredients added by the supplement go to the cart | no body | `200 { saved: true, cartAddedCount }` | recipe_saved, cart_item_added |
| 35 | DELETE | `/user/saved-recipes/{recipeId}` | Remove from the saved list only. Shared posts stay | - | `204` | |
| 36 | PUT | `/user/saved-recipes/{recipeId}/favorite` | Favorite on | no body | `200 { favorite: true }` | |
| 37 | DELETE | `/user/saved-recipes/{recipeId}/favorite` | Favorite off (stays saved) | - | `200 { favorite: false }` | |

Custom option codes (one per item):

| Item | Codes |
| --- | --- |
| taste | MILD / SPICY / SWEET / SALTY / SOUR / NUTTY |
| cookTime | ANY / WITHIN_15 / WITHIN_30 / WITHIN_60 |
| tool | FRYING_PAN / POT / MICROWAVE / AIR_FRYER / OVEN / RICE_COOKER |
| method | STIR_FRY / GRILL / STEAM / BRAISE / SOUP_STEW / SEASONED / DEEP_FRY |
| cuisine | KOREAN / WESTERN / CHINESE / JAPANESE / BUNSIK / SIMPLE |
| Others | Units, categories, excluded ingredients, meal purposes, meals, report reasons, notification types: ERD Appendix C |

## 5. Meal plan (owner: 한윤수)

| No. | Method | Path | Description | Request | Response | Events |
| --- | --- | --- | --- | --- | --- | --- |
| 38 | POST | `/user/meal-plans/generate` | Generate a plan **asynchronously**: check the start date, quota, and overlap, create the plan as `GENERATING`, and reply `202` at once. A server job generates it and sends an in-app notification on completion (`MEAL_PLAN_COMPLETED`) or failure (`MEAL_PLAN_FAILED`). Too few ingredients, AI error, and timeout are failure notifications, not responses. Start date today or later. If dates overlap an existing plan and `overwrite` is not true, reject with the overlapping dates; the frontend shows a warning modal and resends with `overwrite: true`. Overwriting keeps today's meals already cooked. With too few ingredients, generate only the days possible; missing ingredients go to the cart. Charged (monthly count) only when the job succeeds. A new request while one of mine is `GENERATING` is rejected with `409 MEAL_PLAN_IN_PROGRESS` (error toast). Overwrite deletes the old overlapping slots when the job completes, so a failed job keeps the existing plan. (deferred, #144): whether `GENERATING`/`FAILED` plans show in the calendar (39) or count for overlap | `{ startDate, purpose: "BALANCED"\|"DIET"\|"HIGH_PROTEIN"\|"SIMPLE"\|"SAVING", mealTypes: ["BREAKFAST","LUNCH","DINNER"] (1+), servings, overwrite? (default false) }` | `202 { mealPlanId, status: "GENERATING" }` / overlap `409 MEAL_PLAN_OVERLAP` (`details.overlappingDates`) / already generating `409 MEAL_PLAN_IN_PROGRESS` | quota_exceeded, meal_plan_generated / meal_plan_generate_failed (when the job ends), cart_item_added |
| 39 | GET | `/user/meal-plans/calendar` | Dates with meals and plan periods in the month. Past months allowed | `?month=2026-11` | `200 { month, periods: [{ mealPlanId, startDate, endDate }], datesWithMeals: [] }` | |
| 40 | GET | `/user/meal-plans/days/{date}` | Meals of the day | - | `200 { date, slots: [{ slotId, mealType, recipeId, title, ingredientSummary, extraIngredientCount, favorite, cooked }] }` | |
| 41 | DELETE | `/user/meal-plan-slots/{slotId}` | Delete a slot. Today or later only | - | `204` | meal_slot_deleted |
| 42 | POST | `/user/meal-plan-slots/{slotId}/replace` | Replace with a new recipe. Uses one daily recipe count. Today or later only | no body | `200 { slotId, recipeId, title, ingredientSummary, extraIngredientCount }` | quota_exceeded, meal_slot_replaced, meal_slot_replace_failed |

No manual "add meal to an empty slot" and no lazy slot-detail generation: every slot gets a full recipe at generation.

## 6. Notifications (owner: 이상호)

| No. | Method | Path | Description | Request | Response | Events |
| --- | --- | --- | --- | --- | --- | --- |
| 43 | GET | `/user/notifications/summary` | Latest notification text and unread count | - | `200 { latestMessage, unreadCount }` | |
| 44 | GET | `/user/notifications` | Notification list, newest first. Types `EXPIRY_SOON`, `EXPIRED`, `MEAL_PLAN_COMPLETED`, `MEAL_PLAN_FAILED`. Tap: expiry items do nothing; `MEAL_PLAN_COMPLETED` opens p5 at the plan's start date (`mealPlanId`); `MEAL_PLAN_FAILED` opens the meal-plan setup screen. Loading this list marks all unread as read (no separate read API) | `?page=0&size=20` | `200 { content: [{ id, type, message, mealPlanId, createdAt }], page, size, totalElements, hasNext }` (`mealPlanId` only for `MEAL_PLAN_COMPLETED`, otherwise null) | |

In-app only: no push subscription APIs.

## 7. Sharing and cart (owner: 최유리)

| No. | Method | Path | Description | Request | Response | Events |
| --- | --- | --- | --- | --- | --- | --- |
| 45 | GET | `/user/cart-items` | Cart list | - | `200 { items: [{ id, recipeId, recipeTitle, name, categoryCode, imageUrl, amount, unit }] }` | |
| 46 | PATCH | `/user/cart-items/{id}` | Change amount | `{ amount }` | `200 { id, amount, unit }` | |
| 47 | POST | `/user/cart-items/move-to-fridge` | Put the chosen items into the fridge without input. Storage and expiry from category defaults, no price. Moved items leave the cart | `{ ids: [] }` | `200 { addedCount }` | ingredient_added, cart_moved_to_fridge |
| 48 | POST | `/user/cart-items/delete` | Delete selected | `{ ids: [] }` | `204` | |
| 49 | GET | `/user/shared-recipes/weekly-top` | TOP 3 by saves this week | - | `200 { items: [{ rank, id, title, imageUrl, ingredientSummary, extraIngredientCount }] }` | |
| 50 | GET | `/user/shared-recipes/ranking` | Popular = TOP 100 by saves this week, rank change vs last week's rank. Latest = by shared time, "—" instead of a rank | `?sort=popular\|latest &q= &page=0&size=20` | `200 { weekLabel, content: [{ id, rank?, rankChange?, title, imageUrl, ingredientSummary, extraIngredientCount, sharedAt }], page, size, totalElements, hasNext }` | |
| 51 | POST | `/user/shared-recipes` | Share. Title (1–100 UTF-16 code units, nonblank) and one photo required, no confirm modal. The same recipe once per week | multipart `image` + `payload { recipeId, title }` | `201 { id }` | shared_recipe_published |
| 52 | GET | `/user/shared-recipes/{id}` | Shared detail. Fully anonymous (no author info). `have` per ingredient (red border when I do not have it) | - | `200 { id, title, imageUrl, servings, cookMinutes, tips, ingredients: [{ name, amount, unit, have }], steps: [{ stepNo, content }], saveCount, saved }` | |
| 53 | PUT | `/user/shared-recipes/{id}/save` | Save (the original goes into my list, no copy). Missing ingredients go to the cart | no body | `200 { saveCount, saved: true, cartAddedCount }` | recipe_saved, cart_item_added |
| 54 | DELETE | `/user/shared-recipes/{id}/save` | Unsave (cart items stay) | - | `200 { saveCount, saved: false }` | |
| 55 | POST | `/user/shared-recipes/{id}/reports` | Report. No text input even for OTHER; an operator reviews. One report per post per member | `{ reason: "OFFENSIVE_IMAGE"\|"AD_TITLE"\|"DANGEROUS_RECIPE"\|"OTHER" }` | `201` | shared_recipe_reported |

Photo delivery for API 52: `GET /api/v1/user/shared-recipe-photos/{id}`, where `id` is the shared post ID. Requires login and a VISIBLE post; returns the image bytes with its MIME/length and `Cache-Control: no-store`. Missing/hidden posts or missing files return `SHARED_RECIPE_NOT_FOUND` (404). `imageUrl` points at this endpoint, never a storage key or public static directory.

Receipt integration contract (#57, agreed 2026-11-24): API 21 accepts 0–1 `cartDeductions` entries per receipt item; omitted/empty means no deduction. The receipt UseCase validates positive quantities and each item's deduction <= its actual fridge registration amount. It then calls `DeductCartItemsUseCase.deduct(Long userId, List<CartDeductionCommand> deductions)` inside the registration transaction. Each command has `cartItemId`, final `ingredientName`, `BigDecimal amount`, and server-decided `IngredientUnit unit`. The cart UseCase validates ownership and exact name/unit equality, sums requests for the same target, and rejects a total above its current balance. No unit conversion. Its `int` result is the number of rows deleted at zero (`cartRemovedCount`), not the deducted quantity or request count. Partial deduction returns 0. Any failure rolls back receipt, fridge, and cart together. Missing/foreign target -> `CART_ITEM_NOT_FOUND`; name/unit/quantity mismatch -> `INVALID_INPUT`; insufficient remaining amount -> `INSUFFICIENT_QUANTITY` with `details.remaining`. This describes the integration contract; the receipt endpoint and frontend integration are separate implementations.

Cart items cannot be added manually. No "take down my shared post" API yet; authors may delete their own posts (11/24 terms decision), see open questions.

Cart contract for APIs 34, 35, 38, 41, 45, 48, 53 and 54 (#57 and the subsequent approvals cited in the domain rules):

- API 45 returns flat `items`, with non-null positive `amount` (`numeric(10,3)`). Source-slot metadata does not require a new response field; keep the documented response shape.
- Persist the original recipe ID for every add path. The nullable slot FK is initial source metadata, not a list of all meal assignments. Slot deletion clears this FK only; unsaving my/shared recipes and removing meal assignments keep cart rows and edited quantities. API 48 deletes only the selected owned cart IDs and does not unsave recipes or remove meal assignments.
- Match existing rows by user, original recipe ID, exact ingredient name and unit. A new save after unsaving adds only absent missing ingredients; keep already-saved PUT retries idempotent. Before meal-plan additions, aggregate repeated occurrences of the same recipe/name/unit within that one generation. Preserve pre-existing rows and count only new rows in `cartAddedCount` and E62.
- Exclude ingredients with `amount=null`, PINCH or TO_TASTE from all three cart add paths; keep them in recipe responses and do not substitute an arbitrary quantity. This is confirmed by the [cart owner in PR #216](https://github.com/likelion-backendschool-9th/REMAKE/pull/216#issuecomment-7123595032); API 45 keeps its non-null amount contract.
- API 47 preserves quantities: keep the fridge's nine units, and convert TBSP/TSP/CUP to ML with factors 15/5/200 only. Reject overflow with `INVALID_INPUT`; all changes roll back. Storage/expiry still use category defaults and price is null. API 47 uses `RegisterCartIngredientsUseCase` to register each row and emit E22 with `add_source=cart`, `screen=cart_list`, then emits E82 with the actual row count and `was_select_all`.
- APIs 47/48 require non-empty distinct positive `ids`; reject any missing/foreign target without changing other rows. API 46 takes only a positive `amount` within `numeric(10,3)`, leaving the unit and source unchanged. API 45 returns rows by ascending cart ID and can show source titles even after a recipe is unsaved.
- Internal add contract: `AddCartItemsUseCase.add(Long userId, CartSource source, List<CartAdditionCommand> items)` returns actual inserted rows. Commands contain original `recipeId`, optional initial `mealPlanSlotId`, final `ingredientName`, `amount`, `RecipeUnit unit`, and optional server-decided `categoryCode`. The caller validates source ownership/visibility and supplies the final fridge-shortage list; this UseCase does not recalculate fridge shortages. Sources are `OWN_RECIPE`, `SHARED_RECIPE`, `MEAL_PLAN`. Exact catalog categories take precedence over the fallback category; no catalog match and no fallback -> `INVALID_INPUT`. Skip null amounts, PINCH, TO_TASTE and water. MEAL_PLAN aggregates duplicate recipe/name/unit keys within the call and retains the first supplied slot source; other sources keep the first occurrence. Existing rows are never overwritten. DB uniqueness prevents concurrent duplicate additions. E62 uses the lowercase values `own_recipe`, `shared_recipe`, `meal_plan`, once per call with actual additions; zero additions emit nothing. Recipe/meal-plan integration remains in #76/#90/#91.
- Consult [domain rules](domain-rules.md#cart) for pending slot replacement/overwrite integration. This does not permit automatic cart deletion.

## Common: events (owner: 이준호)

| No. | Method | Path | Description | Request | Response |
| --- | --- | --- | --- | --- | --- |
| 56 | POST | `/user/events` | Frontend event batch (max 50). Names and properties in `events.md`. `platform` and `app_version` come from the common headers | `{ events: [{ name, sessionId, screen, properties, occurredAt }] }` | `202 { accepted, rejected }` |

## Handled without an API

| Screen | Handling |
| --- | --- |
| p1 "recent login" bubble | Last provider stored on the device (localStorage) |
| p1.2, p1.3.2 allergy chips (19) | Frontend constants (ERD Appendix B codes) |
| Terms drawer (terms of service, privacy policy, payment terms) | Static text from the docs terms md (11/24 terms decision) |
| p4.1.1 "play steps" | Device speech synthesis reads the step text |
| p3.4 month tabs | The frontend builds the last 6 months from today |
| p3.3.1, p3.3.2 OCR item edit/add | Not stored before registration; screen state only, sent with API 21 |
| p7 select all, delete mode | Frontend state |
| p1.3.4 payment terms checkbox, "details" | Bottom button disabled until checked (frontend). Payment terms text in the terms drawer |
| Toss payment window cancel or failure | Toss returns to the frontend failure URL with `code` and `message`. No server call; go back to the p1.3.4 before-subscribe screen with the common toast |

## Scheduled server jobs (not APIs)

| Job | When | What | Tables | Events |
| --- | --- | --- | --- | --- |
| Expiry notifications | Daily 09:00 | EXPIRY_SOON per ingredient (D-3 to D-DAY), EXPIRED for expired ingredients (one per day). Skip members with notifications off. In-app only | notifications | notification_sent, ingredient_expired |
| Weekly recipe reward | Monday 00:00 | +10 / +6 / +3 to the members who shared last week's top 1-3 recipes by saves (per recipe rank), valid one month. Store last week's TOP 100 as rank records | generation_quota_grants, shared_recipe_weekly_ranks | weekly_reward_granted |
| Receipt cleanup | Daily early morning | Delete receipts older than 6 months by purchase date with items and images | receipts, receipt_items | |
| Notification and code cleanup | Daily early morning | Delete notifications older than 30 days and deletion codes older than a day | notifications, account_deletion_requests | |
| Subscription auto-payment | Daily early morning | Charge 9,900 KRW with the billingKey for ACTIVE subscriptions whose next billing date is today; on success extend by a month. CANCELED subscriptions are not charged and end (ENDED, `users.plan` BASIC, billingKey cleared). On payment failure end the subscription at once (ENDED, `users.plan` BASIC, billingKey cleared) (11/24 terms decision) | subscriptions, subscription_payments, users | subscription_renewed, subscription_payment_failed, subscription_ended |

## Open questions (do not decide in code; ask the PO)

- What the subscription screen shows after scheduling cancellation, and whether a confirm modal is needed.
- Which API and flow handle the 7-day full refund (Toss payment cancel, subscription status, events)?
- Which API and screen let an author delete their own shared post?
- How report sanctions (sharing bans, account suspension) are stored and returned to the frontend (tables, error codes)?
- Which E88 `end_reason` value marks an auto-payment failure ending?
