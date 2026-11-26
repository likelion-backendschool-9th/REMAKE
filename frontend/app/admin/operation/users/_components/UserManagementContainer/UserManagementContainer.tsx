"use client";

import { useState } from "react";

import { parseAsString, parseAsStringLiteral, useQueryStates } from "nuqs";

import { Badge } from "@/components/Badge/Badge";
import { Chip } from "@/components/Chip/Chip";
import { EmptyState } from "@/components/EmptyState/EmptyState";
import { InfoBox } from "@/components/InfoBox/InfoBox";
import { SearchField } from "@/components/SearchField/SearchField";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { TextButton } from "@/components/TextButton/TextButton";
import { Toast } from "@/components/Toast/Toast";
import { useTimedToast } from "@/hooks/useTimedToast";
import { formatFullDate, formatMonthDay } from "@/lib/format";

import { AdminCard } from "../../../../_components/AdminCard/AdminCard";
import { AdminLoadError } from "../../../../_components/AdminLoadError/AdminLoadError";
import { AdminPageHeader } from "../../../../_components/AdminPageHeader/AdminPageHeader";
import { type AdminUser, PROVIDER_LABELS, sanctionLabel, type UserFilter } from "../../_lib/users";
import { QuotaAdjustmentModal } from "../QuotaAdjustmentModal/QuotaAdjustmentModal";

import styles from "./UserManagementContainer.module.css";

type Props = {
  users?: AdminUser[];
  initialSelectedId?: number;
  initialFilter?: UserFilter;
  initialSearch?: string;
  initialModal?: "adjust" | "reason-empty";
  initialReason?: string;
  initialToast?: string;
  hasError?: boolean;
};

