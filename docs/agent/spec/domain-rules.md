# Domain rules

Source: PRD v4 and policy doc v4 (Figma-based, 2026-11-22; Korean, kept in Notion; not in the repo). Markers:

- (proposal): a draft the PO has not confirmed yet. Implement it behind a named constant.
- (open): not decided. Ask the user before implementing.
- (screen pending): the rule is decided, but its screen is in "screens to add" and not in Figma yet.
- Admin rules are on hold because admin screens are not in Figma (see the end).

## Access and common behavior

- Social login only (Kakao, Naver, Google). No email signup or login.
- Entry: splash (`/`) → login, or the fridge if already logged in. Nothing is usable without login. The fridge is the first screen after login.
- Tokens: access 30 min, refresh 14 days, both HttpOnly cookies (`Secure; SameSite=Lax`). The frontend never keeps tokens in JS. On access expiry the frontend calls refresh once and retries the request. If refresh fails, go to login, then back to the original screen.
- Roles `USER` and `ADMIN`. The server allows `/api/v1/admin/**` to ADMIN only (403 otherwise).
- Paths: backend `/api/v1/{role}/{resource}`, frontend `/{role}/{feature}/{resource}`. Role is `public` (before login) or `user`. Sheets, modals, loading and failure screens have no path of their own.
- Timeouts: general 10 s, OCR 30 s, AI recipe 60 s (recipes only; meal-plan generation replies 202 and runs as a server job, see section 5). AI retries once automatically; OCR does not retry.
- Receipt files: <= 10 MB, jpg/png/heic. Shared cooking photos: one required, <= 5 MiB, JPEG/PNG/WebP ([#64 storage agreement](https://github.com/likelion-backendschool-9th/REMAKE/issues/64#issuecomment-7089190543)). The frontend checks first; the server checks last (413, 415).
- Double clicks: buttons such as register and deduct are disabled until the request ends. No server `Idempotency-Key`.
- Concurrent requests on the same ingredient (use, discard, edit, delete): the server locks the ingredient row and handles them one by one; a later request sees the amount left by the earlier one. Deduction checks the remaining amount first. If two requests race for the last stock, the later one gets `INGREDIENT_NOT_FOUND` (404) because the row is already deleted.
- Errors: `{ code, message, status, traceId, fieldErrors?, details? }` with the 59 codes in [`errors.md`](errors.md). Input errors show as red text under the field (#DC2626). Errors without a screen show the shared toast. Errors with a screen use the OCR failure screen, the generation failure screen, the meal-plan overlap modal, or the quota-used-up screen.
- Empty data is not an error: return 200 with an empty value. Empty-list screens (8 places) are (screen pending).
- Loading: lists use skeletons. AI recipe generation uses a loading screen (up to 60 s, screen pending). Meal-plan generation has no loading screen (section 5).
- Success toast: after saving a recipe, replacing a meal-slot recipe, registering a receipt, or moving cart items to the fridge, show how many cart items were added or removed (not shown when 0). Meal-plan generation is asynchronous, so its cart count goes into the completion notification (section 5). Wording is (proposal).
- Toasts use the design-system component `토스트` with three types: default, success, error (decided 11/22).
- Excluded ingredients (allergies) from settings are always applied to recipe and meal-plan generation: they are sent to the AI as ingredients to avoid and never appear in a generated recipe.
- No health or medical information is collected. Settings values are visible to the owner only.
- Terms: no separate consent screen or checkbox. Logging in with a social account means agreeing to the terms of service and having checked the privacy policy (11/24 terms decision). The login screen shows one line "로그인하면 이용 약관에 동의하는 것으로 봅니다" (final wording per Figma). Login screen links open the terms drawer (common-약관드로워) with three documents: terms of service, privacy policy, payment terms; text from the docs terms md. All three: operator 이준호, effective 2026-12-01, service URL https://remake.newlecture.com/.

## Plans and quota

| Feature | Basic | Premium (subscription) |
| --- | --- | --- |
| AI recipe generation | 2 per day | 4 per day |
| Meal plan generation | 1 per month, up to 5 days | 4 per month, up to 7 days |
| Receipt scan (OCR) | no limit, no counter shown | no limit |

- Plan limits live in plan settings, not hard-coded per feature. Premium limits apply as soon as a subscription starts; basic limits apply when it ends.
- Quick, custom, regenerate, and meal-slot recipe replace all share the daily recipe quota. Charge right after the result passes validation and is saved, in the same transaction.
- Not charged: AI error, timeout, format failure, not enough ingredients (`NO_INGREDIENTS`, `MEAL_PLAN_NO_INGREDIENTS`), meal-plan overlap (409).
- Order: base quota first, then reward grants with the nearest expiry first.
- Concurrent generation by the same member: lock the check and charge per member (proposal).
- Reset: daily quota at 00:00 KST, monthly quota on the 1st at 00:00 (proposal).
- Quota 0 shows the quota-used-up screen (recipes and meal plans differ only in wording, screen pending).
- Meal-plan screen text: "이번 달에 남은 식단표 생성 N회", "식단표 생성에 실패하면 횟수는 차감하지 않아요".
- OCR calls are still recorded as events (cost and success rate).

## Units (14 codes, shared by fridge and recipes)

| Code | Label | Kind | Base | Fridge | Recipe |
| --- | --- | --- | --- | --- | --- |
| G | g | weight | 1 g | yes | yes |
| KG | kg | weight | 1000 g | yes | yes |
| ML | ml | volume | 1 ml | yes | yes |
| L | L | volume | 1000 ml | yes | yes |
| TBSP | 큰술 | volume (measure) | 15 ml | no | yes |
| TSP | 작은술 | volume (measure) | 5 ml | no | yes |
| CUP | 컵 | volume (measure) | 200 ml | no | yes |
| EA | 개 | count | - | yes | yes |
| PACK | 팩 | count | - | yes | yes |
| BUNCH | 단 | count | - | yes | yes |
| PIECE | 쪽 | count | - | yes | yes |
| SHEET | 장 | count | - | yes | yes |
| PINCH | 꼬집 | small | - | no | yes |
| TO_TASTE | 약간 | small (no amount) | - | no | yes |

- **The server picks the unit from the ingredient name** (the internal `ingredient_catalog` dictionary, see [`../erd.md`](../erd.md) table 26). The user never chooses a unit. The unit is decided in this order (#145): the name equals a dictionary name exactly; otherwise the longest dictionary name (two characters or more, same category) contained in the name; otherwise `EA`. A unit printed on a receipt does not override it. Cart-to-fridge transfer is the quantity-preservation exception recorded in section 7; it does not call the name resolver.
- Packaging words (봉지, 캔, 병, 통) map to PACK; counting words (마리, 알, 모, 포기) map to EA.
- Deduction converts automatically only within the same kind (weight↔weight, volume↔volume with measures in ml). Different kinds need the user to confirm the amount. PINCH and TO_TASTE are never deducted.
- Fridge amounts are plain numbers, no fractions, up to 3 decimals. Stepper step (proposal): g/ml 50, kg/L 0.5, count units 1.
- The AI may use only these unit codes; the server filters out others.
- Display: `name + amount + label` (양파 1개, 돼지고기 300g, 간장 2큰술), `name + 약간` for no amount, `N인분`, cooking time `N분` or `N시간 N분` from 60 min, expiry `D-N` / `D-DAY` / `폐기-N`, status 위험 · 임박 · 여유.

## Categories (12 codes)

Names use the middle dot. Default shelf life and storage are drafts and may change during development. "전체" (all) is not offered when adding or editing an ingredient.

| Code | Name | Default shelf life | Default storage |
| --- | --- | --- | --- |
| VEGETABLE | 채소 | 7 days | refrigerated |
| MEAT | 육류 | 3 days | refrigerated |
| FRUIT | 과일 | 7 days | refrigerated |
| SEAFOOD | 해산물 | 2 days | refrigerated |
| DAIRY_EGG | 달걀·유제품 | 10 days | refrigerated |
| GRAIN | 곡류·면·빵 | 30 days | room temperature |
| BEAN_TOFU | 두부·콩류 | 5 days | refrigerated |
| PROCESSED | 가공·냉동식품 | 30 days | frozen |
| SIDE_DISH | 김치·반찬 | 14 days | refrigerated |
| SEASONING | 양념 | 180 days | room temperature |
| BEVERAGE | 음료 | 30 days | refrigerated |
| ETC | 기타 | 7 days | refrigerated |

Used by the fridge filter, spending-pattern ratios, expiry estimation (receipts and cart), and cart → fridge.

## 1. Login, onboarding, settings (이준호)

- First login creates the account automatically. Changing providers means a different account (no merging, proposal).
- "최근 로그인" bubble over the last successful provider. Stored on the device (localStorage) and kept after logout.
- Onboarding right after the first login, 2 steps: household size → ingredients to exclude. Both values are saved once at the end. No skip button. Household size is required to continue; allergies may be 0. Never shown again once finished.
- Excluded ingredient chips (19): 난류, 우유, 메밀, 땅콩, 대두, 밀, 고등어, 갑각류, 돼지고기, 새우, 아황산류, 복숭아, 오징어, 닭고기, 쇠고기, 굴, 게, 잣, 어패류. Multiple choice; shows "N개의 재료 제외".
- 0 excluded ingredients: onboarding (p1.2) and settings (p1.3.2) still allow finishing. Only the custom-recipe allergy sheet disables its button at 0.
- Household size: 1–50, digits only. Quick recipes use it as servings.
- Settings: account card (provider, household, allergy count), premium, household size, allergies, "recipe supplement" toggle (default OFF), "receive notifications" toggle, logout, account deletion. Changes apply to later generations.
- Login failure: `OAUTH_FAILED` (401) as a toast. A user cancel is not counted as a failure.
- Logout: confirm modal → delete the refresh token and both cookies.
- **Account deletion**: settings → confirm modal → Kakao and Google get a 6-digit email code; Naver re-authenticates. On success clear the cookies and go to login.
  - Code: 6 digits by email, valid 5 min (timer 04:59), "재전송 하기" sends a new code. More than 5 wrong attempts kills that code. Errors (wrong, expired, too many attempts) show as an error toast with the texts in errors.md (11/22; replaces the red text under the 6 boxes).
  - Email: Kakao requires email consent at login (biz app); Google always provides it. If still missing, `ACCOUNT_EMAIL_MISSING` (contact support).
  - Naver with a different Naver account: do not delete, back to settings (`REAUTH_ACCOUNT_MISMATCH`). Canceling re-authentication: do not delete (proposal).
  - Unlink the provider on deletion; if unlinking fails, still delete and log it (proposal).
  - Data: delete excluded ingredients, tokens, codes, fridge ingredients, logs, receipts, meal plans, cart, notifications. Keep the `users` row with social ID and email cleared (re-signup possible). Recipes stay; only shared recipes the author has not deleted stay, as anonymous posts (11/24 terms decision). Shared recipes of deleted members get no reward (proposal).
- **Subscription** (Toss Payments card auto-billing):
  - Settings "구독 하기" → p1.3.4. Before subscribing: next payment date (today + 1 month), price, 2 benefits, payment terms checkbox, subscribe button. While subscribed: "구독 중" badge, next payment date, price, "구독 해지 하기".
  - Price 9,900 KRW per month, VAT included (11/24 terms decision). Benefits: 4 recipes per day, 4 meal plans per month of 7 days.
  - Flow: check the terms (button disabled until then) → Toss payment window (register card for auto-billing) → on return the server charges the first month and starts the subscription on success. Failure or cancel in the Toss window returns to the before-subscription screen with a toast.
  - Payment terms go in the terms drawer (common-약관드로워) (11/24 terms decision).
  - Renewal: on the next payment date (start + 1 month; the last day when that date does not exist) charge the same card and extend one month on success.
  - Auto-payment failure: end the subscription at once (ENDED, clear `billing_key`, `users.plan` = BASIC). To subscribe again, register a card again (11/24 terms decision).
  - Cancel: schedules the cancellation. Benefits stay until the day before the next payment date; no charge then, and the member returns to basic. The screen or modal after scheduling is (open).
  - After it ends: basic limits; meal plans already used this month still count. No re-subscribing while a cancellation is scheduled (`ALREADY_SUBSCRIBED`); after it ends, subscribe as new (proposal).
  - Account deletion ends the subscription immediately with no more charges. Payment records are kept. No refund for the remaining period (11/24 terms decision).
  - Cooling-off and refunds: full refund within 7 days of payment if no premium benefit (recipe or meal-plan generation beyond basic limits) was used; otherwise cancel only, no refund (11/24 terms decision). Refund API and flow: (open).
  - The Toss secret key and `billingKey` stay on the server only; `billingKey` is stored encrypted.
  - Demo payment: Toss Payments test keys only, no real charge (11/24 terms decision).

## 2. Fridge (김재연)

- First screen: "냉장고에 재료가 N개 있어요", "AI를 활용해 레시피 생성하기 →" (to recipe home), danger / soon / ok cards, ingredient list.
- Ingredient fields: name, category, storage (refrigerated, frozen, room temperature), expiry date, price (optional), amount. Manual add and OCR item edit/add use the same fields. All but price are required; the button is disabled until they are filled.
- Name is the ingredient, not the product ("비비고 냉동 만두" ✗ → "만두" ✓).
- Unit: the server picks it from the name (and the category, API 14 with `categoryCode`). The amount field is disabled until the name is set; the frontend asks API 14 again when the name or category changes so the shown unit equals the saved one. Changing the name or the category re-picks only the unit (with the new name and category); the number stays, but only when the current unit equals the dictionary unit of the name and category before the edit. An ingredient whose unit differs from the dictionary (cart transfer or receipt `PACK` exception, #145, #224, #226) keeps its unit and amount when renamed or recategorized (#233). The ingredient responses (API 13, 15, 16) carry `unitFollowsName`: `true` means the unit is re-picked on edit, so the screen previews it with API 14; `false` means the screen shows the current unit as is. If a dictionary migration later changes the dictionary unit, the ingredient is treated as keeping its unit. Sending the same values again keeps the unit. This applies on add and on edit, also when the ingredient already has use/discard logs (each log keeps the name and unit recorded at that time).
- Amount edit: it fixes an input mistake, so it changes `amount` and `initial_amount` together (the discarded cost divides by `initial_amount`). Reject it with `INVALID_INPUT` once the ingredient has any use/discard log (#128).
- Expiry status: danger = expired or today (D-DAY); soon = D-1 to D-3; ok = later. Shown as `D-N`, `D-DAY`, `폐기-N`. Saved ingredients: the server computes them (summary counts, status filter, and notifications use the same result). Only the OCR result before registration is computed by the frontend with the KST date, because it is not stored and the user edits it (11/25).
- Filter and sort: tapping a status card shows only that status; "재료 위치" and "재료 종류" sheets filter one at a time. Sorted by nearest expiry. List title "N개의 위험/임박/여유 재료". **No ingredient search.**
- Row tap opens the used/discarded sheet; the trailing `>` opens edit.
- Used/discarded: choose "사용 했어요" or "버렸어요" + amount + confirm. Discarded shows "이번 달 버린 식재료 금액에 더해져요".
- More than available: `INSUFFICIENT_QUANTITY` (400). The sheet caps the amount at what remains (capping method is a proposal).
- Amount reaches 0: delete the ingredient row; logs keep the name.
- Discarded cost = price × discarded amount ÷ original amount. No price, no cost. This month's discarded cost shows on "나의 소비" as "N월 버린 식비", which opens p3.4.1 "버린 식재료" (decided 11/25; API proposed in #248).
- Delete: delete icon on the edit screen (screen pending). Allowed even with logs; logs keep the name.
- Price is optional on manual add too ("가격을 정확히 작성할수록 소비 패턴 분석률이 올라가요"). Priced ingredients count in spending patterns.
- Default expiry: ingredients from receipts and the cart get the category default shelf life. No "estimated" flag (only a notice on the OCR result screen). Estimation base date, frozen/room values, and recalculation are up to the developer.
- Expired ingredients are not deleted automatically; they stay as `폐기-N` for the user (proposal).
- No repurchase marker (release 2).

## 3. Receipts and spending (이동준)

- Scan: "영수증 스캔 하기" → camera, or "첨부 하기" / gallery icon → image upload (screens after upload are pending).
- Camera (decided 11/25): open the device camera app, not an in-app camera. Use a file input: camera = `accept="image/*" capture="environment"`, album = `accept="image/*"` without `capture`. Both return one photo file that goes to the scan. No in-app viewfinder, receipt guide frame, flash, or focus control; no permission check or toggle (the device camera app handles its own permission). Devices without a camera open the file picker instead.
- Camera unavailable: shared toast "카메라를 쓸 수 없어요. 갤러리에서 골라 주세요" (`CAMERA_UNAVAILABLE`, wording is a proposal). With the device camera app this rarely happens; keep it as a fallback when no photo can be taken.
- No scan limit. OCR is synchronous (up to 30 s) with the OCR result skeleton. No "analysis done" notification.
- OCR failure: one failure screen with different wording for: unreadable (not a receipt, OCR error, timeout), no food items, purchase date older than 6 months, already registered. The user rescans (screen pending).
- Results contain food only (non-food such as 수세미, 냄비 is filtered out). Each item is filled with category, storage, amount, price, and expiry.
- Review on "영수증 재료": edit (p3.3.1), add (p3.3.2), remove. Removal is allowed so the user can drop an item OCR read wrongly: trash icon on p3.3.1 → confirm modal "재료를 삭제할까요?" (restored 11/24; it was canceled on 11/22). Add and rescan open from the app bar ⋮ sheet ("재료 추가", "영수증 재촬영"). Edits, additions and removals stay in screen state and are sent with registration (removed items are simply not sent).
- Notice: "소비 기한은 카테고리 기본 보관 기간으로 추정 했어요. 소비기한을 확인 하시고 수정해 주세요".
- Register ("식재료 N개 등록 하기"): save the image, receipt, items, and fridge ingredients at once. Unregistered OCR results are never stored; leaving without registering records only an accuracy event.
- Purchase date and store name: empty if OCR cannot read them; both are required at registration.
- Receipts older than 6 months cannot be registered (rejected at scan as an OCR failure).
- **Duplicate receipts are blocked.** Same store + receipt number + purchase time = duplicate (without a number: store + purchase date + food items; without a time: purchase date). Checked right after the scan (OCR failure screen "이미 등록한 영수증이에요", `RECEIPT_DUPLICATE` 409) and again at registration.
- Receipt cart deduction (#57): select a cart row in the bottom sheet immediately before final registration, or skip. Each receipt item can select at most one cart row. Deduct only in the identical unit, within the actual registered amount; sum requests for the same target and validate the cart balance. Delete only rows reaching zero; `cartRemovedCount` counts these deleted rows. Receipt, fridge and cart changes share one transaction. The screen is a separate FE task.
- List: month picker "2026년 11월 ∨" (last 6 months), monthly total, "내 평균보다 N원 더 썼어요", groups by week (week, range, total), receipt rows (store icon, store, date, "식재료 N개", amount).
- Monthly total: food item amounts of that month's receipts (by purchase date).
- Average: difference from my past average (monthly average of the last 6 months). Excluding this month, using only months with records, and hiding when there is no history are (proposal).
- Store icon: match the store name to 14 codes (CU, GS25, SEVEN_ELEVEN, EMART24, EMART, HOMEPLUS, LOTTE_MART, COSTCO, TRADERS, HANARO, GS_FRESH, LOTTE_SUPER, EMART_EVERYDAY, HOMEPLUS_EXPRESS). No match: leave empty and show the default icon (matching method is a proposal).
- Detail: original image, "2026년 11월 영수증", store and branch, purchase date (date only; the purchase time is not shown, decided 11/25), items (image, name, storage, amount, price), total (food items), delete.
- Registered receipts cannot be edited, only deleted. Fridge ingredients are edited in the fridge (editing a price there does not change the receipt).
- Delete: modal → delete receipt, items, and image; it leaves spending and patterns; fridge ingredients stay.
- Retention: 6 months from the purchase date; a daily early-morning job deletes older receipts with their images.
- Image storage type is the backend's choice; the server builds image URLs.
- "나의 소비" (spending pattern): month tabs for the last 6 months (built by the frontend), month spending (positive), "평균 보다 N원 더 지출", one category ratio bar with legend, Mon–Sun day row (amount under the selected day), "총 N원" and the food list for the selected day.
- Pattern scope: receipt items + ingredients added without a receipt that have a price (by registration date). Price edits after registration are reflected (set, change, or clear); the date stays the registration date. Unpriced ones are treated as fridge-only and excluded. Discarded ingredients stay included (already bought).
- Category ratio: 12 categories; categories with 0 spending are left out (proposal).
- Day selection: default today (or the last purchase day if not this month, proposal). No spending: "해당 날짜에는 소비가 없습니다".
- Week row: swipe left/right (carousel) to the previous/next week of the selected month (decided 11/25). Days outside the selected month and days after today cannot be chosen (proposal).
- Average difference: "평균보다 N만원 더 지출" / "덜 지출". From 10,000 원, round to 만원; below that, show 원 (decided 11/25). A difference of 0 shows "평균과 같게 지출" (proposal).
- Discarded food (p3.4.1): "N월 버린 식비 N원" link at the top of 나의 소비 → "N월에 버린 식재료" total (priced items only), "가격을 넣은 재료만 금액에 더해져요", rows (category image, name, "M월 D일 버림", amount or "가격 없음"), newest first. API: API 25 `discardedAmount` and API 61 (proposal, #248).
- No frequent items or purchase cycles.

## 4. AI recipes (김종수)

- Two modes: quick (fridge ingredients and household size, expiring first) and custom (preferences).
- Quick modal: "가구원 수와 냉장고의 재료에 맞게 레시피를 생성 합니다. 레시피를 생성하면 하루 생성 횟수를 차감 합니다" → 생성 하기.
- Custom: servings (required) + ingredients (from my fridge), taste, cooking time, tools, method, cuisine, allergens. One choice per sheet (confirm disabled until chosen). **No free text.** Default servings = household size (proposal).
- One response, no streaming. Loading screen up to 60 s (screen pending).
- Recipe supplement OFF: use only my ingredients. Too few (including an empty fridge) → `NO_INGREDIENTS` → generation failure screen "설정에서 레시피 재료 보충을 켜 보세요"; not charged (screen pending). ON: may use ingredients not in the fridge.
- AI error, timeout, or format failure: retry once, then fail with the failure screen (wording differs); not charged.
- No "AI가 생성한 레시피" label (11/22, kept). The AI-results disclaimer (no guarantee of accuracy, nutrition, or allergy safety; users check before cooking; meal plans do not replace medical or nutrition advice) is only in the terms of service, never on screens (11/24 terms decision).
- Result: name, bookmark (empty before save, filled after), "재료 준비 · servings · time", ingredients, steps with read-aloud, Tips!, "레시피 다시 받기 N회", 요리 완료.
- Regenerate: always tappable (only dimmed); charges the daily quota.
- Generated recipes are stored but hidden from lists until saved. Recipes are never edited after creation.
- Save via the bookmark. Supplemented missing ingredients go to the cart on save, with a count toast (screen pending).
- Saved list: "저장한 레시피 N", chips 전체 · 즐겨찾기 · 바로 가능 (makeable with current fridge ingredients), sort newest saved (default) / oldest (sort sheet pending). No save limit (proposal).
- Favorite: star on saved rows, ☆ on detail, ★ on meal rows. Unsaving removes it.
- Detail: name, ☆, ⋮, "재료 준비 · N인분 · N분 이내", ingredients (name + amount), steps, Tips!, 요리 완료. No "in my fridge" marks on my recipe detail (shared detail marks missing ones with a red border).
- Read-aloud: "만드는 방법 재생하기" reads steps one by one with the device voice; "중단하기" while playing.
- ⋮ "삭제 하기" removes it from my saved list only; shared posts and others' saves stay.
- ⋮ "공유 하기" opens shared write. Disabled if already shared this week (weeks start Monday).
- Cook: "요리 완료" → deduction screen. No cooking history table; recorded only as event `cook_completed`.
- After the cook deduction go to the fridge (p2) and show the bottom sheet "이 레시피 어땠나요?" (추천해요 / 추천하지 않아요 → 보내기). The answer is tracking data only (success rate, later improvements), not used by any feature: the frontend sends E50 `cook_feedback_answered` through API 56. No new API or table.
- Deduction screen: "총 N개의 재료를 차감해 주세요", check recipe ingredients I have, `>` opens the amount sheet, "N개의 재료 차감 하기" (N = checked). Empty state when none are in the fridge (screen pending).
- Cooking from today's meal slot marks that slot cooked (not other days).
- "Recommend this recipe?" screen: position not decided (screen pending).

## 5. Meal plan (한윤수)

- Empty: "이번 주 식단표가 아직 없어요", "냉장고 재료를 활용해서 식단을 만들어 드려요", remaining count, 식단표 만들기.
- Calendar: "2026년 11월 ∨" opens a month picker sheet; month grid Sun–Sat with today and plan period highlighted. Past months and days are viewable.
- Day: tapping a date opens "N월 N일 식단" with meal rows (meal label, recipe name, ★, ingredient summary +N, ⋮). Row tap opens the recipe detail. No-plan day empty state (screen pending).
- Add: "+" next to the sheet title → add sheet (remaining count + 식단표 만들기) → setup. No manual add to an empty meal.
- Setup: start date (from today, not earlier), one purpose, meals (any of breakfast, lunch, dinner), servings (default household size, proposal).
- Purposes: BALANCED (default), DIET, HIGH_PROTEIN, SIMPLE, SAVING. Remember the last choice as the next default. No medical or calorie targets. No "참고용 식단" label (the disclaimer is in the terms of service only, 11/24 terms decision).
- Days: basic up to 5, premium up to 7. With too few ingredients, only as many days as possible.
- Short of days but >= 1 day: missing ingredients still go to the cart, and the completion notification adds "재료가 부족해 N일치만 만들었어요". Whenever cart rows were added (also when all days were made), it adds "부족한 재료 N개를 장바구니에 담았어요" (#91; wording is a proposal). No separate toasts.
- Not even one day: `MEAL_PLAN_NO_INGREDIENTS` → failure notification; the plan is `FAILED`, not charged.
- Each meal slot gets a full recipe from the start (no "menu name first, details later").
- Do not allocate the same ingredient amount to several meals (based on each slot's recipe ingredients; no per-slot planned-ingredient table; method is up to the developer).
- Missing vs short (#78): a fridge ingredient is used only within its remaining amount (fridge amount minus amounts already allocated to other plans). If it runs short, it does not go to the cart; generation stops from that day (fewer days). An ingredient not in the fridge stays in the recipe and goes to the cart. When the AI writes a fridge ingredient as a purchase line without its fridge ID (#91, seen in the 11-26 measurement), the server links it back before trimming if the name matches exactly, the unit converts to the fridge unit, and the item is usable on that slot date (earliest expiry first); if the unit cannot convert, the line stays unlinked but is not added to the cart because the user has an ingredient of that name.
- Each meal uses at least 1 fridge ingredient (#78). Ingredients without an amount ("약간", "한 꼬집") do not count. Basic seasonings follow #158.
- Trimming is per day (#78): if any meal of a day cannot be made, drop that whole day and stop there. Never skip a day to make later days.
- Overlap: if the new period overlaps an existing plan, show the warning modal (cancel / overwrite). Overwrite deletes the overlapping days' slots and refills them when the generation job completes (a failed job keeps the existing plan). Overwrite replaces the whole requested period (start date through the maximum days): the job may use the ingredients allocated to those existing slots from the start, and on success it deletes all of them, so days at the end that were not made for lack of ingredients become empty (#78, changed 11/24). Meals already cooked today stay and are not refilled. Overlap is not charged (modal pending).
- Slot ⋮: "식단 삭제" or "레시피 교체" + confirm. Delete removes that slot only. Replace generates a new recipe and charges 1 daily recipe ("레시피를 교체하게 될 경우 레시피 사용 횟수를 1회 차감 합니다"); not charged on failure. The new recipe's missing ingredients go to the cart with that slot as source; the response carries `cartAddedCount` and the screen shows the same count toast as recipe saves when it is 1 or more (#91, wording is a proposal).
- Past days are view only. ⋮ stays; tapping it shows the `PAST_DATE_NOT_ALLOWED` toast.
- Planning does not deduct fridge ingredients; only cook deduction does.
- Meal plans use the monthly quota; replace uses the daily recipe quota.
- **Asynchronous generation**: the overlap modal and the quota-exhausted screen are checked before generation and stay as they are. Then pressing generate sends API 38 (202) and goes straight to the fridge (index) screen with the toast "식단을 완성하면 알림을 보내드려요". There is no "generating" screen and no per-day progress.
- Completion: in-app notification "11월 24일부터 5일치 식단표가 완성됐어요" (start date and days made; wording is a proposal). Tapping it opens p5 at the plan's start date.
- Failure: in-app notification, not charged, no failure screen. AI error or timeout: "식단표를 만들지 못했어요. 잠시 후 다시 만들어 주세요. 생성 횟수는 차감되지 않았어요". Not even one day (`MEAL_PLAN_NO_INGREDIENTS`): "냉장고 재료가 부족해 식단표를 만들지 못했어요. 생성 횟수는 차감되지 않았어요". Tapping it opens the meal-plan setup screen. Recipe generation failures keep the failure screen (screens to add no. 8).
- Meal-plan notifications are created even for members who turned notifications off.
- Target: generation within 30 s, tune after measuring (measured from acceptance to job completion; the completion notification is sent at that moment). The 60 s limit is for recipes only; the server job's time limit is (open, #122).
- A new generate request while one of mine is `GENERATING` is rejected with `MEAL_PLAN_IN_PROGRESS` (409, error toast).
- Overwrite deletes the old overlapping slots when the job completes, so a failed job keeps the existing plan. Calendar and overlap by status (#144, decided 11/25): only `COMPLETED` plans show in the calendar (39) and count for `hasAnyMealPlan`; `GENERATING` and `FAILED` plans are left out. The overlap check works on existing slots, so the plan status does not matter (a `GENERATING` or `FAILED` plan has no slots; a new request while one is `GENERATING` gets `MEAL_PLAN_IN_PROGRESS`).

## 6. Notifications (이상호)

- Types: `EXPIRY_SOON`, `EXPIRED`, `MEAL_PLAN_COMPLETED`, `MEAL_PLAN_FAILED`.
- Text: "OO의 소비기한이 얼마 남지 않았어요. 제품의 포장지에서 사용 기한을 확인해 주세요", "기한이 지난 재료 N개가 있어요. 냉장고를 확인해 주세요". Meal plans (section 5): "11월 24일부터 5일치 식단표가 완성됐어요" (proposal), "식단표를 만들지 못했어요. 잠시 후 다시 만들어 주세요. 생성 횟수는 차감되지 않았어요", "냉장고 재료가 부족해 식단표를 만들지 못했어요. 생성 횟수는 차감되지 않았어요".
- **In-app only** (top banner and notification screen). No device push.
- Banner: latest notification + red unread count at the top of every tab screen; tap opens the notification screen.
- Screen: grouped by 오늘, 1일 전, 2일 전. Expiry items have no tap action; meal-plan items open p5 at the plan's start date (completed) or the meal-plan setup screen (failed). Gray text is a text color, not a read marker.
- Opening the screen marks all as read. No per-item or mark-all button.
- Empty state (screen pending).
- Expiry soon: each ingredient expiring today to 3 days later (D-3 to D-DAY) gets one notification per day during that period. D-DAY is "danger" on the fridge screen but "soon" in notifications, on purpose.
- Expired: one notification per day with the count. The expired event fires once, when first found.
- Daily job at 09:00 KST for expiry notifications. No expiry notifications for members who turned them off. Meal-plan notifications are created when the generation job ends, regardless of the setting. Notifications older than 30 days are deleted every early morning. No user-configurable thresholds or times.

## 7. Cart and sharing (최유리)

### Cart

Apply #57, the [ERD](../erd.md#19-cart_items-cart), and the subsequently approved contracts below. The exact name/unit comparison key and initial slot-source retention were approved by the user in the implementation conversation; unmeasured-ingredient exclusion was confirmed in the linked PR #216 comment:

- Store the original `recipe_id` for meal-plan additions as well as recipe saves. Use nullable `meal_plan_slot_id` only as the initial meal-slot source; keep it when an existing cart row is reused. Do not restore `meal_plan_id` or `group_name`.
- Deleting a meal assignment clears the slot FK through `ON DELETE SET NULL` and keeps the recipe. Its cart rows are cleaned up first (see "Meal-plan cart cleanup" below, #91). Unsaving either my recipe or a shared recipe keeps cart rows and amounts.
- Deduplicate additions by user + original recipe ID + exactly matching ingredient name + unit. On a new save after unsaving, add only currently missing ingredients absent from that recipe's cart; preserve existing rows and amounts. Retrying an already-saved PUT stays idempotent and must not trigger a new add.
- For repeated use of one original recipe within a single meal-plan generation, aggregate that generation's missing amounts per recipe/name/unit before adding. Keep pre-existing cart rows unchanged; never add the new total to a user-edited amount. Count actual inserted rows, not meals or summed quantities.
- `cart_items.amount` is required, positive, and `numeric(10,3)`. API 45 returns a non-null amount for persisted cart rows. Do not infer nullable cart quantities from the nullable recipe-ingredient quantity.
- Exclude `amount=null`, PINCH and TO_TASTE ingredients from all three cart add paths (meal-plan shortages, my recipe saves, shared recipe saves). Keep them visible in recipes. Do not substitute an arbitrary quantity; keep `cart_items.amount` NOT NULL. Source: [cart owner decision in PR #216](https://github.com/likelion-backendschool-9th/REMAKE/pull/216#issuecomment-7123595032). This policy is implemented by the common add UseCase in #89; my-recipe saves are connected in #76, meal-plan generation and slot replace in #91, shared-recipe saves in #90.
- Cart-to-fridge quantity preservation (decided by cart owner YooRee / `@ureeStar` in the #89 implementation discussion on 2026-11-24; [recorded in #145](https://github.com/likelion-backendschool-9th/REMAKE/issues/145#issuecomment-7125362300)): preserve amount and unit for the fridge's nine units. Convert TBSP/TSP/CUP to ML with factors 15/5/200 only; do not re-resolve a cart unit from the ingredient name or convert weight to count. Reject converted amounts outside `numeric(10,3)` with `INVALID_INPUT` and keep all selected rows. This is an exception to #145's original rule that cart transfer also uses `resolveUnit`; direct registration/editing and receipt registration keep their existing unit rules. The same ingredient name may therefore exist in separate fridge rows with different units (for example onion in EA and G). The receipt deduction contract still requires identical units and does not convert, so an EA receipt item cannot deduct a G cart item. Fridge-owner impact confirmation from `@jykim78982` was requested in #145 and is still pending; do not describe this as cross-feature agreement.
- Cart categories (approved by cart owner YooRee / `@ureeStar` in the same #89 discussion on 2026-11-24): use an exact catalog-name match first; otherwise require a server-decided `categoryCode` from the calling feature. If neither is available, reject with `INVALID_INPUT` rather than inventing an ETC category. Names are stripped before exact comparison. Meal plans (#91) decide each recipe line's category like recipe generation (#76): exact catalog name, else the line's fridge ingredient category, else the code the AI picked; a missing ingredient with none of these (or a name over 50 characters) is not added, and the plan is still saved.
- API selection requires a non-empty list of distinct positive owned IDs. Missing or foreign IDs reject the whole request with `CART_ITEM_NOT_FOUND`; duplicate or invalid IDs are `INVALID_INPUT`. Preserve input-independent lock order by ascending ID. List rows by ascending cart ID; keep entries from different recipes separate.
- Meal-plan cart cleanup (#91; implemented as proposed in #91, planning confirmation by `@JUNHORANG` requested in the PR): when a recipe leaves the plan (slot delete 41, slot replace 42, overwrite 38), `CleanUpMealPlanSlotCartItemsUseCase` runs before the slot is deleted or its `recipe_id` changes. It locks the user's rows with that slot + that recipe in ID order, deletes rows never changed after adding (`updated_at = created_at`), and only clears `meal_plan_slot_id` on rows whose amount was edited or partly deducted by a receipt, and on rows of a recipe the user saved. Clearing the link keeps the amount and `updated_at`. Meal-plan recipes are created per slot, so no other slot uses the same `recipe_id`.
- Slot replace (42): in one transaction save the new recipe, clean up the previous recipe's rows, update the slot, then add the new recipe's missing ingredients with that slot as source. Any failure rolls back all of it.

- Fifth top tab "장바구니".
- Items are added automatically only: meal-plan shortages, supplemented missing ingredients when saving my recipe, missing ingredients when saving a shared recipe. No manual add. The server sets the category.
- Rows: checkbox, image, source recipe name, ingredient + amount, "수량 변경". The same ingredient from different recipes stays separate.
- Actions: amount sheet, select all (N), "선택 삭제" → delete mode (red checks, "삭제 해제" to undo) → "N개의 재료 삭제".
- "N개의 재료 냉장고에 추가": moves to the fridge without input. Storage and expiry from category defaults, no price. Moved items leave the cart, toast "냉장고에 N개 넣었어요" (screen pending).
- Receipt registration deducts only selected rows under #57, matching the final ingredient name and identical server-decided unit. No selection leaves the cart unchanged; no automatic name-based removal.
- Empty: "장바구니가 비어있습니다", buttons disabled.
- No external shop or checkout. If the schedule slips, cart and sharing (P1) move to release 2.

### Sharing (weekly recipes)

- Recipe tab "금주의 레시피" TOP 3 cards + 전체 보기 → "금주의 인기 레시피". No my recipes / shared tab switch.
- TOP 3 card: rank, photo, title, ingredient summary +N. No rank change. Return up to 3 eligible posts, or 0–2 when fewer qualify; never fill vacant ranks with zero-save posts (empty state screen pending).
- Popular list: "11월 셋째 주", popular (default) / latest, search, rows (rank, photo, title, ingredient summary +N, rank change). Popular = TOP 100 by saves this week.
- Rank change vs last week: blue ↑N, red ↓N; "—" if no rank last week; same rank shown as "—" (proposal).
- Latest: all visible posts regardless of save count; "—" instead of rank and change, shared time on the right (1분 전 to 30일 전).
- Ranking eligibility (confirmed 2026-11-25, PR #255 review item 3): exclude posts with zero valid saves this week before assigning TOP 3 and popular TOP 100 ranks. Return only eligible posts, up to each limit. Share age is unrestricted; cumulative saves do not determine eligibility.
- Popular count: saves received from Monday 00:00 KST through the current time that still exist; ties go to the earlier share (proposal). Excluding own saves remains a proposal.
- Search over shared recipes; 0 results empty state (screen pending).
- Only recipes I generated with Remake can be shared, not recipes saved from others. Fully anonymous: no nickname or author info.
- Write: detail ⋮ "공유 하기" → 1 photo (required, "탭해서 이미지 수정"), title (required, maximum 100 UTF-16 code units before trimming, matching Java `@Size` and HTML `maxlength`), recipe content (read only) → "레시피 공유 하기". Enabled only with title and photo. No confirm modal.
- The same recipe can be shared once per week (again next week).
- Shared detail: photo, title, flag (report), "재료 준비 · servings · time", ingredients (missing ones with a red border), steps with read-aloud, Tips!, heart + save count, "레시피 저장", "레시피 저장 시 부족한 재료는 장바구니로 들어가요".
- Save: the heart and "레시피 저장" are the same action. Puts the original recipe in my saved list (no copy). Save count +1; tapping again cancels (-1). Missing ingredients go to the cart. Removing it from my list leaves the shared post.
- Report: flag → 4 reasons (`OFFENSIVE_IMAGE` 불쾌한 이미지 사용, `AD_TITLE` 광고성 제목, `DANGEROUS_RECIPE` 위험한 조리법 공유, `OTHER` 기타) → 신고 하기. No text input, even for 기타. Once per person per recipe; again → `REPORT_ALREADY_EXISTS` toast. No auto hide; operators review (handling screen comes with the admin design, on hold).
- Sanctions after an operator confirms a report as a violation: 1st 7-day recipe sharing ban, 2nd 30-day recipe sharing ban, 3rd 30-day account suspension, 4th permanent account suspension (11/24 terms decision). How sanctions are stored, checked, and returned (API, error codes): (open).
- Authors can delete their own shared posts at any time (11/24 terms decision). This is separate from ⋮ "삭제 하기", which only removes a recipe from my saved list. Screen and API for deleting my shared post: (open).
- **Reward**: members who posted last week's top 1–3 recipes by saves. Per recipe rank, so one member can receive more than once. 1st +10, 2nd +6, 3rd +3 recipe generations, usable for one month from the grant.
- Week = Monday 00:00 to Sunday 24:00 KST. The Monday 00:00 job records last week's TOP 100 and grants rewards, counting only saves that still exist then. Apply the same eligibility rule in #92: zero valid saves for the counted week means no finalized rank or reward (confirmed). Never grant the same week twice. Own saves do not count (proposal). No reward screen or notification; only the remaining count changes.

- #92 implementation: weekly completion, ranks and rewards commit atomically, including empty weeks. `FinalizeWeeklyRankingUseCase.finalizePreviousWeek()` is the internal entry point; there is no manual admin API. Repeated/concurrent execution cannot change a settled week or issue another E75. Proposed: withdrawn authors keep their rank but receive no reward; do not promote the next rank. Expiry is payment time plus one calendar month (month-end clamp).
- Delayed execution limitation: `saved_recipes` keeps no unsave history, so a late first execution cannot reconstruct saves canceled after Monday 00:00. It counts rows still retained when it runs, within the previous week's save-time window. Automatic missed-week recovery is not provided. #230 author-deletion and finalized-reward policies remain open.

### Cooking photo storage (#64)

- `StoreSharedRecipePhotoUseCase.store(SharedRecipePhotoUpload)` stores one photo and returns a random `imageKey`. The #86 publishing UseCase must start a writable transaction and save that key to `shared_recipes.image_key` in the same transaction. There is no separate photo table. Missing/empty photo -> `PHOTO_REQUIRED`; MIME/header mismatch -> `UNSUPPORTED_FILE_TYPE`; over 5 MiB -> `FILE_TOO_LARGE`. Check the actual stream length, not only the declared size. Header checks do not fully decode or convert images.
- Use `remake.upload.root-directory`, explicitly mapped from `REMAKE_UPLOAD_ROOT_DIRECTORY`. Local default: `${java.io.tmpdir}/remake-uploads`. Production requires the variable and must use the shared volume `/data/uploads`, never the temporary default. The deployment owner adds the volume, compose variable and Nginx limits in a separate PR; confirm deployment before merging this feature.
- `FileSystemSharedRecipePhotoStorageAdapter` writes under `shared-recipes/`, does not overwrite existing keys and rejects invalid paths or symbolic-link reads. `LoadSharedRecipePhotoPort` returns the stream, MIME and length; close the stream after use. A missing file returns empty. Delete is idempotent.
- Storage failures remove the attempted file; transaction rollback removes the newly stored file. An immediate deletion failure is retried on rollback; persistent deletion failures are logged without provider details and need manual cleanup. Process-crash recovery is not guaranteed. Photo handling emits no event.
- Global Spring multipart limits are 10 MB per file and 30 MB per request, preserving receipts; the photo service applies its stricter 5 MiB limit. The deployment owner sets both Nginx limits to 32m.
- #86 connects publishing, DB storage and an authenticated image API that checks the shared post's visibility before reading its DB key. Do not expose the volume as a public static directory or accept arbitrary file paths from a request. There is no user post-removal API yet (see the rule above); withdrawal keeps the shared posts the author has not deleted, as anonymous posts, with their photos. Do not delete those files as receipt images. FE resize/compression and authenticated image display belong to #112.

## Admin (on hold)

- Access: social login + ADMIN role at `/admin`. No separate login.
- Metrics, logs, threshold alerts, user management, report handling, account suspension (`SUSPENDED`), per-member quota adjustment, and hidden shared recipes (`HIDDEN`) are on hold until the admin screens are designed.

## Out of scope (release 1)

Payments other than the subscription, shop partnerships and e-commerce, device push notifications, fridge ingredient search, budgets and advanced ledgers, nutrition or calorie targets, health-based personalization, announcements and bulk sending, native apps (the PWA ships to Google Play as a TWA), email signup and login.
