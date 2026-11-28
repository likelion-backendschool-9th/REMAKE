// 남은 생성 횟수(API GET /api/v1/user/usage-quotas). 레시피는 오늘(KST), 식단표는 이번 달 기준이며 성공한 생성만 센다.
// 레시피 생성·다시 받기가 성공하면 lib/queries/recipes.ts가 이 캐시를 무효화한다(실패 시에는 건드리지 않음).

import { useQuery } from "@tanstack/react-query";

import { api, unwrap } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

export type UsageQuotaData = components["schemas"]["UsageQuotaResponse"];

export const usageQuotaKeys = {
  all: ["usage-quotas"] as const,
};

export function useUsageQuotasQuery(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: usageQuotaKeys.all,
    queryFn: async (): Promise<UsageQuotaData> =>
      unwrap(await api.GET("/api/v1/user/usage-quotas")),
    enabled: options?.enabled ?? true,
  });
}
