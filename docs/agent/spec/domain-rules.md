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
- Files (receipt and shared photos): <= 10 MB, jpg/png/heic. The frontend checks first; the server checks last (413, 415).
- Double clicks: buttons such as register and deduct are disabled until the request ends. No server `Idempotency-Key`.
- Concurrent edits of the same ingredient: last request wins. Deduction checks the remaining amount first.
- Errors: `{ code, message, status, traceId, fieldErrors?, details? }` with the 59 codes in [`errors.md`](errors.md). Input errors show as red text under the field (#DC2626). Errors without a screen show the shared toast. Errors with a screen use the OCR failure screen, the generation failure screen, the meal-plan overlap modal, or the quota-used-up screen.
- Empty data is not an error: return 200 with an empty value. Empty-list screens (8 places) are (screen pending).
- Loading: lists use skeletons. AI recipe generation uses a loading screen (up to 60 s, screen pending). Meal-plan generation has no loading screen (section 5).
- Success toast: after saving a recipe, registering a receipt, or moving cart items to the fridge, show how many cart items were added or removed (not shown when 0). Meal-plan generation is asynchronous, so its cart count goes into the completion notification (section 5). Wording is (proposal).
- Toasts use the design-system component `토스트` with three types: default, success, error (decided 11/22).
- Excluded ingredients (allergies) from settings are never used in recipe or meal-plan generation.
- No health or medical information is collected. Settings values are visible to the owner only.
- Terms of service and privacy policy: links on the login screen open a drawer (screen pending). No separate consent screen (proposal).

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

- **The server picks the unit from the ingredient name** (the internal `ingredient_catalog` dictionary, see [`../erd.md`](../erd.md) table 26). The user never chooses a unit. The unit is decided in this order (#145): the name equals a dictionary name exactly; otherwise the longest dictionary name (two characters or more, same category) contained in the name; otherwise `EA`. A unit printed on a receipt does not override it.
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
  - Data: delete excluded ingredients, tokens, codes, fridge ingredients, logs, receipts, meal plans, cart, notifications. Keep the `users` row with social ID and email cleared (re-signup possible). Recipes and anonymous shared recipes stay. Shared recipes of deleted members get no reward (proposal).
- **Subscription** (Toss Payments card auto-billing):
  - Settings "구독 하기" → p1.3.4. Before subscribing: next payment date (today + 1 month), price, 2 benefits, payment terms checkbox, subscribe button. While subscribed: "구독 중" badge, next payment date, price, "구독 해지 하기".
  - Price 9,900 KRW per month. Benefits: 4 recipes per day, 4 meal plans per month of 7 days.
  - Flow: check the terms (button disabled until then) → Toss payment window (register card for auto-billing) → on return the server charges the first month and starts the subscription on success. Failure or cancel in the Toss window returns to the before-subscription screen with a toast.
  - Payment terms go in the terms drawer (screen pending).
  - Renewal: on the next payment date (start + 1 month; the last day when that date does not exist) charge the same card and extend one month on success.
  - Auto-payment failure: (open).
  - Cancel: schedules the cancellation. Benefits stay until the day before the next payment date; no charge then, and the member returns to basic. The screen or modal after scheduling is (open).
  - After it ends: basic limits; meal plans already used this month still count. No re-subscribing while a cancellation is scheduled (`ALREADY_SUBSCRIBED`); after it ends, subscribe as new (proposal).
  - Account deletion ends the subscription immediately with no more charges. Payment records are kept. Refunds for the remaining period are (open).
  - The Toss secret key and `billingKey` stay on the server only; `billingKey` is stored encrypted.
  - Demo payment (Toss test keys or not): (open).

## 2. Fridge (김재연)

- First screen: "냉장고에 재료가 N개 있어요", "AI를 활용해 레시피 생성하기 →" (to recipe home), danger / soon / ok cards, ingredient list.
- Ingredient fields: name, category, storage (refrigerated, frozen, room temperature), expiry date, price (optional), amount. Manual add and OCR item edit/add use the same fields. All but price are required; the button is disabled until they are filled.
- Name is the ingredient, not the product ("비비고 냉동 만두" ✗ → "만두" ✓).
- Unit: the server picks it from the name (and the category, API 14 with `categoryCode`). The amount field is disabled until the name is set; the frontend asks API 14 again when the name or category changes so the shown unit equals the saved one. Changing the name re-picks only the unit; the number stays. This applies on add and on edit, also when the ingredient already has use/discard logs (each log keeps the name and unit recorded at that time).
- Amount edit: it fixes an input mistake, so it changes `amount` and `initial_amount` together (the discarded cost divides by `initial_amount`). Reject it with `INVALID_INPUT` once the ingredient has any use/discard log (#128).
- Expiry status: danger = expired or today (D-DAY); soon = D-1 to D-3; ok = later. Shown as `D-N`, `D-DAY`, `폐기-N`.
- Filter and sort: tapping a status card shows only that status; "재료 위치" and "재료 종류" sheets filter one at a time. Sorted by nearest expiry. List title "N개의 위험/임박/여유 재료". **No ingredient search.**
- Row tap opens the used/discarded sheet; the trailing `>` opens edit.
- Used/discarded: choose "사용 했어요" or "버렸어요" + amount + confirm. Discarded shows "이번 달 버린 식재료 금액에 더해져요".
- More than available: `INSUFFICIENT_QUANTITY` (400). The sheet caps the amount at what remains (capping method is a proposal).
- Amount reaches 0: delete the ingredient row; logs keep the name.
- Discarded cost = price × discarded amount ÷ original amount. No price, no cost. The screen showing this month's discarded cost has no position yet (screen pending).
- Delete: delete icon on the edit screen (screen pending). Allowed even with logs; logs keep the name.
- Price is optional on manual add too ("가격을 정확히 작성할수록 소비 패턴 분석률이 올라가요"). Priced ingredients count in spending patterns.
- Default expiry: ingredients from receipts and the cart get the category default shelf life. No "estimated" flag (only a notice on the OCR result screen). Estimation base date, frozen/room values, and recalculation are up to the developer.
- Expired ingredients are not deleted automatically; they stay as `폐기-N` for the user (proposal).
- No repurchase marker (release 2).

## 3. Receipts and spending (이동준)

- Scan: "영수증 스캔 하기" → camera, or "첨부 하기" / gallery icon → image upload (screens after upload are pending).
- Camera unavailable: shared toast "카메라를 쓸 수 없어요. 갤러리에서 골라 주세요" (`CAMERA_UNAVAILABLE`, wording is a proposal).
- No scan limit. OCR is synchronous (up to 30 s) with the OCR result skeleton. No "analysis done" notification.
- OCR failure: one failure screen with different wording for: unreadable (not a receipt, OCR error, timeout), no food items, purchase date older than 6 months, already registered. The user rescans (screen pending).
- Results contain food only (non-food such as 수세미, 냄비 is filtered out). Each item is filled with category, storage, amount, price, and expiry.
- Review on "영수증 재료": edit (p3.3.1), add (p3.3.2). The OCR result shows food items only, so there is no item removal (screens to add no. 5 canceled, 11/22). Edits stay in screen state and are sent with registration.
- Notice: "소비 기한은 카테고리 기본 보관 기간으로 추정 했어요. 소비기한을 확인 하시고 수정해 주세요".
- Register ("식재료 N개 등록 하기"): save the image, receipt, items, and fridge ingredients at once. Unregistered OCR results are never stored; leaving without registering records only an accuracy event.
- Purchase date and store name: empty if OCR cannot read them; both are required at registration.
- Receipts older than 6 months cannot be registered (rejected at scan as an OCR failure).
- **Duplicate receipts are blocked.** Same store + receipt number + purchase time = duplicate (without a number: store + purchase date + food items; without a time: purchase date). Checked right after the scan (OCR failure screen "이미 등록한 영수증이에요", `RECEIPT_DUPLICATE` 409) and again at registration.
- Cart auto-remove: cart items with the same name as a registered ingredient are removed, with the toast "장바구니에서 N개를 뺐어요" (screen pending).
- List: month picker "2026년 11월 ∨" (last 6 months), monthly total, "내 평균보다 N원 더 썼어요", groups by week (week, range, total), receipt rows (store icon, store, date, "식재료 N개", amount).
- Monthly total: food item amounts of that month's receipts (by purchase date).
- Average: difference from my past average (monthly average of the last 6 months). Excluding this month, using only months with records, and hiding when there is no history are (proposal).
- Store icon: match the store name to 14 codes (CU, GS25, SEVEN_ELEVEN, EMART24, EMART, HOMEPLUS, LOTTE_MART, COSTCO, TRADERS, HANARO, GS_FRESH, LOTTE_SUPER, EMART_EVERYDAY, HOMEPLUS_EXPRESS). No match: leave empty and show the default icon (matching method is a proposal).
- Detail: original image, "2026년 11월 영수증", store and branch, items (image, name, storage, amount, price), total (food items), delete.
- Registered receipts cannot be edited, only deleted. Fridge ingredients are edited in the fridge (editing a price there does not change the receipt).
- Delete: modal → delete receipt, items, and image; it leaves spending and patterns; fridge ingredients stay.
- Retention: 6 months from the purchase date; a daily early-morning job deletes older receipts with their images.
- Image storage type is the backend's choice; the server builds image URLs.
- "나의 소비" (spending pattern): month tabs for the last 6 months (built by the frontend), month spending (positive), "평균 보다 N원 더 지출", one category ratio bar with legend, Mon–Sun day row (amount under the selected day), "총 N원" and the food list for the selected day.
- Pattern scope: receipt items + ingredients added without a receipt that have a price (by registration date). Unpriced ones are treated as fridge-only and excluded. Discarded ingredients stay included (already bought).
- Category ratio: 12 categories; categories with 0 spending are left out (proposal).
- Day selection: default today (or the last purchase day if not this month, proposal). No spending: "해당 날짜에는 소비가 없습니다".
- No frequent items or purchase cycles.

## 4. AI recipes (김종수)

- Two modes: quick (fridge ingredients and household size, expiring first) and custom (preferences).
- Quick modal: "가구원 수와 냉장고의 재료에 맞게 레시피를 생성 합니다. 레시피를 생성하면 하루 생성 횟수를 차감 합니다" → 생성 하기.
- Custom: servings (required) + ingredients (from my fridge), taste, cooking time, tools, method, cuisine, allergens. One choice per sheet (confirm disabled until chosen). **No free text.** Default servings = household size (proposal).
- One response, no streaming. Loading screen up to 60 s (screen pending).
- Recipe supplement OFF: use only my ingredients. Too few (including an empty fridge) → `NO_INGREDIENTS` → generation failure screen "설정에서 레시피 재료 보충을 켜 보세요"; not charged (screen pending). ON: may use ingredients not in the fridge.
- AI error, timeout, or format failure: retry once, then fail with the failure screen (wording differs); not charged.
- No "AI가 생성한 레시피" label.
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
- Purposes: BALANCED (default), DIET, HIGH_PROTEIN, SIMPLE, SAVING. Remember the last choice as the next default. No medical or calorie targets. No "참고용 식단" label.
- Days: basic up to 5, premium up to 7. With too few ingredients, only as many days as possible.
- Short of days but >= 1 day: missing ingredients still go to the cart. The completion notification adds "재료가 부족해 N일치만 만들었어요" and "부족한 재료 N개를 장바구니에 담았어요" (wording is a proposal). No separate toasts.
- Not even one day: `MEAL_PLAN_NO_INGREDIENTS` → failure notification; the plan is `FAILED`, not charged.
- Each meal slot gets a full recipe from the start (no "menu name first, details later").
- Do not allocate the same ingredient amount to several meals (based on each slot's recipe ingredients; no per-slot planned-ingredient table; method is up to the developer).
- Overlap: if the new period overlaps an existing plan, show the warning modal (cancel / overwrite). Overwrite deletes the overlapping days' slots and refills them when the generation job completes (a failed job keeps the existing plan). Meals already cooked today stay and are not refilled. Overlap is not charged (modal pending).
- Slot ⋮: "식단 삭제" or "레시피 교체" + confirm. Delete removes that slot only. Replace generates a new recipe and charges 1 daily recipe ("레시피를 교체하게 될 경우 레시피 사용 횟수를 1회 차감 합니다"); not charged on failure.
- Past days are view only. ⋮ stays; tapping it shows the `PAST_DATE_NOT_ALLOWED` toast.
- Planning does not deduct fridge ingredients; only cook deduction does.
- Meal plans use the monthly quota; replace uses the daily recipe quota.
- **Asynchronous generation**: the overlap modal and the quota-exhausted screen are checked before generation and stay as they are. Then pressing generate sends API 38 (202) and goes straight to the fridge (index) screen with the toast "식단을 완성하면 알림을 보내드려요". There is no "generating" screen and no per-day progress.
- Completion: in-app notification "11월 24일부터 5일치 식단표가 완성됐어요" (start date and days made; wording is a proposal). Tapping it opens p5 at the plan's start date.
- Failure: in-app notification, not charged, no failure screen. AI error or timeout: "식단표를 만들지 못했어요. 잠시 후 다시 만들어 주세요. 생성 횟수는 차감되지 않았어요". Not even one day (`MEAL_PLAN_NO_INGREDIENTS`): "냉장고 재료가 부족해 식단표를 만들지 못했어요. 생성 횟수는 차감되지 않았어요". Tapping it opens the meal-plan setup screen. Recipe generation failures keep the failure screen (screens to add no. 8).
- Meal-plan notifications are created even for members who turned notifications off.
- Target: generation within 30 s, tune after measuring (measured from acceptance to job completion; the completion notification is sent at that moment). The 60 s limit is for recipes only; the server job's time limit is (open, #144).
- A new generate request while one of mine is `GENERATING` is rejected with `MEAL_PLAN_IN_PROGRESS` (409, error toast).
- Overwrite deletes the old overlapping slots when the job completes, so a failed job keeps the existing plan. (deferred, #144): whether `GENERATING`/`FAILED` plans show in the calendar (39) or count for overlap.

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

- Fifth top tab "장바구니".
- Items are added automatically only: meal-plan shortages, supplemented missing ingredients when saving my recipe, missing ingredients when saving a shared recipe. No manual add. The server sets the category.
- Rows: checkbox, image, source recipe name, ingredient + amount, "수량 변경". The same ingredient from different recipes stays separate.
- Actions: amount sheet, select all (N), "선택 삭제" → delete mode (red checks, "삭제 해제" to undo) → "N개의 재료 삭제".
- "N개의 재료 냉장고에 추가": moves to the fridge without input. Storage and expiry from category defaults, no price. Moved items leave the cart, toast "냉장고에 N개 넣었어요" (screen pending).
- Registering a receipt with the same ingredient name removes it automatically.
- Empty: "장바구니가 비어있습니다", buttons disabled.
- No external shop or checkout. If the schedule slips, cart and sharing (P1) move to release 2.

### Sharing (weekly recipes)

- Recipe tab "금주의 레시피" TOP 3 cards + 전체 보기 → "금주의 인기 레시피". No my recipes / shared tab switch.
- TOP 3 card: rank, photo, title, ingredient summary +N. No rank change. Fewer than 3: empty state (screen pending).
- Popular list: "11월 셋째 주", popular (default) / latest, search, rows (rank, photo, title, ingredient summary +N, rank change). Popular = TOP 100 by saves this week.
- Rank change vs last week: blue ↑N, red ↓N; "—" if no rank last week; same rank shown as "—" (proposal).
- Latest: "—" instead of rank and change, shared time on the right (1분 전 to 30일 전).
- Popular count: saves received this week (from Monday); ties go to the earlier share (proposal).
- Search over shared recipes; 0 results empty state (screen pending).
- Only recipes I generated with Remake can be shared, not recipes saved from others. Fully anonymous: no nickname or author info.
- Write: detail ⋮ "공유 하기" → 1 photo (required, "탭해서 이미지 수정"), title (required), recipe content (read only) → "레시피 공유 하기". Enabled only with title and photo. No confirm modal.
- The same recipe can be shared once per week (again next week).
- Shared detail: photo, title, flag (report), "재료 준비 · servings · time", ingredients (missing ones with a red border), steps with read-aloud, Tips!, heart + save count, "레시피 저장", "레시피 저장 시 부족한 재료는 장바구니로 들어가요".
- Save: the heart and "레시피 저장" are the same action. Puts the original recipe in my saved list (no copy). Save count +1; tapping again cancels (-1). Missing ingredients go to the cart. Removing it from my list leaves the shared post.
- Report: flag → 4 reasons (`OFFENSIVE_IMAGE` 불쾌한 이미지 사용, `AD_TITLE` 광고성 제목, `DANGEROUS_RECIPE` 위험한 조리법 공유, `OTHER` 기타) → 신고 하기. No text input, even for 기타. Once per person per recipe; again → `REPORT_ALREADY_EXISTS` toast. No auto hide; operators review (handling screen comes with the admin design, on hold).
- No way to take down my own post (no screen).
- **Reward**: members who posted last week's top 1–3 recipes by saves. Per recipe rank, so one member can receive more than once. 1st +10, 2nd +6, 3rd +3 recipe generations, usable for one month from the grant.
- Week = Monday 00:00 to Sunday 24:00 KST. The Monday 00:00 job records last week's TOP 100 and grants rewards, counting only saves that still exist then. Never grant the same week twice. Own saves do not count; 0 saves get no reward (proposal). No reward screen or notification; only the remaining count changes.

## Admin (on hold)

- Access: social login + ADMIN role at `/admin`. No separate login.
- Metrics, logs, threshold alerts, user management, report handling, account suspension (`SUSPENDED`), per-member quota adjustment, and hidden shared recipes (`HIDDEN`) are on hold until the admin screens are designed.

## Out of scope (release 1)

Payments other than the subscription, shop partnerships and e-commerce, device push notifications, fridge ingredient search, budgets and advanced ledgers, nutrition or calorie targets, health-based personalization, announcements and bulk sending, native apps (the PWA ships to Google Play as a TWA), email signup and login.
