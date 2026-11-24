"use client";

import { useId, useState } from "react";
import Image from "next/image";
import Link from "next/link";

import { AltArrowDownIcon } from "@solar-icons/react/linear";

import { EmptyState } from "@/components/EmptyState/EmptyState";
import { MainHeader } from "@/components/MainHeader/MainHeader";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { dayjs } from "@/lib/dayjs";
import { formatDateRange, formatNumber, formatWon, formatYearMonth } from "@/lib/format";
import { MAIN_TABS, NOTIFICATIONS_HREF, SETTINGS_HREF } from "@/lib/navigation";

import { StoreBadge } from "../../../_components/StoreBadge/StoreBadge";
import {
  formatAverageDiff,
  formatPurchaseDay,
  type ReceiptMonth,
  recentMonths,
} from "../../_lib/receipts";
import { ReceiptMonthSheet } from "../ReceiptMonthSheet/ReceiptMonthSheet";

import styles from "./ReceiptsContainer.module.css";

const RECEIPTS_PATH = "/user/receipt/receipts";
const SCAN_PATH = "/user/receipt/scan";
const PATTERNS_PATH = "/user/receipt/patterns";

type ReceiptsContainerProps = {
  /** 그 달 영수증(API 22). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  receiptMonth?: ReceiptMonth;
  /** 가장 급한 알림 한 줄(API 43). 없으면 알림 띠를 그리지 않는다 */
  notice?: { message: string; unreadCount: number };
  /** 오늘("2026-11-24"). 미리보기용, 기본은 화면을 연 날(월 선택지 6개를 만든다) */
  today?: string;
  /** 월 선택 시트를 처음부터 연다. 미리보기용 */
  initialMonthSheetOpen?: boolean;
};

/**
 * p3 영수증. 위: 영수증 촬영 카드, 이미지 업로드·소비 패턴 카드(6개월 보관 안내). 아래: 전체 영수증(월 선택·월 합계·
 * 평균 차이·주차별 영수증). 영수증 줄을 누르면 영수증 상세(domain-rules 3).
 * 상태: 기본(p3-영수증) / 불러오는 중(p3-영수증-스켈레톤) / 빈 리스트(p3-영수증-빈 리스트) / 날짜 선택(p3-영수증-날짜 선택).
 */
export function ReceiptsContainer({
  receiptMonth,
  notice,
  today,
  initialMonthSheetOpen = false,
}: ReceiptsContainerProps) {
  const uploadId = useId();

  return (
    <>
      <MainHeader
        tabs={MAIN_TABS}
        activeHref={RECEIPTS_PATH}
        settingsHref={SETTINGS_HREF}
        notice={notice && { ...notice, href: NOTIFICATIONS_HREF }}
      />
      <main className={styles.main}>
        <div className={styles.top}>
          {/* 영수증 촬영: 카메라로 이동(카메라를 못 쓰면 그 화면에서 토스트) */}
          <Link href={SCAN_PATH} className={styles.scan}>
            <span className={styles.scanLabel}>쉽고 빠르게 재료 담기</span>
            <span className={styles.scanTitle}>영수증으로 재료 등록 하기</span>
          </Link>

          <section className={styles.tools} aria-label="영수증 도구">
            <div className={styles.toolRow}>
              <span className={styles.toolName}>
                <span className={styles.toolIcon} aria-hidden="true">
                  <Image src="/images/receipt/upload-album.svg" alt="" width={22} height={22} />
                </span>
                영수증 이미지 업로드
              </span>
              {/* TODO: 고른 이미지로 OCR(API 20)을 보내고 결과 화면으로. 업로드 뒤 화면은 피그마 대기 */}
              <input id={uploadId} type="file" accept="image/*" className={styles.srOnly} />
              <label htmlFor={uploadId} className={styles.toolButton}>
                첨부하기
              </label>
            </div>
            <div className={styles.toolRow}>
              <span className={styles.toolName}>
                <span className={styles.toolIcon} aria-hidden="true">
                  <Image src="/images/receipt/pattern-sparkle.svg" alt="" width={22} height={22} />
                </span>
                소비 패턴 확인하기
              </span>
              <Link href={PATTERNS_PATH} className={styles.toolButton} data-tone="soft">
                확인하기
              </Link>
            </div>
            <p className={styles.retention}>영수증은 구매일로부터 6개월 동안 보관돼요</p>
          </section>
        </div>

        <section className={styles.list} aria-labelledby="receipts-title">
          <h2 id="receipts-title" className={styles.listTitle}>
            전체 영수증
            {receiptMonth && (
              <Image
                src="/images/receipt/receipt-title.webp"
                alt=""
                width={20}
                height={20}
                className={styles.listTitleIcon}
              />
            )}
          </h2>
          {receiptMonth ? (
            <ReceiptMonthView
              receiptMonth={receiptMonth}
              today={today}
              initialMonthSheetOpen={initialMonthSheetOpen}
            />
          ) : (
            // 피그마 p3-영수증-스켈레톤. 월 이름도 회색 자리로 둔다(미리 그리는 화면에서 오늘 날짜를 읽지 않음)
            <div className={styles.skeleton} aria-busy="true">
              <Skeleton width="4.625rem" height="1.0625rem" />
              <div className={styles.skeletonSummary}>
                <Skeleton width="9.4375rem" height="1.375rem" />
                <Skeleton width="9.4375rem" height="0.8125rem" />
              </div>
              <Skeleton width="11.125rem" height="0.8125rem" />
              <Skeleton width="100%" height="2.75rem" />
            </div>
          )}
        </section>
      </main>
    </>
  );
}

