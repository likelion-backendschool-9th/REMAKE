"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import type { RecipeStep } from "../_lib/recipe";

function getSynth(): SpeechSynthesis | null {
  return typeof window !== "undefined" && "speechSynthesis" in window
    ? window.speechSynthesis
    : null;
}

/**
 * 만드는 방법 읽어 주기(domain-rules 4). "재생하기"를 누르면 기기 음성(speechSynthesis)으로 단계를 차례로 읽고,
 * 지금 읽는 단계 번호를 돌려준다. 다 읽거나 "중단하기"를 누르면 멈춘다. 화면을 떠나거나 숨겨지면 멈춘다.
 * initialStepNo는 미리보기에서 재생 중 모양을 그릴 때만 넘긴다(소리는 내지 않음).
 */
export function useStepReader(steps: readonly RecipeStep[] | undefined, initialStepNo?: number) {
  const [playingStepNo, setPlayingStepNo] = useState<number | null>(initialStepNo ?? null);
  const cancelledRef = useRef(false);
  // 실제로 소리를 내기 시작했는지. 미리보기 모양(initialStepNo)은 정리 함수에서 지우지 않는다
  const isSpeakingRef = useRef(false);

  const stop = useCallback(() => {
    cancelledRef.current = true;
    isSpeakingRef.current = false;
    getSynth()?.cancel();
    setPlayingStepNo(null);
  }, []);

  const play = useCallback(() => {
    const synth = getSynth();
    // TODO: 음성을 못 쓰는 기기에서 알릴지(토스트 문구) 정해지면 넣는다
    if (!synth || !steps?.length) return;
    synth.cancel();
    cancelledRef.current = false;
    isSpeakingRef.current = true;

    const speak = (index: number) => {
      const step = steps[index];
      if (!step || cancelledRef.current) {
        isSpeakingRef.current = false;
        setPlayingStepNo(null);
        return;
      }
      setPlayingStepNo(step.stepNo);
      const utterance = new SpeechSynthesisUtterance(step.content);
      utterance.lang = "ko-KR";
      utterance.onend = () => speak(index + 1);
      utterance.onerror = () => setPlayingStepNo(null);
      synth.speak(utterance);
    };
    speak(0);
  }, [steps]);

  // 화면을 떠나거나(cacheComponents로) 숨겨지면 읽기를 멈춘다
  useEffect(
    () => () => {
      if (!isSpeakingRef.current) return;
      cancelledRef.current = true;
      isSpeakingRef.current = false;
      getSynth()?.cancel();
      setPlayingStepNo(null);
    },
    [],
  );

  return { playingStepNo, play, stop };
}
