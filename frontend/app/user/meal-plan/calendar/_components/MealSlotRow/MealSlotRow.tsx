import Link from "next/link";

import { MenuDotsVerticalIcon, StarIcon } from "@solar-icons/react/bold";

import { IconButton } from "@/components/IconButton/IconButton";

import { getMealLabel } from "../../../_lib/meal-plan";
import type { MealSlot } from "../../_lib/calendar";

import styles from "./MealSlotRow.module.css";

type MealSlotRowProps = {
  slot: MealSlot;
  /** ⋮ 누르기: 식단 삭제·레시피 교체 시트(지난 날이면 쓰는 쪽에서 토스트) */
  onMore: () => void;
};

/**
 * 피그마 p5 식단 바텀 시트 한 줄: 끼니 칩(아침·점심·저녁 색) + 레시피 이름(★ 즐겨찾기) + 재료 요약(점으로 구분, +N) + ⋮.
 * 줄을 누르면 레시피 상세로 간다(domain-rules 5).
 */
export function MealSlotRow({ slot, onMore }: MealSlotRowProps) {
  return (
    <div className={styles.row}>
      <Link href={`/user/recipe/recipes/${slot.recipeId}`} className={styles.main}>
        <span className={styles.chip} data-meal={slot.mealType}>
          {getMealLabel(slot.mealType)}
        </span>
        <span className={styles.text}>
          <span className={styles.title}>
            {slot.title}
            {slot.favorite && <StarIcon className={styles.star} aria-label="즐겨찾기" role="img" />}
          </span>
          <span className={styles.ingredients}>
            {slot.ingredientSummary.map((name, index) => (
              <span key={`${name}-${index}`} className={styles.ingredient}>
                {name}
              </span>
            ))}
            {slot.extraIngredientCount > 0 && (
              <span className={styles.extra}>+{slot.extraIngredientCount}</span>
            )}
          </span>
        </span>
      </Link>
      <IconButton
        icon={<MenuDotsVerticalIcon />}
        iconSize={24}
        label={`${slot.title} 더보기`}
        onClick={onMore}
        className={styles.more}
      />
    </div>
  );
}
