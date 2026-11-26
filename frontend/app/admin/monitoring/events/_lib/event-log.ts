// a6 이벤트 로그 화면 값. 모양은 피그마와 이벤트 정의서(events 테이블) 기준이고,
// API 64 `GET /api/v1/admin/events` 응답에 맞춰 연동할 때 이름을 맞춘다. 회원은 내부 번호(user_id)만 보이고 개인 정보는 기록하지 않는다(AGENTS.md 제품 규칙 3)

export type EventSource = "CLIENT" | "SERVER";

export type EventLogItem = {
  id: number;
  /** 이벤트 이름. 예: "recipe_generated" */
  eventName: string;
  /** 이벤트 정의서 번호와 설명. 예: "E40", "레시피 생성·다시 받기 성공" */
  eventCode: string;
  eventDescription: string;
  userId: number | null;
  sessionId: string;
  source: EventSource;
  /** ISO 8601(+09:00) */
  occurredAt: string;
  /** 소문자 화면 코드. 예: "recipe_loading" */
  screen: string;
  platform: string;
  appVersion: string;
  /** 시연용 시드 데이터 여부 */
  isSeed: boolean;
  properties: Record<string, string | number | boolean>;
};

/** 목록 "주요 속성" 칸: "key=value, key=value" */
export function formatPropertySummary(properties: EventLogItem["properties"]): string {
  return Object.entries(properties)
    .map(([key, value]) => `${key}=${String(value)}`)
    .join(", ");
}

/** 개발용 미리보기(app/dev)에서만 쓰는 피그마 예시 값 */
export const SAMPLE_EVENT_LOGS: EventLogItem[] = [
  {
    id: 8,
    eventName: "recipe_generated",
    eventCode: "E40",
    eventDescription: "레시피 생성·다시 받기 성공",
    userId: 10233,
    sessionId: "s_8f2a…",
    source: "SERVER",
    occurredAt: "2026-11-24T15:12:08+09:00",
    screen: "recipe_loading",
    platform: "TWA",
    appVersion: "1.0.0",
    isSeed: false,
    properties: {
      recipe_mode: "AUTO",
      duration_ms: 8120,
      is_regenerated: false,
      expiring_used_count: 2,
      shortage_count: 1,
      remaining_quota: 1,
    },
  },
  {
    id: 7,
    eventName: "receipt_scan_failed",
    eventCode: "E31",
    eventDescription: "영수증 인식 실패",
    userId: 10412,
    sessionId: "s_51c0…",
    source: "SERVER",
    occurredAt: "2026-11-24T15:11:52+09:00",
    screen: "receipt_loading",
    platform: "TWA",
    appVersion: "1.0.0",
    isSeed: false,
    properties: { error_code: "RECEIPT_TOO_OLD" },
  },
  {
    id: 6,
    eventName: "ingredient_added",
    eventCode: "E22",
    eventDescription: "재료 등록",
    userId: 10098,
    sessionId: "s_77de…",
    source: "CLIENT",
    occurredAt: "2026-11-24T15:10:31+09:00",
    screen: "ingredient_form",
    platform: "WEB",
    appVersion: "1.0.0",
    isSeed: false,
    properties: { add_source: "manual", category: "VEGETABLE" },
  },
  {
    id: 5,
    eventName: "shared_recipe_reported",
    eventCode: "E74",
    eventDescription: "공유 레시피 신고",
    userId: 10233,
    sessionId: "s_8f2a…",
    source: "SERVER",
    occurredAt: "2026-11-24T15:09:47+09:00",
    screen: "shared_detail",
    platform: "TWA",
    appVersion: "1.0.0",
    isSeed: false,
    properties: { report_reason: "AD_TITLE" },
  },
  {
    id: 4,
    eventName: "notification_banner_clicked",
    eventCode: "E67",
    eventDescription: "알림 띠 누름",
    userId: 10377,
    sessionId: "s_0b9e…",
    source: "CLIENT",
    occurredAt: "2026-11-24T15:08:15+09:00",
    screen: "fridge",
    platform: "TWA",
    appVersion: "1.0.0",
    isSeed: false,
    properties: { notification_type: "EXPIRY_SOON" },
  },
  {
    id: 3,
    eventName: "cook_completed",
    eventCode: "E49",
    eventDescription: "요리 완료",
    userId: 10156,
    sessionId: "s_c3a1…",
    source: "SERVER",
    occurredAt: "2026-11-24T15:07:02+09:00",
    screen: "recipe_detail",
    platform: "TWA",
    appVersion: "1.0.0",
    isSeed: false,
    properties: { from_meal_plan: true },
  },
  {
    id: 2,
    eventName: "error_shown",
    eventCode: "E03",
    eventDescription: "오류 안내 표시",
    userId: 10533,
    sessionId: "s_9d44…",
    source: "CLIENT",
    occurredAt: "2026-11-24T15:05:40+09:00",
    screen: "meal_plan",
    platform: "WEB",
    appVersion: "1.0.0",
    isSeed: false,
    properties: { error_code: "NETWORK_ERROR" },
  },
  {
    id: 1,
    eventName: "login_succeeded",
    eventCode: "E09",
    eventDescription: "로그인 성공",
    userId: 10501,
    sessionId: "s_e612…",
    source: "SERVER",
    occurredAt: "2026-11-24T15:04:11+09:00",
    screen: "login",
    platform: "TWA",
    appVersion: "1.0.0",
    isSeed: false,
    properties: { provider: "KAKAO" },
  },
];
