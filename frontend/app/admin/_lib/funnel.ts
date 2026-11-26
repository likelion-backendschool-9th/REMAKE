// a2 퍼널 화면 값(M17). 어드민 집계 API(#93)가 보류라 모양은 피그마 기준으로 잡았고, API가 정해지면 맞춘다.
// 전환율·이탈 큼 판단은 서버가 정한다(blueprint §1)

export type FunnelStep = {
  /** 예: "로그인 성공" */
  label: string;
  /** 단계를 세는 이벤트 이름. 예: "login_succeeded" */
  eventName: string;
  users: number;
  /** 이전 단계 대비 %. 첫 단계는 null → "-" */
  conversionRate: number | null;
  /** 이탈이 크면 빨간 글과 "이탈 큼" */
  isLargeDrop: boolean;
};

export type FunnelData = {
  /** 핵심 흐름: 로그인 → 요리 완료 */
  core: FunnelStep[];
  /** 영수증 경로: 영수증 스캔 → 재료 등록 */
  receipt: FunnelStep[];
};

/** 개발용 미리보기(app/dev)에서만 쓰는 피그마 예시 값 */
export const SAMPLE_FUNNEL: FunnelData = {
  core: [
    {
      label: "로그인 성공",
      eventName: "login_succeeded",
      users: 1000,
      conversionRate: null,
      isLargeDrop: false,
    },
    {
      label: "가입 완료",
      eventName: "signup_completed",
      users: 860,
      conversionRate: 86,
      isLargeDrop: false,
    },
    {
      label: "온보딩 완료",
      eventName: "onboarding_completed",
      users: 640,
      conversionRate: 74.4,
      isLargeDrop: false,
    },
    {
      label: "첫 재료 등록",
      eventName: "ingredient_added",
      users: 520,
      conversionRate: 81.3,
      isLargeDrop: false,
    },
    {
      label: "레시피 생성",
      eventName: "recipe_generated",
      users: 310,
      conversionRate: 59.6,
      isLargeDrop: true,
    },
    {
      label: "요리 완료",
      eventName: "cook_completed",
      users: 140,
      conversionRate: 45.2,
      isLargeDrop: true,
    },
  ],
  receipt: [
    {
      label: "영수증 스캔 시작",
      eventName: "receipt_scan_started",
      users: 420,
      conversionRate: null,
      isLargeDrop: false,
    },
    {
      label: "영수증 저장",
      eventName: "receipt_saved",
      users: 351,
      conversionRate: 83.6,
      isLargeDrop: false,
    },
    {
      label: "재료 등록",
      eventName: "ingredient_added",
      users: 348,
      conversionRate: 99.1,
      isLargeDrop: false,
    },
  ],
};
