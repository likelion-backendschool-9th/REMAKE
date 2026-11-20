# Data model (32 tables)

Source: table definition and ERD (Korean, kept in Notion / Google Sheets; not in the repo). PostgreSQL. Column descriptions are in the Korean source; this file lists structure only. `PK`, `FK->table`, `UK`, `?` = nullable. `UK(a, b)` is a composite unique key over those columns. Full column details (defaults, nullability, reasons) are in [`../erd.md`](../erd.md).

Common base classes: `BaseTimeEntity` (created_at, updated_at), `BaseCreatedEntity` (created_at). Feature folder = backend package.

## 1. `users` — user package

Social-login member; also holds settings (household size, notifications, recipe supplement) and plan (BASIC/PREMIUM). Admins are users with role ADMIN.

Columns: `id` bigint (PK); `provider` varchar; `provider_user_id` varchar (UK, ?); `email` varchar (?); `role` varchar; `status` varchar; `plan` varchar; `household_size` int (?); `onboarding_completed` boolean; `recipe_supplement_enabled` boolean; `notification_enabled` boolean; `last_meal_purpose` varchar (?); `created_at` timestamp; `updated_at` timestamp; `deleted_at` timestamp (?)

## 2. `user_excluded_ingredients` — user package

Allergy/excluded ingredients chosen by a user (N:N to ingredient_catalog).

Columns: `user_id` bigint (PK,FK->users); `catalog_id` bigint (PK,FK->ingredient_catalog)

## 3. `refresh_tokens` — auth package

Refresh tokens (stored hashed); revoked on logout.

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `token_hash` varchar (UK); `expires_at` timestamp; `revoked_at` timestamp (?); `created_at` timestamp

## 4. `account_deletion_requests` — user package

6-digit email code for account deletion (5 min, resend, locks after 5 failures).

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `code_hash` varchar; `expires_at` timestamp; `attempts` int; `verified_at` timestamp (?); `created_at` timestamp

## 5. `ingredient_categories` — ingredient package

12 ingredient categories with defaults (storage, unit, shelf days). Reference data.

Columns: `id` bigint (PK); `code` varchar (UK); `name` varchar; `image_url` varchar; `default_storage` varchar; `default_unit` varchar; `default_shelf_life_days` int

## 6. `ingredient_catalog` — ingredient package

Ingredient dictionary for autocomplete, category guess and exclusion list. Reference data.

Columns: `id` bigint (PK); `category_id` bigint (FK->ingredient_categories); `name` varchar (UK); `shelf_life_days` int (?); `is_common_exclusion` boolean

## 7. `ingredients` — ingredient package

One fridge item. Becomes FINISHED when used up; has `version` for optimistic locking.

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `category_id` bigint (FK->ingredient_categories); `receipt_item_id` bigint (FK,UK->receipt_items, ?); `name` varchar; `storage` varchar; `expiration_date` date; `is_expiration_estimated` boolean; `amount` numeric(10,3); `initial_amount` numeric(10,3); `unit` varchar; `price` bigint (?); `status` varchar; `version` bigint; `created_at` timestamp; `updated_at` timestamp

## 8. `ingredient_logs` — ingredient package

Used/discarded history; source for discarded-amount stats.

Columns: `id` bigint (PK); `ingredient_id` bigint (FK->ingredients); `cook_log_id` bigint (FK->recipe_cook_logs, ?); `type` varchar; `amount` numeric(10,3); `unit` varchar; `cost_amount` bigint (?); `created_at` timestamp

## 9. `receipts` — receipt package

A confirmed receipt (unconfirmed OCR is never stored). 6-month retention.

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `purchased_on` date (?); `store_name` varchar (?); `store_code` varchar (?); `image_key` varchar; `created_at` timestamp

## 10. `receipt_items` — receipt package

Food items of a receipt (non-food filtered out by OCR).

