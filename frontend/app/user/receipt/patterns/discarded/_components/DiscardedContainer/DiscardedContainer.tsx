"use client";

import { GroupHeader } from "@/components/GroupHeader/GroupHeader";
import { IngredientRow } from "@/components/IngredientRow/IngredientRow";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { formatMonthDay, formatWon } from "@/lib/format";
import { getCategoryImage } from "@/lib/ingredients";

import type { DiscardedMonth } from "../../../_lib/patterns";

import styles from "./DiscardedContainer.module.css";

type DiscardedContainerProps = {
  /** 그 달 버린 식재료(API 없음, 확인 필요). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  discarded?: DiscardedMonth;
};

/**
 * p3.4.1 이번 달 버린 식재료. "N월에 버린 식재료" 금액(가격 있는 재료만), 버린 재료 목록(버린 날, 금액 또는 "가격 없음").
 * 금액 = 가격 × 버린 양 ÷ 처음 양(domain-rules 2). 나의 소비(p3.4)의 "N월 버린 식비"를 누르면 온다.
 */
export function DiscardedContainer({ discarded }: DiscardedContainerProps) {
  return (
    <>
      <SubHeader title="버린 식재료" className={styles.header} />
      <main className={styles.main} aria-busy={!discarded || undefined}>
        {discarded ? (
          <>
            <div className={styles.summary}>
              <p className={styles.month}>{Number(discarded.month.slice(5, 7))}월에 버린 식재료</p>
              <p className={styles.total}>{formatWon(discarded.totalAmount)}</p>
              <p className={styles.notice}>가격을 넣은 재료만 금액에 더해져요</p>
            </div>
            <section aria-label="버린 재료 목록" className={styles.list}>
              <GroupHeader title={`버린 재료 ${discarded.items.length}`} />
              <ul className={styles.items}>
                {discarded.items.map((item, index) => (
                  // 버린 기록 id가 화면 값에 없다(API 미정). 목록 순서는 바뀌지 않는다

                  <li key={index}>
                    <IngredientRow
                      name={item.name}
                      imageSrc={getCategoryImage(item.categoryCode)}
                      description={`${formatMonthDay(item.discardedOn)} 버림`}
                      trailing={
                        item.amount === null ? (
                          <span className={styles.noPrice}>가격 없음</span>
                        ) : (
                          <span className={styles.amount}>{formatWon(item.amount)}</span>
                        )
                      }
                    />
                  </li>
                ))}
              </ul>
            </section>
          </>
        ) : (
          // 피그마에 스켈레톤이 없어 요약 3줄·목록 2줄 자리만 둔다
          <div className={styles.skeleton}>
            <Skeleton width="7rem" height="1.25rem" />
            <Skeleton width="6rem" height="1.875rem" />
            <Skeleton width="100%" height="2.25rem" />
            <Skeleton width="100%" height="2.25rem" />
          </div>
        )}
      </main>
    </>
  );
}
