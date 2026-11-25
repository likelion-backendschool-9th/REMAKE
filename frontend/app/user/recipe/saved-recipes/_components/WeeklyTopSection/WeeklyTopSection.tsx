import Image from "next/image";
import Link from "next/link";

import { EmptyState } from "@/components/EmptyState/EmptyState";
import { Skeleton } from "@/components/Skeleton/Skeleton";

import {
  formatIngredientSummary,
  SHARED_RECIPES_PATH,
  sharedRecipePath,
} from "../../../_lib/recipe";
import type { WeeklyTopItem } from "../../_lib/saved-recipes";

import styles from "./WeeklyTopSection.module.css";

/** 스켈레톤 카드 수(피그마 p4.1-레시피-스켈레톤) */
const SKELETON_CARDS = 3;

/**
 * 금주의 레시피(API 49): 제목·전체 보기(→ p4.1.4), 카드 최대 3장(순위·사진·제목·재료 요약 +N)을 옆으로 넘긴다.
 * items가 없으면 불러오는 중(사진 자리 회색), 0장이면 빈 상태(피그마 p4.1-레시피-빈 리스트).
 */
export function WeeklyTopSection({ items }: { items?: WeeklyTopItem[] }) {
  return (
    <section className={styles.section} aria-labelledby="weekly-top-title">
      <div className={styles.header}>
        <h2 id="weekly-top-title" className={styles.title}>
          금주의 레시피
        </h2>
        <Link href={SHARED_RECIPES_PATH} className={styles.more}>
          전체 보기
        </Link>
      </div>

      {!items ? (
        <div className={styles.cards} aria-busy="true">
          {Array.from({ length: SKELETON_CARDS }, (_, index) => (
            <Skeleton key={index} width="8.75rem" height="8.75rem" className={styles.skeleton} />
          ))}
        </div>
      ) : items.length === 0 ? (
        <EmptyState className={styles.empty}>이번 주 공유된 레시피가 아직 없어요</EmptyState>
      ) : (
        <ol className={styles.cards}>
          {items.map((item) => (
            <li key={item.id} className={styles.cardItem}>
              <Link href={sharedRecipePath(item.id)} className={styles.card}>
                <span className={styles.photo}>
                  {/* 공유 사진 API는 로그인 쿠키가 필요해 Next 이미지 최적화(서버에서 받아 옴)를 거치지 않는다 */}
                  <Image
                    src={item.imageUrl}
                    alt=""
                    fill
                    sizes="8.75rem"
                    unoptimized
                    className={styles.image}
                  />
                  <span className={styles.medal}>
                    <span aria-hidden="true">{item.rank}</span>
                    <span className={styles.srOnly}>{item.rank}위</span>
                  </span>
                </span>
                <span className={styles.name}>{item.title}</span>
                <span className={styles.ingredients}>
                  {formatIngredientSummary(item.ingredientSummary, item.extraIngredientCount)}
                </span>
              </Link>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
