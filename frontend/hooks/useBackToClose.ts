"use client";

import { useEffect, useRef } from "react";

// blueprint §7.1: 시트·모달은 방문 기록(history)에 쌓지 않는다.
// - CloseWatcher가 있는 브라우저(Chromium, TWA 포함): 안드로이드 뒤로 버튼·Esc를 닫기 요청으로 받아
//   열린 시트만 닫는다(화면은 그대로). 기록을 쓰지 않는 브라우저 기능이다. 겹치면 맨 위만 닫힌다.
// - 그 외(Safari·Firefox): Esc만 받는다. 뒤로 가기는 평소처럼 이전 화면으로 가고,
//   시트는 화면이 숨겨질 때 닫힌다(useDialogLayer).

type CloseWatcherLike = { onclose: (() => void) | null; destroy: () => void };
type CloseWatcherCtor = new () => CloseWatcherLike;

function getCloseWatcher(): CloseWatcherCtor | undefined {
  return (globalThis as { CloseWatcher?: CloseWatcherCtor }).CloseWatcher;
}

// CloseWatcher가 없을 때 겹친 시트 중 맨 위만 Esc로 닫기 위한 스택
const stack: Array<() => void> = [];

export function useBackToClose(isOpen: boolean, onClose: () => void) {
  const onCloseRef = useRef(onClose);

  useEffect(() => {
    onCloseRef.current = onClose;
  });

  useEffect(() => {
    if (!isOpen) return;

    const CloseWatcher = getCloseWatcher();
    if (CloseWatcher) {
      const watcher = new CloseWatcher();
      watcher.onclose = () => onCloseRef.current();
      return () => watcher.destroy();
    }

    const close = () => onCloseRef.current();
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && stack[stack.length - 1] === close) close();
    };
    stack.push(close);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      const index = stack.indexOf(close);
      if (index >= 0) stack.splice(index, 1);
    };
  }, [isOpen]);
}
