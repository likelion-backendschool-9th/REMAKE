# Domain rules

Source: PRD v3.1, policy doc v3.1 (Korean, kept in Notion / Google Sheets; not in the repo). Items marked (proposal) are not confirmed by the PO yet; implement them behind named constants.

## Limits (quota)

Values from PRD v3.1 (11/20). Premium is UI only in release 1, and the plan difference is kept small on purpose (the business model is not a focus).

| Feature | Basic | Premium (UI only in release 1) |
|---|---|---|
| AI recipe generation | 2 per day | 4 per day |
| Meal plan generation | 1 per month, up to 5 days | 4 per month, up to 7 days |
| Receipt scan (OCR) | **no limit** (calls still logged) | no limit |

- Only successful generations count; failures do not. Reset: daily 00:00 KST (recipes), 1st of month 00:00 (meal plans).
- Weekly reward for the top 3 saved shared recipes: 1st +10, 2nd +6, 3rd +3 recipe generations to **each recipe's author** (rewards follow recipes, so one author may receive more than one), **valid one month** after grant (`generation_quota_grants.expires_at`).
- Slot-detail generation for a meal-plan slot does not use the daily recipe quota (proposal).

## Expiry status

DANGER = expired or D-DAY; SOON = D-1..D-3; OK = later. Notifications are sent every day while an ingredient is within 3 days of expiry, D-DAY included (fixed, not user-configurable). Daily 09:00 KST batch. The notification rule differs from the screen status on purpose: D-DAY is DANGER on the fridge screen and "expiring soon" (`EXPIRY_SOON`) in notifications.

## Notifications

Types `EXPIRY_SOON` (per ingredient) and `EXPIRED` (one per day, "N expired items"). One on/off switch in settings. No receipt-finished, no recommendation, no restock notifications.

## Units (14 codes, shared by fridge and recipes)

| Code | Label | Kind | Base | Fridge | Recipe |
|---|---|---|---|---|---|
| G | g | weight | 1 g | yes | yes |
| KG | kg | weight | 1000 g | yes | yes |
| ML | ml | volume | 1 ml | yes | yes |
| L | L | volume | 1000 ml | yes | yes |
| TBSP | 큰술 | volume | 15 ml | no | yes |
| TSP | 작은술 | volume | 5 ml | no | yes |
| CUP | 컵 | volume | 200 ml | no | yes |
| EA | 개 | count | - | yes (default) | yes |
| PACK | 팩 | count | - | yes | yes |
| BUNCH | 단 | count | - | yes | yes |
| PIECE | 쪽 | count | - | yes | yes |
| SHEET | 장 | count | - | yes | yes |
| PINCH | 꼬집 | small | - | no | yes |
| TO_TASTE | 약간 | no amount | - | no | yes |

- Auto conversion only within the same kind (weight<->weight, volume<->volume incl. spoon/cup to ml). Different kinds (e.g. fridge 2 EA onion vs recipe 200 g) are **not** auto-deducted; the user confirms (`needsCheck`).
- PINCH and TO_TASTE are never deducted.
- Packaging words (bag, can, bottle, tub) map to PACK; counting words (animal, egg, block, head) map to EA. If OCR cannot read a quantity, register 1 EA.
- Stepper step: g/ml 50, kg/L 0.5, count units 1. Decimals allowed in manual input (up to 3 decimals on receipt confirm), no fractions. Recipe display may show 1/2, 1/3, 1/4 only.
- The AI must only use these unit codes; the server rejects others.

## Categories (12 codes) and default shelf life (draft)

VEGETABLE 7d, FRUIT 7d, MEAT 3d, SEAFOOD 2d, DAIRY_EGG 10d, GRAIN 30d, BEAN_TOFU 5d, PROCESSED 30d, SIDE_DISH 14d, SEASONING 180d, BEVERAGE 30d, ETC 7d. Used when a receipt has no expiry date (marked `isExpirationEstimated`, UI shows "estimate, please check"). Storage locations: refrigerated, frozen, room temperature. Non-food receipt lines (detergent, tissue, cookware) are filtered out by OCR and never stored; Remake manages food only.

