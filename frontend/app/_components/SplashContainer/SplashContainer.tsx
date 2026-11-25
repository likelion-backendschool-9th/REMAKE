"use client";

import Image from "next/image";

import styles from "./SplashContainer.module.css";

/** 미리보기에서 멈춰 볼 장면. start=피그마 스플래쉬 start, end=스플래쉬 end */
export type SplashPhase = "start" | "end";

type SplashContainerProps = {
  /** 애니메이션 없이 이 장면에 멈춘다. 미리보기용 */
  stillPhase?: SplashPhase;
};

/**
 * 스플래시(/). 피그마 스플래쉬 start(5782-4629) → end(5782-4666)를 이어 움직인다.
 * 파란 바탕 가운데 흰 원 안에 로고 그림 → 흰 원이 커져 화면을 덮고, 아래 "리메이크"·슬로건이 흰 글자에서 진한 글자로,
 * 슬로건 끝에 "!"가 나타난다(약 1.2초). 움직임 줄이기 설정이면 처음부터 end 장면.
 * TODO(#95): 애니메이션이 끝나면 로그인 여부를 확인해 로그인(/public/auth/login)·온보딩·냉장고(/user/fridge/ingredients)로
 * 이동한다. 로그인 쿠키는 HttpOnly라 화면에서 읽지 못하므로 API로 확인한다.
 */
export function SplashContainer({ stillPhase }: SplashContainerProps) {
  return (
    <main className={styles.splash} data-phase={stillPhase ?? "play"}>
      <div className={styles.circle} aria-hidden="true" />
      <Image
        src="/images/logo-mark.png"
        alt=""
        width={216}
        height={128}
        priority
        className={styles.mark}
      />
      <div className={styles.brand}>
        <h1 className={styles.name}>리메이크</h1>
        <p className={styles.slogan}>
          당신에게 딱 맞춘 AI 식생활 비서
          {/* 끝 장면에만 보이는 느낌표. 자리는 처음부터 잡아 둬서 나타날 때 글자가 옆으로 밀리지 않는다 */}
          <span className={styles.bang}>!</span>
        </p>
      </div>
    </main>
  );
}
