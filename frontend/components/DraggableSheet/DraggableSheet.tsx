"use client";

import { type PointerEvent, type ReactNode, useRef, useState } from "react";

import { cx } from "@/lib/cx";

import styles from "./DraggableSheet.module.css";

type DraggableSheetProps = {
  /** 접혔을 때 보이는 높이(CSS 길이). 예: "40dvh" */
  collapsedHeight: string;
  /** 펼쳤을 때 높이(CSS 길이). 예: "85dvh" */
  expandedHeight: string;
  /** 손잡이 버튼의 스크린 리더용 이름에 쓰인다. 예: "재료 목록" */
  label: string;
  defaultExpanded?: boolean;
  children: ReactNode;
  className?: string;
};

// 이만큼보다 적게 움직이면 끌기가 아니라 누르기로 본다
const TAP_THRESHOLD = 6;

/**
 * 피그마 디자인 시스템 > 6. 겹쳐 뜨는 것 > 드래그 시트. 화면 아래에 붙어 있고 끌어 올리는 시트
 * (냉장고 재료 목록, 식단표 하루 식단). 손잡이를 끌거나 누르면 접힘·펼침이 바뀐다. 모달이 아니다.
 */
export function DraggableSheet({
  collapsedHeight,
  expandedHeight,
  label,
  defaultExpanded = false,
  children,
  className,
}: DraggableSheetProps) {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);
  const [dragOffset, setDragOffset] = useState<number | null>(null);
  const sheetRef = useRef<HTMLDivElement>(null);
  const measureRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef<{ startY: number; startOffset: number; maxOffset: number } | null>(null);

  // 접혔을 때 아래로 내려간 거리(px) = 펼친 높이 - 접힌 높이
  const getMaxOffset = () => {
    const sheet = sheetRef.current;
    const measure = measureRef.current;
    if (!sheet || !measure) return 0;
    return Math.max(0, sheet.offsetHeight - measure.offsetHeight);
  };

  const handlePointerDown = (event: PointerEvent<HTMLButtonElement>) => {
    const maxOffset = getMaxOffset();
    dragRef.current = {
      startY: event.clientY,
      startOffset: isExpanded ? 0 : maxOffset,
      maxOffset,
    };
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerMove = (event: PointerEvent<HTMLButtonElement>) => {
    const drag = dragRef.current;
    if (!drag) return;
    const moved = event.clientY - drag.startY;
    if (Math.abs(moved) < TAP_THRESHOLD && dragOffset === null) return;
    setDragOffset(Math.min(drag.maxOffset, Math.max(0, drag.startOffset + moved)));
  };

  const handlePointerUp = () => {
    const drag = dragRef.current;
    dragRef.current = null;
    if (!drag) return;
    if (dragOffset === null) {
      setIsExpanded((prev) => !prev); // 누르기
      return;
    }
    setIsExpanded(dragOffset < drag.maxOffset / 2); // 가까운 쪽으로 붙인다
    setDragOffset(null);
  };

  const transform =
    dragOffset !== null
      ? `translateY(${dragOffset}px)`
      : isExpanded
        ? "translateY(0)"
        : `translateY(calc(${expandedHeight} - ${collapsedHeight}))`;

  return (
    <>
      <div
        ref={measureRef}
        className={styles.measure}
        style={{ height: collapsedHeight }}
        aria-hidden="true"
      />
      <section
        ref={sheetRef}
        className={cx(styles.sheet, className)}
        style={{ height: expandedHeight, transform }}
        data-dragging={dragOffset !== null || undefined}
        aria-label={label}
      >
        <button
          type="button"
          className={styles.handle}
          aria-expanded={isExpanded}
          aria-label={`${label} ${isExpanded ? "접기" : "펼치기"}`}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              setIsExpanded((prev) => !prev);
            }
          }}
        >
          <span className={styles.grip} aria-hidden="true" />
        </button>
        <div className={styles.content}>{children}</div>
      </section>
    </>
  );
}
