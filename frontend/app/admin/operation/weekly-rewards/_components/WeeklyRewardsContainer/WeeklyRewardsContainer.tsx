"use client";

import { parseAsString, useQueryState } from "nuqs";

import { Badge } from "@/components/Badge/Badge";
import { Chip } from "@/components/Chip/Chip";
import { EmptyState } from "@/components/EmptyState/EmptyState";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { formatMonthDay, formatNumber } from "@/lib/format";

import { AdminCard } from "../../../../_components/AdminCard/AdminCard";
import { AdminLoadError } from "../../../../_components/AdminLoadError/AdminLoadError";
import { AdminPageHeader } from "../../../../_components/AdminPageHeader/AdminPageHeader";
import type { WeeklyRewardsData } from "../../_lib/weekly-rewards";

import styles from "./WeeklyRewardsContainer.module.css";

export function WeeklyRewardsContainer({
  data,
  initialWeek,
  hasError = false,
}: {
  data?: WeeklyRewardsData;
  initialWeek?: string;
  hasError?: boolean;
}) {
  const [week, setWeek] = useQueryState(
    "week",
    parseAsString.withDefault(initialWeek ?? data?.weeks[0] ?? ""),
  );
  const selected = data?.weeksData.find((item) => item.weekStart === week);

  return (
    <>
      <AdminPageHeader
        title="랭킹·보상"
        description="매주 월요일 0시 정기 작업이 지난주 순위를 기록하고 보상을 줘요. 1위 +10회 · 2위 +6회 · 3위 +3회"
        actions={
          data && (
            <div className={styles.weeks} role="group" aria-label="보상 주 선택">
              {data.weeks.map((value) => (
                <Chip key={value} isSelected={value === week} onClick={() => void setWeek(value)}>
                  {formatMonthDay(value)} 주
                </Chip>
              ))}
            </div>
          )
        }
      />
      {hasError ? (
        <AdminLoadError title="랭킹과 보상을 불러오지 못했어요" />
      ) : !data ? (
        <div aria-busy="true" aria-label="랭킹과 보상 불러오는 중">
          <Skeleton width="100%" height="21.25rem" />
        </div>
      ) : !selected ? (
        <AdminCard>
          <EmptyState>선택한 주의 보상 정보가 없어요</EmptyState>
        </AdminCard>
      ) : (
        <>
          <AdminCard className={styles.card} aria-label="주간 TOP 3">
            <div className={styles.heading}>
              <h2>
                {formatMonthDay(selected.weekStart)}(월) ~ {formatMonthDay(selected.weekEnd)}(일)
                TOP 3
              </h2>
              {selected.grantedAt && <span>{formatMonthDay(selected.grantedAt)} 00:00 지급</span>}
            </div>
            {selected.top.length === 0 ? (
              <div className={styles.empty}>
                <EmptyState>저장된 공유 레시피가 없어 이번 주는 보상이 없어요</EmptyState>
              </div>
            ) : (
              <>
                <div className={styles.scroll}>
                  <table className={styles.table}>
                    <caption className={styles.srOnly}>주간 공유 레시피 순위와 보상</caption>
                    <colgroup>
                      <col className={styles.rankCol} />
                      <col />
                      <col className={styles.countCol} />
                      <col className={styles.authorCol} />
                      <col className={styles.rewardCol} />
                      <col className={styles.grantedCol} />
                    </colgroup>
                    <thead>
                      <tr>
                        {["순위", "공유 레시피", "저장 수", "작성자", "보상", "지급"].map(
                          (label) => (
                            <th key={label} scope="col">
                              {label}
                            </th>
                          ),
                        )}
                      </tr>
                    </thead>
                    <tbody>
                      {[1, 2, 3].map((rank) => {
                        const item = selected.top.find((entry) => entry.rank === rank);
                        return (
                          <tr key={rank} data-empty={!item || undefined}>
                            <td className={styles.rank}>{rank}위</td>
                            <td>{item?.title ?? "저장된 레시피가 없어요"}</td>
                            <td>{item ? formatNumber(item.saveCount) : "-"}</td>
                            <td>{item ? `#${item.authorId}` : "-"}</td>
                            <td className={styles.reward}>
                              {item && item.reward > 0 ? `+${item.reward}회` : "-"}
                            </td>
                            <td>
                              <Badge tone={item?.granted ? "safe" : "neutral"}>
                                {item?.granted
                                  ? "지급 완료"
                                  : item && item.reward > 0
                                    ? "지급 대기"
                                    : "보상 없음"}
                              </Badge>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
                <p className={styles.note}>
                  {selected.top.length < 3
                    ? "저장 수 0인 레시피는 순위에 올라도 보상이 없어요"
                    : "레시피 순위마다 줘서 한 사람이 여러 번 받을 수 있어요 · 숨긴 글은 순위에서 빠져요 · 저장 수 0이면 보상 없음"}
                </p>
              </>
            )}
          </AdminCard>
          {data.history.length > 0 && (
            <AdminCard className={styles.card} aria-label="보상 지급 이력">
              <div className={styles.heading}>
                <h2>보상 지급 이력</h2>
                <span>weekly_reward_granted</span>
              </div>
              <div className={styles.scroll}>
                <table className={`${styles.table} ${styles.history}`}>
                  <caption className={styles.srOnly}>주별 수상자와 보상 지급 합계</caption>
                  <thead>
                    <tr>
                      {["주", "1위", "2위", "3위", "준 횟수 합계"].map((label) => (
                        <th key={label} scope="col">
                          {label}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {data.history.map((entry) => (
                      <tr key={entry.weekStart}>
                        <td>{formatMonthDay(entry.weekStart)} 주</td>
                        {[1, 2, 3].map((rank) => {
                          const winner = entry.winners.find((item) => item.rank === rank);
                          return <td key={rank}>{winner ? `${rank}위 #${winner.userId}` : "-"}</td>;
                        })}
                        <td>{entry.totalReward}회</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </AdminCard>
          )}
        </>
      )}
    </>
  );
}
