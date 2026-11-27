import type { BadgeTone } from "@/components/Badge/Badge";

// a4 신고 관리 · a4.1 신고 상세에서 함께 쓰는 값. 처리 규칙은 docs/agent/spec/domain-rules.md와 erd.md를 따른다.
// API 65(목록)·66(상세)·67(처리)가 붙으면 응답 모양에 맞춘다. SAMPLE_*는 마크업 확인용이라 미리보기(app/dev)에서만 넘긴다.

export const REPORT_TAB_VALUES = ["pending", "done"] as const;
export type ReportTab = (typeof REPORT_TAB_VALUES)[number];

/** 운영자가 고르는 처리 결과. API 67의 result와 같다 */
export type ReportAction = "VIOLATION" | "NO_ISSUE" | "DELETED";

/** 처리 결과가 끝난 글에 붙는 값. 목록·상세 모두 이 값으로 배지를 고른다 */
export type ReportResult = ReportAction;

/** 제재 단계 1~4. 1·2차는 레시피 공유 정지, 3·4차는 계정 정지(4차는 영구) */
export type SanctionLevel = 1 | 2 | 3 | 4;

export type ReportReasonCount = { label: string; count: number };

export type PendingReportItem = {
  sharedRecipeId: number;
  title: string;
  reportCount: number;
  reasons: ReportReasonCount[];
  latestReportedAt: string;
  /** 작성자가 확정된 위반 횟수 */
  authorViolationCount: number;
};

export type HandledReportItem = {
  sharedRecipeId: number;
  title: string;
  reportCount: number;
  result: ReportResult;
  /** 적용한 제재 요약. 없으면 null(문제 없음, 4차 뒤) */
  appliedSanction: string | null;
  handledAt: string;
  operator: string;
};

export type ReportEntry = {
  reportedAt: string;
  reason: string;
  reporterId: number;
};

export type ReportIngredient = {
  name: string;
  amount: string;
  /** 준비되지 않은 재료. 피그마 a4.1에서 빨간 테두리로 보인다 */
  missing: boolean;
};

/** 사용자에게 보이는 레시피 내용(피그마 a4.1 상세 게시글) */
export type ReportRecipe = {
  /** 글 사진. 없으면 이미지 아이콘이 있는 회색 자리를 보인다 */
  imageUrl: string | null;
  servings: string;
  cookingTime: string;
  ingredients: ReportIngredient[];
  steps: string[];
  tips: string[];
};

export type HandledInfo = {
  result: ReportResult;
  handledAt: string;
  operator: string;
  /** 이번 처리로 건 제재 단계. 문제 없음이거나 4차 뒤면 null */
  appliedLevel: SanctionLevel | null;
};

export type ReportDetail = {
  sharedRecipeId: number;
  title: string;
  sharedAt: string;
  saveCount: number;
  recipe: ReportRecipe;
  reports: ReportEntry[];
  author: {
    userId: number;
    violationCount: number;
    /** 현재 제재 한 줄 요약. 예: "없음 (1차 7일 공유 정지 끝남)" */
    currentSanction: string;
  };
  /** null이면 아직 대기 중 */
  handled: HandledInfo | null;
};

/** 신고 사유 배지 색: 위험한 조리법=빨강, 기타=회색, 그 밖=주황(피그마 a4) */
export function getReasonTone(label: string): BadgeTone {
  if (label === "위험한 조리법") return "danger";
  if (label === "기타") return "neutral";
  return "warning";
}

export const REPORT_RESULT_LABEL: Record<ReportResult, string> = {
  VIOLATION: "위반 · 숨김",
  NO_ISSUE: "문제 없음",
  DELETED: "글 삭제",
};

export const REPORT_RESULT_TONE: Record<ReportResult, BadgeTone> = {
  VIOLATION: "danger",
  NO_ISSUE: "safe",
  DELETED: "neutral",
};

const SANCTION_NAME: Record<SanctionLevel, string> = {
  1: "7일 레시피 공유 정지",
  2: "30일 레시피 공유 정지",
  3: "30일 계정 정지",
  4: "영구 계정 정지",
};

