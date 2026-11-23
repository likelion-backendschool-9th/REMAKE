import { cx } from "@/lib/cx";

import styles from "./IngredientChip.module.css";

type IngredientChipProps = {
  name: string;
  /** 이름 옆 파란 글자. 예: "1종", "200g" */
  amount: string;
  /** 피그마 보유=없음: 내 냉장고에 없는 재료(빨간 테두리) */
  isMissing?: boolean;
  className?: string;
};

/** 피그마 디자인 시스템 > 5. 칩·배지 > 재료 칩. 레시피 재료 준비 목록. */
export function IngredientChip({
  name,
  amount,
  isMissing = false,
  className,
}: IngredientChipProps) {
  return (
    <span className={cx(styles.chip, className)} data-missing={isMissing || undefined}>
      <span className={styles.name}>{name}</span>
      <span className={styles.amount}>{amount}</span>
      {/* 색만으로 상태를 전하지 않는다(blueprint §6.2) */}
      {isMissing && <span className={styles.srOnly}>(냉장고에 없음)</span>}
    </span>
  );
}
