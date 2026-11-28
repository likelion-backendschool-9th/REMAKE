// 백엔드 오류 code → 화면 문구. 문구는 docs/agent/spec/errors.md의 제안 문구를 따른다(화면에서 바꿀 수 있고 code는 바뀌지 않음).
// 화면은 서버 message가 아니라 이 표를 쓴다. 모르는 code는 INTERNAL_ERROR 문구로 보인다(blueprint §10).
// 보류 중인 code(ACCOUNT_SUSPENDED)와 화면에 보이지 않는 code(TOKEN_EXPIRED, EVENT_BATCH_TOO_LARGE)만 넣지 않는다.

import { isApiError } from "@/lib/api/errors";

const FALLBACK = "문제가 생겼어요. 잠시 후 다시 시도해 주세요";

export const ERROR_MESSAGES: Record<string, string> = {
  // 공통
  INVALID_INPUT: "입력한 내용을 확인해 주세요",
  UNAUTHORIZED: "로그인이 필요해요",
  RATE_LIMITED: "잠시 후 다시 시도해 주세요",
  INTERNAL_ERROR: FALLBACK,
  SERVICE_UNAVAILABLE: "서비스 점검 중이에요",
  NETWORK_OFFLINE: "인터넷 연결을 확인해 주세요",
  CLIENT_TIMEOUT: "응답이 늦어지고 있어요. 다시 시도해 주세요",
  NOT_FOUND: FALLBACK,
  METHOD_NOT_ALLOWED: FALLBACK,
  NOT_ACCEPTABLE: FALLBACK,
  UNSUPPORTED_MEDIA_TYPE: FALLBACK,

  // 로그인·설정
  OAUTH_FAILED: "로그인에 실패했어요. 다시 시도해 주세요",
  OAUTH_PROVIDER_UNSUPPORTED: "지원하지 않는 로그인 방식이에요",
  REFRESH_TOKEN_INVALID: "로그인 시간이 만료됐어요. 다시 로그인해 주세요",
  REAUTH_ACCOUNT_MISMATCH: "지금 로그인한 계정으로 다시 인증해 주세요",
  ACCOUNT_EMAIL_MISSING: "인증번호를 보낼 이메일이 없어요",
  DELETION_CODE_INVALID: "인증번호가 맞지 않아요",
  DELETION_CODE_EXPIRED: "인증번호 유효 시간이 지났어요. 다시 받아 주세요",
  DELETION_CODE_LOCKED: "입력 횟수를 넘었어요. 인증번호를 다시 받아 주세요",
  ALREADY_SUBSCRIBED: "이미 구독 중이에요",
  SUBSCRIPTION_NOT_FOUND: "구독 중인 멤버십이 없어요",
  BILLING_AUTH_FAILED: "카드 등록에 실패했어요. 다시 시도해 주세요",
  PAYMENT_FAILED: "결제에 실패했어요. 다른 카드로 다시 시도해 주세요",
  PAYMENT_UPSTREAM_ERROR: "결제 서비스에 문제가 있어요. 잠시 후 다시 시도해 주세요",

  // 냉장고
  INGREDIENT_NOT_FOUND: "재료를 찾을 수 없어요",
  INSUFFICIENT_QUANTITY: "가지고 있는 양보다 많아요",
  INGREDIENT_CATEGORY_NOT_FOUND: "선택할 수 없는 카테고리예요",

  // 영수증
  RECEIPT_NOT_RECOGNIZED: "영수증을 인식하지 못했어요. 다시 찍어 주세요",
  OCR_UPSTREAM_ERROR: "영수증을 분석하지 못했어요. 잠시 후 다시 시도해 주세요",
  OCR_TIMEOUT: "분석이 오래 걸리고 있어요. 다시 시도해 주세요",
  RECEIPT_NO_FOOD_ITEMS: "식재료가 없는 영수증이에요",
  RECEIPT_TOO_OLD: "6개월이 지난 영수증은 등록할 수 없어요",
  RECEIPT_DUPLICATE: "이미 등록한 영수증이에요",
  RECEIPT_NOT_FOUND: "영수증을 찾을 수 없어요",
  FILE_TOO_LARGE: "파일이 너무 커요 (최대 10MB)",
  UNSUPPORTED_FILE_TYPE: "지원하지 않는 파일 형식이에요",
  CAMERA_UNAVAILABLE: "카메라를 쓸 수 없어요. 갤러리에서 골라 주세요",

  // AI 레시피
  GENERATION_LIMIT_EXCEEDED: "오늘의 레시피 생성 횟수를 모두 사용했어요",
  NO_INGREDIENTS:
    "냉장고 재료가 부족해서 레시피를 만들지 못했어요. 설정에서 레시피 재료 보충을 켜 보세요",
  RECIPE_NOT_FOUND: "레시피를 찾을 수 없어요",
  COOK_ITEM_INVALID: "차감할 재료를 확인해 주세요",
  SAVED_RECIPE_NOT_FOUND: "저장된 레시피가 아니에요",
  AI_UPSTREAM_ERROR: "레시피를 만들지 못했어요. 횟수는 차감되지 않았어요",
  AI_TIMEOUT: "생성이 오래 걸리고 있어요. 횟수는 차감되지 않았어요",
  AI_INVALID_RESPONSE: "레시피를 만들지 못했어요. 횟수는 차감되지 않았어요",

  // 식단표
  MEAL_PLAN_LIMIT_EXCEEDED: "이번 달 식단표 생성 횟수를 모두 사용했어요",
  MEAL_PLAN_NO_INGREDIENTS:
    "냉장고 재료가 부족해 식단표를 만들지 못했어요. 생성 횟수는 차감되지 않았어요",
  MEAL_PLAN_OVERLAP: "이미 식단이 있는 날짜가 있어요. 덮어쓸까요?",
  PAST_DATE_NOT_ALLOWED: "지난 날짜는 바꿀 수 없어요",
  SLOT_NOT_FOUND: "식단을 찾을 수 없어요",
  MEAL_PLAN_IN_PROGRESS: "식단표를 만들고 있어요. 완성되면 알림을 보내드려요",

  // 공유·장바구니
  CART_ITEM_NOT_FOUND: "이미 삭제된 항목이에요",
  RECIPE_NOT_SHAREABLE: "공유할 수 없는 레시피예요",
  ALREADY_SHARED: "이번 주에 이미 공유한 레시피예요",
  PHOTO_REQUIRED: "실제로 요리한 사진을 올려 주세요",
  SHARED_RECIPE_NOT_FOUND: "삭제되었거나 볼 수 없는 레시피예요",
  REPORT_ALREADY_EXISTS: "이미 신고한 레시피예요",

  // 운영자
  FORBIDDEN: "운영자만 들어올 수 있어요",
  ADMIN_REASON_REQUIRED: "조정 사유를 입력해 주세요",
  REPORT_ALREADY_HANDLED: "다른 운영자가 이미 처리한 신고예요",
  USER_NOT_FOUND: "회원을 찾을 수 없어요",
  REPORT_NOT_FOUND: "신고를 찾을 수 없어요",
};

/** code로 문구를 찾는다. 모르는 code는 INTERNAL_ERROR 문구 */
export function getErrorMessageByCode(code: string): string {
  return ERROR_MESSAGES[code] ?? FALLBACK;
}

/** 잡은 오류(ApiError가 아니어도)를 화면 문구로 바꾼다. traceId는 문의 상세에만 보이고 본문 문구에는 넣지 않는다. */
export function getErrorMessage(error: unknown): string {
  return isApiError(error) ? getErrorMessageByCode(error.code) : FALLBACK;
}
