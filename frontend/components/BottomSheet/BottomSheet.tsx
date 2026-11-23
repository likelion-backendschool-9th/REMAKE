"use client";

import { type ReactNode, useId, useRef } from "react";
import { createPortal } from "react-dom";

import { SheetHeader } from "@/components/SheetHeader/SheetHeader";
import { useDialogLayer } from "@/hooks/useDialogLayer";

import styles from "./BottomSheet.module.css";

type BottomSheetProps = {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  /** 시트 아래 버튼 영역. 예: <Button>확인</Button> */
  footer?: ReactNode;
};

/**
 * 피그마 디자인 시스템 > 6. 겹쳐 뜨는 것 > 딤 배경 + 바텀시트.
 * 열림 상태는 화면의 local state로 둔다(URL에 넣지 않음). 뒤로 가기·Esc·딤 누르기로 닫힌다(blueprint §7.1).
 */
export function BottomSheet({ isOpen, onClose, title, children, footer }: BottomSheetProps) {
  const layerRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  useDialogLayer(isOpen, onClose, layerRef);

  if (!isOpen) return null;

  return createPortal(
    <div ref={layerRef} className={styles.layer}>
      <div className={styles.dim} onClick={onClose} aria-hidden="true" />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className={styles.sheet}
      >
        <div className={styles.content}>
          <SheetHeader title={title} titleId={titleId} onClose={onClose} />
          {children}
        </div>
        {footer}
      </div>
    </div>,
    document.body,
  );
}
