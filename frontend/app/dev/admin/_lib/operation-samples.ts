// Figma a5·a7 복사본. 실제 라우트에서는 가져오지 않는다.
import type { AdminUser, UserSanction } from "../../../admin/operation/users/_lib/users";
import type { WeeklyRewardsData } from "../../../admin/operation/weekly-rewards/_lib/weekly-rewards";

const SHARING: UserSanction[] = [
  {
    level: 2,
    type: "SHARING",
    startsAt: "2026-11-24",
    endsAt: "2026-12-01",
    sharedRecipeTitle: "최저가 할인 링크 확인하세요!!",
  },
  {
    level: 1,
    type: "SHARING",
    startsAt: "2026-11-10",
    endsAt: "2026-11-17",
    sharedRecipeTitle: "초특가 마트 쿠폰 레시피",
  },
];
const PERMANENT: UserSanction[] = [
  {
    level: 4,
    type: "PERMANENT",
    startsAt: "2026-11-23",
    sharedRecipeTitle: "무료 체험 링크 공유 2",
  },
  {
    level: 3,
    type: "ACCOUNT",
    startsAt: "2026-11-20",
    endsAt: "2026-12-20",
    sharedRecipeTitle: "무료 체험 링크 공유",
  },
  {
    level: 2,
    type: "SHARING",
    startsAt: "2026-11-18",
    endsAt: "2026-12-18",
    sharedRecipeTitle: "할인 코드 레시피",
  },
  {
    level: 1,
    type: "SHARING",
    startsAt: "2026-11-16",
    endsAt: "2026-11-23",
    sharedRecipeTitle: "쿠폰 받는 법",
  },
];

function user(
  userId: number,
  provider: AdminUser["provider"],
  day: string,
  plan: AdminUser["plan"] = "BASIC",
  violationCount = 0,
): AdminUser {
  return {
    userId,
    provider,
    joinedAt: `2026-11-${day}`,
    plan,
    violationCount,
    quota: { todayRemaining: 2, rewardRemaining: 0, blocked: false },
    sanctions: [],
  };
}

export const WITHDRAWN_USER: AdminUser = {
  ...user(10044, "KAKAO", "01", "BASIC", 1),
  joinedAt: "2026-10-30",
  deletedAt: "2026-11-20",
  sanctions: [
    { level: 1, type: "SHARING", startsAt: "2026-11-02", sharedRecipeTitle: "할인 쿠폰 레시피" },
  ],
};

export const SAMPLE_USERS: AdminUser[] = [
  {
    ...user(10087, "KAKAO", "03", "BASIC", 2),
    quota: { todayRemaining: 2, rewardRemaining: 6, blocked: false },
    sanctions: SHARING,
    currentSanction: SHARING[0],
  },
  user(10233, "GOOGLE", "04", "PREMIUM"),
  user(10098, "NAVER", "04"),
  {
    ...user(10156, "KAKAO", "06", "BASIC", 3),
    quota: { todayRemaining: 0, rewardRemaining: 0, blocked: true },
    sanctions: PERMANENT.slice(1),
    currentSanction: PERMANENT[1],
  },
  user(10377, "KAKAO", "09", "PREMIUM"),
  user(10412, "GOOGLE", "12", "BASIC", 1),
  {
    ...user(10501, "NAVER", "15", "BASIC", 4),
    quota: { todayRemaining: 0, rewardRemaining: 0, blocked: true },
    sanctions: PERMANENT,
    currentSanction: PERMANENT[0],
  },
  user(10533, "KAKAO", "20"),
  WITHDRAWN_USER,
];

const weeks = ["2026-11-17", "2026-11-10", "2026-11-03"];
const top = [
  {
    rank: 1,
    sharedRecipeId: 1,
    title: "초간단 계란 볶음밥",
    saveCount: 128,
    authorId: 10233,
    reward: 10,
    granted: true,
  },
  {
    rank: 2,
    sharedRecipeId: 2,
    title: "남은 채소 카레",
    saveCount: 97,
    authorId: 10377,
    reward: 6,
    granted: true,
  },
  {
    rank: 3,
    sharedRecipeId: 3,
    title: "두부 강정",
    saveCount: 85,
    authorId: 10233,
    reward: 3,
    granted: true,
  },
];
export const SAMPLE_REWARDS: WeeklyRewardsData = {
  weeks,
  weeksData: [
    { weekStart: weeks[0], weekEnd: "2026-11-23", grantedAt: "2026-11-24", top },
    {
      weekStart: weeks[1],
      weekEnd: "2026-11-16",
      grantedAt: "2026-11-17",
      top: [{ ...top[0], saveCount: 12 }],
    },
    { weekStart: weeks[2], weekEnd: "2026-11-09", top: [] },
  ],
  history: [
    [10233, 10377, 10233],
    [10156, 10412, 10098],
    [10377, 10233, 10501],
  ].map((ids, index) => ({
    weekStart: weeks[index],
    winners: ids.map((userId, rank) => ({ rank: rank + 1, userId })),
    totalReward: 19,
  })),
};
export const EMPTY_REWARDS: WeeklyRewardsData = {
  ...SAMPLE_REWARDS,
  weeksData: SAMPLE_REWARDS.weeksData.map((week) => ({ ...week, top: [], grantedAt: undefined })),
  history: [],
};
export const FEWER_REWARDS: WeeklyRewardsData = {
  ...SAMPLE_REWARDS,
  weeksData: SAMPLE_REWARDS.weeksData.map((week) => ({
    ...week,
    top: [{ ...top[0], saveCount: 12 }],
  })),
  history: [],
};
