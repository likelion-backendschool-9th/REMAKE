-- #57 중복 담기 방지. 원본 레시피가 다른 재료는 별도 행이며 기존 수량·최초 슬롯 출처를 덮어쓰지 않는다.
-- recipe_id가 없는 과거 출처 행은 이 키의 대상이 아니다. 정상 신규 담기는 항상 원본 recipe_id를 받는다.
CREATE UNIQUE INDEX uk_cart_items_user_recipe_name_unit
    ON cart_items (user_id, recipe_id, name, unit)
    WHERE recipe_id IS NOT NULL;
