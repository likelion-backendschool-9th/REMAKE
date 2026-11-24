-- 저장 목록(API 33)과 레시피별 저장 여부 조회용 인덱스. 테이블은 V13(공유 #214)이 만들었다.
-- uk_saved_recipes_user_recipe(user_id, recipe_id)는 V13에 있다.

-- 저장 목록(최신·오래된 저장순)
CREATE INDEX idx_saved_recipes_user_saved_at ON saved_recipes (user_id, saved_at);
-- recipe_id 외래 키 조회(레시피별 저장 여부)
CREATE INDEX idx_saved_recipes_recipe ON saved_recipes (recipe_id);
