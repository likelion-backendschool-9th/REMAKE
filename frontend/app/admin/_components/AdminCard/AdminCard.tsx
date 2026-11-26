import type { HTMLAttributes, ReactNode } from "react";

import { cx } from "@/lib/cx";

import styles from "./AdminCard.module.css";

type AdminCardProps = {
  /** 카드 제목(Remake/subheader). 있으면 카드 맨 위 왼쪽에 둔다 */
  title?: string;
  /** 제목 오른쪽 작은 글(지표 번호·계산식 등) */
  meta?: ReactNode;
  className?: string;
  children?: ReactNode;
} & Omit<HTMLAttributes<HTMLElement>, "className" | "children" | "title">;

/**
 * 운영자 화면의 흰 카드: bg/surface, 선 border/default, radius/lg, 여백 20/24, 내용 간격 16.
 * 대시보드·퍼널·이벤트 로그 등 모든 운영자 화면에서 같은 카드를 쓴다
 */
export function AdminCard({ title, meta, className, children, ...rest }: AdminCardProps) {
  return (
    <section className={cx(styles.card, className)} {...rest}>
      {(title || meta) && (
        <div className={styles.header}>
          {title && <h2 className={styles.title}>{title}</h2>}
          {meta && <p className={styles.meta}>{meta}</p>}
        </div>
      )}
      {children}
    </section>
  );
}
