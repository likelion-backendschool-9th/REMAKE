"use client";

import { useCallback, useEffect, useState } from "react";

/** 토스트가 보이는 시간(ms) 기본값 */
export const TOAST_VISIBLE_MS = 3000;
/** 사라지는 애니메이션 시간(ms). Toast.module.css의 toast-out과 맞춘다 */
export const TOAST_LEAVE_MS = 250;
/**
 * 사라지기 시작한 뒤 지우는 최대 대기(ms). 보통은 애니메이션이 끝날 때(Toast의 onLeft) 지운다.
 * 동작 줄이기 설정처럼 애니메이션이 없거나 프레임이 늦게 오는 기기를 위한 안전장치
 */
const LEAVE_FALLBACK_MS = TOAST_LEAVE_MS + 500;

type ToastState<T> = { value: T; isLeaving: boolean } | null;

/**
 * 잠깐 보였다 사라지는 토스트 상태. show(값)로 띄우면 visibleMs 뒤 isLeaving이 true가 되어
 * 내려가는 애니메이션이 돌고, 애니메이션이 끝나면(onLeft) 값이 지워진다. 보이는 중에 다시 show하면 시간을 처음부터 센다.
 * 예: const toast = useTimedToast<string>(); toast.show("저장했어요");
 *     {toast.value && <Toast isLeaving={toast.isLeaving} onLeft={toast.remove}>{toast.value}</Toast>}
 */
export function useTimedToast<T>(initialValue: T | null = null, visibleMs = TOAST_VISIBLE_MS) {
  const [state, setState] = useState<ToastState<T>>(
    initialValue === null ? null : { value: initialValue, isLeaving: false },
  );

  useEffect(() => {
    if (!state) return;
    const timer = window.setTimeout(
      () => setState((prev) => (prev?.isLeaving ? null : prev && { ...prev, isLeaving: true })),
      state.isLeaving ? LEAVE_FALLBACK_MS : visibleMs,
    );
    return () => window.clearTimeout(timer);
  }, [state, visibleMs]);

  const show = useCallback((value: T) => setState({ value, isLeaving: false }), []);
  /** 사라지는 중인 토스트를 지운다(Toast의 onLeft에 넘긴다) */
  const remove = useCallback(() => setState((prev) => (prev?.isLeaving ? null : prev)), []);

  return { value: state?.value ?? null, isLeaving: state?.isLeaving ?? false, show, remove };
}
