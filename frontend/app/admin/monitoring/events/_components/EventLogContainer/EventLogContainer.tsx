"use client";

import { useState } from "react";

import { Badge } from "@/components/Badge/Badge";
import { Chip } from "@/components/Chip/Chip";
import { EmptyState } from "@/components/EmptyState/EmptyState";
import { SearchField } from "@/components/SearchField/SearchField";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { dayjs } from "@/lib/dayjs";
import { formatMonthDay } from "@/lib/format";

import { AdminLoadError } from "../../../../_components/AdminLoadError/AdminLoadError";
import { AdminPageHeader } from "../../../../_components/AdminPageHeader/AdminPageHeader";
import { type EventLogItem, type EventSource, formatPropertySummary } from "../../_lib/event-log";

import styles from "./EventLogContainer.module.css";

const EVENT_SOURCES: EventSource[] = ["CLIENT", "SERVER"];

type EventLogContainerProps = {
  /** 이벤트 목록(최근 순). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  events?: EventLogItem[];
  /** 불러오기 실패(a0-불러오기 실패와 같은 방식) */
  hasError?: boolean;
  /** 처음 검색어(이벤트 이름) */
  initialEventName?: string;
};

/**
 * a6 이벤트 로그. 쌓인 원본 이벤트를 이벤트 이름·회원 번호·발생 위치로 찾고, 줄을 누르면 오른쪽에 자세히 보인다.
 * 상태: 목록(a6-이벤트 로그) / 검색 결과 없음(a6-이벤트 로그-검색 결과 없음) / 불러오는 중(피그마 없음) / 불러오기 실패.
 * 검색·칩은 API가 붙으면 조회 조건으로 쓴다
 */
export function EventLogContainer({
  events,
  hasError = false,
  initialEventName = "",
}: EventLogContainerProps) {
  const [eventName, setEventName] = useState(initialEventName);
  const [userId, setUserId] = useState("");
  const [sources, setSources] = useState<EventSource[]>(EVENT_SOURCES);
  // "실제 데이터만" = API 64 `excludeSeed`
  const [excludeSeed, setExcludeSeed] = useState(true);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const toggleSource = (source: EventSource) =>
    setSources((current) =>
      current.includes(source) ? current.filter((item) => item !== source) : [...current, source],
    );

  // 고른 줄이 없으면 맨 위(가장 최근) 이벤트를 자세히 보인다
  const selected = events?.find((item) => item.id === selectedId) ?? events?.[0];

  return (
    <>
      <AdminPageHeader
        title="이벤트 로그"
        description="쌓인 원본 이벤트를 찾아 봐요. 회원 번호만 보이고 개인 정보는 기록하지 않아요."
      />

      <div className={styles.filters}>
        <SearchField
          aria-label="이벤트 이름"
          placeholder="이벤트 이름 (예: recipe_generated)"
          value={eventName}
          onChange={(event) => setEventName(event.target.value)}
          className={styles.eventNameField}
        />
        <SearchField
          aria-label="회원 번호"
          placeholder="회원 번호"
          inputMode="numeric"
          value={userId}
          onChange={(event) => setUserId(event.target.value.replace(/\D/g, ""))}
          className={styles.userIdField}
        />
        <div className={styles.chips} role="group" aria-label="발생 위치">
          {EVENT_SOURCES.map((source) => (
            <Chip
              key={source}
              isSelected={sources.includes(source)}
              onClick={() => toggleSource(source)}
            >
              {source}
            </Chip>
          ))}
          <Chip isSelected={excludeSeed} onClick={() => setExcludeSeed(!excludeSeed)}>
            실제 데이터만
          </Chip>
        </div>
      </div>

      {hasError ? (
        <AdminLoadError title="이벤트를 불러오지 못했어요" />
      ) : !events ? (
        <EventTableSkeleton />
      ) : events.length === 0 ? (
        <section className={styles.emptyCard}>
          <EmptyState>
            {eventName || userId
              ? "찾는 이벤트가 없어요. 이벤트 이름을 확인해 주세요"
              : "쌓인 이벤트가 없어요"}
          </EmptyState>
        </section>
      ) : (
        <div className={styles.content}>
          <EventTable events={events} selectedId={selected?.id} onSelect={setSelectedId} />
          {selected && <EventDetail event={selected} />}
        </div>
      )}
    </>
  );
}

