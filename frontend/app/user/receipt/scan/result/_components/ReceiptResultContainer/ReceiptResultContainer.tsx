"use client";

import { useId, useState } from "react";
import { useRouter } from "next/navigation";

import { AddIcon } from "@solar-icons/react/linear";

import { Button } from "@/components/Button/Button";
import { ExpiryLabel } from "@/components/ExpiryLabel/ExpiryLabel";
import { IconButton } from "@/components/IconButton/IconButton";
import { IngredientRow } from "@/components/IngredientRow/IngredientRow";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { todayInKst } from "@/lib/dayjs";
import { formatWon } from "@/lib/format";
import { formatAmount, getCategoryImage, getStorage } from "@/lib/ingredients";

import { StoreBadge } from "../../../../_components/StoreBadge/StoreBadge";
import {
  EXPIRY_NOTICE,
  formatScanPurchase,
  getExpiry,
  SCAN_ITEM_NEW_PATH,
  SCAN_PATH,
  scanItemEditHref,
  type ScanResult,
  sumScanPrice,
} from "../../../_lib/scan";

import styles from "./ReceiptResultContainer.module.css";

type ReceiptResultContainerProps = {
  /** OCR 결과(API 20). 없으면 분석 중(최대 30초). 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  result?: ScanResult;
  /** 오늘("2026-11-24"). 미리보기용, 기본은 화면을 연 날(소비기한 D-N 계산) */
  today?: string;
};

/**
 * p3.3 OCR 결과("영수증"). 위 회색 영역에 영수증 정보(마트·구매일·총 금액), 아래에 냉장고에 추가할 식재료 목록
 * (보관 위치·양·가격, 소비기한, > 수정)과 소비기한 추정 안내. "+"는 재료 추가(p3.3.2), 아래 "영수증 다시 찍기"는 재촬영.
 * 등록 전에는 아무것도 저장하지 않는다. 등록하지 않고 나가면 결과를 버린다(blueprint Receipt flow).
 * 상태: 기본(p3.3-ocr 결과) / 분석 중(p3.3-ocr 결과-스켈레톤). 피그마 11/27 개편으로 ⋮ 시트는 없어졌다.
 */
export function ReceiptResultContainer({ result, today }: ReceiptResultContainerProps) {
  const router = useRouter();
  const titleId = useId();
  const itemCount = result?.items.length ?? 0;

  return (
    <>
      <div className={styles.summaryArea}>
        <SubHeader title="영수증" className={styles.header} />
        {result ? <ScanSummary result={result} /> : <ScanSummarySkeleton />}
      </div>

      <main className={styles.main} aria-busy={!result || undefined}>
        <section aria-labelledby={titleId}>
          <div className={styles.sectionHead}>
            <h2 id={titleId} className={styles.sectionTitle}>
              냉장고에 추가 할 식재료
              {result && <span className={styles.count}>{itemCount}</span>}
            </h2>
            <IconButton
              icon={<AddIcon />}
              iconSize={20}
              label="식재료 추가"
              disabled={!result}
              onClick={() => router.push(SCAN_ITEM_NEW_PATH)}
            />
          </div>
          {result ? (
            <ResultItems result={result} today={today} />
          ) : (
            // 피그마 p3.3-ocr 결과-스켈레톤: 재료 줄 자리 2개(높이 42, 사이 5)
            <div className={styles.skeletons}>
              <Skeleton width="100%" height="2.625rem" />
              <Skeleton width="100%" height="2.625rem" />
            </div>
          )}
        </section>
      </main>

      <div className={styles.footer}>
        {/* TODO: 재촬영은 지금 OCR 결과를 버린다(receipt_result_abandoned). 확인 모달이 필요한지는 피그마 대기 */}
        <button type="button" className={styles.rescan} onClick={() => router.replace(SCAN_PATH)}>
          영수증 다시 찍기
        </button>
        {/* TODO: 장바구니 차감 시트(#57) → API 21 등록 */}
        <Button disabled={!result || itemCount === 0}>식재료 {itemCount}개 등록 하기</Button>
      </div>
    </>
  );
}

