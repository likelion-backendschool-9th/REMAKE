// AI 레시피 생성·다시 받기·상세(API 28 POST /user/recipes/generate, 29 POST /user/recipes/{id}/regenerate,
// GET /user/recipes/{id}). 생성은 동기 응답(서버 최대 60초)이라 TIMEOUT_MS.ai(120초) fetch를 쓴다.
// 하루 횟수는 성공할 때만 차감되므로, 남은 횟수 캐시는 onSuccess에서만 무효화한다(실패 시 건드리지 않음).

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, createApiFetch, TIMEOUT_MS, unwrap } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";
import { type ScreenCode, SCREENS } from "@/lib/events/screens";
import type { RecipeModeProperty } from "@/lib/events/tracker";

import { usageQuotaKeys } from "./usage-quotas";

export type RecipeDetailData = components["schemas"]["RecipeDetailResponse"];
export type GenerateRecipeRequestBody = components["schemas"]["GenerateRecipeRequest"];
/** E39·E42 `recipe_mode` 계약(소문자) */
export type RecipeGenerationMode = RecipeModeProperty;

export type GenerateRecipeVariables = {
  /** 레시피 화면에서는 QUICK·CUSTOM만 쓴다(MEAL_PLAN은 식단표 전용) */
  body: Omit<GenerateRecipeRequestBody, "mode"> & { mode: "QUICK" | "CUSTOM" };
  /** SERVER 이벤트(E40·E41·E07)의 화면 코드로 X-Screen 헤더에 실린다 */
  screen?: ScreenCode;
};

export type RegenerateRecipeVariables = {
  recipeId: number;
  /** 기본값 recipe_result */
  screen?: ScreenCode;
};

export const recipeKeys = {
  all: ["recipes"] as const,
  detail: (id: number) => ["recipes", "detail", id] as const,
};

const aiFetch = createApiFetch(TIMEOUT_MS.ai);

// ===== 최근 생성 레시피의 모드 기억 =====
// 상세 응답에는 mode가 없어서, 결과 화면(/user/recipe/results/[id])에서 다시 받기를 누를 때
// E42 recipe_mode를 정확히 보내려고 생성 시점의 모드를 보관한다. 새로고침에도 남도록 sessionStorage에도 둔다.

const RECIPE_MODE_STORAGE_KEY = "remake:recipe-modes";
/** 보관 개수 상한(오래된 것부터 버림) */
const MAX_STORED_MODES = 30;

const recipeModes = new Map<number, RecipeGenerationMode>();
let restoredFromStorage = false;

function isRecipeMode(value: unknown): value is RecipeGenerationMode {
  return value === "quick" || value === "custom";
}

function restoreRecipeModes(): void {
  if (restoredFromStorage || typeof window === "undefined") return;
  restoredFromStorage = true;
  try {
    const raw = window.sessionStorage.getItem(RECIPE_MODE_STORAGE_KEY);
    if (!raw) return;
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return;
    for (const entry of parsed) {
      if (!Array.isArray(entry) || entry.length !== 2) continue;
      const [id, mode]: unknown[] = entry;
      // 메모리에 이미 있는 값(이번 탭에서 방금 만든 것)이 우선
      if (typeof id === "number" && isRecipeMode(mode) && !recipeModes.has(id)) {
        recipeModes.set(id, mode);
      }
    }
  } catch {
    // 저장소 접근 불가·깨진 값은 무시한다
  }
}

function persistRecipeModes(): void {
  if (typeof window === "undefined") return;
  try {
    window.sessionStorage.setItem(
      RECIPE_MODE_STORAGE_KEY,
      JSON.stringify(Array.from(recipeModes.entries())),
    );
  } catch {
    // 저장소를 못 쓰면 메모리 값만 쓴다
  }
}

export function setRecipeMode(recipeId: number, mode: RecipeGenerationMode): void {
  restoreRecipeModes();
  // 다시 넣어 가장 최근 항목으로 만든다(Map은 넣은 순서를 지킨다)
  recipeModes.delete(recipeId);
  recipeModes.set(recipeId, mode);
  while (recipeModes.size > MAX_STORED_MODES) {
    const oldest = recipeModes.keys().next();
    if (oldest.done) break;
    recipeModes.delete(oldest.value);
  }
  persistRecipeModes();
}

/** 이 탭에서 만든 레시피면 그때 모드, 아니면(저장 레시피 등) null */
export function getRecipeMode(recipeId: number): RecipeGenerationMode | null {
  restoreRecipeModes();
  return recipeModes.get(recipeId) ?? null;
}

// ===== 쿼리·뮤테이션 =====

export function useRecipeDetailQuery(recipeId: number | null, options?: { enabled?: boolean }) {
  const validId = recipeId !== null && Number.isInteger(recipeId) && recipeId > 0;
  return useQuery({
    queryKey: recipeKeys.detail(recipeId ?? 0),
    queryFn: async (): Promise<RecipeDetailData> =>
      unwrap(
        await api.GET("/api/v1/user/recipes/{id}", {
          params: { path: { id: recipeId ?? 0 } },
        }),
      ),
    enabled: validId && (options?.enabled ?? true),
  });
}

export function useGenerateRecipeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (variables: GenerateRecipeVariables): Promise<RecipeDetailData> =>
      unwrap(
        await api.POST("/api/v1/user/recipes/generate", {
          body: variables.body,
          params: { header: variables.screen ? { "X-Screen": variables.screen } : undefined },
          fetch: aiFetch,
        }),
      ),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(recipeKeys.detail(data.id), data);
      setRecipeMode(data.id, variables.body.mode === "CUSTOM" ? "custom" : "quick");
      void queryClient.invalidateQueries({ queryKey: usageQuotaKeys.all });
    },
  });
}

export function useRegenerateRecipeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (variables: RegenerateRecipeVariables): Promise<RecipeDetailData> =>
      unwrap(
        await api.POST("/api/v1/user/recipes/{id}/regenerate", {
          params: {
            path: { id: variables.recipeId },
            header: { "X-Screen": variables.screen ?? SCREENS.recipeResult },
          },
          fetch: aiFetch,
        }),
      ),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(recipeKeys.detail(data.id), data);
      const prevMode = getRecipeMode(variables.recipeId);
      if (prevMode) setRecipeMode(data.id, prevMode);
      void queryClient.invalidateQueries({ queryKey: usageQuotaKeys.all });
    },
  });
}
