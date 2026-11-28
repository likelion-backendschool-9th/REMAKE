// 개발용 미리보기(app/dev)에서만 쓰는 피그마 예시 값. 실제 주소는 API 63이 붙기 전까지 불러오는 중으로 보인다

import type { MetricsTab } from "../../../_lib/admin-menu";
import type { MetricsTabData } from "./metrics";

const DAYS = ["11/18", "11/19", "11/20", "11/21", "11/22", "11/23", "11/24"];

function daily(values: number[]) {
  return values.map((value, index) => ({ label: DAYS[index], value }));
}

export const SAMPLE_METRICS: Record<MetricsTab, MetricsTabData> = {
  registration: {
    cards: [
      {
        code: "M03",
        name: "영수증 OCR 성공률",
        value: "92.3%",
        caption: "목표 90% 이상",
        status: "achieved",
      },
      {
        code: "M04",
        name: "OCR 품목 수정률",
        value: "17%",
        caption: "목표 20% 이하",
        status: "achieved",
      },
      {
        code: "M05",
        name: "영수증 등록 시간(재료 1개당)",
        value: "6.2초",
        caption: "직접 입력 21.0초의 30% · 목표 50% 이하",
        status: "achieved",
      },
    ],
    tables: [
      {
        key: "receipt-scan",
        title: "영수증 스캔 결과",
        meta: "M03 · error_code별",
        columns: [
          { key: "result", label: "결과" },
          { key: "errorCode", label: "error_code", width: "12.5rem" },
          { key: "count", label: "건수", align: "right", width: "3.75rem" },
          { key: "ratio", label: "비율", align: "right", width: "3.75rem" },
        ],
        rows: [
          { result: "성공", errorCode: "-", count: "388", ratio: "88.2%" },
          {
            result: "영수증을 읽지 못함",
            errorCode: "RECEIPT_NOT_RECOGNIZED",
            count: "21",
            ratio: "4.8%",
          },
          {
            result: "OCR 오류·시간 초과",
            errorCode: "OCR_UPSTREAM_ERROR",
            count: "11",
            ratio: "2.5%",
          },
          {
            result: "식재료 없음(따로 셈)",
            errorCode: "RECEIPT_NO_FOOD_ITEMS",
            count: "9",
            ratio: "2.0%",
          },
          {
            result: "6개월 지남(따로 셈)",
            errorCode: "RECEIPT_TOO_OLD",
            count: "4",
            ratio: "0.9%",
          },
          {
            result: "이미 등록한 영수증(따로 셈)",
            errorCode: "RECEIPT_DUPLICATE",
            count: "7",
            ratio: "1.6%",
          },
        ],
        note: "식재료 없음·6개월 지남·이미 등록한 영수증은 OCR 품질과 따로 세요.",
      },
    ],
    daily: {
      title: "일별 영수증 등록 수",
      meta: "receipt_saved",
      items: daily([41, 48, 52, 47, 63, 71, 66]),
      "aria-label": "일별 영수증 등록 수",
    },
  },
  consumption: {
    cards: [
      {
        code: "M00",
        name: "소비기한 내 소진율",
        value: "68.4%",
        caption: "추세가 오르는지",
        status: "trend",
      },
      {
        code: "M07",
        name: "임박 재료 소진율",
        value: "72%",
        caption: "목표 70% 이상",
        status: "achieved",
      },
      {
        code: "M06",
        name: "임박 알림 확인율",
        value: "31%",
        caption: "목표 25% 이상",
        status: "achieved",
      },
    ],
    tables: [
      {
        key: "category-consumption",
        title: "카테고리별 소진",
        meta: "M00 · 사용 / 버림 / 기한 지남",
        columns: [
          { key: "category", label: "카테고리" },
          { key: "used", label: "사용", align: "right", width: "3.75rem" },
          { key: "discarded", label: "버림", align: "right", width: "3.75rem" },
          { key: "expired", label: "기한 지남", align: "right", width: "4.375rem" },
          { key: "ratio", label: "소진율", align: "right", width: "4.375rem" },
        ],
        rows: [
          { category: "채소", used: "412", discarded: "118", expired: "96", ratio: "65.8%" },
          { category: "육류", used: "230", discarded: "31", expired: "18", ratio: "82.4%" },
          { category: "달걀·유제품", used: "188", discarded: "22", expired: "25", ratio: "80.0%" },
          { category: "수산물", used: "74", discarded: "19", expired: "13", ratio: "69.8%" },
          { category: "과일", used: "121", discarded: "35", expired: "28", ratio: "65.8%" },
          { category: "가공식품", used: "96", discarded: "8", expired: "11", ratio: "83.5%" },
        ],
      },
    ],
    daily: {
      title: "일별 버린 금액",
      meta: "M18 · 원",
      items: daily([5200, 7400, 3100, 8800, 6200, 9100, 8400]),
      "aria-label": "일별 버린 금액",
    },
  },
  ai: {
    cards: [],
    tables: [
      {
        key: "ai-success",
        title: "AI 생성 성공률",
        meta: "M09 · 목표 95% 이상",
        columns: [
          { key: "kind", label: "종류" },
          { key: "tries", label: "시도", align: "right", width: "3.75rem" },
          { key: "failures", label: "실패", align: "right", width: "3.125rem" },
          { key: "rate", label: "성공률", align: "right", width: "7.5rem" },
        ],
        rows: [
          {
            kind: "레시피",
            tries: "1,240",
            failures: "39",
            rate: { text: "96.9%", badge: { tone: "safe", label: "달성" } },
          },
          {
            kind: "식단표",
            tries: "186",
            failures: "8",
            rate: { text: "95.7%", badge: { tone: "safe", label: "달성" } },
          },
          {
            kind: "식단 교체",
            tries: "92",
            failures: "2",
            rate: { text: "97.8%", badge: { tone: "safe", label: "달성" } },
          },
        ],
        note: "재료 부족(NO_INGREDIENTS)은 AI 품질과 따로 세요.",
      },
      {
        key: "ai-latency",
        title: "AI·OCR 응답 시간",
        meta: "M10",
        columns: [
          { key: "kind", label: "종류" },
          { key: "p50", label: "p50", align: "right", width: "3.75rem" },
          { key: "p95", label: "p95", align: "right", width: "8.125rem" },
        ],
        rows: [
          { kind: "레시피", p50: "8.2초", p95: "19.5초" },
          {
            kind: "식단표",
            p50: "21.4초",
            p95: { text: "38.0초", badge: { tone: "danger", label: "30초 넘음" } },
          },
          { kind: "식단 교체", p50: "6.1초", p95: "12.3초" },
          { kind: "영수증 OCR", p50: "3.4초", p95: "7.9초" },
        ],
        note: "식단표 목표는 30초 이내, 서버 제한은 60초예요.",
      },
    ],
    daily: {
      title: "AI·OCR 호출 수와 한도 소진",
      meta: "M16",
      items: daily([183, 205, 230, 197, 244, 276, 301]),
      summary: [
        { label: "오늘 호출 수(성공 + 실패)", value: "301회" },
        { label: "한도를 다 쓴 사용자 비율", value: "12%" },
        { label: "보상으로 준 생성 횟수(이번 주)", value: "19회" },
      ],
      "aria-label": "일별 AI·OCR 호출 수",
    },
  },
  "meal-plan-cart": {
    cards: [
      {
        code: "M11",
        name: "식단표 생성 → 칸 열람률",
        value: "63%",
        caption: "목표 60% 이상",
        status: "achieved",
      },
      {
        code: "M12",
        name: "장바구니 → 냉장고 전환율",
        value: "38%",
        caption: "추세 확인",
        status: "trend",
      },
      {
        code: "M09",
        name: "식단표 생성 성공률",
        value: "95.7%",
        caption: "목표 95% 이상",
        status: "achieved",
      },
    ],
    tables: [
      {
        key: "meal-plan-purpose",
        title: "식단 목적별 생성",
        meta: "meal_plan_generated",
        columns: [
          { key: "purpose", label: "식단 목적" },
          { key: "generated", label: "생성", align: "right", width: "3.75rem" },
          { key: "opened", label: "칸 열람", align: "right", width: "4.375rem" },
          { key: "deleted", label: "삭제", align: "right", width: "3.125rem" },
          { key: "replaced", label: "교체", align: "right", width: "3.125rem" },
        ],
        rows: [
          {
            purpose: "균형 잡힌 식단",
            generated: "74",
            opened: "49",
            deleted: "6",
            replaced: "31",
          },
          { purpose: "다이어트 식단", generated: "41", opened: "27", deleted: "4", replaced: "22" },
          { purpose: "고단백 식단", generated: "28", opened: "18", deleted: "2", replaced: "15" },
          { purpose: "간편 식단", generated: "25", opened: "15", deleted: "3", replaced: "14" },
          {
            purpose: "재료 절약 식단",
            generated: "18",
            opened: "12",
            deleted: "1",
            replaced: "10",
          },
        ],
      },
    ],
    daily: {
      title: "장바구니 → 냉장고 이동",
      meta: "cart_moved_to_fridge",
      items: daily([22, 31, 27, 35, 40, 44, 39]),
      "aria-label": "일별 장바구니에서 냉장고로 이동한 수",
    },
  },
  shared: {
    cards: [
      {
        code: "M13",
        name: "공유 작성 완료율",
        value: "54%",
        caption: "목표 50% 이상",
        status: "achieved",
      },
      {
        code: "M14",
        name: "공유 레시피 저장율",
        value: "18%",
        caption: "추세 확인",
        status: "trend",
      },
      {
        code: "M14",
        name: "공유 레시피 신고율",
        value: "0.8%",
        caption: "목표 1% 이하",
        status: "achieved",
      },
    ],
    tables: [
      {
        key: "shared-top",
        title: "이번 주 저장 수 상위",
        meta: "금주의 레시피 후보",
        columns: [
          { key: "rank", label: "순위", width: "2.5rem" },
          { key: "title", label: "공유 레시피" },
          { key: "views", label: "조회", align: "right", width: "3.75rem" },
          { key: "saves", label: "저장", align: "right", width: "3.75rem" },
          { key: "reports", label: "신고", align: "right", width: "3.125rem" },
        ],
        rows: [
          { rank: "1", title: "초간단 계란 볶음밥", views: "640", saves: "128", reports: "0" },
          { rank: "2", title: "남은 채소 카레", views: "512", saves: "97", reports: "0" },
          { rank: "3", title: "두부 강정", views: "470", saves: "85", reports: "1" },
          { rank: "4", title: "다이어트 닭가슴살 샐러드", views: "420", saves: "61", reports: "2" },
          { rank: "5", title: "연어 포케 볼", views: "300", saves: "44", reports: "1" },
        ],
      },
    ],
    daily: {
      title: "일별 공유 글 수",
      meta: "shared_recipe_published",
      items: daily([6, 9, 7, 11, 14, 12, 10]),
      "aria-label": "일별 공유 글 수",
    },
  },
  retention: {
    cards: [
      { code: "M15", name: "D1 리텐션", value: "41%", caption: "추세 확인", status: "trend" },
      { code: "M15", name: "D7 리텐션", value: "22%", caption: "목표 25% 이상", status: "missed" },
      { code: "M15", name: "WAU", value: "1,284명", caption: "주간 활성 사용자", status: "trend" },
    ],
    tables: [
      {
        key: "cohort",
        title: "가입 주별 코호트",
        meta: "app_opened 기준",
        columns: [
          { key: "week", label: "가입 주" },
          { key: "joined", label: "가입", align: "right", width: "3.75rem" },
          { key: "d1", label: "D1", align: "right", width: "3.75rem" },
          { key: "d7", label: "D7", align: "right", width: "3.75rem" },
          { key: "d14", label: "D14", align: "right", width: "3.75rem" },
        ],
        rows: [
          { week: "11월 3일 주", joined: "214", d1: "43%", d7: "24%", d14: "18%" },
          { week: "11월 10일 주", joined: "268", d1: "40%", d7: "21%", d14: "16%" },
          { week: "11월 17일 주", joined: "301", d1: "41%", d7: "22%", d14: "-" },
          { week: "11월 24일 주", joined: "96", d1: "39%", d7: "-", d14: "-" },
        ],
      },
    ],
    daily: {
      title: "주별 WAU",
      meta: "명",
      items: [
        { label: "10/27", value: 407 },
        { label: "11/3", value: 621 },
        { label: "11/10", value: 877 },
        { label: "11/17", value: 1049 },
        { label: "11/24", value: 1284 },
      ],
      "aria-label": "주별 주간 활성 사용자",
    },
  },
  cost: {
    cards: [
      {
        code: "M16",
        name: "오늘 AI·OCR 호출 수",
        value: "301회",
        caption: "성공 + 실패",
        status: "trend",
      },
      {
        code: "M16",
        name: "한도를 다 쓴 사용자 비율",
        value: "12%",
        caption: "quota_exceeded ÷ 호출 사용자",
        status: "trend",
      },
      {
        code: "M16",
        name: "보상으로 준 생성 횟수",
        value: "19회",
        caption: "이번 주 weekly_reward_granted",
        status: "trend",
      },
    ],
    tables: [
      {
        key: "calls-by-kind",
        title: "종류별 호출 수(이번 주)",
        meta: "성공 · 실패",
        columns: [
          { key: "kind", label: "종류" },
          { key: "success", label: "성공", align: "right", width: "4.375rem" },
          { key: "failure", label: "실패", align: "right", width: "3.75rem" },
          { key: "users", label: "호출 사용자", align: "right", width: "5.625rem" },
        ],
        rows: [
          { kind: "레시피 생성", success: "1,201", failure: "39", users: "412" },
          { kind: "레시피 다시 받기", success: "204", failure: "6", users: "131" },
          { kind: "식단표 생성", success: "178", failure: "8", users: "166" },
          { kind: "식단 교체", success: "90", failure: "2", users: "58" },
          { kind: "영수증 OCR", success: "388", failure: "52", users: "203" },
        ],
      },
    ],
    daily: {
      title: "일별 호출 수",
      meta: "AI + OCR",
      items: daily([183, 206, 231, 198, 243, 276, 301]),
      "aria-label": "일별 AI·OCR 호출 수",
    },
  },
  error: {
    cards: [
      {
        code: "M20",
        name: "오류 노출률",
        value: "1.9%",
        caption: "error_shown ÷ screen_viewed · 낮을수록 좋음",
        status: "trend",
      },
      {
        code: "M20",
        name: "가장 많은 오류",
        value: "OCR_UPSTREAM_ERROR",
        caption: "이번 주 52건",
        status: "trend",
      },
      {
        code: "M20",
        name: "오류가 가장 많은 화면",
        value: "receipt_loading",
        caption: "이번 주 81건",
        status: "trend",
      },
    ],
    tables: [
      {
        key: "error-code",
        title: "error_code별 노출",
        meta: "error_shown",
        columns: [
          { key: "errorCode", label: "error_code" },
          { key: "screen", label: "화면", width: "8.75rem" },
          { key: "count", label: "건수", align: "right", width: "3.75rem" },
        ],
        rows: [
          { errorCode: "OCR_UPSTREAM_ERROR", screen: "receipt_loading", count: "52" },
          { errorCode: "RECEIPT_NOT_RECOGNIZED", screen: "receipt_loading", count: "29" },
          { errorCode: "NO_INGREDIENTS", screen: "recipe_loading", count: "24" },
          { errorCode: "REPORT_ALREADY_EXISTS", screen: "shared_detail", count: "11" },
          { errorCode: "INSUFFICIENT_QUANTITY", screen: "fridge", count: "9" },
          { errorCode: "RATE_LIMITED", screen: "recipe_loading", count: "6" },
        ],
      },
    ],
    daily: {
      title: "일별 오류 노출",
      meta: "건",
      items: daily([31, 28, 36, 25, 40, 33, 29]),
      "aria-label": "일별 오류 노출 수",
    },
  },
};
