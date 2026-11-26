"use client";

import { type RefObject, useEffect, useRef } from "react";

import { setPageScrollLocked } from "@/lib/page-scrollbar";

import { useBackToClose } from "./useBackToClose";

// blueprint §6.2: 열린 모달·시트로 포커스를 옮기고, 뒤쪽 내용은 inert로 막고, 닫으면 연 버튼으로 포커스를 돌려준다.
// 레이어는 document.body 바로 아래(포털)에 있어야 한다. 겹쳐 열면 뒤쪽 레이어도 inert가 된다.
// data-dialog-ignore가 있는 body 바로 아래 요소는 inert로 만들지 않는다. 모달 위에서도 읽혀야 하는
// 공통 토스트 영역(aria-live)에 붙인다. inert 안의 aria-live는 스크린 리더가 읽지 않는다.

// 열린 레이어 수. 하나라도 열려 있으면 뒤 화면 스크롤을 막는다.
// 막는 방법은 lib/page-scrollbar: 겹쳐 그리는 스크롤바라 막아도 화면 폭이 바뀌지 않는다(가운데 화면이 움직이지 않음).
let openLayers = 0;

function lockScroll() {
  if (openLayers === 0) setPageScrollLocked(true);
  openLayers += 1;
}

function unlockScroll() {
  openLayers -= 1;
  if (openLayers === 0) setPageScrollLocked(false);
}

export function useDialogLayer(
  isOpen: boolean,
  onClose: () => void,
  layerRef: RefObject<HTMLElement | null>,
) {
  useBackToClose(isOpen, onClose);

  const onCloseRef = useRef(onClose);
  useEffect(() => {
    onCloseRef.current = onClose;
  });

  // 열린 채로 화면이 숨겨지면 닫는다. cacheComponents라 다른 화면으로 가도 이전 화면은 상태를 가진 채
  // 숨겨지고(effect만 정리됨), 뒤로 오면 그대로 다시 보인다. 시트가 열린 채 돌아오지 않게 한다.
  // effect가 정리된 뒤 다시 실행되지 않았으면(숨겨짐·사라짐) 닫는다. 닫힘(isOpen=false)이나
  // 개발 모드 StrictMode의 정리-재실행은 바로 다시 실행되므로 닫지 않는다.
  // 그래서 onClose는 닫기 버튼·Esc 말고도 불린다. 쓰는 쪽은 onClose에 닫힌 상태만 반영하고(취소 기록 등은
  // 취소 버튼에서 따로), 확인 뒤 다른 화면으로 갈 때는 먼저 닫고 이동한다.
  const runRef = useRef(0);
  useEffect(() => {
    const run = (runRef.current += 1);
    if (!isOpen) return;
    return () => {
      window.setTimeout(() => {
        if (runRef.current === run) onCloseRef.current();
      }, 0);
    };
  }, [isOpen]);

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
    lockScroll();

    const target =
      layer.querySelector<HTMLElement>("[data-autofocus]") ??
      layer.querySelector<HTMLElement>('[role="dialog"]');
    target?.focus();

    return () => {
      unlockScroll();
      for (const el of madeInert) el.inert = false;
      opener?.focus();
    };
  }, [isOpen, layerRef]);
}
