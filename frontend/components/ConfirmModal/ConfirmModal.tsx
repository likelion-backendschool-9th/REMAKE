"use client";

import { useId, useRef } from "react";
import { createPortal } from "react-dom";

import { CloseIcon } from "@solar-icons/react/linear";

import { IconButton } from "@/components/IconButton/IconButton";
import { useDialogLayer } from "@/hooks/useDialogLayer";
import { useIsClient } from "@/hooks/useIsClient";

import styles from "./ConfirmModal.module.css";

type ConfirmModalProps = {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel?: string;
  /** 피그마 색: 기본=primary(빠른 레시피 생성), 위험=danger(영수증 삭제·로그아웃·계정 삭제) */
  tone?: "primary" | "danger";
  /** 확인 요청 중이면 버튼을 막는다. */
  isPending?: boolean;
};

/** 피그마 디자인 시스템 > 6. 겹쳐 뜨는 것 > 확인 모달. 가운데 뜨는 확인창. 취소·닫기·뒤로 가기·Esc는 onClose. */
export function ConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel,
  cancelLabel = "취소하기",
  tone = "primary",
  isPending = false,
}: ConfirmModalProps) {
  const layerRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const messageId = useId();
  const isClient = useIsClient();
  useDialogLayer(isOpen && isClient, onClose, layerRef);

  // 포털은 document가 있어야 하므로 서버 렌더링에서는 그리지 않는다
  if (!isOpen || !isClient) return null;

  return createPortal(
    <div ref={layerRef} className={styles.layer}>
      <div className={styles.dim} onClick={onClose} aria-hidden="true" />
      <div
        role={tone === "danger" ? "alertdialog" : "dialog"}
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={messageId}
        tabIndex={-1}
        className={styles.modal}
      >
        <div className={styles.head}>
          <h2 id={titleId} className={styles.title}>
            {title}
          </h2>
          <IconButton
            icon={<CloseIcon strokeWidth={2.25} />}
            iconSize={16}
            label="닫기"
            onClick={onClose}
            className={styles.close}
          />
        </div>
        <p id={messageId} className={styles.message}>
          {message}
        </p>
        <div className={styles.actions}>
          <button type="button" className={styles.action} onClick={onClose} data-autofocus>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={styles.action}
            data-tone={tone}
            onClick={onConfirm}
            disabled={isPending}
            aria-busy={isPending || undefined}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
