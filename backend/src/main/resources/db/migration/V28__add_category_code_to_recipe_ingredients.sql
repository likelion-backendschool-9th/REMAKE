-- 레시피 재료의 카테고리(#76). 장바구니 담기(07)가 사전에 없는 재료도 담을 수 있게 레시피를 만들 때 정해 둔다.
-- 정하는 순서: 사전 이름과 같으면 사전 카테고리 → 내 냉장고 재료로 만든 줄이면 그 재료의 카테고리 → 나머지는 AI가 고른 값.
-- 이 컬럼 전에 만든 레시피와 AI가 고르지 못한 줄은 비어 있다(장바구니가 사전으로 다시 찾고, 그래도 없으면 담지 않음).
ALTER TABLE recipe_ingredients ADD COLUMN category_code varchar(20);

ALTER TABLE recipe_ingredients ADD CONSTRAINT ck_recipe_ingredients_category_code CHECK (category_code IS NULL OR category_code IN (
    'VEGETABLE', 'FRUIT', 'MEAT', 'SEAFOOD', 'DAIRY_EGG', 'GRAIN', 'BEAN_TOFU', 'PROCESSED', 'SIDE_DISH',
    'SEASONING', 'BEVERAGE', 'ETC'));
