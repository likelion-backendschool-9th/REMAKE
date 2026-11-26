"use client";

import { useState } from "react";

import { BottomSheet } from "@/components/BottomSheet/BottomSheet";
import { Button } from "@/components/Button/Button";
import { Stepper } from "@/components/Stepper/Stepper";
import { formatAmount, FRIDGE_UNITS } from "@/lib/ingredients";

import type { CookItem } from "../../_lib/cook";

import styles from "./CookAmountSheet.module.css";

type CookAmountSheetProps = {
  /** 양을 고칠 재료. 없으면 닫힘 */
  item: CookItem | null;
  /** 지금 뺄 양(사용자가 고친 양이 있으면 그 값). null이면 아직 모름(단위 종류가 달라 서버가 못 정함) */
  amount: number | null;
  onClose: () => void;
  /** 확인: 고른 뺄 양. 시트 닫기는 쓰는 쪽에서 한다 */
  onConfirm: (amount: number) => void;
};

/**
 * 피그마 p4.1.1.2-레시피 사용 후 재료 차감-사용 재료 수량 수정(바텀시트/재료 사용, 5992-7325).
 * 재료명 제목, "남는 수량"(차감하고 냉장고에 남을 양), 뺄 수량 스테퍼, 확인.
 * 뺄 양은 한 칸(단위별 step)부터 냉장고에 남은 양(remainingAmount)까지(API 32는 0 이하를 받지 않음).
 */
export function CookAmountSheet({ item, amount, onClose, onConfirm }: CookAmountSheetProps) {
  return (
    <BottomSheet isOpen={item !== null} onClose={onClose} title={item?.name ?? ""}>
      {/* 열 때마다 지금 값으로 되돌리려고 재료가 있을 때만 그린다 */}
      {item && (
        <CookAmountForm key={item.ingredientId} item={item} amount={amount} onConfirm={onConfirm} />
      )}
    </BottomSheet>
  );
}

function CookAmountForm({
  item,
  amount: initialAmount,
  onConfirm,
}: {
  item: CookItem;
  amount: number | null;
  onConfirm: (amount: number) => void;
}) {
  const { step } = FRIDGE_UNITS[item.unit];
  const min = Math.min(step, item.remainingAmount);
  const [amount, setAmount] = useState(() =>
    Math.min(Math.max(initialAmount ?? min, min), item.remainingAmount),
  );
  // 0.5 단위 등 소수 계산 오차를 없앤다(Stepper와 같은 방식)
  const left = Math.round((item.remainingAmount - amount) * 1000) / 1000;

  return (
    <div className={styles.body}>
      <p className={styles.remaining}>남는 수량 {formatAmount(left, item.unit)}</p>
      <div className={styles.amount}>
        <span className={styles.amountLabel}>뺄 수량</span>
        <Stepper
          label="뺄 수량"
          value={amount}
          onChange={setAmount}
          step={step}
          min={min}
          max={item.remainingAmount}
          formatValue={(value) => formatAmount(value, item.unit)}
        />
      </div>
      <Button className={styles.confirm} onClick={() => onConfirm(amount)}>
        확인
      </Button>
    </div>
  );
}
