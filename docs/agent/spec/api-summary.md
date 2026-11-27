# API summary

Source: API definition v4 (Figma-based, Korean, kept in Notion; not in the repo). 71 APIs: 1-60 from v4 and the 11 admin APIs 61-71 added on 11/26 from the Figma "운영자 페이지" (operator page). Compared with v3: 21 kept, 36 changed, 18 removed, 14 added. The 15 admin APIs that were on hold were resolved as 2 kept, 7 changed, 6 removed (see "Admin: changes from the previous admin APIs"). API 72 (discarded food, #248) was added in the repo after v4. Other spec files refer to APIs by these numbers. Error codes per API are in `errors.md`; events are in `events.md`.

## Common

| Item | Rule |
| --- | --- |
| Path | `/api/v1/{role}/{resource}[/{id}[/{action}]]`. Role `public` (before login), `user` (after login), or `admin` (operator, ADMIN role only). Resources are plural kebab-case; actions are trailing verbs (`generate`, `cook`). No `me` under `user` |
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
| Admin access | `/api/v1/admin/**` is for `users.role = ADMIN` only; otherwise 403 `FORBIDDEN` and the frontend shows the a0 "no permission" screen. Login is the app's social-login cookie as is. The frontend decides in advance from `role` in `GET /user/profile` (API 5) |
| Admin period and seed | Monitoring APIs (61-63) share `?period=TODAY\|LAST_7_DAYS\|LAST_30_DAYS` (default `LAST_7_DAYS`). With `?excludeSeed=true` they count only `events.is_seed = false` (API 64 too) |
| Admin personal data | Members appear only as member numbers (`users.id`). Responses never include email or social IDs |
| Admin empty values | When the denominator is 0 or there is no data, the value is null (the frontend shows "-" and hides the target badge). An empty list returns an empty `content`, not an error |
| Payments (Toss Payments) | Card auto-billing. The frontend only opens the card registration window with the client key. The server issues the billingKey (`POST /v1/billing/authorizations/issue`) and charges (`POST /v1/billing/{billingKey}`) with the secret key. Never send the billingKey or secret key to the frontend. The server makes the `orderId` |
| Numbering | Subscription APIs (57-60) belong to area 1 but are numbered last so other documents keep their API numbers. Admin APIs (61-71) are also numbered last for the same reason. API 72 (discarded food) was added in the repo after them |
| Removed | No `Idempotency-Key` header rule (the duplicate-request key table was removed) |

## 1. Login, onboarding, settings (owner: 이준호)

| No. | Method | Path | Description | Request | Response | Events |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | POST | `/public/auth/oauth/{provider}` | Social login and signup. provider = kakao, naver, google. Sets access and refresh tokens as HttpOnly cookies | `{ code, redirectUri }` | `200 { onboardingCompleted }` | login_succeeded, login_failed, signup_completed |
| 2 | POST | `/public/auth/refresh` | Reissue the access cookie from the refresh cookie | no body (refresh cookie) | `204` | |
| 3 | POST | `/user/auth/logout` | Delete the refresh token row and both cookies | no body | `204` | |
| 4 | PUT | `/user/onboarding` | Save onboarding. Sent once when p1.2 is completed, with both screens' values. Zero allergies allowed | `{ householdSize (1-50), allergenCodes: [] }` | `204` | onboarding_completed |
| 5 | GET | `/user/profile` | All settings screen values. `role` (USER/ADMIN) lets the frontend decide admin entry (11/26) | - | `200 { provider, role, plan, householdSize, excludedIngredientCount, recipeSupplementEnabled, notificationEnabled, lastMealPurpose, onboardingCompleted }` | |
| 6 | PATCH | `/user/profile` | Send only the changed values among household size, recipe supplement, notifications | `{ householdSize? (1-50), recipeSupplementEnabled?, notificationEnabled? }` | `200` (same shape as API 5) | |
| 7 | GET | `/user/excluded-ingredients` | My excluded-ingredient codes | - | `200 { allergenCodes: [] }` | |
| 8 | PUT | `/user/excluded-ingredients` | Replace the whole set. Zero allowed (settings "done" stays enabled) | `{ allergenCodes: [] }` | `200 { allergenCodes }` | |
| 9 | POST | `/user/account/deletion-code` | Send a 6-digit code to the Kakao/Google member's email (valid 5 minutes). Resend uses the same API | no body | `201 { expiresAt }` | |
| 10 | POST | `/user/account/deletion` | Delete the account. Kakao/Google confirm with the email code, Naver with a re-authentication code. Clears cookies on success | Kakao/Google `{ method: "EMAIL_CODE", code }` / Naver `{ method: "REAUTH", oauthCode, redirectUri }` | `204` | account_deletion_requested, account_deleted, subscription_ended |
| 57 | GET | `/user/subscription` | My subscription. NONE -> before-subscribe screen, ACTIVE -> subscribed screen. Before subscribing, the next billing date is today + 1 month | - | `200 { status: NONE\|ACTIVE\|CANCELED, price, nextBillingDate, currentPeriodEnd? }` | |
| 58 | POST | `/user/subscription/billing-auth` | Values to open the Toss card registration window. The frontend calls Toss SDK `requestBillingAuth`; Toss returns to the frontend success URL with `authKey` and `customerKey` | no body | `200 { customerKey, orderName, amount }` | |
| 59 | POST | `/user/subscription` | Start the subscription: issue a billingKey with `authKey`, charge the first month (9,900 KRW), and on success start it (`users.plan` PREMIUM, next billing date = today + 1 month). If the first payment fails, no subscription is created | `{ authKey, customerKey }` | `201` (same shape as API 57) | subscription_started, subscription_payment_failed |
| 60 | POST | `/user/subscription/cancel` | Schedule cancellation. Benefits stay until the day before the next billing date; on that date no charge and the plan becomes BASIC. Already scheduled -> 200 unchanged | no body | `200` (same shape as API 57, status CANCELED) | subscription_canceled |
| — | — | refund request API (path fixed in the implementation issue) | 7-day full refund when premium benefits were unused since payment (meal-plan generations = 0 and AI recipe generations above BASIC daily limit = 0). On success: Toss cancel, subscription ENDED, `users.plan` BASIC, billingKey cleared; keep `subscription_payments` rows. Requested from the Figma refund page (#228, #318) | (TBD with path) | (TBD with path) | subscription_refunded |

## 2. Fridge (owner: 김재연)

| No. | Method | Path | Description | Request | Response | Events |
| --- | --- | --- | --- | --- | --- | --- |
| 11 | GET | `/user/ingredients/summary` | Total and per-status counts (independent of filters) | - | `200 { total, danger, soon, ok }` | |
| 12 | GET | `/user/ingredients` | My ingredients, soonest expiry first. **No search** (not in Figma). Not paged | `?status=DANGER\|SOON\|OK &storage=FRIDGE\|FREEZER\|PANTRY &categoryCode=` | `200 { items: [{ id, name, categoryCode, imageUrl, storage, amount, unit, expirationDate, dDay, status }] }` | |
| 13 | GET | `/user/ingredients/{id}` | One ingredient. `unitFollowsName` is `true` when the current unit equals the dictionary unit of the current name and category, so editing the name or category re-picks the unit (preview it with API 14). It is `false` for an ingredient that keeps a non-dictionary unit (cart transfer, receipt `PACK`); the edit screen then shows the current unit and does not call API 14. The same shape and field are returned by API 15 and 16 | - | `200 { id, name, categoryCode, imageUrl, storage, amount, unit, expirationDate, dDay, status, price, unitFollowsName }` | |
| 14 | GET | `/user/ingredient-units` | The backend picks the unit for an ingredient name. The quantity field stays disabled until a name is chosen; then the frontend calls this and enables it. Call again when the name or category changes, including on the OCR item edit screen (p3.3.1) before registration. Uses `ingredient_catalog` with the same rule as saving (15, 16, 21). `categoryCode` is optional: with it the result equals the unit that will be saved (for example "유기농 우유" + DAIRY_EGG -> `PACK`); without it only an exact name match gives the dictionary unit, otherwise `EA`. Send `categoryCode` whenever the screen has one (add p2.2 and OCR edit p3.3.1 require it before saving). An empty `name` or an unknown `categoryCode` is `INVALID_INPUT` | `?name=대파&categoryCode=VEGETABLE` | `200 { unit }` | |
| 15 | POST | `/user/ingredients` | Manual add. Category required (no "all"). The request has no `unit`: the server picks it from the name and category (same rule as API 14 with `categoryCode`, so the preview equals the saved unit) and returns it. Price optional (included in consumption patterns when set) | `{ name, categoryCode, storage, expirationDate, amount, price? }` | `201` (one-ingredient shape) | ingredient_added |
| 16 | PATCH | `/user/ingredients/{id}` | Send only changed values. Changing the name or the category makes the server pick the unit again with the new values and return it (e.g. 대파 1 BUNCH -> 양파 1 EA; the number stays; the result equals API 14 for the same name and category), but only when `unitFollowsName` was `true` before the edit. If it was `false` (unit from the cart or receipt exception, e.g. 양파 200 G, 삼겹살 2 PACK), the unit and amount stay as they are (#233). Sending the same name and category again changes nothing. Also when the ingredient has use/discard logs (logs keep their own name and unit). `amount` fixes an input mistake: it sets both `amount` and `initial_amount`, and is rejected with `INVALID_INPUT` if the ingredient has any use/discard log (#128). When `price` actually changes (a new value or `null`), the same transaction updates the price of the ingredient's `manual_ingredient_expenses` row (registration date stays; receipt ingredients have no such row, so nothing changes). The same price again, or no `price`, does not touch it (#278) | `{ name?, categoryCode?, storage?, expirationDate?, price?, amount? }` | `200` (one-ingredient shape) | |
| 17 | DELETE | `/user/ingredients/{id}` | Delete the ingredient. Use/discard logs stay with the name | - | `204` | |
| 18 | POST | `/user/ingredients/{id}/consume` | Use or discard. Discarding computes the cost when a price exists. Deletes the ingredient at 0 | `{ type: "USED"\|"DISCARDED", amount }` | `200 { remainingAmount, deleted, costAmount? }` | ingredient_used, ingredient_discarded |
| 19 | GET | `/user/ingredient-categories` | 12 categories | - | `200 { items: [{ code, name, imageUrl }] }` | |

`GET /user/ingredient-catalog` (autocomplete) is not part of the API. `ingredient_catalog` is used only inside the server (11/22 decision).

## 3. Receipts and consumption patterns (owner: 이동준)

| No. | Method | Path | Description | Request | Response | Events |
| --- | --- | --- | --- | --- | --- | --- |
| 20 | POST | `/user/receipts/scan` | OCR analysis only, nothing stored. Synchronous. Returns food only. A receipt with no food items, older than 6 months, or already registered (same store (branch name), receipt number, and purchase time; if the number is unreadable compare store, purchase date, and food names; if the time is unreadable compare by purchase date) fails and is shown on the OCR failure screen. No `dDay`/`status`: the result is not stored yet and the user edits the expiry, so the frontend computes them from `expirationDate` with the KST date (11/25) | multipart `image` (jpg, png, heic, 10 MB) | `200 { storeName, storeCode, purchasedOn, purchasedTime, receiptNumber, items: [{ name, categoryCode, storage, amount, unit, price, expirationDate, purchaseQuantity?, volume?: { amount, unit, scope } }] }` | receipt_scan_succeeded, receipt_scan_failed |
| 21 | POST | `/user/receipts` | Save receipt, items, and fridge ingredients at once. The server picks units from names. Only selected cart items are deducted (#57); skipping leaves the cart unchanged, and cartRemovedCount counts rows actually deleted at zero. Receipts older than 6 months cannot be registered. Duplicates are blocked again. After delete, re-registering the same purchase with a `receiptNumber` restores the receipt/spend but skips fridge and cart (`ingredientCount`/`cartRemovedCount` 0) when `receipt_first_applications` already recorded the first apply (#266); without a number, full register again | multipart `image` + `payload { storeName, storeCode, purchasedOn, purchasedTime, receiptNumber, items: [{ name, categoryCode, storage, amount?, purchaseQuantity?, volume?: { amount, unit, scope }, price, expirationDate, cartDeductions?: [{ cartItemId, amount }] }] }` | `201 { receiptId, ingredientCount, cartRemovedCount }` | ingredient_added, receipt_saved |
| 22 | GET | `/user/receipts` | Receipts of the month. Month total from receipt items only; average = average monthly total of the last 6 months | `?month=2026-11` (within the last 6 months) | `200 { month, totalAmount, averageDiffAmount, weeks: [{ label, startDate, endDate, totalAmount, receipts: [{ id, storeCode, storeName, purchasedOn, itemCount, totalAmount }] }] }` | |
| 23 | GET | `/user/receipts/{id}` | One receipt | - | `200 { id, storeCode, storeName, purchasedOn, imageUrl, items: [{ name, categoryCode, imageUrl, storage, amount, unit, price }], totalAmount }` | |
| 24 | DELETE | `/user/receipts/{id}` | Delete receipt, items, image. It leaves spending and patterns; fridge ingredients stay | - | `204` | |
| 25 | GET | `/user/expenses/patterns` | Consumption of the month: `receipt_items` by purchase date + priced `manual_ingredient_expenses` by registration date. `discardedAmount` = discarded cost of the month (sum of non-null `ingredient_logs.cost_amount`, #248) | `?month=2026-11` (within the last 6 months) | `200 { month, totalAmount, averageDiffAmount, discardedAmount, categories: [{ categoryCode, amount, ratio }], dailyTotals: [{ date, amount }] }` | |
| 26 | GET | `/user/expenses/daily-items` | Items of the chosen day from `receipt_items` and `manual_ingredient_expenses` | `?date=2026-11-20` | `200 { date, totalAmount, items: [{ name, categoryCode, imageUrl, storage, amount, unit, price }] }` | |
| 72 | GET | `/user/expenses/discarded-items` | Discarded food of the month for p3.4.1 (#248): one row per `DISCARDED` log via the fridge UseCase, newest first. `discardedOn` = discard date (KST). `amount` = `cost_amount`, null without a price; `totalAmount` sums the non-null amounts and equals API 25 `discardedAmount`. Empty month: 200 with an empty list | `?month=2026-11` (within the last 6 months; omit for the current KST month) | `200 { month, totalAmount, items: [{ name, categoryCode, discardedOn, amount }] }` | |

API 25 defaults to the current KST month. Use the same six calendar months for API 25, 26, and 61. An empty day returns zero and no items. Category ratios use the 0–1 scale. The average excludes the current KST month and months without expense records, as in API 22.

API 20 response fields `purchaseQuantity` and `volume` are planned in #72. Frontend integration will be verified in #101.

### Receipt quantity contract (#173)

- API 20 returns the preview `amount` and `unit` plus the observed `purchaseQuantity` and `volume` when available. If the preview amount cannot be resolved, ask the user to supply a quantity.
- API 21 accepts optional `purchaseQuantity` and `volume { amount, unit, scope }`. Use `G`, `KG`, `ML`, or `L` for `volume.unit`. `PER_PACK` multiplies the measured amount by the purchase quantity. `TOTAL` already represents the full quantity.
- Send a top-level item `amount` only for a final quantity entered by the user. The server saves it in the dictionary unit returned by API 14. Omit that `amount` to let the server resolve the quantity, including when accepting a `PACK` preview.
- For a weight or volume dictionary unit, convert compatible measurements. If the measurement is missing or of a different kind and the purchase quantity is known, store the purchase quantity as `PACK`. For count units, use the purchase quantity. An unresolved quantity returns `400 INVALID_INPUT` with `items[i].purchaseQuantity` in `fieldErrors`.
- Examples for dictionary unit `G`: 500 g `PER_PACK` × 2 → `1000 G`. 500 g `TOTAL` with purchase quantity 2 → `500 G`. 500 mL `PER_PACK` × 1 → `1 PACK`. An explicit `amount: 600` → `600 G`.

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
| 39 | GET | `/user/meal-plans/calendar` | Dates with meals and plan periods in the month. Past months allowed. Only `COMPLETED` plans count; `GENERATING`/`FAILED` plans are left out (#144). `hasAnyMealPlan`: whether the user has any `COMPLETED` plan in any month; `false` shows the empty screen (식단표 만들기) instead of the calendar | `?month=2026-11` | `200 { month, hasAnyMealPlan, periods: [{ mealPlanId, startDate, endDate }], datesWithMeals: [] }` | |
| 40 | GET | `/user/meal-plans/days/{date}` | Meals of the day | - | `200 { date, slots: [{ slotId, mealType, recipeId, title, ingredientSummary, extraIngredientCount, favorite, cooked }] }` | |
| 41 | DELETE | `/user/meal-plan-slots/{slotId}` | Delete a slot. Today or later only | - | `204` | meal_slot_deleted |
| 42 | POST | `/user/meal-plan-slots/{slotId}/replace` | Replace with a new recipe. Uses one daily recipe count. Today or later only | no body | `200 { slotId, recipeId, title, ingredientSummary, extraIngredientCount, cartAddedCount }` (`cartAddedCount`: new cart rows from the new recipe's missing ingredients, #91; toast only when 1 or more) | quota_exceeded, meal_slot_replaced, meal_slot_replace_failed, cart_item_added |

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
| 49 | GET | `/user/shared-recipes/weekly-top` | Up to 3 visible posts with at least one valid save this week; return 0–2 if fewer qualify, never fill with zero-save posts | - | `200 { items: [{ rank, id, title, imageUrl, ingredientSummary, extraIngredientCount }] }` | |
| 50 | GET | `/user/shared-recipes/ranking` | Popular = up to 100 posts with at least one valid save this week, rank change vs last week's rank. Latest = all visible posts regardless of save count, by shared time, "—" instead of a rank | `?sort=popular\|latest &q= &page=0&size=20` | `200 { weekLabel, content: [{ id, rank?, rankChange?, title, imageUrl, ingredientSummary, extraIngredientCount, sharedAt }], page, size, totalElements, hasNext }` | |
| 51 | POST | `/user/shared-recipes` | Share. Title (1–100 UTF-16 code units, nonblank) and one photo required, no confirm modal. The same recipe once per week | multipart `image` + `payload { recipeId, title }` | `201 { id }` | shared_recipe_published |
| 52 | GET | `/user/shared-recipes/{id}` | Shared detail. Fully anonymous (no author info). `have` per ingredient (red border when I do not have it) | - | `200 { id, title, imageUrl, servings, cookMinutes, tips, ingredients: [{ name, amount, unit, have }], steps: [{ stepNo, content }], saveCount, saved }` | |
| 53 | PUT | `/user/shared-recipes/{id}/save` | Save (the original goes into my list, no copy). Missing ingredients go to the cart using the recipe line's category as fallback after an exact catalog match. An eligible shortage without either category fails the whole save with `INVALID_INPUT` (#245) | no body | `200 { saveCount, saved: true, cartAddedCount }` | recipe_saved, cart_item_added |
| 54 | DELETE | `/user/shared-recipes/{id}/save` | Unsave (cart items stay) | - | `200 { saveCount, saved: false }` | |
| 55 | POST | `/user/shared-recipes/{id}/reports` | Report. No text input even for OTHER; an operator reviews. One report per post per member | `{ reason: "OFFENSIVE_IMAGE"\|"AD_TITLE"\|"DANGEROUS_RECIPE"\|"OTHER" }` | `201` | shared_recipe_reported |

Photo delivery for API 52: `GET /api/v1/user/shared-recipe-photos/{id}`, where `id` is the shared post ID. Requires login and a VISIBLE post; returns the image bytes with its MIME/length and `Cache-Control: no-store`. Missing/hidden posts or missing files return `SHARED_RECIPE_NOT_FOUND` (404). `imageUrl` points at this endpoint, never a storage key or public static directory.

Receipt integration contract (#57, agreed 2026-11-24): API 21 accepts 0–1 `cartDeductions` entries per receipt item; omitted/empty means no deduction. The receipt UseCase validates positive quantities and each item's deduction <= its actual fridge registration amount. It then calls `DeductCartItemsUseCase.deduct(Long userId, List<CartDeductionCommand> deductions)` inside the registration transaction. Each command has `cartItemId`, final `ingredientName`, `BigDecimal amount`, and server-decided `IngredientUnit unit`. The cart UseCase validates ownership and exact name/unit equality, sums requests for the same target, and rejects a total above its current balance. No unit conversion. Its `int` result is the number of rows deleted at zero (`cartRemovedCount`), not the deducted quantity or request count. Partial deduction returns 0. Any failure rolls back receipt, fridge, and cart together. Missing/foreign target -> `CART_ITEM_NOT_FOUND`; name/unit/quantity mismatch -> `INVALID_INPUT`; insufficient remaining amount -> `INSUFFICIENT_QUANTITY` with `details.remaining`. This describes the integration contract; the receipt endpoint and frontend integration are separate implementations.

Shared-save category contract (agreed by the recipe owner in [#245](https://github.com/likelion-backendschool-9th/REMAKE/issues/245#issuecomment-7182102063), 2026-11-26): pass the persisted recipe line's `categoryCode` to the common add UseCase without reclassifying it. Exact catalog categories take precedence. After excluding water, null amounts, PINCH and TO_TASTE, no catalog category and no fallback means `INVALID_INPUT`: roll back the save and cart changes and emit no success events. Do not silently omit eligible shortages or invent ETC. Pre-#277 rows and invalid/missing AI categories can remain null; do not backfill them in #245. My-recipe saves currently skip unclassified lines; aligning the two save policies is separate follow-up work.

Cart items cannot be added manually. No "take down my shared post" API yet; authors may delete their own posts (11/24 terms decision), see open questions.

Cart contract for APIs 34, 35, 38, 41, 45, 48, 53 and 54 (#57 and the subsequent approvals cited in the domain rules):

- API 45 returns flat `items`, with non-null positive `amount` (`numeric(10,3)`). Source-slot metadata does not require a new response field; keep the documented response shape.
- Persist the original recipe ID for every add path. The nullable slot FK is initial source metadata, not a list of all meal assignments. Unsaving my/shared recipes keeps cart rows and edited quantities. Removing a meal assignment (41, 42, 38 overwrite) cleans up that slot's rows first (#91, see below). API 48 deletes only the selected owned cart IDs and does not unsave recipes or remove meal assignments.
- Match existing rows by user, original recipe ID, exact ingredient name and unit. A new save after unsaving adds only absent missing ingredients; keep already-saved PUT retries idempotent. Before meal-plan additions, aggregate repeated occurrences of the same recipe/name/unit within that one generation. Preserve pre-existing rows and count only new rows in `cartAddedCount` and E62.
- Exclude ingredients with `amount=null`, PINCH or TO_TASTE from all three cart add paths; keep them in recipe responses and do not substitute an arbitrary quantity. This is confirmed by the [cart owner in PR #216](https://github.com/likelion-backendschool-9th/REMAKE/pull/216#issuecomment-7123595032); API 45 keeps its non-null amount contract.
- API 47 preserves quantities: keep the fridge's nine units, and convert TBSP/TSP/CUP to ML with factors 15/5/200 only. Reject overflow with `INVALID_INPUT`; all changes roll back. Storage/expiry still use category defaults and price is null. API 47 uses `RegisterCartIngredientsUseCase` to register each row and emit E22 with `add_source=cart`, `screen=cart_list`, then emits E82 with the actual row count and `was_select_all`.
- APIs 47/48 require non-empty distinct positive `ids`; reject any missing/foreign target without changing other rows. API 46 takes only a positive `amount` within `numeric(10,3)`, leaving the unit and source unchanged. API 45 returns rows by ascending cart ID and can show source titles even after a recipe is unsaved.
- Internal add contract: `AddCartItemsUseCase.add(Long userId, CartSource source, List<CartAdditionCommand> items)` returns actual inserted rows. Commands contain original `recipeId`, optional initial `mealPlanSlotId`, final `ingredientName`, `amount`, `RecipeUnit unit`, and optional server-decided `categoryCode`. The caller validates source ownership/visibility and supplies the final fridge-shortage list; this UseCase does not recalculate fridge shortages. Sources are `OWN_RECIPE`, `SHARED_RECIPE`, `MEAL_PLAN`. Exact catalog categories take precedence over the fallback category; no catalog match and no fallback -> `INVALID_INPUT`. Skip null amounts, PINCH, TO_TASTE and water. MEAL_PLAN aggregates duplicate recipe/name/unit keys within the call and retains the first supplied slot source; other sources keep the first occurrence. Existing rows are never overwritten. DB uniqueness prevents concurrent duplicate additions. E62 uses the lowercase values `own_recipe`, `shared_recipe`, `meal_plan`, once per call with actual additions; zero additions emit nothing. Callers: my-recipe save (#76), meal-plan generation and slot replace (#91), shared-recipe save (#90).
- Meal-plan cleanup (#91): before slot delete (41), slot replace (42) or overwrite (38) removes a recipe from a slot, rows added from that slot and recipe are deleted if never changed, and otherwise (edited amount, receipt partial deduction, saved recipe) only lose the slot link. Replace then adds the new recipe's missing ingredients in the same transaction. See [domain rules](domain-rules.md#cart).

## Common: events (owner: 이준호)

| No. | Method | Path | Description | Request | Response |
| --- | --- | --- | --- | --- | --- |
| 56 | POST | `/user/events` | Frontend event batch (max 50). Names and properties in `events.md`. `platform` and `app_version` come from the common headers | `{ events: [{ name, sessionId, screen, properties, occurredAt }] }` | `202 { accepted, rejected }` |

## Admin: monitoring (owner: 이준호)

Source: Figma "운영자 페이지" (node 2005:4, desktop 1440), added 11/26. Metric formulas follow `events.md` section 5 (M00-M20). All admin APIs require the ADMIN role (see Common).

| No. | Method | Path | Description | Request | Response |
| --- | --- | --- | --- | --- | --- |
| 61 | GET | `/admin/dashboard` | a1 dashboard in one call (incl. anomaly and no-data states). A value is null when its denominator is 0 (the frontend shows "-" and hides the target badge). Anomalies cover only the OCR failure rate and the report count: one is returned when the last hour is at least twice the average of the same hour over the past 7 days (11/26) | `?period=TODAY\|LAST_7_DAYS\|LAST_30_DAYS&excludeSeed=true\|false` | `200 { northStar: { rate, diffFromLastWeek, weekly: [{ weekStart, rate }] } (M00, 8 weeks), pendingReportCount, discardedAmount: { amount, noPriceCount } (M18), kpis: [{ metricId, name, value, target, achieved }] (M01, M02, M03, M04, M06, M08, M09, M15 D7), anomalies: [{ metric, recentValue, usualValue, message }] }` |
| 62 | GET | `/admin/funnels` | a2 funnels: core flow (6 steps) and receipt path (3 steps) on one screen. `dropHigh = true` ("이탈 큼") when the rate from the previous step is below 70% (11/26) | `?period=TODAY\|LAST_7_DAYS\|LAST_30_DAYS&excludeSeed=true\|false` | `200 { core: [{ step, eventName, userCount, rateFromPrev, dropHigh }], receipt: [same shape] }` |
| 63 | GET | `/admin/metrics/{tab}` | a3 one feature-metrics tab. `tab` = `registration`, `consumption`, `ai`, `meal-plan-cart`, `shared`, `retention`, `cost`, `error` (contents below) | `?period=TODAY\|LAST_7_DAYS\|LAST_30_DAYS&excludeSeed=true\|false` | `200 { cards: [{ metricId, name, value, target, achieved }], tables: [{ key, rows }], daily: [{ date, value }] (7 days) }` |
| 64 | GET | `/admin/events` | a6 event log search, newest first. Members appear only as member numbers (no personal data). Raw events are kept 180 days | `?name=&userId=&source=CLIENT\|SERVER&excludeSeed=true\|false&page=0&size=20` | `200 { content: [{ id, occurredAt, eventName, userId, screen, source, sessionId, platform, appVersion, isSeed, properties }], page, size, totalElements, hasNext }` |

API 63 tabs (metric IDs are `events.md` section 5):

| `tab` | Screen | Contents |
| --- | --- | --- |
| `registration` | a3 등록 (5940:7728) | M03, M04, M05 cards; receipt scan results by `error_code` (count and ratio); daily `receipt_saved` |
| `consumption` | a3 소비 (5940:7895) | M00, M07, M06 cards; used, discarded, expired, and use-up rate by category; daily discarded amount (M18) |
| `ai` | a3 AI (5940:5932) | M09 attempts, failures, and success rate by kind (recipe, meal plan, slot replacement); M10 p50 and p95 (plus receipt OCR); M16 daily calls, quota-exhausted ratio, reward uses |
| `meal-plan-cart` | a3 식단표·장바구니 (5940:8074) | M11, M12, and meal-plan success rate (M09) cards; generations, slot views, deletions, and replacements by the 5 meal purposes; daily `cart_moved_to_fridge` |
| `shared` | a3 공유 (5940:8242) | M13 and M14 (save rate, report rate) cards; this week's top 5 by saves (views, saves, reports); daily `shared_recipe_published` |
| `retention` | a3 리텐션 (5940:8410) | M15 D1, D7, and WAU cards; cohorts by signup week (D1, D7, D14); weekly WAU |
| `cost` | a3 비용 (5940:8561) | M16 cards (today's calls, quota-exhausted ratio, reward uses); successes, failures, and calling users by kind (recipe generation, regeneration, meal plan, slot replacement, OCR); daily calls |
| `error` | a3 오류 (5940:8717) | M20 error exposure rate; most frequent `error_code` and screen; counts by `error_code` x screen; daily error exposures |

## Admin: operations (owner: 이상호)

| No. | Method | Path | Description | Request | Response |
| --- | --- | --- | --- | --- | --- |
| 65 | GET | `/admin/reports` | a4 report list (pending, done, empty). Reports on the same post are one row (per post), most reports first. `pendingCount` is also the side-menu number | `?status=PENDING\|DONE&page=0&size=20` | `200 { pendingCount, content: [{ sharedRecipeId, title, reportCount, reasonCounts: [{ reason, count }], lastReportedAt, authorViolationCount, status, result?, sanction?: { level, type, days }, handledAt?, handledBy? }], page, size, totalElements, hasNext }` |
| 66 | GET | `/admin/reports/{sharedRecipeId}` | a4.1 report detail: the shared recipe as users see it, its reports, the author's violations and current sanction, and the sanction a violation would apply next (`nextSanction` is null after the 4th ban) | - | `200 { sharedRecipe: { id, title, imageUrl, createdAt, saveCount, recipe (ingredients, steps, servings, time) }, status, reports: [{ reportedAt, reason, reporterId }], author: { userId, violationCount, currentSanction? }, nextSanction: { level, type, days (null when permanent) }, handled?: { result, handledAt, handledBy, sanction? } }` |
| 67 | PATCH | `/admin/reports/{sharedRecipeId}` | Handle the reports of one post. `VIOLATION` = hide the post + next sanction, `NO_ISSUE` = unchanged, `DELETED` = delete the post and count it as a violation (next sanction). 409 when the post has no pending report | `{ result: VIOLATION\|NO_ISSUE\|DELETED }` | `200 { result, sanction?: { level, type, endsAt } }` |
| 68 | GET | `/admin/users` | a5 user list and member-number search | `?filter=ALL\|SANCTIONED&userId=&page=0&size=20` | `200 { content: [{ userId, provider, joinedAt, plan, violationCount, sanction?: { type, endsAt }, deletedAt? }], page, size, totalElements, hasNext }` |
| 69 | GET | `/admin/users/{userId}` | a5 user detail panel (no sanction, permanent ban, withdrawn) | - | `200 { userId, provider, joinedAt, deletedAt?, quota: { todayRemaining, rewardRemaining, blocked }, currentSanction?, sanctions: [{ level, type, startsAt, endsAt, sharedRecipeTitle }] }` |
| 70 | POST | `/admin/users/{userId}/generation-quota-adjustments` | Increase or decrease a member's reward generation count. Reason required. A decrease cannot exceed the remaining reward | `{ delta, reason }` | `201 { rewardRemaining }` |
| 71 | GET | `/admin/weekly-rewards` | a7 weekly top 3 and reward history. Without `week`, last week | `?week=2026-11-17` | `200 { weeks: [weekStart], week: { weekStart, weekEnd, grantedAt?, top: [{ rank, sharedRecipeId, title, saveCount, authorId, reward, granted }] }, history: [{ weekStart, winners: [{ rank, userId }], totalReward }] }` |

## Admin: changes from the previous admin APIs

The 15 admin APIs that were on hold in the previous plan. Do not implement the removed ones.

| Previous API | Status | Now |
| --- | --- | --- |
| `GET /api/v1/admin/dashboard/summary` | Changed | 61 `GET /admin/dashboard` (a1 dashboard and anomalies in one call) |
| `GET /api/v1/admin/metrics` | Changed | 63 `GET /admin/metrics/{tab}` (a3, 8 tabs) |
| `GET /api/v1/admin/funnels/{funnelCode}` | Changed | 62 `GET /admin/funnels` (a2 shows both funnels on one screen) |
| `GET /api/v1/admin/events` | Kept | 64 (a6 event log). Adds `source` and `excludeSeed` |
| `GET /api/v1/admin/events/catalog` | Removed | The operator page has no event list or volume screen |
| `GET /api/v1/admin/usage/daily` | Removed | Call volume is in the a3 cost and AI tabs (63) |
| `GET /api/v1/admin/reports` | Changed | 65. One report per row → grouped per post (a4) |
| `PATCH /api/v1/admin/reports/{id}` | Changed | 67 `PATCH /admin/reports/{sharedRecipeId}`. `result` is the three screen buttons (VIOLATION, NO_ISSUE, DELETED); the sanction applies automatically |
| `GET /api/v1/admin/shared-recipes` | Removed | No separate shared-post management screen; the post is shown in the report detail (66) |
| `PATCH /api/v1/admin/shared-recipes/{id}` | Removed | Hiding and deleting happen only through report handling (67) |
| `GET /api/v1/admin/users` | Changed | 68. `?q=` → member-number search and a sanctioned filter (a5) |
| `PATCH /api/v1/admin/users/{id}/generation-quota` | Changed | 70 `POST /admin/users/{userId}/generation-quota-adjustments` (creates one adjustment, reason required) |
| `PATCH /api/v1/admin/users/{id}/status` | Removed | No button for an operator to suspend or restore directly; sanctions come only from report handling |
| `GET /api/v1/admin/audit-logs` | Removed | No action-history screen. The handler and reason stay in each table (ERD 11/26) |
| `GET /api/v1/admin/weekly-rewards` | Kept | 71 (a7 ranking and rewards) |
| `GET /api/v1/admin/reports/{sharedRecipeId}` | Added | 66 (a4.1 report detail) |
| `GET /api/v1/admin/users/{userId}` | Added | 69 (a5 user detail panel) |

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
| Admin "오늘·최근 7일·최근 30일" and "실제 데이터만" (a1, a2, a3, a6) | Screen state. Changing them calls 61-64 again |
| Admin "서비스로 돌아가기" (admin common layout) | Go to the app's first screen (`/`) |
| a4.1 three confirm modals, a5 count-adjust modal | Frontend modals. APIs 67 and 70 are called when the operator confirms |

## Scheduled server jobs (not APIs)

| Job | When | What | Tables | Events |
| --- | --- | --- | --- | --- |
| Expiry notifications | Daily 09:00 | EXPIRY_SOON per ingredient (D-3 to D-DAY), EXPIRED for expired ingredients (one per day). Skip members with notifications off. In-app only | notifications | notification_sent, ingredient_expired |
| Weekly recipe reward | Monday 00:00 | +10 / +6 / +3 to the members who shared last week's top 1-3 recipes by saves (per recipe rank), valid one month. Store up to 100 ranks for posts with at least one valid save in the counted week; zero-save posts receive no rank or reward | generation_quota_grants, shared_recipe_weekly_ranks | weekly_reward_granted |
| Receipt cleanup | Daily early morning | Delete receipts older than 6 months by purchase date with items and images | receipts, receipt_items | |
| Notification and code cleanup | Daily early morning | Delete notifications older than 30 days and deletion codes older than a day | notifications, account_deletion_requests | |
| Subscription auto-payment | Daily early morning | Charge 9,900 KRW with the billingKey for ACTIVE subscriptions whose next billing date is today; on success extend by a month. CANCELED subscriptions are not charged and end (ENDED, `users.plan` BASIC, billingKey cleared). On payment failure end the subscription at once (ENDED, `users.plan` BASIC, billingKey cleared) (11/24 terms decision) | subscriptions, subscription_payments, users | subscription_renewed, subscription_payment_failed, subscription_ended |

## Open questions (do not decide in code; ask the PO)

- What the subscription screen shows after scheduling cancellation, and whether a confirm modal is needed.
- Which API and screen let an author delete their own shared post?
- What a sanctioned member sees in the app (sharing ban, account ban) and which errors the app APIs return (`ACCOUNT_SUSPENDED`, a sharing-ban code). Storage is decided (`user_sanctions`, 11/26); the app screens are on hold as "added page 15".
- Which E88 `end_reason` value marks an auto-payment failure ending?

## Decided (do not reopen in code)

- **7-day full refund** (#228, docs #318): request from the Figma refund page. Eligible when, since payment, meal-plan generation = 0 and AI recipe generation above BASIC daily limit = 0. On success: Toss payment cancel, subscription ENDED, `users.plan` BASIC, billingKey cleared; keep payment rows (no in-app history screen). Server event E89 `subscription_refunded` only (not E88). **Refund API method/path string**: fix in the implementation feat issue, then replace the placeholder row under subscription APIs above.
