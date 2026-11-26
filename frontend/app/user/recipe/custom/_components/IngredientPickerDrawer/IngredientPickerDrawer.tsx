"use client";

import { useState } from "react";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { Checkbox } from "@/components/Checkbox/Checkbox";
import { Drawer } from "@/components/Drawer/Drawer";
import { EmptyState } from "@/components/EmptyState/EmptyState";
import { ExpiryLabel } from "@/components/ExpiryLabel/ExpiryLabel";
import { IngredientRow } from "@/components/IngredientRow/IngredientRow";
import { formatAmount, getCategoryImage, getStorage } from "@/lib/ingredients";

import type { PickableIngredient } from "../../_lib/custom-recipe";

import styles from "./IngredientPickerDrawer.module.css";

type IngredientPickerDrawerProps = {
  isOpen: boolean;
  onClose: () => void;
  /** 내 냉장고 재료(API 12). 서버 순서 그대로(소비기한 급한 순) */
  ingredients: PickableIngredient[];
  /** 열 때 체크해 둘 재료 ID */
  defaultIds: readonly number[];
  onConfirm: (ids: number[]) => void;
};

/**
 * 피그마 p4.1.2-커스텀 레시피 생성-드로워 디세이블·액티브: 내 냉장고 재료를 여러 개 골라 레시피에 넣는다.
 * 하나도 안 고르면 "선택"이 꺼지고, 고르면 "N개의 재료 선택".
 */
export function IngredientPickerDrawer({
  isOpen,
  onClose,
  ingredients,
  defaultIds,
  onConfirm,
}: IngredientPickerDrawerProps) {
  return (
    <Drawer isOpen={isOpen} onClose={onClose} title="재료 선택" contentClassName={styles.content}>
      {/* 열 때마다 체크를 처음 값으로 되돌리려고 열려 있을 때만 그린다 */}
      {isOpen && (
        <PickerBody ingredients={ingredients} defaultIds={defaultIds} onConfirm={onConfirm} />
      )}
    </Drawer>
  );
}

function PickerBody({
  ingredients,
  defaultIds,
  onConfirm,
}: Pick<IngredientPickerDrawerProps, "ingredients" | "defaultIds" | "onConfirm">) {
  const [selected, setSelected] = useState<ReadonlySet<number>>(() => new Set(defaultIds));

  const toggle = (id: number) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  return (
    <>
      {ingredients.length === 0 ? (
        // 피그마에 없는 상태. 냉장고 빈 목록 문구(p2)를 따른다
        <EmptyState>냉장고에 재료가 없어요</EmptyState>
      ) : (
        <ul className={styles.list}>
          {ingredients.map((ingredient) => (
            <li key={ingredient.id}>
              <label className={styles.row}>
                <Checkbox
                  checked={selected.has(ingredient.id)}
                  onChange={() => toggle(ingredient.id)}
                  aria-label={ingredient.name}
                />
                <IngredientRow
                  name={ingredient.name}
                  imageSrc={getCategoryImage(ingredient.categoryCode)}
                  description={`${getStorage(ingredient.storage).place} · ${formatAmount(ingredient.amount, ingredient.unit)}`}
                  trailing={<ExpiryLabel status={ingredient.expiryStatus} text={ingredient.dDay} />}
                  className={styles.ingredient}
                />
              </label>
            </li>
          ))}
        </ul>
      )}

      <BottomBar>
        <Button
          disabled={selected.size === 0}
          onClick={() =>
            onConfirm(ingredients.filter((item) => selected.has(item.id)).map((item) => item.id))
          }
        >
          {selected.size === 0 ? "선택" : `${selected.size}개의 재료 선택`}
        </Button>
      </BottomBar>
    </>
  );
}
