"use client";

import { useState } from "react";

import { BottomSheet } from "@/components/BottomSheet/BottomSheet";
import { Button } from "@/components/Button/Button";
import { ExpiryLabel } from "@/components/ExpiryLabel/ExpiryLabel";
import { SegmentedControl } from "@/components/SegmentedControl/SegmentedControl";
import { Stepper } from "@/components/Stepper/Stepper";
import { formatAmount, FRIDGE_UNITS } from "@/lib/ingredients";

import type { FridgeIngredient } from "../../_lib/fridge";

import styles from "./UseIngredientSheet.module.css";

export type UseKind = "USED" | "DISCARDED";

const USE_OPTIONS = [
  { value: "USED", label: "사용했어요" },
  { value: "DISCARDED", label: "버렸어요" },
] as const satisfies readonly { value: UseKind; label: string }[];

type UseIngredientSheetProps = {
  /** 고른 재료. 없으면 닫힘 */
  ingredient: FridgeIngredient | null;
  onClose: () => void;
  /** 확인: 사용·폐기와 뺄 양. 결과 토스트와 시트 닫기는 쓰는 쪽에서 한다 */
  onConfirm: (kind: UseKind, amount: number) => void;
};

/**
 * 피그마 바텀시트/재료 사용(UseIngredientSheet). 냉장고에서 재료 행을 누르면 뜬다.
 * 사용했어요/버렸어요 + 뺄 수량. 뺄 수량은 남은 양을 넘지 않게 막는다(domain-rules 2, 막는 방식은 제안).
 */
export function UseIngredientSheet({ ingredient, onClose, onConfirm }: UseIngredientSheetProps) {
  return (
    <BottomSheet isOpen={ingredient !== null} onClose={onClose} title={ingredient?.name ?? ""}>
      {/* 열 때마다 처음 값으로 되돌리려고 재료가 있을 때만 그린다 */}
      {ingredient && (
        <UseIngredientForm key={ingredient.id} ingredient={ingredient} onConfirm={onConfirm} />
      )}
    </BottomSheet>
  );
}

function UseIngredientForm({
  ingredient,
  onConfirm,
}: {
  ingredient: FridgeIngredient;
  onConfirm: UseIngredientSheetProps["onConfirm"];
}) {
  const { step } = FRIDGE_UNITS[ingredient.unit];
  const [kind, setKind] = useState<UseKind>("USED");
  // 한 번에 빼는 기본 양: 한 칸(step). 남은 양이 더 적으면 남은 양
  const [amount, setAmount] = useState(Math.min(step, ingredient.amount));

  return (
    <div className={styles.body}>
      <p className={styles.remaining}>
        <span>남은 수량 {formatAmount(ingredient.amount, ingredient.unit)}</span>
        <ExpiryLabel status={ingredient.expiryStatus} text={ingredient.dDay} variant="badge" />
      </p>
      <SegmentedControl
        label="재료 사용 여부"
        options={USE_OPTIONS}
        value={kind}
        onChange={setKind}
      />
      <div className={styles.amount}>
        <span className={styles.amountLabel}>뺄 수량</span>
        <Stepper
          label="뺄 수량"
          value={amount}
          onChange={setAmount}
          step={step}
          min={Math.min(step, ingredient.amount)}
          max={ingredient.amount}
          formatValue={(value) => formatAmount(value, ingredient.unit)}
        />
      </div>
      <p className={styles.notice}>버렸어요를 고르시면 이번 달 버린 식재료 금액에 더해져요</p>
      <Button className={styles.confirm} onClick={() => onConfirm(kind, amount)}>
        확인
      </Button>
    </div>
  );
}
