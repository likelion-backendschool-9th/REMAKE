"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";

import { Dropdown } from "@/components/Dropdown/Dropdown";
import { EmptyState } from "@/components/EmptyState/EmptyState";
import { RankChange } from "@/components/RankChange/RankChange";
import { SearchField } from "@/components/SearchField/SearchField";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";
import { dayjs } from "@/lib/dayjs";

import { formatIngredientSummary, sharedRecipePath } from "../../../_lib/recipe";
import {
  formatSharedAgo,
  RANKING_SORT_LABEL,
  type RankingItem,
  type RankingPage,
  type RankingSort,
} from "../../_lib/shared-recipes";

import styles from "./RankingContainer.module.css";

/** 순위를 굵게 보이는 위 칸 수(피그마: 1~3위 굵게) */
const TOP_RANKS = 3;

type RankingContainerProps = {
  /** 랭킹(API 50). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  ranking?: RankingPage;
  /** 처음 정렬. 미리보기용(피그마 p4.1.4-최신순), 기본 인기순 */
  initialSort?: RankingSort;
  /** 처음 검색어. 미리보기용(피그마 인풋 포커스·검색 결과 없음) */
  initialQuery?: string;
  /** 지금 시각(최신순 "N분 전"). 미리보기용, 기본은 화면을 연 시각 */
  now?: string;
};

/**
 * p4.1.4 금주의 인기 레시피. 주차("11월 셋째 주"), 정렬(인기순 기본·최신순), 검색, 줄(순위·사진·제목·재료 요약 +N·순위 변동).
 * 인기순은 이번 주 유효 저장 순 TOP 100, 최신순은 공유 시각순이고 순위 대신 "—"·오른쪽에 공유한 시간(domain-rules 7).
 * 줄을 누르면 공유 레시피 상세(p4.1.4.1).
 * 상태: 기본 / 인풋 포커스(검색) / 최신순 / 스켈레톤(실제 라우트 처음 상태) / 검색 결과 없음.
 */
export function RankingContainer({
  ranking,
  initialSort = "popular",
  initialQuery = "",
  now,
}: RankingContainerProps) {
  const [sort] = useState<RankingSort>(initialSort);
  const [query, setQuery] = useState(initialQuery);
  const keyword = query.trim();

  return (
    <>
      <SubHeader />
      <main className={styles.main}>
        <div className={styles.heading}>
          <div className={styles.titleBlock}>
            {ranking ? (
              <p className={styles.week}>{ranking.weekLabel}</p>
            ) : (
              <Skeleton width="3.5rem" height="1rem" />
            )}
            <h1 className={styles.title}>금주의 인기 레시피</h1>
          </div>
          {/* TODO: 정렬 시트(인기순·최신순)는 디자인 v2에 없어 보류. 디자인이 나오면 SelectSheet를 연다(API 50 ?sort=) */}
          <Dropdown className={styles.sort}>{RANKING_SORT_LABEL[sort]}</Dropdown>
        </div>

        <SearchField
          aria-label="레시피 검색"
          placeholder="레시피를 검색해 주세요"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          className={styles.search}
        />

        {ranking ? (
          <RankingList ranking={ranking} sort={sort} keyword={keyword} now={now} />
        ) : (
          // 피그마 p4.1.4-이번 주 레시피 랭킹-스켈레톤: 줄 자리 2개
          <div className={styles.skeletons} aria-busy="true">
            <Skeleton width="100%" height="2.5rem" />
            <Skeleton width="100%" height="2.5rem" />
          </div>
        )}
      </main>
    </>
  );
}

/** 값이 온 뒤의 목록. 최신순 "N분 전"을 셀 지금 시각은 이때 읽는다(cacheComponents) */
function RankingList({
  ranking,
  sort,
  keyword,
  now: nowProp,
}: {
  ranking: RankingPage;
  sort: RankingSort;
  keyword: string;
  now?: string;
}) {
  const [now] = useState(() => nowProp ?? dayjs().format());
  // TODO: 검색어는 API 50(?q=)으로 보낸다. 인기순은 TOP 100 안에서 제목 검색, 전체 순위를 유지한다
  const rows = keyword
    ? ranking.content.filter((item) => item.title.toLowerCase().includes(keyword.toLowerCase()))
    : ranking.content;

  if (rows.length === 0) {
    // 피그마 p4.1.4-이번 주 레시피 랭킹-검색 결과 없음. 검색 없이 비었을 때 문구는 피그마에 없어 따로 둔다
    return (
      <EmptyState className={styles.empty}>
        {keyword ? `'${keyword}' 검색 결과가 없어요` : "이번 주 공유된 레시피가 아직 없어요"}
      </EmptyState>
    );
  }

  return (
    <ol className={styles.rows}>
      {rows.map((item) => (
        <li key={item.id}>
          <Link href={sharedRecipePath(item.id)} className={styles.row}>
            <RankCell item={item} />
            <span className={styles.photo}>
              {/* 공유 사진 API는 로그인 쿠키가 필요해 Next 이미지 최적화를 거치지 않는다 */}
              <Image
                src={item.imageUrl}
                alt=""
                fill
                sizes="2.5rem"
                unoptimized
                className={styles.image}
              />
            </span>
            <span className={styles.text}>
              <span className={styles.name}>{item.title}</span>
              <span className={styles.ingredients}>
                {formatIngredientSummary(item.ingredientSummary, item.extraIngredientCount)}
              </span>
            </span>
            {sort === "latest" ? (
              <span className={styles.ago}>{formatSharedAgo(item.sharedAt, now)}</span>
            ) : (
              <ChangeCell change={item.rankChange} />
            )}
          </Link>
        </li>
      ))}
    </ol>
  );
}

/** 순위 칸: "1위"(1~3위 굵게) / 최신순은 "—" */
function RankCell({ item }: { item: RankingItem }) {
  if (item.rank === null) {
    return (
      <span className={styles.rank} data-empty="">
        <span aria-hidden="true">—</span>
        <span className={styles.srOnly}>순위 없음</span>
      </span>
    );
  }
  return (
    <span className={styles.rank} data-top={item.rank <= TOP_RANKS || undefined}>
      {item.rank}위
    </span>
  );
}

/** 순위 변동 칸: 파란 ↑N·빨간 ↓N, 지난주 순위가 없거나 같으면 "—" */
function ChangeCell({ change }: { change: number | null }) {
  if (change === null || change === 0) {
    return (
      <span className={styles.noChange}>
        <span aria-hidden="true">—</span>
        <span className={styles.srOnly}>순위 변동 없음</span>
      </span>
    );
  }
  return (
    <RankChange
      direction={change > 0 ? "up" : "down"}
      amount={Math.abs(change)}
      className={styles.change}
    />
  );
}
