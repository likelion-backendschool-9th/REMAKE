"use client";

import { type RefObject, useEffect } from "react";

import { useBackToClose } from "./useBackToClose";

// blueprint §6.2: 열린 모달·시트로 포커스를 옮기고, 뒤쪽 내용은 inert로 막고, 닫으면 연 버튼으로 포커스를 돌려준다.
// 레이어는 document.body 바로 아래(포털)에 있어야 한다. 겹쳐 열면 뒤쪽 레이어도 inert가 된다.
// data-dialog-ignore가 있는 body 바로 아래 요소는 inert로 만들지 않는다. 모달 위에서도 읽혀야 하는
// 공통 토스트 영역(aria-live)에 붙인다. inert 안의 aria-live는 스크린 리더가 읽지 않는다.
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
      if (
        el instanceof HTMLElement &&
        el !== layer &&
        !el.inert &&
        !el.hasAttribute("data-dialog-ignore")
      ) {
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
