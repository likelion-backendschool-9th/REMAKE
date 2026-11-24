"use client";

import { type MouseEvent, type PointerEvent, useRef, useState } from "react";
import Link from "next/link";

import { EmptyState } from "@/components/EmptyState/EmptyState";
import { IngredientRow } from "@/components/IngredientRow/IngredientRow";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { useIsClient } from "@/hooks/useIsClient";
import { dayjs } from "@/lib/dayjs";
import { formatFullDate, formatWon } from "@/lib/format";
import { formatAmount, getCategoryImage, getStorage } from "@/lib/ingredients";

import {
  CATEGORY_LEGEND,
  type DailyItems,
  defaultDate,
  DISCARDED_PATH,
  formatPatternAverageDiff,
  getLegend,
  monthWeeks,
  patternMonths,
  type SpendingPattern,
  WEEKDAY_LABELS,
} from "../../_lib/patterns";

import styles from "./PatternsContainer.module.css";

type PatternsContainerProps = {
  /** 그 달 소비(API 25). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  pattern?: SpendingPattern;
  /** 고른 날의 품목(API 26). 없으면 그 날 목록을 불러오는 중 */
  dayItems?: DailyItems;
  /** 오늘("2026-11-24"). 미리보기용, 기본은 화면을 연 날 */
  today?: string;
};

/**
 * p3.4 나의 소비. 지난 6개월 월 탭, 버린 식비(→ p3.4.1), 그 달 지출·평균 차이, 카테고리 비율 막대 + 범례,
 * 월~일 한 주(고른 날 아래 그 날 금액), 고른 날의 "총 N원"과 품목 목록(domain-rules 3).
 * 상태: 기본(p3.4-소비 패턴) / 그 날 소비 없음(p3.4-소비 패턴-빈 리스트) / 불러오는 중(p3.4-소비 패턴-스켈레톤).
 * 오늘 날짜는 브라우저에서만 읽는다(미리 그리는 화면에서 시계를 읽지 않음, cacheComponents).
 */
export function PatternsContainer({ pattern, dayItems, today: todayProp }: PatternsContainerProps) {
  const isClient = useIsClient();
  const today = todayProp ?? (isClient ? dayjs().format("YYYY-MM-DD") : null);

  return (
    <>
      <SubHeader title="나의 소비" />
      {today ? (
        <PatternsBody pattern={pattern} dayItems={dayItems} today={today} />
      ) : (
        <main className={styles.main} aria-busy="true">
          <div className={styles.monthTabs} />
          <SummarySkeleton />
        </main>
      )}
    </>
  );
}

