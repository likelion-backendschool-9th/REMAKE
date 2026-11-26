"use client";

import { useEffect, useState } from "react";

/** 기본으로 기다리는 시간(ms). 글자를 치는 동안은 기다리고, 멈춘 뒤 0.3초에 한 번 반영한다 */
export const DEFAULT_DEBOUNCE_MS = 300;

/**
 * 값이 바뀐 뒤 delayMs 동안 다시 바뀌지 않으면 그 값을 돌려준다(디바운스). 그 전에는 마지막으로 반영한 값을 돌려준다.
 * 입력값으로 서버에 요청할 때(검색 API의 queryKey 등) 글자마다 요청하지 않게 쓴다. 처음 값은 기다리지 않고 바로 돌려준다.
 * 예: const debouncedKeyword = useDebounce(keyword);
 */
export function useDebounce<T>(value: T, delayMs: number = DEFAULT_DEBOUNCE_MS): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delayMs);
    return () => window.clearTimeout(timer);
  }, [value, delayMs]);

  return debounced;
}
