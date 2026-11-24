"use client";

import { useId } from "react";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";

import { BottomSheet } from "@/components/BottomSheet/BottomSheet";
import { Button } from "@/components/Button/Button";
import { TextField } from "@/components/TextField/TextField";
import { UNIT_LABELS } from "@/lib/ingredients";

import { type CartAmountForm, cartAmountSchema, type CartItem } from "../../_lib/cart";

type CartAmountSheetProps = {
  /** 고른 항목. 없으면 닫힘 */
  item: CartItem | null;
  onClose: () => void;
  onConfirm: (amount: number) => void;
};

/** 피그마 p7-장바구니-수량 변경: "○○ 수량 변경" 바텀시트 + 숫자 입력(오른쪽 단위) + 확인. */
export function CartAmountSheet({ item, onClose, onConfirm }: CartAmountSheetProps) {
  const formId = useId();
  return (
    <BottomSheet
      isOpen={item !== null}
      onClose={onClose}
      title={item ? `${item.name} 수량 변경` : ""}
      footer={
        <Button type="submit" form={formId}>
          확인
        </Button>
      }
    >
      {/* 열 때마다 처음 값으로 시작하도록 항목이 있을 때만 그린다 */}
      {item && <AmountForm key={item.id} formId={formId} item={item} onConfirm={onConfirm} />}
    </BottomSheet>
  );
}

function AmountForm({
  formId,
  item,
  onConfirm,
}: {
  formId: string;
  item: CartItem;
  onConfirm: (amount: number) => void;
}) {
  const { register, handleSubmit, control } = useForm<CartAmountForm>({
    resolver: zodResolver(cartAmountSchema),
    defaultValues: { amount: item.amount === null ? "" : String(item.amount) },
  });
  const value = useWatch({ control, name: "amount" });
  // 빈칸은 오류 문구 없이 확인만 막는다(제출 때 스키마가 막음)
  const error = value
    ? cartAmountSchema.safeParse({ amount: value }).error?.issues[0]?.message
    : undefined;

  return (
    <form id={formId} onSubmit={handleSubmit(({ amount }) => onConfirm(Number(amount)))} noValidate>
      <TextField
        {...register("amount")}
        aria-label={`${item.name} 수량`}
        inputMode="decimal"
        unit={UNIT_LABELS[item.unit]}
        errorMessage={error}
        autoComplete="off"
        data-autofocus
      />
    </form>
  );
}
