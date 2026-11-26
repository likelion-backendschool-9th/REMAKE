import { ResultState } from "@/components/ResultState/ResultState";
import { SmallButton } from "@/components/SmallButton/SmallButton";

import { AdminCard } from "../AdminCard/AdminCard";

import styles from "./AdminLoadError.module.css";

type AdminLoadErrorProps = {
  /** 예: "지표를 불러오지 못했어요" */
  title: string;
  onRetry?: () => void;
};

/**
 * a0-불러오기 실패. 운영자 목록·지표 화면이 데이터를 못 불러오면 본문 자리에 이 카드를 같은 방식으로 보인다.
 * 페이지 제목은 그대로 두고 그 아래에 둔다
 */
export function AdminLoadError({ title, onRetry }: AdminLoadErrorProps) {
  return (
    <AdminCard className={styles.card}>
      <div className={styles.result}>
        <ResultState variant="error" title={title} description="잠시 후 다시 시도해 주세요." />
      </div>
      <SmallButton tone="outline" onClick={onRetry}>
        다시 시도하기
      </SmallButton>
    </AdminCard>
  );
}
