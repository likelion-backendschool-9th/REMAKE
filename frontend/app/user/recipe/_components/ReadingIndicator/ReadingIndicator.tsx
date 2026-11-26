import type { CSSProperties } from "react";

import styles from "./ReadingIndicator.module.css";

/**
 * 소리 막대 5개. rest는 피그마 정지 화면 높이(px)로, 움직임 줄이기 설정이면 이 모양으로 멈춘다.
 * max는 움직일 때 가장 높은 높이(px, 가운데가 높게). 막대마다 주기를 다르게 하고 시작 지점(음수 지연)을
 * 어긋나게 해서 같은 모양이 반복돼 보이지 않게 한다(말소리처럼 제각각 오르내림).
 */
const BARS = [
  { rest: 18, max: 22, duration: 3.45, delay: -1.05 },
  { rest: 12, max: 28, duration: 2.7, delay: -2.4 },
  { rest: 22, max: 32, duration: 4.05, delay: -0.45 },
  { rest: 28, max: 28, duration: 3.15, delay: -1.8 },
  { rest: 10, max: 22, duration: 3.75, delay: -2.85 },
];

const toRem = (px: number) => `${px / 16}rem`;

/**
 * 피그마 p4.1.1-레시피 상세-레시피 재생: 만드는 방법을 읽는 동안 화면 아래 가운데 소리 막대(60 동그라미).
 * 아래 버튼 자리를 대신한다. 누르면 읽기를 멈춘다.
 */
export function ReadingIndicator({ onStop }: { onStop: () => void }) {
  return (
    <div className={styles.bar}>
      <button type="button" className={styles.button} onClick={onStop} aria-label="읽기 중단하기">
        {BARS.map((bar, index) => (
          <span
            key={index}
            className={styles.wave}
            style={
              {
                "--wave-rest": toRem(bar.rest),
                "--wave-max": toRem(bar.max),
                animationDuration: `${bar.duration}s`,
                animationDelay: `${bar.delay}s`,
              } as CSSProperties
            }
            aria-hidden="true"
          />
        ))}
      </button>
    </div>
  );
}
