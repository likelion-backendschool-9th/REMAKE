-- 사용·폐기 기록에 기록할 때의 재료 카테고리를 남긴다(#252). 이름·단위처럼 기록 시점의 값이라 재료를 지우거나 카테고리를 고쳐도 바뀌지 않는다.
-- 다 쓰거나 버려서 재료 행이 지워진 기록(ingredient_id NULL)도 카테고리를 알 수 있어야 버린 식재료 조회(#248)가 가능하다.
-- NOT NULL은 이 단계에서 걸지 않는다. 배포 중 구버전과 새 버전이 같은 DB를 쓰는 구간과 롤백 뒤에도 구버전이
-- category_id 없이 기록을 쓸 수 있어야 하기 때문이다(deployment.md). 새 코드는 항상 채운다.
-- 이 변경이 배포된 뒤 후속 마이그레이션(#264)에서 남은 NULL을 다시 채우고 NOT NULL을 건다.
ALTER TABLE ingredient_logs ADD COLUMN category_id bigint;

-- 기존 기록: 재료가 남아 있으면 그 재료의 카테고리, 이미 지워졌으면 '기타'(ETC)
UPDATE ingredient_logs l
SET category_id = i.category_id
FROM ingredients i
WHERE l.ingredient_id = i.id;

UPDATE ingredient_logs
SET category_id = (SELECT id FROM ingredient_categories WHERE code = 'ETC')
WHERE category_id IS NULL;

ALTER TABLE ingredient_logs
    ADD CONSTRAINT fk_ingredient_logs_category FOREIGN KEY (category_id) REFERENCES ingredient_categories (id);
