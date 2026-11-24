"use client";

import Image from "next/image";
import Link from "next/link";

import { ArrowRightIcon } from "@solar-icons/react/linear";

import type { ExpiryStatus } from "@/components/ExpiryLabel/ExpiryLabel";
import { Skeleton } from "@/components/Skeleton/Skeleton";

import { ExpirySummary } from "../ExpirySummary/ExpirySummary";

import styles from "./FridgeHero.module.css";

type FridgeHeroProps = {
  total: number;
  counts: Record<ExpiryStatus, number>;
  selectedStatus: ExpiryStatus | null;
  onSelectStatus: (status: ExpiryStatus | null) => void;
  isLoading?: boolean;
};

/** p2 냉장고 위쪽: "냉장고에 재료가 N개 있어요", 레시피 생성 링크, 얼음 장식, 요약 숫자 카드. */
export function FridgeHero({
  total,
  counts,
  selectedStatus,
  onSelectStatus,
  isLoading = false,
}: FridgeHeroProps) {
  return (
    <section className={styles.hero} aria-labelledby="fridge-hero-title">
      {/* 피그마 얼음 장식 4개(꾸밈용) */}
      <div className={styles.ice} aria-hidden="true">
        {[1, 2, 3, 4].map((n) => (
          <Image
            key={n}
            src="/images/fridge/ice.webp"
            alt=""
            width={372}
            height={372}
            className={styles.iceItem}
            data-ice={n}
            priority={n === 1}
          />
        ))}
      </div>

      <div className={styles.copy}>
        {isLoading ? (
          <>
            <h1 id="fridge-hero-title" className={styles.srOnly}>
              냉장고
            </h1>
            <Skeleton width="7.5625rem" height="2.375rem" />
          </>
        ) : (
          <h1 id="fridge-hero-title" className={styles.title} data-empty={total === 0 || undefined}>
            {total === 0 ? (
              <>
                냉장고가
                <br />
                비어 있어요
              </>
            ) : (
              <>
                냉장고에 재료가
                <br />
                {total}개 있어요
              </>
            )}
          </h1>
        )}
        <Link href="/user/recipe/saved-recipes" className={styles.recipeLink}>
          AI를 활용해 레시피 생성하기
          <ArrowRightIcon className={styles.arrow} aria-hidden="true" />
        </Link>
      </div>

      <div className={styles.summary}>
        <ExpirySummary
          counts={counts}
          selected={selectedStatus}
          onSelect={onSelectStatus}
          isLoading={isLoading}
        />
      </div>
    </section>
  );
}