Columns: `id` bigint (PK); `receipt_id` bigint (FK->receipts); `category_id` bigint (FK->ingredient_categories); `name` varchar; `amount` numeric(10,3); `unit` varchar; `price` bigint (?); `storage` varchar; `expiration_date` date (?); `is_expiration_estimated` boolean; `created_at` timestamp

## 11. `recipes` — recipe package

AI recipe body, and the copy made when a recipe is shared or saved from sharing. Never edited after creation.

Columns: `id` bigint (PK); `creator_id` bigint (FK->users); `title` varchar; `servings` int; `cook_minutes` int; `tips` varchar (?); `origin` varchar; `mode` varchar (?); `custom_options` jsonb (?); `supplement_used` boolean; `created_at` timestamp

## 12. `recipe_ingredients` — recipe package

Ingredients and amounts of a recipe.

Columns: `id` bigint (PK); `recipe_id` bigint (FK->recipes); `ingredient_id` bigint (FK->ingredients, ?); `name` varchar; `amount` numeric(10,3) (?); `unit` varchar; `sort_order` int; `created_at` timestamp

## 13. `recipe_steps` — recipe package

Cooking steps.

Columns: `id` bigint (PK); `recipe_id` bigint (FK->recipes); `step_no` int; `content` varchar; `created_at` timestamp

## 14. `recipe_catalog_items` — recipe package

Excluded/allergen catalog items contained in a recipe (for allergen warnings).

Columns: `recipe_id` bigint (PK,FK->recipes); `catalog_id` bigint (PK,FK->ingredient_catalog)

## 15. `saved_recipes` — recipe package

User saved list; is_favorite star; records source shared recipe (save-count source).

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `recipe_id` bigint (FK->recipes); `shared_recipe_id` bigint (FK->shared_recipes, ?); `is_favorite` boolean; `saved_at` timestamp

## 16. `recipe_cook_logs` — recipe package

Record of finishing a recipe and deducting ingredients.

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `recipe_id` bigint (FK->recipes); `servings` int; `feedback` varchar (?); `cooked_at` timestamp

## 17. `meal_plans` — mealplan package

One generated meal plan (from start date, up to 7 days; basic users max 5).

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `start_date` date; `purpose` varchar; `meal_types` varchar; `servings` int; `generated_days` int; `created_at` timestamp

## 18. `meal_plan_slots` — mealplan package

Date/meal slots; AI-generated or MANUAL (user added).

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `meal_plan_id` bigint (FK->meal_plans, ?); `slot_date` date; `meal_type` varchar (UK(user_id, slot_date, meal_type)); `menu_name` varchar; `recipe_id` bigint (FK->recipes, ?); `source` varchar; `created_at` timestamp; `updated_at` timestamp

## 19. `meal_plan_slot_ingredients` — mealplan package

Ingredients planned per slot; used to avoid double allocation and compute shortage.

Columns: `id` bigint (PK); `slot_id` bigint (FK->meal_plan_slots); `ingredient_id` bigint (FK->ingredients, ?); `name` varchar; `amount` numeric(10,3); `unit` varchar; `created_at` timestamp

## 20. `cart_items` — cart package

Cart rows per recipe; added automatically only.

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `recipe_id` bigint (FK->recipes, ?); `meal_plan_id` bigint (FK->meal_plans, ?); `group_name` varchar; `name` varchar; `amount` numeric(10,3); `unit` varchar; `created_at` timestamp; `updated_at` timestamp

## 21. `shared_recipes` — share package

Recipe published to the community (fully anonymous).

Columns: `id` bigint (PK); `recipe_id` bigint (FK,UK->recipes); `title` varchar; `status` varchar; `save_count` int; `created_at` timestamp; `updated_at` timestamp

## 22. `shared_recipe_photos` — share package

Cooking photos for a shared recipe (>= 1).

Columns: `id` bigint (PK); `shared_recipe_id` bigint (FK->shared_recipes); `image_key` varchar; `sort_order` int; `created_at` timestamp

## 23. `shared_recipe_reports` — share package

Reports and admin handling result.

