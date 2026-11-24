import type { CSSProperties } from "react";

import { cx } from "@/lib/cx";

import styles from "./Skeleton.module.css";

type SkeletonProps = {
  /** CSS 길이. 기본 120px(피그마) */
  width?: string;
  /** CSS 길이. 기본 16px(피그마) */
  height?: string;
  /** 사진 자리처럼 모서리를 바꿀 때. 기본 radius/xs */
  radius?: string;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 7. 목록 행·카드 > 스켈레톤 블록. 불러오는 동안 글·사진 자리. 크기는 자유롭게 늘려 쓴다.
 * 스크린 리더에는 숨기므로, 불러오는 영역에 aria-busy="true"를 함께 건다(blueprint §6.3).
 */
export function Skeleton({ width, height, radius, className }: SkeletonProps) {
  const style = {
    "--skeleton-width": width,
    "--skeleton-height": height,
    "--skeleton-radius": radius,
  } as CSSProperties;
  return <span className={cx(styles.skeleton, className)} style={style} aria-hidden="true" />;
}
