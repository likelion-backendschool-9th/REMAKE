"use client";

import { type ReactNode, useId, useRef } from "react";
import { createPortal } from "react-dom";

import { SheetHeader } from "@/components/SheetHeader/SheetHeader";
import { useDialogLayer } from "@/hooks/useDialogLayer";
import { useIsClient } from "@/hooks/useIsClient";

import styles from "./BottomSheet.module.css";

type BottomSheetProps = {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  /** 시트 아래 버튼 영역. 예: <Button>확인</Button> */
  footer?: ReactNode;
  /** 제목 줄(제목 + 닫기)을 화면에서 숨긴다. 제목은 스크린 리더용으로 남는다(피그마 p5 식단표 추가 시트) */
  isHeaderHidden?: boolean;
  /** 위쪽 모서리를 둥글게 하지 않는다(피그마 p5-식단표-식단표 추가 시트) */
  isSquare?: boolean;
};

/**
 * 피그마 디자인 시스템 > 6. 겹쳐 뜨는 것 > 딤 배경 + 바텀시트.
 * 열림 상태는 화면의 local state로 둔다(URL에 넣지 않음). 뒤로 가기·Esc·딤 누르기로 닫힌다(blueprint §7.1).
 */
export function BottomSheet({
  isOpen,
  onClose,
  title,
  children,
  footer,
  isHeaderHidden = false,
  isSquare = false,
}: BottomSheetProps) {
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
        data-square={isSquare || undefined}
      >
        <div className={styles.content}>
          {isHeaderHidden ? (
            <h2 id={titleId} className={styles.srOnly}>
              {title}
            </h2>
          ) : (
            <SheetHeader title={title} titleId={titleId} onClose={onClose} />
          )}
          {children}
        </div>
        {footer}
      </div>
    </div>,
    document.body,
  );
}
