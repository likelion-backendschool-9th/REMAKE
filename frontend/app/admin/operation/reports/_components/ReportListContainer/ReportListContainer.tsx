"use client";

import Link from "next/link";

import { GalleryIcon } from "@solar-icons/react/linear";

import { Badge } from "@/components/Badge/Badge";
import { Chip } from "@/components/Chip/Chip";
import { EmptyState } from "@/components/EmptyState/EmptyState";
import { Skeleton } from "@/components/Skeleton/Skeleton";

import { AdminLoadError } from "../../../../_components/AdminLoadError/AdminLoadError";
import { AdminPageHeader } from "../../../../_components/AdminPageHeader/AdminPageHeader";
import { ADMIN_PATHS } from "../../../../_lib/admin-menu";
import { useReportTabQuery } from "../../_hooks/useReportTabQuery";
import {
  getReasonTone,
  type HandledReportItem,
  type PendingReportItem,
  REPORT_RESULT_LABEL,
  REPORT_RESULT_TONE,
  type ReportTab,
} from "../../_lib/reports";

import styles from "./ReportListContainer.module.css";

type ReportListContainerProps = {
  /** 처리 대기 글(신고 수가 많은 순). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  pending?: PendingReportItem[];
  /** 처리 완료 글(최근 순). 없으면 불러오는 중 */
  done?: HandledReportItem[];
  /** 처리 대기 칩의 숫자. 없으면 목록 길이 */
  pendingCount?: number;
  /** 불러오기 실패(a0-불러오기 실패와 같은 방식) */
  hasError?: boolean;
  /** 주소에 탭이 없을 때 처음 보일 탭 */
  initialTab?: ReportTab;
};

/**
 * a4 신고 관리. 글 단위로 묶인 신고를 처리 대기·처리 완료 탭으로 나눠 보인다. 줄을 누르면 상세(a4.1)로 간다.
 * 상태: 처리 대기(a4) / 처리 완료 / 처리할 신고 없음(빈 화면) / 불러오는 중 / 불러오기 실패.
 * 탭 값은 주소(?tab=done)에 두고, API 65가 붙으면 그대로 조회 조건으로 쓴다
 */
export function ReportListContainer({
  pending,
  done,
  pendingCount,
  hasError = false,
  initialTab = "pending",
}: ReportListContainerProps) {
  const { tab, setTab } = useReportTabQuery(initialTab);
  const waitingCount = pendingCount ?? pending?.length ?? 0;

  return (
    <>
      <AdminPageHeader
        title="신고 관리"
        description="신고가 쌓여도 자동으로 숨기지 않아요. 운영자가 확인해 처리해요."
        actions={
          <div className={styles.tabs} role="group" aria-label="처리 상태">
            <Chip isSelected={tab === "pending"} onClick={() => void setTab("pending")}>
              처리 대기 {waitingCount}
            </Chip>
            <Chip isSelected={tab === "done"} onClick={() => void setTab("done")}>
              처리 완료
            </Chip>
          </div>
        }
      />

      {hasError ? (
        <AdminLoadError title="신고 목록을 불러오지 못했어요" />
      ) : tab === "pending" ? (
        <PendingReportTable items={pending} />
      ) : (
        <DoneReportTable items={done} />
      )}
    </>
  );
}

type PendingReportTableProps = { items?: PendingReportItem[] };

