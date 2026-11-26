# Event tracking contract

Source of truth: the Korean "Remake event definition (Figma-based)" v2 (2026-11-22, Notion "이벤트 v2"; not in the repo). Names are snake_case and fixed; to add or rename an event, update that document first and ask the user.

- Scale: 79 events (P0 36, P1 37, P2 5, plus E50 with priority (open, #53); CLIENT 45, SERVER 34), 103 properties (v2 has 102; `deleted_item_count` restored 11/24), 21 metrics (north star + 20 KPIs), 41 screen codes.
- API numbers are the numbers in [`api-summary.md`](api-summary.md); error codes are from [`errors.md`](errors.md).
- There are no admin events. The operator page (Figma, 11/26) shows the metrics in a1-a3 and the raw events in a6 (APIs 61-64).

## Rules

- **Collection**: events go into the server `events` table (no external analytics tool). The frontend sends CLIENT events to `POST /api/v1/user/events` (API 56, batch <= 50, 202). The server records SERVER events itself.
- **One action is recorded once**, as CLIENT or SERVER (column Source). Results that matter (OCR and AI results, quota, notifications, rewards, ingredient add/deduct, cart add/remove) are SERVER.
- **Naming**: lowercase snake_case. Events are `object_action` in the past tense. Booleans `is_`/`has_`/`was_`, durations `_ms`/`_sec`, money `_amount`, counts `_count`. Code values are lowercase.
- **Priority**: P0 is required to compute a PRD metric, P1 helps explain causes, P2 if time allows.
- **Publisher**: 이준호 builds the shared `EventTracker` module (async insert, PII filter) and the ingest API. Each owner calls `EventTracker` where their API ends (success, failure, or rejection). CLIENT events are sent by the frontend owner of the screen.
- **Errors**: put the error code from `errors.md` in `error_code` unchanged. Errors the server knows (AI, OCR, quota, login) are SERVER events; errors seen on a screen are `error_shown`. AI failures are not written to `usage_logs`, so the event is the only record.
- **Never record**: email, name, social ID, allergy or excluded-ingredient names, typed ingredient names, recipe content, receipt photos, any free text. Record excluded ingredients as a count and ingredients as `category_code`.
- `properties`: flat JSON, string/number/boolean only, <= 25 keys.
- Tracking must never break the user flow (async, swallow errors).
- **Retention**: raw events are kept 180 days. On account deletion only `user_id` stays (`events.user_id` has no foreign key). There is no daily aggregation table for now: compute the metrics below directly from `events` (the operator page a1-a3 and a6 query them through APIs 61-64); decide on a daily table during development.
- Decision [#46](https://github.com/likelion-backendschool-9th/REMAKE/issues/46): `cart_item_added` (E62) is emitted only by `AddCartItemsUseCase`, once per call that inserts rows. Record `cart_source` (`meal_plan`, `own_recipe`, `shared_recipe`) and `item_count` (inserted rows, not groups or summed quantities). Emit nothing if the inserted count is 0. The calling meal-plan and recipe APIs do not emit it themselves. Never record a rolled-back add.
- Meal-plan generation (API 38) is asynchronous: emit `meal_plan_generated` (E53) and `meal_plan_generate_failed` (E54) when the server job ends, not when API 38 replies 202. E54 `error_code` is `MEAL_PLAN_NO_INGREDIENTS` or an `AI_*` code. `notification_sent` (E66) also covers meal-plan notifications (`notification_type` `meal_plan_completed` / `meal_plan_failed`, `batch_count` 1); the banner and list events (E67, E68) count them too. On E53/E54, `duration_ms` runs from accepting API 38 to the job end.

## Common fields and user attributes

| Kind | Name | Type | Rule | Note |
| --- | --- | --- | --- | --- |
| common (`events` column) | `user_id` | number | Internal member number. Never social ID, email, or name | null before login. Only the number remains after deletion |
| common | `session_id` | string | New session after 30 minutes without activity | Session funnels |
| common | `source` | string | `CLIENT` / `SERVER` | One side only per action |
| common | `occurred_at` | timestamp | When the action happened, Korea time | Frontend sends device time; server corrects |
| common | `properties` | JSON | Event properties, flat, <= 25 keys | |
| common | `screen` | string | Screen code (below) | Frontend sets it; SERVER events take header `X-Screen`. null for scheduled jobs |
| common | `platform` | string | `TWA` / `PWA` / `WEB` | Header `X-Client-Platform` |
| common | `app_version` | string | App version | Header `X-App-Version` |
| common | `is_seed` | boolean | true for demo seed events, default false | Only the seed script sets true. Filter `false` for real metrics |
| user attribute | `household_size` | number | Not in events; join `users` | `users.household_size` |
| user attribute | `plan` | string | `BASIC` / `PREMIUM` | `users.plan` |
| user attribute | `signup_date` | date | Cohort key | `users.created_at` |
| user attribute | `provider` | string | Login provider | `users.provider` |

## Events

CLIENT events are sent by the frontend through API 56. SERVER events are recorded where the listed API or job ends.

### Common

| ID | Name | Source | Priority | Properties | Metrics | Publisher (API no.) | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E01 | `app_opened` | CLIENT | P0 | entry_source, is_standalone | M15 | 56 (frontend) | frontend (assigned by 이준호) |
| E02 | `screen_viewed` | CLIENT | P0 | screen_name, previous_screen | M15, M20 | 56 (frontend) | frontend (assigned by 이준호) |
| E03 | `error_shown` | CLIENT | P0 | error_code, http_status, screen_name | M20 | 56 (frontend) | frontend (assigned by 이준호) |
| E07 | `quota_exceeded` | SERVER | P0 | limit_feature, limit_total, used_count | M16 | 28 `POST /user/recipes/generate`<br>29 `POST /user/recipes/{id}/regenerate`<br>42 `POST /user/meal-plan-slots/{slotId}/replace`<br>38 `POST /user/meal-plans/generate` | 김종수 (28·29), 한윤수 (38·42) |

### Login and onboarding

| ID | Name | Source | Priority | Properties | Metrics | Publisher (API no.) | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E08 | `login_started` | CLIENT | P0 | provider, is_recent_provider | M17 | 56 (frontend) | frontend (assigned by 이준호) |
| E09 | `login_succeeded` | SERVER | P0 | provider, is_new_user | M17 | 1 `POST /public/auth/oauth/{provider}` | 이준호 |
| E10 | `login_failed` | SERVER | P0 | provider, error_code | M17, M20 | 1 `POST /public/auth/oauth/{provider}` | 이준호 |
| E11 | `signup_completed` | SERVER | P0 | provider | M01, M02, M17 | 1 `POST /public/auth/oauth/{provider}` | 이준호 |
| E12 | `onboarding_step_completed` | CLIENT | P0 | onboarding_step | M01 | 56 (frontend) | frontend (assigned by 이준호) |
| E13 | `onboarding_completed` | SERVER | P0 | excluded_count | M01, M17 | 4 `PUT /user/onboarding` | 이준호 |
| E14 | `setting_changed` | CLIENT | P1 | setting_key, new_value_bool | - | 56 (frontend) | frontend (assigned by 이준호) |
| E15 | `excluded_ingredients_updated` | CLIENT | P1 | excluded_count | - | 56 (frontend) | frontend (assigned by 이준호) |
| E16 | `logout_confirmed` | CLIENT | P1 | (common only) | M15 | 56 (frontend) | frontend (assigned by 이준호) |
| E17 | `account_deletion_requested` | SERVER | P1 | provider, deletion_method | - | 10 `POST /user/account/deletion` | 이준호 |
| E18 | `account_deleted` | SERVER | P1 | tenure_days, deletion_method | M15 | 10 `POST /user/account/deletion` | 이준호 |

### Subscription

| ID | Name | Source | Priority | Properties | Metrics | Publisher (API no.) | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E83 | `subscription_checkout_started` | CLIENT | P1 | (common only) | - | 56 (frontend) | frontend (assigned by 이준호) |
| E84 | `subscription_started` | SERVER | P1 | payment_amount | - | 59 `POST /user/subscription` | 이준호 |
| E85 | `subscription_payment_failed` | SERVER | P1 | is_renewal, error_code | - | 59 `POST /user/subscription`<br>job: subscription auto-payment | 이준호 |
| E86 | `subscription_renewed` | SERVER | P1 | payment_amount, renewal_count | - | job: subscription auto-payment | 이준호 |
| E87 | `subscription_canceled` | SERVER | P1 | subscribed_days | - | 60 `POST /user/subscription/cancel` | 이준호 |
| E88 | `subscription_ended` | SERVER | P1 | end_reason, subscribed_days | - | job: subscription auto-payment<br>10 `POST /user/account/deletion` | 이준호 |

### Fridge

| ID | Name | Source | Priority | Properties | Metrics | Publisher (API no.) | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E19 | `fridge_list_viewed` | CLIENT | P0 | total_count, danger_count, soon_count, ok_count | M07, M15 | 56 (frontend) | frontend (assigned by 이준호) |
| E20 | `fridge_filter_applied` | CLIENT | P1 | filter_type, filter_value | - | 56 (frontend) | frontend (assigned by 이준호) |
| E21 | `ingredient_add_started` | CLIENT | P0 | add_source | M05 | 56 (frontend) | frontend (assigned by 이준호) |
| E22 | `ingredient_added` | SERVER | P0 | add_source, category_code, storage, has_price, duration_sec | M02, M05, M17 | 15 `POST /user/ingredients`<br>21 `POST /user/receipts`<br>47 `POST /user/cart-items/move-to-fridge` | 김재연 (15), 이동준 (21), 최유리 (47) |
| E23 | `ingredient_edited` | CLIENT | P1 | edit_field | - | 56 (frontend) | frontend (assigned by 이준호) |
| E24 | `ingredient_deleted` | CLIENT | P2 | category_code | - | 56 (frontend) | frontend (assigned by 이준호) |
| E25 | `ingredient_used` | SERVER | P0 | use_via, category_code, expiry_status, used_ratio | M00, M07 | 18 `POST /user/ingredients/{id}/consume`<br>32 `POST /user/recipes/{id}/cook` | 김재연 (18), 김종수 (32) |
| E26 | `ingredient_discarded` | SERVER | P0 | category_code, expiry_status, used_ratio, has_price, cost_amount | M00, M07, M18 | 18 `POST /user/ingredients/{id}/consume` | 김재연 |
| E27 | `ingredient_expired` | SERVER | P0 | category_code, has_price | M00, M07 | job: expiry notifications | 이상호 |

### Receipts

| ID | Name | Source | Priority | Properties | Metrics | Publisher (API no.) | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E28 | `receipt_scan_started` | CLIENT | P0 | scan_method | M03, M05 | 56 (frontend) | frontend (assigned by 이준호) |
| E30 | `receipt_scan_succeeded` | SERVER | P0 | item_count, food_item_count, duration_ms | M03, M10, M16 | 20 `POST /user/receipts/scan` | 이동준 |
| E31 | `receipt_scan_failed` | SERVER | P0 | error_code, duration_ms | M03, M10, M16, M20 | 20 `POST /user/receipts/scan` | 이동준 |
| E32 | `receipt_item_edited` | CLIENT | P0 | item_index, edit_field | M04 | 56 (frontend) | frontend (assigned by 이준호) |
| E33 | `receipt_item_added_manually` | CLIENT | P1 | (common only) | M04 | 56 (frontend) | frontend (assigned by 이준호) |
| E34 | `receipt_saved` | SERVER | P0 | receipt_id, registered_count, ocr_item_count, edited_item_count, deleted_item_count, added_item_count, duration_sec, food_total_amount, cart_removed_count | M04, M05, M12, M17 | 21 `POST /user/receipts` | 이동준 |
| E76 | `receipt_result_abandoned` | CLIENT | P0 | ocr_item_count, edited_item_count, deleted_item_count, added_item_count | M04 | 56 (frontend) | frontend (assigned by 이준호) |
| E35 | `receipt_list_viewed` | CLIENT | P1 | month_offset, has_comparison | M19 | 56 (frontend) | frontend (assigned by 이준호) |
| E36 | `receipt_detail_viewed` | CLIENT | P2 | (common only) | - | 56 (frontend) | frontend (assigned by 이준호) |
| E37 | `receipt_deleted` | CLIENT | P1 | (common only) | - | 56 (frontend) | frontend (assigned by 이준호) |
| E38 | `consumption_pattern_viewed` | CLIENT | P0 | month_offset | M19 | 56 (frontend) | frontend (assigned by 이준호) |

### AI recipes

| ID | Name | Source | Priority | Properties | Metrics | Publisher (API no.) | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E39 | `recipe_generate_started` | CLIENT | P0 | recipe_mode, servings, option_count, supplement_enabled | M08, M09 | 56 (frontend) | frontend (assigned by 이준호) |
| E40 | `recipe_generated` | SERVER | P0 | recipe_id, recipe_mode, is_regenerated, duration_ms, expiring_used_count, shortage_count, remaining_quota | M08, M09, M10, M16, M17 | 28 `POST /user/recipes/generate`<br>29 `POST /user/recipes/{id}/regenerate` | 김종수 |
| E41 | `recipe_generate_failed` | SERVER | P0 | recipe_mode, is_regenerated, error_code, duration_ms | M09, M10, M16, M20 | 28 `POST /user/recipes/generate`<br>29 `POST /user/recipes/{id}/regenerate` | 김종수 |
| E42 | `recipe_regenerated` | CLIENT | P1 | recipe_mode | M09 | 56 (frontend) | frontend (assigned by 이준호) |
| E43 | `recipe_detail_viewed` | CLIENT | P0 | recipe_id, detail_origin | M08, M11 | 56 (frontend) | frontend (assigned by 이준호) |
| E44 | `recipe_saved` | SERVER | P0 | recipe_id, save_source, cart_added_count | M14, M17 | 34 `PUT /user/saved-recipes/{recipeId}`<br>53 `PUT /user/shared-recipes/{id}/save` | 김종수 (34), 최유리 (53) |
| E45 | `recipe_unsaved` | CLIENT | P1 | save_source | - | 56 (frontend) | frontend (assigned by 이준호) |
| E46 | `recipe_saved_list_viewed` | CLIENT | P1 | saved_count, list_filter, list_sort | - | 56 (frontend) | frontend (assigned by 이준호) |
| E47 | `step_play_started` | CLIENT | P2 | recipe_id | - | 56 (frontend) | frontend (assigned by 이준호) |
| E48 | `cook_deduct_viewed` | CLIENT | P1 | recipe_id, deduct_item_count | M08 | 56 (frontend) | frontend (assigned by 이준호) |
| E49 | `cook_completed` | SERVER | P0 | recipe_id, deducted_count, adjusted_count, from_meal_plan | M08, M11, M17 | 32 `POST /user/recipes/{id}/cook` | 김종수 |
| E50 | `cook_feedback_answered` | CLIENT | (open, #53) | recipe_id, recommended | - | 56 (frontend). Fridge (p2) bottom sheet "이 레시피 어땠나요?" after the cook deduction | frontend (assigned by 이준호) |
| E77 | `recipe_favorited` | CLIENT | P2 | is_favorite | - | 56 (frontend) | frontend (assigned by 이준호) |

### Meal plan

| ID | Name | Source | Priority | Properties | Metrics | Publisher (API no.) | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E51 | `meal_plan_setup_viewed` | CLIENT | P0 | remaining_quota | M16 | 56 (frontend) | frontend (assigned by 이준호) |
| E52 | `meal_plan_generate_started` | CLIENT | P0 | meal_purpose, meal_type_count, servings, start_offset_days | M09, M11 | 56 (frontend) | frontend (assigned by 이준호) |
| E78 | `meal_plan_overlap_answered` | CLIENT | P1 | overlap_day_count, overlap_choice | M11 | 56 (frontend) | frontend (assigned by 이준호) |
| E53 | `meal_plan_generated` | SERVER | P0 | meal_plan_id, meal_purpose, meal_type_count, generated_days, shortage_count, cart_added_count, was_overwritten, duration_ms, remaining_quota | M09, M10, M11, M16, M17 | 38 server job end (not the 202 response) | 한윤수 |
| E54 | `meal_plan_generate_failed` | SERVER | P0 | error_code, duration_ms | M09, M10, M16, M20 | 38 server job end (not the 202 response) | 한윤수 |
| E56 | `meal_calendar_viewed` | CLIENT | P1 | month_offset, has_plan | M11, M15 | 56 (frontend) | frontend (assigned by 이준호) |
| E57 | `meal_day_viewed` | CLIENT | P1 | date_relation, slot_count | - | 56 (frontend) | frontend (assigned by 이준호) |
| E58 | `meal_slot_opened` | CLIENT | P0 | meal_type, date_relation | M11 | 56 (frontend) | frontend (assigned by 이준호) |
| E79 | `meal_slot_deleted` | SERVER | P1 | meal_type, date_relation | M11 | 41 `DELETE /user/meal-plan-slots/{slotId}` | 한윤수 |
| E80 | `meal_slot_replaced` | SERVER | P1 | meal_type, date_relation, duration_ms, remaining_quota | M09, M10, M16 | 42 `POST /user/meal-plan-slots/{slotId}/replace` | 한윤수 |
| E81 | `meal_slot_replace_failed` | SERVER | P1 | error_code, duration_ms | M09, M10, M16, M20 | 42 `POST /user/meal-plan-slots/{slotId}/replace` | 한윤수 |

### Cart

| ID | Name | Source | Priority | Properties | Metrics | Publisher (API no.) | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E62 | `cart_item_added` | SERVER | P1 | cart_source, item_count | M12 | 34 `PUT /user/saved-recipes/{recipeId}`<br>53 `PUT /user/shared-recipes/{id}/save`<br>38 `POST /user/meal-plans/generate` | 최유리 |
| E63 | `cart_viewed` | CLIENT | P1 | item_count | M15 | 56 (frontend) | frontend (assigned by 이준호) |
| E64 | `cart_amount_changed` | CLIENT | P2 | (common only) | - | 56 (frontend) | frontend (assigned by 이준호) |
| E65 | `cart_items_deleted` | CLIENT | P1 | deleted_count, was_select_all | M12 | 56 (frontend) | frontend (assigned by 이준호) |
| E82 | `cart_moved_to_fridge` | SERVER | P1 | moved_count, was_select_all | M12 | 47 `POST /user/cart-items/move-to-fridge` | 최유리 |

### Notifications

| ID | Name | Source | Priority | Properties | Metrics | Publisher (API no.) | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E66 | `notification_sent` | SERVER | P0 | notification_type, batch_count | M06 | job: expiry notifications; meal-plan generation job end | 이상호 |
| E67 | `notification_banner_clicked` | CLIENT | P0 | unread_count | M06, M15 | 56 (frontend) | frontend (assigned by 이준호) |
| E68 | `notification_list_viewed` | CLIENT | P1 | unread_count | M06 | 56 (frontend) | frontend (assigned by 이준호) |

### Sharing

| ID | Name | Source | Priority | Properties | Metrics | Publisher (API no.) | Owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| E69 | `ranking_viewed` | CLIENT | P1 | ranking_view, ranking_sort, query_used | M14 | 56 (frontend) | frontend (assigned by 이준호) |
| E70 | `shared_recipe_viewed` | CLIENT | P1 | shared_recipe_id, view_from, rank_bucket | M14 | 56 (frontend) | frontend (assigned by 이준호) |
| E71 | `shared_write_started` | CLIENT | P1 | recipe_id | M13 | 56 (frontend) | frontend (assigned by 이준호) |
| E72 | `shared_recipe_published` | SERVER | P1 | shared_recipe_id | M13 | 51 `POST /user/shared-recipes` | 최유리 |
| E74 | `shared_recipe_reported` | SERVER | P1 | shared_recipe_id, report_reason | M14 | 55 `POST /user/shared-recipes/{id}/reports` | 최유리 |
| E75 | `weekly_reward_granted` | SERVER | P1 | reward_rank, reward_amount | M16 | job: weekly recipe reward | 최유리 |

## Properties

| Property | Type | Values | Events |
| --- | --- | --- | --- |
| `entry_source` | string | direct / shortcut / link | E01 |
| `is_standalone` | boolean | true(installed app) / false(browser) | E01 |
| `screen_name` | string | screen codes (below) | E02, E03 |
| `previous_screen` | string | same codes as screen_name; null on first entry | E02 |
| `error_code` | string | `errors.md` codes (e.g. OCR_UPSTREAM_ERROR, NETWORK_OFFLINE) | E03, E10, E85, E31, E41, E54, E81 |
| `http_status` | number | 400, 401, 429, 502 … | E03 |
| `limit_feature` | string | recipe / meal_plan | E07 |
| `limit_total` | number | recipe per day (basic 2, premium 4, + rewards), meal plan this month (basic 1, premium 4) | E07 |
| `used_count` | number | >= 0 | E07 |
| `provider` | string | kakao / naver / google | E08, E09, E10, E11, E17 |
| `is_recent_provider` | boolean | true / false | E08 |
| `is_new_user` | boolean | true / false | E09 |
| `onboarding_step` | string | household / allergy | E12 |
| `excluded_count` | number | >= 0 | E13, E15 |
| `setting_key` | string | household_size / recipe_supplement / notification | E14 |
| `new_value_bool` | boolean | true / false, null for numeric settings such as household size | E14 |
| `deletion_method` | string | email_code / reauth | E17, E18 |
| `tenure_days` | number | >= 0 | E18 |
| `total_count` | number | >= 0 | E19 |
| `danger_count` | number | >= 0 | E19 |
| `soon_count` | number | >= 0 | E19 |
| `ok_count` | number | >= 0 | E19 |
| `filter_type` | string | status / location / category | E20 |
| `filter_value` | string | DANGER/SOON/OK, FRIDGE/FREEZER/PANTRY, category codes | E20 |
| `add_source` | string | manual / receipt / cart | E21, E22 |
| `category_code` | string | 12 category codes (ERD Appendix A) | E22, E24, E25, E26, E27 |
| `storage` | string | FRIDGE / FREEZER / PANTRY | E22 |
| `has_price` | boolean | true / false | E22, E26, E27 |
| `duration_sec` | number | seconds | E22, E34 |
| `edit_field` | string | name / category / storage / amount / expiration / price | E23, E32 |
| `use_via` | string | manual / cook | E25 |
| `expiry_status` | string | danger / soon / ok | E25, E26 |
| `used_ratio` | number | > 0 and <= 1 | E25, E26 |
| `cost_amount` | number | KRW, >= 0 | E26 |
| `scan_method` | string | camera / upload | E28 |
| `item_count` | number | >= 0 | E30, E62, E63 |
| `food_item_count` | number | >= 0 | E30 |
| `duration_ms` | number | milliseconds (E53, E54: from accepting API 38 to the job end) | E30, E31, E40, E41, E53, E54, E80, E81 |
| `receipt_id` | number | internal receipt id | E34 |
| `item_index` | number | from 0 | E32 |
| `registered_count` | number | >= 0 | E34 |
| `ocr_item_count` | number | >= 0 | E34, E76 |
| `edited_item_count` | number | >= 0 | E34, E76 |
| `deleted_item_count` | number | >= 0; OCR items the user removed before registering (restored 11/24) | E34, E76 |
| `added_item_count` | number | >= 0 | E34, E76 |
| `food_total_amount` | number | KRW | E34 |
| `cart_removed_count` | number | >= 0; actual cart rows deleted at zero after selected receipt deductions (#57), not deducted quantity or request count | E34 |
| `month_offset` | number | 0 (this month), -1 (last month) ... | E35, E38, E56 |
| `has_comparison` | boolean | true / false | E35 |
| `recipe_mode` | string | quick / custom | E39, E40, E41, E42 |
| `servings` | number | >= 1 | E39, E52 |
| `option_count` | number | 0~5 | E39 |
| `supplement_enabled` | boolean | true / false | E39 |
| `recipe_id` | number | internal recipe id | E40, E43, E44, E47, E48, E49, E50, E71 |
| `recommended` | boolean | true (추천해요) / false (추천하지 않아요) | E50 |
| `is_regenerated` | boolean | true / false | E40, E41 |
| `expiring_used_count` | number | >= 0 | E40 |
| `shortage_count` | number | >= 0 | E40, E53 |
| `remaining_quota` | number | >= 0 | E40, E51, E53, E80 |
| `detail_origin` | string | saved / meal_plan | E43 |
| `save_source` | string | own / shared | E44, E45 |
| `cart_added_count` | number | >= 0 | E44, E53 |
| `saved_count` | number | >= 0 | E46 |
| `list_filter` | string | all / favorite / available | E46 |
| `list_sort` | string | saved_desc / saved_asc | E46 |
| `deduct_item_count` | number | >= 0 | E48 |
| `deducted_count` | number | >= 0 | E49 |
| `adjusted_count` | number | >= 0 | E49 |
| `from_meal_plan` | boolean | true / false | E49 |
| `is_favorite` | boolean | true / false | E77 |
| `meal_purpose` | string | balanced / diet / high_protein / simple / saving | E52, E53 |
| `meal_type_count` | number | 1~3 | E52, E53 |
| `start_offset_days` | number | >= 0 (today) | E52 |
| `overlap_day_count` | number | >= 1 | E78 |
| `overlap_choice` | string | overwrite / cancel | E78 |
| `meal_plan_id` | number | internal meal plan id | E53 |
| `generated_days` | number | 1~7 | E53 |
| `was_overwritten` | boolean | true / false | E53 |
| `has_plan` | boolean | true / false | E56 |
| `date_relation` | string | past / today / future | E57, E58, E79, E80 |
| `slot_count` | number | 0~3 | E57 |
| `meal_type` | string | breakfast / lunch / dinner | E58, E79, E80 |
| `cart_source` | string | meal_plan / own_recipe / shared_recipe | E62 |
| `deleted_count` | number | >= 1 | E65 |
| `was_select_all` | boolean | true / false | E65, E82 |
| `moved_count` | number | >= 1 | E82 |
| `notification_type` | string | expiry_soon / expired / meal_plan_completed / meal_plan_failed | E66 |
| `batch_count` | number | >= 1 | E66 |
| `unread_count` | number | >= 0 | E67, E68 |
| `ranking_view` | string | top3 / full | E69 |
| `ranking_sort` | string | popular / latest | E69 |
| `query_used` | boolean | true / false | E69 |
| `shared_recipe_id` | number | internal shared recipe id | E70, E72, E74 |
| `view_from` | string | top3 / ranking / search | E70 |
| `rank_bucket` | string | 1-3 / 4-10 / 11-100 / none | E70 |
| `report_reason` | string | offensive_image / ad_title / dangerous_recipe / other | E74 |
| `reward_rank` | number | 1~3 | E75 |
| `reward_amount` | number | 10 / 6 / 3 | E75 |
| `payment_amount` | number | 9900 | E84, E86 |
| `is_renewal` | boolean | true(monthly auto-payment) / false(first payment) | E85 |
| `renewal_count` | number | >= 1 | E86 |
| `subscribed_days` | number | >= 0 | E87, E88 |
| `end_reason` | string | canceled(period ended after cancel) / account_deleted(withdrawal) / auto-payment failure value added once decided | E88 |

## Metrics

| ID | Metric | Calculation | Events | Target |
| --- | --- | --- | --- | --- |
| M00 | North star: used-before-expiry rate | `ingredient_used` / (`ingredient_used` + `ingredient_discarded` + `ingredient_expired`), by period and category | E25, E26, E27 | rising trend |
| M01 | Login to onboarding completion | users with `onboarding_completed` / users with `signup_completed` (within 24 h). Step drop-off from `onboarding_step_completed` | E11, E12, E13 | >= 70% |
| M02 | First ingredient within 24 h of signup | users with >= 1 `ingredient_added` within 24 h / users with `signup_completed` | E11, E22 | >= 60% |
| M03 | Receipt OCR success rate | `receipt_scan_succeeded` / (succeeded + failed). Count `RECEIPT_NO_FOOD_ITEMS`, `RECEIPT_TOO_OLD`, `RECEIPT_DUPLICATE` separately from OCR quality | E28, E30, E31 | >= 90% |
| M04 | OCR item edit rate | sum of `edited_item_count` + `deleted_item_count` in `receipt_saved` + `receipt_result_abandoned` / sum of their `ocr_item_count` (a removed item was read wrongly). Adds are secondary | E32, E33, E34, E76 | <= 20% |
| M05 | Receipt registration time | per ingredient: receipt = median of `receipt_saved.duration_sec` / `registered_count`; manual = median of `ingredient_added(add_source=manual).duration_sec` | E21, E22, E28, E34 | <= 50% of manual |
| M06 | Expiry notification to view rate | users with `notification_banner_clicked` or `notification_list_viewed` / users with `notification_sent(expiry_soon)` (same day) | E66, E67, E68 | >= 25% |
| M07 | Expiring ingredient use rate | `ingredient_used` with `expiry_status` danger or soon / (used + discarded + `ingredient_expired` in the same states) | E19, E25, E26, E27 | >= 70% |
| M08 | Recipe generation to cook rate | `cook_completed(recipe_id)` / `recipe_generated`. Middle steps `recipe_detail_viewed`, `cook_deduct_viewed` | E39, E40, E43, E48, E49 | >= 30% |
| M09 | AI generation success rate | success / (success + failure), separately for recipes (E40/E41), meal plans (E53/E54), slot replace (E80/E81). Count `NO_INGREDIENTS` separately from AI quality | E39, E40, E41, E42, E52, E53, E54, E80, E81 | >= 95% |
| M10 | AI and OCR response time | p50 and p95 of `duration_ms` (recipe, meal plan, slot replace, OCR separately) | E30, E31, E40, E41, E53, E54, E80, E81 | meal plan within 30 s (PRD; E53/E54 job duration from acceptance to job completion, when the notification is sent); recipe server limit 60 s |
| M11 | Meal plan to slot open rate | users with `meal_slot_opened` / users with `meal_plan_generated` (within 7 days). Delete and replace are secondary | E43, E52, E53, E56, E58, E78, E79, E49 | >= 60% |
| M12 | Cart to fridge conversion | (sum of `cart_moved_to_fridge.moved_count` + sum of `receipt_saved.cart_removed_count`) / sum of `cart_item_added.item_count` (same period). User-deleted share (`cart_items_deleted.deleted_count`) is secondary | E34, E62, E65, E82 | trend only |
| M13 | Shared recipe write completion | `shared_recipe_published` / `shared_write_started` | E71, E72 | >= 50% |
| M14 | Shared recipe save and report rate | `recipe_saved(save_source=shared)` / `shared_recipe_viewed`; `shared_recipe_reported` / `shared_recipe_viewed` | E44, E69, E70, E74 | report rate <= 1% |
| M15 | Retention D1, D7, WAU | based on `app_opened`; cohorts by signup date; weekly active users | E01, E02, E16, E18, E19, E56, E63, E67 | D7 >= 25% |
| M16 | AI and OCR calls and quota use | total success and failure events; users with `quota_exceeded` / calling users; reward counts from `weekly_reward_granted` | E07, E30, E31, E40, E41, E51, E53, E54, E75, E80, E81 | cost control |
| M17 | Core flow funnel | `login_succeeded` → `signup_completed` → `onboarding_completed` → `ingredient_added` → `recipe_generated` → `cook_completed` (receipt path: `receipt_scan_started` → `receipt_saved` → `ingredient_added`) | E08, E09, E10, E11, E13, E22, E34, E40, E44, E49, E53 | finish the core flow |
| M18 | Discarded food cost this month | monthly sum of `ingredient_discarded.cost_amount` (`has_price=true` only; count unpriced ones separately) | E26 | falling trend |
| M19 | Spending pattern usage | users with `consumption_pattern_viewed` / users with `receipt_list_viewed` | E35, E38 | usage check |
| M20 | Error exposure rate | `error_shown` / `screen_viewed`, by `error_code` and `screen_name` | E02, E03, E10, E31, E41, E54, E81 | lower is better |

## Screen codes

Values for `screen`, `screen_name`, and `previous_screen`. Screens marked "screens to add" are not in Figma yet.

| Code | Figma screen | Area |
| --- | --- | --- |
| `splash` | Splash | common |
| `login` | p1 login | common |
| `onboarding_household` | p1.1 household size (onboarding) | common |
| `onboarding_allergy` | p1.2 allergy selection (onboarding) | common |
| `notification_list` | p6 notifications | common |
| `setting_main` | p1.3 settings | settings |
| `logout_modal` | p1.3 logout modal | settings |
| `setting_household` | p1.3.1 household settings | settings |
| `setting_allergy` | p1.3.2 excluded allergy ingredients | settings |
| `account_delete_modal` | p1.3 account deletion confirm modal | settings |
| `account_delete_code` | account deletion email code | settings |
| `subscription_manage` | p1.3.4 subscription management (before and during subscription) | settings |
| `fridge_list` | p2 fridge (incl. location and type sheets) | fridge |
| `ingredient_consume` | p2 ingredient used/discarded sheet | fridge |
| `ingredient_edit` | p2.1 edit ingredient | fridge |
| `ingredient_add` | p2.2 add ingredient | fridge |
| `receipt_list` | p3 receipts | receipts |
| `receipt_scan` | p3.1 camera, image upload (screens to add no. 4) | receipts |
| `receipt_loading` | p3.3 OCR result skeleton | receipts |
| `receipt_scan_error` | OCR failure screen (screens to add no. 4) | receipts |
| `receipt_result` | p3.3 OCR result | receipts |
| `receipt_item_edit` | p3.3.1 edit OCR item | receipts |
| `receipt_item_add` | p3.3.2 add OCR item | receipts |
| `receipt_detail` | p3.2 receipt detail | receipts |
| `receipt_pattern` | p3.4 spending pattern | receipts |
| `recipe_home` | p4.1 recipes (weekly recipes + saved recipes) | recipes |
| `recipe_create` | p4.1 quick recipe generation modal | recipes |
| `recipe_custom` | p4.1.2 custom recipe generation | recipes |
| `recipe_loading` | AI generation loading (screens to add no. 14) | recipes |
| `recipe_generate_error` | generation failure / quota used up screen (screens to add no. 8, 12) | recipes |
| `recipe_result` | p4.1.3 recipe generation result | recipes |
| `recipe_detail` | p4.1.1 recipe detail | recipes |
| `cook_deduct` | p4.1.1.2 ingredient deduction after cooking | recipes |
| `shared_write` | p4.1.1.1 write shared recipe | sharing |
| `recipe_ranking` | p4.1.4 this week's recipe ranking | sharing |
| `shared_detail` | p4.1.4.1 shared recipe detail | sharing |
| `shared_report` | p4.1.4.1 report sheet | sharing |
| `meal_plan_calendar` | p5 meal plan (calendar, bottom sheet, ⋮ sheet, empty state) | meal plan |
| `meal_plan_setup` | meal plan recipe generation | meal plan |
| `meal_plan_overlap_modal` | meal plan overlap warning modal (screens to add no. 10) | meal plan |
| `cart_list` | p7 cart | cart |

## Differences from the current code

- `EventName` now has the 79 events above (names, sources, and allowed properties match this document). `EventContractTest` reads the event table in this file and fails when the enum and the document differ, so change both together. Removed from the enum (not in the contract): `pwa_install_prompted`, `pwa_installed`, `push_permission_result`, `camera_unavailable` (use `error_shown` with `CAMERA_UNAVAILABLE`), `meal_plan_shortage_shown`, `meal_slot_detail_generated`, `meal_slot_detail_failed`, `meal_added`.
- `cook_feedback_answered` (E50) is no longer on hold (11/22): the frontend sends it from the fridge (p2) bottom sheet shown after the cook deduction. Tracking data only (success rate, later improvements); no API or table. Properties are `recipe_id` and `recommended`. Priority is (open, #53).
- The server-side call sites for E84 `subscription_started`, E85 `subscription_payment_failed`, E87 `subscription_canceled`, and E88 `subscription_ended` exist (subscription API, #270). E86 `subscription_renewed` has no call site yet; it comes with the auto-payment job (#168).
- Property **values** (#172): the validator and server senders use lowercase code values (`limit_feature`, `add_source`, `recipe_mode`, `meal_purpose`, `meal_type`, `date_relation`, and the CLIENT lists such as `scan_method`, `filter_type`, `detail_origin`, `view_from`, `rank_bucket`, `ranking_view`, `edit_field`). `storage` and `filter_value` stay uppercase as listed in this document. Existing uppercase rows were rewritten once by Flyway `V30__lowercase_event_property_values.sql`.
- The `Screen` enum matches the 41 screen codes above, plus `UNKNOWN` for unrecognized client strings. Scheduled jobs pass `null` for `screen` (not `unknown`).
- Properties removed from the contract and the code: `permission_result` (with the push event), `needs_check_count`, `feedback_value` (E50 now uses `recipe_id`, `recommended`), `slot_source`, `has_recipe`, `group_count`, `duplicate_suspected`, and `duration_sec` on `receipt_result_abandoned`.
- Priorities now follow the source document. Decision [#58](https://github.com/likelion-backendschool-9th/REMAKE/issues/58) moved sharing, cart, and reward events to P1; the source keeps them P1. Its admin parts (ADMIN gate, analytics APIs) are now the operator page a1-a3 and a6 (APIs 61-64, 11/26).