/** 값이 온 뒤의 목록. 월 선택지(지난 6개월)를 만들 오늘 날짜는 이때 읽는다(cacheComponents) */
function ReceiptMonthView({
  receiptMonth,
  today: todayProp,
  initialMonthSheetOpen,
}: {
  receiptMonth: ReceiptMonth;
  today?: string;
  initialMonthSheetOpen: boolean;
}) {
  const [today] = useState(() => todayProp ?? dayjs().format("YYYY-MM-DD"));
  const [month, setMonth] = useState(receiptMonth.month);
  const [isMonthSheetOpen, setIsMonthSheetOpen] = useState(initialMonthSheetOpen);
  const months = recentMonths(today);
  // TODO: 다른 달을 고르면 API 22(?month=)로 그 달 값을 받는다. 지금은 받은 달만 그린다
  const data = month === receiptMonth.month ? receiptMonth : null;
  const isEmpty = !data || data.weeks.length === 0;

  return (
    <>
      <div className={styles.summary}>
        <button
          type="button"
          className={styles.monthButton}
          aria-haspopup="dialog"
          onClick={() => setIsMonthSheetOpen(true)}
        >
          {formatYearMonth(`${month}-01`)}
          <AltArrowDownIcon className={styles.monthArrow} aria-hidden="true" />
        </button>
        {!isEmpty && data && (
          <>
            <p className={styles.total}>
              {formatNumber(data.totalAmount)}
              <span className={styles.totalUnit}>원</span>
            </p>
            {data.averageDiffAmount !== null && (
              <p className={styles.average}>{formatAverageDiff(data.averageDiffAmount)}</p>
            )}
          </>
        )}
      </div>

      {isEmpty ? (
        // 피그마 p3-영수증-빈 리스트
        <EmptyState className={styles.empty}>
          {Number(month.slice(5, 7))}월에 등록한 영수증이 없어요
        </EmptyState>
      ) : (
        <div className={styles.weeks}>
          {data.weeks.map((week) => (
            <section key={week.startDate} className={styles.week} aria-label={week.label}>
              <div className={styles.weekHeader}>
                <p className={styles.weekLabel}>
                  {week.label}
                  <span className={styles.weekRange}>
                    {formatDateRange(week.startDate, week.endDate)}
                  </span>
                </p>
                <p className={styles.weekTotal}>{formatWon(week.totalAmount)}</p>
              </div>
              <ul className={styles.receipts}>
                {week.receipts.map((receipt) => (
                  <li key={receipt.id}>
                    <Link href={`${RECEIPTS_PATH}/${receipt.id}`} className={styles.receipt}>
                      <StoreBadge storeCode={receipt.storeCode} decorative />
                      <span className={styles.receiptText}>
                        <span className={styles.storeName}>{receipt.storeName}</span>
                        <span className={styles.receiptMeta}>
                          {formatPurchaseDay(receipt.purchasedOn)} 식재료 {receipt.itemCount}개
                        </span>
                      </span>
                      <span className={styles.receiptAmount}>{formatWon(receipt.totalAmount)}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}

      {/* 피그마 p3-영수증-날짜 선택 */}
      {isMonthSheetOpen && (
        <ReceiptMonthSheet
          month={month}
          months={months}
          onClose={() => setIsMonthSheetOpen(false)}
          onConfirm={(value) => {
            setMonth(value);
            setIsMonthSheetOpen(false);
          }}
        />
      )}
    </>
  );
}