/** 위 회색 영역: 마트(배지 + 이름), 구매일, 구분선 아래 총 금액(품목 가격 합) */
function ScanSummary({ result }: { result: ScanResult }) {
  return (
    <div className={styles.summary}>
      <div className={styles.store}>
        <StoreBadge storeCode={result.storeCode} decorative={Boolean(result.storeName)} />
        {/* 매장 이름을 못 읽은 경우는 피그마에 없다. 문구는 임시 */}
        <p className={styles.storeName} data-empty={!result.storeName || undefined}>
          {result.storeName ?? "매장 이름을 읽지 못했어요"}
        </p>
      </div>
      <dl className={styles.facts}>
        <div className={styles.fact}>
          <dt>구매일</dt>
          <dd>
            {result.purchasedOn
              ? formatScanPurchase(result.purchasedOn, result.purchasedTime)
              : "-"}
          </dd>
        </div>
      </dl>
      <div className={styles.total}>
        <span className={styles.totalLabel}>총 금액</span>
        <span className={styles.totalAmount}>{formatWon(sumScanPrice(result.items))}</span>
      </div>
    </div>
  );
}

/** 피그마 p3.3-ocr 결과-스켈레톤: 배지·이름 자리, 구매일 값 자리, 총 금액 값 자리(글자 이름은 그대로) */
function ScanSummarySkeleton() {
  return (
    <div className={styles.summary}>
      <div className={styles.store} data-skeleton="true">
        <Skeleton width="2.75rem" height="2.75rem" />
        <Skeleton width="13rem" height="1.5rem" />
      </div>
      <dl className={styles.facts}>
        <div className={styles.fact}>
          <dt>구매일</dt>
          <dd>
            <Skeleton width="9.4375rem" height="1.25rem" />
          </dd>
        </div>
      </dl>
      <div className={styles.total}>
        <span className={styles.totalLabel}>총 금액</span>
        <Skeleton width="5.8125rem" height="1.625rem" />
      </div>
    </div>
  );
}

/** 값이 온 뒤의 목록. D-N을 셀 오늘 날짜는 이때 읽는다(cacheComponents) */
function ResultItems({ result, today: todayProp }: { result: ScanResult; today?: string }) {
  const [today] = useState(() => todayProp ?? todayInKst());

  return (
    <>
      <ul className={styles.items} aria-label="인식한 식재료">
        {result.items.map((item, index) => {
          const expiry = getExpiry(item.expirationDate, today);
          return (
            // OCR 품목은 id가 없다(API 20). 수정 화면도 순서로 찾는다
            <li key={index}>
              <IngredientRow
                name={item.name}
                imageSrc={getCategoryImage(item.categoryCode)}
                description={
                  <>
                    {getStorage(item.storage).place}
                    <Separator />
                    {item.amount === null ? (
                      // 양을 못 읽은 품목(api-summary #173). 수정 화면에서 넣는다
                      <span className={styles.unmeasured}>수량 측정 불가</span>
                    ) : (
                      formatAmount(item.amount, item.unit)
                    )}
                    {item.price !== null && (
                      <>
                        <Separator />
                        {formatWon(item.price)}
                      </>
                    )}
                  </>
                }
                trailing={<ExpiryLabel status={expiry.status} text={expiry.text} />}
                editHref={scanItemEditHref(index)}
                className={styles.item}
              />
            </li>
          );
        })}
      </ul>
      <p className={styles.notice}>{EXPIRY_NOTICE}</p>
    </>
  );
}

/** 설명 사이 가운데 점(피그마 지름 2 원). 읽을 때는 " · " */
function Separator() {
  return <span className={styles.separator}> · </span>;
}
