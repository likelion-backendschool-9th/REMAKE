"use client";

import { useState } from "react";

import { AltArrowRightIcon } from "@solar-icons/react/linear";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { Checkbox } from "@/components/Checkbox/Checkbox";
import { IconButton } from "@/components/IconButton/IconButton";
import { IngredientRow } from "@/components/IngredientRow/IngredientRow";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";

import { type CookItem, type CookPreview, formatCookAmount } from "../../_lib/cook";
import { CookAmountSheet } from "../CookAmountSheet/CookAmountSheet";

import styles from "./CookContainer.module.css";

type CookContainerProps = {
  /** 차감할 재료(API 31). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  preview?: CookPreview;
  /** 처음 체크해 둘 재료 ID. 없으면 모두 체크. 미리보기용(일부만 체크한 상태) */
  initialCheckedIds?: number[];
  /** 뺄 양 시트를 처음부터 열 재료 ID. 미리보기용(피그마 사용 재료 수량 수정) */
  initialEditingId?: number;
};

/**
 * p4.1.1.2 레시피 사용 후 재료 차감. "총 N개의 재료를 차감해 주세요", 내가 가진 레시피 재료를 체크하고
 * ">"로 뺄 양을 고친 뒤 "N개의 재료 차감하기"(N = 체크한 수, API 32). 물·꼬집·약간은 서버가 빼고 준다(domain-rules 4).
 * 처음에는 모두 체크한다. 레시피대로 요리를 마치고 오는 화면이라 레시피 재료를 냉장고에서 썼다고 보고,
 * 쓰지 않은 재료만 사용자가 해제한다. 모두 해제하면 버튼이 "요리 완료"가 된다(API 32는 빈 items를 차감 없는 요리 완료로 받음).
 * ">"를 누르면 뺄 양 시트(CookAmountSheet)에서 양을 고친다. 고친 양으로 확인하면 그 재료는 체크한다(쓴 재료로 봄).
 * 상태: 기본(모두 체크) / 일부 해제 / 뺄 양 시트 / 스켈레톤(실제 라우트 처음 상태).
 */
export function CookContainer({
  preview,
  initialCheckedIds,
  initialEditingId,
}: CookContainerProps) {
  // 해제한 재료를 기억한다(나머지는 체크). 재료 목록(API 31)이 나중에 와도 처음에 모두 체크된 채로 보인다
  const [unchecked, setUnchecked] = useState<ReadonlySet<number>>(
    () =>
      new Set(
        initialCheckedIds && preview
          ? preview.items
              .map((item) => item.ingredientId)
              .filter((id) => !initialCheckedIds.includes(id))
          : [],
      ),
  );
  const checkedCount = preview
    ? preview.items.filter((item) => !unchecked.has(item.ingredientId)).length
    : 0;
  // 사용자가 시트에서 고친 뺄 양(재료 ID → 냉장고 단위 양). 없으면 서버가 정한 양(API 31 amount)
  const [editedAmounts, setEditedAmounts] = useState<ReadonlyMap<number, number>>(new Map());
  const amountOf = (item: CookItem) => editedAmounts.get(item.ingredientId) ?? item.amount;
  const [editingId, setEditingId] = useState<number | null>(initialEditingId ?? null);
  const editingItem = preview?.items.find((item) => item.ingredientId === editingId) ?? null;

  const handleAmountConfirm = (amount: number) => {
    if (editingId === null) return;
    const id = editingId;
    setEditedAmounts((prev) => new Map(prev).set(id, amount));
    setUnchecked((prev) => {
      const next = new Set(prev);
      next.delete(id);
      return next;
    });
    setEditingId(null);
  };

  const toggle = (id: number) =>
    setUnchecked((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const handleDeduct = () => {
    // 체크했는데 양을 모르는 재료(단위 종류가 달라 서버가 못 정함)가 있으면 먼저 그 재료의 뺄 양 시트를 연다
    const unknown = preview?.items.find(
      (item) => !unchecked.has(item.ingredientId) && amountOf(item) === null,
    );
    if (unknown) {
      setEditingId(unknown.ingredientId);
      return;
    }
    // TODO: API 32({ items: [{ ingredientId, amount }], mealPlanSlotId? })를 보내고 냉장고(p2)로 가서
    // "이 레시피 어땠나요?" 시트(배치 대기, blueprint §4 Not placed yet)를 연다. 체크 0개면 items: [](차감 없이 요리 완료)
  };

  return (
    <>
      <SubHeader />
      <main className={styles.main}>
        <div className={styles.heading}>
          {preview ? (
            <h1 className={styles.title}>총 {preview.items.length}개의 재료를 차감해 주세요</h1>
          ) : (
            <Skeleton width="13.75rem" height="1.25rem" />
          )}
          <p className={styles.description}>이번 레시피에 사용된 재료들을 선택해서 제거해 주세요</p>
        </div>

        {preview ? (
          // TODO: 내 냉장고에 차감할 재료가 하나도 없을 때 빈 화면(피그마 "추가 화면", 디자인 v2 대기)
          <ul className={styles.list}>
            {preview.items.map((item) => (
              <li key={item.ingredientId} className={styles.item}>
                <label className={styles.check}>
                  <Checkbox
                    checked={!unchecked.has(item.ingredientId)}
                    onChange={() => toggle(item.ingredientId)}
                    aria-label={item.name}
                  />
                  <IngredientRow
                    name={item.name}
                    imageSrc={item.imageUrl}
                    trailing={
                      <span className={styles.amount}>
                        {formatCookAmount({ ...item, amount: amountOf(item) })}
                      </span>
                    }
                    className={styles.row}
                  />
                </label>
                <IconButton
                  icon={<AltArrowRightIcon />}
                  iconSize={20}
                  label={`${item.name} 차감할 양 바꾸기`}
                  aria-haspopup="dialog"
                  onClick={() => setEditingId(item.ingredientId)}
                  className={styles.edit}
                />
              </li>
            ))}
          </ul>
        ) : (
          <div className={styles.skeletons} aria-busy="true">
            <Skeleton width="100%" height="1.75rem" />
            <Skeleton width="100%" height="1.75rem" />
          </div>
        )}
      </main>

      <BottomBar>
        <Button disabled={!preview} onClick={handleDeduct}>
          {checkedCount > 0 ? `${checkedCount}개의 재료 차감하기` : "요리 완료"}
        </Button>
      </BottomBar>

      <CookAmountSheet
        item={editingItem}
        amount={editingItem ? amountOf(editingItem) : null}
        onClose={() => setEditingId(null)}
        onConfirm={handleAmountConfirm}
      />
    </>
  );
}
