-- #172: 이벤트 속성 코드 값을 소문자로 맞춘다.
-- storage·category_code·error_code·filter_value(상태·보관 위치)는 정의서 대문자라 바꾸지 않는다.
-- 서버가 쓰던 옛 화면 코드도 정의서 41개에 맞게 옮긴다.

UPDATE events
SET properties = jsonb_set(properties, '{add_source}', to_jsonb(lower(properties->>'add_source')))
WHERE properties ? 'add_source'
  AND properties->>'add_source' <> lower(properties->>'add_source');

UPDATE events
SET properties = jsonb_set(properties, '{limit_feature}', to_jsonb(lower(properties->>'limit_feature')))
WHERE properties ? 'limit_feature'
  AND properties->>'limit_feature' <> lower(properties->>'limit_feature');

UPDATE events
SET properties = jsonb_set(properties, '{recipe_mode}', to_jsonb(lower(properties->>'recipe_mode')))
WHERE properties ? 'recipe_mode'
  AND properties->>'recipe_mode' <> lower(properties->>'recipe_mode');

UPDATE events
SET properties = jsonb_set(properties, '{meal_purpose}', to_jsonb(lower(properties->>'meal_purpose')))
WHERE properties ? 'meal_purpose'
  AND properties->>'meal_purpose' <> lower(properties->>'meal_purpose');

UPDATE events
SET properties = jsonb_set(properties, '{meal_type}', to_jsonb(lower(properties->>'meal_type')))
WHERE properties ? 'meal_type'
  AND properties->>'meal_type' <> lower(properties->>'meal_type');

UPDATE events
SET properties = jsonb_set(properties, '{date_relation}', to_jsonb(lower(properties->>'date_relation')))
WHERE properties ? 'date_relation'
  AND properties->>'date_relation' <> lower(properties->>'date_relation');

UPDATE events SET screen = 'setting_main' WHERE screen = 'settings';
UPDATE events SET screen = 'onboarding_allergy' WHERE screen = 'onboarding';
UPDATE events SET screen = 'meal_plan_calendar' WHERE screen = 'meal_day';
UPDATE events SET screen = 'shared_detail' WHERE screen = 'shared_recipe';
UPDATE events SET screen = 'recipe_ranking' WHERE screen = 'ranking';
UPDATE events SET screen = 'receipt_pattern' WHERE screen = 'consumption_pattern';
UPDATE events SET screen = 'cart_list' WHERE screen = 'cart';
UPDATE events SET screen = NULL WHERE screen = 'unknown';
UPDATE events SET screen = NULL WHERE screen = 'admin';
