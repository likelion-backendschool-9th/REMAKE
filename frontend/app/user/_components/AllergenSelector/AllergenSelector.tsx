"use client";

import { ForbiddenCircleIcon } from "@solar-icons/react/linear";

import { Chip } from "@/components/Chip/Chip";
import { type AllergenCode, ALLERGENS } from "@/lib/allergens";

import styles from "./AllergenSelector.module.css";

type AllergenSelectorProps = {
  selected: ReadonlySet<AllergenCode>;
  onToggle: (code: AllergenCode) => void;
};

/**
 * "N개의 재료 제외" + 알레르기 칩 19개. 여러 개 고른다.
 * 온보딩 p1.2와 설정 p1.3.2가 쓴다.
 */
export function AllergenSelector({ selected, onToggle }: AllergenSelectorProps) {
  return (
    <>
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
            onClick={() => onToggle(code)}
          >
            {name}
          </Chip>
        ))}
      </div>
    </>
  );
}

/** 고른 코드 집합에서 하나를 넣거나 뺀 새 집합 */
export function toggleAllergen(
  prev: ReadonlySet<AllergenCode>,
  code: AllergenCode,
): ReadonlySet<AllergenCode> {
  const next = new Set(prev);
  if (next.has(code)) next.delete(code);
  else next.add(code);
  return next;
}
