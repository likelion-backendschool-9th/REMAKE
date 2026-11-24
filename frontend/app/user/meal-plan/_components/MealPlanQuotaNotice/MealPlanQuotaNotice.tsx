import styles from "./MealPlanQuotaNotice.module.css";

/** "이번 달에 남은 식단표 생성 N회"(N회는 text/warning). 식단표 빈 화면·추가 시트·생성 화면 버튼 위(domain-rules 5) */
export function MealPlanQuotaNotice({ remaining }: { remaining: number }) {
  return (
    <>
      이번 달에 남은 식단표 생성 <span className={styles.count}>{remaining}회</span>
    </>
  );
}