/** "2차 제재(30일 레시피 공유 정지)" 형태. 안내 문구·확인창에 쓴다 */
export function getSanctionName(level: SanctionLevel): string {
  return `${level}차 제재(${SANCTION_NAME[level]})`;
}

/** 확정된 위반 횟수로 다음에 걸릴 제재 단계를 구한다. 4차 뒤(영구 정지 이후)면 null */
export function getNextSanctionLevel(violationCount: number): SanctionLevel | null {
  if (violationCount >= 4) return null;
  return (violationCount + 1) as SanctionLevel;
}

/** 신고 내역을 사유별로 묶어 신고 수가 많은 순으로 보인다 */
export function summarizeReasons(reports: ReportEntry[]): ReportReasonCount[] {
  const counts = new Map<string, number>();
  for (const report of reports) {
    counts.set(report.reason, (counts.get(report.reason) ?? 0) + 1);
  }
  return [...counts.entries()]
    .map(([label, count]) => ({ label, count }))
    .sort((a, b) => b.count - a.count);
}

/** 처리 영역 위 안내 문구(피그마 안내 박스). 4차 직전에는 경고 문구를 보인다 */
export function getNextSanctionNotice(violationCount: number): string {
  const level = getNextSanctionLevel(violationCount);
  if (level === null) {
    return "이미 4차 제재까지 걸렸어요. 위반으로 처리하면 글만 숨겨지고 제재는 더 걸리지 않아요.";
  }
  if (level === 4) {
    return "위반으로 처리하면 4차 제재로 계정이 영구 정지돼요. 되돌릴 수 없으니 신고 내용을 한 번 더 확인해 주세요.";
  }
  return `위반으로 처리하면 글을 숨기고 ${getSanctionName(level)}를 적용해요.`;
}

/** 확인창 문구. 처리 버튼 색은 위험한 처리(위반·삭제)면 빨강, 문제 없음이면 파랑 */
export function getActionCopy(action: ReportAction, violationCount: number) {
  const level = getNextSanctionLevel(violationCount);
  const ordinal = level === null ? null : `${level}차 제재`;

  if (action === "VIOLATION") {
    return {
      title: "위반으로 처리할까요?",
      message: ordinal
        ? `글을 숨기고 작성자에게 ${getSanctionName(level as SanctionLevel)}를 적용해요.`
        : "글만 숨겨요. 제재는 더 걸리지 않아요.",
      confirmLabel: "처리하기",
      tone: "danger" as const,
    };
  }
  if (action === "NO_ISSUE") {
    return {
      title: "문제 없음으로 처리할까요?",
      message: "글은 그대로 보이고, 작성자에게 제재를 적용하지 않아요.",
      confirmLabel: "처리하기",
      tone: "primary" as const,
    };
  }
  return {
    title: "글을 삭제할까요?",
    message: ordinal
      ? `공유 공간에서 글이 지워지고 되돌릴 수 없어요. 위반으로 세어 작성자에게 ${ordinal}를 적용해요.`
      : "공유 공간에서 글이 지워지고 되돌릴 수 없어요. 제재는 더 걸리지 않아요.",
    confirmLabel: "삭제하기",
    tone: "danger" as const,
  };
}

/** 상세 페이지 제목 아래 한 줄. 처리 상태는 처리 여부에 따라 바뀐다 */
export function getDetailMeta(report: ReportDetail): string {
  const status = report.handled ? "처리 완료" : "처리 대기";
  return `${report.sharedAt} 공유 · 저장 ${report.saveCount} · ${status}`;
}

/** 처리한 뒤 처리 결과 박스에 보이는 설명 */
export function getHandledDescription(handled: HandledInfo): string {
  const applied = handled.appliedLevel
    ? ` 작성자에게 ${getSanctionName(handled.appliedLevel)}를 적용했어요.`
    : "";

  if (handled.result === "NO_ISSUE") {
    return "글은 그대로 보이고, 작성자에게 제재를 적용하지 않았어요.";
  }
  if (handled.result === "DELETED") {
    return handled.appliedLevel
      ? `글을 삭제하고${applied}`
      : "글을 삭제했어요. 제재는 더 걸리지 않았어요.";
  }
  return handled.appliedLevel
    ? `글을 숨기고${applied}`
    : "글만 숨겼어요. 제재는 더 걸리지 않았어요.";
}

