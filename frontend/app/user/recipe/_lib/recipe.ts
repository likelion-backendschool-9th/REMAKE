// 레시피 영역(p4) 여러 화면이 같이 쓰는 값과 함수. 저장 목록(p4.1)·금주의 레시피(p4.1)·랭킹(p4.1.4)·
// 레시피 본문(상세 p4.1.1, 생성 결과 p4.1.3, 공유 상세 p4.1.4.1, 공유 작성 p4.1.1.1) 등.

import { formatCookingTime, formatServings } from "@/lib/format";
import { formatUnitAmount, type RecipeUnit, UNIT_LABELS } from "@/lib/ingredients";

/** 레시피 재료 한 줄(API 30·52 ingredients[]). have는 공유 상세에서만 온다(없으면 빨간 테두리) */
export type RecipeIngredient = {
  name: string;
  /** 양. 약간(TO_TASTE)처럼 양이 없으면 null */
  amount: number | null;
  unit: RecipeUnit;
  have?: boolean;
};

export type RecipeStep = { stepNo: number; content: string };

/** 레시피 본문: 재료 준비·만드는 방법·Tips(API 28·29·30·52 공통) */
export type RecipeContent = {
  title: string;
  servings: number;
  cookMinutes: number;
  /** Tips! 문구. 없으면 Tips 상자를 그리지 않는다 */
  tips: string | null;
  ingredients: RecipeIngredient[];
  steps: RecipeStep[];
};

/** 레시피 양에 쓰는 분수(blueprint §10: 1/2·1/3·1/4로 보일 수 있음) */
const FRACTIONS: readonly [number, string][] = [
  [1 / 4, "1/4"],
  [1 / 3, "1/3"],
  [1 / 2, "1/2"],
];

/** 레시피 재료 양: 0.5, "TBSP" → "1/2큰술" / 2, "EA" → "2개" / null 또는 TO_TASTE → "약간" */
export function formatRecipeAmount(amount: number | null, unit: RecipeUnit): string {
  if (amount === null) return formatUnitAmount(amount, unit);
  const fraction = FRACTIONS.find(([value]) => Math.abs(amount - value) < 0.01)?.[1];
  return fraction && unit !== "TO_TASTE"
    ? `${fraction}${UNIT_LABELS[unit]}`
    : formatUnitAmount(amount, unit);
}

/** "재료 준비" 옆 안내: 2, 30 → ["2인분", "30분 이내"] */
export function recipeMeta(servings: number, cookMinutes: number): [string, string] {
  return [formatServings(servings), `${formatCookingTime(cookMinutes)} 이내`];
}

/** 미리보기용 레시피 본문(피그마 p4.1.1 문구). 재료 양은 서버 단위 14개 안에서 고쳤다(피그마의 "1줌·1공기·2줄"은 없는 단위) */
export const SAMPLE_RECIPE_CONTENT: RecipeContent = {
  title: "베이컨 계란 볶음밥",
  servings: 2,
  cookMinutes: 30,
  tips: "잔열을 이용해 가볍게 두세 번만 뒤적여 양상추의 숨이 너무 죽지 않고 아삭함을 유지하게 합니다.",
  ingredients: [
    { name: "양상추", amount: 3, unit: "SHEET" },
    { name: "찬밥", amount: 300, unit: "G" },
    { name: "계란", amount: 1, unit: "EA" },
    { name: "베이컨", amount: 2, unit: "SHEET" },
  ],
  steps: [
    {
      stepNo: 1,
      content:
        "재료 썰기: 베이컨은 1cm 두께로 썰고, 양상추는 볶음밥에 잘 섞이도록 듬성듬성 한 입 크기로 썰어줍니다. 달걀은 그릇에 미리 풀어둡니다.",
    },
    {
      stepNo: 2,
      content:
        "기름 내기: 중불로 달군 팬에 베이컨을 올려 굽습니다. 베이컨에서 기름이 충분히 나오면 한쪽으로 몰아둡니다. (대파가 있다면 이때 같이 볶아 파기름을 내주세요.)",
    },
    {
      stepNo: 3,
      content:
        "스크램블 에그: 팬의 빈 공간에 풀어둔 달걀을 붓고 젓가락으로 빠르게 저어 부드러운 스크램블을 만듭니다.",
    },
    {
      stepNo: 4,
      content:
        "밥 볶기: 달걀이 반쯤 익으면 찬밥을 넣고 주걱을 세워 밥알을 가르듯이 볶아줍니다. 밥알이 코팅되면 간장과 굴소스를 팬 가장자리에 끓이듯 눌러 풍미를 더한 뒤 전체적으로 섞어줍니다.",
    },
    {
      stepNo: 5,
      content:
        "양상추 섞기 (핵심): 가스레인지 불을 끄고, 썰어둔 양상추를 넣습니다. 잔열을 이용해 가볍게 두세 번만 뒤적여 양상추의 숨이 너무 죽지 않고 아삭함을 유지하게 합니다.",
    },
  ],
};

export const RECIPE_HOME_PATH = "/user/recipe/saved-recipes";
export const CUSTOM_RECIPE_PATH = "/user/recipe/custom";
export const SHARED_RECIPES_PATH = "/user/recipe/shared-recipes";

/** 내 레시피 상세 주소(p4.1.1) */
export function recipePath(recipeId: number): string {
  return `/user/recipe/recipes/${recipeId}`;
}

/** 공유 레시피 상세 주소(p4.1.4.1) */
export function sharedRecipePath(sharedRecipeId: number): string {
  return `${SHARED_RECIPES_PATH}/${sharedRecipeId}`;
}

/**
 * 목록 한 줄의 재료 요약. 서버가 앞 3개 이름을 " · "로 이어 주고(ingredientSummary) 나머지 개수를 따로 준다
 * (API 33·49·50, 백엔드 IngredientSummary). 나머지가 0이면 "+N"을 붙이지 않는다.
 * 예: ("미역 · 냉동 만두", 3) → "미역 · 냉동 만두 +3"
 */
export function formatIngredientSummary(summary: string, extraCount: number): string {
  return extraCount > 0 ? `${summary} +${extraCount}` : summary;
}
