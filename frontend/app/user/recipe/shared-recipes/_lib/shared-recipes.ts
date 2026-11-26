// 금주의 인기 레시피(p4.1.4) 값. 모양은 API 50(GET /user/shared-recipes/ranking) 응답을 따랐다.
// TODO: API 50을 붙이면 서버 값을 쓴다. 순위·순위 변동·주차 문구·검색은 서버가 정해 준다.

import { dayjs } from "@/lib/dayjs";

export type RankingSort = "popular" | "latest";

export const RANKING_SORT_LABEL: Record<RankingSort, string> = {
  popular: "인기순",
  latest: "최신순",
};

/** 랭킹 한 줄(API 50 content[]) */
export type RankingItem = {
  /** 공유 글 ID */
  id: number;
  /** 인기순 순위. 최신순이면 null("—") */
  rank: number | null;
  /** 지난주 순위 - 이번 주 순위(양수 상승·음수 하락). 지난주 순위가 없거나 같거나 최신순이면 null("—") */
  rankChange: number | null;
  title: string;
  /** 공유 사진 주소(로그인 쿠키 필요) */
  imageUrl: string;
  ingredientSummary: string;
  extraIngredientCount: number;
  /** 공유한 시각(ISO 8601, +09:00) */
  sharedAt: string;
};

export type RankingPage = {
  /** 서버 문구. 예: "11월 셋째 주" */
  weekLabel: string;
  content: RankingItem[];
};

/** 공유한 시간: "1분 전" ~ "59분 전", "1시간 전" ~ "23시간 전", "1일 전" ~ "30일 전"(domain-rules 7) */
export function formatSharedAgo(sharedAt: string, now: string): string {
  const minutes = Math.max(1, dayjs(now).diff(dayjs(sharedAt), "minute"));
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  return `${Math.min(30, Math.floor(hours / 24))}일 전`;
}

/** 미리보기용 지금 시각(최신순 "N분 전" 계산) */
export const SAMPLE_NOW = "2026-11-25T12:00:00+09:00";

const SAMPLE_ROWS = [
  ["핫 치킨 치즈 포케", "sample-rank-poke", "닭가슴살 · 청상추 · 계란", 3],
  ["비건 유니 짜장", "sample-rank-jjajang", "콩 · 오이 · 춘장", 5],
  ["치즈 오믈렛", "sample-rank-omelet", "계란 · 모짜렐라 치즈 · 당근", 2],
  ["오픈! 전 남친 토스트", "sample-rank-toast", "토스트 식빵 · 블루베리 잼", 3],
  ["다이어트 직빵 마녀 스프", "sample-rank-soup", "감자 · 매운 카레 가루 · 당근", 5],
  ["5분 컷 마라 수혈", "sample-rank-mala", "계란 · 모짜렐라 치즈 · 당근", 3],
] as const;

/** 피그마 p4.1.4 순위 변동: 10 상승, 1 하락, 3 상승, 4 상승, 1 하락, 2 하락 */
const SAMPLE_CHANGES = [10, -1, 3, 4, -1, -2];

/** 미리보기용 인기순(피그마 p4.1.4 문구) */
export const SAMPLE_RANKING: RankingPage = {
  weekLabel: "11월 셋째 주",
  content: SAMPLE_ROWS.map(([title, image, summary, extra], index) => ({
    id: 201 + index,
    rank: index + 1,
    rankChange: SAMPLE_CHANGES[index] ?? null,
    title,
    imageUrl: `/images/recipe/${image}.webp`,
    ingredientSummary: summary,
    extraIngredientCount: extra,
    sharedAt: SAMPLE_NOW,
  })),
};

/** 피그마 p4.1.4-최신순 공유 시각: 1분·2분·2분·5분·59분·1시간·23시간·1일·1일·30일 전 */
const SAMPLE_AGO_MINUTES = [1, 2, 2, 5, 59, 60, 23 * 60, 24 * 60, 24 * 60, 30 * 24 * 60];

/** 미리보기용 최신순(피그마 p4.1.4-최신순): 순위·변동 없이 공유 시각 */
export const SAMPLE_LATEST: RankingPage = {
  weekLabel: "11월 셋째 주",
  content: SAMPLE_AGO_MINUTES.map((minutes, index) => {
    const [title, image, summary, extra] = SAMPLE_ROWS[index % SAMPLE_ROWS.length]!;
    return {
      id: 301 + index,
      rank: null,
      rankChange: null,
      title,
      imageUrl: `/images/recipe/${image}.webp`,
      ingredientSummary: summary,
      extraIngredientCount: extra,
      sharedAt: dayjs(SAMPLE_NOW).subtract(minutes, "minute").format(),
    };
  }),
};