type EventTableProps = {
  events: EventLogItem[];
  selectedId?: number;
  onSelect: (id: number) => void;
};

/** 이벤트 목록 표: 시각 · 이벤트 · 회원 · 화면 · 발생 위치 · 주요 속성 */
function EventTable({ events, selectedId, onSelect }: EventTableProps) {
  return (
    <section className={styles.listCard} aria-label="이벤트 목록">
      <table className={styles.table}>
        <thead>
          <tr>
            <th scope="col" className={styles.timeColumn}>
              시각({formatMonthDay(events[0].occurredAt)})
            </th>
            <th scope="col" className={styles.nameColumn}>
              이벤트
            </th>
            <th scope="col" className={styles.userColumn}>
              회원
            </th>
            <th scope="col" className={styles.screenColumn}>
              화면
            </th>
            <th scope="col" className={styles.sourceColumn}>
              발생 위치
            </th>
            <th scope="col">주요 속성</th>
          </tr>
        </thead>
        <tbody>
          {events.map((item) => {
            const isSelected = item.id === selectedId;
            return (
              <tr
                key={item.id}
                className={styles.row}
                data-selected={isSelected || undefined}
                onClick={() => onSelect(item.id)}
              >
                <td>{dayjs(item.occurredAt).format("HH:mm:ss")}</td>
                <td>
                  {/* 키보드로도 고를 수 있게 이벤트 이름을 버튼으로 둔다. 줄 어디를 눌러도 고른다 */}
                  <button
                    type="button"
                    className={styles.nameButton}
                    aria-pressed={isSelected}
                    onClick={(event) => {
                      event.stopPropagation();
                      onSelect(item.id);
                    }}
                  >
                    {item.eventName}
                  </button>
                </td>
                <td>{item.userId === null ? "-" : `#${item.userId}`}</td>
                <td>{item.screen}</td>
                <td>
                  <Badge tone={item.source === "SERVER" ? "primary" : "neutral"}>
                    {item.source}
                  </Badge>
                </td>
                <td className={styles.properties}>{formatPropertySummary(item.properties)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <p className={styles.footnote}>최근 순 · 원본은 180일 보관</p>
    </section>
  );
}

/** 오른쪽 이벤트 상세: 공통 값과 properties */
function EventDetail({ event }: { event: EventLogItem }) {
  const fields: [string, string][] = [
    ["user_id", event.userId === null ? "-" : String(event.userId)],
    ["session_id", event.sessionId],
    ["source", event.source],
    ["occurred_at", dayjs(event.occurredAt).format("YYYY-MM-DD HH:mm:ss")],
    ["screen", event.screen],
    ["platform", event.platform],
    ["app_version", event.appVersion],
    ["is_seed", String(event.isSeed)],
  ];

  return (
    <aside className={styles.detail} aria-label="이벤트 상세">
      <h2 className={styles.detailTitle}>{event.eventName}</h2>
      <p className={styles.detailMeta}>
        {event.eventCode} · {event.eventDescription}
      </p>
      <dl className={styles.fields}>
        {fields.map(([key, value]) => (
          <div key={key} className={styles.field}>
            <dt>{key}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>
      <div className={styles.propertiesBox}>
        <h3 className={styles.propertiesTitle}>properties</h3>
        <dl className={styles.propertyList}>
          {Object.entries(event.properties).map(([key, value]) => (
            <div key={key} className={styles.field}>
              <dt>{key}</dt>
              <dd>{String(value)}</dd>
            </div>
          ))}
        </dl>
      </div>
    </aside>
  );
}

/** 불러오는 중(피그마 없음): 표 줄 자리 */
function EventTableSkeleton() {
  return (
    <section className={styles.skeletonCard} aria-busy="true" aria-label="이벤트 목록">
      {Array.from({ length: 8 }, (_, index) => (
        <Skeleton key={index} width="100%" height="1.25rem" />
      ))}
    </section>
  );
}
