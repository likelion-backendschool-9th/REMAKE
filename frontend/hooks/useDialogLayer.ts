"use client";

import { type RefObject, useEffect } from "react";

import { useBackToClose } from "./useBackToClose";

// blueprint §6.2: 열린 모달·시트로 포커스를 옮기고, 뒤쪽 내용은 inert로 막고, 닫으면 연 버튼으로 포커스를 돌려준다.
// 레이어는 document.body 바로 아래(포털)에 있어야 한다. 겹쳐 열면 뒤쪽 레이어도 inert가 된다.
export function useDialogLayer(
  isOpen: boolean,
  onClose: () => void,
  layerRef: RefObject<HTMLElement | null>,
) {
  useBackToClose(isOpen, onClose);

  useEffect(() => {
    const layer = layerRef.current;
    if (!isOpen || !layer) return;

    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const madeInert: HTMLElement[] = [];
    for (const el of Array.from(document.body.children)) {
      if (el instanceof HTMLElement && el !== layer && !el.inert) {
        el.inert = true;
        madeInert.push(el);
      }
    }

    const target =
      layer.querySelector<HTMLElement>("[data-autofocus]") ??
      layer.querySelector<HTMLElement>('[role="dialog"]');
    target?.focus();

    return () => {
      for (const el of madeInert) el.inert = false;
      opener?.focus();
    };
  }, [isOpen, layerRef]);
}