Columns: `id` bigint (PK); `shared_recipe_id` bigint (FK->shared_recipes); `reporter_id` bigint (FK->users; UK(shared_recipe_id, reporter_id)); `reason` varchar; `reason_detail` varchar (?); `status` varchar; `result` varchar (?); `handled_by` bigint (FK->users, ?); `handled_at` timestamp (?); `created_at` timestamp

## 24. `shared_recipe_weekly_ranks` — share package

Last week TOP 100 saved by the weekly batch; used for rankChange.

Columns: `id` bigint (PK); `week_start` date; `shared_recipe_id` bigint (FK->shared_recipes); `rank_no` int (UK(week_start, rank_no)); `save_count` int; `created_at` timestamp

## 25. `generation_quota_grants` — quota package

Extra recipe generations (weekly reward, admin adjustment); valid one month.

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `amount` int; `remaining` int; `expires_at` timestamp; `reason` varchar; `week_start` date (?); `rank_no` int (?; UK(week_start, rank_no)); `shared_recipe_id` bigint (FK->shared_recipes, ?); `admin_id` bigint (FK->users, ?); `memo` varchar (?); `created_at` timestamp

## 26. `usage_logs` — quota package

AI/OCR call log with status; source for remaining-quota calculation and admin cost view.

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `type` varchar; `status` varchar; `error_code` varchar (?); `duration_ms` int (?); `grant_id` bigint (FK->generation_quota_grants, ?); `created_at` timestamp

## 27. `notifications` — notification package

Notification rows (EXPIRY_SOON, EXPIRED).

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `ingredient_id` bigint (FK->ingredients, ?); `type` varchar; `dedupe_key` varchar (UK(user_id, dedupe_key)); `title` varchar; `body` varchar; `read_at` timestamp (?); `created_at` timestamp

## 28. `push_subscriptions` — notification package

Web push subscription per device.

Columns: `id` bigint (PK); `user_id` bigint (FK->users); `endpoint` varchar (UK); `p256dh_key` varchar; `auth_key` varchar; `created_at` timestamp

## 29. `events` — event package

Raw user-behavior events (client and server). No FK on user_id.

Columns: `id` bigint (PK); `event_name` varchar; `user_id` bigint (?); `session_id` varchar (?); `source` varchar; `screen` varchar (?); `properties` jsonb (?); `platform` varchar (?); `app_version` varchar (?); `is_seed` boolean; `occurred_at` timestamp; `created_at` timestamp

## 30. `event_daily_stats` — event package

Daily aggregation of events for the dashboard.

Columns: `stat_date` date (PK); `event_name` varchar (PK); `is_seed` boolean (PK); `event_count` bigint; `user_count` int

## 31. `admin_audit_logs` — admin package

Admin actions (report handling, quota adjustment, account restriction).

Columns: `id` bigint (PK); `admin_id` bigint (FK->users); `action` varchar; `target_type` varchar; `target_id` bigint; `detail` jsonb (?); `created_at` timestamp

## 32. `idempotency_keys` — global package

Idempotency-Key and first response (24 h).

Columns: `user_id` bigint (PK,FK->users); `idempotency_key` varchar (PK); `request_path` varchar; `response_status` int; `response_body` jsonb (?); `created_at` timestamp

## Relationships (parent -> child, on delete)

