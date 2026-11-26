-- ingredient_logs.category_id를 NOT NULL로 만든다(#264). V25(#252)는 배포 중 구버전이 category_id 없이 기록을 쓸 수 있도록 NULL을 허용했다.
-- V25가 배포되어 새 버전만 도는 지금 남은 NULL(배포 중 구버전이 쓴 기록)을 채우고 NOT NULL을 건다.
-- 채우는 규칙은 V25와 같다: 재료가 남아 있으면 그 재료의 카테고리, 없으면 '기타'(ETC).
UPDATE ingredient_logs l
SET category_id = i.category_id
FROM ingredients i
WHERE l.category_id IS NULL AND l.ingredient_id = i.id;

UPDATE ingredient_logs
SET category_id = (SELECT id FROM ingredient_categories WHERE code = 'ETC')
WHERE category_id IS NULL;

ALTER TABLE ingredient_logs ALTER COLUMN category_id SET NOT NULL;
