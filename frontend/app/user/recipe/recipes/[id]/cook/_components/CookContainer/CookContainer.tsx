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

import { type CookPreview, formatCookAmount } from "../../_lib/cook";

import styles from "./CookContainer.module.css";

type CookContainerProps = {
  /** 차감할 재료(API 31). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  preview?: CookPreview;
  /** 처음 체크해 둘 재료 ID. 미리보기용(피그마 레시피 수량 변경) */
  initialCheckedIds?: number[];
};

/**
 * p4.1.1.2 레시피 사용 후 재료 차감. "총 N개의 재료를 차감해 주세요", 내가 가진 레시피 재료를 체크하고
 * ">"로 뺄 양을 고친 뒤 "N개의 재료 차감하기"(N = 체크한 수, API 32). 물·꼬집·약간은 서버가 빼고 준다(domain-rules 4).
 * 상태: 기본 / 레시피 수량 변경(체크함) / 스켈레톤(실제 라우트 처음 상태).
 */
export function CookContainer({ preview, initialCheckedIds = [] }: CookContainerProps) {
  const [checked, setChecked] = useState<ReadonlySet<number>>(() => new Set(initialCheckedIds));

  const toggle = (id: number) =>
    setChecked((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const handleDeduct = () => {
    // TODO: API 32({ items: [{ ingredientId, amount }], mealPlanSlotId? })를 보내고 냉장고(p2)로 가서
    // "이 레시피 어땠나요?" 시트(배치 대기, blueprint §4 Not placed yet)를 연다. 체크 0개일 때 동작은 API와 맞춘다
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
                    checked={checked.has(item.ingredientId)}
                    onChange={() => toggle(item.ingredientId)}
                    aria-label={item.name}
                  />
                  <IngredientRow
                    name={item.name}
                    imageSrc={item.imageUrl}
                    trailing={<span className={styles.amount}>{formatCookAmount(item)}</span>}
                    className={styles.row}
                  />
                </label>
                {/* TODO: 뺄 양 시트(피그마 디자인 v2 대기). 남은 양(remainingAmount)까지만 고를 수 있게 한다 */}
                <IconButton
                  icon={<AltArrowRightIcon />}
                  iconSize={20}
                  label={`${item.name} 차감할 양 바꾸기`}
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
          {checked.size > 0 ? `${checked.size}개의 재료 차감하기` : "재료 차감하기"}
        </Button>
      </BottomBar>
    </>
  );
}
