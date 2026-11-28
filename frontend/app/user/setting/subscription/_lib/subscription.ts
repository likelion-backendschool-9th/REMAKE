// 구독 관리(p1.3.4) 화면 값과 문구.
// TODO: API 57(GET /user/subscription)을 붙이면 SAMPLE_SUBSCRIPTION 대신 서버 값을 쓴다.

export type SubscriptionStatus = "NONE" | "ACTIVE" | "CANCELED";

export type Subscription = {
  status: SubscriptionStatus;
  /** 원 단위 정수 */
  price: number;
  /** "2026-12-22". 구독 전이면 "오늘 구독하면 다음 결제일"(오늘 + 1달) */
  nextBillingDate: string;
};

/** 마크업 확인용 값(피그마 p1.3.4와 같음). 미리보기(app/dev)에서만 쓴다 */
export const SAMPLE_SUBSCRIPTION: Subscription = {
  status: "NONE",
  price: 9900,
  nextBillingDate: "2026-12-22",
};

/** 혜택 2개(피그마 p1.3.4 문구 그대로) */
export const BENEFITS = [
  {
    image: "/images/subscription/benefit-recipe.png",
    /** 그림 크기(px). 50×50 칸 안에 가운데 둔다(피그마 11/27: 레시피 50, 식단표 40) */
    imageSize: 50,
    label: "혜택 1.",
    name: "프리미엄",
    highlight: "PLUS",
    description:
      "하루 2회만 가능했던 나만의 맞춤 레시피 추천,\n이제 하루 최대 4회까지 더 풍성하게 받아볼 수 있습니다",
  },
  {
    image: "/images/subscription/benefit-meal-plan.png",
    imageSize: 40,
    label: "혜택 2.",
    name: "맥스",
    highlight: "MENUS",
    description:
      "월 1회(최대 5일) 제공되던 식단 관리에서 벗어나,\n매주 7일 치 맞춤 식단표를 월 4회까지 자유롭게 생성할 수 있습니다",
  },
] as const;

// "자세히 보기" 약관 드로워의 결제 약관 본문(11/24 확정). 원문 docs/developer/payment-terms.md
export { PAYMENT_TERMS } from "@/lib/legal/payment-terms";
