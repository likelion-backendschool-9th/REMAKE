// p4(레시피) 화면 상태 목록. /dev/p4가 이 순서로 목록을 보이고, /dev/p4/[state]가 하나를 그린다.
// 상태를 더하면 여기에 한 줄 추가한다(blueprint §3.1 7번). 실제 라우트는 컨테이너 기본값(불러오는 중)을 쓰고, 마크업 확인용 값은 여기서만 넘긴다.
// 피그마 "디자인" 페이지(디자인 v2)의 p4 프레임만 둔다. "추가 화면" 페이지 프레임(생성 중·실패·횟수 소진·정렬 선택 등)은 아직 없다.

import { CustomRecipeContainer } from "../../../user/recipe/custom/_components/CustomRecipeContainer/CustomRecipeContainer";
import { SAMPLE_CUSTOM_DEFAULTS } from "../../../user/recipe/custom/_lib/custom-recipe";
import { RecipeDetailContainer } from "../../../user/recipe/recipes/[id]/_components/RecipeDetailContainer/RecipeDetailContainer";
import { SAMPLE_RECIPE_DETAIL } from "../../../user/recipe/recipes/[id]/_lib/recipe-detail";
import { CookContainer } from "../../../user/recipe/recipes/[id]/cook/_components/CookContainer/CookContainer";
import { SAMPLE_COOK_PREVIEW } from "../../../user/recipe/recipes/[id]/cook/_lib/cook";
import { RecipeResultContainer } from "../../../user/recipe/results/[id]/_components/RecipeResultContainer/RecipeResultContainer";
import { SAMPLE_RECIPE_RESULT } from "../../../user/recipe/results/[id]/_lib/recipe-result";
import { RecipesContainer } from "../../../user/recipe/saved-recipes/_components/RecipesContainer/RecipesContainer";
import {
  SAMPLE_SAVED_RECIPES,
  SAMPLE_WEEKLY_TOP,
} from "../../../user/recipe/saved-recipes/_lib/saved-recipes";
import { RankingContainer } from "../../../user/recipe/shared-recipes/_components/RankingContainer/RankingContainer";
import {
  SAMPLE_LATEST,
  SAMPLE_NOW,
  SAMPLE_RANKING,
} from "../../../user/recipe/shared-recipes/_lib/shared-recipes";
import { SharedRecipeDetailContainer } from "../../../user/recipe/shared-recipes/[id]/_components/SharedRecipeDetailContainer/SharedRecipeDetailContainer";
import { SAMPLE_SHARED_RECIPE } from "../../../user/recipe/shared-recipes/[id]/_lib/shared-recipe-detail";
import { ShareRecipeContainer } from "../../../user/recipe/shared-recipes/new/_components/ShareRecipeContainer/ShareRecipeContainer";
import type { PreviewState } from "../../_lib/preview";

const HOME_ROUTE = "/user/recipe/saved-recipes";
const DETAIL_ROUTE = "/user/recipe/recipes/[id]";
const CUSTOM_ROUTE = "/user/recipe/custom";
const RESULT_ROUTE = "/user/recipe/results/[id]";
const RANKING_ROUTE = "/user/recipe/shared-recipes";
const SHARED_DETAIL_ROUTE = "/user/recipe/shared-recipes/[id]";
const SHARE_ROUTE = "/user/recipe/shared-recipes/new";
const SHARE_PHOTO = "/images/recipe/sample-share-photo.webp";
const COOK_ROUTE = "/user/recipe/recipes/[id]/cook";
/** 커스텀 레시피 옵션 시트 5개(피그마 프레임 이름 순) */
const CUSTOM_SHEETS = [
  ["taste", "원하는 맛"],
  ["cookTime", "조리 시간"],
  ["tool", "조리 도구"],
  ["method", "조리 방식"],
  ["cuisine", "요리 유형"],
] as const;
const NOTICE = { message: "당근의 소비기한이 임박했어요!", unreadCount: 2 };

