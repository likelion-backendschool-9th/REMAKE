# Event tracking contract

Source of truth: the event data definition sheet (Korean, Google Sheets; not in the repo; 76 events: E01–E75 and E76 `receipt_result_abandoned`, added later). Names are snake_case and fixed; to add or rename an event update the sheet first.

## Rules

- One user action is recorded once, as CLIENT or SERVER (column Source). Frontend sends CLIENT events to `POST /api/v1/user/events` (batch <= 50, 202). Backend emits SERVER events via `EventTracker` at the end of the owning API/batch.
- Common fields on every event: `user_id` (internal number, null before login/after deletion), `session_id` (new after 30 min idle), `source`, `screen`, `platform` (`PWA`/`TWA`/`WEB`, header `X-Client-Platform`), `app_version` (`X-App-Version`), `is_seed` (demo data), `occurred_at` (KST).
- `properties`: flat JSON, string/number/boolean only, <= 25 keys.
- **Never record:** email, name, social ID, allergy/excluded ingredient names, typed ingredient names, recipe content, any free text. Record `category_code`, counts and enums instead.
- Tracking must never break the user flow (async, swallow errors).
- Decision [#46](https://github.com/likelion-backendschool-9th/REMAKE/issues/46): `cart_item_added` (E62) is emitted only by `AddCartItemsUseCase`, once per call that inserts rows. Record `cart_source` (`MEAL_PLAN`, `RECIPE_SAVE`, `SHARED_RECIPE_SAVE`) and `item_count` (total inserted rows, not groups or summed quantities). Skip empty lists and groups without items; emit nothing if the inserted count is 0. The calling meal-plan and recipe APIs do not emit it themselves. Never record a rolled-back add as a successful event.
- Automatic cart cleanup through `RemoveCartItemsByMealPlanSlotsUseCase` during meal-plan overwrite emits no user-deletion event (`cart_items_deleted`).
- Raw events are kept 180 days; daily aggregation in `event_daily_stats`.

## Events

| ID | Name | Source | Priority | Properties | Publisher API |
|---|---|---|---|---|---|
| E01 | `app_opened` | CLIENT | P0 | entry_source, is_standalone | POST /user/events (sent by frontend) |
| E02 | `screen_viewed` | CLIENT | P0 | screen_name, previous_screen | POST /user/events (sent by frontend) |
| E03 | `error_shown` | CLIENT | P0 | error_code, http_status, screen_name | POST /user/events (sent by frontend) |
| E04 | `pwa_install_prompted` | CLIENT | P1 | (common only) | POST /user/events (sent by frontend) |
| E05 | `pwa_installed` | CLIENT | P1 | (common only) | POST /user/events (sent by frontend) |
| E06 | `push_permission_result` | CLIENT | P1 | permission_result | POST /user/events (sent by frontend) |
| E07 | `quota_exceeded` | SERVER | P0 | limit_feature, limit_total, used_count | POST /user/recipes/generate / POST /user/meal-plans/generate |
| E08 | `login_started` | CLIENT | P0 | provider, is_recent_provider | POST /user/events (sent by frontend) |
| E09 | `login_succeeded` | SERVER | P0 | provider, is_new_user | POST /public/auth/oauth/{provider} |
| E10 | `login_failed` | SERVER | P0 | provider, error_code | POST /public/auth/oauth/{provider} |
| E11 | `signup_completed` | SERVER | P0 | provider | POST /public/auth/oauth/{provider} |
| E12 | `onboarding_step_completed` | CLIENT | P0 | onboarding_step, was_skipped | POST /user/events (sent by frontend) |
| E13 | `onboarding_completed` | SERVER | P0 | skipped_step_count, excluded_count | PUT /user/profile |
| E14 | `setting_changed` | CLIENT | P1 | setting_key, new_value_bool | POST /user/events (sent by frontend) |
| E15 | `excluded_ingredients_updated` | CLIENT | P1 | excluded_count | POST /user/events (sent by frontend) |
| E16 | `logout_confirmed` | CLIENT | P1 | (common only) | POST /user/events (sent by frontend) |
| E17 | `account_deletion_requested` | SERVER | P1 | is_resend | POST /user/account/deletion-codes |
| E18 | `account_deleted` | SERVER | P1 | tenure_days | POST /user/account/deletion-codes/verify |
| E19 | `fridge_list_viewed` | CLIENT | P0 | total_count, danger_count, soon_count, ok_count | POST /user/events (sent by frontend) |
| E20 | `fridge_filter_applied` | CLIENT | P1 | filter_type, filter_value | POST /user/events (sent by frontend) |
| E21 | `ingredient_add_started` | CLIENT | P0 | add_source | POST /user/events (sent by frontend) |
| E22 | `ingredient_added` | SERVER | P0 | add_source, category_code, storage, has_price, is_expiration_estimated, duration_sec | POST /user/ingredients / POST /user/receipts |
| E23 | `ingredient_edited` | CLIENT | P1 | edit_field | POST /user/events (sent by frontend) |
| E24 | `ingredient_deleted` | CLIENT | P2 | category_code | POST /user/events (sent by frontend) |
| E25 | `ingredient_used` | SERVER | P0 | use_via, category_code, expiry_status, used_ratio | POST /user/ingredients/{id}/consume / POST /user/recipes/{id}/cook |
| E26 | `ingredient_discarded` | SERVER | P0 | category_code, expiry_status, used_ratio, has_price, cost_amount | POST /user/ingredients/{id}/consume |
| E27 | `ingredient_expired` | SERVER | P0 | category_code, has_price | 서버 배치 소비기한 알림(WBS 6.1) |
| E28 | `receipt_scan_started` | CLIENT | P0 | scan_method | POST /user/events (sent by frontend) |
| E29 | `camera_unavailable` | CLIENT | P1 | (common only) | POST /user/events (sent by frontend) |
| E30 | `receipt_scan_succeeded` | SERVER | P0 | item_count, food_item_count, duration_ms, duplicate_suspected | POST /user/receipts/scan |
| E31 | `receipt_scan_failed` | SERVER | P0 | error_code, duration_ms | POST /user/receipts/scan |
| E32 | `receipt_item_edited` | CLIENT | P0 | item_index, edit_field | POST /user/events (sent by frontend) |
| E33 | `receipt_item_added_manually` | CLIENT | P1 | (common only) | POST /user/events (sent by frontend) |
| E34 | `receipt_saved` | SERVER | P0 | receipt_id, registered_count, ocr_item_count, edited_item_count, deleted_item_count, added_item_count, duration_sec, food_total_amount | POST /user/receipts |
| E76 | `receipt_result_abandoned` | CLIENT | P1 | ocr_item_count, edited_item_count, deleted_item_count, added_item_count, duration_sec | POST /user/events (sent by frontend) |
| E35 | `receipt_list_viewed` | CLIENT | P1 | month_offset, has_comparison | POST /user/events (sent by frontend) |
| E36 | `receipt_detail_viewed` | CLIENT | P2 | (common only) | POST /user/events (sent by frontend) |
| E37 | `receipt_deleted` | CLIENT | P1 | (common only) | POST /user/events (sent by frontend) |
| E38 | `consumption_pattern_viewed` | CLIENT | P0 | month_offset | POST /user/events (sent by frontend) |
| E39 | `recipe_generate_started` | CLIENT | P0 | recipe_mode, servings, option_count, supplement_enabled | POST /user/events (sent by frontend) |
| E40 | `recipe_generated` | SERVER | P0 | recipe_id, recipe_mode, duration_ms, expiring_used_count, shortage_count, allergen_warning, remaining_quota | POST /user/recipes/generate |
| E41 | `recipe_generate_failed` | SERVER | P0 | recipe_mode, error_code, duration_ms | POST /user/recipes/generate |
| E42 | `recipe_regenerated` | CLIENT | P1 | recipe_mode | POST /user/events (sent by frontend) |
| E43 | `recipe_detail_viewed` | CLIENT | P0 | recipe_id, detail_origin | POST /user/events (sent by frontend) |
| E44 | `recipe_saved` | SERVER | P0 | recipe_id, save_source, cart_added_count | PUT /user/saved-recipes/{recipeId} / PUT /user/shared-recipes/{id}/save |
| E45 | `recipe_unsaved` | CLIENT | P1 | save_source | POST /user/events (sent by frontend) |
| E46 | `recipe_saved_list_viewed` | CLIENT | P1 | saved_count | POST /user/events (sent by frontend) |
| E47 | `step_play_started` | CLIENT | P2 | recipe_id | POST /user/events (sent by frontend) |
| E48 | `cook_deduct_viewed` | CLIENT | P1 | recipe_id, deduct_item_count, needs_check_count | POST /user/events (sent by frontend) |
| E49 | `cook_completed` | SERVER | P0 | recipe_id, deducted_count, adjusted_count, servings | POST /user/recipes/{id}/cook |
| E50 | `cook_feedback_answered` | CLIENT | P1 | feedback_value | POST /user/events (sent by frontend) |
| E51 | `meal_plan_setup_viewed` | CLIENT | P0 | remaining_quota | POST /user/events (sent by frontend) |
| E52 | `meal_plan_generate_started` | CLIENT | P0 | meal_purpose, meal_type_count, servings, start_offset_days | POST /user/events (sent by frontend) |
| E53 | `meal_plan_generated` | SERVER | P0 | meal_plan_id, meal_purpose, meal_type_count, generated_days, shortage_count, duration_ms, remaining_quota | POST /user/meal-plans/generate |
| E54 | `meal_plan_generate_failed` | SERVER | P0 | error_code, duration_ms | POST /user/meal-plans/generate |
| E55 | `meal_plan_shortage_shown` | CLIENT | P0 | shortage_count, generated_days | POST /user/events (sent by frontend) |
| E56 | `meal_calendar_viewed` | CLIENT | P1 | month_offset, has_plan | POST /user/events (sent by frontend) |
| E57 | `meal_day_viewed` | CLIENT | P1 | date_relation | POST /user/events (sent by frontend) |
| E58 | `meal_slot_opened` | CLIENT | P0 | meal_type, slot_source, has_recipe | POST /user/events (sent by frontend) |
| E59 | `meal_slot_detail_generated` | SERVER | P1 | recipe_id, duration_ms | POST /user/meal-plan-slots/{slotId}/detail |
| E60 | `meal_slot_detail_failed` | SERVER | P1 | error_code, duration_ms | POST /user/meal-plan-slots/{slotId}/detail |
| E61 | `meal_added` | SERVER | P0 | meal_type, date_relation, recipe_id | POST /user/meal-plan-slots |
| E62 | `cart_item_added` | SERVER | P0 | cart_source, item_count | POST /user/meal-plans/generate / PUT /user/saved-recipes/{recipeId} / PUT /user/shared-recipes/{id}/save |
| E63 | `cart_viewed` | CLIENT | P1 | group_count, item_count | POST /user/events (sent by frontend) |
| E64 | `cart_amount_changed` | CLIENT | P2 | (common only) | POST /user/events (sent by frontend) |
| E65 | `cart_items_deleted` | CLIENT | P1 | deleted_count, was_select_all | POST /user/events (sent by frontend) |
| E66 | `notification_sent` | SERVER | P0 | notification_type, channel, batch_count | 서버 배치 알림 생성(WBS 6.1) |
| E67 | `notification_clicked` | CLIENT | P0 | notification_type, click_from | POST /user/events (sent by frontend) |
| E68 | `notification_list_viewed` | CLIENT | P1 | unread_count | POST /user/events (sent by frontend) |
| E69 | `ranking_viewed` | CLIENT | P1 | ranking_view, query_used | POST /user/events (sent by frontend) |
| E70 | `shared_recipe_viewed` | CLIENT | P0 | shared_recipe_id, view_from, rank_bucket | POST /user/events (sent by frontend) |
| E71 | `shared_write_started` | CLIENT | P0 | recipe_id | POST /user/events (sent by frontend) |
| E72 | `shared_recipe_published` | SERVER | P0 | shared_recipe_id, photo_count | POST /user/shared-recipes |
| E73 | `shared_recipe_saved` | SERVER | P0 | shared_recipe_id, cart_added_count | PUT /user/shared-recipes/{id}/save |
| E74 | `shared_recipe_reported` | SERVER | P0 | shared_recipe_id, report_reason | POST /user/shared-recipes/{id}/reports |
| E75 | `weekly_reward_granted` | SERVER | P1 | reward_rank, reward_amount | 서버 배치 금주의 레시피(WBS 7.3) |
