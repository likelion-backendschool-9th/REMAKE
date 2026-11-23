"use client";

import { useState } from "react";

import { ForbiddenCircleIcon } from "@solar-icons/react/linear";

import { Button } from "@/components/Button/Button";
import { Chip } from "@/components/Chip/Chip";
import { type AllergenCode, ALLERGENS } from "@/lib/allergens";

import { OnboardingLayout } from "../../../_components/OnboardingLayout/OnboardingLayout";

import styles from "./AllergiesContainer.module.css";

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

  const toggle = (code: AllergenCode) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(code)) next.delete(code);
      else next.add(code);
      return next;
    });
  };

  const handleComplete = () => {
    // TODO: 가구원 수와 고른 코드(allergenCodes)를 API 4(PUT /user/onboarding)로 한 번에 저장한 뒤 냉장고로 이동
  };

  return (
    <OnboardingLayout
      title="레시피에서 제외할 재료를 선택해 주세요!"
      footer={<Button onClick={handleComplete}>완료</Button>}
    >
      <p className={styles.count} aria-live="polite">
        <ForbiddenCircleIcon className={styles.countIcon} aria-hidden="true" />
        <span>
          <span className={styles.countNumber} data-active={selected.size > 0 || undefined}>
            {selected.size}개
          </span>
          의 재료 제외
        </span>
      </p>

      <div className={styles.chips} role="group" aria-label="제외할 재료">
        {ALLERGENS.map(({ code, name }) => (
          <Chip
            key={code}
            variant="soft"
            isSelected={selected.has(code)}
            onClick={() => toggle(code)}
          >
            {name}
          </Chip>
        ))}
      </div>
    </OnboardingLayout>
  );
}
