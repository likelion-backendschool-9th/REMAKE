"use client";

import { useState } from "react";

import { Button } from "@/components/Button/Button";
import type { AllergenCode } from "@/lib/allergens";

import {
  AllergenSelector,
  toggleAllergen,
} from "../../../../_components/AllergenSelector/AllergenSelector";
import { QuestionLayout } from "../../../../_components/QuestionLayout/QuestionLayout";

type ExcludedIngredientsContainerProps = {
  /** 지금 저장된 제외 재료. 기본은 마크업 확인용 값(피그마 p1.3.2: 쇠고기) */
  defaultSelected?: AllergenCode[];
};

/**
 * p1.3.2 알레르기 재료 제외. 지금 값을 고친 뒤 "N개의 재료 제외"로 통째 저장한다. 0개여도 저장할 수 있다(정책).
 * 상태: N개 선택(p1.3.2-알러지 재료 제외) / 0개.
 */
export function ExcludedIngredientsContainer({
  // TODO: API 7(GET /user/excluded-ingredients) 값을 쓴다
  defaultSelected = ["BEEF"],
}: ExcludedIngredientsContainerProps) {
  const [selected, setSelected] = useState<ReadonlySet<AllergenCode>>(
    () => new Set(defaultSelected),
  );

  const handleSave = () => {
    // TODO: API 8(PUT /user/excluded-ingredients)로 통째 저장하고 E15 excluded_ingredients_updated를 보낸 뒤 설정으로
  };

  return (
    <QuestionLayout
      title="레시피에서 제외할 재료를 선택해 주세요!"
      footer={<Button onClick={handleSave}>{selected.size}개의 재료 제외</Button>}
    >
      <AllergenSelector
        selected={selected}
        onToggle={(code) => setSelected((prev) => toggleAllergen(prev, code))}
      />
    </QuestionLayout>
  );
}
