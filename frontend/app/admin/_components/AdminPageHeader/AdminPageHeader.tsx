import type { ReactNode } from "react";

import styles from "./AdminPageHeader.module.css";

type AdminPageHeaderProps = {
  title: string;
  description?: string;
  /** 오른쪽 끝에 둘 것(기간 칩 등) */
  actions?: ReactNode;
};

/** 운영자 화면 본문 맨 위 페이지 제목. 왼쪽에 제목·설명, 오른쪽에 기간 칩 같은 동작을 둔다 */
export function AdminPageHeader({ title, description, actions }: AdminPageHeaderProps) {
  return (
    <header className={styles.header}>
      <div className={styles.text}>
        <h1 className={styles.title}>{title}</h1>
        {description && <p className={styles.description}>{description}</p>}
      </div>
      {actions && <div className={styles.actions}>{actions}</div>}
    </header>
  );
}
