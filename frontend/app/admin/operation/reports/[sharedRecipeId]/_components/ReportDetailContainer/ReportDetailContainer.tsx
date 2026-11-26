"use client";

import { useState } from "react";
import Link from "next/link";

import { ArrowLeftIcon } from "@solar-icons/react/linear";

import { Badge } from "@/components/Badge/Badge";
import { Button } from "@/components/Button/Button";
import { ConfirmModal } from "@/components/ConfirmModal/ConfirmModal";
import { InfoBox } from "@/components/InfoBox/InfoBox";
import { TextButton } from "@/components/TextButton/TextButton";
import { Toast } from "@/components/Toast/Toast";
import { useTimedToast } from "@/hooks/useTimedToast";

import { AdminCard } from "../../../../../_components/AdminCard/AdminCard";
import { AdminLoadError } from "../../../../../_components/AdminLoadError/AdminLoadError";
import { AdminPageHeader } from "../../../../../_components/AdminPageHeader/AdminPageHeader";
import { ADMIN_PATHS } from "../../../../../_lib/admin-menu";
import {
  getActionCopy,
  getHandledDescription,
  getNextSanctionNotice,
  getReasonTone,
  REPORT_RESULT_LABEL,
  REPORT_RESULT_TONE,
  type ReportAction,
  type ReportDetail,
  summarizeReasons,
} from "../../../_lib/reports";

import styles from "./ReportDetailContainer.module.css";

type ToastMessage = { variant: "success" | "error"; message: string };

/** 처리 뒤 토스트 문구(a4.1). 처리 완료는 성공, 이미 처리됨은 오류 토스트로 보인다 */
const RESULT_TOAST: Record<ReportAction, ToastMessage> = {
  VIOLATION: { variant: "success", message: "위반으로 처리했어요" },
  NO_ISSUE: { variant: "success", message: "문제 없음으로 처리했어요" },
  DELETED: { variant: "success", message: "글을 삭제했어요" },
};

const ALREADY_HANDLED_TOAST: ToastMessage = {
  variant: "error",
  message: "다른 운영자가 이미 처리한 신고예요. 목록을 새로 불러올게요",
};

type ReportDetailContainerProps = {
  /** 신고 상세. 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  report?: ReportDetail;
  /** 불러오기 실패 */
  hasError?: boolean;
  /** 미리보기에서 확인창을 바로 열 때 쓴다(a4.1 위반 확인·문제 없음 확인·글 삭제 확인) */
  initialAction?: ReportAction;
  /** 미리보기에서 토스트를 바로 띄울 때 쓴다(처리 완료 / 이미 처리됨) */
  initialToast?: "handled" | "alreadyHandled";
};

/**
 * a4.1 신고 상세. 왼쪽은 글 내용, 오른쪽은 신고 내역과 작성자 제재 상태.
 * 처리 대기면 위반(숨김)·문제 없음·글 삭제 중 하나를 확인창을 거쳐 고르고, 처리 끝난 글은 처리 결과만 보인다.
 * 처리 API(67)는 아직 붙이지 않아 확인 뒤에는 토스트만 띄운다
 */
export function ReportDetailContainer({
  report,
  hasError = false,
  initialAction,
  initialToast,
}: ReportDetailContainerProps) {
  const [action, setAction] = useState<ReportAction | null>(initialAction ?? null);
  const toast = useTimedToast<ToastMessage>(
    initialToast === "handled"
      ? RESULT_TOAST.VIOLATION
      : initialToast === "alreadyHandled"
        ? ALREADY_HANDLED_TOAST
        : null,
  );

  const handleConfirm = () => {
    if (!action) return;
    setAction(null);
    toast.show(RESULT_TOAST[action]);
  };

  return (
    <>
      <Link href={ADMIN_PATHS.reports} className={styles.back}>
        <ArrowLeftIcon className={styles.backIcon} aria-hidden="true" />
        신고 목록
      </Link>

      {hasError ? (
        <AdminLoadError title="신고 내용을 불러오지 못했어요" />
      ) : !report ? (
        <DetailSkeleton />
      ) : (
        <>
          <AdminPageHeader title="신고 상세" />

          <div className={styles.body}>
            <RecipeCard report={report} />

            <div className={styles.side}>
              <ReportListCard report={report} />
              <AuthorCard report={report} onChoose={(next) => setAction(next)} />
            </div>
          </div>

          {action && (
            <ActionConfirm
              action={action}
              violationCount={report.author.violationCount}
              onClose={() => setAction(null)}
              onConfirm={handleConfirm}
            />
          )}
        </>
      )}

      {/* 문구를 바꿔 끼우면 읽히도록 항상 그려 두는 영역(blueprint §6.3) */}
      <div className={styles.toastArea} role="status" aria-live="polite">
        {toast.value && (
          <Toast variant={toast.value.variant} isLeaving={toast.isLeaving} onLeft={toast.remove}>
            {toast.value.message}
          </Toast>
        )}
      </div>
    </>
  );
}

