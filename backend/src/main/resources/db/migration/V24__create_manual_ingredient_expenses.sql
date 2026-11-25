-- 등록할 때 원본 값을 보존한다. 냉장고 재료나 영수증의 수명과 독립적이다.
CREATE TABLE manual_ingredient_expenses (
    ingredient_id bigint PRIMARY KEY,
    user_id       bigint NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id   bigint NOT NULL REFERENCES ingredient_categories (id),
    name          varchar(50) NOT NULL,
    storage       varchar(10) NOT NULL,
    amount        numeric(10,3) NOT NULL,
    unit          varchar(10) NOT NULL,
    price         bigint NOT NULL CHECK (price >= 0),
    created_at    timestamp NOT NULL
);

CREATE INDEX idx_manual_ingredient_expenses_user_date
    ON manual_ingredient_expenses (user_id, created_at);

-- 배포 중 구버전에서 발생한 등록도 포함한다. 기존 행의 NULL 영수증 참조는 출처를 증명하지 못한다.
CREATE FUNCTION record_manual_ingredient_expense() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO manual_ingredient_expenses
        (ingredient_id, user_id, category_id, name, storage, amount, unit, price, created_at)
    VALUES (NEW.id, NEW.user_id, NEW.category_id, NEW.name, NEW.storage,
            NEW.initial_amount, NEW.unit, NEW.price, NEW.created_at);
    RETURN NEW;
END;
$$;

CREATE TRIGGER record_manual_ingredient_expense_after_insert
AFTER INSERT ON ingredients
FOR EACH ROW
WHEN (NEW.receipt_item_id IS NULL AND NEW.price IS NOT NULL)
EXECUTE FUNCTION record_manual_ingredient_expense();
