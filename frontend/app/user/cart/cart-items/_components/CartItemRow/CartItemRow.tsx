import { useId } from "react";
import Image from "next/image";

import { Checkbox } from "@/components/Checkbox/Checkbox";
import { SmallButton } from "@/components/SmallButton/SmallButton";
import { formatUnitAmount, getCategoryImage } from "@/lib/ingredients";

import type { CartItem } from "../../_lib/cart";

import styles from "./CartItemRow.module.css";

type CartItemRowProps = {
  item: CartItem;
  isChecked: boolean;
  /** 삭제 모드면 체크가 빨강 */
  isDeleteMode: boolean;
  onToggle: () => void;
  onChangeAmount: () => void;
};

/**
 * 피그마 p7 장바구니 리스트 한 줄: 체크박스 + 그림 44×29 + 레시피 이름(Remake/assist) + 재료·수량(Remake/content) + "수량 변경".
 * 체크박스·그림·글을 눌러도 선택이 바뀐다(라벨).
 */
export function CartItemRow({
  item,
  isChecked,
  isDeleteMode,
  onToggle,
  onChangeAmount,
}: CartItemRowProps) {
  const inputId = useId();
  const amountText = formatUnitAmount(item.amount, item.unit);

  return (
    <div className={styles.row}>
      <label htmlFor={inputId} className={styles.select}>
        <Checkbox
          id={inputId}
          checked={isChecked}
          onChange={onToggle}
          tone={isDeleteMode ? "danger" : "primary"}
        />
        <span className={styles.body}>
          <Image
            src={getCategoryImage(item.categoryCode)}
            alt=""
            width={44}
            height={29}
            className={styles.image}
          />
          <span className={styles.text}>
            <span className={styles.recipe}>{item.recipeTitle}</span>
            <span className={styles.name}>
              {item.name} {amountText}
            </span>
          </span>
        </span>
      </label>
      {/* "약간"은 숫자가 없어 바꿀 수량이 없다 */}
      <SmallButton
        onClick={onChangeAmount}
        disabled={item.unit === "TO_TASTE"}
        aria-label={`${item.name} 수량 변경`}
      >
        수량 변경
      </SmallButton>
    </div>
  );
}
