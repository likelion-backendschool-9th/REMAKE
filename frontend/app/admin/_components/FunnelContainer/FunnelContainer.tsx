"use client";

import { useState } from "react";

import { Skeleton } from "@/components/Skeleton/Skeleton";

import type { FunnelData } from "../../_lib/funnel";
import type { AdminPeriod } from "../../_lib/period";
import { AdminCard } from "../AdminCard/AdminCard";
import { AdminLoadError } from "../AdminLoadError/AdminLoadError";
import { AdminPageHeader } from "../AdminPageHeader/AdminPageHeader";
import { AdminPeriodFilter } from "../AdminPeriodFilter/AdminPeriodFilter";
import { FunnelChart } from "../FunnelChart/FunnelChart";

import styles from "./FunnelContainer.module.css";

type FunnelContainerProps = {
  /** 퍼널 값. 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  funnel?: FunnelData;
  /** 불러오기 실패(a0-불러오기 실패와 같은 방식) */
  hasError?: boolean;
  initialPeriod?: AdminPeriod;
};

/**
 * a2 퍼널(M17). 핵심 흐름(로그인 → 요리 완료)과 영수증 경로(영수증 스캔 → 재료 등록)에서
 * 어느 단계에서 이탈하는지 본다. 불러오는 중 화면은 피그마에 없어 카드 자리에 스켈레톤을 둔다
 */
export function FunnelContainer({
  funnel,
  hasError = false,
  initialPeriod = "last7days",
}: FunnelContainerProps) {
  const [period, setPeriod] = useState<AdminPeriod>(initialPeriod);
  const [isRealOnly, setIsRealOnly] = useState(true);

  return (
    <>
      <AdminPageHeader
        title="퍼널"
        description="핵심 흐름에서 어느 단계에서 이탈하는지 봐요(M17)."
        actions={
          <AdminPeriodFilter
            period={period}
            onPeriodChange={setPeriod}
            isRealOnly={isRealOnly}
            onRealOnlyChange={setIsRealOnly}
          />
        }
      />
      {hasError ? (
        <AdminLoadError title="퍼널을 불러오지 못했어요" />
      ) : !funnel ? (
        <div className={styles.cards} aria-busy="true">
          <FunnelSkeletonCard rows={6} />
          <FunnelSkeletonCard rows={3} />
        </div>
      ) : (
        <div className={styles.cards}>
          <AdminCard title="핵심 흐름" meta="로그인 → 요리 완료">
            <FunnelChart steps={funnel.core} />
          </AdminCard>
          <AdminCard title="영수증 경로" meta="영수증 스캔 → 재료 등록">
            <FunnelChart steps={funnel.receipt} />
          </AdminCard>
        </div>
      )}
    </>
  );
}

/** 불러오는 중(피그마 없음): 제목과 단계 줄 자리 */
function FunnelSkeletonCard({ rows }: { rows: number }) {
  return (
    <div className={styles.skeletonCard}>
      <Skeleton width="7.5rem" height="1.125rem" />
      {Array.from({ length: rows }, (_, index) => (
        <Skeleton key={index} width="100%" height="1.75rem" />
      ))}
    </div>
  );
}
