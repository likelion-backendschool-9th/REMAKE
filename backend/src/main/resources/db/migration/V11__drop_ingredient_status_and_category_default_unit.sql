-- ERD v4에 맞춰 냉장고 스키마를 정리한다(#71). 머지된 V1·V2는 고치지 않고 새 번호로 바꾼다.
--  * ingredients: is_expiration_estimated(추정 표시 없음), status(수량 0이면 행 삭제), version(낙관적 락 근거 없음) 제거
--  * ingredient_categories: default_unit 제거(단위는 재료명으로 서버가 정함), sort_order 추가, SEASONING 이름 '양념'

-- 옛 FINISHED 행은 수량 0이라 새 규칙(0이면 행 삭제)에서는 있을 수 없다. 컬럼을 지우기 전에 같이 지운다.
-- 사용·폐기 기록(ingredient_logs), 레시피 재료, 알림은 재료 연결만 ON DELETE SET NULL로 비워진다.
DELETE FROM ingredients WHERE status = 'FINISHED';

DROP INDEX idx_ingredients_user_status_expiration;
DROP INDEX idx_ingredients_status_expiration;

ALTER TABLE ingredients
    DROP COLUMN is_expiration_estimated,
    DROP COLUMN status,
    DROP COLUMN version;

-- 사용자별 목록과 알림 배치(모든 사용자의 재료를 소비기한 범위로 조회)용
CREATE INDEX idx_ingredients_user_expiration ON ingredients (user_id, expiration_date);
CREATE INDEX idx_ingredients_expiration ON ingredients (expiration_date);

ALTER TABLE ingredient_categories DROP COLUMN default_unit;

-- 카테고리 시트 순서는 ERD 부록 A 순서
ALTER TABLE ingredient_categories ADD COLUMN sort_order int;
UPDATE ingredient_categories SET sort_order = CASE code
    WHEN 'VEGETABLE'  THEN 1
    WHEN 'MEAT'       THEN 2
    WHEN 'FRUIT'      THEN 3
    WHEN 'SEAFOOD'    THEN 4
    WHEN 'DAIRY_EGG'  THEN 5
    WHEN 'GRAIN'      THEN 6
    WHEN 'BEAN_TOFU'  THEN 7
    WHEN 'PROCESSED'  THEN 8
    WHEN 'SIDE_DISH'  THEN 9
    WHEN 'SEASONING'  THEN 10
    WHEN 'BEVERAGE'   THEN 11
    WHEN 'ETC'        THEN 12
END;
ALTER TABLE ingredient_categories ALTER COLUMN sort_order SET NOT NULL;
ALTER TABLE ingredient_categories ADD CONSTRAINT uk_ingredient_categories_sort_order UNIQUE (sort_order);

UPDATE ingredient_categories SET name = '양념' WHERE code = 'SEASONING';