/** 왼쪽 카드: 글 제목, 사진 자리, 2인분·30분 이내, 재료, 조리 순서 */
function RecipeCard({ report }: { report: ReportDetail }) {
  const { recipe } = report;

  return (
    <AdminCard
      title={report.title}
      meta={`공유 ${report.sharedAt} · 저장 ${report.saveCount}`}
      className={styles.recipe}
    >
      <div className={styles.photo} aria-hidden="true" />

      <p className={styles.servings}>
        {recipe.servings} · {recipe.cookingTime}
      </p>

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>재료</h3>
        <ul className={styles.ingredients}>
          {recipe.ingredients.map((ingredient) => (
            <li key={ingredient.name} className={styles.ingredient}>
              {ingredient.name} <span className={styles.amount}>{ingredient.amount}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>조리 순서</h3>
        <ol className={styles.steps}>
          {recipe.steps.map((step) => (
            <li key={step} className={styles.step}>
              {step}
            </li>
          ))}
        </ol>
      </section>
    </AdminCard>
  );
}

/** 오른쪽 위 카드: 신고 사유별 건수와 신고 한 건씩(같은 글은 한 사람이 한 번만 신고) */
function ReportListCard({ report }: { report: ReportDetail }) {
  const reasons = summarizeReasons(report.reports);

  return (
    <AdminCard title={`신고 ${report.reports.length}건`} meta="같은 글은 한 사람이 한 번만">
      <div className={styles.badges}>
        {reasons.map((reason) => (
          <Badge key={reason.label} tone={getReasonTone(reason.label)}>
            {reason.label} {reason.count}
          </Badge>
        ))}
      </div>

      <ul className={styles.reportList}>
        {report.reports.map((entry) => (
          <li key={`${entry.reportedAt}-${entry.reporterId}`} className={styles.reportRow}>
            <Badge tone={getReasonTone(entry.reason)}>{entry.reason}</Badge>
            <span className={styles.reportMeta}>
              {entry.reportedAt} · 회원 #{entry.reporterId}
            </span>
          </li>
        ))}
      </ul>
    </AdminCard>
  );
}

type AuthorCardProps = {
  report: ReportDetail;
  onChoose: (action: ReportAction) => void;
};

/**
 * 오른쪽 아래 카드: 작성자의 확정된 위반·현재 제재와 처리 영역.
 * 처리 대기면 안내 문구와 처리 버튼, 처리 끝났으면 처리 결과만 보인다
 */
function AuthorCard({ report, onChoose }: AuthorCardProps) {
  const { author, handled } = report;

  return (
    <AdminCard title="작성자" meta={`회원 #${author.userId}`}>
      <dl className={styles.facts}>
        <div className={styles.fact}>
          <dt>확정된 위반</dt>
          <dd>{author.violationCount}회</dd>
        </div>
        <div className={styles.fact}>
          <dt>현재 제재</dt>
          <dd>{author.currentSanction}</dd>
        </div>
      </dl>

      {handled ? (
        <div className={styles.result}>
          <Badge tone={REPORT_RESULT_TONE[handled.result]}>
            {REPORT_RESULT_LABEL[handled.result]}
          </Badge>
          <p className={styles.resultText}>{getHandledDescription(handled)}</p>
          <p className={styles.resultMeta}>
            {handled.handledAt} · {handled.operator}
          </p>
        </div>
      ) : (
        <>
          <InfoBox variant="gray">{getNextSanctionNotice(author.violationCount)}</InfoBox>

          <Button tone="danger" size="md" onClick={() => onChoose("VIOLATION")}>
            위반으로 처리하기
          </Button>

          <div className={styles.secondary}>
            <TextButton tone="neutral" onClick={() => onChoose("NO_ISSUE")}>
              문제 없음으로 처리하기
            </TextButton>
            <TextButton tone="danger" onClick={() => onChoose("DELETED")}>
              글 삭제하기
            </TextButton>
          </div>
        </>
      )}
    </AdminCard>
  );
}

type ActionConfirmProps = {
  action: ReportAction;
  violationCount: number;
  onClose: () => void;
  onConfirm: () => void;
};

/** 처리 확인창. 위험한 처리(위반·삭제)는 빨강, 문제 없음은 파랑 */
function ActionConfirm({ action, violationCount, onClose, onConfirm }: ActionConfirmProps) {
  const copy = getActionCopy(action, violationCount);

  return (
    <ConfirmModal
      isOpen
      onClose={onClose}
      onConfirm={onConfirm}
      title={copy.title}
      message={copy.message}
      confirmLabel={copy.confirmLabel}
      tone={copy.tone}
    />
  );
}

/** 불러오는 중(피그마 없음): 카드 두 줄 자리 */
function DetailSkeleton() {
  return (
    <div className={styles.body} aria-busy="true" aria-label="신고 상세 불러오는 중">
      <div className={styles.skeletonCard} />
      <div className={styles.side}>
        <div className={styles.skeletonCard} />
        <div className={styles.skeletonCard} />
      </div>
    </div>
  );
}
