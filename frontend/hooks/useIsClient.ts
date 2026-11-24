"use client";

import { useSyncExternalStore } from "react";

const subscribe = () => () => {};

/**
 * 브라우저에서 그리는 중이면 true. 서버 렌더링과 하이드레이션 첫 렌더에서는 false.
 * document가 필요한 포털(모달·시트)을 서버에서 그리지 않을 때 쓴다(하이드레이션 불일치 없음).
 */
export function useIsClient(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => true,
    () => false,
  );
}
