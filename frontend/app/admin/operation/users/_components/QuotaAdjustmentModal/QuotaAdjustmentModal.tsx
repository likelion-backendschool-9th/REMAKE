"use client";

import { useId, useRef } from "react";
import { createPortal } from "react-dom";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";

import { Stepper } from "@/components/Stepper/Stepper";
import { TextButton } from "@/components/TextButton/TextButton";
import { TextField } from "@/components/TextField/TextField";
import { useDialogLayer } from "@/hooks/useDialogLayer";
import { useIsClient } from "@/hooks/useIsClient";

import styles from "./QuotaAdjustmentModal.module.css";

const schema = z.object({
  reason: z.string().trim().min(1, "사유를 입력해야 조정할 수 있어요"),
  delta: z.number().int(),
});
type Adjustment = z.infer<typeof schema>;

export function QuotaAdjustmentModal({
  userId,
  remaining,
  initialReason = "",
  initialError = false,
  onClose,
  onConfirm,
}: {
  userId: number;
  remaining: number;
  initialReason?: string;
  initialError?: boolean;
  onClose: () => void;
  onConfirm: (adjustment: Adjustment) => void;
}) {
  const layerRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const descriptionId = useId();
  const isClient = useIsClient();
  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<Adjustment>({
    resolver: zodResolver(schema),
    defaultValues: { reason: initialReason, delta: 2 },
  });
  const delta = useWatch({ control, name: "delta" });
  const reason = useWatch({ control, name: "reason" });
  useDialogLayer(isClient, onClose, layerRef);
  if (!isClient) return null;

  return createPortal(
    <div className={styles.layer} ref={layerRef}>
      <div className={styles.dim} aria-hidden="true" onClick={onClose} />
      <form
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        tabIndex={-1}
        onKeyDown={(event) => {
          if (event.key !== "Tab") return;
          const controls = Array.from(
            event.currentTarget.querySelectorAll<HTMLElement>(
              "button:not(:disabled), input:not(:disabled)",
            ),
          );
          const first = controls[0];
          const last = controls.at(-1);
          if (
            event.shiftKey &&
            (document.activeElement === first || document.activeElement === event.currentTarget)
          ) {
            event.preventDefault();
            last?.focus();
          } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first?.focus();
          }
        }}
        onSubmit={handleSubmit((values) => {
          if (values.delta !== 0 && values.delta >= -remaining) onConfirm(values);
        })}
      >
        <h2 id={titleId}>생성 횟수 조정</h2>
        <p id={descriptionId}>
          회원 #{userId} · 지금 남은 보상 생성 횟수 {remaining}회
        </p>
        <div className={styles.stepperRow}>
          <span>늘리거나 줄일 횟수</span>
          <Stepper
            label="조정 횟수"
            value={delta}
            min={-remaining}
            onChange={(value) => setValue("delta", value)}
            formatValue={(value) => `${value > 0 ? "+" : ""}${value}회`}
          />
        </div>
        <TextField
          label="사유(필수)"
          placeholder="조정 사유를 입력해 주세요"
          helperText="조정 이력에 남아요"
          clearable={false}
          errorMessage={
            errors.reason?.message ??
            (initialError && !reason.trim() ? "사유를 입력해야 조정할 수 있어요" : undefined)
          }
          {...register("reason")}
        />
        <div className={styles.actions}>
          <TextButton tone="neutral" onClick={onClose}>
            취소하기
          </TextButton>
          <TextButton type="submit" disabled={delta === 0}>
            조정하기
          </TextButton>
        </div>
      </form>
    </div>,
    document.body,
  );
}
