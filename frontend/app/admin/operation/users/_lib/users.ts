export type UserFilter = "ALL" | "SANCTIONED";

export type UserSanction = {
  level: number;
  type: "SHARING" | "ACCOUNT" | "PERMANENT";
  startsAt: string;
  endsAt?: string;
  sharedRecipeTitle: string;
};

/** 마크업용 표시 모델. API 연결 시 응답을 이 모델로 변환한다. */
export type AdminUser = {
  userId: number;
  provider: "KAKAO" | "GOOGLE" | "NAVER";
  joinedAt: string;
  plan: "BASIC" | "PREMIUM";
  violationCount: number;
  deletedAt?: string;
  quota: { todayRemaining: number; rewardRemaining: number; blocked: boolean };
  currentSanction?: UserSanction;
  sanctions: UserSanction[];
};

export const PROVIDER_LABELS = { KAKAO: "카카오", GOOGLE: "구글", NAVER: "네이버" } as const;

export function sanctionLabel(type: UserSanction["type"], isDetail = false) {
  if (type === "PERMANENT") return "계정 정지(영구)";
  if (type === "ACCOUNT") return "계정 정지";
  return isDetail ? "레시피 공유 정지" : "공유 정지";
}