export const P4_STATES: PreviewState[] = [
  {
    slug: "recipes",
    label: "p4.1-레시피",
    route: HOME_ROUTE,
    render: () => (
      <RecipesContainer
        weeklyTop={SAMPLE_WEEKLY_TOP}
        savedRecipes={SAMPLE_SAVED_RECIPES}
        notice={NOTICE}
      />
    ),
  },
  {
    slug: "recipes-quick",
    label: "p4.1-레시피-빠른 레시피 생성",
    route: HOME_ROUTE,
    render: () => (
      <RecipesContainer
        weeklyTop={SAMPLE_WEEKLY_TOP}
        savedRecipes={SAMPLE_SAVED_RECIPES}
        notice={NOTICE}
        initialQuickModalOpen
      />
    ),
  },
  {
    slug: "recipes-skeleton",
    label: "p4.1-레시피-스켈레톤 (실제 라우트 처음 상태)",
    route: HOME_ROUTE,
    render: () => <RecipesContainer notice={NOTICE} />,
  },
  {
    slug: "recipes-empty",
    label: "p4.1-레시피-빈 리스트",
    route: HOME_ROUTE,
    render: () => (
      <RecipesContainer
        weeklyTop={[]}
        savedRecipes={{ totalCount: 0, content: [] }}
        notice={NOTICE}
      />
    ),
  },
  {
    slug: "detail",
    label: "p4.1.1-레시피 상세",
    route: DETAIL_ROUTE,
    render: () => <RecipeDetailContainer recipe={SAMPLE_RECIPE_DETAIL} />,
  },
  {
    slug: "detail-menu",
    label: "p4.1.1-레시피 상세-케밥 클릭-디세이블",
    route: DETAIL_ROUTE,
    render: () => <RecipeDetailContainer recipe={SAMPLE_RECIPE_DETAIL} initialMenuOpen />,
  },
  {
    slug: "detail-menu-active",
    label: "p4.1.1-레시피 상세-케밥 클릭-액티브",
    route: DETAIL_ROUTE,
    render: () => (
      <RecipeDetailContainer
        recipe={SAMPLE_RECIPE_DETAIL}
        initialMenuOpen
        initialMenuAction="DELETE"
      />
    ),
  },
  {
    slug: "detail-menu-shared",
    label: "p4.1.1-레시피 상세-케밥 클릭 (이번 주 공유함, 공유하기 막힘)",
    route: DETAIL_ROUTE,
    render: () => (
      <RecipeDetailContainer
        recipe={{ ...SAMPLE_RECIPE_DETAIL, shareable: false }}
        initialMenuOpen
      />
    ),
  },
  {
    slug: "detail-reading",
    label: "p4.1.1-레시피 상세-레시피 재생",
    route: DETAIL_ROUTE,
    render: () => <RecipeDetailContainer recipe={SAMPLE_RECIPE_DETAIL} initialPlayingStepNo={1} />,
  },
  {
    slug: "detail-skeleton",
    label: "p4.1.1-레시피 상세-스켈레톤 (실제 라우트 처음 상태)",
    route: DETAIL_ROUTE,
    render: () => <RecipeDetailContainer />,
  },
  {
    slug: "custom",
    label: "p4.1.2-커스텀 레시피 생성",
    route: CUSTOM_ROUTE,
    render: () => <CustomRecipeContainer defaults={SAMPLE_CUSTOM_DEFAULTS} />,
  },
  {
    slug: "custom-ingredients",
    label: "p4.1.2-커스텀 레시피 생성-드로워 디세이블",
    route: CUSTOM_ROUTE,
    render: () => (
      <CustomRecipeContainer defaults={SAMPLE_CUSTOM_DEFAULTS} initialPanel="ingredients" />
    ),
  },
  {
    slug: "custom-ingredients-active",
    label: "p4.1.2-커스텀 레시피 생성-드로워 액티브",
    route: CUSTOM_ROUTE,
    render: () => (
      <CustomRecipeContainer
        defaults={SAMPLE_CUSTOM_DEFAULTS}
        initialPanel="ingredients"
        initialIngredientIds={[2, 4]}
      />
    ),
  },
  ...CUSTOM_SHEETS.map(([panel, title]): PreviewState => ({
    slug: `custom-${panel}`,
    label: `p4.1.2-커스텀 레시피 생성-${title}`,
    route: CUSTOM_ROUTE,
    render: () => <CustomRecipeContainer defaults={SAMPLE_CUSTOM_DEFAULTS} initialPanel={panel} />,
  })),
  {
    slug: "custom-allergens",
    label: "p4.1.2-커스텀 레시피 생성-알레르기 재료",
    route: CUSTOM_ROUTE,
    render: () => (
      <CustomRecipeContainer
        defaults={SAMPLE_CUSTOM_DEFAULTS}
        initialPanel="allergens"
        initialAllergens={["BEEF"]}
      />
    ),
  },
  {
    slug: "custom-skeleton",
    label: "p4.1.2-커스텀 레시피 생성 (불러오는 중, 실제 라우트 처음 상태)",
    route: CUSTOM_ROUTE,
    render: () => <CustomRecipeContainer />,
  },
  {
    slug: "result",
    label: "p4.1.3-레시피 생성 (저장 전)",
    route: RESULT_ROUTE,
    render: () => <RecipeResultContainer recipe={SAMPLE_RECIPE_RESULT} remainingCount={1} />,
  },
  {
    slug: "result-saved",
    label: "p4.1.3-레시피 생성-레시피 저장 (저장 후)",
    route: RESULT_ROUTE,
    render: () => (
      <RecipeResultContainer recipe={{ ...SAMPLE_RECIPE_RESULT, saved: true }} remainingCount={1} />
    ),
  },
  {
    slug: "ranking",
    label: "p4.1.4-이번 주 레시피 랭킹",
    route: RANKING_ROUTE,
    render: () => <RankingContainer ranking={SAMPLE_RANKING} now={SAMPLE_NOW} />,
  },
  {
    slug: "ranking-search",
    label: "p4.1.4-이번 주 레시피 랭킹-인풋 포커스",
    route: RANKING_ROUTE,
    render: () => (
      <RankingContainer ranking={SAMPLE_RANKING} now={SAMPLE_NOW} initialQuery="마라" />
    ),
  },
  {
    slug: "ranking-latest",
    label: "p4.1.4-이번 주 레시피 랭킹-최신순",
    route: RANKING_ROUTE,
    render: () => (
      <RankingContainer ranking={SAMPLE_LATEST} now={SAMPLE_NOW} initialSort="latest" />
    ),
  },
  {
    slug: "ranking-skeleton",
    label: "p4.1.4-이번 주 레시피 랭킹-스켈레톤 (실제 라우트 처음 상태)",
    route: RANKING_ROUTE,
    render: () => <RankingContainer />,
  },
  {
    slug: "ranking-empty",
    label: "p4.1.4-이번 주 레시피 랭킹-검색 결과 없음",
    route: RANKING_ROUTE,
    render: () => (
      <RankingContainer ranking={SAMPLE_RANKING} now={SAMPLE_NOW} initialQuery="떡볶이" />
    ),
  },
  {
    slug: "shared",
    label: "p4.1.4.1-공유 레시피 상세",
    route: SHARED_DETAIL_ROUTE,
    render: () => <SharedRecipeDetailContainer recipe={SAMPLE_SHARED_RECIPE} />,
  },
  {
    slug: "shared-report",
    label: "p4.1.4.1-공유 레시피 상세-신고하기",
    route: SHARED_DETAIL_ROUTE,
    render: () => <SharedRecipeDetailContainer recipe={SAMPLE_SHARED_RECIPE} initialReportOpen />,
  },
  {
    slug: "shared-saved",
    label: "p4.1.4.1-공유 레시피 상세-저장",
    route: SHARED_DETAIL_ROUTE,
    render: () => (
      <SharedRecipeDetailContainer
        recipe={{ ...SAMPLE_SHARED_RECIPE, saved: true, saveCount: 31 }}
      />
    ),
  },
  {
    slug: "shared-skeleton",
    label: "p4.1.4.1-공유 레시피 상세-스켈레톤 (실제 라우트 처음 상태)",
    route: SHARED_DETAIL_ROUTE,
    render: () => <SharedRecipeDetailContainer />,
  },
  {
    slug: "share",
    label: "p4.1.1.1-레시피 공유 작성",
    route: SHARE_ROUTE,
    render: () => <ShareRecipeContainer recipe={SAMPLE_RECIPE_DETAIL} />,
  },
  {
    slug: "share-focus",
    label: "p4.1.1.1-레시피 공유 작성-인풋 포커스",
    route: SHARE_ROUTE,
    render: () => <ShareRecipeContainer recipe={SAMPLE_RECIPE_DETAIL} initialTitleFocused />,
  },
  {
    slug: "share-photo",
    label: "p4.1.1.1-레시피 공유 작성-사진 수정",
    route: SHARE_ROUTE,
    render: () => (
      <ShareRecipeContainer recipe={SAMPLE_RECIPE_DETAIL} initialPhotoUrl={SHARE_PHOTO} />
    ),
  },
  {
    slug: "share-done",
    label: "p4.1.1.1-레시피 공유 작성-완료",
    route: SHARE_ROUTE,
    render: () => (
      <ShareRecipeContainer
        recipe={SAMPLE_RECIPE_DETAIL}
        initialPhotoUrl={SHARE_PHOTO}
        initialTitle="참치 칠리 치즈 포케"
      />
    ),
  },
  {
    slug: "cook",
    label: "p4.1.1.2-레시피 사용 후 재료 차감 (처음 모두 체크)",
    route: COOK_ROUTE,
    render: () => <CookContainer preview={SAMPLE_COOK_PREVIEW} />,
  },
  {
    slug: "cook-checked",
    label: "p4.1.1.2-레시피 사용 후 재료 차감-일부 해제 (쪽파를 쓰지 않음)",
    route: COOK_ROUTE,
    render: () => <CookContainer preview={SAMPLE_COOK_PREVIEW} initialCheckedIds={[11, 13]} />,
  },
  {
    slug: "cook-amount",
    label: "p4.1.1.2-레시피 사용 후 재료 차감-사용 재료 수량 수정 (쪽파 뺄 양 시트)",
    route: COOK_ROUTE,
    render: () => <CookContainer preview={SAMPLE_COOK_PREVIEW} initialEditingId={12} />,
  },
  {
    slug: "cook-skeleton",
    label: "p4.1.1.2-레시피 사용 후 재료 차감-스켈레톤 (실제 라우트 처음 상태)",
    route: COOK_ROUTE,
    render: () => <CookContainer />,
  },
];
