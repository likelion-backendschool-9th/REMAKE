// 냉장고 재료 목록(API GET /api/v1/user/ingredients, 필터 없이 전체). 소비기한이 빠른 순이며
// status·dDay는 서버 값을 그대로 쓴다(표시는 lib/format.ts formatDDay).

import { useQuery } from "@tanstack/react-query";

import { api, unwrap } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

export type FridgeIngredientItem = components["schemas"]["FridgeIngredientResponse"];
export type FridgeIngredientListData = components["schemas"]["FridgeIngredientListResponse"];

export const ingredientKeys = {
  all: ["ingredients"] as const,
  list: () => ["ingredients", "list"] as const,
};

export function useIngredientsQuery(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: ingredientKeys.list(),
    queryFn: async (): Promise<FridgeIngredientListData> =>
      unwrap(await api.GET("/api/v1/user/ingredients")),
    enabled: options?.enabled ?? true,
  });
}