// ---- 마크업 확인용 샘플(미리보기에서만 사용) ----

/** 피그마 a4의 처리 대기 글 수 */
export const SAMPLE_PENDING_COUNT = 12;

export const SAMPLE_PENDING_REPORTS: PendingReportItem[] = [
  {
    sharedRecipeId: 101,
    title: "최저가 할인 링크 확인하세요!!",
    reportCount: 5,
    reasons: [
      { label: "광고성 제목", count: 4 },
      { label: "기타", count: 1 },
    ],
    latestReportedAt: "11월 24일 14:12",
    authorViolationCount: 1,
  },
  {
    sharedRecipeId: 102,
    title: "반숙 계란 장조림",
    reportCount: 3,
    reasons: [{ label: "위험한 조리법", count: 3 }],
    latestReportedAt: "11월 24일 11:40",
    authorViolationCount: 0,
  },
  {
    sharedRecipeId: 103,
    title: "다이어트 닭가슴살 샐러드",
    reportCount: 2,
    reasons: [{ label: "불쾌한 이미지", count: 2 }],
    latestReportedAt: "11월 23일 21:05",
    authorViolationCount: 0,
  },
  {
    sharedRecipeId: 104,
    title: "초간단 김치볶음밥",
    reportCount: 1,
    reasons: [{ label: "기타", count: 1 }],
    latestReportedAt: "11월 23일 18:30",
    authorViolationCount: 0,
  },
  {
    sharedRecipeId: 105,
    title: "연어 포케 볼",
    reportCount: 1,
    reasons: [{ label: "불쾌한 이미지", count: 1 }],
    latestReportedAt: "11월 23일 09:12",
    authorViolationCount: 2,
  },
  {
    sharedRecipeId: 106,
    title: "남은 채소 카레",
    reportCount: 1,
    reasons: [{ label: "광고성 제목", count: 1 }],
    latestReportedAt: "11월 22일 19:48",
    authorViolationCount: 0,
  },
  {
    sharedRecipeId: 107,
    title: "두부 강정",
    reportCount: 1,
    reasons: [{ label: "기타", count: 1 }],
    latestReportedAt: "11월 22일 13:02",
    authorViolationCount: 0,
  },
];

export const SAMPLE_HANDLED_REPORTS: HandledReportItem[] = [
  {
    sharedRecipeId: 201,
    title: "초특가 마트 쿠폰 레시피",
    reportCount: 3,
    result: "VIOLATION",
    appliedSanction: "1차 · 7일 공유 정지",
    handledAt: "11월 10일 10:22",
    operator: "운영자 계정",
  },
  {
    sharedRecipeId: 202,
    title: "계란말이 꿀팁",
    reportCount: 2,
    result: "NO_ISSUE",
    appliedSanction: null,
    handledAt: "11월 9일 18:40",
    operator: "운영자 계정",
  },
  {
    sharedRecipeId: 203,
    title: "무료 체험 링크 공유",
    reportCount: 6,
    result: "DELETED",
    appliedSanction: "3차 · 30일 계정 정지",
    handledAt: "11월 8일 14:05",
    operator: "운영자 계정",
  },
  {
    sharedRecipeId: 204,
    title: "매운 떡볶이",
    reportCount: 1,
    result: "NO_ISSUE",
    appliedSanction: null,
    handledAt: "11월 7일 11:31",
    operator: "운영자 계정",
  },
  {
    sharedRecipeId: 205,
    title: "양배추 스테이크",
    reportCount: 2,
    result: "VIOLATION",
    appliedSanction: "1차 · 7일 공유 정지",
    handledAt: "11월 6일 20:16",
    operator: "운영자 계정",
  },
];

