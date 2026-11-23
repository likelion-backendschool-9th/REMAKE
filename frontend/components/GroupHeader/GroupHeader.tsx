import { cx } from "@/lib/cx";

import styles from "./GroupHeader.module.css";

type GroupHeaderProps = {
  /** 예: "11월 4주차", "오늘" */
  title: string;
  /** 제목 옆 보조 글자. 예: "11월 22일~11월 29일" */
  description?: string;
  /** 오른쪽 끝 글자. 예: "149,500원" */
  trailing?: string;
  /** 화면의 제목 단계에 맞춰 고른다(blueprint §6.4). 기본 h2 */
  as?: "h2" | "h3" | "h4";
  className?: string;
};

/** 피그마 디자인 시스템 > 7. 목록 행·카드 > 묶음 제목. 목록을 날짜·주차로 묶는 제목(알림 오늘·1일 전, 영수증 주차). */
export function GroupHeader({
  title,
  description,
  trailing,
  as: Heading = "h2",
  className,
}: GroupHeaderProps) {
  return (
    <div className={cx(styles.header, className)}>
      <Heading className={styles.title}>{title}</Heading>
      <span className={styles.description}>{description}</span>
      {trailing && <span className={styles.trailing}>{trailing}</span>}
    </div>
  );
}
