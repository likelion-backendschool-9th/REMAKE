// 프로필(API GET /api/v1/user/profile). 가구원 수·재료 보충 설정 등 여러 화면이 함께 쓴다.

import { useQuery } from "@tanstack/react-query";

import { api, unwrap } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

export type UserProfileData = components["schemas"]["ProfileResponse"];

export const profileKeys = {
  all: ["profile"] as const,
};

export function useProfileQuery(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: profileKeys.all,
    queryFn: async (): Promise<UserProfileData> => unwrap(await api.GET("/api/v1/user/profile")),
    enabled: options?.enabled ?? true,
  });
}
