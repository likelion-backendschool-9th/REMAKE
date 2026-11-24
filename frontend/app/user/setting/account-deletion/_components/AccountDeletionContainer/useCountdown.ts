"use client";

import { useCallback, useEffect, useRef, useState } from "react";

/** 지금부터 seconds 뒤의 시각(ms) */
function deadlineAfter(seconds: number): number {
  return Date.now() + seconds * 1000;
}

function secondsLeft(deadline: number): number {
  return Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
}

/**
 * 초 단위로 줄어드는 남은 시간. restart()로 처음부터 다시 센다. 0에서 멈춘다.
 * 만료 시각을 기준으로 계산해서, 백그라운드 탭·절전으로 타이머가 늦게 불려도 시간이 틀어지지 않는다.
 * TODO: API 9 응답의 expiresAt을 받으면 서버 만료 시각으로 맞춘다
 */
export function useCountdown(initialSeconds: number) {
  const [seconds, setSeconds] = useState(initialSeconds);
  // 다시 셀 때마다 1씩 올려 effect를 다시 돌린다. 만료 시각은 effect 안에서 정한다(서버 렌더링에서 Date.now()를 쓰지 않음)
  const [round, setRound] = useState(0);
  const durationRef = useRef(initialSeconds);

  useEffect(() => {
    const deadline = deadlineAfter(durationRef.current);
    const tick = () => {
      const left = secondsLeft(deadline);
      setSeconds(left);
      return left;
    };
    const timer = window.setInterval(() => {
      if (tick() === 0) window.clearInterval(timer);
    }, 250);
    // 탭으로 돌아오면 바로 맞춘다
    const handleVisible = () => {
      if (document.visibilityState === "visible") tick();
    };
    document.addEventListener("visibilitychange", handleVisible);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", handleVisible);
    };
  }, [round]);

  const restart = useCallback((nextSeconds: number) => {
    durationRef.current = nextSeconds;
    setSeconds(nextSeconds);
    setRound((prev) => prev + 1);
  }, []);

  return { seconds, restart };
}

/** 299 → "04:59" */
export function formatCountdown(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(rest).padStart(2, "0")}`;
}