/** 처리 대기 표: 공유 레시피 · 신고 수 · 신고 사유 · 최근 신고 · 작성자 확정 위반 · 상태 */
function PendingReportTable({ items }: PendingReportTableProps) {
  if (!items) return <ListSkeleton label="처리 대기 신고 목록" />;

  return (
    <section className={styles.card} aria-label="처리 대기 신고 목록">
      {items.length === 0 ? (
        <div className={styles.empty}>
          <EmptyState>처리할 신고가 없어요</EmptyState>
        </div>
      ) : (
        <>
          <table className={`${styles.table} ${styles.pending}`}>
            <thead>
              <tr>
                <th scope="col">공유 레시피</th>
                <th scope="col" className={styles.countColumn}>
                  신고 수
                </th>
                <th scope="col" className={styles.reasonColumn}>
                  신고 사유
                </th>
                <th scope="col" className={styles.timeColumn}>
                  최근 신고
                </th>
                <th scope="col" className={styles.violationColumn}>
                  작성자 확정 위반
                </th>
                <th scope="col" className={styles.statusColumn}>
                  상태
                </th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.sharedRecipeId} className={styles.row}>
                  <td>
                    <TitleCell sharedRecipeId={item.sharedRecipeId} title={item.title} />
                  </td>
                  <td className={styles.numeric}>{item.reportCount}건</td>
                  <td>
                    <div className={styles.badges}>
                      {item.reasons.map((reason) => (
                        <Badge key={reason.label} tone={getReasonTone(reason.label)}>
                          {reason.label} {reason.count}
                        </Badge>
                      ))}
                    </div>
                  </td>
                  <td>{item.latestReportedAt}</td>
                  <td className={styles.numeric}>{item.authorViolationCount}회</td>
                  <td>
                    <Badge tone="warning">처리 대기</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className={styles.footnote}>신고 수가 많은 순 · 같은 글 신고는 한 줄로 묶어요</p>
        </>
      )}
    </section>
  );
}

type DoneReportTableProps = { items?: HandledReportItem[] };

/** 처리 완료 표: 공유 레시피 · 신고 수 · 처리 결과 · 적용한 제재 · 처리 시각 · 처리한 운영자 */
function DoneReportTable({ items }: DoneReportTableProps) {
  if (!items) return <ListSkeleton label="처리 완료 신고 목록" />;

  return (
    <section className={styles.card} aria-label="처리 완료 신고 목록">
      {items.length === 0 ? (
        <div className={styles.empty}>
          <EmptyState>처리한 신고가 없어요</EmptyState>
        </div>
      ) : (
        <table className={`${styles.table} ${styles.done}`}>
          <thead>
            <tr>
              <th scope="col">공유 레시피</th>
              <th scope="col" className={styles.countColumn}>
                신고 수
              </th>
              <th scope="col" className={styles.resultColumn}>
                처리 결과
              </th>
              <th scope="col" className={styles.sanctionColumn}>
                적용한 제재
              </th>
              <th scope="col" className={styles.timeColumn}>
                처리 시각
              </th>
              <th scope="col" className={styles.operatorColumn}>
                처리한 운영자
              </th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.sharedRecipeId} className={styles.row}>
                <td>
                  <TitleCell sharedRecipeId={item.sharedRecipeId} title={item.title} />
                </td>
                <td className={styles.numeric}>{item.reportCount}건</td>
                <td>
                  <Badge tone={REPORT_RESULT_TONE[item.result]}>
                    {REPORT_RESULT_LABEL[item.result]}
                  </Badge>
                </td>
                <td>{item.appliedSanction ?? "-"}</td>
                <td>{item.handledAt}</td>
                <td>{item.operator}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

/** 글 제목 칸. 칸 전체를 눌러 상세로 간다(stretched link). 키보드는 제목 링크로 고른다 */
function TitleCell({ sharedRecipeId, title }: { sharedRecipeId: number; title: string }) {
  return (
    <div className={styles.titleCell}>
      <span className={styles.thumb} aria-hidden="true">
        <GalleryIcon className={styles.thumbIcon} />
      </span>
      <Link href={`${ADMIN_PATHS.reports}/${sharedRecipeId}`} className={styles.titleLink}>
        {title}
      </Link>
    </div>
  );
}

/** 불러오는 중(피그마 없음): 표 줄 자리 */
function ListSkeleton({ label }: { label: string }) {
  return (
    <section className={styles.skeleton} aria-busy="true" aria-label={label}>
      {Array.from({ length: 6 }, (_, index) => (
        <Skeleton key={index} width="100%" height="1.25rem" />
      ))}
    </section>
  );
}
