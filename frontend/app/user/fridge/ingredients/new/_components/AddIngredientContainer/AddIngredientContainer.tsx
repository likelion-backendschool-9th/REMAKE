"use client";

import { useState } from "react";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { IngredientForm, type IngredientSheet } from "@/components/IngredientForm/IngredientForm";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { EMPTY_DRAFT, type IngredientDraft, isDraftComplete } from "@/lib/ingredient-form";

import styles from "./AddIngredientContainer.module.css";

type AddIngredientContainerProps = {
  /** 처음 값. 미리보기용, 기본 빈 값(피그마 p2.2-재료 추가) */
  initialDraft?: IngredientDraft;
  /** 처음부터 열어 둘 값 시트. 미리보기용 */
  initialSheet?: IngredientSheet | null;
};

/**
 * p2.2 재료 추가. 가격을 뺀 값을 모두 채우면 "재료 추가" 버튼이 켜진다.
 * 상태: 빈 값(버튼 꺼짐) / 모두 채움 / 값 시트(재료명·카테고리·보관 위치·소비 기한·가격).
 */
export function AddIngredientContainer({
  initialDraft = EMPTY_DRAFT,
  initialSheet = null,
}: AddIngredientContainerProps) {
  const [draft, setDraft] = useState(initialDraft);

  const handleSubmit = () => {
    // TODO: API 15(POST /user/ingredients)로 저장하고 냉장고 목록 쿼리를 무효화한 뒤 냉장고로 돌아간다
  };

  return (
    <>
      <SubHeader title="재료 추가" />
      <main className={styles.main}>
        <IngredientForm draft={draft} onChange={setDraft} initialSheet={initialSheet} />
      </main>
      <BottomBar>
        <Button disabled={!isDraftComplete(draft)} onClick={handleSubmit}>
          재료 추가
        </Button>
      </BottomBar>
    </>
  );
}
