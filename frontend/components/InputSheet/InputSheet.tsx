"use client";

import { type ReactNode, useId, useRef } from "react";
import { createPortal } from "react-dom";

import { SheetHeader } from "@/components/SheetHeader/SheetHeader";
import { useDialogLayer } from "@/hooks/useDialogLayer";
import { useIsClient } from "@/hooks/useIsClient";

import styles from "./InputSheet.module.css";

type InputSheetProps = {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  /** 본문: 텍스트=TextField(+안내), 휠=WheelPicker */
  children: ReactNode;
  /** 아래 버튼. 보통 <Button size="md">수정</Button> */
  footer?: ReactNode;
};

/**
 * 피그마 디자인 시스템 > 6. 겹쳐 뜨는 것 > 입력 시트. 아래쪽에 떠 있는 카드형 입력 시트(좌우 16 여백).
 * 텍스트=재료명·재료 가격, 휠 3열=소비기한·시작일, 휠 2열=캘린더 선택.
 * 열림 상태는 화면의 local state로 둔다. 뒤로 가기·Esc·딤 누르기로 닫힌다(blueprint §7.1).
 */
export function InputSheet({ isOpen, onClose, title, children, footer }: InputSheetProps) {
  const layerRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const isClient = useIsClient();
  useDialogLayer(isOpen && isClient, onClose, layerRef);

  // 포털은 document가 있어야 하므로 서버 렌더링에서는 그리지 않는다
  if (!isOpen || !isClient) return null;

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
        <SheetHeader title={title} titleId={titleId} onClose={onClose} />
        {children}
        {footer}
      </div>
    </div>,
    document.body,
  );
}
