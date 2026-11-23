"use client";

import { useState } from "react";

import { Button } from "@/components/Button/Button";
import type { AllergenCode } from "@/lib/allergens";

import {
  AllergenSelector,
  toggleAllergen,
} from "../../../../_components/AllergenSelector/AllergenSelector";
import { QuestionLayout } from "../../../../_components/QuestionLayout/QuestionLayout";

type AllergiesContainerProps = {
  /** 처음 선택된 재료. 미리보기용, 기본 없음 */
  defaultSelected?: AllergenCode[];
};

/**
 * p1.2 알레르기 선택(온보딩 2단계). 칩 19개 중 여러 개를 고른다. 0개여도 "완료"할 수 있다(정책).
 * 상태: 0개(p1.2-알러지 선택) / N개 선택(p1.2-알러지 선택 포커스).
 */
export function AllergiesContainer({ defaultSelected = [] }: AllergiesContainerProps) {
  const [selected, setSelected] = useState<ReadonlySet<AllergenCode>>(
    () => new Set(defaultSelected),
  );

  const handleComplete = () => {
    // TODO: 가구원 수와 고른 코드(allergenCodes)를 API 4(PUT /user/onboarding)로 한 번에 저장한 뒤 냉장고로 이동
  };

  return (
    <QuestionLayout
      title="레시피에서 제외할 재료를 선택해 주세요!"
      footer={<Button onClick={handleComplete}>완료</Button>}
    >
      <AllergenSelector
        selected={selected}
        onToggle={(code) => setSelected((prev) => toggleAllergen(prev, code))}
      />
    </QuestionLayout>
  );
}
