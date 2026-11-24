"use client";

import { type ReactNode, useId, useRef } from "react";
import { createPortal } from "react-dom";

import { SubHeader } from "@/components/SubHeader/SubHeader";
import { useDialogLayer } from "@/hooks/useDialogLayer";
import { useIsClient } from "@/hooks/useIsClient";

import styles from "./Drawer.module.css";

type DrawerProps = {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
};

/**
 * 피그마 common-약관드로워. 화면 전체를 덮는 드로워: 서브 헤더(← + 제목) + 스크롤되는 내용.
 * 열림 상태는 화면의 local state로 둔다(URL에 넣지 않음). ←·뒤로 가기·Esc로 닫힌다(blueprint §7.1).
 */
export function Drawer({ isOpen, onClose, title, children }: DrawerProps) {
  const layerRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const isClient = useIsClient();
  useDialogLayer(isOpen && isClient, onClose, layerRef);

  // 포털은 document가 있어야 하므로 서버 렌더링에서는 그리지 않는다
  if (!isOpen || !isClient) return null;

  return createPortal(
    <div ref={layerRef} className={styles.layer}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className={styles.drawer}
      >
        <SubHeader title={title} titleAs="h2" titleId={titleId} onBack={onClose} />
        <div className={styles.content}>{children}</div>
      </div>
    </div>,
    document.body,
  );
}
