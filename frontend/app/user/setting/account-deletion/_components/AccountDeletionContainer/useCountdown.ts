"use client";

import { useCallback, useEffect, useState } from "react";

/** 초 단위로 줄어드는 남은 시간. restart()로 처음부터 다시 센다. 0에서 멈춘다. */
export function useCountdown(initialSeconds: number) {
  const [seconds, setSeconds] = useState(initialSeconds);
  const [round, setRound] = useState(0);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setSeconds((prev) => {
        if (prev <= 1) {
          window.clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => window.clearInterval(timer);
  }, [round]);

  const restart = useCallback((nextSeconds: number) => {
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
