-- 등록할 때 원본 값을 보존한다. 냉장고 재료나 영수증의 수명과 독립적이다.
CREATE TABLE manual_ingredient_expenses (
    ingredient_id bigint PRIMARY KEY,
    user_id       bigint NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id   bigint NOT NULL REFERENCES ingredient_categories (id),
    name          varchar(50) NOT NULL,
    storage       varchar(10) NOT NULL,
    amount        numeric(10,3) NOT NULL,
    unit          varchar(10) NOT NULL,
    price         bigint CHECK (price >= 0),
    created_at    timestamp NOT NULL
);

CREATE INDEX idx_manual_ingredient_expenses_user_date
    ON manual_ingredient_expenses (user_id, created_at);