## Receipts

- Flow: scan (analysis only) -> confirm screen (edit, add, remove, choose which to register) -> save. Unconfirmed OCR is not stored; abandonment is measured only via event `receipt_result_abandoned`.
- Confirmed receipts cannot be edited; delete only. Deleting removes it from spending stats but keeps registered fridge ingredients.
- OCR may leave the purchase date and store name unset. The user enters missing values before confirming the receipt.
- Retention 6 months from the purchase date on the receipt; average-spending comparison also uses the last 6 months. Older receipts cannot be registered.
- Monthly total = food items of confirmed receipts. Selecting a month changes the total and the list together.
- Discarded ingredients stay in spending (counted as purchased spending); they are not subtracted from the monthly total. The monthly analysis shows the discarded amount separately (sum of `ingredient_logs.cost_amount`); its Figma screen will be added.
- Store icon: 15 codes (CU, GS25, SEVEN_ELEVEN, EMART24, EMART, HOMEPLUS, LOTTE_MART, COSTCO, TRADERS, HANARO, GS_FRESH, LOTTE_SUPER, EMART_EVERYDAY, HOMEPLUS_EXPRESS, ETC).

## Recipes

- Modes: QUICK (fridge ingredients, expiring first) and CUSTOM (servings required + selected ingredients, taste, time, tools, method, cuisine, excluded allergens). Selection only, no free text.
- Setting "recipe supplement" ON lets the AI use ingredients not in the fridge.
- Allergen warning when a result contains excluded ingredients. Show "AI가 생성한 레시피". Recipes are never edited after creation; the source is not shown.
- After cooking: choose which ingredients to deduct and adjust amounts; feedback ("recommend or not") is stored as events (behavior is an open question).
- Favorites: star toggle on saved recipes; list can filter favorites.

## Meal plan

- Month calendar (lunch/dinner/breakfast selectable), settings: start date, one purpose, meals, servings. Purposes: BALANCED (default), DIET, HIGH_PROTEIN, SIMPLE, SAVING; remembered from last use; no medical or calorie targets; show "참고용".
- If ingredients are insufficient, generate fewer days (up to what can be made) and show shortage; missing ingredients go to the cart.
- Tapping a slot opens the recipe detail. Add meal is required (put a saved recipe in an empty slot); delete is not built; past plans are viewable.
- Ingredient quantities must not be allocated twice across slots (hardest technical part).

## Cart

Items are added automatically only (meal-plan shortages, saving a recipe incl. shared). Grouped by recipe; no merging of duplicates (proposal). Change amount, delete selected. No external shopping or checkout.

## Sharing and ranking

- Share only Remake-generated recipes; title and at least one cooking photo required; confirm modal; fully anonymous; report reasons PHOTO, OFFENSIVE, DANGEROUS, OTHER; admin decides (no auto hide).
- Weekly ranking TOP 100 by saves this week. The weekly job at Monday 00:00 fixes last week's ranking and resets the count: it counts saves made last week that still exist when the job runs (see `../erd.md` §24); ranking screen shows rank change vs last week (arrow); latest sort shows "-" and shared time; TOP 3 card on recipe home shows rank only.
- Saving a shared recipe copies it into the saver's list and adds shortages to the cart; own posts excluded from counts.

## Account

Social login only (Kakao, Naver, Google). No email signup. Deleting an account requires **social re-authentication** with the provider the user signed up with ([#54](https://github.com/likelion-backendschool-9th/REMAKE/issues/54)); email codes are not used and the account is identified only by `(provider, provider_user_id)`. After re-auth, unlink/revoke the provider connection (Kakao `unlink`, Naver token delete, Google `revoke`); if unlinking fails, the service deletion still proceeds and the failure is logged. The service deletion is a soft delete: keep the `users` row, set `deleted_at`, clear `provider_user_id` and `email`, delete personal data and receipt images, and revoke all refresh tokens. Events remain de-identified.

## Out of scope (release 1)

Payments, e-commerce integration, budget/advanced ledger, health-condition personalization, announcements/bulk push, native apps, email login.