/** a5: 실제 라우트는 데이터 연결 전 로딩. 샘플과 로컬 조정은 개발 미리보기에서만 사용한다. */
export function UserManagementContainer({
  users,
  initialSelectedId,
  initialFilter = "ALL",
  initialSearch = "",
  initialModal,
  initialReason = "",
  initialToast,
  hasError = false,
}: Props) {
  const [{ filter, userId }, setQuery] = useQueryStates(
    {
      filter: parseAsStringLiteral(["ALL", "SANCTIONED"] as const).withDefault(initialFilter),
      userId: parseAsString.withDefault(initialSearch),
    },
    { history: "replace" },
  );
  const [selectedId, setSelectedId] = useState(initialSelectedId);
  const [isModalOpen, setIsModalOpen] = useState(Boolean(initialModal));
  const [rewardOverrides, setRewardOverrides] = useState<Record<number, number>>({});
  const toast = useTimedToast(initialToast ?? null);
  const visibleUsers = users?.filter(
    (user) =>
      (filter === "ALL" || (Boolean(user.currentSanction) && !user.deletedAt)) &&
      (!userId || String(user.userId) === userId.replace(/^#/, "")),
  );
  const selected = visibleUsers?.find((user) => user.userId === selectedId) ?? visibleUsers?.[0];
  const remaining = selected
    ? (rewardOverrides[selected.userId] ?? selected.quota.rewardRemaining)
    : 0;

  return (
    <>
      <AdminPageHeader
        title="사용자 관리"
        description="이메일 같은 개인 정보는 보이지 않아요. 회원 번호로 찾아요."
        actions={
          <div className={styles.filters}>
            <div className={styles.chips} role="group" aria-label="사용자 필터">
              <Chip isSelected={filter === "ALL"} onClick={() => void setQuery({ filter: "ALL" })}>
                전체
              </Chip>
              <Chip
                isSelected={filter === "SANCTIONED"}
                onClick={() => void setQuery({ filter: "SANCTIONED" })}
              >
                제재 중
              </Chip>
            </div>
            <SearchField
              className={styles.search}
              aria-label="회원 번호 검색"
              placeholder="회원 번호로 검색"
              value={userId}
              clearable={false}
              onChange={(event) =>
                void setQuery({ userId: event.target.value.replace(/[^#\d]/g, "") })
              }
            />
          </div>
        }
      />
      {hasError ? (
        <AdminLoadError title="사용자를 불러오지 못했어요" />
      ) : !visibleUsers ? (
        <div aria-busy="true" aria-label="사용자 목록 불러오는 중">
          <Skeleton width="100%" height="27.5rem" />
        </div>
      ) : (
        <div className={styles.content}>
          <AdminCard className={styles.list} aria-label="사용자 목록">
            <div className={styles.tableScroll}>
              <table className={styles.table}>
                <caption className={styles.srOnly}>
                  회원 목록. 회원 번호를 선택하면 상세 정보를 확인합니다.
                </caption>
                <colgroup>
                  <col />
                  <col />
                  <col />
                  <col />
                  <col />
                  <col />
                </colgroup>
                <thead>
                  <tr>
                    {["회원 번호", "로그인", "가입일", "구독", "확정 위반", "제재 상태"].map(
                      (label) => (
                        <th scope="col" key={label}>
                          {label}
                        </th>
                      ),
                    )}
                  </tr>
                </thead>
                <tbody>
                  {visibleUsers.map((user) => (
                    <tr key={user.userId} data-selected={selected?.userId === user.userId}>
                      <td>
                        <button
                          className={styles.member}
                          type="button"
                          aria-pressed={selected?.userId === user.userId}
                          aria-controls="admin-user-detail"
                          onClick={() => {
                            setSelectedId(user.userId);
                            setIsModalOpen(false);
                          }}
                        >
                          #{user.userId}
                        </button>
                      </td>
                      <td>
                        <Badge>{PROVIDER_LABELS[user.provider]}</Badge>
                      </td>
                      <td>{formatMonthDay(user.joinedAt)}</td>
                      <td>
                        {user.plan === "PREMIUM" ? <Badge tone="primary">프리미엄</Badge> : "무료"}
                      </td>
                      <td>{user.violationCount}회</td>
                      <td>
                        <div className={styles.sanction}>
                          {user.deletedAt ? (
                            <>
                              <Badge>탈퇴</Badge>
                              <span>{formatMonthDay(user.deletedAt)} 탈퇴</span>
                            </>
                          ) : user.currentSanction ? (
                            <>
                              <Badge
                                tone={
                                  user.currentSanction.type === "SHARING" ? "warning" : "danger"
                                }
                              >
                                {sanctionLabel(user.currentSanction.type)}
                              </Badge>
                              {user.currentSanction.endsAt && (
                                <span>{formatMonthDay(user.currentSanction.endsAt)}까지</span>
                              )}
                            </>
                          ) : (
                            "-"
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {visibleUsers.length === 0 && (
                    <tr>
                      <td className={styles.empty} colSpan={6}>
                        <EmptyState>
                          {userId
                            ? `회원 #${userId.replace(/^#/, "")}를 찾지 못했어요`
                            : "조건에 맞는 사용자가 없어요"}
                        </EmptyState>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </AdminCard>
          {selected && (
            <AdminCard
              className={styles.detail}
              id="admin-user-detail"
              aria-label={`회원 #${selected.userId} 상세`}
            >
              <h2>회원 #{selected.userId}</h2>
              <dl className={styles.facts}>
                <div>
                  <dt>로그인</dt>
                  <dd>{selected.deletedAt ? "-" : PROVIDER_LABELS[selected.provider]}</dd>
                </div>
                <div>
                  <dt>가입일</dt>
                  <dd>{formatFullDate(selected.joinedAt)}</dd>
                </div>
                <div>
                  <dt>구독</dt>
                  <dd>
                    {selected.deletedAt ? "-" : selected.plan === "PREMIUM" ? "프리미엄" : "무료"}
                  </dd>
                </div>
                <div>
                  <dt>확정 위반</dt>
                  <dd>{selected.violationCount}회</dd>
                </div>
                <div>
                  <dt>남은 생성 횟수</dt>
                  <dd>
                    {selected.deletedAt
                      ? "-"
                      : selected.quota.blocked
                        ? "계정 정지로 사용 못 함"
                        : `오늘 ${selected.quota.todayRemaining}회 · 보상 ${remaining}회`}
                  </dd>
                </div>
              </dl>
              {!selected.deletedAt && !selected.quota.blocked && (
                <TextButton
                  className={styles.adjust}
                  withArrow
                  disabled={Boolean(selected.deletedAt) || selected.quota.blocked}
                  onClick={() => setIsModalOpen(true)}
                >
                  생성 횟수 조정
                </TextButton>
              )}
              <dl className={styles.facts}>
                <div>
                  <dt>현재 제재</dt>
                  <dd
                    data-danger={
                      selected.currentSanction?.type !== "SHARING" &&
                      Boolean(selected.currentSanction)
                    }
                  >
                    {selected.deletedAt
                      ? `탈퇴(${formatMonthDay(selected.deletedAt)})`
                      : selected.currentSanction
                        ? `${sanctionLabel(selected.currentSanction.type, true)}${selected.currentSanction.endsAt ? ` · ${formatMonthDay(selected.currentSanction.endsAt)}까지` : ""}`
                        : "없음"}
                  </dd>
                </div>
              </dl>
              <h3>제재 이력</h3>
              {selected.sanctions.length ? (
                <ul className={styles.history}>
                  {selected.sanctions.map((sanction) => (
                    <li key={sanction.level}>
                      <p>
                        {formatMonthDay(sanction.startsAt)}　{sanction.level}차 ·{" "}
                        {sanction.type === "PERMANENT"
                          ? ""
                          : sanction.level === 1
                            ? "7일 "
                            : "30일 "}
                        {sanctionLabel(sanction.type, true)}
                      </p>
                      <small>신고된 글 &quot;{sanction.sharedRecipeTitle}&quot;</small>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className={styles.muted}>제재 이력이 없어요</p>
              )}
              <InfoBox>
                {selected.deletedAt
                  ? "탈퇴하면 회원 번호만 남아요. 지우지 않은 공유 글은 익명 게시물로 남아요."
                  : "제재 단계: 1차 7일 공유 정지 · 2차 30일 공유 정지 · 3차 30일 계정 정지 · 4차 계정 정지(영구)"}
              </InfoBox>
            </AdminCard>
          )}
        </div>
      )}
      {isModalOpen && selected && !selected.deletedAt && !selected.quota.blocked && (
        <QuotaAdjustmentModal
          key={selected.userId}
          userId={selected.userId}
          remaining={remaining}
          initialReason={initialReason}
          initialError={initialModal === "reason-empty"}
          onClose={() => setIsModalOpen(false)}
          onConfirm={({ delta }) => {
            // 미리보기에서만 표시 값을 바꾼다. API 요청·이력 저장은 연동 작업에서 구현한다.
            setRewardOverrides((values) => ({ ...values, [selected.userId]: remaining + delta }));
            setIsModalOpen(false);
            toast.show(`생성 횟수를 ${Math.abs(delta)}회 ${delta > 0 ? "늘렸어요" : "줄였어요"}`);
          }}
        />
      )}
      <div className={styles.toast} aria-live="polite" aria-atomic="true">
        {toast.value && (
          <Toast variant="success" isLeaving={toast.isLeaving} onLeft={toast.remove}>
            {toast.value}
          </Toast>
        )}
      </div>
    </>
  );
}
