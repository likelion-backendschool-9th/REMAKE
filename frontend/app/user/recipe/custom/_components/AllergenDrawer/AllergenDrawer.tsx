"use client";

import { useState } from "react";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { Button } from "@/components/Button/Button";
import { Drawer } from "@/components/Drawer/Drawer";
import type { AllergenCode } from "@/lib/allergens";

import {
  AllergenSelector,
  toggleAllergen,
} from "../../../../_components/AllergenSelector/AllergenSelector";

import styles from "./AllergenDrawer.module.css";

const TITLE = "레시피에서 제외할 재료를 선택해 주세요!";

type AllergenDrawerProps = {
  isOpen: boolean;
  onClose: () => void;
  /** 열 때 골라 둘 알레르기 재료 */
  defaultCodes: readonly AllergenCode[];
  onConfirm: (codes: AllergenCode[]) => void;
};

/**
 * 피그마 p4.1.2-커스텀 레시피 생성-알레르기 재료: 이번 레시피에서 뺄 알레르기 재료 칩(설정 p1.3.2와 같은 19개).
 * 0개여도 확인할 수 있다. 설정의 제외 재료는 바꾸지 않고 이번 생성 요청에만 쓴다.
 */
export function AllergenDrawer({ isOpen, onClose, defaultCodes, onConfirm }: AllergenDrawerProps) {
  return (
    <Drawer
      isOpen={isOpen}
      onClose={onClose}
      title={TITLE}
      hideTitle
      contentClassName={styles.content}
    >
      {isOpen && <AllergenBody defaultCodes={defaultCodes} onConfirm={onConfirm} />}
    </Drawer>
  );
}

function AllergenBody({
  defaultCodes,
  onConfirm,
}: Pick<AllergenDrawerProps, "defaultCodes" | "onConfirm">) {
  const [selected, setSelected] = useState<ReadonlySet<AllergenCode>>(() => new Set(defaultCodes));

  return (
    <>
      <h2 className={styles.title}>{TITLE}</h2>
      <AllergenSelector
        selected={selected}
        onToggle={(code) => setSelected((prev) => toggleAllergen(prev, code))}
      />
      <BottomBar>
        <Button onClick={() => onConfirm([...selected])}>{selected.size}개의 재료 제외</Button>
      </BottomBar>
    </>
  );
}
