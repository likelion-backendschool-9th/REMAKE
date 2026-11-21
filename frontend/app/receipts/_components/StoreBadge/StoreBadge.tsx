import type { CSSProperties } from "react";

import { STORES, toStoreCode } from "./stores";

import styles from "./StoreBadge.module.css";

type StoreBadgeProps = {
  /** 백엔드 store_code. 모르는 코드나 null이면 ETC로 그린다. */
  storeCode: string | null | undefined;
  /** 한 변 길이(px). 기본 44(피그마). 글자·모서리는 같은 비율로 줄고 는다. */
  size?: number;
  /** 옆에 마트 이름이 글자로 이미 있으면 true: 스크린 리더에서 숨긴다. */
  decorative?: boolean;
  className?: string;
};

export function StoreBadge({ storeCode, size, decorative = false, className }: StoreBadgeProps) {
  const code = toStoreCode(storeCode);
  const store = STORES[code];
  const a11y = decorative
    ? ({ "aria-hidden": true } as const)
    : ({ role: "img", "aria-label": store.name } as const);
  const style = size ? ({ "--badge-size": `${size / 16}rem` } as CSSProperties) : undefined;

  return (
    <span
      className={className ? `${styles.badge} ${className}` : styles.badge}
      data-store={code}
      style={style}
      {...a11y}
    >
      <span aria-hidden="true">{store.label}</span>
    </span>
  );
}