/** 피그마 a4.1(처리 끝남·처리 대기 최저가 프레임)과 같은 샘플 레시피 */
const SAMPLE_RECIPE: ReportRecipe = {
  imageUrl: "/images/recipe/sample-shared-photo.webp",
  servings: "2인분",
  cookingTime: "30분 이내",
  ingredients: [
    { name: "양상추", amount: "1줌", missing: true },
    { name: "베이컨", amount: "2줄", missing: false },
    { name: "찬밥", amount: "1공기", missing: false },
    { name: "토마토", amount: "1개", missing: true },
    { name: "계란", amount: "1개", missing: false },
  ],
  steps: [
    "재료 썰기: 베이컨은 1cm 두께로 썰고, 양상추는 볶음밥에 잘 섞이도록 듬성듬성 한 입 크기로 썰어줍니다. 달걀은 그릇에 미리 풀어둡니다.",
    "기름 내기: 중불로 달군 팬에 베이컨을 올려 굽습니다. 베이컨에서 기름이 충분히 나오면 한쪽으로 몰아둡니다. (대파가 있다면 이때 같이 볶아 파기름을 내주세요.)",
    "스크램블 에그: 팬의 빈 공간에 풀어둔 달걀을 붓고 젓가락으로 빠르게 저어 부드러운 스크램블을 만듭니다.",
    "밥 볶기: 달걀이 반쯤 익으면 찬밥을 넣고 주걱을 세워 밥알을 가르듯이 볶아줍니다. 밥알이 코팅되면 간장과 굴소스를 팬 가장자리에 끓이듯 눌러 풍미를 더한 뒤 전체적으로 섞어줍니다.",
    "양상추 섞기 (핵심): 가스레인지 불을 끄고, 썰어둔 양상추를 넣습니다. 잔열을 이용해 가볍게 두세 번만 뒤적여 양상추의 숨이 너무 죽지 않고 아삭함을 유지하게 합니다.",
  ],
  tips: [
    "잔열을 이용해 가볍게 두세 번만 뒤적여 양상추의 숨이 너무 죽지 않고 아삭함을 유지하게 합니다.",
  ],
};

const SAMPLE_ENTRIES: ReportEntry[] = [
  { reportedAt: "11월 24일 14:12", reason: "광고성 제목", reporterId: 10233 },
  { reportedAt: "11월 24일 09:30", reason: "광고성 제목", reporterId: 10412 },
  { reportedAt: "11월 23일 22:18", reason: "기타", reporterId: 10098 },
  { reportedAt: "11월 23일 15:44", reason: "광고성 제목", reporterId: 10377 },
  { reportedAt: "11월 22일 20:01", reason: "광고성 제목", reporterId: 10156 },
];

/** 처리 대기 상세(기본). 확정된 위반 1회 → 다음은 2차 */
export const SAMPLE_REPORT_DETAIL: ReportDetail = {
  sharedRecipeId: 101,
  title: "최저가 할인 링크 확인하세요!!",
  sharedAt: "11월 21일",
  saveCount: 34,
  recipe: SAMPLE_RECIPE,
  reports: SAMPLE_ENTRIES,
  author: {
    userId: 10087,
    violationCount: 1,
    currentSanction: "없음 (1차 7일 공유 정지 끝남)",
  },
  handled: null,
};

/** 위반으로 처리한 뒤(처리 끝남). 확정된 위반 2회, 현재 제재 2차 */
export const SAMPLE_HANDLED_DETAIL: ReportDetail = {
  ...SAMPLE_REPORT_DETAIL,
  author: {
    userId: 10087,
    violationCount: 2,
    currentSanction: "2차 · 12월 24일까지 공유 정지",
  },
  handled: {
    result: "VIOLATION",
    handledAt: "11월 24일 15:30",
    operator: "운영자 계정",
    appliedLevel: 2,
  },
};

/** 4차 직전(확정된 위반 3회). 처리하면 영구 정지라는 경고가 보인다 */
export const SAMPLE_FOURTH_WARNING_DETAIL: ReportDetail = {
  ...SAMPLE_REPORT_DETAIL,
  author: {
    userId: 10087,
    violationCount: 3,
    currentSanction: "없음 (3차 30일 계정 정지 끝남)",
  },
};
