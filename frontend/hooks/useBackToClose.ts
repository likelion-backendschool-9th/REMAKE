"use client";

import { useEffect, useRef } from "react";

// blueprint §7.1: 시트·모달·드로어는 URL 기록을 남기지 않고, 뒤로 가기(안드로이드 뒤로·iOS 스와이프)는 열린 시트만 닫는다.
// - Chromium(TWA 포함): CloseWatcher로 닫기 요청을 받는다(Esc 포함, 겹치면 맨 위만).
// - 그 외(Safari·Firefox): 열 때 URL을 바꾸지 않는 기록을 하나 넣고 popstate에서 닫는다. Esc는 keydown으로 받는다.
//   닫기 버튼 등으로 닫히면 넣어 둔 기록을 history.back()으로 지운다. 단 지금 기록이 그 기록일 때만:
//   시트 안에서 다른 화면으로 이동했다면 back()이 원래 화면으로 되돌려 놓기 때문이다.
//   시트 안에서 이동할 때는 시트를 먼저 닫지 말고 router.replace(또는 <Link replace>)로 이동한다.
//   그러면 넣어 둔 기록이 새 화면으로 바뀌어, 뒤로 가기가 시트를 연 화면으로 바로 간다.

type CloseWatcherLike = { onclose: (() => void) | null; destroy: () => void };
type CloseWatcherCtor = new () => CloseWatcherLike;

function getCloseWatcher(): CloseWatcherCtor | undefined {
  return (globalThis as { CloseWatcher?: CloseWatcherCtor }).CloseWatcher;
}

// history 대체 방식에서 겹친 시트 중 맨 위만 닫기 위한 스택
const stack: Array<() => void> = [];
let ignorePops = 0;

// 넣어 둔 기록을 알아보는 표시. history.state에 넣는다(Next 라우터 상태는 그대로 둔다).
const ENTRY_KEY = "__remakeLayer";
let nextEntryId = 0;

function isCurrentEntry(id: number): boolean {
  const state: unknown = window.history.state;
  return (
    typeof state === "object" &&
    state !== null &&
    (state as Record<string, unknown>)[ENTRY_KEY] === id
  );
}

function handlePopState() {
  if (ignorePops > 0) {
    ignorePops -= 1;
    return;
  }
  stack[stack.length - 1]?.();
}

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

    let closedByBack = false;
    const handler = () => {
      closedByBack = true;
      onCloseRef.current();
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && stack[stack.length - 1] === handler) onCloseRef.current();
    };

    if (stack.length === 0) window.addEventListener("popstate", handlePopState);
    stack.push(handler);
    const entryId = (nextEntryId += 1);
    const currentState: unknown = window.history.state;
    window.history.pushState(
      { ...(typeof currentState === "object" ? currentState : {}), [ENTRY_KEY]: entryId },
      "",
    );
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      const index = stack.indexOf(handler);
      if (index >= 0) stack.splice(index, 1);
      if (!closedByBack && isCurrentEntry(entryId)) {
        // 닫기 버튼 등으로 닫힘: 넣어 둔 기록을 지운다. 이때 생기는 popstate는 무시한다.
        // 지금 기록이 다르면(시트 안에서 이동함) 건드리지 않는다.
        ignorePops += 1;
        window.history.back();
      }
      if (stack.length === 0 && ignorePops === 0) {
        window.removeEventListener("popstate", handlePopState);
      }
    };
  }, [isOpen]);
}
