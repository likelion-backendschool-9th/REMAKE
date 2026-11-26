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
  /** 헤더에 제목을 보이지 않는다(본문에 질문 제목이 있는 드로워, 피그마 p4.1.2 알레르기 재료). 제목은 스크린 리더 이름으로만 쓴다 */
  hideTitle?: boolean;
  /** 본문 모양. 없으면 약관 글 모양(Remake/title·줄바꿈 그대로) */
  contentClassName?: string;
};

/**
 * 피그마 common-약관드로워. 화면 전체를 덮는 드로워: 서브 헤더(← + 제목) + 스크롤되는 내용.
 * 열림 상태는 화면의 local state로 둔다(URL에 넣지 않음). ←·뒤로 가기·Esc로 닫힌다(blueprint §7.1).
 */
export function Drawer({
  isOpen,
  onClose,
  title,
  children,
  hideTitle = false,
  contentClassName,
}: DrawerProps) {
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
        aria-labelledby={hideTitle ? undefined : titleId}
        aria-label={hideTitle ? title : undefined}
        tabIndex={-1}
        className={styles.drawer}
      >
        <SubHeader
          title={hideTitle ? undefined : title}
          titleAs="h2"
          titleId={titleId}
          onBack={onClose}
        />
        <div className={contentClassName ?? styles.content}>{children}</div>
      </div>
    </div>,
    document.body,
  );
}
