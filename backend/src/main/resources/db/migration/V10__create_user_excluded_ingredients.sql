-- 제외 재료(알레르기). ERD v4 2번 user_excluded_ingredients.
-- 칩 19개 코드(allergen_code)를 저장한다. 온보딩·설정에서 PUT으로 통째 교체한다.

CREATE TABLE user_excluded_ingredients (
    user_id        bigint      NOT NULL,
    allergen_code  varchar(30) NOT NULL,
    PRIMARY KEY (user_id, allergen_code),
    CONSTRAINT fk_user_excluded_ingredients_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_user_excluded_ingredients_allergen_code CHECK (allergen_code IN (
        'EGG', 'MILK', 'BUCKWHEAT', 'PEANUT', 'SOYBEAN', 'WHEAT', 'MACKEREL', 'CRUSTACEAN',
        'PORK', 'SHRIMP', 'SULFITE', 'PEACH', 'SQUID', 'CHICKEN', 'BEEF', 'OYSTER', 'CRAB',
        'PINE_NUT', 'SHELLFISH'
    ))
);

CREATE INDEX idx_user_excluded_ingredients_allergen_code
    ON user_excluded_ingredients (allergen_code);