- `users` -> `user_excluded_ingredients` (1:N) via `user_id`
- `ingredient_catalog` -> `user_excluded_ingredients` (1:N) via `catalog_id`
- `users` -> `refresh_tokens` (1:N) via `user_id`
- `users` -> `account_deletion_requests` (1:N) via `user_id`
- `ingredient_categories` -> `ingredient_catalog` (1:N) via `category_id`
- `users` -> `ingredients` (1:N) via `user_id`
- `ingredient_categories` -> `ingredients` (1:N) via `category_id`
- `receipt_items` -> `ingredients` (1:0..1) via `receipt_item_id` — on delete: SET NULL
- `ingredients` -> `ingredient_logs` (1:N) via `ingredient_id` — on delete: RESTRICT
- `recipe_cook_logs` -> `ingredient_logs` (1:N) via `cook_log_id`
- `users` -> `receipts` (1:N) via `user_id`
- `receipts` -> `receipt_items` (1:N) via `receipt_id` — on delete: CASCADE
- `ingredient_categories` -> `receipt_items` (1:N) via `category_id`
- `users` -> `recipes` (1:N) via `creator_id`
- `recipes` -> `recipe_ingredients` (1:N) via `recipe_id`
- `ingredients` -> `recipe_ingredients` (1:N) via `ingredient_id` — on delete: SET NULL
- `recipes` -> `recipe_steps` (1:N) via `recipe_id`
- `recipes` -> `recipe_catalog_items` (1:N) via `recipe_id`
- `ingredient_catalog` -> `recipe_catalog_items` (1:N) via `catalog_id`
- `users` -> `saved_recipes` (1:N) via `user_id`
- `recipes` -> `saved_recipes` (1:N) via `recipe_id`
- `shared_recipes` -> `saved_recipes` (1:N) via `shared_recipe_id` — on delete: SET NULL
- `users` -> `recipe_cook_logs` (1:N) via `user_id`
- `recipes` -> `recipe_cook_logs` (1:N) via `recipe_id`
- `users` -> `meal_plans` (1:N) via `user_id`
- `users` -> `meal_plan_slots` (1:N) via `user_id`
- `meal_plans` -> `meal_plan_slots` (1:N) via `meal_plan_id`
- `recipes` -> `meal_plan_slots` (1:N) via `recipe_id`
- `meal_plan_slots` -> `meal_plan_slot_ingredients` (1:N) via `slot_id` — on delete: CASCADE
- `ingredients` -> `meal_plan_slot_ingredients` (1:N) via `ingredient_id` — on delete: SET NULL
- `users` -> `cart_items` (1:N) via `user_id`
- `recipes` -> `cart_items` (1:N) via `recipe_id` — on delete: SET NULL
- `meal_plans` -> `cart_items` (1:N) via `meal_plan_id` — on delete: SET NULL
- `recipes` -> `shared_recipes` (1:0..1) via `recipe_id`
- `shared_recipes` -> `shared_recipe_photos` (1:N) via `shared_recipe_id` — on delete: CASCADE
- `shared_recipes` -> `shared_recipe_reports` (1:N) via `shared_recipe_id`
- `shared_recipes` -> `shared_recipe_weekly_ranks` (1:N) via `shared_recipe_id`
- `users` -> `shared_recipe_reports` (1:N) via `reporter_id`
- `users` -> `shared_recipe_reports` (1:N) via `handled_by`
- `users` -> `generation_quota_grants` (1:N) via `user_id`
- `users` -> `generation_quota_grants` (1:N) via `admin_id`
- `shared_recipes` -> `generation_quota_grants` (1:N) via `shared_recipe_id`
- `users` -> `usage_logs` (1:N) via `user_id`
- `generation_quota_grants` -> `usage_logs` (1:N) via `grant_id`
- `users` -> `notifications` (1:N) via `user_id`
- `ingredients` -> `notifications` (1:N) via `ingredient_id` — on delete: SET NULL
- `users` -> `push_subscriptions` (1:N) via `user_id`
- `users` -> `admin_audit_logs` (1:N) via `admin_id`
- `users` -> `idempotency_keys` (1:N) via `user_id`

Key constraints: UK(provider, provider_user_id) on users; refresh_tokens.token_hash UK; composite UKs `recipe_steps(recipe_id, step_no)`, `saved_recipes(user_id, recipe_id)` and `(user_id, shared_recipe_id)`, `meal_plan_slots(user_id, slot_date, meal_type)`, `shared_recipe_reports(shared_recipe_id, reporter_id)`, `shared_recipe_weekly_ranks(week_start, rank_no)`, `generation_quota_grants(week_start, rank_no)`, `notifications(user_id, dedupe_key)`; deleting an ingredient with logs is blocked (RESTRICT); index `receipts(user_id, purchased_on)` for expense queries.