function PatternsBody({
  pattern,
  dayItems,
  today,
}: {
  pattern?: SpendingPattern;
  dayItems?: DailyItems;
  today: string;
}) {
  const months = patternMonths(today);
  const [month, setMonth] = useState(pattern?.month ?? months[months.length - 1]);
  const [selectedDate, setSelectedDate] = useState(
    () => dayItems?.date ?? defaultDate(month, today, pattern),
  );
  // TODO: 달을 바꾸면 API 25(?month=), 날을 바꾸면 API 26(?date=)으로 받는다. 지금은 받은 값만 그린다
  const data = pattern?.month === month ? pattern : undefined;
  const items = dayItems?.date === selectedDate ? dayItems : undefined;
  const monthNumber = Number(month.slice(5, 7));

  const selectMonth = (next: string) => {
    setMonth(next);
    setSelectedDate(defaultDate(next, today, pattern?.month === next ? pattern : undefined));
  };

  return (
    <main className={styles.main} aria-busy={!data || undefined}>
      <nav className={styles.monthTabs} aria-label="월 선택">
        {months.map((value) => (
          <button
            key={value}
            type="button"
            className={styles.monthTab}
            aria-current={value === month ? "true" : undefined}
            onClick={() => selectMonth(value)}
          >
            {Number(value.slice(5, 7))}월
          </button>
        ))}
      </nav>

      {data ? (
        <section className={styles.summary} aria-label={`${monthNumber}월 소비`}>
          <div className={styles.summaryText}>
            {/* 버린 식비를 누르면 이번 달 버린 식재료(p3.4.1). 누를 수 있어 파랑(blueprint §6) */}
            <Link href={`${DISCARDED_PATH}?month=${month}`} className={styles.discarded}>
              {monthNumber}월 버린 식비 {formatWon(data.discardedAmount)}
            </Link>
            <p className={styles.total}>{formatWon(data.totalAmount)} 지출</p>
            {data.averageDiffAmount !== null && (
              <p className={styles.average}>{formatPatternAverageDiff(data.averageDiffAmount)}</p>
            )}
          </div>
          <CategoryBar categories={data.categories} />
        </section>
      ) : (
        <SummarySkeleton />
      )}

      <WeekStrip
        key={month}
        month={month}
        today={today}
        selectedDate={selectedDate}
        dailyTotals={data?.dailyTotals}
        onSelect={setSelectedDate}
      />

      <section className={styles.day} aria-labelledby="pattern-day-title">
        <div className={styles.dayHeader}>
          <h2 id="pattern-day-title" className={styles.dayTitle}>
            {formatFullDate(selectedDate)}
          </h2>
          {items ? (
            items.items.length > 0 && (
              <p className={styles.dayTotal}>총 {formatWon(items.totalAmount)}</p>
            )
          ) : (
            <Skeleton width="4.75rem" height="0.8125rem" />
          )}
        </div>
        {!items ? (
          // 피그마 p3.4-소비 패턴-스켈레톤: 품목 자리 2개
          <div className={styles.itemSkeletons} aria-busy="true">
            <Skeleton width="100%" height="2.25rem" />
            <Skeleton width="100%" height="2.25rem" />
          </div>
        ) : items.items.length === 0 ? (
          // 피그마 p3.4-소비 패턴-빈 리스트
          <EmptyState className={styles.empty}>해당 날짜에는 소비가 없습니다</EmptyState>
        ) : (
          <ul className={styles.items}>
            {items.items.map((item, index) => (
              // 그 날 품목은 id가 없다(API 26). 목록 순서는 바뀌지 않는다

              <li key={index}>
                <IngredientRow
                  name={item.name}
                  imageSrc={item.imageUrl ?? getCategoryImage(item.categoryCode)}
                  description={`${getStorage(item.storage).place} · ${formatAmount(item.amount, item.unit)}`}
                  trailing={<span className={styles.price}>{formatWon(item.price)}</span>}
                  className={styles.item}
                />
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}

/** 카테고리 비율 막대(0원 카테고리는 빼고, 비율만큼 폭) + 범례 12개(피그마 순서) */
function CategoryBar({ categories }: { categories: SpendingPattern["categories"] }) {
  const shown = categories.filter((category) => category.amount > 0);
  const description = shown
    .map(
      (category) =>
        `${getLegend(category.categoryCode).label} ${Math.round(category.ratio * 100)}%`,
    )
    .join(", ");

  return (
    <div className={styles.chart}>
      <div className={styles.bar} role="img" aria-label={`카테고리별 지출 비율: ${description}`}>
        {shown.map((category) => (
          <span
            key={category.categoryCode}
            className={styles.barPart}
            style={{
              flexGrow: category.ratio,
              background: getLegend(category.categoryCode).color,
            }}
          />
        ))}
      </div>
      <Legend />
    </div>
  );
}

function Legend() {
  return (
    <ul className={styles.legend} aria-hidden="true">
      {CATEGORY_LEGEND.map((item) => (
        <li key={item.code} className={styles.legendItem}>
          <span className={styles.legendSwatch} style={{ background: item.color }} />
          {item.label}
        </li>
      ))}
    </ul>
  );
}

/**
 * 월~일 날짜 줄. 좌우로 밀면 그 달의 앞·뒤 주로 넘어간다(캐러셀, 11/25 확정).
 * 주마다 한 쪽이고 scroll-snap으로 한 주씩 멈춘다. 처음엔 고른 날이 든 주를 보여 준다.
 * 고른 날은 회색 원 + 굵게, 아래에 그 날 금액(소비가 있을 때).
 * 그 달 밖의 날, 오늘 뒤의 날은 고를 수 없다(제안). 달을 바꾸면 key로 새로 그려 그 달 주로 맞춘다.
 */
function WeekStrip({
  month,
  today,
  selectedDate,
  dailyTotals,
  onSelect,
}: {
  month: string;
  today: string;
  selectedDate: string;
  dailyTotals?: SpendingPattern["dailyTotals"];
  onSelect: (date: string) => void;
}) {
  const weeks = monthWeeks(month);
  const startIndex = Math.max(
    0,
    weeks.findIndex((week) => week.includes(selectedDate)),
  );
  const selectedAmount = dailyTotals?.find((total) => total.date === selectedDate)?.amount;

  const drag = useRef<{ x: number; left: number; moved: boolean } | null>(null);
  const suppressClick = useRef(false);

  // 처음 그릴 때 한 번만 고른 날의 주로 옮긴다(넘기는 중에는 건드리지 않음)
  const scrollToStart = (track: HTMLDivElement | null) => {
    if (track && track.dataset.ready !== "true") {
      track.scrollLeft = startIndex * track.clientWidth;
      track.dataset.ready = "true";
    }
  };

  // 마우스 끌기(데스크톱). 터치·트랙패드는 브라우저 가로 스크롤이 그대로 처리한다
  const onPointerDown = (event: PointerEvent<HTMLDivElement>) => {
    if (event.pointerType !== "mouse" || event.button !== 0) return;
    // 지난 끌기 뒤에 click이 오지 않았어도 새로 누르면 다시 받는다
    suppressClick.current = false;
    drag.current = { x: event.clientX, left: event.currentTarget.scrollLeft, moved: false };
  };

  const onPointerMove = (event: PointerEvent<HTMLDivElement>) => {
    const current = drag.current;
    if (!current) return;
    const dx = event.clientX - current.x;
    // 5px 안쪽 움직임은 날 누르기로 본다
    if (!current.moved && Math.abs(dx) < 5) return;
    const track = event.currentTarget;
    if (!current.moved) {
      current.moved = true;
      track.setPointerCapture(event.pointerId);
      // 끄는 동안은 scroll-snap을 꺼야 손을 따라 움직인다
      track.dataset.dragging = "true";
    }
    track.scrollLeft = current.left - dx;
  };

  const endDrag = (event: PointerEvent<HTMLDivElement>) => {
    const current = drag.current;
    drag.current = null;
    if (!current?.moved) return;
    suppressClick.current = true;
    const track = event.currentTarget;
    const width = track.clientWidth;
    const distance = track.scrollLeft - current.left;
    // 한 주 폭의 15% 넘게 끌면 그 방향으로 한 주 넘김
    const from = Math.round(current.left / width);
    const step = Math.abs(distance) > width * 0.15 ? Math.sign(distance) : 0;
    const page = Math.min(weeks.length - 1, Math.max(0, from + step));
    const release = () => delete track.dataset.dragging;
    track.addEventListener("scrollend", release, { once: true });
    window.setTimeout(release, 600);
    track.scrollTo({ left: page * width, behavior: "smooth" });
  };

  // 끌기가 끝날 때 손을 뗀 자리의 날이 눌리지 않게 막는다
  const onClickCapture = (event: MouseEvent<HTMLDivElement>) => {
    if (!suppressClick.current) return;
    suppressClick.current = false;
    event.preventDefault();
    event.stopPropagation();
  };

  return (
    <div className={styles.week}>
      <div className={styles.weekRow} aria-hidden="true">
        {WEEKDAY_LABELS.map((label) => (
          <span key={label} className={styles.weekday}>
            {label}
          </span>
        ))}
      </div>
      <div
        ref={scrollToStart}
        className={styles.weekTrack}
        role="group"
        aria-roledescription="캐러셀"
        aria-label="날짜 선택, 좌우로 밀어 주 넘기기"
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={endDrag}
        onPointerCancel={endDrag}
        onClickCapture={onClickCapture}
      >
        {weeks.map((week) => (
          <div key={week[0]} className={styles.weekPage}>
            <div className={styles.weekRow}>
              {week.map((date, index) => (
                <button
                  key={date}
                  type="button"
                  className={styles.dayButton}
                  aria-pressed={date === selectedDate}
                  aria-label={`${formatFullDate(date)} ${WEEKDAY_LABELS[index]}요일`}
                  disabled={!date.startsWith(month) || date > today}
                  onClick={() => onSelect(date)}
                >
                  {Number(date.slice(8, 10))}
                </button>
              ))}
            </div>
            <div className={styles.weekRow}>
              {dailyTotals ? (
                week.map((date) => (
                  <span key={date} className={styles.dayAmount}>
                    {date === selectedDate && selectedAmount ? formatWon(selectedAmount) : null}
                  </span>
                ))
              ) : (
                <Skeleton width="100%" height="0.5rem" />
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/** 피그마 p3.4-소비 패턴-스켈레톤: 글 3줄 자리, 막대 자리 + 범례 */
function SummarySkeleton() {
  return (
    <div className={styles.summary} aria-busy="true">
      <div className={styles.summarySkeleton}>
        <Skeleton width="9.375rem" height="1.0625rem" />
        <Skeleton width="9.375rem" height="1.0625rem" />
        <Skeleton width="9.375rem" height="1.0625rem" />
      </div>
      <div className={styles.chart}>
        <Skeleton width="100%" height="0.875rem" radius="0.1875rem" />
        <Legend />
      </div>
    </div>
  );
}
