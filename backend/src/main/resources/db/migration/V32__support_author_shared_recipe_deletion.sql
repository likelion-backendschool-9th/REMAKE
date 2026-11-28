-- 공유 행은 주별 공유 제한과 신고·확정 순위·보상 관계를 위해 보존한다.
ALTER TABLE shared_recipes ALTER COLUMN title DROP NOT NULL;
ALTER TABLE shared_recipes ALTER COLUMN image_key DROP NOT NULL;
ALTER TABLE shared_recipes DROP CONSTRAINT ck_shared_recipes_status;
ALTER TABLE shared_recipes DROP CONSTRAINT ck_shared_recipes_title;
ALTER TABLE shared_recipes ADD CONSTRAINT ck_shared_recipes_status
    CHECK (status IN ('VISIBLE', 'HIDDEN', 'DELETED'));
ALTER TABLE shared_recipes ADD CONSTRAINT ck_shared_recipes_content CHECK (
    (status = 'DELETED' AND title IS NULL AND image_key IS NULL)
    OR (status IN ('VISIBLE', 'HIDDEN') AND title IS NOT NULL AND length(trim(title)) > 0 AND image_key IS NOT NULL)
);
-- 삭제 요청과 같은 트랜잭션에서 키를 남기고, 커밋 후 파일 삭제 성공 때만 제거한다.
CREATE TABLE shared_recipe_photo_deletions (
    image_key varchar(255) PRIMARY KEY,
    requested_at timestamp NOT NULL
);
